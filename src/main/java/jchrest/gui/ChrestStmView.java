// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import jchrest.architecture.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class ChrestStmView extends JPanel {
  private Chrest _model;
  private DefaultListModel _visualStmView, _verbalStmView;
  private JList _visualStmList, _verbalStmList;
  
  //Holds the time that the state of the STM to be drawn should represent.
  private Integer _stateAtTimeValue;

  public ChrestStmView (Chrest model) {
    super ();

    //Associated model variable set-up.
    _model = model;
    _stateAtTimeValue = _model.getLearningClock(); //This must be set before the STM view is constructed since its construction depends on the value being set correctly.
    
    //Visual STM set-up.
    setLayout (new GridLayout (1, 1));
    JPanel visualPanel = new JPanel ();
    visualPanel.setLayout (new GridLayout (1, 1));
    visualPanel.setBorder (new TitledBorder ("Visual STM"));
    _visualStmView = new DefaultListModel ();
    _visualStmList = new JList (_visualStmView);
    _visualStmList.setCellRenderer (new ListNodeRenderer (_model));
    _visualStmList.addMouseListener(new MouseAdapter() {
      
      /**
       * Whenever a node is double-clicked in the visual STM view, a new window
       * pops up containing detailed information about the node.
       * 
       * @param evt 
       */
      public void mouseClicked(MouseEvent evt) {
        JList list = (JList)evt.getSource();
        if (evt.getClickCount() == 2) { 
          int index = list.locationToIndex(evt.getPoint());
          new NodeView (_model, (Node)_visualStmView.getElementAt (index));
        }
      }
    });
    visualPanel.add (new JScrollPane (_visualStmList));

    //Verbal STM set-up.
    JPanel verbalPanel = new JPanel ();
    verbalPanel.setLayout (new GridLayout (1, 1));
    verbalPanel.setBorder (new TitledBorder ("Verbal STM"));
    _verbalStmView = new DefaultListModel ();
    _verbalStmList = new JList (_verbalStmView);
    _verbalStmList.setCellRenderer (new ListNodeRenderer (_model));
    _verbalStmList.addMouseListener(new MouseAdapter() {
      
      /**
       * Whenever a node is double-clicked in the verbal STM view, a new window
       * pops up containing detailed information about the node.
       * 
       * @param evt 
       */
      public void mouseClicked(MouseEvent evt) {
        JList list = (JList)evt.getSource();
        if (evt.getClickCount() == 2) { 
          int index = list.locationToIndex(evt.getPoint());
          new NodeView (_model, (Node)_verbalStmView.getElementAt (index));
        }
      }
    });
    verbalPanel.add (new JScrollPane (_verbalStmList));
  
    //Final set-up.
    JSplitPane jsp = new JSplitPane (JSplitPane.VERTICAL_SPLIT, visualPanel, verbalPanel);
    jsp.setOneTouchExpandable (true);
    add (jsp);

    update ();
  }

  public void update () {
    
    _visualStmView.clear ();
    for (Node node : _model.getVisualStm ()) {
      if(node.getCreationTime() <= this._stateAtTimeValue){
        _visualStmView.addElement (node);
      }
    }
    _visualStmList.setModel (_visualStmView);

    _verbalStmView.clear ();
    for (Node node : _model.getVerbalStm ()) {
      if(node.getCreationTime() <= this._stateAtTimeValue){
        _verbalStmView.addElement (node);
      }
    }
    _verbalStmList.setModel (_verbalStmView);
  }
}

