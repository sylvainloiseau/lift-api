package fr.cnrs.lacito.liftapi.change;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.MultiText;

/// A form removed from a multitext, for instance by setting
/// {@link fr.cnrs.lacito.liftapi.model.MultiText#formTextProperty(String)} to an empty text.
///
/// @param dictionary the dictionary that changed
/// @param text the multitext
/// @param form the form removed, kept with its text and annotations so that a revert
///        brings back the same object
public record FormRemoved(LiftDictionary dictionary, MultiText text, Form form) implements ModelChange {

    @Override
    public Object subject() {
        return form;
    }

    @Override
    public void revert() {
        text.add(form);
    }

    @Override
    public void reapply() {
        text.removeForm(form.getLang());
    }
}
