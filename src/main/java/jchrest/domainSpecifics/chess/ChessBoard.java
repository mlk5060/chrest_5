package jchrest.domainSpecifics.chess;

import jchrest.architecture.VisualSpatialField;
import jchrest.domainSpecifics.Scene;

/**
 * Represents a chess board.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class ChessBoard extends Scene{
  
  public ChessBoard(String boardName, VisualSpatialField associatedVisualSpatialField){
    super(boardName, 8, 8, 1, 1, associatedVisualSpatialField);
  }
  
  @Override
  public ChessObject getSquareContents(int col, int row){
    return (ChessObject)super.getSquareContents(col, row);
  }
}
