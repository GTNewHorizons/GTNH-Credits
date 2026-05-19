package net.noiraude.creditseditor.ui;

import java.awt.Frame;
import java.util.Objects;

import javax.swing.JOptionPane;

import org.jetbrains.annotations.NotNull;

/** Confirms what to do with unsaved changes before a destructive flow proceeds. */
final class UnsavedChangesGate {

    private final @NotNull Frame owner;
    private final @NotNull SessionHolder holder;
    private final @NotNull SaveService saveService;

    UnsavedChangesGate(@NotNull Frame owner, @NotNull SessionHolder holder, @NotNull SaveService saveService) {
        this.owner = Objects.requireNonNull(owner);
        this.holder = Objects.requireNonNull(holder);
        this.saveService = Objects.requireNonNull(saveService);
    }

    /** Returns whether the caller should abort. */
    boolean shouldAbort() {
        if (
            holder.get()
                .filter(EditorSession::isDirty)
                .isEmpty()
        ) return false;
        Object[] options = { I18n.get("dialog.unsaved.button.save"), I18n.get("dialog.unsaved.button.discard"),
            I18n.get("button.cancel") };
        int choice = JOptionPane.showOptionDialog(
            owner,
            I18n.get("dialog.unsaved.message"),
            I18n.get("dialog.unsaved.title"),
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[0]);
        return switch (choice) {
            case 0 -> !saveService.trySave();
            case 1 -> false;
            default -> true;
        };
    }
}
