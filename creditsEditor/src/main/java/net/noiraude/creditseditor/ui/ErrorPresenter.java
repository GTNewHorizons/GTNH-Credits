package net.noiraude.creditseditor.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonParseException;

/**
 * Presents exception-driven error dialogs with a user-friendly primary message and an optional
 * "Details..." toggle that reveals the underlying exception text.
 *
 * <p>
 * The mapping from exception type to friendly message is intentionally narrow: unknown types
 * fall back to a generic wording so no raw filesystem paths, class names, or stack frames leak
 * into the primary dialog line. Diagnostic detail is still reachable via the toggle.
 */
public final class ErrorPresenter {

    private ErrorPresenter() {}

    /**
     * Shows a modal error dialog for the given exception.
     *
     * @param parent parent component (may be {@code null})
     * @param title  dialog title
     * @param ex     the thrown exception
     */
    public static void show(@Nullable Component parent, @NotNull String title, @NotNull Throwable ex) {
        JPanel panel = new JPanel(new BorderLayout(0, ScaledMetrics.gapLarge));
        panel.add(new JLabel(friendlyMessage(ex)), BorderLayout.NORTH);

        JTextArea details = new JTextArea(10, 60);
        details.setEditable(false);
        details.setLineWrap(false);
        details.setText(detailsText(ex));
        details.setCaretPosition(0);

        JScrollPane detailsPane = new JScrollPane(details);
        detailsPane.setBorder(BorderFactory.createEmptyBorder());
        detailsPane.setVisible(false);
        panel.add(detailsPane, BorderLayout.CENTER);

        JButton toggle = new JButton("Show details...");
        toggle.addActionListener(e -> {
            boolean nowVisible = !detailsPane.isVisible();
            detailsPane.setVisible(nowVisible);
            toggle.setText(nowVisible ? "Hide details" : "Show details...");
            Window w = SwingUtilities.getWindowAncestor(panel);
            if (w != null) w.pack();
        });
        JPanel toggleRow = new JPanel(new BorderLayout());
        toggleRow.add(toggle, BorderLayout.WEST);
        panel.add(toggleRow, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(parent, panel, title, JOptionPane.ERROR_MESSAGE);
    }

    private static @NotNull String friendlyMessage(@NotNull Throwable ex) {
        Throwable root = rootCause(ex);
        if (root instanceof NoSuchFileException) return "The requested file does not exist.";
        if (root instanceof AccessDeniedException) return "Access to the file was denied.";
        if (root instanceof JsonParseException) return "The credits JSON file is not valid.";
        if (root instanceof IOException) return "A file access error occurred.";
        return "An unexpected error occurred.";
    }

    private static @NotNull Throwable rootCause(@NotNull Throwable ex) {
        Throwable cur = ex;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur;
    }

    private static @NotNull String detailsText(@NotNull Throwable ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
