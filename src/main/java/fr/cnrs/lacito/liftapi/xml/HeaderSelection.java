package fr.cnrs.lacito.liftapi.xml;

import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import java.util.Set;

/**
 * The part of a header an extract writes: these ranges, of their values these, and these
 * field and trait definitions. The sets compare by identity.
 */
record HeaderSelection(Set<FeatureSet> sets, Set<Feature> features, Set<LiftFieldAndTraitDefinition> definitions) {
}
