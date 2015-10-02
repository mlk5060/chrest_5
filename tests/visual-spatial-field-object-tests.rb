#def create_visual_spatial_field_objects(number_objects_to_make) 
#  domain_time = 0
#  number_fixations = 2
#  time_to_encode_objects = 50
#  time_to_encode_empty_squares = 0
#  visual_spatial_field_access_time = 100
#  time_to_move_object = 250
#  lifespan_for_recognised_objects = 60000
#  lifespan_for_unrecognised_objects = 30000
#
#  # Create the visual_spatial_field
#  visual_spatial_field = VisualSpatialField.new(
#    Chrest.new,
#    Scene.new("test", 1, 1), 
#    time_to_encode_objects,
#    time_to_encode_empty_squares,
#    visual_spatial_field_access_time, 
#    time_to_move_object, 
#    lifespan_for_recognised_objects,
#    lifespan_for_unrecognised_objects,
#    number_fixations,
#    domain_time,
#    false
#  )
#  
#  visual_spatial_field_objects = []
#
#  for i in 0...number_objects_to_make
#    visual_spatial_field_objects.push(VisualSpatialFieldObject.new(visual_spatial_field, i.to_s, i.to_s, domain_time, true))
#  end
#  
#  visual_spatial_field_objects
#end
#
#################################################################################
## Checks that:
## 
## 1) If a VisualSpatialFieldObject encoding a blind square is created:
##   a) Its identifier is set to the result of Scene.getBlindSquareIdentifier().
##   b) Its object class is set to the result of Scene.getBlindSquareIdentifier().
##   c) Its terminus is not set.
## 2) If a VisualSpatialFieldObject encoding an empty square is created:
##   a) Its identifier is set to the result of Scene.getEmptySquareIdentifier().
##   b) Its object class is set to the result of Scene.getEmptySquareIdentifier().
##   c) Its terminus is set to the current time plus thelifespan for an 
##      unrecognised object (will not be recognised since CHREST will not commit
##      any info to LTM).
## 3) If a VisualSpatialFieldObject encoding a non blind/empty square is created:
##   a) Its identifier is set to what is expected.
##   b) Its object class is set to what is expected.
##   c) Its terminus is set to the current time plus thelifespan for an 
##      unrecognised object (will not be recognised since CHREST will not commit
##      any info to LTM).
#unit_test "constructor" do
#  visual_spatial_field_objects = []
#  time_created = 0
#  
#  object_identifier = "0"
#  object_class = "P"
#  
#  #Create a visual_spatial_field so that its object reference can be used in 
#  #the VisualSpatialFieldObject constructor.
#  unrec_obj_life = 30000
#  visual_spatial_field = VisualSpatialField.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, unrec_obj_life, 2, 0, false)
#  
#  #Create the VisualSpatialFieldObject instances that will be tested.
#  visual_spatial_field_objects.push(VisualSpatialFieldObject.new(visual_spatial_field, "willBeOverwritten", Scene.getBlindSquareIdentifier, time_created, false))
#  visual_spatial_field_objects.push(VisualSpatialFieldObject.new(visual_spatial_field, "willBeOverwritten", Scene.getEmptySquareIdentifier, time_created, false))
#  visual_spatial_field_objects.push(VisualSpatialFieldObject.new(visual_spatial_field, object_identifier, object_class, time_created, false))
#  visual_spatial_field_objects.push(VisualSpatialFieldObject.new(visual_spatial_field, "willBeOverwritten", Scene.getBlindSquareIdentifier, time_created, true))
#  visual_spatial_field_objects.push(VisualSpatialFieldObject.new(visual_spatial_field, "willBeOverwritten", Scene.getEmptySquareIdentifier, time_created, true))
#  visual_spatial_field_objects.push(VisualSpatialFieldObject.new(visual_spatial_field, object_identifier, object_class, time_created, true))
#  
#  for i in 0...visual_spatial_field_objects.count
#    visual_spatial_field_object = visual_spatial_field_objects[i]
#    
#    expected_identifier = Scene.getBlindSquareIdentifier()
#    expected_class = Scene.getBlindSquareIdentifier()
#    expected_terminus = nil
#    
#    if i == 1 or i == 4
#      expected_identifier = Scene.getEmptySquareIdentifier()
#      expected_class = Scene.getEmptySquareIdentifier()
#    end
#    
#    if i == 2 or i == 5
#      expected_identifier = object_identifier
#      expected_class = object_class
#    end
#    
#    if i == 4 or i == 5
#      expected_terminus = time_created + unrec_obj_life 
#    end
#    
#    assert_equal(visual_spatial_field, visual_spatial_field_object.getAssociatedVisualSpatialField(), "occurred when checking visual-spatial field associated with visual-spatial field object " + i.to_s)
#    assert_equal(expected_identifier, visual_spatial_field_object.getIdentifier(), "occurred when checking the identifier for visual-spatial field object " + i.to_s)
#    assert_equal(expected_class, visual_spatial_field_object.getObjectClass(), "occurred when checking the class for visual-spatial field object " + i.to_s)
#    assert_equal(time_created, visual_spatial_field_object.getTimeCreated, "occurred when checking the time created for visual-spatial field object " + i.to_s)
#    assert_equal(expected_terminus, visual_spatial_field_object.getTerminus, "occurred when checking the terminus for visual-spatial field object " + i.to_s)
#  end
#end
#
#################################################################################
#unit_test "alive" do
#  unrec_obj_life = 30000
#  visual_spatial_field = VisualSpatialField.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, unrec_obj_life, 2, 0, false)
#  
#  creation_time = 100
#  blind_object = VisualSpatialFieldObject.new(visual_spatial_field, "", Scene.getBlindSquareIdentifier, creation_time, true)
#  non_blind_object = VisualSpatialFieldObject.new(visual_spatial_field, "0", "O", creation_time, true)
#  
#  assert_true(blind_object.alive(creation_time + unrec_obj_life + 1), "occurred when checking if blind object is alive")
#  assert_true(non_blind_object.alive(rand(creation_time...unrec_obj_life)), "occurred when checking if non-blind object is alive")
#  
#  assert_false(blind_object.alive(creation_time - 1), "occurred when checking if blind object is alive")
#  assert_false(non_blind_object.alive(creation_time - 1), "occurred when checking if non-blind object is alive by specifying a time before its creation")
#  assert_false(non_blind_object.alive(creation_time + unrec_obj_life), "occurred when checking if non-blind object is alive by specifying a time after its terminus")
#end
#
#################################################################################
unit_test "create_clone" do
  unrec_obj_life = 30000
  
  visual_spatial_field = VisualSpatialField.new(
    Chrest.new, 
    Scene.new("test", 1, 1, nil), 
    50, 
    0, 
    100, 
    250, 
    60000, 
    unrec_obj_life, 
    2, 
    0, 
    false,
    false
  )
  
  creation_time = 100
  
  # Blind object is used since its terminus will be equal to null so we can
  # check if the terminus is copied correctly.
  blind_object = VisualSpatialFieldObject.new(visual_spatial_field, "", Scene.getBlindSquareIdentifier, creation_time, true, false)
  non_blind_object = VisualSpatialFieldObject.new(visual_spatial_field, "0", "G", creation_time, true, false)
  
  time = creation_time + 1
  until time = creation_time + 5000
    
    if time % 2 == 0
      non_blind_object.setRecognised(time, true)
      blind_object.setRecognised(time, false)
    else
      non_blind_object.setUnrecognised(time, true)
      blind_object.setUnrecognised(time, false)
    end
    
    time += 1
  end
  
  non_blind_object_clone = non_blind_object.createClone()
  blind_object_clone = blind_object.createClone()
  
  assert_equal(blind_object.getIdentifier(), blind_object_clone.getIdentifier(), "occurred when checking identifier for blind object clone")
  assert_equal(blind_object.getObjectClass(), blind_object_clone.getObjectClass(), "occurred when checking object class for blind object clone")
  assert_equal(blind_object.getAssociatedVisualSpatialField(), blind_object_clone.getAssociatedVisualSpatialField(), "occurred when checking associated visual-spatial field for blind object clone")
  assert_equal(blind_object.getTimeCreated(), blind_object_clone.getTimeCreated(), "occurred when checking creation time for blind object clone")
  assert_equal(blind_object.getTerminus(), blind_object_clone.getTerminus(), "occurred when checking terminus for blind object clone")
  
  assert_equal(non_blind_object.getIdentifier(), non_blind_object_clone.getIdentifier(), "occurred when checking identifier for non-blind object clone")
  assert_equal(non_blind_object.getObjectClass(), non_blind_object_clone.getObjectClass(), "occurred when checking object class for non-blind object clone")
  assert_equal(non_blind_object.getAssociatedVisualSpatialField(), non_blind_object_clone.getAssociatedVisualSpatialField(),  "occurred when checking associated visual-spatial field for non-blind object clone")
  assert_equal(non_blind_object.getTimeCreated(), non_blind_object_clone.getTimeCreated(), "occurred when checking creation time for non-blind object clone")
  assert_equal(non_blind_object.getTerminus(), non_blind_object_clone.getTerminus(), "occurred when checking terminus for non-blind object clone")
  
  for i in (creation_time + 1)..time
    assert_equal(non_blind_object.recognised(i), non_blind_object_clone.recognised(i), "occurred when checking recognised status of non-blind object clone")
  end
end
#
#################################################################################
#unit_test "get_associated_visual_spatial_field" do
#  visual_spatial_field = VisualSpatialField.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, 30000, 2, 0, false)
#  object = VisualSpatialFieldObject.new(visual_spatial_field, "0", "G", 0, true)
#  assert_equal(visual_spatial_field, object.getAssociatedVisualSpatialField())
#end
#
#################################################################################
#unit_test "get_identifier" do
#  visual_spatial_field = VisualSpatialField.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, 30000, 2, 0, false)
#  
#  id = "0"
#  object = VisualSpatialFieldObject.new(visual_spatial_field, id, "G", 0, true)
#  
#  assert_equal(id, object.getIdentifier())
#end
#
#################################################################################
#unit_test "get_object_class" do
#  visual_spatial_field = VisualSpatialField.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, 30000, 2, 0, false)
#  
#  object_class = "G"
#  object = VisualSpatialFieldObject.new(visual_spatial_field, "0", object_class, 0, true)
#  
#  assert_equal(object_class, object.getObjectClass())
#end
#
#################################################################################
#unit_test "get_terminus" do
#  unrec_obj_life = 30000
#  visual_spatial_field = VisualSpatialField.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, unrec_obj_life, 2, 0, false)
#  
#  creation_time = 0
#  blind_object = VisualSpatialFieldObject.new(visual_spatial_field, "", Scene.getBlindSquareIdentifier(), creation_time, true)
#  object = VisualSpatialFieldObject.new(visual_spatial_field, "0", "G", creation_time, true)
#  assert_equal(nil, blind_object.getTerminus(), "occurred when checking terminus for blind object")
#  assert_equal(creation_time + unrec_obj_life, object.getTerminus(), "occurred when checking terminus for non-blind object")
#end
#
#################################################################################
#unit_test "get_time_created" do
#  visual_spatial_field = VisualSpatialField.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, 30000, 2, 0, false)
#
#  creation_time = 7000
#  object = VisualSpatialFieldObject.new(visual_spatial_field, "0", "G", creation_time, true)
#  assert_equal(creation_time, object.getTimeCreated())
#end
#
#################################################################################
#unit_test "recognised" do
#  visual_spatial_field_objects = create_visual_spatial_field_objects(1)
#  object = visual_spatial_field_objects[0]
#  object.setRecognised(1000)
#  object.setUnrecognised(2000, true)
#  assert_false(object.recognised(500), "occurred when checking the recognised status of an object at time 500")
#  assert_true(object.recognised(1999), "occurred when checking the recognised status of an object at time 1999")
#  assert_false(object.recognised(2400), "occurred when checking the recognised status of an object at time 2400")
#end
#
#################################################################################
#unit_test "set_recognised" do
#  unrec_obj_life = 30000
#  visual_spatial_field = VisualSpatialField.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, unrec_obj_life, 2, 0, false)
#  
#  creation_time = 7000
#  object = VisualSpatialFieldObject.new(visual_spatial_field, "0", "G", creation_time, true)
#  
#  alive_time = rand(creation_time + 1...(creation_time + unrec_obj_life))
#  after_death = creation_time + unrec_obj_life
#  
#  object.setRecognised(after_death)
#  assert_false(object.recognised(after_death + 100), "occurred when checking the recognised status of the object after setting it to a time after the object's death")
#  
#  object.setRecognised(alive_time)
#  assert_true(object.recognised(rand(alive_time...after_death)), "occurred when checking the recognised status of the object after setting it to a time before the object's death and after its birth")
#end
#
#################################################################################
#unit_test "set_terminus" do
#  unrec_obj_life = 30000
#  visual_spatial_field = VisualSpatialField.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, unrec_obj_life, 2, 0, false)
#  
#  creation_time = 7000
#  blind_object = VisualSpatialFieldObject.new(visual_spatial_field, "", Scene.getBlindSquareIdentifier, creation_time, true)
#  non_blind_object_a = VisualSpatialFieldObject.new(visual_spatial_field, "0", "A", creation_time, true)
#  non_blind_object_b = VisualSpatialFieldObject.new(visual_spatial_field, "1", "B", creation_time, true)
#  
#  # Should be set to time specified even though the second parameter is false
#  # because the object is a blind square.
#  blind_object_terminus = creation_time + 1000
#  blind_object.setTerminus(blind_object_terminus, false)
#  assert_equal(blind_object_terminus, blind_object.getTerminus, "occurred after attempting to set the terminus of a blind square object")
#  
#  # Non blind-object should have its terminus set to the time specified if the
#  # second parameter is true.
#  non_blind_object_a_terminus = creation_time + unrec_obj_life
#  non_blind_object_a.setTerminus(non_blind_object_a_terminus, true)
#  assert_equal(non_blind_object_a_terminus, non_blind_object_a.getTerminus(), "occurred after attempting to set the terminus of a non-blind object and specifying that the terminus should be set to the value passed.")
#  
#  # Should not have terminus set to new value if decayed and second parameter
#  # false.
#  non_blind_object_b_terminus_base = creation_time + unrec_obj_life + 100
#  non_blind_object_b.setTerminus(non_blind_object_b_terminus_base, false)
#  assert_equal(creation_time + unrec_obj_life, non_blind_object_b.getTerminus, "occurred after attempting to set the terminus of a decayed non-blind object")
#  
#  # Should have terminus set to new value if alive and second parameter false
#  non_blind_object_b_terminus_base = (creation_time + unrec_obj_life) - 1
#  non_blind_object_b.setTerminus(non_blind_object_b_terminus_base, false)
#  assert_equal(non_blind_object_b_terminus_base + unrec_obj_life, non_blind_object_b.getTerminus, "occurred after attempting to set the terminus of a non-decayed non-blind object")
#end
#
#################################################################################
#unit_test "set_unrecognised" do
#  unrec_obj_life = 30000
#  rec_obj_life = 60000
#  visual_spatial_field = VisualSpatialField.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, rec_obj_life, unrec_obj_life, 2, 0, false)
#  
#  creation_time = 7000
#  recognised_from_time = creation_time + 1
#  object = VisualSpatialFieldObject.new(visual_spatial_field, "0", "G", creation_time, true)
#  object.setRecognised(recognised_from_time) # Object now recognised.
#  
#  alive_time = rand(recognised_from_time + 1...(recognised_from_time + rec_obj_life))
#  after_death = recognised_from_time + rec_obj_life
#  
#  object.setUnrecognised(after_death, true)
#  assert_true(object.recognised(after_death + 100), "occurred when checking the recognised status of the object after setting it to a time after the object's death")
#  
#  object.setUnrecognised(alive_time, true)
#  assert_false(object.recognised(rand(alive_time...after_death)), "occurred when checking the recognised status of the object after setting it to a time before the object's death and after its birth")
#end
#
