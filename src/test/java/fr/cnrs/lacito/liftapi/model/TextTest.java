package fr.cnrs.lacito.liftapi.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class TextTest {
    @Test
    public void testChangeTextWithNestedSpans() {
        Form text = new Form("fr");
        String input = "Bonjour <span lang=\"en\">Hello <span class=\"em\">world</span></span> !";
        text.changeText(input);

        // On vérifie la structure du TextSpan racine
        List<TextSpan> spanList = text.walkTextSpanTree();
        assertNotNull(spanList);

        // The root should have 7 children :
        // span(),
        //   span("Bonjour "),
        //   span(lang="en"),
        //       span("Hello "),
        //       span(class="em"),
        //           span("world"),
        //   span(" !")
        assertEquals(7, spanList.size());

        // First child : raw text
        assertEquals("Bonjour ", spanList.get(1).toString());

        // Second child : English span
        TextSpan enSpan = spanList.get(2);
        assertEquals("en", enSpan.getLang().get());
        // This Span élément should have two children : "Hello " and a nested span
        List<TextSpan> enSpanList = new ArrayList<>();
        enSpan.walkTextSpanTree(enSpanList);
        assertEquals(4, enSpanList.size());
        assertEquals("Hello ", enSpanList.get(1).toString());

        // Nested Span : class="em"
        TextSpan emSpan = enSpanList.get(2);
        assertEquals("em", emSpan.getSClass().get());
        assertEquals("<span class=\"em\">world</span>", emSpan.toString());

        // Third child : raw text
        assertEquals(" !", spanList.get(6).toString());
    }    
}
