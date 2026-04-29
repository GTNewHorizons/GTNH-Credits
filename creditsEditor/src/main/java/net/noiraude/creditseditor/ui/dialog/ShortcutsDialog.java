package net.noiraude.creditseditor.ui.dialog;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapHuge;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapLarge;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapMedium;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapXXLarge;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.UIManager;

import net.noiraude.creditseditor.ui.I18n;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Modal dialog listing the keyboard shortcuts available in the editor.
 *
 * <p>
 * The shortcut catalogue is hand-maintained against the actual accelerators wired in
 * {@link net.noiraude.creditseditor.ui.EditorMenuBar} and the editing keys provided by
 * Swing's standard text actions; new accelerators must be added here so users can discover
 * them. Closes on the Close button, the window close icon, or the Escape key.
 */
public final class ShortcutsDialog extends JDialog {

    private record Shortcut(@NotNull String keys, @NotNull String description) {

    }

    private record Section(@NotNull String title, @NotNull List<Shortcut> shortcuts) {

    }

    public ShortcutsDialog(@Nullable Frame owner) {
        super(owner, I18n.get("dialog.shortcuts.title"), true);

        JPanel content = new JPanel(new BorderLayout(0, gapXXLarge));
        content.setBorder(BorderFactory.createEmptyBorder(gapHuge, gapHuge, gapXXLarge, gapHuge));
        content.add(buildShortcutsPanel(), BorderLayout.CENTER);
        content.add(buildButtonPanel(), BorderLayout.SOUTH);

        setContentPane(content);
        installCloseOnEscape();
        getRootPane().setDefaultButton(findCloseButton(content));

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private @NotNull JComponent buildShortcutsPanel() {
        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, gapSmall, gapLarge);

        for (Section section : sections()) {
            gbc.gridwidth = 2;
            gbc.insets = new Insets(gapMedium, 0, gapSmall, 0);
            JLabel header = new JLabel(section.title);
            Font base = header.getFont();
            header.setFont(base.deriveFont(Font.BOLD));
            grid.add(header, gbc);
            gbc.gridy++;
            gbc.gridwidth = 1;

            for (Shortcut s : section.shortcuts) {
                gbc.gridx = 0;
                gbc.insets = new Insets(0, gapLarge, gapSmall, gapMedium);
                JLabel keysLabel = new JLabel(s.keys);
                keysLabel.setFont(UIManager.getFont("Label.font"));
                grid.add(keysLabel, gbc);
                gbc.gridx = 1;
                gbc.insets = new Insets(0, 0, gapSmall, 0);
                grid.add(new JLabel(s.description), gbc);
                gbc.gridy++;
            }
        }
        return grid;
    }

    private static @NotNull List<Section> sections() {
        return List.of(
            new Section(
                I18n.get("dialog.shortcuts.section.file"),
                List.of(
                    new Shortcut("Ctrl+O", I18n.get("dialog.shortcuts.file.open")),
                    new Shortcut("Ctrl+S", I18n.get("dialog.shortcuts.file.save")))),
            new Section(
                I18n.get("dialog.shortcuts.section.edit"),
                List.of(
                    new Shortcut("Ctrl+Z", I18n.get("dialog.shortcuts.edit.undo")),
                    new Shortcut("Ctrl+Shift+Z", I18n.get("dialog.shortcuts.edit.redo")),
                    new Shortcut("Ctrl+X", I18n.get("dialog.shortcuts.edit.cut")),
                    new Shortcut("Ctrl+C", I18n.get("dialog.shortcuts.edit.copy")),
                    new Shortcut("Ctrl+V", I18n.get("dialog.shortcuts.edit.paste")),
                    new Shortcut("Ctrl+A", I18n.get("dialog.shortcuts.edit.select_all")))),
            new Section(
                I18n.get("dialog.shortcuts.section.format"),
                List.of(
                    new Shortcut(
                        I18n.get("dialog.shortcuts.format.toolbar.keys"),
                        I18n.get("dialog.shortcuts.format.toolbar.desc")))));
    }

    private @NotNull JComponent buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton closeButton = new JButton(I18n.get("button.close"));
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);
        return panel;
    }

    private void installCloseOnEscape() {
        JRootPane rp = getRootPane();
        rp.registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private static @Nullable JButton findCloseButton(@NotNull JComponent root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton b) return b;
            if (c instanceof JComponent cc) {
                JButton found = findCloseButton(cc);
                if (found != null) return found;
            }
        }
        return null;
    }
}
