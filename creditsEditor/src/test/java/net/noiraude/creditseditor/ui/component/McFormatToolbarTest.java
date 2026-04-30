package net.noiraude.creditseditor.ui.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.GraphicsEnvironment;

import javax.swing.JToggleButton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class McFormatToolbarTest {

    @BeforeEach
    public void requireGraphicsEnvironment() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Swing toolbar tests require a graphics environment");
    }

    @Test
    public void boldButton_selected_whenCaretIsInBoldRunAtConnectTime() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setText("§lHello§rWorld");
        pane.setCaretPosition(3);

        McFormatToolbar toolbar = new McFormatToolbar();
        pane.connectToolbar(toolbar);

        JToggleButton bold = findModifierButton(toolbar, "B");
        assertTrue(bold.isSelected(), "BOLD button should light up for a caret sitting in a bold run");
    }

    @Test
    public void boldButton_notSelected_whenCaretIsInPlainRunAtConnectTime() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setText("§lHello§rWorld");
        pane.setCaretPosition(8); // inside plain "World"

        McFormatToolbar toolbar = new McFormatToolbar();
        pane.connectToolbar(toolbar);

        JToggleButton bold = findModifierButton(toolbar, "B");
        assertFalse(bold.isSelected(), "BOLD button should not light up when the caret is in plain text");
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
        assertFalse(bold.isSelected(), "BOLD button must reflect the plain initial caret position");

        // Move the caret into the bold run after the toolbar is connected. The toolbar reads
        // the model directly via getCaretStyle, so its button reflects the new position
        // regardless of caret-listener ordering.
        pane.setCaretPosition(3);

        assertTrue(bold.isSelected(), "BOLD button must light up after caret moves into the bold run");
    }

    @Test
    public void italicButton_selected_whenCaretIsInItalicRunAtConnectTime() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setText("§oHello");
        pane.setCaretPosition(3);

        McFormatToolbar toolbar = new McFormatToolbar();
        pane.connectToolbar(toolbar);

        JToggleButton italic = findModifierButton(toolbar, "I");
        assertTrue(italic.isSelected(), "ITALIC button should light up for a caret sitting in an italic run");
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
