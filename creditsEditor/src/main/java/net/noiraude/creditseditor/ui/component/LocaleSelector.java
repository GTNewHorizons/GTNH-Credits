package net.noiraude.creditseditor.ui.component;

import java.awt.Component;
import java.util.Locale;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;

/**
 * Combo box for the active editing locale.
 */
public final class LocaleSelector extends JComboBox<String> {

    private final @NotNull DocumentBus bus;
    private boolean syncing;

    public LocaleSelector(@NotNull DocumentBus bus) {
        this.bus = bus;
        setRenderer(new LocaleLabelRenderer());
        addActionListener(e -> {
            if (syncing) return;
            Object selected = getSelectedItem();
            if (selected == null) return;
            bus.setActiveLocale(selected.toString());
        });
        bus.addListener(DocumentBus.TOPIC_SESSION, e -> rebuild());
        bus.addListener(DocumentBus.TOPIC_LOCALE, e -> {
            Object nv = e.getNewValue();
            if (nv == null) return;
            syncSelection(nv.toString());
        });
        rebuild();
    }

    private void rebuild() {
        syncing = true;
        try {
            removeAllItems();
            for (String locale : bus.availableLocales()) {
                addItem(locale);
            }
            if (getItemCount() > 0) {
                setSelectedItem(bus.activeLocale());
            }
        } finally {
            syncing = false;
        }
    }

    private void syncSelection(@NotNull String locale) {
        Object current = getSelectedItem();
        if (current != null && locale.equals(current.toString())) return;
        syncing = true;
        try {
            setSelectedItem(locale);
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
