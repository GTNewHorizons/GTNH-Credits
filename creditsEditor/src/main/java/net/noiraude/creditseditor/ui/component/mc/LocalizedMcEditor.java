package net.noiraude.creditseditor.ui.component.mc;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapHair;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;

import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.Optional;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.TransferHandler;
import javax.swing.text.JTextComponent;

import net.noiraude.creditseditor.mc.McText;
import net.noiraude.creditseditor.service.LangResolver;
import net.noiraude.creditseditor.ui.I18n;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Locale-aware editor with a read-only EN view of the English source value. */
public final class LocalizedMcEditor extends AbstractMcEditor {

    /** Stable identifier for the EN toggle button. */
    public static final @NotNull String COMPONENT_NAME_EN_TOGGLE = "creditsEditor.mc.enToggle";

    /** Stable identifier for the EN read-only pane. */
    public static final @NotNull String COMPONENT_NAME_EN_PANE = "creditsEditor.mc.enPane";

    private final @NotNull JToggleButton enToggle = new JToggleButton(I18n.get("editor.localized.en_toggle.label"));
    private final @NotNull McWysiwygPane enPane;
    private final @NotNull JScrollPane enScroll;

    private @NotNull Supplier<@NotNull Optional<@NotNull String>> englishValueSupplier = Optional::empty;
    private @NotNull String activeLocale = LangResolver.DEFAULT_LOCALE;
    private boolean enViewVisible;

    public LocalizedMcEditor(boolean multiLine) {
        super(multiLine);
        setOpaque(false);
        setPaneTransferHandler(new PlainTextOnly());

        enPane = new McWysiwygPane(multiLine);
        enPane.setName(COMPONENT_NAME_EN_PANE);
        enPane.setEditable(false);
        enPane.setTransferHandler(new PlainTextExportOnly());
        enScroll = new JScrollPane(enPane);
        enScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        enToggle.setName(COMPONENT_NAME_EN_TOGGLE);
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

    @Override
    protected void refreshOwnedPanes() {
        pushContentTo(enPane, enPane.getText());
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
        if (enViewVisible) return;
        pushContentTo(enPane, McText.decodeLang(englishValue));
        setBodyComponent(enScroll);
        setEditable(false);
        enViewVisible = true;
    }

    private void exitEnView() {
        if (!enViewVisible) return;
        restoreDefaultBody();
        setEditable(true);
        enViewVisible = false;
    }

    private void applyLocaleVisibility() {
        boolean isDefault = LangResolver.DEFAULT_LOCALE.equals(activeLocale);
        if (isDefault && enViewVisible) {
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
