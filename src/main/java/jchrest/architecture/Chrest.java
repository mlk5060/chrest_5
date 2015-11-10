// Copyright (c) 2012, Peter C. R. Lane
// with contributions on the emotions code by Marvin Schiller.
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.architecture;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
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
import jchrest.lib.*;
import jchrest.lib.ReinforcementLearning.ReinforcementLearningTheories;
import org.reflections.Reflections;

/**
 * The parent class for an instance of a Chrest model.
 * 
 * @author Peter C. R. Lane
 */
public class Chrest extends Observable {
  
  private DomainSpecifics _domainSpecifics;
  
  //Indicates whether CHREST is currently engaged in an experiment.  Has 
  //implications in execution history recording, Node history updates and 
  //CHREST model state drawing.  By default, a new CHREST instance will be 
  //loaded into and engaged with an experiment.  The difference between the 
  //concepts of "loaded into" and "engaged in" concern whether the model has
  //done something in an experiment.
  private boolean _loadedIntoExperiment = true;
  private boolean _engagedInExperiment = true;
  
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
  
  /************************************/
  /***** Internal clock variables *****/
  /************************************/
  
  // =====================
  // ===== IMPORTANT =====
  // =====================
  //
  // When declaraing a new clock, please ensure that its instance variable name 
  // ends with "Clock".  This will ensure that automated operations using Java 
  // refelection will work with new variables without having to implement 
  // specific code for the new variable.
  
  //Used to control access to the "attention" resource by comparing the time in 
  //the environment this model is situated in against the value of this 
  //variable.
  private int _attentionClock;
  
  //Used to control access to the "learning" resource by comparing the time in 
  //the environment this model is situated in against the value of this 
  //variable.
  private int _learningClock;
  
  //LTM-related time parameters.
  private int _addLinkTime;
  private int _discriminationTime;
  private int _familiarisationTime;
  
  // rho is the probability that a given learning operation will occur
  private float _rho;
  
  // parameter for construction of semantic link
  private boolean _createSemanticLinks;
  
  // - determines number of overlapping items in node images
  private int _similarityThreshold;
  
  // - determines maximum distance to search semantic links
  private int _maximumSemanticDistance = 1;
  
  // template construction parameters
  private boolean _createTemplates;
  private int _minTemplateLevel = 3;
  private int _minTemplateOccurrences = 2;
  
  //Long-term-memory (LTM) holds information within the model permanently and
  //can be cloned.
  private int _totalNodes;
  private final int _drawingAndHistoryThreshold = 5000; //How many nodes in LTM is too many for Node history updates and LTM drawing to occur.
  private Node _visualLtm;
  private Node _verbalLtm;
  private Node _actionLtm;
  
  // short-term-memory holds information within the model temporarily, usually within one experiment
  private final Stm _visualStm;
  private final Stm _verbalStm;
  private final Stm _actionStm; // TODO: Incorporate into displays
  
  // Perception module
  private final Perceiver _perceiver;
  
  //Stores domain times as keys and VisualSpatialField instances as values.  
  //Since a VisualSpatialField can only encode one Scene instance, multiple 
  //instances may be required throughout the lifespan of one CHREST model.  Also
  //enables correct visualisation of a VisualSpatialField at any point in 
  //time.
  private final TreeMap<Integer, VisualSpatialField> _visualSpatialFields = new TreeMap<>();
  
  // Emotions module
  private EmotionAssociator _emotionAssociator;
  
  //Reinforcement learning module
  private ReinforcementLearningTheories _reinforcementLearningTheory;

  public Chrest () {
    
    //TODO: Pass DomainSpecifics sub-class in constructor to make it explicitly 
    //clear that the domain in which CHREST is located is important.  If the 
    //parameter passed is null, don't alter the CHREST model's _domainSpecifics
    //variable (its set to GenericDomain by default).
    this._domainSpecifics = new GenericDomain(this);
    
    this._databaseInterface = new DatabaseInterface(null);
    
    /*********************************************/
    /***** Execution history DB table set-up *****/
    /*********************************************/
    
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
    
    //TODO: All remaining parameters could be set in their declarations above 
    //rather than in the constructor since they are being assigned "simply" (no
    //logic changes the assignment value) and they do not require access to any
    //other instance variables (as above).
    
    /***********************************/
    /***** Set learning parameters *****/
    /***********************************/
    _addLinkTime = 10000;
    _discriminationTime = 10000;
    _familiarisationTime = 2000;
    _rho = 1.0f;
    _similarityThreshold = 4;

    /******************************/
    /***** Set LTM parameters *****/
    /******************************/
    
    this.setClocks(0);
    _totalNodes = 0;
    _visualLtm = new Node (this, 0, jchrest.lib.Pattern.makeVisualList (new String[]{"Root"}), 0);
    _verbalLtm = new Node (this, 0, jchrest.lib.Pattern.makeVerbalList (new String[]{"Root"}), 0);
    _actionLtm = new Node (this, 0, jchrest.lib.Pattern.makeActionList (new String[]{"Root"}), 0);
    _totalNodes = 0; // Node constructor will have incremented _totalNodes, so reset to 0
    _visualStm = new Stm (4, this.getLearningClock());
    _verbalStm = new Stm (2, this.getLearningClock());
    _actionStm = new Stm (4, this.getLearningClock());
    _emotionAssociator = new EmotionAssociator ();
    _reinforcementLearningTheory = null; //Must be set explicitly using Chrest.setReinforcementLearningTheory()
    _perceiver = new Perceiver (this, 2);
            
    /***************************************/
    /***** Set boolean learning values *****/
    /***************************************/
    _createTemplates = true;
    _createSemanticLinks = true;
  }
  
  /****************************************************************************/
  /****************************************************************************/
  /**************************** EXECUTION HISTORY *****************************/
  /****************************************************************************/
  /****************************************************************************/
  
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
  public boolean canUpdateNodeHistoryOrDrawLtmState(){
    return this.getTotalLtmNodes() < this._drawingAndHistoryThreshold;
  }
  
  /**
   * Clones the state of all LTM modalities at the time specified for the 
   * experiment specified.
   * 
   * @param time The time that the cloned LTM should reflect.  If you specify 
   * 10000 then the cloned LTM state will reflect the state of original LTM
   * modality specified at time 10000. 
   * @param experimentName 
   */
  public void cloneLtm(int time){
    
    for(Field field1 : Chrest.class.getDeclaredFields()){
      
      //Store the name of the field since it may be used twice.
      String fieldName = field1.getName();
      
      //Check for LTM instance variable.
      if(fieldName.endsWith("Ltm")){
        
        //This field is a LTM instance variable so get its current value and
        //check to see if the value's type is Node.  If so, continue.
        try {
          Object ltmObject = field1.get(this);
          if(ltmObject instanceof Node){
            
            //Safely cast the field value to be a Node and clone it.  Since this
            //is a LTM modality root node, the entirety of LTM will be cloned.
            Node ltm = (Node)ltmObject;
            ltm.deepClone(time);
          }
        } catch (IllegalArgumentException | IllegalAccessException ex) {
          Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
  }
  
  /**
   * Retrieves the cloned version of the specified LTM modality.
   * 
   * @param modality The {@link jchrest.lib.Modality} of the LTM to retrieve.
   * 
   * @return The root node for the LTM clone specified or null if the LTM 
   * modality specified has not been cloned yet.
   */
  public Node getClonedLtm(Modality modality){
    Node result = null;
    
    String modalityString = modality.toString().toLowerCase();
    for(Field field : Chrest.class.getDeclaredFields()){
      if(field.getName().endsWith("_" + modalityString + "Ltm")){
        try {
          Object value = field.get(this);
          if(value instanceof Node){
            result = (Node)value;
            result = result.getClone();
          }
        } catch (IllegalArgumentException | IllegalAccessException ex) {
          Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
    
    return result;
  }
  
  /**
   * Clears all LTM clones currently associated with this model.
   */
  public void clearClonedLtm(){
    for(Modality modality : Modality.values()){
      Node clonedModalityRootNode = this.getClonedLtm(modality);
      if(clonedModalityRootNode != null){
        this.clearClonedLtm(clonedModalityRootNode);
      }
    }
  }
  
  /**
   * Actually implements the LTM clone clearing referenced in {@link 
   * jchrest.architecture.Chrest#clearClonedLtm()}.
   * 
   * @param node The node whose clone is to be cleared. 
   */
  private void clearClonedLtm(Node node){
    for(Link childLink : node.getChildren()){
      this.clearClonedLtm(childLink.getChildNode());
    }
    node.clearClone();
  }

  /**
   * Retrieve the model's current domain specification.
   */
  public DomainSpecifics getDomainSpecifics () {
    HashMap<String, Object> historyRow = new HashMap<>();
    
    //Generic operation name setter for current method.  Ensures for the row to 
    //be added that, if this method's name is changed, the entry for the 
    //"Operation" column in the execution history table will be updated without 
    //manual intervention and "Filter By Operation" queries run on the execution 
    //history DB table will still work.
    class Local{};
    historyRow.put(Chrest._executionHistoryTableOperationColumnName, 
      ExecutionHistoryOperations.getOperationString(this.getClass(), Local.class.getEnclosingMethod())
    );
    historyRow.put(Chrest._executionHistoryTableOutputColumnName, this._domainSpecifics.getClass().getSimpleName());
    this.addEpisodeToExecutionHistory(historyRow);
    
    return _domainSpecifics;
  }

  /**
   * Set the domain specification.
   */
  public void setDomain (DomainSpecifics domain) {
    _domainSpecifics = domain;
  }

  /**
   * Accessor to retrieve time to add a new link.
   */
  public int getAddLinkTime () {
    return _addLinkTime;
  }

  /**
   * Modify time to add a new link.
   */
  public void setAddLinkTime (int time) {
    _addLinkTime = time;
  }

  /**
   * Accessor to retrieve time to discriminate a new node.
   */
  public int getDiscriminationTime () {
    return _discriminationTime;
  }

  /**
   * Modify time to discriminate a new node.
   */
  public void setDiscriminationTime (int time) {
    _discriminationTime = time;
  }

  /**
   * Accessor to retrieve time to familiarise image of a node.
   */
  public int getFamiliarisationTime () {
    return _familiarisationTime;
  }

  /**
   * Modify time to familiarise image of a node.
   */
  public void setFamiliarisationTime (int time) {
    _familiarisationTime = time;
  }

  /**
   * Accessor to retrieve value of rho, the probability of learning an item.
   */
  public float getRho () {
    return _rho;
  }

  /**
   * Modify value of rho, the probability of learning an item.
   */
  public void setRho (float rho) {
    _rho = rho;
  }

  /**
   * Accessor to retrieve value of similarity threshold, the number of items 
   * which must be shared between two images for a semantic link to be formed.
   */
  public float getSimilarityThreshold () {
    return _similarityThreshold;
  }

  /**
   * Modify value of similarity threshold.
   */
  public void setSimilarityThreshold (int threshold) {
    _similarityThreshold = threshold;
  }

  /**
   * Modify option to create semantic links.
   */
  public void setCreateSemanticLinks (boolean value) {
    _createSemanticLinks = value;
  }

  /**
   * Accessor to option of whether to create semantic links.
   */
  public boolean getCreateSemanticLinks () {
    return _createSemanticLinks;
  }

  /**
   * Modify option to create templates.
   */
  public void setCreateTemplates (boolean value) {
    _createTemplates = value;
  }

  /**
   * Accessor to option of whether to create templates.
   */
  public boolean getCreateTemplates () {
    return _createTemplates;
  }

  /**
   * Accessor to value of minimum template level.
   */
  protected int getMinTemplateLevel () {
    return _minTemplateLevel;
  }

  /**
   * Accessor to minimum require occurrences for forming template.
   */
  protected int getMinTemplateOccurrences () {
    return _minTemplateOccurrences;
  }

  /**
   * Modify values for template construction.
   */
  public void setTemplateConstructionParameters (int minLevel, int minOccurrences) {
    _minTemplateLevel = minLevel;
    _minTemplateOccurrences = minOccurrences;
  }

  /**
   * Accessor to retrieve the size of visual short-term memory.
   */
  public int getVisualStmSize () {
    return _visualStm.getSize ();
  }

  /**
   * Modify size of visual short-term memory.
   */
  public void setVisualStmSize (int size) {
    _visualStm.setSize (size);
    setChanged ();
    if (!_frozen) notifyObservers ();
  }
  
  /**
   * Accessor to retrieve the number of nodes currently in the visual short-term
   * memory. 
   */
  public int getVisualStmNodeCount(){
    return _visualStm.getCount();
  }

  /**
   * Accessor to retrieve the size of verbal short-term memory.
   */
  public int getVerbalStmSize () {
    return _verbalStm.getSize ();
  }

  /**
   * Modify size of verbal short-term memory.
   */
  public void setVerbalStmSize (int size) {
    _verbalStm.setSize (size);
    setChanged ();
    if (!_frozen) notifyObservers ();
  }
  
  /**
   * Accessor to retrieve the number of nodes currently in the verbal short-term
   * memory. 
   */
  public int getVerbalStmNodeCount(){
    return _verbalStm.getCount();
  }
  
  /**
   * Resets this model's learning clock to 0.
   */
  public void resetAttentionClock(){
    this._attentionClock = 0;
    this.setChanged();
    this.notifyObservers();
  }

  /**
   * Retrieve the next available node number.
   * Package access only, as should only be used by Node.java.
   */
  int getNextNodeNumber () {
    _totalNodes += 1;
    return _totalNodes;
  }

  /**
   * Accessor to retrieve the total number of nodes within LTM.
   */
  public int getTotalLtmNodes () {
    return _totalNodes;
  }

  /**
   * Accessor to retrieve visual short-term memory of model.
   */
  public Stm getVisualStm () {
    return _visualStm;
  }

  /**
   * Accessor to retrieve verbal short-term memory of model.
   */
  public Stm getVerbalStm () {
    return _verbalStm;
  }
  
  /**
   * Accessor to retrieve verbal long-term memory of model.
   */
  public Node getVerbalLtm () {
    return _visualLtm;
  }

  /**
   * Accessor to retrieve visual long-term memory of model.
   */
  public Node getVisualLtm () {
    return _visualLtm;
  }

  /** 
   * Return a count of the number of nodes in visual long-term memory.
   */
  public int ltmVisualSize () {
    return _visualLtm.size ();
  }

  /**
   * Return the average depth of nodes in visual long-term memory.
   */
  public double getVisualLtmAverageDepth () {
    return _visualLtm.averageDepth ();
  }

  /**
   * Return the average image size of nodes in visual long-term memory.
   */
  public double getVisualLtmAverageImageSize () {
    return _visualLtm.averageImageSize ();
  }

  /**
   * Return a count of the number of nodes in verbal long-term memory.
   */
  public int ltmVerbalSize () {
    return _verbalLtm.size ();
  }

  /**
   * Return the average depth of nodes in verbal long-term memory.
   */
  public double getVerbalLtmAverageDepth () {
    return _verbalLtm.averageDepth ();
  }

  /**
   * Return a count of the number of nodes in action long-term memory.
   */
  public int ltmActionSize () {
    return _actionLtm.size ();
  }

  /**
   * Return the average depth of nodes in action long-term memory.
   */
  public double getActionLtmAverageDepth () {
    return _actionLtm.averageDepth ();
  }
  
  /**
   * Accessor to retrieve action long-term memory of model.
   */
  public Node getActionLtm () {
    return _actionLtm;
  }
  
  /**
   * Accessor to retrieve action short-term memory of model.
   */
  public Stm getActionStm(){
    return _actionStm;
  }
  
  /**
   * Accessor to retrieve the size of action short-term memory.
   */
  public int getActionStmSize(){
    return _actionStm.getSize();
  }
  
  /**
   * Accessor to retrieve the number of nodes currently in the action short-term
   * memory. 
   */
  public int getActionStmNodeCount(){
    return _actionStm.getCount();
  }

  /**
   * Model is 'experienced' if it has at least 2000 nodes in LTM.
   * This parameter is taken from de Groot and Gobet (1996) to indicate 
   * point when master-level eye heuristics are used instead of novice 
   * ones.
   */
  public boolean isExperienced () {
    if (!_experienced) {
      if (ltmVisualSize()+ltmVerbalSize()+ltmActionSize() > 2000)
        _experienced = true;
    }
    return _experienced;
  }
  private boolean _experienced = false; // for caching experience level

  /**
   * Instruct model to construct templates, if the 'constructTemplates' flag is true.  
   * This method should be called at the end of the learning process.
   * Note, the template construction process only currently works for visual patterns 
   * using the ItemSquarePattern primitive.
   */
  public void constructTemplates (int time) {
    if (_createTemplates) {
      _visualLtm.constructTemplates (time);
    }
  }

  /**
   * Return a count of the number of templates in the model's visual LTM.
   */
  public int countTemplates () {
    return _visualLtm.countTemplates ();
  }

  /**
   * Return the root node of the long-term memory which the given pattern
   * would be sorted through, based on its modality.
   */
  public Node getLtmByModality (ListPattern pattern) {
    if (pattern.isVisual ()) {
      return _visualLtm;
    } else if (pattern.isVerbal ()) {
      return _verbalLtm;
    } else { // if (pattern.isAction ()) 
      return _actionLtm;
    }
  }
  
  public Node getLtmByModality(Modality modality){
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

  private Stm getStmByModality (ListPattern pattern) {
    if (pattern.isVisual ()) {
      return _visualStm;
    } else if (pattern.isVerbal ()) {
      return _verbalStm;
    } else { // if (pattern.isAction ()) 
      return _actionStm;
    }
  }

  // use to freeze/unfreeze updates to the model to prevent GUI
  // seizing up during training
  private boolean _frozen = false;
  
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

  /**
   * Return a map from content sizes to frequencies for the model's LTM.
   */ 
  public Map<Integer, Integer> getContentCounts () {
    Map<Integer, Integer> size = new HashMap<Integer, Integer> ();

    _visualLtm.getContentCounts (size);
    _verbalLtm.getContentCounts (size);
    _actionLtm.getContentCounts (size);

    return size;
  }

  /**
   * Return a map from image sizes to frequencies for the model's LTM.
   */ 
  public Map<Integer, Integer> getImageCounts () {
    Map<Integer, Integer> size = new HashMap<Integer, Integer> ();

    _visualLtm.getImageCounts (size);
    _verbalLtm.getImageCounts (size);
    _actionLtm.getImageCounts (size);

    return size;
  }
  
  /**
   * @return The total number of productions in LTM.
   */
  public int getProductionCount(){
    return this._visualLtm.getProductionCount();
  }

  /**
   * Return a map from number of semantic links to frequencies for the model's LTM.
   */ 
  public Map<Integer, Integer> getSemanticLinkCounts () {
    Map<Integer, Integer> size = new HashMap<Integer, Integer> ();

    _visualLtm.getSemanticLinkCounts (size);
    _verbalLtm.getSemanticLinkCounts (size);
    _actionLtm.getSemanticLinkCounts (size);

    return size;
  }

  /**
   * Add given node to STM.  Check for formation of semantic links by
   * comparing incoming node with the hypothesis, or 'largest', node.
   */
  private void addToStm (Node node, int time) {
    Stm stm = getStmByModality (node.getImage ());

    if (stm.getCount () > 0) {
      Node check = stm.getItem (0); // TODO: make this the hypothesis node
      if (check.getContents().isVisual () && // only add semantic links for visual
          check != node && 
          node.getImage().isSimilarTo (check.getImage (), _similarityThreshold)) {
        node.addSemanticLink (check, time); 
        check.addSemanticLink (node, time); // two-way semantic link
      }
    }

    // TODO: Check if this is the best place
    // Idea is that node's filled slots are cleared when put into STM, 
    // are filled whilst in STM, and forgotten when it leaves.
    node.clearFilledSlots (); 
    stm.add (node, time);

    // inform observers of a change in model's state
    setChanged ();
    if (!_frozen) notifyObservers ();
  }

  /**
   * Accessor to retrieve the model's perceiver object.
   */
  public Perceiver getPerceiver () {
    return _perceiver;
  }

  /** 
   * Retrieve a node in long-term memory using the given ListPattern.
   * The sorting process works through the children of the currentNode.
   * If the link's test matches the remaining part of the pattern, then 
   * the current node is updated, and searching continues through the 
   * children of the new node.
   */
  public Node recognise (ListPattern pattern, int domainTime) {
    Node currentNode = getLtmByModality (pattern);
    
    List<Link> children = currentNode.getChildren ();
    ListPattern sortedPattern = pattern;
    int nextLink = 0;

    while (nextLink < children.size ()) {
      Link link = children.get (nextLink);
      if (link.passes (sortedPattern)) { // descend a test link in network
        // reset the current node, list of children and link index
        currentNode = link.getChildNode ();
        children = link.getChildNode ().getChildren ();
        nextLink = 0;
        // remove the matched test from the sorted pattern
        sortedPattern = sortedPattern.remove (link.getTest ());
      } else { // move on to the next link on same level
        nextLink += 1;
      }
    }

    // try to retrieve a more informative node in semantic links
    currentNode = currentNode.searchSemanticLinks (_maximumSemanticDistance);
    
    // add retrieved node to STM
    addToStm (currentNode, domainTime);

    // return retrieved node
    return currentNode;
  }

  /** 
   * Use given ListPattern to perform a step of learning within the network.
   * First, the pattern is sorted.  Then, if the retrieved node is the 
   * root node or its image mismatches the pattern, discrimination is 
   * used to extend the network.  Otherwise, new information will be added 
   * to the image using the pattern.
   */
  public Node recogniseAndLearn (ListPattern pattern, int time) {
    Node currentNode = recognise (pattern, time);
    if (this.getLearningClock() <= time) { // only try to learn if the learning resource is free at the time of the call
      if (Math.random () < _rho) { // depending on _rho, may refuse to learn some random times
        this.setLearningClock(time); // bring learning clock up to date
        if (!currentNode.getImage().equals (pattern)) { // only try any learning if image differs from pattern
          if (currentNode == getLtmByModality (pattern) || // if is rootnode
            !currentNode.getImage().matches (pattern) || // or mismatch on image
            currentNode.getImage().isFinished () // or image finished
          ) {
            currentNode = currentNode.discriminate (pattern, time); // then discriminate
          } else  { // else familiarise
            currentNode = currentNode.familiarise (pattern, time);
          }
          addToStm (currentNode, time); // add to stm, as node may have changed during learning
        }
      }
    }
    return currentNode;
  }

  /**
   * Used to learn about a new pattern.  Returns the node learnt.
   */
  public Node recogniseAndLearn (ListPattern pattern) {
    return recogniseAndLearn (pattern, this._learningClock);
  }

  /**
   * Used to learn an association between two patterns.  The two patterns may be 
   * of the same or different modality.  Returns the node learnt for the first pattern.
   */
  public Node associateAndLearn (ListPattern pattern1, ListPattern pattern2, int time) {
    if (ListPattern.isSameModality (pattern1, pattern2)) {
      return learnAndLinkPatterns(pattern1, pattern2, time);
    }
    // TODO: Handle differing modalities.
    else if(pattern2.getModalityString().equalsIgnoreCase(Modality.ACTION.toString())){
      return learnPatternsAndCreateProduction(pattern1, pattern2, time);
    }
    else{
      return null;
    }
  }

  public Node associateAndLearn (ListPattern pattern1, ListPattern pattern2) {
    return associateAndLearn (pattern1, pattern2, this.getLearningClock());
  }

  /**
   * Asks Chrest to return the image of the node obtained by sorting given 
   * pattern through the network.
   */
  public ListPattern recallPattern (ListPattern pattern, int domainTime) {
    return recognise(pattern, domainTime).getImage ();
  }

  /** 
   * Asks Chrest to return the image of the node which is associated 
   * with the node obtained by sorting given pattern through the network.
   */
  public ListPattern associatedPattern (ListPattern pattern, int domainTime) {
    Node retrievedNode = recognise (pattern, domainTime);
    if (retrievedNode.getAssociatedNode () != null) {
      return retrievedNode.getAssociatedNode().getImage ();
    } else {
      return null;
    }
  }

  /**
   * Asks Chrest to return the image of the node which names the node 
   * obtained by sorting given pattern through the network.
   */
  public ListPattern namePattern (ListPattern pattern, int domainTime) {
    Node retrievedNode = recognise (pattern, domainTime);
    if (retrievedNode.getNamedBy () != null) {
      return retrievedNode.getNamedBy().getImage ();
    } else {
      return null;
    }
  }
  
  /**
   * Learns first pattern (which can be of any modality) and a second pattern 
   * (whose modality must be "action") and creates a production between the 
   * first pattern and the second pattern pattern
   */
  private Node learnPatternsAndCreateProduction(ListPattern pattern1, ListPattern actionPattern, int time) {
    Node pat1Retrieved = recognise (pattern1, time);
    Boolean actionPatternMatched = false;
    
    // 1. is retrieved node image a match for pattern1?
    if (pat1Retrieved.getImage().matches (pattern1)) {
      
      // 2. does retrieved node have any action links?  If so, check each one to
      // see if it matches actionPattern.
      if (pat1Retrieved.getProductions() != null) {
        HashMap<Node, Double> pattern1ActionLinks = pat1Retrieved.getProductions();
        for (Node currentActionNode : pattern1ActionLinks.keySet()) {
          
          // 3. is linked node image match pattern2? if not, learn pattern2
          if (currentActionNode.getImage().matches (actionPattern)) {
            actionPatternMatched = true;
 
            //   4. if linked node image == pattern2, learn pattern1, else learn pattern2
            if (currentActionNode.getImage().equals (actionPattern)) {
              recogniseAndLearn (pattern1, time); // TODO: this is overlearning?
            }
            else {
              recogniseAndLearn (actionPattern, time);
            } 
          }
        }
        if(!actionPatternMatched){
          recogniseAndLearn (actionPattern, time);
          // force it to correct a mistake
          recogniseAndLearn (pattern1, time);

          if (this.getLearningClock() <= time) {
            Node actionNodeRetrieved = recognise (actionPattern, time);

            // 6. if the action node retrieved's image matches action pattern, learn link, else learn action pattern
            if (actionNodeRetrieved.getImage().matches (actionPattern)) {
              associatePatterns(pat1Retrieved, actionNodeRetrieved, Modality.ACTION.toString(), time);
            }
          }
        }
      }
      else {
        // 5. sort action pattern
        Node actionNodeRetrieved = recognise (actionPattern, time);
        
        // 6. if action node retrieved's image matches action pattern, learn link, else learn action pattern
        if (actionNodeRetrieved.getImage().matches (actionPattern)) {  
          associatePatterns(pat1Retrieved, actionNodeRetrieved, Modality.ACTION.toString(), time);
        } else { // image not a match, so we need to learn action pattern
          recogniseAndLearn (actionPattern, time);
          
          // 5. sort action pattern.
          actionNodeRetrieved = recognise (actionPattern, time);
          
          // 6. if the action node retrieved's image matches action pattern, learn link, else learn action pattern
          if (actionNodeRetrieved.getImage().matches (actionPattern)) {
            associatePatterns(pat1Retrieved, actionNodeRetrieved, Modality.ACTION.toString(), time);
          }
        }
      }
    }
    else { // image not a match, so we need to learn pattern 1
      recogniseAndLearn (pattern1, time);
    }
      
    return pat1Retrieved;
  }
  
  public void reinforceProduction(ListPattern visualPattern, ListPattern actionPattern, Double[] variables, int time){
    
    Node recognisedVisualNode = this.recognise(visualPattern, time);
    Node recognisedActionNode = this.recognise(actionPattern, time);
    
    if(
      visualPattern.getModality().equals(Modality.VISUAL) &&
      actionPattern.getModality().equals(Modality.ACTION) &&
      recognisedVisualNode.getImage().equals(visualPattern) &&
      recognisedActionNode.getImage().equals(actionPattern) 
    ){
      recognisedVisualNode.reinforceProduction(recognisedActionNode, variables, time);
    }
  }

  /**
   * Presents Chrest with a pair of patterns, which it should learn and 
   * then attempt to learn a link.  Assumes the two patterns are of the same 
   * modality.
   */
  private Node learnAndLinkPatterns (ListPattern pattern1, ListPattern pattern2, int time) {
    Node pat1Retrieved = recognise (pattern1, time);
    
   // 1. is retrieved node image a match for pattern1?
    if (pat1Retrieved.getImage().matches (pattern1)) {
      
      // 2. does retrieved node have a lateral link?
      if (pat1Retrieved.getAssociatedNode() != null) {
        // if yes
        //   3. is linked node image match pattern2? if not, learn pattern2
        if (pat1Retrieved.getAssociatedNode().getImage().matches (pattern2)) {
          
          //   if yes
          //   4. if linked node image == pattern2, learn pattern1, else learn pattern2
          if (pat1Retrieved.getAssociatedNode().getImage().equals (pattern2)) {  
            recogniseAndLearn (pattern1, time); // TODO: this is overlearning?
          } else {
            recogniseAndLearn (pattern2, time);
          }
        } else {
          recogniseAndLearn (pattern2, time);
          recogniseAndLearn (pattern1, time);
          
          if (this.getLearningClock() <= time) {
            Node pat2Retrieved = recognise (pattern2, time);
            
            // 6. if pattern2 retrieved node image match for pattern2, learn link, else learn pattern2
            if (pat2Retrieved.getImage().matches (pattern2)) {
              associatePatterns(pat1Retrieved, pat2Retrieved, "", time);
            }
          }
        } 
      } else {
        // if not
        // 5. sort pattern2
        Node pat2Retrieved = recognise (pattern2, time);
        
        // 6. if pattern2 retrieved node image match for pattern2, learn link, else learn pattern2
        if (pat2Retrieved.getImage().matches (pattern2)) {  
          associatePatterns(pat1Retrieved, pat2Retrieved, "", time);
        } 
        else { // image not a match, so we need to learn pattern 2
          recogniseAndLearn (pattern2, time);
          // 5. sort pattern2
          pat2Retrieved = recognise (pattern2, time);
          // 6. if pattern2 retrieved node image match for pattern2, learn link, else learn pattern2
          if (pat2Retrieved.getImage().matches (pattern2)) {
            associatePatterns(pat1Retrieved, pat2Retrieved, "", time);
          }
        }
      }
    } else { // image not a match, so we need to learn pattern 1
      recogniseAndLearn (pattern1, time);
    }
    return pat1Retrieved;
  }

  /**
   * Learns the two patterns assuming the time of presentation is the current 
   * Chrest clock time.
   */
  private void learnAndLinkPatterns (ListPattern pattern1, ListPattern pattern2) {
    learnAndLinkPatterns (pattern1, pattern2, this._attentionClock);
  }
  
  /**
   * Associates two patterns of any modality accordingly.  
   * 
   * @param firstNode The node that the association comes from.
   * @param secondNode The node that the association goes to.
   * @param modalityOfSecondNode The modality of the second node. 
   */
  private void associatePatterns(Node firstNode, Node secondNode, String modalityOfSecondNode, int time){
    if(modalityOfSecondNode.equalsIgnoreCase(Modality.ACTION.toString())){
      firstNode.addProduction(secondNode, time);
    }
    //TODO: Handle verbal and visual patterns differently (if required).
    else{
      firstNode.setAssociatedNode(secondNode, time);
    }
    
    setChanged ();
    if (!_frozen) notifyObservers ();
  }

  /**
   * Learn and link a visual and verbal pattern with a naming link.
   */
  public void learnAndNamePatterns (ListPattern pattern1, ListPattern pattern2, int time) {
    recogniseAndLearn (pattern1, time);
    recogniseAndLearn (pattern2, time);
    if (this.getLearningClock() <= time) {
      if (pattern1.isVisual () && pattern2.isVerbal () && _visualStm.getCount () > 0 && _verbalStm.getCount () > 0) {
        _visualStm.getItem(0).setNamedBy (_verbalStm.getItem (0), time);
        this.advanceLearningClock (getAddLinkTime ());
      }
      setChanged ();
      if (!_frozen) notifyObservers ();
    }
  }

  public void learnAndNamePatterns (ListPattern pattern1, ListPattern pattern2) {
    learnAndNamePatterns (pattern1, pattern2, this.getLearningClock());
  }

  /**
   * Learns the {@link jchrest.lib.Scene} specified.
   * 
   * @param scene The {@link jchrest.lib.Scene} to learn.
   * @param numFixations The number of fixations to make on the 
   * {@link jchrest.lib.Scene} to be learned.
   * @param time The current domain time (in milliseconds).
   */
  public void learnScene (Scene scene, int numFixations, int time) {
    _perceiver.setScene (scene);
    _perceiver.start (numFixations);
    for (int i = 0; i < numFixations; i++) {
      _perceiver.moveEyeAndLearn (time);
    }
  }

  /**
   * Learn the {@link jchrest.lib.Scene} specified with an attached next move.  
   * The move is linked to any chunks in visual STM.
   * 
   * TODO: think about if there should be limitations on this.
   * 
   * @param scene
   * @param move
   * @param numFixations
   * @param time The current domain time (in milliseconds).
   */
  public void learnSceneAndMove (Scene scene, Move move, int numFixations, int time) {
    learnScene (scene, numFixations, time);
    recogniseAndLearn (move.asListPattern ());
    // attempt to link action with each perceived chunk
    if (_visualStm.getCount () > 0 && _actionStm.getCount () > 0) {
      for (Node node : _visualStm) {
        node.addProduction (_actionStm.getItem (0), time);
      }
    }
    setChanged ();
    if (!_frozen) notifyObservers ();
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
   * Return a map of moves vs frequencies.
   * 
   * @param scene
   * @param numFixations
   * @param colour
   * @param time
   * 
   * @return 
   */
  public Map<ListPattern, Integer> getMovePredictions (Scene scene, int numFixations, String colour, int time) {
    scanScene (scene, numFixations, time, false);
    // create a map of moves to their frequency of occurrence in nodes of STM
    Map<ListPattern, Integer> moveFrequencies = new HashMap<ListPattern, Integer> ();
    for (Node node : _visualStm) {
      for (Node action : node.getProductions ().keySet()) {
        if (sameColour(action.getImage(), colour)) {
          if (moveFrequencies.containsKey(action.getImage ())) {
            moveFrequencies.put (
                action.getImage (), 
                moveFrequencies.get(action.getImage ()) + 1
                );
          } else {
            moveFrequencies.put (action.getImage (), 1);
          }
        }
      }
    }
    return moveFrequencies;
  }

  /**
   * Predict a move using a CHUMP-like mechanism.
   * 
   * TODO: Improve the heuristics here.
   * 
   * @param scene
   * @param numFixations
   * @param time
   * 
   * @return 
   */
  public Move predictMove (Scene scene, int numFixations, int time) {
    Map<ListPattern, Integer> moveFrequencies = getMovePredictions (scene, numFixations, null, time);
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
    if (best == null) {
      return new Move ("UNKNOWN", 0, 0);
    } else {
      // list pattern should be one item long, with the first item being an ItemSquarePattern
      if ((best.size () == 1) && (best.getItem(0) instanceof ItemSquarePattern)) {
        ItemSquarePattern move = (ItemSquarePattern)best.getItem (0);
        return new Move (move.getItem (), move.getRow (), move.getColumn ());
      } else {
        return new Move ("UNKNOWN", 0, 0);
      }
    }
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
   * 
   * @return 
   */
  public Move predictMove (Scene scene, int numFixations, String colour, int time) {
    Map<ListPattern, Integer> moveFrequencies = getMovePredictions (scene, numFixations, colour, time);
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
    if (best == null) {
      return new Move ("UNKNOWN", 0, 0);
    } else {
      // list pattern should be one item long, with the first item being an ItemSquarePattern
      if ((best.size () == 1) && (best.getItem(0) instanceof ItemSquarePattern)) {
        ItemSquarePattern move = (ItemSquarePattern)best.getItem (0);
        return new Move (move.getItem (), move.getRow (), move.getColumn ());
      } else {
        return new Move ("UNKNOWN", 0, 0);
      }
    }
  }

  /** 
   * Scan given scene, then return a scene which would be recalled.
   * Default behaviour is to clear STM before scanning a scene.
   * @param scene
   * @param numFixations
   * @param time
   * @return 
   */
  public Scene scanScene (Scene scene, int numFixations, int time, boolean debug) {  
    return scanScene (scene, numFixations, true, time, debug);
  }
  
  /** 
   * Scan given {@link jchrest.lib.Scene} and return a 
   * {@link jchrest.lib.Scene} that would be recalled.
   * 
   * @param scene
   * @param numFixations
   * @param clearStm
   * @param time The current domain time (in milliseconds).
   * 
   * @return A {@link jchrest.lib.Scene} instance composed of objects from the 
   * {@link jchrest.lib.Scene} instance passed as a parameter that this 
   * {@link jchrest.architecture.Chrest} instance recognises.
   */
  public Scene scanScene (Scene scene, int numFixations, boolean clearStm, int time, boolean debug) {
    
    if(debug) System.out.println("=== Chrest.scanScene() ===");
    if(debug) System.out.println("- Requested to scan scene with name '" + scene.getName() + "' at time " + time);
    
    // only clear STM if flag is set
    if (clearStm) {
      if(debug) System.out.println("- Clearing STM");
      _visualStm.clear (time);
    }
        
    //Get the location of the creator in the scene.  If the creator of the scene 
    //is present in it then all chunks will have coordinates relative to the 
    //position of the creator.  Therefore, when visual STM is used to create
    //the recalled scene below, coordinates will be relative to the creator and
    //need to be converted into non-relative coordinates so that the scene can
    //be constructed correctly.
    Square selfLocation = scene.getLocationOfCreator();
    if(debug) System.out.println("- Is the Scene creator encoded and, if so, where? " + 
      (selfLocation  != null ? 
        " Yes, location: " + selfLocation.toString() :
        " No"
      ) 
    );
    
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
    if(debug) System.out.println("- Scanning scene");
    _perceiver.setScene (scene);
    _perceiver.start (numFixations);
    for (int i = 0; i < numFixations; i++) {
      if(debug) System.out.println("   - Moving eye " + (i+1) + " of " + numFixations + " times");
      _perceiver.moveEye (time, debug);
    }
    
    // -- get items from image in STM, and optionally template slots
    // TODO: use frequency count in recall
    if(debug) System.out.println("- Processing recognised chunks");
    for (Node node : _visualStm) {
      
      //If the node isn't the visual LTM root node (nothing recognised) then,
      //continue.
      if(this.getVisualLtm() != node && !node.getImage().isEmpty()){
        ListPattern recalledInformation = node.getImage();

        if (_createTemplates) { // check if templates needed
          recalledInformation = recalledInformation.append(node.getFilledSlots ());
        }
        
        if(debug) System.out.println("   - Processing chunk with image: '" + node.getImage().toString() + "'");
      
        //Add all recognised items to the scene to be returned and flag the 
        //corresponding VisualSpatialFieldObjects as being recognised.
        for (int i = 0; i < recalledInformation.size(); i++){
          PrimitivePattern item = recalledInformation.getItem(i);
          
          if (item instanceof ItemSquarePattern) {
            ItemSquarePattern ios = (ItemSquarePattern)item;
            int col = ios.getColumn ();
            int row = ios.getRow ();
            
            //Translate domain-specific coordinates if necessary.
            if(selfLocation != null){
              col += selfLocation.getColumn();
              row += selfLocation.getRow();
            }
            
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
    //it into the recalled scene.  This enables domain-specific coordinates to
    //be returned for items in the recalled scene if its contents are to be 
    //returned.
    if(selfLocation != null){
      SceneObject self = scene.getSquareContents(selfLocation.getColumn(), selfLocation.getRow());
      recalledScene.addItemToSquare(selfLocation.getColumn(), selfLocation.getRow(), self.getIdentifier(), self.getObjectClass());
    }

    return recalledScene;
  }

  /** 
   * Clear the STM and LTM of the model.
   */
  public void clear () {
    this.clearHistory(); 
    this.setClocks(0);
    _visualLtm.clear ();
    _verbalLtm.clear ();
    _actionLtm.clear ();
    _visualLtm = new Node (this, 0, jchrest.lib.Pattern.makeVisualList (new String[]{"Root"}), 0);
    _verbalLtm = new Node (this, 0, jchrest.lib.Pattern.makeVerbalList (new String[]{"Root"}), 0);
    _actionLtm = new Node (this, 0, jchrest.lib.Pattern.makeActionList (new String[]{"Root"}), 0);
    _totalNodes = 0;
    _visualStm.clear (0);
    _verbalStm.clear (0);
    _experimentsLocatedInNames.clear();
    this._engagedInExperiment = false;
    setChanged ();
    if (!_frozen) notifyObservers ();
  }

  /** 
   * Write model to given Writer object in VNA format
   */
  public void writeModelAsVna (Writer writer) throws IOException {
    writer.write ("*Node data\n\"ID\", \"contents\"\n");
    _visualLtm.writeNodeAsVna (writer);
    writer.write ("*Tie data\nFROM TO\n");
    _visualLtm.writeLinksAsVna (writer);
  }

  /** 
   * Write model semantic links to given Writer object in VNA format
   */
  public void writeModelSemanticLinksAsVna (Writer writer) throws IOException {
    writer.write ("*Node data\n\"ID\", \"contents\"\n");
    _visualLtm.writeNodeAsVna (writer);
    writer.write ("*Tie data\nFROM TO\n");
    _visualLtm.writeSemanticLinksAsVna (writer);
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
  public void assignEmotionToCurrentItem (Stm stm, Emotion emotion) {
    if (stm.getCount () == 0) {
      return;  // STM empty, so nothing to be done
    }
    _emotionAssociator.setRWEmotion (stm.getItem(0), emotion);
  }

  /** 
   * Accessor for the emotion associated with the topmost item in STM.
   */
  public Emotion getCurrentEmotion (Stm stm) {
    if (stm.getCount () == 0) {
      return null;
    } else {
      return _emotionAssociator.getRWEmotion (stm.getItem (0));
    }
  }

  public Emotion getCurrentFollowedByEmotion (Stm stm) {
    if (stm.getCount () == 0) {
      return null;
    } else {
      Node followed_by = stm.getItem(0).getAssociatedNode ();
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
  
  /**
   * Advances the value of "_attentionClock" by the time specified.
   * @param timeToAdvanceBy 
   */
  public void advanceAttentionClock(int timeToAdvanceBy){
    this._attentionClock += timeToAdvanceBy;
    this.setChanged();
  }
  
  /**
   * Sets the value of the "_attentionClock" instance variable to the time 
   * passed.
   * 
   * @param time The time to set the "_attentionClock" instance variable value
   * to.  This time is domain-specific.
   */
  public void setAttentionClock(int time){
    this._attentionClock = time;
    setChanged();
  }
  
  /**
   * Accessor to retrieve the value of the model's "_attentionClock" instance 
   * variable value.
   * 
   * @return The value of the model's "_attentionClock" instance variable value.
   */
  public int getAttentionClock () {
    return _attentionClock;
  }
  
  /**
   * Determines if the CHREST model's attention is currently free or not.
   * 
   * @param domainTime  The current time (in milliseconds) in the domain where 
   * this Chrest instance is located. 
   * 
   * @return True if the value passed is greater than the value of the 
   * "_attentionClock" instance variable, false if not.
   */
  public boolean attentionFree(int domainTime){
    return domainTime >= this.getAttentionClock(); 
  }
  
  
  public void advanceLearningClock(int timeToAdvanceBy){
    this._learningClock += timeToAdvanceBy;
    this.setChanged();
  }
    
  public int getLearningClock(){
    return this._learningClock;
  }
  
  public void setLearningClock(int time){
    this._learningClock = time;
    setChanged();
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
  
  /****************************************************************************/
  /****************************************************************************/
  /*********************** VISUAL-SPATIAL FIELD METHODS ***********************/
  /****************************************************************************/
  /****************************************************************************/
  
  /**
   * Creates a new {@link jchrest.architecture.VisualSpatialField} and registers 
   * it as a value in the {@link java.util.TreeMap} database of 
   * {@link jchrest.architecture.VisualSpatialField}s associated with this 
   * CHREST instance against a key that is the time specified.
   * 
   * Description of parameters are provided here: {@link 
   * jchrest.architecture.VisualSpatialField#VisualSpatialField(
   * jchrest.architecture.Chrest, jchrest.lib.Scene, int, int, int, int, int, 
   * int, int, int, boolean, boolean).
   * 
   * @param sceneToTranspose
   * @param objectEncodingTime
   * @param emptySquareEncodingTime
   * @param accessTime
   * @param objectMovementTime
   * @param lifespanForRecognisedObjects
   * @param lifespanForUnrecognisedObjects
   * @param numberFixations
   * @param domainTime
   * @param encodeGhostObjects
   * @param debug
   */
  public void createNewVisualSpatialField(
    Scene sceneToTranspose, 
    int objectEncodingTime, 
    int emptySquareEncodingTime, 
    int accessTime, 
    int objectMovementTime, 
    int lifespanForRecognisedObjects, 
    int lifespanForUnrecognisedObjects, 
    int numberFixations, 
    int domainTime,
    boolean encodeGhostObjects,
    boolean debug
  ){
    try {
      this._visualSpatialFields.put(domainTime, new VisualSpatialField(
        this,
        sceneToTranspose,
        objectEncodingTime,
        emptySquareEncodingTime,
        accessTime,
        objectMovementTime,
        lifespanForRecognisedObjects,
        lifespanForUnrecognisedObjects,
        numberFixations,
        domainTime,
        encodeGhostObjects,
        debug
      ));
    } catch (VisualSpatialFieldException ex) {
      Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
    }
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
