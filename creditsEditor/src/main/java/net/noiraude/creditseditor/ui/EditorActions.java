package net.noiraude.creditseditor.ui;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;

/** Registry of every editor action instance, shared by the menu bar and the toolbar. */
final class EditorActions {

    final @NotNull OpenAction open;
    final @NotNull NewAction newDoc;
    final @NotNull SaveAction save;
    final @NotNull SaveAsAction saveAs;
    final @NotNull QuitAction quit;
    final @NotNull UndoAction undo;
    final @NotNull RedoAction redo;
    final @NotNull AddLocaleAction addLocale;
    final @NotNull ShortcutsAction shortcuts;
    final @NotNull AboutAction about;

    EditorActions(@NotNull DocumentBus bus) {
        open = new OpenAction(bus);
        newDoc = new NewAction(bus);
        save = new SaveAction(bus);
        saveAs = new SaveAsAction(bus);
        quit = new QuitAction(bus);
        undo = new UndoAction(bus);
        redo = new RedoAction(bus);
        addLocale = new AddLocaleAction(bus);
        shortcuts = new ShortcutsAction(bus);
        about = new AboutAction(bus);
    }
}
