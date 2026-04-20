package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.util.Set;
import java.util.function.BiConsumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.noiraude.libcredits.model.DocumentCategory;

import org.jetbrains.annotations.NotNull;

/**
 * Row of class-flag checkboxes for a {@link DocumentCategory}.
 *
 * <p>
 * Groups the three semantic class markers (`person`, `role`, `detail`) into a single widget
 * that exposes set-based load, per-class selection state, and a toggle listener.
 */
public final class CategoryClassSelector extends JPanel {

    public static final @NotNull String CLASS_PERSON = "person";
    public static final @NotNull String CLASS_ROLE = "role";
    public static final @NotNull String CLASS_DETAIL = "detail";

    private final @NotNull JCheckBox classPerson = new JCheckBox(CLASS_PERSON);
    private final @NotNull JCheckBox classRole = new JCheckBox(CLASS_ROLE);
    private final @NotNull JCheckBox classDetail = new JCheckBox(CLASS_DETAIL);

    public CategoryClassSelector() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
        add(classPerson);
        add(Box.createHorizontalStrut(scaled(8)));
        add(classRole);
        add(Box.createHorizontalStrut(scaled(8)));
        add(classDetail);
        add(Box.createHorizontalGlue());
    }

    public void setClasses(@NotNull Set<String> classes) {
        classPerson.setSelected(classes.contains(CLASS_PERSON));
        classRole.setSelected(classes.contains(CLASS_ROLE));
        classDetail.setSelected(classes.contains(CLASS_DETAIL));
    }

    public boolean isDetailSelected() {
        return classDetail.isSelected();
    }

    /**
     * Registers a listener fired on every checkbox toggle with the class id and new state.
     */
    public void addClassToggleListener(@NotNull BiConsumer<String, Boolean> listener) {
        classPerson.addActionListener(e -> listener.accept(CLASS_PERSON, classPerson.isSelected()));
        classRole.addActionListener(e -> listener.accept(CLASS_ROLE, classRole.isSelected()));
        classDetail.addActionListener(e -> listener.accept(CLASS_DETAIL, classDetail.isSelected()));
    }
}
