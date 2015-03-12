// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

// TODO: Clarify order of row/column in methods calls/displays.
// TODO: Support handling of "blind-spots": best way would seem to be to use
// null values for blind spots.  Need to calculate widest width though and then
// fill in as necessary (think of "in-cone" visions to visualise issue).

import java.util.Arrays;
import java.util.List;

public class Scene {
  
  //Human-readable identifier for the scene.
  private final String _name;
  
  //The maximimum height and width of the scene.
  private final int _height;
  private final int _width;
  
  //The string used to identify "blind-spots".
  private final String _blindSpot = "null";
  
  //Two-dimensional array whose first-dimension array elements embody columns of
  //the scene and second-dimension array elements embody rows of the scene 
  //(congruent with the "along the corridor, up the stairs" approach to "D grid
  //reading). Rows and columns are zero-indexed and each space in the array can 
  //contain multiple objects separated by commas.
  private String[][] _scene;

  public Scene (String name, int height, int width) {
    _name = name;
    _height = height;
    _width = width;
    _scene = new String[_width][_height];
    
    //Instantiate scene with null squares at first (empty squares must be 
    //encoded explicitly).  This allows for "blind-spots" to be distinguished 
    //from empty squares.
    for (int col = 0; col < _width; col++) {
      for (int row = 0; row < _height; row++) {
        _scene[col][row] = this._blindSpot;
      }
    }
  }
  
  public String getBlindSpotIdentifier(){
    return this._blindSpot;
  }

  public String getName () {
    return _name;
  }

  public int getHeight () {
    return _height;
  }

  public int getWidth () {
    return _width;
  }
  
  /**
   * Returns the scene (including empty squares).
   * 
   * @return A ListPattern instance consisting of String interpretations of 
   * ItemSquarePattern instances representing each square of the scene.
   */
  public ListPattern getScene(){
    ListPattern scene = new ListPattern();
    
    for(int col = 0; col < _width; col++){
      for(int row = 0; row < _height; row++){
        String[] objectsOnSquare = this._scene[col][row].split(",");
        for(String objectOnSquare : objectsOnSquare){
          scene.add( new ItemSquarePattern(objectOnSquare, col, row) );
        }
      }
    }
    
    return scene;
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
   * @param items The items to add to the row in column order.
   */
  public void addItemsToRow (int row, char [] items) {
    for (int i = 0; i < items.length && i < _width; ++i) {
      String squareContents = _scene[i][row];
      String item = items[i] + "";
      
      //If the square is currently considered as "blind", add the item.
      if(squareContents.equals(this._blindSpot)){
        _scene[i][row] = item;
      }
      //Else, if the square is empty and the item to be added isn't also empty, or
      //the square is not empty and the item to be added is empty, add the item.
      else if(
        (squareContents.equals(".") && !item.equals(".")) ||
        (!squareContents.equals(".") && item.equals("."))
      ){
        _scene[i][row] = item;
      }
      //Otherwise, the square isn't empty and neither is the item so append the 
      //item to the current contents.
      else{
        _scene[i][row] = squareContents + "," + item;
      }
    }
  }

  public List<String> getSquareContents (int col, int row) {
    if (row >= 0 && row < _height && col >= 0 && col < _width) {
      return Arrays.asList(_scene[col][row].split(","));
    } else {
      return Arrays.asList(new String[]{});
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
    if(squareContents.equals(this._blindSpot)){
      _scene[col][row] = item;
    }
    //Else, if the square is empty and the item to be added isn't also empty, or
    //the square is not empty and the item to be added is empty, add the item.
    else if(
      (squareContents.equals(".") && !item.equals(".")) ||
      (!squareContents.equals(".") && item.equals("."))
    ){
      _scene[col][row] = item;
    }
    //Otherwise, the square isn't empty and neither is the item so append the 
    //item to the current contents.
    else{
      _scene[col][row] = squareContents + "," + item;
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
      if(_scene[col][row].equals (".") || this.isBlindSpot(col, row) ){
        return true;
      }
      else{
        return false;
      }
    } else {
      return true; // no item off scene (!)
    }
  }
  
  /**
   * Determines whether the coordinates specified are a blind spot in the scene.
   * 
   * @param col
   * @param row
   * @return True if the coordinate is a blind spot, false if not.
   */
  public boolean isBlindSpot(int col, int row){
    if (
      row >= 0 && 
      row < _height && 
      col >= 0 && 
      col < _width 
    ) {
      return _scene[col][row].equals(this._blindSpot);
    } else {
      return true;
    }
  }
  
  /**
   * Retrieve all items within given row +/- size, column +/- size (blind spots
   * and empty squares are not returned).
   * 
   * TODO: Convert this to use a circular field of view.
   */
  public ListPattern getItems (int startCol, int startRow, int size) {
    ListPattern items = new ListPattern ();

    for (int col = startCol - size; col <= startCol + size; ++col) {
      if (col >= 0 && col < _width) {
        for (int row = startRow - size; row <= startRow + size; ++row) {
          if (row >= 0 && row < _height) {
            if ( !this.isSquareEmpty(col, row) && !this.isBlindSpot(col, row)) {
              String[] objectsOnSquare = _scene[col][row].split(",");
              for(String objectOnSquare : objectsOnSquare){
                items.add (new ItemSquarePattern (objectOnSquare, col+1, row+1));
              }
            }
          }
        }
      }
    }

    return items;
  }

  /**
   * Count the number of non-empty squares in the scene.
   */
  public int countItems () {
    int items = 0;
    for (int col = 0; col < _width; col++) {
      for (int row = 0; row < _height; row++) {
        if( !this.isSquareEmpty(col, row) && !this.isBlindSpot(col, row) ){
          String[] objectsOnSquare = this._scene[col][row].split(",");
          for(String object : objectsOnSquare){
            items ++;
          }
        }
      }
    }
    return items;
  }

  /**
   * Determines how many items are present both in this scene and the scene 
   * specified on the same rows and columns.  Note that the order of items on 
   * the square does not matter so, if a square in scene A contains "A,B" and
   * the same square in scene B contains "B,A", the number of overlapping pieces
   * recorded for this square will be 2.
   * 
   * @param scene The scene to compare this scene against.
   * @return 
   */
  public int countOverlappingPieces (Scene scene) {
    int items = 0;
    for (int col = 0; col < _width; col++) {
      for (int row = 0; row < _height; row++) {
        if(!this.isSquareEmpty(col, row) && !this.isBlindSpot(col, row)){
          String[] thisSceneSquareContents = _scene[col][row].split(",");
          
          //Get the corresponsing square's contents from the other scene as a
          //List instance since a List has a "remove" function that allows for
          //easily removal of items.  This is required since multiple items with
          //the same identifier may be present on the squares and we want to 
          //ensure that we don't count the same piece more than once.
          List<String> otherSceneSquareContents = scene.getSquareContents (col, row);
          
          for(String thisSceneSquareObject : thisSceneSquareContents){
            if(otherSceneSquareContents.contains(thisSceneSquareObject)){
              items++;
              otherSceneSquareContents.remove(thisSceneSquareObject);
            }
          }
        }
      }
    }
    return items;
  }         

  /**
   * Compute precision of given scene against this one.
   * Precision is the proportion of pieces in given scene which are correct.
   */
  public float computePrecision (Scene scene) {
    if (scene.countItems() == 0) {
      return 0.0f;
    } else {
      return (float)countOverlappingPieces(scene) / (float)scene.countItems();
    }
  }

  /**
   * Compute recall of given scene against this one.
   * Recall is the proportion of pieces in this scene which have been correctly recalled.
   */
  public float computeRecall (Scene scene) {
    if (this.countItems() == 0) {
      return 0.0f;
    } else {
      return (float)countOverlappingPieces(scene) / (float)this.countItems();
    }
  }
  
  /**
   * Compute errors of omission of given scene against this one.
   * Omission is the number of pieces which are in this scene but not in the given one.
   */
  public int computeErrorsOfOmission (Scene scene) {
    int errors = 0;
    for (int col = 0; col < _width; col++) {
      for (int row = 0; row < _height; row++) {
        if(!this.isSquareEmpty(col, row) && !this.isBlindSpot(col, row)){
          List<String> thisSquareContents = this.getSquareContents(col, row);
          List<String> otherSquareContents = scene.getSquareContents (col, row);
          for(String object : thisSquareContents){
            if(otherSquareContents.contains(object)){
              thisSquareContents.remove(object);
              otherSquareContents.remove(object);
            }
          }
          
          //Add the leftovers in this scene's square to the number of errors.
          errors += thisSquareContents.size();
        }
      }
    }
    return errors;
  }

  /**
   * Compute errors of commission of given scene against this one.
   * Commission is the number of pieces which are in the given scene but not in this one.
   */
  public int computeErrorsOfCommission (Scene scene) {
    int errors = 0;
    for (int col = 0; col < _width; col++) {
      for (int row = 0; row < _height; row++) {
        if(!this.isSquareEmpty(col, row) && !this.isBlindSpot(col, row)){
          List<String> thisSquareContents = this.getSquareContents(col, row);
          List<String> otherSquareContents = scene.getSquareContents (col, row);
          for(String object : thisSquareContents){
            if(otherSquareContents.contains(object)){
              thisSquareContents.remove(object);
              otherSquareContents.remove(object);
            }
          }
          
          //Add the leftovers in the other scene's square to the number of 
          //errors.
          errors += otherSquareContents.size();
        }
      }
    }

    return errors;
  }
}

