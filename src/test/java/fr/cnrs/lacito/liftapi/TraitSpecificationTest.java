package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fr.cnrs.lacito.liftapi.change.ChangeRecorder;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionKind;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.RangeReference;
import fr.cnrs.lacito.liftapi.model.UndeclaredName;

/**
 * What the name of a trait or a field refers to: a definition, a range, or nothing
 * ({@link fr.cnrs.lacito.liftapi.model.TraitSpecification}), and the rule that keeps it
 * what the name resolves to as the header changes - the model in memory being what a save
 * and a reload give.
 */
public class TraitSpecificationTest {

    private static final File CORPUS = new File("src/test/resources/lift/20240828Lift.lift");

    private LiftDictionary dictionary;
    private LiftHeader header;

    @BeforeEach
    public void setUp() {
        dictionary = LiftDictionary.makeBuilder()
            .withLiftVersion(LiftVersion.V0_15)
            .withProducer("Test Producer")
            .withMetaLanguages("en")
            .withObjectLanguages("tww")
            .build();
        header = dictionary.getHeader();
    }

    private LiftEntry entryWithTrait(String name, String value) {
        return dictionary.getComponentBuilder().entry()
            .withForm("tww", "nala")
            .addTrait(name, value)
            .build();
    }

    // ------------------------------------------------------------------
    // The corpus
    // ------------------------------------------------------------------

    /**
     * The corpus names 9 of its traits after ranges (morph-type...), and declares none of
     * them: their values used to be strings of an invented definition, invisible to the
     * uses of the features and written back in the header as declarations.
     */
    @Test
    public void theTraitsOfTheCorpusNamedAfterARangeHoldItsFeatures() throws Exception {
        LiftDictionary corpus = LiftDictionary.loadDictionaryFromFile(CORPUS);
        LiftHeader corpusHeader = corpus.getHeader();
        FeatureSet morphTypes = corpusHeader.getFeatureSet("morph-type");

        List<LiftTrait> morphTypeTraits = corpus.getLiftDictionaryRegistry().getTraits().stream()
            .filter(t -> t.getName().equals("morph-type"))
            .toList();
        assertFalse(morphTypeTraits.isEmpty());
        for (LiftTrait trait : morphTypeTraits) {
            assertEquals(new RangeReference(morphTypes), trait.getSpecification());
            assertSame(morphTypes, ((Feature) trait.getTypedValue()).getParentFeatureSet());
        }
        assertFalse(corpusHeader.containsFieldsAndTraitsDefinitions("morph-type"));

        Feature stem = morphTypes.getFeature("stem");
        assertTrue(corpusHeader.countUses(stem) > 0, "the traits count as uses of their feature");
        assertThrows(IllegalStateException.class, () -> morphTypes.removeFeature("stem"));
        assertTrue(corpusHeader.isUsed(morphTypes));
    }

    /** The header holds what the file declares, no more: nothing is invented on load. */
    @Test
    public void theCorpusHeaderHoldsTheDeclaredDefinitionsOnly() throws Exception {
        LiftDictionary corpus = LiftDictionary.loadDictionaryFromFile(CORPUS);
        String text = Files.readString(CORPUS.toPath());
        String fields = text.substring(text.indexOf("<fields>"), text.indexOf("</fields>"));
        Set<String> declared = new TreeSet<>();
        Matcher m = Pattern.compile("<field-definition name=\"([^\"]+)\"").matcher(fields);
        while (m.find()) {
            declared.add(m.group(1));
        }
        Set<String> held = corpus.getHeader().getFieldsAndTraitsDefinitions().stream()
            .map(LiftFieldAndTraitDefinition::getName)
            .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(declared, held);
        // The undeclared fields stay undeclared.
        assertTrue(corpus.getLiftDictionaryRegistry().getFields().stream()
            .anyMatch(f -> f.getSpecification().equals(new UndeclaredName("summary"))));
    }

    // ------------------------------------------------------------------
    // Resolution
    // ------------------------------------------------------------------

    @Test
    public void aTraitNameResolvesToItsDefinitionThenItsRangeThenNothing() {
        FeatureSet colours = header.addFeatureSet("colours");
        assertEquals(new RangeReference(colours), header.resolveTraitSpecification("colours"));
        assertEquals(new UndeclaredName("size"), header.resolveTraitSpecification("size"));

        // A declaration says what the values are: it wins over the range of its name.
        LiftFieldAndTraitDefinition declared = header.createTraitDefinition("colours");
        assertSame(declared, header.resolveTraitSpecification("colours"));
    }

    @Test
    public void aDefinitionForTheOtherKindOfComponentIsNotTaken() {
        FeatureSet colours = header.addFeatureSet("colours");
        LiftFieldAndTraitDefinition forFields = header.createFieldDefinition("colours");
        assertEquals(new RangeReference(colours), header.resolveTraitSpecification("colours"));
        assertSame(forFields, header.resolveFieldSpecification("colours"));

        LiftFieldAndTraitDefinition forTraits = header.createTraitDefinition("size");
        assertSame(forTraits, header.resolveTraitSpecification("size"));
        assertEquals(new UndeclaredName("size"), header.resolveFieldSpecification("size"));

        LiftFieldAndTraitDefinition forBoth = header.createUnknownDefinition("comment");
        assertSame(forBoth, header.resolveTraitSpecification("comment"));
        assertSame(forBoth, header.resolveFieldSpecification("comment"));
    }

    @Test
    public void aFieldNameNeverResolvesToARange() {
        header.addFeatureSet("colours");
        assertEquals(new UndeclaredName("colours"), header.resolveFieldSpecification("colours"));
    }

    @Test
    public void theBuildersResolveWithoutDeclaring() {
        FeatureSet colours = header.addFeatureSet("colours");
        LiftEntry entry = dictionary.getComponentBuilder().entry()
            .withForm("tww", "nala")
            .addTrait("colours", "red")
            .addTrait("size", "big")
            .addField("summary", "en", "A summary")
            .build();

        assertEquals(new RangeReference(colours), entry.getTraits().get(0).getSpecification());
        assertSame(colours.getFeature("red"), entry.getTraits().get(0).getTypedValue());
        assertEquals(new UndeclaredName("size"), entry.getTraits().get(1).getSpecification());
        assertEquals("big", entry.getTraits().get(1).getTypedValue());
        assertEquals(new UndeclaredName("summary"), entry.getFields().get("summary").getSpecification());
        assertTrue(header.getFieldsAndTraitsDefinitions().isEmpty());
    }

    // ------------------------------------------------------------------
    // Saving and reading back
    // ------------------------------------------------------------------

    @Test
    public void rangeAndUndeclaredNamesAreReadBackAsTheyWereAndDeclareNothing(@TempDir Path dir) throws Exception {
        FeatureSet colours = header.addFeatureSet("colours");
        dictionary.getComponentBuilder().entry()
            .withForm("tww", "nala")
            .addTrait("colours", "red")
            .addTrait("size", "big")
            .addField("summary", "en", "A summary")
            .build();

        File saved = dir.resolve("saved.lift").toFile();
        dictionary.save(saved);
        String written = Files.readString(saved.toPath());
        assertFalse(written.contains("field-definition"), written);

        LiftDictionary reloaded = LiftDictionary.loadDictionaryFromFile(saved);
        LiftEntry entry = reloaded.getLiftDictionaryRegistry().getEntries().get(0);
        FeatureSet reloadedColours = reloaded.getHeader().getFeatureSet("colours");
        assertEquals(new RangeReference(reloadedColours), entry.getTraits().get(0).getSpecification());
        assertEquals("red", entry.getTraits().get(0).getValue());
        assertEquals(new UndeclaredName("size"), entry.getTraits().get(1).getSpecification());
        assertEquals(new UndeclaredName("summary"), entry.getFields().get("summary").getSpecification());
        assertTrue(reloaded.getHeader().getFieldsAndTraitsDefinitions().isEmpty());
        assertNotSame(colours, reloadedColours);
    }

    /**
     * The traits of a range element are read before the definitions, whose option-range
     * is set at the end of the header: they are bound to their definition then.
     */
    @Test
    public void aHeaderTraitTakesTheOptionDefinitionDeclaredAfterIt(@TempDir Path dir) throws Exception {
        File file = dir.resolve("test.lift").toFile();
        Files.writeString(file.toPath(), """
            <?xml version="1.0" encoding="UTF-8"?>
            <lift version="0.15" producer="test">
            <header>
            <ranges>
            <range id="tense"><range-element id="pst"><trait name="register" value="formal"/></range-element></range>
            <range id="registers"><range-element id="formal"/></range>
            </ranges>
            <fields>
            <field-definition name="register" type="option" option-range="registers"/>
            </fields>
            </header>
            </lift>
            """);
        LiftDictionary d = LiftDictionary.loadDictionaryFromFile(file);
        LiftHeader h = d.getHeader();
        LiftTrait trait = h.getFeatureSet("tense").getFeature("pst").getTraits().get(0);
        assertSame(h.getFieldsAndTraitsDefinitions("register"), trait.getSpecification());
        assertSame(h.getFeatureSet("registers").getFeature("formal"), trait.getTypedValue());
    }

    // ------------------------------------------------------------------
    // Rebinding
    // ------------------------------------------------------------------

    /**
     * Declaring the name of traits named after a range replaces them by traits of the
     * definition, in place, with their annotations; undoing it puts back the old ones.
     */
    @Test
    public void declaringTheNameOfTraitsRebindsThemAndCanBeUndone() {
        FeatureSet colours = header.addFeatureSet("colours");
        LiftEntry entry = dictionary.getComponentBuilder().entry()
            .withForm("tww", "nala")
            .addTrait("size", "big")
            .addTrait("colours", "red", t -> t.addAnnotation("checked", "yes"))
            .build();
        LiftTrait before = entry.getTraits().get(1);

        LiftFieldAndTraitDefinition declared;
        ChangeRecorder recorder = dictionary.recordChanges();
        declared = header.declareTraitDefinition("colours");

        LiftTrait after = entry.getTraits().get(1);
        assertNotSame(before, after);
        assertSame(declared, after.getSpecification());
        assertEquals("red", after.getTypedValue(), "a STRING definition: the feature id becomes a string");
        assertEquals(1, after.getAnnotations().size());
        assertTrue(before.getAnnotations().isEmpty());
        assertEquals(2, entry.getTraits().size());

        recorder.close();   // not committed: reverts
        assertFalse(header.containsFieldsAndTraitsDefinitions("colours"));
        assertSame(before, entry.getTraits().get(1));
        assertEquals(new RangeReference(colours), before.getSpecification());
        assertEquals(1, before.getAnnotations().size());
    }

    @Test
    public void addingARangeTurnsTheUndeclaredTraitsOfItsNameIntoFeatures() {
        LiftEntry entry = entryWithTrait("colours", "red");
        FeatureSet colours = header.addFeatureSet("colours");
        LiftTrait trait = entry.getTraits().get(0);
        assertEquals(new RangeReference(colours), trait.getSpecification());
        assertSame(colours.getFeature("red"), trait.getTypedValue(), "the missing feature is created");
    }

    @Test
    public void renamingARangeRenamesItsTraitsAndTheyAreReadBackToIt(@TempDir Path dir) throws Exception {
        FeatureSet colours = header.addFeatureSet("colours");
        LiftEntry entry = entryWithTrait("colours", "red");
        LiftEntry other = entryWithTrait("tints", "blue");

        header.renameFeatureSet(colours, "tints");
        assertEquals("tints", entry.getTraits().get(0).getName());
        assertEquals(new RangeReference(colours), other.getTraits().get(0).getSpecification(),
            "the undeclared traits of the new name join the range");

        File saved = dir.resolve("saved.lift").toFile();
        dictionary.save(saved);
        LiftDictionary reloaded = LiftDictionary.loadDictionaryFromFile(saved);
        FeatureSet tints = reloaded.getHeader().getFeatureSet("tints");
        for (LiftTrait trait : reloaded.getLiftDictionaryRegistry().getTraits()) {
            assertEquals(new RangeReference(tints), trait.getSpecification());
        }
    }

    @Test
    public void aRangeIsNotRenamedOverADefinitionThatWouldTakeItsTraits() {
        FeatureSet colours = header.addFeatureSet("colours");
        entryWithTrait("colours", "red");
        header.createTraitDefinition("tints");
        assertThrows(IllegalStateException.class, () -> header.renameFeatureSet(colours, "tints"));
        assertEquals("colours", colours.getId());
    }

    @Test
    public void aRangeIsNotRemovedWhileTraitsAreNamedAfterIt() {
        FeatureSet colours = header.addFeatureSet("colours");
        LiftEntry entry = dictionary.getComponentBuilder().entry().withForm("tww", "nala").build();
        // A trait with no value uses no feature, but still names the range.
        entry.addTrait(new LiftTrait(new RangeReference(colours)));
        assertThrows(IllegalStateException.class, () -> header.removeFeatureSet(colours));

        entry.deleteTrait(entry.getTraits().get(0));
        header.removeFeatureSet(colours);
        assertFalse(header.hasFeatureSet("colours"));
    }

    @Test
    public void aDeclarationWhoseDataModelDoesNotFitTheValuesChangesNothing() {
        LiftEntry entry = entryWithTrait("size", "big");
        LiftTrait trait = entry.getTraits().get(0);
        LiftFieldAndTraitDefinition integer = new LiftFieldAndTraitDefinition("size", header);
        integer.setDataModel(java.util.Optional.of("integer"));

        assertThrows(IllegalArgumentException.class, () -> header.addFieldOrTraitDefinition(integer));
        assertFalse(header.containsFieldsAndTraitsDefinitions("size"));
        assertSame(trait, entry.getTraits().get(0));
        assertEquals(new UndeclaredName("size"), trait.getSpecification());
    }

    @Test
    public void declaringTheNameOfUndeclaredFieldsRebindsThemInPlace() {
        LiftEntry entry = dictionary.getComponentBuilder().entry()
            .withForm("tww", "nala")
            .addField("summary", "en", "A summary")
            .build();
        LiftField field = entry.getFields().get("summary");

        ChangeRecorder recorder = dictionary.recordChanges();
        LiftFieldAndTraitDefinition declared = header.declareFieldDefinition("summary");
        assertSame(field, entry.getFields().get("summary"));
        assertSame(declared, field.getSpecification());

        recorder.close();
        assertEquals(new UndeclaredName("summary"), field.getSpecification());
        assertFalse(header.containsFieldsAndTraitsDefinitions("summary"));
    }

    @Test
    public void aKindThatFitsTraitsRebindsThemAndOneThatExcludesThemIsRefused() {
        FeatureSet colours = header.addFeatureSet("colours");
        LiftFieldAndTraitDefinition definition = header.createFieldDefinition("colours");
        LiftEntry entry = entryWithTrait("colours", "red");
        assertEquals(new RangeReference(colours), entry.getTraits().get(0).getSpecification());

        definition.setKind(LiftFieldAndTraitDefinitionKind.UNKNOWN);
        assertSame(definition, entry.getTraits().get(0).getSpecification());

        assertThrows(IllegalStateException.class,
            () -> definition.setKind(LiftFieldAndTraitDefinitionKind.FIELD));
        assertEquals(LiftFieldAndTraitDefinitionKind.UNKNOWN, definition.getKind());
    }

    @Test
    public void aRangeReferenceTraitHasTheFeatureDataModelAndAStrictSetter() {
        FeatureSet colours = header.addFeatureSet("colours");
        colours.addFeature("red");
        LiftEntry entry = entryWithTrait("colours", "red");
        LiftTrait trait = entry.getTraits().get(0);
        assertInstanceOf(RangeReference.class, trait.getSpecification());
        assertThrows(IllegalArgumentException.class, () -> trait.setValue("green"));
        assertFalse(colours.hasFeature("green"));
    }
}
