package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


/**
 * A component containing a reference to part of speech together with {@link LiftTrait}, to be registered to a {@link LiftSense}.
 *
 * <pre>
 * &lt;grammatical-info value="Noun"&gt;
 * &lt;trait name="Noun-infl-class" value="fo"/&gt;
 * &lt;/grammatical-info&gt;
 * </pre>
 *
 * The part of speech value is a reference towards one
 * of the {@link Feature}; the list of part of speech value is managed through
 * the {@link LiftHeader#getGrammaticalInfoManager()}.
 *
 * Like every other component, a {@code GrammaticalInfo} is an {@link AbstractLiftRoot}
 * and is registered in the dictionary. It has to be: it owns {@link LiftTrait}s, and
 * a component that is not part of the traversal takes its children out of the
 * dictionary with it. While this class sat outside the hierarchy, the traits carried by
 * a {@code <grammatical-info>} were silently dropped on load.
 */
public final class GrammaticalInfo
    extends AbstractLiftRoot
    implements HasTrait {

    private final TrackedProperty<Feature> value;

    final ObservableList<LiftTrait> traits = FXCollections.observableArrayList();
    private ObservableList<LiftTrait> traitsView;

    private LiftSense parent;

    protected GrammaticalInfo(Feature v) {
        this.value = new TrackedProperty<>(this, ModelProperty.GRAMMATICAL_INFO_VALUE, v);
    }

    /**
     * Create a grammatical information for the given part of speech.
     *
     * This only allocates the component: it is detached, and stays inert until it is
     * wired to a sense that belongs to a dictionary.
     *
     * @param value the part of speech
     * @return the new component, attached to nothing
     */
    public static GrammaticalInfo create(Feature value) {
        return new GrammaticalInfo(value);
    }

    public Feature getGramInfoValue() {
        return value.get();
    }

    /**
     * Change the part of speech of this grammatical info.
     *
     * @param v the new value
     */
    public void setGramInfoValue(Feature v) {
        value.set(v);
    }

    /** The part of speech, read-only: it changes through {@link #setGramInfoValue}. */
    public ReadOnlyObjectProperty<Feature> gramInfoValueProperty() {
        return value.getReadOnlyProperty();
    }

    /**
     * The sense this grammatical information belongs to.
     *
     * @return the parent sense, or {@code null} once detached
     */
    public LiftSense getParent() {
        return parent;
    }

    /**
     * Protected: this method is called by the parent when it adopts this annotation.
     * 
     * @param parent the sense this belongs to, or {@code null} when detaching
     *        (see {@link AbstractLiftRoot#detach()})
     */
    protected void setParent(LiftSense parent) {
        this.parent = parent;
    }

    @Override
    public void addTrait(LiftTrait t) {
        addTrait(traits.size(), t);
    }

    @Override
    public void addTrait(int index, LiftTrait t) {
        traits.add(index, t);
        t.setParent(this);
        adopted(t);
    }

    @Override
    public ObservableList<LiftTrait> getTraits() {
        if (traitsView == null) traitsView = FXCollections.unmodifiableObservableList(traits);
        return traitsView;
    }

    // --------------------------------------------------------
    // Text
    // --------------------------------------------------------

    /**
     * GrammaticalInfo has no MultiText: calling this throws.
     *
     * @throws IllegalStateException always
     */
    @Override
    public MultiText getMainMultiText() {
        throw new IllegalStateException(
            "GrammaticalInfo does not have a main MultiText"
        );
    }

    /**
     * GrammaticalInfo has no MultiText: calling this throws.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    protected void addToMainMultiText(Form t) {
        throw new UnsupportedOperationException(
            "GrammaticalInfo does not have a main MultiText"
        );
    }

    @Override
    public AbstractLiftRoot getParentNode() {
        return parent;
    }

    // --------------------------------------------------------
    // Deleting sub-components
    // --------------------------------------------------------

    /**
     * Remove a trait from this grammatical information, unregistering it if this
     * component belongs to a dictionary.
     *
     * @param t a trait of this grammatical information
     */
    @Override
    public void deleteTrait(LiftTrait t) {
        requireChild(t, traits.contains(t));
        removeChild(t);
    }
}
