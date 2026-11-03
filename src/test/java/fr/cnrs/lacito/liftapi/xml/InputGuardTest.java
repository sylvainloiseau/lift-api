package fr.cnrs.lacito.liftapi.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDocumentLoadingException;
import fr.cnrs.lacito.liftapi.LiftVersion;
import fr.cnrs.lacito.liftapi.LoadDiagnostic;
import fr.cnrs.lacito.liftapi.LoadDiagnostic.Kind;
import fr.cnrs.lacito.liftapi.LoadOptions;
import fr.cnrs.lacito.liftapi.NotALiftDocumentException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What the reader does with a document that is not LIFT, or with what a LIFT document holds
 * that is not LIFT: the root must be {@code <lift>} in no namespace; anything else it meets
 * is skipped and reported as a {@link LoadDiagnostic}, never only logged; a strict load fails
 * on what would be lost on save.
 */
class InputGuardTest {

    private static final File CORPUS = new File("src/test/resources/lift/DictionaryLift20261022.lift");

    @TempDir
    Path dir;

    private Path write(String name, String content) throws Exception {
        Path p = dir.resolve(name);
        Files.writeString(p, content);
        return p;
    }

    private LiftDictionary load(String content) throws Exception {
        return LiftDictionary.loadDictionaryFromFile(write("test.lift", content).toFile());
    }

    private LiftDictionary load(String content, LoadOptions options) throws Exception {
        return LiftDictionary.loadDictionaryFromFile(write("test.lift", content).toFile(), options);
    }

    private static List<LoadDiagnostic> of(LiftDictionary d, Kind kind) {
        return d.getLoadDiagnostics().stream().filter(x -> x.kind() == kind).toList();
    }

    // ------------------------------------------------------------------
    // The root
    // ------------------------------------------------------------------

    @Test
    void aDocumentWhoseRootIsNotLiftIsRefused() {
        NotALiftDocumentException e = assertThrows(NotALiftDocumentException.class, () -> load("""
            <html><entry id="x"><lexical-unit><form lang="fr"><text>chat</text></form></lexical-unit></entry></html>
            """));
        assertTrue(e.getMessage().contains("<html>") && e.getMessage().contains("line 1"), e.getMessage());
        // Still a loading exception, for the callers that catch that.
        assertTrue(e instanceof LiftDocumentLoadingException);
    }

    @Test
    void aRangesFileOpenedAsADictionaryIsToldApart() {
        NotALiftDocumentException e = assertThrows(NotALiftDocumentException.class,
            () -> load("<lift-ranges><range id=\"a\"/></lift-ranges>"));
        assertTrue(e.getMessage().contains("ranges file"), e.getMessage());
    }

    @Test
    void aLiftRootInANamespaceIsRefused() {
        NotALiftDocumentException e = assertThrows(NotALiftDocumentException.class,
            () -> load("<x:lift xmlns:x=\"urn:other\" version=\"0.15\"><x:entry id=\"v\"/></x:lift>"));
        assertTrue(e.getMessage().contains("urn:other"), e.getMessage());
        // A default namespace is a namespace too.
        assertThrows(NotALiftDocumentException.class,
            () -> load("<lift xmlns=\"urn:other\" version=\"0.15\"><entry id=\"v\"/></lift>"));
    }

    @Test
    void aLiftWithoutProducerLoads() throws Exception {
        LiftDictionary d = load("<lift version=\"0.13\"><entry id=\"a\"/></lift>");
        assertEquals(1, d.getLiftDictionaryRegistry().nEntries());
        assertEquals(LiftVersion.V0_13, d.getLiftVersion());
        assertTrue(d.getLoadDiagnostics().isEmpty(), d.getLoadDiagnostics().toString());
    }

    // ------------------------------------------------------------------
    // Elements that are not LIFT
    // ------------------------------------------------------------------

    @Test
    void aNestedLiftIsAnUnknownElementAndDoesNotResetTheVersion() throws Exception {
        LiftDictionary d = load("""
            <lift version="0.15" producer="p">
            <entry id="a"><lift version="0.13" producer="q"/></entry>
            </lift>
            """);
        assertEquals(LiftVersion.V0_15, d.getLiftVersion());
        assertEquals("p", d.getLiftProducer());
        List<LoadDiagnostic> unknown = of(d, Kind.UNKNOWN_ELEMENT);
        assertEquals(1, unknown.size(), d.getLoadDiagnostics().toString());
        assertEquals("lift", unknown.getFirst().subject());
        assertEquals(2, unknown.getFirst().line());
        assertTrue(d.losesDataOnSave());
    }

    @Test
    void anElementInANamespaceIsUnknownWhateverItsLocalName() throws Exception {
        LiftDictionary d = load("""
            <lift version="0.15" producer="p">
            <x:entry xmlns:x="urn:other" id="y"/>
            <entry id="z"/>
            </lift>
            """);
        assertEquals(1, d.getLiftDictionaryRegistry().nEntries());
        List<LoadDiagnostic> unknown = of(d, Kind.UNKNOWN_ELEMENT);
        assertEquals(1, unknown.size(), d.getLoadDiagnostics().toString());
        assertEquals("{urn:other}entry", unknown.getFirst().subject());
        assertEquals(LoadDiagnostic.Severity.WARNING, unknown.getFirst().severity());
        assertTrue(unknown.getFirst().losesDataOnSave());
    }

    @Test
    void anUnknownElementIsReportedOnceWithItsOccurrencesAndTheLineOfTheFirst() throws Exception {
        StringBuilder entries = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            // What the unknown element holds is skipped with it, and not counted.
            entries.append("<entry id=\"e").append(i).append("\"><custom><entry id=\"inner").append(i)
                .append("\"/><other/></custom></entry>\n");
        }
        LiftDictionary d = load("<lift version=\"0.15\" producer=\"p\">\n" + entries + "</lift>");
        assertEquals(100, d.getLiftDictionaryRegistry().nEntries());
        assertEquals(1, d.getLoadDiagnostics().size(), d.getLoadDiagnostics().toString());
        LoadDiagnostic custom = d.getLoadDiagnostics().getFirst();
        assertEquals(Kind.UNKNOWN_ELEMENT, custom.kind());
        assertEquals("custom", custom.subject());
        assertEquals(100, custom.occurrences());
        assertEquals(2, custom.line());
        assertTrue(custom.message().contains("(line 2)"), custom.message());
        assertTrue(custom.getFile().orElseThrow().endsWith("test.lift"));
    }

    @Test
    void anUnknownElementInAnExternalRangeNamesTheRangesFile() throws Exception {
        Path ranges = write("dialects.lift-ranges", """
            <lift-ranges>
            <range id="dialects">
            <range-element id="north"><custom/></range-element>
            </range>
            </lift-ranges>
            """);
        LiftDictionary d = load("""
            <lift version="0.15" producer="p"><header><ranges>
            <range id="dialects" href="dialects.lift-ranges"/>
            </ranges></header></lift>
            """);
        LoadDiagnostic custom = of(d, Kind.UNKNOWN_ELEMENT).getFirst();
        assertEquals(ranges.toRealPath(), custom.getFile().orElseThrow().toRealPath());
        assertEquals(3, custom.line());
        assertTrue(custom.message().contains("dialects.lift-ranges"), custom.message());
    }

    @Test
    void anUnknownAttributeIsReportedWithItsElement() throws Exception {
        LiftDictionary d = load("""
            <lift version="0.15" producer="p">
            <entry id="a" colour="red"/>
            <entry id="b" colour="blue"/>
            </lift>
            """);
        List<LoadDiagnostic> attributes = of(d, Kind.UNKNOWN_ATTRIBUTE);
        assertEquals(1, attributes.size(), d.getLoadDiagnostics().toString());
        assertEquals("entry@colour", attributes.getFirst().subject());
        assertEquals(2, attributes.getFirst().occurrences());
        assertTrue(d.losesDataOnSave());
    }

    @Test
    void aGrammaticalInfoOutsideASenseOrAReversalIsUnknown() throws Exception {
        LiftDictionary d = load("""
            <lift version="0.15" producer="p">
            <entry id="a"><grammatical-info value="Noun"/></entry>
            </lift>
            """);
        List<LoadDiagnostic> unknown = of(d, Kind.UNKNOWN_ELEMENT);
        assertEquals(1, unknown.size(), d.getLoadDiagnostics().toString());
        assertEquals("grammatical-info", unknown.getFirst().subject());
        assertTrue(d.getLiftDictionaryRegistry().getGrammaticalInfos().isEmpty());
    }

    @Test
    void aGrammaticalInfoInAReversalAndItsMainIsRead() throws Exception {
        LiftDictionary d = load("""
            <lift version="0.15" producer="p">
            <entry id="a"><sense id="s"><reversal type="en">
            <form lang="en"><text>cat</text></form>
            <main><form lang="en"><text>animal</text></form><grammatical-info value="Noun"/></main>
            <grammatical-info value="Noun"><trait name="number" value="sg"/></grammatical-info>
            </reversal></sense></entry>
            </lift>
            """);
        assertFalse(d.losesDataOnSave(), d.getLoadDiagnostics().toString());
        var reversal = d.getLiftDictionaryRegistry().getReversals().stream()
            .filter(r -> r.getMain() != null).findFirst().orElseThrow();
        var gi = reversal.getGrammaticalInfo().orElseThrow();
        assertEquals("Noun", gi.getGramInfoValue().getId());
        assertEquals(reversal, gi.getParent());
        assertEquals("number", gi.getTraits().getFirst().getName());
        assertEquals("Noun", reversal.getMain().getGrammaticalInfo().orElseThrow().getGramInfoValue().getId());
        assertEquals(2, d.getLiftDictionaryRegistry().getGrammaticalInfos().size());
    }

    // ------------------------------------------------------------------
    // Strict load, limit
    // ------------------------------------------------------------------

    @Test
    void aStrictLoadFailsOnWhatWouldBeLostOnSaveOnly() throws Exception {
        LoadOptions strict = LoadOptions.defaults().withStrict(true);
        LiftDocumentLoadingException e = assertThrows(LiftDocumentLoadingException.class, () -> load("""
            <lift version="0.15" producer="p">
            <entry id="a"><custom/></entry>
            </lift>
            """, strict));
        assertTrue(e.getMessage().contains("custom") && e.getMessage().contains("line 2"), e.getMessage());
        assertThrows(LiftDocumentLoadingException.class, () -> load("""
            <lift version="0.15" producer="p"><entry id="a" colour="red"/></lift>
            """, strict));

        // An undeclared value is kept and written back: not an error.
        LiftDictionary d = load("""
            <lift version="0.15" producer="p">
            <entry id="a"><note type="unheard-of"><form lang="en"><text>x</text></form></note></entry>
            </lift>
            """, strict);
        assertFalse(d.getLoadDiagnostics().isEmpty());
        assertFalse(d.losesDataOnSave());
    }

    @Test
    void pastTheLimitDiagnosticsAreCountedNotListed() throws Exception {
        StringBuilder entries = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            entries.append("<entry id=\"e").append(i).append("\"><custom").append(i).append("/></entry>\n");
        }
        LiftDictionary d = load("<lift version=\"0.15\" producer=\"p\">\n" + entries + "</lift>",
            LoadOptions.defaults().withMaxDiagnostics(3));
        List<LoadDiagnostic> list = d.getLoadDiagnostics();
        assertEquals(4, list.size(), list.toString());
        LoadDiagnostic last = list.getLast();
        assertEquals(Kind.DIAGNOSTICS_TRUNCATED, last.kind());
        assertEquals(7, last.occurrences());
        assertTrue(d.losesDataOnSave());
        assertThrows(IllegalArgumentException.class, () -> LoadOptions.defaults().withMaxDiagnostics(0));
    }

    // ------------------------------------------------------------------
    // A clean file
    // ------------------------------------------------------------------

    /**
     * The corpus, a FLEx export, has no foreign element or attribute and no XML error. What
     * it loses on save are declarations FLEx makes twice: a range element whose id FLEx cut
     * at the first space ({@code non -dual}, {@code non -plural}), and two field
     * definitions. The reader always dropped them; it now says so.
     */
    @Test
    void theCorpusLosesOnlyTheDeclarationsItMakesTwice() throws Exception {
        LiftDictionary d = LiftDictionary.loadDictionaryFromFile(CORPUS);
        String all = d.getLoadDiagnostics().toString();
        List<LoadDiagnostic> lost = d.getLoadDiagnostics().stream().filter(LoadDiagnostic::losesDataOnSave).toList();
        assertEquals(List.of(Kind.DUPLICATE_RANGE_ELEMENT, Kind.DUPLICATE_DEFINITION, Kind.DUPLICATE_DEFINITION),
            lost.stream().map(LoadDiagnostic::kind).toList(), all);
        assertEquals(List.of("paradigm/non", "Particularism", "particularismSource"),
            lost.stream().map(LoadDiagnostic::subject).toList(), all);
        // The duplicate range element is in the ranges file, and named so.
        assertTrue(lost.getFirst().getFile().orElseThrow().toString().endsWith(".lift-ranges"), all);
        assertTrue(d.getLoadDiagnostics().stream().noneMatch(x -> x.severity() == LoadDiagnostic.Severity.ERROR), all);
        // FLEx leaves the empty "dialect" range out of its ranges file: a warning, nothing lost.
        assertTrue(d.getLoadDiagnostics().stream().anyMatch(x -> x.kind() == Kind.MISSING_EXTERNAL_RANGE
            && x.subject().equals("dialect")), all);
        assertThrows(LiftDocumentLoadingException.class,
            () -> LiftDictionary.loadDictionaryFromFile(CORPUS, LoadOptions.defaults().withStrict(true)));
    }
}
