package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The texts of the header - labels, abbreviations and descriptions of feature sets,
 * features and definitions, and the description of the header - are registered as
 * meta-language texts, as every other meta-language text is: their languages are
 * declared and counted, and a form in an undeclared language is refused. The components
 * themselves are not indexed.
 */
public class HeaderTextsTest {

    private static LiftDictionary dictionary() {
        return LiftDictionary.makeBuilder()
            .withLiftVersion(LiftVersion.V0_15)
            .withProducer("HeaderTextsTest")
            .withMetaLanguages("en")
            .withObjectLanguages("tww")
            .build();
    }

    private static LiftDictionary load(Path dir, String xml) throws Exception {
        File file = dir.resolve("header.lift").toFile();
        Files.writeString(file.toPath(), xml);
        return LiftDictionary.loadDictionaryFromFile(file);
    }

    private static final String FILE = """
        <?xml version="1.0" encoding="UTF-8"?>
        <lift version="0.15" producer="test">
        <header>
        <description><form lang="de"><text>Ein Wörterbuch</text></form></description>
        <ranges>
        <range id="colours">
        <label><form lang="fr"><text>Couleurs</text></form></label>
        <range-element id="red"><label><form lang="es"><text>rojo</text></form></label></range-element>
        </range>
        <range id="note-type">
        <range-element id="source"><abbrev><form lang="it"><text>fonte</text></form></abbrev></range-element>
        </range>
        </ranges>
        <fields>
        <field-definition name="colour" type="option" option-range="colours">
        <label><form lang="pt"><text>cor</text></form></label>
        </field-definition>
        </fields>
        </header>
        <entry id="e1"><lexical-unit><form lang="tww"><text>nala</text></form></lexical-unit></entry>
        </lift>
        """;

    @Test
    public void theLanguagesOfTheHeaderAreDeclaredAndCountedOnLoad(@TempDir Path dir) throws Exception {
        LiftDictionary d = load(dir, FILE);
        LiftDictionaryLanguagesManager meta = d.getMetaLanguageManager();
        // The header description, a new range, one of its elements, an element of a
        // built-in range (note-type, which exists before the file is read), a definition.
        for (String lang : new String[] {"de", "fr", "es", "it", "pt"}) {
            assertTrue(meta.hasLanguage(lang), lang + " should be declared");
            assertEquals(1, meta.getLanguageOccurrence(lang), lang);
        }
        assertFalse(d.getObjectLanguageManager().hasLanguage("fr"), "header texts are meta-language");
    }

    @Test
    public void aFormInAnUndeclaredLanguageIsRefusedOnceLoaded(@TempDir Path dir) throws Exception {
        LiftDictionary d = load(dir, FILE);
        Feature red = d.getHeader().getFeatureSet("colours").getFeature("red");
        assertThrows(IllegalArgumentException.class, () -> red.getLabel().add(new Form("xx", "?")));
        red.getLabel().add(new Form("fr", "rouge"));
        assertEquals(2, d.getMetaLanguageManager().getLanguageOccurrence("fr"));
    }

    @Test
    public void theTextsOfANewFeatureSetAndOfItsFeaturesAreRegistered() {
        LiftDictionary d = dictionary();
        FeatureSet colours = d.getComponentBuilder().range("colours")
            .withLabel("en", "Colours")
            .addFeature(f -> f.withLabel("en", "red"), "red")
            .build();
        Feature red = colours.getFeature("red");
        assertNotNull(colours.getLabel().getUUID());
        assertNotNull(red.getLabel().getUUID());
        assertTrue(d.getLiftDictionaryRegistry().getMetaText().contains(red.getLabel()));
        assertEquals(2, d.getMetaLanguageManager().getLanguageOccurrence("en"));
        assertNull(colours.getUUID(), "the header components themselves are not indexed");

        Feature blue = colours.addFeature("blue");
        assertNotNull(blue.getLabel().getUUID(), "a feature added later is registered too");
        assertThrows(IllegalArgumentException.class, () -> blue.getLabel().add(new Form("xx", "?")));
    }

    @Test
    public void featuresAddedToASetNotYetInTheHeaderWaitForIt() {
        LiftDictionary d = dictionary();
        FeatureSet colours = new FeatureSet("colours", d.getHeader());
        Feature red = colours.addFeature("red");
        red.getLabel().add(new Form("xx", "red"));
        assertNull(red.getLabel().getUUID());

        d.getHeader().addFeatureSet(colours);
        assertNotNull(red.getLabel().getUUID());
        assertTrue(d.getMetaLanguageManager().hasLanguage("xx"), "the languages are declared on registration");
    }

    @Test
    public void removingAFeatureSetReleasesItsTexts() {
        LiftDictionary d = dictionary();
        FeatureSet colours = d.getComponentBuilder().range("colours")
            .withLabel("en", "Colours")
            .addFeature(f -> f.withLabel("en", "red"), "red")
            .build();
        Feature red = colours.getFeature("red");
        d.getHeader().removeFeatureSet(colours);

        assertNull(colours.getLabel().getUUID());
        assertNull(red.getLabel().getUUID());
        assertFalse(d.getLiftDictionaryRegistry().getMetaText().contains(red.getLabel()));
        assertEquals(0, d.getMetaLanguageManager().getLanguageOccurrence("en"));
        red.getLabel().add(new Form("xx", "free again"));

        red.getLabel().removeForm("xx");
        d.getHeader().addFeatureSet(colours);
        assertEquals(2, d.getMetaLanguageManager().getLanguageOccurrence("en"), "put back, counted again");
    }

    @Test
    public void removingAFeatureOrADefinitionReleasesItsTexts() {
        LiftDictionary d = dictionary();
        FeatureSet colours = d.getComponentBuilder().range("colours")
            .addFeature(f -> f.withLabel("en", "red"), "red")
            .build();
        LiftFieldAndTraitDefinition def = d.getComponentBuilder().fieldDefinition("colour")
            .withLabel("en", "Colour").build();
        assertEquals(2, d.getMetaLanguageManager().getLanguageOccurrence("en"));

        Feature red = colours.removeFeature("red");
        d.getHeader().removeFieldOrTraitDefinition(def);
        assertNull(red.getLabel().getUUID());
        assertNull(def.getLabel().getUUID());
        assertEquals(0, d.getMetaLanguageManager().getLanguageOccurrence("en"));
    }
}
