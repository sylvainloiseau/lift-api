package fr.cnrs.lacito.liftapi.change;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import javafx.util.Subscription;

/// Records the changes of a dictionary during a block of code, and reverts them unless
/// the block commits: a transaction.
///
/// ```java
/// try (ChangeRecorder rec = dictionary.recordChanges()) {
///     ...                    // any changes
///     rec.commit();          // keep them; without commit, close() reverts them all
/// }
/// rec.changes();             // the changes, in order, e.g. for a report
/// ```
///
/// - An exception thrown in the block skips {@link #commit()}, so the changes made
///   before it are reverted.
/// - Recorders can nest: an inner one that does not commit reverts only its own changes.
///   The outer one sees both the changes and their reverts, so reverting it later is
///   still correct.
/// - The building block of an undo history: one undo step is "the changes recorded
///   during one user action". What a step is called, how steps are merged or limited, is
///   for the application to decide.
public final class ChangeRecorder implements AutoCloseable {

    private final List<ModelChange> changes = new ArrayList<>();
    private final Subscription subscription;
    private boolean committed;
    private boolean closed;

    private ChangeRecorder(LiftDictionary dictionary) {
        this.subscription = dictionary.addChangeListener(changes::add);
    }

    /// Start recording the changes of {@code dictionary}; {@link LiftDictionary#recordChanges()}
    /// is the usual way to call it.
    ///
    /// @param dictionary the dictionary to record
    /// @return a recorder, to close
    public static ChangeRecorder start(LiftDictionary dictionary) {
        if (dictionary == null) {
            throw new IllegalArgumentException("dictionary cannot be null");
        }
        return new ChangeRecorder(dictionary);
    }

    /// The changes recorded so far, in the order they were made.
    public List<ModelChange> changes() {
        return Collections.unmodifiableList(new ArrayList<>(changes));
    }

    /// Keep the changes: {@link #close()} will not revert them.
    public void commit() {
        committed = true;
    }

    /// Whether {@link #commit()} was called.
    public boolean isCommitted() {
        return committed;
    }

    /// Stop recording, and revert the changes, in reverse order, unless
    /// {@link #commit()} was called. Calling it again does nothing.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        subscription.unsubscribe();
        if (!committed) {
            for (int i = changes.size() - 1; i >= 0; i--) {
                changes.get(i).revert();
            }
        }
    }
}
