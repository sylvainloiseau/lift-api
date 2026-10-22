package fr.cnrs.lacito.liftapi.change;

import fr.cnrs.lacito.liftapi.LiftDictionary;

/// A change of a dictionary, already applied when it is reported.
///
/// Every public operation of the model that changes a dictionary reports one or more of
/// these to the dictionary's listeners
/// ({@link LiftDictionary#addChangeListener(java.util.function.Consumer)}). Each one
/// carries what is needed to undo it: {@link #revert()} puts the dictionary back as it
/// was before the change, and {@link #reapply()} makes the change again.
///
/// Both go through the public API of the model, so the dictionary's indexes stay
/// consistent, and both report their own changes like any other operation. Reverting a
/// sequence of changes means reverting them in reverse order. They put back a state that
/// was valid, so they are not refused in a feature set defined in an external file
/// (which is otherwise read-only): a bare feature a builder added there, or a rebinding.
///
/// See the package documentation for the rules.
public sealed interface ModelChange
    permits ValueChanged, ComponentAdded, ComponentRemoved,
            FormAdded, FormRemoved,
            FeatureAdded, FeatureRemoved, FeatureSetAdded, FeatureSetRemoved,
            DefinitionAdded, DefinitionRemoved,
            LanguageAdded, LanguageRemoved {

    /// The dictionary that changed.
    LiftDictionary dictionary();

    /// The object the change is about: the component added or removed, the object whose
    /// value changed, the form, the feature, the definition, the language.
    Object subject();

    /// Undo this change. The state must be the one right after the change: revert
    /// a sequence of changes in reverse order.
    void revert();

    /// Make this change again, after {@link #revert()}.
    void reapply();
}
