package fr.cnrs.lacito.liftapi.cli;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.export.EntrySelection;
import fr.cnrs.lacito.liftapi.export.LatexCompilation;
import fr.cnrs.lacito.liftapi.export.LatexEngine;
import fr.cnrs.lacito.liftapi.export.LatexEngineKind;
import fr.cnrs.lacito.liftapi.export.LatexEngineRequest;
import fr.cnrs.lacito.liftapi.export.LatexExport;
import fr.cnrs.lacito.liftapi.export.LatexExportException;
import fr.cnrs.lacito.liftapi.export.LatexExportOptions;
import fr.cnrs.lacito.liftapi.export.LatexExportWarning;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code lift-api latex <file>}: the LaTeX export, and its compilation to PDF.
 *
 * <p>The settings come from the defaults (or {@code --suggest}), then the JSON file of
 * {@code --config}, then the options: each overrides the one before.
 */
@Command(name = "latex",
    description = {
        "Export a LIFT dictionary to LaTeX, and compile it to PDF with XeLaTeX or LuaLaTeX.",
        "Settings: the defaults (or --suggest), then the JSON file of --config, then the options below; "
            + "each overrides the one before.",
        "Exit codes: 0 done; 1 the export or the compilation failed; 2 bad command line or settings; "
            + "3 warnings with --strict."},
    footer = {
        "",
        "Examples:",
        "  lift-api latex dict.lift --suggest --write-config dict.json      (a starting settings file)",
        "  lift-api latex dict.lift --config dict.json --sample 50 --open -o preview.tex",
        "  lift-api latex dict.lift --config dict.json --latex-engine lualatex --pdf -o dict.tex",
        "  lift-api latex --list-engines",
        "  lift-api latex --write-stylesheet custom.xsl                      (then edit it, and add --stylesheet custom.xsl)"})
final class LatexCommand implements Callable<Integer> {

    /** The environment variable giving the path of the engine, when no option does. */
    static final String ENGINE_VARIABLE = "LIFT_API_LATEX";

    static final int EXIT_FAILED = 1;
    static final int EXIT_USAGE = 2;
    static final int EXIT_WARNINGS = 3;

    @Spec
    private CommandSpec spec;

    @Parameters(index = "0", arity = "0..1", description = "Path to a LIFT dictionary")
    private File dictionaryFile;

    @Option(names = {"-o", "--output"}, description = "The .tex to write; standard output when absent (then no PDF).")
    private Path output;

    @Option(names = "--title", description = "title")
    private String title;

    @Option(names = "--author", description = "author")
    private String author;

    @Option(names = "--thematic-dictionary", negatable = true, description = "makeThematicDictionary: a part by semantic domain (default: yes).")
    private Boolean thematic;

    @Option(names = "--list-of-figures", negatable = true, description = "makeListOfFigure (default: no).")
    private Boolean listOfFigures;

    @Option(names = "--figures", negatable = true, description = "makeFigure: the illustrations, as figures (default: no).")
    private Boolean figures;

    @Option(names = "--illustrations", paramLabel = "<dir>", description = "urlillustration: the directory of the illustration files.")
    private Path illustrations;

    @Option(names = "--language", paramLabel = "<code>=<polyglossia>", description = "languages: the polyglossia name of a language code; repeatable.")
    private Map<String, String> languages;

    @Option(names = "--sort-language", paramLabel = "<code>", description = "sortLanguage: the language whose alphabetical order sorts the entries.")
    private String sortLanguage;

    @Option(names = "--main-font", paramLabel = "<font>", description = "mainFont")
    private String mainFont;

    @Option(names = "--font", paramLabel = "<polyglossia>=<font>", description = "fonts: a font for a polyglossia language; repeatable.")
    private Map<String, String> fonts;

    @Option(names = "--stylesheet", paramLabel = "<file.xsl>",
        description = "customStylesheet: an XSLT file loaded after the bundled stylesheets, whose definitions replace theirs.")
    private Path stylesheet;

    @Option(names = "--config", paramLabel = "<file.json>", description = "The settings, from a JSON file with the keys above.")
    private Path config;

    @Option(names = "--suggest", description = "Start from settings suggested from the dictionary (languages, sort language, pictures/ folder).")
    private boolean suggest;

    @Option(names = "--sample", paramLabel = "<n>", description = "Export about n entries spread over the letters, with the entries they refer to.")
    private Integer sample;

    @Option(names = "--pdf", description = "Also compile the .tex to PDF.")
    private boolean pdf;

    @Option(names = "--latex-engine", paramLabel = "<auto|xelatex|lualatex|path>",
        description = "The engine (implies --pdf): auto (XeLaTeX, else LuaLaTeX), a kind, or a program. "
            + "latexEngine in the settings; else the variable " + ENGINE_VARIABLE + " may give a program.")
    private String latexEngine;

    @Option(names = "--latex-engine-kind", paramLabel = "<xelatex|lualatex>",
        description = "The kind of the program given by path, when its name does not tell.")
    private String latexEngineKind;

    @Option(names = "--open", description = "Open the PDF in the default viewer at the end (implies --pdf).")
    private boolean open;

    @Option(names = "--timeout", paramLabel = "<seconds>", defaultValue = "600", description = "The time allowed for each LaTeX run (default: 600).")
    private long timeoutSeconds;

    @Option(names = "--strict", description = "Treat the warnings as errors: nothing is written when there is one.")
    private boolean strict;

    @Option(names = "--write-config", paramLabel = "<file.json>", description = "Write the resulting settings to a JSON file, and stop: no export.")
    private Path writeConfig;

    @Option(names = "--list-engines", description = "List the engines found, with kind, path and version, and stop.")
    private boolean listEngines;

    @Option(names = "--write-stylesheet", paramLabel = "<file.xsl>",
        description = "Write a starting custom stylesheet (a disabled copy of each definition that may be replaced), and stop.")
    private Path writeStylesheet;

    @Option(names = "--copy-stylesheets", paramLabel = "<dir>", description = "Copy the bundled stylesheets into a directory, and stop.")
    private Path copyStylesheets;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help.")
    private boolean help;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();
        try {
            if (listEngines) {
                List<LatexEngine> all = LatexEngine.findAll();
                if (all.isEmpty()) out.println("No engine found (neither XeLaTeX nor LuaLaTeX).");
                for (LatexEngine e : all) out.println(e.kind().command() + "\t" + e.path() + "\t" + e.version());
                out.flush();
                return 0;
            }
            if (writeStylesheet != null || copyStylesheets != null) {
                if (writeStylesheet != null) {
                    LatexExport.writeStylesheetTemplate(writeStylesheet);
                    out.println("Wrote " + writeStylesheet);
                }
                if (copyStylesheets != null) {
                    for (Path p : LatexExport.copyStylesheets(copyStylesheets)) out.println("Wrote " + p);
                }
                out.flush();
                return 0;
            }
            if (dictionaryFile == null) {
                throw new CommandLine.ParameterException(spec.commandLine(), "Missing the LIFT dictionary to export.");
            }
            LiftDictionary dictionary = Main.load(spec, dictionaryFile);
            LatexExportOptions options = options(dictionary);

            if (writeConfig != null) {
                options.writeJson(writeConfig);
                out.println("Wrote " + writeConfig);
                out.flush();
                return 0;
            }

            boolean compile = pdf || open || latexEngine != null;
            if (compile && output == null) {
                throw new CommandLine.ParameterException(spec.commandLine(), "--pdf, --open and --latex-engine need -o <file.tex>.");
            }
            List<LatexExportWarning> warnings = LatexExport.check(dictionary, options, null);
            for (LatexExportWarning w : warnings) {
                err.println("warning: " + w.message() + (w.examples().isEmpty() ? "" : " (" + String.join(", ", w.examples())
                    + (w.count() > w.examples().size() ? ", ..." : "") + ")"));
            }
            err.flush();
            if (strict && !warnings.isEmpty()) return EXIT_WARNINGS;

            // Found before writing, so that a missing engine does not leave a .tex behind for nothing.
            LatexEngine engine = compile ? LatexEngine.require(engineRequest(options)) : null;

            EntrySelection selection = sample == null ? EntrySelection.all() : EntrySelection.sample(sample);
            if (output == null) {
                printMessages(err, LatexExport.export(dictionary, out, options, selection));
                out.flush();
                return 0;
            }
            printMessages(err, LatexExport.export(dictionary, output, options, selection));
            out.println("Wrote " + output);
            if (!compile) {
                out.flush();
                return 0;
            }
            LatexCompilation result = LatexExport.compile(output, engine, Duration.ofSeconds(timeoutSeconds));
            if (!result.success()) {
                err.println(result.engine().kind().displayName() + " failed:");
                err.println(result.firstError().orElse(""));
                err.println("See the log: " + result.log());
                err.flush();
                return EXIT_FAILED;
            }
            out.println("Wrote " + result.pdf() + " (" + engine.kind().displayName() + ", " + result.runs() + " runs)");
            out.flush();
            if (open) LatexExport.openWithDefaultViewer(result.pdf());
            return 0;
        } catch (LatexExportException e) {
            err.println(spec.commandLine().getColorScheme().errorText(e.getMessage()));
            err.flush();
            return config != null && e.getMessage() != null && e.getMessage().startsWith(config.toString())
                ? EXIT_USAGE : EXIT_FAILED;
        } catch (IOException e) {
            err.println(spec.commandLine().getColorScheme().errorText(e.getMessage()));
            err.flush();
            return EXIT_FAILED;
        }
    }

    private static void printMessages(PrintWriter err, List<String> messages) {
        for (String m : messages) err.println("stylesheet: " + m);
        err.flush();
    }

    /** Defaults (or suggested), then the JSON file, then the options. */
    LatexExportOptions options(LiftDictionary dictionary) throws LatexExportException {
        LatexExportOptions base = suggest
            ? LatexExportOptions.suggested(dictionary, dictionaryFile.toPath())
            : LatexExportOptions.defaults();
        if (config != null) base = base.merge(LatexExportOptions.fromJson(config));
        LatexEngineRequest engine = null;
        if (latexEngine != null) {
            engine = LatexEngineRequest.parse(latexEngine);
            if (engine.path().isEmpty()) base = base.withLatexEngine(engine);
        }
        return base.merge(new LatexExportOptions(title, author, thematic, listOfFigures, figures,
            illustrations == null ? null : illustrations.toAbsolutePath(), languages, sortLanguage, mainFont, fonts, null,
            stylesheet == null ? null : stylesheet.toAbsolutePath()));
    }

    /** --latex-engine (with --latex-engine-kind), else LIFT_API_LATEX, else the settings. */
    private LatexEngineRequest engineRequest(LatexExportOptions options) throws LatexExportException {
        LatexEngineKind kind = null;
        if (latexEngineKind != null) {
            kind = LatexEngineKind.parse(latexEngineKind).orElseThrow(() -> new CommandLine.ParameterException(
                spec.commandLine(), "--latex-engine-kind must be xelatex or lualatex, not " + latexEngineKind));
        }
        if (latexEngine != null) {
            LatexEngineRequest r = LatexEngineRequest.parse(latexEngine);
            return r.path().isPresent() ? LatexEngineRequest.at(r.path().get(), kind) : r;
        }
        String variable = System.getenv(ENGINE_VARIABLE);
        if (variable != null && !variable.isBlank()) return LatexEngineRequest.at(Path.of(variable.trim()), kind);
        return options.latexEngineOrDefault();
    }
}
