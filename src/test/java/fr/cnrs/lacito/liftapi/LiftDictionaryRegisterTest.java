package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.cnrs.lacito.liftapi.builder.DictionaryComponentBuilderFactory;
import fr.cnrs.lacito.liftapi.model.LiftEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LiftDictionaryRegisterTest {

    LiftDictionary dictionary;

    @BeforeEach
    public void setUp() {
        this.dictionary = LiftDictionary.makeBuilder()
            .withLiftVersion(LiftVersion.V0_13)
            .withProducer("Test Producer")
            .withObjectLanguages("en")
            .build();
    }

    @Test
    public void testEntryCount() {
        DictionaryComponentBuilderFactory builder = dictionary.getComponentBuilder();

        builder.entry().withForm("en", "dictionary").build();
        // this method should be refactored
        //assertEquals(1, dictionary.entryCount());

        builder.entry().withForm("en", "registry").build();
        //assertEquals(1, dictionary.entryCount());
    }

    @Test
    public void testRegistryEntryById() {
        DictionaryComponentBuilderFactory builder = dictionary.getComponentBuilder();
        LiftDictionaryRegistry registry =
            dictionary.getLiftDictionaryRegistry();

        builder.entry().withForm("en", "dictionary").build();
        assertEquals(1, registry.getEntriesById().keySet().size());

        builder.entry().withForm("en", "registry").build();
        assertEquals(2, registry.getEntriesById().keySet().size());
    }

    @Test
    public void testRegistryEntryList() {
        DictionaryComponentBuilderFactory builder = dictionary.getComponentBuilder();
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
        DictionaryComponentBuilderFactory builder = dictionary.getComponentBuilder();

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

    @Test
    public void testReferenceCounting() {
        dictionary.getHeader().getRelationTypeManager().addFeature("suppletion");

        DictionaryComponentBuilderFactory builder = dictionary.getComponentBuilder();

        LiftEntry e = builder.entry().withForm("en", "dictionary").build();
        LiftEntry source = builder.entry().withForm("en", "source")
           .addRelation(
            r -> r.withRef(e).withType("suppletion")
        ).build();

        // the "e" entry cannot be removed since it is referenced from the "source" node.
        LiftDictionaryRegistry registry =
            dictionary.getLiftDictionaryRegistry();
        assertThrows(IllegalStateException.class, () -> {
            registry
                .removeFromDictionary(e);
        });
        
        // the e entry can now be removed
        registry
                .removeFromDictionary(source);
        registry
                .removeFromDictionary(e);
    }
}
