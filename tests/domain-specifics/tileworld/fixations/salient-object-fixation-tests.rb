unit_test "constructor" do
  Fixation.class_eval{
    field_accessor :_performanceTime, :_timeDecidedUpon
  }
  
  200.times do
    time_decided_upon = 150
    fixation = SalientObjectFixation.new(time_decided_upon)
    assert_equal(time_decided_upon, fixation._timeDecidedUpon)
  end
end

################################################################################
# Uses 3 scenarios to test the "make" function.  Each scenario is repeated 1000
# times to ensure consistency of behaviour.  The Scene fixated on is of a random
# size (width/height is between 1 and 5) and is semi-randomly populated with
# SceneObjects to ensure diversity of input and robust testing (the fact each 
# scenario is repeated 1000 times should buttress this).
#
# Scenario Descriptions
# =====================
#
# - Scenario 1
#   ~ Function invoked before Fixation performance time
#
# - Scenario 2
#   ~ Function invoked on Fixation performance time
#   ~ Scene is composed of SceneObjects that represent blind/empty squares or
#     the creator.
#
# - Scenario 3
#   ~ Function invoked on Fixation performance time
#   ~ Scene is randomly composed of SceneObjects that represent blind/empty 
#     Squares, the creator, holes, opponents or tiles (at least 1 
#     hole/opponent/tile will be encoded in the Scene).
#
# Expected Outcome
# ================
#
# - Scenarios 1 and 2 should always return null.
# - Scenario 3 should return a Fixation whose value can not be predicted but 
#   will be one of the Squares in the Scene containing a hole/opponent/tile.
unit_test "make" do
  Scene.class_eval{field_accessor :_scene}
  scene_width_field = Scene.java_class.declared_field("_width")
  scene_width_field.accessible = true
  scene_height_field = Scene.java_class.declared_field("_height")
  scene_height_field.accessible = true
  
  for scenario in 1..3
    1000.times do
      scene = Scene.new("", rand(1..5), rand(1..5), 0, 0, nil)

      potential_fixations = nil
      potential_scene_objects = [
        Scene::BLIND_SQUARE_TOKEN,
        Scene::EMPTY_SQUARE_TOKEN,
        Scene::CREATOR_TOKEN
      ]
      
      if scenario == 3
        potential_scene_objects.push(TileworldDomain::HOLE_SCENE_OBJECT_TYPE_TOKEN)
        potential_scene_objects.push(TileworldDomain::OPPONENT_SCENE_OBJECT_TYPE_TOKEN)
        potential_scene_objects.push(TileworldDomain::TILE_SCENE_OBJECT_TYPE_TOKEN)
      end

      for col in 0...scene_width_field.value(scene)
        for row in 0...scene_height_field.value(scene)
          scene_object_to_encode = potential_scene_objects.sample
          
          # In scenario 3, the test expects at least 1 Square to be proposed but
          # due to the random assignment of SceneObjects, it may be that the
          # Scene is entirely populated with SceneObjects that represent 
          # blind/empty Squares or the creator.  In this scenario, ensure that
          # Square with cooridnates (0, 0) contain a hole/opponent/tile.
          if scenario == 3 && col == 0 && row == 0 then scene_object_to_encode = potential_scene_objects[rand(3..5)] end
          
          scene._scene.get(col).set(row, SceneObject.new(scene_object_to_encode))
          if 
            scene_object_to_encode != Scene::BLIND_SQUARE_TOKEN &&
            scene_object_to_encode != Scene::EMPTY_SQUARE_TOKEN &&
            scene_object_to_encode != Scene::CREATOR_TOKEN
          #####
            if potential_fixations == nil then potential_fixations = [] end
            potential_fixations.push(Square.new(col, row))
          end
        end
      end

      fixation = SalientObjectFixation.new(150)
      fixation._performanceTime = fixation._timeDecidedUpon + 30
      
      result = fixation.make(scene, (scenario == 1 ? fixation._performanceTime - 1 : fixation._performanceTime))
      
      if scenario == 3
        assert_true(potential_fixations.include?(result))
      else
        assert_equal(nil, result, "occured in scenario " + scenario.to_s)
      end
    end
  end
end
