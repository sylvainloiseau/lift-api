package fr.cnrs.lacito.liftapi.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

public final class LiftExample extends AbstractNotable {

    public static final String DEFAULT_TRANSLATION_TYPE = "";

    protected Optional<String> source = Optional.empty();

    /**
     * The translations of this example, by type, in document order.
     *
     * This is the source of truth; {@link #translationTexts} mirrors it for the
     * {@code Feature -> MultiText} view handed out by {@link #getTranslations()}.
     */
    private final Map<Feature, LiftTranslation> translations = new LinkedHashMap<>();

    private final ObservableMap<Feature, MultiText> translationTexts =
        FXCollections.observableMap(new LinkedHashMap<>());

    /**
     * Read-only: a translation is a component ({@link LiftTranslation}), added with
     * {@link #addTranslation(LiftTranslation)} or {@link #createTranslation(Feature)} so
     * that it is registered in the dictionary. Putting a bare MultiText in this map
     * would bypass that.
     */
    protected final MapProperty<Feature, MultiText> translationsProperty =
        new SimpleMapProperty<>(
            this,
            "translations",
            FXCollections.unmodifiableObservableMap(translationTexts)
        );

    protected LiftSense parent;

    private final StringProperty sourceProperty = new SimpleStringProperty(
        this,
        "source",
        ""
    );

    protected LiftExample(String source) {
        this.source = Optional.of(source);
        this.sourceProperty.set(source);
    }

    public LiftExample() {}

    /**
     * Create a new, empty translation of the given type and add it to this example.
     *
     * @param type
     * @return return a new empty translation.
     * @throws DuplicateTypeException if the translation type already exists.
     */
    public MultiText createTranslation(Feature type)
        throws DuplicateTypeException {
        if (type == null) throw new IllegalArgumentException(
            "Translation type cannot be null"
        );
        LiftTranslation translation = LiftTranslation.create(type);
        addTranslation(translation);
        return translation.getTranslation();
    }

    /**
     * Add a translation to this example, registering it if this example belongs to a
     * dictionary.
     *
     * @param translation the translation to add
     * @throws DuplicateTypeException if this example already has a translation of the
     *         same type
     */
    public void addTranslation(LiftTranslation translation)
        throws DuplicateTypeException {
        if (translation == null) throw new IllegalArgumentException(
            "Translation cannot be null"
        );
        Feature type = translation.getType();
        if (translations.containsKey(type)) throw new DuplicateTypeException(
            "A translation of type " + type.getId() + " already exists."
        );
        translations.put(type, translation);
        translationTexts.put(type, translation.getTranslation());
        translation.setParent(this);
        adopted(translation);
    }

    /**
     * Remove a translation from this example, unregistering it if this example belongs
     * to a dictionary.
     *
     * @param translation a translation of this example
     */
    public void deleteTranslation(LiftTranslation translation) {
        requireChild(
            translation,
            translation != null && translations.get(translation.getType()) == translation
        );
        orphaned(translation);
        translation.detach();
    }

    /**
     * Change the type of a translation of this example, re-keying it.
     *
     * @param translation a translation of this example
     * @param type its new type
     * @throws DuplicateTypeException if this example already has another translation of
     *         that type; nothing is changed then
     */
    public void retypeTranslation(LiftTranslation translation, Feature type) {
        requireChild(
            translation,
            translation != null && translations.get(translation.getType()) == translation
        );
        if (type == null) throw new IllegalArgumentException(
            "Translation type cannot be null"
        );
        Feature old = translation.getType();
        if (type == old) return;
        if (translations.containsKey(type)) throw new DuplicateTypeException(
            "A translation of type " + type.getId() + " already exists."
        );
        translations.remove(old);
        translationTexts.remove(old);
        translation.assignType(type);
        translations.put(type, translation);
        translationTexts.put(type, translation.getTranslation());
    }

    /**
     * Unlink a translation from the maps of this example; called by
     * {@link AbstractLiftRoot#detach()}.
     */
    void removeTranslation(LiftTranslation translation) {
        translations.remove(translation.getType());
        translationTexts.remove(translation.getType());
    }

    /**
     * Protected: this method is called by the parent when it adopts this LiftExample.
     *
     * @param parent the sense this belongs to, or {@code null} when detaching
     *        (see {@link AbstractLiftRoot#detach()})
     */
    public void setParent(LiftSense parent) {
        this.parent = parent;
    }

    /**
     * @param type
     * @return the translation of the given type.
     * @throws IllegalArgumentException if no translation of this type exists.
     */
    public MultiText getTranslation(Feature type) {
        if (translations.containsKey(type)) {
            return translations.get(type).getTranslation();
        } else {
            throw new IllegalArgumentException(
                "Unknown translation type: " + type.getId()
            );
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

    /**
     * @return an unmodifiable view of the text of each translation, by type
     */
    public Map<Feature, MultiText> getTranslations() {
        return translationsProperty.get();
    }

    /**
     * @return the translations of this example as components, in document order
     */
    public Collection<LiftTranslation> getTranslationComponents() {
        return Collections.unmodifiableCollection(translations.values());
    }

    public MapProperty<Feature, MultiText> translationsProperty() {
        return translationsProperty;
    }

    public StringProperty sourceProperty() {
        return sourceProperty;
    }

    public static LiftExample create() {
        return new LiftExample();
    }

    public static LiftExample create(String source) {
        return new LiftExample(source);
    }

    public MultiText getOrCreateTranslation(Feature type) {
        LiftTranslation existing = translations.get(type);
        return existing != null
            ? existing.getTranslation()
            : createTranslation(type);
    }

    @Override
    public AbstractLiftRoot getParentNode() {
        return parent;
    }
}
