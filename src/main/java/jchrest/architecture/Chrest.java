// Copyright (c) 2012, Peter C. R. Lane
// with contributions on the emotions code by Marvin Schiller.
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.architecture;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.TreeMap;
import jchrest.database.DatabaseInterface;
import jchrest.gui.experiments.Experiment;
import jchrest.lib.*;
import jchrest.lib.ReinforcementLearning.ReinforcementLearningTheories;

/**
 * A CHREST model.
 * 
 * All times are specified in milliseconds.
 * 
 * @author Peter C. R. Lane
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class Chrest extends Observable {
  
  /****************************************************************************/
  /****************************************************************************/
  /**************************** INSTANCE VARIABLES ****************************/
  /****************************************************************************/
  /****************************************************************************/
  
  /**************************/
  /**** Simple variables ****/
  /**************************/
  
  private final int _creationTime;
  private DomainSpecifics _domainSpecifics;
  private String[] _publiclyExecutableMethods = {
    "learn",
    "recognise"
  };
  
  /*************************/
  /**** Debug variables ****/
  /*************************/
  
  private boolean _debug = false;
  private PrintStream _debugOutput = System.out;
  
  /*************************/
  /**** Clock variables ****/
  /*************************/
  
  /** 
   * When declaring a new clock, please ensure that its instance variable name 
   * ends with "Clock".  This will ensure that automated operations using Java 
   * reflection will work with new variables without having to implement new 
   * code.
   */
  
  // Attention parameters
  private int _attentionClock;
  
  private int _ltmLinkTraversalTime = 10; //From "Perception and Memory in Chess" by deGroot and Gobet
  private int _timeToUpdateStm = 50; //From "Perception and Memory in Chess" by deGroot and Gobet
  private int _timeToRetrieveItemFromStm = 10;
  private int _salientSquareSelectionTime = 150; //From "Perception and Memory in Chess" by deGroot and Gobet
  private int _randomSquareSelectionTime = 150; //From "Perception and Memory in Chess" by deGroot and Gobet
  private int _fillTemplateSlotTime = 250;
  private int _visualSpatialFieldPhysicalObjectEncodingTime = 25; 
  private int _visualSpatialFieldEmptySquareEncodingTime = 10; 
  private int _visualSpatialFieldAccessTime = 100; //From "Mental Imagery and Chunks" by Gobet and Waters
  private int _visualSpatialFieldObjectMovementTime = 50;  //From "Mental Imagery and Chunks" by Gobet and Waters
  private int _recognisedVisualSpatialObjectLifespan = 10000; 
  private int _unrecognisedVisualSpatialObjectLifespan = 8000;
  
  // Cognitive parameters
  private int _cognitionClock;
  
  private int _addProductionTime = 10000;
  private int _nodeComparisonTime = 50;
  private int _discriminationTime = 10000;
  private int _familiarisationTime = 2000;
  private int _reinforceProductionTime = 50;
  private int _namingLinkCreationTime = 10000;
  private int _semanticLinkCreationTime = 10000;
  
  // Perceiver parameters
  private int _perceiverClock;
  
  private int _saccadeTime = 30;
  
  /********************************/
  /**** Architecture variables ****/
  /********************************/
  
  // Most of these variables are instantiated when a 
  // jchrest.acrchitecture.Chrest instance is constructed since their 
  // constructors require times of creation or a jchrest.architecture.Chrest
  // instance.
  
  /**
   * When creating a new long-term memory modality, please ensure that its
   * instance variable name adheres to the following pattern: "_modalityLtm".
   * This will ensure that generic operations using Java reflection will work
   * with new long-term memory modalities.
   */
  private Node _visualLtm;
  private Node _verbalLtm;
  private Node _actionLtm;
  
  private HistoryTreeMap _totalNumberVisualLtmNodes = new HistoryTreeMap();
  private HistoryTreeMap _totalNumberVerbalLtmNodes = new HistoryTreeMap();
  private HistoryTreeMap _totalNumberActionLtmNodes = new HistoryTreeMap();
  private int _nextLtmNodeReference = 0;
  
  /**
   * When declaring a new short-term memory modality, please ensure that its
   * instance variable name adheres to the following pattern: "_modalityStm". 
   * This will ensure that generic operations using Java reflection will work
   * with new short-term memory modalities.
   */
  private Stm _visualStm;
  private Stm _verbalStm;
  private Stm _actionStm; // TODO: Incorporate into displays
  
  private final Perceiver _perceiver;
  private final TreeMap<Integer, VisualSpatialField> _visualSpatialFields = new TreeMap();
  private final EmotionAssociator _emotionAssociator = new EmotionAssociator();
  
  /****************************/
  /**** Learning variables ****/
  /****************************/
  
  // The probability that discrimination or familiarisation will occur when 
  // requested (if the learning resource is free).
  private float _rho = 1.0f; 
  
  private boolean _canCreateSemanticLinks = true;
  private int _nodeImageSimilarityThreshold = 4;
  private int _maximumSemanticLinkSearchDistance = 1;
  
  private boolean _canCreateTemplates = true;
  private int _minNodeDepthInNetworkToBeTemplate = 3;
  private int _minItemOrPositionOccurrencesInNodeImagesToBeSlotValue = 2;
  
  private ReinforcementLearningTheories _reinforcementLearningTheory = null; //Must be set explicitly using Chrest.setReinforcementLearningTheory();
  
  /******************************************/
  /**** Experiment information variables ****/
  /******************************************/
  
  private boolean _loadedIntoExperiment = true;
  private boolean _engagedInExperiment = true;
  private Experiment _currentExperiment = null;
  
  //Stores the names of the experiments that this model has been loaded into.
  //Used primarily for rendering the model's state graphically.
  private List<String> _experimentsLocatedInNames = new ArrayList<>();
  
  //Stores the time that an experiment (keys) was run until (values).  Used 
  //primarily for rendering the model's state graphically.
  private Map<String, Integer> _experimentNamesAndMaximumTimes = new HashMap<>();
    
  //Stores the string that is prepended to pre-experiment names in the 
  //"_experimentsLocatedInNames" instance variable.
  private final static String _preExperimentPrepend = "Pre-expt: ";
  
  /***************************************/
  /***** Execution history variables *****/
  /***************************************/
  
  private final DatabaseInterface _databaseInterface;
  
  //The model should not record its execution history by default since it can
  //slow down its operation significantly.
  private boolean _executionHistoryRecordingEnabled = false;
  
  //Save names of important database variables so consistency with operations is 
  //ensured.
  public final static String _executionHistoryTableName = "history";
  public final static String _executionHistoryTableRowIdColumnName = "ID";
  public final static String _executionHistoryTableTimeColumnName = "TIME";
  public final static String _executionHistoryTableOperationColumnName = "OPERATION";
  public final static String _executionHistoryTableInputColumnName = "INPUT";
  public final static String _executionHistoryTableDescriptionColumnName = "DESCRIPTION";
  public final static String _executionHistoryTableOutputColumnName = "OUTPUT";
  private final static String _executionHistoryUniqueRowIndexName = "execution_row_unique";
  
  //Stores execution history table column metadata. This negates querying the 
  //database for such information (possibly slowing performance) and allows for 
  //consistency of operations on information from the execution history database
  //table.  Each ArrayList element denotes a table column and the ordering 
  //imposed by the ArrayList reflects the order of columns in the execution 
  //history database table itself.  The length of each array contained in each 
  //ArrayList element is 2: the first element is the column's name, the second 
  //element is the column's datatype.
  private final ArrayList<String[]> _executionHistoryTableColumnMetadata = new ArrayList<>();
  
  //Set-up a data structure to hold the values set for the columns in the last 
  //history row inserted.  The keys are column names, the values are the column 
  //values for the last row. This will be used to "fill-in" data for execution
  //history rows inserted that do not have all column values specified (values 
  //for the "TIME" column, for instance).
  private final HashMap<String, Object> _lastHistoryRowInserted = new HashMap<>();
  
  /*************************/
  /***** GUI variables *****/
  /*************************/
  
  private final int _nodeDrawingThreshold = 5000;
  
  // use to freeze/unfreeze updates to the model to prevent GUI
  // seizing up during training
  private boolean _frozen = false;
  
  /****************************************************************************/
  /****************************************************************************/
  /******************************** FUNCTIONS *********************************/
  /****************************************************************************/
  /****************************************************************************/

  /**
   * Constructor.
   * 
   * @param time 
   * @param domain 
   */
  public Chrest (int time, Class domain) {
    
    /*******************************/
    /**** Simple variable setup ****/
    /*******************************/
    
    //Set creation time and resource clocks.
    this._creationTime = time;
    
    //Set domain.
    if(domain == null){
      domain = GenericDomain.class;
    }
    
    try {
      this._domainSpecifics = (DomainSpecifics)domain.getConstructor(new Class[]{Chrest.class}).newInstance(this);
    } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
      Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    /******************************/
    /**** Clock variable setup ****/
    /******************************/
    
    //Clocks should be set to 1 less than the time of model creation so that
    //checks on resource availability will pass when first requested.
    this.setClocks(time - 1);
    
    /*************************************/
    /**** Architecture variable setup ****/
    /*************************************/
    
    //Setup long-term memory.
    _visualLtm = new Node (this, Modality.VISUAL, time);
    _verbalLtm = new Node (this, Modality.VERBAL, time);
    _actionLtm = new Node (this, Modality.ACTION, time);
    
    //Setup short-term memory
    _visualStm = new Stm (this, Modality.VISUAL, 4, time);
    _verbalStm = new Stm (this, Modality.VERBAL, 2, time);
    _actionStm = new Stm (this, Modality.ACTION, 4, time);
    
    //Setup remaining architecture variables.
    this._perceiver = new Perceiver (this, 2);
    this._visualSpatialFields.put(time, null);
    
    /*********************************************/
    /***** Execution history DB table set-up *****/
    /*********************************************/
    
    this._databaseInterface = new DatabaseInterface(null);
    
    //Set-up the execution history table column metadata. The first column will 
    //be specified as the primary key for the table when the table is created. 
    String[] idColumnNameAndType = {Chrest._executionHistoryTableRowIdColumnName, "INT"};
    String[] timeColumnNameAndType = {Chrest._executionHistoryTableTimeColumnName, "INT"};
    String[] operationColumnNameAndType = {Chrest._executionHistoryTableOperationColumnName, "VARCHAR(255)"};
    String[] inputColumnNameAndType = {Chrest._executionHistoryTableInputColumnName, "VARCHAR(255)"};
    String[] descriptionColumnNameAndType = {Chrest._executionHistoryTableDescriptionColumnName, "LONGVARCHAR"};
    String[] outputColumnNameAndType = {Chrest._executionHistoryTableOutputColumnName, "VARCHAR(255)"};
    
    this._executionHistoryTableColumnMetadata.add(idColumnNameAndType);
    this._executionHistoryTableColumnMetadata.add(timeColumnNameAndType);
    this._executionHistoryTableColumnMetadata.add(operationColumnNameAndType);
    this._executionHistoryTableColumnMetadata.add(inputColumnNameAndType);
    this._executionHistoryTableColumnMetadata.add(descriptionColumnNameAndType);
    this._executionHistoryTableColumnMetadata.add(outputColumnNameAndType);
    
    this.createExecutionHistoryTable();
    
    //Initialise total node counters to 0 for all modalities. 
    for(Modality modality : Modality.values()){
      String modalityString = modality.toString();
      modalityString = modalityString.substring(0, 1).toUpperCase() + modalityString.substring(1).toLowerCase();
      
      try {
        HistoryTreeMap modalityNodeCountVariable = (HistoryTreeMap)Chrest.class.getDeclaredField("_totalNumber" + modalityString + "LtmNodes").get(this);
        modalityNodeCountVariable.put(time, 0);
      } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
        Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
  
  /**
   * Typical execution cycle for agent:
   * 
   * 1. Is attention/cognition/perceiver free?
   *  1.1. Yes: execute some action that requires attention/cognition/perceiver.
   *  1.2. No: Go back to 1
   * 
   * @param methodName
   * @param parameters
   * @return 
   */
  public Object execute(String methodName, Object[] parameters){
    
    boolean validExecutableMethodName = false;
    for(int i = 0; i < _publiclyExecutableMethods.length; i++){
      if(_publiclyExecutableMethods[i].equals(methodName)){
        validExecutableMethodName = true;
      }
    }
    
    if(validExecutableMethodName){
      Class[] parameterTypes = new Class[parameters.length];
      for(int i = 0; i < parameters.length; i++){
        parameterTypes[i] = parameters[i].getClass();
      }

      try {
        return Chrest.class.getDeclaredMethod(methodName, parameterTypes).invoke(this, parameters);
      } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
        throw new RuntimeException(ex);
      }
    }
    else{
      throw new RuntimeException(
        "The method name specified (" + methodName + ") is not a publicly " +
        "executable method."
      );
    }
  }
  
  void printDebugStatement(String statement){
    if(this._debug) this._debugOutput.println(statement);
  }
    
  public void turnOnDebugging(){
    this._debug = true;
  }
  
  public void turnOffDebugging(){
    this._debug = false;
  }
  
  /**
   * Set to {@link java.lang.System#out} by default.
   * 
   * @param printStream 
   */
  public void setDebugPrintStream(PrintStream printStream){
    this._debugOutput = printStream;
  }
  
  /************************************/
  /**** SIMPLE GETTERS AND SETTERS ****/
  /************************************/
  
  public boolean canCreateSemanticLinks(){
    return _canCreateSemanticLinks;
  }
  
  public boolean canCreateTemplates(){
    return _canCreateTemplates;
  }
  
  public int getAddProductionTime(){
    return this._addProductionTime;
  }
  
  public int getAttentionClock(){
    return _attentionClock;
  }
  
  public int getCreationTime(){
    return this._creationTime;
  }
  
  public int getDiscriminationTime(){
    return _discriminationTime;
  }
  
  public DomainSpecifics getDomainSpecifics(){
    return _domainSpecifics;
  }
  
  public int getFamiliarisationTime(){
    return _familiarisationTime;
  }
  
  public int getLtmLinkTraversalTime(){
    return this._ltmLinkTraversalTime;
  }
  
  protected int getMinItemOrPositionOccurrencesToBeSlotValue(){
    return this._minItemOrPositionOccurrencesInNodeImagesToBeSlotValue;
  }
  
  protected int getMinNodeDepthInNetworkToBeTemplate(){
    return this._minNodeDepthInNetworkToBeTemplate;
  }
  
  int getNextLtmNodeReference(){
    return this._nextLtmNodeReference;
  }
  
  public int getNodeComparisonTime(){
    return this._nodeComparisonTime;
  }
  
  public float getNodeImageSimilarityThreshold() {
    return _nodeImageSimilarityThreshold;
  }
  
  public Perceiver getPerceiver () {
    return _perceiver;
  }
  
  public int getPerceiverClock(){
    return this._perceiverClock;
  }
  
  public int getReinforceProductionTime(){
    return _reinforceProductionTime;
  }
  
  public float getRho(){
    return _rho;
  }
  
  public int getTimeToCreateNamingLink(){
    return this._namingLinkCreationTime;
  }
  
  public int getTimeToCreateSemanticLink(){
    return this._semanticLinkCreationTime;
  }
  
  public int getTimeToRetrieveItemFromStm(){
    return this._timeToRetrieveItemFromStm;
  }
  
  void incrementNextNodeReference(){
    this._nextLtmNodeReference++;
  }
  
  public boolean attentionFree(int time){
    return this._attentionClock <= time;
  }
    
  public boolean cognitionFree(int time){
    return this._cognitionClock <= time;
  }
  
  public boolean perceiverFree(int time){
    return this._perceiverClock <= time;
  }
  
  public void setDomain (DomainSpecifics domain) {
    _domainSpecifics = domain;
  }
  
  public void setAddProductionTime (int time) {
    this._addProductionTime = time;
  }
  
  public void setDiscriminationTime (int time) {
    _discriminationTime = time;
  }
  
  public void setFamiliarisationTime (int time) {
    _familiarisationTime = time;
  }
  
  public void setReinforceProductionTime(int time){
    this._reinforceProductionTime = time;
  }

  public void setRho (float rho) {
    _rho = rho;
  }
  
  public void setNodeImageSimilarityThreshold (int threshold) {
    _nodeImageSimilarityThreshold = threshold;
  }

  public void setCreateSemanticLinks (boolean value) {
    _canCreateSemanticLinks = value;
  }

  public void setCreateTemplates (boolean value) {
    _canCreateTemplates = value;
  }

  public void setLtmLinkTraversalTime(int ltmLinkTraversalTime) {
    this._ltmLinkTraversalTime = ltmLinkTraversalTime;
  }
  
  public void setNodeComparisonTime(int nodeComparisonTime){
    this._nodeComparisonTime = nodeComparisonTime;
  }
  
  /**
   * Default is 2.
   * 
   * @param maximumSemanticLinkSearchDistance The number of semantic links that
   * can be followed from a {@link jchrest.architecture.Node} reached after 
   * sorting a {@link jchrest.lib.ListPattern} vertically through long-term 
   * memory.  For example, if 3 {@link jchrest.architecture.Node}s are 
   * semantically linked to as 1 -> 2 -> 3 and {@link jchrest.architecture.Node} 
   * 1 is retrieved after long-term memory sorting and the maximum semantic link 
   * search distance parameter is set to 1, {@link jchrest.architecture.Node} 2 
   * would be retrieved.
   */
  public void setMaximumSemanticLinkSearchDistance(int maximumSemanticLinkSearchDistance){
    this._maximumSemanticLinkSearchDistance = maximumSemanticLinkSearchDistance;
  }
  
  public void setTemplateConstructionParameters (int minNodeDepthInNetworkToBeTemplate, int minItemOrPositionOccurrencesInNodeImagesToBeSlotValue) {
    if(minNodeDepthInNetworkToBeTemplate >= 1 && minItemOrPositionOccurrencesInNodeImagesToBeSlotValue >= 1){
      this._minNodeDepthInNetworkToBeTemplate = minNodeDepthInNetworkToBeTemplate;
      this._minItemOrPositionOccurrencesInNodeImagesToBeSlotValue = minItemOrPositionOccurrencesInNodeImagesToBeSlotValue;
    }
    else{
      throw new RuntimeException("Template construction parameters not valid, " +
        "should both be >= 1 (min. depth specified = " + minNodeDepthInNetworkToBeTemplate + ", min. " +
        "occurrences specified = " + minItemOrPositionOccurrencesInNodeImagesToBeSlotValue + ")");
    }
  }
  
  public void setTimeToCreateNamingLink(int timeToCreateNamingLink) {
    this._namingLinkCreationTime = timeToCreateNamingLink;
  }

  public void setTimeToCreateSemanticLink(int timeToCreateSemanticLink) {
    this._semanticLinkCreationTime = timeToCreateSemanticLink;
  }
  
  public void setTimeToUpdateStm(int timeToUpdateStm){
    this._timeToUpdateStm = timeToUpdateStm;
  }
  
  public void setTimeToRetrieveItemFromStm(int timeToRetrieveItemFromStm){
    this._timeToRetrieveItemFromStm = timeToRetrieveItemFromStm;
  }

  /**************************************/
  /**** ADVANCED GETTERS AND SETTERS ****/
  /**************************************/
  
  /**
   * @param modality
   * @return The root {@link jchrest.architecture.Node} of the long-term memory 
   * {@link jchrest.lib.Modality} specified.
   */
  public Node getLtmModalityRootNode(Modality modality){
    Node result = null;
    
    String fieldNameIntermediate = modality.toString().toLowerCase();
    for(Field field : Chrest.class.getDeclaredFields()){
      if(field.getName().equals("_" + fieldNameIntermediate + "Ltm")){
        try {
          Object value = field.get(this);
          if(value instanceof Node){
            result = (Node)value;
          }
        } catch (IllegalArgumentException | IllegalAccessException ex) {
          Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
    
    return result;
  }
  
  /**
   * @param pattern
   * @return The root {@link jchrest.architecture.Node} of a long-term memory 
   * {@link jchrest.lib.Modality} specified using the {@link 
   * jchrest.lib.ListPattern} passed.
   */
  public Node getLtmModalityRootNode (ListPattern pattern) {
    return this.getLtmModalityRootNode(pattern.getModality());
  }

  /** 
   * @param modality 
   * @param time 
   *
   * @return A count of the number of {@link jchrest.architecture.Node}s in the 
   * long-term memory {@link jchrest.lib.Modality} specified at the time 
   * requested.  If this {@link #this} model was not created at the time 
   * specified, null is returned.
   */
  public Integer getLtmModalitySize (Modality modality, int time) {
    if(this._creationTime <= time){
      
      String modalityString = modality.toString();
      modalityString = modalityString.substring(0, 1).toUpperCase() + modalityString.substring(1).toLowerCase();
      
      try {
        HistoryTreeMap modalityNodeCountVariable = (HistoryTreeMap)Chrest.class.getDeclaredField("_totalNumber" + modalityString + "LtmNodes").get(this);
        Entry<Integer, Object> entry= modalityNodeCountVariable.floorEntry(time);
        if(entry != null){
          return (Integer)entry.getValue();
        }
      } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
        Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    
    return null;
  }
  
  /**
   * Gets the total number of {@link jchrest.architecture.Node}s contained in 
   * the long-term memory of {@link #this}, irrespective of {@link 
   * jchrest.lib.Modality}, at the time specified.
   * 
   * @param time
   * @return 
   */
  public Integer getLtmSize(int time){
    int size = 0;
    
    for(Modality modality : Modality.values()){
      size += this.getLtmModalitySize(modality, time);
    }
    
    return size;
  }
  
  /**
   * @param modality
   * @return The {@link jchrest.architecture.Stm} associated with this {@link 
   * #this} model with the {@link jchrest.lib.Modality} specified.
   */
  public Stm getStm (Modality modality) {
    try {
      Field stmField = Chrest.class.getDeclaredField("_" + modality.toString().toLowerCase() + "Stm");
      stmField.setAccessible(true);
      return (Stm)stmField.get(this);
    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
      Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    return null;
  }
  
  void incrementLtmModalityNodeCount(Modality modality, int time){
    try {
      String modalityString = modality.toString();
      modalityString = modalityString.substring(0, 1).toUpperCase() + modalityString.substring(1).toLowerCase();
      
      HistoryTreeMap modalityNodeCountVariable = (HistoryTreeMap)Chrest.class.getDeclaredField("_totalNumber" + modalityString + "LtmNodes").get(this);
      Entry<Integer, Object> entry= modalityNodeCountVariable.floorEntry(time);
      if(entry != null){
        Integer currentCount = (Integer)entry.getValue();
        modalityNodeCountVariable.put(time, currentCount + 1);
      }
    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
      Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  /**
   * @param time
   * @return {@link java.lang.Boolean} if this {@link #this} model has more than
   * 2000 nodes (taken from de Groot and Gobet's book "Perception 
   * and Memory in Chess" to indicate the point at which master-level eye 
   * heuristics are used instead of novice ones) in the entirety of its LTM at 
   * the time specified.  If this {@link #this} model was not created at the 
   * time specified, null is returned.
   */
  public Boolean isExperienced (int time) {
    if(this._creationTime <= time){
      return this.getLtmSize(time) > 2000;
    }
    return null;
  }
  
  /**
   * @param modality 
   * @param time
   * 
   * @return The average depth of the long-term memory {@link 
   * jchrest.lib.Modality} specified at the time passed.  If this {@link #this}
   * model was not created at the time specified, null is returned.
   */
  public Double getLtmAverageDepth (Modality modality, int time) {
    if(this._creationTime <= time){
      return this.averageDepthBelowNode(this.getLtmModalityRootNode(modality), time);
    }
    
    return null;
  }
  
  /**
   * @param node
   * @param time
   * @return The average depth below the {@link jchrest.architecture.Node} 
   * passed at the time specified.
   */
  public Double averageDepthBelowNode(Node node, int time) {
    if(this._creationTime <= time){
      List<Integer> depths = new ArrayList ();

      // -- find every depth
      for (Link link : node.getChildren(time)) {
        this.findDepth(link.getChildNode(), 1, depths, time);
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
    
    return null;
  }
  
  /**
   * If this {@link #this} is a child node, then add its depth to depths 
   * otherwise, continue searching through children for the depth.
   */
  private void findDepth (Node node, int currentDepth, List<Integer> depths, int time) {
    List<Link> children = node.getChildren(time);
    
    if (children.isEmpty ()) {
      depths.add (currentDepth);
    } else {
      for (Link link : children) {
        this.findDepth (link.getChildNode(), currentDepth + 1, depths, time);
      }
    }
  }
  
  /**
   * Attempts to add a new {@link jchrest.architecture.Node} to the modality 
   * root {@link jchrest.architecture.Node} identified using the modality of the
   * {@link jchrest.lib.ListPattern} passed at the time specified.  
   * 
   * The new {@link jchrest.architecture.Node}'s image will be empty but its 
   * contents are set to the {@link jchrest.lib.ListPattern} passed.
   * 
   * @param pattern Assumed to contain one "finished" {@link 
   * jchrest.lib.PrimitivePattern}.
   * @param time The time the primitive should be added to LTM.
   * @return An {@link java.util.ArrayList} whose first element contains a
   * {@link jchrest.architecture.Node} and whose second element contains a 
   * {@link java.lang.Boolean} value indicating if a primitive was learned.  
   * Possible values for these elements and the conditions that produce them are 
   * as follows:
   * <ul>
   *  <li>
   *    This {@link #this} doesn't exist at the time specified.
   *    <ul>
   *      <li>Element 1: null</li>
   *      <li>Element 2: {@link java.lang.Boolean#FALSE}</li>
   *    </ul>
   *  </li>
   *  <li>
   *    This {@link #this} does exist at the time specified.
   *    <ul>
   *      <li>
   *        See return values for {@link jchrest.architecture.Node#addChild(
   *        jchrest.lib.ListPattern, jchrest.architecture.Node, int, 
   *        java.lang.String)}. 
   *      </li>
   *    </ul>
   *  </li>
   * </ul>
   */
  private boolean learnPrimitive (ListPattern pattern, int time) {
    assert(pattern.isFinished () && pattern.size () == 1);
    String func = "- learnPrimitive: ";
    
    this.printDebugStatement(
      func + "Attemtping to learn " + pattern.toString() + " as a primitive " + 
      "at time " + time + ". Checking if model exists at this time"
    );
    
    if(this.getCreationTime() <= time){
    
      ListPattern contents = pattern.clone ();
      contents.setNotFinished ();
      
      Node child = new Node (
        this, 
        contents, 
        new ListPattern (pattern.getModality ()), 
        time
      );
      
      this.printDebugStatement(
        func + "Model exists at time specified, appending new child node with ref: " + 
        child.getReference() + " and image '" + child.getImage(time) + "' to the " + 
        pattern.getModalityString() + " root node by a link containing test: " + 
        contents.toString() + "."
      );

      return this.getLtmModalityRootNode(pattern).addChild(
        contents, 
        child, 
        time, 
        this.getCurrentExperimentName()
      );
    }
    else{
      this.printDebugStatement(func + "Model does not exist at this time, returning false.");
      this.printDebugStatement(func + "RETURN");
    }
    
    return false;
  }
  
  /**
   * @param time
   * @return A count of the number of {@link jchrest.architecture.Node}s in 
   * visual LTM that are templates at the time specified.
   */
  public int countTemplatesInVisualLtm(int time) {
    return this.countTemplatesBelowNode(this.getLtmModalityRootNode(Modality.VISUAL), 0, time);
  }
  
  /**
   * @param node
   * @param time
   * @return The number of template {@link jchrest.architecture.Node}s below the
   * {@link jchrest.architecture.Node} passed at the time specified.
   */
  private int countTemplatesBelowNode (Node node, int count, int time) {
    Boolean nodeIsTemplate = node.isTemplate (time);
    if (nodeIsTemplate) count += 1;

    for (Link link : node.getChildren(time)) {
      count += this.countTemplatesBelowNode(link.getChildNode(), count, time);
    }

    return count;
  }
  
  /**
   * @param time
   * @return A map of content sizes to frequencies for this {@link #this} 
   * model's LTM at the time specified.
   */ 
  public Map<Integer, Integer> getContentSizeCounts(int time) {
    Map<Integer, Integer> size = new HashMap();

    for(Modality modality : Modality.values()){
      this.getContentSizeCounts(this.getLtmModalityRootNode(modality), size, time);
    }

    return size;
  }
  
  /**
   * Add a map of content sizes to node counts for this {@link #this} and its 
   * children.
   * 
   * @param node
   * @param contentSizeCountsAndFrequencies
   * @param time
   */
  protected void getContentSizeCounts (Node node, Map<Integer, Integer> contentSizeCountsAndFrequencies, int time) {
    int contentsSize = node.getContents().size ();
    
    if (contentSizeCountsAndFrequencies.containsKey (contentsSize)) {
      contentSizeCountsAndFrequencies.put (contentsSize, contentSizeCountsAndFrequencies.get(contentsSize) + 1);
    } else {
      contentSizeCountsAndFrequencies.put (contentsSize, 1);
    }

    for (Link child : node.getChildren(time)) {
      this.getContentSizeCounts(child.getChildNode(), contentSizeCountsAndFrequencies, time);
    }
  }

  /**
   * @param time 
   * @return A map of image sizes to frequencies across the entirety of this 
   * {@link #this} model's LTM at the time specified.
   */ 
  public Map<Integer, Integer> getImageSizeCounts(int time) {
    Map<Integer, Integer> sizesToFrequencies = new HashMap();

    for(Modality modality : Modality.values()){
      this.getImageSizeCounts(this.getLtmModalityRootNode(modality), sizesToFrequencies, time);
    }

    return sizesToFrequencies;
  }
  
  /**
   * Populates the {@link java.util.Map} passed with how many {@link 
   * jchrest.architecture.Node}s have an image of a particular size at the time
   * specified.
   * 
   * @param node The {@link jchrest.architecture.Node} to take counts from.
   * @param sizesToFrequencies Should be empty when invoking this function.
   * @param time
   */
  public void getImageSizeCounts (Node node, Map<Integer, Integer> sizesToFrequencies, int time) {
    int size = node.getImage(time).size();
    
    if (sizesToFrequencies.containsKey (size)) {
      sizesToFrequencies.put (size, sizesToFrequencies.get(size) + 1);
    } else {
      sizesToFrequencies.put (size, 1);
    }

    for (Link child : node.getChildren(time)) {
      this.getImageSizeCounts(child.getChildNode(), sizesToFrequencies, time);
    }
  }
  
  /**
   * @return The sum of image sizes of the child {@link 
   * jchrest.architecture.Node}s and their child's {@link 
   * jchrest.architecture.Node}s etc. below the {@link 
   * jchrest.architecture.Node} specified at the time specified.
   */
  private int totalImageSize (Node node, int time) {
    int size = node.getImage(time).size();
    
    for (Link link : node.getChildren(time)) {
      size += this.totalImageSize(link.getChildNode(), time);
    }

    return size;
  }
  
  /**
   * @param node
   * @param time
   * @return The average image size of the child {@link 
   * jchrest.architecture.Node}s and their child's {@link 
   * jchrest.architecture.Node}s etc. below the {@link 
   * jchrest.architecture.Node} specified at the time specified.
   */
  public double averageImageSize (Node node, int time) {
    return (double)this.totalImageSize(node, time) / node.size(time);
  }
  
  /**
   * @param time
   * @return The total number of productions for this {@link #this} model's
   * {@link jchrest.lib.Modality#VISUAL} LTM at the time specified.
   */
  public int getProductionCount(int time){
    return this.getProductionCount(_visualLtm, true, time);
  }
  
  /**
   * @param node The {@link jchrest.architecture.Node} to count from.
   * @param recurse Set to {@link java.lang.Boolean#TRUE} to apply function 
   * recursively, returning the number of productions in the {@link 
   * jchrest.architecture.Node}'s children, its children's children etc. at the 
   * time specified.  Set to {@link java.lang.Boolean#FALSE} to just return the 
   * number of productions in the {@link jchrest.architecture.Node} specified at 
   * the time specified.
   * @param time
   * 
   * @return See parameter documentation.
   */
  protected int getProductionCount(Node node, boolean recurse, int time){
    int count = node.getProductions(time).size();
    
    if(recurse){
      for(Link link : node.getChildren(time)){
        count += this.getProductionCount(link.getChildNode(), true, time);
      }
    }
    
    return count;
  }
  
  /**
   * @param time
   * @return A map of the number of semantic links to frequencies for this 
   * {@link #this} model's LTM at the time specified.
   */ 
  public Map<Integer, Integer> getSemanticLinkCounts(int time) {
    Map<Integer, Integer> semanticLinkCountsAndFrequencies = new HashMap();

    for(Modality modality : Modality.values()){
      this.getSemanticLinkCounts(this.getLtmModalityRootNode(modality), semanticLinkCountsAndFrequencies, time);
    }

    return semanticLinkCountsAndFrequencies;
  }
  
  /**
   * Add to a map from number of semantic links to frequency, for this {@link 
   * #this} and its children.
   * 
   * @param node The {@link to start counting frequencies from.
   * @param semanticLinkCountsAndFrequencies 
   * @param time
   */
  public void getSemanticLinkCounts (Node node, Map<Integer, Integer> semanticLinkCountsAndFrequencies, int time) {
    int semanticLinkCount = node.getSemanticLinks(time).size ();
    
    if (semanticLinkCount > 0) { // do not count nodes with no semantic links
      if (semanticLinkCountsAndFrequencies.containsKey (semanticLinkCount)) {
        semanticLinkCountsAndFrequencies.put (semanticLinkCount, semanticLinkCountsAndFrequencies.get(semanticLinkCount) + 1);
      } else {
        semanticLinkCountsAndFrequencies.put (semanticLinkCount, 1);
      }
    }

    for (Link child : node.getChildren(time)) {
      this.getSemanticLinkCounts(child.getChildNode(), semanticLinkCountsAndFrequencies, time);
    }
  }
  
  /*************************************/
  /**** EXECUTION HISTORY FUNCTIONS ****/
  /*************************************/
  
  /**
   * Returns the metadata stored in the relevant data structure in this model
   * instance for the execution history table.  Note that this function doesn't
   * actually query the execution history database table for this metadata.
   * 
   * @return First element of each array in each ArrayList element contains the 
   * column's name and its datatype, in that order.
   */
  public ArrayList<String[]> getExecutionHistoryTableColumnMetadata(){
    return this._executionHistoryTableColumnMetadata;
  }
  
  /**
   * Returns all operations present in the execution history database table.
   */
  public void getExecutionHistoryOperations(){
    String sql = "SELECT " + Chrest._executionHistoryTableOperationColumnName + " FROM " + Chrest._executionHistoryTableName;
    try {
      this._databaseInterface.executeSqliteQuery(sql, null);
    } catch (SQLException ex) {
      Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  /**
   * Creates a new execution history table in this model's database connection.
   */
  private void createExecutionHistoryTable(){
            
    String createTableSqlStatement = "CREATE TABLE " + Chrest._executionHistoryTableName + " (";
    
    //Since the CHREST GUI is multi-threaded it may, in some cases, try to add 
    //duplicate rows to the execution history table.  To prevent this, a unique
    //key index is set with all columns except the primary key column (since 
    //this will always be different due to auto-increment functionality) forming
    //the index.
    String createUniqueRowIndex = "CREATE UNIQUE INDEX " + Chrest._executionHistoryUniqueRowIndexName + ""
      + " ON " + Chrest._executionHistoryTableName + " (";

    //Specify column names and metadata for SQL queries that have partly been
    //constructed above.
    for(int col = 0; col < Chrest.this._executionHistoryTableColumnMetadata.size(); col++){
      String[] colNameAndType = Chrest.this._executionHistoryTableColumnMetadata.get(col);
      String colName = colNameAndType[0];
      createTableSqlStatement += colName + " ";
      
      if(col == 0){
        createTableSqlStatement += colNameAndType[1] + " NOT NULL AUTO_INCREMENT PRIMARY KEY, ";
      }
      else{
        createTableSqlStatement += colNameAndType[1] + ", ";
        createUniqueRowIndex += colName + ", ";
      }
    }
    createTableSqlStatement = createTableSqlStatement.replaceFirst(", $", ");");
    createUniqueRowIndex = createUniqueRowIndex.replaceFirst(", $", ");");
    
    try {
      this._databaseInterface.executeSqliteQuery(createTableSqlStatement, null);
      this._databaseInterface.executeSqliteQuery(createUniqueRowIndex, null);
    } catch (SQLException ex) {
      Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  /**
   * Adds an episode to the execution history table of this model's database 
   * connection.  This function performs the following operations:
   * 
   * <ol>
   *  <li>
   *    If a value for the "TIME" column has not been specified, its value for 
   *    this row is set to the value of the "TIME" column in the last row 
   *    inserted.
   *  </li>
   *  <li>
   *    Creates and executes the SQLite statement that performs the insert. 
   *  </li>
   *  <li>
   *    Updates the last inserted row structure for this model instance.
   *  </li>
   * </ol>
   * 
   * @param columnNamesAndValues The column names and values that should be 
   * added to this model's execution history.  Any columns whose values have not 
   * been specified will have blank values inserted automatically.  To ensure 
   * consistency of operation, specify column names (keys) using this class' 
   * relevant class members (static variables whose names start with 
   * "_historyTable" and end with "ColumnName").
   */
  public void addEpisodeToExecutionHistory(HashMap<String, Object> columnNamesAndValues) {
    
    if(this.canRecordHistory()){

      /*********************************/
      /***** Set time column value *****/
      /*********************************/
      if(columnNamesAndValues.get(Chrest._executionHistoryTableTimeColumnName) == null){
        int time = 0;
        if(!Chrest.this._lastHistoryRowInserted.isEmpty()){
          time = (int)Chrest.this._lastHistoryRowInserted.get(Chrest._executionHistoryTableTimeColumnName);
        }
        columnNamesAndValues.put(Chrest._executionHistoryTableTimeColumnName, time);
      }

      /*************************************/
      /***** Create insert new row SQL *****/
      /*************************************/
      String insertSqlStatementString = "INSERT INTO " + Chrest._executionHistoryTableName + " (";

      //Primary key column will be auto-incremented so specify columns
      //to insert into from the column after the primary key column.
      for(int col = 1; col < Chrest.this._executionHistoryTableColumnMetadata.size(); col++){
        Object[] colNameAndTypeIterator = Chrest.this._executionHistoryTableColumnMetadata.get(col);
        insertSqlStatementString += (String)colNameAndTypeIterator[0] + ", ";
      }
      insertSqlStatementString = insertSqlStatementString.replaceFirst(", $", ") VALUES (");
      
      for(int colIndex = 1; colIndex < Chrest.this._executionHistoryTableColumnMetadata.size(); colIndex++){
        insertSqlStatementString += "?, ";
      }
      insertSqlStatementString = insertSqlStatementString.replaceFirst(", $", ")");

      /*****************************************/
      /***** Create SQL statement bindings *****/
      /*****************************************/

      //Initialise the bindings array.  Note that there are n bindings created
      //where n = the number of history table columns - 1.  This is because the
      //ID column should be auto-incremented so a value is never bound to this
      //column.
      ArrayList<Object> bindings = new ArrayList();
      for(int i = 0; i < this._executionHistoryTableColumnMetadata.size() - 1; i++){
        bindings.add(null);
      }

      //Set bindings for declared columns in the correct order.
      for(Entry<String, Object> columnNameAndValueToInsert : columnNamesAndValues.entrySet()){
        String nameOfColumnToInsertValueInto = columnNameAndValueToInsert.getKey();

        //Skip over the primary key column.
        for(int col = 1; col < Chrest.this._executionHistoryTableColumnMetadata.size(); col++){
          String[] columnNameAndType = Chrest.this._executionHistoryTableColumnMetadata.get(col);
          if(nameOfColumnToInsertValueInto.equals( columnNameAndType[0] )){
            bindings.set(col - 1, columnNameAndValueToInsert.getValue());
          }
        }
      }

      //Fill in any missing column values with empties.
      for(int colIndex = 0; colIndex < bindings.size(); colIndex++){
        if(bindings.get(colIndex) == null){
          bindings.set(colIndex, "");
        }
      } 

      /**************************/
      /***** Execute insert *****/
      /**************************/
      try {
        this._databaseInterface.executeSqliteQuery(insertSqlStatementString, bindings);
      } catch (SQLException ex) {
        Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
      }

      /***************************************************/
      /***** Update last row inserted data structure *****/
      /***************************************************/
      this.updateLastExecutionHistoryRowInserted();
    }
  }
  
  /**
   * Populates the data structure that stores column values for the last row
   * inserted into the model's execution history table with relevant data.
   */
  private void updateLastExecutionHistoryRowInserted(){
    if(this.canRecordHistory()){
      String getLastRowInsertedSql = "SELECT * FROM " + Chrest._executionHistoryTableName + " WHERE " + Chrest._executionHistoryTableRowIdColumnName + " = "
        + "(SELECT MAX(" + Chrest._executionHistoryTableRowIdColumnName + ") FROM " + Chrest._executionHistoryTableName + ")";

      try {
        ArrayList<ArrayList<Object[]>> lastRowInserted = this._databaseInterface.executeSqliteQuery(getLastRowInsertedSql, null);

        if(lastRowInserted != null){
          for(ArrayList<Object[]> rowData : lastRowInserted){
            for(int col = 0; col < rowData.size(); col++){
              Object[] colData = rowData.get(col);
              Chrest.this._lastHistoryRowInserted.put((String)colData[0], colData[1]);
            }
          }

        }
      } catch (SQLException ex) {
        Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
  
  /**
   * Enables a user to switch history recording on/off for this model.
   * 
   * @param value True to turn on history recording, false to turn off
   */
  public void setRecordHistory(boolean value){
    this._executionHistoryRecordingEnabled = value;
  }
  
  /**
   * Indicates whether this model can currently record its execution history.
   * 
   * @return Boolean true if yes, boolean false if not.
   */
  public boolean canRecordHistory(){
    return this._executionHistoryRecordingEnabled;
  }
  
  /**
   * Retrieves entire execution history with no filters.
   * 
   * @return See return value for {@link jchrest.database.DatabaseInterface#executeSqliteQuery(java.lang.String, java.util.ArrayList)}.
   */
  public ArrayList<ArrayList<Object[]>> getExecutionHistory(){
    ArrayList<ArrayList<Object[]>> executionHistory = null;
    String sql = "SELECT * FROM " + Chrest._executionHistoryTableName;
    
    try {
      executionHistory = this._databaseInterface.executeSqliteQuery(sql, null);
    } catch (SQLException ex) {
      Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    return executionHistory;
  }
  
  /**
   * Retrieves the model's execution history from the time specified to the time 
   * specified.
   * 
   * @param from Domain-time to return model's execution history from.
   * @param to Domain-time to return model's execution history to.
   * 
   * @return See return value for {@link jchrest.database.DatabaseInterface#executeSqliteQuery(java.lang.String, java.util.ArrayList)}.
   */
  public ArrayList<ArrayList<Object[]>> getHistory(int from, int to){
    ArrayList<ArrayList<Object[]>> executionHistory = null;
    
    String sql = "SELECT * FROM " + Chrest._executionHistoryTableName + " WHERE time >= ? AND time <= ?";
    ArrayList bindings = new ArrayList();
    bindings.add(from);
    bindings.add(to);
    
    try {
      executionHistory = this._databaseInterface.executeSqliteQuery(sql, bindings);
    } catch (SQLException ex) {
      Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    return executionHistory;
  }
  
  /**
   * Returns the model's execution history filtered by the operation, from time 
   * and to time specified.
   * 
   * @param operation The operation to filter execution history by.
   * @param from Domain-time to return model's execution history from.
   * @param to Domain-time to return model's execution history to.
   * 
   * @return See return value for {@link jchrest.database.DatabaseInterface#executeSqliteQuery(java.lang.String, java.util.ArrayList)}.
   */
  public ArrayList<ArrayList<Object[]>> getHistory(String operation, int from, int to) {
    ArrayList<ArrayList<Object[]>> executionHistory = null;
    
    String sql = "SELECT * FROM " + Chrest._executionHistoryTableName + " WHERE operation = ? AND time >= ? AND time <= ?;";
    ArrayList bindings = new ArrayList();
    bindings.add(operation);
    bindings.add(from);
    bindings.add(to);
    try {
      executionHistory = this._databaseInterface.executeSqliteQuery(sql, bindings);
    } catch (SQLException ex) {
      Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    return executionHistory;
  }
  
  /**
   * Clears the model's current execution history.
   */
  public void clearHistory() {
    String sql = "TRUNCATE TABLE " + Chrest._executionHistoryTableName;
    try {
      this._databaseInterface.executeSqliteQuery(sql, null);
    } catch (SQLException ex) {
      Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  /**
   * @param experiment
   * @return The maximum time set for the specified experiment, if one is set.  
   * If not, this {@link #this} model's clock values are compared and the 
   * greatest clock value is returned.
   */
  public Integer getMaximumTimeForExperiment(String experiment){
    Integer maxTime = this._experimentNamesAndMaximumTimes.get(experiment);
    
    if(maxTime == null){
      maxTime = this.getMaximumClockValue();
    }

    return maxTime;
  }
  
  /**
   * Sets the maximum time for an experiment to the time passed if the model is
   * currently located in an experiment.
   * 
   * @param time
   */
  public void setMaxmimumTimeInExperiment(int time){
    if (!this.getCurrentExperimentName().isEmpty()) this._experimentNamesAndMaximumTimes.put(this.getCurrentExperimentName(), time);
  }
  
  /**
   * Accessor for the text prepended to experiment names.
   * 
   * @return 
   */
  public static String getPreExperimentPrepend(){
    return Chrest._preExperimentPrepend;
  }
  
  /**
   * Adds an experiment name to the list of experiments this model has been
   * located in so far since its creation/last time it was cleared.  The 
   * experiment name will have a repeat number appended to it to differentiate
   * it from previous runs with this experiment.
   * 
   * @param experimentName
   */
  public void addExperimentsLocatedInName(String experimentName){
    int repeatNumber = 1;
    while(this._experimentsLocatedInNames.contains(experimentName + "-" + repeatNumber)){
      repeatNumber++;
    }
    this._experimentsLocatedInNames.add(experimentName + "-" + repeatNumber);
    setChanged();
    notifyObservers();
  }
  
  public Experiment getCurrentExperiment(){
    return this._currentExperiment;
  }
  
  public void setCurrentExperiment(Experiment experiment){
    this._currentExperiment = experiment;
  }
  
  /**
   * Returns all experiment names that the model has been located in so far 
   * since its creation/last time it was cleared.
   * 
   * @return 
   */
  public List<String> getExperimentsLocatedInNames(){
    return this._experimentsLocatedInNames;
  }
  
  public String getCurrentExperimentName(){
    return this._experimentsLocatedInNames.isEmpty() ? "" : this._experimentsLocatedInNames.get(this._experimentsLocatedInNames.size() - 1);
  }
  
  /**
   * Accessor for "_loadedIntoExperiment" instance variable.
   * 
   * @return
   */
  public boolean loadedIntoExperiment(){
    return this._loadedIntoExperiment;
  }
  
  /**
   * Updates model's state so that it now considers itself loaded in an 
   * experiment but hasn't acted within the experiment yet.
   */
  public void setLoadedIntoExperiment(){
    this._loadedIntoExperiment = true;
  }
  
  /**
   * Updates model's state so that it now considers itself not loaded in an 
   * experiment.
   */
  public void setNotLoadedIntoExperiment(){
    this._loadedIntoExperiment = false;
  }
  
  /**
   * Accessor for "_engagedInExperiment" instance variable.
   * 
   * @return
   */
  public boolean engagedInExperiment(){
    return this._engagedInExperiment;
  }
  
  /**
   * If the model is loaded into an experiment the model's state will be updated 
   * so that it now considers itself engaged in an experiment.  
   */
  public void setEngagedInExperiment(){
    if(this.loadedIntoExperiment()){
      this._engagedInExperiment = true;
      this.setChanged();
      this.notifyObservers();
    }
  }
  
  /**
   * Updates model's state so that it now considers itself not engaged in an 
   * experiment.
   */
  public void setNotEngagedInExperiment(){
    this._engagedInExperiment = false;
    this.setChanged();
    this.notifyObservers();
  }
  
  /**
   * Determines if the model will allow Node history updates or LTM drawing
   * given the current size of LTM.
   * 
   * @return 
   */
  public boolean canDrawLtmState(int time){
    int ltmSize = 0;
    
    for(Modality modality : Modality.values()){
      ltmSize += this.getLtmModalitySize(modality, time);
    }
    
    return ltmSize < this._nodeDrawingThreshold;
  }
  
  /***********************/
  /**** GUI functions ****/
  /***********************/
  
  /**
   * Instruct model not to update observers.
   */
  public void freeze () {
    _frozen = true;
  }

  /**
   * Instruct model to now update observers for future changes.
   * Also triggers an immediate update of current observers.
   */
  public void unfreeze () {
    _frozen = false;
    setChanged ();
    notifyObservers ();
  }
  
  /****************************************************************************/
  /****************************************************************************/
  /***************************** TIMED FUNCTIONS ******************************/
  /****************************************************************************/
  /****************************************************************************/
  
  //TODO: Write tests to check functions in this section and check for correct
  //      operation.
  
  /**************************/
  /**** Long-term memory ****/
  /**************************/
  
  /** 
   * @param pattern
   * @param time
   * @return The image of the {@link jchrest.architecture.Node} associated with
   * the {@link jchrest.architecture.Node} recognised after sorting the 
   * {@link jchrest.lib.ListPattern} specified through the long-term memory of 
   * {@link #this} at the time specified.  If cognition is busy at the time
   * specified or there is no {@link jchrest.architecture.Node} associated with
   * the {@link jchrest.architecture.Node} recognised then null is returned.
   */
  public ListPattern getAssociatedPattern (ListPattern pattern, int time) {
    Node recognisedNode = recognise(pattern, time, true);
    
    if(recognisedNode != null){
      
      //Cognition must be free at this point otherwise the recognised node would
      //be null so try to get the node associated with the one recognised.  If 
      //such a node exists, set the cognition clock to the time its set to after 
      //recognition plus the time taken to traverse a long term memory link 
      //(time to get to the associated node from the recognised node).
      Node associatedNode = recognisedNode.getAssociatedNode(time);
      if (associatedNode != null) {
        time += this._ltmLinkTraversalTime;
        this._cognitionClock = time;
        return associatedNode.getImage(time);
      }
    }
    
    return null;
  }
  
  /**
   * @param pattern
   * @param time
   * @return The image of the {@link jchrest.architecture.Node} that names the
   * {@link jchrest.architecture.Node} recognised by sorting the {@link 
   * jchrest.lib.ListPattern} specified through the long-term memory of {@link 
   * #this} at the time specified.  If cognition is busy at the time specified 
   * or there is no {@link jchrest.architecture.Node} that names the {@link 
   * jchrest.architecture.Node} recognised then null is returned.
   */
  public ListPattern getNamedBy (ListPattern pattern, int time) {
    Node recognisedNode = recognise(pattern, time, true);
    
    if(recognisedNode != null){
      
      //Cognition must be free at this point otherwise the recognised node would
      //be null so try to get the node that names the one recognised.  If such
      //a node exists, set the cognition clock to the time its set to after 
      //recognition plus the time taken to traverse a long term memory link 
      //(time to get to the naming node from the recognised node).
      Node namedBy = recognisedNode.getNamedBy(time);
      if (namedBy != null) {
        time += this._ltmLinkTraversalTime;
        this._cognitionClock = time;
        return namedBy.getImage(time);
      }
    }
    
    return null;
  }
  
  /**
   * Learns the two {@link jchrest.lib.ListPattern}s provided completely 
   * (ensures that the {@link jchrest.lib.ListPattern} is an image in a 
   * long-term memory {@link jchrest.architecture.Node}) before associating the 
   * respective long-term memory {@link jchrest.architecture.Node}s.
   * 
   * The association created is determined by the {@link jchrest.lib.Modality}
   * of the {@link jchrest.lib.ListPattern}s provided:
   * 
   * <ul>
   *  <li>
   *    If pattern1 and pattern2 have the same {@link jchrest.lib.Modality}, a
   *    semantic link is created between them.
   *  </li>
   *  <li>
   *    If pattern2 has {@link jchrest.lib.Modality#ACTION} and pattern1 does
   *    not, a production is created between them. 
   *  </li>
   * </ul>
   * 
   * @param pattern1
   * @param pattern2
   * @param time
   * 
   * @return The {@link jchrest.architecture.Node} recognised for pattern1.
   */
//  private ExecutionRequestResponse associate (ListPattern pattern1, ListPattern pattern2, int time) {
//    
//    ArrayList<ListPattern> stm1Contents = new ArrayList();
//    ArrayList<ListPattern> stm2Contents = new ArrayList();
//    
//    this.getStm(pattern1.getModality()).getContents(time).forEach(Node -> stm1Contents.add(Node.getImage(time)));
//    this.getStm(pattern2.getModality()).getContents(time).forEach(Node -> stm2Contents.add(Node.getImage(time)));
//    
//    if(stm1Contents.contains(pattern1) && stm2Contents.contains(pattern2)){
//    
//    if (ListPattern.isSameModality (pattern1, pattern2)) {
//      
//      return learnAndCreateSemanticLink(pattern1, pattern2, time);
//    }
//    else if(pattern2.getModalityString().equalsIgnoreCase(Modality.ACTION.toString())){
//      return learnAndCreateProduction(pattern1, pattern2, time);
//    }
//    else if(pattern1.isVisual() && pattern2.isVerbal()){
//      return createNamedByLink(pattern1, pattern2, time);
//    }
//  }
  
  /**
   * Learns first pattern (which can be of any modality) and a second pattern 
   * (whose {@link jchrest.lib.Modality} is assumed to be {@link 
   * jchrest.lib.Modality#ACTION}) and creates a production between them.
   */
  private Node learnAndCreateProduction(ListPattern pattern, ListPattern actionPattern, int time) {
    Node recognisedNode = recognise (pattern, time, false);
    
    //Was cognition free?
    if(recognisedNode != null){
    
      //Set the time in this function to the value of the cognition clock which
      //will have been incremented during recognition + 1.  This ensures that 
      //any subsequent cognitive functions will occur when invoked.
      time = this._cognitionClock + 1;
    
      //If the test below passes, it may be that the node retrieved is not equal
      //to the first pattern so some overgeneralisation may occur.
      if(recognisedNode.getImage(time).matches(pattern)){
      
        HashMap<Node, Double> recognisedNodeProductions = recognisedNode.getProductions(time);
        if(recognisedNodeProductions != null) {
        
          //Check each production to see if it matches the action pattern 
          //passed.
          boolean actionPatternAlreadyProduction = false;
          for(Node productionNode : recognisedNodeProductions.keySet()){
          
            //Increment time since a production is being traversed to.
            time += this._ltmLinkTraversalTime;
          
            if (productionNode.getImage(time).matches (actionPattern)) {
              
              //TODO: this is overlearning the first pattern?
              if (productionNode.getImage(time).equals(actionPattern)) {
                actionPatternAlreadyProduction = true;
                recogniseAndLearn (pattern, time); 
              }
              else {
                recogniseAndLearn (actionPattern, time);
              }
              
              //Set the time to the value of the cognition clock since this 
              //will have been incremented due to the recogniseAndLearn() calls
              //above + 1.  Will ensure that any further cognitive functions are 
              //not blocked due to the cognition resource being blocked.
              time = this._cognitionClock + 1;
            }
          }
        
          //If the action pattern isn't already a production recogniseAndLearn the 
          //actionPattern and the first pattern again to force correction of 
          //any mistakes and attempt to recognise the action pattern again.
          //
          //Martyn: This comment was lifted from the learnAndCreateSemanticLink() function in 
          //        this class and I think the mistake alluded to is an 
          //        over-generalisation mistake for the node recognised when the
          //        first pattern is presented.  In other words, it may be the
          //        case that instead of the production being "If I see an
          //        unprotected Queen, q, on a chess board and I can take with 
          //        my Queen, q*, then I should take the q with q*" it is "If I 
          //        see a Queen, q, on a chess board and I can take with my 
          //        Queen, q*, then I should take the q with q*".
          //
          //TODO: check if the mistake is that thought of (see Martyn's comment)
          //      or if it is something else entirely.
          //TODO: this may be overlearning since the pattern or actionPattern
          //      may have been learned above when checking if the actionPattern
          //      matches and equals any of the productions contained in the 
          //      Node recognised when the first pattern is presented?
          if(!actionPatternAlreadyProduction){
            
            recogniseAndLearn (actionPattern, time);
            time = this._cognitionClock + 1;
          
            recogniseAndLearn (pattern, time);
            time = this._cognitionClock + 1;
          
            Node actionNodeRetrieved = recognise (actionPattern, time, false);
            time = this._cognitionClock + 1;

            //This may introduce a non-precise production since only part of the
            //action pattern needs to be learned before a production is created.
            //
            //TODO: check with Fernand if this is OK.
            if (actionNodeRetrieved.getImage(time).matches (actionPattern)) {
              time += this._addProductionTime;
              recognisedNode.addProduction(actionNodeRetrieved, 0.0, time);
              setChanged ();
              if (!_frozen) notifyObservers ();
            }
          }
        }
        else {

          Node actionNodeRetrieved = recognise (actionPattern, time, false);
          time = this._cognitionClock + 1;
        
          //If the conditional is passed here, an overgeneralisation may occur 
          //with regards to production creation.
          //
          //TODO: check with Fernand if this is OK.
          if (actionNodeRetrieved.getImage(time).matches (actionPattern)) {
            time += this._addProductionTime;
            recognisedNode.addProduction(actionNodeRetrieved, 0.0, time);
            setChanged ();
            if (!_frozen) notifyObservers ();
          } 
          else { 
            recogniseAndLearn (actionPattern, time);
            time = this._cognitionClock + 1;
          
            actionNodeRetrieved = recognise (actionPattern, time, false);
            time = this._cognitionClock + 1;
          
            //If the conditional is passed here, an overgeneralisation may occur 
            //with regards to production creation.
            //
            //TODO: check with Fernand if this is OK.
            if (actionNodeRetrieved.getImage(time).matches (actionPattern)) {
              time += this._addProductionTime;
              recognisedNode.addProduction(actionNodeRetrieved, 0.0, time);
              setChanged ();
              if (!_frozen) notifyObservers ();
            }
          }
        }
      }
      else { 
        recogniseAndLearn (pattern, time);
      }
    }
    
    this._cognitionClock = time;
    return recognisedNode;
  }
  
  /**
   * Presents Chrest with a pair of patterns, which it should recogniseAndLearn and 
 then attempt to recogniseAndLearn a link.  Assumes the two patterns are of the same 
   * modality.
   */
  private Node learnAndCreateSemanticLink (ListPattern pattern1, ListPattern pattern2, int time) {
    Node pat1RecognisedNode = recognise (pattern1, time, false);
    time = this._cognitionClock + 1;
    
   // 1. is retrieved node image a match for pattern1?
    if (pat1RecognisedNode.getImage(time).matches (pattern1)) {
      
      // 2. does retrieved node have a lateral link?
      Node associatedNode = pat1RecognisedNode.getAssociatedNode(time);
      time += this._ltmLinkTraversalTime;
      
      if (associatedNode != null) {
        
        // if yes
        //   3. is linked node image match pattern2? if not, recogniseAndLearn pattern2
        if (associatedNode.getImage(time).matches (pattern2)) {
          
          //   if yes
          //   4. if linked node image == pattern2, recogniseAndLearn pattern1, else recogniseAndLearn pattern2
          if (associatedNode.getImage(time).equals (pattern2)) {  
            recogniseAndLearn (pattern1, time); // TODO: this is overlearning?
          } else {
            recogniseAndLearn (pattern2, time);
          }
        } else {
          recogniseAndLearn (pattern2, time);
          time = this._cognitionClock + 1;
          
          recogniseAndLearn (pattern1, time);
          time = this._cognitionClock + 1;
          
          Node pat2RecognisedNode = recognise (pattern2, time, false);
          time = this._cognitionClock + 1;

          // 6. if pattern2 retrieved node image match for pattern2, recogniseAndLearn link, else recogniseAndLearn pattern2
          if (pat2RecognisedNode.getImage(time).matches (pattern2)) {
            time += this._semanticLinkCreationTime;
            pat1RecognisedNode.addSemanticLink(pat2RecognisedNode, time);
            setChanged ();
            if (!_frozen) notifyObservers ();
          }
        } 
      } else {
        // if not
        // 5. sort pattern2
        Node pat2RecognisedNode = recognise (pattern2, time, false);
        time = this._cognitionClock + 1;
        
        // 6. if pattern2 retrieved node image match for pattern2, recogniseAndLearn link, else recogniseAndLearn pattern2
        if (pat2RecognisedNode.getImage(time).matches (pattern2)) {  
          time += this._semanticLinkCreationTime;
          pat1RecognisedNode.addSemanticLink(pat2RecognisedNode, time);
          setChanged ();
          if (!_frozen) notifyObservers ();
        } 
        else { // image not a match, so we need to recogniseAndLearn pattern 2
          recogniseAndLearn (pattern2, time);
          time = this._cognitionClock + 1;
          
          // 5. sort pattern2
          pat2RecognisedNode = recognise (pattern2, time, false);
          time = this._cognitionClock + 1;
          
          // 6. if pattern2 retrieved node image match for pattern2, recogniseAndLearn link, else recogniseAndLearn pattern2
          if (pat2RecognisedNode.getImage(time).matches (pattern2)) {
            time += this._semanticLinkCreationTime;
            pat1RecognisedNode.addSemanticLink(pat2RecognisedNode, time);
            setChanged ();
            if (!_frozen) notifyObservers ();
          }
        }
      }
    } else { // image not a match, so we need to recogniseAndLearn pattern 1
      recogniseAndLearn (pattern1, time);
    }
    
    this._cognitionClock = time;
    return pat1RecognisedNode;
  }
  
  /** 
   * Sort the {@link jchrest.lib.ListPattern} provided through the long-term 
   * memory network of {@link #this} (see {@link #recognise(
   * jchrest.lib.ListPattern, int)), add the {@link jchrest.architecture.Node}
   * recognised to the relevant {@link jchrest.architecture.Stm} {@link 
   * jchrest.lib.Modality} and recogniseAndLearn the {@link jchrest.lib.ListPattern} if 
   * the image of the {@link jchrest.architecture.Node} recognised does not 
   * exactly match the {@link jchrest.lib.ListPattern} provided.
   * 
   * @param pattern
   * @param time
   */
  public void recogniseAndLearn (ListPattern pattern, Integer time) {
    String func = "- learn: ";
    
    this.printDebugStatement(func + "START");
    this.printDebugStatement(
      func + "Recognising and learning " + pattern.toString() + " at time " + 
      time + "."
    );
    
    //Try to recognise the pattern specified; will return null if the cognition
    //resource is not free.
    Node node = recognise (pattern, time, true);
    
    this.printDebugStatement(
      func + "Recognition returned " + (node == null ? 
        "null" : 
        "node with reference " + node.getReference()
      )
    );
    
    // If the node recognised is != null, i.e. the cognition resource is free, 
    // continue.
    if (node != null) { 
      
      //Set current time to be equal to the cognitive clock since lerning should 
      //only continue after a node has been retrieved from LTM.
      this.printDebugStatement(
        func + "Node recognised, the current time will be " +
        "set to the current value of the cognition clock (" + 
        this._cognitionClock + ") since learning can only continue after LTM " +
        "retrieval is complete."
      );
      
      time = this._cognitionClock;
      
      //The model should only try to recogniseAndLearn if the image of the recognised node
      //differs from pattern provided.
      ListPattern recognisedNodeImage = node.getImage(time);
      
      this.printDebugStatement(
        func + "Checking if recognised node image (" + 
        recognisedNodeImage.toString() + ") differs from the pattern passed " + 
        "to this function (" + pattern.toString() + ") and whether the model " +
        "doesn't randomly refuse to learn.  If both conditions are true, " + 
        "learning will occur."
      );
      
      if (!recognisedNodeImage.equals (pattern) && Math.random() < _rho) {
        
        this.printDebugStatement(
          func + "Conditions both evaluate to true so learning will be " +
          "performed"
        );

        //NOTE: discrimination and familiarisation return boolean values but 
        //      they aren't assigned to a variable since they should never 
        //      return false if program execution gets to this stage.
        if (
          node == this.getLtmModalityRootNode(pattern) || //i.e. pattern not recognised at all
          !recognisedNodeImage.matches(pattern) || //pattern recognised but image mismatched
          recognisedNodeImage.isFinished() //can't add to image (familiarisation not allowed)
        ){
          this.discriminate(node, pattern, time);
        } else  { 
          this.familiarise(node, pattern, time);
        }
      }
      else{
        this.printDebugStatement(
          func + "Either the input pattern and image of the node recognised " +
          "are exactly the same (" + recognisedNodeImage.equals(pattern) + ") " + 
          "or they differ but the model randomly refused to learn."
        );
      }
    }
    else{
      this.printDebugStatement(
        func + "Cognitive resource isn't free; neither recognition or learning " +
        "performed."
      );
    }
    
    this.printDebugStatement(func + "RETURN");
  }
  
  /**  
   * Retrieves the {@link jchrest.architecture.Node} reached after sorting the 
   * {@link jchrest.lib.ListPattern} provided through the long-term memory of
   * {@link #this} and places it into the relevant {@link 
   * jchrest.architecture.Stm} (the {@link jchrest.lib.ListPattern} is sorted 
   * vertically at first, then horizontally).
   * 
   * Modifies the cognition and attention clocks.
   * 
   * @param pattern
   * @param time 
   * @param considerTimeAndAddRecognisedNodeToStm
   * 
   * @return 
   */
  public Node recognise (ListPattern pattern, Integer time, Boolean considerTimeAndAddRecognisedNodeToStm) {
    String func = "- recognise: ";
    
    this.printDebugStatement(func + "START");
    this.printDebugStatement(func + "Time " + (considerTimeAndAddRecognisedNodeToStm ? 
      "will" : "will not") + " be considered and the node returned by the " +
      "recognition process performed to ascertain if learning should occur " + 
      (considerTimeAndAddRecognisedNodeToStm ? "will" : "will not") + " be " +
      "added to STM."
    );

    if(considerTimeAndAddRecognisedNodeToStm){
      this.printDebugStatement(
        func + "Checking if cognition resource free (is the current value " + 
        "of the cognition clock (" + this._cognitionClock + ") <= the time " + 
        "this function was invoked (" + time + ")?"
      );
    }
    
    if(this.cognitionFree(time) || !considerTimeAndAddRecognisedNodeToStm){
      
      if(considerTimeAndAddRecognisedNodeToStm) this.printDebugStatement(func + "Cognition resource free.");
      this.printDebugStatement(func + "Attempting to recognise " + pattern.toString() + ".");
      
      //Get root node for modality.
      Node currentNode = getLtmModalityRootNode (pattern);
      
      this.printDebugStatement(
        func + "Retrieved " + currentNode.getImage(time).getModalityString() + 
        " modality root node"
      );
      
      if(considerTimeAndAddRecognisedNodeToStm){
        this.printDebugStatement(
          func + "Incrementing current time (" + time + ") by the time taken " +
          "to traverse a LTM link (" + this._ltmLinkTraversalTime + ")"
        );
        
        time += this._ltmLinkTraversalTime;
      }
      
      List<Link> currentNodeTestLinks = currentNode.getChildren(time);
      ListPattern sortedPattern = pattern;
      int linkToCheck = 0;

      while (linkToCheck < currentNodeTestLinks.size()) {
        Link currentNodeTestLink = currentNodeTestLinks.get(linkToCheck);
        
        this.printDebugStatement(
          func + "Checking if " + pattern.toString() + " passes test (" + 
          currentNodeTestLink.getTest().toString() + ") on link " + 
          linkToCheck + " from node " + currentNode.getReference() + "."
        );
        
        if (currentNodeTestLink.passes (sortedPattern)) { // descend a test link in network
          this.printDebugStatement(func + "Test passed, descending the link to its child node");
          
          if(considerTimeAndAddRecognisedNodeToStm){
            this.printDebugStatement(
              func + "Incrementing the current time (" + time + ") by the time " +
              "taken to traverse a LTM link (" + this._ltmLinkTraversalTime + ")."
            );
            
            time += this._ltmLinkTraversalTime;
          }
          
          // reset the current node, list of children and link index
          currentNode = currentNodeTestLink.getChildNode ();
          currentNodeTestLinks = currentNode.getChildren(time);
          linkToCheck = 0;
          
          // remove the matched test from the sorted pattern
          sortedPattern = sortedPattern.remove (currentNodeTestLink.getTest ());
        } 
        else { // move on to the next link on same level
          
          this.printDebugStatement(
            func + "Test not passed, checking the next test link of node " +
            currentNode.getReference() + "."
          );
          
          linkToCheck += 1;
        }
      }
      
      this.printDebugStatement(
        func + "Descended vertically through long-term memory network as far " + 
        "as possible.  Searching horizontally through long-term memory network for " +
        "a more informative node by searching the semantic links of node " + 
        currentNode.getReference()
      );
      
      if(considerTimeAndAddRecognisedNodeToStm){
        this.printDebugStatement(func + "Cognition clock will be set to the current time (" + time + ").");
        this._cognitionClock = time;
      }
      
      // try to retrieve a more informative node in semantic links
      currentNode = this.searchSemanticLinks(currentNode, this._maximumSemanticLinkSearchDistance, time, false);
      this.printDebugStatement(
        func + "Semantic link search retrieved node with reference " + 
        currentNode.getReference() + "."
      );
      
      if(considerTimeAndAddRecognisedNodeToStm){
        
        this.printDebugStatement(
          func + "Current time will now be set to the value of the cognition " +
          "clock, i.e. the time semantic link search completed: " + 
          this._cognitionClock + ".  Adding node " + currentNode.getReference() + 
          " to STM."
        );
        
        time = this._cognitionClock;
        this.addToStm (currentNode, time);
      }
      
      // return retrieved node
      this.printDebugStatement(func + "Returning node " + currentNode.getReference());
      this.printDebugStatement(func + "RETURN");
      return currentNode;
    }
    else{
      if(considerTimeAndAddRecognisedNodeToStm){
        this.printDebugStatement(func + "Cognition resource not free, returning null");
      }

      this.printDebugStatement(func + "RETURN");
      
      return null;
    }
  }
  
  /**
   * Retrieves the {@link jchrest.architecture.Node} with the greatest 
   * information rating (see {@link jchrest.architecture.Node#information(int)} 
   * by following the semantic links from the {@link jchrest.architecture.Node}
   * specified.  If a semantic link is traversed, this {@link #this} model's
   * attention clock is incremented by the time taken to traverse a {@link 
   * jchrest.architecture.Link} in long-term memory.
   * 
   * Modifies the cognition clock only.
   * 
   * @param node The {@link jchrest.architecture.Node} to start the search from.
   * @param semanticSearchDistanceRemaining
   * @param time
   * 
   * @return 
   */
  private Node searchSemanticLinks (Node node, int semanticSearchDistanceRemaining, int time, boolean considerTime) {
    String func = "- searchSemanticLinks: ";
    
    this.printDebugStatement(func + "START");
    this.printDebugStatement(
      func + "Time " + (considerTime ? "will" : "will not") + " be considered."
    );
      
    String debugStatement = func + "Checking if the maximum semantic search " +
      "distance has been reached";
      
    if(considerTime){
      debugStatement += "or if the cognition resource is busy at the time this " +
        "function is invoked (cognition clock = " + this._cognitionClock + ", " +
        "time function invoked = " + time + ")";
    }

    debugStatement += ".";
    this.printDebugStatement(debugStatement);
    
    //If time costs are to be incurred and the cognition resource is busy or the 
    //limit of semantic search has been reached, return the current node.
    if(
      (considerTime && !this.cognitionFree(time)) ||
      semanticSearchDistanceRemaining <= 0
    ){
      debugStatement = func + "Maximum semantic search distance has been reached (" + 
        (semanticSearchDistanceRemaining <= 0) + ")";
        
      if(considerTime){
        debugStatement += "or cognitive resource is not free (" + !this.cognitionFree(time) + ")";
      }
        
      debugStatement += ". Returning current node (ref: " + node.getReference() + ").";
      this.printDebugStatement(debugStatement);
      this.printDebugStatement(func + "RETURN");
      
      return node;
    }
    else{
      
      this.printDebugStatement(
        func + "Checks passed.  Comparing information count of nodes " + 
        "semantically linked to current node " + node.getReference() + "."
      );
      
      Node bestNode = node;
      for (Node comparisonNode : node.getSemanticLinks(time)) {

        this.printDebugStatement(
          func + "Checking semantically linked to node (ref: " + 
          comparisonNode.getReference() + ")."
        );
        
        if(considerTime){
          this.printDebugStatement(
            "Incrementing current time (" + time + ") by the time taken to " +
            "traverse a long-term memory link (" + this._ltmLinkTraversalTime + 
            ") and setting the cognition clock to this value."
          );
          
          this._cognitionClock = time + this._ltmLinkTraversalTime;
        }

        this.printDebugStatement(
          "Searching semantic links of node semantically linked to node " +
          "with ref: " + comparisonNode.getReference() + "."
        );
        
        Node bestChild = this.searchSemanticLinks(comparisonNode, semanticSearchDistanceRemaining - 1, this._cognitionClock, considerTime);

        this.printDebugStatement(
          func + "Checking if most informative semantic child node (" + 
          bestChild.getReference() + ") is more informative than this node (" + 
          bestNode.getReference() + ")."
        );
        
        if(considerTime){
          
          this.printDebugStatement(
            "Since a node comparison is occurring, the current time will be " +
            "incremented by the time taken to perform a node comparison (" + 
            this._nodeComparisonTime + ") and the cognition clock will be set " +
            "to this value."
          );
          
          this._cognitionClock += this._nodeComparisonTime;
        }

        if(
          bestChild.information(considerTime ? this._cognitionClock : time) > 
          bestNode.information (considerTime ? this._cognitionClock : time)
        ) {
          
          this.printDebugStatement(
            func + "Most informative semantic child node (" + bestChild.getReference() + 
            ") is more informative than this node " + bestNode.getReference() + 
            " so node " + bestChild.getReference() + " will be returned"
          );

          bestNode = bestChild;
        }
      }

      this.printDebugStatement(func + "Returning node " + bestNode.getReference());
      this.printDebugStatement(func + "RETURN");
      
      return bestNode;
    }
  }
  
  /******************/
  /**** Learning ****/
  /******************/

  /**
   * Attempts to increase the total number of {@link jchrest.architecture.Node}s 
   * in the LTM network of this {@link #this} model by adding a new "child" to
   * the {@link jchrest.architecture.Node} specified.
   * 
   * If successful, discrimination will set the cognition clock of {@link #this}
   * to the time the new {@link jchrest.architecture.Node} is added to the 
   * long-term memory network of {@link #this}.
   * 
   * <b>NOTE:</b> It is assumed that the cognition resource is free at the time 
   * that discrimination is requested.  Therefore, this function should only be 
   * called as part of a public function that should ensure that the time 
   * specified is one where the cognition resource is free.  If this is not the
   * case, a {@link java.lang.NullPointerException} will be thrown when 
   * determination of what is new in the {@link jchrest.lib.ListPattern} 
   * specified.
   * 
   * Note: in CHREST 2 tests are pointers to nodes.  This can be 
   * implemented using a Link interface, and having a LinkNode class, 
   * so that checking if test passed is done through the interface.
   * This may be needed later for semantic/template learning.
   * 
   * @param nodeToDiscriminateFrom The {@link jchrest.architecture.Node} that 
   * this function will be applied to.
   * @param pattern The information that should be used to discriminate.
   * @param time
   * 
   * @return {@link java.lang.Boolean#TRUE} if discrimination is successful,
   * {@link java.lang.Boolean#FALSE} if not.
   */
  private boolean discriminate (Node nodeToDiscriminateFrom, ListPattern pattern, int time) {
    String func = "- discriminate: ";
    
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
    
    ListPattern newInformation = pattern.remove (nodeToDiscriminateFrom.getContents());

    this.printDebugStatement(
      func + "Reference of node to discriminate from: " + 
      nodeToDiscriminateFrom.getReference() + ", pattern that triggered " + 
      "discrimination: " + pattern.toString() + ", time discrimination " +
      "invoked: " + time + ".  New information in pattern that triggered " +
      "discrimination: " + newInformation + "."
    );

    historyRowToInsert.put(Chrest._executionHistoryTableInputColumnName, pattern.toString() + "(" + pattern.getModalityString() + ")");
    String description = "New info to learn: '" + newInformation.toString() + "'. Checking if empty...";

    //If the new information is empty it must be handled differently to the way
    //it would be handled if it were not empty.  Unfortunately, it is not 
    //therefore possible to combine how the new information is ultimately added
    //to LTM.
    time += this._discriminationTime;
    boolean discriminationSuccessful = false;
    
    if (newInformation.isEmpty()) {

      // Convert new information so that is is no longer empty but rather, an 
      // end chunk delimeter.
      newInformation.setFinished();
      
      this.printDebugStatement(
        func + "New information is empty so the model will now check if " +
        newInformation.toString() + " has been learned."
      );
      
      boolean endChunkDelimiterKnown = this.recognise(newInformation, time, false).getContents().equals (newInformation);
      
      // 1. < $ > known
      if(endChunkDelimiterKnown){

        // 2. if so, use as test
        description += "New info encoded in LTM, add as test to node " + nodeToDiscriminateFrom.getReference() + ".";
        historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, description);
        this.addEpisodeToExecutionHistory(historyRowToInsert);
        this.printDebugStatement(
          func +  newInformation.toString() + " has been learned so it will " +
          "be added as a test on a new link from " + 
          nodeToDiscriminateFrom.getReference() + "."
        );

        discriminationSuccessful = nodeToDiscriminateFrom.addChild(newInformation, time);
      }
      // 2. < $ > not known
      else {

        description += "New info not encoded in LTM, add as test to " + newInformation.getModalityString() + " root node.";
        historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, description);
        this.addEpisodeToExecutionHistory(historyRowToInsert);
        this.printDebugStatement(
          func +  newInformation.toString() + " has not been learned so it will " +
          "be learned as a primitive."
        );

        //Can't use this.learnPrimitive() here since that function sets the 
        //pattern passed to not finished so the end chunk delimiter would be
        //lost.
        Node child = new Node (this, newInformation, newInformation, time);
        discriminationSuccessful = this.getLtmModalityRootNode(newInformation)
          .addChild(newInformation, child, time, this.getCurrentExperimentName());
      }
    }
    //Cases 3, 4 or 5 if new information isn't empty.
    else{
      
      this.printDebugStatement(
        func + "New information is not empty so the model will now check if " +
        newInformation.toString() + " has been learned."
      );
      Node recognisedNode = this.recognise (newInformation, time, false);
      
      description += "Recognised '" + recognisedNode.getImage(time).toString() + "', node ref: " + recognisedNode.getReference() + "). ";

      // 3. if root node is recognised, then the primitive must be learnt
      if (recognisedNode == this.getLtmModalityRootNode(pattern)) {
        description += "Modality root node, add first item of new info as test to this root node.";
        historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, description);
        this.addEpisodeToExecutionHistory(historyRowToInsert);
        this.printDebugStatement(
          func +  newInformation.toString() + " has not been learned so it will " +
          "be learned as a primitive."
        );
        
        discriminationSuccessful = this.learnPrimitive(newInformation.getFirstItem(), time);
      } 
      // 4. recognised node can be used as a test
      else if (recognisedNode.getContents().matches (newInformation)) {
        ListPattern testPattern = recognisedNode.getContents().clone ();
        
        description += "Contents of rec. node matches new info. Add " + recognisedNode.getContents().toString() + " as test to node " + nodeToDiscriminateFrom.getReference() + ".";
        historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, description);
        this.addEpisodeToExecutionHistory(historyRowToInsert);
        
        this.printDebugStatement(
          func +  newInformation.toString() + " has been learned and " +
          testPattern.toString() + " will be used as a test on a new link " +
          "from node " + nodeToDiscriminateFrom.getReference() + "."
        );
        
        discriminationSuccessful = nodeToDiscriminateFrom.addChild (testPattern, time);
      }
      // 5. mismatch, so use only the first item for test
      // NB: first-item must be in network as node was not the root 
      //     node
      else {
        ListPattern firstItem = newInformation.getFirstItem ();
        firstItem.setNotFinished ();
        
        description += "but image does not match new info. Add " + firstItem.toString() + " as test to node " + nodeToDiscriminateFrom.getReference() + ".";
        historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, description);
        this.addEpisodeToExecutionHistory(historyRowToInsert);
        
        this.printDebugStatement(
          func +  newInformation.toString() + " has been learned but only " +
          "the first item from the new information (" + firstItem.toString() + 
          ") will be used on a new link from node " + 
          nodeToDiscriminateFrom.getReference() + "."
        );

        discriminationSuccessful = nodeToDiscriminateFrom.addChild (firstItem, time);
      }
    }
    
    this.printDebugStatement(
      func + "Discrimination " + (discriminationSuccessful ? "was" : 
      "was not") + " successful so the cognition clock " + 
      (discriminationSuccessful ? "will be set to the time discrimination " + 
      "ends (" + time + ")" : "will not be altered") + "."
    );
    
    if(discriminationSuccessful){
      this._cognitionClock = time;
    }
    
    this.printDebugStatement(func + "RETURN");
    return discriminationSuccessful;
  }
  
  /**
   * Attempts to extend the image for a {@link jchrest.architecture.Node} by 
   * adding new information from the {@link jchrest.lib.ListPattern} provided at 
   * the time specified.
   * 
   * If successful, familiarisation will set the cognition clock of {@link 
   * #this} to the time specified.
   * 
   * <b>NOTE:</b> If the new information to add to the image hasn't been learned
   * as a primitive then the primitive will be learned via. discrimination (see 
   * {@link #this#discriminate(jchrest.architecture.Node, 
   * jchrest.lib.ListPattern, int)}).
   * 
   * @param nodeToFamiliarise
   * @param pattern New information to be added to the {@link 
   * jchrest.architecture.Node} to familiarise's image.
   * @param time The time the new information should be added to the
   * {@link jchrest.architecture.Node} to familiarise's image.
   * 
   * @return {@link java.lang.Boolean#TRUE} if familiarisation is successful,
   * {@link java.lang.Boolean#FALSE} if not.
   */
  private boolean familiarise (Node nodeToFamiliarise, ListPattern pattern, int time) {
    String func = "- familiarise: ";
    
    //Set-up history variables.
    HashMap<String, Object> historyRowToInsert= new HashMap<>();
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
    
    ListPattern newInformation = pattern.remove(nodeToFamiliarise.getImage(time)).getFirstItem();
    newInformation.setNotFinished ();

    historyRowToInsert.put(Chrest._executionHistoryTableInputColumnName, pattern.toString() + "(" + pattern.getModalityString() + ")");
    String description = "New info in input: " + newInformation.toString();
    
    this.printDebugStatement(
      func + "Reference of node to attempt familiarisation on: " + 
      nodeToFamiliarise.getReference() + ", pattern that triggered " + 
      "familiarisation: " + pattern.toString() + ", time familiarisation " +
      "invoked: " + time + ".  New information in pattern that triggered " +
      "familiarisation: " + newInformation + "."
    );

    // EXIT if nothing new to recogniseAndLearn
    if (newInformation.isEmpty ()) {  
      description += ", empty.";
      historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, description);
      this.addEpisodeToExecutionHistory(historyRowToInsert);
      
      this.printDebugStatement(func + "No new information in pattern that triggered familiarisation.");
      this.printDebugStatement(func + "RETURN");
      
      return false;
    }

    // Note: CHREST 2 had the idea of not familiarising if image size exceeds 
    // the max of 5 and 2*contents-size.  This avoids overly large images.
    // This idea is not implemented here.
    //
    Node recognisedNode = this.recognise (newInformation, time, false);
    description += ", not empty, node " + recognisedNode.getReference() + " recognised.  ";

    if (recognisedNode == this.getLtmModalityRootNode (pattern)) {

      // primitive not known, so recogniseAndLearn it
      description += "New information not recognised, learning as primitive.";
      historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, description);
      this.addEpisodeToExecutionHistory(historyRowToInsert);
      this.printDebugStatement(func + "New information unrecognised, learning via. discrimination.");

      return this.discriminate(recognisedNode, newInformation, time);

    } else {

      // extend image with new item
      description += "New information recognised, extending Node " + nodeToFamiliarise.getReference() + "'s image.";
      historyRowToInsert.put(Chrest._executionHistoryTableDescriptionColumnName, description);
      this.addEpisodeToExecutionHistory(historyRowToInsert);
      
      this.printDebugStatement(
        func + "New information recognised, attempting to add new information " +
        "to image of node " +  nodeToFamiliarise.getReference() + " at current " + 
        "time (" + time + ") plus the time taken to familiarise (" +
         this._familiarisationTime+ ")."
      );

      time += this._familiarisationTime;
      Node familiarisationResult = nodeToFamiliarise.extendImage (newInformation, time);
      
      if(familiarisationResult != null){
        this.printDebugStatement(
          func + "Familiarisation successful, setting cognition clock to the " +
          "time node " + nodeToFamiliarise.getReference() + " is familiarised (" + 
          time + ") and returning true."
        );
        this.printDebugStatement(func + "RETURN");
        
        this._cognitionClock = time;
        return true;
      }
      else{
        this.printDebugStatement(func + "Familiarisation unsuccessful, returning false.");
        this.printDebugStatement(func + "RETURN");
        
        return false;
      }
    }
  }
  
  /**
   * Attempts to reinforce the specified production if this {@link #this} model
 is free to recogniseAndLearn.
   * 
   * @param visualPattern
   * @param actionPattern
   * @param variables
   * @param time 
   */
  public void reinforceProduction(ListPattern visualPattern, ListPattern actionPattern, Double[] variables, int time){
    
    Node recognisedNode = this.recognise(visualPattern, time, true);
    
    if(recognisedNode != null){
      
      //The cognition clock will have been incremented by recognition and if 
      //program operation reaches this point, its safe to assume that the 
      //cognition resource is free otherwise the recognised node would have been
      //equal to null.  So, attempt to recognise the action pattern at the time 
      //when the visual pattern is retrieved.
      Node recognisedActionNode = this.recognise(actionPattern, this._cognitionClock, true);

      if(recognisedActionNode != null){
        
        //If program operation reaches this point, its safe to assume that the 
        //cognition resource is free otherwise the recognised node would have 
        //been equal to null.  So, reinforce the production at the cognition 
        //clock time (will have been incremented during recognition)
        int timeReinforcementShouldOccur = this._cognitionClock + this._reinforceProductionTime;
        if(recognisedNode.reinforceProduction(recognisedActionNode, variables, timeReinforcementShouldOccur)){
          
          this._cognitionClock = timeReinforcementShouldOccur;
          this.setChanged ();
          if (!_frozen) notifyObservers ();
        }
      }
    }
  }
  
  /***************************/
  /**** Short-term memory ****/
  /***************************/
  
  /**
   * @param pattern
   * @param time
   * 
   * @return {@link java.lang.Boolean#TRUE} if the {@link 
   * jchrest.lib.ListPattern} passed is present as an image of a {@link 
   * jchrest.architecture.Node} in the {@link jchrest.architecture.Stm} whose 
   * {@link jchrest.lib.Modality} matches that of the {@link 
   * jchrest.lib.ListPattern} passed at the time specified.
   */
  public boolean presentInStm(ListPattern pattern, int time){
    List<Node> contents = this.getStm(pattern.getModality()).getContents(time);
    
    if(contents!= null){
      ArrayList<ListPattern> stmNodeImages = new ArrayList();
      contents.forEach(Node -> stmNodeImages.add(Node.getImage(time)));
      if(stmNodeImages.contains(pattern)) return true;
    }
    
    return false;
  }
  
  /**
   * Attempts to add the {@link jchrest.architecture.Node} specified to the 
   * relevant {@link jchrest.architecture.Stm} modality associated with {@link 
   * #this} at the time passed.
   * 
   * Also, create a bidirectional semantic link between the {@link 
   * jchrest.architecture.Node} to add and the current hypothesisBeforeNodeAdded of the 
 relevant {@link jchrest.architecture.Stm} if applicable, i.e. all of the 
   * following must evaluate to true:
   * 
   * <ol type="1">
   *  <li>
   *    The cognition resource of {@link #this} is free after the {@link 
   *    jchrest.architecture.Node} has been added to the relevant {@link 
   *    jchrest.architecture.Stm}.
   *  </li>
   *  <li>
   *    The {@link jchrest.lib.Modality} of the {@link 
   *    jchrest.architecture.Node} added is {@link jchrest.lib.Modality#VISUAL}.
   *  </li>
   *  <li>
   *    The image of the {@link jchrest.architecture.Node} added is sufficiently 
    "similar" to the hypothesisBeforeNodeAdded' image (see {@link 
   *    jchrest.lib.ListPattern#isSimilarTo(jchrest.lib.ListPattern, int)}).
   *  </li>
   *  
   * </ol>
   * 
   * If the {@link jchrest.architecture.Node} is added to a {@link 
   * jchrest.architecture.Stm}, the attention clock will be incremented.  If the
   * bidirectional semantic link is created, the cognition clock will also be
   * incremented.
   * 
   * @param nodeToAdd
   * @param time
   * 
   * @return {@link java.lang.Boolean#TRUE} if the {@link 
   * jchrest.architecture.Node} was added to the relevant {@link 
   * jchrest.architecture.Stm} successfully, {@link java.lang.Boolean#FALSE} if 
   * not.
   */
  private boolean addToStm (Node nodeToAdd, int time) {
    String func = "- addToStm: ";
    this.printDebugStatement(func + "START");
    
    this.printDebugStatement(
      func + "Attempting to add node " + nodeToAdd.getReference() + " to " +
      nodeToAdd.getImage(time).getModalityString() + " STM.  Checking if " + 
      "attention resource is free at time function invoked i.e. is the " +
      "current attention clock value (" + this._attentionClock + ") <= the " + 
      "time this function is invoked (" + time + ")?"
    );
    
    if(this.attentionFree(time)){
      
      this.printDebugStatement(
        func + "Attention resource is free so node " + 
        nodeToAdd.getReference() + " will be added to STM at time " + 
        (time + this._timeToUpdateStm) + " (the current time, " +
        time + ", plus the time it takes to update STM (" + 
        this._timeToUpdateStm + ")."
      );
      
      Stm stm = this.getStm(nodeToAdd.getImage(time).getModality());
      
      // TODO: Check if this is the best place
      // Idea is that nodeToAdd's filled slots are cleared when put into STM, 
      // are filled whilst in STM, and forgotten when it leaves.
      nodeToAdd.clearFilledSlots (time); 
      
      if(stm.add (nodeToAdd, time + this._timeToUpdateStm)){
        
        this.printDebugStatement(
          func + "STM addition successful, setting the current time to the " +
          "time node " + nodeToAdd.getReference() + " was added to STM (" + 
          (time + this._timeToUpdateStm) + ") and setting the attention clock " +
          "to this value."
        );
        time += this._timeToUpdateStm;
        this._attentionClock = time;
        setChanged ();
        if (!_frozen) notifyObservers ();

        //Try to create two-way semantic link
        //TODO: Reimplement this but with a call to a more abstract "associate"
        //      function that will create any type of link possible depending
        //      on the modality of the node just added and the contents of other
        //      STM structures.
        //      Will have implications for production creation for Tileworld
        //      agents, speak to Fernand about this.
//        System.out.println(
//          func + "Attempting to create a semantic link between the node that " +
//          "was the STM hypothesis node before node " + 
//          nodeToAdd.getReference() + " was added and the node just " +
//          "added.  To do this, the cognition resource must be free at the time " +
//          "the node is added to STM.  If it is, the modality of the STM that " + 
//          "the node was added to will also be checked to see is of visual " +
//          "modality and if it is, are there at least two nodes in it since the " +
//          "node just addded may have been the hypothesis previous to the addition."
//        );
        
        
      
//        if (
//          this.cognitionFree(time) &&
//          stm.getModality() == Modality.VISUAL &&
//          stm.getCount(time) > 1
//        ) {
//          
//          //No need to check the result of stm.getItem() below; null will never 
//          //be returned since there will be a node in visual STM (one was just 
//          //added) and STM must exist otherwise the addition above would not 
//          //have occurred.
//          Node hypothesisBeforeNodeAdded = stm.getItem(0, time - 1);
//          time += this._timeToRetrieveItemFromStm;
//          this._attentionClock = time;
//          
//          System.out.println(
//            func + "Cognition is free, the node was added to visual STM " +
//            "and there are at least 2 nodes in visual STM.  Retrieved the " +
//            "visual STM hypothesis that existed before node " + 
//            nodeToAdd.getReference() + " was added (hypothesis ref: " + 
//            hypothesisBeforeNodeAdded.getReference() + ") and have set both " +
//            "the current time and attention clock to the time when STM addition " +
//            "completed plus the time it takes to retrive a STM item (" + 
//            time + ")."
//          );
//        
//          System.out.println(
//            func + "Checking if node " + nodeToAdd.getReference() + "'s image (" + 
//            nodeToAdd.getImage(time) + ") is similar enough to the hypothesis' " + 
//            "image (" + hypothesisBeforeNodeAdded.getImage(time) + ") for a " +
//            "semantic link to be made."
//          );
//          boolean nodeAddedToStmSimilarEnoughToHypothesis = nodeToAdd.getImage(time).isSimilarTo(hypothesisBeforeNodeAdded.getImage(time), this._nodeImageSimilarityThreshold);
//          
//          time += this._nodeComparisonTime;
//          this._cognitionClock = time;
//          System.out.println(
//            func + "Node image comparison complete.  Incremented current " +
//            "time by the time required to compare two nodes (" + time + 
//            ") and set the cognition clock to this value." 
//          );
//          
//          if(nodeAddedToStmSimilarEnoughToHypothesis){
//          
//            System.out.println(
//              func + "Node images are similar enough to create a bilateral " +
//              "semantic link between them.  Attempting to create a semantic " +
//              "link from from node " + nodeToAdd.getReference() + " to " + 
//              hypothesisBeforeNodeAdded.getReference() + "."
//            );
//            if(nodeToAdd.addSemanticLink (hypothesisBeforeNodeAdded, time + this._semanticLinkCreationTime)){
//            
//              time += this._semanticLinkCreationTime;
//              this._cognitionClock = time;
//              System.out.println(
//                func + "Semantic link creation from node " + 
//                nodeToAdd.getReference() + " to " + 
//                hypothesisBeforeNodeAdded.getReference() + " achieved.  Current " +
//                "time and attention clock set to " + time + "."
//              );
//              
//              System.out.println(
//                func + "Attempting to create a semantic link from the " +
//                "visual STM hypothesis to the node added to STM at the " + 
//                "current time, i.e. when the cognition resource is free (" + 
//                this._cognitionClock + ")."
//              );
//              if(hypothesisBeforeNodeAdded.addSemanticLink (nodeToAdd, time)){
//              
//                time += this._semanticLinkCreationTime;
//                this._cognitionClock = time;
//                System.out.println(
//                  func + "Semantic link creation from node " + 
//                  hypothesisBeforeNodeAdded.getReference() + " to " + 
//                  nodeToAdd.getReference() + " achieved.  Current " +
//                  "time and attention clock set to " + time + "."
//                );
//              }
//              else{
//                System.out.println(
//                  func + "Semantic link creation from node " + 
//                  hypothesisBeforeNodeAdded.getReference() + " to " + 
//                  nodeToAdd.getReference() + " failed."
//                );
//              }
//            }
//            else{
//              System.out.println(
//                func + "Semantic link creation from node " + 
//                nodeToAdd.getReference() + " to " + 
//                hypothesisBeforeNodeAdded.getReference() + " failed."
//              );
//            }
//          }
//          else{
//            System.out.println(
//              func + "Semantic link creation denied; images of node added to " + 
//              "visual STM and visual STM hypothesis before addition not " +
//              "similar enough."
//            );
//          }
//        }
//        else{
//          System.out.println(
//            func + "Semantic link creation denied.  Either cognition isn't " +
//            "free (" + (!this.cognitionFree(time)) + "), the STM modality " + 
//            "that the node was added to isn't visual (" + 
//            (stm.getModality() != Modality.VISUAL) + ") or the number of " +
//            "items in visual STM is < 2 (" + (stm.getCount(time) < 2) + ")."
//          );
//        }
        
        this.printDebugStatement(func + "RETURN");
        return true;
      }
      else{
        this.printDebugStatement(func + "STM addition unsuccessful.");
      }
    }
    else{
      this.printDebugStatement(func + "Attention resource isn't free");
    }
    
    this.printDebugStatement(func + "RETURN");
    return false;
  }
  
  /**
   * Modifies the attention clock of {@link #this}.
   * 
   * If the attention resource is free at the time this function is requested, 
 the hypothesisBeforeNodeAdded (the first {@link jchrest.architecture.Node} in a {@link 
   * jchrest.architecture.Stm} modality) in the relevant {@link 
   * jchrest.architecture.Stm} modality associated with {@link #this} is 
   * replaced with the {@link jchrest.architecture.Node} specified.
 
 The time the new hypothesisBeforeNodeAdded is added to the relevant {@link 
   * jchrest.architecture.Stm}, <i>t</i>, is equal to the time this function is 
   * invoked plus the time specified to add update short-term memory (see {@link 
   * #getTimeToUpdateStm()}). The attention clock of {@link #this} will also be
   * set to <i>t</i>.
   * 
   * @param replacement
   * @param time 
   */
  public void replaceStmHypothesis(Node replacement, int time){
    if(this._attentionClock < time){
      time += this._timeToUpdateStm;
      Stm stmToReplaceHypothesisIn = this.getStm(replacement.getImage(time).getModality());
      if(stmToReplaceHypothesisIn.replaceHypothesis(replacement, time)){
        this._attentionClock = time;
      }
    }
  }
  
  /*******************/
  /**** TEMPLATES ****/
  /*******************/
  
  /**
   * Instruct {@link #this} to make templates throughout the entirety of its 
   * visual long-term memory modality at the time specified.
   * 
   * Templates will only be created if all the following statements are true:
   * 
   * <ul>
   *  <li>{@link #this} is "alive" at the time specified.</li> 
   *  <li>{@link #this} can make templates.</li>
   *  <li>
   *    The cognition resource of {@link #this} is free at the time specified.
   *  </li>
   * </ul>
   * 
   * This function is usually called at the end of a training session that 
   * {@link #this} has been engaged in but can be called as {@link #this} is
   * interacting with the external domain.
   * 
   * <b>NOTE:</b> currently, the template construction process only works for 
   * {@link jchrest.lib.Modality#VISUAL} {@link jchrest.architecture.Node}s that
   * have {@link jchrest.lib.ItemSquarePattern} images.
   * 
   * @param time
   */
  public void makeTemplates (int time) {
    if(
      this._creationTime <= time && 
      this._canCreateTemplates &&
      this._cognitionClock < time
    ){
      this.makeTemplates (this._visualLtm, time);
    }
  }
  
  /**
   * 
   * @param node
   * @param time 
   * 
   * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
   */
  
  //TODO: timings need to be implemented here.  In an e-mail to me on the 15th
  //      Dec 2015 regarding this, Fernand says: "It's supposed to be a slow 
  //      process, but we never put a parameter on this. Roughly, I would say 
  //      10 seconds for each slot created. Another possibility, a bit 
  //      speculative, is that it's an even slower process, which occurs for 
  //      example during sleep (e.g. during dreaming). There is a literature 
  //      on consolidation of LTM during sleep that we could use. We'll think 
  //      about this when we'll have submitted a few papers!".
  //      
  //      Personally, I think there should be an access time cost (time taken 
  //      to traverse LTM as per this.recognise()) and then a time cost for
  //      each slot.  This means that the Node.makeTemplate() procedure should
  //      return how many slots were created and then the cognition clock 
  //      should be incremented accordingly.
  private void makeTemplates(Node node, int time){
    
    if(!node.isRootNode()){
      node.makeTemplate(time);
    }
    
    for (Link link : node.getChildren(time)) {
      this.makeTemplates (link.getChildNode(), time);
    }
  }

  //TODO: Organise and check all code below this point.
  
  /******************************/
  /**** PERCEPTION FUNCTIONS ****/
  /******************************/
  
  private boolean isPerceiverFree(int time){
    return this._perceiverClock < time;
  }
  
  public int getSaccadeTime() {
    return _saccadeTime;
  }

  public void setSaccadeTime(int saccadeTime) {
    this._saccadeTime = saccadeTime;
  }
  
  private boolean sameColour (ListPattern move, String colour) {
    if (colour == null) return true;
    if ((move.size () == 1) && (move.getItem(0) instanceof ItemSquarePattern)) {
        ItemSquarePattern m = (ItemSquarePattern)move.getItem (0);
        return m.getItem() == colour;
    } else {
      return false;
    }
  }
  
  /**
   * Learns the {@link jchrest.lib.Scene} specified using the {@link 
   * jchrest.architecture.Perceiver} associated with this {@link #this} model
   * if its perceptual resources aren't busy at the time this function is 
   * requested.
   * 
   * This will populate the visual {@link jchrest.architecture.Stm} of this
   * {@link #this} model with recognised information and various other instance
   * variables of the {@link jchrest.architecture.Perceiver} associated with
   * this {@link #this} model.  The function will also cause this {@link #this}
 model to recogniseAndLearn any information "seen".
   * 
   * @param scene The {@link jchrest.lib.Scene} to recogniseAndLearn from.
   * @param numFixations Number of fixations this {@link #this} model's {@link
   * jchrest.architecture.Perceiver} should make on the {@link 
   * jchrest.lib.Scene}.
   * @param time The time this function is invoked.
   * @param timeSceneSeenUntil The time that this {@link #this} model can no 
   * longer "see" the {@link jchrest.lib.Scene} to be learned from.  If there is
   * no time limit on how long the {@link jchrest.lib.Scene} can be "seen" for 
   * then pass null.
   * @return A {@link java.lang.Boolean} value indicating whether the {@link 
   * jchrest.lib.Scene} specified was "looked at" or not.  {@link 
   * java.lang.Boolean#TRUE} will be returned if this {@link #this} model's 
   * perceptual resources are not busy at the time this function is invoked.
   * {@link java.lang.Boolean#FALSE} will be returned if this {@link #this} 
   * model's perceptual resources are busy at the time this function is invoked.
   */
  public boolean learnScene (Scene scene, int numFixations, int time, Integer timeSceneSeenUntil) {
    if(this.perceiverFree(time)){
      
      _perceiver.setScene (scene);
      _perceiver.start (numFixations, time);
      
      if(timeSceneSeenUntil == null){
        for(int i = 0; i < numFixations; i++, time += this.getSaccadeTime()) {
          _perceiver.moveEyeAndLearn (time);
        }
      }
      else{
        for(int i = 0; i < numFixations || time <= timeSceneSeenUntil; i++, time += this.getSaccadeTime()) {
          _perceiver.moveEyeAndLearn (time);
        }
      }
      
      this._perceiverClock = time;
      return true;
    }
    
    return false;
  }
  
  /**
   * Scans the {@link jchrest.lib.Scene} specified using the {@link 
   * jchrest.architecture.Perceiver} associated with this {@link #this} model
   * if its perceptual resources aren't busy at the time this function is 
   * requested.
   * 
   * This will populate the visual {@link jchrest.architecture.Stm} of this
   * {@link #this} model with recognised information and various other instance
   * variables of the {@link jchrest.architecture.Perceiver} associated with
   * this {@link #this} model.
   * 
   * @param scene The {@link jchrest.lib.Scene} to scan.
   * @param numFixations Number of fixations this {@link #this} model's {@link
   * jchrest.architecture.Perceiver} should make on the {@link 
   * jchrest.lib.Scene}.
   * @param time The time this function is invoked.
   * @param timeSceneSeenUntil The time that this {@link #this} model can no 
   * longer "see" the {@link jchrest.lib.Scene} to be scanned.  If there is no
   * time limit on how long the {@link jchrest.lib.Scene} can be "seen" for then
   * pass null. 
   * @param debug 
   * @return A {@link java.lang.Boolean} value indicating whether the {@link 
   * jchrest.lib.Scene} specified was "looked at" or not.  {@link 
   * java.lang.Boolean#TRUE} will be returned if this {@link #this} model's 
   * perceptual resources are not busy at the time this function is invoked.
   * {@link java.lang.Boolean#FALSE} will be returned if this {@link #this} 
   * model's perceptual resources are busy at the time this function is invoked.
   */
  public boolean scanScene(Scene scene, int numFixations, int time, Integer timeSceneSeenUntil, boolean debug){
    if(debug) System.out.println("\n=== Chrest.scanScene() ===");
    if(debug) System.out.println("- Checking if perceiver resource is free @ time " + time);
    
    if(this.perceiverFree(time)){
      if(debug) System.out.println("   - Perceiver resource free, setting scene for perceiver");
      _perceiver.setScene (scene); //Also clears any previous fixations

      if(debug) System.out.println("   - Starting fixations (setting perceiver to initial fixation)");
      _perceiver.start (numFixations, time);
      
      if(timeSceneSeenUntil == null){
        if(debug) System.out.println("   - Making fixations until I've fixated " + numFixations + " times");
        for (int i = 0; i < numFixations; i++, time += this.getSaccadeTime()) {
          if(debug) System.out.println("   - Making " + (i+1) + " of " + numFixations + " fixations @ time " + time);
          _perceiver.moveEye (time, debug);
        }
      }
      else{
        if(debug) System.out.println("   - Making fixations until I've fixated " + numFixations + " times or time > the time the scene can be seen until (" + timeSceneSeenUntil + ")");
        for (int i = 0; i < numFixations || time <= timeSceneSeenUntil; i++, time += this.getSaccadeTime()) {
          if(debug) System.out.println("   - Making " + (i+1) + " of " + numFixations + " fixations @ time " + time);
          _perceiver.moveEye (time, debug);
        }
      }
      
      this._perceiverClock = time;
      if(debug) System.out.println("   - Finished fixations, perceiver resource will be busy until " + this._perceiverClock);
      if(debug) System.out.println("   - Returning true");
      return true;
    }
    
    if(debug) System.out.println("   - Perceiver resource not free, returning false");
    return false;
  }
  
  /** 
   * Scan given {@link jchrest.lib.Scene}, <i>s</i>, at the time specified and 
   * create a new {@link jchrest.lib.Scene}, <i>s*</i>, that contains 
   * information recognised in <i>s</i>.
   * 
   * Default behaviour is to clear the visual {@link jchrest.architecture.Stm}
   * associated with this {@link #this} before scanning the 
   * {@link jchrest.lib.Scene} specified and to not print debugging information.
   * Use {@link jchrest.architecture.Chrest#scanScene(jchrest.lib.Scene, int, 
   * boolean, int, boolean) if control over clearing visual {@link 
   * jchrest.architecture.Stm} and debugging is required.
   * 
   * @param scene The {@link jchrest.lib.Scene} to scan and recall.
   * @param numFixations Number of fixations this {@link #this} model's {@link
   * jchrest.architecture.Perceiver} should make on the {@link 
   * jchrest.lib.Scene}.
   * @param time The time this function is invoked.
   * @param timeSceneSeenUntil The time that this {@link #this} model can no 
   * longer "see" the {@link jchrest.lib.Scene} to scan and recall.  If there is
   * no time limit on how long the {@link jchrest.lib.Scene} can be "seen" for 
   * then pass null.
   * 
   * @return A {@link jchrest.lib.Scene} containing {@link 
   * jchrest.lib.SceneObject}s that this {@link #this} model recognises in the 
   * {@link jchrest.lib.Scene} passed or null if this {@link #this} model's 
   * perceptual resources are busy. 
   */
  public Scene scanAndRecallScene (Scene scene, int numFixations, int time, Integer timeSceneSeenUntil) {  
    return scanAndRecallScene (scene, numFixations, true, time, timeSceneSeenUntil, false);
  }
  
  /** 
   * Scan given {@link jchrest.lib.Scene}, <i>s</i>, at the time specified and 
   * create a new {@link jchrest.lib.Scene}, <i>s*</i>, that contains 
   * information recognised in <i>s</i>.
   * 
   * @param scene The {@link jchrest.lib.Scene} to scan and recall.
   * @param numFixations Number of fixations this {@link #this} model's {@link
   * jchrest.architecture.Perceiver} should make on the {@link 
   * jchrest.lib.Scene}.
   * @param clearVisualStm Set to {@link java.lang.Boolean#TRUE} to clear the
   * visual {@link jchrest.architecture.Stm} associated with this {@link #this}
   * model before scanning the {@link jchrest.lib.Scene}.  Set to {@link 
   * java.lang.Boolean#FALSE} to not clear visual {@link 
   * jchrest.architecture.Stm}.
   * @param time The time this function is invoked.
   * @param timeSceneSeenUntil The time that this {@link #this} model can no 
   * longer "see" the {@link jchrest.lib.Scene} to scan and recall.  If there is
   * no time limit on how long the {@link jchrest.lib.Scene} can be "seen" for 
   * then pass null.
   * @param debug
   * 
   * @return A {@link jchrest.lib.Scene} containing {@link 
   * jchrest.lib.SceneObject}s that this {@link #this} model recognises in the 
   * {@link jchrest.lib.Scene} passed or null if this {@link #this} model's 
   * perceptual resources are busy.  
   */
  public Scene scanAndRecallScene(Scene scene, int numFixations, boolean clearVisualStm, int time, Integer timeSceneSeenUntil, boolean debug) {
    
    if(debug) System.out.println("=== Chrest.scanScene() ===");
    if(debug) System.out.println("- Requested to scan scene with name '" + scene.getName() + "' at time " + time);
    
    // only clear STM if flag is set
    if (clearVisualStm) {
      if(debug) System.out.println("- Clearing STM");
      _visualStm.clear (time);
    }

    //Get the VisualSpatialField associated with the Scene to be scanned.  If
    //there is an associated VisualSpatialField, SceneObjects that are 
    //recognised when scanning the Scene below will have their corresponding
    //VisualSpatialFieldObject recognised status updated since the 
    //VisualSpatialField is essentially being looked at if the Scene to be 
    //scanned is associated with one.
    VisualSpatialField associatedVisualSpatialField = scene.getVisualSpatialFieldGeneratedFrom();
    if(debug) System.out.println("- Does the Scene to be scanned represent a VisualSpatialField? " + (associatedVisualSpatialField  != null));

    //Create a data structure to the identifiers of objects that are recognised
    //when scanning the Scene.  This will be used to determine what objects are
    //unrecognised if the Scene represents a VisualSpatialField.
    ArrayList<String> recognisedObjectIdentifiers = new ArrayList<>();

    //Instantiate recalled Scene, this will be a "blind" canvas initially.
    Scene recalledScene = new Scene (
      "Recalled scene of " + scene.getName (), 
      scene.getWidth (), 
      scene.getHeight (),
      scene.getVisualSpatialFieldGeneratedFrom()
    );

    //Scan the scene.
    if(this.scanScene(scene, numFixations, time, timeSceneSeenUntil, debug)){

      // -- get items from image in STM, and optionally template slots
      // TODO: use frequency count in recall
      if(debug) System.out.println("- Processing recognised chunks");
      for (Node node : _visualStm) {

        ListPattern nodeImage = node.getImage(time);

        //If the node isn't the visual LTM root node (nothing recognised) then,
        //continue.
        if(this.getLtmModalityRootNode(Modality.VISUAL) != node && !nodeImage.isEmpty()){

          if(debug) System.out.println("   - Processing chunk: " + nodeImage.toString());
          if (_canCreateTemplates) { // check if templates needed
            if(debug) System.out.println("   - Templates are enabled, so append any filled slot information to the chunk");
            nodeImage = nodeImage.append(node.getFilledSlots (time));
          }

          if(debug) System.out.println("   - Processing chunk with image: '" + nodeImage.toString() + "'");

          nodeImage = this.getDomainSpecifics().convertDomainSpecificCoordinatesToSceneSpecificCoordinates(nodeImage, scene);
          if(debug) System.out.println("      - Image with scene-specific coordinates: '" + nodeImage.toString() + "'");

          //Add all recognised items to the scene to be returned and flag the 
          //corresponding VisualSpatialFieldObjects as being recognised.
          for (int i = 0; i < nodeImage.size(); i++){
            PrimitivePattern item = nodeImage.getItem(i);

            if (item instanceof ItemSquarePattern) {
              ItemSquarePattern ios = (ItemSquarePattern)item;
              int col = ios.getColumn ();
              int row = ios.getRow ();

              if(debug) System.out.println("      - Processing object " + ios.toString() );

              //Get the SceneObject that represents the recalled object
              SceneObject recognisedObject = scene.getSquareContents(col, row);
              //TODO: Write a test that checks for the null check below preventing
              //      this function from erroring-out when the recognisedObject is 
              //      set to null (picked up in Netlogo where a turtle learned a
              //      ListPattern like <[H 0 -2][T -2 -1]> and recognised this 
              //      when it was at the edge of a Scene and a hole was in the
              //      location specified in the ListPattern but the tile wasn't
              //      since that location was not represented since the "self" was
              //      on the western-most point of the Scene scanned).
              if(debug) System.out.println("         ~ Equivalent of object in scene scanned");

              //The recalled object may be a ghost (part of a LTM chunk but doesn't exist in the
              //scene being scanned) so check for this here lest a NullPointerException be thrown.
              if(recognisedObject != null){

                if(debug){
                  System.out.println("            = ID: " + recognisedObject.getIdentifier());
                  System.out.println("            = Class: " + recognisedObject.getObjectClass());
                  System.out.println("         ~ Adding object to col " + col + ", row " + row + " in the Scene recalled");
                }

                recalledScene.addItemToSquare(col, row, recognisedObject.getIdentifier(), recognisedObject.getObjectClass());

                if(associatedVisualSpatialField != null){
                  if(debug) System.out.println("         ~ Updating object in associated visual-spatial field");
                  for(VisualSpatialFieldObject objectOnVisualSpatialSquare : associatedVisualSpatialField.getSquareContents(col, row, time)){
                    if(objectOnVisualSpatialSquare.getIdentifier().equals(recognisedObject.getIdentifier())){
                      objectOnVisualSpatialSquare.setRecognised(time, true);
                      recognisedObjectIdentifiers.add(objectOnVisualSpatialSquare.getIdentifier());

                      if(debug){
                        System.out.println("            ID: " + objectOnVisualSpatialSquare.getIdentifier());
                        System.out.println("            Class: " + objectOnVisualSpatialSquare.getObjectClass());
                        System.out.println("            Created at: " + objectOnVisualSpatialSquare.getTimeCreated());
                        System.out.println("            Terminus: " + objectOnVisualSpatialSquare.getTerminus());
                        System.out.println("            Recognised: " + objectOnVisualSpatialSquare.recognised(time));
                        System.out.println("            Ghost: " + objectOnVisualSpatialSquare.isGhost());
                      }
                    }
                  }
                }
              }
              else if(debug){
                System.out.println("            = There is no equivalent object (recognised object must be a ghost)");
              }
            }
          }
        }
      }

      //Process unrecognised objects in the associated visual-spatial field.
      if(associatedVisualSpatialField != null){
        if(debug) System.out.println("- Processing unrecognised objects in associated visual-spatial field");
        for(int row = 0; row < associatedVisualSpatialField.getHeight(); row++){
          for(int col = 0; col < associatedVisualSpatialField.getWidth(); col++){
            if(debug) System.out.println("   - Processing objects on col " + col + ", row " + row);

            for(VisualSpatialFieldObject object : associatedVisualSpatialField.getSquareContents(col, row, time)){  
              if(!recognisedObjectIdentifiers.contains(object.getIdentifier())){
                if(debug){
                  System.out.println("      - Object unrecognised.  Current status:");
                  System.out.println("         ID: " + object.getIdentifier());
                  System.out.println("         Class: " + object.getObjectClass());
                  System.out.println("         Created at: " + object.getTimeCreated());
                  System.out.println("         Terminus: " + object.getTerminus());
                  System.out.println("         Recognised: " + object.recognised(time));
                  System.out.println("         Ghost: " + object.isGhost());
                }

                //Squares that contain blind objects and the creator's avatar will 
                //have null termini so do not overwrite these.
                if(object.getTerminus() == null){
                  object.setUnrecognised(time, false);
                }else{
                  object.setUnrecognised(time, true);
                }

                if(debug){
                  System.out.println("         - After processing:");
                  System.out.println("            ID: " + object.getIdentifier());
                  System.out.println("            Class: " + object.getObjectClass());
                  System.out.println("            Created at: " + object.getTimeCreated());
                  System.out.println("            Terminus: " + object.getTerminus());
                  System.out.println("            Recognised: " + object.recognised(time));
                  System.out.println("            Ghost: " + object.isGhost());
                }
              }
            }
          }
        }
      }

      //If the creator of the scene was identified in the original scene then add
      //it into the recalled scene.
      Square creatorLocation = scene.getLocationOfCreator();
      if(creatorLocation != null){
        SceneObject self = scene.getSquareContents(creatorLocation.getColumn(), creatorLocation.getRow());
        recalledScene.addItemToSquare(creatorLocation.getColumn(), creatorLocation.getRow(), self.getIdentifier(), self.getObjectClass());
      }
      
      return recalledScene;
    }
    
    return null;
  }
  
  /**
   * A predicted move is defined as being a production associated with a visual 
   * {@link jchrest.architecture.Node} recognised after scanning the {@link 
   * jchrest.lib.Scene} passed.  A move is the image of the action {@link 
   * jchrest.architecture.Node} that forms a production.
   * 
   * @param scene
   * @param numFixations
   * @param colour
   * @param time
   * @param timeSceneSeenUntil The time that this {@link #this} model can no 
   * longer "see" the {@link jchrest.lib.Scene} to predict moves in context of.  
   * If there is no time limit on how long the {@link jchrest.lib.Scene} can be 
   * "seen" for then pass null.
   * 
   * @return If this {@link jchrest.architecture.Chrest} model's {@link 
   * jchrest.architecture.Perceiver} is free, a map of predicted moves vs their 
   * frequency of occurrence in visual {@link jchrest.architecture.Stm} after 
   * scanning the {@link jchrest.lib.Scene} passed using {@link 
   * jchrest.architecture.Chrest#scanScene(jchrest.lib.Scene, int, int, int, 
   * boolean)} is returned.  If the {@link jchrest.architecture.Perceiver} is 
   * not free, null is returned.
   */
  public Map<ListPattern, Integer> getMovePredictions (Scene scene, int numFixations, String colour, int time, int timeSceneSeenUntil) {
    
    if(this.scanScene (scene, numFixations, time, timeSceneSeenUntil, false)){
      
      Map<ListPattern, Integer> moveFrequencies = new HashMap<ListPattern, Integer> ();
      for (Node node : _visualStm) {
        for (Node action : node.getProductions (time).keySet()) {
          if (sameColour(action.getImage(time), colour)) {
            if (moveFrequencies.containsKey(action.getImage (time))) {
              moveFrequencies.put (
                  action.getImage (time), 
                  moveFrequencies.get(action.getImage (time)) + 1
                  );
            } else {
              moveFrequencies.put (action.getImage (time), 1);
            }
          }
        }
      }
      return moveFrequencies;
    }
    
    return null;
  }

  /**
   * Predict a move using a CHUMP-like mechanism.
   * 
   * TODO: Improve the heuristics here.
   * 
   * @param scene
   * @param numFixations
   * @param time
   * @param timeSceneSeenUntil The time that this {@link #this} model can no 
   * longer "see" the {@link jchrest.lib.Scene} to predict moves in context of.  
   * If there is no time limit on how long the {@link jchrest.lib.Scene} can be 
   * "seen" for then pass null.
   * 
   * @return 
   */
  public Move predictMove (Scene scene, int numFixations, int time, Integer timeSceneSeenUntil) {
    Map<ListPattern, Integer> moveFrequencies = getMovePredictions (scene, numFixations, null, time, timeSceneSeenUntil);
    
    if(moveFrequencies != null){
      // find the most frequent pattern
      ListPattern best = null;
      int bestFrequency = 0;
      for (ListPattern key : moveFrequencies.keySet ()) {
        if (moveFrequencies.get (key) > bestFrequency) {
          best = key;
          bestFrequency = moveFrequencies.get (key);
        }
      }
      // create a move to return
      // list pattern should be one item long, with the first item being an ItemSquarePattern
      if (best != null && (best.size () == 1) && (best.getItem(0) instanceof ItemSquarePattern)) {
        ItemSquarePattern move = (ItemSquarePattern)best.getItem (0);
        return new Move (move.getItem (), move.getRow (), move.getColumn ());
      }
    }
    
    return new Move ("UNKNOWN", 0, 0);
  }

  /**
   * Predict a move using a CHUMP-like mechanism.
   * 
   * TODO: Improve the heuristics here.
   * 
   * @param scene
   * @param numFixations
   * @param colour
   * @param time
   * @param timeSceneSeenUntil The time that this {@link #this} model can no 
   * longer "see" the {@link jchrest.lib.Scene} to predict moves in context of.  
   * If there is no time limit on how long the {@link jchrest.lib.Scene} can be 
   * "seen" for then pass null.
   * 
   * @return
   */
  public Move predictMove (Scene scene, int numFixations, String colour, int time, Integer timeSceneSeenUntil) {
    Map<ListPattern, Integer> moveFrequencies = getMovePredictions (scene, numFixations, colour, time, timeSceneSeenUntil);
    
    if(moveFrequencies != null){
      // find the most frequent pattern
      ListPattern best = null;
      int bestFrequency = 0;
      for (ListPattern key : moveFrequencies.keySet ()) {
        if (moveFrequencies.get (key) > bestFrequency) {
          best = key;
          bestFrequency = moveFrequencies.get (key);
        }
      }
      // create a move to return
      // list pattern should be one item long, with the first item being an ItemSquarePattern
      if (best != null && (best.size () == 1) && (best.getItem(0) instanceof ItemSquarePattern)) {
        ItemSquarePattern move = (ItemSquarePattern)best.getItem (0);
        return new Move (move.getItem (), move.getRow (), move.getColumn ());
      }
    }
    
    return new Move ("UNKNOWN", 0, 0);
  }
  
  

  

  /** 
   * Clear the STM and LTM of this {@link #this} model.
   */
  //TODO: Time needs to be passed here however, there is a problem: when a new 
  //      experiment begins, it should have an initial time of 0 and if STM is 
  //      cleared (for example) a new empty list will be added at time 0.  
  //      However, since items in STM are handled using 
  //      jchrest.lib.HistoryTreeMap instances, the "put" method will fail since 
  //      adding the empty list at time 0 would be rewriting history (there will 
  //      be entries from the previous experiment at times >= 0).  The same is 
  //      true for other architecture components.  The best solution would be to 
  //      "carry-over" the current experiment time when a CHREST instance is 
  //      moved from one experiment to the other.  Essentially, a CHREST model 
  //      should have its own clock that starts when it is placed in the first 
  //      experiment and is never reset.  Alternatively, a CHREST model should 
  //      only ever exist in one experiment and this function should be removed.
  public void clear () {
    this.clearHistory(); 
    this.setClocks(0);
    
    for(Modality modality : Modality.values()){
      try {
        Field ltmModalityRoot = Chrest.class.getDeclaredField("_" + modality.toString().toLowerCase() + "Ltm");
        Node ltmModalityRootNode = (Node)ltmModalityRoot.get(this);
        ltmModalityRootNode.clear();
        ltmModalityRoot.set(this, new Node(this, modality, 0));
        
        Stm stmModality = (Stm)Chrest.class.getDeclaredField("_" + modality.toString().toLowerCase() + "Stm").get(this);
        stmModality.clear(0);
      } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
        Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    
    this._nextLtmNodeReference = 0;
    _experimentsLocatedInNames.clear();
    this._engagedInExperiment = false;
    setChanged ();
    if (!_frozen) notifyObservers ();
  }

  /** 
   * Write model to given Writer object in VNA format
   */
  public void writeModelAsVna (Writer writer, int time) throws IOException {
    writer.write ("*Node data\n\"ID\", \"contents\"\n");
    _visualLtm.writeNodeAsVna (writer, time);
    writer.write ("*Tie data\nFROM TO\n");
    _visualLtm.writeLinksAsVna (writer, time);
  }

  /** 
   * Write model semantic links to given Writer object in VNA format
   */
  public void writeModelSemanticLinksAsVna (Writer writer, int time) throws IOException {
    writer.write ("*Node data\n\"ID\", \"contents\"\n");
    _visualLtm.writeNodeAsVna (writer, time);
    writer.write ("*Tie data\nFROM TO\n");
    _visualLtm.writeSemanticLinksAsVna (writer, time);
  }

  public void setDefaultAlpha (double alpha) {
    _emotionAssociator.setDefaultAlpha (alpha);
  }

  /**
   * Accessor for Emotion Associator.
   */
  public EmotionAssociator getEmotionAssociator () {
    return _emotionAssociator;
  }

  /**
   * Propagate emotion across all the given STMs.
   */
  public void emoteAndPropagateAcrossModalities (Object stmsobject, int time) {
    Stm[] stms = (Stm[]) stmsobject;
    _emotionAssociator.emoteAndPropagateAcrossModalities (stms, time);
  }

  /**
   * Attach given emotion to top item in STM, if present.
   */
  public void assignEmotionToCurrentItem (Stm stm, Emotion emotion, int time) {
    if (stm.getCount(time) == 0) {
      return;  // STM empty, so nothing to be done
    }
    _emotionAssociator.setRWEmotion (stm.getItem(0, time), emotion);
  }

  /** 
   * Accessor for the emotion associated with the topmost item in STM.
   */
  public Emotion getCurrentEmotion (Stm stm, int time) {
    if (stm.getCount (time) == 0) {
      return null;
    } else {
      return _emotionAssociator.getRWEmotion (stm.getItem (0, time));
    }
  }

  public Emotion getCurrentFollowedByEmotion (Stm stm, int time) {
    if (stm.getCount (time) == 0) {
      return null;
    } else {
      Node followed_by = stm.getItem(0, time).getAssociatedNode (time);
      if (followed_by == null) {
        return null;
      } else {
        return _emotionAssociator.getRWEmotion (followed_by);
      }
    }
  }

  /**
   * Returns the string value of a CHREST instance's _reinforcementLearningTheory
   * variable.
   * 
   * @return 
   */
  public String getReinforcementLearningTheory(){
    if(_reinforcementLearningTheory == null){
      return "null";
    }
    else{
      return _reinforcementLearningTheory.toString();
    }
  }
  
  /**
   * Sets the value of the CHREST instance's _reinforcementLearningTheory 
   * variable to the theory parameter iff _reinforcementLearningTheory is null
   * and if the theory specified is a declared 
   * ReinforcementLearning.ReinforcementLearningTheories constant.
   * This means that a CHREST instance's reinforcement learning theory can only
   * be set once to a theory supported by CHREST.
   * 
   * @param theorySpecified
   */
  public void setReinforcementLearningTheory(ReinforcementLearningTheories theorySpecified){
    if(_reinforcementLearningTheory == null){
      ReinforcementLearningTheories[] theories = ReinforcementLearning.getReinforcementLearningTheories();
      for(ReinforcementLearningTheories theory : theories){
        if(theorySpecified.equals(theory)){
          _reinforcementLearningTheory = theory;
          break;
        }
      }
    }
  }
  
  
  
  public int getCognitionClock(){
    return this._cognitionClock;
  }
  
  /**
   * Returns all instance variables ending with "Clock" for this {@link #this}
   * instance.
   * 
   * @return 
   */
  private ArrayList<Field> getClockInstanceVariables(){
    ArrayList<Field> clockInstanceVariables = new ArrayList<>();
  
    for(Field field : Chrest.class.getDeclaredFields()){
      
      //Store the name of the field since it may be used twice.
      String fieldName = field.getName();
      
      //Check for clock instance variable.
      if(fieldName.endsWith("Clock")){
        clockInstanceVariables.add(field);
      }
    }
    
    return clockInstanceVariables;
  }
  
  /**
   * Returns the value of the clock with the maximum value 
   * @return 
   */
  public int getMaximumClockValue(){
    ArrayList<Integer> clockValues = new ArrayList<>();
    for(Field field : this.getClockInstanceVariables()){
      
      //This field is a clock instance variable so get its current value and
      //check to see if the value's type is Node.  If so, continue.
      try {
        Object fieldValue = field.get(this);
        if(fieldValue instanceof Integer){
          clockValues.add((int)fieldValue);
        }
      } catch (IllegalArgumentException | IllegalAccessException ex) {
        Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    
    return Collections.max(clockValues);
  }
  
  /**
   * Sets all of this {@link #this} instance's clock variables to the time 
   * specified.
   * 
   * @param time 
   */
  public void setClocks(int time){
    for(Field field : this.getClockInstanceVariables()){
      try {
        field.setInt(this, time);
      } catch (IllegalArgumentException | IllegalAccessException ex) {
        Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
  
  public int getTimeToUpdateStm() {
    return _timeToUpdateStm;
  }
  
  /****************************************************************************/
  /****************************************************************************/
  /*********************** VISUAL-SPATIAL FIELD METHODS ***********************/
  /****************************************************************************/
  /****************************************************************************/
  
  /**
   * @param time
   * @return
   */
  public Integer getRecognisedVisualSpatialObjectLifespan(int time){
    return this._recognisedVisualSpatialObjectLifespan;
  }
  
  /**
   * @param time
   * @return 
   */
  public Integer getUnrecognisedVisualSpatialObjectLifespan(int time){
    return this._unrecognisedVisualSpatialObjectLifespan;
  }
  
  /**
   * @param time
   * @return
   */
  public Integer getVisualSpatialFieldPhysicalObjectEncodingTime(int time){
    return this._visualSpatialFieldPhysicalObjectEncodingTime;
  }
  
  /**
   * @param time
   * @return 
   */
  public Integer getVisualSpatialFieldEmptySquareEncodingTime(int time){
    return this._visualSpatialFieldEmptySquareEncodingTime;
  }
  
  /**
   * @param time
   * @return 
   */
  public Integer getVisualSpatialFieldAccessTime(int time){
    return this._visualSpatialFieldAccessTime;
  }
  
  /**
   * @param time
   * @return
   */
  public Integer getVisualSpatialFieldObjectMovementTime(int time){
    return this._visualSpatialFieldObjectMovementTime;
  }
  
  /**
   * Sets the lifespan for any new, recognised {@link 
   * jchrest.architecture.VisualSpatialFieldObject}s.
   * 
   * @param lifespan
   */
  public void setLifespanForRecognisedVisualSpatialObjects(int lifespan){
    this._recognisedVisualSpatialObjectLifespan = lifespan;
  }
  
  /**
   * Sets the lifespan for any new, unrecognised {@link 
   * jchrest.architecture.VisualSpatialFieldObject}s.
   * 
   * @param lifespan
   */
  public void setLifespanForUnrecognisedVisualSpatialObjects(int lifespan){
    this._unrecognisedVisualSpatialObjectLifespan = lifespan;
  }
  
  /**
   * Sets the encoding time for any new {@link 
   * jchrest.lib.VisualSpatialFieldObject}s that represent physical {@link 
   * jchrest.lib.SceneObject}s, i.e. non-empty and non-blind {@link 
   * jchrest.lib.VisualSpatialFieldObject}s.
   * 
   * @param encodingTime
   */
  public void setVisualSpatialFieldPhysicalObjectEncodingTime(int encodingTime){
    this._visualSpatialFieldPhysicalObjectEncodingTime = encodingTime;
  }
  
  /**
   * Sets the encoding time for any new {@link 
   * jchrest.lib.VisualSpatialFieldObject}s that represent empty squares in a 
   * {@link jchrest.lib.Scene}.
   * 
   * @param encodingTime
   */
  public void setVisualSpatialFieldEmptySquareEncodingTime(int encodingTime){
    this._visualSpatialFieldEmptySquareEncodingTime = encodingTime;
  }
  
  /**
   * Sets the base time for accessing a {@link 
   * jchrest.architecture.VisualSpatialField} associated with {@link #this}.
   * 
   * @param accessTime
   */
  public void setVisualSpatialFieldAccessTime(int accessTime){
    this._visualSpatialFieldAccessTime = accessTime;
  }
  
  /**
   * Set the time to move a {@link 
   * jchrest.architecture.VisualSpatialFieldObject} on a {@link 
   * jchrest.architecture.VisualSpatialField} associated with {@link #this}.
   * 
   * @param movementTime
   */
  public void setVisualSpatialFieldObjectMovementTime(int movementTime){
    this._visualSpatialFieldObjectMovementTime = movementTime;
  }
  
  
  /**
   * Attempts to create and associate a new {@link 
   * jchrest.architecture.VisualSpatialField} with this {@link #this} model.
   * If the {@link jchrest.architecture.VisualSpatialField} is successfully
   * created, it will be added to the database of {@link 
   * jchrest.architecture.VisualSpatialField}s associated with this {@link 
   * #this} model at the time its instantiation is complete i.e. the sum of:
   * <ul>
   *  <li>The time this function is invoked</li>
   *  <li>
   *    The time taken to scan the {@link jchrest.lib.Scene} to encode into
   *    the {@link jchrest.architecture.VisualSpatialField} to be created.
   *  </li>
   *  <li>
   *    The time taken to access the {@link 
   *    jchrest.architecture.VisualSpatialField} so it can be instantiated.
   *  </li>
   *  <li>
   *    The time taken to encode any empty squares and {@link 
   *    jchrest.lib.SceneObject}s in the {@link jchrest.lib.Scene} to encode 
   *    into the {@link jchrest.architecture.VisualSpatialField} to be created.
   *  </li>
   * </ul>
   * 
   * @param sceneToEncode
   * 
   * @param timeSceneToEncodeCanBeSeenUntil  The time that this {@link #this} 
   * model can no longer "see" the {@link jchrest.lib.Scene} to be encoded into
   * the {@link jchrest.architecture.VisualSpatialField} to be created.  If 
   * there is no time limit on how long the {@link jchrest.lib.Scene} can be 
   * "seen" for then pass null. 
   * 
   * @param objectEncodingTime How long it takes to encode an object in the 
   * {@link jchrest.architecture.VisualSpatialField} to be created.
   * 
   * @param emptySquareEncodingTime How long it takes to encode an empty square
   * in the {@link jchrest.architecture.VisualSpatialField} to be created.
   * 
   * @param accessTime How long it takes to access the {@link 
   * jchrest.architecture.VisualSpatialField} to be created.
   * 
   * @param objectMovementTime How long it takes to move a {@link 
   * jchrest.architecture.VisualSpatialFieldObject} irrespective of the 
   * "distance" moved in the {@link jchrest.architecture.VisualSpatialField} to 
   * be created.
   * 
   * @param lifespanForRecognisedObjects How long {@link 
   * jchrest.architecture.VisualSpatialFieldObject}s will exist for after being
   * recognised in the {@link jchrest.architecture.VisualSpatialField} to be 
   * created.
   * 
   * @param lifespanForUnrecognisedObjects How long {@link 
   * jchrest.architecture.VisualSpatialFieldObject}s will exist for if they are
   * not recognised in the {@link jchrest.architecture.VisualSpatialField} to be 
   * created.
   * 
   * @param numberFixations The number of fixations to be made by the {@link 
   * jchrest.architecture.Perceiver} associated with this {@link #this} model
   * when scanning the {@link jchrest.lib.Scene} to encode into the {@link 
   * jchrest.architecture.VisualSpatialField} to be created.
   * 
   * @param time The time this function is invoked.
   * 
   * @param encodeGhostObjects Whether or not to encode {@link 
   * jchrest.architecture.SceneObject}s that do not exist in the
   * {@link jchrest.lib.Scene} to encode into the {@link 
   * jchrest.architecture.VisualSpatialField} to be created but are recognised 
   * by virtue of other {@link jchrest.architecture.SceneObject}s that exist and 
   * are recognised in the {@link jchrest.lib.Scene} to encode when the {@link 
   * jchrest.lib.Scene} to encode is scanned during creation of the {@link 
   * jchrest.architecture.VisualSpatialField} to be created.
   * 
   * @param debug Set to true to output debug messages to 
   * {@link java.lang.System#out}.
   * 
   * @return {@link java.lang.Boolean#FALSE} if any of the following conditions
   * are true, {@link java.lang.Boolean#TRUE} otherwise:
   * <ul>
   *  <li>
   *    This {@link #this} model's perceptual resources are not free at the time 
   *    this function is invoked.
   *  </li>
   *  <li>
   *    The {@link jchrest.lib.Scene} passed is entirely blind.
   *  </li>
   *  <li>
   *    Two {@link jchrest.lib.SceneObject}s in the {@link jchrest.lib.Scene}  
   *    passed have the same identifier.  This means that it will be impossible
   *    to identify either {@link jchrest.lib.SceneObject} in the {@link 
   *    jchrest.architecture.VisualSpatialField} to be created if they are to be 
   *    moved at any point in the future.
   *  </li>
   * </ul>
   */
  public boolean createNewVisualSpatialField(
    Scene sceneToEncode,
    Integer timeSceneToEncodeCanBeSeenUntil,
    int objectEncodingTime, 
    int emptySquareEncodingTime, 
    int accessTime, 
    int objectMovementTime, 
    int lifespanForRecognisedObjects, 
    int lifespanForUnrecognisedObjects, 
    int numberFixations, 
    int time,
    boolean encodeGhostObjects,
    boolean debug
  ){
    
    //Attention and perceptual resources must be free to start constructing a 
    //visual-spatial field.
    if(this._attentionClock < time && this._perceiverClock < time){
        
      /******************************************/
      /***** CHECK FOR ENTIRELY BLIND SCENE *****/
      /******************************************/

      //If the scene to encode is entirely blind, the constructor will hang when 
      //the scene to encode is scanned for recognised chunks below so this check 
      //prevents this from happening.
      if(debug) System.out.println("- Checking if the scene to encode is entirely blind...");

      //Create a boolean variable that is only set to true if a non-blind object
      //exists in the scene to encode.
      boolean realityIsBlind = true;

      //Check sceneToEncode for a non-blind object that is not the scene creator.
      for(int col = 0; col < sceneToEncode.getWidth() && realityIsBlind; col++){
        for(int row = 0; row < sceneToEncode.getHeight() && realityIsBlind; row++){
          String objectClass = sceneToEncode.getSquareContents(col, row).getObjectClass();
          if(
            !objectClass.equals(Scene.getBlindSquareToken()) &&
            !objectClass.equals(Scene.getCreatorToken())
          ){
            if(debug) System.out.println("   - Col " + col + ", row " + row + " contains an object with class '" + objectClass + "'.");
            realityIsBlind = false;
            break;
          }
        }
      }

      if(!realityIsBlind){
        if(debug) if(debug) System.out.println("- Scene to encode isn't entirely blind");

        VisualSpatialField visualSpatialField = new VisualSpatialField(this, sceneToEncode, time);

        //Encode the objects here since getting and setting STM info needs to
        //be interspersed.
        

      }
        
////        VisualSpatialField visualSpatialField = new VisualSpatialField(
////          this,
////          sceneToEncode,
////          timeSceneToEncodeCanBeSeenUntil,
////          objectEncodingTime,
////          emptySquareEncodingTime,
////          accessTime,
////          objectMovementTime,
////          lifespanForRecognisedObjects,
////          lifespanForUnrecognisedObjects,
////          numberFixations,
////          time,
////          encodeGhostObjects,
////          debug
////        );
////      
////        int instantiationCompleteTime = visualSpatialField.getTimeInstantiationComplete();
//        this._attentionClock = instantiationCompleteTime;
//        this._visualSpatialFields.put(instantiationCompleteTime, visualSpatialField);
//        return true;
//      }
//      
//      //Perceptual resources not free
//      return false;
//    } catch (VisualSpatialFieldException ex) {
//      Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
//      return false;
    }
    
    return false;
  }
  
  /**
   * Moves {@link jchrest.lib.VisualSpatialFieldObject}s on the relevant 
   * {@link jchrest.architecture.VisualSpatialField} according to the sequence 
   * of moves specified.  {@link jchrest.lib.VisualSpatialFieldObject} movement 
   * can only occur if the attention of this {@link #this} model is free.  
   * 
   * If all moves are successful, the attention clock of this {@link #this} 
   * model will be set to the time this function is invoked plus the time taken
   * to access the relevant {@link jchrest.architecture.VisualSpatialField} plus 
   * the product of the number of moves performed multiplied by the time 
   * specified to move an object in the relevant {@link 
   * jchrest.architecture.VisualSpatialField}.
   * 
   * This method does not constrain the number of squares moved by a {@link 
   * jchrest.lib.VisualSpatialFieldObject} in a {@link 
   * jchrest.architecture.VisualSpatialField}.  Essentially, it takes the same 
   * amount of time to move a {@link jchrest.lib.VisualSpatialFieldObject} 
   * across 5 squares in a {@link jchrest.architecture.VisualSpatialField} as it 
   * does to move it across one.  If movements need to be time-constrained in 
   * this way then these constraints should be reflected in the moves specified.
   * 
   * Note that if a {@link jchrest.lib.VisualSpatialFieldObject} is moved to 
   * coordinates in the {@link jchrest.architecture.VisualSpatialField} 
   * that are already occupied then the two {@link 
   * jchrest.lib.VisualSpatialFieldObject}s will co-exist on the coordinates.
   * 
   * @param moveSequences A 2D {@link java.util.ArrayList} whose first dimension 
   * elements should contain {@link java.util.ArrayList}s of {@link 
   * jchrest.lib.ItemSquarePattern} instances that prescribe a sequence of moves 
   * for one {@link jchrest.lib.VisualSpatialFieldObject} using zero-indexed,
   * {@link jchrest.architecture.VisualSpatialField} coordinates rather than 
   * coordinates used in the external domain or relative to the "mover".  It is 
   * <b>imperative</b> that {@link jchrest.lib.VisualSpatialFieldObject}s to be 
   * moved are identified using their unique identifier (see {@link 
   * jchrest.lib.VisualSpatialFieldObject#getIdentifier()}) rather than their 
   * object class (see {@link 
   * jchrest.lib.VisualSpatialFieldObject#getObjectClass()}). For example, if 
   * two {@link jchrest.lib.VisualSpatialFieldObject}s have the same object 
   * class, A, but have unique identifiers, 0 and 1, and both are to be moved, 0 
   * before 1, then the {@link java.util.ArrayList} passed should specify: 
   * <pre>
   * [ <i>First dimension {@link java.util.ArrayList}</i>
   *    [ <i>Second dimension {@link java.util.ArrayList}</i>
   *      [0 sourceX sourceY], <i>{@link jchrest.lib.ItemSquarePattern}</i>
   *      [0 destinationX desitinationY] <i>{@link jchrest.lib.ItemSquarePattern}</i>
   *    ],
   *    [ <i>Second dimension {@link java.util.ArrayList}</i>
   *      [1 sourceX sourceY], <i>{@link jchrest.lib.ItemSquarePattern}</i>
   *      [1 desitinationX destinationY] <i>{@link jchrest.lib.ItemSquarePattern}</i>
   *    ]
   * ]
   * <pre/>
   * 
   * @param time The time (in milliseconds) when this function is invoked.
   * @param debug
   * 
   * @return {@link java.lang.Boolean#TRUE} if the attention resource of this
   * {@link #this} model is free at the time the function is invoked and a 
   * {@link jchrest.lib.VisualSpatialFieldException} is not thrown, {@link 
   * java.lang.Boolean#FALSE} otherwise.
   * 
   * @throws jchrest.lib.VisualSpatialFieldException If any of the moves 
   * specified cause any of the following apply to the moves specified:
   * <ol type="1">
   *  <li>
   *    More than one object is moved within the same sequence; object movement 
   *    should be strictly serial.
   *  </li>
   *  <li>
   *    The initial {@link jchrest.lib.ItemSquarePattern} in a move sequence 
   *    does not correctly identify where the 
   *    {@link jchrest.lib.VisualSpatialFieldObject} is located in this 
   *    {@link #this}.  If the {@link jchrest.lib.VisualSpatialFieldObject} has 
   *    previously been moved in this {@link #this}, the initial location should 
   *    be its current location in this {@link #this}.
   *  </li>
   *  <li>
   *    Only the initial location of a 
   *    {@link jchrest.lib.VisualSpatialFieldObject} is specified.
   *  </li>
   * </ol>
   */
  public boolean moveVisualSpatialFieldObjects(ArrayList<ArrayList<ItemSquarePattern>> moveSequences, int time, boolean debug) throws VisualSpatialFieldException{
//    if(this._attentionClock < time){
//      
//      try{
//        this._attentionClock = this.getVisualSpatialFields().floorEntry(time).getValue().moveObjects(moveSequences, time, debug);
//        return true;
//      }
//      catch(VisualSpatialFieldException e){
//        return false;
//      }
//    }
    
    return false;
  }
  
  /**
   * Returns this {@link #this} database of 
   * {@link jchrest.architecture.VisualSpatialField}s.
   * 
   * @return 
   */
  public TreeMap<Integer,VisualSpatialField> getVisualSpatialFields(){
    return this._visualSpatialFields;
  }
}
