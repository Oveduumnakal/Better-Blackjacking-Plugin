package docs;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.javadoc.Javadoc;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Immutable data holders describing the documented shape of the source tree.
 *
 * <p>The scanner populates these, the renderer turns them into Markdown, and the
 * coverage checker inspects them for missing public/protected documentation.</p>
 */
public final class JavaDocModel {

	private JavaDocModel() {
	}

	/**
	 * The kind of a type declaration.
	 */
	public enum TypeKind {
		/** A {@code class} declaration. */
		CLASS("class"),
		/** An {@code interface} declaration. */
		INTERFACE("interface"),
		/** An {@code enum} declaration. */
		ENUM("enum"),
		/** An {@code @interface} (annotation) declaration. */
		ANNOTATION("annotation");

		private final String label;

		TypeKind(String label) {
			this.label = label;
		}

		/**
		 * Returns the lowercase label rendered in the Markdown output.
		 *
		 * @return the display label
		 */
		public String label() {
			return label;
		}
	}

	/**
	 * The kind of a member declaration.
	 */
	public enum MemberKind {
		/** A field declaration. */
		FIELD,
		/** An enum constant. */
		ENUM_CONSTANT,
		/** A constructor declaration. */
		CONSTRUCTOR,
		/** A method declaration. */
		METHOD
	}

	/**
	 * A documented type declaration, including its members.
	 */
	public static final class TypeDoc {

		private final String packageName;
		private final String qualifiedName;
		private final String simpleName;
		private final TypeKind kind;
		private final String signature;
		private final EnumSet<Modifier.Keyword> modifiers;
		private final Optional<Javadoc> javadoc;
		private final List<MemberDoc> fields;
		private final List<MemberDoc> constructors;
		private final List<MemberDoc> methods;

		/**
		 * Creates a type record.
		 *
		 * @param packageName the package name, or empty string for the default package
		 * @param qualifiedName the fully-qualified name, with nested types joined by {@code .}
		 * @param simpleName the simple (unqualified) name
		 * @param kind the type kind
		 * @param signature the human-readable declaration line
		 * @param modifiers the declared modifiers
		 * @param javadoc the parsed Javadoc, or empty if undocumented
		 * @param fields the field and enum-constant members
		 * @param constructors the constructor members
		 * @param methods the method members
		 */
		public TypeDoc(
				String packageName,
				String qualifiedName,
				String simpleName,
				TypeKind kind,
				String signature,
				EnumSet<Modifier.Keyword> modifiers,
				Optional<Javadoc> javadoc,
				List<MemberDoc> fields,
				List<MemberDoc> constructors,
				List<MemberDoc> methods) {
			this.packageName = packageName;
			this.qualifiedName = qualifiedName;
			this.simpleName = simpleName;
			this.kind = kind;
			this.signature = signature;
			this.modifiers = modifiers;
			this.javadoc = javadoc;
			this.fields = fields;
			this.constructors = constructors;
			this.methods = methods;
		}

		/**
		 * Returns the package name.
		 *
		 * @return the package name, or empty string for the default package
		 */
		public String packageName() {
			return packageName;
		}

		/**
		 * Returns the fully-qualified name.
		 *
		 * @return the fully-qualified name
		 */
		public String qualifiedName() {
			return qualifiedName;
		}

		/**
		 * Returns the simple name.
		 *
		 * @return the simple name
		 */
		public String simpleName() {
			return simpleName;
		}

		/**
		 * Returns the type kind.
		 *
		 * @return the type kind
		 */
		public TypeKind kind() {
			return kind;
		}

		/**
		 * Returns the human-readable declaration line.
		 *
		 * @return the declaration signature
		 */
		public String signature() {
			return signature;
		}

		/**
		 * Returns the declared modifiers.
		 *
		 * @return the modifier keywords
		 */
		public EnumSet<Modifier.Keyword> modifiers() {
			return modifiers;
		}

		/**
		 * Returns the parsed Javadoc, if present.
		 *
		 * @return the Javadoc, or empty if undocumented
		 */
		public Optional<Javadoc> javadoc() {
			return javadoc;
		}

		/**
		 * Returns the field and enum-constant members.
		 *
		 * @return the fields
		 */
		public List<MemberDoc> fields() {
			return fields;
		}

		/**
		 * Returns the constructor members.
		 *
		 * @return the constructors
		 */
		public List<MemberDoc> constructors() {
			return constructors;
		}

		/**
		 * Returns the method members.
		 *
		 * @return the methods
		 */
		public List<MemberDoc> methods() {
			return methods;
		}
	}

	/**
	 * A documented member declaration (field, enum constant, constructor, or method).
	 */
	public static final class MemberDoc {

		private final MemberKind kind;
		private final String name;
		private final String modifierAndType;
		private final String declaration;
		private final String signature;
		private final List<String> paramTypeSimpleNames;
		private final EnumSet<Modifier.Keyword> modifiers;
		private final boolean overrides;
		private final Optional<Javadoc> javadoc;

		/**
		 * Creates a member record.
		 *
		 * @param kind the member kind
		 * @param name the member name
		 * @param modifierAndType the modifier-and-type prefix shown in summary tables (may be empty)
		 * @param declaration the name-and-parameters portion shown in summary tables
		 * @param paramTypeSimpleNames the parameter type simple names, used to disambiguate overloads
		 * @param modifiers the declared modifiers
		 * @param overrides whether the member carries an {@code @Override} annotation
		 * @param javadoc the parsed Javadoc, or empty if undocumented
		 */
		public MemberDoc(
				MemberKind kind,
				String name,
				String modifierAndType,
				String declaration,
				List<String> paramTypeSimpleNames,
				EnumSet<Modifier.Keyword> modifiers,
				boolean overrides,
				Optional<Javadoc> javadoc) {
			this.kind = kind;
			this.name = name;
			this.modifierAndType = modifierAndType;
			this.declaration = declaration;
			this.signature = (modifierAndType + " " + declaration).trim();
			this.paramTypeSimpleNames = paramTypeSimpleNames;
			this.modifiers = modifiers;
			this.overrides = overrides;
			this.javadoc = javadoc;
		}

		/**
		 * Returns the member kind.
		 *
		 * @return the member kind
		 */
		public MemberKind kind() {
			return kind;
		}

		/**
		 * Returns the member name.
		 *
		 * @return the member name
		 */
		public String name() {
			return name;
		}

		/**
		 * Returns the modifier-and-type prefix used in summary tables.
		 *
		 * @return the modifier-and-type text, possibly empty
		 */
		public String modifierAndType() {
			return modifierAndType;
		}

		/**
		 * Returns the name-and-parameters portion used in summary tables.
		 *
		 * @return the declaration text
		 */
		public String declaration() {
			return declaration;
		}

		/**
		 * Returns the full human-readable declaration line.
		 *
		 * @return the declaration signature
		 */
		public String signature() {
			return signature;
		}

		/**
		 * Returns the parameter type simple names.
		 *
		 * @return the parameter type simple names
		 */
		public List<String> paramTypeSimpleNames() {
			return paramTypeSimpleNames;
		}

		/**
		 * Returns the declared modifiers.
		 *
		 * @return the modifier keywords
		 */
		public EnumSet<Modifier.Keyword> modifiers() {
			return modifiers;
		}

		/**
		 * Returns whether the member is annotated {@code @Override}.
		 *
		 * @return {@code true} if the member overrides a supertype member
		 */
		public boolean overrides() {
			return overrides;
		}

		/**
		 * Returns the parsed Javadoc, if present.
		 *
		 * @return the Javadoc, or empty if undocumented
		 */
		public Optional<Javadoc> javadoc() {
			return javadoc;
		}
	}
}
