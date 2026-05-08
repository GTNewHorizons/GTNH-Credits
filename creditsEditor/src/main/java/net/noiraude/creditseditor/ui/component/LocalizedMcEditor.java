package net.noiraude.creditseditor.ui.component;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapHair;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.UndoableEditListener;

import net.noiraude.creditseditor.service.LangResolver;
import net.noiraude.creditseditor.ui.I18n;
import net.noiraude.creditseditor.ui.MsgArg;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Locale-aware wrapper around {@link MinecraftTextEditor} or {@link MinecraftTextAreaEditor}.
 *
 * <p>
 * Adds an "EN" toggle button to the inner editor's top bar. When the supplier installed via
 * {@link #setEnglishValueSupplier} resolves the field to a present {@link Optional}, toggling
 * EN swaps the inner pane to that read-only English value and reveals a "Copy to &lt;locale&gt;"
 * button next to the toggle; clicking Copy replaces the editing locale's pending value with
 * the English source as if the user had typed it, then exits EN view. When the supplier returns
 * {@link Optional#empty()}, EN view does not engage. The EN toggle and Copy button stay hidden
 * when the active locale is {@link LangResolver#DEFAULT_LOCALE}.
 *
 * <p>
 * The wrapper does not subscribe to any bus topics directly. Callers (Phase C.2 onwards) drive
 * the active locale via {@link #setActiveLocale} and supply the English value through
 * {@link #setEnglishValueSupplier}; the supplier defaults to {@code () -> Optional.empty()}.
 * Text-change observation is exposed as {@link #addTextChangeListener}, a {@code Consumer}-based
 * API that delivers the new value directly without exposing the {@link java.beans.PropertyChangeEvent}
 * machinery used by the inner editor.
 */
public final class LocalizedMcEditor extends JPanel {

    /** Two-state model for which content the inner editor currently displays. */
    private sealed interface ViewState permits Editing,EnViewing {

        /** {@code true} iff the EN read-only view is currently shown. */
        default boolean isEnViewing() {
            return false;
        }

        /**
         * Returns the buffered editing-locale value preserved for restoration on EN view exit, or
         * {@link Optional#empty()} when the EN view is not active.
         */
        default @NotNull Optional<@NotNull String> bufferedEditingValue() {
            return Optional.empty();
        }
    }

    /** Inner editor shows the editing-locale value and accepts user input. */
    private record Editing() implements ViewState {}

    /**
     * Inner editor shows the read-only English source. {@code editingValue} preserves the
     * editing-locale pending value to be restored when the EN view exits.
     */
    private record EnViewing(@NotNull String editingValue) implements ViewState {

        @Override
        public boolean isEnViewing() {
            return true;
        }

        @Override
        public @NotNull Optional<@NotNull String> bufferedEditingValue() {
            return Optional.of(editingValue);
        }
    }

    private static final @NotNull ViewState EDITING = new Editing();

    private final @NotNull AbstractMcEditor inner;
    private final @NotNull JToggleButton enToggle = new JToggleButton(I18n.get("editor.localized.en_toggle.label"));
    private final @NotNull JButton copyButton = new JButton();
    private final @NotNull List<@NotNull Consumer<@NotNull String>> textListeners = new ArrayList<>();

    private @NotNull Supplier<@NotNull Optional<@NotNull String>> englishValueSupplier = Optional::empty;
    private @NotNull String activeLocale = LangResolver.DEFAULT_LOCALE;
    private @NotNull ViewState state = EDITING;

    public LocalizedMcEditor(boolean multiLine) {
        super(new BorderLayout());
        setOpaque(false);
        inner = multiLine ? new MinecraftTextAreaEditor() : new MinecraftTextEditor();
        add(inner, BorderLayout.CENTER);

        enToggle.setToolTipText(I18n.get("editor.localized.en_toggle.tooltip"));
        enToggle.setMargin(new Insets(gapHair, gapSmall, gapHair, gapSmall));
        enToggle.setFocusable(false);
        enToggle.addActionListener(e -> onEnToggleClicked());

        copyButton.setMargin(new Insets(gapHair, gapSmall, gapHair, gapSmall));
        copyButton.setFocusable(false);
        copyButton.setVisible(false);
        copyButton.addActionListener(e -> copyEnglishToEditing());

        inner.addTopBarLeadingComponent(enToggle);
        inner.addTopBarLeadingComponent(copyButton);

        inner.addTextChangeListener(newText -> {
            if (state.isEnViewing()) return;
            for (Consumer<@NotNull String> l : textListeners) l.accept(newText);
        });

        applyLocaleVisibility();
    }

    /**
     * Installs the supplier for the English ({@link LangResolver#DEFAULT_LOCALE}) value of the
     * field currently displayed by this editor. The supplier is invoked when the EN view is
     * switched on and whenever the Copy action runs. An empty {@link Optional} signals that no
     * English source is available; in that case EN view does not engage.
     */
    public void setEnglishValueSupplier(@NotNull Supplier<@NotNull Optional<@NotNull String>> supplier) {
        this.englishValueSupplier = supplier;
    }

    /**
     * Sets the active editing locale tag. When set to {@link LangResolver#DEFAULT_LOCALE}, the
     * EN toggle and Copy button are hidden, and any active EN view is closed (the inner
     * editor's pending value is restored from the wrapper's buffer).
     */
    public void setActiveLocale(@NotNull String locale) {
        this.activeLocale = locale;
        applyLocaleVisibility();
    }

    /**
     * Sets the editing-locale value displayed by the wrapper.
     *
     * <p>
     * When the EN view is on, the value is folded into the {@link EnViewing} state so it shows
     * after EN is turned off. When off, it is forwarded verbatim to the inner editor and does
     * not invoke the registered text listeners (matching {@link AbstractMcEditor#setText}).
     *
     * @param langValue the value in Minecraft lang file format; pass an empty string for empty
     */
    public void setText(@NotNull String langValue) {
        if (state.isEnViewing()) state = new EnViewing(langValue);
        else inner.setText(langValue);
    }

    /** Returns the current editing-locale value, regardless of the EN view state. */
    @Contract(pure = true)
    public @NotNull String getText() {
        return state.bufferedEditingValue()
            .orElseGet(inner::getText);
    }

    /**
     * Registers a listener invoked with the new editing-locale value whenever the user changes
     * the inner editor's content (typing, formatting, Copy action). EN-view transitions and
     * programmatic {@link #setText} calls are suppressed.
     */
    public void addTextChangeListener(@NotNull Consumer<@NotNull String> listener) {
        textListeners.add(listener);
    }

    /**
     * Registers a listener notified of undoable edits on the inner editor. EN-view transitions
     * use silent {@link AbstractMcEditor#setText} calls and do not generate events; the Copy
     * action uses {@link AbstractMcEditor#setTextAsUserInput} and does.
     */
    public void addUndoableEditListener(@NotNull UndoableEditListener l) {
        inner.addUndoableEditListener(l);
    }

    /** Returns {@code true} when the EN view is currently shown. */
    @Contract(pure = true)
    boolean isEnViewing() {
        return state.isEnViewing();
    }

    /** Returns the inner editor used to render and edit the active locale's value. */
    @Contract(pure = true)
    @NotNull
    AbstractMcEditor inner() {
        return inner;
    }

    @Contract(pure = true)
    @NotNull
    JToggleButton enToggleForTest() {
        return enToggle;
    }

    @Contract(pure = true)
    @NotNull
    JButton copyButtonForTest() {
        return copyButton;
    }

    private void onEnToggleClicked() {
        if (enToggle.isSelected()) {
            Optional<String> english = englishValueSupplier.get();
            if (english.isEmpty()) {
                enToggle.setSelected(false);
                return;
            }
            enterEnView(english.get());
        } else {
            exitEnView();
        }
    }

    private void enterEnView(@NotNull String englishValue) {
        if (state.isEnViewing()) return;
        state = new EnViewing(inner.getText());
        inner.setText(englishValue);
        inner.setEditable(false);
        copyButton.setVisible(true);
        enToggle.setSelected(true);
    }

    private void exitEnView() {
        Optional<String> buffered = state.bufferedEditingValue();
        if (buffered.isEmpty()) return;
        inner.setText(buffered.get());
        state = EDITING;
        inner.setEditable(true);
        copyButton.setVisible(false);
        enToggle.setSelected(false);
    }

    private void copyEnglishToEditing() {
        if (!state.isEnViewing()) return;
        Optional<String> english = englishValueSupplier.get();
        if (english.isEmpty()) return;
        // Restore editing mode silently (this puts the previous editing value back into the
        // inner editor, undoing the silent swap performed by enterEnView)...
        exitEnView();
        // ...then replay the English value as user input so the editing-locale change is
        // observed by listeners the same way typing it would be.
        inner.setTextAsUserInput(english.get());
    }

    private void applyLocaleVisibility() {
        boolean isDefault = LangResolver.DEFAULT_LOCALE.equals(activeLocale);
        if (isDefault) exitEnView();
        enToggle.setVisible(!isDefault);
        if (isDefault) copyButton.setVisible(false);
        copyButton.setText(I18n.get("editor.localized.copy_to.label", MsgArg.text(activeLocale)));
        copyButton.setToolTipText(I18n.get("editor.localized.copy_to.tooltip", MsgArg.text(activeLocale)));
    }
}
