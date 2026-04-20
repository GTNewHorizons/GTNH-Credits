package net.noiraude.creditseditor.ui.dialog;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import net.noiraude.creditseditor.ui.AppIcons;
import net.noiraude.creditseditor.ui.AppInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Modal "About" dialog displaying application metadata.
 *
 * <p>
 * Layout mirrors the typical IDE about window: the largest bundled icon on the left and
 * stacked metadata labels on the right. Closes on the Close button, the window close
 * button, or the Escape key.
 */
public final class AboutDialog extends JDialog {

    public AboutDialog(@Nullable Frame owner) {
        super(owner, "About " + AppInfo.name(), true);

        JPanel content = new JPanel(new BorderLayout(scaled(16), scaled(12)));
        content.setBorder(BorderFactory.createEmptyBorder(scaled(16), scaled(16), scaled(12), scaled(16)));

        content.add(buildIconPanel(), BorderLayout.WEST);
        content.add(buildInfoPanel(), BorderLayout.CENTER);
        content.add(buildButtonPanel(), BorderLayout.SOUTH);

        setContentPane(content);
        installCloseOnEscape();
        JButton defaultButton = findCloseButton(content);
        if (defaultButton != null) getRootPane().setDefaultButton(defaultButton);

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private static @NotNull JComponent buildIconPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        List<Image> images = AppIcons.load();
        if (!images.isEmpty()) {
            JLabel label = new JLabel(new ImageIcon(images.get(images.size() - 1)));
            wrap.add(label, BorderLayout.NORTH);
        }
        return wrap;
    }

    private static @NotNull JComponent buildInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel nameLabel = leftLabel(AppInfo.name());
        Font base = nameLabel.getFont();
        nameLabel.setFont(base.deriveFont(Font.BOLD, base.getSize2D() + scaled(6)));
        panel.add(nameLabel);

        panel.add(Box.createVerticalStrut(scaled(6)));
        panel.add(leftLabel("Version " + AppInfo.version()));

        String description = AppInfo.description();
        String license = AppInfo.license();
        if (!description.isEmpty() || !license.isEmpty()) {
            panel.add(Box.createVerticalStrut(scaled(10)));
            if (!description.isEmpty()) panel.add(leftLabel(description));
            if (!license.isEmpty()) panel.add(leftLabel(license));
        }

        String copyright = AppInfo.copyright();
        if (!copyright.isEmpty()) {
            panel.add(Box.createVerticalStrut(scaled(10)));
            panel.add(leftLabel(copyright));
        }

        return panel;
    }

    private @NotNull JComponent buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);
        return panel;
    }

    private static @NotNull JLabel leftLabel(@NotNull String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void installCloseOnEscape() {
        JRootPane rp = getRootPane();
        rp.registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private static @Nullable JButton findCloseButton(@NotNull Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton b) return b;
            if (c instanceof Container cc) {
                JButton found = findCloseButton(cc);
                if (found != null) return found;
            }
        }
        return null;
    }
}
