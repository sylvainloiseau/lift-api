package fr.cnrs.lacito.liftapi.builder;

import org.junit.Test;

import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftVariant;

public class BuilderTest {

    @Test
    public void testBuilderEntry() {
        LiftEntry entry = Builders.entry()
            .withForm("en", "dictionary")
            .addSense(s -> s
                .withGloss("en", "reference book")
                .withDefinition("en", "A book of words and definitions")
            )
            .build();        
        
    }

    @Test
    public void testBuilderCompleteEntryWithMultipleSenses() {
        LiftEntry word = Builders.entry()
            .withId("word-001")
            .withForm("en", "run")
            .withForm("fr", "courir")
            .addSense(s -> s
                .withOrder(1)
                .withGloss("en", "to move quickly on foot")
                .withDefinition("en", "To move at a pace faster than walking")
                .withPartOfSpeech("verb")
                .addExample(ex -> ex
                    .withExample("en", "She runs every morning")
                    .addTranslation("litteral", "fr", "Elle court chaque matin")
                )
            )
            .addSense(s -> s
                .withOrder(2)
                .withGloss("en", "to manage or operate")
                .withPartOfSpeech("verb")
            )
            .addPronunciation(p -> p
                .withPronunciation("en", "rʌn")
            )
            .addNote("source", "en", "From Old English 'irnan'")
            .build();
    }

    @Test
    public void testBuilderVariant() {
        LiftVariant variant = Builders.variant()
            .withForm("en", "ran")
            .withForm("fr", "courait")
            .build();
    }

    @Test
    public void testBuilderQuickEntry() {
        LiftEntry quick = Builders.entry("dog", "a domesticated canine");
    }

    @Test
    public void testBuilderProgrammaticBuildingWithLoops() {
        EntryBuilder entry = Builders.entry();
        String[] languages = {"en", "fr", "es"};
        String word = "run";
        for (String lang : languages) {
            entry.withForm(lang, word);
        }
        entry.build();
    }
}
