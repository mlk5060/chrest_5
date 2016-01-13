// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import jchrest.architecture.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class ListNodeRenderer extends JLabel implements ListCellRenderer {
  private Chrest _model;
  private int _time;

  ListNodeRenderer (Chrest model, int time) {
    _model = model;
    _time = time;
  }

  public Component getListCellRendererComponent (
      JList list,
      Object value,
      int index,
      boolean isSelected,
      boolean cellHasFocus) {
    JLabel cell = new JLabel ("");
    cell.setBorder (new CompoundBorder (new EmptyBorder (3, 3, 3, 3), new EtchedBorder ()));
    cell.setIcon (new NodeIcon ((Node)value, list, _time));

    return cell;

      }
}

