package fr.cnrs.lacito.liftapi.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.change.DefinitionAdded;
import fr.cnrs.lacito.liftapi.change.DefinitionRemoved;
import fr.cnrs.lacito.liftapi.change.FeatureSetAdded;
import fr.cnrs.lacito.liftapi.change.FeatureSetRemoved;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/**
 * Represents the header of a LIFT file, including feature sets and field/trait definitions.
 *
 * The header knows the dictionary it belongs to, so that its changes - a feature added to
 * a range, a definition edited - are reported like those of the entries (see the
 * {@code change} package). Its components are not registered in the dictionary's
 * indexes, which only hold the entries' subtrees.
 */
public final class LiftHeader extends AbstractLiftRoot {

    private static final String NOTE_TYPE_RANGE = "note-type";
    private static final String TRANSLATION_TYPE_RANGE = "translation-type";
    private static final String GRAMMATICAL_INFO_RANGE = "grammatical-info";
    private static final String RELATION_TYPE_RANGE = "lexical-relation";
    private static final String INVERSE_TYPE_RANGE = "reversal-type";
    private static final String ANNOTATION_TYPE_RANGE = "annotation-type";
    private static final String ETYMOLOGY_TYPE_RANGE = "etymology";

    /** The dictionary this header belongs to; set once, by the dictionary. */
    private LiftDictionary dictionary;

    /** The definitions by name, and the same in a list for the observable views. */
    private final Map<String, LiftFieldAndTraitDefinition> fieldsAndTraitsDefinition = new HashMap<>();
    private final ObservableList<LiftFieldAndTraitDefinition> definitions =
        FXCollections.observableArrayList(d -> new Observable[] {d.kindProperty(), d.targetsProperty()});
    private final ObservableList<LiftFieldAndTraitDefinition> definitionsView =
        FXCollections.unmodifiableObservableList(definitions);

    /** The feature sets by id, and the same in a list, in the order they were added. */
    private final Map<String, FeatureSet> featureSetsMap = new HashMap<>();
    private final ObservableList<FeatureSet> featureSets = FXCollections.observableArrayList();
    private final ObservableList<FeatureSet> featureSetsView =
        FXCollections.unmodifiableObservableList(featureSets);

    private final FeatureSet noteTypesManager;
    private final FeatureSet relationTypesManager;
    private final FeatureSet inverseTypesManager;
    private final FeatureSet etymologyTypesManager;
    private final FeatureSet translationTypesManager;
    private final FeatureSet grammaticalInfoManager;
    private final FeatureSet annotationTypesManager;
    private final Set<FeatureSet> builtIn;

    /**
     * Creates a new header, with its built-in feature sets: the types of notes,
     * relations, reversals, etymologies, translations, annotations, and the parts of
     * speech.
     */
    public LiftHeader() {
        noteTypesManager = new FeatureSet(NOTE_TYPE_RANGE, this);
        relationTypesManager = new FeatureSet(RELATION_TYPE_RANGE, this);
        inverseTypesManager = new FeatureSet(INVERSE_TYPE_RANGE, this);
        etymologyTypesManager = new FeatureSet(ETYMOLOGY_TYPE_RANGE, this);
        translationTypesManager = new FeatureSet(TRANSLATION_TYPE_RANGE, this);
        annotationTypesManager = new FeatureSet(ANNOTATION_TYPE_RANGE, this);
        grammaticalInfoManager = new FeatureSet(GRAMMATICAL_INFO_RANGE, this);
        builtIn = Set.of(noteTypesManager, relationTypesManager, inverseTypesManager,
            etymologyTypesManager, translationTypesManager, grammaticalInfoManager,
            annotationTypesManager);
        for (FeatureSet fs : new FeatureSet[] {noteTypesManager, relationTypesManager,
            inverseTypesManager, etymologyTypesManager, translationTypesManager,
            grammaticalInfoManager, annotationTypesManager}) {
            featureSetsMap.put(fs.getId(), fs);
            featureSets.add(fs);
        }
    }

    /** The dictionary this header belongs to, or {@code null} for a header on its own. */
    LiftDictionary getDictionary() {
        return dictionary;
    }

    /** Set by the dictionary that creates this header, through ModelInternals. */
    void setDictionary(LiftDictionary dictionary) {
        if (this.dictionary != null && this.dictionary != dictionary) {
            throw new IllegalStateException("This header already belongs to a dictionary.");
        }
        this.dictionary = dictionary;
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
     * @throws IllegalArgumentException if a feature set with the same ID already exists,
     *         or if {@code fs} was created for another header
     */
    public void addFeatureSet(FeatureSet fs) {
        if (hasFeatureSet(fs.getId()))
            throw new IllegalArgumentException("duplicate feature set: " + fs.getId());
        if (fs.getParentNode() != this)
            throw new IllegalArgumentException("This feature set belongs to another header: " + fs.getId());
        featureSetsMap.put(fs.getId(), fs);
        featureSets.add(fs);
        LiftDictionary observer = Changes.observing(this);
        if (observer != null) {
            Changes.fire(observer, new FeatureSetAdded(observer, this, fs));
        }
    }

    /**
     * Removes a feature set, with its features, from this header.
     *
     * The built-in feature sets (the types of notes, relations... and the parts of
     * speech) cannot be removed, nor a feature set a field or trait definition uses as
     * its range.
     *
     * @param fs a feature set of this header
     * @throws IllegalArgumentException if {@code fs} is not in this header, or is built in
     * @throws IllegalStateException if a definition uses it
     */
    public void removeFeatureSet(FeatureSet fs) {
        if (fs == null || featureSetsMap.get(fs.getId()) != fs)
            throw new IllegalArgumentException("This feature set is not in this header.");
        if (builtIn.contains(fs))
            throw new IllegalArgumentException("A built-in feature set cannot be removed: " + fs.getId());
        for (LiftFieldAndTraitDefinition d : definitions) {
            if (d.getResolvedFeatureSet().orElse(null) == fs) {
                throw new IllegalStateException(
                    "The feature set " + fs.getId() + " is the range of the definition " + d.getName());
            }
        }
        featureSetsMap.remove(fs.getId());
        featureSets.remove(fs);
        LiftDictionary observer = Changes.observing(this);
        if (observer != null) {
            Changes.fire(observer, new FeatureSetRemoved(observer, this, fs));
        }
    }

    /**
     * Returns the feature sets of this header, in the order they were added.
     *
     * @return a read-only observable list
     */
    public ObservableList<FeatureSet> getFeatureSets() {
        return featureSetsView;
    }

    // ------------------------------------------------------------------------
    // Field and list definitions
    // ------------------------------------------------------------------------

    /**
     * Creates a definition with the given name and a {@link
     * LiftFieldAndTraitDefinitionKind#UNKNOWN} kind, ie available
     * for Field as well as for Trait, and adds it to this header.
     *
     * @param name the name of the definition to create
     * @return the created definition
     * @throws IllegalArgumentException if a definition with that name exists
     */
    public LiftFieldAndTraitDefinition createUnknownDefinition(String name) {
        LiftFieldAndTraitDefinition fd = new LiftFieldAndTraitDefinition(name, this);
        addFieldOrTraitDefinition(fd);
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
     * Adds a definition, created for this header, to it: the way back after
     * {@link #removeFieldOrTraitDefinition}.
     *
     * @param fd the definition
     * @throws IllegalArgumentException if a definition with that name exists, or if
     *         {@code fd} was created for another header
     */
    public void addFieldOrTraitDefinition(LiftFieldAndTraitDefinition fd) {
        if (fd.getParentNode() != this)
            throw new IllegalArgumentException("This definition belongs to another header: " + fd.getName());
        if (fieldsAndTraitsDefinition.containsKey(fd.getName()))
            throw new IllegalArgumentException("Duplicate field or trait definition: " + fd.getName());
        fieldsAndTraitsDefinition.put(fd.getName(), fd);
        definitions.add(fd);
        LiftDictionary observer = Changes.observing(this);
        if (observer != null) {
            Changes.fire(observer, new DefinitionAdded(observer, this, fd));
        }
    }

    /**
     * Removes a definition from this header.
     *
     * Refused while a field or a trait of the dictionary uses it, as the deletion of a
     * referenced entry is: they would be left without a definition.
     *
     * @param fd a definition of this header
     * @throws IllegalArgumentException if {@code fd} is not in this header
     * @throws IllegalStateException if a field or a trait uses it
     */
    public void removeFieldOrTraitDefinition(LiftFieldAndTraitDefinition fd) {
        if (fd == null || fieldsAndTraitsDefinition.get(fd.getName()) != fd)
            throw new IllegalArgumentException("This definition is not in this header.");
        if (isUsed(fd)) {
            throw new IllegalStateException(
                "The definition " + fd.getName() + " is used by a field or a trait.");
        }
        fieldsAndTraitsDefinition.remove(fd.getName());
        definitions.remove(fd);
        LiftDictionary observer = Changes.observing(this);
        if (observer != null) {
            Changes.fire(observer, new DefinitionRemoved(observer, this, fd));
        }
    }

    /**
     * Whether a field or a trait of the dictionary uses {@code fd}.
     */
    boolean isUsed(LiftFieldAndTraitDefinition fd) {
        if (dictionary == null) {
            return false;
        }
        var registry = dictionary.getLiftDictionaryRegistry();
        return registry.getTraits().stream().anyMatch(t -> t.getSpecification() == fd)
            || registry.getFields().stream().anyMatch(f -> f.getSpecification() == fd);
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
     * Returns all the field and trait definitions.
     *
     * @return a read-only observable list; change it with
     *         {@link #createUnknownDefinition}, {@link #addFieldOrTraitDefinition} and
     *         {@link #removeFieldOrTraitDefinition}
     */
    public ObservableList<LiftFieldAndTraitDefinition> getFieldsAndTraitsDefinitions() {
        return definitionsView;
    }

    /**
     * Returns the field and trait definitions for the given target.
     *
     * The list is live: it follows the definitions added and removed, and the changes of
     * their targets and kind.
     *
     * @param target the target to filter by
     * @return a read-only filtered view
     */
    public ObservableList<LiftFieldAndTraitDefinition> getFieldsAndTraitsDefinitionsFor(LiftFieldAndTraitDefinitionTarget target) {
        return new FilteredList<>(definitionsView, fd -> fd.getTargets().contains(target));
    }

    /**
     * Returns the field definitions, as a live read-only view.
     */
    public ObservableList<LiftFieldAndTraitDefinition> getFieldsDefinitions() {
        return new FilteredList<>(definitionsView,
            fd -> fd.getKind() == LiftFieldAndTraitDefinitionKind.FIELD);
    }

    /**
     * Returns the field definitions for the given target, as a live read-only view.
     */
    public ObservableList<LiftFieldAndTraitDefinition> getFieldsDefinitionsFor(LiftFieldAndTraitDefinitionTarget target) {
        return new FilteredList<>(definitionsView,
            fd -> fd.getKind() == LiftFieldAndTraitDefinitionKind.FIELD && fd.getTargets().contains(target));
    }

    /**
     * Returns the trait definitions, as a live read-only view.
     */
    public ObservableList<LiftFieldAndTraitDefinition> getTraitsDefinitions() {
        return new FilteredList<>(definitionsView,
            fd -> fd.getKind() == LiftFieldAndTraitDefinitionKind.TRAIT);
    }

    /**
     * Returns the trait definitions for the given target, as a live read-only view.
     */
    public ObservableList<LiftFieldAndTraitDefinition> getTraitsDefinitionsFor(LiftFieldAndTraitDefinitionTarget target) {
        return new FilteredList<>(definitionsView,
            fd -> fd.getKind() == LiftFieldAndTraitDefinitionKind.TRAIT && fd.getTargets().contains(target));
    }

    /**
     * Returns the definition with the given name.
     *
     * @param id the name
     * @return the definition
     * @throws IllegalArgumentException if there is none
     */
    public LiftFieldAndTraitDefinition getFieldsAndTraitsDefinitions(String id) {
        if (!fieldsAndTraitsDefinition.containsKey(id)) {
            throw new IllegalArgumentException("No such field or trait definition: " + id);
        }
        return fieldsAndTraitsDefinition.get(id);
    }

    /**
     * Returns the trait definition with the given name, creating it if there is none.
     *
     * @throws IllegalArgumentException if a field definition has that name
     */
    public LiftFieldAndTraitDefinition getOrCreateTraitsDefinitions(String id) {
        LiftFieldAndTraitDefinition existing = fieldsAndTraitsDefinition.get(id);
        if (existing != null && existing.getKind() == LiftFieldAndTraitDefinitionKind.FIELD) {
            throw new IllegalArgumentException("Cannot create a trait definition with this name: a field definition already exists: " + id);
        }
        return existing != null ? existing : createTraitDefinition(id);
    }

    /**
     * Returns the field definition with the given name, creating it if there is none.
     *
     * @throws IllegalArgumentException if a trait definition has that name
     */
    public LiftFieldAndTraitDefinition getOrCreateFieldDefinitions(String id) {
        LiftFieldAndTraitDefinition existing = fieldsAndTraitsDefinition.get(id);
        if (existing != null && existing.getKind() == LiftFieldAndTraitDefinitionKind.TRAIT) {
            throw new IllegalArgumentException("Cannot create a field definition with this name: a trait definition already exists: " + id);
        }
        return existing != null ? existing : createFieldDefinition(id);
    }

    public FeatureSet getGrammaticalInfoManager() {
        return grammaticalInfoManager;
    }

    public FeatureSet getNoteTypeManager() {
        return noteTypesManager;
    }

    public FeatureSet getRelationTypeManager() {
        return relationTypesManager;
    }

    public FeatureSet getInverseTypeManager() {
        return inverseTypesManager;
    }

    public FeatureSet getEtymologyTypeManager() {
        return etymologyTypesManager;
    }

    public FeatureSet getTranslationTypeManager() {
        return translationTypesManager;
    }

    public FeatureSet getAnnotationTypeManager() {
        return annotationTypesManager;
    }

    @Override
    public AbstractLiftRoot getParentNode() {
        return null;
    }
}
