package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.cnrs.lacito.liftapi.internal.DictionaryMutator;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** {@link LiftDictionary#extract}: deep, standalone copies of entries. */
class ExtractTest {

    private static final String FIELD_DICTIONARY = "lift/DictionaryLift20261022.lift";

    private static LiftDictionary field;

    @BeforeAll
    static void load() {
        field = Utils.loadDictionaryForTest(FIELD_DICTIONARY);
    }

    private static String xml(LiftDictionary d) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            d.save(out);
        } catch (WrittingLiftDocumentException e) {
            throw new AssertionError(e);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static String savedIn(java.io.File dir, LiftDictionary d) {
        try {
            java.io.File f = java.io.File.createTempFile("extract-oracle", ".lift", dir);
            try {
                d.save(f);
                return java.nio.file.Files.readString(f.toPath(), StandardCharsets.UTF_8);
            } finally {
                f.delete();
            }
        } catch (java.io.IOException | WrittingLiftDocumentException e) {
            throw new AssertionError(e);
        }
    }

    private static void assertSameXml(String expected, String actual, String what) {
        if (expected.equals(actual)) return;
        int i = 0;
        while (i < Math.min(expected.length(), actual.length()) && expected.charAt(i) == actual.charAt(i)) i++;
        int from = Math.max(0, i - 200);
        throw new AssertionError(what + ": differs at " + i + "\n--- expected\n"
            + expected.substring(from, Math.min(expected.length(), i + 300)) + "\n--- actual\n"
            + actual.substring(from, Math.min(actual.length(), i + 300)));
    }

    private static List<LiftEntry> entries(LiftDictionary d) {
        return List.copyOf(d.getLiftDictionaryRegistry().getEntries());
    }

    /** The XML oracle: the full extract of every entry is the dictionary, as saved. */
    @Test
    void fullExtractOfEveryEntryIsTheDictionary() {
        for (String file : List.of(FIELD_DICTIONARY, "lift/latex-sample.lift", "lift/tiny015.xml",
            "lift/tinyTextSpan.xml", "lift/tinyMultiTextAnnotation.xml", "lift/header_fieldDefinition.xml")) {
            LiftDictionary d = Utils.loadDictionaryForTest(file);
            LiftDictionary copy = d.extract(entries(d),
                ExtractOptions.DEFAULT.withHeader(ExtractOptions.HeaderScope.FULL));
            // Saved in the same directory, so that the hrefs of external ranges are the same.
            java.io.File dir = Utils.resourceFile(file).getParentFile();
            assertSameXml(savedIn(dir, d), savedIn(dir, copy), file);
            assertEquals(d.entryCount(), copy.entryCount(), file);
            assertTrue(copy.getLoadDiagnostics().isEmpty(), file);
            assertTrue(copy.getSourceFile().isEmpty(), file);
        }
    }

    @Test
    void oneEntryWithTheUsedHeaderIsWrittenAsInTheDictionary() {
        LiftEntry entry = withRelation();
        LiftDictionary copy = field.extract(List.of(entry));
        assertEquals(1, copy.entryCount());
        LiftEntry c = copy.getLiftDictionaryRegistry().getEntries().get(0);
        assertEquals(entry.getId(), c.getId());
        assertEquals(entry.getGuid(), c.getGuid());
        assertEquals(entry.getSenses().size(), c.getSenses().size());
        // The entry is written in the extract exactly as in the dictionary.
        assertEquals(entryXml(xml(field), entry.getId().orElseThrow()), entryXml(xml(copy), entry.getId().orElseThrow()));
    }

    @Test
    void theUsedHeaderHoldsOnlyWhatTheEntryUses() {
        LiftEntry entry = withRelation();
        LiftDictionary copy = field.extract(List.of(entry));
        int all = 0;
        for (FeatureSet fs : field.getHeader().getFeatureSets()) all += fs.getFeatures().size();
        int copied = 0;
        for (FeatureSet fs : copy.getHeader().getFeatureSets()) {
            copied += fs.getFeatures().size();
            assertFalse(fs.isExternallyDefined(), "an extract copies an external range into itself");
        }
        assertTrue(copied < all / 10, copied + " values copied of " + all);
    }

    @Test
    void referencesOutsideTheExtractStayIds() {
        LiftEntry entry = withRelation();
        LiftDictionary copy = field.extract(List.of(entry));
        LiftRelation r = copy.getLiftDictionaryRegistry().getRelations().get(0);
        assertTrue(r.getRefId().isPresent());
        assertNull(r.getRefObject());
        assertFalse(copy.isReferenceStub(copy.getLiftDictionaryRegistry().getEntries().get(0)));
    }

    @Test
    void stubsGiveReferencesATarget() {
        LiftEntry entry = withRelation();
        LiftDictionary copy = field.extract(List.of(entry),
            ExtractOptions.DEFAULT.withReferences(ExtractOptions.ReferenceTargets.STUBS));
        List<LiftEntry> copied = copy.getLiftDictionaryRegistry().getEntries();
        assertTrue(copied.size() > 1);
        assertFalse(copy.isReferenceStub(copied.get(0)));
        for (int i = 1; i < copied.size(); i++) {
            LiftEntry stub = copied.get(i);
            assertTrue(copy.isReferenceStub(stub));
            assertTrue(stub.getRelations().isEmpty() && stub.getVariants().isEmpty());
            LiftEntry original = (LiftEntry) field.getLiftDictionaryRegistry().getEntryOrSenseByLiftId(stub.getId().orElseThrow());
            assertEquals(original.getForms().getLangs(), stub.getForms().getLangs());
            for (String lang : original.getForms().getLangs()) {
                assertEquals(original.getForms().getForm(lang).map(Form::toPlainText),
                    stub.getForms().getForm(lang).map(Form::toPlainText));
            }
        }
        for (LiftRelation r : copy.getLiftDictionaryRegistry().getRelations()) {
            assertNotNull(r.getRefObject(), "resolved to a stub or a copy: " + r.getRefId());
            assertTrue(((AbstractLiftRoot) r.getRefObject()).getOwningDictionary() == copy);
        }
        assertFalse(field.isReferenceStub(entry));
    }

    @Test
    void theCopyAndTheSourceShareNothing() {
        List<LiftEntry> some = entries(field).subList(0, 50);
        LiftDictionary copy = field.extract(some, ExtractOptions.DEFAULT.withReferences(ExtractOptions.ReferenceTargets.STUBS));
        Set<Object> source = identitySet();
        for (LiftEntry e : some) collect(e, source);
        collectHeader(field, source);
        Set<Object> copied = identitySet();
        for (LiftEntry e : copy.getLiftDictionaryRegistry().getEntries()) collect(e, copied);
        collectHeader(copy, copied);
        for (Object o : copied) {
            assertFalse(source.contains(o), "shared: " + o);
        }
    }

    @Test
    void changingOneLeavesTheOtherUnchanged() {
        LiftEntry entry = entries(field).get(3);
        String before = xml(field);
        LiftDictionary copy = field.extract(List.of(entry));
        LiftEntry c = copy.getLiftDictionaryRegistry().getEntries().get(0);
        String lang = c.getForms().getLangs().iterator().next();
        c.getForms().removeForm(lang);
        c.addForm(new Form(lang, "changed in the copy"));
        copy.removeEntry(c);
        assertEquals(before, xml(field));

        LiftDictionary copy2 = field.extract(List.of(entry));
        String copyBefore = xml(copy2);
        Optional<Form> form = entry.getForms().getForm(lang);
        entry.getForms().removeForm(lang);
        assertEquals(copyBefore, xml(copy2));
        form.ifPresent(entry::addForm);
        assertEquals(before, xml(field));
    }

    @Test
    void entriesComeInTheOrderGivenOnce() {
        List<LiftEntry> all = entries(field);
        List<LiftEntry> given = List.of(all.get(5), all.get(2), all.get(5), all.get(9));
        LiftDictionary copy = field.extract(given);
        List<LiftEntry> copied = copy.getLiftDictionaryRegistry().getEntries();
        assertEquals(3, copied.size());
        assertEquals(all.get(5).getId(), copied.get(0).getId());
        assertEquals(all.get(2).getId(), copied.get(1).getId());
        assertEquals(all.get(9).getId(), copied.get(2).getId());
    }

    @Test
    void anEntryOfAnotherDictionaryIsRefused() {
        LiftDictionary other = Utils.loadDictionaryForTest("lift/tiny015.xml");
        LiftEntry foreign = entries(other).get(0);
        assertThrows(IllegalArgumentException.class, () -> field.extract(List.of(foreign)));
    }

    @Test
    void noEntryGivesTheHeaderOnly() {
        LiftDictionary copy = field.extract(List.of(), ExtractOptions.DEFAULT.withHeader(ExtractOptions.HeaderScope.FULL));
        assertEquals(0, copy.entryCount());
        assertEquals(field.getHeader().getFeatureSets().size(), copy.getHeader().getFeatureSets().size());
    }

    /** Not a performance assertion that could flake: a generous bound, and the figure printed. */
    @Test
    void oneEntryIsCheap() {
        List<LiftEntry> all = entries(field);
        for (int i = 0; i < 200; i++) field.extract(List.of(all.get(i)));     // warm up
        long start = System.nanoTime();
        int n = 500;
        for (int i = 0; i < n; i++) {
            field.extract(List.of(all.get(i % all.size())),
                ExtractOptions.DEFAULT.withReferences(ExtractOptions.ReferenceTargets.STUBS));
        }
        double perEntry = (System.nanoTime() - start) / 1e6 / n;
        long fullStart = System.nanoTime();
        field.extract(all, ExtractOptions.DEFAULT.withHeader(ExtractOptions.HeaderScope.FULL));
        double full = (System.nanoTime() - fullStart) / 1e6;
        System.out.printf("extract: %.3f ms per entry (USED + STUBS); %.1f ms for %d entries (FULL)%n",
            perEntry, full, all.size());
        assertTrue(perEntry < 20, perEntry + " ms per entry");
    }

    // ------------------------------------------------------------------

    private LiftEntry withRelation() {
        for (LiftEntry e : entries(field)) {
            if (!e.getRelations().isEmpty() && e.getRelations().get(0).getRefId().isPresent()) {
                String target = e.getRelations().get(0).getRefId().get();
                if (!target.equals(e.getId().orElse(null))) return e;
            }
        }
        throw new IllegalStateException("the fixture has an entry with a relation to another entry");
    }

    private static String entryXml(String xml, String id) {
        String quoted = "id=\"" + id.replace("&", "&amp;").replace("\"", "&quot;") + "\"";
        int at = xml.indexOf(quoted);
        int start = xml.lastIndexOf("<entry", at);
        int end = xml.indexOf("</entry>", at) + "</entry>".length();
        return xml.substring(start, end);
    }

    private static Set<Object> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private static void collect(AbstractLiftRoot node, Set<Object> out) {
        out.add(node);
        for (AbstractLiftRoot child : DictionaryMutator.childrenOf(node)) collect(child, out);
    }

    private static void collectHeader(LiftDictionary d, Set<Object> out) {
        out.add(d.getHeader());
        for (FeatureSet fs : d.getHeader().getFeatureSets()) {
            out.add(fs);
            for (Feature f : fs.getFeatures().values()) out.add(f);
        }
        out.addAll(new ArrayList<>(d.getHeader().getFieldsAndTraitsDefinitions()));
    }
}
