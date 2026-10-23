package fr.cnrs.lacito.liftapi.export;

import static org.junit.jupiter.api.Assertions.*;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.Utils;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The LaTeX export: its output, its parameters, its letters.
 *
 * <p>{@code latex-sample.lift} is the expected-output test. To regenerate
 * {@code latex-sample.expected.tex} after a deliberate change of the stylesheets, run this
 * test with {@code -Dlatex.regenerate=true}, then review the diff.
 */
class LatexExportTest {

    static final String SAMPLE = "lift/latex-sample.lift";
    static final String TINY = "lift/tiny.xml";
    static final String CORPUS = "lift/DictionaryLift20261022.lift";

    /** The settings of the expected output. */
    static LatexExportOptions sampleOptions() {
        return LatexExportOptions.defaults()
            .withTitle("Lexique d'essai")
            .withAuthor("Test")
            .withFigures(true)
            .withSortLanguage("fr")
            .withPolyglossiaLanguages(Map.of("fr", "french", "oc-FR-fonipa", "occitan"));
    }

    static String export(LiftDictionary d, LatexExportOptions options) throws Exception {
        StringWriter w = new StringWriter();
        LatexExport.export(d, w, options, EntrySelection.all());
        return w.toString();
    }

    static String body(String tex) {
        return tex.substring(tex.indexOf("\\begin{document}"));
    }

    /** The \subsection headings of the alphabetic part, in order. */
    static List<String> letters(String tex) {
        String part = tex.substring(tex.indexOf("Alphabetic dictionary"));
        int end = part.indexOf("Thematic dictionary");
        if (end >= 0) part = part.substring(0, end);
        List<String> result = new ArrayList<>();
        Matcher m = Pattern.compile("\\\\subsection\\{([^}]*)\\}").matcher(part);
        while (m.find()) result.add(m.group(1));
        return result;
    }

    @Test
    void theSampleGivesTheExpectedOutput(@TempDir Path dir) throws Exception {
        Path pictures = Files.createDirectories(dir.resolve("pictures"));
        Files.copy(Utils.resourceFile("lift/pictures/P1230164.JPG").toPath(), pictures.resolve("P1230164.JPG"));
        LiftDictionary d = Utils.loadDictionaryForTest(SAMPLE);
        Path tex = dir.resolve("sample.tex");
        LatexExport.export(d, tex, sampleOptions().withIllustrationDirectory(pictures), EntrySelection.all());
        String actual = Files.readString(tex).replace("\r\n", "\n");

        if (Boolean.getBoolean("latex.regenerate")) {
            Path expected = Path.of("src/test/resources/lift/latex-sample.expected.tex");
            Files.writeString(expected, actual);
            fail("Regenerated " + expected.toAbsolutePath() + ": review the diff, then run without latex.regenerate.");
        }
        String expected = Files.readString(Utils.resourceFile("lift/latex-sample.expected.tex").toPath()).replace("\r\n", "\n");
        assertEquals(expected, actual);
        assertTrue(actual.contains("\\includegraphics[width=0.3\\linewidth]{pictures/P1230164.JPG}"),
            "the illustration directory is relative to the .tex");
    }

    @Test
    void theLabelsAreSafeAndTheTextsEscaped() throws Exception {
        String tex = export(Utils.loadDictionaryForTest(TINY), LatexExportOptions.defaults());
        assertTrue(tex.contains("\\label{kemia-napuo-002794b9-f063-4c6b-b77d-39b8ecd618d1}"), "no space in a label");
        assertTrue(tex.contains("which\\_kind"));

        String sample = export(Utils.loadDictionaryForTest(SAMPLE), LatexExportOptions.defaults());
        assertTrue(sample.contains("\\{a\\} \\$5 \\& \\#1 \\^{}2 50\\% \\textasciitilde{}3 \\textbackslash{}4 \\_5"), "every special character");
        assertTrue(sample.contains("mauvais\\textbackslash{}DEG.COMP"), "a backslash is not a line break");
        assertTrue(sample.contains("\\entry{f\\textasciitilde{}e}"));
        assertTrue(sample.contains("\\label{f-e-93b25f44-982d-4484-820b-743b2434aaf1}"), "no ~ in a label");
        assertTrue(sample.contains("\\latexrelationcomponent{-u$_{1}$}"), "a headword in a relation is escaped once");
        for (String label : labels(sample)) {
            assertTrue(label.matches("[A-Za-z0-9:-]+"), label);
        }
    }

    private static Set<String> labels(String tex) {
        Set<String> result = new LinkedHashSet<>();
        Matcher m = Pattern.compile("\\\\(?:label|ref|pageref)\\{([^}]*)\\}").matcher(tex);
        while (m.find()) result.add(m.group(1));
        return result;
    }

    @Test
    void eachParameterHasItsEffect(@TempDir Path dir) throws Exception {
        LiftDictionary d = Utils.loadDictionaryForTest(SAMPLE);
        String none = export(d, LatexExportOptions.defaults());
        assertTrue(none.contains("\\title{}\\author{}"));
        assertTrue(none.contains("Thematic dictionary"), "the thematic part is on by default");
        assertFalse(none.contains("\\listoffigures"));
        assertFalse(none.contains("\\begin{figure}"), "figures are off by default");
        assertFalse(none.contains("See figure"), "no reference to a figure that is not there");
        assertFalse(none.contains("\\setmainfont"));
        assertTrue(none.contains("\\setdefaultlanguage{english}"));
        assertFalse(none.contains("\\foreignlanguage"));

        String all = export(d, LatexExportOptions.defaults()
            .withTitle("A & B").withAuthor("C_D")
            .withThematicDictionary(false).withListOfFigures(true).withFigures(true)
            .withIllustrationDirectory(dir.resolve("my pictures"))
            .withSortLanguage("fr")
            .withPolyglossiaLanguages(Map.of("fr", "french", "oc-FR-fonipa", "occitan", "qaa-x-spec", "occitan"))
            .withMainFont("Charis SIL")
            .withFonts(Map.of("occitan", "Doulos SIL")));
        assertTrue(all.contains("\\title{A \\& B}\\author{C\\_D}"));
        assertFalse(all.contains("Thematic dictionary"));
        assertTrue(all.contains("\\listoffigures"));
        assertTrue(all.contains("\\begin{figure}"));
        String prefix = dir.resolve("my pictures").toAbsolutePath().toString().replace('\\', '/') + "/";
        assertTrue(all.contains("{" + prefix + "P1230164.JPG}"), "an absolute prefix, with a space");
        assertTrue(all.contains("\\setmainfont{Charis SIL}"));
        assertTrue(all.contains("\\newfontfamily\\occitanfont{Doulos SIL}"));
        assertTrue(all.contains("\\setdefaultlanguage{french}"), "the sort language is the default language");
        assertEquals(1, count(all, "\\setotherlanguage{occitan}"), "two codes, one name, one declaration");
        assertFalse(all.contains("\\setotherlanguage{french}"), "the default language is not declared again");
        assertTrue(all.contains("\\entry{\\foreignlanguage{occitan}{agyj}}"));
        assertFalse(all.contains("\\foreignlanguage{french}"), "the default language is not switched to");
    }

    private static int count(String s, String sub) {
        int n = 0;
        for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + 1)) n++;
        return n;
    }

    @Test
    void aBooleanGivenAsTheStringFalseIsFalse() throws Exception {
        // As xsltproc --stringparam gives it.
        var t = TransformerFactory.newDefaultInstance()
            .newTemplates(new StreamSource(LatexExport.class.getResource(LatexExport.STYLESHEET).toExternalForm()))
            .newTransformer();
        t.setParameter("makeThematicDictionary", "false");
        t.setParameter("makeFigure", "true");
        StringWriter w = new StringWriter();
        t.transform(new StreamSource(Utils.resourceFile(SAMPLE)), new StreamResult(w));
        assertFalse(w.toString().contains("Thematic dictionary"));
        assertTrue(w.toString().contains("\\begin{figure}"));
    }

    @Test
    void theLettersIgnoreCaseAccentsDashesAndTheGraphemeJoiner() throws Exception {
        String tex = export(Utils.loadDictionaryForTest(SAMPLE), LatexExportOptions.defaults().withSortLanguage("fr"));
        assertEquals(List.of("A", "B", "F", "G", "P", "U", "ʃ"), letters(tex));
        String a = section(tex, "A");
        assertTrue(a.indexOf("{arɛ̃ʒ}") < a.indexOf("{ãtʃe}"), "ã sorts as a: arɛ̃ʒ, then ãtʃe");
        String f = section(tex, "F");
        assertTrue(f.contains("{Fødu}") && f.contains("{f\\textasciitilde{}e}"), "upper case under the same letter");
        assertTrue(section(tex, "U").contains("{-u$_{1}$}"), "-u (homograph 1) under u");
        assertTrue(section(tex, "ʃ").contains("ʃəbri"), "the entry starting with U+034F under ʃ");
    }

    private static String section(String tex, String letter) {
        int start = tex.indexOf("\\subsection{" + letter + "}");
        int end = tex.indexOf("\\end{multicols}", start);
        return tex.substring(start, end);
    }

    @Test
    void exportingADictionaryOrItsFileGivesTheSameText() throws Exception {
        LiftDictionary d = Utils.loadDictionaryForTest(SAMPLE);
        StringWriter fromFile = new StringWriter();
        LatexExport.export(Utils.resourceFile(SAMPLE), fromFile, sampleOptions());
        assertEquals(fromFile.toString(), export(d, sampleOptions()));
    }

    @Test
    void anUnsavedChangeIsExported() throws Exception {
        LiftDictionary d = Utils.loadDictionaryForTest(SAMPLE);
        LiftEntry pir = d.getLiftDictionaryRegistry().getEntries().stream()
            .filter(e -> LatexExport.headwordForm(e).equals("pir")).findFirst().orElseThrow();
        pir.getForms().formTextProperty("oc-FR-fonipa").set("pirr");
        assertTrue(export(d, LatexExportOptions.defaults()).contains("\\entry{pirr}"));
    }

    @Test
    void theJavaLetterIsTheStylesheetLetterForEveryFormOfTheCorpus() throws Exception {
        LiftDictionary corpus = Utils.loadDictionaryForTest(CORPUS);
        List<String> forms = corpus.getLiftDictionaryRegistry().getEntries().stream()
            .map(LatexExport::headwordForm).toList();
        StringBuilder xml = new StringBuilder("<lift>");
        for (String form : forms) {
            xml.append("<entry><lexical-unit><form lang=\"x\"><text>")
                .append(form.replace("&", "&amp;").replace("<", "&lt;"))
                .append("</text></form></lexical-unit></entry>");
        }
        xml.append("</lift>");
        String probe = """
            <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
              <xsl:import href="%s"/>
              <xsl:output method="text" encoding="UTF-8"/>
              <xsl:template match="/">
                <xsl:for-each select="/lift/entry">
                  <xsl:variable name="l"><xsl:call-template name="letter"/></xsl:variable>
                  <xsl:value-of select="$l"/>
                  <!-- The key gives the same letter: this entry is among the entries of its letter. -->
                  <xsl:if test="not(key('letter', $l)[generate-id() = generate-id(current())])">!KEY!</xsl:if>
                  <xsl:text>&#10;</xsl:text>
                </xsl:for-each>
              </xsl:template>
            </xsl:stylesheet>
            """.formatted(LatexExport.class.getResource(LatexExport.STYLESHEET).toExternalForm());
        var t = TransformerFactory.newDefaultInstance()
            .newTemplates(new StreamSource(new java.io.StringReader(probe), "probe.xsl")).newTransformer();
        StringWriter w = new StringWriter();
        t.transform(new StreamSource(new java.io.StringReader(xml.toString())), new StreamResult(w));
        String[] xslt = w.toString().split("\n", -1);
        for (int i = 0; i < forms.size(); i++) {
            assertEquals(xslt[i], LatexExport.headingLetter(forms.get(i)), "letter of " + forms.get(i));
        }
        assertFalse(w.toString().contains("!KEY!"), "the key \"letter\" and the template \"letter\" agree");
    }

    @Test
    void theKeyRepeatsTheFoldingStringsOfTheParameters() throws Exception {
        String main = read(LatexExport.STYLESHEET);
        String parameters = read(LatexExport.PARAMETERS);
        String from = variable(parameters, "foldFrom");
        String to = variable(parameters, "foldTo");
        Matcher key = Pattern.compile("<xsl:key name=\"letter\"[^>]*use=\"([^\"]*)\"").matcher(main);
        assertTrue(key.find());
        assertTrue(key.group(1).endsWith("'" + from + "', '" + to + "')"), "change foldFrom and foldTo in both places");
    }

    private static String read(String resource) throws IOException {
        try (InputStream in = LatexExport.class.getResourceAsStream(resource)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String variable(String xsl, String name) {
        Matcher m = Pattern.compile("<xsl:variable name=\"" + name + "\" select=\"'([^']*)'\"/>").matcher(xsl);
        assertTrue(m.find(), name);
        return m.group(1);
    }

    @Test
    void aSampleTakesOneEntryPerLetterFirstAndTheEntriesTheyReferTo() {
        LiftDictionary d = Utils.loadDictionaryForTest(SAMPLE);
        Set<LiftEntry> sample = LatexExport.sample(d, 7, "fr");
        Set<String> letters = new LinkedHashSet<>();
        for (LiftEntry e : sample) letters.add(LatexExport.headingLetter(LatexExport.headwordForm(e)));
        assertEquals(Set.of("a", "b", "f", "g", "p", "u", "ʃ"), letters, "one per letter before a second of any letter");

        List<String> forms = sample.stream().map(LatexExport::headwordForm).toList();
        assertTrue(forms.contains("agyj"), "the first entry of a");
        assertTrue(forms.contains("batøz"));
        // batøz refers to agyj, pir and f~e: all of them are in.
        assertTrue(forms.contains("pir") && forms.contains("f~e"));

        Set<LiftEntry> withAgyju = LatexExport.sample(d, 8, "fr");
        List<String> more = withAgyju.stream().map(LatexExport::headwordForm).toList();
        assertTrue(more.contains("agyju"), "the second entry of a");
        assertTrue(more.contains("-u"), "the entries agyju refers to");
    }

    @Test
    void aSampleExportHasOnlyTheSampledEntries() throws Exception {
        LiftDictionary d = Utils.loadDictionaryForTest(SAMPLE);
        StringWriter w = new StringWriter();
        LatexExport.export(d, w, LatexExportOptions.defaults().withSortLanguage("fr"), EntrySelection.sample(1));
        String tex = w.toString();
        // The first letter's first entry: agyj, which refers to nothing.
        assertEquals(List.of("A"), letters(tex));
        assertEquals(1, count(body(tex), "\\label{"));
    }

    @Test
    void aFailedExportLeavesTheExistingFile(@TempDir Path dir) throws Exception {
        Path tex = dir.resolve("out.tex");
        Files.writeString(tex, "previous");
        LiftDictionary d = Utils.loadDictionaryForTest(TINY);
        LatexExportOptions bad = LatexExportOptions.defaults().withPolyglossiaLanguages(Map.of("en", "eng;lish"));
        assertThrows(LatexExportException.class, () -> LatexExport.export(d, tex, bad, EntrySelection.all()));
        assertEquals("previous", Files.readString(tex));
        try (var files = Files.list(dir)) {
            assertEquals(List.of(tex), files.toList(), "no temporary file left");
        }
    }

    @Test
    void theCorpusExports() throws Exception {
        LiftDictionary corpus = Utils.loadDictionaryForTest(CORPUS);
        long start = System.nanoTime();
        String tex = export(corpus, LatexExportOptions.suggested(corpus, Utils.resourceFile(CORPUS).toPath()));
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.println("The corpus (" + corpus.getLiftDictionaryRegistry().getEntries().size()
            + " entries) exports in " + ms + " ms, " + tex.length() + " characters.");
        assertTrue(tex.endsWith("\\end{document}"));
        assertEquals(corpus.getLiftDictionaryRegistry().getEntries().stream()
                .filter(e -> !LatexExport.headingLetter(LatexExport.headwordForm(e)).isEmpty()).count(),
            count(body(tex.substring(0, tex.indexOf("Thematic dictionary"))), "\\latexentrystart"),
            "every entry with a form is in the alphabetic part");
        assertFalse(letters(tex).contains(""), "no empty heading");
        assertFalse(letters(tex).contains("͏"), "no heading for the grapheme joiner");
    }

    @Test
    void theStylesheetsAreResourcesOfTheModule() {
        assertTrue(LatexExport.stylesheet().isPresent());
        assertNotNull(LatexExport.class.getResource("latex/latex.xsl"));
        assertNotNull(LatexExport.class.getResource(LatexExport.PARAMETERS));
        assertTrue(new File(Utils.resourceFile(SAMPLE).getParentFile(), "DictionaryLift20261022.lift-ranges").exists());
    }
}
