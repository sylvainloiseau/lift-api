package fr.cnrs.lacito.liftapi.export;

import java.util.Locale;
import java.util.Optional;

/** The TeX engines that compile the export: both support fontspec and polyglossia. */
public enum LatexEngineKind {
    XELATEX("xelatex", "XeLaTeX"),
    LUALATEX("lualatex", "LuaLaTeX");

    private final String command;
    private final String displayName;

    LatexEngineKind(String command, String displayName) {
        this.command = command;
        this.displayName = displayName;
    }

    /** The program's name, without extension: {@code xelatex}, {@code lualatex}. */
    public String command() {
        return command;
    }

    /** The name to show: {@code XeLaTeX}, {@code LuaLaTeX}. */
    public String displayName() {
        return displayName;
    }

    /** The kind named {@code xelatex} or {@code lualatex}, in any case. */
    public static Optional<LatexEngineKind> parse(String name) {
        if (name == null) return Optional.empty();
        String n = name.trim().toLowerCase(Locale.ROOT);
        for (LatexEngineKind k : values()) {
            if (k.command.equals(n)) return Optional.of(k);
        }
        return Optional.empty();
    }

    /** The kind of a program, guessed from its file name ({@code xelatex.exe}...). */
    static Optional<LatexEngineKind> guess(String fileName) {
        String n = fileName.toLowerCase(Locale.ROOT);
        for (LatexEngineKind k : values()) {
            if (n.startsWith(k.command)) return Optional.of(k);
        }
        return Optional.empty();
    }
}
