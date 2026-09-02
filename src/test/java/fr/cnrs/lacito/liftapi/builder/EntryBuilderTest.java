package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.xml.LiftVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EntryBuilderTest {

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

}
