package net.noiraude.creditseditor.ui.component;

/**
 * Single-line editable field for strings that may contain Minecraft {@code §x} formatting codes.
 *
 * <p>
 * Accepts and returns values in Minecraft 1.7.10 lang file format (backslashes escaped as
 * {@code \\}). A toolbar and {@code [<>]} / {@code [Aa]} toggle switch between rendered
 * (WYSIWYG) and raw mode.
 *
 * <p>
 * Fires a {@code "text"} {@link java.beans.PropertyChangeEvent} on user input. The event
 * value is in lang file format. Programmatic {@code setText} calls do not fire the event.
 */
public final class MinecraftTextEditor extends AbstractMcEditor {

    public MinecraftTextEditor() {
        super(false);
    }
}
