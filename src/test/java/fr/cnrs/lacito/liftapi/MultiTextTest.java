package fr.cnrs.lacito.liftapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;

public class MultiTextTest {
    
    @Test
    public void testText () {
        String fileTested = "lift/tinyTextSpan.xml";
        int entryTested = 0;

        LiftDictionary lf = Utils.loadDictionaryForTest(fileTested);
        Optional<Form> tww = lf.getLiftDictionaryComponents().getAllEntries().get(entryTested).getForms().getForm("tww");
        assertTrue(tww.isPresent());
        assertEquals("<span>nala</span>", tww.get().toString());
    }

    @Test
    public void testTextAndSeveralSpan () {
        String fileTested="lift/tinyTextSpan.xml";
        int entryTested = 2;

        LiftDictionary lf = Utils.loadDictionaryForTest(fileTested);
        Optional<Form> form = lf.getLiftDictionaryComponents().getAllEntries().get(entryTested).getForms().getForm("tww");
        assertTrue(form.isPresent());
        String found = form.get().toString();
        assertEquals("<span><span>kemia <span><span></span>napuo</span></span></span>", found);
    }

    @Test
    public void testTextAndSpan () {
        String fileTested = "lift/tinyTextSpan.xml";
        int entryTested = 1;

        LiftDictionary lf = Utils.loadDictionaryForTest(fileTested);
        Optional<Form> form = lf.getLiftDictionaryComponents().getAllEntries().get(entryTested).getForms().getForm("tww");
        assertTrue(form.isPresent());
        String found = form.get().toString();
        assertEquals("<span>kemia <span>napuo</span></span>", found);
    }

    @Test
    public void testMultiTextLevelAnnotationOnLexicalUnit() {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/tinyMultiTextAnnotation.xml");

        List<LiftAnnotation> annotations = lf.getLiftDictionaryComponents()
            .getAllEntries().getFirst().getForms().getAnnotations();

        assertEquals(1, annotations.size());
        LiftAnnotation a = annotations.getFirst();
        assertEquals("source", a.getName());
        assertEquals("elicitation", a.getValue().orElse(""));
        assertEquals("alice", a.getWho().orElse(""));
        assertEquals("2026-02-22", a.getWhen().orElse(""));
        assertEquals("lexical-unit note", a.getText().getForm("en").map(Form::toPlainText).orElse(""));
    }

}
