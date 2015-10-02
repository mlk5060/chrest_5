unit_test "constructor" do
  non_blind_or_empty_object_id = "0"
  non_blind_or_empty_object_class = "A"
  
  blind_square_object = SceneObject.new("shouldBeOverwritten", Scene.getBlindSquareIdentifier())
  empty_square_object = SceneObject.new("shouldBeOverwritten", Scene.getEmptySquareIdentifier())
  non_blind_or_empty_object = SceneObject.new(non_blind_or_empty_object_id, non_blind_or_empty_object_class)
  
  assert_equal(blind_square_object.getIdentifier(), Scene.getBlindSquareIdentifier())
  assert_equal(blind_square_object.getObjectClass(), Scene.getBlindSquareIdentifier())
  
  assert_equal(empty_square_object.getIdentifier(), Scene.getEmptySquareIdentifier())
  assert_equal(empty_square_object.getObjectClass(), Scene.getEmptySquareIdentifier())
  
  assert_equal(non_blind_or_empty_object.getIdentifier(), non_blind_or_empty_object_id)
  assert_equal(non_blind_or_empty_object.getObjectClass(), non_blind_or_empty_object_class)
end
