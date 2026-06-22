package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftVariant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BuilderTest {

    LiftDictionary dictionary;

    @BeforeEach
    public void setUp() {
        this.dictionary = LiftDictionary.makeBuilder()
            .withLiftVersion("0.13")
            .withProducer("Test Producer")
            .build();
    }

    @Test
    public void testBuilderEntry() {
        DictionaryBuilder builder = dictionary.getComponentBuilder();
        LiftEntry entry = builder
            .entry()
            .withForm("en", "dictionary")
            .addSense(s ->
                s
                    .withGloss("en", "reference book")
                    .withDefinition("en", "A book of words and definitions")
            )
            .build();
    }

    @Test
    public void testBuilderCompleteEntryWithMultipleSenses() {
        LiftEntry word = dictionary
            .getComponentBuilder()
            .entry()
            .withId("word-001")
            .withForm("en", "run")
            .withForm("fr", "courir")
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
                            .withExample("en", "She runs every morning")
                            .addTranslation(
                                "litteral",
                                "fr",
                                "Elle court chaque matin"
                            )
                    )
            )
            .addSense(s ->
                s
                    .withOrder(2)
                    .withGloss("en", "to manage or operate")
                    .withPartOfSpeech("verb")
            )
            .addPronunciation(p -> p.withPronunciation("en", "rʌn"))
            .addNote("source", "en", "From Old English 'irnan'")
            .build();
    }

    @Test
    public void testBuilderVariant() {
        LiftVariant variant = dictionary
            .getComponentBuilder()
            .variant()
            .withForm("en", "ran")
            .withForm("fr", "courait")
            .build();
    }

    @Test
    public void testBuilderQuickEntry() {
        LiftEntry quick = dictionary
            .getComponentBuilder()
            .entry("en", "dog", "en", "a domesticated canine");
    }

    @Test
    public void testBuilderProgrammaticBuildingWithLoops() {
        EntryBuilder entry = dictionary.getComponentBuilder().entry();
        String[] languages = { "en", "fr", "es" };
        String word = "run";
        for (String lang : languages) {
            entry.withForm(lang, word);
        }
        entry.build();
    }
}
