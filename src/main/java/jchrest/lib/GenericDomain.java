// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jchrest.architecture.Chrest;

/**
  * The GenericDomain is used when no domain-specific methods have been created.
  */
public class GenericDomain extends DomainSpecifics {
  
  public GenericDomain(Chrest model) {
    super(model);
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
  @Override
  public Set<Square> proposeSalientSquareFixations (Scene scene, Chrest model, int time) {
    Set<Square> result = new HashSet<> ();
    
    int randomCol = new java.util.Random().nextInt(scene.getWidth ());
    int randomRow = new java.util.Random().nextInt(scene.getHeight ());
    String objectOnSquare = scene.getSquareContents(randomCol, randomRow).getObjectClass();
    
    while( objectOnSquare.equals(Scene.getBlindSquareToken()) ){
      randomCol = new java.util.Random().nextInt(scene.getWidth ());
      randomRow = new java.util.Random().nextInt(scene.getHeight ());
      objectOnSquare = scene.getSquareContents(randomCol, randomRow).getObjectClass();
    }

    result.add (new Square(randomCol, randomRow));
    return result;
  }

  /**
   * No possible movement fixations, so return empty list of proposals.
   */
  public List<Square> proposeMovementFixations (Scene scene, Square square) {
    return new ArrayList<Square> ();
  }

  @Override
  public int getCurrentTime() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * Takes into account the location of a creator in the {@link 
   * jchrest.lib.Scene} passed so that, if one is present, coordinates are 
   * translated so they are relative to the location of the {@link 
   * jchrest.lib.Scene} creator otherwise, coordinates remain unaltered.
   * 
   * @param listPattern
   * @param scene
   * @return 
   */
  @Override
  public ListPattern convertDomainSpecificCoordinatesToSceneSpecificCoordinates(ListPattern listPattern, Scene scene) {
    Square locationOfCreator = scene.getLocationOfCreator();
    if(locationOfCreator == null){
      return listPattern;
    }
    else{
      ListPattern convertedListPattern = new ListPattern(listPattern.getModality());
      Iterator<PrimitivePattern> iterator = listPattern.iterator();
      while(iterator.hasNext()){
        PrimitivePattern pattern = iterator.next();
        assert (pattern instanceof ItemSquarePattern);
        ItemSquarePattern isp = (ItemSquarePattern)pattern;
        convertedListPattern.add(
          new ItemSquarePattern(
            isp.getItem(),
            isp.getColumn() + locationOfCreator.getColumn(),
            isp.getRow() + locationOfCreator.getRow()
          )
        );
      }
      
      return convertedListPattern;
    }
  }

  /**
   * Takes into account the location of a creator in the {@link 
   * jchrest.lib.Scene} passed so that, if one is present, coordinates are 
   * translated so they are relative to the location of the {@link 
   * jchrest.lib.Scene} creator otherwise, coordinates remain unaltered. 
   * 
   * @param listPattern
   * @param scene
   * @return 
   */
  @Override
  public ListPattern convertSceneSpecificCoordinatesToDomainSpecificCoordinates(ListPattern listPattern, Scene scene) {
    Square locationOfCreator = scene.getLocationOfCreator();
    if(locationOfCreator == null){
      return listPattern;
    }
    else{
      ListPattern convertedListPattern = new ListPattern(listPattern.getModality());
      Iterator<PrimitivePattern> iterator = listPattern.iterator();
      while(iterator.hasNext()){
        PrimitivePattern pattern = iterator.next();
        assert (pattern instanceof ItemSquarePattern);
        ItemSquarePattern isp = (ItemSquarePattern)pattern;
        convertedListPattern.add(
          new ItemSquarePattern(
            isp.getItem(),
            isp.getColumn() - locationOfCreator.getColumn(),
            isp.getRow() - locationOfCreator.getRow()
          )
        );
      }
      
      return convertedListPattern;
    }
  }
}
