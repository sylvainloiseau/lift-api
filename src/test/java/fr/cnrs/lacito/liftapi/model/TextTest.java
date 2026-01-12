package fr.cnrs.lacito.liftapi.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.TextSpan;

public class TextTest {
    @Test
    public void testChangeTextWithNestedSpans() {
        Form text = new Form("fr");
        String input = "Bonjour <span lang=\"en\">Hello <span class=\"em\">world</span></span> !";
        text.changeText(input);

        // On vérifie la structure du TextSpan racine
        List<TextSpan> spanList = text.walkTextSpanTree();
        assertNotNull(spanList);

        // Le root doit avoir 7 enfants : 
        // span(), 
        //   span("Bonjour "),
        //   span(lang="en"),
        //       span("Hello "),
        //       span(class="em"),
        //           span("world"),
        //   span(" !")
        assertEquals(7, spanList.size());

        // Premier enfant : texte brut
        assertEquals("Bonjour ", spanList.get(1).toString());

        // Deuxième enfant : span anglais
        TextSpan enSpan = spanList.get(2);
        assertEquals("en", enSpan.getLang().get());
        // Ce span doit avoir 2 enfants : "Hello " et un span imbriqué
        List<TextSpan> enSpanList = new ArrayList<>();
        enSpan.walkTextSpanTree(enSpanList);
        assertEquals(4, enSpanList.size());
        assertEquals("Hello ", enSpanList.get(1).toString());

        // Span imbriqué : class="em"
        TextSpan emSpan = enSpanList.get(2);
        assertEquals("em", emSpan.getSClass().get());
        assertEquals("<span class=\"em\">world</span>", emSpan.toString());

        // Troisième enfant : texte brut
        assertEquals(" !", spanList.get(6).toString());
    }    
}
