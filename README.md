# lift-api

A library for working with dictionaries in LIFT format (<a
href="https://github.com/sillsdev/lift-standard">https://github.com/sillsdev/lift-standard</a>).

The LIFT dictionary format allows to represent complex linguistic structures and
has been elaborated for descriptive linguistics.

# Features

- complete modeling of the LIFT dictionary data structure (with its entry, note, example components, but also the note, field, trait, annotation components, references to media; cross-references, feature sets, etc.)
- load and save LIFT dictionary
- walk the dictionary data structure: hierarchically, or by components types
- maintain a registry of the features set and their features: manipulate feature names, count, etc.
- maintain a registry of all metalanguage or object language forms, number of occurrences of each languages
- maintain a registry of cross-references between components
- offer methods to mutate the dictionary, with two API (a fluent API or a low-level API): create components, update component properties, delete or move components, etc.

# Installation

```
```

# Usage

## Dictionary creation

Reading a file:

```java
import java.io.File;
import fr.cnrs.lacito.liftapi.LiftDictionary;

File file = new File("dictionary.lift");
LiftDictionary lf = LiftDictionary.loadDictionaryFromFile(file);

// ...

lf.save();
```

Building from scratch:

```java
import fr.cnrs.lacito.liftapi.LiftDictionary;

LiftDictionary dictionary = LiftDictionary.makeBuilder()
        .withLiftVersion("0.15")
        .withProducer("Test Producer")
        .build();

```

## Adding entries

```java
import fr.cnrs.lacito.liftapi.LiftDictionary;

        LiftEntry word = dictionary
            .getComponentBuilder()
            .entry()
            .withId("word-001")
            .withForm("en", "run")
            .withForm("fr", "courir")
            .addSense(s ->
                s
                    .withOrder(1)
                    .withGloss("en", "to move quickly on foot")
                    .withDefinition(
                        "en",
                        "To move at a pace faster than walking"
                    )
                    .withPartOfSpeech("verb")
                    .addExample(ex ->
                        ex
                            .withExample("en", "She runs every morning")
                            .addTranslation(
                                "litteral",
                                "fr",
                                "Elle court chaque matin"
                            )
                    )
            )
            .addSense(s ->
                s
                    .withOrder(2)
                    .withGloss("en", "to manage or operate")
                    .withPartOfSpeech("verb")
            )
            .addPronunciation(p -> p.withPronunciation("en", "rʌn"))
            .addNote("source", "en", "From Old English 'irnan'")
            .build();
```

## Listening to changes

Every change of a dictionary is reported as a reversible event: a component added or
removed, a value changed (a text, a date, a type…), a form, a feature, a definition, a
language. Each can be reverted and reapplied, which is what an undo history needs.

```java
import fr.cnrs.lacito.liftapi.change.ModelChange;

        List<ModelChange> changes = new ArrayList<>();
        Subscription s = dictionary.addChangeListener(changes::add);

        word.getSenses().getFirst().getGlosses().formTextProperty("en").set("to sprint");

        // Undo everything, last change first.
        for (int i = changes.size() - 1; i >= 0; i--) {
            changes.get(i).revert();
        }
        s.unsubscribe();
```

Loading a file reports nothing, and costs nothing more. See the javadoc of the
`fr.cnrs.lacito.liftapi.change` package for the rules.

## Transactions

```java
        try (ChangeRecorder rec = dictionary.recordChanges()) {
            importEntries(dictionary, file);  // any changes; an exception skips the commit
            rec.commit();                     // keep them; without commit, all are reverted
        }
```

## CLI

Utility for loading a dictionary:

```
java -jar lift-api/target/lift-api-0.1-SNAPSHOT-jar-with-dependencies.jar <dictionaryFile>
```

Will print error message or quick summary (the same as `lift-api info <dictionaryFile>`).

## LaTeX export

`fr.cnrs.lacito.liftapi.export` turns a dictionary into a LaTeX document, and compiles it to
PDF. The conversion is done by XSLT 1.0 stylesheets (formerly the
[lift2latex](https://github.com/sylvainloiseau/lift2latex) repository), run by the JDK's
own processor: `src/main/resources/fr/cnrs/lacito/liftapi/export/latex/`.

The output needs **XeLaTeX or LuaLaTeX** (it uses `fontspec` and `polyglossia`): TeX Live,
MacTeX (macOS) or MiKTeX (Windows).

From Java:

```java
LatexExportOptions options = LatexExportOptions.suggested(dictionary, file.toPath())
    .withTitle("Lexicon")
    .withMainFont("Charis SIL");
for (LatexExportWarning w : LatexExport.check(dictionary, options, tex)) {
    System.err.println(w.message());
}
LatexExport.export(dictionary, tex, options, EntrySelection.all());
LatexCompilation pdf = LatexExport.compile(tex,
    LatexEngine.require(options.latexEngineOrDefault()), Duration.ofMinutes(10));
```

From the command line:

```
lift-api latex dict.lift --suggest --write-config dict.json      # a starting settings file
lift-api latex dict.lift --config dict.json --sample 50 --open -o preview.tex
lift-api latex dict.lift --config dict.json --latex-engine lualatex --pdf -o dict.tex
lift-api latex --list-engines
```

`lift-api latex --help` lists every option. The settings come from the defaults (or
`--suggest`, which proposes them from the dictionary), then the JSON file of `--config`,
then the options; each overrides the one before. The JSON file uses the names of the
stylesheet parameters; every key is optional:

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

| Key | Meaning |
|---|---|
| `title`, `author` | the title page |
| `makeThematicDictionary` | a second part, with the entries grouped by semantic domain (`semantic-domain-ddp4`); default true |
| `makeListOfFigure` | a list of figures at the end; default false |
| `makeFigure` | the illustrations, as figures; default false |
| `urlillustration` | the directory of the illustration files, relative to the JSON file |
| `languages` | the [polyglossia](https://ctan.org/pkg/polyglossia) name of each language code: texts in that language are written in it |
| `sortLanguage` | the language whose alphabetical order sorts the entries; its polyglossia name is the document's default language |
| `mainFont` | the main font; the LaTeX default (Latin Modern) has no IPA characters |
| `fonts` | a font for some polyglossia languages |
| `latexEngine` | `auto` (XeLaTeX, else LuaLaTeX), `xelatex` or `lualatex`; a program's path is given by `--latex-engine <path>` or the variable `LIFT_API_LATEX` |

Entries are listed under their first letter, without case or accent, after a leading `-`
(affixes) and U+034F; digraphs (`ng`, `ch`) are not letters of their own.

### Customising the output

A custom stylesheet changes the output without editing the bundled ones: `--stylesheet
custom.xsl`, `"customStylesheet": "custom.xsl"` in the JSON settings (relative to the JSON
file), or the "Custom stylesheet" field of DictionaryEditor's export dialog.

It is loaded after the bundled stylesheets (the export compiles a wrapper that imports the
bundled `lift2latex.xsl`, then the custom file), so what it defines replaces their
definitions. It holds only what it changes, can call any bundled template, and can import
other files by relative paths. For example, to abbreviate one more part of speech:

```xml
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template name="abbreviate_pos">
    <xsl:param name="pos"/>
    <xsl:choose>
      <xsl:when test="$pos = 'Nom'">n.</xsl:when>
      <xsl:when test="$pos = 'Verbe'">v.</xsl:when>
      <xsl:otherwise><xsl:value-of select="$pos"/></xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
```

`lift-api latex --write-stylesheet custom.xsl` writes a starting file: a disabled copy of
each definition that may be replaced (rename `disabled:template` to `xsl:template` to use
one). `lift-api latex --copy-stylesheets <dir>` copies the bundled files, to read them.

What may be replaced is kept stable (renaming it is a breaking change):

| Template | What it writes |
|---|---|
| `latex-preamble`, `latex-package` | the document class, the packages, the layout |
| `latex-styling-command` | the macros that style the entries (`\entry`, `\latexsensestart`, `\glossfr`, `\definition`...): the usual place to change the look |
| `latex-startdocument`, `latex-enddocument`, `latex-startsection`, `latex-endsection` | the title page, the contents, the sections of letters and domains |
| `match="entry"`, `match="sense"`, `match="gloss"` | an entry, a sense, a gloss |
| `make_headword`, `make_headword_string`, `make_illustration` | a headword, the figures of a sense |
| `abbreviate_pos`, `abbreviate_trait`, `abbreviate_slot` | the abbreviations of grammatical categories |
| the `xsl:param`s of `parameters.xsl` | their defaults (a value given from outside still wins) |

Limits, by XSLT's rules: `xsl:apply-imports` in the custom file does not reach the bundled
templates (copy a template to extend it); the letters (keys and folding strings) cannot be
replaced; a different `xsl:output` would break the output. A stylesheet that does not
compile is reported with its file and line; `xsl:message`s are printed as `stylesheet: …`.
A custom stylesheet is code (XSLT can call Java): a service accepting stylesheets from
others must compile them with secure processing, without extension functions.

The stylesheets also run on their own, with any XSLT 1.0 processor:

```
xsltproc --stringparam title "Lexicon" --stringparam languages "fr=french;oc=occitan" \
         --stringparam sortLanguage fr lift2latex.xsl dict.lift > dict.tex
```

# Running tests

All tests:

```bash
mvn test -pl lift-api
```

For a specific test:

```bash
mvn test -pl lift-api -Dtest=MultiTextTest#testTextAndSeveralSpan
```

## Using it as a dependency

Released versions are on Maven Central: no repository to declare, no token.

```xml
<dependency>
  <groupId>io.github.sylvainloiseau</groupId>
  <artifactId>lift-api</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Releasing

Pushing a tag `vX.Y.Z` runs `.github/workflows/release.yml`, which tests, signs and
publishes version `X.Y.Z` to Maven Central (the secrets it needs are listed at its top).

## Installing into the local Maven repository

Since this is a multi-module Maven project, install from the repository root:

```bash
mvn install
```

This builds and installs both `lift-api` and `dictionary-editor-fx` into your local Maven repository.
