package fr.cnrs.lacito.liftapi.xml;

import java.io.File;
import java.io.FileNotFoundException;

import fr.cnrs.lacito.liftapi.LiftDictionary;

/**
 * Facade for serializing a LiftDictionary to XML.
 * 
 * Uses LiftWriterSession for actual serialization and proper resource management.
 * 
 * Usage:
 * <pre>
 *   LiftDictionary dict = LiftDictionary.loadDictionaryWithFile(inputFile);
 *   // ... modify dictionary ...
 *   try (LiftWriterSession session = new LiftWriterSession(outputFile)) {
 *       session.marshall(dict);
 *   }
 * </pre>
 * 
 * Note: For backward compatibility with existing code that used LiftWriter directly,
 * you can still use the LiftWriter class, but it's recommended to migrate to LiftWriterSession
 * for better resource management and API clarity.
 */
public class LiftWriter {

    private final File outputFile;

    /**
     * Create a new writer for the given output file.
     * 
     * @param f the file to write to
     * @throws FileNotFoundException if the file cannot be created
     */
    public LiftWriter(File f) throws FileNotFoundException {
        if (f == null) {
            throw new FileNotFoundException("Output file cannot be null");
        }
        this.outputFile = f;
    }

    /**
     * Marshall a dictionary to the output file.
     * 
     * Recommended: Use LiftWriterSession directly for better resource management:
     * <pre>
     *   try (LiftWriterSession session = new LiftWriterSession(outputFile)) {
     *       session.marshall(dictionary);
     *   }
     * </pre>
     * 
     * @param d the dictionary to serialize
     * @throws Exception if serialization fails
     */
    public void marshall(LiftDictionary d) throws Exception {
        try (LiftWriterSession session = new LiftWriterSession(outputFile)) {
            session.marshall(d);
        }
    }
}
