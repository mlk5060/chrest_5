package jchrest.lib;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jchrest.architecture.Chrest;

/**
 *
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class TileworldDomain implements DomainSpecifics{

  @Override
  public ListPattern normalise(ListPattern pattern) {
    //Remove self from pattern since the location of self doesn't need to be
    //learned.
    for(PrimitivePattern prim : pattern){
      ItemSquarePattern item = (ItemSquarePattern)prim;
      if(item.getItem().equals("SELF")){
        ListPattern primToRemove = new ListPattern();
        primToRemove.add(prim);
        pattern.remove( primToRemove );
      }
    }
    
    return pattern;
  }

  /**
   * In Tileworld, salient squares are any that aren't empty.
   * @param scene
   * @param model
   * @return 
   */
  @Override
  public Set<Square> proposeSalientSquareFixations(Scene scene, Chrest model) {
    Set<Square> salientSquareFixations = new HashSet<Square> ();
    for(int row = 0; row < scene.getHeight(); row++){
      for(int col = 0; col < scene.getWidth(); col++){
        if( !scene.getItem(row, col).equals(".") ){
          salientSquareFixations.add(new Square(col, row));
        }
      }
    }
    return salientSquareFixations;
  }

  @Override
  public List<Square> proposeMovementFixations(Scene scene, Square square) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  
}
