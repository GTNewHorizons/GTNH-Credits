package net.noiraude.creditseditor.ui.component.mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import javax.swing.TransferHandler;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class McStyledTransferHandlerTest {

    @BeforeEach
    public void requireGraphicsEnvironment() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Swing components require a graphics environment");
    }

    private static McWysiwygPane newPane() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setTransferHandler(new McStyledTransferHandler());
        return pane;
    }

    private static String exported(McWysiwygPane pane) throws Exception {
        Transferable t = invokeCreateTransferable(pane);
        return (String) t.getTransferData(DataFlavor.stringFlavor);
    }

    private static Transferable invokeCreateTransferable(McWysiwygPane pane) throws Exception {
        var m = TransferHandler.class.getDeclaredMethod("createTransferable", javax.swing.JComponent.class);
        m.setAccessible(true);
        return (Transferable) m.invoke(pane.getTransferHandler(), pane);
    }

    @Test
    public void copy_emitsSectionCodes() throws Exception {
        McWysiwygPane pane = newPane();
        pane.setStyledText("§lhello§r world");
        pane.select(
            0,
            pane.getDocument()
                .getLength());

        assertEquals("§lhello§r world", exported(pane));
    }

    @Test
    public void copy_emptySelection_emitsEmptyString() throws Exception {
        McWysiwygPane pane = newPane();
        pane.setStyledText("hello");

        assertEquals("", exported(pane));
    }

    @Test
    public void paste_appliesAttributes() {
        McWysiwygPane pane = newPane();
        pane.setStyledText("");
        TransferHandler.TransferSupport support = new TransferHandler.TransferSupport(
            pane,
            new StringSelection("§lhello§r world"));

        assertTrue(
            pane.getTransferHandler()
                .importData(support));

        StyledDocument doc = (StyledDocument) pane.getDocument();
        Element first = doc.getCharacterElement(0);
        assertTrue(StyleConstants.isBold(first.getAttributes()), "first run is bold");
        Element later = doc.getCharacterElement(doc.getLength() - 1);
        assertFalse(StyleConstants.isBold(later.getAttributes()), "trailing run is not bold");
    }

    @Test
    public void paste_restoresInputAttributes() {
        McWysiwygPane pane = newPane();
        pane.setStyledText("§oitalic");
        pane.setCaretPosition(3);

        AttributeSet preInput = pane.getInputAttributes()
            .copyAttributes();
        assertTrue(StyleConstants.isItalic(preInput), "caret inside an italic run carries italic input");

        TransferHandler.TransferSupport support = new TransferHandler.TransferSupport(
            pane,
            new StringSelection("§lbold"));
        assertTrue(
            pane.getTransferHandler()
                .importData(support));

        AttributeSet postInput = pane.getInputAttributes();
        assertTrue(StyleConstants.isItalic(postInput), "italic restored after paste");
        assertFalse(StyleConstants.isBold(postInput), "bold from pasted segment does not leak into input");
    }

    @Test
    public void paste_intoReadOnlyPane_isRefused() {
        McWysiwygPane pane = newPane();
        pane.setEditable(false);
        TransferHandler.TransferSupport support = new TransferHandler.TransferSupport(pane, new StringSelection("x"));

        assertFalse(
            pane.getTransferHandler()
                .canImport(support));
    }

    @Test
    public void copy_fromReadOnlyPane_works() throws Exception {
        McWysiwygPane pane = newPane();
        pane.setStyledText("§ared");
        pane.setEditable(false);
        pane.select(
            0,
            pane.getDocument()
                .getLength());

        assertEquals(
            TransferHandler.COPY,
            pane.getTransferHandler()
                .getSourceActions(pane));
        assertEquals("§ared", exported(pane));
    }
}
