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
    @Getter protected LiftDictionaryCompoments liftDictionaryComponents;
    @Getter @Setter private String liftVersion;
    @Getter @Setter private String liftProducer;
    @Getter @Setter private File source;
    
    public final static LiftDictionary loadDictionaryWithFile(File f) throws LiftDocumentLoadingException {
        LiftDictionary d = LiftDictionaryLoader.LoadWithSax(f, false);
        d.setSource(f);
        return d;
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
            liftWriter =  new LiftWriter(f);
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

    protected LiftDictionary(LiftDictionaryCompoments ldc) {
        LOGGER.info("Dictionary created with " + ldc.getAllEntries().size() + " entries.");
        this.liftDictionaryComponents = ldc;
    }

    public void addIds() {
        // TODO
    }

    public void fillLexicalEntryOrderNumber() {
        // TODO
    }

    public int n_entries() {
        return this.liftDictionaryComponents.getAllEntries().size();
    }

    public Set<String> get_object_languages_in_lexical_unit() {
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

    public Set<String> getTraitName() {
        return this.liftDictionaryComponents.getAllTraits().stream()
            .map(t -> t.getName())
            .collect(Collectors.toSet());
    }

    public Set<String> getFieldType() {
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

    public Set<String> getLangInObjectTextSpan() {
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
