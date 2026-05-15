package net.noiraude.creditseditor.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandStackSnapshot;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class EditorMenuBarTest {

    private static final EditorActions.Handlers NOOP_HANDLERS = new EditorActionsHandlers();

    @TempDir
    Path temp;

    @BeforeEach
    public void requireGraphicsEnvironment() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Swing menu bar tests require a graphics environment");
    }

    // -----------------------------------------------------------------------
    // Save
    // -----------------------------------------------------------------------

    @Test
    public void save_disabledBeforeAnyDirtyEvent() {
        Fixture f = newFixture();
        assertFalse(saveItem(f).isEnabled());
    }

    @Test
    public void save_enabledOnDirtyTrue() {
        Fixture f = newFixture();
        f.bus.fireDirtyChanged(true);
        assertTrue(saveItem(f).isEnabled());
    }

    @Test
    public void save_disabledOnDirtyFalse() {
        Fixture f = newFixture();
        f.bus.fireDirtyChanged(true);
        f.bus.fireDirtyChanged(false);
        assertFalse(saveItem(f).isEnabled());
    }

    // -----------------------------------------------------------------------
    // Save As
    // -----------------------------------------------------------------------

    @Test
    public void saveAs_disabledBeforeSessionLoaded() {
        Fixture f = newFixture();
        assertFalse(saveAsItem(f).isEnabled());
    }

    @Test
    public void saveAs_enabledAfterSessionLoaded() throws Exception {
        Fixture f = newFixture();
        EditorSession session = newSession();
        try {
            f.setSession(session);
            assertTrue(saveAsItem(f).isEnabled());
        } finally {
            session.close();
        }
    }

    // -----------------------------------------------------------------------
    // Undo
    // -----------------------------------------------------------------------

    @Test
    public void undo_disabledBeforeAnyCommandStackEvent_labelBare() {
        Fixture f = newFixture();
        assertFalse(undoItem(f).isEnabled());
        assertEquals("Undo", undoItem(f).getText());
    }

    @Test
    public void undo_disabledWhenStackEmpty_labelBare() {
        Fixture f = newFixture();
        f.bus.fireCommandStackChanged(CommandStackSnapshot.EMPTY);
        assertFalse(undoItem(f).isEnabled());
        assertEquals("Undo", undoItem(f).getText());
    }

    @Test
    public void undo_enabledWhenStackHasUndo_labelCarriesCommandName() {
        Fixture f = newFixture();
        f.bus.fireCommandStackChanged(snapshot(true, false, "Add Category", null));
        assertTrue(undoItem(f).isEnabled());
        assertEquals("Undo Add Category", undoItem(f).getText());
    }

    // -----------------------------------------------------------------------
    // Redo
    // -----------------------------------------------------------------------

    @Test
    public void redo_disabledBeforeAnyCommandStackEvent_labelBare() {
        Fixture f = newFixture();
        assertFalse(redoItem(f).isEnabled());
        assertEquals("Redo", redoItem(f).getText());
    }

    @Test
    public void redo_disabledWhenStackEmpty_labelBare() {
        Fixture f = newFixture();
        f.bus.fireCommandStackChanged(CommandStackSnapshot.EMPTY);
        assertFalse(redoItem(f).isEnabled());
        assertEquals("Redo", redoItem(f).getText());
    }

    @Test
    public void redo_enabledWhenStackHasRedo_labelCarriesCommandName() {
        Fixture f = newFixture();
        f.bus.fireCommandStackChanged(snapshot(false, true, null, "Rename Person"));
        assertTrue(redoItem(f).isEnabled());
        assertEquals("Redo Rename Person", redoItem(f).getText());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    @Contract(" -> new")
    private static @NotNull Fixture newFixture() {
        return new Fixture();
    }

    @Contract(" -> new")
    private @NotNull EditorSession newSession() throws Exception {
        return EditorSession.open(
            Files.createTempDirectory(temp, "session")
                .toString());
    }

    @Contract("_, _, _, _ -> new")
    private static @NotNull CommandStackSnapshot snapshot(boolean canUndo, boolean canRedo, @Nullable String undoName,
        @Nullable String redoName) {
        return new CommandStackSnapshot(canUndo, canRedo, Optional.ofNullable(undoName), Optional.ofNullable(redoName));
    }

    private static @NotNull JMenuItem saveItem(@NotNull Fixture f) {
        return item(f.bar, "File", "Save Resources");
    }

    private static @NotNull JMenuItem saveAsItem(@NotNull Fixture f) {
        return item(f.bar, "File", "Save Resources As");
    }

    private static @NotNull JMenuItem undoItem(@NotNull Fixture f) {
        return item(f.bar, "Edit", "Undo");
    }

    private static @NotNull JMenuItem redoItem(@NotNull Fixture f) {
        return item(f.bar, "Edit", "Redo");
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
        final @NotNull EditorActions actions = new EditorActions(NOOP_HANDLERS, bus);
        final @NotNull EditorMenuBar bar = new EditorMenuBar(actions);

        void setSession(@NotNull EditorSession session) {
            bus.setSession(session.creditsDoc(), session.langDoc());
            bus.fireDirtyChanged(session.isDirty());
            bus.fireCommandStackChanged(CommandStackSnapshot.of(session.stack));
        }
    }

    private static class EditorActionsHandlers implements EditorActions.Handlers {

        @Contract(pure = true)
        @Override
        public void onOpen() {}

        @Contract(pure = true)
        @Override
        public void onNew() {}

        @Contract(pure = true)
        @Override
        public void onSave() {}

        @Contract(pure = true)
        @Override
        public void onSaveAs() {}

        @Contract(pure = true)
        @Override
        public void onQuit() {}

        @Contract(pure = true)
        @Override
        public void onUndo() {}

        @Contract(pure = true)
        @Override
        public void onRedo() {}

        @Contract(pure = true)
        @Override
        public void onShortcuts() {}

        @Contract(pure = true)
        @Override
        public void onAbout() {}
    }
}
