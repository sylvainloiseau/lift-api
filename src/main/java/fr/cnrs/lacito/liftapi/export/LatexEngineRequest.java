package fr.cnrs.lacito.liftapi.export;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Which engine to compile with: {@link #auto()} (XeLaTeX, else LuaLaTeX), a kind, or a
 * program given by its path.
 *
 * <p>Only the first two can be saved with the settings ({@link #settingsValue()}): a path
 * belongs to the machine, not to the document.
 */
public final class LatexEngineRequest {

    private static final LatexEngineRequest AUTO = new LatexEngineRequest(null, null);

    private final LatexEngineKind kind;
    private final Path path;

    private LatexEngineRequest(LatexEngineKind kind, Path path) {
        this.kind = kind;
        this.path = path;
    }

    /** XeLaTeX when it is found, else LuaLaTeX. */
    public static LatexEngineRequest auto() {
        return AUTO;
    }

    /** That kind only. */
    public static LatexEngineRequest of(LatexEngineKind kind) {
        return new LatexEngineRequest(Objects.requireNonNull(kind), null);
    }

    /**
     * That program. Its kind is {@code kind}, or guessed from its name when {@code kind} is
     * null.
     */
    public static LatexEngineRequest at(Path program, LatexEngineKind kind) {
        return new LatexEngineRequest(kind, Objects.requireNonNull(program));
    }

    /**
     * {@code auto}, {@code xelatex}, {@code lualatex} (in any case), or else the path of a
     * program.
     */
    public static LatexEngineRequest parse(String value) {
        if (value == null || value.isBlank() || value.trim().equalsIgnoreCase("auto")) return AUTO;
        return LatexEngineKind.parse(value).map(LatexEngineRequest::of)
            .orElseGet(() -> at(Path.of(value.trim()), null));
    }

    public boolean isAuto() {
        return kind == null && path == null;
    }

    /** The kind asked for, or given with a path. */
    public Optional<LatexEngineKind> kind() {
        return Optional.ofNullable(kind);
    }

    /** The program, when one was given. */
    public Optional<Path> path() {
        return Optional.ofNullable(path);
    }

    /** {@code auto}, {@code xelatex} or {@code lualatex}; empty for a path, which is not saved. */
    public Optional<String> settingsValue() {
        if (path != null) return Optional.empty();
        return Optional.of(kind == null ? "auto" : kind.command());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LatexEngineRequest r && r.kind == kind && Objects.equals(r.path, path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, path);
    }

    @Override
    public String toString() {
        return path != null ? path.toString() : settingsValue().orElseThrow();
    }
}
