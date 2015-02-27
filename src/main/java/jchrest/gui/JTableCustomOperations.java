/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jchrest.gui;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

/**
 * Class containing useful non-native Java functionality for JTables.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class JTableCustomOperations {
  
  /**
   * Resizes all columns in a table to be wide enough to display the content
   * in each column's widest cell.
   * 
   * @param table 
   */
  public static void resizeColumnsToFitWidestCellContentInColumn(JTable table){
    TableColumnModel columnModel = table.getColumnModel();
    for (int column = 0; column < table.getColumnCount(); column++) {
      //int width = 0; // Min width
      int width = table.getTableHeader().getHeaderRect(column).width; //Min width
      for (int row = 0; row < table.getRowCount(); row++) {
        TableCellRenderer renderer = table.getCellRenderer(row, column);
        Component comp = table.prepareRenderer(renderer, row, column);
        width = Math.max(comp.getPreferredSize().width, width);
      }
      columnModel.getColumn(column).setPreferredWidth(width);
    }
  }
}
