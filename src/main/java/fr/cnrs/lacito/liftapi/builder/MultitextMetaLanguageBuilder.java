package fr.cnrs.lacito.liftapi.builder;

import java.util.UUID;

import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.MultiTextMetaLanguage;
import javafx.collections.MapChangeListener;

public final class MultitextMetaLanguageBuilder extends MultiTextBuilder {

    protected MultitextMetaLanguageBuilder(LiftDictionaryRegistry registry) {
        super(registry);
        super.element = new MultiTextMetaLanguage(registry);
        // this.element.formsProperty().get().addListener(
        //     (MapChangeListener<String, Form>)
        //     (change) -> {
        //             if (change.wasAdded()) {
        //                 registry.getLanguageCounter().addObjectLanguageOccurrence(change.getKey());
        //             }
        //             if (change.wasRemoved()) {
        //                 registry.getLanguageCounter().removeObjectLanguageOccurrence(change.getKey());
        //             }
        //         }
        // );
    }

    public MultiTextMetaLanguage build() {
        UUID uuid = registry.getNewUUID();
        this.element.setUUID(uuid);
        MultiTextMetaLanguage e = (MultiTextMetaLanguage) element;
        registry.register(e);
        return e;
    }

}
