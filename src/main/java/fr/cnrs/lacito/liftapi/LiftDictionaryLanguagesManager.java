package fr.cnrs.lacito.liftapi;

import java.util.Set;

import fr.cnrs.lacito.liftapi.change.LanguageAdded;
import fr.cnrs.lacito.liftapi.change.LanguageRemoved;
import fr.cnrs.lacito.liftapi.internal.ChangeSupport;
import javafx.beans.property.ReadOnlySetProperty;
import javafx.beans.property.ReadOnlySetWrapper;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

/**
 * Manages a set of languages (either meta languages or object languages) of a {@link LiftDictionary}.
 */
public class LiftDictionaryLanguagesManager {

    private final ObservableSet<String> languageSet = FXCollections.observableSet();
    private final ObservableSet<String> languagesView = FXCollections.unmodifiableObservableSet(languageSet);
    private final ReadOnlySetWrapper<String> languages =
        new ReadOnlySetWrapper<>(this, "languages", languagesView);

    /** The dictionary these languages belong to, to report their changes to. */
    private final LiftDictionary dictionary;

    private final SimpleMapProperty<String, Integer> languageCounts =
        new SimpleMapProperty<>(FXCollections.observableHashMap());

    LiftDictionaryLanguagesManager(LiftDictionary dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * The languages, as a read-only property; change them with {@link #addLanguage} and
     * {@link #removeLanguage}.
     */
    public ReadOnlySetProperty<String> languagesProperty() {
        return languages.getReadOnlyProperty();
    }

    /**
     * @return the languages, as a read-only observable set
     */
    public ObservableSet<String> getLanguages() {
        return languagesView;
    }

    public boolean hasLanguage(String lang) {
        return languageSet.contains(lang);
    }

    public void addLanguage(String lang) {
        if (languageSet.contains(lang)) {
            throw new IllegalArgumentException(
                "The languages already contain: " + lang
            );
        }
        languageSet.add(lang);
        languageCounts.get().put(lang, 0);
        if (ChangeSupport.anyObserved() && dictionary != null && dictionary.getChangeSupport().isObserved()) {
            dictionary.getChangeSupport().fire(new LanguageAdded(dictionary, this, lang));
        }
    }

    public void removeLanguage(String lang) {
        if (!languageSet.contains(lang)) {
            throw new IllegalArgumentException(
                "The languages do not contain: " + lang
            );
        }
        if (languageCounts.get().getOrDefault(lang, 0) != 0) {
            throw new IllegalArgumentException(
                "Cannot remove language with non-zero count: " + lang
            );
        }
        languageSet.remove(lang);
        languageCounts.get().remove(lang);
        if (ChangeSupport.anyObserved() && dictionary != null && dictionary.getChangeSupport().isObserved()) {
            dictionary.getChangeSupport().fire(new LanguageRemoved(dictionary, this, lang));
        }
    }

    /**
     * Record one more use of {@code lang}.
     *
     * @throws IllegalArgumentException if the language is not registered
     */
    public void addLanguageOccurrence(String lang) {
        requireKnown(lang);
        languageCounts.get().merge(lang, 1, Integer::sum);
    }

    /**
     * Record one fewer use of {@code lang}. The count never goes below zero.
     *
     * @throws IllegalArgumentException if the language is not registered
     */
    public void removeLanguageOccurrence(String lang) {
        requireKnown(lang);
        int count = languageCounts.get().getOrDefault(lang, 0);
        languageCounts.get().put(lang, Math.max(0, count - 1));
    }

    /** The number of forms currently using {@code lang}. */
    public int getLanguageOccurrence(String lang) {
        return languageCounts.get().getOrDefault(lang, 0);
    }

    private void requireKnown(String lang) {
        if (!languageCounts.get().containsKey(lang)) {
            throw new IllegalArgumentException(
                "Language not registered in the dictionary: " + lang
            );
        }
    }

    /// Directly set the count of occurrences for a language.
    /// This is used when loading a dictionary from XML, where we can count the occurrences of each language in the XML and set it directly.
    public void setLanguageOccurrence(String lang, Long long1) {
        languageCounts.get().put(lang, long1.intValue());
    }

}
