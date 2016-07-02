// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

import java.util.ArrayList;

/** 
 * Parent class of all patterns.
 *
 * @author Peter C. R. Lane
 */
public abstract class Pattern {

  /**
   * Factory method to make a NumberPattern.
   * 
   * @param number
   * @return 
   */
  public static NumberPattern makeNumber (int number) {
    return NumberPattern.create (number);
  }

  /**
   * Factory method to make a StringPattern.
   * 
   * @param str
   * @return 
   */
  public static StringPattern makeString (String str) {
    return StringPattern.create (str);
  }

  private static ListPattern makeList (int[] numbers, Modality modality) {
    ListPattern list = new ListPattern (modality);
    for (int i = 0; i < numbers.length; ++i)
    {
      list.add (NumberPattern.create (numbers[i]));
    }
    return list;
  }

  /** 
   * Factory method.
   * 
   * @param numbers Each {@link java.lang.Integer} is converted into a {@link 
   * jchrest.lib.NumberPattern} and added to a {@link jchrest.lib.ListPattern}.
   * 
   * @return A {@link jchrest.lib.ListPattern} containing {@code numbers} with
   * {@link jchrest.lib.Modality#VISUAL}.
   */
  public static ListPattern makeVisualList (int[] numbers) {
    return makeList (numbers, Modality.VISUAL);
  }

  public static ListPattern makeVerbalList (int[] numbers) {
    return makeList (numbers, Modality.VERBAL);
  }

  public static ListPattern makeList (String[] strings, Modality modality) {
    ListPattern list = new ListPattern (modality);
    for (int i = 0; i < strings.length; ++i)
    {
      list.add (StringPattern.create (strings[i]));
    }
    return list;
  }

  /** Factory method to make a ListPattern given an array of Strings.
   * Each number is converted into a StringPattern and added to the 
   * ListPattern.
   */
  public static ListPattern makeVisualList (String[] strings) {
    return makeList (strings, Modality.VISUAL);
  }

  public static ListPattern makeVerbalList (String[] strings) {
    return makeList (strings, Modality.VERBAL);
  }

  public static ListPattern makeActionList (String[] strings) {
    return makeList (strings, Modality.ACTION);
  }

  public abstract boolean matches (Pattern pattern);
  public abstract String toString ();
}

