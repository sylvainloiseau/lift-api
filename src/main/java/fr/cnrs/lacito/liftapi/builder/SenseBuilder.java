package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftIllustration;
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
public class SenseBuilder extends AbstractLiftElementBuilder<LiftSense> {

    public SenseBuilder() {
        this.element = LiftSense.create();
    }

    /**
     * Set the sense ID.
     */
    @Override
    public SenseBuilder withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Set the sense GUID.
     */
    @Override
    public SenseBuilder withGuid(String guid) {
        super.withGuid(guid);
        return this;
    }

    /**
     * Add a gloss in the specified language.
     */
    public SenseBuilder withGloss(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
        }
        element.getGloss().add(new Form(language, text));
        return this;
    }

    /**
     * Add a gloss.
     */
    public SenseBuilder withGloss(Form gloss) {
        if (gloss == null) {
            throw new IllegalArgumentException("Gloss cannot be null");
        }
        element.getGloss().add(gloss);
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
        if (pos != null) {
            element.setGrammaticalInfo(pos);
        }
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
        ExampleBuilder eb = new ExampleBuilder();
        config.accept(eb);
        element.addExample(eb.build());
        return this;
    }

    /**
     * Add an example directly.
     */
    public SenseBuilder addExample(LiftExample example) {
        if (example == null) {
            throw new IllegalArgumentException("Example cannot be null");
        }
        element.addExample(example);
        return this;
    }

    /**
     * Add an illustration.
     */
    public SenseBuilder addIllustration(LiftIllustration illustration) {
        if (illustration == null) {
            throw new IllegalArgumentException("Illustration cannot be null");
        }
        element.addIllustration(illustration);
        return this;
    }

    /**
     * Add a relation via nested builder configuration.
     */
    public SenseBuilder addRelation(Consumer<RelationBuilder> config) {
        RelationBuilder rb = new RelationBuilder();
        config.accept(rb);
        element.addRelation(rb.build());
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
        SenseBuilder sb = new SenseBuilder();
        config.accept(sb);
        element.addSense(sb.build());
        return this;
    }

    /**
     * Add a sub-sense directly.
     */
    public SenseBuilder addSubSense(LiftSense subSense) {
        if (subSense == null) {
            throw new IllegalArgumentException("Sub-sense cannot be null");
        }
        element.addSense(subSense);
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public SenseBuilder addNote(String type, String language, String text) {
        super.addNote(type, language, text);
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public SenseBuilder addNote(Consumer<NoteBuilder> config) {
        super.addNote(config);
        return this;
    }

    /**
     * Add a trait.
     */
    @Override
    public SenseBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    /**
     * Add a field.
     */
    @Override
    public SenseBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    /**
     * Build the sense.
     */
    @Override
    public LiftSense build() {
        return element;
    }
}
