package fr.cnrs.lacito.liftapi;

// import java.util.HashMap;
// import java.util.Map;
import java.util.Set;

import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.MultiText;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

public class LiftDictionaryLanguagesManager {

    // private final Map<String, Integer> objectLanguageCount = new HashMap<>();
    // private final Map<String, Integer> metaLanguageCount = new HashMap<>();
    private LiftDictionaryRegistry liftDictionaryRegistry;
    private final SimpleSetProperty<String> metaLanguagesProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    private final SimpleSetProperty<String> objectLanguagesProperty = new SimpleSetProperty<>(FXCollections.observableSet());

	public LiftDictionaryLanguagesManager(LiftDictionaryRegistry liftDictionaryRegistry) {
		this.liftDictionaryRegistry = liftDictionaryRegistry;
        discoverObjectLanguage();
        discoverMetaLanguage();
	}

    public SimpleSetProperty<String> metaLanguagesProperty() {
        return metaLanguagesProperty;
    }

    public SimpleSetProperty<String> objectLanguagesProperty() {
        return objectLanguagesProperty;
    }

    public void addMetaLang(String metaLang) {
        if (metaLanguagesProperty.get().contains(metaLang)) {
            throw new IllegalArgumentException("The meta languages already contain: " + metaLang);
        }
        metaLanguagesProperty.get().add(metaLang);
    }

    public void addObjectLang(String objectLang) {
        if (objectLanguagesProperty.get().contains(objectLang)) {
            throw new IllegalArgumentException("The object languages already contain: " + objectLang);
        }
        objectLanguagesProperty.get().add(objectLang);
    }

    // the following method where for MultitextBuilder

    // public void addMetaLanguageOccurrence(String key) {
    //     metaLanguageCount.put(key, metaLanguageCount.getOrDefault(key, 0) + 1);
    // }

    // public void removeMetaLanguageOccurrence(String key) {
    //     Integer count = metaLanguageCount.get(key);
    //     if (count == 1) {
    //         metaLanguageCount.remove(key);
    //     } else {
    //         metaLanguageCount.put(key, count - 1);
    //         this.liftDictionaryRegistry.metaLanguagesProperty().get().remove(key);
    //     }
    // }
    
    // public void addObjectLanguageOccurrence(String key) {
    //     objectLanguageCount.put(key, objectLanguageCount.getOrDefault(key, 0) + 1);
    // }

    // public void removeObjectLanguageOccurrence(String key) {
    //     Integer count = objectLanguageCount.get(key);
    //     if (count == 1) {
    //         objectLanguageCount.remove(key);
    //         this.liftDictionaryRegistry.objectLanguagesProperty().get().remove(key);
    //     } else {
    //         objectLanguageCount.put(key, count - 1);
    //     }
    // }
 
    private void discoverMetaLanguage() {
        Set<String> metaLang = metaLanguagesProperty.get();
        for (MultiText m : this.liftDictionaryRegistry.metaTextProperty().get()) {
            for (Form f : m.getForms()) {
                metaLang.add(f.getLang());
            }
        }
    }

    private void discoverObjectLanguage() {
        Set<String> objectLang = objectLanguagesProperty.get();
        for (MultiText m : this.liftDictionaryRegistry.objectTextProperty().get()) {
            for (Form f : m.getForms()) {
                objectLang.add(f.getLang());
            }
        }
    }

}
