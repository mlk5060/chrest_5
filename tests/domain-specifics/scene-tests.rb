# jchrest.lib.Scene tests

################################################################################
# Tests that a Scene correctly throws exceptions with illegal dimensions and 
# is entirely blind after construction if no illegal arguments are specified.
unit_test "constructor" do
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  scene_height_field = Scene.java_class.declared_field("_height")
  scene_width_field = Scene.java_class.declared_field("_width")
  scene_height_field.accessible = true
  scene_width_field.accessible = true
  
  for repeat in 1..3
  
    exception_thrown = false
    begin
      scene = Scene.new("test", 
        repeat == 1 ? -1 : repeat == 2 ? 0 : 2, 
        repeat == 1 ? -1 : repeat == 2 ? 0 : 2,
        1, 1, nil
      )
    rescue
      exception_thrown = true
    end
    
    expected_exception_thrown = (repeat == 3 ? false : true)
    assert_equal(
      exception_thrown,
      expected_exception_thrown,
      "occurred when checking if an exception is thrown during construction " +
      "in repeat " + repeat.to_s
    )
  
    if repeat == 3
      for col in 0...scene_width_field.value(scene)
        for row in 0...scene_height_field.value(scene)
          object = scene._scene.get(col).get(row)
          #Can't check identifier since this will be assigned randomly.
          assert_equal(Scene.getBlindSquareToken, object.getObjectType, "occurred when checking the type of the item on col " + col.to_s + " and row " + row.to_s)
        end
      end
    end
  end
end

################################################################################
# Create a scene representing a visual-cone (contains blind-spots) and runs the
# following sub-tests:
# 
# 1) Blind spots are overwritten.
# 2) Adding a non-empty object to an empty square causes the square to no 
#    longer be empty.
# 3) Adding an empty object to a non-empty square causes the square to become 
#    empty.
# 4) Adding a non-empty object to a non-empty square results in the new object 
#    replacing the old object.
# 5) Specifying a column/row coordinate < 0 or > max width/height of the Scene
#    throws an exception.
#    
# The scene created originally looks like the following ("x" represents blind
# squares).  Note that Scene and domain coordinates are both zero-indexed:
#
#   |------|------|------|------|------|
# 2 |  a   |      |      |  b   |      |
#   |------|------|------|------|------|
# 1    x   |      |  c   |      |  x
#          |------|------|------|
# 0    x      x   | SELF |  x      x
#                 |------|
#      0      1       2     3      4
unit_test "add_object_to_square" do
  Scene.class_eval{
    field_accessor :_scene
  }
  
  scene_height_field = Scene.java_class.declared_field("_height")
  scene_width_field = Scene.java_class.declared_field("_width")
  scene_height_field.accessible = true
  scene_width_field.accessible = true
  
  scene_object_identifier_field = SceneObject.java_class.declared_field("_identifier")
  scene_object_type_field = SceneObject.java_class.declared_field("_objectType")
  scene_object_identifier_field.accessible = true
  scene_object_type_field.accessible = true
  
  ########################################
  ##### CONSTRUCT AND POPULATE Scene #####
  ########################################
  
  scene = Scene.new("test", 5, 3, 0, 0, nil)
  scene.addObjectToSquare(2, 0, SceneObject.new("0", Scene.getCreatorToken))
  scene.addObjectToSquare(1, 1, SceneObject.new(Scene.getEmptySquareToken))
  scene.addObjectToSquare(2, 1, SceneObject.new("1", "c"))
  scene.addObjectToSquare(3, 1, SceneObject.new(Scene.getEmptySquareToken))
  scene.addObjectToSquare(0, 2, SceneObject.new("2", "a"))
  scene.addObjectToSquare(1, 2, SceneObject.new(Scene.getEmptySquareToken))
  scene.addObjectToSquare(2, 2, SceneObject.new(Scene.getEmptySquareToken))
  scene.addObjectToSquare(3, 2, SceneObject.new("3", "b"))
  scene.addObjectToSquare(4, 2, SceneObject.new(Scene.getEmptySquareToken))
  
  ################
  ##### TEST #####
  ################
  
  for col in 0...scene_width_field.value(scene)
    for row in 0...scene_height_field.value(scene)
      expected_identifier = nil
      expected_object_type = Scene.getBlindSquareToken()
      
      if col == 2 && row == 0 then expected_identifier, expected_object_type = "0", Scene.getCreatorToken() end
      if col == 1 && row == 1 then expected_object_type = Scene.getEmptySquareToken() end
      if col == 2 && row == 1 then expected_identifier, expected_object_type = "1", "c" end
      if col == 3 && row == 1 then expected_object_type = Scene.getEmptySquareToken() end
      if col == 0 && row == 2 then expected_identifier, expected_object_type = "2", "a" end
      if col == 1 && row == 2 then expected_object_type = Scene.getEmptySquareToken() end
      if col == 2 && row == 2 then expected_object_type = Scene.getEmptySquareToken() end
      if col == 3 && row == 2 then expected_identifier, expected_object_type = "3", "b" end
      if col == 4 && row == 2 then expected_object_type = Scene.getEmptySquareToken() end
      
      scene_object = scene._scene.get(col).get(row)
      if expected_identifier!= nil
        assert_equal(expected_identifier, scene_object_identifier_field.value(scene_object))
      end
      
      assert_equal(expected_object_type, scene_object_type_field.value(scene_object))
    end
  end
end

################################################################################
unit_test "add_objects_to_row" do
  scene = Scene.new("test", 5, 3, 0, 0, nil)
  blind = Scene.getBlindSquareToken
  empty = Scene.getEmptySquareToken
  
  row_0_items = ArrayList.new
  row_0_items.add(SceneObject.new(blind))
  row_0_items.add(SceneObject.new(blind))
  row_0_items.add(SceneObject.new("0", "d"))
  row_0_items.add(SceneObject.new(blind))
  row_0_items.add(SceneObject.new(blind))
  
  row_1_items = ArrayList.new
  row_1_items.add(SceneObject.new(blind))
  row_1_items.add(SceneObject.new(empty))
  row_1_items.add(SceneObject.new("1", "c"))
  row_1_items.add(SceneObject.new(empty))
  row_1_items.add(SceneObject.new(blind))
  
  row_2_items = ArrayList.new
  row_2_items.add(SceneObject.new("2", "a"))
  row_2_items.add(SceneObject.new(empty))
  row_2_items.add(SceneObject.new(empty))
  row_2_items.add(SceneObject.new("3", "b"))
  row_2_items.add(SceneObject.new(empty))

  scene.addObjectsToRow(0, row_0_items)
  scene.addObjectsToRow(1, row_1_items)
  scene.addObjectsToRow(2, row_2_items)
  
  for row in 0..2
    for col in 0..4
      
      expected_identifier = nil
      expected_object_class = blind
      
      if row == 0 
        if col == 2
          expected_identifier = "0"
          expected_object_class = "d"
        end
      end
      
      if row == 1
        if col == 1 or col == 3
          expected_object_class = empty
        elsif col == 2
          expected_identifier = "1"
          expected_object_class = "c"
        end
      end
      
      if row == 2
        if col == 1 or col == 2 or col == 4
          expected_object_class = empty
        elsif col == 0
          expected_identifier = "2"
          expected_object_class = "a"
        else
          expected_identifier = "3"
          expected_object_class = "b"
        end
      end
      
      squareContents = scene.getSquareContents(col, row)
      if expected_identifier != nil 
        assert_equal(expected_identifier, squareContents.getIdentifier(), "occurred when checking the object identifier of the object on col " + col.to_s + " and row " + row.to_s)
      end
      assert_equal(expected_object_class, squareContents.getObjectType(), "occurred when checking the object type of the object on col " + col.to_s + " and row " + row.to_s)
    end
  end
end

################################################################################
unit_test "are_domain_specific_coordinates_represented" do
  scene_width = 5
  scene_height = 5
  min_domain_specific_col = 7
  min_domain_specific_row = 6
  scene = Scene.new("", scene_width, scene_height, min_domain_specific_col, min_domain_specific_row, nil)
  
  assert_true(
    scene.areDomainSpecificCoordinatesRepresented(
      min_domain_specific_col + (scene_width - 1),
      min_domain_specific_row + (scene_height - 1)
    ),
    "occurred when coordinates should be represented"
  )
  
  assert_false(
    scene.areDomainSpecificCoordinatesRepresented(
      min_domain_specific_col + scene_width, 
      min_domain_specific_row + scene_height
    ),
    "occurred when coordinates should not be represented"
  )
end

################################################################################
# Scene used with scene-specific (SS) and domain-specific (DS) coordinates.
# 
# SS DS
#       |---|---|---|---|---|
# 4  11 |   |   |   |   |   |
#       |---|---|---|---|---|
# 3  10 |   |   |   |   |   |
#       |---|---|---|---|---|
# 2  9  |   |   |   |   |   |
#       |---|---|---|---|---|
# 1  8  |   |   |   |   |   |
#       |---|---|---|---|---|
# 0  7  |   |   |   |   |   |
#       |---|---|---|---|---|
#         7   8   9   10  11
#         0   1   2   3   4
unit_test "get_domain_col_and_row_from_scene_col_and_row" do
  scene = Scene.new("", 5, 5, 7, 7, nil)
  assert_equal(10, scene.getDomainSpecificColFromSceneSpecificCol(3), "occurred when getting column")
  assert_equal(11, scene.getDomainSpecificRowFromSceneSpecificRow(4), "occurred when getting row")
end

################################################################################
unit_test "get_minimum_domain_specific_column_and_row" do
  minimum_domain_specific_col = 5
  minimum_domain_specific_row = 4
  scene = Scene.new("", 2, 2, minimum_domain_specific_col, minimum_domain_specific_row, nil)
  
  # Despite the constructor setting the "_minimumDomainSpecificColumn" and 
  # "_minimumDomainSpecificRow" Scene variables above, do it manually here so 
  # that this test will not break if the constructor is modified in future.
  # These variables are final so can't be set using a class_eval construct, 
  # their "accessible" property needs to be set to true manually.
  minimum_domain_specific_col_field = scene.java_class.declared_field("_minimumDomainSpecificColumn")
  minimum_domain_specific_row_field = scene.java_class.declared_field("_minimumDomainSpecificRow")
  minimum_domain_specific_col_field.accessible = true
  minimum_domain_specific_row_field.accessible = true
  minimum_domain_specific_col_field.set_value(scene, minimum_domain_specific_col)
  minimum_domain_specific_row_field.set_value(scene, minimum_domain_specific_row)
  
  assert_equal(
    minimum_domain_specific_col_field.value(scene),
    scene.getMinimumDomainSpecificColumn(),
    "occurred when checking the result of 'getMinimumDomainSpecificColumn'"
  )
  
  assert_equal(
    minimum_domain_specific_row_field.value(scene),
    scene.getMinimumDomainSpecificRow(),
    "occurred when checking the result of 'getMinimumDomainSpecificRow'"
  )
end

################################################################################
unit_test "get-square-contents" do
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  scene = Scene.new("test", 2, 2, 0, 0, nil)
  scene._scene.get(1).set(0, SceneObject.new("0", "a"))
  scene._scene.get(1).set(1, SceneObject.new(Scene.getEmptySquareToken()))
  
  for row in 0..1
    for col in 0..1
      contents_of_square = scene.getSquareContents(col, row)
      expected_object_identifier = nil
      expected_object_class = Scene.getBlindSquareToken()
      
      if(col == 1)
        if(row == 0)
          expected_object_identifier = "0"
          expected_object_class = "a"
        elsif(row == 1)
          expected_object_class = Scene.getEmptySquareToken()
        end
      end
      
      if expected_object_identifier != nil
        assert_equal(expected_object_identifier, contents_of_square.getIdentifier(), "occurred when checking the identifier of the item on col " + col.to_s + " and row " + row.to_s)
      end
      assert_equal(expected_object_class, contents_of_square.getObjectType(), "occurred when checking the type of the item on col " + col.to_s + " and row " + row.to_s)
    end
  end
  
  assert_equal(nil, scene.getSquareContents(scene.getWidth(), 0), "occured when checking what's returned when specifying a col that's out of scope")
  assert_equal(nil, scene.getSquareContents(0, scene.getHeight()), "occured when checking what's returned when specifying a row that's out of scope")
end

################################################################################
unit_test "get-square-contents-as-list-pattern" do
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  scene = Scene.new("test", 3, 3, 0, 0, nil)
  scene._scene.get(1).set(0, SceneObject.new("1", "a"))
  scene._scene.get(1).set(1, SceneObject.new("0", Scene.getCreatorToken()))
  scene._scene.get(2).set(2, SceneObject.new(Scene.getEmptySquareToken()))
  
  for row in 0..2
    for col in 0..2
      
      expected_object_type = Scene.getBlindSquareToken()
      if(col == 1 && row == 0) then expected_object_type = "a" end
      if(col == 1 && row == 1) then expected_object_type = Scene.getCreatorToken() end
      if(col == 2 && row == 2) then expected_object_type = Scene.getEmptySquareToken() end
      
      expected_list_pattern = ListPattern.new
      expected_list_pattern.add(ItemSquarePattern.new(expected_object_type.to_s, col, row))
      
      result = scene.getSquareContentsAsListPattern(col, row)
      
      assert_equal(
        expected_list_pattern.toString(), 
        result.toString(), 
        "occurred when checking col " + col.to_s + " and row " + row.to_s
      )
    end
  end
end

################################################################################
# Tests whether all permutations of possible parameters to 
# Scene.getAsListPattern return the correct output when given a scene containing 
# blind, empty and non-empty squares.  The scene used is depicted 
# visually below ("x" represents a blind square in the scene):
#
#       |------|------|------|------|------|
# 2   2 |  a   |      |      |  b   |      |
#       |------|------|------|------|------|
# 1   1    x   |      |  c   |      |  x
#              |------|------|------|
# 0   0    x      x   | SELF |  x      x
#                     |------|
#          0      1      2      3      4     SCENE-SPECIFIC COORDS
#          
#         -2      -1     0      1      2     CREATOR-RELATIVE COORDS
#
# NOTE: can't accurately test situation where SceneObjects are identified by 
#       SceneObject identifiers since SceneObjects that represent blind squares
#       have randomly generated identifiers.
unit_test "get-as-list-pattern" do
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  scene = Scene.new("test", 5, 3, 0, 0, nil)
  
  scene._scene.get(2).set(0, SceneObject.new("0", Scene.getCreatorToken))
  scene._scene.get(1).set(1, SceneObject.new(Scene.getEmptySquareToken))
  scene._scene.get(2).set(1, SceneObject.new("1", "c"))
  scene._scene.get(3).set(1, SceneObject.new(Scene.getEmptySquareToken))
  scene._scene.get(0).set(2, SceneObject.new("2", "a"))
  scene._scene.get(1).set(2, SceneObject.new(Scene.getEmptySquareToken))
  scene._scene.get(2).set(2, SceneObject.new(Scene.getEmptySquareToken))
  scene._scene.get(3).set(2, SceneObject.new("3", "b"))
  scene._scene.get(4).set(2, SceneObject.new(Scene.getEmptySquareToken))
  
  blind = Scene.getBlindSquareToken
  empty = Scene.getEmptySquareToken
  
  expected_list_pattern = ListPattern.new
  expected_list_pattern.add(ItemSquarePattern.new(blind, 0, 0))
  expected_list_pattern.add(ItemSquarePattern.new(blind, 1, 0))
  expected_list_pattern.add(ItemSquarePattern.new(Scene.getCreatorToken, 2, 0))
  expected_list_pattern.add(ItemSquarePattern.new(blind, 3, 0))
  expected_list_pattern.add(ItemSquarePattern.new(blind, 4, 0))
  expected_list_pattern.add(ItemSquarePattern.new(blind, 0, 1))
  expected_list_pattern.add(ItemSquarePattern.new(empty, 1, 1))
  expected_list_pattern.add(ItemSquarePattern.new("c", 2, 1))
  expected_list_pattern.add(ItemSquarePattern.new(empty, 3, 1))
  expected_list_pattern.add(ItemSquarePattern.new(blind, 4, 1))
  expected_list_pattern.add(ItemSquarePattern.new("a", 0, 2))
  expected_list_pattern.add(ItemSquarePattern.new(empty, 1, 2))
  expected_list_pattern.add(ItemSquarePattern.new(empty, 2, 2))
  expected_list_pattern.add(ItemSquarePattern.new("b", 3, 2))
  expected_list_pattern.add(ItemSquarePattern.new(empty, 4, 2))
  
  assert_equal(
    expected_list_pattern, 
    scene.getAsListPattern()
  )
end

################################################################################
#Sub-tests:
# 1) If the scenes to be used do not contain the same number of squares, an
#    illegal argument exception should be thrown.
# 2) If there are fewer non-empty squares in the base scene than the scene to
#    compare against, 0 should be returned (this would indicate errors of 
#    ommission).
# 3) The correct value is returned when there are errors of commission in a 
#    scene when compared against another.
unit_test "compute-errors-of-commission" do
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  ######################
  ##### Sub-Test 1 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 1, 1, 0, 0, nil)
  scene2 = Scene.new("test-scene-2", 1, 2, 0, 0, nil)
  error_thrown = false
  
  begin
    scene1.computeErrorsOfCommission(scene2)
  rescue 
    #Swallow the error output to retain pretty test output but set error_thrown 
    #flag to true to indicate that an error was actually thrown.
    error_thrown = true
  end

  assert_true(error_thrown, "occured in sub-test 1.")
  
  ######################
  ##### Sub-Test 2 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 2, 2, 0, 0, nil)
  scene2 = Scene.new("test-scene-2", 2, 2, 0, 0, nil)
  
  scene1._scene.get(0).set(0, SceneObject.new("a"))
  
  scene2._scene.get(0).set(0, SceneObject.new("a"))
  scene2._scene.get(1).set(0, SceneObject.new("b"))
  
  assert_equal(0, scene1.computeErrorsOfCommission(scene2), "occurred in sub-test 2.")
  
  ######################
  ##### Sub-Test 3 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 5, 3, 0, 0, nil)
  scene2 = Scene.new("test-scene-2", 5, 3, 0, 0, nil)
  
  blind = Scene.getBlindSquareToken
  empty = Scene.getEmptySquareToken
  
  #Scene 1 should have 2 more objects than scene 2.
  scene_1_row_0_items = ArrayList.new
  scene_1_row_0_items.add(SceneObject.new(blind))
  scene_1_row_0_items.add(SceneObject.new(blind))
  scene_1_row_0_items.add(SceneObject.new("d"))
  scene_1_row_0_items.add(SceneObject.new(blind))
  scene_1_row_0_items.add(SceneObject.new(blind))
  scene_1_row_1_items = ArrayList.new
  scene_1_row_1_items.add(SceneObject.new(blind))
  scene_1_row_1_items.add(SceneObject.new(empty))
  scene_1_row_1_items.add(SceneObject.new("c"))
  scene_1_row_1_items.add(SceneObject.new(empty))
  scene_1_row_1_items.add(SceneObject.new(blind))
  scene_1_row_2_items = ArrayList.new
  scene_1_row_2_items.add(SceneObject.new("a"))
  scene_1_row_2_items.add(SceneObject.new(empty))
  scene_1_row_2_items.add(SceneObject.new("e"))
  scene_1_row_2_items.add(SceneObject.new("b"))
  scene_1_row_2_items.add(SceneObject.new("f"))
  
  scene_2_row_0_items = ArrayList.new
  scene_2_row_0_items.add(SceneObject.new(blind))
  scene_2_row_0_items.add(SceneObject.new(blind))
  scene_2_row_0_items.add(SceneObject.new("d"))
  scene_2_row_0_items.add(SceneObject.new(blind))
  scene_2_row_0_items.add(SceneObject.new(blind))
  scene_2_row_1_items = ArrayList.new
  scene_2_row_1_items.add(SceneObject.new(blind))
  scene_2_row_1_items.add(SceneObject.new(empty))
  scene_2_row_1_items.add(SceneObject.new("c"))
  scene_2_row_1_items.add(SceneObject.new(empty))
  scene_2_row_1_items.add(SceneObject.new(blind))
  scene_2_row_2_items = ArrayList.new
  scene_2_row_2_items.add(SceneObject.new("a"))
  scene_2_row_2_items.add(SceneObject.new(empty))
  scene_2_row_2_items.add(SceneObject.new(empty))
  scene_2_row_2_items.add(SceneObject.new("b"))
  scene_2_row_2_items.add(SceneObject.new(empty))
  
  scene1.addObjectsToRow(0, scene_1_row_0_items)
  scene1.addObjectsToRow(1, scene_1_row_1_items)
  scene1.addObjectsToRow(2, scene_1_row_2_items)
  
  scene2.addObjectsToRow(0, scene_2_row_0_items)
  scene2.addObjectsToRow(1, scene_2_row_1_items)
  scene2.addObjectsToRow(2, scene_2_row_2_items)
  
  assert_equal(2, scene1.computeErrorsOfCommission(scene2), "occurred in sub-test 3.")
end

################################################################################
#Sub-tests:
# 1) If the scenes to be used do not contain the same number of squares, an
#    illegal argument exception should be thrown.
# 2) If there are more non-empty squares in the base scene than the scene to
#    compare against, 0 should be returned (this would indicate errors of 
#    commission).
# 3) The correct value is returned when there are errors of commission in a 
#    scene when compared against another.
unit_test "compute-errors-of-omission" do
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  ######################
  ##### Sub-Test 1 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 1, 1, 0, 0, nil)
  scene2 = Scene.new("test-scene-2", 1, 2, 0, 0, nil)
  error_thrown = false
  
  begin
    scene1.computeErrorsOfOmission(scene2)
  rescue 
    #Swallow the error output to retain pretty test output but set error_thrown 
    #flag to true to indicate that an error was actually thrown.
    error_thrown = true
  end

  assert_true(error_thrown, "occured in sub-test 1.")
  
  ######################
  ##### Sub-Test 2 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 2, 2, 0, 0, nil)
  scene2 = Scene.new("test-scene-2", 2, 2, 0, 0, nil)
  
  scene1._scene.get(0).set(0, SceneObject.new("a"))
  scene1._scene.get(1).set(0, SceneObject.new("b"))
  
  scene2._scene.get(0).set(0, SceneObject.new("a"))
  
  assert_equal(0, scene1.computeErrorsOfOmission(scene2), "occurred in sub-test 2.")
  
  ######################
  ##### Sub-Test 3 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 5, 3, 0, 0, nil)
  scene2 = Scene.new("test-scene-2", 5, 3, 0, 0, nil)
  
  blind = Scene.getBlindSquareToken
  empty = Scene.getEmptySquareToken
  
  #Scene 1 should have 2 fewer objects than scene 2.
  scene_1_row_0_items = ArrayList.new
  scene_1_row_0_items.add(SceneObject.new(blind))
  scene_1_row_0_items.add(SceneObject.new(blind))
  scene_1_row_0_items.add(SceneObject.new("0", "d"))
  scene_1_row_0_items.add(SceneObject.new(blind))
  scene_1_row_0_items.add(SceneObject.new(blind))
  scene_1_row_1_items = ArrayList.new
  scene_1_row_1_items.add(SceneObject.new(blind))
  scene_1_row_1_items.add(SceneObject.new(empty))
  scene_1_row_1_items.add(SceneObject.new("1", "c"))
  scene_1_row_1_items.add(SceneObject.new(empty))
  scene_1_row_1_items.add(SceneObject.new(blind))
  scene_1_row_2_items = ArrayList.new
  scene_1_row_2_items.add(SceneObject.new("2", "a"))
  scene_1_row_2_items.add(SceneObject.new(empty))
  scene_1_row_2_items.add(SceneObject.new(empty))
  scene_1_row_2_items.add(SceneObject.new("3", "b"))
  scene_1_row_2_items.add(SceneObject.new(empty))
  
  scene_2_row_0_items = ArrayList.new
  scene_2_row_0_items.add(SceneObject.new(blind))
  scene_2_row_0_items.add(SceneObject.new(blind))
  scene_2_row_0_items.add(SceneObject.new("0", "d"))
  scene_2_row_0_items.add(SceneObject.new(blind))
  scene_2_row_0_items.add(SceneObject.new(blind))
  scene_2_row_1_items = ArrayList.new
  scene_2_row_1_items.add(SceneObject.new(blind))
  scene_2_row_1_items.add(SceneObject.new(empty))
  scene_2_row_1_items.add(SceneObject.new("1", "c"))
  scene_2_row_1_items.add(SceneObject.new(empty))
  scene_2_row_1_items.add(SceneObject.new(blind))
  scene_2_row_2_items = ArrayList.new
  scene_2_row_2_items.add(SceneObject.new("2", "a"))
  scene_2_row_2_items.add(SceneObject.new(empty))
  scene_2_row_2_items.add(SceneObject.new("3", "e"))
  scene_2_row_2_items.add(SceneObject.new("4", "b"))
  scene_2_row_2_items.add(SceneObject.new("5", "f"))
  
  scene1.addObjectsToRow(0, scene_1_row_0_items)
  scene1.addObjectsToRow(1, scene_1_row_1_items)
  scene1.addObjectsToRow(2, scene_1_row_2_items)
  
  scene2.addObjectsToRow(0, scene_2_row_0_items)
  scene2.addObjectsToRow(1, scene_2_row_1_items)
  scene2.addObjectsToRow(2, scene_2_row_2_items)
  
  assert_equal(2, scene1.computeErrorsOfOmission(scene2), "occurred in sub-test 3.")
end

################################################################################
#Sub-tests:
# 1) 0 is returned if either scene in calculation is empty.
# 2) Error is thrown if size of scenes to be used in calculation are not equal.
# 3) The correct value is returned when objects in a scene are represented using
#    their object class.
unit_test "compute-precision" do
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  ######################
  ##### Sub-Test 1 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 5, 3, 0, 0, nil)
  
  scene2 = Scene.new("test-scene-2", 5, 3, 0, 0, nil)
  scene2._scene.get(0).set(0, SceneObject.new("0", "a"))
  
  scene3 = Scene.new("test-scene-3", 5, 3, 0, 0, nil)
  
  #Note that object representation has no bearing for this sub-test so false is
  #passed as the parameter concerning this feature in the computePrecision 
  #function.
  assert_equal(0.0, scene1.computePrecision(scene2), "occurred in sub-test 1 when base scene is empty.")
  assert_equal(0.0, scene2.computePrecision(scene3), "occurred in sub-test 1 when scene compared against is empty.")
  
  ######################
  ##### Sub-Test 2 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 1, 1, 0, 0, nil)
  scene2 = Scene.new("test-scene-2", 1, 2, 0, 0, nil)
  error_thrown = false
  
  begin
    #Note that object representation has no bearing for this sub-test so false 
    #is passed as the parameter concerning this feature in the computePrecision 
    #function.
    scene1.computePrecision(scene2, false)
  rescue 
    #Swallow the error output to retain pretty test output but set error_thrown 
    #flag to true to indicate that an error was actually thrown.
    error_thrown = true
  end

  assert_true(error_thrown, "occured in sub-test 2.")
  
  ######################
  ##### Sub-Test 3 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 5, 3, 0, 0, nil)
  scene2 = Scene.new("test-scene-2", 5, 3, 0, 0, nil)
  
  blind = Scene.getBlindSquareToken
  empty = Scene.getEmptySquareToken
  
  #Note that objects in both scenes are in the correct locations whilst their
  #object identifiers differ but their classes match.
  scene_1_row_0_items = ArrayList.new
  scene_1_row_0_items.add(SceneObject.new(blind))
  scene_1_row_0_items.add(SceneObject.new(blind))
  scene_1_row_0_items.add(SceneObject.new("1", "a"))
  scene_1_row_0_items.add(SceneObject.new("2", "d"))
  scene_1_row_0_items.add(SceneObject.new(blind))
  scene_1_row_1_items = ArrayList.new
  scene_1_row_1_items.add(SceneObject.new(blind))
  scene_1_row_1_items.add(SceneObject.new(empty))
  scene_1_row_1_items.add(SceneObject.new("0", "b"))
  scene_1_row_1_items.add(SceneObject.new("3", "e"))
  scene_1_row_1_items.add(SceneObject.new(blind))
  scene_1_row_2_items = ArrayList.new
  scene_1_row_2_items.add(SceneObject.new("4", "c"))
  scene_1_row_2_items.add(SceneObject.new(empty))
  scene_1_row_2_items.add(SceneObject.new(empty))
  scene_1_row_2_items.add(SceneObject.new(empty))
  scene_1_row_2_items.add(SceneObject.new("5", "f"))
  
  scene_2_row_0_items = ArrayList.new
  scene_2_row_0_items.add(SceneObject.new(blind))
  scene_2_row_0_items.add(SceneObject.new(blind))
  scene_2_row_0_items.add(SceneObject.new("0", "a"))
  scene_2_row_0_items.add(SceneObject.new("1", "d"))
  scene_2_row_0_items.add(SceneObject.new(blind))
  scene_2_row_1_items = ArrayList.new
  scene_2_row_1_items.add(SceneObject.new(blind))
  scene_2_row_1_items.add(SceneObject.new(empty))
  scene_2_row_1_items.add(SceneObject.new("2", "b"))
  scene_2_row_1_items.add(SceneObject.new("3", "e"))
  scene_2_row_1_items.add(SceneObject.new(blind))
  scene_2_row_2_items = ArrayList.new
  scene_2_row_2_items.add(SceneObject.new("4", "c"))
  scene_2_row_2_items.add(SceneObject.new(empty))
  scene_2_row_2_items.add(SceneObject.new(empty))
  scene_2_row_2_items.add(SceneObject.new(empty))
  scene_2_row_2_items.add(SceneObject.new("5", "f"))
  
  scene1.addObjectsToRow(0, scene_1_row_0_items)
  scene1.addObjectsToRow(1, scene_1_row_1_items)
  scene1.addObjectsToRow(2, scene_1_row_2_items)
  
  scene2.addObjectsToRow(0, scene_2_row_0_items)
  scene2.addObjectsToRow(1, scene_2_row_1_items)
  scene2.addObjectsToRow(2, scene_2_row_2_items)
  
  assert_equal(1, scene1.computePrecision(scene2), "occurred in sub-test 3.")
end

################################################################################
#Sub-tests:
# 1) 0 is returned if either scene in calculation is empty.
# 2) Error is thrown if size of scenes to be used in calculation are not equal.
# 3) The correct value is returned when objects in a scene are represented using
#    their object class.
unit_test "compute-recall" do 
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  ######################
  ##### Sub-Test 1 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 5, 3, 0, 0, nil)
  
  scene2 = Scene.new("test-scene-2", 5, 3, 0, 0, nil)
  scene2._scene.get(0).set(0, SceneObject.new("0", "a"))
  
  scene3 = Scene.new("test-scene-3", 5, 3, 0, 0, nil)
  
  #Note that object representation has no bearing for this sub-test so false is
  #passed as the parameter concerning this feature in the computePrecision 
  #function.
  assert_equal(0.0, scene1.computeRecall(scene2), "occurred in sub-test 1 when base scene is empty.")
  assert_equal(0.0, scene2.computeRecall(scene3), "occurred in sub-test 1 when scene compared against is empty.")
  
  ######################
  ##### Sub-Test 2 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 1, 1, 0, 0, nil)
  scene2 = Scene.new("test-scene-2", 1, 2, 0, 0, nil)
  error_thrown = false
  
  begin
    #Note that object representation has no bearing for this sub-test so false 
    #is passed as the parameter concerning this feature in the computeRecall 
    #function.
    scene1.computeRecall(scene2)
  rescue 
    #Swallow the error output to retain pretty test output but set error_thrown 
    #flag to true to indicate that an error was actually thrown.
    error_thrown = true
  end

  assert_true(error_thrown, "occured in sub-test 2.")
  
  ######################
  ##### Sub-Test 3 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 5, 3, 0, 0, nil)
  scene2 = Scene.new("test-scene-2", 5, 3, 0, 0, nil)
  
  blind = Scene.getBlindSquareToken
  empty = Scene.getEmptySquareToken
  
  #Note that no object's location is the same between scenes and whilst the
  #object classes specified are the same in each scene, object identities differ.
  scene_1_row_0_items = ArrayList.new
  scene_1_row_0_items.add(SceneObject.new(blind))
  scene_1_row_0_items.add(SceneObject.new("1", "a"))
  scene_1_row_0_items.add(SceneObject.new("2", "d"))
  scene_1_row_0_items.add(SceneObject.new(empty))
  scene_1_row_0_items.add(SceneObject.new(blind))
  scene_1_row_1_items = ArrayList.new
  scene_1_row_1_items.add(SceneObject.new(blind))
  scene_1_row_1_items.add(SceneObject.new("0", "b"))
  scene_1_row_1_items.add(SceneObject.new("6", "e"))
  scene_1_row_1_items.add(SceneObject.new(blind))
  scene_1_row_1_items.add(SceneObject.new(blind))
  scene_1_row_2_items = ArrayList.new
  scene_1_row_2_items.add(SceneObject.new(empty))
  scene_1_row_2_items.add(SceneObject.new("7", "c"))
  scene_1_row_2_items.add(SceneObject.new(empty))
  scene_1_row_2_items.add(SceneObject.new("8", "f"))
  scene_1_row_2_items.add(SceneObject.new(empty))
  
  scene_2_row_0_items = ArrayList.new
  scene_2_row_0_items.add(SceneObject.new(blind))
  scene_2_row_0_items.add(SceneObject.new(blind))
  scene_2_row_0_items.add(SceneObject.new("0", "a"))
  scene_2_row_0_items.add(SceneObject.new("1", "d"))
  scene_2_row_0_items.add(SceneObject.new(blind))
  scene_2_row_1_items = ArrayList.new
  scene_2_row_1_items.add(SceneObject.new(blind))
  scene_2_row_1_items.add(SceneObject.new(empty))
  scene_2_row_1_items.add(SceneObject.new("2", "b"))
  scene_2_row_1_items.add(SceneObject.new("3", "e"))
  scene_2_row_1_items.add(SceneObject.new(blind))
  scene_2_row_2_items = ArrayList.new
  scene_2_row_2_items.add(SceneObject.new("4", "c"))
  scene_2_row_2_items.add(SceneObject.new(empty))
  scene_2_row_2_items.add(SceneObject.new(empty))
  scene_2_row_2_items.add(SceneObject.new(empty))
  scene_2_row_2_items.add(SceneObject.new("5", "f"))
  
  scene1.addObjectsToRow(0, scene_1_row_0_items)
  scene1.addObjectsToRow(1, scene_1_row_1_items)
  scene1.addObjectsToRow(2, scene_1_row_2_items)
  
  scene2.addObjectsToRow(0, scene_2_row_0_items)
  scene2.addObjectsToRow(1, scene_2_row_1_items)
  scene2.addObjectsToRow(2, scene_2_row_2_items)
  
  assert_equal(1.0, scene1.computeRecall(scene2), "occurred in sub-test 3.")
end

# Blind square identifier not publicly settable so not possible to test 
# "getBlindSquareToken" accurately.

# Empty square identifier not publicly settable so not possible to test 
# "getEmptySquareToken" accurately.

# Self identifier not publicly settable so not possible to test 
# "getSelfIdentifier" accurately.

################################################################################
# Tests function using 8 scenarios (scenarios 3-6 test the "sameDomainSpace" 
# function, essentially.  Each scenario tests a particular clause in a 
# conditional in the function.
# 
# Scenario Descriptions
# =====================
# 
# 1: Scene is null
# 2: Classes are not equal
# 3: sameDomainSpace evaluates to false (wrong width)
# 4: sameDomainSpace evaluates to false (wrong height)
# 5: sameDomainSpace evaluates to false (wrong min col)
# 6: sameDomainSpace evaluates to false (wrong min row)
# 7: SceneObjects not in same place
# 8: Everything OK
#
# The function should return false for every scenario except 8.
unit_test "equals" do
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  for scenario in 1..8
    
    scene = Scene.new("", 5, 5, 4, 4, nil)
    comparison_scene = 
      (scenario == 1 ? 
        nil : 
        (scenario == 2 ? 
          ChessBoard.new("") : 
          Scene.new(
            "",
            (scenario == 3 ? 2 : 5),
            (scenario == 4 ? 2 : 5),
            (scenario == 5 ? 2 : 4),
            (scenario == 6 ? 2 : 4),
            nil
          ) 
        )
      )
      
    scene._scene.get(0).set(0, SceneObject.new("3", "T"))
    if comparison_scene != nil 
      comparison_scene._scene.get(scenario == 7 ? 2 : 0).set(0, SceneObject.new("3", "T"))
    end
      
    assert_equal(
      (scenario == 8 ? true : false),
      scene.equals(comparison_scene),
      "occurred during scenario " + scenario.to_s
    )
  end
end

################################################################################
unit_test "get_height" do
  height = 3
  scene = Scene.new("test-scene", 5, height, 0, 0, nil)
  assert_equal(height, scene.getHeight())
end

################################################################################
#Tests all possible permutations of the "getItemsInScopeAsListPatternMethod".
#
#              |------|------|------|
# 2   4    x   |      |  g   |      |  x
#       |------|------|------|------|------|
# 1   3 |      |  x   |  d   |  x   |      |
#       |------|------|------|------|------|
# 0   2 |      |  b   | SELF |  e   |      |
#       |------|------|------|------|------|
# -1  1 |      |  x   |  a   |  x   |      |
#       |------|------|------|------|------|
# -2  0    x   |      |  f   |      |  x
#              |------|------|------|
#          0      1      2      3      4      SCENE-SPECIFIC COORDS
#          
#         -2     -1      0      1      2      CREATOR-RELATIVE COORDS
unit_test "get_items_in_scope_as_list_pattern" do
  blind = Scene.getBlindSquareToken
  empty = Scene.getEmptySquareToken
  scene = Scene.new("test", 5, 5, 0, 0, nil)

  row_0_items = ArrayList.new
  row_0_items.add(SceneObject.new(blind))
  row_0_items.add(SceneObject.new(empty))
  row_0_items.add(SceneObject.new("1", "f"))
  row_0_items.add(SceneObject.new(empty))
  row_0_items.add(SceneObject.new(blind))
  
  row_1_items = ArrayList.new
  row_1_items.add(SceneObject.new(empty))
  row_1_items.add(SceneObject.new(blind))
  row_1_items.add(SceneObject.new("2", "a"))
  row_1_items.add(SceneObject.new(blind))
  row_1_items.add(SceneObject.new(empty))
  
  row_2_items = ArrayList.new
  row_2_items.add(SceneObject.new(empty))
  row_2_items.add(SceneObject.new("3", "b"))
  row_2_items.add(SceneObject.new("0", Scene.getCreatorToken()))
  row_2_items.add(SceneObject.new("4", "e"))
  row_2_items.add(SceneObject.new(empty))
  
  row_3_items = ArrayList.new
  row_3_items.add(SceneObject.new(empty))
  row_3_items.add(SceneObject.new(blind))
  row_3_items.add(SceneObject.new("5", "d"))
  row_3_items.add(SceneObject.new(blind))
  row_3_items.add(SceneObject.new(empty))
  
  row_4_items = ArrayList.new
  row_4_items.add(SceneObject.new(blind))
  row_4_items.add(SceneObject.new(empty))
  row_4_items.add(SceneObject.new("6", "g"))
  row_4_items.add(SceneObject.new(empty))
  row_4_items.add(SceneObject.new(blind))
  
  scene.addObjectsToRow(0, row_0_items)
  scene.addObjectsToRow(1, row_1_items)
  scene.addObjectsToRow(2, row_2_items)
  scene.addObjectsToRow(3, row_3_items)
  scene.addObjectsToRow(4, row_4_items)
  
  expected_list_pattern = ListPattern.new
  expected_list_pattern.add(ItemSquarePattern.new(blind, 0, 0))
  expected_list_pattern.add(ItemSquarePattern.new(empty, 1, 0))
  expected_list_pattern.add(ItemSquarePattern.new("f", 2, 0))
  expected_list_pattern.add(ItemSquarePattern.new(empty, 3, 0))
  expected_list_pattern.add(ItemSquarePattern.new(blind, 4, 0))
  expected_list_pattern.add(ItemSquarePattern.new(empty, 0, 1))
  expected_list_pattern.add(ItemSquarePattern.new(blind, 1, 1))
  expected_list_pattern.add(ItemSquarePattern.new("a", 2, 1))
  expected_list_pattern.add(ItemSquarePattern.new(blind, 3, 1))
  expected_list_pattern.add(ItemSquarePattern.new(empty, 4, 1))
  expected_list_pattern.add(ItemSquarePattern.new(empty, 0, 2))
  expected_list_pattern.add(ItemSquarePattern.new("b", 1, 2))
  expected_list_pattern.add(ItemSquarePattern.new(Scene.getCreatorToken(), 2, 2))
  expected_list_pattern.add(ItemSquarePattern.new("e", 3, 2))
  expected_list_pattern.add(ItemSquarePattern.new(empty, 4, 2))
  expected_list_pattern.add(ItemSquarePattern.new(empty, 0, 3))
  expected_list_pattern.add(ItemSquarePattern.new(blind, 1, 3))
  expected_list_pattern.add(ItemSquarePattern.new("d", 2, 3))
  expected_list_pattern.add(ItemSquarePattern.new(blind, 3, 3))
  expected_list_pattern.add(ItemSquarePattern.new(empty, 4, 3))
  expected_list_pattern.add(ItemSquarePattern.new(blind, 0, 4))
  expected_list_pattern.add(ItemSquarePattern.new(empty, 1, 4))
  expected_list_pattern.add(ItemSquarePattern.new("g", 2, 4))
  expected_list_pattern.add(ItemSquarePattern.new(empty, 3, 4))
  expected_list_pattern.add(ItemSquarePattern.new(blind, 4, 4))
  
  assert_equal(
    expected_list_pattern, 
    scene.getItemsInScopeAsListPattern(2, 2, 2)
  )
end

################################################################################
#          |------|------|------|
# 4    x   |      |  g   |      |  x
#   |------|------|------|------|------|
# 3 |      |  x   |  d   |  x   |      |
#   |------|------|------|------|------|
# 2 |      |  b   | SELF |  e   |      |
#   |------|------|------|------|------|
# 1 |      |  x   |  a   |  x   |      |
#   |------|------|------|------|------|
# 0    x   |      |  f   |      |  x
#          |------|------|------|
#      0      1      2      3      4      
unit_test "get_location_of_self" do
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  blind = Scene.getBlindSquareToken
  empty = Scene.getEmptySquareToken
  
  scene_with_creator = Scene.new("", 5, 5, 0, 0, nil)
  scene_with_creator._scene.get(1).set(0, SceneObject.new(empty))
  scene_with_creator._scene.get(2).set(0, SceneObject.new("1", "f"))
  scene_with_creator._scene.get(3).set(0, SceneObject.new(empty))
  scene_with_creator._scene.get(0).set(1, SceneObject.new(empty))
  scene_with_creator._scene.get(2).set(1, SceneObject.new("2", "a"))
  scene_with_creator._scene.get(4).set(1, SceneObject.new(empty))
  scene_with_creator._scene.get(0).set(2, SceneObject.new(empty))
  scene_with_creator._scene.get(1).set(2, SceneObject.new("3", "b"))
  scene_with_creator._scene.get(2).set(2, SceneObject.new("0", Scene.getCreatorToken()))
  scene_with_creator._scene.get(3).set(2, SceneObject.new("4", "e"))
  scene_with_creator._scene.get(4).set(2, SceneObject.new(empty))
  scene_with_creator._scene.get(0).set(3, SceneObject.new(empty))
  scene_with_creator._scene.get(2).set(3, SceneObject.new("5", "d"))
  scene_with_creator._scene.get(4).set(3, SceneObject.new(empty))
  scene_with_creator._scene.get(1).set(4, SceneObject.new(empty))
  scene_with_creator._scene.get(2).set(4, SceneObject.new("6", "g"))
  scene_with_creator._scene.get(3).set(4, SceneObject.new(empty))
  
  scene_without_creator = Scene.new("", 5, 5, 0, 0, nil)
  scene_without_creator._scene.get(1).set(0, SceneObject.new(empty))
  scene_without_creator._scene.get(2).set(0, SceneObject.new("1", "f"))
  scene_without_creator._scene.get(3).set(0, SceneObject.new(empty))
  scene_without_creator._scene.get(0).set(1, SceneObject.new(empty))
  scene_without_creator._scene.get(2).set(1, SceneObject.new("2", "a"))
  scene_without_creator._scene.get(4).set(1, SceneObject.new(empty))
  scene_without_creator._scene.get(0).set(2, SceneObject.new(empty))
  scene_without_creator._scene.get(1).set(2, SceneObject.new("3", "b"))
  scene_without_creator._scene.get(2).set(2, SceneObject.new("0", "c"))
  scene_without_creator._scene.get(3).set(2, SceneObject.new("4", "e"))
  scene_without_creator._scene.get(4).set(2, SceneObject.new(empty))
  scene_without_creator._scene.get(0).set(3, SceneObject.new(empty))
  scene_without_creator._scene.get(2).set(3, SceneObject.new("5", "d"))
  scene_without_creator._scene.get(4).set(3, SceneObject.new(empty))
  scene_without_creator._scene.get(1).set(4, SceneObject.new(empty))
  scene_without_creator._scene.get(2).set(4, SceneObject.new("6", "g"))
  scene_without_creator._scene.get(3).set(4, SceneObject.new(empty))
  
  assert_equal(Square.new(2, 2).toString(), scene_with_creator.getLocationOfCreator().toString(), "occurred when scene creator is present in scene.")
  assert_equal(nil, scene_without_creator.getLocationOfCreator(), "occurred when scene creator is not present in scene.")
end

################################################################################
unit_test "get_name" do
  scene = Scene.new("", 5, 5, 0, 0, nil)
  assert_equal("", scene.getName(), "occurred when scene name should be empty.")
  
  scene_name = "test"
  scene = Scene.new(scene_name, 5, 5, 0, 0, nil)
  assert_equal(scene_name, scene.getName(), "occurred when scene name should not be empty.")
end

################################################################################
unit_test "get_width" do
  width = 5
  scene = Scene.new("", 5, width, 0, 0, nil)
  assert_equal(width, scene.getWidth())
end

################################################################################
# Tests "Scene.isBlind" using various scenarios. 
#
# Scenario Details
# ================
# 
# - Scenario 1
#   ~ Input Scene is composed of SceneObjects whose type is equal to 
#     Scene::BLIND_SQUARE_TOKEN.
# - Scenario 2
#   ~ Input Scene is composed of multiple SceneObjects whose type is equal to 
#     Scene::BLIND_SQUARE_TOKEN and 1 SceneObject whose type is equal to 
#     Scene::BLIND_CREATOR_TOKEN.
# - Scenario 3 
#   ~ Input Scene is composed of 1 SceneObject whose type is equal to 
#     Scene::BLIND_SQUARE_TOKEN, 1 SceneObject whose type is equal to 
#     Scene::BLIND_CREATOR_TOKEN and multiple SceneObject whose type is equal to
#     either Scene::EMPTY_SQUARE_TOKEN or "none of the above".
#     
# Expected Output
# ===============
# 
# Method should return the following for each scenario:
# 
# - Scenario 1: true
# - Scenario 2: true
# - Scenario 3: false
unit_test "is_blind" do
  for scenario in 1..3
    
    ##################
    ##### SET-UP #####
    ##################
    
    # Create Scene with huge dimensions so that there is a good mix of 
    # SceneObjects with different types in scenario 3
    scene = Scene.new("", 70, 70, 0, 0, nil)

    # Need access to the private, final "_scene" Scene variable so that other
    # methods aren't relied upon (if they change this test may break).  So, 
    # since the variable is final, a "class_eval" construct won't allow access
    # to the variable so its accessibility must be set manually.
    scene_field = scene.java_class.declared_field("_scene")
    scene_field.accessible = true
    scene_data_structure = scene_field.value(scene)

    # Populate a data structure with object types to use when populating the
    # Scene with SceneObjects
    scene_object_types = []
    if [1,2].include?(scenario)
      scene_object_types.push(Scene::BLIND_SQUARE_TOKEN)
    else
      scene_object_types.push(Scene::EMPTY_SQUARE_TOKEN)
      (("a".."z").to_a - [Scene::CREATOR_TOKEN, Scene::EMPTY_SQUARE_TOKEN, Scene::BLIND_SQUARE_TOKEN]).each { 
        |scene_object_type| scene_object_types.push(scene_object_type) 
      }
    end
    
    # Populate the Scene with SceneObjects.
    for col in 0...scene_data_structure.size()
      for row in 0...scene_data_structure.get(col).size()
        scene_data_structure.get(col).set(row, SceneObject.new(scene_object_types.sample))
      end
    end
    
    # In scenario 2, add 1 creator SceneObject to the Scene.
    if scenario == 2 then scene_data_structure.get(34).set(34, SceneObject.new(Scene::CREATOR_TOKEN)) end
    
    # In scenario 3, add 1 creator SceneObject to the Scene and 1 blind 
    # SceneObject to the Scene so that the test can verify that the Scene must 
    # consist *entirely* of blind SceneObjects and a creator SceneObject for 
    # the function to return true.
    if scenario == 3 
      scene_data_structure.get(0).set(0, SceneObject.new(Scene.getBlindSquareToken))
      scene_data_structure.get(34).set(34, SceneObject.new(Scene::CREATOR_TOKEN))
    end
    
    ################
    ##### TEST #####
    ################
    
    expected_result = (scenario == 3 ? false : true)
    assert_equal(
      expected_result,
      scene.isBlind(),
      "occurred in scenario " + scenario.to_s
    )
  end
end

################################################################################
unit_test "is_square_blind" do
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  empty = Scene.getEmptySquareToken
  
  scene = Scene.new("", 5, 5, 0, 0, nil)
  scene._scene.get(1).set(0, SceneObject.new(empty))
  scene._scene.get(2).set(0, SceneObject.new("1", "f"))
  scene._scene.get(3).set(0, SceneObject.new(empty))
  scene._scene.get(0).set(1, SceneObject.new(empty))
  scene._scene.get(2).set(1, SceneObject.new("2", "a"))
  scene._scene.get(4).set(1, SceneObject.new(empty))
  scene._scene.get(0).set(2, SceneObject.new(empty))
  scene._scene.get(1).set(2, SceneObject.new("3", "b"))
  scene._scene.get(2).set(2, SceneObject.new("0", "c"))
  scene._scene.get(3).set(2, SceneObject.new("4", "e"))
  scene._scene.get(4).set(2, SceneObject.new(empty))
  scene._scene.get(0).set(3, SceneObject.new(empty))
  scene._scene.get(2).set(3, SceneObject.new("5", "d"))
  scene._scene.get(4).set(3, SceneObject.new(empty))
  scene._scene.get(1).set(4, SceneObject.new(empty))
  scene._scene.get(2).set(4, SceneObject.new("6", "g"))
  scene._scene.get(3).set(4, SceneObject.new(empty))
  
  assert_true(scene.isSquareBlind(0, 0), "occurred when square specified should be blind.")
  assert_false(scene.isSquareBlind(1, 0), "occurred when square specified should be empty.")
  assert_false(scene.isSquareBlind(2, 0), "occurred when square specified should contain an item.")
end

################################################################################
unit_test "is_square_empty" do
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  empty = Scene.getEmptySquareToken
  
  scene = Scene.new("", 5, 5, 0, 0, nil)
  scene._scene.get(1).set(0, SceneObject.new(empty))
  scene._scene.get(2).set(0, SceneObject.new("1", "f"))
  scene._scene.get(3).set(0, SceneObject.new(empty))
  scene._scene.get(0).set(1, SceneObject.new(empty))
  scene._scene.get(2).set(1, SceneObject.new("2", "a"))
  scene._scene.get(4).set(1, SceneObject.new(empty))
  scene._scene.get(0).set(2, SceneObject.new(empty))
  scene._scene.get(1).set(2, SceneObject.new("3", "b"))
  scene._scene.get(2).set(2, SceneObject.new("0", "c"))
  scene._scene.get(3).set(2, SceneObject.new("4", "e"))
  scene._scene.get(4).set(2, SceneObject.new(empty))
  scene._scene.get(0).set(3, SceneObject.new(empty))
  scene._scene.get(2).set(3, SceneObject.new("5", "d"))
  scene._scene.get(4).set(3, SceneObject.new(empty))
  scene._scene.get(1).set(4, SceneObject.new(empty))
  scene._scene.get(2).set(4, SceneObject.new("6", "g"))
  scene._scene.get(3).set(4, SceneObject.new(empty))
  
  assert_false(scene.isSquareEmpty(0, 0), "occurred when square specified should be blind.")
  assert_true(scene.isSquareEmpty(1, 0), "occurred when square specified should be empty.")
  assert_false(scene.isSquareEmpty(2, 0), "occurred when square specified should contain an item.")
end

################################################################################
unit_test "same_domain_space" do
  scene = Scene.new("", 4, 4, 3, 3, nil)
  scene_1 = Scene.new("", 3, 4, 3, 3, nil)
  scene_2 = Scene.new("", 4, 3, 3, 3, nil)
  scene_3 = Scene.new("", 4, 4, 4, 3, nil)
  scene_4 = Scene.new("", 4, 5, 3, 4, nil)
  scene_5 = Scene.new("", 4, 4, 3, 3, nil)
  
  assert_false(scene.sameDomainSpace(scene_1), "occurred when checking scene against scene whose width is different")
  assert_false(scene.sameDomainSpace(scene_2), "occurred when checking scene against scene whose height is different")
  assert_false(scene.sameDomainSpace(scene_3), "occurred when checking scene against scene whose min domain column is different")
  assert_false(scene.sameDomainSpace(scene_4), "occurred when checking scene against scene whose min domain row is different")
  assert_true(scene.sameDomainSpace(scene_5), "occurred when checking scene against scene whose dimensions are all the same")
end

################################################################################
unit_test "get_scene_specific_col_from_domain_specific_col" do
  
  # Note that the width and height are different so that the upper bound for 
  # width is too large for height; enables verification that the correct 
  # dimension is being used in the function.
  scene = Scene.new("", 5, 4, 2, 2, nil)
  
  # Scenario 1: col specified less than minimum col
  # Scenario 2: col specified greater than maximum col
  # Scenario 3: col specified minimum col
  # Scenario 4: col specified maximum col
  for scenario in 1..4
    scene_specific_col = scene.getSceneSpecificColFromDomainSpecificCol(
      scenario == 1 ? 1 : scenario == 2 ? 7 : scenario == 3 ? 2 : 6
    )

    expected_scene_specific_col = 
      (scenario.between?(1,2) ? nil : scenario == 3 ? 0 : 4)

    assert_equal(
      expected_scene_specific_col,
      scene_specific_col,
      "occurred in scenario " + scenario.to_s
    )
  end
end

################################################################################
unit_test "get_scene_specific_col_from_domain_specific_col" do
  
  # Note that the width and height are different so that the upper bound for 
  # height is too large for width; enables verification that the correct 
  # dimension is being used in the function.
  scene = Scene.new("", 5, 6, 2, 2, nil)
  
  # Scenario 1: row specified less than minimum row
  # Scenario 2: row specified greater than maximum row
  # Scenario 3: row specified minimum row
  # Scenario 4: row specified maximum row
  for scenario in 1..4
  scene_specific_col = scene.getSceneSpecificRowFromDomainSpecificRow(
    scenario == 1 ? 1 : scenario == 2 ? 8 : scenario == 3 ? 2 : 7
  )
  
  expected_scene_specific_row = 
    (scenario.between?(1,2) ? nil : scenario == 3 ? 0 : 5)
  
    assert_equal(
      expected_scene_specific_row,
      scene_specific_col,
      "occurred in scenario " + scenario.to_s
    )
  end
end