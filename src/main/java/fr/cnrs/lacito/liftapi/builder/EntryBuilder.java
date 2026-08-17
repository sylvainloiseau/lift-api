package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftObject;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftVariant;
import java.util.function.Consumer;

/**
 * Builder for creating LiftEntry instances with a fluent API.
 *
 * Usage:
 * <pre>
 *   LiftEntry entry = Builders.entry()
 *       .withForm("en", "dictionary")
 *       .withForm("fr", "dictionnaire")
 *       .addSense(s -> s
 *           .withGloss("en", "reference book")
 *           .withDefinition("en", "A book of words and definitions")
 *       )
 *       .build();
 * </pre>
 */
public class EntryBuilder extends AbstractLiftElementBuilder<LiftEntry, LiftObject> {

    protected EntryBuilder(LiftDictionary dictionary, LiftObject parent) {
        super(LiftEntry.create(), dictionary, parent);
    }

    /**
     * Set the entry ID.
     */
    @Override
    public EntryBuilder withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Set the entry GUID.
     */
    @Override
    public EntryBuilder withGuid(String guid) {
        super.withGuid(guid);
        return this;
    }

    /**
     * Add a form (lexical unit) in the specified language.
     */
    public EntryBuilder withForm(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException(
                "Language and text cannot be null"
            );
        }
        element.addForm(new Form(language, text));
        return this;
    }

    /**
     * Add a form to the entry.
     */
    public EntryBuilder withForm(Form form) {
        if (form == null) {
            throw new IllegalArgumentException("Form cannot be null");
        }
        element.addForm(form);
        return this;
    }

    /**
     * Add a citation in the specified language.
     */
    public EntryBuilder withCitation(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException(
                "Language and text cannot be null"
            );
        }
        element.getCitations().add(new Form(language, text));
        return this;
    }

    /**
     * Add a sense via nested builder configuration.
     */
    public EntryBuilder addSense(Consumer<SenseBuilder> config) {
        SenseBuilder sb = new SenseBuilder(dictionary, element);
        config.accept(sb);
        element.addSense(sb.build());
        return this;
    }

    /**
     * Add a sense directly.
     */
    public EntryBuilder addSense(LiftSense sense) {
        if (sense == null) {
            throw new IllegalArgumentException("Sense cannot be null");
        }
        element.addSense(sense);
        return this;
    }

    /**
     * Add multiple senses.
     */
    public EntryBuilder addSenses(LiftSense... senses) {
        if (senses != null) {
            for (LiftSense sense : senses) {
                if (sense != null) {
                    element.addSense(sense);
                }
            }
        }
        return this;
    }

    /**
     * Add a pronunciation via nested builder configuration.
     */
    public EntryBuilder addPronunciation(
        Consumer<PronunciationBuilder> config
    ) {
        PronunciationBuilder pb = new PronunciationBuilder(dictionary, element);
        config.accept(pb);
        element.addPronunciation(pb.build());
        return this;
    }

    /**
     * Add a pronunciation directly.
     */
    public EntryBuilder addPronunciation(LiftPronunciation pronunciation) {
        if (pronunciation == null) {
            throw new IllegalArgumentException("Pronunciation cannot be null");
        }
        element.addPronunciation(pronunciation);
        return this;
    }

    /**
     * Add a variant via nested builder configuration.
     */
    public EntryBuilder addVariant(Consumer<VariantBuilder> config) {
        VariantBuilder vb = new VariantBuilder(dictionary, element);
        config.accept(vb);
        element.addVariant(vb.build());
        return this;
    }

    /**
     * Add a variant directly.
     */
    public EntryBuilder addVariant(LiftVariant variant) {
        if (variant == null) {
            throw new IllegalArgumentException("Variant cannot be null");
        }
        element.addVariant(variant);
        return this;
    }

    /**
     * Add a relation with type and target ID.
     */
    public EntryBuilder addRelation(String type, String targetId) {
        if (type == null || targetId == null) {
            throw new IllegalArgumentException(
                "Type and targetId cannot be null"
            );
        }
        LiftRelation relation = new RelationBuilder(this.dictionary, this.element, type).withRefId(targetId).build();
        element.addRelation(relation);
        return this;
    }

    /**
     * Add a relation via nested builder configuration.
     */
    public EntryBuilder addRelation(Consumer<RelationBuilder> config) {
        RelationBuilder rb = new RelationBuilder(dictionary, element);
        config.accept(rb);
        element.addRelation(rb.build());
        return this;
    }

    /**
     * Add an etymology via nested builder configuration.
     */
    public EntryBuilder addEtymology(Consumer<EtymologyBuilder> config, String type, String source) {
        EtymologyBuilder eb = new EtymologyBuilder(this.dictionary, element, type, source);
        config.accept(eb);
        element.addEtymology(eb.build());
        return this;
    }

    /**
     * Set the order attribute.
     */
    public EntryBuilder withOrder(String order) {
        if (order != null) {
            element.setOrder(order);
        }
        return this;
    }

    /**
     * Set the date deleted.
     */
    public EntryBuilder dateDeleted(String date) {
        if (date != null) {
            element.setDateDeleted(date);
        }
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public EntryBuilder addNote(String type, String language, String text) {
        super.addNote(type, language, text);
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public EntryBuilder addNote(Consumer<NoteBuilder> config, String type) {
        super.addNote(config, type);
        return this;
    }

    /**
     * Add a trait.
     */
    @Override
    public EntryBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    /**
     * Add a field.
     */
    @Override
    public EntryBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    /**
     * Build the entry.
     */
    @Override
    public LiftEntry build() {
        if (element.getForms().isEmpty()) {
            throw new IllegalStateException(
                "Entry must have at least one form"
            );
        }
        super.register();
        return element;
    }
}
