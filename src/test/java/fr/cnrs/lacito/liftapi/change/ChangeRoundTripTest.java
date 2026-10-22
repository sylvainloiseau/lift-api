package fr.cnrs.lacito.liftapi.change;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
import fr.cnrs.lacito.liftapi.internal.DictionaryMutator;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.AbstractNotable;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.GrammaticalInfo;
import fr.cnrs.lacito.liftapi.model.HasSense;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionDataModel;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionKind;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionTarget;
import fr.cnrs.lacito.liftapi.model.LiftIllustration;
import fr.cnrs.lacito.liftapi.model.LiftMedia;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftReversal;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.LiftTranslation;
import fr.cnrs.lacito.liftapi.model.LiftVariant;
import fr.cnrs.lacito.liftapi.model.MultiText;

/**
 * The decisive test of the change events: random operations on the corpus, through the
 * public API only.
 *
 * <ol>
 * <li>After each operation, if the dictionary changed (its full {@link ModelSnapshot}),
 * at least one event must have been reported; if the operation was refused, nothing may
 * have changed and nothing may have been reported.</li>
 * <li>Reverting every event, last first, gives back the loaded dictionary, with
 * consistent indexes.</li>
 * <li>Reapplying them, first first, gives back the edited one.</li>
 * </ol>
 *
 * A failure names the seed and lists the operations: the run can be replayed.
 */
class ChangeRoundTripTest {

    private static final File CORPUS = new File("src/test/resources/lift/DictionaryLift20261022.lift");
    private static final int OPERATIONS = 250;

    static Stream<Long> seeds() {
        return LongStream.rangeClosed(1, 12).boxed();
    }

    @ParameterizedTest
    @MethodSource("seeds")
    void everyChangeIsReportedAndCanBeRevertedThenReapplied(long seed) throws Exception {
        Logger.getLogger(LiftDictionary.class.getName()).setLevel(Level.WARNING);
        LiftDictionary d = LiftDictionary.loadDictionaryFromFile(CORPUS);
        List<String> s0 = ModelSnapshot.of(d);
        List<ModelChange> events = new ArrayList<>();
        var subscription = d.addChangeListener(events::add);

        Ctx ctx = new Ctx(d, new Random(seed));
        List<String> log = new ArrayList<>();
        List<String> before = s0;
        for (int i = 0; i < OPERATIONS; i++) {
            Op op = OPS.get(ctx.r.nextInt(OPS.size()));
            int n0 = events.size();
            RuntimeException refusal = null;
            try {
                op.action().accept(ctx);
            } catch (Skip e) {
                // Nothing to work on: an ordinary operation, which may have changed things.
            } catch (RuntimeException e) {
                refusal = e;
            }
            List<String> after = ModelSnapshot.of(d);
            log.add("#" + i + " " + op.name()
                + (refusal == null ? "" : " (refused: " + refusal.getClass().getSimpleName() + ")")
                + " -> " + (events.size() - n0) + " event(s)");
            if (refusal != null) {
                if (!after.equals(before) || events.size() != n0) {
                    fail("seed " + seed + ": a refused operation changed the dictionary or reported something: "
                        + op.name() + "\n" + diff(before, after) + "\n" + String.join("\n", log), refusal);
                }
            } else if (!after.equals(before) && events.size() == n0) {
                fail("seed " + seed + ": changed without an event: " + op.name() + "\n"
                    + diff(before, after) + "\n" + String.join("\n", log));
            }
            before = after;
        }
        List<String> s1 = before;
        subscription.unsubscribe();
        List<ModelChange> recorded = List.copyOf(events);

        for (int i = recorded.size() - 1; i >= 0; i--) {
            recorded.get(i).revert();
        }
        assertSame("seed " + seed + ": reverting every change", s0, ModelSnapshot.of(d), log);
        assertIndexesConsistent(d);

        for (ModelChange c : recorded) {
            c.reapply();
        }
        assertSame("seed " + seed + ": reapplying every change", s1, ModelSnapshot.of(d), log);
        assertIndexesConsistent(d);
    }

    private static void assertSame(String what, List<String> expected, List<String> actual, List<String> log) {
        if (!expected.equals(actual)) {
            fail(what + " does not give the expected dictionary:\n" + diff(expected, actual)
                + "\n" + String.join("\n", log));
        }
    }

    private static String diff(List<String> a, List<String> b) {
        List<String> onlyA = new ArrayList<>(a);
        onlyA.removeAll(b);
        List<String> onlyB = new ArrayList<>(b);
        onlyB.removeAll(a);
        StringBuilder sb = new StringBuilder();
        onlyA.stream().limit(15).forEach(l -> sb.append("- ").append(l).append('\n'));
        onlyB.stream().limit(15).forEach(l -> sb.append("+ ").append(l).append('\n'));
        if (sb.isEmpty()) {
            int i = 0;
            while (i < Math.min(a.size(), b.size()) && a.get(i).equals(b.get(i))) i++;
            sb.append("same lines, in another order; first difference at line ").append(i).append(":\n");
            for (int k = Math.max(0, i - 2); k < Math.min(a.size(), i + 4); k++) sb.append("  expected ").append(a.get(k)).append('\n');
            for (int k = Math.max(0, i - 2); k < Math.min(b.size(), i + 4); k++) sb.append("  actual   ").append(b.get(k)).append('\n');
        }
        return sb.toString();
    }

    /**
     * Every component reachable from the entries is registered, and the registry holds
     * nothing else.
     */
    static void assertIndexesConsistent(LiftDictionary d) {
        Map<Class<?>, Integer> reachable = new HashMap<>();
        for (LiftEntry e : d.getLiftDictionaryRegistry().getEntries()) {
            count(e, reachable);
        }
        LiftDictionaryRegistry reg = d.getLiftDictionaryRegistry();
        Map<Class<?>, Integer> registered = new HashMap<>();
        registered.put(LiftEntry.class, reg.getEntries().size());
        registered.put(LiftSense.class, reg.getSenses().size());
        registered.put(LiftExample.class, reg.getExamples().size());
        registered.put(LiftVariant.class, reg.getVariants().size());
        registered.put(LiftTrait.class, reg.getTraits().size());
        registered.put(LiftReversal.class, reg.getReversals().size());
        registered.put(LiftRelation.class, reg.getRelations().size());
        registered.put(LiftPronunciation.class, reg.getPronunciations().size());
        registered.put(LiftNote.class, reg.getNotes().size());
        registered.put(LiftTranslation.class, reg.getTranslations().size());
        registered.put(LiftMedia.class, reg.getMedias().size());
        registered.put(LiftIllustration.class, reg.getIllustrations().size());
        registered.put(LiftField.class, reg.getFields().size());
        registered.put(LiftEtymology.class, reg.getEtymologies().size());
        registered.put(GrammaticalInfo.class, reg.getGrammaticalInfos().size());
        registered.put(LiftAnnotation.class, reg.getAnnotations().size());
        registered.replaceAll((k, v) -> v == 0 && !reachable.containsKey(k) ? null : v);
        registered.values().removeIf(v -> v == null);
        assertEquals(reachable, registered, "the registry and the tree disagree");
    }

    private static void count(AbstractLiftRoot node, Map<Class<?>, Integer> counts) {
        if (node.getUUID() == null) {
            fail("reachable but not registered: " + node.getClass().getSimpleName());
        }
        counts.merge(node.getClass(), 1, Integer::sum);
        for (AbstractLiftRoot child : DictionaryMutator.childrenOf(node)) {
            count(child, counts);
        }
    }

    // ------------------------------------------------------------------
    // The operations
    // ------------------------------------------------------------------

    record Op(String name, Consumer<Ctx> action) {}

    static final class Ctx {
        final LiftDictionary d;
        final Random r;
        int counter;

        Ctx(LiftDictionary d, Random r) {
            this.d = d;
            this.r = r;
        }

        String fresh() {
            return "x" + (counter++);
        }

        <T> T pick(List<T> list) {
            if (list.isEmpty()) throw new Skip();
            return list.get(r.nextInt(list.size()));
        }

        LiftDictionaryRegistry reg() {
            return d.getLiftDictionaryRegistry();
        }

        LiftEntry entry() {
            return pick(reg().getEntries());
        }

        LiftSense sense() {
            return pick(reg().getSenses());
        }

        String objLang() {
            return pick(new ArrayList<>(d.getObjectLanguageManager().getLanguages()));
        }

        String metaLang() {
            return pick(new ArrayList<>(d.getMetaLanguageManager().getLanguages()));
        }

        Feature type(FeatureSet set) {
            String[] ids = {"t1", "t2", "t3"};
            return set.getOrCreateFeature(ids[r.nextInt(ids.length)]);
        }

        LiftFieldAndTraitDefinition traitDefinition() {
            return d.getHeader().declareTraitDefinition("test-trait");
        }

        LiftFieldAndTraitDefinition fieldDefinition() {
            return d.getHeader().declareFieldDefinition("test-field-" + r.nextInt(3));
        }

        MultiText someMultiText() {
            LiftSense s = sense();
            return switch (r.nextInt(4)) {
                case 0 -> entry().getForms();
                case 1 -> s.getGlosses();
                case 2 -> s.getDefinition();
                default -> entry().getCitations();
            };
        }

        boolean metaLanguageText(MultiText mt) {
            return mt.getParent() instanceof LiftSense;
        }

        int at(List<?> list) {
            return r.nextInt(list.size() + 1);
        }
    }

    /** Thrown when an operation finds nothing to work on: not a refusal. */
    static final class Skip extends RuntimeException {
        Skip() {
            super(null, null, false, false);
        }
    }

    private static final List<Op> OPS = List.of(
        // Entries
        new Op("add entry", c -> {
            LiftEntry e = new LiftEntry();
            e.addForm(new Form(c.objLang(), c.fresh()));
            if (c.r.nextBoolean()) c.d.addEntry(e);
            else c.d.addEntry(e, c.at(c.reg().getEntries()));
        }),
        new Op("remove entry", c -> c.d.removeEntry(c.entry())),
        new Op("rename entry", c -> c.entry().setId("id-" + c.fresh())),
        new Op("entry order and dates", c -> {
            LiftEntry e = c.entry();
            e.setOrder(c.r.nextBoolean() ? null : c.fresh());
            e.setDateModified(c.r.nextBoolean() ? null : "2026-10-" + (10 + c.r.nextInt(10)));
            e.guidProperty().set(c.fresh());
        }),
        // Senses
        new Op("add sense", c -> {
            LiftSense s = new LiftSense();
            s.addGloss(new Form(c.metaLang(), c.fresh()));
            if (c.r.nextBoolean()) {
                LiftEntry e = c.entry();
                e.addSense(c.at(e.getSenses()), s);
            } else {
                LiftSense p = c.sense();
                p.addSense(c.at(p.getSenses()), s);
            }
        }),
        new Op("delete sense", c -> {
            LiftSense s = c.sense();
            ((HasSense) s.getParent()).deleteSense(s);
        }),
        new Op("move sense", c -> {
            LiftSense s = c.sense();
            LiftEntry target = c.entry();
            if (s.getParentEntry() == target) throw new Skip();
            s.detach();
            target.addSense(c.at(target.getSenses()), s);
        }),
        new Op("sense order", c -> c.sense().setOrder(c.r.nextBoolean() ? null : c.r.nextInt(5))),
        new Op("grammatical info", c -> {
            LiftSense s = c.sense();
            switch (c.r.nextInt(3)) {
                case 0 -> s.setGrammaticalInfo(c.type(c.d.getHeader().getGrammaticalInfoManager()));
                case 1 -> s.deleteGrammaticalInfo();
                default -> s.getGrammaticalInfo().orElseThrow(Skip::new)
                    .setGramInfoValue(c.type(c.d.getHeader().getGrammaticalInfoManager()));
            }
        }),
        // Examples and translations
        new Op("add example", c -> {
            LiftSense s = c.sense();
            LiftExample x = new LiftExample();
            x.getExample().add(new Form(c.objLang(), c.fresh()));
            s.addExample(c.at(s.getExamples()), x);
        }),
        new Op("delete example", c -> {
            LiftExample x = c.pick(c.reg().getExamples());
            x.getParent().deleteExample(x);
        }),
        new Op("move example", c -> {
            LiftExample x = c.pick(c.reg().getExamples());
            LiftSense target = c.sense();
            x.detach();
            target.addExample(c.at(target.getExamples()), x);
        }),
        new Op("example source", c -> c.pick(c.reg().getExamples()).setSource(c.r.nextBoolean() ? null : c.fresh())),
        new Op("translation", c -> {
            LiftExample x = c.pick(c.reg().getExamples());
            FeatureSet types = c.d.getHeader().getTranslationTypeManager();
            switch (c.r.nextInt(3)) {
                case 0 -> x.createTranslation(c.type(types)).add(new Form(c.metaLang(), c.fresh()));
                case 1 -> x.deleteTranslation(c.pick(new ArrayList<>(x.getTranslationComponents())));
                default -> c.pick(new ArrayList<>(x.getTranslationComponents())).setType(c.type(types));
            }
        }),
        // Notes, fields, traits, annotations
        new Op("note", c -> {
            AbstractNotable n = c.r.nextBoolean() ? c.entry() : c.sense();
            FeatureSet types = c.d.getHeader().getNoteTypeManager();
            switch (c.r.nextInt(3)) {
                case 0 -> {
                    LiftNote note = new LiftNote(c.type(types));
                    note.addText(new Form(c.metaLang(), c.fresh()));
                    n.addNote(note);
                }
                case 1 -> n.deleteNote(c.pick(new ArrayList<>(n.getNotes().values())));
                default -> c.pick(new ArrayList<>(n.getNotes().values())).setType(c.type(types));
            }
        }),
        new Op("field", c -> {
            LiftEntry e = c.entry();
            if (c.r.nextBoolean()) {
                LiftField f = new LiftField(c.fieldDefinition());
                f.addText(new Form(c.metaLang(), c.fresh()));
                e.addField(f);
            } else {
                e.deleteField(c.pick(new ArrayList<>(e.getFields().values())));
            }
        }),
        new Op("trait", c -> {
            LiftSense s = c.sense();
            switch (c.r.nextInt(3)) {
                case 0 -> s.addTrait(c.at(s.getTraits()), new LiftTrait(c.traitDefinition(), c.fresh()));
                case 1 -> s.deleteTrait(c.pick(s.getTraits()));
                default -> {
                    LiftTrait t = c.pick(c.reg().getTraits());
                    if (t.getSpecification().getDataModel() != LiftFieldAndTraitDefinitionDataModel.STRING) throw new Skip();
                    t.stringValueProperty().set(c.fresh());
                }
            }
        }),
        new Op("annotation", c -> {
            FeatureSet types = c.d.getHeader().getAnnotationTypeManager();
            LiftAnnotation a = new LiftAnnotation(c.type(types));
            a.setValue(c.fresh());
            switch (c.r.nextInt(3)) {
                case 0 -> {
                    LiftEntry e = c.entry();
                    e.addAnnotation(c.at(e.getAnnotations()), a);
                }
                case 1 -> {
                    MultiText mt = c.someMultiText();
                    mt.addAnnotation(c.at(mt.getAnnotations()), a);
                }
                default -> {
                    Form f = c.pick(new ArrayList<>(c.someMultiText().getForms()));
                    f.addAnnotation(c.at(f.getAnnotations()), a);
                }
            }
        }),
        new Op("delete annotation", c -> {
            LiftAnnotation a = c.pick(c.reg().getAnnotations());
            a.getParent().deleteAnnotation(a);
        }),
        new Op("annotation attributes", c -> {
            LiftAnnotation a = c.pick(c.reg().getAnnotations());
            a.whoProperty().set(c.fresh());
            a.setWhen("2026-10-14");
            a.setType(c.type(c.d.getHeader().getAnnotationTypeManager()));
        }),
        // Texts
        new Op("type in a form", c -> {
            MultiText mt = c.someMultiText();
            String lang = c.metaLanguageText(mt) ? c.metaLang() : c.objLang();
            mt.formTextProperty(lang).set(c.r.nextInt(4) == 0 ? "" : c.fresh());
        }),
        new Op("change a form's text", c -> c.pick(new ArrayList<>(c.someMultiText().getForms())).changeText(c.fresh())),
        new Op("languages", c -> {
            var m = c.r.nextBoolean() ? c.d.getObjectLanguageManager() : c.d.getMetaLanguageManager();
            if (c.r.nextBoolean()) m.addLanguage("qaa-" + c.fresh());
            else m.removeLanguage(c.pick(new ArrayList<>(m.getLanguages())));
        }),
        // Relations and variants
        new Op("relation", c -> {
            LiftEntry e = c.entry();
            switch (c.r.nextInt(3)) {
                case 0 -> {
                    LiftRelation rel = new LiftRelation(c.type(c.d.getHeader().getRelationTypeManager()));
                    rel.setRefId(c.entry().getId().orElseThrow());
                    e.addRelation(c.at(e.getRelations()), rel);
                }
                case 1 -> e.deleteRelation(c.pick(e.getRelations()));
                default -> {
                    LiftRelation rel = c.pick(c.reg().getRelations());
                    rel.setRefId(c.entry().getId().orElseThrow());
                    rel.setOrder(c.r.nextInt(3));
                    rel.setType(c.type(c.d.getHeader().getRelationTypeManager()));
                }
            }
        }),
        new Op("variant", c -> {
            LiftEntry e = c.entry();
            if (c.r.nextBoolean()) {
                LiftVariant v = new LiftVariant();
                v.getForms().add(new Form(c.objLang(), c.fresh()));
                e.addVariant(c.at(e.getVariants()), v);
            } else {
                e.deleteVariant(c.pick(e.getVariants()));
            }
        }),
        // Pronunciations, media, illustrations, etymologies
        new Op("pronunciation", c -> {
            LiftEntry e = c.entry();
            switch (c.r.nextInt(3)) {
                case 0 -> {
                    LiftPronunciation p = new LiftPronunciation();
                    p.getPronunciation().add(new Form(c.objLang(), c.fresh()));
                    p.addMedia(new LiftMedia(c.fresh() + ".wav"));
                    e.addPronunciation(c.at(e.getPronunciations()), p);
                }
                case 1 -> e.deletePronunciation(c.pick(e.getPronunciations()));
                default -> {
                    LiftMedia m = c.pick(c.reg().getMedias());
                    if (c.r.nextBoolean()) m.setHref(c.fresh() + ".mp3");
                    else m.getParent().deleteMedia(m);
                }
            }
        }),
        new Op("illustration", c -> {
            LiftSense s = c.sense();
            switch (c.r.nextInt(3)) {
                case 0 -> s.addIllustration(c.at(s.getIllustrations()), new LiftIllustration(c.fresh() + ".png"));
                case 1 -> s.deleteIllustration(c.pick(s.getIllustrations()));
                default -> c.pick(c.reg().getIllustrations()).hrefProperty().set(c.fresh() + ".jpg");
            }
        }),
        new Op("etymology", c -> {
            LiftEntry e = c.entry();
            FeatureSet types = c.d.getHeader().getEtymologyTypeManager();
            switch (c.r.nextInt(3)) {
                case 0 -> e.addEtymology(c.at(e.getEtymologies()), LiftEtymology.create(c.type(types), c.fresh()));
                case 1 -> e.deleteEtymology(c.pick(e.getEtymologies()));
                default -> {
                    LiftEtymology y = c.pick(c.reg().getEtymologies());
                    y.setSource(c.fresh());
                    y.setType(c.type(types));
                }
            }
        }),
        new Op("reversal", c -> {
            LiftSense s = c.sense();
            FeatureSet types = c.d.getHeader().getInverseTypeManager();
            switch (c.r.nextInt(4)) {
                case 0 -> {
                    LiftReversal rev = new LiftReversal(c.type(types));
                    rev.getForms().add(new Form(c.metaLang(), c.fresh()));
                    s.addReversal(c.at(s.getReversals()), rev);
                }
                case 1 -> s.deleteReversal(c.pick(s.getReversals()));
                case 2 -> {
                    LiftReversal main = new LiftReversal(c.type(types));
                    c.pick(c.reg().getReversals()).setMain(main);
                }
                default -> c.pick(c.reg().getReversals()).deleteMain();
            }
        }),
        // Header
        new Op("feature", c -> {
            FeatureSet set = c.d.getHeader().getNoteTypeManager();
            switch (c.r.nextInt(4)) {
                case 0 -> set.addFeature(c.fresh()).getLabel().formTextProperty(c.metaLang()).set(c.fresh());
                case 1 -> {
                    // A feature in use is refused; mostly pick unused ones, so that a
                    // removal is still exercised, and now and then any one.
                    List<String> unused = set.getFeatures().values().stream()
                        .filter(f -> !c.d.getHeader().isUsed(f)).map(Feature::getId).toList();
                    set.removeFeature(c.pick(unused.isEmpty() || c.r.nextInt(4) == 0
                        ? new ArrayList<>(set.getFeatures().keySet()) : unused));
                }
                case 2 -> set.changeFeatureId(c.pick(new ArrayList<>(set.getFeatures().values())), c.fresh());
                default -> c.pick(new ArrayList<>(set.getFeatures().values()))
                    .setSuperordinateFeature(c.pick(new ArrayList<>(set.getFeatures().values())));
            }
        }),
        new Op("feature set", c -> {
            var h = c.d.getHeader();
            switch (c.r.nextInt(3)) {
                case 0 -> {
                    FeatureSet added = h.addFeatureSet("range-" + c.fresh());
                    // An href whose file was never loaded makes the set unrenamable.
                    if (c.r.nextBoolean()) added.setHref(c.fresh());
                }
                case 1 -> h.removeFeatureSet(c.pick(h.getFeatureSets()));
                // Refused for a built-in set or an unloaded external one, as allowed.
                default -> h.renameFeatureSet(c.pick(h.getFeatureSets()), "range-" + c.fresh());
            }
        }),
        new Op("trait and field specifications", c -> {
            var h = c.d.getHeader();
            switch (c.r.nextInt(6)) {
                // A trait named after a range, or undeclared: the builder resolves the name.
                case 0 -> c.d.getComponentBuilder()
                    .trait(c.sense(), c.pick(h.getFeatureSets()).getId(), "v" + c.r.nextInt(3)).build();
                case 1 -> c.d.getComponentBuilder()
                    .trait(c.sense(), "informal-" + c.r.nextInt(3), c.fresh()).build();
                // Declaring the name of traits named after a range, or undeclared, rebinds them.
                case 2 -> h.declareTraitDefinition(c.pick(c.reg().getTraits()).getName());
                // A range named like undeclared traits: they become traits of the range.
                case 3 -> h.addFeatureSet("informal-" + c.r.nextInt(3));
                // A renamed range takes the undeclared traits of its new name.
                case 4 -> h.renameFeatureSet(c.pick(h.getFeatureSets()), "informal-" + c.r.nextInt(3));
                // Declaring the name of undeclared fields (the corpus has "note", "summary").
                default -> h.declareFieldDefinition(c.pick(c.reg().getFields()).getName());
            }
        }),
        new Op("definition", c -> {
            var h = c.d.getHeader();
            switch (c.r.nextInt(5)) {
                case 0 -> h.createFieldDefinition("def-" + c.fresh()).setTargets("entry sense");
                case 1 -> h.removeFieldOrTraitDefinition(c.pick(h.getFieldsAndTraitsDefinitions()));
                case 2 -> c.pick(h.getFieldsAndTraitsDefinitions()).setKind(
                    c.r.nextBoolean() ? LiftFieldAndTraitDefinitionKind.FIELD : LiftFieldAndTraitDefinitionKind.UNKNOWN);
                case 3 -> {
                    LiftFieldAndTraitDefinition def = c.pick(h.getFieldsAndTraitsDefinitions());
                    def.setTargets(java.util.Set.of(LiftFieldAndTraitDefinitionTarget.values()[c.r.nextInt(3)]));
                    def.setWritingSystem(java.util.Optional.of(c.metaLang()));
                }
                default -> {
                    LiftFieldAndTraitDefinition def = c.pick(h.getFieldsAndTraitsDefinitions());
                    def.setDataModel(java.util.Optional.of(c.r.nextBoolean() ? "multitext" : "multistring"));
                    def.setResolvedRange(java.util.Optional.of(c.pick(h.getFeatureSets())));
                }
            }
        })
    );
}
