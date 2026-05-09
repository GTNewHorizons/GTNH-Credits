package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapMedium;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;

import java.awt.Component;
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
import net.noiraude.creditseditor.command.impl.DocumentEditCommand;
import net.noiraude.creditseditor.command.impl.EditFieldCommand;
import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.creditseditor.service.LangResolver;
import net.noiraude.creditseditor.ui.I18n;
import net.noiraude.creditseditor.ui.component.mc.LocalizedMcEditor;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.DocumentCategory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Form panel that displays and edits the fields of a single {@link DocumentCategory}.
 *
 * <p>
 * Subscribes to {@link DocumentBus#TOPIC_CATEGORY} to reload the current category when it
 * is the one that changed and to {@link DocumentBus#TOPIC_LOCALE} to rebuild the display
 * name from the active locale. Removal of the current category is detected by the owning
 * {@code DetailPanel}, which calls {@link #clear()} and switches the card away from this
 * view; this view therefore does not listen for {@code TOPIC_CATEGORIES}. Lang-derived
 * fields (display name, description) are read from and written to the active locale's
 * {@link LangDocument}, falling back to the default locale via the resolver chain when the
 * active locale has no value. Class checkboxes are delegated to
 * {@link CategoryClassSelector}; the description row is delegated to
 * {@link CategoryDescriptionSection}.
 */
public final class CategoryDetailView extends DetailView<DocumentCategory> {

    private final @NotNull DocumentBus bus;

    private final @NotNull JTextField idField = new JTextField();
    private final @NotNull JLabel langKeyLabel = new JLabel();
    private final @NotNull LocalizedMcEditor displayNameEditor = new LocalizedMcEditor(false);
    private final @NotNull CategoryClassSelector classSelector = new CategoryClassSelector();
    private final @NotNull CategoryDescriptionSection descriptionSection = new CategoryDescriptionSection();
    private final @NotNull Component spacer = Box.createVerticalGlue();

    public CategoryDetailView(@NotNull DocumentBus bus, @NotNull CommandExecutor onCommand) {
        super(onCommand);
        this.bus = bus;
        idField.setEditable(false);
        idField.setBackground(UIManager.getColor("Panel.background"));
        langKeyLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        displayNameEditor.setEnglishValueSupplier(this::englishDisplayName);
        displayNameEditor.setActiveLocale(bus.activeLocale());
        buildLayout();
        wireEvents();
        updateDescriptionVisibility();

        // The display-name and description editors stay in sync with their underlying Swing
        // documents (undoable edits mutate the document directly); calling setText on them
        // would reset the caret while the user is typing. Only the class checkboxes need
        // event-driven updates.
        bus.addListener(
            DocumentBus.TOPIC_CATEGORY,
            e -> { if (current != null && e.getNewValue() == current) reloadClassCheckboxes(); });
        bus.addListener(DocumentBus.TOPIC_LOCALE, e -> onLocaleChanged());
    }

    private void buildLayout() {
        GridBagConstraints label = labelConstraints();
        GridBagConstraints field = fieldConstraints();

        // Row 0: ID
        label.gridy = 0;
        add(new JLabel(I18n.get("view.category.id.label")), label);
        field.gridy = 0;
        add(idField, field);

        // Row 1: Lang key (derived, read-only)
        label.gridy = 1;
        add(new JLabel(I18n.get("view.category.lang_key.label")), label);
        field.gridy = 1;
        add(langKeyLabel, field);

        // Row 2: Display name
        label.gridy = 2;
        add(new JLabel(I18n.get("view.category.display_name.label")), label);
        field.gridy = 2;
        add(displayNameEditor, field);

        // Row 3: Classes
        label.gridy = 3;
        add(new JLabel(I18n.get("view.category.classes.label")), label);
        field.gridy = 3;
        add(classSelector, field);

        // Row 4: Details text (visible only when "detail" class is checked)
        label.gridy = 4;
        label.anchor = GridBagConstraints.NORTHWEST;
        label.insets = new Insets(gapMedium, gapMedium, gapSmall, gapSmall);
        add(descriptionSection.label(), label);
        field.gridy = 4;
        field.fill = GridBagConstraints.BOTH;
        field.weighty = 0;
        add(descriptionSection.editor(), field);

        // Row 5: Spacer always presents; absorbs extra vertical space when description is
        // hidden so rows stay top-aligned. Weight is swapped to the description editor when
        // the description row is visible, so the editor expands to fill the panel instead.
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
        displayNameEditor.addTextChangeListener(value -> {
            if (loading || current == null) return;
            String key = "credits.category." + KeySanitizer.sanitize(current.id);
            LangDocument target = bus.langDoc(bus.activeLocale());
            if (target == null) return;
            if (value.isEmpty()) target.remove(key);
            else target.set(key, value);
            bus.fireLangChanged(key);
        });
        displayNameEditor.addUndoableEditListener(e -> {
            if (!loading && current != null) {
                onCommand.execute(new DocumentEditCommand(I18n.get("command.edit.display_name"), e.getEdit()));
            }
        });
    }

    private void wireDescriptionEvents() {
        descriptionSection.addTextPropertyChangeListener(e -> {
            if (loading || current == null) return;
            String key = "credits.category." + KeySanitizer.sanitize(current.id) + ".detail";
            String value = (String) e.getNewValue();
            if (value.isEmpty()) bus.langDoc()
                .remove(key);
            else bus.langDoc()
                .set(key, value);
            bus.fireLangChanged(key);
        });
        descriptionSection.addUndoableEditListener(e -> {
            if (!loading && current != null) {
                onCommand.execute(new DocumentEditCommand(I18n.get("command.edit.details"), e.getEdit()));
            }
        });
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
    }

    /**
     * Populates all fields from {@code cat} without firing any commands.
     * Call after any external model change: initial load, undo, or redo.
     */
    public void load(@NotNull DocumentCategory cat) {
        current = cat;
        loading = true;
        try {
            String key = "credits.category." + KeySanitizer.sanitize(cat.id);
            idField.setText(cat.id);
            langKeyLabel.setText(key);
            displayNameEditor.setActiveLocale(bus.activeLocale());
            displayNameEditor.setText(resolveDisplayName(key));
            String detail = bus.langDoc()
                .get(key + ".detail");
            descriptionSection.setText(detail != null ? detail : "");
            classSelector.setClasses(cat.classes);
        } finally {
            loading = false;
        }
        updateDescriptionVisibility();
    }

    /**
     * Returns the active locale's value for {@code key}, falling back to the default-locale
     * value when the active locale has no non-empty entry. An empty string represents both
     * "absent everywhere" and "intentionally cleared in the default locale" so the editor
     * shows nothing for the user to translate from.
     */
    private @NotNull String resolveDisplayName(@NotNull String key) {
        String activeLocale = bus.activeLocale();
        String active = lookup(activeLocale, key);
        if (active != null) return active;
        if (LangResolver.DEFAULT_LOCALE.equals(activeLocale)) return "";
        String fallback = lookup(LangResolver.DEFAULT_LOCALE, key);
        return fallback != null ? fallback : "";
    }

    private @Nullable String lookup(@NotNull String locale, @NotNull String key) {
        LangDocument doc = bus.langDoc(locale);
        if (doc == null) return null;
        String value = doc.get(key);
        return (value == null || value.isEmpty()) ? null : value;
    }

    private @NotNull Optional<String> englishDisplayName() {
        if (current == null) return Optional.empty();
        String key = "credits.category." + KeySanitizer.sanitize(current.id);
        return Optional.ofNullable(lookup(LangResolver.DEFAULT_LOCALE, key));
    }

    private void onLocaleChanged() {
        String activeLocale = bus.activeLocale();
        displayNameEditor.setActiveLocale(activeLocale);
        if (current == null) return;
        String key = "credits.category." + KeySanitizer.sanitize(current.id);
        loading = true;
        try {
            displayNameEditor.setText(resolveDisplayName(key));
        } finally {
            loading = false;
        }
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

        // Transfer weighty to whichever component should absorb extra vertical space.
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
