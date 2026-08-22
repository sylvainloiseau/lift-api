package fr.cnrs.lacito.liftapi;

import java.util.Set;

import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

public class LiftDictionaryLanguagesManager {

    private final SimpleSetProperty<String> languages =
        new SimpleSetProperty<>(FXCollections.observableSet());

    private final SimpleMapProperty<String, Integer> languageCounts =
        new SimpleMapProperty<>(FXCollections.observableHashMap());


    protected LiftDictionaryLanguagesManager() {
        
    }

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
        if (languageCounts.get().get(lang) != 0) {
            throw new IllegalArgumentException(
                "Cannot remove language with non-zero count: " + lang
            );
        }
        languages.get().remove(lang);
        languageCounts.get().remove(lang);
    }

    public void addLanguageOccurrence(String key) {
        languageCounts.get().put(key, languageCounts.get().get(key) + 1);
    }

    public void removeLanguageOccurrence(String key) {
        Integer count = languageCounts.get().get(key);
        if (count != 0) {
            count = languageCounts.get().put(key, count);
            languageCounts.get().put(key, count - 1);
        }
    }


}
