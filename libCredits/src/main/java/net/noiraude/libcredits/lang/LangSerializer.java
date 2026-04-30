package net.noiraude.libcredits.lang;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Serializes a {@link LangDocument} to the Minecraft {@code .lang} file format.
 *
 * <p>
 * The output is UTF-8 text. Foreign lines are written in their original order.
 * Deleted key-value lines are skipped. Pending inserts are appended after all
 * original content, preceded by a blank line if the file does not already end
 * with one. Consecutive blank lines in the output are collapsed to a single
 * blank line.
 */
public final class LangSerializer {

    private LangSerializer() {}

    /**
     * Writes {@code doc} as a {@code .lang} file to {@code out}.
     * The caller is responsible for closing the stream.
     *
     * @throws IOException if the stream cannot be written
     */
    public static void write(LangDocument doc, OutputStream out) throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean lastWasBlank = false;

        for (LangDocument.Line line : doc.lines()) {
            if (line instanceof LangDocument.KeyValueLine && ((LangDocument.KeyValueLine) line).deleted) continue;
            if (line instanceof LangDocument.BlankLine) {
                if (!lastWasBlank) {
                    sb.append('\n');
                    lastWasBlank = true;
                }
            } else {
                sb.append(line.toFileString())
                    .append('\n');
                lastWasBlank = false;
            }
        }

        if (
            !doc.pendingInserts()
                .isEmpty()
        ) {
            if (!lastWasBlank && sb.length() > 0) sb.append('\n');
            for (Map.Entry<String, String> entry : doc.pendingInserts()
                .entrySet()) {
                sb.append(entry.getKey())
                    .append('=')
                    .append(entry.getValue())
                    .append('\n');
            }
        }

        out.write(
            sb.toString()
                .getBytes(StandardCharsets.UTF_8));
    }
}
