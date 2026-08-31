package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasNote;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.LiftDictionaryBuilder;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

/**
 * Fluent API for creating a {@link LiftFieldAndTraitDefinition}.
 * 
 */
public class LiftFieldAndTraitDefinitionBuilder
    extends AbstractLiftElementBuilder<LiftFieldAndTraitDefinition, LiftHeader>
{

    public LiftFieldAndTraitDefinitionBuilder(
        LiftDictionary dictionary,
        String definitionId
    ) {
        super(new LiftFieldAndTraitDefinition(definitionId, dictionary.getHeader()), dictionary, dictionary.getHeader());
    }

    public LiftFieldAndTraitDefinitionBuilder withDescription(String lang, String description) {
        element.getDescription().add(new Form (lang, description));
        return this;
    }

    // Set<LiftFieldAndTraitDefinitionTarget> getTargets() 

    // Optional<LiftFieldAndTraitDefinitionDataModel> getType() 

    // void setDataModel(Optional<String> typeStr) 

    @Override
    public LiftFieldAndTraitDefinition build() {
        // dictionary.getHeader().addFeatureSet(element);
        return element;
    }
}
