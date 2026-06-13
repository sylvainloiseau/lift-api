package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;

public final class MultiTextObjectLanguage extends MultiText {

    private LiftDictionaryRegistry registry;

    public MultiTextObjectLanguage(LiftDictionaryRegistry registry) {
        this.registry = registry;
    }
}
