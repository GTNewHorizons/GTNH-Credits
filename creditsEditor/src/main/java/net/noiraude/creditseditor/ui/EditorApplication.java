package net.noiraude.creditseditor.ui;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Composition root: assembles the bus, the session holder, every presenter, and the main window. */
public final class EditorApplication {

    private final @NotNull MainWindow window;
    private final @NotNull OpenPresenter openPresenter;

    public EditorApplication() {
        SessionHolder holder = new SessionHolder();
        DocumentBus bus = new DocumentBus(holder);
        CommandPresenter commandPresenter = new CommandPresenter(bus, holder);

        this.window = new MainWindow(bus, commandPresenter);

        new EditAbortedPresenter(bus, window);
        SavePresenter savePresenter = new SavePresenter(bus, window, holder);
        UnsavedChangesGate gate = new UnsavedChangesGate(window, holder, savePresenter);
        new SaveAsPresenter(bus, window, holder);
        this.openPresenter = new OpenPresenter(bus, window, holder, gate);
        new QuitPresenter(bus, holder, gate);
        new UndoRedoPresenter(bus, holder, commandPresenter);
        new HelpPresenter(bus, window);
        new TitlePresenter(bus, window, holder);
        new ManageLocalesPresenter(bus, window, holder, commandPresenter);
    }

    /** Shows the main window and loads {@code initialPath} if provided. */
    public void start(@Nullable String initialPath) {
        window.setVisible(true);
        if (initialPath != null) openPresenter.loadInitial(initialPath);
    }
}
