package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.GrammaticalInfo;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;

import org.junit.jupiter.api.Test;

/**
 * {@code parent.deleteX(child)} is the mirror of {@code parent.addX(child)}.
 *
 * {@code addX} closed the hole where a component could sit in the tree without the
 * dictionary knowing about it. Deletion has the same hole in reverse: unlinking a
 * component without unregistering it leaves a component in the dictionary's indexes -
 * visible through {@code getSenses()}, still counting towards the language managers -
 * that no traversal from an entry can reach. The rule is symmetric: {@code deleteX}
 * unregisters if, and only if, the receiver is attached.
 */
public class DeletionTest {

    private static LiftDictionary dictionary() {
        return LiftDictionary.makeBuilder()
            .withLiftVersion(LiftVersion.V0_15)
            .withProducer("DeletionTest")
            .withObjectLanguages("qyz")
            .withMetaLanguages("en")
            .build();
    }

    private static LiftEntry entry(LiftDictionary d, String form) {
        return d.getComponentBuilder().entry().withForm("qyz", form).build();
    }

    private static LiftSense sense(LiftDictionary d, LiftEntry parent, String gloss) {
        return d.getComponentBuilder().sense(parent).withGloss("en", gloss).build();
    }

    // ------------------------------------------------------------------
    // deleteX unwires and unregisters
    // ------------------------------------------------------------------

    @Test
    public void deletingFromAnAttachedComponentUnregistersTheSubtree() {
        LiftDictionary d = dictionary();
        LiftDictionaryRegistry registry = d.getLiftDictionaryRegistry();
        LiftEntry entry = entry(d, "run");
        LiftSense sense = sense(d, entry, "to run");

        LiftExample example = new LiftExample();
        example.getExample().add(new Form("qyz", "he runs fast"));
        LiftTrait trait = new LiftTrait(
            d.getHeader().getOrCreateTraitsDefinitions("style"),
            "formal"
        );
        example.addTrait(trait);
        sense.addExample(example);
        assertNotNull(example.getUUID());
        assertNotNull(trait.getUUID());

        sense.deleteExample(example);

        // unwired, both ways
        assertTrue(sense.getExamples().isEmpty());
        assertNull(example.getParent());
        // and unregistered, all the way down
        assertNull(example.getUUID());
        assertNull(trait.getUUID());
        assertFalse(registry.getExamples().contains(example));
        assertFalse(registry.getTraits().contains(trait));
        assertFalse(registry.getObjectText().contains(example.getExample()));
        assertEquals(
            1,
            d.getObjectLanguageManager().getLanguageOccurrence("qyz"),
            "only the entry's own form should be left counting"
        );
    }

    @Test
    public void deletingFromADetachedComponentOnlyUnwires() {
        LiftEntry entry = new LiftEntry();
        entry.addForm(new Form("qyz", "run"));
        LiftSense sense = new LiftSense();
        entry.addSense(sense);
        LiftExample example = new LiftExample();
        sense.addExample(example);

        sense.deleteExample(example);

        assertTrue(sense.getExamples().isEmpty());
        assertNull(example.getParent());
        assertNull(example.getUUID());
    }

    @Test
    public void deletingAGrammaticalInfoTakesItsTraitsWithIt() {
        LiftDictionary d = dictionary();
        LiftEntry entry = entry(d, "run");
        LiftSense sense = sense(d, entry, "to run");
        GrammaticalInfo gi = d.getComponentBuilder()
            .grammaticalInfo(sense, "Verb")
            .addTrait("style", "formal")
            .build();
        LiftTrait trait = gi.getTraits().get(0);
        assertNotNull(trait.getUUID());

        sense.deleteGrammaticalInfo();

        assertTrue(sense.getGrammaticalInfo().isEmpty());
        assertNull(gi.getUUID());
        assertNull(trait.getUUID());
        assertFalse(d.getLiftDictionaryRegistry().getGrammaticalInfos().contains(gi));
    }

    @Test
    public void aDeletedSubtreeCanBeAttachedAgain() {
        LiftDictionary d = dictionary();
        LiftEntry from = entry(d, "run");
        LiftEntry to = entry(d, "walk");
        LiftSense sense = sense(d, from, "to run");

        from.deleteSense(sense);
        assertNull(sense.getUUID());

        to.addSense(sense);

        assertNotNull(sense.getUUID());
        assertSame(to, sense.getParentEntry());
        assertEquals(1, d.getLiftDictionaryRegistry().getSenses().size());
    }

    // ------------------------------------------------------------------
    // Guards
    // ------------------------------------------------------------------

    @Test
    public void deletingAComponentHeldByAnotherParentIsRefused() {
        LiftDictionary d = dictionary();
        LiftEntry first = entry(d, "run");
        LiftEntry second = entry(d, "walk");
        LiftSense sense = sense(d, first, "to run");

        assertThrows(
            IllegalArgumentException.class,
            () -> second.deleteSense(sense),
            "a sense must only be deletable from the entry that holds it"
        );
        assertNotNull(sense.getUUID(), "the sense must be untouched");
        assertSame(first, sense.getParentEntry());
    }

    @Test
    public void aComponentStillReferredToCannotBeDeleted() {
        LiftDictionary d = dictionary();
        d.getHeader().getRelationTypeManager().addFeature("suppletion");
        LiftEntry target = entry(d, "go");
        d.getComponentBuilder()
            .entry()
            .withForm("qyz", "went")
            .addRelation(r -> r.withRef(target).withType("suppletion"))
            .build();

        assertThrows(IllegalStateException.class, () -> d.removeEntry(target));
        assertNotNull(
            target.getUUID(),
            "a refused deletion must leave the dictionary exactly as it was"
        );
        assertTrue(d.getLiftDictionaryRegistry().getEntries().contains(target));
    }

    // ------------------------------------------------------------------
    // Entries, which have no parent to delete them from
    // ------------------------------------------------------------------

    @Test
    public void entriesGoThroughTheDictionary() {
        LiftDictionary d = dictionary();
        LiftEntry first = entry(d, "one");
        LiftEntry second = entry(d, "two");
        LiftEntry third = entry(d, "three");

        d.removeEntry(second);

        assertNull(second.getUUID());
        assertEquals(2, d.getLiftDictionaryRegistry().getEntries().size());

        // Undoing that deletion must put the entry back where it was.
        d.addEntry(second, 1);

        assertNotNull(second.getUUID());
        assertEquals(
            java.util.List.of(first, second, third),
            java.util.List.copyOf(d.getLiftDictionaryRegistry().getEntries())
        );
    }

    @Test
    public void anEntryCannotBeAddedToTwoDictionaries() {
        LiftDictionary source = dictionary();
        LiftDictionary target = dictionary();
        LiftEntry entry = entry(source, "run");

        assertThrows(IllegalArgumentException.class, () -> target.addEntry(entry));

        source.removeEntry(entry);
        target.addEntry(entry);

        assertSame(target, entry.getOwningDictionary());
        assertEquals(1, target.getLiftDictionaryRegistry().getEntries().size());
    }
}
