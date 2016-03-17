// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

/**
 * Square is a convenience class to hold a row and column.
 *
 * @author Peter C. R. Lane
 */
public class Square {
  private int _column;
  private int _row;

  /**
   * Constructor makes an instance from given row and column.
   */
  public Square (int col, int row) {
    _column = col;
    _row = row;
  }

  /**
   * Accessor method for the column.
   */
  public int getColumn () {
    return _column;
  }

  /**
   * Accessor method for the row.
   */
  public int getRow () {
    return _row;
  }

  @Override
  public String toString () {
    return "(" + _column + ", " + _row + ")";
  }
  
  @Override
  public boolean equals(Object square){
    return square != null && square.getClass().equals(Square.class) && this.toString().equals(square.toString());
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + this._column;
    hash = 31 * hash + this._row;
    return hash;
  }
}

