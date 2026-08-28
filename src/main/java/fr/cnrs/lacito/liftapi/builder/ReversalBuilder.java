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
            throw new IllegalArgumentException("Annotation name cannot be null or blank");
        }
        // if (!dictionary.getHeader().getInverseTypeManager().hasRangeElements(type)) {
        //     dictionary.getHeader().getInverseTypeManager().addFeature(type);
        // }
        Feature e = dictionary.getHeader().getInverseTypeManager().getFeature(type);
        super.withType(e);
        return this;
    }

    /**
     * Build the relation.
     */
    @Override
    public LiftReversal build() {
        if (element.getType() == null) {
            throw new IllegalStateException("Relation must have a type");
        }
        super.register();
        return element;
    }

}
