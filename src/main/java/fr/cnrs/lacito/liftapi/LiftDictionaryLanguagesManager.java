package fr.cnrs.lacito.liftapi;

import java.util.Set;

import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

/**
 * Manages a set of languages (either meta languages or object languages) of a {@link LiftDictionary}.
 */
public class LiftDictionaryLanguagesManager {

    private final SimpleSetProperty<String> languages =
        new SimpleSetProperty<>(FXCollections.observableSet());

    private final SimpleMapProperty<String, Integer> languageCounts =
        new SimpleMapProperty<>(FXCollections.observableHashMap());

    protected LiftDictionaryLanguagesManager() {}

    public SimpleSetProperty<String> languagesProperty() {
        return languages;
    }

    public Set<String> getLanguages() {
        return languages.get();
    }

    public boolean hasLanguage(String lang) {
        return languages.get().contains(lang);
    }

    public void addLanguage(String lang) {
        if (languages.get().contains(lang)) {
            throw new IllegalArgumentException(
                "The languages already contain: " + lang
            );
        }
        languages.get().add(lang);
        languageCounts.get().put(lang, 0);
    }

    public void removeLanguage(String lang) {
        if (!languages.get().contains(lang)) {
            throw new IllegalArgumentException(
                "The languages do not contain: " + lang
            );
        }
        if (languageCounts.get().getOrDefault(lang, 0) != 0) {
            throw new IllegalArgumentException(
                "Cannot remove language with non-zero count: " + lang
            );
        }
        languages.get().remove(lang);
        languageCounts.get().remove(lang);
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
