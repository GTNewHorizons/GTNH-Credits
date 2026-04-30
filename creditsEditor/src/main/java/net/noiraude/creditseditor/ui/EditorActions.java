package net.noiraude.creditseditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bundle of {@link Action} instances backing every {@link EditorMenuBar} entry.
 *
 * <p>
 * Each action carries its label, mnemonic, and accelerator at construction. Dynamic state
 * (Save enablement, Undo/Redo enablement and named-variant labels) is driven by bus
 * subscriptions to {@link DocumentBus#TOPIC_SESSION} and
 * {@link DocumentBus#TOPIC_COMMAND_STACK}; the owning window does not call into this class
 * after construction.
 *
 * <p>
 * Menu items bound to these actions update themselves through Swing's standard
 * {@link java.beans.PropertyChangeListener} wiring on {@link Action} property changes.
 */
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

    private final @NotNull Supplier<@Nullable EditorSession> sessionSupplier;

    EditorActions(@NotNull Handlers h, @NotNull DocumentBus bus,
        @NotNull Supplier<@Nullable EditorSession> sessionSupplier) {
        this.sessionSupplier = sessionSupplier;

        open = make(
            I18n.get("menu.file.open"),
            I18n.get("menu.file.open.mnemonic"),
            KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK),
            h::onOpen);
        newDoc = make(I18n.get("menu.file.new"), I18n.get("menu.file.new.mnemonic"), null, h::onNew);
        save = make(
            I18n.get("menu.file.save"),
            I18n.get("menu.file.save.mnemonic"),
            KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
            h::onSave);
        saveAs = make(
            I18n.get("menu.file.save_as"),
            I18n.get("menu.file.save_as.mnemonic"),
            KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
            h::onSaveAs);
        quit = make(I18n.get("menu.file.quit"), I18n.get("menu.file.quit.mnemonic"), null, h::onQuit);
        undo = make(
            I18n.get("menu.edit.undo"),
            I18n.get("menu.edit.undo.mnemonic"),
            KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK),
            h::onUndo);
        redo = make(
            I18n.get("menu.edit.redo"),
            I18n.get("menu.edit.redo.mnemonic"),
            KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
            h::onRedo);
        shortcuts = make(
            I18n.get("menu.help.shortcuts"),
            I18n.get("menu.help.shortcuts.mnemonic"),
            null,
            h::onShortcuts);
        about = make(
            I18n.get("menu.help.about", AppInfo.name()),
            I18n.get("menu.help.about.mnemonic"),
            null,
            h::onAbout);

        bus.addListener(DocumentBus.TOPIC_SESSION, e -> refresh());
        bus.addListener(DocumentBus.TOPIC_COMMAND_STACK, e -> refresh());

        refresh();
    }

    /**
     * Re-evaluates enablement and dynamic labels against the session returned by the
     * supplier. Visible for tests; production callers use bus events instead.
     */
    void refresh() {
        EditorSession session = sessionSupplier.get();
        boolean loaded = session != null;
        save.setEnabled(loaded);
        saveAs.setEnabled(loaded);

        boolean canUndo = loaded && session.stack.canUndo();
        boolean canRedo = loaded && session.stack.canRedo();
        undo.setEnabled(canUndo);
        redo.setEnabled(canRedo);

        String undoName = loaded ? session.stack.peekUndoName() : null;
        undo.putValue(
            Action.NAME,
            undoName != null ? I18n.get("menu.edit.undo.named", undoName) : I18n.get("menu.edit.undo"));
        String redoName = loaded ? session.stack.peekRedoName() : null;
        redo.putValue(
            Action.NAME,
            redoName != null ? I18n.get("menu.edit.redo.named", redoName) : I18n.get("menu.edit.redo"));
    }

    private static @NotNull Action make(@NotNull String label, @NotNull String mnemonic,
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
