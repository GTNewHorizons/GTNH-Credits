package net.noiraude.gtnhcredits.client.credits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.noiraude.gtnhcredits.credits.CreditsCategory;
import net.noiraude.gtnhcredits.credits.CreditsData;
import net.noiraude.gtnhcredits.credits.CreditsPerson;

import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class CreditsController {

    private final CreditsData data;
    private int selectedIndex = 0;
    private String personFilter = "";
    /** Compiled regex for {@link #personFilter}, or {@code null} when the pattern is invalid. */
    private Pattern personFilterPattern = null;

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

    /** Returns the currently selected category, or {@code null} if there are no categories. */
    public @Nullable CreditsCategory getSelectedCategory() {
        if (data.categories.isEmpty()) return null;
        return data.categories.get(Math.min(selectedIndex, data.categories.size() - 1));
    }

    /**
     * Returns all persons belonging to the given category, sorted by name and deduplicated:
     * multiple entries with the same name are merged and their roles are combined
     * (unique roles, preserving first-encountered order).
     */
    public List<CreditsPerson> getPersonsForCategory(CreditsCategory category) {
        // Accumulate roles per name, preserving first-encounter order with LinkedHashMap.
        Map<String, LinkedHashSet<String>> rolesByName = new LinkedHashMap<>();
        for (CreditsPerson p : data.persons) {
            if (p.categoryRoles.containsKey(category.id)) {
                rolesByName.computeIfAbsent(p.name, k -> new LinkedHashSet<>())
                    .addAll(p.categoryRoles.getOrDefault(category.id, Collections.emptyList()));
            }
        }
        Pattern filter = personFilterPattern;
        String lowerFilter = personFilter.toLowerCase();
        return rolesByName.entrySet()
            .stream()
            .filter(e -> {
                if (personFilter.isEmpty()) return true;
                String name = EnumChatFormatting.getTextWithoutFormattingCodes(e.getKey());
                return filter != null ? filter.matcher(name)
                    .find()
                    : name.toLowerCase()
                        .contains(lowerFilter);
            })
            .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
            .map(e -> {
                Map<String, List<String>> catRoles = new HashMap<>();
                catRoles.put(category.id, new ArrayList<>(e.getValue()));
                return new CreditsPerson(e.getKey(), catRoles);
            })
            .collect(Collectors.toList());
    }

    /** Returns the display name for a category, preferring a translation over the raw id. */
    public String getCategoryDisplayName(int index) {
        if (index < 0 || index >= data.categories.size()) return "";
        String id = data.categories.get(index).id;
        String key = "credits.category." + sanitizeKey(id);
        String name = StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key) : id;
        return EnumChatFormatting.getTextWithoutFormattingCodes(name);
    }

    /**
     * Sanitizes a key for use as a translation key suffix:
     * dots and hyphens are deleted, runs of spaces are collapsed to a single underscore, and the result is lowercased.
     * The original unsanitized value should be used as fallback display text.
     */
    public static String sanitizeKey(String key) {
        return key.replace(".", "")
            .replace("-", "")
            .replaceAll(" +", "_")
            .toLowerCase();
    }
}
