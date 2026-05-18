package net.noiraude.creditseditor.ui.component.mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.noiraude.creditseditor.service.LangResolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LocalizedMcEditorTest {

    @BeforeEach
    public void requireGraphicsEnvironment() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Swing components require a graphics environment");
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

        editor.enToggleForTest()
            .doClick();

        assertTrue(editor.isEnViewing());
        assertEquals(
            "Hello",
            editor.enPaneForTest()
                .orElseThrow()
                .getText(),
            "EN viewer shows english value");
        assertFalse(
            editor.enPaneForTest()
                .orElseThrow()
                .isEditable(),
            "EN viewer is read-only");
        assertEquals("Bonjour", editor.getText(), "locale editor still holds the editing-locale value");

        editor.enToggleForTest()
            .doClick();

        assertFalse(editor.isEnViewing());
        assertEquals("Bonjour", editor.getText(), "locale editor untouched across EN round-trip");
        assertTrue(editor.isEditable(), "locale editor is editable");
    }

    @Test
    public void enToggle_doesNotEngageWhenSupplierReturnsEmpty() {
        LocalizedMcEditor editor = new LocalizedMcEditor(false);
        editor.setActiveLocale("fr_FR");
        editor.setText("Bonjour");

        editor.enToggleForTest()
            .doClick();

        assertFalse(editor.isEnViewing(), "EN view stays closed when no English source is available");
        assertFalse(
            editor.enToggleForTest()
                .isSelected(),
            "toggle snaps back to unselected");
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

        editor.enToggleForTest()
            .doClick();

        assertTrue(editor.isEnViewing(), "EN view engages when an English source is available");
        assertTrue(received.isEmpty(), "entering EN view must not notify text listeners");
        assertEquals("Bonjour", editor.getText(), "editing-locale value is preserved while EN view is on");
    }

    @Test
    public void setActiveLocale_default_hidesEnToggleAndExitsEnView() {
        LocalizedMcEditor editor = new LocalizedMcEditor(false);
        editor.setEnglishValueSupplier(() -> Optional.of("Hello"));
        editor.setActiveLocale("fr_FR");
        editor.setText("Bonjour");
        editor.enToggleForTest()
            .doClick();
        assertTrue(editor.isEnViewing());

        editor.setActiveLocale(LangResolver.DEFAULT_LOCALE);

        assertFalse(editor.isEnViewing(), "switching to default locale closes EN view");
        assertEquals("Bonjour", editor.getText(), "editing-locale value is preserved");
        assertFalse(
            editor.enToggleForTest()
                .isVisible(),
            "EN toggle hidden when active locale is en_US");
    }

    @Test
    public void setText_whileEnViewing_updatesLocaleEditorOnly() {
        LocalizedMcEditor editor = new LocalizedMcEditor(false);
        editor.setEnglishValueSupplier(() -> Optional.of("Hello"));
        editor.setActiveLocale("fr_FR");
        editor.setText("Bonjour");
        editor.enToggleForTest()
            .doClick();

        editor.setText("Salut");

        assertEquals(
            "Hello",
            editor.enPaneForTest()
                .orElseThrow()
                .getText(),
            "EN viewer still shows English while EN is on");
        assertEquals("Salut", editor.getText(), "locale editor reflects the new editing-locale value");

        editor.enToggleForTest()
            .doClick();

        assertEquals("Salut", editor.getText(), "locale editor value is shown after EN is turned off");
    }
}
