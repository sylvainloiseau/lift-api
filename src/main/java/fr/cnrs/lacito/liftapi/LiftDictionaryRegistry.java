package fr.cnrs.lacito.liftapi;

import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithoutField;
import fr.cnrs.lacito.liftapi.model.AbstractIdentifiable;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.DuplicateIdException;
import fr.cnrs.lacito.liftapi.model.HasField;
import fr.cnrs.lacito.liftapi.model.HasNote;
import fr.cnrs.lacito.liftapi.model.HasPronunciation;
import fr.cnrs.lacito.liftapi.model.HasRefId;
import fr.cnrs.lacito.liftapi.model.HasRelations;
import fr.cnrs.lacito.liftapi.model.HasReversal;
import fr.cnrs.lacito.liftapi.model.HasSense;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftIllustration;
import fr.cnrs.lacito.liftapi.model.LiftMedia;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftReversal;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.LiftVariant;
import fr.cnrs.lacito.liftapi.model.MultiText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

///
///
/// Offer two main functionalities:
///
/// - unmodifiable collections for all the components of a LIFT dictionary [getEntries(), getSenses(), getExamples(), ...]
///
/// - function for removing components from the dictionary [removeFromDictionary(AbstractLiftRoot node)]
///   - Adding nodes to dictionary should be made using the ComponentBuilder API (see LiftDictionary#getComponentBuilder())
///
///
public class LiftDictionaryRegistry {

    private final LiftDictionaryFeatureManager counter;
    private final LiftDictionaryUUIDManager uuidManager =
        new LiftDictionaryUUIDManager();

    private LiftDictionaryLanguagesManager objectLanguagesManager;
    private LiftDictionaryLanguagesManager metaLanguagesManager;

    /**
     * Map from IDs (of referenced object) to objects pointing at them in the dictionary.
     */
    public Map<String, List<HasRefId>> refId2HasRefIdList = new HashMap<>();

    protected final ObservableMap<String, LiftEntry> entriesByLiftId =
        FXCollections.observableHashMap();

    protected final ObservableMap<String, LiftSense> sensesByLiftId =
        FXCollections.observableHashMap();

    protected Map<String, UUID> entryLiftId2Uuid = new HashMap<>(200);

    protected Map<String, UUID> senseLiftId2Uuid = new HashMap<>(200);

    protected final ObservableMap<UUID, LiftEntry> entriesById =
        FXCollections.observableHashMap();

    private final ObservableMap<UUID, LiftSense> sensesById =
        FXCollections.observableHashMap();

    protected final ObservableMap<UUID, LiftExample> examplesById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftVariant> variantsById =
        FXCollections.observableHashMap();
    protected final ObservableMap<UUID, LiftTrait> traitsById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftReversal> reversalsById =
        FXCollections.observableHashMap();
    protected final ObservableMap<UUID, LiftRelation> relationsById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftPronunciation> pronunciationsById =
        FXCollections.observableHashMap();
    protected final ObservableMap<UUID, LiftNote> notesById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftMedia> mediasById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftIllustration> illustrationsById =
        FXCollections.observableHashMap();
    protected final ObservableMap<UUID, LiftField> fieldsById =
        FXCollections.observableHashMap();
    protected final ObservableMap<UUID, LiftEtymology> etymologiesById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftAnnotation> annotationsById =
        FXCollections.observableHashMap();
    protected final ObservableMap<UUID, MultiText> objectTextById =
        FXCollections.observableHashMap();
    protected final ObservableMap<UUID, MultiText> metaTextById =
        FXCollections.observableHashMap();

    /**
     * The ID on a {@link HasRefId} component may point towards an entry or a sense.
     */
    public AbstractIdentifiable getEntryOrSenseByLiftId(String liftId) {
        boolean inEntries = entriesByLiftId.containsKey(liftId);
        boolean inSenses = sensesByLiftId.containsKey(liftId);
        if (inEntries && inSenses) {
            throw new IllegalStateException("Cannot have the same liftId for a sense and an entries");
        }
        if (inEntries) {
            return entriesByLiftId.get(liftId);
        } else if (inSenses) {
            return sensesByLiftId.get(liftId);
        } else {
            return null;
        }
    }

    private ObservableList<LiftEntry> entries = FXCollections.observableArrayList();
    private ObservableList<LiftEntry> entriesReadOnly = FXCollections.unmodifiableObservableList(entries);

    public ObservableList<LiftEntry> getEntries() {
        // In the particular case of entries, we
        // do not use an list listining to the xById map, instead we register the entry
        // directlyf into the list in order to keep the orders of the entries in the dictionary.
        return entriesReadOnly;
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------

    Map<
      Class< ? extends AbstractLiftRoot>,
      ReadOnlyListWrapper<? extends AbstractLiftRoot>
    > observableList = new HashMap<>();

    @SuppressWarnings("unchecked")
    public ObservableList<LiftSense> getSenses() {
        if (!observableList.containsKey(LiftSense.class)) {
            this.<LiftSense>populateObservableList(LiftSense.class, sensesById);
        }
        return (ObservableList<LiftSense>) observableList.get(LiftSense.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftExample> getExamples() {
        if (!observableList.containsKey(LiftExample.class)) {
            this.<LiftExample>populateObservableList(LiftExample.class, examplesById);
        }
        return (ObservableList<LiftExample>) observableList.get(LiftExample.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftVariant> getVariants() {
        if (!observableList.containsKey(LiftVariant.class)) {
            this.<LiftVariant>populateObservableList(LiftVariant.class, variantsById);
        }
        return (ObservableList<LiftVariant>) observableList.get(LiftVariant.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftTrait> getTraits() {
        if (!observableList.containsKey(LiftTrait.class)) {
            this.<LiftTrait>populateObservableList(LiftTrait.class, traitsById);
        }
        return (ObservableList<LiftTrait>) observableList.get(LiftTrait.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftReversal> getReversals() {
        if (!observableList.containsKey(LiftReversal.class)) {
            this.<LiftReversal>populateObservableList(LiftReversal.class, reversalsById);
        }
        return (ObservableList<LiftReversal>) observableList.get(LiftReversal.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftRelation> getRelations() {
        if (!observableList.containsKey(LiftRelation.class)) {
            this.<LiftRelation>populateObservableList(LiftRelation.class, relationsById);
        }
        return (ObservableList<LiftRelation>) observableList.get(LiftRelation.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftPronunciation> getPronunciations() {
        if (!observableList.containsKey(LiftPronunciation.class)) {
            this.<LiftPronunciation>populateObservableList(LiftPronunciation.class, pronunciationsById);
        }
        return (ObservableList<LiftPronunciation>) observableList.get(LiftPronunciation.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftNote> getNotes() {
        if (!observableList.containsKey(LiftNote.class)) {
            this.<LiftNote>populateObservableList(LiftNote.class, notesById);
        }
        return (ObservableList<LiftNote>) observableList.get(LiftNote.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftMedia> getMedias() {
        if (!observableList.containsKey(LiftMedia.class)) {
            this.<LiftMedia>populateObservableList(LiftMedia.class, mediasById);
        }
        return (ObservableList<LiftMedia>) observableList.get(LiftMedia.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftIllustration> getIllustrations() {
        if (!observableList.containsKey(LiftIllustration.class)) {
            this.<LiftIllustration>populateObservableList(LiftIllustration.class, illustrationsById);
        }
        return (ObservableList<LiftIllustration>) observableList.get(LiftIllustration.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftField> getFields() {
        if (!observableList.containsKey(LiftField.class)) {
            this.<LiftField>populateObservableList(LiftField.class, fieldsById);
        }
        return (ObservableList<LiftField>) observableList.get(LiftField.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftEtymology> getEtymologies() {
        if (!observableList.containsKey(LiftEtymology.class)) {
            this.<LiftEtymology>populateObservableList(LiftEtymology.class, etymologiesById);
        }
        return (ObservableList<LiftEtymology>) observableList.get(LiftEtymology.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftAnnotation> getAnnotations() {
        if (!observableList.containsKey(LiftAnnotation.class)) {
            this.<LiftAnnotation>populateObservableList(LiftAnnotation.class, annotationsById);
        }
        return (ObservableList<LiftAnnotation>) observableList.get(LiftAnnotation.class).getReadOnlyProperty();
    }

    private <T extends AbstractLiftRoot> void populateObservableList(
        Class<T> clazz,
        ObservableMap<UUID, T> map) {
            ReadOnlyListWrapper<T> x = new ReadOnlyListWrapper<>(FXCollections.observableArrayList(map.values()));
            map.addListener(
                (MapChangeListener<UUID, T>) change -> {
                    if (change.wasAdded()) {
                        x.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        x.remove(change.getValueRemoved());
                    }
                }
            );

            observableList.put(clazz, x);
    }

// ---------
// ---------
// ---------

    private ObservableList<MultiText> objectText = null;
    private ObservableList<MultiText> objectTextReadOnly = null;

    public ObservableList<MultiText> getObjectText() {
        if (objectText == null) {
            objectText = FXCollections.observableList(
                FXCollections.observableArrayList(
                    objectTextById.values()
                )
            );
            objectTextById.addListener(
                (MapChangeListener<UUID, MultiText>) change -> {
                    if (change.wasAdded()) {
                        objectText.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        objectText.remove(change.getValueRemoved());
                    }
                }
            );
        }
        objectTextReadOnly = FXCollections.unmodifiableObservableList(objectText);
        return objectTextReadOnly;
    }

    private ObservableList<MultiText> metaText = null;
    private ObservableList<MultiText> metaTextReadOnly = null;

    public ObservableList<MultiText> getMetaText() {
        if (metaText == null) {
            metaText = FXCollections.observableList(
                FXCollections.observableArrayList(
                    metaTextById.values()
                )
            );
            metaTextById.addListener(
                (MapChangeListener<UUID, MultiText>) change -> {
                    if (change.wasAdded()) {
                        metaText.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        metaText.remove(change.getValueRemoved());
                    }
                }
            );
        }
        metaTextReadOnly = FXCollections.unmodifiableObservableList(metaText);
        return metaTextReadOnly;
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------

    public Map<UUID, LiftEntry> getEntriesById() {
        return entriesById;
    }

    protected LiftDictionaryRegistry() {
        //initializeReadOnlyLists();

        //Type t = LiftEntry.class;
        //@SuppressWarnings("unchecked")
        //ObservableList<LiftEntry> entries = (ObservableList<
        //    LiftEntry
        //>) getType2Collection(t);

        // should be created at the end of the initialisation of objects of constructor
        // because rely on other object (such as metaLanguagesProperty)
        counter = new LiftDictionaryFeatureManager(this);
    }

    public LiftDictionaryFeatureManager getCounter() {
        return counter;
    }

    public void addToDictionaryLowLevel(LiftEntry e, int index) {
        if (index > entries.size()) throw new IllegalArgumentException("Index is greater than array size (" + index + ", " + entries.size() + ").");
        addToDictionaryLowLevel(e);
        // TODO ugly hack...
        entries.removeLast();
        entries.add(index, e);
    }

    /**
     * Add a node (and its descendants) to the directory using the low-level
     * API. This interface is intended for :
     *
     * - unmarshalling efficiently the dictionary.
     * - inserting into the dictionary a node you haven't created (undoing a suppression, moving a node from a parent to another, etc.)
     *
     * If you are creating a node from scratch, the high-level (fluent) API ({@link
     * LiftDictionary#getComponentBuilder()}) should be preferred.
     *
     * The relation parent / child is not manager here:
     * <ul>
     * <li> if you use addToDictionaryLowLevel
     * for inserting a LiftExemple into the dictionary, in addition to calling this method,
     * you still have to set the parent ({@link LiftExample#setParent}) of this
     * example and add this example to its parent ({@link LiftSense#addExample(LiftExample)}).</li>
     * <li> In the case of a LiftEntry, though, nothing more need to be done.</li>
     * </ul>
     *
     * All subnodes of the node (added with addX method, such as {@link
     * LiftEntry#addSense(LiftSense sense)}) will also be added to the dictionary.
     */
    public void addToDictionaryLowLevel(AbstractLiftRoot node) {
        // 1. Add the node to the register:
        // register the node and it(s) multiText(s) in the registry
        register(node);

        // 2. recursively add its descendants
        if (node instanceof LiftEntry e) {
            e.getVariants().forEach(x -> addToDictionaryLowLevel(x));
            e.getEtymologies().forEach(x -> addToDictionaryLowLevel(x));
        }

        if (node instanceof LiftSense s) {
            s.getExamples().forEach(x -> addToDictionaryLowLevel(x));
            s.getIllustrations().forEach(x -> addToDictionaryLowLevel(x));
            s.getReversals().forEach(x -> addToDictionaryLowLevel(x));
        }

        if (node instanceof AbstractExtensibleWithoutField a) {
            a.getAnnotations().forEach(x -> addToDictionaryLowLevel(x));
            a.getTraits().forEach(x -> addToDictionaryLowLevel(x));
            if (node instanceof HasField b) {
                b.getFields().values().forEach(x -> addToDictionaryLowLevel(x));
            }
        }

        if (node instanceof HasNote n) {
            n.getNotes()
                .values()
                .forEach(x -> addToDictionaryLowLevel(x));
        }

        if (node instanceof HasPronunciation n) {
            n.getPronunciations().forEach(x -> addToDictionaryLowLevel(x));
        }

        if (node instanceof HasRelations n) {
            n.getRelations().forEach(x -> addToDictionaryLowLevel(x));
        }

        if (node instanceof HasReversal r) {
            r.getReversals().forEach(x -> addToDictionaryLowLevel(x));
        }

        if (node instanceof HasSense s) {
            s.getSenses().forEach(x -> addToDictionaryLowLevel(x));
        }
    }

    /**
     * Non-recursively add the node. Should not be called directly: use the
     * fluent API instead ({@link LiftDictionary#getComponentBuilder()}).
     *
     * Register the node in the dictionary :
     *
     * <ul>
     * <li>Add a UUID to the node</li>
     * <li>Register the maping (node, UUID) in collections used internally</li>
     * <li>Increment a counter for component referenced from another ones (LiftRelation, etc. : see HasRef).</li>
     * <li>Add a LIFT ID to the node (for entry and sense) if it doesn't have one.</li>
     * </ul>
     *
     * @throws IllegalArgumentException if the node already as an UUID
     * @throws IllegalArgumentException (TODO) if the node is not a LiftEntry and has no parent
     */
    public void register(AbstractLiftRoot node) {
        if (node.getUUID() != null) {
            throw new IllegalArgumentException(
                "This node seems to have already been registered in a dictionary."
            );
        }
        UUID uuid = getNewUUID();
        node.setUUID(uuid);

        // TODO : put getParent in AbstractLiftRoot
        // if (!(node instanceof LiftEntry)  && node.getParent() == null) {
        //     throw new IllegalArgumentException(
        //         "Node should have a parent"
        //     );
        // }

        switch (node) {
            case LiftEntry e -> {
                entriesById.put(e.getUUID(), e);
                if (e.getId().isEmpty()) {
                    String uuidS = e.getUUID().toString();
                    e.setId(uuidS);
                }
                if (entriesByLiftId.containsKey(e.getId().get())) {
                    throw new DuplicateIdException(
                        "Duplicate lift id: " + e.getId().get()
                    );
                }
                entriesByLiftId.put(e.getId().get(), e);
                entryLiftId2Uuid.put(e.getId().get(), e.getUUID());
                entries.add(e);
            }
            case LiftSense s -> {
                sensesById.put(s.getUUID(), s);
                if (s.getId().isEmpty()) {
                    String uuidS = s.getUUID().toString();
                    s.setId(uuidS);
                }
                sensesByLiftId.put(s.getId().get(), s);
                senseLiftId2Uuid.put(s.getId().get(), s.getUUID());
            }
            case LiftExample o -> { examplesById.put(o.getUUID(), o) ; }
            case LiftVariant o -> { variantsById.put(o.getUUID(), o); }
            case LiftTrait o -> { traitsById.put(o.getUUID(), o); }
            case LiftReversal o -> { reversalsById.put(o.getUUID(), o); }
            case LiftRelation o -> { relationsById.put(o.getUUID(), o); }
            case LiftPronunciation o -> { pronunciationsById.put(o.getUUID(), o); }
            case LiftNote o -> { notesById.put(o.getUUID(), o); }
            case LiftMedia o -> { mediasById.put(o.getUUID(), o); }
            case LiftIllustration o -> { illustrationsById.put(o.getUUID(), o); }
            case LiftField o -> { fieldsById.put(o.getUUID(), o); }
            case LiftEtymology o -> { etymologiesById.put(o.getUUID(), o); }
            case LiftAnnotation o -> { annotationsById.put(o.getUUID(), o); }
            default -> throw new IllegalStateException(
                "Unknown type: " + node.getClass()
            );
        }
        switch (node) {
            case LiftEntry e -> {
                registerObjectMultiText(e.getMainMultiText());
            }
            case LiftSense s -> {
                registerMetaMultiText(s.getMainMultiText());
                registerMetaMultiText(s.getDefinition());
            }
            case LiftExample e -> {
                registerObjectMultiText(e.getExample());
                e.getTranslations()
                    .values()
                    .forEach(x -> registerMetaMultiText(x));
            }
            case LiftVariant v -> {
                registerObjectMultiText(v.getForms());
            }
            case LiftTrait _ -> {
            }
            case LiftReversal v -> {
                registerObjectMultiText(v.getForms());
            }
            case LiftRelation r -> {
                registerMetaMultiText(r.getUsage());
            }
            case LiftPronunciation p -> {
                registerObjectMultiText(p.getPronunciation());
            }
            case LiftNote n -> {
                registerMetaMultiText(n.getText());
            }
            case LiftMedia m -> {
                registerMetaMultiText(m.getLabel());
            }
            case LiftIllustration i -> {
                registerMetaMultiText(i.getLabel());
            }
            case LiftField f -> {
                registerMetaMultiText(f.getText());
            }
            case LiftEtymology e -> {
                registerObjectMultiText(e.getForms());
            }
            case LiftAnnotation a -> {
                registerMetaMultiText(a.getText());
            }
            default -> throw new IllegalStateException(
                "Unknown type: " + node.getClass()
            );
        }

        if (node instanceof HasRefId hasref) {
            String target = null;

            // TODO : which is available here, depending on high/low level ?
            // Try to avoid the test
            if (hasref.getRefId().isPresent() )
                target = hasref.getRefId().get();
            else if (hasref.getRefObject() != null && hasref.getRefObject().getId().isPresent())
                target = hasref.getRefObject().getId().get();

            if (target != null && !target.trim().isEmpty()) {
                if (! refId2HasRefIdList.containsKey(target)) {
                    refId2HasRefIdList.put(target, new ArrayList<HasRefId>());
                }
                refId2HasRefIdList.get(target).add(hasref);
            }
        }
    }

    public void registerObjectMultiText(MultiText element) {
        if (element.getUUID() != null) {
            throw new IllegalArgumentException("UUID already set");
        }
        UUID uuid = getNewUUID();
        element.setUUID(uuid);
        objectTextById.put(uuid, element);
        element.setLanguagesManager(objectLanguagesManager);
    }

    public void registerMetaMultiText(MultiText element) {
        if (element.getUUID() != null) {
            throw new IllegalArgumentException("UUID already set");
        }
        UUID uuid = getNewUUID();
        element.setUUID(uuid);
        metaTextById.put(uuid, element);
        element.setLanguagesManager(metaLanguagesManager);
    }

    /**
     * Remove a node from the registries, remove its UUID.
     */
    protected void unregister(AbstractLiftRoot node) {
        Map<UUID, ? extends AbstractLiftRoot> map = null;
        switch (node) {
            case LiftEntry _ ->  map = entriesById;
            case LiftSense _ ->  map = sensesById;
            case LiftExample _ ->  map = examplesById;
            case LiftVariant _ ->  map = variantsById;
            case LiftTrait _ ->  map = traitsById;
            case LiftReversal _ ->  map = reversalsById;
            case LiftRelation _ ->  map = relationsById;
            case LiftPronunciation _ ->  map = pronunciationsById;
            case LiftNote _ ->  map = notesById;
            case LiftMedia _ ->  map = mediasById;
            case LiftIllustration _ ->  map = illustrationsById;
            case LiftField _ ->  map = fieldsById;
            case LiftEtymology _ ->  map = etymologiesById;
            case LiftAnnotation _ ->  map = annotationsById;
            default -> throw new IllegalStateException(
                "Unknown type: " + node.getClass()
            );
        }
        if (!map.containsKey(node.getUUID())) {
            throw new IllegalArgumentException(
                "Entry not found in registry: " + node.getUUID()
            );
        }
        map.remove(node.getUUID());

        node.setUUID(null);

        if (node instanceof AbstractIdentifiable identifiable) {
            String liftId = identifiable.getId().get();
            switch (identifiable) {
                case LiftEntry _ ->  {
                    entriesByLiftId.remove(liftId);
                    entryLiftId2Uuid.remove(liftId);
                }
                case LiftSense _ ->  {
                    sensesByLiftId.remove(liftId);
                    senseLiftId2Uuid.remove(liftId);
                }
            }
        }

        // TODO inefficient
        if (node instanceof LiftEntry e) {
            entries.removeIf(x -> x == e);
        }

        switch (node) {
            case LiftEntry e -> {
                unregisterObjectMultiText(e.getMainMultiText());
            }
            case LiftSense s -> {
                unregisterMetaMultiText(s.getMainMultiText());
                unregisterMetaMultiText(s.getDefinition());
            }
            case LiftExample e -> {
                unregisterObjectMultiText(e.getExample());
                e.getTranslations()
                    .values()
                    .forEach(x -> unregisterMetaMultiText(x));
            }
            case LiftVariant v -> {
                unregisterObjectMultiText(v.getForms());
            }
            case LiftTrait _ -> {
            }
            case LiftReversal v -> {
                unregisterObjectMultiText(v.getForms());
            }
            case LiftRelation r -> {
                unregisterMetaMultiText(r.getUsage());
            }
            case LiftPronunciation p -> {
                unregisterObjectMultiText(p.getPronunciation());
            }
            case LiftNote n -> {
                unregisterMetaMultiText(n.getText());
            }
            case LiftMedia m -> {
                unregisterMetaMultiText(m.getLabel());
            }
            case LiftIllustration i -> {
                unregisterMetaMultiText(i.getLabel());
            }
            case LiftField f -> {
                unregisterMetaMultiText(f.getText());
            }
            case LiftEtymology e -> {
                unregisterObjectMultiText(e.getForms());
            }
            case LiftAnnotation a -> {
                unregisterMetaMultiText(a.getText());
            }
            default -> throw new IllegalStateException(
                "Unknown type: " + node.getClass()
            );
        }
    }

    protected void unregisterObjectMultiText(MultiText node) {
        objectTextById.remove(node.getUUID());
        node.unregister();
        node.setUUID(null);
        node.setLanguagesManager(null);
    }

    protected void unregisterMetaMultiText(MultiText node) {
        metaTextById.remove(node.getUUID());
        node.unregister();
        node.setUUID(null);
        node.setLanguagesManager(null);
    }

    protected void unregisterRec(AbstractLiftRoot node) {
        // 1. First, manage reference counting
        if (node instanceof LiftRelation r) {
            final String target = r
                .getRefObject().getId()
                .orElseThrow(() ->
                    new IllegalArgumentException("Reference ID is missing")
                );
            refId2HasRefIdList.get(target).removeIf(o -> o == r);
        } else if (node instanceof LiftVariant a) {
            final String target = a
                .getRefObject().getId()
                .orElseThrow(() ->
                    new IllegalArgumentException("Reference ID is missing")
                );
            refId2HasRefIdList.get(target).removeIf(o -> o == a);

        // 2. check that this node is not refered from another node
        } else if (node instanceof AbstractIdentifiable i) {
            final String refId = i
                .getId()
                .orElseThrow(() ->
                    new IllegalArgumentException("Reference ID is missing")
                );
            if (
                refId2HasRefIdList.containsKey(refId) &&
                refId2HasRefIdList.get(refId).size() > 0
            ) {
                throw new IllegalStateException(
                    "Cannot delete this node: it is referenced from other nodes."
                );
            }
        }

        // 3. remove this node from the registers
        unregister(node);

        // 4. recursively unregister its descendants and its multitext
        switch (node) {
            case LiftEntry e -> {
                e.getVariants().forEach(x -> unregisterRec(x));
                e.getEtymologies().forEach(x -> unregisterRec(x));
                unregisterObjectMultiText(node.getMainMultiText());
            }
            case LiftSense s -> {
                s.getExamples().forEach(x -> unregisterRec(x));
                s.getIllustrations().forEach(x -> unregisterRec(x));
                s.getReversals().forEach(x -> unregisterRec(x));
                unregisterMetaMultiText(node.getMainMultiText());
                unregisterMetaMultiText(s.getDefinition());
            }
            case LiftExample e -> {
                unregisterObjectMultiText(e.getExample());
                e.getTranslations()
                    .values()
                    .forEach(x -> unregisterMetaMultiText(x));
            }
            case LiftVariant v -> {
                unregisterObjectMultiText(v.getForms());
            }
            case LiftTrait _ -> {
            }
            case LiftReversal v -> {
                unregisterObjectMultiText(v.getForms());
            }
            case LiftRelation r -> {
                unregisterMetaMultiText(r.getUsage());
            }
            case LiftPronunciation p -> {
                unregisterObjectMultiText(p.getPronunciation());
            }
            case LiftNote n -> {
                unregisterMetaMultiText(n.getText());
            }
            case LiftMedia m -> {
                unregisterMetaMultiText(m.getLabel());
            }
            case LiftIllustration i -> {
                unregisterMetaMultiText(i.getLabel());
            }
            case LiftField f -> {
                unregisterMetaMultiText(f.getText());
            }
            case LiftEtymology e -> {
                unregisterObjectMultiText(e.getForms());
            }
            case LiftAnnotation a -> {
                unregisterMetaMultiText(a.getText());
            }
            default -> throw new IllegalStateException(
                "Unknown type: " + node.getClass()
            );
        }

        if (node instanceof AbstractExtensibleWithoutField a) {
            a.getAnnotations().forEach(x -> unregisterRec(x));
            a.getTraits().forEach(x -> unregisterRec(x));
            if (node instanceof HasField b) {
                b.getFields().values().forEach(x -> unregisterRec(x));
            }
        }

        if (node instanceof HasNote n) {
            n.getNotes()
                .values()
                .forEach(x -> unregisterRec(x));
        }

        if (node instanceof HasPronunciation n) {
            n.getPronunciations().forEach(x -> unregisterRec(x));
        }

        if (node instanceof HasRelations n) {
            n.getRelations().forEach(x -> unregisterRec(x));
        }

        if (node instanceof HasReversal r) {
            r.getReversals().forEach(x -> unregisterRec(x));
        }

        if (node instanceof HasSense s) {
            s.getSenses().forEach(x -> unregisterRec(x));
        }
    }

    /**
     * Completely remove a node from the dictionary.
     *
     * The node will not be seen by its parent
     * (for instance a sense will not be seen anymore by its parent entry).
     *
     * The node (and all its descendants) will be removed from the dictionary registry.
     *
     * The node will keep its reference towards its child, and the child towards the node.
     *
     * This subtree can be registered again in this dictionary or another.
     *
     * @param node
     */
    public void removeFromDictionary(AbstractLiftRoot node) {
        // remove the link parent -> self

        // pb : should be made afer the second sine it can throw an exception.
        node.detach();

        unregisterRec(node);
    }

    public UUID getNewUUID() {
        return uuidManager.getUniqueUuid();
    }

    public int nEntries() {
        return entriesById.values().size();
    }

    protected void setLanguagesManager(
            LiftDictionaryLanguagesManager objectLanguagesManager,
            LiftDictionaryLanguagesManager metaLanguagesManager) {
        this.objectLanguagesManager = objectLanguagesManager;
        this.metaLanguagesManager = metaLanguagesManager;
    }


    // Old implementation providing the ObservableList for all object type

    // private ObservableList<LiftSense> senses = null;
    // private ObservableList<LiftSense> sensesReadOnly = null;

    // public ObservableList<LiftSense> getSenses() {
    //     if (senses == null) {
    //         senses = FXCollections.observableList(
    //             FXCollections.observableArrayList(
    //                 sensesById.values()
    //             )
    //         );
    //         sensesById.addListener(
    //             (MapChangeListener<UUID, LiftSense>) change -> {
    //                 if (change.wasAdded()) {
    //                     senses.add(change.getValueAdded());
    //                 } else if (change.wasRemoved()) {
    //                     senses.remove(change.getValueRemoved());
    //                 }
    //             }
    //         );
    //     }
    //     sensesReadOnly = FXCollections.unmodifiableObservableList(senses);
    //     return sensesReadOnly;
    // }

    // private ObservableList<LiftExample> examples = null;
    // private ObservableList<LiftExample> examplesReadOnly = null;

    // public ObservableList<LiftExample> getExamples() {
    //     if (examples == null) {
    //         examples = FXCollections.observableList(
    //             FXCollections.observableArrayList(
    //                 examplesById.values()
    //             )
    //         );
    //         examplesById.addListener(
    //             (MapChangeListener<UUID, LiftExample>) change -> {
    //                 if (change.wasAdded()) {
    //                     examples.add(change.getValueAdded());
    //                 } else if (change.wasRemoved()) {
    //                     examples.remove(change.getValueRemoved());
    //                 }
    //             }
    //         );
    //     }
    //     examplesReadOnly = FXCollections.unmodifiableObservableList(examples);
    //     return examplesReadOnly;
    // }



    // private ObservableList<LiftVariant> variantsReadOnly = null;

    // public ObservableList<LiftVariant> getVariants() {
    //     if (variantsReadOnly == null) {
    //         variantsReadOnly = FXCollections.observableList(
    //             FXCollections.observableArrayList(
    //                 variantsById.values()
    //             )
    //         );
    //     }
    //     variantsById.addListener(
    //         (MapChangeListener<UUID, LiftVariant>) change -> {
    //             if (change.wasAdded()) {
    //                 variantsReadOnly.add(change.getValueAdded());
    //             } else if (change.wasRemoved()) {
    //                 variantsReadOnly.remove(change.getValueRemoved());
    //             }
    //         }
    //     );
    //     return variantsReadOnly;
    // }

    // private ObservableList<LiftTrait> traits = null;

    // public ObservableList<LiftTrait> getTraits() {
    //     if (traits == null) {
    //         traits = FXCollections.observableList(
    //             FXCollections.observableArrayList(
    //                 traitsById.values()
    //             )
    //         );
    //         traitsById.addListener(
    //             (MapChangeListener<UUID, LiftTrait>) change -> {
    //                 if (change.wasAdded()) {
    //                     traits.add(change.getValueAdded());
    //                 } else if (change.wasRemoved()) {
    //                     traits.remove(change.getValueRemoved());
    //                 }
    //             }
    //         );
    //     }

    //     return traits;
    // }

    // private ObservableList<LiftReversal> reversalsReadOnly = null;

    // public ObservableList<LiftReversal> getReversals() {
    //     if (reversalsReadOnly == null) {
    //         reversalsReadOnly = FXCollections.observableList(
    //             FXCollections.observableArrayList(
    //                 reversalsById.values()
    //             )
    //         );
    //         reversalsById.addListener(
    //             (MapChangeListener<UUID, LiftReversal>) change -> {
    //                 if (change.wasAdded()) {
    //                     reversalsReadOnly.add(change.getValueAdded());
    //                 } else if (change.wasRemoved()) {
    //                     reversalsReadOnly.remove(change.getValueRemoved());
    //                 }
    //             }
    //         );
    //     }

    //     return reversalsReadOnly;
    // }

    // private ObservableList<LiftRelation> relationsReadOnly = null;

    // public ObservableList<LiftRelation> getRelations() {
    //     if (relationsReadOnly == null) {
    //         relationsReadOnly = FXCollections.observableList(
    //             FXCollections.observableArrayList(
    //                 relationsById.values()
    //             )
    //         );
    //         relationsById.addListener(
    //             (MapChangeListener<UUID, LiftRelation>) change -> {
    //                 if (change.wasAdded()) {
    //                     relationsReadOnly.add(change.getValueAdded());
    //                 } else if (change.wasRemoved()) {
    //                     relationsReadOnly.remove(change.getValueRemoved());
    //                 }
    //             }
    //         );
    //     }

    //     return relationsReadOnly;
    // }

    // private ObservableList<LiftPronunciation> pronunciationsReadOnly = null;

    // public ObservableList<LiftPronunciation> getPronunciations() {
    //     if (pronunciationsReadOnly == null) {
    //         pronunciationsReadOnly = FXCollections.observableList(
    //             FXCollections.observableArrayList(
    //                 pronunciationsById.values()
    //             )
    //         );
    //         pronunciationsById.addListener(
    //             (MapChangeListener<UUID, LiftPronunciation>) change -> {
    //                 if (change.wasAdded()) {
    //                     pronunciationsReadOnly.add(change.getValueAdded());
    //                 } else if (change.wasRemoved()) {
    //                     pronunciationsReadOnly.remove(change.getValueRemoved());
    //                 }
    //             }
    //         );
    //     }

    //     return pronunciationsReadOnly;
    // }

    // private ObservableList<LiftNote> notesReadOnly = null;

    // public ObservableList<LiftNote> getNotes() {
    //     if (notesReadOnly == null) {
    //         notesReadOnly = FXCollections.observableList(
    //             FXCollections.observableArrayList(
    //                 notesById.values()
    //             )
    //         );
    //         notesById.addListener(
    //             (MapChangeListener<UUID, LiftNote>) change -> {
    //                 if (change.wasAdded()) {
    //                     notesReadOnly.add(change.getValueAdded());
    //                 } else if (change.wasRemoved()) {
    //                     notesReadOnly.remove(change.getValueRemoved());
    //                 }
    //             }
    //         );
    //     }

    //     return notesReadOnly;
    // }

    // private ObservableList<LiftMedia> mediasReadOnly = null;

    // public ObservableList<LiftMedia> getMedias() {
    //     if (mediasReadOnly == null) {
    //         mediasReadOnly = FXCollections.observableList(
    //             FXCollections.observableArrayList(
    //                 mediasById.values()
    //             )
    //         );
    //         mediasById.addListener(
    //             (MapChangeListener<UUID, LiftMedia>) change -> {
    //                 if (change.wasAdded()) {
    //                     mediasReadOnly.add(change.getValueAdded());
    //                 } else if (change.wasRemoved()) {
    //                     mediasReadOnly.remove(change.getValueRemoved());
    //                 }
    //             }
    //         );
    //     }

    //     return mediasReadOnly;
    // }

    // private ObservableList<LiftIllustration> illustrationsReadOnly = null;

    // public ObservableList<LiftIllustration> getIllustrations() {
    //     if (illustrationsReadOnly == null) {
    //         illustrationsReadOnly = FXCollections.observableList(
    //             FXCollections.observableArrayList(
    //                 illustrationsById.values()
    //             )
    //         );
    //         illustrationsById.addListener(
    //             (MapChangeListener<UUID, LiftIllustration>) change -> {
    //                 if (change.wasAdded()) {
    //                     illustrationsReadOnly.add(change.getValueAdded());
    //                 } else if (change.wasRemoved()) {
    //                     illustrationsReadOnly.remove(change.getValueRemoved());
    //                 }
    //             }
    //         );
    //     }

    //     return illustrationsReadOnly;
    // }

    // private ObservableList<LiftField> fieldsReadOnly = null;

    // public ObservableList<LiftField> getFields() {
    //     if (fieldsReadOnly == null) {
    //         fieldsReadOnly = FXCollections.observableList(
    //             FXCollections.observableArrayList(
    //                 fieldsById.values()
    //             )
    //         );
    //         fieldsById.addListener(
    //             (MapChangeListener<UUID, LiftField>) change -> {
    //                 if (change.wasAdded()) {
    //                     fieldsReadOnly.add(change.getValueAdded());
    //                 } else if (change.wasRemoved()) {
    //                     fieldsReadOnly.remove(change.getValueRemoved());
    //                 }
    //             }
    //         );
    //     }
    //     return fieldsReadOnly;
    // }

    // private ObservableList<LiftEtymology> etymologiesReadOnly = null;

    // public ObservableList<LiftEtymology> getEtymologies() {
    //     if (etymologiesReadOnly == null) {
    //         etymologiesReadOnly = FXCollections.observableList(
    //             FXCollections.observableArrayList(
    //                 etymologiesById.values()
    //             )
    //         );
    //         etymologiesById.addListener(
    //             (MapChangeListener<UUID, LiftEtymology>) change -> {
    //                 if (change.wasAdded()) {
    //                     etymologiesReadOnly.add(change.getValueAdded());
    //                 } else if (change.wasRemoved()) {
    //                     etymologiesReadOnly.remove(change.getValueRemoved());
    //                 }
    //             }
    //         );
    //     }
    //     return etymologiesReadOnly;
    // }

    // private ObservableList<LiftAnnotation> annotationsReadOnly = null;

    // public ObservableList<LiftAnnotation> getAnnotations() {
    //     if (annotationsReadOnly == null) {
    //         annotationsReadOnly = FXCollections.observableList(
    //             FXCollections.observableArrayList(
    //                 annotationsById.values()
    //             )
    //         );
    //         annotationsById.addListener(
    //             (MapChangeListener<UUID, LiftAnnotation>) change -> {
    //                 if (change.wasAdded()) {
    //                     annotationsReadOnly.add(change.getValueAdded());
    //                 } else if (change.wasRemoved()) {
    //                     annotationsReadOnly.remove(change.getValueRemoved());
    //                 }
    //             }
    //         );
    //     }
    //     return annotationsReadOnly;
    // }

    // private ObservableList<MultiText> objectTextReadOnly = null;

    // public ObservableList<MultiText> getObjectText() {
    //     if (objectTextReadOnly == null) {
    //         objectTextReadOnly = FXCollections.observableList(
    //             FXCollections.observableArrayList(
    //                 objectTextById.values()
    //             )
    //         );
    //         objectTextById.addListener(
    //             (MapChangeListener<UUID, MultiText>) change -> {
    //                 if (change.wasAdded()) {
    //                     objectTextReadOnly.add(change.getValueAdded());
    //                 } else if (change.wasRemoved()) {
    //                     objectTextReadOnly.remove(change.getValueRemoved());
    //                 }
    //             }
    //         );
    //     }
    //     return objectTextReadOnly;
    // }

    // private ObservableList<MultiText> metaTextReadOnly = null;

    // public ObservableList<MultiText> getMetaText() {
    //     if (metaTextReadOnly == null) {
    //         metaTextReadOnly = FXCollections.observableList(
    //             FXCollections.observableArrayList(
    //                 metaTextById.values()
    //             )
    //         );
    //         metaTextById.addListener(
    //             (MapChangeListener<UUID, MultiText>) change -> {
    //                 if (change.wasAdded()) {
    //                     metaTextReadOnly.add(change.getValueAdded());
    //                 } else if (change.wasRemoved()) {
    //                     metaTextReadOnly.remove(change.getValueRemoved());
    //                 }
    //             }
    //         );
    //     }
    //     return metaTextReadOnly;
    // }

}
