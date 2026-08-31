package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.xml.LiftVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FeatureBuilderTest {

    LiftDictionary dictionary;

    @BeforeEach
    public void setUp() {
        this.dictionary = LiftDictionary.makeBuilder()
            .withLiftVersion(LiftVersion.V0_13)
            .withProducer("Test Producer")
            .withMetaLanguages("en", "fr")
            .withObjectLanguages("tww", "tpi")
            .addFeatureSet(
                e -> e.withLabel("en", "foo"),
                "myclassification"
            )
            .build();
    }

    @Test
    public void testFeatureBuilderRaw() {
        assertEquals(9, dictionary.getHeader().getFeatureSets().size());
        Feature f = new FeatureBuilder(
            dictionary,
            dictionary.getHeader().getFeatureSet("myclassification"),
            "myfeature"
        ).build();
        assertEquals("myfeature", f.getId());
        assertEquals("myclassification", f.getParentFeatureSet().getId());
        assertEquals(9, dictionary.getHeader().getFeatureSets().size());
    }

    @Test
    public void testFeatureBuilderPropertyDoublonException() {
        new FeatureBuilder(
            dictionary,
            dictionary.getHeader().getFeatureSet("myclassification"),
            "myfeature"
        ).build();

        FeatureBuilder f2 = new FeatureBuilder(
            dictionary,
            dictionary.getHeader().getFeatureSet("myclassification"),
            "myfeature"
        );

        assertThrows(IllegalArgumentException.class, () -> f2.build());
    }

}
