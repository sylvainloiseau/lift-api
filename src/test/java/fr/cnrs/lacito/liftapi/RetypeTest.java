package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.cnrs.lacito.liftapi.model.DuplicateTypeException;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftReversal;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTranslation;
import fr.cnrs.lacito.liftapi.model.LiftVariant;
import fr.cnrs.lacito.liftapi.model.MultiText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.beans.value.WritableValue;

import org.junit.jupiter.api.Test;

/**
 * Changing the type of a {@code HasType} component.
 *
 * A component's type used to be writable from anywhere - {@code setType}, or
 * {@code typeProperty().set(...)} - while two parents hold their children keyed by that
 * type: notes and translations. Retyping one left it filed under its old type, so it
 * could no longer be found, nor deleted, and a second child of the new type was then
 * accepted. Renaming a type ({@code FeatureSet.changeFeatureId}) did the same to notes,
 * whose keys were the type's id.
 *
 * The type now only changes through {@code setType}, which delegates to the parent's
 * {@code retypeX} method when there is a parent; that method refuses a duplicate and
 * re-keys the child. The property is read-only.
 */
public class RetypeTest {

    private static LiftDictionary dictionary() {
        return LiftDictionary.makeBuilder()
            .withLiftVersion(LiftVersion.V0_15)
            .withProducer("RetypeTest")
            .withObjectLanguages("qyz")
            .withMetaLanguages("en")
            .build();
    }

    private static LiftEntry entry(LiftDictionary d) {
        return d.getComponentBuilder().entry().withForm("qyz", "run").build();
    }

    private static LiftSense sense(LiftDictionary d, LiftEntry entry) {
        return d.getComponentBuilder().sense(entry).withGloss("en", "to run").build();
    }

    private static Feature noteType(LiftDictionary d, String id) {
        return d.getHeader().getNoteTypeManager().getOrCreateFeature(id);
    }

    // ------------------------------------------------------------------
    // Notes: keyed by type
    // ------------------------------------------------------------------

    @Test
    public void retypingANoteReKeysIt() {
        LiftDictionary d = dictionary();
        LiftEntry entry = entry(d);
        LiftNote note = new LiftNote(noteType(d, "source"));
        entry.addNote(note);

        note.setType(noteType(d, "general"));

        assertSame(note, entry.getNote("general"));
        assertSame(note, entry.getNote(noteType(d, "general")));
        assertEquals(List.of(noteType(d, "general")), List.copyOf(entry.getNotes().keySet()));
        assertThrows(IllegalArgumentException.class, () -> entry.getNote("source"));

        // The old type is free again, and the retyped note can still be deleted.
        entry.addNote(new LiftNote(noteType(d, "source")));
        entry.deleteNote(note);
        assertEquals(1, entry.getNotes().size());
        assertFalse(d.getLiftDictionaryRegistry().getNotes().contains(note));
    }

    @Test
    public void retypingANoteToATypeAlreadyUsedIsRefusedAndChangesNothing() {
        LiftDictionary d = dictionary();
        LiftEntry entry = entry(d);
        LiftNote source = new LiftNote(noteType(d, "source"));
        LiftNote general = new LiftNote(noteType(d, "general"));
        entry.addNote(source);
        entry.addNote(general);

        assertThrows(DuplicateTypeException.class, () -> source.setType(noteType(d, "general")));

        assertSame(noteType(d, "source"), source.getType());
        assertSame(source, entry.getNote("source"));
        assertSame(general, entry.getNote("general"));
    }

    @Test
    public void notesSurviveARenameOfTheirType() {
        LiftDictionary d = dictionary();
        LiftEntry entry = entry(d);
        Feature source = noteType(d, "source");
        LiftNote note = new LiftNote(source);
        entry.addNote(note);

        d.getHeader().getNoteTypeManager().changeFeatureId(source, "origin");

        assertSame(note, entry.getNote("origin"));
        assertThrows(
            DuplicateTypeException.class,
            () -> entry.addNote(new LiftNote(source)),
            "the renamed type is still the one in use"
        );
        entry.deleteNote(note);
        assertTrue(entry.getNotes().isEmpty());
    }

    // ------------------------------------------------------------------
    // Translations: keyed by type
    // ------------------------------------------------------------------

    @Test
    public void retypingATranslationReKeysIt() {
        LiftDictionary d = dictionary();
        FeatureSet types = d.getHeader().getTranslationTypeManager();
        Feature free = types.getOrCreateFeature("free");
        Feature literal = types.getOrCreateFeature("literal");
        Feature back = types.getOrCreateFeature("back");
        LiftExample example = d.getComponentBuilder()
            .example(sense(d, entry(d)), "qyz", "he runs");
        MultiText text = example.createTranslation(free);
        example.createTranslation(literal);
        LiftTranslation translation = example.getTranslationComponents().iterator().next();

        translation.setType(back);

        assertSame(text, example.getTranslation(back));
        assertFalse(example.getTranslations().containsKey(free));
        assertThrows(DuplicateTypeException.class, () -> translation.setType(literal));
        assertSame(back, translation.getType());
    }

    // ------------------------------------------------------------------
    // List-held components: the parent is still the one to change the type
    // ------------------------------------------------------------------

    @Test
    public void listHeldComponentsAreRetypedThroughTheirParent() {
        LiftDictionary d = dictionary();
        LiftEntry entry = entry(d);
        LiftSense sense = sense(d, entry);
        Feature a = new Feature("a", null);
        Feature b = new Feature("b", null);

        LiftEtymology etymology = LiftEtymology.create(a, "src");
        entry.addEtymology(etymology);
        LiftVariant variant = new LiftVariant();
        variant.setType(a);
        entry.addVariant(variant);
        LiftRelation onEntry = new LiftRelation(a);
        entry.addRelation(onEntry);
        LiftRelation onSense = new LiftRelation(a);
        sense.addRelation(onSense);
        LiftRelation onVariant = new LiftRelation(a);
        variant.addRelation(onVariant);
        LiftReversal reversal = new LiftReversal(a);
        sense.addReversal(reversal);
        LiftReversal nested = new LiftReversal(a);
        reversal.addReversal(nested);
        LiftReversal main = new LiftReversal(a);
        reversal.setMain(main);
        LiftAnnotation onComponent = new LiftAnnotation(a);
        entry.addAnnotation(onComponent);
        LiftAnnotation onText = new LiftAnnotation(a);
        entry.getForms().addAnnotation(onText);

        List<fr.cnrs.lacito.liftapi.model.HasType> all = List.of(
            etymology, variant, onEntry, onSense, onVariant,
            reversal, nested, main, onComponent, onText
        );
        for (var component : all) {
            component.setType(b);
        }
        for (var component : all) {
            assertSame(b, component.getType(), component.getClass().getSimpleName());
        }
        // Being list-held, several children of one parent may share a type.
        entry.addRelation(new LiftRelation(b));
    }

    @Test
    public void aParentRefusesToRetypeAComponentItDoesNotHold() {
        LiftDictionary d = dictionary();
        LiftEntry entry = entry(d);
        LiftEntry other = d.getComponentBuilder().entry().withForm("qyz", "walk").build();
        Feature a = new Feature("a", null);
        LiftVariant variant = new LiftVariant();
        variant.setType(a);
        entry.addVariant(variant);
        LiftNote note = new LiftNote(noteType(d, "source"));
        entry.addNote(note);
        LiftAnnotation annotation = new LiftAnnotation(a);
        entry.getForms().addAnnotation(annotation);

        assertThrows(IllegalArgumentException.class, () -> other.retypeVariant(variant, a));
        assertThrows(
            IllegalArgumentException.class,
            () -> other.retypeNote(note, noteType(d, "general"))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> other.getForms().retypeAnnotation(annotation, a)
        );
    }

    // ------------------------------------------------------------------
    // Detached components and the property
    // ------------------------------------------------------------------

    @Test
    public void aComponentWithoutParentTakesTheTypeDirectly() {
        LiftDictionary d = dictionary();
        LiftNote note = new LiftNote(noteType(d, "source"));

        note.setType(noteType(d, "general"));

        assertSame(noteType(d, "general"), note.getType());
    }

    @Test
    public void typePropertyIsReadOnlyAndObservable() {
        LiftDictionary d = dictionary();
        LiftEntry entry = entry(d);
        LiftNote note = new LiftNote(noteType(d, "source"));
        entry.addNote(note);
        List<Feature> seen = new ArrayList<>();
        note.typeProperty().addListener((obs, o, n) -> seen.add(n));

        note.setType(noteType(d, "general"));

        assertEquals(List.of(noteType(d, "general")), seen);
        assertFalse(
            note.typeProperty() instanceof WritableValue,
            "writing the property would bypass the parent"
        );
    }

    // ------------------------------------------------------------------
    // The annotation counter
    // ------------------------------------------------------------------

    @Test
    public void annotationCounterFollowsRetypeRenameAndDeletion() {
        LiftDictionary d = dictionary();
        FeatureSet types = d.getHeader().getAnnotationTypeManager();
        Feature checked = types.getOrCreateFeature("checked");
        Feature reviewed = types.getOrCreateFeature("reviewed");
        LiftEntry entry = entry(d);
        LiftAnnotation annotation = new LiftAnnotation(checked);
        entry.addAnnotation(annotation);
        Map<Feature, Integer> count = d.getCounter().getAnnotationTypeCount();
        assertEquals(Map.of(checked, 1), count);

        annotation.setType(reviewed);
        assertEquals(Map.of(reviewed, 1), count);

        types.changeFeatureId(reviewed, "approved");
        assertEquals(Map.of(reviewed, 1), count, "a rename keeps the count on its feature");
        assertEquals("approved", count.keySet().iterator().next().getId());

        entry.deleteAnnotation(annotation);
        assertTrue(count.isEmpty());

        // Once out of the dictionary, the annotation no longer affects the count.
        annotation.setType(checked);
        assertTrue(count.isEmpty());
    }
}
