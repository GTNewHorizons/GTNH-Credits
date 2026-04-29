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

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.ui.EditorMenuBar.EditActions;
import net.noiraude.creditseditor.ui.EditorMenuBar.FileActions;
import net.noiraude.creditseditor.ui.EditorMenuBar.HelpActions;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class EditorMenuBarTest {

    private static final Runnable NOOP = () -> {};

    @TempDir
    Path temp;

    @BeforeEach
    public void requireGraphicsEnvironment() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Swing menu bar tests require a graphics environment");
    }

    @Test
    public void refreshNull_disablesAllStatefulItems_andResetsLabels() {
        EditorMenuBar bar = newBar();
        bar.refresh(null);

        assertFalse(item(bar, "File", "Save").isEnabled(), "Save must be disabled with no session");
        assertFalse(item(bar, "Edit", "Undo").isEnabled(), "Undo must be disabled with no session");
        assertFalse(item(bar, "Edit", "Redo").isEnabled(), "Redo must be disabled with no session");
        assertEquals("Undo", item(bar, "Edit", "Undo").getText());
        assertEquals("Redo", item(bar, "Edit", "Redo").getText());
    }

    @Test
    public void refreshWithEmptySession_enablesSaveOnly() throws Exception {
        EditorSession session = newSession();
        try {
            EditorMenuBar bar = newBar();
            bar.refresh(session);

            assertTrue(item(bar, "File", "Save").isEnabled(), "Save must be enabled whenever a session is loaded");
            assertFalse(item(bar, "Edit", "Undo").isEnabled(), "Undo must be disabled with an empty command stack");
            assertFalse(item(bar, "Edit", "Redo").isEnabled(), "Redo must be disabled with an empty command stack");
        } finally {
            session.close();
        }
    }

    @Test
    public void afterExecute_undoEnabled_withCommandNameInLabel() throws Exception {
        EditorSession session = newSession();
        try {
            session.stack.execute(new NamedCommand("Add Category"));

            EditorMenuBar bar = newBar();
            bar.refresh(session);

            JMenuItem undo = item(bar, "Edit", "Undo");
            assertTrue(undo.isEnabled(), "Undo must enable after executing a command");
            assertEquals("Undo Add Category", undo.getText());
            assertFalse(item(bar, "Edit", "Redo").isEnabled(), "Redo must remain disabled until something is undone");
        } finally {
            session.close();
        }
    }

    @Test
    public void afterUndo_redoEnabled_withCommandNameInLabel() throws Exception {
        EditorSession session = newSession();
        try {
            session.stack.execute(new NamedCommand("Rename Person"));
            session.stack.undo();

            EditorMenuBar bar = newBar();
            bar.refresh(session);

            assertFalse(item(bar, "Edit", "Undo").isEnabled(), "Undo must be disabled when the undo stack is empty");
            JMenuItem redo = item(bar, "Edit", "Redo");
            assertTrue(redo.isEnabled(), "Redo must enable after undoing a command");
            assertEquals("Redo Rename Person", redo.getText());
        } finally {
            session.close();
        }
    }

    @Test
    public void refreshAfterStateChange_picksUpNewCommandStackState() throws Exception {
        EditorSession session = newSession();
        try {
            EditorMenuBar bar = newBar();
            bar.refresh(session);
            assertFalse(item(bar, "Edit", "Undo").isEnabled());

            session.stack.execute(new NamedCommand("Move Role"));
            bar.refresh(session);
            assertTrue(item(bar, "Edit", "Undo").isEnabled(), "Undo must reflect post-execute state once refreshed");
            assertEquals("Undo Move Role", item(bar, "Edit", "Undo").getText());

            session.stack.undo();
            bar.refresh(session);
            assertFalse(item(bar, "Edit", "Undo").isEnabled());
            assertTrue(item(bar, "Edit", "Redo").isEnabled());
            assertEquals("Redo Move Role", item(bar, "Edit", "Redo").getText());
        } finally {
            session.close();
        }
    }

    private @NotNull EditorMenuBar newBar() {
        return new EditorMenuBar(
            new FileActions(NOOP, NOOP, NOOP, NOOP),
            new EditActions(NOOP, NOOP),
            new HelpActions(NOOP, NOOP));
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
