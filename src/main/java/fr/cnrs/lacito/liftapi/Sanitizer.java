package fr.cnrs.lacito.liftapi;

import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionTarget;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.MultiText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Sanitizer {

    private final LiftHeader header;
    private final LiftDictionaryRegistry registry;
    private LiftDictionary dictionary;

    public Sanitizer(
        LiftHeader header,
        LiftDictionary dictionary
    ) {
        this.header = header;
        this.dictionary = dictionary;
        this.registry = dictionary.getLiftDictionaryRegistry();
    }

    public void operate() {
        updateFieldAndTraitDefinitionsWithTargetClasses();
    }

    public void updateFieldAndTraitDefinitionsWithTargetClasses() {
        Map<String, List<String>> classByTrait = new HashMap<>();
        // iterate on trait and see with which object each trait name is used.
        for (LiftTrait t : registry.traitsById.values()) {
            String traitName = t.getDefinition().getName();
            String className = t.getParent().getClass().getName();
            classByTrait.compute(traitName, (k, v) -> {
                if (v == null) v = new ArrayList<>();
                v.add(className);
                return v;
            });
        }
        // iterate on field and see with which object each trait name is used.
        for (LiftField t : registry.fieldsById.values()) {
            String traitName = t.getType().getName();
            String className = t.getParent().getClass().getName();
            classByTrait.compute(traitName, (k, v) -> {
                if (v == null) v = new ArrayList<>();
                v.add(className);
                return v;
            });
        }
        // Now update field and trait definition with the resolved targets.
        for (LiftFieldAndTraitDefinition def : header.getFieldsAndTraitsDefinitions()) {
            String name = def.getName();
            for (String className : classByTrait.get(name)) {
                LiftFieldAndTraitDefinitionTarget target =
                    LiftFieldAndTraitDefinitionTarget.fromClassName(className);
                if (target != null) def.getTargets().add(target);
            }
        }
    }

    private void discoverLanguage() {
        Set<String> objectLang = discoverLanguage(
            registry.objectTextById.values()
        );
        for (String lang : objectLang) {
            if (!dictionary.getObjectLanguageManager().hasLanguage(lang))
                dictionary.getObjectLanguageManager().addLanguage(lang);
            // if (!header.containsObjectLanguage(lang)) {
            //     header.addObjectLanguage(lang);
            //}
        }
        Set<String> metaLang = discoverLanguage(registry.metaTextById.values());
        for (String lang : metaLang) {
            if (!dictionary.getMetaLanguageManager().hasLanguage(lang))
                dictionary.getMetaLanguageManager().addLanguage(lang);
            // if (!header.containsMetaLanguage(lang)) {
            //     header.addMetaLanguage(lang);
            // }
        }
    }

   public static Set<String> discoverLanguage(
        Collection<MultiText> multiTexts
    ) {
        Set<String> lang = new HashSet<>();
        for (MultiText m : multiTexts) {
            for (Form f : m.getForms()) {
                lang.add(f.getLang());
            }
        }
        return lang;
    }
}
