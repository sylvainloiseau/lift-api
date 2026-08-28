package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftVersion;
import fr.cnrs.lacito.liftapi.model.LiftEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BuilderTest {

    LiftDictionary dictionary;

    @BeforeEach
    public void setUp() {
        this.dictionary = LiftDictionary.makeBuilder()
            .withLiftVersion(LiftVersion.V0_13)
            .withProducer("Test Producer")
            .withMetaLanguages("en", "fr")
            .withObjectLanguages("tww", "tpi")
            .build();
    }

    @Test
    public void testBuilderEntry() {
        DictionaryComponentBuilderFactory builder = dictionary.getComponentBuilder();
        builder
            .entry()
            .withForm("tww", "nofua")
            .addSense(s ->
                s
                    .withGloss("en", "book")
                    .withDefinition("en", "Any printed or written material")
            )
            .build();
            assertEquals(1, dictionary.getLiftDictionaryRegistry().getEntries().size());
    }

    @Test
    public void testBuilderCompleteEntryWithMultipleSenses() {
        dictionary
            .getComponentBuilder()
            .entry()
            .withId("word-001")
            .withForm("tww", "honolu")
            .withForm("tpi", "ran")
            .addSense(s ->
                s
                    .withOrder(1)
                    .withGloss("en", "to move quickly on foot")
                    .withDefinition(
                        "en",
                        "To move at a pace faster than walking"
                    )
                    .withPartOfSpeech("verb")
                    .addExample(ex ->
                        ex
                            .withExample("tww", "mwe molunomwij")
                            .addTranslation(
                                "litteral",
                                "fr",
                                "Il s'enfuit"
                            )
                    )
            )
            .addSense(s ->
                s
                    .withOrder(2)
                    .withGloss("en", "to manage or operate")
                    .withPartOfSpeech("verb")
            )
            .addPronunciation(p -> p.withPronunciation("tww", "honolu"))
            .addNote("source", "en", "From Old English 'irnan'")
            .build();
    }

    @Test
    public void testBuilderVariant() {
        LiftEntry entry = dictionary
            .getComponentBuilder()
            .entry()
            .withId("word-001")
            .withForm("tww", "esejle")
            .build();

        dictionary
            .getComponentBuilder()
            .variant(entry)
            .withRefId(entry.getId().get())
            .withForm("tww", "eseile")
            //.withForm("tww", "esijle")
            .build();
    }

    @Test
    public void testBuilderQuickEntry() {
        dictionary
            .getComponentBuilder()
            .entry("tww", "heifo", "en", "dog");
    }

    @Test
    public void testBuilderProgrammaticBuildingWithLoops() {
        dictionary.getObjectLanguageManager().addLanguage("en");
        dictionary.getObjectLanguageManager().addLanguage("fr");
        dictionary.getObjectLanguageManager().addLanguage("es");
        EntryBuilder entry = dictionary.getComponentBuilder().entry();
        String[] languages = {"en", "fr", "es"};
        String word = "run";
        for (String lang : languages) {
            entry.withForm(lang, word);
        }
        entry.build();
    }

    @Test
    public void testEtymology() {
        LiftEntry e = dictionary
            .getComponentBuilder()
            .entry("tww", "heifo", "en", "a domesticated canine");
        dictionary.getComponentBuilder().etymology(e, "source", "Maiden 2004").build();
        assertEquals(1, e.getEtymologies().size());
        assertEquals(1, dictionary.getLiftDictionaryRegistry().getEtymologies().size());
    }

    @Test
    public void testAddEtymology() {
        DictionaryComponentBuilderFactory builder = dictionary.getComponentBuilder();
        LiftEntry entry = builder
            .entry()
            .withForm("tww", "dictionary")
            .addSense(s ->
                s
                    .withGloss("en", "reference book")
                    .withDefinition("en", "A book of words and definitions")
            )
            .addEtymology(s -> s.addForm("tww", "foo"), "x", "y")
            .build();
            assertEquals(1, dictionary.getLiftDictionaryRegistry().getEntries().size());
            assertEquals(1, entry.getEtymologies().size());
            assertEquals(1, dictionary.getLiftDictionaryRegistry().getEtymologies().size());
    }

    @Test
    public void testAutomaticallyAddTranslationType() {
        DictionaryComponentBuilderFactory builder = dictionary.getComponentBuilder();

        // No Translation type so far
        assertEquals(0, dictionary.getHeader().getTranslationTypeManager().getFeatures().values().size());

        builder
            .entry()
            .withForm("tww", "nofua")
            .addSense(s ->
                s
                    .withGloss("en", "book")
                    .withDefinition("en", "Any book")
                    .addExample(e -> e
                        .withExample("tww", "a nofuafo lomwij")
                        .addTranslation(
                            "litteral",
                            "fr",
                            "J'ai un livre"
                        )
                    )
            )
            .build();
            assertEquals(1, dictionary.getLiftDictionaryRegistry().getEntries().size());
            assertEquals(1, dictionary.getLiftDictionaryRegistry().getSenses().size());
            assertEquals(1, dictionary.getLiftDictionaryRegistry().getExamples().size());
            assertEquals(1, dictionary.getHeader().getTranslationTypeManager().getFeatures().values().size());
    }

    @Test
    public void testBuildExampleWithTrait() {
        DictionaryComponentBuilderFactory builder = dictionary.getComponentBuilder();

        dictionary.getHeader().getOrCreateTraitsDefinitions("foo");
        builder
            .entry()
            .withForm("tww", "nofua")
            .addSense(s ->
                s
                    .withGloss("en", "book")
                    .withDefinition("en", "Any book")
                    .addExample(e -> e
                        .withExample("tww", "nofua-fo")
                        .addTrait("foo", "bar"))
            )
            .build();

            assertEquals(1, dictionary.getLiftDictionaryRegistry().getEntries().size());
            assertEquals(1, dictionary.getLiftDictionaryRegistry().getTraits().size());
            assert(
                dictionary.getHeader().containsFieldsAndTraitsDefinitions("foo")
            );
            assertEquals("bar", dictionary.getLiftDictionaryRegistry().getTraits().get(0).getValue());

    }
}
