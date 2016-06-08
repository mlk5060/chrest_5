################################################################################
# The constructor will calculate the time the AttackDefenseFixtion is decided 
# upon by determining the type of ChessObject seen in the most recent Fixation 
# performed by the Perceiver associated with the model constructing the 
# AttackDefenseFixtion and calculating what moves can be made in context of the
# Scene provided to the constructor.
# 
# The time taken to decide upon a Fixation of this type differs depending upon
# the piece being fixated on and the chess board the fixation is being generated 
# in context of.
# 
# Scenario 1 
#   - Scene to make Fixation in context of is entirely blind.
#   
# Scenario 2
#   - Scene to make Fixation in context of is not entirely blind.
#   - AttackDefenseFixation.make() invoked at time before CHREST model 
#     associated with the AttackDefenseFixation has been created.
#     
# Scenario 3
#   - Scene to make Fixation in context of is not entirely blind.
#   - AttackDefenseFixation.make() invoked at time when CHREST model 
#     associated with the AttackDefenseFixation has been created.    
#   - No Fixation made when AttackDefenseFixation.make() is invoked.
#   
# Scenario 4
#   - Scene to make Fixation in context of is not entirely blind.
#   - Fixation made before AttackDefenseFixation.make() is invoked.
#   - AttackDefenseFixation.make() invoked at time when CHREST model 
#     associated with the AttackDefenseFixation has been created.  
#   - Previous fixation's column coordinate = null.
#   
# Scenario 5
#   - Scene to make Fixation in context of is not entirely blind.
#   - AttackDefenseFixation.make() invoked at time when CHREST model 
#     associated with the AttackDefenseFixation has been created.  
#   - Fixation made before AttackDefenseFixation.make() is invoked.
#   - Previous fixation's column coordinate != null.
#   - Previous fixation's row coordinate = null.
#   
# Scenario 6
#   - Scene to make Fixation in context of is not entirely blind.
#   - AttackDefenseFixation.make() invoked at time when CHREST model 
#     associated with the AttackDefenseFixation has been created.  
#   - Fixation made before AttackDefenseFixation.make() is invoked.
#   - Previous fixation's column coordinate != null.
#   - Previous fixation's row coordinate != null.
#   - SceneObject fixated on in previous Fixation = null.
#   
# Scenario 7
#   - Scene to make Fixation in context of is not entirely blind.
#   - AttackDefenseFixation.make() invoked at time when CHREST model 
#     associated with the AttackDefenseFixation has been created.  
#   - Fixation made before AttackDefenseFixation.make() is invoked.
#   - Previous fixation's column coordinate != null.
#   - Previous fixation's row coordinate != null.
#   - SceneObject seen in previous Fixation != null.
#   - SceneObject fixated on in previous Fixation is not a ChessObject.
# 
# Scenario 8
#   - Scene to make Fixation in context of is not entirely blind.
#   - AttackDefenseFixation.make() invoked at time when CHREST model 
#     associated with the AttackDefenseFixation has been created.  
#   - Fixation made before AttackDefenseFixation.make() is invoked.
#   - Previous fixation's column coordinate != null.
#   - Previous fixation's row coordinate != null.
#   - SceneObject seen in previous Fixation != null.
#   - SceneObject fixated on in previous Fixation is a ChessObject.
#   - ChessObject fixated on in previous Fixation represents a blind Square.
#   
# Scenario 9
#   - Scene to make Fixation in context of is not entirely blind.
#   - AttackDefenseFixation.make() invoked at time when CHREST model 
#     associated with the AttackDefenseFixation has been created.  
#   - Fixation made before AttackDefenseFixation.make() is invoked.
#   - Previous fixation's column coordinate != null.
#   - Previous fixation's row coordinate != null.
#   - SceneObject seen in previous Fixation != null.
#   - SceneObject fixated on in previous Fixation is a ChessObject.
#   - ChessObject fixated on in previous Fixation does not represent a blind 
#     Square.
#   - ChessObject fixated on in previous Fixation represents an empty Square.
#   
# For the remaining scenarios, the following conditions are true:
#   - Scene to make Fixation in context of is not entirely blind.
#   - AttackDefenseFixation.make() invoked at time when CHREST model 
#     associated with the AttackDefenseFixation has been created.  
#   - Fixation made before AttackDefenseFixation.make() is invoked.
#   - Previous fixation's column coordinate != null.
#   - Previous fixation's row coordinate != null.
#   - SceneObject seen in previous Fixation != null.
#   - SceneObject fixated on in previous Fixation is a ChessObject.
#   - Scene to perform AttackDefenseFixation on is a ChessBoard.
#   - ChessObject fixated on in previous Fixation does not represent a blind 
#     Square.
#   - ChessObject fixated on in previous Fixation does not represent an empty 
#     Square.
# 
# With regard to the final condition, scenarios 11-34 represent different 
# ChessObjects that were fixated on in the previous Fixation made and their
# ability to move.
# 
# Scenario 10/11, 22/23: Pawn (white/black)
# Scenario 12/13, 24/25: Knight (white/black)
# Scenario 14/15, 26/27: Bishop (white/black)
# Scenario 16/17, 28/39: Rook (white/black)
# Scenario 18/19, 30/31: Queen (white/black)
# Scenario 20/21, 32/33: King (white/black)
# 
# In scenarios 10-21, the ChessBoard to make the AttackDefenseFixation on is 
# set-up such that the ChessObject whose moves are to be calculated can not move
# whereas, in scenarios 22-33, the ChessObject has unrestricted movement. 
#
# The test should check the values of the following AttackDefenseFixation 
# instance variables after the AttackDefenseFixation has been constructed:
# 
# - _squareToFixateOn: this should be a range of values so an 
#                      AttackDefenseFixation is repeatedly constructed to allow
#                      for as many possible values in this range to be set.
# - _timeDecidedUpon
# 
# The exact moves capable of being made by each ChessObject are tested in other 
# test files and so are not handled here.  
# 
unit_test "constructor" do
  
  Chrest.class_eval{
    field_accessor :_timeToAccessVisualSpatialField, :_timeToMoveVisualSpatialFieldObject
  }
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  # Need access to all private Fixation instance variables except 
  # "_timeDecidedUpon" so make them all writable from Ruby.
  Fixation.class_eval{ 
    field_accessor :_performanceTime, :_performed, :_scene, :_colFixatedOn, :_rowFixatedOn, :_objectSeen
  }
  
  # Need access to the private AttackDefenseFixation instance variable that 
  # stores the Square to fixate on
  AttackDefenseFixation.class_eval{
    field_reader :_squareToFixateOn, :_board
  }
  
  for scenario in 1..33
    time = 5

    ###############################
    ##### CHREST MODEL SET-UP #####
    ###############################

    model_creation_time = time
    model = Chrest.new(model_creation_time, false)

    ##################################
    ##### CONSTRUCT SCENE TO USE #####
    ##################################

    # Depending upon the scenario, a different piece will be fixated on in the
    # chess board.  Note that only scenarios 10-33 have non-empty pieces 
    # specified for the piece to fixate on since scenarios < 10 should not 
    # reach the piece determination code block in the AttackDefenseFixation 
    # constructor so setting up non-empty pieces is unnecessary for these 
    # scenarios.
    piece_to_fixate_on = "."
    case scenario
    when 10,22
      piece_to_fixate_on = "P"
    when 11,23
      piece_to_fixate_on = "p"
    when 12,24
      piece_to_fixate_on = "N"
    when 13,25
      piece_to_fixate_on = "n"
    when 14,26
      piece_to_fixate_on = "B"
    when 15,27
      piece_to_fixate_on = "b"
    when 16,28
      piece_to_fixate_on = "R"
    when 17,29
      piece_to_fixate_on = "r"
    when 18,30
      piece_to_fixate_on = "Q"
    when 19,31
      piece_to_fixate_on = "q"
    when 20,32
      piece_to_fixate_on = "K"
    when 21,33
      piece_to_fixate_on = "k"
    end

    # Construct the Scene to pass to the AttackDefenseFixation constructor. 
    # This should be a ChessBoard with the piece to fixate on located on 
    # specific coordinates so that the squares expected to be moved to and 
    # time taken to decide upon the AttackDefenseFixation constructed can be 
    # calculated exactly.
    board_to_construct = 
      "......../" +
      "......../" +
      "......../" +
      "..." + piece_to_fixate_on + "..../" +
      "......../" +
      "......../" +
      "......../" +
      "........"

    scene_to_make_fixation_in_context_of = ChessDomain.constructBoard(board_to_construct)
    piece_to_fixate_on_location = Square.new(3,4)

    #If this is scenario 1, the board should be entirely blind.
    if scenario == 1
      for col in 0...scene_to_make_fixation_in_context_of.getWidth()
        for row in 0...scene_to_make_fixation_in_context_of.getHeight()
          piece = ChessObject.new(Scene.getBlindSquareToken())
          scene_to_make_fixation_in_context_of._scene.get(col).set(row, piece)
        end
      end 
    end

    # If scenario is between 10-21 then the piece fixated on should not be 
    # able to move so should be surrounded by pieces of the same colour 
    if scenario.between?(10, 21)
      piece_to_fixate_on_colour = scene_to_make_fixation_in_context_of.getSquareContents(piece_to_fixate_on_location.getColumn(), piece_to_fixate_on_location.getRow()).getColour()
      for col in 0...scene_to_make_fixation_in_context_of.getWidth()
        for row in 0...scene_to_make_fixation_in_context_of.getHeight()

          # Put a pawn (could be any piece though) of the same colour on all 
          # Squares except the Square containing the piece to fixate on in context
          # of the chess board.
          if(!piece_to_fixate_on_location.equals(Square.new(col, row)))
            piece = ChessObject.new( 
              piece_to_fixate_on_colour == Color::WHITE ?
                "P" : 
                "p"
            )
            scene_to_make_fixation_in_context_of._scene.get(col).set(row, piece)
          end
        end
      end 
    end

    #########################################
    ##### SET-UP AND ADD FIRST FIXATION #####
    #########################################

    # The first Fixation should be on the piece to fixate on so that the 
    # subsequent AttackDefenseFixation constructed will select this piece as
    # the piece to find object moves from.
    time += 100
    if scenario != 3
      first_fixation = CentralFixation.new(time, 0)
      first_fixation._performanceTime = (time += 200)
      first_fixation._performed = true
      first_fixation._scene = scene_to_make_fixation_in_context_of
      first_fixation._colFixatedOn = piece_to_fixate_on_location.getColumn()
      first_fixation._rowFixatedOn = piece_to_fixate_on_location.getRow()
      first_fixation._objectSeen = scene_to_make_fixation_in_context_of.getSquareContents(first_fixation._colFixatedOn, first_fixation._rowFixatedOn)
      model.getPerceiver.addFixation(first_fixation)

      # Now that the first Fixation has been added, change its variables 
      # according to the scenario (checks on these variables would prevent the
      # Fixation from being added in most cases so this needs to be done after 
      # the addition).
      if scenario == 4 then first_fixation._colFixatedOn = nil end
      if scenario == 5 then first_fixation._rowFixatedOn = nil end
      if scenario == 6 then first_fixation._objectSeen = nil end
      if scenario == 7 then first_fixation._objectSeen = SceneObject.new("0", "K") end
      if scenario == 8 then first_fixation._objectSeen = ChessObject.new(Scene.getBlindSquareToken()) end
      if scenario == 9 then first_fixation._objectSeen = ChessObject.new(Scene.getEmptySquareToken()) end
    end

    time += 100

    ##################################
    ##### SET EXPECTED VARIABLES #####
    ##################################

    ##### EXPECTED TIME DECIDED UPON #####

    # The expected time for the AttackDefenseFixation to be decided upon 
    # will always be the time the constructor was invoked plus the time taken
    # for the CHREST model to access its visual-spatial field.
    expected_time_decided_upon = time + model._timeToAccessVisualSpatialField

    # If the current scenario is >= 10 then piece moves will be attempted. To 
    # keep code DRY, set the time taken to move a piece across a Square on the 
    # visual-spatial field representation of the chess board, even if
    # it won't be used.
    time_to_traverse_a_square = model._timeToMoveVisualSpatialFieldObject
    squares_traversed = 0

    ##### EXPECTED POTENTIAL FIXATIONS #####

    # No Fixations are expected if performance_time = 1 or scenario is = 
    # 1-21 (in case of the latter, no pieces can be moved).
    expected_potential_fixations = []

    #Pawn move
    if [10,11,22,23].include?(scenario)

      # In scenarios 10 and 11, the pawn will not consider an "initial" move
      # because its standard move is blocked so only 3 squares are considered.
      # Otherwise, 4 squares are considered
      if [10,11].include?(scenario)
        squares_traversed = 3
      else 
        squares_traversed = 4 

        #The square traversed to will differ depending upon the colour of the
        #pawn and pawn's can only move vertically so set the row appropriately
        expected_potential_fixations.push([3, scenario == 22 ? 5 : 3])
      end
    end

    #Knight: should always be 8 moves considered (3 squares per move)
    if [12,13,24,25].include?(scenario) 
      squares_traversed = (8 * 3)

      if[24,25].include?(scenario)
        expected_potential_fixations.push([1, 5])
        expected_potential_fixations.push([2, 6])
        expected_potential_fixations.push([4, 6])
        expected_potential_fixations.push([5, 5])
        expected_potential_fixations.push([5, 3])
        expected_potential_fixations.push([4, 2])
        expected_potential_fixations.push([2, 2])
        expected_potential_fixations.push([1, 3])
      end
    end

    #Bishop
    if [14,15,26,27].include?(scenario) 

      # In scenarios 14 and 15, the bishop will not consider squares after the
      # first square moved in any possible direction since it is blocked so 
      # only 4 squares are considered.  Otherwise, 13 squares are considered
      # since the bishop is free to move along its movement lines until it 
      # reaches the edge of the board.
      if[14,15].include?(scenario)
        squares_traversed = 4
      else
        squares_traversed = 13
        expected_potential_fixations.push([2, 5])
        expected_potential_fixations.push([1, 6])
        expected_potential_fixations.push([0, 7])
        expected_potential_fixations.push([4, 5])
        expected_potential_fixations.push([5, 6])
        expected_potential_fixations.push([6, 7])
        expected_potential_fixations.push([4, 3])
        expected_potential_fixations.push([5, 2])
        expected_potential_fixations.push([6, 1])
        expected_potential_fixations.push([7, 0])
        expected_potential_fixations.push([2, 3])
        expected_potential_fixations.push([1, 2])
        expected_potential_fixations.push([0, 1])
      end
    end

    #Rook: should always be 14 squares traversed
    if [16,17,28,29].include?(scenario)

      # In scenarios 16 and 17, the rook will not consider squares after the
      # first square moved in any possible direction since it is blocked so 
      # only 4 squares are considered.  Otherwise, 14 squares are considered
      # since the rook is free to move along its movement lines until it 
      # reaches the edge of the board.
      if[16,17].include?(scenario)
        squares_traversed = 4
      else
        squares_traversed = 14
        expected_potential_fixations.push([2, 4])
        expected_potential_fixations.push([1, 4])
        expected_potential_fixations.push([0, 4])
        expected_potential_fixations.push([3, 5])
        expected_potential_fixations.push([3, 6])
        expected_potential_fixations.push([3, 7])
        expected_potential_fixations.push([4, 4])
        expected_potential_fixations.push([5, 4])
        expected_potential_fixations.push([6, 4])
        expected_potential_fixations.push([7, 4])
        expected_potential_fixations.push([3, 3])
        expected_potential_fixations.push([3, 2])
        expected_potential_fixations.push([3, 1])
        expected_potential_fixations.push([3, 0])
      end
    end

    #Queen
    if [18,19,30,31].include?(scenario) 

      # In scenarios 18 and 19, the queen will not consider squares after the
      # first square moved in any possible direction since it is blocked so 
      # only 8 squares are considered.  Otherwise, 27 squares are considered
      # since the queen is free to move along its movement lines until it 
      # reaches the edge of the board.
      if[18,19].include?(scenario)
        squares_traversed = 8
      else
        squares_traversed = 27 
        expected_potential_fixations.push([2, 5])
        expected_potential_fixations.push([1, 6])
        expected_potential_fixations.push([0, 7])
        expected_potential_fixations.push([4, 5])
        expected_potential_fixations.push([5, 6])
        expected_potential_fixations.push([6, 7])
        expected_potential_fixations.push([4, 3])
        expected_potential_fixations.push([5, 2])
        expected_potential_fixations.push([6, 1])
        expected_potential_fixations.push([7, 0])
        expected_potential_fixations.push([2, 3])
        expected_potential_fixations.push([1, 2])
        expected_potential_fixations.push([0, 1])
        expected_potential_fixations.push([2, 4])
        expected_potential_fixations.push([1, 4])
        expected_potential_fixations.push([0, 4])
        expected_potential_fixations.push([3, 5])
        expected_potential_fixations.push([3, 6])
        expected_potential_fixations.push([3, 7])
        expected_potential_fixations.push([4, 4])
        expected_potential_fixations.push([5, 4])
        expected_potential_fixations.push([6, 4])
        expected_potential_fixations.push([7, 4])
        expected_potential_fixations.push([3, 3])
        expected_potential_fixations.push([3, 2])
        expected_potential_fixations.push([3, 1])
        expected_potential_fixations.push([3, 0])
      end
    end

    #King: should always be 8 squares traversed
    if [20,21,32,33].include?(scenario) 
      squares_traversed = 8
      if[32,33].include?(scenario)
        expected_potential_fixations.push([2, 4])
        expected_potential_fixations.push([2, 5])
        expected_potential_fixations.push([3, 5])
        expected_potential_fixations.push([4, 5])
        expected_potential_fixations.push([4, 4])
        expected_potential_fixations.push([4, 3])
        expected_potential_fixations.push([3, 3])
        expected_potential_fixations.push([2, 3])
      end
    end

    expected_potential_fixations.map!{|x| Square.new(x[0], x[1]).toString() }
    expected_time_decided_upon += squares_traversed * time_to_traverse_a_square

    #########################################################
    ##### CONSTRUCT AttackDefenseFixation AND RUN TESTS #####
    #########################################################

    invocation_time = (scenario == 1 ? model_creation_time - 1 : time)
    error_msg_append = " in scenario " + scenario.to_s + " when CHREST model " + 
      (model.isExperienced(invocation_time) ? "is" : "is not") + " experienced"

    # Repeat test 300 times since it needs to be ensured that:
    # - If the piece fixated on can not be moved, the output of the 
    #   constructor never changes
    # - If the piece fixated on can be moved, all Fixations made are included
    #   in those expected.
    300.times do

      # Construct AttackDefenseFixation
      exception_thrown = false
      begin
        attack_defense_fixation = AttackDefenseFixation.new(
          model, 
          scene_to_make_fixation_in_context_of, 
          invocation_time
        )
      rescue
        exception_thrown = true
      end

      assert_equal(
        (scenario == 1 ? true : false),
        exception_thrown,
        "occurred when checking if an IllegalArgumentException is thrown" +
        error_msg_append
      )

      # If scenario doesn't equal 1, the AttackDefenseFixation will be 
      # constructed, i.e. not nil, so its properties can be examined. 
      if scenario != 1
        assert_equal(
          expected_time_decided_upon,
          attack_defense_fixation.getTimeDecidedUpon(),
          "occurred when checking the time the fixation is decided upon" + error_msg_append
        )

        assert_true(
          (scenario.between?(23, 34) ? 
            expected_potential_fixations.include?(attack_defense_fixation._squareToFixateOn.toString()) :
            attack_defense_fixation._colFixatedOn == nil && attack_defense_fixation._rowFixatedOn == nil
          ),
          "occurred when checking the square fixated upon" + error_msg_append
        )

        assert_equal(
          scene_to_make_fixation_in_context_of,
          attack_defense_fixation._board,
          "occurred when checking the board the fixation is made in context of"
        )
      end

    end
  end
end

################################################################################
# Runs 8 scenarios, the first 7 of which translate to a test on a specific 
# clause of the conditional that controls what value is returned by "make" when 
# invoked.  The final scenario should always return a Square whereas the first
# seven should return null.
#
# Scenario 1: Fail
#   - Invoke "make" before performance time of AttackDefenseFixation
# 
# Scenario 2: Fail
#   - Invoke "make" on the performance time of AttackDefenseFixation
#   - Have the scene input to "make" be entirely blind
# 
# Scenario 3: Fail
#   - Invoke "make" on the performance time of AttackDefenseFixation
#   - Have the scene input to "make" not be entirely blind
#   - Have the square to fixate on equal to null
#   
# Scenario 4: Fail
#   - Invoke "make" on the performance time of AttackDefenseFixation
#   - Have the scene input to "make" not be entirely blind
#   - Have the square to fixate on not equal to null
#   - Have the scene input to "make" be different to the one that the Square to 
#     fixate on by the AttackDefenseFixation was made in context of.
#     
# Scenario 5: Fail
#   - Invoke "make" on the performance time of AttackDefenseFixation
#   - Have the scene input to "make" not be entirely blind
#   - Have the square to fixate on not equal to null
#   - Have the scene input to "make" be the same as the one that the Square to 
#     fixate on by the AttackDefenseFixation was made in context of.
#   - Have the Square to fixate on be a blind square.
#   
# Scenario 6: Pass
#   - Invoke "make" on the performance time of AttackDefenseFixation
#   - Have the scene input to "make" not be entirely blind
#   - Have the square to fixate on not equal to null
#   - Have the scene input to "make" be the same as the one that the Square to 
#     fixate on by the AttackDefenseFixation was made in context of.
#   - Have the Square to fixate on not be a blind square.
unit_test "make" do
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  # Need access to the private AttackDefenseFixation instance variable that 
  # stores the Square to fixate on
  AttackDefenseFixation.class_eval{
    field_accessor :_squareToFixateOn
  }
  
  for scenario in 1..6
    square_to_fixate_on = Square.new(0, 6)
    
    #################################
    ##### CONSTRUCT CHESS BOARD #####
    #################################
    
    chess_board_to_decide_upon_fixation_in_context_of = 
      "rnbqkbnr/" +
      "pppppppp/" +
      "......../" +
      "......../" +
      "......../" +
      "......../" +
      "PPPPPPPP/" +
      "RNBQKBNR"
    
    chess_board_that_fixation_was_decided_in_context_of = ChessDomain.constructBoard(chess_board_to_decide_upon_fixation_in_context_of)
    chess_board_that_fixation_is_to_be_made_in_context_of = ChessDomain.constructBoard(chess_board_to_decide_upon_fixation_in_context_of)
    
    if scenario == 2
      for col in 0...chess_board_that_fixation_is_to_be_made_in_context_of.getWidth()
        for row in 0...chess_board_that_fixation_is_to_be_made_in_context_of.getHeight()
          chess_board_that_fixation_is_to_be_made_in_context_of._scene.get(col).set(row, SceneObject.new(Scene.getBlindSquareToken()))
        end
      end
    end
    
    if scenario.between?(4,5) 
      object = nil
      
      if scenario == 4 
        object = ChessObject.new(Scene.getEmptySquareToken())
      else
        # Need to ensure that the "equal" condition passes in the "make" 
        # function to ensure that the blind square conditional is working 
        # properly. So, the blind square object needs to have the same ID as the 
        # pawn on [0, 6] in the Scene (SceneObjects representing blind Squares 
        # have their "identifier" instance variable set to "null" no matter what 
        # parameter is supplied to the SceneObject constructor).  
        # 
        # Setting "Fixation._identifier" can't be done using any jRuby
        # constructs like "field_accessor" since the "Fixation._identifier" 
        # instance variable is not just private, its final.  So, to set it, 
        # Java-esque reflection is required:
        #
        # 1) Construct the blind square object
        # 2) Get the Java class of the blind square object (ChessObject, other
        #    methods expect ChessBoards to contain ChessObjects so maintain this
        #    requirement to avoid any unintended consequences).
        # 3) Get the superclass of the ChessObject (SceneObject) and get the
        #    "_identifier" as a JavaField object.
        # 4) Set the "accessible" property of the "_identifier" JavaField object
        #    to true (removes final and private modification constraints) and
        #    set the value of this variable in context of the blind square 
        #    object to the value of the "_identifier" for the pawn ChessObject
        #    on Square [0, 6] in the Scene used in the constructor to the
        #    AttackDefenseFixation constructor.
        object = ChessObject.new(Scene.getBlindSquareToken())
        id_field = object.java_class().superclass().declared_field("_identifier")
        id_field.accessible = true
        id_field.set_value(object, chess_board_that_fixation_was_decided_in_context_of.getSquareContents(
          square_to_fixate_on.getColumn(), square_to_fixate_on.getRow()
        ).getIdentifier())
      end
      
      chess_board_that_fixation_is_to_be_made_in_context_of._scene.get(square_to_fixate_on.getColumn()).set(
        square_to_fixate_on.getRow(),
        object
      )
    end
    
    #####################################################
    ##### CONSTRUCT MODEL AND AttackDefenseFixation #####
    #####################################################
    
    time = 0
    model = Chrest.new(time, false)

    # Normally, a previous Fixation would be required for an AttackDefenseFixation
    # to be constructed properly but the relevant instance variables of the 
    # AttackDefenseFixation constructed here can be set in this test since the 
    # only purpose of this test is to test the output of the "make()" function, 
    # not the AttackDefenseFixation constructor.  Therefore, call the constructor
    # without previously adding a Fixation to the Perceiver associated with the
    # CHREST model making the AttackDefenseFixation.
    fixation = AttackDefenseFixation.new(model, chess_board_that_fixation_was_decided_in_context_of, time += 50)
    fixation_performance_time = (time += 100)
    fixation.setPerformanceTime(fixation_performance_time)
    fixation._squareToFixateOn = (scenario == 3 ? nil : Square.new(0,6))
  
    ################
    ##### TEST #####
    ################
    expected_result = (scenario == 6 ? square_to_fixate_on : nil)
    
    # Make the Fixation 300 times to ensure output is consistent.
    300.times do
      result = fixation.make(
        chess_board_that_fixation_is_to_be_made_in_context_of, 
        (scenario == 1 ? fixation_performance_time - 10 : fixation_performance_time)
      )
      
      assert_equal(
        expected_result,
        result,
        "occurred in scenario " + scenario.to_s
      )
    end
  end
end
