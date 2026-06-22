package fr.cnrs.lacito.liftapi;

import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithoutField;
import fr.cnrs.lacito.liftapi.model.AbstractIdentifiable;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasField;
import fr.cnrs.lacito.liftapi.model.HasNote;
import fr.cnrs.lacito.liftapi.model.HasPronunciation;
import fr.cnrs.lacito.liftapi.model.HasRelations;
import fr.cnrs.lacito.liftapi.model.HasReversal;
import fr.cnrs.lacito.liftapi.model.HasSense;
import fr.cnrs.lacito.liftapi.model.HasType;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
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
import fr.cnrs.lacito.liftapi.model.MultiTextMetaLanguage;
import fr.cnrs.lacito.liftapi.model.MultiTextObjectLanguage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

public class LiftDictionaryRegistry {

    public boolean populatingMode = false;
    private final LiftDictionaryFeatureManager counter;
    private final LiftDictionaryUUIDManager uuidManager =
        new LiftDictionaryUUIDManager();

    /**
     * List of IDs to which there is a reference pointing in the dictionary
     */
    private Map<String, Integer> refId2Occurrences = new HashMap<>();

    private final ObservableMap<String, LiftEntry> entriesByLiftId =
        FXCollections.observableHashMap();

    protected final ObservableMap<String, LiftSense> sensesByLiftId =
        FXCollections.observableHashMap();

    protected Map<String, UUID> entryLiftId2Uuid = new HashMap<>(200);
    protected Map<String, UUID> senseLiftId2Uuid = new HashMap<>(200);
    protected final ObservableMap<UUID, LiftEntry> entriesById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftSense> sensesById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftExample> examplesById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftVariant> variantsById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftTrait> traitsById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftReversal> reversalsById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftRelation> relationsById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftPronunciation> pronunciationsById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftNote> notesById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftMedia> mediasById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftIllustration> illustrationsById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftField> fieldsById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftEtymology> etymologiesById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, LiftAnnotation> annotationsById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, MultiText> objectTextById =
        FXCollections.observableHashMap();
    private final ObservableMap<UUID, MultiText> metaTextById =
        FXCollections.observableHashMap();

    private ObservableList<LiftEntry> entriesReadOnly = null;

    public ObservableList<LiftEntry> getEntries() {
        if (entriesReadOnly == null) {
            entriesReadOnly = FXCollections.observableArrayList(
                entriesById.values()
            );
            entriesById.addListener(
                (MapChangeListener<UUID, LiftEntry>) change -> {
                    if (change.wasAdded()) {
                        entriesReadOnly.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        entriesReadOnly.remove(change.getValueRemoved());
                    }
                }
            );
        }
        return entriesReadOnly;
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
     * Add a node to the directory. The node should not have been created with
     * the fluent API ({@link LiftDictionary#getComponentBuilder()}), since via this API,
     * the node is automatically added to the directory (an Exception will be thrown).
     *
     * This method is intended for nodes that has been created manually, via the
     * constructors for {@link LiftEntry}, {@link LiftSense}, etc.
     *
     * All subnodes of the node (added with addX method, such as {@link
     * LiftElement#addSense}) will also be added to the dictionary.
     */
    public void addToDictionary(AbstractLiftRoot node) {
        // add to register
        register(node);

        // recursively add its descendants
        switch (node) {
            case LiftEntry e -> {
                e.getVariants().forEach(x -> addToDictionary(x));
                e.getEtymologies().forEach(x -> addToDictionary(x));
                registerObjectLanguage(node.getMainMultiText());
            }
            case LiftSense s -> {
                s.getExamples().forEach(x -> addToDictionary(x));
                s.getIllustrations().forEach(x -> addToDictionary(x));
                s.getReversals().forEach(x -> addToDictionary(x));
                registerMetaLanguage(node.getMainMultiText());
                registerMetaLanguage(s.getDefinition());
            }
            case LiftExample e -> {
                registerObjectLanguage(e.getExample());
                e.getTranslations()
                    .values()
                    .forEach(x -> unregisterMetaMultiText(x));
            }
            case LiftVariant v -> {
                registerObjectLanguage(v.getForms());
            }
            case LiftTrait _ -> {
            }
            case LiftReversal v -> {
                registerObjectLanguage(v.getForms());
            }
            case LiftRelation r -> {
                registerMetaLanguage(r.getUsage());
            }
            case LiftPronunciation p -> {
                registerObjectLanguage(p.getPronunciation());
            }
            case LiftNote n -> {
                registerMetaLanguage(n.getText());
            }
            case LiftMedia m -> {
                registerMetaLanguage(m.getLabel());
            }
            case LiftIllustration i -> {
                registerMetaLanguage(i.getLabel());
            }
            case LiftField f -> {
                registerMetaLanguage(f.getText());
            }
            case LiftEtymology e -> {
                registerObjectLanguage(e.getForms());
            }
            case LiftAnnotation a -> {
                registerMetaLanguage(a.getText());
            }
            default -> throw new IllegalStateException(
                "Unknown type: " + node.getClass()
            );
        }

        if (node instanceof AbstractExtensibleWithoutField a) {
            a.getAnnotations().forEach(x -> addToDictionary(x));
            a.getTraits().forEach(x -> addToDictionary(x));
            if (node instanceof HasField b) {
                b.getFields().forEach(x -> addToDictionary(x));
            }
        }

        if (node instanceof HasNote n) {
            n.getNotes()
                .values()
                .forEach(x -> addToDictionary(x));
        }

        if (node instanceof HasPronunciation n) {
            n.getPronunciations().forEach(x -> addToDictionary(x));
        }

        if (node instanceof HasRelations n) {
            n.getRelations().forEach(x -> addToDictionary(x));
        }

        if (node instanceof HasReversal r) {
            r.getReversals().forEach(x -> addToDictionary(x));
        }

        if (node instanceof HasSense s) {
            s.getSenses().forEach(x -> addToDictionary(x));
        }
    }

    /**
     * Non-recursively add the node.
     *
     * Add a LIFT ID to the node if it doesn't have one.
     */
    public void register(AbstractLiftRoot node) {
        if (node.getUUID() != null) {
            throw new IllegalArgumentException(
                "Added node should not contains a UUID"
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
                String uuidS = e.getUUID().toString();
                if (e.getId().isEmpty()) {
                    e.setId(uuidS);
                }
                entryLiftId2Uuid.put(uuidS, e.getUUID());
            }
            case LiftSense s -> {
                sensesById.put(s.getUUID(), s);
                String uuidS = s.getUUID().toString();
                if (s.getId().isEmpty()) {
                    s.setId(uuidS);
                }
                senseLiftId2Uuid.put(uuidS, s.getUUID());
            }
            case LiftExample o -> examplesById.put(o.getUUID(), o);
            case LiftVariant o -> {
                variantsById.put(o.getUUID(), o);
                String refId = o.getRefId().orElseThrow();
                if (
                    !populatingMode &&
                    !(entriesByLiftId.containsKey(refId) ||
                        sensesByLiftId.containsKey(refId))
                ) {
                    throw new IllegalArgumentException(
                        "Reference id " +
                            refId +
                            " not found in senses or entries."
                    );
                }
            }
            case LiftTrait o -> traitsById.put(o.getUUID(), o);
            case LiftReversal o -> reversalsById.put(o.getUUID(), o);
            case LiftRelation o -> {
                relationsById.put(o.getUUID(), o);
                String refId = o.getRefID().orElseThrow();
                if (
                    !populatingMode &&
                    !(entriesByLiftId.containsKey(refId) ||
                        sensesByLiftId.containsKey(refId))
                ) {
                    throw new IllegalArgumentException(
                        "Reference id " +
                            refId +
                            " not found in senses or entries."
                    );
                }
                refId2Occurrences.merge(refId, 1, Integer::sum);
            }
            case LiftPronunciation o -> pronunciationsById.put(o.getUUID(), o);
            case LiftNote o -> notesById.put(o.getUUID(), o);
            case LiftMedia o -> mediasById.put(o.getUUID(), o);
            case LiftIllustration o -> illustrationsById.put(o.getUUID(), o);
            case LiftField o -> fieldsById.put(o.getUUID(), o);
            case LiftEtymology o -> etymologiesById.put(o.getUUID(), o);
            case LiftAnnotation o -> annotationsById.put(o.getUUID(), o);
            default -> throw new IllegalStateException(
                "Unknown type: " + node.getClass()
            );
        }
    }

    public void registerObjectLanguage(MultiText element) {
        if (element.getUUID() != null) {
            throw new IllegalArgumentException("UUID already set");
        }
        UUID uuid = getNewUUID();
        element.setUUID(uuid);
        objectTextById.put(uuid, element);
    }

    public void registerMetaLanguage(MultiText element) {
        if (element.getUUID() != null) {
            throw new IllegalArgumentException("UUID already set");
        }
        UUID uuid = getNewUUID();
        element.setUUID(uuid);
        metaTextById.put(uuid, element);
    }

    // unregister : remove a node from the registry

    protected void unregister(AbstractLiftRoot node) {
        Map<UUID, ? extends AbstractLiftRoot> map = null;
        switch (node) {
            case LiftEntry _ -> {
                map = entriesById;
            }
            case LiftSense _ -> {
                map = sensesById;
            }
            case LiftExample _ -> {
                map = examplesById;
            }
            case LiftVariant _ -> {
                map = variantsById;
            }
            case LiftTrait _ -> {
                map = traitsById;
            }
            case LiftReversal _ -> {
                map = reversalsById;
            }
            case LiftRelation _ -> {
                map = relationsById;
            }
            case LiftPronunciation _ -> {
                map = pronunciationsById;
            }
            case LiftNote _ -> {
                map = notesById;
            }
            case LiftMedia _ -> {
                map = mediasById;
            }
            case LiftIllustration _ -> {
                map = illustrationsById;
            }
            case LiftField _ -> {
                map = fieldsById;
            }
            case LiftEtymology _ -> {
                map = etymologiesById;
            }
            case LiftAnnotation _ -> {
                map = annotationsById;
            }
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
    }

    protected void unregisterObjectMultiText(MultiText node) {
        objectTextById.remove(node.getUUID());
    }

    protected void unregisterMetaMultiText(MultiText node) {
        metaTextById.remove(node.getUUID());
    }

    // access to content of the dictionary

    public List<LiftEntry> getEntryByForm(String form, String lang) {
        final Form empty = new Form(lang, "");
        return entriesById
            .values()
            .stream()
            .filter(x -> x.getForms().containsLang(lang))
            .filter(x ->
                x.getForms().getForm(lang).get().toPlainText().equals(form)
            )
            .toList();
    }

    public List<MultiText> searchInMetaLanguage(String lang, String searched) {
        if (lang == null) throw new IllegalArgumentException(
            "lang must not be null"
        );
        if (searched == null) throw new IllegalArgumentException(
            "searched string must not be null"
        );
        return metaTextById
            .values()
            .stream()
            .filter(x -> x.containsLang(lang))
            .filter(x -> x.getForm(lang).get().toPlainText().matches(searched))
            .toList();
    }

    public List<MultiText> searchInObjectLanguage(
        String lang,
        String searched
    ) {
        if (lang == null) throw new IllegalArgumentException(
            "lang must not be null"
        );
        if (searched == null) throw new IllegalArgumentException(
            "searched string must not be null"
        );
        return objectTextById
            .values()
            .stream()
            .filter(x -> x.containsLang(lang))
            .filter(x -> x.getForm(lang).get().toPlainText().matches(searched))
            .toList();
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
            String referenced = r.getRefID().orElse("");
            refId2Occurrences.compute(referenced, (k, v) -> v - 1);
        } else if (node instanceof LiftVariant a) {
            String referenced = a.getRefId().orElse("");
            refId2Occurrences.compute(referenced, (k, v) -> v - 1);
        } else if (node instanceof AbstractIdentifiable i) {
            String refId = i.getId().orElse("");
            if (
                refId2Occurrences.containsKey(refId) &&
                refId2Occurrences.get(refId) > 0
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

    public void enterPopulatingMode() {
        populatingMode = true;
    }

    public void postPopulate(LiftHeader header) {
        checkForIdRefs();
        Set<String> objectLang = discoverLanguage(objectTextById.values());
        for (String lang : objectLang) {
            if (!header.containsObjectLanguage(lang)) {
                header.addObjectLanguage(lang);
            }
        }
        Set<String> metaLang = discoverLanguage(metaTextById.values());
        for (String lang : metaLang) {
            if (!header.containsMetaLanguage(lang)) {
                header.addMetaLanguage(lang);
            }
        }

        Set<String> etymologyTypes = etymologiesById
            .values()
            .stream()
            .filter(x -> x.getType().isPresent())
            .map(x -> x.getType().get())
            .distinct()
            .collect(Collectors.toSet());
        for (String type : etymologyTypes) {
            if (!header.containsEtymologyType(type)) {
                header.addEtymologyType(type);
            }
        }

        Set<String> noteTypes = notesById
            .values()
            .stream()
            .filter(x -> x.getType().isPresent())
            .map(x -> x.getType().get())
            .distinct()
            .collect(Collectors.toSet());
        for (String type : noteTypes) {
            if (!header.containsNoteType(type)) {
                header.addNoteType(type);
            }
        }

        Set<String> relationTypes = relationsById
            .values()
            .stream()
            .filter(x -> x.getType().isPresent())
            .map(x -> x.getType().get())
            .distinct()
            .collect(Collectors.toSet());
        for (String type : relationTypes) {
            if (!header.containsRelationType(type)) {
                header.addRelationType(type);
            }
        }

        ObservableSet<String> translationTypes = discoverTranslationType(
            examplesById.values()
        );
        for (String type : translationTypes) {
            if (!header.containsTranslationType(type)) {
                header.addTranslationType(type);
            }
        }

        guessFieldKind();

        this.populatingMode = true;
    }

    /**
     * After reading a dictionary serialisation, check that all
     * the references to another nodes are valid (i.e. the referenced node exists).
     */
    private void checkForIdRefs() {
        if (!populatingMode) throw new IllegalStateException(
            "Cant check for id refs after populating mode is off"
        );
        for (String id : refId2Occurrences.keySet()) {
            if (
                !entriesByLiftId.containsKey(id) &&
                !sensesByLiftId.containsKey(id)
            ) {
                throw new IllegalArgumentException(
                    "Reference id " + id + " not found in entries or senses."
                );
            }
        }
    }

    // call finalizer (or whatever) on LiftXMLFactory
    // call discoverFields() on ValueManager, LangManager
    // update Header accordingly
    // Link range to Trait definition

    /**
     * Post-process field-definitions with UNKNOWN kind/type in the header by checking whether
     * the name matches a trait name or a field name actually used in the dictionary.
     * A name that appears as a trait name is classified as TRAIT; as a field name, FIELD.
     */
    public void guessFieldKind() {
        java.util.Set<String> traitNames = traitsById
            .values()
            .stream()
            .map(LiftTrait::getName)
            .collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> fieldNames = fieldsById
            .values()
            .stream()
            .map(LiftField::getName)
            .collect(java.util.stream.Collectors.toSet());

        for (LiftFieldAndTraitDefinition fd : header.getFields()) {
            if (fd.getKind() == LiftFieldAndTraitDefinitionKind.UNKNOWN) {
                if (traitNames.contains(fd.getName())) {
                    fd.setKind(LiftFieldAndTraitDefinitionKind.TRAIT);
                } else if (fieldNames.contains(fd.getName())) {
                    fd.setKind(LiftFieldAndTraitDefinitionKind.FIELD);
                }
            }
            // Link option-range to actual LiftHeaderRange object (m/)
            fd.resolveRange(header);
        }
    }

    public static ObservableSet<String> discoverTranslationType(
        Collection<LiftExample> exemples
    ) {
        Set<String> type = new HashSet<>();
        for (LiftExample ex : exemples) {
            for (String t : ex.getTranslations().keySet()) {
                type.add(t);
            }
        }
        return FXCollections.observableSet(type);
    }

    public static Set<String> discoverType(Collection<HasType> objects) {
        Set<String> type = new HashSet<>();
        for (HasType a : objects) {
            type.add(a.getType().get());
        }
        return type;
    }

    public static Set<String> discoverLanguage(
        Collection<MultiText> multiTexts
    ) {
        Set<String> lang = new HashSet<>();
        for (MultiText m : multiTexts) {
            for (Form f : m.getForms()) {
                lang.add(f.getLang());
            }
        }
        return lang;
    }
}
