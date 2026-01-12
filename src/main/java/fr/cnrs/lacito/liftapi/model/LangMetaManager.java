package fr.cnrs.lacito.liftapi.model;

public final class LangMetaManager extends LangManager<LangMeta> {

    @Override
    void addLang(String lang) {
        super.langs.put(lang, new LangMeta(lang));
    }

}
