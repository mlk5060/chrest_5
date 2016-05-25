// Copyright (c) 2012, Peter C. R. Lane
// with contributions on the emotions code by Marvin Schiller.
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.architecture;

import jchrest.lib.VisualSpatialFieldObject;
import jchrest.domainSpecifics.Scene;
import jchrest.domainSpecifics.generic.GenericDomain;
import jchrest.domainSpecifics.DomainSpecifics;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.TreeMap;
import jchrest.database.DatabaseInterface;
import jchrest.domainSpecifics.Fixation;
import jchrest.domainSpecifics.SceneObject;
import jchrest.gui.experiments.Experiment;
import jchrest.lib.*;
import jchrest.lib.ReinforcementLearning.Theory;
import org.uncommons.watchmaker.framework.EvaluatedCandidate;
import org.uncommons.watchmaker.framework.selection.RouletteWheelSelection;

/**
 * A CHREST model.
 * 
 * All times are specified in milliseconds.
 * 
 * @author Peter C. R. Lane
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
//TODO: Implement template time variables (how long to create a template, fill 
//      slots etc.
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
  private int _timeToAccessVisualSpatialField = 100; //From "Mental Imagery and Chunks" by Gobet and Waters
  private int _timeToEncodeRecognisedSceneObjectAsVisualSpatialFieldObject = 5;
  private int _timeToEncodeUnrecognisedNonEmptySquareSceneObjectAsVisualSpatialFieldObject = 25; 
  private int _timeToEncodeUnrecognisedEmptySquareSceneObjectAsVisualSpatialFieldObject = 10; 
  private int _timeToProcessUnrecognisedSceneObjectDuringVisualSpatialFieldConstruction = 10;
  private int _recognisedVisualSpatialFieldObjectLifespan = 10000; 
  private int _unrecognisedVisualSpatialFieldObjectLifespan = 8000;
  private int _timeToMoveVisualSpatialFieldObject = 50;  //From "Mental Imagery and Chunks" by Gobet and Waters
  private int _timeToCheckNodeForProductions = 40;
  
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
  
  /******************************/
  /**** Perceptual variables ****/
  /******************************/
  
  //Used for scheduling and tracking the next fixation point that is to be made
  //by this model, if applicable.
  private HistoryTreeMap _fixationsScheduled = new HistoryTreeMap();
  
  //Stipulates whether object locations in a Scene will have their coordinates 
  //specified relative to the agent equipped with CHREST's location in the Scene 
  //or not.  Can not be modified when set since its not currently possible to 
  //tell whether stored visual information is relative to an agent's location or 
  //not, e.g. does <[T 1 1]> indicate that there is a "T" object on domain 
  //coordinates [1, 1] or that there is a "T" object on coordinates 1 square 
  //north and 1 square east of the agent equipped with CHREST.
  private final boolean _learnObjectLocationsRelativeToAgent;
  
  //Indicates whether the model is currently performing a Fixation set.
  private boolean _performingFixations = false;
  private int _fixationsAttemptedInCurrentSet = 0;
  
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
  
  private Theory _reinforcementLearningTheory = null; //Must be set explicitly using Chrest.setReinforcementLearningTheory();
  
  /****************************************/
  /**** Visual-Spatial Field variables ****/
  /****************************************/
  
  //Stores all VisualSpatialFieldObject identifiers that have been recognised in
  //the Fixation set currently being performed.
  private List<String> _recognisedVisualSpatialFieldObjectIdentifiers = new ArrayList();
  
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
   * Note that the domain for {@link #this} is set to be {@link 
   * jchrest.domainSpecifics.generic.GenericDomain} initially and should be
   * modified, if necessary, after {@link #this} has been constructed.
   * 
   * @param time 
   * @param learnObjectLocationsRelativeToAgent When the {@link 
   * jchrest.architecture.Perceiver} associated with {@link #this} generates new
   * {@link jchrest.lib.Modality#VISUAL} {@link jchrest.lib.ListPattern 
   * ListPatterns} from {@link jchrest.domainSpecifics.Scene Scenes} or when 
   * {@link jchrest.lib.ListPattern ListPatterns} stored in long-term memory are 
   * used to suggest new {@link jchrest.domainSpecifics.Fixation Fixations}, 
   * this variable is used to control whether the columns and rows of {@link 
   * jchrest.lib.Square Squares} that {@link jchrest.domainSpecifics.SceneObject 
   * SceneObjects} are located on are absolute or relative to the agent that
   * is equipped with {@link #this}.  Consider the following {@link 
   * jchrest.domainSpecifics.Scene} ("SELF" denotes the agent equipped with
   * {@link #this}, "OO" denotes a {@link jchrest.domainSpecifics.SceneObject}):
   * 
   * Row
   *    |----|----|----|
   *  2 |    |    |    |
   *    |----|----|----|
   *  1 |    |SELF|    |
   *    |----|----|----|
   *  0 |    |    | OO |
   *    |----|----|----|
   *      0     1    2    Col
   * 
   * If this variable is set to {@link java.lang.Boolean#TRUE} and the agent is
   * to learn the location of "OO", the {@link jchrest.lib.ListPattern} 
   * generated and (potentially) memorised would be: <[OO, 1, -1]> ("OO" is 1 
   * square east and 1 square south of the agent's location). If this 
   * variable were set to {@link java.lang.Boolean#FALSE}, the {@link 
   * jchrest.lib.ListPattern} generated and (potentially) memorised would be: 
   * <[OO, 2, 0]>.  Note that, if the domain-coordinates were not zero-indexed,
   * the coordinates used would change, i.e. the zero-indexed {@link 
   * jchrest.domainSpecifics.Scene} coordinates are never used in this case.
   * 
   * <b>CAVEATS:</b> this variable can not be changed after being set and if
   * set to {@link java.lang.Boolean#TRUE}, all {@link 
   * jchrest.domainSpecifics.Scene Scenes} used by the {@link 
   * jchrest.architecture.Perceiver} associated with {@link #this} must have the
   * agent equipped with {@link #this} identified (see {@link 
   * jchrest.domainSpecifics.Scene#getCreatorToken()).
   */
  public Chrest (int time, boolean learnObjectLocationsRelativeToAgent) {
    
    /*******************************/
    /**** Simple variable setup ****/
    /*******************************/
    
    //Set creation time and resource clocks.
    this._creationTime = time;
    this._domainSpecifics = new GenericDomain(this, 10, 3);
    this._learnObjectLocationsRelativeToAgent = learnObjectLocationsRelativeToAgent;
    
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
    this._perceiver = new Perceiver(this, time);
    this._visualSpatialFields.put(time - 1, null);
    
    //Add first entry at time - 1 so that, if a fixation is made at the time 
    //this CHREST model is created, the HistoryTreeMap can be updated correctly.
    this._fixationsScheduled.put(time - 1, new ArrayList());
    
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
  
  /***************************/
  /**** DEBUGGING METHODS ****/
  /***************************/
  
  public void printDebugStatement(String statement){
    if(this._debug) this._debugOutput.println(statement);
  }
    
  public void turnOnDebugging(){
    this._debug = true;
  }
  
  public void turnOffDebugging(){
    this._debug = false;
  }
  
  public boolean debug(){
    return this._debug;
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
  
  public int getCognitionClock(){
    return this._cognitionClock;
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
  
  public Integer getRecognisedVisualSpatialFieldObjectLifespan(){
    return this._recognisedVisualSpatialFieldObjectLifespan;
  }
  
  public int getReinforceProductionTime(){
    return _reinforceProductionTime;
  }
  
  public float getRho(){
    return _rho;
  }
  
  public Integer getTimeToAccessVisualSpatialField(){
    return this._timeToAccessVisualSpatialField;
  }
  
  public int getTimeToCheckNodeForProductions(){
    return this._timeToCheckNodeForProductions;
  }
  
  public int getTimeToCreateNamingLink(){
    return this._namingLinkCreationTime;
  }
  
  public int getTimeToCreateSemanticLink(){
    return this._semanticLinkCreationTime;
  }
  
  public int getTimeToEncodeRecognisedSceneObjectAsVisualSpatialFieldObject(){
    return this._timeToEncodeRecognisedSceneObjectAsVisualSpatialFieldObject;
  }
  
  public Integer getTimeToEncodeUnrecognisedEmptySquareSceneObjectAsVisualSpatialFieldObject(){
    return this._timeToEncodeUnrecognisedEmptySquareSceneObjectAsVisualSpatialFieldObject;
  }
  
  public Integer getTimeToEncodeUnrecognisedNonEmptySquareSceneObjectAsVisualSpatialFieldObject(){
    return this._timeToEncodeUnrecognisedNonEmptySquareSceneObjectAsVisualSpatialFieldObject;
  }
  
  public Integer getTimeToMoveVisualSpatialFieldObject(){
    return this._timeToMoveVisualSpatialFieldObject;
  }
  
  public int getTimeToProcessUnrecognisedSceneObjectDuringVisualSpatialFieldConstruction(){
    return _timeToProcessUnrecognisedSceneObjectDuringVisualSpatialFieldConstruction;
  }

  public int getTimeToRetrieveItemFromStm(){
    return this._timeToRetrieveItemFromStm;
  }

  public Integer getUnrecognisedVisualSpatialFieldObjectLifespan(){
    return this._unrecognisedVisualSpatialFieldObjectLifespan;
  }
  
  public int getTimeToUpdateStm() {
    return _timeToUpdateStm;
  }
  
  /**
   * @return The {@link jchrest.architecture.VisualSpatialField 
   * VisualSpatialFields} constructed by {@link #this}.
   */
  public TreeMap<Integer,VisualSpatialField> getVisualSpatialFields(){
    return this._visualSpatialFields;
  }
  
  void incrementNextNodeReference(){
    this._nextLtmNodeReference++;
  }
  
  public boolean isLearningObjectLocationsRelativeToAgent(){
    return _learnObjectLocationsRelativeToAgent;
  }
  
  public boolean isAttentionFree(int time){
    return this._attentionClock <= time;
  }
    
  public boolean isCognitionFree(int time){
    return this._cognitionClock <= time;
  }
  
  public boolean isPerceiverFree(int time){
    return this._perceiverClock <= time;
  }
  
  public void setDomain (DomainSpecifics domain) {
    _domainSpecifics = domain;
  }
  
  public void setAddProductionTime (int time) {
    this._addProductionTime = time;
  }
  
  public void setTimeToCheckNodeForProductions(int time){
    this._timeToCheckNodeForProductions = time;
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
  
  public void setRecognisedVisualSpatialFieldObjectLifespan(int lifespan){
    this._recognisedVisualSpatialFieldObjectLifespan = lifespan;
  }
  
  public void setTimeToEncodeRecognisedSceneObjectAsVisualSpatialFieldObject(int time){
    this._timeToEncodeRecognisedSceneObjectAsVisualSpatialFieldObject = time;
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
  
  /**
   * @param time The base time for accessing a {@link 
   * jchrest.architecture.VisualSpatialField} associated with {@link #this}.
   */
  public void setTimeToAccessVisualSpatialField(int time){
    this._timeToAccessVisualSpatialField = time;
  }
  
  public void setTimeToCreateNamingLink(int timeToCreateNamingLink) {
    this._namingLinkCreationTime = timeToCreateNamingLink;
  }
  
  public void setTimeToCreateSemanticLink(int timeToCreateSemanticLink) {
    this._semanticLinkCreationTime = timeToCreateSemanticLink;
  }
  
  /**
   * @param time The time taken to encode new {@link 
   * jchrest.domainSpecifics.SceneObject SceneObjects} that represent empty 
   * {@link jchrest.lib.Square Squares} in a {@link 
   * jchrest.domainSpecifics.Scene} as {@link 
   * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects}.
   */
  public void setTimeToEncodeUnrecognisedEmptySquareSceneObjectAsVisualSpatialFieldObject(int time){
    this._timeToEncodeUnrecognisedEmptySquareSceneObjectAsVisualSpatialFieldObject = time;
  }
  
  /**
   * @param time The time taken to encode new {@link 
   * jchrest.domainSpecifics.SceneObject SceneObjects} that do not represent 
   * empty {@link jchrest.lib.Square Squares} in a {@link 
   * jchrest.domainSpecifics.Scene} as {@link 
   * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects}.
   */
  public void setTimeToEncodeUnrecognisedNonEmptySquareSceneObjectAsVisualSpatialFieldObject(int time){
    this._timeToEncodeUnrecognisedNonEmptySquareSceneObjectAsVisualSpatialFieldObject = time;
  }
  
  /**
   * @param movementTime The time taken to move a {@link 
   * jchrest.architecture.VisualSpatialFieldObject} on a {@link 
   * jchrest.architecture.VisualSpatialField} associated with {@link #this}.
   */
  public void setTimeToMoveVisualSpatialFieldObject(int time){
    this._timeToMoveVisualSpatialFieldObject = time;
  }
  
  /**
   * @param time The base time taken to process a {@link 
   * jchrest.domainSpecifics.SceneObject} during {@link 
   * jchrest.architecture.VisualSpatialField} construction (see {@link 
   * #this#constructVisualSpatialField(int)}, irrespective of whether the {@link 
   * jchrest.domainSpecifics.SceneObject} is encoded as a {@link 
   * jchrest.lib.VisualSpatialFieldObject}.
   */
  public void setTimeToProcessUnrecognisedSceneObjectDuringVisualSpatialFieldConstruction(int time){
    this._timeToProcessUnrecognisedSceneObjectDuringVisualSpatialFieldConstruction = time;
  }

  public void setTimeToUpdateStm(int timeToUpdateStm){
    this._timeToUpdateStm = timeToUpdateStm;
  }
  
  public void setTimeToRetrieveItemFromStm(int timeToRetrieveItemFromStm){
    this._timeToRetrieveItemFromStm = timeToRetrieveItemFromStm;
  }
  
  public void setUnrecognisedVisualSpatialFieldObjectLifespan(int lifespan){
    this._unrecognisedVisualSpatialFieldObjectLifespan = lifespan;
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
      List<Link> nodeChildren = node.getChildren(time);
      if(nodeChildren != null){
        for (Link link : node.getChildren(time)) {
          this.findDepth(link.getChildNode(), 1, depths, time);
        }
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
  
  private void findDepth (Node node, int currentDepth, List<Integer> depths, int time) {
    List<Link> children = node.getChildren(time);
    
    if(children == null || children.isEmpty()) {
      depths.add(currentDepth);
    } 
    else {
      for (Link link : children) {
        this.findDepth(link.getChildNode(), currentDepth + 1, depths, time);
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
   * 
   * @return See return values for {@link 
   * jchrest.architecture.Node#addChild(jchrest.lib.ListPattern, 
   * jchrest.architecture.Node, int, java.lang.String)}.
   */
  private boolean learnPrimitive (ListPattern pattern, int time) {
    this.printDebugStatement("===== Chrest.learnPrimitive() =====");
    this.printDebugStatement("- Attemtping to learn " + pattern.toString() + " as a primitive at time " + time);
    
    assert(pattern.isFinished () && pattern.size () == 1);

    ListPattern contents = pattern.clone();
    contents.setNotFinished();

    Node primitive = new Node(this, contents, new ListPattern(pattern.getModality()), time);

    this.printDebugStatement(
      "- Attempting to append new child Node with ref: " + primitive.getReference() + 
      " and image '" + primitive.getImage(time) + "' to the " + pattern.getModalityString() + 
      " root node by a link containing test: " + contents.toString() + "."
    );

    boolean result = this.getLtmModalityRootNode(pattern).addChild(contents, primitive, time, this.getCurrentExperimentName());
    
    this.printDebugStatement("Returning " + result);
    this.printDebugStatement("===== RETURN Chrest.learnPrimitive() =====");
    return result;
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
    boolean nodeIsTemplate = node.isTemplate (time);
    if(nodeIsTemplate) count += 1;

    List<Link> children = node.getChildren(time);
    if(children != null){
      for (Link link : children) {
        count += this.countTemplatesBelowNode(link.getChildNode(), count, time);
      }
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

    List<Link> children = node.getChildren(time);
    if(children != null){
      for (Link child : children) {
        this.getContentSizeCounts(child.getChildNode(), contentSizeCountsAndFrequencies, time);
      }
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
    ListPattern image = node.getImage(time);
    if(image != null){
      int size = image.size();
    
      if (sizesToFrequencies.containsKey (size)) {
        sizesToFrequencies.put (size, sizesToFrequencies.get(size) + 1);
      } else {
        sizesToFrequencies.put (size, 1);
      }
    }

    List<Link> children = node.getChildren(time);
    if(children != null){
      for (Link child : children) {
        this.getImageSizeCounts(child.getChildNode(), sizesToFrequencies, time);
      }
    }
  }
  
  /**
   * @return The sum of image sizes of the child {@link 
   * jchrest.architecture.Node}s and their child's {@link 
   * jchrest.architecture.Node}s etc. below the {@link 
   * jchrest.architecture.Node} specified at the time specified.
   */
  private int totalImageSize (Node node, int time) {
    int size = 0;
    ListPattern image = node.getImage(time);
    if(image != null){
      size = image.size();
    }
    
    List<Link> children = node.getChildren(time);
    if(children != null){
      for (Link link : children) {
        size += this.totalImageSize(link.getChildNode(), time);
      }
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
    int count = 0;
    
    HashMap<Node, Double> productions = node.getProductions(time);
    if(productions != null){
      count = productions.size();
      
      if(recurse){
        List<Link> children = node.getChildren(time);
        if(children != null){
          for(Link link : children){
            count += this.getProductionCount(link.getChildNode(), true, time);
          }
        }
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
    int semanticLinkCount = 0;
    
    List<Node> semanticLinks = node.getSemanticLinks(time);
    if(semanticLinks != null){
      semanticLinkCount = semanticLinks.size ();
    }
    
    if (semanticLinkCount > 0) { // do not count nodes with no semantic links
      if (semanticLinkCountsAndFrequencies.containsKey (semanticLinkCount)) {
        semanticLinkCountsAndFrequencies.put (semanticLinkCount, semanticLinkCountsAndFrequencies.get(semanticLinkCount) + 1);
      } else {
        semanticLinkCountsAndFrequencies.put (semanticLinkCount, 1);
      }
    }

    List<Link> children = node.getChildren(time);
    if(children != null){
      for (Link child : children) {
        this.getSemanticLinkCounts(child.getChildNode(), semanticLinkCountsAndFrequencies, time);
      }
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
          lastRowInserted.stream().forEach((rowData) -> {
            for(int col = 0; col < rowData.size(); col++){
              Object[] colData = rowData.get(col);
              Chrest.this._lastHistoryRowInserted.put((String)colData[0], colData[1]);
            }
          });

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
   * Sort the {@link jchrest.lib.ListPattern} provided through the long-term 
   * memory network of {@link #this} (see {@link #recognise(
   * jchrest.lib.ListPattern, int)), add the {@link jchrest.architecture.Node}
   * recognised to the relevant {@link jchrest.architecture.Stm} {@link 
   * jchrest.lib.Modality} and learn the {@link jchrest.lib.ListPattern} if 
   * the image of the {@link jchrest.architecture.Node} recognised does not 
   * exactly match the {@link jchrest.lib.ListPattern} provided.
   * 
   * @param pattern
   * @param time
   * 
   * @return 
   * <ul>
   *  <li>
   *    {@link jchrest.lib.Status#COGNITION_BUSY} if the cognitive resource of 
   *    {@link #this} is busy at the {@code time} specified.
   *  </li>
   *  <li>
   *    {@link jchrest.lib.Status#INPUT_ALREADY_LEARNED} if the {@code pattern}
   *    specified causes the recognition of a {@link jchrest.architecture.Node}
   *    from long-term memory that returns {@code pattern} when {@link 
   *    jchrest.architecture.Node#getImage(int)} is invoked on it.
   *  </li>
   *  <li>
   *    {@link jchrest.lib.Status#LEARNING_REFUSED} if {@link #this} randomly
   *    refuses to learn even if the {@code pattern} retrieves a {@link 
   *    jchrest.architecture.Node} from long-term memory that doesn't return 
   *    {@code pattern} when {@link jchrest.architecture.Node#getImage(int)} is 
   *    invoked on it.
   *  </li>
   *  <li>
   *    {@link jchrest.lib.Status#DISCRIMINATION_SUCCESSFUL} if discrimination 
   *    is attempted and is successful.
   *  </li>
   *  <li>
   *    {@link jchrest.lib.Status#DISCRIMINATION_FAILED} if discrimination is
   *    attempted but fails.
   *  </li>
   *  <li>
   *    {@link jchrest.lib.Status#FAMILIARISATION_SUCCESSFUL} if familiarisation 
   *    is attempted and is successful.
   *  </li>
   *  <li>
   *    {@link jchrest.lib.Status#FAMILIARISATION_FAILED} if familiarisation is
   *    attempted but fails.
   *  </li>
   * </ul>
   */
  public ChrestStatus recogniseAndLearn (ListPattern pattern, int time) { 
    this.printDebugStatement("===== Chrest.recogniseAndLearn() =====");
    this.printDebugStatement("- Recognising and learning " + pattern.toString() + " at time " + time);
    ChrestStatus result;
    
    //Try to recognise the pattern specified; will return null if the cognition
    //resource is not free.
    Node nodeRecognised = recognise(pattern, time, true);
    
    this.printDebugStatement(
      "- Recognition returned " + (nodeRecognised == null ? 
        "null" : 
        "node with reference " + nodeRecognised.getReference()
      )
    );
    
    // If the node recognised is != null, i.e. the cognition resource is free, 
    // continue.
    if(nodeRecognised != null){ 
      
      //Set current time to be equal to the cognitive clock since lerning should 
      //only continue after a node has been retrieved from LTM.
      this.printDebugStatement(
        "- A Node has been recognised, the current time will be " +
        "set to the current value of the cognition clock (" + 
        this._cognitionClock + ") since learning can only continue after LTM " +
        "retrieval is complete."
      );
      
      time = this._cognitionClock;
      
      //The model should only try to learn if the image of the recognised node
      //differs from pattern provided.
      ListPattern recognisedNodeImage = nodeRecognised.getImage(time);
      
      if(recognisedNodeImage.equals(pattern)){
        this.printDebugStatement(
          "- The image of the recognised Node (" + recognisedNodeImage.toString() + ") " +
          "matches the input pattern so the input has been learned therefore, " +
          "no learning will occur, exiting"
        );
        result = ChrestStatus.INPUT_ALREADY_LEARNED;
      }
      else if(Math.random() >= _rho){
        this.printDebugStatement("- The model randomly refused to learn, exiting");
        result = ChrestStatus.LEARNING_REFUSED;
      }
      else {
        this.printDebugStatement(
          "- The image of the recognised Node (" + recognisedNodeImage.toString() + ") " +
          "does not match the input pattern so the input has not been fully learned " +
          "and the model did not randomly refuse to learn, determining if discrimination " +
          "or familiarisation should occur."
        );
        this.printDebugStatement(
          "- If any of the following statements evaluate to true, discrimination " +
          "will occur.  Otherwise, familiarisation will occur." +
          "\n  ~ Is the Node recognised a root LTM Node: " + nodeRecognised.isRootNode() +
          "\n  ~ Does the image of the Node recognised mismatch the input pattern: " + !recognisedNodeImage.matches(pattern) +
          "\n  ~ Is the image of the Node recognised 'finished': " + recognisedNodeImage.isFinished()
        );

        if (
          nodeRecognised.isRootNode() || //i.e. pattern not recognised at all
          !recognisedNodeImage.matches(pattern) || //pattern recognised but image mismatched
          recognisedNodeImage.isFinished() //can't add to image (familiarisation not allowed)
        ){
          this.printDebugStatement("- Discrimination will occur.");
          result = this.discriminate(nodeRecognised, pattern, time);
        } else  { 
          this.printDebugStatement("- Familiarisation will occur.");
          result = this.familiarise(nodeRecognised, pattern, time);
        }
      }
    }
    else{
      result = ChrestStatus.COGNITION_BUSY;
      this.printDebugStatement(
        "- Recognition returned null so the cognitive resource isn't free; " +
        "neither recognition or learning can be performed, exiting."
      );
    }
    
    this.printDebugStatement("Returning " + result.name());
    this.printDebugStatement("===== RETURN Chrest.recogniseAndLearn() =====");
    return result;
  }
  
  /**  
   * Retrieves the {@link jchrest.architecture.Node} reached after sorting the 
   * {@link jchrest.lib.ListPattern} provided through the long-term memory of
   * {@link #this} vertically, then horizontally.
   * 
   * If a non-root {@link jchrest.architecture.Node} is retrieved, it is placed 
   * into the relevant {@link jchrest.architecture.Stm} {@link 
   * jchrest.lib.Modality} based on the result of invoking {@link 
   * jchrest.architecture.Node#getModality()} on the {@link 
   * jchrest.architecture.Node} retrieved.
   * 
   * Modifies the cognition and attention clocks.
   * 
   * @param pattern
   * @param time 
   * @param considerTimeAndAddRecognisedNodeToStm
   * 
   * @return If {@code considerTimeAndAddRecognisedNodeToStm} is set to {@link 
   * java.lang.Boolean#FALSE} or if its set to {@link java.lang.Boolean#TRUE}
   * and {@link jchrest.architecture.Chrest#isCognitionFree(int)} returns {@link 
   * java.lang.Boolean#TRUE} when {@code time} is passed as a parameter, the 
   * {@link jchrest.architecture.Node} reached (as explained in the parameter
   * description above) is returned.  This may be a root {@link 
   * jchrest.architecture.Node}.
   * <p>
   * If {@code considerTimeAndAddRecognisedNodeToStm} is set to {@link 
   * java.lang.Boolean#TRUE} and {@link 
   * jchrest.architecture.Chrest#isCognitionFree(int)} returns {@link 
   * java.lang.Boolean#FALSE} when {@code time} is passed as a parameter, {@code
   * null} is returned.
   */
  public Node recognise (ListPattern pattern, Integer time, Boolean considerTimeAndAddRecognisedNodeToStm) {
    this.printDebugStatement("===== Chrest.recognise() =====");
    this.printDebugStatement("- Time " + (considerTimeAndAddRecognisedNodeToStm ? 
      "will" : "will not") + " be considered and the node returned by the " +
      "recognition process performed to ascertain if learning should occur " + 
      (considerTimeAndAddRecognisedNodeToStm ? "will" : "will not") + " be " +
      "added to STM."
    );
    
    ////////////////////////////////////////////////////
    ///// CHECK AVAILABILITY OF COGNITIVE RESOURCE /////
    ////////////////////////////////////////////////////
    
    if(considerTimeAndAddRecognisedNodeToStm){
      this.printDebugStatement(
        "- Checking if cognition resource free (is the current value " + 
        "of the cognition clock (" + this._cognitionClock + ") <= the time " + 
        "this function was invoked (" + time + ")?"
      );
    }
    
    if(this.isCognitionFree(time) || !considerTimeAndAddRecognisedNodeToStm){
      
      if(considerTimeAndAddRecognisedNodeToStm) this.printDebugStatement("- Cognition resource free.");
      this.printDebugStatement("- Attempting to recognise " + pattern.toString() + ".");
      
      ////////////////////////////////////////
      ///// DETERMINE MODALITY TO SEARCH /////
      ////////////////////////////////////////
      
      Node currentNode = this.getLtmModalityRootNode(pattern);
      
      this.printDebugStatement(
        "  ~ Retrieved " + currentNode.getImage(time).getModalityString() + 
        " modality root node"
      );
      
      if(considerTimeAndAddRecognisedNodeToStm){
        this.printDebugStatement(
          "- Incrementing current time (" + time + ") by the time taken " +
          "to traverse a LTM link (" + this._ltmLinkTraversalTime + ")"
        );
        
        time += this._ltmLinkTraversalTime;
      }
      
      ///////////////////////////////////
      ///// TRAVERSE LTM VERTICALLY /////
      ///////////////////////////////////
      
      List<Link> currentNodeTestLinks = currentNode.getChildren(time);
      ListPattern sortedPattern = pattern;
      int linkToCheck = 0;

      while(currentNodeTestLinks != null && linkToCheck < currentNodeTestLinks.size()) {
        Link currentNodeTestLink = currentNodeTestLinks.get(linkToCheck);
        
        this.printDebugStatement(
          "- Checking if " + pattern.toString() + " passes test (" + 
          currentNodeTestLink.getTest().toString() + ") on link " + 
          linkToCheck + " from node " + currentNode.getReference() + "."
        );
        
        if (currentNodeTestLink.passes (sortedPattern)) { // descend a test link in network
          this.printDebugStatement("  ~ Test passed, descending the link to its child node");
          
          if(considerTimeAndAddRecognisedNodeToStm){
            this.printDebugStatement(
              "  ~ Incrementing the current time (" + time + ") by the time " +
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
           "  ~ Test not passed, checking the next test link of node " +
            currentNode.getReference() + "."
          );
          
          linkToCheck += 1;
        }
      }
      
      if(considerTimeAndAddRecognisedNodeToStm){
        this.printDebugStatement("- Cognition clock will be set to the current time (" + time + ").");
        this._cognitionClock = time;
      }
      
      /////////////////////////////////////
      ///// CHECK FOR NON-RECOGNITION /////
      /////////////////////////////////////
      
      this.printDebugStatement(
        "- Descended vertically through long-term memory network as far " + 
        "as possible.  If a non-root Node has been retrieved, recognition " +
        "will continue"
      );
      
      if(!currentNode.isRootNode()){
      
        this.printDebugStatement("- A non-root Node has been retrieved.");
        this.printDebugStatement(
          "-Searching horizontally through long-term memory for a more " +
          "informative node by searching the semantic links of node " + 
          currentNode.getReference()
        );
        
        /////////////////////////////////////
        ///// TRAVERSE LTM HORIZONTALLY /////
        /////////////////////////////////////
      
        // try to retrieve a more informative node in semantic links
        currentNode = this.searchSemanticLinks(currentNode, this._maximumSemanticLinkSearchDistance, time, false);
        this.printDebugStatement(
          "- Semantic link search retrieved node with reference " + 
          currentNode.getReference() + "."
        );
        
        //////////////////////
        ///// ADD TO STM /////
        //////////////////////
        
        if(considerTimeAndAddRecognisedNodeToStm){

          this.printDebugStatement(
            "- Current time will now be set to the value of the cognition " +
            "clock, i.e. the time semantic link search completed: " + 
            this._cognitionClock + ".  Adding node " + currentNode.getReference() + 
            " to STM."
          );

          time = this._cognitionClock;
          this.addToStm (currentNode, time);
        }
      }
      else{
        this.printDebugStatement("Root Node retrieved, exiting");
      }
      
      // return retrieved node
      this.printDebugStatement("- Returning node " + currentNode.getReference());
      this.printDebugStatement("===== RETURN =====");
      return currentNode;
    }
    else{
      if(considerTimeAndAddRecognisedNodeToStm){
        this.printDebugStatement("- Cognition resource not free, returning null");
      }

      this.printDebugStatement("===== RETURN =====");
      
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
      (considerTime && !this.isCognitionFree(time)) ||
      semanticSearchDistanceRemaining <= 0
    ){
      debugStatement = func + "Maximum semantic search distance has been reached (" + 
        (semanticSearchDistanceRemaining <= 0) + ")";
        
      if(considerTime){
        debugStatement += "or cognitive resource is not free (" + !this.isCognitionFree(time) + ")";
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
      List<Node> semanticLinks = node.getSemanticLinks(time);
      if(semanticLinks != null){
        for (Node comparisonNode : semanticLinks) {

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
      }

      this.printDebugStatement(func + "Returning node " + bestNode.getReference());
      this.printDebugStatement(func + "RETURN");

      return bestNode;
    }
  }
  
  /********************************/
  /**** Learning Functionality ****/
  /********************************/
  
  /**
   * Attempts to learn a production between the {@code vision} and {@code
   * action} specified.
   * 
   * To learn a production, the following statements must all evaluate to {@link 
   * java.lang.Boolean#TRUE}:
   * <ol type="1">
   *  <li>{@link #this} must exist at the {@code time} specified.</li>
   *  <li>
   *    The attention of {@link #this} must be free at the {@code time} 
   *    specified.
   *  </li>
   *  <li>
   *    The {@code vision} and {@code action} specified must return {@link 
   *    jchrest.lib.Modality#VISUAL} and {@link jchrest.lib.Modality#ACTION}
   *    respectively when {@link jchrest.lib.ListPattern#getModality()} is 
   *    invoked on them.
   *  </li>
   *  <li>
   *    The {@code vision} and {@code action} specified must be present
   *    in {@link jchrest.lib.Modality#VISUAL} and {@link 
   *    jchrest.lib.Modality#ACTION} {@link jchrest.architecture.Stm} at the
   *    {@code time} specified, i.e. a non-null and non-empty {@link 
   *    java.util.List} must be returned by {@link 
   *    jchrest.architecture.Chrest#searchStm(jchrest.lib.ListPattern, int)} 
   *    when the {@code vision} and {@code action} specified are passed as 
   *    parameters.
   *  </li>
   *  <li>
   *    The cognition of {@link #this} must be free after {@link 
   *    jchrest.lib.Modality#VISUAL} and {@link jchrest.lib.Modality#ACTION} 
   *    {@link jchrest.architecture.Stm} have been searched for the {@code 
   *    visualNode} and {@code actionNode} specified.
   *  </li>
   * </ol>
   * 
   * After invoking {@link 
   * jchrest.architecture.Chrest#searchStm(jchrest.lib.ListPattern, int)}, the
   * {@link jchrest.architecture.Node Nodes} selected for use in the production
   * to be learned may not return {@link java.lang.Boolean#TRUE} when {@link 
   * jchrest.architecture.Node#getContents()} is compared to the {@code vision} 
   * or {@code action} using {@link 
   * jchrest.lib.ListPattern#equals(java.lang.Object)}.  Thus, 
   * "over-generalisation" can occur in such scenarios.  For example, in 
   * Tileworld, the {@code vision} passed may be {@code <[T 0 1][H 1 1][H 0 2]>} 
   * and the {@code action} passed may be to push the tile ahead north, i.e. 
   * {@code <[PT 0 1]>}.  However, if {@code <[T 0 1][H 1 1][H 0 2]>} is not 
   * exactly represented by a {@link jchrest.architecture.Node} in {@link 
   * jchrest.architecture.Stm} but {@code <[T 0 1][H 1 1]>} is, the production 
   * learned will be equal to "whenever a tile can be seen on the square 
   * immediately ahead and a hole can be seen to the right of the tile, push the 
   * tile north" rather than "whenever a tile can be seen on the square 
   * immediately ahead and <b>two holes can be seen, one to the right of the 
   * tile and the other north of the tile</b>, push the tile north".
   * 
   * This method may consume the <i>attentional</i> and <i>cognitive</i> 
   * resources of {@link #this}:
   * <ul>
   *  <li>
   *    If attention is free at the {@code time} specified, attention is 
   *    consumed according to the documentation for {@link 
   *    jchrest.architecture.Chrest#searchStm(jchrest.lib.ListPattern, int)}.
   *  </li>
   *  <li>
   *    Cognition is consumed if an attempt to learn a production is successful.  
   *    The actual time cognition is consumed until is equal to the time the 
   *    {@link jchrest.architecture.Chrest#searchStm(jchrest.lib.ListPattern, 
   *    int)} completes plus the value returned by {@link 
   *    #this#getAddProductionTime()}.
   *  </li>
   * </ul>
   * 
   * @param visualNode
   * @param actionNode
   * @param time
   * 
   * @return The following conditions will cause the following {@link 
   * jchrest.lib.ChrestStatus} to be returned (note that conditions are 
   * cumulative, i.e. when checking the second condition, the first condition
   * does not apply):
   * <ol type="1">
   *  <li>
   *    If {@link #this} does not exist at the {@code time} specified, {@link 
   *    jchrest.lib.ChrestStatus#MODEL_DOES_NOT_EXIST_AT_TIME} is returned.
   *  </li>
   *  <li>
   *    If {@link jchrest.architecture.Chrest#isAttentionFree(int)} returns 
   *    {@link java.lang.Boolean#FALSE} when invoked in context of {@link #this} 
   *    and {@code time} is specified as a parameter, {@link 
   *    jchrest.lib.ChrestStatus#ATTENTION_BUSY} is returned.
   *  </li>
   *  <li>
   *    If {@link jchrest.architecture.Chrest#searchStm(jchrest.lib.ListPattern, 
   *    int)} does not return a {@link jchrest.architecture.Node} whose contents
   *    match the {@code vision} specified, {@link 
   *    jchrest.lib.ChrestStatus#VISION_NOT_IN_STM} is returned.
   *  </li>
   *  <li>
   *    If {@link jchrest.architecture.Chrest#searchStm(jchrest.lib.ListPattern, 
   *    int)} does return a {@link jchrest.architecture.Node} whose contents
   *    match the {@code vision} specified, but does not return a {@link 
   *    jchrest.architecture.Node} whose contents match the {@code action} 
   *    specified, {@link jchrest.lib.ChrestStatus#ACTION_NOT_IN_STM} is 
   *    returned.
   *  </li>
   *  <li>
   *    See return values for {@link 
   *    jchrest.architecture.Node#addProduction(jchrest.architecture.Node, 
   *    java.lang.Double, int)}.
   *  </li>
   * </ol>
   */
  public ChrestStatus learnProduction(ListPattern vision, ListPattern action, int time){
    this.printDebugStatement("===== Chrest.learnProduction() =====");
    this.printDebugStatement(
      "- Attempting to learn a production between the vision (" + vision.toString() + 
      ") and action (" + action.toString() + ") specified at time " + time
    );
    ChrestStatus result = ChrestStatus.LEARN_PRODUCTION_FAILED;
    
    //////////////////////////////
    ///// PRELIMINARY CHECKS /////
    //////////////////////////////
    
    this.printDebugStatement(
      "- Checking if the following statements all evaluate to true: " +
      "\n  ~ This CHREST model exists at the time this method is requested: " + (this._creationTime <= time) + 
      "\n  ~ Attention is free at the time this method is requested: " + (this.isAttentionFree(time)) +
      "\n  ~ Vision specified has Visual modality: " + (vision.getModality() == Modality.VISUAL) +
      "\n  ~ Action specified has Action modality: " + (action.getModality() == Modality.ACTION)
    );
    
    if(
      this._creationTime <= time && 
      this.isAttentionFree(time) &&
      vision.getModality() == Modality.VISUAL &&
      action.getModality() == Modality.ACTION
    ){
      this.printDebugStatement("    + All OK");
      
      //////////////////////////////
      ///// GET MATCHING NODES /////
      //////////////////////////////

      ///// GET MATCHING VISUAL NODES /////
      
      this.printDebugStatement(
        "- Checking if visual STM is empty (" + 
        this.getStm(Modality.VISUAL).getContents(time).isEmpty() + "), if it " +
        "is, it will not be searched for Nodes matching the vision input."
      );
      
      List<Node> matchingVisualNodes = new ArrayList();
      if(!this.getStm(Modality.VISUAL).getContents(time).isEmpty()){
        this.printDebugStatement("  ~ Visual STM is not empty, searching for Nodes whose contents match vision"); 
        
        //Since this method checks that attention is free and that visual STM is
        //not empty before program control gets to here, it can be safely 
        //assumed that attention will be consumed during this.searchStm() since 
        //visual STM is not empty.  Thus, before searching action STM, the 
        //current time according to the scope of this method can be safely set 
        //to the value of this model's attention clock after the search since it 
        //is only this search that will have affected this model's cognition 
        //clock.  Setting this time correctly is important since, the action STM 
        //search may return null incorrectly due to attention being consumed by 
        //performing the visual STM searcg.
        matchingVisualNodes = this.searchStm(vision, time);
        time = this._attentionClock;
        this.printDebugStatement("- Visual STM search complete, current time set to time search completes (" + time + ")");
      }
      else{
        this.printDebugStatement("  ~ Visual STM is empty"); 
      }
      
      ///// GET MATCHING ACTION NODES /////
      
      this.printDebugStatement(
        "- Checking if action STM is empty (" + 
        this.getStm(Modality.VISUAL).getContents(time).isEmpty() + "), if it " +
        "is, it will not be searched for Nodes matching the action input."
      );
      
      List<Node> matchingActionNodes = new ArrayList(); 
      if(!this.getStm(Modality.ACTION).getContents(time).isEmpty()){
        this.printDebugStatement("  ~ Action STM is not empty, searching for Nodes whose contents match action"); 
      
        //Like the visual STM search above, setting the current time correctly 
        //is important here: the next time the current time is used is when a 
        //production is created (if applicable) and this should occur after the
        //visual and action STM searches.
        matchingActionNodes = this.searchStm(action, time);
        time = Math.max(time, this._attentionClock);
        this.printDebugStatement("- Action STM search complete, current time set to time search completes (" + time + ")");
      }
      else{
        this.printDebugStatement("  ~ Action STM is empty"); 
      }
      
      this.printDebugStatement("- Time after searches complete: " + time);
      
      ArrayList<List<Node>> matchingVisualAndActionNodes = new ArrayList();
      matchingVisualAndActionNodes.add(matchingVisualNodes);
      matchingVisualAndActionNodes.add(matchingActionNodes);
      this.printDebugStatement("- Matching Nodes: \n  ~ Visual:");
      for(Node node : matchingVisualAndActionNodes.get(0)){
        this.printDebugStatement("    + Ref: " + node.getReference() + ", contents: " + node.getContents().toString());
      }
      this.printDebugStatement("  ~ Action:");
      for(Node node : matchingVisualAndActionNodes.get(1)){
        this.printDebugStatement("    + Ref: " + node.getReference() + ", contents: " + node.getContents().toString());
      }
      
      ////////////////////////////////////////////////////
      ///// DETERMINE NODES TO BE USED IN PRODUCTION /////
      ////////////////////////////////////////////////////
      
      //Set up a data structure that will specify the visual/action Node to be
      //used in the production and whether the vision/action input to this 
      //method needs to be learned (learning required if none of the images of
      //the visual/action Nodes that match equal the vision/action input).  
      Node[] nodesToBeUsed = new Node[2];
      
      for(int i = 0; i < matchingVisualAndActionNodes.size(); i++){
        ListPattern input = (i == 0 ? vision : action);
        String inputType = (i == 0 ? "vision" : "action");
        String modality = (i == 0 ? "visual" : "action");
        this.printDebugStatement(
            "- Setting the " + modality + " Node to use in the production to " +
            "either the first Node returned whose contents equals the " + inputType +
            " or, if no such Node has been returned, the Node returned whose " +
            "contents match the " + inputType + " most (biggest ListPattern that " +
            "matches)"
          );
      
        List<Node> matchingNodes = matchingVisualAndActionNodes.get(i);
        for(Node node : matchingNodes){
          this.printDebugStatement("  ~ Processing Node " + node.getReference());
        
          ListPattern nodeContents = node.getContents();
          this.printDebugStatement("    + Contents: " + nodeContents);
          
          /////////////////////////////////////////
          ///// CHECK IF CONTENTS EQUAL INPUT /////
          /////////////////////////////////////////
          
          if(nodeContents.equals(input)){
            this.printDebugStatement(
              "      = Contents equals " + inputType + ", setting this Node to " +
              "be the " + modality + " Node to use in the production and ending " +
              "search for " + modality + " Node to use in production"
            );
            nodesToBeUsed[i] = node;
            break;
          }
          
          /////////////////////////////////////////
          ///// CHECK IF CONTENTS MATCH INPUT /////
          /////////////////////////////////////////
          
          //Node must match (only matches nodes are returned in list being 
          //iterated through) so, if the visual Node recognised hasn't been set 
          //yet or, if one has but this Node's contents are bigger than the 
          //visual Node currently recognised, set this Node to be the visual 
          //Node recognised.
          else if(
            nodesToBeUsed[i] == null || 
            nodesToBeUsed[i].getContents().size() < nodeContents.size()
          ){
          
            this.printDebugStatement(
              "    + Either no " + modality + " STM Node has been set to use " + 
              "in the production yet (" + (nodesToBeUsed[i] == null) + ") or the " + 
              modality + " STM Node to use in the production has been set " +
              "already but the size of its contents are smaller than this " +
              "Node's (" + (nodesToBeUsed[i] != null && 
              nodesToBeUsed[i].getContents().size() < nodeContents.size()) + 
              ").  Regardless, this Node will be set to the " + modality + 
              " Node to be used in the production, for the moment"
            );
            nodesToBeUsed[i] = node;
          }
          else{
            this.printDebugStatement(
              "    + The contents of this node does not equal the " + inputType + 
              " and is not bigger than the contents of the current Node to " +
              "use in the production"
            );
          }
        }
      }
      
      //////////////////////////
      ///// ADD PRODUCTION /////
      //////////////////////////
      
      this.printDebugStatement(
        "- Checking if a visual and action Node have been specified for use " +
        "in creating the production."
      );
      
      Node visualNode = nodesToBeUsed[0];
      Node actionNode = nodesToBeUsed[1];
      
      if(visualNode != null && actionNode != null){
        this.printDebugStatement(
          "  ~ Visual and action Nodes specified, attempting to add " +
          "production between Nodes " + visualNode.getReference() + " and " + 
          actionNode.getReference() + " at current time (" + time + ") plus " +
          "the time taken to add a production (" + this._addProductionTime + ")"
        );
        time += this._addProductionTime;
        result = visualNode.addProduction(actionNode, time);
        
        if(result == ChrestStatus.LEARN_PRODUCTION_SUCCESSFUL){
          this.printDebugStatement(
            "    + Production added successfully, setting cognition clock to " +
            "the time the production was added (" + time + ")"
          );
          this._cognitionClock = time;
        }
        else{
          this.printDebugStatement("    + Production not added");
        }
      }
      else if(visualNode == null){
        this.printDebugStatement("  ~ Visual Node not specified, exiting");
        result = ChrestStatus.VISION_NOT_IN_STM;
      }
      else if(actionNode == null){
        this.printDebugStatement("  ~ Action Node not specified, exiting");
        result = ChrestStatus.ACTION_NOT_IN_STM;
      }
    }
    else{
      this.printDebugStatement("    + One or more conditions evaluated to false, exiting");
      if(this._creationTime > time){
        result = ChrestStatus.MODEL_DOES_NOT_EXIST_AT_TIME;
      }
      else if(!this.isAttentionFree(time)){
        result = ChrestStatus.ATTENTION_BUSY;
      }
      else if(vision.getModality() == Modality.VISUAL || action.getModality() == Modality.ACTION){
        throw new IllegalArgumentException(
          "The vision or action specified does not have the correct modality " +
          "\n- Does the vision specified (" + vision.toString() + ") have visual " + 
          "modality: " + (vision.getModality() == Modality.VISUAL) +
          "\n- Does the action specified (" + action.toString() + ") have action " + 
          "modality: " + (action.getModality() == Modality.ACTION)
        );
      }
    }
    
    this.printDebugStatement("- Returning " + result.name());
    this.printDebugStatement("===== RETURN Chrest.learnProduction() =====");
    return result;
  }
  
  /**
   * Determines the {@link jchrest.lib.Modality} of the two {@link 
   * jchrest.architecture.Node Nodes} passed as parameters and creates the 
   * relevant type of association between them if {@link 
   * #this#isCognitionFree(int)} returns {@link java.lang.Boolean#TRUE} at the
   * {@code time} specified.
   * 
   * An association will only be created if the following statements all 
   * evaluate to {@link java.lang.Boolean#TRUE}:
   * 
   * <ol type="1">
   *  <li>
   *    {@link #this#isCognitionFree(int)} returns {@link 
   *    java.lang.Boolean#TRUE} at the {@code time} specified.
   *  </li>
   *  <li>
   *    Invoking {@link jchrest.architecture.Node#isRootNode()} on {@code 
   *    nodeToAssociateFrom} and {@code nodeToAssociateTo} returns
   *    {@link java.lang.Boolean#FALSE}.
   *  </li>
   *  <li>
   *    The association to create doesn't already exist between {@code 
   *    nodeToAssociateFrom} and {@code nodeToAssociateTo}.
   *  </li>
   * </ol>
   * 
   * The types of association created are as follows:
   * 
   * <table border="1">
   *  <tr>
   *    <th>Node to associate from {@link jchrest.lib.Modality}</th>
   *    <th>Node to associate to {@link jchrest.lib.Modality}</th>
   *    <th>Association created (function invoked)</th>
   *  </tr>
   *  <tr>
   *    <td>Equal to node to associate to {@link jchrest.lib.Modality}</td>
   *    <td>Equal to node to associate from {@link jchrest.lib.Modality}</td>
   *    <td>
   *      Semantic link ({@link jchrest.architecture.Node#addSemanticLink(
   *      jchrest.architecture.Node, int)})
   *    </td>
   *  </tr>
   *  <tr>
   *    <td>{@link jchrest.lib.Modality#VISUAL}</td>
   *    <td>{@link jchrest.lib.Modality#ACTION}</td>
   *    <td>
   *      Production ({@link jchrest.architecture.Node#addProduction(
   *      jchrest.architecture.Node, java.lang.Double, int)})
   *    </td>
   *  </tr>
   *  <tr>
   *    <td>{@link jchrest.lib.Modality#VISUAL}</td>
   *    <td>{@link jchrest.lib.Modality#VERBAL}</td>
   *    <td>
   *      Named-by link ({@link jchrest.architecture.Node#setNamedBy(
   *      jchrest.architecture.Node, int)})
   *    </td>
   *  </tr>
   * </table>
   * 
   * <b>NOTE:</b> semantic link associations are special cases since the
   * association created will be bi-directional and, in addition to the 
   * conditions for association creation listed above, {@link 
   * jchrest.lib.ListPattern#isSimilarTo(jchrest.lib.ListPattern, int)} must 
   * evaluate to {@link java.lang.Boolean#TRUE} when the result of invoking 
   * {@link jchrest.architecture.Node#getImage(int)} on {@code 
   * nodeToAssociateFrom} and {@code nodeToAssociateTo} are passed as the first 
   * and second parameters, respectively and {@link 
   * #this#_nodeImageSimilarityThreshold} is passed as a third parameter.
   * 
   * If an association is created, the cognition clock of {@link #this} will be 
   * set to the following times:
   * 
   * <table>
   *  <tr>
   *    <th>Association</th>
   *    <th>Cognition Clock Set To</th>
   *  </tr>
   *  <tr>
   *    <td>Semantic Link</td>
   *    <td>
   *      {@code time} + {@link #this#getNodeComparisonTime()} + ({@link 
   *      #this#getTimeToCreateSemanticLink()} * 2)
   *    </td>
   *  </tr>
   *  <tr>
   *    <td>Production</td>
   *    <td>{@code time} + {@link #this#getAddProductionTime()}</td>
   *  </tr>
   *  <tr>
   *    <td>Named-by link</td>
   *    <td>{@code time} + {@link #this#getTimeToCreateNamingLink()}</td>
   *  </tr>
   * </table>
   * 
   * @param nodeToAssociateFrom
   * @param nodeToAssociateTo
   * @param time 
   * 
   * @return Whether the association was created or not.
   */
  private boolean associateNodes(Node nodeToAssociateFrom, Node nodeToAssociateTo, int time){
    this.printDebugStatement("===== Chrest.associatedNodes() =====");
    boolean associationCreated = false;
    
    this.printDebugStatement(
      "- Checking if cognition is free at time function invoked (" + time + ") " + 
      "and the nodes to associate aren't root nodes"
    );
    if(this.isCognitionFree(time) && !nodeToAssociateFrom.isRootNode() && !nodeToAssociateTo.isRootNode()){
      
      this.printDebugStatement("  ~ All OK");
      Modality nodeToAssociateFromModality = nodeToAssociateFrom.getModality();
      Modality nodeToAssociateToModality = nodeToAssociateTo.getModality();
    
      this.printDebugStatement(
        "- Checking modality of nodes to associate (node to associate from modality: '" +
        nodeToAssociateFromModality.toString() + "', node to associate to modality: '" +
        nodeToAssociateToModality.toString() + "'."
      );
      
      if(nodeToAssociateFromModality.equals(nodeToAssociateToModality)){
        this.printDebugStatement("  ~ Attempting to create a semantic link");
        
        List<Node> nodeToAssociateFromSemanticLinks = nodeToAssociateFrom.getSemanticLinks(time);
        List<Node> nodeToAssociateToSemanticLinks = nodeToAssociateTo.getSemanticLinks(time);
        this.printDebugStatement("- Checking if semantic link exists between nodes in either direction");
        
        if(
          (
            !nodeToAssociateFromSemanticLinks.contains(nodeToAssociateTo) ||
            !nodeToAssociateToSemanticLinks.contains(nodeToAssociateFrom)
          )
        ){
          
          this.printDebugStatement(
            "  ~ A semantic link between the nodes doesn't exist in one/both " +
            "directions, comparing node images to see if they're similar " +
            "enough for a semantic link to be created between them"
          );
          
          time += this._nodeComparisonTime;
          this.printDebugStatement(
            "  ~ Time incremented by node comparison time specified (" + 
            this._nodeComparisonTime + ") to " + time
          );
          
          if(nodeToAssociateTo.getImage(time).isSimilarTo(nodeToAssociateFrom.getImage(time), this._nodeImageSimilarityThreshold)){
            this.printDebugStatement("  ~ Node images similar enough to create semantic link between them");
            
            this.printDebugStatement("- Determining if a uni/bi-directional semantic link needs to be created");
            boolean fromToAssociationCreated = false;
            boolean toFromAssociationCreated = false;
            
            if(!nodeToAssociateFromSemanticLinks.contains(nodeToAssociateTo)){
              
              this.printDebugStatement(
                "  ~ Creating semantic link in direction of node to add " +
                "association from to node to add association to."
              );
              time += this._semanticLinkCreationTime;
              this.printDebugStatement(
                "  ~ Time incremented by semantic link creation time specified (" + 
                this._semanticLinkCreationTime + ") to " + time
              );
              
              fromToAssociationCreated = nodeToAssociateFrom.addSemanticLink(nodeToAssociateTo, time);
            }
          
            if(!nodeToAssociateToSemanticLinks.contains(nodeToAssociateFrom)){
              this.printDebugStatement(
                "  ~ Creating semantic link in direction of node to add " +
                "association to to node to add association from."
              );
              time += this._semanticLinkCreationTime;
              this.printDebugStatement(
                "  ~ Time incremented by semantic link creation time specified (" + 
                this._semanticLinkCreationTime + ") to " + time
              );
              toFromAssociationCreated = nodeToAssociateTo.addSemanticLink(nodeToAssociateFrom, time);
            }
          
            if(fromToAssociationCreated || toFromAssociationCreated){
              associationCreated = true;
            }
          }
          else{
            this.printDebugStatement("  ~ Node images not similar enough to create semantic link, exiting.");
          }
        }
        else{
          this.printDebugStatement("  ~ Semantic links exist between both nodes in both directions, exiting.");
        }
      }
      else if(
        nodeToAssociateFromModality.equals(Modality.VISUAL) && 
        nodeToAssociateToModality.equals(Modality.VERBAL)
      ){
        this.printDebugStatement("  ~ Attempting to create a naming link");
        
        this.printDebugStatement("- Checking if nodes have a naming link between them already");
        Node namedBy = nodeToAssociateFrom.getNamedBy(time);
        if(namedBy == null || !namedBy.equals(nodeToAssociateTo)){
          
          time += this._namingLinkCreationTime;
          this.printDebugStatement(
            "  ~ Nodes do not have a naming link between them already, " +
            "creating naming link and incrementing time by naming link creation time " +
            "specified (" + this._namingLinkCreationTime + ") to " + time
          );
          associationCreated = nodeToAssociateFrom.setNamedBy(nodeToAssociateTo, time);
        }
      }
    }
    else{
      this.printDebugStatement(
        "- Either cognition is not free (" + !this.isCognitionFree(time) + ") " + 
        ", the node to associate from is a root node (" + 
        nodeToAssociateFrom.isRootNode() + ") or the node to associate to is " +
        "a root node (" + nodeToAssociateTo.isRootNode() + ")"
      );
    }
    
    this.printDebugStatement("- Checking if an association was created, if so, the cognition clock will be modified");
    if(associationCreated){
      this._cognitionClock = time;
      this.printDebugStatement("  ~ An association was created, cognition clock set to " + time);
    }
    else{
      this.printDebugStatement("  ~ No association was created, cognition clock will not be modified");
    }
    
    this.printDebugStatement("- Returning " + associationCreated);
    this.printDebugStatement("===== RETURN =====");
    return associationCreated;
  }

  /**
   * Attempts to increase the total number of {@link jchrest.architecture.Node}s 
   * in the LTM network of this {@link #this} model by adding a new "child" to
   * the {@link jchrest.architecture.Node} specified.
   * 
   * If successful, discrimination will set the cognition clock of {@link #this}
   * to the time the new {@link jchrest.architecture.Node} is added to the 
   * long-term memory network of {@link #this}.
   * <p>
   * <b>NOTE:</b> It is assumed that the cognition resource is free at the time 
   * that discrimination is requested.  Therefore, this method should only be 
   * called by another method which should ensure that the cognitive resource 
   * is free at the {@code time} specified.  If this is not the case, a {@link 
   * java.lang.NullPointerException} will be thrown when the method attempts to 
   * determine if there are unlearened {@link jchrest.lib.PrimitivePattern 
   * PrimitivePatterns} in the {@code pattern} specified.
   * 
   * @param nodeToDiscriminateFrom 
   * @param pattern The information that triggered discrimination.
   * @param time
   * 
   * @return Either {@link jchrest.lib.Status#DISCRIMINATION_SUCCESSFUL} or
   * {@link jchrest.lib.Status#DISCRIMINATION_FAILED}.
   */
  private ChrestStatus discriminate (Node nodeToDiscriminateFrom, ListPattern pattern, int time) {
    this.printDebugStatement("===== Chrest.discriminate() =====");
    boolean discriminationSuccessful;
    
    //////////////////////////////////////////////
    ///// GET NEW INFORMATION IN ListPattern /////
    //////////////////////////////////////////////
    
    ListPattern newInformation = pattern.remove(nodeToDiscriminateFrom.getContents());
    
    //If there is no new information, set the new information to the end 
    //delimiter so that the model can check if it is recognised.
    if(newInformation.isEmpty()) newInformation.setFinished();

    this.printDebugStatement(
      "- Node to discriminate from reference: " + 
      nodeToDiscriminateFrom.getReference() + ", pattern that triggered " + 
      "discrimination: " + pattern.toString() + ", time discrimination " +
      "invoked: " + time + ", new information in pattern that triggered " +
      "discrimination: " + newInformation + "."
    );
    
    /////////////////////////////////////
    ///// RECOGNISE NEW INFORMATION /////
    /////////////////////////////////////
    
    //Recognition at this stage doesn't incur a time cost since its assumed that
    //the recognition that occured during the method that invokes 
    //Chrest.discriminate() has identified what needs to be learned and whether
    //it is known.  Recognition is called here for the purposes of code, not 
    //because its assumed to occur in human-beings.
    this.printDebugStatement("- Attempting to recognise new information");
    Node nodeRetrievedAfterRecognisingNewInformation = this.recognise(newInformation, time, false);
    this.printDebugStatement(
      "- Reference of Node retrieved after recognising new information: " + 
        nodeRetrievedAfterRecognisingNewInformation.getReference()
    );

    ////////////////////////
    ///// DISCRIMINATE /////
    ////////////////////////
    
    this.printDebugStatement("- Discrimination will now occur at time " + (time + this._discriminationTime));
    time += this._discriminationTime;

    //If the new information is empty it must be handled differently to the way
    //it would be handled if it were not empty.  Unfortunately, it is not 
    //therefore possible to combine how the new information is ultimately added
    //to LTM.
    if(newInformation.isEmpty()){
      
      this.printDebugStatement(
        "- New information is empty, checking if " + newInformation.toString() + 
        " has been recognised/learned."
      );
      
      boolean endChunkDelimiterKnown = nodeRetrievedAfterRecognisingNewInformation.getContents().equals(newInformation);
      
      //1. < $ > known, use as test
      if(endChunkDelimiterKnown){

        this.printDebugStatement(
          "  ~ " +  newInformation.toString() + " has been learned so it will " +
          "be added as a test on a new link from " + 
          nodeToDiscriminateFrom.getReference() + "."
        );

        discriminationSuccessful = nodeToDiscriminateFrom.addChild(newInformation, time);
      }
      // 2. < $ > not known, learn as primitive but not using 
      //    Chrest.learnPrimtive since that function sets the pattern passed to 
      //    not finished so the end chunk delimiter would be lost.
      else {
        this.printDebugStatement("  ~ " + newInformation.toString() + " has not been learned so it will be learned as a primitive.");

        Node child = new Node (this, newInformation, newInformation, time);
        discriminationSuccessful = this.getLtmModalityRootNode(newInformation)
          .addChild(newInformation, child, time, this.getCurrentExperimentName());
      }
    }
    //New information isn't empty.
    else{
      
      this.printDebugStatement(
        "- New information is not empty so the model will now check if " +
        newInformation.toString() + " has been learned."
      );
      
      //3. New information unrecognised, learn first item as a primitive
      if (nodeRetrievedAfterRecognisingNewInformation.isRootNode()) {
        this.printDebugStatement(
          "  ~ " + newInformation.toString() + " has not been learned so it will " +
          "be learned as a primitive."
        );
        
        discriminationSuccessful = this.learnPrimitive(newInformation.getFirstItem(), time);
      } 
      //4. New information recognised and contents of recognised Node match 
      //   input (will occur if contents unfinished.  Use 
      //   recognised node contents can be used as a test
      else if (nodeRetrievedAfterRecognisingNewInformation.getContents().matches (newInformation)) {
        ListPattern testPattern = nodeRetrievedAfterRecognisingNewInformation.getContents().clone ();
        
        this.printDebugStatement(
          "  ~ " + newInformation.toString() + " has been learned and " +
          testPattern.toString() + " will be used as a test on a new link " +
          "from node " + nodeToDiscriminateFrom.getReference() + "."
        );
        
        discriminationSuccessful = nodeToDiscriminateFrom.addChild (testPattern, time);
      }
      // 5. mismatch, so use only the first item for test
      // NB: first-item must be in network as node was not the root 
      //     node
      else {
        ListPattern firstItem = newInformation.getFirstItem();
        firstItem.setNotFinished();
        
        this.printDebugStatement(
          "  ~ " + newInformation.toString() + " has been learned but only " +
          "the first item from the new information (" + firstItem.toString() + 
          ") will be used on a new link from node " + 
          nodeToDiscriminateFrom.getReference() + "."
        );

        discriminationSuccessful = nodeToDiscriminateFrom.addChild (firstItem, time);
      }
    }
    
    this.printDebugStatement(
      "- Discrimination " + (discriminationSuccessful ? "was" : "was not") + 
      " successful so the cognition clock " + (discriminationSuccessful ? 
      "will be set to the time discrimination ends (" + time + ")" :
      "will not be altered") + "."
    );
    
    if(discriminationSuccessful){
      this._cognitionClock = time;
    }
    
    ChrestStatus discriminationResult = (discriminationSuccessful ? ChrestStatus.DISCRIMINATION_SUCCESSFUL : ChrestStatus.DISCRIMINATION_FAILED);
    this.printDebugStatement("- Returning " + discriminationResult.name());
    this.printDebugStatement("===== RETURN Chrest.discriminate() =====");
    return discriminationResult;
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
   * @return Either {@link jchrest.lib.Status#FAMILIARISATION_SUCCESSFUL} or
   * {@link jchrest.lib.Status#FAMILIARISATION_FAILED}.
   */
  private ChrestStatus familiarise (Node nodeToFamiliarise, ListPattern pattern, int time) {
    this.printDebugStatement("===== Chrest.familiarise() ====");
    boolean familiarisationSuccessful = false;
    
    ListPattern newInformation = pattern.remove(nodeToFamiliarise.getImage(time)).getFirstItem();
    newInformation.setNotFinished();

    this.printDebugStatement(
      "- Reference of node to attempt familiarisation on: " + 
      nodeToFamiliarise.getReference() + ", pattern that triggered " + 
      "familiarisation: " + pattern.toString() + ", time familiarisation " +
      "invoked: " + time + ".  New information in pattern that triggered " +
      "familiarisation: " + newInformation + "."
    );

    this.printDebugStatement("- Checking if there is any new information to learn (" + !newInformation.isEmpty() + ")");
    if(!newInformation.isEmpty()) {  
      this.printDebugStatement("  ~ There is new information to learn.");

      // Note: CHREST 2 had the idea of not familiarising if image size exceeds 
      // the max of 5 and 2*contents-size.  This avoids overly large images.
      // This idea is not implemented here.
      this.printDebugStatement("- Attemtping to recognise new informaion");
      Node recognisedNode = this.recognise(newInformation, time, false);

      if(recognisedNode.isRootNode()){
        this.printDebugStatement("  ~ New information unrecognised, learning via. discrimination.");
        this.printDebugStatement("===== RETURN Chrest.familiarisation() =====");
        return this.discriminate(recognisedNode, newInformation, time);
      } 
      else {
        this.printDebugStatement(
          "  ~ New information recognised, attempting to add new information " +
          "to image of node " +  nodeToFamiliarise.getReference() + " at current " + 
          "time (" + time + ") plus the time taken to familiarise (" +
           this._familiarisationTime+ ")."
        );

        time += this._familiarisationTime;
        familiarisationSuccessful = nodeToFamiliarise.extendImage(newInformation, time);
      
        if(familiarisationSuccessful){
          this.printDebugStatement(
            "- Familiarisation successful, setting cognition clock to the " +
            "time node " + nodeToFamiliarise.getReference() + " is familiarised (" + 
            time + ")."
          );
        
          this._cognitionClock = time;
        }
        else{
          this.printDebugStatement("- Familiarisation unsuccessful, exiting.");
        }
      }
    }
    else{
      this.printDebugStatement("  ~ There is no new information to learn, exiting.");
    }
    
    ChrestStatus result = (familiarisationSuccessful ? ChrestStatus.FAMILIARISATION_SUCCESSFUL : ChrestStatus.FAMILIARISATION_FAILED);
    this.printDebugStatement("Returning " + result.name());
    this.printDebugStatement("===== RETURN Chrest.familiarise() =====");
    return result;
  }
  
  /**
   * Attempts to reinforce the production specified by the {@code actionNode} 
   * and {@code visualNode} passed as parameters.
   * 
   * Invoking this function may consume the attentional and cognitive resources
   * of {@link #this}:
   * <ul>
   *  <li>
   *    Attention is consumed since the {@code actionNode} and {@code 
   *    visualNode} must both be present in {@link jchrest.lib.Modality#ACTION} 
   *    and {@link jchrest.lib.Modality#VISUAL} {@link jchrest.architecture.Stm} 
   *    respectively and checking this consumes attention: each {@link 
   *    jchrest.architecture.Stm} is cycled through from the hypothesis until 
   *    the {@code actionNode} or {@code visualNode} is found (if at all). 
   *    Checking each {@link jchrest.architecture.Node} in {@link 
   *    jchrest.architecture.Stm} incurs the time cost specified by {@link 
   *    #this#getTimeToRetrieveItemFromStm()}.
   *  </li>
   *  <li>
   *    Cognition is consumed since it requires time to reinforce a production.
   *    If a production between the {@code actionNode} and {@code visualNode} 
   *    exists, and they are both present in their respective {@link 
   *    jchrest.architecture.Stm Stm's}, and {@link 
   *    jchrest.architecture.Node#reinforceProduction(jchrest.architecture.Node, 
   *    java.lang.Double[], int)} returns {@link java.lang.Boolean#TRUE},  
   *    cognition will be consumed until the {@code actionNode} and {@code 
   *    visualNode} are retrieved from {@link jchrest.architecture.Stm} plus the 
   *    time specified by {@link #this#getReinforceProductionTime()}.
   *  </li>
   * </ul>
   * 
   * @param visualPattern
   * @param actionPattern
   * @param variables The variables required by the result of {@link 
   * #this#getReinforcementLearningTheory()} to invoke {@link 
   * jchrest.lib.ReinforcementLearning.Theory#calculateReinforcementValue(
   * java.lang.Double[])}. 
   * @param time 
   * 
   * @return {@link java.lang.Boolean#TRUE} if reinforcement occurs, i.e. if 
   * all the following statements evaluate to {@link java.lang.Boolean#TRUE}, 
   * {@link java.lang.Boolean#FALSE} if not.
   * <ul>
   *  <li>
   *    The {@link jchrest.lib.Modality#ACTION} {@link jchrest.architecture.Stm} 
   *    associated with {@link #this} exists at the {@code time} specified.
   *  </li>
   *  <li>
   *    The {@link jchrest.lib.Modality#VISUAL} {@link jchrest.architecture.Stm} 
   *    associated with {@link #this} exists at the {@code time} specified.
   *  </li>
   *  <li>
   *    The result of {@link jchrest.architecture.Node#getModality()} is {@link 
   *    jchrest.lib.Modality#ACTION} for the {@code actionNode} specified.
   *  </li>
   *  <li>
   *    The result of {@link jchrest.architecture.Node#getModality()} is {@link 
   *    jchrest.lib.Modality#VISUAL} for the {@code visualNode} specified.
   *  </li>
   *  <li>
   *    {@link #this#isAttentionFree(int)} returns {@link 
   *    java.lang.Boolean#TRUE} when the {@code time} specified is passed as a
   *    parameter.
   *  </li>
   *  <li>
   *    The {@code actionNode} specified is present in {@link 
   *    jchrest.lib.Modality#ACTION} {@link jchrest.architecture.Stm} at the
   *    {@code time} specified.
   *  </li>
   *  <li>
   *    The {@code visualNode} specified is present in {@link 
   *    jchrest.lib.Modality#VISUAL} {@link jchrest.architecture.Stm} at the
   *    {@code time} specified.
   *  </li>
   *  <li>
   *    {@link #this#isCognitionFree(int)} returns {@link 
   *    java.lang.Boolean#TRUE} after retrieving the {@code actionNode} and 
   *    {@code visualNode} from {@link jchrest.architecture.Stm}.
   *  </li>
   *  <li>
   *    Invoking {@link jchrest.architecture.Node#reinforceProduction(
   *    jchrest.architecture.Node, java.lang.Double[], int)} returns {@link 
   *    java.lang.Boolean#TRUE} when invoked on the {@code visualNode} and the
   *    {@code actionNode}, {@code variables} and time reinforcement should 
   *    occur are passed as parameters.
   *  </li>
   * </ul>
   */
  public boolean reinforceProduction(Node visualNode, Node actionNode, Double[] variables, int time){
    this.printDebugStatement("===== Chrest.reinforceProduction() =====");
    boolean reinforcementSuccessful = false;
    
    //////////////////////////////
    ///// PRELIMINARY CHECKS /////
    //////////////////////////////
    
    List<Node> actionStmContents = this.getStm(Modality.ACTION).getContents(time);
    List<Node> visualStmContents = this.getStm(Modality.VISUAL).getContents(time);
    
    if(this.debug()){
      this.printDebugStatement("- Checking if the following all evaluate to true:");
      this.printDebugStatement("  ~ Does action STM exist at the time this function is invoked? " + (actionStmContents != null));
      this.printDebugStatement("  ~ Does visual STM exist at the time this function is invoked? " + (visualStmContents != null));
      this.printDebugStatement("  ~ Does the action Node specified have action modality? " + (actionNode.getModality().equals(Modality.ACTION)));
      this.printDebugStatement("  ~ Does the visual Node specified have visual modality? " + (visualNode.getModality().equals(Modality.VISUAL)));
      this.printDebugStatement("  ~ Is attention free at time " + time + "? " + (this.isAttentionFree(time)));
    }
    
    if(
      actionStmContents != null &&
      visualStmContents != null &&
      actionNode.getModality().equals(Modality.ACTION) &&
      visualNode.getModality().equals(Modality.VISUAL) &&
      this.isAttentionFree(time)
    ){
      
      this.printDebugStatement(
        "- Checking if the action and visual Node specified are in their " +
        "respective STMs (checking references)"
      );
      
      if(this.debug()){
        this.printDebugStatement("  ~ Action Node reference " + actionNode.getReference());
        this.printDebugStatement("  ~ Action STM contents:");
        for(Node actionStmContent : actionStmContents){
          this.printDebugStatement("    > Node reference: " + actionStmContent.getReference());
        }
        
        this.printDebugStatement("  ~ Visual Node reference " + visualNode.getReference());
        this.printDebugStatement("  ~ Visual STM contents:");
        for(Node visualStmContent : visualStmContents){
          this.printDebugStatement("    > Node reference: " + visualStmContent.getReference());
        }
      }
      
      ////////////////////////////////////////////////
      ///// CHECK THAT actionNode AND visualNode /////
      ///// ARE IN STM AND COGNITION IS FREE     /////
      ////////////////////////////////////////////////
      
      boolean actionNodeInStm = false;
      boolean visualNodeInStm = false;
      
      for(Node actionStmContent : actionStmContents){
        time += this._timeToRetrieveItemFromStm;
        if(actionNode.getReference() == actionStmContent.getReference()){
          actionNodeInStm = true;
          break;
        }
      }
      this.printDebugStatement("- Time after searching action STM for action Node specified : " + time);
      
      for(Node visualStmContent : visualStmContents){
        time += this._timeToRetrieveItemFromStm;
        if(visualNode.getReference() == visualStmContent.getReference()){
          visualNodeInStm = true;
          break;
        }
      }
      this.printDebugStatement("- Time after searching visual STM for visual Node specified : " + time);
      
      this._attentionClock = time;
      this.printDebugStatement("- Consuming attention");
      
      this.printDebugStatement("- Checking if the following all evaluate to true:");
      if(this.debug()){
        this.printDebugStatement("  ~ Action Node in STM? " + actionNodeInStm);
        this.printDebugStatement("  ~ Visual Node in STM? " + visualNodeInStm);
        this.printDebugStatement("  ~ Is cognition free at time " + time + "? " + this.isCognitionFree(time));
      }
      if(actionNodeInStm && visualNodeInStm && this.isCognitionFree(time)){
        
        ///////////////////////////////////////////
        ///// ATTEMPT TO REINFORCE PRODUCTION /////
        ///////////////////////////////////////////
        
        this.printDebugStatement(
          "- All checks evaluate to true, attempting to reinforce production " +
          "at time " + time + " plus the time taken to reinforce productions (" + 
          this._reinforceProductionTime + ")"
        );
        
        int timeReinforcementShouldOccur = time + this._reinforceProductionTime;
        if(visualNode.reinforceProduction(actionNode, variables, time)){
          this._cognitionClock = timeReinforcementShouldOccur;
          this.printDebugStatement("  ~ Production reinforcement successful, consuming cognition");
          
          this.setChanged();
          if (!_frozen) notifyObservers ();
          reinforcementSuccessful = true;
        }
        else{
          this.printDebugStatement("  ~ Production reinforcement unsuccessful, exiting");
        }
      }
    }
    
    this.printDebugStatement("- Returning " + reinforcementSuccessful);
    this.printDebugStatement("- Attention clock set to: " + this._attentionClock);
    this.printDebugStatement("- Cognition clock set to: " + this._cognitionClock);    
    this.printDebugStatement("===== RETURN =====");
    return reinforcementSuccessful;
  }
  
  /*****************************************/
  /**** Short-term Memory Functionality ****/
  /*****************************************/
  
  /**
   * Attempts to retrieve the {@link jchrest.architecture.Node} from the {@code
   * index} specified at the {@code time} specified from the {@link 
   * jchrest.architecture.Stm} {@link jchrest.lib.Modality} specified.
   * <p>
   * If successful, this method will consume the attention resource of {@link 
   * #this} according to the following equation where <i>A</i> is the attention
   * clock of {@link #this}, <i>t</i> is the {@code time} specified, <i>r</i> 
   * is the result of {@link #this#getTimeToRetrieveItemFromStm()} and <i>i</i> 
   * is the {@code index} specified.
   * <p>
   * A = t + (r * i)
   * 
   * @param stmModality
   * @param index Non-zero indexed, i.e. if the first {@link 
   * jchrest.architecture.Node} in the {@code stmModality} specified is 
   * required, pass 1.
   * @param time
   * 
   * @return {@code null} if any of the following statements evaluate to {@link 
   * java.lang.Boolean#TRUE}.  Otherwise, the {@link jchrest.architecture.Node} 
   * in the {@code index} specified at the {@code time} specified in the {@link 
   * jchrest.architecture.Stm} {@link jchrest.lib.Modality} specified by {@code 
   * stmModality} is returned.
   * <ul>
   *  <li>{@link #this} does not exist at the {@code time} specified.</li>
   *  <li>
   *    {@link #this#isAttentionFree(int)} returns {@link 
   *    java.lang.Boolean#FALSE} at the {@code time} specified.
   *  </li>
   *  <li>
   *    The {@code index} specified is greater than the number of
   *    {@link jchrest.architecture.Node Nodes} in the {@link 
   *    jchrest.architecture.Stm} {@link jchrest.lib.Modality} specified at the
   *    {@code time} specified.
   *  </li>
   * </ul>
   */
  public Node getStmItem(Modality stmModality, int index, int time){
    this.printDebugStatement("===== Chrest.getStmItem() =====");
    this.printDebugStatement(
      "- Attempting to get the Node in position " + index + " from " + 
      stmModality.toString() + " at time " + time
    );
    
    Node stmItem = null;
    List<Node> stmContents = this.getStm(stmModality).getContents(time);
    
    this.printDebugStatement(
      "- Checking if the following statements all evaluate to true: " +
      "\n  ~ This model exists at the time specified: " + (this.getCreationTime() <= time) +
      "\n  ~ The attention of this model is free at the time specified: " + this.isAttentionFree(time) +
      "\n  ~ The index specified is smaller than or equal to the number of Nodes in STM at the time specified: " + (index <= stmContents.size())
    );
    if(
      this.getCreationTime() <= time && 
      this.isAttentionFree(time) &&
      index <= stmContents.size()
    ){
      this.printDebugStatement("- All OK");
      
      //Set the attention clock to the index * the time taken to retrieve a node
      //from STM.  Note that the index is 0 indexed so needs to have 1 added.
      this._attentionClock = time + (this._timeToRetrieveItemFromStm * index); 
      this.printDebugStatement(
        "- Attention clock is set to " + this._attentionClock + ", i.e the " +
        "time specified (" + time + ") plus the product of the time taken to " +
        "retrieve an item from STM (" + this._timeToRetrieveItemFromStm + ") " +
        "multiplied by the index specified (" + index + ")"
      );
      
      stmItem = stmContents.get(index - 1);
    }
    else{
      this.printDebugStatement("- A statement evaluated to false, exiting");
    }
    
    this.printDebugStatement(
      "- Returning " + (stmItem == null ? 
        "null" : 
        "Node with reference " + stmItem.getReference() + ", contents " + 
        stmItem.getContents() + " and image " + stmItem.getImage(time)
      )
    );
    this.printDebugStatement("===== RETURN Chrest.getStmItem() =====");
    return stmItem;
  }
  
  /**
   * Invokes {@link jchrest.architecture.Stm#getContents(int)} on the {@link 
   * jchrest.architecture.Stm} {@link jchrest.lib.Modality} of the {@code 
   * pattern} specified and searches the {@link jchrest.architecture.Node Nodes}
   * returned for any whose {@link jchrest.architecture.Node#getContents()} 
   * result matches the {@code pattern} specified.
   * 
   * If {@link jchrest.architecture.Chrest#isAttentionFree(int)} returns {@link 
   * java.lang.Boolean#TRUE} at the {@code time} specified and invoking {@link 
   * jchrest.architecture.Stm#getContents(int)} on the {@link 
   * jchrest.architecture.Stm} {@link jchrest.lib.Modality} of the {@code 
   * pattern} specified returns some {@link jchrest.architecture.Node Nodes}, 
   * attention will be consumed until some time <i>A</i>.  <i>A</i> will equal
   * the {@code time} specified, <i>T</i>, plus the product of the number of 
   * {@link jchrest.architecture.Node Nodes} returned, <i>N</i>, multiplied by 
   * the total of the time taken to retrieve a {@link jchrest.architecture.Node} 
   * from {@link jchrest.architecture.Stm}, <i>R</i>, plus the time taken to 
   * compare a {@link jchrest.architecture.Node}, <i>C</i>.  As an equation:
   * 
   * A = T + (N * (R + C))
   * 
   * @param pattern
   * @param time
   * 
   * @return If {@link jchrest.architecture.Chrest#isAttentionFree(int)} 
   * returns {@link java.lang.Boolean#FALSE}, {@code null} is returned.
   * <p>
   * If {@link jchrest.architecture.Node Nodes} match the condition stated 
   * above, a {@link java.util.List} of these {@link jchrest.architecture.Node 
   * Nodes} will be returned.
   * </p>
   * If no {@link jchrest.architecture.Node Nodes} match the condition stated 
   * above, an empty {@link java.util.List} will be returned.
   */
  public List<Node> searchStm(ListPattern pattern, int time){
    this.printDebugStatement("===== Chrest.searchStm() =====");
    List<Node> matchingNodes = null;
    
    //////////////////////////////////////
    ///// CHECK IF ATTENTION IS FREE /////
    //////////////////////////////////////
    
    this.printDebugStatement("- Checking if attention is free at time method invoked " + time);
    if(this.isAttentionFree(time)){
      this.printDebugStatement("  ~ Attention is free");
      
      ////////////////////////////
      ///// GET STM CONTENTS /////
      ////////////////////////////
      
      this.printDebugStatement("- Getting contents of " + pattern.getModality().toString() + " at time " + time);
      matchingNodes = new ArrayList();
      List<Node> stmContents = this.getStm(pattern.getModality()).getContents(time);

      if(stmContents != null && !stmContents.isEmpty()){
        this.printDebugStatement("  ~ STM contents are not null or empty");
        
        this.printDebugStatement("- Checking STM contents for any Nodes whose contents matches " + pattern.toString());
        
        ////////////////////////////////
        ///// PROCESS STM CONTENTS /////
        ////////////////////////////////
        
        for(Node stmNode : stmContents){
          time += this._timeToRetrieveItemFromStm + this._nodeComparisonTime;
          this.printDebugStatement(
            "  ~ Processing Node with reference " + stmNode.getReference() + ". " +
            "Incremented time by time taken to retrieve a Node from STM (" + 
            this._timeToRetrieveItemFromStm + ") plus the time taken to compare " +
            "a Node (" + this._nodeComparisonTime + "). Time now equals " + time
          );

          this.printDebugStatement(
            "    + Checking if STM Node contents (" + stmNode.getContents().toString() +
            ") is a pre-sequence (matches) " + pattern.toString()
          );
          if(stmNode.getContents().matches(pattern)){
            this.printDebugStatement("      = Match, adding Node " + stmNode.getReference() + " to the list to be returned");
            matchingNodes.add(stmNode);
          }
          else{
            this.printDebugStatement("      = No match, processing next Node");
          }
        }
        
        /////////////////////////////
        ///// CONSUME ATTENTION /////
        /////////////////////////////
        
        this._attentionClock = time;
      }
      else{
        this.printDebugStatement("  ~ STM contents are null (" + (stmContents == null) + ") or empty, exiting");
      }
    }
    else{
      this.printDebugStatement("  ~ Attention is not free, exiting");
    }
    
    //////////////////
    ///// RETURN /////
    //////////////////
    
    if(matchingNodes == null){
      this.printDebugStatement("- Returning null");  
    }
    else if(matchingNodes.isEmpty()){
      this.printDebugStatement("- Returning an empty list");
    }
    else{
      this.printDebugStatement("- Returning Nodes with references: ");
      for(Node node : matchingNodes) this.printDebugStatement("  ~ " + node.getReference());
    }
    this.printDebugStatement("===== RETURN Chrest.searchStm() =====");
    return matchingNodes;
  }
  
  /**
   * Attempts to add the {@link jchrest.architecture.Node} specified to the 
   * relevant {@link jchrest.architecture.Stm} modality associated with {@link 
   * #this} at the time passed.
   * 
   * If invoking {@link jchrest.architecture.Node#getModality()} on the {@code 
   * nodeToAdd} returns {@link jchrest.lib.Modality#VERBAL} and there is a 
   * {@link jchrest.architecture.Node} in {@link jchrest.lib.Modality#VISUAL} 
   * {@link jchrest.architecture.Stm} in the hypothesis position that returns
   * {@link java.lang.Boolean#FALSE} when {@link 
   * jchrest.architecture.Node#isRootNode()} is invoked upon it, {@link 
   * jchrest.architecture.Chrest#associateNodes(jchrest.architecture.Node, 
   * jchrest.architecture.Node, int)} is invoked, potentially creating a 
   * <i>naming</i> link between the two @link jchrest.architecture.Node Nodes}.
   * 
   * If a naming link is not created/applicable, creation of a <i>semantic</i> 
   * link is attempted between the {@code nodeToAdd} and the {@link 
   * jchrest.architecture.Node} in the hypothesis position of the {@link 
   * jchrest.architecture.Stm} {@link jchrest.lib.Modality} equal to the result
   * of invoking  {@link jchrest.architecture.Node#getModality()} on the {@code 
   * nodeToAdd}.  If there is a {@link jchrest.architecture.Node} in the 
   * hypothesis position of the relevant {@link jchrest.architecture.Stm} {@link 
   * jchrest.lib.Modality}, invoking {@link 
   * jchrest.architecture.Node#isRootNode()} upon it must return {@link 
   * java.lang.Boolean#FALSE} for a semantic link to be created.
   * 
   * If the {@link jchrest.architecture.Node} is added to a {@link 
   * jchrest.architecture.Stm}, the attention clock of {@link #this} will be 
   * modified and likewise for the cognition clock if an association is created. 
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
    this.printDebugStatement("===== Chrest.addToStm() =====");
    
    this.printDebugStatement(
      "- Attempting to add node " + nodeToAdd.getReference() + " to " +
      nodeToAdd.getModality() + " STM.  Checking if " + 
      "attention resource is free at time function invoked i.e. is the " +
      "current attention clock value (" + this._attentionClock + ") <= the " + 
      "time this function is invoked (" + time + ")?"
    );
    
    if(this.isAttentionFree(time)){
      
      this.printDebugStatement(
        "  ~ Attention resource is free so node " + nodeToAdd.getReference() + 
        " will be added to STM at time " + (time + this._timeToUpdateStm) + 
        " (the current time, " + time + ", plus the time it takes to update STM (" + 
        this._timeToUpdateStm + ")."
      );
      
      Modality nodeToAddModality = nodeToAdd.getModality();
      Stm stm = this.getStm(nodeToAddModality);
      
      // TODO: Check if this is the best place
      // Idea is that nodeToAdd's filled slots are cleared when put into STM, 
      // are filled whilst in STM, and forgotten when it leaves.
      nodeToAdd.clearFilledSlots(time); 
      
      if(stm.add(nodeToAdd, time + this._timeToUpdateStm)){
        
        this.printDebugStatement(
          "- STM addition successful, setting the current time to the " +
          "time node " + nodeToAdd.getReference() + " was added to STM (" + 
          (time + this._timeToUpdateStm) + ") and setting the attention clock " +
          "to this value."
        );
        time += this._timeToUpdateStm;
        this._attentionClock = time;
        setChanged ();
        if (!_frozen) notifyObservers ();

        boolean nonSemanticAssociationsCreated = false;
        if(nodeToAddModality.equals(Modality.VERBAL)){
          List<Node> visualStmContents = this.getStm(Modality.VISUAL).getContents(time);
          if(visualStmContents != null && !visualStmContents.isEmpty()){
            Node visualStmHypothesis = visualStmContents.get(0);
            this.printDebugStatement(
              "- The Node to add has " + Modality.VERBAL.toString() + " " +
              "modality and visual STM has Node " + 
              visualStmHypothesis.getReference() + " as its hypothesis.  " +
              "Attempting to associate these Nodes."
            );
            nonSemanticAssociationsCreated = this.associateNodes(visualStmHypothesis, nodeToAdd, time);
          }
        }

        if(!nonSemanticAssociationsCreated){
          List<Node> stmContents = stm.getContents(time);
          if(stmContents != null && !stmContents.isEmpty()){
            Node stmHypothesis = stmContents.get(0);
            
            this.printDebugStatement(
              "- The Node to add has " + nodeToAdd.getModality().toString() + " " +
              "modality and " + nodeToAdd.getModality().toString() + " STM has Node " + 
              stmHypothesis.getReference() + " as its hypothesis.  " +
              "Attempting to associate these Nodes if they aren't the same Node."
            );
            if(nodeToAdd.getReference() != stmHypothesis.getReference()){
              this.associateNodes(stmHypothesis, nodeToAdd, time);
            }
          }
        }
        
        this.printDebugStatement("===== RETURN =====");
        return true;
      }
      else{
        this.printDebugStatement("  ~ STM addition unsuccessful.");
      }
    }
    else{
      this.printDebugStatement("  ~ Attention resource isn't free");
    }
    
    this.printDebugStatement("===== RETURN =====");
    return false;
  }
  
  /**
   * Modifies the attention clock of {@link #this}.
   * 
   * If the attention resource is free at the time this function is requested,
   * the hypothesis (the first {@link jchrest.architecture.Node} in a {@link 
   * jchrest.architecture.Stm} modality) in the relevant {@link 
   * jchrest.architecture.Stm} modality associated with {@link #this} is 
   * replaced with the {@link jchrest.architecture.Node} specified.
   * 
   * The time the new hypothesis is added to the relevant {@link 
   * jchrest.architecture.Stm}, <i>t</i>, is equal to the time this function is 
   * invoked plus the time specified to add update short-term memory (see {@link 
   * #getTimeToUpdateStm()}). The attention clock of {@link #this} will also be
   * set to <i>t</i>.
   * 
   * @param replacement
   * @param time 
   */
  public void replaceStmHypothesis(Node replacement, int time){
    this.printDebugStatement("===== Chrest.replaceStmHypothesis() =====");
    this.printDebugStatement(
      "- Attempting to replace the " + replacement.getModality().toString() + 
      " hypothesis with Node " + replacement.getReference() + " at time " + time
    );
    
    this.printDebugStatement("- Hypothesis can be replaced if attention is free at this time");
    if(this.isAttentionFree(time)){
      this.printDebugStatement(
        "  ~ Attention is free at time " + time + " so the relevant " +
        "STM hypothesis will be replaced at " + time + " plus the time taken " +
        "to update STM (" + this._timeToUpdateStm  + "), i.e. at time " + (time + 
        this._timeToUpdateStm)
      );
      time += this._timeToUpdateStm;
      Stm stmToReplaceHypothesisIn = this.getStm(replacement.getModality());
      if(stmToReplaceHypothesisIn.replaceHypothesis(replacement, time)){
        this._attentionClock = time;
      }
    }
    else{
      this.printDebugStatement(
        "  ~ Attention is not free at time " + time + " so the relevant " +
        "STM hypothesis will not be replaced"
      );
    }
    
    this.printDebugStatement("===== RETURN Chrest.replaceStmHypothesis() =====");
  }
  
  /**
   * Selects an {@link jchrest.lib.Modality#ACTION} {@link 
   * jchrest.architecture.Node} that is linked to via. a production from one of
   * the {@link jchrest.lib.Modality#VISUAL} {@link jchrest.architecture.Node 
   * Nodes} currently in {@link jchrest.lib.Modality#VISUAL} {@link 
   * jchrest.architecture.Stm}.
   * 
   * The {@link jchrest.lib.Modality#ACTION} {@link jchrest.architecture.Node} 
   * selected is selected using two rounds of {@link 
   * org.uncommons.watchmaker.framework.selection.RouletteWheelSelection}.  The
   * first round considers all {@link jchrest.lib.Modality#VISUAL} {@link 
   * jchrest.architecture.Node Nodes} currently in {@link 
   * jchrest.lib.Modality#VISUAL} {@link jchrest.architecture.Stm} that contain
   * productions.  These {@link jchrest.lib.Modality#VISUAL} {@link 
   * jchrest.architecture.Node Nodes} are selected between based upon their 
   * total information content, i.e. the result of invoking {@link 
   * jchrest.lib.ListPattern#size()} on the result of invoking {@link 
   * jchrest.architecture.Node#getAllInformation(int)} on them.  Therefore, 
   * {@link jchrest.lib.Modality#VISUAL} {@link jchrest.architecture.Node Nodes}
   * that are better known have a greater chance of being selected. After 
   * selecting a {@link jchrest.lib.Modality#VISUAL} {@link 
   * jchrest.architecture.Node}, its productions are selected between based upon 
   * their values. 
   * 
   * @return A two-element {@link jchrest.architecture.Node} {@link 
   * java.util.Arrays Array}.  If no {@link jchrest.lib.Modality#VISUAL} or 
   * {@link jchrest.lib.Modality#ACTION} {@link jchrest.architecture.Node} can 
   * be selected, both elements will equal {@code null}. Otherwise, the first 
   * element will equal the {@link jchrest.lib.Modality#VISUAL} {@link 
   * jchrest.architecture.Node} selected and the second element will equal the
   * {@link jchrest.lib.Modality#ACTION} {@link jchrest.architecture.Node} 
   * selected.
   */
  public Node[] generateActionUsingVisualPatternRecognition(int time){
    this.printDebugStatement("===== Chrest.generateActionUsingVisualPatternRecognition() =====");
    this.printDebugStatement("- Invoked at time " + time);
    Node visualNodeSelected = null;
    Node actionNodeSelected = null;
    Node[] visualAndActionNodesSelected = new Node[2];
    RouletteWheelSelection rws = new RouletteWheelSelection();
    
    this.printDebugStatement(
      "- Checking if the following statements are both true:" +
      "\n  ~ This model exists at the time specified: " + (this.getCreationTime() <= time) +
      "\n  ~ The attention of this model is free at the time specified: " + this.isAttentionFree(time)
    );
    if(this.getCreationTime() <= time && this.isAttentionFree(time)){
      this.printDebugStatement("    + All OK.");
      
      List<Node> visualStmContents = this.getStm(Modality.VISUAL).getContents(time);
      
      //Visual STM contents can be assumed to be not null safely since null is 
      //only returned if STM doesn't exist at the time its contents are 
      //retrieved.  However, if program control gets to here, its been assured 
      //that the model exists at this time and STMs are created when the model
      //is.  Therefore, null should not be returned.
      this.printDebugStatement("- Checking if visual STM is empty at this time");
      if(!visualStmContents.isEmpty()){
        this.printDebugStatement("  ~ Visual STM is not empty at this time");
        
        /////////////////////////////////////////////////
        ///// GET VISUAL STM NODES WITH PRODUCTIONS /////
        /////////////////////////////////////////////////
        
        this.printDebugStatement("- Getting any visual STM Nodes with productions");
        ArrayList<EvaluatedCandidate<Node>> visualNodesToSelectFrom = new ArrayList();
        for(Node visualStmNode : visualStmContents){
          time += this._timeToRetrieveItemFromStm;
          this.printDebugStatement(
            "  ~ Retrieved visual Node with ref " + visualStmNode.getReference() +
            ", contents: " + visualStmNode.getContents().toString() + " and " +
            "image: " + visualStmNode.getImage(time).toString() + ".  Incremented " +
            "current time by the time taken to retrieve a Node from STM (" + 
            this._timeToRetrieveItemFromStm + ").  Current time now equal to: " + time
          );
          
          if(!visualStmNode.getProductions(time).isEmpty()){
            time += this._timeToCheckNodeForProductions;
            this.printDebugStatement(
              "    + Node has productions.  Incremented current time by time " +
              "taken to check a Node for productions (" + 
              this._timeToCheckNodeForProductions + ").  Current time now equal " +
              "to: " + time
            );
            visualNodesToSelectFrom.add(new EvaluatedCandidate(visualStmNode, visualStmNode.getAllInformation(time).size()));
          }
          else{
            this.printDebugStatement("    + Node has no productions, checking next visual STM Node");
          }
          
          this._attentionClock = time;
        }
        this.printDebugStatement(
          "- Attention clock set to " + this._attentionClock + " after " +
          "identifying visual STM Nodes with produtions"
        );
        
        //////////////////////////////////////////////////////
        ///// SELECT A VISUAL NODE TO GET AN ACTION FROM /////
        //////////////////////////////////////////////////////
        
        if(!visualNodesToSelectFrom.isEmpty()){
          this.printDebugStatement("- Selecting a visual Node for pattern-recognition from the following: " + visualNodesToSelectFrom.toString());
          List<Node> visualNodesSelected = rws.select(visualNodesToSelectFrom, true, 1, new Random());
          if(!visualNodesSelected.isEmpty()){
            visualNodeSelected = visualNodesSelected.get(0);
            this.printDebugStatement("  ~ Visual Node with reference " + visualNodeSelected.getReference() + " was selected");
          }
          else{
            this.printDebugStatement("  ~ No visual Node was selected, exiting");
          }
        }
        else{
          this.printDebugStatement("- No visual STM Nodes contain productions, exiting");
        }
        
        /////////////////////////////////////////////////////
        ///// GET ACTION FROM VISUAL NODE'S PRODUCTIONS /////
        /////////////////////////////////////////////////////
        
        if(visualNodeSelected != null){
          ArrayList<EvaluatedCandidate<Node>> productionsToSelectFrom = new ArrayList();
          for(Entry<Node, Double> productions : visualNodeSelected.getProductions(time).entrySet()){
            productionsToSelectFrom.add(new EvaluatedCandidate(productions.getKey(), productions.getValue()));
          }
          
          this.printDebugStatement(
            "- Selecting an action from Node " + visualNodeSelected.getReference() + 
            "'s productions at time " + time + ", i.e." + productionsToSelectFrom.toString()
          );
          List<Node> actionNodesSelected = rws.select(productionsToSelectFrom, true, 1, new Random());
          
          if(!actionNodesSelected.isEmpty()){
            actionNodeSelected = actionNodesSelected.get(0);
            this.printDebugStatement(
              "  ~ Action Node with ref " + actionNodeSelected.getReference() +
              ", contents " + actionNodeSelected.getContents().toString() + 
              "and image " + actionNodeSelected.getImage(time).toString() + 
              "selected."
            );
          }
          else{
            this.printDebugStatement("  ~ No action Node selected, exiting");
          }
        }
      }
      else{
        this.printDebugStatement("  ~ Visual STM is empty at this time, exiting");
      }
    } 
    else{
      this.printDebugStatement("    + A statement evaluated to false, exiting.");
    }
    
    ///////////////////////////////
    ///// ASSIGN RETURN VALUE /////
    ///////////////////////////////
    
    if(visualNodeSelected != null && actionNodeSelected != null){
      visualAndActionNodesSelected[0] = visualNodeSelected;
      visualAndActionNodesSelected[1] = actionNodeSelected;
    }
    
    this.printDebugStatement("- Returning " + Arrays.toString(visualAndActionNodesSelected));
    this.printDebugStatement("===== RETURN Chrest.generateActionUsingVisualPatternRecognition() =====");
    return visualAndActionNodesSelected;
  }
  
  /********************************/
  /**** Template Functionality ****/
  /********************************/
  
  /**
   * Instruct {@link #this} to create templates throughout the entirety of its 
   * visual long-term memory modality at the time specified.
   * 
   * Templates will only be created if all the following statements are true:
   * 
   * <ul>
   *  <li>{@link #this} is "alive" at the time specified.</li> 
   *  <li>{@link #this} can construct templates.</li>
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
    
    List<Link> children = node.getChildren(time);
    if(children != null){
      for (Link link : children) {
        this.makeTemplates (link.getChildNode(), time);
      }
    }
  }

  //TODO: Organise and check all code below this point.
  
  /**********************************/
  /**** Perception Functionality ****/
  /**********************************/
  
  /**
   * @return The time taken for the {@link jchrest.architecture.Perceiver} 
   * associated with {@link #this} to perform a saccade, i.e. to move the 
   * {@link jchrest.architecture.Perceiver Perceiver's} focus to a particular
   * {@link jchrest.lib.Square} in a {@link jchrest.lib.Scene}.
   */
  public int getSaccadeTime() {
    return _saccadeTime;
  }

  /**
   * Sets the time taken for the {@link jchrest.architecture.Perceiver} 
   * associated with {@link #this} to perform a saccade, i.e. to move the 
   * {@link jchrest.architecture.Perceiver Perceiver's} focus to a particular
   * {@link jchrest.lib.Square} in a {@link jchrest.lib.Scene}.
   * 
   * @param saccadeTime 
   */
  public void setSaccadeTime(int saccadeTime) {
    this._saccadeTime = saccadeTime;
  }
  
  /**
   * @param time
   * 
   * @return The next {@link jchrest.domainSpecifics.Fixation Fixations} that 
   * are scheduled to be made by {@link #this} or {@code null} if {@link #this}
   * was not created at the {@code time} specified.
   */
  public List<Fixation> getScheduledFixations(int time){
    Entry<Integer, Object> fixationsToMakeAtTime = this._fixationsScheduled.floorEntry(time);
    return fixationsToMakeAtTime == null ? null : (List<Fixation>)fixationsToMakeAtTime.getValue();
  }
  
  /**
   * Creates/performs {@link jchrest.domainSpecifics.Fixation Fixations} 
   * in accordance with {@link #this#getDomainSpecifics()}, the {@code scene} 
   * specified and the {@code time} specified. <b>After initialising a new 
   * {@link jchrest.domainSpecifics.Fixation} set, it is imperative that this 
   * method is called every millisecond in isolation until {@link 
   * java.lang.Boolean#TRUE} is returned.</b>
   * <p>
   * The method proceeds by invoking the following functions in the order 
   * specified:
   * <ol type="1">
   *  <li>{@link jchrest.architecture.Chrest#getInitialFixation(int)}</li>
   *  <li>
   *    {@link jchrest.architecture.Chrest#performScheduledFixations(java.util.List, 
   *    jchrest.domainSpecifics.Scene, int)}
   *  </li>
   *  <li>{@link jchrest.architecture.Chrest#tagVisualSpatialFieldObjectsFixatedOnAsRecognised(jchrest.domainSpecifics.Fixation)</li>
   *  <li>
   *    Check if {@link jchrest.domainSpecifics.Fixation} set is now complete, 
   *    i.e. does the number of {@link jchrest.domainSpecifics.Fixation 
   *    Fixations} performed equal the result of invoking {@link 
   *    jchrest.domainSpecifics.DomainSpecifics#getMaximumFixationsInSet()} in
   *    context of {@link #this#getDomainSpecifics()} or does invoking {@link 
   *    jchrest.domainSpecifics.DomainSpecifics#isFixationSetComplete(int)} in
   *    context of {@link #this#getDomainSpecifics()} return {@link 
   *    java.lang.Boolean#TRUE}?
   *    <ul>
   *      <li>{@link jchrest.domainSpecifics.Fixation} set complete</li>
   *      <ol type="1">
   *        <li>{@link jchrest.architecture.Chrest#tagUnrecognisedVisualSpatialFieldObjectsAfterFixationSetComplete(jchrest.domainSpecifics.Fixation)}</li>
   *        <li>
   *          {@link jchrest.architecture.Perceiver#learnFromNewFixations(int)} 
   *          (in context of {@link #this#getPerceiver()}).
   *        </li>
   *        <li>
   *          If {@code constructVisualSpatialField} specifies {@link 
   *          java.lang.Boolean#TRUE}, {@link 
   *          jchrest.architecture.Chrest#constructVisualSpatialField(int)} is
   *          invoked.
   *        </li>
   *      </ol>
   *      <li>{@link jchrest.domainSpecifics.Fixation} set not complete</li>
   *      <ol type="1">
   *        <li>{@link jchrest.architecture.Chrest#scheduleFixationsForPerformance(java.util.List, int)</li>
   *        <li>{@link jchrest.architecture.Chrest#getNonInitialFixation(int, int, int)}</li>
   *      </ol>
   *    </ul>
   *  </li>
   * </ol>
   * 
   * @param scene The {@link jchrest.domainSpecifics.Scene} that will be used
   * to schedule or make a new {@link jchrest.domainSpecifics.Fixation}.
   * 
   * @param constructVisualSpatialField
   * 
   * @param time
   * 
   * @return Whether a {@link jchrest.domainSpecifics.Fixation} set is being 
   * performed.  Note that this does not take into account when attention is
   * free.
   */
  //TODO: Should a new Fixation immediately be scheduled when attention is free
  //      or should a Fixation be completely performed and STM allowed to update
  //      before the next Fixation is performed?  Currently, it may be the case
  //      that only the last Fixation performed will cause visual STM to be 
  //      updated.
  public boolean scheduleOrMakeNextFixation(Scene scene, boolean constructVisualSpatialField, int time){
    this.printDebugStatement("===== Chrest.scheduleOrMakeNextFixation() =====");
    
    this.printDebugStatement("- Checking if model exists at the time the function is requested (" + time + ")");
    if(this._creationTime <= time){
      this.printDebugStatement("   ~ Model exists at the time the function is requested");
      
      List<Fixation> fixationsScheduled = this.getScheduledFixations(time);
      Perceiver perceiver = this.getPerceiver();
      this.printDebugStatement("- Fixations scheduled:");
      if(this._debug){
        for(Fixation fixationScheduled : fixationsScheduled){
          this._debugOutput.println(fixationScheduled.toString());
        }
      }
      
      ///// ADD INITIAL FIXATION /////  
      this.printDebugStatement("- Checking if an initial Fixation should be scheduled");
      Fixation initialFixation = this.getInitialFixation(time);
      if(initialFixation != null) fixationsScheduled.add(initialFixation);
      
      //Only continue if performing a Fixation set.  If a Fixation set has 
      //completed but a new one hasn't started, this conditional won't pass 
      //since this._performingFixations won't have been set to true by 
      //getInitialFixation() above.
      if(this._performingFixations){
      
        ///// PERFORM FIXATIONS /////
        this.printDebugStatement("- Performing Fixations that are due to be performed");
        List<Fixation> fixationsAttempted = this.performScheduledFixations(fixationsScheduled, scene, time);

        ///// TAG RECOGNISED VisualSpatialFieldObjects /////
        this.printDebugStatement(
          "- Removing any Fixations that were attempted (regardless of the " +
          "success of their performance) from the Fixations scheduled and any " +
          "that have been performed will be added to a list of Fixations performed"
        );
        List<Fixation> fixationsPerformed = new ArrayList();
        for(int i = 0; i < fixationsScheduled.size(); i++){
          Fixation fixationScheduled = fixationsScheduled.get(i);
          for(Fixation fixationAttempted : fixationsAttempted){

            if(fixationScheduled.getReference().equals(fixationAttempted.getReference())){
              if(fixationAttempted.hasBeenPerformed()){
                fixationsPerformed.add(fixationAttempted);
              }

              fixationsScheduled.remove(i);
            }
          }
        }
      
        this.printDebugStatement("- Fixations scheduled to be performed now: " + fixationsScheduled.toString());
        this.printDebugStatement("- Fixations that were performed: " + fixationsPerformed.toString());
        this.printDebugStatement("- Attempting to tag any VisualSpatialFieldObjects that may have been fixated on when fixation was performed");
        for(Fixation fixationPerformed : fixationsPerformed){
          this.tagVisualSpatialFieldObjectsFixatedOnAsRecognised(fixationPerformed);
        }

        //////////////////////////////////////
        ///// FIXATION SET NOW COMPLETE? /////
        //////////////////////////////////////
      
        //this.performScheduledFixations() will have updated the Fixations 
        //attempted data structure of the Perceiver associated with this CHREST
        //model so fixation set completion can now be checked.
        this.printDebugStatement(
          "- Checking if Fixation set complete: have the maximum number " +
          "of Fixations been attempted (Fixations attempted: " + 
          perceiver.getFixations(time).size() + ", maximum # Fixations that " + 
          "can be attempted: " + this.getDomainSpecifics().getMaximumFixationsInSet() + 
          ")" + " or does the DomainSpecifics (" + this.getDomainSpecifics().getClass().getSimpleName() + 
          ") specify that the Fixation set is now complete?"
        );
            
        if(
          this._fixationsAttemptedInCurrentSet >= this.getDomainSpecifics().getMaximumFixationsInSet() ||
          this.getDomainSpecifics().isFixationSetComplete(time)
        ){
          /////////////////////////////////
          ///// FIXATION SET COMPLETE /////
          /////////////////////////////////
          
          this.printDebugStatement("  ~ Fixation set complete");
          this._performingFixations = false;

          ///// TAG UNRECOGNISED VisualSpatialFieldObjects /////
          this.printDebugStatement(
            "- Tagging any VisualSpatialFieldObjects that were not recognised " +
            "during this Fixation set as being unrecognised"
          );
          List<Fixation> perceiverFixations = this.getPerceiver().getFixations(time);
          this.tagUnrecognisedVisualSpatialFieldObjectsAfterFixationSetComplete(perceiverFixations.get(perceiverFixations.size() - 1));

          ///// LEARN FROM FIXATIONS /////
          this.printDebugStatement("- Learning from Fixations performed");
          perceiver.learnFromNewFixations(time);

          ///// CONSTRUCT VisualSpatialField ///// 
          this.printDebugStatement("- Constructing VisualSpatialField using Fixations if requested");
          if(constructVisualSpatialField){
            this.printDebugStatement("  ~ VisualSpatialField should be constructed at time when attention is free (" + this._attentionClock + ")");
            this.constructVisualSpatialField(this._attentionClock);
          }
          else{
            this.printDebugStatement(" ~ VisualSpatialField will not be constructed");
          }

          this._recognisedVisualSpatialFieldObjectIdentifiers.clear();
          fixationsScheduled.clear();
          this._fixationsAttemptedInCurrentSet = 0;
        }
        else{
          /////////////////////////////////////
          ///// FIXATION SET NOT COMPLETE /////
          /////////////////////////////////////
          this.printDebugStatement("  ~ Fixation set not complete");

          ///// SCHEDULE FIXATIONS FOR PERFORMANCE /////
          this.printDebugStatement("- Scheduling any Fixations to make for performance");
          this.scheduleFixationsForPerformance(fixationsScheduled, time);

          ///// ADD NEW FIXATIONS /////
          List<Fixation> perceiverFixations = this.getPerceiver().getFixations(time);
          int numberPerceiverFixations = (perceiverFixations == null ? 0 : perceiverFixations.size());
          Fixation nonInitialFixation = this.getNonInitialFixation(time, fixationsScheduled.size(), numberPerceiverFixations);
          if(nonInitialFixation != null) fixationsScheduled.add(nonInitialFixation);
        }

        this._fixationsScheduled.put(time, fixationsScheduled);
      }
      else {
        this.printDebugStatement("  ~ Not currently performing a Fixation set at the moment, exiting");
      }
    }
    else{
      this.printDebugStatement("   ~ Model does not exist at the time the function is requested, exiting.");
    }

    this.printDebugStatement("- Returning boolean " + this._performingFixations);
    this.printDebugStatement("===== RETURN =====");
    return this._performingFixations;
  }
  
  /**
   * Designed to be used by {@link #this#scheduleOrMakeNextFixation(
   * jchrest.domainSpecifics.Scene, boolean, int)}.
   * 
   * @param time
   * 
   * @return The result of invoking {@link 
   * jchrest.domainSpecifics.DomainSpecifics#getInitialFixationInSet(int)} on 
   * the result of {@link #this#getDomainSpecifics()} if {@link 
   * #this#isAttentionFree(int)} returns {@link java.lang.Boolean#TRUE} when 
   * checked against {@code time} and {@link #this#_performingFixations} is set
   * to {@link java.lang.Boolean#FALSE}.  Otherwise {@code null} is returned.
   * <p>
   * If a {@link jchrest.domainSpecifics.Fixation} is returned, the following
   * variables are also affected:
   * <ul>
   *  <li>
   *    The attention of {@link #this} will be consumed until the time specified 
   *    by invoking {@link jchrest.domainSpecifics.Fixation#getTimeDecidedUpon()} 
   *    on the {@link jchrest.domainSpecifics.Fixation} returned.
   *  </li>
   *  <li>
   *    {@link #this#_performingFixations} will be set to {@link 
   *    java.lang.Boolean#TRUE}.
   *  </li>
   *  <li>
   *    {@link jchrest.architecture.Perceiver#clearFixations(int)} will be 
   *    invoked on the {@link jchrest.architecture.Perceiver} associated with
   *    {@link #this} at the time specified.  Required since an {@link 
   *    java.lang.IllegalStateException} may be thrown by {@link 
   *    jchrest.architecture.Perceiver#learnFromNewFixations(int)} when invoked
   *    by {@link 
   *    jchrest.architecture.Chrest#scheduleOrMakeNextFixation(jchrest.domainSpecifics.Scene, 
   *    boolean, int)} if a {@link jchrest.domainSpecifics.Fixation} is 
   *    performed successfully and the {@link jchrest.lib.Square} or {@link 
   *    jchrest.domainSpecifics.SceneObject} fixated on has been fixated on by
   *    another {@link jchrest.domainSpecifics.Fixation} in a previous {@link 
   *    jchrest.domainSpecifics.Fixation} set.  Essentially, this check should
   *    only apply to {@link jchrest.domainSpecifics.Fixation Fixations} in the
   *    same set so the {@link jchrest.architecture.Perceiver#clearFixations(int)}
   *    is required.
   *  </li>
   * </ul>
   */
  private Fixation getInitialFixation(int time){
    this.printDebugStatement("===== Chrest.scheduleInitialFixation() =====");
    
    if(this.debug()){
      this.printDebugStatement("- Attempting to get an initial Fixation.  This will occur if the following all evaluate to true:");
      this.printDebugStatement("  ~ Attention is free at time this function is invoked (" + time + "): " + this.isAttentionFree(time));
      this.printDebugStatement("  ~ This CHREST model is not currently performing Fixations: " + !this._performingFixations);
    }
    
    Fixation initialFixation = null;
    
    if(this.isAttentionFree(time) && !this._performingFixations){
      this.printDebugStatement("- All checks evaluate to true");
      
      initialFixation = this.getDomainSpecifics().getInitialFixationInSet(time);
      this.getPerceiver().clearFixations(time);
      this._performingFixations = true;
      this._attentionClock = initialFixation.getTimeDecidedUpon();
    }
    
    if(this.debug()){
      this.printDebugStatement("- Initial Fixation to return: " + (initialFixation == null ? "null" : initialFixation.toString()));
      this.printDebugStatement("- Attention clock set to " + this._attentionClock);
      this.printDebugStatement("- CHREST model performing Fixations: " + this._performingFixations);
      this.printDebugStatement("- Number of items in Visual STM: " + this.getStm(Modality.VISUAL).getCount(time));
      this.printDebugStatement("- Number of Fixations attempted by Perceiver: " + this.getPerceiver().getFixations(time).size());
      this.printDebugStatement("- Fixation to learn from in Perceiver: " + this.getPerceiver().getFixationToLearnFrom());
    }
    
    this.printDebugStatement("===== RETURN =====");
    return initialFixation;
  }
  
  /**
   * Designed to be used by {@link #this#scheduleOrMakeNextFixation(
   * jchrest.domainSpecifics.Scene, boolean, int)}.
   * 
   * @param time
   * @param numberFixationsScheduled
   * 
   * @return The result of invoking {@link 
   * jchrest.domainSpecifics.DomainSpecifics#getNonInitialFixationInSet(int)} on 
   * the result of {@link #this#getDomainSpecifics()} if the following all
   * evaluate to {@link java.lang.Boolean#TRUE}:
   * <ul>
   *  <li>
   *    {@link #this#isAttentionFree(int)} returns {@link 
   *    java.lang.Boolean#TRUE} when checked against {@code time}
   *  </li>
   *  <li>
   *    {@link #this#_performingFixations} is set to {@link 
   *    java.lang.Boolean#TRUE}.
   *  </li>
   *  <li>
   *    The {@code numberFixationsScheduled} plus the {@code 
   *    numberFixationsAttempted} specified is less than the result of invoking 
   *    {@link jchrest.domainSpecifics.DomainSpecifics#getMaximumFixationsInSet()}
   *    on the result of {@link #this#getDomainSpecifics()}.
   *  </li>
   *  <li>
   *    Invoking {@link jchrest.domainSpecifics.DomainSpecifics#shouldAddNewFixation(int)}
   *    on the result of {@link #this#getDomainSpecifics()} returns {@link 
   *    java.lang.Boolean#TRUE}.
   *  </li>
   * </ul>
   * If any of the statements above evaluate to {@link java.lang.Boolean#FALSE}, 
   * {@code null} is returned.
   * <p>
   * If a {@link jchrest.domainSpecifics.Fixation} is returned, the attention of 
   * {@link #this} will be consumed until the time specified by invoking {@link 
   * jchrest.domainSpecifics.Fixation#getTimeDecidedUpon()} on the {@link 
   * jchrest.domainSpecifics.Fixation} returned.
   */
  private Fixation getNonInitialFixation(int time, int numberFixationsScheduled, int numberFixationsAttempted){
    this.printDebugStatement("===== Chrest.getNonInitialFixation() =====");
    
    if(this.debug()){
      this.printDebugStatement("- Attempting to get a non initial Fixation. This will occur if the following all evaluate to true:");
      this.printDebugStatement("  ~ Attention is free at time this function is invoked (" + time + "): " + this.isAttentionFree(time));
      this.printDebugStatement("  ~ This CHREST model is currently performing Fixations: " + this._performingFixations);
      this.printDebugStatement(
        "  ~ Number of fixations scheduled (" + numberFixationsScheduled + ") plus " +
        "number fixations attempted (" + numberFixationsAttempted + ") is less " +
        "than the maximum number of fixations in this domain (" + 
        this.getDomainSpecifics().getMaximumFixationsInSet() + "): " + 
        (numberFixationsScheduled + numberFixationsAttempted < this.getDomainSpecifics().getMaximumFixationsInSet())
      );
      this.printDebugStatement(
        "  ~ The domain stipulates that a new fixation should be added (" + 
        this.getDomainSpecifics().shouldAddNewFixation(time) + ")"
      );
    }
    
    Fixation nonInitialFixation = null;

    if(
      this.isAttentionFree(time) &&
      this._performingFixations &&
      numberFixationsScheduled + numberFixationsAttempted < this.getDomainSpecifics().getMaximumFixationsInSet() &&
      this.getDomainSpecifics().shouldAddNewFixation(time)
    ){
      nonInitialFixation = this.getDomainSpecifics().getNonInitialFixationInSet(time);
      this._attentionClock = nonInitialFixation.getTimeDecidedUpon();
    }
    
    this.printDebugStatement("- Non-initial Fixation to return: " + (nonInitialFixation == null ? "null" : nonInitialFixation.toString()));
    this.printDebugStatement("- Attention clock set to: " + this._attentionClock);
    this.printDebugStatement("===== RETURN =====");
    return nonInitialFixation;
  }
  
  /**
   * Designed to be used by {@link #this#scheduleOrMakeNextFixation(
   * jchrest.domainSpecifics.Scene, boolean, int)}.
   * <p>
   * This function will attempt to invoke {@link 
   * jchrest.domainSpecifics.Fixation#perform(jchrest.domainSpecifics.Scene, 
   * int)} on the first {@link jchrest.domainSpecifics.Fixation} in {@code 
   * fixationsScheduled} whose value for {@link 
   * jchrest.domainSpecifics.Fixation#getPerformanceTime()} equals the {@code 
   * time} specified.  If {@link 
   * jchrest.domainSpecifics.Fixation#perform(jchrest.domainSpecifics.Scene, 
   * int)} returns {@link java.lang.Boolean#TRUE}, {@link 
   * #this#_fixationsAttemptedInCurrentSet} is incremented by 1 and the 
   * following statements are checked. If any evaluate to {@link 
   * java.lang.Boolean#TRUE}, {@link 
   * jchrest.architecture.Perceiver#learnFromNewFixations(int)} is invoked:
   * <ul>
   *  <li>
   *    The {@link jchrest.domainSpecifics.Fixation} has fixated on a {@link 
   *    jchrest.lib.Square} in the domain that has previously been fixated on 
   *    by another {@link jchrest.domainSpecifics.Fixation} in the current 
   *    fixation set, i.e. a {@link jchrest.domainSpecifics.Fixation} in {@link 
   *    jchrest.architecture.Perceiver#getFixationsPerformed(int)}.
   *  </li>
   *  <li>
   *    The {@link jchrest.domainSpecifics.Fixation} has fixated on a {@link 
   *    jchrest.domainSpecifics.SceneObject} in the domain that has previously 
   *    been fixated on by another {@link jchrest.domainSpecifics.Fixation} in 
   *    the current fixation set, i.e. a {@link 
   *    jchrest.domainSpecifics.Fixation} in {@link 
   *    jchrest.architecture.Perceiver#getFixationsPerformed(int)}.
   *  </li>
   *  <li>
   *    Invoking {@link 
   *    jchrest.domainSpecifics.DomainSpecifics#shouldLearnFromNewFixations(
   *    int)} in context of {@link #this#getDomainSpecifics()} returns {@link 
   *    java.lang.Boolean#TRUE}.
   *  </li>
   * </ul>
   * The first two statements trigger this behaviour since an {@link 
   * java.lang.IllegalStateException} is thrown by {@link 
   * jchrest.architecture.Perceiver#learnFromNewFixations(int)} if more than 
   * one {@link jchrest.domainSpecifics.Fixation} in the set of {@link 
   * jchrest.domainSpecifics.Fixation Fixations} to learn from has fixated on 
   * the same domain {@link jchrest.lib.Square} or {@link 
   * jchrest.domainSpecifics.SceneObject}.  Thus, by checking if these 
   * conditions apply before the {@link jchrest.domainSpecifics.Fixation} just 
   * performed is added, an {@link java.lang.IllegalStateException} will not be 
   * thrown.
   * <p>
   * Regardless of the outcome of {@link 
   * jchrest.domainSpecifics.Fixation#perform(jchrest.domainSpecifics.Scene, 
   * int)}, the {@link jchrest.domainSpecifics.Fixation} will be removed from
   * {@code fixationsScheduled} and passed as input to {@link 
   * jchrest.architecture.Perceiver#addFixation(
   * jchrest.domainSpecifics.Fixation)} for the {@link 
   * jchrest.architecture.Perceiver} associated with {@link #this}. 
   * <p>
   * Any other {@link jchrest.domainSpecifics.Fixation Fixations} whose
   * {@link jchrest.domainSpecifics.Fixation#getPerformanceTime()} equals the 
   * {@code time} specified will be "abandoned", i.e. {@link 
   * jchrest.domainSpecifics.Fixation#perform(jchrest.domainSpecifics.Scene, 
   * int)} will not be invoked on them and they will be removed from {@code 
   * fixationsScheduled}.
   * 
   * @param fixationsScheduled
   * @param scene
   * @param time
   * 
   * @return The {@link jchrest.domainSpecifics.Fixation Fixations} in {@code 
   * fixationsScheduled} whose {@link 
   * jchrest.domainSpecifics.Fixation#getPerformanceTime()} is equal to {@code 
   * time}.
   */
  private List<Fixation> performScheduledFixations(List<Fixation> fixationsScheduled, Scene scene, int time){
    this.printDebugStatement("===== Chrest.performScheduledFixations() =====");
    this.printDebugStatement("- Fixations to process: " + fixationsScheduled.toString());
    boolean functionHasAttemptedToPerformFixation = false;
    
    ArrayList<Fixation> fixationsWithPerformanceTimeEqualToTimeMethodInvoked = new ArrayList();
    for(Fixation fixation : fixationsScheduled){
      
      this.printDebugStatement("- Checking if the following Fixation is to be performed now (" + time + "): " + fixation.toString());

      Integer performanceTime = fixation.getPerformanceTime();
      if(performanceTime != null){
        
        if(performanceTime == time){
          this.printDebugStatement("  ~ Fixation is to be performed now");
          
          this.printDebugStatement(
            "- Adding Fixation to the Fixations whose performance time is " +
            "equal to the time this method is invoked."
          );
          fixationsWithPerformanceTimeEqualToTimeMethodInvoked.add(fixation);
 
          this.printDebugStatement(
            "- Checking if this function has not already performed a Fixation (" +
            !functionHasAttemptedToPerformFixation + ").  If not, an attempt " +
            "will be made to perform this Fixation"
          );
          if(!functionHasAttemptedToPerformFixation){
            
            this.printDebugStatement(
              "  ~ This function hasn't performed a Fixation yet.  " +
              "Incrementing the fixations attempted in current set counter " +
              "and attempting to perform the Fixation being processed"
            );
            this._fixationsAttemptedInCurrentSet++;
            functionHasAttemptedToPerformFixation = true;
            
            if(fixation.perform(scene, time)){
              this.printDebugStatement("- Fixation was performed successfully");
              
              /////////////////////////////////////////////////
              ///// SHOULD LEARN FROM PERFORMED FIXATIONS /////
              /////////////////////////////////////////////////
            
              this.printDebugStatement("- Checking if the model should now learn from the Fixations it has performed");
              
              this.printDebugStatement(
                "- Checking if the Fixation just performed fixated on a " +
                "SceneObject or Square already fixated on in this fixation set."
              );
            
              boolean objectOrLocationFixatedOnAgain = false;
              
              //Assume that these are all OK, a null pointer will be thrown 
              //below if not.  If they're not OK then its a CHREST programmer's 
              //fault (something must have not been set correctly in 
              //Fixation.perform() or functions that Fixation.perform() relies 
              //on), nothing at runtime should affect their setting!
              Scene sceneFixatedOn = fixation.getScene();
              SceneObject objectFixatedOn = fixation.getObjectSeen();
              Integer sceneSpecificColFixatedOn = fixation.getColFixatedOn();
              Integer sceneSpecificRowFixatedOn = fixation.getRowFixatedOn();
            
              String identifierForObjectJustFixatedOn = objectFixatedOn.getIdentifier();
              int fixationJustPerformedDomainSpecificCol = sceneFixatedOn.getDomainSpecificColFromSceneSpecificCol(sceneSpecificColFixatedOn);
              int fixationJustPerformedDomainSpecificRow = sceneFixatedOn.getDomainSpecificRowFromSceneSpecificRow(sceneSpecificRowFixatedOn);
              this.printDebugStatement("  ~ Identifier for SceneObject fixated on: " + identifierForObjectJustFixatedOn);
              this.printDebugStatement("  ~ Square fixated on (domain-specific coordinates): (" + fixationJustPerformedDomainSpecificCol + ", " + fixationJustPerformedDomainSpecificRow + ")");

              List<Fixation> mostRecentFixations = this.getPerceiver().getFixations(time);
              for(int j = this.getPerceiver().getFixationToLearnFrom(); j < mostRecentFixations.size(); j++){
                Fixation f = mostRecentFixations.get(j);

                if(f.hasBeenPerformed()){
                  this.printDebugStatement("    + Checking SceneObject and Square fixated on by Fixation " + f.toString());
                  String identifierForObjectFixatedOn = f.getObjectSeen().getIdentifier();
                  int fixationDomainSpecificCol = f.getScene().getDomainSpecificColFromSceneSpecificCol(f.getColFixatedOn());
                  int fixationDomainSpecificRow = f.getScene().getDomainSpecificRowFromSceneSpecificRow(f.getRowFixatedOn());

                  if( 
                    identifierForObjectFixatedOn.equals(identifierForObjectJustFixatedOn) ||
                    (
                      fixationDomainSpecificCol == fixationJustPerformedDomainSpecificCol &&
                      fixationDomainSpecificRow == fixationJustPerformedDomainSpecificRow
                    )
                  ){
                    this.printDebugStatement(
                      "      > SceneObject or Square fixated on in this previously " +
                      "performed Fixation match the SceneObject or Square fixated " +
                      "on by the Fixation just performed.  Stopping checks for " +
                      "repeat fixations on SceneObjects or Squares."
                    );
                    objectOrLocationFixatedOnAgain = true;
                    break;
                  }
                  else{
                    this.printDebugStatement(
                      "      > SceneObject or Square do not match the " +
                      "SceneObject or Square fixated on by the Fixation just " +
                      "performed.  Checking next Fixation performed."
                    );
                  }
                }
              }
              
              boolean shouldLearnFromNewFixations = this.getDomainSpecifics().shouldLearnFromNewFixations(time);
              this.printDebugStatement(
                "- Checking if the domain (" + this.getDomainSpecifics().getClass().getCanonicalName() + 
                ") stipulates that Fixations should be learned from after " +
                "Fixation performance (" + shouldLearnFromNewFixations + ") or " +
                "if the Fixation just performed fixated on a SceneObject/Square " +
                "that was fixated on previosly by a Fixation in the current set (" +
                objectOrLocationFixatedOnAgain + ").  If so, Fixations that have " +
                "been performed but not learned from yet will be learned from."
              );
              if(
                shouldLearnFromNewFixations ||
                objectOrLocationFixatedOnAgain
              ){
                this.printDebugStatement("  ~ Fixations will be learned from");
                this.getPerceiver().learnFromNewFixations(time);
              }
              else{
                this.printDebugStatement("  ~ Fixations will not be learned from");
              }
            }
            else{
              this.printDebugStatement("- Fixation performance unsuccessful");
            }
            
            /////////////////////////////////////
            ///// ADD FIXATION TO PERCEIVER /////
            /////////////////////////////////////

            //Now, add the new Fixation after learning others since this 
            //Fixation may contain a duplicate object/location.  Consequently, 
            //when this function is called again, the "fixation to learn from" 
            //Perceiever index will start from the Fixation just performed and 
            //will not throw a duplicate object/location exception when 
            //"this.getPerceiver().learnFromNewFixations(time);" is called above
            this.printDebugStatement("- Adding Fixation to Perceiver's Fixation data structure");
            this.getPerceiver().addFixation(fixation);
          }
          else{
            this.printDebugStatement(
              "  ~ A Fixation has already been performed so this Fixation will be abandoned."
            );
          }
        }
        else if(performanceTime < time){
          throw new IllegalStateException(
            "The following Fixation was scheduled to be performed at a time " +
            "in the past " + performanceTime + "but wasn't: " + fixation.toString()
          );
        }
        else{
          this.printDebugStatement("  ~ Fixation performance time set, but not reached yet.  Fixation will not be performed.");
        }
      }
      else{
        this.printDebugStatement("  ~ Fixation performance time not set.  Fixation will not be performed.");
      }
    }
    
    this.printDebugStatement("- The following Fixations were attempted: " + fixationsWithPerformanceTimeEqualToTimeMethodInvoked);
    
    this.printDebugStatement("Returning the Fixations attempted");
    this.printDebugStatement("===== RETURN Chrest.performScheduledFixations() =====");
    return fixationsWithPerformanceTimeEqualToTimeMethodInvoked;
  }
  
  /**
   * Designed to be used by {@link #this#scheduleOrMakeNextFixation(
   * jchrest.domainSpecifics.Scene, boolean, int)}.
   * <p>
   * Updates the recognised status of any {@link 
   * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} recognised
   * after the {@code fixation} specified is performed.
   * <p>
   * The method assumes that from the time the {@code fixation} was performed, 
   * {@link jchrest.lib.Modality#VISUAL} {@link jchrest.architecture.Stm} has 
   * not been updated by any other process.  So, it is only {@link 
   * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} that are
   * present in the contents, image and filled item/position slots of {@link 
   * jchrest.architecture.Node Nodes} added to {@link 
   * jchrest.lib.Modality#VISUAL} {@link jchrest.architecture.Stm} from the 
   * result of invoking {@link 
   * jchrest.domainSpecifics.Fixation#getPerformanceTime()} on the {@code 
   * fixation} specified to the result of {@link #this#getAttentionClock()} that
   * have their recognised status set to {@link java.lang.Boolean#TRUE} at the
   * value specified by {@link #this#getAttentionClock()}.
   * <p>
   * If the {@code fixation} specified has not been performed or did not fixate
   * on a {@link jchrest.domainSpecifics.Scene} that represents a {@link 
   * jchrest.architecture.VisualSpatialField} or no new {@link 
   * jchrest.architecture.Node Nodes} have been added between the time the 
   * {@code fixation} specified was performed and the value of {@link 
   * #this#getAttentionClock()}, this method will do nothing.
   * 
   * @param fixation
   */
  private void tagVisualSpatialFieldObjectsFixatedOnAsRecognised(Fixation fixation){
    this.printDebugStatement("===== Chrest.tagVisualSpatialFieldObjectsFixatedOnAsRecognised() =====");
    this.printDebugStatement("- Processing Fixation: " + fixation.toString());
    
    if(fixation.hasBeenPerformed()){
              
      Scene sceneFixatedOn = fixation.getScene();
      VisualSpatialField visualSpatialFieldRepresented = sceneFixatedOn.getVisualSpatialFieldRepresented();
      if(visualSpatialFieldRepresented != null){
        this.printDebugStatement("- Scene fixated on represents a VisualSpatialField");
                
        //Get any new Nodes that may have been recognised by performing 
        //the Fixation.
        List<Node> visualStmBeforeRecognition = this.getStm(Modality.VISUAL).getContents(fixation.getPerformanceTime());
        List<Node> visualStmAfterRecognition = this.getStm(Modality.VISUAL).getContents(this._attentionClock);
        
        List<Node> newNodesRecognised = new ArrayList();
        this.printDebugStatement("- Nodes in visual STM when Fixation performed (references):");
        if(this._debug){
          for(Node node : visualStmBeforeRecognition){
            this._debugOutput.println("  ~ " + node.getReference());
          }
        }
                
        this.printDebugStatement("- Nodes in visual STM after Fixation performed and visual STM updated (references):");
        if(this._debug){
          for(Node node : visualStmAfterRecognition){
            this._debugOutput.println("  ~ " + node.getReference());
          }
        }

        if(visualStmBeforeRecognition == null || visualStmBeforeRecognition.isEmpty()){
          for(Node nodeRecognised : visualStmAfterRecognition){
            newNodesRecognised.add(nodeRecognised);
          }
        }
        else{
          for(Node nodeRecognised : visualStmAfterRecognition){
            if(!visualStmBeforeRecognition.contains(nodeRecognised)){
              newNodesRecognised.add(nodeRecognised);
            }
          }
        }
                
        //Remove root Nodes from newNodesRecognised since these will 
        //cause problems
        for(int n = 0; n < newNodesRecognised.size(); n++){
          if(newNodesRecognised.get(n).isRootNode()) newNodesRecognised.remove(n);
        }

        this.printDebugStatement("- Nodes added after Fixation performance (references):");
        if(this._debug){
          for(Node node : newNodesRecognised){
            this._debugOutput.println("  ~ " + node.getReference());
          }
        }

        //Process each Node recognised.  
        for(Node nodeRecognised : newNodesRecognised){
          this.printDebugStatement("- Processing VisualSpatialFieldObjects in Node " + nodeRecognised.getReference());

          ListPattern objectsRecognised = nodeRecognised.getAllInformation(this._attentionClock);
          this.printDebugStatement("  ~ Objects recognised: " + objectsRecognised.toString());

          //Determining if this CHREST model is learning object locations 
          //relative to the agent equipped with this model.  If this is 
          //the case, convert the agent-relative coordinates that will be 
          //present in the ItemSquarePatterns of the content/image 
          //ListPattern to domain-specific coordinates so that the 
          //relevant VisualSpatialField coordinates can be identified.
          for(PrimitivePattern objectRecognised : objectsRecognised){
            ItemSquarePattern objectRec = (ItemSquarePattern)objectRecognised;
            int col = objectRec.getColumn();
            int row = objectRec.getRow();
            this.printDebugStatement("- Processing " + objectRec.toString());

            if(this.isLearningObjectLocationsRelativeToAgent()){
              Square locationOfCreator = (Square)visualSpatialFieldRepresented.getCreatorDetails(this._attentionClock).get(1);
              int locationOfCreatorCol = visualSpatialFieldRepresented.getDomainSpecificColFromVisualSpatialFieldCol(locationOfCreator.getColumn());
              int locationOfCreatorRow = visualSpatialFieldRepresented.getDomainSpecificRowFromVisualSpatialFieldRow(locationOfCreator.getRow());
              col = locationOfCreatorCol + col;
              row = locationOfCreatorRow + row;
            }

            col = visualSpatialFieldRepresented.getVisualSpatialFieldColFromDomainSpecificCol(col);
            row = visualSpatialFieldRepresented.getVisualSpatialFieldRowFromDomainSpecificRow(row);
            this.printDebugStatement("  ~ VisualSpatialFieldCoordinates referenced: (" + col + ", " + row + ")");

            //Cycle through all VisualSpatialFieldObjects on the 
            //coordinates and check if they are alive and of the same type
            //as that defined by the ItemSquarePattern in the 
            //content/image ListPattern.  If so, tag them as recognised.
            //
            //NOTE: there may be more than one VisualSpatialFieldObject 
            //that is alive and has the same type on the coordinates.  All
            //such VisualSpatialFieldObjects will be tagged as recognised.
            this.printDebugStatement(
              "  ~ Checking if the type of any VisualSpatialFieldObjects on " +
              "these coordinates match the item referenced (" + 
              objectRec.getItem() + ") and if they do, are they also 'alive' " +
              "on the VisualSpatialField"
            );
            List<VisualSpatialFieldObject> coordinateContents = visualSpatialFieldRepresented.getCoordinateContents(col, row);
            for(VisualSpatialFieldObject objectOnVisualSpatialFieldCoordinates : coordinateContents){

              this.printDebugStatement(objectOnVisualSpatialFieldCoordinates.toString());
              if(
                objectOnVisualSpatialFieldCoordinates.isAlive(this._attentionClock) && 
                objectOnVisualSpatialFieldCoordinates.getObjectType().equals(objectRec.getItem())
              ){
                this.printDebugStatement(
                  "    + VisualSpatialFieldObject's type matches and it is alive, " +
                  "setting its recognised status to true at time it is " + 
                  "recognised (" + this._attentionClock + ")"
                );
                objectOnVisualSpatialFieldCoordinates.setRecognised(this._attentionClock, true);
                this._recognisedVisualSpatialFieldObjectIdentifiers.add(objectOnVisualSpatialFieldCoordinates.getIdentifier());
              }
              else{
                this.printDebugStatement(
                  "    + VisualSpatialFieldObject's type does not match or it " +
                  "is not alive. Processing next VisualSpatialFieldObject on " +
                  "the coordinates"
                );
              }
            }
          }
        }
      }
      else{
        this.printDebugStatement("- Scene fixated on does not represent a VisualSpatialField, exiting");
      }
    }
    else{
      this.printDebugStatement("- Fixation has not been performed, exiting");
    }
    
    this.printDebugStatement("===== RETURN =====");
  }
  
  /**
   * Designed to be used by {@link #this#scheduleOrMakeNextFixation(
   * jchrest.domainSpecifics.Scene, boolean, int)}.
   * <p>
   * Sets the recognised status of any {@link 
   * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} to {@link 
   * java.lang.Boolean#FALSE} at the time a {@link 
   * jchrest.domainSpecifics.Fixation} set is complete if the following 
   * statements hold:
   * 
   * <ul>
   *  <li>
   *    The {@link jchrest.lib.VisualSpatialFieldObject} has not been recognised 
   *    during performance of the current {@link 
   *    jchrest.domainSpecifics.Fixation} set.
   *  </li>
   *  <li>
   *    Invoking {@link jchrest.lib.VisualSpatialFieldObject#isAlive(int)} on 
   *    the {@link jchrest.lib.VisualSpatialFieldObject} returns {@link 
   *    java.lang.Boolean#TRUE} when the current {@link 
   *    jchrest.domainSpecifics.Fixation} set is complete.
   *  </li>
   *  <li>
   *    The {@link jchrest.lib.VisualSpatialFieldObject} does not represent the
   *    agent equipped with CHREST.  This should never occur but if it does, it 
   *    can cause serious problems if {@link #this} is learning the locations
   *    of {@link jchrest.domainSpecifics.SceneObject SceneObjects} in a {@link 
   *    jchrest.domainSpecifics.Scene} relative to the location of the agent 
   *    equipped with {@link #this} since changes to the recognised status of a 
   *    {@link jchrest.lib.VisualSpatialFieldObject} entails setting the {@link 
   *    jchrest.lib.VisualSpatialFieldObject#_terminus} meaning that a {@link 
   *    jchrest.lib.VisualSpatialFieldObject} representing the agent equipped 
   *    with {@link #this} may decay in its current {@link 
   *    jchrest.architecture.VisualSpatialField}.
   *  </li>
   * </ul>
   * 
   * This method assumes that previous {@link jchrest.domainSpecifics.Fixation 
   * Fixations} in the set that the {@code lastFixationAttempted} specified is
   * a part of have also been made on the same {@link 
   * jchrest.architecture.VisualSpatialField} as that fixated on by the {@code 
   * lastFixationAttempted} specified.  This means that the time that 
   * unrecognised {@link jchrest.lib.VisualSpatialFieldObject 
   * VisualSpatialFieldObjects} are tagged as being unrecognised at is 
   * determined by considering which of the two values is larger: the result of
   * invoking {@link jchrest.domainSpecifics.Fixation#getPerformanceTime()} or
   * the result of invoking {@link #this#getAttentionClock()}.  This essentially
   * equates to the time that the {@link jchrest.domainSpecifics.Fixation} set
   * is complete (like other methods used by {@link 
   * #this#scheduleOrMakeNextFixation(jchrest.domainSpecifics.Scene, boolean, 
   * int)}), this method assumes that the attention resource is consumed by 
   * methods handling {@link jchrest.domainSpecifics.Fixation} scheduling).  So,
   * if attention is free before the {@link lastFixationAttempted} is performed,
   * the {@link jchrest.domainSpecifics.Fixation} set is complete at this time
   * otherwise, it is complete when attention becomes free.
   * 
   * @param mostRecentFixationPerformed
   */
  private void tagUnrecognisedVisualSpatialFieldObjectsAfterFixationSetComplete(Fixation lastFixationAttempted){
    this.printDebugStatement("===== Chrest.tagUnrecognisedVisualSpatialFieldObjectsAfterFixationSetComplete() =====");
    this.printDebugStatement("- Last Fixation attempted specified as: " + lastFixationAttempted.toString());
    
    VisualSpatialField visualSpatialFieldRepresented = lastFixationAttempted.getScene().getVisualSpatialFieldRepresented();
    
    if(this.debug()){
      this.printDebugStatement("- Method will continue if the following statements evaluate to true:");
      this.printDebugStatement("  ~ This CHREST model is no longer performing Fixations: " + !this._performingFixations);
      this.printDebugStatement("  ~ The last Fixation attempted actually attempted to fixate on a Scene representing a VisualSpatialField: " + (visualSpatialFieldRepresented != null));
    }
    
    if(
      !this._performingFixations && 
      visualSpatialFieldRepresented != null
    ){
      this.printDebugStatement("- All statements evaluate to true, continuing");
      
      this.printDebugStatement(
        "- Determining the latest time when attention will be free so that " +
        "unrecognised VisualSpatialFieldObjects can be tagged as unrecognised.  " +
        "This will be either the time the last Fixation attempted was performed (" +
        + lastFixationAttempted.getPerformanceTime() + ") or the time the " +
        "attention clock is currently set to (" + this._attentionClock + ")."
      );
      int time = Math.max(lastFixationAttempted.getPerformanceTime(), this._attentionClock);
      
      this.printDebugStatement(
        "- Tagging VisualSpatialFieldObjects whose identifiers are not in the " +
        "following list as being unrecognised at time (" + time + "): " + 
        this._recognisedVisualSpatialFieldObjectIdentifiers
      );
        
      for(int col = 0; col < visualSpatialFieldRepresented.getWidth(); col++){
        for(int row = 0; row < visualSpatialFieldRepresented.getHeight(); row++){
          
          this.printDebugStatement(
            "- Processing VisualSpatialFieldObjects on VisualSpatialFieldCoordinates (" + 
            col + ", " + row + ")"
          );
          
          for(VisualSpatialFieldObject visualSpatialFieldObject : visualSpatialFieldRepresented.getCoordinateContents(col, row, time, false)){

            this.printDebugStatement("  ~ Processing VisualSpatialFieldObject:" + visualSpatialFieldObject.toString());
            if(this.debug()){
              this.printDebugStatement("    + Checking if all the following statements evaluate to true:");
              this.printDebugStatement("      > The VisualSpatialFieldObject is alive at time " + time + ": " + visualSpatialFieldObject.isAlive(time));
              this.printDebugStatement("      > The VisualSpatialFieldObject does not represent the creator: " + !visualSpatialFieldObject.getObjectType().equals(Scene.getCreatorToken()));
              this.printDebugStatement("      > The VisualSpatialFieldObject was not recognised when the most recent Fixation set was performed: " + !this._recognisedVisualSpatialFieldObjectIdentifiers.contains(visualSpatialFieldObject.getIdentifier()));
            }
            
            if(
              visualSpatialFieldObject.isAlive(time) && 
              !visualSpatialFieldObject.getObjectType().equals(Scene.getCreatorToken()) &&
              !this._recognisedVisualSpatialFieldObjectIdentifiers.contains(visualSpatialFieldObject.getIdentifier())
            ){
              this.printDebugStatement(
                "    + All checks evaluate to true, the VisualSpatialFieldObject's " +
                "recognised status will be set to false at time " + time
              );
              visualSpatialFieldObject.setUnrecognised(time, true);
            }
            else{
              this.printDebugStatement(
                "    + Not all checks evaluate to true so this " +
                "VisualSpatialFieldObject's recognised status will not be " +
                "modified"
              );
            }
          }
        }
      }
    }
    else{
      this.printDebugStatement("  ~ Not all statements are true, exiting");
    }
      
    this.printDebugStatement("===== RETURN =====");
  }
  
  /**
   * Designed to be used by {@link #this#scheduleOrMakeNextFixation(
   * jchrest.domainSpecifics.Scene, boolean, int)}.
   * <p>
   * If invoking {@link jchrest.domainSpecifics.Fixation#getTimeDecidedUpon()}
   * on any {@link jchrest.domainSpecifics.Fixation} in {@code 
   * fixationsScheduled} equals the {@code time} specified, an attempt will be 
   * made to schedule that {@link jchrest.domainSpecifics.Fixation} for 
   * performance.
   * <p>
   * A {@link jchrest.domainSpecifics.Fixation} can only be scheduled for 
   * performance if the {@link jchrest.architecture.Perceiver} associated with
   * {@link #this} is free, i.e. if passing the {@code time} specified to {@link 
   * #this#isPerceiverFree(int)} returns {@link java.lang.Boolean#TRUE}.  If 
   * not, the {@link jchrest.domainSpecifics.Fixation} will be abandoned, i.e.
   * removed from {@code fixationsScheduled}.  This means that if there is more
   * than one {@link jchrest.domainSpecifics.Fixation} in {@code 
   * fixationsScheduled} that returns the {@code time} specified when {@link 
   * jchrest.domainSpecifics.Fixation#getTimeDecidedUpon()} is invoked, only 
   * the first {@link jchrest.domainSpecifics.Fixation} in the list will be 
   * scheduled for performance, the rest will be abandoned.
   * <p>
   * Any {@link jchrest.domainSpecifics.Fixation Fixations} whose value for 
   * {@link jchrest.domainSpecifics.Fixation#getPerformanceTime()} is not equal
   * to {@code null} will simply be ignored since these {@link 
   * jchrest.domainSpecifics.Fixation Fixations} have already been scheduled for
   * performance.
   * 
   * @param fixationsScheduled
   * @param time
   * 
   * @return {@code fixationsScheduled} after the processing described in the
   * method description has been applied.
   * 
   * @throws IllegalStateException If any {@link 
   * jchrest.domainSpecifics.Fixation} in {@code fixationsScheduled} returns 
   * {@code null} when {@link 
   * jchrest.domainSpecifics.Fixation#getPerformanceTime()} is invoked on it and
   * and its value for {@link 
   * jchrest.domainSpecifics.Fixation#getTimeDecidedUpon()} is in the past 
   * relative to the {@code time} specified. This means that the {@link 
   * jchrest.domainSpecifics.Fixation} has not been scheduled correctly and 
   * model execution should not continue in such circumstances.
   */
  private List<Fixation> scheduleFixationsForPerformance(List<Fixation> fixationsScheduled, int time){
    this.printDebugStatement("===== Chrest.scheduleFixationsForPerformance() =====");
    this.printDebugStatement("- Fixations to process: " + fixationsScheduled);
    
    Iterator<Fixation> iterator = fixationsScheduled.iterator();
    while(iterator.hasNext()){
      
      Fixation fixation = iterator.next();
      this.printDebugStatement(
        "- Checking if the following Fixation should be scheduled for " +
        "performance: " + fixation.toString()
      );
          
      //Only process fixations that aren't scheduled for performance yet.
      if(fixation.getPerformanceTime() == null){
        int timeDecidedUpon = fixation.getTimeDecidedUpon();
        this.printDebugStatement( "  ~ Fixation's performance time not yet set");
        
        this.printDebugStatement(
          "- Checking if the current time (" + time + ") is equal to the time " +
          "the Fixation is decided upon (" + timeDecidedUpon + ")"
        );
            
        if(timeDecidedUpon == time){
          this.printDebugStatement("  ~ Fixation is to be decided upon now");
          
          this.printDebugStatement("- Checking if Perceiver is free"); 
          if(this.isPerceiverFree(time)){
            this.printDebugStatement(
              "   + Perceiver free, scheduling Fixation for " +
              "performance at the current time (" + time + ") plus the " +
              "time taken to perform a saccade (" + this._saccadeTime + ") " +
              "and consuming the Perceiver resource until this time"
            );
            fixation.setPerformanceTime(time + this._saccadeTime);
            this._perceiverClock = fixation.getPerformanceTime();
          }
          else {
            this.printDebugStatement("  ~ Perceiver not free, abandoning Fixation");
            iterator.remove();
          }
        }
        else if(timeDecidedUpon < time){
          throw new IllegalStateException(
            "The following Fixation was scheduled to be decided upon at time " + 
            timeDecidedUpon + " but wasn't so its performance time has not been " +
            "set correctly: " + fixation.toString()
          );
        }
      }
      else{
        this.printDebugStatement("  ~ Fixation's performance time already set, skipping processing");
      }
    }
    
    this.printDebugStatement("- Returning " + fixationsScheduled.toString());
    this.printDebugStatement("===== RETURN Chrest.scheduleFixationsForPerformance() =====");
    return fixationsScheduled;
  }
  
  /**********************************************/
  /***** Visual-Spatial Field Functionality *****/
  /**********************************************/
  
  /**
   * Attempts to create and associate a new {@link 
   * jchrest.architecture.VisualSpatialField} with {@link #this}.
   * <p>
   * If a {@link jchrest.architecture.VisualSpatialField} is successfully
   * created, it will be added to the database of {@link 
   * jchrest.architecture.VisualSpatialField VisualSpatialFields} associated 
   * with {@link #this} at the {@code time} specified.
   * <p>
   * The {@link jchrest.architecture.VisualSpatialField} created represents what
   * has been "seen" by {@link #this} based upon the results of passing the 
   * {@code time} specified as an input parameter to {@link 
   * jchrest.architecture.Stm#getContents(int)} in context of {@link 
   * jchrest.lib.Modality#VISUAL} {@link jchrest.architecture.Stm} and 
   * {@link jchrest.architecture.Perceiver#getFixationsPerformed(int)}.  
   * <p>
   * {@link jchrest.domainSpecifics.SceneObject SceneObjects} that have been 
   * fixated on and recognised are encoded as {@link 
   * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} first.  To 
   * do this, all {@link jchrest.lib.ItemSquarePattern ItemSquarePatterns} in 
   * the {@link jchrest.lib.ListPattern ListPatterns} present in the result of 
   * invoking {@link jchrest.architecture.Node#getContents()} or {@link 
   * jchrest.architecture.Node#getImage(int)} on each {@link 
   * jchrest.architecture.Node} returned by invoking {@link 
   * jchrest.architecture.Stm#getContents(int)} on {@link 
   * jchrest.lib.Modality#VISUAL} {@link jchrest.architecture.Stm} are checked 
   * to see if the {@link jchrest.domainSpecifics.SceneObject} referenced is
   * present on the {@link jchrest.lib.Square} referenced.  If not, and the 
   * {@link jchrest.lib.Square} referenced is represented in the {@link 
   * jchrest.architecture.VisualSpatialField} constructed, a new recognised 
   * {@link jchrest.lib.VisualSpatialFieldObject} is created to represent the
   * {@link jchrest.domainSpecifics.SceneObject} referenced.  In addition, any
   * {@link jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} 
   * on coordinates within the {@link 
   * jchrest.architecture.Perceiver#getFixationFieldOfView()} on the {@link 
   * jchrest.architecture.VisualSpatialField} will have their termini refreshed.
   * If the {@link jchrest.lib.Square} referenced already contains a {@link 
   * jchrest.lib.VisualSpatialFieldObject} in the {@link 
   * jchrest.architecture.VisualSpatialField} being constructed, no new {@link 
   * jchrest.lib.VisualSpatialFieldObject} is constructed but {@link 
   * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} within the
   * {@link jchrest.architecture.Perceiver#getFixationFieldOfView()} on the
   * {@link jchrest.architecture.VisualSpatialField} coordinates referenced by
   * the location of the {@link jchrest.domainSpecifics.SceneObject} will have
   * their termini refreshed.
   * <p>
   * Unrecognised {@link jchrest.domainSpecifics.SceneObject SceneObjects} are
   * then processed in a similar fashion except they are encoded as {@link 
   * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} in order of 
   * their appearance in the result of {@link 
   * jchrest.architecture.Perceiver#getFixationsPerformed(int)}; unrecognised 
   * {@link jchrest.domainSpecifics.SceneObject SceneObjects} in the most recent 
   * {@link jchrest.lib.Fixation} performed are encoded as {@link 
   * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} first.
   * 
   * @param time
   */
  //TODO: Currently, the "make_fixations_in_chess_domain" checks that the 
  //      function handles root Nodes correctly when processing recognised 
  //      objects but it would be better if there were an explicit check in the 
  //      test that validates this directly.
  //TODO: If the same SceneObject is fixated on more than once consecutively, 
  //      should there be a time cost incurred for processing each Fixation 
  //      after the first one it appears in?
  private void constructVisualSpatialField(int time){
    this.printDebugStatement("===== Chrest.constructVisualSpatialField() =====");
    
    //Attention must be free to start constructing a visual-spatial field.
    this.printDebugStatement("- Checking if attention is free at time " + time);
    if(this.isAttentionFree(time)){
      
      this.printDebugStatement("- Attention is free, checking if any Fixations have been performed at time " + time);
      List<Fixation> fixationsPerformed = this.getPerceiver().getFixationsPerformed(time);
      if(fixationsPerformed != null && !fixationsPerformed.isEmpty()){
      
        this.printDebugStatement("- Fixations have been performed, instantiating VisualSpatialField");
        
        //////////////////////////////////////////
        ///// CONSTRUCT VISUAL-SPATIAL FIELD /////
        //////////////////////////////////////////
        
        //Need to construct a new VisualSpatialField that represents the Scenes 
        //successfully fixated on.  To do this, get the min and max 
        //domain-specific col/row from the Scenes fixated on successfully (this
        //can allow the VisualSpatialField space to be bigger than what can be
        //seen "physically").
        this.printDebugStatement("\n===== Constructing visual-spatial field");
        ArrayList<Integer> domainSpecificColumnsFixatedOn = new ArrayList();
        ArrayList<Integer> domainSpecificRowsFixatedOn = new ArrayList();
        for(Fixation fixationPerformed : fixationsPerformed){
          Scene sceneFixatedOn = fixationPerformed.getScene();
          
          //Add min col/row of Scene fixated on.
          domainSpecificColumnsFixatedOn.add(sceneFixatedOn.getMinimumDomainSpecificColumn());
          domainSpecificRowsFixatedOn.add(sceneFixatedOn.getMinimumDomainSpecificRow());
          
          //Add max col/row of Scene fixated on.
          domainSpecificColumnsFixatedOn.add( (sceneFixatedOn.getMinimumDomainSpecificColumn() + sceneFixatedOn.getWidth()) - 1 );
          domainSpecificRowsFixatedOn.add( (sceneFixatedOn.getMinimumDomainSpecificRow() + sceneFixatedOn.getHeight()) - 1 );
          
          this.printDebugStatement(
            "- Fixated on Scene with name '" + sceneFixatedOn.getName() + "'" +
            "\n   ~ Min col: " + domainSpecificColumnsFixatedOn.get(0) +
            "\n   ~ Min row: " + domainSpecificRowsFixatedOn.get(0) +
            "\n   ~ Max col: " + domainSpecificColumnsFixatedOn.get(1) +
            "\n   ~ Max row: " + domainSpecificRowsFixatedOn.get(1)
          );
        }
        
        Integer minDomainSpecificColOfSceneFixatedOn = Collections.min(domainSpecificColumnsFixatedOn);
        Integer minDomainSpecificRowOfSceneFixatedOn = Collections.min(domainSpecificRowsFixatedOn);
        Integer maxDomainSpecificColOfSceneFixatedOn = Collections.max(domainSpecificColumnsFixatedOn);
        Integer maxDomainSpecificRowOfSceneFixatedOn = Collections.max(domainSpecificRowsFixatedOn);
        this.printDebugStatement(
          "\n- Minimum and maximum domain-specific column and row fixated on:" +
          "\n   ~ Min col: " + minDomainSpecificColOfSceneFixatedOn +
          "\n   ~ Min row: " + minDomainSpecificRowOfSceneFixatedOn +
          "\n   ~ Max col: " + maxDomainSpecificColOfSceneFixatedOn +
          "\n   ~ Max row: " + maxDomainSpecificRowOfSceneFixatedOn
        );
        
        //To understand why 1 is added: consider a case where the max col is 4
        //and the min col is 1, the width should be 4 but 4 - 1 = 3.
        int width = (maxDomainSpecificColOfSceneFixatedOn - minDomainSpecificColOfSceneFixatedOn) + 1;
        int height = (maxDomainSpecificRowOfSceneFixatedOn - minDomainSpecificRowOfSceneFixatedOn) + 1;
          
        //Get the location of the creator, if required.
        List creatorDetails = null;
        if(this.isLearningObjectLocationsRelativeToAgent()){
          this.printDebugStatement(
            "\n- CHREST model is learning object locations relative to itself so " +
            "the identifier and location of the agent equipped with CHREST needs " +
            "to be encoded in the VisualSpatialField being instantiated.  The " + 
            "location encoded will be the location of the agent in the most " +
            "recent Fixation performed."
          );
          
          creatorDetails = new ArrayList();
          
          //Get the most recent Fixation performed.  No need for a null check 
          //since the function has already established by this point that 
          //Fixations have been performed.
          Fixation mostRecentFixation = fixationsPerformed.get(fixationsPerformed.size() - 1);
          
          //Get the Scene that the most recently performed Fixation was made in
          //context of since this is needed to get the domain-specific 
          //coordinates of the agent.  This shouldn't return null but check 
          //any way (better to be safe than sorry!).
          Scene mostRecentlyFixatedOnScene = mostRecentFixation.getScene();
          if(mostRecentlyFixatedOnScene != null){
          
            //Scene is OK so the agent's location should be able to be 
            //retrieved but perform a null check anyway (better to be safe than 
            //sorry!).
            Square mostRecentLocationOfCreator = mostRecentFixation.getScene().getLocationOfCreator();
            if(mostRecentLocationOfCreator != null){
          
              //Get the Scene-specific column and row of the creator and use
              //this information to extract the creator's identifer and its 
              //domain-specific coordinates.
              int mostRecentLocationOfCreatorCol = mostRecentLocationOfCreator.getColumn();
              int mostRecentLocationOfCreatorRow = mostRecentLocationOfCreator.getRow();
              
              int domainSpecificColAgentLocation = mostRecentlyFixatedOnScene.getDomainSpecificColFromSceneSpecificCol(mostRecentLocationOfCreatorCol);
              int domainSpecificRowAgentLocation = mostRecentlyFixatedOnScene.getDomainSpecificRowFromSceneSpecificRow(mostRecentLocationOfCreatorRow);

              Integer agentLocationInVisualSpatialFieldCol = domainSpecificColAgentLocation - minDomainSpecificColOfSceneFixatedOn;
              Integer agentLocationInVisualSpatialFieldRow = domainSpecificRowAgentLocation - minDomainSpecificRowOfSceneFixatedOn;
              String agentIdentifier = mostRecentlyFixatedOnScene.getSquareContents(mostRecentLocationOfCreatorCol, mostRecentLocationOfCreatorRow).getIdentifier();
              
              this.printDebugStatement(
                "   ~ The agent's identifier is '" + agentIdentifier + "' " +
                "and its location in the domain according to the most recent " +
                "Fixation performed is (" + domainSpecificColAgentLocation + ", " + 
                domainSpecificRowAgentLocation + ").  This means that the " +
                "agent will be encoded on VisualSpatialField coordinates (" +
                agentLocationInVisualSpatialFieldCol + ", " + 
                agentLocationInVisualSpatialFieldRow + ")."
              );

              //Add the information to the data structure containing the 
              //creator's details.
              creatorDetails.add(agentIdentifier);
              creatorDetails.add(new Square(agentLocationInVisualSpatialFieldCol, agentLocationInVisualSpatialFieldRow));
            }
            else {
              throw new IllegalStateException(
                "CHREST is learning object locations relatibe to the agent " +
                "equipped with CHREST but the agent's location has not been " +
                "specified in the Scene that the most recent Fixation was " +
                "performed in context of.  Fixation details:\n" + 
                mostRecentFixation.toString()
              );
            }
          }
          else{
            throw new IllegalStateException(
              "CHREST is learning object locations relative to the agent " +
              "equipped with CHREST but the most recent Fixation performed " +
              "does not have a Scene set.  Fixation details:\n" + 
              mostRecentFixation.toString()
            );
          }
        }
        
        this.printDebugStatement(
          "\n- Instantiating VisualSpatialField that is " + width + " columns by " +
          height + " rows and will represent domain-specific coordinates from " + 
          "(" + minDomainSpecificColOfSceneFixatedOn + ", " + minDomainSpecificRowOfSceneFixatedOn + ") to " +
          "(" + maxDomainSpecificColOfSceneFixatedOn + ", " + maxDomainSpecificRowOfSceneFixatedOn + ")" +
          (creatorDetails == null ? 
            "" : 
            "and the agent creating the VisualSpatialField has identifier: '" +
            (String)creatorDetails.get(0) + "' and will be located on coordinates " +
            ((Square)creatorDetails.get(1)).toString() + " in the " +
            "VisualSpatialField."
          )
        );
        
        //VisualSpatialField is entirely unknown when constructed with the 
        //exception of the coordinates containing the creator (if specified).
        VisualSpatialField visualSpatialField = new VisualSpatialField(
          "Visual-Spatial Field @ " + time + " ms", 
          width, 
          height, 
          minDomainSpecificColOfSceneFixatedOn,
          minDomainSpecificRowOfSceneFixatedOn,
          this,
          creatorDetails,
          time
        );
        
        this.printDebugStatement(
          "\n- Adding the VisualSpatialField to this model's " +
          "database of VisualSpatialFields at time " + time
        );
        this._visualSpatialFields.put(time, visualSpatialField);
        
        ///////////////////////////////////////
        ///// GET SceneObjects FIXATED ON /////
        ///////////////////////////////////////
        
        //When Fixations are stored by the Perceiver, any SceneObjects found in
        //the coordinates around the Fixation point (dictated by the Perceiver's
        //"fixation field of view" parameter) are also "seen" but this 
        //information is not recorded.  So, to determine all SceneObjects that
        //have actually been "seen" by this CHREST model in the set of Fixations
        //used in this function, the Fixations need to be recreated.
        //
        //Create HashMap containing the identifier of each SceneObject fixated 
        //on and the Scene it was fixated on in context of.  This will provide 
        //all the information required to construct VisualSpatialFieldObject 
        //representations for each SceneObject fixated on.
        this.printDebugStatement("\n===== Getting SceneObjects fixated on");
        List<HashMap<SceneObject, Scene>> sceneObjectsSeenInfo = new ArrayList();
        Map<Square, Scene> coordinatesFixatedOn = new HashMap();
        
        for(int fixation = 0; fixation < fixationsPerformed.size(); fixation++){
          Fixation fixationPerformed = fixationsPerformed.get(fixation);
          this.printDebugStatement("\n- Processing Fixation " + (fixation + 1) + ":\n" + fixationPerformed.toString());
          
          ListPattern objectsSeenInFixationFieldOfView = this.getPerceiver().getObjectsSeenInFixationFieldOfView(fixationPerformed, false);
          this.printDebugStatement("   ~ This ListPattern was generated when this Fixation was performed: " + objectsSeenInFixationFieldOfView.toString());
          
          this.printDebugStatement("   ~ Stripping ListPattern of any blind squares since these shouldn't be considered at all and creators since one has already been added");
          objectsSeenInFixationFieldOfView = objectsSeenInFixationFieldOfView.removeBlindObjects();
          objectsSeenInFixationFieldOfView = objectsSeenInFixationFieldOfView.removeCreatorObject();
          this.printDebugStatement("   ~ ListPattern after stripping blind squares and creator from it: " + objectsSeenInFixationFieldOfView.toString());
          
          this.printDebugStatement("   ~ Using this ListPattern's primitives to get required information");
          for(int primitive = 0; primitive < objectsSeenInFixationFieldOfView.size(); primitive++){
            
            PrimitivePattern sceneObjectSeen = objectsSeenInFixationFieldOfView.getItem(primitive);
              
            //Get SceneObject from Scene that the current Fixation was performed 
            //on by converting the domain-specific/agent-relative coordinates of 
            //PrimitivePattern to coordinates specific to the Scene the Fixation
            //was performed in context of.
            ItemSquarePattern sceneObjectSeenIsp = (ItemSquarePattern)sceneObjectSeen;
            Scene sceneFixationPerformedOn = fixationPerformed.getScene();
            Integer sceneSpecificCol;
            Integer sceneSpecificRow;
            
            coordinatesFixatedOn.put(
              new Square(sceneObjectSeenIsp.getColumn(), sceneObjectSeenIsp.getRow()), 
              sceneFixationPerformedOn
            );
            
            if(this.isLearningObjectLocationsRelativeToAgent()){
              Square locationOfCreator = sceneFixationPerformedOn.getLocationOfCreator();
              sceneSpecificCol = locationOfCreator.getColumn() + sceneObjectSeenIsp.getColumn();
              sceneSpecificRow = locationOfCreator.getRow() + sceneObjectSeenIsp.getRow();
            }
            else{
              sceneSpecificCol = sceneFixationPerformedOn.getSceneSpecificColFromDomainSpecificCol(sceneObjectSeenIsp.getColumn());
              sceneSpecificRow = sceneFixationPerformedOn.getSceneSpecificRowFromDomainSpecificRow(sceneObjectSeenIsp.getRow());
            }
            this.printDebugStatement("      + Primitive " + primitive + "'s scene-specific coordinates: (" + sceneSpecificCol + ", " + sceneSpecificRow +")");
            
            
            //Now that scene-specific coordinates for the SceneObject fixated on
            //have been calculated, retrieve the SceneObject and add an entry to 
            //the sceneObjectsSeenInfo data structure.
            SceneObject sceneObject = sceneFixationPerformedOn.getSquareContents(sceneSpecificCol, sceneSpecificRow);
            HashMap sceneObjectSeenInfo = new HashMap();
            sceneObjectSeenInfo.put(sceneObject, sceneFixationPerformedOn);
            sceneObjectsSeenInfo.add(sceneObjectSeenInfo);
            this.printDebugStatement("      + SceneObject on these coordinates has " + sceneObject.toString());
            this.printDebugStatement("      + Name of Scene that SceneObject was fixated on in context of: " + sceneFixationPerformedOn.getName());
          }
        }
        this.printDebugStatement("\n- SceneObjects seen information: ");
        if(this.debug()){
          for(HashMap<SceneObject, Scene> sceneObjectSeenInfo : sceneObjectsSeenInfo){
            for(Entry<SceneObject, Scene> info : sceneObjectSeenInfo.entrySet()){
              this.printDebugStatement("   ~ " + info.getKey().toString());
              this.printDebugStatement("   ~ Name of Scene fixated on in context of: " + info.getValue().getName());
            }
          }
        }
        
        /////////////////////////////////////////////////////////////
        ///// DETERMINE RECOGNISED SceneObjects AND COORDINATES /////
        /////////////////////////////////////////////////////////////
        
        //A SceneObject/coordinate is recognised if it is present in a STM 
        //Node's contents/image.  If a SceneObject is recognised, an attempt to 
        //construct a VisualSpatialFieldObject representation of it will be 
        //made.  If the coordinates referenced by the SceneObject are 
        //recognised, any VisualSpatialFieldObjects on the relevant 
        //VisualSpatialField coordinates and coordinates in scope of the 
        //Fixation field of view around this coordinate will have their termini
        //extended.
        this.printDebugStatement("\n===== Encoding recognised SceneObjects");
        List<Node> visualStmContentsAtCurrentTime = this.getStm(Modality.VISUAL).getContents(time);
        this.printDebugStatement("- State of visual STM at time " + time + " (hypothesis first):");
        if(this.debug()){
          for(int n = 0; n < visualStmContentsAtCurrentTime.size(); n++){
            Node stmNode = visualStmContentsAtCurrentTime.get(n);
            this.printDebugStatement(
              "   ~ STM Node " + n + " contents: " + stmNode.getContents().toString() + ", image: " + stmNode.getImage(time).toString()
            );
          }
        }
        
        //Remove root Nodes from STM since these will cause problems if 
        //processed
        for(int n = 0; n < visualStmContentsAtCurrentTime.size(); n++){
          Node node = visualStmContentsAtCurrentTime.get(n);
          if(node.isRootNode()) visualStmContentsAtCurrentTime.remove(n);
        }
        
        //This will be used to control two loops below.  Ensures consistency of
        //function behaviour.
        int numberNodesInVisualStm = visualStmContentsAtCurrentTime.size();
        
        //Data structure below will be populated with SceneObjects recognised 
        //and will allow the function to also determine what SceneObjects are
        //unrecognised. The List elements reflect the STM Node that each 
        //SceneObject recognised was recognised in context of (important when
        //handling encoding times later).
        List<Map<SceneObject, Scene>> sceneObjectsRecognisedInStmNodes = new ArrayList();
        List<List<Square>> domainSpecificCoordinatesRecognisedInStmNodes = new ArrayList();
        
        //Process most recent STM Node first.
        for(int n = 0; n < numberNodesInVisualStm; n++){
          this.printDebugStatement("\n- STM Node " + n);
          
          //Will be added to the recognised SceneObject list.
          Map<SceneObject, Scene> sceneObjectRecognisedInfo = new HashMap();
          List<Square> domainSpecificCoordinatesRecognised = new ArrayList();
          
          //Get the contents and image of the current STM Node.  These will be
          //used to determine if any SceneObjects or coordinates are recognised.
          Node stmNode = visualStmContentsAtCurrentTime.get(n);
          ListPattern content = stmNode.getContents();
          ListPattern image = stmNode.getImage(time);
          
          //Check if each of the SceneObjects or coordinates fixated on are
          //recognised.
          for(HashMap<SceneObject, Scene> sceneObjectSeenInfo : sceneObjectsSeenInfo){
            for(Entry<SceneObject, Scene> info : sceneObjectSeenInfo.entrySet()){
              
              SceneObject sceneObject = info.getKey();
              Scene sceneThatSceneObjectWasFixatedOnInContextOf = info.getValue();
              
              //To check if the SceneObject is recognised, its coordinates need
              //to either be domain-specific if object loctaions are not being
              //learned relative to the agent equipped with CHREST or 
              //agent-relative if they are (they need to match what is in LTM, 
              //essentially.  To do this, get the coordinates of the SceneObject 
              //relative to the Scene it was seen in first. These can either be
              //used directly if object loctaions are not being learned relative 
              //to the agent equipped with CHREST or can be used to calculate
              //agent-relative coordinates if object locations are being learned
              //relative to the agent equipped with CHREST.
              Square domainSpecificLocationOfSceneObjectSeen = null;
              for(int col = 0; col < sceneThatSceneObjectWasFixatedOnInContextOf.getWidth(); col++){
                for(int row = 0; row < sceneThatSceneObjectWasFixatedOnInContextOf.getHeight(); row++){
                  if(sceneThatSceneObjectWasFixatedOnInContextOf.getSquareContents(col, row).getIdentifier().equals(sceneObject.getIdentifier())){
                    domainSpecificLocationOfSceneObjectSeen = new Square(
                      sceneThatSceneObjectWasFixatedOnInContextOf.getDomainSpecificColFromSceneSpecificCol(col),
                      sceneThatSceneObjectWasFixatedOnInContextOf.getDomainSpecificRowFromSceneSpecificRow(row)
                    );
                  }
                }
              }

              if(domainSpecificLocationOfSceneObjectSeen != null){
                
                //Create a data structure to store a List of Squares whose 
                //column and row values will be formatted in the same way as 
                //they would be in S/LTM.  A List is used since, if object 
                //locations are agent-relative, the agent may have moved around
                //whilst making Fixations so, for each of its past loctaions, 
                //the SceneObject will have been in a different place.
                List<Square> colsAndRowsToSearchFor = new ArrayList();

                if(this.isLearningObjectLocationsRelativeToAgent()){
                  
                  //For each location of the agent, calculate where the 
                  //sceneObjectSeen would have been relative to it.
                  for(HashMap<SceneObject, Scene> sceneObjectSeen : sceneObjectsSeenInfo){
                    for(Scene sceneFixatedOn: sceneObjectSeen.values()){
                      Square locationOfCreator = sceneFixatedOn.getLocationOfCreator();
                      int locationOfCreatorCol = sceneFixatedOn.getDomainSpecificColFromSceneSpecificCol(locationOfCreator.getColumn());
                      int locationOfCreatorRow = sceneFixatedOn.getDomainSpecificRowFromSceneSpecificRow(locationOfCreator.getRow());
                      Square colAndRowToSearchFor = new Square(
                        domainSpecificLocationOfSceneObjectSeen.getColumn() - locationOfCreatorCol,
                        domainSpecificLocationOfSceneObjectSeen.getRow() - locationOfCreatorRow
                      );
                      if(!colsAndRowsToSearchFor.contains(colAndRowToSearchFor)) colsAndRowsToSearchFor.add(colAndRowToSearchFor);
                    }
                  }
                }
                else{
                  colsAndRowsToSearchFor.add(new Square(
                    domainSpecificLocationOfSceneObjectSeen.getColumn(),
                    domainSpecificLocationOfSceneObjectSeen.getRow()
                  ));
                }

                for(Square colAndRowToSearchFor : colsAndRowsToSearchFor){
                  int colToSearchFor = colAndRowToSearchFor.getColumn();
                  int rowToSearchFor = colAndRowToSearchFor.getRow();
                  
                  //Create an ItemSquarePattern using the col and row to search
                  //for that will potentially match an ItemSquarePattern in 
                  //the Node's content/image.
                  ItemSquarePattern stmNodeCompatibleIsp = new ItemSquarePattern(
                    sceneObject.getObjectType(),
                    colToSearchFor,
                    rowToSearchFor
                  );

                  //If the exact ItemSqarePattern created above is present in
                  //the contents or image of this STM Node, add the 
                  //SceneObject and the Scene it was fixated on in context of
                  //to the recognised sceneObjects List.
                  if(content.contains(stmNodeCompatibleIsp) || image.contains(stmNodeCompatibleIsp)){
                    sceneObjectRecognisedInfo.putIfAbsent(sceneObject, sceneThatSceneObjectWasFixatedOnInContextOf);
                  }
                  
                  for(PrimitivePattern contentPrim : content){
                    ItemSquarePattern contentIsp = (ItemSquarePattern)contentPrim;
                    if(
                      contentIsp.getColumn() == colToSearchFor && 
                      contentIsp.getRow() == rowToSearchFor && 
                      !domainSpecificCoordinatesRecognised.contains(colAndRowToSearchFor)
                    ){
                      domainSpecificCoordinatesRecognised.add(colAndRowToSearchFor);
                    }
                  }

                  for(PrimitivePattern imagePrim : image){
                    ItemSquarePattern imageIsp = (ItemSquarePattern)imagePrim;
                    if(
                      imageIsp.getColumn() == colToSearchFor && 
                      imageIsp.getRow() == rowToSearchFor &&
                      !domainSpecificCoordinatesRecognised.contains(colAndRowToSearchFor)
                    ){
                      domainSpecificCoordinatesRecognised.add(colAndRowToSearchFor);
                    }
                  }
                }
              }
            }
          }
          
          this.printDebugStatement("   ~ " + sceneObjectRecognisedInfo.size() + " SceneObjects recognised:");
          if(this.debug()){
            for(SceneObject recognisedSceneObject : sceneObjectRecognisedInfo.keySet()){
              this.printDebugStatement("      + " + recognisedSceneObject.toString());
            }
          }
          
          this.printDebugStatement("   ~ " + domainSpecificCoordinatesRecognised.size() + " coordinates recognised:");
          if(this.debug()){
            for(Square coordinatesRecognised : domainSpecificCoordinatesRecognised){
              this.printDebugStatement("      + " + coordinatesRecognised.toString());
            }
          }
          
          sceneObjectsRecognisedInStmNodes.add(sceneObjectRecognisedInfo);
          domainSpecificCoordinatesRecognisedInStmNodes.add(domainSpecificCoordinatesRecognised);
        }
        
        /////////////////////////////////////////////////////////////////
        ///// ENCODE RECOGNISED SceneObjects AND REFRESH TERMINI OF /////
        /////  VisualSPatialFieldObjects ON RECOGNISED COORDINATES  /////
        /////////////////////////////////////////////////////////////////
        
        //Now, encode recognised SceneObjects and refresh termini of 
        //SceneObjects on coordinates recognised.
        for(int node = 0; node < numberNodesInVisualStm; node++){
          this.printDebugStatement("\n===== Encoding recognised SceneObjects and refreshing termini of VisualSpatialFieldObjects on recognised coordinates");
          this.printDebugStatement("- Processing SceneObjects and coordinates recognised in STM Node " + node);
          this.printDebugStatement("- Incrementing current time (" + time + ") by time taken to retreieve a Node from STM (" + this._timeToRetrieveItemFromStm + ")");
          time += this._timeToRetrieveItemFromStm;
          
          boolean visualSpatialFieldObjectEncoded = false;
          
          this.printDebugStatement("- If any SceneObjects recognised in this Node are to be encoded, " +
            "they will all be encoded at the same time, i.e. the current time (" + 
            time + ") plus the time taken to encode a recognised SceneObject (" +
            this._timeToEncodeRecognisedSceneObjectAsVisualSpatialFieldObject + "), in other words, at time "
            + (time + this._timeToEncodeRecognisedSceneObjectAsVisualSpatialFieldObject)
          );
          int visualSpatialFieldObjectEncodingTime = time + this._timeToEncodeRecognisedSceneObjectAsVisualSpatialFieldObject;
          
          this.printDebugStatement("- Encoding any recognised SceneObjects first (if there are any)");
          for(Entry<SceneObject, Scene> recognisedSceneObjectInfo : sceneObjectsRecognisedInStmNodes.get(node).entrySet()){
            SceneObject recognisedSceneObject = recognisedSceneObjectInfo.getKey();
            Scene sceneThatRecognisedSceneObjectWasFixatedOnInContextOf = recognisedSceneObjectInfo.getValue();
            
            Integer visualSpatialFieldCol = null;
            Integer visualSpatialFieldRow = null;
            for(int col = 0; col < sceneThatRecognisedSceneObjectWasFixatedOnInContextOf.getWidth(); col++){
              for(int row = 0; row < sceneThatRecognisedSceneObjectWasFixatedOnInContextOf.getHeight(); row++){
                if(sceneThatRecognisedSceneObjectWasFixatedOnInContextOf.getSquareContents(col, row).getIdentifier().equals(recognisedSceneObject.getIdentifier())){

                  visualSpatialFieldCol = visualSpatialField.getVisualSpatialFieldColFromDomainSpecificCol(
                    sceneThatRecognisedSceneObjectWasFixatedOnInContextOf.getDomainSpecificColFromSceneSpecificCol(col)
                  );

                  visualSpatialFieldRow = visualSpatialField.getVisualSpatialFieldRowFromDomainSpecificRow(
                    sceneThatRecognisedSceneObjectWasFixatedOnInContextOf.getDomainSpecificRowFromSceneSpecificRow(row)
                  );
                }
              }
            }
            
            this.printDebugStatement(
              "   ~ Attempting to encode SceneObject " + recognisedSceneObject + 
              " on visual-spatial field coordinates (" + visualSpatialFieldCol + 
              ", " + visualSpatialFieldRow + ")"
            );
        
            boolean visualSpatialFieldObjectCreated = this.encodeVisualSpatialFieldObjectDuringVisualSpatialFieldConstruction(
              visualSpatialField,
              visualSpatialFieldCol,
              visualSpatialFieldRow,
              recognisedSceneObject,
              visualSpatialFieldObjectEncodingTime,
              true
            );

            this.printDebugStatement("   ~ SceneObject encoding successful? " + visualSpatialFieldObjectCreated);
            if(visualSpatialFieldObjectCreated) visualSpatialFieldObjectEncoded = true;
          }
          
          this.printDebugStatement("\n- Refreshing VisualSpatialFieldObjects on recognised coordinates (if there are any)");
          for(Square domainSpecificCoordinatesRecognised : domainSpecificCoordinatesRecognisedInStmNodes.get(node)){
            Integer visualSpatialFieldCol = visualSpatialField.getVisualSpatialFieldColFromDomainSpecificCol(domainSpecificCoordinatesRecognised.getColumn()); 
            Integer visualSpatialFieldRow = visualSpatialField.getVisualSpatialFieldRowFromDomainSpecificRow(domainSpecificCoordinatesRecognised.getRow());
            if(visualSpatialFieldCol != null && visualSpatialFieldRow != null){
              this.refreshVisualSpatialFieldObjectTermini(
                visualSpatialField, 
                visualSpatialFieldCol,
                visualSpatialFieldRow,
                time
              );
            }
          }
          
          if(visualSpatialFieldObjectEncoded){
            this.printDebugStatement("\n- Since a SceneObject was encoded, the current time will be set to " + visualSpatialFieldObjectEncodingTime);
            time = visualSpatialFieldObjectEncodingTime;
          }
          this.printDebugStatement("\n- Finished processing SceneObjects and coordinates in STM Node " + node + " at time " + time);
        }
        
        ////////////////////////////////////////////
        ///// ENCODE UNRECOGNISED SceneObjects /////
        ////////////////////////////////////////////
        
        //Determine unrecognised SceneObjects. 
        List<HashMap<SceneObject, Scene>> unrecognisedSceneObjectsInfo = new ArrayList();
        for(HashMap<SceneObject, Scene> sceneObjectSeenInfo : sceneObjectsSeenInfo){
          for(Entry<SceneObject, Scene> info : sceneObjectSeenInfo.entrySet()){
            boolean sceneObjectSeenRecognised = false;

            for(Map<SceneObject, Scene> sceneObjectInfoRecognisedInStmNode : sceneObjectsRecognisedInStmNodes){
              for(SceneObject sceneObjectRecognised : sceneObjectInfoRecognisedInStmNode.keySet()){
                if(sceneObjectRecognised.getIdentifier().equals(info.getKey().getIdentifier())){
                  sceneObjectSeenRecognised = true;
                  break;
                }
              }
            }

            if(!sceneObjectSeenRecognised){
              HashMap<SceneObject, Scene> unrecognisedSceneObjectInfo = new HashMap();
              unrecognisedSceneObjectInfo.put(info.getKey(), info.getValue());
              unrecognisedSceneObjectsInfo.add(unrecognisedSceneObjectInfo);
            }
          }
        }
        
        if(!unrecognisedSceneObjectsInfo.isEmpty()){
          this.printDebugStatement("\n===== Processing unrecognised SceneObjects");
          
          //Encode from most -> least recent (last to first).  To do this, 
          //convert the HashMap containing the unrecognised SceneObject info
          //to an Array (HashMap's don't have numbered elements).  Now we can
          //go backwards through data.
          //
          //TODO: put in a parameter for how many unrecognised SceneObjects can 
          //      be processed. 
          for(int object = unrecognisedSceneObjectsInfo.size() - 1; object >= 0; object--){
            
            this.printDebugStatement("   ~ Incrementing current time (" + time + ") by the time taken " +
              "to process an unrecognised VisualSptialFieldObject (" + 
              this._timeToProcessUnrecognisedSceneObjectDuringVisualSpatialFieldConstruction + ")"
            );
            time += this._timeToProcessUnrecognisedSceneObjectDuringVisualSpatialFieldConstruction;
            this.printDebugStatement("   ~ Current time = " + time);
            
            HashMap<SceneObject, Scene> unrecognisedSceneObjectInfo = unrecognisedSceneObjectsInfo.get(object);
            for(Entry<SceneObject, Scene> info : unrecognisedSceneObjectInfo.entrySet()){
              SceneObject unrecognisedSceneObject = info.getKey();
              Scene sceneThatUnrecognisedSceneObjectWasFixatedOnInContextOf = info.getValue();

              //Get visual-spatial field coordinates by getting domain-specific
              //coordinates 
              Integer visualSpatialFieldCol = null;
              Integer visualSpatialFieldRow = null;
              for(int col = 0; col < sceneThatUnrecognisedSceneObjectWasFixatedOnInContextOf.getWidth(); col++){
                for(int row = 0; row < sceneThatUnrecognisedSceneObjectWasFixatedOnInContextOf.getHeight(); row++){
                  if(sceneThatUnrecognisedSceneObjectWasFixatedOnInContextOf.getSquareContents(col, row).getIdentifier().equals(unrecognisedSceneObject.getIdentifier())){

                    visualSpatialFieldCol = visualSpatialField.getVisualSpatialFieldColFromDomainSpecificCol(
                      sceneThatUnrecognisedSceneObjectWasFixatedOnInContextOf.getDomainSpecificColFromSceneSpecificCol(col)
                    );

                    visualSpatialFieldRow = visualSpatialField.getVisualSpatialFieldRowFromDomainSpecificRow(
                      sceneThatUnrecognisedSceneObjectWasFixatedOnInContextOf.getDomainSpecificRowFromSceneSpecificRow(row)
                    );
                  }
                }
              }            

              if(visualSpatialFieldCol != null && visualSpatialFieldRow != null){
                this.printDebugStatement("   ~ Attempting to encode SceneObject with " + unrecognisedSceneObject.toString() + 
                  " as a VisualSpatialFieldObject at the current time + "  +
                  (unrecognisedSceneObject.getObjectType().equals(Scene.getEmptySquareToken()) ? 
                    this._timeToEncodeUnrecognisedEmptySquareSceneObjectAsVisualSpatialFieldObject + "since this is an empty square":
                    this._timeToEncodeUnrecognisedNonEmptySquareSceneObjectAsVisualSpatialFieldObject + "since this is a non-empty square"
                  )
                );

                int encodingTime = time + (unrecognisedSceneObject.getObjectType().equals(Scene.getEmptySquareToken()) ? 
                  this._timeToEncodeUnrecognisedEmptySquareSceneObjectAsVisualSpatialFieldObject :
                  this._timeToEncodeUnrecognisedNonEmptySquareSceneObjectAsVisualSpatialFieldObject
                );
                this.printDebugStatement("   ~ Attempting to encode VisualSpatialFieldObject at time " + encodingTime);

                boolean visualSpatialFieldObjectCreated = this.encodeVisualSpatialFieldObjectDuringVisualSpatialFieldConstruction(
                  visualSpatialField,
                  visualSpatialFieldCol,
                  visualSpatialFieldRow,
                  unrecognisedSceneObject,
                  encodingTime, 
                  false
                );

                this.refreshVisualSpatialFieldObjectTermini(visualSpatialField, visualSpatialFieldCol, visualSpatialFieldRow, time);

                if(visualSpatialFieldObjectCreated){
                  time = encodingTime;
                  this.printDebugStatement(
                    "   ~ VisualSpatialFieldObject encoded, setting current " +
                    "time to the time the VisualSpatialFieldObject was encoded (" +
                    time + ")"
                  );
                }
              }
            }
            
            this.printDebugStatement("- Time after processing unrecognised SceneObject = " + time);
          } 
        }
        this._attentionClock = time;
      }
      this.printDebugStatement("Attention clock set to time " + this._attentionClock);
      
    }
    this.printDebugStatement("===== RETURN =====");
  }
  
  /**
   * Moves {@link jchrest.lib.VisualSpatialFieldObject 
   * VisualSpatialFieldObjects} on the {@link 
   * jchrest.architecture.VisualSpatialField} present at the {@code time} 
   * specified according to the {@code moveSequences} specified.  
   * <p>
   * {@link jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObject}
   * movement can only occur if the attention of {@link #this} is free.  If all 
   * moves are successful, the attention clock of {@link #this} will be set to 
   * the sum of {@link #this#getTimeToAccessVisualSpatialField()} and the number 
   * of moves performed multiplied by {@link 
   * #this#getTimeToMoveVisualSpatialFieldObject()}.
   * <p>
   * The number of {@link jchrest.lib.Square Squares} moved by a {@link 
   * jchrest.lib.VisualSpatialFieldObject} is not constrained by this method.  
   * Therefore, according to this method, it takes the same amount of time to 
   * move a {@link jchrest.lib.VisualSpatialFieldObject} across 5 {@link 
   * jchrest.lib.Square Squares }as it does to move it across 1.  Any movement 
   * constraints of this sort should be denoted by the moves specified.
   * <p>
   * If a {@link jchrest.lib.VisualSpatialFieldObject} is moved to a {@link 
   * jchrest.lib.Square} on a {@link jchrest.architecture.VisualSpatialField}
   * that is already occupied then the two {@link 
   * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} will 
   * co-exist on the {@link jchrest.lib.Square} unless the square contains a
   * {@link jchrest.lib.VisualSpatialFieldObject} whose {@link 
   * jchrest.lib.VisualSpatialFieldObject#getObjectType()} is equal to {@link 
   * jchrest.domainSpecifics.Scene#getEmptySquareToken()} or {@link 
   * jchrest.lib.VisualSpatialFieldObject#getUnknownSquareToken()}.
   * <p>
   * {@link jchrest.lib.VisualSpatialFieldObject} movement occurs in two phases:
   * "pick-up" and "put-down".  In both phases, any {@link 
   * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} on 
   * coordinates that fall within {@link 
   * jchrest.architecture.Perceiver#getFixationFieldOfView()} on the relevant 
   * {@link jchrest.architecture.VisualSpatialField} around the {@link 
   * jchrest.lib.VisualSpatialFieldObject} being moved will have their termini
   * refreshed since attention will be focused upon them.
   * <p>
   * Note that the function will handle {@link 
   * jchrest.lib.VisualSpatialFieldObject} movement to coordinates not 
   * represented in the relevant {@link jchrest.architecture.VisualSpatialField}
   * gracefully, i.e. if a {@link jchrest.lib.VisualSpatialFieldObject} is moved
   * to coordinates outside the range encoded by the relevant {@link 
   * jchrest.architecture.VisualSpatialField} and subsequent moves have been 
   * specified in {@code moveSequences}, these will not be implemented and the
   * next {@link jchrest.architecture.VisualSpatialFieldObject} move sequence
   * will be processed (if present).
   * 
   * @param moveSequences A 2D {@link java.util.List} whose first dimension 
   * elements should contain {@link java.util.List Lists} of {@link 
   * jchrest.lib.ItemSquarePattern ItemSquarePatterns} that prescribe a sequence 
   * of moves for one {@link jchrest.lib.VisualSpatialFieldObject} using 
   * coordinates relative to the {@link jchrest.architecture.VisualSpatialField}
   * it is located on.  It is <b>imperative</b> that:
   * <ol type="1">
   *  <li>
   *    The first {@link jchrest.lib.ItemSquarePattern} specifies the current
   *    location of the {@link jchrest.lib.VisualSpatialFieldObject} to move.
   *  </li>
   *  <li>
   *    {@link jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} 
   *    are identified using their identifier (see {@link 
   *    jchrest.lib.VisualSpatialFieldObject#getIdentifier()}) rather than their 
   *    type (see {@link jchrest.lib.VisualSpatialFieldObject#getObjectClass()}).
   *  </li>
   * </ol>
   * For example, if two {@link jchrest.lib.VisualSpatialFieldObject 
   * VisualSpatialFieldObjects} return the same value for {@link 
   * jchrest.lib.VisualSpatialFieldObject#getObjectClass()} ("A", for example)
   * but have different results for {@link 
   * jchrest.lib.VisualSpatialFieldObject#getIdentifier()} ("0" and "1", for 
   * example) and both are to be moved, "0" before "1", then the {@link 
   * java.util.List} passed should be of the form: 
   * <p>
   * {
   *  {[0 sourceX sourceY], [0 destinationX desitinationY]}, 
   *  {[1 sourceX sourceY], [1 desitinationX destinationY]}
   * }
   * 
   * @param time The current time (in milliseconds) in the domain when object
   * movement is requested.
   * 
   * @throws jchrest.lib.VisualSpatialFieldException If {@code moveSequences} 
   * cause any of the following statements to evaluate to {@link 
   * java.lang.Boolean#TRUE}:
   * <ol type="1">
   *  <li>
   *    More than one {@link jchrest.lib.VisualSpatialFieldObject} is moved 
   *    within the same sequence; object movement should be strictly serial.
   *    Therefore, whereas {{[0 1 1][0 2 2]}{[1 2 3][1 4 5]}} is valid,
   *    {{[0 1 1][5 2 2]}{[1 2 3][1 4 5]}} is not since an attempt is made to
   *    move "5" during "0"s movement specification.
   *  </li>
   *  <li>
   *    The initial {@link jchrest.lib.ItemSquarePattern} in a move sequence 
   *    does not correctly identify where the 
   *    {@link jchrest.lib.VisualSpatialFieldObject} is located.
   *  </li>
   *  <li>
   *    Only the initial location of a 
   *    {@link jchrest.lib.VisualSpatialFieldObject} is specified.
   *  </li>
   * </ol>
   */
  public void moveObjectsInVisualSpatialField(ArrayList<ArrayList<ItemSquarePattern>> moveSequences, int time) throws VisualSpatialFieldException {
    
    this.printDebugStatement("===== Chrest.moveObjects() =====");
    Entry<Integer, VisualSpatialField> mostRecentVisualSpatialFieldEntryWhenFunctionInvoked = this.getVisualSpatialFields().floorEntry(time);
    
    //Check that attention is free, if so, continue.
    this.printDebugStatement("- Checking if attention is free at time function invoked (" + time + ")");
    if(this.isAttentionFree(time)){
      
      this.printDebugStatement("- Attention is free");
      
      //Clone the current VisualSpatialField so that if any moves are illegal, 
      //all moves performed up until the illegal move can be reversed.
      this.printDebugStatement(
        "- Cloning most recent visual-spatial field stored relative to when " +
        "this function was invoked so, if any moves are illegal, the " +
        "visual-spatial field state will be reverted."
      );
      
      Integer timeMostRecentVisualSpatialFieldCreated = mostRecentVisualSpatialFieldEntryWhenFunctionInvoked.getKey();
      VisualSpatialField visualSpatialField = mostRecentVisualSpatialFieldEntryWhenFunctionInvoked.getValue();
      
      VisualSpatialField visualSpatialFieldBeforeMovesApplied = new VisualSpatialField(
        visualSpatialField.getName(),
        visualSpatialField.getWidth(),
        visualSpatialField.getHeight(),
        visualSpatialField.getMinimumDomainSpecificCol(),
        visualSpatialField.getMinimumDomainSpecificRow(),
        this,
        visualSpatialField.getCreatorDetails(timeMostRecentVisualSpatialFieldCreated),
        timeMostRecentVisualSpatialFieldCreated
      );
      
      for(int col = 0; col < visualSpatialField.getWidth(); col++){
        for(int row = 0; row < visualSpatialField.getHeight(); row++){
          this.printDebugStatement("   ~ Cloning contents of visual-spatial field coordinates (" + col + ", " + row + ")");
          List<VisualSpatialFieldObject> coordinateContents = visualSpatialField.getCoordinateContents(col, row);
          for(int object = 0; object < coordinateContents.size(); object++){
            
            this.printDebugStatement(
              "      + Checking if VisualSpatialFieldObject " + object + " is " +
              "the creator if so, its already been cloned when the cloned " +
              "VisualSpatialField was created"
            );
            VisualSpatialFieldObject original = coordinateContents.get(object);
            this.printDebugStatement("      + VisualSpatialFieldObject details:" + original.toString());
            
            if(!original.getObjectType().equals(Scene.getCreatorToken())){
              this.printDebugStatement(
                "      + VisualSpatialFieldObject isn't the creator so it will " +
                "be cloned and added to the cloned VisualSpatialField"
              );
              VisualSpatialFieldObject clone = original.createClone();
              visualSpatialFieldBeforeMovesApplied.addObjectToCoordinates(col, row, clone, time);
            }
          }
        }
      }
      
      //Track the time taken so far to process the object moves.  Used to 
      //assign terminus values for VisualSpatialFieldObjects moved and to update 
      //the attention clock.
      time += this._timeToAccessVisualSpatialField;   
      this.printDebugStatement("- Time moves begin: " + time);
      
      //Process each object move sequence.
      try{
        for(int objectMoveSequence = 0; objectMoveSequence < moveSequences.size(); objectMoveSequence++){

          //Get the first move sequence for an object and check to see if at 
          //least one movement has been specified for it.
          ArrayList<ItemSquarePattern> moveSequence = moveSequences.get(objectMoveSequence);
          this.printDebugStatement("- Processing move sequence " + objectMoveSequence);
          
          if(moveSequence.size() >= 2){
            this.printDebugStatement("   ~ Move sequence has more than 1 move");

            //Extract the information for the object to move.
            ItemSquarePattern moveFromDetails = moveSequence.get(0);
            String moveFromIdentifier = moveFromDetails.getItem();
            int colToMoveFrom = moveFromDetails.getColumn();
            int rowToMoveFrom = moveFromDetails.getRow();

            //Process each move for this object starting from the first element of 
            //the current second dimension array.
            for(int movement = 1; movement < moveSequence.size(); movement++){
              
              //Get the details of the object movement.
              ItemSquarePattern moveToDetails = moveSequence.get(movement);
              String moveToIdentifier = moveToDetails.getItem();
              int colToMoveTo = moveToDetails.getColumn();
              int rowToMoveTo = moveToDetails.getRow();
              
              this.printDebugStatement("   ~ Move from details: " + moveFromDetails.toString());
              this.printDebugStatement("   ~ Move to details: " + moveToDetails.toString());
              
              //Check to see if the identifier given for this move is the same
              //as that declared initially. If it isn't, serial movement is not
              //implemented so the entire move sequence should fail.
              if( moveFromIdentifier.equals(moveToIdentifier) ){
                this.printDebugStatement("   ~ Move refers to the same VisualSpatialFieldObject");

                List<VisualSpatialFieldObject> objectsOnSquareToMoveFrom = visualSpatialField.getCoordinateContents(colToMoveFrom, rowToMoveFrom, time, false);
                VisualSpatialFieldObject objectToMove = null;
                this.printDebugStatement("   ~ Checking for VisualSpatialFieldObject on VisualSpatialField coordinates to move from");
                
                for(VisualSpatialFieldObject objectOnSquareToMoveFrom : objectsOnSquareToMoveFrom){
                  this.printDebugStatement("      + Checking VisualSpatialFieldObject with details:" + objectOnSquareToMoveFrom.toString());
                  
                  if(
                    objectOnSquareToMoveFrom.getIdentifier().equals(moveFromIdentifier) &&
                    objectOnSquareToMoveFrom.isAlive(time)
                  ){
                    this.printDebugStatement("         = This is the VisualSpatialFieldObject to move and it is alive so it will be moved.");
                    objectToMove = objectOnSquareToMoveFrom;
                    break;
                  }
                }
                
                if(objectToMove != null){
                    
                  //Refresh the termini of any VisualSpatialFieldObjects on
                  //the coordinates to move the VisualSpatialFieldObject from
                  //now, before time is incremented by the time taken by this
                  //CHREST model to move a VisualSpatialFieldObject in a 
                  //VisualSpatialField.
                  this.refreshVisualSpatialFieldObjectTermini(visualSpatialField, colToMoveFrom, rowToMoveFrom, time);
                    
                  //Remove the object from the visual-spatial coordinates at
                  //this time by setting its terminus to the time the 
                  //visual-spatial field is accessed.
                  objectToMove.setTerminus(time, true);
                  this.printDebugStatement("         = Terminus of VisualSpatialFieldObject to move set to " + objectToMove.getTerminus());
                    
                  //Check to see if the VisualSpatialField coordinates should 
                  //be re-encoded as an empty square. This should occur if the 
                  //VisualSpatialFieldObject being moved is not co-habiting 
                  //the square with any VisualSpatialFieldObjects that denote 
                  //physical (non-empty square) VisualSpatialFieldObjects that 
                  //are currently alive.
                  this.printDebugStatement(
                    "         = Checking if the VisualSpatialField " +
                    "coordinates should be encoded as an empty square.  " +
                    "This will not occur if any VisualSpatialFieldObject on " +
                    "the coordinates is not the VisualSpatialFieldObject " +
                    "being moved and is alive at time " + time + " or is the " +
                     "creator and its terminus has not yet been set"
                  );
                  
                  boolean makeSquareToMoveFromEmpty = true;
                  for(VisualSpatialFieldObject objectToCheck : objectsOnSquareToMoveFrom){
                    this.printDebugStatement("            > Checking VisualSpatialObject:" + objectToCheck.toString());

                    if(
                      (
                        !objectToCheck.getIdentifier().equals(objectToMove.getIdentifier()) &&
                        objectToCheck.isAlive(time)
                      )
                      ||
                      (
                        objectToCheck.getIdentifier().equals(Scene.getCreatorToken()) &&
                        objectToCheck.getTerminus() == null
                      )
                    ){
                      this.printDebugStatement(
                        "         = This is not the VisualSpatialFieldObject " +
                        "to move and is alive at time " + time + " or is the " +
                        "creator and its terminus has not been set so the " +
                        "coordinates to move the VisualSpatialFieldObject from " +
                        "should not be encoded as an empty square"
                      );
                      makeSquareToMoveFromEmpty = false;
                      break;
                    }
                  }
                  this.printDebugStatement(
                    "         = The coordinates will " + (makeSquareToMoveFromEmpty ? "" : "not") +
                    "be encoded as an empty square after the VisualSpatialFieldObject has been moved"
                  );
                    
                  if(makeSquareToMoveFromEmpty){
                    VisualSpatialFieldObject emptySquare = new VisualSpatialFieldObject(
                      Scene.getEmptySquareToken(),
                      this,
                      visualSpatialField,
                      time,
                      false,
                      true
                    );

                    visualSpatialField.addObjectToCoordinates(colToMoveFrom, rowToMoveFrom, emptySquare, time);
                  }
                    
                  //Increment the time by the time taken by this model to move 
                  //a VisualSpatialFieldObject in a VisualSpatialField.  Do 
                  //this now since it should still take time to move a 
                  //VisualSpatialFieldObject even if it is moved to 
                  //VisualSpatialField coordinates not represented in the 
                  //VisualSpatialField (the "putting-down" step of the move is 
                  //not actually performed in this case).
                  this.printDebugStatement("\n      + Incrementing current time (" + time + ") by the " +
                    "time taken by this CHREST model to move a " +
                    "VisualSpatialFieldObject (" + 
                    this._timeToMoveVisualSpatialFieldObject + ")"
                  );
                  time += this._timeToMoveVisualSpatialFieldObject;
                  this.printDebugStatement("      + Time now equal to " + time);
                    
                  //Create a new VisualSpatialFieldObject that represents the 
                  //VisualSpatialFieldObject after the move.  It is assumed 
                  //that the VisualSpatialFieldObject is unrecognised.
                  objectToMove = new VisualSpatialFieldObject(
                    objectToMove.getIdentifier(),
                    objectToMove.getObjectType(),
                    this,
                    visualSpatialField,
                    time,
                    false,
                    true
                  );
                    
                  this.printDebugStatement(
                    "      + Created the VisualSpatialFieldObject to be " + 
                    "added to the VisualSpatialField coordinates to move " +
                    "the VisualSpatialFieldObject to:" + objectToMove.toString()
                  );
                
                  this.printDebugStatement("   ~ VisualSpatialFieldObject 'picked-up' successfully");
                  
                  List<VisualSpatialFieldObject> contentsOfCoordinatesToMoveTo = visualSpatialField.getCoordinateContents(colToMoveTo, rowToMoveTo, time, false);               
                  if(contentsOfCoordinatesToMoveTo != null){
                    
                    this.printDebugStatement(
                      "   ~ VisualSpatialField coordinates to move the " + 
                      "VisualSpatialFieldObject to (" + colToMoveTo + ", " + 
                      rowToMoveTo + ") are represented in the VisualSpatialField " +
                      "so the VisualSpatialFieldObject will be moved there"
                    );
                    
                    //Process the termini of objects on the square to be moved
                    //to.
                    this.printDebugStatement("   ~ Updating termini of VisualSpatialFieldObjects on VisualSpatialFieldcoordinates to move to");
                    this.refreshVisualSpatialFieldObjectTermini(visualSpatialField, colToMoveTo, rowToMoveTo, time);
                    
                    //Now, "move" the object to be moved to its destination 
                    //coordinates.
                    visualSpatialField.addObjectToCoordinates(colToMoveTo, rowToMoveTo, objectToMove, time);
                    if(this.debug()){
                      this.printDebugStatement("   ~ Added VisualSpatialFieldObject to VisualSpatialFieldCoordinates to move to.  Coordinate content:");
                      for(VisualSpatialFieldObject objectOnSquareToMoveTo : visualSpatialField.getCoordinateContents(colToMoveTo, rowToMoveTo, time, false)){
                        this.printDebugStatement("\n" + objectOnSquareToMoveTo.toString());
                      }
                    }
                  }
                  else{
                    this.printDebugStatement(
                      "   ~ Coordinates to move VisualSpatialFieldObject to " +
                      "are not represented in the VisualSpatialField so the " +
                      "VisualSpatialFieldObject will not be 'put-down'.  Skipping " +
                      "remaining moves for this VisualSpatialFieldObject now"
                    );
                    break;
                  }
                  
                  //Set the current location of the VisualSpatialFieldObject to 
                  //be its destination so that the next move can be processed 
                  //correctly.
                  moveFromDetails = moveToDetails;
                }
                //The object is not at the location specified.
                else{
                  this.printDebugStatement(
                    "   ~ VisualSpatialFieldObject not found. Checking if the " +
                    "location specified is incorrect or if the " +
                    "VisualSpatialFieldObject has decayed."
                  );
                  
                  for(int col = 0; col < visualSpatialField.getWidth(); col++){
                    for(int row = 0; row < visualSpatialField.getHeight(); row++){
                      for(VisualSpatialFieldObject vsfo : visualSpatialField.getCoordinateContents(col, row, time, false)){
                        if(vsfo.getIdentifier().equals(moveFromIdentifier)){
                          throw new VisualSpatialFieldException(
                            "The initial location specified for the following " +
                            "VisualSpatialFieldObject is incorrect:" + 
                            vsfo.toString() + 
                            "\n- Initial location specified: (" +
                            colToMoveFrom + ", " + rowToMoveFrom + ")" +
                            "\n- Actual location: (" + col + ", " + row + ")"
                          );
                        }
                      }
                    }
                  }
                  
                  this.printDebugStatement(
                    "   ~ VisualSpatialFieldObject is not present on the " +
                    "VisualSpatialField at time " + time + " so it must have " +
                    "decayed.  Skipping to the next VisualSpatialFieldObject " +
                    "move sequence."
                  );
                  break;
                }
              }
              else{
                this.printDebugStatement("- The VisualSpatialFieldObject to move is not consistently referred to in the sequence being processed, exiting");
                throw new VisualSpatialFieldException(
                  "Sequence " + objectMoveSequence + " does not consistently " +
                  "refer to the same VisualSpatialFieldObject (move " + movement + " refers to " +
                  moveToIdentifier + " so serial movement not implemented."
                );
              }
            }//End move for an object.
          }//End check for number of object moves being greater than or equal to 2.
          else{
            this.printDebugStatement("- VisualSpatialFieldObject move sequence only contains the initial location for a VisualSpatialFieldObject, exiting");
            throw new VisualSpatialFieldException(
              "The move sequence " + moveSequence.toString() + " does not " +
              "contain any moves after the current location of the " +
              "VisualSpatialFieldObject is specified."
            );
          }
        }//End entire movement sequence for all objects.
      } 
      catch (VisualSpatialFieldException e){
        this.printDebugStatement(
          "   - VisualSpatialFieldObjectMoveException thrown, reverting " +
          "VisualSpatialField to its state before moves were processed.  " +
          "Attention clock will remain unchanged."
        );
        
        this._visualSpatialFields.replace(this._visualSpatialFields.lastEntry().getKey(), visualSpatialFieldBeforeMovesApplied);
        throw e;
      }
    }
    else{
      this.printDebugStatement("- Attention is not free, exiting");
    }
    
    this.printDebugStatement(
      "- VisualSpatialFieldObject move sequence processed successfully.  " +
      "Setting attention clock to time " + time
    );
    this._attentionClock = time;
    this.printDebugStatement("===== RETURN =====");
  }
  
     /**
   * Intended for use during {@link jchrest.architecture.VisualSpatialField}
   * construction: encodes a {@link jchrest.domainSpecifics.SceneObject} as a 
   * {@link jchrest.lib.VisualSpatialFieldObject} on the {@link 
   * jchrest.architecture.VisualSpatialField} coordinates specified at the 
   * {@code encodingTime} specified if there are no other {@link 
   * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} occupying
   * these coordinates (usually, a {@link 
   * jchrest.architecture.VisualSpatialField} can accommodate multiple {@link 
   * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} on the same
   * coordinates).
   * <p>
   * If a new {@link jchrest.lib.VisualSpatialFieldObject} is encoded, its 
   * creation time will be set to the {@code time} specified and its 
   * terminus will be set according to the values of the {@code time} and {@code 
   * sceneObjectRecognised} specified: if {@code sceneObjectRecognised} is set 
   * to {@link java.lang.Boolean#TRUE} the {@link 
   * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObject's} terminus 
   * will be set to the sum of {@code time} and {@link 
   * #this#getRecognisedVisualSpatialFieldObjectLifespan()}, if {@code 
   * sceneObjectRecognised} is set to {@link java.lang.Boolean#FALSE} the {@link 
   * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObject's} terminus 
   * will be set to the sum of {@code time} and {@link 
   * #this#getUnrecognisedVisualSpatialFieldObjectLifespan()}.
   * 
   * @param visualSpatialField The {@link 
   * jchrest.architecture.VisualSpatialField} to encode the new {@link 
   * jchrest.lib.VisualSpatialFieldObject} on, if applicable.
   * @param col The column in the {@link 
   * jchrest.architecture.VisualSpatialField} to encode the new {@link 
   * jchrest.lib.VisualSpatialFieldObject} on, if applicable.
   * @param row The row in the {@link 
   * jchrest.architecture.VisualSpatialField} to encode the new {@link 
   * jchrest.lib.VisualSpatialFieldObject} on, if applicable.
   * @param sceneObjectToEncode
   * @param time The creation time of the new {@link 
   * jchrest.lib.VisualSpatialFieldObject}.
   * @param sceneObjectRecognised Determines the recognised status of the {@link
   * jchrest.lib.VisualSpatialFieldObject} to be encoded.  Set to {@link 
   * java.lang.Boolean#TRUE} to specify that the {@link
   * jchrest.lib.VisualSpatialFieldObject} to be encoded is recognised, set to 
   * {@link java.lang.Boolean#FALSE} to specify that it is unrecognised (see
   * {@link jchrest.lib.VisualSpatialFieldObject#setRecognised(int, boolean)).
   * 
   * @return {@link java.lang.Boolean#TRUE} if a new {@link 
   * jchrest.lib.VisualSpatialFieldObject} was encoded or {@link 
   * java.lang.Boolean#FALSE} if not.
   */
  private boolean encodeVisualSpatialFieldObjectDuringVisualSpatialFieldConstruction(
    VisualSpatialField visualSpatialField,
    int col,
    int row,
    SceneObject sceneObjectToEncode,
    int time,
    boolean sceneObjectRecognised
  ){
    this.printDebugStatement("\n===== Chrest.encodeVisualSpatialFieldObjectDuringVisualSpatialFieldConstruction() =====");
    this.printDebugStatement("- Checking if the SceneObject should have a VisualSpatialFieldObject " +
      "representation encoded on visual-spatial coordinates (" + col +
      ", " + row + ")");
    
    boolean visualSpatialFieldObjectCreated = false;

    
    List<VisualSpatialFieldObject> coordinateContents = visualSpatialField.getCoordinateContents(col, row, time, false);
    this.printDebugStatement("- Contents of coordinates:");
    if(this.debug()){
      for(VisualSpatialFieldObject coordinateContent : coordinateContents){
        this.printDebugStatement(coordinateContent.toString());
      }
    }

    if(coordinateContents.isEmpty()){

      this.printDebugStatement("\n- Attempting to create a VisualSpatialObject representing the " +
        "SceneObject with " + sceneObjectToEncode.toString() + " at time " + 
        time
      );

      VisualSpatialFieldObject visualSpatialFieldObject = new VisualSpatialFieldObject(
        sceneObjectToEncode.getIdentifier(),
        sceneObjectToEncode.getObjectType(),
        this,
        visualSpatialField,
        time,
        sceneObjectRecognised,
        true
      );

      this.printDebugStatement("- VisualSpatialFieldObject created:\n" + visualSpatialFieldObject.toString());

      try {
        visualSpatialFieldObjectCreated = visualSpatialField.addObjectToCoordinates(
          col,
          row,
          visualSpatialFieldObject,
          time
        );
      } catch (VisualSpatialFieldException ex) {
        Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    
    this.printDebugStatement("===== RETURN =====");
    return visualSpatialFieldObjectCreated;
  }
  
  /**
   * Updates the terminus of all {@link jchrest.lib.VisualSpatialFieldObject 
   * VisualSpatialFieldObjects} that are on all coordinates specified by 
   * subtracting/adding the result of invoking {@link 
   * jchrest.architecture.Perceiver#getFixationFieldOfView()} in context of the
   * {@link jchrest.architecture.Perceiver} associated with {@link #this} from 
   * the {@code visualSpatialFieldCol} and {@code visualSpatialFieldRow} 
   * specified at the {@code time} specified.
   * <p>
   * So, if {@code visualSpatialFieldCol} and {@code visualSpatialFieldRow} are
   * equal to 2, the {@code visualSpatialField} specified has dimensions 5 * 5 
   * and {@link jchrest.architecture.Perceiver#getFixationFieldOfView()} returns 
   * 2, all {@link jchrest.lib.VisualSpatialFieldObject 
   * VisualSpatialFieldObjects} in the {@code visualSpatialField} specified will
   * have their terminus updated if they are "alive" (see {@link 
   * jchrest.lib.VisualSpatialFieldObject#isAlive(int)}) at {@code time}.
   * 
   * @param visualSpatialField
   * @param visualSpatialFieldCol
   * @param visualSpatialFieldRow
   * @param time 
   */
  void refreshVisualSpatialFieldObjectTermini(
    VisualSpatialField visualSpatialField,
    int visualSpatialFieldCol, 
    int visualSpatialFieldRow, 
    int time
  ){
    this.printDebugStatement("\n===== Chrest.refreshVisualSpatialFieldObjectTermini() =====");
    this.printDebugStatement(
      "- Refreshing termini of VisualSpatialFieldObjects alive at time " + time + 
      " on visual-spatial field coordinates that are " + 
      this.getPerceiver().getFixationFieldOfView() + " square around " +
      "coordinates being processed (" + visualSpatialFieldCol + ", " +
      visualSpatialFieldRow + ")"
    );
    
    TreeMap<Double, String> unknownProbabilities = new TreeMap();
    unknownProbabilities.put(1.0, Scene.getBlindSquareToken());
    Scene visualSpatialFieldAsScene = visualSpatialField.getAsScene(time, unknownProbabilities);
    ListPattern visualSpatialFieldObjectsInScope = visualSpatialFieldAsScene.getItemsInScopeAsListPattern(
      visualSpatialFieldCol,
      visualSpatialFieldRow,
      this.getPerceiver().getFixationFieldOfView()
    );

    for(PrimitivePattern visualSpatialFieldObjectInScope : visualSpatialFieldObjectsInScope){
      ItemSquarePattern visualSpatialFieldObject = (ItemSquarePattern)visualSpatialFieldObjectInScope;
      
      int domainSpecificCol = visualSpatialFieldAsScene.getDomainSpecificColFromSceneSpecificCol(visualSpatialFieldObject.getColumn());
      int domainSpecificRow = visualSpatialFieldAsScene.getDomainSpecificRowFromSceneSpecificRow(visualSpatialFieldObject.getRow());

      Integer visualSpatialCol = visualSpatialField.getVisualSpatialFieldColFromDomainSpecificCol(domainSpecificCol);
      Integer visualSpatialRow = visualSpatialField.getVisualSpatialFieldRowFromDomainSpecificRow(domainSpecificRow);
      
      this.printDebugStatement("   ~ Processing VisualSpatialObjects on coordinates (" + visualSpatialCol + ", " + visualSpatialRow + ")");
      List<VisualSpatialFieldObject> objectsOnCoordinates = visualSpatialField.getCoordinateContents(
        visualSpatialCol,
        visualSpatialRow,
        time, 
        false
      );
      
      for(VisualSpatialFieldObject objectOnCoordinates : objectsOnCoordinates){
        this.printDebugStatement("   ~ Processing VisualSpatialFieldObject:\n" + objectOnCoordinates.toString());
        this.printDebugStatement("\n   ~ Checking if this VisualSpatialFieldObject is alive and doesn't have a null terminus");
        if(objectOnCoordinates.isAlive(time) && objectOnCoordinates.getTerminus() != null){
          this.printDebugStatement("   ~ VisualSpatialFieldObject is alive and doesn't have a null terminus. Refreshing terminus at time " + time + ".");
          objectOnCoordinates.setTerminus(time, false);
          this.printDebugStatement("   ~ Terminus = " + objectOnCoordinates.getTerminus());
        }
      }
    }
    
    this.printDebugStatement("===== RETURN =====");
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

  public Theory getReinforcementLearningTheory(){
    return this._reinforcementLearningTheory;
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
  public void setReinforcementLearningTheory(Theory theorySpecified){
    if(_reinforcementLearningTheory == null){
      Theory[] theories = ReinforcementLearning.getReinforcementLearningTheories();
      for(Theory theory : theories){
        if(theorySpecified.equals(theory)){
          _reinforcementLearningTheory = theory;
          break;
        }
      }
    }
  }
  
  /**
   * @return All instance variables ending with "Clock" for this {@link #this}
   * instance.
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
   * @return The value of the clock with the maximum value 
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
}
