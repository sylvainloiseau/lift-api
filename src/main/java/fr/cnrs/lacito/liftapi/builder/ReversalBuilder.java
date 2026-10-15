package fr.cnrs.lacito.liftapi.builder;

import java.util.function.Consumer;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasReversal;
import fr.cnrs.lacito.liftapi.model.LiftReversal;

/**
 * Builder for creating LiftReversal instances with a fluent API.
 *
 * Usage:
 * <pre>
 *   dictionary.getComponentBuilder()
 *       .reversal(sense)
 *       .withType("en")
 *       .withForm("en", "house")
 *       .withMain(m -> m.withForm("en", "building"))
 *       .build();
 * </pre>
 *
 * A reversal goes into one of two slots of its parent: the list of reversals of a sense
 * (or of a reversal), or the single {@code <main>} of a reversal. Both hold a
 * {@link LiftReversal}, so the type of the component cannot tell them apart; a builder
 * made by {@link #withMain(Consumer)} or
 * {@link DictionaryComponentBuilderFactory#reversalMain(LiftReversal)} goes into the
 * {@code <main>} slot, any other into the list.
 */
public class ReversalBuilder extends AbstractLiftElementBuilder<LiftReversal, HasReversal> {

    /** Whether the reversal built is the {@code <main>} of its parent. */
    private final boolean main;

    protected ReversalBuilder(LiftDictionary dictionary, HasReversal parent) {
        this(dictionary, parent, false);
    }

    private ReversalBuilder(LiftDictionary dictionary, HasReversal parent, boolean main) {
        super(new LiftReversal(null), dictionary, parent);
        this.main = main;
    }

    /**
     * A builder for the {@code <main>} of {@code parent}.
     */
    static ReversalBuilder main(LiftDictionary dictionary, LiftReversal parent) {
        if (parent == null) {
            throw new IllegalArgumentException("The main of a reversal needs a reversal");
        }
        return new ReversalBuilder(dictionary, parent, true);
    }

    public ReversalBuilder withType(String type) {
        // An empty type is a valid attribute value.
        if (type == null) {
            throw new IllegalArgumentException("Reversal type cannot be null");
        }
        // Create the type on demand.
        Feature f = dictionary
            .getHeader()
            .getInverseTypeManager()
            .getOrCreateFeature(type);
        super.withType(f);
        return this;
    }

    /**
     * Add a form in the specified language.
     */
    public ReversalBuilder withForm(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
        }
        element.getForms().add(new Form(language, text));
        return this;
    }

    /**
     * Add a form.
     */
    public ReversalBuilder withForm(Form form) {
        if (form == null) {
            throw new IllegalArgumentException("Form cannot be null");
        }
        element.getForms().add(form);
        return this;
    }

    /**
     * Set the {@code <main>} of this reversal via nested builder configuration.
     */
    public ReversalBuilder withMain(Consumer<ReversalBuilder> config) {
        ReversalBuilder mb = main(dictionary, element);
        config.accept(mb);
        mb.build();
        return this;
    }

    /**
     * Build the reversal, adding it to its parent's list of reversals or, for a main,
     * setting it as its parent's {@code <main>} (in place of the current one).
     */
    @Override
    public LiftReversal build() {
        // no type check: the type is optional in the schema
        if (main) {
            attach(() -> dictionary.getMutator().attachMain(element, (LiftReversal) parent));
        } else {
            super.attach();
        }
        return element;
    }

}
