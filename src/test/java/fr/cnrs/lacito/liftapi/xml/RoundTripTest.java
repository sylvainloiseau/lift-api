package fr.cnrs.lacito.liftapi.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.Utils;
import fr.cnrs.lacito.liftapi.WrittingLiftDocumentException;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * {@code load -> save -> load} over the test corpora.
 *
 * This is the regression net for the serialization layer: it fails as soon as a
 * write/read pair loses or mangles anything the model holds, and it checks that
 * saving does not make a document less schema-valid than it was.
 *
 * Output always goes to a JUnit {@link TempDir}, never to the build output directory.
 */
public class RoundTripTest {

    private static final String XSD_0_13 =
        "fr/cnrs/lacito/liftapi/schema/lift-0.13.xsd";

    @TempDir
    File tempDir;

    @Test
    public void roundTripTiny() throws Exception {
        assertRoundTrip("lift/tiny.xml");
    }

    @Test
    public void roundTripLargeDictionary() throws Exception {
        assertRoundTrip("lift/20240828Lift.lift");
    }

    @Test
    public void roundTripTiny015() throws Exception {
        assertRoundTrip("lift/tiny015.xml");
    }

    @Test
    public void roundTripTextSpan() throws Exception {
        assertRoundTrip("lift/tinyTextSpan.xml");
    }

    /**
     * An annotation carried by a MultiText itself has no valid place in the schema:
     * {@code multitext-content} is {@code form*}. Saving therefore moves it inside the
     * first form, where {@code form-no-lang-content} does allow it, and re-reading
     * makes it an annotation of that form. This normalization is deliberate and
     * lossless in content - only the owner changes.
     */
    @Test
    public void multiTextAnnotationIsNormalizedIntoAForm() throws Exception {
        LiftDictionary before = Utils.loadDictionaryForTest(
            "lift/tinyMultiTextAnnotation.xml"
        );
        LiftEntry entryBefore = before
            .getLiftDictionaryRegistry()
            .getEntries()
            .get(0);
        assertEquals(1, entryBefore.getForms().getAnnotations().size());
        assertEquals(
            0,
            entryBefore.getForms().getForm("tww").get().getAnnotations().size()
        );

        File saved = save(before, "annotation.lift");
        LiftEntry entryAfter = Utils.loadDictionaryForTest(saved)
            .getLiftDictionaryRegistry()
            .getEntries()
            .get(0);

        assertEquals(
            0,
            entryAfter.getForms().getAnnotations().size(),
            "the annotation should no longer sit directly on the MultiText"
        );
        List<LiftAnnotation> onForm = entryAfter
            .getForms()
            .getForm("tww")
            .get()
            .getAnnotations();
        assertEquals(1, onForm.size());
        assertEquals("source", onForm.get(0).getType().getId());
        assertEquals("elicitation", onForm.get(0).getValue());
        assertEquals("alice", onForm.get(0).getWho());
        assertEquals(
            "lexical-unit note",
            onForm.get(0).getText().getForm("en").get().toPlainText()
        );

        assertNoNewSchemaErrors(
            Utils.resourceFile("lift/tinyMultiTextAnnotation.xml"),
            saved
        );
    }

    /**
     * The writer must produce the LIFT 0.15 spelling of the constructs that differ
     * between 0.13 and 0.15, and the {@code <label>} / {@code <subsense>} wrappers
     * that {@code lift-0.15.rng} requires.
     */
    @Test
    public void savedFileUsesLift015Vocabulary() throws Exception {
        String saved = Files.readString(
            save(Utils.loadDictionaryForTest("lift/tiny015.xml"), "out.lift")
                .toPath(),
            StandardCharsets.UTF_8
        );

        assertTrue(
            saved.contains("<field name=\"literal-meaning\""),
            "field-content declares @name in 0.15, not @type:\n" + saved
        );
        assertTrue(
            saved.contains("<field-definition name=\"literal-meaning\""),
            "field-defn-content requires @name:\n" + saved
        );
        assertTrue(
            saved.contains("<subsense"),
            "nested senses must be written as <subsense>:\n" + saved
        );
        assertTrue(
            saved.contains("<label>"),
            "media and illustration labels need a <label> wrapper:\n" + saved
        );
        assertTrue(
            saved.contains("encoding=\"UTF-8\"") ||
                saved.contains("encoding=\"utf-8\""),
            "the XML declaration must name the encoding actually used:\n" +
                saved.substring(0, Math.min(200, saved.length()))
        );
    }

    /**
     * The 0.13 corpora must keep the 0.13 spelling.
     */
    @Test
    public void savedFileKeepsLift013Vocabulary() throws Exception {
        String saved = Files.readString(
            save(Utils.loadDictionaryForTest("lift/tiny.xml"), "out013.lift")
                .toPath(),
            StandardCharsets.UTF_8
        );
        assertTrue(saved.contains("version=\"0.13\""), saved);
        assertTrue(
            !saved.contains("<field name="),
            "field-content declares @type in 0.13:\n" + saved
        );
    }

    // ------------------------------------------------------------------

    private void assertRoundTrip(String resource) throws Exception {
        File source = Utils.resourceFile(resource);
        LiftDictionary first = Utils.loadDictionaryForTest(source);
        List<String> before = DictionarySnapshot.of(first);

        File saved = save(first, "roundtrip.lift");

        LiftDictionary second = Utils.loadDictionaryForTest(saved);
        List<String> after = DictionarySnapshot.of(second);

        assertEquals(
            String.join("\n", before),
            String.join("\n", after),
            "Saving and re-reading " + resource + " changed the dictionary"
        );

        assertNoNewSchemaErrors(source, saved);
    }

    private File save(LiftDictionary d, String name)
        throws WrittingLiftDocumentException {
        File out = new File(tempDir, name);
        d.save(out);
        return out;
    }

    /**
     * Compare the schema errors of the saved file with those of the original.
     *
     * The corpora are real FLEx exports and are not all strictly valid to begin
     * with (20240828Lift.lift is a 0.13 document that already uses the 0.15
     * {@code <field-definition>} element), so the meaningful assertion is that
     * saving introduces no error that the input did not already have.
     *
     * The JDK ships no RELAX NG {@link SchemaFactory}, so validation falls back to
     * the bundled {@code lift-0.13.xsd}; 0.15 documents are covered by the
     * structural comparison plus the explicit vocabulary assertions above.
     */
    private void assertNoNewSchemaErrors(File original, File saved)
        throws Exception {
        Schema schema = liftSchema();
        if (schema == null) return;

        Set<String> originalErrors = validationErrors(schema, original);
        Set<String> savedErrors = validationErrors(schema, saved);

        List<String> introduced = new ArrayList<>(savedErrors);
        introduced.removeAll(originalErrors);
        assertTrue(
            introduced.isEmpty(),
            "Saving introduced schema violations absent from the source file: " +
                introduced
        );
    }

    private static Schema liftSchema() throws Exception {
        try {
            SchemaFactory relaxNg = SchemaFactory.newInstance(
                XMLConstants.RELAXNG_NS_URI
            );
            return relaxNg.newSchema(
                new StreamSource(
                    RoundTripTest.class.getClassLoader()
                        .getResourceAsStream(
                            "fr/cnrs/lacito/liftapi/schema/lift-0.15.rng"
                        )
                )
            );
        } catch (IllegalArgumentException noRelaxNgProvider) {
            // Expected on a stock JDK; fall back to the bundled 0.13 XSD.
        }
        SchemaFactory xsd = SchemaFactory.newInstance(
            XMLConstants.W3C_XML_SCHEMA_NS_URI
        );
        return xsd.newSchema(
            new StreamSource(
                RoundTripTest.class.getClassLoader().getResourceAsStream(XSD_0_13)
            )
        );
    }

    private static Set<String> validationErrors(Schema schema, File f)
        throws IOException {
        Set<String> messages = new LinkedHashSet<>();
        Validator validator = schema.newValidator();
        validator.setErrorHandler(
            new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {}

                @Override
                public void error(SAXParseException e) {
                    messages.add(normalize(e));
                }

                @Override
                public void fatalError(SAXParseException e) {
                    messages.add(normalize(e));
                }
            }
        );
        try {
            validator.validate(new StreamSource(f));
        } catch (org.xml.sax.SAXException e) {
            messages.add(String.valueOf(e.getMessage()));
        }
        return messages;
    }

    /** Drop line/column so the same defect matches across differently formatted files. */
    private static String normalize(SAXParseException e) {
        return String.valueOf(e.getMessage());
    }
}
