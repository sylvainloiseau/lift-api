package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A MultiText is a set of parallel {@link Form} in one or several languages,
 * either a set of object language(s) or a set of meta languages(s). In a
 * MultiText there can be only one {@link Form} in each language.
 * 
 * All LIFT field holding textual content (appart from terminological components : type of {@link LiftField},
 * name and value of {@link LiftTrait}, etc.) are stored in such MultiText object, for instance:
 * <ul>
 * <li> The forms of a lexical entry {@link LiftEntry#getForms()} (in object languages)
 * <li> The definition of a sense {@link LiftSense#getDefinition()} (in meta languages)
 * </ul>
 * 
 * @see Form
 */
public final class MultiText {

    protected Map<String, Form> multitext = null;
    protected final static Set<String> EMPTY_LANG_SET = Collections.unmodifiableSet(new HashSet<>());
    protected final static Map<String, Form> EMPTY_FORM_MAP = Collections.unmodifiableMap(new HashMap<>());
    protected List<LiftAnnotation> annotations = new ArrayList<>();

    protected MultiText() {
    }

    public boolean isEmpty() {
        return multitext == null;
    }

    public Optional<Form> getForm(String lang) {
        if (isEmpty()) return Optional.empty();
        else if (multitext.containsKey(lang)) return Optional.of(multitext.get(lang));
        else return Optional.empty();
    }

    public Collection<Form> getForms() {
        if (isEmpty()) return EMPTY_FORM_MAP.values();
        return multitext.values();
    }

    public void removeForm(String lang) {
        if (isEmpty()) {
            throw new IllegalArgumentException("This multitext is empty.");
        }
        if (!multitext.containsKey(lang)) {
            throw new IllegalArgumentException("No text in language: " + lang);
        }
        multitext.remove(lang);
    }

    public Set<String> getLangs() {
        if (isEmpty()) return EMPTY_LANG_SET;
        return multitext.keySet();
    }

    public void add(Form f) {
        if (multitext == null) {
            multitext = new HashMap<>();
        }
        String lang = f.lang;
        if (multitext.containsKey(lang)) throw new DuplicateLangException("Duplicate lang: " + lang);
        multitext.put(lang, f);
    }
}
