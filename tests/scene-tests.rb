# jchrest.lib.Scene tests

################################################################################
#Tests that a scene is entirely blind after construction.
unit_test "constructor" do
  scene = Scene.new("test", 2, 2, nil)
  for row in 0...scene.getHeight()
    for col in 0 ...scene.getWidth()
      object = scene.getSquareContents(col, row)
      assert_equal(Scene.getBlindSquareToken, object.getIdentifier, "occurred when checking the identifier of the item on col " + col.to_s + " and row " + row.to_s)
      assert_equal(Scene.getBlindSquareToken, object.getObjectClass, "occurred when checking the class of the item on col " + col.to_s + " and row " + row.to_s)
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
#    
# The scene created originally looks like the following ("x" represents blind
# squares):
#
#   |------|------|------|------|------|
# 2 |  a   |      |      |  b   |      |
#   |------|------|------|------|------|
# 1    x   |      |  c   |      |  x
#          |------|------|------|
# 0    x      x   | SELF |  x      x
#                 |------|
#      0      1       2     3      4
unit_test "add-item-to-square" do
  scene = Scene.new("test", 5, 3, nil)
  
  ######################
  ##### Sub-Test 1 #####
  ######################
  #
  # The Scene will currently be entirely blind at the moment so add items
  # accordingly.
  scene.addItemToSquare(2, 0, "0", Scene.getCreatorToken)
  scene.addItemToSquare(1, 1, nil, Scene.getEmptySquareToken)
  scene.addItemToSquare(2, 1, "1", "c")
  scene.addItemToSquare(3, 1, nil, Scene.getEmptySquareToken)
  scene.addItemToSquare(0, 2, "2", "a")
  scene.addItemToSquare(1, 2, nil, Scene.getEmptySquareToken)
  scene.addItemToSquare(2, 2, nil, Scene.getEmptySquareToken)
  scene.addItemToSquare(3, 2, "3", "b")
  scene.addItemToSquare(4, 2, nil, Scene.getEmptySquareToken)
  
  for row in 0..2
    for col in 0..4
      
      expected_object_identifier = Scene.getBlindSquareToken
      expected_object_class = Scene.getBlindSquareToken
      
      if col == 2 and row == 0 
        expected_object_identifier = "0"
        expected_object_class = Scene.getCreatorToken
      end
      
      if row == 1
        if col == 1 or col == 3
          expected_object_identifier = Scene.getEmptySquareToken
          expected_object_class = Scene.getEmptySquareToken
        elsif col == 2
          expected_object_identifier = "1"
          expected_object_class = "c"
        end
      end
      
      if row == 2
        if col == 1 or col == 2 or col == 4
          expected_object_identifier = Scene.getEmptySquareToken
          expected_object_class = Scene.getEmptySquareToken
        elsif col == 0
          expected_object_identifier = "2"
          expected_object_class = "a"
        else
          expected_object_identifier = "3"
          expected_object_class = "b"
        end
      end
      
      square_contents = scene.getSquareContents(col, row)
      assert_equal(expected_object_identifier, square_contents.getIdentifier(), "occurred when checking the identifier for the item on col " + col.to_s + " and row " + row.to_s + " in sub-test 1.")
      assert_equal(expected_object_class, square_contents.getObjectClass(), "occurred when checking the class of the item on col " + col.to_s + " and row " + row.to_s + " in sub-test 1.")
    end
  end
  
  ######################
  ##### Sub-Test 2 #####
  ######################
  
  scene.addItemToSquare(1, 1, "4", "e")
  
  for row in 0..2
    for col in 0..4
      
      expected_object_identifier = Scene.getBlindSquareToken
      expected_object_class = Scene.getBlindSquareToken
      
      if col == 2 and row == 0 
        expected_object_identifier = "0"
        expected_object_class = Scene.getCreatorToken
      end
      
      if row == 1
        if col == 1 
          expected_object_identifier = "4"
          expected_object_class = "e"
        elsif col == 3
          expected_object_identifier = Scene.getEmptySquareToken
          expected_object_class = Scene.getEmptySquareToken
        elsif col == 2
          expected_object_identifier = "1"
          expected_object_class = "c"
        end
      end
      
      if row == 2
        if col == 1 or col == 2 or col == 4
          expected_object_identifier = Scene.getEmptySquareToken
          expected_object_class = Scene.getEmptySquareToken
        elsif col == 0
          expected_object_identifier = "2"
          expected_object_class = "a"
        else
          expected_object_identifier = "3"
          expected_object_class = "b"
        end
      end
      
      square_contents = scene.getSquareContents(col, row)
      assert_equal(expected_object_identifier, square_contents.getIdentifier(), "occurred when checking the identifier for the item on col " + col.to_s + " and row " + row.to_s + " in sub-test 2.")
      assert_equal(expected_object_class, square_contents.getObjectClass(), "occurred when checking the class of the item on col " + col.to_s + " and row " + row.to_s + " in sub-test 2.")
    end
  end
  
  ######################
  ##### Sub-Test 3 #####
  ######################
  
  scene.addItemToSquare(1, 1, Scene.getEmptySquareToken, Scene.getEmptySquareToken)
  for row in 0..2
    for col in 0..4
      
      expected_object_identifier = Scene.getBlindSquareToken
      expected_object_class = Scene.getBlindSquareToken
      
      if col == 2 and row == 0 
        expected_object_identifier = "0"
        expected_object_class = Scene.getCreatorToken
      end
      
      if row == 1
        if col == 1 or col == 3
          expected_object_identifier = Scene.getEmptySquareToken
          expected_object_class = Scene.getEmptySquareToken
        elsif col == 2
          expected_object_identifier = "1"
          expected_object_class = "c"
        end
      end
      
      if row == 2
        if col == 1 or col == 2 or col == 4
          expected_object_identifier = Scene.getEmptySquareToken
          expected_object_class = Scene.getEmptySquareToken
        elsif col == 0
          expected_object_identifier = "2"
          expected_object_class = "a"
        else
          expected_object_identifier = "3"
          expected_object_class = "b"
        end
      end
      
      square_contents = scene.getSquareContents(col, row)
      assert_equal(expected_object_identifier, square_contents.getIdentifier(), "occurred when checking the identifier for the item on col " + col.to_s + " and row " + row.to_s + " in sub-test 3.")
      assert_equal(expected_object_class, square_contents.getObjectClass(), "occurred when checking the class of the item on col " + col.to_s + " and row " + row.to_s + " in sub-test 3.")
    end
  end
  
  ######################
  ##### Sub-Test 4 #####
  ######################
  
  scene.addItemToSquare(2, 1, "5", "e")
  for row in 0..2
    for col in 0..4
      
      expected_object_identifier = Scene.getBlindSquareToken
      expected_object_class = Scene.getBlindSquareToken
      
      if col == 2 and row == 0 
        expected_object_identifier = "0"
        expected_object_class = Scene.getCreatorToken
      end
      
      if row == 1
        if col == 1 or col == 3
          expected_object_identifier = Scene.getEmptySquareToken
          expected_object_class = Scene.getEmptySquareToken
        elsif col == 2
          expected_object_identifier = "5"
          expected_object_class = "e"
        end
      end
      
      if row == 2
        if col == 1 or col == 2 or col == 4
          expected_object_identifier = Scene.getEmptySquareToken
          expected_object_class = Scene.getEmptySquareToken
        elsif col == 0
          expected_object_identifier = "2"
          expected_object_class = "a"
        else
          expected_object_identifier = "3"
          expected_object_class = "b"
        end
      end
      
      square_contents = scene.getSquareContents(col, row)
      assert_equal(expected_object_identifier, square_contents.getIdentifier(), "occurred when checking the identifier for the item on col " + col.to_s + " and row " + row.to_s + " in sub-test 4.")
      assert_equal(expected_object_class, square_contents.getObjectClass(), "occurred when checking the class of the item on col " + col.to_s + " and row " + row.to_s + " in sub-test 4.")
    end
  end
end

################################################################################
unit_test "add-items-to-row" do
  scene = Scene.new("test", 5, 3, nil)
  blind = Scene.getBlindSquareToken
  empty = Scene.getEmptySquareToken
  
  row_0_items = ArrayList.new
  row_0_items.add(SceneObject.new("", blind))
  row_0_items.add(SceneObject.new("", blind))
  row_0_items.add(SceneObject.new("0", "d"))
  row_0_items.add(SceneObject.new("", blind))
  row_0_items.add(SceneObject.new("", blind))
  
  row_1_items = ArrayList.new
  row_1_items.add(SceneObject.new("", blind))
  row_1_items.add(SceneObject.new("", empty))
  row_1_items.add(SceneObject.new("1", "c"))
  row_1_items.add(SceneObject.new("", empty))
  row_1_items.add(SceneObject.new("", blind))
  
  row_2_items = ArrayList.new
  row_2_items.add(SceneObject.new("2", "a"))
  row_2_items.add(SceneObject.new("", empty))
  row_2_items.add(SceneObject.new("", empty))
  row_2_items.add(SceneObject.new("3", "b"))
  row_2_items.add(SceneObject.new("", empty))

  scene.addItemsToRow(0, row_0_items)
  scene.addItemsToRow(1, row_1_items)
  scene.addItemsToRow(2, row_2_items)
  
  for row in 0..2
    for col in 0..4
      
      expected_identifier = blind
      expected_object_class = blind
      
      if row == 0 
        if col == 2
          expected_identifier = "0"
          expected_object_class = "d"
        end
      end
      
      if row == 1
        if col == 1 or col == 3
          expected_identifier = empty
          expected_object_class = empty
        elsif col == 2
          expected_identifier = "1"
          expected_object_class = "c"
        end
      end
      
      if row == 2
        if col == 1 or col == 2 or col == 4
          expected_identifier = empty
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
      assert_equal(expected_identifier, squareContents.getIdentifier(), "occurred when checking the object identifier of the object on col " + col.to_s + " and row " + row.to_s)
      assert_equal(expected_object_class, squareContents.getObjectClass(), "occurred when checking the object class of the object on col " + col.to_s + " and row " + row.to_s)
    end
  end
end

################################################################################
unit_test "get-square-contents" do
  scene = Scene.new("test", 2, 2, nil)
  scene.addItemToSquare(1, 0, "0", "a")
  scene.addItemToSquare(1, 1, "", Scene.getEmptySquareToken())
  
  for row in 0..1
    for col in 0..1
      contents_of_square = scene.getSquareContents(col, row)
      expected_object_identifier = Scene.getBlindSquareToken()
      expected_object_class = Scene.getBlindSquareToken()
      
      if(col == 1)
        if(row == 0)
          expected_object_identifier = "0"
          expected_object_class = "a"
        elsif(row == 1)
          expected_object_identifier = Scene.getEmptySquareToken()
          expected_object_class = Scene.getEmptySquareToken()
        end
      end
      
      assert_equal(expected_object_identifier, contents_of_square.getIdentifier(), "occurred when checking the identifier of the item on col " + col.to_s + " and row " + row.to_s)
      assert_equal(expected_object_class, contents_of_square.getObjectClass(), "occurred when checking the class of the item on col " + col.to_s + " and row " + row.to_s)
    end
  end
  
  assert_equal(nil, scene.getSquareContents(scene.getWidth(), 0), "occured when checking what's returned when specifying a col that's out of scope")
  assert_equal(nil, scene.getSquareContents(0, scene.getHeight()), "occured when checking what's returned when specifying a row that's out of scope")
end

################################################################################
#Tests that all variations of parameters passed to the 
#"Scene.getSquareContentsAsListPattern()" method return results as expected.
unit_test "get-square-contents-as-list-pattern" do
  scene = Scene.new("test", 3, 3, nil)
  scene.addItemToSquare(1, 0, "1", "a")
  scene.addItemToSquare(1, 1, "0", Scene.getCreatorToken())
  scene.addItemToSquare(2, 2, "", Scene.getEmptySquareToken())
  
  for row in 0..2
    for col in 0..2
      
      expected_object_identifier = Scene.getBlindSquareToken()
      expected_object_class = Scene.getBlindSquareToken()
      
      if(col == 1)
        if(row == 0)
          expected_object_identifier = "1"
          expected_object_class = "a"
        elsif(row == 1)
          expected_object_identifier = "0"
          expected_object_class = Scene.getCreatorToken()
        end
      elsif(col == 2)
        if(row == 2)
          expected_object_identifier = Scene.getEmptySquareToken()
          expected_object_class = Scene.getEmptySquareToken()
        end
      end
      
      expected_list_pattern_objects_identified_by_id = ListPattern.new
      expected_list_pattern_objects_identified_by_id.add(ItemSquarePattern.new(expected_object_identifier.to_s, col, row))
      
      expected_list_pattern_objects_identified_by_class = ListPattern.new
      expected_list_pattern_objects_identified_by_class.add(ItemSquarePattern.new(expected_object_class.to_s, col, row))
      
      objects_identified_by_id = scene.getSquareContentsAsListPattern(col, row, false)
      objects_identified_by_class = scene.getSquareContentsAsListPattern(col, row, true)
      
      assert_equal(expected_list_pattern_objects_identified_by_id.toString(), objects_identified_by_id.toString(), "occurred when checking col " + col.to_s + " and row " + row.to_s + " and object identifiers are requested in the list pattern returned")
      assert_equal(expected_list_pattern_objects_identified_by_class.toString(), objects_identified_by_class.toString(), "occurred when checking col " + col.to_s + " and row " + row.to_s + " and object classes are requested in the list pattern returned")
    end
  end
end

################################################################################
#Tests whether all permutations of possible parameters to 
#Scene.getAsListPattern return the correct output when given a scene containing 
#blind, empty and non-empty squares.  The scene used is depicted 
#visually below ("x" represents a blind square in the scene):
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
unit_test "get-as-list-pattern" do
  scene = Scene.new("test", 5, 3, nil)
  
  scene.addItemToSquare(2, 0, "0", Scene.getCreatorToken)
  scene.addItemToSquare(1, 1, "", Scene.getEmptySquareToken)
  scene.addItemToSquare(2, 1, "1", "c")
  scene.addItemToSquare(3, 1, "", Scene.getEmptySquareToken)
  scene.addItemToSquare(0, 2, "2", "a")
  scene.addItemToSquare(1, 2, "", Scene.getEmptySquareToken)
  scene.addItemToSquare(2, 2, "", Scene.getEmptySquareToken)
  scene.addItemToSquare(3, 2, "3", "b")
  scene.addItemToSquare(4, 2, "", Scene.getEmptySquareToken)
  
  blind = Scene.getBlindSquareToken
  empty = Scene.getEmptySquareToken
  
  expected_list_pattern_objects_identified_by_id = ListPattern.new
  expected_list_pattern_objects_identified_by_id.add(ItemSquarePattern.new(blind, 0, 0))
  expected_list_pattern_objects_identified_by_id.add(ItemSquarePattern.new(blind, 1, 0))
  expected_list_pattern_objects_identified_by_id.add(ItemSquarePattern.new("0", 2, 0))
  expected_list_pattern_objects_identified_by_id.add(ItemSquarePattern.new(blind, 3, 0))
  expected_list_pattern_objects_identified_by_id.add(ItemSquarePattern.new(blind, 4, 0))
  expected_list_pattern_objects_identified_by_id.add(ItemSquarePattern.new(blind, 0, 1))
  expected_list_pattern_objects_identified_by_id.add(ItemSquarePattern.new(empty, 1, 1))
  expected_list_pattern_objects_identified_by_id.add(ItemSquarePattern.new("1", 2, 1))
  expected_list_pattern_objects_identified_by_id.add(ItemSquarePattern.new(empty, 3, 1))
  expected_list_pattern_objects_identified_by_id.add(ItemSquarePattern.new(blind, 4, 1))
  expected_list_pattern_objects_identified_by_id.add(ItemSquarePattern.new("2".to_s, 0, 2))
  expected_list_pattern_objects_identified_by_id.add(ItemSquarePattern.new(empty, 1, 2))
  expected_list_pattern_objects_identified_by_id.add(ItemSquarePattern.new(empty, 2, 2))
  expected_list_pattern_objects_identified_by_id.add(ItemSquarePattern.new("3", 3, 2))
  expected_list_pattern_objects_identified_by_id.add(ItemSquarePattern.new(empty, 4, 2))
  
  expected_list_pattern_objects_identified_by_class = ListPattern.new
  expected_list_pattern_objects_identified_by_class.add(ItemSquarePattern.new(blind, 0, 0))
  expected_list_pattern_objects_identified_by_class.add(ItemSquarePattern.new(blind, 1, 0))
  expected_list_pattern_objects_identified_by_class.add(ItemSquarePattern.new(Scene.getCreatorToken, 2, 0))
  expected_list_pattern_objects_identified_by_class.add(ItemSquarePattern.new(blind, 3, 0))
  expected_list_pattern_objects_identified_by_class.add(ItemSquarePattern.new(blind, 4, 0))
  expected_list_pattern_objects_identified_by_class.add(ItemSquarePattern.new(blind, 0, 1))
  expected_list_pattern_objects_identified_by_class.add(ItemSquarePattern.new(empty, 1, 1))
  expected_list_pattern_objects_identified_by_class.add(ItemSquarePattern.new("c", 2, 1))
  expected_list_pattern_objects_identified_by_class.add(ItemSquarePattern.new(empty, 3, 1))
  expected_list_pattern_objects_identified_by_class.add(ItemSquarePattern.new(blind, 4, 1))
  expected_list_pattern_objects_identified_by_class.add(ItemSquarePattern.new("a", 0, 2))
  expected_list_pattern_objects_identified_by_class.add(ItemSquarePattern.new(empty, 1, 2))
  expected_list_pattern_objects_identified_by_class.add(ItemSquarePattern.new(empty, 2, 2))
  expected_list_pattern_objects_identified_by_class.add(ItemSquarePattern.new("b", 3, 2))
  expected_list_pattern_objects_identified_by_class.add(ItemSquarePattern.new(empty, 4, 2))
  
  assert_equal(expected_list_pattern_objects_identified_by_id, scene.getAsListPattern(false), "occurred when checking if list pattern returned is correct when objects are identified using unique identifiers")
  assert_equal(expected_list_pattern_objects_identified_by_class, scene.getAsListPattern(true), "occurred when checking if list pattern returned is correct when objects are identified using their class")
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
  
  ######################
  ##### Sub-Test 1 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 1, 1, nil)
  scene2 = Scene.new("test-scene-2", 1, 2, nil)
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
  
  scene1 = Scene.new("test-scene-1", 2, 2, nil)
  scene2 = Scene.new("test-scene-2", 2, 2, nil)
  
  scene1.addItemToSquare(0, 0, "0", "a")
  
  scene2.addItemToSquare(0, 0, "0", "a")
  scene2.addItemToSquare(1, 0, "0", "b")
  
  assert_equal(0, scene1.computeErrorsOfCommission(scene2), "occurred in sub-test 2.")
  
  ######################
  ##### Sub-Test 3 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 5, 3, nil)
  scene2 = Scene.new("test-scene-2", 5, 3, nil)
  
  blind = Scene.getBlindSquareToken
  empty = Scene.getEmptySquareToken
  
  #Scene 1 should have 2 more objects than scene 2.
  scene_1_row_0_items = ArrayList.new
  scene_1_row_0_items.add(SceneObject.new("", blind))
  scene_1_row_0_items.add(SceneObject.new("", blind))
  scene_1_row_0_items.add(SceneObject.new("0", "d"))
  scene_1_row_0_items.add(SceneObject.new("", blind))
  scene_1_row_0_items.add(SceneObject.new("", blind))
  scene_1_row_1_items = ArrayList.new
  scene_1_row_1_items.add(SceneObject.new("", blind))
  scene_1_row_1_items.add(SceneObject.new("", empty))
  scene_1_row_1_items.add(SceneObject.new("1", "c"))
  scene_1_row_1_items.add(SceneObject.new("", empty))
  scene_1_row_1_items.add(SceneObject.new("", blind))
  scene_1_row_2_items = ArrayList.new
  scene_1_row_2_items.add(SceneObject.new("2", "a"))
  scene_1_row_2_items.add(SceneObject.new("", empty))
  scene_1_row_2_items.add(SceneObject.new("3", "e"))
  scene_1_row_2_items.add(SceneObject.new("4", "b"))
  scene_1_row_2_items.add(SceneObject.new("5", "f"))
  
  scene_2_row_0_items = ArrayList.new
  scene_2_row_0_items.add(SceneObject.new("", blind))
  scene_2_row_0_items.add(SceneObject.new("", blind))
  scene_2_row_0_items.add(SceneObject.new("0", "d"))
  scene_2_row_0_items.add(SceneObject.new("", blind))
  scene_2_row_0_items.add(SceneObject.new("", blind))
  scene_2_row_1_items = ArrayList.new
  scene_2_row_1_items.add(SceneObject.new("", blind))
  scene_2_row_1_items.add(SceneObject.new("", empty))
  scene_2_row_1_items.add(SceneObject.new("1", "c"))
  scene_2_row_1_items.add(SceneObject.new("", empty))
  scene_2_row_1_items.add(SceneObject.new("", blind))
  scene_2_row_2_items = ArrayList.new
  scene_2_row_2_items.add(SceneObject.new("2", "a"))
  scene_2_row_2_items.add(SceneObject.new("", empty))
  scene_2_row_2_items.add(SceneObject.new("", empty))
  scene_2_row_2_items.add(SceneObject.new("3", "b"))
  scene_2_row_2_items.add(SceneObject.new("", empty))
  
  scene1.addItemsToRow(0, scene_1_row_0_items)
  scene1.addItemsToRow(1, scene_1_row_1_items)
  scene1.addItemsToRow(2, scene_1_row_2_items)
  
  scene2.addItemsToRow(0, scene_2_row_0_items)
  scene2.addItemsToRow(1, scene_2_row_1_items)
  scene2.addItemsToRow(2, scene_2_row_2_items)
  
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
  
  ######################
  ##### Sub-Test 1 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 1, 1, nil)
  scene2 = Scene.new("test-scene-2", 1, 2, nil)
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
  
  scene1 = Scene.new("test-scene-1", 2, 2, nil)
  scene2 = Scene.new("test-scene-2", 2, 2, nil)
  
  scene1.addItemToSquare(0, 0, "0", "a")
  scene1.addItemToSquare(1, 0, "0", "b")
  
  scene2.addItemToSquare(0, 0, "0", "a")
  
  assert_equal(0, scene1.computeErrorsOfOmission(scene2), "occurred in sub-test 2.")
  
  ######################
  ##### Sub-Test 3 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 5, 3, nil)
  scene2 = Scene.new("test-scene-2", 5, 3, nil)
  
  blind = Scene.getBlindSquareToken
  empty = Scene.getEmptySquareToken
  
  #Scene 1 should have 2 fewer objects than scene 2.
  scene_1_row_0_items = ArrayList.new
  scene_1_row_0_items.add(SceneObject.new("", blind))
  scene_1_row_0_items.add(SceneObject.new("", blind))
  scene_1_row_0_items.add(SceneObject.new("0", "d"))
  scene_1_row_0_items.add(SceneObject.new("", blind))
  scene_1_row_0_items.add(SceneObject.new("", blind))
  scene_1_row_1_items = ArrayList.new
  scene_1_row_1_items.add(SceneObject.new("", blind))
  scene_1_row_1_items.add(SceneObject.new("", empty))
  scene_1_row_1_items.add(SceneObject.new("1", "c"))
  scene_1_row_1_items.add(SceneObject.new("", empty))
  scene_1_row_1_items.add(SceneObject.new("", blind))
  scene_1_row_2_items = ArrayList.new
  scene_1_row_2_items.add(SceneObject.new("2", "a"))
  scene_1_row_2_items.add(SceneObject.new("", empty))
  scene_1_row_2_items.add(SceneObject.new("", empty))
  scene_1_row_2_items.add(SceneObject.new("3", "b"))
  scene_1_row_2_items.add(SceneObject.new("", empty))
  
  scene_2_row_0_items = ArrayList.new
  scene_2_row_0_items.add(SceneObject.new("", blind))
  scene_2_row_0_items.add(SceneObject.new("", blind))
  scene_2_row_0_items.add(SceneObject.new("0", "d"))
  scene_2_row_0_items.add(SceneObject.new("", blind))
  scene_2_row_0_items.add(SceneObject.new("", blind))
  scene_2_row_1_items = ArrayList.new
  scene_2_row_1_items.add(SceneObject.new("", blind))
  scene_2_row_1_items.add(SceneObject.new("", empty))
  scene_2_row_1_items.add(SceneObject.new("1", "c"))
  scene_2_row_1_items.add(SceneObject.new("", empty))
  scene_2_row_1_items.add(SceneObject.new("", blind))
  scene_2_row_2_items = ArrayList.new
  scene_2_row_2_items.add(SceneObject.new("2", "a"))
  scene_2_row_2_items.add(SceneObject.new("", empty))
  scene_2_row_2_items.add(SceneObject.new("3", "e"))
  scene_2_row_2_items.add(SceneObject.new("4", "b"))
  scene_2_row_2_items.add(SceneObject.new("5", "f"))
  
  scene1.addItemsToRow(0, scene_1_row_0_items)
  scene1.addItemsToRow(1, scene_1_row_1_items)
  scene1.addItemsToRow(2, scene_1_row_2_items)
  
  scene2.addItemsToRow(0, scene_2_row_0_items)
  scene2.addItemsToRow(1, scene_2_row_1_items)
  scene2.addItemsToRow(2, scene_2_row_2_items)
  
  assert_equal(2, scene1.computeErrorsOfOmission(scene2), "occurred in sub-test 3.")
end

################################################################################
#Sub-tests:
# 1) 0 is returned if either scene in calculation is empty.
# 2) Error is thrown if size of scenes to be used in calculation are not equal.
# 3) The correct value is returned when objects in a scene are represented using
#    their object class.
# 4) The correct value is returned when objects in a scene are represented using
#    their unique identifiers.
unit_test "compute-precision" do
  
  ######################
  ##### Sub-Test 1 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 5, 3, nil)
  
  scene2 = Scene.new("test-scene-2", 5, 3, nil)
  scene2.addItemToSquare(0, 0, "0", "a")
  
  scene3 = Scene.new("test-scene-3", 5, 3, nil)
  
  #Note that object representation has no bearing for this sub-test so false is
  #passed as the parameter concerning this feature in the computePrecision 
  #function.
  assert_equal(0.0, scene1.computePrecision(scene2, false), "occurred in sub-test 1 when base scene is empty.")
  assert_equal(0.0, scene2.computePrecision(scene3, false), "occurred in sub-test 1 when scene compared against is empty.")
  
  ######################
  ##### Sub-Test 2 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 1, 1, nil)
  scene2 = Scene.new("test-scene-2", 1, 2, nil)
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
  
  scene1 = Scene.new("test-scene-1", 5, 3, nil)
  scene2 = Scene.new("test-scene-2", 5, 3, nil)
  
  blind = Scene.getBlindSquareToken
  empty = Scene.getEmptySquareToken
  
  #Note that objects in both scenes are in the correct locations whilst their
  #object identifiers differ but their classes match.
  scene_1_row_0_items = ArrayList.new
  scene_1_row_0_items.add(SceneObject.new("", blind))
  scene_1_row_0_items.add(SceneObject.new("", blind))
  scene_1_row_0_items.add(SceneObject.new("1", "a"))
  scene_1_row_0_items.add(SceneObject.new("2", "d"))
  scene_1_row_0_items.add(SceneObject.new("", blind))
  scene_1_row_1_items = ArrayList.new
  scene_1_row_1_items.add(SceneObject.new("", blind))
  scene_1_row_1_items.add(SceneObject.new("", empty))
  scene_1_row_1_items.add(SceneObject.new("0", "b"))
  scene_1_row_1_items.add(SceneObject.new("3", "e"))
  scene_1_row_1_items.add(SceneObject.new("", blind))
  scene_1_row_2_items = ArrayList.new
  scene_1_row_2_items.add(SceneObject.new("4", "c"))
  scene_1_row_2_items.add(SceneObject.new("", empty))
  scene_1_row_2_items.add(SceneObject.new("", empty))
  scene_1_row_2_items.add(SceneObject.new("", empty))
  scene_1_row_2_items.add(SceneObject.new("5", "f"))
  
  scene_2_row_0_items = ArrayList.new
  scene_2_row_0_items.add(SceneObject.new("", blind))
  scene_2_row_0_items.add(SceneObject.new("", blind))
  scene_2_row_0_items.add(SceneObject.new("0", "a"))
  scene_2_row_0_items.add(SceneObject.new("1", "d"))
  scene_2_row_0_items.add(SceneObject.new("", blind))
  scene_2_row_1_items = ArrayList.new
  scene_2_row_1_items.add(SceneObject.new("", blind))
  scene_2_row_1_items.add(SceneObject.new("", empty))
  scene_2_row_1_items.add(SceneObject.new("2", "b"))
  scene_2_row_1_items.add(SceneObject.new("3", "e"))
  scene_2_row_1_items.add(SceneObject.new("", blind))
  scene_2_row_2_items = ArrayList.new
  scene_2_row_2_items.add(SceneObject.new("4", "c"))
  scene_2_row_2_items.add(SceneObject.new("", empty))
  scene_2_row_2_items.add(SceneObject.new("", empty))
  scene_2_row_2_items.add(SceneObject.new("", empty))
  scene_2_row_2_items.add(SceneObject.new("5", "f"))
  
  scene1.addItemsToRow(0, scene_1_row_0_items)
  scene1.addItemsToRow(1, scene_1_row_1_items)
  scene1.addItemsToRow(2, scene_1_row_2_items)
  
  scene2.addItemsToRow(0, scene_2_row_0_items)
  scene2.addItemsToRow(1, scene_2_row_1_items)
  scene2.addItemsToRow(2, scene_2_row_2_items)
  
  assert_equal(1, scene1.computePrecision(scene2, true), "occurred in sub-test 3.")
  
  ######################
  ##### Sub-Test 4 #####
  ######################
  
  assert_equal(0.5, scene1.computePrecision(scene2, false), "occurred in sub-test 4.")
end

################################################################################
#Sub-tests:
# 1) 0 is returned if either scene in calculation is empty.
# 2) Error is thrown if size of scenes to be used in calculation are not equal.
# 3) The correct value is returned when objects in a scene are represented using
#    their object class.
# 4) The correct value is returned when objects in a scene are represented using
#    their unique identifiers.
unit_test "compute-recall" do 
  
  ######################
  ##### Sub-Test 1 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 5, 3, nil)
  
  scene2 = Scene.new("test-scene-2", 5, 3, nil)
  scene2.addItemToSquare(0, 0, "0", "a")
  
  scene3 = Scene.new("test-scene-3", 5, 3, nil)
  
  #Note that object representation has no bearing for this sub-test so false is
  #passed as the parameter concerning this feature in the computePrecision 
  #function.
  assert_equal(0.0, scene1.computeRecall(scene2, false), "occurred in sub-test 1 when base scene is empty.")
  assert_equal(0.0, scene2.computeRecall(scene3, false), "occurred in sub-test 1 when scene compared against is empty.")
  
  ######################
  ##### Sub-Test 2 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 1, 1, nil)
  scene2 = Scene.new("test-scene-2", 1, 2, nil)
  error_thrown = false
  
  begin
    #Note that object representation has no bearing for this sub-test so false 
    #is passed as the parameter concerning this feature in the computeRecall 
    #function.
    scene1.computeRecall(scene2, false)
  rescue 
    #Swallow the error output to retain pretty test output but set error_thrown 
    #flag to true to indicate that an error was actually thrown.
    error_thrown = true
  end

  assert_true(error_thrown, "occured in sub-test 2.")
  
  ######################
  ##### Sub-Test 3 #####
  ######################
  
  scene1 = Scene.new("test-scene-1", 5, 3, nil)
  scene2 = Scene.new("test-scene-2", 5, 3, nil)
  
  blind = Scene.getBlindSquareToken
  empty = Scene.getEmptySquareToken
  
  #Note that no object's location is the same between scenes and whilst the
  #object classes specified are the same in each scene, object identities differ.
  scene_1_row_0_items = ArrayList.new
  scene_1_row_0_items.add(SceneObject.new("", blind))
  scene_1_row_0_items.add(SceneObject.new("1", "a"))
  scene_1_row_0_items.add(SceneObject.new("2", "d"))
  scene_1_row_0_items.add(SceneObject.new("", empty))
  scene_1_row_0_items.add(SceneObject.new("", blind))
  scene_1_row_1_items = ArrayList.new
  scene_1_row_1_items.add(SceneObject.new("", blind))
  scene_1_row_1_items.add(SceneObject.new("0", "b"))
  scene_1_row_1_items.add(SceneObject.new("6", "e"))
  scene_1_row_1_items.add(SceneObject.new("", blind))
  scene_1_row_1_items.add(SceneObject.new("", blind))
  scene_1_row_2_items = ArrayList.new
  scene_1_row_2_items.add(SceneObject.new("", empty))
  scene_1_row_2_items.add(SceneObject.new("7", "c"))
  scene_1_row_2_items.add(SceneObject.new("", empty))
  scene_1_row_2_items.add(SceneObject.new("8", "f"))
  scene_1_row_2_items.add(SceneObject.new("", empty))
  
  scene_2_row_0_items = ArrayList.new
  scene_2_row_0_items.add(SceneObject.new("", blind))
  scene_2_row_0_items.add(SceneObject.new("", blind))
  scene_2_row_0_items.add(SceneObject.new("0", "a"))
  scene_2_row_0_items.add(SceneObject.new("1", "d"))
  scene_2_row_0_items.add(SceneObject.new("", blind))
  scene_2_row_1_items = ArrayList.new
  scene_2_row_1_items.add(SceneObject.new("", blind))
  scene_2_row_1_items.add(SceneObject.new("", empty))
  scene_2_row_1_items.add(SceneObject.new("2", "b"))
  scene_2_row_1_items.add(SceneObject.new("3", "e"))
  scene_2_row_1_items.add(SceneObject.new("", blind))
  scene_2_row_2_items = ArrayList.new
  scene_2_row_2_items.add(SceneObject.new("4", "c"))
  scene_2_row_2_items.add(SceneObject.new("", empty))
  scene_2_row_2_items.add(SceneObject.new("", empty))
  scene_2_row_2_items.add(SceneObject.new("", empty))
  scene_2_row_2_items.add(SceneObject.new("5", "f"))
  
  scene1.addItemsToRow(0, scene_1_row_0_items)
  scene1.addItemsToRow(1, scene_1_row_1_items)
  scene1.addItemsToRow(2, scene_1_row_2_items)
  
  scene2.addItemsToRow(0, scene_2_row_0_items)
  scene2.addItemsToRow(1, scene_2_row_1_items)
  scene2.addItemsToRow(2, scene_2_row_2_items)
  
  assert_equal(1.0, scene1.computeRecall(scene2, true), "occurred in sub-test 3.")
  
  ######################
  ##### Sub-Test 4 #####
  ######################
  
  assert_equal(0.5, scene1.computeRecall(scene2, false), "occurred in sub-test 4.")
end

# Blind square identifier not publicly settable so not possible to test 
# "getBlindSquareToken" accurately.

# Empty square identifier not publicly settable so not possible to test 
# "getEmptySquareToken" accurately.

# Self identifier not publicly settable so not possible to test 
# "getSelfIdentifier" accurately.

################################################################################
unit_test "get_height" do
  height = 3
  scene = Scene.new("test-scene", 5, height, nil)
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
  scene = Scene.new("test", 5, 5, nil)

  row_0_items = ArrayList.new
  row_0_items.add(SceneObject.new("", blind))
  row_0_items.add(SceneObject.new("", empty))
  row_0_items.add(SceneObject.new("1", "f"))
  row_0_items.add(SceneObject.new("", empty))
  row_0_items.add(SceneObject.new("", blind))
  
  row_1_items = ArrayList.new
  row_1_items.add(SceneObject.new("", empty))
  row_1_items.add(SceneObject.new("", blind))
  row_1_items.add(SceneObject.new("2", "a"))
  row_1_items.add(SceneObject.new("", blind))
  row_1_items.add(SceneObject.new("", empty))
  
  row_2_items = ArrayList.new
  row_2_items.add(SceneObject.new("", empty))
  row_2_items.add(SceneObject.new("3", "b"))
  row_2_items.add(SceneObject.new("0", Scene.getCreatorToken()))
  row_2_items.add(SceneObject.new("4", "e"))
  row_2_items.add(SceneObject.new("", empty))
  
  row_3_items = ArrayList.new
  row_3_items.add(SceneObject.new("", empty))
  row_3_items.add(SceneObject.new("", blind))
  row_3_items.add(SceneObject.new("5", "d"))
  row_3_items.add(SceneObject.new("", blind))
  row_3_items.add(SceneObject.new("", empty))
  
  row_4_items = ArrayList.new
  row_4_items.add(SceneObject.new("", blind))
  row_4_items.add(SceneObject.new("", empty))
  row_4_items.add(SceneObject.new("6", "g"))
  row_4_items.add(SceneObject.new("", empty))
  row_4_items.add(SceneObject.new("", blind))
  
  scene.addItemsToRow(0, row_0_items)
  scene.addItemsToRow(1, row_1_items)
  scene.addItemsToRow(2, row_2_items)
  scene.addItemsToRow(3, row_3_items)
  scene.addItemsToRow(4, row_4_items)
  
  expected_items_identified_by_object_class = ListPattern.new
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(blind, 0, 0))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(empty, 1, 0))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new("f", 2, 0))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(empty, 3, 0))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(blind, 4, 0))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(empty, 0, 1))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(blind, 1, 1))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new("a", 2, 1))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(blind, 3, 1))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(empty, 4, 1))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(empty, 0, 2))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new("b", 1, 2))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(Scene.getCreatorToken(), 2, 2))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new("e", 3, 2))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(empty, 4, 2))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(empty, 0, 3))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(blind, 1, 3))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new("d", 2, 3))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(blind, 3, 3))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(empty, 4, 3))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(blind, 0, 4))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(empty, 1, 4))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new("g", 2, 4))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(empty, 3, 4))
  expected_items_identified_by_object_class.add(ItemSquarePattern.new(blind, 4, 4))
  
  expected_items_identified_by_object_id = ListPattern.new
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(blind, 0, 0))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(empty, 1, 0))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new("1", 2, 0))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(empty, 3, 0))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(blind, 4, 0))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(empty, 0, 1))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(blind, 1, 1))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new("2", 2, 1))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(blind, 3, 1))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(empty, 4, 1))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(empty, 0, 2))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new("3", 1, 2))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new("0", 2, 2))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new("4", 3, 2))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(empty, 4, 2))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(empty, 0, 3))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(blind, 1, 3))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new("5", 2, 3))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(blind, 3, 3))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(empty, 4, 3))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(blind, 0, 4))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(empty, 1, 4))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new("6", 2, 4))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(empty, 3, 4))
  expected_items_identified_by_object_id.add(ItemSquarePattern.new(blind, 4, 4))
  
  assert_equal(expected_items_identified_by_object_class, scene.getItemsInScopeAsListPattern(2, 2, 2, true), "occurred when items should be identified by their class.")
  assert_equal(expected_items_identified_by_object_id, scene.getItemsInScopeAsListPattern(2, 2, 2, false), "occurred when items should be identified by their id.")
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
  blind = Scene.getBlindSquareToken
  empty = Scene.getEmptySquareToken
  
  scene_with_creator = Scene.new("", 5, 5, nil)
  scene_with_creator.addItemToSquare(1, 0, "", empty);
  scene_with_creator.addItemToSquare(2, 0, "1", "f");
  scene_with_creator.addItemToSquare(3, 0, "", empty);
  scene_with_creator.addItemToSquare(0, 1, "", empty);
  scene_with_creator.addItemToSquare(2, 1, "2", "a");
  scene_with_creator.addItemToSquare(4, 1, "", empty);
  scene_with_creator.addItemToSquare(0, 2, "", empty);
  scene_with_creator.addItemToSquare(1, 2, "3", "b");
  scene_with_creator.addItemToSquare(2, 2, "0", Scene.getCreatorToken());
  scene_with_creator.addItemToSquare(3, 2, "4", "e");
  scene_with_creator.addItemToSquare(4, 2, "", empty);
  scene_with_creator.addItemToSquare(0, 3, "", empty);
  scene_with_creator.addItemToSquare(2, 3, "5", "d");
  scene_with_creator.addItemToSquare(4, 3, "", empty);
  scene_with_creator.addItemToSquare(1, 4, "", empty);
  scene_with_creator.addItemToSquare(2, 4, "6", "g");
  scene_with_creator.addItemToSquare(3, 4, "", empty);
  
  scene_without_creator = Scene.new("", 5, 5, nil)
  scene_without_creator.addItemToSquare(1, 0, "", empty);
  scene_without_creator.addItemToSquare(2, 0, "1", "f");
  scene_without_creator.addItemToSquare(3, 0, "", empty);
  scene_without_creator.addItemToSquare(0, 1, "", empty);
  scene_without_creator.addItemToSquare(2, 1, "2", "a");
  scene_without_creator.addItemToSquare(4, 1, "", empty);
  scene_without_creator.addItemToSquare(0, 2, "", empty);
  scene_without_creator.addItemToSquare(1, 2, "3", "b");
  scene_without_creator.addItemToSquare(2, 2, "0", "c");
  scene_without_creator.addItemToSquare(3, 2, "4", "e");
  scene_without_creator.addItemToSquare(4, 2, "", empty);
  scene_without_creator.addItemToSquare(0, 3, "", empty);
  scene_without_creator.addItemToSquare(2, 3, "5", "d");
  scene_without_creator.addItemToSquare(4, 3, "", empty);
  scene_without_creator.addItemToSquare(1, 4, "", empty);
  scene_without_creator.addItemToSquare(2, 4, "6", "g");
  scene_without_creator.addItemToSquare(3, 4, "", empty);
  
  assert_equal(Square.new(2, 2).toString(), scene_with_creator.getLocationOfCreator().toString(), "occurred when scene creator is present in scene.")
  assert_equal(nil, scene_without_creator.getLocationOfCreator(), "occurred when scene creator is not present in scene.")
end

################################################################################
unit_test "get_name" do
  scene = Scene.new("", 5, 5, nil)
  assert_equal("", scene.getName(), "occurred when scene name should be empty.")
  
  scene_name = "test"
  scene = Scene.new(scene_name, 5, 5, nil)
  assert_equal(scene_name, scene.getName(), "occurred when scene name should not be empty.")
end

################################################################################
unit_test "get_width" do
  width = 5
  scene = Scene.new("", 5, width, nil)
  assert_equal(width, scene.getWidth())
end

################################################################################
unit_test "is_square_blind" do
  empty = Scene.getEmptySquareToken
  
  scene = Scene.new("", 5, 5, nil)
  scene.addItemToSquare(1, 0, "", empty);
  scene.addItemToSquare(2, 0, "1", "f");
  scene.addItemToSquare(3, 0, "", empty);
  scene.addItemToSquare(0, 1, "", empty);
  scene.addItemToSquare(2, 1, "2", "a");
  scene.addItemToSquare(4, 1, "", empty);
  scene.addItemToSquare(0, 2, "", empty);
  scene.addItemToSquare(1, 2, "3", "b");
  scene.addItemToSquare(2, 2, "0", "c");
  scene.addItemToSquare(3, 2, "4", "e");
  scene.addItemToSquare(4, 2, "", empty);
  scene.addItemToSquare(0, 3, "", empty);
  scene.addItemToSquare(2, 3, "5", "d");
  scene.addItemToSquare(4, 3, "", empty);
  scene.addItemToSquare(1, 4, "", empty);
  scene.addItemToSquare(2, 4, "6", "g");
  scene.addItemToSquare(3, 4, "", empty);
  
  assert_true(scene.isSquareBlind(0, 0), "occurred when square specified should be blind.")
  assert_false(scene.isSquareBlind(1, 0), "occurred when square specified should be empty.")
  assert_false(scene.isSquareBlind(2, 0), "occurred when square specified should contain an item.")
end

################################################################################
unit_test "is_square_empty" do
  empty = Scene.getEmptySquareToken
  
  scene = Scene.new("", 5, 5, nil)
  scene.addItemToSquare(1, 0, "", empty);
  scene.addItemToSquare(2, 0, "1", "f");
  scene.addItemToSquare(3, 0, "", empty);
  scene.addItemToSquare(0, 1, "", empty);
  scene.addItemToSquare(2, 1, "2", "a");
  scene.addItemToSquare(4, 1, "", empty);
  scene.addItemToSquare(0, 2, "", empty);
  scene.addItemToSquare(1, 2, "3", "b");
  scene.addItemToSquare(2, 2, "0", "c");
  scene.addItemToSquare(3, 2, "4", "e");
  scene.addItemToSquare(4, 2, "", empty);
  scene.addItemToSquare(0, 3, "", empty);
  scene.addItemToSquare(2, 3, "5", "d");
  scene.addItemToSquare(4, 3, "", empty);
  scene.addItemToSquare(1, 4, "", empty);
  scene.addItemToSquare(2, 4, "6", "g");
  scene.addItemToSquare(3, 4, "", empty);
  
  assert_false(scene.isSquareEmpty(0, 0), "occurred when square specified should be blind.")
  assert_true(scene.isSquareEmpty(1, 0), "occurred when square specified should be empty.")
  assert_false(scene.isSquareEmpty(2, 0), "occurred when square specified should contain an item.")
end