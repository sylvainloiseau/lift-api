package fr.cnrs.lacito.liftapi.export;

import java.util.List;

/**
 * Something that will go wrong, or look wrong, in a LaTeX export
 * ({@link LatexExport#check}). It does not prevent the export.
 *
 * @param kind what is wrong; an application can show its own message for it
 * @param message an English message
 * @param count how many items it concerns
 * @param examples up to five of them (language codes, file names, entry forms...)
 */
public record LatexExportWarning(Kind kind, String message, int count, List<String> examples) {

    public LatexExportWarning {
        examples = List.copyOf(examples);
    }

    public enum Kind {
        /** A language of the dictionary has no polyglossia name. */
        LANGUAGE_NOT_MAPPED,
        /** A polyglossia name is not one polyglossia knows (a typo, or a newer polyglossia). */
        UNKNOWN_POLYGLOSSIA_NAME,
        /** Figures are on and some illustration files are not in the directory. */
        ILLUSTRATIONS_MISSING,
        /** Figures are on, the dictionary has illustrations, and no directory is set. */
        NO_ILLUSTRATION_DIRECTORY,
        /** The thematic part is on, and no sense has a semantic domain: it is empty. */
        NO_SEMANTIC_DOMAIN,
        /** Entries without a form, which have no letter: left out of the alphabetic part. */
        ENTRY_WITHOUT_FORM,
        /** A font is given for a polyglossia name no language maps to. */
        FONT_WITHOUT_LANGUAGE,
        /** The custom stylesheet does not exist. */
        CUSTOM_STYLESHEET_MISSING,
        /** The custom stylesheet does not compile; the example is the error, with its line. */
        CUSTOM_STYLESHEET_INVALID,
        /** The .tex or the PDF exists, and will be replaced. */
        OUTPUT_EXISTS
    }
}
