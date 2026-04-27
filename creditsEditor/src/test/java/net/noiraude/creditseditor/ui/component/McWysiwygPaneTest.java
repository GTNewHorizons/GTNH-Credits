package net.noiraude.creditseditor.ui.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.EnumSet;

import net.noiraude.creditseditor.mc.McFormatCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class McWysiwygPaneTest {

    @BeforeEach
    public void requireGraphicsEnvironment() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Swing JTextPane requires a graphics environment");
    }

    @Test
    public void setText_thenGetText_delegatesToModel() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setText("§aHello");
        assertEquals("§aHello", pane.getText());
    }

    @Test
    public void applyCode_onSelection_preservesSelectionBounds() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setText("Hello");
        pane.select(1, 4); // "ell"

        pane.applyCode(McFormatCode.BOLD, true);

        assertEquals(1, pane.getSelectionStart());
        assertEquals(4, pane.getSelectionEnd());
    }

    @Test
    public void caretMove_syncsPendingForInputAttributes() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setText("§aHello§cWorld");
        // Caret at position 5 sits between green 'o' (pos 4) and red 'W' (pos 5). Moving the
        // caret triggers the pane's listener, which syncs the carry from caret-1 ('o') and
        // flushes it to the input-attributes map. getCaretStyle reads the document directly
        // and must reflect GREEN.
        pane.setCaretPosition(5);

        EnumSet<McFormatCode> style = pane.getCaretStyle();
        assertTrue(style.contains(McFormatCode.GREEN), "preceding char 'o' is green");
    }
}
