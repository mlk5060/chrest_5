// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import jchrest.architecture.Chrest;
import jchrest.architecture.Link;
import jchrest.architecture.Node;

public class NodeView extends JFrame implements java.util.Observer {
  private final Chrest _model;
  private final Node _node;
  private final int _time;
  private final JLabel _contentsLabel;
  private final JLabel _imageLabel;
  private final JLabel _associatedNode;
  private final JLabel _namedBy;
  private final DefaultListModel _childLinksView, _similarityLinksView;
  private final JList _childLinks, _similarityLinks;

  public NodeView (Chrest model, Node node, int time) {
    _model = model;
    _node = node;
    _node.addObserver (this);
    setTitle ("Node: " + _node.getReference ());
    addWindowListener (new WindowAdapter () {
      public void windowClosing (WindowEvent e) {
        ((NodeView)e.getWindow()).close ();
      }
    });
    _time = time;

    // create and add the widgets
    _contentsLabel = new JLabel (_node.getContents().toString ());
    _imageLabel = new JLabel (_node.getImage(_time).toString ());

    JPanel fields = new JPanel ();
    fields.setLayout (new GridLayout (4, 2));
    fields.add (new JLabel ("Contents: ", SwingConstants.RIGHT));
    fields.add (_contentsLabel);
    fields.add (new JLabel ("Image: ", SwingConstants.RIGHT));
    fields.add (_imageLabel);
    
    _associatedNode = new JLabel ("");
    _associatedNode.setBorder (new CompoundBorder (new EmptyBorder (3, 3, 3, 3), new EtchedBorder ()));
    Node associatedNode = _node.getAssociatedNode (_time);
    if (associatedNode != null) {
      _associatedNode.setIcon (new NodeIcon (associatedNode, _associatedNode, _time));
    }
    fields.add (new JLabel ("Assocated node: ", SwingConstants.RIGHT));
    fields.add (_associatedNode);

    _namedBy = new JLabel ("");
    _namedBy.setBorder (new CompoundBorder (new EmptyBorder (3, 3, 3, 3), new EtchedBorder ()));
    Node namedBy = _node.getNamedBy (_time);
    if (namedBy != null) {
      _namedBy.setIcon (new NodeIcon (namedBy, _namedBy, _time));
    }
    fields.add (new JLabel ("Named by: ", SwingConstants.RIGHT));
    fields.add (_namedBy);

    _childLinksView = new DefaultListModel ();
    _childLinks = new JList (_childLinksView);
    _childLinks.setCellRenderer (new ListNodeRenderer (_model, _time));
    _childLinks.setLayoutOrientation (JList.HORIZONTAL_WRAP);
    _childLinks.setVisibleRowCount (1);
    _childLinks.addMouseListener(new MouseAdapter () {
      public void mouseClicked(MouseEvent evt) {
        JList list = (JList)evt.getSource();
        if (evt.getClickCount() == 2) { 
          int index = list.locationToIndex(evt.getPoint());
          new NodeView (_model, (Node)_childLinksView.getElementAt (index), _time);
        }
      }
    });

    _similarityLinksView = new DefaultListModel ();
    _similarityLinks = new JList (_childLinksView);
    _similarityLinks.setCellRenderer (new ListNodeRenderer (_model, _time));
    _similarityLinks.setLayoutOrientation (JList.HORIZONTAL_WRAP);
    _childLinks.setVisibleRowCount (1);
    _similarityLinks.addMouseListener(new MouseAdapter () {
      public void mouseClicked(MouseEvent evt) {
        JList list = (JList)evt.getSource();
        if (evt.getClickCount() == 2) { 
          int index = list.locationToIndex(evt.getPoint());
          new NodeView (_model, (Node)_similarityLinksView.getElementAt (index), _time);
        }
      }
    });

    setLayout (new BorderLayout ());
    add (fields, BorderLayout.NORTH);
    add (new JScrollPane (_childLinks));
    add (new JScrollPane (_similarityLinks), BorderLayout.SOUTH);

    pack ();
    setVisible (true);

    updateDisplays ();
  }

  void close () {
    _node.deleteObserver (this);
    
    Node associatedNode = _node.getAssociatedNode (_time);
    if (associatedNode != null) {
      associatedNode.deleteObserver (this);
    }
    
    Node namedBy = _node.getNamedBy (_time);
    if (namedBy != null) {
      namedBy.deleteObserver (this);
    }
    
    List<Link> children = _node.getChildren (_time);
    if(children != null){
      for (Link link : children) {
        link.getChildNode().deleteObserver (this);
      }
    }
    
    List<Node> semanticLinks = _node.getSemanticLinks (_time);
    if(semanticLinks != null){
      for (Node node : semanticLinks) {
        node.deleteObserver (this);
      }
    }
    
    setVisible (false);
    dispose ();
  }

  private void updateDisplays () {
    _imageLabel.setText (_node.getImage(_time).toString ());
    
    Node associatedNode = _node.getAssociatedNode (_time);
    if (associatedNode != null) {
      _associatedNode.setIcon (new NodeIcon (associatedNode, _associatedNode, _time));
      associatedNode.addObserver (this);
    }
    
    Node namedBy = _node.getNamedBy (_time);
    if (namedBy != null) {
      _namedBy.setIcon (new NodeIcon (namedBy, _namedBy, _time));
      namedBy.addObserver (this);
    }

    _childLinksView.clear ();
    List<Link> children = _node.getChildren (_time);
    if(children != null){
      for (Link link: children) {
        _childLinksView.addElement (link.getChildNode ());
        link.getChildNode().addObserver (this);
      }
    }
    _childLinks.setModel (_childLinksView);

    _similarityLinksView.clear ();
    List<Node> semanticLinks = _node.getSemanticLinks (_time);
    if(semanticLinks != null){
      for (Node node : semanticLinks) {
        _similarityLinksView.addElement (node);
        node.addObserver (this);
      }
    }
    _similarityLinks.setModel (_similarityLinksView);
  }

  public void update (java.util.Observable o, Object arg) {
    if (arg instanceof String && ((String)arg).equals("close")) {
      close ();
    } else {
      // update displays
      updateDisplays ();
    }
  }
}

