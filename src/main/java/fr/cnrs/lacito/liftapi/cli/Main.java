package fr.cnrs.lacito.liftapi.cli;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDocumentLoadingException;
import java.io.File;
import java.io.PrintWriter;
import java.util.Set;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

/**
 * lift-api's command line: {@code lift-api info <file>}, {@code lift-api latex <file> ...}.
 *
 * <p>{@code lift-api <file>} still runs {@code info}, as it did before the subcommands.
 */
@Command(name = "lift-api", mixinStandardHelpOptions = true,
    subcommands = {InfoCommand.class, LatexCommand.class},
    description = "Read LIFT dictionaries. Without a subcommand, lift-api <file> runs info.")
public final class Main implements Runnable {

    @CommandLine.Spec
    private CommandSpec spec;

    private static final Set<String> SUBCOMMANDS = Set.of("info", "latex", "help", "-h", "--help", "-V", "--version");

    public static void main(String[] args) {
        System.exit(execute(args, new PrintWriter(System.out, true), new PrintWriter(System.err, true)));
    }

    /** Runs a command line, and returns its exit code. */
    public static int execute(String[] args, PrintWriter out, PrintWriter err) {
        CommandLine command = new CommandLine(new Main());
        command.setOut(out);
        command.setErr(err);
        // execute() maps a thrown exception and a bad command line to a non-zero exit
        // code. Catching and printing here instead reported every failure as success.
        command.setExecutionExceptionHandler((ex, cmd, parseResult) -> {
            // Report the problem, not a stack trace.
            cmd.getErr().println(cmd.getColorScheme().errorText(ex.getMessage()));
            cmd.getErr().flush();
            return cmd.getCommandSpec().exitCodeOnExecutionException();
        });
        return command.execute(withDefaultSubcommand(args));
    }

    /** {@code lift-api <file>} → {@code lift-api info <file>}. */
    static String[] withDefaultSubcommand(String[] args) {
        if (args.length == 0 || SUBCOMMANDS.contains(args[0])) return args;
        String[] withInfo = new String[args.length + 1];
        withInfo[0] = "info";
        System.arraycopy(args, 0, withInfo, 1, args.length);
        return withInfo;
    }

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    static LiftDictionary load(CommandSpec spec, File file) {
        try {
            return LiftDictionary.loadDictionaryFromFile(file);
        } catch (LiftDocumentLoadingException e) {
            throw new CommandLine.ExecutionException(spec.commandLine(), "Cannot read " + file + ": " + e.getMessage(), e);
        }
    }
}
