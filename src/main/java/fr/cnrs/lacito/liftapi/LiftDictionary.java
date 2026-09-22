package fr.cnrs.lacito.liftapi;

import fr.cnrs.lacito.liftapi.builder.DictionaryComponentBuilderFactory;
import fr.cnrs.lacito.liftapi.internal.DictionaryMutator;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.MultiText;
import fr.cnrs.lacito.liftapi.model.TextSpan;
import fr.cnrs.lacito.liftapi.xml.LiftDictionaryXmlReader;
import fr.cnrs.lacito.liftapi.xml.LiftWriterSession;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/// The entry point for working with a LIFT dictionary.
///
/// Methods are distributed between this class and other classes in the same package
/// (such as [LiftDictionaryRegistry], [DictionaryComponentBuilderFactory], [LiftDictionaryLanguagesManager], ...).
/// Each dictionary owns one instance of each of them - they are not singletons and must
/// not be shared between dictionaries - reachable from here through [LiftDictionary#getLiftDictionaryRegistry()],
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

    private final LiftDictionaryCounterManager counter;

    private final DictionaryMutator mutator;

    // TODO Ugly hack n°1
    public void turnOffLanguageManager() {
        registry.setLanguagesManager(null, null);
    }

    // TODO Ugly hack n°1
    public void turnOnLanguageManager() {
        registry.setLanguagesManager(objectLanguagesManager, metaLanguagesManager);
    }

    // Getters/Setters

    public LiftDictionaryCounterManager getCounter() {
        return counter;
    }

    /**
     * The mutation core of this dictionary.
     *
     * <b>Not public API.</b> {@link DictionaryMutator} lives in a package this module
     * does not export, so this method is unreachable from outside the library - javac
     * rejects any use of the returned value, including through {@code var}. It is
     * {@code public} only so that the {@code builder} and {@code xml} packages can
     * reach it from inside the module.
     */
    public DictionaryMutator getMutator() {
        return mutator;
    }

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
        return this.objectLanguagesManager;
    }

    public LiftDictionaryLanguagesManager getMetaLanguageManager() {
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
        this.mutator = new DictionaryMutator(this.registry);
        registry.setLanguagesManager(objectLanguagesManager, metaLanguagesManager);
        this.header = new LiftHeader();
        this.componentBuilder = new DictionaryComponentBuilderFactory(this);
        counter = new LiftDictionaryCounterManager(this);
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
        // try-with-resources so that a failure inside close() is suppressed onto the
        // real cause rather than replacing it. The session only promotes its
        // temporary file over `f` when marshalling completed.
        try (LiftWriterSession liftWriter = new LiftWriterSession(f)) {
            liftWriter.marshall(this);
        } catch (Exception e) {
            throw new WrittingLiftDocumentException(e);
        }
    }

    // Utility methods

    @Deprecated
    public int entryCount() {
        return this.registry.entriesById.size();
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

    public Map<String, Long> getValueCounterForTraitName(String traitName) {
        return this.registry.getTraits()
            .stream()
            .filter(t -> t.getSpecification().getName().equals(traitName))
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

    public List<MultiText> searchInMetaLanguage(String lang, String regexp) {
        return searchInLanguage(lang, regexp, registry.metaTextById);
    }

    public List<MultiText> searchInObjectLanguage(
        String lang,
        String regexp
    ) {
        return searchInLanguage(lang, regexp, registry.objectTextById);
    }

    private List<MultiText> searchInLanguage(
        String lang,
        String regexp,
        Map<UUID, MultiText> texts
    ) {
        if (lang == null) throw new IllegalArgumentException(
            "lang must not be null"
        );
        if (regexp == null) throw new IllegalArgumentException(
            "searched string must not be null"
        );
        Pattern p = Pattern.compile(regexp);
        return texts
            .values()
            .stream()
            .filter(x -> x.containsLang(lang))
            .filter(x -> p.matcher(x.getForm(lang).get().toPlainText()).matches())
            .toList();
    }

}
