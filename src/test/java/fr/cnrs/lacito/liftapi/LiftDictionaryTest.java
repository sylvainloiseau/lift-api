package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import fr.cnrs.lacito.liftapi.model.LiftHeaderRangeElement;

public class LiftDictionaryTest {

    private static final Logger LOGGER = Logger.getLogger(LiftDictionary.class.getName());

    @Test
    public void testObjectLanguagesInAllFields () {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/tinywithseveralObjectLanguageInVariousPlaces.xml");
        Set<String> objectLanguages = lf.getObjectLanguagesOfAllText();
        assertTrue(objectLanguages.containsAll(Arrays.asList("tww", "tpi")));
    }

    @Test
    public void testGetMetaLanguagesInAllField() {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/20240828Lift.lift");
        Set<String> metaLanguages = lf.getMetaLanguagesOfAllText();
        assertEquals(2, metaLanguages.size());
        //System.out.println(metaLanguages.toString());
        assertTrue(metaLanguages.contains("tpi"));
        assertTrue(metaLanguages.containsAll(Arrays.asList("tpi", "tpi")));
    }

    @Test
    public void testGetGramInfoSet () {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/tiny.xml");
        Set<String> gramInfo = lf.getGramInfoSet();
        assertTrue(gramInfo.containsAll(Arrays.asList("Interrogative pro-form", "Noun")));
    }

    @Test
    public void testGetGramInfoCounter () {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/tiny.xml");
        Map<String, Long> gramInfo = lf.getGramInfoCounter();
        LOGGER.info(gramInfo.toString());
        assertEquals(1, gramInfo.get("Interrogative pro-form"));
    }

    @Test
    public void testGetGramInfoCounterLargeDictionary () {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/20240828Lift.lift");
        Map<String, Long> gramInfo = lf.getGramInfoCounter();
        assertEquals(14, gramInfo.get("Interrogative pro-form"));
    }

    @Test
    public void testObjectLanguagesInForm () {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/tiny.xml");
        Set<String> objectLanguages = lf.getObjectLanguagesInLexicalUnit();
        assertTrue(objectLanguages.containsAll(Arrays.asList("tww")));
    }

    @Test
    public void testGetTraitName() {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/20240828Lift.lift");
        Set<String> traitNames = lf.getTraitName();
        LOGGER.info(traitNames.toString());
        assertTrue(traitNames.contains("semantic-domain-ddp4"));
    }

    @Test
    public void testGetValueCounterForTraitName() {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/20240828Lift.lift");
        Map<String, Long> traitValueCounter = lf.getValueCounterForTraitName("semantic-domain-ddp4");
        assertEquals(traitValueCounter.get("6.7 Tool"), Long.valueOf(33));
    }

    @Test
    public void testGetLangInObjectTextSpan() {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/tinyTextSpan.xml");
        Set<String> langInSpan = lf.getLangInObjectTextSpan();
        LOGGER.info(langInSpan.toString());
        assertTrue(langInSpan.contains("foo"));
    }

    @Test
    public void testGetTranslationType() {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/tiny_translation.xml");
        Set<String> translationType = lf.getHeader().getTranslationTypeManager().getRangeElements().values().stream().map(LiftHeaderRangeElement::getId).collect(Collectors.toSet());
        assertEquals(new HashSet<String>(Arrays.asList("free", "litteral")), translationType);
    }

}
