package jchrest.architecture;

import java.util.ArrayList;
import java.util.List;
import jchrest.lib.ListPattern;

/**
 * Class that represents the "Mind's Eye".  The mind's eye is a finite-sized 
 * list of "ListPattern" objects that do not have to be learned in LTM. 
 * Therefore, the mind's eye is not a list of "Node" objects since a node is 
 * only created when the pattern exists in a chunk in LTM.
 * 
 * @author Martyn Lloyd-Kelly <mlk5060@liverpool.ac.uk>
 */
public class MindsEye {
  
  private List<ListPattern> _items;
  private int _size;
  
  //Constructor
  public MindsEye(int size){
    _size = size;
    _items = new ArrayList<>();
  }
  
  /**
   * Adds the specified pattern to the mind's eye if the mind's eye is not 
   * already "full", the pattern is of a visual modality and the pattern is not
   * empty.
   * 
   * @param pattern The pattern to be added to the mind's eye.
   * @return True if pattern was added, false if not.
   */
  public boolean add(ListPattern pattern){
    boolean patternAdded = false;
    
    if(_items.size() < _size && pattern.isVisual() && !pattern.isEmpty()){
      _items.add(pattern);
      patternAdded = true;
    }
    
    return patternAdded;
  }
  
  /**
   * Clears the mind's eye of all content.
   */
  public void clear(){
    _items.clear();
  }
  
  /**
   * Returns the entire contents of the mind's eye as an array.
   * 
   * @return Object[]
   */
  public Object[] getContents(){
    return _items.toArray();
  }
  
  /**
   * Returns the current size of the mind's eye.
   * @return int
   */
  public int getSize(){
    return _size;
  }
  
  /**
   * Sets the size of the mind's eye (the number of items it can hold at any 
   * time) to the value specified in the parameter passed.
   * 
   * @param size The new size of the mind's eye.
   */
  public void setSize(int size){
    _size = size;
  }
}
