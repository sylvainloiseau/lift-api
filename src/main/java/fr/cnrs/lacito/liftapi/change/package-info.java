/**
 * The changes of a dictionary, as reversible events: what an undo history, a
 * transaction or a change log is built on.
 *
 * <h2>Listening</h2>
 *
 * <pre>{@code
 * Subscription s = dictionary.addChangeListener(change -> history.add(change));
 * ...
 * s.unsubscribe();
 * }</pre>
 *
 * Every public operation that changes a dictionary reports one or more
 * {@link fr.cnrs.lacito.liftapi.change.ModelChange}s:
 *
 * <ul>
 * <li>{@link fr.cnrs.lacito.liftapi.change.ComponentAdded} and
 * {@link fr.cnrs.lacito.liftapi.change.ComponentRemoved}: a component (with its whole
 * subtree) attached to or removed from its parent, or an entry added to or removed from
 * the dictionary. For a component held in a list, the event carries its position;
 * one held in a map (notes, fields, translations) or a single slot (grammatical info,
 * main of a reversal) is put back under its key or in its slot.</li>
 * <li>{@link fr.cnrs.lacito.liftapi.change.ValueChanged}: a value replaced, with the old
 * and the new value, and a {@link fr.cnrs.lacito.liftapi.change.ModelProperty} saying
 * which one (a date, a type, the text of a form, a trait value...).</li>
 * <li>{@link fr.cnrs.lacito.liftapi.change.FormAdded} and
 * {@link fr.cnrs.lacito.liftapi.change.FormRemoved}: a form of a multitext. Typing in a
 * text field bound to {@link fr.cnrs.lacito.liftapi.model.MultiText#formTextProperty}
 * reports a {@code FormAdded} for the first character, a {@code ValueChanged} for each
 * edit, and a {@code FormRemoved} when the text becomes empty.</li>
 * <li>The header: features, feature sets and field or trait definitions added and
 * removed; the languages of the dictionary.</li>
 * </ul>
 *
 * <h2>Rules</h2>
 *
 * <ul>
 * <li><b>Synchronous</b>, on the thread that made the change, <b>after</b> it succeeded.
 * An operation that throws reports nothing, and changes nothing.</li>
 * <li><b>Only attached objects report.</b> Building a subtree outside a dictionary (a
 * new entry, a sense before it is added) reports nothing; attaching it is one
 * {@code ComponentAdded}, which holds the objects themselves, so that reverting and
 * reapplying it removes and puts back the whole subtree as it was built.</li>
 * <li><b>Loading reports nothing</b>: a dictionary is only handed out once loaded, so no
 * listener can be there yet.</li>
 * <li><b>Cascades are reported in order.</b> An operation that changes several things
 * reports each of them: attaching a component whose forms use a language the dictionary
 * did not have reports a {@code LanguageAdded} before the {@code ComponentAdded}; a sense
 * attached without a LIFT id reports the id the dictionary gives it; replacing a
 * grammatical info reports the removal of the previous one.</li>
 * <li><b>A listener must not change the dictionary</b> while it is told of a change; it
 * gets an {@code IllegalStateException}. Collect the changes, act on them afterwards.</li>
 * <li>Derived state - the indexes of the registry, the counters, the occurrences of the
 * languages, the reference counts - is not reported: it follows from the changes.</li>
 * </ul>
 *
 * <h2>Reverting</h2>
 *
 * {@link fr.cnrs.lacito.liftapi.change.ModelChange#revert()} undoes a change, and
 * {@link fr.cnrs.lacito.liftapi.change.ModelChange#reapply()} makes it again. Both go
 * through the public API, so the indexes stay consistent, and both report their own
 * changes (an undo history ignores the changes it makes itself). Revert a sequence in
 * reverse order, reapply it in order.
 *
 * <h2>Transactions</h2>
 *
 * <pre>{@code
 * try (ChangeRecorder rec = dictionary.recordChanges()) {
 *     importEntries(file);   // any changes; an exception skips the commit
 *     rec.commit();          // keep them; without commit, close() reverts them all
 * }
 * }</pre>
 *
 * An undo history is built the same way: one undo step is the changes recorded during
 * one user action. What a step is called, how steps are merged, where the saved state
 * is, how many are kept: that is for the application to decide.
 *
 * <h2>Cost</h2>
 *
 * While no dictionary has a listener - while a file is read, in particular - a change
 * costs one more field read: nothing is looked up, computed or allocated for reporting.
 */
package fr.cnrs.lacito.liftapi.change;
