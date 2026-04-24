package net.noiraude.creditseditor.ui.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.EnumSet;

import javax.swing.JTextPane;

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
    public void roundTrip_plainText_preserved() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setText("Hello World");
        assertEquals("Hello World", pane.getText());
    }

    @Test
    public void roundTrip_singleColor_preserved() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setText("§aHello");
        assertEquals("§aHello", pane.getText());
    }

    @Test
    public void applyCode_noSelection_togglesPendingCodeCarry() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setText("");
        assertFalse(
            pane.getCaretStyle()
                .contains(McFormatCode.BOLD));

        pane.applyCode(McFormatCode.BOLD, true);
        assertTrue(
            pane.getCaretStyle()
                .contains(McFormatCode.BOLD));

        pane.applyCode(McFormatCode.BOLD, false);
        assertFalse(
            pane.getCaretStyle()
                .contains(McFormatCode.BOLD));
    }

    @Test
    public void pendingCodes_syncFromPrecedingChar_whenCaretBetweenStyledRuns() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setText("§aHello§cWorld");
        JTextPane inner = (JTextPane) pane.getViewport()
            .getView();
        // Document content is "HelloWorld" (codes removed). Caret at position 5
        // sits between "o" (GREEN run) and "W" (RED run); the carry reads attrs
        // from position caret-1 which is the "o", so carry must be GREEN.
        inner.setCaretPosition(5);

        EnumSet<McFormatCode> style = pane.getCaretStyle();
        assertTrue("preceding char 'o' is green", style.contains(McFormatCode.GREEN));
        assertFalse("preceding char is not red", style.contains(McFormatCode.RED));
    }

    @Test
    public void applyCode_onSelection_preservesSelectionBounds() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setText("Hello");
        JTextPane inner = (JTextPane) pane.getViewport()
            .getView();
        inner.select(1, 4); // "ell"

        pane.applyCode(McFormatCode.BOLD, true);

        assertEquals(1, inner.getSelectionStart());
        assertEquals(4, inner.getSelectionEnd());
    }
}
