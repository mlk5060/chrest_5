// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

import java.util.Objects;

/**
 * The ItemSquarePattern is a type of PrimitivePattern used to hold 
 * objects places on a square array.  The item-on-square is treated 
 * as a single object.  Instances of this class are immutable.
 *
 * @author Peter C. R. Lane
 */
public class ItemSquarePattern extends PrimitivePattern {
  
  private final String _item;
  private final int _column;
  private final int _row;

  /**
   * Constructor takes a string to identify the item, and a column and row
   * to identify the square.
   */
  public ItemSquarePattern (String item, int column, int row) {
    _item = item;
    _column = column;
    _row = row;
  }

  /** 
   * Accessor method for the item.
   * 
   * @return 
   */
  public String getItem () {
    return _item;
  }

  /**
   * Accessor method for the column.
   * 
   * @return 
   */
  public int getColumn () {
    return _column;
  }

  /**
   * Accessor method for the row.
   * 
   * @return
   */
  public int getRow () {
    return _row;
  }

  /**
   * @return {@link java.lang.Boolean#TRUE} if the following statements all 
   * evaluate to {@link java.lang.Boolean#TRUE}:
   * <ul>
   *  <li>
   *    The {@code object} specified is an {@link jchrest.lib.ItemSquarePattern}.
   *  </li>
   *  <li>
   *    The result of invoking {@link jchrest.lib.ItemSquarePattern#getItem()} 
   *    on this {@link jchrest.lib.ItemSquarePattern} and the {@code object}
   *    specified returns the same result.
   *  </li>
   *  <li>
   *    The result of invoking {@link jchrest.lib.ItemSquarePattern#getColumn()} 
   *    on this {@link jchrest.lib.ItemSquarePattern} and the {@code object}
   *    specified returns the same result.
   *  </li>
   *  <li>
   *    The result of invoking {@link jchrest.lib.ItemSquarePattern#getRow()} 
   *    on this {@link jchrest.lib.ItemSquarePattern} and the {@code object}
   *    specified returns the same result.
   *  </li>
   * </ul>
   */
  @Override
  public boolean equals (Object object) {
    if (object instanceof ItemSquarePattern) {
      ItemSquarePattern ios = (ItemSquarePattern)object;
      return (_item.equals (ios.getItem ()) &&
          _column == ios.getColumn () &&
          _row == ios.getRow ());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 47 * hash + Objects.hashCode(this._item);
    hash = 47 * hash + this._column;
    hash = 47 * hash + this._row;
    return hash;
  }

  /** 
   * @return The result of invoking {@link 
   * jchrest.lib.ItemSquarePattern#equals(java.lang.Object)}.
   */
  public boolean matches (Pattern givenPattern) {
    return this.equals ((ItemSquarePattern)givenPattern);
  }

  public String toString () {
    return "[" + _item + " " + _column + " " + _row + "]";
  }
}

