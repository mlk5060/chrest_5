# Check that, in the scene represented below, the correct movement fixations
# are generated ("X" represents a blind square, the meaning of other identifiers 
# are delineated in the test code).
# 
#     |-----|-----|-----|-----|-----|
#  4  |  T  |  X  |  H  |  X  |  T  |
#     |-----|-----|-----|-----|-----|
#  3  |  X  |  H  |  T  |     |  X  |
#     |-----|-----|-----|-----|-----|
#  2  |     | T,H | SELF|  T  |  T  |
#     |-----|-----|-----|-----|-----|
#  1  |  X  | T,H |  O  |  T  |  X  |
#     |-----|-----|-----|-----|-----|
#  0  |  T  |  X  |     |  X  |  T  |
#     |-----|-----|-----|-----|-----|
#        0     1     2     3     4
unit_test "moves" do
  
  hole_identifier = "H"
  tile_identifier = "T"
  opponent_identifier = "O"
  
  scene = Scene.new("Test movement scene", 5, 5)
  scene.addItemToSquare(0, 0, tile_identifier)
  scene.addItemToSquare(2, 0, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(4, 0, tile_identifier)
  
  scene.addItemToSquare(1, 1, tile_identifier)
  scene.addItemToSquare(1, 1, hole_identifier)
  scene.addItemToSquare(2, 1, opponent_identifier)
  scene.addItemToSquare(3, 1, tile_identifier)
  
  scene.addItemToSquare(0, 2, Scene.getEmptySquareIdentifier())
  scene.addItemToSquare(1, 2, tile_identifier) 
  scene.addItemToSquare(1, 2, hole_identifier)
  scene.addItemToSquare(2, 2, Scene.getSelfIdentifier())
  scene.addItemToSquare(3, 2, tile_identifier)
  scene.addItemToSquare(4, 2, tile_identifier)
  
  scene.addItemToSquare(1, 3, hole_identifier)
  scene.addItemToSquare(2, 3, tile_identifier)
  scene.addItemToSquare(3, 3, Scene.getEmptySquareIdentifier())
  
  scene.addItemToSquare(0, 4, tile_identifier)
  scene.addItemToSquare(2, 4, hole_identifier)
  scene.addItemToSquare(4, 4, tile_identifier)
  
  object_movements = ArrayList.new
  object_movements.add(1)
  object_movements.add(1)
  object_movements.add(1)
  object_movements.add(1)
  
  item_identifiers_and_movements = HashMap.new
  item_identifiers_and_movements.put(tile_identifier, object_movements)
  item_identifiers_and_movements.put(Scene.getSelfIdentifier(), object_movements)
  item_identifiers_and_movements.put(opponent_identifier, object_movements)
  
  domain = TileworldDomain.new(Chrest.new)
  row = 0
  col = 0
  until row == scene.getHeight() do
    until col == scene.getWidth() do
      expected_number_of_movement_fixations = 0
      expected_movement_fixations = ArrayList.new()
      
      if (col == 2 and row == 1)
        expected_number_of_movement_fixations = 2
        expected_movement_fixations.add(Square.new(3, 1))
        expected_movement_fixations.add(Square.new(2, 0))
      elsif (col == 3 and row == 1)
        expected_number_of_movement_fixations = 1
        expected_movement_fixations.add(Square.new(4, 1))
      elsif (col == 2 and row == 2)
        expected_number_of_movement_fixations = 1
        expected_movement_fixations.add(Square.new(2, 3))
      elsif (col == 2 and row == 3)
        expected_number_of_movement_fixations = 1
        expected_movement_fixations.add(Square.new(2, 4))
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
