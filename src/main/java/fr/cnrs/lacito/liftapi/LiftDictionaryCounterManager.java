package fr.cnrs.lacito.liftapi;

import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionTarget;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionDataModel;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;

/**
 * The {@link LiftDictionaryCounterManager} keeps track of the number of
 *  annotations/field/trait name, value, according to their host
 * (entry, sense) that are used in the dictionary.
 */
public class LiftDictionaryCounterManager {

    Map<
      String,
      Map<String, Long>
    > featureSetCounter = new HashMap<>();

    /**
     * Create a observable map of the number of occurrences of each features of a feature set,
     * whatever the traits it is used on.
     * 
     * @param featureSetName
     * @return
     */
    public Map<String, Long> getFeatureSetCounter(String featureSetName) {
        if (featureSetCounter.containsKey(featureSetName)) {
            return featureSetCounter.get(featureSetName);
        }

        if (!dictionary.getHeader().hasFeatureSet(featureSetName)) {
            throw new IllegalArgumentException("No feature set with name: " + featureSetName);
        }

        Map<String, Long> featureCounter = dictionary.getLiftDictionaryRegistry().getTraits().stream()
            .filter(
                trait -> trait.getSpecification().getDataModel().isPresent() &&
                (
                trait.getSpecification().getDataModel().get() == LiftFieldAndTraitDefinitionDataModel.FEATURE
                ||
                trait.getSpecification().getDataModel().get() == LiftFieldAndTraitDefinitionDataModel.FEATURE_SET
                ||
                trait.getSpecification().getDataModel().get() == LiftFieldAndTraitDefinitionDataModel.FEATURE_LIST
                )
            )
            .filter(trait -> trait.getSpecification().getResolvedFeatureSet().isPresent())
            .filter(trait -> trait.getSpecification().getResolvedFeatureSet().get().getId().equals(featureSetName))
            .flatMap(trait -> {
                return switch(trait.getSpecification().getDataModel().get()) {
                    case FEATURE -> Stream.<Feature>of(trait.featureValueProperty().get());
                    case FEATURE_SET -> trait.featureSetValueProperty().get().stream();
                    case FEATURE_LIST -> trait.featureListValueProperty().get().stream();
                    default ->
                        throw new IllegalStateException("Unexpected data model: " + trait.getSpecification().getDataModel().get());
                };
            })
            .map(x -> x.getId())
            .collect(Collectors.groupingBy(x -> x, Collectors.counting()));;

        featureSetCounter.put(featureSetName, featureCounter);
        return featureSetCounter.get(featureSetName);
    }




    //public record FieldSpec (LiftFieldAndTraitDefinitionTarget host, String name) {}

    /**
     * Number of annotations of each type in the dictionary.
     *
     * Keyed by the {@link Feature} itself: a feature compares by identity, so renaming
     * it ({@code FeatureSet.changeFeatureId}) does not split or strand its count. Kept
     * up to date by a listener on the registry's annotations (added / removed) and by
     * {@link #annotationRetyped} on each annotation's type (changed).
     */
    private final Map<Feature, Integer> annotationTypeCount = new HashMap<>();

    /**
     * One listener, shared by every annotation in the dictionary: moves one count from
     * the old type to the new one when an annotation's type changes.
     */
    private final ChangeListener<Feature> annotationRetyped = (obs, oldType, newType) -> {
        decrementAnnotationType(oldType);
        incrementAnnotationType(newType);
    };
    private final Map<String, Set<String>> traitValue = new HashMap<>();

    private final Map<LiftFieldAndTraitDefinitionTarget, Set<LiftFieldAndTraitDefinition>> fields =
        new HashMap<>();

    private final Map<
        LiftFieldAndTraitDefinitionTarget,
        Map<String, Set<String>>
    > traits = new HashMap<>();

    private final Map<
        LiftFieldAndTraitDefinitionTarget,
        Map<String, Set<String>>
    > annotations = new HashMap<>();

    private final LiftDictionaryRegistry liftDictionaryRegistry;
    private LiftDictionary dictionary;

    public LiftDictionaryCounterManager(
        LiftDictionary dictionary
    ) {
        this.dictionary = dictionary;
        this.liftDictionaryRegistry = dictionary.getLiftDictionaryRegistry();
        initTraitValue();
        initAnnotationNameCount();
    }

    public void attachTargetOnField(
        LiftFieldAndTraitDefinition fieldName,
        LiftFieldAndTraitDefinitionTarget target
    ) {
        fields.compute(target, (k, v) -> {
            if (v == null) {
                v = new TreeSet<>();
            }
            v.add(fieldName);
            return v;
        });
    }

    public void discover() {
        discoverFields();
    }

    private void discoverFields() {
        for (LiftField f : liftDictionaryRegistry.getFields()) {
            LiftFieldAndTraitDefinitionTarget key =
                LiftFieldAndTraitDefinitionTarget.fromType(f.getParent());
            fields.compute(key, (k, v) -> {
                if (v == null) {
                    v = new TreeSet<LiftFieldAndTraitDefinition>();
                }
                v.add(f.getSpecification());
                return v;
            });
        }
    }

    public Set<LiftFieldAndTraitDefinition> getFieldNameForTarget(
        LiftFieldAndTraitDefinitionTarget k
    ) {
        return fields.get(k);
    }

    // getKnownTraitValues
    // TODO bug: when removing a trait, it remove its value
    private void initTraitValue() {
        for (LiftTrait trait : this.liftDictionaryRegistry.getTraits()) {
            traitValue.compute(
                trait.getSpecification().getName(),
                key2SetUpdater(trait.getValue())
            );
        }

        // no we are not discovering values on the fly.
        this.liftDictionaryRegistry.getTraits().addListener(
            new ListChangeListener<LiftTrait>() {
                @Override
                public void onChanged(Change<? extends LiftTrait> change) {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            for (LiftTrait trait : change.getAddedSubList()) {
                                traitValue.compute(
                                    trait.getSpecification().getName(),
                                    key2SetUpdater(trait.getValue())
                                );
                            }
                        }
                        if (change.wasRemoved()) {
                            for (LiftTrait trait : change.getRemoved()) {
                                if (traitValue.containsKey(trait.getSpecification().getName())) {
                                    Set<String> values = traitValue.get(
                                        trait.getSpecification().getName()
                                    );
                                    values.remove(trait.getValue());
                                    if (values.isEmpty()) {
                                        traitValue.remove(trait.getSpecification().getName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        );
    }

    /**
     * The number of annotations of each type in the dictionary.
     *
     * Annotations held by a {@code Form} are not registered in the dictionary and are
     * therefore not counted.
     *
     * @return an unmodifiable, live view; annotations without a type are not counted
     */
    public Map<Feature, Integer> getAnnotationTypeCount() {
        return Collections.unmodifiableMap(annotationTypeCount);
    }

    private void initAnnotationNameCount() {
        for (LiftAnnotation annotation : this.liftDictionaryRegistry.getAnnotations()) {
            startCountingAnnotation(annotation);
        }
        this.liftDictionaryRegistry.getAnnotations().addListener(
            new ListChangeListener<LiftAnnotation>() {
                @Override
                public void onChanged(Change<? extends LiftAnnotation> change) {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            for (LiftAnnotation annotation : change.getAddedSubList()) {
                                startCountingAnnotation(annotation);
                            }
                        }
                        if (change.wasRemoved()) {
                            for (LiftAnnotation annotation : change.getRemoved()) {
                                stopCountingAnnotation(annotation);
                            }
                        }
                    }
                }
            }
        );
    }

    private void startCountingAnnotation(LiftAnnotation annotation) {
        incrementAnnotationType(annotation.getType());
        annotation.typeProperty().addListener(annotationRetyped);
    }

    private void stopCountingAnnotation(LiftAnnotation annotation) {
        annotation.typeProperty().removeListener(annotationRetyped);
        decrementAnnotationType(annotation.getType());
    }

    private void incrementAnnotationType(Feature type) {
        if (type != null) {
            annotationTypeCount.merge(type, 1, Integer::sum);
        }
    }

    private void decrementAnnotationType(Feature type) {
        if (type != null) {
            // Remove the entry when the count reaches zero, so the map only lists
            // types actually in use.
            annotationTypeCount.computeIfPresent(type, (k, n) -> n == 1 ? null : n - 1);
        }
    }

    private BiFunction<
        ? super String,
        ? super Set<String>,
        ? extends Set<String>
    > key2SetUpdater(String value) {
        return (key, currentMap) -> {
            if (currentMap == null) {
                currentMap = new HashSet<String>();
            }
            currentMap.add(value);
            return currentMap;
        };
    }
}
