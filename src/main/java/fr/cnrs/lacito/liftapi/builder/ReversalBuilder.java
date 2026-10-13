package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.HasReversal;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.LiftReversal;

/**
 * Builder for creating LiftReversal instances with a fluent API.
 */
public class ReversalBuilder extends AbstractLiftElementBuilder<LiftReversal, HasReversal> {

    protected ReversalBuilder(LiftDictionary dictionary, HasReversal parent) {
        super(new LiftReversal(null), dictionary, parent);
    }

    public ReversalBuilder withType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Reversal type cannot be null or blank");
        }
        // Create the type on demand, as this builder's own constructor already does.
        Feature f = dictionary
            .getHeader()
            .getInverseTypeManager()
            .getOrCreateFeature(type);
        super.withType(f);
        return this;
    }

    /**
     * Build the relation.
     */
    @Override
    public LiftReversal build() {
        // no: type is optional in the schema
        // if (element.getType() == null) {
        //     throw new IllegalStateException("Reversal must have a type");
        // }
        super.attach();
        return element;
    }

}
