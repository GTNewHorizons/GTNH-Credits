package net.noiraude.creditseditor.ui;

import java.awt.Component;
import java.awt.Container;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

/** Test-side helpers for locating Swing components by their {@code Component.setName} identifier. */
public final class SwingFinder {

    private SwingFinder() {}

    /** Returns the first component under {@code root} whose name equals {@code name}. */
    public static @NotNull Optional<@NotNull Component> findByName(@NotNull Container root, @NotNull String name) {
        if (name.equals(root.getName())) return Optional.of(root);
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName())) return Optional.of(child);
            if (child instanceof Container nested) {
                Optional<Component> found = findByName(nested, name);
                if (found.isPresent()) return found;
            }
        }
        return Optional.empty();
    }

    /** Returns the first component under {@code root} matching {@code name} and {@code type}. */
    public static <T extends Component> @NotNull Optional<@NotNull T> findByName(@NotNull Container root,
        @NotNull String name, @NotNull Class<T> type) {
        return findByName(root, name).filter(type::isInstance)
            .map(type::cast);
    }
}
