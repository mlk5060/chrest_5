// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.architecture;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jchrest.lib.ExecutionHistoryOperations;

import jchrest.lib.ItemSquarePattern;
import jchrest.lib.ListPattern;
import jchrest.lib.Modality;
import jchrest.lib.PrimitivePattern;
import jchrest.lib.ReinforcementLearning;

/**
 * Represents a node within the model's long-term memory discrimination network.
 * Methods support learning and also display.
 * 
 * Nodes maintain a history of themselves to support CHREST's operation in 
 * simulations where an external clock to CHREST is used.
 *
 * @author Peter C. R. Lane
 */
public class Node extends Observable {

  /****************************************************************************/
  /****************************************************************************/
  /*********************** CONSTANT INSTANCE VARIABLES  ***********************/
  /****************************************************************************/
  /****************************************************************************/
  
  //The variables listed in this section stay consistent throughout the Node's
  //life-cycle.
  private final ListPattern _contents; //The test-link trail that leads to this 
                                       //Node in LTM.
  private final int _creationTime;
  private final Chrest _model;
  private final int _reference;
  
  /****************************************************************************/
  /****************************************************************************/
  /************************ DYNAMIC INSTANCE VARIABLES ************************/
  /****************************************************************************/
  /****************************************************************************/
  
  //The variables listed in this section can change throughout the Node's 
  //life-cycle.
  private HashMap<Node, Double> _productions;
  private Node _associatedNode;
  private List<Link> _children;
  private List<ItemSquarePattern> _filledItemSlots;
  private List<ItemSquarePattern> _filledPositionSlots;
  private ListPattern _image;
  private List<ItemSquarePattern> _itemSlots;
  private Node _namedBy;
  private List<ItemSquarePattern> _positionSlots;
  private List<Node> _semanticLinks;
  private Node _clone;
  
  /****************************************************************************/
  /****************************************************************************/
  /************************ HISTORY INSTANCE VARIABLES ************************/
  /****************************************************************************/
  /****************************************************************************/
 
  // ===========================================================================
  // ================================ IMPORTANT ================================
  // ===========================================================================
  //
  // When declaraing a new history variable, please ensure that its instance
  // variable name ends with "History".  This will ensure that automated 
  // operations on history variables using Java refelection will work with new
  // variables without having to implement specific code for the new variable.
  //
  // ===========================================================================
  // ===========================================================================
  // ===========================================================================
  //
  //All of the history variables have keys that are timestamps (domain time) 
  //hence the use of TreeMap since it is possible to sort and retrieve keys 
  //in some order.  These  variables are used by Node.deepClone() to instantiate 
  //cloned Node instances if a historical clone is requested.
  //
  //The keys for all instance variable history variables are domainTimes that 
  //indicate the contents of the history variable at the time indicated by the
  //key until the next entry in the history variable.  
  //
  //The values for the following history variables are either individual or 
  //multiple Node references.  These are used to indicate what Nodes were 
  //present in the instance variable they keep a history for from the domain 
  //time indicated by the respective key.
  private TreeMap<Integer, HashMap<Integer, Double>> _productionHistory = new TreeMap<>();
  private TreeMap<Integer, Integer> _associatedNodeHistory = new TreeMap<>();
  private TreeMap<Integer, Integer> _namedByHistory = new TreeMap<>();
  private TreeMap<Integer, List<Integer>> _semanticLinksHistory = new TreeMap<>();
  
  //The values for the following history variables are single or multiple 
  //instances of ListPattern or ItemSquarePattern objects.  These are used to 
  //indicate what instances were present in the instance variable they keep a 
  //history for from the domain time indicated by the respective key.
  private TreeMap<Integer, ListPattern> _imageHistory = new TreeMap<>();
  private TreeMap<Integer, List<ItemSquarePattern>> _itemSlotsHistory = new TreeMap<>();
  private TreeMap<Integer, List<ItemSquarePattern>> _positionSlotsHistory = new TreeMap<>();
  
  //The childrenHistory variable stores the information required to construct a
  //link instance since it is most efficient to clone a link and its child node
  //when required otherwise, many similar Node objects would be cloned, wasting
  //memory.
  private TreeMap<Integer, List<List<Object>>> _childrenHistory = new TreeMap<>();
  
  /****************************************************************************/
  /****************************************************************************/
  /******************************** FUNCTIONS *********************************/
  /****************************************************************************/
  /****************************************************************************/

  /**
   * Constructor to construct a new root node for the model.  
   */
  public Node (Chrest model, int reference, ListPattern type, int domainTime) {
    this (model, reference, type, type, domainTime);
  }
 
  /**
   * When constructing non-root nodes in the network, the new contents and image 
   * must be defined.  Assume that the image always starts empty.
   */
  public Node (Chrest model, ListPattern contents, ListPattern image, int domainTime) {
    this (model, model.getNextNodeNumber (), contents, image, domainTime);
  }

  /**
   * Constructor to build a new Chrest node with given reference, contents and image.
   * Package access only, as should only be used by Chrest.java.
   */
  Node (Chrest model, int reference, ListPattern contents, ListPattern image, int creationTime) {
    _model = model;
    _reference = reference;
    _contents = contents.clone ();
    _image = image; //The contents of the chunk inside the node.
    _children = new ArrayList<Link> ();
    _semanticLinks = new ArrayList<Node> ();
    _associatedNode = null;
    _namedBy = null;
    _productions = new HashMap<>();
    _creationTime = creationTime;
    
    
    //Set-up history variables except for STM history since this should only be
    //modified when the Node is input/output of STM and this happens 
    //independently of Node creation.
    this.updateProductionHistory(creationTime);
    this.updateAssociatedNodeHistory(creationTime);
    this.updateChildHistory(creationTime);
    this.updateImageHistory(creationTime);
    this.updateItemSlotHistory(creationTime);
    this.updatePositionSlotHistory(creationTime);
    this.updateSemanticLinkHistory(creationTime);
  }
  
  /**
   * @return The productions that existed for this {@link #this} at the time 
   * specified.  If no productions were present at the time specified then null 
   * is returned. 
   */
  private HashMap<Integer, Double> getProductionsAtTime(int time){
    return this._productionHistory.floorEntry(time).getValue();
  }
  
  /**
   * Returns the reference of the Node that was set as this Node instance's 
   * associated node at the time specified.
   * 
   * @param time
   * 
   * @return An Integer representing the reference of the Node that this Node
   * was associated with at the time specified.  If no Node was associated with
   * this Node at the time specified then null is returned. 
   */
  private Integer getAssociatedNodeAtTime(int time){
    return this._associatedNodeHistory.floorEntry(time).getValue();
  }
  
  /**
   * Returns the information required to construct a child of this Node at the 
   * time specified.
   * 
   * @param time
   * 
   * @return A List of Object instances containing the information required to
   * construct a Link instance i.e. test pattern, child node reference, link 
   * creation time. If this Node had no children at the time specified then null 
   * is returned. 
   */
  private List<List<Object>> getChildrenAtTime(int time){
    return this._childrenHistory.floorEntry(time).getValue();
  }
  
  /**
   * Returns the image of this Node at the time specified.
   * 
   * @param time
   * 
   * @return A ListPattern representing the state of this Node's image at the
   * time specified.  If this Node did not have an image at the time specified 
   * then null is returned. 
   */
  private ListPattern getImageAtTime(int time){
    return this._imageHistory.floorEntry(time).getValue();
  }
  
  /**
   * Returns the contents of this Node's item slots at the time specified.
   * 
   * @param time
   * 
   * @return A List of ItemSquarePattern instances representing this Node's item
   * slot contents at the time specified.  If this Node had no item slots at the 
   * time specified then null is returned. 
   */
  private List<ItemSquarePattern> getItemSlotsAtTime(int time){
    return this._itemSlotsHistory.floorEntry(time).getValue();
  }
  
  /**
   * Returns the reference of the Node that was set as this Node instance's 
   * named by node at the time specified. 
   * 
   * @param time
   * 
   * @return An Integer representing the reference of the Node that this Node
   * was named by at the time specified.  If this Node had no named by value at 
   * the time specified then null is returned. 
   */
  private Integer getNamedByAtTime(int time){
    return this._namedByHistory.floorKey(time);
  }
  
  /**
   * Returns the contents of this Node's position slots at the time specified.
   * 
   * @param time
   * 
   * @return A List of ItemSquarePattern instances representing this Node's 
   * position slot contents at the time specified.  If this Node had no position 
   * slots at the time specified then null is returned. 
   */
  private List<ItemSquarePattern> getPositionSlotsAtTime(int time){
    return this._positionSlotsHistory.floorEntry(time).getValue();
  }
  
  /**
   * Returns the references of the Nodes that were semantically linked to this
   * Node at the time specified.
   * 
   * @param time
   * 
   * @return A List of Integers representing the Nodes that were semantically
   * linked to this Node at the time specified.  If this Node had no semantic
   * links at the time specified then null is returned.
   */
  private List<Integer> getSemanticLinksAtTime(int time){
    return this._semanticLinksHistory.floorEntry(time).getValue();
  }
  
  /**
   * @param time 
   */
  private void updateProductionHistory(int time){
    if(this._productions.isEmpty()){
      this._productionHistory.put(time, null);
    }
    else{
      HashMap<Integer, Double> newActionLinkHistoryEntry = new HashMap<>();
      for(Entry<Node, Double> actionLink : this.getProductions().entrySet()){
        newActionLinkHistoryEntry.put(actionLink.getKey().getReference(), actionLink.getValue());
      }
      this._productionHistory.put(time, newActionLinkHistoryEntry);
    }
  }
  
  /**
   * Updates the Node's associated node history.
   * @param time 
   */
  private void updateAssociatedNodeHistory(int time){
    if(this._associatedNode == null){
      this._associatedNodeHistory.put(time, null);
    }
    else{
      this._associatedNodeHistory.put(time, this._associatedNode.getReference());
    }
  }
  
  /**
   * Updates the Node's child history.
   * @param time 
   */
  private void updateChildHistory(int time){
    if(this._children.isEmpty()){
      this._childrenHistory.put(time, null);
    }
    else{
      List<List<Object>> copiedChildrenDetails = new ArrayList<>();
      Iterator<Link> childrenIterator = this._children.iterator();

      while(childrenIterator.hasNext()){
        Link childToProcess = childrenIterator.next();
        ArrayList<Object> copiedChildDetails = new ArrayList<>();
        copiedChildDetails.add(childToProcess.getTest().clone());
        copiedChildDetails.add(childToProcess.getChildNode().getReference());
        copiedChildDetails.add(childToProcess.getCreationTime());
        copiedChildDetails.add(childToProcess.getExperimentCreatedIn());

        copiedChildrenDetails.add(copiedChildDetails);
      }

      this._childrenHistory.put(time, copiedChildrenDetails);
    }
  }
  
  /**
   * Updates the Node's image history.
   * @param time 
   */
  private void updateImageHistory(int time){
    if(this._image.isEmpty()){
      this._imageHistory.put(time, null);
    }
    else{
      this._imageHistory.put(time, this._image.clone());
    }
  }
  
  /**
   * Updates the Node's item slot history.
   * @param time 
   */
  private void updateItemSlotHistory(int time){
    if(this._itemSlots == null){
      this._itemSlotsHistory.put(time, null);
    }
    else if(this._itemSlots.isEmpty()){
      this._itemSlotsHistory.put(time, null);
    }
    else{
      Iterator<ItemSquarePattern> itemSlotIterator = this._itemSlots.iterator();
      List<ItemSquarePattern> itemSlotsCopy = new ArrayList<>();

      while(itemSlotIterator.hasNext()){
        ItemSquarePattern itemSlotContents = itemSlotIterator.next();
        itemSlotsCopy.add(new ItemSquarePattern(itemSlotContents.getItem(), itemSlotContents.getColumn(), itemSlotContents.getRow()));
      }

      this._itemSlotsHistory.put(time, itemSlotsCopy);
    }
  }
  
  /**
   * Updates the Node's named by history.
   * 
   * @param time 
   */
  private void updateNamedByHistory(int time){
    if(this._namedBy == null){
      this._namedByHistory.put(time, null);
    }
    else{
      this._namedByHistory.put(time, this._namedBy._reference);
    }
  }
  
  /**
   * Updates the Node's position slot history.
   * @param time 
   */
  private void updatePositionSlotHistory(int time){
    if(this._positionSlots == null){
      this._positionSlotsHistory.put(time, null);
    }
    else if(this._positionSlots.isEmpty()){
      this._positionSlotsHistory.put(time, null);
    }
    else{
      Iterator<ItemSquarePattern> positionSlotIterator = this._positionSlots.iterator();
      List<ItemSquarePattern> positionSlotsCopy = new ArrayList<>();

      while(positionSlotIterator.hasNext()){
          ItemSquarePattern positionSlotContents = positionSlotIterator.next();
          positionSlotsCopy.add(new ItemSquarePattern(positionSlotContents.getItem(), positionSlotContents.getColumn(), positionSlotContents.getRow()));
        }

      this._positionSlotsHistory.put(time, positionSlotsCopy);
    }
  }
  
  /**
   * Updates the Node's semantic link history.
   * @param time 
   */
  private void updateSemanticLinkHistory(int time){
    if(this._semanticLinks.isEmpty()){
      this._semanticLinksHistory.put(time, null);
    }
    else{
      Iterator<Node> semanticLinksIterator = this._semanticLinks.iterator();
      List<Integer> semanticLinkCopy = new ArrayList<>();

      while(semanticLinksIterator.hasNext()){
        semanticLinkCopy.add(semanticLinksIterator.next()._reference);
      }

      this._semanticLinksHistory.put(time, semanticLinkCopy);
    }
  }

  /**
   * When the model is reset, all observers of individual nodes must be closed.
   * This method notifies observers to close themselves, and then 
   * requests child nodes to do the same.
   */
  void clear () {
    setChanged ();
    notifyObservers ("close");
    for (Link child : _children) {
      child.getChildNode().clear ();
    }
     
    //Clear all instance variables that end with "History".
    for(Field field : Node.class.getDeclaredFields()){
      if(field.getName().endsWith("History")){
        try {
          
          //Get the object reference by the field for this Node instance.
          Object historyObject = (TreeMap)field.get(this);
          
          //If the object reference is an instance of a TreeMap, call the 
          //TreeMap.clear() function on it.
          if(historyObject instanceof TreeMap){
            TreeMap historyTreeMap = (TreeMap)historyObject;
            historyTreeMap.clear();
          }
        } catch (IllegalArgumentException | IllegalAccessException ex) {
          Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
  }

  /**
   * Accessor to reference number of node.
   */
  public int getReference () {
    return _reference;
  }

  /**
   * Accessor to contents of node.
   */
  public ListPattern getContents () {
    return _contents;
  }

  /**
   * Accessor to image of node.
   */
  public ListPattern getImage () {
    return _image;
  }

  /**
   * Change the node's image.  Also notifies any observers.
   */
  public void setImage (ListPattern image, int time) {
    _image = image;
    this.updateImageHistory(time);
    setChanged ();
    notifyObservers ();
  }

  /**
   * Accessor to children of node.
   */
  public List<Link> getChildren () {
    return _children;
  }

  /**
   * Add a new test link with given test pattern and child node.
   */
  void addTestLink (ListPattern test, Node child, int time, String currentExperimentName) {
    _children.add (0, new Link (test, child, time, currentExperimentName));
    this.updateChildHistory(time);
    setChanged ();
    notifyObservers ();
  }

  /**
   * Make a semantic link between this node and given node.  Do not add duplicates.
   */
  void addSemanticLink (Node node, int time) {
    if (!_semanticLinks.contains (node)) {
      _semanticLinks.add (node);
      this.updateSemanticLinkHistory(time);
      setChanged ();
      notifyObservers ();
    }
  }

  /**
   * Accessor to list of semantic links.
   */
  public List<Node> getSemanticLinks () {
    return _semanticLinks;
  }

  /**
   * Accessor to node that is associated with this node.
   */
  public Node getAssociatedNode () {
    return _associatedNode;
  }

  /**
   * Modify node that is associated with this node.
   */
  public void setAssociatedNode (Node node, int time) {
    _associatedNode = node;
    this.updateAssociatedNodeHistory(time);
    setChanged ();
    notifyObservers ();
  }

  /**
   * Accessor to node that names this node.
   */
  public Node getNamedBy () {
    return _namedBy;
  }

  /**
   * Modify node that names this node.
   */
  public void setNamedBy (Node node, int time) {
    _namedBy = node;
    this.updateNamedByHistory(time);
    setChanged ();
    notifyObservers ();
  }

  /**
   * Add the node specified by the parameter passed to this function to the list 
   * of productions for the current node if the following are true. 
   * <ul>
   *  <li>
   *    The node specified by the parameter passed to the function has action 
   *    modality.
   *  </li>
   *  <li>
   *    The node specified by the parameter passed to the function is not 
   *    already a key in the current node's _productions variable.
   *  </li>
   * </ul>
   * 
   * @param node The node that the production will terminate with.
   * @param time
   */
  public void addProduction (Node node, int time) {
    if (node.getImage().getModality().equals(Modality.ACTION) && !_productions.containsKey(node)) { 
      _productions.put(node, 0.00);
    }
    
    this.updateProductionHistory(time);
  }

  /**
   * Accessor to return the action nodes that this node is linked to.
   * 
   * @return 
   */
  public HashMap<Node, Double> getProductions () {
    return _productions;
  }
  
  /**
   * Reinforces the link between the current node and the action node specified 
   * using the reinforcement learning theory that the node's containing model
   * is set to.
   * 
   * @param actionNode The action node whose link from this Node will be 
   * reinforced.
   * @param variables The variables that need to be passed for the Reinforcement
   * Learning Theory that will be used to calculate the reinforcement value 
   * {@link jchrest.lib.ReinforcementLearning}
   * @param time The time that the reinforcement is requested.
   */
  public void reinforceProduction (Node actionNode, Double[] variables, int time){
    String reinforcementLearningTheory = _model.getReinforcementLearningTheory();
    if (
      !reinforcementLearningTheory.equals("null") && 
      _productions.containsKey(actionNode) && 
      actionNode.getContents().getModality().equals(Modality.ACTION)
    ){
      _productions.put(actionNode, (_productions.get(actionNode) + ReinforcementLearning.ReinforcementLearningTheories.valueOf(reinforcementLearningTheory).calculateReinforcementValue(variables)));
      this.updateProductionHistory(time);
    }
  }
  
  /**
   * @param recurse Set to {@link java.lang.Boolean#TRUE} to apply function 
   * recursively, returning the number of productions in this {@link #this}
   * and its children, its children's children etc.  Set to {@link 
   * java.lang.Boolean#FALSE} to just return the number of productions in this
   * {@link #this} only.
   * 
   * @return See parameter documentation.
   */
  protected int getProductionCount(boolean recurse){
    int count = this._productions.size();
    
    if(recurse){
      for(Link child : this._children){
        count += child.getChildNode().getProductionCount(true);
      }
    }
    
    return count;
  }

  /** 
   * Compute the size of the network below the current node.
   */
  public int size () {
    int count = 1; // for self
    for (Link link : _children) {
      count += link.getChildNode().size ();
    }

    return count;
  }

  /**
   * Compute the amount of information in current node.  
   * Information is based on the size of the image + the number of slots.
   */
  public int information () {
    if (_reference == 0) return 0; // root node has 0 information
    int information = _image.size ();
    if (_itemSlots != null) {
      information += _itemSlots.size ();
    }
    if (_positionSlots != null) {
      information += _positionSlots.size ();
    }

    return information;
  }

  /**
   * Add to a map of content sizes to node counts for this node and its children.
   */
  protected void getContentCounts (Map<Integer, Integer> size) {
    int csize = _contents.size ();
    if (size.containsKey (csize)) {
      size.put (csize, size.get(csize) + 1);
    } else {
      size.put (csize, 1);
    }

    for (Link child : _children) {
      child.getChildNode().getContentCounts (size);
    }
  }

  /**
   * Add to a map of image sizes to node counts for this node and its children.
   */
  protected void getImageCounts (Map<Integer, Integer> size) {
    int csize = _image.size ();
    if (size.containsKey (csize)) {
      size.put (csize, size.get(csize) + 1);
    } else {
      size.put (csize, 1);
    }

    for (Link child : _children) {
      child.getChildNode().getImageCounts (size);
    }
  }

  /**
   * Add to a map from number of semantic links to frequency, for this node and its children.
   */
  protected void getSemanticLinkCounts (Map<Integer, Integer> size) {
    int csize = _semanticLinks.size ();
    if (csize > 0) { // do not count nodes with no semantic links
      if (size.containsKey (csize)) {
        size.put (csize, size.get(csize) + 1);
      } else {
        size.put (csize, 1);
      }
    }

    for (Link child : _children) {
      child.getChildNode().getSemanticLinkCounts (size);
    }
  }
  
  

  /**
   * Compute the total size of images below the current node.
   */
  private int totalImageSize () {
    int size = _image.size ();
    for (Link link : _children) {
      size += link.getChildNode().totalImageSize ();
    }

    return size;
  }

  /**
   * If this node is a child node, then add its depth to depths.  
   * Otherwise, continue searching through children for the depth.
   */
  private void findDepth (int currentDepth, List<Integer> depths) {
    if (_children.isEmpty ()) {
      depths.add (currentDepth);
    } else {
      for (Link link : _children) {
        link.getChildNode().findDepth (currentDepth + 1, depths);
      }
    }
  }

  /**
   * Compute the average depth of nodes below this point.
   */
  public double averageDepth () {
    List<Integer> depths = new ArrayList<Integer> ();
    // -- find every depth
    for (Link link : _children) {
      link.getChildNode().findDepth(1, depths);
    }

    // -- compute the average of the depths
    int sum = 0;
    for (Integer depth : depths) {
      sum += depth;
    }
    if (depths.isEmpty ()) {
      return 0.0;
    } else {
      return (double)sum / (double)depths.size ();
    }
  }

  /**
   * Compute the average size of the images in nodes below this point.
   */
  public double averageImageSize () {
    return (double)totalImageSize() / size();
  }

  /**
   * Count templates in part of network rooted at this node.
   */
  public int countTemplates () {
    int count = 0;
    if (isTemplate ()) count += 1;

    for (Link link : _children) {
      count += link.getChildNode().countTemplates ();
    }

    return count;
  }

  

  public List<ItemSquarePattern> getFilledItemSlots () {
    return _filledItemSlots;
  }

  public List<ItemSquarePattern> getFilledPositionSlots () {
    return _filledPositionSlots;
  }

  /**
   * Returns true if this node is a template.  To be a template, the node 
   * must have at least one slot of any kind.
   */
  public boolean isTemplate () {
    if (_itemSlots == null || _positionSlots == null) {
      return false;
    }

    // is a template if there is at least one slot
    if (_itemSlots.size () > 0) return true;
    if (_positionSlots.size () > 0) return true;

    return false;
  }

  /**
   * Clear out the template slots.
   */
  public void clearTemplate (int time) {
    if (_itemSlots != null){
      _itemSlots.clear ();
      this.updateItemSlotHistory(time);
    }
    if (_positionSlots != null){
      _positionSlots.clear ();
      this.updatePositionSlotHistory(time);
    }
  }

  /**
   * Attempt to fill some of the slots using the items in the given pattern.
   */
  public void fillSlots (ListPattern pattern) {
    // create arraylists only when required, as most nodes do not need to 
    // waste the storage space.
    if (_itemSlots == null) {
      _itemSlots = new ArrayList<ItemSquarePattern> ();
    }
    if (_positionSlots == null) {
      _positionSlots = new ArrayList<ItemSquarePattern> ();
    }
    if (_filledItemSlots == null) {
      _filledItemSlots = new ArrayList<ItemSquarePattern> ();
    }
    if (_filledPositionSlots == null) {
      _filledPositionSlots = new ArrayList<ItemSquarePattern> ();
    }
    for (int index = 0; index < pattern.size (); index++) {
      boolean slotFilled = false;
      if (pattern.getItem(index) instanceof ItemSquarePattern) {
        ItemSquarePattern item = (ItemSquarePattern)(pattern.getItem (index));
        // only try to fill a slot if item is not already in image or slot
        if (!_image.contains (item) && 
            !_filledItemSlots.contains (item) && 
            !_filledPositionSlots.contains (item)) { 
          // 1. check the item slots
          for (ItemSquarePattern slot : _itemSlots) {
            if (!slotFilled) {
              if (slot.getItem().equals(item.getItem ())) {
                _filledItemSlots.add (item);
                slotFilled = true;
              }
            }
          }

          // 2. check the position slots
          for (ItemSquarePattern slot : _positionSlots) {
            if (!slotFilled) {
              if (slot.getRow () == item.getRow () &&
                  slot.getColumn () == item.getColumn ()) {
                _filledPositionSlots.add (item);
                slotFilled = true;
                  }
            }
          }
        }
      }
    }
  }

  public void clearFilledSlots () {
    if (_filledItemSlots == null) _filledItemSlots = new ArrayList<ItemSquarePattern> ();
    if (_filledPositionSlots == null) _filledPositionSlots = new ArrayList<ItemSquarePattern> ();

    _filledItemSlots.clear ();
    _filledPositionSlots.clear ();
  }

  /**
   * Retrieve all primitive items stored in slots of template as a ListPattern.
   * The retrieved pattern may contain duplicate primitive items, but will be 
   * untangled in Chrest#scanScene.
   */
  ListPattern getFilledSlots () {
    ListPattern filledSlots = new ListPattern ();
    for (ItemSquarePattern filledSlot : _filledItemSlots) {
      filledSlots.add (filledSlot);
    }
    for (ItemSquarePattern filledSlot : _filledPositionSlots) {
      filledSlots.add (filledSlot);
    }
    return filledSlots;
  }

  /**
   * Converts this node into a template, if appropriate, and repeats for 
   * all child nodes.
   * Note: usually, this process is done as a whole at the end of training, but 
   * can also be done on a node-by-node basis, during training.
   */
  public void constructTemplates (int time) {
    _itemSlots = new ArrayList<ItemSquarePattern> ();
    _positionSlots = new ArrayList<ItemSquarePattern> ();

    if (canFormTemplate ()) {
      // gather images of current node, test links and similar nodes together, 
      // removing the contents from them
      List<ListPattern> patterns = new ArrayList<ListPattern> ();
      patterns.add (_image.remove (_contents));
      for (Link link : _children) {
        patterns.add (link.getChildNode().getImage().remove (_contents));
      }
      for (Node node : _semanticLinks) {
        patterns.add (node.getImage().remove (_contents));
      }
      // create a hashmap of counts of occurrences of items and of squares
      Map<String,Integer> countItems = new HashMap<String,Integer> ();
      Map<Integer,Integer> countPositions = new HashMap<Integer,Integer> ();
      for (ListPattern pattern : patterns) {
        for (PrimitivePattern pattern_item : pattern) {
          if (pattern_item instanceof ItemSquarePattern) {
            ItemSquarePattern item = (ItemSquarePattern)pattern_item;
            if (countItems.containsKey (item.getItem ())) {
              countItems.put (item.getItem (), countItems.get(item.getItem ()) + 1);
            } else {
              countItems.put (item.getItem (), 1);
            }
            // TODO: Check construction of 'posn_key', try 1000 = scene.getWidth ?
            Integer posn_key = item.getRow () + 1000 * item.getColumn ();
            if (countPositions.containsKey (posn_key)) {
              countPositions.put (posn_key, countPositions.get(posn_key) + 1);
            } else {
              countPositions.put (posn_key, 1);
            }
          }
        }
      }

      // make slots
      // 1. from items which repeat more than minimumNumberOccurrences
      for (String itemKey : countItems.keySet ()) {
        if (countItems.get(itemKey) >= _model.getMinTemplateOccurrences ()) {
          _itemSlots.add (new ItemSquarePattern (itemKey, -1, -1));
        }
      }
      // 2. from locations which repeat more than minimumNumberOccurrences
      for (Integer posnKey : countPositions.keySet ()) {
        if (countPositions.get(posnKey) >= _model.getMinTemplateOccurrences ()) {
          _positionSlots.add (new ItemSquarePattern ("slot", posnKey / 1000, posnKey - (1000 * (posnKey/1000))));
        }
      }
      
      this.updateItemSlotHistory(time);
      this.updatePositionSlotHistory(time);
    }

    // continue conversion for children of this node
    for (Link link : _children) {
      link.getChildNode().constructTemplates (time);
    }

  }

  /** Return true if template conditions are met:
   * 1. contents size > _model.getMinTemplateLevel ()
   * then:
   * 2. gather together current node image and images of all nodes 
   * linked by the test and semantic links
   *    remove the contents of current node from those images
   *    see if any piece or square repeats more than once
   */
  public boolean canFormTemplate () {
    // return false if node is too shallow in network
    if (_contents.size () <= _model.getMinTemplateLevel ()) return false;
    // gather images of current node and test links together, removing the contents from them
    List<ListPattern> patterns = new ArrayList<ListPattern> ();
    patterns.add (_image.remove (_contents));
    for (Link link : _children) {
      patterns.add (link.getChildNode().getImage().remove (_contents));
    }
    for (Node node : _semanticLinks) {
      patterns.add (node.getImage().remove (_contents));
    }
    // create a hashmap of counts of occurrences of items and of squares
    Map<String,Integer> countItems = new HashMap<String,Integer> ();
    Map<Integer,Integer> countPositions = new HashMap<Integer,Integer> ();
    for (ListPattern pattern : patterns) {
      for (PrimitivePattern pattern_item : pattern) {
        if (pattern_item instanceof ItemSquarePattern) {
          ItemSquarePattern item = (ItemSquarePattern)pattern_item;
          if (countItems.containsKey (item.getItem ())) {
            countItems.put (item.getItem (), countItems.get(item.getItem ()) + 1);
          } else {
            countItems.put (item.getItem (), 1);
          }
          Integer posn_key = item.getRow () + 1000 * item.getColumn ();
          if (countPositions.containsKey (posn_key)) {
            countPositions.put (posn_key, countPositions.get(posn_key) + 1);
          } else {
            countPositions.put (posn_key, 1);
          }
        }
      }
    }

    // make slots
    // 1. from items which repeat more than minimumNumberOccurrences
    for (String itemKey : countItems.keySet ()) {
      if (countItems.get(itemKey) >= _model.getMinTemplateOccurrences ()) {
        return true;
      }
    }
    // 2. from locations which repeat more than minimumNumberOccurrences
    for (Integer posnKey : countPositions.keySet ()) {
      if (countPositions.get(posnKey) >= _model.getMinTemplateOccurrences ()) {
        return true;
      }
    }
    return false;
  }

  /**
   * LearnPrimitive is used to construct a test link and node containing 
   * precisely the given pattern.  It is assumed the given pattern contains 
   * a single primitive item, and is finished.
   * TODO: CLEAN UP CODE AND DESCRIPTION
   */
  public Node learnPrimitive (ListPattern pattern, int domainTime) {
    assert (pattern.isFinished () && pattern.size () == 1);
    ListPattern contents = pattern.clone ();
    contents.setNotFinished ();
    Node child = new Node (_model, contents, new ListPattern (pattern.getModality ()), domainTime);
    addTestLink (contents, child, domainTime, _model.getCurrentExperimentName());
    _model.advanceLearningClock (_model.getDiscriminationTime ());

    return child;
  }

  /**
   * addTest is used to construct a test link using the given pattern, 
   * with a new empty child node.  It is assumed the given pattern is 
   * non-empty and constitutes a valid, new test for the current Node.
   */
  private Node addTest (ListPattern pattern, int domainTime) {
    
    //Set-up history variables
    HashMap<String, Object> historyRowToInsert = new HashMap<>();
    historyRowToInsert.put(Chrest._executionHistoryTableTimeColumnName, domainTime);
    
    //Generic operation name setter for current method.  Ensures for the row to 
    //be added that, if this method's name is changed, the entry for the 
    //"Operation" column in the execution history table will be updated without 
    //manual intervention and "Filter By Operation" queries run on the execution 
    //history DB table will still work.
    class Local{};
    historyRowToInsert.put(Chrest._executionHistoryTableOperationColumnName, 
      ExecutionHistoryOperations.getOperationString(this.getClass(), Local.class.getEnclosingMethod())
    );
    historyRowToInsert.put(Chrest._executionHistoryTableInputColumnName, pattern.toString());
    
    // ignore if already a test
    for (Link child : this._children) {
      if (child.getTest().equals (pattern)) {
        historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, "Input already a test for node " + this.getReference() + ", exiting.");
        historyRowToInsert.put(Chrest._executionHistoryTableOutputColumnName, "Node (ref: " + this.getReference() + ", image: " + this.getImage().toString() + ")");
        this._model.addEpisodeToExecutionHistory(historyRowToInsert);
        return this;
      }
    }
    
    historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, "Using input to create new test & child from node " + this.getReference() + ".");
    this._model.addEpisodeToExecutionHistory(historyRowToInsert);
    
    Node child = new Node (
      _model, 
      ( (_reference == 0) ? pattern : _model.getDomainSpecifics().normalise (_contents.append(pattern))), // don't append to 'Root'
      ( (_reference == 0) ? pattern : _model.getDomainSpecifics().normalise (_contents.append(pattern))), // make same as contents vs Chrest 2
      domainTime
    );

    this.addTestLink (pattern, child, domainTime, _model.getCurrentExperimentName());
    _model.advanceLearningClock (_model.getDiscriminationTime ());
    return child;
  }

  /**
   * extendImage is used to add new information to the node's image.
   * It is assumed the given pattern is non-empty and is a valid extension.
   */
  private Node extendImage (ListPattern newInformation, int time) {
    this.setImage (_model.getDomainSpecifics().normalise (_image.append (newInformation)), time);
    this.updateImageHistory(time);
    _model.advanceLearningClock (_model.getFamiliarisationTime ());

    return this;
  }

  /**
   * Discrimination learning extends the LTM network by adding new 
   * nodes.
   * Note: in CHREST 2 tests are pointers to nodes.  This can be 
   * implemented using a Link interface, and having a LinkNode class, 
   * so that checking if test passed is done through the interface.
   * This may be needed later for semantic/template learning.
   */
  Node discriminate (ListPattern pattern, int time) {
    
    ListPattern newInformation = pattern.remove (_contents);
    
    //Set-up history variables.
    HashMap<String, Object> historyRowToInsert = new HashMap<>();
    historyRowToInsert.put(Chrest._executionHistoryTableTimeColumnName, time);
    
    //Generic operation name setter for current method.  Ensures for the row to 
    //be added that, if this method's name is changed, the entry for the 
    //"Operation" column in the execution history table will be updated without 
    //manual intervention and "Filter By Operation" queries run on the execution 
    //history DB table will still work.
    class Local{};
    historyRowToInsert.put(Chrest._executionHistoryTableOperationColumnName, 
      ExecutionHistoryOperations.getOperationString(this.getClass(), Local.class.getEnclosingMethod())
    );
    historyRowToInsert.put(Chrest._executionHistoryTableInputColumnName, pattern.toString() + "(" + pattern.getModalityString() + ")");
    String description = "New info in input: '" + newInformation.toString() + "'. ";

    // cases 1 & 2 if newInformation is empty
    if (newInformation.isEmpty ()) {
      
      // change for conformance
      newInformation.setFinished ();
      
      // 1. < $ > known
      if (_model.recognise (newInformation, time).getContents ().equals (newInformation) ) {
        
        // 2. if so, use as test
        description += "New info encoded in LTM, add as test to node " + this._reference + ".";
        historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, description);
        this._model.addEpisodeToExecutionHistory(historyRowToInsert);
        return this.addTest(newInformation, time);
      
      }
      // 2. < $ > not known
      else {
        
        description += "New info not encoded in LTM, add as test to " + newInformation.getModalityString() + " root node.";
        historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, description);
        this._model.addEpisodeToExecutionHistory(historyRowToInsert);
        
        Node child = new Node (_model, newInformation, newInformation, time);
        _model.getLtmByModality(newInformation).addTestLink (newInformation, child, time, _model.getCurrentExperimentName());
        return child;
      }
    }

    Node retrievedChunk = _model.recognise (newInformation, time);
    description += "Recognised '" + retrievedChunk.getImage().toString() + "', node ref: " + retrievedChunk.getReference() + "). ";

    if (retrievedChunk == _model.getLtmByModality (pattern)) {

      // 3. if root node is retrieved, then the primitive must be learnt
      description += "Modality root node, add first item of new info as test to this root node.";
      historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, description);
      this._model.addEpisodeToExecutionHistory(historyRowToInsert);
      return _model.getLtmByModality(newInformation).learnPrimitive (newInformation.getFirstItem (), time);

    } else if (retrievedChunk.getContents().matches (newInformation)) {

      // 4. retrieved chunk can be used as a test
      description += "Image of rec. node matches new info. Add " + retrievedChunk.getContents().toString() + " as test to node " + this.getReference() + ".";
      historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, description);
      this._model.addEpisodeToExecutionHistory(historyRowToInsert);
      
      ListPattern testPattern = retrievedChunk.getContents().clone ();
      return this.addTest (testPattern, time);

    } else {

      // 5. mismatch, so use only the first item for test
      // NB: first-item must be in network as retrievedChunk was not the root 
      //     node
      ListPattern firstItem = newInformation.getFirstItem ();
      firstItem.setNotFinished ();
      description += "but image does not match new info. Add " + firstItem.toString() + " as test to node " + this.getReference() + ".";
      historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, description);
      this._model.addEpisodeToExecutionHistory(historyRowToInsert);
      
      return this.addTest (firstItem, time);
    }
  }

  /**
   * Familiarisation learning extends the image in a node by adding new 
   * information from the given pattern.
   */
  Node familiarise (ListPattern pattern, int domainTime) {
    
    ListPattern newInformation = pattern.remove(_image).getFirstItem();
    newInformation.setNotFinished ();
    
    //Set-up history variables.
    HashMap<String, Object> historyRowToInsert= new HashMap<>();
    historyRowToInsert.put(Chrest._executionHistoryTableTimeColumnName, domainTime);
    
    //Generic operation name setter for current method.  Ensures for the row to 
    //be added that, if this method's name is changed, the entry for the 
    //"Operation" column in the execution history table will be updated without 
    //manual intervention and "Filter By Operation" queries run on the execution 
    //history DB table will still work.
    class Local{};
    historyRowToInsert.put(Chrest._executionHistoryTableOperationColumnName, 
      ExecutionHistoryOperations.getOperationString(this.getClass(), Local.class.getEnclosingMethod())
    );
    historyRowToInsert.put(Chrest._executionHistoryTableInputColumnName, pattern.toString() + "(" + pattern.getModalityString() + ")");
    String description = "New info in input: " + newInformation.toString();
    
    // EXIT if nothing to learn
    if (newInformation.isEmpty ()) {  
      description += ", empty.";
      historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, description);
      this._model.addEpisodeToExecutionHistory(historyRowToInsert);
      return this;
    }

    // Note: CHREST 2 had the idea of not familiarising if image size exceeds 
    // the max of 5 and 2*contents-size.  This avoids overly large images.
    // This idea is not implemented here.
    //
    Node retrievedChunk = _model.recognise (newInformation, domainTime);
    description += ", not empty, node " + retrievedChunk.getReference() + " retrieved";

    if (retrievedChunk == _model.getLtmByModality (pattern)) {

      // primitive not known, so learn it
      historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, description);
      this._model.addEpisodeToExecutionHistory(historyRowToInsert);
      return _model.getLtmByModality(newInformation).learnPrimitive (newInformation, domainTime);

    } else {

      // extend image with new item
      historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, description);
      this._model.addEpisodeToExecutionHistory(historyRowToInsert);
      return this.extendImage (newInformation, domainTime);
    }
  }

  /**
   * Search this node's semantic links for a more informative node, and return one if 
   * found.
   */
  public Node searchSemanticLinks (int maximumSemanticDistance) {
    if (maximumSemanticDistance <= 0) return this; // reached limit of search
    Node bestNode = this;
    for (Node compare : _semanticLinks) {
      Node bestChild = compare.searchSemanticLinks (maximumSemanticDistance - 1);
      if (bestChild.information () > bestNode.information ()) {
        bestNode = bestChild;
      }
    }

    return bestNode;
  }

  /**
   * Write node information in VNA format.
   */
  public void writeNodeAsVna (Writer writer) throws IOException {
    writer.write ("" + _reference + " \"" + _contents.toString() + "\"\n");
    for (Link link : _children) {
      link.getChildNode().writeNodeAsVna (writer);
    }
  }

  public void writeLinksAsVna (Writer writer) throws IOException {
    // write my links
    for (Link link : _children) {
      writer.write ("" + _reference + " " + link.getChildNode().getReference () + "\n");
    }
    // repeat for children
    for (Link link : _children) {
      link.getChildNode().writeLinksAsVna (writer);
    }
  }

  public void writeSemanticLinksAsVna (Writer writer) throws IOException {
    // write my links
    for (Node node : _semanticLinks) {
      writer.write ("" + _reference + " " + node.getReference () + "\n");
    }
    // repeat for children
    for (Link link : _children) {
      link.getChildNode().writeSemanticLinksAsVna (writer);
    }
  }
  
  /**
   * Returns the domain time that this Node instance was created.
   * 
   * @return 
   */
  public int getCreationTime(){
    return this._creationTime;
  }
  
  /**
   * Clones this Node deeply by cloning all Node instances referenced by this
   * Node and so on.
   * 
   * @param time
   * @return 
   */
  public Node deepClone(int time){
    this.deepClone(time, new ArrayList<>());
    return this._clone;
  }
  
  /**
   * Deeply clones the Node's current or historical state so any Node instances
   * referenced by the current Node and any Node instances they reference are
   * cloned too, recursively.  
   * <ul>
   *  <li>
   *    If a historical clone is requested then the state of the Node instances 
   *    returned will be as they were at the time closest to the time specified.
   *  </li>
   * </ul>
   * 
   * @param time Set this to -1 if the creation time of nodes is not of interest.
   * 
   * @param setOfClonedNodes The set of currently cloned nodes.  Since this 
   * mechanism creates deep clones exact duplicate clones should
   * not be created (this will cause problems with Node instance variable 
   * references to other Nodes).  Usually an empty set should be passed when 
   * this function is invoked, the function itself will pass an instantiated set 
   * as it recurses itself.
   * 
   * @return 
   */
  private ArrayList<Integer> deepClone(int time, ArrayList<Integer> setOfClonedNodeReferences){
    if( this.getCreationTime() <= time || time == -1 ){

      //Check that the node to be cloned doesn't already exist in the set of 
      //cloned nodes.
      boolean nodeAlreadyCloned = false;
      for(Integer cloneReference : setOfClonedNodeReferences){
        if(cloneReference == this._reference){
          nodeAlreadyCloned = true;
          break;
        }
      }
      if(!nodeAlreadyCloned){

        /**********************************/
        /**** CONSTRUCT CLONE INSTANCE ****/
        /**********************************/
        
        Node clone = new Node(this._model, this._reference, this._contents.clone(), this._image.clone(), this._creationTime);
        this._clone = clone;
        setOfClonedNodeReferences.add(clone._reference);
        
        /*********************************************/
        /**** CLONE NODES REFERENCED BY THIS NODE ****/
        /*********************************************/
         
        //Clone Node instances in instance variables that contain one node.
        if(this._associatedNode != null){
          this._associatedNode.deepClone(time, setOfClonedNodeReferences);
        }
        
        if(this._namedBy != null){
          this._namedBy.deepClone(time, setOfClonedNodeReferences);
        }
        
        //Clone Node instances in instance variables that contain multiple nodes.
        for(Node node : this._productions.keySet()){
          if(node != null){
            node.deepClone(time, setOfClonedNodeReferences);
          }
        }
        
        for(Link childLink : this._children){
          Node childNode = childLink.getChildNode();
          if(childNode != null){
            childNode.deepClone(time, setOfClonedNodeReferences);
          }
        }
        
        for(Node node : this._semanticLinks){
          if(node != null){
            node.deepClone(time, setOfClonedNodeReferences);
          }
        }
        
        /**********************************************************************/
        /**** INSTANTIATE INSTANCE VARIABLES AS THEY WERE @ TIME SPECIFIED ****/
        /**********************************************************************/
        
        HashMap<Integer, Double> historicalProductions = this.getProductionsAtTime(time);
        if(historicalProductions != null){
          for(Entry<Integer, Double> historicalActionLink : historicalProductions.entrySet()){
            //An action link can only every be in action LTM so just search the
            //Node instances in this LTM modality for the relevant clone.
            clone._productions.put( Node.searchForNodeFromBaseNode(historicalActionLink.getKey(), this._model.getLtmByModality(Modality.ACTION))._clone, historicalActionLink.getValue() );
          }
        }
        
        Integer historicalAssociatedNodeReference = this.getAssociatedNodeAtTime(time);
        if(historicalAssociatedNodeReference != null){
          //An associated Node may be of any modality in LTM so a general 
          //modality search must be performed to retrieve the relevant clone.
          Node result = Node.searchForNodeInLtm(historicalAssociatedNodeReference, this._model);
          clone._associatedNode = result._clone;
        }
        
        List<List<Object>> historicalChildLinkDetails = this.getChildrenAtTime(time);
        if(historicalChildLinkDetails != null){
          for(List<Object> historicalChildDetail : historicalChildLinkDetails){
            
            ListPattern testPattern = (ListPattern)historicalChildDetail.get(0);
            Integer childNodeReference = (Integer)historicalChildDetail.get(1);
            Integer creationTime = (Integer)historicalChildDetail.get(2);
            String createdInExperiment = (String)historicalChildDetail.get(3);
            
            //A child node will only ever be a descendent of this node so use 
            //this Node as the starting point for the clone search.
            Node clonedChild = Node.searchForNodeFromBaseNode( childNodeReference, this)._clone;
            
            clone._children.add(new Link(testPattern, clonedChild, creationTime, createdInExperiment));
          }
        }
        
        if(this._filledItemSlots!= null){
          if(!this._filledItemSlots.isEmpty()){
            for(ItemSquarePattern itemInSlot : this._filledItemSlots){
              clone._filledItemSlots.add(new ItemSquarePattern(itemInSlot.getItem(), itemInSlot.getColumn(), itemInSlot.getRow()));
            }
          }
        }
        
        if(this._filledPositionSlots != null){
          if(!this._filledPositionSlots.isEmpty()){
            for(ItemSquarePattern itemInSlot : this._filledPositionSlots){
              clone._filledPositionSlots.add(new ItemSquarePattern(itemInSlot.getItem(), itemInSlot.getColumn(), itemInSlot.getRow()));
            }
          }
        }
        
        ListPattern historicalImage = this.getImageAtTime(time);
        if(historicalImage != null){
          clone._image = historicalImage.clone();
        }
        
        List<ItemSquarePattern> historicalItemSlots = this.getItemSlotsAtTime(time);
        if(historicalItemSlots != null){
          for(ItemSquarePattern historicalItemSlot: historicalItemSlots){
            clone._itemSlots.add(new ItemSquarePattern(historicalItemSlot.getItem(), historicalItemSlot.getColumn(), historicalItemSlot.getRow()));
          }
        }
        
        Integer historicalNamedBy = this.getNamedByAtTime(time);
        if(historicalNamedBy != null){
          //The Node in _namedBy will only ever be a verbal Node.
          clone._namedBy = Node.searchForNodeFromBaseNode(historicalNamedBy, this._model.getLtmByModality(Modality.VERBAL))._clone;
        }
        
        List<ItemSquarePattern> historicalPositionSlots = this.getItemSlotsAtTime(time);
        if(historicalPositionSlots != null){
          for(ItemSquarePattern historicalPositionSlot: historicalItemSlots){
            clone._positionSlots.add(new ItemSquarePattern(historicalPositionSlot.getItem(), historicalPositionSlot.getColumn(), historicalPositionSlot.getRow()));
          }
        }
        
        List<Integer> historicalSemanticLinks = this.getSemanticLinksAtTime(time);
        if(historicalSemanticLinks != null){
          for(Integer historicalSemanticLink : historicalSemanticLinks){
            //Semantic links will only ever be visual.
            clone._semanticLinks.add( Node.searchForNodeFromBaseNode(historicalSemanticLink, this._model.getLtmByModality(Modality.VISUAL))._clone );
          }
        }
      }
    }
    
    return setOfClonedNodeReferences;
  }
  
  /**
   * Searches through all LTM modalities in the model specified for the Node
   * reference specified and returns that Node instance.
   * 
   * @param reference The Node instance to search for and retrieve.
   * @param model The CHREST model whose LTM is to be searched.
   * 
   * @return The matching Node reference from LTM or null if no Node instance
   * in LTM has a reference that matches that supplied.
   */
  public static Node searchForNodeInLtm(int reference, Chrest model){
    
    Node result = null;
    
    breakpoint:
    for(Field field : model.getClass().getDeclaredFields()){
      field.setAccessible(true);
      for(Modality modality : Modality.values()){
        if(field.getName().endsWith("_" + modality.toString().toLowerCase() + "Ltm")){
          Object ltmObject;
        
          try {
            ltmObject = field.get(model);
            if(ltmObject instanceof Node){
              Node ltmRootNode = (Node)ltmObject;
              ArrayList<Node> searchResult = Node.searchForNodeFromBaseNode(reference, ltmRootNode, new ArrayList<>());
              if(searchResult.size() > 0){
                result = searchResult.get(0);
                field.setAccessible(false);
                break breakpoint;
              }
            }
          } catch (IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
      field.setAccessible(false);
    }
    
    return result;
  }
  
  /**
   * Searches recursively from the Node instance provided, n, for a Node 
   * instance whose reference, r, is equal to the reference provided.  If r != 
   * reference provided then then the references of n's children are checked and
   * so on until either a Node instance's reference matches that provided or not.
   * 
   * @param reference The reference of the Node instance to find and retrieve.
   * @param node The Node to search from, n.
   * 
   * @return The Node instance whose reference is equal to the reference 
   * provided or null if the references of n or n's descendents do not equal the 
   * reference provided.
   */
  public static Node searchForNodeFromBaseNode(int reference, Node node){
    ArrayList<Node> result = Node.searchForNodeFromBaseNode(reference, node, new ArrayList<>());
    return result.get(0);
  }
  
  /**
   * Performs the actual search described in the public 
   * {@link jchrest.architecture.Node#searchForNodeFromBaseNode} method.  This
   * method accepts a stack data structure that has a Node instance added to it
   * if the reference for the Node instance being searched is equal to the 
   * reference provided.
   * 
   * @param reference The reference of the Node instance to find and retrieve.
   * @param node The Node instance to be searched.
   * @param result The result of the search, this should be set to null at first 
   * but is updated if a Node whose reference is equal to the reference provided
   * is found.
   * 
   * @return Either null if the reference for the Node being searched !=
   * the reference provided or the Node whose reference does match the reference
   * provided in the first position of the stack.
   */
  private static ArrayList<Node> searchForNodeFromBaseNode(int reference, Node node, ArrayList<Node> result){
    if(node._reference != reference){
      for(Link nodeLink : node._children){
        Node.searchForNodeFromBaseNode(reference, nodeLink.getChildNode(), result);
      }
    }
    else{
      result.add(node);
    }
    
    return result;
  }
  
  public Node getClone(){
    return this._clone;
  }
  
  public void clearClone(){
    this._clone = null;
  }
}

