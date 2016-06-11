################################################################################
# Tests that the visual-spatial field constructor operates as expected using two
# scenarios:
#
# Scenario 1: The CHREST model to be associated with the VisualSpatialField is
#             set to null
# Scenario 2: The CHREST model to be associated with the VisualSpatialField is
#             not set to null, creator details are specified however, the 
#             location of the creator is not represented in the 
#             VisualSpatialField under construction.
# Scenario 3: The CHREST model to be associated with the VisualSpatialField is
#             not set to null, creator details are specified and the location of
#             the creator is represented in the VisualSpatialField under 
#             construction.
# Scenario 4: The CHREST model to be associated with the VisualSpatialField is
#             not set to null but no creator details are specified.
#
# In scenarios 1 and 2 the constructor should throw an exception.  In scenario 3 
# the VisualSpatialField constructed should not throw an exception and the 
# visual-spatial field itself should have one VisualSpatialFieldObject encoded 
# upon it: the creator.  In scenario 4, the VisualSpatialField constructed 
# should not throw an exception but the visual-spatial field itself should have 
# no VisualSpatialFieldObjects encoded upon it.  
unit_test "constructor" do
  
  for scenario in 1..4
    
    # Create CHREST model since a new VisualSpatialField 
    time = 0
    model = Chrest.new(time, false)

    vsf_name = "test"
    vsf_width = 5
    vsf_height = 4
    vsf_min_ds_col = 3
    vsf_min_ds_row = 2
    vsf_creation_time = time + 100
    vsf_associated_model = (scenario == 1 ? nil : model)

    creator_details = nil
    if scenario != 4 
      creator_details = ArrayList.new()
      creator_details.add("00")
      creator_details.add(
        (scenario == 2 ?
          Square.new(vsf_width, vsf_height) :
          Square.new(2, 2)
        )
      )
    end

    exception_thrown = false
    begin
    visual_spatial_field = VisualSpatialField.new(vsf_name, vsf_width, vsf_height, 
      vsf_min_ds_col, vsf_min_ds_row, vsf_associated_model, creator_details, vsf_creation_time)
    rescue
      exception_thrown = true
    end
    
    expected_exception_thrown = ([1, 2].include?(scenario) ? true : false)
    assert_equal(expected_exception_thrown, exception_thrown)

    if ![1, 2].include?(scenario)
      # Check instance variables set correctly
      name_field = VisualSpatialField.java_class.declared_field("_name")
      creation_time_field = VisualSpatialField.java_class.declared_field("_creationTime")
      width_field = VisualSpatialField.java_class.declared_field("_width")
      height_field = VisualSpatialField.java_class.declared_field("_height")
      min_ds_col_field = VisualSpatialField.java_class.declared_field("_minDomainSpecificCol")
      min_ds_row_field = VisualSpatialField.java_class.declared_field("_minDomainSpecificRow")
      associated_model_field = VisualSpatialField.java_class.declared_field("_associatedModel")
      name_field.accessible = true
      creation_time_field.accessible = true
      width_field.accessible = true
      height_field.accessible = true
      min_ds_col_field.accessible = true
      min_ds_row_field.accessible = true
      associated_model_field.accessible = true

      assert_equal(vsf_name, name_field.value(visual_spatial_field), "occurred when checking the name of the VisualSpatialField constructed")
      assert_equal(vsf_creation_time, creation_time_field.value(visual_spatial_field), "occurred when checking the creation time of the VisualSpatialField constructed")
      assert_equal(vsf_width, width_field.value(visual_spatial_field), "occurred when checking the width of the VisualSpatialField constructed")
      assert_equal(vsf_height, height_field.value(visual_spatial_field), "occurred when checking the height of the VisualSpatialField constructed")
      assert_equal(vsf_min_ds_col, min_ds_col_field.value(visual_spatial_field), "occurred when checking the minimum domain-specific column of the VisualSpatialField constructed")
      assert_equal(vsf_min_ds_row, min_ds_row_field.value(visual_spatial_field), "occurred when checking mininimum domain-specific row of the VisualSpatialField constructed")
      assert_equal(model, associated_model_field.value(visual_spatial_field), "occurred when checking the CHREST model associated with the VisualSpatialField constructed")

      # Check contents set correctly
      expected_visual_spatial_field_data = 
        Array.new(vsf_width){
          Array.new(vsf_height) {
            Array.new
          }  
        }

      if scenario == 3
        expected_visual_spatial_field_data[creator_details.get(1).getColumn()][creator_details.get(1).getRow()] = [[
          creator_details.get(0),
          Scene.getCreatorToken(),
          false,
          vsf_creation_time,
          nil
        ]]
      end
    
      check_visual_spatial_field_against_expected(visual_spatial_field, expected_visual_spatial_field_data, vsf_creation_time + 1000, "")
  
    end
  end
end

################################################################################
unit_test "simple_getters" do
  name = "test"
  height = 2
  width = 3
  min_domain_specific_col = 4
  min_domain_specific_row = 5
  model = Chrest.new(0, true)
  
  visual_spatial_field = VisualSpatialField.new(name, width, height, min_domain_specific_col, min_domain_specific_row, model, nil, 0)
  
  error_msg = "occurred when checking the "
  assert_equal(name, visual_spatial_field.getName(), error_msg + "name getter")
  assert_equal(height, visual_spatial_field.getHeight(), error_msg + "height getter")
  assert_equal(width, visual_spatial_field.getWidth(), error_msg + "width getter")
  assert_equal(min_domain_specific_col, visual_spatial_field.getMinimumDomainSpecificCol(), error_msg + "minimum domain column getter")
  assert_equal(min_domain_specific_row, visual_spatial_field.getMinimumDomainSpecificRow(), error_msg + "minimum domain row getter")
  assert_equal(model, visual_spatial_field.getAssociatedModel(), error_msg + "model getter")
end

################################################################################
# Tests "addObjectToCoordinates()" using 24 scenarios that encompass all 
# possible combinations of additions:
# 
# Scenario Descriptions
# =====================
# 
# The first three scenarios should result in the function throwing an exception:
# 
# 1. Add a VisualSpatialFieldObject that represents a blind square.
# 2. Add a VisualSpatialFieldObject that doesn't represent a blind square to 
#    coordinates that are not represented in a VisualSpatialField.
# 3. Add a VisualSpatialFieldObject that already exists on a VisualSpatialField.
# 
# For all but one of the remaining scenarios, no exception should be thrown. In
# these scenarios, a particular type of VisualSpatialFieldObject, v, is added to 
# a square containing a particular type of VisualSpatialFieldObject, w, at a 
# particular time, t, which is relative to the terminus of w.
# 
# v is set to the following in the scenarios indicated:
# 
# 4-10. A VisualSpatialFieldObject representing the agent equipped with CHREST 
# 11-17. A VisualSpatialFieldObject that represents an empty square
# 18-24. A VisualSpatialFieldObject that is not the agent equipped with CHREST 
#        or an empty square
# 
# w and t are set to the following in the scenarios indicated:
# 
#  4/11/18. w = None (coordinates do not contain any VisualSpatialFieldObjects)
#           t = N/A
#  5/12/19. w = A VisualSpatialFieldObject representing an empty square
#           t = Before w's terminus (the coordinates are an empty square)
#  6/13/20. w = A VisualSpatialFieldObject representing an empty square
#           t = After w's terminus (the coordinates were an empty square but now
#               have an unknown VisualSpatialFieldObject status) 
#  7/14/21. w = Two VisualSpatialFieldObjects representing non-empty squares 
#               and not the agent equipped with CHREST (two 
#               VisualSpatialFieldObjects are used to test whether *all* such 
#               VisualSpatialFieldObjects have their terminus set correctly when
#               a VisualSpatialFieldObject representing an empty square is added
#               to coordinates they occupy).
#           t = Before w's terminus
#  8/15/22. w = Two VisualSpatialFieldObjects representing non-empty squares 
#               and not the agent equipped with CHREST (two 
#               VisualSpatialFieldObjects are used for the same reasons as in
#               scenarios 7/14/21).
#           t = After w's terminus
#  9/16/23. t = A VisualSpatialFieldObject representing the agent equipped with 
#               CHREST
#           w = Before t's terminus
#  10/17/24. t = A VisualSpatialFieldObject representing the agent equipped with 
#               CHREST
#            w = After t's terminus
#
# NOTE: scenario 9 should throw an exception since w represents an agent 
#       equipped with CHREST and is alive when v, which also represents the same 
#       agent as w, is added.  All exception-throwing conditions should now have
#       been triggered.
unit_test "add_object_to_coordinates" do
  
  #######################################################
  ##### SET-UP ACCESS TO PRIVATE INSTANCE VARIABLES #####
  #######################################################
  Chrest.class_eval{
    field_accessor :_recognisedVisualSpatialFieldObjectLifespan,
      :_unrecognisedVisualSpatialFieldObjectLifespan
  }
  
  visual_spatial_field_field = VisualSpatialField.java_class.declared_field("_visualSpatialField")
  visual_spatial_field_field.accessible = true
  
  VisualSpatialFieldObject.class_eval{
    field_accessor :_timeCreated, :_terminus
  }
  
  identifier_field = SceneObject.java_class.declared_field("_identifier")
  type_field = SceneObject.java_class.declared_field("_objectType")
  identifier_field.accessible = true
  type_field.accessible = true
  
  #########################
  ##### SCENARIO LOOP #####
  #########################
  for scenario in 1..24
    time = 0
    model = Chrest.new(time, false)
    
    #######################################
    ##### SET-UP VISUAL-SPATIAL FIELD #####
    #######################################
    
    # Set dimensions
    vsf_width = 2
    vsf_height = 2
    visual_spatial_field_creation_time = time
    
    # Add creator details, if the scenario deems it necessary.
    creator_details = nil
    if [9, 10, 16, 17, 23, 24].include?(scenario)
      creator_details = ArrayList.new()
      creator_details.add("00")
      creator_details.add(Square.new(1, 0))
    end
    
    # Construct the visual-spatial field
    visual_spatial_field = VisualSpatialField.new("", vsf_width, vsf_height, 0, 0, model, creator_details, visual_spatial_field_creation_time)
    
    ###########################################################################
    ##### ADD EXISTING OBJECTS TO SQUARE THAT NEW OBJECT WILL BE ADDED TO #####
    ###########################################################################
    
    existing_object_identifiers_and_types = []
    if scenario == 3 then existing_object_identifiers_and_types.push(["45", "AA"]) end
    if [5, 6, 12, 13, 19, 20].include?(scenario) then existing_object_identifiers_and_types.push(["34", Scene.getEmptySquareToken()]) end
    if [7, 8, 14, 15, 21, 22].include?(scenario) then existing_object_identifiers_and_types = [["34", "P"], ["35", "Q"]] end
    
    for existing_object_identifier_and_type in existing_object_identifiers_and_types
      existing_object = VisualSpatialFieldObject.new(
        existing_object_identifier_and_type[0],
        existing_object_identifier_and_type[1],
        model,
        visual_spatial_field,
        time + 100,
        true,
        true
      )
      existing_object._terminus = time + 10000
      
      coordinate_contents_history = visual_spatial_field_field.value(visual_spatial_field).get(1).get(0)
      current_coordinate_contents = coordinate_contents_history.lastEntry().getValue
      new_coordinate_contents = ArrayList.new()
      new_coordinate_contents.addAll(current_coordinate_contents)
      new_coordinate_contents.add(existing_object)
      coordinate_contents_history.put(existing_object._timeCreated.to_java(:int), new_coordinate_contents)
    end
    
    # Add the agent equipped with CHREST to the 
    # "existing_object_identifiers_and_types" variable after its contents have 
    # been added above. Doing this before the addition would mean that the agent
    # equipped with CHREST will be added twice since it is added when the
    # VisualSpatialField is created.
    if [9, 10, 16, 17, 23, 24].include?(scenario) then existing_object_identifiers_and_types.push(["00", Scene.getCreatorToken()]) end
    
    ################################
    ##### CREATE OBJECT TO ADD #####
    ################################
    
    object_to_add_identifier_and_type = ["100", "Z"]
    if scenario == 3 then object_to_add_identifier_and_type = [existing_object_identifier_and_type[0], "D"] end #Same object (id same but diff. class, tests that id only checked)
    if scenario.between?(4, 10) then object_to_add_identifier_and_type = ["100", Scene.getCreatorToken()] end
    if scenario.between?(11, 17) then object_to_add_identifier_and_type = ["100", Scene.getEmptySquareToken()] end
    
    object_to_add = VisualSpatialFieldObject.new( 
      object_to_add_identifier_and_type[0], 
      object_to_add_identifier_and_type[1],
      model,
      visual_spatial_field, 
      time + 9000, 
      ([9, 10, 16, 17, 23, 24].include?(scenario) ? false : true),
      true
    )
    
    # Set-up the blind square here since the VisualSpatialFieldObject 
    # constructor will throw an exception if a blind square token is passed as 
    # the VisualSpatialFieldObject type.
    if scenario == 1
      identifier_field.set_value(object_to_add, "98")
      type_field.set_value(object_to_add, Scene.getBlindSquareToken())
    end
    
    ############################################
    ##### SET COORDINATES TO ADD OBJECT TO #####
    ############################################
    
    coordinates_to_add_object_to = 
      (scenario == 2 ? 
        [vsf_width, vsf_height] : 
        [1, 0]
      )
      
    #####################################
    ##### SET TIME TO ADD OBJECT AT #####
    #####################################
      
    # Before setting the time to add, special considerations need to be made for
    # the existing object if its the agent equipped with CHREST
    if [9, 10, 16, 17, 23, 24].include?(scenario)
      creator = visual_spatial_field_field.value(visual_spatial_field).get(1).get(0).lastEntry().getValue().get(0)
      if [10, 17, 24].include?(scenario) then creator._terminus = time + 9000 end
      existing_object = creator
    end
      
    time_to_add_object_at = object_to_add._timeCreated + 5 # By default, just after the object to add has been created (existing object won't have decayed if present)
    if [6, 8, 10, 13, 15, 17, 20, 22, 24].include?(scenario) then time_to_add_object_at = (existing_object._terminus + 100) end # Otherwise, after the existing object has decayed.
      
    ###########################
    ##### INVOKE FUNCTION #####
    ###########################
    
    exception_thrown = false
    result = false
    begin
      result = visual_spatial_field.addObjectToCoordinates(
        coordinates_to_add_object_to[0],
        coordinates_to_add_object_to[1],
        object_to_add,
        time_to_add_object_at
      )
    rescue
      exception_thrown = true
    end
    
    ##################################
    ##### SET EXPECTED VARIABLES #####
    ##################################
    expected_exception_thrown = (scenario.between?(1, 3) || scenario == 9 ? true : false)
    expected_result = (scenario.between?(1, 3) || scenario == 9 ? false : true)
    
    assert_equal(expected_exception_thrown, exception_thrown, "checking exception in scenario " + scenario.to_s)
    assert_equal(expected_result, result, "checking result in scenario " + scenario.to_s)
    
    if scenario > 4 && scenario != 9
      
      # Create and populate the data structure containing the expected state of 
      # the visual-spatial field after adding the object.
      expected_visual_spatial_field_data = Array.new(2){ Array.new(2) { Array.new } }
      expected_visual_spatial_field_data[coordinates_to_add_object_to[0]][coordinates_to_add_object_to[1]] = []
      
      object_to_add_data = [
        identifier_field.value(object_to_add),
        type_field.value(object_to_add),
        [10, 16, 17, 23, 24].include?(scenario) ? false : true,
        object_to_add._timeCreated,
        object_to_add._terminus
      ]

      # Existing objects should always be present if scenario is not 4, 11 or 
      # 18 (these scenarios have no pre-existing objects on the coordinates the
      # new object is added to).
      existing_object_data = []
      if ![4, 11, 18].include?(scenario)
        for existing_object_identifier_and_type in existing_object_identifiers_and_types
          existing_object_data.push([
            existing_object_identifier_and_type[0],
            existing_object_identifier_and_type[1],
            ([9, 10, 16, 17, 23, 24].include?(scenario) ? false : true),
            existing_object._timeCreated,
            existing_object._terminus
          ])
        end
      end
      
      # The terminus of the VisualSpatialFieldObject that exists on the 
      # coordinates the new VisualSpatialFieldObject is added to should only be 
      # set to the time when the new VisualSpatialFieldObject is added, i.e. the 
      # existing VisualSpatialFieldObject on the coordinates the new 
      # VisualSpatialFieldObject is added to should only be overwritten when:
      # 
      # - A VisualSpatialFieldObject representing the creator is being added to
      #   coordinates that contain a VisualSpatialFieldObject represention of an 
      #   empty square whose terminus has not been reached when the addition 
      #   occurs.
      # - A VisualSpatialFieldObject representing an empty square is being added 
      #   to coordinates that contain a VisualSpatialFieldObject represention of 
      #   an empty square whose terminus has not been reached when the addition 
      #   occurs.
      # - A VisualSpatialFieldObject representing an empty square is being added 
      #   to coordinates that contain a VisualSpatialFieldObject represention of 
      #   a non empty square whose terminus has not been reached when the addition 
      #   occurs. 
      # - A VisualSpatialFieldObject representing an empty square is being added 
      #   to coordinates that contain a VisualSpatialFieldObject represention of 
      #   the agent equipped with CHREST whose terminus has not been reached 
      #   when the addition occurs.
      # - A VisualSpatialFieldObject representing a non-empty square and not the
      #   agent equipped with CHREST is being added to coordinates that contain 
      #   a VisualSpatialFieldObject represention of an empty square whose 
      #   terminus has not been reached when the addition occurs. 
      if [5, 12, 14, 16, 19].include?(scenario) 
        for object_data in existing_object_data
          object_data[4] = time_to_add_object_at
        end
      end
      
      for object_data in existing_object_data
          expected_visual_spatial_field_data[coordinates_to_add_object_to[0]][coordinates_to_add_object_to[1]].push(object_data)
        end
      
      expected_visual_spatial_field_data[coordinates_to_add_object_to[0]][coordinates_to_add_object_to[1]].push(object_to_add_data)
      
      check_visual_spatial_field_against_expected(
        visual_spatial_field, 
        expected_visual_spatial_field_data, 
        time_to_add_object_at + 1,
        "in scenario " + scenario.to_s
      )
    end
  end
end

################################################################################
# Tests the "getAsScene()" function using 4 scenarios:
# 
# 1. An unknown probabilities TreeMap value is the unknown 
#    VisualSpatialFieldObject token.
# 2. The unknown probabilities TreeMap does not contain a 1.0 key.
# 3. The unknown probabilities TreeMap contains a key greater than 1.0.
# 4. The unknown probabilities TreeMap is constructed correctly.
# 
# In all scenarios except scenario 4, an exception should be thrown due to the
# incorrect formatting of the unknown probabilities TreeMap.
# 
# The VisualSpatialField used has the following properties:
# 
# - Dimensions are 3 * 3. 
# - Coordinates (0, 0) will contain a VisualSpatialFieldObject representing the 
#   agent equipped with CHREST, i.e. the creator of the VisualSpatialField.
# - Coordinates (1, 0) will contain two VisualSpatialFieldObjects representing
#   an empty square however, the terminus for the first will be reached before
#   the second is created (in accordance with standard VisualSpatialField
#   restrictions, i.e. no more than one empty square may be alive on the same
#   VisualSpatialField coordinates at any time).
# - Coordinates (2, 0) will contain 4 VisualSpatialFieldObjects representing 
#   non-empty squares and SceneObjects that are not the creator.  All will be
#   created at the same time but the second and fourth VisualSpatialFieldObjects
#   will have a greater terminus than the first and third 
#   VisualSpatialFieldObjects.
# - All other coordinates have no VisualSpatialFieldObjects placed upon them.
# 
# The "getAsScene()" function will be invoked when the second 
# VisualSpatialFieldObject on (1, 0) is alive (not the first) and the second and 
# fourth VisualSpatialFieldObjects are alive on (2, 0).  Therefore, the 
# VisualSpatialField will look like the following:
# 
# Notation Used
# =============
# 
# - VisualSpatialFieldObject representing the creator is denoted by "SELF".
# - VisualSpatialFieldObjects representing empty squares are denoted by ".".
# - VisualSpatialFieldObjects representing non-empty squares and not the creator
#   are denoted by their object type.
# - If multiple VisualSpatialFieldObjects occupy the same coordinates, they are
#   seperated by a comma.
#   
# VisualSpatialField State when Function Invoked
# ==============================================
# 
#    |-----|-----|-----|
# 2  |     |     |     |
#    |-----|-----|-----|
# 1  |     |     |     |
#    |-----|-----|-----|
# 0  |SELF |  .  | B,D |
#    |-----|-----|-----|
#       0     1     2     COORDINATES
# 
# Since the unknown probabilities TreeMap is constructed correctly in scenario 
# 4, the Scene generated should look like the following given that 
# VisualSpatialField coordinates devoid of VisualSpatialFieldObjects at the time 
# the function is invoked hve a 50% chance of having either a SceneObject 
# representing an empty or blind square encoded on them:
# 
# Notation Used
# =============
# 
# - SceneObject representing the creator is denoted by "SELF".
# - SceneObjects representing blind squares are denoted by "*".
# - SceneObjects representing empty squares are denoted by ".".
# - SceneObjects representing non-empty squares and not the creator are denoted 
#   by their object type.
# - If a coordinate potentially has more than one type of SceneObject encoded 
#   upon it, the potential types of SceneObject that may be found upon it are 
#   seperated by a forward slash.
# 
# |-----|-----|-----|
# | ./* | ./* | ./* |
# |-----|-----|-----|
# | ./* | ./* | ./* |
# |-----|-----|-----|
# |SELF |  .  |  D  |
# |-----|-----|-----|
# 
# Since this test intends to check that VisualSpatialFieldObjects are translated 
# exactly into SceneObjects, the identifiers and types for 
# VisualSpatialFieldObjects on (0, 0), (1, 0) and (2, 0) are explicitly set.
unit_test "get_as_scene" do
  
  #######################################################
  ##### SET-UP ACCESS TO PRIVATE INSTANCE VARIABLES #####
  #######################################################
  
  visual_spatial_field_field = VisualSpatialField.java_class.declared_field("_visualSpatialField")
  visual_spatial_field_field.accessible = true
  
  identifier_field = SceneObject.java_class.declared_field("_identifier")
  type_field = SceneObject.java_class.declared_field("_objectType")
  identifier_field.accessible = true
  type_field.accessible = true
  
  VisualSpatialFieldObject.class_eval{
    field_accessor :_timeCreated, :_terminus
  }
  
  scene_scene_field = Scene.java_class.declared_field("_scene")
  scene_height_field = Scene.java_class.declared_field("_height")
  scene_width_field = Scene.java_class.declared_field("_width")
  scene_visual_spatial_field_represented_field = Scene.java_class.declared_field("_visualSpatialFieldRepresented")
  scene_scene_field.accessible = true
  scene_height_field.accessible = true
  scene_width_field.accessible = true
  scene_visual_spatial_field_represented_field.accessible = true
  
  #########################
  ##### SCENARIO LOOP #####
  #########################
  
  for scenario in 1..4
    
    #############################################################
    ##### INSTANTIATE TIME, CHREST AND VISUAL-SPATIAL FIELD #####
    #############################################################
    
    # Instantiate time and create CHREST model
    time = 0
    model = Chrest.new(time, true)

    # Add creator
    creator_details = ArrayList.new()
    creator_details.add("00")
    creator_details.add(Square.new(0, 0))
    
    # Create VisualSpatialField
    visual_spatial_field = VisualSpatialField.new("", 3, 3, 0, 0, model, creator_details, time)

    ###############################################################
    ##### ADD VisualSpatialFieldObjects TO VisualSpatialField #####
    ###############################################################
    
    # NOTE: the creator has already been added in the section immediately 
    # proceeding this one.
    
    # Get the actual VisualSpatialField data structure so that data can be set
    # explicitly.
    vsf = visual_spatial_field_field.value(visual_spatial_field)

    # Add empty squares
    empty_square_1 = VisualSpatialFieldObject.new("emp_1", Scene.getEmptySquareToken(), model, visual_spatial_field, time + 10, false, false)
    empty_square_2 = VisualSpatialFieldObject.new("emp_2", Scene.getEmptySquareToken(), model, visual_spatial_field, empty_square_1._terminus.to_java(:int), false, false)
    empty_square_1._terminus = time + 20
    empty_square_2._terminus = time + 200
    
    vsf.get(1).get(0).lastEntry().getValue().add(empty_square_1)
    vsf.get(1).get(0).lastEntry().getValue().add(empty_square_2)

    # Add non-empty squares
    non_empty_square_1 = VisualSpatialFieldObject.new("non_emp_1", "A", model, visual_spatial_field, time + 10, false, false)
    non_empty_square_2 = VisualSpatialFieldObject.new("non_emp_2", "B", model, visual_spatial_field, time + 10, false, false)
    non_empty_square_3 = VisualSpatialFieldObject.new("non_emp_3", "C", model, visual_spatial_field, time + 10, false, false)
    non_empty_square_4 = VisualSpatialFieldObject.new("non_emp_4", "D", model, visual_spatial_field, time + 20, false, false)
    non_empty_square_1._terminus = time + 20
    non_empty_square_2._terminus = time + 200
    non_empty_square_3._terminus = time + 20
    non_empty_square_4._terminus = time + 200
    
    vsf.get(2).get(0).lastEntry().getValue().add(non_empty_square_1)
    vsf.get(2).get(0).lastEntry().getValue().add(non_empty_square_2)
    vsf.get(2).get(0).lastEntry().getValue().add(non_empty_square_3)
    vsf.get(2).get(0).lastEntry().getValue().add(non_empty_square_4)

    #######################################################
    ##### SET-UP UNKNOWN PROBABILITIES DATA STRUCTURE #####
    #######################################################
    
    unknown_probabilities = TreeMap.new()
    if scenario == 1
      unknown_probabilities.put(1.0, VisualSpatialFieldObject.getUnknownSquareToken())
    elsif scenario == 2
      unknown_probabilities.put(0.5, Scene.getEmptySquareToken())
    elsif scenario == 3
      unknown_probabilities.put(0.5, Scene.getEmptySquareToken())
      unknown_probabilities.put(1.0, Scene.getBlindSquareToken())
      unknown_probabilities.put(1.5, "Z")
    else 
      unknown_probabilities.put(0.5, Scene.getEmptySquareToken())
      unknown_probabilities.put(1.0, Scene.getBlindSquareToken())
    end
    
    ##################################
    ##### SET EXPECTED VARIABLES #####
    ##################################
    
    expected_exception_thrown = (scenario == 4 ? false : true)
    expected_result = []
    
    ################
    ##### TEST #####
    ################
    
    exception_thrown = false
    result = nil
    begin
      result = visual_spatial_field.getAsScene(time + 150, unknown_probabilities)
    rescue 
      exception_thrown = true
    end
    
    assert_equal(
      expected_exception_thrown,
      exception_thrown,
      "occurred when checking if an exception is thrown in scenario " + scenario.to_s
    )
    
    # This code block should only be entered in context of scenario 4.
    if result != nil
      for col in 0...scene_width_field.value(result)
        for row in 0...scene_height_field.value(result)
          object = scene_scene_field.value(result).get(col).get(row)

          # Since there is a 50% chance SceneObjects on rows 1 and 2 may have 
          # either blind or empty square object types, the variables that store 
          # expected identifiers and types need to be able to store multiple 
          # values so, make them arrays.
          expected_identifier = nil
          expected_types = []

          if col == 0 && row == 0 
            expected_identifier = "00" 
            expected_types.push(Scene.getCreatorToken())
          elsif col == 1 && row == 0 
            expected_identifier = "emp_2"
            expected_types.push(Scene.getEmptySquareToken())
          elsif col == 2 && row == 0 
            expected_identifier = "non_emp_4"
            expected_types.push("D")
          else
            expected_types.push(Scene.getEmptySquareToken())
            expected_types.push(Scene.getBlindSquareToken())
          end

          # In some cases, the identifier can not be predicted so skip checking it
          if(expected_identifier != nil)
            assert_equal(
              expected_identifier, 
              identifier_field.value(object), 
              "occurred when checking the identifier of the item on col " + 
                col.to_s + " and row " + row.to_s
            )
          end

          assert_true(
            expected_types.include?(type_field.value(object)), 
            "occurred when checking the type of the item on col " + col.to_s + 
            " and row " + row.to_s
          )
        end
      end
      
      assert_equal(
        visual_spatial_field,
        scene_visual_spatial_field_represented_field.value(result), 
        "occurred when checking if the VisualSpatialField represented by the " +
        "Scene generated is the VisualSpatialField used in the test"
      )
    end
  end
end

################################################################################
# This test uses a VisualSpatialField composed of one square and 6 scenarios to
# check the functionality of "getCoordinateContents".
# 
# Scenario Descriptions
# =====================
# 
# 1. Coordinates specified are not represented in the VisualSpatialField.
# 2. Coordinates specified contain the agent equipped with CHREST (the creator).
# 3. Coordinates specified contains two VisualSpatialFieldObjects representing
#    an empty square but the first has decayed when the function is invoked.
# 4. Coordinates specified contain four VisualSpatialFieldObjects representing
#    non-empty squares and non-creators but the first and third 
#    VisualSpatialFieldObjects have decayed when the function is invoked.
# 5. Coordinates specified have no VisualSpatialFieldObjects on them when the
#    function is invoked and the second parameter to the function is set to 
#    false.
# 6. Coordinates specified have no VisualSpatialFieldObjects on them when the
#    function is invoked and the second parameter to the function is set to 
#    true.
#
# When checking the output of the function, the returned value is converted to
# an array and each VisualSpatialFieldObject is replaced by its identifier, this
# allows for accurate checking of the function since each 
# VisualSpatialFieldObject is unique in context of this test. 
#
# Expected Output
# ===============
# 1. Null.
# 2. An array containing the identifier for the creator.
# 3. An array containing the identifier for the second empty square.
# 4. An array containing the identifiers for the second and fourth 
#    VisualSpatialFieldObjects.
# 5. Empty array.
# 6. An array containing the result of 
#    VisualSpatialFieldObject.getUnknownSquareToken.
#
# Finally, the test will add a new VisualSpatialFieldObject to the ArrayList 
# returned (if applicable) and will invoke the function again to see if the
# new VisualSpatialFieldObject has been added to the coordinate.  In all 
# scenarios, this should evaluate to false since it should only be possible to
# add VisualSpatialFieldObjects to a VisualSpatialField using the add function 
# defined by the VisualSpatialField class and VisualSpatialFieldObjects should
# never be able to be removed.
unit_test "get_coordinate_contents" do
  
  #######################################################
  ##### SET-UP ACCESS TO PRIVATE INSTANCE VARIABLES #####
  #######################################################
  
  visual_spatial_field_field = VisualSpatialField.java_class.declared_field("_visualSpatialField")
  visual_spatial_field_height_field = VisualSpatialField.java_class.declared_field("_height")
  visual_spatial_field_width_field = VisualSpatialField.java_class.declared_field("_width")
  visual_spatial_field_field.accessible = true
  visual_spatial_field_height_field.accessible = true
  visual_spatial_field_width_field.accessible = true
  
  identifier_field = SceneObject.java_class.declared_field("_identifier")
  type_field = SceneObject.java_class.declared_field("_objectType")
  identifier_field.accessible = true
  type_field.accessible = true
  
  VisualSpatialFieldObject.class_eval{
    field_accessor :_timeCreated, :_terminus
  }
  
  #########################
  ##### SCENARIO LOOP #####
  #########################
  
  for scenario in 1..6
  
    #############################################################
    ##### INSTANTIATE TIME, CHREST AND VISUAL-SPATIAL FIELD #####
    #############################################################
    
    # Instantiate time and CHREST
    time = 0
    model = Chrest.new(time, true)

    # Add creator
    creator_details = nil
    if scenario == 2
      creator_details = ArrayList.new()
      creator_details.add("00")
      creator_details.add(Square.new(0, 0))
    end
    
    # Create VisualSpatialField
    visual_spatial_field = VisualSpatialField.new("", 1, 1, 0, 0, model, creator_details, time)
    
    ###############################################################
    ##### ADD VisualSpatialFieldObjects TO VisualSpatialField #####
    ###############################################################
    
    vsf = visual_spatial_field_field.value(visual_spatial_field)
    if scenario == 3  
      empty_square_1 = VisualSpatialFieldObject.new("emp_1", Scene.getEmptySquareToken(), model, visual_spatial_field, time + 10, false, false)
      empty_square_2 = VisualSpatialFieldObject.new("emp_2", Scene.getEmptySquareToken(), model, visual_spatial_field, empty_square_1._terminus.to_java(:int), false, false)
      empty_square_1._terminus = time + 100
      empty_square_2._terminus = time + 200
      
      vsf.get(0).get(0).lastEntry().getValue().add(empty_square_1)
      vsf.get(0).get(0).lastEntry().getValue().add(empty_square_2)
    elsif scenario == 4
      obj_1 = VisualSpatialFieldObject.new("obj_1", "A", model, visual_spatial_field, time, false, false)
      obj_2 = VisualSpatialFieldObject.new("obj_2", "B", model, visual_spatial_field, time, false, false)
      obj_3 = VisualSpatialFieldObject.new("obj_3", "C", model, visual_spatial_field, time, true, false)
      obj_4 = VisualSpatialFieldObject.new("obj_4", "D", model, visual_spatial_field, time, true, false)
      obj_1._terminus = time + 100
      obj_2._terminus = time + 200
      obj_3._terminus = time + 100
      obj_4._terminus = time + 200
      
      vsf.get(0).get(0).lastEntry().getValue().add(obj_1)
      vsf.get(0).get(0).lastEntry().getValue().add(obj_2)
      vsf.get(0).get(0).lastEntry().getValue().add(obj_3)
      vsf.get(0).get(0).lastEntry().getValue().add(obj_4)
    end
    
    ##############################################
    ##### SET COORDINATES TO GET CONTENTS OF #####
    ##############################################
    
    coordinates = (scenario == 1 ? 
      [visual_spatial_field_width_field.value(visual_spatial_field), visual_spatial_field_height_field.value(visual_spatial_field)] :
      [0, 0]
    )
    
    ############################################
    ##### SET SECOND PARAMETER TO FUNCTION #####
    ############################################
    
    return_unknown_square_if_empty = (scenario == 5 ? false : true)
    
    ########################################
    ##### SET EXPECTED RESULT VARIABLE #####
    ########################################
    
    expected_result = nil
    if scenario == 2
      expected_result = ["00"]
    elsif scenario == 3
      expected_result = ["emp_2"]
    elsif scenario == 4
      expected_result = ["obj_2", "obj_4"]
    elsif scenario == 5
      expected_result = []
    elsif scenario == 6
      expected_result = [VisualSpatialFieldObject.getUnknownSquareToken()]
    end
    
    ################
    ##### TEST #####
    ################
    
    result = visual_spatial_field.getCoordinateContents(
      coordinates[0], 
      coordinates[1], 
      time + 150, 
      return_unknown_square_if_empty
    )
    
    # Check if result is nil (will be in scenario 1).  If it isn't, convert it 
    # that it can be checked.
    if result != nil 
      result_array = result.to_a
      result_array.map!{|x| identifier_field.value(x)}
    end
    
    assert_equal(
      expected_result, 
      result_array, 
      "occurred when checking the result of the function in scenario " + 
      scenario.to_s
    )
    
    ########################################
    ##### MODIFY RESULT AND TEST AGAIN #####
    ########################################
    
    if result != nil
      new_obj = VisualSpatialFieldObject.new("9999", "Z", model, visual_spatial_field, time, false, false)
      new_obj._terminus = (time + 200)
      result.add(new_obj)
      
      result_repeat = visual_spatial_field.getCoordinateContents(
        coordinates[0], 
        coordinates[1], 
        time + 150, 
        return_unknown_square_if_empty
      )
      
      result_repeat_array = result_repeat.to_a
      result_repeat_array.map!{|x| identifier_field.value(x)}
      assert_false(
        result_repeat_array.include?(identifier_field.value(new_obj)),
        "occurred when checking if a new object added to the result of the " +
        "function is present when the function is reinvoked in scenario " + 
        scenario.to_s
      )
    end
  end
end

################################################################################
# Tests "getCreatorDetails()" using three scenarios:
# 
# 1. Function is invoked when no creator exists on a VisualSpatialField.
# 2. Function is invoked when two VisualSpatialFieldObjects representing the 
#    agent equipped with CHREST (the creator) exist but the first one added 
#    should be returned.
# 3. Function is invoked when two VisualSpatialFieldObjects representing the 
#    agent equipped with CHREST (the creator) exist but the second one added 
#    should be returned.
unit_test "get_creator_details" do
  
  visual_spatial_field_field = VisualSpatialField.java_class.declared_field("_visualSpatialField")
  visual_spatial_field_field.accessible = true
  
  VisualSpatialFieldObject.class_eval{
    field_accessor :_timeCreated, :_terminus
  }
  
  for scenario in 1..3
    time = 0
    model = Chrest.new(time, true)
  
    creator_details = nil
    if scenario != 1
      creator_details = ArrayList.new()
      creator_details.add("00")
      creator_details.add(Square.new(0,0))
    end
    
    visual_spatial_field = VisualSpatialField.new("", 2, 2, 0, 0, model, creator_details, time)
    
    if scenario != 1
      # Set the terminus for the first VisualSpatialFieldObject representing the
      # agent equipped with CHREST and place another on the VisualSpatialField
      # in a different location.  Set the terminus of the first 
      # VisualSpatialFieldObject representing the agent equipped with CHREST to
      # be earlier than the creation time of the second VisualSpatialFieldObject
      # representing the agent equipped with CHREST.
      vsf = visual_spatial_field_field.value(visual_spatial_field)
      vsf.get(0).get(0).lastEntry().getValue().get(0)._terminus = (time + 100)
      
      # Adding second VisualSpatialFieldObject representing the agent equipped 
      # with CHREST to VisualSpatialField in a different location to the first
      # (1, 1) instead of (0, 0).
      vsf.get(1).get(1).lastEntry().getValue().add(VisualSpatialFieldObject.new(
        "00", 
        Scene.getCreatorToken(), 
        model, 
        visual_spatial_field, 
        vsf.get(0).get(0).lastEntry().getValue().get(0)._terminus + 10, 
        false, 
        false
      ))
    end
    
    # Invoke function
    creator_details = visual_spatial_field.getCreatorDetails(
      (scenario == 3 ? 
        vsf.get(1).get(1).lastEntry().getValue().get(0)._timeCreated :
        time + 10
      )
    )
    
    expected_creator_details = nil
    if scenario == 2 
      expected_creator_details = ArrayList.new()
      expected_creator_details.add("00")
      expected_creator_details.add(Square.new(0, 0))
    elsif scenario == 3
      expected_creator_details = ArrayList.new()
      expected_creator_details.add("00")
      expected_creator_details.add(Square.new(1, 1))
    end
    
    assert_equal(
      creator_details,
      expected_creator_details,
      "occurred during scenario " + scenario.to_s
    )
  end
end

################################################################################
# Tests the "getDomainSpecificColFromVisualSpatialFieldCol()" and 
# "getDomainSpecificRowFromVisualSpatialFieldRow()" functions using the 
# following VisualSpatialField and scenarios:
# 
# VisualSpatialField Used
# =======================
#
#       |---|---|
# 1  7  |   |   |
#       |---|---|
# 0  6  |   |   |
#       |---|---|
#         4   5    DOMAIN-SPECIFIC COORDINATES
#         0   1    VISUAL-SPATIAL FIELD COORDINATES
#
# Scenarios Used
# ==============
# 
# 1. VisualSpatialField column and row input to function both equal 0.
# 2. VisualSpatialField column and row input to function both equal 1.
# 3. VisualSpatialField column and row input to function are not represented in
#    the VisualSpatialField.
#
# Expected Output
# ===============
# 
# 1. Column = 4, row = 6
# 2. Column = 5, row = 7
# 3. Column and row = null
unit_test "get_domain_specific_col_and_row" do
  visual_spatial_field = VisualSpatialField.new("", 2, 2, 4, 6, Chrest.new(0, false), nil, 0)
  
  for scenario in 1..3
    coordinate = 0
    if scenario == 2 then coordinate = 1 end
    if scenario == 3 then coordinate = 2 end
    
    col = visual_spatial_field.getDomainSpecificColFromVisualSpatialFieldCol(coordinate)
    row = visual_spatial_field.getDomainSpecificRowFromVisualSpatialFieldRow(coordinate)
    
    expected_col = 4
    expected_row = 6
    if scenario == 2 then expected_col, expected_row = 5, 7 end
    if scenario == 3 then expected_col, expected_row = nil, nil end
    
    assert_equal(expected_col, col)
    assert_equal(expected_row, row)
  end
end

################################################################################
# Tests the "getVisualSpatialFieldColFromDomainSpecificCol()" and 
# "getVisualSpatialFieldColFromDomainSpecificRow()" functions using the 
# following VisualSpatialField and scenarios:
# 
# VisualSpatialField Used
# =======================
#
#       |---|---|
# 1  7  |   |   |
#       |---|---|
# 0  6  |   |   |
#       |---|---|
#         4   5    DOMAIN-SPECIFIC COORDINATES
#         0   1    VISUAL-SPATIAL FIELD COORDINATES
#
# Scenarios Used
# ==============
# 
# 1. Domain-specific column and row input to function equal 4 and 6.
# 2. Domain-specific column and row input to function equal 5 and 7.
# 3. Domain-specific column and row input to function are not represented in
#    the VisualSpatialField.
#
# Expected Output
# ===============
# 
# 1. Column and row = 0
# 2. Column and row = 1
# 3. Column and row = null
unit_test "get_visual_spatial_field_col_and_row" do
  visual_spatial_field = VisualSpatialField.new("", 2, 2, 4, 6, Chrest.new(0, false), nil, 0)
  
  for scenario in 1..3
    domain_col = 4
    domain_row = 6
    if scenario == 2 then domain_col, domain_row = 5, 7 end
    if scenario == 3 then domain_col, domain_row = 6, 8 end
    
    col = visual_spatial_field.getVisualSpatialFieldColFromDomainSpecificCol(domain_col)
    row = visual_spatial_field.getVisualSpatialFieldRowFromDomainSpecificRow(domain_row)
    
    expected_col = expected_row = 0
    if scenario == 2 then expected_col = expected_row = 1 end
    if scenario == 3 then expected_col = expected_row = nil end
    
    assert_equal(expected_col, col)
    assert_equal(expected_row, row)
  end
end

################################################################################
################################################################################
############################## NON-TEST FUNCTIONS ##############################
################################################################################
################################################################################

def check_visual_spatial_field_against_expected(visual_spatial_field, expected_visual_spatial_field_data, time_to_check_at, error_msg_postpend)
  
  # VisualSpatialField instance field access
  width_field = VisualSpatialField.java_class.declared_field("_width")
  width_field.accessible = true
  
  height_field = VisualSpatialField.java_class.declared_field("_height")
  height_field.accessible = true
  
  visual_spatial_field_field = VisualSpatialField.java_class.declared_field("_visualSpatialField")
  visual_spatial_field_field.accessible = true
  vsf = visual_spatial_field_field.value(visual_spatial_field)
  
  # VisualSpatialFieldObject instance field access
  vsfo_recognised_history_field = VisualSpatialFieldObject.java_class.declared_field("_recognisedHistory")
  vsfo_recognised_history_field.accessible = true
  
  VisualSpatialFieldObject.class_eval{
    field_accessor :_timeCreated, :_terminus
  }
  
  # SceneObject instance field access
  so_identifier_field = SceneObject.java_class.declared_field("_identifier")
  so_identifier_field.accessible = true
  
  so_type_field = SceneObject.java_class.declared_field("_objectType")
  so_type_field.accessible = true
  
  #################
  ##### TESTS #####
  #################
  
  for col in 0...width_field.value(visual_spatial_field)
    for row in 0...height_field.value(visual_spatial_field)

      coordinate_contents = vsf.get(col).get(row).floorEntry(time_to_check_at.to_java(:int)).getValue()
      
      assert_equal(
        expected_visual_spatial_field_data[col][row].size(),
        coordinate_contents.size(),
        "occurred when checking the number of VisualSpatialFieldObjects on " +
        "col " + col.to_s + ", row " + row.to_s + " " + error_msg_postpend
      )
      
#      puts "===== Col " + col.to_s + ", row " + row.to_s + " ====="

      for object in 0...coordinate_contents.size()
#        puts "+++ Object " + object.to_s + " +++"
        vsf_object = coordinate_contents.get(object)
#        puts vsf_object.toString()
        

        error_msg = "VisualSpatialFieldObject " + (object + 1).to_s + " on col " +
        col.to_s + ", row " + row.to_s + " " + error_msg_postpend

        # In some cases, the identifier for a VisualSpatialFieldObject may be 
        # randomly generated so the expected identifier should be nil.  If this
        # is the case, don't check the identifier since its impossible to 
        # predict a randomly assigned identifier so the test will never pass.
        if expected_visual_spatial_field_data[col][row][object][0] != nil
          assert_equal(
            expected_visual_spatial_field_data[col][row][object][0],
            so_identifier_field.value(vsf_object),
            "occurred when checking the identifier of " + error_msg
          )
        end
        
        assert_equal(
          expected_visual_spatial_field_data[col][row][object][1],
          so_type_field.value(vsf_object),
          "occurred when checking the type of " + error_msg
        )

        assert_equal(
          expected_visual_spatial_field_data[col][row][object][2],
          vsfo_recognised_history_field.value(vsf_object).floorEntry(time_to_check_at.to_java(:int)).getValue(),
          "occurred when checking the recognised status of " + error_msg
        )

        assert_equal(
          expected_visual_spatial_field_data[col][row][object][3],
          vsf_object._timeCreated,
          "occurred when checking the creation time of " + error_msg
        )

        assert_equal(
          expected_visual_spatial_field_data[col][row][object][4],
          vsf_object._terminus,
          "occurred when checking the terminus of " + error_msg
        )

      end
    end
  end
end

