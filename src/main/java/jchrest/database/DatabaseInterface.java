package jchrest.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An interface to a h2 database.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class DatabaseInterface {
  
  private Connection _databaseConnection = null;
  
  /**
   * Constructor.
   * 
   * @param databaseUrl Pass null for an in-memory database.
   */
  public DatabaseInterface(String databaseUrl){
    
    try {
      Class.forName("org.h2.Driver");
      
      if(databaseUrl == null){
        this._databaseConnection = DriverManager.getConnection("jdbc:h2:mem:");
      } else {
        this._databaseConnection = DriverManager.getConnection(databaseUrl);
      }
      
      //Turn off auto-commits since transactions may need to be rolled-back.
      this._databaseConnection.setAutoCommit(false);
    } catch (SQLException | ClassNotFoundException ex) {
      Logger.getLogger(DatabaseInterface.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  /**
   * Main function through which all database queries should be routed.
   * 
   * @param sqlString The raw SQL statement that should be executed.  If 
   * the statement is parameterised, ensure that bindings are also passed.
   * @param bindings The parameters that should be bound into the statement 
   * in the order specified, i.e. the first item in this ArrayList will be bound
   * as the first parameter in the statement.
   * 
   * @return Null if the SQL query submitted will not retrieve results 
   * otherwise, a three-dimensional data structure.  The first dimension's 
   * length will be <i>n</i> where <i>n</i> = the number of rows returned by 
   * the SQL query submitted. The second dimension's length will be <i>m</i> 
   * where <i>m</i> = the number of columns in the table.  The third dimension's
   * length will be 2: the first element contains the column's name and the 
   * second element contains the column's value for that row.
   */
  public ArrayList<ArrayList<Object[]>> executeSqliteQuery(String sqlString, ArrayList<Object> bindings) throws SQLException{
    PreparedStatement sql = null;
    ResultSet sqlStatementResults = null;
    ArrayList<ArrayList<Object[]>> results = null;
    boolean sqlReturnsResults = true;
     
    try{
      sql = this._databaseConnection.prepareStatement(sqlString);

      /***************************/
      /***** Bind parameters *****/
      /***************************/
      if(bindings != null){
        for(int binding = 0; binding < bindings.size(); binding++){
          sql.setObject(binding + 1, bindings.get(binding));
        }
      }
      
      /**********************************************/
      /***** Check if query modifies table data *****/
      /**********************************************/
      
      //Query doesn't modify table data.
      if(sql.getMetaData() == null){
        sqlReturnsResults = false;
        sql.execute();
      }
      //Query does modify table data.
      else {
        sqlStatementResults = sql.executeQuery();
      }
      
      /*************************/
      /***** Build results *****/
      /*************************/
      if(sqlReturnsResults){
        results = new ArrayList();
        
        while(sqlStatementResults.next()){

          ArrayList<Object[]> rowData = new ArrayList<>();
          ResultSetMetaData sqlStatementResultsMetadata = sqlStatementResults.getMetaData();

          for(int col = 1; col <= sqlStatementResultsMetadata.getColumnCount(); col++){
            Object[] columnNameAndValue = new Object[2];
            columnNameAndValue[0] = sqlStatementResultsMetadata.getColumnName(col);
            columnNameAndValue[1] = sqlStatementResults.getObject(col);
            rowData.add(columnNameAndValue);
          }
          
          results.add(rowData);
        }
      }
      
      /******************************/
      /***** Commit transaction *****/
      /******************************/
      
      //If the transaction modified database information, commit now (if there 
      //was an error with the SQL query we'd never have gotten this far).
      if(!sqlReturnsResults){
        this._databaseConnection.commit();
      }      
    } catch (SQLException ex) {
      this._databaseConnection.rollback();
      Logger.getLogger(DatabaseInterface.class.getName()).log(Level.SEVERE, null, ex);
    } finally {
      if(sql != null){
        //Ensure resources are freed and keep things running as smoothly as 
        //possible.
        sql.close();
      }
    }
    
    return results;
  }
}
