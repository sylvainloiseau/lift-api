package fr.cnrs.lacito.liftapi.change;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftVersion;
import fr.cnrs.lacito.liftapi.internal.ChangeSupport;
import fr.cnrs.lacito.liftapi.model.DuplicateTypeException;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import javafx.util.Subscription;

/**
 * The rules of the change events, one by one (see the package documentation).
 */
class ChangeEventsTest {

    private LiftDictionary d;
    private LiftEntry entry;
    private final List<ModelChange> events = new ArrayList<>();
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        d = LiftDictionary.makeBuilder()
            .withLiftVersion(LiftVersion.V0_15)
            .withObjectLanguages("qyz")
            .withMetaLanguages("en", "fr")
            .withNoteType("general", "usage")
            .build();
        entry = d.getComponentBuilder().entry().withForm("qyz", "walk")
            .addSense(s -> s.withGloss("en", "to walk")).build();
        subscription = d.addChangeListener(events::add);
    }

    private LiftSense sense() {
        return entry.getSenses().getFirst();
    }

    private Feature noteType(String id) {
        return d.getHeader().getNoteTypeManager().getFeature(id);
    }

    @Test
    void anAdditionIsReportedOnceAndRevertedByADeletion() {
        LiftSense s = new LiftSense();
        s.addGloss(new Form("en", "to stroll"));
        entry.addSense(0, s);

        // The id the dictionary gives the new sense, then the addition itself.
        assertEquals(2, events.size());
        ValueChanged<?> id = assertInstanceOf(ValueChanged.class, events.getFirst());
        assertSame(ModelProperty.LIFT_ID, id.property());
        ComponentAdded added = assertInstanceOf(ComponentAdded.class, events.getLast());
        assertSame(entry, added.parent());
        assertSame(s, added.component());
        assertEquals(OptionalInt.of(0), added.position());
        assertTrue(added.registered());

        added.revert();
        assertFalse(entry.getSenses().contains(s));
        assertNull(s.getUUID(), "the revert unregisters the sense");
        added.reapply();
        assertSame(s, entry.getSenses().getFirst());
        assertNotNull(s.getUUID());
    }

    @Test
    void aBuilderAdditionIsReportedOnceToo() {
        LiftSense s = d.getComponentBuilder().sense(entry).withGloss("en", "to march").build();
        assertEquals(1, events.stream().filter(e -> e instanceof ComponentAdded).count());
        ComponentAdded added = (ComponentAdded) events.stream().filter(e -> e instanceof ComponentAdded).findFirst().orElseThrow();
        assertSame(s, added.component());
        assertTrue(added.registered(), "a revert must unregister what the builder registered");
    }

    @Test
    void aDeletionRemembersItsPosition() {
        LiftSense second = new LiftSense();
        entry.addSense(second);
        LiftSense first = sense();
        events.clear();

        entry.deleteSense(first);
        ComponentRemoved removed = assertInstanceOf(ComponentRemoved.class, events.getFirst());
        assertEquals(OptionalInt.of(0), removed.position());
        assertTrue(removed.unregistered());

        removed.revert();
        assertSame(first, entry.getSenses().getFirst(), "put back at its position");
    }

    @Test
    void aDetachKeepsTheRegistrationAndSaysSo() {
        LiftSense s = sense();
        s.detach();
        ComponentRemoved removed = assertInstanceOf(ComponentRemoved.class, events.getFirst());
        assertFalse(removed.unregistered());
        assertNotNull(s.getUUID());

        LiftEntry other = d.getComponentBuilder().entry().withForm("qyz", "run").build();
        events.clear();
        other.addSense(s);
        ComponentAdded added = assertInstanceOf(ComponentAdded.class, events.getFirst());
        assertFalse(added.registered(), "the sense was already registered");
        added.revert();
        assertNotNull(s.getUUID(), "reverting a move detaches, it does not unregister");
    }

    @Test
    void aMapHeldComponentHasNoPosition() {
        LiftNote note = new LiftNote(noteType("general"));
        entry.addNote(note);
        ComponentAdded added = assertInstanceOf(ComponentAdded.class, events.getFirst());
        assertTrue(added.position().isEmpty());
        assertTrue(note.positionInParent().isEmpty());
    }

    @Test
    void aRefusedOperationReportsNothing() {
        entry.addNote(new LiftNote(noteType("general")));
        events.clear();
        assertThrows(DuplicateTypeException.class, () -> entry.addNote(new LiftNote(noteType("general"))));
        assertTrue(events.isEmpty());
    }

    @Test
    void aDetachedObjectReportsNothing() {
        LiftEntry detached = new LiftEntry();
        detached.addForm(new Form("qyz", "x"));
        detached.addSense(new LiftSense());
        detached.setDateCreated("2026-10-14");
        assertTrue(events.isEmpty());
    }

    @Test
    void aValueChangeCarriesTheOldAndTheNewValue() {
        entry.setDateModified("2026-10-14");
        ValueChanged<?> changed = assertInstanceOf(ValueChanged.class, events.getFirst());
        assertSame(ModelProperty.DATE_MODIFIED, changed.property());
        assertNull(changed.oldValue());
        assertEquals("2026-10-14", changed.newValue());
        changed.revert();
        assertTrue(entry.getDateModified().isEmpty());
    }

    @Test
    void typingInATextIsReportedAsFormChanges() {
        var text = sense().getGlosses().formTextProperty("fr");
        text.set("m");
        text.set("marcher");
        text.set("");
        assertInstanceOf(FormAdded.class, events.get(0));
        ValueChanged<?> typed = assertInstanceOf(ValueChanged.class, events.get(1));
        assertSame(ModelProperty.FORM_TEXT, typed.property());
        assertEquals("m", typed.oldValue());
        assertEquals("marcher", typed.newValue());
        assertInstanceOf(FormRemoved.class, events.get(2));
        for (int i = events.size() - 1; i >= 0; i--) events.get(i).revert();
        assertFalse(sense().getGlosses().containsLang("fr"));
    }

    @Test
    void aSpanSurvivesAnUndoOfItsText() {
        Form f = sense().getGlosses().getForm("en").orElseThrow();
        f.changeText("<span lang=\"fr\">aller</span> à <span class=\"x\">pied</span>");
        String marked = f.getMarkup();
        f.changeText("plain");
        events.getLast().revert();
        assertEquals(marked, f.getMarkup());
    }

    @Test
    void aNewLanguageComesBeforeTheComponentThatBringsIt() {
        LiftSense s = new LiftSense();
        LiftEntry detached = new LiftEntry();
        detached.addForm(new Form("qaa", "x"));
        d.addEntry(detached);
        assertInstanceOf(LanguageAdded.class, events.get(0));
        assertInstanceOf(ComponentAdded.class, events.get(1));
        for (int i = events.size() - 1; i >= 0; i--) events.get(i).revert();
        assertFalse(d.getObjectLanguageManager().hasLanguage("qaa"));
        assertNotNull(s);
    }

    @Test
    void headerChangesAreReported() {
        Feature f = d.getHeader().getNoteTypeManager().addFeature("comment");
        assertInstanceOf(FeatureAdded.class, events.getFirst());
        d.getHeader().getNoteTypeManager().changeFeatureId(f, "remark");
        ValueChanged<?> renamed = assertInstanceOf(ValueChanged.class, events.getLast());
        assertSame(ModelProperty.FEATURE_ID, renamed.property());
        renamed.revert();
        assertSame(f, d.getHeader().getNoteTypeManager().getFeature("comment"));
    }

    @Test
    void aFeatureSetRenameIsReportedOnceAndReverted() {
        FeatureSet usage = d.getHeader().addFeatureSet("usage");
        events.clear();
        d.getHeader().renameFeatureSet(usage, "register");
        assertEquals(1, events.size());
        ValueChanged<?> renamed = assertInstanceOf(ValueChanged.class, events.getFirst());
        assertSame(ModelProperty.FEATURE_SET_ID, renamed.property());
        assertEquals("usage", renamed.oldValue());
        assertEquals("register", renamed.newValue());

        renamed.revert();
        assertSame(usage, d.getHeader().getFeatureSet("usage"));
        assertFalse(d.getHeader().hasFeatureSet("register"));

        events.clear();
        FeatureSet pos = d.getHeader().getGrammaticalInfoManager();
        assertThrows(IllegalArgumentException.class, () -> d.getHeader().renameFeatureSet(pos, "pos"));
        assertTrue(events.isEmpty(), "a refused rename reports nothing");
    }

    @Test
    void aListenerCannotChangeTheDictionary() {
        Subscription mutating = d.addChangeListener(c -> entry.setDateCreated("now"));
        assertThrows(IllegalStateException.class, () -> entry.setDateModified("2026-10-14"));
        mutating.unsubscribe();
    }

    @Test
    void anUnsubscribedListenerHearsNothing() {
        subscription.unsubscribe();
        entry.setDateModified("2026-10-14");
        assertTrue(events.isEmpty());
    }

    @Test
    void aRecorderRevertsUnlessCommitted() {
        try (ChangeRecorder rec = d.recordChanges()) {
            entry.setDateModified("2026-10-14");
            entry.addNote(new LiftNote(noteType("usage")));
            assertEquals(2, rec.changes().size());
        }
        assertTrue(entry.getDateModified().isEmpty());
        assertTrue(entry.getNotes().isEmpty());

        try (ChangeRecorder rec = d.recordChanges()) {
            entry.setDateModified("2026-10-14");
            rec.commit();
        }
        assertEquals("2026-10-14", entry.getDateModified().orElseThrow());
    }

    @Test
    void aNestedRecorderRevertsOnlyItsOwnChanges() {
        try (ChangeRecorder outer = d.recordChanges()) {
            entry.setDateCreated("2026-01-01");
            try (ChangeRecorder inner = d.recordChanges()) {
                entry.setDateModified("2026-10-14");
            }
            assertTrue(entry.getDateModified().isEmpty());
            assertEquals("2026-01-01", entry.getDateCreated().orElseThrow());
            outer.commit();
        }
        assertEquals("2026-01-01", entry.getDateCreated().orElseThrow());
    }

    @Test
    void anExceptionInTheBlockRevertsTheRecordedChanges() {
        assertThrows(IllegalStateException.class, () -> {
            try (ChangeRecorder rec = d.recordChanges()) {
                entry.setDateModified("2026-10-14");
                throw new IllegalStateException("failed import");
            }
        });
        assertTrue(entry.getDateModified().isEmpty());
    }

    @Test
    void loadingReportsNothingEvenWhileAnotherDictionaryIsObserved() throws Exception {
        // This dictionary is observed, so emission points cannot take their fast path.
        long before = ChangeSupport.firedCount();
        LiftDictionary loaded = LiftDictionary.loadDictionaryFromFile(
            new File("src/test/resources/lift/DictionaryLift20261022.lift"));
        assertEquals(before, ChangeSupport.firedCount());
        assertTrue(events.isEmpty());
        assertFalse(loaded.getLiftDictionaryRegistry().getEntries().isEmpty());
    }

    @Test
    void aTraitValueIsReportedTyped() {
        var def = d.getHeader().declareTraitDefinition("status");
        LiftTrait t = new LiftTrait(def, "draft");
        entry.addTrait(t);
        events.clear();
        t.setValue("final");
        ValueChanged<?> changed = assertInstanceOf(ValueChanged.class, events.getFirst());
        assertSame(ModelProperty.TRAIT_VALUE, changed.property());
        assertEquals("draft", changed.oldValue());
        changed.revert();
        assertEquals("draft", t.getValue());
    }

    @Test
    void aTextChangeLeadsBackToItsComponent() {
        Form f = sense().getGlosses().getForm("en").orElseThrow();
        assertSame(sense().getGlosses(), f.getMultiText());
        f.changeText("to amble");
        ValueChanged<?> changed = assertInstanceOf(ValueChanged.class, events.getLast());
        Form owner = assertInstanceOf(Form.class, changed.owner());
        assertSame(sense(), owner.getMultiText().getParent());

        sense().getGlosses().removeForm("en");
        assertNull(f.getMultiText(), "a removed form belongs to no multitext");
        assertNull(new Form("en", "loose").getMultiText());
    }
}
