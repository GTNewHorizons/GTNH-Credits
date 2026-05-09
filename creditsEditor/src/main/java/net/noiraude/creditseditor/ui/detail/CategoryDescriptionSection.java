package net.noiraude.creditseditor.ui.detail;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.JLabel;
import javax.swing.event.UndoableEditListener;

import net.noiraude.creditseditor.ui.I18n;
import net.noiraude.creditseditor.ui.component.mc.LocalizedMcEditor;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Holder binding the "Details:" label to its locale-aware multi-line editor.
 *
 * <p>
 * Not a {@link javax.swing.JComponent}: the label and editor are added separately to the
 * owning two-column form grid so they line up with the other rows. This holder lets the
 * owner carry one field instead of two and toggles both components' visibility together.
 *
 * <p>
 * Indexed-detail keys ({@code .detail.0}, {@code .detail.1}, ...) are a manual-editing
 * convenience in lang files and are treated by the editor as equivalent to a single
 * {@code .detail} value with {@code \n} paragraph separators; no per-paragraph editor is
 * built here.
 */
public final class CategoryDescriptionSection {

    private final @NotNull JLabel label = new JLabel(I18n.get("section.category.details.label"));
    private final @NotNull LocalizedMcEditor editor = new LocalizedMcEditor(true);

    @Contract(pure = true)
    public @NotNull JLabel label() {
        return label;
    }

    @Contract(pure = true)
    public @NotNull LocalizedMcEditor editor() {
        return editor;
    }

    public void setText(@NotNull String text) {
        editor.setText(text);
    }

    public void setActiveLocale(@NotNull String locale) {
        editor.setActiveLocale(locale);
    }

    public void setEnglishValueSupplier(@NotNull Supplier<@NotNull Optional<@NotNull String>> supplier) {
        editor.setEnglishValueSupplier(supplier);
    }

    public void setVisible(boolean visible) {
        label.setVisible(visible);
        editor.setVisible(visible);
    }

    public void addTextChangeListener(@NotNull Consumer<@NotNull String> listener) {
        editor.addTextChangeListener(listener);
    }

    public void addUndoableEditListener(@NotNull UndoableEditListener listener) {
        editor.addUndoableEditListener(listener);
    }
}
