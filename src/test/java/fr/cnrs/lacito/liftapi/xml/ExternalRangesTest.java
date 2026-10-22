package fr.cnrs.lacito.liftapi.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDocumentLoadingException;
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
 * The ranges a {@code .lift} file defines in an external {@code .lift-ranges} file
 * ({@code <range id="..." href="..."/>}): read once, read-only, written back as the
 * reference (with what the {@code .lift} file declares inline), made internal on demand.
 */
class ExternalRangesTest {

    private static final File CORPUS = new File("src/test/resources/lift/DictionaryLift20261022.lift");

    @TempDir
    Path dir;

    private static final String DIALECTS = """
        <?xml version="1.0" encoding="UTF-8"?>
        <lift-ranges>
        <range id="dialects" guid="g-dialects">
        <label><form lang="en"><text>Dialects</text></form></label>
        <range-element id="north" guid="g-north">
        <label><form lang="en"><text>North</text></form></label>
        <trait name="catalog-source-id" value="N"/>
        </range-element>
        <range-element id="far-north" parent="north">
        <label><form lang="en"><text>Far north</text></form></label>
        </range-element>
        <range-element id="south">
        <label><form lang="en"><text>South</text></form></label>
        </range-element>
        </range>
        <range id="registers"><range-element id="formal"/></range>
        </lift-ranges>
        """;

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    @Test
    void theCorpusRangesAreFilledFromTheRangesFile() throws Exception {
        LiftDictionary d = LiftDictionary.loadDictionaryFromFile(CORPUS);
        FeatureSet pos = d.getHeader().getGrammaticalInfoManager();

        assertTrue(pos.isExternallyDefined());
        assertEquals("DictionaryLift20261022.lift-ranges", pos.getHref().orElseThrow());
        assertEquals(
            CORPUS.toPath().resolveSibling("DictionaryLift20261022.lift-ranges").toRealPath(),
            pos.getExternalDefinitionFile().orElseThrow());
        Feature adverb = pos.getFeature("Adverbe");
        assertEquals("Adverb", adverb.getLabel().getForm("en").orElseThrow().toPlainText());
        assertEquals("adv", adverb.getAbbrev().getForm("fr").orElseThrow().toPlainText());
        assertEquals("46e4fe08-ffa0-4c8b-bf98-2c56f38904d9", adverb.getGuid().orElseThrow());
        assertEquals("catalog-source-id", adverb.getTraits().getFirst().getName());
        assertSame(pos.getFeature("Nom"), pos.getFeature("Nom propre").getSuperOrdinateFeature().orElseThrow());
        assertFalse(adverb.isDeclaredInline());
        // Referenced, but left out of the ranges file by FLEx: an empty external range.
        FeatureSet dialect = d.getHeader().getFeatureSet("dialect");
        assertTrue(dialect.isExternallyDefined());
        assertTrue(dialect.getFeatures().isEmpty());
        assertTrue(dialect.isExternalDefinitionMissing());
        assertFalse(pos.isExternalDefinitionMissing());
    }

    @Test
    void aRangesFileIsParsedOnceWhateverTheNumberOfRangesReferringToIt() throws Exception {
        int before = LiftRangesFileReader.FILES_PARSED.get();
        LiftDictionary.loadDictionaryFromFile(CORPUS);
        assertEquals(1, LiftRangesFileReader.FILES_PARSED.get() - before);
    }

    @Test
    void anHrefIsAPathOrAFileUriAndFallsBackOnTheFileOfTheSameNameNextToTheLiftFile() throws Exception {
        Path ranges = write("dialects.lift-ranges", DIALECTS);
        Files.createDirectories(dir.resolve("sub"));
        Path inSub = write("sub/other.lift-ranges", DIALECTS);
        Path base = dir.toRealPath();

        assertEquals(ranges.toRealPath(), ExternalRangesFiles.resolve("dialects.lift-ranges", base).orElseThrow());
        assertEquals(ranges.toRealPath(), ExternalRangesFiles.resolve("./dialects.lift-ranges", base).orElseThrow());
        assertEquals(inSub.toRealPath(), ExternalRangesFiles.resolve("sub/other.lift-ranges", base).orElseThrow());
        assertEquals(ranges.toRealPath(),
            ExternalRangesFiles.resolve(ranges.toAbsolutePath().toUri().toString(), base).orElseThrow());
        assertEquals(ranges.toRealPath(), ExternalRangesFiles.resolve(ranges.toAbsolutePath().toString(), base).orElseThrow());
        // file://name: the name is parsed as the host.
        assertEquals(ranges.toRealPath(), ExternalRangesFiles.resolve("file://dialects.lift-ranges", base).orElseThrow());
        // The path of the machine that exported the file.
        assertEquals(ranges.toRealPath(),
            ExternalRangesFiles.resolve("file:///C:/Users/someone/project/dialects.lift-ranges", base).orElseThrow());
        assertEquals(ranges.toRealPath(),
            ExternalRangesFiles.resolve("C:\\Users\\someone\\dialects.lift-ranges", base).orElseThrow());
        assertEquals(ranges.toRealPath(),
            ExternalRangesFiles.resolve("/home/someone/dialects.lift-ranges", base).orElseThrow());
        assertTrue(ExternalRangesFiles.resolve("missing.lift-ranges", base).isEmpty());
        assertThrows(IllegalArgumentException.class,
            () -> ExternalRangesFiles.resolve("http://example.org/dialects.lift-ranges", base));
    }

    @Test
    void aRangeWithAnHttpReferenceIsNotRead() throws Exception {
        assertThrows(LiftDocumentLoadingException.class, () -> load(lift(
            "<range id=\"dialects\" href=\"http://example.org/dialects.lift-ranges\"/>", "")));
    }

    @Test
    void aMissingRangeOrFileGivesAnEmptyExternalRangeOrAnErrorInStrictMode() throws Exception {
        write("dialects.lift-ranges", DIALECTS);
        LiftDictionary d = load(lift("""
            <range id="colours" href="dialects.lift-ranges"/>
            <range id="moods" href="missing.lift-ranges"/>
            """, ""));
        FeatureSet colours = d.getHeader().getFeatureSet("colours");
        assertTrue(colours.isExternallyDefined());
        assertTrue(colours.getFeatures().isEmpty());
        assertTrue(colours.getExternalDefinitionFile().isPresent());
        assertTrue(colours.isExternalDefinitionMissing());
        assertTrue(d.getHeader().getFeatureSet("moods").getExternalDefinitionFile().isEmpty());

        File file = dir.resolve("test.lift").toFile();
        assertThrows(LiftDocumentLoadingException.class,
            () -> new LiftDictionaryXmlReader(file, LiftDictionary.makeBuilder().build(), true).parse());
    }

    @Test
    void anInvalidRangesFileIsAnError() throws Exception {
        write("twice.lift-ranges", """
            <lift-ranges><range id="a"/><range id="a"/></lift-ranges>
            """);
        write("href.lift-ranges", """
            <lift-ranges><range id="a" href="other.lift-ranges"/></lift-ranges>
            """);
        write("root.lift-ranges", """
            <lift><range id="a"/></lift>
            """);
        for (String file : List.of("twice", "href", "root")) {
            assertThrows(LiftDocumentLoadingException.class,
                () -> load(lift("<range id=\"a\" href=\"" + file + ".lift-ranges\"/>", "")), file);
        }
    }

    @Test
    void anErrorInTheRangesFileNamesItAndItsLine() throws Exception {
        write("dialects.lift-ranges", """
            <lift-ranges>
            <range id="dialects">
            <range-element/>
            </range>
            </lift-ranges>
            """);
        LiftDocumentLoadingException e = assertThrows(LiftDocumentLoadingException.class,
            () -> load(lift("<range id=\"dialects\" href=\"dialects.lift-ranges\"/>", "")));
        String message = e.getMessage();
        assertTrue(message.contains("dialects.lift-ranges") && message.contains("line 3"), message);
    }

    @Test
    void aRangeDeclaredTwiceWithAnHrefIsAnError() throws Exception {
        write("dialects.lift-ranges", DIALECTS);
        assertThrows(LiftDocumentLoadingException.class, () -> load(lift("""
            <range id="dialects"><range-element id="east"/></range>
            <range id="dialects" href="dialects.lift-ranges"/>
            """, "")));
    }

    @Test
    void aRangeElementDeclaredTwiceKeepsItsFirstDeclaration() throws Exception {
        write("dialects.lift-ranges", """
            <lift-ranges><range id="dialects">
            <range-element id="non"><label><form lang="en"><text>first</text></form></label></range-element>
            <range-element id="non"><label><form lang="en"><text>second</text></form></label></range-element>
            </range></lift-ranges>
            """);
        LiftDictionary d = load(lift("<range id=\"dialects\" href=\"dialects.lift-ranges\"/>", ""));
        assertEquals("first", d.getHeader().getFeatureSet("dialects").getFeature("non")
            .getLabel().getForm("en").orElseThrow().toPlainText());
    }

    // ------------------------------------------------------------------
    // Values the external range does not declare
    // ------------------------------------------------------------------

    @Test
    void aValueTheExternalRangeDoesNotDeclareIsABareFeatureThatIsNotWritten() throws Exception {
        write("types.lift-ranges", """
            <lift-ranges><range id="note-type"><range-element id="grammar"/></range></lift-ranges>
            """);
        LiftDictionary d = load(lift("<range id=\"note-type\" href=\"types.lift-ranges\"/>", """
            <entry id="e1"><note type="reference"><form lang="en"><text>p. 3</text></form></note></entry>
            """));
        FeatureSet types = d.getHeader().getNoteTypeManager();
        assertFalse(types.getFeature("reference").isDeclared());
        assertTrue(types.getFeature("grammar").isDeclared());
        // Through the public API, a new feature is refused; through a builder, it is a type.
        assertThrows(IllegalStateException.class, () -> types.getOrCreateFeature("other"));
        d.getComponentBuilder().note(d.getLiftDictionaryRegistry().getEntries().getFirst(), "comment").build();
        assertTrue(types.hasFeature("comment"));

        String saved = save(d, "saved.lift");
        assertFalse(saved.contains("<range-element"), saved);
        assertTrue(load(dir.resolve("saved.lift")).getHeader().getNoteTypeManager().hasFeature("reference"));
    }

    // ------------------------------------------------------------------
    // Read-only, and made internal
    // ------------------------------------------------------------------

    @Test
    void anExternalRangeIsReadOnlyUntilItIsMadeInternal() throws Exception {
        write("dialects.lift-ranges", DIALECTS);
        LiftDictionary d = load(lift("<range id=\"dialects\" href=\"dialects.lift-ranges\"/>", ""));
        LiftHeader header = d.getHeader();
        FeatureSet dialects = header.getFeatureSet("dialects");
        Feature north = dialects.getFeature("north");
        Feature south = dialects.getFeature("south");
        LiftTrait trait = north.getTraits().getFirst();
        d.getMetaLanguageManager().addLanguage("fr");

        List<Executable> edits = List.of(
            () -> dialects.addFeature("east"),
            () -> dialects.removeFeature("far-north"),
            () -> dialects.changeFeatureId(south, "deep-south"),
            () -> dialects.getOrCreateFeature("west"),
            () -> dialects.setGuid("other"),
            () -> dialects.setDateModified("2026-10-22"),
            () -> dialects.getLabel().add(new Form("fr", "Dialectes")),
            () -> dialects.getLabel().removeForm("en"),
            () -> header.renameFeatureSet(dialects, "regions"),
            () -> north.setGuid("other"),
            () -> north.guidProperty().set("other"),
            () -> south.setSuperordinateFeature(north),
            () -> north.getLabel().getForm("en").orElseThrow().changeText("Northern"),
            () -> north.getDescription().add(new Form("en", "up there")),
            () -> trait.setValue("M"),
            () -> north.deleteTrait(trait),
            () -> south.addTrait(new LiftTrait(header.resolveTraitSpecification("catalog-source-id")))
        );
        for (Executable edit : edits) {
            assertThrows(IllegalStateException.class, edit);
        }
        assertEquals(3, dialects.getFeatures().size());
        assertEquals("North", north.getLabel().getForm("en").orElseThrow().toPlainText());

        header.makeFeatureSetInternal(dialects);
        assertFalse(dialects.isExternallyDefined());
        for (Executable edit : edits) {
            try {
                edit.execute();
            } catch (Throwable e) {
                throw new AssertionError("refused once internal", e);
            }
        }
        assertEquals("regions", dialects.getId());
        assertThrows(IllegalArgumentException.class, () -> header.makeFeatureSetInternal(dialects));
    }

    @Test
    void makingARangeInternalIsReportedAndCanBeUndone() throws Exception {
        write("dialects.lift-ranges", DIALECTS);
        LiftDictionary d = load(lift("<range id=\"dialects\" href=\"dialects.lift-ranges\"/>", ""));
        FeatureSet dialects = d.getHeader().getFeatureSet("dialects");
        List<ModelChange> changes = new ArrayList<>();
        d.addChangeListener(changes::add);

        d.getHeader().makeFeatureSetInternal(dialects);
        dialects.addFeature("east");
        List<ModelChange> recorded = List.copyOf(changes);
        for (int i = recorded.size() - 1; i >= 0; i--) {
            recorded.get(i).revert();
        }
        assertTrue(dialects.isExternallyDefined());
        assertFalse(dialects.hasFeature("east"));
        for (ModelChange change : recorded) {
            change.reapply();
        }
        assertFalse(dialects.isExternallyDefined());
        assertTrue(dialects.hasFeature("east"));
    }

    // ------------------------------------------------------------------
    // Writing
    // ------------------------------------------------------------------

    @Test
    void anExternalRangeIsWrittenAsTheReferenceAndTheRangesFileIsNeverWritten() throws Exception {
        Path ranges = write("dialects.lift-ranges", DIALECTS);
        LiftDictionary d = load(lift("<range id=\"dialects\" href=\"dialects.lift-ranges\"/>", ""));

        String saved = save(d, "test.lift");
        assertTrue(saved.contains("<range id=\"dialects\" href=\"dialects.lift-ranges\"></range>"), saved);
        assertFalse(saved.contains("north"), saved);
        assertEquals(DIALECTS, Files.readString(ranges));

        d.getHeader().makeFeatureSetInternal(d.getHeader().getFeatureSet("dialects"));
        saved = save(d, "test.lift");
        assertFalse(saved.contains("href"), saved);
        assertTrue(saved.contains("<range-element id=\"far-north\" parent=\"north\">"), saved);
        assertEquals(DIALECTS, Files.readString(ranges));
        assertFalse(load(dir.resolve("test.lift")).getHeader().getFeatureSet("dialects").isExternallyDefined());
    }

    @Test
    void savedInAnotherDirectoryTheReferenceStillNamesTheSameFile() throws Exception {
        Path ranges = write("dialects.lift-ranges", DIALECTS);
        LiftDictionary d = load(lift("<range id=\"dialects\" href=\"dialects.lift-ranges\"/>", ""));
        Files.createDirectories(dir.resolve("elsewhere/deeper"));

        String saved = save(d, "elsewhere/deeper/copy.lift");
        assertTrue(saved.contains("href=\"../../dialects.lift-ranges\""), saved);
        LiftDictionary reloaded = load(dir.resolve("elsewhere/deeper/copy.lift"));
        FeatureSet dialects = reloaded.getHeader().getFeatureSet("dialects");
        assertEquals(ranges.toRealPath(), dialects.getExternalDefinitionFile().orElseThrow());
        assertTrue(dialects.hasFeature("north"));
        // Saved back next to the ranges file, the reference is the one read.
        assertTrue(save(reloaded, "back.lift").contains("href=\"dialects.lift-ranges\""));
    }

    // ------------------------------------------------------------------
    // Inline overrides
    // ------------------------------------------------------------------

    @Test
    void theChildrenOfTheRangeInTheLiftFileOverrideTheExternalFile() throws Exception {
        write("dialects.lift-ranges", DIALECTS);
        LiftDictionary d = load(lift("""
            <range id="dialects" href="dialects.lift-ranges">
            <label><form lang="fr"><text>Dialectes</text></form></label>
            <trait name="source" value="survey"/>
            <range-element id="north">
            <label><form lang="en"><text>Northern</text></form></label>
            </range-element>
            <range-element id="east" parent="far-north"/>
            </range>
            """, ""));
        FeatureSet dialects = d.getHeader().getFeatureSet("dialects");

        // The range's label replaces the file's; its trait is added.
        assertEquals(List.of("fr"), List.copyOf(dialects.getLabel().getLangs()));
        assertTrue(dialects.isDeclaredInline(dialects.getLabel()));
        assertFalse(dialects.isDeclaredInline(dialects.getDescription()));
        assertTrue(dialects.isDeclaredInline(dialects.getTraits().getFirst()));
        // An inline element replaces the file's as a whole: no guid, no trait any more.
        Feature north = dialects.getFeature("north");
        assertTrue(north.isDeclaredInline());
        assertEquals("Northern", north.getLabel().getForm("en").orElseThrow().toPlainText());
        assertTrue(north.getGuid().isEmpty());
        assertTrue(north.getTraits().isEmpty());
        // The file's elements follow the override; an inline element names a file's one.
        assertSame(north, dialects.getFeature("far-north").getSuperOrdinateFeature().orElseThrow());
        assertSame(dialects.getFeature("far-north"), dialects.getFeature("east").getSuperOrdinateFeature().orElseThrow());
        assertTrue(dialects.getFeature("east").isDeclaredInline());
        assertFalse(dialects.getFeature("south").isDeclaredInline());
        // Read-only too.
        assertThrows(IllegalStateException.class, () -> north.setGuid("g"));

        String saved = save(d, "saved.lift");
        assertTrue(saved.contains("Dialectes") && saved.contains("Northern") && saved.contains("survey"), saved);
        assertTrue(saved.contains("<range-element id=\"east\" parent=\"far-north\">"), saved);
        assertFalse(saved.contains("South") || saved.contains("Far north") || saved.contains(">Dialects<"), saved);

        assertEquals(snapshot(d), snapshot(load(dir.resolve("saved.lift"))));
    }

    // ------------------------------------------------------------------

    private Path write(String name, String content) throws Exception {
        Path file = dir.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    private static String lift(String ranges, String entries) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <lift version="0.15" producer="test">
            <header><ranges>
            """ + ranges + """
            </ranges></header>
            """ + entries + """
            </lift>
            """;
    }

    private LiftDictionary load(String xml) throws Exception {
        return load(write("test.lift", xml));
    }

    private static LiftDictionary load(Path file) throws Exception {
        return LiftDictionary.loadDictionaryFromFile(file.toFile());
    }

    private String save(LiftDictionary d, String name) throws Exception {
        File file = dir.resolve(name).toFile();
        d.save(file);
        return Files.readString(file.toPath());
    }

    private static List<String> snapshot(LiftDictionary d) {
        return DictionarySnapshot.of(d);
    }
}
