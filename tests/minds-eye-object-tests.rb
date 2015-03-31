def create_minds_eye_objects(number_objects_to_make) 
  domain_time = 0
  number_fixations = 2
  time_to_encode_objects = 50
  time_to_encode_empty_squares = 0
  minds_eye_access_time = 100
  time_to_move_object = 250
  lifespan_for_recognised_objects = 60000
  lifespan_for_unrecognised_objects = 30000

  # Create the minds eye
  minds_eye = MindsEye.new(
    Chrest.new,
    Scene.new("test", 1, 1), 
    time_to_encode_objects,
    time_to_encode_empty_squares,
    minds_eye_access_time, 
    time_to_move_object, 
    lifespan_for_recognised_objects,
    lifespan_for_unrecognised_objects,
    number_fixations,
    domain_time
  )
  
  minds_eye_objects = []

  for i in 0...number_objects_to_make
    minds_eye_objects.push(MindsEyeObject.new(minds_eye, i.to_s, domain_time))
  end
  
  minds_eye_objects
end

unit_test "constructor" do

  # Create a minds eye
  unrec_obj_life = 30000
  minds_eye = MindsEye.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, unrec_obj_life, 2, 0)
  
  objects = []
  time_created = 0
  non_blind_identifier = "P"
  objects.push(MindsEyeObject.new(minds_eye, Scene.getBlindSquareIdentifier, time_created))
  objects.push(MindsEyeObject.new(minds_eye, non_blind_identifier, time_created))
  
  for i in 0...objects.count
    object = objects[i]
    
    expected_identifier = Scene.getBlindSquareIdentifier
    expected_terminus = nil
    
    if(i == 1)
      expected_identifier = non_blind_identifier
      expected_terminus = time_created + unrec_obj_life
    end
    
    assert_equal(minds_eye, object.getAssociatedMindsEye(), "occurred when checking mind's eye associated with mind's eye object " + (i+1).to_s)
    assert_equal(expected_identifier, object.getIdentifier, "occurred when checking the identifier for mind's eye object " + (i+1).to_s)
    assert_equal(time_created, object.getTimeCreated, "occurred when checking the time created for mind's eye object " + (i+1).to_s)
    assert_equal(expected_terminus, object.getTerminus, "occurred when checking the terminus for mind's eye object " + (i+1).to_s)
  end
end

unit_test "alive" do
  unrec_obj_life = 30000
  minds_eye = MindsEye.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, unrec_obj_life, 2, 0)
  
  creation_time = 100
  blind_object = MindsEyeObject.new(minds_eye, Scene.getBlindSquareIdentifier, creation_time)
  non_blind_object = MindsEyeObject.new(minds_eye, "O", creation_time)
  
  assert_true(blind_object.alive(creation_time + 100000), "occurred when checking if blind object is alive")
  assert_true(non_blind_object.alive(rand(creation_time...unrec_obj_life)), "occurred when checking if non-blind object is alive")
  
  assert_false(blind_object.alive(creation_time - 1), "occurred when checking if blind object is alive")
  assert_false(non_blind_object.alive(creation_time - 1), "occurred when checking if non-blind object is alive by specifying a time before its creation")
  assert_false(non_blind_object.alive(creation_time + unrec_obj_life), "occurred when checking if non-blind object is alive by specifying a time after its terminus")
end

unit_test "create_clone" do
  unrec_obj_life = 30000
  minds_eye = MindsEye.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, unrec_obj_life, 2, 0)
  
  creation_time = 100
  
  # Blind object is used since its terminus will be equal to null so we can
  # check if the terminus is copied correctly.
  blind_object = MindsEyeObject.new(minds_eye, Scene.getBlindSquareIdentifier, creation_time)
  non_blind_object = MindsEyeObject.new(minds_eye, "G", creation_time)
  
  time = creation_time + 1
  for i in 0...rand(500...1000)
    time += 1
    non_blind_object.setRecognised(time)
  end
  
  blind_object_clone = blind_object.createClone()
  non_blind_object_clone = non_blind_object.createClone()
  
  assert_equal(blind_object.getIdentifier(), blind_object_clone.getIdentifier(), "occurred when checking identifier for blind object")
  assert_equal(blind_object.getAssociatedMindsEye(), blind_object_clone.getAssociatedMindsEye(), "occurred when checking associated minds eye for blind object")
  assert_equal(blind_object.getTimeCreated(), blind_object_clone.getTimeCreated(), "occurred when checking creation time for blind object")
  assert_equal(blind_object.getTerminus(), blind_object_clone.getTerminus(), "occurred when checking terminus for blind object")
  
  assert_equal(non_blind_object.getIdentifier(), non_blind_object_clone.getIdentifier(), "occurred when checking identifier for non-blind object")
  assert_equal(non_blind_object.getAssociatedMindsEye(), non_blind_object_clone.getAssociatedMindsEye(),  "occurred when checking associated minds eye for non-blind object")
  assert_equal(non_blind_object.getTimeCreated(), non_blind_object_clone.getTimeCreated(), "occurred when checking creation time for non-blind object")
  assert_equal(non_blind_object.getTerminus(), non_blind_object_clone.getTerminus(), "occurred when checking terminus for non-blind object")
  for i in (creation_time + 1)..time
    assert_equal(non_blind_object.recognised(i), non_blind_object_clone.recognised(i), "occurred when checking recognised status of non-blind object")
  end
end

unit_test "get_associated_minds_eye" do
  minds_eye = MindsEye.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, 30000, 2, 0)
  object = MindsEyeObject.new(minds_eye, "G", 0)
  assert_equal(minds_eye, object.getAssociatedMindsEye())
end

unit_test "get_identifier" do
  minds_eye = MindsEye.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, 30000, 2, 0)
  
  id = "G"
  object = MindsEyeObject.new(minds_eye, id, 0)
  assert_equal(id, object.getIdentifier())
end

unit_test "get_terminus" do
  unrec_obj_life = 30000
  minds_eye = MindsEye.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, unrec_obj_life, 2, 0)
  
  creation_time = 0
  blind_object = MindsEyeObject.new(minds_eye, Scene.getBlindSquareIdentifier(), creation_time)
  object = MindsEyeObject.new(minds_eye, "G", creation_time)
  assert_equal(nil, blind_object.getTerminus(), "occurred when checking terminus for blind object")
  assert_equal(creation_time + unrec_obj_life, object.getTerminus(), "occurred when checking terminus for non-blind object")
end

unit_test "get_time_created" do
  minds_eye = MindsEye.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, 30000, 2, 0)

  creation_time = 7000
  object = MindsEyeObject.new(minds_eye, "G", creation_time)
  assert_equal(creation_time, object.getTimeCreated())
end

unit_test "recognised" do
  minds_eye_objects = create_minds_eye_objects(1)
  object = minds_eye_objects[0]
  object.setRecognised(1000)
  object.setUnrecognised(2000)
  assert_false(object.recognised(500), "occurred when checking the recognised status of an object at time 500")
  assert_true(object.recognised(1999), "occurred when checking the recognised status of an object at time 1999")
  assert_false(object.recognised(2400), "occurred when checking the recognised status of an object at time 2400")
end

unit_test "set_recognised" do
  unrec_obj_life = 30000
  minds_eye = MindsEye.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, unrec_obj_life, 2, 0)
  
  creation_time = 7000
  object = MindsEyeObject.new(minds_eye, "G", creation_time)
  
  alive_time = rand(creation_time + 1...(creation_time + unrec_obj_life))
  after_death = creation_time + unrec_obj_life
  
  object.setRecognised(after_death)
  assert_false(object.recognised(after_death + 100), "occurred when checking the recognised status of the object after setting it to a time after the object's death")
  
  object.setRecognised(alive_time)
  assert_true(object.recognised(rand(alive_time...after_death)), "occurred when checking the recognised status of the object after setting it to a time before the object's death and after its birth")
end

unit_test "set_terminus" do
  unrec_obj_life = 30000
  minds_eye = MindsEye.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, 60000, unrec_obj_life, 2, 0)
  
  creation_time = 7000
  blind_object = MindsEyeObject.new(minds_eye, Scene.getBlindSquareIdentifier, creation_time)
  non_blind_object_a = MindsEyeObject.new(minds_eye, "A", creation_time)
  non_blind_object_b = MindsEyeObject.new(minds_eye, "B", creation_time)
  
  # Should be set to time specified even though the second parameter is false
  # because the object is a blind square.
  blind_object_terminus = creation_time + 1000
  blind_object.setTerminus(blind_object_terminus, false)
  assert_equal(blind_object_terminus, blind_object.getTerminus, "occurred after attempting to set the terminus of a blind square object")
  
  # Non blind-object should have its terminus set to the time specified if the
  # second parameter is true.
  non_blind_object_a_terminus = creation_time + unrec_obj_life + 8
  non_blind_object_a.setTerminus(non_blind_object_a_terminus, true)
  assert_equal(non_blind_object_a_terminus, non_blind_object_a.getTerminus(), "occurred after attempting to set the terminus of a non-blind object and specifying that the terminus should be set to the value passed.")
  
  # Should not have terminus set to new value if decayed and second parameter
  # false.
  non_blind_object_b_terminus_base = creation_time + unrec_obj_life + 100
  non_blind_object_b.setTerminus(non_blind_object_b_terminus_base, false)
  assert_equal(creation_time + unrec_obj_life, non_blind_object_b.getTerminus, "occurred after attempting to set the terminus of a decayed non-blind object")
  
  # Should have terminus set to new value if alive and second parameter false
  non_blind_object_b_terminus_base = (creation_time + unrec_obj_life) - 1
  non_blind_object_b.setTerminus(non_blind_object_b_terminus_base, false)
  assert_equal(non_blind_object_b_terminus_base + unrec_obj_life, non_blind_object_b.getTerminus, "occurred after attempting to set the terminus of a non-decayed non-blind object")
end

unit_test "set_unrecognised" do
  unrec_obj_life = 30000
  rec_obj_life = 60000
  minds_eye = MindsEye.new(Chrest.new, Scene.new("test", 1, 1), 50, 0, 100, 250, rec_obj_life, unrec_obj_life, 2, 0)
  
  creation_time = 7000
  recognised_from_time = creation_time + 1
  object = MindsEyeObject.new(minds_eye, "G", creation_time)
  object.setRecognised(recognised_from_time) # Object now recognised.
  
  alive_time = rand(recognised_from_time + 1...(recognised_from_time + rec_obj_life))
  after_death = recognised_from_time + rec_obj_life
  
  object.setUnrecognised(after_death)
  assert_true(object.recognised(after_death + 100), "occurred when checking the recognised status of the object after setting it to a time after the object's death")
  
  object.setUnrecognised(alive_time)
  assert_false(object.recognised(rand(alive_time...after_death)), "occurred when checking the recognised status of the object after setting it to a time before the object's death and after its birth")
end

