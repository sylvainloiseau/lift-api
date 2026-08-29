package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftObject;
import java.util.function.Consumer;

/**
 * Builder for creating LiftEntry instances with a fluent API.
 *
 * At least one form is the minimum requirement in order to build the entry.
 * 
 * Usage:
 *
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
 * 
 */
public final class EntryBuilder extends AbstractLiftElementWithFieldAndNoteAndIdBuilder<LiftEntry, LiftObject> {

    protected EntryBuilder(LiftDictionary dictionary, LiftObject parent) {
        super(LiftEntry.create(), dictionary, parent);
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
        sb.build();
        //element.addSense(sb.build());
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
        pb.build();
        //element.addPronunciation(pb.build());
        return this;
    }

    /**
     * Add a variant via nested builder configuration.
     */
    public EntryBuilder addVariant(Consumer<VariantBuilder> config) {
        VariantBuilder vb = new VariantBuilder(dictionary, element);
        config.accept(vb);
        vb.build();
        //element.addVariant(vb.build());
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
        new RelationBuilder(this.dictionary, this.element, type).withRefId(targetId).build();
        return this;
    }

    /**
     * Add a relation via nested builder configuration.
     */
    public EntryBuilder addRelation(Consumer<RelationBuilder> config) {
        RelationBuilder rb = new RelationBuilder(dictionary, element);
        config.accept(rb);
        rb.build();
        // element.addRelation(rb.build());
        return this;
    }

    /**
     * Add an etymology via nested builder configuration.
     * @see DictionaryComponentBuilderFactory#etymology(LiftEntry, String, String)
     */
    public EntryBuilder addEtymology(Consumer<EtymologyBuilder> config, String type, String source) {
        EtymologyBuilder eb = new EtymologyBuilder(this.dictionary, element, type, source);
        config.accept(eb);
        eb.build();
        //element.addEtymology(eb.build());
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

    // Override Id method (so that the good type is returned)

    @Override
    public EntryBuilder withId(String id) {
        super.withGuid(id);
        return this;
    }

    @Override
    public EntryBuilder withGuid(String guid) {
        super.withGuid(guid);
        return this;
    }

    // Override addNote in order to return the good type

    @Override
    public EntryBuilder addNote(String type, String language, String text) {
        super.addNote(type, language, text);
        return this;
    }

    @Override
    public EntryBuilder addNote(Consumer<NoteBuilder> config, String type) {
        super.addNote(config, type);
        return this;
    }

    // Override WithField so that the correct type is returned
    
    @Override
    public EntryBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    @Override
    public EntryBuilder addField(String name, Consumer<FieldBuilder> config) {
        super.addField(name, config);
        return this;
    }

    // Override Trait And Annotation builder in order to return the correct type

    @Override
    public EntryBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    @Override
    public EntryBuilder addTrait(
        String name,
        String value,
        Consumer<TraitBuilder> config
    ) {
        super.addTrait(name, value, config);
        return this;
    }

    @Override
    public EntryBuilder addAnnotation(
        String name,
        String value
    ) {
        super.addAnnotation(name, value);
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
