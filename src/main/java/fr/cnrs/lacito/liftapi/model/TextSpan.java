package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A chunk of linguistic content, which can have a language, a class and a href specification.
 * 
 * Correspond to {@code &lt;span>} element in LIFT vocabulary.
 */
public final class TextSpan extends TextComponent {

    String toString = null;
    
    protected Optional<String> sLang = Optional.empty();
    protected Optional<String> sHref = Optional.empty();
    protected Optional<String> sClass = Optional.empty();
    protected List<TextSpan> innerContent = new ArrayList<>();
    String terminalOrNull = null;

    protected TextSpan() {
    }

    protected TextSpan(String s) {
        this.setString(s);
    }

    public void setLang(String lang) {
        this.sLang = Optional.of(lang);
    }

    public Optional<String> getLang() {
        return sLang;
    }

    public void setHref(String href) {
        this.sHref = Optional.of(href);
    }

    public Optional<String> getHref() {
        return sHref;
    }

    public void setsClass(String sClass) {
        this.sClass = Optional.of(sClass);
    }

    public Optional<String> getSClass() {
        return sClass;
    }

    protected boolean isTerminal() {
        return terminalOrNull != null;
    }

    protected void setString(String s) {
        if (innerContent.size() != 0) throw new IllegalStateException("Cannot set a string to a TextSpan if if has an inner content");
        terminalOrNull = s;
    }

    protected void addSpan(TextSpan ts) {
        if (isTerminal()) throw new IllegalStateException("Cannot contains TextSpan if it is a terminal string node.");
        innerContent.add(ts);
    }

    public String toString() {
        if (toString == null) {
            StringBuffer strb = new StringBuffer();
            toString(strb);
            toString = strb.toString();
        }
        return toString;
    }

    public void toString(StringBuffer strb) {
        if (isTerminal()) {
            strb.append(this.terminalOrNull);
        } else {
            strb.append("<span");
            if (sLang.isPresent()) {
                strb.append(" lang=\"").append(sLang.get()).append("\"");
            }
            if (sClass.isPresent()) {
                strb.append(" class=\"").append(sClass.get()).append("\"");
            }
            strb.append(">");
            for (TextSpan ts : innerContent) {
                ts.toString(strb);
            }
            strb.append("</span>");
        }
    }

    /**
     * Returns only the textual content, stripping all {@code <span>} markup.
     */
    public String toPlainText() {
        StringBuffer strb = new StringBuffer();
        toPlainText(strb);
        return strb.toString();
    }

    public void toPlainText(StringBuffer strb) {
        if (isTerminal()) {
            strb.append(this.terminalOrNull);
        } else {
            for (TextSpan ts : innerContent) {
                ts.toPlainText(strb);
            }
        }
    }

    public void walkTextSpanTree(List<TextSpan> result) {
        result.add(this);
        if (!isTerminal()) {
            for (TextSpan ts : innerContent) {
                ts.walkTextSpanTree(result);
            }
        }
    }

}
