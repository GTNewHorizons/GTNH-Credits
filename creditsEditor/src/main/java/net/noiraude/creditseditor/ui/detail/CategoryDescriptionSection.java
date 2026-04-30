package net.noiraude.creditseditor.ui.detail;

import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.event.UndoableEditListener;

import net.noiraude.creditseditor.ui.I18n;
import net.noiraude.creditseditor.ui.component.MinecraftTextAreaEditor;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Holder binding the "Details:" label to its {@link MinecraftTextAreaEditor}.
 *
 * <p>
 * Not a {@link javax.swing.JComponent}: the label and editor are added separately to the
 * owning two-column form grid so they line up with the other rows. This holder lets the
 * owner carry one field instead of two and toggles both components' visibility together.
 */
public final class CategoryDescriptionSection {

    private final @NotNull JLabel label = new JLabel(I18n.get("section.category.details.label"));
    private final @NotNull MinecraftTextAreaEditor editor = new MinecraftTextAreaEditor();

    @Contract(pure = true)
    public @NotNull JLabel label() {
        return label;
    }

    @Contract(pure = true)
    public @NotNull MinecraftTextAreaEditor editor() {
        return editor;
    }

    public void setText(@NotNull String text) {
        editor.setText(text);
    }

    public void setVisible(boolean visible) {
        label.setVisible(visible);
        editor.setVisible(visible);
    }

    public void addTextPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        editor.addPropertyChangeListener("text", listener);
    }

    public void addUndoableEditListener(@NotNull UndoableEditListener listener) {
        editor.addUndoableEditListener(listener);
    }
}
