package docs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-line driver for the JavaDocs.md generator and freshness gate.
 *
 * <p>This lives in a standalone Gradle build ({@code javadoc-tools}) invoked only by CI and
 * developers, so the third-party javaparser dependency never enters the plugin build the
 * RuneLite plugin-hub compiles (which runs under dependency verification). Usage:</p>
 *
 * <pre>
 * generate &lt;sourceRoot&gt; &lt;outputFile&gt;
 * check    &lt;sourceRoot&gt; &lt;outputFile&gt; [--exclude-overrides]
 * </pre>
 */
public final class JavaDocsCli {

	private static final int DIFF_PREVIEW_LINES = 20;

	private JavaDocsCli() {
	}

	/**
	 * Runs the {@code generate} or {@code check} subcommand.
	 *
	 * @param args the subcommand, source root, output file, and optional {@code --exclude-overrides}
	 */
	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("usage: (generate|check) <sourceRoot> <outputFile> [--exclude-overrides]");
			System.exit(2);
			return;
		}
		String command = args[0];
		Path sourceRoot = Paths.get(args[1]);
		Path output = Paths.get(args[2]);
		boolean excludeOverrides = false;
		for (int i = 3; i < args.length; i++) {
			if ("--exclude-overrides".equals(args[i])) {
				excludeOverrides = true;
			}
		}
		if ("generate".equals(command)) {
			generate(sourceRoot, output);
		}
		else if ("check".equals(command)) {
			check(sourceRoot, output, excludeOverrides);
		}
		else {
			System.err.println("unknown command: " + command);
			System.exit(2);
		}
	}

	/**
	 * Scans, renders, and writes the Markdown file as UTF-8 with LF line endings.
	 *
	 * @param sourceRoot the {@code src/main/java} directory to scan
	 * @param output the {@code JavaDocs.md} file to write
	 */
	private static void generate(Path sourceRoot, Path output) {
		List<JavaDocModel.TypeDoc> types = new JavaDocScanner(sourceRoot).scan();
		String markdown = new MarkdownRenderer().render(types);
		try {
			Files.write(output, markdown.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to write " + output, e);
		}
		System.out.println("Wrote " + output + " (" + types.size() + " types)");
	}

	/**
	 * Runs the drift and coverage checks and exits non-zero if either reports a problem.
	 *
	 * @param sourceRoot the {@code src/main/java} directory to scan
	 * @param output the committed {@code JavaDocs.md} to compare against
	 * @param excludeOverrides whether {@code @Override} methods are exempt from the coverage requirement
	 */
	private static void check(Path sourceRoot, Path output, boolean excludeOverrides) {
		List<JavaDocModel.TypeDoc> types = new JavaDocScanner(sourceRoot).scan();
		List<String> failures = new ArrayList<>();
		checkDrift(types, output, failures);
		checkCoverage(types, excludeOverrides, failures);
		if (!failures.isEmpty()) {
			System.err.println(String.join("\n\n", failures));
			System.exit(1);
			return;
		}
		System.out.println("JavaDocs.md is fresh and all public/protected members are documented.");
	}

	/**
	 * Compares freshly rendered Markdown against the committed file byte-for-byte.
	 *
	 * @param types the scanned type records
	 * @param output the committed {@code JavaDocs.md}
	 * @param failures the accumulating failure-message list
	 */
	private static void checkDrift(List<JavaDocModel.TypeDoc> types, Path output, List<String> failures) {
		String expected = new MarkdownRenderer().render(types);
		if (!Files.exists(output)) {
			failures.add("JavaDocs.md is missing. Run ./gradlew generateJavaDocs and commit the result.");
			return;
		}
		String actual = readLf(output);
		if (!expected.equals(actual)) {
			StringBuilder message = new StringBuilder(
					"JavaDocs.md is out of date. Run ./gradlew generateJavaDocs and commit the result.");
			message.append("\n").append(firstDiff(expected, actual));
			failures.add(message.toString());
		}
	}

	/**
	 * Runs the coverage checker and records any offenders.
	 *
	 * @param types the scanned type records
	 * @param excludeOverrides whether {@code @Override} methods are exempt
	 * @param failures the accumulating failure-message list
	 */
	private static void checkCoverage(List<JavaDocModel.TypeDoc> types, boolean excludeOverrides,
			List<String> failures) {
		List<String> offenders = new CoverageChecker(excludeOverrides).findOffenders(types);
		if (!offenders.isEmpty()) {
			StringBuilder message = new StringBuilder(
					"The following public/protected declarations are missing Javadoc:");
			for (String offender : offenders) {
				message.append("\n  ").append(offender);
			}
			failures.add(message.toString());
		}
	}

	/**
	 * Reads a file as UTF-8, normalizing line endings to LF for comparison.
	 *
	 * @param path the file to read
	 * @return the file content with {@code \n} line endings
	 */
	private static String readLf(Path path) {
		try {
			String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
			return content.replace("\r\n", "\n").replace("\r", "\n");
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to read " + path, e);
		}
	}

	/**
	 * Builds a short preview of the first differing lines to aid debugging.
	 *
	 * @param expected the freshly rendered content
	 * @param actual the committed content
	 * @return a unified-style preview of the first divergence
	 */
	private static String firstDiff(String expected, String actual) {
		String[] expectedLines = expected.split("\n", -1);
		String[] actualLines = actual.split("\n", -1);
		int max = Math.max(expectedLines.length, actualLines.length);
		StringBuilder preview = new StringBuilder("First difference:");
		int shown = 0;
		for (int i = 0; i < max && shown < DIFF_PREVIEW_LINES; i++) {
			String expectedLine = i < expectedLines.length ? expectedLines[i] : "<end of file>";
			String actualLine = i < actualLines.length ? actualLines[i] : "<end of file>";
			if (!expectedLine.equals(actualLine)) {
				preview.append("\n  line ").append(i + 1);
				preview.append("\n  - committed: ").append(actualLine);
				preview.append("\n  + expected:  ").append(expectedLine);
				shown++;
			}
		}
		return preview.toString();
	}
}
