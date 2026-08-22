package fr.cnrs.lacito.liftapi;

import fr.cnrs.lacito.liftapi.builder.DictionaryObjectBuilderFactory;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.MultiText;
import fr.cnrs.lacito.liftapi.model.TextSpan;
import fr.cnrs.lacito.liftapi.xml.LiftDictionaryXmlReader;
import fr.cnrs.lacito.liftapi.xml.LiftWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;

/// The entry point for working with a LIFT dictionary. 
/// 
/// - create dictionary from XML ([loadDictionaryFromFile]) or from scratch ([makeBuilder])
/// - update dictionary with the fluent API ([getComponentBuilder]), delete components ([LiftDictionaryRegistry#removeFromDictionary])
/// - access dictionary content ([getEntryByForm], [searchInMetaLanguage], [searchInObjectLanguage])
/// - manage languages ([getObjectLanguageManager], [getMetaLanguageManager])

public final class LiftDictionary {

    // Constants

    protected LiftVersion DEFAULT_VERSION = LiftVersion.V0_15;
    protected String DEFAULT_PRODUCER = "fr.cnrs.lacito.liftapi";

    // Utils

    private static final Logger LOGGER = Logger.getLogger(
        LiftDictionary.class.getName()
    );

    // Fields

    private final DictionaryObjectBuilderFactory componentBuilder;

    private File source;

    protected final LiftHeader header;

    private LiftDictionaryRegistry registry;

    protected LiftVersion liftVersion = DEFAULT_VERSION;

    protected String liftProducer = DEFAULT_PRODUCER;

    private final LiftDictionaryLanguagesManager objectLanguagesManager =
        new LiftDictionaryLanguagesManager();

    private final LiftDictionaryLanguagesManager metaLanguagesManager =
        new LiftDictionaryLanguagesManager();

    // Getters/Setters

    public DictionaryObjectBuilderFactory getComponentBuilder() {
        return componentBuilder;
    }

    public LiftVersion getLiftVersion() {
        return liftVersion;
    }

    public void setLiftVersion(LiftVersion liftVersion) {
        if (liftVersion == null) {
            throw new IllegalArgumentException("liftVersion cannot be null");
        }
        this.liftVersion = liftVersion;
    }

    public LiftHeader getHeader() {
        return this.header;
    }

    public LiftDictionaryRegistry getLiftDictionaryRegistry() {
        return registry;
    }

    public LiftDictionaryLanguagesManager getObjectLanguageManager() {
        return objectLanguagesManager;
    }

    public LiftDictionaryLanguagesManager getMetaLanguageManager() {
        return metaLanguagesManager;
    }

    public String getLiftProducer() {
        return liftProducer;
    }

    public void setLiftProducer(String liftProducer) {
        if (liftProducer == null) {
            throw new IllegalArgumentException("liftProducer cannot be null");
        }
        this.liftProducer = liftProducer;
    }

    // Static methods for instantiation

    public static final LiftDictionary loadDictionaryFromFile(File f)
        throws LiftDocumentLoadingException {
        //long size = f.length();
        //long dictionarySizeUnits = (int) (size / 1024 / 1024);
        // the zero-argument constructor should be called
        LiftDictionaryRegistry registry = new LiftDictionaryRegistry();
        LiftDictionaryXmlReader r = new LiftDictionaryXmlReader(
            f,
            registry,
            false
        );
        r.parse();
        LiftHeader header = r.getHeader();
        LiftDictionary d = new LiftDictionary(
            registry,
            header
        );
        d.source = f;
        LOGGER.info(
            "Dictionary created with " +
                d.getLiftDictionaryRegistry().nEntries() +
                " entries."
        );
        return d;
    }

    public static final LiftDictionaryBuilder makeBuilder() {
        return new LiftDictionaryBuilder();
    }

    // Constructors

    protected LiftDictionary() {
        this(new LiftDictionaryRegistry(), new LiftHeader());
    }

    protected LiftDictionary(
        LiftDictionaryRegistry registry,
        LiftHeader header
    ) {
        this.registry = registry;
        registry.setLanguagesManager(objectLanguagesManager, metaLanguagesManager);
        this.header = header;
        this.componentBuilder = new DictionaryObjectBuilderFactory(this);
    }

    // Public methods

    /**
     * Save the dictionary at the location it was read.
     * @throws WrittingLiftDocumentException
     */
    public void save() throws WrittingLiftDocumentException {
        if (this.source == null) {
            throw new WrittingLiftDocumentException("No source file known");
        }
        save(this.source);
    }

    /**
     * Save the dictionary at the given location using Lift-XML vocabulary.
     * @param f the File to be writed
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

    // Utility methods

    @Deprecated
    public int entryCount() {
        return this.registry.entriesById.size();
    }

    public Set<String> getObjectLanguagesInLexicalUnit() {
        Set<String> objectLanguages = new HashSet<>();
        for (LiftEntry e : this.registry.getEntries()) {
            // objectLanguages.addAll( ((Subfields)e.getAnnotationOrTraitOrField()).get_object_languages() );
            objectLanguages.addAll(e.getForms().getLangs());
        }
        return objectLanguages;
    }

    public Map<String, Long> getGramInfoCounter() {
        Map<String, Long> result = this.registry.getSenses()
            .stream()
            .filter(x -> x.getGrammaticalInfo().isPresent())
            .collect(
                Collectors.groupingBy(
                    x ->
                        x.getGrammaticalInfo().orElseThrow().getGramInfoValue().getId(),
                    Collectors.counting()
                )
            );
        return result;
    }

    public Set<String> getGramInfoSet() {
        Set<String> gramInfoSet = new HashSet<>();
        for (LiftSense s : this.registry.getSenses()) {
            s.getGrammaticalInfo().ifPresent(gi ->
                gramInfoSet.add(gi.getGramInfoValue().getId())
            );
        }
        return gramInfoSet;
    }

    public Set<String> getTraitName() {
        return this.registry.getTraitsReadOnly()
            .stream()
            .map(t -> t.getDefinition().getName())
            .collect(Collectors.toSet());
    }

    public Set<LiftFieldAndTraitDefinition> getFieldType() {
        return this.registry.getFieldsReadOnly()
            .stream()
            .map(t -> t.getName())
            .collect(Collectors.toSet());
    }

    // public Set<LiftHeaderRangeElement> getTranslationType() {
    //     Set<LiftHeaderRangeElement> result = new HashSet<>();
    //     for (LiftExample le : this.registry.getExamples()) {
    //         result.addAll(le.getTranslations().keySet());
    //     }
    //     return result;
    // }

    public Map<String, Long> getValueCounterForTraitName(String traitName) {
        return this.registry.getTraitsReadOnly()
            .stream()
            .filter(t -> t.getDefinition().getName().equals(traitName))
            .collect(
                Collectors.groupingBy(x -> x.getValue(), Collectors.counting())
            );
    }

    public Set<String> getLangInObjectTextSpan() {
        List<MultiText> ms =
            this.registry.getObjectTextReadOnly();
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

    // access to content of the dictionary

    public List<LiftEntry> getEntryByForm(String lang, String form) {
        return registry.entriesById
            .values()
            .stream()
            .filter(x -> x.getForms().containsLang(lang))
            .filter(x ->
                x.getForms().getForm(lang).get().textProperty().get().equals(form)
            )
            .toList();
    }

    public List<MultiText> searchInMetaLanguage(String lang, String searched) {
        return searchInLanguage(lang, searched, registry.metaTextById);
    }

    public List<MultiText> searchInObjectLanguage(
        String lang,
        String searched
    ) {
        return searchInLanguage(lang, searched, registry.objectTextById);
    }

    private List<MultiText> searchInLanguage(
        String lang,
        String searched,
        Map<UUID, MultiText> texts
    ) {
        if (lang == null) throw new IllegalArgumentException(
            "lang must not be null"
        );
        if (searched == null) throw new IllegalArgumentException(
            "searched string must not be null"
        );
        return texts
            .values()
            .stream()
            .filter(x -> x.containsLang(lang))
            .filter(x -> x.getForm(lang).get().toPlainText().matches(searched))
            .toList();
    }

}
