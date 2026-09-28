package docs;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.javadoc.Javadoc;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses the project's Java sources into a {@link JavaDocModel.TypeDoc} list.
 *
 * <p>Only {@code src/main/java} is scanned; {@code build/}, {@code generated/}, and
 * test sources are ignored. Every type and member is captured regardless of
 * modifier, together with its parsed Javadoc (empty when undocumented).</p>
 */
public final class JavaDocScanner {

	private final Path sourceRoot;

	/**
	 * Creates a scanner rooted at the given source directory.
	 *
	 * @param sourceRoot the {@code src/main/java} directory to scan
	 */
	public JavaDocScanner(Path sourceRoot) {
		this.sourceRoot = sourceRoot;
	}

	/**
	 * Parses every source file and returns the collected type records.
	 *
	 * @return the type records, in parse order (the renderer sorts them)
	 */
	public List<JavaDocModel.TypeDoc> scan() {
		JavaParser parser = new JavaParser(
				new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_11));
		List<JavaDocModel.TypeDoc> out = new ArrayList<>();
		List<Path> files = sourceFiles();
		for (Path file : files) {
			CompilationUnit cu = parse(parser, file);
			String packageName = cu.getPackageDeclaration()
					.map(pd -> pd.getNameAsString())
					.orElse("");
			for (TypeDeclaration<?> type : cu.getTypes()) {
				collectType(type, packageName, out);
			}
		}
		return out;
	}

	/**
	 * Returns the {@code .java} files under the source root, excluding build output.
	 *
	 * @return the source file paths
	 */
	private List<Path> sourceFiles() {
		if (!Files.isDirectory(sourceRoot)) {
			return List.of();
		}
		try (Stream<Path> walk = Files.walk(sourceRoot)) {
			return walk
					.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().endsWith(".java"))
					.filter(p -> !isExcluded(p))
					.collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to walk source root " + sourceRoot, e);
		}
	}

	/**
	 * Reports whether a path lies under an excluded directory.
	 *
	 * @param path the candidate source file
	 * @return {@code true} if the file should be skipped
	 */
	private boolean isExcluded(Path path) {
		for (Path segment : path) {
			String name = segment.toString();
			if (name.equals("build") || name.equals("generated")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Parses a single source file, failing loudly on syntax errors.
	 *
	 * @param parser the configured parser
	 * @param file the file to parse
	 * @return the parsed compilation unit
	 */
	private CompilationUnit parse(JavaParser parser, Path file) {
		try {
			String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
			ParseResult<CompilationUnit> result = parser.parse(content);
			if (!result.isSuccessful() || !result.getResult().isPresent()) {
				throw new IllegalStateException("Failed to parse " + file + ": " + result.getProblems());
			}
			return result.getResult().get();
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to read " + file, e);
		}
	}

	/**
	 * Records a type declaration and recurses into its nested types.
	 *
	 * @param type the type declaration
	 * @param packageName the enclosing package name
	 * @param out the accumulating result list
	 */
	private void collectType(TypeDeclaration<?> type, String packageName, List<JavaDocModel.TypeDoc> out) {
		String qualifiedName = type.getFullyQualifiedName().orElse(type.getNameAsString());
		JavaDocModel.TypeKind kind = kindOf(type);
		List<JavaDocModel.MemberDoc> fields = new ArrayList<>();
		List<JavaDocModel.MemberDoc> constructors = new ArrayList<>();
		List<JavaDocModel.MemberDoc> methods = new ArrayList<>();
		if (type instanceof EnumDeclaration) {
			for (EnumConstantDeclaration entry : ((EnumDeclaration) type).getEntries()) {
				fields.add(enumConstant(entry));
			}
		}
		for (FieldDeclaration field : type.getFields()) {
			for (VariableDeclarator variable : field.getVariables()) {
				fields.add(fieldMember(field, variable));
			}
		}
		for (ConstructorDeclaration ctor : type.getConstructors()) {
			constructors.add(constructorMember(ctor));
		}
		for (MethodDeclaration method : type.getMethods()) {
			methods.add(methodMember(method));
		}
		for (BodyDeclaration<?> member : type.getMembers()) {
			if (member instanceof AnnotationMemberDeclaration) {
				methods.add(annotationMember((AnnotationMemberDeclaration) member));
			}
		}
		out.add(new JavaDocModel.TypeDoc(
				packageName,
				qualifiedName,
				type.getNameAsString(),
				kind,
				typeSignature(type, kind),
				modifiersOf(type),
				javadocOf(type),
				fields,
				constructors,
				methods));
		for (BodyDeclaration<?> member : type.getMembers()) {
			if (member instanceof TypeDeclaration) {
				collectType((TypeDeclaration<?>) member, packageName, out);
			}
		}
	}

	/**
	 * Determines the kind of a type declaration.
	 *
	 * @param type the type declaration
	 * @return the matching kind
	 */
	private JavaDocModel.TypeKind kindOf(TypeDeclaration<?> type) {
		if (type instanceof EnumDeclaration) {
			return JavaDocModel.TypeKind.ENUM;
		}
		if (type instanceof AnnotationDeclaration) {
			return JavaDocModel.TypeKind.ANNOTATION;
		}
		if (type instanceof ClassOrInterfaceDeclaration && ((ClassOrInterfaceDeclaration) type).isInterface()) {
			return JavaDocModel.TypeKind.INTERFACE;
		}
		return JavaDocModel.TypeKind.CLASS;
	}

	/**
	 * Builds the display signature for a type declaration.
	 *
	 * @param type the type declaration
	 * @param kind the resolved type kind
	 * @return the declaration line
	 */
	private String typeSignature(TypeDeclaration<?> type, JavaDocModel.TypeKind kind) {
		StringBuilder sb = new StringBuilder();
		sb.append(modifierPrefix(type));
		sb.append(kind.label().equals("annotation") ? "@interface" : kind.label());
		sb.append(' ').append(type.getNameAsString());
		if (type instanceof ClassOrInterfaceDeclaration) {
			ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration) type;
			if (!decl.getTypeParameters().isEmpty()) {
				sb.append('<')
						.append(decl.getTypeParameters().stream()
								.map(tp -> tp.asString())
								.collect(Collectors.joining(", ")))
						.append('>');
			}
		}
		return sb.toString();
	}

	/**
	 * Builds a member record for an enum constant.
	 *
	 * @param entry the enum constant declaration
	 * @return the member record
	 */
	private JavaDocModel.MemberDoc enumConstant(EnumConstantDeclaration entry) {
		return new JavaDocModel.MemberDoc(
				JavaDocModel.MemberKind.ENUM_CONSTANT,
				entry.getNameAsString(),
				"",
				entry.getNameAsString(),
				List.of(),
				EnumSet.noneOf(Modifier.Keyword.class),
				false,
				javadocOf(entry));
	}

	/**
	 * Builds a member record for one field variable.
	 *
	 * @param field the field declaration (holds modifiers and Javadoc)
	 * @param variable the individual variable declarator
	 * @return the member record
	 */
	private JavaDocModel.MemberDoc fieldMember(FieldDeclaration field, VariableDeclarator variable) {
		String modifierAndType = (modifierPrefix(field) + variable.getTypeAsString()).trim();
		return new JavaDocModel.MemberDoc(
				JavaDocModel.MemberKind.FIELD,
				variable.getNameAsString(),
				modifierAndType,
				variable.getNameAsString(),
				List.of(),
				modifiersOf(field),
				false,
				javadocOf(field));
	}

	/**
	 * Builds a member record for a constructor.
	 *
	 * @param ctor the constructor declaration
	 * @return the member record
	 */
	private JavaDocModel.MemberDoc constructorMember(ConstructorDeclaration ctor) {
		String modifierAndType = (modifierPrefix(ctor) + typeParameterPrefix(ctor.getTypeParameters())).trim();
		StringBuilder declaration = new StringBuilder();
		declaration.append(ctor.getNameAsString());
		declaration.append('(').append(parameterList(ctor.getParameters())).append(')');
		declaration.append(throwsSuffix(ctor.getThrownExceptions().stream()
				.map(t -> t.asString())
				.collect(Collectors.toList())));
		return new JavaDocModel.MemberDoc(
				JavaDocModel.MemberKind.CONSTRUCTOR,
				ctor.getNameAsString(),
				modifierAndType,
				declaration.toString(),
				paramSimpleNames(ctor.getParameters()),
				modifiersOf(ctor),
				false,
				javadocOf(ctor));
	}

	/**
	 * Builds a member record for a method.
	 *
	 * @param method the method declaration
	 * @return the member record
	 */
	private JavaDocModel.MemberDoc methodMember(MethodDeclaration method) {
		String modifierAndType = (modifierPrefix(method)
				+ typeParameterPrefix(method.getTypeParameters())
				+ method.getTypeAsString()).trim();
		StringBuilder declaration = new StringBuilder();
		declaration.append(method.getNameAsString());
		declaration.append('(').append(parameterList(method.getParameters())).append(')');
		declaration.append(throwsSuffix(method.getThrownExceptions().stream()
				.map(t -> t.asString())
				.collect(Collectors.toList())));
		return new JavaDocModel.MemberDoc(
				JavaDocModel.MemberKind.METHOD,
				method.getNameAsString(),
				modifierAndType,
				declaration.toString(),
				paramSimpleNames(method.getParameters()),
				modifiersOf(method),
				method.getAnnotationByName("Override").isPresent(),
				javadocOf(method));
	}

	/**
	 * Builds a member record for an annotation element.
	 *
	 * @param member the annotation member declaration
	 * @return the member record
	 */
	private JavaDocModel.MemberDoc annotationMember(AnnotationMemberDeclaration member) {
		String modifierAndType = (modifierPrefix(member) + member.getTypeAsString()).trim();
		return new JavaDocModel.MemberDoc(
				JavaDocModel.MemberKind.METHOD,
				member.getNameAsString(),
				modifierAndType,
				member.getNameAsString() + "()",
				List.of(),
				modifiersOf(member),
				false,
				javadocOf(member));
	}

	/**
	 * Renders a parameter list as {@code Type name} pairs.
	 *
	 * @param parameters the parameters
	 * @return the comma-separated parameter text
	 */
	private String parameterList(List<Parameter> parameters) {
		return parameters.stream()
				.map(p -> paramType(p) + " " + p.getNameAsString())
				.collect(Collectors.joining(", "));
	}

	/**
	 * Renders a parameter's declared type, expanding varargs to {@code ...}.
	 *
	 * @param parameter the parameter
	 * @return the type text
	 */
	private String paramType(Parameter parameter) {
		String type = parameter.getType().asString();
		return parameter.isVarArgs() ? type + "..." : type;
	}

	/**
	 * Computes the overload-disambiguating simple type names for a parameter list.
	 *
	 * @param parameters the parameters
	 * @return the erased simple type names
	 */
	private List<String> paramSimpleNames(List<Parameter> parameters) {
		return parameters.stream()
				.map(p -> simpleTypeName(p.getType().asString()) + (p.isVarArgs() ? "[]" : ""))
				.collect(Collectors.toList());
	}

	/**
	 * Reduces a possibly-qualified, generic type string to a stable simple name.
	 *
	 * @param type the raw type string
	 * @return the simple type name with generics stripped and package removed
	 */
	private String simpleTypeName(String type) {
		StringBuilder sb = new StringBuilder();
		int depth = 0;
		for (int i = 0; i < type.length(); i++) {
			char c = type.charAt(i);
			if (c == '<') {
				depth++;
			}
			else if (c == '>') {
				depth--;
			}
			else if (depth == 0) {
				sb.append(c);
			}
		}
		String stripped = sb.toString().trim();
		int dot = stripped.lastIndexOf('.');
		return dot >= 0 ? stripped.substring(dot + 1) : stripped;
	}

	/**
	 * Renders method or constructor type parameters as a {@code <T, U> } prefix.
	 *
	 * @param typeParameters the type parameters
	 * @return the prefix, or empty string when there are none
	 */
	private String typeParameterPrefix(List<com.github.javaparser.ast.type.TypeParameter> typeParameters) {
		if (typeParameters.isEmpty()) {
			return "";
		}
		return "<" + typeParameters.stream()
				.map(tp -> tp.asString())
				.collect(Collectors.joining(", ")) + "> ";
	}

	/**
	 * Renders a {@code throws} suffix from exception type names.
	 *
	 * @param thrown the thrown exception type strings
	 * @return the suffix, or empty string when nothing is thrown
	 */
	private String throwsSuffix(List<String> thrown) {
		if (thrown.isEmpty()) {
			return "";
		}
		return " throws " + String.join(", ", thrown);
	}

	/**
	 * Renders a node's modifiers as a trailing-spaced prefix.
	 *
	 * @param node the modifier-bearing node
	 * @return the modifier text ending in a space, or empty string when none
	 */
	private String modifierPrefix(NodeWithModifiers<?> node) {
		String mods = node.getModifiers().stream()
				.map(m -> m.getKeyword().asString())
				.collect(Collectors.joining(" "));
		return mods.isEmpty() ? "" : mods + " ";
	}

	/**
	 * Resolves a declaration's Javadoc, recovering a comment that JavaParser attached
	 * to a leading annotation when the Javadoc sits between annotations rather than above them.
	 *
	 * @param node the declaration
	 * @return the parsed Javadoc, or empty if the declaration is undocumented
	 */
	private Optional<Javadoc> javadocOf(BodyDeclaration<?> node) {
		Optional<Javadoc> direct = parseJavadoc(node.getComment().orElse(null));
		if (direct.isPresent()) {
			return direct;
		}
		for (AnnotationExpr annotation : node.getAnnotations()) {
			Optional<Javadoc> onAnnotation = parseJavadoc(annotation.getComment().orElse(null));
			if (onAnnotation.isPresent()) {
				return onAnnotation;
			}
		}
		return Optional.empty();
	}

	/**
	 * Parses a comment into a {@link Javadoc}, or returns empty when it is not a Javadoc comment.
	 *
	 * @param comment the comment to parse, may be {@code null}
	 * @return the parsed Javadoc, or empty
	 */
	private Optional<Javadoc> parseJavadoc(Comment comment) {
		if (comment instanceof JavadocComment) {
			return Optional.of(((JavadocComment) comment).parse());
		}
		return Optional.empty();
	}

	/**
	 * Collects a node's modifier keywords into an {@link EnumSet}.
	 *
	 * @param node the modifier-bearing node
	 * @return the modifier keyword set
	 */
	private EnumSet<Modifier.Keyword> modifiersOf(NodeWithModifiers<?> node) {
		EnumSet<Modifier.Keyword> set = EnumSet.noneOf(Modifier.Keyword.class);
		for (Modifier modifier : node.getModifiers()) {
			set.add(modifier.getKeyword());
		}
		return set;
	}
}
