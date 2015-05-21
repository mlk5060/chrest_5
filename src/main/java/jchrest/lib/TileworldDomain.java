package jchrest.lib;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jchrest.architecture.Chrest;

/**
 * Used for Tileworld modelling.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class TileworldDomain extends DomainSpecifics{
  
  public TileworldDomain(Chrest model) {
    super(model);
  }

  @Override
  public ListPattern normalise(ListPattern pattern) {
    ListPattern result = new ListPattern(pattern.getModality());
    
    //Remove self from pattern since the location of self doesn't need to be
    //learned and remove duplicates that may have been added due to random 
    //fixations.
    for(PrimitivePattern prim : pattern){
      ItemSquarePattern item = (ItemSquarePattern)prim;
      if(!item.getItem().equalsIgnoreCase(Scene.getSelfIdentifier()) && !result.contains(prim)){
        result.add(prim);
      }
    }
    
    if(this._associatedModel != null){
      HashMap<String, Object> historyRowToInsert = new HashMap<>();
      
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
      historyRowToInsert.put(Chrest._executionHistoryTableOutputColumnName, result.toString() + "(" + result.getModalityString() + ")");
      this._associatedModel.addEpisodeToExecutionHistory(historyRowToInsert);
    }
    
    return result;
  }

  /**
   * In Tileworld, salient squares are those that aren't blind spots or empty.
   * 
   * @param scene
   * @param model
   * @return 
   */
  @Override
  public Set<Square> proposeSalientSquareFixations(Scene scene, Chrest model) {
    Set<Square> salientSquareFixations = new HashSet<>();
    for(int col = 0; col < scene.getWidth(); col++){
      for(int row = 0; row < scene.getHeight(); row++){
        ListPattern squareContents = scene.getItemsOnSquare(col, row, false, false);
        if( !squareContents.isEmpty() ){
          salientSquareFixations.add(new Square(col, row));
        }
      }
    }
    return salientSquareFixations;
  }

  @Override
  public List<Square> proposeMovementFixations(Scene scene, Square square) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public int getCurrentTime() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}
