package fr.cnrs.lacito.liftapi;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

public class Utils {

    /**
     * Resolve a classpath resource to a {@link File}.
     *
     * {@code URL.getPath()} is not URL-decoded, so it breaks as soon as the build
     * directory contains a space (or any other escaped character). Going through
     * {@code URI} fixes that.
     */
    public static final File resourceFile(String resource) {
        URL resourceUrl = Utils.class.getClassLoader().getResource(resource);
        if (resourceUrl == null) throw new IllegalStateException(
            "Resource not found on the test classpath: " + resource
        );
        try {
            return Paths.get(resourceUrl.toURI()).toFile();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(
                "Cannot turn resource URL into a file: " + resourceUrl,
                e
            );
        }
    }

    public static final LiftDictionary loadDictionaryForTest(String file) {
        return loadDictionaryForTest(resourceFile(file));
    }

    public static final LiftDictionary loadDictionaryForTest(File file) {
        try {
            return LiftDictionary.loadDictionaryFromFile(file);
        } catch (LiftDocumentLoadingException e) {
            // Rethrowing (rather than calling fail() and returning null) keeps the
            // real cause visible instead of surfacing it as a downstream NPE.
            throw new AssertionError(
                "Could not load test dictionary: " + file,
                e
            );
        }
    }
}
