# Check that, in the scene represented below, the correct movement fixations
# are generated ("X" represents a blind square, the meaning of other identifiers 
# are delineated in the test code).
# 
#     |-----|-----|-----|-----|-----|
#  4  |  T  |  X  |  H  |  X  |  T  |
#     |-----|-----|-----|-----|-----|
#  3  |  X  |  H  |  T  |     |  X  |
#     |-----|-----|-----|-----|-----|
#  2  |     |  H  | SELF|  T  |  T  |
#     |-----|-----|-----|-----|-----|
#  1  |  X  |  H  |  O  |  T  |  X  |
#     |-----|-----|-----|-----|-----|
#  0  |  T  |  X  |     |  X  |  T  |
#     |-----|-----|-----|-----|-----|
#        0     1     2     3     4
unit_test "moves" do
  
  hole_token = "H"
  tile_token = "T"
  opponent_token = "O"
  
  scene = Scene.new("Test movement scene", 5, 5, nil)
  scene.addItemToSquare(0, 0, "1", tile_token)
  scene.addItemToSquare(2, 0, "", Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(4, 0, "2", tile_token)
  
  scene.addItemToSquare(1, 1, "3", hole_token)
  scene.addItemToSquare(2, 1, "4", opponent_token)
  scene.addItemToSquare(3, 1, "5", tile_token)
  
  scene.addItemToSquare(0, 2, "", Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(1, 2, "6", hole_token)
  scene.addItemToSquare(2, 2, "0", Scene.getCreatorToken())
  scene.addItemToSquare(3, 2, "7", tile_token)
  scene.addItemToSquare(4, 2, "8", tile_token)
  
  scene.addItemToSquare(1, 3, "9", hole_token)
  scene.addItemToSquare(2, 3, "10", tile_token)
  scene.addItemToSquare(3, 3, "", Scene.getEmptySquareIdentifier())
  
  scene.addItemToSquare(0, 4, "11", tile_token)
  scene.addItemToSquare(2, 4, "12", hole_token)
  scene.addItemToSquare(4, 4, "13", tile_token)
  
#  object_movements = ArrayList.new
#  object_movements.add(1)
#  object_movements.add(1)
#  object_movements.add(1)
#  object_movements.add(1)
#  
#  item_identifiers_and_movements = HashMap.new
#  item_identifiers_and_movements.put(tile_token, object_movements)
#  item_identifiers_and_movements.put(Scene.getSelfIdentifier(), object_movements)
#  item_identifiers_and_movements.put(opponent_token, object_movements)
  
  domain = TileworldDomain.new(Chrest.new)
  row = 0
  col = 0
  until row == scene.getHeight() do
    until col == scene.getWidth() do
      expected_number_of_movement_fixations = 0
      expected_movement_fixations = ArrayList.new()
      
      if (
        (col == 0 and row == 0) or
        (col == 4 and row == 0) or
        (col == 2 and row == 1) or
        (col == 3 and row == 1) or 
        (col == 2 and row == 2) or
        (col == 3 and row == 2) or
        (col == 4 and row == 2) or
        (col == 2 and row == 3) or
        (col == 0 and row == 4) or
        (col == 4 and row == 4)
      )
        expected_number_of_movement_fixations = 4
        expected_movement_fixations.add(Square.new(col, row + 1))
        expected_movement_fixations.add(Square.new(col + 1, row))
        expected_movement_fixations.add(Square.new(col, row - 1))
        expected_movement_fixations.add(Square.new(col - 1, row))
      end
      
      movement_fixations = domain.proposeMovementFixations(scene, Square.new(col, row))     
      assert_equal(expected_number_of_movement_fixations, movement_fixations.size(), "Occurred when checking the number of movement fixations for an object on square with col " + col.to_s + " and row " + row.to_s + ".  Fixations calculated: " + movement_fixations.to_s + ".")
      assert_equal(expected_movement_fixations.to_s, movement_fixations.to_s, "Occurred when checking the movement fixations for an object on square with col " + col.to_s + " and row " + row.to_s + ".  Fixations calculated: " + movement_fixations.to_s + ".")
      
      col += 1
    end
    
    row += 1
    col = 0
  end
  
end
