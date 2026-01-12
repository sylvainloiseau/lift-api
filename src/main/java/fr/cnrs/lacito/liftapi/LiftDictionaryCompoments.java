package fr.cnrs.lacito.liftapi;

import java.util.List;
import java.util.Map;

import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.LiftIllustration;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftVariant;
import fr.cnrs.lacito.liftapi.model.MultiText;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftMedia;

/**
 * Convenient and efficient access to various data type of the dictionary.
 */
public interface LiftDictionaryCompoments {

    public LiftHeader getHeader();

    public List<LiftEntry> getAllEntries();

    public Map<String, LiftEntry> getEntryById();

    public List<LiftEntry> getEntryWithoutId();

    public List<LiftSense> getAllSenses();

    public Map<String, LiftSense> getSenseById();

    public List<LiftSense> getSenseWithoutId();

    public List<LiftAnnotation> getAllAnnotations();

    public List<LiftNote> getAllNotes();

    public List<LiftPronunciation> getAllPronunciations();

    public List<LiftField> getAllFields();

    public List<LiftTrait> getAllTraits();

    public List<MultiText> getAllObjectLanguagesMultiText();

    public List<MultiText> getAllMetaLanguagesMultiText();

    public List<LiftRelation> getAllRelations();

    public List<LiftExample> getAllExamples();

    public List<LiftVariant> getAllVariants();

    public List<LiftMedia> getAllMedias();

    public List<LiftIllustration> getAllIllustrations();

}
