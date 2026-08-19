package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.cnrs.lacito.liftapi.builder.DictionaryObjectBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LiftDictionaryRegisterTest {

    LiftDictionary dictionary;

    @BeforeEach
    public void setUp() {
        this.dictionary = LiftDictionary.makeBuilder()
            .withLiftVersion(LiftVersion.V0_13)
            .withProducer("Test Producer")
            .build();
    }

    @Test
    public void testEntryCount() {
        DictionaryObjectBuilderFactory builder = dictionary.getComponentBuilder();

        builder.entry().withForm("en", "dictionary").build();
        // this method should be refactored
        //assertEquals(1, dictionary.entryCount());

        builder.entry().withForm("en", "registry").build();
        //assertEquals(1, dictionary.entryCount());
    }

    @Test
    public void testRegistryEntryById() {
        DictionaryObjectBuilderFactory builder = dictionary.getComponentBuilder();
        LiftDictionaryRegistry registry =
            dictionary.getLiftDictionaryRegistry();

        builder.entry().withForm("en", "dictionary").build();
        assertEquals(1, registry.getEntriesById().keySet().size());

        builder.entry().withForm("en", "registry").build();
        assertEquals(2, registry.getEntriesById().keySet().size());
    }

    @Test
    public void testRegistryEntryList() {
        DictionaryObjectBuilderFactory builder = dictionary.getComponentBuilder();
        LiftDictionaryRegistry registry =
            dictionary.getLiftDictionaryRegistry();

        assertEquals(0, registry.getEntries().size());

        builder.entry().withForm("en", "dictionary").build();
        assertEquals(1, registry.getEntries().size());

        builder.entry().withForm("en", "registry").build();
        assertEquals(2, registry.getEntries().size());
    }

    @Test
    public void testRegistryThrowsExceptionOnAdd() {
        DictionaryObjectBuilderFactory builder = dictionary.getComponentBuilder();

        LiftDictionaryRegistry registry =
            dictionary.getLiftDictionaryRegistry();

        assertEquals(0, registry.getEntries().size());

        builder.entry().withForm("en", "dictionary").build();
        assertEquals(1, registry.getEntries().size());

        assertThrows(Exception.class, () -> {
            registry
                .getEntries()
                .add(builder.entry().withForm("en", "foo").build());
        });
    }
}
