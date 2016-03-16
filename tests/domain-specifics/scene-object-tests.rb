################################################################################
# Tests the constructor using 6 scenarios:
# 
# 1: identifier = null
# 2: object type = null
# 3: object type = blind square token
# 4: object type = empty square token
# 5: object type = creator token
# 6: object type != blind/empty/creator token
#
# The test checks whether an exception is thrown and both the identifier and 
# object type of the SceneObject created (if no exception should be thrown).
unit_test "constructor" do
  
  for scenario in 1..6
    
    ##################
    ##### SET-UP #####
    ##################
    
    non_blind_or_empty_object_identifier = "0000"
    non_blind_empty_creator_object_type = "A"
    exception_thrown = false
    scene_object = nil
    
    ##############################
    ##### INVOKE CONSTRUCTOR #####
    ##############################
    
    begin
      scene_object = SceneObject.new(
        (scenario == 1 ? nil : scenario.between?(3,4) ? "overwrite" : non_blind_or_empty_object_identifier),
        (scenario == 2 ? 
          nil : 
          (scenario == 3 ? 
            Scene.getBlindSquareToken() : 
            (scenario == 4 ? 
              Scene.getEmptySquareToken() :
              (scenario == 5 ?
                Scene.getCreatorToken() :
                non_blind_empty_creator_object_type
              )
            )  
          )
        )
      )
    rescue
      exception_thrown = true
    end
    
    ################################################
    ##### GET scene_object IDENTIFIER AND TYPE #####
    ################################################
    
    # Set two variables to nil that will store the identifier and type of the
    # SceneObject created above.  If the constructor has thrown an error, the
    # SceneObject won't be created and so these variables will remain as nil.
    # Otherwise, get the value of the SceneObject created's "_identifier" and
    # "_objectType" directly (should not use getters and setters since these may 
    # change in future resulting in this test breaking).  Since these instance
    # variables are final, the class_eval construct can't be used so they need 
    # to be accessed "manually" by altering their "accessible" parameter.
    scene_object_identifier = nil
    scene_object_type = nil
    
    if scene_object != nil
      identifier_field = scene_object.java_class.declared_field("_identifier")
      type_field = scene_object.java_class.declared_field("_objectType")
      identifier_field.accessible = true
      type_field.accessible = true
    
      scene_object_identifier = identifier_field.value(scene_object)
      scene_object_type = type_field.value(scene_object)
    end
    
    ##################################
    ##### SET EXPECTED VARIABLES #####
    ##################################
    
    # Exception should be thrown in scenarios 1 and 2.
    expected_exception_thrown = (scenario.between?(1,2) ? true : false)
    
    # Scene object identifier in scenarios 1 and 2 should be nil since no 
    # SceneObject should be created.  Should be the blind square token in 
    # scenario 3, should be the empty square token in scenario 4 and should be
    # the identifier specified in all other scenarios.
    expected_scene_object_identifier = 
      (scenario.between?(1,2) ? 
        nil :
        (scenario == 3 ? 
          Scene.getBlindSquareToken() :
          (scenario == 4 ?
            Scene.getEmptySquareToken() :
            non_blind_or_empty_object_identifier
          )
        )
      )
    
    # Scene object type in scenarios 1 and 2 should be nil since no SceneObject 
    # should be created.  Should be the blind square token in scenario 3, should 
    # be the empty square token in scenario 4, should be the creator token in 
    # scenario 5 and should be the identifier specified in scenario 6.
    expected_scene_object_type = 
      (scenario.between?(1,2) ? 
        nil :
        (scenario == 3 ? 
          Scene.getBlindSquareToken() :
          (scenario == 4 ?
            Scene.getEmptySquareToken() :
            (scenario == 5 ?
              Scene.getCreatorToken() :
              non_blind_empty_creator_object_type
            )
          )
        )
      )

    #################
    ##### TESTS #####
    #################
    
    assert_equal(
      expected_exception_thrown,
      exception_thrown,
      "occurred when checking if an exception is thrown in scenario " + scenario.to_s
    )
    
    assert_equal(
      expected_scene_object_identifier,
      scene_object_identifier,
      "occurred when checking the identifier for the SceneObject in scenario " + scenario.to_s
    )
    
    assert_equal(
      expected_scene_object_type,
      scene_object_type,
      "occurred when checking the type for the SceneObject in scenario " + scenario.to_s
    )
  end
end

################################################################################
unit_test "get_identifier" do
  identifier = "0000"
  scene_object = SceneObject.new(identifier, "A")
  assert_equal(identifier, scene_object.getIdentifier())
end

################################################################################
unit_test "get_object_type" do
  type = "A"
  scene_object = SceneObject.new("0000", type)
  assert_equal(type, scene_object.getObjectType())
end