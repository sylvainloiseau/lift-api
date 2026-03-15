package fr.cnrs.lacito.liftapi.exporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

//import net.sf.saxon.TransformerFactoryImpl;

public class LatexExporter {

    private static final Logger LOGGER = Logger.getLogger(LatexExporter.class.getName());
    private static final String STYLESHEET_FILENAME = "";

    private File inputFile;
    private File outFile;

    public void transform2Latex(File outfile) {
        InputStream is = null;
        OutputStream outs = null;
        InputStream stylesheet = null;
        try {
            is = new FileInputStream(this.inputFile);
            LOGGER.info("Loading dictionary: " + outfile);

            outs = new FileOutputStream(outfile);
            LOGGER.info("Outputstream opened: " + outfile);

            stylesheet = LatexExporter.class.getResourceAsStream(STYLESHEET_FILENAME);
            LOGGER.info("Stylesheet loaded: " + LatexExporter.class.getResource(STYLESHEET_FILENAME));

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer(new StreamSource(stylesheet));
            transformer.transform(new StreamSource(is), new StreamResult(outs));
            LOGGER.log(Level.INFO, "transformation succeed");
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "File not found during LaTeX export", e);
        } catch (TransformerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error while closing LaTeX export input stream", e);
                }
            }
            if (outs != null) {
                try {
                    outs.close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error while closing LaTeX export output stream", e);
                }
            }
            if (stylesheet != null) {
                try {
                    stylesheet.close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error while closing LaTeX export stylesheet stream", e);
                }
            }
        }
    }
}
