package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;

import org.junit.jupiter.api.Test;

/**
 * The attached / detached contract of the model package.
 *
 * A component used to be able to enter the tree without the dictionary ever hearing
 * about it: {@code LiftSense.addExample(...)} and its siblings are public, and they only
 * wired parent and child together. The component was then written out on save but did
 * not exist as far as every query API was concerned - no UUID, absent from the
 * registries and from the observable lists the UI binds to, its text not counted towards
 * the dictionary's languages.
 *
 * The rule that closed that hole is checked here: an {@code addX()} registers its
 * argument if, and only if, the receiver is attached to a dictionary.
 */
public class AttachmentTest {

    private static LiftDictionary dictionary() {
        return LiftDictionary.makeBuilder()
            .withLiftVersion(LiftVersion.V0_15)
            .withProducer("AttachmentTest")
            .withObjectLanguages("qyz")
            .withMetaLanguages("en")
            .build();
    }

    private static LiftEntry entry(LiftDictionary d, String form) {
        return d.getComponentBuilder().entry().withForm("qyz", form).build();
    }

    /** A sense needs a gloss or a definition to be built; the gloss is meta language. */
    private static LiftSense sense(LiftDictionary d, LiftEntry parent, String gloss) {
        return d.getComponentBuilder().sense(parent).withGloss("en", gloss).build();
    }

    // ------------------------------------------------------------------
    // The loophole itself
    // ------------------------------------------------------------------

    @Test
    public void addingToAnAttachedComponentRegistersIt() {
        LiftDictionary d = dictionary();
        LiftDictionaryRegistry registry = d.getLiftDictionaryRegistry();
        LiftEntry entry = entry(d, "run");
        LiftSense sense = sense(d, entry, "to run");

        // The model's own method, bypassing the builders entirely.
        LiftExample example = new LiftExample();
        example.getExample().add(new Form("qyz", "he runs fast"));
        sense.addExample(example);

        assertNotNull(example.getUUID(), "the example should have been given a UUID");
        assertSame(d, example.getOwningDictionary());
        assertTrue(
            registry.getExamples().contains(example),
            "the example should appear in the registry's observable list"
        );
        assertTrue(
            registry.getObjectText().contains(example.getExample()),
            "the example's MultiText should have been registered too"
        );
        assertEquals(
            2,
            d.getObjectLanguageManager().getLanguageOccurrence("qyz"),
            "the entry form and the example form should both be counted"
        );
    }

    @Test
    public void adoptingASubtreeLearnsItsLanguages() {
        LiftDictionary d = dictionary();
        LiftEntry entry = entry(d, "run");
        LiftSense sense = sense(d, entry, "to run");

        LiftExample example = new LiftExample();
        example.getExample().add(new Form("abc", "il court"));
        sense.addExample(example);

        assertTrue(
            d.getObjectLanguageManager().hasLanguage("abc"),
            "a language arriving with an adopted subtree should be declared"
        );
        assertEquals(1, d.getObjectLanguageManager().getLanguageOccurrence("abc"));
    }

    @Test
    public void registrationReachesTheWholeSubtree() {
        LiftDictionary d = dictionary();
        LiftDictionaryRegistry registry = d.getLiftDictionaryRegistry();
        LiftEntry entry = entry(d, "run");
        LiftSense sense = sense(d, entry, "to run");

        LiftExample example = new LiftExample();
        LiftTrait trait = new LiftTrait(
            d.getHeader().getOrCreateTraitsDefinitions("style"),
            "formal"
        );
        example.addTrait(trait);

        sense.addExample(example);

        assertNotNull(trait.getUUID(), "a grandchild should be registered as well");
        assertTrue(registry.getTraits().contains(trait));
    }

    // ------------------------------------------------------------------
    // Detached stays detached
    // ------------------------------------------------------------------

    @Test
    public void buildingOutsideADictionaryRegistersNothing() {
        LiftEntry entry = new LiftEntry();
        entry.addForm(new Form("qyz", "run"));
        LiftSense sense = new LiftSense();
        entry.addSense(sense);
        LiftExample example = new LiftExample();
        example.getExample().add(new Form("qyz", "he runs fast"));
        sense.addExample(example);

        assertNull(entry.getOwningDictionary());
        assertNull(sense.getOwningDictionary());
        assertNull(entry.getUUID());
        assertNull(sense.getUUID());
        assertNull(example.getUUID());
        assertSame(entry, sense.getParentEntry());
    }

    @Test
    public void attachingADetachedSubtreeRegistersItInOneGo() {
        LiftDictionary d = dictionary();
        LiftDictionaryRegistry registry = d.getLiftDictionaryRegistry();

        LiftEntry entry = new LiftEntry();
        entry.addForm(new Form("qyz", "run"));
        LiftSense sense = new LiftSense();
        entry.addSense(sense);
        LiftExample example = new LiftExample();
        example.getExample().add(new Form("qyz", "he runs fast"));
        sense.addExample(example);

        d.addEntry(entry);

        assertNotNull(entry.getUUID());
        assertNotNull(sense.getUUID());
        assertNotNull(example.getUUID());
        assertSame(d, example.getOwningDictionary());
        assertEquals(1, registry.getEntries().size());
        assertEquals(1, registry.getSenses().size());
        assertEquals(1, registry.getExamples().size());
    }

    @Test
    public void anOrphanCannotBeAdopted() {
        LiftDictionary d = dictionary();
        LiftSense orphan = new LiftSense();

        // A component other than an entry can only reach a dictionary through its
        // parent, so there is no public way to register an orphan. Checked here at the
        // level where the invariant is stated.
        assertThrows(
            IllegalArgumentException.class,
            () -> d.getMutator().adoptSubtree(orphan)
        );
    }

    // ------------------------------------------------------------------
    // Move semantics
    // ------------------------------------------------------------------

    @Test
    public void movingASenseBetweenEntriesOfTheSameDictionary() {
        LiftDictionary d = dictionary();
        LiftDictionaryRegistry registry = d.getLiftDictionaryRegistry();
        LiftEntry from = entry(d, "run");
        LiftEntry to = entry(d, "walk");
        LiftSense sense = sense(d, from, "to run");
        java.util.UUID uuid = sense.getUUID();

        sense.detach();

        assertEquals(
            uuid,
            sense.getUUID(),
            "detach() unlinks but must not unregister"
        );
        assertTrue(registry.getSenses().contains(sense));
        assertNull(sense.getOwningDictionary(), "unlinked, so no longer attached");
        assertTrue(from.getSenses().isEmpty());

        // Re-attaching must not trip the duplicate-registration guard.
        to.addSense(sense);

        assertEquals(uuid, sense.getUUID());
        assertSame(to, sense.getParentEntry());
        assertSame(d, sense.getOwningDictionary());
        assertEquals(1, registry.getSenses().size());
    }

    @Test
    public void movingASenseBetweenDictionaries() {
        LiftDictionary source = dictionary();
        LiftDictionary target = dictionary();
        LiftEntry from = entry(source, "run");
        LiftEntry to = entry(target, "walk");
        LiftSense sense = sense(source, from, "to run");

        from.deleteSense(sense);

        assertNull(sense.getUUID(), "deleteSense() also unregisters");
        assertFalse(source.getLiftDictionaryRegistry().getSenses().contains(sense));

        to.addSense(sense);

        assertNotNull(sense.getUUID());
        assertSame(target, sense.getOwningDictionary());
        assertEquals(1, target.getLiftDictionaryRegistry().getSenses().size());
        assertEquals(
            1,
            target.getMetaLanguageManager().getLanguageOccurrence("en"),
            "the gloss should now count towards the target dictionary"
        );
    }

    @Test
    public void aSenseStillOwnedByAnotherDictionaryIsRefused() {
        LiftDictionary source = dictionary();
        LiftDictionary target = dictionary();
        LiftEntry from = entry(source, "run");
        LiftEntry to = entry(target, "walk");
        LiftSense sense = sense(source, from, "to run");

        // Detached from its entry, but never released by its dictionary.
        sense.detach();

        assertThrows(
            IllegalArgumentException.class,
            () -> to.addSense(sense),
            "a component registered elsewhere must not be silently adopted"
        );
    }
}
