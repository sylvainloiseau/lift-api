package fr.cnrs.lacito.liftapi.model;

public final class LangObjectManager extends LangManager<LangObject> {

    @Override
    void addLang(String lang) {
        super.langs.put(lang, new LangObject(lang));
    }
    
}
