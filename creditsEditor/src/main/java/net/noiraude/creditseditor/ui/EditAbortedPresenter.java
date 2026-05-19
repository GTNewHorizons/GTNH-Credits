package net.noiraude.creditseditor.ui;

import java.awt.Frame;
import java.util.Objects;

import javax.swing.JOptionPane;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.EditAbortedException;

import org.jetbrains.annotations.NotNull;

/** Surfaces aborted-edit exceptions to the user. */
final class EditAbortedPresenter {

    private final @NotNull Frame owner;

    EditAbortedPresenter(@NotNull DocumentBus bus, @NotNull Frame owner) {
        this.owner = Objects.requireNonNull(owner);
        bus.addListener(DocumentBus.TOPIC_EDIT_ABORTED, e -> show((EditAbortedException) e.getNewValue()));
    }

    private void show(@NotNull EditAbortedException ex) {
        JOptionPane.showMessageDialog(
            owner,
            I18n.get("dialog.edit_aborted.message", MsgArg.text(ex.getMessage())),
            I18n.get("dialog.edit_aborted.title"),
            JOptionPane.WARNING_MESSAGE);
    }
}
