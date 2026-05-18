package net.noiraude.creditseditor.ui.component.mc;

import net.noiraude.creditseditor.mc.McText;
import net.noiraude.creditseditor.service.LangResolver;
import net.noiraude.creditseditor.ui.I18n;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import java.awt.Insets;
import java.util.Optional;
import java.util.function.Supplier;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapHair;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;

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

        enPane = new McWysiwygPane(multiLine);
        enPane.setName(COMPONENT_NAME_EN_PANE);
        enPane.setEditable(false);
        installHandlerOn(enPane);
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
        installHandlerOn(enPane);
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
}
