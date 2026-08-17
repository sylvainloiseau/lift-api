package fr.cnrs.lacito.liftapi.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

public final class LiftHeader extends AbstractLiftRoot {

    private static final String NOTE_TYPE_RANGE = "note-type";
    private static final String TRANSLATION_TYPE_RANGE = "translation-type";
    private static final String GRAMMATICAL_INFO_RANGE = "grammatical-info";
    private static final String RELATION_TYPE_RANGE = "relation-type";
    private static final String INVERSE_TYPE_RANGE = "inverse-type";
    private static final String ETYMOLOGY_TYPE_RANGE = "etymology-type";

    private Map<String, LiftFieldAndTraitDefinition> fieldsAndTraitsDefinition = new HashMap<>();

    public Map<String, List<LiftFieldAndTraitDefinition>> rangeId2TraitDefinition = new HashMap<>();

    private final ObservableList<LiftHeaderRange> derivedRangeList =
        FXCollections.observableArrayList();

    private final ObservableMap<String, LiftHeaderRange> rangesMap =
        FXCollections.observableHashMap();

    //private LiftHeaderTypeManager noteTypesManager;
    //private LiftHeaderTypeManager relationTypesManager;
    //private LiftHeaderTypeManager inverseTypesManager;
    //private LiftHeaderTypeManager etymologyTypesManager;
    //private LiftHeaderTypeManager translationTypesManager;

    private LiftHeaderRange noteTypesManager;
    private LiftHeaderRange relationTypesManager;
    private LiftHeaderRange inverseTypesManager;
    private LiftHeaderRange etymologyTypesManager;
    private LiftHeaderRange translationTypesManager;

    private final SimpleSetProperty<String> metaLanguages =
        new SimpleSetProperty<>(FXCollections.emptyObservableSet());

    private final SimpleSetProperty<String> objectLanguages =
        new SimpleSetProperty<>(FXCollections.emptyObservableSet());

    public LiftHeader() {
        rangesMap.addListener(
            new MapChangeListener<String, LiftHeaderRange>() {
                @Override
                public void onChanged(
                    MapChangeListener.Change<
                        ? extends String,
                        ? extends LiftHeaderRange
                    > change
                ) {
                    if (change.wasRemoved()) {
                        derivedRangeList.remove(change.getValueRemoved());
                    } else {
                        derivedRangeList.add(change.getValueAdded());
                    }
                }
            }
        );

        // //noteTypesManager = new LiftHeaderTypeManager(NOTE_TYPE_RANGE, rangesMap, this);
        // noteTypesManager = new LiftHeaderTypeManager(rangesMap.computeIfAbsent(NOTE_TYPE_RANGE, x -> new LiftHeaderRange(NOTE_TYPE_RANGE, this)), null);
        // //relationTypesManager = new LiftHeaderTypeManager(RELATION_TYPE_RANGE, rangesMap, this);
        // relationTypesManager = new LiftHeaderTypeManager(rangesMap.computeIfAbsent(RELATION_TYPE_RANGE, x -> new LiftHeaderRange(RELATION_TYPE_RANGE, this)), null);
        // //inverseTypesManager = new LiftHeaderTypeManager(INVERSE_TYPE_RANGE, rangesMap, this);
        // inverseTypesManager = new LiftHeaderTypeManager(rangesMap.computeIfAbsent(INVERSE_TYPE_RANGE, x -> new LiftHeaderRange(INVERSE_TYPE_RANGE, this)), null);
        // //etymologyTypesManager = new LiftHeaderTypeManager(ETYMOLOGY_TYPE_RANGE, rangesMap, this);
        // etymologyTypesManager = new LiftHeaderTypeManager(rangesMap.computeIfAbsent(ETYMOLOGY_TYPE_RANGE, x -> new LiftHeaderRange(ETYMOLOGY_TYPE_RANGE, this)), null);
        // //translationTypesManager = new LiftHeaderTypeManager(TRANSLATION_TYPE_RANGE, rangesMap, this);
        // translationTypesManager = new LiftHeaderTypeManager(rangesMap.computeIfAbsent(TRANSLATION_TYPE_RANGE, x -> new LiftHeaderRange(TRANSLATION_TYPE_RANGE, this)), null);
        //noteTypesManager = new LiftHeaderTypeManager(NOTE_TYPE_RANGE, rangesMap, this);

        noteTypesManager = new LiftHeaderRange(NOTE_TYPE_RANGE, this);
        relationTypesManager = new LiftHeaderRange(RELATION_TYPE_RANGE, this);
        inverseTypesManager = new LiftHeaderRange(INVERSE_TYPE_RANGE, this);
        etymologyTypesManager = new LiftHeaderRange(ETYMOLOGY_TYPE_RANGE, this);
        translationTypesManager = new LiftHeaderRange(TRANSLATION_TYPE_RANGE, this);
    }

    public MultiText getDescription() {
        return getMainMultiText();
    }

    public boolean hasRanges(String id) {
        return rangesMap.containsKey(id);
    }

    public LiftHeaderRange getRange(String id) {
        if (!rangesMap.containsKey(id)) {
            throw new IllegalArgumentException("Range not found: " + id);
        }
        return rangesMap.get(id);
    }

    public LiftHeaderRange createRange(String id) {
        LiftHeaderRange r = new LiftHeaderRange(id,this);
        rangesMap.put(id, r);
        return r;
    }

    public void addRanges(LiftHeaderRange range) {
        rangesMap.put(range.getId(), range);
    }

    public ObservableList<LiftHeaderRange> getRanges() {
        return derivedRangeList;
    }

    public LiftFieldAndTraitDefinition createUnknownDefinition(String name) {
        LiftFieldAndTraitDefinition fd = new LiftFieldAndTraitDefinition(
            name,
            this
        );
        fieldsAndTraitsDefinition.put(name, fd);
        return fd;
    }

    public LiftFieldAndTraitDefinition createTraitDefinition(String name) {
        LiftFieldAndTraitDefinition fd = createUnknownDefinition(name);
        fd.setKind(LiftFieldAndTraitDefinitionKind.TRAIT);
        return fd;
    }

    public LiftFieldAndTraitDefinition createFieldDefinition(String name) {
        LiftFieldAndTraitDefinition fd = createUnknownDefinition(name);
        fd.setKind(LiftFieldAndTraitDefinitionKind.FIELD);
        return fd;
    }

    public boolean containsFieldsAndTraitsDefinitions(String name) {
        return fieldsAndTraitsDefinition.containsKey(name);
    }

    public Collection<LiftFieldAndTraitDefinition> getFieldsAndTraitsDefinitions() {
        return fieldsAndTraitsDefinition.values();
    }

    public LiftFieldAndTraitDefinition getFieldsAndTraitsDefinitions(String id) {
        return fieldsAndTraitsDefinition.get(id);
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

    // note types

    public LiftHeaderRange getNoteTypeManager() {
        return noteTypesManager;
    }

    // public SimpleSetProperty<LiftHeaderRangeElement> noteTypesProperty() {
    //     return noteTypesManager.typesProperty();
    // }

    // public void addNoteType(String type) {
    //     noteTypesManager.createRangeElement(type);
    // }

    // public boolean containsNoteType(String type) {
    //     return noteTypesManager.containsType(type);
    // }

    // public LiftHeaderRangeElement getNoteType(String type) {
    //     return noteTypesManager.getType(type);
    // }

    // relation types

    public LiftHeaderRange getRelationTypeManager() {
        return relationTypesManager;
    }

    // public SimpleSetProperty<LiftHeaderRangeElement> relationTypesProperty() {
    //     return relationTypesManager.typesProperty();
    // }

    // public void addRelationType(String type) {
    //     relationTypesManager.createRangeElement(type);
    // }

    // public boolean containsRelationType(String type) {
    //     return relationTypesManager.containsType(type);
    // }

    // public LiftHeaderRangeElement getRelationType(String type) {
    //     return relationTypesManager.getType(type);
    // }

    // inverse types

    public LiftHeaderRange getInverseTypeManager() {
        return inverseTypesManager;
    }

    // public SimpleSetProperty<LiftHeaderRangeElement> inverseTypesProperty() {
    //     return inverseTypesManager.typesProperty();
    // }

    // public void addInverseType(String type) {
    //     inverseTypesManager.createRangeElement(type);
    // }

    // public boolean containsInverseType(String type) {
    //     return inverseTypesManager.containsType(type);
    // }

    // public LiftHeaderRangeElement getInverseType(String type) {
    //     return inverseTypesManager.getType(type);
    // }

    // etymology types

    public LiftHeaderRange getEtymologyTypeManager() {
        return etymologyTypesManager;
    }

    // public SimpleSetProperty<LiftHeaderRangeElement> etymologyTypesProperty() {
    //     return etymologyTypesManager.typesProperty();
    // }

    // public void addEtymologyType(String type) {
    //     etymologyTypesManager.createRangeElement(type);
    // }

    // public boolean containsEtymologyType(String type) {
    //     return etymologyTypesManager.containsType(type);
    // }

    // public LiftHeaderRangeElement getEtymologyType(String type) {
    //     return etymologyTypesManager.getType(type);
    // }

    // translation types

    public LiftHeaderRange getTranslationTypeManager() {
        return translationTypesManager;
    }

    // public SimpleSetProperty<LiftHeaderRangeElement> translationTypesProperty() {
    //     return translationTypesManager.typesProperty();
    // }

    // public void addTranslationType(String type) {
    //     translationTypesManager.createRangeElement(type);
    // }

    // public boolean containsTranslationType(String type) {
    //     return translationTypesManager.containsType(type);
    // }

    // public LiftHeaderRangeElement getTranslationType(String type) {
    //     return translationTypesManager.getType(type);
    // }

    ///** duplicate of LiftHeaderRange */
    //private class LiftHeaderTypeManager {

    //    private String name;
    //    private LiftHeaderRange range;

    //    private SimpleSetProperty<LiftHeaderRangeElement> types = null;
    //    // new SimpleSetProperty<>(
    //    //     FXCollections.emptyObservableSet()
    //    // );

    //    LiftHeaderTypeManager(LiftHeaderRange range, LiftHeader header) {
    //        this.range = range;
    //        this.name = range.getId();
    //    }

    //    protected SimpleSetProperty<LiftHeaderRangeElement> typesProperty() {
    //        if (types == null) {
    //            initTypes();
    //        }
    //        return types;
    //    }

    //    protected LiftHeaderRangeElement getType(String type) {
    //        return range.getRangeElements().get(type);
    //    }

    //    protected void addType(String type) {
    //        range
    //            .getRangeElements()
    //            .put(type, new LiftHeaderRangeElement(type, range));
    //    }

    //    protected boolean containsType(String type) {
    //        return range.getRangeElements().containsKey(type);
    //    }

    //    private void initTypes() {
    //        MapProperty<String, LiftHeaderRangeElement> typeRangeElements = rangesMap
    //            .get(name)
    //            .getRangeElements();
    //        types = new SimpleSetProperty<>(
    //            FXCollections.emptyObservableSet()
    //        );
    //        types.addAll(typeRangeElements.values());
    //        typeRangeElements.addListener(
    //            new MapChangeListener<String, LiftHeaderRangeElement>() {
    //                @Override
    //                public void onChanged(
    //                    Change<
    //                        ? extends String,
    //                        ? extends LiftHeaderRangeElement
    //                    > change
    //                ) {
    //                    if (change.wasAdded()) {
    //                        types.add(change.getValueAdded());
    //                    } else if (change.wasRemoved()) {
    //                        types.remove(change.getValueRemoved());
    //                    }
    //                }
    //            }
    //        );
    //    }
    //}

}
