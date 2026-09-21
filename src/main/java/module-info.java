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

    exports fr.cnrs.lacito.liftapi;
    exports fr.cnrs.lacito.liftapi.builder;
    exports fr.cnrs.lacito.liftapi.model;
    exports fr.cnrs.lacito.liftapi.xml;

    opens fr.cnrs.lacito.liftapi;
    opens fr.cnrs.lacito.liftapi.builder;
    opens fr.cnrs.lacito.liftapi.model;
    opens fr.cnrs.lacito.liftapi.xml;

    // cli.Main is the jar's main class and annotates a private field, which picocli
    // reads reflectively: without this the CLI throws InaccessibleObjectException on
    // the module path.
    opens fr.cnrs.lacito.liftapi.cli to info.picocli;
}
