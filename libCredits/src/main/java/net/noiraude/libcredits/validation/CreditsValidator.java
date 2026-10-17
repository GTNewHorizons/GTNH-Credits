package net.noiraude.libcredits.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.noiraude.libcredits.lang.DetailLangKey;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.CreditsKeys;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.model.DocumentRole;

/** Structural and source-content checks shared by the reader, editor and headless validator. */
public final class CreditsValidator {

    private CreditsValidator() {}

    /** Checks a document independently of any installed Minecraft resources. Does not mutate it. */
    public static List<String> validate(CreditsDocument document) {
        List<String> errors = new ArrayList<>();
        Set<String> categories = new HashSet<>();
        Map<String, String> categoryKeys = new LinkedHashMap<>();
        Map<String, String> roleKeys = new LinkedHashMap<>();
        if (document.categories.isEmpty()) errors.add("at least one category is required");
        for (DocumentCategory category : document.categories) {
            if (!CreditsKeys.isValidId(category.id)) errors.add("invalid category id: " + category.id);
            if (!categories.add(category.id)) errors.add("duplicate category ids: " + category.id);
            collision(categoryKeys, category.id, "category", errors);
        }
        for (DocumentPerson person : document.persons) {
            if (
                person.name == null || person.name.isEmpty()
                    || person.name.codePointCount(0, person.name.length()) > 80
                    || person.name.chars()
                        .anyMatch(Character::isISOControl)
            ) {
                errors.add("invalid person name: " + person.name);
            }
            if (person.memberships.isEmpty()) errors.add("person has no category: " + person.name);
            Set<String> memberships = new HashSet<>();
            for (DocumentMembership membership : person.memberships) {
                if (!categories.contains(membership.categoryId)) {
                    errors.add("person \"" + person.name + "\": unknown category id \"" + membership.categoryId + "\"");
                }
                if (!memberships.add(membership.categoryId))
                    errors.add("duplicate person category: " + membership.categoryId);
                if (new HashSet<>(membership.roles).size() != membership.roles.size()) {
                    errors.add("duplicate roles for " + person.name + " in " + membership.categoryId);
                }
                checkRoleCollisions(roleKeys, membership.roles, errors);
            }
        }
        return errors;
    }

    /**
     * Checks local role keys across the document and proposed additions without mutating either.
     * Reusing an identical local id is allowed; external translation keys are ignored.
     * Other document constraints are deliberately unchecked while editing.
     */
    public static List<String> validateRoleAdditions(CreditsDocument document, Iterable<DocumentRole> additions) {
        Map<String, String> roleKeys = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (DocumentPerson person : document.persons) {
            for (DocumentMembership membership : person.memberships) {
                checkRoleCollisions(roleKeys, membership.roles, errors);
            }
        }
        checkRoleCollisions(roleKeys, additions, errors);
        return errors;
    }

    private static void checkRoleCollisions(Map<String, String> keys, Iterable<DocumentRole> roles,
        List<String> errors) {
        for (DocumentRole role : roles) {
            if (!role.isExternal()) collision(keys, role.value(), "role", errors);
        }
    }

    /**
     * Also requires English text for credits-owned labels and detail sections. If externalKeyExists
     * is null, external existence is deliberately unchecked; it requires the target pack's resources.
     */
    public static List<String> validateContent(CreditsDocument document, LangDocument english,
        Predicate<String> externalKeyExists) {
        List<String> errors = validate(document);
        for (DocumentCategory category : document.categories) {
            String prefix = "credits.category." + CreditsKeys.sanitize(category.id);
            requireText(english, prefix, errors);
            if (
                category.classes.contains("detail") && !new DetailLangKey(prefix).read(english)
                    .filter(
                        value -> !value.replace("\\n", "")
                            .trim()
                            .isEmpty())
                    .isPresent()
            ) {
                errors.add("missing English description: " + prefix + ".detail");
            }
        }
        Set<DocumentRole> seen = new HashSet<>();
        for (DocumentPerson person : document.persons) {
            for (DocumentMembership membership : person.memberships) {
                for (DocumentRole role : membership.roles) {
                    if (!seen.add(role)) continue;
                    if (role.isExternal()) {
                        if (externalKeyExists != null && !externalKeyExists.test(role.translationKey())) {
                            errors.add("missing external translation: " + role.translationKey());
                        }
                    } else {
                        requireText(english, role.translationKey(), errors);
                    }
                }
            }
        }
        return errors;
    }

    private static void requireText(LangDocument language, String key, List<String> errors) {
        String value = language.get(key);
        if (
            value == null || value.trim()
                .isEmpty()
        ) errors.add("missing English text: " + key);
    }

    private static void collision(Map<String, String> keys, String id, String kind, List<String> errors) {
        if (id == null) return;
        String key = CreditsKeys.sanitize(id);
        String previous = keys.putIfAbsent(key, id);
        if (previous != null && !previous.equals(id)) {
            errors.add(kind + " translation-key collision: \"" + previous + "\" and \"" + id + "\"");
        }
    }
}
