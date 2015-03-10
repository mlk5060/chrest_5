// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

// TODO: Clarify order of row/column in methods calls/displays.

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Scene {
  
  //Human-readable identifier for the scene.
  private final String _name;
  
  //The maximimum height and width of the scene.
  private final int _height;
  private final int _width;
  
  //Two-dimensional array whose first-dimension array elements embody rows of
  //the scene and second-dimension array elements embody columns of the scene.
  //Rows and columns are zero-indexed.  Each square can contain multiple objects
  //separated by commas.
  private String[][] _scene;

  public Scene (String name, int height, int width) {
    _name = name;
    _height = height;
    _width = width;
    _scene = new String[_height][_width];
    
    //Instantiate scene with empty items at first.
    for (int row = 0; row < _height; row++) {
      for (int col = 0; col < _width; col++) {
        _scene[row][col] = ".";
      }
    }
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
    
    for(int row = 0; row < _height; row++){
      for(int col = 0; col < _width; col++){
        String[] objectsOnSquare = this._scene[row][col].split(",");
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
      String squareContents = _scene[row][i];
      
      //If the square is empty, replace it with the item specified.
      if(squareContents.equals(".")){
        _scene[row][i] = items[i] + "";
      }
      //The square isn't empty, append the item to the current contents.
      else{
        _scene[row][i] = squareContents + "," + items[i];
      }
    }
  }

  public List<String> getSquareContents (int row, int column) {
    if (row >= 0 && row < _height && column >= 0 && column < _width) {
      return Arrays.asList(_scene[row][column].split(","));
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
   * @param column
   * @param item 
   */
  public void addItemToSquare (int row, int column, String item) {
    assert (row >= 0 && row < _height && column >= 0 && column < _width);
    
    if(_scene[row][column].equals(".")){
      _scene[row][column] = item;
    }
    else{
      _scene[row][column] = _scene[row][column] + "," + item;
    }
  }

  /**
   * Determines whether the coordinate specified in the scene contains an item
   * or not.  
   * 
   * @param row
   * @param column
   * @return False if there is an item on the coordinate specified in this 
   * scene, true if not.  If the row or column specified is less than 0 or 
   * greater than/equal to the max height/width of this scene then the 
   * coordinate specified can't be seen.  Consequently, true is returned.
   */
  public boolean isEmpty (int row, int column) {
    if (
      row >= 0 && 
      row < _height && 
      column >= 0 && 
      column < _width
    ) {
      return _scene[row][column].equals (".");
    } else {
      return true; // no item off scene (!)
    }
  }
  
  /**
   * Retrieve all items within given row +/- size, column +/- size
   * TODO: Convert this to use a circular field of view.
   */
  public ListPattern getItems (int startRow, int startColumn, int size) {
    ListPattern items = new ListPattern ();

    for (int col = startColumn - size; col <= startColumn + size; ++col) {
      if (col >= 0 && col < _width) {
        for (int row = startRow - size; row <= startRow + size; ++row) {
          if (row >= 0 && row < _height) {
            String squareContents = _scene[row][col];
            if (!squareContents.equals(".")) {
              String[] objectsOnSquare = squareContents.split(",");
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
    for (int row = 0; row < _height; row++) {
      for (int col = 0; col < _width; col++) {
        if(!isEmpty (row, col)){
          String[] objectsOnSquare = this._scene[row][col].split(",");
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
    for (int row = 0; row < _height; row++) {
      for (int col = 0; col < _width; col++) {
        if(!this.isEmpty(row, col)){
          String[] thisSceneSquareContents = _scene[row][col].split(",");
          
          //Get the corresponsing square's contents from the other scene as a
          //List instance since a List has a "remove" function that allows for
          //easily removal of items.  This is required since multiple items with
          //the same identifier may be present on the squares and we want to 
          //ensure that we don't count the same piece more than once.
          List<String> otherSceneSquareContents = scene.getSquareContents (row, col);
          
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
    for (int row = 0; row < _height; row++) {
      for (int col = 0; col < _width; col++) {
        if(!this.isEmpty(row, col)){
          List<String> thisSquareContents = this.getSquareContents(row, col);
          List<String> otherSquareContents = scene.getSquareContents (row, col);
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
    for (int row = 0; row < _height; row++) {
      for (int col = 0; col < _width; col++) {
        if(!this.isEmpty(row, col)){
          List<String> thisSquareContents = this.getSquareContents(row, col);
          List<String> otherSquareContents = scene.getSquareContents (row, col);
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

