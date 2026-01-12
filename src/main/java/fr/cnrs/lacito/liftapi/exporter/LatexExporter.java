package fr.cnrs.lacito.liftapi.exporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

//import net.sf.saxon.TransformerFactoryImpl;

public class LatexExporter {

    private static final Logger LOGGER = Logger.getLogger(LatexExporter.class.getName());
    private final static String STYLESHEET_FILENAME= "";

    private File inputFile;
    private File outFile;

    public void transform2Latex(File outfile) {
        // try (
        //     InputStream is = new FileInputStream(this.file);
        //     OutputStream outs = new FileOutputStream(outfile);
        //     InputStream stylesheet = LiftDocument.class.getResourceAsStream(STYLESHEET_FILENAME);
        //         ) {
        //             System.out.println("foo");
        // } catch (FileNotFoundException e) {
        //     Alert alert = new Alert(AlertType.WARNING);
        //     alert.setTitle("Impossible d'exécuter la transformation");
        //     alert.setHeaderText(null);
        //     alert.setContentText("The source file does not exist: " + outfile);
        //     alert.showAndWait();
        //     return;
        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

        InputStream is = null;
        try {
            is = new FileInputStream(this.inputFile);
        } catch (FileNotFoundException e) {
            // Alert alert = new Alert(AlertType.WARNING);
            // alert.setTitle("Impossible d'exécuter la transformation");
            // alert.setHeaderText(null);
            // alert.setContentText("The source file does not exist: " + outfile);
            // alert.showAndWait();
            return;
        }
        LOGGER.info("Loading dictionary: " + outfile);

        OutputStream outs = null;
        try {
            outs = new FileOutputStream(outfile);
        } catch (FileNotFoundException e) {
            // Alert alert = new Alert(AlertType.WARNING);
            // alert.setTitle("Impossible d'exécuter la transformation");
            // alert.setHeaderText(null);
            // alert.setContentText("Can't create the output file");
            // alert.showAndWait();
            return;
        }
        LOGGER.info("Outputstream opened: " + outfile.toString());

        InputStream stylesheet = LatexExporter.class.getResourceAsStream(STYLESHEET_FILENAME);
        LOGGER.info("Stylesheet loaded: " + LatexExporter.class.getResource(STYLESHEET_FILENAME).toString());

        TransformerFactory tf;
        tf = TransformerFactory.newInstance();
        try {
            Transformer transformer = tf.newTransformer(new StreamSource(stylesheet));
            transformer.transform(new StreamSource(is), new StreamResult(outs));
        } catch (TransformerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            // Alert alert = new Alert(AlertType.ERROR);
            // alert.setTitle("Erreur lors de l'exécution de la transformation");
            // alert.setHeaderText(null);
            // alert.setContentText(e.getMessageAndLocation());
            // alert.showAndWait();
            return;
        }

        LOGGER.log(Level.INFO, "transformation succeed");
        // Alert alert = new Alert(AlertType.INFORMATION);
        // alert.setTitle("Transformation succeed");
        // alert.setHeaderText(null);
        // alert.setContentText("Transformation succeed");
        // alert.showAndWait();

        try {
            is.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        try {
            outs.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            stylesheet.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
