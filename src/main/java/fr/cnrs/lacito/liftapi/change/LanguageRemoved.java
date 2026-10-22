package fr.cnrs.lacito.liftapi.change;

import fr.cnrs.lacito.liftapi.internal.ModelInternals;
import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryLanguagesManager;

/// A language removed from the object or meta languages of the dictionary.
///
/// @param dictionary the dictionary that changed
/// @param languages the object or the meta languages of the dictionary
/// @param language the language code removed
public record LanguageRemoved(LiftDictionary dictionary, LiftDictionaryLanguagesManager languages, String language) implements ModelChange {

    @Override
    public Object subject() {
        return language;
    }

    @Override
    public void revert() {
        ModelInternals.get().withoutDefinitionGuard(() -> {
            languages.addLanguage(language);
        });
    }

    @Override
    public void reapply() {
        ModelInternals.get().withoutDefinitionGuard(() -> {
            languages.removeLanguage(language);
        });
    }
}
