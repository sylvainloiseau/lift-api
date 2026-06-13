package fr.cnrs.lacito.liftapi.builder;

import java.util.UUID;

import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
import fr.cnrs.lacito.liftapi.model.MultiTextObjectLanguage;
import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import fr.cnrs.lacito.liftapi.model.Form;

public final class MultitextObjectLanguageBuilder extends MultiTextBuilder {

    protected MultitextObjectLanguageBuilder(LiftDictionaryRegistry registry) {
        super(registry);
        super.element = new MultiTextObjectLanguage(registry);
    }

    public MultiTextObjectLanguage build() {
        UUID uuid = registry.getNewUUID();
        this.element.setUUID(uuid);
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

        MultiTextObjectLanguage e = (MultiTextObjectLanguage) element;
        registry.register(e);
        return e;
    }
    
}
