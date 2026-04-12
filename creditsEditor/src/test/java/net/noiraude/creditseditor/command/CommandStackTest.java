package net.noiraude.creditseditor.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unused")
public class CommandStackTest {

    private CommandStack stack;
    private List<String> log;

    @Before
    public void setUp() {
        stack = new CommandStack();
        log = new ArrayList<>();
    }

    private Command cmd(String name) {
        return new LoggingCommand(name, log);
    }

    private static final class LoggingCommand implements Command {

        private final String name;
        private final List<String> log;

        LoggingCommand(String name, List<String> log) {
            this.name = name;
            this.log = log;
        }

        @Override
        public void execute() {
            log.add("do:" + name);
        }

        @Override
        public void undo() {
            log.add("undo:" + name);
        }

        @Override
        public String getDisplayName() {
            return name;
        }
    }

    // -----------------------------------------------------------------------
    // execute
    // -----------------------------------------------------------------------

    @Test
    public void execute_callsExecuteOnCommand() {
        stack.execute(cmd("A"));
        assertEquals(List.of("do:A"), log);
    }

    @Test
    public void execute_enablesUndo() {
        stack.execute(cmd("A"));
        assertTrue(stack.canUndo());
    }

    @Test
    public void execute_doesNotEnableRedo() {
        stack.execute(cmd("A"));
        assertFalse(stack.canRedo());
    }

    // -----------------------------------------------------------------------
    // undo
    // -----------------------------------------------------------------------

    @Test
    public void undo_callsUndoOnCommand() {
        stack.execute(cmd("A"));
        stack.undo();
        assertEquals(List.of("do:A", "undo:A"), log);
    }

    @Test
    public void undo_enablesRedo() {
        stack.execute(cmd("A"));
        stack.undo();
        assertTrue(stack.canRedo());
    }

    @Test
    public void undo_emptyStack_throws() {
        try {
            stack.undo();
        } catch (IllegalStateException e) {
            return;
        }
        throw new AssertionError("Expected IllegalStateException");
    }

    @Test
    public void undo_multipleCommands_lifoOrder() {
        stack.execute(cmd("A"));
        stack.execute(cmd("B"));
        stack.undo();
        stack.undo();
        assertEquals(List.of("do:A", "do:B", "undo:B", "undo:A"), log);
    }

    // -----------------------------------------------------------------------
    // redo
    // -----------------------------------------------------------------------

    @Test
    public void redo_callsExecuteAgain() {
        stack.execute(cmd("A"));
        stack.undo();
        stack.redo();
        assertEquals(List.of("do:A", "undo:A", "do:A"), log);
    }

    @Test
    public void redo_emptyStack_throws() {
        try {
            stack.redo();
        } catch (IllegalStateException e) {
            return;
        }
        throw new AssertionError("Expected IllegalStateException");
    }

    @Test
    public void redo_afterRedo_redoStackDecreases() {
        stack.execute(cmd("A"));
        stack.execute(cmd("B"));
        stack.undo();
        stack.undo();
        assertTrue(stack.canRedo());
        stack.redo();
        assertTrue(stack.canRedo());
        stack.redo();
        assertFalse(stack.canRedo());
    }

    // -----------------------------------------------------------------------
    // execute clears redo stack
    // -----------------------------------------------------------------------

    @Test
    public void execute_afterUndo_clearsRedoStack() {
        stack.execute(cmd("A"));
        stack.undo();
        assertTrue(stack.canRedo());
        stack.execute(cmd("B"));
        assertFalse("redo stack should be cleared after new execute", stack.canRedo());
    }

    @Test
    public void execute_afterUndo_newCommandExecuted() {
        stack.execute(cmd("A"));
        stack.undo();
        stack.execute(cmd("B"));
        assertEquals(List.of("do:A", "undo:A", "do:B"), log);
    }

    // -----------------------------------------------------------------------
    // peekUndoName / peekRedoName
    // -----------------------------------------------------------------------

    @Test
    public void peekUndoName_returnsTopCommandName() {
        stack.execute(cmd("Alpha"));
        stack.execute(cmd("Beta"));
        assertEquals("Beta", stack.peekUndoName());
    }

    @Test
    public void peekUndoName_emptyStack_returnsNull() {
        assertNull(stack.peekUndoName());
    }

    @Test
    public void peekRedoName_afterUndo_returnsCommandName() {
        stack.execute(cmd("Alpha"));
        stack.undo();
        assertEquals("Alpha", stack.peekRedoName());
    }

    @Test
    public void peekRedoName_emptyStack_returnsNull() {
        assertNull(stack.peekRedoName());
    }

    // -----------------------------------------------------------------------
    // dirty state
    // -----------------------------------------------------------------------

    @Test
    public void newStack_isNotDirty() {
        assertFalse(stack.isDirty());
    }

    @Test
    public void afterExecute_isDirty() {
        stack.execute(cmd("A"));
        assertTrue(stack.isDirty());
    }

    @Test
    public void afterMarkClean_isNotDirty() {
        stack.execute(cmd("A"));
        stack.markClean();
        assertFalse(stack.isDirty());
    }

    @Test
    public void afterMarkClean_thenUndo_isDirty() {
        stack.execute(cmd("A"));
        stack.markClean();
        stack.undo();
        assertTrue(stack.isDirty());
    }

    @Test
    public void undoBackToCleanMark_isNotDirty() {
        stack.execute(cmd("A"));
        stack.execute(cmd("B"));
        stack.markClean();
        stack.execute(cmd("C"));
        assertTrue(stack.isDirty());
        stack.undo(); // undo C, back at clean mark
        assertFalse(stack.isDirty());
    }

    @Test
    public void executeAfterUndo_invalidatesCleanMark() {
        stack.execute(cmd("A"));
        stack.markClean();
        stack.undo();
        stack.execute(cmd("B")); // diverges from saved state
        assertTrue(stack.isDirty());
        // Cannot undo back to clean: clean mark was cleared
        stack.undo(); // undo B
        assertTrue("clean mark was invalidated, should still be dirty", stack.isDirty());
    }

    @Test
    public void redoBackToCleanMark_isNotDirty() {
        stack.execute(cmd("A"));
        stack.execute(cmd("B"));
        stack.markClean();
        stack.undo();
        assertTrue(stack.isDirty());
        stack.redo(); // back at clean mark
        assertFalse(stack.isDirty());
    }

    // -----------------------------------------------------------------------
    // clear
    // -----------------------------------------------------------------------

    @Test
    public void clear_emptiesBothStacks() {
        stack.execute(cmd("A"));
        stack.undo();
        stack.clear();
        assertFalse(stack.canUndo());
        assertFalse(stack.canRedo());
    }

    @Test
    public void clear_resetsCleanMark() {
        stack.execute(cmd("A"));
        stack.clear();
        assertFalse(stack.isDirty());
    }
}
