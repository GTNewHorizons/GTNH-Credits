package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapTiny;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.impl.DocumentEditCommand;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sub-panel shown in {@link PersonDetailView} below the membership table.
 *
 * <p>
 * Composes a {@link RoleListPanel} (top) with a {@link RoleDetailCard} (bottom) and mediates
 * selection between them: when the list selection settles, the parent loads or clears the
 * detail card based on how many roles are selected.
 */
public final class MembershipRolePanel extends JPanel {

    private final @NotNull DocumentBus bus;
    private final @NotNull RoleListPanel roleListPanel;
    private final @NotNull RoleDetailCard roleDetailCard;

    public MembershipRolePanel(@NotNull DocumentBus bus, @NotNull CommandExecutor onCommand) {
        this.bus = bus;
        this.roleListPanel = new RoleListPanel(bus, onCommand);
        this.roleDetailCard = new RoleDetailCard(
            this::onDisplayNameChanged,
            e -> onCommand.execute(new DocumentEditCommand("Edit role display name", e.getEdit())));

        setBorder(BorderFactory.createTitledBorder("Roles"));
        setLayout(new GridBagLayout());
        assembleLayout();

        roleListPanel.setSelectionListener(this::refreshDetailForm);
    }

    /**
     * Floors the panel height so the role list portion stays visible. The detail card is
     * allowed to collapse below its preferred height when space is tight.
     */
    @Override
    public Dimension getMinimumSize() {
        Dimension min = super.getMinimumSize();
        Insets borderInsets = getInsets();
        Dimension listMin = roleListPanel.getMinimumSize();
        int rowSpacing = gapTiny * 2; // single inter-row gap between list panel and detail card
        int floorH = borderInsets.top + borderInsets.bottom + rowSpacing + listMin.height;
        return new Dimension(min.width, Math.max(min.height, floorH));
    }

    /**
     * Sets the current person and loads the role list for {@code membership}, or resets to
     * the placeholder state when {@code membership} is {@code null}.
     */
    public void load(@Nullable DocumentPerson person, @Nullable DocumentMembership membership) {
        roleListPanel.load(person, membership);
    }

    /** Rebuilds the role list from the current membership, preserving the selection. */
    public void refresh() {
        roleListPanel.refresh();
    }

    private void assembleLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(gapTiny, gapSmall, gapTiny, gapSmall);

        gbc.gridy = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        add(roleListPanel, gbc);

        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        add(roleDetailCard.panel, gbc);
    }

    private void refreshDetailForm() {
        List<String> selected = roleListPanel.getSelectedRoles();
        if (selected.size() == 1) {
            String role = selected.getFirst();
            String langKey = RoleListPanel.langKeyForRole(role);
            String stored = bus.hasSession() ? bus.langDoc()
                .get(langKey) : null;
            roleDetailCard.load(role, langKey, stored);
        } else {
            roleDetailCard.clear();
        }
    }

    private void onDisplayNameChanged(@NotNull String value) {
        if (!bus.hasSession()) return;
        List<String> sel = roleListPanel.getSelectedRoles();
        if (sel.size() != 1) return;
        String langKey = RoleListPanel.langKeyForRole(sel.getFirst());
        if (value.isEmpty()) bus.langDoc()
            .remove(langKey);
        else bus.langDoc()
            .set(langKey, value);
        bus.fireLangChanged(langKey);
    }
}
