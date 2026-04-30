package net.noiraude.creditseditor.ui.component;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.creditseditor.ui.I18n;

import org.jetbrains.annotations.NotNull;

/**
 * Live preview label that shows the lang key suffix that would be derived from a
 * {@link JTextComponent}'s current content via {@link KeySanitizer}.
 *
 * <p>
 * Sanitization runs on the editor thread on every document change, so the user sees right away
 * which characters get stripped, replaced, or lowercased. When the input matches its sanitized
 * form, the label renders in the disabled foreground color so it reads as a passive hint;
 * when sanitization changes the input, it switches to the warning foreground so the
 * transformation is visible rather than silent.
 *
 * @implNote The label tracks the supplied text component's {@link Document}; if the component
 *           swaps documents at runtime, callers must rebind by constructing a fresh hint.
 */
public final class KeyHintLabel extends JLabel {

    private final @NotNull JTextComponent source;

    public KeyHintLabel(@NotNull JTextComponent source) {
        this.source = source;
        source.getDocument()
            .addDocumentListener(new AnyChangeListener(this::refresh));
        refresh();
    }

    private void refresh() {
        String input = source.getText();
        if (input == null || input.isEmpty()) {
            setText(" ");
            setForeground(UIManager.getColor("Label.disabledForeground"));
            return;
        }
        String sanitized = KeySanitizer.sanitize(input);
        boolean changed = !sanitized.equals(input);
        setText(I18n.get(changed ? "keyhint.changed" : "keyhint.unchanged", sanitized));
        setForeground(changed ? warningColor() : UIManager.getColor("Label.disabledForeground"));
    }

    private static @NotNull Color warningColor() {
        Color c = UIManager.getColor("Component.warning.focusedBorderColor");
        if (c != null) return c;
        c = UIManager.getColor("Component.error.focusedBorderColor");
        if (c != null) return c;
        return new Color(0xC07000);
    }
}
