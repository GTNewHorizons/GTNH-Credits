package net.noiraude.creditseditor.ui.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.EnumSet;

import net.noiraude.creditseditor.mc.McFormatCode;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class McWysiwygPaneTest {

    @Before
    public void requireGraphicsEnvironment() {
        Assume.assumeFalse("Swing JTextPane requires a graphics environment", GraphicsEnvironment.isHeadless());
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
        assertTrue("preceding char 'o' is green", style.contains(McFormatCode.GREEN));
    }
}
