package net.noiraude.creditseditor.ui.component;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Component;
import java.awt.GraphicsEnvironment;

import javax.swing.JToggleButton;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class McFormatToolbarTest {

    @Before
    public void requireGraphicsEnvironment() {
        Assume.assumeFalse("Swing toolbar tests require a graphics environment", GraphicsEnvironment.isHeadless());
    }

    @Test
    public void boldButton_selected_whenCaretIsInBoldRunAtConnectTime() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setText("§lHello§rWorld");
        pane.setCaretPosition(3);

        McFormatToolbar toolbar = new McFormatToolbar();
        pane.connectToolbar(toolbar);

        JToggleButton bold = findModifierButton(toolbar, "B");
        assertTrue("BOLD button should light up for a caret sitting in a bold run", bold.isSelected());
    }

    @Test
    public void boldButton_notSelected_whenCaretIsInPlainRunAtConnectTime() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setText("§lHello§rWorld");
        pane.setCaretPosition(8); // inside plain "World"

        McFormatToolbar toolbar = new McFormatToolbar();
        pane.connectToolbar(toolbar);

        JToggleButton bold = findModifierButton(toolbar, "B");
        assertFalse("BOLD button should not light up when the caret is in plain text", bold.isSelected());
    }

    @Test
    public void boldButton_updatesAfterCaretMove_whenConnectedBeforeMove() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setText("§lHello§rWorld");
        // Caret starts in the plain "World" run before the toolbar attaches.
        pane.setCaretPosition(8);

        McFormatToolbar toolbar = new McFormatToolbar();
        pane.connectToolbar(toolbar);

        JToggleButton bold = findModifierButton(toolbar, "B");
        assertFalse("BOLD button must reflect the plain initial caret position", bold.isSelected());

        // Move the caret into the bold run after the toolbar is connected. The toolbar reads
        // the model directly via getCaretStyle, so its button reflects the new position
        // regardless of caret-listener ordering.
        pane.setCaretPosition(3);

        assertTrue("BOLD button must light up after caret moves into the bold run", bold.isSelected());
    }

    @Test
    public void italicButton_selected_whenCaretIsInItalicRunAtConnectTime() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setText("§oHello");
        pane.setCaretPosition(3);

        McFormatToolbar toolbar = new McFormatToolbar();
        pane.connectToolbar(toolbar);

        JToggleButton italic = findModifierButton(toolbar, "I");
        assertTrue("ITALIC button should light up for a caret sitting in an italic run", italic.isSelected());
    }

    private static JToggleButton findModifierButton(McFormatToolbar toolbar, String label) {
        for (Component c : toolbar.getComponents()) {
            if (c instanceof JToggleButton btn && label.equals(btn.getText())) {
                return btn;
            }
        }
        fail("modifier button with label '" + label + "' not found in toolbar");
        throw new AssertionError("unreachable");
    }
}
