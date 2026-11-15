package fr.cnrs.lacito.liftapi.xml;

import fr.cnrs.lacito.liftapi.ExtractOptions;
import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LoadOptions;
import fr.cnrs.lacito.liftapi.internal.DictionaryMutator;
import fr.cnrs.lacito.liftapi.model.AbstractIdentifiable;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.GrammaticalInfo;
import fr.cnrs.lacito.liftapi.model.HasRefId;
import fr.cnrs.lacito.liftapi.model.HasType;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.RangeReference;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.SAXParser;

/**
 * The work of {@link LiftDictionary#extract(Collection, ExtractOptions)}: the entries, the
 * part of the header they use and the stubs of what they refer to are written by
 * {@link LiftWriterSession} into memory, and read back by {@link LiftSaxHandler} into the
 * new dictionary. Going through the writer and the reader is what makes the copy exactly
 * what a save and a reload give, and keeps it complete as the model grows; only the header
 * values the entries use are written, so the cost follows the size of the entries, not of
 * the header (a field dictionary's ranges can hold thousands of values).
 *
 * <p>Not API: reached through {@code LiftDictionary.extract} only (this package is not
 * exported).</p>
 */
public final class LiftExtractor {

    private LiftExtractor() {
    }

    /**
     * Fill {@code target}, a new empty dictionary, with an extract of {@code source}.
     *
     * @param baseDirectory the directory the {@code href}s of external ranges are relative to
     * @param sourceFile    the file {@code source} was read from, or {@code null}
     * @return the LIFT ids of the stub entries added
     */
    public static Set<String> extractInto(
        LiftDictionary source, Collection<LiftEntry> entries, ExtractOptions options,
        LiftDictionary target, Path baseDirectory, File sourceFile
    ) {
        Set<LiftEntry> selected = Collections.newSetFromMap(new IdentityHashMap<>());
        List<LiftEntry> ordered = new ArrayList<>();
        for (LiftEntry e : entries) {
            if (e == null || e.getOwningDictionary() != source) {
                throw new IllegalArgumentException("not an entry of this dictionary: " + e);
            }
            if (selected.add(e)) {
                ordered.add(e);
            }
        }
        List<AbstractLiftRoot> nodes = new ArrayList<>();
        for (LiftEntry e : ordered) {
            collect(e, nodes);
        }
        List<LiftEntry> stubs = options.references() == ExtractOptions.ReferenceTargets.STUBS
            ? stubsFor(source, nodes, selected)
            : List.of();
        // A stub's forms and glosses use no header value: the selection follows the extract only.
        HeaderSelection header = options.header() == ExtractOptions.HeaderScope.FULL ? null : usedBy(nodes);
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(8192);
            try (LiftWriterSession writer = new LiftWriterSession(buffer, baseDirectory)) {
                writer.marshall(source, ordered, stubs, header);
            }
            File document = sourceFile != null ? sourceFile : baseDirectory.resolve("extract.lift").toFile();
            SAXParser parser = LiftXmlParsers.newSecureParser();
            LiftSaxHandler handler = new LiftSaxHandler(target, LoadOptions.defaults(), document, true);
            parser.parse(new ByteArrayInputStream(buffer.toByteArray()), handler,
                document.getAbsoluteFile().toURI().toString());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot extract entries: " + e.getMessage(), e);
        }
        Set<String> stubIds = new LinkedHashSet<>();
        for (LiftEntry stub : stubs) {
            stub.getId().ifPresent(stubIds::add);
        }
        return Set.copyOf(stubIds);
    }

    private static void collect(AbstractLiftRoot node, List<AbstractLiftRoot> nodes) {
        nodes.add(node);
        for (AbstractLiftRoot child : DictionaryMutator.childrenOf(node)) {
            collect(child, nodes);
        }
    }

    /** The entries, not extracted, that the relations and variants of the extract refer to. */
    private static List<LiftEntry> stubsFor(
        LiftDictionary source, List<AbstractLiftRoot> nodes, Set<LiftEntry> selected
    ) {
        Set<LiftEntry> stubs = Collections.newSetFromMap(new IdentityHashMap<>());
        List<LiftEntry> ordered = new ArrayList<>();
        var registry = source.getLiftDictionaryRegistry();
        for (AbstractLiftRoot node : nodes) {
            if (!(node instanceof HasRefId ref)) {
                continue;
            }
            String id = DictionaryMutator.referenceTarget(ref);
            if (id == null) {
                continue;
            }
            AbstractIdentifiable target = registry.getEntryOrSenseByLiftId(id);
            LiftEntry entry = entryOf(target);
            if (entry != null && !selected.contains(entry) && stubs.add(entry)) {
                ordered.add(entry);
            }
        }
        return ordered;
    }

    private static LiftEntry entryOf(AbstractLiftRoot node) {
        for (AbstractLiftRoot n = node; n != null; n = n.getParentNode()) {
            if (n instanceof LiftEntry e) {
                return e;
            }
        }
        return null;
    }

    /** The ranges, values and definitions the nodes use; a value with the values above it. */
    private static HeaderSelection usedBy(List<AbstractLiftRoot> nodes) {
        Set<FeatureSet> sets = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Feature> features = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<LiftFieldAndTraitDefinition> definitions = Collections.newSetFromMap(new IdentityHashMap<>());
        for (AbstractLiftRoot node : nodes) {
            switch (node) {
                case LiftTrait t -> {
                    switch (t.getSpecification()) {
                        case LiftFieldAndTraitDefinition d -> addDefinition(d, definitions, sets);
                        case RangeReference r -> sets.add(r.range());
                        default -> {
                        }
                    }
                    Object value = t.getTypedValue();
                    if (value instanceof Feature f) {
                        addFeature(f, features, sets);
                    } else if (value instanceof Collection<?> values) {
                        for (Object v : values) {
                            if (v instanceof Feature f) {
                                addFeature(f, features, sets);
                            }
                        }
                    }
                }
                case LiftField f -> {
                    if (f.getSpecification() instanceof LiftFieldAndTraitDefinition d) {
                        addDefinition(d, definitions, sets);
                    }
                }
                case GrammaticalInfo g -> addFeature(g.getGramInfoValue(), features, sets);
                default -> {
                }
            }
            if (node instanceof HasType typed) {
                addFeature(typed.getType(), features, sets);
            }
        }
        return new HeaderSelection(sets, features, definitions);
    }

    private static void addDefinition(
        LiftFieldAndTraitDefinition d, Set<LiftFieldAndTraitDefinition> definitions, Set<FeatureSet> sets
    ) {
        if (definitions.add(d)) {
            d.getResolvedFeatureSet().ifPresent(sets::add);
        }
    }

    private static void addFeature(Feature f, Set<Feature> features, Set<FeatureSet> sets) {
        for (Feature g = f; g != null && features.add(g); g = g.getSuperOrdinateFeature().orElse(null)) {
            if (g.getParent() != null) {
                sets.add(g.getParent());
            }
        }
    }
}
