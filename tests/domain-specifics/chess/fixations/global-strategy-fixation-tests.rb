unit_test "constructor" do
  
  # Need to be able to read the value of the private "_model" 
  # GlobalStrategyFixation instance variable to ensure the constructor operates 
  # correctly.
  GlobalStrategyFixation.class_eval{
    field_reader :_model
  }
  
  ########################################
  ##### CONSTRUCT MODEL AND FIXATION #####
  ########################################
  time = 0
  model = Chrest.new(time += 50, false)
  
  time + 50
  fixation = GlobalStrategyFixation.new(model, time)
  
  #################
  ##### TESTS #####
  #################
  assert_equal(
    time + 150,
    fixation.getTimeDecidedUpon(),
    "occurred when checking the time the fixation is decided upon"
  )
  
  assert_equal(
    model,
    fixation._model,
    "occurred when checking the model the fixation is associated with"
  )
end

################################################################################
# To test SalientManFixation.make() the following scenarios are repeated 3 times
# with the time the function is invoked differing:
# 
# Repeat 1: "make()" invoked before SalientManFixation performance time.
# Repeat 2: "make()" invoked at SalientManFixation performance time.
# Repeat 3: "make()" invoked after SalientManFixation performance time.
# 
# For repeat 1, all scenarios should return nil.  For repeats 2 and 3, the first 
# four scenarios should return nil whereas the rest should return a Square in a
# range.  The scenarios run are described below:
# 
# Scenario 1: Fail.  
#   - Function invoked before CHREST model created.  NOTE: for this scenario, the 
#     repeat conditions concerning invocation times of the function do not apply.
#    
# Scenario 2: Fail
#   - Function invoked after CHREST model created.
#   - No Fixation has been performed when GlobalStrategyFixation.make() is 
#     invoked.
#   
# Scenario 3:
#   - Function invoked after CHREST model created.
#   - Fixation has been performed when GlobalStrategyFixation.make() is invoked.
#   - Scene passed as input parameter is not a ChessBoard instance
#     
# Scenario 4:
#   - Function invoked after CHREST model created.
#   - Fixation has been performed when GlobalStrategyFixation.make() is invoked.
#   - Scene passed as input parameter is a ChessBoard instance
#   - Scene passed as input parameter is entirely blind
#
# In the remaining scenarios: 
#   - Function invoked after CHREST model created.
#   - Fixation has been performed when GlobalStrategyFixation.make() is invoked.
#   - Scene passed as input parameter is a ChessBoard instance
#   - Scene passed as input parameter is not entirely blind
#     
# The Fixations added will ensure that all possible combinations of board 
# quadrants are returned by GlobalStrategyFixation.make().  The table below 
# shows, for each scenario, what Fixations should be proposed (in context of 
# board quadrants) and what Fixations should have been made (again, in context 
# of board quadrants) prior to invoking GlobalStrategyFixation.make() to return 
# the intended Fixations.  Note that scenario 4 is listed again for completeness 
# but invoking GlobalStrategyFixation.make() will not actually return Fixations 
# for every board quadrant in this case since the function requires at least 1 
# previous Fixation to have been made in order to propose a Square to fixate on.
# 
#            |-------------------|
#            | Fixation Proposed |
# |----------|-------------------|---------------------|
# | Scenario | WK | WQ | BK | BQ | Prev. Fixation Req. |
# |----------|----|----|----|----|---------------------|
# | 4        | Y  | Y  | Y  | Y  | None                |
# | 5        | Y  | Y  | Y  | N  | BQ                  |
# | 6        | Y  | Y  | N  | Y  | BK                  |
# | 7        | Y  | Y  | N  | N  | BQ, BK              |
# | 8        | Y  | N  | Y  | Y  | WQ                  |
# | 9        | Y  | N  | Y  | N  | WQ, BQ              |
# | 10       | Y  | N  | N  | Y  | WQ, BK              |
# | 11       | Y  | N  | N  | N  | WQ, BK, BQ          |
# | 12       | N  | Y  | Y  | Y  | WK                  |
# | 13       | N  | Y  | Y  | N  | WK, BQ              |
# | 14       | N  | Y  | N  | Y  | WK, BK              |
# | 15       | N  | Y  | N  | N  | WK, BK, BQ          |
# | 16       | N  | N  | Y  | Y  | WK, WQ              |
# | 17       | N  | N  | Y  | N  | WK, WQ, BQ          |
# | 18       | N  | N  | N  | Y  | WK, WQ, BK          |
# | 19       | N  | N  | N  | N  | WK, WQ, BK, BQ      |
# |----------|----|----|----|----|---------------------|
# 
unit_test "make" do
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  for performance_time in 1..3
    for scenario in 1..19
      
      time = 0
      
      ###########################
      ##### CONSTRUCT MODEL #####
      ###########################
      
      # Construct model
      model_creation_time = (time += 50)
      model = Chrest.new(model_creation_time, false)
        
      ###############################################
      ##### CONSTRUCT SCENE TO MAKE FIXATION ON #####
      ###############################################
      
      scene = nil
      
      # If this is scenario 3, the Scene to make the GlobalStrategyFixation on
      # should not be a ChessBoard instance.
      if scenario == 3
        scene = Scene.new("", 8, 8, 1, 1, nil)
      else
        scene = 
          "rnbqkbnr/" +
          "pppppppp/" +
          "......../" +
          "......../" +
          "......../" +
          "......../" +
          "PPPPPPPP/" +
          "RNBQKBNR"
      end
      
      if scene.is_a? String then scene = ChessDomain.constructBoard(scene) end
      
      # If scenario is 4, the ChessBoard instance should be entirely blind
      if scenario == 4
        for col in 0...scene.getWidth()
          for row in 0...scene.getHeight()
            scene._scene.get(col).set(row, SceneObject.new(Scene.getBlindSquareToken()))
          end
        end
      end
      
      ########################################
      ##### CONSTRUCT QUADRANT FIXATIONS #####
      ########################################
      
      fixations_to_add = []
      white_queen_quadrant = []
      white_king_quadrant = []
      black_queen_quadrant = []
      black_king_quadrant = []
      if scenario >= 5
        
        # Need to be able to set private Fixation instance variables manually
        Fixation.class_eval{
          field_accessor :_performed, :_scene, :_colFixatedOn, :_rowFixatedOn
        }
      
        # Set-up quadrant fixations
        for col in 0..3
          for row in 0..3
            white_queen_quadrant.push([col, row])
          end
        end

        for col in 4..7
          for row in 0..3
            white_king_quadrant.push([col, row])
          end
        end

        
        for col in 0..3
          for row in 4..7
            black_queen_quadrant.push([col, row])
          end
        end

        for col in 4..7
          for row in 4..7
            black_king_quadrant.push([col, row])
          end
        end

        # Specify squares to fixate on
        squares_to_fixate_on = []
        if scenario.between?(12, 19)
          squares_to_fixate_on.push(white_king_quadrant.sample)
        end

        if scenario.between?(8,11) || scenario.between?(16,19)
          squares_to_fixate_on.push(white_queen_quadrant.sample)
        end

        if [6,7,10,11,14,15,18,19].include?(scenario)
          squares_to_fixate_on.push(black_king_quadrant.sample)
        end

        if scenario > 4 && scenario % 2 != 0
          squares_to_fixate_on.push(black_queen_quadrant.sample)
        end
        
        #Should always be able to fixate on the center
        squares_to_fixate_on.push([3,3])
        squares_to_fixate_on.push([3,4])
        squares_to_fixate_on.push([4,3])
        squares_to_fixate_on.push([4,4])

        #Add fixations to add to relevant array
        for square_to_fixate_on in squares_to_fixate_on
          fixation = CentralFixation.new(time += 10)
          fixation.setPerformanceTime(fixation.getTimeDecidedUpon() + 10)
          time = fixation.getPerformanceTime

          fixation._performed = true
          fixation._scene = scene
          fixation._colFixatedOn = square_to_fixate_on[0]
          fixation._rowFixatedOn = square_to_fixate_on[1]
          fixations_to_add.push(fixation)
        end
      end
      
      ############################################
      ##### CONSTRUCT GlobalStrategyFixation #####
      ############################################
      global_strategy_fixation = GlobalStrategyFixation.new(model, time)
      global_strategy_fixation.setPerformanceTime(global_strategy_fixation.getTimeDecidedUpon() + 50)
      time = global_strategy_fixation.getPerformanceTime()
      
      ########################################
      ##### SET "make()" INVOCATION TIME #####
      ########################################
      
      make_invocation_time =
        (scenario == 1 ?
          model_creation_time - 1:
          (performance_time == 1 ? 
            time - 1 : 
            (performance_time == 2 ?
              time :
              time + 1  
            )
          ) 
        )
      
      ###############################################
      ##### ADD PREVIOUS FIXATIONS TO PERCEIVER #####
      ###############################################
      
      #Only do this if this isn't scenario 2
      if scenario != 2
        perceiver = model.getPerceiver()
        for fixation_to_add in fixations_to_add
          perceiver.addFixation(fixation_to_add)
        end
      end
      
      ################
      ##### TEST #####
      ################
      
      # Set what Squares are expected 
      expected = []
      if scenario <= 4 || performance_time == 1 
        expected.push(nil) 
        
      else
      
        if scenario.between?(5,11) 
          for coordinate_pair in white_king_quadrant
            expected.push(Square.new(coordinate_pair[0], coordinate_pair[1]).toString())
          end
        end

        if scenario.between?(5,7) or scenario.between?(12, 15)
          for coordinate_pair in white_queen_quadrant
            expected.push(Square.new(coordinate_pair[0], coordinate_pair[1]).toString())
          end
        end

        if [5,8,9,12,13,16,17].include?(scenario)
          for coordinate_pair in black_king_quadrant
            expected.push(Square.new(coordinate_pair[0], coordinate_pair[1]).toString())
          end
        end

        if scenario > 4 and scenario % 2 == 0
          for coordinate_pair in black_queen_quadrant
            expected.push(Square.new(coordinate_pair[0], coordinate_pair[1]).toString())
          end
        end
        
        expected.push(Square.new(3, 3).toString())
        expected.push(Square.new(3, 4).toString())
        expected.push(Square.new(4, 3).toString())
        expected.push(Square.new(4, 4).toString())
      end

      # Invoke "make" 500 times so that it can be reasonably assurred that all
      # possible Squares are returned for a scenario.
      500.times do
        result = global_strategy_fixation.make(scene, make_invocation_time)
        if result != nil then result = result.toString() end
        
          assert_true(
            expected.include?(result),
            "occurred in scenario " + scenario.to_s + " when invoking 'make' " +
            (performance_time == 1 ? "before" : performance_time == 2 ? "at" : 
            "after") + " the Fixation's performance time; '" + (result == nil ?
            "null" : result.to_s) + "' is not expected"
          )
      end
    end
  end
end