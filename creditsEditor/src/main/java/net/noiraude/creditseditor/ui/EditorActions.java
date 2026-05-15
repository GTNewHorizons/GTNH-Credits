package net.noiraude.creditseditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Composition of every {@link Action} backing the editor's menu and toolbar. */
final class EditorActions {

    /**
     * Sink for menu activations. Implemented by the owning window.
     */
    interface Handlers {

        void onOpen();

        void onNew();

        void onSave();

        void onSaveAs();

        void onQuit();

        void onUndo();

        void onRedo();

        void onShortcuts();

        void onAbout();
    }

    final @NotNull Action open;
    final @NotNull Action newDoc;
    final @NotNull Action save;
    final @NotNull Action saveAs;
    final @NotNull Action quit;
    final @NotNull Action undo;
    final @NotNull Action redo;
    final @NotNull Action shortcuts;
    final @NotNull Action about;

    EditorActions(@NotNull Handlers h, @NotNull DocumentBus bus) {
        open = makeStatic(
            I18n.get("menu.file.open"),
            I18n.get("menu.file.open.mnemonic"),
            KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK),
            h::onOpen);
        newDoc = makeStatic(I18n.get("menu.file.new"), I18n.get("menu.file.new.mnemonic"), null, h::onNew);
        save = new SaveAction(bus, h::onSave);
        saveAs = new SaveAsAction(bus, h::onSaveAs);
        quit = makeStatic(I18n.get("menu.file.quit"), I18n.get("menu.file.quit.mnemonic"), null, h::onQuit);
        undo = new UndoAction(bus, h::onUndo);
        redo = new RedoAction(bus, h::onRedo);
        shortcuts = makeStatic(
            I18n.get("menu.help.shortcuts"),
            I18n.get("menu.help.shortcuts.mnemonic"),
            null,
            h::onShortcuts);
        about = makeStatic(
            I18n.get("menu.help.about", MsgArg.text(AppInfo.name())),
            I18n.get("menu.help.about.mnemonic"),
            null,
            h::onAbout);
    }

    private static @NotNull Action makeStatic(@NotNull String label, @NotNull String mnemonic,
        @Nullable KeyStroke accelerator, @NotNull Runnable callback) {
        AbstractAction action = new AbstractAction(label) {

            @Override
            public void actionPerformed(ActionEvent e) {
                callback.run();
            }
        };
        if (mnemonic.length() == 1) {
            action.putValue(Action.MNEMONIC_KEY, (int) Character.toUpperCase(mnemonic.charAt(0)));
        }
        if (accelerator != null) {
            action.putValue(Action.ACCELERATOR_KEY, accelerator);
        }
        return action;
    }
}
