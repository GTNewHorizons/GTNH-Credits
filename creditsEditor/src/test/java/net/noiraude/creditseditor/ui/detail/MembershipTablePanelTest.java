package net.noiraude.creditseditor.ui.detail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JTable;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandStack;
import net.noiraude.creditseditor.command.impl.AddMembershipCommand;
import net.noiraude.creditseditor.command.impl.RemoveMembershipCommand;
import net.noiraude.libcredits.lang.LangParser;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class MembershipTablePanelTest {

    private static final Runnable NOOP = () -> {};

    @Before
    public void requireGraphicsEnvironment() {
        Assume.assumeFalse("Swing JTable requires a graphics environment", GraphicsEnvironment.isHeadless());
    }

    @Test
    public void addButton_invokesOnAddCallback() {
        AtomicInteger addCount = new AtomicInteger();
        MembershipTablePanel panel = new MembershipTablePanel(addCount::incrementAndGet, NOOP);
        button(panel, "Add").doClick();
        assertEquals(1, addCount.get());
    }

    @Test
    public void removeButton_invokesOnRemoveCallback() {
        AtomicInteger removeCount = new AtomicInteger();
        MembershipTablePanel panel = new MembershipTablePanel(NOOP, removeCount::incrementAndGet);
        button(panel, "Remove").doClick();
        assertEquals(1, removeCount.get());
    }

    @Test
    public void setMemberships_addRow_appearsInTable() {
        MembershipTablePanel panel = new MembershipTablePanel(NOOP, NOOP);
        List<DocumentMembership> memberships = new ArrayList<>();
        memberships.add(new DocumentMembership("team"));
        panel.setMembershipsPreservingSelection(memberships, null);
        assertEquals(1, table(panel).getRowCount());

        memberships.add(new DocumentMembership("dev"));
        panel.setMembershipsPreservingSelection(memberships, null);
        assertEquals(2, table(panel).getRowCount());
        assertEquals("dev", table(panel).getValueAt(1, 0));
    }

    @Test
    public void setMemberships_removeRow_disappearsFromTable() {
        MembershipTablePanel panel = new MembershipTablePanel(NOOP, NOOP);
        List<DocumentMembership> memberships = new ArrayList<>();
        memberships.add(new DocumentMembership("team"));
        memberships.add(new DocumentMembership("dev"));
        panel.setMembershipsPreservingSelection(memberships, null);
        assertEquals(2, table(panel).getRowCount());

        memberships.removeFirst();
        panel.setMembershipsPreservingSelection(memberships, null);
        assertEquals(1, table(panel).getRowCount());
        assertEquals("dev", table(panel).getValueAt(0, 0));
    }

    @Test
    public void setMemberships_reorder_rowsReflectNewOrder_andSelectionFollowsCategory() {
        MembershipTablePanel panel = new MembershipTablePanel(NOOP, NOOP);
        DocumentMembership team = new DocumentMembership("team");
        DocumentMembership dev = new DocumentMembership("dev");
        panel.setMembershipsPreservingSelection(new ArrayList<>(List.of(team, dev)), "dev");
        assertEquals(1, panel.getSelectedRow());

        panel.setMembershipsPreservingSelection(new ArrayList<>(List.of(dev, team)), "dev");
        assertEquals("dev", table(panel).getValueAt(0, 0));
        assertEquals("team", table(panel).getValueAt(1, 0));
        assertEquals("selection follows the category id across reorder", 0, panel.getSelectedRow());
    }

    @Test
    public void undoOfAddMembership_restoresPreviousTableContents() {
        DocumentBus bus = newBus();
        CommandStack stack = new CommandStack();
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("team"));

        MembershipTablePanel panel = new MembershipTablePanel(NOOP, NOOP);
        bus.addListener(DocumentBus.TOPIC_PERSON, reloadOn(panel, alice));
        panel.setMembershipsPreservingSelection(alice.memberships, null);
        assertEquals(1, table(panel).getRowCount());

        stack.execute(new AddMembershipCommand(bus, alice, new DocumentMembership("dev")));
        assertEquals(2, table(panel).getRowCount());
        assertEquals("dev", table(panel).getValueAt(1, 0));

        stack.undo();
        assertEquals(1, table(panel).getRowCount());
        assertEquals("team", table(panel).getValueAt(0, 0));
    }

    @Test
    public void undoOfRemoveMembership_restoresRowAtOriginalIndex() {
        DocumentBus bus = newBus();
        CommandStack stack = new CommandStack();
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("team"));
        alice.memberships.add(new DocumentMembership("dev"));
        alice.memberships.add(new DocumentMembership("contrib"));

        MembershipTablePanel panel = new MembershipTablePanel(NOOP, NOOP);
        bus.addListener(DocumentBus.TOPIC_PERSON, reloadOn(panel, alice));
        panel.setMembershipsPreservingSelection(alice.memberships, null);
        assertEquals(3, table(panel).getRowCount());

        DocumentMembership middle = alice.memberships.get(1);
        stack.execute(new RemoveMembershipCommand(bus, alice, middle));
        assertEquals(2, table(panel).getRowCount());
        assertEquals("team", table(panel).getValueAt(0, 0));
        assertEquals("contrib", table(panel).getValueAt(1, 0));

        stack.undo();
        assertEquals(3, table(panel).getRowCount());
        assertEquals("dev", table(panel).getValueAt(1, 0));
        assertTrue("undo restores the same membership instance", alice.memberships.get(1) == middle);
    }

    private static @NotNull DocumentBus newBus() {
        DocumentBus bus = new DocumentBus();
        bus.setSession(CreditsDocument.empty(), LangParser.empty());
        return bus;
    }

    private static @NotNull PropertyChangeListener reloadOn(@NotNull MembershipTablePanel panel,
        @NotNull DocumentPerson target) {
        return evt -> {
            if (evt.getNewValue() == target) {
                panel.setMembershipsPreservingSelection(target.memberships, null);
            }
        };
    }

    private static @NotNull JButton button(@NotNull Container root, @NotNull String label) {
        for (Component c : descendants(root)) {
            if (c instanceof JButton b && label.equals(b.getText())) return b;
        }
        throw new AssertionError("button not found: " + label);
    }

    private static @NotNull JTable table(@NotNull Container root) {
        for (Component c : descendants(root)) {
            if (c instanceof JTable t) return t;
        }
        throw new AssertionError("JTable not found");
    }

    private static @NotNull List<Component> descendants(@NotNull Container root) {
        List<Component> out = new ArrayList<>();
        collect(root, out);
        return out;
    }

    private static void collect(@NotNull Container c, @NotNull List<Component> out) {
        for (Component child : c.getComponents()) {
            out.add(child);
            if (child instanceof Container container) collect(container, out);
        }
    }
}
