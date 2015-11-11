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
  private static final String TILE_TOKEN = "T";
  private static final String HOLE_TOKEN = "H"; 
  private static final String OPPONENT_TOKEN = "O";
  
  public TileworldDomain(Chrest model) {
    super(model);
  }

  /** 
   * @param pattern
   * @return A {@link jchrest.lib.ListPattern} stripped of {@link 
   * jchrest.lib.ItemSquarePattern}s that:
   * 
   * <ol type="1">
   *  <li>
   *    Represent the CHREST model or the agent equipped with the CHREST model.
   *  </li>
   *  <li> 
   *    Blind, empty and unknown {@link jchrest.lib.ItemSquarePattern}s.
   *  </li>
   *  <li> 
   *    Are duplicated in the {@link jchrest.lib.ListPattern} passed.
   *  </li>
   * </ol>
   */
  @Override
  public ListPattern normalise(ListPattern pattern) {
    ListPattern result = new ListPattern(pattern.getModality());
    
    for(PrimitivePattern prim : pattern){
      ItemSquarePattern itemDetails = (ItemSquarePattern)prim;
      String item = itemDetails.getItem();
      if(
        !item.equals(Scene.getBlindSquareToken()) &&
        !item.equals(Scene.getEmptySquareToken()) &&
        !item.equalsIgnoreCase(Scene.getCreatorToken()) && 
        !item.equalsIgnoreCase(VisualSpatialFieldObject.getUnknownSquareToken()) &&
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
   * @param scene
   * @param model
   * @return The location of the creator of the {@link jchrest.lib.Scene} 
   * specified or, if the creator is not present, a random {@link 
   * jchrest.lib.Square} that isn't blind, empty or unknown.
   */
  @Override
  public Set<Square> proposeSalientSquareFixations(Scene scene, Chrest model) {
    Set<Square> result = new HashSet<> ();
    
    int randomCol = new java.util.Random().nextInt(scene.getWidth ());
    int randomRow = new java.util.Random().nextInt(scene.getHeight ());

    String objectOnSquare = scene.getSquareContents(randomCol, randomRow).getObjectClass();
    while( objectOnSquare.equals(Scene.getBlindSquareToken()) ){
      randomCol = new java.util.Random().nextInt(scene.getWidth ());
      randomRow = new java.util.Random().nextInt(scene.getHeight ());
      objectOnSquare = scene.getSquareContents(randomCol, randomRow).getObjectClass();
    }

    result.add (new Square(randomCol, randomRow));
    return result;
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
    
    String objectOnSquare = scene.getSquareContents(col, row).getObjectClass();
    if(
      objectOnSquare.equals(TILE_TOKEN) ||
      objectOnSquare.equals(Scene.getCreatorToken()) ||
      objectOnSquare.equals(OPPONENT_TOKEN)
    ){
      movementFixations.add(new Square(col, row + 1));//North
      movementFixations.add(new Square(col + 1, row));//East
      movementFixations.add(new Square(col, row - 1));//South
      movementFixations.add(new Square(col - 1, row));//West
    }
    
    return movementFixations;
  }

  @Override
  public int getCurrentTime() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  
  public static String getHoleIdentifier(){
    return HOLE_TOKEN;
  }
  
  public static String getOpponentIdentifier(){
    return OPPONENT_TOKEN;
  }
  
  public static String getTileIdentifier(){
    return TILE_TOKEN;
  }
}
