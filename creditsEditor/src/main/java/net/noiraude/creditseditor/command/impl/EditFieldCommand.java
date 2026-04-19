package net.noiraude.creditseditor.command.impl;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generic command that sets a single field on any mutable object.
 *
 * <p>
 * The getter is called during {@link #execute} to capture the old value for undo. The getter
 * and setter must operate on the same field.
 *
 * @param <T> the type of the field value
 */
public final class EditFieldCommand<T> extends AbstractLightEditCommand {

    private final @NotNull String displayName;
    private final @NotNull Supplier<T> getter;
    private final @NotNull Consumer<T> setter;
    private final @Nullable T newValue;
    private @Nullable T oldValue;

    public EditFieldCommand(@NotNull String displayName, @NotNull Supplier<T> getter, @NotNull Consumer<T> setter,
        @Nullable T newValue) {
        this.displayName = displayName;
        this.getter = getter;
        this.setter = setter;
        this.newValue = newValue;
    }

    @Override
    public void execute() {
        oldValue = getter.get();
        setter.accept(newValue);
    }

    @Override
    public void undo() {
        setter.accept(oldValue);
    }

    @Override
    public @NotNull String getDisplayName() {
        return displayName;
    }

}
