package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.*;

import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.impl.DocumentEditCommand;
import net.noiraude.creditseditor.command.impl.EditFieldCommand;
import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.creditseditor.ui.component.MinecraftTextAreaEditor;
import net.noiraude.creditseditor.ui.component.MinecraftTextEditor;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.DocumentCategory;

/**
 * Form panel that displays and edits the fields of a single {@link DocumentCategory}.
 *
 * <p>
 * Lang-derived fields (display name, description) are read from and written to the
 * {@link LangDocument} directly via {@link #setContext(LangDocument)}, so no synchronisation
 * step is needed. Call {@link #load(DocumentCategory)} whenever the active category changes
 * or the document has been refreshed by an undo/redo.
 */
public final class CategoryDetailView extends DetailView<DocumentCategory> {

    private static final String CLASS_PERSON = "person";
    private static final String CLASS_ROLE = "role";
    private static final String CLASS_DETAIL = "detail";

    private LangDocument langDoc;

    private final JTextField idField = new JTextField();
    private final JLabel langKeyLabel = new JLabel();
    private final MinecraftTextEditor displayNameEditor = new MinecraftTextEditor();
    private final JCheckBox classPerson = new JCheckBox("person");
    private final JCheckBox classRole = new JCheckBox("role");
    private final JCheckBox classDetail = new JCheckBox("detail");
    private final JLabel descriptionLabel = new JLabel("Details:");
    private final MinecraftTextAreaEditor descriptionEditor = new MinecraftTextAreaEditor();
    private final java.awt.Component spacer = Box.createVerticalGlue();

    public CategoryDetailView(CommandExecutor onCommand) {
        super(onCommand);
        idField.setEditable(false);
        idField.setBackground(UIManager.getColor("Panel.background"));
        langKeyLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        buildLayout();
        wireEvents();
        updateDescriptionVisibility();
    }

    private void buildLayout() {
        GridBagConstraints label = labelConstraints();
        GridBagConstraints field = fieldConstraints();

        // Row 0: ID
        label.gridy = 0;
        add(new JLabel("ID:"), label);
        field.gridy = 0;
        add(idField, field);

        // Row 1: Lang key (derived, read-only)
        label.gridy = 1;
        add(new JLabel("Lang key:"), label);
        field.gridy = 1;
        add(langKeyLabel, field);

        // Row 2: Display name
        label.gridy = 2;
        add(new JLabel("Display name:"), label);
        field.gridy = 2;
        add(displayNameEditor, field);

        // Row 3: Classes
        label.gridy = 3;
        add(new JLabel("Display:"), label);
        field.gridy = 3;
        add(buildClassRow(), field);

        // Row 4: Details text (visible only when "detail" class is checked)
        label.gridy = 4;
        label.anchor = GridBagConstraints.NORTHWEST;
        label.insets = new Insets(scaled(6), scaled(6), scaled(4), scaled(4));
        add(descriptionLabel, label);
        field.gridy = 4;
        field.fill = GridBagConstraints.BOTH;
        field.weighty = 0;
        add(descriptionEditor, field);

        // Row 5: Spacer always present; absorbs extra vertical space when description is
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

    private JPanel buildClassRow() {
        JPanel classRow = new JPanel();
        classRow.setLayout(new BoxLayout(classRow, BoxLayout.X_AXIS));
        classRow.setOpaque(false);
        classRow.add(classPerson);
        classRow.add(Box.createHorizontalStrut(scaled(8)));
        classRow.add(classRole);
        classRow.add(Box.createHorizontalStrut(scaled(8)));
        classRow.add(classDetail);
        classRow.add(Box.createHorizontalGlue());
        return classRow;
    }

    private void wireEvents() {
        wireDisplayNameEvents();
        wireDescriptionEvents();
        wireClassCheckboxEvents();
    }

    private void wireDisplayNameEvents() {
        displayNameEditor.addPropertyChangeListener("text", e -> {
            if (!loading && current != null && langDoc != null) {
                String key = "credits.category." + KeySanitizer.sanitize(current.id);
                String value = (String) e.getNewValue();
                if (value.isEmpty()) langDoc.remove(key);
                else langDoc.set(key, value);
            }
        });
        displayNameEditor.addUndoableEditListener(e -> {
            if (!loading && current != null) {
                onCommand.execute(new DocumentEditCommand("Edit display name", e.getEdit()));
            }
        });
    }

    private void wireDescriptionEvents() {
        descriptionEditor.addPropertyChangeListener("text", e -> {
            if (!loading && current != null && langDoc != null) {
                String key = "credits.category." + KeySanitizer.sanitize(current.id) + ".detail";
                String value = (String) e.getNewValue();
                if (value.isEmpty()) langDoc.remove(key);
                else langDoc.set(key, value);
            }
        });
        descriptionEditor.addUndoableEditListener(e -> {
            if (!loading && current != null) {
                onCommand.execute(new DocumentEditCommand("Edit details", e.getEdit()));
            }
        });
    }

    private void wireClassCheckboxEvents() {
        classPerson.addActionListener(e -> onClassToggle(CLASS_PERSON, classPerson.isSelected()));
        classRole.addActionListener(e -> onClassToggle(CLASS_ROLE, classRole.isSelected()));
        classDetail.addActionListener(e -> {
            onClassToggle(CLASS_DETAIL, classDetail.isSelected());
            updateDescriptionVisibility();
        });
    }

    /**
     * Sets the lang document used for reading and writing display name and description.
     * Call once after a session is loaded.
     */
    public void setContext(LangDocument langDoc) {
        this.langDoc = langDoc;
    }

    /**
     * Populates all fields from {@code cat} without firing any commands.
     * Call after any external model change: initial load, undo, or redo.
     */
    public void load(DocumentCategory cat) {
        current = cat;
        loading = true;
        try {
            String key = "credits.category." + KeySanitizer.sanitize(cat.id);
            idField.setText(cat.id);
            langKeyLabel.setText(key);
            String name = langDoc != null ? langDoc.get(key) : null;
            displayNameEditor.setText(name != null ? name : "");
            String detail = langDoc != null ? langDoc.get(key + ".detail") : null;
            descriptionEditor.setText(detail != null ? detail : "");
            classPerson.setSelected(cat.classes.contains(CLASS_PERSON));
            classRole.setSelected(cat.classes.contains(CLASS_ROLE));
            classDetail.setSelected(cat.classes.contains(CLASS_DETAIL));
        } finally {
            loading = false;
        }
        updateDescriptionVisibility();
    }

    private void onClassToggle(String cls, boolean selected) {
        if (loading || current == null) return;
        Set<String> newSet = new LinkedHashSet<>(current.classes);
        if (selected) {
            newSet.add(cls);
        } else {
            newSet.remove(cls);
        }
        if (!newSet.equals(current.classes)) {
            onCommand.execute(
                new EditFieldCommand<>("Edit classes", () -> current.classes, v -> current.classes = v, newSet));
        }
    }

    private void updateDescriptionVisibility() {
        boolean visible = classDetail.isSelected();
        descriptionLabel.setVisible(visible);
        descriptionEditor.setVisible(visible);

        // Transfer weighty to whichever component should absorb extra vertical space.
        GridBagLayout gbl = (GridBagLayout) getLayout();
        GridBagConstraints descGbc = gbl.getConstraints(descriptionEditor);
        descGbc.weighty = visible ? 1.0 : 0;
        gbl.setConstraints(descriptionEditor, descGbc);
        GridBagConstraints spacerGbc = gbl.getConstraints(spacer);
        spacerGbc.weighty = visible ? 0 : 1.0;
        gbl.setConstraints(spacer, spacerGbc);

        revalidate();
        repaint();
    }
}
