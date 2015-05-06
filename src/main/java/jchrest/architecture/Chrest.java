// Copyright (c) 2012, Peter C. R. Lane
// with contributions on the emotions code by Marvin Schiller.
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.architecture;

import com.almworks.sqlite4java.*;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jchrest.lib.*;
import jchrest.lib.ReinforcementLearning.ReinforcementLearningTheories;

/**
 * The parent class for an instance of a Chrest model.
 * 
 * @author Peter C. R. Lane
 */
public class Chrest extends Observable {
  
  /****************************/
  /***** Domain specifics *****/
  /****************************/
  
  //By default, a CHREST model should be located in a generic-domain.  If it 
  //should be located in a different domain, this must be set explicitly.
  private DomainSpecifics _domainSpecifics = new GenericDomain();
  
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
  
  //Since the CHREST GUI is multi-threaded and sqlite4java is single-threaded, 
  //SQLite table operations need to be executed in a thread.  So create a new
  //variable to hold an SQLiteQueue (a thread) object.
  private SQLiteQueue _historyThread = null;
  
  //The model should record its execution history by default (for the purposes 
  //of debugging).
  private boolean _historyRecordingEnabled = true;
  
  //Create a new connection to the history table but don't set it up yet; its
  //status will be used to determine if a new history table should be 
  //instantiated when requested.
  private SQLiteConnection _historyConnection = null;
  
  //Set-up the timeout variables for history table operations.
  private final long _historyTableQueryTimeout = 5;
  private final TimeUnit _historyTableQueryTimeoutUnit = TimeUnit.SECONDS; 
  
  //Set-up history table name so consistency with operations is ensured.
  private final static String _historyTableName = "history";
  public final static String _historyTableRowIdColumnName = "id";
  public final static String _historyTableTimeColumnName = "time";
  public final static String _historyTableOperationColumnName = "operation";
  public final static String _historyTableInputColumnName = "input";
  public final static String _historyTableDescriptionColumnName = "description";
  public final static String _historyTableOutputColumnName = "output";
  
  //Set-up a data structure to hold all history table column indicies, names and
  //types.  The first dimension of the map holds column indicies whilst the 
  //second holds the column name and the SQLiteConstant value for its data type.
  //Indicies are stored here so that column values can be bound correctly when
  //inserting data into the table.
  private final HashMap<Integer, Object[]> _historyTableColumnIndexNameAndType = new HashMap<>();
  
  //Set-up a data structure to hold the values set for the columns in the last 
  //history row inserted.  The keys are column names, the values are the column 
  //values for the last row.
  private final HashMap<String, Object> _lastHistoryRowInserted = new HashMap<>();
  
  /************************************/
  /***** Internal clock variables *****/
  /************************************/
  
  //Indicates the time at which CHREST will be free to perform mind's eye 
  //operations.
  private int _attentionClock;
  
  //Indicates the time at which CHREST will be free to perform LTM/STM 
  //operations.
  private int _learningClock; 
  
  // timing parameters
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
  
  //Mind's Eye database - stores domain times as keys and mind's eye instances
  //as values.  Since a mind's eye can only transpose one Scene instance, 
  //multiple instances may be required throughout the lifespan of one CHREST
  //model.  To enable correct visualisation of the mind's eye at any point in 
  //time, a CHREST model needs to be able to store and retrieve mind's eye 
  //instances.  This variable provides such functionality.
  private final TreeMap<Integer, MindsEye> _mindsEyes = new TreeMap<>();
  
  // Emotions module
  private EmotionAssociator _emotionAssociator;
  
  //Reinforcement learning module
  private ReinforcementLearningTheories _reinforcementLearningTheory;

  //TODO: Pass DomainSpecifics sub-class in constructor to make it explicitly 
  //clear that the domain in which CHREST is located is important.  If the 
  //parameter passed is null, don't alter the CHREST model's _domainSpecifics
  //variable (its set to GenericDomain by default).
  public Chrest () {
    
    /************************************/
    /***** Execution history set-up *****/
    /************************************/
    
    //This line is extremely important: without it, execution history will not 
    //be able to operate.  Essentially, the line tells SQLite where the native
    //files required for database interaction are found.  Conseuqntly, history
    //recording should be OS-agnostic and work without the user setting up any
    //paths.
    SQLite.setLibraryPath("../sqlite4java-392");
    
    //Turn off extensive logging by default on the output stream.
    Logger.getLogger("com.almworks.sqlite4java").setLevel(Level.OFF);
    
    //Set-up the "dictionary" for column indicies, names and types to ensure 
    //consistency with execution history DB table operations.  Note that the
    //first column will be specified as the primary key for the execution 
    //history DB table when the table is created. 
    Object[] idColumnNameAndType = {Chrest._historyTableRowIdColumnName, SQLiteConstants.SQLITE_INTEGER};
    Object[] timeColumnNameAndType = {Chrest._historyTableTimeColumnName, SQLiteConstants.SQLITE_INTEGER};
    Object[] operationColumnNameAndType = {Chrest._historyTableOperationColumnName, SQLiteConstants.SQLITE_TEXT};
    Object[] inputColumnNameAndType = {Chrest._historyTableInputColumnName, SQLiteConstants.SQLITE_TEXT};
    Object[] descriptionColumnNameAndType = {Chrest._historyTableDescriptionColumnName, SQLiteConstants.SQLITE_TEXT};
    Object[] outputColumnNameAndType = {Chrest._historyTableOutputColumnName, SQLiteConstants.SQLITE_TEXT};
    
    this._historyTableColumnIndexNameAndType.put(0, idColumnNameAndType);
    this._historyTableColumnIndexNameAndType.put(1, timeColumnNameAndType);
    this._historyTableColumnIndexNameAndType.put(2, operationColumnNameAndType);
    this._historyTableColumnIndexNameAndType.put(3, inputColumnNameAndType);
    this._historyTableColumnIndexNameAndType.put(4, descriptionColumnNameAndType);
    this._historyTableColumnIndexNameAndType.put(5, outputColumnNameAndType);
    
    //Instantiate the execution history DB table.
    this.instantiateHistoryDatabase();
    
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
    _attentionClock = 0;
    _learningClock = 0;
    _totalNodes = 0;
    _visualLtm = new Node (this, 0, jchrest.lib.Pattern.makeVisualList (new String[]{"Root"}), 0);
    _verbalLtm = new Node (this, 0, jchrest.lib.Pattern.makeVerbalList (new String[]{"Root"}), 0);
    _actionLtm = new Node (this, 0, jchrest.lib.Pattern.makeActionList (new String[]{"Root"}), 0);
    _totalNodes = 0; // Node constructor will have incremented _totalNodes, so reset to 0
    _visualStm = new Stm (4, this._learningClock);
    _verbalStm = new Stm (2, this._learningClock);
    _actionStm = new Stm (4, this._learningClock);
    _emotionAssociator = new EmotionAssociator ();
    _reinforcementLearningTheory = null; //Must be set explicitly using Chrest.setReinforcementLearningTheory()
    _perceiver = new Perceiver (this);
            
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
  
  private String sqlDataTypeConstantToString(int sqliteDataTypeConstant){
    switch(sqliteDataTypeConstant){
      case SQLiteConstants.SQLITE_INTEGER:
        return "INT";
      case SQLiteConstants.SQLITE_FLOAT:
        return "REAL";
      case SQLiteConstants.SQLITE_TEXT:
        return "TEXT";
      case SQLiteConstants.SQLITE_BLOB:
        return "BLOB";
      default:
        return "NULL";
    }
  }
  
  public int getNumberHistoryTableColumns(){
    return this._historyTableColumnIndexNameAndType.size();
  }
  
  /**
   * Main function through which all SQLite queries should be routed.  This 
   * function performs the following operations:
   * 
   * <ol>
   *  <li>
   *    Checks whether this model can record execution history, if so, the 
   *    function will continue.
   *  </li>
   *  <li>
   *    Creates and starts a new thread that executes SQLite operations, if one 
   *    is not already active.
   *  </li>
   *  <li>
   *    Creates and opens a new connection to an in-memory SQLite database if 
   *    one is not already active.
   *  </li>
   *  <li>
   *    Builds and executes the SQLite statement specified, binding parameters, 
   *    if specified.
   *  </li>
   *  <li>
   *    Delete any duplicate rows created using an INSERT statement.
   *  </li>
   *  <li>
   *    Passes the results of the SQLite query to a callback function, if 
   *    specified.
   *  </li>
   * </ol>
   * 
   * @param sqliteString The raw SQLite statement that should be executed.  If 
   * the statement is parameterised, ensure that bindings are also passed.
   * @param bindings The parameters that should be substituted into the 
   * statement, in order i.e. the first element will be substituted for the 
   * first "?" in the statement.
   * @param object The object that contains the callback function to be invoked.
   * @param method The callback function to be invoked after the SQL query 
   * specified has been executed.  The results of the SQL query can be passed to
   * the method specified.
   */
  private void executeSqliteQuery(String sqliteString, ArrayList bindings, Object object, Method method){
    if(this.canRecordHistory()){
      
      //Create and start a new history thread, if one doesn't already exist.  
      //This is impreative to allow for multi-threading which, given that CHREST 
      //uses a GUI, is extremely important since GUI's are full of threads.
      if(this._historyThread == null){
        this._historyThread = new SQLiteQueue();
        this._historyThread.start();
      }
      
      try{
        this._historyThread.execute(new SQLiteJob<SQLiteStatement>() {

          @Override
          protected SQLiteStatement job(SQLiteConnection connection) throws SQLiteException {
            
            /*****************************************/
            /***** Create new history connection *****/
            /*****************************************/
            
            if(Chrest.this._historyConnection == null){
              _historyConnection = new SQLiteConnection(); //No argument: creates new DB in memory not on disk.

              try {
                _historyConnection.open(true);
              } catch (SQLiteException ex) {
                Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
              }
            }
            
            /*******************************/
            /***** Build SQL statement *****/
            /*******************************/
            
            //Initialise the SQL Statement so it can be returned.
            SQLiteStatement sql = null;
            
            try {
              sql = Chrest.this._historyConnection.prepare(sqliteString);

              //Set-up bindings.
              if(bindings != null){
                for(int binding = 0; binding < bindings.size(); binding++){
                  sql.bind( 
                    (binding + 1), //Add 1 to the binding number since bindings are not zero-indexed
                    bindings.get(binding).toString()
                  );  
                }
              }

              /*********************************/
              /***** Execute SQL statement *****/
              /*********************************/
              sql.stepThrough();
              
              /***********************************************/
              /***** Check for and delete duplicate rows *****/
              /***********************************************/
              
              //Due to how threads operate, it can sometimes be the case that 
              //SQL insert statements are executed more than once producing 
              //duplicate rows in the execution history table.  This may be 
              //confusing for users who are using the GUI to debug or trace 
              //CHREST's execution so, dispose of any duplicate inserts.
              
              //SQLite4java has no constants for statement types so a string
              //search is required.
              if(sql.getSqlParts().toString().startsWith("INSERT")){
                SQLiteStatement lastRow = Chrest.this._historyConnection.prepare("SELECT * FROM " + Chrest._historyTableName + " WHERE " + Chrest._historyTableRowIdColumnName + " = (SELECT last_insert_rowid())");
                SQLiteStatement lastRowButOne = Chrest.this._historyConnection.prepare("SELECT * FROM " + Chrest._historyTableName + " WHERE " + Chrest._historyTableRowIdColumnName + " = (SELECT (last_insert_rowid() - 1))");
                
                //This will fail if there has only been one row inserted so far.
                while(lastRow.step() && lastRowButOne.step()){
                  
                  boolean allColumnValuesEqual = true;
                  
                  //Check from column 1 in the last row and the last row but one 
                  //since the primary key column value will always be different
                  //due to the auto increment functionality of the primary key 
                  //column.
                  for(int col = 1; col < lastRow.columnCount(); col++){
                    
                    //Get the column values as strings so that any inequality is
                    //purely due to differing information in the column rather
                    //than data type.
                    String lastRowColString = lastRow.columnString(col);
                    String lastRowButOneColString = lastRowButOne.columnString(col);

                    if(!lastRowColString.equals(lastRowButOneColString)){
                      allColumnValuesEqual = false;
                    }
                  }
                  
                  //Delete the oldest duplicate value.  No reason why, the 
                  //younger could be deleted.
                  if(allColumnValuesEqual){
                    Chrest.this._historyConnection.prepare("DELETE FROM " + Chrest._historyTableName + " WHERE " + Chrest._historyTableRowIdColumnName + " = (SELECT last_insert_rowid() - 1)").stepThrough();
                  }
                }
              }
            } catch (SQLiteException ex) {
              Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            /***************************************/
            /***** Return SQL statement result *****/
            /***************************************/
            return sql;
          }

          @Override
          protected void jobFinished(SQLiteStatement sql){
            
            /************************************/
            /***** Invoke callback function *****/
            /************************************/
            
            try {
              if(object != null && method != null){
                method.invoke(object, sql);
              }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
              Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
            }
            
          }
        }).get(this._historyTableQueryTimeout, this._historyTableQueryTimeoutUnit);
      } catch (InterruptedException | ExecutionException | TimeoutException ex) {
        Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
  
  /**
   * Instantiates a history database for this model if one does not already 
   * exist.
   */
  private void instantiateHistoryDatabase(){
            
    String createTableSqlStatement = "CREATE TABLE " + Chrest._historyTableName + " (";

    //To ensure that columns are added to the createTableSqlStatement 
    //statement in order, use explicit key identifiers in the for loop 
    //below
    for(int col = 0; col < Chrest.this._historyTableColumnIndexNameAndType.size(); col++){
      Object[] colNameAndType = Chrest.this._historyTableColumnIndexNameAndType.get(col);
      createTableSqlStatement += (String)colNameAndType[0] + " ";

      //If this is the first column being created, declare it as a primary key.
      if(col == 0){
        createTableSqlStatement += "INTEGER PRIMARY KEY, ";
      }
      else{
        createTableSqlStatement += Chrest.this.sqlDataTypeConstantToString((Integer)colNameAndType[1]) + ", ";
      }
    }
    createTableSqlStatement = createTableSqlStatement.replaceFirst(", $", ");");
    Chrest.this.executeSqliteQuery(createTableSqlStatement, null, null, null);
  }
  
  /**
   * Adds an episode to the execution history of this model.  This function 
   * performs the following operations:
   * 
   * <ol>
   *  <li>
   *    Sets the "time" column value for this row to the last row's time column 
   *    value if one has not been specified.
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
  public void addToHistory(HashMap<String, Object> columnNamesAndValues) {

    /*********************************/
    /***** Set time column value *****/
    /*********************************/
    if(columnNamesAndValues.get(Chrest._historyTableTimeColumnName) == null){
      Integer time = 0;
      if(!Chrest.this._lastHistoryRowInserted.isEmpty()){
        time = (Integer)Chrest.this._lastHistoryRowInserted.get(Chrest._historyTableTimeColumnName);
      }
      columnNamesAndValues.put(Chrest._historyTableTimeColumnName, time);
    }

    /*************************************/
    /***** Create insert new row SQL *****/
    /*************************************/
    String insertSqlStatementString = "INSERT INTO " + Chrest._historyTableName + " (";

    //Primary key column will be auto-incremented so specify columns
    //to insert into from the column after the primary key column.
    for(int col = 1; col < Chrest.this._historyTableColumnIndexNameAndType.size(); col++){
      Object[] colNameAndTypeIterator = Chrest.this._historyTableColumnIndexNameAndType.get(col);
      insertSqlStatementString += (String)colNameAndTypeIterator[0] + ", ";
    }

    insertSqlStatementString = insertSqlStatementString.replaceFirst(", $", ") VALUES (");
    for(int colIndex = 1; colIndex < Chrest.this._historyTableColumnIndexNameAndType.size(); colIndex++){
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
    for(int i = 0; i < this._historyTableColumnIndexNameAndType.size() - 1; i++){
      bindings.add(null);
    }

    //Set bindings for declared columns in the correct order.
    for(Entry<String, Object> columnNameAndValueToInsert : columnNamesAndValues.entrySet()){
      String nameOfColumnToInsertValueInto = columnNameAndValueToInsert.getKey();

      for(Entry<Integer, Object[]> columnIndexNameAndType : Chrest.this._historyTableColumnIndexNameAndType.entrySet()){
        if(nameOfColumnToInsertValueInto.equals( (String)(columnIndexNameAndType.getValue())[0] )){
          bindings.set(columnIndexNameAndType.getKey() - 1, columnNameAndValueToInsert.getValue());
        }
      }
    }

    //Fill in any missing column values.
    for(int colIndex = 0; colIndex < bindings.size(); colIndex++){
      if(bindings.get(colIndex) == null){
        bindings.set(colIndex, "");
      }
    } 

    /**************************/
    /***** Execute insert *****/
    /**************************/
    Chrest.this.executeSqliteQuery(insertSqlStatementString, bindings, null, null);

    /***************************************************/
    /***** Update last inserted row data structure *****/
    /***************************************************/
    String getLastRowInsertedSql = "SELECT * FROM " + Chrest._historyTableName + " WHERE " + Chrest._historyTableRowIdColumnName + " = (SELECT last_insert_rowid())";

    try {
      Chrest.this.executeSqliteQuery(
        getLastRowInsertedSql,
        null,
        Chrest.this, 
        Chrest.class.getMethod("updateLastHistoryRowInserted", new Class[]{SQLiteStatement.class})
      );
    } catch (NoSuchMethodException | SecurityException ex) {
      Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  /**
   * Populates the _lastHistoryRowInserted instance variable with the data 
   * present in the parameter passed.
   * 
   * @param lastRowInsertedSql The result of the SQL used to insert the most 
   * recent row into the execution history table.
   */
  public void updateLastHistoryRowInserted(SQLiteStatement lastRowInsertedSql){
    
    try{
      while(lastRowInsertedSql.step()){
        
        for(int col = 0; col < lastRowInsertedSql.columnCount(); col++){
          Chrest.this._lastHistoryRowInserted.put(lastRowInsertedSql.getColumnName(col), lastRowInsertedSql.columnValue(col));
        }
      }
    } catch (SQLiteException ex) {
      Logger.getLogger(Chrest.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  /**
   * Enables a user to switch history recording on/off for this model.
   * 
   * @param value True to turn on history recording, false to turn off
   */
  public void setRecordHistory(boolean value){
    this._historyRecordingEnabled = value;
  }
  
  /**
   * Indicates whether this model can currently record its execution history.
   * 
   * @return Boolean true if yes, boolean false if not.
   */
  public boolean canRecordHistory(){
    return this._historyRecordingEnabled;
  }
  
  /**
   * Asynchronous retrieval of history: retrieves entire execution history and 
   * passes the result to the callback function specified.
   * 
   * @param object The object to invoke the specified method on when the history
   * database thread completes retrieval of execution history.
   * @param method The method to invoke in the specified object when the history
   * database thread completes retrieval of execution history.
   */
  public void getHistory(Object object, Method method){
    String sql = "SELECT * FROM " + Chrest._historyTableName;
    this.executeSqliteQuery(sql, null, object, method);
  }
  
  /**
   * Asynchronous retrieval of history: retrieves the model's execution history 
   * from the time specified to the time specified and passes the result to the 
   * callback function specified.
   * 
   * @param from Domain-time to return model's execution history from.
   * @param to Domain-time to return model's execution history to.
   * @param object The object to invoke the specified method on when the history
   * database thread completes retrieval of execution history.
   * @param method The method to invoke in the specified object when the history
   * database thread completes retrieval of execution history.
   */
  public void getHistory(int from, int to, Object object, Method method){
    String sql = "SELECT * FROM " + Chrest._historyTableName + " WHERE time >= ? AND time <= ?";
    ArrayList bindings = new ArrayList();
    bindings.add(from);
    bindings.add(to);
    this.executeSqliteQuery(sql, bindings, object, method);
  }
  
  /**
   * Returns the model's execution history filtered by the operation and time 
   * specified and passes the result to the callback function specified.
   * 
   * @param operation The operation to filter execution history by.
   * @param from Domain-time to return model's execution history from.
   * @param to Domain-time to return model's execution history to.
   * @param object The object to invoke the specified method on when the history
   * database thread completes retrieval of execution history.
   * @param method The method to invoke in the specified object when the history
   * database thread completes retrieval of execution history.
   */
  public void getHistory(String operation, int from, int to, Object object, Method method) {
    String sql = "SELECT * FROM " + Chrest._historyTableName + " WHERE operation = ? AND time >= ? AND time <= ?";
    ArrayList bindings = new ArrayList();
    bindings.add(operation);
    bindings.add(from);
    bindings.add(to);
    this.executeSqliteQuery(sql, bindings, object, method);
  }
  
  /**
   * Clears the model's current execution history.
   */
  public void clearHistory() {
    String sql = "DELETE FROM " + Chrest._historyTableName;
    this.executeSqliteQuery(sql, null, null, null);
    this._historyConnection.dispose();
  }
  
  /**
   * Retrieves the maximum time set for an experiment if one is set or the 
   * current learning clock value of CHREST if not.
   * 
   * @param experiment
   * @return 
   */
  public Integer getMaximumTimeForExperiment(String experiment){
    Integer maxTime = this._experimentNamesAndMaximumTimes.get(experiment);
    
    if(maxTime == null){
      maxTime = this.getLearningClock();
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
   * Returns a list of operations that CHREST can perform as strings.
   * 
   * @return List of operations that can be performed by CHREST. 
   */
  public static List<String> getPossibleOperations(){
    return Arrays.asList(Arrays.toString(Operations.values()).replaceAll("\\[| |\\]", "").split(","));
  }

  /**
   * Retrieve the model's current domain specification.
   */
  public DomainSpecifics getDomainSpecifics () {
    HashMap<String, Object> historyRow = new HashMap<>();
    historyRow.put(Chrest._historyTableOperationColumnName, Operations.GET_DOMAIN_SPECIFICS.name());
    historyRow.put(Chrest._historyTableOutputColumnName, this._domainSpecifics.getClass().getSimpleName());
    this.addToHistory(historyRow);
    
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
   * Accessor to retrieve current learning time of model.
   */
  public int getLearningClock () {
    return _learningClock;
  }
  
  /**
   * Resets this model's learning clock to 0.
   */
  public void resetLearningClock(){
    this._learningClock = 0;
    this.setChanged();
    this.notifyObservers();
  }

  /**
   * Advance the learning clock by given amount.
   */
  public void advanceLearningClock (int time) {
    _learningClock += time;
    setChanged ();
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
    if (_learningClock <= time) { // only try to learn if learning clock is 'behind' the time of the call
      if (Math.random () < _rho) { // depending on _rho, may refuse to learn some random times
        _learningClock = time; // bring clock up to date
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
    return recogniseAndLearn (pattern, _learningClock);
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
      return learnPatternAndLinkToActionPattern(pattern1, pattern2, time);
    }
    else{
      return null;
    }
  }

  public Node associateAndLearn (ListPattern pattern1, ListPattern pattern2) {
    return associateAndLearn (pattern1, pattern2, _learningClock);
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
   * Presents Chrest with a pair of patterns which it should learn and then 
   * associate together using an action link.  The first pattern can be of any 
   * modality whilst the second pattern must have an "action" modality.  The 
   * method assumes that the second pattern has action modality and the time of 
   * presentation is the current learning clock time.
   */
  private void learnPatternAndLinkToActionPattern (ListPattern pattern1, ListPattern actionPattern) {
    learnPatternAndLinkToActionPattern (pattern1, actionPattern, _learningClock);
  }
  
  /**
   * Learns first pattern (which can be of any modality) and a second pattern 
   * (whose modality must be "action") and learns an action link between the 
   * first pattern and the second pattern pattern
   */
  private Node learnPatternAndLinkToActionPattern(ListPattern pattern1, ListPattern actionPattern, int time) {
    Node pat1Retrieved = recognise (pattern1, time);
    Boolean actionPatternMatched = false;
    
    // 1. is retrieved node image a match for pattern1?
    if (pat1Retrieved.getImage().matches (pattern1)) {
      
      // 2. does retrieved node have any action links?  If so, check each one to
      // see if it matches actionPattern.
      if (pat1Retrieved.getActionLinks() != null) {
        HashMap<Node, Double> pattern1ActionLinks = pat1Retrieved.getActionLinks();
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

          if (_learningClock <= time) {
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
          
          if (_learningClock <= time) {
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
    learnAndLinkPatterns (pattern1, pattern2, _learningClock);
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
      firstNode.addActionLink(secondNode, time);
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
    if (_learningClock <= time) {
      if (pattern1.isVisual () && pattern2.isVerbal () && _visualStm.getCount () > 0 && _verbalStm.getCount () > 0) {
        _visualStm.getItem(0).setNamedBy (_verbalStm.getItem (0), time);
        advanceLearningClock (getAddLinkTime ());
      }
      setChanged ();
      if (!_frozen) notifyObservers ();
    }
  }

  public void learnAndNamePatterns (ListPattern pattern1, ListPattern pattern2) {
    learnAndNamePatterns (pattern1, pattern2, _learningClock);
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
        node.addActionLink (_actionStm.getItem (0), time);
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
    scanScene (scene, numFixations, time);
    // create a map of moves to their frequency of occurrence in nodes of STM
    Map<ListPattern, Integer> moveFrequencies = new HashMap<ListPattern, Integer> ();
    for (Node node : _visualStm) {
      for (Node action : node.getActionLinks ().keySet()) {
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
  public Scene scanScene (Scene scene, int numFixations, int time) {  
    return scanScene (scene, numFixations, true, time);
  }
  
  /** 
   * Scan given scene, then return a scene which would be recalled.  The 
   * recognised status and terminus values for all objects that may be present 
   * in the visual-spatial field of the mind's eye associated with this scene 
   * are also updated (if applicable).
   * 
   * @param scene
   * @param numFixations
   * @param clearStm
   * @param time The current domain time (in milliseconds).
   * @return 
   */
  public Scene scanScene (Scene scene, int numFixations, boolean clearStm, int time) {
    
    //Set the mind's eye associated with the scene so that objects in the 
    //related visual-spatial field can have their terminus values updated since
    //the scene is being interacted with.
    MindsEye associatedMindsEye = null;
    for(Map.Entry<Integer, MindsEye> mindsEyeEntry : this._mindsEyes.entrySet()){
      if(mindsEyeEntry.getValue().getSceneTransposed().equals(scene)){
        associatedMindsEye = mindsEyeEntry.getValue();
        break;
      }
    }
    
    //Create a data structure to hold a list of all objects in the scene that
    //have been recognised.  This will be used to set the "recognised" status
    //of objects in the associated mind's eye after the scene has been scanned.
    ArrayList<MindsEyeObject> recognisedObjects = new ArrayList<>();
    
    // only clear STM if flag is set
    if (clearStm) { 
      _visualStm.clear (time);
    }
    
    //Scan the scene.
    _perceiver.setScene (scene);
    _perceiver.start (numFixations);
    for (int i = 0; i < numFixations; i++) {
      _perceiver.moveEye (time);
    }
    
    // Build up and return recalled scene
    Scene recalledScene = new Scene (
      "Recalled scene of " + scene.getName (), 
      scene.getWidth (), 
      scene.getHeight ()
    );
    
    //Get the location of the creator in the scene.  If the creator of the scene 
    //is present in it then all learned info will have coordinates relative to 
    //the position of the creator.  Therefore, when visual STM is used to create
    //the recalled scene below, coordinates will be relative to the creator and
    //need to be converted into non-relative coordinates so that the scene can
    //be constructed correctly.
    Square self = scene.getLocationOfSelf();
    
    // -- get items from image in STM, and optionally template slots
    // TODO: use frequency count in recall
    for (Node node : _visualStm) {
      ListPattern recalledInformation = node.getImage();
      
      if (_createTemplates) { // check if templates needed
        recalledInformation = recalledInformation.append(node.getFilledSlots ());
      }
      
      //Add all recognised items to the scene to be returned and flag the 
      //corresponding mind's eye objects as being recognised.
      for (PrimitivePattern item : recalledInformation) {
        if (item instanceof ItemSquarePattern) {
          ItemSquarePattern ios = (ItemSquarePattern)item;
          int col = ios.getColumn ();
          int row = ios.getRow ();
          
          //Translate domain-specific coordinates if necessary.
          if(self != null){
           col += self.getColumn();
           row += self.getRow();
          }
          
          //Add the item to the recalled scene.
          recalledScene.addItemToSquare (col, row, ios.getItem ());
          
          //Update the recognised status of the associated mind's eye object, if
          //applicable.
          if(associatedMindsEye != null){
            ArrayList<MindsEyeObject> objects = associatedMindsEye.getObjectsOnVisualSpatialSquare(col, row);
            for(MindsEyeObject object: objects){
              if(object.getIdentifier().equals(ios.getItem())){
                object.setRecognised(time);
                recognisedObjects.add(object);
              }
            }
          }
        }
      }
    }
    
    //Finally, cycle through the objects in the original scene and flag all 
    //unrecognised items as being unrecognised in the mind's eye associated with
    //the scene (if applicable).
    if(associatedMindsEye != null){
      for(int col = 0; col < scene.getWidth(); col++){
        for(int row = 0; row < scene.getHeight(); row++){
          ArrayList<MindsEyeObject> objects = associatedMindsEye.getObjectsOnVisualSpatialSquare(col, row);
          for(MindsEyeObject object: objects){
            if(
              !object.getIdentifier().equals(Scene.getBlindSquareIdentifier()) && //Only update termini of actual objects.
              !recognisedObjects.contains(object) //Do not process recognised objects!
            ){
              object.setUnrecognised(time);
            }
          }
        }
      }
    }

    return recalledScene;
  }

  /** 
   * Clear the STM and LTM of the model.
   */
  public void clear () {
    this.clearHistory(); 
    _attentionClock = 0;
    _learningClock = 0;
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
  public void emoteAndPropagateAcrossModalities (Object stmsobject) {
    Stm[] stms = (Stm[]) stmsobject;
    _emotionAssociator.emoteAndPropagateAcrossModalities (stms, _learningClock);
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
  
  /****************************************************************************/
  /****************************************************************************/
  /**************************** MINDS EYE METHODS *****************************/
  /****************************************************************************/
  /****************************************************************************/
  
  /**
   * Creates a new mind's eye and registers it in the database of mind's eyes 
   * associated with this CHREST instance at the time specified.
   * 
   * Description of parameters are provided here: {@link 
   * jchrest.architecture.MindsEye#MindsEye(jchrest.architecture.Chrest, 
   * jchrest.lib.Scene, int, int, int, int, int, int, int, int)}.
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
   */
  public void createNewMindsEye(Scene sceneToTranspose, int objectEncodingTime, int emptySquareEncodingTime, int accessTime, int objectMovementTime, int lifespanForRecognisedObjects, int lifespanForUnrecognisedObjects, int numberFixations, int domainTime){
    this._mindsEyes.put(domainTime, new MindsEye(
      this,
      sceneToTranspose,
      objectEncodingTime,
      emptySquareEncodingTime,
      accessTime,
      objectMovementTime,
      lifespanForRecognisedObjects,
      lifespanForUnrecognisedObjects,
      numberFixations,
      domainTime
    ));
  }
  
  /**
   * Returns the history of mind's eye instance creation for this model.
   * 
   * @return 
   */
  public TreeMap<Integer,MindsEye> getMindsEyes(){
    return this._mindsEyes;
  }
}
