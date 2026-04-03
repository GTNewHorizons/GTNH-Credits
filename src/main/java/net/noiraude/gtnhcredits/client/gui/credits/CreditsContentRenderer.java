package net.noiraude.gtnhcredits.client.gui.credits;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.StatCollector;
import net.minecraft.util.StringTranslate;
import net.noiraude.gtnhcredits.model.CreditsCategory;
import net.noiraude.gtnhcredits.model.CreditsPerson;
import net.noiraude.gtnhcredits.repository.CreditsController;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.drawable.ITextLine;
import com.cleanroommc.modularui.drawable.text.RichText;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Platform;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class CreditsContentRenderer {

    /**
     * Reflection handles for StringTranslate internals used to enumerate indexed detail paragraph keys.
     * {@code getInstance()} (obfuscated {@code func_74808_a}) is package-private; accessed via reflection.
     * {@code languageList} (obfuscated {@code field_74816_c}) is the translation map.
     */
    private static final Method GET_INSTANCE_METHOD;
    private static final Field TRANSLATION_MAP_FIELD;

    static {
        Method m = null;
        Field f = null;
        try {
            m = StringTranslate.class.getDeclaredMethod("getInstance");
            m.setAccessible(true);
            f = StringTranslate.class.getDeclaredField("languageList");
            f.setAccessible(true);
        } catch (Exception ignored) {}
        GET_INSTANCE_METHOD = m;
        TRANSLATION_MAP_FIELD = f;
    }

    // -------------------------------------------------------------------------

    static void render(RichText rt, CreditsController controller, int contentWidth) {
        rt.alignment(Alignment.CenterLeft);
        CreditsCategory category = controller.getSelectedCategory();
        if (category == null) return;
        boolean detailRendered = category.classes.contains("detail") && appendDetail(rt, category.id);
        if (!category.classes.contains("person")) return;
        List<CreditsPerson> persons = controller.getPersonsForCategory(category);
        if (persons.isEmpty()) return;
        if (detailRendered) rt.emptyLine();
        if (category.classes.contains("role")) {
            appendPersonsWithRoles(rt, persons, contentWidth);
        } else {
            appendPersonsInline(rt, persons, contentWidth);
        }
    }

    /**
     * Renders the detail section for a category. Returns true if anything was rendered.
     *
     * <p>
     * Two complementary formats are supported and may coexist:
     * <ul>
     * <li><b>Plain key</b> {@code credits.category.{id}.detail}: the whole value is one or more
     * paragraphs separated by the literal two-character sequence {@code \n}. Each paragraph is
     * rendered as a single line; paragraphs are separated by an empty line.</li>
     * <li><b>Indexed keys</b> {@code credits.category.{id}.detail.{suffix}}: each key is one
     * paragraph. Within a paragraph value, the literal {@code \n} sequence is a line break
     * (no empty-line spacing between lines of the same paragraph). Paragraphs are separated by
     * an empty line. Suffixes are sorted in natural order ({@code "1" < "2" < "10"}).</li>
     * </ul>
     * Plain and indexed paragraphs are rendered in that order if both are present. Different language
     * files may define different indexed suffixes; only the keys present in the active locale are used.
     */
    private static boolean appendDetail(RichText rt, String categoryId) {
        String sanitized = CreditsController.sanitizeKey(categoryId);
        boolean rendered = false;

        // --- Plain key (old style) ---
        String plainKey = "credits.category." + sanitized + ".detail";
        if (StatCollector.canTranslate(plainKey)) {
            String value = StatCollector.translateToLocal(plainKey);
            if (!value.equals(plainKey)) {
                for (String paragraph : value.split("\\\\n", -1)) {
                    if (rendered) rt.emptyLine();
                    rt.add(IKey.str(paragraph));
                    rt.newLine();
                    rendered = true;
                }
            }
        }

        // --- Indexed paragraph keys (new style) ---
        String prefix = "credits.category." + sanitized + ".detail.";
        for (String paragraphValue : indexedDetailParagraphs(prefix)) {
            if (rendered) rt.emptyLine();
            for (String line : paragraphValue.split("\\\\n", -1)) {
                rt.add(IKey.str(line));
                rt.newLine();
            }
            rendered = true;
        }

        return rendered;
    }

    /**
     * Returns the values of all translation keys that start with {@code prefix}, ordered by the
     * natural sort of their suffix (the part after the prefix). Natural sort compares embedded
     * numeric runs by value, so {@code "1" < "2" < "10"} and {@code "abc1" < "abc2" < "abc10"}.
     */
    private static List<String> indexedDetailParagraphs(String prefix) {
        if (GET_INSTANCE_METHOD == null || TRANSLATION_MAP_FIELD == null) return Collections.emptyList();
        try {
            // Cast to Map<?,?>: wildcard parameters are erased so this cast is fully checked at
            // runtime (just instanceof Map), no unchecked warning, and no heap pollution.
            Map<?, ?> map = (Map<?, ?>) TRANSLATION_MAP_FIELD.get(GET_INSTANCE_METHOD.invoke(null));
            List<IndexedParagraph> paragraphs = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object rawKey = entry.getKey();
                if (!(rawKey instanceof String key)) continue;
                if (!key.startsWith(prefix) || key.length() <= prefix.length()) continue;
                paragraphs.add(new IndexedParagraph(key.substring(prefix.length()), (String) entry.getValue()));
            }
            Collections.sort(paragraphs);
            return paragraphs.stream()
                .map(p -> p.value)
                .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * A translation value paired with its key suffix, sortable in natural order.
     * Natural sort compares contiguous digit runs by numeric value so that
     * {@code "1" < "2" < "10"} rather than the lexicographic {@code "1" < "10" < "2"}.
     */
    private static final class IndexedParagraph implements Comparable<IndexedParagraph> {

        final String suffix;
        final String value;

        IndexedParagraph(String suffix, String value) {
            this.suffix = suffix;
            this.value = value;
        }

        @Override
        public int compareTo(IndexedParagraph other) {
            List<String> aToks = tokenize(this.suffix);
            List<String> bToks = tokenize(other.suffix);
            int len = Math.min(aToks.size(), bToks.size());
            for (int i = 0; i < len; i++) {
                String at = aToks.get(i), bt = bToks.get(i);
                int c = compareTokens(at, bt);
                if (c != 0) return c;
            }
            return aToks.size() - bToks.size();
        }

        /**
         * Splits {@code s} into alternating digit and non-digit runs.
         * E.g. {@code "abc10def"} → {@code ["abc", "10", "def"]}.
         */
        private static List<String> tokenize(String s) {
            List<String> tokens = new ArrayList<>();
            int i = 0;
            while (i < s.length()) {
                boolean digit = Character.isDigit(s.charAt(i));
                int j = i + 1;
                while (j < s.length() && Character.isDigit(s.charAt(j)) == digit) j++;
                tokens.add(s.substring(i, j));
                i = j;
            }
            return tokens;
        }

        /**
         * Compares two tokens from the same position. Both being digit-only → numeric comparison
         * (shorter = smaller; equal length → lexicographic, which is equivalent to numeric for
         * zero-padded-free suffixes). Mixed or non-digit → lexicographic.
         */
        private static int compareTokens(String a, String b) {
            if (Character.isDigit(a.charAt(0)) && Character.isDigit(b.charAt(0))) {
                if (a.length() != b.length()) return a.length() - b.length();
            }
            return a.compareTo(b);
        }
    }

    // -------------------------------------------------------------------------

    @SuppressWarnings("SizeReplaceableByIsEmpty")
    private static void appendPersonsWithRoles(RichText rt, List<CreditsPerson> persons, int contentWidth) {
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        // Separator: space, bold em-dash, reset, space.
        // Bold rendering makes the em-dash 1px wider than normal measurement.
        String separator = " §l—§r ";
        int separatorWidth = fr.getStringWidth(" — ") + 1;
        int commaSpaceWidth = fr.getStringWidth(", ");
        int ellipsisWidth = fr.getStringWidth("...");
        for (CreditsPerson person : persons) {
            // Persons returned by getPersonsForCategory have exactly one category entry.
            Iterator<List<String>> it = person.categoryRoles.values()
                .iterator();
            List<String> personRoles = it.hasNext() ? it.next() : Collections.emptyList();
            int nameWidth = fr.getStringWidth(person.name);
            int rolesAvailable = contentWidth - nameWidth - separatorWidth;
            if (personRoles.isEmpty() || rolesAvailable <= 0) {
                rt.addLine(new CenteredLine(person.name + "§r", nameWidth, contentWidth));
                continue;
            }
            StringBuilder roles = new StringBuilder();
            int rolesWidth = 0;
            boolean truncated = false;
            for (int i = 0; i < personRoles.size(); i++) {
                String roleName = roleDisplayName(personRoles.get(i));
                int roleWidth = fr.getStringWidth(roleName);
                int addedWidth = (i == 0) ? roleWidth : commaSpaceWidth + roleWidth;
                if (rolesWidth + addedWidth <= rolesAvailable) {
                    if (i > 0) roles.append(", ");
                    roles.append(roleName);
                    rolesWidth += addedWidth;
                } else {
                    truncated = true;
                    break;
                }
            }
            if (truncated) {
                if (roles.length() > 0) {
                    roles.append(", ...");
                    rolesWidth += commaSpaceWidth + ellipsisWidth;
                } else {
                    roles.append("...");
                    rolesWidth = ellipsisWidth;
                }
            }
            String lineText = person.name + "§r" + separator + "§o" + roles + "§r";
            int lineWidth = nameWidth + separatorWidth + rolesWidth;
            rt.addLine(new CenteredLine(lineText, lineWidth, contentWidth));
        }
    }

    @SuppressWarnings("SizeReplaceableByIsEmpty")
    private static void appendPersonsInline(RichText rt, List<CreditsPerson> persons, int contentWidth) {
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        int separatorWidth = fr.getStringWidth(", ");
        int commaWidth = fr.getStringWidth(",");
        StringBuilder line = new StringBuilder();
        int lineWidth = 0;
        for (CreditsPerson person : persons) {
            int nameWidth = fr.getStringWidth(person.name);
            if (line.length() > 0) {
                if (lineWidth + separatorWidth + nameWidth > contentWidth) {
                    // Not the last line: append trailing comma to the flushed line.
                    rt.addLine(new CenteredLine(line + ",", lineWidth + commaWidth, contentWidth));
                    line = new StringBuilder(person.name).append("§r");
                    lineWidth = nameWidth;
                } else {
                    line.append(", ")
                        .append(person.name)
                        .append("§r");
                    lineWidth += separatorWidth + nameWidth;
                }
            } else {
                line.append(person.name)
                    .append("§r");
                lineWidth = nameWidth;
            }
        }
        if (line.length() > 0) {
            // Last line: no trailing comma.
            rt.addLine(new CenteredLine(line.toString(), lineWidth, contentWidth));
        }
    }

    private static String roleDisplayName(String role) {
        String key = "credits.person.role." + CreditsController.sanitizeKey(role);
        return StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key) : role;
    }

    private static final class CenteredLine implements ITextLine {

        private final String text;
        private final int textWidth;
        private final int containerWidth;

        CenteredLine(String text, int textWidth, int containerWidth) {
            this.text = text;
            this.textWidth = textWidth;
            this.containerWidth = containerWidth;
        }

        @Override
        public void draw(GuiContext context, FontRenderer fr, float x, float y, int color, boolean shadow,
            int availableWidth, int availableHeight) {
            Platform.setupDrawFont();
            int cx = (int) x + (containerWidth - textWidth) / 2;
            fr.drawString(text, cx, (int) y, color, shadow);
        }

        @Override
        public int getWidth() {
            return containerWidth;
        }

        @Override
        public int getHeight(FontRenderer fr) {
            return fr.FONT_HEIGHT + 1;
        }

        @Override
        public Object getHoveringElement(FontRenderer fr, int x, int y) {
            return null;
        }
    }

    private CreditsContentRenderer() {}
}
