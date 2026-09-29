package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

public final class LiftEtymology
    extends AbstractExtensibleWithField
    implements HasGlosses, HasType
{

    protected final MultiText glosses = new MultiText(this);

    protected LiftEntry parent;

    private final ReadOnlyObjectWrapper<Feature> typeProperty;

    private final ReadOnlyStringWrapper sourcePropertyWrapper;

    public LiftEtymology(Feature type, String source) {
        this.typeProperty = new ReadOnlyObjectWrapper<>(
            this,
            "type",
            type
        );
        this.sourcePropertyWrapper = new ReadOnlyStringWrapper(
            this,
            "source",
            source
        );
    }

    // Parent -----------------------------------

    public LiftEntry getParent() {
        return parent;
    }

    /**
     * Protected: this method is called by the parent when it adopts this etymology.
     * 
     * @param parent
     */
    protected void setParent(LiftEntry parent) {
        this.parent = parent;
    }

    // Type -----------------------------------

    /**
     * <cite>
     * Gives the etymological relationship between this sense and
     * some other word in another language. This is a reference to a range-element in
     * the etymology range.
     * </cite>
     * 
     * Lift 0.15 specification, p. 9.
     */
    @Override
    public Feature getType() {
        return typeProperty.get();
    }

    /**
     * Change the type of this etymology, through
     * {@link LiftEntry#retypeEtymology(LiftEtymology, Feature)} when it belongs to an
     * entry.
     */
    @Override
    public void setType(Feature type) {
        if (parent != null) {
            parent.retypeEtymology(this, type);
        } else {
            assignType(type);
        }
    }

    /**
     * Write the type. Called by {@link #setType(Feature)} when this component has no
     * parent, and otherwise by the parent's {@code retype} method once it has checked
     * and re-keyed what depends on the type.
     */
    void assignType(Feature type) {
        // TODO should check that the type belong to the etymology-type range
        this.typeProperty.set(type);
    }

    @Override
    public ReadOnlyObjectProperty<Feature> typeProperty() {
        return typeProperty.getReadOnlyProperty();
    }

    // Source -----------------------------------
    
    /**
     * <cite>
     * Gives the language for the source language of the
     * etymological relation. Where possible a lang type code (RFC 5646) should be
     * used, but proto languages tend not to appear in the Ethnologue and so a
     * uniquely identifying name may be given here.
     * </cite>
     * 
     * Lift 0.15 specification, p. 9.
     * 
     * @return
     */
    public String getSource() {
        return sourcePropertyWrapper.get();
    }

    public void setSource(String source) {
        sourcePropertyWrapper.set(source);
    }

    public ReadOnlyStringProperty sourceProperty() {
        return sourcePropertyWrapper.getReadOnlyProperty();
    }

    // Form -----------------------------------

    public void addForm(Form form) {
        addToMainMultiText(form);
    }

    public MultiText getForms() {
        return getMainMultiText();
    }

    // Gloss -----------------------------------

    @Override
    public void addGloss(Form gloss) {
        glosses.add(gloss);
    }

    /**
     * <cite>Gives glosses of the word that the etymological relationship is with.</cite>
     * 
     * Lift 0.15 specification, p. 9.
     * 
     */
    @Override
    public MultiText getGlosses() {
        return glosses;
    }

    public static LiftEtymology create(Feature type, String source) {
        return new LiftEtymology(type, source);
    }

    @Override
    public AbstractLiftRoot getParentNode() {
        return parent;
    }
}
