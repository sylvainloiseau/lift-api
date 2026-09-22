package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.LiftVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FeatureSetBuilderTest {

    LiftDictionary dictionary;

    @BeforeEach
    public void setUp() {
        this.dictionary = LiftDictionary.makeBuilder()
            .withLiftVersion(LiftVersion.V0_13)
            .withProducer("Test Producer")
            .withMetaLanguages("en", "fr")
            .withObjectLanguages("tww", "tpi")
            .build();
    }

    @Test
    public void testFeatureBuilderRaw() {
        FeatureSet fs = new FeatureSetBuilder(
            dictionary,
            "myfeature"
        ).build();
        assertEquals("myfeature", fs.getId());
    }

    @Test
    public void testFeatureBuilderRaw2() {
        FeatureSet fs = new FeatureSetBuilder(
            dictionary,
            "myfeature"
        ).withAbbreviation("en", "myabbreviation")
        .withDescription("en", "my description")
        .withHref("http://www.foo.fr")
        .withLabel("en", "my label")
        .build();
        assertEquals("myfeature", fs.getId());
        assertEquals("http://www.foo.fr", fs.getHref().get());
        assertEquals("my description", fs.getDescription().getForm("en").get().toPlainText());
        assertEquals(0, fs.getFeatures().keySet().size());
    }

    @Test
    public void testFeatureSetBuilderDuplicate() {
        new FeatureSetBuilder(
            dictionary,
            "myfeature"
        ).build();

        FeatureSetBuilder fsb = new FeatureSetBuilder(
            dictionary,
            "myfeature"
        );
        assertThrows(IllegalArgumentException.class, () -> fsb.build());
    }

    @Test
    public void testFeatureSetAddFeature() {
        new FeatureSetBuilder(
            dictionary,
            "myfeature"
        )
        .addFeature(f -> f.withLabel("en", "foo"), "my category 1")
        .addFeature(f -> f.withLabel("en", "bar"), "my category 2")
        .build();

        assertEquals(2, dictionary.getHeader().getFeatureSet("myfeature").getFeatures().keySet().size());
    }

}
