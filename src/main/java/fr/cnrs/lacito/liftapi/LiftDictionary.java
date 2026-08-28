package fr.cnrs.lacito.liftapi;

import fr.cnrs.lacito.liftapi.builder.DictionaryComponentBuilderFactory;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
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
/// Methods are distributed between this class and other classes in the same package
/// (such as [LiftDictionaryRegistry], [DictionaryComponentBuilderFactory], [LiftDictionaryLanguagesManager], ...)
/// whose singleton instance is accessible from here through [LiftDictionary#getLiftDictionaryRegistry)],
/// [LiftDictionary#getComponentBuilder()],
/// [LiftDictionary#getObjectLanguageManager()] and [LiftDictionary#getMetaLanguageManager()], etc.
/// 
/// Functionalities include:
/// 
/// - create dictionary from an XML document ([LiftDictionary#loadDictionaryFromFile(File f)]) or from scratch ([LiftDictionary#makeBuilder()])
/// - add components to the dictionary with the fluent API ([LiftDictionary#getComponentBuilder()]), delete components ([LiftDictionaryRegistry#removeFromDictionary(AbstractLiftRoot node)])
/// - lookup into dictionary content ([LiftDictionary#getEntryByForm(String lang, String form)], [LiftDictionary#searchInMetaLanguage(String lang, String searched)], [LiftDictionary#searchInObjectLanguage(String lang, String searched)])
/// - manage languages ([LiftDictionary#getObjectLanguageManager()], [LiftDictionary#getMetaLanguageManager()])
///
/// Internaly, components of the dictionary are linked in two ways:
/// 
/// - with links from parent node to child node and from child node to parent
/// - by managing list and map of components of a given type
/// 
/// Creating, adding or removing a component from the dictionary implies taking
/// care of these two aspects.
public final class LiftDictionary {

    // Constants

    protected LiftVersion DEFAULT_VERSION = LiftVersion.V0_15;
    protected String DEFAULT_PRODUCER = "fr.cnrs.lacito.liftapi";

    // Utils

    private static final Logger LOGGER = Logger.getLogger(
        LiftDictionary.class.getName()
    );

    // Fields

    private final DictionaryComponentBuilderFactory componentBuilder;

    private File source;

    protected final LiftHeader header;

    private LiftDictionaryRegistry registry;

    protected LiftVersion liftVersion = DEFAULT_VERSION;

    protected String liftProducer = DEFAULT_PRODUCER;

    private LiftDictionaryLanguagesManager objectLanguagesManager =
        new LiftDictionaryLanguagesManager();

    private LiftDictionaryLanguagesManager metaLanguagesManager =
        new LiftDictionaryLanguagesManager();

    // TODO Ugly hack n°1
    public void turnOffLanguageManager() {
        registry.setLanguagesManager(null, null);
    }

    // TODO Ugly hack n°1
    public void turnOnLanguageManager() {
        registry.setLanguagesManager(objectLanguagesManager, metaLanguagesManager);
    }

    // Getters/Setters

    public DictionaryComponentBuilderFactory getComponentBuilder() {
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
        // System.out.println("returning:" + objectLanguagesManager);
        return this.objectLanguagesManager;
    }

    public LiftDictionaryLanguagesManager getMetaLanguageManager() {
        // System.out.println("returning:" + metaLanguagesManager);
        return this.metaLanguagesManager;
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
        LiftDictionary d = new LiftDictionary();

        LiftDictionaryXmlReader r = new LiftDictionaryXmlReader(
            f,
            d,
            false
        );
        r.parse();
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
        this.registry = new LiftDictionaryRegistry();
        registry.setLanguagesManager(objectLanguagesManager, metaLanguagesManager);
        this.header = new LiftHeader();
        this.componentBuilder = new DictionaryComponentBuilderFactory(this);
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
        return this.registry.getTraits()
            .stream()
            .map(t -> t.getDefinition().getName())
            .collect(Collectors.toSet());
    }

    public Set<LiftFieldAndTraitDefinition> getFieldType() {
        return this.registry.getFields()
            .stream()
            .map(t -> t.getType())
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
        return this.registry.getTraits()
            .stream()
            .filter(t -> t.getDefinition().getName().equals(traitName))
            .collect(
                Collectors.groupingBy(x -> x.getValue(), Collectors.counting())
            );
    }

    public Set<String> getLangInObjectTextSpan() {
        List<MultiText> ms =
            this.registry.getObjectText();
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
