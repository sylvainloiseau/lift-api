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
        dictionary.getHeader().declareTraitDefinition("morph-type");
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

    // ------------------------------------------------------------------
    // What uses a feature
    // ------------------------------------------------------------------
    //
    // FeatureSet.removeFeature removed a feature that components still held, which were
    // then left with a type, a category or a trait value that no range declares; and
    // nothing in the API said whether a feature was used.

    private DictionaryComponentBuilderFactory builder() {
        return dictionary.getComponentBuilder();
    }

    private LiftEntry anEntry() {
        return builder().entry().withForm("tww", "nala").build();
    }

    /** {@code id} is used {@code uses} times, and cannot be removed from {@code set}. */
    private void assertInUse(FeatureSet set, String id, long uses) {
        Feature feature = set.getFeature(id);
        assertTrue(dictionary.getHeader().isUsed(feature), id);
        assertEquals(uses, dictionary.getHeader().countUses(feature), id);
        assertThrows(IllegalStateException.class, () -> set.removeFeature(id));
        assertSame(feature, set.getFeature(id), "a refused removal leaves the feature in place");
    }

    @Test
    public void theTypeOfATypedComponentIsAUseOfItsFeature() {
        LiftEntry entry = anEntry();
        LiftSense sense = builder().sense(entry).withGloss("en", "sun").build();
        var example = builder().example(sense).withExample("tww", "nala ka").build();
        builder().note(entry, "general").addText("en", "a note").build();
        builder().relation("synonym", entry).withRef(entry).build();
        builder().annotation(entry, "review").withValue("ok").build();
        builder().etymology(entry, "borrowing", "Tok Pisin").addForm("tpi", "nalaa").build();
        builder().reversal(sense).withType("en-rev").withForm("en", "sun").build();
        builder().translation(example, "free").addText("en", "the sun").build();

        var header = dictionary.getHeader();
        assertInUse(header.getNoteTypeManager(), "general", 1);
        assertInUse(header.getRelationTypeManager(), "synonym", 1);
        assertInUse(header.getAnnotationTypeManager(), "review", 1);
        assertInUse(header.getEtymologyTypeManager(), "borrowing", 1);
        assertInUse(header.getInverseTypeManager(), "en-rev", 1);
        assertInUse(header.getTranslationTypeManager(), "free", 1);
    }

    @Test
    public void anAnnotationOnAFormIsAUseOfItsType() {
        LiftEntry entry = anEntry();
        Form form = entry.getForms().getForm("tww").orElseThrow();
        builder().annotation(form, "spelling").withValue("checked").build();
        assertInUse(dictionary.getHeader().getAnnotationTypeManager(), "spelling", 1);
    }

    @Test
    public void aCategoryIsAUseOfItsPartOfSpeech() {
        LiftSense sense = builder().sense(anEntry()).withGloss("en", "sun").build();
        builder().grammaticalInfo(sense, "Noun").build();
        assertInUse(dictionary.getHeader().getGrammaticalInfoManager(), "Noun", 1);
    }

    @Test
    public void theValueOfAnOptionTraitIsAUseOfItsFeatures() {
        FeatureSet domains = dictionary.getHeader().addFeatureSet("semantic-domain");
        domains.addFeature("1.1 Sky");
        domains.addFeature("1.5 Plant");
        domains.addFeature("2 Person");
        definition("domain", "option", domains);
        definition("domains", "option-collection", domains);
        definition("ordered-domains", "option-sequence", domains);
        LiftEntry entry = anEntry();
        builder().trait(entry, "domain", "1.1 Sky").build();
        builder().trait(entry, "domains", "1.1 Sky, 1.5 Plant").build();
        builder().trait(entry, "ordered-domains", "1.5 Plant").build();

        // Ids with a space are compared as features, never split as words.
        assertInUse(domains, "1.1 Sky", 2);
        assertInUse(domains, "1.5 Plant", 2);
        assertFalse(dictionary.getHeader().isUsed(domains.getFeature("2 Person")));
        domains.removeFeature("2 Person");
        assertFalse(domains.hasFeature("2 Person"));
    }

    @Test
    public void beingTheSuperordinateOfAFeatureIsAUse() {
        FeatureSet pos = dictionary.getHeader().getGrammaticalInfoManager();
        Feature verb = pos.addFeature("Verb");
        pos.addFeature("Transitive verb").setSuperordinateFeature(verb);
        assertInUse(pos, "Verb", 1);
    }

    @Test
    public void aTraitOnARangeElementUsesItsFeatureAndItsDefinition() {
        var header = dictionary.getHeader();
        FeatureSet usage = header.addFeatureSet("usage");
        usage.addFeature("archaic");
        LiftFieldAndTraitDefinition register = definition("register", "option", usage);
        Feature noun = header.getGrammaticalInfoManager().addFeature("Noun");
        noun.addTrait(new LiftTrait(register, usage.getFeature("archaic")));

        // The header's components are not in the registry: they are looked at too.
        assertInUse(usage, "archaic", 1);
        assertTrue(header.isUsed(register));
        assertEquals(1, header.countUses(register));
        assertThrows(IllegalStateException.class,
            () -> header.removeFieldOrTraitDefinition(register));
    }

    @Test
    public void aFeatureSetWithAFeatureInUseCannotBeRemoved() {
        var header = dictionary.getHeader();
        FeatureSet status = header.addFeatureSet("status");
        status.addFeature("draft");
        header.getGrammaticalInfoManager().addFeature("Noun")
            .setSuperordinateFeature(status.getFeature("draft"));

        assertThrows(IllegalStateException.class, () -> header.removeFeatureSet(status));
        assertSame(status, header.getFeatureSet("status"));
    }

    // ------------------------------------------------------------------
    // Renaming a feature set
    // ------------------------------------------------------------------
    //
    // A feature could be renamed (FeatureSet.changeFeatureId) and a feature set could
    // not: its id was final.

    @Test
    public void aFeatureSetIsRenamedInPlace() {
        var header = dictionary.getHeader();
        FeatureSet usage = header.addFeatureSet("usage");
        usage.addFeature("archaic");
        LiftFieldAndTraitDefinition register = definition("register", "option", usage);
        int position = header.getFeatureSets().indexOf(usage);

        header.renameFeatureSet(usage, "register-values");

        assertEquals("register-values", usage.getId());
        assertSame(usage, header.getFeatureSet("register-values"));
        assertFalse(header.hasFeatureSet("usage"));
        assertEquals(position, header.getFeatureSets().indexOf(usage));
        assertSame(usage, register.getResolvedFeatureSet().orElseThrow());
        // The set is still the header's: it can be removed, its features found.
        assertSame(usage.getFeature("archaic"), header.getFeatureSet("register-values").getFeature("archaic"));
    }

    @Test
    public void aRefusedRenameLeavesTheHeaderAsItWas() {
        var header = dictionary.getHeader();
        FeatureSet usage = header.addFeatureSet("usage");
        header.addFeatureSet("status");
        FeatureSet pos = header.getGrammaticalInfoManager();
        FeatureSet external = header.addFeatureSet("dialects");
        external.setHref("dialects.lift-ranges");
        FeatureSet elsewhere = new FeatureSet("elsewhere", header);

        assertThrows(IllegalArgumentException.class, () -> header.renameFeatureSet(pos, "pos"));
        assertThrows(IllegalArgumentException.class, () -> header.renameFeatureSet(usage, "status"));
        assertThrows(IllegalArgumentException.class, () -> header.renameFeatureSet(usage, " "));
        assertThrows(IllegalArgumentException.class, () -> header.renameFeatureSet(elsewhere, "x"));
        assertThrows(IllegalStateException.class, () -> header.renameFeatureSet(external, "regions"));

        assertSame(usage, header.getFeatureSet("usage"));
        assertSame(pos, header.getFeatureSet("grammatical-info"));
        assertSame(external, header.getFeatureSet("dialects"));
    }

    @Test
    public void aRenamedRangeIsWrittenUnderItsNewIdAndItsValuesStillResolve(
        @org.junit.jupiter.api.io.TempDir java.nio.file.Path dir
    ) throws Exception {
        LiftDictionary d = load(dir, MULTI_VALUED_HEADER + """
            <entry id="e1"><trait name="colour" value="red, light blue"/></entry>
            </lift>
            """);
        d.getHeader().renameFeatureSet(d.getHeader().getFeatureSet("colours"), "colors");
        File saved = dir.resolve("saved.lift").toFile();
        d.save(saved);
        LiftDictionary reloaded = LiftDictionary.loadDictionaryFromFile(saved);

        FeatureSet colors = reloaded.getHeader().getFeatureSet("colors");
        assertFalse(reloaded.getHeader().hasFeatureSet("colours"));
        assertSame(colors,
            reloaded.getHeader().getFieldsAndTraitsDefinitions("colour").getResolvedFeatureSet().orElseThrow());
        LiftTrait trait = reloaded.getLiftDictionaryRegistry().getTraits().iterator().next();
        assertEquals(Set.of(colors.getFeature("red"), colors.getFeature("light blue")),
            trait.getTypedValue());
    }

    @Test
    public void aLoadedExternalRangesFileIsRewrittenWithTheNewId(
        @org.junit.jupiter.api.io.TempDir java.nio.file.Path dir
    ) throws Exception {
        FeatureSet dialects = dictionary.getHeader().addFeatureSet("dialects");
        dialects.addFeature("north");
        dialects.setHref("test.lift-ranges");
        dialects.setExternalContentLoaded(true);

        dictionary.getHeader().renameFeatureSet(dialects, "regions");
        dictionary.save(dir.resolve("test.lift").toFile());

        String ranges = java.nio.file.Files.readString(dir.resolve("test.lift-ranges"));
        assertTrue(ranges.contains("\"regions\""), ranges);
        assertFalse(ranges.contains("\"dialects\""), ranges);
    }

    @Test
    public void theHierarchyInsideAFeatureSetGoesWithIt() {
        var header = dictionary.getHeader();
        FeatureSet usage = header.addFeatureSet("usage");
        Feature old = usage.addFeature("old");
        usage.addFeature("archaic").setSuperordinateFeature(old);

        header.removeFeatureSet(usage);
        assertFalse(header.hasFeatureSet("usage"));
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
        var def = dictionary.getHeader().declareTraitDefinition("status");
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

    /**
     * The traits of a range element, read before the {@code <fields>} of the header,
     * used to create their definition on the fly; its declaration, read afterwards,
     * replaced it and left those traits holding a definition the header no longer had.
     * A file saved from such a dictionary (FieldWorks' {@code absten-feature-value})
     * could no longer be read back. Those traits are now undeclared names while the
     * header is read, and are bound to the declaration at its end.
     */
    @Test
    public void aDefinitionDeclaredAfterTheTraitsOfARangeElementIsAppliedToThem(
        @org.junit.jupiter.api.io.TempDir java.nio.file.Path dir
    ) throws Exception {
        LiftDictionary d = load(dir, """
            <?xml version="1.0" encoding="UTF-8"?>
            <lift version="0.15" producer="test">
            <header>
            <ranges>
            <range id="tense"><range-element id="pst"><trait name="abbrev-kind" value="short"/></range-element></range>
            </ranges>
            <fields>
            <field-definition name="abbrev-kind" class="sense" type="string">
            <label><form lang="en"><text>Kind of abbreviation</text></form></label>
            </field-definition>
            </fields>
            </header>
            </lift>
            """);
        var header = d.getHeader();
        LiftFieldAndTraitDefinition declared = header.getFieldsAndTraitsDefinitions("abbrev-kind");
        LiftTrait trait = header.getFeatureSet("tense").getFeature("pst").getTraits().get(0);
        assertSame(declared, trait.getSpecification());
        assertEquals("sense", declared.getTargetAsString());
        assertEquals("Kind of abbreviation",
            declared.getLabel().getForm("en").orElseThrow().toPlainText());

        File saved = dir.resolve("saved.lift").toFile();
        d.save(saved);
        LiftDictionary reloaded = LiftDictionary.loadDictionaryFromFile(saved);
        assertTrue(reloaded.getHeader().containsFieldsAndTraitsDefinitions("abbrev-kind"));
    }

    /**
     * A trait named after a range, with no definition of that name (the corpus'
     * {@code morph-type}), got a STRING definition created on the fly: its values were
     * strings, so the feature they name could be removed while in use, was not renamed
     * with them, and the invented definition was written in the header on save. The
     * trait now refers to the range itself, and its value is the feature.
     */
    @Test
    public void aTraitNamedAfterARangeHoldsAFeatureThatCountsAsUsed(
        @org.junit.jupiter.api.io.TempDir java.nio.file.Path dir
    ) throws Exception {
        FeatureSet morphTypes = dictionary.getHeader().addFeatureSet("morph-type");
        LiftEntry entry = dictionary.getComponentBuilder().entry()
            .withForm("tww", "nala")
            .addTrait("morph-type", "stem")
            .build();
        Feature stem = morphTypes.getFeature("stem");
        assertSame(stem, entry.getTraits().get(0).getTypedValue());
        assertEquals(1, dictionary.getHeader().countUses(stem));
        assertThrows(IllegalStateException.class, () -> morphTypes.removeFeature("stem"));

        morphTypes.changeFeatureId(stem, "root");
        assertEquals("root", entry.getTraits().get(0).getValue());

        File saved = dir.resolve("saved.lift").toFile();
        dictionary.save(saved);
        assertFalse(java.nio.file.Files.readString(saved.toPath()).contains("<field tag=\"morph-type\""));
    }

    /**
     * The range of an option definition could change while its traits held features of
     * the old range: the file then named the new range with the old features' ids, and
     * was read back into the new range - not the dictionary that was saved. The change is
     * refused while traits use the definition, as a change of its data model is.
     */
    @Test
    public void theRangeOfAnOptionDefinitionDoesNotChangeWhileTraitsUseIt() {
        FeatureSet registers = dictionary.getHeader().addFeatureSet("registers");
        FeatureSet styles = dictionary.getHeader().addFeatureSet("styles");
        LiftFieldAndTraitDefinition register = definition("register", "option", registers);
        LiftEntry entry = dictionary.getComponentBuilder().entry()
            .withForm("tww", "nala")
            .addTrait("register", "formal")
            .build();

        assertThrows(IllegalStateException.class, () -> register.setResolvedRange(Optional.of(styles)));
        assertSame(registers, register.getResolvedFeatureSet().orElseThrow());

        // Setting the same range, or a range once no trait uses the definition, is fine.
        register.setResolvedRange(Optional.of(registers));
        entry.deleteTrait(entry.getTraits().get(0));
        register.setResolvedRange(Optional.of(styles));
        assertSame(styles, register.getResolvedFeatureSet().orElseThrow());
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
        <field-definition name="colour" type="option-collection" option-range="colours"/>
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

    /**
     * The reader did not map {@code <entry order>}: it was stored as an unknown
     * attribute, which the writer does not write, so a save dropped it.
     */
    @Test
    public void theOrderOfAnEntrySurvivesASaveAndAReload(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        LiftDictionary d = load(dir, """
            <?xml version="1.0" encoding="UTF-8"?>
            <lift version="0.15" producer="test">
            <entry id="e1" order="2"/>
            </lift>
            """);
        LiftEntry entry = d.getLiftDictionaryRegistry().getEntries().getFirst();
        assertEquals("2", entry.getOrder().orElse(null));
        assertTrue(entry.getOtherXmlAttributes().isEmpty());

        File saved = dir.resolve("saved.lift").toFile();
        d.save(saved);
        LiftDictionary reloaded = LiftDictionary.loadDictionaryFromFile(saved);
        assertEquals("2", reloaded.getLiftDictionaryRegistry().getEntries().getFirst().getOrder().orElse(null));
    }

    /**
     * The elements that read their attributes by name (trait, range element...) ignored
     * the others silently; they are now kept with the unknown attributes, as on the
     * elements read through {@code populate}, and logged.
     */
    @Test
    public void anUnknownAttributeOnAnElementReadByNameIsKept(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        LiftDictionary d = load(dir, """
            <?xml version="1.0" encoding="UTF-8"?>
            <lift version="0.15" producer="test">
            <header><ranges>
            <range id="colours"><range-element id="red" shade="dark"/></range>
            </ranges></header>
            <entry id="e1"><trait name="colour" value="red" certainty="low"/></entry>
            </lift>
            """);
        LiftTrait trait = d.getLiftDictionaryRegistry().getTraits().getFirst();
        assertEquals(java.util.Map.of("certainty", "low"), trait.getOtherXmlAttributes());
        assertEquals(
            java.util.Map.of("shade", "dark"),
            d.getHeader().getFeatureSet("colours").getFeature("red").getOtherXmlAttributes()
        );
    }

    /**
     * The writer writes the dates of fields, ranges and range elements, but the reader
     * did not read them: a save dropped them.
     */
    @Test
    public void theDatesOfAFieldAndARangeElementSurviveASaveAndAReload(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        LiftDictionary d = load(dir, """
            <?xml version="1.0" encoding="UTF-8"?>
            <lift version="0.15" producer="test">
            <header><ranges>
            <range id="colours" dateCreated="2020-01-01"><range-element id="red" dateModified="2021-02-02"/></range>
            </ranges></header>
            <entry id="e1"><field name="comment" dateCreated="2022-03-03"><form lang="en"><text>x</text></form></field></entry>
            </lift>
            """);
        File saved = dir.resolve("saved.lift").toFile();
        d.save(saved);
        LiftDictionary reloaded = LiftDictionary.loadDictionaryFromFile(saved);

        FeatureSet colours = reloaded.getHeader().getFeatureSet("colours");
        assertEquals("2020-01-01", colours.getDateCreated().orElse(null));
        assertEquals("2021-02-02", colours.getFeature("red").getDateModified().orElse(null));
        assertEquals("2022-03-03",
            reloaded.getLiftDictionaryRegistry().getFields().getFirst().getDateCreated().orElse(null));
        assertTrue(colours.getFeature("red").getOtherXmlAttributes().isEmpty());
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
