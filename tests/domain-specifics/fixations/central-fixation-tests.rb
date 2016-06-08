unit_test "constructor" do
  time = 80
  fixation = CentralFixation.new(time, 0)
  assert_equal(
    time,
    fixation.getTimeDecidedUpon(),
    "occurred when checking the time the fixation is decided upon"
  )
end

unit_test "make" do
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  time = 0
  fixation = CentralFixation.new(time, 0)
  fixation.setPerformanceTime(fixation.getTimeDecidedUpon() + 100)
  
  ################################
  ##### SCENE OBJECT TESTING #####
  ################################
  
  # Try to make fixation on an entirely blind Scene at valid times (exactly when
  # fixation is scheduled to be performed and after the fixation is scheduled to
  # be performed).
  for i in 1..2
    time = (i == 1 ? fixation.getPerformanceTime() : fixation.getPerformanceTime() + 10)
    assert_equal(
      nil, 
      fixation.make(Scene.new("", 7, 8, 1, 1, nil), time),
      "occurred when fixation is made on a blind scene at a time " + (i == 1 ? 
      "equal to the performance time" : "later than the performance time") + 
      "that the fixation is scheduled for." 
    )
  end
  
  # Test the function thoroughly using all possible combinations of 5 * 5
  # Scene (non sub-classed) objects with all possible combinations of potential
  # fixations on empty/blind squares.
  for scenario in 1..130
    
    # Get scenario details
    scene_and_expected_fixations = get_scenario_details(scenario)
    scene = scene_and_expected_fixations[0]
    expected_fixations = scene_and_expected_fixations[1]
    
    #Try to make fixation before performance time
    fixation_made = fixation.make(scene, fixation.getPerformanceTime() - 10)
    assert_equal(
      nil,
      fixation_made,
      "occurred when checking if a fixation is returned when an attempt is " +
      "made to make the fixation before its performance time in scenario " + 
      scenario.to_s
    )
    
    #Try to make fixation at the performance time and after the performance time
    for i in 1..2
      time = (i == 1 ? fixation.getPerformanceTime() : fixation.getPerformanceTime() + 10)
      fixations_made = []
      
      # Make the fixation 300 times, this should verify that only the fixations
      # expected are ever made.
      300.times do
        fixation_made = fixation.make(scene, time)
        if fixation_made != nil 
          fixation_made = fixation_made.toString()
          if !fixations_made.include?(fixation_made)
            fixations_made.push(fixation_made)
          end
        end
      end

      assert_equal(
        expected_fixations.length,
        fixations_made.length,
        "occurred when checking the number of fixations made against the number " +
        "expected for scenario " + scenario.to_s 
      )

      for expected_fixation in expected_fixations
        assert_true(
          fixations_made.include?(expected_fixation),
          "occurred when checking if " + expected_fixation.to_s + " is included " +
          "in the fixations made"
        )
      end
    end
  end
  
  ###########################################
  ##### SUBCLASSED SCENE OBJECT TESTING #####
  ###########################################

  # This portion of the test will use a ChessBoard object.
  blind_scene = ChessBoard.new("")
  non_blind_standard_chess_board = ChessDomain.construct_board(
    "rbnqknbr/" + 
    "pppppppp/" +
    "......../" +
    "......../" +
    "......../" +
    "......../" +
    "PPPPPPPP/" +
    "RBNQKNBR"
  )
    
  # Should only ever fixate on the coordinates:
  north_west_of_absolute_centre_fixated_on = false #[3, 4]
  north_east_of_absolute_centre_fixated_on = false #[4, 4]
  south_west_of_absolute_centre_fixated_on = false #[3, 3]
  south_east_of_absolute_centre_fixated_on = false #[4, 3]

  # Used to keep testing after all expected coordinates have been fixated on 
  # to ensure that no fixations are made on unexpected coordinates.
  fixations_made = 0 

  while 
    !north_west_of_absolute_centre_fixated_on ||
    !north_east_of_absolute_centre_fixated_on ||
    !south_west_of_absolute_centre_fixated_on ||
    !south_east_of_absolute_centre_fixated_on ||
    fixations_made < 200

    square_to_fixate_on = fixation.make(non_blind_standard_chess_board, fixation.getPerformanceTime())
    fixation_col = square_to_fixate_on.getColumn()
    fixation_row = square_to_fixate_on.getRow()

    if(fixation_col == 3 && fixation_row == 4) then north_west_of_absolute_centre_fixated_on = true end
    if(fixation_col == 4 && fixation_row == 4) then north_east_of_absolute_centre_fixated_on = true end
    if(fixation_col == 3 && fixation_row == 3) then south_west_of_absolute_centre_fixated_on = true end
    if(fixation_col == 4 && fixation_row == 3) then south_east_of_absolute_centre_fixated_on = true end

    assert_true(
      (fixation_col == 3 && fixation_row == 3) ||
      (fixation_col == 3 && fixation_row == 4) ||
      (fixation_col == 4 && fixation_row == 3) ||
      (fixation_col == 4 && fixation_row == 4),
      "occurred when making fixation on a non-blind chess board.  Fixation " +
      "proposed: " + square_to_fixate_on.toString() + " is not an expected " +
      "fixation"
    )

    fixations_made += 1
  end
end

################################################################################
# Constructs 1 of 130 Scene objects (not sub-classed) and returns the Scene 
# object constructed along with the Fixations expected to be made if
# CentralFixation.make() is invoked in context of the Scene.
# 
# This function constructs Scene objects with all possible combinations of 
# widths and heights up to 5 (5 is the maximum width/height since this makes 
# Scenes complex enough to verify that CentralFixation.make() operates 
# correctly). This also ensures that there is at most a 2 square clearance from
# the centre square(s) in the largest Scenes so scaling Scene size up past 5 * 5 
# shouldn't give erroneous results.
#
# The Scene constructed and Fixations expected are determined by the scenario
# number passed as a parameter.
def get_scenario_details(scenario)
  
  width = nil
  height = nil
  blind_squares = []
  expected_fixations = []

  case scenario
  
  ##############################################################################
  # 1 across, 1 down
  # 
  # |---|
  # | . | 1
  # |---|
  when 1
    width = 1
    height = 1
    expected_fixations = [[0,0]]
  
  # |---|
  # | x | 2
  # |---|
  when 2
    width = 1
    height = 1
    blind_squares = [[0,0]]
 
  ##############################################################################
  # 2 across, 1 down
  # 
  # |---|---|
  # | . | . | 3
  # |---|---|
  when 3
    width = 2
    height = 1
    expected_fixations = [[0,0],[1,0]]
  
  # |---|---|
  # | x | . | 4
  # |---|---|
  when 4
    width = 2
    height = 1
    expected_fixations = [[1,0]]
    blind_squares = [[0,0]]
  
  # |---|---|
  # | . | x | 5
  # |---|---|
  when 5
    width = 2
    height = 1
    expected_fixations = [[0,0]]
    blind_squares = [[1,0]]
    
  # |---|---|
  # | x | x | 6
  # |---|---|
  when 6
    width = 2
    height = 1
    blind_squares = [[0,0],[1,0]]
  
  ##############################################################################
  # 3 across, 1 down
  # 
  # |---|---|---|
  # |   | . |   | 7
  # |---|---|---|
  when 7
    width = 3
    height = 1
    expected_fixations = [[1,0]]
  
  # |---|---|---|
  # |   | x |   | 8
  # |---|---|---|
  when 8
    width = 3
    height = 1
    blind_squares = [[1,0]]
  
  ##############################################################################
  # 4 across, 1 down
  # 
  # |---|---|---|---|
  # |   | . | . |   | 9 
  # |---|---|---|---|
  when 9
    width = 4
    height = 1
    expected_fixations = [[1,0],[2,0]]
  
  # |---|---|---|---|
  # |   | x | . |   | 10
  # |---|---|---|---|
  when 10
    width = 4
    height = 1
    expected_fixations = [[2,0]]
    blind_squares = [[1,0]]
  
  # |---|---|---|---|
  # |   | . | x |   | 11
  # |---|---|---|---|
  when 11
    width = 4
    height = 1
    expected_fixations = [[1,0]]
    blind_squares = [[2,0]]
  
  # |---|---|---|---|
  # |   | x | x |   | 12
  # |---|---|---|---|
  when 12
    width = 4
    height = 1
    blind_squares = [[1,0],[2,0]]
  
  ##############################################################################
  # 5 across, 1 down
  # 
  # |---|---|---|---|---|
  # |   |   | . |   |   | 13
  # |---|---|---|---|---|
  when 13
    width = 5
    height = 1
    expected_fixations = [[2,0]]
  
  # |---|---|---|---|---|
  # |   |   | x |   |   | 14
  # |---|---|---|---|---|
  when 14
    width = 5
    height = 1
    blind_squares = [[2,0]]
  
  ##############################################################################
  ##############################################################################
  # 1 across, 2 down
  # 
  # |---|
  # | . |
  # |---| 15
  # | . |
  # |---|
  when 15
    width = 1
    height = 2
    expected_fixations = [[0,0],[0,1]]
  
  # |---|
  # | . |
  # |---| 16
  # | x |
  # |---|
  when 16
    width = 1
    height = 2
    expected_fixations = [[0,1]]
    blind_squares = [[0,0]]
  
  # |---|
  # | x |
  # |---| 17
  # | . |
  # |---|
  when 17
    width = 1
    height = 2
    expected_fixations = [[0,0]]
    blind_squares = [[0,1]]
  
  # |---|
  # | x |
  # |---| 18
  # | x |
  # |---|
  when 18
    width = 1
    height = 2
    blind_squares = [[0,0],[0,1]]
 
  ##############################################################################
  # 2 across, 2 down
  # 
  # |---|---|
  # | . | . |
  # |---|---| 19
  # | . | . |
  # |---|---|
  when 19
    width = 2
    height = 2
    expected_fixations = [[0,0],[0,1],[1,0],[1,1]]
  
  # |---|---|
  # | . | . |
  # |---|---| 20
  # | x | . |
  # |---|---|
  when 20
    width = 2
    height = 2
    expected_fixations = [[0,1],[1,0],[1,1]]
    blind_squares = [[0,0]]
  
  # |---|---|
  # | x | . |
  # |---|---| 21
  # | . | . |
  # |---|---|
  when 21
    width = 2
    height = 2
    expected_fixations = [[0,0],[1,0],[1,1]]
    blind_squares = [[0,1]]
  
  # |---|---|
  # | . | x |
  # |---|---| 22
  # | . | . |
  # |---|---|
  when 22
    width = 2
    height = 2
    expected_fixations = [[0,0],[0,1],[1,0]]
    blind_squares = [[1,1]]
  
  # |---|---|
  # | . | . |
  # |---|---| 23
  # | . | x |
  # |---|---|
  when 23
    width = 2
    height = 2
    expected_fixations = [[0,0],[0,1],[1,1]]
    blind_squares = [[1,0]]
  
  # |---|---|
  # | x | . |
  # |---|---| 24
  # | x | . |
  # |---|---|
  when 24
    width = 2
    height = 2
    expected_fixations = [[1,0],[1,1]]
    blind_squares = [[0,0],[0,1]]
  
  # |---|---|
  # | . | x |
  # |---|---| 25
  # | x | . |
  # |---|---|
  when 25
    width = 2
    height = 2
    expected_fixations = [[0,1],[1,0]]
    blind_squares = [[0,0],[1,1]]
  
  # |---|---|
  # | . | . |
  # |---|---| 26
  # | x | x |
  # |---|---|
  when 26
    width = 2
    height = 2
    expected_fixations = [[0,1],[1,1]]
    blind_squares = [[0,0],[1,0]]
  
  # |---|---|
  # | x | x |
  # |---|---| 27
  # | . | . |
  # |---|---|
  when 27
    width = 2
    height = 2
    expected_fixations = [[0,0],[1,0]]
    blind_squares = [[0,1],[1,1]]
  
  # |---|---|
  # | x | . |
  # |---|---| 28
  # | . | x |
  # |---|---|
  when 28
    width = 2
    height = 2
    expected_fixations = [[0,0],[1,1]]
    blind_squares = [[0,1],[1,0]]
  
  # |---|---|
  # | . | x |
  # |---|---| 29
  # | . | x |
  # |---|---|
  when 29
    width = 2
    height = 2
    expected_fixations = [[0,0],[0,1]]
    blind_squares = [[1,0],[1,1]]
  
  # |---|---|
  # | x | x |
  # |---|---| 30
  # | . | x |
  # |---|---|
  when 30
    width = 2
    height = 2
    expected_fixations = [[0,0]]
    blind_squares = [[0,1],[1,1],[1,0]]
  
  # |---|---|
  # | . | x |
  # |---|---| 31
  # | x | x |
  # |---|---|
  when 31
    width = 2
    height = 2
    expected_fixations = [[0,1]]
    blind_squares = [[0,0],[1,1],[1,0]]
  
  # |---|---|
  # | x | . |
  # |---|---| 32
  # | x | x |
  # |---|---|
  when 32
    width = 2
    height = 2
    expected_fixations = [[1,1]]
    blind_squares = [[0,0],[0,1],[1,0]]
  
  # |---|---|
  # | x | x |
  # |---|---| 33
  # | x | . |
  # |---|---|
  when 33
    width = 2
    height = 2
    expected_fixations = [[1,0]]
    blind_squares = [[0,0],[0,1],[1,1]]
  
  # |---|---|
  # | x | x |
  # |---|---| 34
  # | x | x |
  # |---|---|
  when 34
    width = 2
    height = 2
    blind_squares = [[0,0],[0,1],[1,0],[1,1]]
  
  ##############################################################################
  # 3 across, 2 down
  # 
  # |---|---|---|
  # |   | . |   |
  # |---|---|---| 35
  # |   | . |   |
  # |---|---|---|
  when 35
    width = 3
    height = 2
    expected_fixations = [[1,0],[1,1]]
  
  # |---|---|---|
  # |   | . |   |
  # |---|---|---| 36
  # |   | x |   |
  # |---|---|---|
  when 36
    width = 3
    height = 2
    expected_fixations = [[1,1]]
    blind_squares = [[1,0]]
  
  # |---|---|---|
  # |   | x |   |
  # |---|---|---| 37
  # |   | . |   |
  # |---|---|---|
  when 37
    width = 3
    height = 2
    expected_fixations = [[1,0]]
    blind_squares = [[1,1]]
  
  # |---|---|---|
  # |   | x |   |
  # |---|---|---| 38
  # |   | x |   |
  # |---|---|---|
  when 38
    width = 3
    height = 2
    blind_squares = [[1,0],[1,1]]
  
  ##############################################################################
  # 4 across, 2 down
  # 
  # |---|---|---|---|
  # |   | . | . |   |
  # |---|---|---|---| 39
  # |   | . | . |   |
  # |---|---|---|---|
  when 39
    width = 4
    height = 2
    expected_fixations = [[1,0],[1,1],[2,0],[2,1]]
  
  # |---|---|---|---|
  # |   | . | . |   |
  # |---|---|---|---| 40
  # |   | x | . |   |
  # |---|---|---|---|
  when 40
    width = 4
    height = 2
    expected_fixations = [[1,1],[2,0],[2,1]]
    blind_squares = [[1,0]]
  
  # |---|---|---|---|
  # |   | x | . |   |
  # |---|---|---|---| 41
  # |   | . | . |   |
  # |---|---|---|---|
  when 41
    width = 4
    height = 2
    expected_fixations = [[1,0],[2,0],[2,1]]
    blind_squares = [[1,1]]
  
  # |---|---|---|---|
  # |   | . | x |   |
  # |---|---|---|---| 42
  # |   | . | . |   |
  # |---|---|---|---|
  when 42
    width = 4
    height = 2
    expected_fixations = [[1,0],[1,1],[2,0]]
    blind_squares = [[2,1]]
  
  # |---|---|---|---|
  # |   | . | . |   |
  # |---|---|---|---| 43
  # |   | . | x |   |
  # |---|---|---|---|
  when 43
    width = 4
    height = 2
    expected_fixations = [[1,0],[1,1],[2,1]]
    blind_squares = [[2,0]]
  
  # |---|---|---|---|
  # |   | x | . |   |
  # |---|---|---|---| 44
  # |   | x | . |   |
  # |---|---|---|---|
  when 44
    width = 4
    height = 2
    expected_fixations = [[2,0],[2,1]]
    blind_squares = [[1,0],[1,1]]
  
  # |---|---|---|---|
  # |   | . | x |   |
  # |---|---|---|---| 45
  # |   | x | . |   |
  # |---|---|---|---|
  when 45
    width = 4
    height = 2
    expected_fixations = [[1,1],[2,0]]
    blind_squares = [[1,0],[2,1]]
  
  # |---|---|---|---|
  # |   | . | . |   |
  # |---|---|---|---| 46
  # |   | x | x |   |
  # |---|---|---|---|
  when 46
    width = 4
    height = 2
    expected_fixations = [[1,1],[2,1]]
    blind_squares = [[1,0],[2,0]]
  
  # |---|---|---|---|
  # |   | x | x |   |
  # |---|---|---|---| 47
  # |   | . | . |   |
  # |---|---|---|---|
  when 47
    width = 4
    height = 2
    expected_fixations = [[1,0],[2,0]]
    blind_squares = [[1,1],[2,1]]
  
  # |---|---|---|---|
  # |   | x | . |   |
  # |---|---|---|---| 48
  # |   | . | x |   |
  # |---|---|---|---|
  when 48
    width = 4
    height = 2
    expected_fixations = [[1,0],[2,1]]
    blind_squares = [[1,1],[2,0]]
  
  # |---|---|---|---|
  # |   | . | x |   |
  # |---|---|---|---| 49
  # |   | . | x |   |
  # |---|---|---|---|
  when 49
    width = 4
    height = 2
    expected_fixations = [[1,0],[1,1]]
    blind_squares = [[2,0],[2,1]]
  
  # |---|---|---|---|
  # |   | x | x |   |
  # |---|---|---|---| 50
  # |   | . | x |   |
  # |---|---|---|---|
  when 50
    width = 4
    height = 2
    expected_fixations = [[1,0]]
    blind_squares = [[1,1],[2,0],[2,1]]
  
  # |---|---|---|---|
  # |   | . | x |   |
  # |---|---|---|---| 51
  # |   | x | x |   |
  # |---|---|---|---|
  when 51
    width = 4
    height = 2
    expected_fixations = [[1,1]]
    blind_squares = [[1,0],[2,0],[2,1]]
  
  # |---|---|---|---|
  # |   | x | . |   |
  # |---|---|---|---| 52
  # |   | x | x |   |
  # |---|---|---|---|
  when 52
    width = 4
    height = 2
    expected_fixations = [[2,1]]
    blind_squares = [[1,0],[1,1],[2,0]]
  
  # |---|---|---|---|
  # |   | x | x |   |
  # |---|---|---|---| 53
  # |   | x | . |   |
  # |---|---|---|---|
  when 53
    width = 4
    height = 2
    expected_fixations = [[2,0]]
    blind_squares = [[1,0],[1,1],[2,1]]
  
  # |---|---|---|---|
  # |   | x | x |   |
  # |---|---|---|---| 54
  # |   | x | x |   |
  # |---|---|---|---|
  when 54
    width = 4
    height = 2
    blind_squares = [[1,0],[1,1],[2,0],[2,1]]
  
  ##############################################################################
  # 5 across, 2 down
  # 
  # |---|---|---|---|---|
  # |   |   | . |   |   |
  # |---|---|---|---|---| 55
  # |   |   | . |   |   |
  # |---|---|---|---|---|
  when 55
    width = 5
    height = 2
    expected_fixations = [[2,0],[2,1]]
  
  # |---|---|---|---|---|
  # |   |   | . |   |   |
  # |---|---|---|---|---| 56
  # |   |   | x |   |   |
  # |---|---|---|---|---|
  when 56
    width = 5
    height = 2
    expected_fixations = [[2,1]]
    blind_squares = [[2,0]]
  
  # |---|---|---|---|---|
  # |   |   | x |   |   |
  # |---|---|---|---|---| 57
  # |   |   | . |   |   |
  # |---|---|---|---|---|
  when 57
    width = 5
    height = 2
    expected_fixations = [[2,0]]
    blind_squares = [[2,1]]

  # |---|---|---|---|---|
  # |   |   | x |   |   |
  # |---|---|---|---|---| 58
  # |   |   | x |   |   |
  # |---|---|---|---|---|
  when 58
    width = 5
    height = 2
    blind_squares = [[2,0],[2,1]]
  
  ##############################################################################
  ##############################################################################
  # 1 across, 3 down
  # 
  # |---|
  # |   |
  # |---|
  # | . | 59
  # |---|
  # |   |
  # |---|
  when 59
    width = 1
    height = 3
    expected_fixations = [[0,1]]
  
  # |---|
  # |   |
  # |---|
  # | x | 60
  # |---|
  # |   |
  # |---|
  when 60
    width = 1
    height = 3
    blind_squares = [[0,1]]
 
  ##############################################################################
  # 2 across, 3 down
  # 
  # |---|---|
  # |   |   |
  # |---|---|
  # | . | . | 61
  # |---|---|
  # |   |   |
  # |---|---|
  when 61
    width = 2
    height = 3
    expected_fixations = [[0,1],[1,1]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | x | . | 62
  # |---|---|
  # |   |   |
  # |---|---|
  when 62
    width = 2
    height = 3
    expected_fixations = [[1,1]]
    blind_squares = [[0,1]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | . | x | 63
  # |---|---|
  # |   |   |
  # |---|---|
  when 63
    width = 2
    height = 3
    expected_fixations = [[0,1]]
    blind_squares = [[1,1]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | x | x | 64
  # |---|---|
  # |   |   |
  # |---|---|
  when 64
    width = 2
    height = 3
    blind_squares = [[0,1],[1,1]]
  
  ##############################################################################
  # 3 across, 3 down
  # 
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  # |   | . |   | 65
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  when 65
    width = 3
    height = 3
    expected_fixations = [[1,1]]
  
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  # |   | x |   | 66
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  when 66
    width = 3
    height = 3
    blind_squares = [[1,1]]
  
  ##############################################################################
  # 4 across, 3 down
  # 
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | . | . |   | 67
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 67
    width = 4
    height = 3
    expected_fixations = [[1,1],[2,1]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | x | . |   | 68
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 68
    width = 4
    height = 3
    expected_fixations = [[2,1]]
    blind_squares = [[1,1]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | . | x |   | 69
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 69
    width = 4
    height = 3
    expected_fixations = [[1,1]]
    blind_squares = [[2,1]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | x | x |   | 70
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 70
    width = 4
    height = 3
    blind_squares = [[1,1],[2,1]]
  
  ##############################################################################
  # 5 across, 3 down
  # 
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  # |   |   | . |   |   | 71
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  when 71
    width = 5
    height = 3
    expected_fixations = [[2,1]]
  
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  # |   |   | x |   |   | 72
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  when 72
    width = 5
    height = 3
    blind_squares = [[2,1]]
  
  ##############################################################################
  ##############################################################################
  # 1 across, 4 down
  # 
  # |---|
  # |   |
  # |---|
  # | . |
  # |---| 73
  # | . |
  # |---|
  # |   |
  # |---|
  when 73
    width = 1
    height = 4
    expected_fixations = [[0,1],[0,2]]
  
  # |---|
  # |   |
  # |---|
  # | . |
  # |---| 74
  # | x |
  # |---|
  # |   |
  # |---|
  when 74
    width = 1
    height = 4
    expected_fixations = [[0,2]]
    blind_squares = [[0,1]]
  
  # |---|
  # |   |
  # |---|
  # | x |
  # |---| 75
  # | . |
  # |---|
  # |   |
  # |---|
  when 75
    width = 1
    height = 4
    expected_fixations = [[0,1]]
    blind_squares = [[0,2]]
  
  # |---|
  # |   |
  # |---|
  # | x |
  # |---| 76
  # | x |
  # |---|
  # |   |
  # |---|
  when 76
    width = 1
    height = 4
    blind_squares = [[0,1],[0,2]]
 
  ##############################################################################
  # 2 across, 4 down
  # 
  # |---|---|
  # |   |   |
  # |---|---|
  # | . | . |
  # |---|---| 77
  # | . | . |
  # |---|---|
  # |   |   |
  # |---|---|
  when 77
    width = 2
    height = 4
    expected_fixations = [[0,1],[0,2],[1,1],[1,2]]
    blind_squares = []
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | . | . |
  # |---|---| 78
  # | x | . |
  # |---|---|
  # |   |   |
  # |---|---|
  when 78
    width = 2
    height = 4
    expected_fixations = [[0,2],[1,1],[1,2]]
    blind_squares = [[0,1]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | x | . |
  # |---|---| 79
  # | . | . |
  # |---|---|
  # |   |   |
  # |---|---|
  when 79
    width = 2
    height = 4
    expected_fixations = [[0,1],[1,1],[1,2]]
    blind_squares = [[0,2]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | . | x |
  # |---|---| 80
  # | . | . |
  # |---|---|
  # |   |   |
  # |---|---|
  when 80
    width = 2
    height = 4
    expected_fixations = [[0,1],[0,2],[1,1]]
    blind_squares = [[1,2]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | . | . |
  # |---|---| 81
  # | . | x |
  # |---|---|
  # |   |   |
  # |---|---|
  when 81
    width = 2
    height = 4
    expected_fixations = [[0,1],[0,2],[1,2]]
    blind_squares = [[1,1]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | x | . |
  # |---|---| 82
  # | x | . |
  # |---|---|
  # |   |   |
  # |---|---|
  when 82
    width = 2
    height = 4
    expected_fixations = [[1,1],[1,2]]
    blind_squares = [[0,1],[0,2]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | . | x |
  # |---|---| 83
  # | x | . |
  # |---|---|
  # |   |   |
  # |---|---|
  when 83
    width = 2
    height = 4
    expected_fixations = [[0,2],[1,1]]
    blind_squares = [[0,1],[1,2]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | . | . |
  # |---|---| 84
  # | x | x |
  # |---|---|
  # |   |   |
  # |---|---|
  when 84
    width = 2
    height = 4
    expected_fixations = [[0,2],[1,2]]
    blind_squares = [[0,1],[1,1]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | x | x |
  # |---|---| 85
  # | . | . |
  # |---|---|
  # |   |   |
  # |---|---|
  when 85
    width = 2
    height = 4
    expected_fixations = [[0,1],[1,1]]
    blind_squares = [[0,2],[1,2]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | x | . |
  # |---|---| 86
  # | . | x |
  # |---|---|
  # |   |   |
  # |---|---|
  when 86
    width = 2
    height = 4
    expected_fixations = [[0,1],[1,2]]
    blind_squares = [[0,2],[1,1]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | . | x |
  # |---|---| 87
  # | . | x |
  # |---|---|
  # |   |   |
  # |---|---|
  when 87
    width = 2
    height = 4
    expected_fixations = [[0,1],[0,2]]
    blind_squares = [[1,1],[1,2]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | x | x |
  # |---|---| 88
  # | . | x |
  # |---|---|
  # |   |   |
  # |---|---|
  when 88
    width = 2
    height = 4
    expected_fixations = [[0,1]]
    blind_squares = [[0,2],[1,1],[1,2]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | . | x |
  # |---|---| 89
  # | x | x |
  # |---|---|
  # |   |   |
  # |---|---|
  when 89
    width = 2
    height = 4
    expected_fixations = [[0,2]]
    blind_squares = [[0,1],[1,1],[1,2]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | x | . |
  # |---|---| 90
  # | x | x |
  # |---|---|
  # |   |   |
  # |---|---|
  when 90
    width = 2
    height = 4
    expected_fixations = [[1,2]]
    blind_squares = [[0,1],[0,2],[1,1]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | x | x |
  # |---|---| 91
  # | x | . |
  # |---|---|
  # |   |   |
  # |---|---|
  when 91
    width = 2
    height = 4
    expected_fixations = [[1,1]]
    blind_squares = [[0,1],[0,2],[1,2]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # | x | x |
  # |---|---| 92
  # | x | x |
  # |---|---|
  # |   |   |
  # |---|---|
  when 92
    width = 2
    height = 4
    blind_squares = [[0,1],[0,2],[1,1],[1,2]]
  
  ##############################################################################
  # 3 across, 4 down
  # 
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  # |   | . |   |
  # |---|---|---| 93
  # |   | . |   |
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  when 93
    width = 3
    height = 4
    expected_fixations = [[1,1],[1,2]]
  
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  # |   | . |   |
  # |---|---|---| 94
  # |   | x |   |
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  when 94
    width = 3
    height = 4
    expected_fixations = [[1,2]]
    blind_squares = [[1,1]]
  
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  # |   | x |   |
  # |---|---|---| 95
  # |   | . |   |
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  when 95
    width = 3
    height = 4
    expected_fixations = [[1,1]]
    blind_squares = [[1,2]]
  
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  # |   | x |   |
  # |---|---|---| 96
  # |   | x |   |
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  when 96
    width = 3
    height = 4
    blind_squares = [[1,1],[1,2]]
  
  ##############################################################################
  # 4 across, 4 down
  # 
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | . | . |   |
  # |---|---|---|---| 97
  # |   | . | . |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 97
    width = 4
    height = 4
    expected_fixations = [[1,1],[1,2],[2,1],[2,2]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | . | . |   |
  # |---|---|---|---| 98
  # |   | x | . |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 98
    width = 4
    height = 4
    expected_fixations = [[1,2],[2,1],[2,2]]
    blind_squares = [[1,1]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | x | . |   |
  # |---|---|---|---| 99
  # |   | . | . |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 99
    width = 4
    height = 4
    expected_fixations = [[1,1],[2,1],[2,2]]
    blind_squares = [[1,2]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | . | x |   |
  # |---|---|---|---| 100
  # |   | . | . |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 100
    width = 4
    height = 4
    expected_fixations = [[1,1],[1,2],[2,1]]
    blind_squares = [[2,2]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | . | . |   |
  # |---|---|---|---| 101
  # |   | . | x |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 101
    width = 4
    height = 4
    expected_fixations = [[1,1],[1,2],[2,2]]
    blind_squares = [[2,1]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | x | . |   |
  # |---|---|---|---| 102
  # |   | x | . |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 102
    width = 4
    height = 4
    expected_fixations = [[2,1],[2,2]]
    blind_squares = [[1,1],[1,2]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | . | x |   |
  # |---|---|---|---| 103
  # |   | x | . |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 103
    width = 4
    height = 4
    expected_fixations = [[1,2],[2,1]]
    blind_squares = [[1,1],[2,2]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | . | . |   |
  # |---|---|---|---| 104
  # |   | x | x |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 104
    width = 4
    height = 4
    expected_fixations = [[1,2],[2,2]]
    blind_squares = [[1,1],[2,1]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | x | x |   |
  # |---|---|---|---| 105
  # |   | . | . |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 105
    width = 4
    height = 4
    expected_fixations = [[1,1],[2,1]]
    blind_squares = [[1,2],[2,2]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | x | . |   |
  # |---|---|---|---| 106
  # |   | . | x |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 106
    width = 4
    height = 4
    expected_fixations = [[1,1],[2,2]]
    blind_squares = [[1,2],[2,1]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | . | x |   |
  # |---|---|---|---| 107
  # |   | . | x |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 107
    width = 4
    height = 4
    expected_fixations = [[1,1],[1,2]]
    blind_squares = [[2,1],[2,2]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | x | x |   |
  # |---|---|---|---| 108
  # |   | . | x |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 108
    width = 4
    height = 4
    expected_fixations = [[1,1]]
    blind_squares = [[1,2],[2,1],[2,2]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | . | x |   |
  # |---|---|---|---| 109
  # |   | x | x |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 109
    width = 4
    height = 4
    expected_fixations = [[1,2]]
    blind_squares = [[1,1],[2,1],[2,2]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | x | . |   |
  # |---|---|---|---| 110
  # |   | x | x |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 110
    width = 4
    height = 4
    expected_fixations = [[2,2]]
    blind_squares = [[1,1],[1,2],[2,1]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | x | x |   |
  # |---|---|---|---| 111
  # |   | x | . |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 111
    width = 4
    height = 4
    expected_fixations = [[2,1]]
    blind_squares = [[1,1],[1,2],[2,2]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | x | x |   |
  # |---|---|---|---| 112
  # |   | x | x |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 112
    width = 4
    height = 4
    blind_squares = [[1,1],[1,2],[2,1],[2,2]]
  
  ##############################################################################
  # 5 across, 4 down
  # 
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  # |   |   | . |   |   |
  # |---|---|---|---|---| 113
  # |   |   | . |   |   |
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  when 113
    width = 5
    height = 4
    expected_fixations = [[2,1],[2,2]]
  
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  # |   |   | . |   |   |
  # |---|---|---|---|---| 114
  # |   |   | x |   |   |
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  when 114
    width = 5
    height = 4
    expected_fixations = [[2,2]]
    blind_squares = [[2,1]]
  
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  # |   |   | x |   |   |
  # |---|---|---|---|---| 115
  # |   |   | . |   |   |
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  when 115
    width = 5
    height = 4
    expected_fixations = [[2,1]]
    blind_squares = [[2,2]]
  
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  # |   |   | x |   |   |
  # |---|---|---|---|---| 116
  # |   |   | x |   |   |
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  when 116
    width = 5
    height = 4
    blind_squares = [[2,1],[2,2]]
  
  ##############################################################################
  ##############################################################################
  # 1 across, 5 down
  
  # |---|
  # |   |
  # |---|
  # |   |
  # |---|
  # | . | 117
  # |---|
  # |   |
  # |---|
  # |   |
  # |---|
  when 117
    width = 1
    height = 5
    expected_fixations = [[0,2]]
  
  # |---|
  # |   |
  # |---|
  # |   |
  # |---|
  # | x | 118
  # |---|
  # |   |
  # |---|
  # |   |
  # |---|
  when 118
    width = 1
    height = 5
    blind_squares = [[0,2]]
  
  ##############################################################################
  # 2 across, 5 down
  
  # |---|---|
  # |   |   |
  # |---|---|
  # |   |   |
  # |---|---|
  # | . | . | 119
  # |---|---|
  # |   |   |
  # |---|---|
  # |   |   |
  # |---|---|
  when 119
    width = 2
    height = 5
    expected_fixations = [[0,2],[1,2]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # |   |   |
  # |---|---|
  # | x | . | 120
  # |---|---|
  # |   |   |
  # |---|---|
  # |   |   |
  # |---|---|
  when 120
    width = 2
    height = 5
    expected_fixations = [[1,2]]
    blind_squares = [[0,2]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # |   |   |
  # |---|---|
  # | . | x | 121
  # |---|---|
  # |   |   |
  # |---|---|
  # |   |   |
  # |---|---|
  when 121
    width = 2
    height = 5
    expected_fixations = [[0,2]]
    blind_squares = [[1,2]]
  
  # |---|---|
  # |   |   |
  # |---|---|
  # |   |   |
  # |---|---|
  # | x | x | 122
  # |---|---|
  # |   |   |
  # |---|---|
  # |   |   |
  # |---|---|
  when 122
    width = 2
    height = 5
    blind_squares = [[0,2],[1,2]]
  
  ##############################################################################
  # 3 across, 5 down
  
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  # |   | . |   | 123
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  when 123
    width = 3
    height = 5
    expected_fixations = [[1,2]]
  
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  # |   | x |   | 124
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  # |   |   |   |
  # |---|---|---|
  when 124
    width = 3
    height = 5
    blind_squares = [[1,2]]
  
  ##############################################################################
  # 4 across, 5 down
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | . | . |   | 125
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 125
    width = 4
    height = 5
    expected_fixations = [[1,2],[2,2]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | x | . |   | 126
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 126
    width = 4
    height = 5
    expected_fixations = [[2,2]]
    blind_squares = [[1,2]]
  
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | . | x |   | 127
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 127
    width = 4
    height = 5
    expected_fixations = [[1,2]]
    blind_squares = [[2,2]]
    
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   | x | x |   | 128
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  # |   |   |   |   |
  # |---|---|---|---|
  when 128
    width = 4
    height = 5
    blind_squares = [[1,2],[2,2]]
  
  ##############################################################################
  # 5 across, 5 down
  
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  # |   |   | . |   |   | 129
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  when 129
    width = 5
    height = 5
    expected_fixations = [[2,2]]
  
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  # |   |   | x |   |   | 130
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  # |   |   |   |   |   |
  # |---|---|---|---|---|
  when 130
    width = 5
    height = 5
    blind_squares = [[2,2]]
  end
  
  ###############################
  ##### CONSTRUCT THE SCENE #####
  ###############################
  
  scene = Scene.new("", width, height, 1, 1, nil)
  
  #The scene will be entirely blind at first so turn all squares not defined to
  #be blind into empty squares.
  empty = Scene.getEmptySquareToken()
  for col in 0...scene.getWidth()
    for row in 0...scene.getHeight()
      
      square_should_be_blind = false
      for blind_square in blind_squares
        if blind_square[0] == col and blind_square[1] == row
          square_should_be_blind = true
        end
      end
      
      if !square_should_be_blind
        scene._scene.get(col).set(row, SceneObject.new(empty))
      end
      
    end
  end
  
  ######################################
  ##### CONVERT EXPECTED FIXATIONS #####
  ######################################
  expected_fixations = expected_fixations.map!{
    |expected_fixation| Square.new(expected_fixation[0], expected_fixation[1]).toString()
  }
  
  return [scene, expected_fixations]
end
