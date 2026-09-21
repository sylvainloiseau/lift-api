package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftSense;
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
        LiftEntry entry = dictionary
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

        assertEquals("honolu", entry.getForms().getForm("tww").get().toPlainText());
        assertEquals("ran", entry.getForms().getForm("tpi").get().toPlainText());

        assertEquals(2, entry.getSenses().size());
        LiftSense first = entry.getSenses().get(0);
        assertEquals(1, first.getOrder().get());
        assertEquals(
            "to move quickly on foot",
            first.getGlosses().getForm("en").get().toPlainText()
        );
        assertEquals(
            "To move at a pace faster than walking",
            first.getDefinition().getForm("en").get().toPlainText()
        );
        assertEquals(
            "verb",
            first.getGrammaticalInfo().get().getGramInfoValue().getId()
        );
        assertEquals(1, first.getExamples().size());
        assertEquals(
            "mwe molunomwij",
            first.getExamples().get(0).getExample().getForm("tww").get().toPlainText()
        );

        assertEquals(2, entry.getSenses().get(1).getOrder().get());

        assertEquals(1, entry.getPronunciations().size());
        assertEquals(
            "honolu",
            entry
                .getPronunciations()
                .get(0)
                .getPronunciation()
                .getForm("tww")
                .get()
                .toPlainText()
        );

        assertEquals(
            "From Old English 'irnan'",
            entry.getNote("source").getText().getForm("en").get().toPlainText()
        );

        assertEquals(1, dictionary.getLiftDictionaryRegistry().getEntries().size());
        assertEquals(2, dictionary.getLiftDictionaryRegistry().getSenses().size());
    }

}
