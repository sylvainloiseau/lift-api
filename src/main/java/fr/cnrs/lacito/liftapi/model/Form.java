package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Getter;

/**
 * A textual representation of linguistic material in a given language.
 * 
 * The text can have global annotation (see {@link LiftAnnotation}) and
 * can be composed of attribute-holding chunks (see {@link TextSpan}).
 */
public final class Form implements HasAnnotation {

    public final static Form EMPTY_FORM = new Form("");

    protected final String lang;
    private String toText = null;
    @Getter protected final List<LiftAnnotation> annotations = new ArrayList<>();
    private final Deque<TextSpan> current = new ArrayDeque<>();
    private TextSpan root = new TextSpan();
    private int textSpanNumber = 0;

    // JavaFX properties (javafx.base only; no UI dependency)
    private final ReadOnlyStringWrapper langProperty;
    private final StringProperty textProperty;
    private boolean syncingFromProperty = false;
    private boolean syncingFromModel = false;
    
    public Form(String lang, String text) {
        this.lang = lang;
        this.langProperty = new ReadOnlyStringWrapper(this, "lang", lang);
        this.textProperty = new SimpleStringProperty(this, "text", text);
        this.textProperty.addListener((obs, oldV, newV) -> {
            // Keep model in sync when bound from UI
            if (syncingFromModel) return;
            syncingFromProperty = true;
            try {
                changeText(newV == null ? "" : newV);
            } finally {
                syncingFromProperty = false;
            }
        });
        changeText(text);
    }

    protected Form(String lang) {
        this.lang = lang;
        this.langProperty = new ReadOnlyStringWrapper(this, "lang", lang);
        this.textProperty = new SimpleStringProperty(this, "text", "");
        this.textProperty.addListener((obs, oldV, newV) -> {
            if (syncingFromModel) return;
            syncingFromProperty = true;
            try {
                changeText(newV == null ? "" : newV);
            } finally {
                syncingFromProperty = false;
            }
        });
        current.push(root);
        textSpanNumber += 1;
    }

    public String getLang() {
        return lang;
    }

    public ReadOnlyStringProperty langProperty() {
        return langProperty.getReadOnlyProperty();
    }

    public StringProperty textProperty() {
        return textProperty;
    }

    public void append(String string) {
        current.peek().addSpan(new TextSpan(string));
        textSpanNumber += 1;
    }

    public void append(TextSpan span) {
        this.current.peek().addSpan(span);
        this.current.push(span);
        textSpanNumber += 1;
    }

    public void pop() {
        this.current.pop();
    }

    @Override
    public String toString() {
        if (toText == null) {
            StringBuffer strb = new StringBuffer();
            current.peek().toString(strb);
            toText = strb.toString();
        }
        return toText;
    }

    /**
     * Returns only the textual content, stripping all {@code <span>} markup.
     * Use this for UI display; use {@link #toString()} for serialization.
     */
    public String toPlainText() {
        StringBuffer strb = new StringBuffer();
        current.peek().toPlainText(strb);
        return strb.toString();
    }

    @Override
    public void addAnnotation(LiftAnnotation a) {
        annotations.add(a);
        a.setParent(this);
    }

    /**
     * Returns the list of text span (ordered in a depth-first traversal order)
     * associated with this text.
     * 
     * @return a list of text spans
     */
    public List<TextSpan> walkTextSpanTree() {
        List<TextSpan> result = new ArrayList<>(textSpanNumber);
        current.peek().walkTextSpanTree(result);
        return result;
    }

    public void changeText(String input) {
        // Réinitialise le contenu
        while (current.size() > 0) {
            current.pop();
        }
        root = new TextSpan();
        current.push(root);
        textSpanNumber = 1;
        toText = null;

        parseSpanContent(input, current.peek());

        // Keep observable in sync if changeText called directly by API users
        if (!syncingFromProperty && textProperty != null) {
            String normalized = input == null ? "" : input;
            if (!normalized.equals(textProperty.get())) {
                syncingFromModel = true;
                try {
                    textProperty.set(normalized);
                } finally {
                    syncingFromModel = false;
                }
            }
        }
    }

    private void parseSpanContent(String input, TextSpan parent) {
        Pattern spanPattern = Pattern.compile(
            "<span\\s+([^>]*)>(.*)</span>", Pattern.DOTALL);

        int idx = 0;
        while (idx < input.length()) {
            Matcher matcher = spanPattern.matcher(input.substring(idx));
            if (matcher.find() && matcher.start() == 0) {
                // Attributs de la balise
                String attrs = matcher.group(1);
                String content = matcher.group(2);

                String lang = null, clazz = null;
                Matcher attrMatcher = Pattern.compile("(lang|class)\\s*=\\s*\"([^\"]*)\"").matcher(attrs);
                while (attrMatcher.find()) {
                    if ("lang".equals(attrMatcher.group(1))) lang = attrMatcher.group(2);
                    if ("class".equals(attrMatcher.group(1))) clazz = attrMatcher.group(2);
                }
                TextSpan span = new TextSpan();
                if (lang != null) span.setLang(lang);
                if (clazz != null) span.setsClass(clazz);

                // Récursivité sur le contenu interne
                parseSpanContent(content, span);
                parent.addSpan(span);

                // Avance l'index après ce span
                idx += matcher.end();
            } else {
                // Texte brut jusqu'au prochain <span> ou fin
                int nextSpan = input.indexOf("<span", idx);
                String text;
                if (nextSpan == -1) {
                    text = input.substring(idx);
                    idx = input.length();
                } else {
                    text = input.substring(idx, nextSpan);
                    idx = nextSpan;
                }
                if (!text.isEmpty()) {
                    parent.addSpan(new TextSpan(text));
                }
            }
        }
    }
}
