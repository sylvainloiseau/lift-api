package fr.cnrs.lacito.liftapi.export;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.Utils;
import fr.cnrs.lacito.liftapi.export.LatexExportWarning.Kind;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A custom stylesheet, loaded after the bundled ones, replaces their definitions. */
class LatexCustomStylesheetTest {

    private static final String HEAD = "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n";
    private static final String TAIL = "</xsl:stylesheet>\n";

    private static Path stylesheet(Path dir, String name, String body) throws Exception {
        Path p = dir.resolve(name);
        Files.writeString(p, HEAD + body + TAIL);
        return p;
    }

    private static String export(LiftDictionary d, LatexExportOptions o) throws Exception {
        StringWriter w = new StringWriter();
        LatexExport.export(d, w, o, EntrySelection.all());
        return w.toString();
    }

    private static final String NOM_ABBREVIATED = """
        <xsl:template name="abbreviate_pos">
          <xsl:param name="pos"/>
          <xsl:choose>
            <xsl:when test="$pos = 'Nom'">n.</xsl:when>
            <xsl:otherwise><xsl:value-of select="$pos"/></xsl:otherwise>
          </xsl:choose>
        </xsl:template>
        """;

    @Test
    void aNamedTemplateReplacesTheBundledOneAndNothingElse(@TempDir Path dir) throws Exception {
        LiftDictionary d = Utils.loadDictionaryForTest(LatexExportTest.SAMPLE);
        LatexExportOptions o = LatexExportTest.sampleOptions();
        String bundled = export(d, o);
        String custom = export(d, o.withCustomStylesheet(stylesheet(dir, "custom.xsl", NOM_ABBREVIATED)));
        assertTrue(bundled.contains("\\pos{Nom}"));
        assertEquals(bundled.replace("\\pos{Nom}", "\\pos{n.}"), custom);
    }

    @Test
    void aMatchTemplateAndTheMacrosAreReplaced(@TempDir Path dir) throws Exception {
        Path custom = stylesheet(dir, "custom.xsl", """
            <xsl:template match="gloss">
              <xsl:text>\\myGloss{</xsl:text>
              <xsl:call-template name="escape_for_latex"><xsl:with-param name="string" select="text"/></xsl:call-template>
              <xsl:text>}</xsl:text>
            </xsl:template>
            <xsl:template name="latex-styling-command">
              <xsl:text>\\newcommand{\\myGloss}[1]{#1}</xsl:text>
            </xsl:template>
            """);
        String tex = export(Utils.loadDictionaryForTest(LatexExportTest.TINY), LatexExportOptions.defaults().withCustomStylesheet(custom));
        assertTrue(tex.contains("\\myGloss{which\\_kind}"), "a bundled template is called from the custom one");
        assertTrue(tex.contains("\\newcommand{\\myGloss}[1]{#1}"));
        assertFalse(tex.contains("\\newcommand\\latexentrystart"), "the bundled macros are replaced");
    }

    @Test
    void aParameterDefaultIsReplacedAndAGivenValueStillWins(@TempDir Path dir) throws Exception {
        Path custom = stylesheet(dir, "custom.xsl", "<xsl:param name=\"title\" select=\"'From the stylesheet'\"/>\n");
        LiftDictionary d = Utils.loadDictionaryForTest(LatexExportTest.TINY);
        LatexExportOptions o = LatexExportOptions.defaults().withCustomStylesheet(custom);
        assertTrue(export(d, o).contains("\\title{From the stylesheet}"));
        assertTrue(export(d, o.withTitle("Given")).contains("\\title{Given}"));
    }

    @Test
    void theCustomStylesheetImportsByARelativePath(@TempDir Path dir) throws Exception {
        Files.createDirectories(dir.resolve("parts"));
        stylesheet(dir.resolve("parts"), "pos.xsl", NOM_ABBREVIATED);
        Path custom = stylesheet(dir, "custom.xsl", "<xsl:import href=\"parts/pos.xsl\"/>\n");
        String tex = export(Utils.loadDictionaryForTest(LatexExportTest.SAMPLE), LatexExportOptions.defaults().withCustomStylesheet(custom));
        assertTrue(tex.contains("\\pos{n.}"));
    }

    @Test
    void aStylesheetThatDoesNotCompileNamesItsFileAndLineAndWritesNothing(@TempDir Path dir) throws Exception {
        Path bad = stylesheet(dir, "bad.xsl", "<xsl:template match=\"/\">\n<xsl:value-of select=\"concat(\"/>\n</xsl:template>\n");
        Path tex = dir.resolve("out.tex");
        Files.writeString(tex, "previous");
        LatexExportException e = assertThrows(LatexExportException.class, () -> LatexExport.export(
            Utils.loadDictionaryForTest(LatexExportTest.TINY), tex, LatexExportOptions.defaults().withCustomStylesheet(bad),
            EntrySelection.all()));
        assertTrue(e.getMessage().startsWith("The custom stylesheet does not compile: bad.xsl, line 3: "), e.getMessage());
        assertEquals("previous", Files.readString(tex));
    }

    @Test
    void aMissingStylesheetIsAnErrorBeforeWriting(@TempDir Path dir) {
        Path tex = dir.resolve("out.tex");
        LatexExportException e = assertThrows(LatexExportException.class, () -> LatexExport.export(
            Utils.loadDictionaryForTest(LatexExportTest.TINY), tex,
            LatexExportOptions.defaults().withCustomStylesheet(dir.resolve("none.xsl")), EntrySelection.all()));
        assertTrue(e.getMessage().contains("does not exist"));
        assertFalse(Files.exists(tex));
    }

    @Test
    void theMessagesOfTheStylesheetAreReturnedAndATerminationIsAnError(@TempDir Path dir) throws Exception {
        LiftDictionary d = Utils.loadDictionaryForTest(LatexExportTest.TINY);
        Path talking = stylesheet(dir, "talking.xsl",
            "<xsl:template name=\"latex-startdocument\"><xsl:message>hello from the stylesheet</xsl:message></xsl:template>\n");
        List<String> messages = LatexExport.export(d, new StringWriter(),
            LatexExportOptions.defaults().withCustomStylesheet(talking), EntrySelection.all());
        assertEquals(List.of("hello from the stylesheet"), messages);

        Path stopping = stylesheet(dir, "stopping.xsl",
            "<xsl:template match=\"entry\"><xsl:message terminate=\"yes\">no entry wanted</xsl:message></xsl:template>\n");
        LatexExportException e = assertThrows(LatexExportException.class, () -> LatexExport.export(d, new StringWriter(),
            LatexExportOptions.defaults().withCustomStylesheet(stopping), EntrySelection.all()));
        assertTrue(e.getMessage().endsWith("the stylesheet stopped: no entry wanted"), e.getMessage());
    }

    @Test
    void theStartingStylesheetChangesNothingUntilEdited(@TempDir Path dir) throws Exception {
        Path starting = dir.resolve("custom.xsl");
        LatexExport.writeStylesheetTemplate(starting);
        String text = Files.readString(starting);
        for (String point : LatexExport.EXTENSION_POINTS) {
            assertTrue(text.contains("=\"" + point + "\""), point);
        }
        assertTrue(text.contains("<disabled:param name=\"title\""));

        LiftDictionary d = Utils.loadDictionaryForTest(LatexExportTest.SAMPLE);
        LatexExportOptions o = LatexExportTest.sampleOptions();
        assertEquals(export(d, o), export(d, o.withCustomStylesheet(starting)), "a no-op");

        // Enabling the copy of abbreviate_pos, then editing it.
        int start = text.indexOf("<disabled:template name=\"abbreviate_pos\">");
        int end = text.indexOf("</disabled:template>", start) + "</disabled:template>".length();
        String block = text.substring(start, end)
            .replace("disabled:template", "xsl:template")
            .replace("<xsl:when test=\"$pos='Noun'\">n</xsl:when>", "<xsl:when test=\"$pos='Nom'\">n.</xsl:when>");
        Files.writeString(starting, text.substring(0, start) + block + text.substring(end));
        assertTrue(export(d, o.withCustomStylesheet(starting)).contains("\\pos{n.}"));
    }

    @Test
    void theCopiedStylesheetsRunWithXsltproc(@TempDir Path dir) throws Exception {
        List<Path> copies = LatexExport.copyStylesheets(dir.resolve("xsl"));
        assertEquals(3, copies.size());
        Path xsltproc = Path.of("/usr/bin/xsltproc");
        assumeTrue(Files.isExecutable(xsltproc), "no xsltproc");
        Path out = dir.resolve("out.tex");
        Process p = new ProcessBuilder(xsltproc.toString(), "-o", out.toString(),
            dir.resolve("xsl/lift2latex.xsl").toString(), Utils.resourceFile(LatexExportTest.SAMPLE).toString())
            .redirectErrorStream(true).start();
        assertTrue(p.waitFor(60, TimeUnit.SECONDS));
        assertEquals(0, p.exitValue(), new String(p.getInputStream().readAllBytes()));
        StringWriter bundled = new StringWriter();
        LatexExport.export(Utils.resourceFile(LatexExportTest.SAMPLE), bundled, LatexExportOptions.defaults());
        assertEquals(LatexExportTest.letters(bundled.toString()), LatexExportTest.letters(Files.readString(out)),
            "the same letters, in the same order");
    }

    @Test
    void theSettingIsInTheJson(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("s.json"), "{\"customStylesheet\": \"xsl/custom.xsl\"}");
        LatexExportOptions o = LatexExportOptions.fromJson(dir.resolve("s.json"));
        assertEquals(dir.resolve("xsl/custom.xsl").toAbsolutePath(), o.customStylesheet(), "relative to the JSON file");
        o.writeJson(dir.resolve("t.json"));
        assertTrue(Files.readString(dir.resolve("t.json")).contains("\"customStylesheet\" : \"xsl/custom.xsl\""));
        assertEquals(o, LatexExportOptions.fromJson(dir.resolve("t.json")));
        LatexExportException typo = assertThrows(LatexExportException.class,
            () -> LatexExportOptions.fromJson("{\"customStyleSheet\": \"a.xsl\"}", null));
        assertTrue(typo.getMessage().contains("did you mean customStylesheet?"), typo.getMessage());
    }

    @Test
    void theChecksReportAMissingOrInvalidStylesheet(@TempDir Path dir) throws Exception {
        LiftDictionary d = Utils.loadDictionaryForTest(LatexExportTest.TINY);
        assertTrue(LatexExport.check(d, LatexExportOptions.defaults().withCustomStylesheet(dir.resolve("none.xsl")), null)
            .stream().anyMatch(w -> w.kind() == Kind.CUSTOM_STYLESHEET_MISSING));
        Path bad = stylesheet(dir, "bad.xsl", "<xsl:template match=\"/\">\n<xsl:value-of select=\"concat(\"/>\n</xsl:template>\n");
        LatexExportWarning w = LatexExport.check(d, LatexExportOptions.defaults().withCustomStylesheet(bad), null).stream()
            .filter(x -> x.kind() == Kind.CUSTOM_STYLESHEET_INVALID).findFirst().orElseThrow();
        assertTrue(w.examples().get(0).startsWith("bad.xsl, line 3: "), w.examples().get(0));
        Path good = stylesheet(dir, "good.xsl", NOM_ABBREVIATED);
        assertTrue(LatexExport.check(d, LatexExportOptions.defaults().withCustomStylesheet(good), null).stream()
            .noneMatch(x -> x.kind().name().startsWith("CUSTOM_STYLESHEET")));
    }
}
