// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

// TODO: Clarify order of row/column in methods calls/displays.
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Scene {
  
  //Human-readable identifier for the scene.
  private final String _name;
  
  //The maximimum height and width of the scene.
  private final int _height;
  private final int _width;
  
  //The string used to identify "blind-spots".
  private static final String _blindSquareIdentifier = "null";
  private static final String _emptySquareIdentifier = ".";
  
  //Two-dimensional array whose first-dimension array elements embody columns of
  //the scene and second-dimension array elements embody rows of the scene 
  //(congruent with the "along the corridor, up the stairs" approach to 2D grid
  //reading). Rows and columns are zero-indexed and each space in the array can 
  //contain multiple objects separated by commas.
  private final String[][] _scene;

  public Scene (String name, int width, int height) {
    _name = name;
    _height = height;
    _width = width;
    _scene = new String[_width][_height];
    
    //Instantiate scene with null squares at first (empty squares must be 
    //encoded explicitly).  This allows for "blind-spots" to be distinguished 
    //from empty squares.
    for (int col = 0; col < _width; col++) {
      for (int row = 0; row < _height; row++) {
        _scene[col][row] = Scene._blindSquareIdentifier;
      }
    }
  }
  
  /**
   * Adds the item identifier to the specified square in the scene.  If the 
   * specified square is currently empty, the item specified replaces the 
   * "empty" identifier.  If the specified square is not currently empty, the 
   * item identifier specified is appended to the current contents of the square
   * with a comma prefix i.e. if the specified square contains "A" and "B" is
   * to be added, the contents of the specified square will equal "A,B".
   * 
   * @param row
   * @param col
   * @param item 
   */
  public void addItemToSquare (int col, int row, String item) {
    assert (row >= 0 && row < _height && col >= 0 && col < _width);
    
    String squareContents = _scene[col][row];
    
    //If the square is currently considered as "blind", add the item.
    if(squareContents.equals(Scene._blindSquareIdentifier)){
      _scene[col][row] = item;
    }
    //Else, if the square is empty and the item to be added isn't also empty, or
    //the square is not empty and the item to be added is empty, add the item.
    else if(
      (squareContents.equals(Scene._emptySquareIdentifier) && !item.equals(Scene._emptySquareIdentifier)) ||
      (!squareContents.equals(Scene._emptySquareIdentifier) && item.equals(Scene._emptySquareIdentifier))
    ){
      _scene[col][row] = item;
    }
    //If the square contents is empty and the item is empty, do nothing.
    else if(squareContents.equals(Scene._emptySquareIdentifier) && item.equals(Scene._emptySquareIdentifier)){}
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
   * @return 
   */
  public int computeErrorsOfCommission (Scene sceneToCompareAgainst) {
    return sceneToCompareAgainst.getAllItemsInScene().size() - this.getAllItemsInScene().size();
  }
  
  /**
   * Compute errors of omission of given scene against this one.
   * Omission is the number of pieces which are in this scene but not in the 
   * given one.
   * 
   * @param sceneToCompareAgainst
   * @return 
   */
  public int computeErrorsOfOmission (Scene sceneToCompareAgainst) {
    return this.getAllItemsInScene().size() - sceneToCompareAgainst.getAllItemsInScene().size();
  }
  
  /**
   * Compute precision of this scene against a given scene i.e. the proportion 
   * of pieces in the scene that are correct in their placement when compared 
   * to the other.
   * 
   * @param sceneToCompareAgainst
   * @return 
   */
  public float computePrecision (Scene sceneToCompareAgainst) {
    if(this.getAllItemsInScene().isEmpty() || sceneToCompareAgainst.getAllItemsInScene().isEmpty()){
      return 0.0f;
    }
    else{
      int numberOfCorrectlyPlacedItems = 0;
      for(int row = 0; row < this._height; row++){
        for(int col = 0; col < this._width; col++){
          ListPattern itemsOnSquareInThisScene = this.getItemsOnSquare(col, row);
          ListPattern itemsOnSquareInOtherScene = sceneToCompareAgainst.getItemsOnSquare(col, row);

          for(PrimitivePattern itemOnSquareInOtherScene : itemsOnSquareInOtherScene){
            if(itemsOnSquareInThisScene.contains(itemOnSquareInOtherScene)){
              numberOfCorrectlyPlacedItems++;
            }
          }
        }
      }

      return (float)numberOfCorrectlyPlacedItems / (float)sceneToCompareAgainst.getAllItemsInScene().size();
    }
  }
  
  /**
   * Compute recall of given scene against this one.
   * Recall is the proportion of pieces in this scene which have been correctly 
   * recalled, irrespective of correct placement.
   * 
   * @param sceneToCompareAgainst
   * @return 
   */
  public float computeRecall (Scene sceneToCompareAgainst) {
    int numberOfItemsInThisScene = this.getAllItemsInScene().size();
    int numberOfItemsInOtherScene = sceneToCompareAgainst.getAllItemsInScene().size();
    
    if(numberOfItemsInThisScene == 0 || numberOfItemsInOtherScene == 0){
      return 0.0f;
    }
    else{
      return (float)this.getAllItemsInScene().size() / (float)sceneToCompareAgainst.getAllItemsInScene().size();
    }
  }
  
   /**
   * Returns the items in this scene (no blind or empty spots included).
   * 
   * @return 
   */
  public ListPattern getAllItemsInScene(){
    ListPattern itemsInScene = new ListPattern();
    
    for (int row = 0; row < this._height; row++) {
      for (int col = 0; col < this._width; col++) {
        itemsInScene = itemsInScene.append(this.getItemsOnSquare(col, row));
      }
    }
    
    return itemsInScene;
  }
  
  /**
   * Returns the string used to denote blind squares in the scene.
   * 
   * @return 
   */
  public String getBlindSquareIdentifier(){
    return Scene._blindSquareIdentifier;
  }
  
  /**
   * Returns the string used to denote empty squares in the scene.
   * 
   * @return 
   */
  public String getEmptySquareIdentifier(){
    return Scene._emptySquareIdentifier;
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
   * Retrieve all items within given row +/- size, column +/- size (blind spots
   * and empty squares are not returned).
   * 
   * @param startCol
   * @param startRow
   * @param colScope
   * @param rowScope
   * 
   * @return 
   */
  public ListPattern getItemsInScope (int startCol, int startRow, int colScope, int rowScope) {
    ListPattern items = new ListPattern ();

    for (int row = startRow - rowScope; row <= startRow + rowScope; row++) {
      if (row >= 0 && row < _height) {
        
        for (int col = startCol - colScope; col <= startCol + colScope; col++) {
          if (col >= 0 && col < _width) {
            items = items.append(this.getItemsOnSquare(col, row));
          }
        }
        
      }
    }

    return items;
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
   * Returns the maximum width of the scene.
   * 
   * @return 
   */
  public int getWidth () {
    return _width;
  }
  
  /**
   * Returns the scene (including blind and empty squares) from east -> west 
   * then south -> north.
   * 
   * @return A ListPattern instance consisting of String interpretations of 
   * ItemSquarePattern instances representing each square of the scene.
   */
  public ListPattern getScene(){
    ListPattern scene = new ListPattern();
    
    for(int row = 0; row < _height; row++){
      for(int col = 0; col < _width; col++){
        String[] objectsOnSquare = this._scene[col][row].split(",");
        for(String objectOnSquare : objectsOnSquare){
          scene.add( new ItemSquarePattern(objectOnSquare, col, row) );
        }
      }
    }
    
    return scene;
  }

  /**
   * Returns all "actual" items on a square in the scene.
   * 
   * @param col
   * @param row
   * @return 
   */
  public ListPattern getItemsOnSquare (int col, int row) {
    ListPattern itemsOnSquare = new ListPattern();
    
    if (row >= 0 && row < _height && col >= 0 && col < _width) {
      LinkedList<String> squareContents = new LinkedList<>(Arrays.asList(_scene[col][row].split(",")));
      
      //Remove empty and blind identifiers
      while(squareContents.contains(Scene._emptySquareIdentifier)){
        squareContents.remove(Scene._emptySquareIdentifier);
      }
      while(squareContents.contains(Scene._blindSquareIdentifier)){
        squareContents.remove(Scene._blindSquareIdentifier);
      }
      
      //If there are items then add ItemSquarePattern representations of the 
      //items to the ListPattern to be returned.
      if( !squareContents.isEmpty() ){
        for(String itemIdentifier : squareContents){
          itemsOnSquare.add(new ItemSquarePattern(itemIdentifier, col, row));
        }
      }
    }
    
    return itemsOnSquare;
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
      return _scene[col][row].equals(Scene._blindSquareIdentifier);
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
      return _scene[col][row].equals (Scene._emptySquareIdentifier);
    } else {
      return true; // no item off scene (!)
    }
  } 
}

