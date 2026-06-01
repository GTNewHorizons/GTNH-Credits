package net.noiraude.libcredits.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.Test;

public class DetailLangKeyTest {

    private static final String PREFIX = "credits.category.team";
    private static final String PLAIN_KEY = PREFIX + ".detail";

    private static LangDocument parse(String text) throws Exception {
        return LangParser.parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void read_joinsPlainWithIndexedSiblings() throws Exception {
        LangDocument doc = parse(
            PLAIN_KEY + "=intro paragraph\n"
                + PLAIN_KEY
                + ".0=second paragraph\n"
                + PLAIN_KEY
                + ".1=third paragraph\n");

        Optional<String> joined = new DetailLangKey(PREFIX).read(doc);

        assertEquals(Optional.of("intro paragraph\\nsecond paragraph\\nthird paragraph"), joined);
    }

    @Test
    public void read_returnsIndexedOnlyWhenNoPlainKey() throws Exception {
        LangDocument doc = parse(PLAIN_KEY + ".0=p0\n" + PLAIN_KEY + ".1=p1\n");

        Optional<String> joined = new DetailLangKey(PREFIX).read(doc);

        assertEquals(Optional.of("p0\\np1"), joined);
    }

    @Test
    public void read_returnsPlainOnlyWhenNoSiblings() throws Exception {
        LangDocument doc = parse(PLAIN_KEY + "=just plain\n");

        Optional<String> joined = new DetailLangKey(PREFIX).read(doc);

        assertEquals(Optional.of("just plain"), joined);
    }

    @Test
    public void read_returnsEmptyWhenNoEntries() throws Exception {
        LangDocument doc = parse("");

        Optional<String> joined = new DetailLangKey(PREFIX).read(doc);

        assertFalse(joined.isPresent());
    }

    @Test
    public void read_naturalSortsSuffix() throws Exception {
        LangDocument doc = parse(
            PLAIN_KEY + "=plain\n" + PLAIN_KEY + ".10=ten\n" + PLAIN_KEY + ".2=two\n" + PLAIN_KEY + ".1=one\n");

        Optional<String> joined = new DetailLangKey(PREFIX).read(doc);

        assertEquals(Optional.of("plain\\none\\ntwo\\nten"), joined);
    }

    @Test
    public void write_normalizesPlainAndRemovesIndexedSiblings() throws Exception {
        LangDocument doc = parse(PLAIN_KEY + "=old\n" + PLAIN_KEY + ".0=p0\n" + PLAIN_KEY + ".1=p1\n");

        new DetailLangKey(PREFIX).write(doc, "all new");

        assertEquals(Optional.of("all new"), new DetailLangKey(PREFIX).read(doc));
        assertFalse("indexed sibling 0 must be removed", doc.contains(PLAIN_KEY + ".0"));
        assertFalse("indexed sibling 1 must be removed", doc.contains(PLAIN_KEY + ".1"));
        assertTrue("plain key must remain", doc.contains(PLAIN_KEY));
        assertEquals("all new", doc.get(PLAIN_KEY));
    }

    @Test
    public void write_preservesForeignAndUnrelatedDetailKeys() throws Exception {
        LangDocument doc = parse(
            PLAIN_KEY + "=old\n"
                + PLAIN_KEY
                + ".0=p0\n"
                + "credits.category.dev.detail=other\n"
                + "credits.category.dev.detail.0=other p0\n");

        new DetailLangKey(PREFIX).write(doc, "all new");

        assertEquals("other", doc.get("credits.category.dev.detail"));
        assertEquals("other p0", doc.get("credits.category.dev.detail.0"));
    }

    @Test
    public void remove_clearsPlainAndIndexedSiblings() throws Exception {
        LangDocument doc = parse(PLAIN_KEY + "=plain\n" + PLAIN_KEY + ".0=p0\n" + PLAIN_KEY + ".1=p1\n");

        new DetailLangKey(PREFIX).remove(doc);

        assertFalse(doc.contains(PLAIN_KEY));
        assertFalse(doc.contains(PLAIN_KEY + ".0"));
        assertFalse(doc.contains(PLAIN_KEY + ".1"));
        assertFalse(
            new DetailLangKey(PREFIX).read(doc)
                .isPresent());
    }
}
