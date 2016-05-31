################################################################################
# Tests the ChessDomain.constructBoard()" method using 5 scenarios that are 
# designed to trigger the IllegalArgumentException appropriately.
#
# Scenario 1: include a non-empty, non-FEN recognised character in board 
#             definition.
# Scenario 2: use an empty character in board definition.
# Scenario 3: remove an 'end-row' format character on a line other than the last
#             character of the defnition.
# Scenario 4: append an 'end-row' format character to the board definition.
# Scenario 5: valid board defintion.
#
# For each scenario, a test is performed to ensure that an 
# IllegalArgumentException is/is not thrown as expected.  For scenario 5, the
# ChessBoard constructed is checked to ensure that the definition produces the
# expected ChessBoard.
unit_test "construct_board" do
  
  for scenario in 1..5
    
    ########################
    ##### DEFINE BOARD #####
    ########################
    
    board_to_construct =
      "rnbqkbnr/" +
      "pppppppp/" +
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "PPPPPPPP/" + 
      "RNBQKBNR"
    
    if scenario == 1 then board_to_construct[0] = "g" end
    if scenario == 2 then board_to_construct[31] = " " end
    if scenario == 3 then board_to_construct[8] = "" end
    if scenario == 4 then board_to_construct = "/" end
    
    ###########################
    ##### CONSTRUCT BOARD #####
    ###########################
    
    exception_thrown = false
    board = nil
    begin 
      board = ChessDomain.constructBoard(board_to_construct)
    rescue
      exception_thrown = true
    end
    
    expected_error_thrown = (scenario != 5 ? true : false)
    
    assert_equal(
      expected_error_thrown,
      exception_thrown,
      "occurred in scenario " + scenario.to_s + " when checking if an exception " +
      "is thrown due to the format of the board definition string "
    )
    
    ##############################################
    ##### CHECK BOARD CONSTRUCTED IS CORRECT #####
    ##############################################
  
    if scenario == 5
      for col in 0...board.getWidth()
        for row in 0...board.getHeight()
          object_type = 
            row == 0 ? 
              col == 0 || col == 7 ? "R" :
              col == 1 || col == 6 ? "N" :
              col == 2 || col == 5 ? "B" :
              col == 3 ? "Q" :
              "K"
            :
            row == 1 ? "P" 
            :
            row == 6 ? "p"
            :
            row == 7 ?
              col == 0 || col == 7 ? "r" :
              col == 1 || col == 6 ? "n" :
              col == 2 || col == 5 ? "b" :
              col == 3 ? "q" :
              "k"
            :
            "."

          assert_equal(
            object_type, 
            board.getSquareContents(col, row).getObjectType(), 
            "occurred when checking the contents of col " + col.to_s + ", row " + 
            row.to_s + " in scenario " + scenario.to_s + " for the ChessBoard " + 
            "constructed"
          )
        end
      end
    end
  end
end

################################################################################
unit_test "get_big_pieces" do
  board = ChessDomain.constructBoard(
    "rnbqkbnr/" +
    "pppppppp/" +
    "......../" +
    "......../" +
    "......../" +
    "......../" +
    "PPPPPPPP/" + 
    "RNBQKBNR"
  )
  
  big_pieces = ChessDomain.getBigPieces(board).to_a
  expected = [
    ItemSquarePattern.new("R", 0, 0),
    ItemSquarePattern.new("r", 0, 7),
    ItemSquarePattern.new("N", 1, 0),
    ItemSquarePattern.new("n", 1, 7),
    ItemSquarePattern.new("B", 2, 0),
    ItemSquarePattern.new("b", 2, 7),
    ItemSquarePattern.new("Q", 3, 0),
    ItemSquarePattern.new("q", 3, 7),
    ItemSquarePattern.new("K", 4, 0),
    ItemSquarePattern.new("k", 4, 7),
    ItemSquarePattern.new("B", 5, 0),
    ItemSquarePattern.new("b", 5, 7),
    ItemSquarePattern.new("N", 6, 0),
    ItemSquarePattern.new("n", 6, 7),
    ItemSquarePattern.new("R", 7, 0),
    ItemSquarePattern.new("r", 7, 7)
  ]
  
  #################
  ##### TESTS #####
  #################
  
  assert_equal(
    big_pieces.length,
    expected.length,
    "occurred when checking the number of pieces expected"
  )
  
  #Check the actual contents of "big_pieces" against "expected" using 
  #"include?" so that the order of ItemSquarePatterns in "expected" doesn't have
  #an effect on the test outcome.  To do this, all ItemSquarePattern objects in 
  #"big_pieces" and "expected" need to be converted to Strings otherwise, the 
  #test will always fail since it will be comparing two different objects. Can 
  #do this using a for loop for both the "big_pieces" and "expected" arrays 
  #that only considers the length of the "big_pieces" array since it was proven 
  #above that these two arrays are the same length.
  big_pieces.map!(&:to_s)
  expected.map!(&:to_s)
  
  for piece in expected
    assert_true(
      big_pieces.include?(piece),
      "occurred when checking if the expected piece " + piece + " is present " +
      "in the pieces returned by the function"
    )
  end
end

################################################################################
unit_test "get_offensive_pieces" do
  board = ChessDomain.constructBoard(
    "rnbqkbnr/" +
    "pp.ppppp/" +
    "......../" +
    "......N./" +
    "..p...../" +
    "......../" +
    "PPPPPPPP/" + 
    "RNBQKB.R"
  )
  
  offensive_pieces = ChessDomain.getOffensivePieces(board).to_a
  expected = [
    ItemSquarePattern.new("p", 2, 3),
    ItemSquarePattern.new("N", 6, 4)
  ]
  
  #################
  ##### TESTS #####
  #################
  
  assert_equal(
    offensive_pieces.length,
    expected.length,
    "occurred when checking the number of pieces expected"
  )

  #Check the actual contents of "offensive_pieces" against "expected" using 
  #"include?" so that the order of ItemSquarePatterns in "expected" doesn't have
  #an effect on the test outcome.  To do this, all ItemSquarePattern objects in 
  #"offensive_pieces" and "expected" need to be converted to Strings otherwise, 
  #the test will always fail since it will be comparing two different objects. 
  #Can do this using a for loop for both the "offensive_pieces" and "expected" 
  #arrays that only considers the length of the "offensive_pieces" array since 
  #it was proven above that these two arrays are the same length.
  offensive_pieces.map!(&:to_s)
  expected.map!(&:to_s)
  
  for piece in expected
    assert_true(
      offensive_pieces.include?(piece),
      "occurred when checking if the expected piece " + piece + " is present " +
      "in the pieces returned by the function"
    )
  end
end

################################################################################
unit_test "different_colour" do
  board = ChessDomain.constructBoard(
    "rnbqkbnr/" +
    "pppppppp/" +
    "......../" +
    "......../" +
    "......../" +
    "......../" +
    "PPPPPPPP/" + 
    "RNBQKBNR"
  )
  
  assert_true(ChessDomain.differentColour(board, Square.new(0, 0), Square.new(3, 7)), "occurred during sub-test 1")
  assert_false(ChessDomain.differentColour(board, Square.new(0, 0), Square.new(0, 1)), "occurred during sub-test 2 (two white pieces)")
  assert_false(ChessDomain.differentColour(board, Square.new(0, 7), Square.new(3, 7)), "occurred during sub-test 3 (two black pieces)")
  assert_false(ChessDomain.differentColour(board, Square.new(0, 0), Square.new(0, 3)), "occurred during sub-test 4 (non-empty square and empty square)")
  assert_false(ChessDomain.differentColour(board, Square.new(0, 0), Square.new(0, 3)), "occurred during sub-test 5 (empty square and non-empty square)")
  assert_false(ChessDomain.differentColour(board, Square.new(0, 4), Square.new(0, 3)), "occurred during sub-test 5 (two empty squares)")
end

################################################################################
# The "lineMove" function will "draw" a line of movement until it reaches the 
# edge of a chess board or until it encounters a piece.  So, to test the 
# function, create two boards: an empty board and one containing pieces.  The 
# empty board will be used to check if a line is "drawn" until the edge of the
# board is reached whilst the non-empty board will be used to check if a line is
# "drawn" until a piece is encountered.
#
# No matter what type of board is used, a line is drawn along each caridnal and
# primary inter-cardinal compass direction since these are the only types of 
# lines used in chess.
unit_test "line_move" do
  
  empty_board_to_construct = 
    "......../" +
    "......../" +
    "......../" +
    "...q..../" +
    "......../" +
    "......../" +
    "......../" +
    "........"
  
  non_empty_board_to_construct =
    "......../" +
    ".R.N.Q../" +
    "......../" +
    ".r.q.k../" +
    "......../" +
    ".p.P.b../" +
    "......../" +
    "........"
  
  start_square = Square.new(3, 4)
  empty_board = ChessDomain.constructBoard(empty_board_to_construct)
  non_empty_board = ChessDomain.constructBoard(non_empty_board_to_construct)
  
  #################
  ##### TESTS #####
  #################
  
  #Check that invalid deltas cause IllegalArgumentExceptions to be thrown.
  for i in 1..4
    exception_thrown = false
    
    begin
      ChessDomain.lineMove(
        empty_board, 
        start_square,
        i == 1 ? -2 : i == 2 ? 2 : 0,
        i == 3 ? -2 : i == 4 ? 2 : 0
      )
    rescue
      exception_thrown = true
    end
    
    assert_true(
      exception_thrown,
      i == 1 ? "occurred when checking if an exception is thrown when row delta = -2" :
      i == 2 ? "occurred when checking if an exception is thrown when row delta = 2" :
      i == 3 ? "occurred when checking if an exception is thrown when col delta = -2" :
      "occurred when checking if an exception is thrown when col delta = 2"
    )
  end
  
  #Check that an empty start square causes an IllegalArgumentException to be 
  #thrown.
  exception_thrown = false
  begin
    ChessDomain.lineMove(empty_board, Square.new(0, 0), 1, 0)
  rescue
    exception_thrown = true
  end
  
  assert_true(
    exception_thrown, 
    "occurred when checking if an exception is thrown when the start square 
    specified is empty"
  )
  
  #Check lines when deltas are OK
  for compass_direction in 1..8
    row_delta = 0
    col_delta = 0
    expected_squares_visited_when_board_is_empty = []
    expected_squares_visited_when_board_is_not_empty = []
    expected_squares_considered_when_board_is_empty = nil
    expected_squares_considered_when_board_is_not_empty = 2
    compass_direction_name = nil
    
    #Set deltas and expected squares visited
    if(compass_direction == 1) 
      compass_direction_name = "north"
      row_delta = 1
      expected_squares_visited_when_board_is_empty.push(Square.new(3, 5))
      expected_squares_visited_when_board_is_empty.push(Square.new(3, 6))
      expected_squares_visited_when_board_is_empty.push(Square.new(3, 7))
      expected_squares_visited_when_board_is_not_empty.push(Square.new(3, 5))
      expected_squares_visited_when_board_is_not_empty.push(Square.new(3, 6))
    elsif(compass_direction == 2) 
      compass_direction_name = "north-east"
      col_delta = 1
      row_delta = 1
      expected_squares_visited_when_board_is_empty.push(Square.new(4, 5))
      expected_squares_visited_when_board_is_empty.push(Square.new(5, 6))
      expected_squares_visited_when_board_is_empty.push(Square.new(6, 7))
      expected_squares_visited_when_board_is_not_empty.push(Square.new(4, 5))
      expected_squares_visited_when_board_is_not_empty.push(Square.new(5, 6))
    elsif(compass_direction == 3) 
      compass_direction_name = "east"
      col_delta = 1 
      expected_squares_visited_when_board_is_empty.push(Square.new(4, 4))
      expected_squares_visited_when_board_is_empty.push(Square.new(5, 4))
      expected_squares_visited_when_board_is_empty.push(Square.new(6, 4))
      expected_squares_visited_when_board_is_empty.push(Square.new(7, 4))
      expected_squares_visited_when_board_is_not_empty.push(Square.new(4, 4))
    elsif(compass_direction == 4) 
      compass_direction_name = "south-east"
      col_delta = 1
      row_delta = -1
      expected_squares_visited_when_board_is_empty.push(Square.new(4, 3))
      expected_squares_visited_when_board_is_empty.push(Square.new(5, 2))
      expected_squares_visited_when_board_is_empty.push(Square.new(6, 1))
      expected_squares_visited_when_board_is_empty.push(Square.new(7, 0))
      expected_squares_visited_when_board_is_not_empty.push(Square.new(4, 3))
    elsif(compass_direction == 5) 
      compass_direction_name = "south"
      row_delta = -1
      expected_squares_visited_when_board_is_empty.push(Square.new(3, 3))
      expected_squares_visited_when_board_is_empty.push(Square.new(3, 2))
      expected_squares_visited_when_board_is_empty.push(Square.new(3, 1))
      expected_squares_visited_when_board_is_empty.push(Square.new(3, 0))
      expected_squares_visited_when_board_is_not_empty.push(Square.new(3, 3))
      expected_squares_visited_when_board_is_not_empty.push(Square.new(3, 2))
    elsif(compass_direction == 6) 
      compass_direction_name = "south-west"
      col_delta = -1
      row_delta = -1
      expected_squares_visited_when_board_is_empty.push(Square.new(2, 3))
      expected_squares_visited_when_board_is_empty.push(Square.new(1, 2))
      expected_squares_visited_when_board_is_empty.push(Square.new(0, 1))
      expected_squares_visited_when_board_is_not_empty.push(Square.new(2, 3))
    elsif(compass_direction == 7) 
      compass_direction_name = "west"
      col_delta = -1
      expected_squares_visited_when_board_is_empty.push(Square.new(2, 4))
      expected_squares_visited_when_board_is_empty.push(Square.new(1, 4))
      expected_squares_visited_when_board_is_empty.push(Square.new(0, 4))
      expected_squares_visited_when_board_is_not_empty.push(Square.new(2, 4))
    elsif(compass_direction == 8) 
      compass_direction_name = "north-west"
      col_delta = -1
      row_delta = 1
      expected_squares_visited_when_board_is_empty.push(Square.new(2, 5))
      expected_squares_visited_when_board_is_empty.push(Square.new(1, 6))
      expected_squares_visited_when_board_is_empty.push(Square.new(0, 7))
      expected_squares_visited_when_board_is_not_empty.push(Square.new(2, 5))
      expected_squares_visited_when_board_is_not_empty.push(Square.new(1, 6))
    end
    
    #Set expected squares traversed when board is empty.
    if([1, 2, 6, 7, 8].include?(compass_direction))
      expected_squares_considered_when_board_is_empty = 3
    else
      expected_squares_considered_when_board_is_empty = 4
    end
    
    #######################
    ##### GET RESULTS #####
    #######################
    empty_board_result = ChessDomain.lineMove(empty_board, start_square, col_delta, row_delta)
    empty_board_squares_visited = empty_board_result[0].to_a
    empty_board_squares_considered = empty_board_result[1]
    non_empty_board_result = ChessDomain.lineMove(non_empty_board, start_square, col_delta, row_delta)
    non_empty_board_squares_visited = non_empty_board_result[0].to_a
    non_empty_board_squares_considered = non_empty_board_result[1]
    
    #################
    ##### TESTS #####
    #################
    
    #First check that the number of squares visited for the compass direction 
    #equals the number expected when the board is/is not empty.
    assert_equal(
      expected_squares_visited_when_board_is_empty.length, 
      empty_board_squares_visited.length,
      "occurred when checking the number of squares visited when board is empty " +
      "and compass direction is set to " + compass_direction_name + ""
    )
    assert_equal(
      expected_squares_visited_when_board_is_not_empty.length, 
      non_empty_board_squares_visited.length,
      "occurred when checking the number of squares vistited when board is not empty " +
      "and compass direction is set to " + compass_direction_name
    )
    
    #Now that its been asserted that the number of squares visited and the 
    #number of squares expected to be visited are equal, transform each element
    #of these arrays into their string representation so that an "include?" 
    #check can be performed on the contents of what is returned vs. what is
    #expected.  This will ensure that the test won't fail due to incorrect
    #ordering of what squares are expected in the relevant arrays.
    empty_board_squares_visited.map!(&:to_s)
    expected_squares_visited_when_board_is_empty.map!(&:to_s)
    non_empty_board_squares_visited.map!(&:to_s)
    expected_squares_visited_when_board_is_not_empty.map!(&:to_s)

    
    #Check the contents of what is squares are visited against what is expected
    #for empty/non-empty boards.
    for square in expected_squares_visited_when_board_is_empty
      assert_true(
        empty_board_squares_visited.include?(square),
        "occurred when checking if the square " + square + " is included in " +
        "the squares visited when the board is empty and compass direction is " + 
        "set to " + compass_direction_name
      )
    end
    
    for square in expected_squares_visited_when_board_is_not_empty
      assert_true(
        non_empty_board_squares_visited.include?(square),
        "occurred when checking if the square " + square + " is included in " +
        "the squares visited when the board is not empty and compass direction is " + 
        "set to " + compass_direction_name
      )
    end
    
    #Check the number of squares considered against what is expected for 
    #empty/non-empty boards.
    assert_equal(
      expected_squares_considered_when_board_is_empty, 
      empty_board_squares_considered,
      "occurred when checking the number of squares considered when the board " +
      "is empty and compass direction is set to " + compass_direction_name
    )
    assert_equal(
      expected_squares_considered_when_board_is_not_empty, 
      non_empty_board_squares_considered,
      "occurred when checking the number of squares considered when the board " +
      "is not empty and compass direction is set to " + compass_direction_name
    )
  end
end

################################################################################
unit_test "valid_move" do
  board = ChessDomain.constructBoard(
    "rnbqkbnr/" +
    "ppppp.pp/" +
    "...P..../" +
    "......../" +
    "......../" +
    ".....p../" +
    "PPP.PPPP/" + 
    "RNBQKBNR"
  )
  
  #Check that specifying an empty square as the start square throws an 
  #IllegalArgumentException.
  exception_thrown = false
  begin
    ChessDomain.validMove(board, Square.new(0, 2), Square(3, 4))
  rescue
    exception_thrown = true
  end
  
  assert_true(
    exception_thrown,
    "occurred when checking if an exception is thrown when the start square 
    specified is empty"
  )
  
  #Check output with non-empty start squares.
  assert_true(
    ChessDomain.validMove(board, Square.new(3, 0), Square.new(3, 4)),
    "occurred when checking if the white queen can move to an empty square."
  )
  
  assert_true(
    ChessDomain.validMove(board, Square.new(5, 2), Square.new(4, 2)),
    "occurred when checking if a black pawn can move to square occupied by " +
    "a white pawn"
  )
  
  assert_false(
    ChessDomain.validMove(board, Square.new(3, 0), Square.new(4, 1)), 
    "occurred when checking if the white queen can move to square occupied by " +
    "a white pawn"
  )
end

################################################################################
# Checks for correct operation of the ChessDomain.getPawnMoves() function when
# the square specified as an input parameter to the function contains a black
# pawn.
# 
# Since pawn movement is rather complex, a number of sub-tests need to be 
# performed to ensure that the function operates correctly.
# 
# Pawns can make 4 types of moves: a standard move (1 square forward), an 
# initial move (2 squares forward if the pawn is located on the starting row),
# a left capture and a right capture.  Producing a table of all possible 
# combinations gives 16 sub-tests:
# 
# |----------|---------------|--------------|--------------|---------------|
# | Sub-test | Standard move | Initial move | Capture left | Capture right |
# |----------|---------------|--------------|--------------|---------------|
# | 1        | Y             | Y            | Y            | Y             |
# | 2        | Y             | Y            | Y            | N             |
# | 3        | Y             | Y            | N            | Y             |
# | 4        | Y             | Y            | N            | N             |
# | 5        | Y             | N            | Y            | Y             |
# | 6        | Y             | N            | Y            | N             |
# | 7        | Y             | N            | N            | Y             |
# | 8        | Y             | N            | N            | N             |
# | 9        | N             | Y            | Y            | Y             |
# | 10       | N             | Y            | Y            | N             |
# | 11       | N             | Y            | N            | Y             |
# | 12       | N             | Y            | N            | N             |
# | 13       | N             | N            | Y            | Y             |
# | 14       | N             | N            | Y            | N             |
# | 15       | N             | N            | N            | Y             |
# | 16       | N             | N            | N            | N             |
# |----------|---------------|--------------|--------------|---------------|
#
# Sub-tests 9 to 12 are not possible though since, if a standard move is not
# possible, an initial move is not possible.  Therefore, there are only 12
# sub-tests to perform.
unit_test "get_black_pawn_moves" do
  
  check_exception_thrown_by_get_moves_method_for_piece("p")
  
  for subtest in 1..12
    
    ##########################
    ##### SET-UP SUBTEST #####
    ##########################
    
    board_to_construct = ""
    board = nil
    initial_pawn_location = nil
    expected_moves = []
    expected_squares_considered = 0
    
    # Standard move: Y
    # Initial move: Y
    # Capture left: Y
    # Capture right: Y
    if subtest == 1
      board_to_construct =
        "rnbqkbnr/" +
        "pppppppp/" +
        "...P.P../" +
        "......../" +
        "......../" +
        "......../" +
        "PPP.P.PP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(4, 6)
      expected_moves.push(Square.new(4, 5))
      expected_moves.push(Square.new(4, 4))
      expected_moves.push(Square.new(3, 5))
      expected_moves.push(Square.new(5, 5))
      expected_squares_considered = 4
    
    # Standard move: Y
    # Initial move: Y
    # Capture left: Y
    # Capture right: N
    elsif subtest == 2
      board_to_construct =
        "rnbqkbnr/" +
        "pppppppp/" +
        "......P./" +
        "......../" +
        "......../" +
        "......../" +
        "PPPPPP.P/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(7, 6)
      expected_moves.push(Square.new(7, 5))
      expected_moves.push(Square.new(7, 4))
      expected_moves.push(Square.new(6, 5))
      expected_squares_considered = 3
    
    # Standard move: Y
    # Initial move: Y
    # Capture left: N
    # Capture right: Y
    elsif subtest == 3
      board_to_construct =
        "rnbqkbnr/" +
        "pppppppp/" +
        ".P....../" +
        "......../" +
        "......../" +
        "......../" +
        "P.PPPPPP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(0, 6)
      expected_moves.push(Square.new(0, 5))
      expected_moves.push(Square.new(0, 4))
      expected_moves.push(Square.new(1, 5))
      expected_squares_considered = 3
    
    # Standard move: Y
    # Initial move: Y
    # Capture left: N
    # Capture right: N
    elsif subtest == 4
      board_to_construct =
        "rnbqkbnr/" +
        "pppppppp/" +
        "......../" +
        "......../" +
        "......../" +
        "......../" +
        "PPPPPPPP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(4, 6)
      expected_moves.push(Square.new(4, 5))
      expected_moves.push(Square.new(4, 4))
      expected_squares_considered = 4
      
    # Standard move: Y
    # Initial move: N
    # Capture left: Y
    # Capture right: Y
    elsif subtest == 5
      board_to_construct =
        "rnbqkbnr/" +
        "pppp.ppp/" +
        "......../" +
        "....p.../" +
        "...P.P../" +
        "......../" +
        "PPP.P.PP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(4, 4)
      expected_moves.push(Square.new(4, 3))
      expected_moves.push(Square.new(3, 3))
      expected_moves.push(Square.new(5, 3))
      expected_squares_considered = 4
    
    # Standard move: Y
    # Initial move: N
    # Capture left: Y
    # Capture right: N
    elsif subtest == 6
      board_to_construct =
        "rnbqkbnr/" +
        "ppppppp./" +
        ".......p/" +
        "......P./" +
        "......../" +
        "......../" +
        "PPPPPP.P/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(7, 5)
      expected_moves.push(Square.new(7, 4))
      expected_moves.push(Square.new(6, 4))
      expected_squares_considered = 3
      
    # Standard move: Y
    # Initial move: N
    # Capture left: N
    # Capture right: Y
    elsif subtest == 7 
      board_to_construct =
        "rnbqkbnr/" +
        ".ppppppp/" +
        "p......./" +
        ".P....../" +
        "......../" +
        "......../" +
        "PPPPPPPP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(0, 5)
      expected_moves.push(Square.new(0, 4))
      expected_moves.push(Square.new(1, 4))
      expected_squares_considered = 3
    
    # Standard move: Y
    # Initial move: N
    # Capture left: N
    # Capture right: N
    elsif subtest == 8
      board_to_construct =
        "rnbqkbnr/" +
        "ppp.pppp/" +
        "...p..../" +
        "......../" +
        "......../" +
        "......../" +
        "PPPPPPPP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(3, 5)
      expected_moves.push(Square.new(3, 4))
      expected_squares_considered = 4
      
    # Standard move: N
    # Initial move: N
    # Capture left: Y
    # Capture right: Y
    elsif subtest == 9
      board_to_construct =
        "rnbqkbnr/" +
        "pppp.ppp/" +
        "......../" +
        "....p.../" +
        "...PPP../" +
        "......../" +
        "PPP...PP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(4, 4)
      expected_moves.push(Square.new(3, 3))
      expected_moves.push(Square.new(5, 3))
      expected_squares_considered = 3
      
    # Standard move: N
    # Initial move: N
    # Capture left: Y
    # Capture right: N
    elsif subtest == 10
      board_to_construct =
        "rnbqkbnr/" +
        "ppppppp./" +
        "......../" +
        ".......p/" +
        "......PP/" +
        "......../" +
        "PPPPPP../" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(7, 4)
      expected_moves.push(Square.new(6, 3))
      expected_squares_considered = 2
      
    # Standard move: N
    # Initial move: N
    # Capture left: N
    # Capture right: Y
    elsif subtest == 11
      board_to_construct =
        "rnbqkbnr/" +
        ".ppppppp/" +
        "......../" +
        "p......./" +
        "PP....../" +
        "......../" +
        "PPPPPPPP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(0, 4)
      expected_moves.push(Square.new(1, 3))
      expected_squares_considered = 2
      
    # Standard move: N
    # Initial move: N
    # Capture left: N
    # Capture right: N
    elsif subtest == 12
      board_to_construct =
        "rnbqkbnr/" +
        "pppp.ppp/" +
        "......../" +
        "....p.../" +
        "....P.../" +
        "......../" +
        "PPPP.PPP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(4, 4)
      expected_squares_considered = 3
    end
    
    ########################################################
    ##### CONSTRUCT BOARD AND GET BLACK PAWN MOVE DATA #####
    ########################################################

    board = ChessDomain.constructBoard(board_to_construct)
    pawn_moves_and_squares_traversed = ChessDomain.getPawnMoves(board, initial_pawn_location)
    moves = pawn_moves_and_squares_traversed[0]
    squares_considered = pawn_moves_and_squares_traversed[1]

    #################
    ##### TESTS #####
    #################
    
    assert_equal(
      expected_moves.length, 
      moves.length, 
      "occurred when checking number of potential moves in sub-test " + 
      subtest.to_s
    )

    #Check the actual contents of "moves" against "expected_moves" using 
    #"include?" so that the order of Squares in "expected_moves" doesn't have
    #an effect on the test outcome.  To do this, all Square objects in "moves" 
    #and "expected_moves" need to be converted to Strings otherwise, the 
    #test will always fail since it will be comparing two different objects. Can 
    #do this using a for loop for both the "moves" and "expected_moves" arrays 
    #that only considers the length of the "expected_moves" array since it was 
    #proven above that these two arrays are the same length.
    moves = moves.to_a
    moves.map!(&:to_s)
    expected_moves.map!(&:to_s)

    for expected_move in expected_moves
      assert_true(
        moves.include?(expected_move), 
        "occurred when checking if the expected move " + expected_move + " " +
        "is present in the potential moves returned in sub-test " + subtest.to_s
      )
    end

    assert_equal(
      expected_squares_considered, 
      squares_considered,
      "occurred when checking the number of squares considered in sub-test " +
      subtest.to_s
    )
  end
end

################################################################################
# Checks for correct operation of the ChessDomain.getPawnMoves() function when 
# a white pawn is on the square specified as a parameter to the function.
# 
# Since pawn movement is rather complex, a number of sub-tests need to be 
# performed to ensure that the function operates correctly.
# 
# Pawns can make 4 types of moves: a standard move (1 square forward), an 
# initial move (2 squares forward if the pawn is located on the starting row),
# a left capture and a right capture.  Producing a table of all possible 
# combinations gives 16 sub-tests:
# 
# |----------|---------------|--------------|--------------|---------------|
# | Sub-test | Standard move | Initial move | Capture left | Capture right |
# |----------|---------------|--------------|--------------|---------------|
# | 1        | Y             | Y            | Y            | Y             |
# | 2        | Y             | Y            | Y            | N             |
# | 3        | Y             | Y            | N            | Y             |
# | 4        | Y             | Y            | N            | N             |
# | 5        | Y             | N            | Y            | Y             |
# | 6        | Y             | N            | Y            | N             |
# | 7        | Y             | N            | N            | Y             |
# | 8        | Y             | N            | N            | N             |
# | 9        | N             | Y            | Y            | Y             |
# | 10       | N             | Y            | Y            | N             |
# | 11       | N             | Y            | N            | Y             |
# | 12       | N             | Y            | N            | N             |
# | 13       | N             | N            | Y            | Y             |
# | 14       | N             | N            | Y            | N             |
# | 15       | N             | N            | N            | Y             |
# | 16       | N             | N            | N            | N             |
# |----------|---------------|--------------|--------------|---------------|
#
# Sub-tests 9 to 12 are not possible though since, if a standard move is not
# possible, an initial move is not possible.  Therefore, there are only 12
# sub-tests to perform.
unit_test "get_white_pawn_moves" do
  
  check_exception_thrown_by_get_moves_method_for_piece("P")
  
  for subtest in 1..12
    
    ##########################
    ##### SET-UP SUBTEST #####
    ##########################
    
    board_to_construct = ""
    board = nil
    initial_pawn_location = nil
    expected_moves = []
    expected_squares_considered = 0
    
    # Standard move: Y
    # Initial move: Y
    # Capture left: Y
    # Capture right: Y
    if subtest == 1
      board_to_construct =
        "rnbqkbnr/" +
        "pppppppp/" +
        "......../" +
        "......../" +
        "......../" +
        "...p.p../" +
        "PPPPPPPP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(4, 1)
      expected_moves.push(Square.new(4, 2))
      expected_moves.push(Square.new(4, 3))
      expected_moves.push(Square.new(3, 2))
      expected_moves.push(Square.new(5, 2))
      expected_squares_considered = 4
    
    # Standard move: Y
    # Initial move: Y
    # Capture left: Y
    # Capture right: N
    elsif subtest == 2
      board_to_construct =
        "rnbqkbnr/" +
        "pppppppp/" +
        "......../" +
        "......../" +
        "......../" +
        "......p./" +
        "PPPPPPPP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(7, 1)
      expected_moves.push(Square.new(7, 2))
      expected_moves.push(Square.new(7, 3))
      expected_moves.push(Square.new(6, 2))
      expected_squares_considered = 3
    
    # Standard move: Y
    # Initial move: Y
    # Capture left: N
    # Capture right: Y
    elsif subtest == 3
      board_to_construct =
        "rnbqkbnr/" +
        "p.pppppp/" +
        "......../" +
        "......../" +
        "......../" +
        ".p....../" +
        "PPPPPPPP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(0, 1)
      expected_moves.push(Square.new(0, 2))
      expected_moves.push(Square.new(0, 3))
      expected_moves.push(Square.new(1, 2))
      expected_squares_considered = 3
    
    # Standard move: Y
    # Initial move: Y
    # Capture left: N
    # Capture right: N
    elsif subtest == 4
      board_to_construct =
        "rnbqkbnr/" +
        "pppppppp/" +
        "......../" +
        "......../" +
        "......../" +
        "......../" +
        "PPPPPPPP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(4, 1)
      expected_moves.push(Square.new(4, 2))
      expected_moves.push(Square.new(4, 3))
      expected_squares_considered = 4
      
    # Standard move: Y
    # Initial move: N
    # Capture left: Y
    # Capture right: Y
    elsif subtest == 5
      board_to_construct =
        "rnbqkbnr/" +
        "pppp.ppp/" +
        "......../" +
        "...p.p../" +
        "....P.../" +
        "......../" +
        "PPPP.PPP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(4, 3)
      expected_moves.push(Square.new(4, 4))
      expected_moves.push(Square.new(3, 4))
      expected_moves.push(Square.new(5, 4))
      expected_squares_considered = 4
    
    # Standard move: Y
    # Initial move: N
    # Capture left: Y
    # Capture right: N
    elsif subtest == 6
      board_to_construct =
        "rnbqkbnr/" +
        "pppppp.p/" +
        "......../" +
        "......p./" +
        ".......P/" +
        "......../" +
        "PPPPPPP./" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(7, 3)
      expected_moves.push(Square.new(7, 4))
      expected_moves.push(Square.new(6, 4))
      expected_squares_considered = 3
      
    # Standard move: Y
    # Initial move: N
    # Capture left: N
    # Capture right: Y
    elsif subtest == 7 
      board_to_construct =
        "rnbqkbnr/" +
        "p.pppppp/" +
        "......../" +
        ".p....../" +
        "P......./" +
        "......../" +
        ".PPPPPPP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(0, 3)
      expected_moves.push(Square.new(0, 4))
      expected_moves.push(Square.new(1, 4))
      expected_squares_considered = 3
    
    # Standard move: Y
    # Initial move: N
    # Capture left: N
    # Capture right: N
    elsif subtest == 8
      board_to_construct =
        "rnbqkbnr/" +
        "pppppppp/" +
        "......../" +
        "......../" +
        "......../" +
        "...P..../" +
        "PPP.PPPP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(3, 2)
      expected_moves.push(Square.new(3, 3))
      expected_squares_considered = 4
      
    # Standard move: N
    # Initial move: N
    # Capture left: Y
    # Capture right: Y
    elsif subtest == 9
      board_to_construct =
        "rnbqkbnr/" +
        "ppp...pp/" +
        "......../" +
        "...ppp../" +
        "....P.../" +
        "......../" +
        "PPPP.PPP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(4, 3)
      expected_moves.push(Square.new(3, 4))
      expected_moves.push(Square.new(5, 4))
      expected_squares_considered = 3
      
    # Standard move: N
    # Initial move: N
    # Capture left: Y
    # Capture right: N
    elsif subtest == 10
      board_to_construct =
        "rnbqkbnr/" +
        "pppppp../" +
        "......../" +
        "......pp/" +
        ".......P/" +
        "......../" +
        "PPPPPPP./" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(7, 3)
      expected_moves.push(Square.new(6, 4))
      expected_squares_considered = 2
      
    # Standard move: N
    # Initial move: N
    # Capture left: N
    # Capture right: Y
    elsif subtest == 11
      board_to_construct =
        "rnbqkbnr/" +
        "..pppppp/" +
        "......../" +
        "pp....../" +
        "P......./" +
        "......../" +
        ".PPPPPPP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(0, 3)
      expected_moves.push(Square.new(1, 4))
      expected_squares_considered = 2
      
    # Standard move: N
    # Initial move: N
    # Capture left: N
    # Capture right: N
    elsif subtest == 12
      board_to_construct =
        "rnbqkbnr/" +
        "pppp.ppp/" +
        "......../" +
        "....p.../" +
        "....P.../" +
        "......../" +
        "PPPP.PPP/" + 
        "RNBQKBNR"
      
      initial_pawn_location = Square.new(4, 3)
      expected_squares_considered = 3
    end
    
    ########################################################
    ##### CONSTRUCT BOARD AND GET WHITE PAWN MOVE DATA #####
    ########################################################

    board = ChessDomain.constructBoard(board_to_construct)
    pawn_moves_and_squares_traversed = ChessDomain.getPawnMoves(board, initial_pawn_location)
    moves = pawn_moves_and_squares_traversed[0]
    squares_considered = pawn_moves_and_squares_traversed[1]

    #################
    ##### TESTS #####
    #################
    
    assert_equal(
      expected_moves.length, 
      moves.length, 
      "occurred when checking number of potential moves in sub-test " + 
      subtest.to_s
    )

    #Check the actual contents of "moves" against "expected_moves" using 
    #"include?" so that the order of Squares in "expected_moves" doesn't have
    #an effect on the test outcome.  To do this, all Square objects in "moves" 
    #and "expected_moves" need to be converted to Strings otherwise, the 
    #test will always fail since it will be comparing two different objects. Can 
    #do this using a for loop for both the "moves" and "expected_moves" arrays 
    #that only considers the length of the "expected_moves" array since it was 
    #proven above that these two arrays are the same length.
    moves = moves.to_a
    moves.map!(&:to_s)
    expected_moves.map!(&:to_s)

    for expected_move in expected_moves
      assert_true(
        moves.include?(expected_move), 
        "occurred when checking if the expected move " + expected_move + " " +
        "is present in the potential moves returned in sub-test " + subtest.to_s
      )
    end

    assert_equal(
      expected_squares_considered, 
      squares_considered,
      "occurred when checking the number of squares considered in sub-test " +
      subtest.to_s
    )
  end
end

################################################################################
# To test the "getKnightMoves()" function, the function is first invoked with a 
# square specifying every piece other than a white/black knight to see if an
# exception is thrown correctly.  Following this, a white/black knight is placed 
# upon each coordinate on a chess board and three types of test are conducted:
# 
# 1. Board is empty except for the knight
# 2. Board is full of pieces of a different colour to the knight
# 3. Board is full of pieces of the same colour as the knight
# 
# For each test type, the number and coordinates of squares that can be moved to
# are checked along with the number of squares considered.
#
# In the first test type, the knight should be able to move freely except to
# squares that are not "on the board".  For instance, if the knight is on rank
# "a", file 1 it should not be able to move to the left or down.  In the second 
# test type, the squares moved to and considered should not differ from the 
# first test type since the function considers any square occupied by an 
# opposing piece colour as a square that can be moved to. For the third test 
# type, the knight should not be able to move anywhere but will consider a 
# number of squares to move to.
#
# The test is run for both white and black knights to ensure that the function
# operates correctly for both colours.
unit_test "get_knight_moves" do
  
  for colour in 1..2
    piece = (colour == 1 ? "N" : "n") 

    #Used in error messages.
    piece_colour = (colour == 1 ? "white" : "black") 

    check_exception_thrown_by_get_moves_method_for_piece(piece)
  
    for test_type in 1..3

      #Each "coordinate" is a character in the "board_to_construct" string below.
      for coordinate in 0...71
        board_to_construct = 
          "......../" +
          "......../" +
          "......../" +
          "......../" +
          "......../" +
          "......../" +
          "......../" +
          "........"

        #Perform a test if the "coordinate" on the board is an empty square 
        #(ignore new row characters).
        if board_to_construct[coordinate] == "."

          #####################################
          ##### SET-UP BOARD TO CONSTRUCT #####
          #####################################

          #Put the knight on the board coordinate specified.
          board_to_construct[coordinate] = piece

          #Fill the empty spaces with pieces, if the test-type is 2 or 3.
          pieces_to_fill_empty_spaces_with = nil
          if test_type == 2
            pieces_to_fill_empty_spaces_with = (colour == 1 ? get_pieces(false, false, true) : get_pieces(false, true, false))
          elsif test_type == 3
            pieces_to_fill_empty_spaces_with = (colour == 1 ? get_pieces(false, true, false) : get_pieces(false, false, true))
            
            #For this test type, knights shouldn't be included in the piece 
            #selection since this will cause a problem later when the location 
            #of the knight to move is determined since there may be > 1 knight 
            #of the same colour and the incorrect one may be chosen to be moved. 
            #Therefore, only 1 knight should be on the board.
            pieces_to_fill_empty_spaces_with.delete_if {|x| x == piece}
          end

          #Randomly place a piece from the set created above onto each empty
          #coordinate on the board.
          if pieces_to_fill_empty_spaces_with != nil and !pieces_to_fill_empty_spaces_with.empty?
            for coordinate in 0...71
              if board_to_construct[coordinate] == "."
                board_to_construct[coordinate] = pieces_to_fill_empty_spaces_with.sample
              end
            end
          end

          #################################################################
          ##### CONSTRUCT THE BOARD AND GET KNIGHT TO MOVE'S LOCATION #####
          #################################################################

          # Construct the board
          board = ChessDomain.constructBoard(board_to_construct)

          #Get the board-specific coordinates of the knight, these are required to
          #calculate the moves expected to be returned by the function.
          knight_location = []
          expected_moves = []

          for col in 0...board.getWidth()
            for row in 0...board.getHeight()
              if board.getSquareContents(col, row).getObjectType() == piece 
                knight_location.push(col)
                knight_location.push(row)
              end
            end
          end

          ##########################################################
          ##### GENERATE EXPECTED MOVES AND SQUARES CONSIDERED #####
          ##########################################################

          if knight_location[1] < 6
            if knight_location[0] > 0
              expected_moves.push(Square.new(knight_location[0] - 1, knight_location[1] + 2))
            end
            if knight_location[0] < 7
              expected_moves.push(Square.new(knight_location[0] + 1, knight_location[1] + 2))
            end
          end

          if knight_location[1] > 1
            if knight_location[0] > 0
              expected_moves.push(Square.new(knight_location[0] - 1, knight_location[1] - 2))
            end
            if knight_location[0] < 7
              expected_moves.push(Square.new(knight_location[0] + 1, knight_location[1] - 2))
            end
          end

          if knight_location[0] < 6
            if knight_location[1] > 0
              expected_moves.push(Square.new(knight_location[0] + 2, knight_location[1] - 1))
            end
            if knight_location[1] < 7
              expected_moves.push(Square.new(knight_location[0] + 2, knight_location[1] + 1))
            end
          end

          if knight_location[0] > 1
            if knight_location[1] > 0
              expected_moves.push(Square.new(knight_location[0] - 2, knight_location[1] - 1))
            end
            if knight_location[1] < 7
              expected_moves.push(Square.new(knight_location[0] - 2, knight_location[1] + 1))
            end
          end

          #The number of squares considered will always be equal to the number 
          #of expected moves multiplied by 3 (knights always move 3 squares to 
          #their destination).
          expected_squares_considered = expected_moves.size * 3

          #If test type equals 3, no moves should be made but the number of 
          #squares considered should be equal to the value of how many moves would
          #potentially be expected so clear the "expected_moves" variable after 
          #the "expected_squares_considered" variable is calculated.
          if test_type == 3
            expected_moves = []
          end

          ####################################
          ##### GET KNIGHT MOVEMENT DATA #####
          ####################################

          moves_and_squares_considered = ChessDomain.getKnightMoves(board, Square.new(knight_location[0], knight_location[1]))
          moves = moves_and_squares_considered[0].to_a
          squares_considered = moves_and_squares_considered[1]

          #################
          ##### TESTS #####
          #################

          assert_equal(
            expected_moves.size, 
            moves.size,
            "occurred when checking the number of expected moves against the number
            of moves generated for test type " + test_type.to_s + " with a " +
            piece_colour + " piece"
          )

          assert_equal(
            expected_squares_considered, 
            squares_considered,
            "occurred when checking the number of squares expected to be considered
            against the number of squares actually considered for test type " + 
            test_type.to_s + " with a " + piece_colour + " piece"
          )

          #Check the actual contents of "moves" against "expected_moves" using 
          #"include?" so that the order of squares in "expected_moves" doesn't have
          #an effect on the test outcome.  To do this, all Square objects in "moves"
          #and "expected_moves" need to be converted to Strings otherwise, the test
          #will always fail since it will be comparing two different objects. Can 
          #do this using a for loop for both the "moves" and "expected_moves" arrays 
          #that only considers the length of the "moves" array since it was proven 
          #above that these two arrays are the same length.
          moves.map!(&:to_s)
          expected_moves.map!(&:to_s)

          for expected_move in expected_moves
            assert_true(
              moves.include?(expected_move),
              "occurred when checking if the expected move " + expected_move + 
              "is contained in the moves generated for test type " + test_type.to_s +
              " with a " + piece_colour + " piece"
            )
          end
        end
      end
    end
  end
end

################################################################################
# To test the "getKingMoves()" function, the function is first invoked with a 
# square specifying every piece other than a white/black king to see if an
# exception is thrown correctly.  Following this, a white/black king is placed 
# upon each coordinate on a chess board and three types of test are conducted:
# 
# 1. Board is empty except for the king
# 2. Board is full of pieces of a different colour to the king
# 3. Board is full of pieces of the same colour as the king
# 
# For each test type, the number and coordinates of squares that can be moved to
# are checked along with the number of squares considered.
#
# In the first test type, the king should be able to move freely except to
# squares that are not "on the board".  For instance, if the king is on rank
# "a", file 1 it should not be able to move to the left or down.  In the second 
# test type, the squares moved to and considered should not differ from the 
# first test type since the function considers any square occupied by an 
# opposing piece colour as a square that can be moved to. For the third test 
# type, the king should not be able to move anywhere but will consider a 
# number of squares to move to.
#
# The test is run for both white and black kings to ensure that the function
# operates correctly for both colours.
unit_test "get_king_moves" do
  for colour in 1..2
    piece = (colour == 1 ? "K" : "k") 

    #Used in error messages.
    piece_colour = (colour == 1 ? "white" : "black") 

    check_exception_thrown_by_get_moves_method_for_piece(piece)

    for test_type in 1..3
    
      #Each "coordinate" is a character in the "board_to_construct" string below.
      for coordinate in 0...71
        board_to_construct = 
          "......../" +
          "......../" +
          "......../" +
          "......../" +
          "......../" +
          "......../" +
          "......../" +
          "........"

        #Perform a test if the "coordinate" on the board is an empty square 
        #(ignore new row characters).
        if board_to_construct[coordinate] == "."

          #####################################
          ##### SET-UP BOARD TO CONSTRUCT #####
          #####################################

          #Put the king on the board coordinate specified.
          board_to_construct[coordinate] = piece

          #Fill the empty spaces with pieces, if the test-type is 2 or 3.
          pieces_to_fill_empty_spaces_with = nil
          if test_type == 2
            pieces_to_fill_empty_spaces_with = (colour == 1 ? get_pieces(false, false, true) : get_pieces(false, true, false))
          elsif test_type == 3
            pieces_to_fill_empty_spaces_with = (colour == 1 ? get_pieces(false, true, false) : get_pieces(false, false, true))
            
            #For this test type, kings shouldn't be included in the piece 
            #selection since this will cause a problem later when the location 
            #of the king to move is determined since there may be > 1 king 
            #of the same colour and the incorrect one may be chosen to be moved. 
            #Therefore, only 1 king should be on the board.
            pieces_to_fill_empty_spaces_with.delete_if {|x| x == piece}
          end

          #Randomly place a piece from the set created above onto each empty
          #coordinate on the board.
          if pieces_to_fill_empty_spaces_with != nil and !pieces_to_fill_empty_spaces_with.empty?
            for coordinate in 0...71
              if board_to_construct[coordinate] == "."
                board_to_construct[coordinate] = pieces_to_fill_empty_spaces_with.sample
              end
            end
          end

          ###############################################################
          ##### CONSTRUCT THE BOARD AND GET KING TO MOVE'S LOCATION #####
          ###############################################################

          # Construct the board
          board = ChessDomain.constructBoard(board_to_construct)

          #Get the board-specific coordinates of the knight, these are required to
          #calculate the moves expected to be returned by the function.
          king_location = []
          expected_moves = []

          for col in 0...board.getWidth()
            for row in 0...board.getHeight()
              if board.getSquareContents(col, row).getObjectType() == piece 
                king_location.push(col)
                king_location.push(row)
              end
            end
          end

          ##########################################################
          ##### GENERATE EXPECTED MOVES AND SQUARES CONSIDERED #####
          ##########################################################

          if king_location[1] > 0
            expected_moves.push(Square.new(king_location[0], king_location[1] - 1))
          end
          if king_location[1] < 7
            expected_moves.push(Square.new(king_location[0], king_location[1] + 1))
          end
          if king_location[0] > 0
            expected_moves.push(Square.new(king_location[0] - 1, king_location[1]))
          end
          if king_location[0] < 7
            expected_moves.push(Square.new(king_location[0] + 1, king_location[1]))
          end
          if king_location[1] > 0 and king_location[0] > 0
            expected_moves.push(Square.new(king_location[0] - 1, king_location[1] - 1))
          end
          if king_location[1] > 0 and king_location[0] < 7
            expected_moves.push(Square.new(king_location[0] + 1, king_location[1] - 1))
          end
          if king_location[1] < 7 and king_location[0] > 0
            expected_moves.push(Square.new(king_location[0] - 1, king_location[1] + 1))
          end
          if king_location[1] < 7 and king_location[0] < 7
            expected_moves.push(Square.new(king_location[0] + 1, king_location[1] + 1))
          end

          #The number of squares considered will always be equal to the number of
          #expected moves (kings always move 1 square to their destination).
          expected_squares_considered = expected_moves.size

          #If test type equals 3, no moves should be made but the number of 
          #squares considered should be equal to the value of how many moves would
          #potentially be expected so clear the "expected_moves" variable after 
          #the "expected_squares_considered" variable is calculated.
          if test_type == 3
            expected_moves = []
          end

          ##################################
          ##### GET KING MOVEMENT DATA #####
          ##################################

          moves_and_squares_considered = ChessDomain.getKingMoves(board, Square.new(king_location[0], king_location[1]))
          moves = moves_and_squares_considered[0].to_a
          squares_considered = moves_and_squares_considered[1]

          #################
          ##### TESTS #####
          #################

          assert_equal(
            expected_moves.size, 
            moves.size,
            "occurred when checking the number of expected moves against the number
            of moves generated for test type " + test_type.to_s
          )

          assert_equal(
            expected_squares_considered, 
            squares_considered,
            "occurred when checking the number of squares expected to be considered
            against the number of squares actually considered for test type " + 
            test_type.to_s
          )

          #Check the actual contents of "moves" against "expected_moves" using 
          #"include?" so that the order of squares in "expected_moves" doesn't have
          #an effect on the test outcome.  To do this, all Square objects in "moves"
          #and "expected_moves" need to be converted to Strings otherwise, the test
          #will always fail since it will be comparing two different objects. Can 
          #do this using a for loop for both the "moves" and "expected_moves" arrays 
          #that only considers the length of the "moves" array since it was proven 
          #above that these two arrays are the same length.
          moves.map!(&:to_s)
          expected_moves.map!(&:to_s)

          for expected_move in expected_moves
            assert_true(
              moves.include?(expected_move),
              "occurred when checking if the expected move " + expected_move + 
              "is contained in the moves generated for test type " + test_type.to_s
            )
          end
        end
      end
    end
  end
end

################################################################################
# The "getQueenMoves()" function is essentially a wrapper for the "lineMoves()"
# function and simply checks if the Square specified as input to the function
# contains a black/white queen and passes delta values resulting in cardinal and
# primary inter-cardinal compass direction line moves to be projected.
#
# Since the "lineMoves()" functionality is checked in a test previous to this, 
# if all is well, this test just needs to ensure that the correct lines are 
# drawn.  Thus, this test does not need to check if a line move is terminated 
# when a piece is encountered so a chess board containing 1 queen and a check of 
# the moves returned is sufficient.  Note that the number of squares considered 
# when getting the queen's moves is not checked since this is verified in the 
# "linesMoves()" test.
unit_test "get_queen_moves" do
  
  for colour in 1..2
    piece = (colour == 1 ? "Q" : "q")
    piece_colour = (colour == 1 ? "white" : "black")
  
    check_exception_thrown_by_get_moves_method_for_piece(piece)
  
    ##################################################
    ##### CONSTRUCT BOARD AND GET QUEEN LOCATION #####
    ##################################################
    
    board_to_construct = 
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "........"

    board_to_construct[31] = piece
    board = ChessDomain.constructBoard(board_to_construct)
    queen_location = []
    
    for col in 0...board.getWidth()
      for row in 0...board.getHeight()
        if board.getSquareContents(col, row).getObjectType() == piece 
          queen_location.push(col)
          queen_location.push(row)
        end
      end
    end
    
    ####################################
    ##### CONSTRUCT EXPECTED MOVES #####
    ####################################

    expected_moves = []
    
    #North movements
    for row in (queen_location[1] + 1)..7 
      expected_moves.push(Square.new(queen_location[0], row)) 
    end

    #North-east movements
    col = (queen_location[0] + 1)
    for row in (queen_location[1] + 1)..7
      expected_moves.push(Square.new(col, row))
      col += 1
    end

    #East movements
    for col in (queen_location[0] + 1)..7 
      expected_moves.push(Square.new(col, queen_location[1])) 
    end

    #South-east movements
    col = (queen_location[0] + 1)
    for row in (queen_location[1] - 1).downto(0)
      expected_moves.push(Square.new(col, row))
      col += 1
    end

    #South movements
    for row in (queen_location[1] - 1).downto(0)
      expected_moves.push(Square.new(queen_location[0], row)) 
    end

    #South-west movements
    col = (queen_location[0] - 1)
    for row in (queen_location[1] - 1).downto(0)
      expected_moves.push(Square.new(col, row))
      col -= 1
    end

    #West movements
    for col in (queen_location[0] - 1).downto(0)
      expected_moves.push(Square.new(col, queen_location[1])) 
    end

    #North-west movements
    col = (queen_location[0] - 1)
    for row in (queen_location[1] + 1)..7
      expected_moves.push(Square.new(col, row))
      col -= 1
    end
    
    #Remove any squares whose row/col = 8 (this is not a valid board-as-scene 
    #coordinate
    expected_moves.delete_if {|square| square.getColumn() == 8 || square.getRow() == 8}
    
    ###################################
    ##### GET QUEEN MOVEMENT DATA #####
    ###################################
    
    moves_and_squares_considered = ChessDomain.getQueenMoves(board, Square.new(queen_location[0], queen_location[1]))
    moves = moves_and_squares_considered[0].to_a

    #################
    ##### TESTS #####
    #################
    
    assert_equal(
      expected_moves.length, 
      moves.length, 
      "occurred when checking the number of moves expected to be returned with a " +
      piece_colour + " queen"
    )
    
    moves.map!(&:to_s)
    expected_moves.map!(&:to_s)
    
    for expected_move in expected_moves
      assert_true(
        moves.include?(expected_move),
        "occurred when checking if the move " + expected_move + " is present in 
        the moves returned with a " + piece_colour + " queen"
      )
    end
  end
end

################################################################################
# The "getRookMoves()" function is essentially a wrapper for the "lineMoves()"
# function and simply checks if the Square specified as input to the function
# contains a black/white rook and passes delta values resulting in cardinal 
# compass direction line moves to be projected.
#
# Since the "lineMoves()" functionality is checked in a test previous to this, 
# if all is well, this test just needs to ensure that the correct lines are 
# drawn.  Thus, this test does not need to check if a line move is terminated 
# when a piece is encountered so a chess board containing 1 rook and a check of 
# the moves returned is sufficient.  Note that the number of squares considered 
# when getting the rook's moves is not checked since this is verified in the 
# "linesMoves()" test.
unit_test "get_rook_moves" do
  
  for colour in 1..2
    piece = (colour == 1 ? "R" : "r")
    piece_colour = (colour == 1 ? "white" : "black")
  
    check_exception_thrown_by_get_moves_method_for_piece(piece)
  
    ##################################################
    ##### CONSTRUCT BOARD AND GET QUEEN LOCATION #####
    ##################################################
    
    board_to_construct = 
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "........"

    board_to_construct[31] = piece
    board = ChessDomain.constructBoard(board_to_construct)
    rook_location = []
    
    for col in 0...board.getWidth()
      for row in 0...board.getHeight()
        if board.getSquareContents(col, row).getObjectType() == piece 
          rook_location.push(col)
          rook_location.push(row)
        end
      end
    end
    
    ####################################
    ##### CONSTRUCT EXPECTED MOVES #####
    ####################################

    expected_moves = []
    
    #North movements
    for row in (rook_location[1] + 1)..7 
      expected_moves.push(Square.new(rook_location[0], row)) 
    end

    #East movements
    for col in (rook_location[0] + 1)..7 
      expected_moves.push(Square.new(col, rook_location[1])) 
    end

    #South movements
    for row in (rook_location[1] - 1).downto(0)
      expected_moves.push(Square.new(rook_location[0], row)) 
    end

    #West movements
    for col in (rook_location[0] - 1).downto(0)
      expected_moves.push(Square.new(col, rook_location[1])) 
    end
    
    #Remove any squares whose row/col = 8 (this is not a valid board-as-scene 
    #coordinate
    expected_moves.delete_if {|square| square.getColumn() == 8 || square.getRow() == 8}
    
    ##################################
    ##### GET ROOK MOVEMENT DATA #####
    ##################################
    
    moves_and_squares_considered = ChessDomain.getRookMoves(board, Square.new(rook_location[0], rook_location[1]))
    moves = moves_and_squares_considered[0].to_a

    #################
    ##### TESTS #####
    #################
    
    assert_equal(
      expected_moves.length, 
      moves.length, 
      "occurred when checking the number of moves expected to be returned with a " +
      piece_colour + " rook"
    )
    
    moves.map!(&:to_s)
    expected_moves.map!(&:to_s)
    
    for expected_move in expected_moves
      assert_true(
        moves.include?(expected_move),
        "occurred when checking if the move " + expected_move + " is present in 
        the moves returned with a " + piece_colour + " rook"
      )
    end
  end
end

################################################################################
# The "getBishopMoves()" function is essentially a wrapper for the "lineMoves()"
# function and simply checks if the Square specified as input to the function
# contains a black/white bishop and passes delta values resulting in primary 
# inter-cardinal compass direction line moves to be projected.
#
# Since the "lineMoves()" functionality is checked in a test previous to this, 
# if all is well, this test just needs to ensure that the correct lines are 
# drawn.  Thus, this test does not need to check if a line move is terminated 
# when a piece is encountered so a chess board containing 1 bishop and a check 
# of the moves returned is sufficient.  Note that the number of squares 
# considered when getting the queen's moves is not checked since this is 
# verified in the "linesMoves()" test.
unit_test "get_bishop_moves" do
  
  for colour in 1..2
    piece = (colour == 1 ? "B" : "b")
    piece_colour = (colour == 1 ? "white" : "black")
  
    check_exception_thrown_by_get_moves_method_for_piece(piece)
  
    ###################################################
    ##### CONSTRUCT BOARD AND GET BISHOP LOCATION #####
    ###################################################
    
    board_to_construct = 
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "........"

    board_to_construct[31] = piece
    board = ChessDomain.constructBoard(board_to_construct)
    bishop_location = []
    
    for col in 0...board.getWidth()
      for row in 0...board.getHeight()
        if board.getSquareContents(col, row).getObjectType() == piece 
          bishop_location.push(col)
          bishop_location.push(row)
        end
      end
    end
    
    ####################################
    ##### CONSTRUCT EXPECTED MOVES #####
    ####################################

    expected_moves = []

    #North-east movements
    col = (bishop_location[0] + 1)
    for row in (bishop_location[1] + 1)..7
      expected_moves.push(Square.new(col, row))
      col += 1
    end

    #South-east movements
    col = (bishop_location[0] + 1)
    for row in (bishop_location[1] - 1).downto(0)
      expected_moves.push(Square.new(col, row))
      col += 1
    end

    #South-west movements
    col = (bishop_location[0] - 1)
    for row in (bishop_location[1] - 1).downto(0)
      expected_moves.push(Square.new(col, row))
      col -= 1
    end

    #North-west movements
    col = (bishop_location[0] - 1)
    for row in (bishop_location[1] + 1)..7
      expected_moves.push(Square.new(col, row))
      col -= 1
    end
    
    #Remove any squares whose row/col = 8 (this is not a valid board-as-scene 
    #coordinate
    expected_moves.delete_if {|square| square.getColumn() == 8 || square.getRow() == 8}
    
    ####################################
    ##### GET BISHOP MOVEMENT DATA #####
    ####################################
    
    moves_and_squares_considered = ChessDomain.getBishopMoves(board, Square.new(bishop_location[0], bishop_location[1]))
    moves = moves_and_squares_considered[0].to_a

    #################
    ##### TESTS #####
    #################
    
    assert_equal(
      expected_moves.length, 
      moves.length, 
      "occurred when checking the number of moves expected to be returned with a " +
      piece_colour + " bishop"
    )
    
    moves.map!(&:to_s)
    expected_moves.map!(&:to_s)
    
    for expected_move in expected_moves
      assert_true(
        moves.include?(expected_move),
        "occurred when checking if the move " + expected_move + " is present in 
        the moves returned with a " + piece_colour + " bishop"
      )
    end
  end
end

################################################################################
unit_test "constructor" do
  model = Chrest.new(0, false)
  
  # Check if exceptions are thrown as expected when differing values for the
  # initial fixation threshold constructor parameter are provided.
  for i in 1..3
    initial_fixation_threshold = (i == 1 ? -1 : i == 2 ? 0 : 1) 
    exception_thrown = false
    begin
      ChessDomain.new(model, initial_fixation_threshold, 3, 5)
    rescue
      exception_thrown = true
    end

    assert_equal(
      ([1, 2].include?(i) ? true : false),
      exception_thrown,
      "occurred when checking if an exception is thrown when the initial fixation 
      threshold parameter is set to " + initial_fixation_threshold.to_s + " and 
      all other constructor parameters are valid."
    )
  end
  
  # Check if exceptions are thrown as expected when differing values for the
  # maximum peripheral square fixation attempts constructor parameter are 
  # provided.
  for i in 1..3
    max_periphery_fixation_attempts = (i == 1 ? -1 : i == 2 ? 0 : 1)
    exception_thrown = false
    begin
      ChessDomain.new(model, 4, max_periphery_fixation_attempts, 5)
    rescue
      exception_thrown = true
    end
    
    assert_equal(
      ([1, 2].include?(i) ? true : false),
      exception_thrown,
      "occurred when checking if an exception is thrown when the maximum periphery
      fixation attempts parameter is set to " + max_periphery_fixation_attempts.to_s + 
      " and all other constructor parameters are valid."
    )
  end
  
  # Check if exceptions are thrown as expected when differing values for the
  # maximum fixations in set constructor parameter are provided.
  initial_fixation_threshold = 4
  for i in 1..3
    max_fixations_in_set = (
      i == 1 ? 
        initial_fixation_threshold - 1 : 
        i == 2 ? 
          initial_fixation_threshold : 
          initial_fixation_threshold + 1
    ) 
    
    exception_thrown = false
    begin
      ChessDomain.new(model, initial_fixation_threshold, 3, max_fixations_in_set)
    rescue
      exception_thrown = true
    end

    assert_equal(
      (i == 1 ? true : false),
      exception_thrown,
      "occurred when checking if an exception is thrown when the maximum fixations " +
      "in set parameter is set to " + max_fixations_in_set.to_s + " and " +
      "all other constructor parameters are valid."
    )
  end
  
  #In the tests above, there will have been ChessDomain instances correctly 
  #created so no explicit test is performed to check this.
end

################################################################################
unit_test "normalisation" do
  
  list_pattern = ListPattern.new
  white_and_black_pieces = get_pieces(false, true, true);
  for piece in white_and_black_pieces
    list_pattern.add(ItemSquarePattern.new(piece, 0, 2))
    list_pattern.add(ItemSquarePattern.new(piece, 0, 1))
    list_pattern.add(ItemSquarePattern.new(piece, 2, 0))
    list_pattern.add(ItemSquarePattern.new(piece, 1, 0))
  end
  
  expected_list_pattern = ListPattern.new
  white_pieces_canonical_order = ["P", "K", "B", "N", "Q", "R"]
  for piece in white_pieces_canonical_order
    for i in 1..2
      piece = (i == 1 ? piece : piece.downcase)
      expected_list_pattern.add(ItemSquarePattern.new(piece, 0, 1))
      expected_list_pattern.add(ItemSquarePattern.new(piece, 0, 2))
      expected_list_pattern.add(ItemSquarePattern.new(piece, 1, 0))
      expected_list_pattern.add(ItemSquarePattern.new(piece, 2, 0))
    end
  end

  normalised_list_pattern = ChessDomain.new(Chrest.new(0, false), 4, 3, 8).normalise(list_pattern)
  
  assert_equal(
    normalised_list_pattern.to_s,
    expected_list_pattern.to_s
  )
end

################################################################################
# Simply checks if the "getInitialFixationInSet()" method returns an instance of 
# CentralFixation.  Variables for this instance are checked in the 
# CentralFixation tests.
unit_test "get_initial_fixation_in_set" do
  50.times do
    initial_fixation = ChessDomain.new(Chrest.new(0, false), 4, 3, 8).getInitialFixationInSet(0)
    assert_true(initial_fixation.java_kind_of?(CentralFixation))
  end
end

################################################################################
# Checks operation of the "getNonInitialFixationInSet" function.
# 
# Note that none of the Fixations constructed are actually "performed", rather,
# their variables are set as though they are in the normal course of running a
# simulation with CHREST.  Thus, some variables may not make much sense with 
# respect to the values they are set with however, other tests ensure that such 
# variables are set correctly.
unit_test "get_non_initial_fixation_in_set" do
  
  ########################################
  ##### SET-UP INSTANCE FIELD ACCESS #####
  ########################################
  
  # Fixation instance variables need to be set so they appear to have been 
  # "performed", grant access to these variables.
  Fixation.class_eval{ 
    field_accessor :_scene, :_performanceTime, :_timeDecidedUpon, :_performed, :_colFixatedOn, :_rowFixatedOn, :_objectSeen
  }
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  # Scene dimensions need to be accessed at times, grant access here.
  scene_width_field = Scene.java_class.declared_field("_width")
  scene_width_field.accessible = true
  scene_height_field = Scene.java_class.declared_field("_height")
  scene_height_field.accessible = true
  
  # Particular Fixation data structures in a CHREST model and Perceiver are 
  # integral to the operation of the function being tested and need to be 
  # manipulated precisely.  Access to these data structures is enabled here.
  Chrest.class_eval{
    field_accessor :_fixationsScheduled, :_saccadeTime
  }
  perceiver_fixations_field = Perceiver.java_class.declared_field("_fixations")
  perceiver_fixations_field.accessible = true
  
  #####################
  ##### MAIN LOOP #####
  #####################
  
  #Some Fixations returned when a HypothesisDiscriminationFixation has not been 
  #performed successfully can return null depending on the previous Fixation 
  #made.  Essentially, this should be allowed to occur since the code's ability 
  #to deal with this needs to be verified.  However, its not possible to 
  #determine if null was returned when a Fixation was generated under these
  #circumstances so the best solution is to run the test a number of times to 
  #ensure that all possible situations can occur and are handled.
  200.times do
    
    time = 0

    # Need to be able to specify if a CHREST model is experienced "on-the-fly"
    # otherwise, when trying to get non-initial fixations, the model would have 
    # to have a certain number of Nodes, n, in LTM to return "true" when the 
    # model's "experienced" status is queried when determining if a 
    # GlobalStrategyFixation or a PeripheralItemFixation should be made.  Thus, 
    # if n is changed this test will break in addition, performing this learning 
    # in a test adds extra code that will just complicate an already complex 
    # test!
    #
    # To circumvent this, subclass the "Chrest" java class with a jRuby class 
    # that will be used in place of the "Chrest" java class in this test. In the 
    # subclass, override "Chrest.isExperienced()" (the method used to determine 
    # the "experienced" status of a CHREST model) and have it return a class 
    # variable (for the subclass) that can be set at will.
    model = Class.new(Chrest) {
      @@experienced = false

      def isExperienced(x)
        return @@experienced
      end

      def setExperienced(bool)
        @@experienced = bool
      end
    }.new(time, false)

    #########################
    ##### DOMAIN SET-UP #####
    #########################

    intial_fixation_threshold = 4
    chess_domain = ChessDomain.new(model, intial_fixation_threshold, 3, 8)

    ########################
    ##### BOARD SET-UP #####
    ########################

    chess_board = 
      "rnbqkbnr/" +
      "pppppppp/" +
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "PPPPPPPP/" +
      "RNBQKBNR"
    board = ChessDomain.constructBoard(chess_board)

    ########################################################
    ##### SET-UP FIXATION DATA STRUCTURE FOR PERCEIVER #####
    ########################################################
    
    fixations_attempted = ArrayList.new()

    ######################################################################
    ##### GET FIXATIONS WHEN INITIAL FIXATIONS THRESHOLD NOT REACHED #####
    ######################################################################

    # Populate the model's perceiver fixations with x performed fixations where
    # x is equal to intial_fixation_threshold.
    time += 50
    intial_fixation_threshold.times do
      
      # Get Fixation and check its class
      fixation = chess_domain.getNonInitialFixationInSet(time)
      assert_true(
        fixation.java_kind_of?(SalientManFixation),
        "occurred when checking the type of Fixation returned when initial " +
        "fixations have not been completed yet"
      )
      
      # Add Fixation to CHREST model's "_fixationsScheduled" data structure.
      fixations_scheduled = ArrayList.new()
      fixations_scheduled.add(fixation)
      model._fixationsScheduled.put(time.to_java(:int), fixations_scheduled)
      
      # Set Fixation variables so it has been "performed"
      fixation._performed = true
      fixation._performanceTime = fixation._timeDecidedUpon + model._saccadeTime
      fixation._scene = board
      fixation._colFixatedOn = 0
      fixation._rowFixatedOn = 1
      fixation._objectSeen = board._scene.get(fixation._colFixatedOn).get(fixation._rowFixatedOn)
      # The value that fixation._objectSeen above is a good example of the 
      # nonsensical value setting mentioned in the preamble to this test.

      # Remove/add the Fixation from/to the CHREST model's/Perceiver's Fixation 
      # data structure
      fixations_scheduled = ArrayList.new()
      model._fixationsScheduled.put(fixation._performanceTime.to_java(:int), fixations_scheduled)
      fixations_attempted.add(fixation)
      perceiver_fixations_field.value(model.getPerceiver()).put(fixation._performanceTime.to_java(:int), fixations_attempted)
      
      # Advance time
      time = fixation._performanceTime + 300
    end
    
    ##################################################################
    ##### GET FIXATIONS WHEN INITIAL FIXATIONS THRESHOLD REACHED #####
    ##################################################################

    # When the initial fixations threshold has been reached, a 
    # HypothesisDiscriminationFixation should always be returned unless:
    # 
    # 1. A HypothesisDiscriminationFixation is scheduled to be performed but has
    #    not been performed when the function is invoked. 
    # 2. The previous Fixation attempted was a HypothesisDiscriminationFixation 
    #    but wasn't performed successfully.  
    # 
    # In these cases, an AttackDefenseFixation, GlobalStrategyFixation, 
    # PeripheralItemFixation or PeripheralSquareFixation should be returned.
    # Note that the function will return either a GlobalStrategyFixation or a
    # PeripheralItemFixation depending on whether the CHREST model invoking the
    # function is experienced or not (experienced: GlobalStrategyFixation, 
    # inexperienced: PeripheralItemFixation).
    # 
    # Since there is an equal probability of generating these Fixation types in 
    # the cases described, the function will be invoked until all these Fixation 
    # types have been returned in both cases.  To facilitate this, create 
    # boolean flags that indicate whether each Fixation has been returned in 
    # each case and set them to false initially.
    
    # Boolean flags when function is invoked before 
    # HypothesisDiscriminationFixation is performed.
    attack_defense_fixation_returned_before_hypothesis_discrimination_fixation_performed = false
    global_strategy_or_peripheral_item_fixation_returned_before_hypothesis_discrimination_fixation_performed = false
    peripheral_square_fixation_returned_before_hypothesis_discrimination_fixation_performed = false
    
    # Boolean flags when function is invoked after
    # HypothesisDiscriminationFixation is performed.
    attack_defense_fixation_returned_after_hypothesis_discrimination_fixation_performed = false
    global_strategy_or_peripheral_item_fixation_returned_after_hypothesis_discrimination_fixation_performed = false
    peripheral_square_fixation_returned_after_hypothesis_discrimination_fixation_performed = false
    
    # Function invocation loop
    while 
      !attack_defense_fixation_returned_before_hypothesis_discrimination_fixation_performed or 
      !global_strategy_or_peripheral_item_fixation_returned_before_hypothesis_discrimination_fixation_performed
      !peripheral_square_fixation_returned_before_hypothesis_discrimination_fixation_performed or
      !attack_defense_fixation_returned_after_hypothesis_discrimination_fixation_performed or
      !global_strategy_or_peripheral_item_fixation_returned_after_hypothesis_discrimination_fixation_performed or
      !peripheral_square_fixation_returned_after_hypothesis_discrimination_fixation_performed
    
      ############################################################
      ##### GET HypothesisDiscriminationFixation AND PERFORM #####
      ############################################################
      
      # Get the next Fixation from the function.  This should be a 
      # HypothesisDiscriminationFixation instance since:
      #
      # 1. The function is invoked for the first time after the initial fixation
      #    threshold has been reached (first iteration of while loop).
      # 2. The function is invoked after a HypothesisDiscriminationFixation has 
      #    been attempted but performed unsuccessfully (iteration 2+ of while 
      #    loop).
      fixation = chess_domain.getNonInitialFixationInSet(time)
      assert_equal(
        HypothesisDiscriminationFixation.java_class,
        fixation.java_class,
        "occurred when checking the type of Fixation returned after initial " + 
        "fixations have been completed and a hypothesis-discrimination fixation " +
        "hasn't been attempted"
      )
      
      # Add the Fixation to the CHREST model's Fixations scheduled data 
      # structure.
      fixations_scheduled = ArrayList.new()
      fixations_scheduled.add(fixation)
      model._fixationsScheduled.put(time.to_java(:int), fixations_scheduled)
      
      # Set Fixation variables that would be set if the Fixation were performed
      # "properly"
      fixation._performanceTime = fixation._timeDecidedUpon + model._saccadeTime
      fixation._scene = board
      
      # Remove/add the last, unperformed, HypothesisDiscriminationFixation 
      # instance from/to the CHREST model's/Perceiver's Fixation data structure
      fixations_scheduled = ArrayList.new()
      model._fixationsScheduled.put(fixation._performanceTime.to_java(:int), fixations_scheduled)
      fixations_attempted.add(fixation)
      perceiver_fixations_field.value(model.getPerceiver()).put(fixation._performanceTime.to_java(:int), fixations_attempted)
      
      ##########################################################################
      ##### GET FIXATION BEFORE HypothesisDiscriminationFixation PERFORMED #####
      ##########################################################################
      
      # Invoke the function before the time the HypothesisDiscriminationFixation
      # was performed to see if the correct type of Fixation is performed when a 
      # Hypothesis DiscriminationFixation is scheduled to be performed but 
      # hasn't been performed yet.  This Fixation will not be added to the 
      # Fixation data structures, however. 
      time_before_hypothesis_discrimination_fixation_performed = rand(time...fixation._performanceTime)
      fixation_returned_before_hypothesis_discrimination_fixation_performed = chess_domain.getNonInitialFixationInSet(
        time_before_hypothesis_discrimination_fixation_performed
      )
      
      assert_true(
        (
          fixation_returned_before_hypothesis_discrimination_fixation_performed.java_kind_of?(AttackDefenseFixation) || 
          fixation_returned_before_hypothesis_discrimination_fixation_performed.java_kind_of?(
            model.isExperienced(time_before_hypothesis_discrimination_fixation_performed) ? 
              GlobalStrategyFixation : 
              PeripheralItemFixation
          ) ||
          fixation_returned_before_hypothesis_discrimination_fixation_performed.java_kind_of?(PeripheralSquareFixation)
        ),
        "occurred when checking the Fixation class returned by the function when " +
        "a HypothesisDiscriminationFixation is scheduled for performance but " +
        "hasn't been performed when the function is invoked.  Fixation returned:" +
        fixation_returned_before_hypothesis_discrimination_fixation_performed.toString()
      )
      
      # Set while loop control variable accordingly.
      case fixation_returned_before_hypothesis_discrimination_fixation_performed.java_class
      when AttackDefenseFixation.java_class
        attack_defense_fixation_returned_before_hypothesis_discrimination_fixation_performed = true
      when model.isExperienced(time_before_hypothesis_discrimination_fixation_performed) ? GlobalStrategyFixation.java_class : PeripheralItemFixation.java_class
        global_strategy_or_peripheral_item_fixation_returned_before_hypothesis_discrimination_fixation_performed = true
      when PeripheralSquareFixation.java_class
        peripheral_square_fixation_returned_before_hypothesis_discrimination_fixation_performed = true
      end
      
      #############################
      ##### GET NEXT FIXATION #####
      #############################
      
      # Advance time
      time = fixation._performanceTime + 100
      
      #Get the next fixation, this will be performed.
      fixation = chess_domain.getNonInitialFixationInSet(time)
      assert_true(
        (
          fixation_returned_before_hypothesis_discrimination_fixation_performed.java_kind_of?(AttackDefenseFixation) || 
          fixation_returned_before_hypothesis_discrimination_fixation_performed.java_kind_of?(
            model.isExperienced(time) ? 
              GlobalStrategyFixation : 
              PeripheralItemFixation
          ) ||
          fixation_returned_before_hypothesis_discrimination_fixation_performed.java_kind_of?(PeripheralSquareFixation)
        ),
        "occurred when checking the type of Fixation returned when the last " +
        "Fixation attempted was a HypothesisDiscriminationFixation but was not " +
        "performed successfully. Fixation returned was a " + 
        fixation.java_class.to_s + "."
      )

      #Set the relevant boolean value that controls the while loop 
      case fixation.java_class
      when AttackDefenseFixation.java_class
        attack_defense_fixation_returned_after_hypothesis_discrimination_fixation_performed = true
      when model.isExperienced(time) ? GlobalStrategyFixation.java_class : PeripheralItemFixation.java_class
        global_strategy_or_peripheral_item_fixation_returned_after_hypothesis_discrimination_fixation_performed = true
      when PeripheralSquareFixation.java_class
        peripheral_square_fixation_returned_after_hypothesis_discrimination_fixation_performed = true
      end
      
      # Add the Fixation to the CHREST model's Fixation to make data structure.
      fixations_scheduled = ArrayList.new()
      fixations_scheduled.add(fixation)
      model._fixationsScheduled.put(time.to_java(:int), fixations_scheduled)

      # "Perform" the fixation.  Note that the coordinates fixated on are 
      # randomly generated, this is because some Fixations returned when a 
      # HypothesisDiscriminationFixation has not been performed successfully 
      # can return null depending on the previous Fixation made.  Essentially,
      # this should be allowed to occur since the code's ability to deal with
      # this needs to be verified.
      time = fixation.getTimeDecidedUpon() + model._saccadeTime
      fixation._performanceTime = time
      fixation._performed = true
      fixation._scene = board
      fixation._colFixatedOn = rand(0...scene_width_field.value(board))
      fixation._rowFixatedOn =  rand(0...scene_height_field.value(board))
      fixation._objectSeen = board._scene.get(fixation._colFixatedOn).get(fixation._rowFixatedOn)
      # The value that fixation._objectSeen above is a good example of the 
      # nonsensical value setting mentioned in the preamble to this test.
  
      # Remove/add the last Fixation from/to the CHREST model's/Perceiver's 
      # Fixation data structure
      fixations_scheduled = ArrayList.new()
      model._fixationsScheduled.put(fixation._performanceTime.to_java(:int), fixations_scheduled)
      fixations_attempted.add(fixation)
      perceiver_fixations_field.value(model.getPerceiver()).put(fixation._performanceTime.to_java(:int), fixations_attempted)
      
      # Advance time
      time = fixation._performanceTime + 300
    end
  end
end

################################################################################
unit_test "should_learn_from_new_fixations" do
  
  # Since precise control is needed over what Fixations should be generated, 
  # the private "_scene" instance variable for the Fixations generated needs to
  # be accessible since "Fixation.perform()" and "Fixation.make()" should not be
  # used as these will extra code to be included that makes the test more 
  # complex.  If "_scene" can not be not accessed, NullPointerExceptions will be
  # thrown when certain Fixations are generated since these Fixations require 
  # access to previous Fixation's "_scene" variables and this variable can only 
  # be set (without using reflection) when "Fixation.perform()" is invoked.
  Fixation.class_eval{ field_writer :_scene }
    
  # Since "Fixation.perform()" is not to be used in this test, it is also 
  # necessary to invoke the private "Fixation.setPerformed()" method; this is 
  # only accessible when "Fixation.perform()" is used and sets the coordinates 
  # of the Square fixated on and the SceneObject "seen" when the Fixation 
  # occurs.  If these variables are not set for a Fixation, 
  # NullPointerExceptions will be thrown when other Fixations are being 
  # generated.
  set_performed_method = Fixation.java_class.declared_method(:setPerformed, Java::int, Java::int, SceneObject)
  set_performed_method.accessible = true
    
  time = 0
  chess_board_to_construct = 
    "rnbqkbnr/" +
    "pppppppp/" +
    "......../" +
    "......../" +
    "......../" +
    "......../" +
    "PPPPPPPP/" +
    "RNBQKBNR"
  
  model = Chrest.new(time += 5, false)
  perceiver = model.getPerceiver()
  max_peripheral_item_fixations = 3
  max_fixations_in_set = 8
  chess_domain = ChessDomain.new(model, 4, max_peripheral_item_fixations, max_fixations_in_set)
  chess_board = ChessDomain.constructBoard(chess_board_to_construct)
  
  assert_false(
    chess_domain.shouldLearnFromNewFixations(time - 3),
    "occurred when the Perceiver does not exist at the time the function is invoked"
  )
  
  assert_false(
    chess_domain.shouldLearnFromNewFixations(time),
    "occurred when no fixations have been added to the Perceiver at the time " +
    "the function is invoked"
  )
  
  #Make n fixations where n = maximum number of fixations in a set.
  for i in 1..max_fixations_in_set
    fixation = nil
    time += 100
    square_fixated_on = nil
    
    #First 5 fixations should not trigger any conditions in 
    #"shouldLearnFromNewFixations()" however the last 3 should:
    # - Fixation 6 should trigger the non-empty square condition ONLY
    # - Fixation 7 should trigger the GlobalStrategyFixation condition ONLY
    # - Fixation 8 should trigger the PeripheralItemFixation condition ONLY
    if(i == 1)  
      fixation = CentralFixation.new(time)
      square_fixated_on = Square.new(0,0)
    elsif(i == 2)  
      fixation = SalientManFixation.new(model, time)
      square_fixated_on = Square.new(1,0)
    elsif(i == 3)  
      fixation = HypothesisDiscriminationFixation.new(model, time) 
      square_fixated_on = Square.new(2,0)
    elsif(i == 4)  
      fixation = AttackDefenseFixation.new(model, chess_board, time) 
      square_fixated_on = Square.new(3,0)
    elsif(i == 5  || i == 6)  
      fixation = PeripheralSquareFixation.new(model, time) 
      square_fixated_on = (i == 5 ? Square.new(4,0) : Square.new(0,3))
    elsif(i == 7)  
      fixation = GlobalStrategyFixation.new(model, time) 
      square_fixated_on = Square.new(5,0)
    elsif(i == 8)  
      fixation = PeripheralItemFixation.new(model, max_peripheral_item_fixations, time) 
      square_fixated_on = Square.new(6,0)
    end
    
    fixation._scene = chess_board
    col = square_fixated_on.getColumn()
    row = square_fixated_on.getRow()
    set_performed_method.invoke(fixation, col, row, chess_board.getSquareContents(col, row))
    fixation.setPerformanceTime(fixation.getTimeDecidedUpon + 100)
    perceiver.addFixation(fixation)
    time += (fixation.getPerformanceTime() + 10)
    
    assert_equal(
      (i.between?(1, 5) ? false : true),
      chess_domain.shouldLearnFromNewFixations(time),
      "occurred when processing fixation " + i.to_s
    )
  end
end

################################################################################
# Test-specific function that returns pieces that can exist on the chess boards 
# used by CHREST.
def get_pieces(empty_squares, white_pieces, black_pieces)
  pieces = []
  if(empty_squares) then pieces.push(".") end
  if(white_pieces) then pieces.push(*["P","B","N","R","Q","K"]) end
  if(black_pieces) then pieces.push(*["p","b","n","r","q","k"]) end
  return pieces
end

################################################################################
# Test-specific function that checks to see if the "piece_to_check"s "getMoves"
# method throws an IllegalArgumentException when incorrect pieces are provided
# to it, i.e. does the "getPawnMoves" method throw an IllegalArgumentException
# if the square specified contains any piece other than a pawn?
def check_exception_thrown_by_get_moves_method_for_piece(piece_to_check)
  
  pieces = get_pieces(true, true, true)
  
  #Remove the white and black versions of the piece from pieces.
  pieces.delete_if{|piece| piece == piece_to_check.upcase || piece == piece_to_check.downcase}
  
  #Put each piece in pieces into a board to construct and then invoke the 
  #relevant "getMoves" function based upon "piece_to_check".  All pieces other
  #than the correct ones should be supplied as input and should all cause the
  #relevant "getMoves method to throw an exception.
  for piece in pieces
  
    board_to_construct = 
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "........"

    board_to_construct[0] = piece
    board = ChessDomain.constructBoard(board_to_construct)
    
    exception_thrown = false
    begin
      if(piece_to_check == "p" or piece_to_check == "P") then ChessDomain.getPawnMoves(board, Square.new(0,7)) end
      if(piece_to_check == "n" or piece_to_check == "N") then ChessDomain.getKnightMoves(board, Square.new(0,7)) end
      if(piece_to_check == "b" or piece_to_check == "B") then ChessDomain.getBishopMoves(board, Square.new(0,7)) end
      if(piece_to_check == "r" or piece_to_check == "R") then ChessDomain.getRookMoves(board, Square.new(0,7)) end
      if(piece_to_check == "q" or piece_to_check == "Q") then ChessDomain.getQueenMoves(board, Square.new(0,7)) end
      if(piece_to_check == "k" or piece_to_check == "K") then ChessDomain.getKingMoves(board, Square.new(0,7)) end
    rescue
      exception_thrown = true
    end
    
    assert_true(
      exception_thrown, 
      "occurred when checking if an exception is thrown when a square containing a '" +
      piece + "' is specified as input to the 'getMoves' function for a '" + piece_to_check +
      "' piece"
    )
  end
end
