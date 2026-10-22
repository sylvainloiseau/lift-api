package fr.cnrs.lacito.liftapi.change;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryLanguagesManager;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithField;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithoutField;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.MultiText;
import fr.cnrs.lacito.liftapi.xml.DictionarySnapshot;

/**
 * Everything a dictionary holds, as comparable lines: what {@link DictionarySnapshot}
 * covers (the entries), plus what it leaves out because the file round trip does not need
 * it - guids, the header (ranges, features, definitions) and the languages with their
 * occurrence counts. Lists in order, maps sorted by key.
 */
final class ModelSnapshot {

    private final List<String> lines = new ArrayList<>();

    private ModelSnapshot() {}

    static List<String> of(LiftDictionary d) {
        ModelSnapshot s = new ModelSnapshot();
        s.lines.addAll(DictionarySnapshot.of(d));
        // The registry's lists of senses follow registration, not the document: sort.
        TreeMap<String, String> guids = new TreeMap<>();
        for (LiftEntry e : d.getLiftDictionaryRegistry().getEntries()) {
            guids.put("entry " + e.getId().orElse(""), e.getGuid().orElse(""));
        }
        for (LiftSense x : d.getLiftDictionaryRegistry().getSenses()) {
            guids.put("sense " + x.getId().orElse(""), x.getGuid().orElse(""));
        }
        guids.forEach((k, v) -> s.emit("guid[" + k + "]", v));
        s.languages("objectLanguages", d.getObjectLanguageManager());
        s.languages("metaLanguages", d.getMetaLanguageManager());
        s.multiText("header.description", d.getHeader().getDescription());
        List<FeatureSet> sets = new ArrayList<>(d.getHeader().getFeatureSets());
        sets.sort(Comparator.comparing(FeatureSet::getId));
        for (FeatureSet fs : sets) {
            s.featureSet("range[" + fs.getId() + "]", fs);
        }
        List<LiftFieldAndTraitDefinition> defs = new ArrayList<>(d.getHeader().getFieldsAndTraitsDefinitions());
        defs.sort(Comparator.comparing(LiftFieldAndTraitDefinition::getName));
        for (LiftFieldAndTraitDefinition def : defs) {
            s.definition("definition[" + def.getName() + "]", def);
        }
        return s.lines;
    }

    private void languages(String p, LiftDictionaryLanguagesManager m) {
        for (String lang : new TreeSet<>(m.getLanguages())) {
            emit(p + "[" + lang + "]", String.valueOf(m.getLanguageOccurrence(lang)));
        }
    }

    private void featureSet(String p, FeatureSet fs) {
        emit(p + ".guid", fs.getGuid().orElse(""));
        emit(p + ".href", fs.getHref().orElse(""));
        multiText(p + ".label", fs.getLabel());
        multiText(p + ".abbrev", fs.getAbbrev());
        multiText(p + ".description", fs.getDescription());
        extensible(p, fs);
        for (Feature f : new TreeMap<>(fs.getFeatures()).values()) {
            String q = p + ".feature[" + f.getId() + "]";
            emit(q + ".guid", f.getGuid().orElse(""));
            emit(q + ".parent", f.getSuperOrdinateFeature().map(Feature::getId).orElse(""));
            multiText(q + ".label", f.getLabel());
            multiText(q + ".abbrev", f.getAbbrev());
            multiText(q + ".description", f.getDescription());
            extensible(q, f);
        }
    }

    private void definition(String p, LiftFieldAndTraitDefinition def) {
        emit(p + ".kind", String.valueOf(def.getKind()));
        emit(p + ".targets", def.getTargetAsString());
        emit(p + ".dataModel", def.getTypeStr() + (def.isDataModelDeclared() ? "" : " (undeclared)"));
        emit(p + ".writingSystem", def.getWritingSystem().orElse(""));
        emit(p + ".range", def.getResolvedFeatureSet().map(FeatureSet::getId).orElse(""));
        multiText(p + ".label", def.getLabel());
        multiText(p + ".description", def.getDescription());
    }

    private void extensible(String p, AbstractExtensibleWithoutField o) {
        emit(p + ".dateCreated", o.getDateCreated().orElse(""));
        emit(p + ".dateModified", o.getDateModified().orElse(""));
        int i = 0;
        for (LiftTrait t : o.getTraits()) {
            emit(p + ".trait#" + i + ".name", DictionarySnapshot.specification(t.getSpecification()));
            emit(p + ".trait#" + (i++) + ".value", t.getValue());
        }
        i = 0;
        for (LiftAnnotation a : o.getAnnotations()) {
            emit(p + ".annotation#" + (i++), a.getType() == null ? "" : a.getType().getId());
        }
        if (o instanceof AbstractExtensibleWithField wf) {
            for (Map.Entry<String, LiftField> f : new TreeMap<>(wf.getFields()).entrySet()) {
                emit(p + ".field", DictionarySnapshot.specification(f.getValue().getSpecification()));
            }
        }
    }

    private void multiText(String p, MultiText mt) {
        for (Form f : new TreeMap<>(mt.getFormsByLanguage()).values()) {
            emit(p + ".form[" + f.getLang() + "]", f.toString());
        }
    }

    private void emit(String key, String value) {
        lines.add(key + " = " + value);
    }
}
