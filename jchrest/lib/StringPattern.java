package jchrest.lib;

/**
 * The StringPattern is a type of PrimitivePattern used to hold 
 * Strings.  The String is treated as a single object, and 
 * cannot be decomposed into smaller elements, such as letters.
 *
 * @author Peter C. R. Lane
 */
public class StringPattern extends PrimitivePattern {
  private String _name;

  /**
   * Constructor takes a String name which is used to 
   * denote this pattern.
   */
  public StringPattern (String name) {
    _name = name;
  }

  /**
   * Accessor method for the stored name.
   */
  public String getString () {
    return _name;
  }

  /**
   * Two StringPatterns are only equal if their stored names 
   * are the same.
   */
  public boolean equals (Pattern pattern) {
    if (pattern instanceof StringPattern) {
      return _name.equals (((StringPattern)pattern).getString ());
    } else {
      return false;
    }
  }

  /** 
   * Two StringPatterns only match if their stored names are the same.
   */
  public boolean matches (Pattern pattern) {
    if (pattern instanceof StringPattern) {
      return _name.equals (((StringPattern)pattern).getString ());
    } else {
      return false;
    }
  }

  public String toString () {
    return _name;
  }
}

