package jchrest.domainSpecifics.chess;

import java.awt.Color;
import jchrest.domainSpecifics.Scene;
import jchrest.domainSpecifics.SceneObject;

/**
 * Represents a chess object on a {@link 
 * jchrest.domainSpecifics.chess.ChessBoard} (pieces and empty squares).
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class ChessObject extends SceneObject{
  
  private final Color _colour;
  
  /**
   * 
   * @param identifier A unique identifier for {@link #this}.
   * @param objectClass The class of {@link #this}: should be of length 1 and
   * in <a 
   * href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">FEN
   * </a> notation, e.g. a black pawn should be "p".  Empty squares should be
   * noted as "."
   */
  public ChessObject(String identifier, String objectClass) {
    super(identifier, objectClass);
    
    if(!objectClass.equals(Scene.getBlindSquareToken()) && objectClass.length() != 1){
      throw new IllegalArgumentException(
        "The object class specified's length is not equal to 1: " + objectClass +
        " (length = " + objectClass.length() + ")"
      );
    }
    
    this._colour = (Character.isUpperCase(objectClass.charAt(0)) ? 
      Color.WHITE : 
      (Character.isLowerCase(objectClass.charAt(0)) ? 
        Color.BLACK :
        null
      )
    );
  }
  
  public Color getColour(){
    return this._colour;
  }
  
  @Override
  public String toString(){
    return super.toString() + ", colour: " + (this._colour == null ? "null" : this._colour.toString());
  }
}
