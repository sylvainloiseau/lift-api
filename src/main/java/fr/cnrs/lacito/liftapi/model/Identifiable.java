package fr.cnrs.lacito.liftapi.model;

public sealed interface Identifiable
    permits AbstractIdentifiable {
    
    abstract public void setId(String id);

    abstract public void setGuid(String guid);
}
