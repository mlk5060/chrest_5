/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jchrest.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import jchrest.gui.Shell;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Handles reading in of data to use in CHREST experiments.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class InputOutput {
  
  public static PairedPattern generatePairedPattern (String input, boolean createVisualPatterns) throws IOException {
    
    String[] potentialPair = input.split (":");
    if (potentialPair.length != 2) throw new IOException (); // malformed pair
    ListPattern pat1;
    if (createVisualPatterns) {
      pat1 = Pattern.makeVisualList (potentialPair[0].trim().split("[, ]"));
    } else {
      pat1 = Pattern.makeVerbalList (potentialPair[0].trim().split("[, ]"));
    }
    pat1.setFinished ();
    ListPattern pat2 = Pattern.makeVerbalList (potentialPair[1].trim().split("[, ]"));
    pat2.setFinished ();

    return new PairedPattern (pat1, pat2);
  }
  
  public static List<ListPattern> readItems (BufferedReader input, boolean verbal) throws IOException {
    List<ListPattern> items = new ArrayList<ListPattern> ();
    String line = input.readLine ();

    while (line != null) {
      ListPattern pattern;
      if (verbal) {
        pattern = Pattern.makeVerbalList (line.trim().split("[, ]"));
      } else {
        pattern = Pattern.makeVisualList (line.trim().split("[, ]"));
      }
      pattern.setFinished ();
      items.add (pattern);
      line = input.readLine ();
    } 

    return items;
  }
  
  public static List<PairedPattern> readPairedItems (BufferedReader input, boolean createVisualPatterns) throws IOException {
    List<PairedPattern> items = new ArrayList<PairedPattern> ();
    String line = input.readLine ();
    while (line != null) {
      String[] pair = line.split (":");
      if (pair.length != 2) throw new IOException (); // malformed pair
      ListPattern pat1;
      if (createVisualPatterns) {
        pat1 = Pattern.makeVisualList (pair[0].trim().split("[, ]"));
      } else {
        pat1 = Pattern.makeVerbalList (pair[0].trim().split("[, ]"));
      }
      pat1.setFinished ();
      ListPattern pat2 = Pattern.makeVerbalList (pair[1].trim().split("[, ]"));
      pat2.setFinished ();
      items.add (new PairedPattern (pat1, pat2));

      line = input.readLine ();
    }

    return items;
  }
  
  public static boolean validateXmlInputData(Shell shell, String filepathToXmlInputData, String filepathToXmlInputDataSchema){
    
    try {
      // parse an XML document into a DOM tree
      DocumentBuilderFactory factoryA = DocumentBuilderFactory.newInstance();
      factoryA.setNamespaceAware(true);
      
      DocumentBuilder parser = factoryA.newDocumentBuilder();
      Document document = parser.parse(new File(filepathToXmlInputData));
      
      // create a SchemaFactory capable of understanding WXS schemas
      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    
      // load a WXS schema, represented by a Schema instance
      Source schemaFile = new StreamSource(new File(filepathToXmlInputDataSchema));
      Schema schema = factory.newSchema(schemaFile);
      
      // create a Validator instance, which can be used to validate an instance document
      Validator validator = schema.newValidator();
      validator.validate(new DOMSource(document));
      
    } catch (ParserConfigurationException ex) {
      Logger.getLogger(InputOutput.class.getName()).log(Level.SEVERE, null, ex);
      return false;
    } catch (SAXException | IOException ex) {
      JOptionPane.showMessageDialog (shell,
        "<html><body><p style='width: 400px;'>"
        + "<b>" + ex.getMessage() + "</b>" 
          + "<br/><br/><i>XML input:</i> '" + filepathToXmlInputData.replace("..", "chrest-dir") + "' "
          + "<br/><br/><i>XML schema:</i> '" + filepathToXmlInputDataSchema.replace("..", "chrest-dir") + "'"
          + "</p></body></html>", 
        "XML Validation Error",
        JOptionPane.ERROR_MESSAGE
      );
      Logger.getLogger(InputOutput.class.getName()).log(Level.SEVERE, null, ex);
      return false;
    }
    
    return true;
  }
}
