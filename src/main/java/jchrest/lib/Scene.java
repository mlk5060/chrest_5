// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

// TODO: Clarify order of row/column in methods calls/displays.
public class Scene {
  
  //Human-readable identifier for the scene.
  private final String _name;
  
  //The maximimum height and width of the scene.
  private final int _height;
  private final int _width;
  
  //Two-dimensional array whose first-dimension array elements embody rows of
  //the scene and second-dimension array elements embody columns of the scene.
  //Rows and columns are zero-indexed.
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
   * @return A ListPattern instance consisting of ItemSquarePattern instances 
   * representing each square of the scene.
   */
  public ListPattern getScene(){
    ListPattern scene = new ListPattern();
    
    for(int row = 0; row < _height; row++){
      for(int col = 0; col < _width; col++){
       scene.add( new ItemSquarePattern(this._scene[row][col], row, col) );
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
   * @param row The row to be modified.
   * @param items The items to add to the row in column order.
   */
  public void addItemsToRow (int row, char [] items) {
    for (int i = 0; i < items.length && i < _width; ++i) {
      _scene[row][i] = items[i] + "";
    }
  }

  public String getItem (int row, int column) {
    if (row >= 0 && row < _height && column >= 0 && column < _width) {
      return _scene[row][column];
    } else {
      return "";
    }
  }

  /**
   * Set the identifier for an item in the specified row and column of the 
   * scene.
   * 
   * @param row
   * @param column
   * @param item 
   */
  public void setItem (int row, int column, String item) {
    assert (row >= 0 && row < _height && column >= 0 && column < _width);
    _scene[row][column] = item;
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
            if (!_scene[row][col].equals(".")) {
              items.add (new ItemSquarePattern (_scene[row][col], col+1, row+1));
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
        if (isEmpty (row, col)) {
          ;
        } else {
          items += 1;
        }
      }
    }
    return items;
  }

  /**
   * Determines how many items are present both in this scene and the scene 
   * specified on the same rows and columns.
   * 
   * @param scene The scene to compare this scene against.
   * @return 
   */
  public int countOverlappingPieces (Scene scene) {
    int items = 0;
    for (int row = 0; row < _height; row++) {
      for (int col = 0; col < _width; col++) {
        if (isEmpty (row, col)) {
          ;
        } else if (_scene[row][col].equals (scene.getItem (row, col))) {
          items += 1;
        } else {
          ;
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
        if (isEmpty(row, col)) {
          ; // do nothing for empty squares
        } else if (_scene[row][col].equals (scene.getItem (row, col))) {
          ; // no error if this and given scene have the same item
        } else { // an item in this scene is not in given scene
          errors += 1;
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
        if (scene.isEmpty (row, col)) {
          ; // do nothing for empty squares in given scene
        } else if (scene.getItem(row, col).equals (_scene[row][col])) {
          ; // no error if given and this scene have the same item
        } else { // an item in given scene is not in this scene
          errors += 1;
        }
      }
    }

    return errors;
  }
}

