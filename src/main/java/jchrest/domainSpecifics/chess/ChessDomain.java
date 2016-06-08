// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.domainSpecifics.chess;

import java.awt.Color;
import jchrest.domainSpecifics.DomainSpecifics;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jchrest.architecture.Chrest;
import jchrest.domainSpecifics.Fixation;
import jchrest.domainSpecifics.chess.fixations.AttackDefenseFixation;
import jchrest.domainSpecifics.chess.fixations.GlobalStrategyFixation;
import jchrest.domainSpecifics.fixations.CentralFixation;
import jchrest.domainSpecifics.fixations.PeripheralItemFixation;
import jchrest.domainSpecifics.chess.fixations.SalientManFixation;
import jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation;
import jchrest.domainSpecifics.fixations.PeripheralSquareFixation;
import jchrest.lib.ItemSquarePattern;
import jchrest.lib.ListPattern;
import jchrest.lib.PrimitivePattern;
import jchrest.domainSpecifics.Scene;
import jchrest.lib.Square;

/**
  * Used to play chess.
  * 
  * @author Peter C. R. Lane
  * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
  */
public class ChessDomain extends DomainSpecifics {
  
  //Stores the canonical order of chess pieces
  private static final Map<String, Integer> PIECE_ORDER = new HashMap();
  static {
    PIECE_ORDER.put("P", 0);
    PIECE_ORDER.put("p", 1);
    PIECE_ORDER.put("K", 2);
    PIECE_ORDER.put("k", 3);
    PIECE_ORDER.put("B", 4);
    PIECE_ORDER.put("b", 5);
    PIECE_ORDER.put("N", 6);
    PIECE_ORDER.put("n", 7);
    PIECE_ORDER.put("Q", 8);
    PIECE_ORDER.put("q", 9);
    PIECE_ORDER.put("R", 10);
    PIECE_ORDER.put("r", 11);
  }
  
  private int _initialFixationThreshold = 4;
  private final int _fixationPeripheryMaxAttempts;
  
  private int _timeTakenToDecideOnGlobalStrategyFixations;
  private int _timeTakenToDecideOnSalientManFixations;
  
  /**
   * @param definition Should be the definition of a full chess board in <a 
   * href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">FEN
   * </a> notation. Empty squares should be indicated using a full stop (.); 
   * counts of empty squares not permitted.
   * 
   * @return A {@link jchrest.domainSpecifics.Scene} representation of the {@code 
   * definition} provided with the first character being positioned at the 
   * top-left of the {@link jchrest.domainSpecifics.Scene} (coordinates [0, 7]) and the last 
   * character being positioned at the bottom-right of the {@link 
   * jchrest.domainSpecifics.Scene} (coordinates [7, 0]).
   */
  public static Scene constructBoard (String definition) {
    if(!definition.matches("([p,n,b,r,q,k,P,N,B,R,Q,K,\\.]{8}\\/){7}[p,n,b,r,q,k,P,N,B,R,Q,K,\\.]{8}")){
      throw new IllegalArgumentException(
        "Board definition provided violates allowed format: " +
        "\n - Each line must only contain 8 of the following characters: 'p', " +
        "'n', 'b', 'r', 'q', 'k', 'P', 'N', 'B', 'R', 'Q', 'K' or '.'" +
        "\n - The first 7 groups of the 8 characters above must end with '/'" +
        "\n - The last group of the 8 characters above must not end with '/'" +
        "\n\n Board defintion provided: " + definition
      );
    }
    
    ChessBoard board = new ChessBoard("chess-board");

    for (int col = 0; col < 8; ++col) {
      for (int row = 0; row < 8; ++row) {
        String pieceType = definition.substring(col + 9*row, 1 + col + 9*row);
        ChessObject piece = new ChessObject(pieceType);
        
        board.addObjectToSquare(col, Math.abs((row + 1) - 8), piece);
      }
    }

    return board;
  }
  
  /**
   * @param board
   * 
   * @return The set of "big" {@link jchrest.domainSpecifics.chess.ChessObject
   * pieces} in the {@link jchrest.domainSpecifics.chess.ChessBoard} specified, 
   * i.e. any non-empty/blind {@link jchrest.domainSpecifics.chess.ChessObject} 
   * other than a pawn.
   */
  public static List<ItemSquarePattern> getBigPieces(ChessBoard board) {
    List<ItemSquarePattern> result = new ArrayList();

    for (int col = 0; col < board.getWidth(); ++col) {
      for (int row = 0; row < board.getHeight(); ++row) {
        if (!board.isSquareEmpty(col, row) && !board.isSquareBlind(col, row)) {
          ListPattern itemsOnSquare = board.getSquareContentsAsListPattern(col, row);
          for(PrimitivePattern itemOnSquare : itemsOnSquare){
            ItemSquarePattern ios = (ItemSquarePattern)itemOnSquare;
            if( !ios.getItem().equalsIgnoreCase("P") ){
              result.add(ios);
            }
          }
        }
      }
    }

    return result;
  }

  /**
   * @param board
   * 
   * @return The set of "offensive" {@link 
   * jchrest.domainSpecifics.chess.ChessObject pieces} in the {@link 
   * jchrest.domainSpecifics.chess.ChessBoard} specified, 
   * i.e. any non-empty/blind {@link jchrest.domainSpecifics.chess.ChessObject
   * pieces} whose {@link jchrest.domainSpecifics.chess.ChessObject#getColour()}
   * does not match the side of the {@link 
   * jchrest.domainSpecifics.chess.ChessBoard} whose coordinates they are 
   * currently found upon (a black piece on white's side of the board, for 
   * example).
   */
  public static List<ItemSquarePattern> getOffensivePieces(ChessBoard board) {
    List<ItemSquarePattern> result = new ArrayList();

    for (int col = 0; col < board.getWidth (); ++col) {
      for (int row = 0; row < board.getHeight (); ++row) {
        if(
          !board.isSquareBlind(col, row) &&
          !board.isSquareEmpty(col, row) 
        ){
          ChessObject piece = board.getSquareContents(col, row);
          
          // black piece on white side
          if(piece.getColour().equals(Color.BLACK) && row <= 3) { 
            result.add(new ItemSquarePattern(piece.getObjectType(), col, row));
          }
          // white piece on black side
          else if(piece.getColour().equals(Color.WHITE) && row >= 4) { 
            result.add(new ItemSquarePattern(piece.getObjectType(), col, row));
          }
        }
      }
    }

    return result;
  }
  
  /**
   * @param board
   * @param square1 
   * @param square2 
   * 
   * @return {@link java.lang.Boolean#TRUE} if the result of invoking {@link 
   * jchrest.domainSpecifics.chess.ChessObject#getColour()} on the {@link 
   * jchrest.domainSpecifics.chess.ChessObject ChessObjects} present on
   * {@code square1} and {@code square2} are not {@code null} and are equal, 
   * {@link java.lang.Boolean#FALSE} if not.
   */
  public static boolean differentColour(ChessBoard board, Square square1, Square square2){
    Color item1Colour = board.getSquareContents(square1.getColumn(), square1.getRow()).getColour();
    Color item2Colour = board.getSquareContents(square2.getColumn(), square2.getRow()).getColour();
    return item1Colour != null && item2Colour != null && !item1Colour.equals(item2Colour);
  }
  
  /**
   * @param board
   * @param startSquare The {@link jchrest.lib.Square} to find line movements 
   * from.  This must contain a non-empty {@link 
   * jchrest.domainSpecifics.chess.ChessObject}.
   * @param rowDelta The value to increment the row by after checking a {@link 
   * jchrest.lib.Square} (defines the y-component of the line).  Should be -1, 
   * 0 or 1 so that a line may only be drawn in each cardinal and primary 
   * inter-cardinal compass direction. 
   * @param colDelta The value to increment the column by after checking a 
   * {@link jchrest.lib.Square} (defines the x-component of the line). Should be
   * -1, 0 or 1 so that a line may only be drawn in each cardinal and primary 
   * inter-cardinal compass direction.
   * 
   * @return A two-element {@link java.util.Arrays Array} whose first element is
   * a {@link java.util.List} of {@link jchrest.lib.Square Squares} that can be
   * moved to along the line and whose second element is the number of {@link 
   * jchrest.lib.Square Squares} considered when calculating what {@link 
   * jchrest.lib.Square Squares} can be moved to.
   * 
   * The line specified by {@code rowDelta} and {@code colDelta} will be 
   * followed until the edge of the {@code board} specified is reached or 
   * another chess piece of any colour is encountered.  If a piece is 
   * encountered and it is of a different colour, the {@link jchrest.lib.Square}
   * that the differently coloured piece is on is added to the {@link 
   * java.util.List} of {@link jchrest.lib.Square Squares} returned.
   * 
   * <b>NOTE:</b> If a king of an opposing colour is encountered, this function
   * will consider this as a standard capture.
   */
  static Object[] lineMove(ChessBoard board, Square startSquare, int colDelta, int rowDelta) {
    if(colDelta < -1 || colDelta > 1 || rowDelta < -1 || rowDelta > 1){
      throw new IllegalArgumentException("The col/row delta specified is < -1 or > 1");
    }
    
    String objectOnSquareClass = board.getSquareContents(startSquare.getColumn(), startSquare.getRow()).getObjectType();
    if(objectOnSquareClass.equals(Scene.getEmptySquareToken())){
      throw new IllegalArgumentException("The start square specified " + startSquare.toString() + " is empty");
    }
    
    Object[] result = new Object[2];
    List<Square> potentialSquaresToMoveTo = new ArrayList();
    int squaresConsidered = 0;
    
    int tryCol = startSquare.getColumn() + colDelta;
    int tryRow = startSquare.getRow() + rowDelta;
    boolean otherPieceEncountered = false;
    
    while(!otherPieceEncountered && tryCol >= 0 && tryCol <= 7 && tryRow >=0 && tryRow <= 7){
      Square destination = new Square (tryCol, tryRow);
      squaresConsidered++;
      
      if( board.isSquareEmpty(destination.getColumn(), destination.getRow()) ){
        potentialSquaresToMoveTo.add(destination);
        tryCol += colDelta;
        tryRow += rowDelta;
      } else {
        otherPieceEncountered = true;
        if( differentColour(board, startSquare, destination) ){
          potentialSquaresToMoveTo.add(destination);
        } 
      }
    }
    
    result[0] = potentialSquaresToMoveTo;
    result[1] = squaresConsidered;
    return result;
  }

  /**
   * 
   * @param board
   * @param source Must contain a non-empty {@link 
   * jchrest.domainSpecifics.chess.ChessObject}.
   * @param destination
   * @return {@link java.lang.Boolean#TRUE} if the {@code destination} is empty
   * or could result in a capture, i.e. if either of the following statements
   * are {@link java.lang.Boolean#TRUE}, {@link java.lang.Boolean#FALSE} 
   * otherwise:
   * 
   * <ol type="1">
   *    <li>
   *      {@link jchrest.lib.Scene#isSquareEmpty(int, int)} returns {@link 
   *      java.lang.Boolean#TRUE} in context of the {@code board} and {@code 
   *      destination} specified.
   *    </li>
   *    <li>
   *      {@link #this#differentColour(jchrest.lib.Scene, jchrest.lib.Square, 
   *      jchrest.lib.Square)} returns {@link java.lang.Boolean#TRUE} in context 
   *      of the {@code board}, {@code source} and {@code destination} 
   *      specified.
   *    </li>
   * </ol>
   * 
   * <b>NOTE:</b> this function does not check if the <i>movement</i> of the 
   * piece from the {@code source} to the {@code destination} is valid according
   * to the result of invoking {@link 
   * jchrest.domainSpecifics.chess.ChessObject#getObjectType()} on the {@link 
   * jchrest.domainSpecifics.chess.ChessObject} present on {@code source}, i,e.
   * if {@code source} contains a queen and {@code desitination} can only be
   * reached using a knight movement but {@code destination} is empty or 
   * contains a {@link jchrest.domainSpecifics.chess.ChessObject} of a different
   * colour to the queen, this function will return {@link 
   * java.lang.Boolean#TRUE}.
   */
  public static boolean validMove(ChessBoard board, Square source, Square destination){
    if(board.getSquareContents(source.getColumn(), source.getRow()).getObjectType().equals(Scene.getEmptySquareToken())){
      throw new IllegalArgumentException("The source square specified " + source + " is empty");
    }
    return board.isSquareEmpty(destination.getColumn(), destination.getRow()) || ChessDomain.differentColour(board, source, destination);
  }
  
  /**
   * @param board 
   * @param square Must contain a black/white pawn {@link 
   * jchrest.domainSpecifics.chess.ChessObject}.
   * 
   * @return A two-element {@link java.util.Arrays Array} whose first element is
   * a {@link java.util.List} of {@link jchrest.lib.Square Squares} that the 
   * pawn can move to and whose second element is the number of {@link 
   * jchrest.lib.Square Squares} considered when calculating where the pawn 
   * could be moved to.
   * 
   * The function will check if the pawn can:
   * 
   * <ul>
   *    <li>
   *      Make a standard move: is the {@link jchrest.lib.Square} that's 1 ahead
   *      of the {@code square} specified empty? 
   *    </li>
   *    <li>
   *      Make an initial move: is the {@link jchrest.lib.Square} that's 2 ahead
   *      of the {@code square} specified empty? 
   *    </li>
   *    <li>
   *      Capture any pieces: are the squares to the immediate diagonal 
   *      left/right of the {@code square} specified occupied by white pieces 
   *      (note that en-passant captures are ignored)?
   *    </li>
   * </ul>
   */
  public static Object[] getPawnMoves(ChessBoard board, Square square){
    ChessObject squareContents = board.getSquareContents(square.getColumn(), square.getRow());
    
    if(!squareContents.getObjectType().equalsIgnoreCase("p")){
      throw new IllegalArgumentException(
        "The square specified " + square.toString() + " does not contain a pawn"
      );
    }
    
    Object[] result = new Object[2];
    List<Square> moves = new ArrayList();
    int squaresConsidered = 0;
    boolean blackPawn = squareContents.getColour().equals(Color.BLACK);

    //Check for standard move
    squaresConsidered++;
    int colToCheck = square.getColumn(); 
    int rowToCheck = (blackPawn ? square.getRow() - 1 : square.getRow() + 1);
    if( board.getSquareContents(colToCheck, rowToCheck) != null && board.isSquareEmpty(colToCheck, rowToCheck) ){
      moves.add(new Square (square.getColumn(), blackPawn ? square.getRow() - 1 : square.getRow() + 1));
      
      //Check for an initial move now that a standard move is possible.
      squaresConsidered++;
      if(
        (blackPawn ? square.getRow() == 6 : square.getRow() == 1) && 
        board.isSquareEmpty(square.getColumn(), blackPawn ? square.getRow() - 2 : square.getRow() + 2)
      ){
        moves.add(new Square(square.getColumn(), blackPawn ? square.getRow() - 2 : square.getRow() + 2));
      }
    }

    //Check for captures.
    if(square.getColumn() > 0){ // not in column a
      squaresConsidered++;
      Square destination = new Square(square.getColumn() - 1, blackPawn ? square.getRow() - 1 : square.getRow() + 1);
      if(board.getSquareContents(destination.getColumn(), destination.getRow()) != null && differentColour(board, square, destination)){
        moves.add(destination);
      }
    }
    if(square.getColumn() < 7){ // not in column h
      squaresConsidered++;
      Square destination = new Square (square.getColumn() + 1, blackPawn ? square.getRow() - 1 : square.getRow() + 1);
      if(board.getSquareContents(destination.getColumn(), destination.getRow()) != null && differentColour(board, square, destination)){
        moves.add(destination);
      }
    }

    result[0] = moves;
    result[1] = squaresConsidered;
    return result;
  }
  
  /**
   * @param board 
   * @param square Must contain a knight {@link 
   * jchrest.domainSpecifics.chess.ChessObject}.
   * 
   * @return A two-element {@link java.util.Arrays Array} whose first element is
   * a {@link java.util.List} of {@link jchrest.lib.Square Squares} that the 
   * knight can move to and whose second element is the number of {@link 
   * jchrest.lib.Square Squares} considered when calculating what {@link 
   * jchrest.lib.Square Squares} can be moved to.
   * 
   * The function will check if the {@link jchrest.lib.Square Squares} that the
   * knight can move to are empty or are occupied by pieces of a different 
   * colour, i.e. whether it can capture any pieces.
   */
  public static Object[] getKnightMoves(ChessBoard board, Square square){
    ChessObject squareContents = board.getSquareContents(square.getColumn(), square.getRow());
    
    if(!squareContents.getObjectType().equalsIgnoreCase("n")){
      throw new IllegalArgumentException(
        "The square specified " + square.toString() + " does not contain a knight"
      );
    }
    
    Object[] result = new Object[2];
    List<Square> moves = new ArrayList();
    int squaresTraversed = 0;

    //Check for left/right 1 + up 2
    if(square.getRow() < 6){ // not rows 7 or 8
      if(square.getColumn() > 0){ // not column a
        squaresTraversed += 3;
        Square destination = new Square(square.getColumn() - 1, square.getRow() + 2);
        if( ChessDomain.validMove(board, square, destination) ) moves.add(destination);
      }
      if(square.getColumn() < 7){ // not column h
        squaresTraversed += 3;
        Square destination = new Square(square.getColumn() + 1, square.getRow() + 2);
        if( ChessDomain.validMove(board, square, destination) ) moves.add(destination);
      }
    }

    //Check for left/right 1 + down 2
    if(square.getRow() > 1){ // not rows 1 or 2
      if(square.getColumn() > 0){ // not column a
        squaresTraversed += 3;
        Square destination = new Square(square.getColumn() - 1, square.getRow() - 2);
        if( validMove(board, square, destination) ) moves.add(destination);
      }
      if(square.getColumn() < 7){ // not column h
        squaresTraversed += 3;
        Square destination = new Square(square.getColumn() + 1, square.getRow() - 2);
        if( validMove(board, square, destination) ) moves.add(destination);
      }
    }
    
    //Check for right 2 + up/down 1
    if(square.getColumn() < 6){ // not columns g or h
      if(square.getRow() > 0){ // not row 1
        squaresTraversed += 3;
        Square destination = new Square(square.getColumn() + 2, square.getRow() - 1);
        if( validMove(board, square, destination) ) moves.add(destination);
      }
      if(square.getRow() < 7){ // not row 8
        squaresTraversed += 3;
        Square destination = new Square(square.getColumn() + 2, square.getRow() + 1);
        if( validMove(board, square, destination) ) moves.add(destination);
      }
    }

    //Check for left 2 + up/down 1
    if(square.getColumn() > 1){ // not columns a or b
      if(square.getRow() > 0){ // not row 1
        squaresTraversed += 3;
        Square destination = new Square(square.getColumn() - 2, square.getRow() - 1);
        if( validMove(board, square, destination) ) moves.add(destination);
      }
      if(square.getRow() < 7){ // not row 8
        squaresTraversed += 3;
        Square destination = new Square(square.getColumn() - 2, square.getRow() + 1);
        if( validMove(board, square, destination) ) moves.add(destination);
      }
    }

    result[0] = moves;
    result[1] = squaresTraversed;
    return result;
  }

  /**
   * @param board 
   * @param square Must contain a king {@link 
   * jchrest.domainSpecifics.chess.ChessObject}.
   * 
   * @return A two-element {@link java.util.Arrays Array} whose first element is
   * a {@link java.util.List} of {@link jchrest.lib.Square Squares} that the 
   * king can move to and whose second element is the number of {@link 
   * jchrest.lib.Square Squares} considered when calculating what {@link 
   * jchrest.lib.Square Squares} can be moved to.
   * 
   * The function will check if the {@link jchrest.lib.Square Squares} that the
   * king can move to are empty or are occupied by pieces of a different 
   * colour, i.e. whether it can capture any pieces.
   * 
   * <b>NOTE:</b> this function does not check if the {@link jchrest.lib.Square}
   * moved to will put the king in check, if it is 1 square from a king of the
   * opposing colour or if a capture may result in the king moving to a {@link 
   * jchrest.lib.Square} occupied by a king {@link 
   * jchrest.domainSpecifics.chess.ChessObject} of the opposing colour.
   */
  public static Object[] getKingMoves(ChessBoard board, Square square) {
    ChessObject squareContents = board.getSquareContents(square.getColumn(), square.getRow());
    
    if(!squareContents.getObjectType().equalsIgnoreCase("k")){
      throw new IllegalArgumentException(
        "The square specified " + square.toString() + " does not contain a king"
      );
    }
    
    Object[] result = new Object[2];
    List<Square> moves = new ArrayList();
    int squaresTraversed = 0;

    if(square.getRow() > 0){ // not in row 8
      squaresTraversed++;
      Square destination = new Square(square.getColumn(), square.getRow() - 1);
      if( validMove(board, square, destination) ) moves.add(destination);
    }
    if(square.getRow() < 7){ // not in row 1
      squaresTraversed++;
      Square destination = new Square(square.getColumn(), square.getRow() + 1);
      if( validMove(board, square, destination) ) moves.add(destination);
    }
    if(square.getColumn() > 0){ // not in column 1
      squaresTraversed++;
      Square destination = new Square(square.getColumn() - 1, square.getRow());
      if( validMove(board, square, destination) ) moves.add(destination);
    }
    if(square.getColumn() < 7){ // not in column 8
      squaresTraversed++;
      Square destination = new Square(square.getColumn() + 1, square.getRow());
      if( validMove(board, square, destination) ) moves.add(destination);
    }
    if(square.getRow() > 0 && square.getColumn() > 0){ // not in row 8 or column 1
      squaresTraversed++;
      Square destination = new Square(square.getColumn() - 1, square.getRow() - 1);
      if( validMove(board, square, destination) ) moves.add(destination);
    }
    if(square.getRow() > 0 && square.getColumn() < 7){ // not in row 8 or column 8
      squaresTraversed++;
      Square destination = new Square(square.getColumn() + 1, square.getRow() - 1);
      if( validMove(board, square, destination) ) moves.add(destination);
    }
    if(square.getRow() < 7 && square.getColumn() > 0){ // not in row 1 or column 1
      squaresTraversed++;
      Square destination = new Square(square.getColumn() - 1, square.getRow() + 1);
      if( validMove(board, square, destination) ) moves.add(destination);
    }
    if(square.getRow() < 7 && square.getColumn() < 7){ // not in row 8 or column 8
      squaresTraversed++;
      Square destination = new Square(square.getColumn() + 1, square.getRow() + 1);
      if( validMove(board, square, destination) ) moves.add(destination);
    }

    result[0] = moves;
    result[1] = squaresTraversed;
    return result;
  }

  /**
   * @param board 
   * @param square Must contain a queen {@link 
   * jchrest.domainSpecifics.chess.ChessObject}.
   * 
   * @return A two-element {@link java.util.Arrays Array} whose first element is
   * a {@link java.util.List} of {@link jchrest.lib.Square Squares} that the 
   * queen can move to and whose second element is the number of {@link 
   * jchrest.lib.Square Squares} considered when calculating what {@link 
   * jchrest.lib.Square Squares} can be moved to (done by getting the result of 
   * passing delta parameters to {@link 
   * #this#lineMove(jchrest.domainSpecifics.chess.ChessBoard, 
   * jchrest.lib.Square, int, int) that specify cardinal and primary 
   * inter-cardinal compass lines).
   */
  public static Object[] getQueenMoves(ChessBoard board, Square square){
    ChessObject squareContents = board.getSquareContents(square.getColumn(), square.getRow());
    
    if(!squareContents.getObjectType().equalsIgnoreCase("q")){
      throw new IllegalArgumentException(
        "The square specified " + square.toString() + " does not contain a queen"
      );
    }
    
    Object[] result = new Object[2];
    List<Square> moves = new ArrayList();
    int squaresTraversed = 0;
    Object[][] movesData = new Object[][]{
      lineMove(board, square, 0, 1), //North movement
      lineMove(board, square, +1, +1), //North-east movement
      lineMove(board, square, +1, 0), //East movement
      lineMove(board, square, +1, -1), //South-east movement
      lineMove(board, square, 0, -1), //South movement
      lineMove(board, square, -1, -1), //South-west movement
      lineMove(board, square, -1, 0), //West movement
      lineMove(board, square, -1, +1) //North-west movement
    };
    
    for(int i = 0; i < movesData.length; i++){
      Object[] moveData = movesData[i];
      moves.addAll((List<Square>)moveData[0]);
      squaresTraversed += (int)moveData[1];
    }

    result[0] = moves;
    result[1] = squaresTraversed;
    return result;
  }

  /**
   * @param board 
   * @param square Must contain a rook {@link 
   * jchrest.domainSpecifics.chess.ChessObject}.
   * 
   * @return A two-element {@link java.util.Arrays Array} whose first element is
   * a {@link java.util.List} of {@link jchrest.lib.Square Squares} that the 
   * rook can move to and whose second element is the number of {@link 
   * jchrest.lib.Square Squares} considered when calculating what {@link 
   * jchrest.lib.Square Squares} can be moved to (done by getting the result of 
   * passing delta parameters to {@link 
   * #this#lineMove(jchrest.domainSpecifics.chess.ChessBoard, 
   * jchrest.lib.Square, int, int) that specify cardinal compass lines).
   */
  public static Object[] getRookMoves(ChessBoard board, Square square){
    ChessObject squareContents = board.getSquareContents(square.getColumn(), square.getRow());
    
    if(!squareContents.getObjectType().equalsIgnoreCase("r")){
      throw new IllegalArgumentException(
        "The square specified " + square.toString() + " does not contain a rook"
      );
    }
    
    Object[] result = new Object[2];
    List<Square> moves = new ArrayList();
    int squaresTraversed = 0;
    Object[][] movesData = new Object[][]{
      lineMove(board, square, 0, 1), //North movement
      lineMove(board, square, +1, 0), //East movement
      lineMove(board, square, 0, -1), //South movement
      lineMove(board, square, -1, 0), //West movement
    };
    
    for(int i = 0; i < movesData.length; i++){
      Object[] moveData = movesData[i];
      moves.addAll((List<Square>)moveData[0]);
      squaresTraversed += (int)moveData[1];
    }

    result[0] = moves;
    result[1] = squaresTraversed;
    return result;
  }

  /**
   * @param board 
   * @param square Must contain a bishop {@link 
   * jchrest.domainSpecifics.chess.ChessObject}.
   * 
   * @return A two-element {@link java.util.Arrays Array} whose first element is
   * a {@link java.util.List} of {@link jchrest.lib.Square Squares} that the 
   * bishop can move to and whose second element is the number of {@link 
   * jchrest.lib.Square Squares} considered when calculating what {@link 
   * jchrest.lib.Square Squares} can be moved to (done by getting the result of 
   * passing delta parameters to {@link 
   * #this#lineMove(jchrest.domainSpecifics.chess.ChessBoard, 
   * jchrest.lib.Square, int, int) that specify primary inter-cardinal compass 
   * lines).
   */
  public static Object[] getBishopMoves(ChessBoard board, Square square){
    ChessObject squareContents = board.getSquareContents(square.getColumn(), square.getRow());
    
    if(!squareContents.getObjectType().equalsIgnoreCase("b")){
      throw new IllegalArgumentException(
        "The square specified " + square.toString() + " does not contain a bishop"
      );
    }
    
    Object[] result = new Object[2];
    List<Square> moves = new ArrayList();
    int squaresTraversed = 0;
    Object[][] movesData = new Object[][]{
      lineMove(board, square, +1, +1), //North-east movement
      lineMove(board, square, +1, -1), //South-east movement
      lineMove(board, square, -1, -1), //South-west movement
      lineMove(board, square, -1, +1) //North-west movement
    };
    
    for(int i = 0; i < movesData.length; i++){
      Object[] moveData = movesData[i];
      moves.addAll((List<Square>)moveData[0]);
      squaresTraversed += (int)moveData[1];
    }

    result[0] = moves;
    result[1] = squaresTraversed;
    return result;
  }
  
  /**
   * Constructor.
   * 
   * @param model The {@link jchrest.architecture.Chrest} model that is 
   * situated in a chess domain.
   * 
   * @param initialFixationThreshold The number of {@link 
   * jchrest.domainSpecifics.Fixation Fixations} that can be made by the {@link 
   * jchrest.architecture.Chrest} model constructing {@link #this} before it is
   * no longer considered to be making initial {@link 
   * jchrest.domainSpecifics.Fixation Fixations} in a set (see {@link 
   * #this#doneInitialFixations(int)}).  
   * 
   * If the value specified for this parameter is {@code null}, a value of 4 is 
   * used since this is the default value for this parameter as specified in the 
   * "Salient-Man" fixation heuristic description in section 8.7.6 of 
   * "Perception and Memory in Chess" by de Groot and Gobet.
   * 
   * If the value specified for this parameter is not {@code null}, it must be
   * greater than 0 otherwise a {@link java.lang.IllegalArgumentException} will
   * be thrown.
   * 
   * @param fixationPeripheryMaxAttempts If a {@link 
   * jchrest.domainSpecifics.fixations.PeripheralItemFixation} is attempted, 
   * this parameter specified how many attempts will be made to find a suitable
   * {@link jchrest.lib.Square} in the periphery before attempting the {@link 
   * jchrest.domainSpecifics.fixations.PeripheralItemFixation} is abandoned (see
   * {@link jchrest.domainSpecifics.fixations.PeripheralItemFixation#PeripheralItemFixation(
   * jchrest.architecture.Chrest, int, int)}).
   * 
   * @param maxFixationsInSet When making a set of {@link 
   * jchrest.domainSpecifics.Fixation Fixations}, this parameter defines how 
   * many can be attempted in total before a new set is initialised (see {@link 
   * #this#shouldClearFixations(int)}.
   * 
   * @param timeTakenToDecideOnGlobalStrategyFixations
   * @param timeTakenToDecideOnSalientManFixations
   */
  public ChessDomain(
    Chrest model, 
    Integer initialFixationThreshold, 
    int fixationPeripheryMaxAttempts, 
    int maxFixationsInSet,
    int timeTakenToDecideOnGlobalStrategyFixations,
    int timeTakenToDecideOnSalientManFixations
  ) {
    super(model, maxFixationsInSet);
    
    if(initialFixationThreshold != null){
      if(initialFixationThreshold > 0){
        this._initialFixationThreshold = initialFixationThreshold;
      }
      else{
        throw new IllegalArgumentException(
          "The initial fixation threshold specified as a parameter to the " + 
          this.getClass().getCanonicalName() + " constructor (" + 
          initialFixationThreshold + ") is <= 0."
        );
      }
    }
    else{
      this._initialFixationThreshold = 4;
    }
    
    if(fixationPeripheryMaxAttempts > 0){
      this._fixationPeripheryMaxAttempts = fixationPeripheryMaxAttempts;
    }
    else{
      throw new IllegalArgumentException(
        "The maximum number of attempts to make a fixation on an item " +
        "in the periphery specified as a parameter to the " + 
        this.getClass().getCanonicalName() + " constructor (" + 
        fixationPeripheryMaxAttempts + ") is <= 0."
      );
    }
    
    if(maxFixationsInSet < this._initialFixationThreshold){
      throw new IllegalArgumentException(
        "The maximum number of fixations to make in a set specified as a " +
        "parameter to the " + this.getClass().getCanonicalName() + " " +
        "constructor (" + maxFixationsInSet + ") is < the initial fixation " +
        "threshold specified (" + this._initialFixationThreshold + ")."
      );
    }
    
    //Set time taken to decide upon global strategy/salient man fixations
    if(timeTakenToDecideOnGlobalStrategyFixations < 0 || timeTakenToDecideOnSalientManFixations < 0){
      throw new IllegalArgumentException(
        "One or both of the times taken to decide on global strategy or salient " +
        "man fixations is < 0 (time specified to decide on global strategy fixations: " +
        timeTakenToDecideOnGlobalStrategyFixations + ", time specified to decide on " +
        "salient man fixations: " + timeTakenToDecideOnSalientManFixations +
        ")."
      );
    }
    else{
      this._timeTakenToDecideOnGlobalStrategyFixations = timeTakenToDecideOnGlobalStrategyFixations;
      this._timeTakenToDecideOnSalientManFixations = timeTakenToDecideOnSalientManFixations;
    }
  }

  /**
   * @param pattern
   * 
   * @return The {@link jchrest.lib.ListPattern} specified after sorting its
   * constituent {@link jchrest.lib.ItemSquarePattern ItemSquarePatterns} into a 
   * canonical order of chess pieces, as defined in section 8.7.1, paragraph 
   * "C: Comparison with other models." in "Perception and Memory in Chess" by 
   * de Groot and Gobet (1996).  This order is: P p K k B b N n Q q R r.  If the 
   * pieces are the same, then order is based on column, and then on row.
   */
  @Override
  public ListPattern normalise(ListPattern pattern){
    ListPattern result = new ListPattern (pattern.getModality()); 
    
    //Remove blind and empty squares.
    pattern = pattern.removeBlindEmptyAndUnknownItems();
    
    //Remove any duplicates
    for(PrimitivePattern prim : pattern) {
      if(!result.contains (prim)){
        result = result.append (prim);
      }
    }
    
    //Sort into canonical order
    result = result.sort (new Comparator<PrimitivePattern> () {
      @Override
      public int compare (PrimitivePattern left, PrimitivePattern right) {
        assert (left instanceof ItemSquarePattern);
        assert (right instanceof ItemSquarePattern);
        ItemSquarePattern leftIos = (ItemSquarePattern)left;
        ItemSquarePattern rightIos = (ItemSquarePattern)right;

        // check item
        if (PIECE_ORDER.get(leftIos.getItem()) < PIECE_ORDER.get (rightIos.getItem ())) return -1;
        if (PIECE_ORDER.get(leftIos.getItem()) > PIECE_ORDER.get (rightIos.getItem ())) return 1;
        // check column
        if (leftIos.getColumn() < rightIos.getColumn()) return -1;
        if (leftIos.getColumn() > rightIos.getColumn()) return 1;
        // check row
        if (leftIos.getRow() < rightIos.getRow()) return -1;
        if (leftIos.getRow() > rightIos.getRow()) return 1;
        return 0;
      }
    });
    
    return result;
  }
  
  /**
   * @param time
   * 
   * @return A {@link jchrest.domainSpecifics.fixations.CentralFixation} whose
   * {@link jchrest.domainSpecifics.Fixation#getTimeDecidedUpon()} will return
   * the {@code time} specified plus the value returned by invoking {@link j
   * chrest.architecture.Chrest#getTimeTakenToDecideUponCentralFixation()} in 
   * context of {@link #this#_associatedModel}.
   */
  @Override
  public Fixation getInitialFixationInSet(int time){
    return new CentralFixation(time, this._associatedModel.getTimeTakenToDecideUponCentralFixations());
  }
  
  /**
   * @param time
   * @return A new {@link jchrest.domainSpecifics.Fixation} whose type is 
   * determined by comparing <i>n</i> (the sum of the number of {@link 
   * jchrest.domainSpecifics.Fixation Fixations} to make and the number of {@link 
   * jchrest.domainSpecifics.Fixation Fixations} attempted at the {@code time} 
   * specified) to <i>t</i> (the initial {@link 
   * jchrest.domainSpecifics.Fixation} threshold specified as a parameter to 
   * {@link #this#TileworldDomain(jchrest.architecture.Chrest, int, int, int)}):
   * 
   * <ol type="1">
   *  <li>
   *    If <i>n</i> &lt; <i>t</i> a {@link 
   *    jchrest.domainSpecifics.chess.fixations.SalientManFixation} is returned.
   *  </li>
   *  <li>
   *    If <i>n</i> &gt;&#61; <i>t</i>, the {@link 
   *    jchrest.domainSpecifics.Fixation Fixations} scheduled to be performed 
   *    and the most recent {@link jchrest.domainSpecifics.Fixation} attempted
   *    by the {@link jchrest.architecture.Chrest} model using {@link #this} are
   *    retrieved in context of the {@code time} specified. 
   *    <ol type="1">
   *      <li>
   *        If there is no {@link 
   *        jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation} 
   *        scheduled to be performed or the most recently attempted {@link 
   *        jchrest.domainSpecifics.Fixation} was a {@link 
   *        jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation} 
   *        whose performance was unsuccessful, a {@link 
   *        jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation} 
   *        is returned.
   *      </li>
   *      <li>
   *        If there is a {@link 
   *        jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation} 
   *        scheduled to be performed or the most recently attempted {@link 
   *        jchrest.domainSpecifics.Fixation} was a {@link 
   *        jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation} 
   *        that was performed successfully, one of the following {@link 
   *        jchrest.domainSpecifics.Fixation Fixations} are returned with equal
   *        probability:
   *        <ul>
   *          <li>{@link jchrest.domainSpecifics.chess.fixations.AttackDefenseFixation}</li>
   *          <li>{@link jchrest.domainSpecifics.fixations.PeripheralSquareFixation}</li>
   *          <li>
   *            Get <i>e</i> (the result of invoking {@link 
   *            jchrest.architecture.Chrest#isExperienced(int)} in context of 
   *            the {@link jchrest.architecture.Chrest} model using {@link 
   *            #this}).
   *            <ul>
   *              <li>
   *                If <i>e</i> is {@link java.lang.Boolean#TRUE}, return a
   *                {@link jchrest.domainSpecifics.chess.fixations.GlobalStrategyFixation}.
   *              </li>
   *              <li>
   *                If <i>e</i> is {@link java.lang.Boolean#FALSE}, return a
   *                {@link jchrest.domainSpecifics.fixations.PeripheralItemFixation}.
   *              </li>
   *            </ul>
   *          </li>
   *        </ul>
   *      </li>
   *    </ol>
   *  </li>
   * </ol>
   */
  @Override
  public Fixation getNonInitialFixationInSet(int time){
    List<Fixation> fixationsScheduled = this._associatedModel.getScheduledFixations(time);
    List<Fixation> fixationsAttempted = this._associatedModel.getPerceiver().getFixations(time);
    int numberFixationsToMake = (fixationsScheduled == null ? 0 : fixationsScheduled.size());
    int numberFixationsAttempted = (fixationsAttempted == null ? 0 : fixationsAttempted.size());
    
    if((numberFixationsToMake + numberFixationsAttempted) < this._initialFixationThreshold){
      return new SalientManFixation(this._associatedModel, time, this._timeTakenToDecideOnSalientManFixations);
    }
    else{
      
      //In this case, a HypothesisDiscriminationFixation should always be 
      //attempted unless:
      //
      // 1. There is such a Fixation already being deliberated on but hasn't 
      //    been performed yet.
      // 2. The most recent Fixation attempted was such a Fixation but wasn't
      //    performed.
      //
      //In the first case, the outcome of attempting to make the 
      //HypothesisDiscriminationFixation is unknown so instead of generating 
      //another which may fail again (essentially wasting a Fixation), generate
      //another type of Fixation.
      //
      //In the second case, the outcome of attempting to make a 
      //HypothesisDiscriminationFixation is known and the attempt was 
      //unsuccessful so other Fixations need to be made to try and replace the
      //current visual STM hypothesis since its information is not useful in the
      //current Scene.
      //
      //To perform these checks, get the Fixations currently being deliberated
      //on and the Fixations performed up until the time specified by the 
      //CHREST model associated with this domain.  NOTE: there is no need to 
      //check for whether the Lists returned are null or empty since this will 
      //have been checked when doneInitialFixations() is called in the "if" part 
      //of the conditional surrounding this block.
      
      //Check for a HypothesisDiscriminationFixation currently being decided 
      //upon.
      boolean hypothesisDiscriminationFixationBeingDeliberatedOn = false;
      for(Fixation fixation : fixationsScheduled){
        if(fixation.getClass().equals(HypothesisDiscriminationFixation.class)){
          hypothesisDiscriminationFixationBeingDeliberatedOn = true;
          break;
        }
      }
      
      //Check for a recent attempt at a HypothesisDiscriminationFixation that
      //failed.
      boolean mostRecentFixationAttemptedFailedAndWasHDF = false;
      Fixation mostRecentFixationAttempted = null;
      if(fixationsAttempted != null){
        mostRecentFixationAttempted = fixationsAttempted.get(fixationsAttempted.size() - 1);
        mostRecentFixationAttemptedFailedAndWasHDF = (
          !mostRecentFixationAttempted.hasBeenPerformed() && 
          mostRecentFixationAttempted.getClass().equals(HypothesisDiscriminationFixation.class)
        );
      }
      
      if(
        hypothesisDiscriminationFixationBeingDeliberatedOn ||
        (
          !hypothesisDiscriminationFixationBeingDeliberatedOn &&
          mostRecentFixationAttemptedFailedAndWasHDF
        )
      ){
        
        Fixation fixation = null;
        while(fixation == null){
          double r = Math.random();
          
          if(r < 0.3333){
            fixation = new AttackDefenseFixation(this._associatedModel, (ChessBoard)mostRecentFixationAttempted.getScene(), time);
          }
          else if(r >= 0.3333 && r < 0.6667) {
            if(this._associatedModel.isExperienced(time)){
              fixation = new GlobalStrategyFixation(this._associatedModel, time, this._timeTakenToDecideOnGlobalStrategyFixations);
            }
            else{
              fixation = new PeripheralItemFixation(this._associatedModel, this._fixationPeripheryMaxAttempts, time, this._associatedModel.getTimeTakenToDecideUponPeripheralItemFixations());
            }
          }
          else{
            fixation = new PeripheralSquareFixation(this._associatedModel, time, this._associatedModel.getTimeTakenToDecideUponPeripheralSquareFixations());
          }
        }
        
        return fixation;
      }
      else{
        return new HypothesisDiscriminationFixation(this._associatedModel, time);
      }
    }
  }
  
  /**
   *
   * @param time
   * @return {@link java.lang.Boolean#TRUE} since there are no additional checks
   * to be made when adding a new {@link jchrest.domainSpecifics.Fixation} in
   * {@link jchrest.architecture.Chrest#scheduleOrMakeNextFixation(
   * jchrest.domainSpecifics.Scene, boolean, int)}.
   */
  @Override
  public boolean shouldAddNewFixation(int time){
    return true;
  }
  
  /**
   * @param time
   * 
   * @return {@link java.lang.Boolean#FALSE} since the only reason a {@link 
   * jchrest.domainSpecifics.Fixation} set should end in Chess is if the maximum
   * number of {@link jchrest.domainSpecifics.Fixation Fixations} have been
   * attempted.
   */
  @Override
  public boolean isFixationSetComplete(int time) {
    return false;
  }
  
  /**
   * 
   * @param time
   * 
   * @return {@link java.lang.Boolean#TRUE} if any of the following statements 
   * evaluate to {@link java.lang.Boolean#TRUE} for the {@link 
   * jchrest.domainSpecifics.Fixation} most recently performed by the {@link 
   * jchrest.architecture.Perceiver} associated with the {@link 
   * jchrest.architecture.Chrest} model using {@link #this} (based upon the 
   * {@code time} specified).  Otherwise, {@link java.lang.Boolean#FALSE} is 
   * returned:
   * 
   * <ol type="1">
   *    <li>
   *      {@link jchrest.domainSpecifics.Fixation} was made on an empty {@link 
   *      jchrest.lib.Square}.
   *    </li>
   *    <li>
   *      {@link jchrest.domainSpecifics.Fixation} was a {@link 
   *      jchrest.domainSpecifics.chess.fixations.GlobalStrategyFixation}.
   *    </li>
   *    <li>
   *      {@link jchrest.domainSpecifics.Fixation} was a {@link 
   *      jchrest.domainSpecifics.fixations.PeripheralItemFixation}.
   *    </li>
   *    <li>
   *      {@link jchrest.domainSpecifics.Fixation} was performed on a
   *      {@link jchrest.lib.Square} that has been fixated on before in this
   *      set of {@link jchrest.domainSpecifics.Fixation Fixations}.
   *    </li>
   * </ol>
   * 
   * {@link java.lang.Boolean#FALSE} is also returned if the {@link 
   * jchrest.architecture.Perceiver} associated with the {@link 
   * jchrest.architecture.Chrest} model was not created at the {@code time} 
   * specified or no {@link jchrest.domainSpecifics.Fixation Fixations} have 
   * been added to the {@link jchrest.architecture.Perceiver} at the {@code 
   * time} specified.
   */
  @Override
  public boolean shouldLearnFromNewFixations(int time){
    Fixation mostRecentFixationPerformed = this._associatedModel.getPerceiver().getMostRecentFixationPerformed(time);
    
    //If the Perceiver/CHREST model was not created at the time the function is
    //invoked or no fixations have been performed yet, do not continue.
    if(mostRecentFixationPerformed != null){
      
      //Was the most recent fixation on an empty square?
      if(mostRecentFixationPerformed.getScene().isSquareEmpty(mostRecentFixationPerformed.getColFixatedOn(), mostRecentFixationPerformed.getRowFixatedOn())) return true;
      
      //Was the most recent fixation a global strategy fixation?
      if(mostRecentFixationPerformed instanceof GlobalStrategyFixation) return true;
      
      //Was the most recent fixation a peripheral item fixation?
      if(mostRecentFixationPerformed instanceof PeripheralItemFixation) return true;
    }
    
    return false;
  }
  
  public int getTimeTakenToDecideOnGlobalStrategyFixations(){
    return this._timeTakenToDecideOnGlobalStrategyFixations;
  }
  
  public int getTimeTakenToDecideOnSalientManFixations(){
    return this._timeTakenToDecideOnSalientManFixations;
  }
  
  /**
   * 
   * @param time Should be >= 0
   */
  public void setTimeTakenToDecideOnGlobalStrategyFixations(int time){
    if(time < 0){
      throw new IllegalArgumentException(
        "The time specified to decide on a global strategy fixation is < 0 (" + time + ")."
      );
    }
    else{
      this._timeTakenToDecideOnGlobalStrategyFixations = time;
    }
  }
  
  /**
   * 
   * @param time Should be >= 0
   */
  public void setTimeTakenToDecideOnSalientManFixations(int time){
    if(time < 0){
      throw new IllegalArgumentException(
        "The time specified to decide on a salient man fixation is < 0 (" + time + ")."
      );
    }
    else{
      this._timeTakenToDecideOnSalientManFixations = time;
    }
  }
}

