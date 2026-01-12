package fr.cnrs.lacito.liftapi.model;

public sealed interface ExtensibleWithoutField
    permits HasField, AbstractExtensibleWithoutField {
}
