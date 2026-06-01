package net.noiraude.creditseditor.ui;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapMedium;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.ui.component.LocaleControls;

import org.jetbrains.annotations.NotNull;

/** Top toolbar of the editor window: surfaces the file/edit actions and the active editing locale. */
final class EditorToolBar extends JPanel {

    EditorToolBar(@NotNull DocumentBus bus, @NotNull EditorActions actions) {
        super(new BorderLayout());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, gapSmall, gapSmall));
        left.add(iconButton(actions.open, ActionIcons.open()));
        left.add(iconButton(actions.save, ActionIcons.save()));
        left.add(iconButton(actions.saveAs, ActionIcons.saveAs()));
        left.add(Box.createHorizontalStrut(gapMedium));
        left.add(iconButton(actions.undo, ActionIcons.undo()));
        left.add(iconButton(actions.redo, ActionIcons.redo()));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, gapSmall, gapSmall));
        right.add(new LocaleControls(bus));

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
    }

    private static @NotNull JButton iconButton(@NotNull Action action, @NotNull Icon icon) {
        JButton btn = new JButton(action);
        btn.setHideActionText(true);
        btn.setIcon(icon);
        return btn;
    }
}
