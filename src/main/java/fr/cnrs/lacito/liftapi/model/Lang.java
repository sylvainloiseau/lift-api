package fr.cnrs.lacito.liftapi.model;

public sealed class Lang permits LangObject, LangMeta {
    protected String id;
    protected Lang(String id) {
        this.id = id;
    }
}
