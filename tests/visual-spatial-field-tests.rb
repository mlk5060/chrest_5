################################################################################
# Tests that an entirely blind scene is handled correctly by the visual-spatial 
# field constructor, i.e. nothing is encoded so CHREST's attention clock is not 
# incremented from the time of visual-spatial field creation specified.
unit_test "constructor (blind scene to encode)" do
  scene = Scene.new("blind", 10, 10, nil)
  scene.addItemToSquare(5, 5, "00", Scene.getCreatorToken())
  
  creation_time = 0
  time_to_encode_objects = 50
  time_to_encode_empty_squares = 5
  visual_spatial_field_access_time = 100
  time_to_move_object = 250
  lifespan_for_recognised_objects = 10000
  lifespan_for_unrecognised_objects = 5000
  number_fixations = 20
  
  model = Chrest.new
  model.setDomain(GenericDomain.new(model))
  
  visual_spatial_field = VisualSpatialField.new(
    model,
    scene, 
    time_to_encode_objects,
    time_to_encode_empty_squares,
    visual_spatial_field_access_time, 
    time_to_move_object, 
    lifespan_for_recognised_objects,
    lifespan_for_unrecognised_objects,
    number_fixations,
    creation_time,
    false,
    false
  )
  
  assert_equal(0, model.getAttentionClock, "occurred when checking CHREST's attention clock.")
  assert_equal(0, visual_spatial_field.getHeight(), "occurred when checking the height of the visual-spatial field.")
  assert_equal(0, visual_spatial_field.getWidth(), "occurred when checking the width of the visual-spatial field.")
end

################################################################################
# Tests that the visual-spatial field constructor operates as expected given all
# possible permutations of the parameters that can be supplied to the 
# constructor:
# 
# 1) Do not encode the scene creator or ghost objects.
# 2) Do not encode the scene creator but encode ghost objects.
# 3) Encode the scene creator but do not encode ghost objects.
# 4) Encode the scene creator and ghost objects.
#
# The outcome of the constructor for each of these permutations is constructed 
# for a variety of scenarios designed to cover every possible scenario that may
# occur when a visual-spatial field is constructed.
unit_test "constructor (non-blind scenes to encode)" do
  
  ##########################################
  ##### SET INDEPENDENT TEST VARIABLES #####
  ##########################################
  
  time_to_encode_objects = 50
  time_to_encode_empty_squares = 5
  visual_spatial_field_access_time = 100
  time_to_move_object = 250
  recognised_object_lifespan = 20000
  unrecognised_object_lifespan = 10000
  number_fixations = 20 # Crucial to ensure that all chunks that should be 
                        # returned are returned during object recognition in
                        # visual-spatial field construction
  
  #####################
  ##### TEST LOOP #####
  #####################
  
  for test in 1..4
    encode_scene_creator = false
    encode_ghost_objects = false
    
    if test == 1
    elsif test == 2
      encode_ghost_objects = true
    elsif test == 3
      encode_scene_creator = true
    else
      encode_ghost_objects = true
      encode_scene_creator = true
    end
    
    # Get scenario data.
    scenario_data = get_visual_spatial_field_construction_scenario_data(
      encode_scene_creator, 
      encode_ghost_objects, 
      time_to_encode_objects, 
      time_to_encode_empty_squares,
      recognised_object_lifespan
    )
    
    #########################
    ##### SCENARIO LOOP #####
    #########################
    
    for scenario in 1..scenario_data.count
      
      data = scenario_data[scenario - 1]
      
      reality = data[0]
      list_patterns_to_learn = data[1]
      number_chunks_recognised = data[2]
      expected_visual_spatial_field_object_properties = data[3]
      squares_to_be_ignored = data[4]
      number_unrecognised_objects = data[5]
      number_empty_squares = data[6]
      squares_to_fixate_on = data[7]
      
      # Create a new CHREST instance and set its domain (important to enable 
      # correct or expected perceptual mechanisms).
      model = Chrest.new
      model.setDomain(GenericDomain.new(model))

      # Set the model's field of view to 1 so that unrecognised objects do not
      # cause the model to not recognise intended objects due to list-patterns 
      # input to LTM containing extraneous objects.
      model.getPerceiver().setFieldOfView(1)
      
      # Set the domain time (the external clock to CHREST) to 0.
      domain_time = 0
      
      ###############################
      ##### LEARN LIST PATTERNS #####
      ###############################

      for list_pattern in list_patterns_to_learn
        recognised_chunk = model.recogniseAndLearn(list_pattern, domain_time).getImage()
        until recognised_chunk.contains(list_pattern.getItem(list_pattern.size()-1))
          recognised_chunk = model.recogniseAndLearn(list_pattern, domain_time).getImage()
          domain_time += 1
        end
      end

      # Set domain time to time that learning finishes so that when the 
      # visual-spatial field of the model is constructed below, 
      # the attention clock of the model will be free to do this.
      domain_time = model.getAttentionClock

      ############################################
      ##### INSTANTIATE VISUAL-SPATIAL FIELD #####
      ############################################

      # Since reality is scanned using CHREST's perceptual mechanisms when 
      # encoding reality into a visual-spatial field and since CHREST's 
      # perceptual mechanisms are not deterministic with respect to what order 
      # squares in reality are fixated on, testing of the visual-spatial field 
      # constructed should only be performed when the objects recognised in 
      # reality are recognised in the order specified for each list pattern to 
      # learn in each sub-test.  This allows for accurate calculations to be 
      # made regarding creation/terminus times and recognised/ghost status for 
      # each VisualSpatialFieldObject in the visual-spatial field.  So, to control when 
      # the model is ready for testing, the boolean flag set initially to 
      # "false" below is used.  This is only set to true when the contents of 
      # STM after constructing the visual-spatial field are what is expected by 
      # the current scenario.
      visual_stm_contents_as_expected = false
      fixations_as_expected = false
      creation_time = domain_time
      expected_stm_contents = ""
      for list_pattern in list_patterns_to_learn
        expected_stm_contents += list_pattern.toString()
      end

      while !visual_stm_contents_as_expected or !fixations_as_expected do
        
        #Reset loop control variables.
        visual_stm_contents_as_expected = false
        fixations_as_expected = false
        
        # Set creation time to the current domain time (this is important in 
        # calculating a lot of test variables below).
        creation_time = domain_time

        # Construct the visual-spatial field.
        visual_spatial_field = VisualSpatialField.new(
          model,
          reality, 
          time_to_encode_objects,
          time_to_encode_empty_squares,
          visual_spatial_field_access_time, 
          time_to_move_object, 
          recognised_object_lifespan,
          unrecognised_object_lifespan,
          number_fixations,
          domain_time,
          encode_ghost_objects,
          false
        )

        # Get contents of STM (will have been populated during object 
        # recognition during visual-spatial field construction) and remove root 
        # nodes and nodes with empty images.  This will leave retrieved chunks 
        # that have non-empty images, i.e. these images should contain the 
        # list-patterns learned by the model.
        stm = model.getVisualStm()
        stm_contents = ""
        for i in (stm.getCount() - 1).downto(0)
          chunk = stm.getItem(i)
          if( !chunk.equals(model.getVisualLtm()) )
            if(!chunk.getImage().isEmpty())
              stm_contents += chunk.getImage().toString()
            end
          end
        end

        # Check if STM contents are as expected, if they are, set the flag that
        # controls when the model is ready for testing to true.
        expected_stm_contents == stm_contents ? visual_stm_contents_as_expected = true : nil
        
        # Check if the fixations expected to have been made have been made
        fixations_as_expected = expected_fixations_made?(model, squares_to_fixate_on)

        # Advance domain time to the time that the visual-spatial field will be 
        # completely instantiated so that the model's attention will be free 
        # should a new visual-field need to be constructed.
        domain_time = model.getAttentionClock
      end
    
      ###################
      ##### TESTING #####
      ###################
      
      error_message_test_type = "occurred when scene creator " + 
        ((test == 1 || test == 2) ? "is not" : "is") + 
        " encoded and ghost objects " +
        ((test == 1 || test == 3) ? "are not" : "are") +
        " encoded"

      expected_visual_spatial_field_object_properties = add_expected_values_for_unrecognised_visual_spatial_objects(
        reality, 
        expected_visual_spatial_field_object_properties, 
        squares_to_be_ignored,
        time_to_encode_objects,
        time_to_encode_empty_squares,
        unrecognised_object_lifespan,
        number_chunks_recognised
      )
    
      # Set the time that the model's attention is expected to become free after
      # instantiating the visual-spatial field.
      expected_attention_free_time = get_visual_spatial_field_instantiation_complete_time(
        creation_time, 
        visual_spatial_field_access_time,
        time_to_encode_objects,
        time_to_encode_empty_squares,
        number_chunks_recognised,
        number_unrecognised_objects,
        number_empty_squares
      )
      assert_equal(expected_attention_free_time, model.getAttentionClock(), error_message_test_type + " and the attention clock of the CHREST model in scenario " + scenario.to_s + " is checked.")
     
      # 1) Test that the number of items on each visual-spatial coordinate is as 
      #    expected.
      # 2) For each VisualSpatialFieldObject on each visual-spatial coordinate:
      #    a) Set its creation and terminus values now that the visual-spatial 
      #       field creation time has been set.
      #    b) Check that its identifier, class, time created, terminus, 
      #       recognised status and ghost status is as expected.
      error_message_prescript = error_message_test_type + " in scenario " + scenario.to_s + " when checking "
    
      visual_spatial_field_to_check = get_entire_visual_spatial_field(visual_spatial_field)
      for row in 0...reality.getHeight()
        for col in 0...reality.getWidth()

          visual_spatial_field_objects = visual_spatial_field_to_check.get(col).get(row)
          assert_equal(expected_visual_spatial_field_object_properties[col][row].count(), visual_spatial_field_objects.size(), error_message_prescript + "the number of items on col " + col.to_s + ", row " + row.to_s)

          for i in 0...visual_spatial_field_objects.size()
            error_message_postscript = " for object " + i.to_s  + " on col " + col.to_s + ", row " + row.to_s + "."
            expected_visual_spatial_field_object = expected_visual_spatial_field_object_properties[col][row][i]

            expected_visual_spatial_field_object[2] += (creation_time + visual_spatial_field_access_time)
            expected_visual_spatial_field_object[3] = (expected_visual_spatial_field_object[3] == nil ? nil : (expected_visual_spatial_field_object[3] + expected_visual_spatial_field_object[2]))

            visual_spatial_field_object = visual_spatial_field_objects[i]
            
            assert_equal(expected_visual_spatial_field_object[0], visual_spatial_field_object.getIdentifier(), error_message_prescript + "the identifier" + error_message_postscript)
            assert_equal(expected_visual_spatial_field_object[1], visual_spatial_field_object.getObjectClass(), error_message_prescript + "the object class" + error_message_postscript)
            assert_equal(expected_visual_spatial_field_object[2], visual_spatial_field_object.getTimeCreated(), error_message_prescript + "the creation time" + error_message_postscript)
            assert_equal(expected_visual_spatial_field_object[3], visual_spatial_field_object.getTerminus(), error_message_prescript + "the terminus" + error_message_postscript)
            assert_equal(expected_visual_spatial_field_object[4], visual_spatial_field_object.recognised(domain_time), error_message_prescript + "the recognised status" + error_message_postscript)
            assert_equal(expected_visual_spatial_field_object[5], visual_spatial_field_object.isGhost(), error_message_prescript + "the ghost status" + error_message_postscript)
          end
        end
      end
    end
  end
end

################################################################################
# Tests for correct operation of the 
# "VisualSpatialField.checkForDuplicateObjects()" method (this has private 
# access in VisualSpatialScene but is used in the constructor so must be 
# accessed implicitly through the constructor rather than explicitly calling 
# it).  To do this, three sub-tests are performed:
# 
# 1) A Scene containing blind and empty squares only is constructed.  Despite
#    all VisualSpatialFieldObject instances that represent blind and empty 
#    having identical identifiers, these should be exluded from the duplicate
#    check and thus, no error should be thrown.
# 2) A Scene containing blind and empty squares along with two objects that have 
#    the same class but different identifiers is constructed.  Despite the 
#    objects having the same class, their identifiers differ so no error should
#    be thrown.
# 3) A Scene containing blind and empty squares along with two objects that have 
#    different classes but the same identifiers is constructed.  In this case,
#    an error should be thrown due to the existence of two objects with the same
#    identifier, irrespective of whether they have the same class.
#
unit_test "duplicate items" do
  
  ######################
  ##### SUB-TEST 1 #####
  ######################
  
  model = Chrest.new
  model.setDomain(GenericDomain.new(model))
  model.getPerceiver().setFieldOfView(1)
  
  scene = Scene.new("Blind and empty", 5, 5, nil)
  scene.addItemToSquare(0, 0, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(0, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  
  # The CHREST model needs to fixate on both the squares with the objects on
  # so this data structure facilitates checking that this is the case when the
  # visual-spatial field is constructed.  If the required squares are not 
  # fixated on, the visual-spatial field is constructed again until they are.
  squares_to_fixate_on = [
    [0, 0],
    [0, 1]
  ]
  
  # Used to stipulate if the fixations that should be made have been made in 
  # order for the test to proceed.
  fixations_as_expected = false
  
  # Used to determine if an error is thrown correctly when both squares 
  # containing the objects of interest to this test are fixated on.
  error_thrown = false
  
  begin #Required since an error will be thrown (like a try-catch block in Java)
    
    until fixations_as_expected
      VisualSpatialField.new( 
        model,
        scene, 
        0,
        0,
        0, 
        0, 
        0,
        0,
        20,
        0,
        false,
        false
      )
    
      fixations_as_expected = expected_fixations_made?(model, squares_to_fixate_on)
    end
    
  rescue
    error_thrown = true
  end
  assert_false(error_thrown, "occurred when checking if an error is thrown after encoding only blind and empy squares in the Scene to encode.")
  
  ######################
  ##### SUB-TEST 2 #####
  ######################
  
  scene = Scene.new("Duplicate classes, unique IDs", 5, 5, nil)
  scene.addItemToSquare(0, 0, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(0, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(0, 2, "0", "A")
  scene.addItemToSquare(0, 3, "1", "A")
  
  # Create a new CHREST model
  model = Chrest.new
  model.setDomain(GenericDomain.new(model))
  model.getPerceiver().setFieldOfView(2)
  
  # Modify "squares_to_fixate_on" data structure and reset the 
  # "fixations_as_expected" variable
  squares_to_fixate_on = [
    [0, 2],
    [0, 3]
  ]
  
  # Reset the "fixations_as_expected" variable.
  fixations_as_expected = false
  
  # Reset the "error_thrown" variable.
  error_thrown = false
  
  begin #Required since an error will be thrown (like a try-catch block in Java)
    until fixations_as_expected
      VisualSpatialField.new( 
        model,
        scene, 
        0,
        0,
        0, 
        0, 
        0,
        0,
        20,
        0,
        false,
        false
      )
    
      fixations_as_expected = expected_fixations_made?(model, squares_to_fixate_on)
    end
  rescue
    error_thrown = true
  end
  assert_false(error_thrown, "occurred when checking if an error is thrown after encoding objects with duplicate classes but unique identifiers in the Scene to encode.")

  ######################
  ##### SUB-TEST 3 #####
  ######################
  
  scene = Scene.new("Unique classes, duplicate IDs", 5, 5, nil)
  scene.addItemToSquare(0, 0, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(0, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(0, 2, "0", "A")
  scene.addItemToSquare(0, 3, "0", "B")
  
  # Reset the CHREST model
  model = Chrest.new
  model.setDomain(GenericDomain.new(model))
  model.getPerceiver().setFieldOfView(2)
  
  # Modify "squares_to_fixate_on" data structure and reset the 
  # "fixations_as_expected" variable
  squares_to_fixate_on = [
    [0, 2],
    [0, 3]
  ]
  fixations_as_expected = false
  
  # Reset the "error_thrown" variable.
  error_thrown = false
  
  begin #Required since an error will be thrown (like a try-catch block in Java)
    until fixations_as_expected
      VisualSpatialField.new( 
        model,
        scene, 
        0,
        0,
        0, 
        0, 
        0,
        0,
        20,
        0,
        false,
        false
      )
    
      fixations_as_expected = expected_fixations_made?(model, squares_to_fixate_on)
    end
  rescue
    error_thrown = true
  end
  assert_true(error_thrown, "occurred when checking if an error is thrown after encoding objects with unique classes but duplicate identifiers in the Scene to encode.")
end

################################################################################
# Checks for correct operation of the "VisualSpatialField.getAsScene()" function
# when all possible permutations of parameters are supplied.  Four scenarios 
# are tested:
# 
# 1) The Scene returned is as expected after all objects are encoded but before 
#    any of their termini are reached.  Ghost objects are to not be present in 
#    the Scene returned.
# 2) The Scene returned is as expected after all objects are encoded but before 
#    any of their termini are reached.  Ghost objects are to be present in the 
#    Scene returned.
# 3) The Scene returned is as expected after all objects are encoded but before 
#    any of their termini are reached.  Two objects should exist on the same 
#    coordinates.
# 4) The Scene returned is as expected after the termini for all objects have 
#    been reached.
unit_test "get_as_scene" do
  
  # Set the objects that will be used.
  test_objects = [
    ["0", "A"], 
    ["1", "B"],
    [VisualSpatialField.getGhostObjectIdPrefix + "0", "B"],
    ["3", "C"],
    ["4", Scene.getCreatorToken()]
  ]
  
  ########################
  ##### CREATE SCENE #####
  ########################

  scene = Scene.new("Test scene", 5, 5, nil)
  scene.addItemToSquare(2, 0, test_objects[4][0], test_objects[4][1])
  scene.addItemToSquare(1, 1, test_objects[0][0], test_objects[0][1])
  scene.addItemToSquare(2, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(3, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(0, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(1, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(2, 2, test_objects[1][0], test_objects[1][1])
  scene.addItemToSquare(3, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(4, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(1, 3, test_objects[3][0], test_objects[3][1])
  scene.addItemToSquare(2, 3, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(3, 3, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(2, 4, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())

  ###################################
  ##### CREATE NEW CHREST MODEL #####
  ###################################
  model = Chrest.new
  model.setDomain(GenericDomain.new(model))
  model.getPerceiver.setFieldOfView(1)

  #########################
  ##### SET TEST TIME #####
  #########################

  # Set the domain time (the time against which all CHREST operations will be
  # performed in this test).
  domain_time = 0

  ###########################
  ##### CHREST LEARNING #####
  ###########################

  # Since the scene creator is present, the locations of 0 and the ghost object
  # should be learned as creator-relative since this is how the coordinates will
  # be formatted during recognition when the visual-spatial field is 
  # constructed.
  real_object_pattern = ItemSquarePattern.new(test_objects[0][1], -1, 1)
  ghost_object_pattern = ItemSquarePattern.new(test_objects[2][1], -1, 0)
  list_pattern_to_learn = ListPattern.new()
  list_pattern_to_learn.add(real_object_pattern)
  list_pattern_to_learn.add(ghost_object_pattern)
  recognised_chunk = model.recogniseAndLearn(list_pattern_to_learn, domain_time).getImage().toString()
  until recognised_chunk == list_pattern_to_learn.toString()
    domain_time += 1
    recognised_chunk = model.recogniseAndLearn(list_pattern_to_learn, domain_time).getImage().toString()
  end

  # Set the domain time to be the value of CHREST's learning clock since, when
  # the visual-spatial field is constructed, the LTM of the model will contain
  # the completely familiarised learned pattern enabling expected visual-spatial
  # field construction due to chunk recognition retrieving the learned pattern.
  domain_time = model.getLearningClock()

  ##########################################
  ##### CONSTRUCT VISUAL-SPATIAL FIELD #####
  ##########################################

  # Set visual-spatial field variables.
  creation_time = domain_time
  number_fixations = 20
  time_to_encode_objects = 50
  time_to_encode_empty_squares = 10
  visual_spatial_field_access_time = 100
  time_to_move_object = 250
  recognised_object_lifespan = 60000
  unrecognised_object_lifespan = 30000
  
  creation_time = domain_time

  visual_stm_contents_as_expected = false
  expected_stm_contents = recognised_chunk
  
  expected_fixations_made = false
  fixations_expected = [
    [2, 0],
    [1, 1], 
    [2, 2],
    [1, 3],
  ]
  
  until visual_stm_contents_as_expected and expected_fixations_made do
    
    visual_stm_contents_as_expected = false
    expected_fixations_made = false

    # Set creation time to the current domain time (this is important in 
    # calculating a lot of test variables below).
    creation_time = domain_time

    # Construct the visual-spatial field.
    visual_spatial_field = VisualSpatialField.new(
      model,
      scene, 
      time_to_encode_objects,
      time_to_encode_empty_squares,
      visual_spatial_field_access_time, 
      time_to_move_object, 
      recognised_object_lifespan,
      unrecognised_object_lifespan,
      number_fixations,
      domain_time,
      true,
      false
    )

    # Get contents of STM (will have been populated during object 
    # recognition during visual-spatial field construction) and remove root 
    # nodes and nodes with empty images.  This will leave retrieved chunks 
    # that have non-empty images, i.e. these images should contain the 
    # list-patterns learned by the model.
    stm = model.getVisualStm()
    stm_contents = ""
    for i in (stm.getCount() - 1).downto(0)
      chunk = stm.getItem(i)
      if( !chunk.equals(model.getVisualLtm()) )
        if(!chunk.getImage().isEmpty())
          stm_contents += chunk.getImage().toString()
        end
      end
    end

    # Check if STM contents are as expected, if they are, set the flag that
    # controls when the model is ready for testing to true.
    expected_stm_contents == stm_contents ? visual_stm_contents_as_expected = true : nil
    
    expected_fixations_made = expected_fixations_made?(model, fixations_expected)

    # Advance domain time to the time that the visual-spatial field will be 
    # completely instantiated so that the model's attention will be free 
    # should a new visual-field need to be constructed.
    domain_time = model.getAttentionClock
  end
  
  ######################
  ##### SUB-TEST 1 #####
  ######################

  visual_spatial_field_as_scene_without_ghost_objects = visual_spatial_field.getAsScene(domain_time, false)
  for row in 0...visual_spatial_field.getHeight()
    for col in 0...visual_spatial_field.getWidth()

      expected_content = SceneObject.new(Scene.getBlindSquareToken(), Scene.getBlindSquareToken())
      if 
        ((row == 1 or row == 3) and (col = 2 or col == 3)) or
        (row == 2 and col != 2) or
        (row == 4 and col == 2)
      then
        expected_content = SceneObject.new(Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
      elsif row == 0 and col == 2
        expected_content = SceneObject.new(test_objects[4][0], test_objects[4][1])
      elsif row == 1 and col == 1
        expected_content = SceneObject.new(test_objects[0][0], test_objects[0][1])
      elsif row == 2 and col == 2
        expected_content = SceneObject.new(test_objects[1][0], test_objects[1][1])
      elsif row == 3 and col == 1
        expected_content = SceneObject.new(test_objects[3][0], test_objects[3][1])
      end

      contents = visual_spatial_field_as_scene_without_ghost_objects.getSquareContents(col, row)
      assert_equal(expected_content.getIdentifier(), contents.getIdentifier(), "occurred when checking identifier for object on col " + col.to_s + ", row " + row.to_s + " before object move and when ghost objects should not be returned")
      assert_equal(expected_content.getObjectClass(), contents.getObjectClass(), "occurred when checking object class for object on col " + col.to_s + ", row " + row.to_s + " before object move and when ghost objects should not be returned")
    end
  end

  ######################
  ##### SUB-TEST 2 #####
  ######################
  
  visual_spatial_field_as_scene_with_ghost_objects = visual_spatial_field.getAsScene(domain_time, true)
  for row in 0...visual_spatial_field.getHeight()
    for col in 0...visual_spatial_field.getWidth()

      expected_content = SceneObject.new(Scene.getBlindSquareToken(), Scene.getBlindSquareToken())
      if 
        ((row == 1 or row == 3) and (col = 2 or col == 3)) or
        (row == 2 and col != 2) or
        (row == 4 and col == 2)
      then
        expected_content = SceneObject.new(Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
      elsif row == 0 and col == 1
        expected_content = SceneObject.new(test_objects[2][0], test_objects[2][1])
      elsif row == 0 and col == 2
        expected_content = SceneObject.new(test_objects[4][0], test_objects[4][1])
      elsif row == 1 and col == 1
        expected_content = SceneObject.new(test_objects[0][0], test_objects[0][1])
      elsif row == 2 and col == 2
        expected_content = SceneObject.new(test_objects[1][0], test_objects[1][1])
      elsif row == 3 and col == 1
        expected_content = SceneObject.new(test_objects[3][0], test_objects[3][1])
      end

      contents = visual_spatial_field_as_scene_with_ghost_objects.getSquareContents(col, row)
      assert_equal(expected_content.getIdentifier(), contents.getIdentifier(), "occurred when checking identifier for object on col " + col.to_s + ", row " + row.to_s + " before object move and when ghost objects should be returned")
      assert_equal(expected_content.getObjectClass(), contents.getObjectClass(), "occurred when checking object class for object on col " + col.to_s + ", row " + row.to_s + " before object move and when ghost objects should be returned")
    end
  end

  ######################
  ##### SUB-TEST 3 #####
  ######################
  
  move_object_0 = ArrayList.new
  move_object_0.add(ItemSquarePattern.new(test_objects[0][0], 1, 1))
  move_object_0.add(ItemSquarePattern.new(test_objects[0][0], 2, 2))
  move_sequence = ArrayList.new
  move_sequence.add(move_object_0)
  visual_spatial_field.moveObjects(move_sequence, domain_time, false)
  domain_time = model.getAttentionClock
  visual_spatial_field_as_scene = visual_spatial_field.getAsScene(domain_time, true)

  for row in 0...visual_spatial_field.getHeight()
    for col in 0...visual_spatial_field.getWidth()

      expected_content = SceneObject.new(Scene.getBlindSquareToken(), Scene.getBlindSquareToken())
      if 
        (row == 1 and (col == 1 or col = 2 or col == 3)) or
        (row == 2 and col != 2) or
        (row == 3 and (col == 2 or col == 3)) or 
        (row == 4 and col == 2)
      then
        expected_content = SceneObject.new(Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
      elsif row == 0 and col == 1
        expected_content = SceneObject.new(test_objects[2][0], test_objects[2][1])
      elsif row == 0 and col == 2
        expected_content = SceneObject.new(test_objects[4][0], test_objects[4][1])
      elsif row == 2 and col == 2
        expected_content = SceneObject.new(test_objects[0][0], test_objects[0][1])
      elsif row == 3 and col == 1
        expected_content = SceneObject.new(test_objects[3][0], test_objects[3][1])
      end

      contents = visual_spatial_field_as_scene.getSquareContents(col, row)
      assert_equal(expected_content.getIdentifier(), contents.getIdentifier(), "occurred when checking identifier for object on col " + col.to_s + ", row " + row.to_s + " after object move")
      assert_equal(expected_content.getObjectClass(), contents.getObjectClass(), "occurred when checking object class for object on col " + col.to_s + ", row " + row.to_s + " after object move")
    end
  end

  ######################
  ##### SUB-TEST 4 #####
  ######################
  
  maximum_terminus = 0;
  vis_spatial_field = get_entire_visual_spatial_field(visual_spatial_field)
  for row in 0...visual_spatial_field.getHeight()
    for col in 0...visual_spatial_field.getWidth() 
      for object in vis_spatial_field.get(col).get(row)
        terminus = object.getTerminus()
        if terminus != nil
          terminus > maximum_terminus ? maximum_terminus = terminus : nil
        end
      end
    end
  end

  domain_time = maximum_terminus + 1
  visual_spatial_field_as_scene = visual_spatial_field.getAsScene(domain_time, true)

  for row in 0...visual_spatial_field.getHeight()
    for col in 0...visual_spatial_field.getWidth()

      expected_content = SceneObject.new(Scene.getBlindSquareToken(), Scene.getBlindSquareToken())
      if  
        (row == 0 and col == 1) or 
        ((row == 1 or row == 3) and (col = 1 or col = 2 or col == 3)) or
        row == 2 or
        (row == 4 and col == 2)
      then
        expected_content = SceneObject.new(VisualSpatialFieldObject.getUnknownSquareToken(), VisualSpatialFieldObject.getUnknownSquareToken())
      elsif row == 0 and col == 2
        expected_content = SceneObject.new(test_objects[4][0], test_objects[4][1])
      end

      contents = visual_spatial_field_as_scene.getSquareContents(col, row)
      assert_equal(expected_content.getIdentifier(), contents.getIdentifier(), "occurred when checking identifier for object on col " + col.to_s + ", row " + row.to_s + " after object move and after all object's termini have been reached")
      assert_equal(expected_content.getObjectClass(), contents.getObjectClass(), "occurred when checking object class for object on col " + col.to_s + ", row " + row.to_s + " after object move and after all object's termini have been reached")
    end
  end
end

################################################################################
# Tests for correct operation of the "VisualSpatialField.moveObjects()" function
# when moving a recognised real object in all possible scenarios.
# 
# The scene used in the following test is illustrated below ("x" represents a 
# blind square, objects are denoted by their identifiers followed by their 
# object class in parenthesis).
# 
#                  --------
# 4     x      x   |      |   x      x
#           ----------------------
# 3     x   | 2(C) |      |      |   x
#    ------------------------------------
# 2  |      |      | 1(B) |      |      |
#    ------------------------------------
# 1     x   | 0(A) |      |      |   x
#           ----------------------
# 0     x      x   |3(SLF)|   x      x
#                  --------
#       0      1      2       3      4     COORDINATES
#          
unit_test "move_object (recognised real object)" do
  
  move_types = [
    "to square containing a live blind object",
    "from/to square containing a live empty object",
    "to/from square containing the creator",
    "to/from square containing a live, unrecognised non-empty/blind/creator object",
    "to/from square containing a live, recognised non-empty/blind/creator object"
  ]
  
  for move_number in 0...move_types.count
    
    objects = [
      ["0", "A"],
      ["1", "B"],
      ["2", "C"],
      ["3", Scene.getCreatorToken()]
    ]

    scene = Scene.new("Test", 5, 5, nil)
    scene.addItemToSquare(2, 0, objects[3][0], objects[3][1])
    scene.addItemToSquare(1, 1, objects[0][0], objects[0][1])
    scene.addItemToSquare(2, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(3, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(0, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(1, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(2, 2, objects[1][0], objects[1][1])
    scene.addItemToSquare(3, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(4, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(1, 3, objects[2][0], objects[2][1])
    scene.addItemToSquare(2, 3, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(3, 3, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(2, 4, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())

    ###################################
    ##### CREATE NEW CHREST MODEL #####
    ###################################
    model = Chrest.new
    model.setDomain(GenericDomain.new(model))
    model.getPerceiver.setFieldOfView(1)

    ###########################
    ##### CHREST LEARNING #####
    ###########################

    # Set the domain time (the time against which all CHREST operations will be
    # performed in this test).
    domain_time = 0

    # Since the scene creator is present, coordinates of the object to learn must
    # be set relative to the creator's location.
    pattern = ItemSquarePattern.new(objects[0][1], -1, 1)
    list_pattern_to_learn = ListPattern.new()
    list_pattern_to_learn.add(pattern)

    recognised_chunk = model.recogniseAndLearn(list_pattern_to_learn, domain_time).getImage().toString()
    until recognised_chunk == list_pattern_to_learn.toString()
      domain_time += 1
      recognised_chunk = model.recogniseAndLearn(list_pattern_to_learn, domain_time).getImage().toString()
    end

    # Set the domain time to be the value of CHREST's learning clock since, 
    # when the visual-spatial field is constructed, the LTM of the model will 
    # contain the completely familiarised learned pattern enabling expected 
    # visual-spatial field construction due to chunk recognition retrieving 
    # the learned pattern.
    domain_time = model.getLearningClock()

    ##########################################
    ##### CONSTRUCT VISUAL-SPATIAL FIELD #####
    ##########################################

    # Set visual-spatial field variables.
    creation_time = domain_time
    number_fixations = 20
    time_to_encode_objects = 50
    time_to_encode_empty_squares = 10
    visual_spatial_field_access_time = 100
    time_to_move_object = 250
    recognised_object_lifespan = 60000
    unrecognised_object_lifespan = 30000

    creation_time = domain_time
    
    visual_stm_contents_as_expected = false
    expected_stm_contents = recognised_chunk
    
    expected_fixations_made = false
    fixations_expected = [
      [2, 0],
      [1, 1], 
      [2, 2],
      [1, 3],
    ]

    until visual_stm_contents_as_expected and expected_fixations_made do
      
      visual_stm_contents_as_expected = false
      expected_fixations_made = false

      creation_time = domain_time

      # Construct the visual-spatial field.
      visual_spatial_field = VisualSpatialField.new(
        model,
        scene, 
        time_to_encode_objects,
        time_to_encode_empty_squares,
        visual_spatial_field_access_time, 
        time_to_move_object, 
        recognised_object_lifespan,
        unrecognised_object_lifespan,
        number_fixations,
        domain_time,
        true,
        false
      )

      # Get contents of STM (will have been populated during object 
      # recognition during visual-spatial field construction) and remove root 
      # nodes and nodes with empty images.  This will leave retrieved chunks 
      # that have non-empty images, i.e. these images should contain the 
      # list-patterns learned by the model.
      stm = model.getVisualStm()
      stm_contents = ""
      for i in (stm.getCount() - 1).downto(0)
        chunk = stm.getItem(i)
        if( !chunk.equals(model.getVisualLtm()) )
          if(!chunk.getImage().isEmpty())
            stm_contents += chunk.getImage().toString()
          end
        end
      end

      # Check if STM contents are as expected, if they are, set the flag that
      # controls when the model is ready for testing to true.
      expected_stm_contents == stm_contents ? visual_stm_contents_as_expected = true : nil
      
      expected_fixations_made = expected_fixations_made?(model, fixations_expected)

      # Advance domain time to the time that the visual-spatial field will be 
      # completely instantiated so that the model's attention will be free 
      # should a new visual-field need to be constructed.
      domain_time = model.getAttentionClock
    end

    ####################################################################
    ##### SET-UP EXPECTED VISUAL-SPATIAL FIELD COORDINATE CONTENTS #####
    ####################################################################

    expected_visual_spatial_field_object_properties = Array.new
    for col in 0...visual_spatial_field.getSceneEncoded().getWidth()
      expected_visual_spatial_field_object_properties.push([])
      for row in 0...visual_spatial_field.getSceneEncoded().getHeight()
        expected_visual_spatial_field_object_properties[col].push([])

        if (col == 2 and row == 0)
          expected_visual_spatial_field_object_properties[col][row].push([
            objects[3][0],
            objects[3][1],
            creation_time + visual_spatial_field_access_time,
            nil,
            false,
            false
          ])
        else
          expected_visual_spatial_field_object_properties[col][row].push([
            Scene.getBlindSquareToken(),
            Scene.getBlindSquareToken(),
            creation_time + visual_spatial_field_access_time,
            nil,
            false,
            false
          ])
        end
      end
    end

    number_objects_encoded = 1
    number_empty_squares_encoded = 0

    # Set expected object values for coordinates containing recognised object.
    expected_visual_spatial_field_object_properties[1][1][0][3] = get_creation_time_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_encoded, number_empty_squares_encoded)
    expected_visual_spatial_field_object_properties[1][1].push([
      objects[0][0],
      objects[0][1],
      expected_visual_spatial_field_object_properties[1][1][0][3],
      expected_visual_spatial_field_object_properties[1][1][0][3] + recognised_object_lifespan,
      true,
      false
    ])

    # Set expected object values for coordinates containing unrecognised objects.
    for row in 0...visual_spatial_field.getHeight()
      for col in 0...visual_spatial_field.getWidth()

        process_coordinates = false
        identifier = Scene.getEmptySquareToken()
        obj_class = Scene.getEmptySquareToken()

        if 
          ( (row == 1 or row == 3) and (col == 2 or col == 3) ) or
          (row == 2 and (col != 2)) or
          (row == 4 and (col == 2))
        then
          number_empty_squares_encoded += 1
          process_coordinates = true

        elsif(row == 2 and col == 2) or (row == 3 and col == 1)
          number_objects_encoded += 1
          process_coordinates = true

          if (row == 2 and col == 2)
            identifier = objects[1][0]
            obj_class = objects[1][1]
          else
            identifier = objects[2][0]
            obj_class = objects[2][1]
          end
        end

        if(process_coordinates)
          expected_visual_spatial_field_object_properties[col][row][0][3] = get_creation_time_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_encoded, number_empty_squares_encoded)
          expected_visual_spatial_field_object_properties[col][row].push([
            identifier,
            obj_class,
            expected_visual_spatial_field_object_properties[col][row][0][3],
            expected_visual_spatial_field_object_properties[col][row][0][3] + unrecognised_object_lifespan,
            false,
            false,
          ])
        end
      end
    end
    
    check_visual_spatial_field_against_expected(
      visual_spatial_field,
      expected_visual_spatial_field_object_properties,
      model.getAttentionClock(),
      "when checking initial state of visual-spatial field"
    )
    
    ############################################################################
    if (move_number == 0)
      # ================
      # MOVE DESCRIPTION
      # ================
      # - Move to square that has a live blind object on it.
      # - Moves performed:
      #   + Object 0 moved from (1, 1) to (1, 0).
      # 
      # ===============
      # EXPECTED OUTPUT
      # ===============
      # - Object 0 on (1, 1) should have terminus modified
      # - Empty square object should be placed on (1, 1)
      # - Objects on coordinates (1, 0) should not be altered.
      # - Attention clock of model should be set to the time that object 0 is
      #   "put-down" on (1, 0).
      # - Expected visual-spatial field state:
      # 
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) |      |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 1))
      move.add(ItemSquarePattern.new(objects[0][0], 1, 0))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters.
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 1)
      expected_visual_spatial_field_object_properties[1][1][1][3] = pickup_time
      
      # New empty square object should be added to (1, 1) when 0 picked up.
      expected_visual_spatial_field_object_properties[1][1].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
      
      # Objects on (1, 0) should not be modified.
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after moving " + move_types[move_number] + 
        " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after moving " + move_types[move_number] + 
        " (move number: " + move_number.to_s + ")"
      )
    
    ############################################################################
    elsif (move_number == 1)
      # ================
      # MOVE DESCRIPTION
      # ================
      # - Move to square that has a live empty object on it.
      # - Move from square that was empty and has no other live objects on it
      #   after object move.
      # - Move(s) performed:
      #   + Object 0 moved from (1, 1) to (1, 2).
      #   + Object 0 moved from (1, 2) to (3, 2)
      # - In between moves, object 0's recognised status will be manually set
      #   to true to ensure that a recognised object is being moved (object 0
      #   will become unrecognised after first part of move).
      # 
      # ===============
      # EXPECTED OUTPUT
      # ===============
      # 
      # - First move
      #   + Object 0 on (1, 1) should have terminus set to time when object 0 is
      #     "picked-up".
      #   + Empty square object should be placed on (1, 1) when object 0 is 
      #     "picked-up".
      #   + Empty square object on (1, 2) should have terminus set to time when
      #     object 0 is "put-down".
      #   + Object 0 should be added to (1, 2) at time of put-down but should no
      #     longer be recognised.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (1, 2).
      #   + Expected visual-spatial field state:
      #	
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      | 0(A) | 1(B) |      |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      #
      # - Second move
      #   + Object 0 on (1, 2) should have terminus set to time when object 0 is
      #     "picked-up".
      #   + Empty square object should be placed on (1, 2) when object 0 is 
      #     "picked-up".
      #   + Empty square object on (3, 2) should have terminus set to time when
      #     object 0 is "put-down".
      #   + Object 0 should be added to (3, 2) at time of put-down but should no
      #     longer be recognised.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (3, 2).
      #   + Expected visual-spatial field state:
      #	
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) | 0(A) |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      
      ######################
      ##### FIRST MOVE #####
      ######################
      
      # Construct move
      move = ArrayList.new()
      move.add(ItemSquarePattern.new(objects[0][0], 1, 1))
      move.add(ItemSquarePattern.new(objects[0][0], 1, 2))
      move_sequence = ArrayList.new()
      move_sequence.add(move)
      
      # Set relevant timing parameters.
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 1)
      expected_visual_spatial_field_object_properties[1][1][1][3] = pickup_time
      
      # New empty square object should be added to (1, 1) when 0 picked up.
      expected_visual_spatial_field_object_properties[1][1].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      # Set terminus for empty square on (1, 2)
      expected_visual_spatial_field_object_properties[1][2][1][3] = putdown_time
      
      # Object 0 should be added to (1, 2) at putdown time.  Should no longer be
      # recognised.
      expected_visual_spatial_field_object_properties[1][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        false
      ])
      
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      
      #######################
      ##### SECOND MOVE #####
      #######################
      
      # Artificially make object 0 recognised again
      vsf = get_entire_visual_spatial_field(visual_spatial_field)
      vsf.get(1).get(2).get(2).setRecognised(putdown_time, true)
      
      # Set recognised status and terminus of object 0 on (1, 2)
      expected_visual_spatial_field_object_properties[1][2][2][3] = putdown_time + recognised_object_lifespan
      expected_visual_spatial_field_object_properties[1][2][2][4] = true
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 2))
      move.add(ItemSquarePattern.new(objects[0][0], 3, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 2)
      expected_visual_spatial_field_object_properties[1][2][2][3] = pickup_time
      
      # Add empty square to (1, 2)
      expected_visual_spatial_field_object_properties[1][2].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      # Set terminus for empty square on (3, 2)
      expected_visual_spatial_field_object_properties[3][2][1][3] = putdown_time
      
      # Add object 0 to (3, 2)
      expected_visual_spatial_field_object_properties[3][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after moving " + move_types[move_number] + 
        " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after moving " + move_types[move_number] + 
        " (move number: " + move_number.to_s + ")"
      )
      
    ############################################################################
    elsif (move_number == 2)
      # ================
      # MOVE DESCRIPTION
      # ================
      # - Move to square that has the creator on it.
      # - Move from square that has the creator on it.
      # - Move(s) performed:
      #   + Object 0 moved from (1, 1) to (2, 0).
      #   + Object 0 moved from (2, 0) to (3, 2).
      # - In between moves, object 0's recognised status will be manually set
      #   to true.
      # 
      # ===============
      # EXPECTED OUTPUT
      # ===============
      # - After first move
      #   + Object 0 on (1, 1) should have terminus set to time when it is
      #     "picked-up".
      #   + Empty square object should be placed on (1, 1) when object 0 is 
      #     "picked-up".
      #   + Object 0 should be added to (2, 0) at time of "put-down" but should 
      #     no longer be recognised.
      #   + The creator object on (2, 0) should not be modified.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (2, 0).
      #   + Expected visual-spatial field state:
      #     
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) |      |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   | 0(A) |   x      x
      #                  |3(SLF)|
      #                  --------
      #       0      1      2       3      4     COORDINATES
      #     
      # - After second move
      #   + Object 0 on (2, 0) should have terminus set to time when it is 
      #     "picked-up".
      #   + The other objects on (2, 0) should not be modified.
      #   + The empty square on (3, 2) should have its terminus set to the time
      #     object 0 is "put-down".
      #   + Object 0 should be added to (3, 2).  Its creation time should be set
      #     to the time it is "put-down" and it should still be unrecognised.
      #   + Expected visual-spatial field state:
      #
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) | 0(A) |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
            
      ##############################
      ##### FIRST PART OF MOVE #####
      ##############################
      
      # Construct move.
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 1))
      move.add(ItemSquarePattern.new(objects[0][0], 2, 0))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters.
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 1)
      expected_visual_spatial_field_object_properties[1][1][1][3] = pickup_time
      
      # New empty square object should be added to (1, 1) when 0 picked up.
      expected_visual_spatial_field_object_properties[1][1].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
      
      # Object 0 should be added to (2, 0) at first putdown time.  Should no 
      # longer be recognised.
      expected_visual_spatial_field_object_properties[2][0].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      # Do not modify anything about the creator.
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      
      ###############################
      ##### SECOND PART OF MOVE #####
      ###############################
      
      # Artificially make object 0 recognised again
      vsf = get_entire_visual_spatial_field(visual_spatial_field)
      vsf.get(2).get(0).get(1).setRecognised(putdown_time, true)
      
      # Set recognised status and terminus of object 0 on (2, 0)
      expected_visual_spatial_field_object_properties[2][0][1][3] = putdown_time + recognised_object_lifespan
      expected_visual_spatial_field_object_properties[2][0][1][4] = true
      
      # Construct move.
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 2, 0))
      move.add(ItemSquarePattern.new(objects[0][0], 3, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant time parameters.
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (2, 0)
      expected_visual_spatial_field_object_properties[2][0][1][3] = pickup_time
      
      # Do not modify anything about the creator.
      
      # Set terminus for empty square object on (3, 2)
      expected_visual_spatial_field_object_properties[3][2][1][3] = putdown_time
      
      # Add object 1 to (3, 2)
      expected_visual_spatial_field_object_properties[3][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      
    ############################################################################
    elsif(move_number == 3)
      #	================
      # MOVE DESCRIPTION
      # ================
      #	- Move to square that has a live, unrecognised object on it.
      #	- Move from square that has a live, unrecognised object on it.
      # - Move(s) performed:
      #   + Object 0 moved from (1, 1) to (2, 2).
      #   + Object 0 moved from (2, 2) to (3, 2).
      # - In between moves, object 0's recognised status will be manually set
      #   to true.
      #	
      #	===============
      # EXPECTED OUTPUT
      # ===============
      # - After first move
      #   + Object 0 on (1, 1) should have terminus set to time when it is
      #     "picked-up".
      #   + Empty square object should be placed on (1, 1) when object 0 is 
      #     "picked-up".
      #   + Object 0 should be added to (2, 2) at time of "put-down" but should 
      #     no longer be recognised.
      #   + Object 1 on (2, 2) should have its terminus extended to the
      #     time object 0 is "put-down" plus the lifespan for an unrecognised
      #     object.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (2, 2).
      #   + Expected visual-spatial field state:
      #   
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 0(A) |      |      |
      #    |      |      | 1(B) |      |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      #       
      # - After second move
      #   + Object 0 on (2, 2) should have terminus set to time when it is
      #     "picked-up".
      #   + Terminus for object 1 should be extended to the time object 0 is
      #     "picked-up" plus the lifespan specified for unrecognised objects.
      #   + The empty square object on (3, 2) should have its terminus set to
      #     the time that object 0 is "put-down".
      #   + Object 0 should be added to (3, 2) at time of "put-down" and should 
      #     not be recognised.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (3, 2).
      #   + Expected visual-spatial field state:
      #
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) | 0(A) |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      
      ##############################
      ##### FIRST PART OF MOVE #####
      ##############################
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 1))
      move.add(ItemSquarePattern.new(objects[0][0], 2, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant time parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 1)
      expected_visual_spatial_field_object_properties[1][1][1][3] = pickup_time
      
      # New empty square object should be added to (1, 1) when 0 picked up.
      expected_visual_spatial_field_object_properties[1][1].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
      
      # Object 0 should be added to (2, 2) at first putdown time.  Should no 
      # longer be recognised.
      expected_visual_spatial_field_object_properties[2][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      # Update terminus for object 1 on (2, 2) since the coordinates have been
      # looked at and the object is alive when 0 is putdown.
      expected_visual_spatial_field_object_properties[2][2][1][3] = putdown_time + unrecognised_object_lifespan
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      ###############################
      ##### SECOND PART OF MOVE #####
      ###############################
      
      # Make object 0 recognised again
      vsf = get_entire_visual_spatial_field(visual_spatial_field)
      vsf.get(2).get(2).get(2).setRecognised(putdown_time, true)
      
      # Set recognised status and terminus of object 0 on (2, 2)
      expected_visual_spatial_field_object_properties[2][2][2][3] = putdown_time + recognised_object_lifespan
      expected_visual_spatial_field_object_properties[2][2][2][4] = true
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 2, 2))
      move.add(ItemSquarePattern.new(objects[0][0], 3, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (2, 2)
      expected_visual_spatial_field_object_properties[2][2][2][3] = pickup_time
      
      # Set terminus for object 1 on (2, 2)
      expected_visual_spatial_field_object_properties[2][2][1][3] = pickup_time + unrecognised_object_lifespan
      
      # Set terminus for empty square object on (3, 2)
      expected_visual_spatial_field_object_properties[3][2][1][3] = putdown_time
      
      # Add object 0 to (3, 2)
      expected_visual_spatial_field_object_properties[3][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
    
    ############################################################################
    elsif(move_number == 4)
      #	================
      # MOVE DESCRIPTION
      # ================
      #	- Move to square that has a live, recognised object on it.
      #	- Move from square that has a live, recognised object on it.
      # - Move(s) performed:
      #   + Object 0 moved from (1, 1) to (1, 3).
      #   + Object 0 moved from (1, 3) to (3, 2).
      # - In between moves, object 0's recognised status will be manually set
      #   to true.
      # - Object 2's recognised status will be set manually.
      #	
      #	===============
      # EXPECTED OUTPUT
      # ===============
      # - After first move
      #   + Object 0 on (1, 1) should have terminus set to time when it is
      #     "picked-up".
      #   + Empty square object should be placed on (1, 1) when object 0 is 
      #     "picked-up".
      #   + Object 0 should be added to (1, 3) at time of "put-down" but should 
      #     no longer be recognised.
      #   + Object 2 on (2, 2) should have its terminus extended to the
      #     time object 0 is "put-down" plus the lifespan for a recognised
      #     object.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (2, 2).
      #   + Expected visual-spatial field state:
      #   
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 0(A) |      |      |   x
      #           | 2(C) |      |      |
      #    ------------------------------------
      # 2  |      |      | 1(B) |      |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      #       
      # - After second move
      #   + Object 0 on (1, 3) should have terminus set to time when it is
      #     "picked-up".
      #   + Terminus for object 2 should be extended to the time object 0 is
      #     "picked-up" plus the lifespan specified for recognised objects.
      #   + The empty square object on (3, 2) should have its terminus set to
      #     the time that object 0 is "put-down".
      #   + Object 0 should be added to (3, 2) at time of "put-down" and should 
      #     not be recognised.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (3, 2).
      #   + Expected visual-spatial field state:
      #
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) | 0(A) |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      
      vsf = get_entire_visual_spatial_field(visual_spatial_field)
      vsf.get(1).get(3).get(1).setRecognised(model.getAttentionClock(), true)
      expected_visual_spatial_field_object_properties[1][3][1][3] = model.getAttentionClock() + recognised_object_lifespan
      expected_visual_spatial_field_object_properties[1][3][1][4] = true
      
      ##############################
      ##### FIRST PART OF MOVE #####
      ##############################
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 1))
      move.add(ItemSquarePattern.new(objects[0][0], 1, 3))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant time parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 1)
      expected_visual_spatial_field_object_properties[1][1][1][3] = pickup_time
      
      # New empty square object should be added to (1, 1) when 0 picked up.
      expected_visual_spatial_field_object_properties[1][1].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
      
      # Object 0 should be added to (1, 3) at first putdown time.  Should no 
      # longer be recognised.
      expected_visual_spatial_field_object_properties[1][3].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      # Update terminus for object 2 on (1, 3) since the coordinates have been
      # looked at and the object is alive when 0 is putdown.
      expected_visual_spatial_field_object_properties[1][3][1][3] = putdown_time + recognised_object_lifespan
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      ###############################
      ##### SECOND PART OF MOVE #####
      ###############################
      
      # Make object 0 recognised again
      vsf = get_entire_visual_spatial_field(visual_spatial_field)
      vsf.get(1).get(3).get(2).setRecognised(putdown_time, true)
      
      # Set recognised status and terminus of object 0 on (1, 3)
      expected_visual_spatial_field_object_properties[1][3][2][3] = putdown_time + recognised_object_lifespan
      expected_visual_spatial_field_object_properties[1][3][2][4] = true
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 3))
      move.add(ItemSquarePattern.new(objects[0][0], 3, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 3)
      expected_visual_spatial_field_object_properties[1][3][2][3] = pickup_time
      
      # Set terminus for object 2 on (1, 3)
      expected_visual_spatial_field_object_properties[1][3][1][3] = pickup_time + recognised_object_lifespan
      
      # Set terminus for empty square object on (3, 2)
      expected_visual_spatial_field_object_properties[3][2][1][3] = putdown_time
      
      # Add object 0 to (3, 2)
      expected_visual_spatial_field_object_properties[3][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
    end 
  end 
end

################################################################################
# Tests for correct operation of the "VisualSpatialField.moveObjects()" function
# when moving an unrecognised real object in all possible scenarios.
# 
# The scene used in the following test is illustrated below ("x" represents a 
# blind square, real objects are denoted by their identifiers and their class 
# are in parenthesis, ghost objects are denoted by lower case letters in 
# parenthesis).
# 
#                  --------
# 4     x      x   |      |   x      x
#           ----------------------
# 3     x   | 2(C) |      |      |   x
#    ------------------------------------
# 2  |      |      | 1(B) |      |      |
#    ------------------------------------
# 1     x   | 0(A) |      |      |   x
#           ----------------------
# 0     x      x   |3(SLF)|   x      x
#                  --------
#       0      1      2       3      4     COORDINATES
#          
unit_test "move_object (unrecognised real object)" do
  
  move_types = [
    "to square containing a live blind object",
    "from/to square containing a live empty object",
    "to/from square containing the creator",
    "to/from square containing a live unrecognised object",
    "to/from square containing a live recognised object"
  ]
  
  for move_number in 0...move_types.count
    
    objects = [
      ["0", "A"],
      ["1", "B"],
      ["2", "C"],
      ["3", Scene.getCreatorToken()]
    ]

    scene = Scene.new("Test", 5, 5, nil)
    scene.addItemToSquare(2, 0, objects[3][0], objects[3][1])
    scene.addItemToSquare(1, 1, objects[0][0], objects[0][1])
    scene.addItemToSquare(2, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(3, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(0, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(1, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(2, 2, objects[1][0], objects[1][1])
    scene.addItemToSquare(3, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(4, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(1, 3, objects[2][0], objects[2][1])
    scene.addItemToSquare(2, 3, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(3, 3, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(2, 4, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())

    ###################################
    ##### CREATE NEW CHREST MODEL #####
    ###################################
    model = Chrest.new
    model.setDomain(GenericDomain.new(model))
    model.getPerceiver.setFieldOfView(1)

    ##########################################
    ##### CONSTRUCT VISUAL-SPATIAL FIELD #####
    ##########################################
    
    expected_fixations_made = false
    fixations_expected = [
      [2, 0],
      [1, 1], 
      [2, 2],
      [1, 3],
    ]
    
    until expected_fixations_made
      # Set visual-spatial field variables.
      creation_time = 0
      number_fixations = 20
      time_to_encode_objects = 50
      time_to_encode_empty_squares = 10
      visual_spatial_field_access_time = 100
      time_to_move_object = 250
      recognised_object_lifespan = 60000
      unrecognised_object_lifespan = 30000

      visual_spatial_field = VisualSpatialField.new(
        model,
        scene, 
        time_to_encode_objects,
        time_to_encode_empty_squares,
        visual_spatial_field_access_time, 
        time_to_move_object, 
        recognised_object_lifespan,
        unrecognised_object_lifespan,
        number_fixations,
        creation_time,
        true,
        false
      )
      
      expected_fixations_made = expected_fixations_made?(model, fixations_expected)
    end

    ####################################################################
    ##### SET-UP EXPECTED VISUAL-SPATIAL FIELD COORDINATE CONTENTS #####
    ####################################################################

    expected_visual_spatial_field_object_properties = Array.new
    for col in 0...visual_spatial_field.getSceneEncoded().getWidth()
      expected_visual_spatial_field_object_properties.push([])
      for row in 0...visual_spatial_field.getSceneEncoded().getHeight()
        expected_visual_spatial_field_object_properties[col].push([])

        if (col == 2 and row == 0)
          expected_visual_spatial_field_object_properties[col][row].push([
            objects[3][0],
            objects[3][1],
            creation_time + visual_spatial_field_access_time,
            nil,
            false,
            false
          ])
        else
          expected_visual_spatial_field_object_properties[col][row].push([
            Scene.getBlindSquareToken(),
            Scene.getBlindSquareToken(),
            creation_time + visual_spatial_field_access_time,
            nil,
            false,
            false
          ])
        end
      end
    end

    number_objects_encoded = 0
    number_empty_squares_encoded = 0

    # Set expected object values for coordinates containing unrecognised objects.
    for row in 0...visual_spatial_field.getHeight()
      for col in 0...visual_spatial_field.getWidth()

        process_coordinates = false
        identifier = Scene.getEmptySquareToken()
        obj_class = Scene.getEmptySquareToken()

        if 
          ( (row == 1 or row == 3) and (col == 2 or col == 3) ) or
          (row == 2 and (col != 2)) or
          (row == 4 and (col == 2))
        then
          number_empty_squares_encoded += 1
          process_coordinates = true

        elsif(row == 2 and col == 2) or (row == 1 and col == 1) or (row == 3 and col == 1)
          number_objects_encoded += 1
          process_coordinates = true

          if (row == 2 and col == 2)
            identifier = objects[1][0]
            obj_class = objects[1][1]
          elsif (row == 1 and col == 1)
            identifier = objects[0][0]
            obj_class = objects[0][1]
          else
            identifier = objects[2][0]
            obj_class = objects[2][1]
          end
        end

        if(process_coordinates)
          expected_visual_spatial_field_object_properties[col][row][0][3] = get_creation_time_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_encoded, number_empty_squares_encoded)
          expected_visual_spatial_field_object_properties[col][row].push([
            identifier,
            obj_class,
            expected_visual_spatial_field_object_properties[col][row][0][3],
            expected_visual_spatial_field_object_properties[col][row][0][3] + unrecognised_object_lifespan,
            false,
            false,
          ])
        end
      end
    end

    check_visual_spatial_field_against_expected(
      visual_spatial_field,
      expected_visual_spatial_field_object_properties,
      model.getAttentionClock(),
      "when checking initial state of visual-spatial field"
    )
    
    ############################################################################
    if (move_number == 0)
      # ================
      # MOVE DESCRIPTION
      # ================
      # - Move to square that has a live blind object on it.
      # - Moves performed:
      #   + Object 0 moved from (1, 1) to (1, 0).
      # 
      # ===============
      # EXPECTED OUTPUT
      # ===============
      # - Object 0 on (1, 1) should have terminus modified
      # - Empty square object should be placed on (1, 1)
      # - Objects on coordinates (1, 0) should not be altered.
      # - Attention clock of model should be set to the time that object 0 is
      #   "put-down" on (1, 0).
      # - Expected visual-spatial field state:
      # 
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) |      |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 1))
      move.add(ItemSquarePattern.new(objects[0][0], 1, 0))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters.
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 1)
      expected_visual_spatial_field_object_properties[1][1][1][3] = pickup_time
      
      # New empty square object should be added to (1, 1) when 0 picked up.
      expected_visual_spatial_field_object_properties[1][1].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
      
      # Objects on (1, 0) should not be modified.
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after moving " + move_types[move_number] + 
        " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after moving " + move_types[move_number] + 
        " (move number: " + move_number.to_s + ")"
      )
    
    ############################################################################
    elsif (move_number == 1)
      # ================
      # MOVE DESCRIPTION
      # ================
      # - Move to square that has a live empty object on it.
      # - Move from square that was empty and has no other live objects on it
      #   after object move.
      # - Move(s) performed:
      #   + Object 0 moved from (1, 1) to (1, 2).
      #   + Object 0 moved from (1, 2) to (3, 2)
      # 
      # ===============
      # EXPECTED OUTPUT
      # ===============
      # 
      # - First move
      #   + Object 0 on (1, 1) should have terminus set to time when object 0 is
      #     "picked-up".
      #   + Empty square object should be placed on (1, 1) when object 0 is 
      #     "picked-up".
      #   + Empty square object on (1, 2) should have terminus set to time when
      #     object 0 is "put-down".
      #   + Object 0 should be added to (1, 2) at time of put-down.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (1, 2).
      #   + Expected visual-spatial field state:
      #	
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      | 0(A) | 1(B) |      |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      #
      # - Second move
      #   + Object 0 on (1, 2) should have terminus set to time when object 0 is
      #     "picked-up".
      #   + Empty square object should be placed on (1, 2) when object 0 is 
      #     "picked-up".
      #   + Empty square object on (3, 2) should have terminus set to time when
      #     object 0 is "put-down".
      #   + Object 0 should be added to (3, 2) at time of put-down.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (3, 2).
      #   + Expected visual-spatial field state:
      #	
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) | 0(A) |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      
      ######################
      ##### FIRST MOVE #####
      ######################
      
      # Construct move
      move = ArrayList.new()
      move.add(ItemSquarePattern.new(objects[0][0], 1, 1))
      move.add(ItemSquarePattern.new(objects[0][0], 1, 2))
      move_sequence = ArrayList.new()
      move_sequence.add(move)
      
      # Set relevant timing parameters.
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 1)
      expected_visual_spatial_field_object_properties[1][1][1][3] = pickup_time
      
      # New empty square object should be added to (1, 1) when 0 picked up.
      expected_visual_spatial_field_object_properties[1][1].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      # Set terminus for empty square on (1, 2)
      expected_visual_spatial_field_object_properties[1][2][1][3] = putdown_time
      
      # Object 0 should be added to (1, 2) at putdown time.
      expected_visual_spatial_field_object_properties[1][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        false
      ])
      
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      
      #######################
      ##### SECOND MOVE #####
      #######################
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 2))
      move.add(ItemSquarePattern.new(objects[0][0], 3, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 2)
      expected_visual_spatial_field_object_properties[1][2][2][3] = pickup_time
      
      # Add empty square to (1, 2)
      expected_visual_spatial_field_object_properties[1][2].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      # Set terminus for empty square on (3, 2)
      expected_visual_spatial_field_object_properties[3][2][1][3] = putdown_time
      
      # Add object 0 to (3, 2)
      expected_visual_spatial_field_object_properties[3][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after moving " + move_types[move_number] + 
        " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after moving " + move_types[move_number] + 
        " (move number: " + move_number.to_s + ")"
      )
      
    ############################################################################
    elsif (move_number == 2)
      # ================
      # MOVE DESCRIPTION
      # ================
      # - Move to square that has the creator on it.
      # - Move from square that has the creator on it.
      # - Move(s) performed:
      #   + Object 0 moved from (1, 1) to (2, 0).
      #   + Object 0 moved from (2, 0) to (3, 2).
      # 
      # ===============
      # EXPECTED OUTPUT
      # ===============
      # - After first move
      #   + Object 0 on (1, 1) should have terminus set to time when it is
      #     "picked-up".
      #   + Empty square object should be placed on (1, 1) when object 0 is 
      #     "picked-up".
      #   + Object 0 should be added to (2, 0) at time of "put-down".
      #   + The creator object on (2, 0) should not be modified.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (2, 0).
      #   + Expected visual-spatial field state:
      #     
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) |      |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   | 0(A) |   x      x
      #                  |3(SLF)|
      #                  --------
      #       0      1      2       3      4     COORDINATES
      #     
      # - After second move
      #   + Object 0 on (2, 0) should have terminus set to time when it is 
      #     "picked-up".
      #   + The other objects on (2, 0) should not be modified.
      #   + The empty square on (3, 2) should have its terminus set to the time
      #     object 0 is "put-down".
      #   + Object 0 should be added to (3, 2).  Its creation time should be set
      #     to the time it is "put-down".
      #   + Expected visual-spatial field state:
      #
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) | 0(A) |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
            
      ##############################
      ##### FIRST PART OF MOVE #####
      ##############################
      
      # Construct move.
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 1))
      move.add(ItemSquarePattern.new(objects[0][0], 2, 0))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters.
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 1)
      expected_visual_spatial_field_object_properties[1][1][1][3] = pickup_time
      
      # New empty square object should be added to (1, 1) when 0 picked up.
      expected_visual_spatial_field_object_properties[1][1].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
      
      # Object 0 should be added to (2, 0) at first putdown time.
      expected_visual_spatial_field_object_properties[2][0].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      # Do not modify anything about the creator.
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      
      ###############################
      ##### SECOND PART OF MOVE #####
      ###############################
      
      # Construct move.
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 2, 0))
      move.add(ItemSquarePattern.new(objects[0][0], 3, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant time parameters.
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (2, 0)
      expected_visual_spatial_field_object_properties[2][0][1][3] = pickup_time
      
      # Do not modify anything about the creator.
      
      # Set terminus for empty square object on (3, 2)
      expected_visual_spatial_field_object_properties[3][2][1][3] = putdown_time
      
      # Add object 1 to (3, 2)
      expected_visual_spatial_field_object_properties[3][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      
    ############################################################################
    elsif(move_number == 3)
      #	================
      # MOVE DESCRIPTION
      # ================
      #	- Move to square that has alive object on it that is not a blind square,
      #	  an empty square or the creator.
      #	- Move from square that has alive object on it that is not a blind 
      #   square, an empty square or the creator.
      # - Move(s) performed:
      #   + Object 0 moved from (1, 1) to (2, 2).
      #   + Object 0 moved from (2, 2) to (3, 2).
      #	
      #	===============
      # EXPECTED OUTPUT
      # ===============
      # - After first move
      #   + Object 0 on (1, 1) should have terminus set to time when it is
      #     "picked-up".
      #   + Empty square object should be placed on (1, 1) when object 0 is 
      #     "picked-up".
      #   + Object 0 should be added to (2, 2) at time of "put-down".
      #   + Object 1 on (2, 2) should have its terminus extended to the
      #     time object 0 is "put-down" plus the lifespan for an unrecognised
      #     object.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (2, 2).
      #   + Expected visual-spatial field state:
      #   
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 0(A) |      |      |
      #    |      |      | 1(B) |      |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      #       
      # - After second move
      #   + Object 0 on (2, 2) should have terminus set to time when it is
      #     "picked-up".
      #   + Terminus for object 1 should be extended to the time object 0 is
      #     "picked-up" plus the lifespan specified for unrecognised objects.
      #   + The empty square object on (3, 2) should have its terminus set to
      #     the time that object 0 is "put-down".
      #   + Object 0 should be added to (3, 2) at time of "put-down".
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (3, 2).
      #   + Expected visual-spatial field state:
      #
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) | 0(A) |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      
      ##############################
      ##### FIRST PART OF MOVE #####
      ##############################
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 1))
      move.add(ItemSquarePattern.new(objects[0][0], 2, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant time parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 1)
      expected_visual_spatial_field_object_properties[1][1][1][3] = pickup_time
      
      # New empty square object should be added to (1, 1) when 0 picked up.
      expected_visual_spatial_field_object_properties[1][1].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
      
      # Object 0 should be added to (2, 2) at first putdown time.  Should no 
      # longer be recognised.
      expected_visual_spatial_field_object_properties[2][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      # Update terminus for object 1 on (2, 2) since the coordinates have been
      # looked at and the object is alive when 0 is putdown.
      expected_visual_spatial_field_object_properties[2][2][1][3] = putdown_time + unrecognised_object_lifespan
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      ###############################
      ##### SECOND PART OF MOVE #####
      ###############################
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 2, 2))
      move.add(ItemSquarePattern.new(objects[0][0], 3, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (2, 2)
      expected_visual_spatial_field_object_properties[2][2][2][3] = pickup_time
      
      # Set terminus for object 1 on (2, 2)
      expected_visual_spatial_field_object_properties[2][2][1][3] = pickup_time + unrecognised_object_lifespan
      
      # Set terminus for empty square object on (3, 2)
      expected_visual_spatial_field_object_properties[3][2][1][3] = putdown_time
      
      # Add object 0 to (3, 2)
      expected_visual_spatial_field_object_properties[3][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      
    ############################################################################
    elsif(move_number == 4)
      #	================
      # MOVE DESCRIPTION
      # ================
      #	- Move to square that has a live, recognised object on it.
      #	- Move from square that has a live, recognised object on it.
      # - Move(s) performed:
      #   + Object 0 moved from (1, 1) to (1, 3).
      #   + Object 0 moved from (1, 3) to (3, 2).
      # - Object 2's recognised status will be set manually.
      #	
      #	===============
      # EXPECTED OUTPUT
      # ===============
      # - After first move
      #   + Object 0 on (1, 1) should have terminus set to time when it is
      #     "picked-up".
      #   + Empty square object should be placed on (1, 1) when object 0 is 
      #     "picked-up".
      #   + Object 0 should be added to (1, 3) at time of "put-down".
      #   + Object 2 on (2, 2) should have its terminus extended to the
      #     time object 0 is "put-down" plus the lifespan for a recognised
      #     object.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (2, 2).
      #   + Expected visual-spatial field state:
      #   
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 0(A) |      |      |   x
      #           | 2(C) |      |      |
      #    ------------------------------------
      # 2  |      |      | 1(B) |      |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      #       
      # - After second move
      #   + Object 0 on (1, 3) should have terminus set to time when it is
      #     "picked-up".
      #   + Terminus for object 2 should be extended to the time object 0 is
      #     "picked-up" plus the lifespan specified for recognised objects.
      #   + The empty square object on (3, 2) should have its terminus set to
      #     the time that object 0 is "put-down".
      #   + Object 0 should be added to (3, 2) at time of "put-down".
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (3, 2).
      #   + Expected visual-spatial field state:
      #
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) | 0(A) |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      vsf = get_entire_visual_spatial_field(visual_spatial_field)
      vsf.get(1).get(3).get(1).setRecognised(model.getAttentionClock(), true)
      expected_visual_spatial_field_object_properties[1][3][1][3] = model.getAttentionClock() + recognised_object_lifespan
      expected_visual_spatial_field_object_properties[1][3][1][4] = true
      
      ##############################
      ##### FIRST PART OF MOVE #####
      ##############################
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 1))
      move.add(ItemSquarePattern.new(objects[0][0], 1, 3))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant time parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 1)
      expected_visual_spatial_field_object_properties[1][1][1][3] = pickup_time
      
      # New empty square object should be added to (1, 1) when 0 picked up.
      expected_visual_spatial_field_object_properties[1][1].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
      
      # Object 0 should be added to (1, 3) at first putdown time.
      expected_visual_spatial_field_object_properties[1][3].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      # Update terminus for object 2 on (1, 3) since the coordinates have been
      # looked at and the object is alive when 0 is putdown.
      expected_visual_spatial_field_object_properties[1][3][1][3] = putdown_time + recognised_object_lifespan
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      ###############################
      ##### SECOND PART OF MOVE #####
      ###############################
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 3))
      move.add(ItemSquarePattern.new(objects[0][0], 3, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 3)
      expected_visual_spatial_field_object_properties[1][3][2][3] = pickup_time
      
      # Set terminus for object 2 on (1, 3)
      expected_visual_spatial_field_object_properties[1][3][1][3] = pickup_time + recognised_object_lifespan
      
      # Set terminus for empty square object on (3, 2)
      expected_visual_spatial_field_object_properties[3][2][1][3] = putdown_time
      
      # Add object 0 to (3, 2)
      expected_visual_spatial_field_object_properties[3][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
    end 
  end 
end
  
################################################################################
# Tests for correct operation of the "VisualSpatialField.moveObjects()" function
# when moving a ghost object in every possible situation that may occur.
# 
# The scene used in the following test is illustrated below ("x" represents a 
# blind square, real objects are denoted by their identifiers and their class 
# are in parenthesis, ghost objects are denoted by lower case letters in 
# parenthesis).
# 
#                  --------
# 4     x      x   |      |   x      x
#           ----------------------
# 3     x   | 2(C) |      |      |   x
#    ------------------------------------
# 2  |      |      | 1(B) |      |      |
#    ------------------------------------
# 1     x   | 4(D) |      |      |   x
#           ----------------------
# 0     x     0(a) |3(SLF)|   x      x
#                  --------
#       0      1      2       3      4     COORDINATES
#          
unit_test "move_object (ghost object)" do
  
  move_types = [
    "from/to square containing a live blind object",
    "from/to square containing a live empty object",
    "to/from square containing the creator",
    "to/from square containing a live unrecognised object",
    "to/from square containing a live recognised object"
  ]
  
  for move_number in 0...move_types.count
    
    objects = [
      [VisualSpatialField.getGhostObjectIdPrefix + "0", "A"],
      ["1", "B"],
      ["2", "C"],
      ["3", Scene.getCreatorToken()],
      ["4", "D"]
    ]

    scene = Scene.new("Test", 5, 5, nil)
    scene.addItemToSquare(2, 0, objects[3][0], objects[3][1])
    scene.addItemToSquare(1, 1, objects[4][0], objects[4][1])
    scene.addItemToSquare(2, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(3, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(0, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(1, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(2, 2, objects[1][0], objects[1][1])
    scene.addItemToSquare(3, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(4, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(1, 3, objects[2][0], objects[2][1])
    scene.addItemToSquare(2, 3, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(3, 3, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(2, 4, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())

    ###################################
    ##### CREATE NEW CHREST MODEL #####
    ###################################
    model = Chrest.new
    model.setDomain(GenericDomain.new(model))
    model.getPerceiver.setFieldOfView(1)

    ###########################
    ##### CHREST LEARNING #####
    ###########################

    # Set the domain time (the time against which all CHREST operations will be
    # performed in this test).
    domain_time = 0

    # Since the scene creator is present, coordinates of the object to learn must
    # be set relative to the creator's location.
    real_object = ItemSquarePattern.new(objects[4][1], -1, 1)
    ghost_object = ItemSquarePattern.new(objects[0][1], -1, 0)
    list_pattern_to_learn = ListPattern.new()
    list_pattern_to_learn.add(real_object)
    list_pattern_to_learn.add(ghost_object)

    recognised_chunk = model.recogniseAndLearn(list_pattern_to_learn, domain_time).getImage().toString()
    until recognised_chunk == list_pattern_to_learn.toString()
      domain_time += 1
      recognised_chunk = model.recogniseAndLearn(list_pattern_to_learn, domain_time).getImage().toString()
    end

    # Set the domain time to be the value of CHREST's learning clock since, 
    # when the visual-spatial field is constructed, the LTM of the model will 
    # contain the completely familiarised learned pattern enabling expected 
    # visual-spatial field construction due to chunk recognition retrieving 
    # the learned pattern.
    domain_time = model.getLearningClock()

    ##########################################
    ##### CONSTRUCT VISUAL-SPATIAL FIELD #####
    ##########################################

    # Set visual-spatial field variables.
    creation_time = domain_time
    number_fixations = 20
    time_to_encode_objects = 50
    time_to_encode_empty_squares = 10
    visual_spatial_field_access_time = 100
    time_to_move_object = 250
    recognised_object_lifespan = 60000
    unrecognised_object_lifespan = 30000

    creation_time = domain_time
    
    visual_stm_contents_as_expected = false
    expected_stm_contents = recognised_chunk
    
    expected_fixations_made = false
    fixations_expected = [
      [2, 0],
      [1, 1], 
      [2, 2],
      [1, 3],
    ]

    until visual_stm_contents_as_expected and expected_fixations_made do
      
      visual_stm_contents_as_expected = false
      expected_fixations_made = false

      creation_time = domain_time

      # Construct the visual-spatial field.
      visual_spatial_field = VisualSpatialField.new(
        model,
        scene, 
        time_to_encode_objects,
        time_to_encode_empty_squares,
        visual_spatial_field_access_time, 
        time_to_move_object, 
        recognised_object_lifespan,
        unrecognised_object_lifespan,
        number_fixations,
        domain_time,
        true,
        false
      )

      # Get contents of STM (will have been populated during object 
      # recognition during visual-spatial field construction) and remove root 
      # nodes and nodes with empty images.  This will leave retrieved chunks 
      # that have non-empty images, i.e. these images should contain the 
      # list-patterns learned by the model.
      stm = model.getVisualStm()
      stm_contents = ""
      for i in (stm.getCount() - 1).downto(0)
        chunk = stm.getItem(i)
        if( !chunk.equals(model.getVisualLtm()) )
          if(!chunk.getImage().isEmpty())
            stm_contents += chunk.getImage().toString()
          end
        end
      end

      # Check if STM contents are as expected, if they are, set the flag that
      # controls when the model is ready for testing to true.
      expected_stm_contents == stm_contents ? visual_stm_contents_as_expected = true : nil
      
      expected_fixations_made = expected_fixations_made?(model, fixations_expected)

      # Advance domain time to the time that the visual-spatial field will be 
      # completely instantiated so that the model's attention will be free 
      # should a new visual-field need to be constructed.
      domain_time = model.getAttentionClock
    end

    ####################################################################
    ##### SET-UP EXPECTED VISUAL-SPATIAL FIELD COORDINATE CONTENTS #####
    ####################################################################

    expected_visual_spatial_field_object_properties = Array.new
    for col in 0...visual_spatial_field.getSceneEncoded().getWidth()
      expected_visual_spatial_field_object_properties.push([])
      for row in 0...visual_spatial_field.getSceneEncoded().getHeight()
        expected_visual_spatial_field_object_properties[col].push([])

        if (col == 2 and row == 0)
          expected_visual_spatial_field_object_properties[col][row].push([
            objects[3][0],
            objects[3][1],
            creation_time + visual_spatial_field_access_time,
            nil,
            false,
            false
          ])
        else
          expected_visual_spatial_field_object_properties[col][row].push([
            Scene.getBlindSquareToken(),
            Scene.getBlindSquareToken(),
            creation_time + visual_spatial_field_access_time,
            nil,
            false,
            false
          ])
        end
      end
    end

    number_objects_encoded = 1
    number_empty_squares_encoded = 0

    # Set expected object values for coordinates containing recognised objects.
    expected_visual_spatial_field_object_properties[1][1][0][3] = get_creation_time_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_encoded, number_empty_squares_encoded)
    expected_visual_spatial_field_object_properties[1][1].push([
      objects[4][0],
      objects[4][1],
      expected_visual_spatial_field_object_properties[1][1][0][3],
      expected_visual_spatial_field_object_properties[1][1][0][3] + recognised_object_lifespan,
      true,
      false
    ])
  
    expected_visual_spatial_field_object_properties[1][0][0][3] = get_creation_time_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_encoded, number_empty_squares_encoded)
    expected_visual_spatial_field_object_properties[1][0].push([
      objects[0][0],
      objects[0][1],
      expected_visual_spatial_field_object_properties[1][0][0][3],
      expected_visual_spatial_field_object_properties[1][0][0][3] + recognised_object_lifespan,
      true,
      true
    ])

    # Set expected object values for coordinates containing unrecognised objects.
    for row in 0...visual_spatial_field.getHeight()
      for col in 0...visual_spatial_field.getWidth()

        process_coordinates = false
        identifier = Scene.getEmptySquareToken()
        obj_class = Scene.getEmptySquareToken()

        if 
          ( (row == 1 or row == 3) and (col == 2 or col == 3) ) or
          (row == 2 and (col != 2)) or
          (row == 4 and (col == 2))
        then
          number_empty_squares_encoded += 1
          process_coordinates = true

        elsif(row == 2 and col == 2) or (row == 3 and col == 1)
          number_objects_encoded += 1
          process_coordinates = true

          if (row == 2 and col == 2)
            identifier = objects[1][0]
            obj_class = objects[1][1]
          else
            identifier = objects[2][0]
            obj_class = objects[2][1]
          end
        end

        if(process_coordinates)
          expected_visual_spatial_field_object_properties[col][row][0][3] = get_creation_time_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_encoded, number_empty_squares_encoded)
          expected_visual_spatial_field_object_properties[col][row].push([
            identifier,
            obj_class,
            expected_visual_spatial_field_object_properties[col][row][0][3],
            expected_visual_spatial_field_object_properties[col][row][0][3] + unrecognised_object_lifespan,
            false,
            false,
          ])
        end
      end
    end

    check_visual_spatial_field_against_expected(
      visual_spatial_field,
      expected_visual_spatial_field_object_properties,
      model.getAttentionClock(),
      "when checking initial state of visual-spatial field"
    )
    
    ############################################################################
    if (move_number == 0)
      # ================
      # MOVE DESCRIPTION
      # ================
      # - Move from square that was blind 
      # - Move to square that has a live blind object on it.
      # - Moves performed:
      #   + Object 0 moved from (1, 0) to (0, 0).
      # 
      # ===============
      # EXPECTED OUTPUT
      # ===============
      # - Object 0 on (1, 0) should have terminus modified
      # - Blind square object should be placed on (1, 0)
      # - Objects on coordinates (0, 0) should not be altered.
      # - Attention clock of model should be set to the time that object 0 is
      #   "put-down" on (0, 0).
      # - Expected visual-spatial field state:
      # 
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) |      |      |
      #    ------------------------------------
      # 1     x   | 4(D) |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 0))
      move.add(ItemSquarePattern.new(objects[0][0], 0, 0))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters.
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 0)
      expected_visual_spatial_field_object_properties[1][0][1][3] = pickup_time
      
      # New blind square object should be added to (1, 0) when 0 picked up.
      expected_visual_spatial_field_object_properties[1][0].push([
        Scene.getBlindSquareToken(),
        Scene.getBlindSquareToken(),
        pickup_time,
        nil,
        false,
        false
      ])
      
      # Objects on (0, 0) should not be modified.
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after moving " + move_types[move_number] + 
        " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after moving " + move_types[move_number] + 
        " (move number: " + move_number.to_s + ")"
      )
    
    ############################################################################
    elsif (move_number == 1)
      # ================
      # MOVE DESCRIPTION
      # ================
      # - Move to square that has a live empty object on it.
      # - Move from square that was empty and has no other live objects on it
      #   after objet move.
      # - Move(s) performed:
      #   + Object 0 moved from (1, 0) to (1, 2).
      #   + Object 0 moved from (1, 2) to (3, 2)
      # - In between moves, object 0's recognised status will be manually set
      #   to true.
      # 
      # ===============
      # EXPECTED OUTPUT
      # ===============
      # 
      # - First move
      #   + Object 0 on (1, 0) should have terminus set to time when object 0 is
      #     "picked-up".
      #   + Blind square object should be placed on (1, 0) when object 0 is 
      #     "picked-up".
      #   + Empty square object on (1, 2) should have terminus set to time when
      #     object 0 is "put-down".
      #   + Object 0 should be added to (1, 2) at time of put-down but should no
      #     longer be recognised.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (1, 2).
      #   + Expected visual-spatial field state:
      #	
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      | 0(a) | 1(B) |      |      |
      #    ------------------------------------
      # 1     x   | 4(D) |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      #
      # - Second move
      #   + Object 0 on (1, 2) should have terminus set to time when object 0 is
      #     "picked-up".
      #   + Empty square object should be placed on (1, 2) when object 0 is 
      #     "picked-up".
      #   + Empty square object on (3, 2) should have terminus set to time when
      #     object 0 is "put-down".
      #   + Object 0 should be added to (3, 2) at time of put-down but should no
      #     longer be recognised.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (3, 2).
      #   + Expected visual-spatial field state:
      #	
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) | 0(a) |      |
      #    ------------------------------------
      # 1     x   | 4(D) |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      
      ######################
      ##### FIRST MOVE #####
      ######################
      
      # Construct move
      move = ArrayList.new()
      move.add(ItemSquarePattern.new(objects[0][0], 1, 0))
      move.add(ItemSquarePattern.new(objects[0][0], 1, 2))
      move_sequence = ArrayList.new()
      move_sequence.add(move)
      
      # Set relevant timing parameters.
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 0)
      expected_visual_spatial_field_object_properties[1][0][1][3] = pickup_time
      
      # New blind square object should be added to (1, 0) when 0 picked up.
      expected_visual_spatial_field_object_properties[1][0].push([
        Scene.getBlindSquareToken(),
        Scene.getBlindSquareToken(),
        pickup_time,
        nil,
        false,
        false
      ])
    
      # Set terminus for empty square on (1, 2)
      expected_visual_spatial_field_object_properties[1][2][1][3] = putdown_time
      
      # Object 0 should be added to (1, 2) at putdown time.  Should no longer be
      # recognised.
      expected_visual_spatial_field_object_properties[1][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        true
      ])
      
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      
      #######################
      ##### SECOND MOVE #####
      #######################
      
      # Artificially make object 0 recognised again
      vsf = get_entire_visual_spatial_field(visual_spatial_field)
      vsf.get(1).get(2).get(2).setRecognised(putdown_time, true)
      
      # Set recognised status and terminus of object 0 on (1, 2)
      expected_visual_spatial_field_object_properties[1][2][2][3] = putdown_time + recognised_object_lifespan
      expected_visual_spatial_field_object_properties[1][2][2][4] = true
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 2))
      move.add(ItemSquarePattern.new(objects[0][0], 3, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 2)
      expected_visual_spatial_field_object_properties[1][2][2][3] = pickup_time
      
      # Add empty square to (1, 2)
      expected_visual_spatial_field_object_properties[1][2].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      # Set terminus for empty square on (3, 2)
      expected_visual_spatial_field_object_properties[3][2][1][3] = putdown_time
      
      # Add object 0 to (3, 2)
      expected_visual_spatial_field_object_properties[3][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        true
      ])
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after moving " + move_types[move_number] + 
        " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after moving " + move_types[move_number] + 
        " (move number: " + move_number.to_s + ")"
      )
      
    ############################################################################
    elsif (move_number == 2)
      # ================
      # MOVE DESCRIPTION
      # ================
      # - Move to square that has the creator on it.
      # - Move from square that has the creator on it.
      # - Move(s) performed:
      #   + Object 0 moved from (1, 0) to (2, 0).
      #   + Object 0 moved from (2, 0) to (3, 2).
      # - In between moves, object 0's recognised status will be manually set
      #   to true.
      # 
      # ===============
      # EXPECTED OUTPUT
      # ===============
      # - After first move
      #   + Object 0 on (1, 0) should have terminus set to time when it is
      #     "picked-up".
      #   + Blind square object should be placed on (1, 0) when object 0 is 
      #     "picked-up".
      #   + Object 0 should be added to (2, 0) at time of "put-down" but should 
      #     no longer be recognised.
      #   + The creator object on (2, 0) should not be modified.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (2, 0).
      #   + Expected visual-spatial field state:
      #     
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) |      |      |
      #    ------------------------------------
      # 1     x   | 4(D) |      |      |   x
      #           ----------------------
      # 0     x      x   | 0(a) |   x      x
      #                  |3(SLF)|
      #                  --------
      #       0      1      2       3      4     COORDINATES
      #     
      # - After second move
      #   + Object 0 on (2, 0) should have terminus set to time when it is 
      #     "picked-up".
      #   + The other objects on (2, 0) should not be modified.
      #   + The empty square on (3, 2) should have its terminus set to the time
      #     object 0 is "put-down".
      #   + Object 0 should be added to (3, 2).  Its creation time should be set
      #     to the time it is "put-down" and it should still be unrecognised.
      #   + Expected visual-spatial field state:
      #
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) | 0(a) |      |
      #    ------------------------------------
      # 1     x   | 4(D) |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
            
      ##############################
      ##### FIRST PART OF MOVE #####
      ##############################
      
      # Construct move.
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 0))
      move.add(ItemSquarePattern.new(objects[0][0], 2, 0))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters.
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 1)
      expected_visual_spatial_field_object_properties[1][0][1][3] = pickup_time
      
      # New blind square object should be added to (1, 0) when 0 picked up.
      expected_visual_spatial_field_object_properties[1][0].push([
        Scene.getBlindSquareToken(),
        Scene.getBlindSquareToken(),
        pickup_time,
        nil,
        false,
        false
      ])
      
      # Object 0 should be added to (2, 0) at first putdown time.  Should no 
      # longer be recognised.
      expected_visual_spatial_field_object_properties[2][0].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        true
      ])
    
      # Do not modify anything about the creator.
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      
      ###############################
      ##### SECOND PART OF MOVE #####
      ###############################
      
      # Artificially make object 0 recognised again
      vsf = get_entire_visual_spatial_field(visual_spatial_field)
      vsf.get(2).get(0).get(1).setRecognised(putdown_time, true)
      
      # Set recognised status and terminus of object 0 on (2, 0)
      expected_visual_spatial_field_object_properties[2][0][1][3] = putdown_time + recognised_object_lifespan
      expected_visual_spatial_field_object_properties[2][0][1][4] = true
      
      # Construct move.
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 2, 0))
      move.add(ItemSquarePattern.new(objects[0][0], 3, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant time parameters.
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (2, 0)
      expected_visual_spatial_field_object_properties[2][0][1][3] = pickup_time
      
      # Do not modify anything about the creator.
      
      # Set terminus for empty square object on (3, 2)
      expected_visual_spatial_field_object_properties[3][2][1][3] = putdown_time
      
      # Add object 0 to (3, 2)
      expected_visual_spatial_field_object_properties[3][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        true
      ])
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      
    ############################################################################
    elsif(move_number == 3)
      #	================
      # MOVE DESCRIPTION
      # ================
      #	- Move to square that has a live object on it that is not a blind square,
      #	  an empty square or the creator.
      #	- Move from square that has a live object on it that is not a blind 
      #   square, an empty square or the creator.
      # - Move(s) performed:
      #   + Object 0 moved from (1, 0) to (2, 2).
      #   + Object 0 moved from (2, 2) to (3, 2).
      # - In between moves, object 0's recognised status will be manually set
      #   to true.
      #	
      #	===============
      # EXPECTED OUTPUT
      # ===============
      # - After first move
      #   + Object 0 on (1, 0) should have terminus set to time when it is
      #     "picked-up".
      #   + Blind square object should be placed on (1, 0) when object 0 is 
      #     "picked-up".
      #   + Object 0 should be added to (2, 2) at time of "put-down" but should 
      #     no longer be recognised.
      #   + Object 1 on (2, 2) should have its terminus extended to the
      #     time object 0 is "put-down" plus the lifespan for an unrecognised
      #     object.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (2, 2).
      #   + Expected visual-spatial field state:
      #   
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 0(a) |      |      |
      #    |      |      | 1(B) |      |      |
      #    ------------------------------------
      # 1     x   | 4(D) |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      #       
      # - After second move
      #   + Object 0 on (2, 2) should have terminus set to time when it is
      #     "picked-up".
      #   + Terminus for object 1 should be extended to the time object 0 is
      #     "picked-up" plus the lifespan specified for unrecognised objects.
      #   + The empty square object on (3, 2) should have its terminus set to
      #     the time that object 0 is "put-down".
      #   + Object 0 should be added to (3, 2) at time of "put-down" and should 
      #     not be recognised.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (3, 2).
      #   + Expected visual-spatial field state:
      #
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) | 0(a) |      |
      #    ------------------------------------
      # 1     x   | 4(D) |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      
      ##############################
      ##### FIRST PART OF MOVE #####
      ##############################
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 0))
      move.add(ItemSquarePattern.new(objects[0][0], 2, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant time parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 0)
      expected_visual_spatial_field_object_properties[1][0][1][3] = pickup_time
      
      # New blind square object should be added to (1, 0) when 0 picked up.
      expected_visual_spatial_field_object_properties[1][0].push([
        Scene.getBlindSquareToken(),
        Scene.getBlindSquareToken(),
        pickup_time,
        nil,
        false,
        false
      ])
      
      # Object 0 should be added to (2, 2) at first putdown time.  Should no 
      # longer be recognised.
      expected_visual_spatial_field_object_properties[2][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        true
      ])
    
      # Update terminus for object 1 on (2, 2) since the coordinates have been
      # looked at and the object is alive when 0 is putdown.
      expected_visual_spatial_field_object_properties[2][2][1][3] = putdown_time + unrecognised_object_lifespan
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      ###############################
      ##### SECOND PART OF MOVE #####
      ###############################
      
      # Make object 0 recognised again
      vsf = get_entire_visual_spatial_field(visual_spatial_field)
      vsf.get(2).get(2).get(2).setRecognised(putdown_time, true)
      
      # Set recognised status and terminus of object 0 on (2, 2)
      expected_visual_spatial_field_object_properties[2][2][2][3] = putdown_time + recognised_object_lifespan
      expected_visual_spatial_field_object_properties[2][2][2][4] = true
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 2, 2))
      move.add(ItemSquarePattern.new(objects[0][0], 3, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (2, 2)
      expected_visual_spatial_field_object_properties[2][2][2][3] = pickup_time
      
      # Set terminus for object 1 on (2, 2)
      expected_visual_spatial_field_object_properties[2][2][1][3] = pickup_time + unrecognised_object_lifespan
      
      # Set terminus for empty square object on (3, 2)
      expected_visual_spatial_field_object_properties[3][2][1][3] = putdown_time
      
      # Add object 0 to (3, 2)
      expected_visual_spatial_field_object_properties[3][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        true
      ])
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      
    ############################################################################
    elsif(move_number == 4)
      #	================
      # MOVE DESCRIPTION
      # ================
      #	- Move to square that has a live, recognised object on it.
      #	- Move from square that has a live, recognised object on it.
      # - Move(s) performed:
      #   + Object 0 moved from (1, 0) to (1, 3).
      #   + Object 0 moved from (1, 3) to (3, 2).
      # - In between moves, object 0's recognised status will be manually set
      #   to true.
      # - Object 2's recognised status will be set manually.
      #	
      #	===============
      # EXPECTED OUTPUT
      # ===============
      # - After first move
      #   + Object 0 on (1, 0) should have terminus set to time when it is
      #     "picked-up".
      #   + Blind square object should be placed on (1, 0) when object 0 is 
      #     "picked-up".
      #   + Object 0 should be added to (1, 3) at time of "put-down" but should 
      #     no longer be recognised.
      #   + Object 2 on (2, 2) should have its terminus extended to the
      #     time object 0 is "put-down" plus the lifespan for a recognised
      #     object.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (2, 2).
      #   + Expected visual-spatial field state:
      #   
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 0(a) |      |      |   x
      #           | 2(C) |      |      |
      #    ------------------------------------
      # 2  |      |      | 1(B) |      |      |
      #    ------------------------------------
      # 1     x   | 4(D) |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      #       
      # - After second move
      #   + Object 0 on (1, 3) should have terminus set to time when it is
      #     "picked-up".
      #   + Terminus for object 2 should be extended to the time object 0 is
      #     "picked-up" plus the lifespan specified for recognised objects.
      #   + The empty square object on (3, 2) should have its terminus set to
      #     the time that object 0 is "put-down".
      #   + Object 0 should be added to (3, 2) at time of "put-down" and should 
      #     not be recognised.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (3, 2).
      #   + Expected visual-spatial field state:
      #
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(C) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(B) | 0(a) |      |
      #    ------------------------------------
      # 1     x   | 4(D) |      |      |   x
      #           ----------------------
      # 0     x      x   |3(SLF)|   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      
      vsf = get_entire_visual_spatial_field(visual_spatial_field)
      vsf.get(1).get(3).get(1).setRecognised(model.getAttentionClock(), true)
      expected_visual_spatial_field_object_properties[1][3][1][3] = model.getAttentionClock() + recognised_object_lifespan
      expected_visual_spatial_field_object_properties[1][3][1][4] = true
      
      ##############################
      ##### FIRST PART OF MOVE #####
      ##############################
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 0))
      move.add(ItemSquarePattern.new(objects[0][0], 1, 3))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant time parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 1)
      expected_visual_spatial_field_object_properties[1][0][1][3] = pickup_time
      
      # New blind square object should be added to (1, 0) when 0 picked up.
      expected_visual_spatial_field_object_properties[1][0].push([
        Scene.getBlindSquareToken(),
        Scene.getBlindSquareToken(),
        pickup_time,
        nil,
        false,
        false
      ])
      
      # Object 0 should be added to (1, 3) at first putdown time.  Should no 
      # longer be recognised.
      expected_visual_spatial_field_object_properties[1][3].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        true
      ])
    
      # Update terminus for object 2 on (1, 3) since the coordinates have been
      # looked at and the object is alive when 0 is putdown.
      expected_visual_spatial_field_object_properties[1][3][1][3] = putdown_time + recognised_object_lifespan
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      
      ###############################
      ##### SECOND PART OF MOVE #####
      ###############################
      
      # Make object 0 recognised again
      vsf = get_entire_visual_spatial_field(visual_spatial_field)
      vsf.get(1).get(3).get(2).setRecognised(putdown_time, true)
      
      # Set recognised status and terminus of object 0 on (1, 3)
      expected_visual_spatial_field_object_properties[1][3][2][3] = putdown_time + recognised_object_lifespan
      expected_visual_spatial_field_object_properties[1][3][2][4] = true
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 3))
      move.add(ItemSquarePattern.new(objects[0][0], 3, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 3)
      expected_visual_spatial_field_object_properties[1][3][2][3] = pickup_time
      
      # Set terminus for object 2 on (1, 3)
      expected_visual_spatial_field_object_properties[1][3][1][3] = pickup_time + recognised_object_lifespan
      
      # Set terminus for empty square object on (3, 2)
      expected_visual_spatial_field_object_properties[3][2][1][3] = putdown_time
      
      # Add object 0 to (3, 2)
      expected_visual_spatial_field_object_properties[3][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        putdown_time + unrecognised_object_lifespan,
        false,
        true
      ])
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
    end 
  end 
end

################################################################################
# Tests for correct operation of the "VisualSpatialField.moveObjects()" function
# when moving the creator in all possible scenarios.
# 
# The scene used in the following test is illustrated below ("x" represents a 
# blind square, real objects are denoted by their identifiers and their class 
# are in parenthesis, ghost objects are denoted by lower case letters in 
# parenthesis).
# 
#                  --------
# 4     x      x   |      |   x      x
#           ----------------------
# 3     x   | 2(B) |      |      |   x
#    ------------------------------------
# 2  |      |      | 1(A) |      |      |
#    ------------------------------------
# 1     x   |      |      |      |   x
#           ----------------------
# 0     x      x   |SLF(0)|   x      x
#                  --------
#       0      1      2       3      4     COORDINATES
#          
unit_test "move_object (creator)" do
  
  move_types = [
    "to square containing a live blind object",
    "from/to square containing a live empty object",
    "to/from square containing a live unrecognised object",
    "to/from square containing a live recognised object"
  ]
  
  for move_number in 0...move_types.count
    
    objects = [
      ["0", Scene.getCreatorToken()],
      ["1", "A"],
      ["2", "B"]
    ]

    scene = Scene.new("Test", 5, 5, nil)
    scene.addItemToSquare(2, 0, objects[0][0], objects[0][1])
    scene.addItemToSquare(1, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(2, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(3, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(0, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(1, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(2, 2, objects[1][0], objects[1][1])
    scene.addItemToSquare(3, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(4, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(1, 3, objects[2][0], objects[2][1])
    scene.addItemToSquare(2, 3, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(3, 3, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
    scene.addItemToSquare(2, 4, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())

    ###################################
    ##### CREATE NEW CHREST MODEL #####
    ###################################
    model = Chrest.new
    model.setDomain(GenericDomain.new(model))
    model.getPerceiver.setFieldOfView(1)

    ##########################################
    ##### CONSTRUCT VISUAL-SPATIAL FIELD #####
    ##########################################

    # Set visual-spatial field variables.
    creation_time = 0
    number_fixations = 20
    time_to_encode_objects = 50
    time_to_encode_empty_squares = 10
    visual_spatial_field_access_time = 100
    time_to_move_object = 250
    recognised_object_lifespan = 60000
    unrecognised_object_lifespan = 30000
    
    expected_fixations_made = false
    fixations_expected = [
      [2, 0],
      [2, 2],
      [1, 3]
    ]

    until expected_fixations_made do

      visual_spatial_field = VisualSpatialField.new(
        model,
        scene, 
        time_to_encode_objects,
        time_to_encode_empty_squares,
        visual_spatial_field_access_time, 
        time_to_move_object, 
        recognised_object_lifespan,
        unrecognised_object_lifespan,
        number_fixations,
        creation_time,
        true,
        false
      )
      
      expected_fixations_made = expected_fixations_made?(model, fixations_expected)
    end

    ####################################################################
    ##### SET-UP EXPECTED VISUAL-SPATIAL FIELD COORDINATE CONTENTS #####
    ####################################################################

    expected_visual_spatial_field_object_properties = Array.new
    for col in 0...visual_spatial_field.getSceneEncoded().getWidth()
      expected_visual_spatial_field_object_properties.push([])
      for row in 0...visual_spatial_field.getSceneEncoded().getHeight()
        expected_visual_spatial_field_object_properties[col].push([])

        if (col == 2 and row == 0)
          expected_visual_spatial_field_object_properties[col][row].push([
            objects[0][0],
            objects[0][1],
            creation_time + visual_spatial_field_access_time,
            nil,
            false,
            false
          ])
        else
          expected_visual_spatial_field_object_properties[col][row].push([
            Scene.getBlindSquareToken(),
            Scene.getBlindSquareToken(),
            creation_time + visual_spatial_field_access_time,
            nil,
            false,
            false
          ])
        end
      end
    end

    number_objects_encoded = 0
    number_empty_squares_encoded = 0

    # Set expected object values for coordinates containing unrecognised objects.
    for row in 0...visual_spatial_field.getHeight()
      for col in 0...visual_spatial_field.getWidth()

        process_coordinates = false
        identifier = Scene.getEmptySquareToken()
        obj_class = Scene.getEmptySquareToken()

        if 
          ( (row == 1) and (col != 0 and col != 4) ) or
          (row == 2 and (col != 2)) or
          (row == 3 and (col == 2 or col == 3)) or
          (row == 4 and (col == 2))
        then
          number_empty_squares_encoded += 1
          process_coordinates = true

        elsif(
            (row == 2 and col == 2) or
            (row == 3 and col == 1)
          )
          number_objects_encoded += 1
          process_coordinates = true

          if (row == 2 and col == 2)
            identifier = objects[1][0]
            obj_class = objects[1][1]
          else
            identifier = objects[2][0]
            obj_class = objects[2][1]
          end
        end

        if(process_coordinates)
          expected_visual_spatial_field_object_properties[col][row][0][3] = get_creation_time_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_encoded, number_empty_squares_encoded)
          expected_visual_spatial_field_object_properties[col][row].push([
            identifier,
            obj_class,
            expected_visual_spatial_field_object_properties[col][row][0][3],
            expected_visual_spatial_field_object_properties[col][row][0][3] + unrecognised_object_lifespan,
            false,
            false,
          ])
        end
      end
    end

    check_visual_spatial_field_against_expected(
      visual_spatial_field,
      expected_visual_spatial_field_object_properties,
      model.getAttentionClock(),
      "when checking initial state of visual-spatial field"
    )
    
    ############################################################################
    if (move_number == 0)
      # ================
      # MOVE DESCRIPTION
      # ================
      # - Move to square that has a live blind object on it.
      # - Moves performed:
      #   + Object 0 moved from (2, 0) to (1, 0).
      # 
      # ===============
      # EXPECTED OUTPUT
      # ===============
      # - Object 0 on (2, 0) should have terminus modified
      # - Empty square object should be placed on (2, 0)
      # - Objects on coordinates (1, 0) should not be altered.
      # - Attention clock of model should be set to the time that object 0 is
      #   "put-down" on (1, 0).
      # - Expected visual-spatial field state:
      # 
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(B) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(A) |      |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |      |   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 2, 0))
      move.add(ItemSquarePattern.new(objects[0][0], 1, 0))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters.
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (2, 0)
      expected_visual_spatial_field_object_properties[2][0][0][3] = pickup_time
      
      # New empty square object should be added to (2, 0) when 0 picked up.
      expected_visual_spatial_field_object_properties[2][0].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
      
      # Objects on (1, 0) should not be modified.
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after moving " + move_types[move_number] + 
        " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after moving " + move_types[move_number] + 
        " (move number: " + move_number.to_s + ")"
      )
    
    ############################################################################
    elsif (move_number == 1)
      # ================
      # MOVE DESCRIPTION
      # ================
      # - Move to square that has a live empty object on it.
      # - Move from square that was empty and has no other live objects on it
      #   after object move.
      # - Move(s) performed:
      #   + Object 0 moved from (2, 0) to (1, 2).
      #   + Object 0 moved from (1, 2) to (3, 2)
      # 
      # ===============
      # EXPECTED OUTPUT
      # ===============
      # 
      # - First move
      #   + Object 0 on (2, 0) should have terminus set to time when object 0 is
      #     "picked-up".
      #   + Empty square object should be placed on (2, 0) when object 0 is 
      #     "picked-up".
      #   + Empty square object on (1, 2) should have terminus set to time when
      #     object 0 is "put-down".
      #   + Object 0 should be added to (1, 2) at time of put-down but should no
      #     longer be recognised.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (1, 2).
      #   + Expected visual-spatial field state:
      #	
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(B) |      |      |   x
      #    ------------------------------------
      # 2  |      |0(SLF)| 1(A) |      |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |      |   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      #
      # - Second move
      #   + Object 0 on (1, 2) should have terminus set to time when object 0 is
      #     "picked-up".
      #   + Empty square object should be placed on (1, 2) when object 0 is 
      #     "picked-up".
      #   + Empty square object on (3, 2) should have terminus set to time when
      #     object 0 is "put-down".
      #   + Object 0 should be added to (3, 2) at time of put-down but should no
      #     longer be recognised.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (3, 2).
      #   + Expected visual-spatial field state:
      #	
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(B) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(A) |0(SLF)|      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |      |   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      
      ######################
      ##### FIRST MOVE #####
      ######################
      
      # Construct move
      move = ArrayList.new()
      move.add(ItemSquarePattern.new(objects[0][0], 2, 0))
      move.add(ItemSquarePattern.new(objects[0][0], 1, 2))
      move_sequence = ArrayList.new()
      move_sequence.add(move)
      
      # Set relevant timing parameters.
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (2, 0)
      expected_visual_spatial_field_object_properties[2][0][0][3] = pickup_time
      
      # New empty square object should be added to (2, 0) when 0 picked up.
      expected_visual_spatial_field_object_properties[2][0].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      # Set terminus for empty square on (1, 2)
      expected_visual_spatial_field_object_properties[1][2][1][3] = putdown_time
      
      # Object 0 should be added to (1, 2) at putdown time.
      expected_visual_spatial_field_object_properties[1][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        nil,
        false,
        false
      ])
      
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      
      #######################
      ##### SECOND MOVE #####
      #######################
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 2))
      move.add(ItemSquarePattern.new(objects[0][0], 3, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 2)
      expected_visual_spatial_field_object_properties[1][2][2][3] = pickup_time
      
      # Add empty square to (1, 2)
      expected_visual_spatial_field_object_properties[1][2].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
    
      # Set terminus for empty square on (3, 2)
      expected_visual_spatial_field_object_properties[3][2][1][3] = putdown_time
      
      # Add object 0 to (3, 2)
      expected_visual_spatial_field_object_properties[3][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        nil,
        false,
        false
      ])
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after moving " + move_types[move_number] + 
        " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after moving " + move_types[move_number] + 
        " (move number: " + move_number.to_s + ")"
      )
      
    ############################################################################
    elsif (move_number == 2)
      #	================
      # MOVE DESCRIPTION
      # ================
      #	- Move to square that has a live object on it that is not a blind square
      #	  or an empty square.
      #	- Move from square that has alive object on it that is not a blind 
      #   square or an empty square.
      # - Move(s) performed:
      #   + Object 0 moved from (2, 0) to (2, 2).
      #   + Object 0 moved from (2, 2) to (3, 2).
      #	
      #	===============
      # EXPECTED OUTPUT
      # ===============
      # - After first move
      #   + Object 0 on (2, 0) should have terminus set to time when it is
      #     "picked-up".
      #   + Empty square object should be placed on (2, 0) when object 0 is 
      #     "picked-up".
      #   + Object 0 should be added to (2, 2) at time of "put-down"
      #   + Object 1 on (2, 2) should have its terminus extended to the
      #     time object 0 is "put-down" plus the lifespan for an unrecognised
      #     object.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (2, 2).
      #   + Expected visual-spatial field state:
      #   
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(B) |      |      |   x
      #    ------------------------------------
      # 2  |      |      |0(SLF)|      |      |
      #    |      |      | 1(A) |      |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |      |   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      #       
      # - After second move
      #   + Object 0 on (2, 2) should have terminus set to time when it is
      #     "picked-up".
      #   + Terminus for object 1 should be extended to the time object 0 is
      #     "picked-up" plus the lifespan specified for unrecognised objects.
      #   + The empty square object on (3, 2) should have its terminus set to
      #     the time that object 0 is "put-down".
      #   + Object 0 should be added to (3, 2) at time of "put-down" and should 
      #     not be recognised.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (3, 2).
      #   + Expected visual-spatial field state:
      #
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(B) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(A) |0(SLF)|      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |      |   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      
      ##############################
      ##### FIRST PART OF MOVE #####
      ##############################
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 2, 0))
      move.add(ItemSquarePattern.new(objects[0][0], 2, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant time parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (2, 0)
      expected_visual_spatial_field_object_properties[2][0][0][3] = pickup_time
      
      # New empty square object should be added to (2, 0) when 0 picked up.
      expected_visual_spatial_field_object_properties[2][0].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
      
      # Object 0 should be added to (2, 2) at first putdown time.
      expected_visual_spatial_field_object_properties[2][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        nil,
        false,
        false
      ])
    
      # Update terminus for object 1 on (2, 2) since the coordinates have been
      # looked at and the object is alive when 0 is putdown.
      expected_visual_spatial_field_object_properties[2][2][1][3] = putdown_time + unrecognised_object_lifespan
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      ###############################
      ##### SECOND PART OF MOVE #####
      ###############################
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 2, 2))
      move.add(ItemSquarePattern.new(objects[0][0], 3, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (2, 2)
      expected_visual_spatial_field_object_properties[2][2][2][3] = pickup_time
      
      # Set terminus for object 1 on (2, 2)
      expected_visual_spatial_field_object_properties[2][2][1][3] = pickup_time + unrecognised_object_lifespan
      
      # Set terminus for empty square object on (3, 2)
      expected_visual_spatial_field_object_properties[3][2][1][3] = putdown_time
      
      # Add object 0 to (3, 2)
      expected_visual_spatial_field_object_properties[3][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        nil,
        false,
        false
      ])
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      
    ############################################################################
    elsif(move_number == 3)
      #	================
      # MOVE DESCRIPTION
      # ================
      #	- Move to square that has a live, recognised object on it.
      #	- Move from square that has a live, recognised object on it.
      # - Move(s) performed:
      #   + Object 0 moved from (2, 0) to (1, 3).
      #   + Object 0 moved from (1, 3) to (3, 2).
      # - Object 2's recognised status will be set manually.
      #	
      #	===============
      # EXPECTED OUTPUT
      # ===============
      # - After first move
      #   + Object 0 on (2, 0) should have terminus set to time when it is
      #     "picked-up".
      #   + Empty square object should be placed on (2, 0) when object 0 is 
      #     "picked-up".
      #   + Object 0 should be added to (1, 3) at time of "put-down".
      #   + Object 2 on (1, 3) should have its terminus extended to the
      #     time object 0 is "put-down" plus the lifespan for a recognised
      #     object.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (1, 3).
      #   + Expected visual-spatial field state:
      #   
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   |0(SLF)|      |      |   x
      #           | 2(B) |      |      |
      #    ------------------------------------
      # 2  |      |      | 1(A) |      |      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |      |   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      #       
      # - After second move
      #   + Object 0 on (1, 3) should have terminus set to time when it is
      #     "picked-up".
      #   + Terminus for object 2 should be extended to the time object 0 is
      #     "picked-up" plus the lifespan specified for recognised objects.
      #   + The empty square object on (3, 2) should have its terminus set to
      #     the time that object 0 is "put-down".
      #   + Object 0 should be added to (3, 2) at time of "put-down" and should 
      #     not be recognised.
      #   + Attention clock of model should be set to the time that object 0 is
      #     "put-down" on (3, 2).
      #   + Expected visual-spatial field state:
      #
      #                  --------
      # 4     x      x   |      |   x      x
      #           ----------------------
      # 3     x   | 2(B) |      |      |   x
      #    ------------------------------------
      # 2  |      |      | 1(A) |0(SLF)|      |
      #    ------------------------------------
      # 1     x   |      |      |      |   x
      #           ----------------------
      # 0     x      x   |      |   x      x
      #                  --------
      #       0      1      2       3      4     COORDINATES
      
      vsf = get_entire_visual_spatial_field(visual_spatial_field)
      vsf.get(1).get(3).get(1).setRecognised(model.getAttentionClock(), true)
      expected_visual_spatial_field_object_properties[1][3][1][3] = model.getAttentionClock() + recognised_object_lifespan
      expected_visual_spatial_field_object_properties[1][3][1][4] = true
      
      ##############################
      ##### FIRST PART OF MOVE #####
      ##############################
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 2, 0))
      move.add(ItemSquarePattern.new(objects[0][0], 1, 3))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant time parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (2, 0)
      expected_visual_spatial_field_object_properties[2][0][0][3] = pickup_time
      
      # New empty square object should be added to (2, 0) when 0 picked up.
      expected_visual_spatial_field_object_properties[2][0].push([
        Scene.getEmptySquareToken(),
        Scene.getEmptySquareToken(),
        pickup_time,
        pickup_time + unrecognised_object_lifespan,
        false,
        false
      ])
      
      # Object 0 should be added to (1, 3) at first putdown time.
      expected_visual_spatial_field_object_properties[1][3].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        nil,
        false,
        false
      ])
    
      # Update terminus for object 2 on (1, 3) since the coordinates have been
      # looked at and the object is alive when 0 is putdown.
      expected_visual_spatial_field_object_properties[1][3][1][3] = putdown_time + recognised_object_lifespan
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after first part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      ###############################
      ##### SECOND PART OF MOVE #####
      ###############################
      
      # Construct move
      move = ArrayList.new
      move.add(ItemSquarePattern.new(objects[0][0], 1, 3))
      move.add(ItemSquarePattern.new(objects[0][0], 3, 2))
      move_sequence = ArrayList.new
      move_sequence.add(move)
      
      # Set relevant timing parameters
      time_move_requested = model.getAttentionClock()
      pickup_time = time_move_requested + visual_spatial_field_access_time
      putdown_time = pickup_time + time_to_move_object
      
      # Set terminus for object 0 on (1, 3)
      expected_visual_spatial_field_object_properties[1][3][2][3] = pickup_time
      
      # Set terminus for object 2 on (1, 3)
      expected_visual_spatial_field_object_properties[1][3][1][3] = pickup_time + recognised_object_lifespan
      
      # Set terminus for empty square object on (3, 2)
      expected_visual_spatial_field_object_properties[3][2][1][3] = putdown_time
      
      # Add object 0 to (3, 2)
      expected_visual_spatial_field_object_properties[3][2].push([
        objects[0][0],
        objects[0][1],
        putdown_time,
        nil,
        false,
        false
      ])
    
      visual_spatial_field.moveObjects(move_sequence, time_move_requested, false)
      check_visual_spatial_field_against_expected(
        visual_spatial_field,
        expected_visual_spatial_field_object_properties,
        model.getAttentionClock(),
        "when checking state of visual-spatial field after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
      assert_equal(
        putdown_time, 
        model.getAttentionClock(), 
        "occurred when checking the time that the CHREST model associated with " +
        "the visual-spatial field will be free after second part of moving " + 
        move_types[move_number] + " (move number: " + move_number.to_s + ")"
      )
    end 
  end 
end

################################################################################
# Tests for correct behaviour when illegal move requests are made.
# 
# 1) Request a move that is legal but while the attention resource is consumed.
# 2) Request a move when the attention resource is free and the first object 
#    move is legal but the initial location for the second object is incorrect.
# 3) Request a move when the attention resource is free and the first object 
#    move is legal but only the initial location for the second object move is 
#    specified.
# 4) Request a move when the attention resource is free and the first object 
#    move is legal but object movement in the second object move is not serial. 
#
# The scene used in the following test resembles a "cone" of vision i.e. the 
# further ahead the observer sees, the wider its field of vision.  A diagram of 
# this scene can be found below ("x" represents a "blind spot" and an object
# is denoted by its identifier and its class is in parenthesis).
# 
#   ----------------------
# 1 | 3(C) | 1(B) |      |
#   ----------------------
# 0    x   | 0(A) |  x
#          --------
#      0      1      2    VISUAL-SPATIAL FIELD COORDS
#
unit_test "move_objects_illegally" do
  
  # Set the objects that will be used.
  test_objects = [
    ["0", "A"], 
    ["1", "B"], 
    ["2", "C"]
  ]
  
  # Create the scene to be transposed into the visual-spatial field.
  scene = Scene.new("Test scene", 3, 2, nil)
  scene.addItemToSquare(1, 0, test_objects[0][0], test_objects[0][1])
  scene.addItemToSquare(0, 1, test_objects[2][0], test_objects[2][1])
  scene.addItemToSquare(1, 1, test_objects[1][0], test_objects[1][1])
  scene.addItemToSquare(2, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  
  # Create a new CHREST instance and set its domain (important to enable 
  # perceptual mechanisms).
  model = Chrest.new
  model.setDomain(GenericDomain.new(model))
  
  # Set independent variables.
  creation_time = 0
  number_fixations = 5
  time_to_encode_objects = 50
  time_to_encode_empty_squares = 0
  visual_spatial_field_access_time = 100
  time_to_move_object = 250
  lifespan_for_recognised_objects = 60000
  lifespan_for_unrecognised_objects = 30000
  
  # Create the visual-spatial field
  expected_fixations_made = false
  fixations_expected = [
    [1, 0],
    [0, 1],
    [1, 1]
  ]

  until expected_fixations_made do
  
    visual_spatial_field = VisualSpatialField.new(
      model,
      scene, 
      time_to_encode_objects,
      time_to_encode_empty_squares,
      visual_spatial_field_access_time, 
      time_to_move_object, 
      lifespan_for_recognised_objects,
      lifespan_for_unrecognised_objects,
      number_fixations,
      creation_time,
      false,
      false
    )
    
    expected_fixations_made = expected_fixations_made?(model, fixations_expected)
  end
  
  expected_visual_spatial_field_object_properties = Array.new
  for col in 0...visual_spatial_field.getSceneEncoded().getWidth()
    expected_visual_spatial_field_object_properties.push([])
    for row in 0...visual_spatial_field.getSceneEncoded().getHeight()
      expected_visual_spatial_field_object_properties[col].push([])
      expected_visual_spatial_field_object_properties[col][row].push([
        Scene.getBlindSquareToken(),
        Scene.getBlindSquareToken(),
        creation_time + visual_spatial_field_access_time,
        nil,
        false,
        false
      ])
    end
  end
  
  expected_visual_spatial_field_object_properties[1][0][0][3] = get_creation_time_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, 1, 0)
  expected_visual_spatial_field_object_properties[1][0].push([
    test_objects[0][0],
    test_objects[0][1],
    get_creation_time_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, 1, 0),
    get_terminus_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, 1, 0, lifespan_for_unrecognised_objects),
    false,
    false,
  ])

  expected_visual_spatial_field_object_properties[0][1][0][3] = get_creation_time_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, 2, 0)
  expected_visual_spatial_field_object_properties[0][1].push([
    test_objects[2][0],
    test_objects[2][1],
    get_creation_time_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, 2, 0),
    get_terminus_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, 2, 0, lifespan_for_unrecognised_objects),
    false,
    false,
  ])

  expected_visual_spatial_field_object_properties[1][1][0][3] = get_creation_time_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 0)
  expected_visual_spatial_field_object_properties[1][1].push([
    test_objects[1][0],
    test_objects[1][1],
    get_creation_time_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 0),
    get_terminus_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 0, lifespan_for_unrecognised_objects),
    false,
    false,
  ])

  expected_visual_spatial_field_object_properties[2][1][0][3] = get_creation_time_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 1)
  expected_visual_spatial_field_object_properties[2][1].push([
    Scene.getEmptySquareToken(),
    Scene.getEmptySquareToken(),
    get_creation_time_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 1),
    get_terminus_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 1, lifespan_for_unrecognised_objects),
    false,
    false,
  ])
  
  begin
    object_0_legal_move = ArrayList.new
    object_0_legal_move.add(ItemSquarePattern.new(test_objects[0][0], 1, 0))
    object_0_legal_move.add(ItemSquarePattern.new(test_objects[0][0], 1, 1))
    moves = ArrayList.new
    moves.add(object_0_legal_move)
    visual_spatial_field.moveObjects(moves, model.getAttentionClock() - 1, false)
  rescue
  end
  
  check_visual_spatial_field_against_expected(
    visual_spatial_field,
    expected_visual_spatial_field_object_properties,
    model.getAttentionClock(),
    "after attempting to perform a legal move but before attention is free"
  )
  
  begin
    object_1_incorrect_initial_location = ArrayList.new
    object_1_incorrect_initial_location.add(ItemSquarePattern.new(test_objects[1][0], 1, 0))
    object_1_incorrect_initial_location.add(ItemSquarePattern.new(test_objects[1][0], 2, 1))
    moves = ArrayList.new
    moves.add(object_0_legal_move)
    moves.add(object_1_incorrect_initial_location)
    visual_spatial_field.moveObjects(moves, model.getAttentionClock(), false)
  rescue
  end
  
  check_visual_spatial_field_against_expected(
    visual_spatial_field,
    expected_visual_spatial_field_object_properties,
    model.getAttentionClock(),
    "after attempting to perform a move when an object's initial location specification is incorrect"
  )

  begin
    object_1_initial_location_only = ArrayList.new
    object_1_initial_location_only.add(ItemSquarePattern.new(test_objects[1][0], 1, 1))
    moves = ArrayList.new
    moves.add(object_0_legal_move)
    moves.add(object_1_initial_location_only)
    visual_spatial_field.moveObjects(moves, model.getAttentionClock(), false)
  rescue
  end
  
  check_visual_spatial_field_against_expected(
    visual_spatial_field,
    expected_visual_spatial_field_object_properties,
    model.getAttentionClock(),
    "after attempting to perform a move where only the initial location of an object is specified"
  )
  
  begin
    object_1_non_serial = ArrayList.new
    object_1_non_serial.add(ItemSquarePattern.new(test_objects[1][0], 1, 1))
    object_1_non_serial.add(ItemSquarePattern.new(test_objects[1][0], 2, 1))
    object_1_non_serial.add(ItemSquarePattern.new(test_objects[2][0], 0, 1))
    moves = ArrayList.new
    moves.add(object_0_legal_move)
    moves.add(object_1_non_serial)
    visual_spatial_field.moveObjects(moves, model.getAttentionClock(), false)
  rescue
  end
  
  check_visual_spatial_field_against_expected(
    visual_spatial_field,
    expected_visual_spatial_field_object_properties,
    model.getAttentionClock(),
    "after attempting to move an object part-way through another object's move sequence"
  )
  assert_equal(creation_time + visual_spatial_field_access_time + (time_to_encode_objects * 3) + time_to_encode_empty_squares, model.getAttentionClock(), "occurred when checking the time that the CHREST model associated with the visual-spatial field.")
end

################################################################################
# BEFORE MOVE
# ===========
#
#                  --------
# 4     x      x   |      |   x      x
#           ----------------------
# 3     x   |      | 4(D) |      |   x
#    ------------------------------------
# 2  |      | 2(B) |      | 5(E) | 3(C) |
#    ------------------------------------
# 1     x   |      | 1(A) |      |   x
#           ----------------------
# 0     x      x   |0(SLF)|   x      x
#                  --------
#       0      1      2       3      4
#       
# AFTER MOVE
# ==========
#
#                  --------
# 4     x      x   |      |   x      x
#           ----------------------
# 3     x   |      | 4(D) |      |   x
#           |      | 5(E) |      |
#    ------------------------------------
# 2  |      | 2(B) |      |      | 3(C) |
#    ------------------------------------
# 1     x   |      | 1(A) |      |   x
#           ----------------------
# 0     x      x   |0(SLF)|   x      x
#                  --------
#       0      1      2       3      4
#
# AT TERMINUS FOR NON-MOVED OBJECTS
# =================================
# 
#                  --------
# 4     x      x   |  ?   |   x      x
#           ----------------------
# 3     x   |  ?   | 4(D) |   ?  |   x
#           |      | 5(E) |      |
#    ------------------------------------
# 2  |  ?   |  ?   |  ?   |   ?  |   ?  |
#    ------------------------------------
# 1     x   |  ?   |  ?   |   ?  |   x
#           ----------------------
# 0     x      x   |0(SLF)|   x      x
#                  --------
#       0      1      2       3      4
unit_test "get" do
  
  objects = [
    ["0", Scene.getCreatorToken],
    ["1", "A"],
    ["2", "B"],
    ["3", "C"],
    ["4", "D"],
    ["5", "E"]
  ]
  scene = Scene.new("", 5, 5, nil)
  scene.addItemToSquare(2, 0, objects[0][0], objects[0][1])
  scene.addItemToSquare(1, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(2, 1, objects[1][0], objects[1][1])
  scene.addItemToSquare(3, 1, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(0, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(1, 2, objects[2][0], objects[2][1])
  scene.addItemToSquare(2, 2, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(3, 2, objects[5][0], objects[5][1])
  scene.addItemToSquare(4, 2, objects[3][0], objects[3][1])
  scene.addItemToSquare(1, 3, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(2, 3, objects[4][0], objects[4][1])
  scene.addItemToSquare(3, 3, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  scene.addItemToSquare(2, 4, Scene.getEmptySquareToken(), Scene.getEmptySquareToken())
  
  model = Chrest.new()
  model.setDomain(GenericDomain.new(model))
  
  object_encoding_time = 1
  empty_square_encoding_time = 1
  access_time = 1
  object_movement_time = 1
  recognised_object_lifespan = 60000 
  unrecognised_object_lifespan = 30000
  visual_spatial_field_creation_time = 0
  
  expected_fixations_made = false
  fixations_expected = [
    [2, 0],
    [2, 1],
    [1, 2],
    [3, 2],
    [4, 2],
    [2, 3]
  ]
  
  until expected_fixations_made
    visual_spatial_field = VisualSpatialField.new(
      model, 
      scene,
      object_encoding_time,
      empty_square_encoding_time, 
      access_time, 
      object_movement_time, 
      recognised_object_lifespan, 
      unrecognised_object_lifespan, 
      20, 
      visual_spatial_field_creation_time, 
      false,
      false
    )
    
    expected_fixations_made = expected_fixations_made?(model, fixations_expected)
  end
  
  ##############################
  ##### INITIAL STATE TEST #####
  ##############################
  
  # Construct expected visual-spatial field data structure
  expected_vsf = []
  for col in 0...visual_spatial_field.getWidth()
    expected_vsf.push([])
    for row in 0...visual_spatial_field.getHeight()
      expected_vsf[col].push([])
    end
  end
  
  # Set two variables used to count the number of empty squares and objects
  # placed so that empty squares and non-creator/blind/empty objects can have
  # their creation and terminus times set correctly.
  number_empty_squares_encoded = 0
  number_objects_encoded = 0
  
  for row in 0...visual_spatial_field.getHeight()
    for col in 0...visual_spatial_field.getWidth()
      
      #Assume this will be a blind square.
      obj_identifier = Scene.getBlindSquareToken()
      obj_class = Scene.getBlindSquareToken()
      created_at = visual_spatial_field_creation_time + access_time
      terminus = nil
      recognised = false
      ghost = false
      
      # Used to control overwriting of created_at and terminus values.
      set_times = false
      
      # Creator check
      if (col == 2 and row == 0) 
        obj_identifier = objects[0][0]
        obj_class = objects[0][1]
      
      # Object check
      elsif(
        (col == 2 and row == 1) or
        (col == 1 and row == 2) or
        (col == 3 and row == 2) or
        (col == 4 and row == 2) or
        (col == 2 and row == 3)
      )
        number_objects_encoded += 1
        set_times = true
        
        if(col == 2 and row == 1) # OBJECT 1(A)
          obj_identifier = objects[1][0]
          obj_class = objects[1][1]
        elsif(col == 1 and row == 2) # OBJECT 2(B)
          obj_identifier = objects[2][0]
          obj_class = objects[2][1]
        elsif(col == 3 and row == 2) # OBJECT 5(E)
          obj_identifier = objects[5][0]
          obj_class = objects[5][1]
        elsif(col == 4 and row == 2) # OBJECT 3(C)
          obj_identifier = objects[3][0]
          obj_class = objects[3][1]
        elsif(col == 2 and row == 3) # OBJECT 4(D)
          obj_identifier = objects[4][0]
          obj_class = objects[4][1]
        end
      
      # Empty square check
      elsif(
        ((row == 1 or row == 3) and (col == 1 or col == 3)) or
        (row == 2 and (col == 0 or col == 2)) or
        (row == 4 and col == 2)
      )
        number_empty_squares_encoded += 1
        set_times = true
        
        obj_identifier = Scene.getEmptySquareToken()
        obj_class = Scene.getEmptySquareToken()
      end
      
      # Overwrite created_at and terminus values if required.
      if(set_times)
        created_at = get_creation_time_for_object_after_visual_spatial_field_creation(
          visual_spatial_field_creation_time,
          access_time,
          object_encoding_time,
          empty_square_encoding_time,
          number_objects_encoded,
          number_empty_squares_encoded
        )
        
        terminus = created_at + unrecognised_object_lifespan
      end
      
      # Push object onto expected data structure at the coordinates indicated.
      expected_vsf[col][row].push([
        obj_identifier,
        obj_class,
        created_at,
        terminus,
        recognised,
        ghost
      ])
      
    end
  end
  
  check_visual_spatial_field_at_time_against_expected(
    model.getAttentionClock, 
    visual_spatial_field, 
    expected_vsf, 
    "after move but before the terminus of any object"
  )
  
  ############################################
  ##### TWO OBJECTS ON COORDINATES CHECK #####
  ############################################
  
  # Perform move so two objects are on same coordinates (4 and 5)
  move = ArrayList.new
  move.add(ItemSquarePattern.new(objects[5][0], 3, 2))
  move.add(ItemSquarePattern.new(objects[5][0], 2, 3))
  moves = ArrayList.new
  moves.add(move)
  time_move_requested = model.getAttentionClock() + 1
  visual_spatial_field.moveObjects(moves, time_move_requested, false)
  
  # Set some relevant times for expected visual-spatial field calculation
  pickup_time = time_move_requested + access_time
  putdown_time = pickup_time + object_movement_time
  
  # Modify the expected visual-spatial field data structure
  expected_vsf[3][2].clear 
  expected_vsf[3][2].push([
    Scene.getEmptySquareToken(),
    Scene.getEmptySquareToken(),
    pickup_time,
    pickup_time + unrecognised_object_lifespan,
    false,
    false
  ])

  expected_vsf[2][3][0][3] = putdown_time + unrecognised_object_lifespan
  expected_vsf[2][3].push([
    objects[5][0],
    objects[5][1],
    putdown_time,
    putdown_time + unrecognised_object_lifespan,
    false,
    false
  ])

  check_visual_spatial_field_at_time_against_expected(
    model.getAttentionClock(), 
    visual_spatial_field,
    expected_vsf,
    "after move but before the terminus of any object"
  )
  
  ###########################################################
  ##### GET SCENE AFTER MOST OBJECT TERMINI HAVE PASSED #####
  ###########################################################
  
  # Objects 4 and 5 will have termini greater than objects 1-3 because they have
  # been interacted with since construction of the visual-spatial field.  So,
  # get the state of the visual-spatial field when the termini for objects 1-3
  # has passed but before the termini for objects 4 and 5.
  
  # Its expected that, any square not occupied by the creator, objects 4 & 5 or 
  # that are blind, should be "unknown" with regard to their object status.
  for col in 0...visual_spatial_field.getWidth()
    for row in 0...visual_spatial_field.getHeight()
      
      if (
        (row == 1 and (col != 0 and col != 4)) or
        (row == 2) or
        (row == 3 and (col == 1 or col == 3)) or
        (row == 4 and col == 2)
      )
        expected_vsf[col][row].clear()
        expected_vsf[col][row].push([
          VisualSpatialFieldObject.getUnknownSquareToken(),
          VisualSpatialFieldObject.getUnknownSquareToken(),
          -1,
          nil,
          false,
          false
        ])
      end
    end
  end
  
  check_visual_spatial_field_at_time_against_expected(
    expected_vsf[2][3][0][3] - 1, #Object 4 terminus - 1
    visual_spatial_field,
    expected_vsf,
    "after move and termini of objects 1-3 but before the termini of objects 4 and 5"
  )
end

################################################################################
# Checks for correct operation of the "VisualSpatialField.getSquareContents()"
# method by performing three sub-tests:
# 
# 1) Get contents of a square not represented in the visual-spatial field.
# 2) Get contents of a square that contains more than one object but only one
#    object is alive at time of content retrieval.
# 3) Get contents of a square at a time when all contained objects have decayed.
# 
# To enable sub-test 2, VisualSpatialFieldObject's must be moved otherwise it is 
# not possible to have two objects co-exist on the same visual-spatial field 
# coordinates.  The state of the visual-spatial field before and after the move
# performed are illustrated below (objects are denoted by their unique 
# identifiers with their class in parenthesis):
# 
# INITIAL SCENE STATE
# ===================
# 
#    ----------------------
# 0  | 0(A) | 1(B) | 2(C) |
#    ---------------------- 
#       0      1      2      COORDINATES
#
# AFETR MOVE
# ==========
# 
#    ----------------------
# 0  | 0(A) | 1(B) |      |
#    |      | 2(C) |      |
#    ----------------------
#       0      1      2      COORDINATES
#
unit_test "get_square_contents" do
  objects = [
    ["0", "A"],
    ["1", "B"],
    ["2", "C"]
  ]
  scene = Scene.new("test", 3, 1, nil)
  scene.addItemToSquare(0, 0, objects[0][0], objects[0][1])
  scene.addItemToSquare(1, 0, objects[1][0], objects[1][1])
  scene.addItemToSquare(2, 0, objects[2][0], objects[2][1])
  
  model = Chrest.new
  model.setDomain(GenericDomain.new(model))
  model.getPerceiver().setFieldOfView(2)
  
  expected_fixations_made = false
  fixations_expected = [
    [0, 0],
    [1, 0],
    [2, 0]
  ]  
  
  until expected_fixations_made
    visual_spatial_field = VisualSpatialField.new(
      model,
      scene, 
      10, 
      5, 
      20, 
      10, 
      60000, 
      30000, 
      20, 
      0,
      false,
      false
    )
    
    expected_fixations_made = expected_fixations_made?(model, fixations_expected)
  end
  
  ######################
  ##### SUB-TEST 1 #####
  ######################
  assert_equal(
    ArrayList.new(), 
    visual_spatial_field.getSquareContents(1, 1, model.getAttentionClock()), 
    "occurred when checking output for coordinates not represented in the visual-spatial field"
  )
  
  ######################
  ##### SUB-TEST 2 #####
  ######################
  
  # Move object 2 onto same square as object 1.
  move = ArrayList.new()
  move.add(ItemSquarePattern.new(objects[2][0], 2, 0))
  move.add(ItemSquarePattern.new(objects[2][0], 1, 0))
  moves = ArrayList.new()
  moves.add(move)
  visual_spatial_field.moveObjects(moves, model.getAttentionClock(), false)
  
  # Set the terminus of object 1 to the model's current attention clock value - 
  # 1.
  vsf = get_entire_visual_spatial_field(visual_spatial_field)
  vsf.get(1).get(0).get(1).setTerminus(model.getAttentionClock() - 1, true)
  
  # Get the contents of coordinates (1, 0) at the time specified by the model's 
  # current attention clock value.
  square_contents = visual_spatial_field.getSquareContents(1, 0, model.getAttentionClock())
  test_description = "after moving object " + objects[2][0] + " onto the same square as " + objects[1][0] + 
    " in the visual-spatial field and getting the square's contents after " + objects[1][0] + "'s terminus has " +
    "been reached but before " + objects[2][0] + "'s has been"
  assert_equal(1, square_contents.size(), "occurred when checking the number of objects returned " + test_description)
  assert_equal(objects[2][0], square_contents.get(0).getIdentifier(), "occurred when checking the identifier of the object returned ")
  assert_equal(objects[2][1], square_contents.get(0).getObjectClass(), "occurred when checking the class of the object returned ")
  
  ######################
  ##### SUB-TEST 3 #####
  ######################
  
  # Determine the maximum terminus for objects on coordinates (1, 0)
  objs = vsf.get(1).get(0)
  max_terminus = 0
  for obj in objs
    if obj.getTerminus() > max_terminus
      max_terminus = obj.getTerminus()
    end
  end
  
  # Get the contents of coordinates (1, 0) at the maximum terminus decided upon
  # above.
  square_contents = visual_spatial_field.getSquareContents(1, 0, max_terminus)
  test_description = "when the contents of coordinates (1, 0) are retrieved at a " + 
    "time when the termini for all objects on these coordinates has passed"
  assert_equal(1, square_contents.size(), "occurred when checking the number of objects returned " + test_description)
  assert_equal(VisualSpatialFieldObject.getUnknownSquareToken(), square_contents.get(0).getIdentifier(), "occurred when checking the identifier of the object returned ")
  assert_equal(VisualSpatialFieldObject.getUnknownSquareToken(), square_contents.get(0).getObjectClass(), "occurred when checking the class of the object returned ")
end

################################################################################
################################################################################
############################## NON-TEST FUNCTIONS ##############################
################################################################################
################################################################################

def get_creation_time_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
  return creation_time + 
    visual_spatial_field_access_time + 
    (number_objects_placed * time_to_encode_objects) + 
    (number_empty_squares_placed * time_to_encode_empty_squares)
end

def get_terminus_for_object_after_visual_spatial_field_creation(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed, object_lifespan)
  return creation_time + 
    visual_spatial_field_access_time + 
    (number_objects_placed * time_to_encode_objects) + 
    (number_empty_squares_placed * time_to_encode_empty_squares) + 
    object_lifespan
end

################################################################################
# To test visual-spatial field construction completely, a number of scenarios 
# need to be modelled and their output checked.  These scenarios must take into 
# consideration:
# 
# 1) All combinations of real/ghost object encoding.
# 2) Occurrence of real/ghost objects in recognised chunks.
# 3) Object class of real/ghost objects.
# 4) Location of real/ghost objects.
# 
# These scenarios are detailed in the table below.  Note that all possible 
# permutations of variables that need to be varied are included in the table 
# below for completeness (despite most not being applicable).
# 
# |--------------------------------------------------------------------------|
# | ================                                                         |
# | ===== NOTE =====                                                         |
# | ================                                                         |
# |                                                                          |
# | 1) If object 1 and 2 occur in the same chunk the pattern specifying      |
# |    object 2 in the chunk comes after the pattern specifying object 1.    |
# | 2) If object 1 and 2 occur in the same chunk and object 1 is a ghost,    |
# |    assume that there is an object, -1, that is real and occurs before    |
# |    object 1. This is because a chunk that specifies a ghost object first |
# |    would never be retrieved during visual-spatial field construction     |
# |    since the ghost does not exist in the Scene (reality) that is scanned |
# |--------------------------------------------------------------------------|
# 
# |------------|--------|--------|-------------|------------|-----------|--------------------------------------------|
# | Scenario # | Obj. 1 | Obj. 2 | Same Chunk? | Obj. Class | Obj. Loc. | Description                                |
# |------------|--------|--------|-------------|------------|-----------|--------------------------------------------|
# | 1          | Real   | Real   | Yes         | =          | =         | IMPOSSIBLE: same object occurs twice in    |
# |            |        |        |             |            |           | the same chunk (no duplicates allowed in   |
# |            |        |        |             |            |           | list patterns to be learned).              |
# | 2          |        |        |             |            | !=        | Objects 1 and 2 are two distinct objects   |
# |            |        |        |             |            |           | (different locations) and are recognised   |
# |            |        |        |             |            |           | at the same time.                          |                                                       |
# | 3          |        |        |             | !=         | =         | IMPOSSIBLE: co-habitation of coordinates   |
# |            |        |        |             |            |           | not supported in Scenes so not possible    |
# |            |        |        |             |            |           | to learn and therefore retrieve a chunk    |
# |            |        |        |             |            |           | specifying two objects on same coordinates |
# |            |        |        |             |            |           | simultaneously.                            |
# | 4          |        |        |             |            | !=        | Objects 1 and 2 are two distinct objects   |
# |            |        |        |             |            |           | (different object class and location) and  |
# |            |        |        |             |            |           | are recognised at the same time.           |
# | 5          |        |        | No          | =          | =         | Objects 1 and 2 are the same (same object  |
# |            |        |        |             |            |           | class and location) and object is          |
# |            |        |        |             |            |           | recognised twice (object present in two    |
# |            |        |        |             |            |           | distinct chunks).                          |
# | 6          |        |        |             |            | !=        | Objects 1 and 2 are two distinct objects   |
# |            |        |        |             |            |           | (different locations) and object 2 is      |
# |            |        |        |             |            |           | recognised after object 1.                 |
# | 7          |        |        |             | !=         | =         | IMPOSSIBLE: objects 1 and 2 are two        |
# |            |        |        |             |            |           | distinct objects (different object class)  |
# |            |        |        |             |            |           | but are located on the same coordinates.   |
# |            |        |        |             |            |           | Since the "realness" of an object is based |
# |            |        |        |             |            |           | upon its existence in reality, this would  |
# |            |        |        |             |            |           | mean that 1 of the objects is actually a   |
# |            |        |        |             |            |           | ghost.                                     |
# | 8          |        |        |             |            | !=        | Objects 1 and 2 are two distinct objects   |
# |            |        |        |             |            |           | (different object class and location) and  |
# |            |        |        |             |            |           | object 2 is recognised after object 1.     |
# | 9          |        | Ghost  | Yes         | =          | =         | IMPOSSIBLE: see scenario 1 description.    |
# |            |        |        |             |            |           | Also, object 2 is not a ghost since 1 is   |
# |            |        |        |             |            |           | real and 1 and 2 are the same.             |
# | 10         |        |        |             |            | !=        | See scenario 2 description.                |
# | 11         |        |        |             | !=         | =         | IMPOSSIBLE: See scenario 3 description.    |
# | 12         |        |        |             |            | !=        | See scenario 4 description.                |
# | 13         |        |        | No          | =          | =         | See scenario 5 description.  Also, object  |
# |            |        |        |             |            |           | 2 is not a ghost since 1 is real and 1 and |
# |            |        |        |             |            |           | 2 are the same.                            |
# | 14         |        |        |             |            | !=        | See scenario 6 description.                |
# | 15         |        |        |             | !=         | =         | Objects 1 and 2 are different (different   |
# |            |        |        |             |            |           | object class) and object 2 is recognised   |
# |            |        |        |             |            |           | after object 1.                            |
# | 16         |        |        |             |            | !=        | See scenario 8 description.                |
# | 17         | Ghost  | Real   | Yes         | =          | =         | IMPOSSIBLE: see scenario 1 description.    |
# |            |        |        |             |            |           | Also, object 1 is not a ghost since 2 is   |
# |            |        |        |             |            |           | real and 1 and 2 are the same.             |
# | 18         |        |        |             |            | !=        | See scenario 2 description.                |
# | 19         |        |        |             | !=         | =         | IMPOSSIBLE: see scenario 3 description.    |
# | 20         |        |        |             |            | !=        | See scenario 4 description.                |
# | 21         |        |        | No          | =          | =         | See scenario 5 description. Also, object   |
# |            |        |        |             |            |           | 1 is not a ghost since 2 is real and 1 and |
# |            |        |        |             |            |           | 2 are the same.                            |
# | 22         |        |        |             |            | !=        | See scenario 6 description.                |
# | 23         |        |        |             | !=         | =         | See scenario 15 description.                |
# | 24         |        |        |             |            | !=        | See scenario 8 description.                |
# | 25         |        | Ghost  | Yes         | =          | =         | IMPOSSIBLE: see scenario 1 description.    |
# | 26         |        |        |             |            | !=        | See scenario 2 description.                |
# | 27         |        |        |             | !=         | =         | IMPOSSIBLE: see scenario 3 description.    |
# | 28         |        |        |             |            | !=        | See scenario 4 description.                |
# | 29         |        |        | No          | =          | =         | See scenario 5 description.                |
# | 30         |        |        |             |            | !=        | See scenario 6 description.                |
# | 31         |        |        |             | !=         | =         | See scenario 15 description.                |
# | 32         |        |        |             |            | !=        | See scenario 8 description.                |
# |------------|--------|--------|-------------|------------|-----------|--------------------------------------------|
#  
# Scenarios 1, 3, 7, 9, 11, 17, 19, 25 and 27 should not be modelled since their 
# occurrence during normal CHREST operation is impossible.  Furthermore, 
# scenarios 13 and 21 are not modelled since the ghost object in these scenarios 
# is actually a real object and this scenario is already modelled as scenario 5.
# 
# The scenarios modelled are listed below (original scenario numbers given in 
# the table above are included in brackets beside the actual scenario 
# numbering).
# 
# For each scenario, the list patterns CHREST is trained with to create the
# scenario are detailed along with the important patterns in these list patterns
# that create the scenario delineated (if not immediately obvious).  Ghost 
# objects are represented by lower case object classes, in the code, their 
# object class will be upper-case.  Individual patterns are denoted by square 
# brackets and chunks are denoted by angled brackets.
# 
# 1(2): 2 real objects with same class but diff. location in same chunk
#       - List pattern(s) used: <[A, 1, 2][A, 1, 3]>
#    
# 2(4): 2 real objects with diff. class and location in same chunk
#       - List pattern(s) used: <[A, 1, 2][B, 1, 3]>
#    
# 3(5): 2 real objects with same class and location in diff. chunks        
#       - List pattern(s) used: <[A, 1, 2][B, 1, 3]><[D, 2, 2][A, 1, 2]>
#       - Pattern 1 in chunk 1 and pattern 2 in chunk 2 create the scenario.
#    
# 4(6): 2 real objects with same class but diff. location in diff. chunks  
#       - List pattern(s) used: <[A, 1, 2][B, 1, 3]><[D, 2, 2][A, 1, 4]>
#       - Pattern 1 in chunk 1 and pattern 2 in chunk 2 create the scenario.
# 
# 5(8): 2 real objects with diff. class and location in diff. chunks
#       - List pattern(s) used: <[A, 1, 2][B, 1, 3]><[D, 2, 2][C, 2, 3]>
#       - All object classes and locations are unique.
# 
# 6(10): Real before ghost with same class but diff. location in same chunk
#        - List pattern(s) used: <[A, 1, 2][a, 1, 3]>
# 
# 7(12): Real before ghost with diff. class and location in same chunk
#        - List pattern(s) used: <[A, 1, 2][b, 1, 3]>
#   
# 8(14): Real before ghost with same class but diff. location in diff. chunks
#        - List pattern(s) used: <[A, 1, 2][b, 1, 3]><[D, 2, 2][a, 2, 4]>
#        - Pattern 1 in chunk 1 and pattern 2 in chunk 2 create the scenario.
# 
# 9(15): Real before ghost with diff. class but same location in diff. chunks
#        - List pattern(s) used: <[A, 1, 2][b, 1, 3]><[D, 2, 2][c, 1, 2]>
#        - Pattern 1 in chunk 1 and pattern 2 in chunk 2 create the scenario.
# 
# 10(16): Real before ghost with diff. class and location in diff. chunks
#         - List pattern(s) used: <[A, 1, 2][B, 1, 3]><[D, 2, 2][c, 2, 3]>
#         - All object classes and locations are unique and real objects come
#           before the ghost object.
#    
# NOTE: In remaining scenarios, the list patterns don't just consist of a 
#       ghost and real/ghost, i.e. in some cases, there aren't just two 
#       patterns in a chunk.  This is because a ghost can't be the first 
#       pattern in a list pattern to be learned since the chunk created would 
#       never be retrieved. The only way chunks can be retrieved in these 
#       scenarios is by CHREST scanning reality for recognised objects; ghosts 
#       aren't present in reality so the test-links leading to a chunk that has 
#       a ghost object as its first pattern would never be traversed in LTM.
#    
# 11(18): Ghost before real with same class but diff. location in same chunk
#         - List pattern(s) used: <[A, 1, 2][b, 1, 3][B, 3, 2]>
#         - Patterns 2 and 3 in the chunk create the scenario.
# 
# 12(20): Ghost before real with diff. class and location in same chunk
#         - List pattern(s) used: <[A, 1, 2][b, 1, 3][C, 3, 2]>
#         - Patterns 2 and 3 in the chunk create the scenario.
#   
# 13(22): Ghost before real with same class but diff. location in diff. chunks
#         - List pattern(s) used: <[A, 1, 2][b, 1, 3]><[B, 3, 2][C, 2, 4]>
#         - Pattern 2 in chunk 1 and pattern 1 in chunk 2 create the scenario.
# 
# 14(23): Ghost before real with diff. class but same location in diff. chunks
#         - List pattern(s) used: <[A, 1, 2][b, 1, 3]><[B, 3, 2][D, 1, 3]>
#         - Pattern 2 in chunk 1 and pattern 2 in chunk 2 create the scenario.
# 
# 15(24): Ghost before real with diff. class and location in diff. chunks
#         - List pattern(s) used: <[A, 1, 2][b, 1, 3]><[C, 3, 2][D, 2, 4]>
#         - No pattern in chunk 2 has same object class or location as the
#           ghost object represented in pattern 2 of chunk 1.
# 
# 16(26): 2 ghosts with same class but diff. location in same chunk
#         - List pattern(s) used: <[A, 1, 2][b, 1, 3][b, 3, 2]>
#    
# 17(28): 2 ghosts with diff. class and location in same chunk
#         - List pattern(s) used: <[A, 1, 2][b, 1, 3][c, 3, 2]>
# 
# 18(29): 2 ghosts with same class and location in diff. chunks
#         - List pattern(s) used: <[A, 1, 2][b, 1, 3]><[D, 2, 2][b, 1, 3]>
#         - Pattern 2 in chunks 1 and 2 create the scenario.
#    
# 19(62): 2 ghosts with same class but diff. location in diff. chunks
#         - List pattern(s) used: <[A, 1, 2][b, 1, 3]><[D, 2, 2][b, 2, 4]>
#         - Pattern 2 in chunks 1 and 2 create the scenario.
#    
# 20(63): 2 ghosts with diff. class but same location in diff. chunks
#         - List pattern(s) used: <[A, 1, 2][b, 1, 3]><[D, 2, 2][c, 1, 3]>
#         - Pattern 2 in chunks 1 and 2 create the scenario.
# 
# 21(64): 2 ghosts with diff. class and location in diff. chunks
#         - List pattern(s) used: <[A, 1, 2][b, 1, 3]><[D, 2, 2][c, 2, 4]>
# 
# In addition, another 3 scenarios are modelled:
# 
# 22: A ghost object and blind square in reality occupy the same coordinates.
# 23: A ghost object and an empty square in reality occupy the same coordinates.
# 24: A ghost object and an unrecognised non-empty object occupies the same
#     coordinates.
#     
# Finally, if the scene creator should be present in reality and ghost objects
# are to be encoded, one additional scenario is encoded to ensure that the 
# creator's avatar overwrites the ghost object:
# 
# 25: A ghost object occupies the same coordinates as the scene creator.
#
# Note that correct encoding of squares that are blind, empty or occupied by an 
# unrecognised object is tested in each scenario modelled.
def get_visual_spatial_field_construction_scenario_data(
    encode_scene_creator, 
    encode_ghost_objects, 
    time_to_encode_objects, 
    time_to_encode_empty_squares,
    recognised_object_lifespan
  )
  
  # This data structure will be populated with the following data for each 
  # scenario and returned:
  # 
  # 1) The reality that has been created and should be used by the CHREST model
  #    being tested.
  # 2) The list-patterns to learn by the CHREST model being tested.
  # 3) The number of chunks that should be recognised by the CHREST model being
  #    tested.
  # 4) The basic expected values for the objects on the visual spatial field 
  #    that should be constructed, i.e. expected ID, object class, recognised 
  #    status and ghost status of all blind objects on all squares (the 
  #    visual-spatial field is entirely blind when first constructed).  This is
  #    provided for convenience.
  # 5) The jchrest.lib.Scene instances that are used by the 
  #    "add_expected_values_for_unrecognised_visual_spatial_objects" to skip 
  #    over squares that contain recognised real objects when adding expected 
  #    values for unrecognised objects on visual-spatial squares.
  # 6) The number of unrecognised objects present in reality.
  # 7) The number of empty squares present in reality.
  # 8) The coordinates (col, row) of squares containing unrecognised objects 
  #    that should be fixated on.
  scenario_data = Array.new
  
  max_scenario = 24
  if(encode_scene_creator and encode_ghost_objects)
    max_scenario = 25
  end
  
  for scenario in 1..max_scenario
    
    # Create reality and populate with empty squares.  The reality created and 
    # used contains blind and empty squares in an elaborate diamond shape.  This
    # provides a difficult and rich test environment to test for correct 
    # operation of the visual-spatial field constructor mechanism.
    # 
    # For each scenario, reality is embellished with additional objects that 
    # may be recognised/unrecognised (there are always 2 unrecognised objects 
    # added to ensure that unrecognised object encoding is performed 
    # successfully).  The initial state of reality is illustrated below, if the
    # "encode_scene_creator" parameter is set to true, the relevant creator 
    # avatar (see Scene.getCreatorToken()) is added to the centre of reality at
    # coordinates (2, 2):
    #
    #                -------
    # 4     x     x  |     |  x     x
    #          ------------------- 
    # 3     x  |     |     |     |  x
    #    -------------------------------
    # 2  |     |     |     |     |     |
    #    -------------------------------
    # 1     x  |     |     |     |  x
    #          -------------------
    # 0     x     x  |     |  x     x
    #                -------
    #       0     1     2     3     4     COORDINATES
    #
    # ==================
    # ===== LEGEND =====
    # ==================
    # 
    # - "x": blind square
    # 
    #   -------
    # - |     | : empty square
    #   -------
    # 
    reality = Scene.new("Reality", 5, 5, nil)
    reality.addItemToSquare(2, 0, "", Scene.getEmptySquareToken())
    reality.addItemToSquare(1, 1, "", Scene.getEmptySquareToken())
    reality.addItemToSquare(2, 1, "", Scene.getEmptySquareToken())
    reality.addItemToSquare(3, 1, "", Scene.getEmptySquareToken())
    reality.addItemToSquare(0, 2, "", Scene.getEmptySquareToken())
    reality.addItemToSquare(1, 2, "", Scene.getEmptySquareToken())
    reality.addItemToSquare(3, 2, "", Scene.getEmptySquareToken())
    reality.addItemToSquare(4, 2, "", Scene.getEmptySquareToken())
    reality.addItemToSquare(1, 3, "", Scene.getEmptySquareToken())
    reality.addItemToSquare(2, 3, "", Scene.getEmptySquareToken())
    reality.addItemToSquare(3, 3, "", Scene.getEmptySquareToken())
    reality.addItemToSquare(2, 4, "", Scene.getEmptySquareToken())
    
    #Encode scene creator avatar, if specified.
    if(encode_scene_creator) 
      reality.addItemToSquare(2, 2, "00", Scene.getCreatorToken())
    else
      reality.addItemToSquare(2, 2, "", Scene.getEmptySquareToken())
    end
    
    # Initialise the data structure used to store what list patterns should be
    # learned by the CHREST model being tested for this scenario.
    list_patterns_to_learn = Array.new
    
    # Initialise the data structure used to indicate how many chunks should be
    # recognised when the CHREST model being tested scans the reality 
    # constructed by this scenario when constructing its visual-spatial
    # field.
    number_recognised_chunks = 0
    
    # Create the data structure that stores the basic expected values for 
    # VisualSpatialFieldObjects on the visual spatial field that should be 
    # constructed when the CHREST model uses the reality specified by this 
    # scenario. As mentioned above, the first VisualSpatialFieldObject on each 
    # coordinate is expected to be a blind square whose creation and terminus 
    # times are not yet known since the actual creation time of the 
    # VisualSpatialField to test is not  known.
    expected_visual_spatial_field_object_properties = Array.new
    for col in 0...reality.getWidth()
      expected_visual_spatial_field_object_properties.push(Array.new)
      for row in 0...reality.getHeight()
        expected_visual_spatial_field_object_properties[col].push(Array.new)
        expected_visual_spatial_field_object_properties[col][row].push([
          Scene.getBlindSquareToken, #Expected ID
          Scene.getBlindSquareToken, #Expected class
          0, #Expected creation time.
          nil, #Expected lifespan (not exact terminus) of the object.
          false, #Expected recognised status
          false # Expected ghost status
        ])
      end
    end
    
    squares_to_be_ignored = Array.new
    number_unrecognised_objects = 0
    number_empty_squares = 0
    squares_to_fixate_on = Array.new
    
    ############################################################################
    if scenario == 1
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |     |  x     x
      #          ------------------- 
      # 3     x  |  A  |  G  |  F  |  x
      #    -------------------------------
      # 2  |     |  A  |     |     |     |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |     |  x     x
      #                -------
      #       0     1     2     3     4
      #
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][A, 1, 3]>
      # 
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, the same 
      # outcome should always be produced for this scenario.  Two distinct, 
      # recognised VisualSpatialFieldObject instances should be encoded (to differentiate 
      # between the "A" objects, the second "A" object will be referred to as 
      # "A*").
      # 
      # - Creation times
      #   ~ A/A*: The first occurrence of "A" and "A*" is in the first chunk 
      #           processed so both objects are encoded at the same time (when 
      #           the first chunk is processed).
      #           
      # - Terminus times
      #   ~ A/A*: The last occurrence of "A" and "A*" is in the first chunk 
      #           processed and no other objects (recognised or unrecognised) 
      #           overwrite them.  Therefore, their lifespan will be set to the 
      #           lifespan specified for recognised objects.
      # 
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      #     
      # Blind objects on coordinates (1, 2) and (1, 3) should be overwritten at 
      # the same time (when the chunk is processed) so their termini should be 
      # equal.
      
      # Create list patterns to learn.
      list_pattern = ListPattern.new
      list_pattern.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern.add(ItemSquarePattern.new("A", 1, 3))
      list_patterns_to_learn.push(list_pattern)
      
      # All objects learned should be real so add them to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(1, 3, "1", "A")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(3, 3, "2", "F")
      reality.addItemToSquare(2, 3, "3", "G")

      #Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0", 
        "A", 
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])

      expected_visual_spatial_field_object_properties[1][3].push([
        "1", 
        "A", 
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])

      #Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(1, 3))

      #Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = (time_to_encode_objects)
      expected_visual_spatial_field_object_properties[1][3][0][3] = (time_to_encode_objects)
      
      number_recognised_chunks = 1
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 8 : 9
      squares_to_fixate_on = [
        [3, 3],
        [2, 3]
      ]
             
    ############################################################################
    elsif scenario == 2
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |     |  x     x
      #          ------------------- 
      # 3     x  |  B  |     |  F  |  x
      #    -------------------------------
      # 2  |     |  A  |     |     |  G  |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |     |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][B, 1, 3]>
      # 
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, the same 
      # outcome should always be produced for this scenario.  Two distinct, 
      # recognised VisualSpatialFieldObject instances should be encoded.
      #   
      # - Creation times
      #   ~ A/B: The first occurrence of "A" and "B" is in the first chunk 
      #          processed so both objects are encoded at the same time (when 
      #          the first chunk is processed).
      #          
      # - Terminus times
      #   ~ A/B: The last occurrence of "A" and "B" is in the first chunk 
      #          processed and no other objects (recognised or unrecognised) 
      #          overwrite them.  Therefore, their lifespan will be set to the 
      #          lifespan specified for recognised objects.
      #      
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # Blind objects on coordinates (1, 2) and (1, 3) should be overwritten at 
      # the same time (when the chunk is processed) so their termini should be 
      # equal.
      
      # Create list patterns to learn.
      list_pattern = ListPattern.new
      list_pattern.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern.add(ItemSquarePattern.new("B", 1, 3))
      list_patterns_to_learn.push(list_pattern)
      
      # All objects learned should be real so add them to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(1, 3, "1", "B")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(3, 3, "2", "F")
      reality.addItemToSquare(4, 2, "3", "G")

      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0", 
        "A", 
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])

      expected_visual_spatial_field_object_properties[1][3].push([
        "1", 
        "B", 
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])

      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(1, 3))

      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = (time_to_encode_objects)
      expected_visual_spatial_field_object_properties[1][3][0][3] = (time_to_encode_objects)
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 1
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 8 : 9
      squares_to_fixate_on = [
        [3, 3],
        [4, 2]
      ]
 
    ############################################################################
    elsif scenario == 3
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |  F  |  x     x
      #          ------------------- 
      # 3     x  |  B  |  D  |  G  |  x
      #    -------------------------------
      # 2  |     |  A  |     |     |     |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |     |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][B, 1, 3]><[D, 2, 3][A, 1, 2]>
      # 
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      #       
      # No matter what the "encode_ghost_objects" parameter is set to, the same 
      # outcome should always be produced for this scenario.  Three distinct, 
      # recognised VisualSpatialFieldObject instances should be encoded (the "A" objects 
      # recognised are actually the same object).
      # 
      # - Creation times
      #   ~ A/B: The first occurrence of "A" and "B" is in the first chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the first chunk is processed). 
      #   ~ D: The first occurrence of "D" is in the second chunk processed 
      #        so it is encoded when the second chunk is processed.
      #        
      # - Terminus times
      #   ~ B: The last occurrence of "B" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrites it.  
      #        Therefore, its lifespan will be set to the lifespan specified 
      #        for recognised objects.
      #   ~ A/D: The last occurrence of "A" and "D" is in the second chunk 
      #          processed and no other objects (recognised or 
      #          unrecognised) overwrite them.  Therefore, their lifespan 
      #          will be set to the lifespan specified for recognised 
      #          objects.  With regards to "A", its terminus will be extended 
      #          if its current terminus has not been reached when the second 
      #          chunk is encoded (timing parameters provided to 
      #          visual-spatial field construction method may prevent this if 
      #          changed from their original values).
      #      
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # Blind objects on (1, 2) and (1, 3) should be overwritten at the same
      # time (when the first chunk is processed) so their termini should be
      # equal.  The blind object on (2, 3) should be overwritten when the 
      # second chunk is processed.
      
      # Create list patterns to learn.
      list_pattern_1 = ListPattern.new
      list_pattern_1.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern_1.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern_2 = ListPattern.new
      list_pattern_2.add(ItemSquarePattern.new("D", 2, 3))
      list_pattern_2.add(ItemSquarePattern.new("A", 1, 2))
      list_patterns_to_learn.push(list_pattern_1)
      list_patterns_to_learn.push(list_pattern_2)
      
      # All objects learned should be real so add them to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(1, 3, "1", "B")
      reality.addItemToSquare(2, 3, "2", "D")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(2, 4, "3", "F")
      reality.addItemToSquare(3, 3, "4", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0", 
        "A", 
        time_to_encode_objects,
        time_to_encode_objects + recognised_object_lifespan,
        true,
        false
      ])
    
      expected_visual_spatial_field_object_properties[1][3].push([
        "1", 
        "B", 
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
      
      expected_visual_spatial_field_object_properties[2][3].push([
        "2", 
        "D", 
        time_to_encode_objects * 2,
        recognised_object_lifespan,
        true,
        false
      ])
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(1, 3))
      squares_to_be_ignored.push(Square.new(2, 3))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[2][3][0][3] = (time_to_encode_objects * 2)
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 2
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 7 : 8
      squares_to_fixate_on = [
        [2, 4],
        [3, 3]
      ]
      
    ############################################################################
    elsif scenario == 4
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |  A  |  x     x
      #          ------------------- 
      # 3     x  |  B  |  F  |     |  x
      #    -------------------------------
      # 2  |     |  A  |     |  D  |     |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |  G  |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][B, 1, 3]><[D, 3, 2][A, 2, 4]>
      #       
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, the same 
      # outcome should always be produced for this scenario.  Four distinct, 
      # recognised VisualSpatialFieldObject instances should be encoded (to differentiate 
      # between the "A" objects, the second "A" object will be referred to as 
      # "A*").
      # 
      # - Creation times
      #   ~ A/B: The first occurrence of "A" and "B" is in the first chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the first chunk is processed). 
      #   ~ D/A*: The first occurrence of "D" and "A*" is in the second 
      #           chunk processed so both objects are encoded at the same 
      #           time (when the second chunk is processed). 
      #           
      # - Terminus times
      #   ~ A/B: The last occurrence of "A" and "B" is in the first chunk 
      #          processed and no other objects (recognised or 
      #          unrecognised) overwrite them.  Therefore, their lifespan 
      #          will be set to the lifespan specified for recognised 
      #          objects.
      #   ~ D/A*: The last occurrence of "D" and "A*" is in the second chunk 
      #           processed and no other objects (recognised or 
      #           unrecognised) overwrite them.  Therefore, their lifespan 
      #           will be set to the lifespan specified for recognised 
      #           objects.
      #      
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # Blind objects on (1, 2) and (1, 3) should be overwritten at the same
      # time (when the first chunk is processed) so their termini should be
      # equal.  Blind objects on (3, 2) and (1, 4) should also be 
      # overwritten at the same time (when the second chunk is processed) so 
      # their termini should be equal.
      
      # Create list patterns to learn.
      list_pattern_1 = ListPattern.new
      list_pattern_1.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern_1.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern_2 = ListPattern.new
      list_pattern_2.add(ItemSquarePattern.new("D", 3, 2))
      list_pattern_2.add(ItemSquarePattern.new("A", 2, 4))
      list_patterns_to_learn.push(list_pattern_1)
      list_patterns_to_learn.push(list_pattern_2)
      
      # All objects learned should be real so add them to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(1, 3, "1", "B")
      reality.addItemToSquare(3, 2, "2", "D")
      reality.addItemToSquare(2, 4, "3", "A")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(2, 3, "4", "F")
      reality.addItemToSquare(2, 0, "5", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      expected_visual_spatial_field_object_properties[1][3].push([
        "1",
        "B",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      expected_visual_spatial_field_object_properties[3][2].push([
        "2",
        "D",
        (time_to_encode_objects * 2),
        recognised_object_lifespan,
        true,
        false
      ])
      
      expected_visual_spatial_field_object_properties[2][4].push([
        "3",
        "A",
        (time_to_encode_objects * 2),
        recognised_object_lifespan,
        true,
        false
      ])
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(1, 3))
      squares_to_be_ignored.push(Square.new(3, 2))
      squares_to_be_ignored.push(Square.new(2, 4))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[3][2][0][3] = (time_to_encode_objects * 2)
      expected_visual_spatial_field_object_properties[2][4][0][3] = (time_to_encode_objects * 2)
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 2
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 6 : 7
      squares_to_fixate_on = [
        [2, 3],
        [2, 0]
      ]
      
    ############################################################################
    elsif scenario == 5
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |     |  x     x
      #          ------------------- 
      # 3     x  |  B  |  C  |  F  |  x
      #    -------------------------------
      # 2  |     |  A  |     |     |  G  |
      #    -------------------------------
      # 1     x  |     |     |  D  |  x
      #          -------------------
      # 0     x     x  |     |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][B, 1, 3]><[D, 3, 1][C, 2, 3]>
      #       
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, the same 
      # outcome should always be produced for this scenario.  Four distinct, 
      # recognised VisualSpatialFieldObject instances should be encoded.
      # 
      # - Creation times
      #   ~ A/B: The first occurrence of "A" and "B" is in the first chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the first chunk is processed).  
      #   ~ D/C: The first occurrence of "D" and "C" is in the second 
      #          chunk processed so both objects are encoded at the same 
      #          time (when the second chunk is processed).  
      #          
      # - Terminus times
      #   ~ A/B: The last occurrence of "A" and "B" is in the first chunk 
      #          processed and no other objects (recognised or 
      #          unrecognised) overwrite them.  Therefore, their lifespan 
      #          will be set to the lifespan specified for recognised 
      #          objects.
      #   ~ D/C: The last occurrence of "D" and "C" is in the second chunk 
      #          processed and no other objects (recognised or 
      #          unrecognised) overwrite them.  Therefore, their lifespan 
      #          will be set to the lifespan specified for recognised 
      #          objects.
      #
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # Blind objects on (1, 2) and (1, 3) should be overwritten at the same
      # time (when the first chunk is processed).  The blind objects on 
      # (3, 1) and (2, 3) should also be overwritten at the same time (when 
      # the second chunk is processed).
      
      # Create list patterns to learn.
      list_pattern_1 = ListPattern.new
      list_pattern_1.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern_1.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern_2 = ListPattern.new
      list_pattern_2.add(ItemSquarePattern.new("D", 3, 1))
      list_pattern_2.add(ItemSquarePattern.new("C", 2, 3))
      list_patterns_to_learn.push(list_pattern_1)
      list_patterns_to_learn.push(list_pattern_2)
      
      # All objects learned should be real so add them to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(1, 3, "1", "B")
      reality.addItemToSquare(3, 1, "2", "D")
      reality.addItemToSquare(2, 3, "3", "C")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(3, 3, "4", "F")
      reality.addItemToSquare(4, 2, "5", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      expected_visual_spatial_field_object_properties[1][3].push([
        "1",
        "B",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      expected_visual_spatial_field_object_properties[3][1].push([
        "2",
        "D",
        (time_to_encode_objects * 2),
        recognised_object_lifespan,
        true,
        false
      ])
    
      expected_visual_spatial_field_object_properties[2][3].push([
        "3",
        "C",
        (time_to_encode_objects * 2),
        recognised_object_lifespan,
        true,
        false
      ])
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(1, 3))
      squares_to_be_ignored.push(Square.new(3, 1))
      squares_to_be_ignored.push(Square.new(2, 3))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[3][1][0][3] = (time_to_encode_objects * 2)
      expected_visual_spatial_field_object_properties[2][3][0][3] = (time_to_encode_objects * 2)
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 2
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 6 : 7
      
      squares_to_fixate_on = [
        [3, 3],
        [4, 2]
      ]
           
    ############################################################################
    elsif scenario == 6
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |     |  x     x
      #          ------------------- 
      # 3     x  |     |     |     |  x
      #    -------------------------------
      # 2  |     |  A  |     |  F  |  G  |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |     |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][a, 1, 3]>
      # 
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, one 
      # distinct, recognised VisualSpatialFieldObject instance for object "A" should be 
      # encoded.  If the "encode_ghost_objects" parameter is set to true, an
      # additional recognised VisualSpatialFieldObject instance for object "a" should be
      # encoded.
      # 
      # - ID
      #   ~ a: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/a:  The first occurrence of "A" and "a" is in the first chunk 
      #           processed so both objects are encoded at the same time 
      #           (when the first chunk is processed).
      #           
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrite it.  
      #        Therefore, its lifespan will be set to the lifespan specified 
      #        for recognised objects.
      #   ~ a: The last occurrence of "a" is in the first chunk processed but
      #        "a" exists on a square that is empty in reality, so it will be 
      #        overwritten. Therefore, the lifespan of "a" will be set to the 
      #        time taken to encode another two unrecognised objects ("F" and 
      #        "G") plus six/seven empty squares (six if the scene creator is 
      #        encoded).
      #      
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # The blind object on (1, 2) should be overwritten when the first chunk 
      # is processed.  If ghost objects are to be encoded, the blind object 
      # on (1, 3) should also be overwritten at the same time.
      
      # Create list patterns to learn.
      list_pattern = ListPattern.new
      list_pattern.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern.add(ItemSquarePattern.new("A", 1, 3))
      list_patterns_to_learn.push(list_pattern)
      
      # Only the first object learned should be real so add it to reality.
      reality.addItemToSquare(1, 2, "0", "A")

      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(3, 2, "1", "F")
      reality.addItemToSquare(4, 2, "2", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "A",
          time_to_encode_objects,
          (time_to_encode_objects * 2) + (time_to_encode_empty_squares * (encode_scene_creator ? 6 : 7)),
          true,
          true,
        ])
      end
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 1
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 9 : 10
      
      squares_to_fixate_on = [
        [3, 2],
        [4, 2]
      ]
    
    ############################################################################
    elsif scenario == 7
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |     |  x     x
      #          ------------------- 
      # 3     x  |     |  G  |     |  x
      #    -------------------------------
      # 2  |     |  A  |     |  F  |     |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |     |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][b, 1, 3]>
      # 
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, one 
      # distinct, recognised VisualSpatialFieldObject instance for object "A" should be 
      # encoded.  If the "encode_ghost_objects" parameter is set to true, an
      # additional recognised VisualSpatialFieldObject instance for object "b" should be
      # encoded.
      # 
      # - ID
      #   ~ b: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/b:  The first occurrence of "A" and "b" is in the first chunk 
      #           processed so both objects are encoded at the same time 
      #           (when the first chunk is processed).
      #           
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrite it.  
      #        Therefore, its lifespan will be set to the lifespan specified 
      #        for recognised objects.
      #   ~ b: The last occurrence of "b" is in the first chunk processed but
      #        "b" exists on a square that is empty in reality, so it will be 
      #        overwritten. Therefore, the lifespan of "b" will be set to the 
      #        time taken to encode another unrecognised object ("F") plus 
      #        seven/eight empty squares (seven if the scene creator is 
      #        encoded).
      #      
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # The blind object on (1, 2) should be overwritten when the first chunk 
      # is processed.  If ghost objects are to be encoded, the blind object 
      # on (1, 3) should also be overwritten at the same time.
      
      # Create list patterns to learn.
      list_pattern = ListPattern.new
      list_pattern.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern.add(ItemSquarePattern.new("B", 1, 3))
      list_patterns_to_learn.push(list_pattern)
      
      # Only the first object learned should be real so add it to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(3, 2, "1", "F")
      reality.addItemToSquare(2, 3, "2", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "B",
          time_to_encode_objects,
          time_to_encode_objects + (time_to_encode_empty_squares * (encode_scene_creator ? 7 : 8)),
          true,
          true,
        ])
      end
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 1
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 9 : 10
      
      squares_to_fixate_on = [
        [3, 2],
        [2, 3]
      ]
      
    ############################################################################
    elsif scenario == 8
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |  G  |  x     x
      #          ------------------- 
      # 3     x  |     |     |  F  |  x
      #    -------------------------------
      # 2  |     |  A  |     |     |     |
      #    -------------------------------
      # 1     x  |     |     |  D  |  x
      #          -------------------
      # 0     x     x  |     |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][b, 1, 3]><[D, 3, 1][a, 1, 1]>
      #       
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, two 
      # distinct, recognised VisualSpatialFieldObject instances for objects "A" and "D" 
      # should be encoded.  If the "encode_ghost_objects" parameter is set to 
      # true, two additional recognised VisualSpatialFieldObject instances for objects "b"
      # and "a" should be encoded.
      # 
      # - ID
      #   ~ b: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #   ~ a: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "1" appended 
      #        since it is the second ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/b: The first occurrence of "A" and "b" is in the first chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the first chunk is processed).
      #   ~ D/a: The first occurrence of "D" and "a" is in the second chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the second chunk is processed).
      #          
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrite it.  
      #        Therefore, its lifespan will be set to the lifespan specified 
      #        for recognised objects.
      #   ~ b: The last occurrence of "b" is in the first chunk processed but
      #        "b" exists on a square that is empty in reality, so it will be 
      #        overwritten. Therefore, the lifespan of "b" will be set to the 
      #        time taken to encode the second chunk plus eight/nine empty 
      #        squares (eight if the scene creator is encoded).
      #   ~ D: The last occurrence of "D" is in the second chunk processed 
      #        and no other objects (recognised or unrecognised) overwrite 
      #        it.  Therefore, its lifespan will be set to the lifespan 
      #        specified for recognised objects.
      #   ~ a: The last occurrence of "a" is in the second chunk processed 
      #        but "a" exists on a square that is empty in reality, so it 
      #        will be overwritten. Therefore, the lifespan of "a" will be 
      #        set to the time taken to encode two empty squares (the 
      #        unrecognised objects and scene creator are not present on any 
      #        squares before (1, 1) is processed).
      #      
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # Blind objects on (1, 2) and (3, 1) should be overwritten when the first 
      # and second chunk, respectively, are processed.  If ghost objects are to
      # be encoded, blind objects on (1, 3) and (1, 1) should be overwritten 
      # when the first and second chunk, respectively, are processed.
      
      # Create list patterns to learn.
      list_pattern_1 = ListPattern.new
      list_pattern_1.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern_1.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern_2 = ListPattern.new
      list_pattern_2.add(ItemSquarePattern.new("D", 3, 1))
      list_pattern_2.add(ItemSquarePattern.new("A", 1, 1))
      list_patterns_to_learn.push(list_pattern_1)
      list_patterns_to_learn.push(list_pattern_2)
      
      # Only the first object learned in each list pattern should be real so add 
      # these to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(3, 1, "1", "D")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(3, 3, "2", "F")
      reality.addItemToSquare(2, 4, "3", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "B",
          time_to_encode_objects,
          time_to_encode_objects + (time_to_encode_empty_squares * (encode_scene_creator ? 7 : 8)),
          true,
          true,
        ])
      end
    
      expected_visual_spatial_field_object_properties[3][1].push([
        "1",
        "D",
        time_to_encode_objects * 2,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][1].push([
          VisualSpatialField.getGhostObjectIdPrefix + "1",
          "A",
          time_to_encode_objects * 2,
          (time_to_encode_empty_squares * 2),
          true,
          true
        ])
      end
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(3, 1))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[3][1][0][3] = (time_to_encode_objects * 2)
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
        expected_visual_spatial_field_object_properties[1][1][0][3] = (time_to_encode_objects * 2)
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 2
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 8 : 9
      squares_to_fixate_on = [
        [3, 3],
        [2, 4]
      ]
      
    ############################################################################
    elsif scenario == 9
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |     |  x     x
      #          ------------------- 
      # 3     x  |     |  D  |     |  x
      #    -------------------------------
      # 2  |     |  A  |     |  F  |     |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |  G  |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][b, 1, 3]><[D, 2, 3][c, 1, 2]>
      #       
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, two 
      # distinct, recognised VisualSpatialFieldObject instances for objects "A" and "D" 
      # should be encoded.  If the "encode_ghost_objects" parameter is set to 
      # true, one additional recognised VisualSpatialFieldObject instance for object "b"
      # should be encoded.  Object "c" is not encoded since it occupies the same 
      # visual-spatial coordinates as object "A" so, since "c" is a ghost object 
      # and "A" is not, despite "c" being recognised more recently than "A", 
      # ghost objects can not overwrite real objects.
      # 
      # - ID
      #   ~ b: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #   ~ c: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/b: The first occurrence of "A" and "b" is in the first chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the first chunk is processed).
      #   ~ D: The first occurrence of "D" is in the second chunk processed 
      #        so it is encoded when the second chunk is processed.
      #        
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrite it.
      #        However, if ghost objects are to be encoded: "c" occupies 
      #        the same coordinates as "A" so "A"s lifespan will be updated so
      #        that it is equal to "D"s (if its current terminus has not been 
      #        reached when the second chunk is encoded depending upon timing 
      #        parameters provided to the visual-spatial field when it is 
      #        initialised).
      #        Otherwise, if ghost objects are not to be encoded, "A"s lifespan
      #        will be set to the lifespan specified for recognised objects.
      #   ~ b: The last occurrence of "b" is in the first chunk processed but
      #        "b" exists on a square that is empty in reality, so it will be 
      #        overwritten. Therefore, the lifespan of "b" will be set to the 
      #        time taken to encode the second chunk, two unrecognised 
      #        objects ("F" and "G") plus six/seven empty squares (six if the 
      #        scene creator is encoded).
      #   ~ D: The last occurrence of "D" is in the second chunk processed 
      #        and no other objects (recognised or unrecognised) overwrite 
      #        it.  Therefore, its lifespan will be set to the lifespan 
      #        specified for recognised objects.
      #      
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # Blind objects on (1, 2) and (2, 3) should be overwritten when the first 
      # and second chunk, respectively, are processed.  If ghost objects are to
      # be encoded, the blind object on (1, 3) should be overwritten when the
      # first chunk is processed.
      
      # Create list patterns to learn.
      list_pattern_1 = ListPattern.new
      list_pattern_1.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern_1.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern_2 = ListPattern.new
      list_pattern_2.add(ItemSquarePattern.new("D", 2, 3))
      list_pattern_2.add(ItemSquarePattern.new("C", 1, 2))
      list_patterns_to_learn.push(list_pattern_1)
      list_patterns_to_learn.push(list_pattern_2)
      
      # Only the first object learned in each list pattern should be real so add 
      # these to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(2, 3, "1", "D")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(3, 2, "2", "F")
      reality.addItemToSquare(2, 0, "3", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        (encode_ghost_objects ? (time_to_encode_objects + recognised_object_lifespan) : recognised_object_lifespan),
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "B",
          time_to_encode_objects,
          (time_to_encode_objects * 3) + (time_to_encode_empty_squares * (encode_scene_creator ? 6 : 7)),
          true,
          true,
        ])
      end
    
      expected_visual_spatial_field_object_properties[2][3].push([
        "1",
        "D",
        time_to_encode_objects * 2,
        recognised_object_lifespan,
        true,
        false
      ])

      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(2, 3))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[2][3][0][3] = (time_to_encode_objects * 2)
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 2
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 8 : 9
      squares_to_fixate_on = [
        [3, 2],
        [2, 0]
      ]
      
    ############################################################################
    elsif scenario == 10
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |     |  x     x
      #          ------------------- 
      # 3     x  |  B  |     |     |  x
      #    -------------------------------
      # 2  |     |  A  |     |  F  |     |
      #    -------------------------------
      # 1     x  |     |  D  |  G  |  x
      #          -------------------
      # 0     x     x  |     |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][B, 1, 3]><[D, 2, 1][c, 2, 3]>
      #       
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, three 
      # distinct, recognised VisualSpatialFieldObject instances for objects "A", "B" and 
      # "D" should be encoded.  If the "encode_ghost_objects" parameter is set 
      # to true, one additional recognised VisualSpatialFieldObject instance for object 
      # "c" should be encoded. 
      # 
      # - ID
      #   ~ c: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/B: The first occurrence of "A" and "B" is in the first chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the first chunk is processed).
      #   ~ D/c: The first occurrence of "D" and "c" is in the second chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the second chunk is processed).
      #          
      # - Terminus times
      #   ~ A/B: The last occurrences of "A" and "B" are in the first chunk 
      #          processed and no other objects (recognised or unrecognised) 
      #          overwrite them.  Therefore, their lifespan will be set to 
      #          the lifespan specified for recognised objects.
      #   ~ D: The last occurrence of "D" is in the second chunk processed 
      #        and no other objects (recognised or unrecognised) overwrite 
      #        it.  Therefore, its lifespan will be set to the lifespan 
      #        specified for recognised objects.
      #   ~ c: The last occurrence of "c" is in the second chunk processed 
      #        but "c" exists on a square that is empty in reality, so it 
      #        will be overwritten. Therefore, the lifespan of "c" will be 
      #        set to the time taken to encode another two unrecognised 
      #        objects ("F" and "G") plus five/six empty squares (five if the 
      #        scene creator is encoded).
      #      
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # Blind objects on (1, 2) and (1, 3) should be overwritten at the same
      # when the first chunk is processed.  The blind object on (2, 1) should be 
      # overwritten when the second chunk is processed.  If ghost objects are to
      # be encoded, the blind object on (2, 3) should be overwritten when the
      # second chunk is processed.
      
      # Create list patterns to learn.
      list_pattern_1 = ListPattern.new
      list_pattern_1.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern_1.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern_2 = ListPattern.new
      list_pattern_2.add(ItemSquarePattern.new("D", 2, 1))
      list_pattern_2.add(ItemSquarePattern.new("C", 2, 3))
      list_patterns_to_learn.push(list_pattern_1)
      list_patterns_to_learn.push(list_pattern_2)
      
      # Both objects in the first list pattern and the first object in the 
      # second list pattern learned should be real so add these to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(1, 3, "1", "B")
      reality.addItemToSquare(2, 1, "2", "D")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(3, 2, "3", "F")
      reality.addItemToSquare(3, 1, "4", "G")

      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      expected_visual_spatial_field_object_properties[1][3].push([
        "1",
        "B",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false,
      ])
    
      expected_visual_spatial_field_object_properties[2][1].push([
        "2",
        "D",
        time_to_encode_objects * 2,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[2][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "C",
          (time_to_encode_objects * 2),
          (time_to_encode_objects * 2) + (time_to_encode_empty_squares * (encode_scene_creator ? 5 : 6)),
          true,
          true
        ])
      end
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(2, 1))
      squares_to_be_ignored.push(Square.new(1, 3))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[2][1][0][3] = (time_to_encode_objects * 2)
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[2][3][0][3] = (time_to_encode_objects * 2)
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 2
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 7 : 8
      squares_to_fixate_on = [
        [3, 2],
        [3, 1]
      ]
      
    ############################################################################
    elsif scenario == 11
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |  F  |  x     x
      #          ------------------- 
      # 3     x  |     |     |     |  x
      #    -------------------------------
      # 2  |     |  A  |     |  B  |     |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |  G  |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][b, 1, 3][B, 3, 2]>
      # 
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, two 
      # distinct, recognised VisualSpatialFieldObject instances for objects "A" and "B"
      # should be encoded.  If the "encode_ghost_objects" parameter is set 
      # to true, one additional recognised VisualSpatialFieldObject instance for object 
      # "b" should be encoded. 
      # 
      # - ID
      #   ~ b: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/b/B: The first occurrence of "A", "b" and "B" is in the first 
      #            chunk processed so all three objects are encoded at the 
      #            same time (when the first chunk is processed).
      #            
      # - Terminus times:
      #   ~ A/B: The last occurrences of "A" and "B" are in the first chunk 
      #          processed and no other objects (recognised or unrecognised) 
      #          overwrite them.  Therefore, their lifespan will be set to 
      #          the lifespan specified for recognised objects.
      #   ~ b: The last occurrence of "b" is in the first chunk processed 
      #        but "b" exists on a square that is empty in reality, so it 
      #        will be overwritten. Therefore, the lifespan of "b" will be 
      #        set to the time taken to encode one unrecognised object ("G") 
      #        plus six/seven empty squares (six if the scene creator is 
      #        encoded).
      #           
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # Blind objects on (1, 2) and (3, 2) should be overwritten when the 
      # first chunk is processed.  If ghost objects are to be encoded, the
      # blind object on (1, 3) should also be encoded when the first chunk is
      # processed.
      
      # Create list patterns to learn.
      list_pattern = ListPattern.new
      list_pattern.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern.add(ItemSquarePattern.new("B", 3, 2))
      list_patterns_to_learn.push(list_pattern)
      
      # First and last object in list pattern learned should be real so add 
      # these to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(3, 2, "1", "B")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(2, 4, "3", "F")
      reality.addItemToSquare(2, 0, "4", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "B",
          time_to_encode_objects,
          time_to_encode_objects + (time_to_encode_empty_squares * (encode_scene_creator ? 6 : 7)),
          true,
          true,
        ])
      end
      
      expected_visual_spatial_field_object_properties[3][2].push([
        "1",
        "B",
        time_to_encode_objects ,
        recognised_object_lifespan,
        true,
        false
      ])

      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(3, 2))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[3][2][0][3] = time_to_encode_objects
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 1
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 8 : 9
      
      squares_to_fixate_on = [
        [2, 4],
        [2, 0]
      ]
      
    ############################################################################
    elsif scenario == 12
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |     |  x     x
      #          ------------------- 
      # 3     x  |     |  F  |     |  x
      #    -------------------------------
      # 2  |     |  A  |     |  C  |     |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |  G  |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][b, 1, 3][C, 3, 2]>
      #       
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, two 
      # distinct, recognised VisualSpatialFieldObject instances for objects "A" and "C"
      # should be encoded.  If the "encode_ghost_objects" parameter is set 
      # to true, one additional recognised VisualSpatialFieldObject instance for object 
      # "b" should be encoded. 
      # 
      # - ID
      #   ~ b: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/b/C: The first occurrence of "A", "b" and "C" is in the first 
      #            chunk processed so all three objects are encoded at the 
      #            same time (when the first chunk is processed).
      #            
      # - Terminus times:
      #   ~ A/C: The last occurrences of "A" and "C" are in the first chunk 
      #          processed and no other objects (recognised or unrecognised) 
      #          overwrite them.  Therefore, their lifespan will be set to 
      #          the lifespan specified for recognised objects.
      #   ~ b: The last occurrence of "b" is in the first chunk processed 
      #        but "b" exists on a square that is empty in reality, so it 
      #        will be overwritten. Therefore, the lifespan of "b" will be 
      #        set to the time taken to encode one unrecognised object ("G")
      #        plus six/seven empty squares (six if the scene creator is 
      #        encoded).
      #           
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # Blind objects on (1, 2) and (3, 2) should be overwritten when the 
      # first chunk is processed.  If ghost objects are to be encoded, the
      # blind object on (1, 3) should also be encoded when the first chunk is
      # processed.
      
      # Create list patterns to learn.
      list_pattern = ListPattern.new
      list_pattern.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern.add(ItemSquarePattern.new("C", 3, 2))
      list_patterns_to_learn.push(list_pattern)
      
      # First and last object in list pattern learned should be real so add 
      # these to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(3, 2, "1", "C")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(2, 3, "2", "F")
      reality.addItemToSquare(2, 0, "3", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "B",
          time_to_encode_objects,
          time_to_encode_objects + (time_to_encode_empty_squares * (encode_scene_creator ? 6 : 7)),
          true,
          true,
        ])
      end
    
      expected_visual_spatial_field_object_properties[3][2].push([
        "1",
        "C",
        time_to_encode_objects ,
        recognised_object_lifespan,
        true,
        false
      ])
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(3, 2))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[3][2][0][3] = time_to_encode_objects
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 1
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 8 : 9
      
      squares_to_fixate_on = [
        [2, 3],
        [2, 0]
      ]
      
    ############################################################################
    elsif scenario == 13
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |  C  |  x     x
      #          ------------------- 
      # 3     x  |     |     |     |  x
      #    -------------------------------
      # 2  |  F  |  A  |     |  B  |     |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |  G  |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][b, 1, 3]><[B, 3, 2][C, 2, 4]>
      #       
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, three 
      # distinct, recognised VisualSpatialFieldObject instances for objects "A", "B" and 
      # "C" should be encoded.  If the "encode_ghost_objects" parameter is set 
      # to true, one additional recognised VisualSpatialFieldObject instance for object 
      # "b" should be encoded. 
      # 
      # - ID
      #   ~ b: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/b: The first occurrence of "A" and "b" is in the first chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the first chunk is processed).
      #   ~ B/C: The first occurrence of "B" and "C" is in the second chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the second chunk is processed).
      #          
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrite it.  
      #        Therefore, its lifespan will be set to the lifespan specified 
      #        for recognised objects.
      #   ~ b: The last occurrence of "b" is in the first chunk processed but
      #        "b" exists on a square that is empty in reality, so it will be 
      #        overwritten. Therefore, the lifespan of "b" will be set to the 
      #        time taken to encode the second chunk, two unrecognised 
      #        objects ("F" and "G") plus five/six empty squares (five if the 
      #        scene creator is encoded).
      #   ~ B/C: The last occurrence of "B" and "C" is in the second chunk 
      #          processed and no other objects (recognised or unrecognised) 
      #          overwrite them.  Therefore, their lifespan will be set to 
      #          the lifespan specified for recognised objects.
      #      
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # The blind object on (1, 2) should be overwritten when the first chunk is 
      # processed.  The blind objects on (3, 2) and (2, 4) should be overwritten
      # when the second chunk is processed.  If ghost objects are to be encoded, 
      # the blind object on (1, 3) should also be encoded when the first chunk 
      # is processed.
      
      # Create list patterns to learn.
      list_pattern_1 = ListPattern.new
      list_pattern_1.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern_1.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern_2 = ListPattern.new
      list_pattern_2.add(ItemSquarePattern.new("B", 3, 2))
      list_pattern_2.add(ItemSquarePattern.new("C", 2, 4))
      list_patterns_to_learn.push(list_pattern_1)
      list_patterns_to_learn.push(list_pattern_2)
      
      # All objects except the second object in the first list pattern learned 
      # should be real so add them to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(3, 2, "1", "B")
      reality.addItemToSquare(2, 4, "2", "C")

      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(0, 2, "3", "F")
      reality.addItemToSquare(2, 0, "4", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "B",
          time_to_encode_objects,
          (time_to_encode_objects * 3) + (time_to_encode_empty_squares * (encode_scene_creator ? 5 : 6)),
          true,
          true,
        ])
      end
    
      expected_visual_spatial_field_object_properties[3][2].push([
        "1",
        "B",
        (time_to_encode_objects * 2) ,
        recognised_object_lifespan,
        true,
        false
      ])

      expected_visual_spatial_field_object_properties[2][4].push([
        "2",
        "C",
        (time_to_encode_objects * 2),
        recognised_object_lifespan,
        true,
        false
      ])

      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(3, 2))
      squares_to_be_ignored.push(Square.new(2, 4))
       
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[3][2][0][3] = (time_to_encode_objects * 2)
      expected_visual_spatial_field_object_properties[2][4][0][3] = (time_to_encode_objects * 2)
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 2
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 7 : 8
      
      squares_to_fixate_on = [
        [0, 2],
        [2, 0]
      ]
      
    ############################################################################  
    elsif scenario == 14
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |     |  x     x
      #          ------------------- 
      # 3     x  |  D  |     |     |  x
      #    -------------------------------
      # 2  |     |  A  |     |  B  |  G  |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |  F  |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][b, 1, 3]><[B, 3, 2][D, 1, 3]>
      # 
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, three 
      # distinct, recognised VisualSpatialFieldObject instances for objects "A", "B" and 
      # "D" should be encoded.  If the "encode_ghost_objects" parameter is set 
      # to true, one additional recognised VisualSpatialFieldObject instance for object 
      # "b" should be encoded. 
      #
      # - ID
      #   ~ b: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/b: The first occurrence of "A" and "b" is in the first chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the first chunk is processed).
      #   ~ B/D: The first occurrence of "B" and "D" is in the second chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the second chunk is processed).
      #          
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrite it.  
      #        Therefore, its lifespan will be set to the lifespan specified 
      #        for recognised objects.
      #   ~ b: The last occurrence of "b" is in the first chunk processed but
      #        "b" will be overwritten by "D" since "D" is recognised more
      #        recently and is not a ghost object.  Therefore, "b"s lifespan
      #        will be set to the time taken to encode the second chunk.
      #   ~ B/D: The last occurrence of "B" and "D" is in the second chunk 
      #          processed and no other objects (recognised or unrecognised) 
      #          overwrite them.  Therefore, their lifespan will be set to 
      #          the lifespan specified for recognised objects.
      #
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # The blind object on (1, 2) should be overwritten when the first chunk is 
      # processed.  The blind objects on (3, 2) should be overwritten when the 
      # second chunk is processed.  If ghost objects are to be encoded, the 
      # blind object on (1, 3) should be overwritten when the first chunk is 
      # processed.  Otherwise, it should be overwritten when the second chunk is
      # processed.
      
      # Create list patterns to learn.
      list_pattern_1 = ListPattern.new
      list_pattern_1.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern_1.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern_2 = ListPattern.new
      list_pattern_2.add(ItemSquarePattern.new("B", 3, 2))
      list_pattern_2.add(ItemSquarePattern.new("D", 1, 3))
      list_patterns_to_learn.push(list_pattern_1)
      list_patterns_to_learn.push(list_pattern_2)
      
      # All objects except the second object in the first list pattern learned 
      # should be real so add them to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(3, 2, "1", "B")
      reality.addItemToSquare(1, 3, "2", "D")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(2, 0, "3", "F")
      reality.addItemToSquare(4, 2, "4", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "B",
          time_to_encode_objects,
          time_to_encode_objects,
          true,
          true
        ])
      end
    
      expected_visual_spatial_field_object_properties[3][2].push([
        "1",
        "B",
        time_to_encode_objects * 2,
        recognised_object_lifespan,
        true,
        false
      ])
    
      expected_visual_spatial_field_object_properties[1][3].push([
        "2",
        "D",
        time_to_encode_objects * 2,
        recognised_object_lifespan,
        true,
        false
      ])
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(3, 2))
      squares_to_be_ignored.push(Square.new(1, 3))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[1][3][0][3] = (time_to_encode_objects * 2)
      expected_visual_spatial_field_object_properties[3][2][0][3] = (time_to_encode_objects * 2)
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 2
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 7 : 8
      squares_to_fixate_on = [
        [2, 0],
        [4, 2]
      ]
        
    ############################################################################  
    elsif scenario == 15
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |  D  |  x     x
      #          ------------------- 
      # 3     x  |     |     |     |  x
      #    -------------------------------
      # 2  |  F  |  A  |     |  C  |  G  |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |     |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][b, 1, 3]><[C, 3, 2][D, 2, 4]>
      #       
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, three 
      # distinct, recognised VisualSpatialFieldObject instances for objects "A", "C" and 
      # "D" should be encoded.  If the "encode_ghost_objects" parameter is set 
      # to true, one additional recognised VisualSpatialFieldObject instance for object 
      # "b" should be encoded. 
      #
      # - ID
      #   ~ b: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/b: The first occurrence of "A" and "b" is in the first chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the first chunk is processed).
      #   ~ C/D: The first occurrence of "C" and "D" is in the second chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the second chunk is processed).
      #          
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrite it.  
      #        Therefore, its lifespan will be set to the lifespan specified 
      #        for recognised objects.
      #   ~ b: The last occurrence of "b" is in the first chunk processed but
      #        "b" exists on a square that is empty in reality, so it will be 
      #        overwritten. Therefore, the lifespan of "b" will be set to the 
      #        time taken to encode the second chunk, two unrecognised 
      #        objects ("F" and "G") plus five/six empty squares (five if the
      #        scene creator is encoded).
      #   ~ C/D: The last occurrence of "C" and "D" is in the second chunk 
      #          processed and no other objects (recognised or unrecognised) 
      #          overwrite them.  Therefore, their lifespan will be set to 
      #          the lifespan specified for recognised objects.
      #      
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # The blind object on (1, 2) should be overwritten when the first chunk is 
      # processed.  Blind objects on (3, 2) and (2, 4) should be overwritten 
      # when the second chunk is processed.  If ghost objects should be encoded,
      # the blind object on (1, 3) should be overwritten when the first chunk is
      # processed.
      
      # Create list patterns to learn.
      list_pattern_1 = ListPattern.new
      list_pattern_1.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern_1.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern_2 = ListPattern.new
      list_pattern_2.add(ItemSquarePattern.new("C", 3, 2))
      list_pattern_2.add(ItemSquarePattern.new("D", 2, 4))
      list_patterns_to_learn.push(list_pattern_1)
      list_patterns_to_learn.push(list_pattern_2)
      
      # All objects except the second object in the first list pattern should be 
      # real so add them to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(3, 2, "1", "C")
      reality.addItemToSquare(2, 4, "2", "D")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(0, 2, "3", "F")
      reality.addItemToSquare(4, 2, "4", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "B",
          time_to_encode_objects,
          (time_to_encode_objects * 3) + (time_to_encode_empty_squares * (encode_scene_creator ? 5 : 6)),
          true,
          true
        ])
      end
    
      expected_visual_spatial_field_object_properties[3][2].push([
        "1",
        "C",
        time_to_encode_objects * 2,
        recognised_object_lifespan,
        true,
        false
      ])
    
      expected_visual_spatial_field_object_properties[2][4].push([
        "2",
        "D",
        time_to_encode_objects * 2,
        recognised_object_lifespan,
        true,
        false
      ])
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(3, 2))
      squares_to_be_ignored.push(Square.new(2, 4))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[3][2][0][3] = (time_to_encode_objects * 2)
      expected_visual_spatial_field_object_properties[2][4][0][3] = (time_to_encode_objects * 2)
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 2
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 7 : 8
      squares_to_fixate_on = [
        [0, 2],
        [4, 2]
      ]
      
    ############################################################################
    elsif scenario == 16
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |     |  x     x
      #          ------------------- 
      # 3     x  |     |     |     |  x
      #    -------------------------------
      # 2  |     |  A  |     |     |     |
      #    -------------------------------
      # 1     x  |     |     |  G  |  x
      #          -------------------
      # 0     x     x  |  F  |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][b, 1, 3][b, 3, 2]>
      # 
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, one 
      # distinct, recognised VisualSpatialFieldObject instance for object "A" should be 
      # encoded.  If the "encode_ghost_objects" parameter is set to true, two 
      # additional recognised VisualSpatialFieldObject instances for objects "b" should be 
      # encoded (to differentiate between the "b" objects, the second "b" object 
      # will be referred to as "b*"). 
      #
      # - ID
      #   ~ b: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #   ~ b*: should equal the result of calling the 
      #         "VisualSpatialField.getGhostObjectIdPrefix()" method with "1" appended 
      #         since it is the second ghost object encoded in the chunks 
      #         recognised.
      #         
      # - Creation times
      #   ~ A/b/b*: The first occurrence of "A", "b" and "b*" is in the first 
      #             chunk processed so all three objects are encoded at the 
      #             same time (when the first chunk is processed).
      #             
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrite it.  
      #        Therefore, its lifespan will be set to the lifespan specified 
      #        for recognised objects.
      #   ~ b: The last occurrence of "b" is in the first chunk processed but 
      #        it exists on a square that is empty in reality, so it will be 
      #        overwritten. Therefore, the lifespan of "b" will be set to the 
      #        time taken to encode two unrecognised objects ("F" and "G") plus 
      #        six/seven empty squares (six if the scene creator is encoded).
      #   ~ b*: The last occurrence of "b*" is in the first chunk processed but 
      #         it exists on a square that is empty in reality, so it will be 
      #         overwritten. Therefore, the lifespan of "b*" will be set to the 
      #         time taken to encode two unrecognised objects ("F" and "G") plus 
      #         four/five empty squares (four if the scene creator is encoded).
      #              
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # The blind object on (1, 2) should be overwritten when the first chunk is 
      # processed.  If blind objects are to be encoded, blind objects on (1, 3) 
      # and (3, 2) are also overwritten when the first chunk is processed.
      
      # Create list patterns to learn.
      list_pattern = ListPattern.new
      list_pattern.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern.add(ItemSquarePattern.new("B", 3, 2))
      list_patterns_to_learn.push(list_pattern)
      
      # Only the first object learned should be real so add it to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(2, 0, "1", "F")
      reality.addItemToSquare(3, 2, "2", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "B",
          time_to_encode_objects,
          (time_to_encode_objects * 2) + (time_to_encode_empty_squares * (encode_scene_creator ? 6 : 7)),
          true,
          true
        ])

        expected_visual_spatial_field_object_properties[3][2].push([
          VisualSpatialField.getGhostObjectIdPrefix + "1",
          "B",
          time_to_encode_objects,
          (time_to_encode_objects * 2) + (time_to_encode_empty_squares * (encode_scene_creator ? 4 : 5)),
          true,
          true
        ])
      end
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
        expected_visual_spatial_field_object_properties[3][2][0][3] = time_to_encode_objects
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 1
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 9 : 10
      
      squares_to_fixate_on = [
        [2, 0],
        [3, 2]
      ]
      
    ############################################################################
    elsif scenario == 17
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |     |  x     x
      #          ------------------- 
      # 3     x  |     |     |  G  |  x
      #    -------------------------------
      # 2  |     |  A  |     |     |  F  |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |     |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][b, 1, 3][c, 3, 2]>
      #       
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, one 
      # distinct, recognised VisualSpatialFieldObject instance for object "A" should be 
      # encoded.  If the "encode_ghost_objects" parameter is set to true, two 
      # additional recognised VisualSpatialFieldObject instances for objects "b" and "c" 
      # should be encoded. 
      #
      # - ID
      #   ~ b: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #   ~ c: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "1" appended 
      #        since it is the second ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/b/c: The first occurrence of "A", "b" and "c" is in the first 
      #            chunk processed so all three objects are encoded at the 
      #            same time (when the first chunk is processed).
      #            
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrite it.  
      #        Therefore, its lifespan will be set to its creation time plus 
      #        the lifespan specified for recognised objects.
      #   ~ b: The last occurrence of "b" is in the first chunk processed but 
      #        it exists on a square that is empty in reality, so it will be 
      #        overwritten. Therefore, the lifespan of "b" will be set to the 
      #        time taken to encode one unrecognised object ("F") plus 
      #        seven/eight empty squares (seven if the scene creator is 
      #        encoded).
      #   ~ c: The last occurrence of "c" is in the first chunk processed but 
      #        it exists on a square that is empty in reality, so it will be 
      #        overwritten. Therefore, the lifespan of "c" will be set to the 
      #        time taken to encode six/seven empty squares (six if the 
      #        scene creator is encoded).
      #              
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # The blind object on (1, 2) should be overwritten when the first chunk is 
      # processed.  If ghost objects should be encoded, the blind objects on 
      # (1, 3) and (3, 2) should be overwritten at the same time.
     
      # Create list patterns to learn.
      list_pattern = ListPattern.new
      list_pattern.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern.add(ItemSquarePattern.new("C", 3, 2))
      list_patterns_to_learn.push(list_pattern)
      
      # Only the first object learned should be real so add it to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(4, 2, "1", "F")
      reality.addItemToSquare(3, 3, "2", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "B",
          time_to_encode_objects,
          time_to_encode_objects + (time_to_encode_empty_squares * (encode_scene_creator ? 7 : 8)),
          true,
          true
        ])
    
        expected_visual_spatial_field_object_properties[3][2].push([
          VisualSpatialField.getGhostObjectIdPrefix + "1",
          "C",
          time_to_encode_objects,
          (time_to_encode_empty_squares * (encode_scene_creator ? 6 : 7)),
          true,
          true
        ])
      end
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
        expected_visual_spatial_field_object_properties[3][2][0][3] = time_to_encode_objects
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 1
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 9 : 10
      
      squares_to_fixate_on = [
        [4, 2],
        [3, 3]
      ]
      
    ############################################################################
    elsif scenario == 18
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |  F  |  x     x
      #          ------------------- 
      # 3     x  |     |     |  G  |  x
      #    -------------------------------
      # 2  |     |  A  |     |     |     |
      #    -------------------------------
      # 1     x  |     |     |  D  |  x
      #          -------------------
      # 0     x     x  |     |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][b, 1, 3]><[D, 3, 1][b, 1, 3]>
      # 
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, two 
      # distinct, recognised VisualSpatialFieldObject instances for objects "A" and "D" 
      # should be encoded.  If the "encode_ghost_objects" parameter is set to 
      # true, one additional recognised VisualSpatialFieldObject instance for object 
      # "b" should be encoded (the two "b" objects refer to the same object). 
      #
      # - ID
      #   ~ b: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/b: The first occurrence of "A" and "b" is in the first chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the first chunk is processed). 
      #   ~ D: The first occurrence of "D" is in the second chunk processed 
      #        so it is encoded when the second chunk is processed.
      #        
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrites it.  
      #        Therefore, its lifespan will be set to its creation time plus 
      #        the lifespan specified for recognised objects.
      #   ~ D: The last occurrence of "D" is in the second chunk processed 
      #        and no other objects (recognised or unrecognised) overwrite 
      #        it.  Therefore, its lifespan will be set to the lifespan 
      #        specified for recognised objects.
      #   ~ b: The last occurrence of "b" is in the second chunk processed 
      #        and its terminus will be extended if its current terminus has 
      #        not been reached when the second chunk is encoded (timing 
      #        parameters provided to visual-spatial field construction 
      #        method may prevent this if changed from their original
      #        values).  Object "b" is, however, overwritten by an empty 
      #        square so its lifespan is set to the time taken to encode 
      #        the second chunk plus the time taken to encode seven/eight empty 
      #        squares (seven if the scene creator is encoded).
      # 
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # Blind objects on (1, 2) and (3, 1) should be overwritten when the first 
      # and second chunks are processed, respectively.  If ghost objects are to
      # be encoded, the blind object on (1, 3) should be overwritten when the 
      # first chunk is processed.
      
      # Create list patterns to learn.
      list_pattern_1 = ListPattern.new
      list_pattern_1.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern_1.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern_2 = ListPattern.new
      list_pattern_2.add(ItemSquarePattern.new("D", 3, 1))
      list_pattern_2.add(ItemSquarePattern.new("B", 1, 3))
      list_patterns_to_learn.push(list_pattern_1)
      list_patterns_to_learn.push(list_pattern_2)
      
      # Only the first object in each list pattern learned should be real so add 
      # them to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(3, 1, "1", "D")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(2, 4, "2", "F")
      reality.addItemToSquare(3, 3, "3", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "B",
          time_to_encode_objects,
          time_to_encode_objects + (time_to_encode_empty_squares * (encode_scene_creator ? 7 : 8)),
          true,
          true
        ])
      end
    
      expected_visual_spatial_field_object_properties[3][1].push([
        "1",
        "D",
        time_to_encode_objects * 2,
        recognised_object_lifespan,
        true,
        false
      ])
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(3, 1))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[3][1][0][3] = (time_to_encode_objects * 2)
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 2
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 8 : 9
      
      squares_to_fixate_on = [
        [2, 4],
        [3, 3]
      ]
      
    ############################################################################
    elsif scenario == 19
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |     |  x     x
      #          ------------------- 
      # 3     x  |     |  D  |     |  x
      #    -------------------------------
      # 2  |     |  A  |     |     |  F  |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |  G  |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][b, 1, 3]><[D, 2, 3][b, 2, 4]>
      # 
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, two 
      # distinct, recognised VisualSpatialFieldObject instances for objects "A" and "D" 
      # should be encoded.  If the "encode_ghost_objects" parameter is set to 
      # true, two additional recognised VisualSpatialFieldObject instances for objects 
      # "b" should be encoded (to differentiate between the "b" objects, the 
      # second "b" object will be referred to as "b*").
      # 
      # - ID
      #   ~ b: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #   ~ b*: should equal the result of calling the 
      #         "VisualSpatialField.getGhostObjectIdPrefix()" method with "1" appended 
      #         since it is the first ghost object encoded in the chunks 
      #         recognised.
      #         
      # - Creation times
      #   ~ A/b: The first occurrence of "A" and "b" is in the first chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the first chunk is processed). 
      #   ~ D/b*: The first occurrence of "D" and "b*" is in the second 
      #           chunk processed so both objects are encoded at the same 
      #           time (when the second chunk is processed). 
      #           
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrites it.  
      #        Therefore, its lifespan will be set to the lifespan specified 
      #        for recognised objects.
      #   ~ b: The last occurrence of "b" is in the first chunk processed but
      #        "b" exists on a square that is empty in reality, so it will be 
      #        overwritten. Therefore, the lifespan of "b" will be set to the 
      #        time taken to encode the second chunk, two unrecognised 
      #        objects ("F" and "G") plus six/seven empty squares (six if the 
      #        scene creator is to be encoded).
      #   ~ D: The last occurrence of "D" is in the second chunk processed 
      #        and no other objects (recognised or unrecognised) overwrites 
      #        it.  Therefore, its lifespan will be set to the lifespan 
      #        specified for recognised objects.
      #   ~ b*: The last occurrence of "b*" is in the second chunk processed 
      #         but "b*" exists on a square that is empty in reality, so it 
      #         will be overwritten. Therefore, the lifespan of "b*" will be 
      #         set to the time taken to encode two unrecognised objects 
      #         ("F" and "G") plus eight/nine empty squares (eight if the scene
      #         creator is to be encoded).
      #      
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # Blind objects on (1, 2) and (2, 3) should be overwritten when the first
      # and second chunks are processed, respectively.  If ghost objects should
      # be encoded then the blind objects on (1, 3) and (2, 4) should be 
      # overwritten when the first and second chunks are processed, 
      # respectively.
      
      # Create list patterns to learn.
      list_pattern_1 = ListPattern.new
      list_pattern_1.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern_1.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern_2 = ListPattern.new
      list_pattern_2.add(ItemSquarePattern.new("D", 2, 3))
      list_pattern_2.add(ItemSquarePattern.new("B", 2, 4))
      list_patterns_to_learn.push(list_pattern_1)
      list_patterns_to_learn.push(list_pattern_2)
      
      # Only the first object in each list pattern learned should be real so add 
      # them to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(2, 3, "1", "D")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(4, 2, "2", "F")
      reality.addItemToSquare(2, 0, "3", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "B",
          time_to_encode_objects,
          (time_to_encode_objects * 3) + (time_to_encode_empty_squares * (encode_scene_creator ? 6 : 7)),
          true,
          true
        ])
      end
    
      expected_visual_spatial_field_object_properties[2][3].push([
        "1",
        "D",
        time_to_encode_objects * 2,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[2][4].push([
          VisualSpatialField.getGhostObjectIdPrefix + "1",
          "B",
          (time_to_encode_objects * 2),
          (time_to_encode_objects * 2) + (time_to_encode_empty_squares * (encode_scene_creator ? 8 : 9)),
          true,
          true
        ])
      end
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(2, 3))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[2][3][0][3] = (time_to_encode_objects * 2)
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
        expected_visual_spatial_field_object_properties[2][4][0][3] = (time_to_encode_objects * 2)
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 2
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 8 : 9
      
      squares_to_fixate_on = [
        [4, 2],
        [2, 0]
      ]
      
    ############################################################################
    elsif scenario == 20
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |     |  x     x
      #          ------------------- 
      # 3     x  |     |  D  |     |  x
      #    -------------------------------
      # 2  |     |  A  |     |     |  F  |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |  G  |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][b, 1, 3]><[D, 2, 3][c, 1, 3]>
      # 
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, two 
      # distinct, recognised VisualSpatialFieldObject instances for objects "A" and "D" 
      # should be encoded.  If the "encode_ghost_objects" parameter is set to 
      # true, two additional recognised VisualSpatialFieldObject instances for objects 
      # "b" and "c" should be encoded.
      # 
      # - ID
      #   ~ b: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #   ~ c: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "1" appended 
      #        since it is the secomd ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/b: The first occurrence of "A" and "b" is in the first chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the first chunk is processed).
      #   ~ D/c: The first occurrence of "D" and "c" is in the second chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the second chunk is processed).
      #          
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrite it.  
      #        Therefore, its lifespan will be set to the lifespan specified 
      #        for recognised objects.
      #   ~ b: The last occurrence of "b" is in the first chunk processed but
      #        "b" will be overwritten by "c" since "c" is recognised more
      #        recently.  Therefore, "b"s lifespan will be set to the time 
      #        taken to encode the second chunk.
      #   ~ D: The last occurrence of "D" is in the second chunk processed 
      #        and no other objects (recognised or unrecognised) overwrite 
      #        it.  Therefore, its lifespan will be set to the lifespan 
      #        specified for recognised objects.
      #   ~ c: The last occurrence of "c" is in the second chunk processed 
      #        but "c" exists on a square that is empty in reality, so it 
      #        will be overwritten. Therefore, the lifespan of "c" will be 
      #        set to the time taken to encode two unrecognised objects 
      #        ("F" and "G") plus six/seven empty squares (seven if the scene
      #        creator is encoded).
      #
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # Blind objects on (1, 2) and (2, 3) should be overwritten when the first 
      # and second chunk are processed, respectively.  If ghost objects are to 
      # be encoded, the blind object on (1, 3) will be overwritten when the 
      # first chunk is processed.
      
      # Create list patterns to learn.
      list_pattern_1 = ListPattern.new
      list_pattern_1.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern_1.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern_2 = ListPattern.new
      list_pattern_2.add(ItemSquarePattern.new("D", 2, 3))
      list_pattern_2.add(ItemSquarePattern.new("C", 1, 3))
      list_patterns_to_learn.push(list_pattern_1)
      list_patterns_to_learn.push(list_pattern_2)
      
      # Only the first object in each list pattern learned should be real so add 
      # them to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(2, 3, "1", "D")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(4, 2, "2", "F")
      reality.addItemToSquare(2, 0, "3", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "B",
          time_to_encode_objects,
          time_to_encode_objects,
          true,
          true
        ])
      end
    
      expected_visual_spatial_field_object_properties[2][3].push([
        "1",
        "D",
        time_to_encode_objects * 2,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "1",
          "C",
          time_to_encode_objects * 2,
          (time_to_encode_objects * 2) + (time_to_encode_empty_squares * (encode_scene_creator ? 6 : 7)),
          true,
          true
        ])
      end
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(2, 3))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[2][3][0][3] = (time_to_encode_objects * 2)
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 2
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 8 : 9
      
      squares_to_fixate_on = [
        [4, 2],
        [2, 0]
      ]
      
    ############################################################################
    elsif scenario == 21
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |     |  x     x
      #          ------------------- 
      # 3     x  |     |  D  |     |  x
      #    -------------------------------
      # 2  |  F  |  A  |     |  G  |     |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |     |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A, 1, 2][b, 1, 3]><[D, 2, 3][c, 2, 4]>
      # 
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, two 
      # distinct, recognised VisualSpatialFieldObject instances for objects "A" and "D" 
      # should be encoded.  If the "encode_ghost_objects" parameter is set to 
      # true, two additional recognised VisualSpatialFieldObject instances for objects 
      # "b" and "c" should be encoded.
      # 
      # - ID
      #   ~ b: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #   ~ c: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "1" appended 
      #        since it is the second ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/b: The first occurrence of "A" and "b" is in the first chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the first chunk is processed).
      #   ~ D/c: The first occurrence of "D" and "c" is in the second chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the second chunk is processed).
      #          
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrite it.  
      #        Therefore, its lifespan will be set to the lifespan specified 
      #        for recognised objects.
      #   ~ b: The last occurrence of "b" is in the first chunk processed but
      #        "b" will be overwritten by an empty square.  Therefore, "b"s 
      #        lifespan will be set to the time taken to encode the second 
      #        chunk, two unrecognised objects ("F" and "G") and the time 
      #        taken to encode six/seven empty squares (six if the scene creator
      #        is encoded).
      #   ~ D: The last occurrence of "D" is in the second chunk processed 
      #        and no other objects (recognised or unrecognised) overwrite 
      #        it.  Therefore, its lifespan will be set to the lifespan 
      #        specified for recognised objects.
      #   ~ c: The last occurrence of "c" is in the second chunk processed 
      #        but "c" will be overwritten by an empty square.  Therefore, 
      #        "c"s lifespan will be set to the time taken to encode two 
      #        unrecognised objects ("F" and "G") and the time taken to 
      #        encode eight/nine empty squares (nine if the scene creator
      #        is encoded).
      #
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # Blind objects on (1, 2) and (2, 3) should be overwritten when the first 
      # and second chunk are processed, respectively.  If ghost objects are 
      # encoded, the blind objects on (1, 3) and (2, 4) will be overwritten when 
      # the first and second chunk are processed, respectively.
      
      # Create list patterns to learn.
      list_pattern_1 = ListPattern.new
      list_pattern_1.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern_1.add(ItemSquarePattern.new("B", 1, 3))
      list_pattern_2 = ListPattern.new
      list_pattern_2.add(ItemSquarePattern.new("D", 2, 3))
      list_pattern_2.add(ItemSquarePattern.new("C", 2, 4))
      list_patterns_to_learn.push(list_pattern_1)
      list_patterns_to_learn.push(list_pattern_2)
      
      # Only the first object in each list pattern learned should be real so add 
      # them to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      reality.addItemToSquare(2, 3, "1", "D")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(0, 2, "2", "F")
      reality.addItemToSquare(3, 2, "3", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "B",
          time_to_encode_objects,
          (time_to_encode_objects * 3) + (time_to_encode_empty_squares * (encode_scene_creator ? 6 : 7)),
          true,
          true
        ])
      end
    
      expected_visual_spatial_field_object_properties[2][3].push([
        "1",
        "D",
        time_to_encode_objects * 2,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[2][4].push([
          VisualSpatialField.getGhostObjectIdPrefix + "1",
          "C",
          (time_to_encode_objects * 2),
          (time_to_encode_objects * 2) + (time_to_encode_empty_squares * (encode_scene_creator ? 8 : 9)),
          true,
          true
        ])
      end
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      squares_to_be_ignored.push(Square.new(2, 3))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      expected_visual_spatial_field_object_properties[2][3][0][3] = (time_to_encode_objects * 2)
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[1][3][0][3] = time_to_encode_objects
        expected_visual_spatial_field_object_properties[2][4][0][3] = (time_to_encode_objects * 2)
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 2
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 8 : 9
      
      squares_to_fixate_on = [
        [0, 2],
        [3, 2]
      ]
      
    ############################################################################
    elsif scenario == 22
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |     |  x     x
      #          ------------------- 
      # 3     x  |     |     |  G  |  x
      #    -------------------------------
      # 2  |     |  A  |     |     |  F  |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     a     x  |     |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A 1 2][a 0 0]>
      #       
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, one 
      # distinct, recognised VisualSpatialFieldObject instances for object "A" should be 
      # encoded.  If the "encode_ghost_objects" parameter is set to true, one 
      # additional recognised VisualSpatialFieldObject instance for object "a" should be 
      # encoded.
      # 
      # - ID
      #   ~ a: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/a: The first occurrence of "A" and "a" is in the first chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the first chunk is processed).
      #          
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrite it.  
      #        Therefore, its lifespan will be set to the lifespan specified 
      #        for recognised objects.
      #   ~ a: The last occurrence of "a" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrite it 
      #        since it is located on a blind square in reality and blind
      #        squares can not overwrite ghost objects. Therefore, its 
      #        lifespan will be set to the lifespan specified for recognised 
      #        objects.
      #        
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # The blind object on (1, 2) should be overwritten when the first chunk is 
      # processed.  If ghost objects should be encoded, the blind square on 
      # (0, 0) will also be encoded when the first chunk is processed.
      
      # Create list patterns to learn.
      list_pattern = ListPattern.new
      list_pattern.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern.add(ItemSquarePattern.new("A", 0, 0))
      list_patterns_to_learn.push(list_pattern)
      
      # Only the first object learned should be real so add it to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(4, 2, "2", "F")
      reality.addItemToSquare(3, 3, "3", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[0][0].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "A",
          time_to_encode_objects,
          recognised_object_lifespan,
          true,
          true
        ])
      end
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      
      if(encode_ghost_objects)
        squares_to_be_ignored.push(Square.new(0, 0))
      end
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[0][0][0][3] = time_to_encode_objects
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 1
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 9 : 10
      
      squares_to_fixate_on = [
        [4, 2],
        [3, 3]
      ]
      
    ############################################################################
    elsif scenario == 23
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |  G  |  x     x
      #          ------------------- 
      # 3     x  |     |     |     |  x
      #    -------------------------------
      # 2  |     |  A  |     |  F  |     |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |     |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A 1 2][a 2 0]>
      #       
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, one 
      # distinct, recognised VisualSpatialFieldObject instances for object "A" should be 
      # encoded.  If the "encode_ghost_objects" parameter is set to true, one 
      # additional recognised VisualSpatialFieldObject instance for object "a" should be 
      # encoded.
      # 
      # - ID
      #   ~ a: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/a: The first occurrence of "A" and "a" is in the first chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the first chunk is processed).
      #          
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrite it.  
      #        Therefore, its lifespan will be set to the lifespan specified 
      #        for recognised objects.
      #   ~ a: The last occurrence of "a" is in the first chunk processed 
      #        but "a" will be overwritten by an empty square. Therefore, its 
      #        lifespan will be set to the time taken to encode an empty 
      #        square.
      #           
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # The blind object on (1, 2) should be overwritten when the first chunk is 
      # processed.  If ghost objects should be encoded, the blind square on 
      # (2, 0) will also be encoded when the first chunk is processed.
      
      # Create list patterns to learn.
      list_pattern = ListPattern.new
      list_pattern.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern.add(ItemSquarePattern.new("A", 2, 0))
      list_patterns_to_learn.push(list_pattern)
      
      # Only the first object learned should be real so add it to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(3, 2, "2", "F")
      reality.addItemToSquare(2, 4, "3", "G")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[2][0].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "A",
          time_to_encode_objects,
          time_to_encode_empty_squares,
          true,
          true
        ])
      end
      
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[2][0][0][3] = time_to_encode_objects
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 1
      number_unrecognised_objects = 2
      number_empty_squares = encode_scene_creator ? 9 : 10
      
      squares_to_fixate_on = [
        [3, 2],
        [2, 4]
      ]
     
    ############################################################################
    elsif scenario == 24
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |  B  |  x     x
      #          ------------------- 
      # 3     x  |     |     |     |  x
      #    -------------------------------
      # 2  |     |  A  |     |  G  |  F  |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |     |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A 1 2][a 2 4]>
      #       
      # ==============================================================
      # Expected VisualSpatialFieldObjects and Properties for Recognised Objects
      # ==============================================================
      # 
      # No matter what the "encode_ghost_objects" parameter is set to, one 
      # distinct, recognised VisualSpatialFieldObject instances for object "A" should be 
      # encoded.  If the "encode_ghost_objects" parameter is set to true, one 
      # additional recognised VisualSpatialFieldObject instance for object "a" should be 
      # encoded.
      # 
      # - ID
      #   ~ a: should equal the result of calling the 
      #        "VisualSpatialField.getGhostObjectIdPrefix()" method with "0" appended 
      #        since it is the first ghost object encoded in the chunks 
      #        recognised.
      #        
      # - Creation times
      #   ~ A/a: The first occurrence of "A" and "a" is in the first chunk 
      #          processed so both objects are encoded at the same time 
      #          (when the first chunk is processed).
      #          
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrite it.  
      #        Therefore, its lifespan will be set to the lifespan specified 
      #        for recognised objects.
      #   ~ a: The last occurrence of "a" is in the first chunk processed 
      #        but "a" will be overwritten by an unrecognised object. Therefore, 
      #        its lifespan will be set to the time taken to encode three 
      #        unrecognised objects ("B", "F" and "G") and eight/nine empty 
      #        squares (eight if the scene creator is encoded).
      #           
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # The blind object on (1, 2) should be overwritten when the first chunk is 
      # processed.  If ghost objects should be encoded, the blind square on 
      # (2, 4) will also be encoded when the first chunk is processed.
      
      # Create list patterns to learn.
      list_pattern = ListPattern.new
      list_pattern.add(ItemSquarePattern.new("A", 1, 2))
      list_pattern.add(ItemSquarePattern.new("A", 2, 4))
      list_patterns_to_learn.push(list_pattern)
      
      # The first object learned should be real so add it to reality.
      reality.addItemToSquare(1, 2, "0", "A")
      
      # Add an object to reality that wasn't learned on the same coordinates as 
      # the ghost object.
      reality.addItemToSquare(2, 4, "1", "B")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(3, 2, "2", "G")
      reality.addItemToSquare(4, 2, "3", "F")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[1][2].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[2][4].push([
          VisualSpatialField.getGhostObjectIdPrefix + "0",
          "A",
          time_to_encode_objects,
          (time_to_encode_objects * 3) + (time_to_encode_empty_squares * (encode_scene_creator ? 8 : 9)),
          true,
          true
        ])
      end
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(1, 2))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[1][2][0][3] = time_to_encode_objects
      
      if(encode_ghost_objects)
        expected_visual_spatial_field_object_properties[2][4][0][3] = time_to_encode_objects
      end
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 1
      number_unrecognised_objects = 3
      number_empty_squares = encode_scene_creator ? 8 : 9
      
      squares_to_fixate_on = [
        [2, 4],
        [3, 2],
        [4, 2]
      ]
      
    ############################################################################
    elsif scenario == 25
      
      # =============================
      # Expected Visual-Spatial Field
      # =============================
      # 
      #                -------
      # 4     x     x  |  F  |  x     x
      #          ------------------- 
      # 3     x  |     |  A  |     |  x
      #    -------------------------------
      # 2  |     |     | SELF|     |  G  |
      #    -------------------------------
      # 1     x  |     |     |     |  x
      #          -------------------
      # 0     x     x  |     |  x     x
      #                -------
      #       0     1     2     3     4
      #       
      # ======================
      # List Patterns to Learn
      # ======================
      # 
      # <[A 2 3][b 2 2]> (a real object must be encoded so that CHREST attempts 
      # to add the ghost object).
      # 
      # =========================================================
      # Expected VisualSpatialFieldObject Properties for Recognised Objects
      # =========================================================
      # 
      # One distinct, recognised VisualSpatialFieldObject instance for object "A" should 
      # be encoded.  Despite ghost object encoding being enabled, object "a" is 
      # not encoded since it occupies the same coordinates as the Scene 
      # creator's avatar.
      # 
      # - Creation times
      #   ~ A: The first occurrence of "A" is in the first chunk processed so 
      #        it is encoded when the first chunk is processed.
      #        
      # - Terminus times
      #   ~ A: The last occurrence of "A" is in the first chunk processed and 
      #        no other objects (recognised or unrecognised) overwrites it.  
      #        Therefore, its lifespan will be set to its creation time plus 
      #        the lifespan specified for recognised objects.
      # 
      # =======================================================
      # Terminus for Blind Objects on Recognised Object Squares
      # =======================================================
      # 
      # The blind object on (2, 3) should be overwritten when the first chunk is 
      # processed.
      
      # Create list patterns to learn.
      list_pattern = ListPattern.new
      list_pattern.add(ItemSquarePattern.new("A", 2, 3))
      list_pattern.add(ItemSquarePattern.new("B", 2, 2))
      list_patterns_to_learn.push(list_pattern)
      
      # The first object learned should be real so add it to reality.
      reality.addItemToSquare(2, 3, "0", "A")
      
      # Add two unrecognised, non-empty objects to reality.
      reality.addItemToSquare(4, 2, "2", "G")
      reality.addItemToSquare(2, 4, "3", "F")
      
      # Add expected values for recognised VisualSpatialFieldObjects.
      expected_visual_spatial_field_object_properties[2][3].push([
        "0",
        "A",
        time_to_encode_objects,
        recognised_object_lifespan,
        true,
        false
      ])
    
      # Add coordinates to the "squares_to_be_ignored" variable.
      squares_to_be_ignored.push(Square.new(2, 3))
      
      # Set termini for blind objects to be overwritten by recognised objects.
      expected_visual_spatial_field_object_properties[2][3][0][3] = time_to_encode_objects
      
      # Set variables for recognised, unrecognised and empty square counters.
      number_recognised_chunks = 1
      number_unrecognised_objects = 2
      number_empty_squares = 9
      
      squares_to_fixate_on = [
        [4, 2],
        [2, 4]
      ]
    end
    
    # If the scene's creator has been encoded, the patterns contained in the 
    # "list_patterns_to_learn" array should have creator-specific coordinates
    # since, when learning, CHREST will translate the locations of objects so 
    # they are relative to the location of the creator in the Scene.   
    # 
    # If this isn't done, tests that make use of the data prepared in this 
    # function will not complete since they check the string representations of 
    # what is being learned against what is learned to control progression 
    # through the test itself.  For example, if reality has the creator's 
    # location encoded but the coordinates of objects to learn in the 
    # "list_patterns_to_learn" array are not translated into creator-relative 
    # coordinates then, the strings compared will be something like:
    # 
    # TO BE LEARNED        LEARNED
    # <[A 1 2][B 1 3]>     <[A -1 0][B -1 1]> 
    # 
    # This results in an infinite loop of learning within a test even though the
    # object coordinates indicate the same absolute location.
    if(encode_scene_creator)
      for i in 0...list_patterns_to_learn.count
        list_pattern_with_translated_coords = ListPattern.new
        for pattern in list_patterns_to_learn[i]
          list_pattern_with_translated_coords.add(ItemSquarePattern.new(pattern.getItem(), pattern.getColumn() - 2, pattern.getRow() - 2))
        end
        list_patterns_to_learn[i] = list_pattern_with_translated_coords
      end
    end
    
    #Populate the "scenario_data" array with the last scenario data set 
    #constructed.
    scenario_data.push([
      reality, 
      list_patterns_to_learn,
      number_recognised_chunks,
      expected_visual_spatial_field_object_properties,
      squares_to_be_ignored,
      number_unrecognised_objects,
      number_empty_squares,
      squares_to_fixate_on
    ])
  end
  
  return scenario_data
end

# This function performs a number of actions:
# 
# 1) Adds expected VisualSpatialFieldObject values for visual-spatial squares that should 
#    be empty.
# 2) Adds expected VisualSpatialFieldObject values for visual-spatial squares that should 
#    contain unrecognised, real objects.
# 3) Sets terminus values for blind objects that are placed initially on the
#    visual-spatial squares referenced in the first two actions.
# 4) Calculates the number of empty squares on the scene encoded.
# 5) Calculates the number of unrecognised objects on the scene encoded. 
# 
# Unless a coordinate is present in the "squares_to_be_ignored" data structure,
# a square represented in the current expected VisualSpatialFieldObject values data 
# structure will have actions 1-3 applied to it.
# 
# The function returns an array consisting of three elements:
# 
# 1) The modified data structure containing expected VisualSpatialFieldObject values.
# 2) The number of unrecognised objects present on the Scene passed as a 
#    parameter.
# 3) The number of empty squares present on the Scene passed as a parameter.
# 
def add_expected_values_for_unrecognised_visual_spatial_objects(
    scene_encoded_into_visual_spatial_field, 
    expected_visual_spatial_field_object_properties, 
    squares_to_be_ignored,
    time_to_encode_objects,
    time_to_encode_empty_squares,
    unrecognised_object_lifespan,
    number_chunks_recognised 
  )
    #First, overwrite all Square instances in the "squares_to_ignore" data 
    #structure with their String representations so that it can be determined if 
    #a square should be processed in the loop below (the "include?" statement 
    #will always evaluate to false otherwise since it will compare object 
    #references rather than the actual coordinates specified).
    for i in 0...squares_to_be_ignored.count
      squares_to_be_ignored[i] = squares_to_be_ignored[i].toString()
    end
    
    #For any coordinates that shouldn't be ignored:
    #
    # 1) Check if the square indicated by the coordinates in the scene encoded
    #    into the visual-spatial field should be blind.  If so, skip to the next
    #    coordinates. 
    # 2) If the square indicated by the coordinates in the scene encoded
    #    into the visual-spatial field shouldn't be blind and shouldn't be 
    #    ignored, set the terminus for the first blind object on the square, if 
    #    this hasn't been done already (the terminus for such an object should 
    #    be set if there is a ghost object on the square that will be 
    #    overwritten by an empty square).
    # 3) Add a new data structure containing the expected object values for the
    #    unrecognised object to be added to the square (the object to be added
    #    is determined by checking the contents of this square in the scene that 
    #    has been encoded into the visual-spatial field consequently, this may
    #    only be either an empty square or an unreocgnised object) and set its 
    #    values accordingly:
    #    - ID: should equal the ID of the SceneObject that exists on this square
    #          in the Scene that has been encoded into the visual-spatial field.
    #    - Class: should equal the class of the SceneObject that exists on this 
    #             square in the Scene that has been encoded into the 
    #             visual-spatial field.
    #    - Creation time: dependent on number of unrecognised objects and empty
    #                     squares encountered thus far.  Note that the main loop
    #                     here processes squares from west -> east and south -> 
    #                     north to ensure that creation time setting is correct
    #                     (this is the order in which unrecognised objects and
    #                     empty squares are encoded during visual-spatial 
    #                     construction).
    #    - Terminus: should always be equal to the specified unrecognised object
    #                lifespan since these objects shouldn't be overwritten.
    #    - Recognised status: should be false.
    #    - Ghost status: should be false.
    number_empty_squares = 0
    number_unrecognised_objects = 0
    for row in 0...scene_encoded_into_visual_spatial_field.getHeight()
      for col in 0...scene_encoded_into_visual_spatial_field.getWidth()
        
        if( !squares_to_be_ignored.include?(Square.new(col, row).toString()) )
          object_on_square_in_scene_encoded = scene_encoded_into_visual_spatial_field.getSquareContents(col, row)
          class_of_object_on_square_in_reality = object_on_square_in_scene_encoded.getObjectClass
          
          if(class_of_object_on_square_in_reality != Scene.getBlindSquareToken())
            
            if(class_of_object_on_square_in_reality == Scene.getEmptySquareToken())
              number_empty_squares += 1
            elsif(class_of_object_on_square_in_reality != Scene.getCreatorToken())
              number_unrecognised_objects += 1
            end
            
            if(class_of_object_on_square_in_reality == Scene.getCreatorToken())
              expected_visual_spatial_field_object_properties[col][row][0] = [
                object_on_square_in_scene_encoded.getIdentifier(),
                class_of_object_on_square_in_reality,
                0,
                nil,
                false,
                false
              ]
            else
              #If the blind object identifier on this square hasn't already had
              #its terminus set, do so now.
              if (expected_visual_spatial_field_object_properties[col][row][0][3] == nil)
                expected_visual_spatial_field_object_properties[col][row][0][3] = (time_to_encode_objects * (number_chunks_recognised + number_unrecognised_objects)) + (time_to_encode_empty_squares * number_empty_squares)
              end

              expected_visual_spatial_field_object_properties[col][row].push([
                object_on_square_in_scene_encoded.getIdentifier(),
                class_of_object_on_square_in_reality,
                (time_to_encode_objects * (number_chunks_recognised + number_unrecognised_objects)) + (time_to_encode_empty_squares * number_empty_squares),
                unrecognised_object_lifespan,
                false,
                false
              ])
            end
          end
        end
      end
    end
    
    return expected_visual_spatial_field_object_properties
  end
  
def get_visual_spatial_field_instantiation_complete_time(creation_time, visual_spatial_field_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_recognised_chunks, number_unrecognised_objects, number_empty_squares)
  return creation_time +
    visual_spatial_field_access_time + 
    (time_to_encode_objects * number_recognised_chunks) + 
    (time_to_encode_objects * number_unrecognised_objects) + 
    (time_to_encode_empty_squares * number_empty_squares)
end

def expected_fixations_made?(model, squares_expected_to_have_been_fixated_on)
  fixations = model.getPerceiver().getFixations()
  
  # A square may have been fixated on more than once so remove duplicates to 
  # speed up function operation.
  fixations = fixations.to_a.uniq
  
  for i in 0...squares_expected_to_have_been_fixated_on.size()
    square_to_fixate_on = squares_expected_to_have_been_fixated_on[i]
    fixations_checked = Array.new
    square_fixated_on = false

    for j in 0...fixations.size()
      fixation = fixations[j]
      fixation_to_check = [fixation.getX(), fixation.getY()]

      if(!fixations_checked.include? fixation_to_check)
        !fixations_checked.push(fixation_to_check)

        if(fixation_to_check[0] == square_to_fixate_on[0] and fixation_to_check[1] == square_to_fixate_on[1])
          square_fixated_on = true
        end
      end
    end
    
    if !square_fixated_on
      return false
    end
  end

  return true
end

def check_visual_spatial_field_against_expected(visual_spatial_field, expected_visual_spatial_field, time_to_check_recognised_status_against, test_description)

  visual_spatial_field_to_check = get_entire_visual_spatial_field(visual_spatial_field)
  
  for row in 0...visual_spatial_field.getHeight()
    for col in 0...visual_spatial_field.getWidth()
      
      visual_spatial_field_objects = visual_spatial_field_to_check.get(col).get(row)
      assert_equal(expected_visual_spatial_field[col][row].count(), visual_spatial_field_objects.size(), "occurred when checking the number of items on col " + col.to_s + ", row " + row.to_s + " " + test_description)

      for i in 0...visual_spatial_field_objects.size()
        error_message_postscript = " for object " + i.to_s  + " on col " + col.to_s + ", row " + row.to_s
        expected_visual_spatial_field_object = expected_visual_spatial_field[col][row][i]
        visual_spatial_field_object = visual_spatial_field_objects[i]
        assert_equal(expected_visual_spatial_field_object[0], visual_spatial_field_object.getIdentifier(), "occurred when checking the identifier" + error_message_postscript + " " + test_description)
        assert_equal(expected_visual_spatial_field_object[1], visual_spatial_field_object.getObjectClass(), "occurred when checking the object class" + error_message_postscript + " " + test_description)
        assert_equal(expected_visual_spatial_field_object[2], visual_spatial_field_object.getTimeCreated(), "occurred when checking the creation time" + error_message_postscript + " " + test_description)
        assert_equal(expected_visual_spatial_field_object[3], visual_spatial_field_object.getTerminus(), "occurred when checking the terminus" + error_message_postscript + " " + test_description)
        assert_equal(expected_visual_spatial_field_object[4], visual_spatial_field_object.recognised(time_to_check_recognised_status_against), "occurred when checking the recognised status" + error_message_postscript + " " + test_description)
        assert_equal(expected_visual_spatial_field_object[5], visual_spatial_field_object.isGhost(), "occurred when checking the ghost status" + error_message_postscript + " " + test_description)
      end
    end
  end
end

def get_entire_visual_spatial_field (visual_spatial_field)
  get_method = VisualSpatialField.java_class.declared_method(:get)
  get_method.accessible = true
  return get_method.invoke(visual_spatial_field.java_object).to_java()
end

def check_visual_spatial_field_at_time_against_expected (time, visual_spatial_field, expected_visual_spatial_field_at_time, test_description)
  vsf = visual_spatial_field.get(time)
  
  for col in 0...vsf.count
    for row in 0...vsf[col].count
      
      assert_equal(
        expected_visual_spatial_field_at_time[col][row].count(), 
        vsf[col][row].count(),
        "occurred when checking the number of objects on col, row (" + col.to_s + ", " + row.to_s + ") " + test_description
      )
      
      for object in 0...vsf[col][row].count
        
        expected_obj = expected_visual_spatial_field_at_time[col][row][object]
        actual_obj = vsf[col][row][object]
        error_message_postscript = " for object " + object.to_s + " (ID: " + actual_obj.getIdentifier +  
          ", class: " + actual_obj.getObjectClass() + ") on col, row (" + col.to_s + ", " + row.to_s + ")"
        
        assert_equal(expected_obj[0], actual_obj.getIdentifier(), "occurred when checking the identifier" + error_message_postscript + " " + test_description)
        assert_equal(expected_obj[1], actual_obj.getObjectClass(), "occurred when checking the object class" + error_message_postscript + " " + test_description)
        assert_equal(expected_obj[2], actual_obj.getTimeCreated(), "occurred when checking the creation time" + error_message_postscript + " " + test_description)
        assert_equal(expected_obj[3], actual_obj.getTerminus(), "occurred when checking the terminus" + error_message_postscript + " " + test_description)
        assert_equal(expected_obj[4], actual_obj.recognised(time), "occurred when checking the recognised status" + error_message_postscript + " " + test_description)
        assert_equal(expected_obj[5], actual_obj.isGhost(), "occurred when checking the ghost status" + error_message_postscript + " " + test_description)
      end
    end
  end
end
