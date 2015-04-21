// Copyright (c) 2014, Martyn Lloyd-Kelly
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import org.jsoup.Jsoup;

/**
 * Contains methods for exporting CHREST data.
 * 
 * @author martyn
 */
public class ExportData {
  
  /**
   * Allows the user to save data in the directory specified.
   * 
   * @param anchorPoint The {@link java.awt.Component} that the save dialog will
   * be anchored to and inherit its look and feel from (can be null).
   * @param directoryName The name of the directory that the data will be saved
   * in.
   * @param dataToSave Should contain three pieces of information in the 
   * following order:
   * <ol>
   *  <li>The data to save</li>
   *  <li>The filename for this data</li>
   *  <li>The file's extension type (txt, csv etc. Don't include the preceeding
   *  dot)</li>
   * <ol>
   */
  public static void saveFile(Component anchorPoint, String directoryName, ArrayList<ArrayList<String>> dataToSave){
    
    //Check that each piece of data to be saved has the necessary three parts, 
    //if not, display a warning to the user and abort execution of the method.
    for(int i = 0; i < dataToSave.size(); i++){
      int dataSize = dataToSave.get(i).size();
      
      if( dataSize != 3 ){
        JOptionPane.showMessageDialog(anchorPoint, "There are " + dataSize + " pieces of information for item " + i + " to be saved.  Please rectify so that 3 pieces \n"
          + "of information (data to be written to file, filename and file extension) are specified to continue.");
        return;
      }
    }
    
    //Create a "Save File" dialog that can only list directories since the 
    //user should select an existing directory to save data in not a file.
    final JFileChooser fc = new JFileChooser();
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    int resultOfFileSelect = fc.showSaveDialog(anchorPoint);

    //If the user selected a directory, store this directory as an object
    //and extract its absolute path as a string.  Otherwise, do nothing.
    if (resultOfFileSelect == JFileChooser.APPROVE_OPTION) {
      File directoryToCreateResultDirectoryIn = fc.getSelectedFile();
      String directoryToCreateResultDirectoryInAbsPath = directoryToCreateResultDirectoryIn.getAbsolutePath();

      //Check that the directory specified by the user can be written and
      //executed, if not, display an error informing the user that the 
      //directory specified has incorrect permissions set that prevent this
      //function from executing further.
      if (directoryToCreateResultDirectoryIn.canWrite() && directoryToCreateResultDirectoryIn.canExecute()) {

        //Create a new file object for the directory that is to be created 
        //inside the directory that the user has specified.  Creation of a 
        //file to store data prevents related data from becoming disjoint in the 
        //file system (inconvenient for the user).
        File directoryToStoreData = new File(directoryToCreateResultDirectoryInAbsPath + File.separator + directoryName);

        //Check that the default directory name doesn't already exist.  If it
        //does, append a number to the end of the directory name and check
        //again.  If the directory name doesn't exist, create the directory
        //and set permissions to allow data to be written to files within it.
        int i = 0;
        while (directoryToStoreData.exists()) {
          i++;
          directoryToStoreData = new File(directoryToCreateResultDirectoryInAbsPath + File.separator + directoryName + i);
        }
        directoryToStoreData.mkdir();
        directoryToStoreData.setExecutable(true, true);
        directoryToStoreData.setWritable(true, true);

        //Extract absolute path to the directory that will contain the data 
        //files and create the path names for the data files.
        String directoryToStoreDataAbsPath = directoryToStoreData.getAbsolutePath();
        for(ArrayList<String> data : dataToSave){
        
          String fileAbsPath = directoryToStoreDataAbsPath + File.separator + data.get(1) + "." + data.get(2);

          //Create the data file and set read, write and execute permissions
          //so that data can be written to it.
          File file = new File(fileAbsPath);

          try {
            file.createNewFile();
          } catch (IOException ex) {
            Logger.getLogger(PairedAssociateInterface.class.getName()).log(Level.SEVERE, null, ex);
          }

          file.setWritable(true, true);
          file.setReadable(true, true);
          file.setExecutable(true, true);


          //Write data to data file.
          try (PrintWriter filePrintWriter = new PrintWriter(file)) {
            filePrintWriter.write(data.get(0));
          } catch (FileNotFoundException ex) {
            Logger.getLogger(PairedAssociateInterface.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      } else {
        JOptionPane.showMessageDialog(null, "Directory '" + directoryToCreateResultDirectoryInAbsPath + "' does not have write and/or execute privileges", "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }
  
  /**
   * Converts JTable data into CSV format by first extracting column headers 
   * then data on a per-row basis.
   *
   * @param table The JTable whose model data is to be converted into CSV.
   * @return CSV string of the model data of the JTable passed.
   */
  public static String extractJTableDataAsCsv(JTable table) {
    AbstractTableModel atm = (AbstractTableModel) table.getModel();
    int nRow = atm.getRowCount();
    int nCol = atm.getColumnCount();
    String tableData = "";

    for (int col = 0; col < nCol; col++) {
      tableData += "," + Jsoup.parse(atm.getColumnName(col)).text();
    }
    tableData += "\n";

    for (int row = 0; row < nRow; row++) {
      for (int col = 0; col < nCol; col++) {
        tableData += "," + Jsoup.parse(atm.getValueAt(row, col).toString()).text();
      }
      tableData += "\n";
    }

    return tableData.replaceAll("^,", "").replaceAll("\n,", "\n");
  }
}
