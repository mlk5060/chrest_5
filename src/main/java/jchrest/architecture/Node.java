// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.architecture;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import jchrest.lib.ChrestStatus;
import jchrest.lib.HistoryTreeMap;
import jchrest.lib.ItemSquarePattern;
import jchrest.lib.ListPattern;
import jchrest.lib.Modality;
import jchrest.lib.Pattern;
import jchrest.lib.PrimitivePattern;
import jchrest.lib.ReinforcementLearning.Theory;
import jchrest.lib.Square;

/**
 * Represents a node within a {@link jchrest.architecture.Chrest} model's 
 * long-term memory (LTM) discrimination network.
 * 
 * Each {@link #this} has a unique reference with respect to the LTM network of 
 * the {@link jchrest.architecture.Chrest} model it is located in and a time of 
 * creation.  A {@link #this} usually has "children" that are composed of a
 * {@link jchrest.architecture.Link} that terminates with another {@link 
 * jchrest.architecture.Node}.  
 * 
 * Each {@link jchrest.architecture.Node} maintains a non-mutable <i>contents
 * </i> instance variable that is the aggregation of all {@link 
 * jchrest.lib.ListPattern}s found on the tests of {@link 
 * jchrest.architecture.Link}s that must be passed to reach {@link #this} in the 
 * LTM of the {@link jchrest.architecture.Chrest} this {@link #this} is 
 * associated with.
 * 
 * Each {@link jchrest.architecture.Node} also contains an <i>image</i> that may
 * change over the {@link jchrest.architecture.Node}'s lifespan.  The image of
 * a {@link jchrest.architecture.Node} is also referred to as being a <i>chunk
 * </i> and may be empty.  Note that a {@link jchrest.architecture.Node}'s 
 * contents and image are two distinct concepts.
 * 
 * As well as the vertical {@link jchrest.architecture.Link}s described, a 
 * {@link jchrest.architecture.Node} may also contain horizontal associations
 * with other {@link jchrest.architecture.Node}s:
 * 
 * <ul>
 *  <li>
 *    General: an association between two {@link jchrest.architecture.Node}s of 
 *    any {@link jchrest.lib.Modality}; does not represent any particular 
 *    concept.  A {@link jchrest.architecture.Node} may only have one general 
 *    association.
 *  </li>
 *  <li>
 *    Naming: an association between a {@link jchrest.lib.Modality#VISUAL} 
 *    {@link jchrest.architecture.Node} and a {@link 
 *    jchrest.lib.Modality#VERBAL} {@link jchrest.architecture.Node} that 
 *    represents a concept where something that is seen has a verbal identifier.  
 *    A {@link jchrest.lib.Modality#VISUAL} {@link jchrest.architecture.Node} 
 *    may only have one naming association and the association is unilateral 
 *    (from {@link jchrest.lib.Modality#VISUAL} to {@link 
 *    jchrest.lib.Modality#VERBAL}).
 *  </li>
 *  <li>
 *    Production: an association between a {@link jchrest.lib.Modality#VISUAL} 
 *    {@link jchrest.architecture.Node} and an {@link 
 *    jchrest.lib.Modality#ACTION} {@link jchrest.architecture.Node} that 
 *    represents a concept where, given a visual state of the environment, some 
 *    action is performed.  A production also has a <i>value</i> that may serve
 *    to represent a concept such as the optimality of the production (this is
 *    where {@link jchrest.lib.ReinforcementLearning} becomes useful).  A {@link 
 *    jchrest.lib.Modality#VISUAL} or {@link jchrest.lib.Modality#ACTION}
 *    {@link jchrest.architecture.Node} is not limited to how many production
 *    associations it may have and the associations are unilateral (from {@link 
 *    jchrest.lib.Modality#VISUAL} to {@link jchrest.lib.Modality#ACTION}).
 *  </li>
 *  <li>
 *    Semantic: an association between two {@link jchrest.architecture.Node}s of
 *    the same {@link jchrest.lib.Modality} that represents a concept whereby 
 *    information that occurs with a high temporal proximity is likely to be
 *    related.  A {@link jchrest.architecture.Node} is not limited to how many 
 *    semantic associations it may have and the associations can be bilateral.
 *  </li>
 * </ul>
 * 
 * A {@link jchrest.architecture.Node} may also become a template whereby it 
 * stores the images of other {@link jchrest.architecture.Node}s in a data
 * structure (slots) that can be accessed when the template {@link 
 * jchrest.architecture.Node} is accessed (links and associations to this
 * information do not have to be traversed).
 * 
 * A {@link jchrest.architecture.Node} may be a "root" modality node, i.e. it is 
 * at the "top" level of LTM and classifies the modality of all its children.
 * A modality root node contains no information about the external environment 
 * and the image and contents it is initialised with will not be modified during 
 * its lifetime.  Furthermore, a root node contains no horizontal associations 
 * with other {@link jchrest.architecture.Node}s and can not become a template.
 * 
 * Methods support learning and also display.
 *
 * @author Peter C. R. Lane
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class Node extends Observable {
  
  /****************************/
  /**** INSTANCE VARIABLES ****/
  /****************************/

  //The variables listed below stay consistent throughout the Node's life-cycle.
  private final ListPattern _contents;
  private final int _creationTime;
  private final Chrest _model;
  private final int _reference;
  private final boolean _rootNode;
  private final Modality _modality;
  
  //The variables listed below do not stay consistent throughout the Node's
  //life-cycle.
  private HistoryTreeMap<Integer, List<Link>> _childHistory = new HistoryTreeMap();
  private HistoryTreeMap<Integer, LinkedHashMap<Node, Double>> _productionHistory = new HistoryTreeMap();
  private HistoryTreeMap<Integer, Node> _associatedNodeHistory = new HistoryTreeMap();
  private HistoryTreeMap<Integer, Node> _namedByHistory = new HistoryTreeMap();
  private HistoryTreeMap<Integer, List<Node>> _semanticLinksHistory = new HistoryTreeMap();
  private HistoryTreeMap<Integer, ListPattern> _imageHistory = new HistoryTreeMap();
  private HistoryTreeMap<Integer, Boolean> _templateHistory = new HistoryTreeMap();
  
  // Template slot history variables: only instantiated when needed since most 
  // Node instances will never become templates so they don't need to waste the 
  // storage space.
  private HistoryTreeMap _itemSlotsHistory;
  private HistoryTreeMap _positionSlotsHistory;
  private HistoryTreeMap _filledItemSlotsHistory;
  private HistoryTreeMap _filledPositionSlotsHistory;
  
  /**********************/
  /**** CONSTRUCTORS ****/
  /**********************/

  /**
   * Constructs a new root {@link jchrest.architecture.Node}.
   * 
   * Package access only: should only be used by {@link 
   * jchrest.architecture.Chrest}.
   * 
   * @param model
   * @param modality
   * @param creationTime
   */
  Node (Chrest model, Modality modality, int creationTime) {
    this(
      model, 
      true,
      Pattern.makeList(new String[]{"Root"}, modality),
      Pattern.makeList(new String[]{"Root"}, modality), 
      creationTime
    );
  }
 
  /**
   * Constructs a new, non-root {@link jchrest.architecture.Node}.
   * 
   * TODO: This is public since making it package-access only causes testing to 
   *       become nearly impossible; try to circumvent this.  
   * 
   * @param model
   * @param contents
   * @param image
   * @param creationTime
   */
  public Node (Chrest model, ListPattern contents, ListPattern image, int creationTime) {
    this (model, false, contents, image, creationTime);
  }

  /**
   * Constructs a new {@link jchrest.architecture.Node} (root/non-root).
   * 
   * Package access only: should only be used by {@link 
   * jchrest.architecture.Chrest}.
   */
  private Node (Chrest model, boolean rootNode, ListPattern contents, ListPattern image, int creationTime) {
    if(model.getCreationTime() <= creationTime){
      this._creationTime = creationTime;
      this._model = model;
      this._rootNode = rootNode;
      this._reference = this._model.getNextLtmNodeReference();
      this._contents = contents.clone();
      this._modality = image.getModality();
      
      this._childHistory.put(creationTime, new ArrayList());
      this._productionHistory.put(creationTime, new LinkedHashMap());
      this._associatedNodeHistory.put(creationTime, null);
      this._namedByHistory.put(creationTime, null);
      this._semanticLinksHistory.put(creationTime, new ArrayList());
      this._imageHistory.put(creationTime, image);
      this._templateHistory.put(creationTime, false);
      
      this._model.incrementNextNodeReference();
      
      if(!rootNode){
        this._model.incrementLtmModalityNodeCount(contents.getModality(), creationTime);
      }
    }
    else{
      throw new RuntimeException("Creation time specified for new Node instance ("
        + creationTime + ") is earlier than the creation time of the CHREST model "
        + "it will be associated with (" + model.getCreationTime() + ")"
      );
    }
  }
  
  /**************************/
  /**** SIMPLE FUNCTIONS ****/
  /**************************/
  
  /**
   * Notifies observers of {@link #this} to close themselves, and then requests 
   * {@link #this}'s child nodes to do the same.
   */
  void clear () {
    setChanged ();
    notifyObservers ("close");
    for (Link child : (List<Link>)this._childHistory.lastEntry().getValue()) {
      child.getChildNode().clear();
    }
  }
  
  /**
   * @return The time this {@link #this} was created.
   */
  public int getCreationTime(){
    return this._creationTime;
  }
  
  /**
   * @return The contents of this {@link #this}, i.e. the aggregation of {@link 
   * jchrest.lib.ListPattern}s that are tests on the {@link 
   * jchrest.architecture.Links} that must be passed to arrive at this {@link 
   * #this} from the relevant modality root {@link jchrest.architecture.Node} 
   * (the contents of the relevant modality root {@link 
   * jchrest.architecture.Node} are excluded from the {@link 
   * jchrest.lib.ListPattern} returned).
   */
  public ListPattern getContents () {
    return _contents;
  }
  
  /**
   * @return The {@link jchrest.lib.Modality} of {@link #this}.
   */
  public Modality getModality(){
    return _modality;
  }
  
  /**
   * @return The unique, immutable reference for this {@link #this}.
   */
  public int getReference () {
    return _reference;
  }
  
  /**
   * @return {@link java.lang.Boolean#TRUE} if this {@link #this} is a root
   * modality {@link jchrest.architecture.Node}, {@link java.lang.Boolean#FALSE}
   * if not.
   */
  public boolean isRootNode(){
    return this._rootNode;
  }
  
  /**************************/
  /**** METRIC FUNCTIONS ****/
  /**************************/
  
  /**
   * @param time
   * @return The number of {@link jchrest.architecture.Node}s below this {@link 
   * #this} including itself at the time specified.
   */
  public int size (int time) {
    int count = 0;
    if(this.getCreationTime() <= time){
      count = 1; // for self
      
      List<Link> children = this.getChildren(time);
      if(children != null){
        for (Link link : children) {
          count += link.getChildNode().size (time);
        }
      }
    }

    return count;
  }

  /**
   * @param time
   * @return Compute the amount of information (the size of the image plus the 
   * number of item and position slots) in this {@link #this} at the time 
   * specified.
   */
  public int information (int time) {
    if (this.isRootNode()) return 0; // root node has 0 information
    
    int information = 0;
    
    ListPattern image = this.getImage(time);
    List<String> itemSlots = this.getItemSlots(time);
    List<Square> positionSlots = this.getPositionSlots(time);
    
    if(image != null) information += image.size();
    if(itemSlots != null) information += itemSlots.size();
    if(positionSlots != null) information += positionSlots.size();

    return information;
  }
  
  /*************************/
  /**** CHILD FUNCTIONS ****/
  /*************************/  
  
  /**
   * @param time
   * 
   * @return The {@link jchrest.architecture.Link}s that were present in this
   * {@link #this} at the time specified.  If this {@link #this} was not created 
   * at the time specified, null is returned.
   */
  public List<Link> getChildren(int time){
    Entry entry = this._childHistory.floorEntry(time);
    return entry == null ? null : (List<Link>)entry.getValue();
  }
  
  /**
   * Attempt to add a new {@link jchrest.architecture.Link} to this {@link 
   * #this} whose test and child {@link jchrest.architecture.Node} is equal to 
   * that specified; notifies observers if successful.
   * 
   * @param test The test for the new {@link jchrest.architecture.Link} to be
   * added.
   * @param childToAdd The {@link jchrest.architecture.Node} that the new {@link 
   * jchrest.architecture.Link} should terminate with.
   * @param time The time that the new {@link jchrest.architecture.Link} should
   * be added.
   * @param currentExperimentName
   * 
   * @return If any of the following are true, {@link java.lang.Boolean#FALSE} 
   * is returned otherwise, {@link java.lang.Boolean#TRUE} is returned:
   * <ul>
   *  <li>
   *    This {@link #this} and the {@link jchrest.architecture.Node} to add
   *    as a child are the same {@link jchrest.architecture.Node} (same result
   *    for {@link jchrest.architecture.Node#getReference()}.
   *  </li>
   *  <li>
   *    This {@link #this} or the {@link jchrest.architecture.Node} to add as a 
   *    child do not exist at the {@code time} specified.
   *  </li>
   *  <li>
   *    The result of invoking {@link jchrest.architecture.Node#getModality()} 
   *    on {@link #this} and the {@code childToAdd} are not equal.
   *  </li>
   *  <li>
   *    The {@code test} specified already exists as a test for {@link #this} or
   *    the {@code childToAdd} is already a child of {@link #this}.
   *  </li>
   *  <li>
   *    An attempt is made to rewrite the child history of {@link #this} (see
   *    {@link jchrest.lib.HistoryTreeMap#put(java.lang.Integer, 
   *    java.lang.Object)). 
   *  </li>
   * </ul>
   */
  boolean addChild(ListPattern test, Node childToAdd, int time, String currentExperimentName) {
    this._model.printDebugStatement("===== Node.addChild() =====");
    this._model.printDebugStatement(
      "- Node " + childToAdd.getReference() + " is to be added as a child to " +
      "Node " + this.getReference() + " at time " + time + " using a link with " +
      "test " + test.toString()
    );
    this._model.printDebugStatement(
      "Checking if the following statements all evaluate to true: " + 
      "\n  ~ Node to add child to and child to add aren't the same: " + (this.getReference() != childToAdd.getReference()) +
      "\n  ~ Node to add child to and child to add both exist at the time the child is to be added: " + (this.getCreationTime() <= time && childToAdd.getCreationTime() <= time) +
      "\n  ~ The modality of the Node to add child to and child to add are the same: " + (this.getModality() == childToAdd.getModality())
    );
    
    boolean childAdded = false;
    
    if(
      this.getReference() != childToAdd.getReference() &&
      (this.getCreationTime() <= time && childToAdd.getCreationTime() <= time) &&
      this.getModality() == childToAdd.getModality()
    ){
      
      this._model.printDebugStatement("  ~ All OK");
      this._model.printDebugStatement(
        "- Checking if the test that will exist on the link from parent to " +
        "child is already present on an link from the parent to the child or " +
        "if the child to add is already a child of the parent."
      );
      
      boolean testOrChildAlreadyPresent = false;
      List<Link> children = this.getChildren(time);
      if(children != null){
        for (Link testLink : children) {
          this._model.printDebugStatement(
            "  ~ Checking link with test " + testLink.getTest() + " and child " +
            "Node with reference " + testLink.getChildNode().getReference()
          );
          if(testLink.getTest().equals(test) || testLink.getChildNode().getReference() == childToAdd.getReference()) {
            this._model.printDebugStatement("    + Match");
            testOrChildAlreadyPresent = true;
            break;
          }
        }
      }
      
      if(!testOrChildAlreadyPresent){
        this._model.printDebugStatement(
          "- Test does not already exist on a link from parent to child and " + 
          "the child to add is not already a child of the parent so " +
          "an attempt will be made to add the child to the parent at time " + 
          time + "."
        );

        List<Link> testLinksToAdd = new ArrayList<>();
        testLinksToAdd.add(new Link (test, childToAdd, time, currentExperimentName));
        if(children != null) testLinksToAdd.addAll(children);
        this._childHistory.put(time, testLinksToAdd);

        this._model.printDebugStatement("- Addition of child to parent successful");
        childAdded = true;
        this.setChanged();
        this.notifyObservers();
      }
      else{
        this._model.printDebugStatement(
          "- Either the test to use already exists as a test link from the " +
          "Node to add the child to or the child Node is already linked to by " +
          "by the parent, exiting."
        );
      }
    }
    else{
      this._model.printDebugStatement("  ~ Statement evaluated to false, exiting.");
    }
    
    this._model.printDebugStatement("- Returning " + childAdded);
    this._model.printDebugStatement("===== RETURN Node.discriminate() =====");
    return childAdded;
  }
  
  /**
   * Attempts to add a new {@link jchrest.architecture.Link} that terminates 
   * with a new {@link jchrest.architecture.Node} that contains an empty image
   * to this {@link #this} at the time specified using the {@link 
   * jchrest.lib.ListPattern} provided.
   * 
   * @param pattern Assumed to be non-empty and constitutes a valid, new test 
   * for this {@link #this}, i.e. this will be used as the new {@link 
   * jchrest.architecture.Link}'s test.
   * 
   * @param time The time the new {@link jchrest.architecture.Link} and 
   * {@link jchrest.architecture.Node} should be created.
   * 
   * @return See {@link jchrest.architecture.Node#addChild(
   * jchrest.lib.ListPattern, jchrest.architecture.Node, int, java.lang.String)}. 
   */
  boolean addChild (ListPattern pattern, int time) {
    
    Node child = new Node (
      _model, 
      ( (_reference == 0) ? pattern : _model.getDomainSpecifics().normalise (_contents.append(pattern))), // don't append to 'Root'
      ( (_reference == 0) ? pattern : _model.getDomainSpecifics().normalise (_contents.append(pattern))), // make same as contents vs Chrest 2
      time
    );

    return this.addChild(
      pattern, 
      child, 
      time, 
      this._model.getCurrentExperimentName()
    );
  }
  
  /*************************/
  /**** IMAGE FUNCTIONS ****/
  /*************************/
  
  /**
   * @param time
   * 
   * @return The image of this {@link #this} at the time specified.  If this 
   * {@link #this} did not exist at the time specified, null is returned.
   */
  public ListPattern getImage(int time){
    Entry entry = this._imageHistory.floorEntry(time);
    return entry == null ? null : (ListPattern)entry.getValue();
  }
  
  /**
   * Set this {@link #this}'s image to that specified at the time passed and 
   * notifies observers if the following statements are all true:
   * <ul>
   *  <li>This {@link #this} exists at the time specified</li>
   *  <li>
   *    This {@link #this} is not a modality root {@link 
   *    jchrest.architecture.Node}</li> 
   *  <li>
   *    The new {@link jchrest.lib.ListPattern} is the same modality as this 
   *    {@link #this}
   *  </li>
   *  <li>
   *    The time specified will not rewrite this {@link #this}'s image history
   *  </li>
   * </ul>
   * 
   * @param image
   * @param time
   * @return {@link java.lang.Boolean#TRUE} if the image of this {@link #this} 
   * is set successfully, {@link java.lang.Boolean#FALSE} if not.
   */
  private boolean setImage (ListPattern image, int time) {
    String func = "- setImage: ";
    this._model.printDebugStatement(
      func + "Attempting to set image of node " + this.getReference() + " to " +
      image.toString() + " at time " + time + ".  This will be done if this " +
      "node was created before or at time " + time + " (creation time of node = " + 
      this.getCreationTime() + "), this node is not a root node and the " + 
      "new image's modality is equal to the modality of this node (modality " + 
      "of this node = " + this.getModality() + ", modality of " +
      "new image = " + image.getModalityString() + ")."
    );
    
    if(
      this._creationTime <= time &&
      !this.isRootNode() &&
      image.getModality() == this.getModality()
    ){
      this._imageHistory.put(time, image);
      this.setChanged();
      this.notifyObservers();

      this._model.printDebugStatement(func + "Set image successful, returning true");
      this._model.printDebugStatement(func + "RETURN");

      return true;
    }
    else{
      this._model.printDebugStatement(func + "Checks failed, returning false");
    }
    
    this._model.printDebugStatement(func + "RETURN");
    return false;
  }
  
  /**
   * Attempts to append new information in the {@link jchrest.lib.ListPattern} 
   * provided to this {@link #this}'s image at the time specified.
   * 
   * @param extension Assumed that invoking {@link 
   * jchrest.lib.ListPattern#isEmpty()} on this would return {@link 
   * java.lang.Boolean#FALSE}.
   * @param time The time that the image of {@link #this} will be extended with
   * with {@code extension}.
   * 
   * @return If the following statements all evaluate to {@link 
   * java.lang.Boolean#TRUE}, {@link java.lang.Boolean#TRUE} is returned 
   * otherwise, {@link java.lang.Boolean#FALSE} is returned:
   * <ul>
   *  <li>
   *    Invoking {@link jchrest.lib.ListPattern#getModality()} on {@code 
   *    extension} equals the result of invoking {@link 
   *    jchrest.architecture.Node#getModality()} on {@link #this}.
   *  </li>
   *  <li>
   *    {@link #this} exists at the {@code time} specified.
   *  </li>
   *  <li>
   *    {@link jchrest.architecture.Node.setImage(jchrest.lib.ListPattern, int) 
   *    returns {@link java.lang.Boolean#TRUE}
   *  </li>
   * </ul>
   * 
   */
  boolean extendImage(ListPattern extension, int time) {
    this._model.printDebugStatement("===== Node.extendImage() =====");
    this._model.printDebugStatement(
      "- The image of Node " + this.getReference() + " is to be extended " + 
      "(familiarised) with pattern " + extension.toString() + " at time " + time
    );
    
    boolean imageExtendedSuccessfully = false;
    
    this._model.printDebugStatement(
      "Checking if:" +
      "\n  ~ The Node to familiarise exists at the time specified:" + (this._creationTime <= time) +
      "\n  ~ The modality of the input pattern matches the modality of the node to familiarise: " + (this.getModality() == extension.getModality())
    );
    
    if(
      this._creationTime <= time &&
      this.getModality() == extension.getModality()
    ){
      this._model.printDebugStatement("  ~ All OK, familiarising Node");
      
      ListPattern newImage = this._model.getDomainSpecifics().normalise(this.getImage(time).append(extension));
      imageExtendedSuccessfully = this.setImage(newImage, time);
      this._model.printDebugStatement("- Image extension " + (imageExtendedSuccessfully ? "successful" : "unsuccessful"));
    }
    else {
      this._model.printDebugStatement("  ~ A statement evaluated to false, exiting");
    }
    
    this._model.printDebugStatement("- Returning " + imageExtendedSuccessfully);
    this._model.printDebugStatement("===== RETURN Node.extendImage() =====");
    return imageExtendedSuccessfully;
  }
  
  /********************************/
  /**** PRODUCTION FUNCTIONS ******/
  /********************************/
  
  /**
   * @param time
   * @return The productions that exist for this {@link #this} at the time 
   * specified.  If this {@link #this} did not exist at the time specified, 
   * null is returned.
   */
  public LinkedHashMap<Node, Double> getProductions(int time){
    Entry<Integer, LinkedHashMap<Node, Double>> entry = this._productionHistory.floorEntry(time);
    return entry == null ? null : entry.getValue();
  }
  
  /**
   * Attempts to add the {@code node} specified to the productions of {@link 
   * #this} with the {@code productionValue} specified at the {@code time}
   * specified.
   * 
   * A production will be created if the following conditions all evaluate to 
   * {@link java.lang.Boolean#TRUE}:
   * 
   * <ul>
   *  <li>
   *    Invoking {@link jchrest.architecture.Node#getCreationTime()} on {@link 
   *    #this} and the {@code node} specified is less than or equal to the 
   *    {@code time} specified.
   *  </li>
   *  <li>
   *    Invoking {@link jchrest.architecture.Node#getModality()} on {@link 
   *    #this} and the {@code node} specified returns {@link 
   *    jchrest.lib.Modality#VISUAL} and {@link jchrest.lib.Modality##ACTION},
   *    respectively.
   *  </li>
   *  <li>
   *    Invoking {@link jchrest.architecture.Node#isRootNode()} on both {@link 
   *    #this} and the {@code node} specified returns {@link 
   *    java.lang.Boolean#FALSE}.
   *  </li>
   *  <li>
   *    A production between {@link #this} and the {@code node} specified does
   *    not already exist at the {@code time} specified.  This is checked by
   *    comparing the result of invoking {@link 
   *    jchrest.architecture.Node#getImage(int)} at the {@code time} specified
   *    in context of {@link #this} and the {@code node} specified using {@link
   *    jchrest.lib.ListPattern#equals(java.lang.Object)}.
   *  </li>
   *  <li>
   *    Adding the production will not rewrite the production history of {@link 
   *    #this}.
   *  </li>
   * </ul>
   * 
   * If a production is successfully created, its initial value will equal 1.0 
   * so that it has a chance of being selected via roulette wheel selection 
   * (see {@link jchrest.architecture.Chrest#generateActionUsingVisualPatternRecognition(int)}.
   * 
   * @param node The {@link jchrest.architecture.Node} that the production 
   * should terminate with.
   * @param time The time the production should be added.
   * 
   * @return If a production is added, {@link 
   * jchrest.lib.ChrestStatus#LEARN_PRODUCTION_SUCCESSFUL} will be returned. 
   * Otherwise, if the production already exists, {@link 
   * jchrest.lib.ChrestStatus#PRODUCTION_ALREADY_LEARNED} will be returned or,
   * if this is not the case, {@link 
   * jchrest.lib.ChrestStatus#LEARN_PRODUCTION_FAILED} is returned.
   */
  ChrestStatus addProduction(Node node, int time){
    this._model.printDebugStatement("===== Node.addProduction() =====");
    this._model.printDebugStatement(
      "- Attempting to add a production between Node " + this.getReference() +
      " and Node " + node.getReference() + " at time " + time
    );
    ChrestStatus result = ChrestStatus.LEARN_PRODUCTION_FAILED;
    
    //No need to check if this node and the node to create a production between
    //are the same since they must have different modalities.  The implication
    //is that the same node cannot belong to two modalities and since the 
    //modality of the nodes to create a production between are checked below, 
    //this will ensure that this node cannot creation a production to itself.
    this._model.printDebugStatement(
      "- Checking if the following statements all evaluate to true: " +
      "\n  ~ Was Node " + this.getReference() + " created on/before the time specified: " + (this.getCreationTime() <= time) +
      "\n  ~ Was Node " + node.getReference() + " created on/before the time specified: " + (node.getCreationTime() <= time) +
      "\n  ~ Does Node " + this.getReference() + " have visual modality: " + (this.getModality() == Modality.VISUAL) +
      "\n  ~ Does Node " + node.getReference() + " have action modality: " + (node.getModality() == Modality.ACTION) +
      "\n  ~ Is Node " + this.getReference() + " not a root Node: " + (!this.isRootNode()) +
      "\n  ~ Is Node " + node.getReference() + " not a root Node: " + (!node.isRootNode())
    );
    if(
      this.getCreationTime() <= time &&
      node.getCreationTime() <= time &&
      this.getModality() == Modality.VISUAL &&
      node.getModality() == Modality.ACTION &&
      !this.isRootNode() &&
      !node.isRootNode()
    ){
      this._model.printDebugStatement("    + All OK");
      
      this._model.printDebugStatement("- Checking if production already exists for Node " + this.getReference());
      LinkedHashMap<Node, Double> currentProductions = this.getProductions(time);
      if(currentProductions != null){
        
        boolean productionAlreadyExists = false;
        for(Node currentProduction: currentProductions.keySet()){
          if(currentProduction.getImage(time).equals(node.getImage(time))){
            productionAlreadyExists = true;
          }
        }
        
        if(!productionAlreadyExists){
          this._model.printDebugStatement("  ~ Production does not already exist");

          this._model.printDebugStatement("- Attempting to add production");
          LinkedHashMap<Node, Double> newProductions = new LinkedHashMap();
          newProductions.put(node, 1.0);
          newProductions.putAll(currentProductions);
          this._productionHistory.put(time, newProductions);

          this._model.printDebugStatement("  ~ Production added successfully");
          this.setChanged();
          this.notifyObservers();
          result = ChrestStatus.LEARN_PRODUCTION_SUCCESSFUL;
        }
        else{
          this._model.printDebugStatement("  ~ Production already exists, exiting");
          result = ChrestStatus.PRODUCTION_ALREADY_LEARNED;
        }
      }
    }
    else{
      this._model.printDebugStatement("    + A statement evaluated to false, exiting");
    }
    
    this._model.printDebugStatement("===== RETURN Node.addProduction() =====");
    return result;
  }
  
  /**
   * Reinforces the production specified using the reinforcement learning theory 
   * that this {@link #this}'s {@link jchrest.architecture.Chrest} model is set 
   * to if the following statements all evaluate to true:
   * 
   * <ol type="1">
   *  <li>
   *    This {@link #this}'s creation time is less than or equal to the time 
   *    specified
   *  </li>
   *  <li>
   *    The creation time of the action {@link jchrest.architecture.Node} that 
   *    constitutes the production to be reinforced is less than or equal to the 
   *    time specified
   *  </li>
   *  <li>
   *    This {@link #this}'s {@link jchrest.lib.Modality} is {@link 
   *    jchrest.lib.Modality#VISUAL}
   *  </li>
   *  <li>
   *    The {@link jchrest.lib.Modality} of the action {@link 
   *    jchrest.architecture.Node} that constitutes the production to be 
   *    reinforced is {@link jchrest.lib.Modality#ACTION}
   *  </li>
   *  <li>
   *    This function is not attempting to rewrite the production history of 
   *    this {@link #this}
   *  </li>
   *  <li>
   *    The {@link jchrest.architecture.Chrest} model that this {@link #this} is
   *    associated with has had its reinforcement learning theory set to one of
   *    those specified in {@link jchrest.lib.ReinforcementLearning}
   *  </li>
   *  <li>
   *    The productions of this {@link #this} contains the action {@link 
   *    jchrest.architecture.Node} provided at the time specified specified
   *  </li>
   * </ol>
   * 
   * @param node The action {@link jchrest.architecture.Node} that constitutes 
   * the production to be reinforced.
   * @param variables The variables that need to be passed for the Reinforcement
   * Learning Theory that will be used to calculate the reinforcement value (see
   * {@link jchrest.lib.ReinforcementLearning}).
   * @param time The time that the reinforcement should occur.
   * 
   * @return True if the production specified is reinforced successfully, false 
   * otherwise.
   */
  boolean reinforceProduction (Node node, Double[] variables, int time){
    this._model.printDebugStatement("===== Node.reinforceProduction() =====");
    this._model.printDebugStatement("- Reference of visual Node specified: " + this._reference);
    this._model.printDebugStatement("- Reference of action Node specified: " + node._reference);
    this._model.printDebugStatement("- Reinforcement learning theory variables specified: " + Arrays.toString(variables));
    this._model.printDebugStatement("- Time reinforcment should occur: " + time);
    
    boolean reinforceProductionSuccessful = false;
    Entry<Integer, LinkedHashMap<Node, Double>> productionsAtTime = this._productionHistory.floorEntry(time);
    Theory reinforcementLearningTheory = this._model.getReinforcementLearningTheory();
    
    this._model.printDebugStatement("Checking if the following all evaluate to true:");
    if(this._model.debug()){
      this._model.printDebugStatement("  ~ Visual Node has been created at time reinforcement should occur: " + (this.getCreationTime() <= time));
      this._model.printDebugStatement("  ~ Action Node has been created at time reinforcement should occur: " + (node.getCreationTime() <= time));
      this._model.printDebugStatement("  ~ Visual Node has Visual modality: " + (this.getModality() == Modality.VISUAL));
      this._model.printDebugStatement("  ~ Action Node has Action modality: " + (node.getModality() == Modality.ACTION));
      this._model.printDebugStatement("  ~ Visual Node has productions at time reinforcement should occur: " + (productionsAtTime != null));
      this._model.printDebugStatement("  ~ Visual Node is engaged in a production with the Action Node: " + (((LinkedHashMap)productionsAtTime.getValue()).containsKey(node)));
      this._model.printDebugStatement("  ~ The reinforcement learning theory of the model associated with the Visual Node has been set: " + (reinforcementLearningTheory != null));
    }
    
    if(
      this.getCreationTime() <= time &&
      node.getCreationTime() <= time &&
      this.getModality() == Modality.VISUAL &&
      node.getModality() == Modality.ACTION &&
      productionsAtTime != null &&
      productionsAtTime.getValue().containsKey(node) &&
      reinforcementLearningTheory != null
    ){
      this._model.printDebugStatement("- All checks evaluate to true, attempting to reinforce production");
      LinkedHashMap<Node, Double> currentProductions = (LinkedHashMap)productionsAtTime.getValue();
      double reinforcedValue = currentProductions.get(node) + reinforcementLearningTheory.calculateReinforcementValue(variables);
      
      LinkedHashMap<Node, Double> newProductions = new LinkedHashMap();
      for(Entry<Node, Double> currentProduction : currentProductions.entrySet()){
        if(currentProduction.getKey().equals(node)){
          newProductions.put(node, reinforcedValue);
        }
        else{
          newProductions.put(currentProduction.getKey(), currentProduction.getValue());
        }
      }
      
      this._productionHistory.put(time, newProductions);
      reinforceProductionSuccessful = true;
    }
    
    if(reinforceProductionSuccessful){
      this.setChanged();
      this.notifyObservers();
    }
    
    this._model.printDebugStatement("- Returning " + reinforceProductionSuccessful);
    this._model.printDebugStatement("===== RETURN Node.reinforceProduction() =====");
    return reinforceProductionSuccessful;
  }
  
  /*********************************/
  /**** SEMANTIC LINK FUNCTIONS ****/
  /*********************************/
  
  /**
   * @param time
   * 
   * @return The {@link jchrest.architecture.Node}s that this {@link #this} is
   * semantically linked to at the time specified.  If this {@link #this} was 
   * not created at the time specified, null is returned.
   */
  public List<Node> getSemanticLinks(int time){
    Entry entry = this._semanticLinksHistory.floorEntry(time);
    return entry == null ? null : (List<Node>)entry.getValue();
  }
  
  /**
   * Adds a new semantic link to this {@link #this}'s semantic links at the time
   * specified if the following conditions are all true:
   * 
   * <ul>
   *  <li>
   *    This {@link #this} and the {@link jchrest.architecture.Node} passed are
   *    not the same {@link jchrest.architecture.Node}
   *  </li>
   *  <li>
   *    This {@link #this}'s creation time is less than or equal to the time 
   *    specified
   *  </li>
   *  <li>
   *    The creation time of the {@link jchrest.architecture.Node} to add a 
   *    semantic link to is less than or equal to the time specified
   *  </li>
   *  <li>
   *    This {@link #this} is not a root node
   *  </li>
   *  <li>
   *    The {@link jchrest.architecture.Node} to add a semantic link to is not
   *    a root node
   *  </li>
   *  <li>
   *    This function is not attempting to rewrite the semantic link history of 
   *    this {@link #this}
   *  </li>
   *  <li>
   *    This {@link #this}'s semantic links does not already contain the {@link 
   *    jchrest.architecture.Node} specified
   *  </li>
   * </ul>
   * 
   * @param node
   * @param time 
   * @return {@link java.lang.Boolean#TRUE} if the semantic link is added, 
   * {@link java.lang.Boolean#FALSE} if not.
   */
  boolean addSemanticLink(Node node, int time){
    if(
      this != node &&
      this.getCreationTime() <= time &&
      node.getCreationTime() <= time &&
      !this.isRootNode() &&
      !node.isRootNode()
    ){
      List<Node> semanticLinks = this.getSemanticLinks(time);
      if(semanticLinks != null && !semanticLinks.contains(node)){
        
        ArrayList<Node> semanticLinksToAdd = new ArrayList();
        semanticLinksToAdd.add(node);
        semanticLinksToAdd.addAll(semanticLinks);
        this._semanticLinksHistory.put(time, semanticLinksToAdd);
        this.setChanged();
        this.notifyObservers();
        return true;
      }
    }
    
    return false;
  }
  
  /***********************************/
  /**** ASSOCIATED NODE FUNCTIONS ****/
  /***********************************/
  
  /**
   * @param time
   * 
   * @return The {@link jchrest.architecture.Node} associated with this {@link
   * #this} at the time specified. If this {@link #this} was not associated with
   * another {@link jchrest.architecture.Node} at the time specified then null
   * is returned. 
   */
  public Node getAssociatedNode(int time){
    Entry entry = this._associatedNodeHistory.floorEntry(time);
    return entry == null ? null : (Node)entry.getValue();
  }
  
  /**
   * Set the {@link jchrest.architecture.Node} that is associated with this 
   * {@link #this} at the time specified and set the learning clock of the 
   * {@link jchrest.architecture.Chrest} model associated with this {@link 
   * #this} to the time this function was invoked plus the time returned by 
   * {@link jchrest.architecture.Chrest#getTimeToCreateSemanticLink()}.
   * 
   * @param node
   * @param time
   * @return True if this function has now associated the two {@link 
   * jchrest.architecture.Node}s together, false if not.  The {@link 
   * jchrest.architecture.Node}s won't be associated if any of the following
   * conditions evaluate to true:
   * <ul>
   *  <li>
   *    This {@link #this} and the {@link jchrest.architecture.Node} to create
   *    the association between are the same {@link jchrest.architecture.Node}.
   *  </li>
   *  <li>
   *    This {@link #this} hasn't been created when this function is invoked.
   *  </li>
   *  <li>
   *    The {@link jchrest.architecture.Node} to be associated with this 
   *    {@link #this} hasn't been created when this function is invoked.
   *  </li>
   *  <li>
   *    This {@link #this} is a root node.
   *  </li>
   *  <li>
   *    The {@link jchrest.architecture.Node} to associate with {@link #this} is
   *    a root node.
   *  </li>
   *  <li>
   *    {@link #this} is not currently associated with the {@link 
   *    jchrest.architecture.Node} to be associated.
   *  </li>
   *  <li>
   *    This function will not rewrite the associated node history of {@link 
   *    #this} (see {@link jchrest.architecture.Chrest#isRewritingHistory(
   *    java.util.TreeMap, int)).
   *  </li>
   * </ul>
   */
  boolean setAssociatedNode (Node node, int time) {
    if(
      node != this &&
      this.getCreationTime() <= time &&
      node.getCreationTime() <= time &&
      !this.isRootNode() &&
      !node.isRootNode() &&
      this.getAssociatedNode(time) != node
    ){
      this._associatedNodeHistory.put(time, node);
      setChanged ();
      notifyObservers ();
      return true;
    }
    
    return false;
  }
  
  /********************************/
  /**** NAMED BY FUNCTIONALITY ****/
  /********************************/
  
  /**
   * @param time
   * 
   * @return The {@link jchrest.architecture.Node} that named this {@link #this}
   * at the time specified or null if no {@link jchrest.architecture.Node} named
   * this {@link #this} at the time specified.
   */
  public Node getNamedBy(int time){
    Entry<Integer, Node> mostRecentlyNamedBy = this._namedByHistory.floorEntry(time);
    return mostRecentlyNamedBy == null ? null : mostRecentlyNamedBy.getValue();
  }
  
  /**
   * Modify what {@link jchrest.lib.Modality#VERBAL} {@link 
   * jchrest.architecture.Node} is linked to this {@link #this}.
   * 
   * @param node
   * @param time
   */
  boolean setNamedBy (Node node, int time) {
    
    //No need to check if this node and the node it is named by are the same 
    //node since they must have different modalities.  The implication is that 
    //the same node cannot belong to two modalities and since the  modality of 
    //the nodes to create a production between are checked below, this will 
    //ensure that a node is not named by itself. 
    if(
      this.getCreationTime() <= time &&
      node.getCreationTime() <= time &&
      !this.isRootNode() &&
      !node.isRootNode() &&
      this.getModality() == Modality.VISUAL &&
      node.getModality() == Modality.VERBAL
    ){
      this._namedByHistory.put(time, node);
      setChanged ();
      notifyObservers ();
      return true;
    }
    
    return false;
  }
  
  /********************************/
  /**** TEMPLATE FUNCTIONALITY ****/
  /********************************/
  
  /**
   * @param time
   * @return {@link java.lang.Boolean#TRUE} if {@link #this} is a template at 
   * the time specified otherwise, {@link java.lang.Boolean#FALSE}.
   */
  public boolean isTemplate(int time){
    Entry<Integer, Boolean> result = this._templateHistory.floorEntry(time);
    return result == null ? false : result.getValue();
  }
  
  /** 
   * @param time
   * @return {@link java.lang.Boolean#TRUE} if all of the following conditions
   * are true at the time specified, {@link java.lang.Boolean#FALSE} otherwise:
   * <ol type="1">
   *  <li>
   *    {@link #this} is not a root node. This may seem superfluous considering
   *    there is a check on the depth of {@link #this} performed below as well.
   *    However, it may be that its possible to specify that the minimum depth
   *    that a node must be before it can become a template is 0, this would 
   *    make root nodes pass this function.  So, better to be safe than sorry.
   *  </li>
   *  <li>
   *    {@link #this} exists at the time specified.
   *  </li>
   *  <li>
   *    {@link #this} isn't already a template at the time specified (this also
   *    means that the function won't rewrite the item/position slot or filled
   *    item/position slot history of {@link #this} since the data structures
   *    that store the history of this information only exist if {@link #this} 
   *    is already a template).
   *  </li>
   *  <li>
   *    The depth of {@link #this} in long-term memory surpasses the threshold 
   *    stipulated in the {@link jchrest.architecture.Chrest} model that {@link 
   *    #this} is associated with (see {@link 
   *    jchrest.architecture.Chrest#getMinNodeDepthInNetworkToBeTemplate()}).
   *  </li>
   *  <li>
   *    Gather together: 
   *    <ol>
   *      <li>{@link #this}'s image.</li>
   *      <li>The images of {@link #this}'s immediate children</li>
   *      <li>
   *        The images of {@link jchrest.architecture.Node}s that {@link #this}
   *        is semantically linked to
   *      </li>
   *    </ol>
   *    Then, after removing {@link #this}'s contents (see {@link 
   *    jchrest.architecture.Node#getContents()} from the cumulative image 
   *    generated, check if an item or position occurs more than the threshold 
   *    stipulated in the {@link jchrest.architecture.Chrest} model that {@link 
   *    #this} is associated with (see {@link 
   *    jchrest.architecture.Chrest#getMinItemOrPositionOccurrencesToBeSlotValue()   */
  boolean canBeTemplate (int time) {
    if(
      !this.isRootNode() &&
      this.getCreationTime() <= time &&
      !this.isTemplate(time) &&
      this.getContents().size () >= this._model.getMinNodeDepthInNetworkToBeTemplate() //Check depth of node.
    ){
    
      //Construct cumulative image.
      List<ListPattern> cumulativeImage = new ArrayList<ListPattern> ();
      ListPattern contentsOfThisNode = this.getContents();

      cumulativeImage.add( this.getImage(time).remove(contentsOfThisNode) );

      List<Link> children = this.getChildren(time);
      if(children != null){
        for (Link link : children) {
          cumulativeImage.add( link.getChildNode().getImage(time).remove(contentsOfThisNode) );
        }
      }

      List<Node> semanticLinks = this.getSemanticLinks(time);
      if(semanticLinks != null){
        for (Node node : semanticLinks) {
          cumulativeImage.add( node.getImage(time).remove(contentsOfThisNode) );
        }
      }

      //Create a hashmap of occurrences of items and positions in the cumulative
      //image.
      Map<String,Integer> itemCount = new LinkedHashMap<String, Integer> ();
      Map<Integer,Integer> positionCount = new LinkedHashMap<Integer, Integer> ();
      for (ListPattern cumulativeImagePattern : cumulativeImage) {
        for (PrimitivePattern cumulativeImagePatternPrimitive : cumulativeImagePattern) {
          if (cumulativeImagePatternPrimitive instanceof ItemSquarePattern) {
            ItemSquarePattern cumulativeImageIsp = (ItemSquarePattern)cumulativeImagePatternPrimitive;

            if (itemCount.containsKey (cumulativeImageIsp.getItem ())) {
              itemCount.put (cumulativeImageIsp.getItem (), itemCount.get(cumulativeImageIsp.getItem ()) + 1);
            } else {
              itemCount.put (cumulativeImageIsp.getItem (), 1);
            }

            Integer posn_key = cumulativeImageIsp.getRow () + 1000 * cumulativeImageIsp.getColumn ();
            if (positionCount.containsKey (posn_key)) {
              positionCount.put (posn_key, positionCount.get(posn_key) + 1);
            } else {
              positionCount.put (posn_key, 1);
            }
          }
        }
      }

      //Check if any item occurs more frequently than the stipulated threshold.
      for (Entry<String, Integer> itemOccurrences : itemCount.entrySet()) {
        if (itemOccurrences.getValue() >= _model.getMinItemOrPositionOccurrencesToBeSlotValue ()) {
          return true;
        }
      }

      //Check if any position occurs more frequently than the stipulated 
      //threshold.
      for (Entry<Integer, Integer> positionOccurrences : positionCount.entrySet()) {
        if (positionOccurrences.getValue() >= _model.getMinItemOrPositionOccurrencesToBeSlotValue ()) {
          return true;
        }
      }
    }
    
    return false;
  }
  
  /**
   * Makes {@link #this} into a template at the time specified, if {@link 
   * jchrest.architecture.Node#canBeTemplate(int)} returns {@link 
   * java.lang.Boolean#TRUE} and the function will not rewrite the template/slot
   * history of {@link #this}.
   * 
   * @param time
   */
  boolean makeTemplate (int time) {
    if(this.canBeTemplate(time)){
      
      //Instantiate the slot history instance variables, if necessary (this Node 
      //may have been a template then cleared in which case, instantiation of
      //these instance variables is not required).
      if(this._itemSlotsHistory == null) this._itemSlotsHistory = new HistoryTreeMap();
      if(this._positionSlotsHistory == null) this._positionSlotsHistory = new HistoryTreeMap();
      if(this._filledItemSlotsHistory == null) this._filledItemSlotsHistory = new HistoryTreeMap();
      if(this._filledPositionSlotsHistory == null) this._filledPositionSlotsHistory = new HistoryTreeMap();
      
      // Since four historical instance variables are required to make a Node
      // into a template, check that adding an entry with the time specified 
      // will not rewrite any of their histories.
      if(
        !this._itemSlotsHistory.rewritingHistory(time) &&
        !this._positionSlotsHistory.rewritingHistory(time) &&
        !this._filledItemSlotsHistory.rewritingHistory(time) &&
        !this._filledPositionSlotsHistory.rewritingHistory(time) 
      ){
        
        // When a Node is converted into a template, no slots should be filled.
        this._filledItemSlotsHistory.put(time, new ArrayList());
        this._filledPositionSlotsHistory.put(time, new ArrayList());
      
        //Construct cumulative image
        List<ListPattern> cumulativeImage = new ArrayList<>();
        ListPattern contentsOfThisNode = this.getContents();
        cumulativeImage.add( this.getImage(time).remove(contentsOfThisNode) );

        List<Link> children = this.getChildren(time);
        if(children != null){
          for (Link link : children) {
            cumulativeImage.add( link.getChildNode().getImage(time).remove(contentsOfThisNode) );
          }
        }

        List<Node> semanticLinks = this.getSemanticLinks(time);
        if(semanticLinks != null){
          for (Node node : semanticLinks) {
            cumulativeImage.add( node.getImage(time).remove(contentsOfThisNode) );
          }
        }

        //Create a hashmap of occurrences of items and positions in the cumulative
        //image.
        Map<String,Integer> itemCount = new LinkedHashMap<String, Integer> ();
        Map<Integer,Integer> positionCount = new LinkedHashMap<Integer, Integer> ();
        for (ListPattern cumulativeImagePattern : cumulativeImage) {
          for (PrimitivePattern cumulativeImagePatternPrimitive : cumulativeImagePattern) {
            if (cumulativeImagePatternPrimitive instanceof ItemSquarePattern) {
              ItemSquarePattern cumulativeImageIsp = (ItemSquarePattern)cumulativeImagePatternPrimitive;

              if (itemCount.containsKey (cumulativeImageIsp.getItem ())) {
                itemCount.put (cumulativeImageIsp.getItem (), itemCount.get(cumulativeImageIsp.getItem ()) + 1);
              } else {
                itemCount.put (cumulativeImageIsp.getItem (), 1);
              }

              Integer posn_key = cumulativeImageIsp.getRow () + 1000 * cumulativeImageIsp.getColumn ();
              if (positionCount.containsKey (posn_key)) {
                positionCount.put (posn_key, positionCount.get(posn_key) + 1);
              } else {
                positionCount.put (posn_key, 1);
              }
            }
          }
        }

        // Construct and add item and position slot values.  Since there was a
        // history rewrite check earlier, there is no requirement at this point
        // to check the result of "putting" the slot values in the respective
        // historical instance variables because this will always succeed.
        List<String> itemSlotEntry = new ArrayList();
        for (Entry<String, Integer> itemOccurrences : itemCount.entrySet()) {
          if (itemOccurrences.getValue() >= _model.getMinItemOrPositionOccurrencesToBeSlotValue ()) {
            itemSlotEntry.add (itemOccurrences.getKey());
          }
        }
        this._itemSlotsHistory.put(time, itemSlotEntry);

        List<Square> positionSlotEntry = new ArrayList();
        for (Entry<Integer, Integer> positionOccurrences : positionCount.entrySet()) {
          if (positionOccurrences.getValue() >= _model.getMinItemOrPositionOccurrencesToBeSlotValue ()) {
            Integer posnKey = positionOccurrences.getKey();
            positionSlotEntry.add (new Square (
              posnKey / 1000, 
              posnKey - (1000 * (posnKey/1000))
            ));
          }
        }
        this._positionSlotsHistory.put(time, positionSlotEntry);

        // Finally, add an entry to specify that the Node is a template at the
        // time specified.
        this._templateHistory.put(time, true);
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Clears non-filled/filled item/position slots of {@link #this} at the time 
   * specified if {@link #this} is a template (see {@link jchrest.architecture.
   * Node#isTemplate(int)}) so {@link #this} is no longer considered to be a 
   * template.
   * 
   * @param time
   * @return {@link java.lang.Boolean#TRUE} if {@link #this} is successfully
   * converted to a non-template {@link jchrest.architecture.Node}, {@link 
   * java.lang.Boolean#FALSE} otherwise.
   */
  boolean makeNonTemplate(int time){
    if(
      this.isTemplate(time) &&
      !this._itemSlotsHistory.rewritingHistory(time) &&
      !this._positionSlotsHistory.rewritingHistory(time) &&
      !this._filledItemSlotsHistory.rewritingHistory(time) &&
      !this._filledPositionSlotsHistory.rewritingHistory(time) &&
      !this._templateHistory.rewritingHistory(time)
    ){
      this._itemSlotsHistory.put(time, null);
      this._positionSlotsHistory.put(time, null);
      this._filledItemSlotsHistory.put(time, null);
      this._filledPositionSlotsHistory.put(time, null);
      this._templateHistory.put(time, false);
      return true;
    }
    return false;
  }
  
  /**
   * Attempt to fill some of this {@link #this}'s slots using the items in the 
   * given pattern.  This will only work if the conditions below are all true:
   * <ul>
   *  <li>
   *    {@link #this} is a template at the time specified (also means {@link 
   *    #this} exists at the time specified.
   *  </li>
   *  <li>
   *    The modality of the {@link jchrest.lib.ListPattern} to fill {@link 
   *    #this}'s slots with is of the same modality as {@link #this}'s image.
   *  </li>
   *  <li>
   *    Filling the slots at this point in time would not rewrite the filled
   *    item/position slot history of {@link #this}.
   * </ul>
   * 
   * @param pattern
   * @param time
   * @return The number of slots filled or null if any condition listed above 
   * evaluates to false.
   */
  Integer fillSlots (ListPattern pattern, int time) {
    if(
      this.isTemplate(time) &&
      pattern.getModality() == this.getModality() &&
      !this._filledItemSlotsHistory.rewritingHistory(time) &&
      !this._filledPositionSlotsHistory.rewritingHistory(time)
    ){
      List<ItemSquarePattern> itemsForItemSlot = new ArrayList();
      List<ItemSquarePattern> itemsForPositionSlot = new ArrayList();

      //Check each primitive pattern in the pattern passed to this function to see
      //if:
      // 
      //a) It is an ItemSquarePattern
      //b) Its item/position is a potential slot value.
      //
      //If both conditions are true, add the first item or position slot value to 
      //the filled item/position slot data structure.
      for (int index = 0; index < pattern.size (); index++) {
        boolean slotFilled = false;
        if (pattern.getItem(index) instanceof ItemSquarePattern) {
          ItemSquarePattern item = (ItemSquarePattern)(pattern.getItem (index));

          // only try to fill a slot if item is not already in image or slot
          List<ItemSquarePattern> filledItemSlots = this.getFilledItemSlots(time);
          List<ItemSquarePattern> filledPositionSlots = this.getFilledPositionSlots(time);
          if (
            !this.getImage(time).contains(item) && 
            (filledItemSlots != null && !filledItemSlots.contains(item)) && 
            (filledPositionSlots != null && !filledPositionSlots.contains(item))
          ) { 

            // 1. check the item slots
            List<String> itemSlots = this.getItemSlots(time);
            if(itemSlots != null){
              for (String slot : itemSlots) {
                if (!slotFilled) {
                  if (slot.equals(item.getItem ())) {
                    itemsForItemSlot.add (item);
                    slotFilled = true;
                  }
                }
              }
            }

            // 2. check the position slots
            List<Square> positionSlots = this.getPositionSlots(time);
            if(positionSlots != null){
              for (Square slot : positionSlots) {
                if (!slotFilled) {
                  if (
                    slot.getRow () == item.getRow() &&
                    slot.getColumn () == item.getColumn()
                  ){
                    itemsForPositionSlot.add (item);
                    slotFilled = true;
                  }
                }
              }
            }
          }
        }
      }

      if(!itemsForItemSlot.isEmpty()) this._filledItemSlotsHistory.put(time, itemsForItemSlot);
      if(!itemsForPositionSlot.isEmpty()) this._filledPositionSlotsHistory.put(time, itemsForPositionSlot);
      
      return itemsForItemSlot.size() + itemsForPositionSlot.size();
    }
    
    return null;
  }
  
  /**
   * Clears {@link #this}'s filled item/position slots at the time specified
   * if {@link #this} is a template.
   * 
   * @param time 
   * @return {@link java.lang.Boolean#TRUE} if filled slots are cleared, 
   * {@link java.lang.Boolean#FALSE} otherwise.
   */
  boolean clearFilledSlots(int time){
    if(this.isTemplate(time)){
      this._filledItemSlotsHistory.put(time, new ArrayList());
      this._filledPositionSlotsHistory.put(time, new ArrayList());
      return true;
    }
    
    return false;
  }

  /**
   * @param time
   * 
   * @return The item slots of {@link #this} at the time specified (may be 
   * empty).  Null is returned if {@link #this} is not a template at the time 
   * specified. 
   */
  public List<String> getItemSlots(int time){
    if(this._itemSlotsHistory != null){
      Entry itemSlotsAtTime = this._itemSlotsHistory.floorEntry(time);
      if(itemSlotsAtTime != null){
        return (List<String>)itemSlotsAtTime.getValue();
      }
    }
    return null;
  }
  
  /**
   * @param time
   * 
   * @return The position slots of {@link #this} at the time specified (may be 
   * empty).  Null is returned if {@link #this} is not a template at the time 
   * specified. 
   */
  public List<Square> getPositionSlots(int time){
    if(this._positionSlotsHistory != null){
      Entry positionSlotsAtTime = this._positionSlotsHistory.floorEntry(time);
      if(positionSlotsAtTime != null){
        return (List<Square>)positionSlotsAtTime.getValue();
      }
    }
    return null;
  }
  
  /**
   * @param time
   * @return The filled item slots of {@link #this} at the time specified (may 
   * be empty).  Null is returned if {@link #this} is not a template at the time 
   * specified. 
   */
  public List<ItemSquarePattern> getFilledItemSlots(int time) {
    if(this._filledItemSlotsHistory != null){
      Entry filledItemSlotsAtTime = this._filledItemSlotsHistory.floorEntry(time);
      if(filledItemSlotsAtTime != null){
        return (List<ItemSquarePattern>)filledItemSlotsAtTime.getValue();
      }
    }
    return null;
  }

  /**
   * @param time
   * @return The filled item slots of {@link #this} at the time specified (may 
   * be empty).  Null is returned if {@link #this} is not a template at the time 
   * specified. 
   */
  public List<ItemSquarePattern> getFilledPositionSlots(int time) {
    if(this._filledPositionSlotsHistory != null){
      Entry filledPositionSlotsAtTime = this._filledPositionSlotsHistory.floorEntry(time);
      if(filledPositionSlotsAtTime != null){
        return (List<ItemSquarePattern>)filledPositionSlotsAtTime.getValue();
      }
    }
    return null;
  }
  
  /**
   * @return The contents of filled item/position with no duplicate {@link
   * jchrest.lib.Pattern}s.  Three values may be returned: null (if {@link 
   * #this} is not a template at the time specified or has no entry in its 
   * filled item/position slot history data structures at a time earlier than or 
   * equal to the time specified), an empty {@link jchrest.lib.ListPattern} (if 
   * {@link #this} is a template but hasn't had either its item/position slots 
   * filled earlier than or equal to the time specified) or a non-empty {@link 
   * jchrest.lib.ListPattern} (if {@link #this} is a template and has had either 
   * its item/position slots filled earlier than or equal to the time 
   * specified).
   */
  public ListPattern getFilledSlots (int time) {
    List<ItemSquarePattern> filledItemSlots = this.getFilledItemSlots(time);
    List<ItemSquarePattern> filledPositionSlots = this.getFilledPositionSlots(time);
    
    if(filledItemSlots != null || filledPositionSlots != null){
      ListPattern listPattern = new ListPattern(this.getModality());
      List<ItemSquarePattern> filledItemAndPositionSlots = new ArrayList();
      if(filledItemSlots != null) filledItemAndPositionSlots.addAll(filledItemSlots);
      if(filledPositionSlots != null) filledItemAndPositionSlots.addAll(filledPositionSlots);
      
      for (ItemSquarePattern filledSlotValue : filledItemAndPositionSlots) {
        boolean slotValueAlreadyInListPattern = false;
        for(PrimitivePattern pattern : listPattern){
          ItemSquarePattern isp = (ItemSquarePattern)pattern;
          if(isp.toString().equals(filledSlotValue.toString())){
            slotValueAlreadyInListPattern = true;
          }
        }
        
        if(!slotValueAlreadyInListPattern){
          listPattern.add(filledSlotValue);
        }
      }

      return listPattern;
    }
    return null;
  }
  
  /*********************************/
  /**** MISCELLANEOUS FUNCTIONS ****/
  /*********************************/
  
  /**
   * 
   * @param time
   * 
   * @return A {@link jchrest.lib.ListPattern} containing the concatenation of 
   * {@link #this#getContents()}, {@link #this#getImage(int)} and {@link 
   * #this#getFilledSlots(int)} at the {@code time} specified, with no duplicate
   * {@link jchrest.lib.PrimitivePattern PrimitivePatterns}.
   */
  public ListPattern getAllInformation(int time){
    this._model.printDebugStatement("===== Node.getInformation() =====");
    this._model.printDebugStatement("- Getting information at time " + time);
    ListPattern information = new ListPattern(this._modality);
    ListPattern contents = this.getContents();
    ListPattern image = this.getImage(time);
    ListPattern filledSlots = this.getFilledSlots(time);
    
    this._model.printDebugStatement("- Contents: " + contents.toString());
    this._model.printDebugStatement("- Image: " + (image == null ? "null" : image.toString()));
    this._model.printDebugStatement("- Filled slots: " + (filledSlots == null ? "null" : filledSlots.toString()));
    
    information = information.append(contents);
    this._model.printDebugStatement("- Information after appending contents: " + information.toString());
    
    if(image != null){
      image = image.remove(contents);
      information = information.append(image);
    }
    this._model.printDebugStatement("- Information after appending image: " + information.toString());
    
    if(filledSlots != null){
      filledSlots = filledSlots.remove(contents).remove(image);
      information = information.append(filledSlots);
    }
    this._model.printDebugStatement("- Information after appending filled slots: " + information.toString());
    
    this._model.printDebugStatement("- Returning " + information.toString());
    this._model.printDebugStatement("===== RETURN Node.getInformation() =====");
    return information;
  }
  
  /***********************/
  /**** VNA FUNCTIONS ****/
  /***********************/
  
  /**
   * Write information (reference and contents of {@link #this} in VNA format.
   * 
   * @param writer
   * @param time
   * @throws java.io.IOException
   */
  public void writeNodeAsVna (Writer writer, int time) throws IOException {
    writer.write ("" + _reference + " \"" + _contents.toString() + "\"\n");
    
    List<Link> children = this.getChildren(time);
    if(children != null){
      for (Link link : children) {
        link.getChildNode().writeNodeAsVna (writer, time);
      }
    }
  }

  public void writeLinksAsVna (Writer writer, int time) throws IOException {
    List<Link> children = this.getChildren(time);
    if(children != null){
      
      // write my links
      for (Link link : children) {
        writer.write ("" + _reference + " " + link.getChildNode().getReference () + "\n");
      }
    
      // repeat for children
      for (Link link : this.getChildren(time)) {
        link.getChildNode().writeLinksAsVna (writer, time);
      }
    }
  }

  public void writeSemanticLinksAsVna (Writer writer, int time) throws IOException {
    // write my links
    List<Node> semanticLinks = this.getSemanticLinks(time);
    if(semanticLinks != null){
      for (Node node : semanticLinks) {
        writer.write ("" + _reference + " " + node.getReference () + "\n");
      }
    }
    
    // repeat for children
    List<Link> children = this.getChildren(time);
    if(children != null){
      for (Link link : children) {
        link.getChildNode().writeSemanticLinksAsVna (writer, time);
      }
    }
  }
}

