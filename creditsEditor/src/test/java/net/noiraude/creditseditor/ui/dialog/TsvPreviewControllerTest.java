package net.noiraude.creditseditor.ui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

public class TsvPreviewControllerTest {

    @Test
    public void causeMessageOf_prefersCauseMessage() {
        Throwable cause = new RuntimeException("inner boom");
        ExecutionException ex = new ExecutionException("outer boom", cause);
        assertEquals("inner boom", TsvPreviewController.causeMessageOf(ex));
    }

    @Test
    public void causeMessageOf_fallsBackToOuterMessageWhenCauseIsNull() {
        ExecutionException ex = new ExecutionException("outer boom", null);
        assertEquals("outer boom", TsvPreviewController.causeMessageOf(ex));
    }

    @Test
    public void causeMessageOf_returnsPlaceholderWhenAllMessagesNull() {
        ExecutionException ex = new ExecutionException(null, null);
        assertEquals("unknown error", TsvPreviewController.causeMessageOf(ex));
    }
}
