package net.noiraude.gtnhcredits.client.credits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.noiraude.gtnhcredits.credits.CreditsCategory;
import net.noiraude.gtnhcredits.credits.CreditsData;
import net.noiraude.gtnhcredits.credits.CreditsPerson;
import net.noiraude.gtnhcredits.util.FuzzyFinder;

import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class CreditsController {

    private final CreditsData data;
    private int selectedIndex = 0;
    private String personFilter = "";

    public CreditsController() {
        this.data = CreditsRepository.load();
    }

    public List<CreditsCategory> getCategories() {
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

    public void setPersonFilter(String filter) {
        this.personFilter = filter == null ? "" : filter;
    }

    /**
     * Returns the currently selected category, or {@code null} if there are no
     * categories.
     */
    public @Nullable CreditsCategory getSelectedCategory() {
        if (data.categories.isEmpty()) return null;
        return data.categories.get(Math.min(selectedIndex, data.categories.size() - 1));
    }

    /**
     * Returns all persons belonging to the given category, sorted by name and
     * deduplicated:
     * multiple entries with the same name are merged and their roles are combined
     * (unique roles, preserving first-encountered order).
     */
    public List<CreditsPerson> getPersonsForCategory(CreditsCategory category) {
        // Accumulate roles per name, preserving first-encounter order with
        // LinkedHashMap.
        Map<String, LinkedHashSet<String>> rolesByName = new LinkedHashMap<>();
        for (CreditsPerson p : data.persons) {
            if (p.categories.contains(category.id)) {
                rolesByName.computeIfAbsent(p.name, k -> new LinkedHashSet<>())
                    .addAll(p.roles);
            }
        }
        if (personFilter.isEmpty()) return rolesByName.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
            .map(
                e -> new CreditsPerson(
                    e.getKey(),
                    Collections.singletonList(category.id),
                    new ArrayList<>(e.getValue())))
            .collect(Collectors.toList());

        List<String> matchedNames = FuzzyFinder.findMatchesWithThreshold(
            rolesByName.entrySet()
                .stream()
                .map(e -> e.getKey())
                .collect(Collectors.toList()),
            personFilter,
            10);

        return matchedNames.stream()
            .map(e -> new CreditsPerson(e, Collections.singletonList(category.id), new ArrayList<>(rolesByName.get(e))))
            .collect(Collectors.toList());
        // return rolesByName.entrySet()
        // .stream()
        // .filter(e -> {
        // if (personFilter.isEmpty())
        // return true;
        // String name = EnumChatFormatting.getTextWithoutFormattingCodes(e.getKey());
        // return filter != null ? filter.matcher(name)
        // .find()
        // : name.toLowerCase()
        // .contains(lowerFilter);
        // })
        // .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
        // .map(
        // e -> new CreditsPerson(
        // e.getKey(),
        // Collections.singletonList(category.id),
        // new ArrayList<>(e.getValue())))
        // .collect(Collectors.toList());
    }

    /**
     * Returns the display name for a category, preferring a translation over the
     * raw id.
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
}
