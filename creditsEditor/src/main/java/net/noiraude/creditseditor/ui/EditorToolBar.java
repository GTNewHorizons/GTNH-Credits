package net.noiraude.creditseditor.ui;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapMedium;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;
import static net.noiraude.creditseditor.ui.ScaledMetrics.localeSelectorWidth;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.ui.component.LocaleSelector;

import org.jetbrains.annotations.NotNull;

/** Top toolbar of the editor window: surfaces the file/edit actions and the active editing locale. */
final class EditorToolBar extends JPanel {

    EditorToolBar(@NotNull DocumentBus bus, @NotNull EditorActions actions) {
        super(new BorderLayout());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, gapSmall, gapSmall));
        left.add(new JButton(actions.open));
        left.add(new JButton(actions.save));
        left.add(new JButton(actions.saveAs));
        left.add(Box.createHorizontalStrut(gapMedium));
        left.add(staticTextButton(actions.undo, I18n.get("action.undo")));
        left.add(staticTextButton(actions.redo, I18n.get("action.redo")));

        LocaleSelector localeSelector = new LocaleSelector(bus);
        localeSelector.setPreferredSize(new Dimension(localeSelectorWidth, localeSelector.getPreferredSize().height));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, gapSmall, gapSmall));
        right.add(localeSelector);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
    }

    private static @NotNull JButton staticTextButton(@NotNull Action action, @NotNull String label) {
        JButton btn = new JButton(action);
        btn.setHideActionText(true);
        btn.setText(label);
        return btn;
    }
}
