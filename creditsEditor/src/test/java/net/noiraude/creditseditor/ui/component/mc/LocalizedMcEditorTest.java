package net.noiraude.creditseditor.ui.component.mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.text.BadLocationException;

import net.noiraude.creditseditor.service.LangResolver;
import net.noiraude.creditseditor.ui.SwingFinder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LocalizedMcEditorTest {

    @BeforeEach
    public void requireGraphicsEnvironment() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Swing components require a graphics environment");
    }

    private static JToggleButton enToggle(LocalizedMcEditor editor) {
        return SwingFinder.findByName(editor, LocalizedMcEditor.COMPONENT_NAME_EN_TOGGLE, JToggleButton.class)
            .orElseThrow();
    }

    private static JButton rawToggle(LocalizedMcEditor editor) {
        return SwingFinder.findByName(editor, LocalizedMcEditor.COMPONENT_NAME_RAW_TOGGLE, JButton.class)
            .orElseThrow();
    }

    private static Optional<McWysiwygPane> enPane(LocalizedMcEditor editor) {
        return SwingFinder.findByName(editor, LocalizedMcEditor.COMPONENT_NAME_EN_PANE, McWysiwygPane.class);
    }

    private static McWysiwygPane mainPane(LocalizedMcEditor editor) {
        return SwingFinder.findByName(editor, LocalizedMcEditor.COMPONENT_NAME_MAIN_PANE, McWysiwygPane.class)
            .orElseThrow();
    }

    private static String docText(McWysiwygPane pane) throws BadLocationException {
        return pane.getDocument()
            .getText(
                0,
                pane.getDocument()
                    .getLength());
    }

    @Test
    public void singleLine_textChangeForwardsInnerEdits() {
        LocalizedMcEditor editor = new LocalizedMcEditor(false);
        editor.setActiveLocale("fr_FR");
        editor.setText("Bonjour");

        List<String> received = new ArrayList<>();
        editor.addTextChangeListener(received::add);

        editor.setTextAsUserInput("Salut");

        assertFalse(received.isEmpty(), "user-input replacement should fire the text listener");
        assertEquals("Salut", received.get(received.size() - 1));
    }

    @Test
    public void multiLine_constructsUsableEditor() {
        LocalizedMcEditor editor = new LocalizedMcEditor(true);
        editor.setText("line1\\nline2");
        assertEquals("line1\\nline2", editor.getText());
    }

    @Test
    public void enToggle_swapsBodyAndPreservesEditingValue() {
        LocalizedMcEditor editor = new LocalizedMcEditor(false);
        editor.setEnglishValueSupplier(() -> Optional.of("Hello"));
        editor.setActiveLocale("fr_FR");
        editor.setText("Bonjour");

        enToggle(editor).doClick();

        McWysiwygPane en = enPane(editor).orElseThrow();
        assertEquals("Hello", en.getText(), "EN viewer shows english value");
        assertFalse(en.isEditable(), "EN viewer is read-only");
        assertEquals("Bonjour", editor.getText(), "locale editor still holds the editing-locale value");

        enToggle(editor).doClick();

        assertTrue(enPane(editor).isEmpty(), "EN viewer is detached after exit");
        assertEquals("Bonjour", editor.getText(), "locale editor untouched across EN round-trip");
        assertTrue(editor.isEditable(), "locale editor is editable");
    }

    @Test
    public void enToggle_doesNotEngageWhenSupplierReturnsEmpty() {
        LocalizedMcEditor editor = new LocalizedMcEditor(false);
        editor.setActiveLocale("fr_FR");
        editor.setText("Bonjour");

        enToggle(editor).doClick();

        assertTrue(enPane(editor).isEmpty(), "EN view stays closed when no English source is available");
        assertFalse(enToggle(editor).isSelected(), "toggle snaps back to unselected");
        assertEquals("Bonjour", editor.getText(), "locale editor untouched");
    }

    @Test
    public void enView_doesNotFireListenersAndPreservesLocaleValue() {
        LocalizedMcEditor editor = new LocalizedMcEditor(false);
        editor.setEnglishValueSupplier(() -> Optional.of("Hello"));
        editor.setActiveLocale("fr_FR");
        editor.setText("Bonjour");

        List<String> received = new ArrayList<>();
        editor.addTextChangeListener(received::add);

        enToggle(editor).doClick();

        assertTrue(enPane(editor).isPresent(), "EN view engages when an English source is available");
        assertTrue(received.isEmpty(), "entering EN view must not notify text listeners");
        assertEquals("Bonjour", editor.getText(), "editing-locale value is preserved while EN view is on");
    }

    @Test
    public void setActiveLocale_default_hidesEnToggleAndExitsEnView() {
        LocalizedMcEditor editor = new LocalizedMcEditor(false);
        editor.setEnglishValueSupplier(() -> Optional.of("Hello"));
        editor.setActiveLocale("fr_FR");
        editor.setText("Bonjour");
        enToggle(editor).doClick();
        assertTrue(enPane(editor).isPresent());

        editor.setActiveLocale(LangResolver.DEFAULT_LOCALE);

        assertTrue(enPane(editor).isEmpty(), "switching to default locale closes EN view");
        assertEquals("Bonjour", editor.getText(), "editing-locale value is preserved");
        assertFalse(enToggle(editor).isVisible(), "EN toggle hidden when active locale is en_US");
    }

    @Test
    public void setText_whileEnViewing_updatesLocaleEditorOnly() {
        LocalizedMcEditor editor = new LocalizedMcEditor(false);
        editor.setEnglishValueSupplier(() -> Optional.of("Hello"));
        editor.setActiveLocale("fr_FR");
        editor.setText("Bonjour");
        enToggle(editor).doClick();

        editor.setText("Salut");

        assertEquals(
            "Hello",
            enPane(editor).orElseThrow()
                .getText(),
            "EN viewer still shows English while EN is on");
        assertEquals("Salut", editor.getText(), "locale editor reflects the new editing-locale value");

        enToggle(editor).doClick();

        assertEquals("Salut", editor.getText(), "locale editor value is shown after EN is turned off");
    }

    @Test
    public void rawToggle_appliesToBothPanes_whileEnViewing() throws BadLocationException {
        LocalizedMcEditor editor = new LocalizedMcEditor(false);
        editor.setEnglishValueSupplier(() -> Optional.of("§lHello"));
        editor.setActiveLocale("fr_FR");
        editor.setText("§lBonjour");

        enToggle(editor).doClick();
        rawToggle(editor).doClick();

        McWysiwygPane en = enPane(editor).orElseThrow();
        assertTrue(docText(en).contains("§"), "EN pane shows literal § in raw mode");

        rawToggle(editor).doClick();

        assertFalse(docText(en).contains("§"), "EN pane shows styled text in rendered mode");

        rawToggle(editor).doClick();
        enToggle(editor).doClick();

        assertTrue(docText(mainPane(editor)).contains("§"), "main pane stays in raw mode after EN view exit");
    }
}
