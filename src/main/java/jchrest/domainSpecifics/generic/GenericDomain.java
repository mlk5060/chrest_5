// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.domainSpecifics.generic;

import jchrest.domainSpecifics.DomainSpecifics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jchrest.architecture.Chrest;
import jchrest.domainSpecifics.Fixation;
import jchrest.lib.ExecutionHistoryOperations;
import jchrest.lib.ItemSquarePattern;
import jchrest.lib.ListPattern;
import jchrest.lib.PrimitivePattern;
import jchrest.domainSpecifics.Scene;
import jchrest.lib.Square;
import jchrest.lib.VisualSpatialFieldObject;

/**
  * The GenericDomain is used when no domain-specific methods have been created.
  */
public class GenericDomain extends DomainSpecifics {
  
  public GenericDomain(Chrest model, Integer maxFixationsInSet) {
    super(model,maxFixationsInSet);
  }
  
  /**
   * @param pattern
   * @return A {@link jchrest.lib.ListPattern} stripped of {@link 
   * jchrest.lib.ItemSquarePattern}s that:
   * 
   * <ol type="1">
   *  <li>
   *    Represent the CHREST model or the agent equipped with the CHREST model.
   *  </li>
   *  <li> 
   *    Blind, empty and unknown {@link jchrest.lib.ItemSquarePattern}s.
   *  </li>
   *  <li> 
   *    Are duplicated in the {@link jchrest.lib.ListPattern} passed.
   *  </li>
   * </ol>
   */
  @Override
  public ListPattern normalise (ListPattern pattern) {
    ListPattern result = new ListPattern(pattern.getModality());
    
    for(PrimitivePattern prim : pattern){
      if(prim instanceof ItemSquarePattern){
        ItemSquarePattern itemSquarePrim = (ItemSquarePattern)prim;
        String identifier = itemSquarePrim.getItem();
        if( 
          !identifier.equalsIgnoreCase(Scene.getCreatorToken()) &&
          !identifier.equals(Scene.getEmptySquareToken()) && 
          !identifier.equals(Scene.getBlindSquareToken()) &&
          !identifier.equals(VisualSpatialFieldObject.getUnknownSquareToken()) &&
          !result.contains(prim)
        ){
          result.add(prim);
        } 
      }
      else{
        result.add(prim);
      }
    }
    
    if(this._associatedModel != null){
      HashMap<String, Object> historyRowToInsert = new HashMap<>();
      
      //Generic operation name setter for current method.  Ensures for the row to 
      //be added that, if this method's name is changed, the entry for the 
      //"Operation" column in the execution history table will be updated without 
      //manual intervention and "Filter By Operation" queries run on the execution 
      //history DB table will still work.
      class Local{};
      historyRowToInsert.put(Chrest._executionHistoryTableOperationColumnName, 
        ExecutionHistoryOperations.getOperationString(this.getClass(), Local.class.getEnclosingMethod())
      );
      historyRowToInsert.put(Chrest._executionHistoryTableInputColumnName, pattern.toString() + "(" + pattern.getModalityString() + ")");
      historyRowToInsert.put(Chrest._executionHistoryTableOutputColumnName, result.toString() + "(" + result.getModalityString() + ")");
      this._associatedModel.addEpisodeToExecutionHistory(historyRowToInsert);
    }
    
    return result;
  }

  /** 
   * @param scene
   * @param model
   * @return A random square that doesn't represent a blind square.
   */
//  @Override
//  public Set<Square> getSalientObjectFixations (Scene scene, Chrest model, int time) {
//    Set<Square> result = new HashSet<> ();
//    
//    int randomCol = new java.util.Random().nextInt(scene.getWidth ());
//    int randomRow = new java.util.Random().nextInt(scene.getHeight ());
//    String objectOnSquare = scene.getSquareContents(randomCol, randomRow).getObjectClass();
//    
//    while( objectOnSquare.equals(Scene.getBlindSquareToken()) ){
//      randomCol = new java.util.Random().nextInt(scene.getWidth ());
//      randomRow = new java.util.Random().nextInt(scene.getHeight ());
//      objectOnSquare = scene.getSquareContents(randomCol, randomRow).getObjectClass();
//    }
//
//    result.add (new Square(randomCol, randomRow));
//    return result;
//  }

  /**
   * No possible movement fixations, so return empty list of proposals.
   */
  public List<Square> proposeMovementFixations (Scene scene, Square square) {
    return new ArrayList<Square> ();
  }

  @Override
  public Fixation getInitialFixationInSet(int time) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Fixation getNonInitialFixationInSet(int time) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean shouldLearnFromNewFixations(int time) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean isFixationSetComplete(int time) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean shouldAddNewFixation(int time) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}
