// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

import jchrest.domainSpecifics.Scene;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * The ListPattern is the primary datatype used to represent compound 
 * patterns within Chrest.  A ListPattern holds an ordered list of 
 * instances of other pattern types.  The ListPattern may optionally 
 * indicate that it cannot be extended by setting the _finished flag.
 * Note that once a pattern is 'finished', it cannot be added to.
 *
 * TODO: Think about if ListPatterns can be embedded within ListPatterns
 *       - would have to look inside ListPattern to make the match.
 *
 * @author Peter C. R. Lane
 */
public class ListPattern extends Pattern implements Iterable<PrimitivePattern> {
  private List<PrimitivePattern> _list;  // items within the pattern
  private Modality _modality;   // record type of ListPattern
  private boolean _finished;    // marker to indicate if pattern complete

  public ListPattern () {
    this (Modality.VISUAL);
  }

  public ListPattern (Modality modality) {
    _list = new ArrayList<PrimitivePattern> ();
    _modality = modality;
    _finished = false;
  }

  /** 
   * Used in constructing instances by {@link Pattern} class.
   * Add pattern to list, unless the pattern is 'finished'.
   */
  public void add (PrimitivePattern pattern) {
    if (!_finished) {
      _list.add (pattern);
    }
  }

  /**
   * Construct a copy of this pattern, so that it can be modified 
   * without affecting the original.
   */
  public ListPattern clone () {
    ListPattern result = new ListPattern (_modality);
    for (PrimitivePattern pattern : _list) {
      result.add (pattern);
    }
    if (isFinished ()) {
      result.setFinished ();
    }
    return result;
  }

  /**
   * Return the number of patterns held inside the list pattern.
   */
  public int size () {
    return _list.size ();
  }

  /**
   * Check if the list pattern is empty, holding no patterns.
   */
  public boolean isEmpty () {
    return _list.isEmpty ();
  }

  /**
   * Retrieve the indexed item from the list pattern.
   * There is no check on the validity of the index.
   */
  public PrimitivePattern getItem (int index) {
    return _list.get (index);
  }

  /**
   * Accessor method to _finished property.
   */
  public boolean isFinished () {
    return _finished;
  }

  /**
   * Class level method to check if two patterns have the same modality.
   */
  static public boolean isSameModality (ListPattern pattern1, ListPattern pattern2) {
    return pattern1._modality == pattern2._modality;
  }

  /**
   * Accessor to retrieve the modality of the pattern.
   */
  public Modality getModality () {
    return _modality;
  }

  /**
   * Mutator to change modality of pattern.
   */
  public void setModality (Modality modality) {
    _modality = modality;
  }

  /**
   * Accessor method to check visual modality.
   */
  public boolean isVisual () {
    return _modality == Modality.VISUAL;
  }

  /**
   * Accessor method to check verbal modality.
   */
  public boolean isVerbal () {
    return _modality == Modality.VERBAL;
  }

  /**
   * Accessor method to check action modality.
   */
  public boolean isAction () {
    return _modality == Modality.ACTION;
  }

  /**
   * Convert the modality into a string.
   */
  public String getModalityString () {
    if (isVisual ()) {
      return "Visual";
    } else if (isVerbal ()) {
      return "Verbal";
    } else { // if (isAction ())
      return "Action";
    }
  }

  /**
   * Set the _finished property to true.
   */
  public void setFinished () {
    _finished = true;
  }

  /**
   * Set the _finished property to false.
   */
  public void setNotFinished () {
    _finished = false;
  }

  /** 
   * @param pattern
   * 
   * @return {@link java.lang.Boolean#TRUE} if the {@code pattern} specified is 
   * not null, {@link #this} and the {@code pattern} specified are both 
   * instances of {@link jchrest.lib.ListPattern}, the {@link 
   * jchrest.lib.Modality Modalities} of {@link #this} and the {@code pattern} 
   * specified are equal, the result of invoking {@link 
   * jchrest.lib.ListPattern#size()} on {@link #this} and the {@code pattern} 
   * specified are equal, {@link #this} and the {@code pattern} specified 
   * contain the same items and {@link #this} and the {@code pattern} specified
   * are both un/finished.
   */
  @Override
  public boolean equals(Object pattern) { 
    if(pattern != null && this.getClass().equals(pattern.getClass())){
      ListPattern patt = (ListPattern)pattern;
      
      if(
        this._modality == patt._modality &&
        this.size() == patt.size() 
      ){

        for (int i = 0, n = size (); i < n; ++i) {
          if (!patt.getItem(i).equals(getItem(i))) {
            return false; // false if any item not the same
          }
        }
        
        // Finally, they must both have the 'finished' property the same
        return _finished == patt.isFinished ();
      }
    }
    
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 89 * hash + Objects.hashCode(this._list);
    hash = 89 * hash + Objects.hashCode(this._modality);
    hash = 89 * hash + (this._finished ? 1 : 0);
    return hash;
  }

  /** 
   * Determines whether {@link #this} and the {@code pattern} specified match.
   * 
   * @param pattern
   * 
   * @return If the following conditions all evaluate to {@link 
   * java.lang.Boolean#TRUE} then {@link java.lang.Boolean#TRUE} is returned.  
   * Otherwise, {@link java.lang.Boolean#FALSE} is returned:
   * <ul>
   *  <li>{@code pattern} is also a {@link jchrest.lib.ListPattern}</li>
   *  <li>
   *    Invoking {@link jchrest.lib.ListPattern#getModality()} on {@link #this}
   *    and {@code pattern} returns the same {@link jchrest.lib.Modality}.
   *  </li>
   *  <li>
   *    If {@link #this} is "finished":
   *    <ul>
   *      <li>
   *        The same value for {@link jchrest.lib.ListPattern#size()} must be
   *        returned when it is invoked on {@link #this} and {@code pattern}.
   *      </li>
   *      <li>
   *        {@code pattern} must also be "finished"
   *      </li>
   *    </ul>
   *  </li>
   *  <li>
   *    If {@link #this} is not "finished" the value for {@link 
   *    jchrest.lib.ListPattern#size()} must be less than or equal to the result
   *    for this method when invoked in context of {@code pattern}.
   *  </li>
   *  <li>
   *    The ordering of {@link jchrest.lib.PrimitivePattern PrimitivePatterns}
   *    in {@link #this} must match the ordering of {@link 
   *    jchrest.lib.PrimitivePattern PrimitivePatterns} in {@code pattern}.
   *  </li>
   * </ul>
   */
  @Override
  public boolean matches (Pattern pattern) {
    if (!(pattern instanceof ListPattern)) return false;
    ListPattern patternToMatchAgainst = (ListPattern)pattern;

    if (this._modality != patternToMatchAgainst._modality) return false;

    // check relative sizes of patterns
    if (this.isFinished ()) {
      if (this.size () != patternToMatchAgainst.size()) return false;
      if (!patternToMatchAgainst.isFinished ()) return false;

    } else {
      // this pattern cannot be larger than given pattern to match it.
      if (this.size () > patternToMatchAgainst.size ()) return false;
    }
    // now just check that the items in this pattern match up with the given pattern
    for (int i = 0, n = size (); i < n; ++i) {
      if (!patternToMatchAgainst.getItem(i).equals(this.getItem (i))) {
        return false; // false if any item not the same
      }
    }
    return true;

  }

  /**
   * Return a new ListPattern forming the parts of this pattern without 
   * the matching elements of the given pattern. 
   */
  public ListPattern remove (ListPattern pattern) {
    ListPattern result = new ListPattern (_modality);

    int i = 0;
    boolean takingItems = false;
    while (i < size ()) {
      if (takingItems) {
        result.add (getItem (i));
      } else if (i < pattern.size () && pattern.getItem(i).equals(getItem (i))) {
        ;
      } else {
        takingItems = true;
        result.add (getItem (i));
      }
      i += 1;
    }
    if (isFinished () && !(result.isEmpty () && pattern.isFinished ())) {
      result.setFinished ();
    }

    return result;
  }
  
  /**
   * Return a new ListPattern formed from the contents of this list pattern and the 
   * contents of the given pattern appended to it.
   */
  public ListPattern append (ListPattern pattern) {
    ListPattern result = new ListPattern (_modality);
    
    for (PrimitivePattern item : _list) {
      result.add (item);
    }

    for (PrimitivePattern item : pattern) { 
      result.add (item);
    }

    if (pattern.isFinished ()) {
      result.setFinished ();
    }

    return result;
  }

  /** Return a new ListPattern formed from the contents of this list pattern and 
   * the given PrimitivePattern appended to it.
   */
  public ListPattern append (PrimitivePattern pattern) {
    ListPattern result = new ListPattern (_modality);

    for (PrimitivePattern item : _list) {
      result.add (item);
    }
    result.add (pattern);

    return result;
  }

  /**
   * Construct a new pattern containing just the first item in this one.
   */
  public ListPattern getFirstItem () {
    ListPattern pattern = new ListPattern (_modality);
    if (size () > 0) {
      pattern.add (getItem (0));
    }
    pattern.setFinished ();

    return pattern;
  }

  /**
   * Render the list pattern as a string.
   */
  public String toString () {
    String result = "< ";
    for (PrimitivePattern pattern : _list) {
      result += pattern.toString () + " ";
    }
    if (_finished) result += "$ ";

    return result + ">";
  }

  public boolean contains (PrimitivePattern given) {
    for (PrimitivePattern item : _list) {
      if (item.equals (given)) return true;
    }
    return false;
  }

  /**
   * Compare this list pattern with a given list pattern, returning true if 
   * the two share k or more items.
   */
  public boolean isSimilarTo (ListPattern pattern, int k) {
    int count = 0;

    for (PrimitivePattern item : _list) {
      if (pattern.contains (item)) {
        count += 1;
        // remove the matching item from pattern
        ListPattern itemPattern = new ListPattern (_modality);
        itemPattern.add (item);
        pattern = pattern.remove (itemPattern);
      } 
    }
    
    return count >= k;
  }

  /**
   * Return a new list pattern with the items sorted using the given comparator.
   */
  public ListPattern sort (Comparator<PrimitivePattern> comparator) {
    ListPattern result = new ListPattern (_modality);
    List<PrimitivePattern> items = new ArrayList<PrimitivePattern> ();
    for (PrimitivePattern pattern : _list) {
      items.add (pattern);
    }
    Collections.sort (items, comparator);
    for (PrimitivePattern pattern : items) {
      result.add (pattern);
    }
    if (isFinished ()) {
      result.setFinished ();
    }
    return result;
  }
  
  /**
   * 
   * @return If {@link #this} contains {@link jchrest.lib.ItemSquarePattern 
   * ItemSquarePatterns}, any that return {@link 
   * jchrest.domainSpecifics.Scene#BLIND_SQUARE_TOKEN} when {@link 
   * jchrest.lib.ItemSquarePattern#getItem()} is invoked on them will be 
   * removed from {@link #this}.
   */
  public ListPattern removeBlindObjects(){
    ListPattern result = new ListPattern(this.getModality());
    
    for(PrimitivePattern pattern : this){
      String item = ((ItemSquarePattern)pattern).getItem();
      if(!item.equals(Scene.BLIND_SQUARE_TOKEN)){
        result.add(pattern);
      }
    }
    
    return result;
  }
  
  /**
   * 
   * @return If {@link #this} contains {@link jchrest.lib.ItemSquarePattern 
   * ItemSquarePatterns}, any that return {@link 
   * jchrest.domainSpecifics.Scene#CREATOR_TOKEN} when {@link 
   * jchrest.lib.ItemSquarePattern#getItem()} is invoked on them will be 
   * removed from {@link #this}.
   */
  public ListPattern removeCreatorObject(){
    ListPattern result = new ListPattern(this.getModality());
    
    for(PrimitivePattern pattern : this){
      String item = ((ItemSquarePattern)pattern).getItem();
      if(!item.equals(Scene.CREATOR_TOKEN)){
        result.add(pattern);
      }
    }
    
    return result;
  }
  
  /**
   * Creates a new {@link jchrest.lib.ListPattern} by removing all patterns from 
   * this {@link #this} where the result of calling 
   * {@link jchrest.lib.ItemSquarePattern#getItem()} returns 
   * {@link jchrest.lib.Scene#getBlindSquareToken()}, 
   * {@link jchrest.lib.Scene#getEmptySquareToken()} or
   * {@link jchrest.lib.VisualSpatialFieldObject#getUnknownSquareToken()}.
   * 
   * @return 
   */
  public ListPattern removeBlindEmptyAndUnknownItems(){
    ListPattern result = new ListPattern(this.getModality());
    
    for(PrimitivePattern pattern : this){
      String item = ((ItemSquarePattern)pattern).getItem();
      if( 
        !item.equals(Scene.getBlindSquareToken()) && 
        !item.equals(Scene.getEmptySquareToken()) &&
        !item.equals(VisualSpatialFieldObject.getUnknownSquareToken())
      ){
        result.add(pattern);
      }
    }
    
    return result;
  }

  /** 
   * Support iteration over the items of a list pattern.
   */
  public Iterator<PrimitivePattern> iterator () {
    return new ListPatternIterator (_list);
  }
  
  class ListPatternIterator implements Iterator<PrimitivePattern> {
    private int _index = 0;
    private List<PrimitivePattern> _items;

    ListPatternIterator (List<PrimitivePattern> items) {
      _items = items;
    }

    public boolean hasNext () {
      return _index < _items.size ();
    }

    public PrimitivePattern next () {
      if (hasNext ()) {
        _index += 1;
        return _items.get(_index-1);
      }
      throw new java.util.NoSuchElementException();
    }

    public void remove () {
      throw new UnsupportedOperationException ();
    }
  }
}

