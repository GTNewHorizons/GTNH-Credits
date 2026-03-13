package net.noiraude.gtnhcredits.client.credits;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.noiraude.gtnhcredits.GTNHCredits;
import net.noiraude.gtnhcredits.credits.CreditsCategory;
import net.noiraude.gtnhcredits.credits.CreditsData;
import net.noiraude.gtnhcredits.credits.CreditsPerson;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class CreditsRepository {

    private static final Logger LOG = LogManager.getLogger(CreditsRepository.class);
    private static final ResourceLocation LOCATION = new ResourceLocation(GTNHCredits.MODID, "credits.json");

    // Defensive limits; mirror the schema's maxLength/maxItems where applicable.
    private static final int MAX_CATEGORIES = 256;
    private static final int MAX_PERSONS = 10_000;
    private static final int MAX_STRING_SHORT = 32; // key (id, role)
    private static final int MAX_STRING_NAME = 80; // person name

    /**
     * Mirrors the schema's key pattern: letter start, letter/digit end, letters/digits/spaces/dots/underscores/hyphens
     * inside.
     */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z]([A-Za-z0-9 ._-]*[A-Za-z0-9])?$");

    static CreditsData load() {
        try (InputStream is = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(LOCATION)
            .getInputStream(); InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return parse(
                new JsonParser().parse(reader)
                    .getAsJsonObject());
        } catch (IOException e) {
            LOG.error("Failed to load credits.json", e);
            return CreditsData.EMPTY;
        } catch (Exception e) {
            LOG.error("credits.json is malformed or invalid, falling back to empty credits", e);
            return CreditsData.EMPTY;
        }
    }

    private static CreditsData parse(JsonObject root) {
        List<CreditsCategory> categories = parseCategories(root);
        List<CreditsPerson> persons = parsePersons(root);
        persons.sort(Comparator.comparing(p -> p.name, String.CASE_INSENSITIVE_ORDER));
        return new CreditsData(categories, persons);
    }

    private static List<CreditsCategory> parseCategories(JsonObject root) {
        List<CreditsCategory> categories = new ArrayList<>();
        int count = 0;
        for (JsonElement el : root.getAsJsonArray("category")) {
            if (count++ >= MAX_CATEGORIES) {
                LOG.warn("credits.json: too many categories, truncating at {}", MAX_CATEGORIES);
                break;
            }
            JsonObject obj = el.getAsJsonObject();
            String id = cap(
                obj.get("id")
                    .getAsString(),
                MAX_STRING_SHORT,
                "category id");
            if (!isValidKey(id)) {
                LOG.warn("credits.json: skipping category with invalid id \"{}\"", id);
                continue;
            }
            Set<String> classes = new HashSet<>();
            if (obj.has("class")) classes.addAll(readStringOrArray(obj.get("class"), "class"));
            categories.add(new CreditsCategory(id, Collections.unmodifiableSet(classes)));
        }
        return categories;
    }

    private static List<CreditsPerson> parsePersons(JsonObject root) {
        List<CreditsPerson> persons = new ArrayList<>();
        if (!root.has("person")) return persons;
        int count = 0;
        for (JsonElement el : root.getAsJsonArray("person")) {
            if (count++ >= MAX_PERSONS) {
                LOG.warn("credits.json: too many persons, truncating at {}", MAX_PERSONS);
                break;
            }
            JsonObject obj = el.getAsJsonObject();
            String name = cap(
                obj.get("name")
                    .getAsString(),
                MAX_STRING_NAME,
                "person name");
            List<String> catIds = parseValidKeys(obj.get("category"), "person category id", name, "category id");
            List<String> roles = obj.has("role") ? parseValidKeys(obj.get("role"), "role", name, "role")
                : new ArrayList<>();
            persons.add(new CreditsPerson(name, catIds, roles));
        }
        return persons;
    }

    private static List<String> parseValidKeys(JsonElement el, String field, String personName, String kind) {
        List<String> result = new ArrayList<>();
        for (String value : readStringOrArray(el, field)) {
            if (isValidKey(value)) {
                result.add(value);
            } else {
                LOG.warn("credits.json: skipping invalid {} \"{}\" on person \"{}\"", kind, value, personName);
            }
        }
        return result;
    }

    private static List<String> readStringOrArray(JsonElement el, String field) {
        if (el.isJsonPrimitive())
            return Collections.singletonList(cap(el.getAsString(), CreditsRepository.MAX_STRING_SHORT, field));
        List<String> result = new ArrayList<>();
        for (JsonElement item : el.getAsJsonArray())
            result.add(cap(item.getAsString(), CreditsRepository.MAX_STRING_SHORT, field));
        return result;
    }

    /** Returns true if {@code s} is a valid key per the schema pattern. */
    private static boolean isValidKey(String s) {
        return KEY_PATTERN.matcher(s)
            .matches();
    }

    /** Truncates {@code s} to {@code max} characters, logging a warning if truncation occurs. */
    private static String cap(String s, int max, String field) {
        if (s.length() <= max) return s;
        LOG.warn("credits.json: {} value too long ({}), truncating to {} chars", field, s.length(), max);
        return s.substring(0, max);
    }
}
