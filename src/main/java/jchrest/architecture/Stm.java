// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.architecture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Class manages the short-term memory for one modality of a Chrest model.
 * Each short-term memory has a maximum capacity, and stores a list of nodes.
 */
public class Stm implements Iterable<Node> {
  private int _size;
  private List<Node> _items;
  
  //Stores times as keys and Node references as values in the order that the
  //Node instances appear in STM.  This is used to draw STM correctly when 
  //a historical state of CHREST is requested.
  private TreeMap<Integer, List<Integer>> _history;

  /**
   * Constructor requires the maximum capacity to be set.
   */
  public Stm (int size, int domainTime) {
    _size = size;
    _items = new ArrayList<Node> ();
    _history = new TreeMap<>();
  }

  /**
   * Accessor for the maximum capacity.
   */
  public int getSize () {
    return _size;
  }

  /**
   * Alter the maximum capacity.  When the size is changed, all items in the 
   * short-term memory are cleared.
   */
  public void setSize (int size) {
    _size = size;
    _items.clear ();
  }

  /**
   * Return a count of how many items are actually in the short-term memory.
   */
  public int getCount () {
    return _items.size ();
  }

  /**
   * Retrieve a node within the short-term memory by its index position.
   * There is no error checking on the retrieval.
   */
  public Node getItem (int index) {
    return _items.get (index);
  }

  /**
   * When adding a new node to STM, the new node is added to the top of STM 
   * with the queue cut at the bottom to keep STM to the fixed size constraints.
   * However, the most informative node is maintained in the list, by re-adding 
   * it to STM, if lost.
   */
  public void add (Node node, int time) {
    
    // find the most informative node which also matches this node's contents
    Node hypothesis = node;
    for (Node check : _items) {
      if(
        hypothesis.getContents ().matches (check.getContents ()) &&
        check.information () > hypothesis.information ()
      ) {
        hypothesis = check;
      }
    }
    // put this node at the front of STM, and remove any duplicate
    _items.remove (node);
    _items.add (0, node);

    // truncate STM to be of at most _size elements
    while (_items.size () > _size) {
      int lastItemIndex = _items.size () - 1;
      _items.remove (lastItemIndex);
    }
    
    // if most informative node not in STM, then add it back in to top
    if (!_items.contains (hypothesis)) {
      int lastItemIndex = _items.size () - 1;
      _items.remove (_items.size () - 1); // losing bottom item
      _items.add (0, hypothesis);
    }
    
    this.addHistoryEntry(time);
  }

  /**
   * Replace the topmost (hypothesis) node with the given one.
   */
  public void replaceHypothesis (Node node, int time) {
    if (_items.size () > 0) {
      _items.remove (0);
    }
    // make sure there are no duplicates
    if (_items.contains (node)) {
      _items.remove (node);
    }
    // add to top of STM
    _items.add (0, node);
    this.addHistoryEntry(time);
  }

  /**
   * Remove all items from STM.
   */
  public void clear (int time) {
    _items.clear ();
    this.addHistoryEntry(time);
  }

  /**
   * Add a lateral link indicating that the second node in this STM 
   * is associated with the top node.  The link is only added if not already 
   * present, and the model's clock is advanced by the time to add a link.
   * Returns boolean to indicate if learning occurred or not.
   */
  public boolean learnLateralLinks (Chrest model, int time) {
    if (
      _items.size () >= 2 && 
      _items.get(1).getAssociatedNode () != _items.get(0)
    ){
      _items.get(1).setAssociatedNode (_items.get(0), time);
      model.advanceAttentionClock (model.getAddLinkTime ());
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Adds an entry to the history of this STM.
   * 
   * @param time 
   */
  private void addHistoryEntry(int time){
    Iterator<Node> stmIterator = this._items.iterator();
    List<Integer> stmNodeReferences = new ArrayList<>();
    while(stmIterator.hasNext()){
      stmNodeReferences.add( new Integer(stmIterator.next().getReference()) );
    }
    this._history.put(time, stmNodeReferences);
  }
  
  public Entry<Integer, List<Integer>> getStateAtTime(int time){
    return this._history.floorEntry(time);
  }

  /** 
   * Support iteration over the nodes in STM.
   */
  public Iterator<Node> iterator () {
    return new StmIterator (_items);
  }
  
  class StmIterator implements Iterator<Node> {
    private int _index = 0;
    private List<Node> _items;

    StmIterator (List<Node> items) {
      _items = items;
    }

    public boolean hasNext () {
      return _index < _items.size ();
    }

    public Node next () {
      if (hasNext ()) {
        _index += 1;
        return _items.get(_index-1);
      }
      throw new java.util.NoSuchElementException();
    }

    public void remove () {
      throw new UnsupportedOperationException ();
    }
  }
}

