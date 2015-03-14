# jchrest.lib.Scene tests

# Blind spot identifier and empty square identifiers not publicly settable so
# not possible to test functions that return them accurately.

unit_test "constructor" do
  scene = Scene.new("test", 2, 2)
  expected_scene_contents = ListPattern.new()
  for row in 0..1
    for col in 0..1
      expected_scene_contents.add(ItemSquarePattern.new("null", col, row))
    end
  end
  assert_equal(expected_scene_contents, scene.getScene())
end

unit_test "add-item-to-square" do
  scene = Scene.new("test", 5, 3)
  
  # Create a scene representing a visual-cone (contains blind-spots) and test 
  # that:
  # 1) Blind spots are overwritten.
  # 2) Squares that are empty do not have an empty object identifier added.
  # 3) Squares that are empty are replaced by a non-empty object.
  # 4) Squares that aren't empty are replaced by an empty object.
  # 5) Squares that aren't empty are added to if a non-empty object is 
  #    specified.
  
  # Test 1
  scene.addItemToSquare(2, 0, "d")
  scene.addItemToSquare(1, 1, ".")
  scene.addItemToSquare(2, 1, "c")
  scene.addItemToSquare(3, 1, ".")
  scene.addItemToSquare(0, 2, "a")
  scene.addItemToSquare(1, 2, ".")
  scene.addItemToSquare(2, 2, ".")
  scene.addItemToSquare(3, 2, "b")
  scene.addItemToSquare(4, 2, ".")
  
  expected_scene_contents = ListPattern.new
  for row in 0..2
    for col in 0..4
      item = "null"
      
      if col == 2 and row == 0 
        item = "d"
      end
      
      if row == 1
        if col == 1 or col == 3
          item = "."
        elsif col == 2
          item = "c"
        end
      end
      
      if row == 2
        if col == 1 or col == 2 or col == 4
          item = "."
        elsif col == 0
          item = "a"
        else
          item = "b"
        end
      end
      
      expected_scene_contents.add(ItemSquarePattern.new(item, col, row))
    end
  end
  assert_equal(expected_scene_contents, scene.getScene, "testing that blind spots are overwritten")
  
  #Test 2
  scene.addItemToSquare(1, 1, ".")
  assert_equal(expected_scene_contents, scene.getScene, "testing that empty squares do not contain multiple empty square identifiers")

  #Test 3
  scene.addItemToSquare(1, 1, "e")
  expected_scene_contents = ListPattern.new
  for row in 0..2
    for col in 0..4
      item = "null"
      
      if col == 2 and row == 0 
        item = "d"
      end
      
      if row == 1
        if col == 3
          item = "."
        elsif col == 1
          item = "e"
        elsif col == 2
          item = "c"
        end
      end
      
      if row == 2
        if col == 1 or col == 2 or col == 4
          item = "."
        elsif col == 0
          item = "a"
        else
          item = "b"
        end
      end
      
      expected_scene_contents.add(ItemSquarePattern.new(item, col, row))
    end
  end
  assert_equal(expected_scene_contents, scene.getScene, "testing that empty squares are overwritten by non-empty object identifiers")
  
  #Test 4
  scene.addItemToSquare(1, 1, ".")
  expected_scene_contents = ListPattern.new()
  for row in 0..2
    for col in 0..4
      item = "null"
      
      if col == 2 and row == 0 
        item = "d"
      end
      
      if row == 1
        if col == 1 or col == 3
          item = "."
        elsif col == 2
          item = "c"
        end
      end
      
      if row == 2
        if col == 1 or col == 2 or col == 4
          item = "."
        elsif col == 0
          item = "a"
        else
          item = "b"
        end
      end
      
      expected_scene_contents.add(ItemSquarePattern.new(item, col, row))
    end
  end
  assert_equal(expected_scene_contents, scene.getScene(), "testing that non-empty squares are overwritten by empty square identifiers")
  
  #Test 5
  scene.addItemToSquare(2, 1, "e")
  expected_scene_contents = ListPattern.new()
  for row in 0..2
    for col in 0..4
      item = "null"
      
      if col == 2 and row == 0 
        item = "d"
      end
      
      if row == 1
        if col == 1 or col == 3
          item = "."
        elsif col == 2
          expected_scene_contents.add(ItemSquarePattern.new("c", col, row))
          item = "e"
        end
      end
      
      if row == 2
        if col == 1 or col == 2 or col == 4
          item = "."
        elsif col == 0
          item = "a"
        else
          item = "b"
        end
      end
      
      expected_scene_contents.add(ItemSquarePattern.new(item, col, row))
    end
  end
  assert_equal(expected_scene_contents, scene.getScene(), "testing that non-empty identifiers are added to non-empty squares")
end

unit_test "add-items-to-row" do
  
  # Scene.addItemsToRow simply wraps the Scene.addItemToSquare with an access
  # control method that blocks blind spots from being processed so, if 
  # Scene.addItemToSquare is working correctly, the only thing we need to check
  # here is whether blind-spots are ignored.  To do this, we'll create a Scene
  # instance representing a visual-cone (will contain many blind-spots) using 
  # Scene.addItemToSquare.
  scene = Scene.new("test", 5, 3)
  scene.addItemsToRow(0, "  d  ".to_java.toCharArray)
  scene.addItemsToRow(1, " .c. ".to_java.toCharArray)
  scene.addItemsToRow(2, "a..b.".to_java.toCharArray)
  
  expected_scene_contents = ListPattern.new()
  for row in 0..2
    for col in 0..4
      item = "null"
      
      if col == 2 and row == 0 
        item = "d"
      end
      
      if row == 1
        if col == 1 or col == 3
          item = "."
        elsif col == 2
          item = "c"
        end
      end
      
      if row == 2
        if col == 1 or col == 2 or col == 4
          item = "."
        elsif col == 0
          item = "a"
        else
          item = "b"
        end
      end
      
      expected_scene_contents.add(ItemSquarePattern.new(item, col, row))
    end
  end
  
  assert_equal(expected_scene_contents, scene.getScene())
end

unit_test "compute-errors-of-commission" do
  scene1 = Scene.new("test-scene-1", 5, 3)
  scene2 = Scene.new("test-scene-2", 5, 3)
  
  #Scene 2 is the scene being created by someone after seeing scene 1.  Scene 2 
  #should have 2 more objects than scene 1 (too many objects).
  scene1.addItemsToRow(0, "  d  ".to_java.toCharArray)
  scene1.addItemsToRow(1, " .c. ".to_java.toCharArray)
  scene1.addItemsToRow(2, "a..b.".to_java.toCharArray)
  scene2.addItemsToRow(0, "  d  ".to_java.toCharArray)
  scene2.addItemsToRow(1, " .c. ".to_java.toCharArray)
  scene2.addItemsToRow(2, "a..be".to_java.toCharArray)
  scene2.addItemToSquare(4, 2, "f")
  
  assert_equal(2, scene1.computeErrorsOfCommission(scene2))
end

unit_test "compute-errors-of-omission" do
  scene1 = Scene.new("test-scene-1", 5, 3)
  scene2 = Scene.new("test-scene-2", 5, 3)
  
  #Scene 2 is the scene being created by someone after seeing scene 1.  Scene 2 
  #should have 2 fewer objects than scene 1 (too few objects).
  scene1.addItemsToRow(0, "  d  ".to_java.toCharArray)
  scene1.addItemsToRow(1, " .c. ".to_java.toCharArray)
  scene1.addItemsToRow(2, "a..b.".to_java.toCharArray)
  scene2.addItemsToRow(0, "  d  ".to_java.toCharArray)
  scene2.addItemsToRow(1, " ... ".to_java.toCharArray)
  scene2.addItemsToRow(2, "a....".to_java.toCharArray)
  
  assert_equal(2, scene1.computeErrorsOfOmission(scene2))
end

unit_test "compute-precision" do
  scene1 = Scene.new("test-scene-1", 5, 3)
  scene2 = Scene.new("test-scene-2", 5, 3)
  
  #Scene 2 is the scene being created by someone after seeing scene 1.
  scene1.addItemsToRow(0, "  d  ".to_java.toCharArray)
  scene1.addItemsToRow(1, " .c. ".to_java.toCharArray)
  scene1.addItemsToRow(2, "a..b.".to_java.toCharArray)
  scene2.addItemsToRow(0, "  d  ".to_java.toCharArray)
  scene2.addItemsToRow(1, " .c. ".to_java.toCharArray)
  scene2.addItemsToRow(2, ".....".to_java.toCharArray)
  assert_equal(0.5, scene2.computePrecision(scene1), "both scenes contain items")
  
  #Scene 1 is now empty (scene 2 isn't) so precision should equal 0.
  scene1 = Scene.new("test-scene-1", 5, 3)
  scene2.addItemsToRow(0, "  d  ".to_java.toCharArray)
  scene2.addItemsToRow(1, " .c. ".to_java.toCharArray)
  scene2.addItemsToRow(2, ".....".to_java.toCharArray)
  assert_equal(0.0, scene2.computePrecision(scene1), "scene 1 is empty, scene 2 isn't")
  
  #Scene 2 is now empty (scene 1 isn't) so precision should equal 0.
  scene2 = Scene.new("test-scene-2", 5, 3)
  scene1.addItemsToRow(0, "  d  ".to_java.toCharArray)
  scene1.addItemsToRow(1, " .c. ".to_java.toCharArray)
  scene1.addItemsToRow(2, "a..b.".to_java.toCharArray)
  assert_equal(0.0, scene2.computePrecision(scene1), "scene 2 is empty, scene 1 isn't")
end

unit_test "compute-recall" do 
  scene1 = Scene.new("test-scene-1", 5, 3)
  scene2 = Scene.new("test-scene-2", 5, 3)
  
  #Scene 2 is the scene being created by someone after seeing scene 1.
  scene1.addItemsToRow(0, "  d  ".to_java.toCharArray)
  scene1.addItemsToRow(1, " .c. ".to_java.toCharArray)
  scene1.addItemsToRow(2, "a..b.".to_java.toCharArray)
  scene2.addItemsToRow(0, "  d  ".to_java.toCharArray)
  scene2.addItemsToRow(1, " .c. ".to_java.toCharArray)
  scene2.addItemsToRow(2, ".....".to_java.toCharArray)
  assert_equal(0.5, scene2.computeRecall(scene1))
  
  #Scene 1 is now empty (scene 2 isn't) so recall should equal 0.
  scene1 = Scene.new("test-scene-1", 5, 3)
  scene2.addItemsToRow(0, "  d  ".to_java.toCharArray)
  scene2.addItemsToRow(1, " .c. ".to_java.toCharArray)
  scene2.addItemsToRow(2, ".....".to_java.toCharArray)
  assert_equal(0.0, scene2.computeRecall(scene1), "scene 1 is empty, scene 2 isn't")
  
  #Scene 2 is now empty (scene 1 isn't) so precision should equal 0.
  scene2 = Scene.new("test-scene-2", 5, 3)
  scene1.addItemsToRow(0, "  d  ".to_java.toCharArray)
  scene1.addItemsToRow(1, " .c. ".to_java.toCharArray)
  scene1.addItemsToRow(2, "a..b.".to_java.toCharArray)
  assert_equal(0.0, scene2.computeRecall(scene1), "scene 2 is empty, scene 1 isn't")
end

unit_test "get_all_items_in_scene" do
  scene = Scene.new("test-scene", 5, 3)
  scene.addItemsToRow(0, "  d  ".to_java.toCharArray)
  scene.addItemsToRow(1, " ... ".to_java.toCharArray)
  scene.addItemsToRow(2, "a..be".to_java.toCharArray)
  scene.addItemToSquare(4, 2, "f")
  
  expected_items = ListPattern.new
  expected_items.add(ItemSquarePattern.new("d", 2, 0))
  expected_items.add(ItemSquarePattern.new("a", 0, 2))
  expected_items.add(ItemSquarePattern.new("b", 3, 2))
  expected_items.add(ItemSquarePattern.new("e", 4, 2))
  expected_items.add(ItemSquarePattern.new("f", 4, 2))
  
  assert_equal(expected_items, scene.getAllItemsInScene())
end

unit_test "get_height" do
  height = 3
  scene = Scene.new("test-scene", 5, height)
  assert_equal(height, scene.getHeight())
end

unit_test "get_items_in_scope" do
  scene = Scene.new("test", 5, 5)
  scene.addItemsToRow(0, " .f. ".to_java.toCharArray);
  scene.addItemsToRow(1, ". a .".to_java.toCharArray);
  scene.addItemsToRow(2, ".bce.".to_java.toCharArray);
  scene.addItemsToRow(3, ". d .".to_java.toCharArray);
  scene.addItemsToRow(4, " .g. ".to_java.toCharArray);
  scene.addItemToSquare(3, 2, "h");
  
  expected_items = ListPattern.new
  expected_items.add(ItemSquarePattern.new("f", 2, 0))
  expected_items.add(ItemSquarePattern.new("a", 2, 1))
  expected_items.add(ItemSquarePattern.new("b", 1, 2))
  expected_items.add(ItemSquarePattern.new("c", 2, 2))
  expected_items.add(ItemSquarePattern.new("e", 3, 2))
  expected_items.add(ItemSquarePattern.new("h", 3, 2))
  expected_items.add(ItemSquarePattern.new("d", 2, 3))
  expected_items.add(ItemSquarePattern.new("g", 2, 4))
  
  assert_equal(expected_items, scene.getItemsInScope(2, 2, 1, 2))
end

unit_test "get_items_on_square" do
  scene = Scene.new("test", 5, 5)
  scene.addItemsToRow(0, " .f. ".to_java.toCharArray);
  scene.addItemsToRow(1, ". a .".to_java.toCharArray);
  scene.addItemsToRow(2, ".bce.".to_java.toCharArray);
  scene.addItemsToRow(3, ". d .".to_java.toCharArray);
  scene.addItemsToRow(4, " .g. ".to_java.toCharArray);
  scene.addItemToSquare(3, 2, "h");
  
  assert_equal(ListPattern.new, scene.getItemsOnSquare(0, 0), "checking blind square")
  assert_equal(ListPattern.new, scene.getItemsOnSquare(4, 3), "checking empty square")
  
  expected_list_pattern = ListPattern.new
  expected_list_pattern.add(ItemSquarePattern.new("e", 3, 2))
  expected_list_pattern.add(ItemSquarePattern.new("h", 3, 2))
  assert_equal(expected_list_pattern, scene.getItemsOnSquare(3, 2), "checking square with multiple items")
end

unit_test "get_name" do
  scene = Scene.new("", 5, 5)
  assert_equal("", scene.getName(), "empty scene name")
  
  scene_name = "test"
  scene = Scene.new(scene_name, 5, 5)
  assert_equal(scene_name, scene.getName(), "non-empty scene name")
end

unit_test "get_scene" do
  scene = Scene.new("test", 5, 5)
  scene.addItemsToRow(0, " .f. ".to_java.toCharArray);
  scene.addItemsToRow(1, ". a .".to_java.toCharArray);
  scene.addItemsToRow(2, ".bce.".to_java.toCharArray);
  scene.addItemsToRow(3, ". d .".to_java.toCharArray);
  scene.addItemsToRow(4, " .g. ".to_java.toCharArray);
  scene.addItemToSquare(3, 2, "h");
  
  expected_scene = ListPattern.new
  expected_scene.add(ItemSquarePattern.new("null", 0, 0))
  expected_scene.add(ItemSquarePattern.new(".", 1, 0))
  expected_scene.add(ItemSquarePattern.new("f", 2, 0))
  expected_scene.add(ItemSquarePattern.new(".", 3, 0))
  expected_scene.add(ItemSquarePattern.new("null", 4, 0))
  
  expected_scene.add(ItemSquarePattern.new(".", 0, 1))
  expected_scene.add(ItemSquarePattern.new("null", 1, 1))
  expected_scene.add(ItemSquarePattern.new("a", 2, 1))
  expected_scene.add(ItemSquarePattern.new("null", 3, 1))
  expected_scene.add(ItemSquarePattern.new(".", 4, 1))
  
  expected_scene.add(ItemSquarePattern.new(".", 0, 2))
  expected_scene.add(ItemSquarePattern.new("b", 1, 2))
  expected_scene.add(ItemSquarePattern.new("c", 2, 2))
  expected_scene.add(ItemSquarePattern.new("e", 3, 2))
  expected_scene.add(ItemSquarePattern.new("h", 3, 2))
  expected_scene.add(ItemSquarePattern.new(".", 4, 2))
  
  expected_scene.add(ItemSquarePattern.new(".", 0, 3))
  expected_scene.add(ItemSquarePattern.new("null", 1, 3))
  expected_scene.add(ItemSquarePattern.new("d", 2, 3))
  expected_scene.add(ItemSquarePattern.new("null", 3, 3))
  expected_scene.add(ItemSquarePattern.new(".", 4, 3))
  
  expected_scene.add(ItemSquarePattern.new("null", 0, 4))
  expected_scene.add(ItemSquarePattern.new(".", 1, 4))
  expected_scene.add(ItemSquarePattern.new("g", 2, 4))
  expected_scene.add(ItemSquarePattern.new(".", 3, 4))
  expected_scene.add(ItemSquarePattern.new("null", 4, 4))
  assert_equal(expected_scene, scene.getScene())
end

unit_test "get_width" do
  width = 5
  scene = Scene.new("", 5, width)
  assert_equal(width, scene.getWidth())
end

unit_test "is_square_blind" do
  scene = Scene.new("test", 5, 5)
  scene.addItemsToRow(0, " .f. ".to_java.toCharArray);
  scene.addItemsToRow(1, ". a .".to_java.toCharArray);
  scene.addItemsToRow(2, ".bce.".to_java.toCharArray);
  scene.addItemsToRow(3, ". d .".to_java.toCharArray);
  scene.addItemsToRow(4, " .g. ".to_java.toCharArray);
  scene.addItemToSquare(3, 2, "h");
  
  assert_true(scene.isSquareBlind(0, 0), "square is blind")
  assert_false(scene.isSquareBlind(1, 0), "square is empty")
  assert_false(scene.isSquareBlind(2, 0), "square has 1 item")
  assert_false(scene.isSquareBlind(3, 2), "square has 2 items")
end

unit_test "is_square_empty" do
  scene = Scene.new("test", 5, 5)
  scene.addItemsToRow(0, " .f. ".to_java.toCharArray);
  scene.addItemsToRow(1, ". a .".to_java.toCharArray);
  scene.addItemsToRow(2, ".bce.".to_java.toCharArray);
  scene.addItemsToRow(3, ". d .".to_java.toCharArray);
  scene.addItemsToRow(4, " .g. ".to_java.toCharArray);
  scene.addItemToSquare(3, 2, "h");
  
  assert_false(scene.isSquareEmpty(0, 0), "square is blind")
  assert_true(scene.isSquareEmpty(1, 0), "square is empty")
  assert_false(scene.isSquareEmpty(2, 0), "square has 1 item")
  assert_false(scene.isSquareEmpty(3, 2), "square has 2 items")
end