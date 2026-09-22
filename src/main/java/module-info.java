/**
 * A library for reading, editing and writing LIFT (Lexicon Interchange Format)
 * dictionaries.
 *
 * @see fr.cnrs.lacito.liftapi
 */
module fr.cnrs.lacito.liftapi {
    requires transitive java.xml;
    requires java.logging;
    // The model is built on JavaFX properties and observable collections, which are
    // part of this module's public API: `requires transitive` is deliberate.
    requires transitive javafx.base;
    requires info.picocli;

    // The public API. Everything a consumer needs is here: a dictionary, the model it
    // holds, and the fluent API for extending it.
    exports fr.cnrs.lacito.liftapi;
    exports fr.cnrs.lacito.liftapi.builder;
    exports fr.cnrs.lacito.liftapi.model;

    // fr.cnrs.lacito.liftapi.xml and fr.cnrs.lacito.liftapi.internal are deliberately
    // NOT exported. Serialization is reached through LiftDictionary.loadDictionaryFromFile
    // and LiftDictionary.save, and the mutation core through the builders; their types
    // being public is a requirement of javac across packages, not an invitation to call
    // them. Leaving them unexported makes that distinction compiler-enforced.

    opens fr.cnrs.lacito.liftapi;
    opens fr.cnrs.lacito.liftapi.builder;
    opens fr.cnrs.lacito.liftapi.model;

    // cli.Main is the jar's main class and annotates a private field, which picocli
    // reads reflectively: without this the CLI throws InaccessibleObjectException on
    // the module path.
    opens fr.cnrs.lacito.liftapi.cli to info.picocli;
}
