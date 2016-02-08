package jchrest.lib;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.BiFunction;

/**
 * Should be used to maintain a history of states for classes in the {@link
 * jchrest.architecture} package.
 * 
 * Despite instances of this class extending {@link java.util.TreeMap>} the only
 * functionality available to modify its {@link java.util.Map.Entry entries} 
 * are those functions that add new {@link java.util.Map.Entry entries}, i.e.
 * 
 * <ul>
 *  <li>
 *    {@link jchrest.lib.HistoryTreeMap#put(java.lang.Integer, 
 *    java.lang.Object)}
 *  </li>
 *  <li>{@link jchrest.lib.HistoryTreeMap#putAll(java.util.Map)}</li>
 * </ul>
 * 
 * Both of these methods always check to see if the new {@link 
 * java.util.Map.Entry} that will be created does not rewrite the existing 
 * history of {@link #this}.  If they will, a {@link java.lang.RuntimeException}
 * is thrown.
 * 
 * The following methods inherited from {@link java.util.TreeMap>} are also
 * unsupported since they will either modify {@link java.util.Map.Entry entries} 
 * in unexpected ways, remove {@link java.util.Map.Entry entries} or replace the 
 * values of {@link java.util.Map.Entry entries}, i.e. they will rewrite the
 * history of a {@link jchrest.lib.HistoryTreeMap}:
 * 
 * <ul>
 *  <li>
 *    {@link java.util.TreeMap#merge(java.lang.Object, java.lang.Object, 
 *    java.util.function.BiFunction)}
 *  </li>
 *  <li>{@link java.util.TreeMap#pollFirstEntry()}</li>
 *  <li>{@link java.util.TreeMap#pollLastEntry()}</li>
 *  <li>
 *    {@link java.util.TreeMap#putIfAbsent(java.lang.Object, 
 *    java.lang.Object)} (redundant since the {@link 
 *    jchrest.lib.HistoryTreeMap#put(java.lang.Integer, java.lang.Object)} and 
 *    {@link jchrest.lib.HistoryTreeMap#putAll(java.util.Map)} functions already 
 *    provide such functionality)
 *  </li>
 *  <li>{@link java.util.TreeMap#remove(java.lang.Object)}</li>
 *  <li>
 *    {@link java.util.TreeMap#remove(java.lang.Object, 
 *    java.lang.Object)}
 *  </li>
 *  <li>
 *    {@link java.util.TreeMap#replace(java.lang.Object, 
 *    java.lang.Object)}
 *  </li>
 *  <li>
 *    {@link java.util.TreeMap#replace(java.lang.Object, java.lang.Object, 
 *    java.lang.Object)}
 *  </li>
 *  <li>{@link java.util.TreeMap#replaceAll(java.util.function.BiFunction)}</li>
 * </ul>
 * 
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class HistoryTreeMap extends TreeMap<Integer, Object>{
  
  /**
   * Determines if adding the specified key to {@link #this} would rewrite its
   * current history.
   * 
   * @param time
   * @return 
   */
  public boolean rewritingHistory(Integer time){
    return this.containsKey(time) || this.ceilingKey(time) != null;
  }
  
  /**
   * 
   * @param key
   * @param value
   * @param func
   * @return
   * @throws UnsupportedOperationException since it should not be possible to
   * rewrite the history of {@link #this} by merging.
   */
  @Override
  public Object merge(
    Integer key, 
    Object value,
    BiFunction<? super Object, ? super Object, ? extends Object> func
  ) throws UnsupportedOperationException{
    throw new UnsupportedOperationException();
  }
  
  /**
   * 
   * @return
   * @throws UnsupportedOperationException since it should not be possible to
   * rewrite the history of {@link #this} by removing entries.
   */
  @Override
  public Entry pollFirstEntry() throws UnsupportedOperationException{
    throw new UnsupportedOperationException();
  }
  
  /**
   * 
   * @return
   * @throws UnsupportedOperationException since it should not be possible to
   * rewrite the history of {@link #this} by removing entries.
   */
  @Override
  public Entry pollLastEntry() throws UnsupportedOperationException{
    throw new UnsupportedOperationException();
  }
  
  /**
   * 
   * @param time
   * @param value
   * @return
   * @throws UnsupportedOperationException since the only functions that should 
   * be used to add entries to {@link #this} are {@link 
   * jchrest.lib.HistoryTreeMap#put(java.lang.Integer, java.lang.Object)} and
   * {@link jchrest.lib.HistoryTreeMap#putAll(java.util.Map)}.
   */
  @Override
  public Object putIfAbsent(Integer time, Object value) throws UnsupportedOperationException{
    throw new UnsupportedOperationException();
  }
  
  /**
   *
   * @param time
   * @param value
   * @return The result of {@link java.util.TreeMap#put(java.lang.Object, 
   * java.lang.Object) if adding a new {@link java.util.Map.Entry} 
   * consisting of the time and value specified will not rewrite the history of
   * {@link #this}, null if it will.
   */
  @Override
  public Object put(Integer time, Object value) {
    if(!this.rewritingHistory(time)){
      super.put(time, value);
      return true;
    }
    
    return false;
  }
  
  /**
   * @param map
   */
  @Override
  public void putAll(Map <? extends Integer, ? extends Object> map) {
    HistoryTreeMap mapSpecified = (HistoryTreeMap)map;
    int earliestTimeInMapSpecified = mapSpecified.firstKey();
    
    if(!this.rewritingHistory(earliestTimeInMapSpecified)){
      super.putAll(mapSpecified);
    }
  }
  
  /**
   * @param time
   * @return 
   * @throws UnsupportedOperationException since it should not be possible to
   * rewrite the history of {@link #this} by removing entries.
   */
  @Override
  public Object remove(java.lang.Object time) throws UnsupportedOperationException{
    throw new UnsupportedOperationException();
  }
  
  /**
   * @param time
   * @param value
   * @return 
   * @throws UnsupportedOperationException since it should not be possible to
   * rewrite the history of {@link #this} by removing entries.
   */
  @Override
  public boolean remove(java.lang.Object time, java.lang.Object value) throws UnsupportedOperationException{
    throw new UnsupportedOperationException();
  }
  
  /**
   * @param time
   * @param value
   * @return
   * @throws UnsupportedOperationException since it should not be possible to
   * rewrite the history of {@link #this} by replacing entries.
   */
  @Override
  public Object replace(Integer time, Object value) throws UnsupportedOperationException{
    throw new UnsupportedOperationException();
  }
  
  /**
   * @param time
   * @param oldValue
   * @param newValue
   * @return
   * @throws UnsupportedOperationException since it should not be possible to
   * rewrite the history of {@link #this} by replacing entries.
   */
  @Override
  public boolean replace(Integer time, Object oldValue, Object newValue) throws UnsupportedOperationException{
    throw new UnsupportedOperationException();
  }
  
  /**
   * @param func
   * @throws UnsupportedOperationException since it should not be possible to
   * rewrite the history of {@link #this} by replacing entries.
   */
  @Override
  public void replaceAll(BiFunction<? super Integer, ? super Object, ? extends Object> func) throws UnsupportedOperationException{
    throw new UnsupportedOperationException();
  }
}
