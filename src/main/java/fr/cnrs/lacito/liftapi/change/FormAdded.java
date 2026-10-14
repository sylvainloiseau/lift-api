package fr.cnrs.lacito.liftapi.change;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.MultiText;

/// A form added to a multitext.
///
/// @param dictionary the dictionary that changed
/// @param text the multitext
/// @param form the form added; a revert removes this very object, and a reapply adds it
///        back with its text and annotations
public record FormAdded(LiftDictionary dictionary, MultiText text, Form form) implements ModelChange {

    @Override
    public Object subject() {
        return form;
    }

    @Override
    public void revert() {
        text.removeForm(form.getLang());
    }

    @Override
    public void reapply() {
        text.add(form);
    }
}
