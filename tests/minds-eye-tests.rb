##################### GLOBAL TEST FUNCTIONS ###############################

# Array returned will go from young -> old chunks (like STM).
def get_non_empty_and_non_root_visual_stm_items(model)
  visual_stm = model.getVisualStm
  non_empty_and_non_root_visual_stm_items = []
  
  for i in 0...visual_stm.getCount do
    visual_stm_item = visual_stm.getItem(i)
    if !visual_stm_item.equals(model.getVisualLtm) and !visual_stm_item.getImage().isEmpty()
      non_empty_and_non_root_visual_stm_items.push(visual_stm_item)
    end
  end
  
  return non_empty_and_non_root_visual_stm_items
end

def get_first_occurrence_of_each_object_in_stm(model)
  non_empty_and_non_root_visual_stm_items = get_non_empty_and_non_root_visual_stm_items(model).reverse
  first_occurrence_of_objects_in_stm = []
  
  for i in 0...non_empty_and_non_root_visual_stm_items.count do
    first_occurrence_of_objects_in_stm.push([])
    visual_stm_chunk = non_empty_and_non_root_visual_stm_items[i].getImage()
    
    for j in 0...visual_stm_chunk.size() do
      pattern = visual_stm_chunk.getItem(j)
      
      #Check through array to return to see if pattern is already in there.  
      pattern_already_present = false
      for k in 0...first_occurrence_of_objects_in_stm.count()
        chunk = first_occurrence_of_objects_in_stm[k]
        for pat in 0...chunk.size
          if chunk[pat].toString() == pattern.toString()
            pattern_already_present = true
          end
        end
      end
      
      #Add pattern to the end of the current index of the array to be returned
      # if it isn't already in the array.
      if !pattern_already_present
        current_chunk = first_occurrence_of_objects_in_stm[i]
        current_chunk.push(pattern)
      end
    end
  end
  
  return first_occurrence_of_objects_in_stm
end

def get_last_occurrence_of_each_object_in_stm(model)
  non_empty_and_non_root_visual_stm_items = get_non_empty_and_non_root_visual_stm_items(model).reverse
  last_occurrence_of_each_object_in_stm = []
  
  for i in 0...non_empty_and_non_root_visual_stm_items.count do
    last_occurrence_of_each_object_in_stm.push([])
    visual_stm_chunk = non_empty_and_non_root_visual_stm_items[i].getImage()
    
    for j in 0...visual_stm_chunk.size() do
      pattern = visual_stm_chunk.getItem(j)
      
      #Check through array to return to see if pattern is already in there.  
      # If it is, delete it and add it to the current array index
      for k in 0...last_occurrence_of_each_object_in_stm.count()
        chunk = last_occurrence_of_each_object_in_stm[k]
        for pat in 0...chunk.size
          if chunk[pat].toString() == pattern.toString()
            chunk.delete(pat)
          end
        end
      end
      
      last_occurrence_of_each_object_in_stm[i].push(pattern)
    end
  end
  
  return last_occurrence_of_each_object_in_stm
end

def get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
  return creation_time + 
    minds_eye_access_time + 
    (number_objects_placed * time_to_encode_objects) + 
    (number_empty_squares_placed * time_to_encode_empty_squares)
end

def get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed, object_lifespan)
  return creation_time + 
    minds_eye_access_time + 
    (number_objects_placed * time_to_encode_objects) + 
    (number_empty_squares_placed * time_to_encode_empty_squares) + 
    object_lifespan
end

################################################################################
# Tests that an entirely blind scene is handled correctly by the mind's eye
# constructor.
################################################################################
unit_test "blind_scene" do
  scene = Scene.new("blind test", 10, 10)
  
  creation_time = 0
  time_to_encode_objects = 50
  time_to_encode_empty_squares = 0
  minds_eye_access_time = 100
  time_to_move_object = 250
  lifespan_for_recognised_objects = 10000
  lifespan_for_unrecognised_objects = 5000
  number_fixations = rand(1..100)
  
  model = Chrest.new
  model.setDomain(GenericDomain.new(model))
  
  minds_eye = MindsEye.new(
    model,
    scene, 
    time_to_encode_objects,
    time_to_encode_empty_squares,
    minds_eye_access_time, 
    time_to_move_object, 
    lifespan_for_recognised_objects,
    lifespan_for_unrecognised_objects,
    number_fixations,
    creation_time
  )
  
  assert_equal(creation_time + minds_eye_access_time, model.getAttentionClock)
  for row in 0...scene.getHeight()
    for col in 0...scene.getWidth()
      objects = minds_eye.getObjectsOnVisualSpatialSquare(col, row)
      for i in 0...objects.size
        object = objects[i]
        assert_equal(Scene.getBlindSquareIdentifier(), object.getIdentifier())
        assert_equal(creation_time + minds_eye_access_time, object.getTimeCreated())
        assert_equal(nil, object.getTerminus())
        assert_false(object.recognised(model.getAttentionClock))
      end
    end
  end
end

################################################################################
# The scene used to instantiate the mind's eye in this test resembles a 
# diamond (makes test harder).  A diagram of the final scene can be found below:
# "x" represents a "blind spot", single upper-case characters represent 
# distinct objects, numbers represent the scene and mind's eye x and y 
# coordinates, respectively. 
#
#                -------
# 4    x      x  |  C  |  x     x
#          ------------------- 
# 3    x   |     |     |  D  |  x
#    -------------------------------
# 2  |     | A,B |     |     |     |
#    -------------------------------
# 1    x   |     |     |  E  |  x
#          -------------------
# 0    x      x  |     |  x     x
#                -------
#      0      1     2     3     4
process_test "constructor (scene with non-creator-specific coordinates)" do
  
  ###########################
  ##### CONSTRUCT SCENE #####
  ###########################
  
  # Set the objects we will be using in a 2D array.  Each object has its own
  # array whose element contents are as described below:
  # 
  # - First element: object identifier
  # - Second element: time the object was added to the visual-spatial field 
  #   (created)
  # - Third element: terminus 
  # - Fourth element: recognised status
  # 
  # Second and third elements will be set later since they depend on whether 
  # an object was a part of a visual STM chunk or not and what chunk they were
  # a part of.
  objects_creation_termini_recognised = [
    ["A", nil, nil, false],
    ["B", nil, nil, false],
    ["C", nil, nil, false],
    ["D", nil, nil, false],
    ["E", nil, nil, false],
    ["F", nil, nil, false]
  ]
  
  # Create the scene to be transposed into the mind's eye (exclude object F for
  # now).
  scene = Scene.new("Test scene", 5, 5)
  scene.addItemToSquare(2, 0, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(1, 1, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(2, 1, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(3, 1, objects_creation_termini_recognised[4][0])
  scene.addItemToSquare(0, 2, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(1, 2, objects_creation_termini_recognised[0][0])
  scene.addItemToSquare(1, 2, objects_creation_termini_recognised[1][0])
  scene.addItemToSquare(2, 2, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(3, 2, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(4, 2, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(1, 3, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(2, 3, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(3, 3, objects_creation_termini_recognised[3][0])
  scene.addItemToSquare(2, 4, objects_creation_termini_recognised[2][0])
  
  ##########################################
  ##### SET INDEPENDENT TEST VARIABLES #####
  ##########################################
  
  domain_time = 0
  time_to_encode_objects = 50
  time_to_encode_empty_squares = 0
  minds_eye_access_time = 100
  time_to_move_object = 250
  lifespan_for_recognised_objects = 10000
  lifespan_for_unrecognised_objects = 5000
  number_fixations = rand(1..13) # Used by CHREST for learning and mind's eye 
                                  # for scanning. This value will ultimately 
                                  # affect how many items in the scene may 
                                  # potentially be recognised during mind's eye 
                                  # instantiation thus affecting a number of 
                                  # timing variables.  The test is flexible 
                                  # enough to cope with any value but 1-9 is 
                                  # used since at least 1 fixation is needed and 
                                  # the total number of visible squares in the 
                                  # environment is 13.

  ##################################
  ##### CREATE CHREST INSTANCE #####
  ##################################
  
  # Create a new CHREST instance and set its domain (important to enable correct
  # or expected perceptual mechanisms).
  model = Chrest.new
  model.setDomain(GenericDomain.new(model))
  
  #######################
  ##### LEARN SCENE #####
  #######################
  
  # Learn for a minute.
  until domain_time == 6000 do
    model.learnScene(scene, number_fixations, domain_time)
    domain_time += 1
  end
  
  ######################
  ##### EDIT SCENE #####
  ######################
  
  # Add in a new object on the same coordinates as object "e" so that we can
  # determine if unrecognised objects are processed correctly.
  scene.addItemToSquare(3, 1, objects_creation_termini_recognised[5][0])
  
  ##################################
  ##### INSTANTIATE MIND'S EYE #####
  ##################################
  
  creation_time = domain_time

  minds_eye = MindsEye.new(
    model,
    scene, 
    time_to_encode_objects,
    time_to_encode_empty_squares,
    minds_eye_access_time, 
    time_to_move_object, 
    lifespan_for_recognised_objects,
    lifespan_for_unrecognised_objects,
    number_fixations,
    domain_time
  )

  #########################################
  ##### CALCULATE DEPENDENT VARIABLES #####
  #########################################
  
  # Get chunks that were recognised when scanning the Scene that was being
  # transposed into the mind's eye (visual STM will still be in the state it was 
  # when the mind's eye was created).
  non_empty_and_non_root_visual_stm_items = get_non_empty_and_non_root_visual_stm_items(model)
  
  # Calculate item occurrence in chunks (needed for various calculations below).
  first_occurrence_of_each_object_in_stm = get_first_occurrence_of_each_object_in_stm(model)
  last_occurrence_of_each_object_in_stm = get_last_occurrence_of_each_object_in_stm(model)
  
  # Calculate the number of objects that were recognised (needed for various 
  # calculations below).
  number_recognised_objects = 0;
  for i in 0...first_occurrence_of_each_object_in_stm.count do
    number_recognised_objects += first_occurrence_of_each_object_in_stm[i].count
  end
  
  # Since some objects in the scene may have been learned (with the definite 
  # exception of object f), the expected attention free time should equal:
  # 1) The time that the minds eye was created plus...
  # 2) The time taken to access the minds eye plus...
  # 3) The time taken to encode one object multiplied by the value of 
  #    "number_visual_stm_chunks" chunks (it takes the same length of time to 
  #    encode all items in a chunk as it does to encode one individual object). 
  #    Plus...
  # 4) The time taken to encode one object multiplied by the number of 
  #    recognised objects subtracted from the total number of objects (number of
  #    objects that were not part of chunks and were thus placed individually).
  # 5) The time taken to encode 5 empty squares.
  expected_attention_free_time = (
    creation_time +
    minds_eye_access_time + 
    (time_to_encode_objects * non_empty_and_non_root_visual_stm_items.count()) + 
    (time_to_encode_objects * ((objects_creation_termini_recognised.count()) - number_recognised_objects)) + 
    (time_to_encode_empty_squares * 9) 
  )
  
  # Calculate creation values for recognised objects and set the recognised 
  # status for any recognised objects accordingly.
  number_objects_placed = 0;
  number_empty_squares_placed = 0;
  for i in 0...first_occurrence_of_each_object_in_stm.count
    number_objects_placed += 1
    creation = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
    for j in 0...first_occurrence_of_each_object_in_stm[i].count
      for k in 0...objects_creation_termini_recognised.count
        if objects_creation_termini_recognised[k][0] == first_occurrence_of_each_object_in_stm[i][j].getItem()
          objects_creation_termini_recognised[k][1] = creation
          objects_creation_termini_recognised[k][3] = true
        end
      end
    end
  end
  
  # Calculate terminus values for recognised objects (reset number objects/empty
  # squares placed variables since they'll have been incremented above and 
  # they'd finish being double the amount otherwise).
  number_objects_placed = 0;
  number_empty_squares_placed = 0;
  for i in 0...last_occurrence_of_each_object_in_stm.count
    number_objects_placed += 1
    terminus = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed, lifespan_for_recognised_objects)
    for j in 0...last_occurrence_of_each_object_in_stm[i].count
      for k in 0...objects_creation_termini_recognised.count
        if objects_creation_termini_recognised[k][0] == last_occurrence_of_each_object_in_stm[i][j].getItem()
          objects_creation_termini_recognised[k][2] = terminus
        end
      end
    end
  end
  
  # Calculate creation and terminus times for unrecognised objects.  This must 
  # be done in order of mind's eye object encoding since objects are placed in a 
  # specified order.
  number_empty_squares_placed += 1
  expected_terminus_blind_object_1 = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
  
  number_empty_squares_placed += 1
  expected_terminus_blind_object_2 = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
  
  number_empty_squares_placed += 1
  expected_terminus_blind_object_3 = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
  
  #Object E
  if(objects_creation_termini_recognised[4][1] == nil and objects_creation_termini_recognised[4][2] == nil)
    number_objects_placed += 1
    objects_creation_termini_recognised[4][1] = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
  end
  
  # Object F
  number_objects_placed += 1
  objects_creation_termini_recognised[5][1] = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
  objects_creation_termini_recognised[5][2] = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed, lifespan_for_unrecognised_objects)
  
  #E's expected terminus is dependent on F's so is set here.
  if(objects_creation_termini_recognised[4][3])
    objects_creation_termini_recognised[4][2] = objects_creation_termini_recognised[5][2] + (lifespan_for_recognised_objects - lifespan_for_unrecognised_objects)
  else
    objects_creation_termini_recognised[4][2] = objects_creation_termini_recognised[5][2]
  end
  
  number_empty_squares_placed += 1
  expected_terminus_blind_object_4 =  get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
  
  #Object A
  if(objects_creation_termini_recognised[0][1] == nil and objects_creation_termini_recognised[0][2] == nil)
    number_objects_placed += 1
    objects_creation_termini_recognised[0][1] = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
    objects_creation_termini_recognised[0][2] = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed, lifespan_for_unrecognised_objects)
  end
  
  #Object B
  if(objects_creation_termini_recognised[1][1] == nil and objects_creation_termini_recognised[1][2] == nil)
    number_objects_placed += 1
    objects_creation_termini_recognised[1][1] = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
    objects_creation_termini_recognised[1][2] = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed, lifespan_for_unrecognised_objects)
  
    #If B was not recognised then A's terminus will have been updated so set its
    #terminus here accordingly.
    if(objects_creation_termini_recognised[0][3])
      objects_creation_termini_recognised[0][2] = objects_creation_termini_recognised[1][2] + (lifespan_for_recognised_objects - lifespan_for_unrecognised_objects)
    else
      objects_creation_termini_recognised[0][2] = objects_creation_termini_recognised[1][2]
    end
    
  end
  
  number_empty_squares_placed += 1
  expected_terminus_blind_object_5 =  get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
  
  number_empty_squares_placed += 1
  expected_terminus_blind_object_6 =  get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
  
  number_empty_squares_placed += 1
  expected_terminus_blind_object_7 =  get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
  
  number_empty_squares_placed += 1
  expected_terminus_blind_object_8 =  get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
  
  number_empty_squares_placed += 1
  expected_terminus_blind_object_9 =  get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
  
  #Object C
  if(objects_creation_termini_recognised[2][1] == nil and objects_creation_termini_recognised[2][2] == nil)
    number_objects_placed += 1
    objects_creation_termini_recognised[2][1] = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
    objects_creation_termini_recognised[2][2] = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed, lifespan_for_unrecognised_objects)
  end
  
  #Object D
  if(objects_creation_termini_recognised[3][1] == nil and objects_creation_termini_recognised[3][2] == nil)
    number_objects_placed += 1
    objects_creation_termini_recognised[3][1] = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
    objects_creation_termini_recognised[3][2] = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed, lifespan_for_unrecognised_objects)
  end

  #################
  ##### TESTS #####
  #################
  
  for row in 0...scene.getHeight()
    for col in 0...scene.getWidth()
      
      objects_on_square = minds_eye.getObjectsOnVisualSpatialSquare(col, row)
      
      # Check number of objects on square.  Only 1 object is expected on most 
      # squares (since all squares start blind) however, sometimes either 2 or 3 
      # may be expected (2 if the square should contain 1 object, 3 if it should
      # contain 2).
      number_objects_on_square = objects_on_square.size()
      expected_number_objects_on_square = 1
      if((col == 3 and row == 3) or (col == 2 and row == 4))
        expected_number_objects_on_square = 2
      elsif((col == 3 and row == 1) or (col == 1 and row == 2) )
        expected_number_objects_on_square = 3
      end
      assert_equal(expected_number_objects_on_square, number_objects_on_square, "occurred when checking the number of mind's eye objects on xy coordinates " + col.to_s + ", " + row.to_s)
      
      for i in 0...number_objects_on_square
        expected_item_identifier = Scene.getBlindSquareIdentifier()
        expected_creation_time = creation_time + minds_eye_access_time
        expected_terminus_time = nil
        expected_recognised_status = false
        
        if(col == 2 and row == 0)
          if(i == 0)
            expected_terminus_time = expected_terminus_blind_object_1
          end
        elsif(col == 1 and row == 1)
          if(i == 0)
            expected_terminus_time = expected_terminus_blind_object_2
          end
        elsif(col == 2 and row == 1)
          if(i == 0)
            expected_terminus_time = expected_terminus_blind_object_3
          end
        elsif(col == 3 and row == 1)
          if(i == 0)
            expected_terminus_time = objects_creation_termini_recognised[4][1]
          elsif(i == 1)
            expected_item_identifier = objects_creation_termini_recognised[4][0]
            expected_creation_time = objects_creation_termini_recognised[4][1]
            expected_terminus_time = objects_creation_termini_recognised[4][2]
            expected_recognised_status = objects_creation_termini_recognised[4][3]
          elsif(i == 2)
            expected_item_identifier = objects_creation_termini_recognised[5][0]
            expected_creation_time = objects_creation_termini_recognised[5][1]
            expected_terminus_time = objects_creation_termini_recognised[5][2]
            expected_recognised_status = objects_creation_termini_recognised[5][3]
          end
        elsif(col == 0 and row == 2)
          if(i == 0)
            expected_terminus_time = expected_terminus_blind_object_4
          end
        elsif(col == 1 and row == 2)
          if(i == 0)
            expected_terminus_time = objects_creation_termini_recognised[0][1]
          elsif(i == 1)
            expected_item_identifier = objects_creation_termini_recognised[0][0]
            expected_creation_time = objects_creation_termini_recognised[0][1]
            expected_terminus_time = objects_creation_termini_recognised[0][2]
            expected_recognised_status = objects_creation_termini_recognised[0][3]
          elsif(i == 2)
            expected_item_identifier = objects_creation_termini_recognised[1][0]
            expected_creation_time = objects_creation_termini_recognised[1][1]
            expected_terminus_time = objects_creation_termini_recognised[1][2]
            expected_recognised_status = objects_creation_termini_recognised[1][3]
          end
        elsif(col == 2 and row == 2)
          if(i == 0)
            expected_terminus_time = expected_terminus_blind_object_5
          end
        elsif(col == 3 and row == 2)
          if(i == 0)
            expected_terminus_time = expected_terminus_blind_object_6
          end
        elsif(col == 4 and row == 2)
          if(i == 0)
            expected_terminus_time = expected_terminus_blind_object_7
          end
        elsif(col == 1 and row == 3)
          if(i == 0)
            expected_terminus_time = expected_terminus_blind_object_8
          end
        elsif(col == 2 and row == 3)
          if(i == 0)
            expected_terminus_time = expected_terminus_blind_object_9
          end
        elsif(col == 3 and row == 3)
          if(i == 0)
            expected_terminus_time = objects_creation_termini_recognised[3][1]
          elsif(i == 1)
            expected_item_identifier = objects_creation_termini_recognised[3][0]
            expected_creation_time = objects_creation_termini_recognised[3][1]
            expected_terminus_time = objects_creation_termini_recognised[3][2]
            expected_recognised_status = objects_creation_termini_recognised[3][3]
          end
        elsif(col == 2 and row == 4)
          if(i == 0)
            expected_terminus_time = objects_creation_termini_recognised[2][1]
          elsif(i == 1)
            expected_item_identifier = objects_creation_termini_recognised[2][0]
            expected_creation_time = objects_creation_termini_recognised[2][1]
            expected_terminus_time = objects_creation_termini_recognised[2][2]
            expected_recognised_status = objects_creation_termini_recognised[2][3]
          end
        end
        
        object = objects_on_square.get(i)
        assert_equal(expected_item_identifier, object.getIdentifier(), "occurred when checking item identifier for item " + (i + 1).to_s + " (" + object.getIdentifier() + ") on xy coordinates " + col.to_s + ", " + row.to_s)
        assert_equal(expected_creation_time, object.getTimeCreated(), "occurred when checking time of creation for item " + (i + 1).to_s + " (" + object.getIdentifier() + ") on xy coordinates " + col.to_s + ", " + row.to_s)
        assert_equal(expected_terminus_time, object.getTerminus(), "occurred when checking terminus time for item " + (i + 1).to_s + " (" + object.getIdentifier() + ") on xy coordinates " + col.to_s + ", " + row.to_s)
        
        # The creation time of the last object is passed here since this is the
        # last object to be created therefore, the recognised history for each 
        # object will be in its final state.
        assert_equal(expected_recognised_status, object.recognised(objects_creation_termini_recognised[5][1]), "occurred when checking recognised status for item " + (i + 1).to_s + " (" + object.getIdentifier() + ") on xy coordinates " + col.to_s + ", " + row.to_s)
      end
    end
  end
  
  assert_equal(expected_attention_free_time, model.getAttentionClock(), "occurred when checking the time the CHREST model assosicated with the mind's eye will be free after mind's eye instantiation.")

end

################################################################################
# The scene used to construct the mind's eye in this test resembles a "cone" 
# of vision i.e. the further ahead the observer sees, the wider its field of 
# vision (makes test harder).  A diagram of this scene can be found below:
# "x" represents a "blind spot", single upper-case characters represent 
# distinct objects, the outer-most numbers represent the coordinates relative 
# to the creator while the inner-most numbers represent the minds eye x and y 
# coordinates. 
# 
#        -------------------------------
# 3    3 |     | C,D |     |     |  E  | 
#        -------------------------------
# 2    2    x  |     |  B  |     |  x
#              -------------------
# 1    1    x     x  |  A  |  x     x
#                    -------
# 0    0    x     x  |SELF |  x     x
#                    -------
#           0     1     2     3     4    MIND'S EYE COORDS
#          
#          -2    -1     0     1     2    CREATOR-RELATIVE COORDS
process_test "constructor (scene with creator-specific coordinates)" do
  
  ###########################
  ##### CONSTRUCT SCENE #####
  ###########################
  
  # Set the objects we will be using in a 2D array.  Each object has its own
  # array whose element contents are as described below:
  # 
  # - First element: object identifier
  # - Second element: time the object was added to the visual-spatial field 
  #   (created)
  # - Third element: terminus 
  # - Fourth element: recognised status
  # 
  # Second and third elements will be set later since they depend on whether 
  # an object was a part of a visual STM chunk or not and what chunk they were
  # a part of.
  objects_creation_termini_recognised = [
    ["A", nil, nil, false],
    ["B", nil, nil, false],
    ["C", nil, nil, false],
    ["D", nil, nil, false],
    ["E", nil, nil, false],
    ["F", nil, nil, false]
  ]
  
  # Create the initial Scene to be transposed into the mind's eye.
  scene = Scene.new("Test scene", 5, 4)
  scene.addItemToSquare(2, 0, Scene.getSelfIdentifier())
  scene.addItemToSquare(2, 1, objects_creation_termini_recognised[0][0])
  scene.addItemToSquare(1, 2, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(2, 2, objects_creation_termini_recognised[1][0])
  scene.addItemToSquare(3, 2, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(0, 3, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(1, 3, objects_creation_termini_recognised[2][0])
  scene.addItemToSquare(1, 3, objects_creation_termini_recognised[3][0])
  scene.addItemToSquare(2, 3, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(3, 3, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(4, 3, objects_creation_termini_recognised[4][0])
  
  ##########################################
  ##### SET INDEPENDENT TEST VARIABLES #####
  ##########################################
  
  domain_time = 0
  time_to_encode_objects = 50
  time_to_encode_empty_squares = 0
  minds_eye_access_time = 100
  time_to_move_object = 250
  lifespan_for_recognised_objects = 10000
  lifespan_for_unrecognised_objects = 5000
  number_fixations = rand(1..9) # Used by CHREST for learning and mind's eye 
                                 # for scanning. This value will ultimately 
                                 # affect how many items in the scene may 
                                 # potentially be recognised during mind's eye 
                                 # instantiation thus affecting a number of 
                                 # timing variables. The test is flexible enough 
                                 # to cope with any value but 1-9 is used since 
                                 # at least 1 fixation is needed and the total 
                                 # number of visible squares in the environment 
                                 # is 9.
  
  ##################################
  ##### CREATE CHREST INSTANCE #####
  ##################################
  
  # Create a new CHREST instance and set its domain (important to enable correct
  # or expected perceptual mechanisms).
  model = Chrest.new
  model.setDomain(GenericDomain.new(model))
  
  #######################
  ##### LEARN SCENE #####
  #######################
  
  # Learn for a minute.
  until domain_time == 6000 do
    model.learnScene(scene, number_fixations, domain_time)
    domain_time += 1
  end
  
  ######################
  ##### EDIT SCENE #####
  ######################
  
  # Add in a new object on the same coordinates as object "e" so that we can
  # determine if unrecognised objects are processed correctly.
  scene.addItemToSquare(4, 3, objects_creation_termini_recognised[5][0])
  
  ##################################
  ##### INSTANTIATE MIND'S EYE #####
  ##################################
  
  creation_time = domain_time
  
  minds_eye = MindsEye.new(
    model,
    scene, 
    time_to_encode_objects,
    time_to_encode_empty_squares,
    minds_eye_access_time, 
    time_to_move_object, 
    lifespan_for_recognised_objects,
    lifespan_for_unrecognised_objects,
    number_fixations,
    creation_time
  )
  
  #########################################
  ##### CALCULATE DEPENDENT VARIABLES #####
  #########################################
  
  # Get chunks that were recognised when scanning the Scene that was being
  # transposed into the mind's eye (visual STM will still be in the state it was 
  # when the mind's eye was created).
  non_empty_and_non_root_visual_stm_items = get_non_empty_and_non_root_visual_stm_items(model)
  
  # Calculate item occurrence in chunks (needed for various calculations below).
  first_occurrence_of_each_object_in_stm = get_first_occurrence_of_each_object_in_stm(model)
  last_occurrence_of_each_object_in_stm = get_last_occurrence_of_each_object_in_stm(model)
  
  # Calculate the number of objects that were recognised (needed for various 
  # calculations below).
  number_recognised_objects = 0;
  for i in 0...first_occurrence_of_each_object_in_stm.count do
    number_recognised_objects += first_occurrence_of_each_object_in_stm[i].count
  end
  
  # Since some objects in the scene may have been learned (with the definite 
  # exception of the self and object f), the expected attention free time should 
  # equal:
  # 1) The domain time that the minds eye was created plus...
  # 2) The time taken to access the minds eye plus...
  # 3) The time taken to encode one object multiplied by the value of 
  #    "number_visual_stm_chunks" chunks (it takes the same length of time to 
  #    encode all items in a chunk as it does to encode one individual object). 
  #    Plus...
  # 4) The time taken to encode one object multiplied by the number of 
  #    recognised objects subtracted from the total number of objects (number of
  #    objects that were not part of chunks and were thus placed individually).
  # 5) The time taken to encode 5 empty squares.
  expected_attention_free_time = (
    creation_time +
    minds_eye_access_time + 
    (time_to_encode_objects * non_empty_and_non_root_visual_stm_items.count()) + 
    (time_to_encode_objects * ((objects_creation_termini_recognised.count() + 1) - number_recognised_objects)) + #Plus 1 is for "SELF"
    (time_to_encode_empty_squares * 5) 
  )
  
  # Calculate creation values for recognised objects and set the recognised 
  # status for any recognised objects accordingly.
  number_objects_placed = 0;
  number_empty_squares_placed = 0;
  for i in 0...first_occurrence_of_each_object_in_stm.count
    number_objects_placed += 1
    creation = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
    for j in 0...first_occurrence_of_each_object_in_stm[i].count
      for k in 0...objects_creation_termini_recognised.count
        if objects_creation_termini_recognised[k][0] == first_occurrence_of_each_object_in_stm[i][j].getItem()
          objects_creation_termini_recognised[k][1] = creation
          objects_creation_termini_recognised[k][3] = true
        end
      end
    end
  end
  
  # Calculate terminus values for recognised objects (reset number objects/empty
  # squares placed variables since they'll have been incremented above and 
  # they'd finish being double the amount otherwise).
  number_objects_placed = 0;
  number_empty_squares_placed = 0;
  for i in 0...last_occurrence_of_each_object_in_stm.count
    number_objects_placed += 1
    terminus = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed, lifespan_for_recognised_objects)
    for j in 0...last_occurrence_of_each_object_in_stm[i].count
      for k in 0...objects_creation_termini_recognised.count
        if objects_creation_termini_recognised[k][0] == last_occurrence_of_each_object_in_stm[i][j].getItem()
          objects_creation_termini_recognised[k][2] = terminus
        end
      end
    end
  end
  
  # Calculate creation and terminus times for unrecognised objects.  This must 
  # be done in order of mind's eye object encoding since objects are placed in a 
  # specified order.
  number_objects_placed += 1
  self_creation_time = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
  self_terminus_time = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed, lifespan_for_unrecognised_objects)
  
  #Object A
  if(objects_creation_termini_recognised[0][1] == nil and objects_creation_termini_recognised[0][2] == nil)
    number_objects_placed += 1
    objects_creation_termini_recognised[0][1] = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
    objects_creation_termini_recognised[0][2] = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed, lifespan_for_unrecognised_objects)
  end
  
  number_empty_squares_placed += 1
  expected_terminus_blind_object_1 =  creation_time + minds_eye_access_time + (time_to_encode_objects * number_objects_placed) + (time_to_encode_empty_squares * number_empty_squares_placed)
  
  #Object B
  if(objects_creation_termini_recognised[1][1] == nil and objects_creation_termini_recognised[1][2] == nil)
    number_objects_placed += 1
    objects_creation_termini_recognised[1][1] = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
    objects_creation_termini_recognised[1][2] = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed, lifespan_for_unrecognised_objects)
  end
  
  number_empty_squares_placed += 1
  expected_terminus_blind_object_2 =  creation_time + minds_eye_access_time + (time_to_encode_objects * number_objects_placed) + (time_to_encode_empty_squares * number_empty_squares_placed)
  
  number_empty_squares_placed += 1
  expected_terminus_blind_object_3 =  creation_time + minds_eye_access_time + (time_to_encode_objects * number_objects_placed) + (time_to_encode_empty_squares * number_empty_squares_placed)
  
  #Object C
  if(objects_creation_termini_recognised[2][1] == nil and objects_creation_termini_recognised[2][2] == nil)
    number_objects_placed += 1
    objects_creation_termini_recognised[2][1] = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
    objects_creation_termini_recognised[2][2] = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed, lifespan_for_unrecognised_objects)
  end
  
  #Object D
  if(objects_creation_termini_recognised[3][1] == nil and objects_creation_termini_recognised[3][2] == nil)
    number_objects_placed += 1
    objects_creation_termini_recognised[3][1] = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
    objects_creation_termini_recognised[3][2] = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed, lifespan_for_unrecognised_objects)
  
    #If D was not recognised then C's terminus will have been updated.
    if(objects_creation_termini_recognised[2][3])
      objects_creation_termini_recognised[2][2] = objects_creation_termini_recognised[3][2] + (lifespan_for_recognised_objects - lifespan_for_unrecognised_objects)
    else
      objects_creation_termini_recognised[2][2] = objects_creation_termini_recognised[3][2]
    end
  end
  
  number_empty_squares_placed += 1
  expected_terminus_blind_object_4 =  creation_time + minds_eye_access_time + (time_to_encode_objects * number_objects_placed) + (time_to_encode_empty_squares * number_empty_squares_placed)
  
  number_empty_squares_placed += 1
  expected_terminus_blind_object_5 =  creation_time + minds_eye_access_time + (time_to_encode_objects * number_objects_placed) + (time_to_encode_empty_squares * number_empty_squares_placed)
  
  #Object E
  if(objects_creation_termini_recognised[4][1] == nil and objects_creation_termini_recognised[4][2] == nil)
    number_objects_placed += 1
    objects_creation_termini_recognised[4][1] = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
  end
  
  #Object F
  number_objects_placed += 1
  objects_creation_termini_recognised[5][1] = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed)
  objects_creation_termini_recognised[5][2] = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, number_objects_placed, number_empty_squares_placed, lifespan_for_unrecognised_objects)
  
  #E's expected terminus is dependent on F's so is set here.
  if(objects_creation_termini_recognised[4][3])
    objects_creation_termini_recognised[4][2] = objects_creation_termini_recognised[5][2] + (lifespan_for_recognised_objects - lifespan_for_unrecognised_objects)
  else
    objects_creation_termini_recognised[4][2] = objects_creation_termini_recognised[5][2]
  end
  
  #################
  ##### TESTS #####
  #################
  
  for row in 0...scene.getHeight()
    for col in 0...scene.getWidth()
      
      objects_on_square = minds_eye.getObjectsOnVisualSpatialSquare(col, row)
      
      # Check number of objects on square.  Only 1 object is expected on most 
      # squares (since all squares start blind) however, sometimes either 2 or 3 
      # may be expected (2 if the square should contain 1 object, 3 if it should
      # contain 2).
      number_objects_on_square = objects_on_square.size()
      expected_number_objects_on_square = 1
      if( (col == 2 and row == 0) or (col == 2 and row == 1) or (col == 2 and row == 2) )
        expected_number_objects_on_square = 2
      elsif( (col == 1 and row == 3) or (col == 4 and row == 3) )
        expected_number_objects_on_square = 3
      end
      assert_equal(expected_number_objects_on_square, number_objects_on_square, "occurred when checking the number of mind's eye objects on xy coordinates " + col.to_s + ", " + row.to_s)
      
      for i in 0...number_objects_on_square
        expected_item_identifier = Scene.getBlindSquareIdentifier()
        expected_creation_time = creation_time + minds_eye_access_time
        expected_terminus_time = nil
        expected_recognised_status = false
        
        if(col == 2 and row == 0)
          if(i == 0)
            expected_terminus_time = self_creation_time
          elsif(i == 1)
            expected_item_identifier = Scene.getSelfIdentifier
            expected_creation_time = self_creation_time
            expected_terminus_time = self_terminus_time
          end
        elsif(col == 2 and row == 1)
          if(i == 0)
            expected_terminus_time = objects_creation_termini_recognised[0][1]
          elsif(i == 1)
            expected_item_identifier = objects_creation_termini_recognised[0][0]
            expected_creation_time = objects_creation_termini_recognised[0][1]
            expected_terminus_time = objects_creation_termini_recognised[0][2]
            expected_recognised_status = objects_creation_termini_recognised[0][3]
          end
        elsif(col == 1 and row == 2)
          if(i == 0)
            expected_terminus_time = expected_terminus_blind_object_1
          end
        elsif(col == 2 and row == 2)
          if(i == 0)
            expected_terminus_time = objects_creation_termini_recognised[1][1]
          elsif(i == 1)
            expected_item_identifier = objects_creation_termini_recognised[1][0]
            expected_creation_time = objects_creation_termini_recognised[1][1]
            expected_terminus_time = objects_creation_termini_recognised[1][2]
            expected_recognised_status = objects_creation_termini_recognised[1][3]
          end
        elsif(col == 3 and row == 2)
          if(i == 0)
            expected_terminus_time =  expected_terminus_blind_object_2
          end
        elsif(col == 0 and row == 3)
          if(i == 0)
            expected_terminus_time =  expected_terminus_blind_object_3
          end
        elsif(col == 1 and row == 3)
          if(i == 0)
            expected_terminus_time = objects_creation_termini_recognised[2][1]
          elsif(i == 1)
            expected_item_identifier = objects_creation_termini_recognised[2][0]
            expected_creation_time = objects_creation_termini_recognised[2][1]
            expected_terminus_time = objects_creation_termini_recognised[2][2]
            expected_recognised_status = objects_creation_termini_recognised[2][3]
          elsif(i == 2)    
            expected_item_identifier = objects_creation_termini_recognised[3][0]
            expected_creation_time = objects_creation_termini_recognised[3][1]
            expected_terminus_time = objects_creation_termini_recognised[3][2]
            expected_recognised_status = objects_creation_termini_recognised[3][3]
          end
        elsif(col == 2 and row == 3)
          if(i == 0)
            expected_terminus_time =  expected_terminus_blind_object_4
          end
        elsif(col == 3 and row == 3)
          if(i == 0)
            expected_terminus_time =  expected_terminus_blind_object_5
          end
        elsif(col == 4 and row == 3)
          if(i == 0)
            expected_terminus_time = objects_creation_termini_recognised[4][1]
          elsif(i == 1)
            expected_item_identifier = objects_creation_termini_recognised[4][0]
            expected_creation_time = objects_creation_termini_recognised[4][1]
            expected_terminus_time = objects_creation_termini_recognised[4][2]
            expected_recognised_status = objects_creation_termini_recognised[4][3]
          elsif(i == 2)    
            expected_item_identifier = objects_creation_termini_recognised[5][0]
            expected_creation_time = objects_creation_termini_recognised[5][1]
            expected_terminus_time = objects_creation_termini_recognised[5][2]
            expected_recognised_status = objects_creation_termini_recognised[5][3]
          end
        end
        
        object = objects_on_square.get(i)
        assert_equal(expected_item_identifier, object.getIdentifier(), "occurred when checking item identifier for item " + (i + 1).to_s + " (" + object.getIdentifier() + ") on xy coordinates " + col.to_s + ", " + row.to_s)
        assert_equal(expected_creation_time, object.getTimeCreated(), "occurred when checking time of creation for item " + (i + 1).to_s + " (" + object.getIdentifier() + ") on xy coordinates " + col.to_s + ", " + row.to_s)
        assert_equal(expected_terminus_time, object.getTerminus(), "occurred when checking terminus time for item " + (i + 1).to_s + " (" + object.getIdentifier() + ") on xy coordinates " + col.to_s + ", " + row.to_s)
        
        # The creation time of the last object is passed here since this is the
        # last object to be created therefore, the recognised history for each 
        # object will be in its final state.
        assert_equal(expected_recognised_status, object.recognised(objects_creation_termini_recognised[5][1]), "occurred when checking recognised status for item " + (i + 1).to_s + " (" + object.getIdentifier() + ") on xy coordinates " + col.to_s + ", " + row.to_s)
      end
    end
  end
  
  assert_equal(expected_attention_free_time, model.getAttentionClock(), "occurred when checking the time the CHREST model assosicated with the mind's eye will be free after mind's eye instantiation.")
end

################################################################################
# The scene used to instantiate the mind's eye in this test resembles a 
# diamond (makes test harder).  A diagram of the final scene can be found below:
# "x" represents a "blind spot", single upper-case characters represent 
# distinct objects, numbers represent the scene and mind's eye x and y 
# coordinates, respectively. 
#
#                -------
# 4    x      x  |  C  |  x     x
#          ------------------- 
# 3    x   |     |     |  D  |  x
#    -------------------------------
# 2  |     | A,B |     |     |     |
#    -------------------------------
# 1    x   |     |     | E,F |  x
#          -------------------
# 0    x      x  |     |  x     x
#                -------
#      0      1     2     3     4
unit_test "get-objects-on-visual-spatial-square" do
  
  scene = Scene.new("Test scene", 5, 5)
  scene.addItemToSquare(2, 0, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(1, 1, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(2, 1, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(3, 1, "E")
  scene.addItemToSquare(3, 1, "F")
  scene.addItemToSquare(0, 2, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(1, 2, "A")
  scene.addItemToSquare(1, 2, "B")
  scene.addItemToSquare(2, 2, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(3, 2, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(4, 2, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(1, 3, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(2, 3, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(3, 3, "D")
  scene.addItemToSquare(2, 4, "C")
  
  ##################################
  ##### CREATE CHREST INSTANCE #####
  ##################################

  model = Chrest.new
  model.setDomain(GenericDomain.new(model))
  
  ##################################
  ##### INSTANTIATE MIND'S EYE #####
  ##################################
  
  minds_eye = MindsEye.new(
    model,
    scene, 
    50,
    0,
    100, 
    250, 
    10000,
    5000,
    8,
    0
  )
  
  #################
  ##### TESTS #####
  #################
  
  for row in 0...scene.getHeight()
    for col in 0...scene.getWidth()
      minds_eye_coord_content = minds_eye.getObjectsOnVisualSpatialSquare(col, row)
      
      expected_number_objects = 1
      expected_identifier = Scene.getBlindSquareIdentifier
      
      for i in 0...minds_eye_coord_content.size
        if(col == 3 and row == 1)
          expected_number_objects = 3
          if(i == 1)
            expected_identifier = "E"
          elsif(i == 2)
            expected_identifier = "F"
          end
        elsif(col == 1 and row == 2)
          expected_number_objects = 3
          if(i == 1)
            expected_identifier = "A"
          elsif(i == 2)
            expected_identifier = "B"
          end
        elsif(col == 3 and row == 3)
          expected_number_objects = 2
          if(i == 1)
            expected_identifier = "D"
          end
        elsif(col == 2 and row == 4)
          expected_number_objects = 2
          if(i == 1)
            expected_identifier = "C"
          end
        end
      
        assert_equal(expected_identifier, minds_eye_coord_content[i].getIdentifier(), "occurred when checking identifier for object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s)
      end
      
      assert_equal(expected_number_objects, minds_eye_coord_content.size, "occurred when checking the number of objects present on coordinates " + col.to_s + ", " + row.to_s)
    end
  end
end

################################################################################
# Simple test for MindsEye.getRecognisedObjectLifespan() method
unit_test "get-recognised-object-lifespan" do 
  scene = Scene.new("Test scene", 1, 1)

  model = Chrest.new
  model.setDomain(GenericDomain.new(model))
  
  for i in 0...50
    recognised_object_lifespan = rand(10000..50000)
    minds_eye = MindsEye.new(model, scene, 50, 0, 100, 250, recognised_object_lifespan, 5000, 8, 0)
    assert_equal(recognised_object_lifespan, minds_eye.getRecognisedObjectLifespan())
  end
end

################################################################################
# Simple test for MindsEye.getSceneTransposed() method
unit_test "get-scene-transposed" do 
  scene_one = Scene.new("Test scene one", 1, 1)
  scene_two = Scene.new("Test scene two", 2, 2)

  model = Chrest.new
  model.setDomain(GenericDomain.new(model))

  model.createNewMindsEye(scene_two, 50, 0, 100, 250, 10000, 5000, 8, 0)
  model.createNewMindsEye(scene_one, 50, 0, 100, 250, 10000, 5000, 8, model.getAttentionClock)
  minds_eyes = model.getMindsEyes()
  
  assert_equal(scene_two, minds_eyes.firstEntry.getValue.getSceneTransposed(), "occurred when checking the scene associated with the first mind's eye created")
  assert_equal(scene_one, minds_eyes.lastEntry.getValue.getSceneTransposed(), "occurred when checking the scene associated with the second mind's eye created")
end

################################################################################
# Simple test for MindsEye.getUnrecognisedObjectLifespan() method
unit_test "get-recognised-object-lifespan" do 
  scene = Scene.new("Test scene", 1, 1)

  model = Chrest.new
  model.setDomain(GenericDomain.new(model))
  
  for i in 0...50
    unrecognised_object_lifespan = rand(500..1000)
    minds_eye = MindsEye.new(model, scene, 50, 0, 100, 250, 10000, unrecognised_object_lifespan, 8, 0)
    assert_equal(unrecognised_object_lifespan, minds_eye.getUnrecognisedObjectLifespan())
  end
end

################################################################################
# The scene used in the following test resembles a "cone" of vision i.e. the 
# further ahead the observer sees, the wider its field of vision.  A diagram of 
# this scene can be found below:
# "x" represents a "blind spot", single upper-case characters represent 
# distinct objects, the outer-most numbers represent the coordinates relative 
# to the creator while the inner-most numbers represent the minds eye x and y 
# coordinates. 
# 
#        -------------------------------
# 3    3 |     | C,D |     |     |  E  | 
#        -------------------------------
# 2    2    x  |     |  B  |     |  x
#              -------------------
# 1    1    x     x  |  A  |  x     x
#                    -------
# 0    0    x     x  |SELF |  x     x
#                    -------
#           0     1     2     3     4    MIND'S EYE COORDS
#          
#          -2    -1     0     1     2    CREATOR-RELATIVE COORDS
unit_test "get-visual-spatial-field-as-scene" do
  
  # Create the scene to be transposed into the mind's eye.
  scene = Scene.new("Test scene", 5, 4)
  scene.addItemToSquare(2, 0, Scene.getSelfIdentifier())
  scene.addItemToSquare(2, 1, "A")
  scene.addItemToSquare(1, 2, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(2, 2, "B")
  scene.addItemToSquare(3, 2, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(0, 3, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(1, 3, "C")
  scene.addItemToSquare(1, 3, "D")
  scene.addItemToSquare(2, 3, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(3, 3, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(4, 3, "E")
  
  ##################################
  ##### CREATE CHREST INSTANCE #####
  ##################################
  
  domain_time = 0
  time_to_encode_objects = 50
  time_to_encode_empty_squares = 5
  minds_eye_access_time = 100
  time_to_move_object = 250
  lifespan_for_recognised_objects = 10000
  lifespan_for_unrecognised_objects = 5000
  number_fixations = rand(1..9)
  
  model = Chrest.new
  model.setDomain(GenericDomain.new(model))
  
  ##################################
  ##### INSTANTIATE MIND'S EYE #####
  ##################################
  
  creation_time = domain_time
  
  minds_eye = MindsEye.new(
    model,
    scene, 
    time_to_encode_objects,
    time_to_encode_empty_squares,
    minds_eye_access_time, 
    time_to_move_object, 
    lifespan_for_recognised_objects,
    lifespan_for_unrecognised_objects,
    number_fixations,
    creation_time
  )

  #################
  ##### TESTS #####
  #################
  
  # Get the state of the visual-spatial field before any objects have been 
  # placed.  The scene returned should be completely blind.
  minds_eye_scene = minds_eye.getVisualSpatialFieldAsScene(creation_time + minds_eye_access_time)
  for row in 0...scene.getHeight()
    for col in 0...scene.getWidth()
      minds_eye_coord_content = minds_eye_scene.getItemsOnSquare(col, row, false, true)
      
      for i in 0...minds_eye_coord_content.size
        assert_equal(Scene.getBlindSquareIdentifier(), minds_eye_coord_content.getItem(i).getItem(), "occurred when checking the identifier for object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " when checking visual-spatial field contents before any objects should have been encoded")
      end
    end
  end
  
  # Get the state of the visual-spatial field after first 5 objects have been 
  # placed.
  minds_eye_scene = minds_eye.getVisualSpatialFieldAsScene(creation_time + minds_eye_access_time + (time_to_encode_objects * 5) + (time_to_encode_empty_squares * 3))
  for row in 0...scene.getHeight()
    for col in 0...scene.getWidth()
      minds_eye_coord_content = minds_eye_scene.getItemsOnSquare(col, row, false, true)
      
      expected_identifier = Scene.getBlindSquareIdentifier
      
      for i in 0...minds_eye_coord_content.size
        if(col == 2 and row == 0)
          expected_identifier = Scene.getSelfIdentifier
        elsif(col == 2 and row == 1)
          expected_identifier = "A"
        elsif(col == 1 and row == 2)
          expected_identifier = Scene.getEmptySquareIdentifier
        elsif(col == 2 and row == 2)
          expected_identifier = "B"
        elsif(col == 3 and row == 2)
          expected_identifier = Scene.getEmptySquareIdentifier
        elsif(col == 0 and row == 3)
          expected_identifier = Scene.getEmptySquareIdentifier
        elsif(col == 1 and row == 3)
          if(i == 0)
            expected_identifier = "C"
          elsif(i == 1)
            expected_identifier = "D"
          end
        end
        
        assert_equal(expected_identifier, minds_eye_coord_content.getItem(i).getItem(), "occurred when checking identifier for object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " when checking visual-spatial field contents after placing 5 objects")
      end
    end
  end
  
  # Get the state of the visual-spatial field after all objects have been 
  # placed and before any objects have decayed.  The scene returned should match 
  # the scene transposed exactly.
  minds_eye_scene = minds_eye.getVisualSpatialFieldAsScene(model.getAttentionClock)
  for row in 0...scene.getHeight()
    for col in 0...scene.getWidth()
      minds_eye_coord_content = minds_eye_scene.getItemsOnSquare(col, row, false, true)
      
      expected_identifier = Scene.getBlindSquareIdentifier
      
      for i in 0...minds_eye_coord_content.size
        if(col == 2 and row == 0)
          expected_identifier = Scene.getSelfIdentifier
        elsif(col == 2 and row == 1)
          expected_identifier = "A"
        elsif(col == 1 and row == 2)
          expected_identifier = Scene.getEmptySquareIdentifier
        elsif(col == 2 and row == 2)
          expected_identifier = "B"
        elsif(col == 3 and row == 2)
          expected_identifier = Scene.getEmptySquareIdentifier
        elsif(col == 0 and row == 3)
          expected_identifier = Scene.getEmptySquareIdentifier
        elsif(col == 1 and row == 3)
          if(i == 0)
            expected_identifier = "C"
          elsif(i == 1)
            expected_identifier = "D"
          end
        elsif(col == 2 and row == 3)
          expected_identifier = Scene.getEmptySquareIdentifier
        elsif(col == 3 and row == 3)
          expected_identifier = Scene.getEmptySquareIdentifier
        elsif(col == 4 and row == 3)
          expected_identifier = "E"
        end
      
        assert_equal(expected_identifier, minds_eye_coord_content.getItem(i).getItem(), "occurred when checking identifier for object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " when checking visual-spatial field contents after all objects have been encoded and before any should have decayed")
      end
    end
  end
  
  # Get the state of the visual-spatial field after all objects have decayed.
  # The scene returned should have empty squares for all non-permanently blind
  # squares.
  minds_eye_scene = minds_eye.getVisualSpatialFieldAsScene(model.getAttentionClock + lifespan_for_unrecognised_objects)
  for row in 0...scene.getHeight()
    for col in 0...scene.getWidth()
      minds_eye_coord_content = minds_eye_scene.getItemsOnSquare(col, row, false, true)
      
      expected_identifier = Scene.getEmptySquareIdentifier
      
      for i in 0...minds_eye_coord_content.size
        
        if(
          (col == 0 and row == 0) or (col == 1 and row == 0) or (col == 3 and row == 0) or (col == 4 and row == 0) or
          (col == 0 and row == 1) or (col == 1 and row == 1) or (col == 3 and row == 1) or (col == 4 and row == 1) or
          (col == 0 and row == 2) or (col == 4 and row == 2)
        )
          expected_identifier = Scene.getBlindSquareIdentifier
        end
        assert_equal(expected_identifier, minds_eye_coord_content.getItem(i).getItem(), "occurred when checking identifier for object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " when checking visual-spatial field contents after all objects have been encoded and decayed")
      end
    end
  end
end

################################################################################
# Tests here check correct operation when moving objects in mind's eye using 
# three scenarios:
# 1) Moving an object from a square to another that contains another object that
#    is "alive" when the move occurs.
# 2) Moving an object from a square that contains another object to a blind 
#    square.
# 3) Moving an object from a square to another that contains another object that
#    is "dead" when the move occurs.
unit_test "move_object" do
  
  # Set the objects that will be used.
  test_objects = ["A", "B", "C", "D"]
  
  # Create the scene to be transposed into the mind's eye.
  scene = Scene.new("Test scene", 3, 2)
  scene.addItemToSquare(1, 0, test_objects[0])
  scene.addItemToSquare(0, 1, test_objects[2])
  scene.addItemToSquare(1, 1, test_objects[1])
  scene.addItemToSquare(2, 1, Scene.getEmptySquareIdentifier())
  
  # Create a new CHREST instance and set its domain (important to enable 
  # perceptual mechanisms).
  model = Chrest.new
  model.setDomain(GenericDomain.new(model))
  
  # Set independent variables.
  creation_time = 0
  number_fixations = 2
  time_to_encode_objects = 50
  time_to_encode_empty_squares = 0
  minds_eye_access_time = 100
  time_to_move_object = 250
  lifespan_for_recognised_objects = 60000
  lifespan_for_unrecognised_objects = 30000
  
  # Create the minds eye
  minds_eye = MindsEye.new(
    model,
    scene, 
    time_to_encode_objects,
    time_to_encode_empty_squares,
    minds_eye_access_time, 
    time_to_move_object, 
    lifespan_for_recognised_objects,
    lifespan_for_unrecognised_objects,
    number_fixations,
    creation_time
  )
  
  ######################
  ##### FIRST MOVE #####
  ######################
  
  object_a_single_legal_move = ArrayList.new
  object_a_single_legal_move.add(ItemSquarePattern.new(test_objects[0], 1, 0))
  object_a_single_legal_move.add(ItemSquarePattern.new(test_objects[0], 1, 1))
  legal_single_object_move = ArrayList.new
  legal_single_object_move.add(object_a_single_legal_move)
  time_first_move_requested = model.getAttentionClock()
  minds_eye.moveObjects(legal_single_object_move, time_first_move_requested)
  
  for row in 0...minds_eye.getSceneTransposed.getHeight()
    for col in 0...minds_eye.getSceneTransposed.getWidth()
      objects = minds_eye.getObjectsOnVisualSpatialSquare(col, row)
      for i in 0...objects.size()
        object = objects[i]
        
        expected_identifier = Scene.getBlindSquareIdentifier()
        expected_creation_time = creation_time + minds_eye_access_time
        expected_terminus = nil
        expected_recognised = false
        
        # Col 0 and row 0 is blind so no expected values need to be overwritten.
        if(col == 1 and row == 0)
          if(i == 0)
            expected_terminus = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 1, 0)
          elsif(i == 1)
            # Object A's original location.  The terminus for object A here 
            # should be different to normal since it is being moved from this
            # square so should no longer exist on these coordinates from the 
            # time specified.
            expected_identifier = test_objects[0]
            expected_creation_time = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 1, 0)
            expected_terminus = time_first_move_requested + minds_eye_access_time
          end
        # Col 2 and row 0 is blind so no expected values need to be overwritten.
        elsif(col == 0 and row == 1)
          if(i == 0)
            expected_terminus = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 2, 0)
          elsif(i == 1)
            expected_identifier = test_objects[2]
            expected_creation_time = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 2, 0)
            expected_terminus = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 2, 0, lifespan_for_unrecognised_objects)
          end
        elsif(col == 1 and row == 1)
          if(i == 0)
            expected_terminus = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 0)
          elsif(i == 1)
            # Object B - the terminus of object B should be extended from its
            # original value since A has been moved onto its location therefore
            # it has been "looked at" and refreshed.
            expected_identifier = test_objects[1]
            expected_creation_time = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 0)
            expected_terminus = time_first_move_requested + minds_eye_access_time + time_to_move_object + lifespan_for_unrecognised_objects
          elsif(i == 2)
            # Object A's new location.
            expected_identifier = test_objects[0]
            expected_creation_time = time_first_move_requested + minds_eye_access_time + time_to_move_object
            expected_terminus = time_first_move_requested + minds_eye_access_time + time_to_move_object + lifespan_for_unrecognised_objects
          end
        elsif(col == 2 and row == 1)
          if(i == 0)
            expected_terminus = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 1)
          end
        end
        
        assert_equal(expected_identifier, object.getIdentifier, "occurred when checking identifier of object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " of the mind's eye after first move.")
        assert_equal(expected_creation_time, object.getTimeCreated, "occurred when checking creation time of object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " of the mind's eye after first move.")
        assert_equal(expected_terminus, object.getTerminus, "occurred when checking terminus of object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " of the mind's eye after first move.")
        assert_equal(expected_recognised, object.recognised(model.getAttentionClock()), "occurred when checking recognised status of object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " of the mind's eye after first move.")
      end
    end
  end
  assert_equal(time_first_move_requested + minds_eye_access_time + time_to_move_object, model.getAttentionClock(), "occurred when checking the time that the CHREST model associated with the mind's eye will be free after first move.")
  
  #######################
  ##### SECOND MOVE #####
  #######################
  
  object_a_move_to_blind_square = ArrayList.new
  object_a_move_to_blind_square.add(ItemSquarePattern.new(test_objects[0], 1, 1))
  object_a_move_to_blind_square.add(ItemSquarePattern.new(test_objects[0], 2, 0))
  moves = ArrayList.new
  moves.add(object_a_move_to_blind_square)
  time_second_move_requested = model.getAttentionClock()
  minds_eye.moveObjects(moves, model.getAttentionClock())
  
  for row in 0...minds_eye.getSceneTransposed.getHeight()
    for col in 0...minds_eye.getSceneTransposed.getWidth()
      objects = minds_eye.getObjectsOnVisualSpatialSquare(col, row)
      for i in 0...objects.size()
        object = objects[i]
        
        expected_identifier = Scene.getBlindSquareIdentifier()
        expected_creation_time = creation_time + minds_eye_access_time
        expected_terminus = nil
        expected_recognised = false
        
        # Col 0 and row 0 is blind so no expected values need to be overwritten.
        if(col == 1 and row == 0)
          if(i == 0)
            expected_terminus = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 1, 0)
          elsif(i == 1)
            expected_identifier = test_objects[0]
            expected_creation_time = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 1, 0)
            expected_terminus = time_first_move_requested + minds_eye_access_time
          end
        # Col 2 and row 0 is blind so no expected values need to be overwritten.
        # Object A is moved here but again, since the square is blind, nothing
        # changes.
        elsif(col == 0 and row == 1)
          if(i == 0)
            expected_terminus = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 2, 0)
          elsif(i == 1)
            expected_identifier = test_objects[2]
            expected_creation_time = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 2, 0)
            expected_terminus = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 2, 0, lifespan_for_unrecognised_objects)
          end
        elsif(col == 1 and row == 1)
          if(i == 0)
            expected_terminus = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 0)
          elsif(i == 1)
            # Since object A is being moved, B's terminus should be updated.
            expected_identifier = test_objects[1]
            expected_creation_time = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 0)
            expected_terminus = time_second_move_requested + minds_eye_access_time + lifespan_for_unrecognised_objects
          elsif(i == 2)
            # Object A is now being moved from this square so its terminus will
            # be different to previous.
            expected_identifier = test_objects[0]
            expected_creation_time = time_first_move_requested + minds_eye_access_time + time_to_move_object
            expected_terminus = time_second_move_requested + minds_eye_access_time
          end
        elsif(col == 2 and row == 1)
          if(i == 0)
            expected_terminus = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 1)
          end
        end
        
        assert_equal(expected_identifier, object.getIdentifier, "occurred when checking identifier of object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " of the mind's eye after second move.")
        assert_equal(expected_creation_time, object.getTimeCreated, "occurred when checking creation time of object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " of the mind's eye after second move.")
        assert_equal(expected_terminus, object.getTerminus, "occurred when checking terminus of object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " of the mind's eye after second move.")
        assert_equal(expected_recognised, object.recognised(model.getAttentionClock()), "occurred when checking recognised status of object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " of the mind's eye after second move.")
      end
    end
  end
  assert_equal(time_second_move_requested + minds_eye_access_time + time_to_move_object, model.getAttentionClock(), "occurred when checking the time that the CHREST model associated with the mind's eye will be free after second move.")
  
  ######################
  ##### THIRD MOVE #####
  ######################
  
  #Third move allows for checking of no update to terminus when object is moved
  #onto square occupied by other object that has "died".  In this test, object
  #B's terminus will have been updated when object A was moved onto its square
  #in the first move.  Since the move occurred after object C was created, 
  #object B's terminus will now be greater than that of C's so move B from 1, 1
  #onto 0, 1 (where C is) at the time of C's terminus.
  object_b_move = ArrayList.new
  object_b_move.add(ItemSquarePattern.new(test_objects[1], 1, 1))
  object_b_move.add(ItemSquarePattern.new(test_objects[1], 0, 1))
  moves = ArrayList.new
  moves.add(object_b_move)
  time_third_move_requested = minds_eye.getObjectsOnVisualSpatialSquare(0, 1).get(1).getTerminus
  minds_eye.moveObjects(moves, time_third_move_requested)

  for row in 0...minds_eye.getSceneTransposed.getHeight()
    for col in 0...minds_eye.getSceneTransposed.getWidth()
      objects = minds_eye.getObjectsOnVisualSpatialSquare(col, row)
      for i in 0...objects.size()
        object = objects[i]
        
        expected_identifier = Scene.getBlindSquareIdentifier()
        expected_creation_time = creation_time + minds_eye_access_time
        expected_terminus = nil
        expected_recognised = false
        
        # Col 0 and row 0 is blind so no expected values need to be overwritten.
        if(col == 1 and row == 0)
          if(i == 0)
            expected_terminus = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 1, 0)
          elsif(i == 1)
            expected_identifier = test_objects[0]
            expected_creation_time = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 1, 0)
            expected_terminus = time_first_move_requested + minds_eye_access_time
          end
        #Col 2 and row 0 is blind so no expected values need to be overwritten.
        elsif(col == 0 and row == 1)
          if(i == 0)
            expected_terminus = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 2, 0)
          elsif(i == 1)
            # Object B is moved onto the same square as object C but C is "dead"
            # so no values here are different to what they were previously.
            expected_identifier = test_objects[2]
            expected_creation_time = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 2, 0)
            expected_terminus = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 2, 0, lifespan_for_unrecognised_objects)
          elsif(i == 2)
            # Object B now present here so calculate values accordingly.
            expected_identifier = test_objects[1]
            expected_creation_time = time_third_move_requested + minds_eye_access_time + time_to_move_object
            expected_terminus = time_third_move_requested + minds_eye_access_time + time_to_move_object + lifespan_for_unrecognised_objects
          end
        elsif(col == 1 and row == 1)
          if(i == 0)
            expected_terminus = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 0)
          elsif(i == 1)
            # Object B is being moved so its terminus should be different to 
            # previous.
            expected_identifier = test_objects[1]
            expected_creation_time = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 0)
            expected_terminus = time_third_move_requested + minds_eye_access_time
          elsif(i == 2)
            expected_identifier = test_objects[0]
            expected_creation_time = time_first_move_requested + minds_eye_access_time + time_to_move_object
            expected_terminus = time_second_move_requested + minds_eye_access_time
          end
        elsif(col == 2 and row == 1)
          if(i == 0)
            expected_terminus = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 1)
          end
        end
        
        assert_equal(expected_identifier, object.getIdentifier, "occurred when checking identifier of object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " of the mind's eye after third move.")
        assert_equal(expected_creation_time, object.getTimeCreated, "occurred when checking creation time of object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " of the mind's eye after third move.")
        assert_equal(expected_terminus, object.getTerminus, "occurred when checking terminus of object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " of the mind's eye after third move.")
        assert_equal(expected_recognised, object.recognised(model.getAttentionClock()), "occurred when checking recognised status of object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " of the mind's eye after third move.")
      end
    end
  end
  assert_equal(time_third_move_requested + minds_eye_access_time + time_to_move_object, model.getAttentionClock(), "occurred when checking the time that the CHREST model associated with the mind's eye will be free after third move.")
end

################################################################################
# Tests for correct behaviour when illegal move requests are made.
# 1) Request a move that is legal but before attention is free after mind's 
#    eye creation.
# 2) Request a move when the CHREST model associated with the mind's eye is 
#    free and the first object move is legal but the initial location for the 
#    second object is incorrect.
# 3) Request a move when the CHREST model associated with the mind's eye is 
#    free and the first object move is legal but only the initial location for 
#    the second object move is specified.
# 4) Request a move when the CHREST model associated with the mind's eye is 
#    free and the first object move is legal but object movement in the second
#    object move is not serial. 
unit_test "move_objects_illegally" do
  
  # Set the objects that will be used.
  test_objects = ["A", "B", "C", "D"]
  
  # Create the scene to be transposed into the mind's eye.
  scene = Scene.new("Test scene", 3, 2)
  scene.addItemToSquare(1, 0, test_objects[0])
  scene.addItemToSquare(0, 1, test_objects[2])
  scene.addItemToSquare(1, 1, test_objects[1])
  scene.addItemToSquare(2, 1, Scene.getEmptySquareIdentifier())
  
  # Create a new CHREST instance and set its domain (important to enable 
  # perceptual mechanisms).
  model = Chrest.new
  model.setDomain(GenericDomain.new(model))
  
  # Set independent variables.
  creation_time = 0
  number_fixations = 2
  time_to_encode_objects = 50
  time_to_encode_empty_squares = 0
  minds_eye_access_time = 100
  time_to_move_object = 250
  lifespan_for_recognised_objects = 60000
  lifespan_for_unrecognised_objects = 30000
  
  # Create the minds eye
  minds_eye = MindsEye.new(
    model,
    scene, 
    time_to_encode_objects,
    time_to_encode_empty_squares,
    minds_eye_access_time, 
    time_to_move_object, 
    lifespan_for_recognised_objects,
    lifespan_for_unrecognised_objects,
    number_fixations,
    creation_time
  )
  
  object_a_legal_move = ArrayList.new
  object_a_legal_move.add(ItemSquarePattern.new(test_objects[0], 1, 0))
  object_a_legal_move.add(ItemSquarePattern.new(test_objects[0], 1, 1))
  moves = ArrayList.new
  moves.add(object_a_legal_move)
  minds_eye.moveObjects(moves, model.getAttentionClock() - 1)

  begin
    object_b_incorrect_initial_location = ArrayList.new
    object_b_incorrect_initial_location.add(ItemSquarePattern.new(test_objects[1], 1, 0))
    object_b_incorrect_initial_location.add(ItemSquarePattern.new(test_objects[1], 2, 1))
    moves = ArrayList.new
    moves.add(object_a_legal_move)
    moves.add(object_b_incorrect_initial_location)
    minds_eye.moveObjects(moves, model.getAttentionClock())
  rescue # Swallow the exception thrown to keep test output "pretty".
  end

  begin
    object_b_initial_location_only = ArrayList.new
    object_b_initial_location_only.add(ItemSquarePattern.new(test_objects[1], 1, 1))
    moves = ArrayList.new
    moves.add(object_a_legal_move)
    moves.add(object_b_initial_location_only)
    minds_eye.moveObjects(moves, model.getAttentionClock())
  rescue # Swallow the exception thrown to keep test output "pretty".
  end
  
  begin
    object_b_non_serial = ArrayList.new
    object_b_non_serial.add(ItemSquarePattern.new(test_objects[1], 1, 1))
    object_b_non_serial.add(ItemSquarePattern.new(test_objects[1], 2, 1))
    object_b_non_serial.add(ItemSquarePattern.new(test_objects[2], 0, 1))
    moves = ArrayList.new
    moves.add(object_a_legal_move)
    moves.add(object_b_non_serial)
    minds_eye.moveObjects(moves, model.getAttentionClock())
  rescue # Swallow the exception thrown to keep test output "pretty".
  end
  
  for row in 0...minds_eye.getSceneTransposed.getHeight()
    for col in 0...minds_eye.getSceneTransposed.getWidth()
      objects = minds_eye.getObjectsOnVisualSpatialSquare(col, row)
      for i in 0...objects.size()
        object = objects[i]
        
        expected_identifier = Scene.getBlindSquareIdentifier()
        expected_creation_time = creation_time + minds_eye_access_time
        expected_terminus = nil
        expected_recognised = false
        
        # Col 0 and row 0 is blind so no expected values need to be overwritten.
        if(col == 1 and row == 0)
          if(i == 0)
            expected_terminus = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 1, 0)
          elsif(i == 1)
            # Object A's original location.  The terminus for object A here 
            # should not be different to incorrect_normal since the move is requested 
            # before the attention of the CHREST model associated with this 
            # mind's eye is free.
            expected_identifier = test_objects[0]
            expected_creation_time = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 1, 0)
            expected_terminus = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 1, 0, lifespan_for_unrecognised_objects)
          end
        # Col 2 and row 0 is blind so no expected values need to be overwritten.
        elsif(col == 0 and row == 1)
          if(i == 0)
            expected_terminus = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 2, 0)
          elsif(i == 1)
            expected_identifier = test_objects[2]
            expected_creation_time = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 2, 0)
            expected_terminus = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 2, 0, lifespan_for_unrecognised_objects)
          end
        elsif(col == 1 and row == 1)
          if(i == 0)
            expected_terminus = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 0)
          elsif(i == 1)
            expected_identifier = test_objects[1]
            expected_creation_time = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 0)
            expected_terminus = get_terminus_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 0, lifespan_for_unrecognised_objects)
          end
        elsif(col == 2 and row == 1)
          if(i == 0)
            expected_terminus = get_creation_time_for_object_after_minds_eye_creation(creation_time, minds_eye_access_time, time_to_encode_objects, time_to_encode_empty_squares, 3, 1)
          end
        end
        
        assert_equal(expected_identifier, object.getIdentifier, "occurred when checking identifier of object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " of the mind's eye.")
        assert_equal(expected_creation_time, object.getTimeCreated, "occurred when checking creation time of object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " of the mind's eye.")
        assert_equal(expected_terminus, object.getTerminus, "occurred when checking terminus of object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " of the mind's eye.")
        assert_equal(expected_recognised, object.recognised(model.getAttentionClock()), "occurred when checking recognised status of object " + (i+1).to_s + " on coordinates " + col.to_s + ", " + row.to_s + " of the mind's eye.")
      end
    end
  end
  assert_equal(creation_time + minds_eye_access_time + (time_to_encode_objects * 3) + time_to_encode_empty_squares, model.getAttentionClock(), "occurred when checking the time that the CHREST model associated with the mind's eye.")
end