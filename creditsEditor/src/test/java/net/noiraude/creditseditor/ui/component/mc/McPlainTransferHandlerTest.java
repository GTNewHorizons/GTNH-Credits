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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class McPlainTransferHandlerTest {

    @BeforeEach
    public void requireGraphicsEnvironment() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Swing components require a graphics environment");
    }

    private static McWysiwygPane newPane() {
        McWysiwygPane pane = new McWysiwygPane(true);
        pane.setTransferHandler(new McPlainTransferHandler());
        return pane;
    }

    private static String exported(McWysiwygPane pane) throws Exception {
        var m = TransferHandler.class.getDeclaredMethod("createTransferable", javax.swing.JComponent.class);
        m.setAccessible(true);
        Transferable t = (Transferable) m.invoke(pane.getTransferHandler(), pane);
        return (String) t.getTransferData(DataFlavor.stringFlavor);
    }

    @Test
    public void copy_emitsVerbatim() throws Exception {
        McWysiwygPane pane = newPane();
        pane.setPlainText("§lhello");
        pane.select(
            0,
            pane.getDocument()
                .getLength());

        assertEquals("§lhello", exported(pane));
    }

    @Test
    public void paste_isVerbatim() throws Exception {
        McWysiwygPane pane = newPane();
        pane.setPlainText("");
        TransferHandler.TransferSupport support = new TransferHandler.TransferSupport(
            pane,
            new StringSelection("§lhello"));

        assertTrue(
            pane.getTransferHandler()
                .importData(support));

        String docText = pane.getDocument()
            .getText(
                0,
                pane.getDocument()
                    .getLength());
        assertTrue(docText.contains("§l"), "raw paste keeps literal §l in the document");
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
        pane.setPlainText("§ared");
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
