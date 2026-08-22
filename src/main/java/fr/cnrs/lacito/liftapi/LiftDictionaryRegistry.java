package fr.cnrs.lacito.liftapi;

import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithoutField;
import fr.cnrs.lacito.liftapi.model.AbstractIdentifiable;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.DuplicateIdException;
import fr.cnrs.lacito.liftapi.model.Form;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

public class LiftDictionaryRegistry {

    private final LiftDictionaryFeatureManager counter;
    private final LiftDictionaryUUIDManager uuidManager =
        new LiftDictionaryUUIDManager();
        
    /**
     * Map from IDs (of referenced object) to objects pointing at them in the dictionary
     */
    public Map<String, List<HasRefId>> refId2Occurrences = new HashMap<>();

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

    private LiftDictionaryLanguagesManager objectLanguagesManager;
    private LiftDictionaryLanguagesManager metaLanguagesManager;

    public AbstractIdentifiable getEntryOrSenseByLiftId(String liftId) {
        if (entriesByLiftId.containsKey(liftId)) {
            return entriesByLiftId.get(liftId);
        } else if (sensesByLiftId.containsKey(liftId)) {
            return sensesByLiftId.get(liftId);
        } else {
            return null;
        }
    }

    private ObservableList<LiftEntry> entriesReadOnly = FXCollections.observableArrayList();

    public ObservableList<LiftEntry> getEntries() {
        // In the particular case of entries, we
        // do not use an observable list but register the entry
        // from the begining in order to keep the order of entries.

        // if (entriesReadOnly == null) {
        //     entriesReadOnly = FXCollections.observableArrayList(
        //         entriesById.values()
        //     );
        //     entriesById.addListener(
        //         (MapChangeListener<UUID, LiftEntry>) change -> {
        //             if (change.wasAdded()) {
        //                 entriesReadOnly.add(change.getValueAdded());
        //             } else if (change.wasRemoved()) {
        //                 entriesReadOnly.remove(change.getValueRemoved());
        //             }
        //         }
        //     );
        // }
        return FXCollections.unmodifiableObservableList(entriesReadOnly);
    }

    private ObservableList<LiftSense> sensesReadOnly = null;

    public ObservableList<LiftSense> getSenses() {
        if (sensesReadOnly == null) {
            sensesReadOnly = FXCollections.observableArrayList(
                sensesById.values()
            );
            sensesById.addListener(
                (MapChangeListener<UUID, LiftSense>) change -> {
                    if (change.wasAdded()) {
                        sensesReadOnly.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        sensesReadOnly.remove(change.getValueRemoved());
                    }
                }
            );
        }
        return sensesReadOnly;
    }

    private ObservableList<LiftExample> examplesReadOnly = null;

    public ObservableList<LiftExample> getExamples() {
        if (examplesReadOnly == null) {
            examplesReadOnly = FXCollections.observableArrayList(
                examplesById.values()
            );
            examplesById.addListener(
                (MapChangeListener<UUID, LiftExample>) change -> {
                    if (change.wasAdded()) {
                        examplesReadOnly.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        examplesReadOnly.remove(change.getValueRemoved());
                    }
                }
            );
        }
        return examplesReadOnly;
    }

    private ObservableList<LiftVariant> variantsReadOnly = null;

    public ObservableList<LiftVariant> getVariantsReadOnly() {
        if (variantsReadOnly == null) {
            variantsReadOnly = FXCollections.observableArrayList(
                variantsById.values()
            );
        }
        variantsById.addListener(
            (MapChangeListener<UUID, LiftVariant>) change -> {
                if (change.wasAdded()) {
                    variantsReadOnly.add(change.getValueAdded());
                } else if (change.wasRemoved()) {
                    variantsReadOnly.remove(change.getValueRemoved());
                }
            }
        );
        return variantsReadOnly;
    }

    private ObservableList<LiftTrait> traitsReadOnly = null;

    public ObservableList<LiftTrait> getTraitsReadOnly() {
        if (traitsReadOnly == null) {
            traitsReadOnly = FXCollections.observableArrayList(
                traitsById.values()
            );
            traitsById.addListener(
                (MapChangeListener<UUID, LiftTrait>) change -> {
                    if (change.wasAdded()) {
                        traitsReadOnly.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        traitsReadOnly.remove(change.getValueRemoved());
                    }
                }
            );
        }

        return traitsReadOnly;
    }

    private ObservableList<LiftReversal> reversalsReadOnly = null;

    public ObservableList<LiftReversal> getReversalsReadOnly() {
        if (reversalsReadOnly == null) {
            reversalsReadOnly = FXCollections.observableArrayList(
                reversalsById.values()
            );
            reversalsById.addListener(
                (MapChangeListener<UUID, LiftReversal>) change -> {
                    if (change.wasAdded()) {
                        reversalsReadOnly.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        reversalsReadOnly.remove(change.getValueRemoved());
                    }
                }
            );
        }

        return reversalsReadOnly;
    }

    private ObservableList<LiftRelation> relationsReadOnly = null;

    public ObservableList<LiftRelation> getRelationsReadOnly() {
        if (relationsReadOnly == null) {
            relationsReadOnly = FXCollections.observableArrayList(
                relationsById.values()
            );
            relationsById.addListener(
                (MapChangeListener<UUID, LiftRelation>) change -> {
                    if (change.wasAdded()) {
                        relationsReadOnly.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        relationsReadOnly.remove(change.getValueRemoved());
                    }
                }
            );
        }

        return relationsReadOnly;
    }

    private ObservableList<LiftPronunciation> pronunciationsReadOnly = null;

    public ObservableList<LiftPronunciation> getPronunciationsReadOnly() {
        if (pronunciationsReadOnly == null) {
            pronunciationsReadOnly = FXCollections.observableArrayList(
                pronunciationsById.values()
            );
            pronunciationsById.addListener(
                (MapChangeListener<UUID, LiftPronunciation>) change -> {
                    if (change.wasAdded()) {
                        pronunciationsReadOnly.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        pronunciationsReadOnly.remove(change.getValueRemoved());
                    }
                }
            );
        }

        return pronunciationsReadOnly;
    }

    private ObservableList<LiftNote> notesReadOnly = null;

    public ObservableList<LiftNote> getNotesReadOnly() {
        if (notesReadOnly == null) {
            notesReadOnly = FXCollections.observableArrayList(
                notesById.values()
            );
            notesById.addListener(
                (MapChangeListener<UUID, LiftNote>) change -> {
                    if (change.wasAdded()) {
                        notesReadOnly.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        notesReadOnly.remove(change.getValueRemoved());
                    }
                }
            );
        }

        return notesReadOnly;
    }

    private ObservableList<LiftMedia> mediasReadOnly = null;

    public ObservableList<LiftMedia> getMediasReadOnly() {
        if (mediasReadOnly == null) {
            mediasReadOnly = FXCollections.observableArrayList(
                mediasById.values()
            );
            mediasById.addListener(
                (MapChangeListener<UUID, LiftMedia>) change -> {
                    if (change.wasAdded()) {
                        mediasReadOnly.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        mediasReadOnly.remove(change.getValueRemoved());
                    }
                }
            );
        }

        return mediasReadOnly;
    }

    private ObservableList<LiftIllustration> illustrationsReadOnly = null;

    public ObservableList<LiftIllustration> getIllustrationsReadOnly() {
        if (illustrationsReadOnly == null) {
            illustrationsReadOnly = FXCollections.observableArrayList(
                illustrationsById.values()
            );
            illustrationsById.addListener(
                (MapChangeListener<UUID, LiftIllustration>) change -> {
                    if (change.wasAdded()) {
                        illustrationsReadOnly.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        illustrationsReadOnly.remove(change.getValueRemoved());
                    }
                }
            );
        }

        return illustrationsReadOnly;
    }

    private ObservableList<LiftField> fieldsReadOnly = null;

    public ObservableList<LiftField> getFieldsReadOnly() {
        if (fieldsReadOnly == null) {
            fieldsReadOnly = FXCollections.observableArrayList(
                fieldsById.values()
            );
            fieldsById.addListener(
                (MapChangeListener<UUID, LiftField>) change -> {
                    if (change.wasAdded()) {
                        fieldsReadOnly.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        fieldsReadOnly.remove(change.getValueRemoved());
                    }
                }
            );
        }
        return fieldsReadOnly;
    }

    private ObservableList<LiftEtymology> etymologiesReadOnly = null;

    public ObservableList<LiftEtymology> getEtymologiesReadOnly() {
        if (etymologiesReadOnly == null) {
            etymologiesReadOnly = FXCollections.observableArrayList(
                etymologiesById.values()
            );
            etymologiesById.addListener(
                (MapChangeListener<UUID, LiftEtymology>) change -> {
                    if (change.wasAdded()) {
                        etymologiesReadOnly.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        etymologiesReadOnly.remove(change.getValueRemoved());
                    }
                }
            );
        }
        return etymologiesReadOnly;
    }

    private ObservableList<LiftAnnotation> annotationsReadOnly = null;

    public ObservableList<LiftAnnotation> getAnnotationsReadOnly() {
        if (annotationsReadOnly == null) {
            annotationsReadOnly = FXCollections.observableArrayList(
                annotationsById.values()
            );
            annotationsById.addListener(
                (MapChangeListener<UUID, LiftAnnotation>) change -> {
                    if (change.wasAdded()) {
                        annotationsReadOnly.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        annotationsReadOnly.remove(change.getValueRemoved());
                    }
                }
            );
        }
        return annotationsReadOnly;
    }

    private ObservableList<MultiText> objectTextReadOnly = null;

    public ObservableList<MultiText> getObjectTextReadOnly() {
        if (objectTextReadOnly == null) {
            objectTextReadOnly = FXCollections.observableArrayList(
                objectTextById.values()
            );
            objectTextById.addListener(
                (MapChangeListener<UUID, MultiText>) change -> {
                    if (change.wasAdded()) {
                        objectTextReadOnly.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        objectTextReadOnly.remove(change.getValueRemoved());
                    }
                }
            );
        }
        return objectTextReadOnly;
    }

    private ObservableList<MultiText> metaTextReadOnly = null;

    public ObservableList<MultiText> getMetaTextReadOnly() {
        if (metaTextReadOnly == null) {
            metaTextReadOnly = FXCollections.observableArrayList(
                metaTextById.values()
            );
            metaTextById.addListener(
                (MapChangeListener<UUID, MultiText>) change -> {
                    if (change.wasAdded()) {
                        metaTextReadOnly.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        metaTextReadOnly.remove(change.getValueRemoved());
                    }
                }
            );
        }
        return metaTextReadOnly;
    }

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

    /**
     * Add a node (and its descendants) to the directory using the low-level
     * API, intended for unmarshalling efficiently
     * the dictionary. The high-level (fluent) API, using {@link
     * LiftDictionary#getComponentBuilder()}, should be preferred in all other
     * situations.
     *
     * This method is intended for nodes that has been created manually, via the
     * constructors for {@link LiftEntry}, {@link LiftSense}, etc.
     *
     * All subnodes of the node (added with addX method, such as {@link
     * LiftElement#addSense}) will also be added to the dictionary.
     */
    public void addToDictionaryLowLevel(AbstractLiftRoot node) {
        
        // 1/ Check that the languages present in multitexts are registered in the dictionary
        // if not, add them to the dictionary (to the languages manager)
        if (! (node instanceof LiftTrait)) {
            if (node instanceof LiftEntry || node instanceof LiftExample || node instanceof LiftVariant
                || node instanceof LiftReversal || node instanceof LiftPronunciation || node instanceof LiftEtymology
            ) {
                for (Form f : node.getMainMultiText().getForms()) {
                    String lang = f.getLang();
                    if (!objectLanguagesManager.hasLanguage(lang)) {
                        objectLanguagesManager.addLanguage(lang);
                    }
                }
            } else if (node instanceof LiftSense || node instanceof LiftRelation
                || node instanceof LiftNote || node instanceof LiftMedia
                || node instanceof LiftIllustration || node instanceof LiftField
                || node instanceof LiftAnnotation
            ) {
                for (Form f : node.getMainMultiText().getForms()) {
                    String lang = f.getLang();
                    if (!metaLanguagesManager.hasLanguage(lang)) {
                        metaLanguagesManager.addLanguage(lang);
                    }
                }
            }
            for (Form f : node.getMainMultiText().getForms()) {
                String lang = f.getLang();
                if (!objectLanguagesManager.hasLanguage(lang)) {
                    objectLanguagesManager.addLanguage(lang);
                }
            }
        }
        // Sense et Exemple contain a second MultiText.
        if (node instanceof LiftSense s) {
            for (Form f : s.getDefinition().getForms()) {
                String lang = f.getLang();
                if (!metaLanguagesManager.hasLanguage(lang)) {
                    metaLanguagesManager.addLanguage(lang);
                }
            }
        } else if (node instanceof LiftExample e) {
            e.getTranslations()
                .values()
                .forEach(x -> {
                    for (Form f : x.getForms()) {
                        String lang = f.getLang();
                        if (!metaLanguagesManager.hasLanguage(lang)) {
                            metaLanguagesManager.addLanguage(lang);
                        }
                    }
                });
        }

        // 2. Add the node to the register.
        // register the node and it(s) multiText(s) in the registry
        register(node);

        // recursively add its descendants
        switch (node) {
            case LiftEntry e -> {
                e.getVariants().forEach(x -> addToDictionaryLowLevel(x));
                e.getEtymologies().forEach(x -> addToDictionaryLowLevel(x));
                //registerObjectMultiText(node.getMainMultiText());
            }
            case LiftSense s -> {
                s.getExamples().forEach(x -> addToDictionaryLowLevel(x));
                s.getIllustrations().forEach(x -> addToDictionaryLowLevel(x));
                s.getReversals().forEach(x -> addToDictionaryLowLevel(x));
                //registerMetaMultiText(node.getMainMultiText());
                //registerMetaMultiText(s.getDefinition());
            }
            case LiftExample e -> {
                // registerObjectMultiText(e.getExample());
                // e.getTranslations()
                //     .values()
                //     .forEach(x -> registerMetaMultiText(x));
            }
            case LiftVariant v -> {
                // registerObjectMultiText(v.getForms());
            }
            case LiftTrait _ -> {
            }
            case LiftReversal v -> {
                // registerObjectMultiText(v.getForms());
            }
            case LiftRelation r -> {
                // registerMetaMultiText(r.getUsage());
            }
            case LiftPronunciation p -> {
                // registerObjectMultiText(p.getPronunciation());
            }
            case LiftNote n -> {
                // registerMetaMultiText(n.getText());
            }
            case LiftMedia m -> {
                // registerMetaMultiText(m.getLabel());
            }
            case LiftIllustration i -> {
                // registerMetaMultiText(i.getLabel());
            }
            case LiftField f -> {
                // registerMetaMultiText(f.getText());
            }
            case LiftEtymology e -> {
                // registerObjectMultiText(e.getForms());
            }
            case LiftAnnotation a -> {
                // registerMetaMultiText(a.getText());
            }
            default -> throw new IllegalStateException(
                "Unknown type: " + node.getClass()
            );
        }

        if (node instanceof AbstractExtensibleWithoutField a) {
            a.getAnnotations().forEach(x -> addToDictionaryLowLevel(x));
            a.getTraits().forEach(x -> addToDictionaryLowLevel(x));
            if (node instanceof HasField b) {
                b.getFields().forEach(x -> addToDictionaryLowLevel(x));
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
     * Register the node in the dictionary; add a LIFT ID to the node if it
     * doesn't have one.
     */
    public void register(AbstractLiftRoot node) {
        if (node.getUUID() != null) {
            throw new IllegalArgumentException(
                "This node seems to have already been registered in a dictionary."
            );
        }
        UUID uuid = getNewUUID();
        node.setUUID(uuid);

        // Test if node has a parent.
        // and if the parent belongs to the dictionary...
        //
        // setParent should be protected for all class (called in the addChild method)
        //if (!(node instanceof LiftEntry)  && node.getParent() == null) {
        //    throw new IllegalArgumentException(
        //        "Node should have a parent"
        //    );
        //}

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
                entriesReadOnly.add(e);
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
     * unregister : remove a node from the registry
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
    }

    protected void unregisterMetaMultiText(MultiText node) {
        metaTextById.remove(node.getUUID());
        node.unregister();
    }

    /**
     * Completely remove a node from the dictionary. The node will not be seen by its parent
     * (for instance a sense will not be seen anymore by its parent entry), and
     * all the node's descendants will be removed as well.
     *
     * @param entry
     */
    public void removeFromDictionary(AbstractLiftRoot node) {

        // manage reference counting
        if (node instanceof LiftRelation r) {
            final String target = r
                .getRefObject().getId()
                .orElseThrow(() ->
                    new IllegalArgumentException("Reference ID is missing")
                );
            refId2Occurrences.get(target).removeIf(o -> o == r);
        } else if (node instanceof LiftVariant a) {
            final String target = a
                .getRefObject().getId()
                .orElseThrow(() ->
                    new IllegalArgumentException("Reference ID is missing")
                );
            refId2Occurrences.get(target).removeIf(o -> o == a);
        } else if (node instanceof AbstractIdentifiable i) {
            final String refId = i
                .getId()
                .orElseThrow(() ->
                    new IllegalArgumentException("Reference ID is missing")
                );
            if (
                refId2Occurrences.containsKey(refId) &&
                refId2Occurrences.get(refId).size() > 0
            ) {
                throw new IllegalStateException(
                    "Cannot delete this node: it is referenced from other nodes."
                );
            }
        }

        // remove from this register
        unregister(node);

        // detach from its parent
        node.detach();

        // recursively remove its descendants
        switch (node) {
            case LiftEntry e -> {
                e.getVariants().forEach(x -> removeFromDictionary(x));
                e.getEtymologies().forEach(x -> removeFromDictionary(x));
                unregisterObjectMultiText(node.getMainMultiText());
            }
            case LiftSense s -> {
                s.getExamples().forEach(x -> removeFromDictionary(x));
                s.getIllustrations().forEach(x -> removeFromDictionary(x));
                s.getReversals().forEach(x -> removeFromDictionary(x));
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
            a.getAnnotations().forEach(x -> removeFromDictionary(x));
            a.getTraits().forEach(x -> removeFromDictionary(x));
            if (node instanceof HasField b) {
                b.getFields().forEach(x -> removeFromDictionary(x));
            }
        }

        if (node instanceof HasNote n) {
            n.getNotes()
                .values()
                .forEach(x -> removeFromDictionary(x));
        }

        if (node instanceof HasPronunciation n) {
            n.getPronunciations().forEach(x -> removeFromDictionary(x));
        }

        if (node instanceof HasRelations n) {
            n.getRelations().forEach(x -> removeFromDictionary(x));
        }

        if (node instanceof HasReversal r) {
            r.getReversals().forEach(x -> removeFromDictionary(x));
        }

        if (node instanceof HasSense s) {
            s.getSenses().forEach(x -> removeFromDictionary(x));
        }
    }

    public UUID getNewUUID() {
        return uuidManager.getUniqueUuid();
    }

    public int nEntries() {
        return entriesById.values().size();
    }

    protected void setLanguagesManager(LiftDictionaryLanguagesManager objectLanguagesManager,
            LiftDictionaryLanguagesManager metaLanguagesManager) {
        this.objectLanguagesManager = objectLanguagesManager;
        this.metaLanguagesManager = metaLanguagesManager;
    }

}
