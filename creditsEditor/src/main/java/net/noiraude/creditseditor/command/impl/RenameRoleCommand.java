package net.noiraude.creditseditor.command.impl;

import java.util.ArrayList;
import java.util.List;

import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

/**
 * Replaces all occurrences of a role string across the entire document and renames
 * the associated lang key.
 *
 * <p>
 * If the target role already exists in the lang file, the source lang key is simply
 * removed (the target keeps its own display name). If the target does not have a lang
 * entry, the source's display name is moved to the target's key.
 */
public final class RenameRoleCommand extends AbstractStructuralCommand {

    private final CreditsDocument creditsDoc;
    private final LangDocument langDoc;
    private final String sourceRole;
    private final String targetRole;

    /** Tracks each replacement site so undo can restore it precisely. */
    private final List<ReplacementSite> sites = new ArrayList<>();
    private String savedLangValue;
    private boolean langKeyMoved;

    public RenameRoleCommand(CreditsDocument creditsDoc, LangDocument langDoc, String sourceRole, String targetRole) {
        this.creditsDoc = creditsDoc;
        this.langDoc = langDoc;
        this.sourceRole = sourceRole;
        this.targetRole = targetRole;
    }

    @Override
    public void execute() {
        sites.clear();

        // Replace all occurrences in the document
        for (DocumentPerson person : creditsDoc.persons) {
            for (DocumentMembership membership : person.memberships) {
                for (int i = 0; i < membership.roles.size(); i++) {
                    if (
                        membership.roles.get(i)
                            .equals(sourceRole)
                    ) {
                        // Skip if the target role already exists in this membership
                        if (membership.roles.contains(targetRole)) {
                            sites.add(new ReplacementSite(membership, i, true));
                            membership.roles.remove(i);
                            i--;
                        } else {
                            sites.add(new ReplacementSite(membership, i, false));
                            membership.roles.set(i, targetRole);
                        }
                    }
                }
            }
        }

        // Handle lang key
        String sourceKey = "credits.person.role." + KeySanitizer.sanitize(sourceRole);
        String targetKey = "credits.person.role." + KeySanitizer.sanitize(targetRole);
        savedLangValue = langDoc != null ? langDoc.get(sourceKey) : null;

        if (langDoc != null && savedLangValue != null) {
            langDoc.remove(sourceKey);
            if (!langDoc.contains(targetKey)) {
                langDoc.set(targetKey, savedLangValue);
                langKeyMoved = true;
            } else {
                langKeyMoved = false;
            }
        } else {
            langKeyMoved = false;
        }
    }

    @Override
    public void undo() {
        // Restore lang key
        if (langDoc != null && savedLangValue != null) {
            String sourceKey = "credits.person.role." + KeySanitizer.sanitize(sourceRole);
            String targetKey = "credits.person.role." + KeySanitizer.sanitize(targetRole);
            if (langKeyMoved) {
                langDoc.remove(targetKey);
            }
            langDoc.set(sourceKey, savedLangValue);
        }

        // Restore all replacement sites in reverse order
        for (int i = sites.size() - 1; i >= 0; i--) {
            ReplacementSite site = sites.get(i);
            if (site.wasDuplicate) {
                site.membership.roles.add(site.index, sourceRole);
            } else {
                site.membership.roles.set(site.index, sourceRole);
            }
        }

        sites.clear();
    }

    @Override
    public String getDisplayName() {
        return "Rename role '" + sourceRole + "' to '" + targetRole + "'";
    }

    /** Returns the number of persons affected by this rename. */
    public int affectedCount() {
        return sites.size();
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private static final class ReplacementSite {

        final DocumentMembership membership;
        final int index;
        final boolean wasDuplicate;

        ReplacementSite(DocumentMembership membership, int index, boolean wasDuplicate) {
            this.membership = membership;
            this.index = index;
            this.wasDuplicate = wasDuplicate;
        }
    }
}
