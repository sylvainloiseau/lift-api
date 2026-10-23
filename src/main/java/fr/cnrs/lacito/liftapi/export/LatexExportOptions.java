package fr.cnrs.lacito.liftapi.export;

import com.fasterxml.jackson.jr.ob.JSON;
import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryLanguagesManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * The settings of a LaTeX export: the parameters of the stylesheets, and the engine that
 * compiles the result.
 *
 * <p>Every component may be null, which means "not given": {@link #merge} then keeps the
 * value it is merged into, and the stylesheets use their default. The JSON settings file
 * ({@link #fromJson}, {@link #writeJson}) uses the names of the stylesheet parameters
 * (comments give them below).
 *
 * @param title {@code title}
 * @param author {@code author}
 * @param thematicDictionary {@code makeThematicDictionary}: a part by semantic domain
 * @param listOfFigures {@code makeListOfFigure}
 * @param figures {@code makeFigure}: the illustrations, as figures
 * @param illustrationDirectory {@code urlillustration}: where the illustration files are
 * @param polyglossiaLanguages {@code languages}: the polyglossia name of each LIFT language code
 * @param sortLanguage {@code sortLanguage}: the language code whose alphabetical order sorts
 *        the entries; its polyglossia name is the document's default language
 * @param mainFont {@code mainFont}
 * @param fonts {@code fonts}: a font for some polyglossia names
 * @param latexEngine {@code latexEngine}: not a stylesheet parameter; {@code auto},
 *        {@code xelatex} or {@code lualatex}, never a path
 * @param customStylesheet {@code customStylesheet}: not a stylesheet parameter; an XSLT
 *        file loaded after the bundled stylesheets, whose definitions replace theirs (see
 *        {@link LatexExport}). It is code: an application that accepts it from others
 *        (a web service) must compile it with secure processing, extension functions off
 */
public record LatexExportOptions(
    String title,
    String author,
    Boolean thematicDictionary,
    Boolean listOfFigures,
    Boolean figures,
    Path illustrationDirectory,
    Map<String, String> polyglossiaLanguages,
    String sortLanguage,
    String mainFont,
    Map<String, String> fonts,
    LatexEngineRequest latexEngine,
    Path customStylesheet
) {

    static final String TITLE = "title";
    static final String AUTHOR = "author";
    static final String THEMATIC = "makeThematicDictionary";
    static final String LIST_OF_FIGURES = "makeListOfFigure";
    static final String FIGURES = "makeFigure";
    static final String ILLUSTRATIONS = "urlillustration";
    static final String LANGUAGES = "languages";
    static final String SORT_LANGUAGE = "sortLanguage";
    static final String MAIN_FONT = "mainFont";
    static final String FONTS = "fonts";
    static final String LATEX_ENGINE = "latexEngine";
    static final String CUSTOM_STYLESHEET = "customStylesheet";

    /** The JSON keys, in the order they are written. */
    public static final List<String> KEYS = List.of(TITLE, AUTHOR, THEMATIC, LIST_OF_FIGURES, FIGURES,
        ILLUSTRATIONS, LANGUAGES, SORT_LANGUAGE, MAIN_FONT, FONTS, LATEX_ENGINE, CUSTOM_STYLESHEET);

    public LatexExportOptions {
        polyglossiaLanguages = polyglossiaLanguages == null ? null
            : Collections.unmodifiableMap(new TreeMap<>(polyglossiaLanguages));
        fonts = fonts == null ? null : Collections.unmodifiableMap(new TreeMap<>(fonts));
        if (latexEngine != null && latexEngine.path().isPresent()) {
            throw new IllegalArgumentException("The settings name an engine kind, not a program: " + latexEngine);
        }
    }

    /** Nothing given: the stylesheets' defaults. */
    public static LatexExportOptions defaults() {
        return new LatexExportOptions(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Settings suggested for a first export of a dictionary:
     * <ul>
     * <li>each language (object and meta) whose code polyglossia knows gets its name;</li>
     * <li>the sort language is the language of most glosses and definitions;</li>
     * <li>the illustrations are in the dictionary's {@code pictures/} folder when it exists
     *     (FieldWorks puts them there), and figures are on when the dictionary has
     *     illustrations and that folder exists.</li>
     * </ul>
     *
     * @param dictionaryFile the dictionary's file, or null
     */
    public static LatexExportOptions suggested(LiftDictionary dictionary, Path dictionaryFile) {
        Map<String, String> languages = new TreeMap<>();
        for (String code : dictionaryLanguages(dictionary)) {
            Polyglossia.nameFor(code).ifPresent(name -> languages.put(code, name));
        }
        String sort = mainMetaLanguage(dictionary);
        Path pictures = null;
        Boolean figures = null;
        if (dictionaryFile != null && dictionaryFile.toAbsolutePath().getParent() != null) {
            Path p = dictionaryFile.toAbsolutePath().getParent().resolve("pictures");
            if (Files.isDirectory(p)) {
                pictures = p;
                figures = !dictionary.getLiftDictionaryRegistry().getIllustrations().isEmpty();
            }
        }
        return new LatexExportOptions(null, null, null, null, figures, pictures,
            languages.isEmpty() ? null : languages, sort, null, null, null, null);
    }

    /**
     * The language most glosses and definitions are in: the one readers read the dictionary
     * in. The counts of the language manager are not used, since they include the header,
     * whose range descriptions are often in English whatever the dictionary.
     */
    static String mainMetaLanguage(LiftDictionary dictionary) {
        Map<String, Integer> counts = new TreeMap<>();
        for (fr.cnrs.lacito.liftapi.model.LiftSense s : dictionary.getLiftDictionaryRegistry().getSenses()) {
            for (String lang : s.getGlosses().getLangs()) counts.merge(lang, 1, Integer::sum);
            for (String lang : s.getDefinition().getLangs()) counts.merge(lang, 1, Integer::sum);
        }
        if (counts.isEmpty()) {
            LiftDictionaryLanguagesManager meta = dictionary.getMetaLanguageManager();
            return meta.getLanguages().stream()
                .max(Comparator.comparingInt(meta::getLanguageOccurrence).thenComparing(Comparator.reverseOrder()))
                .orElse(null);
        }
        return counts.entrySet().stream()
            .max(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
            .map(Map.Entry::getKey).orElse(null);
    }

    /** The object and meta languages of a dictionary, sorted, each once. */
    static List<String> dictionaryLanguages(LiftDictionary dictionary) {
        java.util.TreeSet<String> all = new java.util.TreeSet<>(dictionary.getObjectLanguageManager().getLanguages());
        all.addAll(dictionary.getMetaLanguageManager().getLanguages());
        return List.copyOf(all);
    }

    /**
     * The components of {@code overrides} that are not null replace these; the maps merge
     * per key, so that an override changes one language and keeps the others.
     */
    public LatexExportOptions merge(LatexExportOptions overrides) {
        if (overrides == null) return this;
        return new LatexExportOptions(
            or(overrides.title, title),
            or(overrides.author, author),
            or(overrides.thematicDictionary, thematicDictionary),
            or(overrides.listOfFigures, listOfFigures),
            or(overrides.figures, figures),
            or(overrides.illustrationDirectory, illustrationDirectory),
            mergeMaps(polyglossiaLanguages, overrides.polyglossiaLanguages),
            or(overrides.sortLanguage, sortLanguage),
            or(overrides.mainFont, mainFont),
            mergeMaps(fonts, overrides.fonts),
            or(overrides.latexEngine, latexEngine),
            or(overrides.customStylesheet, customStylesheet));
    }

    private static <T> T or(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private static Map<String, String> mergeMaps(Map<String, String> base, Map<String, String> overrides) {
        if (base == null) return overrides;
        if (overrides == null) return base;
        Map<String, String> m = new TreeMap<>(base);
        m.putAll(overrides);
        return m;
    }

    public LatexExportOptions withTitle(String v) { return merge(only(TITLE, v)); }
    public LatexExportOptions withAuthor(String v) { return merge(only(AUTHOR, v)); }
    public LatexExportOptions withThematicDictionary(Boolean v) { return merge(only(THEMATIC, v)); }
    public LatexExportOptions withListOfFigures(Boolean v) { return merge(only(LIST_OF_FIGURES, v)); }
    public LatexExportOptions withFigures(Boolean v) { return merge(only(FIGURES, v)); }
    public LatexExportOptions withIllustrationDirectory(Path v) { return merge(only(ILLUSTRATIONS, v)); }
    public LatexExportOptions withSortLanguage(String v) { return merge(only(SORT_LANGUAGE, v)); }
    public LatexExportOptions withMainFont(String v) { return merge(only(MAIN_FONT, v)); }
    public LatexExportOptions withLatexEngine(LatexEngineRequest v) { return merge(only(LATEX_ENGINE, v)); }
    public LatexExportOptions withCustomStylesheet(Path v) { return merge(only(CUSTOM_STYLESHEET, v)); }

    /** These settings with {@code languages} instead of the current mapping (not merged). */
    public LatexExportOptions withPolyglossiaLanguages(Map<String, String> languages) {
        return new LatexExportOptions(title, author, thematicDictionary, listOfFigures, figures,
            illustrationDirectory, languages, sortLanguage, mainFont, fonts, latexEngine, customStylesheet);
    }

    /** These settings with {@code fonts} instead of the current ones (not merged). */
    public LatexExportOptions withFonts(Map<String, String> fonts) {
        return new LatexExportOptions(title, author, thematicDictionary, listOfFigures, figures,
            illustrationDirectory, polyglossiaLanguages, sortLanguage, mainFont, fonts, latexEngine, customStylesheet);
    }

    private static LatexExportOptions only(String key, Object v) {
        return new LatexExportOptions(
            key.equals(TITLE) ? (String) v : null,
            key.equals(AUTHOR) ? (String) v : null,
            key.equals(THEMATIC) ? (Boolean) v : null,
            key.equals(LIST_OF_FIGURES) ? (Boolean) v : null,
            key.equals(FIGURES) ? (Boolean) v : null,
            key.equals(ILLUSTRATIONS) ? (Path) v : null,
            null,
            key.equals(SORT_LANGUAGE) ? (String) v : null,
            key.equals(MAIN_FONT) ? (String) v : null,
            null,
            key.equals(LATEX_ENGINE) ? (LatexEngineRequest) v : null,
            key.equals(CUSTOM_STYLESHEET) ? (Path) v : null);
    }

    /** Whether the thematic part is made: the stylesheet's default is yes. */
    public boolean thematicDictionaryOrDefault() {
        return thematicDictionary == null || thematicDictionary;
    }

    public boolean figuresOrDefault() {
        return figures != null && figures;
    }

    public boolean listOfFiguresOrDefault() {
        return listOfFigures != null && listOfFigures;
    }

    public LatexEngineRequest latexEngineOrDefault() {
        return latexEngine == null ? LatexEngineRequest.auto() : latexEngine;
    }

    /* ─── JSON ─── */

    /**
     * Reads a JSON settings file. Every key is optional. An unknown key, or a value of the
     * wrong type, is an error that names the key. A relative {@code urlillustration} is
     * relative to the file's directory.
     */
    public static LatexExportOptions fromJson(Path file) throws LatexExportException {
        Map<String, Object> m;
        try {
            m = JSON.std.mapFrom(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new LatexExportException("Cannot read the settings file " + file + ": " + e.getMessage(), e);
        }
        Path base = file.toAbsolutePath().getParent();
        try {
            return fromMap(m, base);
        } catch (LatexExportException e) {
            throw new LatexExportException(file + ": " + e.getMessage(), e);
        }
    }

    /** As {@link #fromJson}, from the JSON text; relative paths are relative to {@code base}. */
    public static LatexExportOptions fromJson(String json, Path base) throws LatexExportException {
        try {
            return fromMap(JSON.std.mapFrom(json), base);
        } catch (IOException e) {
            throw new LatexExportException("Invalid settings: " + e.getMessage(), e);
        }
    }

    private static LatexExportOptions fromMap(Map<String, Object> m, Path base) throws LatexExportException {
        if (m == null) throw new LatexExportException("The settings must be a JSON object.");
        for (String key : m.keySet()) {
            if (!KEYS.contains(key)) {
                String hint = closest(key).map(k -> "; did you mean " + k + "?").orElse("");
                throw new LatexExportException("unknown key " + key + hint + " The keys are: " + String.join(", ", KEYS) + ".");
            }
        }
        Path dir = path(string(m, ILLUSTRATIONS), base);
        Path custom = path(string(m, CUSTOM_STYLESHEET), base);
        String engine = string(m, LATEX_ENGINE);
        LatexEngineRequest request = null;
        if (engine != null) {
            request = LatexEngineRequest.parse(engine);
            if (request.path().isPresent()) {
                throw new LatexExportException(LATEX_ENGINE + " must be auto, xelatex or lualatex, not " + engine
                    + " (a program's path is not saved in the settings).");
            }
        }
        return new LatexExportOptions(string(m, TITLE), string(m, AUTHOR), bool(m, THEMATIC),
            bool(m, LIST_OF_FIGURES), bool(m, FIGURES), dir, map(m, LANGUAGES), string(m, SORT_LANGUAGE),
            string(m, MAIN_FONT), map(m, FONTS), request, custom);
    }

    /** A path of the settings: relative to the settings file's directory. */
    private static Path path(String value, Path base) {
        if (value == null || value.isBlank()) return null;
        Path p = Path.of(value);
        return p.isAbsolute() || base == null ? p : base.resolve(p).normalize();
    }

    private static String string(Map<String, Object> m, String key) throws LatexExportException {
        Object v = m.get(key);
        if (v == null || v instanceof String) return (String) v;
        throw new LatexExportException(key + " must be a string, not " + v + ".");
    }

    private static Boolean bool(Map<String, Object> m, String key) throws LatexExportException {
        Object v = m.get(key);
        if (v == null || v instanceof Boolean) return (Boolean) v;
        throw new LatexExportException(key + " must be true or false, not " + v + ".");
    }

    private static Map<String, String> map(Map<String, Object> m, String key) throws LatexExportException {
        Object v = m.get(key);
        if (v == null) return null;
        if (!(v instanceof Map<?, ?> raw)) {
            throw new LatexExportException(key + " must be an object such as {\"fr\": \"french\"}, not " + v + ".");
        }
        Map<String, String> result = new TreeMap<>();
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            if (!(e.getValue() instanceof String s)) {
                throw new LatexExportException(key + "." + e.getKey() + " must be a string, not " + e.getValue() + ".");
            }
            result.put(String.valueOf(e.getKey()), s);
        }
        return result;
    }

    /** The key at the smallest edit distance from {@code key}, when it is at most 3. */
    private static java.util.Optional<String> closest(String key) {
        String best = null;
        int bestDistance = 4;
        for (String k : KEYS) {
            int d = distance(key.toLowerCase(java.util.Locale.ROOT), k.toLowerCase(java.util.Locale.ROOT));
            if (d < bestDistance) {
                bestDistance = d;
                best = k;
            }
        }
        return java.util.Optional.ofNullable(best);
    }

    private static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }

    /**
     * Writes these settings as JSON, with only the components that are given. The
     * illustration directory is written relative to the file's directory when it is
     * inside it, so that a folder holding both can be moved.
     */
    public void writeJson(Path file) throws IOException {
        Files.writeString(file, toJson(file.toAbsolutePath().getParent()), StandardCharsets.UTF_8);
    }

    /** The JSON of {@link #writeJson}; the illustration directory is relative to {@code base} when inside it. */
    public String toJson(Path base) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        put(m, TITLE, title);
        put(m, AUTHOR, author);
        put(m, THEMATIC, thematicDictionary);
        put(m, LIST_OF_FIGURES, listOfFigures);
        put(m, FIGURES, figures);
        if (illustrationDirectory != null) m.put(ILLUSTRATIONS, relative(illustrationDirectory, base));
        put(m, LANGUAGES, polyglossiaLanguages);
        put(m, SORT_LANGUAGE, sortLanguage);
        put(m, MAIN_FONT, mainFont);
        put(m, FONTS, fonts);
        if (latexEngine != null) put(m, LATEX_ENGINE, latexEngine.settingsValue().orElse(null));
        if (customStylesheet != null) m.put(CUSTOM_STYLESHEET, relative(customStylesheet, base));
        return JSON.std.with(JSON.Feature.PRETTY_PRINT_OUTPUT).asString(m) + "\n";
    }

    /** A path, relative to {@code base} when it is inside it, with "/" as the separator. */
    private static String relative(Path path, Path base) {
        Path p = path.toAbsolutePath().normalize();
        String s = base != null && p.startsWith(base.toAbsolutePath().normalize())
            ? base.toAbsolutePath().normalize().relativize(p).toString()
            : p.toString();
        return s.isEmpty() ? "." : s.replace(java.io.File.separatorChar, '/');
    }

    private static void put(Map<String, Object> m, String key, Object value) {
        if (value != null) m.put(key, value);
    }

    /* ─── Stylesheet parameters ─── */

    /**
     * The parameters of the stylesheets for these settings. {@code illustrationsBase} is
     * the directory the {@code .tex} is written in: the illustration directory is then
     * written relative to it when it is inside it; null writes it absolute.
     */
    Map<String, Object> stylesheetParameters(Path illustrationsBase) throws LatexExportException {
        Map<String, Object> p = new LinkedHashMap<>();
        if (title != null) p.put(TITLE, title);
        if (author != null) p.put(AUTHOR, author);
        if (thematicDictionary != null) p.put(THEMATIC, thematicDictionary);
        if (listOfFigures != null) p.put(LIST_OF_FIGURES, listOfFigures);
        if (figures != null) p.put(FIGURES, figures);
        if (illustrationDirectory != null) p.put(ILLUSTRATIONS, urlIllustration(illustrationDirectory, illustrationsBase));
        if (polyglossiaLanguages != null) p.put(LANGUAGES, pairs(LANGUAGES, polyglossiaLanguages));
        if (sortLanguage != null) p.put(SORT_LANGUAGE, sortLanguage);
        if (mainFont != null) p.put(MAIN_FONT, mainFont);
        if (fonts != null) p.put(FONTS, pairs(FONTS, fonts));
        return p;
    }

    /** "/abs/dir/" or "rel/dir/": "/" as the separator, which LaTeX needs even on Windows, and a trailing "/". */
    static String urlIllustration(Path dir, Path base) {
        Path d = dir.toAbsolutePath().normalize();
        String s;
        if (base != null && d.startsWith(base.toAbsolutePath().normalize())) {
            s = base.toAbsolutePath().normalize().relativize(d).toString();
        } else {
            s = d.toString();
        }
        s = s.replace('\\', '/');
        if (s.isEmpty()) return "";
        return s.endsWith("/") ? s : s + "/";
    }

    private static String pairs(String parameter, Map<String, String> map) throws LatexExportException {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, String> e : map.entrySet()) {
            String k = e.getKey().trim();
            String v = Objects.requireNonNullElse(e.getValue(), "").trim();
            if (v.isEmpty()) continue;
            if (k.contains(";") || k.contains("=") || v.contains(";") || v.contains("=")) {
                throw new LatexExportException(parameter + ": \"" + k + "\" = \"" + v + "\" may not contain ';' or '='.");
            }
            out.add(k + "=" + v);
        }
        return String.join(";", out);
    }
}
