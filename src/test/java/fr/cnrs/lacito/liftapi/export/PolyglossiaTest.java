package fr.cnrs.lacito.liftapi.export;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PolyglossiaTest {

    @Test
    void namesAndCodes() {
        assertTrue(Polyglossia.names().size() > 100);
        assertTrue(Polyglossia.isKnown("french"));
        assertTrue(Polyglossia.isKnown("british"), "an alias");
        assertFalse(Polyglossia.isKnown("tuwari"));
        assertEquals("french", Polyglossia.nameFor("fr").orElseThrow());
        assertEquals("french", Polyglossia.nameFor("fr-FR").orElseThrow());
        assertEquals("french", Polyglossia.nameFor("fra").orElseThrow(), "three letters");
        assertEquals("occitan", Polyglossia.nameFor("oc-FR-fonipa").orElseThrow());
        assertEquals("english", Polyglossia.nameFor("EN").orElseThrow());
        assertTrue(Polyglossia.nameFor("tww").isEmpty());
        assertTrue(Polyglossia.nameFor("qaa-x-spec").isEmpty());
        assertTrue(Polyglossia.nameFor("").isEmpty());
    }
}
