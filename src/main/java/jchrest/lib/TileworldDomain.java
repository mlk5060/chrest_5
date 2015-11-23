package jchrest.lib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
  private final int _verticalFieldOfView;
  private final int _horizontalFieldOfView;
  
  /**
   * 
   * @param model
   * @param verticalFieldOfView How many squares ahead can be seen.
   * @param horizontalFieldOfView How many squares to the side can be seen.
   */
  public TileworldDomain(Chrest model, Integer verticalFieldOfView, Integer horizontalFieldOfView) {
    super(model);
    this._verticalFieldOfView = verticalFieldOfView;
    this._horizontalFieldOfView = horizontalFieldOfView;
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
    
    SceneObject objectOnSquare = scene.getSquareContents(col, row);
    
    if(objectOnSquare != null){
      String objectOnSquareClass = objectOnSquare.getObjectClass();
      if(
        objectOnSquareClass.equals(TILE_TOKEN) ||
        objectOnSquareClass.equals(Scene.getCreatorToken()) ||
        objectOnSquareClass.equals(OPPONENT_TOKEN)
      ){
        if ((row + 1 >= 0) && (row + 1 < scene.getHeight())) movementFixations.add(new Square(col, row + 1)); //North
        if ((col + 1 >= 0) && (col + 1 < scene.getWidth())) movementFixations.add(new Square(col + 1, row));//East
        if ((row - 1 >= 0) && (row - 1 < scene.getHeight())) movementFixations.add(new Square(col, row - 1));//South
        if ((col - 1 >= 0) && (col - 1 < scene.getWidth())) movementFixations.add(new Square(col - 1, row));//West
      }
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

  /**
   * Converts relative coordinates in {@link jchrest.lib.ItemSquarePattern}s 
   * contained in a {@link jchrest.lib.ListPattern} to zero-indexed coordinates
   * so the information in the {@link jchrest.lib.ListPattern}'s {@link 
   * jchrest.lib.ItemSquarePattern}s can be mapped onto a 
   * {@link jchrest.lib.Scene}.  For example, if the 
   * coordinates to translate are (-2, -2) and the horizontal/vertical field of
   * view specified is 2, the returned coordinates would be (0, 0).
   * 
   * @param listPattern
   * @param scene Not used so can be set to null
   * @return 
   */
  @Override
  public ListPattern convertDomainSpecificCoordinatesToSceneSpecificCoordinates(ListPattern listPattern, Scene scene) {
    ListPattern preparedListPattern = new ListPattern(Modality.VISUAL);
    Iterator<PrimitivePattern> listPatternIterator = listPattern.iterator();
    
    while(listPatternIterator.hasNext()){
      ItemSquarePattern isp = (ItemSquarePattern)listPatternIterator.next();
      preparedListPattern.add(
        new ItemSquarePattern(
          isp.getItem(),
          isp.getColumn() + this._horizontalFieldOfView, 
          isp.getRow() + this._verticalFieldOfView
        )
      );
    }
    
    return preparedListPattern;
  }

  /**
   * Converts zero-indexed coordinates in {@link jchrest.lib.ItemSquarePattern}s 
   * contained in a {@link jchrest.lib.ListPattern} to coordinates relative to
   * the sight parameters specified for this class.  For example, if the 
   * coordinates to translate are (0, 0) and the horizontal/vertical field of
   * view specified is 2, the returned coordinates would be (-2, -2).
   * 
   * @param listPattern
   * @param scene Not used, can be set to null
   * @return 
   */
  @Override
  public ListPattern convertSceneSpecificCoordinatesToDomainSpecificCoordinates(ListPattern listPattern, Scene scene) {
    ListPattern preparedListPattern = new ListPattern(Modality.VISUAL);
    Iterator<PrimitivePattern> listPatternIterator = listPattern.iterator();
    
    while(listPatternIterator.hasNext()){
      ItemSquarePattern isp = (ItemSquarePattern)listPatternIterator.next();
      preparedListPattern.add(
        new ItemSquarePattern(
          isp.getItem(),
          isp.getColumn() - this._horizontalFieldOfView, 
          isp.getRow() - this._verticalFieldOfView
        )
      );
    }
    
    return preparedListPattern;
  }
}
