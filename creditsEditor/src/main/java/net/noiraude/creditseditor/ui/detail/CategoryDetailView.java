package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapMedium;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.LangFieldWriter;
import net.noiraude.creditseditor.command.impl.EditCategoryDetailsCommand;
import net.noiraude.creditseditor.command.impl.EditCategoryDisplayNameCommand;
import net.noiraude.creditseditor.command.impl.EditFieldCommand;
import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.creditseditor.service.LangResolver;
import net.noiraude.creditseditor.ui.I18n;
import net.noiraude.creditseditor.ui.component.mc.LocalizedMcEditor;
import net.noiraude.libcredits.lang.DetailLangKey;
import net.noiraude.libcredits.lang.LangKey;
import net.noiraude.libcredits.model.DocumentCategory;

import org.jetbrains.annotations.NotNull;

/**
 * Form panel that displays and edits the fields of a single {@link DocumentCategory}.
 *
 * <p>
 * Subscribes to {@link DocumentBus#TOPIC_CATEGORY} to reload the current category when it
 * is the one that changed, to {@link DocumentBus#TOPIC_LOCALE} to rebuild the display
 * name and description from the active locale, and to {@link DocumentBus#TOPIC_LANG} to
 * refresh the rendered text after an undo or redo writes to the active locale's lang
 * document. Removal of the current category is detected by the owning
 * {@code DetailPanel}, which calls {@link #clear()} and switches the card away from this
 * view; this view therefore does not listen for {@code TOPIC_CATEGORIES}.
 */
public final class CategoryDetailView extends DetailView<DocumentCategory> {

    private final @NotNull DocumentBus bus;

    private final @NotNull JTextField idField = new JTextField();
    private final @NotNull JLabel langKeyLabel = new JLabel();
    private final @NotNull LocalizedMcEditor displayNameEditor = new LocalizedMcEditor(false);
    private final @NotNull CategoryClassSelector classSelector = new CategoryClassSelector();
    private final @NotNull CategoryDescriptionSection descriptionSection = new CategoryDescriptionSection();
    private final @NotNull Component spacer = Box.createVerticalGlue();

    private @NotNull String shadowDisplayName = "";
    private @NotNull String shadowDescription = "";
    private boolean displayNameCommitPending;
    private boolean descriptionCommitPending;

    private @NotNull Optional<CategoryKeys> categoryKeys = Optional.empty();

    private record CategoryKeys(@NotNull LangKey name, @NotNull DetailLangKey detail) {}

    public CategoryDetailView(@NotNull DocumentBus bus, @NotNull CommandExecutor onCommand) {
        super(onCommand);
        this.bus = bus;
        idField.setEditable(false);
        idField.setBackground(UIManager.getColor("Panel.background"));
        langKeyLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        displayNameEditor.setEnglishValueSupplier(this::englishDisplayName);
        displayNameEditor.setActiveLocale(bus.activeLocale());
        descriptionSection.setEnglishValueSupplier(this::englishDescription);
        descriptionSection.setActiveLocale(bus.activeLocale());
        buildLayout();
        wireEvents();
        updateDescriptionVisibility();

        bus.addListener(
            DocumentBus.TOPIC_CATEGORY,
            e -> { if (current != null && e.getNewValue() == current) reloadClassCheckboxes(); });
        bus.addListener(DocumentBus.TOPIC_LOCALE, e -> onLocaleChanged());
        bus.addListener(DocumentBus.TOPIC_LANG, e -> {
            Object nv = e.getNewValue();
            if (nv != null) onLangChanged(nv.toString());
        });
    }

    private void buildLayout() {
        GridBagConstraints label = labelConstraints();
        GridBagConstraints field = fieldConstraints();

        label.gridy = 0;
        add(new JLabel(I18n.get("view.category.id.label")), label);
        field.gridy = 0;
        add(idField, field);

        label.gridy = 1;
        add(new JLabel(I18n.get("view.category.lang_key.label")), label);
        field.gridy = 1;
        add(langKeyLabel, field);

        label.gridy = 2;
        add(new JLabel(I18n.get("view.category.display_name.label")), label);
        field.gridy = 2;
        add(displayNameEditor, field);

        label.gridy = 3;
        add(new JLabel(I18n.get("view.category.classes.label")), label);
        field.gridy = 3;
        add(classSelector, field);

        label.gridy = 4;
        label.anchor = GridBagConstraints.NORTHWEST;
        label.insets = new Insets(gapMedium, gapMedium, gapSmall, gapSmall);
        add(descriptionSection.label(), label);
        field.gridy = 4;
        field.fill = GridBagConstraints.BOTH;
        field.weighty = 0;
        add(descriptionSection.editor(), field);

        GridBagConstraints spacerGbc = new GridBagConstraints();
        spacerGbc.gridy = 5;
        spacerGbc.gridx = 0;
        spacerGbc.gridwidth = 2;
        spacerGbc.weighty = 1.0;
        spacerGbc.fill = GridBagConstraints.VERTICAL;
        add(spacer, spacerGbc);
    }

    private void wireEvents() {
        wireDisplayNameEvents();
        wireDescriptionEvents();
        classSelector.addClassToggleListener((cls, selected) -> {
            onClassToggle(cls, selected);
            if (CategoryClassSelector.CLASS_DETAIL.equals(cls)) updateDescriptionVisibility();
        });
    }

    private void wireDisplayNameEvents() {
        displayNameEditor.addTextChangeListener(text -> {
            if (loading || displayNameCommitPending) return;
            displayNameCommitPending = true;
            EventQueue.invokeLater(() -> {
                displayNameCommitPending = false;
                if (loading) return;
                commitDisplayName();
            });
        });
    }

    private void wireDescriptionEvents() {
        descriptionSection.addTextChangeListener(text -> {
            if (loading || descriptionCommitPending) return;
            descriptionCommitPending = true;
            EventQueue.invokeLater(() -> {
                descriptionCommitPending = false;
                if (loading) return;
                commitDescription();
            });
        });
    }

    private void commitDisplayName() {
        categoryKeys.ifPresent(
            keys -> bus.langDoc(bus.activeLocale())
                .ifPresent(target -> {
                    String newValue = displayNameEditor.getText();
                    String oldValue = shadowDisplayName;
                    if (oldValue.equals(newValue)) return;
                    int caretAfter = displayNameEditor.getCaretPosition();
                    shadowDisplayName = newValue;
                    loading = true;
                    try {
                        onCommand.execute(
                            EditCategoryDisplayNameCommand.create(
                                LangFieldWriter.ofBus(bus, target, keys.name()),
                                oldValue,
                                newValue,
                                displayNameEditor::setCaretPosition,
                                caretAfter));
                    } finally {
                        loading = false;
                    }
                }));
    }

    private void commitDescription() {
        categoryKeys.ifPresent(
            keys -> bus.langDoc(bus.activeLocale())
                .ifPresent(target -> {
                    String newValue = descriptionSection.getText();
                    String oldValue = shadowDescription;
                    if (oldValue.equals(newValue)) return;
                    int caretAfter = descriptionSection.editor()
                        .getCaretPosition();
                    shadowDescription = newValue;
                    loading = true;
                    try {
                        onCommand.execute(
                            EditCategoryDetailsCommand.create(
                                LangFieldWriter.ofBus(bus, target, keys.detail()),
                                oldValue,
                                newValue,
                                descriptionSection.editor()::setCaretPosition,
                                caretAfter));
                    } finally {
                        loading = false;
                    }
                }));
    }

    private void reloadClassCheckboxes() {
        if (current == null) return;
        loading = true;
        try {
            classSelector.setClasses(current.classes);
        } finally {
            loading = false;
        }
        updateDescriptionVisibility();
    }

    /**
     * Clears the current subject so stale bus events targeted at the previously displayed
     * category are ignored. Called by the owning panel when switching away from this view.
     */
    public void clear() {
        current = null;
        categoryKeys = Optional.empty();
        shadowDisplayName = "";
        shadowDescription = "";
    }

    /**
     * Populates all fields from {@code cat} without firing any commands.
     * Call after any external model change: initial load, undo, or redo.
     */
    public void load(@NotNull DocumentCategory cat) {
        current = cat;
        String prefix = categoryPrefix(cat);
        CategoryKeys keys = new CategoryKeys(new LangKey(prefix), new DetailLangKey(prefix));
        categoryKeys = Optional.of(keys);
        loading = true;
        try {
            idField.setText(cat.id);
            langKeyLabel.setText(prefix);
            displayNameEditor.setActiveLocale(bus.activeLocale());
            String displayName = resolveLangValue(keys.name());
            displayNameEditor.setText(displayName);
            shadowDisplayName = displayName;
            descriptionSection.setActiveLocale(bus.activeLocale());
            String description = resolveLangValue(keys.detail());
            descriptionSection.setText(description);
            shadowDescription = description;
            classSelector.setClasses(cat.classes);
        } finally {
            loading = false;
        }
        updateDescriptionVisibility();
    }

    /**
     * Returns the active locale's value for {@code key}, falling back to the default-locale
     * value when the active locale has no non-empty entry. An empty string represents both
     * "absent everywhere" and "intentionally cleared in the default locale".
     */
    private @NotNull String resolveLangValue(@NotNull LangKey key) {
        String activeLocale = bus.activeLocale();
        Optional<String> active = lookup(activeLocale, key);
        if (active.isPresent()) return active.get();
        if (LangResolver.DEFAULT_LOCALE.equals(activeLocale)) return "";
        return lookup(LangResolver.DEFAULT_LOCALE, key).orElse("");
    }

    private @NotNull Optional<String> lookup(@NotNull String locale, @NotNull LangKey key) {
        return bus.langDoc(locale)
            .flatMap(key::read)
            .filter(v -> !v.isEmpty());
    }

    private @NotNull Optional<String> englishDisplayName() {
        return categoryKeys.flatMap(keys -> lookup(LangResolver.DEFAULT_LOCALE, keys.name()));
    }

    private @NotNull Optional<String> englishDescription() {
        return categoryKeys.flatMap(keys -> lookup(LangResolver.DEFAULT_LOCALE, keys.detail()));
    }

    private void onLocaleChanged() {
        String activeLocale = bus.activeLocale();
        displayNameEditor.setActiveLocale(activeLocale);
        descriptionSection.setActiveLocale(activeLocale);
        categoryKeys.ifPresent(keys -> {
            loading = true;
            try {
                String displayName = resolveLangValue(keys.name());
                displayNameEditor.setText(displayName);
                shadowDisplayName = displayName;
                String description = resolveLangValue(keys.detail());
                descriptionSection.setText(description);
                shadowDescription = description;
            } finally {
                loading = false;
            }
        });
    }

    private void onLangChanged(@NotNull String changedKey) {
        categoryKeys.ifPresent(keys -> {
            if (
                changedKey.equals(
                    keys.name()
                        .key())
            ) refreshDisplayName(keys.name());
            else if (
                changedKey.equals(
                    keys.detail()
                        .key())
            ) refreshDescription(keys.detail());
        });
    }

    private void refreshDisplayName(@NotNull LangKey nameKey) {
        String resolved = resolveLangValue(nameKey);
        if (resolved.equals(displayNameEditor.getText())) {
            shadowDisplayName = resolved;
            return;
        }
        loading = true;
        try {
            displayNameEditor.setText(resolved);
            shadowDisplayName = resolved;
        } finally {
            loading = false;
        }
    }

    private void refreshDescription(@NotNull DetailLangKey detailKey) {
        String resolved = resolveLangValue(detailKey);
        if (resolved.equals(descriptionSection.getText())) {
            shadowDescription = resolved;
            return;
        }
        loading = true;
        try {
            descriptionSection.setText(resolved);
            shadowDescription = resolved;
        } finally {
            loading = false;
        }
    }

    private static @NotNull String categoryPrefix(@NotNull DocumentCategory cat) {
        return "credits.category." + KeySanitizer.sanitize(cat.id);
    }

    private void onClassToggle(@NotNull String cls, boolean selected) {
        if (loading || current == null) return;
        DocumentCategory cat = current;
        Set<String> newSet = new LinkedHashSet<>(cat.classes);
        if (selected) newSet.add(cls);
        else newSet.remove(cls);
        if (newSet.equals(cat.classes)) return;
        onCommand.execute(new EditFieldCommand<>(I18n.get("command.edit.classes"), () -> cat.classes, v -> {
            cat.classes = v;
            bus.fireCategoryChanged(cat);
        }, newSet));
    }

    private void updateDescriptionVisibility() {
        boolean visible = classSelector.isDetailSelected();
        descriptionSection.setVisible(visible);

        GridBagLayout gbl = (GridBagLayout) getLayout();
        GridBagConstraints descGbc = gbl.getConstraints(descriptionSection.editor());
        descGbc.weighty = visible ? 1.0 : 0;
        gbl.setConstraints(descriptionSection.editor(), descGbc);
        GridBagConstraints spacerGbc = gbl.getConstraints(spacer);
        spacerGbc.weighty = visible ? 0 : 1.0;
        gbl.setConstraints(spacer, spacerGbc);

        revalidate();
        repaint();
    }
}
