package jchrest.lib;

import java.util.ArrayList;
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
  
  //These variables should not be changed during run-time since problems with 
  //"Scene" instances will ensue.
  private final String _tileIdentifier;
  private final String _holeIdentifier; 
  private final String _opponentIdentifier;
  
  public TileworldDomain(Chrest model) {
    super(model);
    this._tileIdentifier = "T";
    this._holeIdentifier = "H";
    this._opponentIdentifier = "O";
  }
  
  public TileworldDomain(Chrest model, String holeIdentifier, String opponentIdentifier, String tileIdentifier){
    super(model);
    this._holeIdentifier = holeIdentifier;
    this._opponentIdentifier = opponentIdentifier;
    this._tileIdentifier = tileIdentifier;
  }

  /**
   * Removes blind, empty and self objects from the {@link 
   * jchrest.lib.ListPattern} passed.
   * 
   * @param pattern
   * @return
   */
  @Override
  public ListPattern normalise(ListPattern pattern) {
    ListPattern result = new ListPattern(pattern.getModality());
    
    for(PrimitivePattern prim : pattern){
      ItemSquarePattern itemDetails = (ItemSquarePattern)prim;
      String item = itemDetails.getItem();
      if(
        !item.equals(Scene.getBlindSquareIdentifier()) &&
        !item.equals(Scene.getEmptySquareIdentifier()) &&
        !item.equalsIgnoreCase(Scene.getSelfIdentifier()) && 
        !result.contains(prim)
      ){
        result.add(itemDetails);
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
   * In Tileworld, salient squares are those that aren't occupied solely by the 
   * scene creator, aren't blind spots and aren't empty.
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
        
        ListPattern squareContents = scene.getItemsOnSquareAsListPattern(col, row, false, false);
        boolean onlyCreatorOnSquare = false;
        
        if(squareContents.size() > 1){
          for(PrimitivePattern object  : squareContents){
            if(object instanceof ItemSquarePattern){
              ItemSquarePattern obj = (ItemSquarePattern)object;
              if(obj.getItem().equals(Scene.getSelfIdentifier())){
                onlyCreatorOnSquare = true;
              }
            }
          }
        }
        
        if( !squareContents.isEmpty() && !onlyCreatorOnSquare ){
          salientSquareFixations.add(new Square(col, row));
        }
      }
    }
    return salientSquareFixations;
  }

  @Override
  public List<Square> proposeMovementFixations(Scene scene, Square square) {
    ArrayList<Square> movementFixations = new ArrayList<>();
    int col = square.getColumn();
    int row = square.getRow();
    
    ListPattern squareContents = scene.getItemsOnSquareAsListPattern(col, row, false, false);
    if( !squareContents.isEmpty() ){
      for(PrimitivePattern squareContent : squareContents){
        String item = ((ItemSquarePattern)squareContent).getItem();
        
        if(
          item.equals(this._tileIdentifier) ||
          item.equals(Scene.getSelfIdentifier()) ||
          item.equals(this._opponentIdentifier)
        ){
          int squareCol = square.getColumn();
          int squareRow = square.getRow();
          movementFixations.add(new Square(squareCol, squareRow + 1));//North
          movementFixations.add(new Square(squareCol + 1, squareRow));//East
          movementFixations.add(new Square(squareCol, squareRow - 1));//South
          movementFixations.add(new Square(squareCol - 1, squareRow));//West
        }
      }
    }
    
    return movementFixations;
  }

  @Override
  public int getCurrentTime() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  
  public String getHoleIdentifier(){
    return this._holeIdentifier;
  }
  
  public String getOpponentIdentifier(){
    return this._opponentIdentifier;
  }
  
  public String getTileIdentifier(){
    return this._tileIdentifier;
  }
}
