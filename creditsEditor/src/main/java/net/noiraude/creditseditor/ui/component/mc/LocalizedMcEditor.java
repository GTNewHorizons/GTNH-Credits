package net.noiraude.creditseditor.ui.component.mc;

import net.noiraude.creditseditor.mc.McText;
import net.noiraude.creditseditor.service.LangResolver;
import net.noiraude.creditseditor.ui.I18n;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.TransferHandler;
import javax.swing.text.JTextComponent;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.Optional;
import java.util.function.Supplier;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapHair;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;

/**
 * Locale-aware editor that lets the user peek at the English source of the currently edited
 * field by replacing the editing pane with a short-lived read-only pane that exists only while
 * the EN view is active.
 */
public final class LocalizedMcEditor extends AbstractMcEditor {

    private sealed interface ViewState permits Editing,EnViewing {
    }

    private record Editing() implements ViewState {}

    private record EnViewing(@NotNull McWysiwygPane pane, @NotNull JScrollPane scroll) implements ViewState {}

    private static final @NotNull ViewState EDITING = new Editing();

    private final boolean multiLine;
    private final @NotNull JToggleButton enToggle = new JToggleButton(I18n.get("editor.localized.en_toggle.label"));

    private @NotNull Supplier<@NotNull Optional<@NotNull String>> englishValueSupplier = Optional::empty;
    private @NotNull String activeLocale = LangResolver.DEFAULT_LOCALE;
    private @NotNull ViewState state = EDITING;

    public LocalizedMcEditor(boolean multiLine) {
        super(multiLine);
        this.multiLine = multiLine;
        setOpaque(false);
        setPaneTransferHandler(new PlainTextOnly());

        enToggle.setToolTipText(I18n.get("editor.localized.en_toggle.tooltip"));
        enToggle.setMargin(new Insets(gapHair, gapSmall, gapHair, gapSmall));
        enToggle.setFocusable(false);
        enToggle.addActionListener(e -> onEnToggleClicked());
        addTopBarLeadingComponent(enToggle);

        applyLocaleVisibility();
    }

    /** Provides the English source value to display when the EN view is requested. */
    @Contract(mutates = "this")
    public void setEnglishValueSupplier(@NotNull Supplier<@NotNull Optional<@NotNull String>> supplier) {
        this.englishValueSupplier = supplier;
    }

    /** Sets the active editing locale tag. */
    public void setActiveLocale(@NotNull String locale) {
        this.activeLocale = locale;
        applyLocaleVisibility();
    }

    @Contract(pure = true)
    boolean isEnViewing() {
        return state instanceof EnViewing;
    }

    /** Returns the read-only pane used to display the English source while EN view is active. */
    @Contract(pure = true)
    @NotNull
    Optional<@NotNull McWysiwygPane> enPaneForTest() {
        return state instanceof EnViewing v ? Optional.of(v.pane()) : Optional.empty();
    }

    @Contract(pure = true)
    @NotNull
    JToggleButton enToggleForTest() {
        return enToggle;
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
        if (state instanceof EnViewing) return;
        McWysiwygPane pane = new McWysiwygPane(multiLine);
        pane.setEditable(false);
        pane.setTransferHandler(new PlainTextExportOnly());
        pane.setText(McText.decodeLang(englishValue));
        JScrollPane scroll = new JScrollPane(pane);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        setBodyComponent(scroll);
        setEditable(false);
        state = new EnViewing(pane, scroll);
    }

    private void exitEnView() {
        if (!(state instanceof EnViewing)) return;
        restoreDefaultBody();
        setEditable(true);
        state = EDITING;
    }

    private void applyLocaleVisibility() {
        boolean isDefault = LangResolver.DEFAULT_LOCALE.equals(activeLocale);
        if (isDefault && state instanceof EnViewing) {
            enToggle.setSelected(false);
            exitEnView();
        }
        enToggle.setVisible(!isDefault);
    }

    /** Transfer handler that exports only the plain-text selection and refuses imports. */
    private static final class PlainTextExportOnly extends TransferHandler {

        @Override
        public int getSourceActions(@NotNull JComponent c) {
            return COPY;
        }

        @Override
        protected @NotNull Transferable createTransferable(@NotNull JComponent c) {
            JTextComponent tc = (JTextComponent) c;
            String sel = tc.getSelectedText();
            return new StringSelection(sel == null ? "" : sel);
        }

        @Override
        public boolean canImport(@NotNull TransferSupport support) {
            return false;
        }
    }

    /** Transfer handler that exports and imports plain-text selections only. */
    private static final class PlainTextOnly extends TransferHandler {

        @Override
        public int getSourceActions(@NotNull JComponent c) {
            return COPY_OR_MOVE;
        }

        @Override
        protected @NotNull Transferable createTransferable(@NotNull JComponent c) {
            JTextComponent tc = (JTextComponent) c;
            String sel = tc.getSelectedText();
            return new StringSelection(sel == null ? "" : sel);
        }

        @Override
        protected void exportDone(@NotNull JComponent source, @NotNull Transferable data, int action) {
            if (action == MOVE && source instanceof JTextComponent tc) tc.replaceSelection("");
        }

        @Override
        public boolean canImport(@NotNull TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(@NotNull TransferSupport support) {
            if (!canImport(support)) return false;
            if (!(support.getComponent() instanceof JTextComponent tc)) return false;
            if (!tc.isEditable() || !tc.isEnabled()) return false;
            try {
                String text = (String) support.getTransferable()
                    .getTransferData(DataFlavor.stringFlavor);
                tc.replaceSelection(text);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }
}
