// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import jchrest.architecture.*;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.*;
import javax.swing.border.*;
import jchrest.lib.ItemSquarePattern;
import jchrest.lib.ListPattern;
import jchrest.lib.Modality;

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
    _stateAtTimeValue = _model.getAttentionClock(); //This must be set before the STM view is constructed since its construction depends on the value being set correctly.
    
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

    update (_model.getAttentionClock(), false);
  }

  public void update (int stateAtTime, boolean historicalSearch) {
    _verbalStmView.clear();
    _visualStmView.clear();
    
    if(historicalSearch){
      //Since all Nodes in LTM will have been cloned thanks to the 
      //ChrestView.update() method at this stage and since the Node.deepClone()
      //method clones Nodes so that their state reflects the original Node's 
      //state at the time specified (in this case, "stateAtTime"), we just need 
      //to fetch the clones and add them to the respective STM view.
      
      Entry<Integer, java.util.List<Integer>> verbalStmContents = _model.getVerbalStm().getStateAtTime(stateAtTime);
      if(verbalStmContents != null){
        Iterator<Integer> verbalStmContentIterator = verbalStmContents.getValue().iterator();
        while(verbalStmContentIterator.hasNext()){
          _verbalStmView.addElement( Node.searchForNodeFromBaseNode(verbalStmContentIterator.next(), this._model.getLtmByModality(Modality.VERBAL)).getClone() );
        }
      }
      
      Entry<Integer, java.util.List<Integer>> visualStmContents = _model.getVisualStm().getStateAtTime(stateAtTime);
      if(visualStmContents != null){
        Iterator<Integer> visualStmContentIterator = visualStmContents.getValue().iterator();
        while(visualStmContentIterator.hasNext()){
          _visualStmView.addElement( Node.searchForNodeFromBaseNode(visualStmContentIterator.next(), this._model.getLtmByModality(Modality.VISUAL)).getClone() );
        }
      }
    }
    else{
      Iterator<Node> verbalStm = _model.getVerbalStm().iterator();
      Iterator<Node> visualStm = _model.getVisualStm().iterator();
      
      while(verbalStm.hasNext()){
        _verbalStmView.addElement(verbalStm.next());
      }
      
      while(visualStm.hasNext()){
        _visualStmView.addElement (visualStm.next());
      }
    }
    
    _verbalStmList.setModel (_verbalStmView);
    _visualStmList.setModel (_visualStmView);
  }
}

