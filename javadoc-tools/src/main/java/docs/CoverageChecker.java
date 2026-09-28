package docs;

import com.github.javaparser.ast.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds public and protected declarations that are missing Javadoc.
 *
 * <p>Package-private and private declarations are never flagged. Interface and
 * annotation members, and enum constants, are treated as effectively public
 * unless explicitly marked {@code private}. {@code @Override} methods are
 * required to be documented by default; pass {@code excludeOverrides} to skip
 * them.</p>
 */
public final class CoverageChecker {

	private final boolean excludeOverrides;

	private final Map<String, JavaDocModel.TypeDoc> byQualifiedName = new HashMap<>();

	private final Map<String, Boolean> visibilityCache = new HashMap<>();

	/**
	 * Creates a checker.
	 *
	 * @param excludeOverrides whether {@code @Override} methods are exempt from the requirement
	 */
	public CoverageChecker(boolean excludeOverrides) {
		this.excludeOverrides = excludeOverrides;
	}

	/**
	 * Returns the sorted list of undocumented public/protected offenders.
	 *
	 * <p>A declaration is in scope only when it is public/protected <em>and</em> every
	 * enclosing type is itself externally reachable; members of a package-private (or
	 * private) type are never flagged, matching the "public + protected only" scope.</p>
	 *
	 * @param types the scanned type records
	 * @return the offender identifiers, sorted
	 */
	public List<String> findOffenders(List<JavaDocModel.TypeDoc> types) {
		byQualifiedName.clear();
		visibilityCache.clear();
		for (JavaDocModel.TypeDoc type : types) {
			byQualifiedName.put(type.qualifiedName(), type);
		}
		List<String> offenders = new ArrayList<>();
		for (JavaDocModel.TypeDoc type : types) {
			if (!typeExternallyVisible(type)) {
				continue;
			}
			if (!type.javadoc().isPresent()) {
				offenders.add(type.qualifiedName() + " (type)");
			}
			collectMembers(type, type.fields(), offenders);
			collectMembers(type, type.constructors(), offenders);
			collectMembers(type, type.methods(), offenders);
		}
		offenders.sort(String::compareTo);
		return offenders;
	}

	/**
	 * Reports whether a type is reachable as external API (it and every enclosing type
	 * are public/protected, with interface and annotation members implicitly public).
	 *
	 * @param type the type record
	 * @return {@code true} if the type is externally reachable
	 */
	private boolean typeExternallyVisible(JavaDocModel.TypeDoc type) {
		Boolean cached = visibilityCache.get(type.qualifiedName());
		if (cached != null) {
			return cached;
		}
		JavaDocModel.TypeDoc enclosing = enclosingType(type);
		boolean visibleHere;
		if (enclosing != null
				&& (enclosing.kind() == JavaDocModel.TypeKind.INTERFACE
				|| enclosing.kind() == JavaDocModel.TypeKind.ANNOTATION)) {
			visibleHere = true;
		}
		else {
			visibleHere = type.modifiers().contains(Modifier.Keyword.PUBLIC)
					|| type.modifiers().contains(Modifier.Keyword.PROTECTED);
		}
		boolean result = visibleHere && (enclosing == null || typeExternallyVisible(enclosing));
		visibilityCache.put(type.qualifiedName(), result);
		return result;
	}

	/**
	 * Finds the immediately enclosing type of a nested type, if any.
	 *
	 * @param type the type record
	 * @return the enclosing type, or {@code null} for a top-level type
	 */
	private JavaDocModel.TypeDoc enclosingType(JavaDocModel.TypeDoc type) {
		String qualifiedName = type.qualifiedName();
		int lastDot = qualifiedName.lastIndexOf('.');
		if (lastDot < 0) {
			return null;
		}
		return byQualifiedName.get(qualifiedName.substring(0, lastDot));
	}

	/**
	 * Flags undocumented members that require documentation.
	 *
	 * @param type the enclosing type
	 * @param members the members to inspect
	 * @param offenders the accumulating offender list
	 */
	private void collectMembers(
			JavaDocModel.TypeDoc type,
			List<JavaDocModel.MemberDoc> members,
			List<String> offenders) {
		for (JavaDocModel.MemberDoc member : members) {
			if (!requiresDoc(type, member)) {
				continue;
			}
			if (member.javadoc().isPresent()) {
				continue;
			}
			offenders.add(memberId(type, member));
		}
	}

	/**
	 * Reports whether a member requires documentation under the gate rules.
	 *
	 * @param type the enclosing type
	 * @param member the member record
	 * @return {@code true} if the member is in scope and not exempt
	 */
	private boolean requiresDoc(JavaDocModel.TypeDoc type, JavaDocModel.MemberDoc member) {
		if (excludeOverrides && member.overrides()) {
			return false;
		}
		return isEffectivelyPublicOrProtected(type, member);
	}

	/**
	 * Computes a member's effective visibility, accounting for implicit-public contexts.
	 *
	 * @param type the enclosing type
	 * @param member the member record
	 * @return {@code true} if the member is effectively public or protected
	 */
	private boolean isEffectivelyPublicOrProtected(JavaDocModel.TypeDoc type, JavaDocModel.MemberDoc member) {
		if (member.kind() == JavaDocModel.MemberKind.ENUM_CONSTANT) {
			return true;
		}
		if (member.modifiers().contains(Modifier.Keyword.PUBLIC)
				|| member.modifiers().contains(Modifier.Keyword.PROTECTED)) {
			return true;
		}
		boolean implicitlyPublicContext = type.kind() == JavaDocModel.TypeKind.INTERFACE
				|| type.kind() == JavaDocModel.TypeKind.ANNOTATION;
		return implicitlyPublicContext && !member.modifiers().contains(Modifier.Keyword.PRIVATE);
	}

	/**
	 * Builds the stable offender identifier for a member.
	 *
	 * @param type the enclosing type
	 * @param member the member record
	 * @return the identifier, e.g. {@code com.example.Foo#increment(int)}
	 */
	private String memberId(JavaDocModel.TypeDoc type, JavaDocModel.MemberDoc member) {
		if (member.kind() == JavaDocModel.MemberKind.METHOD
				|| member.kind() == JavaDocModel.MemberKind.CONSTRUCTOR) {
			return type.qualifiedName() + "#" + member.name()
					+ "(" + String.join(", ", member.paramTypeSimpleNames()) + ")";
		}
		return type.qualifiedName() + "#" + member.name();
	}
}
