package fr.cnrs.lacito.liftapi;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.MultiText;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.TextSpan;
import fr.cnrs.lacito.liftapi.xml.LiftWriter;
import lombok.Getter;
import lombok.Setter;

public final class LiftDictionary {
    
    private static final Logger LOGGER = Logger.getLogger(LiftDictionary.class.getName());
    @Getter protected LiftDictionaryComponents liftDictionaryComponents;
    @Getter @Setter private String liftVersion;
    @Getter @Setter private String liftProducer;
    @Getter @Setter private File source;

    /**
     * Prevent instantiation
     */
    private LiftDictionary() {
        
    }

    public final static LiftDictionary loadWithFile(File f) throws LiftDocumentLoadingException {
        LiftDictionary d = LiftDictionaryLoader.loadWithSax(f, false);
        d.setSource(f);
        return d;
    }

    /**
     * Save the dictionary at the location it was read.
     * @throws WritingLiftDocumentException
     */
    public void save() throws WritingLiftDocumentException {
        save(this.source);
    }

    /** 
     * Save the dictionary at the given location.
     * @param f the File to read
     * @throws WritingLiftDocumentException
     */
    public void save(File f) throws WritingLiftDocumentException {
        if (f == null) {
            throw new IllegalArgumentException("Cannot save LiftDictionary: no file provided. Please provide a file to save the dictionary to.");
        }
        try {
            LiftWriter liftWriter = new LiftWriter(f);
            liftWriter.marshall(this);
        } catch (FileNotFoundException e) {
            LOGGER.severe("Error while writing: " + e.getMessage());
            throw new WritingLiftDocumentException(e);
        } catch (XMLStreamException xE) {
            LOGGER.severe("Error while writing: " + xE.getMessage());
            throw new WritingLiftDocumentException(xE);
        } catch (Exception e) {
            LOGGER.severe("Error while writing: " + e.getMessage());
            throw new WritingLiftDocumentException(e);
        }
    }

    protected LiftDictionary(LiftDictionaryComponents ldc) {
        LOGGER.info("Dictionary created with " + ldc.getAllEntries().size() + " entries.");
        this.liftDictionaryComponents = ldc;
    }

    public void addIds() {
        // TODO. Should be part of a normalize() method that would also add missing fields, traits, etc. and check the consistency of the dictionary.
        throw new UnsupportedOperationException("Not implemented yet: addIds");
    }

    public void fillLexicalEntryOrderNumber() {
        // TODO. Should be part of a normalize() method that would also add missing fields, traits, etc. and check the consistency of the dictionary.
        throw new UnsupportedOperationException("Not implemented yet: fillLexicalEntryOrderNumber");
    }

    public int getEntryCount() {
        return this.liftDictionaryComponents.getAllEntries().size();
    }

    public Set<String> getObjectLanguagesInLexicalUnit() {
        Set<String> objectLanguages = new HashSet<>();
        for (LiftEntry e : this.liftDictionaryComponents.getAllEntries()) {
            // objectLanguages.addAll( ((Subfields)e.getAnnotationOrTraitOrField()).get_object_languages() );
            objectLanguages.addAll( e.getForms().getLangs() );
        }
        return objectLanguages;
    }

    public Map<String, Long> getGramInfoCounter() {
        Map<String, Long> result = this.liftDictionaryComponents.getAllSenses().stream()
            .filter(x -> x.getGrammaticalInfo().isPresent())
            .collect(
                Collectors.groupingBy(
                    x -> x.getGrammaticalInfo().orElseThrow().getGramInfoValue(),
                    Collectors.counting()
                )
            );
        return result;
    }

    public Set<String> getGramInfoSet() {
        Set<String> gramInfoSet = new HashSet<>();
        for (LiftSense s : this.liftDictionaryComponents.getAllSenses()) {
            s.getGrammaticalInfo().ifPresent(
                gi -> gramInfoSet.add(gi.getGramInfoValue())
            );
        }
        return gramInfoSet;
    }

    public Set<String> getObjectLanguagesOfAllText() {
        return getLanguagesInAllField(this.liftDictionaryComponents.getAllObjectLanguagesMultiText());        
    }

    public Set<String> getMetaLanguagesOfAllText() {
        return getLanguagesInAllField(this.liftDictionaryComponents.getAllMetaLanguagesMultiText());
    }

    public Set<String> getTraitNames() {
        return this.liftDictionaryComponents.getAllTraits().stream()
            .map(t -> t.getName())
            .collect(Collectors.toSet());
    }

    public Set<String> getFieldTypes() {
        return this.liftDictionaryComponents.getAllFields().stream()
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
        return this.liftDictionaryComponents.getAllTraits().stream()
            .filter(t -> t.getName().equals(traitName))
            .collect(
                Collectors.groupingBy(
                    x -> x.getValue(),
                    Collectors.counting()
                )
            );
    }

    public Set<String> getLanguagesInObjectTextSpan() {
        List<MultiText> ms = this.liftDictionaryComponents.getAllObjectLanguagesMultiText();
        Set<String> langs = new HashSet<>();
        for (MultiText m : ms) {
            for (Form t: m.getForms()) {
                for (TextSpan ts : t.walkTextSpanTree()) {
                    if (ts.getLang().isPresent()) {
                        langs.add(ts.getLang().get());
                    }
                }
            }
        }
        return langs;
    }

    private Set<String> getLanguagesInAllField(List<MultiText> multiTexts ) {
        Set<String> languages = new HashSet<>();
        for (MultiText m : multiTexts) {
            languages.addAll(m.getLangs());
        }
        return languages;
    }

}
