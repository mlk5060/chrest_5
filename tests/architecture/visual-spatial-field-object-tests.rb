################################################################################
# VisualSpatialFieldObjects have two constructors, one where the 
# VisualSpatialFieldObject's identifier is set implicitly and one where the
# VisualSpatialFieldObject's identifier is set explicitly.
# 
# In this test, both constructors are tested at once since they should act in 
# the same way (except for identifier setting, as explained).  All types of 
# VisualSpatialFieldObjects that may be created are tested to ensure that no 
# particular VisualSpatialFieldObject type receives "special" consideration 
# except for blind squares, which should never have VisualSpatialFieldObject 
# representations constructed.
# 
# 16 scenarios are used to test the constructors:
# 
# Scenario Descriptions
# =====================
# 
# 1-8. Terminus not set by constructor
# 9-16. Terminus set by constructor
#
# 1-4/9-12. VisualSpatialFieldObject unrecognised
# 5-8/13-16. VisualSpatialFieldObject recognised
#
# 1/5/9/13. VisualSpatialFieldObject type = creator
# 2/6/10/14. VisualSpatialFieldObject type =  Empty square
# 3/7/11/15. VisualSpatialFieldObject type = Physical object
# 4/8/12/16. VisualSpatialFieldObject type = Blind square
# 
# Expected Output
# ===============
# 
# In scenarios 4, 8, 12 and 16 an IllegalArgumentException should be thrown and
# no VisualSpatialFieldObject should be constructed.  In all other scenarios, 
# no IllegalArgumentException should be thrown and a VisualSpatialFieldObject 
# should be constructed and returned by each constructor.
# 
# The identifier, recognised history and terminus of the 
# VisualSpatialFieldObject should differ under certain circumstances.  
# Therefore, the following values are expected for these variables given the
# conditions described:
# 
# - Identifier
#   ~ When the constructor that sets this variable implictly is used, the test
#     checks that this variable isn't empty for the VisualSpatialFieldObject
#     constructed since its value can not be predicted. 
#   ~ When the constructor that sets this variable explictly is used, the test
#     checks that this variable is equal to what is specified for the parameter
#     in the constructor.
#     
# - Recognised history
#   ~ In scenarios 1-4 and 9-12 the most recent value in this variable for the
#     VisualSpatialFieldObject constructed should be boolean false since
#     the scenarios tell the constructor that the VisualSpatialFieldObject to be 
#     constructed should not be recognised.
#   ~ In scenarios 5-8 and 13-16 the most recent value in this variable for the
#     VisualSpatialFieldObject constructed should be boolean true since the
#     scenarios tell the constructor that the VisualSpatialFieldObject to be 
#     constructed should be recognised.
#
# - Terminus
#   ~ In scenarios 1-8 this should equal null since the terminus should not be
#     set by either constructor.
#   ~ In scenarios 9-12 this should equal the creation time of the 
#     VisualSpatialFieldObject constructed plus the value specified for the
#     "_unrecognisedVisualSpatialFieldObjectLifespan" variable of the CHREST 
#     model that the VisualSpatialFieldObject constructed is associated with 
#     since the scenarios state that the terminus of the 
#     VisualSpatialFieldObject to be constructed should be set by the 
#     constructor and the VisualSpatialFieldObject to be constructed is 
#     unrecognised.
#   ~ In scenarios 13-16 this should equal the creation time of the 
#     VisualSpatialFieldObject constructed plus the value specified for the
#     "_recognisedVisualSpatialFieldObjectLifespan" variable of the CHREST 
#     model that the VisualSpatialFieldObject constructed is associated with
#     since the scenarios state that the terminus of the 
#     VisualSpatialFieldObject to be constructed should be set by the 
#     constructor and the VisualSpatialFieldObject to be constructed is 
#     recognised.
unit_test "constructors" do
  
  #############################################################
  ##### SET-UP ACCESS TO RELEVANT PRIVATE INSTANCE FIELDS #####
  #############################################################
  
  Chrest.class_eval{
    field_accessor :_recognisedVisualSpatialFieldObjectLifespan, 
    :_unrecognisedVisualSpatialFieldObjectLifespan
  }
  
  so_identifier_field = SceneObject.java_class.declared_field("_identifier")
  so_type_field = SceneObject.java_class.declared_field("_objectType")
  so_identifier_field.accessible = true
  so_type_field.accessible = true
  
  VisualSpatialFieldObject.class_eval{
    field_accessor :_terminus, :_timeCreated
  }
  
  vsfo_model_field = VisualSpatialFieldObject.java_class.declared_field("_associatedModel")
  vsfo_vsf_field = VisualSpatialFieldObject.java_class.declared_field("_associatedVisualSpatialField")
  vsfo_rec_hist_field = VisualSpatialFieldObject.java_class.declared_field("_recognisedHistory")
  vsfo_model_field.accessible = true
  vsfo_vsf_field.accessible = true
  vsfo_rec_hist_field.accessible = true
  
  #########################
  ##### SCENARIO LOOP #####
  #########################
  
  for scenario in 1..16
    
    #########################################################
    ##### CONSTRUCT Chrest MODEL AND VisualSpatialField #####
    #########################################################
    
    model = Chrest.new(0, false)
    model._recognisedVisualSpatialFieldObjectLifespan = 10000
    model._unrecognisedVisualSpatialFieldObjectLifespan = 5000
    
    visual_spatial_field = VisualSpatialField.new("", 1, 1, 0, 0, model, nil, 0)
    
    ###########################
    ##### SET OBJECT TYPE #####
    ###########################
    
    object_type = Scene.getCreatorToken()
    if [2, 6, 10, 14].include?(scenario) then object_type = Scene.getEmptySquareToken() end
    if [3, 7, 11, 15].include?(scenario) then object_type = "AA" end
    if [4, 8, 12, 16].include?(scenario) then object_type = Scene.getBlindSquareToken() end
    
    ###########################################
    ##### SET RECOGNISED STATUS OF OBJECT #####
    ###########################################
    
    recognised = (scenario.between?(1, 4) || scenario.between?(9, 12) ? false : true)
    
    ################################
    ##### SET TERMINUS SETTING #####
    ################################
    
    set_terminus = (scenario.between?(1, 8) ? false : true)
    
    ###############################
    ##### INVOKE CONSTRUCTORS #####
    ###############################
    
    # Create array to store VisualSpatialFieldObjects constructed.
    visual_spatial_field_objects_created = []
    
    # Invoke constructor where identifier set implictly
    implicit_identifier_constructor_exception_thrown = false
    begin
      visual_spatial_field_objects_created.push(VisualSpatialFieldObject.new(
        object_type, 
        model,
        visual_spatial_field,
        0,
        recognised,
        set_terminus
      ))
    rescue
      implicit_identifier_constructor_exception_thrown = true
    end
    
    # Constructor where identifier set explictly
    explicit_identifier_constructor_exception_thrown = false
    begin
      visual_spatial_field_objects_created.push(VisualSpatialFieldObject.new(
        "00",
        object_type, 
        model,
        visual_spatial_field,
        0,
        recognised,
        set_terminus
      ))
    rescue
      explicit_identifier_constructor_exception_thrown = true
    end
    
    #################
    ##### TESTS #####
    #################
    
    expected_exception_thrown = ([4, 8, 12, 16].include?(scenario) ? true : false)
    
    assert_equal(
      expected_exception_thrown, 
      implicit_identifier_constructor_exception_thrown,
      "occurred when checking if an exception is thrown when invoking the " +
      "constructor that sets VisualSpatialFieldObject identifers implicitly " +
      "in context of scenario " + scenario.to_s
    )
    
    assert_equal(
      expected_exception_thrown, 
      explicit_identifier_constructor_exception_thrown,
      "occurred when checking if an exception is thrown when invoking the " +
      "constructor that sets VisualSpatialFieldObject identifers explicitly " +
      "in context of scenario " + scenario.to_s
    )
    
    for i in 0...visual_spatial_field_objects_created.size
      visual_spatial_field_object_created = visual_spatial_field_objects_created[i]
      
      # Check identifier of VisualSpatialFieldObject depending on whether one 
      # was set explicitly or implicitly
      if i == 0
        assert_false(
          so_identifier_field.value(visual_spatial_field_object_created).empty?,
          "occurred when checking the identifier of the VisualSpatialFieldObject " +
          "created when invoking the constructor that sets " +
          "VisualSpatialFieldObject identifers implicitly in context of " +
          "scenario " + scenario.to_s
        )
      else
        assert_equal(
          "00", 
          so_identifier_field.value(visual_spatial_field_object_created),
          "occurred when checking the identifier of the VisualSpatialFieldObject " +
          "created when invoking the constructor that sets " +
          "VisualSpatialFieldObject identifers explicitly in context of " +
          "scenario " + scenario.to_s
        )
      end
      
      assert_equal(
        object_type,
        so_type_field.value(visual_spatial_field_object_created),
        "occurred when checking the type of the VisualSpatialFieldObject " +
        "constructed after invoking the constructor that sets " +
        "VisualSpatialFieldObject identifers " + (i == 0 ? "implicitly" : 
        "explicitly") + " in context of scenario " + scenario.to_s
      )
      
      assert_equal(
        model,
        vsfo_model_field.value(visual_spatial_field_object_created),
        "occurred when checking the model associated with the VisualSpatialFieldObject " +
        "constructed after invoking the constructor that sets " +
        "VisualSpatialFieldObject identifers " + (i == 0 ? "implicitly" : 
        "explicitly") + " in context of scenario " + scenario.to_s
      )
      
      assert_equal(
        visual_spatial_field,
        vsfo_vsf_field.value(visual_spatial_field_object_created),
        "occurred when checking the VisualSpatialField associated with the " +
        "VisualSpatialFieldObject constructed after invoking the constructor " +
        "that sets VisualSpatialFieldObject identifers " + (i == 0 ? "implicitly" : 
        "explicitly") + " in context of scenario " + scenario.to_s
      )
      
      assert_equal(
        (scenario.between?(1, 4) || scenario.between?(9, 12) ? false : true),
        vsfo_rec_hist_field.value(visual_spatial_field_object_created).lastEntry().getValue(),
        "occurred when checking the recognised status of the VisualSpatialFieldObject " +
        "constructed after invoking the constructor that sets " +
        "VisualSpatialFieldObject identifers " + (i == 0 ? "implicitly" : 
        "explicitly") + " in context of scenario " + scenario.to_s
      )
      
      assert_equal(
        (scenario.between?(1, 8) ? nil :
          (scenario.between?(9, 12) ?
            model._unrecognisedVisualSpatialFieldObjectLifespan : 
            model._recognisedVisualSpatialFieldObjectLifespan
          )
        ),
        visual_spatial_field_object_created._terminus,
        "occurred when checking the terminus of the VisualSpatialFieldObject " +
        "constructed after invoking the constructor that sets " +
        "VisualSpatialFieldObject identifers " + (i == 0 ? "implicitly" : 
        "explicitly") + " in context of scenario " + scenario.to_s
      )
      
      assert_equal(
        0,
        visual_spatial_field_object_created._timeCreated,
        "occurred when checking the creation time of the VisualSpatialFieldObject " +
        "constructed after invoking the constructor that sets " +
        "VisualSpatialFieldObject identifers " + (i == 0 ? "implicitly" : 
        "explicitly") + " in context of scenario " + scenario.to_s
      )
    end
  end
end

################################################################################
unit_test "simple_getters" do
  VisualSpatialFieldObject.class_eval{
    field_accessor :_terminus
  }
  
  model = Chrest.new(0, true)
  vsf = VisualSpatialField.new("", 2, 2, 0, 0, model, nil, 0)
  time_created = 0
  terminus = 10000
  vsfo = VisualSpatialFieldObject.new("00", "AA", model, vsf, time_created, true, false)
  vsfo._terminus = terminus
  
  assert_equal(model, vsfo.getAssociatedModel())
  assert_equal(vsf, vsfo.getAssociatedVisualSpatialField())
  assert_equal(terminus, vsfo.getTerminus())
  assert_equal(time_created, vsfo.getTimeCreated())
end

################################################################################
# Tests the "createClone()" function using 6 scenarios that focus on the 
# terminus of the original VisualSpatialFieldObject and the type of the original
# VisualSpatialFieldObject.  These features are focused upon since the terminus
# can be set during VisualSpatialFieldObject clone construction (which can cause
# problems if handled incorrectly) and no VisualSpatialFieldObject type should 
# have special treatment:
#
# Scenario Description
# ====================
#
# 1-3. Terminus of original VisualSpatialFieldObject set to null
# 4-6. Terminus of original VisualSpatialFieldObject set to 500
#
# 1/4. Original VisualSpatialFieldObject represents the agent equipped with 
#      CHREST, i.e. the creator.
# 2/5. Original VisualSpatialFieldObject represents an empty square.
# 3/6. Original VisualSpatialFieldObject represents a physical object.
#
# In all scenarios, the clone should be an exact replica of the original except 
# that it shouldn't have the same object reference.  To test this, the terminus
# of the clone is modified after creation and then compared to the terminus of
# the original and they should differ.
unit_test "create_clone" do
  
  #############################################################
  ##### SET-UP ACCESS TO RELEVANT PRIVATE INSTANCE FIELDS #####
  #############################################################
  
  VisualSpatialFieldObject.class_eval{
    field_accessor :_terminus, :_timeCreated
  }
  
  vsfo_assoc_model_field = VisualSpatialFieldObject.java_class.declared_field("_associatedModel")
  vsfo_assoc_vsf_field = VisualSpatialFieldObject.java_class.declared_field("_associatedVisualSpatialField")
  vsfo_rec_hist_field = VisualSpatialFieldObject.java_class.declared_field("_recognisedHistory")
  vsfo_assoc_model_field.accessible = true
  vsfo_assoc_vsf_field.accessible = true
  vsfo_rec_hist_field.accessible = true
  
  #########################
  ##### SCENARIO LOOP #####
  #########################
  
  for scenario in 1..6
    
    ######################################################
    ##### SET-UP Chrest MODEL AND VisualSpatialField #####
    ######################################################
    
    model = Chrest.new(0, true)
    vsf = VisualSpatialField.new("", 1, 1, 0, 0, model, nil, 0)
    
    #############################################
    ##### SET VisualSpatialFieldObject TYPE #####
    #############################################
    
    object_type = Scene.getCreatorToken()
    if [2, 5].include?(scenario) then object_type = Scene.getEmptySquareToken() end
    if [3, 6].include?(scenario) then object_type = "AA" end
    
    ###########################################
    ##### CREATE VisualSpatialFieldObject #####
    ###########################################
    
    original = VisualSpatialFieldObject.new(object_type, model, vsf, 100, false, false)
    original._terminus = (scenario.between?(1, 3) ? nil : 500)
    
    #######################################
    ##### CONSTRUCT RECOGNISED STATUS #####
    #######################################
    
    vsfo_rec_hist = vsfo_rec_hist_field.value(original)
    vsfo_rec_hist.put(200, true)
    vsfo_rec_hist.put(300, false)
    vsfo_rec_hist.put(400, true)
  
    #################
    ##### TESTS #####
    #################
    
    clone = original.create_clone()
    
    error_msg = "occurred when checking xxx in scenario " + scenario.to_s
    assert_equal(vsfo_assoc_model_field.value(clone), vsfo_assoc_model_field.value(original), error_msg.gsub("xxx", "the associated CHREST model")) 
    assert_equal(vsfo_assoc_vsf_field.value(clone), vsfo_assoc_vsf_field.value(original), error_msg.gsub("xxx", "the associated VisualSpatialField")) 
    assert_equal(clone._timeCreated, original._timeCreated, error_msg.gsub("xxx", "the time created"))
    assert_equal(clone._terminus, original._terminus, error_msg.gsub("xxx", "the terminus"))
    assert_equal(vsfo_rec_hist_field.value(clone).size(), vsfo_rec_hist_field.value(original).size(), error_msg.gsub("xxx", "the size of recognised history"))
    
    for clone_rec_hist_entry in vsfo_rec_hist_field.value(clone).entrySet()
      original_value = vsfo_rec_hist_field.value(original).get(clone_rec_hist_entry.getKey().to_java(:int))
      assert_equal(
        clone_rec_hist_entry.getValue(),
        original_value,
        error_msg.gsub("xxx", "the value for entry with key " + clone_rec_hist_entry.getKey().to_java(:int).to_s + " in the recognised history")
      )
    end
    
    # Alter clone and see if original changes (it shouldn't)
    clone._terminus = 1000
    assert_true(clone._terminus != original._terminus, error_msg.gsub("xxx", "the terminus after modifying the clone"))
  end
end

################################################################################
# Tests the "isAlive" function by invoking this function in context of  all 
# possible VisualSpatialFieldObject types to ensure that no particular 
# VisualSpatialFieldObject type receives "special" treatment.
unit_test "is_alive" do
  
  #############################################################
  ##### SET-UP ACCESS TO RELEVANT PRIVATE INSTANCE FIELDS #####
  #############################################################
  
  VisualSpatialFieldObject.class_eval{
    field_accessor :_terminus
  }
  
  #########################
  ##### SCENARIO LOOP #####
  #########################
  
  for scenario in 1..3
    
    ######################################################
    ##### SET-UP Chrest MODEL AND VisualSpatialField #####
    ######################################################
    
    model = Chrest.new(0, true)
    vsf = VisualSpatialField.new("", 1, 1, 0, 0, model, nil, 0)
    
    #############################################
    ##### SET VisualSpatialFieldObject TYPE #####
    #############################################
    
    object_type = Scene.getCreatorToken()
    if scenario == 2 then object_type = Scene.getEmptySquareToken() end
    if scenario == 3 then object_type = "AA" end
    
    ###########################################
    ##### CREATE VisualSpatialFieldObject #####
    ###########################################
    
    vsfo = VisualSpatialFieldObject.new(object_type, model, vsf, 100, false, false)
    vsfo._terminus = nil
  
    #################
    ##### TESTS #####
    #################
  
    error_msg = "occurred when checking if VisualSpatialFieldObject is alive "
  
    # Before created
    assert_false(vsfo.isAlive(99), error_msg + " before its creation in scenario " + scenario.to_s)

    # After created and terminus null
    assert_true(vsfo.isAlive(100), error_msg + " on its creation time when its terminus is null in scenario " + scenario.to_s)
    assert_true(vsfo.isAlive(150), error_msg + " after its creation time when its terminus is null in scenario " + scenario.to_s)

    # After created and terminus not null and greater than time
    vsfo._terminus = 200
    assert_true(vsfo.isAlive(150), error_msg + " after its creation time when its terminus is not null in scenario " + scenario.to_s)

    # After created and terminus not null and greater than or equal to time
    assert_false(vsfo.isAlive(200), error_msg + " after its creation time but on its terminus when its terminus is not null in scenario " + scenario.to_s)
    assert_false(vsfo.isAlive(260), error_msg + " after its creation time but after its terminus when its terminus is not null in scenario " + scenario.to_s)
  end
end

################################################################################
unit_test "is_recognised" do
  
  #############################################################
  ##### SET-UP ACCESS TO RELEVANT PRIVATE INSTANCE FIELDS #####
  #############################################################
  
  vsfo_rec_hist_field = VisualSpatialFieldObject.java_class.declared_field("_recognisedHistory")
  vsfo_rec_hist_field.accessible = true
    
  ##########################################################################
  ##### SET-UP Chrest, VisualSpatialField AND VisualSpatialFieldObject #####
  ##########################################################################

  model = Chrest.new(0, true)
  vsf = VisualSpatialField.new("", 1, 1, 0, 0, model, nil, 0)
  vsfo = VisualSpatialFieldObject.new("AA", model, vsf, 100, false, false)

  #######################################
  ##### CONSTRUCT RECOGNISED STATUS #####
  #######################################

  vsfo_rec_hist = vsfo_rec_hist_field.value(vsfo)
  vsfo_rec_hist.put(200, true)
  vsfo_rec_hist.put(300, false)
  vsfo_rec_hist.put(400, true)
  
  #################
  ##### TESTS #####
  #################
  
  error_msg = "at time xxx"
  assert_false(vsfo.isRecognised(99), error_msg.gsub("xxx", 99.to_s))
  assert_false(vsfo.isRecognised(100), error_msg.gsub("xxx", 100.to_s))
  assert_false(vsfo.isRecognised(150), error_msg.gsub("xxx", 150.to_s))
  assert_false(vsfo.isRecognised(199), error_msg.gsub("xxx", 199.to_s))
  assert_true(vsfo.isRecognised(200), error_msg.gsub("xxx", 200.to_s))
  assert_true(vsfo.isRecognised(250), error_msg.gsub("xxx", 250.to_s))
  assert_true(vsfo.isRecognised(299), error_msg.gsub("xxx", 299.to_s))
  assert_false(vsfo.isRecognised(300), error_msg.gsub("xxx", 300.to_s))
  assert_false(vsfo.isRecognised(350), error_msg.gsub("xxx", 350.to_s))
  assert_false(vsfo.isRecognised(399), error_msg.gsub("xxx", 399.to_s))
  assert_true(vsfo.isRecognised(400), error_msg.gsub("xxx", 400.to_s))
  assert_true(vsfo.isRecognised(450), error_msg.gsub("xxx", 450.to_s))
  assert_true(vsfo.isRecognised(499), error_msg.gsub("xxx", 499.to_s))
end

################################################################################
# Tests the "setRecognised()" function using 6 scenarios that focus on when the
# recognised status of a VisualSpatialFieldObject is updated (the first 
# parameter passed to the function) and whether the VisualSpatialFieldObjects
# terminus is automatically updated (the second parameter passed to the 
# function).
#
# Scenario Descriptions
# =====================
# 
# 1-3. Terminus should not be automatically updated.
# 4-6. Terminus should be automatically updated.
# 
# 1/4. Before VisualSpatialFieldObject is created.
# 2/5. After VisualSpatialFieldObject is created.
# 3/6. After VisualSpatialFieldObject's terminus.
unit_test "set_recognised" do
  
  #############################################################
  ##### SET-UP ACCESS TO RELEVANT PRIVATE INSTANCE FIELDS #####
  #############################################################
  
  Chrest.class_eval{
    field_accessor :_recognisedVisualSpatialFieldObjectLifespan
  }
  
  VisualSpatialFieldObject.class_eval{
    field_accessor :_terminus
  }
  
  vsfo_rec_hist_field = VisualSpatialFieldObject.java_class.declared_field("_recognisedHistory")
  vsfo_rec_hist_field.accessible = true
  
  for scenario in 1..6
    
    ##########################################################################
    ##### SET-UP Chrest, VisualSpatialField AND VisualSpatialFieldObject #####
    ##########################################################################

    model = Chrest.new(0, true)
    vsf = VisualSpatialField.new("", 1, 1, 0, 0, model, nil, 0)
    vsfo = VisualSpatialFieldObject.new("AA", model, vsf, 100, false, false)
    vsfo._terminus = 500

    #######################################
    ##### SET PARAMETERS FOR FUNCTION #####
    #######################################
    time_to_set_recognised = 
      ([1, 4].include?(scenario) ? 
        99 : 
        ([2, 5].include?(scenario) ?
          150 :
          501
        )
      )
      
    update_terminus = (scenario.between?(1, 3) ? false : true)
    
    ###########################
    ##### INVOKE FUNCTION #####
    ###########################
    
    vsfo.setRecognised(time_to_set_recognised, update_terminus)
    
    ################
    ##### TEST #####
    ################
    
    expected_recognition_value = ([2, 5].include?(scenario) ? true : nil)
    expected_terminus = (scenario == 5 ? time_to_set_recognised + model._recognisedVisualSpatialFieldObjectLifespan : 500)
    
    assert_equal(
      expected_recognition_value,
      vsfo_rec_hist_field.value(vsfo).get(time_to_set_recognised.to_java(:int)),
      "occurred when checking recognition value in scenario " + scenario.to_s
    )
    
    assert_equal(
      expected_terminus,
      vsfo._terminus,
      "occurred when checking the terminus in scenario " + scenario.to_s
    )
  end
end

################################################################################
# Tests the "setTerminus()" function using 4 scenarios that focus on the 
# recognised status of the VisualSpatialFieldObject whose terminus is being set
# (determines how the terminus is set, if applicable), and whether the terminus
# should be set to the first parameter specified to the function:
# 
# Scenario Descriptions
# =====================
# 
# 1-2. VisualSpatialFieldObject not recognised
# 3-4. VisualSpatialFieldObject recognised
# 
# 1/3: Set terminus to time
# 2/4: Don't set terminus to time
#
# Expected Output
# ===============
# 
# 1. Set to time specified
# 2. Set to time specified + unrecognised VisualSpatialFieldObject life defined 
#    in the CHREST model associated with the VisualSpatialFieldObject.
# 3. Set to time specified
# 4. Set to time specified + recognised VisualSpatialFieldObject life defined 
#    in the CHREST model associated with the VisualSpatialFieldObject.
unit_test "set_terminus" do
  
  #############################################################
  ##### SET-UP ACCESS TO RELEVANT PRIVATE INSTANCE FIELDS #####
  #############################################################
  
  Chrest.class_eval{
    field_accessor :_recognisedVisualSpatialFieldObjectLifespan,
      :_unrecognisedVisualSpatialFieldObjectLifespan
  }
  
  VisualSpatialFieldObject.class_eval{
    field_accessor :_terminus
  }
  
  vsfo_rec_hist_field = VisualSpatialFieldObject.java_class.declared_field("_recognisedHistory")
  vsfo_rec_hist_field.accessible = true
  
  for scenario in 1..4
    
    ##########################################################################
    ##### SET-UP Chrest, VisualSpatialField AND VisualSpatialFieldObject #####
    ##########################################################################

    model = Chrest.new(0, true)
    vsf = VisualSpatialField.new("", 1, 1, 0, 0, model, nil, 0)
    vsfo = VisualSpatialFieldObject.new("AA", model, vsf, 100, (scenario.between?(1, 2) ? false : true), false)
    vsfo._terminus = 500

    ###########################
    ##### INVOKE FUNCTION #####
    ###########################
    
    time_specified = 150
    vsfo.setTerminus(time_specified, ([1, 3].include?(scenario) ? true : false))
    
    ################
    ##### TEST #####
    ################
    
    expected_terminus = 
      (scenario == 2 ?
        time_specified + model._unrecognisedVisualSpatialFieldObjectLifespan :
        (scenario == 4 ?
          time_specified + model._recognisedVisualSpatialFieldObjectLifespan :
          time_specified
        )
      )
    
    assert_equal(
      expected_terminus,
      vsfo._terminus,
      "occurred when checking the terminus in scenario " + scenario.to_s
    )
  end
end

################################################################################
# Tests the "setUnrecognised()" function using 6 scenarios that focus on when 
# the recognised status of a VisualSpatialFieldObject is updated (the first 
# parameter passed to the function) and whether the VisualSpatialFieldObjects
# terminus is automatically updated (the second parameter passed to the 
# function).
#
# Scenario Descriptions
# =====================
# 
# 1-3. Terminus should not be automatically updated.
# 4-6. Terminus should be automatically updated.
# 
# 1/4. Before VisualSpatialFieldObject is created.
# 2/5. After VisualSpatialFieldObject is created.
# 3/6. After VisualSpatialFieldObject's terminus.
unit_test "set_unrecognised" do
  
  #############################################################
  ##### SET-UP ACCESS TO RELEVANT PRIVATE INSTANCE FIELDS #####
  #############################################################
  
  Chrest.class_eval{
    field_accessor :_unrecognisedVisualSpatialFieldObjectLifespan
  }
  
  VisualSpatialFieldObject.class_eval{
    field_accessor :_terminus
  }
  
  vsfo_rec_hist_field = VisualSpatialFieldObject.java_class.declared_field("_recognisedHistory")
  vsfo_rec_hist_field.accessible = true
  
  for scenario in 1..6
    
    ##########################################################################
    ##### SET-UP Chrest, VisualSpatialField AND VisualSpatialFieldObject #####
    ##########################################################################

    model = Chrest.new(0, true)
    vsf = VisualSpatialField.new("", 1, 1, 0, 0, model, nil, 0)
    vsfo = VisualSpatialFieldObject.new("AA", model, vsf, 100, true, false)
    vsfo._terminus = 500

    #######################################
    ##### SET PARAMETERS FOR FUNCTION #####
    #######################################
    time_to_set_unrecognised = 
      ([1, 4].include?(scenario) ? 
        99 : 
        ([2, 5].include?(scenario) ?
          150 :
          501
        )
      )
      
    update_terminus = (scenario.between?(1, 3) ? false : true)
    
    ###########################
    ##### INVOKE FUNCTION #####
    ###########################
    
    vsfo.setUnrecognised(time_to_set_unrecognised, update_terminus)
    
    ################
    ##### TEST #####
    ################
    
    expected_recognition_value = ([2, 5].include?(scenario) ? false : nil)
    expected_terminus = (scenario == 5 ? time_to_set_unrecognised + model._unrecognisedVisualSpatialFieldObjectLifespan : 500)
    
    assert_equal(
      expected_recognition_value,
      vsfo_rec_hist_field.value(vsfo).get(time_to_set_unrecognised.to_java(:int)),
      "occurred when checking recognition value in scenario " + scenario.to_s
    )
    
    assert_equal(
      expected_terminus,
      vsfo._terminus,
      "occurred when checking the terminus in scenario " + scenario.to_s
    )
  end
end