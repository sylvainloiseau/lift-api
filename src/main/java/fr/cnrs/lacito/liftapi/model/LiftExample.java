package fr.cnrs.lacito.liftapi.model;

import java.util.Map;
import java.util.Optional;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import lombok.Setter;

public final class LiftExample extends AbstractNotable {

    public final static String DEFAULT_TRANSLATION_TYPE = "";

    protected Optional<String> source = Optional.empty();
    protected final MapProperty<String, MultiText> translationsProperty =
            new SimpleMapProperty<>(this, "translations", FXCollections.observableHashMap());
    @Setter protected LiftSense parent;

    private final StringProperty sourceProperty = new SimpleStringProperty(this, "source", "");
    
    protected LiftExample(String source) {
        this.source = Optional.of(source);
        this.sourceProperty.set(source);
    }

    protected LiftExample() {
    }

    /**
     * @param type
     * @return return a new empty translation.
     * @throws DuplicateTypeException if the translation type already exists.
     */
    public MultiText create_translation(String type) throws DuplicateTypeException {
        if (type == null) throw new IllegalArgumentException("Translation type cannot be null");
        if (translationsProperty.containsKey(type)) throw new DuplicateTypeException("A translation of type " + type + "already exist.");
        MultiText newTranslation = new MultiText();
        translationsProperty.put(type, newTranslation);
        return newTranslation;
    }

    /**
     * @param type
     * @return the translation of the given type.
     * @throws IllegalArgumentException if no translation of this type exists.
     */
    public MultiText get_translation(String type) {
        if (translationsProperty.containsKey(type)) {
            return translationsProperty.get(type);
        } else {
            throw new IllegalArgumentException("Unknown translation type: " + type);
        }
    }

    public MultiText getExample() {
        return getMainMultiText();
    }

    public Optional<String> getSource() {
        return source;
    }

    public void setSource(String value) {
        this.source = Optional.of(value);
        this.sourceProperty.set(value);
    }

    public LiftSense getParent() {
        return parent;
    }

    public Map<String, MultiText> getTranslations() {
        return translationsProperty.get();
    }

    public MapProperty<String, MultiText> translationsProperty() {
        return translationsProperty;
    }

    public StringProperty sourceProperty() {
        return sourceProperty;
    }
}
