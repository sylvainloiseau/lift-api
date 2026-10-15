package fr.cnrs.lacito.liftapi.builder;

import java.util.Optional;
import java.util.Set;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionKind;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionTarget;
import fr.cnrs.lacito.liftapi.model.LiftHeader;

/**
 * Builder for the definition of a field or a trait, in the header (a
 * {@code <field-definition>}, or a {@code <field tag="...">} of a LIFT 0.13 header).
 *
 * Usage:
 * <pre>
 *   dictionary.getComponentBuilder()
 *       .fieldDefinition("dialect")
 *       .withKind(LiftFieldAndTraitDefinitionKind.TRAIT)
 *       .withDataModel("option")
 *       .withOptionRange(dialects)
 *       .withLabel("en", "Dialect")
 *       .build();
 * </pre>
 *
 * The kind is left unknown unless set, or implied by the data model (see
 * {@link LiftFieldAndTraitDefinition#setDataModel(Optional)}): such a definition serves
 * fields as well as traits.
 */
public final class FieldDefinitionBuilder
    extends AbstractLiftElementBuilder<LiftFieldAndTraitDefinition, LiftHeader>
{

    /**
     * @param name the name of the definition, unique in the header
     * @throws IllegalArgumentException if {@code name} is null
     */
    protected FieldDefinitionBuilder(LiftDictionary dictionary, String name) {
        super(new LiftFieldAndTraitDefinition(requireName(name), dictionary.getHeader()), dictionary, dictionary.getHeader());
    }

    private static String requireName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("A field or trait definition needs a name");
        }
        return name;
    }

    /**
     * Whether the definition is for fields or for traits.
     */
    public FieldDefinitionBuilder withKind(LiftFieldAndTraitDefinitionKind kind) {
        element.setKind(kind);
        return this;
    }

    /**
     * The components the definition applies to, as the space-separated list of the
     * {@code class} attribute.
     */
    public FieldDefinitionBuilder withTargets(String targets) {
        element.setTargets(targets);
        return this;
    }

    /**
     * The components the definition applies to.
     */
    public FieldDefinitionBuilder withTargets(Set<LiftFieldAndTraitDefinitionTarget> targets) {
        element.setTargets(targets);
        return this;
    }

    /**
     * The data model, as the {@code type} attribute writes it ({@code "option"},
     * {@code "multitext"}...). An unknown value leaves the data model undeclared.
     */
    public FieldDefinitionBuilder withDataModel(String type) {
        element.setDataModel(Optional.ofNullable(type));
        return this;
    }

    /**
     * The feature set the values of an option definition are taken from (its
     * {@code option-range}).
     */
    public FieldDefinitionBuilder withOptionRange(FeatureSet range) {
        element.setResolvedRange(Optional.ofNullable(range));
        return this;
    }

    /**
     * The writing systems of the values, as the {@code writing-system} attribute lists
     * them.
     */
    public FieldDefinitionBuilder withWritingSystem(String writingSystem) {
        element.setWritingSystem(Optional.ofNullable(writingSystem));
        return this;
    }

    /**
     * Add a label in the specified language.
     */
    public FieldDefinitionBuilder withLabel(String language, String text) {
        return withLabel(new Form(requireText(language, text), text));
    }

    /**
     * Add a label.
     */
    public FieldDefinitionBuilder withLabel(Form label) {
        if (label == null) {
            throw new IllegalArgumentException("Label cannot be null");
        }
        element.getLabel().add(label);
        return this;
    }

    /**
     * Add a description in the specified language.
     */
    public FieldDefinitionBuilder withDescription(String language, String text) {
        return withDescription(new Form(requireText(language, text), text));
    }

    /**
     * Add a description.
     */
    public FieldDefinitionBuilder withDescription(Form description) {
        if (description == null) {
            throw new IllegalArgumentException("Description cannot be null");
        }
        element.getDescription().add(description);
        return this;
    }

    private static String requireText(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
        }
        return language;
    }

    /**
     * Build the definition, adding it to the header.
     *
     * @throws IllegalArgumentException if the header already has a definition of this
     *         name
     */
    @Override
    public LiftFieldAndTraitDefinition build() {
        attach(() -> dictionary.getHeader().addFieldOrTraitDefinition(element));
        return element;
    }
}
