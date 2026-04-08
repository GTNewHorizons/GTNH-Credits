package net.noiraude.creditseditor.ui.detail;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.command.impl.EditFieldCommand;
import net.noiraude.creditseditor.model.EditorCategory;
import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.creditseditor.ui.component.MinecraftTextAreaEditor;
import net.noiraude.creditseditor.ui.component.MinecraftTextEditor;

/**
 * Form panel that displays and edits the fields of a single {@link EditorCategory}.
 *
 * <p>
 * Changes fire {@link Command} objects through the supplied executor rather than mutating
 * the model directly. Call {@link #load(EditorCategory)} whenever the active category
 * changes or the model has been refreshed by an undo/redo.
 */
public final class CategoryDetailView extends JPanel {

    private static final String CLASS_PERSON = "person";
    private static final String CLASS_ROLE = "role";
    private static final String CLASS_DETAIL = "detail";

    private final Consumer<Command> onCommand;
    private EditorCategory current;
    private boolean loading;

    private final JTextField idField = new JTextField();
    private final JLabel langKeyLabel = new JLabel();
    private final MinecraftTextEditor displayNameEditor = new MinecraftTextEditor();
    private final JCheckBox classPerson = new JCheckBox("person");
    private final JCheckBox classRole = new JCheckBox("role");
    private final JCheckBox classDetail = new JCheckBox("detail");
    private final JLabel descriptionLabel = new JLabel("Description:");
    private final MinecraftTextAreaEditor descriptionEditor = new MinecraftTextAreaEditor();

    public CategoryDetailView(Consumer<Command> onCommand) {
        this.onCommand = onCommand;
        setLayout(new GridBagLayout());

        idField.setEditable(false);
        idField.setBackground(UIManager.getColor("Panel.background"));
        langKeyLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

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
        add(new JLabel("Classes:"), label);
        field.gridy = 3;
        JPanel classRow = new JPanel();
        classRow.setLayout(new BoxLayout(classRow, BoxLayout.X_AXIS));
        classRow.setOpaque(false);
        classRow.add(classPerson);
        classRow.add(Box.createHorizontalStrut(8));
        classRow.add(classRole);
        classRow.add(Box.createHorizontalStrut(8));
        classRow.add(classDetail);
        classRow.add(Box.createHorizontalGlue());
        add(classRow, field);

        // Row 4: Description (visible only when "detail" class is checked)
        label.gridy = 4;
        label.anchor = GridBagConstraints.NORTHWEST;
        label.insets = new Insets(6, 6, 4, 4);
        add(descriptionLabel, label);
        field.gridy = 4;
        field.fill = GridBagConstraints.BOTH;
        field.weighty = 1.0;
        add(descriptionEditor, field);

        // Wire events
        displayNameEditor.addPropertyChangeListener("text", e -> {
            if (!loading && current != null) {
                String newVal = (String) e.getNewValue();
                if (!newVal.equals(current.displayName)) {
                    onCommand.accept(
                        new EditFieldCommand<>(
                            "Edit display name",
                            () -> current.displayName,
                            v -> current.displayName = v,
                            newVal));
                }
            }
        });

        descriptionEditor.addPropertyChangeListener("text", e -> {
            if (!loading && current != null) {
                String newVal = (String) e.getNewValue();
                if (!newVal.equals(current.description)) {
                    onCommand.accept(
                        new EditFieldCommand<>(
                            "Edit description",
                            () -> current.description,
                            v -> current.description = v,
                            newVal));
                }
            }
        });

        classPerson.addActionListener(e -> onClassToggle(CLASS_PERSON, classPerson.isSelected()));
        classRole.addActionListener(e -> onClassToggle(CLASS_ROLE, classRole.isSelected()));
        classDetail.addActionListener(e -> {
            onClassToggle(CLASS_DETAIL, classDetail.isSelected());
            updateDescriptionVisibility();
        });

        updateDescriptionVisibility();
    }

    /**
     * Populates all fields from {@code cat} without firing any commands.
     * Call after any external model change (undo, redo, or initial load).
     */
    public void load(EditorCategory cat) {
        current = cat;
        loading = true;
        try {
            idField.setText(cat.id);
            langKeyLabel.setText("credits.category." + KeySanitizer.sanitize(cat.id));
            displayNameEditor.setText(cat.displayName);
            descriptionEditor.setText(cat.description);
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
            onCommand.accept(
                new EditFieldCommand<>("Edit classes", () -> current.classes, v -> current.classes = v, newSet));
        }
    }

    private void updateDescriptionVisibility() {
        boolean visible = classDetail.isSelected();
        descriptionLabel.setVisible(visible);
        descriptionEditor.setVisible(visible);
        revalidate();
        repaint();
    }

    // -----------------------------------------------------------------------
    // Layout helpers
    // -----------------------------------------------------------------------

    private static GridBagConstraints labelConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = new Insets(4, 6, 4, 4);
        return c;
    }

    private static GridBagConstraints fieldConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        c.weighty = 0;
        c.insets = new Insets(4, 0, 4, 6);
        return c;
    }
}
