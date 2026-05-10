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
