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
  private final String _tileToken;
  private final String _holeToken; 
  private final String _opponentToken;
  
  public TileworldDomain(Chrest model) {
    super(model);
    this._tileToken = "T";
    this._holeToken = "H";
    this._opponentToken = "O";
  }
  
  public TileworldDomain(Chrest model, String holeToken, String opponentToken, String tileToken){
    super(model);
    this._holeToken = holeToken;
    this._opponentToken = opponentToken;
    this._tileToken = tileToken;
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
        !item.equalsIgnoreCase(Scene.getCreatorToken()) && 
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
        
        ListPattern squareContents = scene.getSquareContentsAsListPattern(col, row, false, false);
        boolean onlyCreatorOnSquare = false;
        
        if(squareContents.size() > 1){
          for(PrimitivePattern object  : squareContents){
            if(object instanceof ItemSquarePattern){
              ItemSquarePattern obj = (ItemSquarePattern)object;
              if(obj.getItem().equals(Scene.getCreatorToken())){
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

  /**
   * If the {@link jchrest.lib.Square} passed on the {@link jchrest.lib.Scene} 
   * passed contains a tile, opponent or the scene creator, then the 
   * {@link jchrest.lib.Square}s that are 1 square north, east, south and west 
   * of the passed {@link jchrest.lib.Square} will be added to the 
   * {@link java.util.List} of {@link jchrest.lib.Square}s returned.
   * 
   * @param scene
   * @param square
   * @return 
   */
  @Override
  public List<Square> proposeMovementFixations(Scene scene, Square square) {
    ArrayList<Square> movementFixations = new ArrayList<>();
    int col = square.getColumn();
    int row = square.getRow();
    
    ListPattern squareContents = scene.getSquareContentsAsListPattern(col, row, false, true);
    if( !squareContents.isEmpty() ){
      for(PrimitivePattern squareContent : squareContents){
        String item = ((ItemSquarePattern)squareContent).getItem();
        
        if(
          item.equals(this._tileToken) ||
          item.equals(Scene.getCreatorToken()) ||
          item.equals(this._opponentToken)
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
    return this._holeToken;
  }
  
  public String getOpponentIdentifier(){
    return this._opponentToken;
  }
  
  public String getTileIdentifier(){
    return this._tileToken;
  }
}
