package fr.cnrs.lacito.liftapi.model;

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
}
