package fr.cnrs.lacito.liftapi.export;

import static org.junit.jupiter.api.Assertions.*;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.Utils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The settings: JSON, merge, suggestions. */
class LatexExportOptionsTest {

    @Test
    void everyKeyIsRead(@TempDir Path dir) throws Exception {
        Path json = dir.resolve("s.json");
        Files.writeString(json, """
            {"title": "T", "author": "A", "makeThematicDictionary": false, "makeListOfFigure": true,
             "makeFigure": true, "urlillustration": "pictures", "languages": {"fr": "french"},
             "sortLanguage": "fr", "mainFont": "Charis SIL", "fonts": {"french": "Doulos SIL"},
             "latexEngine": "lualatex"}
            """);
        LatexExportOptions o = LatexExportOptions.fromJson(json);
        assertEquals("T", o.title());
        assertEquals("A", o.author());
        assertEquals(false, o.thematicDictionary());
        assertEquals(true, o.listOfFigures());
        assertEquals(true, o.figures());
        assertEquals(dir.resolve("pictures").toAbsolutePath(), o.illustrationDirectory(), "relative to the JSON file");
        assertEquals(Map.of("fr", "french"), o.polyglossiaLanguages());
        assertEquals("fr", o.sortLanguage());
        assertEquals("Charis SIL", o.mainFont());
        assertEquals(Map.of("french", "Doulos SIL"), o.fonts());
        assertEquals(LatexEngineRequest.of(LatexEngineKind.LUALATEX), o.latexEngine());
    }

    @Test
    void everyKeyIsOptional() throws Exception {
        LatexExportOptions o = LatexExportOptions.fromJson("{\"title\": \"T\"}", null);
        assertEquals(LatexExportOptions.defaults().withTitle("T"), o);
    }

    @Test
    void anUnknownKeyIsAnErrorWithASuggestion() {
        LatexExportException e = assertThrows(LatexExportException.class,
            () -> LatexExportOptions.fromJson("{\"makeFigures\": true}", null));
        assertTrue(e.getMessage().contains("unknown key makeFigures; did you mean makeFigure?"), e.getMessage());
    }

    @Test
    void aWrongTypeIsAnErrorThatNamesTheKey() {
        assertTrue(assertThrows(LatexExportException.class, () -> LatexExportOptions.fromJson("{\"makeFigure\": \"yes\"}", null))
            .getMessage().startsWith("makeFigure must be true or false"));
        assertTrue(assertThrows(LatexExportException.class, () -> LatexExportOptions.fromJson("{\"languages\": [\"fr\"]}", null))
            .getMessage().startsWith("languages must be an object"));
        assertTrue(assertThrows(LatexExportException.class, () -> LatexExportOptions.fromJson("{\"latexEngine\": \"/usr/bin/xelatex\"}", null))
            .getMessage().startsWith("latexEngine must be auto, xelatex or lualatex"));
    }

    @Test
    void writingThenReadingGivesTheSameSettings(@TempDir Path dir) throws Exception {
        LatexExportOptions o = LatexExportTest.sampleOptions()
            .withIllustrationDirectory(dir.resolve("pictures"))
            .withMainFont("Charis SIL").withFonts(Map.of("occitan", "Doulos SIL"))
            .withLatexEngine(LatexEngineRequest.of(LatexEngineKind.XELATEX));
        Path json = dir.resolve("s.json");
        o.writeJson(json);
        assertTrue(Files.readString(json).contains("\"urlillustration\" : \"pictures\""), "relative to the JSON file");
        assertEquals(o, LatexExportOptions.fromJson(json));
    }

    @Test
    void theMergeKeepsWhatIsNotGivenAndMergesTheMapsPerKey() {
        LatexExportOptions base = LatexExportOptions.defaults().withTitle("T").withFigures(true)
            .withPolyglossiaLanguages(Map.of("fr", "french", "tww", "tuwari"));
        LatexExportOptions over = LatexExportOptions.defaults().withFigures(false)
            .withPolyglossiaLanguages(Map.of("tww", "occitan"));
        LatexExportOptions m = base.merge(over);
        assertEquals("T", m.title());
        assertEquals(false, m.figures());
        assertEquals(Map.of("fr", "french", "tww", "occitan"), m.polyglossiaLanguages());
    }

    @Test
    void theSuggestions() {
        LiftDictionary tiny = Utils.loadDictionaryForTest(LatexExportTest.TINY);
        LatexExportOptions t = LatexExportOptions.suggested(tiny, Utils.resourceFile(LatexExportTest.TINY).toPath());
        assertEquals(Map.of("en", "english"), t.polyglossiaLanguages());
        assertEquals("en", t.sortLanguage());

        Path corpusFile = Utils.resourceFile(LatexExportTest.CORPUS).toPath();
        LatexExportOptions c = LatexExportOptions.suggested(Utils.loadDictionaryForTest(LatexExportTest.CORPUS), corpusFile);
        assertEquals("fr", c.sortLanguage(), "the language of the glosses, not of the header's descriptions");
        assertEquals("occitan", c.polyglossiaLanguages().get("oc-FR-fonipa"), "from the primary subtag");
        assertEquals("french", c.polyglossiaLanguages().get("fr"));
        assertFalse(c.polyglossiaLanguages().containsKey("qaa-x-spec"));
        assertEquals(corpusFile.getParent().resolve("pictures"), c.illustrationDirectory(), "the pictures/ folder");
        assertEquals(true, c.figures(), "the corpus has illustrations");
    }

    @Test
    void theIllustrationPrefix(@TempDir Path dir) {
        assertEquals("pictures/", LatexExportOptions.urlIllustration(dir.resolve("pictures"), dir));
        assertEquals(dir.resolve("pictures").toAbsolutePath().toString().replace('\\', '/') + "/",
            LatexExportOptions.urlIllustration(dir.resolve("pictures"), dir.resolve("other")));
    }
}
