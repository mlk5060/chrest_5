/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jchrest.gui;

import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Class containing useful non-native Java functionality for JTables.
 * 
 * @author Martyn Lloyd-Kelly {@code <martynlk@liverpool.ac.uk>}
 */
public class JTableCustomOperations {
  
  /**
   * Resizes all columns in a table to be wide enough to display the content
   * in each column's widest cell.
   * 
   * @param table 
   */
  public static void resizeColumnsToFitWidestCellContentInColumn(JTable table){
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    DefaultTableColumnModel colModel = (DefaultTableColumnModel)table.getColumnModel();
    
    for (int column = 0; column < table.getColumnCount() && table.getColumnCount() > 0; column++) {
      TableColumn col = colModel.getColumn(column);
      int width = 0;

      // Get width of column header
      TableCellRenderer renderer = col.getHeaderRenderer();
      if (renderer == null) {
        renderer = table.getTableHeader().getDefaultRenderer();
      }
      java.awt.Component comp = renderer.getTableCellRendererComponent(
        table, col.getHeaderValue(), false, false, 0, 0);
      width = comp.getPreferredSize().width;

      // Get maximum width of column data
      for (int row = 0; row < table.getRowCount(); row++) {
        comp = renderer.getTableCellRendererComponent(
            table, table.getValueAt(row, column), false, false, row, column);
        width = Math.max(width, comp.getPreferredSize().width);
      }

      // Add margin
      width += 2*5;

      // Set the width
      col.setPreferredWidth(width);
    }
  }
}
