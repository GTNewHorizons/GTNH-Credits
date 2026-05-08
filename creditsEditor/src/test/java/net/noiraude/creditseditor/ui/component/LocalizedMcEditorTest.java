package net.noiraude.creditseditor.ui.component;

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

        editor.inner()
            .setTextAsUserInput("Salut");

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
    public void enToggle_swapsToEnglishAndPreservesEditingValue() {
        LocalizedMcEditor editor = new LocalizedMcEditor(false);
        editor.setEnglishValueSupplier(() -> Optional.of("Hello"));
        editor.setActiveLocale("fr_FR");
        editor.setText("Bonjour");

        editor.enToggleForTest()
            .doClick();

        assertTrue(editor.isEnViewing());
        assertEquals(
            "Hello",
            editor.inner()
                .getText(),
            "EN view shows english value");
        assertFalse(
            editor.inner()
                .isEditable(),
            "EN view is read-only");
        assertEquals("Bonjour", editor.getText(), "wrapper still exposes the editing-locale value through the buffer");

        editor.enToggleForTest()
            .doClick();

        assertFalse(editor.isEnViewing());
        assertEquals(
            "Bonjour",
            editor.inner()
                .getText(),
            "editing-locale value restored on EN-off");
        assertTrue(
            editor.inner()
                .isEditable(),
            "editing mode is editable again");
        assertEquals("Bonjour", editor.getText());
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
        assertEquals(
            "Bonjour",
            editor.inner()
                .getText(),
            "inner editor untouched");
    }

    @Test
    public void copy_replacesEditingValueWithEnglishAndExitsEnView() {
        LocalizedMcEditor editor = new LocalizedMcEditor(false);
        editor.setEnglishValueSupplier(() -> Optional.of("Hello"));
        editor.setActiveLocale("fr_FR");
        editor.setText("Bonjour");

        List<String> received = new ArrayList<>();
        editor.addTextChangeListener(received::add);

        editor.enToggleForTest()
            .doClick();
        assertTrue(
            editor.copyButtonForTest()
                .isVisible(),
            "Copy button revealed by EN toggle");
        editor.copyButtonForTest()
            .doClick();

        assertFalse(editor.isEnViewing(), "Copy turns EN view off");
        assertEquals("Hello", editor.getText(), "editing-locale value is replaced with the English source");
        assertFalse(received.isEmpty(), "Copy fires the text listener so observers see the change");
        assertEquals("Hello", received.get(received.size() - 1));
    }

    @Test
    public void setActiveLocale_default_hidesEnControlsAndExitsEnView() {
        LocalizedMcEditor editor = new LocalizedMcEditor(false);
        editor.setEnglishValueSupplier(() -> Optional.of("Hello"));
        editor.setActiveLocale("fr_FR");
        editor.setText("Bonjour");
        editor.enToggleForTest()
            .doClick();
        assertTrue(editor.isEnViewing());

        editor.setActiveLocale(LangResolver.DEFAULT_LOCALE);

        assertFalse(editor.isEnViewing(), "switching to default locale closes EN view");
        assertEquals(
            "Bonjour",
            editor.inner()
                .getText(),
            "editing-locale value is restored");
        assertFalse(
            editor.enToggleForTest()
                .isVisible(),
            "EN toggle hidden when active locale is en_US");
        assertFalse(
            editor.copyButtonForTest()
                .isVisible(),
            "Copy button hidden when active locale is en_US");
    }

    @Test
    public void setText_whileEnViewing_updatesBufferOnly() {
        LocalizedMcEditor editor = new LocalizedMcEditor(false);
        editor.setEnglishValueSupplier(() -> Optional.of("Hello"));
        editor.setActiveLocale("fr_FR");
        editor.setText("Bonjour");
        editor.enToggleForTest()
            .doClick();

        editor.setText("Salut");

        assertEquals(
            "Hello",
            editor.inner()
                .getText(),
            "inner editor still shows English while EN is on");
        assertEquals("Salut", editor.getText(), "wrapper buffer reflects the new editing-locale value");

        editor.enToggleForTest()
            .doClick();

        assertEquals(
            "Salut",
            editor.inner()
                .getText(),
            "buffered value is shown after EN is turned off");
    }
}
