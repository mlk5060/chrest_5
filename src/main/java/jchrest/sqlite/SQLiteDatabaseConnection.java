package jchrest.sqlite;

import com.almworks.sqlite4java.SQLite;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteConstants;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteJob;
import com.almworks.sqlite4java.SQLiteQueue;
import com.almworks.sqlite4java.SQLiteStatement;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jchrest.architecture.Chrest;

/**
 *
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class SQLiteDatabaseConnection {
  
  private final SQLiteQueue _databaseJobQueue;
  private int _currentDatabaseJobQueuePriority = 0;
  private final SQLiteConnection _databaseConnection;
  private final long _databaseConnectionQueryTimeout;
  private final TimeUnit _databaseConnectionQueryTimeoutUnit; 
  
  /**
   * Constructor.
   * 
   * @param database Pass null for an in-memory database.
   * @param timeout The length of time to wait for a query to be executed by the
   * connection before control is returned.
   * @param timeoutUnit The unit of time for the timeout parameter, should be a 
   * {@link java.util.concurrent.TimeUnit} constant.
   */
  public SQLiteDatabaseConnection(File database, long timeout, TimeUnit timeoutUnit){
    
    //This line is extremely important: without it, SQLite4java will not be able 
    //to operate.  Essentially, this declares where the native files required 
    //for SQLite4java to operate are found.  The relative path specification
    //ensures that SQLite4java's operation should be OS-agnostic and work 
    //"out-of-the-box" without the user setting up any paths.
    SQLite.setLibraryPath("../sqlite4java-392");
      
    this._databaseJobQueue = new SQLiteQueue();
    this._databaseJobQueue.start();
    
    if(database == null){
      this._databaseConnection = new SQLiteConnection();
    } else {
      this._databaseConnection = new SQLiteConnection(database);
    }
    
    this._databaseConnectionQueryTimeout = timeout;
    this._databaseConnectionQueryTimeoutUnit = timeoutUnit;
  }
  
  /**
   * Allows the 
   * @param level 
   */
  public static void changeLoggingLevel(Level level){
    Logger.getLogger("com.almworks.sqlite4java").setLevel(level);
  }
  
  /**
   * Main function through which all SQLite queries should be routed.  This 
   * function performs the following operations:
   * 
   * <ol>
   *  <li>
   *    Starts this instance's database thread, if it hasn't already been 
   *    started.
   *  </li>
   *  <li>
   *    Opens this instance's database connection, if it is closed.
   *  </li>
   *  <li>
   *    Extracts the table name from the SQL query specified and performs any
   *    pre-execution operations on that table.
   *  </li>
   *  <li>
   *    Builds and executes the SQLite statement specified, binding parameters, 
   *    if specified.
   *  </li>
   *  <li>
   *    Passes the results of the SQLite query to a callback method, if 
   *    specified.
   *  </li>
   * </ol>
   * 
   * @param sqliteString The raw SQLite statement that should be executed.  If 
   * the statement is parameterised, ensure that bindings are also passed.
   * @param columnNamesAndBindings The parameters that should be substituted 
   * into the statement for the columns specified.  Keys should be column names
   * in the table that is to be operated on and values should be the parameters
   * bound for the relevant table column.
   * @param callbackObject The object that data returned by the SQLite query
   * should be sent to.
   * @param callbackMethod The method that data returned by the SQLite query
   * should be sent to (should be a method of the callbackObject specified).
   */
  public void executeSqliteQuery(String sqliteString, ArrayList<Object> bindings, Object callbackObject, Method callbackMethod){
//    System.out.println();
//    
//    System.out.println("SQL statement to execute: '" + sqliteString + "'.");
//    System.out.println("Bindings: " + (bindings == null ? "null" : Arrays.deepToString(bindings.toArray())) );
//    System.out.println("Callback object: " + (callbackObject == null ? "null" : callbackObject.getClass().getSimpleName()) );
//    System.out.println("Callback method: " + (callbackMethod == null ? "null" : callbackMethod.getName()) );
    
    //Start the thread specified if it hasn't been already.  This is
    //impreative to allow for multi-threading which, given that CHREST uses a
    //GUI, is extremely important since GUI's are full of threads.
    
    //System.out.println("Starting DB thread (if required)...");
    this._databaseJobQueue.start();
    //System.out.println(this._databaseJobQueue.toString());
    //System.out.println("Is current thread the DB thread? " + this._databaseJobQueue.isDatabaseThread());

    Thread jobQueueThread = this.getJobQueueThread();
    if(jobQueueThread != null){
      System.out.println("DB thread state: " + jobQueueThread.getState().toString()); 
    }
    
    
    //System.out.println("Attempting to execute next job in queue...");

    try{
      
      this._databaseJobQueue.execute(new SQLiteJob<ArrayList<HashMap<String, Object>>> () {
        
        @Override
        protected void jobStarted(SQLiteConnection connection) throws java.lang.Throwable{
          System.out.println("Job started");
        }
        
        @Override
        protected void jobCancelled() throws java.lang.Throwable{
          System.out.println("Job cancelled");
        }
        
        protected void jobError(java.lang.Throwable error) throws java.lang.Throwable{
          System.out.println("JOB ERROR");
          System.out.println(error.getMessage());
        }

        @Override
        protected ArrayList<HashMap<String, Object>> job(SQLiteConnection connection) throws SQLiteException {
          
          System.out.println("Executing...");

          //Boolean flag that determines if the SQLite statement executed is
          //actually committed since some SQLiteExceptions may occur that 
          //require the transaction within which the SQLite statement is 
          //executed to be rolled back.
          boolean commit = true;
          boolean executeSqlInTransaction = !sqliteString.startsWith("SELECT");

          /************************************/
          /***** Open database connection *****/
          /************************************/

          if(!SQLiteDatabaseConnection.this._databaseConnection.isOpen()){
            try {
              SQLiteDatabaseConnection.this._databaseConnection.open(true);
            } catch (SQLiteException ex) {
              Logger.getLogger(SQLiteDatabaseConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
          }
        
          /***************************/
          /***** Set-up Bindings *****/
          /***************************/
          
          SQLiteStatement sql = SQLiteDatabaseConnection.this._databaseConnection.prepare(sqliteString);

          //Set-up bindings.
          if(bindings != null){
            //System.out.println("# bindings: " + bindings.size());
            for(int binding = 0; binding < bindings.size(); binding++){
              //System.out.println("Getting binding " + binding);
              sql.bind(binding + 1, bindings.get(binding).toString());
            }
          }

          /***************************************************/
          /***** Execute SQL statement and build results *****/
          /***************************************************/
          
          if(executeSqlInTransaction){
            SQLiteDatabaseConnection.this._databaseConnection.prepare("BEGIN;").stepThrough();
          }
          
          ArrayList<HashMap<String,Object>> sqlResults = new ArrayList<>();
          try{
            while(sql.step()){
              HashMap<String,Object> columnNameAndValue = new HashMap();
              for(int col = 0; col < sql.columnCount(); col++){
                columnNameAndValue.put(sql.getColumnName(col), sql.columnValue(col));
              }
              sqlResults.add(columnNameAndValue);
            }
          } catch (SQLiteException e){
            System.out.println("-- ROLLBACK");
            
            if(executeSqlInTransaction){
              SQLiteDatabaseConnection.this._databaseConnection.prepare("ROLLBACK;").stepThrough();
            }
            
            commit = false;
          } finally {
            sql.dispose();
          }
          
          if(executeSqlInTransaction && commit){
            System.out.println("-- COMMIT");
            SQLiteDatabaseConnection.this._databaseConnection.prepare("COMMIT;").stepThrough();
          }
          
          return sqlResults;
        }

        @Override
        /**
         * Invokes the callback method in the callback object specified.
         */
        protected void jobFinished(ArrayList<HashMap<String, Object>> columnNamesAndValues){

          try {
            if(callbackObject != null && callbackMethod != null){
              System.out.println("-- Calling " + callbackObject.getClass().getSimpleName() + "." + callbackMethod.getName());
              callbackMethod.invoke(callbackObject, columnNamesAndValues);
            }
          } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(SQLiteDatabaseConnection.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }).get(SQLiteDatabaseConnection.this._databaseConnectionQueryTimeout, SQLiteDatabaseConnection.this._databaseConnectionQueryTimeoutUnit);
    } catch (InterruptedException | ExecutionException | TimeoutException ex) {
      
      System.out.println("----- Stack trace for thread START -----");
      StackTraceElement[] stackTrace = jobQueueThread.getStackTrace();
      for(int i = (stackTrace.length - 1); i < stackTrace.length; i-- ){
        System.out.println(stackTrace[i].toString());
      }
      System.out.println("----- Stack trace for thread END -----");
      Logger.getLogger(SQLiteDatabaseConnection.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  private Thread getJobQueueThread(){
    Thread jobQueueThread = null;
    
    for(Thread thread: Thread.getAllStackTraces().keySet()){
      if(thread.getName().equals("SQLiteQueue[]")){
        jobQueueThread = thread;
      }
    }
    
    return jobQueueThread;
  }
  
  /**
   * Returns the string used to specify the SQLite data type constant requested.
   * Useful for unambiguous declarations of column data types.
   * 
   * @param sqliteDataTypeConstant See {@link 
   * com.almworks.sqlite4java.SQLiteConstants}.
   * @return 
   */
  public static String sqlDataTypeConstantToString(int sqliteDataTypeConstant){
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
}
