package fr.cnrs.lacito.liftapi.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;

public final class LiftExample extends AbstractNotable {

    public final static String DEFAULT_TRANSLATION_TYPE = "";

    @Getter protected Optional<String> source = Optional.empty();
    // TODO only case of MultiText in Map. Create a type Translation implements HasMultitext?
    @Getter protected final Map<String, MultiText> translations = new HashMap<>();
    @Getter @Setter protected LiftSense parent;
    
    protected LiftExample(String source) {
        this.source = Optional.of(source);
    }

    protected LiftExample() {
    }

    /**
     * 
     * @param type
     * @return return a new empty translation.
     * @throws DuplicateTypeException if the translation type already exists.
     */
    // TODO should be in factory for consistency
    public MultiText create_translation(String type) throws DuplicateTypeException {
        if (type == null) throw new IllegalArgumentException("Translation type cannot be null");
        if (translations.containsKey(type)) throw new DuplicateTypeException("A translation of type " + type + "already exist.");
        MultiText newTranslation = new MultiText();
        translations.put(type, newTranslation);
        return newTranslation;
    }

    /**
     * 
     * @param type
     * @return the translation of the given type or an empty translation if the type does not exist.
     * @throws IllegalArgumentException if no translation of this type exists.
     */
    public MultiText get_translation(String type) {
        if (translations.containsKey(type)) {
            return translations.get(type);
        } else {
            throw new IllegalArgumentException("Unknown translation type: " + type);
        }
    }

    public MultiText getExample() {
        return getMainMultiText();
    }

    public void setSource(String value) {
        this.source = Optional.of(value);
    }

}
