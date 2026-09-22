package fr.cnrs.lacito.liftapi.xml;

import fr.cnrs.lacito.liftapi.LiftVersion;

/**
 * Represents the LIFT vocabulary, including constants for XML element and attribute names.
 */
public final class LiftVocabulary {

    private LiftVocabulary() {}

    public static final String DATE_DELETED_ATTRIBUTE = "dateDeleted";
    public static final String DATE_CREATED_ATTRIBUTE = "dateCreated";
    public static final String DATE_MODIFIED_ATTRIBUTE = "dateModified";
    public static final String CLASS_ATTRIBUTE = "class";
    public static final String PARENT_ATTRIBUTE = "parent";
    public static final String TAG_ATTRIBUTE = "tag";
    public static final String OPTION_RANGE_ATTRIBUTE = "option-range";
    public static final String WRITING_SYSTEM_ATTRIBUTE = "writing-system";
    public static final String ORDER_ATTRIBUTE = "order";
    public static final String TYPE_ATTRIBUTE = "type";
    public static final String SOURCE_ATTRIBUTE = "source";
    public static final String REF_ATTRIBUTE = "ref";
    public static final String VALUE_ATTRIBUTE = "value";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String WHO_ATTRIBUTE = "who";
    public static final String WHEN_ATTRIBUTE = "when";
    public static final String LANG_ATTRIBUTE = "lang";
    public static final String GUID_ATTRIBUTE = "guid";
    public static final String HREF_ATTRIBUTE = "href";
    public static final String ID_ATTRIBUTE = "id";

    public static final String LIFT_LOCAL_NAME = "lift";
    public final static String ENTRY_LOCAL_NAME = "entry";
    public final static String SENSE_LOCAL_NAME = "sense";
    public final static String SUBSENSE_LOCAL_NAME = "subsense";
    public final static String LIFT_URI = "";
    public static final String EXAMPLE_LOCAL_NAME = "example";
    public static final String ETYMOLOGY_LOCAL_NAME = "etymology";
    public static final String VARIANT_LOCAL_NAME = "variant";
    public static final String RELATION_LOCAL_NAME = "relation";
    public static final String PRONUNCIATION_LOCAL_NAME = "pronunciation";
    public final static String LEXICAL_UNIT_LOCAL_NAME = "lexical-unit";
    public final static String FORM_LOCAL_NAME = "form";
    public final static String TEXT_LOCAL_NAME = "text";
    public final static String SPAN_LOCAL_NAME = "span";
    public final static String TRAIT_LOCAL_NAME = "trait";
    public final static String ANNOTATION_LOCAL_NAME = "annotation";
    public final static String FIELD_LOCAL_NAME = "field";
    public final static String GRAM_INFO_LOCAL_NAME = "grammatical-info";
    public final static String GLOSS_LOCAL_NAME = "gloss";
    public final static String CITATION_LOCAL_NAME = "citation";
    public final static String DEFINITION_LOCAL_NAME = "definition";
    public final static String LABEL_LOCAL_NAME = "label"; // in range, range element, field-description, illustration and media// Duplicate with HEADER_RANGE_LABEL_LOCAL_NAME and HEADER_FIELD_DESCRIPTION_LABEL_LOCAL_NAME
    public final static String NOTE_LOCAL_NAME = "note";
    public final static String USAGE_LOCAL_NAME = "usage";
    public final static String REVERSAL_LOCAL_NAME = "reversal";
    public final static String MAIN_LOCAL_NAME = "main";
    public final static String TRANSLATION_LOCAL_NAME = "translation";
    public final static String MEDIA_LOCAL_NAME = "media";
    public static final String HEADER_LOCAL_NAME = "header";
    public static final String ILLUSTRATION_LOCAL_NAME = "illustration";
    public static final String HEADER_DESCRIPTION_LOCAL_NAME = "description"; // in header, range, range-element // see HEADER_RANGE_DESCRIPTION_LOCAL_NAME and HEADER_FIELD_DESCRIPTION_DESCRIPTION_LOCAL_NAME
    public static final String HEADER_FIELDS_DEFINITION_LOCAL_NAME = "fields";
    public static final String HEADER_RANGES_LOCAL_NAME = "ranges";
    public static final String HEADER_RANGE_LOCAL_NAME = "range";
    //public static final String HEADER_RANGE_LABEL_LOCAL_NAME = "label";
    public static final String HEADER_RANGE_ABBREV_LOCAL_NAME = "abbrev";
    //public static final String HEADER_RANGE_DESCRIPTION_LOCAL_NAME = "description";
    public static final String HEADER_RANGE_ELEMENT_LOCAL_NAME = "range-element";
    public static final String LIFT_RANGES_ROOT = "lift-ranges";
    public static final String HEADER_FIELD_DEFINITION_LOCAL_NAME = "field-definition";
    //public static final String HEADER_FIELD_DESCRIPTION_LABEL_LOCAL_NAME = "label";
    //public static final String HEADER_FIELD_DESCRIPTION_DESCRIPTION_LOCAL_NAME = "description";
    public static final String PRODUCER_ATTRIBUTE = "producer";
    public static final String VERSION_ATTRIBUTE = "version";

    /**
     * The attribute of {@code <field>} naming its {@code field-definition}.
     *
     * {@code field-content} declares {@code @type} in LIFT 0.13 and {@code @name}
     * in LIFT 0.15.
     */
    public static String fieldNameAttribute(LiftVersion version) {
        return switch (version) {
            case V0_13 -> TYPE_ATTRIBUTE;
            case V0_15 -> NAME_ATTRIBUTE;
        };
    }

    /**
     * The element declaring a field or trait definition in the header.
     *
     * LIFT 0.13 uses {@code <field tag="...">}; LIFT 0.15 renamed it to
     * {@code <field-definition name="...">}.
     */
    public static String fieldDefinitionElement(LiftVersion version) {
        return switch (version) {
            case V0_13 -> FIELD_LOCAL_NAME;
            case V0_15 -> HEADER_FIELD_DEFINITION_LOCAL_NAME;
        };
    }

    /**
     * The attribute naming a field or trait definition in the header.
     *
     * @see #fieldDefinitionElement(LiftVersion)
     */
    public static String fieldDefinitionNameAttribute(LiftVersion version) {
        return switch (version) {
            case V0_13 -> TAG_ATTRIBUTE;
            case V0_15 -> NAME_ATTRIBUTE;
        };
    }

    /** The textual form of a version, as written on {@code lift/@version}. */
    public static String versionAttributeValue(LiftVersion version) {
        return switch (version) {
            case V0_13 -> "0.13";
            case V0_15 -> "0.15";
        };
    }
}
