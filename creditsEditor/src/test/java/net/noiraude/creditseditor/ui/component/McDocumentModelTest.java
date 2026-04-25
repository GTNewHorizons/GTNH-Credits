package net.noiraude.creditseditor.ui.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;

import javax.swing.text.StyledDocument;

import net.noiraude.creditseditor.mc.McFormatCode;

import org.junit.Test;

public class McDocumentModelTest {

    @Test
    public void roundTrip_plainText_preserved() {
        McDocumentModel model = new McDocumentModel(true);
        model.setText("Hello World");
        assertEquals("Hello World", model.getText());
    }

    @Test
    public void roundTrip_singleColor_preserved() {
        McDocumentModel model = new McDocumentModel(true);
        model.setText("§aHello");
        assertEquals("§aHello", model.getText());
    }

    @Test
    public void roundTrip_multipleParagraphs_preserved() {
        McDocumentModel model = new McDocumentModel(true);
        model.setText("§aLine one\n§bLine two");
        assertEquals("§aLine one\n§bLine two", model.getText());
    }

    @Test
    public void rawMode_returnsLiteralText() {
        McDocumentModel model = new McDocumentModel(true);
        model.setText("§aHello");
        model.setRawMode(true);
        assertEquals("§aHello", model.getText());
    }

    @Test
    public void applyCode_noSelection_togglesPendingCarry() {
        McDocumentModel model = new McDocumentModel(true);
        model.setText("");
        assertFalse(
            model.getPendingCodes()
                .contains(McFormatCode.BOLD));

        model.applyCode(McFormatCode.BOLD, true, 0, 0);
        assertTrue(
            model.getPendingCodes()
                .contains(McFormatCode.BOLD));

        model.applyCode(McFormatCode.BOLD, false, 0, 0);
        assertFalse(
            model.getPendingCodes()
                .contains(McFormatCode.BOLD));
    }

    @Test
    public void applyReset_noSelection_clearsCarry() {
        McDocumentModel model = new McDocumentModel(true);
        model.setText("");
        model.applyCode(McFormatCode.BOLD, true, 0, 0);
        model.applyCode(McFormatCode.RED, true, 0, 0);

        model.applyReset(0, 0);

        assertTrue(
            model.getPendingCodes()
                .isEmpty());
    }

    @Test
    public void applyColorReset_noSelection_keepsModifiers() {
        McDocumentModel model = new McDocumentModel(true);
        model.setText("");
        model.applyCode(McFormatCode.BOLD, true, 0, 0);
        model.applyCode(McFormatCode.RED, true, 0, 0);

        model.applyColorReset(0, 0);

        EnumSet<McFormatCode> codes = model.getPendingCodes();
        assertTrue("BOLD modifier survives color reset", codes.contains(McFormatCode.BOLD));
        assertFalse("RED color cleared by color reset", codes.contains(McFormatCode.RED));
    }

    @Test
    public void syncPendingFromCaret_readsPrecedingChar() {
        McDocumentModel model = new McDocumentModel(true);
        model.setText("§aHello§cWorld");
        // Document content is "HelloWorld" (codes removed). Caret at position 5 sits between
        // the last green char ('o') and the first red char ('W'); syncing from caret should
        // read attrs at position 4 ('o'), so the carry must be GREEN.
        model.syncPendingFromCaret(5);

        EnumSet<McFormatCode> codes = model.getPendingCodes();
        assertTrue("preceding char 'o' is green", codes.contains(McFormatCode.GREEN));
        assertFalse("preceding char is not red", codes.contains(McFormatCode.RED));
    }

    @Test
    public void getCaretStyle_noSelection_readsDocumentDirectly() {
        McDocumentModel model = new McDocumentModel(true);
        model.setText("§lHello§rWorld");
        // Caret at position 3 sits in the bold "Hello" run. getCaretStyle reads the document
        // directly (without consulting the carry), so it must return BOLD even when the carry
        // has not yet been synced.
        EnumSet<McFormatCode> codes = model.getCaretStyle(3, 3);
        assertTrue(codes.contains(McFormatCode.BOLD));
    }

    @Test
    public void getCaretStyle_withSelection_readsAtSelectionStart() {
        McDocumentModel model = new McDocumentModel(true);
        model.setText("§lHello§rWorld");
        // Selection [3, 8) starts inside the bold run; getCaretStyle should report BOLD.
        EnumSet<McFormatCode> codes = model.getCaretStyle(3, 8);
        assertTrue(codes.contains(McFormatCode.BOLD));
    }

    @Test
    public void applyCode_onSelection_writesAttributesToDocument() {
        McDocumentModel model = new McDocumentModel(true);
        model.setText("Hello");
        StyledDocument doc = model.getDocument();

        model.applyCode(McFormatCode.BOLD, true, 1, 4);

        EnumSet<McFormatCode> middle = McSwingStyle.fromAttributes(
            doc.getCharacterElement(2)
                .getAttributes());
        assertTrue("middle of selected range carries BOLD", middle.contains(McFormatCode.BOLD));
        EnumSet<McFormatCode> outside = McSwingStyle.fromAttributes(
            doc.getCharacterElement(0)
                .getAttributes());
        assertFalse("char before selection does not carry BOLD", outside.contains(McFormatCode.BOLD));
    }
}
