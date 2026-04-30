package net.noiraude.creditseditor.ui.dialog;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapHuge;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapXXLarge;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import net.noiraude.creditseditor.ui.I18n;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Modal dialog listing the keyboard shortcuts available in the editor.
 *
 * <p>
 * The shortcut catalogue is hand-maintained against the accelerators wired in
 * {@link net.noiraude.creditseditor.ui.EditorMenuBar} and the standard text-editing keys
 * provided by Swing; new accelerators must be added here so users can discover them.
 *
 * <p>
 * Rows are rendered as a single HTML table inside one {@link JLabel}, which gives consistent
 * column alignment without per-row layout bookkeeping.
 */
public final class ShortcutsDialog extends JDialog {

    private record Shortcut(@NotNull String keys, @NotNull String description) {}

    private record Section(@NotNull String title, @NotNull List<Shortcut> shortcuts) {}

    public ShortcutsDialog(@Nullable Frame owner) {
        super(owner, I18n.get("dialog.shortcuts.title"), true);

        JPanel content = new JPanel(new BorderLayout(0, gapXXLarge));
        content.setBorder(BorderFactory.createEmptyBorder(gapHuge, gapHuge, gapXXLarge, gapHuge));
        content.add(buildShortcutsLabel(), BorderLayout.CENTER);
        content.add(buildButtonPanel(), BorderLayout.SOUTH);

        setContentPane(content);
        installCloseOnEscape();

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private static @NotNull JComponent buildShortcutsLabel() {
        StringBuilder html = new StringBuilder("<html><table cellpadding='2' cellspacing='0'>");
        boolean firstSection = true;
        for (Section section : sections()) {
            if (!firstSection) {
                html.append("<tr><td colspan='2'>&nbsp;</td></tr>");
            }
            firstSection = false;
            html.append("<tr><th colspan='2' align='left'>")
                .append(escape(section.title))
                .append("</th></tr>");
            for (Shortcut s : section.shortcuts) {
                html.append("<tr><td><tt>")
                    .append(escape(s.keys))
                    .append("</tt></td>");
                html.append("<td>&nbsp;&nbsp;")
                    .append(escape(s.description))
                    .append("</td></tr>");
            }
        }
        html.append("</table></html>");
        return new JLabel(html.toString());
    }

    private static @NotNull String escape(@NotNull String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private static @NotNull List<Section> sections() {
        return List.of(
            new Section(
                I18n.get("dialog.shortcuts.section.file"),
                List.of(
                    new Shortcut("Ctrl+O", I18n.get("dialog.shortcuts.file.open")),
                    new Shortcut("Ctrl+S", I18n.get("dialog.shortcuts.file.save")),
                    new Shortcut("Ctrl+Shift+S", I18n.get("dialog.shortcuts.file.save_as")))),
            new Section(
                I18n.get("dialog.shortcuts.section.edit"),
                List.of(
                    new Shortcut("Ctrl+Z", I18n.get("dialog.shortcuts.edit.undo")),
                    new Shortcut("Ctrl+Shift+Z", I18n.get("dialog.shortcuts.edit.redo")),
                    new Shortcut("Ctrl+X", I18n.get("dialog.shortcuts.edit.cut")),
                    new Shortcut("Ctrl+C", I18n.get("dialog.shortcuts.edit.copy")),
                    new Shortcut("Ctrl+V", I18n.get("dialog.shortcuts.edit.paste")),
                    new Shortcut("Ctrl+A", I18n.get("dialog.shortcuts.edit.select_all")))));
    }

    private @NotNull JComponent buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton closeButton = new JButton(I18n.get("button.close"));
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);
        getRootPane().setDefaultButton(closeButton);
        return panel;
    }

    private void installCloseOnEscape() {
        JRootPane rp = getRootPane();
        rp.registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
}
