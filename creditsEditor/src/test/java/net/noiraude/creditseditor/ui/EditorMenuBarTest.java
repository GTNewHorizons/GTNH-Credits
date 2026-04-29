package net.noiraude.creditseditor.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.Command;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class EditorMenuBarTest {

    private static final EditorActions.Handlers NOOP_HANDLERS = new EditorActions.Handlers() {

        @Override
        public void onOpen() {}

        @Override
        public void onNew() {}

        @Override
        public void onSave() {}

        @Override
        public void onQuit() {}

        @Override
        public void onUndo() {}

        @Override
        public void onRedo() {}

        @Override
        public void onShortcuts() {}

        @Override
        public void onAbout() {}
    };

    @TempDir
    Path temp;

    @BeforeEach
    public void requireGraphicsEnvironment() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Swing menu bar tests require a graphics environment");
    }

    @Test
    public void noSession_allStatefulItemsDisabled_andLabelsAreBare() {
        Fixture f = newFixture();

        assertFalse(item(f.bar, "File", "Save").isEnabled(), "Save must be disabled with no session");
        assertFalse(item(f.bar, "Edit", "Undo").isEnabled(), "Undo must be disabled with no session");
        assertFalse(item(f.bar, "Edit", "Redo").isEnabled(), "Redo must be disabled with no session");
        assertEquals("Undo", item(f.bar, "Edit", "Undo").getText());
        assertEquals("Redo", item(f.bar, "Edit", "Redo").getText());
    }

    @Test
    public void emptySessionLoaded_enablesSaveOnly() throws Exception {
        Fixture f = newFixture();
        EditorSession session = newSession();
        try {
            f.setSession(session);

            assertTrue(item(f.bar, "File", "Save").isEnabled(), "Save must be enabled whenever a session is loaded");
            assertFalse(item(f.bar, "Edit", "Undo").isEnabled(), "Undo must be disabled with an empty command stack");
            assertFalse(item(f.bar, "Edit", "Redo").isEnabled(), "Redo must be disabled with an empty command stack");
        } finally {
            session.close();
        }
    }

    @Test
    public void afterExecute_undoEnabled_withCommandNameInLabel() throws Exception {
        Fixture f = newFixture();
        EditorSession session = newSession();
        try {
            f.setSession(session);
            session.stack.execute(new NamedCommand("Add Category"));
            f.bus.fireCommandStackChanged();

            JMenuItem undo = item(f.bar, "Edit", "Undo");
            assertTrue(undo.isEnabled(), "Undo must enable after executing a command");
            assertEquals("Undo Add Category", undo.getText());
            assertFalse(item(f.bar, "Edit", "Redo").isEnabled(), "Redo must remain disabled until something is undone");
        } finally {
            session.close();
        }
    }

    @Test
    public void afterUndo_redoEnabled_withCommandNameInLabel() throws Exception {
        Fixture f = newFixture();
        EditorSession session = newSession();
        try {
            f.setSession(session);
            session.stack.execute(new NamedCommand("Rename Person"));
            session.stack.undo();
            f.bus.fireCommandStackChanged();

            assertFalse(item(f.bar, "Edit", "Undo").isEnabled(), "Undo must be disabled when the undo stack is empty");
            JMenuItem redo = item(f.bar, "Edit", "Redo");
            assertTrue(redo.isEnabled(), "Redo must enable after undoing a command");
            assertEquals("Redo Rename Person", redo.getText());
        } finally {
            session.close();
        }
    }

    @Test
    public void busEvents_drivePostExecuteAndPostUndoTransitions() throws Exception {
        Fixture f = newFixture();
        EditorSession session = newSession();
        try {
            f.setSession(session);
            assertFalse(item(f.bar, "Edit", "Undo").isEnabled());

            session.stack.execute(new NamedCommand("Move Role"));
            f.bus.fireCommandStackChanged();
            assertTrue(item(f.bar, "Edit", "Undo").isEnabled(), "Undo must reflect post-execute state once published");
            assertEquals("Undo Move Role", item(f.bar, "Edit", "Undo").getText());

            session.stack.undo();
            f.bus.fireCommandStackChanged();
            assertFalse(item(f.bar, "Edit", "Undo").isEnabled());
            assertTrue(item(f.bar, "Edit", "Redo").isEnabled());
            assertEquals("Redo Move Role", item(f.bar, "Edit", "Redo").getText());
        } finally {
            session.close();
        }
    }

    private static @NotNull Fixture newFixture() {
        return new Fixture();
    }

    private @NotNull EditorSession newSession() throws Exception {
        return EditorSession.open(
            Files.createTempDirectory(temp, "session")
                .toString());
    }

    private static @NotNull JMenuItem item(@NotNull EditorMenuBar bar, @NotNull String menuName,
        @NotNull String labelPrefix) {
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu menu = bar.getMenu(i);
            if (menu == null || !menuName.equals(menu.getText())) continue;
            for (int j = 0; j < menu.getItemCount(); j++) {
                JMenuItem child = menu.getItem(j);
                if (
                    child != null && child.getText() != null
                        && child.getText()
                            .startsWith(labelPrefix)
                ) {
                    return child;
                }
            }
        }
        throw new AssertionError("menu item not found: " + menuName + " / " + labelPrefix);
    }

    private static final class Fixture {

        final @NotNull DocumentBus bus = new DocumentBus();
        final EditorSession[] sessionHolder = new EditorSession[] { null };
        final @NotNull EditorActions actions = new EditorActions(NOOP_HANDLERS, bus, () -> sessionHolder[0]);
        final @NotNull EditorMenuBar bar = new EditorMenuBar(actions);

        void setSession(@NotNull EditorSession session) {
            sessionHolder[0] = session;
            bus.setSession(session.creditsDoc(), session.langDoc());
        }
    }

    private record NamedCommand(@NotNull String name) implements Command {

        @Override
        public void execute() {}

        @Override
        public void undo() {}

        @Override
        public @NotNull String getDisplayName() {
            return name;
        }
    }
}
