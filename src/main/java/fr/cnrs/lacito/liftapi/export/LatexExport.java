package fr.cnrs.lacito.liftapi.export;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.WrittingLiftDocumentException;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftIllustration;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.xml.LiftWriterSession;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Collator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Exports a dictionary to LaTeX with the bundled lift2latex stylesheets, and compiles the
 * result to PDF.
 *
 * <p>The stylesheets are XSLT 1.0, run by the JDK's own processor. They are resources of
 * this package ({@code latex/lift2latex.xsl}, {@code latex.xsl}, {@code parameters.xsl}),
 * so they also run with any other XSLT 1.0 processor, e.g. {@code xsltproc}.
 *
 * <pre>
 * LatexExportOptions options = LatexExportOptions.suggested(dictionary, file).withTitle("Lexicon");
 * LatexExport.export(dictionary, Path.of("dict.tex"), options, EntrySelection.all());
 * LatexCompilation pdf = LatexExport.compile(Path.of("dict.tex"),
 *     LatexEngine.require(options.latexEngineOrDefault()), Duration.ofMinutes(5));
 * </pre>
 */
public final class LatexExport {

    private static final Logger LOGGER = Logger.getLogger(LatexExport.class.getName());

    /** The main stylesheet, next to latex.xsl and parameters.xsl, which it imports. */
    static final String STYLESHEET = "latex/lift2latex.xsl";
    static final String PARAMETERS = "latex/parameters.xsl";

    private static final String SEMANTIC_DOMAIN = "semantic-domain-ddp4";
    private static final String CGJ = "͏";

    private LatexExport() {}

    /* ─── Export ─── */

    /**
     * Writes the LaTeX of a dictionary (with its unsaved changes) to {@code texFile}. The
     * file is written next to itself first, then moved into place, so that a failed export
     * leaves an existing file as it was. The illustration directory is written relative to
     * the {@code .tex} when it is inside the {@code .tex}'s folder.
     *
     * @return the messages of the stylesheets ({@code xsl:message}), for information
     */
    public static List<String> export(LiftDictionary dictionary, Path texFile, LatexExportOptions options,
                                      EntrySelection selection) throws LatexExportException {
        // Compiled first: a stylesheet that is missing or does not compile writes nothing.
        Templates templates = templates(options.customStylesheet());
        Path target = texFile.toAbsolutePath();
        Path dir = target.getParent();
        Path tmp;
        try {
            tmp = Files.createTempFile(dir, target.getFileName().toString(), ".tmp");
        } catch (IOException e) {
            throw new LatexExportException("Cannot write in " + dir + ": " + e.getMessage(), e);
        }
        try {
            List<String> messages;
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                messages = transform(templates, serialize(dictionary, selection, options), w,
                    options.stylesheetParameters(dir));
            } catch (IOException e) {
                throw new LatexExportException("Cannot write " + target + ": " + e.getMessage(), e);
            }
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return messages;
        } catch (IOException e) {
            throw new LatexExportException("Cannot write " + target + ": " + e.getMessage(), e);
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Cannot remove " + tmp, e);
            }
        }
    }

    /**
     * Writes the LaTeX of a dictionary (with its unsaved changes). The illustration
     * directory is written as an absolute path.
     *
     * @return the messages of the stylesheets ({@code xsl:message}), for information
     */
    public static List<String> export(LiftDictionary dictionary, Writer out, LatexExportOptions options,
                                      EntrySelection selection) throws LatexExportException {
        Templates templates = templates(options.customStylesheet());
        return transform(templates, serialize(dictionary, selection, options), out, options.stylesheetParameters(null));
    }

    /**
     * Writes the LaTeX of a LIFT file, read directly by the stylesheets.
     *
     * @return the messages of the stylesheets ({@code xsl:message}), for information
     */
    public static List<String> export(File liftFile, Writer out, LatexExportOptions options) throws LatexExportException {
        Templates templates = templates(options.customStylesheet());
        return transform(templates, new StreamSource(liftFile), out, options.stylesheetParameters(null));
    }

    private static StreamSource serialize(LiftDictionary dictionary, EntrySelection selection,
                                          LatexExportOptions options) throws LatexExportException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            if (selection instanceof EntrySelection.Sample s) {
                Set<LiftEntry> chosen = sample(dictionary, s.size(), options.sortLanguage());
                try (LiftWriterSession w = new LiftWriterSession(bytes, baseDirectory(dictionary))) {
                    w.marshall(dictionary, chosen::contains);
                }
            } else {
                dictionary.save(bytes);
            }
        } catch (WrittingLiftDocumentException e) {
            throw new LatexExportException("Cannot serialize the dictionary: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new LatexExportException("Cannot serialize the dictionary: " + e.getMessage(), e);
        }
        return new StreamSource(new ByteArrayInputStream(bytes.toByteArray()));
    }

    private static Path baseDirectory(LiftDictionary dictionary) {
        File f = dictionary.getSourceFile().orElse(null);
        Path dir = f == null ? null : f.getAbsoluteFile().toPath().getParent();
        return dir != null ? dir : Path.of("").toAbsolutePath();
    }

    private static List<String> transform(Templates templates, StreamSource source, Writer out,
                                          Map<String, Object> parameters) throws LatexExportException {
        Diagnostics diagnostics = new Diagnostics();
        try {
            Transformer t = templates.newTransformer();
            t.setErrorListener(diagnostics);
            parameters.forEach(t::setParameter);
            t.transform(source, new StreamResult(out));
            return List.copyOf(diagnostics.messages);
        } catch (TransformerException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "The LaTeX export failed", e);
            throw new LatexExportException("The LaTeX export failed: " + diagnostics.runtimeError(e), e);
        }
    }

    /* ─── Stylesheets ─── */

    /** The compiled bundled stylesheets, made once, on first use. */
    private static final class Stylesheets {
        static final Templates TEMPLATES = compile();

        private static Templates compile() {
            URL url = LatexExport.class.getResource(STYLESHEET);
            try {
                return factory(new Diagnostics()).newTemplates(new StreamSource(url.toExternalForm()));
            } catch (TransformerConfigurationException e) {
                throw new IllegalStateException("The bundled stylesheet " + url + " does not compile", e);
            }
        }
    }

    /**
     * The JDK's processor, never a factory another jar on the class path declares, so that
     * the result does not change with the application's dependencies. Its messages go to
     * {@code diagnostics}, not to the standard error.
     */
    private static TransformerFactory factory(Diagnostics diagnostics) {
        TransformerFactory f = TransformerFactory.newDefaultInstance();
        f.setErrorListener(diagnostics);
        return f;
    }

    /**
     * The stylesheets to run: the bundled ones, or the bundled ones with a custom
     * stylesheet whose definitions replace theirs.
     *
     * <p>XSLT gives the later of two imported stylesheets the higher precedence. A wrapper,
     * built here, imports the bundled {@code lift2latex.xsl} and then the custom file, so
     * that the custom file holds only what it changes, and needs not know where the bundled
     * files are. Its own imports resolve against its own location. It is compiled at each
     * export (about 100 ms), so that an edit of it, or of a file it imports, is always seen.
     */
    static Templates templates(Path customStylesheet) throws LatexExportException {
        if (customStylesheet == null) return Stylesheets.TEMPLATES;
        if (!Files.isRegularFile(customStylesheet)) {
            throw new LatexExportException("The custom stylesheet " + customStylesheet + " does not exist.");
        }
        String bundled = LatexExport.class.getResource(STYLESHEET).toExternalForm();
        String custom = customStylesheet.toAbsolutePath().toUri().toString();
        String wrapper = "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
            + "<xsl:import href=\"" + xmlAttribute(bundled) + "\"/>"
            + "<xsl:import href=\"" + xmlAttribute(custom) + "\"/>"
            + "</xsl:stylesheet>";
        Diagnostics diagnostics = new Diagnostics();
        try {
            return factory(diagnostics).newTemplates(new StreamSource(new java.io.StringReader(wrapper), custom + "#wrapper"));
        } catch (TransformerConfigurationException e) {
            throw new LatexExportException("The custom stylesheet does not compile: " + diagnostics.compileError(e), e);
        }
    }

    private static String xmlAttribute(String s) {
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;");
    }

    /**
     * Collects what the processor reports: the {@code xsl:message}s (as warnings), and the
     * errors, whose location XSLTC puts in the text ("file:/…/custom.xsl: line 12: …").
     */
    static final class Diagnostics implements javax.xml.transform.ErrorListener {
        private static final Pattern LOCATED = Pattern.compile("^(?:file:|jar:|jrt:)\\S*?([^/\\s]+): line (\\d+): (.*)$", Pattern.DOTALL);

        final List<String> messages = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        @Override
        public void warning(TransformerException e) {
            messages.add(e.getMessage());
        }

        @Override
        public void error(TransformerException e) {
            errors.add(e.getMessage());
        }

        @Override
        public void fatalError(TransformerException e) throws TransformerException {
            errors.add(e.getMessage());
            throw e;
        }

        /** "custom.xsl, line 12: message": the first error that has a location, else the first. */
        String compileError(Exception e) {
            for (String error : errors) {
                Matcher m = LOCATED.matcher(error);
                if (m.matches()) return m.group(1) + ", line " + m.group(2) + ": " + m.group(3);
            }
            if (!errors.isEmpty()) return errors.get(0);
            return e.getMessage();
        }

        /** The text of an {@code xsl:message terminate="yes"}, or the error. */
        String runtimeError(Exception e) {
            String text = String.valueOf(e.getMessage());
            if (text.contains("xsl:message") && !messages.isEmpty()) {
                return "the stylesheet stopped: " + messages.get(messages.size() - 1);
            }
            return compileError(e);
        }
    }

    /**
     * The templates of the bundled stylesheets that a custom stylesheet may replace. They
     * are kept stable: renaming one is a breaking change. The rest is internal.
     */
    public static final List<String> EXTENSION_POINTS = List.of(
        "latex-preamble", "latex-package", "latex-styling-command",
        "latex-startdocument", "latex-enddocument", "latex-startsection", "latex-endsection",
        "entry", "sense", "gloss",
        "make_headword", "make_headword_string", "make_illustration",
        "abbreviate_pos", "abbreviate_trait", "abbreviate_slot");

    private static final List<String> MATCH_TEMPLATES = List.of("entry", "sense", "gloss");
    private static final String DISABLED_NAMESPACE = "urn:lift2latex:disabled";

    /**
     * Writes a custom stylesheet that changes nothing: each extension point, and each
     * parameter, as a disabled copy of its bundled definition. A top-level element in
     * another namespace than XSLT's is ignored, so the file is valid and has no effect;
     * renaming {@code disabled:template} to {@code xsl:template} (start and end tags)
     * makes a copy replace the bundled definition.
     */
    public static void writeStylesheetTemplate(Path file) throws IOException {
        String main = resource(STYLESHEET);
        String latex = resource("latex/latex.xsl");
        String parameters = resource(PARAMETERS);
        StringBuilder b = new StringBuilder();
        b.append("""
            <?xml version="1.0" encoding="utf-8"?>
            <!--
             | A custom stylesheet for the LaTeX export of lift-api (and DictionaryEditor).
             |
             | It is loaded after the bundled stylesheets: what it defines replaces their
             | definitions. Give it with lift-api latex (option stylesheet), in the
             | JSON settings (customStylesheet), or in DictionaryEditor's export dialog.
             |
             | Below, each definition that may be replaced is a disabled copy of the bundled
             | one: as long as it is a disabled:template (or disabled:param), it has no effect.
             | To change one, rename disabled:template to xsl:template in its start and end
             | tags, then edit it. Delete the copies you do not need.
             |
             | Templates can call the bundled ones (escape_for_latex, in-language...). The
             | letters (keys and folding strings) cannot be replaced, and xsl:apply-imports
             | does not reach the bundled templates: copy a template to extend it.
             -->
            <xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:disabled=""" + '"' + DISABLED_NAMESPACE + "\">\n\n");
        b.append("<!-- The parameters, with their bundled defaults. A value given from outside still wins. -->\n");
        Matcher params = Pattern.compile("[ \\t]*<xsl:param name=\"[^\"]*\" select=\"[^\"]*\"/>").matcher(parameters);
        while (params.find()) b.append(params.group().replace("<xsl:param ", "<disabled:param ")).append('\n');
        for (String name : EXTENSION_POINTS) {
            String attribute = MATCH_TEMPLATES.contains(name) ? "match" : "name";
            String source = main.contains("<xsl:template " + attribute + "=\"" + name + "\"") ? main : latex;
            Matcher m = Pattern.compile("[ \\t]*<xsl:template " + attribute + "=\"" + Pattern.quote(name) + "\">.*?</xsl:template>",
                Pattern.DOTALL).matcher(source);
            if (!m.find()) throw new IllegalStateException("No template " + name + " in the bundled stylesheets");
            b.append("\n<!-- ").append(attribute.equals("match") ? "The template matching " : "The template ")
                .append(name).append(" -->\n");
            b.append(m.group().replaceFirst("<xsl:template ", "<disabled:template ")
                .replaceFirst("</xsl:template>$", "</disabled:template>")).append('\n');
        }
        b.append("\n</xsl:stylesheet>\n");
        Files.writeString(file, b.toString(), StandardCharsets.UTF_8);
    }

    /** Copies the bundled stylesheets into a directory: lift2latex.xsl, latex.xsl, parameters.xsl. */
    public static List<Path> copyStylesheets(Path directory) throws IOException {
        Files.createDirectories(directory);
        List<Path> written = new ArrayList<>();
        for (String name : List.of("lift2latex.xsl", "latex.xsl", "parameters.xsl")) {
            Path target = directory.resolve(name);
            Files.writeString(target, resource("latex/" + name), StandardCharsets.UTF_8);
            written.add(target);
        }
        return written;
    }

    private static String resource(String name) throws IOException {
        try (InputStream in = LatexExport.class.getResourceAsStream(name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /* ─── Letters ─── */

    /** foldFrom, foldTo, upperFrom, upperTo of parameters.xsl. */
    private static final class Fold {
        static final String FROM;
        static final String TO;
        static final String UPPER_FROM;
        static final String UPPER_TO;

        static {
            Map<String, String> v = new LinkedHashMap<>();
            try (InputStream in = LatexExport.class.getResourceAsStream(PARAMETERS)) {
                DocumentBuilderFactory f = DocumentBuilderFactory.newDefaultInstance();
                f.setNamespaceAware(true);
                NodeList vars = f.newDocumentBuilder().parse(in)
                    .getElementsByTagNameNS("http://www.w3.org/1999/XSL/Transform", "variable");
                for (int i = 0; i < vars.getLength(); i++) {
                    Element e = (Element) vars.item(i);
                    String select = e.getAttribute("select");
                    v.put(e.getAttribute("name"), select.substring(1, select.length() - 1));
                }
            } catch (Exception e) {
                throw new IllegalStateException("Cannot read " + PARAMETERS, e);
            }
            FROM = v.get("foldFrom");
            TO = v.get("foldTo");
            UPPER_FROM = v.get("upperFrom");
            UPPER_TO = v.get("upperTo");
        }
    }

    /**
     * The letter an entry with this form is listed under, as the stylesheet computes it:
     * U+034F removed, a leading "-" skipped, then the first character without case or
     * accent. Empty for an empty form.
     */
    public static String headingLetter(String form) {
        if (form == null) return "";
        String f = form.replace(CGJ, "");
        if (f.startsWith("-")) f = f.substring(1);
        if (f.isEmpty()) return "";
        String c = f.substring(0, 1);
        return translate(c, Fold.FROM, Fold.TO);
    }

    /** The heading written for a letter: in upper case where it has one. */
    public static String headingOf(String letter) {
        return translate(letter, Fold.UPPER_FROM, Fold.UPPER_TO);
    }

    /** XPath's translate(), on UTF-16 units as the JDK's processor does. */
    private static String translate(String s, String from, String to) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int k = from.indexOf(c);
            if (k < 0) b.append(c);
            else if (k < to.length()) b.append(to.charAt(k));
        }
        return b.toString();
    }

    /** The form an entry is listed by: the text of its first form, as written (forms sorted by language). */
    static String headwordForm(LiftEntry entry) {
        return entry.getForms().getForms().stream()
            .min(Comparator.comparing(Form::getLang))
            .map(Form::toPlainText)
            .orElse("");
    }

    /* ─── Sample ─── */

    /**
     * About {@code n} entries spread over the letters: the first entry of each letter (in
     * alphabetical order), then the second of each, and so on; plus the entries they refer
     * to, one level, so that their references resolve.
     */
    static Set<LiftEntry> sample(LiftDictionary dictionary, int n, String sortLanguage) {
        Collator collator = Collator.getInstance(Locale.forLanguageTag(
            sortLanguage == null || sortLanguage.isBlank() ? "en" : sortLanguage));
        Map<String, List<LiftEntry>> byLetter = new TreeMap<>(collator);
        for (LiftEntry e : dictionary.getLiftDictionaryRegistry().getEntries()) {
            String letter = headingLetter(headwordForm(e));
            if (!letter.isEmpty()) byLetter.computeIfAbsent(letter, k -> new ArrayList<>()).add(e);
        }
        Comparator<LiftEntry> byForm = Comparator.comparing(e -> sortForm(headwordForm(e)), collator);
        byLetter.values().forEach(l -> l.sort(byForm));
        Set<LiftEntry> chosen = new LinkedHashSet<>();
        for (int round = 0; chosen.size() < n; round++) {
            boolean any = false;
            for (List<LiftEntry> letter : byLetter.values()) {
                if (round < letter.size() && chosen.size() < n) {
                    chosen.add(letter.get(round));
                    any = true;
                }
            }
            if (!any) break;
        }
        Set<LiftEntry> withTargets = new LinkedHashSet<>(chosen);
        for (LiftEntry e : chosen) withTargets.addAll(referencedEntries(dictionary, e));
        return withTargets;
    }

    private static String sortForm(String form) {
        String f = form.replace(CGJ, "");
        return f.startsWith("-") ? f.substring(1) : f;
    }

    /** The entries an entry refers to: its relations, and those of its senses. */
    private static Set<LiftEntry> referencedEntries(LiftDictionary dictionary, LiftEntry entry) {
        Set<LiftEntry> result = new HashSet<>();
        List<LiftRelation> relations = new ArrayList<>(entry.getRelations());
        for (LiftSense s : allSenses(entry)) relations.addAll(s.getRelations());
        for (LiftRelation r : relations) {
            r.getRefId().ifPresent(ref -> {
                Object target = dictionary.getLiftDictionaryRegistry().getEntryOrSenseByLiftId(ref);
                if (target instanceof LiftEntry t) result.add(t);
                else if (target instanceof LiftSense s && s.getParentEntry() != null) result.add(s.getParentEntry());
            });
        }
        return result;
    }

    private static List<LiftSense> allSenses(LiftEntry entry) {
        List<LiftSense> result = new ArrayList<>();
        ArrayList<LiftSense> todo = new ArrayList<>(entry.getSenses());
        while (!todo.isEmpty()) {
            LiftSense s = todo.remove(0);
            result.add(s);
            todo.addAll(s.getSenses());
        }
        return result;
    }

    /* ─── Checks ─── */

    /**
     * What will go wrong, or look wrong, in an export with these settings. These are
     * warnings: the export stays possible. They read the dictionary, and compile the
     * custom stylesheet if there is one; they do not run the stylesheets.
     *
     * @param texFile the file the export will write, or null
     */
    public static List<LatexExportWarning> check(LiftDictionary dictionary, LatexExportOptions options, Path texFile) {
        List<LatexExportWarning> warnings = new ArrayList<>();
        Map<String, String> languages = options.polyglossiaLanguages() == null ? Map.of() : options.polyglossiaLanguages();

        List<String> unmapped = LatexExportOptions.dictionaryLanguages(dictionary).stream()
            .filter(code -> languages.getOrDefault(code, "").isBlank())
            .toList();
        if (!unmapped.isEmpty()) {
            warnings.add(warning(LatexExportWarning.Kind.LANGUAGE_NOT_MAPPED, unmapped,
                n -> n + (n == 1 ? " language has" : " languages have") + " no polyglossia name: its texts are "
                    + "written in the default language"));
        }

        List<String> unknown = languages.values().stream().filter(v -> !v.isBlank())
            .map(String::trim).distinct().filter(v -> !Polyglossia.isKnown(v)).toList();
        if (!unknown.isEmpty()) {
            warnings.add(warning(LatexExportWarning.Kind.UNKNOWN_POLYGLOSSIA_NAME, unknown,
                n -> n + (n == 1 ? " name is" : " names are") + " not known to polyglossia (a typo, or a newer "
                    + "polyglossia): LaTeX will stop on it"));
        }

        List<LiftIllustration> illustrations = dictionary.getLiftDictionaryRegistry().getIllustrations();
        if (options.figuresOrDefault() && !illustrations.isEmpty()) {
            Path dir = options.illustrationDirectory();
            if (dir == null) {
                warnings.add(warning(LatexExportWarning.Kind.NO_ILLUSTRATION_DIRECTORY,
                    illustrations.stream().map(LiftIllustration::getHref).toList(),
                    n -> "The dictionary has " + n + " illustration" + (n == 1 ? "" : "s")
                        + ", and no illustration directory is set"));
            } else {
                List<String> missing = illustrations.stream().map(LiftIllustration::getHref)
                    .filter(href -> href != null && !Files.isRegularFile(dir.resolve(href)))
                    .distinct().toList();
                if (!missing.isEmpty()) {
                    warnings.add(warning(LatexExportWarning.Kind.ILLUSTRATIONS_MISSING, missing,
                        n -> n + " illustration file" + (n == 1 ? " is" : "s are") + " not in " + dir
                            + ": LaTeX will stop on " + (n == 1 ? "it" : "them")));
                }
            }
        }

        if (options.thematicDictionaryOrDefault()) {
            boolean anyDomain = false;
            for (LiftTrait t : dictionary.getLiftDictionaryRegistry().getTraits()) {
                if (SEMANTIC_DOMAIN.equals(t.getName()) && t.getParentNode() instanceof LiftSense) {
                    anyDomain = true;
                    break;
                }
            }
            if (!anyDomain) {
                warnings.add(warning(LatexExportWarning.Kind.NO_SEMANTIC_DOMAIN, List.of(),
                    n -> "No sense has a semantic domain (" + SEMANTIC_DOMAIN + "): the thematic part will be empty"));
            }
        }

        List<String> withoutForm = dictionary.getLiftDictionaryRegistry().getEntries().stream()
            .filter(e -> headingLetter(headwordForm(e)).isEmpty())
            .map(e -> e.getId().orElse("?"))
            .toList();
        if (!withoutForm.isEmpty()) {
            warnings.add(warning(LatexExportWarning.Kind.ENTRY_WITHOUT_FORM, withoutForm,
                n -> n + (n == 1 ? " entry has" : " entries have") + " no form: left out of the alphabetic part"));
        }

        if (options.fonts() != null) {
            Set<String> names = new HashSet<>();
            languages.values().forEach(v -> names.add(v.trim()));
            List<String> orphans = options.fonts().entrySet().stream()
                .filter(e -> !e.getValue().isBlank() && !names.contains(e.getKey().trim()))
                .map(Map.Entry::getKey).toList();
            if (!orphans.isEmpty()) {
                warnings.add(warning(LatexExportWarning.Kind.FONT_WITHOUT_LANGUAGE, orphans,
                    n -> n + " font" + (n == 1 ? " is" : "s are") + " given for a polyglossia name no language maps to"));
            }
        }

        Path custom = options.customStylesheet();
        if (custom != null) {
            if (!Files.isRegularFile(custom)) {
                warnings.add(warning(LatexExportWarning.Kind.CUSTOM_STYLESHEET_MISSING, List.of(custom.toString()),
                    n -> "The custom stylesheet " + custom + " does not exist"));
            } else {
                try {
                    templates(custom);
                } catch (LatexExportException e) {
                    String error = e.getMessage().replaceFirst("^The custom stylesheet does not compile: ", "");
                    warnings.add(warning(LatexExportWarning.Kind.CUSTOM_STYLESHEET_INVALID, List.of(error),
                        n -> "The custom stylesheet does not compile: " + error));
                }
            }
        }

        if (texFile != null) {
            List<String> existing = new ArrayList<>();
            if (Files.exists(texFile)) existing.add(texFile.toString());
            Path pdf = pdfOf(texFile);
            if (Files.exists(pdf)) existing.add(pdf.toString());
            if (!existing.isEmpty()) {
                warnings.add(warning(LatexExportWarning.Kind.OUTPUT_EXISTS, existing,
                    n -> String.join(" and ", existing) + (n == 1 ? " exists and will" : " exist and will") + " be replaced"));
            }
        }
        return warnings;
    }

    private static LatexExportWarning warning(LatexExportWarning.Kind kind, List<String> items,
                                              java.util.function.IntFunction<String> message) {
        return new LatexExportWarning(kind, message.apply(items.size()), items.size(),
            items.stream().limit(5).toList());
    }

    /** The PDF next to a .tex: same name, .pdf. */
    public static Path pdfOf(Path texFile) {
        String name = texFile.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return texFile.resolveSibling((dot > 0 ? name.substring(0, dot) : name) + ".pdf");
    }

    /* ─── Compilation ─── */

    private static final Pattern FILE_LINE_ERROR = Pattern.compile("^(.*?):(\\d+): (.*)$");

    /**
     * Compiles a {@code .tex} to PDF, in its folder: twice, for the references and the
     * table of contents, and a third time when the log asks for it. Each run is killed
     * after {@code timeout}. The method can be cancelled by interrupting its thread, which
     * kills the engine.
     *
     * @return the result: on failure, the first error of the log
     * @throws LatexExportException when the engine cannot be started, times out, or is cancelled
     */
    public static LatexCompilation compile(Path texFile, LatexEngine engine, Duration timeout) throws LatexExportException {
        Path tex = texFile.toAbsolutePath();
        Path dir = tex.getParent();
        String name = tex.getFileName().toString();
        Path log = tex.resolveSibling(name.replaceFirst("\\.[^.]*$", "") + ".log");
        Path pdf = pdfOf(tex);
        int runs = 0;
        while (true) {
            runs++;
            int exit = run(engine, dir, name, timeout);
            String logText = readLog(log);
            if (exit != 0) {
                String[] error = firstError(logText);
                return new LatexCompilation(false, engine, pdf, log, runs,
                    error == null ? engine.kind().displayName() + " stopped (exit code " + exit + ")." : error[0],
                    error == null || error[1] == null ? null : Integer.valueOf(error[1]));
            }
            boolean rerun = logText.contains("Rerun to get") || logText.contains("Label(s) may have changed");
            if (runs >= 3 || (runs >= 2 && !rerun)) break;
        }
        return new LatexCompilation(Files.isRegularFile(pdf), engine, pdf, log, runs, null, null);
    }

    private static int run(LatexEngine engine, Path dir, String texName, Duration timeout) throws LatexExportException {
        ProcessBuilder pb = new ProcessBuilder(engine.path().toString(),
            "-interaction=nonstopmode", "-halt-on-error", "-file-line-error", texName)
            .directory(dir.toFile())
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD);
        Process p;
        try {
            p = pb.start();
        } catch (IOException e) {
            throw new LatexExportException("Cannot run " + engine.path() + ": " + e.getMessage(), e);
        }
        try {
            if (!p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                p.descendants().forEach(ProcessHandle::destroyForcibly);
                p.destroyForcibly();
                throw new LatexExportException(engine.kind().displayName() + " did not finish within "
                    + timeout.toSeconds() + " s, and was stopped.");
            }
            return p.exitValue();
        } catch (InterruptedException e) {
            p.descendants().forEach(ProcessHandle::destroyForcibly);
            p.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new LatexExportException("The compilation was cancelled.", e);
        }
    }

    private static String readLog(Path log) {
        try {
            // TeX writes its log in the encoding of the system, or as bytes: read it leniently.
            return new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    /** {text, line} of the first error of a log, or null. */
    static String[] firstError(String log) {
        String[] lines = log.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i];
            Matcher m = FILE_LINE_ERROR.matcher(l);
            boolean fileLine = m.matches() && !l.startsWith("l.");
            if (fileLine || l.startsWith("! ")) {
                StringBuilder b = new StringBuilder(l);
                for (int j = i + 1; j < Math.min(lines.length, i + 3); j++) b.append('\n').append(lines[j]);
                String line = fileLine ? m.group(2) : lineOfBangError(lines, i);
                return new String[] {b.toString(), line};
            }
        }
        return null;
    }

    /** After a "! message", TeX gives "l.<n> ..." a few lines below. */
    private static String lineOfBangError(String[] lines, int from) {
        for (int j = from + 1; j < Math.min(lines.length, from + 10); j++) {
            Matcher m = Pattern.compile("^l\\.(\\d+)").matcher(lines[j]);
            if (m.find()) return m.group(1);
        }
        return null;
    }

    /* ─── Opening ─── */

    /**
     * Opens a file in the platform's default application: {@code open} on macOS,
     * {@code xdg-open} on Linux and the BSDs, {@code rundll32 url.dll,FileProtocolHandler}
     * on Windows. It does not wait for the viewer.
     *
     * <p>{@code java.awt.Desktop} would do the same, but it would make this library need the
     * {@code java.desktop} module (AWT) for one call.
     */
    public static void openWithDefaultViewer(Path file) throws LatexExportException {
        List<String> command = openCommand(System.getProperty("os.name", ""), file.toAbsolutePath());
        try {
            new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        } catch (IOException e) {
            throw new LatexExportException("Cannot open " + file + " (" + command.get(0) + ": " + e.getMessage()
                + "). Open it yourself.", e);
        }
    }

    static List<String> openCommand(String osName, Path file) {
        String os = osName.toLowerCase(Locale.ROOT);
        if (os.startsWith("mac") || os.startsWith("darwin")) return List.of("open", file.toString());
        if (os.startsWith("windows")) return List.of("rundll32", "url.dll,FileProtocolHandler", file.toString());
        return List.of("xdg-open", file.toString());
    }

    /** For tests: whether the sample selection is the one {@code selection} names. */
    static Predicate<LiftEntry> selected(LiftDictionary dictionary, EntrySelection selection, String sortLanguage) {
        if (selection instanceof EntrySelection.Sample s) return sample(dictionary, s.size(), sortLanguage)::contains;
        return e -> true;
    }

    static Optional<URL> stylesheet() {
        return Optional.ofNullable(LatexExport.class.getResource(STYLESHEET));
    }
}
