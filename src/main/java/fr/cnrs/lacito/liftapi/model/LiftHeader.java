package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

public final class LiftHeader extends AbstractLiftRoot {

    private List<LiftFieldAndTraitDefinition> fields = new ArrayList<>();
    private List<LiftHeaderRange> ranges = new ArrayList<>();

    private SimpleSetProperty<String> noteTypes = new SimpleSetProperty<>(
        FXCollections.emptyObservableSet()
    );

    private SimpleSetProperty<String> relationTypes = new SimpleSetProperty<>(
        FXCollections.emptyObservableSet()
    );

    private SimpleSetProperty<String> inverseTypes = new SimpleSetProperty<>(
        FXCollections.emptyObservableSet()
    );

    private SimpleSetProperty<String> etymologyTypes = new SimpleSetProperty<>(
        FXCollections.emptyObservableSet()
    );

    private SimpleSetProperty<String> translationTypes =
        new SimpleSetProperty<>(FXCollections.emptyObservableSet());

    private SimpleSetProperty<String> metaLanguages;
    private SimpleSetProperty<String> objectLanguages;

    public LiftHeader() {}

    public MultiText getDescription() {
        return getMainMultiText();
    }

    public List<LiftHeaderRange> getRanges() {
        return ranges;
    }

    public List<LiftFieldAndTraitDefinition> getFields() {
        return fields;
    }

    public SimpleSetProperty<String> getObjectLanguages() {
        return objectLanguages;
    }

    public void addObjectLanguage(String lang) {
        objectLanguages.add(lang);
    }

    public boolean containsObjectLanguage(String lang) {
        return objectLanguages.contains(lang);
    }

    public SimpleSetProperty<String> getMetaLanguages() {
        return metaLanguages;
    }

    public void addMetaLanguage(String lang) {
        metaLanguages.add(lang);
    }

    public boolean containsMetaLanguage(String lang) {
        return metaLanguages.contains(lang);
    }

    public SimpleSetProperty<String> getNoteTypes() {
        return noteTypes;
    }

    public void addNoteType(String type) {
        noteTypes.add(type);
    }

    public boolean containsNoteType(String type) {
        return noteTypes.contains(type);
    }

    public SimpleSetProperty<String> getRelationTypes() {
        return relationTypes;
    }

    public void addRelationType(String type) {
        relationTypes.add(type);
    }

    public boolean containsRelationType(String type) {
        return relationTypes.contains(type);
    }

    public SimpleSetProperty<String> getInverseTypes() {
        return inverseTypes;
    }

    public void addInverseType(String type) {
        inverseTypes.add(type);
    }

    public boolean containsInverseType(String type) {
        return inverseTypes.contains(type);
    }

    public SimpleSetProperty<String> getEtymologyTypes() {
        return etymologyTypes;
    }

    public void addEtymologyType(String type) {
        etymologyTypes.add(type);
    }

    public boolean containsEtymologyType(String type) {
        return etymologyTypes.contains(type);
    }

    public SimpleSetProperty<String> getTranslationTypes() {
        return translationTypes;
    }

    public void addTranslationType(String type) {
        translationTypes.add(type);
    }

    public boolean containsTranslationType(String type) {
        return translationTypes.contains(type);
    }
}
