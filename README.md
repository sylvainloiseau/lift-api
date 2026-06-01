# lift-api

A library for querying a dictionary in LIFT format.

```java
import java.io.File;
import java.util.List;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.LiftDictionary;

File file = new File("dictionary.lift");
LiftDictionary lf = LiftDictionary.loadDictionaryWithFile(file);

List<LiftEntry> entries = lf.getLiftDictionaryComponents().getAllEntries();

// ...

lf.save();
```

## Running tests

All tests:

```bash
mvn test -pl lift-api
```

For a specific test:

```bash
mvn test -pl lift-api -Dtest=MultiTextTest#testTextAndSeveralSpan
```

## Installing into the local Maven repository

Since this is a multi-module Maven project, install from the repository root:

```bash
mvn install
```

This builds and installs both `lift-api` and `dictionary-editor-fx` into your local Maven repository.

# Author

Master students in Computer science at [Institut Galilé, USPN University](https://galilee.univ-paris13.fr/master/master-informatique/) :

- Ayman Jari
- Erij Mazouz
- Ermeline Bresson
- Inès Gbadamassi
- Maryse Goeh-Akue