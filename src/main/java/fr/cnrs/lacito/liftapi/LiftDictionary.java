package fr.cnrs.lacito.liftapi;

import fr.cnrs.lacito.liftapi.builder.DictionaryBuilder;
import fr.cnrs.lacito.liftapi.model.Form;
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
import fr.cnrs.lacito.liftapi.model.TextSpan;
import fr.cnrs.lacito.liftapi.xml.LiftDictionaryXmlReader;
import fr.cnrs.lacito.liftapi.xml.LiftWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import lombok.Getter;
import lombok.Setter;

/**
 * This class represents a LIFT dictionary. It is the main entry point for the API.
 * 
 * It contains the header and the components of the dictionary, as well as some metadata about the dictionary (version, producer, etc.).
 * 
 * It also contains some methods to manipulate the dictionary, such as adding ids to entries, saving the dictionary, etc.
 *
 * It is designed to be immutable, but some setters are still present for convenience. They should be removed in the future
 * 
 * In order to create a dictionary, see the {@LiftDictionaryBuilder} class.
 * 
 */
public final class LiftDictionary {

    private static final Logger LOGGER = Logger.getLogger(
        LiftDictionary.class.getName()
    );

    private final LiftDictionaryLanguagesManager languageManager;

    @Getter
    protected LiftDictionaryComponents liftDictionaryComponents;

    @Getter
    public DictionaryBuilder componentBuilder;

    @Getter
    @Setter // setter should be removed
    protected String liftVersion;

    @Getter
    @Setter // setter should be removed
    protected String liftProducer;

    private File source;

    protected LiftHeader header;

    private LiftDictionaryRegistry registry;

    public static final LiftDictionary loadDictionaryFromFile(File f)
        throws LiftDocumentLoadingException {
        // the zero-argument constructor should be called
        LiftDictionary d = LiftDictionaryXmlReader.loadWithSax(f, false);
        // call finalizer (or whatever) on LiftXMLFactory
        // call discoverFields() on ValueManager, LangManager
        // update Header accordingly
        d.source = f;

        return d;
    }

    public static final LiftDictionaryBuilder makeBuilder() {
        return new LiftDictionaryBuilder();
    }

    /**
     * Save the dictionary at the location it was read.
     * @throws WrittingLiftDocumentException
     */
    public void save() throws WrittingLiftDocumentException {
        save(this.source);
    }

    /**
     * Save the dictionary at the given location.
     * @param f the File to read
     * @throws WrittingLiftDocumentException
     */
    public void save(File f) throws WrittingLiftDocumentException {
        LiftWriter liftWriter = null;
        try {
            liftWriter = new LiftWriter(f);
        } catch (FileNotFoundException e) {
            throw new WrittingLiftDocumentException(e);
        }

        try {
            liftWriter.marshall(this);
        } catch (FileNotFoundException fE) {
            throw new WrittingLiftDocumentException(fE);
        } catch (XMLStreamException xE) {
            throw new WrittingLiftDocumentException(xE);
        } catch (Exception e) {
            throw new WrittingLiftDocumentException(e);
        }
    }

    public LiftDictionary(LiftDictionaryComponents ldc) {
        this();
        LOGGER.info(
            "Dictionary created with " +
                ldc.getAllEntries().size() +
                " entries."
        );
        this.liftDictionaryComponents = ldc;
    }

    protected LiftDictionary() {
        registry = new LiftDictionaryRegistry();
        languageManager = new LiftDictionaryLanguagesManager(registry);
        componentBuilder = new DictionaryBuilder(registry);
    }

    public LiftDictionaryLanguagesManager getLanguageManager() {
        return languageManager;
    }

    public DictionaryBuilder getComponentBuilder() {
        return componentBuilder;
    }

    public void addIds() {
        // TODO
    }

    public void fillLexicalEntryOrderNumber() {
        // TODO
    }

    public int entryCount() {
        return this.liftDictionaryComponents.getAllEntries().size();
    }

    public Set<String> getObjectLanguagesInLexicalUnit() {
        Set<String> objectLanguages = new HashSet<>();
        for (LiftEntry e : this.liftDictionaryComponents.getAllEntries()) {
            // objectLanguages.addAll( ((Subfields)e.getAnnotationOrTraitOrField()).get_object_languages() );
            objectLanguages.addAll(e.getForms().getLangs());
        }
        return objectLanguages;
    }

    public Map<String, Long> getGramInfoCounter() {
        Map<String, Long> result = this.liftDictionaryComponents
            .getAllSenses()
            .stream()
            .filter(x -> x.getGrammaticalInfo().isPresent())
            .collect(
                Collectors.groupingBy(
                    x ->
                        x.getGrammaticalInfo().orElseThrow().getGramInfoValue(),
                    Collectors.counting()
                )
            );
        return result;
    }

    public Set<String> getGramInfoSet() {
        Set<String> gramInfoSet = new HashSet<>();
        for (LiftSense s : this.liftDictionaryComponents.getAllSenses()) {
            s.getGrammaticalInfo().ifPresent(gi ->
                gramInfoSet.add(gi.getGramInfoValue())
            );
        }
        return gramInfoSet;
    }

    public Set<String> getObjectLanguagesOfAllText() {
        return getLanguagesInAllField(
            this.liftDictionaryComponents.getAllObjectLanguagesMultiText()
        );
    }

    public Set<String> getMetaLanguagesOfAllText() {
        return getLanguagesInAllField(
            this.liftDictionaryComponents.getAllMetaLanguagesMultiText()
        );
    }

    public Set<String> getTraitName() {
        return this.liftDictionaryComponents
            .getAllTraits()
            .stream()
            .map(t -> t.getName())
            .collect(Collectors.toSet());
    }

    public Set<String> getFieldType() {
        return this.liftDictionaryComponents
            .getAllFields()
            .stream()
            .map(t -> t.getName())
            .collect(Collectors.toSet());
    }

    public Set<String> getTranslationType() {
        Set<String> result = new HashSet<>();
        for (LiftExample le : this.liftDictionaryComponents.getAllExamples()) {
            result.addAll(le.getTranslations().keySet());
        }
        return result;
    }

    public Map<String, Long> getValueCounterForTraitName(String traitName) {
        return this.liftDictionaryComponents
            .getAllTraits()
            .stream()
            .filter(t -> t.getName().equals(traitName))
            .collect(
                Collectors.groupingBy(x -> x.getValue(), Collectors.counting())
            );
    }

    public Set<String> getLangInObjectTextSpan() {
        List<MultiText> ms =
            this.liftDictionaryComponents.getAllObjectLanguagesMultiText();
        Set<String> langs = new HashSet<>();
        for (MultiText m : ms) {
            for (Form t : m.getForms()) {
                for (TextSpan ts : t.walkTextSpanTree()) {
                    if (ts.getLang().isPresent()) {
                        langs.add(ts.getLang().get());
                    }
                }
            }
        }
        return langs;
    }

    private Set<String> getLanguagesInAllField(List<MultiText> multiTexts) {
        Set<String> languages = new HashSet<>();
        for (MultiText m : multiTexts) {
            languages.addAll(m.getLangs());
        }
        return languages;
    }

    //    public void removeEntry(LiftEntry entry) {
    //        removeMultitext(entry.getForms());
    //        removeMultitext(entry.getCitations());
    //        for (LiftAnnotation a : entry.getAnnotations()) {
    //            removeAnnotation(a);
    //        }
    //        for (LiftField f : entry.getFields()) {
    //            removeField(f);
    //        }
    //        for (LiftEtymology e : entry.getEtymologies()) {
    //            removeEtymology(e);
    //        }
    //        for (LiftNote n : entry.getNotes()) {
    //            removeNote(n);
    //        }
    //        for (LiftPronunciation p : entry.getPronunciations()) {
    //            removePronunciation(p);
    //        }
    //        for (LiftRelation r : entry.getRelations()) {
    //            removeRelation(r);
    //        }
    //        for (LiftSense s : entry.getSenses()) {
    //            removeSense(s);
    //        }
    //        for (LiftTrait t : entry.getTraits()) {
    //            removeTrait(t);
    //        }
    //        for (LiftVariant v : entry.getVariants()) {
    //            removeVariant(v);
    //        }
    //        this.liftDictionaryComponents.getEntryById((entry.getId()).
    //            .stream()
    //            .filter(s -> s != entry);
    //    }

    // public void removeSense(LiftEntry parent, LiftSense sense) {}

    // public void removeExample(LiftSense parent, LiftExample example) {}
    // //public void removeMedia
    // //public void removeMedia
}
