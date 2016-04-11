################################################################################
# Tests the constructor using 5 scenarios:
# 
# 1: identifier = null
# 2: object type = null
# 3: identifier = empty
# 4: object type = empty
# 5: identifier and object type not null and not empty
#
# The test checks whether an exception is thrown and both the identifier and 
# object type of the SceneObject created (if no exception should be thrown).
unit_test "constructor" do
  
  for scenario in 1..5
    
    ##################
    ##### SET-UP #####
    ##################
    
    identifier_field = SceneObject.java_class.declared_field("_identifier")
    type_field = SceneObject.java_class.declared_field("_objectType")
    identifier_field.accessible = true
    type_field.accessible = true
    
    identifier = "0"
    if (scenario == 1) then identifier = nil end
    if (scenario == 3) then identifier = "" end
    
    type = "A"
    if (scenario == 2) then type = nil end
    if (scenario == 4) then type = "" end
    
    ##############################
    ##### INVOKE CONSTRUCTOR #####
    ##############################
    
    exception_thrown = false
    scene_object = nil
    begin
      scene_object = SceneObject.new(identifier, type)
    rescue
      exception_thrown = true
    end
    
    #################
    ##### TESTS #####
    #################
    
    assert_equal(
      (scenario == 5 ? false : true),
      exception_thrown,
      "occurred when checking if an exception is thrown in scenario " + scenario.to_s
    )
    
    if scene_object != nil
      assert_equal(
        identifier,
        identifier_field.value(scene_object),
        "occurred when checking the identifier for the SceneObject in scenario " + scenario.to_s
      )

      assert_equal(
        type,
        type_field.value(scene_object),
        "occurred when checking the type for the SceneObject in scenario " + scenario.to_s
      )
    end
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