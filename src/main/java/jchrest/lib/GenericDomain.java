// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jchrest.architecture.Chrest;

/**
  * The GenericDomain is used when no domain-specific methods have been created.
  */
public class GenericDomain implements DomainSpecifics {
  
  /**
   * Remove self from pattern since the creator will never need to learn its 
   * own location given that everything will be relative to it if it exists in
   * the pattern passed.  Also, remove duplicates.
   */
  public ListPattern normalise (ListPattern pattern) {
    ListPattern result = new ListPattern();
    
    for(PrimitivePattern prim : pattern){
      String identifier = "";
      
      if(prim instanceof StringPattern){
        StringPattern stringPrim = (StringPattern)prim;
        identifier = stringPrim.getString();
      }
      else if(prim instanceof ItemSquarePattern){
        ItemSquarePattern itemSquarePrim = (ItemSquarePattern)prim;
        identifier = itemSquarePrim.getItem();
      }
      
      if( 
        ( !identifier.equalsIgnoreCase(Scene.getSelfIdentifier()) || identifier.isEmpty() ) && 
        !result.contains(prim)
      ){
        result.add(prim);
      }
    }
    
    return result;
  }

  /** 
   * Return a random square on scene that isn't blind or empty.
   * @param scene
   * @param model
   * @return 
   */
  public Set<Square> proposeSalientSquareFixations (Scene scene, Chrest model) {
    Set<Square> result = new HashSet<Square> ();
    
    int randomCol = new java.util.Random().nextInt(scene.getWidth ());
    int randomRow = new java.util.Random().nextInt(scene.getHeight ());
    
    while( scene.getItemsOnSquare(randomCol, randomRow, false, false).isEmpty() ){
      randomCol = new java.util.Random().nextInt(scene.getWidth ());
      randomRow = new java.util.Random().nextInt(scene.getHeight ());
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
}
