# Exporting a dictionary to LaTeX (and PDF) with the lift2latex stylesheets: plan (2026-10-23, revised three times on 2026-10-23)

## Goal

The XSLT stylesheets of `../lift2latex` turn a LIFT dictionary into a LaTeX document.
Today they are a separate repository, run by hand with an XSLT 2.0 processor, and their
parameters are edited in place (`parameters.xsl` holds one dictionary's title and a path
on the author's machine).

They move into lift-api, rewritten in **XSLT 1.0** for the JDK's own processor, behind a
small Java API. Then:
- the conversion runs anywhere lift-api runs: from a program, from lift-api's command
  line, and later from a web service;
- it goes up to the PDF when XeLaTeX or LuaLaTeX is installed;
- DictionaryEditor offers a dialog for the parameters, checks them before exporting, and
  can preview a sample;
- lift-api's tests check that what lift-api writes is still what the stylesheets read.

**Revisions:**
- First revision (2026-10-23), after the user's answers:
  - the stylesheets lose their own licence;
  - the command line takes every parameter of `parameters.xsl`, and a JSON file;
  - `lift-api <file>` keeps working;
  - DictionaryEditor gets a directory chooser and a polyglossia field per language.
- Second revision (2026-10-23):
  - option B is chosen (XSLT 1.0, no Saxon);
  - "Load / Save settings" and the per-dictionary memory are accepted.
- Third revision (2026-10-23):
  - the improvements first proposed in §6 are adopted and placed in the steps:
    - the PDF (step 5);
    - the checks (step 4);
    - the defaults (steps 3 and 9);
    - the preview on a sample (steps 3, 7 and 9);
    - `--write-config` (step 7);
    - the fonts (steps 2 and 3);
    - the alphabetical order, in XSLT 1.0 (step 2);
  - exporting only the filtered entries stays for later (§7);
  - the test data: a 10-entry extract of the corpus, and `tiny.xml` (step 8).
- Fourth revision (2026-10-23):
  - the engine search prefers XeLaTeX over LuaLaTeX;
  - the user can choose the engine on the command line and in the dialog;
  - the PDF can be opened in the platform's default viewer at the end (step 5).

---

## 1. Current state

### 1.1 `../lift2latex` (github.com/sylvainloiseau/lift2latex, 4 commits)

| File | Lines | Role |
|---|---|---|
| `lift2latex.xsl` | 601 | main stylesheet: `version="2.0"`, `xsl:import`s the two below, `xsl:output method="text"` |
| `latex.xsl` | 288 | LaTeX preamble and styling commands (named templates); `version="1.0"` |
| `parameters.xsl` | 59 | settings, as **`xsl:variable`**s (so they cannot be set from outside) |
| `style.xsl` | 0 | empty |

**What they use beyond XSLT 1.0** (19 lines in all):
- `xsl:for-each-group` in 3 places:
  - the entries by first letter, ignoring a leading `-` (`lift2latex.xsl:23`);
  - the senses by semantic domain, `semantic-domain-ddp4` (`:49`);
  - an entry's relations by type (`:169`);
- XPath 2.0 `if … then … else` in the grouping and sort keys;
- `replace()` in `escape_for_latex`. It only escapes `\` and `_`, and `\` → `\\` is wrong
  for LaTeX (a line break);
- an XPath 3.1 map for `languages`, read with `map:keys` and `$languages(.)`
  (`latex.xsl:242–250`).

**The settings, all hard-coded in `parameters.xsl`:**

| Parameter | Type | Current value |
|---|---|---|
| `title` | string | "Saint-Agnant-de-Versillat variety: lexicon" |
| `author` | string | empty |
| `makeThematicDictionary` | boolean | `true()` |
| `makeListOfFigure` | boolean | `false()` |
| `makeFigure` | boolean | `false()` |
| `urlillustration` | string, prefixed to `illustration/@href` | a path on the author's machine |
| `languages` | map from LIFT language code to polyglossia name | `fr`, `tpi` → `french`, `tww` → `tuwari`, `en` → `english` |

The output needs **XeLaTeX or LuaLaTeX** (`fontspec`, `polyglossia`).

### 1.2 What the corpus shows

`src/test/resources/lift/DictionaryLift20261022.lift`: 2,604 entries.

| Item | Corpus data | Consequence for the export |
|---|---|---|
| Languages | `oc-FR-fonipa` (3,289 forms), `fr` (1,167), `en` (27), `qaa-x-spec` (6) | none of these codes is a polyglossia name, so the mapping matters |
| `_` in glosses | 239 entries (`prendre_les_tiges`) | escaped today |
| `~` | 3 entries (`f~e`) | not escaped today |
| `\` | 1 entry (`mauvais\DEG.COMP`, entry `pir`) | today it becomes a LaTeX line break |
| Leading `-` | 95 forms (affixes) | |
| Initial outside a–z | 273 forms (`ʃ`, `ʒ`, `ã`, `ɛ`…) | |
| Upper-case initial | 3 forms (`Fødu`, `SMS`, `CHU`) | |
| U+034F (combining grapheme joiner) | inside `galup͏ɛt`, and at the **start** of `͏ʃəbri` | today `͏ʃəbri` gets a letter heading of its own, which looks empty |
| Illustrations | 3 (`galup͏ɛt`, `ʃataɲɛr`, `arbøj`); their files are not in the repository | |
| Entry relations | `_component-lexeme` (474), `Comparer` (4) | |
| Sense relations | `Synonyme`, `Quasi synonyme`, … | |
| Semantic domains | in 856 entries | |
| Examples | 7 entries (`batøz`…) | |
| Subsenses | 1 entry (`arɛ̃ʒ`) | |
| Entries with no form | none | |

`src/test/resources/lift/tiny.xml`:
- 2 entries, languages `tww`, `en` and `tpi`;
- `_` in a gloss (`which_kind`);
- an entry id with a space (`kemia napuo_…`), which a `\label{}` must survive;
- a semantic domain and a note.

### 1.3 lift-api

- **Module** `fr.cnrs.lacito.liftapi`:
  - it exports `liftapi`, `builder`, `model` and `change`;
  - it `requires transitive java.xml` (which holds `javax.xml.transform` and the JDK's
    XSLTC processor) and `javafx.base`, and `requires info.picocli`.
- **Command line:** `cli.Main` (picocli), a single command that prints a dictionary's
  languages.
- **Build:** a jar-with-dependencies whose main class is `cli.Main`; published to Maven
  Central and GitHub Packages; licence AFL-3.0.
- **`LiftDictionary`:**
  - `loadDictionaryFromFile(File)`, `save()`, `save(File)`;
  - **no save to a stream**;
  - `getObjectLanguageManager()` and `getMetaLanguageManager()` list the languages.

### 1.4 DictionaryEditor's packaging

DictionaryEditor is a named module.
- It builds a runtime with `maven-jlink-plugin` (profile `dist`), then installers with
  `jpackage`.
- jlink accepts only explicit modules. Option B keeps this working: the processor is in
  `java.xml`, and jackson-jr (step 6) is a real module.

### 1.5 Why not Saxon (for the record)

Saxon-HE 13.1 is the only maintained open-source XSLT 2.0 and 3.0 processor for Java. It
was set aside for three reasons:
- it adds about 7 MB with `xmlresolver`;
- its jar is a signed automatic module, which jlink refuses: DictionaryEditor would need
  the `moditect` plugin, rechecked at each upgrade;
- the stylesheets use XSLT 2.0+ in only 19 lines.

Reconsider it if the stylesheets come to need XSLT 3.0, for example a full alphabet with
digraphs (§7).

---

## 2. Work plan in lift-api

### Step 1: bring the files over, with their history

- **Target directory:** `src/main/resources/fr/cnrs/lacito/liftapi/export/latex/`. It is
  the path of the Java package that loads the files (step 3): in a named module, a
  package's resources are encapsulated, so only lift-api reads them, and nothing needs
  `opens`.
- **Keep the history:**
  `git subtree add --prefix=src/main/resources/fr/cnrs/lacito/liftapi/export/latex ../lift2latex master`.
- **Then, in a separate commit:**
  - remove `LICENSE`, `style.xsl` (empty), `.gitignore` and `.DS_Store`. The stylesheets
    are covered by lift-api's licence, with no mention of their own;
  - fold `README.md` (two lines) into lift-api's README (step 8).
- **Before changing anything,** record the reference output:
  - make the test data of step 8;
  - run the current stylesheets on it and on the corpus once, by hand, with any XSLT 2.0
    processor (Saxon on the command line, as a tool, not a dependency);
  - keep the `.tex` files. The rewrite of step 2 is checked against them.
- **Afterwards:** archive the `lift2latex` repository on GitHub, with a README pointing to
  lift-api.

### Step 2: the stylesheets: XSLT 1.0, parameters, escaping, alphabetical order, fonts

**XSLT 1.0.**
- `version="1.0"` on the three stylesheets.
- Each `for-each-group` becomes Muenchian grouping: an `xsl:key`, and
  `generate-id() = generate-id(key(…)[1])`.
  - **Letters:** on the heading letter defined below.
  - **Semantic domains:** on `@value` of `sense/trait[@name='semantic-domain-ddp4']`.
  - **Relations by type,** within an entry: on
    `concat(generate-id(ancestor::entry), '|', @type)`, so that the groups stay per entry.
  - Each key sits next to the template that uses it, with a comment that gives the
    XSLT 2.0 form it replaces.

**Alphabetical order, in XSLT 1.0.**
- **The heading letter** of an entry is the first character of its form, after:
  - removing U+034F (combining grapheme joiner), and skipping a leading `-`. `͏ʃəbri` goes
    under `ʃ`, and `-u` under `u`;
  - **removing accents and case,** with `translate()`. Upper case goes to lower case for
    the Latin letters, and the precomposed accented letters go to their base letter:
    `É`, `é`, `è`, `ê` → `e`, `Ã`, `ã` → `a`, `Ø`, `ø` → `ø` (a letter of its own, not
    an accented `o`), and so on.
    - The two strings of `translate()` are defined once, in variables of
      `parameters.xsl` (`foldFrom`, `foldTo`), with a comment.
    - They cover Latin-1 Supplement and Latin Extended-A. A form written with a combining
      accent (`a` + U+0303) already starts with the base letter.
  - The heading is written in upper case (`translate()` the other way); a letter with no
    upper case (`ʃ`, `ʒ`, `ɛ`, `ŋ`, `ə`) stays as it is.
- **The key** is one XPath expression, used for both grouping and sorting, since an
  `xsl:key` cannot call a template.
  - It is built from `substring()` and `translate()`: first remove U+034F with
    `translate(form, '&#x34F;', '')`, then skip a leading `-` with
    `substring(…, 1 + starts-with(…, '-'), 1)`, then fold the character.
  - It covers a leading `-`, U+034F anywhere, and both together.
  - A comment says what it does not handle: other invisible characters, which the corpus
    does not have.
- **Sorting within a letter:** `xsl:sort` with `lang="{$sortLanguage}"`. The JDK's
  processor then uses a `java.text.Collator`, which ignores case and accents at the
  primary level. So `ãtʃe` sorts with the `a`s, and `Fødu` with the `f`s.
  - The new parameter `sortLanguage` defaults to `''`, which is code-point order as
    today.
  - DictionaryEditor and the command line set it from the first meta language: `fr` for
    the corpus.
  - A test checks the Collator behaviour, since this depends on XSLTC.
- **Letters that are not single characters** (digraphs such as `ng`, `ch`), and a
  user-defined alphabet, are not handled (§7).

**Parameters.** In `parameters.xsl`, `xsl:variable` → **`xsl:param`**, with neutral
defaults. The names are also the JSON keys (step 6).

| Parameter | Default | Origin |
|---|---|---|
| `title`, `author` | `''` | `parameters.xsl` |
| `makeThematicDictionary` | `true()` | `parameters.xsl` |
| `makeListOfFigure`, `makeFigure` | `false()` | `parameters.xsl` |
| `urlillustration` | `''` (paths relative to the `.tex` file) | `parameters.xsl` |
| `languages` | `''`: `code=name` pairs separated by `;`, e.g. `'fr=french;en=english;tww=tuwari'` | `parameters.xsl` |
| `sortLanguage` | `''` (code-point order) | new: alphabetical order |
| `mainFont` | `''` (the LaTeX default) | new: fonts |
| `fonts` | `''`: `polyglossia-name=font` pairs separated by `;`, e.g. `'french=Charis SIL;tuwari=Doulos SIL'` | new: fonts |

- The separator is `;` because font names contain spaces.
- These are global parameters of the main stylesheet. Java sets them, and someone running
  the files by hand (`xsltproc --stringparam title …`) can too.
- **Booleans:** a parameter set from outside is a string in XSLT 1.0. Java passes
  `Boolean`, which JAXP converts, but `xsltproc --stringparam makeFigure false` gives the
  string `'false'`, which is *true*. So all booleans are read through one named template
  that accepts `true()` and `'true'`.

**Languages and fonts in the preamble.**
- A recursive template walks the `languages` pairs, and writes `\setotherlanguage{…}` once
  per *polyglossia name*. Today `fr` and `tpi`, both mapping to `french`, write it twice.
- `polyglossia-name(code)` returns the name for a code, or `''`, wherever the stylesheet
  switches language.
- `mainFont` → `\setmainfont{…}`.
- Each `fonts` pair → `\newfontfamily\<name>font{…}`, which polyglossia uses for that
  language.

**Escaping.**
- `escape_for_latex` becomes a recursive template that replaces each of
  `\ { } $ & # ^ _ % ~` with its LaTeX form: `\textbackslash{}`, `\{`, `\}`, `\$`, `\&`,
  `\#`, `\^{}`, `\_`, `\%`, `\textasciitilde{}`.
- Every text taken from the dictionary goes through it: forms, glosses, definitions,
  notes, examples, translations, illustration labels.
- **Ids in `\label{}` and `\ref{}`** need a safe form instead: an id can hold spaces
  (`kemia napuo_…` in `tiny.xml`), and non-ASCII letters. Keep letters, digits, `-` and
  `:`, and turn anything else into `-`. The guid suffix keeps the result unique.

**Check against the reference** of step 1. The output must be the same, except where the
escaping, the headings or the order changed on purpose. Check those differences by eye
once, then make the new output the expected file of step 8.

### Step 3: the Java API, `fr.cnrs.lacito.liftapi.export`

**A new exported package:**

```java
/** Exports a dictionary to LaTeX with the bundled lift2latex stylesheets, and compiles it. */
public final class LatexExport {
    /** Writes the .tex next to which illustrations paths are resolved (see below). */
    public static void export(LiftDictionary dictionary, Path texFile,
                              LatexExportOptions options, EntrySelection selection)
        throws LatexExportException;
    public static void export(LiftDictionary dictionary, Writer out,
                              LatexExportOptions options, EntrySelection selection)
        throws LatexExportException;
    public static void export(File liftFile, Writer out, LatexExportOptions options)
        throws LatexExportException;

    public static List<LatexExportWarning> check(LiftDictionary dictionary,
                                                 LatexExportOptions options, Path texFile);  // step 4
    public static LatexCompilation compile(Path texFile, LatexEngine engine,
                                           Duration timeout) throws LatexExportException; // step 5
    public static void openWithDefaultViewer(Path file) throws LatexExportException;  // step 5
}

/** The parameters of the stylesheets. */
public record LatexExportOptions(
    String title,                        // title
    String author,                       // author
    Boolean thematicDictionary,          // makeThematicDictionary
    Boolean listOfFigures,               // makeListOfFigure
    Boolean figures,                     // makeFigure
    Path illustrationDirectory,          // urlillustration
    Map<String, String> polyglossiaLanguages,   // languages: LIFT code -> polyglossia name
    String sortLanguage,                 // sortLanguage
    String mainFont,                     // mainFont
    Map<String, String> fonts,           // fonts: polyglossia name -> font
    LatexEngineRequest latexEngine) {    // not a stylesheet parameter: the engine (step 5);
                                         // AUTO, XELATEX or LUALATEX, never a path

    public static LatexExportOptions defaults();
    /** Defaults suggested from a dictionary: languages, sort language, illustrations. */
    public static LatexExportOptions suggested(LiftDictionary dictionary, Path dictionaryFile);
    public static LatexExportOptions fromJson(Path file) throws LatexExportException;
    public void writeJson(Path file) throws IOException;
    /** The components of {@code overrides} that are not null replace these; maps merge per key. */
    public LatexExportOptions merge(LatexExportOptions overrides);
    // with…(…) for each component
}

/** Which entries go into the export. */
public sealed interface EntrySelection {
    static EntrySelection all();
    /** About n entries spread over the letters, with the entries they refer to. */
    static EntrySelection sample(int n);
}
```

**Options.**
- **`null` means "not given"** in every component, so that `merge` can apply the
  precedence of step 7; `defaults()` has none.
- **`LatexExportException`** is checked, like `LiftDocumentLoadingException`. It carries a
  message for the user, and the cause.

**`suggested(dictionary, dictionaryFile)`** provides the defaults for a first export:
- **languages:** each object and meta language whose code polyglossia knows gets its name.
  The table is `Polyglossia` (below), matching on the primary subtag, so `fr-FR` →
  `french`. The others are left out, and step 4 warns about them;
- **`sortLanguage`:** the first meta language;
- **`illustrationDirectory`:** the dictionary's `pictures/` folder when it exists
  (FieldWorks puts them there);
- **`figures`:** true when the dictionary has illustrations and that folder exists.

**`Polyglossia`,** a small public class:
- the list of polyglossia's language names, from its documentation, as a resource file
  `polyglossia-languages.txt`;
- the mapping from ISO 639-1 and the common ISO 639-3 codes to those names;
- `names()`, `nameFor(String code)`, `isKnown(String name)`;
- used by `suggested`, by the checks, and by DictionaryEditor's combo boxes.

**`illustrationDirectory` → `urlillustration`:**
- with `export(…, Path texFile, …)`: **relative to the `.tex`** when the directory is
  inside the `.tex`'s folder (e.g. `pictures/`), so that the folder can be moved or sent as
  a whole; absolute otherwise;
- with the `Writer` variants: absolute;
- always with `/` as the separator (LaTeX does not accept `\`, even on Windows), and with
  a trailing `/`, since the stylesheet concatenates it with `@href`;
- a path with spaces: check what `\includegraphics` needs in the tests (recent LaTeX
  kernels accept spaces).

**Maps → string parameters:**
- joined with `;`;
- a code or a name containing `;` or `=` is rejected with a message.

**`EntrySelection.sample(n)`:**
- takes the entries round-robin over the heading letters, so that the preview shows the
  headings: one per letter, then a second per letter, and so on, up to `n`;
- adds the entries they refer to (one level), so that `\ref`s resolve;
- the heading letter is computed in Java by `LatexExport.headingLetter(String form)`,
  with the same rule as the stylesheet. A test checks that the two agree on every form of
  the corpus.

**Processor.**
- `TransformerFactory.newDefaultInstance()`: always the JDK's XSLTC, not a factory that
  another jar on the class path declares.
- The stylesheet is `LatexExport.class.getResource("latex/lift2latex.xsl")`, with that URL
  as the system id, so that the relative `xsl:import`s resolve inside the jar.
- Compile once into a `Templates` (thread-safe), lazily, in a holder class.
- XSLTC compiles templates into bytecode: a very large template (the preamble in
  `latex.xsl`) could hit the 64 KB method limit. Check it in step 2, and split the
  template if it does.

**Input.**
- From a `File`: a `StreamSource` on the file.
- From a `LiftDictionary`, which may hold unsaved edits: a new public
  `LiftDictionary.save(OutputStream)`, written to a byte array, then transformed.
- With a selection other than `all()`: the internal writer writes only the selected
  entries. This is not public: the `xml` package is not exported.

**Output.** Text, UTF-8. With a `Path`, write to a temporary file in the same folder, then
move it into place, so that a failed export leaves the previous `.tex` intact.

**Errors.**
- A `TransformerException` becomes a `LatexExportException` with a plain message.
- The stylesheet line goes to the log.

**Module system:** `exports fr.cnrs.lacito.liftapi.export;`

### Step 4: checks before exporting

`LatexExport.check(dictionary, options, texFile)` returns `LatexExportWarning`s. Each has a
kind, a message, a count, and up to five examples. They are warnings: the export stays
possible.

| Kind | When |
|---|---|
| `LANGUAGE_NOT_MAPPED` | an object or meta language used in the dictionary has no polyglossia name |
| `UNKNOWN_POLYGLOSSIA_NAME` | a mapped name is not in `Polyglossia.names()` (perhaps a typo, perhaps a newer polyglossia) |
| `ILLUSTRATIONS_MISSING` | `figures` is on and some `illustration/@href` files are not in the directory |
| `NO_ILLUSTRATION_DIRECTORY` | `figures` is on, the dictionary has illustrations, and no directory is set |
| `NO_SEMANTIC_DOMAIN` | `thematicDictionary` is on and no sense has a `semantic-domain-ddp4` trait: an empty section |
| `ENTRY_WITHOUT_FORM` | entries with no form, which have no letter |
| `FONT_WITHOUT_LANGUAGE` | a font is given for a polyglossia name that no language maps to |
| `OUTPUT_EXISTS` | the `.tex` (or the PDF) exists and will be replaced |

The checks read the model; they do not run the stylesheet. The command line prints them
(step 7), and the dialog lists them (step 9).

### Step 5: compiling to PDF

`LatexExport.compile(texFile, engine, timeout)`:

**The engine.**
- **Kinds:** `LatexEngineKind` is `XELATEX` or `LUALATEX`. Both compile the same `.tex`,
  since the stylesheets use `fontspec` and `polyglossia`, which both support. Choosing
  makes sense:
  - a user may have only one installed;
  - LuaLaTeX handles some complex scripts and large fonts better;
  - XeLaTeX is usually faster, and is the default.
- **The request:** `LatexEngineRequest` is `AUTO`, `XELATEX`, `LUALATEX`, or an explicit
  path. `AUTO` is the default.
- **Search order:** for `AUTO`, **XeLaTeX first**, then LuaLaTeX. For a kind, only that
  kind. For a path, that program; its kind is guessed from its name, or taken from
  `--latex-engine-kind`.
- **Where to look:**
  - on the `PATH`;
  - then where TeX distributions install, since a macOS application started from the
    Finder does not get the shell's `PATH`:
    - `/Library/TeX/texbin` and `/usr/local/texlive/*/bin/*` (macOS, Linux; the most
      recent year first);
    - `%LOCALAPPDATA%\Programs\MiKTeX\miktex\bin\x64`,
      `%ProgramFiles%\MiKTeX\miktex\bin\x64` and `C:\texlive\*\bin\windows`
      (Windows).
- **API:**
  - `LatexEngine.find(LatexEngineRequest)` returns the engine to use, or empty, with its
    kind, path and version (`xelatex --version`, first line);
  - `LatexEngine.findAll()` returns every engine found, for the dialog's list.
- **When the requested kind is not found:** this is an error, not a silent switch to the
  other kind: the user asked for it. The message says what was found instead ("LuaLaTeX
  not found; XeLaTeX is installed at …").
- **In the JSON settings:**
  - the **kind** may be given (`"latexEngine": "xelatex" | "lualatex" | "auto"`), since it
    belongs to the document: fonts can behave slightly differently;
  - the **path** may not, since it belongs to the machine. It is given by
    `--latex-engine <path>`, the environment variable `LIFT_API_LATEX`, or
    DictionaryEditor's preferences.

- **Runs** the engine with `-interaction=nonstopmode -halt-on-error -file-line-error`, in
  the `.tex`'s folder:
  - twice, for the references and the table of contents;
  - a third time when the log asks for it ("Rerun to get cross-references right").
- **The timeout** applies to each run, which is killed when it expires.
- **The result** (`LatexCompilation`): success, the PDF path, the log path, and on failure
  the **first error of the log**: the `! …` line, with the `file:line:` before it and the
  two lines after.
- **It can be cancelled:** the method checks for interruption and kills the process.
- It only runs a process: no dependency.

**Opening the PDF in the platform's default viewer.**
- `LatexExport.openWithDefaultViewer(Path)` runs the platform's "open" command:
  - `open <file>` on macOS;
  - `xdg-open <file>` on Linux (and the BSDs);
  - `rundll32 url.dll,FileProtocolHandler <file>` on Windows. Unlike `cmd /c start`, it
    has no quoting trap with spaces in the path.
- It does not wait for the viewer, and fails with a clear message when the command is
  missing (a Linux without `xdg-utils`, or a headless server).
- `java.awt.Desktop.open` would do the same, but it needs the `java.desktop` module (AWT)
  in lift-api, for one call, and is unreliable on some Linux desktops. A library used on
  servers should not require AWT.
- **DictionaryEditor** uses JavaFX's `HostServices.showDocument` instead (step 9), which
  it already has.

### Step 6: the JSON library

- **`com.fasterxml.jackson.jr:jackson-jr-objects` 2.22.4.** About 110 KB, plus
  `jackson-core`. Both are real modules (`module-info` in `META-INF/versions/9`), so
  jlink accepts them.
- `requires com.fasterxml.jackson.jr.ob;` in lift-api. It is not transitive: no JSON type
  appears in the API.
- `JSON.std.mapFrom(…)` reads the file into a `Map`. `fromJson` then checks each key and
  type itself, to give precise messages.

### Step 7: the command line

- **`cli.Main` becomes a command with subcommands:**
  - `lift-api info <file>`: the current behaviour;
  - `lift-api <file>` keeps working: when the first argument is not a subcommand, run
    `info`;
  - `lift-api latex <file>`:

    | Option | Effect |
    |---|---|
    | `-o, --output <file.tex>` | the output; standard output when absent (then no PDF) |
    | `--title <text>` | `title` |
    | `--author <text>` | `author` |
    | `--[no-]thematic-dictionary` | `makeThematicDictionary` |
    | `--[no-]list-of-figures` | `makeListOfFigure` |
    | `--[no-]figures` | `makeFigure` |
    | `--illustrations <dir>` | `urlillustration` |
    | `--language <code>=<polyglossia>`, repeatable | `languages` |
    | `--sort-language <code>` | `sortLanguage` |
    | `--main-font <font>` | `mainFont` |
    | `--font <polyglossia>=<font>`, repeatable | `fonts` |
    | `--config <file.json>` | all of the above, from a file |
    | `--suggest` | start from `LatexExportOptions.suggested(…)` instead of the defaults |
    | `--sample <n>` | export a sample (`EntrySelection.sample(n)`) |
    | `--pdf` | also compile; the engine is found as `AUTO` (XeLaTeX first) unless one of the next two options is given |
    | `--latex-engine <auto\|xelatex\|lualatex\|path>` | which engine (implies `--pdf`); `latexEngine` in the JSON file |
    | `--latex-engine-kind <xelatex\|lualatex>` | the kind of the program given by path, when its name does not tell |
    | `--open` | open the PDF in the default viewer at the end (implies `--pdf`) |
    | `--list-engines` | list the engines found, with kind, path and version, and stop |
    | `--write-config <file.json>` | write the resulting settings, and stop: no export |

    Picocli handles the negatable booleans (`negatable = true`, as `Boolean` so that "not
    given" is `null`), and turns the repeated `--language` and `--font` options into maps.
- **Precedence:** defaults (or `--suggest`), then the JSON file, then the command line:
  `base.merge(fromJson(config)).merge(fromCommandLine)`. The maps merge per key, so
  `--language tww=tuwari` changes one mapping and keeps the file's others.
- **Warnings:** the checks of step 4 are printed on standard error before exporting.
  `--strict` makes them errors.
- **The JSON file** uses the names of the stylesheet parameters:

  ```json
  {
    "title": "Saint-Agnant-de-Versillat variety: lexicon",
    "author": "",
    "makeThematicDictionary": true,
    "makeListOfFigure": false,
    "makeFigure": true,
    "urlillustration": "pictures/",
    "languages": { "oc-FR-fonipa": "occitan", "fr": "french", "en": "english" },
    "sortLanguage": "fr",
    "mainFont": "Charis SIL",
    "fonts": { "occitan": "Doulos SIL" },
    "latexEngine": "xelatex"
  }
  ```

  - Every key is optional.
  - An unknown key, or a value of the wrong type, is an error that names the key and lists
    the valid ones (`makeFigures` → "unknown key makeFigures; did you mean makeFigure?").
  - A relative `urlillustration` is relative to the JSON file.
- **Exit codes:**
  - 0: success;
  - 1: the export or the compilation failed. The first LaTeX error is printed, with the
    log path;
  - 2: a bad command line or JSON file;
  - 3: warnings with `--strict`.
- **Examples** (in the README and `--help`):

  ```
  lift-api latex dict.lift --suggest --write-config dict.json      # a starting settings file
  lift-api latex dict.lift --config dict.json --sample 50 --open -o preview.tex
  lift-api latex dict.lift --config dict.json --latex-engine lualatex --pdf -o dict.tex
  lift-api latex --list-engines
  ```

### Step 8: test data, tests and documentation (lift-api)

**Test data,** in `src/test/resources/lift/`:
- **`latex-sample.lift`:** 10 entries extracted from `DictionaryLift20261022.lift`. It
  keeps the corpus's header, so that it shares its `.lift-ranges` file.

  | Entry | What it covers |
  |---|---|
  | `agyju` | two `_component-lexeme` relations, to the next two entries; a semantic domain |
  | `agyj` | target of a relation; heading `a` |
  | `-u` | leading `-`, goes under `u`; target of a relation |
  | `ãtʃe` | accented initial: under `a`, sorted with the `a`s |
  | `arɛ̃ʒ` | a subsense |
  | `galup͏ɛt` | an illustration, a note, a definition, `_` in a gloss, U+034F inside the form |
  | `batøz` | examples, a semantic domain, two senses |
  | `Fødu` | upper-case initial: under `f` |
  | `f~e` | `~` in the form |
  | `pir` | `\` in a gloss (`mauvais\DEG.COMP`) |

  It also has three additions made by hand, each marked by an XML comment, for what the
  corpus lacks:
  - a `Comparer` relation from `agyju` to `agyj`, so that an entry has relations of two
    types;
  - a note on `pir` holding `{ } $ & # ^ %`;
  - an entry whose form starts with U+034F, as `͏ʃəbri` in the corpus, with its two
    relations removed, so that it does not refer outside the file.
- **`tiny.xml`** (existing, 2 entries):
  - the languages `tww`, `en` and `tpi`, none of which `Polyglossia` knows except `en`;
  - an id with a space, for `\label`;
  - `_` in a gloss;
  - a semantic domain.
- **An entry with no form:** built in the test with the builders, not stored in a file,
  since lift-api may refuse to read one.
- **A picture:** one small PNG named like `galup͏ɛt`'s illustration, in a test folder
  `pictures/`. `arbøj`'s picture is absent from the extract, and the corpus's other two
  are missing, which tests `ILLUSTRATIONS_MISSING`.

**`LatexExportTest`:**
- `latex-sample.lift` → compared exactly with `latex-sample.expected.tex` (line endings
  normalized), so that any change of output is a visible diff.
- `tiny.xml` → the label of `kemia napuo_…` is safe, and `which_kind` is escaped.
- Each parameter has its effect:
  - title, author, each boolean;
  - the illustration prefix, relative and absolute, with a space in the path;
  - two codes mapped to one polyglossia name give one `\setotherlanguage`;
  - `mainFont` and `fonts` give `\setmainfont` and `\newfontfamily`;
  - `sortLanguage = fr` puts `ãtʃe` among the `a`s and `Fødu` among the `f`s.
- A boolean passed as the string `'false'` counts as false.
- The headings: `-u` under `U`, `Fødu` under `F`, and the U+034F entry under `ʃ`. No
  empty heading.
- Exporting a `LiftDictionary` loaded from the file gives the same text as exporting the
  file. This also tests `save(OutputStream)`.
- `headingLetter` in Java agrees with the stylesheet on every form of the corpus.
- `sample(3)` on `latex-sample.lift`: one entry per letter first, plus the targets of
  `agyju`'s relations when it is chosen.
- A failed export leaves an existing `.tex` unchanged.
- The corpus exports without error, and the output equals the reference of step 1 apart
  from the intended changes. Note the run time.

**`LatexExportCheckTest`:** one test per warning kind, on `latex-sample.lift` and
`tiny.xml`. For example, `tiny.xml` with suggested options gives `LANGUAGE_NOT_MAPPED` for
`tww` and `tpi`.

**`LatexEngineTest`** (no TeX needed: the search runs over a temporary folder holding
fake `xelatex` and `lualatex` scripts):
- `AUTO` finds XeLaTeX when both exist, and LuaLaTeX when only it exists;
- a requested kind that is missing is an error naming the other kind found;
- a path's kind is guessed from its name, or given;
- `latexEngine` is read from and written to the JSON;
- `openWithDefaultViewer` builds the right command for each platform (the command is
  checked, not run).

**`LatexCompileTest`,** only when an engine is found (`Assumptions.assumeTrue`):
- `latex-sample.lift` compiles to a PDF;
- a `.tex` with an error gives the first `!` line and its line number;
- the timeout kills a run.

**`LatexExportOptionsTest`:**
- JSON reading: all keys, some keys, an unknown key with a suggestion, a wrong type, a
  relative `urlillustration`;
- the merge, including per-key maps;
- `writeJson` then `fromJson` gives back the same options;
- `suggested` on `tiny.xml` and on the corpus.

**`cli` tests:**
- `latex` with options only, with `--config` only, with both (the command line wins);
- `--suggest --write-config`;
- `--sample`;
- the exit codes, including `--strict`;
- `lift-api <file>` still printing the languages.

**Documentation:**
- lift-api `README.md`, a "LaTeX export" section:
  - the Java call and the command line, with the examples of step 7;
  - the JSON format;
  - running the stylesheets by hand with `xsltproc` (with `--stringparam`);
  - XeLaTeX or LuaLaTeX, and where to get them (TeX Live, MacTeX, MiKTeX);
- Javadoc on the package and the types.

---

## 3. Work plan in DictionaryEditor

### Step 9: the export dialog

- **Menu:** Tools ▸ "Export as LaTeX / PDF…", next to "Export as CSV".

**Fields:**
- **Title**, **Author**: text fields.
- **Three checkboxes:** thematic dictionary, list of figures, figures.
- **Illustrations directory:** a text field with "Browse…", which opens a
  `DirectoryChooser` (it is a directory, not a file). It is disabled while "figures" is
  unchecked.
- **Languages:** a grid with two sections, object languages and meta languages. It has one
  row per language of the dictionary, with three columns:
  - the code;
  - the **polyglossia name**: an editable `ComboBox` over `Polyglossia.names()`, so that a
    typo is less likely and a name absent from the list can still be typed;
  - the **font**: an editable `ComboBox` over the system's font families
    (`javafx.scene.text.Font.getFamilies()`, the names XeLaTeX finds); empty means the
    main font.

  An empty name means "not mapped". A language that is both object and meta appears once,
  in the object section. Two codes with the same polyglossia name share one font: editing
  one row's font updates the other.
- **Main font:** the same font `ComboBox`, empty for the LaTeX default.
- **Sort order:** a `ComboBox` of the dictionary's meta languages, plus "code points".
- **Output file:** a text field with "Browse…" (a `FileChooser` for `.tex`). The PDF goes
  next to it with the same name.
- **"Also make the PDF":** a checkbox, with, beside it:
  - **The engine:** a `ComboBox` with "Automatic (XeLaTeX, else LuaLaTeX)", "XeLaTeX" and
    "LuaLaTeX".
    - Each item shows what was found: "XeLaTeX — TeX Live 2026, /Library/TeX/texbin",
      or "LuaLaTeX — not found".
    - An item not found cannot be chosen.
    - The choice is saved with the settings (`latexEngine`).
  - **"Choose the program…":** a `FileChooser` that sets a path for one kind. It is stored
    in the application's preferences, not in the settings, since it belongs to the
    machine.
  - **"Open the PDF when done":** a checkbox, checked by default, remembered in the
    preferences.
  - When no engine is found at all, the PDF box is disabled. A message says so, with a
    link to the installation instructions (TeX Live, MacTeX, MiKTeX) and "Choose the
    program…".
  - `LatexEngine.findAll()` runs once when the dialog opens, off the FX thread, since it
    starts processes for the versions.

**Defaults, for a first export of a dictionary:** `LatexExportOptions.suggested(…)`.
- The output is next to the dictionary, with the same name and `.tex`.
- The sort order is the first meta language.
- The PDF box is checked when an engine is found.

**Checks:**
- under the fields, the warnings of `LatexExport.check`, updated as fields change
  (debounced);
- each warning has an icon and its message, and expands to its examples;
- a warning about a language selects that language's row when clicked;
- warnings do not block the export.

**Buttons:**
- **"Preview":** exports `EntrySelection.sample(50)` to a temporary folder, compiles it,
  and opens the PDF (`HostServices.showDocument`). It needs an engine; without one, it
  opens the `.tex`. It does not change the settings or the output file.
- **"Load settings…", "Save settings…":** read and write a JSON file, in the format of
  `--config`, so that settings move between the dialog and the command line.
- **"Export":** writes the `.tex`, then the PDF when asked.
- **"Close".**

**Settings memory:** the dialog opens with the last settings used for this dictionary.
They are stored in the application's preferences, keyed by the dictionary's path, as the
JSON of `writeJson`.

**Running the export and the compilation:**
- off the FX thread, as one task with two phases ("Writing the LaTeX file", "Compiling,
  pass 1/2");
- a progress bar and a "Cancel" button, which interrupts the task and kills LaTeX;
- the dialog stays open, its fields disabled, while it runs.

**At the end:**
- **On success:**
  - when "Open the PDF when done" is checked, the PDF opens in the platform's default
    viewer (`HostServices.showDocument` on the file's URI), and the dialog closes;
  - otherwise, or when only the `.tex` was made, a message with the paths and "Open PDF",
    "Open .tex", "Show in folder" buttons (`HostServices.showDocument` on the file and on
    its folder).
- **On a LaTeX error:**
  - the first error and its line;
  - a "Show the log" button, which opens the log in a read-only text area, scrolled to
    the error;
  - the `.tex` is kept, so that the user can fix it or report it.
- **On an export error:** `Dialogs.error` with the message.

**Packaging:** nothing to change; jackson-jr comes with lift-api, as a real module.

**Tests (TestFX):**
- the dialog lists the demo dictionary's object and meta languages, pre-filled where
  `Polyglossia` knows the code;
- an unmapped language shows `LANGUAGE_NOT_MAPPED`, and clicking it selects its row;
- a filled dialog produces a `.tex` with the title, the mapping and the fonts;
- "Save settings…" then "Load settings…" gives the same fields back;
- the settings come back when the dialog is reopened;
- the defaults of a first export: output next to the dictionary, sort language;
- with no engine (simulated), the PDF box is disabled;
- the engine list shows XeLaTeX before LuaLaTeX, and an engine not found cannot be
  chosen (simulated search results);
- the engine choice is saved and loaded with the settings;
- compiling, preview included, only when an engine is found.

---

## 4. Order

1. lift-api, step 1: the move (one commit); the test data of step 8, and the reference
   output.
2. Step 2: the XSLT 1.0 rewrite, parameters, escaping, alphabetical order, fonts, checked
   against the reference.
3. Steps 3, 4 and 6: the API, the checks, jackson-jr.
4. Step 5: the compilation.
5. Steps 7 and 8: the command line, the tests and the documentation. Then a release of
   lift-api (`mvn deploy`).
6. DictionaryEditor, step 9.

---

## 5. Decided

- **Processor:** XSLT 1.0 with the JDK's processor (§1.5); JSON with jackson-jr.
- **Licence:** the stylesheets have no licence of their own; they are part of lift-api
  (AFL-3.0).
- **Command line:**
  - every parameter has an option, and a JSON file uses the same names;
  - the command line overrides the file, which overrides the defaults;
  - `lift-api <file>` keeps working;
  - `--write-config` writes the resulting settings.
- **Dialog:**
  - a directory chooser for the illustrations;
  - a polyglossia name and a font for each object and meta language;
  - "Load / Save settings", and per-dictionary memory of the settings.
- **Adopted improvements:**
  - the PDF (step 5): XeLaTeX first, then LuaLaTeX; the engine can be chosen on the
    command line (`--latex-engine`) and in the dialog; the PDF opens in the default viewer
    (`--open`, and a checkbox in the dialog);
  - the checks (step 4);
  - the defaults of a first export (steps 3 and 9);
  - the preview on a sample (steps 3, 7 and 9);
  - the fonts (steps 2, 3 and 9);
  - the alphabetical order without case or accents, in XSLT 1.0 (step 2).

---

## 6. Questions

None at the moment.

---

## 7. Later

- **Export the entries of the current filter.** Export only what DictionaryEditor's entry
  list shows (a search, a filter), for a partial lexicon or a word list for one semantic
  domain.
  - It is `EntrySelection.matching(Predicate<LiftEntry>)`, beside `sample(n)`.
  - A reference to an entry outside the selection must be decided: show it as plain text
    without `\ref`, or include the referenced entry.
- **A user-defined alphabet:**
  - ordered letters, digraphs included (`ng`, `ch`);
  - letters that the Collator does not order as the language does.

  This is hard in XSLT 1.0, which cannot compute a grouping key with a template. It is the
  main reason to reconsider XSLT 3.0 (§1.5). Another way: lift-api computes the heading
  and the sort key in Java, and passes them to the stylesheet as attributes of the
  serialized entries. The rule then lives in Java alone, but the stylesheets no longer
  run by hand on a plain LIFT file.

---

## 8. Implementation status (2026-10-23)

**Done**: steps 1–9. Tests pass:
- lift-api: 299 tests, including the compilation with XeLaTeX and LuaLaTeX;
- DictionaryEditor: 135 tests, including a PDF made from the dialog.

DictionaryEditor's jlink image builds, and contains jackson-jr. The export runs from inside
the image.

**Commits.** Only step 1 is committed in lift-api:
- the subtree import of lift2latex;
- the working copy of lift2latex, which had uncommitted changes in its own repository;
- the removal of `LICENSE`, `style.xsl`, `.gitignore` and `README.md`.

The rest is uncommitted in both repositories. The `lift2latex` repository is not archived
on GitHub: that is for the user to do.

**Where the code differs from the plan:**
- **No code-point order.** The JDK's processor always sorts with a `java.text.Collator`,
  in the machine's default locale when `lang` is empty. So `sortLanguage` empty means
  English, never the machine's locale. The dialog offers "English (default)" instead of
  "code points".
- **Default language.** The polyglossia name of `sortLanguage` is the document's default
  language (`\setdefaultlanguage`), English otherwise. This needs no new parameter.
- **`sortLanguage` suggested.** `suggested()` takes the language of most glosses and
  definitions, not the most used meta language: the corpus's header descriptions are in
  English, which made `en` the most used.
- **Texts in their language.** Texts in a mapped language are written in
  `\foreignlanguage{name}{…}`, so that the languages and fonts have an effect. The current
  stylesheets declared no language at all: `set_languages` was commented out.
- **`\glossin{lang}{…}`** replaces `\gloss<lang>{…}`. A code such as `oc-FR-fonipa`
  cannot be in a command name.
- **"See figure" only with figures on.** Its `\ref`s were undefined otherwise.
- **The domains sort on their numbers** ("5 La maison" before "5.1.1 Mobilier").
- **XSLTC quirk.** A global variable that uses the parameters only through a called template
  is initialized before the parameters are set. `defaultPolyglossia` passes them
  explicitly, with a comment.
- **Labels of figures** come from the sense's id, not from the form. Two figures of one
  entry had the same label.
- **The polyglossia table** was generated from the installed polyglossia v2.9:
  - 83 languages, 41 alias names, and the BCP 47 codes of each language;
  - a three-letter code is matched through `Locale`.
- **`LiftDictionary.getSourceFile()`** is new, for the `href`s of a sample. The sample is
  written by `LiftWriterSession`, which now also writes to a stream and can filter
  entries; this stays internal.
- **The test data:** `latex-sample.lift` has 11 entries: the 10 of the plan, and `͏ʃəbri`.
  The special characters went into a definition and the relations onto a sense: the
  stylesheets write neither notes nor entry relations other than components.

**Known limits:**
- **Sans-serif text.** The sans-serif headings and page headers stay in Latin Modern
  Sans, which has no `ʃ`, even with a main font. A `sansFont` parameter would fix it.
- **Load warnings.** The command line prints lift-api's load warnings (`java.util.logging`)
  on standard error, as `info` already did.

---
---

# Addendum (2026-10-23): a custom stylesheet that overrides the bundled ones

Status: **implemented** (2026-10-23), with the questions of A.6 decided as recommended.
See A.7.

## A.1 Goal

Users can change the output without editing the bundled stylesheets. A new setting names
an XSLT file, given on the command line, in the dialog, or in the JSON settings. That
stylesheet is loaded after the bundled ones, and what it defines replaces their
definitions:
- the LaTeX macros that style an entry (`latex-styling-command`);
- the packages and the page layout (`latex-package`);
- how a part of speech is abbreviated (`abbreviate_pos`);
- how a gloss or a sense is written (the templates matching `gloss`, `sense`);
- the defaults of the parameters, and so on.

## A.2 The mechanism: an in-memory wrapper

XSLT already has the rule: of two imported stylesheets, the one imported *later* has the
higher import precedence, and its templates, parameters and variables win. So the export
compiles a small wrapper, built in memory:

```xml
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:import href="(URL of the bundled latex/lift2latex.xsl)"/>
  <xsl:import href="(URL of the user's file)"/>
</xsl:stylesheet>
```

The user's file is a plain stylesheet holding only what it changes. It does not import
the bundled files, and does not need to know where they are (inside a jar or a jlink
image). Its own `xsl:import`s and `xsl:include`s resolve against its own location, so it
can be split into several files.

**Checked with the JDK's processor (2026-10-23), on `tiny.xml`:**
- a named template (`abbreviate_pos`) and a match template (`gloss`) of the user's file
  replace the bundled ones;
- they can call the bundled templates (`escape_for_latex`): all templates are in one
  stylesheet;
- a parameter declared in the user's file with another default (`title`) replaces the
  bundled default, and a value given from outside (Java, the command line, the JSON) still
  wins over it.

**What overriding cannot do, by XSLT's rules** (to document):
- **`xsl:apply-imports` reaches nothing bundled.** From the user's file, it only sees the
  stylesheets that file imports itself, not its sibling in the wrapper. So "do what the
  bundled template does, then add something" means copying the bundled template, or
  calling a named template.
  - A `lift2latex:` URI scheme would allow it; it is left for later (A.6, decision 2).
- **Keys are merged, not replaced.** An `xsl:key name="letter"` in the user's file adds to
  the bundled key, which would then index entries twice.
  - The grouping key is literal text (§1 step 2).
  - Changing `foldFrom` / `foldTo` in the user's file changes the headings, but neither
    the grouping nor the Java `headingLetter` used by the preview sample.
  - Documented as not supported: the letters are not overridable.
- **`xsl:output`:** the user's declaration merges with the bundled one. `method="xml"`
  would break the output. Documented.

## A.3 The setting

| Where | Name | Value |
|---|---|---|
| `LatexExportOptions` | `customStylesheet` (`Path`) | not a stylesheet parameter; null = none |
| JSON | `"customStylesheet"` | a path, relative to the JSON file, like `urlillustration`; written relative to it when inside its folder |
| Command line | `--stylesheet <file.xsl>` | overrides the JSON value |
| Dialog | "Custom stylesheet" | a field, with "Browse…" (a `FileChooser` for `*.xsl`, `*.xslt`) and "Clear" |

`merge`, `writeJson` and `fromJson` handle it like `illustrationDirectory`. The unknown-key
check of `fromJson` learns the new key.

## A.4 Work plan

### Step A1: lift-api, compiling with a custom stylesheet

- **`LatexExport`:**
  - With `customStylesheet == null`: the cached `Templates`, as now.
  - Otherwise:
    - build the wrapper above. The bundled URL is
      `LatexExport.class.getResource(STYLESHEET)`, and the user's is `path.toUri()`;
    - compile the wrapper with a system id, so that the processor's messages name the
      files;
    - compile it at each export, with no cache (A.6, decision 1).
- **Errors.** An `ErrorListener` collects the processor's messages with their location.
  - A compilation error becomes a `LatexExportException` such as
    "custom.xsl, line 12: … (the stylesheet does not compile)".
  - Errors at run time (`xsl:message terminate="yes"`, a wrong call) are reported the same
    way, with their file and line.
  - Messages from `xsl:message` without `terminate` are collected, and returned to the
    caller, to show as information (the stylesheet's author may use them).
- **A missing file** is an error before anything is written.
- **Security.**
  - The user's stylesheet is code: XSLTC can call Java through extension functions
    (`xmlns:java=…`).
  - For the command line and the editor, it is the user's own file on the user's own
    machine, so nothing more is needed. The secure-processing feature stays as the JDK
    sets it.
  - A future web service must not accept an uploaded stylesheet without
    `FEATURE_SECURE_PROCESSING`, with extension functions disabled and `document()`
    limited. Say so in the Javadoc of `customStylesheet`.

### Step A2: lift-api, what a user needs to write one

Overriding a template means knowing its name and parameters. Three aids:

- **The extension points, documented and kept stable.** A comment block at the top of
  `lift2latex.xsl` lists what may be overridden, with each template's parameters and what
  it writes. The names below are then part of the public interface: renaming one is a
  breaking change, noted in the README.

  | Template | What it writes |
  |---|---|
  | `latex-preamble`, `latex-package`, `latex-styling-command` | the document class, the packages, the macros that style the entries (`\entry`, `\latexsensestart`, `\glossfr`…) |
  | `latex-startdocument`, `latex-enddocument`, `latex-startsection`, `latex-endsection` | the title page, the table of contents, the sections of letters and domains |
  | `match="entry"`, `match="sense"`, `match="gloss"` | an entry, a sense, a gloss |
  | `make_headword`, `make_headword_string`, `make_illustration` | a headword, a figure |
  | `abbreviate_pos`, `abbreviate_trait`, `abbreviate_slot` | the abbreviations of grammatical categories |
  | the `xsl:param`s of `parameters.xsl` | their defaults |

  Everything else is internal and may change: `escape_for_latex`, `label`, `letter`,
  `in-language`, the keys, the folding strings. Most often, a user only needs to redefine
  a LaTeX macro of `latex-styling-command`, or an abbreviation.
- **A starting file.** `LatexExport.writeStylesheetTemplate(Path)` writes a stylesheet that
  changes nothing: a header explaining the mechanism, and each extension point as a
  commented-out copy of its bundled definition, ready to uncomment and edit.
  - The command line exposes it as `lift-api latex --write-stylesheet custom.xsl`; the
    dialog's button is in step A4.
  - It is generated from the bundled files, so that it never lags behind them: it copies
    the named templates of the table above.
  - The copy is commented out, so the file is valid and has no effect until edited.
- **The bundled files themselves.** `LatexExport.copyStylesheets(Path dir)` copies
  `lift2latex.xsl`, `latex.xsl` and `parameters.xsl`, to read them or to run them with
  `xsltproc`. The command line has `--copy-stylesheets <dir>`.

### Step A3: lift-api, command line and checks

- **New options:**
  - `--stylesheet <file.xsl>`;
  - `--write-stylesheet <file.xsl>` and `--copy-stylesheets <dir>`, which, like
    `--write-config`, stop without exporting.
- **The checks (`LatexExport.check`)** gain two kinds:
  - `CUSTOM_STYLESHEET_MISSING`: the file does not exist;
  - `CUSTOM_STYLESHEET_INVALID`: it does not compile, with the first error and its line.
    Compiling it is quick, so the check does it; it still does not run it.

  With `--strict`, they stop the export. Without it, a stylesheet that does not compile
  fails the export anyway, with exit code 1.
- **README:** a section "Customising the output":
  - the mechanism and its limits (A.2);
  - the table of extension points;
  - two examples: redefining `\glossfr` in `latex-styling-command`, and adding `Nom → n.`
    to `abbreviate_pos`.

### Step A4: DictionaryEditor, the dialog

- **A row "Custom stylesheet":** the field, "Browse…" and "Clear", and a
  "Create a starting stylesheet…" button. It writes `writeStylesheetTemplate` to a file
  the user chooses (next to the dictionary by default), puts it in the field, and offers
  to open it (`HostServices.showDocument`, the system's editor for `.xsl`).
- **The preview uses it too:** the usual loop is to edit the stylesheet, click "Preview",
  and look.
- **The warnings list** shows the two new kinds. `CUSTOM_STYLESHEET_INVALID` expands to the
  processor's message, with its line.
- **Checking again on change:** the stylesheet file can change outside the dialog. The
  check runs again when the dialog regains focus, as well as when the field changes.
- **Memory:** the path is in the remembered settings and in the saved JSON, relative to
  the dictionary's folder when it is inside it.

### Step A5: tests

**lift-api, `LatexCustomStylesheetTest`:**
- overriding:
  - a named template (`abbreviate_pos`: `Nom` → `n.`) changes only the parts of speech:
    the rest of the output equals the expected output with only those lines changed;
  - a match template (`gloss`) and a LaTeX macro (`latex-styling-command`);
- a parameter's default from the custom file is used, and a value given from outside
  still wins over it;
- the custom file's own `xsl:import` of a second file, by a relative path;
- errors:
  - a custom file that does not compile: `LatexExportException` naming the file and the
    line, and an existing `.tex` left as it was;
  - a missing file: an error before writing;
  - `xsl:message terminate="yes"`: an error with its text;
- **the starting file:**
  - with it, the output is exactly the expected output (it is a no-op);
  - once its `abbreviate_pos` block is uncommented, it compiles;
- `copyStylesheets` then `xsltproc` (when installed) on the copies gives the same output
  as the bundled export;
- JSON:
  - `customStylesheet` relative to the JSON file;
  - the round trip through `writeJson` / `fromJson`;
  - an unknown-key typo (`customStyleSheet`) suggests the right key;
- the checks `CUSTOM_STYLESHEET_MISSING` and `CUSTOM_STYLESHEET_INVALID`.

**lift-api, `MainTest`:**
- `--stylesheet` overrides the JSON value;
- `--write-stylesheet` and `--copy-stylesheets` write their files and export nothing;
- an invalid stylesheet exits with 1 and prints the file and line.

**DictionaryEditor, `LatexExportDialogTest`:**
- a stylesheet chosen through the (replaced) chooser is used by "Export", is saved in the
  settings, and comes back on reopening;
- "Create a starting stylesheet…" writes a file that compiles and fills the field;
- an invalid stylesheet shows `CUSTOM_STYLESHEET_INVALID` with its line.

## A.5 Order

1. Steps A1 and A3 (mechanism, errors, option, checks), with their tests.
2. Step A2: the extension-point comment, the starting file, the copy; the README.
3. A lift-api release, then step A4 in DictionaryEditor.

## A.6 Decided (2026-10-23)

1. **No cache:** the custom stylesheet is compiled at each export, about 100 ms with the
   bundled files.
2. **No `lift2latex:` URI scheme for now.** Overriding, and calling named templates, cover
   the common cases. The scheme can be added later without changing the setting.
3. **One custom stylesheet.** It can import or include other files itself.

## A.7 Implementation status (2026-10-23)

**Done:** steps A1–A5, uncommitted. Tests pass:
- lift-api: 313 (11 for the custom stylesheet, 3 for the command line);
- DictionaryEditor: 138 (3 for the dialog).

From inside DictionaryEditor's jlink image, a custom stylesheet replaces a parameter
default: the wrapper imports the bundled stylesheet by its `jrt:` URL.

**Details:**
- **The starting file** (`writeStylesheetTemplate`) copies each extension point, and each
  `xsl:param`, from the bundled files, as a top-level element in another namespace
  (`disabled:template`, `disabled:param`).
  - XSLT ignores such elements, so the file is valid and changes nothing.
  - A copy is enabled by renaming `disabled:template` to `xsl:template`.
  - XML comments were not used: the copies hold comments and `--`, which a comment
    cannot contain.
- **XSLTC gives no locator for compilation errors.** The file and the line are taken from
  the message text ("file:/…/x.xsl: line 3: …") and shown as "x.xsl, line 3: …".
- **`xsl:message`s** come as warnings, and are returned by `export` (now
  `List<String>`). The command line prints them as `stylesheet: …`, and the dialog shows
  them at the end. `terminate="yes"` becomes "the stylesheet stopped: <its text>".
- **The dialog checks again** when its window regains focus, since the stylesheet is
  edited in another application.

