package fr.cnrs.lacito.liftapi.internal;

import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.GrammaticalInfo;
import fr.cnrs.lacito.liftapi.model.HasRefId;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

/**
 * The indexes a dictionary keeps over its components.
 *
 * <h2>Why this is a class of its own</h2>
 *
 * Reading these indexes is public API - that is what
 * {@code LiftDictionaryRegistry.getSenses()} and its siblings are for - while writing
 * them must not be reachable from outside the library. Java offers no way to give one
 * package write access to another package's fields, so the two cannot live in the same
 * class: the writer ({@link DictionaryMutator}) is in this unexported package, the
 * reader ({@code fr.cnrs.lacito.liftapi.LiftDictionaryRegistry}) is in the exported one,
 * and this object is the state they share. The reader only ever hands out unmodifiable
 * views of it.
 *
 * <h2>Not public API</h2>
 *
 * The package {@code fr.cnrs.lacito.liftapi.internal} is deliberately not exported by
 * {@code module-info.java}. The fields here are {@code public} only because javac
 * requires it for access from the sibling packages inside this module; no consumer of
 * the library can reach them.
 */
public final class DictionaryRegisters {

    private final LiftDictionaryUUIDManager uuidManager =
        new LiftDictionaryUUIDManager();

    /**
     * Map from the LIFT id of a referenced component to the components pointing at it.
     *
     * Kept private, with the mutating half reachable only from this package: exposing
     * the live map let any caller corrupt the reference counting that deletion relies on
     * to refuse removing a component that is still referenced.
     */
    private final Map<String, List<HasRefId>> refId2HasRefIdList = new HashMap<>();

    // ------------------------------------------------------------------
    // The indexes
    // ------------------------------------------------------------------

    public final ObservableMap<String, LiftEntry> entriesByLiftId =
        FXCollections.observableHashMap();

    public final ObservableMap<String, LiftSense> sensesByLiftId =
        FXCollections.observableHashMap();

    public final Map<String, UUID> entryLiftId2Uuid = new HashMap<>(200);

    public final Map<String, UUID> senseLiftId2Uuid = new HashMap<>(200);

    public final ObservableMap<UUID, LiftEntry> entriesById =
        FXCollections.observableHashMap();
    public final ObservableMap<UUID, LiftSense> sensesById =
        FXCollections.observableHashMap();
    public final ObservableMap<UUID, LiftExample> examplesById =
        FXCollections.observableHashMap();
    public final ObservableMap<UUID, LiftVariant> variantsById =
        FXCollections.observableHashMap();
    public final ObservableMap<UUID, LiftTrait> traitsById =
        FXCollections.observableHashMap();
    public final ObservableMap<UUID, LiftReversal> reversalsById =
        FXCollections.observableHashMap();
    public final ObservableMap<UUID, LiftRelation> relationsById =
        FXCollections.observableHashMap();
    public final ObservableMap<UUID, LiftPronunciation> pronunciationsById =
        FXCollections.observableHashMap();
    public final ObservableMap<UUID, LiftNote> notesById =
        FXCollections.observableHashMap();
    public final ObservableMap<UUID, LiftMedia> mediasById =
        FXCollections.observableHashMap();
    public final ObservableMap<UUID, LiftIllustration> illustrationsById =
        FXCollections.observableHashMap();
    public final ObservableMap<UUID, LiftField> fieldsById =
        FXCollections.observableHashMap();
    public final ObservableMap<UUID, LiftEtymology> etymologiesById =
        FXCollections.observableHashMap();
    public final ObservableMap<UUID, LiftAnnotation> annotationsById =
        FXCollections.observableHashMap();
    public final ObservableMap<UUID, GrammaticalInfo> grammaticalInfosById =
        FXCollections.observableHashMap();
    public final ObservableMap<UUID, MultiText> objectTextById =
        FXCollections.observableHashMap();
    public final ObservableMap<UUID, MultiText> metaTextById =
        FXCollections.observableHashMap();

    /**
     * The entries, in document order.
     *
     * Entries are held in a list of their own rather than derived from
     * {@link #entriesById}, because the order they appear in the dictionary is
     * meaningful and a hash map does not keep it.
     */
    public final ObservableList<LiftEntry> entries =
        FXCollections.observableArrayList();

    public UUID newUUID() {
        return uuidManager.getUniqueUuid();
    }

    // ------------------------------------------------------------------
    // Per-kind index lookup
    // ------------------------------------------------------------------

    /**
     * The index holding nodes of the same kind as {@code node}.
     *
     * The element type is captured by the type variable {@code T} rather than written
     * as a wildcard: you can read from a {@code Map<UUID, ? extends AbstractLiftRoot>}
     * but never {@code put} into one, because the compiler cannot prove the value
     * matches the map's actual element type. With {@code T} it can, so callers get a
     * map they may both read and write.
     *
     * The cast is unchecked but safe by construction: each branch below returns the map
     * declared for exactly the runtime type matched, so the returned map only ever
     * receives nodes of its own kind.
     *
     * @param node the node whose index is wanted
     * @return the index for that kind of node, never {@code null}
     * @throws IllegalStateException if the node is of an unknown kind
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractLiftRoot> Map<UUID, T> nodesById(T node) {
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
            case GrammaticalInfo _ ->  map = grammaticalInfosById;
            default -> throw new IllegalStateException(
                "Unknown type: " + node.getClass()
            );
        }
        return (Map<UUID, T>) map;
    }

    // ------------------------------------------------------------------
    // Reference counting
    // ------------------------------------------------------------------

    /** The LIFT ids that at least one component in the dictionary refers to. */
    public Set<String> referencedTargetIds() {
        return Collections.unmodifiableSet(refId2HasRefIdList.keySet());
    }

    /** The components referring to {@code targetId}; empty if nothing does. */
    public List<HasRefId> referencesTo(String targetId) {
        List<HasRefId> sources = refId2HasRefIdList.get(targetId);
        return sources == null
            ? List.of()
            : Collections.unmodifiableList(sources);
    }

    /** Whether any component refers to {@code targetId}. */
    public boolean isReferenced(String targetId) {
        List<HasRefId> sources = refId2HasRefIdList.get(targetId);
        return sources != null && !sources.isEmpty();
    }

    void addReference(String targetId, HasRefId source) {
        refId2HasRefIdList
            .computeIfAbsent(targetId, k -> new ArrayList<>())
            .add(source);
    }

    void removeReference(String targetId, HasRefId source) {
        List<HasRefId> sources = refId2HasRefIdList.get(targetId);
        if (sources != null) sources.removeIf(o -> o == source);
    }
}
