# lift-api

A library for dictionaries in LIFT format.

# Installation

```
```

# Usage

## Dictionary creation

Reading a file:

```java
import java.io.File;
import fr.cnrs.lacito.liftapi.LiftDictionary;

File file = new File("dictionary.lift");
LiftDictionary lf = LiftDictionary.loadDictionaryFromFile(file);

// ...

lf.save();
```

Building from scratch:

```java
import fr.cnrs.lacito.liftapi.LiftDictionary;

LiftDictionary dictionary = LiftDictionary.makeBuilder()
        .withLiftVersion("0.15")
        .withProducer("Test Producer")
        .build();

```

## Adding entries

```java
import fr.cnrs.lacito.liftapi.LiftDictionary;

        LiftEntry word = dictionary
            .getComponentBuilder()
            .entry()
            .withId("word-001")
            .withForm("en", "run")
            .withForm("fr", "courir")
            .addSense(s ->
                s
                    .withOrder(1)
                    .withGloss("en", "to move quickly on foot")
                    .withDefinition(
                        "en",
                        "To move at a pace faster than walking"
                    )
                    .withPartOfSpeech("verb")
                    .addExample(ex ->
                        ex
                            .withExample("en", "She runs every morning")
                            .addTranslation(
                                "litteral",
                                "fr",
                                "Elle court chaque matin"
                            )
                    )
            )
            .addSense(s ->
                s
                    .withOrder(2)
                    .withGloss("en", "to manage or operate")
                    .withPartOfSpeech("verb")
            )
            .addPronunciation(p -> p.withPronunciation("en", "rʌn"))
            .addNote("source", "en", "From Old English 'irnan'")
            .build();

```

# Running tests

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
