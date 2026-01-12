package fr.cnrs.lacito.liftapi;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.Test;

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
        assertTrue(metaLanguages.containsAll(Arrays.asList("tww", "tpi")));
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
        assertEquals(gramInfo.get("Interrogative pro-form"), Long.valueOf(1));
    }

    @Test
    public void testGetGramInfoCounterLargeDictionary () {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/20240828Lift.lift");
        Map<String, Long> gramInfo = lf.getGramInfoCounter();
        assertEquals(gramInfo.get("Interrogative pro-form"), Long.valueOf(20));
    }

    @Test
    public void testObjectLanguagesInForm () {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/tiny.xml");
        Set<String> objectLanguages = lf.get_object_languages_in_lexical_unit();
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
        Set<String> translationType = lf.getTranslationType();
        assertEquals(new HashSet<String>(Arrays.asList("free", "litteral")), translationType); 
    }
    
    
}
