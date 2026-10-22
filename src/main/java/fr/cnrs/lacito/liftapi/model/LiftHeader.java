package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.change.DefinitionAdded;
import fr.cnrs.lacito.liftapi.change.DefinitionRemoved;
import fr.cnrs.lacito.liftapi.change.FeatureSetAdded;
import fr.cnrs.lacito.liftapi.change.FeatureSetRemoved;
import fr.cnrs.lacito.liftapi.internal.DictionaryMutator;
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
 *
 * <h2>What the names of traits and fields refer to</h2>
 *
 * The name of a trait refers to a definition, a range, or nothing
 * ({@link TraitSpecification}); the name of a field to a definition or nothing
 * ({@link FieldSpecification}). {@link #resolveTraitSpecification(String)} and
 * {@link #resolveFieldSpecification(String)} say which, given the header as it is.
 *
 * <b>Every trait's and field's specification is what its name resolves to now.</b> That
 * is what makes the dictionary in memory what a save and a reload give: the writer writes
 * names, the reader resolves them. The operations of the header that change a resolution
 * keep it true:
 * <ul>
 * <li>adding a definition ({@link #addFieldOrTraitDefinition}), adding a feature set
 *     ({@link #addFeatureSet(FeatureSet)}), renaming one ({@link #renameFeatureSet}), or
 *     changing the kind of a definition so that it fits more components
 *     ({@link LiftFieldAndTraitDefinition#setKind}) <i>rebinds</i> the traits and fields
 *     of that name. A trait is replaced by a new one, whose value is the old one parsed
 *     for the new specification (a feature a range lacks is created, as the reader does),
 *     and which takes the old one's annotations and place; a field is rebound in place.
 *     All or nothing: if a value does not parse for the new specification, the operation
 *     throws and changes nothing. The rebinding is reported after the operation, as
 *     changes of its own;</li>
 * <li>removing a definition or a feature set that a name resolves to is refused while
 *     traits or fields of that name exist; so is a kind that would exclude them.</li>
 * </ul>
 *
 * <h2>Feature sets defined in an external file</h2>
 *
 * A {@code <range href="...">} takes its content from a {@code .lift-ranges} file: the set
 * is read-only (see {@link FeatureSet}) until {@link #makeFeatureSetInternal(FeatureSet)}.
 * Rebinding is not refused in such a set: it changes what the names of traits refer to,
 * not what the file says.
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
     * How many callers asked not to rebind: the reader, until the end of the header (the
     * {@code option-range} of the definitions is set only then), and the reverting and
     * reapplying of changes (the rebinding is reported, and put back, by changes of its
     * own).
     */
    private int rebindingSuspensions;

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

    /**
     * Set by the dictionary that creates this header, through ModelInternals. The texts
     * of the header and of what it already holds (the built-in feature sets) are then
     * registered as meta-language texts of that dictionary.
     */
    void setDictionary(LiftDictionary dictionary) {
        if (this.dictionary != null && this.dictionary != dictionary) {
            throw new IllegalStateException("This header already belongs to a dictionary.");
        }
        this.dictionary = dictionary;
        if (dictionary != null) {
            dictionary.getMutator().registerHeaderTexts(this);
        }
    }

    /** Whether {@code fs} is one of the feature sets of this header. */
    boolean holds(FeatureSet fs) {
        return featureSetsMap.get(fs.getId()) == fs;
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
     * The texts of the feature set and of its features are registered as meta-language
     * texts of the dictionary (see {@code DictionaryMutator.registerHeaderTexts}): their
     * languages are declared, and later forms must be in a declared language.
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
        // The undeclared traits of that name now name a range: their values become features.
        Rebinding rebinding = prepareRebinding(fs.getId(),
            traitSpecificationFor(fs.getId(), fieldsAndTraitsDefinition.get(fs.getId()), fs), null);
        featureSetsMap.put(fs.getId(), fs);
        featureSets.add(fs);
        if (dictionary != null) {
            dictionary.getMutator().registerHeaderTexts(fs);
        }
        LiftDictionary observer = Changes.observing(this);
        if (observer != null) {
            Changes.fire(observer, new FeatureSetAdded(observer, this, fs));
        }
        rebinding.apply();
    }

    /**
     * Removes a feature set, with its features, from this header.
     *
     * The built-in feature sets (the types of notes, relations... and the parts of
     * speech) cannot be removed, nor a feature set in use ({@link #isUsed(FeatureSet)}:
     * the range of a definition, or the name of traits), nor one with a feature in use
     * ({@link #isUsed(Feature)}). What the set
     * itself holds - the hierarchy of its features, the traits of its range elements -
     * goes with it and does not count as a use.
     *
     * @param fs a feature set of this header
     * @throws IllegalArgumentException if {@code fs} is not in this header, or is built in
     * @throws IllegalStateException if a definition or a trait uses it, or one of its
     *         features is in use
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
        if (traitsNamingTheRange(fs, fs).findAny().isPresent()) {
            throw new IllegalStateException(
                "The feature set " + fs.getId() + " is the range of traits named after it.");
        }
        for (Feature f : fs.getFeatures().values()) {
            if (uses(f, fs).findAny().isPresent()) {
                throw new IllegalStateException(
                    "The feature " + f.getId() + " of the feature set " + fs.getId() + " is in use.");
            }
        }
        if (dictionary != null) {
            dictionary.getMutator().releaseHeaderTexts(fs);
        }
        featureSetsMap.remove(fs.getId());
        featureSets.remove(fs);
        LiftDictionary observer = Changes.observing(this);
        if (observer != null) {
            Changes.fire(observer, new FeatureSetRemoved(observer, this, fs));
        }
    }

    /**
     * Renames a feature set of this header.
     *
     * What refers to the set holds the object, not the id, and follows: the definitions
     * taking it as their range (the writer emits their {@code option-range} from it),
     * the components typed by its features, and the traits named after it (a
     * {@link RangeReference}: they are written under the new id). The set keeps its place
     * among the others.
     *
     * The undeclared traits named {@code newId} now name a range, and are rebound to it.
     * Refused if traits are named after the set and a definition named {@code newId}
     * fits traits: on reload, they would take that definition instead.
     *
     * Refused for a built-in feature set: its id is the LIFT range name that a reader
     * maps to the types of notes, relations... or to the parts of speech, and under
     * another name it would no longer be one. Refused too for a set defined in an
     * external ranges file ({@link FeatureSet#isExternallyDefined()}): that file is never
     * written, and names the set by its id; make the set internal first
     * ({@link #makeFeatureSetInternal(FeatureSet)}).
     *
     * Reported as a change of {@code ModelProperty.FEATURE_SET_ID}.
     *
     * @param fs a feature set of this header
     * @param newId its new id
     * @throws IllegalArgumentException if {@code fs} is not in this header, is built in,
     *         or if {@code newId} is blank or already the id of a feature set
     * @throws IllegalStateException if {@code fs} is defined in an external ranges file,
     *         or if traits are named after it and a definition named {@code newId} fits
     *         traits
     */
    public void renameFeatureSet(FeatureSet fs, String newId) {
        if (fs == null || featureSetsMap.get(fs.getId()) != fs)
            throw new IllegalArgumentException("This feature set is not in this header.");
        if (builtIn.contains(fs))
            throw new IllegalArgumentException("A built-in feature set cannot be renamed: " + fs.getId());
        if (newId == null || newId.isBlank())
            throw new IllegalArgumentException("The id of a feature set cannot be blank.");
        if (featureSetsMap.containsKey(newId))
            throw new IllegalArgumentException("Duplicate feature set: " + newId);
        ExternalDefinitionGuard.requireWritable(fs);
        LiftFieldAndTraitDefinition definitionOfTheNewId = fieldsAndTraitsDefinition.get(newId);
        if (definitionOfTheNewId != null && definitionOfTheNewId.fitsTraits()
            && traitsNamingTheRange(fs, null).findAny().isPresent()) {
            throw new IllegalStateException(
                "The feature set " + fs.getId() + " cannot be renamed " + newId + ": the traits named after it "
                    + "would be read back as traits of the definition " + newId + ".");
        }
        Rebinding rebinding = prepareRebinding(newId, traitSpecificationFor(newId, definitionOfTheNewId, fs), null);
        featureSetsMap.remove(fs.getId());
        featureSetsMap.put(newId, fs);
        // Last, so that the change is reported once the set is under its new key.
        fs.setId(newId);
        rebinding.apply();
    }

    /**
     * Makes a feature set defined in an external ranges file internal: its {@code href}
     * is removed, it becomes writable, and the writer writes it in full in the header
     * instead of the reference. It keeps its content, as read from the file (with what the
     * {@code .lift} file declared inline). It no longer follows the external file: if other
     * dictionaries share that file, their changes are not seen any more.
     *
     * Reported as a change of {@code ModelProperty.HREF}; reverting it makes the set
     * external, and read-only, again.
     *
     * @param fs an externally defined feature set of this header
     * @throws IllegalArgumentException if {@code fs} is not in this header, or is not
     *         externally defined
     */
    public void makeFeatureSetInternal(FeatureSet fs) {
        if (fs == null || featureSetsMap.get(fs.getId()) != fs)
            throw new IllegalArgumentException("This feature set is not in this header.");
        if (!fs.isExternallyDefined())
            throw new IllegalArgumentException("The feature set " + fs.getId() + " is not defined in an external file.");
        fs.setHref(null);
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
        return createDefinition(name, LiftFieldAndTraitDefinitionKind.UNKNOWN);
    }

    /** The kind is set before the definition is added, so that it rebinds only what it fits. */
    private LiftFieldAndTraitDefinition createDefinition(String name, LiftFieldAndTraitDefinitionKind kind) {
        LiftFieldAndTraitDefinition fd = new LiftFieldAndTraitDefinition(name, this);
        fd.setKind(kind);
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
        return createDefinition(name, LiftFieldAndTraitDefinitionKind.TRAIT);
    }

    /**
     * Creates a field (a kind of {@link LiftFieldAndTraitDefinitionKind#FIELD}) definition with the given name.
     *
     * @param name the name of the definition to create
     * @return the created definition
     */
    public LiftFieldAndTraitDefinition createFieldDefinition(String name) {
        return createDefinition(name, LiftFieldAndTraitDefinitionKind.FIELD);
    }

    /**
     * Adds a definition, created for this header, to it: the way back after
     * {@link #removeFieldOrTraitDefinition}.
     *
     * The traits (if {@code fd} fits traits) and the fields (if it fits fields) of that
     * name are rebound to it: the traits named after a range, or undeclared, are replaced
     * by traits of {@code fd} whose values are the old ones parsed for its data model.
     *
     * @param fd the definition
     * @throws IllegalArgumentException if a definition with that name exists, if
     *         {@code fd} was created for another header, or if the value of a trait of
     *         that name does not parse for {@code fd}'s data model
     */
    public void addFieldOrTraitDefinition(LiftFieldAndTraitDefinition fd) {
        if (fd.getParentNode() != this)
            throw new IllegalArgumentException("This definition belongs to another header: " + fd.getName());
        if (fieldsAndTraitsDefinition.containsKey(fd.getName()))
            throw new IllegalArgumentException("Duplicate field or trait definition: " + fd.getName());
        Rebinding rebinding = prepareRebinding(fd.getName(),
            traitSpecificationFor(fd.getName(), fd, featureSetsMap.get(fd.getName())),
            fieldSpecificationFor(fd.getName(), fd));
        fieldsAndTraitsDefinition.put(fd.getName(), fd);
        definitions.add(fd);
        if (dictionary != null) {
            dictionary.getMutator().registerHeaderTexts(fd);
        }
        LiftDictionary observer = Changes.observing(this);
        if (observer != null) {
            Changes.fire(observer, new DefinitionAdded(observer, this, fd));
        }
        rebinding.apply();
    }

    /**
     * Removes a definition from this header.
     *
     * Refused while a field or a trait of the dictionary uses it, as the deletion of a
     * referenced entry is: they would be left without a definition. (So no trait or
     * field needs rebinding: those of that name that it does not fit resolve to the same
     * thing without it.)
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
        if (dictionary != null) {
            dictionary.getMutator().releaseHeaderTexts(fd);
        }
        fieldsAndTraitsDefinition.remove(fd.getName());
        definitions.remove(fd);
        LiftDictionary observer = Changes.observing(this);
        if (observer != null) {
            Changes.fire(observer, new DefinitionRemoved(observer, this, fd));
        }
    }

    // --------------------------------------------------------
    // Uses of the header's declarations
    // --------------------------------------------------------

    /**
     * Whether a field or a trait of the dictionary uses {@code fd}: one held by a
     * component of an entry, or by a component of the header (a trait on a range
     * element, say).
     *
     * {@link #removeFieldOrTraitDefinition} refuses a definition in use.
     *
     * @param fd a definition
     * @return {@code false} while this header belongs to no dictionary
     */
    public boolean isUsed(LiftFieldAndTraitDefinition fd) {
        return uses(fd).findAny().isPresent();
    }

    /**
     * The number of fields and traits of the dictionary that use {@code fd}, counted as
     * {@link #isUsed(LiftFieldAndTraitDefinition)} looks for them.
     *
     * @param fd a definition
     * @return the number of uses; 0 while this header belongs to no dictionary
     */
    public long countUses(LiftFieldAndTraitDefinition fd) {
        return uses(fd).count();
    }

    /**
     * Whether something in the dictionary uses {@code f}:
     * <ul>
     * <li>the type of a note, a relation, an annotation, an etymology, a reversal or a
     *     translation;</li>
     * <li>the value of a category (grammatical info);</li>
     * <li>the value of an option trait, alone or in a collection or a sequence;</li>
     * <li>another feature, of any set, whose superordinate it is.</li>
     * </ul>
     * Those held by the components of the header (the traits and annotations of a range
     * or of a range element) count as well as those of the entries.
     *
     * {@link FeatureSet#removeFeature(String)} and {@link #removeFeatureSet} refuse a
     * feature in use: whatever uses it would be left with a feature that no range
     * declares. The answer is found by going through the dictionary, which is fine for a
     * deletion or a dialog.
     *
     * @param f a feature
     * @return {@code false} while this header belongs to no dictionary
     */
    public boolean isUsed(Feature f) {
        return uses(f, null).findAny().isPresent();
    }

    /**
     * The number of uses of {@code f}, counted as {@link #isUsed(Feature)} looks for
     * them: each component using it counts once.
     *
     * @param f a feature
     * @return the number of uses; 0 while this header belongs to no dictionary
     */
    public long countUses(Feature f) {
        return uses(f, null).count();
    }

    /**
     * Whether {@code fs} is in use: the range of a definition, or the range of traits
     * named after it ({@link RangeReference}), even traits with no value.
     *
     * {@link #removeFeatureSet} refuses a feature set in use. That its features are used
     * is another question: see {@link #isUsed(Feature)}.
     *
     * @param fs a feature set
     * @return whether a definition or a trait uses it
     */
    public boolean isUsed(FeatureSet fs) {
        return countUses(fs) > 0;
    }

    /**
     * The number of definitions taking {@code fs} as their range, plus the number of
     * traits named after it.
     *
     * @param fs a feature set
     * @return the number of uses
     */
    public long countUses(FeatureSet fs) {
        long definitionsUsingIt = definitions.stream()
            .filter(d -> d.getResolvedFeatureSet().orElse(null) == fs)
            .count();
        return definitionsUsingIt + traitsNamingTheRange(fs, null).count();
    }

    /** Whether a trait uses {@code fd}: then it cannot become a field definition. */
    boolean isUsedByTraits(LiftFieldAndTraitDefinition fd) {
        return allTraits(null).anyMatch(t -> t.getSpecification() == fd);
    }

    /** Whether a field uses {@code fd}: then it cannot become a trait definition. */
    boolean isUsedByFields(LiftFieldAndTraitDefinition fd) {
        return allFields().anyMatch(f -> f.getSpecification() == fd);
    }

    /** The fields and traits that use {@code fd}. */
    private Stream<AbstractLiftRoot> uses(LiftFieldAndTraitDefinition fd) {
        return Stream.concat(
            allTraits(null).filter(t -> t.getSpecification() == fd),
            allFields().filter(f -> f.getSpecification() == fd));
    }

    /** The traits named after {@code fs}, leaving out those {@code ignoring} holds. */
    private Stream<LiftTrait> traitsNamingTheRange(FeatureSet fs, FeatureSet ignoring) {
        RangeReference reference = new RangeReference(fs);
        return allTraits(ignoring).filter(t -> reference.equals(t.getSpecification()));
    }

    /**
     * Every trait of the dictionary: those of the entries, and those the components of
     * this header hold (leaving out those of {@code ignoring} and its features).
     */
    private Stream<LiftTrait> allTraits(FeatureSet ignoring) {
        Stream<AbstractLiftRoot> ofTheEntries = dictionary == null
            ? Stream.empty()
            : dictionary.getLiftDictionaryRegistry().getTraits().stream().map(AbstractLiftRoot.class::cast);
        return Stream.concat(ofTheEntries, heldByTheHeader(ignoring).stream())
            .filter(LiftTrait.class::isInstance)
            .map(LiftTrait.class::cast);
    }

    /** Every field of the dictionary, those of the entries and those of the header. */
    private Stream<LiftField> allFields() {
        Stream<AbstractLiftRoot> ofTheEntries = dictionary == null
            ? Stream.empty()
            : dictionary.getLiftDictionaryRegistry().getFields().stream().map(AbstractLiftRoot.class::cast);
        return Stream.concat(ofTheEntries, heldByTheHeader(null).stream())
            .filter(LiftField.class::isInstance)
            .map(LiftField.class::cast);
    }

    /**
     * What uses {@code f}, leaving out what the set {@code ignoring} and its features
     * hold, and the hierarchy inside that set: when a whole set is removed, those go
     * with it.
     */
    private Stream<Object> uses(Feature f, FeatureSet ignoring) {
        if (dictionary == null) {
            return Stream.empty();
        }
        var registry = dictionary.getLiftDictionaryRegistry();
        Stream<Object> typed = Stream.<List<? extends HasType>>of(
                registry.getNotes(), registry.getRelations(), registry.getAnnotations(),
                registry.getEtymologies(), registry.getReversals(), registry.getTranslations())
            .flatMap(List::stream)
            .filter(n -> n.getType() == f)
            .map(Object.class::cast);
        Stream<Object> categories = registry.getGrammaticalInfos().stream()
            .filter(g -> g.getGramInfoValue() == f)
            .map(Object.class::cast);
        Stream<Object> traits = registry.getTraits().stream()
            .filter(t -> holdsFeature(t, f))
            .map(Object.class::cast);
        Stream<Object> header = heldByTheHeader(ignoring).stream()
            .filter(n -> n instanceof LiftAnnotation a && a.getType() == f
                || n instanceof LiftTrait t && holdsFeature(t, f))
            .map(Object.class::cast);
        Stream<Object> subordinates = featureSets.stream()
            .filter(fs -> fs != ignoring)
            .flatMap(fs -> fs.getFeatures().values().stream())
            .filter(g -> g.getSuperOrdinateFeature().orElse(null) == f)
            .map(Object.class::cast);
        return Stream.of(typed, categories, traits, header, subordinates).flatMap(s -> s);
    }

    /** Whether the value of {@code t} is {@code f}, or a collection holding it. */
    private static boolean holdsFeature(LiftTrait t, Feature f) {
        return switch (t.getSpecification().getDataModel()) {
            case FEATURE -> t.getTypedValue() == f;
            // Feature compares by identity, so contains() asks for this very feature.
            case FEATURE_SET, FEATURE_LIST -> ((Collection<?>) t.getTypedValue()).contains(f);
            default -> false;
        };
    }

    /**
     * The traits, annotations and fields held by the components of this header, at any
     * depth: the header does not register its components in the dictionary's indexes,
     * so the registry does not list them.
     *
     * @param ignoring a feature set whose own holdings, and those of its features, are
     *        left out; {@code null} for none
     */
    private List<AbstractLiftRoot> heldByTheHeader(FeatureSet ignoring) {
        List<AbstractLiftRoot> holders = new ArrayList<>();
        holders.add(this);
        for (FeatureSet fs : featureSets) {
            if (fs != ignoring) {
                holders.add(fs);
                holders.addAll(fs.getFeatures().values());
            }
        }
        holders.addAll(definitions);
        Deque<AbstractLiftRoot> pending = new ArrayDeque<>();
        for (AbstractLiftRoot holder : holders) {
            for (MultiText text : DictionaryMutator.headerTextsOf(holder)) {
                if (text.hasAnnotations()) {
                    pending.addAll(text.getAnnotations());
                }
                for (Form form : text.getForms()) {
                    if (form.hasAnnotations()) {
                        pending.addAll(form.getAnnotations());
                    }
                }
            }
            if (holder instanceof AbstractExtensibleWithoutField a) {
                pending.addAll(a.getAnnotations());
                pending.addAll(a.getTraits());
                if (holder instanceof HasField withFields) {
                    pending.addAll(withFields.getFields().values());
                }
            }
        }
        // Below a trait, an annotation or a field, the components are ordinary ones.
        List<AbstractLiftRoot> held = new ArrayList<>();
        while (!pending.isEmpty()) {
            AbstractLiftRoot node = pending.pop();
            held.add(node);
            pending.addAll(DictionaryMutator.childrenOf(node));
        }
        return held;
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
     * Declares a trait definition of the given name, or returns the definition of that
     * name if there is one.
     *
     * This declares the name: a definition created here is written in the header, and
     * the traits of that name are rebound to it (see
     * {@link #addFieldOrTraitDefinition}). To find what a trait name refers to without
     * declaring it, use {@link #resolveTraitSpecification(String)}.
     *
     * @throws IllegalArgumentException if a field definition has that name
     */
    public LiftFieldAndTraitDefinition declareTraitDefinition(String id) {
        LiftFieldAndTraitDefinition existing = fieldsAndTraitsDefinition.get(id);
        if (existing != null && existing.getKind() == LiftFieldAndTraitDefinitionKind.FIELD) {
            throw new IllegalArgumentException("Cannot create a trait definition with this name: a field definition already exists: " + id);
        }
        return existing != null ? existing : createTraitDefinition(id);
    }

    /**
     * Declares a field definition of the given name, or returns the definition of that
     * name if there is one.
     *
     * This declares the name: a definition created here is written in the header, and
     * the fields of that name are rebound to it. To find what a field name refers to
     * without declaring it, use {@link #resolveFieldSpecification(String)}.
     *
     * @throws IllegalArgumentException if a trait definition has that name
     */
    public LiftFieldAndTraitDefinition declareFieldDefinition(String id) {
        LiftFieldAndTraitDefinition existing = fieldsAndTraitsDefinition.get(id);
        if (existing != null && existing.getKind() == LiftFieldAndTraitDefinitionKind.TRAIT) {
            throw new IllegalArgumentException("Cannot create a field definition with this name: a trait definition already exists: " + id);
        }
        return existing != null ? existing : createFieldDefinition(id);
    }

    // --------------------------------------------------------
    // What the names of traits and fields refer to
    // --------------------------------------------------------

    /**
     * What a trait name refers to, given this header as it is:
     * <ol>
     * <li>the definition of that name, if its kind is not {@code FIELD}: a declaration
     *     says what the values are (possibly the features of another range, or a
     *     collection of them), so it wins over a range of the same name;</li>
     * <li>else the feature set of that id, as a {@link RangeReference}: the values are its
     *     features;</li>
     * <li>else an {@link UndeclaredName}: the values are strings.</li>
     * </ol>
     *
     * Nothing is created: an undeclared name stays undeclared, and is not written in the
     * header. The trait builders take their specification from here.
     *
     * @param name the name of a trait
     * @return what it refers to
     */
    public TraitSpecification resolveTraitSpecification(String name) {
        return traitSpecificationFor(name, fieldsAndTraitsDefinition.get(name), featureSetsMap.get(name));
    }

    /**
     * What a field name refers to: the definition of that name if its kind is not
     * {@code TRAIT}, else an {@link UndeclaredName}. A field's value is a text, so a field
     * name never refers to a range.
     *
     * @param name the name of a field
     * @return what it refers to
     */
    public FieldSpecification resolveFieldSpecification(String name) {
        return fieldSpecificationFor(name, fieldsAndTraitsDefinition.get(name));
    }

    /** The resolution of a trait name, given the definition and the range of that name. */
    private static TraitSpecification traitSpecificationFor(
        String name,
        LiftFieldAndTraitDefinition definitionOfTheName,
        FeatureSet rangeOfTheName
    ) {
        if (definitionOfTheName != null && definitionOfTheName.fitsTraits()) {
            return definitionOfTheName;
        }
        if (rangeOfTheName != null) {
            return new RangeReference(rangeOfTheName);
        }
        return new UndeclaredName(name);
    }

    /** The resolution of a field name, given the definition of that name. */
    private static FieldSpecification fieldSpecificationFor(
        String name,
        LiftFieldAndTraitDefinition definitionOfTheName
    ) {
        if (definitionOfTheName != null && definitionOfTheName.fitsFields()) {
            return definitionOfTheName;
        }
        return new UndeclaredName(name);
    }

    /** Whether {@code fd} is the definition of its name in this header. */
    boolean holdsDefinition(LiftFieldAndTraitDefinition fd) {
        return fieldsAndTraitsDefinition.get(fd.getName()) == fd;
    }

    /**
     * Prepares the rebinding that {@code fd} taking the kind {@code newKind} implies: its
     * traits and fields are those it fits once it has that kind. Called by
     * {@link LiftFieldAndTraitDefinition#setKind} before it changes the kind; the kinds
     * that would exclude the traits or fields using it are refused before.
     */
    Rebinding prepareRebindingForKind(LiftFieldAndTraitDefinition fd, LiftFieldAndTraitDefinitionKind newKind) {
        String name = fd.getName();
        TraitSpecification traitTarget = newKind != LiftFieldAndTraitDefinitionKind.FIELD
            ? fd
            : traitSpecificationFor(name, null, featureSetsMap.get(name));
        FieldSpecification fieldTarget = newKind != LiftFieldAndTraitDefinitionKind.TRAIT
            ? fd
            : new UndeclaredName(name);
        return prepareRebinding(name, traitTarget, fieldTarget);
    }

    /**
     * Runs {@code operation} without rebinding: how the changes of the header are
     * reverted and reapplied (the rebinding they caused is reported after them, and put
     * back by its own changes). Reached through ModelInternals.
     */
    void withoutRebinding(Runnable operation) {
        rebindingSuspensions++;
        try {
            operation.run();
        } finally {
            rebindingSuspensions--;
        }
    }

    /** Called by the reader at the start of the header, through ModelInternals. */
    void suspendRebinding() {
        rebindingSuspensions++;
    }

    /**
     * Called by the reader at the end of the header, through ModelInternals: the traits
     * and fields held by the header's components were built while the definitions were
     * not all read, nor their ranges set. Each is rebound to what its name resolves to
     * now. (The entries come after the header: they resolve against the whole of it.)
     */
    void resumeRebinding() {
        rebindingSuspensions--;
        if (rebindingSuspensions > 0) {
            return;
        }
        Set<String> names = new LinkedHashSet<>();
        allTraits(null).forEach(t -> names.add(t.getName()));
        allFields().forEach(f -> names.add(f.getName()));
        for (String name : names) {
            prepareRebinding(name, resolveTraitSpecification(name), resolveFieldSpecification(name)).apply();
        }
    }

    /**
     * The traits and fields named {@code name} whose specification is not the target,
     * checked to be rebindable: nothing changes until {@link Rebinding#apply()}.
     *
     * @param traitTarget the specification the traits of that name are to have
     * @param fieldTarget the specification the fields of that name are to have;
     *        {@code null} to leave the fields alone
     * @throws IllegalArgumentException if the value of a trait does not parse for
     *         {@code traitTarget}
     */
    private Rebinding prepareRebinding(String name, TraitSpecification traitTarget, FieldSpecification fieldTarget) {
        if (rebindingSuspensions > 0) {
            return Rebinding.NONE;
        }
        List<LiftTrait> traitsToReplace = allTraits(null)
            .filter(t -> t.getName().equals(name) && !t.getSpecification().equals(traitTarget))
            .toList();
        for (LiftTrait trait : traitsToReplace) {
            LiftTrait.checkParsable(traitTarget, trait.getValueOrNull());
        }
        List<LiftField> fieldsToRebind = fieldTarget == null
            ? List.of()
            : allFields()
                .filter(f -> f.getName().equals(name) && !f.getSpecification().equals(fieldTarget))
                .toList();
        if (traitsToReplace.isEmpty() && fieldsToRebind.isEmpty()) {
            return Rebinding.NONE;
        }
        // Not refused in a feature set defined in an external file: the XML of the traits
        // and fields does not change, only what their name refers to.
        return () -> ExternalDefinitionGuard.withoutGuard(() -> {
            for (LiftTrait trait : traitsToReplace) {
                replaceTrait(trait, traitTarget);
            }
            for (LiftField field : fieldsToRebind) {
                field.respecify(fieldTarget);
            }
        });
    }

    /**
     * Replaces {@code old} by a trait of {@code target} at the same place in the same
     * parent, with the old value parsed for {@code target} and the old annotations: a
     * trait's value is held in a property of its data model's kind, so a trait does not
     * change its specification. Reported as the addition of the new trait, the move of
     * the annotations and the removal of the old trait.
     */
    private static void replaceTrait(LiftTrait old, TraitSpecification target) {
        HasTrait parent = old.getParent();
        LiftTrait replacement = new LiftTrait(target);
        String value = old.getValueOrNull();
        if (value != null) {
            replacement.setTypedValue(LiftTrait.parseValue(target, value));
        }
        old.getOtherXmlAttributes().forEach(replacement::putOtherXmlAttribute);
        int index = 0;
        while (parent.getTraits().get(index) != old) {
            index++;
        }
        parent.addTrait(index, replacement);
        for (LiftAnnotation annotation : List.copyOf(old.getAnnotations())) {
            old.deleteAnnotation(annotation);
            replacement.addAnnotation(annotation);
        }
        parent.deleteTrait(old);
    }

    /** A rebinding checked by {@link #prepareRebinding}, applied once its cause is done. */
    @FunctionalInterface
    interface Rebinding {

        Rebinding NONE = () -> {};

        void apply();
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
