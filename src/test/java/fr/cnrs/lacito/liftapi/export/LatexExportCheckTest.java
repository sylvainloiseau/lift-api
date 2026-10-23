package fr.cnrs.lacito.liftapi.export;

import static org.junit.jupiter.api.Assertions.*;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.Utils;
import fr.cnrs.lacito.liftapi.export.LatexExportWarning.Kind;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The checks made before an export. */
class LatexExportCheckTest {

    private static Optional<LatexExportWarning> find(List<LatexExportWarning> warnings, Kind kind) {
        return warnings.stream().filter(w -> w.kind() == kind).findFirst();
    }

    @Test
    void languagesWithoutAPolyglossiaName() {
        LiftDictionary tiny = Utils.loadDictionaryForTest(LatexExportTest.TINY);
        LatexExportOptions suggested = LatexExportOptions.suggested(tiny, null);
        assertEquals(Map.of("en", "english"), suggested.polyglossiaLanguages(), "polyglossia knows en only");
        LatexExportWarning w = find(LatexExport.check(tiny, suggested, null), Kind.LANGUAGE_NOT_MAPPED).orElseThrow();
        assertEquals(List.of("tpi", "tww"), w.examples());
        assertEquals(2, w.count());

        LatexExportOptions all = suggested.withPolyglossiaLanguages(Map.of("en", "english", "tpi", "english", "tww", "english"));
        assertTrue(find(LatexExport.check(tiny, all, null), Kind.LANGUAGE_NOT_MAPPED).isEmpty());
    }

    @Test
    void anUnknownPolyglossiaName() {
        LiftDictionary tiny = Utils.loadDictionaryForTest(LatexExportTest.TINY);
        LatexExportOptions o = LatexExportOptions.defaults().withPolyglossiaLanguages(Map.of("tww", "tuwari", "en", "english"));
        assertEquals(List.of("tuwari"), find(LatexExport.check(tiny, o, null), Kind.UNKNOWN_POLYGLOSSIA_NAME).orElseThrow().examples());
    }

    @Test
    void illustrations(@TempDir Path dir) throws Exception {
        LiftDictionary sample = Utils.loadDictionaryForTest(LatexExportTest.SAMPLE);
        LatexExportOptions figures = LatexExportOptions.defaults().withFigures(true);
        assertTrue(find(LatexExport.check(sample, figures, null), Kind.NO_ILLUSTRATION_DIRECTORY).isPresent());
        assertTrue(find(LatexExport.check(sample, LatexExportOptions.defaults(), null), Kind.NO_ILLUSTRATION_DIRECTORY).isEmpty(),
            "no warning about figures that are off");

        LatexExportWarning missing = find(LatexExport.check(sample, figures.withIllustrationDirectory(dir), null),
            Kind.ILLUSTRATIONS_MISSING).orElseThrow();
        assertEquals(List.of("P1230164.JPG"), missing.examples());

        Path pictures = Utils.resourceFile("lift/pictures").toPath();
        assertTrue(find(LatexExport.check(sample, figures.withIllustrationDirectory(pictures), null),
            Kind.ILLUSTRATIONS_MISSING).isEmpty());

        LiftDictionary corpus = Utils.loadDictionaryForTest(LatexExportTest.CORPUS);
        LatexExportWarning corpusMissing = find(LatexExport.check(corpus, figures.withIllustrationDirectory(pictures), null),
            Kind.ILLUSTRATIONS_MISSING).orElseThrow();
        assertEquals(List.of("P1230167.JPG", "P1230178.JPG"), corpusMissing.examples().stream().sorted().toList(),
            "the corpus's two other pictures are not in the repository");
    }

    @Test
    void noSemanticDomain() {
        LiftDictionary sample = Utils.loadDictionaryForTest(LatexExportTest.SAMPLE);
        assertTrue(find(LatexExport.check(sample, LatexExportOptions.defaults(), null), Kind.NO_SEMANTIC_DOMAIN).isEmpty());
        LiftDictionary noDomain = LiftDictionary.makeBuilder().build();
        noDomain.getComponentBuilder().entry().withForm("fr", "chat").build();
        assertTrue(find(LatexExport.check(noDomain, LatexExportOptions.defaults(), null), Kind.NO_SEMANTIC_DOMAIN).isPresent());
        assertTrue(find(LatexExport.check(noDomain, LatexExportOptions.defaults().withThematicDictionary(false), null),
            Kind.NO_SEMANTIC_DOMAIN).isEmpty());
    }

    @Test
    void anEntryWithoutForm() {
        LiftDictionary d = LiftDictionary.makeBuilder().build();
        d.getComponentBuilder().entry().withForm("fr", "chat").build();
        LiftEntry empty = d.getComponentBuilder().entry().build();
        LatexExportWarning w = find(LatexExport.check(d, LatexExportOptions.defaults(), null), Kind.ENTRY_WITHOUT_FORM).orElseThrow();
        assertEquals(1, w.count());
        assertEquals(List.of(empty.getId().orElse("?")), w.examples());
    }

    @Test
    void aFontWithoutLanguage() {
        LiftDictionary tiny = Utils.loadDictionaryForTest(LatexExportTest.TINY);
        LatexExportOptions o = LatexExportOptions.defaults()
            .withPolyglossiaLanguages(Map.of("en", "english"))
            .withFonts(Map.of("english", "Charis SIL", "occitan", "Doulos SIL"));
        assertEquals(List.of("occitan"), find(LatexExport.check(tiny, o, null), Kind.FONT_WITHOUT_LANGUAGE).orElseThrow().examples());
    }

    @Test
    void theOutputExists(@TempDir Path dir) throws Exception {
        LiftDictionary tiny = Utils.loadDictionaryForTest(LatexExportTest.TINY);
        Path tex = dir.resolve("d.tex");
        assertTrue(find(LatexExport.check(tiny, LatexExportOptions.defaults(), tex), Kind.OUTPUT_EXISTS).isEmpty());
        Files.writeString(tex, "");
        Files.writeString(dir.resolve("d.pdf"), "");
        assertEquals(2, find(LatexExport.check(tiny, LatexExportOptions.defaults(), tex), Kind.OUTPUT_EXISTS).orElseThrow().count());
    }
}
