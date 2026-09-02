package fr.cnrs.lacito.liftapi.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;

/**
 * Represents the header of a LIFT file, including feature sets and field/trait definitions.
 */
public final class LiftHeader extends AbstractLiftRoot {

    private static final String NOTE_TYPE_RANGE = "note-type";
    private static final String TRANSLATION_TYPE_RANGE = "translation-type";
    private static final String GRAMMATICAL_INFO_RANGE = "grammatical-info";
    private static final String RELATION_TYPE_RANGE = "relation-type";
    private static final String INVERSE_TYPE_RANGE = "inverse-type";
    private static final String ANNOTATION_TYPE_RANGE = "annotation-type";
    private static final String ETYMOLOGY_TYPE_RANGE = "etymology-type";
    private static final String VARIANT_TYPE_RANGE = "variant-type";

    private Map<String, LiftFieldAndTraitDefinition> fieldsAndTraitsDefinition = new HashMap<>();

    private final ObservableList<FeatureSet> derivedFeatureSetList =
        FXCollections.observableArrayList();

    private final ObservableMap<String, FeatureSet> featureSetsMap =
        FXCollections.observableHashMap();

    //private LiftHeaderTypeManager noteTypesManager;
    //private LiftHeaderTypeManager relationTypesManager;
    //private LiftHeaderTypeManager inverseTypesManager;
    //private LiftHeaderTypeManager etymologyTypesManager;
    //private LiftHeaderTypeManager translationTypesManager;

    private FeatureSet noteTypesManager;
    private FeatureSet relationTypesManager;
    private FeatureSet inverseTypesManager;
    private FeatureSet etymologyTypesManager;
    private FeatureSet translationTypesManager;
    private FeatureSet grammaticalInfoManager;
    private FeatureSet annotationTypesManager;
    private FeatureSet variantTypesManager;

    private final SimpleSetProperty<String> metaLanguages =
        new SimpleSetProperty<>(FXCollections.observableSet());

    private final SimpleSetProperty<String> objectLanguages =
        new SimpleSetProperty<>(FXCollections.observableSet());

    public LiftHeader() {
        featureSetsMap.addListener(
            new MapChangeListener<String, FeatureSet>() {
                @Override
                public void onChanged(
                    MapChangeListener.Change<
                        ? extends String,
                        ? extends FeatureSet
                    > change
                ) {
                    if (change.wasRemoved()) {
                        derivedFeatureSetList.remove(change.getValueRemoved());
                    } else {
                        derivedFeatureSetList.add(change.getValueAdded());
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

        noteTypesManager = new FeatureSet(NOTE_TYPE_RANGE, this);
        relationTypesManager = new FeatureSet(RELATION_TYPE_RANGE, this);
        inverseTypesManager = new FeatureSet(INVERSE_TYPE_RANGE, this);
        etymologyTypesManager = new FeatureSet(ETYMOLOGY_TYPE_RANGE, this);
        translationTypesManager = new FeatureSet(TRANSLATION_TYPE_RANGE, this);
        annotationTypesManager = new FeatureSet(TRANSLATION_TYPE_RANGE, this);
        grammaticalInfoManager = new FeatureSet(GRAMMATICAL_INFO_RANGE, this);
        variantTypesManager = new FeatureSet(TRANSLATION_TYPE_RANGE, this);

        featureSetsMap.put(NOTE_TYPE_RANGE, noteTypesManager);
        featureSetsMap.put(RELATION_TYPE_RANGE, relationTypesManager);
        featureSetsMap.put(INVERSE_TYPE_RANGE, inverseTypesManager);
        featureSetsMap.put(ETYMOLOGY_TYPE_RANGE, etymologyTypesManager);
        featureSetsMap.put(TRANSLATION_TYPE_RANGE, translationTypesManager);
        featureSetsMap.put(GRAMMATICAL_INFO_RANGE, grammaticalInfoManager);
        featureSetsMap.put(ANNOTATION_TYPE_RANGE, annotationTypesManager);
        featureSetsMap.put(VARIANT_TYPE_RANGE, variantTypesManager);
    }

    /**
     * Returns the description of this header.
     *
     * @return the description of this header
     */
    public MultiText getDescription() {
        return getMainMultiText();
    }

    // ------------------------------------------------------------------------
    // Feature set management
    // ------------------------------------------------------------------------

    /**
     * Returns whether this header has a feature set with the given ID.
     *
     * @param id the ID of the feature set to check
     * @return true if this header has a feature set with the given ID, false otherwise
     */
    public boolean hasFeatureSet(String id) {
        return featureSetsMap.containsKey(id);
    }

    /**
     * Returns the feature set with the given ID.
     *
     * @param id the ID of the feature set to return
     * @return the feature set with the given ID
     * @throws IllegalArgumentException if no feature set with the given ID exists
     */
    public FeatureSet getFeatureSet(String id) {
        if (!featureSetsMap.containsKey(id)) {
            throw new IllegalArgumentException("Range not found: " + id);
        }
        return featureSetsMap.get(id);
    }

    /**
     * Creates a new feature set with the given ID and adds it to this header.
     *
     * @param id the ID of the feature set to create
     * @return the newly created feature set
     * @throws IllegalArgumentException if a feature set with the same ID already exists
     */
    public FeatureSet addFeatureSet(String id) {
        if (hasFeatureSet(id))
            throw new IllegalArgumentException("duplicate feature set: " + id);
        FeatureSet fs = new FeatureSet(id, this);
        addFeatureSet(fs);
        return fs;
    }

    /**
     * Adds a feature set to this header.
     *
     * @param fs the feature set to add
     * @throws IllegalArgumentException if a feature set with the same ID already exists
     */
    public void addFeatureSet(FeatureSet fs) {
        if (hasFeatureSet(fs.getId()))
            throw new IllegalArgumentException("duplicate feature set: " + fs.getId());
        featureSetsMap.put(fs.getId(), fs);
    }

    /**
     * Returns the list of feature sets in this header.
     *
     * @return the list of feature sets
     */
    public ObservableList<FeatureSet> getFeatureSets() {
        return derivedFeatureSetList;
    }

    // ------------------------------------------------------------------------
    // Field and list definitions
    // ------------------------------------------------------------------------

    /**
     * Creates a definition with the given name and a {@link
     * LiftFieldAndTraitDefinitionKind#UNKNOWN} kind, ie available
     * for Field as well as for Trait.
     *
     * @param name the name of the definition to create
     * @return the created definition
     */
    public LiftFieldAndTraitDefinition createUnknownDefinition(String name) {
        LiftFieldAndTraitDefinition fd = new LiftFieldAndTraitDefinition(
            name,
            this
        );
        fieldsAndTraitsDefinition.put(name, fd);
        return fd;
    }

    /**
     * Creates a trait (a kind of {@link LiftFieldAndTraitDefinitionKind#TRAIT}) definition with the given name.
     *
     * @param name the name of the definition to create
     * @return the created definition
     */
    public LiftFieldAndTraitDefinition createTraitDefinition(String name) {
        LiftFieldAndTraitDefinition fd = createUnknownDefinition(name);
        fd.setKind(LiftFieldAndTraitDefinitionKind.TRAIT);
        return fd;
    }

    /**
     * Creates a field (a kind of {@link LiftFieldAndTraitDefinitionKind#FIELD}) definition with the given name.
     *
     * @param name the name of the definition to create
     * @return the created definition
     */
    public LiftFieldAndTraitDefinition createFieldDefinition(String name) {
        LiftFieldAndTraitDefinition fd = createUnknownDefinition(name);
        fd.setKind(LiftFieldAndTraitDefinitionKind.FIELD);
        return fd;
    }

    /**
     * Returns whether the header contains a definition with the given name.
     *
     * @param name the name of the definition to check
     * @return true if the definition is found, false otherwise
     */
    public boolean containsFieldsAndTraitsDefinitions(String name) {
        return fieldsAndTraitsDefinition.containsKey(name);
    }

    /**
     * Returns the collection of all fields and traits definitions.
     *
     * @return the collection of definitions
     */
    public Collection<LiftFieldAndTraitDefinition> getFieldsAndTraitsDefinitions() {
        return fieldsAndTraitsDefinition.values();
    }

    /**
     * Returns the collection of fields and traits definitions for the given target.
     *
     * @param target the target to filter by
     * @return the filtered collection of definitions
     */
    public ObservableList<LiftFieldAndTraitDefinition> getFieldsAndTraitsDefinitionsFor(LiftFieldAndTraitDefinitionTarget target) {
        FilteredList<LiftFieldAndTraitDefinition> filteredList = new FilteredList<>(FXCollections.observableArrayList(fieldsAndTraitsDefinition.values()));
        filteredList.setPredicate(fd -> fd.getTargets().contains(target) );
        return filteredList;
    }

    /**
     * Returns the collection of fields definitions.
     *
     * @return the collection of fields definitions
     */
    public ObservableList<LiftFieldAndTraitDefinition> getFieldsDefinitions() {
        FilteredList<LiftFieldAndTraitDefinition> filteredList = new FilteredList<>(FXCollections.observableArrayList(fieldsAndTraitsDefinition.values()));
        filteredList.setPredicate(fd -> {
            return fd.getKind() == LiftFieldAndTraitDefinitionKind.FIELD;
        });
        return filteredList;
    }

    /**
     * Returns the collection of fields definitions for the given target.
     *
     * @param target the target to filter by
     * @return the filtered collection of fields definitions
     */
    public ObservableList<LiftFieldAndTraitDefinition> getFieldsDefinitionsFor(LiftFieldAndTraitDefinitionTarget target) {
        FilteredList<LiftFieldAndTraitDefinition> filteredList = new FilteredList<>(getFieldsDefinitions());
        filteredList.setPredicate(fd -> {
            return fd.getTargets().contains(target);
        });
        return filteredList;
    }

    /**
     * Returns the collection of traits definitions.
     *
     * @return the collection of traits definitions
     */
    public ObservableList<LiftFieldAndTraitDefinition> getTraitsDefinitions() {
        FilteredList<LiftFieldAndTraitDefinition> filteredList = new FilteredList<>(FXCollections.observableArrayList(fieldsAndTraitsDefinition.values()));
        filteredList.setPredicate(fd -> {
            return fd.getKind() == LiftFieldAndTraitDefinitionKind.TRAIT;
        });
        return filteredList;
    }

    /**
     * Returns the collection of traits definitions for the given target.
     *
     * @param target the target to filter by
     * @return the filtered collection of traits definitions
     */
    public ObservableList<LiftFieldAndTraitDefinition> getTraitsDefinitionsFor(LiftFieldAndTraitDefinitionTarget target) {
        FilteredList<LiftFieldAndTraitDefinition> filteredList = new FilteredList<>(FXCollections.observableArrayList(getTraitsDefinitions()));
        filteredList.setPredicate(fd -> {
            return fd.getTargets().contains(target);
        });
        return filteredList;
    }

    /**
     * Returns the field or trait definition for the given id.
     *
     * @param id the id of the field or trait definition to return
     * @return the field or trait definition
     * @throws IllegalArgumentException if no such field or trait definition exists
     */
    public LiftFieldAndTraitDefinition getFieldsAndTraitsDefinitions(String id) {
        if (!fieldsAndTraitsDefinition.containsKey(id)) {
            throw new IllegalArgumentException("No such field or trait definition: " + id);
        }
        return fieldsAndTraitsDefinition.get(id);
    }

    /**
     * Returns the trait definition for the given id, or creates it if it does not exist.
     *
     * @param id the id of the trait definition to return or create
     * @return the trait definition
     * @throws IllegalArgumentException if a field definition already exists with the given id
     */
    public LiftFieldAndTraitDefinition getOrCreateTraitsDefinitions(String id) {
        if (fieldsAndTraitsDefinition.containsKey(id)
            && fieldsAndTraitsDefinition.get(id).getKind() == LiftFieldAndTraitDefinitionKind.FIELD) {
            throw new IllegalArgumentException("Cannot create a trait definition with this name: a field definition already exists: " + id);
        }
        if (!fieldsAndTraitsDefinition.containsKey(id)) {
            fieldsAndTraitsDefinition.put(id, createTraitDefinition(id));
        }
        return fieldsAndTraitsDefinition.get(id);
    }

    /**
     * Returns the field definition for the given id, or creates it if it does not exist.
     *
     * @param id the id of the field definition to return or create
     * @return the field definition
     * @throws IllegalArgumentException if a trait definition already exists with the given id
     */
    public LiftFieldAndTraitDefinition getOrCreateFieldDefinitions(String id) {
        if (fieldsAndTraitsDefinition.containsKey(id)
            && fieldsAndTraitsDefinition.get(id).getKind() == LiftFieldAndTraitDefinitionKind.TRAIT) {
            throw new IllegalArgumentException("Cannot create a field definition with this name: a trait definition already exists: " + id);
        }
        if (!fieldsAndTraitsDefinition.containsKey(id)) {
            fieldsAndTraitsDefinition.put(id, createFieldDefinition(id));
        }
        return fieldsAndTraitsDefinition.get(id);
    }

    // ------------------------------------------------------------------------
    // Fixed feature set
    // ------------------------------------------------------------------------

    /**
     * Returns the grammatical info manager.
     */
    public FeatureSet getGrammaticalInfoManager() {
        return grammaticalInfoManager;
    }

    // note types

    /**
     * Returns the note type manager.
     */
    public FeatureSet getNoteTypeManager() {
        return noteTypesManager;
    }

    // relation types

    /**
     * Returns the variant type manager.
     */
    public FeatureSet getVariantTypeManager() {
        return variantTypesManager;
    }

    // relation types

    /**
     * Returns the relation type manager.
     */
    public FeatureSet getRelationTypeManager() {
        return relationTypesManager;
    }

    // inverse types

    /**
     * Returns the inverse type manager.
     */
    public FeatureSet getInverseTypeManager() {
        return inverseTypesManager;
    }

    // etymology types

    /**
     * Returns the etymology type manager.
     */
    public FeatureSet getEtymologyTypeManager() {
        return etymologyTypesManager;
    }

    // translation types

    /**
     * Returns the translation type manager.
     */
    public FeatureSet getTranslationTypeManager() {
        return translationTypesManager;
    }

    // translation types

    /**
     * Returns the annotation type manager.
     */
    public FeatureSet getAnnotationTypeManager() {
        return annotationTypesManager;
    }
}
