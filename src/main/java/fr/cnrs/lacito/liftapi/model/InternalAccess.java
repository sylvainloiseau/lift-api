package fr.cnrs.lacito.liftapi.model;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.UUID;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryLanguagesManager;
import fr.cnrs.lacito.liftapi.internal.ModelInternals;

/**
 * The model's side of {@link ModelInternals}: the package-private wiring that the
 * mutator, the XML reader and the dictionary need, and that consumers must not call.
 * Installed by {@link AbstractLiftRoot}'s static initializer.
 */
final class InternalAccess implements ModelInternals.Access {

    @Override
    public void setUUID(AbstractLiftRoot node, UUID uuid) {
        node.setUUID(uuid);
    }

    @Override
    public void setUUID(MultiText text, UUID uuid) {
        text.setUUID(uuid);
    }

    @Override
    public void setOwningDictionary(LiftEntry entry, LiftDictionary dictionary) {
        entry.setOwningDictionary(dictionary);
    }

    @Override
    public void setLanguagesManager(MultiText text, LiftDictionaryLanguagesManager manager) {
        text.setLanguagesManager(manager);
    }

    @Override
    public void unregister(MultiText text) {
        text.unregister();
    }

    @Override
    public void putOtherXmlAttribute(AbstractLiftRoot node, String name, String value) {
        node.putOtherXmlAttribute(name, value);
    }

    @Override
    public void setDictionary(LiftHeader header, LiftDictionary dictionary) {
        header.setDictionary(dictionary);
    }

    @Override
    public void setFieldSpecification(LiftField field, FieldSpecification specification) {
        field.respecify(specification);
    }

    @Override
    public void withoutRebinding(LiftHeader header, Runnable operation) {
        header.withoutRebinding(operation);
    }

    @Override
    public void suspendRebinding(LiftHeader header) {
        header.suspendRebinding();
    }

    @Override
    public void resumeRebinding(LiftHeader header) {
        header.resumeRebinding();
    }

    @Override
    public void withoutDefinitionGuard(Runnable operation) {
        DefinitionGuard.withoutGuard(operation);
    }

    @Override
    public void setHref(FeatureSet set, String href) {
        set.setHref(href);
    }

    @Override
    public void setExternalDefinition(FeatureSet set, Path file, boolean missing) {
        set.setExternalDefinitionFile(file);
        set.setExternalDefinitionMissing(missing);
    }

    @Override
    public void markDeclaredInline(FeatureSet set, Object part) {
        set.markDeclaredInline(part);
    }

    @Override
    public Feature getOrCreateUndeclaredFeature(FeatureSet set, String id) {
        return set.getOrCreateUndeclaredFeature(id);
    }

    @Override
    public void setDeclared(Feature feature, boolean declared) {
        feature.setDeclared(declared);
    }

    @Override
    public void setDeclared(FeatureSet set, boolean declared) {
        set.setDeclared(declared);
    }

    @Override
    public void setUndeclaredFeatureListener(LiftHeader header, Consumer<Feature> listener) {
        header.setUndeclaredFeatureListener(listener);
    }
}
