package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
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
public final class MultiText {

    protected final MapProperty<String, Form> formsProperty =
            new SimpleMapProperty<>(FXCollections.observableHashMap());
    protected final static Set<String> EMPTY_LANG_SET = Collections.unmodifiableSet(new HashSet<>());
    protected List<LiftAnnotation> annotations = new ArrayList<>();
    private final ConcurrentHashMap<String, StringProperty> formTextProperties = new ConcurrentHashMap<>();

    protected MultiText() {
    }

    public boolean isEmpty() {
        return formsProperty.isEmpty();
    }

    public Optional<Form> getForm(String lang) {
        if (lang == null) return Optional.empty();
        return Optional.ofNullable(formsProperty.get(lang));
    }

    public Collection<Form> getForms() {
        return formsProperty.values();
    }

    public void removeForm(String lang) {
        if (isEmpty()) {
            throw new IllegalArgumentException("This multitext is empty.");
        }
        if (!formsProperty.containsKey(lang)) {
            throw new IllegalArgumentException("No text in language: " + lang);
        }
        formsProperty.remove(lang);
    }

    public Set<String> getLangs() {
        if (isEmpty()) return EMPTY_LANG_SET;
        return formsProperty.keySet();
    }

    public void add(Form f) {
        String lang = f.lang;
        if (formsProperty.containsKey(lang)) throw new DuplicateLangException("Duplicate lang: " + lang);
        formsProperty.put(lang, f);
    }

    /**
     * JavaFX observable access to underlying forms map.
     */
    public MapProperty<String, Form> formsProperty() {
        return formsProperty;
    }

    /**
     * A bidirectional JavaFX property for the text of a given language form.
     * Setting it updates the underlying {@link MultiText} by adding/updating/removing the {@link Form}.
     */
    public StringProperty formTextProperty(String lang) {
        if (lang == null) return new SimpleStringProperty("");
        final String key = lang.trim();
        if (key.isEmpty()) return new SimpleStringProperty("");

        return formTextProperties.computeIfAbsent(key, l -> {
            SimpleStringProperty p = new SimpleStringProperty(this, "formText[" + l + "]", getForm(l).map(Form::toPlainText).orElse(""));
            AtomicBoolean syncing = new AtomicBoolean(false);
            AtomicReference<Form> currentFormRef = new AtomicReference<>(getForm(l).orElse(null));
            AtomicReference<ChangeListener<String>> formTextListenerRef = new AtomicReference<>(null);

            Runnable attachFormListener = () -> {
                Form current = currentFormRef.get();
                ChangeListener<String> oldListener = formTextListenerRef.get();
                if (oldListener != null && current != null) {
                    try {
                        current.textProperty().removeListener(oldListener);
                    } catch (Exception ignored) {
                    }
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
                    if (!newText.equals(p.get())) {
                        syncing.set(true);
                        try {
                            p.set(newText);
                        } finally {
                            syncing.set(false);
                        }
                    }
                };
                formTextListenerRef.set(listener);
                next.textProperty().addListener(listener);
            };

            // When property changes -> update model
            p.addListener((obs, oldV, newV) -> {
                if (syncing.get()) return;
                syncing.set(true);
                try {
                String v = newV == null ? "" : newV;
                if (v.isBlank()) {
                    if (getForm(l).isPresent()) {
                        try {
                            removeForm(l);
                        } catch (Exception ignored) {
                        }
                    }
                } else {
                    getForm(l).ifPresentOrElse(existing -> existing.changeText(v), () -> {
                        try {
                            add(new Form(l, v));
                        } catch (Exception ignored) {
                        }
                    });
                }
                } finally {
                    syncing.set(false);
                }
            });

            // When map changes (add/remove/replace) -> update property value
            ObservableMap<String, Form> map = formsProperty.get();
            MapChangeListener<String, Form> listener = change -> {
                if (!l.equals(change.getKey())) return;
                String newText = Optional.ofNullable(formsProperty.get(l)).map(Form::toPlainText).orElse("");
                if (!newText.equals(p.get())) {
                    if (syncing.get()) return;
                    syncing.set(true);
                    try {
                        p.set(newText);
                    } finally {
                        syncing.set(false);
                    }
                }
                attachFormListener.run();
            };
            map.addListener(listener);
            attachFormListener.run();
            return p;
        });
    }
}
