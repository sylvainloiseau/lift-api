package fr.cnrs.lacito.liftapi.internal;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.UUID;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryLanguagesManager;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.FieldSpecification;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.MultiText;

/**
 * The internal wiring of the model, for the code of this module that sits outside the
 * {@code model} package (the mutator, the XML reader, the dictionary).
 *
 * These operations used to be public methods of the model ({@code setUUID},
 * {@code setOwningDictionary}, {@code setLanguagesManager}...), callable by any consumer,
 * with a "do not call" comment. They are now package-private in {@code model}, and the
 * model hands this module an {@link Access} to them when it is initialized. This package
 * is not exported, so consumers cannot reach it.
 *
 * <b>Not public API.</b>
 */
public final class ModelInternals {

    /**
     * The package-private operations of the model, implemented in {@code model}.
     */
    public interface Access {

        void setUUID(AbstractLiftRoot node, UUID uuid);

        void setUUID(MultiText text, UUID uuid);

        void setOwningDictionary(LiftEntry entry, LiftDictionary dictionary);

        void setLanguagesManager(MultiText text, LiftDictionaryLanguagesManager manager);

        void unregister(MultiText text);

        void putOtherXmlAttribute(AbstractLiftRoot node, String name, String value);

        void setDictionary(LiftHeader header, LiftDictionary dictionary);

        /**
         * Sets the specification of a field without the header's checks: how a
         * {@code FIELD_SPECIFICATION} change is reverted and reapplied.
         */
        void setFieldSpecification(LiftField field, FieldSpecification specification);

        /**
         * Runs {@code operation} on the header without rebinding the traits and fields
         * it affects: how the header's changes are reverted and reapplied, since the
         * rebinding they caused was reported after them, and is reverted and reapplied
         * by its own changes.
         */
        void withoutRebinding(LiftHeader header, Runnable operation);

        /**
         * Suspends rebinding while the reader reads the header: the {@code option-range}
         * of the definitions is set only once all the ranges are read.
         */
        void suspendRebinding(LiftHeader header);

        /**
         * Resumes rebinding at the end of the header, and rebinds every trait and field
         * the header's components hold to what its name resolves to.
         */
        void resumeRebinding(LiftHeader header);

        /**
         * Runs {@code operation} without refusing the changes of feature sets defined in
         * an external ranges file: for the reader, which fills them, and for the changes
         * that are reverted or reapplied, which put back a state that was valid.
         */
        void withoutDefinitionGuard(Runnable operation);

        /**
         * Sets the {@code href} of a feature set: by the reader, once the set holds the
         * content of its external file, and by the revert of
         * {@code LiftHeader.makeFeatureSetInternal}.
         */
        void setHref(FeatureSet set, String href);

        /**
         * Records the ranges file the {@code href} of a feature set was resolved to on load
         * ({@code null} if none was found), and whether the range was missing from it.
         */
        void setExternalDefinition(FeatureSet set, Path file, boolean missing);

        /**
         * Records that the {@code .lift} file declared {@code part} inline in the
         * {@code <range>} of an externally defined set: a feature, a text of the set, or a
         * trait, field or annotation of the set.
         */
        void markDeclaredInline(FeatureSet set, Object part);

        /**
         * The feature of that id, or a new bare one - even in a set defined in an external
         * file: a component uses a value its range does not declare. For the reader and the
         * builders.
         */
        Feature getOrCreateUndeclaredFeature(FeatureSet set, String id);

        /**
         * Sets whether the range of a feature declares it: by the reader, which declares a
         * bare feature when its {@code <range-element>} comes, and by the revert of
         * {@code LiftHeader.declareFeature}.
         */
        void setDeclared(Feature feature, boolean declared);

        /**
         * Sets whether the file declares a feature set: by the reader, which declares a
         * built-in set when its {@code <range>} comes, and by the revert of
         * {@code LiftHeader.declareFeatureSet}.
         */
        void setDeclared(FeatureSet set, boolean declared);

        /**
         * Called with each bare, undeclared feature created in the header's sets, or
         * {@code null}: how the reader warns about the values and ranges a file does not
         * declare.
         */
        void setUndeclaredFeatureListener(LiftHeader header, Consumer<Feature> listener);
    }

    private static volatile Access access;

    private ModelInternals() {}

    /**
     * Called once, by the model, when its classes are initialized.
     */
    public static void install(Access a) {
        if (access != null) {
            throw new IllegalStateException("The model access is already installed.");
        }
        access = a;
    }

    /**
     * The access to the model's internal wiring.
     */
    public static Access get() {
        Access a = access;
        if (a == null) {
            // The model installs it from a static initializer: make sure it has run.
            try {
                Class.forName(
                    AbstractLiftRoot.class.getName(),
                    true,
                    AbstractLiftRoot.class.getClassLoader()
                );
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
            a = access;
        }
        return a;
    }
}
