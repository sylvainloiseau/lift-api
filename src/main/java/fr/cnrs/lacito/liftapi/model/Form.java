package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.change.ModelProperty;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A transcription of linguistic content in a given language and with a given writting system.
 *
 * The transcription can be composed of attribute-holding chunks (see {@link TextSpan}).
 * 
 * The lang cannot be changed after creation. In order to change the lang, you have to create a new {@link Form}.
 *
 * This component can have {@link LiftAnnotation}s.
 */
public final class Form implements HasAnnotation {

    public static final Form EMPTY_FORM = new Form("");

    protected final String lang;
    private String toText = null;
    private String toPlainText = null;

    // Allocated on the first annotation, or the first call to getAnnotations(): there
    // are many Forms, and very few have annotations.
    private ObservableList<LiftAnnotation> annotations;
    private ObservableList<LiftAnnotation> annotationsView;

    /** The multitext holding this form, set by {@link MultiText#add(Form)}. */
    private MultiText owner;

    private final Deque<TextSpan> current = new ArrayDeque<>();
    private TextSpan root = new TextSpan();
    private int textSpanNumber = 0;

    // JavaFX properties (javafx.base only; no UI dependency)
    private final ReadOnlyStringWrapper langProperty;
    private final StringProperty textProperty;
    private boolean syncingFromProperty = false;
    private boolean syncingFromModel = false;

    public Form(String lang, String text) {
        if (lang == null) throw new IllegalArgumentException(
            "lang cannot be null"
        );
        if (lang.isEmpty()) throw new IllegalArgumentException(
            "lang cannot be empty"
        );
        if (text == null) throw new IllegalArgumentException(
            "text cannot be null"
        );
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

    public Form(String lang) {
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

    /**
     * Append a piece of text. For building a form (the XML reader), before it is added to
     * a multitext: this is not reported as a change. To edit a form, use
     * {@link #changeText(String)} or {@link #textProperty()}.
     */
    public void append(String string) {
        current.peek().addSpan(new TextSpan(string));
        textSpanNumber += 1;
    }

    /**
     * Open a nested span; see {@link #append(String)} and {@link #pop()}.
     */
    public void append(TextSpan span) {
        this.current.peek().addSpan(span);
        this.current.push(span);
        textSpanNumber += 1;
    }

    public void pop() {
        this.current.pop();
    }

    /**
     * Returns the innermost {@code TextSpan} currently open, which is the root
     * wrapper (no attributes, children are the actual content) whenever no
     * {@code <span>} is being built.
     *
     * While {@link #append(TextSpan)} is pushing spans, this is the span being filled,
     * not the root.
     */
    public TextSpan getTextSpanRoot() {
        return current.peek();
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
        if (toPlainText == null) {
            StringBuffer strb = new StringBuffer();
            current.peek().toPlainText(strb);
            toPlainText = strb.toString();
        }
        return toPlainText;
    }

    /**
     * The text of this form with its markup: what is inside the {@code <text>} element,
     * {@code <span>}s included. {@link #changeText(String)} parses it back into the same
     * spans, which is why it is the value reported for a change of text
     * ({@link ModelProperty#FORM_TEXT}).
     */
    public String getMarkup() {
        StringBuffer strb = new StringBuffer();
        TextSpan top = getTextSpanRoot();
        if (top.isTerminal()) {
            top.toString(strb);
        } else {
            for (TextSpan child : top.getInnerContent()) {
                child.toString(strb);
            }
        }
        return strb.toString();
    }

    /**
     * The multitext holding this form: set when the form is added to it, cleared when it
     * is removed. From there, {@link MultiText#getParent()} gives the component.
     *
     * For instance, a {@link fr.cnrs.lacito.liftapi.change.ValueChanged} of the text of a
     * form has the form as its owner: this is how to tell which text of which component
     * changed.
     *
     * @return the multitext, or {@code null} for a form that is in none
     */
    public MultiText getMultiText() {
        return owner;
    }

    void setOwner(MultiText owner) {
        this.owner = owner;
    }

    @Override
    public void addAnnotation(LiftAnnotation a) {
        addAnnotation(annotationsOrEmpty().size(), a);
    }

    /**
     * Add an annotation at a given position; it is registered when the component this
     * form belongs to is in a dictionary.
     */
    @Override
    public void addAnnotation(int index, LiftAnnotation a) {
        annotations().add(index, a);
        a.setParent(this);
        AbstractLiftRoot component = owner == null ? null : owner.getParent();
        if (component != null) {
            component.adoptedIn(this, a);
        }
    }

    /**
     * A {@code Form} is not an {@code AbstractLiftRoot}, so the annotations it holds are
     * never registered in a dictionary (see {@code DictionaryCensusTest}); this only has
     * a link to undo.
     */
    @Override
    public void deleteAnnotation(LiftAnnotation a) {
        if (a == null || annotationsOrEmpty().stream().noneMatch(x -> x == a)) {
            throw new IllegalArgumentException(
                "This annotation is not held by this " + getClass().getSimpleName() + "."
            );
        }
        AbstractLiftRoot component = owner == null ? null : owner.getParent();
        if (component != null) {
            component.removeChildIn(this, a);
        } else {
            a.unlink();
        }
    }

    @Override
    public void retypeAnnotation(LiftAnnotation a, Feature type) {
        if (a == null || annotationsOrEmpty().stream().noneMatch(x -> x == a)) {
            throw new IllegalArgumentException(
                "This annotation is not held by this " + getClass().getSimpleName() + "."
            );
        }
        a.assignType(type);
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
     * Whether this form has annotations; unlike {@link #getAnnotations()}, it does
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
        // The old text is only computed when somebody listens.
        String old = Changes.observing(this) == null ? null : getMarkup();
        // Réinitialise le contenu
        while (current.size() > 0) {
            current.pop();
        }
        root = new TextSpan();
        current.push(root);
        textSpanNumber = 1;
        toText = null;
        toPlainText = null;

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
        if (old != null) {
            Changes.valueChanged(this, ModelProperty.FORM_TEXT, old, getMarkup());
        }
    }

    /**
     * Parse the markup of a form's text ({@code <span>} elements, possibly nested, with
     * {@code lang}, {@code class} and {@code href} attributes) into {@code parent}.
     *
     * Written by hand rather than with a regular expression: a greedy pattern took
     * {@code <span>a</span>b<span>c</span>} as one span holding {@code a</span>b<span>c},
     * so that a text with two spans did not survive {@link #changeText(String)}.
     * A {@code </span>} with no opening tag, or an unterminated {@code <span}, is kept
     * as text.
     */
    private void parseSpanContent(String input, TextSpan parent) {
        if (input == null) {
            return;
        }
        int i = parseSpans(input, 0, parent, true);
        if (i < input.length()) {
            parent.addSpan(new TextSpan(input.substring(i)));
        }
    }

    /**
     * Parse spans and text from {@code i} into {@code parent}, up to the
     * {@code </span>} that closes it (or the end, for the root).
     *
     * @return the index after what was consumed
     */
    private static int parseSpans(String s, int i, TextSpan parent, boolean root) {
        StringBuilder text = new StringBuilder();
        while (i < s.length()) {
            if (startsSpan(s, i)) {
                int end = s.indexOf('>', i);
                if (end < 0) {
                    break;
                }
                flush(text, parent);
                TextSpan span = new TextSpan();
                Matcher attr = ATTRIBUTE.matcher(s.substring(i + 5, end));
                while (attr.find()) {
                    switch (attr.group(1)) {
                        case "lang" -> span.setLang(attr.group(2));
                        case "class" -> span.setsClass(attr.group(2));
                        case "href" -> span.setHref(attr.group(2));
                        default -> { }
                    }
                }
                i = parseSpans(s, end + 1, span, false);
                parent.addSpan(span);
            } else if (!root && s.startsWith("</span>", i)) {
                flush(text, parent);
                return i + "</span>".length();
            } else {
                text.append(s.charAt(i));
                i++;
            }
        }
        if (i < s.length()) {
            // An unterminated "<span": the rest is text.
            text.append(s, i, s.length());
            i = s.length();
        }
        flush(text, parent);
        return i;
    }

    private static final Pattern ATTRIBUTE = Pattern.compile("(lang|class|href)\\s*=\\s*\"([^\"]*)\"");

    private static boolean startsSpan(String s, int i) {
        if (!s.startsWith("<span", i)) {
            return false;
        }
        int next = i + 5;
        return next < s.length() && (s.charAt(next) == '>' || Character.isWhitespace(s.charAt(next)));
    }

    private static void flush(StringBuilder text, TextSpan parent) {
        if (!text.isEmpty()) {
            parent.addSpan(new TextSpan(text.toString()));
            text.setLength(0);
        }
    }
}
