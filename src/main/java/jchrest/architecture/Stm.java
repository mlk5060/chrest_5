// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.architecture;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import jchrest.lib.HistoryTreeMap;
import jchrest.lib.Modality;

/**
 * Represents the short-term memory for a {@link jchrest.lib.Modality} of a 
 * {@link jchrest.architecture.Chrest} model, i.e. a place where pointers to
 * {@link jchrest.architecture.Node}s in long-term memory are stored (input to
 * long-term memory should be stored in structures akin to, for example, 
 * phonological loops for verbal input; not yet present in the CHREST 
 * architecture).
 * 
 * {@link jchrest.architecture.Stm} is, essentially, a finite-sized FIFO {@link 
 * java.util.List} however, it is actually a pseudo FIFO {@link java.util.List} 
 * since the pointer to the most informative {@link jchrest.architecture.Node} 
 * (the <i>hypothesis</i>) is always retained in the first index of the list 
 * (unless the {@link jchrest.architecture.Stm} is "manually" cleared). So, if a 
 * {@link jchrest.architecture.Node} is added to a {@link 
 * jchrest.architecture.Stm}, causing the size of the {@link 
 * jchrest.architecture.Stm} to exceed its maximum capacity, the {@link 
 * jchrest.architecture.Node}s in the elements of the {@link java.util.List} 
 * that are greater than the {@link jchrest.architecture.Stm}'s maximum capacity 
 * are removed.  However, if during this truncation process, the most 
 * informative {@link jchrest.architecture.Node} is removed, it is re-added as 
 * the first element in the {@link java.util.List} (and the {@link 
 * java.util.List} is again truncated to its maximum capacity).
 * 
 * @author Peter C. R. Lane
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class Stm implements Iterable<Node> {
  private final int _creationTime;
  private final Modality _modality;
  private final HistoryTreeMap _capacityHistory = new HistoryTreeMap();
  private final HistoryTreeMap _itemHistory = new HistoryTreeMap();

  /**
   * Initialises this {@link #this} with the capacity stipulated and no contents
   * at the time specified.
   * 
   * <b>NOTE:</b> The initial key values for any {@link 
   * jchrest.lib.HistoryTreeMap HistoryTreeMaps} of {@link #this} are set to 1 
   * less than the {@code time} specified so that they can be modified 
   * immediately when {@link #this} is created (if necessary).
   * 
   * @param model
   * @param modality
   * @param capacity
   * @param time
   */
  public Stm (Chrest model, Modality modality, int capacity, int time) {
    if(model.getCreationTime() <= time){
      this._creationTime = time;
      this._modality = modality;
      this._capacityHistory.put(time - 1, capacity);
      this._itemHistory.put(time - 1, new ArrayList());
    }
    else{
      throw new RuntimeException("Creation time specified for new Stm instance ("
        + time + ") is earlier than the creation time of the CHREST model "
        + "it will be associated with (" + model.getCreationTime() + ")"
      );
    }
  }
  
  /**************************/
  /**** SETTER FUNCTIONS ****/
  /**************************/
  
  /**
   * Creates a new state for {@link #this} by adding a new {@link 
   * jchrest.architecture.Node} reference to the "top" of {@link #this}'s
   * contents at the time specified if the following are all true:
   * <ul>
   *  <li>
   *    The {@link jchrest.architecture.Node} specified and {@link #this} were 
   *    created before the time specified
   *  </li>  
   *  <li>
   *    The {@link jchrest.architecture.Node} specified is of the same modality 
   *    as {@link #this}
   *  </li>
   *  <li>
   *    Adding the {@link jchrest.architecture.Node} specified will not rewrite
   *    the contents history of {@link #this}
   *  </li>
   * </ul>
   * 
   * The reference to the most informative {@link jchrest.architecture.Node} is 
   * maintained by re-adding it to the "top" of this {@link #this}'s contents 
   * (if lost due to the addition of the {@link jchrest.architecture.Node} 
   * specified).
   * 
   * @param nodeToAdd 
   * @param time The time that the {@link jchrest.architecture.Node} specified
   * should be added to this {@link this}'s contents.
   */
  boolean add(Node nodeToAdd, int time){
    
    if(
      this._creationTime <= time &&
      nodeToAdd.getCreationTime() <= time &&
      this._modality == nodeToAdd.getModality() &&
      !this._itemHistory.rewritingHistory(time)
    ){
      ArrayList<Node> newStmContents = new ArrayList();
      
      //Assume that the new node is the hypothesis before trying to find a more
      //informative node in the most recent state of this STM.
      Node hypothesis = nodeToAdd;
      for(Node node : (List<Node>)this._itemHistory.floorEntry(time).getValue() ) {
        
        //Update the hypothesis if a more informative node is found in context
        //of the same input.
        if(
          hypothesis.getContents().matches(node.getContents()) &&
          node.information(time) > hypothesis.information(time)
        ){
          hypothesis = node;
        }

        //Add the node from the current state of STM to the new STM contents
        //in order.
        newStmContents.add(node);
      }
    
      //Put the new node at the "top" of the new STM contents, and remove any 
      //duplicates of this node reference in the new STM contents.
      for(int i = 0; i < newStmContents.size(); i++){
        if(newStmContents.get(i) == nodeToAdd){
          newStmContents.remove(i);
        }
      }
      newStmContents.add(0, nodeToAdd);

      //Ensure the number of node references in the new STM state does not 
      //exceed the maximum capacity of STM at the current time by removing node
      //references from the "bottom" of the new STM contents.
      while(newStmContents.size () > this.getCapacity(time)) {
        newStmContents.remove(newStmContents.size() - 1);
      }

      //If the hypothesis has been lost due to the truncation of new STM 
      //contents above, add it back in to the "top" of the new STM contents (and
      //remove the "bottom" node reference to ensure the new contents does not
      //exceed this STM's capacity at the current time).
      if( !newStmContents.contains(hypothesis) ) {
        newStmContents.add(0, hypothesis);
        newStmContents.remove(newStmContents.size() - 1);
      }

      //Update the item history of this STM
      return (boolean)this._itemHistory.put(time, newStmContents);
    }
    
    return false;
  }
  
  /**
   * At the time specified, replace the {@link jchrest.architecture.Node} 
   * reference at the "top" of this {@link #this} (the hypothesis) with the
   * reference of the {@link jchrest.architecture.Node} passed.
   * 
   * @param replacement
   * @param time
   * @return A {@link java.lang.Boolean} that indicates whether the hypothesis
   * in this {@link #this} was successfully replaced.  The hypothesis will be
   * replaced and {@link java.lang.Boolean#TRUE} is returned if:
   * <ol type="1">
   *  <li>
   *    This {@link #this} exists at the time the hypothesis is to be replaced.
   *  </li>
   *  <li>
   *    The number of {@link jchrest.architecture.Node} references in this 
   *    {@link #this} is greater than 0 at the time the hypothesis is to be
   *    replaced.
   *  </li>
   * </ol>
   * If either of the conditions above are not true, the hypothesis is not 
   * replaced and {@link java.lang.Boolean#FALSE} is returned.
   */
  public boolean replaceHypothesis (Node replacement, int time) {
    if(
      this._creationTime <= time &&
      replacement.getCreationTime() <= time &&
      this._modality == replacement.getModality()
    ){
      List<Node> stmContentsAtTime = (List<Node>)this._itemHistory.floorEntry(time).getValue();
      if (stmContentsAtTime.size() > 0) {
        List<Node> newStmContents = new ArrayList();
        
        //Copy the current contents of STM and remove the top node reference.
        for(Node node : stmContentsAtTime){
          newStmContents.add(node);
        }
        newStmContents.remove(0);
      
        //Remove the replacement node reference in the new STM contents if it 
        //exists and add it back in at the top.
        if (newStmContents.contains(replacement)) {
          newStmContents.remove(replacement);
        }
        newStmContents.add(0, replacement);
        
        return (boolean)this._itemHistory.put(time, newStmContents);
      }
    }
    
    return false;
  }
  
  /**
   * Set the maximum capacity of {@link #this} to the value stipulated at the 
   * time specified if {@link #this} was created on or after the time specified
   * and setting a new capacity would not rewrite {@link #this}'s history.  
   * 
   * When the capacity is changed, the contents of this {@link #this} is 
   * cleared at the time specified.
   * 
   * @param newCapacity
   * @param time
   * @return {@link java.lang.Boolean#TRUE} if this {@link #this} exists at the
   * time this function is requested, {@link java.lang.Boolean#FALSE} if not.
   */
  public boolean setCapacity (int newCapacity, int time) {
    if(
      this._creationTime <= time &&
      !this._itemHistory.rewritingHistory(time) &&
      !this._capacityHistory.rewritingHistory(time)
    ){
      this._capacityHistory.put(time, newCapacity);
      this._itemHistory.put(time, new ArrayList());
      return true;
    }
    
    return false;
  }
  
  /**
   * Clears {@link #this} at the time specified.
   * 
   * @param time
   * 
   * @return {@link java.lang.Boolean#TRUE} if {@link #this} was successfully
   * cleared, {@link java.lang.Boolean#FALSE} if not, i.e. if any of the 
   * following are true:
   * <ul>
   *  <li>
   *    {@link #this} does not exist at the time specified.
   *  </li>
   *  <li>
   *    Clearing the contents of {@link #this} would rewrite its item history.
   *  </li>
   * </ul>
   */
  public boolean clear (int time) {
    if(this._creationTime <= time){
      return (boolean)this._itemHistory.put(time, new ArrayList());
    }
    return false;
  }
  
  /**************************/
  /**** GETTER FUNCTIONS ****/
  /**************************/

  /**
   * @param time
   * @return The maximum capacity of this {@link #this} at the time specified or
   * null if the time specified is before the time this {@link #this} was 
   * created.
   */
  public Integer getCapacity(int time) {
    return (time < this._creationTime) ? null : (Integer)this._capacityHistory.floorEntry(time).getValue();
  }
  
  /**
   * @param time
   * @return The contents of {@link #this} at the time specified.  If {@link 
   * #this} did not exist at the time specified, null is returned.
   */
  public List<Node> getContents(int time){
    Entry floorEntry = this._itemHistory.floorEntry(time);
    return floorEntry == null ? null : (List<Node>)floorEntry.getValue();
  }
  
  /**
   * @param time
   * @return The number of {@link jchrest.architecture.Node}s in {@link #this} 
   * at the time specified.  If {@link #this} did not exist at the time 
   * specified, null is returned.
   */
  public Integer getCount (int time) {
    List<Node> contentsAtTime = this.getContents(time);
    return contentsAtTime == null? null : contentsAtTime.size();
  }

  /**
   * @param index
   * @param time
   * @return The {@link jchrest.architecture.Node} at the index specified from 
   * {@link #this} at the time specified.  If {@link #this} did not exist at the 
   * time specified, null is returned.
   */
  public Node getItem (int index, int time) {
    List<Node> contentsAtTime = this.getContents(time);
    return contentsAtTime == null ? null : contentsAtTime.get(index);
  }
  
  Modality getModality(){
    return this._modality;
  }

  /********************************/
  /**** ITERATOR FUNCTIONALITY ****/
  /********************************/

  /** 
   * Support iteration over the most recent state of {@link #this}.
   */
  public Iterator<Node> iterator () {
    return new StmIterator ();
  }
  
  class StmIterator implements Iterator<Node> {
    private int _index = 0;
    private final List<Node> _items = new ArrayList();

    StmIterator () {
      for(Node node : (List<Node>)Stm.this._itemHistory.lastEntry().getValue()){
        _items.add(node);
      }
    }

    @Override
    public boolean hasNext () {
      return _index < _items.size ();
    }

    @Override
    public Node next () {
      if (hasNext ()) {
        _index += 1;
        return _items.get(_index-1);
      }
      throw new java.util.NoSuchElementException();
    }

    @Override
    public void remove () {
      throw new UnsupportedOperationException ();
    }
  }
}

