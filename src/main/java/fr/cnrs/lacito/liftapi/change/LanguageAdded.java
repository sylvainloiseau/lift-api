package fr.cnrs.lacito.liftapi.change;

import fr.cnrs.lacito.liftapi.internal.ModelInternals;
import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryLanguagesManager;

/// A language added to the object or meta languages of the dictionary.
///
/// Also reported when attaching a component brings forms in a language the dictionary
/// did not have yet: then it comes before the {@link ComponentAdded}.
///
/// @param dictionary the dictionary that changed
/// @param languages the object or the meta languages of the dictionary
/// @param language the language code added
public record LanguageAdded(LiftDictionary dictionary, LiftDictionaryLanguagesManager languages, String language) implements ModelChange {

    @Override
    public Object subject() {
        return language;
    }

    @Override
    public void revert() {
        ModelInternals.get().withoutDefinitionGuard(() -> {
            languages.removeLanguage(language);
        });
    }

    @Override
    public void reapply() {
        ModelInternals.get().withoutDefinitionGuard(() -> {
            languages.addLanguage(language);
        });
    }
}
