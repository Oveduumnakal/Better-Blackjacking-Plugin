package docs;

import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Renders a sorted {@link JavaDocModel.TypeDoc} list into the single {@code JavaDocs.md} string.
 *
 * <p>The layout mirrors a generated Javadoc page: each type shows Summary tables
 * (nested types, enum constants, fields, constructors, methods) followed by Detail
 * sections. Output is deterministic — types and members are sorted by stable keys,
 * inline tags are converted uniformly, line endings are {@code \n}, and there is a
 * single trailing newline. No timestamps, paths, or version strings appear.</p>
 */
public final class MarkdownRenderer {

	private static final String TITLE = "# Better Blackjacking — JavaDoc Reference";

	private static final Pattern INLINE_TAG =
			Pattern.compile("\\{@(code|literal|link|linkplain)\\s+([^}]*)}");

	private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

	private static final Comparator<JavaDocModel.TypeDoc> TYPE_ORDER =
			Comparator.comparing((JavaDocModel.TypeDoc t) -> t.packageName())
					.thenComparing(JavaDocModel.TypeDoc::qualifiedName);

	private static final Comparator<JavaDocModel.MemberDoc> MEMBER_ORDER =
			Comparator.comparing((JavaDocModel.MemberDoc m) -> m.name())
					.thenComparing(m -> String.join(",", m.paramTypeSimpleNames()));

	/**
	 * Renders the full document.
	 *
	 * @param types the collected type records (any order; sorted internally)
	 * @return the complete Markdown document, ending in exactly one newline
	 */
	public String render(List<JavaDocModel.TypeDoc> types) {
		List<JavaDocModel.TypeDoc> sorted = types.stream()
				.sorted(TYPE_ORDER)
				.collect(Collectors.toList());
		StringBuilder sb = new StringBuilder();
		sb.append(TITLE).append("\n\n");
		sb.append("<!-- GENERATED FILE — DO NOT EDIT BY HAND.\n");
		sb.append("     Run `./gradlew generateJavaDocs` and commit the result. -->\n\n");
		sb.append("## Contents\n\n");
		for (JavaDocModel.TypeDoc type : sorted) {
			sb.append("- [")
					.append(type.qualifiedName())
					.append("](#")
					.append(slug(type.qualifiedName()))
					.append(")\n");
		}
		sb.append("\n");
		for (JavaDocModel.TypeDoc type : sorted) {
			sb.append("---\n\n");
			renderType(sb, type, sorted);
		}
		return normalizeTrailing(sb.toString());
	}

	/**
	 * Renders a single type section: header, summaries, then details.
	 *
	 * @param sb the output buffer
	 * @param type the type record
	 * @param allTypes the full sorted type list, used to find nested types
	 */
	private void renderType(StringBuilder sb, JavaDocModel.TypeDoc type, List<JavaDocModel.TypeDoc> allTypes) {
		sb.append("## ").append(type.qualifiedName()).append("\n\n");
		sb.append("_").append(type.kind().label()).append("_\n\n");
		sb.append("`").append(type.signature()).append("`\n\n");
		type.javadoc().ifPresent(doc -> appendDescription(sb, doc));
		type.javadoc().ifPresent(doc -> appendTypeTags(sb, doc));

		List<JavaDocModel.TypeDoc> nested = nestedTypes(type, allTypes);
		List<JavaDocModel.MemberDoc> enumConstants = membersOfKind(
				type.fields(), JavaDocModel.MemberKind.ENUM_CONSTANT);
		List<JavaDocModel.MemberDoc> fields = membersOfKind(
				type.fields(), JavaDocModel.MemberKind.FIELD);
		List<JavaDocModel.MemberDoc> constructors = sortMembers(type.constructors());
		List<JavaDocModel.MemberDoc> methods = sortMembers(type.methods());

		renderNestedSummary(sb, nested);
		renderMemberSummary(sb, "Enum Constant Summary", "Enum Constant", enumConstants, false);
		renderMemberSummary(sb, "Field Summary", "Field", fields, true);
		renderMemberSummary(sb, "Constructor Summary", "Constructor", constructors, false);
		renderMemberSummary(sb, "Method Summary", "Method", methods, true);

		renderMemberDetail(sb, "Enum Constant Detail", enumConstants);
		renderMemberDetail(sb, "Field Detail", fields);
		renderMemberDetail(sb, "Constructor Detail", constructors);
		renderMemberDetail(sb, "Method Detail", methods);
	}

	/**
	 * Renders the nested-type summary table, if the type has nested types.
	 *
	 * @param sb the output buffer
	 * @param nested the direct nested types, already sorted
	 */
	private void renderNestedSummary(StringBuilder sb, List<JavaDocModel.TypeDoc> nested) {
		if (nested.isEmpty()) {
			return;
		}
		sb.append("### Nested Type Summary\n\n");
		sb.append("| Type | Description |\n");
		sb.append("|---|---|\n");
		for (JavaDocModel.TypeDoc child : nested) {
			sb.append("| _").append(child.kind().label()).append("_ [`")
					.append(child.simpleName()).append("`](#")
					.append(slug(child.qualifiedName())).append(") | ")
					.append(summaryText(child.javadoc())).append(" |\n");
		}
		sb.append("\n");
	}

	/**
	 * Renders one member summary table, if the group is non-empty.
	 *
	 * @param sb the output buffer
	 * @param heading the section heading (e.g. {@code Method Summary})
	 * @param memberColumn the header for the declaration column
	 * @param members the group's members, already sorted
	 * @param withType whether to include a leading {@code Modifier and Type} column
	 */
	private void renderMemberSummary(
			StringBuilder sb,
			String heading,
			String memberColumn,
			List<JavaDocModel.MemberDoc> members,
			boolean withType) {
		if (members.isEmpty()) {
			return;
		}
		sb.append("### ").append(heading).append("\n\n");
		if (withType) {
			sb.append("| Modifier and Type | ").append(memberColumn).append(" | Description |\n");
			sb.append("|---|---|---|\n");
		}
		else {
			sb.append("| ").append(memberColumn).append(" | Description |\n");
			sb.append("|---|---|\n");
		}
		for (JavaDocModel.MemberDoc member : members) {
			sb.append("| ");
			if (withType) {
				sb.append(cellCode(member.modifierAndType())).append(" | ");
			}
			sb.append(cellCode(member.declaration())).append(" | ")
					.append(summaryText(member.javadoc())).append(" |\n");
		}
		sb.append("\n");
	}

	/**
	 * Renders one member detail section, if the group is non-empty.
	 *
	 * @param sb the output buffer
	 * @param heading the section heading (e.g. {@code Method Detail})
	 * @param members the group's members, already sorted
	 */
	private void renderMemberDetail(StringBuilder sb, String heading, List<JavaDocModel.MemberDoc> members) {
		if (members.isEmpty()) {
			return;
		}
		sb.append("### ").append(heading).append("\n\n");
		for (JavaDocModel.MemberDoc member : members) {
			sb.append("#### ").append(member.name()).append("\n\n");
			sb.append("`").append(member.signature()).append("`\n\n");
			member.javadoc().ifPresent(doc -> appendDescription(sb, doc));
			member.javadoc().ifPresent(doc -> appendMemberTags(sb, doc));
		}
	}

	/**
	 * Finds the direct nested types of a type from the full sorted list.
	 *
	 * @param type the enclosing type
	 * @param allTypes the full sorted type list
	 * @return the direct nested types, in sorted order
	 */
	private List<JavaDocModel.TypeDoc> nestedTypes(
			JavaDocModel.TypeDoc type,
			List<JavaDocModel.TypeDoc> allTypes) {
		String prefix = type.qualifiedName() + ".";
		return allTypes.stream()
				.filter(t -> t.qualifiedName().startsWith(prefix))
				.filter(t -> t.qualifiedName().indexOf('.', prefix.length()) < 0)
				.collect(Collectors.toList());
	}

	/**
	 * Filters a member list to a single kind, sorted by the stable member order.
	 *
	 * @param members the members to filter
	 * @param kind the kind to keep
	 * @return the matching members, sorted
	 */
	private List<JavaDocModel.MemberDoc> membersOfKind(
			List<JavaDocModel.MemberDoc> members,
			JavaDocModel.MemberKind kind) {
		return members.stream()
				.filter(m -> m.kind() == kind)
				.sorted(MEMBER_ORDER)
				.collect(Collectors.toList());
	}

	/**
	 * Sorts a member list by the stable member order.
	 *
	 * @param members the members to sort
	 * @return the sorted members
	 */
	private List<JavaDocModel.MemberDoc> sortMembers(List<JavaDocModel.MemberDoc> members) {
		return members.stream()
				.sorted(MEMBER_ORDER)
				.collect(Collectors.toList());
	}

	/**
	 * Appends a Javadoc description body, if it has visible text.
	 *
	 * @param sb the output buffer
	 * @param doc the parsed Javadoc
	 */
	private void appendDescription(StringBuilder sb, Javadoc doc) {
		String body = convertInlineTags(doc.getDescription().toText()).trim();
		if (!body.isEmpty()) {
			sb.append(body).append("\n\n");
		}
	}

	/**
	 * Appends type-level block tags as paragraph lines (Deprecated, See, and others).
	 *
	 * @param sb the output buffer
	 * @param doc the parsed Javadoc
	 */
	private void appendTypeTags(StringBuilder sb, Javadoc doc) {
		for (JavadocBlockTag tag : doc.getBlockTags()) {
			String content = convertInlineTags(tag.getContent().toText()).trim();
			String name = tag.getName().orElse("");
			StringBuilder line = new StringBuilder();
			line.append("**").append(typeTagLabel(tag)).append(":**");
			if (!name.isEmpty()) {
				line.append(" `").append(name).append("`");
			}
			if (!content.isEmpty()) {
				line.append(name.isEmpty() ? " " : " — ").append(content);
			}
			sb.append(line).append("\n\n");
		}
	}

	/**
	 * Appends member-level block tags grouped as Parameters, Returns, Throws, then extras.
	 *
	 * @param sb the output buffer
	 * @param doc the parsed Javadoc
	 */
	private void appendMemberTags(StringBuilder sb, Javadoc doc) {
		boolean any = false;
		for (JavadocBlockTag tag : doc.getBlockTags()) {
			if (tag.getType() == JavadocBlockTag.Type.PARAM) {
				sb.append("- **Parameter** `").append(tag.getName().orElse("")).append("`");
				appendDash(sb, tagContent(tag));
				sb.append("\n");
				any = true;
			}
		}
		for (JavadocBlockTag tag : doc.getBlockTags()) {
			if (tag.getType() == JavadocBlockTag.Type.RETURN) {
				sb.append("- **Returns:**");
				appendSpace(sb, tagContent(tag));
				sb.append("\n");
				any = true;
			}
		}
		for (JavadocBlockTag tag : doc.getBlockTags()) {
			if (tag.getType() == JavadocBlockTag.Type.THROWS
					|| tag.getType() == JavadocBlockTag.Type.EXCEPTION) {
				sb.append("- **Throws** `").append(tag.getName().orElse("")).append("`");
				appendDash(sb, tagContent(tag));
				sb.append("\n");
				any = true;
			}
		}
		for (JavadocBlockTag tag : doc.getBlockTags()) {
			if (isExtraMemberTag(tag)) {
				sb.append("- **").append(capitalize(tag.getTagName())).append(":**");
				appendSpace(sb, tagContent(tag));
				sb.append("\n");
				any = true;
			}
		}
		if (any) {
			sb.append("\n");
		}
	}

	/**
	 * Reports whether a member tag is neither a parameter, return, nor throws tag.
	 *
	 * @param tag the block tag
	 * @return {@code true} if the tag is rendered in the generic extras group
	 */
	private boolean isExtraMemberTag(JavadocBlockTag tag) {
		JavadocBlockTag.Type type = tag.getType();
		return type != JavadocBlockTag.Type.PARAM
				&& type != JavadocBlockTag.Type.RETURN
				&& type != JavadocBlockTag.Type.THROWS
				&& type != JavadocBlockTag.Type.EXCEPTION;
	}

	/**
	 * Returns a block tag's content with inline tags converted and edges trimmed.
	 *
	 * @param tag the block tag
	 * @return the cleaned content text
	 */
	private String tagContent(JavadocBlockTag tag) {
		return convertInlineTags(tag.getContent().toText()).trim();
	}

	/**
	 * Appends {@code  — content} when content is present, nothing otherwise.
	 *
	 * @param sb the output buffer
	 * @param content the trimmed content text
	 */
	private void appendDash(StringBuilder sb, String content) {
		if (!content.isEmpty()) {
			sb.append(" — ").append(content);
		}
	}

	/**
	 * Appends {@code  content} when content is present, nothing otherwise.
	 *
	 * @param sb the output buffer
	 * @param content the trimmed content text
	 */
	private void appendSpace(StringBuilder sb, String content) {
		if (!content.isEmpty()) {
			sb.append(" ").append(content);
		}
	}

	/**
	 * Chooses the display label for a type-level block tag.
	 *
	 * @param tag the block tag
	 * @return the label (e.g. {@code Deprecated}, {@code See}, or the capitalized tag name)
	 */
	private String typeTagLabel(JavadocBlockTag tag) {
		switch (tag.getType()) {
			case DEPRECATED:
				return "Deprecated";
			case SEE:
				return "See";
			case PARAM:
				return "Param";
			case RETURN:
				return "Returns";
			case THROWS:
			case EXCEPTION:
				return "Throws";
			default:
				return capitalize(tag.getTagName());
		}
	}

	/**
	 * Extracts the first-sentence summary for a table Description cell.
	 *
	 * @param javadoc the member or type Javadoc
	 * @return the escaped, single-line first sentence, or empty string if undocumented
	 */
	private String summaryText(java.util.Optional<Javadoc> javadoc) {
		if (!javadoc.isPresent()) {
			return "";
		}
		String text = convertInlineTags(javadoc.get().getDescription().toText());
		text = HTML_TAG.matcher(text).replaceAll(" ");
		text = firstSentence(text).replace("\n", " ").replace("\r", " ").trim();
		text = text.replaceAll("\\s+", " ");
		return text.replace("|", "\\|");
	}

	/**
	 * Returns the first sentence of description text (up to a period at a word boundary).
	 *
	 * @param text the description text
	 * @return the first sentence, including its terminating period
	 */
	private String firstSentence(String text) {
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '.') {
				boolean atEnd = i + 1 >= text.length();
				if (atEnd || Character.isWhitespace(text.charAt(i + 1))) {
					return text.substring(0, i + 1);
				}
			}
		}
		return text;
	}

	/**
	 * Wraps text in inline code for a table cell, escaping pipes.
	 *
	 * @param text the raw cell text
	 * @return the escaped inline-code cell, or an empty string when the text is blank
	 */
	private String cellCode(String text) {
		if (text.isEmpty()) {
			return "";
		}
		return "`" + text.replace("|", "\\|") + "`";
	}

	/**
	 * Converts supported inline tags to inline code, leaving other inline tags raw.
	 *
	 * @param text the raw description or content text
	 * @return the converted text
	 */
	private String convertInlineTags(String text) {
		Matcher matcher = INLINE_TAG.matcher(text);
		StringBuffer out = new StringBuffer();
		while (matcher.find()) {
			String inner = matcher.group(2).trim();
			matcher.appendReplacement(out, Matcher.quoteReplacement("`" + inner + "`"));
		}
		matcher.appendTail(out);
		return out.toString();
	}

	/**
	 * Capitalizes the first character of a tag name for display.
	 *
	 * @param tagName the raw tag name
	 * @return the capitalized name
	 */
	private String capitalize(String tagName) {
		if (tagName == null || tagName.isEmpty()) {
			return "";
		}
		return Character.toUpperCase(tagName.charAt(0)) + tagName.substring(1);
	}

	/**
	 * Produces a GitHub-compatible heading anchor slug.
	 *
	 * @param heading the heading text
	 * @return the anchor slug
	 */
	private String slug(String heading) {
		StringBuilder sb = new StringBuilder();
		for (char c : heading.toLowerCase().toCharArray()) {
			if (Character.isLetterOrDigit(c) && c < 128) {
				sb.append(c);
			}
			else if (c == ' ' || c == '-') {
				sb.append('-');
			}
		}
		return sb.toString();
	}

	/**
	 * Normalizes line endings to {@code \n} and enforces exactly one trailing newline.
	 *
	 * @param text the assembled document text
	 * @return the normalized text
	 */
	private String normalizeTrailing(String text) {
		String lf = text.replace("\r\n", "\n").replace("\r", "\n");
		int end = lf.length();
		while (end > 0 && lf.charAt(end - 1) == '\n') {
			end--;
		}
		return lf.substring(0, end) + "\n";
	}
}
