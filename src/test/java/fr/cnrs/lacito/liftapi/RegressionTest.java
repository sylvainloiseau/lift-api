package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.cnrs.lacito.liftapi.builder.DictionaryComponentBuilderFactory;
import fr.cnrs.lacito.liftapi.model.DuplicateIdException;
import fr.cnrs.lacito.liftapi.model.DuplicateTypeException;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftReversal;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.LiftVersion;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for defects found by auditing {@code lift-api}.
 *
 * Each test here pins down one bug that the suite used to let through.
 */
public class RegressionTest {

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

    // ------------------------------------------------------------------
    // Builders
    // ------------------------------------------------------------------

    /**
     * {@code DictionaryMutator.wire} had no branch for a reversal, so building one
     * through the fluent API always threw "Unsupported element type" - after the
     * reversal had already been registered, leaving it in the indexes with no parent.
     */
    @Test
    public void aReversalCanBeBuiltThroughTheFluentApi() {
        dictionary.getHeader().getInverseTypeManager().addFeature("headword");
        LiftEntry entry = dictionary
            .getComponentBuilder()
            .entry()
            .withForm("tww", "nala")
            .build();
        LiftSense sense = dictionary
            .getComponentBuilder()
            .sense(entry)
            .withGloss("en", "sun")
            .build();

        LiftReversal reversal = dictionary
            .getComponentBuilder()
            .reversal(sense)
            .withType("headword")
            .build();

        assertSame(sense, reversal.getParent());
        assertTrue(sense.getReversals().contains(reversal));
        assertTrue(
            dictionary.getLiftDictionaryRegistry().getReversals().contains(reversal)
        );
    }

    /** {@code withId} used to call {@code withGuid}, so the id was replaced by a UUID. */
    @Test
    public void entryWithIdSetsTheLiftIdNotTheGuid() {
        LiftEntry entry = dictionary
            .getComponentBuilder()
            .entry()
            .withId("word-001")
            .withForm("tww", "esejle")
            .build();

        assertEquals("word-001", entry.getId().get());
        assertEquals(Optional.empty(), entry.getGuid());
        assertSame(
            entry,
            dictionary
                .getLiftDictionaryRegistry()
                .getEntryOrSenseByLiftId("word-001")
        );
    }

    @Test
    public void senseWithIdSetsTheLiftIdNotTheGuid() {
        LiftEntry entry = dictionary
            .getComponentBuilder()
            .entry()
            .withForm("tww", "esejle")
            .build();
        LiftSense sense = dictionary
            .getComponentBuilder()
            .sense(entry)
            .withId("sense-001")
            .withGloss("en", "a word")
            .build();

        assertEquals("sense-001", sense.getId().get());
        assertEquals(Optional.empty(), sense.getGuid());
    }

    /** A duplicate id used to leave the child attached to a parent it was never registered under. */
    @Test
    public void aRejectedRegistrationLeavesNoHalfBuiltGraph() {
        dictionary
            .getComponentBuilder()
            .entry()
            .withId("dup")
            .withForm("tww", "one")
            .build();

        assertThrows(
            DuplicateIdException.class,
            () ->
                dictionary
                    .getComponentBuilder()
                    .entry()
                    .withId("dup")
                    .withForm("tww", "two")
                    .build()
        );

        assertEquals(
            1,
            dictionary.getLiftDictionaryRegistry().getEntries().size()
        );
    }

    /** {@code AnnotationBuilder} used to require the annotation type to exist already. */
    @Test
    public void annotationCanBeAddedOnAFreshDictionary() {
        LiftEntry entry = dictionary
            .getComponentBuilder()
            .entry()
            .withForm("tww", "nala")
            .addAnnotation("checked", "yes")
            .build();

        List<LiftAnnotation> annotations = entry.getAnnotations();
        assertEquals(1, annotations.size());
        assertEquals("checked", annotations.get(0).getType().getId());
        assertEquals("yes", annotations.get(0).getValue());
        assertTrue(
            dictionary.getHeader().getAnnotationTypeManager().hasFeature("checked")
        );
    }

    /** Variants and traits used to attach their children twice. */
    @Test
    public void variantChildrenAreAttachedExactlyOnce() {
        LiftEntry entry = dictionary
            .getComponentBuilder()
            .entry()
            .withForm("tww", "esejle")
            .addVariant(v ->
                v
                    .withForm("tww", "eseile")
                    .addPronunciation(p -> p.withPronunciation("tww", "e.sei.le"))
            )
            .build();

        assertEquals(1, entry.getVariants().size());
        assertEquals(1, entry.getVariants().get(0).getPronunciations().size());
        assertEquals(
            1,
            dictionary.getLiftDictionaryRegistry().getPronunciations().size()
        );
    }

    @Test
    public void traitAnnotationsAreAttachedExactlyOnce() {
        dictionary.getHeader().getOrCreateTraitsDefinitions("morph-type");
        LiftEntry entry = dictionary
            .getComponentBuilder()
            .entry()
            .withForm("tww", "nala")
            .addTrait("morph-type", "stem", t -> t.addAnnotation("checked", "yes"))
            .build();

        assertEquals(1, entry.getTraits().size());
        assertEquals(1, entry.getTraits().get(0).getAnnotations().size());
        assertEquals(
            1,
            dictionary.getLiftDictionaryRegistry().getAnnotations().size()
        );
    }

    /** {@code RelationBuilder.withOrder} had an empty body. */
    @Test
    public void relationOrderIsKept() {
        LiftEntry target = dictionary
            .getComponentBuilder()
            .entry()
            .withForm("tww", "nala")
            .build();
        LiftEntry source = dictionary
            .getComponentBuilder()
            .entry()
            .withForm("tww", "kemia")
            .addRelation(r ->
                r.withRef(target).withType("synonym").withOrder(3)
            )
            .build();

        LiftRelation relation = source.getRelations().get(0);
        assertEquals(3, relation.getOrder().get());
    }

    /** {@code withAnnotationType} wrote into the inverse-type manager. */
    @Test
    public void dictionaryBuilderRegistersAnnotationTypesInTheRightRange() {
        LiftDictionary d = LiftDictionary.makeBuilder()
            .withAnnotationType("checked")
            .build();

        assertTrue(d.getHeader().getAnnotationTypeManager().hasFeature("checked"));
        assertFalse(d.getHeader().getInverseTypeManager().hasFeature("checked"));
    }

    /**
     * A component must be registered by its own builder, not by a later sweep over the
     * parent's subtree. GrammaticalInfo had no builder, so the one a sense created
     * through {@code withPartOfSpeech} never entered the dictionary - and neither did
     * the traits hanging off it.
     */
    @Test
    public void builderRegistersGrammaticalInfo() {
        LiftEntry entry = dictionary
            .getComponentBuilder()
            .entry()
            .withForm("tww", "nala")
            .addSense(s -> s.withGloss("en", "to run").withPartOfSpeech("verb"))
            .build();

        LiftSense sense = entry.getSenses().get(0);
        assertEquals(
            "verb",
            sense.getGrammaticalInfo().get().getGramInfoValue().getId()
        );
        assertEquals(
            1,
            dictionary.getLiftDictionaryRegistry().getGrammaticalInfos().size(),
            "the grammatical information is not in the dictionary"
        );
        assertSame(
            sense.getGrammaticalInfo().get(),
            dictionary.getLiftDictionaryRegistry().getGrammaticalInfos().get(0)
        );
    }

    @Test
    public void builderRegistersTraitsCarriedByGrammaticalInfo() {
        LiftEntry entry = dictionary
            .getComponentBuilder()
            .entry()
            .withForm("tww", "nala")
            .addSense(s ->
                s
                    .withGloss("en", "to run")
                    .withPartOfSpeech(
                        "verb",
                        g -> g.addTrait("Verb-infl-class", "fo")
                    )
            )
            .build();

        LiftSense sense = entry.getSenses().get(0);
        assertEquals(1, dictionary.getLiftDictionaryRegistry().getGrammaticalInfos().size());
        assertEquals(1, sense.getGrammaticalInfo().get().getTraits().size());
        assertEquals(
            1,
            dictionary.getLiftDictionaryRegistry().getTraits().size(),
            "the trait carried by the grammatical information is not in the dictionary"
        );
    }

    /** A sense holds one grammatical information; replacing it must not leak the old one. */
    @Test
    public void replacingGrammaticalInfoRemovesThePreviousOne() {
        LiftEntry entry = dictionary
            .getComponentBuilder()
            .entry()
            .withForm("tww", "nala")
            .addSense(s -> s.withGloss("en", "to run").withPartOfSpeech("verb"))
            .build();
        LiftSense sense = entry.getSenses().get(0);

        assertEquals(
            1,
            dictionary.getLiftDictionaryRegistry().getGrammaticalInfos().size()
        );

        dictionary.getComponentBuilder().grammaticalInfo(sense, "noun").build();

        assertEquals(
            "noun",
            sense.getGrammaticalInfo().get().getGramInfoValue().getId()
        );
        assertEquals(
            1,
            dictionary.getLiftDictionaryRegistry().getGrammaticalInfos().size(),
            "the replaced grammatical information is still registered"
        );
    }

    // ------------------------------------------------------------------
    // Model
    // ------------------------------------------------------------------

    /**
     * The FEATURE_SET / FEATURE_LIST branches of the {@link LiftTrait} constructor were
     * crossed over, and both wrapped a null collection.
     */
    @Test
    public void traitCanHoldAFeatureSetAndAFeatureList() {
        FeatureSet domains = dictionary.getHeader().addFeatureSet("semantic-domain");
        Feature sky = domains.addFeature("1.1 Sky");
        Feature sun = domains.addFeature("1.1.1 Sun");

        LiftTrait asSet = new LiftTrait(
            definition("domains-unordered", "option-collection", domains),
            new HashSet<>(List.of(sky))
        );
        assertEquals("1.1 Sky", asSet.getValue());

        LiftTrait asList = new LiftTrait(
            definition("domains-ordered", "option-sequence", domains),
            List.of(sky, sun)
        );
        assertEquals("1.1 Sky, 1.1.1 Sun", asList.getValue());
    }

    private LiftFieldAndTraitDefinition definition(
        String name,
        String type,
        FeatureSet range
    ) {
        LiftFieldAndTraitDefinition def = dictionary
            .getHeader()
            .createTraitDefinition(name);
        def.setDataModel(Optional.of(type));
        def.setResolvedRange(Optional.of(range));
        return def;
    }

    /** {@code featuresProperty()} was built on an unmodifiable empty set. */
    @Test
    public void featuresPropertyTracksTheFeatureMap() {
        FeatureSet set = dictionary.getHeader().addFeatureSet("usage");
        set.addFeature("archaic");

        Set<Feature> asSet = set.featuresProperty().get();
        assertEquals(1, asSet.size());

        Feature slang = set.addFeature("slang");
        assertTrue(set.featuresProperty().contains(slang));

        set.removeFeature("archaic");
        assertEquals(1, set.featuresProperty().size());
        assertTrue(set.featuresProperty().contains(slang));
    }

    /** Three of the built-in ranges were all constructed with the translation-type id. */
    @Test
    public void builtInFeatureSetsHaveDistinctIds() {
        assertEquals(
            "translation-type",
            dictionary.getHeader().getTranslationTypeManager().getId()
        );
        assertEquals(
            "annotation-type",
            dictionary.getHeader().getAnnotationTypeManager().getId()
        );
        assertEquals(
            "variant-type",
            dictionary.getHeader().getVariantTypeManager().getId()
        );
        assertEquals(
            dictionary.getHeader().getFeatureSets().size(),
            dictionary
                .getHeader()
                .getFeatureSets()
                .stream()
                .map(FeatureSet::getId)
                .distinct()
                .count(),
            "two built-in ranges share an id"
        );
    }

    /** A map replacement reports both wasRemoved() and wasAdded(). */
    @Test
    public void replacingAFeatureSetKeepsItInTheDerivedList() {
        int before = dictionary.getHeader().getFeatureSets().size();
        FeatureSet added = dictionary.getHeader().addFeatureSet("usage");
        assertTrue(dictionary.getHeader().getFeatureSets().contains(added));
        assertEquals(before + 1, dictionary.getHeader().getFeatureSets().size());
    }

    /** {@code <span>} without attributes used to spin {@code parseSpanContent} forever. */
    @Test
    public void formParsesSpanWithoutAttributes() {
        assertTimeoutPreemptively(
            Duration.ofSeconds(5),
            () -> {
                Form f = new Form("tww", "");
                f.changeText("before<span>inner</span>after");
                assertEquals("beforeinnerafter", f.toPlainText());

                Form unterminated = new Form("tww", "");
                unterminated.changeText("text <span lang=\"en\">oops");
                assertTrue(unterminated.toPlainText().contains("text"));
            }
        );
    }

    /** A rejected duplicate language used to leave the occurrence count inflated. */
    @Test
    public void aRejectedFormDoesNotInflateTheLanguageCount() {
        LiftEntry entry = dictionary
            .getComponentBuilder()
            .entry()
            .withForm("tww", "nala")
            .build();

        assertEquals(
            1,
            dictionary.getObjectLanguageManager().getLanguageOccurrence("tww")
        );
        assertThrows(
            Exception.class,
            () -> entry.getForms().add(new Form("tww", "duplicate"))
        );
        assertEquals(
            1,
            dictionary.getObjectLanguageManager().getLanguageOccurrence("tww")
        );
    }

    /** Releasing a subtree used to unregister each MultiText twice. */
    @Test
    public void deletingAnEntryDecrementsEachLanguageOnce() {
        dictionary.getComponentBuilder().entry().withForm("tww", "one").build();
        LiftEntry second = dictionary
            .getComponentBuilder()
            .entry()
            .withForm("tww", "two")
            .build();

        assertEquals(
            2,
            dictionary.getObjectLanguageManager().getLanguageOccurrence("tww")
        );

        dictionary.removeEntry(second);

        assertEquals(
            1,
            dictionary.getObjectLanguageManager().getLanguageOccurrence("tww"),
            "the deleted entry's form was counted out twice"
        );
        // A language still in use cannot be removed.
        assertThrows(
            IllegalArgumentException.class,
            () -> dictionary.getObjectLanguageManager().removeLanguage("tww")
        );
    }

    /** A detached MultiText must stop feeding its old dictionary's counters. */
    @Test
    public void aDetachedMultiTextStopsCountingLanguages() {
        LiftEntry entry = dictionary
            .getComponentBuilder()
            .entry()
            .withForm("tww", "nala")
            .build();

        dictionary.removeEntry(entry);
        assertEquals(
            0,
            dictionary.getObjectLanguageManager().getLanguageOccurrence("tww")
        );

        entry.getForms().add(new Form("tpi", "nalo"));
        assertEquals(
            0,
            dictionary.getObjectLanguageManager().getLanguageOccurrence("tpi"),
            "a removed entry still mutates the dictionary's counters"
        );
    }

    /** The duplicate check keyed on the definition object, which has no equals. */
    @Test
    public void aDuplicateFieldIsRejected() {
        LiftEntry entry = dictionary
            .getComponentBuilder()
            .entry()
            .withForm("tww", "nala")
            .addField("literal-meaning", "en", "first")
            .build();

        assertThrows(
            DuplicateTypeException.class,
            () ->
                dictionary
                    .getComponentBuilder()
                    .field(entry, "literal-meaning")
                    .addText("en", "second")
                    .build()
        );
        assertEquals("literal-meaning", entry.getFields().keySet().iterator().next());
        assertEquals(1, entry.getFields().size());
    }

    /** addNote used to throw ClassCastException for an example, and print to stdout. */
    @Test
    public void aDuplicateNoteTypeIsReportedWithAUsefulMessage() {
        LiftEntry entry = dictionary
            .getComponentBuilder()
            .entry()
            .withId("word-001")
            .withForm("tww", "nala")
            .addNote("source", "en", "first")
            .build();

        DuplicateTypeException thrown = assertThrows(
            DuplicateTypeException.class,
            () ->
                dictionary
                    .getComponentBuilder()
                    .note(entry, "source")
                    .addText("en", "second")
                    .build()
        );
        assertTrue(thrown.getMessage().contains("source"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("word-001"), thrown.getMessage());

        LiftNote kept = entry.getNote("source");
        assertEquals("first", kept.getText().getForm("en").get().toPlainText());
    }

    // ------------------------------------------------------------------
    // Registry
    // ------------------------------------------------------------------

    /** Senses had no duplicate-id guard, so one of two senses sharing an id was lost. */
    @Test
    public void duplicateSenseIdIsRejected() {
        DictionaryComponentBuilderFactory builder = dictionary.getComponentBuilder();
        LiftEntry entry = builder.entry().withForm("tww", "nala").build();
        builder.sense(entry).withId("s1").withGloss("en", "one").build();

        assertThrows(
            DuplicateIdException.class,
            () -> builder.sense(entry).withId("s1").withGloss("en", "two").build()
        );
    }

    /** Unknown languages used to unbox a null Integer. */
    @Test
    public void countingAnUnknownLanguageIsReportedNotNullPointer() {
        LiftDictionaryLanguagesManager manager =
            dictionary.getObjectLanguageManager();
        assertThrows(
            IllegalArgumentException.class,
            () -> manager.addLanguageOccurrence("xyz")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> manager.removeLanguageOccurrence("xyz")
        );
    }
}
