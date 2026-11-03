package fr.cnrs.lacito.liftapi;

import java.nio.file.Path;
import java.util.Optional;

/// Something the reader noticed while loading a file: content it skipped, content it keeps
/// but will not save, a recoverable error of the XML parser, a declaration the file lacks.
///
/// Diagnostics are on the model of compiler diagnostics: a [Kind] says what happened (an
/// application can show its own, translated, message for it), a [Severity] how serious it
/// is, and the position is that of the first occurrence. The same problem met again is not
/// a new diagnostic: [#occurrences()] counts it. An unknown element in each of 2,600
/// entries is one diagnostic, not 2,600.
///
/// A file written by a LIFT tool normally gives none of severity [Severity#WARNING] or
/// [Severity#ERROR]. The [Severity#INFO] ones are about what the file uses without
/// declaring it, which is kept and written back as it was read.
///
/// @param kind what happened
/// @param severity how serious it is: a function of the kind
/// @param message an English message, with the line of the first occurrence
/// @param file the file read: the `.lift` file, or the `.lift-ranges` file a range of the
///        header refers to; `null` if the parser did not say
/// @param line the line of the first occurrence, or -1 if unknown
/// @param column the column of the first occurrence, or -1 if unknown
/// @param subject what the diagnostic is about, for grouping and for an application's own
///        messages: an element name (`custom`, or `{namespace}name`), an attribute
///        (`entry@colour`), a range (`dialects`), a value in its range
///        (`dialects/north`), a definition name; `null` for an XML error
/// @param occurrences how many times it was met, at least 1
/// @see LiftDictionary#getLoadDiagnostics()
public record LoadDiagnostic(
    Kind kind,
    Severity severity,
    String message,
    Path file,
    int line,
    int column,
    String subject,
    int occurrences
) {

    public LoadDiagnostic {
        if (kind == null || message == null) {
            throw new IllegalArgumentException("A diagnostic needs a kind and a message");
        }
        if (severity == null) {
            severity = kind.severity();
        }
        if (occurrences < 1) {
            throw new IllegalArgumentException("occurrences must be at least 1: " + occurrences);
        }
    }

    /// The file, if the parser said which.
    public Optional<Path> getFile() {
        return Optional.ofNullable(file);
    }

    /// Whether saving the dictionary leaves out what this diagnostic is about:
    /// [Kind#losesDataOnSave()].
    public boolean losesDataOnSave() {
        return kind.losesDataOnSave();
    }

    /// How serious a diagnostic is.
    public enum Severity {
        /// What the file uses without declaring it: kept, and written back as read.
        INFO,
        /// A problem in the file. The load goes on; what [Kind#losesDataOnSave()] is not saved.
        WARNING,
        /// A recoverable error of the XML parser: the file is not well-formed or not valid
        /// XML at that point, and the parser went on.
        ERROR
    }

    /// What a diagnostic is about.
    public enum Kind {
        /// An element that is not LIFT: another name, an element in a namespace (LIFT
        /// elements are in none), a `<lift>` other than the root, a `<grammatical-info>`
        /// outside a sense or a reversal. Skipped with all it holds.
        UNKNOWN_ELEMENT(Severity.WARNING, true),
        /// An attribute LIFT does not define on that element. Kept in
        /// `AbstractLiftRoot.getOtherXmlAttributes()`, but not saved.
        UNKNOWN_ATTRIBUTE(Severity.WARNING, true),
        /// A recoverable error of the XML parser. What it concerns may be lost.
        XML_ERROR(Severity.ERROR, true),
        /// A range of the header refers to an external file that cannot be found, or that
        /// does not define it: the range is empty. The reference is written back as read.
        MISSING_EXTERNAL_RANGE(Severity.WARNING, false),
        /// A range element declared twice in its range: the second declaration is ignored.
        DUPLICATE_RANGE_ELEMENT(Severity.WARNING, true),
        /// A field or trait definition declared twice: the last declaration is kept.
        DUPLICATE_DEFINITION(Severity.WARNING, true),
        /// A trait whose name has a definition for fields, or a field whose name has a
        /// definition for traits: it does not take that definition. Saved as read.
        DEFINITION_KIND_MISMATCH(Severity.WARNING, false),
        /// A built-in range the file uses but does not declare: its values are kept, and
        /// it is not written in the header (as in the file).
        UNDECLARED_RANGE(Severity.INFO, false),
        /// A value a declared range does not declare: kept, and not written in the header
        /// (as in the file).
        UNDECLARED_VALUE(Severity.INFO, false),
        /// The list of diagnostics reached the limit of the load options
        /// ([LoadOptions#maxDiagnostics()]): this one counts the problems not listed.
        /// [LiftDictionary#losesDataOnSave()] still takes them into account.
        DIAGNOSTICS_TRUNCATED(Severity.INFO, false);

        private final Severity severity;
        private final boolean losesDataOnSave;

        Kind(Severity severity, boolean losesDataOnSave) {
            this.severity = severity;
            this.losesDataOnSave = losesDataOnSave;
        }

        /// How serious a diagnostic of this kind is.
        public Severity severity() {
            return severity;
        }

        /// Whether saving the dictionary leaves out what a diagnostic of this kind is
        /// about: the file saved is not the file read. A strict load
        /// ([LoadOptions#strict()]) fails on the first one.
        public boolean losesDataOnSave() {
            return losesDataOnSave;
        }
    }
}
