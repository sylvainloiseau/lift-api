package fr.cnrs.lacito.liftapi;

/// How [LiftDictionary#loadDictionaryFromFile(java.io.File, LoadOptions)] reads a file.
///
/// @param strict whether the load fails, rather than going on with a diagnostic, on the
///        first problem that would make the file saved differ from the file read
///        ([LoadDiagnostic.Kind#losesDataOnSave()]: an unknown element or attribute, a
///        declaration made twice, a recoverable XML error), and on an external range that
///        cannot be found. What the file uses without declaring it
///        ([LoadDiagnostic.Severity#INFO]) never fails a load: it is kept and written back
/// @param maxDiagnostics how many distinct diagnostics are listed, at least 1; past it,
///        one [LoadDiagnostic.Kind#DIAGNOSTICS_TRUNCATED] diagnostic counts the others, so
///        that a pathological file cannot grow the list without bound
/// @see LiftDictionary#getLoadDiagnostics()
public record LoadOptions(boolean strict, int maxDiagnostics) {

    /// The default limit of [#maxDiagnostics()].
    public static final int DEFAULT_MAX_DIAGNOSTICS = 1000;

    public LoadOptions {
        if (maxDiagnostics < 1) {
            throw new IllegalArgumentException("maxDiagnostics must be at least 1: " + maxDiagnostics);
        }
    }

    /// Not strict, and [#DEFAULT_MAX_DIAGNOSTICS] diagnostics at most.
    public static LoadOptions defaults() {
        return new LoadOptions(false, DEFAULT_MAX_DIAGNOSTICS);
    }

    /// These options, strict or not.
    public LoadOptions withStrict(boolean strict) {
        return new LoadOptions(strict, maxDiagnostics);
    }

    /// These options, with another limit to the diagnostics listed.
    public LoadOptions withMaxDiagnostics(int maxDiagnostics) {
        return new LoadOptions(strict, maxDiagnostics);
    }
}
