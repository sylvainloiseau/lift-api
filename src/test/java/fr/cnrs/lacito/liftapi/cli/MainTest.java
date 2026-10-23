package fr.cnrs.lacito.liftapi.cli;

import static org.junit.jupiter.api.Assertions.*;

import fr.cnrs.lacito.liftapi.Utils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The command line. */
class MainTest {

    record Run(int exit, String out, String err) {}

    static Run run(String... args) {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int exit = Main.execute(args, new PrintWriter(out, true), new PrintWriter(err, true));
        return new Run(exit, out.toString(), err.toString());
    }

    static String sample() {
        return Utils.resourceFile("lift/latex-sample.lift").toString();
    }

    @Test
    void aFileAloneStillPrintsTheLanguages() {
        Run r = run(sample());
        assertEquals(0, r.exit(), r.err());
        assertTrue(r.out().contains("Entries: 11"));
        assertTrue(r.out().contains("Object languages: oc-FR-fonipa"));
        assertEquals(r.out(), run("info", sample()).out());
    }

    @Test
    void latexWithOptionsOnly(@TempDir Path dir) throws Exception {
        Path tex = dir.resolve("d.tex");
        Run r = run("latex", sample(), "--title", "Mon lexique", "--no-thematic-dictionary",
            "--language", "fr=french", "--language", "oc-FR-fonipa=occitan", "--sort-language", "fr", "-o", tex.toString());
        assertEquals(0, r.exit(), r.err());
        String s = Files.readString(tex);
        assertTrue(s.contains("\\title{Mon lexique}"));
        assertFalse(s.contains("Thematic dictionary"));
        assertTrue(s.contains("\\setotherlanguage{occitan}"));
    }

    @Test
    void theCommandLineOverridesTheSettingsFile(@TempDir Path dir) throws Exception {
        Path json = dir.resolve("s.json");
        Files.writeString(json, "{\"title\": \"From the file\", \"author\": \"Me\", \"languages\": {\"fr\": \"french\", \"oc-FR-fonipa\": \"occitan\"}}");
        Path tex = dir.resolve("d.tex");
        assertEquals(0, run("latex", sample(), "--config", json.toString(), "-o", tex.toString()).exit());
        assertTrue(Files.readString(tex).contains("\\title{From the file}\\author{Me}"));

        assertEquals(0, run("latex", sample(), "--config", json.toString(), "--title", "From the options",
            "--language", "oc-FR-fonipa=french", "-o", tex.toString()).exit());
        String s = Files.readString(tex);
        assertTrue(s.contains("\\title{From the options}\\author{Me}"));
        assertFalse(s.contains("occitan"), "one language changed, the others kept");
        assertTrue(s.contains("\\setotherlanguage{french}"));
    }

    @Test
    void suggestAndWriteConfig(@TempDir Path dir) throws Exception {
        Path json = dir.resolve("s.json");
        Run r = run("latex", sample(), "--suggest", "--write-config", json.toString());
        assertEquals(0, r.exit(), r.err());
        String s = Files.readString(json);
        assertTrue(s.contains("\"oc-FR-fonipa\" : \"occitan\""), s);
        assertTrue(s.contains("\"sortLanguage\" : \"fr\""), s);
        try (var files = Files.list(dir)) {
            assertEquals(1, files.count(), "no export");
        }
    }

    @Test
    void aSampleToStandardOutput() {
        Run r = run("latex", sample(), "--sample", "1", "--sort-language", "fr");
        assertEquals(0, r.exit(), r.err());
        assertTrue(r.out().contains("\\entry{agyj}"));
        assertFalse(r.out().contains("\\entry{pir}"));
    }

    @Test
    void theExitCodes(@TempDir Path dir) throws Exception {
        Path bad = dir.resolve("bad.json");
        Files.writeString(bad, "{\"makeFigures\": true}");
        Run usage = run("latex", sample(), "--config", bad.toString());
        assertEquals(2, usage.exit());
        assertTrue(usage.err().contains("did you mean makeFigure?"), usage.err());

        assertEquals(2, run("latex", sample(), "--no-such-option").exit());
        assertEquals(2, run("latex", sample(), "--pdf").exit(), "--pdf needs -o");

        Run strict = run("latex", sample(), "--strict", "-o", dir.resolve("d.tex").toString());
        assertEquals(3, strict.exit(), "the languages are not mapped: warnings");
        assertTrue(strict.err().contains("warning: "));
        assertFalse(Files.exists(dir.resolve("d.tex")), "nothing written");

        assertEquals(1, run("latex", dir.resolve("missing.lift").toString()).exit());
    }

    @Test
    void aCustomStylesheetFromTheOptionsOverridesTheSettingsFile(@TempDir Path dir) throws Exception {
        String head = "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">";
        Files.writeString(dir.resolve("a.xsl"), head + "<xsl:param name=\"title\" select=\"'A'\"/></xsl:stylesheet>");
        Files.writeString(dir.resolve("b.xsl"), head + "<xsl:param name=\"title\" select=\"'B'\"/></xsl:stylesheet>");
        Files.writeString(dir.resolve("s.json"), "{\"customStylesheet\": \"a.xsl\"}");
        Path tex = dir.resolve("d.tex");
        assertEquals(0, run("latex", sample(), "--config", dir.resolve("s.json").toString(), "-o", tex.toString()).exit());
        assertTrue(Files.readString(tex).contains("\\title{A}"));
        assertEquals(0, run("latex", sample(), "--config", dir.resolve("s.json").toString(),
            "--stylesheet", dir.resolve("b.xsl").toString(), "-o", tex.toString()).exit());
        assertTrue(Files.readString(tex).contains("\\title{B}"));
    }

    @Test
    void writeAStartingStylesheetAndCopyTheBundledOnes(@TempDir Path dir) throws Exception {
        Run r = run("latex", "--write-stylesheet", dir.resolve("custom.xsl").toString(),
            "--copy-stylesheets", dir.resolve("xsl").toString());
        assertEquals(0, r.exit(), r.err());
        assertTrue(Files.readString(dir.resolve("custom.xsl")).contains("disabled:template"));
        assertTrue(Files.exists(dir.resolve("xsl/lift2latex.xsl")));
        assertTrue(Files.exists(dir.resolve("xsl/latex.xsl")));
        assertTrue(Files.exists(dir.resolve("xsl/parameters.xsl")));
    }

    @Test
    void anInvalidStylesheetExitsWithOneAndNamesItsLine(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("bad.xsl"), "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n"
            + "<xsl:template match=\"/\">\n<xsl:value-of select=\"concat(\"/>\n</xsl:template>\n</xsl:stylesheet>\n");
        Run r = run("latex", sample(), "--stylesheet", dir.resolve("bad.xsl").toString(), "-o", dir.resolve("d.tex").toString());
        assertEquals(1, r.exit());
        assertTrue(r.err().contains("bad.xsl, line 3: "), r.err());
    }

    @Test
    void listEngines() {
        Run r = run("latex", "--list-engines");
        assertEquals(0, r.exit(), r.err());
    }
}
