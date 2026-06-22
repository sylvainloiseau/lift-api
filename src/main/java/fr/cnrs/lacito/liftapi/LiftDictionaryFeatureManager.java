package fr.cnrs.lacito.liftapi;

import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionTarget;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import javafx.collections.ListChangeListener;
import lombok.val;

/**
 * The {@LiftDictionaryFeatureManager} keeps track of the number of
 *  annotations/field/trait name, value, according to their host
 * (entry, sense) that are used in the dictionary.
 */
public class LiftDictionaryFeatureManager {

    //public record FieldSpec (LiftFieldAndTraitDefinitionTarget host, String name) {}

    private final Map<String, Integer> annotationNameCount = new HashMap<>();
    private final Map<String, Set<String>> traitValue = new HashMap<>();

    private final Map<LiftFieldAndTraitDefinitionTarget, Set<String>> fields =
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

    public LiftDictionaryFeatureManager(
        LiftDictionaryRegistry liftDictionaryRegistry
    ) {
        this.liftDictionaryRegistry = liftDictionaryRegistry;
        initTraitValue();
        initAnnotationNameCount();
    }

    public void attachTargetOnField(
        String fieldName,
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
        for (LiftField f : liftDictionaryRegistry.getFieldsReadOnly()) {
            LiftFieldAndTraitDefinitionTarget key =
                LiftFieldAndTraitDefinitionTarget.fromType(f.getParent());
            fields.compute(key, (k, v) -> {
                if (v == null) {
                    v = new TreeSet<>();
                }
                v.add(f.getName());
                return v;
            });
        }
    }

    public Set<String> getFieldNameForTarget(
        LiftFieldAndTraitDefinitionTarget k
    ) {
        return fields.get(k);
    }

    // getKnownTraitValues
    // TODO bug: when removing a trait, it remove its value altooghtehe
    private void initTraitValue() {
        for (LiftTrait trait : this.liftDictionaryRegistry.getTraitsReadOnly()) {
            traitValue.compute(
                trait.getName(),
                key2SetUpdater(trait.getValue())
            );
        }

        // no we are not discovering values on the fly.
        this.liftDictionaryRegistry.getTraitsReadOnly().addListener(
            new ListChangeListener<LiftTrait>() {
                @Override
                public void onChanged(Change<? extends LiftTrait> change) {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            for (LiftTrait trait : change.getAddedSubList()) {
                                traitValue.compute(
                                    trait.getName(),
                                    key2SetUpdater(trait.getValue())
                                );
                            }
                        }
                        if (change.wasRemoved()) {
                            for (LiftTrait trait : change.getRemoved()) {
                                if (traitValue.containsKey(trait.getName())) {
                                    Set<String> values = traitValue.get(
                                        trait.getName()
                                    );
                                    values.remove(trait.getValue());
                                    if (values.isEmpty()) {
                                        traitValue.remove(trait.getName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        );
    }

    // substitute for getKnownAnnotationNames
    private void initAnnotationNameCount() {
        for (LiftAnnotation annotation : this.liftDictionaryRegistry.getAnnotationsReadOnly()) {
            annotationNameCount.put(
                annotation.getName(),
                annotationNameCount.getOrDefault(annotation.getName(), 0) + 1
            );
        }
        this.liftDictionaryRegistry.getAnnotationsReadOnly().addListener(
            new ListChangeListener<LiftAnnotation>() {
                @Override
                public void onChanged(Change<? extends LiftAnnotation> change) {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            for (LiftAnnotation annotation : change.getAddedSubList()) {
                                annotationNameCount.put(
                                    annotation.getName(),
                                    annotationNameCount.getOrDefault(
                                        annotation.getName(),
                                        0
                                    ) + 1
                                );
                            }
                        }
                        if (change.wasRemoved()) {
                            for (LiftAnnotation annotation : change.getRemoved()) {
                                if (
                                    annotationNameCount.containsKey(
                                        annotation.getName()
                                    )
                                ) {
                                    int count = annotationNameCount.get(
                                        annotation.getName()
                                    );
                                    if (count == 1) {
                                        annotationNameCount.remove(
                                            annotation.getName()
                                        );
                                    } else {
                                        annotationNameCount.put(
                                            annotation.getName(),
                                            count - 1
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        );
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
