package fr.cnrs.lacito.liftapi.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftVersion;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionDataModel;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionKind;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionTarget;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The builders accept what the LIFT schema accepts, and can express every component a
 * file can contain: they are the only code that creates components, the XML reader
 * included.
 */
public class SchemaLevelBuildersTest {

    LiftDictionary dictionary;
    DictionaryComponentBuilderFactory b;

    @BeforeEach
    public void setUp() {
        dictionary = LiftDictionary.makeBuilder()
            .withLiftVersion(LiftVersion.V0_15)
            .withProducer("Test Producer")
            .withMetaLanguages("en")
            .withObjectLanguages("tww")
            .build();
        b = dictionary.getComponentBuilder();
    }

    // ------------------------------------------------------------------
    // No editorial checks
    // ------------------------------------------------------------------

    @Test
    public void anEntryWithoutLexicalUnitCanBeBuilt() {
        LiftEntry entry = b.entry().build();
        assertTrue(entry.getForms().isEmpty());
        assertTrue(dictionary.getLiftDictionaryRegistry().getEntries().contains(entry));
    }

    @Test
    public void aSenseWithoutGlossOrDefinitionCanBeBuilt() {
        LiftEntry entry = b.entry().withForm("tww", "nala").build();
        LiftSense sense = b.sense(entry).build();
        assertSame(entry, sense.getParent());
    }

    @Test
    public void anExampleWithoutTextCanBeBuilt() {
        LiftEntry entry = b.entry().withForm("tww", "nala").addSense(s -> {}).build();
        LiftExample example = b.example(entry.getSenses().get(0)).build();
        assertTrue(example.getExample().isEmpty());
    }

    @Test
    public void schemaRequirementsAreStillEnforced() {
        LiftEntry entry = b.entry().withForm("tww", "nala").addSense(s -> {}).build();
        assertThrows(IllegalArgumentException.class, () -> b.etymology(entry, "borrowed", null));
        assertThrows(IllegalArgumentException.class, () -> b.relation(null, entry));
        assertThrows(IllegalArgumentException.class, () -> b.illustration(entry.getSenses().get(0), null));
    }

    // ------------------------------------------------------------------
    // Field definitions
    // ------------------------------------------------------------------

    @Test
    public void aFieldDefinitionIsBuiltIntoTheHeader() {
        FeatureSet colours = b.range("colours")
            .addFeature(f -> f.withLabel("en", "red"), "red")
            .build();
        LiftFieldAndTraitDefinition def = b.fieldDefinition("colour")
            .withDataModel("option")
            .withTargets("entry sense")
            .withOptionRange(colours)
            .withWritingSystem("en")
            .withLabel("en", "Colour")
            .withDescription("en", "The colour of the thing")
            .build();

        assertSame(def, dictionary.getHeader().getFieldsAndTraitsDefinitions("colour"));
        assertEquals(LiftFieldAndTraitDefinitionKind.TRAIT, def.getKind(), "an option implies a trait");
        assertEquals(Optional.of(LiftFieldAndTraitDefinitionDataModel.FEATURE), def.getDataModel());
        assertEquals(
            Set.of(LiftFieldAndTraitDefinitionTarget.ENTRY, LiftFieldAndTraitDefinitionTarget.SENSE),
            def.getTargets()
        );
        assertEquals(Optional.of(colours), def.getResolvedFeatureSet());
        assertEquals("Colour", def.getLabel().getForm("en").orElseThrow().toPlainText());
    }

    @Test
    public void aSecondDefinitionOfTheSameNameIsRefused() {
        b.fieldDefinition("colour").build();
        assertThrows(IllegalArgumentException.class, () -> b.fieldDefinition("colour").build());
    }

    @Test
    public void aBuilderCannotBeBuiltTwice() {
        FieldDefinitionBuilder fdb = b.fieldDefinition("colour");
        fdb.build();
        assertThrows(IllegalStateException.class, fdb::build);
    }

    // ------------------------------------------------------------------
    // Trait values: one parser
    // ------------------------------------------------------------------

    @Test
    public void aTraitBuilderDeclaresAnUnknownFeature() {
        FeatureSet colours = b.range("colours").build();
        b.fieldDefinition("colour").withDataModel("option-collection").withOptionRange(colours).build();
        LiftEntry entry = b.entry()
            .addTrait("colour", "red, light blue")
            .build();
        LiftTrait trait = entry.getTraits().get(0);
        assertEquals(2, trait.featureSetValueProperty().size());
        assertEquals(Set.of("red", "light blue"), colours.getFeatures().keySet());
    }

    @Test
    public void settingAValueOnATraitRefusesAnUnknownFeature() {
        FeatureSet colours = b.range("colours").addFeature(f -> {}, "red").build();
        b.fieldDefinition("colour").withDataModel("option").withOptionRange(colours).build();
        LiftEntry entry = b.entry().addTrait("colour", "red").build();
        LiftTrait trait = entry.getTraits().get(0);
        assertThrows(IllegalArgumentException.class, () -> trait.setValue("blue"));
        assertEquals(Set.of("red"), colours.getFeatures().keySet());
    }
}
