// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import javax.swing.*;
import javax.swing.border.TitledBorder;

/**
  * This panel displays the model's current clock time.
  *
  * @author Peter C. R. Lane
  */
public class ChrestTimeView extends JPanel {
  private final JLabel _display;

  public ChrestTimeView (Integer time) {
    super ();
    setBorder (new TitledBorder ("Clock (ms)"));
    _display = new JLabel ("" + time);
    _display.setToolTipText("Time CHREST finished learning in experiment.");
    add (_display);
  }

  public void update (Integer time) {
    _display.setText ("" + time);
  }
}
