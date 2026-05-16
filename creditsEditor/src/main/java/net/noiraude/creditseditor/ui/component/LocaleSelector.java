package net.noiraude.creditseditor.ui.component;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapTiny;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.util.Locale;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.service.LangResolver;
import net.noiraude.creditseditor.ui.I18n;

import org.jetbrains.annotations.NotNull;

/** Combo box plus add and remove buttons for the active editing locale of the current session. */
public final class LocaleSelector extends JPanel {

    private final @NotNull DocumentBus bus;
    private final @NotNull JComboBox<String> combo = new JComboBox<>();
    private final @NotNull JButton addButton = new JButton("+");
    private final @NotNull JButton removeButton = new JButton("−");
    private boolean syncing;

    public LocaleSelector(@NotNull DocumentBus bus) {
        super(new BorderLayout(gapTiny, 0));
        this.bus = bus;
        addButton.setMargin(new Insets(0, gapTiny, 0, gapTiny));
        addButton.setToolTipText(I18n.get("action.add_locale"));
        addButton.addActionListener(e -> bus.fireAddLocaleRequested());

        removeButton.setMargin(new Insets(0, gapTiny, 0, gapTiny));
        removeButton.setToolTipText(I18n.get("action.remove_locale"));
        removeButton.addActionListener(e -> bus.fireRemoveLocaleRequested());

        combo.setRenderer(new LocaleLabelRenderer());
        combo.addActionListener(e -> {
            if (syncing) return;
            Object selected = combo.getSelectedItem();
            if (selected == null) return;
            bus.setActiveLocale(selected.toString());
        });

        JPanel buttons = new JPanel(new BorderLayout(gapTiny, 0));
        buttons.add(addButton, BorderLayout.WEST);
        buttons.add(removeButton, BorderLayout.EAST);

        add(combo, BorderLayout.CENTER);
        add(buttons, BorderLayout.EAST);

        bus.addListener(DocumentBus.TOPIC_SESSION, e -> rebuild());
        bus.addListener(DocumentBus.TOPIC_LOCALE, e -> rebuild());
        rebuild();
    }

    private void rebuild() {
        syncing = true;
        try {
            combo.removeAllItems();
            boolean hasSession = bus.hasSession();
            combo.setEnabled(hasSession);
            addButton.setEnabled(hasSession);
            removeButton.setEnabled(hasSession && !LangResolver.DEFAULT_LOCALE.equals(bus.activeLocale()));
            for (String locale : bus.availableLocales()) {
                combo.addItem(locale);
            }
            if (combo.getItemCount() > 0) {
                combo.setSelectedItem(bus.activeLocale());
            }
        } finally {
            syncing = false;
        }
    }

    private static final class LocaleLabelRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value == null) return this;
            String tag = value.toString();
            setText(renderLabel(tag));
            return this;
        }

        private static @NotNull String renderLabel(@NotNull String tag) {
            int underscore = tag.indexOf('_');
            String language = underscore < 0 ? tag : tag.substring(0, underscore);
            String displayLanguage = Locale.of(language)
                .getDisplayLanguage(Locale.getDefault());
            if (displayLanguage.isEmpty() || displayLanguage.equals(language)) return tag;
            return tag + " -- " + displayLanguage;
        }
    }
}
