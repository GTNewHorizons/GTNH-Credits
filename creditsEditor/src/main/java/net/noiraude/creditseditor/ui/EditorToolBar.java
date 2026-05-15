package net.noiraude.creditseditor.ui;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;
import static net.noiraude.creditseditor.ui.ScaledMetrics.localeSelectorWidth;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.ui.component.LocaleSelector;

import org.jetbrains.annotations.NotNull;

/**
 * Top toolbar of the editor window: surfaces the Save action and the active editing locale.
 */
final class EditorToolBar extends JPanel {

    EditorToolBar(@NotNull DocumentBus bus, @NotNull Action saveAction) {
        super(new FlowLayout(FlowLayout.TRAILING, gapSmall, gapSmall));
        JButton saveButton = new JButton(saveAction);
        LocaleSelector localeSelector = new LocaleSelector(bus);
        localeSelector.setPreferredSize(new Dimension(localeSelectorWidth, localeSelector.getPreferredSize().height));
        add(saveButton);
        add(localeSelector);
    }
}
