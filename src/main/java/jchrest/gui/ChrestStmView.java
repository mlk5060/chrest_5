// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import jchrest.architecture.*;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import jchrest.lib.Modality;

public class ChrestStmView extends JPanel {
  private Chrest _model;
  private DefaultListModel _visualStmView, _verbalStmView;
  private JList _visualStmList, _verbalStmList;
  
  //Holds the time that the state of the STM to be drawn should represent.
  private Integer _stateAtTimeValue;

  public ChrestStmView (Chrest model, int time) {
    super ();

    //Associated model variable set-up.
    _model = model;
    
    //Set the following variable value before the STM view is constructed since 
    //its construction depends on the value being set correctly.
    _stateAtTimeValue = time; 
    
    //Visual STM set-up.
    setLayout (new GridLayout (1, 1));
    JPanel visualPanel = new JPanel ();
    visualPanel.setLayout (new GridLayout (1, 1));
    visualPanel.setBorder (new TitledBorder ("Visual STM"));
    _visualStmView = new DefaultListModel ();
    _visualStmList = new JList (_visualStmView);
    _visualStmList.setCellRenderer (new ListNodeRenderer (_model, _stateAtTimeValue));
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
          new NodeView (_model, (Node)_visualStmView.getElementAt (index), time);
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
    _verbalStmList.setCellRenderer (new ListNodeRenderer (_model, _stateAtTimeValue));
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
          new NodeView (_model, (Node)_verbalStmView.getElementAt (index), ChrestStmView.this._stateAtTimeValue);
        }
      }
    });
    verbalPanel.add (new JScrollPane (_verbalStmList));
  
    //Final set-up.
    JSplitPane jsp = new JSplitPane (JSplitPane.VERTICAL_SPLIT, visualPanel, verbalPanel);
    jsp.setOneTouchExpandable (true);
    add (jsp);

    update (_model.getMaximumClockValue());
  }

  public void update (int time) {
    _verbalStmView.clear();
    _visualStmView.clear();
    
    List<Node> verbalStm = this._model.getStm(Modality.VERBAL).getContents(time);
    List<Node> visualStm = this._model.getStm(Modality.VISUAL).getContents(time);

    for(Node verbalStmNode : verbalStm){
      _verbalStmView.addElement(verbalStmNode);
    }

    for(Node visualStmNode : visualStm){
      _visualStmView.addElement(visualStmNode);
    }
    
    _verbalStmList.setModel (_verbalStmView);
    _visualStmList.setModel (_visualStmView);
  }
}

