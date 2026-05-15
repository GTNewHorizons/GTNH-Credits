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

/** Re-applies the most recently undone command. */
final class RedoAction extends AbstractAction {

    private final @NotNull DocumentBus bus;

    RedoAction(@NotNull DocumentBus bus) {
        super(I18n.get("action.redo"));
        this.bus = bus;
        String mnemonic = I18n.get("action.redo.mnemonic");
        if (mnemonic.length() == 1) {
            putValue(Action.MNEMONIC_KEY, (int) Character.toUpperCase(mnemonic.charAt(0)));
        }
        putValue(
            Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        setEnabled(false);
        bus.addListener(DocumentBus.TOPIC_COMMAND_STACK, e -> {
            CommandStackSnapshot snapshot = (CommandStackSnapshot) e.getNewValue();
            setEnabled(snapshot.canRedo());
            putValue(
                Action.NAME,
                snapshot.redoName()
                    .map(name -> I18n.get("action.redo.named", MsgArg.text(name)))
                    .orElseGet(() -> I18n.get("action.redo")));
        });
    }

    @Override
    public void actionPerformed(@NotNull ActionEvent e) {
        bus.fireRedoRequested();
    }
}
