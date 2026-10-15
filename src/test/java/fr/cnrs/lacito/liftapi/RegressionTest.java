package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import fr.cnrs.lacito.liftapi.model.LiftMedia;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftReversal;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.LiftVariant;
import fr.cnrs.lacito.liftapi.LiftVersion;

import java.io.File;
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
        // A variant's type is a trait: there is no built-in variant-type range.
        assertFalse(dictionary.getHeader().hasFeatureSet("variant-type"));
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

    // ------------------------------------------------------------------
    // Ids and positions
    // ------------------------------------------------------------------

    /**
     * {@code setId} used to set the field only: the dictionary's id index still knew the
     * entry under its old id, and a duplicate went unnoticed.
     */
    @Test
    public void renamingAnEntryUpdatesTheIdIndex() {
        LiftEntry a = dictionary.getComponentBuilder().entry().withForm("tww", "a").build();
        LiftEntry b = dictionary.getComponentBuilder().entry().withForm("tww", "b").build();
        String old = a.getId().orElseThrow();
        a.setId("renamed");
        var registry = dictionary.getLiftDictionaryRegistry();
        assertSame(a, registry.getEntryOrSenseByLiftId("renamed"));
        assertNull(registry.getEntryOrSenseByLiftId(old));
        assertThrows(DuplicateIdException.class, () -> b.setId("renamed"));
        assertEquals(b.getId().orElseThrow(), registry.getEntryOrSenseByLiftId(b.getId().orElseThrow()).getId().orElseThrow());
    }

    /**
     * Renaming an entry something refers to would leave the relation pointing at nothing,
     * as deleting it would: refused the same way.
     */
    @Test
    public void aReferencedEntryCannotBeRenamed() {
        dictionary.getHeader().getRelationTypeManager().addFeature("synonym");
        LiftEntry target = dictionary.getComponentBuilder().entry().withForm("tww", "a").build();
        dictionary.getComponentBuilder().entry().withForm("tww", "b")
            .addRelation(r -> r.withRef(target).withType("synonym")).build();
        assertThrows(IllegalStateException.class, () -> target.setId("renamed"));
    }

    /**
     * Only senses, examples and entries could be inserted at a position: undoing the
     * deletion of a trait, a relation... moved it to the end of its list.
     */
    @Test
    public void listHeldComponentsCanBeInsertedAtAPosition() {
        LiftEntry entry = dictionary.getComponentBuilder().entry().withForm("tww", "a").build();
        var def = dictionary.getHeader().getOrCreateTraitsDefinitions("status");
        LiftTrait first = new LiftTrait(def, "1");
        LiftTrait second = new LiftTrait(def, "2");
        entry.addTrait(first);
        entry.addTrait(0, second);
        assertEquals(List.of(second, first), entry.getTraits());
        assertEquals(java.util.OptionalInt.of(1), first.positionInParent());

        LiftVariant v1 = new LiftVariant();
        LiftVariant v2 = new LiftVariant();
        entry.addVariant(v1);
        entry.addVariant(0, v2);
        assertEquals(java.util.OptionalInt.of(0), v2.positionInParent());

        LiftPronunciation p = new LiftPronunciation();
        entry.addPronunciation(p);
        LiftMedia m1 = new LiftMedia("a.wav");
        LiftMedia m2 = new LiftMedia("b.wav");
        p.addMedia(m1);
        p.addMedia(0, m2);
        assertEquals(List.of(m2, m1), p.getMedias());
    }

    /**
     * FieldWorks exports may declare a field definition twice. Reading such a file used
     * to keep the last declaration; it must not stop the whole load.
     */
    @Test
    public void aDefinitionDeclaredTwiceInAFileKeepsTheLastOne(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("twice.lift");
        java.nio.file.Files.writeString(file, """
            <?xml version="1.0" encoding="UTF-8"?>
            <lift version="0.13" producer="test">
            <header>
            <fields>
            <field-definition name="note-a" class="entry" type="multistring"/>
            <field-definition name="note-a" class="sense" type="multitext"/>
            </fields>
            </header>
            <entry id="e1"><lexical-unit><form lang="tww"><text>a</text></form></lexical-unit></entry>
            </lift>
            """);
        LiftDictionary d = LiftDictionary.loadDictionaryFromFile(file.toFile());
        assertEquals(1, d.getLiftDictionaryRegistry().getEntries().size());
        assertEquals("sense", d.getHeader().getFieldsAndTraitsDefinitions("note-a").getTargetAsString());
        assertEquals(1, d.getHeader().getFieldsAndTraitsDefinitions().stream()
            .filter(x -> x.getName().equals("note-a")).count());
    }

    // ------------------------------------------------------------------
    // Default LIFT ids
    // ------------------------------------------------------------------

    /**
     * The default LIFT id of an entry or a sense used to be its internal UUID. Internal
     * UUIDs are now a per-dictionary counter, unique only within the dictionary, while
     * the LIFT id is written to the file: it must stay a random UUID of its own.
     */
    @Test
    public void aDefaultLiftIdIsARandomUuidDistinctFromTheInternalOne() {
        LiftEntry entry = new LiftEntry();
        entry.getForms().add(new Form("tww", "nala"));
        LiftSense sense = new LiftSense();
        sense.getGlosses().add(new Form("en", "house"));
        entry.addSense(sense);
        dictionary.addEntry(entry);

        for (var node : List.of(entry, sense)) {
            java.util.UUID liftId = java.util.UUID.fromString(node.getId().orElseThrow());
            assertEquals(4, liftId.version(), "the LIFT id must be a random (version 4) UUID");
            assertFalse(liftId.equals(node.getUUID()), "the LIFT id must not be the internal UUID");
            assertEquals(8, node.getUUID().version(), "the internal UUID is a custom (version 8) one");
        }
    }

    /**
     * Ids generated after a reload must not collide with the ids generated, and saved,
     * in an earlier session - which ids derived from a per-dictionary counter would.
     */
    @Test
    public void idsGeneratedAfterAReloadDoNotCollideWithSavedOnes(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        for (int i = 0; i < 20; i++) {
            LiftEntry e = new LiftEntry();
            e.getForms().add(new Form("tww", "w" + i));
            dictionary.addEntry(e);
        }
        File file = dir.resolve("ids.lift").toFile();
        dictionary.save(file);
        Set<String> saved = new HashSet<>();
        for (LiftEntry e : dictionary.getLiftDictionaryRegistry().getEntries()) {
            saved.add(e.getId().orElseThrow());
        }

        LiftDictionary reloaded = LiftDictionary.loadDictionaryFromFile(file);
        for (int i = 0; i < 20; i++) {
            LiftEntry e = new LiftEntry();
            e.getForms().add(new Form("tww", "v" + i));
            reloaded.addEntry(e);
            assertFalse(saved.contains(e.getId().orElseThrow()), "a new id collides with a saved one");
        }
        assertEquals(40, reloaded.getLiftDictionaryRegistry().getEntries().size());
    }

    // ------------------------------------------------------------------
    // Builder-only construction (plan builder-only-construction-2026-10)
    // ------------------------------------------------------------------

    private static LiftDictionary load(java.nio.file.Path dir, String xml) throws Exception {
        File file = dir.resolve("test.lift").toFile();
        java.nio.file.Files.writeString(file.toPath(), xml);
        return LiftDictionary.loadDictionaryFromFile(file);
    }

    private static final String MULTI_VALUED_HEADER = """
        <?xml version="1.0" encoding="UTF-8"?>
        <lift version="0.15" producer="test">
        <header>
        <ranges>
        <range id="colours"><range-element id="red"/><range-element id="light blue"/></range>
        </ranges>
        <fields>
        <field-definition name="colour" kind="trait" type="option-collection" option-range="colours"/>
        </fields>
        </header>
        """;

    /**
     * {@code LiftTrait.getValue()} joined the features of a multi-valued trait with
     * {@code ", "} while the reader split on whitespace: reading back gave the ids
     * {@code "red,"} and {@code "blue"}. Both now use commas, since feature ids may
     * contain spaces.
     */
    @Test
    public void aMultiValuedTraitSurvivesASaveAndAReload(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        LiftDictionary d = load(dir, MULTI_VALUED_HEADER + """
            <entry id="e1"><trait name="colour" value="red, light blue"/></entry>
            </lift>
            """);
        File saved = dir.resolve("saved.lift").toFile();
        d.save(saved);
        LiftDictionary reloaded = LiftDictionary.loadDictionaryFromFile(saved);

        LiftTrait trait = reloaded.getLiftDictionaryRegistry().getTraits().iterator().next();
        Set<String> ids = new HashSet<>();
        trait.featureSetValueProperty().forEach(f -> ids.add(f.getId()));
        assertEquals(Set.of("red", "light blue"), ids);
        assertEquals(
            Set.of("red", "light blue"),
            reloaded.getHeader().getFeatureSet("colours").getFeatures().keySet(),
            "no feature should have been created from a badly split value"
        );
    }

    /** The spaces around the ids of a multi-valued trait are not part of them. */
    @Test
    public void theSpacesAroundTheIdsOfAMultiValuedTraitAreTrimmed(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        LiftDictionary d = load(dir, MULTI_VALUED_HEADER + """
            <entry id="e1"><trait name="colour" value=" red ,light blue,, "/></entry>
            </lift>
            """);
        LiftTrait trait = d.getLiftDictionaryRegistry().getTraits().iterator().next();
        assertEquals(2, trait.featureSetValueProperty().size());
        assertEquals(2, d.getHeader().getFeatureSet("colours").getFeatures().size());
    }

    /**
     * {@code VariantBuilder.withRefId} looked the target up at once and, when it did not
     * exist yet, set a null target and forgot the id: a variant referring to an entry
     * further down a file, or created later in a batch, lost its reference.
     */
    @Test
    public void aReferenceToATargetBuiltLaterIsKept() {
        DictionaryComponentBuilderFactory b = dictionary.getComponentBuilder();
        LiftEntry source = b.entry().withId("source").withForm("tww", "nala")
            .addVariant(v -> v.withRefId("target"))
            .addRelation("synonym", "target")
            .build();
        LiftVariant variant = source.getVariants().get(0);
        LiftRelation relation = source.getRelations().get(0);
        assertEquals(Optional.of("target"), variant.getRefId());
        assertEquals(Optional.of("target"), relation.getRefId());
        assertNull(variant.getRefObject(), "the target does not exist yet");
        assertTrue(
            dictionary.getLiftDictionaryRegistry().isReferenced("target"),
            "the reference is counted before its target exists"
        );

        LiftEntry target = b.entry().withId("target").withForm("tww", "pala").build();
        dictionary.resolveReferences();
        assertSame(target, variant.getRefObject());
        assertSame(target, relation.getRefObject());
        assertThrows(IllegalStateException.class, () -> dictionary.removeEntry(target));
    }

    /** A reference to an existing target points to it at once. */
    @Test
    public void aReferenceToAnExistingTargetIsResolvedAtOnce() {
        DictionaryComponentBuilderFactory b = dictionary.getComponentBuilder();
        LiftEntry target = b.entry().withId("target").withForm("tww", "pala").build();
        LiftEntry source = b.entry().withForm("tww", "nala")
            .addVariant(v -> v.withRefId("target"))
            .build();
        assertSame(target, source.getVariants().get(0).getRefObject());
    }

    /**
     * An error raised while building a component during a load says where in the file
     * the element is: the builders know nothing of the file.
     */
    @Test
    public void anErrorWhileLoadingGivesThePositionInTheFile(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        LiftDocumentLoadingException e = assertThrows(LiftDocumentLoadingException.class, () -> load(dir, """
            <?xml version="1.0" encoding="UTF-8"?>
            <lift version="0.15" producer="test">
            <entry id="e1">
            <relation ref="e1"/>
            </entry>
            </lift>
            """));
        String message = String.valueOf(e.getCause().getMessage());
        assertTrue(message.contains("line 4"), message);
        assertTrue(message.contains("type"), message);
    }

    /** A duplicate id found while loading keeps its type, and gains the position. */
    @Test
    public void aDuplicateIdWhileLoadingKeepsItsTypeAndGivesThePosition(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        DuplicateIdException e = assertThrows(DuplicateIdException.class, () -> load(dir, """
            <?xml version="1.0" encoding="UTF-8"?>
            <lift version="0.15" producer="test">
            <entry id="e1"/>
            <entry id="e1"/>
            </lift>
            """));
        assertTrue(e.getMessage().contains("line 4"), e.getMessage());
    }
}
