package net.noiraude.gtnhcredits.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.noiraude.gtnhcredits.Config;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.util.FuzzyFinder;

import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class CreditsController {

    private final CreditsDocument data;
    private int selectedIndex = 0;
    private String personFilter = "";
    private Pattern personFilterPattern = null;
    private FilterMethod filterMethod = FilterMethod.EXACT;

    public enum FilterMethod {

        EXACT("gui.credits.button.filter.exact"),
        FUZZY("gui.credits.button.filter.fuzzy");

        final String lang;

        FilterMethod(String lang) {
            this.lang = lang;
        }

        public String getLang() {
            return this.lang;
        }
    }

    public CreditsController() {
        this.data = CreditsRepository.load();
    }

    public List<DocumentCategory> getCategories() {
        return data.categories;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void selectCategory(int index) {
        this.selectedIndex = index;
    }

    public String getPersonFilter() {
        return personFilter;
    }

    public FilterMethod getFilterMethod() {
        return filterMethod;
    }

    public void setPersonFilter(String filter) {
        this.personFilter = filter == null ? "" : filter;
        if (this.personFilter.isEmpty()) {
            this.personFilterPattern = null;
        } else {
            try {
                this.personFilterPattern = Pattern
                    .compile(this.personFilter, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            } catch (PatternSyntaxException e) {
                this.personFilterPattern = null; // fall back to literal substring match
            }
        }
    }

    public void setFilterMethod(FilterMethod method) {
        this.filterMethod = method;
    }

    /**
     * Returns the currently selected category, or {@code null} if there are no categories.
     */
    public @Nullable DocumentCategory getSelectedCategory() {
        if (data.categories.isEmpty()) return null;
        return data.categories.get(Math.min(selectedIndex, data.categories.size() - 1));
    }

    /**
     * Returns all persons belonging to the given category, sorted by name and deduplicated:
     * multiple entries with the same name are merged and their roles are combined
     * (unique roles, preserving first-encountered order).
     */
    public List<DocumentPerson> getPersonsForCategory(DocumentCategory category) {
        // Phase 1: accumulate roles and display name per person key (plain name, no formatting).
        Map<String, LinkedHashSet<String>> rolesByKey = new LinkedHashMap<>();
        Map<String, String> displayNameByKey = new LinkedHashMap<>();
        for (DocumentPerson p : data.persons) {
            for (DocumentMembership m : p.memberships) {
                if (m.categoryId.equals(category.id)) {
                    String key = EnumChatFormatting.getTextWithoutFormattingCodes(p.name);
                    displayNameByKey.putIfAbsent(key, p.name);
                    rolesByKey.computeIfAbsent(key, k -> new LinkedHashSet<>())
                        .addAll(m.roles);
                    break; // at most one membership per category per person
                }
            }
        }

        // Phase 2: build lookup index (order guaranteed by CreditsParser contract).
        Map<String, DocumentPerson> personIndex = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashSet<String>> e : rolesByKey.entrySet()) {
            String key = e.getKey();
            DocumentPerson dp = new DocumentPerson(displayNameByKey.get(key));
            dp.memberships.add(new DocumentMembership(category.id, new ArrayList<>(e.getValue())));
            personIndex.put(key, dp);
        }

        Map<String, DocumentPerson> result = filterMethod == FilterMethod.EXACT ? getRegexFilteredNames(personIndex)
            : getFuzzyFilteredNames(personIndex);
        return new ArrayList<>(result.values());
    }

    /**
     * Returns the display name for a category, preferring a translation over the raw id.
     */
    public String getCategoryDisplayName(int index) {
        if (index < 0 || index >= data.categories.size()) return "";
        String id = data.categories.get(index).id;
        String key = "credits.category." + sanitizeKey(id);
        String name = StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key) : id;
        return EnumChatFormatting.getTextWithoutFormattingCodes(name);
    }

    /**
     * Sanitizes a key for use as a translation key suffix:
     * dots and hyphens are deleted, runs of spaces are collapsed to a single
     * underscore, and the result is lowercased.
     * The original unsanitized value should be used as fallback display text.
     */
    public static String sanitizeKey(String key) {
        return key.replace(".", "")
            .replace("-", "")
            .replaceAll(" +", "_")
            .toLowerCase();
    }

    private Map<String, DocumentPerson> getRegexFilteredNames(Map<String, DocumentPerson> personIndex) {
        if (personFilter.isEmpty()) return personIndex;
        Pattern filter = personFilterPattern;
        String lowerFilter = personFilter.toLowerCase();
        Map<String, DocumentPerson> result = new LinkedHashMap<>();
        for (Map.Entry<String, DocumentPerson> e : personIndex.entrySet()) {
            String key = e.getKey();
            boolean matches = filter != null ? filter.matcher(key)
                .find()
                : key.toLowerCase()
                    .contains(lowerFilter);
            if (matches) result.put(key, e.getValue());
        }
        return result;
    }

    private Map<String, DocumentPerson> getFuzzyFilteredNames(Map<String, DocumentPerson> personIndex) {
        if (personFilter.isEmpty()) return personIndex;
        List<String> matchedKeys = FuzzyFinder.findMatchesWithThreshold(
            new ArrayList<>(personIndex.keySet()),
            personFilter,
            Config.getInstance().fuzzyThreshold);
        matchedKeys.sort(String.CASE_INSENSITIVE_ORDER);
        Map<String, DocumentPerson> result = new LinkedHashMap<>();
        for (String key : matchedKeys) {
            result.put(key, personIndex.get(key));
        }
        return result;
    }

}
