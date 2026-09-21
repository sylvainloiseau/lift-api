package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasSense;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import java.util.function.Consumer;

/**
 * Builder for creating LiftSense instances with a fluent API.
 *
 * Usage:
 * <pre>
 *   LiftSense sense = Builders.sense()
 *       .withGloss("en", "reference book")
 *       .withDefinition("en", "A book containing words and their definitions")
 *       .withPartOfSpeech("noun")
 *       .addExample(ex -> ex
 *           .withExample("en", "I found it in the dictionary")
 *       )
 *       .addSubSense(sub -> sub
 *           .withGloss("en", "a specific type of dictionary")
 *       )
 *       .build();
 * </pre>
 */
public final class SenseBuilder extends AbstractLiftElementWithFieldAndNoteAndIdBuilder<LiftSense, HasSense> {

    protected SenseBuilder(LiftDictionary dictionary, HasSense parent) {
        super(LiftSense.create(), dictionary, parent);
    }

    /**
     * Add a gloss in the specified language.
     */
    public SenseBuilder withGloss(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
        }
        element.getGlosses().add(new Form(language, text));
        return this;
    }

    /**
     * Add a gloss.
     */
    public SenseBuilder withGloss(Form gloss) {
        if (gloss == null) {
            throw new IllegalArgumentException("Gloss cannot be null");
        }
        element.getGlosses().add(gloss);
        return this;
    }

    /**
     * Add a definition in the specified language.
     */
    public SenseBuilder withDefinition(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
        }
        element.getDefinition().add(new Form(language, text));
        return this;
    }

    /**
     * Add a definition.
     */
    public SenseBuilder withDefinition(Form definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Definition cannot be null");
        }
        element.getDefinition().add(definition);
        return this;
    }

    /**
     * Set the part of speech.
     */
    public SenseBuilder withPartOfSpeech(String pos) {
        if (pos == null) {
            throw new IllegalArgumentException("Part of speech cannot be null");
        }
        Feature gramInfo = dictionary.getHeader().getGrammaticalInfoManager().getOrCreateFeature(pos);
        element.setGrammaticalInfo(gramInfo);
        return this;
    }

    /**
     * Set the order of this sense.
     */
    public SenseBuilder withOrder(Integer order) {
        if (order != null) {
            element.setOrder(order);
        }
        return this;
    }

    /**
     * Add an example via nested builder configuration.
     */
    public SenseBuilder addExample(Consumer<ExampleBuilder> config) {
        ExampleBuilder eb = new ExampleBuilder(dictionary, element);
        config.accept(eb);
        eb.build();
        return this;
    }

    /**
     * Add a relation via nested builder configuration.
     */
    public SenseBuilder addRelation(Consumer<RelationBuilder> config, String type) {
        RelationBuilder rb = new RelationBuilder(dictionary, element, type);
        config.accept(rb);
        rb.build();
        return this;
    }

    /**
     * Add a relation directly.
     */
    public SenseBuilder addRelation(LiftRelation relation) {
        if (relation == null) {
            throw new IllegalArgumentException("Relation cannot be null");
        }
        element.addRelation(relation);
        return this;
    }

    /**
     * Add a sub-sense via nested builder configuration.
     */
    public SenseBuilder addSubSense(Consumer<SenseBuilder> config) {
        SenseBuilder sb = new SenseBuilder(dictionary, element);
        config.accept(sb);
        sb.build();
        return this;
    }

    // Override Id method (so that the good type is returned)

    @Override
    public SenseBuilder withId(String id) {
        super.withId(id);
        return this;
    }

    @Override
    public SenseBuilder withGuid(String guid) {
        super.withGuid(guid);
        return this;
    }

    // Override addNote in order to return the good type

    @Override
    public SenseBuilder addNote(String type, String language, String text) {
        super.addNote(type, language, text);
        return this;
    }

    @Override
    public SenseBuilder addNote(Consumer<NoteBuilder> config, String type) {
        super.addNote(config, type);
        return this;
    }

    // Override WithField so that the correct type is returned
    
    @Override
    public SenseBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    @Override
    public SenseBuilder addField(String name, Consumer<FieldBuilder> config) {
        super.addField(name, config);
        return this;
    }

    // Override Trait And Annotation builder in order to return the correct type

    @Override
    public SenseBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    @Override
    public SenseBuilder addTrait(
        String name,
        String value,
        Consumer<TraitBuilder> config
    ) {
        super.addTrait(name, value, config);
        return this;
    }

    @Override
    public SenseBuilder addAnnotation(
        String name,
        String value
    ) {
        super.addAnnotation(name, value);
        return this;
    }

    /**
     * Build the sense.
     */
    @Override
    public LiftSense build() {
        if (
            element.getDefinition().getLangs().size() == 0 
            &&
            element.getGlosses().getLangs().size() == 0 
        ) {
            throw new IllegalStateException("A sense must contain at least one gloss or one definition.");
        }
        super.register();
        return element;
    }
}
