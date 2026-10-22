package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.cnrs.lacito.liftapi.change.ModelChange;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

/**
 * The values a range does not declare, and the built-in ranges a file does not declare:
 * flagged, read-only, not written - so that a file loaded and saved is the file read -
 * until they are declared.
 */
class UndeclaredValuesTest {

    @TempDir
    Path dir;

    /** {@code colours} declares {@code red} only, {@code note-type} {@code grammar} only. */
    private static final String HEADER = """
        <ranges>
        <range id="colours"><range-element id="red"><label><form lang="en"><text>Red</text></form></label></range-element></range>
        <range id="note-type"><range-element id="grammar"/></range>
        </ranges>
        <fields><field-definition name="colours" type="option" option-range="colours"/></fields>
        """;

    private static final String ENTRIES = """
        <entry id="e1"><lexical-unit><form lang="xx"><text>kato</text></form></lexical-unit>
        <trait name="colours" value="blue"/>
        <note type="reference"><form lang="en"><text>p. 3</text></form></note>
        <sense id="s1"><grammatical-info value="Noun"/></sense>
        </entry>
        <entry id="e2"><lexical-unit><form lang="xx"><text>mato</text></form></lexical-unit>
        <trait name="colours" value="blue"/>
        <sense id="s2"><grammatical-info value="Verb"/></sense>
        </entry>
        """;

    // ------------------------------------------------------------------
    // Flags
    // ------------------------------------------------------------------

    @Test
    void whatTheFileDoesNotDeclareIsFlagged() throws Exception {
        LiftDictionary d = load(lift(HEADER, ENTRIES));
        LiftHeader header = d.getHeader();
        FeatureSet colours = header.getFeatureSet("colours");

        assertTrue(colours.isDeclared());
        assertTrue(colours.getFeature("red").isDeclared());
        assertFalse(colours.getFeature("blue").isDeclared());
        assertTrue(header.getNoteTypeManager().isDeclared());
        assertFalse(header.getNoteTypeManager().getFeature("reference").isDeclared());
        // Built in, but not in the file.
        FeatureSet pos = header.getGrammaticalInfoManager();
        assertFalse(pos.isDeclared());
        assertFalse(pos.getFeature("Noun").isDeclared());
        assertFalse(header.getAnnotationTypeManager().isDeclared());
    }

    @Test
    void whatIsCreatedExplicitlyIsDeclared() {
        LiftDictionary d = LiftDictionary.makeBuilder().withNoteType("comment").build();
        LiftHeader header = d.getHeader();
        assertTrue(header.getNoteTypeManager().isDeclared());
        assertTrue(header.getNoteTypeManager().getFeature("comment").isDeclared());
        // A new dictionary: its built-in sets are undeclared, as for a file (Q1).
        assertFalse(header.getRelationTypeManager().isDeclared());

        FeatureSet colours = header.addFeatureSet("colours");
        assertTrue(colours.isDeclared());
        assertTrue(colours.addFeature("red").isDeclared());
        assertTrue(d.getComponentBuilder().feature(colours, "green").build().isDeclared());
        // A builder that needs a type creates a bare one.
        d.getComponentBuilder().entry().withForm("xx", "a").build();
        Feature synonym = d.getComponentBuilder().relation("synonym",
            d.getLiftDictionaryRegistry().getEntries().getFirst()).build().getType();
        assertFalse(synonym.isDeclared());
    }

    // ------------------------------------------------------------------
    // Read-only
    // ------------------------------------------------------------------

    @Test
    void anUndeclaredValueAndAnUndeclaredRangeAreReadOnly() throws Exception {
        LiftDictionary d = load(lift(HEADER, ENTRIES));
        LiftHeader header = d.getHeader();
        FeatureSet colours = header.getFeatureSet("colours");
        Feature blue = colours.getFeature("blue");
        FeatureSet pos = header.getGrammaticalInfoManager();

        List<Executable> refused = List.of(
            () -> blue.getLabel().add(new Form("en", "Blue")),
            () -> blue.setGuid("g"),
            () -> colours.changeFeatureId(blue, "navy"),
            () -> blue.setSuperordinateFeature(colours.getFeature("red")),
            () -> blue.addTrait(new LiftTrait(header.resolveTraitSpecification("x"), "y")),
            () -> pos.addFeature("Adjective"),
            () -> pos.getLabel().add(new Form("en", "Parts of speech")),
            () -> pos.getFeature("Noun").setGuid("g")
        );
        for (Executable edit : refused) {
            assertThrows(IllegalStateException.class, edit);
        }
        // The declared values of the range stay editable; one may name an undeclared parent.
        colours.getFeature("red").getLabel().getForm("en").orElseThrow().changeText("Scarlet");
        colours.addFeature("green").setSuperordinateFeature(blue);
        // An unused undeclared value may be removed.
        d.getLiftDictionaryRegistry().getTraits().stream()
            .filter(t -> t.getName().equals("colours")).toList()
            .forEach(t -> ((fr.cnrs.lacito.liftapi.model.LiftEntry) t.getParent()).deleteTrait(t));
        assertThrows(IllegalStateException.class, () -> colours.removeFeature("blue")); // the parent of green
        colours.getFeature("green").setSuperordinateFeature(null);
        colours.removeFeature("blue");
        assertFalse(colours.hasFeature("blue"));
    }

    // ------------------------------------------------------------------
    // Declaring
    // ------------------------------------------------------------------

    @Test
    void aDeclaredValueOrRangeIsWritableAndWritten() throws Exception {
        LiftDictionary d = load(lift(HEADER, ENTRIES));
        LiftHeader header = d.getHeader();
        Feature blue = header.getFeatureSet("colours").getFeature("blue");
        FeatureSet pos = header.getGrammaticalInfoManager();
        Feature noun = pos.getFeature("Noun");

        header.declareFeature(blue);
        blue.setGuid("g-blue");
        // Declaring a value of an undeclared range declares the range too.
        header.declareFeature(noun);
        assertTrue(pos.isDeclared());
        assertFalse(pos.getFeature("Verb").isDeclared());
        header.declareFeatureSet(header.getEtymologyTypeManager());

        assertThrows(IllegalArgumentException.class, () -> header.declareFeature(blue));
        assertThrows(IllegalArgumentException.class, () -> header.declareFeatureSet(pos));

        String saved = save(d);
        assertTrue(saved.contains("<range-element id=\"blue\" guid=\"g-blue\">"), saved);
        assertTrue(saved.contains("<range id=\"grammatical-info\"><range-element id=\"Noun\">"), saved);
        assertFalse(saved.contains("\"Verb\"></range-element>"), saved);
        assertTrue(saved.contains("<range id=\"etymology\"></range>"), saved);
        LiftDictionary reloaded = load(dir.resolve("saved.lift"));
        assertTrue(reloaded.getHeader().getFeatureSet("colours").getFeature("blue").isDeclared());
        assertTrue(reloaded.getHeader().getGrammaticalInfoManager().isDeclared());
        assertFalse(reloaded.getHeader().getGrammaticalInfoManager().getFeature("Verb").isDeclared());
    }

    @Test
    void declaringIsReportedAndCanBeUndone() throws Exception {
        LiftDictionary d = load(lift(HEADER, ENTRIES));
        LiftHeader header = d.getHeader();
        Feature noun = header.getGrammaticalInfoManager().getFeature("Noun");
        List<ModelChange> changes = new ArrayList<>();
        d.addChangeListener(changes::add);

        header.declareFeature(noun);
        noun.setGuid("g");
        List<ModelChange> recorded = List.copyOf(changes);
        for (int i = recorded.size() - 1; i >= 0; i--) {
            recorded.get(i).revert();
        }
        assertFalse(noun.isDeclared());
        assertFalse(header.getGrammaticalInfoManager().isDeclared());
        assertTrue(noun.getGuid().isEmpty());
        for (ModelChange change : recorded) {
            change.reapply();
        }
        assertTrue(noun.isDeclared());
        assertEquals("g", noun.getGuid().orElseThrow());
    }

    @Test
    void aValueOfAnExternalRangeIsDeclaredOnceTheRangeIsInternal() throws Exception {
        Files.writeString(dir.resolve("shared.lift-ranges"), """
            <lift-ranges><range id="note-type"><range-element id="grammar"/></range></lift-ranges>
            """);
        LiftDictionary d = load(lift("<ranges><range id=\"note-type\" href=\"shared.lift-ranges\"/></ranges>", ENTRIES));
        LiftHeader header = d.getHeader();
        Feature reference = header.getNoteTypeManager().getFeature("reference");
        assertThrows(IllegalStateException.class, () -> header.declareFeature(reference));
        header.makeFeatureSetInternal(header.getNoteTypeManager());
        header.declareFeature(reference);
        assertTrue(reference.isDeclared());
    }

    // ------------------------------------------------------------------
    // Warnings, and option-range
    // ------------------------------------------------------------------

    @Test
    void theReaderWarnsOnceAboutEachUndeclaredValueAndRange() throws Exception {
        LiftDictionary d = load(lift(HEADER, ENTRIES));
        List<String> warnings = d.getLoadWarnings();

        assertEquals(1, warnings.stream().filter(w -> w.contains("\"blue\"")).count(), warnings.toString());
        assertEquals(1, warnings.stream().filter(w -> w.contains("\"reference\"")).count(), warnings.toString());
        // One for the range, not one per value.
        assertEquals(1, warnings.stream().filter(w -> w.contains("grammatical-info")).count(), warnings.toString());
        assertFalse(warnings.stream().anyMatch(w -> w.contains("Noun") || w.contains("Verb")), warnings.toString());
        assertTrue(warnings.stream().allMatch(w -> w.contains("(line ")), warnings.toString());
        assertTrue(LiftDictionary.makeBuilder().build().getLoadWarnings().isEmpty());
    }

    @Test
    void anOptionRangeNamingARangeTheFileDoesNotDeclareIsAnError() throws Exception {
        LiftDocumentLoadingException e = assertThrows(LiftDocumentLoadingException.class, () -> load(lift("""
            <fields>
            <field-definition name="colour" type="option" option-range="colours"/>
            </fields>
            """, "")));
        assertTrue(e.getMessage().contains("colour") && e.getMessage().contains("colours")
            && e.getMessage().contains("line 5"), e.getMessage());

        // A built-in range always exists: it stays undeclared, with a warning.
        LiftDictionary d = load(lift("""
            <fields><field-definition name="kind" type="option" option-range="note-type"/></fields>
            """, ""));
        assertSame(d.getHeader().getNoteTypeManager(),
            d.getHeader().getFieldsAndTraitsDefinitions("kind").getResolvedFeatureSet().orElseThrow());
        assertFalse(d.getHeader().getNoteTypeManager().isDeclared());
        assertTrue(d.getLoadWarnings().stream().anyMatch(w -> w.contains("note-type")), d.getLoadWarnings().toString());
        assertTrue(save(d).contains("option-range=\"note-type\""));
    }

    // ------------------------------------------------------------------

    private static String lift(String header, String entries) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <lift version="0.15" producer="test">
            <header>
            """ + header + """
            </header>
            """ + entries + """
            </lift>
            """;
    }

    private LiftDictionary load(String xml) throws Exception {
        Path file = dir.resolve("test.lift");
        Files.writeString(file, xml);
        return load(file);
    }

    private static LiftDictionary load(Path file) throws Exception {
        return LiftDictionary.loadDictionaryFromFile(file.toFile());
    }

    private String save(LiftDictionary d) throws Exception {
        File file = dir.resolve("saved.lift").toFile();
        d.save(file);
        return Files.readString(file.toPath());
    }
}
