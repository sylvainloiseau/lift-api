package fr.cnrs.lacito.liftapi.xml;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithField;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithoutField;
import fr.cnrs.lacito.liftapi.model.AbstractNotable;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftIllustration;
import fr.cnrs.lacito.liftapi.model.LiftMedia;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftReversal;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.LiftVariant;
import fr.cnrs.lacito.liftapi.model.MultiText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Renders a {@link LiftDictionary} as a canonical, order-independent-where-it-should-be
 * list of text lines, so that two dictionaries can be compared for structural equality
 * with a readable diff.
 *
 * Everything the LIFT serialization is supposed to preserve is included: LIFT ids, every
 * {@link MultiText} form (lang + markup-bearing text), traits, fields, notes, annotations,
 * media/illustration labels and reversal nesting depth.
 */
public final class DictionarySnapshot {

    private final List<String> lines = new ArrayList<>();

    private DictionarySnapshot() {}

    public static List<String> of(LiftDictionary d) {
        DictionarySnapshot s = new DictionarySnapshot();
        s.emit("lift.version", String.valueOf(d.getLiftVersion()));
        s.emit(
            "lift.entryCount",
            String.valueOf(d.getLiftDictionaryRegistry().getEntries().size())
        );
        s.emit(
            "lift.senseCount",
            String.valueOf(d.getLiftDictionaryRegistry().getSenses().size())
        );
        for (LiftEntry e : d.getLiftDictionaryRegistry().getEntries()) {
            s.entry("entry", e);
        }
        return s.lines;
    }

    // ------------------------------------------------------------------
    // Components
    // ------------------------------------------------------------------

    private void entry(String path, LiftEntry e) {
        String p = path + "[" + e.getId().orElse("?") + "]";
        emit(p + ".id", e.getId().orElse(""));
        emit(p + ".order", e.getOrder().orElse(""));
        emit(p + ".dateDeleted", e.getDateDeleted().orElse(""));
        multiText(p + ".lexical-unit", e.getForms());
        multiText(p + ".citation", e.getCitations());
        extensible(p, e);
        notable(p, e);
        int i = 0;
        for (LiftPronunciation x : e.getPronunciations()) pronunciation(
            p + ".pronunciation#" + (i++),
            x
        );
        i = 0;
        for (LiftVariant x : e.getVariants()) variant(p + ".variant#" + (i++), x);
        i = 0;
        for (LiftRelation x : e.getRelations()) relation(
            p + ".relation#" + (i++),
            x
        );
        i = 0;
        for (LiftEtymology x : e.getEtymologies()) etymology(
            p + ".etymology#" + (i++),
            x
        );
        i = 0;
        for (LiftSense x : e.getSenses()) sense(p + ".sense#" + (i++), x);
    }

    private void sense(String p, LiftSense s) {
        emit(p + ".id", s.getId().orElse(""));
        emit(
            p + ".order",
            s.getOrder().map(String::valueOf).orElse("")
        );
        emit(
            p + ".gramInfo",
            s.getGrammaticalInfo().map(g -> g.getGramInfoValue().getId()).orElse("")
        );
        s
            .getGrammaticalInfo()
            .ifPresent(g -> {
                int t = 0;
                for (LiftTrait x : g.getTraits()) trait(
                    p + ".gramInfo.trait#" + (t++),
                    x
                );
            });
        multiText(p + ".gloss", s.getGlosses());
        multiText(p + ".definition", s.getDefinition());
        extensible(p, s);
        notable(p, s);
        int i = 0;
        for (LiftRelation x : s.getRelations()) relation(
            p + ".relation#" + (i++),
            x
        );
        i = 0;
        for (LiftExample x : s.getExamples()) example(p + ".example#" + (i++), x);
        i = 0;
        for (LiftIllustration x : s.getIllustrations()) {
            String q = p + ".illustration#" + (i++);
            emit(q + ".href", x.getHref());
            multiText(q + ".label", x.getLabel());
        }
        i = 0;
        for (LiftReversal x : s.getReversals()) reversal(
            p + ".reversal#" + (i++),
            x
        );
        i = 0;
        for (LiftSense x : s.getSenses()) sense(p + ".subsense#" + (i++), x);
    }

    private void reversal(String p, LiftReversal r) {
        emit(p + ".type", r.getType() == null ? "" : r.getType().getId());
        multiText(p + ".form", r.getForms());
        if (r.getMain() != null) {
            reversal(p + ".main", r.getMain());
        } else {
            emit(p + ".main", "");
        }
    }

    private void example(String p, LiftExample e) {
        emit(p + ".source", e.getSource().orElse(""));
        multiText(p + ".form", e.getExample());
        // Translations are keyed by Feature; sort by feature id for a stable order.
        Map<String, MultiText> byType = new TreeMap<>();
        for (Map.Entry<Feature, MultiText> t : e.getTranslations().entrySet()) {
            byType.put(t.getKey().getId(), t.getValue());
        }
        for (Map.Entry<String, MultiText> t : byType.entrySet()) {
            multiText(p + ".translation[" + t.getKey() + "]", t.getValue());
        }
        extensible(p, e);
        notable(p, e);
    }

    private void etymology(String p, LiftEtymology e) {
        emit(p + ".type", e.getType() == null ? "" : e.getType().getId());
        emit(p + ".source", e.getSource());
        multiText(p + ".form", e.getForms());
        multiText(p + ".gloss", e.getGlosses());
        extensible(p, e);
    }

    private void variant(String p, LiftVariant v) {
        emit(
            p + ".ref",
            v.getRefObject() == null
                ? v.getRefId().orElse("")
                : v.getRefObject().getId().orElse("")
        );
        multiText(p + ".form", v.getForms());
        extensible(p, v);
        int i = 0;
        for (LiftPronunciation x : v.getPronunciations()) pronunciation(
            p + ".pronunciation#" + (i++),
            x
        );
        i = 0;
        for (LiftRelation x : v.getRelations()) relation(
            p + ".relation#" + (i++),
            x
        );
    }

    private void relation(String p, LiftRelation r) {
        emit(p + ".type", r.getType() == null ? "" : r.getType().getId());
        emit(
            p + ".ref",
            r.getRefObject() == null
                ? r.getRefId().orElse("")
                : r.getRefObject().getId().orElse("")
        );
        emit(p + ".order", r.getOrder().map(String::valueOf).orElse(""));
        multiText(p + ".usage", r.getUsage());
        extensible(p, r);
    }

    private void pronunciation(String p, LiftPronunciation pr) {
        multiText(p + ".form", pr.getPronunciation());
        extensible(p, pr);
        int i = 0;
        for (LiftMedia m : pr.getMedias()) {
            String q = p + ".media#" + (i++);
            emit(q + ".href", m.getHref());
            multiText(q + ".label", m.getLabel());
        }
    }

    private void note(String p, LiftNote n) {
        emit(p + ".type", n.getType() == null ? "" : n.getType().getId());
        multiText(p + ".form", n.getText());
        extensible(p, n);
    }

    private void trait(String p, LiftTrait t) {
        emit(p + ".name", t.getSpecification().getName());
        emit(p + ".value", t.getValue());
        int i = 0;
        for (LiftAnnotation a : t.getAnnotations()) annotation(
            p + ".annotation#" + (i++),
            a
        );
    }

    private void annotation(String p, LiftAnnotation a) {
        emit(p + ".name", a.getType() == null ? "" : a.getType().getId());
        emit(p + ".value", a.getValue());
        emit(p + ".who", a.getWho());
        emit(p + ".when", a.getWhen());
        multiText(p + ".form", a.getText());
    }

    private void field(String p, LiftField f) {
        emit(p + ".name", f.getSpecification().getName());
        multiText(p + ".form", f.getText());
        extensible(p, f);
    }

    // ------------------------------------------------------------------
    // Shared facets
    // ------------------------------------------------------------------

    private void extensible(String p, AbstractExtensibleWithoutField o) {
        emit(p + ".dateCreated", o.getDateCreated().orElse(""));
        emit(p + ".dateModified", o.getDateModified().orElse(""));
        int i = 0;
        for (LiftTrait t : o.getTraits()) trait(p + ".trait#" + (i++), t);
        i = 0;
        for (LiftAnnotation a : o.getAnnotations()) annotation(
            p + ".annotation#" + (i++),
            a
        );
        if (o instanceof AbstractExtensibleWithField wf) {
            for (String name : new TreeMap<>(wf.getFields()).keySet()) {
                field(p + ".field[" + name + "]", wf.getFields().get(name));
            }
        }
    }

    private void notable(String p, AbstractNotable o) {
        for (String type : new TreeMap<>(o.getNotes()).keySet()) {
            note(p + ".note[" + type + "]", o.getNotes().get(type));
        }
    }

    private void multiText(String p, MultiText mt) {
        if (mt == null) {
            emit(p, "<null>");
            return;
        }
        // Forms are held in a hash map: sort by language for a stable comparison.
        Map<String, Form> byLang = new TreeMap<>();
        for (Form f : mt.getForms()) byLang.put(f.getLang(), f);
        for (Map.Entry<String, Form> f : byLang.entrySet()) {
            emit(p + ".form[" + f.getKey() + "]", f.getValue().toString());
            int i = 0;
            for (LiftAnnotation a : f.getValue().getAnnotations()) annotation(
                p + ".form[" + f.getKey() + "].annotation#" + (i++),
                a
            );
        }
        int i = 0;
        for (LiftAnnotation a : mt.getAnnotations()) annotation(
            p + ".annotation#" + (i++),
            a
        );
    }

    private void emit(String key, String value) {
        if (value == null || value.isEmpty()) return;
        lines.add(key + " = " + value);
    }
}
