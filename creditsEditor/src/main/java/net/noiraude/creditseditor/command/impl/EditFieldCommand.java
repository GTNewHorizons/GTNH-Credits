package net.noiraude.creditseditor.command.impl;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.noiraude.creditseditor.command.Command;

/**
 * Generic command that sets a single field on any mutable object.
 *
 * <p>
 * The getter is called during {@link #execute} to capture the old value for undo. The getter
 * and setter must operate on the same field.
 *
 * @param <T> the type of the field value
 */
public final class EditFieldCommand<T> implements Command {

    private final String displayName;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final T newValue;
    private T oldValue;

    public EditFieldCommand(String displayName, Supplier<T> getter, Consumer<T> setter, T newValue) {
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
    public String getDisplayName() {
        return displayName;
    }
}
