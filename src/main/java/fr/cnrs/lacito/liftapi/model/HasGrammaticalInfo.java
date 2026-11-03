package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;
import javafx.beans.property.ReadOnlyObjectProperty;

/**
 * A component that holds at most one {@link GrammaticalInfo} ({@code <grammatical-info>}):
 * a sense, and a reversal or the {@code <main>} of a reversal (LIFT {@code reversal-content}
 * and {@code reversal-main}).
 *
 * The grammatical info is a component in a single slot: setting one deletes the current
 * one first, and its changes are reported as components added and removed.
 */
public sealed interface HasGrammaticalInfo extends LiftObject permits LiftSense, LiftReversal {

    /** The grammatical information, if there is one. */
    Optional<GrammaticalInfo> getGrammaticalInfo();

    /**
     * The grammatical info, read-only: it changes through
     * {@link #setGrammaticalInfo(GrammaticalInfo)} and {@link #deleteGrammaticalInfo()}.
     */
    ReadOnlyObjectProperty<GrammaticalInfo> grammaticalInfoProperty();

    /**
     * Attach a grammatical information, in place of the current one, which is deleted (and
     * unregistered) first. {@code gi} is registered if this component belongs to a
     * dictionary.
     *
     * @param gi the grammatical information, or {@code null} to delete the current one
     */
    void setGrammaticalInfo(GrammaticalInfo gi);

    /**
     * Create a grammatical information for the given part of speech and attach it, in place
     * of the current one (see {@link #setGrammaticalInfo(GrammaticalInfo)}).
     *
     * @param value the part of speech
     */
    default void setGrammaticalInfo(Feature value) {
        setGrammaticalInfo(GrammaticalInfo.create(value));
    }

    /**
     * Remove the grammatical information, unregistering it - and the traits it carries - if
     * this component belongs to a dictionary. Does nothing if there is none.
     */
    void deleteGrammaticalInfo();
}
