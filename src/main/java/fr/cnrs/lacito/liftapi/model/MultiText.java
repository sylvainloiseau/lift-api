package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
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
public sealed class MultiText
    implements HasAnnotation
    permits MultiTextMetaLanguage, MultiTextObjectLanguage
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

    public MultiText() {}

    public MultiText(LiftDictionaryRegistry registry) {
        this.registry = registry;
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
    }

    public Set<String> getLangs() {
        if (isEmpty()) return EMPTY_LANG_SET;
        return lang2FormMap.keySet();
    }

    public void add(Form f) {
        String lang = f.lang;
        if (lang2FormMap.containsKey(lang)) throw new DuplicateLangException(
            "Duplicate lang: " + lang
        );
        lang2FormMap.put(lang, f);
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
}
