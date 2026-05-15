package net.noiraude.creditseditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandStackSnapshot;

import org.jetbrains.annotations.NotNull;

/** Reverts the most recently applied command. */
final class UndoAction extends AbstractAction {

    private final @NotNull Runnable callback;

    UndoAction(@NotNull DocumentBus bus, @NotNull Runnable callback) {
        super(I18n.get("menu.edit.undo"));
        this.callback = callback;
        String mnemonic = I18n.get("menu.edit.undo.mnemonic");
        if (mnemonic.length() == 1) {
            putValue(Action.MNEMONIC_KEY, (int) Character.toUpperCase(mnemonic.charAt(0)));
        }
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        setEnabled(false);
        bus.addListener(DocumentBus.TOPIC_COMMAND_STACK, e -> {
            CommandStackSnapshot snapshot = (CommandStackSnapshot) e.getNewValue();
            setEnabled(snapshot.canUndo());
            putValue(
                Action.NAME,
                snapshot.undoName()
                    .map(name -> I18n.get("menu.edit.undo.named", MsgArg.text(name)))
                    .orElseGet(() -> I18n.get("menu.edit.undo")));
        });
    }

    @Override
    public void actionPerformed(@NotNull ActionEvent e) {
        callback.run();
    }
}
