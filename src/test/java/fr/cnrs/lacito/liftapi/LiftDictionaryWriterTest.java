package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.xml.LiftVersion;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Save-path behaviour.
 *
 * The content of what is written is covered by
 * {@link fr.cnrs.lacito.liftapi.xml.RoundTripTest}; what is checked here is that
 * saving is safe: it never leaves a half-written file behind, and it never
 * rewrites files it did not read.
 */
public class LiftDictionaryWriterTest {

    private static final Logger LOGGER = Logger.getLogger(
        LiftDictionaryWriterTest.class.getName()
    );

    @TempDir
    File tempDir;

    @Test
    public void saveWritesAReadableFile() throws Exception {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/20240828Lift.lift");
        File out = new File(tempDir, "20240828Lift.written.lift");
        LOGGER.info("Writing to " + out);
        lf.save(out);

        assertTrue(out.isFile(), "save() should have created " + out);
        assertTrue(out.length() > 0, "save() produced an empty file");
        assertEquals(
            lf.getLiftDictionaryRegistry().getEntries().size(),
            Utils.loadDictionaryForTest(out)
                .getLiftDictionaryRegistry()
                .getEntries()
                .size()
        );
    }

    /**
     * A failing save must not destroy the file it was about to overwrite: the
     * dictionary is written to a temporary sibling and moved into place only once
     * the whole document has been serialized.
     */
    @Test
    public void failedSaveLeavesTheExistingFileIntact() throws Exception {
        LiftDictionary lf = LiftDictionary.makeBuilder()
            .withLiftVersion(LiftVersion.V0_13)
            .withObjectLanguages("tww")
            .withMetaLanguages("en")
            .withRelationType("synonym")
            .build();
        LiftEntry target = lf
            .getComponentBuilder()
            .entry()
            .withForm("tww", "nala")
            .build();
        LiftEntry source = lf
            .getComponentBuilder()
            .entry()
            .withForm("tww", "kemia")
            .addRelation(r -> r.withRef(target).withType("synonym"))
            .build();

        File file = new File(tempDir, "dictionary.lift");
        lf.save(file);
        String goodContent = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        assertTrue(goodContent.contains("kemia"));

        // Break the model so that serialization fails part way through the document:
        // a relation with no type makes the writer throw while the entries are
        // already being written.
        source.getRelations().get(0).setType(null);

        assertThrows(
            WrittingLiftDocumentException.class,
            () -> lf.save(file)
        );

        assertEquals(
            goodContent,
            Files.readString(file.toPath(), StandardCharsets.UTF_8),
            "A save that fails half way must leave the previous file untouched"
        );
        assertEquals(
            List.of(file.getName()),
            List.of(tempDir.list()),
            "The failed save left a temporary file behind"
        );
    }

    /**
     * {@code 20240828Lift.lift} declares {@code <range href="..."/>} pointing at a
     * ranges file that is never loaded. Saving must not create or truncate that
     * file, because the data to put in it was never read.
     */
    @Test
    public void saveDoesNotRewriteExternalRangesThatWereNeverRead()
        throws Exception {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/20240828Lift.lift");
        File out = new File(tempDir, "out.lift");
        lf.save(out);

        File externalRanges = new File(
            tempDir,
            "20240828Lift/20240828Lift.lift-ranges"
        );
        assertTrue(
            !externalRanges.exists(),
            "Saving rewrote an external ranges file that was never loaded: " +
                externalRanges
        );
    }
}
