package fr.cnrs.lacito.liftapi;

import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
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
import fr.cnrs.lacito.liftapi.model.MultiTextMetaLanguage;
import fr.cnrs.lacito.liftapi.model.MultiTextObjectLanguage;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LiftDictionaryRegistry {

    private LiftDictionaryFeatureManager counter = null;
    private final LiftDictionaryUUIDManager uuidManager = new LiftDictionaryUUIDManager();

    private final Map<UUID, LiftEntry> entriesById = new HashMap<>();
    private final Map<UUID, LiftSense> sensesById = new HashMap<>();
    private final Map<UUID, LiftExample> examplesById = new HashMap<>();
    private final Map<UUID, LiftVariant> variantsById = new HashMap<>();
    private final Map<UUID, LiftTrait> traitsById = new HashMap<>();
    private final Map<UUID, LiftReversal> reversalsById = new HashMap<>();
    private final Map<UUID, LiftRelation> relationsById = new HashMap<>();
    private final Map<UUID, LiftPronunciation> pronunciationsById = new HashMap<>();
    private final Map<UUID, LiftNote> notesById = new HashMap<>();
    private final Map<UUID, LiftMedia> mediasById = new HashMap<>();
    private final Map<UUID, LiftIllustration> illustrationsById = new HashMap<>();
    private final Map<UUID, LiftField> fieldsById = new HashMap<>();
    private final Map<UUID, LiftEtymology> etymologiesById = new HashMap<>();
    private final Map<UUID, LiftAnnotation> annotationsById = new HashMap<>();
    private final Map<UUID, MultiText> objectTextById = new HashMap<>();
    private final Map<UUID, MultiText> metaTextById = new HashMap<>();

	private final SimpleListProperty<LiftEntry> entries;
    private final SimpleListProperty<LiftSense> senses;
    private final SimpleListProperty<LiftExample> examples;
    private final SimpleListProperty<LiftVariant> variants;
    private final SimpleListProperty<LiftTrait> traits;
    private final SimpleListProperty<LiftReversal> reversals;
    private final SimpleListProperty<LiftRelation> relations;
    private final SimpleListProperty<LiftPronunciation> pronunciations;
    private final SimpleListProperty<LiftNote> notes;
    private final SimpleListProperty<LiftMedia> medias;
    private final SimpleListProperty<LiftIllustration> illustrations;
    private final SimpleListProperty<LiftField> fields;
    private final SimpleListProperty<LiftEtymology> etymologies;
    private final SimpleListProperty<LiftAnnotation> annotations;
    private final SimpleListProperty<MultiText> objectText;
    private final SimpleListProperty<MultiText> metaText;
    
    protected LiftDictionaryRegistry() {
        entries = new SimpleListProperty<LiftEntry>(FXCollections.observableArrayList(entriesById.values()));
        senses = new SimpleListProperty<>(  FXCollections.observableArrayList(sensesById.values()));
        examples = new SimpleListProperty<>(FXCollections.observableArrayList(examplesById.values()));
        variants = new SimpleListProperty<>(FXCollections.observableArrayList(variantsById.values()));
        traits = new SimpleListProperty<>(FXCollections.observableArrayList(traitsById.values()));
        reversals = new SimpleListProperty<>(FXCollections.observableArrayList(reversalsById.values()));
        relations = new SimpleListProperty<>(FXCollections.observableArrayList(relationsById.values()));
        pronunciations = new SimpleListProperty<>(FXCollections.observableArrayList(pronunciationsById.values()));
        notes = new SimpleListProperty<>(FXCollections.observableArrayList(notesById.values()));
        medias = new SimpleListProperty<>(FXCollections.observableArrayList(mediasById.values()));
        illustrations = new SimpleListProperty<>(FXCollections.observableArrayList(illustrationsById.values()));
        fields = new SimpleListProperty<>(FXCollections.observableArrayList(fieldsById.values()));
        etymologies = new SimpleListProperty<>(FXCollections.observableArrayList(etymologiesById.values()));
        annotations = new SimpleListProperty<>(FXCollections.observableArrayList(annotationsById.values()));
        objectText = new SimpleListProperty<>(FXCollections.observableArrayList(objectTextById.values()));
        metaText = new SimpleListProperty<>(FXCollections.observableArrayList(metaTextById.values()));

        // should be created at the end of the initialisation of objects of constructor
        // because rely on other object (such as metaLanguagesProperty)
    }

    public LiftDictionaryFeatureManager getCounter() {
        if (counter == null) {
            counter = new LiftDictionaryFeatureManager(this);
        }
        return counter;
    }

    public void register(AbstractLiftRoot node) {
        switch (node) {
            case LiftEntry e -> entriesById.put(e.getUUID(), e);
            case LiftSense s -> sensesById.put(s.getUUID(), s);
            case LiftExample o -> examplesById.put(o.getUUID(), o);
            case LiftVariant o -> variantsById.put(o.getUUID(), o);
            case LiftTrait o -> traitsById.put(o.getUUID(), o);
            case LiftReversal o -> reversalsById.put(o.getUUID(), o);
            case LiftRelation o -> relationsById.put(o.getUUID(), o);
            case LiftPronunciation o -> pronunciationsById.put(o.getUUID(), o);
            case LiftNote o -> notesById.put(o.getUUID(), o);
            case LiftMedia o -> mediasById.put(o.getUUID(), o);
            case LiftIllustration o -> illustrationsById.put(o.getUUID(), o);
            case LiftField o -> fieldsById.put(o.getUUID(), o);
            case LiftEtymology o -> etymologiesById.put(o.getUUID(), o);
            case LiftAnnotation o -> annotationsById.put(o.getUUID(), o);
            default -> throw new IllegalStateException("Unknown type: " + node.getClass());
        }
    }

    //    case MultiText o -> objectTextById.put(o.getUUID(), o);
    //    case MultiText o -> metaTextById.put(o.getUUID(), o);

    protected void unregister(AbstractLiftRoot node) {
        switch (node) {
            case LiftEntry e -> entriesById.remove(e.getUUID());
            case LiftSense s -> sensesById.remove(s.getUUID());
            case LiftExample o -> examplesById.remove(o.getUUID());
            case LiftVariant o -> variantsById.remove(o.getUUID());
            case LiftTrait o -> traitsById.remove(o.getUUID());
            case LiftReversal o -> reversalsById.remove(o.getUUID());
            case LiftRelation o -> relationsById.remove(o.getUUID());
            case LiftPronunciation o -> pronunciationsById.remove(o.getUUID());
            case LiftNote o -> notesById.remove(o.getUUID());
            case LiftMedia o -> mediasById.remove(o.getUUID());
            case LiftIllustration o -> illustrationsById.remove(o.getUUID());
            case LiftField o -> fieldsById.remove(o.getUUID());
            case LiftEtymology o -> etymologiesById.remove(o.getUUID());
            case LiftAnnotation o -> annotationsById.remove(o.getUUID());
            default -> throw new IllegalStateException("Unknown type: " + node.getClass());
        }
    }

    public void register(MultiTextObjectLanguage element) {
        objectTextById.put(element.getUUID(), element);
    }

    public void register(MultiTextMetaLanguage element) {
        metaTextById.put(element.getUUID(), element);
    }

    public void remove(LiftEntry entry) {
        if (!entriesById.containsKey(entry.getUUID())) {
            throw new IllegalArgumentException("Entry not found in registry: " + entry.getUUID());
        }
        entriesById.remove(entry.getUUID());
        //entry.removeFromRegistry(this);
        for (LiftSense sense : entry.getSenses()) {
            remove(sense);
        }
        // TODO etc
    }

    public void remove(LiftSense sense) {
        sensesById.remove(sense.getUUID());
    }

    public ListProperty<LiftEntry> entriesProperty() {
        return entries;
    }

    public ListProperty<LiftSense> sensesProperty() {
        return senses;
    }

    public ListProperty<LiftExample> examplesProperty() {
        return examples;
    }

    public ListProperty<LiftVariant> variantsProperty() {
        return variants;
    }

    public ListProperty<LiftTrait> traitsProperty() {
        return traits;
    }

    public ListProperty<LiftReversal> reversalsProperty() {
        return reversals;
    }

    public ListProperty<LiftRelation> relationsProperty() {
        return relations;
    }

    public ListProperty<LiftPronunciation> pronunciationsProperty() {
        return pronunciations;
    }

    public ListProperty<LiftNote> notesProperty() {
        return notes;
    }

    public ListProperty<LiftMedia> mediasProperty() {
        return medias;
    }

    public ListProperty<LiftIllustration> illustrationsProperty() {
        return illustrations;
    }

    public ListProperty<LiftField> fieldsProperty() {
        return fields;
    }

    public ListProperty<LiftEtymology> etymologiesProperty() {
        return etymologies;
    }

    public ListProperty<LiftAnnotation> annotationsProperty() {
        return annotations;
    }

    public ListProperty<MultiText> objectTextProperty() {
        return objectText;
    }

    public ListProperty<MultiText> metaTextProperty() {
        return metaText;
    }

    public ObservableList<LiftEntry> getEntries() {
        return entries.get();
    }

    public ObservableList<LiftSense> getSenses() {
        return senses.get();
    }

    public ObservableList<LiftExample> getExamples() {
        return examples.get();
    }

    public ObservableList<LiftVariant> getVariants() {
        return variants.get();
    }

    public ObservableList<LiftTrait> getTraits() {
        return traits.get();
    }

    public ObservableList<LiftReversal> getReversals() {
        return reversals.get();
    }

    public ObservableList<LiftRelation> getRelations() {
        return relations.get();
    }

    public ObservableList<LiftPronunciation> getPronunciations() {
        return pronunciations.get();
    }

    public ObservableList<LiftNote> getNotes() {
        return notes.get();
    }

    public ObservableList<LiftMedia> getMedias() {
        return medias.get();
    }

    public ObservableList<LiftIllustration> getIllustrations() {
        return illustrations.get();
    }

    public ObservableList<LiftField> getFields() {
        return fields.get();
    }

    public ObservableList<LiftEtymology> getEtymologies() {
        return etymologies.get();
    }

    public ObservableList<LiftAnnotation> getAnnotations() {
        return annotations.get();
    }

    public ObservableList<MultiText> getObjectText() {
        return objectText.get();
    }

    public ObservableList<MultiText> getMetaText() {
        return metaText.get();
    }

    public UUID getNewUUID() {
        return uuidManager.getUniqueUuid();
    }


}
