package fr.cnrs.lacito.liftapi.model;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class FormTest {

    @Test public void testNullOrEmptyLang () {
        assertThrows(IllegalArgumentException.class, () -> new Form(null, "text"));
        assertThrows(IllegalArgumentException.class, () -> new Form("", "text"));
    }

    @Test public void testNullText () {
        assertThrows(IllegalArgumentException.class, () -> new Form("en", null));
    }

}
