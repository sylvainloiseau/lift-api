package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;

public final class MultiTextMetaLanguage extends MultiText {

    private LiftDictionaryRegistry registry;

    public MultiTextMetaLanguage(LiftDictionaryRegistry registry) {
        this.registry = registry;
    }
}
