// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

// TODO: Clarify order of row/column in methods calls/displays.
import java.util.Arrays;
import java.util.LinkedList;

/**
 * The Scene class is intended to represent the external environment that a 
 * CHREST model can "see"; either the whole environment if CHREST's sight is 
 * allocentric or a portion of the environment if it is egocentric.
 * 
 * NOTE: All "get" methods pertaining to retrieval of Scene information should
 * be routed through the {@link #getItemsOnSquare(int, int, boolean, boolean, int)}
 * method since this will also update the corresponding 
 * {@link jchrest.lib.MindsEyeObject} instances in the visual-spatial field of 
 * the {@link jchrest.architecture.MindsEye} instance that is associated with
 * this Scene instance.
 * 
 * @author Peter C. R. Lane <p.c.lane@herts.ac.uk>
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class Scene {
  
  //Human-readable identifier for the scene.
  private final String _name;
  
  //The maximimum height and width of the scene.
  private final int _height;
  private final int _width;
  
  //The string used to identify "blind-spots" i.e. squares that can't be seen in
  //a Scene instance.
  private static final String BLIND_SQUARE_IDENTIFIER = "null";
  
  //The string used to identify empty squares
  private static final String EMPTY_SQUARE_IDENTIFIER = ".";
  
  //The string used to identify the creator of the Scene instance.
  private static final String SELF_IDENTIFIER = "SELF";
  
  //Two-dimensional array whose first-dimension array elements embody columns of
  //the scene and second-dimension array elements embody rows of the scene 
  //(congruent with the "along the corridor, up the stairs" approach to 2D grid
  //reading). Rows and columns are zero-indexed and each space in the array can 
  //contain multiple objects separated by commas.
  private final String[][] _scene;

  /**
   * Constructor: the instance created is initially "blind", empty squares and
   * items must be added using the appropriate methods from this class.
   * 
   * @param name Human-readable identifier for the Scene instance created.
   * @param width Represents the maximum number of indivisible x-coordinates 
   * that can be "seen" in an external vision.
   * @param height Represents the maximum number of indivisible y-coordinates 
   * that can be "seen" in an external vision.
   */
  public Scene (String name, int width, int height) {
    this._name = name;
    this._height = height;
    this._width = width;
    this._scene = new String[_width][_height];
    
    //Instantiate scene with null squares at first (empty squares must be 
    //encoded explicitly).  This allows for "blind-spots" to be distinguished 
    //from empty squares.
    for (int col = 0; col < this._width; col++) {
      for (int row = 0; row < this._height; row++) {
        this._scene[col][row] = Scene.BLIND_SQUARE_IDENTIFIER;
      }
    }
  }
  
  /**
   * Adds the item identifier to the specified square in the scene.  
   * <ul>
   *  <li>
   *    If the square is currently considered as being a blind spot, add the
   *    item.  This may seem nonsensical but if addition is blocked on blind
   *    squares, no Scene would ever contain anything other than blind squares
   *    since Scene instances are completely blind after initial instantiation.
   *  </li>
   *  <li>
   *    If the specified square is currently empty, the item specified replaces 
   *    the "empty" identifier.
   *  </li>
   *  <li>
   *    If the specified square is not currently empty and the item to be added
   *    is not empty, then the item is appended to the current contents of the 
   *    square with a comma prefix i.e. if the specified square contains "A" and 
   *    "B" is to be added, the contents of the specified square will equal 
   *    "A,B".
   *  </li>
   *  <li>
   *    If the specified square is not currently empty and the item to be added
   *    is empty, then the square becomes empty i.e. if the specified square 
   *    contains "A,B" and the square is to become empty, the contents of the 
   *    specified square will contain the empty square identifier only.
   *  </li>
   * </ul>
   * 
   * @param row
   * @param col
   * @param item 
   */
  public void addItemToSquare (int col, int row, String item) {
    assert (row >= 0 && row < _height && col >= 0 && col < _width);
    
    String squareContents = _scene[col][row];
    
    //If the square is currently considered as "blind", add the item.
    if(squareContents.equals(Scene.BLIND_SQUARE_IDENTIFIER)){
      _scene[col][row] = item;
    }
    //Else, if the square is empty and the item to be added isn't empty, or
    //the square is not empty and the item to be added is empty, overwrite the
    //existing square content (square is always "wiped clean" if not empty and
    //empty identifier is added).
    else if(
      (squareContents.equals(Scene.EMPTY_SQUARE_IDENTIFIER) && !item.equals(Scene.EMPTY_SQUARE_IDENTIFIER)) ||
      (!squareContents.equals(Scene.EMPTY_SQUARE_IDENTIFIER) && item.equals(Scene.EMPTY_SQUARE_IDENTIFIER))
    ){
      _scene[col][row] = item;
    }
    //If the square is empty and the item is empty, do nothing.
    else if(squareContents.equals(Scene.EMPTY_SQUARE_IDENTIFIER) && item.equals(Scene.EMPTY_SQUARE_IDENTIFIER)){}
    //Otherwise, the square isn't empty and neither is the item so append the 
    //item to the current contents.
    else{
      _scene[col][row] = squareContents + "," + item;
    }
  }
  
  /**
   * Adds items to columns in the specified row from column 0 incrementally.  
   * For example, if a scene's row is 3 squares wide and the number of items to
   * be added is 5 then the first item in the "items" parameter will be added at
   * column 0 in the row specified, the second item in the "items" parameter 
   * will be added at column 1 in the row specified and so on until all items
   * have been processed.  If the number of items specified is greater than the
   * number of columns in the row, the extra items are ignored.
   * 
   * If a row's square already contains an item, the new item is appended to the
   * square using a comma i.e. if "B" is to be added to a square containing "A"
   * the result would be "A,B".
   * 
   * @param row The row to be modified.
   * @param items The items to add to the row in column order.  To specify that
   * a square should be left as a blind-spot, pass a single whitespace character
   * for that square.
   */
  public void addItemsToRow (int row, char [] items) {
    
    //If the square is not meant to be left as a blind-spot (indicated by 
    //white-space in the char array) then add the item to the square specified
    //accordingly.
    for (int i = 0; i < items.length; ++i) {
      String item = items[i] + "";
      if(!item.equals(" ")){
        this.addItemToSquare(i, row, item);
      }
    }
  }
  
  /**
   * Compute errors of commission of given scene against this one.
   * Commission is the number of pieces which are in the given scene but not in 
   * this one.
   * 
   * @param sceneToCompareAgainst
   * @param selfRelativeCoordinates Set to true to return square coordinates 
   * relative to the creator of the scene if the creator is identified in the 
   * scene itself.
   * 
   * @return 
   */
  public int computeErrorsOfCommission (Scene sceneToCompareAgainst, boolean selfRelativeCoordinates) {
    return sceneToCompareAgainst.getItemsInScene(selfRelativeCoordinates).size() - this.getItemsInScene(selfRelativeCoordinates).size();
  }
  
  /**
   * Compute errors of omission of given scene against this one.
   * Omission is the number of pieces which are in this scene but not in the 
   * given one.
   * 
   * @param sceneToCompareAgainst
   * @param selfRelativeCoordinates Set to true to return square coordinates 
   * relative to the creator of the scene if the creator is identified in the 
   * scene itself.
   * 
   * @return 
   */
  public int computeErrorsOfOmission (Scene sceneToCompareAgainst, boolean selfRelativeCoordinates) {
    return this.getItemsInScene(selfRelativeCoordinates).size() - sceneToCompareAgainst.getItemsInScene(selfRelativeCoordinates).size();
  }
  
  /**
   * Compute precision of this scene against a given scene i.e. the proportion 
   * of pieces in the scene that are correct in their placement when compared 
   * to the other.
   * 
   * @param sceneToCompareAgainst
   * @param selfRelativeCoordinates Set to true to return square coordinates 
   * relative to the creator of the scene if the creator is identified in the 
   * scene itself.
   * 
   * @return 
   */
  public float computePrecision (Scene sceneToCompareAgainst, boolean selfRelativeCoordinates) {
    if(
      this.getItemsInScene(selfRelativeCoordinates).isEmpty() || 
      sceneToCompareAgainst.getItemsInScene(selfRelativeCoordinates).isEmpty()
    ){
      return 0.0f;
    }
    else{
      int numberOfCorrectlyPlacedItems = 0;
      for(int row = 0; row < this._height; row++){
        for(int col = 0; col < this._width; col++){
          ListPattern itemsOnSquareInThisScene = this.getItemsOnSquare(col, row, selfRelativeCoordinates, false);
          ListPattern itemsOnSquareInOtherScene = sceneToCompareAgainst.getItemsOnSquare(col, row, selfRelativeCoordinates, false);

          for(PrimitivePattern itemOnSquareInOtherScene : itemsOnSquareInOtherScene){
            if(itemsOnSquareInThisScene.contains(itemOnSquareInOtherScene)){
              numberOfCorrectlyPlacedItems++;
            }
          }
        }
      }

      return (float)numberOfCorrectlyPlacedItems / (float)sceneToCompareAgainst.getItemsInScene(selfRelativeCoordinates).size();
    }
  }
  
  /**
   * Compute recall of given scene against this one.
   * Recall is the proportion of pieces in this scene which have been correctly 
   * recalled, irrespective of correct placement.
   * 
   * @param sceneToCompareAgainst
   * @param selfRelativeCoordinates Set to true to return square coordinates 
   * relative to the creator of the scene if the creator is identified in the 
   * scene itself.
   * 
   * @return 
   */
  public float computeRecall (Scene sceneToCompareAgainst, boolean selfRelativeCoordinates) {
    float numberOfItemsInThisScene = (float)this.getItemsInScene(selfRelativeCoordinates).size();
    float numberOfItemsInOtherScene = (float)sceneToCompareAgainst.getItemsInScene(selfRelativeCoordinates).size();
    
    if(numberOfItemsInThisScene == 0 || numberOfItemsInOtherScene == 0){
      return 0.0f;
    }
    else{
      return numberOfItemsInThisScene / numberOfItemsInOtherScene;
    }
  }
  
  /**
   * Returns the string used to denote blind squares in the scene.
   * 
   * @return 
   */
  public static String getBlindSquareIdentifier(){
    return Scene.BLIND_SQUARE_IDENTIFIER;
  }
  
  /**
   * Returns the string used to denote empty squares in the scene.
   * 
   * @return 
   */
  public static String getEmptySquareIdentifier(){
    return Scene.EMPTY_SQUARE_IDENTIFIER;
  }
  
  /**
   * Returns the scene (including blind and empty squares) from east -> west 
   * then south -> north.  If the creator of the Scene is present in the Scene
   * coordinates will be relative to the agent (unless overridden by the 
   * "noRelativeCoordinates" parameter) otherwise, coordinates will be 
   * Scene-specific.
   * 
   * @param noRelativeCoordinates Set to true to force the function to return 
   * Scene specific coordinates even when the Scene's creator is identified in 
   * the Scene.
   * 
   * @return A ListPattern instance consisting of String interpretations of 
   * ItemSquarePattern instances representing each square of the scene.
   */
  public ListPattern getEntireScene(boolean noRelativeCoordinates){
    ListPattern scene = new ListPattern();
    
    //Get the location of the agent that constructed this scene in the 
    //scene.  This will be used to determine if the coordinates for the
    //items are to be absolute (Scene-specific) or relative to the creator.
    Square locationOfSelf = this.getLocationOfSelf();
    
    for(int row = 0; row < _height; row++){
      for(int col = 0; col < _width; col++){
        
        ListPattern itemsOnSquare = this.getItemsOnSquare(col, row, noRelativeCoordinates, true);
        
        if(locationOfSelf == null || noRelativeCoordinates){
          for(PrimitivePattern item : itemsOnSquare){
            if(item instanceof ItemSquarePattern){
              ItemSquarePattern itemOnSquare = (ItemSquarePattern)item;
              scene.add( new ItemSquarePattern(itemOnSquare.getItem(), col, row) );
            }
          }
        }
        else {
          for(PrimitivePattern item : itemsOnSquare){
            if(item instanceof ItemSquarePattern){
              ItemSquarePattern itemOnSquare = (ItemSquarePattern)item;
              scene.add(new ItemSquarePattern(itemOnSquare.getItem(), (col - locationOfSelf.getColumn()), (row - locationOfSelf.getRow())));
            }
          }
        }
      }
    }
    
    return scene;
  }
  
  /**
   * Returns the maximum height of the scene.
   * 
   * @return 
   */
  public int getHeight () {
    return _height;
  }
  
  /**
   * Returns the items in this scene (excluding blind and empty squares) from 
   * east -> west then south -> north.
   * 
   * @param selfRelativeCoordinates Set to true to return square coordinates 
   * relative to the creator of the scene if the creator is identified in the 
   * scene itself.
   * 
   * @return 
   */
  public ListPattern getItemsInScene(boolean selfRelativeCoordinates){
    ListPattern itemsInScene = new ListPattern();
    
    for (int row = 0; row < this._height; row++) {
      for (int col = 0; col < this._width; col++) {
        itemsInScene = itemsInScene.append(this.getItemsOnSquare(col, row, selfRelativeCoordinates, false));
      }
    }
    
    return itemsInScene;
  }
  
  /**
   * Retrieve all items within given row +/- size, column +/- size (blind 
   * and empty squares are not returned).
   * 
   * @param startCol
   * @param startRow
   * @param colScope
   * @param rowScope
   * @param selfRelativeCoordinates Set to true to return square coordinates 
   * relative to the creator of the scene if the creator is identified in the 
   * scene itself.
   * 
   * @return 
   */
  public ListPattern getItemsInScope (int startCol, int startRow, int colScope, int rowScope, boolean selfRelativeCoordinates) {
    ListPattern items = new ListPattern ();

    for (int row = startRow - rowScope; row <= startRow + rowScope; row++) {
      if (row >= 0 && row < _height) {
        for (int col = startCol - colScope; col <= startCol + colScope; col++) {
          if (col >= 0 && col < _width) {
            items = items.append(this.getItemsOnSquare(col, row, selfRelativeCoordinates, false));
          }
        }
      }
    }
    return items;
  }
  
  /**
   * Returns all items on a square in the scene.  If the creator of the scene 
   * has identified itself in the scene itself then the coordinates for items 
   * returned will be relative to the agent's location in the scene.
   * 
   * @param col
   * @param row
   * @param selfRelativeCoordinates Set to true to return square coordinates 
   * relative to the creator of the scene if the creator is identified in the 
   * scene itself.
   * @param includeBlindAndEmptySquares Set to true to return blind and empty
   * square identifiers.
   * 
   * @return 
   */
  public ListPattern getItemsOnSquare (int col, int row, boolean selfRelativeCoordinates, boolean includeBlindAndEmptySquares) {
    ListPattern itemsOnSquare = new ListPattern();
    
    if (row >= 0 && row < _height && col >= 0 && col < _width) {
      
      LinkedList<String> squareContents = new LinkedList<>(Arrays.asList(_scene[col][row].split(",")));
      
      //Remove empty and blind identifiers
      if(!includeBlindAndEmptySquares){
        while(squareContents.contains(Scene.EMPTY_SQUARE_IDENTIFIER)){
          squareContents.remove(Scene.EMPTY_SQUARE_IDENTIFIER);
        }
        while(squareContents.contains(Scene.BLIND_SQUARE_IDENTIFIER)){
          squareContents.remove(Scene.BLIND_SQUARE_IDENTIFIER);
        }
      }
      
      //If there are items then add ItemSquarePattern representations of the 
      //items to the ListPattern to be returned.
      if( !squareContents.isEmpty() ){
        
        //Get the location of the agent that constructed this scene in the 
        //scene.  This will be used to determine if the coordinates for the
        //items are to be absolute (Scene-specific) or relative to the creator..
        Square locationOfSelf = this.getLocationOfSelf();
      
        //Process each item on the square accordingly.
        for(String itemIdentifier : squareContents){
          if(selfRelativeCoordinates && locationOfSelf != null){
            itemsOnSquare.add(new ItemSquarePattern(itemIdentifier, (col - locationOfSelf.getColumn()), (row - locationOfSelf.getRow())));
          }
          else{
            itemsOnSquare.add(new ItemSquarePattern(itemIdentifier, col, row));
          }
        }
      }
    }
    
    return itemsOnSquare;
  }
  
  /**
   * Returns the location of the entity that constructed this Scene instance (if
   * it identified itself) in the Scene itself.
   * 
   * @return An instance of {@link jchrest.lib.Square} with the Scene 
   * coordinates that the entity which created this Scene is located at or null
   * if the creator is not present in the Scene.
   */
  public Square getLocationOfSelf(){
    for(int row = 0; row < this._height; row++){
      for(int col = 0; col < this._width; col++){
        String[] squareContents = this._scene[col][row].split(",");
        for(int i = 0; i < squareContents.length; i++){
          if(squareContents[i].equals(Scene.SELF_IDENTIFIER)){
            return new Square(col, row);
          }
        }
      }
    }
    
    return null;
  }

  /**
   * Returns the name of the scene.
   * 
   * @return 
   */
  public String getName () {
    return _name;
  }
  
   /**
   * Returns the string used to denote the creator of a scene in the scene.
   * @return 
   */
  public static String getSelfIdentifier(){
    return Scene.SELF_IDENTIFIER;
  }

  /**
   * Returns the maximum width of the scene.
   * 
   * @return 
   */
  public int getWidth () {
    return _width;
  }
  
  /**
   * Determines whether the coordinates specified are a blind spot in the scene.
   * 
   * @param col
   * @param row
   * @return True if the coordinate is a blind spot, false if not.
   */
  public boolean isSquareBlind(int col, int row){
    if (
      row >= 0 && 
      row < _height && 
      col >= 0 && 
      col < _width 
    ) {
      return _scene[col][row].equals(Scene.BLIND_SQUARE_IDENTIFIER);
    } else {
      return true;
    }
  }

  /**
   * Determines whether the coordinate specified in the scene contains an item
   * or not.  
   * 
   * @param row
   * @param col
   * @return False if there is an item on the coordinate specified in this
   * scene, true if not or the item is a blind-spot.  If the row or col 
   * specified is less than 0 or greater than/equal to the max
   * height/width of this scene then the coordinate specified is considered to
   * be a blind spot.  Consequently, true is returned.
   */
  public boolean isSquareEmpty (int col, int row) {
    if (
      row >= 0 && 
      row < _height && 
      col >= 0 && 
      col < _width 
    ) {
      return _scene[col][row].equals (Scene.EMPTY_SQUARE_IDENTIFIER);
    } else {
      return true; // no item off scene (!)
    }
  }
}

