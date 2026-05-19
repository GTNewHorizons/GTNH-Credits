package net.noiraude.creditseditor.ui;

import java.awt.Frame;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import javax.swing.JOptionPane;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;

/** Persists the active session to a user-chosen location. */
final class SaveAsPresenter {

    private final @NotNull DocumentBus bus;
    private final @NotNull Frame owner;
    private final @NotNull SessionHolder holder;

    SaveAsPresenter(@NotNull DocumentBus bus, @NotNull Frame owner, @NotNull SessionHolder holder) {
        this.bus = Objects.requireNonNull(bus);
        this.owner = Objects.requireNonNull(owner);
        this.holder = Objects.requireNonNull(holder);
        bus.addListener(DocumentBus.TOPIC_REQUEST_SAVE_AS, e -> onSaveAs());
    }

    private void onSaveAs() {
        holder.ifPresent(this::chooseAndWrite);
    }

    private void chooseAndWrite(@NotNull EditorSession session) {
        CreditsResourceChooser.chooseForSaveAs(owner)
            .ifPresent(target -> writeTo(session, target));
    }

    private void writeTo(@NotNull EditorSession session, @NotNull Path target) {
        if (isNonEmptyTarget(target) && !confirmOverwrite(target)) return;
        try {
            session.saveAs(target.toString());
        } catch (Exception ex) {
            ErrorPresenter.show(owner, I18n.get("dialog.save_as.error.title"), ex);
            return;
        }
        bus.fireDirtyChanged(session.isDirty());
    }

    private static boolean isNonEmptyTarget(@NotNull Path target) {
        if (!Files.exists(target)) return false;
        if (Files.isRegularFile(target)) return true;
        if (!Files.isDirectory(target)) return false;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(target)) {
            return entries.iterator()
                .hasNext();
        } catch (IOException ex) {
            return true;
        }
    }

    private boolean confirmOverwrite(@NotNull Path target) {
        int choice = JOptionPane.showConfirmDialog(
            owner,
            I18n.get(
                "dialog.save_as.confirm.message",
                MsgArg.text(
                    target.getFileName()
                        .toString())),
            I18n.get("dialog.save_as.confirm.title"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }
}
