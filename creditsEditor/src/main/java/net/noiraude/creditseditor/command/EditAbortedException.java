package net.noiraude.creditseditor.command;

import org.jetbrains.annotations.NotNull;

/**
 * Signals that an in-progress edit operation could not complete and the caller should treat
 * the action as aborted.
 *
 * <p>
 * Thrown by the document model bridges (via {@link net.noiraude.creditseditor.ui.component})
 * when an underlying Swing API rejects the operation, and caught by {@link CommandStack#execute}
 * so the failed command never reaches the undo stack. The top-level event handler in the main
 * window converts it into a user-visible dialog.
 */
public final class EditAbortedException extends RuntimeException {

    public EditAbortedException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }
}
