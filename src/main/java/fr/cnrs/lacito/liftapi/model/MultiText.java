package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.LiftDictionaryLanguagesManager;
import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
import fr.cnrs.lacito.liftapi.LiftDictionary;
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
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
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

    protected final MapProperty<String, Form> lang2FormMap =
        new SimpleMapProperty<>(FXCollections.observableHashMap());
    protected static final Set<String> EMPTY_LANG_SET =
        Collections.unmodifiableSet(new HashSet<>());
    protected final List<LiftAnnotation> annotations = new ArrayList<>();
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

    public Collection<Form> getForms() {
        return lang2FormMap.values();
    }

    public List<LiftAnnotation> getAnnotations() {
        return annotations;
    }

    public boolean containsLang(String lang) {
        return lang2FormMap.containsKey(lang);
    }

    public void removeForm(String lang) {
        if (isEmpty()) {
            throw new IllegalArgumentException("This multitext is empty.");
        }
        if (!lang2FormMap.containsKey(lang)) {
            throw new IllegalArgumentException("No text in language: " + lang);
        }
        lang2FormMap.remove(lang);
        languageManager.removeLanguageOccurrence(lang);
    }

    public Set<String> getLangs() {
        if (isEmpty()) return EMPTY_LANG_SET;
        return lang2FormMap.keySet();
    }

    public void add(Form f) {
        // languageManager can be null in low-level operations
        // if (languageManager != null) {
        //   System.out.println(languageManager);
        // }
        if (languageManager != null) {
            if (!languageManager.hasLanguage(f.lang)) {
                throw new IllegalArgumentException(
                    "Language not registered in the dictionary: " + f.lang
                );
            }
            languageManager.addLanguageOccurrence(f.lang);
        }
        if (lang2FormMap.containsKey(f.lang)) throw new DuplicateLangException(
            "Duplicate lang: " + f.lang + "; form '" + f.toPlainText() + "'"
            + " already exists in this MultiText: '" + lang2FormMap.get(f.lang).toPlainText() + "'"
        );
        lang2FormMap.put(f.lang, f);
    }

    @Override
    public void addAnnotation(LiftAnnotation a) {
        annotations.add(a);
        a.setParent(this);
    }

    /**
     * JavaFX observable access to underlying forms map.
     */
    public MapProperty<String, Form> formsProperty() {
        return lang2FormMap;
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
            ObservableMap<String, Form> map = lang2FormMap.get();
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

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUUID() {
        return uuid;
    }

    /// With the fluent API, this method is called before any Form has been added.
    /// With the low-level API (for loading from XML), this method is called after
    // all Forms in the XML document have been added: the LiftFactoryNew take care of computing the numbers of occurrences.
    public void setLanguagesManager(LiftDictionaryLanguagesManager languageManager) {
        
        // can be set on null on purpose, when unregistering the parent component.
        if (languageManager == null) return;
        
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

    public void unregister() {
        for (String lang : lang2FormMap.keySet()) {
            languageManager.removeLanguageOccurrence(lang);
        }        
    }
}
