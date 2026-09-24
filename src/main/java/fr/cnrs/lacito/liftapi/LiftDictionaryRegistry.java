package fr.cnrs.lacito.liftapi;

import fr.cnrs.lacito.liftapi.internal.DictionaryRegisters;
import fr.cnrs.lacito.liftapi.model.AbstractIdentifiable;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

/**
 * A read-only view of everything a dictionary contains.
 *
 * It offers unmodifiable, observable collections of every kind of component -
 * {@code getEntries()}, {@code getSenses()}, {@code getExamples()}, ... - plus lookup by
 * LIFT id and the reference counting used to tell whether a component is still pointed
 * at from somewhere else.
 *
 * <h2>Nothing here mutates the dictionary</h2>
 *
 * Changing a dictionary is done through the components themselves:
 *
 * <ul>
 * <li>{@code parent.addX(child)} wires a component in and, if {@code parent} belongs to
 * a dictionary, registers it there; {@code parent.deleteX(child)} is its mirror. See the
 * {@code fr.cnrs.lacito.liftapi.model} package documentation.</li>
 * <li>{@link LiftDictionary#getComponentBuilder()} is the fluent way to create one.</li>
 * <li>An entry has no parent, so {@link LiftDictionary#addEntry(LiftEntry)} and
 * {@link LiftDictionary#removeEntry(LiftEntry)} handle that one case.</li>
 * </ul>
 *
 * The code that writes these indexes lives in {@code fr.cnrs.lacito.liftapi.internal},
 * a package this module does not export, so it is unreachable from outside the library.
 */
public class LiftDictionaryRegistry {

    /**
     * The indexes themselves, shared with the mutation core.
     *
     * The field is private and its type is not exported, so this class can hand out
     * unmodifiable views of the indexes without any caller being able to reach the live
     * collections behind them.
     */
    private final DictionaryRegisters registers;

    LiftDictionaryRegistry(DictionaryRegisters registers) {
        if (registers == null) {
            throw new IllegalArgumentException("registers cannot be null");
        }
        this.registers = registers;
    }

    // -----------------------------------------------------------------------
    // References between components
    // -----------------------------------------------------------------------

    /**
     * The LIFT ids that at least one component in the dictionary refers to.
     *
     * @return an unmodifiable view
     */
    public Set<String> getReferencedTargetIds() {
        return registers.referencedTargetIds();
    }

    /**
     * The components referring to the given LIFT id.
     *
     * @param targetId the LIFT id of the referenced component
     * @return an unmodifiable list, empty if nothing refers to {@code targetId}
     */
    public List<HasRefId> getReferencesTo(String targetId) {
        return registers.referencesTo(targetId);
    }

    /** Whether any component refers to the given LIFT id. */
    public boolean isReferenced(String targetId) {
        return registers.isReferenced(targetId);
    }

    // -----------------------------------------------------------------------
    // Lookup by LIFT id
    // -----------------------------------------------------------------------

    /**
     * The ID on a {@link HasRefId} component may point towards an entry or a sense.
     */
    public AbstractIdentifiable getEntryOrSenseByLiftId(String liftId) {
        boolean inEntries = registers.entriesByLiftId.containsKey(liftId);
        boolean inSenses = registers.sensesByLiftId.containsKey(liftId);
        if (inEntries && inSenses) {
            throw new IllegalStateException("Cannot have the same liftId for a sense and an entries");
        }
        if (inEntries) {
            return registers.entriesByLiftId.get(liftId);
        } else if (inSenses) {
            return registers.sensesByLiftId.get(liftId);
        } else {
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Entries
    // -----------------------------------------------------------------------

    private ObservableList<LiftEntry> entriesReadOnly;

    public ObservableList<LiftEntry> getEntries() {
        // In the particular case of entries, we do not use a list listening to the
        // xById map: the entries are held in a list of their own in order to keep the
        // order they have in the dictionary.
        if (entriesReadOnly == null) {
            entriesReadOnly =
                FXCollections.unmodifiableObservableList(registers.entries);
        }
        return entriesReadOnly;
    }

    private ObservableMap<UUID, LiftEntry> entriesByIdReadOnly;

    public Map<UUID, LiftEntry> getEntriesById() {
        if (entriesByIdReadOnly == null) {
            entriesByIdReadOnly =
                FXCollections.unmodifiableObservableMap(registers.entriesById);
        }
        return entriesByIdReadOnly;
    }

    public int nEntries() {
        return registers.entriesById.size();
    }

    // -----------------------------------------------------------------------
    // One observable list per kind of component
    // -----------------------------------------------------------------------

    private final Map<
      Class< ? extends AbstractLiftRoot>,
      ReadOnlyListWrapper<? extends AbstractLiftRoot>
    > observableList = new HashMap<>();

    @SuppressWarnings("unchecked")
    public ObservableList<LiftSense> getSenses() {
        if (!observableList.containsKey(LiftSense.class)) {
            this.<LiftSense>populateObservableList(LiftSense.class, registers.sensesById);
        }
        return (ObservableList<LiftSense>) observableList.get(LiftSense.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftExample> getExamples() {
        if (!observableList.containsKey(LiftExample.class)) {
            this.<LiftExample>populateObservableList(LiftExample.class, registers.examplesById);
        }
        return (ObservableList<LiftExample>) observableList.get(LiftExample.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftVariant> getVariants() {
        if (!observableList.containsKey(LiftVariant.class)) {
            this.<LiftVariant>populateObservableList(LiftVariant.class, registers.variantsById);
        }
        return (ObservableList<LiftVariant>) observableList.get(LiftVariant.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftTrait> getTraits() {
        if (!observableList.containsKey(LiftTrait.class)) {
            this.<LiftTrait>populateObservableList(LiftTrait.class, registers.traitsById);
        }
        return (ObservableList<LiftTrait>) observableList.get(LiftTrait.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftReversal> getReversals() {
        if (!observableList.containsKey(LiftReversal.class)) {
            this.<LiftReversal>populateObservableList(LiftReversal.class, registers.reversalsById);
        }
        return (ObservableList<LiftReversal>) observableList.get(LiftReversal.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftRelation> getRelations() {
        if (!observableList.containsKey(LiftRelation.class)) {
            this.<LiftRelation>populateObservableList(LiftRelation.class, registers.relationsById);
        }
        return (ObservableList<LiftRelation>) observableList.get(LiftRelation.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftPronunciation> getPronunciations() {
        if (!observableList.containsKey(LiftPronunciation.class)) {
            this.<LiftPronunciation>populateObservableList(LiftPronunciation.class, registers.pronunciationsById);
        }
        return (ObservableList<LiftPronunciation>) observableList.get(LiftPronunciation.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftNote> getNotes() {
        if (!observableList.containsKey(LiftNote.class)) {
            this.<LiftNote>populateObservableList(LiftNote.class, registers.notesById);
        }
        return (ObservableList<LiftNote>) observableList.get(LiftNote.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftMedia> getMedias() {
        if (!observableList.containsKey(LiftMedia.class)) {
            this.<LiftMedia>populateObservableList(LiftMedia.class, registers.mediasById);
        }
        return (ObservableList<LiftMedia>) observableList.get(LiftMedia.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftIllustration> getIllustrations() {
        if (!observableList.containsKey(LiftIllustration.class)) {
            this.<LiftIllustration>populateObservableList(LiftIllustration.class, registers.illustrationsById);
        }
        return (ObservableList<LiftIllustration>) observableList.get(LiftIllustration.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftField> getFields() {
        if (!observableList.containsKey(LiftField.class)) {
            this.<LiftField>populateObservableList(LiftField.class, registers.fieldsById);
        }
        return (ObservableList<LiftField>) observableList.get(LiftField.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftEtymology> getEtymologies() {
        if (!observableList.containsKey(LiftEtymology.class)) {
            this.<LiftEtymology>populateObservableList(LiftEtymology.class, registers.etymologiesById);
        }
        return (ObservableList<LiftEtymology>) observableList.get(LiftEtymology.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<GrammaticalInfo> getGrammaticalInfos() {
        if (!observableList.containsKey(GrammaticalInfo.class)) {
            this.<GrammaticalInfo>populateObservableList(GrammaticalInfo.class, registers.grammaticalInfosById);
        }
        return (ObservableList<GrammaticalInfo>) observableList.get(GrammaticalInfo.class).getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public ObservableList<LiftAnnotation> getAnnotations() {
        if (!observableList.containsKey(LiftAnnotation.class)) {
            this.<LiftAnnotation>populateObservableList(LiftAnnotation.class, registers.annotationsById);
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

    // -----------------------------------------------------------------------
    // Texts
    // -----------------------------------------------------------------------

    private ObservableList<MultiText> objectText = null;
    private ObservableList<MultiText> objectTextReadOnly = null;

    public ObservableList<MultiText> getObjectText() {
        if (objectText == null) {
            objectText = FXCollections.observableList(
                FXCollections.observableArrayList(
                    registers.objectTextById.values()
                )
            );
            registers.objectTextById.addListener(
                (MapChangeListener<UUID, MultiText>) change -> {
                    if (change.wasAdded()) {
                        objectText.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        objectText.remove(change.getValueRemoved());
                    }
                }
            );
            objectTextReadOnly = FXCollections.unmodifiableObservableList(objectText);
        }
        return objectTextReadOnly;
    }

    private ObservableList<MultiText> metaText = null;
    private ObservableList<MultiText> metaTextReadOnly = null;

    public ObservableList<MultiText> getMetaText() {
        if (metaText == null) {
            metaText = FXCollections.observableList(
                FXCollections.observableArrayList(
                    registers.metaTextById.values()
                )
            );
            registers.metaTextById.addListener(
                (MapChangeListener<UUID, MultiText>) change -> {
                    if (change.wasAdded()) {
                        metaText.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        metaText.remove(change.getValueRemoved());
                    }
                }
            );
            metaTextReadOnly = FXCollections.unmodifiableObservableList(metaText);
        }
        return metaTextReadOnly;
    }
}
