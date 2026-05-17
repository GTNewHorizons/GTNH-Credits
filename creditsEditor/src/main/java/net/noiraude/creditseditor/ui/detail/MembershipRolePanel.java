package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapTiny;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.LangFieldWriter;
import net.noiraude.creditseditor.command.impl.EditRoleDisplayNameCommand;
import net.noiraude.creditseditor.service.LangResolver;
import net.noiraude.creditseditor.ui.I18n;
import net.noiraude.libcredits.lang.LangKey;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sub-panel shown in {@link PersonDetailView} below the membership table. Composes a
 * {@link RoleListPanel} with a {@link RoleDetailCard} and mediates role selection between
 * them.
 */
public final class MembershipRolePanel extends JPanel {

    private final @NotNull DocumentBus bus;
    private final @NotNull CommandExecutor onCommand;
    private final @NotNull RoleListPanel roleListPanel;
    private final @NotNull RoleDetailCard roleDetailCard;

    private @NotNull Optional<LangKey> currentRoleKey = Optional.empty();
    private @NotNull String shadowRoleDisplayName = "";

    public MembershipRolePanel(@NotNull DocumentBus bus, @NotNull CommandExecutor onCommand) {
        this.bus = bus;
        this.onCommand = onCommand;
        this.roleListPanel = new RoleListPanel(bus, onCommand);
        this.roleDetailCard = new RoleDetailCard(e -> onRoleNameEditFired());
        this.roleDetailCard.setEnglishValueSupplier(this::englishDisplayName);
        this.roleDetailCard.setActiveLocale(bus.activeLocale());

        setBorder(BorderFactory.createTitledBorder(I18n.get("panel.memberships.roles.title")));
        setLayout(new GridBagLayout());
        assembleLayout();

        roleListPanel.setSelectionListener(this::refreshDetailForm);
        bus.addListener(DocumentBus.TOPIC_LOCALE, e -> onLocaleChanged());
        bus.addListener(DocumentBus.TOPIC_LANG, e -> {
            Object nv = e.getNewValue();
            if (nv != null) onLangChanged(nv.toString());
        });
    }

    @Override
    public @NotNull Dimension getMinimumSize() {
        Dimension min = super.getMinimumSize();
        Insets borderInsets = getInsets();
        Dimension listMin = roleListPanel.getMinimumSize();
        int rowSpacing = gapTiny * 2;
        int floorH = borderInsets.top + borderInsets.bottom + rowSpacing + listMin.height;
        return new Dimension(min.width, Math.max(min.height, floorH));
    }

    /** Loads the role list for {@code membership} on {@code person}. */
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
        if (selected.size() != 1) {
            currentRoleKey = Optional.empty();
            shadowRoleDisplayName = "";
            roleDetailCard.clear();
            return;
        }
        String role = selected.getFirst();
        String langKeyStr = RoleListPanel.langKeyForRole(role);
        LangKey langKey = new LangKey(langKeyStr);
        String displayName = resolveDisplayName(langKey);
        currentRoleKey = Optional.of(langKey);
        shadowRoleDisplayName = displayName;
        roleDetailCard.load(role, langKeyStr, displayName);
    }

    private void onLocaleChanged() {
        roleDetailCard.setActiveLocale(bus.activeLocale());
        refreshDetailForm();
    }

    private void onLangChanged(@NotNull String changedKey) {
        currentRoleKey.ifPresent(key -> {
            if (!changedKey.equals(key.key())) return;
            String resolved = resolveDisplayName(key);
            if (resolved.equals(roleDetailCard.getDisplayNameText())) {
                shadowRoleDisplayName = resolved;
                return;
            }
            roleDetailCard.setDisplayNameSilently(resolved);
            shadowRoleDisplayName = resolved;
        });
    }

    private void onRoleNameEditFired() {
        if (!bus.hasSession()) return;
        currentRoleKey.ifPresent(
            key -> bus.langDoc(bus.activeLocale())
                .ifPresent(target -> {
                    String newValue = roleDetailCard.getDisplayNameText();
                    String oldValue = shadowRoleDisplayName;
                    shadowRoleDisplayName = newValue;
                    onCommand.execute(
                        EditRoleDisplayNameCommand.create(LangFieldWriter.ofBus(bus, target, key), oldValue, newValue));
                }));
    }

    private @NotNull String resolveDisplayName(@NotNull LangKey key) {
        String activeLocale = bus.activeLocale();
        Optional<String> active = lookup(activeLocale, key);
        if (active.isPresent()) return active.get();
        if (LangResolver.DEFAULT_LOCALE.equals(activeLocale)) return "";
        return lookup(LangResolver.DEFAULT_LOCALE, key).orElse("");
    }

    private @NotNull Optional<String> lookup(@NotNull String locale, @NotNull LangKey key) {
        return bus.langDoc(locale)
            .flatMap(key::read)
            .filter(v -> !v.isEmpty());
    }

    private @NotNull Optional<String> englishDisplayName() {
        return currentRoleKey.flatMap(key -> lookup(LangResolver.DEFAULT_LOCALE, key));
    }
}
