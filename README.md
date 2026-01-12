# lift-api

An library for querying a dictionary in LIFT format.

```
import java.util.List;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.LiftDictionary;

String url = "dictionary.lift";
LiftDictionary lf = LiftDictionary.loadDictionaryWithFile(lf);

List<LiftEntry> entries = this.liftDictionaryComponents.getAllEntries()

// ...

lf.save();

```

# Running test

For specific test:

```
mvn test -Dtest=MultiTextTest#testTextAndSeveralSpan
```

# Installing into the maven repository

```
mvn install:install-file \
   -Dfile=target/lift-api-0.1-SNAPSHOT-jar-with-dependencies.jar \
   -DgroupId=fr.cnrs.lacito \
   -DartifactId=lift-api \
   -Dversion=0.1-SNAPSHOT \
   -Dpackaging=jar \
   -DgeneratePom=true
```
