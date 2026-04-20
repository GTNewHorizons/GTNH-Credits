package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.impl.AddMembershipCommand;
import net.noiraude.creditseditor.command.impl.AddPersonRoleCommand;
import net.noiraude.creditseditor.command.impl.CompoundCommand;
import net.noiraude.creditseditor.command.impl.RemoveMembershipCommand;
import net.noiraude.creditseditor.command.impl.RemovePersonCommand;
import net.noiraude.creditseditor.mc.McText;
import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Detail panel card shown when multiple persons are selected.
 *
 * <p>
 * Displays the selection count and provides bulk operation buttons:
 * assign to a category, add a role in the category, remove from the category, and delete all.
 * Each operation dispatches a single {@link CompoundCommand} through the executor.
 *
 * <p>
 * Subscribes to {@link DocumentBus#TOPIC_PERSONS} to re-resolve the currently loaded
 * selection by name; signals back through {@code onResolved} so the owning detail panel
 * can collapse to a single-person view or empty when appropriate.
 */
public final class BulkPersonView extends JPanel {

    private final @NotNull DocumentBus bus;
    private final @NotNull CommandExecutor onCommand;
    private final @NotNull Consumer<List<DocumentPerson>> onResolved;
    private @Nullable DocumentCategory selectedCategory;
    private @NotNull List<DocumentPerson> persons = List.of();

    private final @NotNull JLabel countLabel = new JLabel();

    public BulkPersonView(@NotNull DocumentBus bus, @NotNull CommandExecutor onCommand,
        @NotNull Consumer<List<DocumentPerson>> onResolved) {
        this.bus = bus;
        this.onCommand = onCommand;
        this.onResolved = onResolved;
        setLayout(new BorderLayout());

        countLabel.setFont(
            countLabel.getFont()
                .deriveFont(
                    Font.BOLD,
                    countLabel.getFont()
                        .getSize() + 2f));
        countLabel.setHorizontalAlignment(SwingConstants.CENTER);
        countLabel.setBorder(BorderFactory.createEmptyBorder(scaled(12), 0, scaled(16), 0));
        add(countLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, scaled(16), scaled(16), scaled(16)));

        JLabel bulkLabel = new JLabel("Bulk operations:");
        bulkLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bulkLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, scaled(8), 0));
        buttonPanel.add(bulkLabel);

        addActionButton(buttonPanel, "Assign to category...", this::onAssignToCategory);
        buttonPanel.add(Box.createVerticalStrut(scaled(4)));
        addActionButton(buttonPanel, "Add role in category...", this::onAddRoleInCategory);
        buttonPanel.add(Box.createVerticalStrut(scaled(4)));
        addActionButton(buttonPanel, "Remove from category...", this::onRemoveFromCategory);
        buttonPanel.add(Box.createVerticalStrut(scaled(12)));
        addActionButton(buttonPanel, "Delete all", this::onDeleteAll);

        buttonPanel.add(Box.createVerticalGlue());
        add(buttonPanel, BorderLayout.CENTER);

        bus.addListener(DocumentBus.TOPIC_PERSONS, e -> reresolve());
    }

    /** Sets the currently selected category so picker dialogs can default to it. */
    public void setSelectedCategory(@Nullable DocumentCategory category) {
        this.selectedCategory = category;
    }

    /** Loads the given selection into the view. */
    public void load(@NotNull List<DocumentPerson> persons) {
        this.persons = persons;
        countLabel.setText(persons.size() + " persons selected");
    }

    private void reresolve() {
        if (!bus.hasSession() || persons.isEmpty()) return;
        List<String> names = persons.stream()
            .map(p -> p.name)
            .toList();
        List<DocumentPerson> found = bus.creditsDoc().persons.stream()
            .filter(p -> names.contains(p.name))
            .collect(Collectors.toList());
        if (found.size() > 1) load(found);
        onResolved.accept(found);
    }

    // -----------------------------------------------------------------------
    // Bulk operations
    // -----------------------------------------------------------------------

    private void onAssignToCategory() {
        if (persons.isEmpty() || !bus.hasSession()) return;

        DocumentCategory target = pickCategory("Assign to category");
        if (target == null) return;

        CompoundCommand.Builder builder = new CompoundCommand.Builder(
            "Assign " + persons.size() + " person(s) to " + target.id);

        int added = 0;
        for (DocumentPerson person : persons) {
            boolean alreadyMember = person.memberships.stream()
                .anyMatch(m -> m.categoryId.equals(target.id));
            if (!alreadyMember) {
                builder.add(new AddMembershipCommand(bus, person, new DocumentMembership(target.id)));
                added++;
            }
        }

        if (builder.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "All selected persons are already in category '" + target.id + "'.",
                "No changes",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        onCommand.execute(builder.build());
        JOptionPane.showMessageDialog(
            this,
            added + " person(s) added to '" + target.id + "'.",
            "Assign complete",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void onAddRoleInCategory() {
        if (persons.isEmpty() || !bus.hasSession()) return;

        DocumentCategory target = pickCategory("Add role in category");
        if (target == null) return;

        String role = promptForRole(target);
        if (role == null) return;

        CompoundCommand.Builder builder = new CompoundCommand.Builder("Add role '" + role + "' in " + target.id);
        RoleAddCounts counts = collectRoleAddCommands(builder, target, role);

        if (builder.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "No changes: all eligible persons already have this role.",
                "No changes",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        onCommand.execute(builder.build());
        showRoleAddSummary(counts, target);
    }

    private @Nullable String promptForRole(@NotNull DocumentCategory target) {
        String role = JOptionPane.showInputDialog(this, "Role to add in '" + target.id + "':");
        if (role == null) return null;
        String stripped = role.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    private @NotNull RoleAddCounts collectRoleAddCommands(@NotNull CompoundCommand.Builder builder,
        @NotNull DocumentCategory target, @NotNull String role) {
        int added = 0;
        int skipped = 0;
        for (DocumentPerson person : persons) {
            DocumentMembership membership = person.memberships.stream()
                .filter(m -> m.categoryId.equals(target.id))
                .findFirst()
                .orElse(null);
            if (membership == null) {
                skipped++;
                continue;
            }
            if (membership.roles.contains(role)) continue;
            builder.add(new AddPersonRoleCommand(bus, person, membership, role));
            added++;
        }
        return new RoleAddCounts(added, skipped);
    }

    private void showRoleAddSummary(@NotNull RoleAddCounts counts, @NotNull DocumentCategory target) {
        String message = counts.added() + " person(s) received the role.";
        if (counts.skipped() > 0) {
            message += "\n" + counts.skipped() + " person(s) skipped (not in category '" + target.id + "').";
        }
        JOptionPane.showMessageDialog(this, message, "Role added", JOptionPane.INFORMATION_MESSAGE);
    }

    private record RoleAddCounts(int added, int skipped) {}

    private void onRemoveFromCategory() {
        if (persons.isEmpty() || !bus.hasSession()) return;

        // Collect categories where at least one selected person appears
        Set<String> presentIds = new LinkedHashSet<>();
        for (DocumentPerson person : persons) {
            for (DocumentMembership m : person.memberships) {
                presentIds.add(m.categoryId);
            }
        }
        if (presentIds.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Selected persons have no category memberships.",
                "Nothing to remove",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<DocumentCategory> available = bus.creditsDoc().categories.stream()
            .filter(c -> presentIds.contains(c.id))
            .collect(Collectors.toList());
        DocumentCategory target = pickFromList(available, "Remove from category");
        if (target == null) return;

        CompoundCommand.Builder builder = new CompoundCommand.Builder(
            "Remove " + persons.size() + " person(s) from " + target.id);

        for (DocumentPerson person : persons) {
            person.memberships.stream()
                .filter(m -> m.categoryId.equals(target.id))
                .findFirst()
                .ifPresent(membership -> builder.add(new RemoveMembershipCommand(bus, person, membership)));
        }

        if (builder.isEmpty()) return;
        onCommand.execute(builder.build());
    }

    private void onDeleteAll() {
        if (persons.isEmpty() || !bus.hasSession()) return;

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete " + persons.size() + " selected person(s)?\nThis cannot be undone without Ctrl+Z.",
            "Confirm delete",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) return;

        CompoundCommand.Builder builder = new CompoundCommand.Builder("Delete " + persons.size() + " person(s)");
        for (DocumentPerson person : persons) {
            builder.add(new RemovePersonCommand(bus, person));
        }
        onCommand.execute(builder.build());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private @Nullable DocumentCategory pickCategory(@NotNull String title) {
        if (!bus.hasSession()) return null;
        return pickFromList(bus.creditsDoc().categories, title);
    }

    private @Nullable DocumentCategory pickFromList(@NotNull List<DocumentCategory> categories, @NotNull String title) {
        if (categories.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No categories available.", title, JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        String[] labels = categories.stream()
            .map(this::categoryLabel)
            .toArray(String[]::new);
        JComboBox<String> combo = new JComboBox<>(labels);
        if (selectedCategory != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).id.equals(selectedCategory.id)) {
                    combo.setSelectedIndex(i);
                    break;
                }
            }
        }
        int result = JOptionPane
            .showConfirmDialog(this, combo, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return null;
        return categories.get(combo.getSelectedIndex());
    }

    private @NotNull String categoryLabel(@NotNull DocumentCategory cat) {
        String displayName = bus.langDoc()
            .get("credits.category." + KeySanitizer.sanitize(cat.id));
        if (displayName == null || displayName.isEmpty()) return cat.id;
        return cat.id + " (" + McText.strip(displayName) + ")";
    }

    private static void addActionButton(@NotNull JPanel panel, @NotNull String text, @NotNull Runnable action) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
        button.addActionListener(e -> action.run());
        panel.add(button);
    }
}
