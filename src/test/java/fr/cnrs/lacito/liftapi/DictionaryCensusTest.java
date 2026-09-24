package fr.cnrs.lacito.liftapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import fr.cnrs.lacito.liftapi.internal.DictionaryMutator;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.LiftEntry;

import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Every component in a LIFT file must end up in the dictionary's registry.
 *
 * Registering a component means reaching it: the loader walks a subtree through
 * {@code DictionaryMutator.childrenOf}, and a kind of component missing from that walk
 * is silently dropped - it exists in the object graph, hanging off its parent, but the
 * dictionary does not know about it, so it is invisible to the registry accessors and
 * is left behind when its parent is deleted.
 *
 * That is exactly what had happened to {@code <media>} (0 of 22 registered) and to the
 * traits carried by {@code <grammatical-info>} (675 of 5130 traits missing). No test
 * caught it, because every other test asserted on components reached through their
 * parent rather than through the registry.
 *
 * This test closes that hole for good: it counts the elements in the source file and
 * compares them with the registry, so omitting a component type from the traversal
 * fails here rather than silently losing a user's data.
 *
 * <h2>One known gap, deliberately not asserted</h2>
 *
 * An {@code <annotation>} inside a {@code <form>} or {@code <gloss>} is held by the
 * {@link fr.cnrs.lacito.liftapi.model.Form}, and a {@code Form} is not an
 * {@code AbstractLiftRoot}, so no traversal over components can reach it. Such
 * annotations are therefore never registered, and the MultiText each of them carries is
 * never counted towards the dictionary's languages. Closing that needs a model change,
 * so this test counts only annotations outside a form and the gap is recorded here
 * rather than hidden.
 */
public class DictionaryCensusTest {

    @Test
    public void largeCorpusIsFullyRegistered() {
        assertCensus("lift/20240828Lift.lift");
    }

    @Test
    public void lift015CorpusIsFullyRegistered() {
        assertCensus("lift/tiny015.xml");
    }

    @Test
    public void tinyCorpusIsFullyRegistered() {
        assertCensus("lift/tiny.xml");
    }

    // ------------------------------------------------------------------

    private void assertCensus(String resource) {
        File file = Utils.resourceFile(resource);
        ElementCount inFile = countElements(file);
        LiftDictionaryRegistry registry = Utils.loadDictionaryForTest(file)
            .getLiftDictionaryRegistry();

        assertEveryComponentIsRegistered(registry, resource);

        List<String> mismatches = new ArrayList<>();

        // A <sense> and a <subsense> both produce a LiftSense; a <reversal> and the
        // <main> nested in it both produce a LiftReversal.
        check(mismatches, "entry", inFile.of("entry"), () -> registry.getEntries().size());
        check(
            mismatches,
            "sense + subsense",
            inFile.of("sense") + inFile.of("subsense"),
            () -> registry.getSenses().size()
        );
        check(mismatches, "example", inFile.of("example"), () -> registry.getExamples().size());
        check(mismatches, "variant", inFile.of("variant"), () -> registry.getVariants().size());
        check(mismatches, "trait", inFile.of("trait"), () -> registry.getTraits().size());
        check(
            mismatches,
            "reversal + main",
            inFile.of("reversal") + inFile.of("main"),
            () -> registry.getReversals().size()
        );
        check(mismatches, "relation", inFile.of("relation"), () -> registry.getRelations().size());
        check(
            mismatches,
            "pronunciation",
            inFile.of("pronunciation"),
            () -> registry.getPronunciations().size()
        );
        check(mismatches, "note", inFile.of("note"), () -> registry.getNotes().size());
        check(mismatches, "media", inFile.of("media"), () -> registry.getMedias().size());
        check(
            mismatches,
            "illustration",
            inFile.of("illustration"),
            () -> registry.getIllustrations().size()
        );
        check(mismatches, "field", inFile.of("field"), () -> registry.getFields().size());
        check(
            mismatches,
            "etymology",
            inFile.of("etymology"),
            () -> registry.getEtymologies().size()
        );
        // Annotations attached to a <form> or <gloss> are held by the Form, which is
        // not an AbstractLiftRoot, so the traversal cannot reach them and they are not
        // registered. See the note on the class. Only component-level annotations are
        // asserted here.
        check(
            mismatches,
            "annotation (outside a form)",
            inFile.of("annotation"),
            () -> registry.getAnnotations().size()
        );
        check(
            mismatches,
            "grammatical-info",
            inFile.of("grammatical-info"),
            () -> registry.getGrammaticalInfos().size()
        );

        assertEquals(
            List.of(),
            mismatches,
            "Components present in " +
                resource +
                " but missing from (or duplicated in) the registry"
        );
    }

    /**
     * Every component hanging off an entry must carry a UUID.
     *
     * This is the general form of the whole class of bug the counts above catch case by
     * case: a component sitting in the object graph that the dictionary knows nothing
     * about. Counting per element type only catches an omission in the traversal;
     * walking the graph catches any escape route, whatever the mechanism - a component
     * wired in through a public {@code addX()}, a child kind nobody thought to
     * enumerate, a registration silently skipped.
     */
    private static void assertEveryComponentIsRegistered(
        LiftDictionaryRegistry registry,
        String resource
    ) {
        List<String> unregistered = new ArrayList<>();
        for (LiftEntry entry : registry.getEntries()) {
            collectUnregistered(entry, entry, unregistered);
        }
        assertEquals(
            List.of(),
            unregistered,
            "Components reachable from an entry of " +
                resource +
                " that were never registered"
        );
    }

    private static void collectUnregistered(
        AbstractLiftRoot node,
        LiftEntry entry,
        List<String> unregistered
    ) {
        if (node.getUUID() == null && unregistered.size() < 20) {
            unregistered.add(
                node.getClass().getSimpleName() +
                    " under entry " +
                    entry.getId().orElse("<no id>")
            );
        }
        for (AbstractLiftRoot child : DictionaryMutator.childrenOf(node)) {
            collectUnregistered(child, entry, unregistered);
        }
    }

    private static void check(
        List<String> mismatches,
        String label,
        int expected,
        IntSupplier registered
    ) {
        int actual = registered.getAsInt();
        if (actual != expected) {
            mismatches.add(
                label + ": file has " + expected + ", registry has " + actual
            );
        }
    }

    // ------------------------------------------------------------------

    /** Counts of start elements by local name, excluding the {@code <header>}. */
    private record ElementCount(Map<String, Integer> counts) {
        int of(String localName) {
            return counts.getOrDefault(localName, 0);
        }
    }

    /**
     * Count the LIFT elements in a file.
     *
     * The {@code <header>} is skipped: the ranges and field definitions it declares are
     * a different kind of object ({@code FeatureSet}, {@code Feature},
     * {@code LiftFieldAndTraitDefinition}) and are not held in the per-component
     * registries this test is about. In particular a 0.13 header spells its field
     * declarations {@code <field tag="...">}, which would otherwise be counted as
     * entry-level fields.
     */
    private static ElementCount countElements(File file) {
        Map<String, Integer> counts = new HashMap<>();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();
            parser.parse(
                file,
                new DefaultHandler() {
                    private int headerDepth = 0;
                    private int formDepth = 0;

                    @Override
                    public void startElement(
                        String uri,
                        String localName,
                        String qName,
                        Attributes attributes
                    ) {
                        if ("header".equals(localName) || headerDepth > 0) {
                            headerDepth++;
                            return;
                        }
                        // Annotations inside a form are held by the Form, not by a
                        // component, and are not registered; do not count them.
                        if (formDepth > 0 && "annotation".equals(localName)) {
                            return;
                        }
                        if ("form".equals(localName) || "gloss".equals(localName)) {
                            formDepth++;
                        }
                        counts.merge(localName, 1, Integer::sum);
                    }

                    @Override
                    public void endElement(
                        String uri,
                        String localName,
                        String qName
                    ) {
                        if (headerDepth > 0) {
                            headerDepth--;
                            return;
                        }
                        if ("form".equals(localName) || "gloss".equals(localName)) {
                            formDepth--;
                        }
                    }
                }
            );
        } catch (Exception e) {
            throw new AssertionError("Cannot count elements of " + file, e);
        }
        return new ElementCount(counts);
    }
}
