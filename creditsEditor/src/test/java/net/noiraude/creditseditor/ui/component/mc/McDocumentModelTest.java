package net.noiraude.creditseditor.ui.component.mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import javax.swing.text.StyledDocument;

import net.noiraude.creditseditor.mc.McFormatCode;

import org.junit.jupiter.api.Test;

@SuppressWarnings("SpellCheckingInspection")
public class McDocumentModelTest {

    @Test
    public void roundTrip_plainText_preserved() throws javax.swing.text.BadLocationException {
        McDocumentModel model = new McDocumentModel(true);
        model.setStyledText("Hello World");
        assertEquals("Hello World", model.getText());
    }

    @Test
    public void roundTrip_singleColor_preserved() throws javax.swing.text.BadLocationException {
        McDocumentModel model = new McDocumentModel(true);
        model.setStyledText("§aHello");
        assertEquals("§aHello", model.getText());
    }

    @Test
    public void roundTrip_multipleParagraphs_preserved() throws javax.swing.text.BadLocationException {
        McDocumentModel model = new McDocumentModel(true);
        model.setStyledText("§aLine one\n§bLine two");
        assertEquals("§aLine one\n§bLine two", model.getText());
    }

    @Test
    public void setPlainText_preservesVerbatim() throws javax.swing.text.BadLocationException {
        McDocumentModel model = new McDocumentModel(true);
        model.setPlainText("§aHello");
        assertEquals("§aHello", model.getText());
    }

    @Test
    public void applyCode_noSelection_togglesPendingCarry() throws javax.swing.text.BadLocationException {
        McDocumentModel model = new McDocumentModel(true);
        model.setStyledText("");
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
    public void applyReset_noSelection_clearsCarry() throws javax.swing.text.BadLocationException {
        McDocumentModel model = new McDocumentModel(true);
        model.setStyledText("");
        model.applyCode(McFormatCode.BOLD, true, 0, 0);
        model.applyCode(McFormatCode.RED, true, 0, 0);

        model.applyReset(0, 0);

        assertTrue(
            model.getPendingCodes()
                .isEmpty());
    }

    @Test
    public void applyColorReset_noSelection_keepsModifiers() throws javax.swing.text.BadLocationException {
        McDocumentModel model = new McDocumentModel(true);
        model.setStyledText("");
        model.applyCode(McFormatCode.BOLD, true, 0, 0);
        model.applyCode(McFormatCode.RED, true, 0, 0);

        model.applyColorReset(0, 0);

        EnumSet<McFormatCode> codes = model.getPendingCodes();
        assertTrue(codes.contains(McFormatCode.BOLD), "BOLD modifier survives color reset");
        assertFalse(codes.contains(McFormatCode.RED), "RED color cleared by color reset");
    }

    @Test
    public void syncPendingFromCaret_readsPrecedingChar() throws javax.swing.text.BadLocationException {
        McDocumentModel model = new McDocumentModel(true);
        model.setStyledText("§aHello§cWorld");
        // Document content is "HelloWorld" (codes removed). Caret at position 5 sits between
        // the last green char ('o') and the first red char ('W'); syncing from caret should
        // read attrs at position 4 ('o'), so the carry must be GREEN.
        model.syncPendingFromCaret(5);

        EnumSet<McFormatCode> codes = model.getPendingCodes();
        assertTrue(codes.contains(McFormatCode.GREEN), "preceding char 'o' is green");
        assertFalse(codes.contains(McFormatCode.RED), "preceding char is not red");
    }

    @Test
    public void getCaretStyle_noSelection_readsDocumentDirectly() throws javax.swing.text.BadLocationException {
        McDocumentModel model = new McDocumentModel(true);
        model.setStyledText("§lHello§rWorld");
        // Caret at position 3 sits in the bold "Hello" run. getCaretStyle reads the document
        // directly (without consulting the carry), so it must return BOLD even when the carry
        // has not yet been synced.
        EnumSet<McFormatCode> codes = model.getCaretStyle(3, 3);
        assertTrue(codes.contains(McFormatCode.BOLD));
    }

    @Test
    public void getCaretStyle_withSelection_readsAtSelectionStart() throws javax.swing.text.BadLocationException {
        McDocumentModel model = new McDocumentModel(true);
        model.setStyledText("§lHello§rWorld");
        // Selection [3, 8) starts inside the bold run; getCaretStyle should report BOLD.
        EnumSet<McFormatCode> codes = model.getCaretStyle(3, 8);
        assertTrue(codes.contains(McFormatCode.BOLD));
    }

    @Test
    public void applyCode_onSelection_writesAttributesToDocument() throws javax.swing.text.BadLocationException {
        McDocumentModel model = new McDocumentModel(true);
        model.setStyledText("Hello");
        StyledDocument doc = model.getDocument();

        model.applyCode(McFormatCode.BOLD, true, 1, 4);

        EnumSet<McFormatCode> middle = McSwingStyle.fromAttributes(
            doc.getCharacterElement(2)
                .getAttributes());
        assertTrue(middle.contains(McFormatCode.BOLD), "middle of selected range carries BOLD");
        EnumSet<McFormatCode> outside = McSwingStyle.fromAttributes(
            doc.getCharacterElement(0)
                .getAttributes());
        assertFalse(outside.contains(McFormatCode.BOLD), "char before selection does not carry BOLD");
    }
}
