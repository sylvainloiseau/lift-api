package fr.cnrs.lacito.liftapi.export;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A TeX engine installed on this machine.
 *
 * <p>They are looked for on the {@code PATH}, then where TeX distributions install: an
 * application started from the macOS Finder does not get the shell's {@code PATH}.
 *
 * @param kind XeLaTeX or LuaLaTeX
 * @param path the program
 * @param version the first line of {@code --version}, or empty when it did not answer
 */
public record LatexEngine(LatexEngineKind kind, Path path, String version) {

    /** The engine to use for a request, or empty when none is found. For {@code auto}, XeLaTeX first. */
    public static Optional<LatexEngine> find(LatexEngineRequest request) {
        return find(request, searchDirectories());
    }

    /**
     * The engine to use for a request. When the requested one is not found, the message
     * says what was found instead: the user asked for it, so there is no silent switch.
     */
    public static LatexEngine require(LatexEngineRequest request) throws LatexExportException {
        return require(request, searchDirectories());
    }

    /** Every engine found, XeLaTeX first, each kind once (the first found on the search path). */
    public static List<LatexEngine> findAll() {
        return findAll(searchDirectories());
    }

    /** The program at {@code path}, of {@code kind}, or of the kind its name tells when {@code kind} is null. */
    public static LatexEngine at(Path path, LatexEngineKind kind) throws LatexExportException {
        if (!Files.isRegularFile(path) || !Files.isExecutable(path)) {
            throw new LatexExportException("No LaTeX program at " + path + ".");
        }
        LatexEngineKind k = kind != null ? kind : LatexEngineKind.guess(path.getFileName().toString())
            .orElseThrow(() -> new LatexExportException(
                "Cannot tell from its name whether " + path + " is XeLaTeX or LuaLaTeX: give its kind."));
        return new LatexEngine(k, path, versionOf(path));
    }

    /** "XeLaTeX (XeTeX 3.141592653-2.6-0.999996 (TeX Live 2026)), /Library/TeX/texbin/xelatex". */
    public String describe() {
        return kind.displayName() + (version.isEmpty() ? "" : " (" + version + ")") + ", " + path;
    }

    static Optional<LatexEngine> find(LatexEngineRequest request, List<Path> directories) {
        try {
            return Optional.of(require(request, directories));
        } catch (LatexExportException e) {
            return Optional.empty();
        }
    }

    static LatexEngine require(LatexEngineRequest request, List<Path> directories) throws LatexExportException {
        if (request.path().isPresent()) {
            return at(request.path().get(), request.kind().orElse(null));
        }
        List<LatexEngine> found = findAll(directories);
        List<LatexEngineKind> wanted = request.kind().map(List::of)
            .orElse(List.of(LatexEngineKind.XELATEX, LatexEngineKind.LUALATEX));
        for (LatexEngineKind k : wanted) {
            for (LatexEngine e : found) {
                if (e.kind() == k) return e;
            }
        }
        if (found.isEmpty()) {
            throw new LatexExportException(
                "Neither XeLaTeX nor LuaLaTeX was found. Install a TeX distribution (TeX Live, MacTeX, MiKTeX), "
                + "or give the program's path.");
        }
        LatexEngine other = found.get(0);
        throw new LatexExportException(wanted.get(0).displayName() + " not found; "
            + other.kind().displayName() + " is installed at " + other.path() + ".");
    }

    static List<LatexEngine> findAll(List<Path> directories) {
        List<LatexEngine> result = new ArrayList<>();
        for (LatexEngineKind k : LatexEngineKind.values()) {
            for (Path dir : directories) {
                Optional<Path> program = program(dir, k);
                if (program.isPresent()) {
                    result.add(new LatexEngine(k, program.get(), versionOf(program.get())));
                    break;
                }
            }
        }
        return result;
    }

    private static Optional<Path> program(Path dir, LatexEngineKind kind) {
        for (String name : List.of(kind.command(), kind.command() + ".exe")) {
            Path p = dir.resolve(name);
            if (Files.isRegularFile(p) && Files.isExecutable(p)) return Optional.of(p);
        }
        return Optional.empty();
    }

    /** The {@code PATH}, then the usual installation directories, most recent TeX Live first. */
    static List<Path> searchDirectories() {
        Set<Path> dirs = new LinkedHashSet<>();
        String path = System.getenv("PATH");
        if (path != null) {
            for (String d : path.split(File.pathSeparator)) {
                if (!d.isBlank()) dirs.add(Path.of(d));
            }
        }
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
        if (windows) {
            String local = System.getenv("LOCALAPPDATA");
            if (local != null) dirs.add(Path.of(local, "Programs", "MiKTeX", "miktex", "bin", "x64"));
            String programs = System.getenv("ProgramFiles");
            if (programs != null) dirs.add(Path.of(programs, "MiKTeX", "miktex", "bin", "x64"));
            for (Path year : subdirectoriesNewestFirst(Path.of("C:\\texlive"))) {
                dirs.add(year.resolve("bin").resolve("windows"));
            }
        } else {
            dirs.add(Path.of("/Library/TeX/texbin"));
            for (Path year : subdirectoriesNewestFirst(Path.of("/usr/local/texlive"))) {
                dirs.addAll(subdirectoriesNewestFirst(year.resolve("bin")));
            }
            dirs.add(Path.of("/usr/bin"));
            dirs.add(Path.of("/usr/local/bin"));
            dirs.add(Path.of("/opt/homebrew/bin"));
        }
        return List.copyOf(dirs);
    }

    private static List<Path> subdirectoriesNewestFirst(Path dir) {
        List<Path> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) return result;
        try (DirectoryStream<Path> s = Files.newDirectoryStream(dir, Files::isDirectory)) {
            s.forEach(result::add);
        } catch (IOException e) {
            return result;
        }
        result.sort(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed());
        return result;
    }

    /** The first line of {@code program --version}, or "" when it does not answer within 10 s. */
    static String versionOf(Path program) {
        try {
            Process p = new ProcessBuilder(program.toString(), "--version").redirectErrorStream(true).start();
            String first;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                first = r.readLine();
            }
            if (!p.waitFor(10, TimeUnit.SECONDS)) p.destroyForcibly();
            return first == null ? "" : first.trim();
        } catch (IOException e) {
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }
}
