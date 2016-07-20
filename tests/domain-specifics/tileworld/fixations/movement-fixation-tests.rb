unit_test "constructor" do
  Fixation.class_eval{ field_accessor :_timeDecidedUpon }
  MovementFixation.class_eval{ field_accessor :_associatedModel }
  
  time = 0
  model = Chrest.new(time, true)
  time_decided_upon = (time += 100)
  fixation = MovementFixation.new(model, time_decided_upon)
  
  assert_equal(model, fixation._associatedModel)
  assert_equal(time_decided_upon, fixation._timeDecidedUpon)
end

################################################################################
# Tests the "make()" function by using 23 scenarios, each of which is repeated
# 200 times to ensure consistency of behaviour:
#
# Scenario Descriptions
# =====================
# 
# - Scenario 1
#   ~ Fixation made at time before Fixation is due to be performed
#
# - Scenario 2
#   ~ Fixation made at time when Fixation is to be performed
#   ~ No Fixations previously performed (Perceiver Fixation structure empty)
#
# - Scenario 3
#   ~ Fixation made at time when Fixation is to be performed
#   ~ No Fixations previously performed (Perceiver Fixation structure not empty
#     but last Fixation not performed successfully)
#
# - Scenario 4
#   ~ Fixation made at time when Fixation is to be performed
#   ~ Fixation previously performed 
#   ~ Column previously fixated on isn't represented in the Scene to make
#     the MovementFixation on (row is)
#
# - Scenario 5
#   ~ Fixation made at time when Fixation is to be performed
#   ~ Fixation previously performed 
#   ~ Row previously fixated on isn't represented in the Scene to make
#     the MovementFixation on (column is)
#     
# - Scenario 6
#   ~ Fixation made at time when Fixation is to be performed
#   ~ Fixation previously performed 
#   ~ Coordinates previously fixated on are represented in the Scene to make
#     the MovementFixation on
#   ~ SceneObject on coordinates previously fixated on returns a hole.
#   
# - Scenario 7
#   ~ Fixation made at time when Fixation is to be performed
#   ~ Fixation previously performed 
#   ~ Coordinates previously fixated on are represented in the Scene to make
#     the MovementFixation on
#   ~ SceneObject on coordinates previously fixated on returns an empty square.
#   
# - Scenario 8
#   ~ Fixation made at time when Fixation is to be performed
#   ~ Fixation previously performed 
#   ~ Coordinates previously fixated on are represented in the Scene to make
#     the MovementFixation on
#   ~ SceneObject on coordinates previously fixated on returns a blind square.
#   
# Scenes used for Scenarios 1-8
# =============================
# 
# Notation used:
#   - Empty squares are denoted by "."
#   - The creator is denoted by "SELF"
#   - The SceneObject to fixate on is denoted by "FOBJ"
# 
#   |----|----|----|
# 2 |  . | .  | .  |
#   |----|----|----|
# 1 |  . |FOBJ| .  |
#   |----|----|----|
# 0 |  . |SELF| .  |
#   |----|----|----|
#     0    1    2    
#
# Scenarios from 9 onwards should pass but with various Squares proposed for
# the fixation to make.  The following scenarios should be run with 3 different
# types of movable Tileworld SceneObjects:
# 
# 1. Tile (scenarios 9 to 13)
# 2. Opponent (scenarios 14 to 18)
# 3. Creator (scenarios 19 to 23)
# 
# There are 5 scenarios to be run for each movable Tileworld SceneObject from
# n to n + 4:
# 
# - Scenario n
#   ~ Fixation made at time when Fixation is to be performed
#   ~ Fixation previously performed 
#   ~ Coordinates previously fixated on are represented in the Scene to make
#     the MovementFixation on
#   ~ SceneObject on coordinates previously fixated on contains a moveable 
#     SceneObject.
#     + Should only be able to move to 1 Square, other squares occupied by 
#       blind Squares (tests that blind Squares are not proposed as Fixations).
#       
# - Scenario n + 1
#   ~ Fixation made at time when Fixation is to be performed
#   ~ Fixation previously performed 
#   ~ Coordinates previously fixated on are represented in the Scene to make
#     the MovementFixation on
#   ~ SceneObject on coordinates previously fixated on contains a moveable 
#     SceneObject.
#     + Moveable SceneObject located on the most north-eastern Square in the 
#       Scene being fixated on.  The moveable SceneObject should only be able to 
#       move to 2 Squares in this case: the Square to its west and the Square to
#       its south (tests that Squares with an x/y value greater than the maximum 
#       x/y coordinates of the Scene fixated on are not proposed as Fixations).
#       
# - Scenario n + 2
#   ~ Fixation made at time when Fixation is to be performed
#   ~ Fixation previously performed 
#   ~ Coordinates previously fixated on are represented in the Scene to make
#     the MovementFixation on
#   ~ SceneObject on coordinates previously fixated on contains a moveable 
#     SceneObject.
#     + Moveable SceneObject located on the most south-westerley Square in the 
#       Scene being fixated on.  The moveable SceneObject should only be able to 
#       move to 2 Squares in this case: the Square to its east and the Square to
#       its north (tests that Squares with an x/y value less than the minimum 
#       x/y coordinates of the Scene fixated on are not proposed as Fixations).
#       
# - Scenario n + 3
#   ~ Fixation made at time when Fixation is to be performed
#   ~ Fixation previously performed 
#   ~ Coordinates previously fixated on are represented in the Scene to make
#     the MovementFixation on
#   ~ SceneObject on coordinates previously fixated on contains a moveable 
#     SceneObject.
#     + Moveable SceneObject located on the most north-eastern Square in the 
#       Scene being fixated on and both the Square to its west and the Square to
#       its south are blind Squares (tests that a combination of blind Squares
#       and potential movement Fixations are not proposed as Fixations and that 
#       the function will return null when no valid Fixations are available).
#       
# - Scenario n + 4
#   ~ Fixation made at time when Fixation is to be performed
#   ~ Fixation previously performed 
#   ~ Coordinates previously fixated on are represented in the Scene to make
#     the MovementFixation on
#   ~ SceneObject on coordinates previously fixated on contains a moveable 
#     SceneObject.      
#     + Moveable SceneObject located in the centre of the Scene and Squares to 
#       the north, east, south and west of the moveable SceneObject are 
#       represented in the Scene and are not occupied by blind squares.  
#       Instead, these Squares are occupied either by empty Squares or other,
#       non-blind Square SceneObjects in Tileworld, including the creator (tests 
#       that Squares to move to which contain SceneObjects other than blind 
#       Squares are still proposed as potential Fixations).  Note that due to 
#       the fact each scenario is repeated 200 times, all variations of 
#       non-blind SceneObjects should be included on the Squares surrounding the 
#       moveable SceneObject.
#       
# The Scenes used for "n" scenarios are displayed below.  Note that, if the 
# creator is the SceneObject to fixate on then, the SceneObject denoted by 
# "MOBJ" is replaced by the SceneObject denoting the creator and the Square 
# occupied by the creator will be empty.
#       
# Scenes used for "n" Scenarios
# =============================
# 
# Notation used:
#   - Blind squares are denoted by "*"
#   - Empty squares are denoted by "."
#   - The creator is denoted by "SELF"
#   - The moveable SceneObject is denoted by "MOBJ"
#
# - Scenario n        - Scenario n + 1    - Scenario n + 2    - Scenario n + 3
# 
#   |----|----|----|    |----|----|----|    |----|----|----|    |----|----|----|
# 2 |  . | *  | .  |    | .  | .  |MOBJ|    | .  | .  | .  |    |  . | *  |MOBJ|
#   |----|----|----|    |----|----|----|    |----|----|----|    |----|----|----|
# 1 |  * |MOBJ| *  |    | .  | .  | .  |    | .  | .  | .  |    |  . | .  | *  |
#   |----|----|----|    |----|----|----|    |----|----|----|    |----|----|----|
# 0 |  . |SELF| .  |    | .  |SELF| .  |    |MOBJ|SELF| .  |    |  . |SELF| .  |
#   |----|----|----|    |----|----|----|    |----|----|----|    |----|----|----|
#     0    1    2         0    1    2         0    1    2         0    1    2
#
# - Scenario n + 4
#
#   |----|----|----|
# 2 |  . |obj1| .  |
#   |----|----|----|
# 1 |obj4|MOBJ|obj2|
#   |----|----|----|
# 0 |  . |SELF| .  |
#   |----|----|----|
#     0    1    2
#
# Expected Output
# ===============
# 
# - For scenarios 1-8, null should be returned.
# - For scenario n, coordinates (1, 0) should be returned.
# - For scenario n + 1, coordinates (2, 1) or (1, 2) should be returned.
# - For scenario n + 2, coordinates (1, 0) or (0, 1) should be returned.
# - For scenario n + 3, null should be returned.
# - For scenario n + 4, coordinates (1, 0) or (0, 1) or (1, 2) or (2, 1) should
#   be returned.
unit_test "make" do
  
  ####################################################
  ##### SET-UP ACCESS TO PRIVATE INSTANCE FIELDS #####
  ####################################################
  
  Fixation.class_eval{ 
    field_accessor :_timeDecidedUpon,
    :_performanceTime, 
    :_performed,
    :_scene,
    :_colFixatedOn,
    :_rowFixatedOn,
    :_objectSeen
  }
  
  Scene.class_eval{
    field_accessor :_scene
  }
  scene_minimum_domain_specific_column_field = Scene.java_class.declared_field("_minimumDomainSpecificColumn")
  scene_minimum_domain_specific_column_field.accessible = true
  scene_minimum_domain_specific_row_field = Scene.java_class.declared_field("_minimumDomainSpecificRow")
  scene_minimum_domain_specific_row_field.accessible = true
  scene_width_field = Scene.java_class.declared_field("_width")
  scene_width_field.accessible = true
  scene_height_field = Scene.java_class.declared_field("_height")
  scene_height_field.accessible = true
  
  Chrest.class_eval{
    field_accessor :_saccadeTime
  }
  chrest_perceiver_field = Chrest.java_class.declared_field("_perceiver")
  chrest_perceiver_field.accessible = true
  
  perceiver_fixations_field = Perceiver.java_class.declared_field("_fixations")
  perceiver_fixations_field.accessible = true
  
  #########################
  ##### SCENARIO LOOP #####
  #########################
  
  for scenario in 1..23 
    200.times do
      
      time = 0

      ##################################
      ##### CONSTRUCT Chrest MODEL #####
      ##################################

      model = Chrest.new(time, true)

      ###########################
      ##### CONSTRUCT Scene #####
      ###########################

      scene = Scene.new("", 3, 3, 2, 2, nil)

      ##### Construct and place creator
      creator_location = [1,0]
      if scenario >= 19 then creator_location = [] end
      if !creator_location.empty? then scene._scene.get(creator_location[0]).set(creator_location[1], SceneObject.new(Scene::CREATOR_TOKEN)) end

      ##### Construct and place SceneObject to fixate on.

      # Set type of object to fixate on
      object_to_fixate_on_type = TileworldDomain::TILE_SCENE_OBJECT_TYPE_TOKEN

      if scenario == 6
        object_to_fixate_on_type = TileworldDomain::HOLE_SCENE_OBJECT_TYPE_TOKEN
      elsif scenario == 7
        object_to_fixate_on_type = Scene::EMPTY_SQUARE_TOKEN
      elsif scenario == 8
        object_to_fixate_on_type = Scene::BLIND_SQUARE_TOKEN
      elsif scenario >= 14 && scenario < 19
        object_to_fixate_on_type = TileworldDomain::OPPONENT_SCENE_OBJECT_TYPE_TOKEN
      elsif scenario >= 19
        object_to_fixate_on_type = Scene::CREATOR_TOKEN
      end

      # Set location of object to fixate on
      object_to_fixate_on_coords = [1,1]
      if [10, 12, 15, 17, 20, 22].include?(scenario)
          object_to_fixate_on_coords = [2,2]
      elsif [11, 16, 21].include?(scenario)
        object_to_fixate_on_coords = [0,0]
      end

      # Construct object to fixate on and place
      scene._scene.get(object_to_fixate_on_coords[0]).set(object_to_fixate_on_coords[1], SceneObject.new(object_to_fixate_on_type))

      ##### Construct extra SceneObjects (as required)
      extra_scene_objects = []
      if [9, 14, 19].include?(scenario)
        extra_scene_objects.push([0,1,Scene::BLIND_SQUARE_TOKEN])
        extra_scene_objects.push([1,2,Scene::BLIND_SQUARE_TOKEN])
        extra_scene_objects.push([2,1,Scene::BLIND_SQUARE_TOKEN])
      elsif [12, 17, 22].include?(scenario)
        extra_scene_objects.push([1,2,Scene::BLIND_SQUARE_TOKEN])
        extra_scene_objects.push([2,1,Scene::BLIND_SQUARE_TOKEN])
      elsif [13, 18, 23].include?(scenario)
        potential_object_types = [
          Scene::EMPTY_SQUARE_TOKEN,
          TileworldDomain::HOLE_SCENE_OBJECT_TYPE_TOKEN,
          TileworldDomain::OPPONENT_SCENE_OBJECT_TYPE_TOKEN,
          TileworldDomain::TILE_SCENE_OBJECT_TYPE_TOKEN,
        ]
        extra_scene_objects.push([0,1,potential_object_types.sample])
        extra_scene_objects.push([1,0,potential_object_types.sample])
        extra_scene_objects.push([1,2,potential_object_types.sample])
        extra_scene_objects.push([2,1,potential_object_types.sample])
      end

      for extra_scene_object in extra_scene_objects
        scene._scene.get(extra_scene_object[0]).set(extra_scene_object[1], SceneObject.new(extra_scene_object[2]))
      end

      ##### Fill in empty Squares
      for col in 0...scene_width_field.value(scene)
        for row in 0...scene_height_field.value(scene)

          # Set a boolean flag that controls if a SceneObject representing an
          # empty Square should be encoded on the coordinates being processed.
          encode_empty_square = true

          # If the coordinates being processed are occupied by the creator or 
          # the SceneObject to fixate on, don't encode them as an empty Square.
          if 
            (!creator_location.empty? && col == creator_location[0] && row == creator_location[1]) ||
            (col == object_to_fixate_on_coords[0] && row == object_to_fixate_on_coords[1])
          #####
            encode_empty_square = false
          end

          # If the coordinates being processed are occupied by an "extra"
          # SceneObject, don't encode them as an empty Square.
          for extra_scene_object in extra_scene_objects
            if col == extra_scene_object[0] && row == extra_scene_object[1]
              encode_empty_square = false
            end
          end

          # Encode an empty square on the coordinates being processed, if 
          # applicable.
          if encode_empty_square
            scene._scene.get(col).set(row, SceneObject.new(Scene::EMPTY_SQUARE_TOKEN))
          end

        end
      end

      #######################################
      ##### CONSTRUCT PREVIOUS Fixation #####
      #######################################

      previous_fixation = HypothesisDiscriminationFixation.new(model, time)
      previous_fixation._performanceTime = previous_fixation._timeDecidedUpon + model._saccadeTime
      previous_fixation._performed = (scenario == 3 ? false : true)
      previous_fixation._scene = scene
      previous_fixation._colFixatedOn = 1
      previous_fixation._rowFixatedOn = 1

      if scenario == 4 
        previous_fixation._colFixatedOn = scene_minimum_domain_specific_column_field.value(scene) + scene_width_field.value(scene)
      elsif scenario == 5
        previous_fixation._rowFixatedOn = scene_minimum_domain_specific_row_field.value(scene) + scene_height_field.value(scene)
      elsif [10, 12, 15, 17, 20, 22].include?(scenario)
        previous_fixation._colFixatedOn = 2
        previous_fixation._rowFixatedOn = 2
      elsif [11, 16, 20, 21].include?(scenario)
        previous_fixation._colFixatedOn = 0
        previous_fixation._rowFixatedOn = 0
      end

      # Set the SceneObject fixated on by getting the contents of the 
      # coordinates fixated on. In scenarios 4 and 5, however, the column and 
      # row fixated on are not represented in the Scene and will cause an 
      # IndexOutOfBoundsException if used so code against this (means that in
      # scenarios 4 and 5, the previous fixation's "_objectSeen" variable will
      # be set to null which is OK since this variable is not used in these
      # scenarios.
      if ![4,5].include?(scenario)
        previous_fixation._objectSeen = scene._scene.get(previous_fixation._colFixatedOn).get(previous_fixation._rowFixatedOn)
      end

      # Advance time so that when the MovementFixation is constructed, the time 
      # it is decided upon will be later than the performance time of the 
      # previous fixation causing the timeline of the test to be correct.
      time = previous_fixation._performanceTime

      ##############################################
      ##### ADD PREVIOUS Fixation TO Perceiver #####
      ##############################################

      if scenario != 2
        perceiver_fixations = ArrayList.new()
        perceiver_fixations.add(previous_fixation)
        perceiver_fixations_field.value(chrest_perceiver_field.value(model)).put(previous_fixation._performanceTime, perceiver_fixations)
      end

      ######################################
      ##### CONSTRUCT MovementFixation #####
      ######################################

      fixation = MovementFixation.new(model, time)
      fixation._performanceTime = fixation._timeDecidedUpon + model._saccadeTime

      #########################################################
      ##### SET TIME TO INVOKE MovementFixation.make() AT #####
      #########################################################

      time_to_make_fixation_at = (scenario == 1 ? fixation._performanceTime - 1 : fixation._performanceTime)

      ###########################
      ##### INVOKE FUNCTION #####
      ###########################

      result = fixation.make(scene, time_to_make_fixation_at)

      ###############################
      ##### SET EXPECTED RESULT #####
      ###############################

      expected_result = nil
      if [9,14,19].include?(scenario)
        expected_result = [Square.new(1,0)]
      elsif [10,15,20].include?(scenario)
        expected_result = [Square.new(1,2), Square.new(2,1)]
      elsif [11,16,21].include?(scenario)
        expected_result = [Square.new(0,1), Square.new(1,0)]
      elsif [13,18,23].include?(scenario)
        expected_result = [Square.new(0,1), Square.new(1,0), Square.new(1,2), Square.new(2,1)]
      end

      #################
      ##### TESTS #####
      #################

      if result == nil
        assert_equal(result, expected_result, "occurred in scenario " + scenario.to_s)
      else
        assert_true(expected_result.include?(result), "occurred in scenario " + scenario.to_s)
      end
    end
  end
end