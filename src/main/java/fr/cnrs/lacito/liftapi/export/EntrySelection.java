package fr.cnrs.lacito.liftapi.export;

/** Which entries go into a LaTeX export. */
public sealed interface EntrySelection {

    /** Every entry. */
    static EntrySelection all() {
        return All.INSTANCE;
    }

    /**
     * About {@code n} entries spread over the letters, for a preview: the first entry of
     * each letter, then the second of each, and so on, plus the entries they refer to, so
     * that their references resolve.
     */
    static EntrySelection sample(int n) {
        if (n < 1) throw new IllegalArgumentException("A sample needs at least one entry: " + n);
        return new Sample(n);
    }

    /** Every entry. */
    enum All implements EntrySelection { INSTANCE }

    /** A sample of about {@code size} entries; see {@link EntrySelection#sample(int)}. */
    record Sample(int size) implements EntrySelection {}
}
