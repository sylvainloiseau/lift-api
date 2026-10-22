package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.LiftDictionaryLanguagesManager;
import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.change.FormAdded;
import fr.cnrs.lacito.liftapi.change.FormRemoved;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

/**
 * A MultiText is a set of parallel {@link Form} in one or several languages + writting system coordinate.
 *
 * A languages + writting system coordinate can be any documented string refering to a language or a language+writting system.
 *
 * Some Multitext make more sense for giving the same content in several languages (for instance, a {@link LiftSense#getGlosses()}),
 * while other MultiText make more sense for various writting system ({@link LiftVariant#getForms()}).
 *
 * In a MultiText there can be only one {@link Form} in each language/writting system.
 *
 * A multitex is either an object language(s) or a meta language multitext; in the first
 * case the languages of the multitext should belong to the {@link LiftDictionary#getObjectLanguageManager()} set,
 * in the second they should belong {@link LiftDictionary#getMetaLanguageManager()}.
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
public final class MultiText
    implements HasAnnotation
    {

    protected LiftDictionaryRegistry registry;

    /** The forms, by language: a map, their order is not data. */
    private final ObservableMap<String, Form> lang2FormMap = FXCollections.observableHashMap();
    private ObservableMap<String, Form> formsView;
    protected static final Set<String> EMPTY_LANG_SET =
        Collections.unmodifiableSet(new HashSet<>());
    // Allocated on the first annotation, or the first call to getAnnotations(): there
    // are many MultiTexts, and very few have annotations.
    private ObservableList<LiftAnnotation> annotations;
    private ObservableList<LiftAnnotation> annotationsView;
    private final ConcurrentHashMap<
        String,
        StringProperty
    > lang2textPropertyMap = new ConcurrentHashMap<>();
    private UUID uuid;
    private AbstractLiftRoot parent;

    private LiftDictionaryLanguagesManager languageManager;

    public MultiText(AbstractLiftRoot parent) {
        this.parent = parent;
    }

    public AbstractLiftRoot getParent() {
        return parent;
    }

    public boolean isEmpty() {
        return lang2FormMap.isEmpty();
    }

    public Optional<Form> getForm(String lang) {
        if (lang == null) return Optional.empty();
        return Optional.ofNullable(lang2FormMap.get(lang));
    }

    /**
     * @return the forms, read-only; change them with {@link #add(Form)},
     *         {@link #removeForm(String)} or {@link #formTextProperty(String)}
     */
    public Collection<Form> getForms() {
        return Collections.unmodifiableCollection(lang2FormMap.values());
    }

    /**
     * @return the annotations, as a read-only observable list
     */
    @Override
    public ObservableList<LiftAnnotation> getAnnotations() {
        if (annotationsView == null) annotationsView = FXCollections.unmodifiableObservableList(annotations());
        return annotationsView;
    }

    /**
     * Whether this multitext has annotations; unlike {@link #getAnnotations()}, it does
     * not allocate anything.
     */
    public boolean hasAnnotations() {
        return annotations != null && !annotations.isEmpty();
    }

    /** The annotations, created on demand. */
    ObservableList<LiftAnnotation> annotations() {
        if (annotations == null) {
            annotations = FXCollections.observableArrayList();
        }
        return annotations;
    }

    /** The annotations, without allocating: an empty list when there are none. */
    java.util.List<LiftAnnotation> annotationsOrEmpty() {
        return annotations == null ? java.util.List.of() : annotations;
    }

    public boolean containsLang(String lang) {
        return lang2FormMap.containsKey(lang);
    }

    public void removeForm(String lang) {

        DefinitionGuard.requireWritable(this);
        if (isEmpty()) {
            throw new IllegalArgumentException("This multitext is empty.");
        }
        if (!lang2FormMap.containsKey(lang)) {
            throw new IllegalArgumentException("No text in language: " + lang);
        }
        LiftDictionary observer = Changes.observing(this);
        Form removed = lang2FormMap.remove(lang);
        if (languageManager != null) {
            languageManager.removeLanguageOccurrence(lang);
        }
        LiftDictionary registry = parent == null ? null : parent.getOwningDictionary();
        if (registry != null) {
            for (LiftAnnotation a : removed.annotationsOrEmpty()) {
                registry.getMutator().releaseSubtree(a);
            }
        }
        removed.setOwner(null);
        if (observer != null) {
            Changes.fire(observer, new FormRemoved(observer, this, removed));
        }
    }

    public Set<String> getLangs() {
        if (isEmpty()) return EMPTY_LANG_SET;
        return Collections.unmodifiableSet(lang2FormMap.keySet());
    }

    public void add(Form f) {

        DefinitionGuard.requireWritable(this);
        // Validate everything before touching the language counters: incrementing
        // first left the count permanently inflated when the duplicate check threw.
        if (languageManager != null && !languageManager.hasLanguage(f.lang)) {
            throw new IllegalArgumentException(
                "Language not registered in the dictionary: " + f.lang
            );
        }
        if (lang2FormMap.containsKey(f.lang)) throw new DuplicateLangException(
            "Duplicate lang: " + f.lang + "; form '" + f.toPlainText() + "'"
            + " already exists in this MultiText: '" + lang2FormMap.get(f.lang).toPlainText() + "'"
        );
        if (f.getMultiText() != null && f.getMultiText() != this) throw new IllegalArgumentException(
            "This form already belongs to another MultiText: remove it from there first."
        );
        f.setOwner(this);
        lang2FormMap.put(f.lang, f);
        if (languageManager != null) {
            languageManager.addLanguageOccurrence(f.lang);
        }
        // The annotations of the form come with it, as a subtree does.
        LiftDictionary registry = parent == null ? null : parent.getOwningDictionary();
        if (registry != null) {
            for (LiftAnnotation a : f.annotationsOrEmpty()) {
                registry.getMutator().adoptSubtree(a);
            }
        }
        LiftDictionary observer = Changes.observing(this);
        if (observer != null) {
            Changes.fire(observer, new FormAdded(observer, this, f));
        }
    }

    @Override
    public void addAnnotation(LiftAnnotation a) {
        addAnnotation(annotationsOrEmpty().size(), a);
    }

    /**
     * Add an annotation at a given position; it is registered when this multitext's
     * component belongs to a dictionary.
     */
    @Override
    public void addAnnotation(int index, LiftAnnotation a) {
        DefinitionGuard.requireWritable(this);
        annotations().add(index, a);
        a.setParent(this);
        if (parent != null) {
            parent.adoptedIn(this, a);
        }
    }

    /**
     * Remove an annotation, unregistering it if this multitext's component belongs to a
     * dictionary.
     */
    @Override
    public void deleteAnnotation(LiftAnnotation a) {
        DefinitionGuard.requireWritable(this);
        if (a == null || annotationsOrEmpty().stream().noneMatch(x -> x == a)) {
            throw new IllegalArgumentException(
                "This annotation is not held by this " + getClass().getSimpleName() + "."
            );
        }
        if (parent != null) {
            parent.removeChildIn(this, a);
        } else {
            a.unlink();
        }
    }

    @Override
    public void retypeAnnotation(LiftAnnotation a, Feature type) {
        DefinitionGuard.requireWritable(this);
        if (a == null || annotationsOrEmpty().stream().noneMatch(x -> x == a)) {
            throw new IllegalArgumentException(
                "This annotation is not held by this " + getClass().getSimpleName() + "."
            );
        }
        a.assignType(type);
    }

    /**
     * The forms by language, as a read-only observable map.
     */
    public ObservableMap<String, Form> getFormsByLanguage() {
        if (formsView == null) formsView = FXCollections.unmodifiableObservableMap(lang2FormMap);
        return formsView;
    }

    /**
     * @deprecated use {@link #getFormsByLanguage()}, which is observable; this returns a
     *             read-only property over it, and will be removed
     */
    @Deprecated(forRemoval = true)
    public ReadOnlyMapProperty<String, Form> formsProperty() {
        return new ReadOnlyMapWrapper<>(this, "forms", getFormsByLanguage()).getReadOnlyProperty();
    }

    /**
     * A bidirectional JavaFX property for the text of a given language form.
     * Setting it updates the underlying {@link MultiText} by adding/updating/removing the {@link Form}.
     */
    public StringProperty formTextProperty(String lang) {
        if (lang == null) return new SimpleStringProperty("");
        final String key = lang.trim();
        if (key.isEmpty()) return new SimpleStringProperty("");

        return lang2textPropertyMap.computeIfAbsent(key, l -> {
            SimpleStringProperty textProperty = new SimpleStringProperty(
                this,
                "formText[" + l + "]",
                getForm(l).map(Form::toPlainText).orElse("")
            );
            AtomicBoolean syncing = new AtomicBoolean(false);
            AtomicReference<Form> currentFormRef = new AtomicReference<>(
                getForm(l).orElse(null)
            );
            AtomicReference<ChangeListener<String>> formTextListenerRef =
                new AtomicReference<>(null);

            // attach a listener to the actual Form object corresponding
            // to the language (the form object may be a new one) and fire change to the property textPropery.
            Runnable attachFormListener = () -> {
                Form current = currentFormRef.get();
                ChangeListener<String> oldListener = formTextListenerRef.get();
                if (oldListener != null && current != null) {
                    try {
                        current.textProperty().removeListener(oldListener);
                    } catch (Exception ignored) {}
                }

                Form next = getForm(l).orElse(null);
                currentFormRef.set(next);
                if (next == null) {
                    formTextListenerRef.set(null);
                    return;
                }

                ChangeListener<String> listener = (obs, ov, nv) -> {
                    if (syncing.get()) return;
                    String newText = nv == null ? "" : nv;
                    if (!newText.equals(textProperty.get())) {
                        syncing.set(true);
                        try {
                            textProperty.set(newText);
                        } finally {
                            syncing.set(false);
                        }
                    }
                };
                formTextListenerRef.set(listener);
                next.textProperty().addListener(listener);
            };

            // When property changes -> update model
            // (remove from the map if text.equals(""), add if not present and text exists, update text if present and different)
            textProperty.addListener((obs, oldV, newV) -> {
                if (syncing.get()) return;
                syncing.set(true);
                try {
                    String v = newV == null ? "" : newV;
                    if (v.isBlank()) {
                        if (getForm(l).isPresent()) {
                            try {
                                removeForm(l);
                            } catch (Exception ignored) {}
                        }
                    } else {
                        getForm(l).ifPresentOrElse(
                            existing -> existing.changeText(v),
                            () -> {
                                try {
                                    add(new Form(l, v));
                                } catch (Exception ignored) {}
                            }
                        );
                    }
                } finally {
                    syncing.set(false);
                }
            });

            // When map changes (add/remove/replace) -> update property value
            // The property value is updated, but we have also to listen to the new Form : call runnable.
            ObservableMap<String, Form> map = lang2FormMap;
            MapChangeListener<String, Form> listener = change -> {
                if (!l.equals(change.getKey())) return;
                String newText = Optional.ofNullable(lang2FormMap.get(l))
                    .map(Form::toPlainText)
                    .orElse("");
                if (!newText.equals(textProperty.get())) {
                    if (syncing.get()) return;
                    syncing.set(true);
                    try {
                        textProperty.set(newText);
                    } finally {
                        syncing.set(false);
                    }
                }
                attachFormListener.run();
            };
            map.addListener(listener);
            attachFormListener.run();
            return textProperty;
        });
    }

    /** Package-private: the mutator reaches it through ModelInternals. */
    void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUUID() {
        return uuid;
    }

    /// Called on registration. A MultiText built into an attached parent is empty at
    /// that point; one built into a detached parent - every text of a file being read,
    /// whose entry is registered once complete - is already filled, and its forms are
    /// counted here.
    ///
    /// Passing `null` detaches this MultiText from its dictionary: it stops
    /// contributing to that dictionary's language counters.
    void setLanguagesManager(LiftDictionaryLanguagesManager languageManager) {
        // can be set on null on purpose, when unregistering the parent component.
        if (languageManager == null) {
            this.languageManager = null;
            return;
        }

        this.languageManager = languageManager;
        for (String lang : lang2FormMap.keySet()) {
            if (!languageManager.hasLanguage(lang)) {
                throw new IllegalArgumentException(
                    "Language not registered in the dictionary: " + lang + "; " +
                    "contains: " + languageManager.getLanguages().toString()
                );
            }
            // TODO unsatisfying. Two places for adding occurrence.
            // Very complex logic: when exactly the LanguageManager is set...
            languageManager.addLanguageOccurrence(lang);
        }
    }

    void unregister() {
        if (languageManager == null) return;
        for (String lang : lang2FormMap.keySet()) {
            languageManager.removeLanguageOccurrence(lang);
        }
    }
}
