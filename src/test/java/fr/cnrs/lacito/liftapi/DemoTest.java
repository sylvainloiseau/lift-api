package fr.cnrs.lacito.liftapi;

import fr.cnrs.lacito.liftapi.builder.DictionaryComponentBuilderFactory;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.LiftVersion;

import java.io.File;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DemoTest {

    LiftDictionary dictionary;

    @Test
    public void testDemo() {
        // 1/ Creating a dictionary
        // ----------------------------------------------------------------------
        // 1.1/ by reading a file:
        // ----------------------------------------------------------------------
        LiftDictionary lf = null;
        try {
            lf = LiftDictionary.loadDictionaryFromFile(new File("src/test/resources/lift/tiny.xml"));
        } catch (LiftDocumentLoadingException e) {
            e.printStackTrace();
        }

        // ----------------------------------------------------------------------
        // 1.2/ using the fluent api:
        // ----------------------------------------------------------------------
        this.dictionary = LiftDictionary.makeBuilder()
            .withLiftVersion(LiftVersion.V0_13)
            .withProducer("Test Producer")
            .withMetaLanguages("en", "fr")
            .withObjectLanguages("en", "fr")
            .build();

        // 2/ Adding new data with the fluent API
        //
        // ----------------------------------------------------------------------
        // 2.1/ Adding entries, (top-level element)
        // ----------------------------------------------------------------------
        //
        DictionaryComponentBuilderFactory builder = dictionary.getComponentBuilder();
        LiftEntry entry = builder
            .entry()
            .withForm("en", "dictionary")
            .addSense(s ->
                s
                    .withGloss("en", "reference book")
                    .withDefinition("en", "A book of words and definitions")
            )
            .build();

        // The complete data model of lift is accessible:
        builder
            .entry()
            .withId("word-001")
            .withForm("en", "run")
            .withForm("fr", "courir")
            .addNote(
                n -> n.addText("fr", "to be checked"),
                "Comment note"
            )
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

        // ----------------------------------------------------------------------
        // 2.2/ Adding component to entries or any other already existing element
        // ----------------------------------------------------------------------

        // In order to add a component on an existing component -- such as a sense on an entry,
        // first fetch the parent component (here, the entry), then use the
        // fluent API for the child component, making reference to the parent.

        assertEquals(2, dictionary.getLiftDictionaryRegistry().getEntries().size());

        LiftEntry e = dictionary.getEntryByForm("fr", "courir").get(0);
        builder.sense(e)
            .withGloss("en", "to run")
            .withDefinition("en", "xyz")
            .build();

        // 3. Searching the dictionary
        // Extensive tools for accessing dictionary content
        // meta languages (used in all multitext containing descriptive content)
        dictionary.getLiftDictionaryRegistry().getObjectText();

        // For all component types, a list can be retrieved:
        dictionary.getLiftDictionaryRegistry().getEntries();
        dictionary.getLiftDictionaryRegistry().getIllustrations();
        // etc.

    }

}
