package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.cnrs.lacito.liftapi.model.DuplicateTypeException;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTranslation;
import fr.cnrs.lacito.liftapi.model.MultiText;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Translations of an example are components ({@link LiftTranslation}).
 *
 * They used to be bare MultiTexts in a map of their example: one created on an example
 * already in a dictionary was never registered - no UUID, absent from the meta-language
 * texts, its languages neither checked nor counted. These tests pin the translation to
 * the same attached / detached contract as every other component.
 */
public class TranslationTest {

    private static LiftDictionary dictionary() {
        return LiftDictionary.makeBuilder()
            .withLiftVersion(LiftVersion.V0_15)
            .withProducer("TranslationTest")
            .withObjectLanguages("qyz")
            .withMetaLanguages("en", "fr")
            .withTranslationType("free", "literal")
            .build();
    }

    private static LiftSense sense(LiftDictionary d) {
        LiftEntry entry = d.getComponentBuilder().entry().withForm("qyz", "run").build();
        return d.getComponentBuilder().sense(entry).withGloss("en", "to run").build();
    }

    private static LiftExample example(LiftDictionary d, LiftSense sense) {
        return d.getComponentBuilder().example(sense, "qyz", "he runs fast");
    }

    private static Feature type(LiftDictionary d, String id) {
        return d.getHeader().getTranslationTypeManager().getFeature(id);
    }

    // ------------------------------------------------------------------
    // The original bug: a translation created on an attached example
    // ------------------------------------------------------------------

    @Test
    public void translationCreatedOnAnAttachedExampleIsRegistered() {
        LiftDictionary d = dictionary();
        LiftDictionaryRegistry registry = d.getLiftDictionaryRegistry();
        LiftExample example = example(d, sense(d));

        MultiText text = example.createTranslation(type(d, "free"));

        assertNotNull(text.getUUID(), "the translation text should have been registered");
        assertTrue(registry.getMetaText().contains(text));
        assertEquals(1, registry.getTranslations().size());
        LiftTranslation translation = registry.getTranslations().get(0);
        assertSame(text, translation.getTranslation());
        assertSame(example, translation.getParent());
        assertSame(d, translation.getOwningDictionary());
    }

    @Test
    public void registeredTranslationTextChecksAndCountsItsLanguages() {
        LiftDictionary d = dictionary();
        LiftExample example = example(d, sense(d));
        int before = d.getMetaLanguageManager().getLanguageOccurrence("fr");

        MultiText text = example.getOrCreateTranslation(type(d, "free"));
        text.add(new Form("fr", "il court vite"));

        assertEquals(before + 1, d.getMetaLanguageManager().getLanguageOccurrence("fr"));
        assertThrows(
            IllegalArgumentException.class,
            () -> text.add(new Form("xx", "undeclared")),
            "a registered text should refuse a language the dictionary does not declare"
        );
    }

    @Test
    public void getOrCreateTranslationReturnsTheExistingOne() {
        LiftDictionary d = dictionary();
        LiftExample example = example(d, sense(d));

        MultiText first = example.getOrCreateTranslation(type(d, "free"));
        MultiText second = example.getOrCreateTranslation(type(d, "free"));

        assertSame(first, second);
        assertEquals(1, d.getLiftDictionaryRegistry().getTranslations().size());
    }

    @Test
    public void duplicateTypeIsRefused() {
        LiftDictionary d = dictionary();
        LiftExample example = example(d, sense(d));
        example.createTranslation(type(d, "free"));

        assertThrows(
            DuplicateTypeException.class,
            () -> example.addTranslation(LiftTranslation.create(type(d, "free")))
        );
    }

    // ------------------------------------------------------------------
    // Detached translations and the view on them
    // ------------------------------------------------------------------

    @Test
    public void translationOfADetachedExampleIsRegisteredWithIt() {
        LiftDictionary d = dictionary();
        LiftSense sense = sense(d);

        LiftExample example = LiftExample.create();
        example.getExample().add(new Form("qyz", "he runs fast"));
        LiftTranslation translation = LiftTranslation.create(type(d, "free"));
        translation.getTranslation().add(new Form("en", "he runs fast"));
        example.addTranslation(translation);
        assertNull(translation.getUUID(), "nothing is registered while detached");

        sense.addExample(example);

        assertNotNull(translation.getUUID());
        assertNotNull(translation.getTranslation().getUUID());
    }

    @Test
    public void translationsViewIsReadOnlyAndKeepsInsertionOrder() {
        LiftDictionary d = dictionary();
        LiftExample example = example(d, sense(d));
        example.createTranslation(type(d, "literal"));
        example.createTranslation(type(d, "free"));

        assertEquals(
            List.of(type(d, "literal"), type(d, "free")),
            List.copyOf(example.getTranslations().keySet())
        );
        assertThrows(
            UnsupportedOperationException.class,
            () -> example.getTranslations().put(type(d, "free"), new MultiText(example))
        );
    }

    // ------------------------------------------------------------------
    // Deletion
    // ------------------------------------------------------------------

    @Test
    public void deleteTranslationUnregistersIt() {
        LiftDictionary d = dictionary();
        LiftDictionaryRegistry registry = d.getLiftDictionaryRegistry();
        LiftExample example = example(d, sense(d));
        MultiText text = example.createTranslation(type(d, "free"));
        text.add(new Form("fr", "il court vite"));
        LiftTranslation translation = registry.getTranslations().get(0);
        int before = d.getMetaLanguageManager().getLanguageOccurrence("fr");

        example.deleteTranslation(translation);

        assertTrue(example.getTranslations().isEmpty());
        assertTrue(example.getTranslationComponents().isEmpty());
        assertNull(translation.getParent());
        assertNull(translation.getUUID());
        assertNull(text.getUUID());
        assertFalse(registry.getTranslations().contains(translation));
        assertFalse(registry.getMetaText().contains(text));
        assertEquals(before - 1, d.getMetaLanguageManager().getLanguageOccurrence("fr"));
    }

    @Test
    public void deletingTheExampleUnregistersItsTranslations() {
        LiftDictionary d = dictionary();
        LiftDictionaryRegistry registry = d.getLiftDictionaryRegistry();
        LiftSense sense = sense(d);
        LiftExample example = example(d, sense);
        MultiText text = example.createTranslation(type(d, "free"));

        sense.deleteExample(example);

        assertTrue(registry.getTranslations().isEmpty());
        assertNull(text.getUUID());
        assertFalse(registry.getMetaText().contains(text));
        // the translation stays wired to its example, ready to be attached again
        assertSame(text, example.getTranslation(type(d, "free")));
    }

    @Test
    public void deletingATranslationOfAnotherExampleIsRefused() {
        LiftDictionary d = dictionary();
        LiftSense sense = sense(d);
        LiftExample a = example(d, sense);
        LiftExample b = example(d, sense);
        a.createTranslation(type(d, "free"));
        LiftTranslation ofA = a.getTranslationComponents().iterator().next();

        assertThrows(IllegalArgumentException.class, () -> b.deleteTranslation(ofA));
    }

    // ------------------------------------------------------------------
    // Builders
    // ------------------------------------------------------------------

    @Test
    public void nestedBuildersRegisterTranslations() {
        LiftDictionary d = dictionary();
        LiftDictionaryRegistry registry = d.getLiftDictionaryRegistry();

        d.getComponentBuilder()
            .entry()
            .withForm("qyz", "run")
            .addSense(s ->
                s
                    .withGloss("en", "to run")
                    .addExample(ex ->
                        ex
                            .withExample("qyz", "he runs fast")
                            .addTranslation("free", "en", "he runs fast")
                            .addTranslation("free", "fr", "il court vite")
                            .addTranslation("literal", t -> t.addText("en", "he runs quick"))
                    )
            )
            .build();

        assertEquals(2, registry.getTranslations().size());
        LiftExample example = registry.getExamples().get(0);
        MultiText free = example.getTranslation(type(d, "free"));
        assertNotNull(free.getUUID());
        assertEquals(2, free.getForms().size(), "forms of one type make one translation");
        assertTrue(registry.getMetaText().contains(free));
        assertTrue(registry.getMetaText().contains(example.getTranslation(type(d, "literal"))));
    }

    @Test
    public void builderAddsANewTranslationTypeToTheHeader() {
        LiftDictionary d = dictionary();
        LiftExample example = example(d, sense(d));

        LiftTranslation t = d.getComponentBuilder().translation(example, "back", "en", "back");

        assertTrue(d.getHeader().getTranslationTypeManager().hasFeature("back"));
        assertNotNull(t.getUUID());
        assertSame(t.getTranslation(), example.getTranslation(type(d, "back")));
    }

    @Test
    public void builderRefusesATypeGivenBothWays() {
        LiftDictionary d = dictionary();
        LiftSense sense = sense(d);

        assertThrows(
            IllegalStateException.class,
            () -> d.getComponentBuilder()
                .example(sense)
                .withExample("qyz", "he runs fast")
                .addTranslation("free", t -> t.addText("en", "he runs fast"))
                .addTranslation("free", "fr", "il court vite")
        );
    }
}
