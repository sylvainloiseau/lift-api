package fr.cnrs.lacito.liftapi.model;

import java.util.HashMap;
import java.util.Map;

public abstract sealed class LangManager<L extends Lang> permits LangObjectManager, LangMetaManager {
    protected Map<String, L> langs;

    protected LangManager() {
        this.langs = new HashMap<>();
    }
    
    protected boolean hasLang(String lang) {
        return langs.containsKey(lang);
    }

    abstract void addLang(String lang);
    
    public L getLang(String lang) {
        if (!hasLang(lang)) {
            throw new IllegalArgumentException("No language: " + lang);
        }
        return langs.get(lang);
    }
}
