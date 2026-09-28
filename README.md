# lift-api

A library for working with dictionaries in LIFT format (<a
href="https://github.com/sillsdev/lift-standard">https://github.com/sillsdev/lift-standard</a>).

The LIFT dictionary format allows to represent complex linguistic structures and
has been elaborated for descriptive linguistics.

# Features

- complete modeling of the LIFT dictionary data structure (with its entry, note, example components, but also the note, field, trait, annotation components, references to media; cross-references, feature sets, etc.)
- load and save LIFT dictionary
- walk the dictionary data structure: hierarchically, or by components types
- maintain a registry of the features set and their features: manipulate feature names, count, etc.
- maintain a registry of all metalanguage or object language forms, number of occurrences of each languages
- maintain a registry of cross-references between components
- offer methods to mutate the dictionary, with two API (a fluent API or a low-level API): create components, update component properties, delete or move components, etc.

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
## CLI

Utility for loading a dictionary:

```
java -jar lift-api/target/lift-api-0.1-SNAPSHOT-jar-with-dependencies.jar <dictionaryFile>
```

Will print error message or quick summary.

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
