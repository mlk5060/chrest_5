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
  scene.addItemToSquare(2, 0, "", Scene.getEmptySquareToken())
  scene.addItemToSquare(4, 0, "2", tile_token)
  
  scene.addItemToSquare(1, 1, "3", hole_token)
  scene.addItemToSquare(2, 1, "4", opponent_token)
  scene.addItemToSquare(3, 1, "5", tile_token)
  
  scene.addItemToSquare(0, 2, "", Scene.getEmptySquareToken())
  scene.addItemToSquare(1, 2, "6", hole_token)
  scene.addItemToSquare(2, 2, "0", Scene.getCreatorToken())
  scene.addItemToSquare(3, 2, "7", tile_token)
  scene.addItemToSquare(4, 2, "8", tile_token)
  
  scene.addItemToSquare(1, 3, "9", hole_token)
  scene.addItemToSquare(2, 3, "10", tile_token)
  scene.addItemToSquare(3, 3, "", Scene.getEmptySquareToken())
  
  scene.addItemToSquare(0, 4, "11", tile_token)
  scene.addItemToSquare(2, 4, "12", hole_token)
  scene.addItemToSquare(4, 4, "13", tile_token)
  
  domain = TileworldDomain.new(Chrest.new, 2, 2)
  row = 0
  col = 0
  until row == scene.getHeight() do
    until col == scene.getWidth() do
      expected_number_of_movement_fixations = 0
      expected_movement_fixations = ArrayList.new()
      
      if (
        (col == 0 and row == 0) or
        (col == 4 and row == 0) or
        (col == 0 and row == 4) or
        (col == 4 and row == 4)
      )
        expected_number_of_movement_fixations = 2
        
        if(col == 0 and row == 0)
          expected_movement_fixations.add(Square.new(col, row + 1))
          expected_movement_fixations.add(Square.new(col + 1, row))
        elsif(col == 4 and row == 0)
          expected_movement_fixations.add(Square.new(col, row + 1))
          expected_movement_fixations.add(Square.new(col - 1, row))
        elsif(col == 0 and row == 4)
          expected_movement_fixations.add(Square.new(col, row - 1))
          expected_movement_fixations.add(Square.new(col + 1, row))
        else
          expected_movement_fixations.add(Square.new(col, row - 1))
          expected_movement_fixations.add(Square.new(col - 1, row))
        end
      elsif(col == 4 and row == 2)
        expected_number_of_movement_fixations = 3
        expected_movement_fixations.add(Square.new(col, row + 1))
        expected_movement_fixations.add(Square.new(col - 1, row))
        expected_movement_fixations.add(Square.new(col, row - 1))
      elsif(
        (col == 2 and row == 1) or
        (col == 3 and row == 1) or 
        (col == 2 and row == 2) or
        (col == 3 and row == 2) or
        (col == 2 and row == 3)
      )
        expected_number_of_movement_fixations = 4
        expected_movement_fixations.add(Square.new(col, row + 1))
        expected_movement_fixations.add(Square.new(col + 1, row))
        expected_movement_fixations.add(Square.new(col, row - 1))
        expected_movement_fixations.add(Square.new(col - 1, row))
      end
      
      movement_fixations = domain.proposeMovementFixations(scene, Square.new(col, row))  
      assert_equal(expected_number_of_movement_fixations, movement_fixations.size(), "Occurred when checking the number of movement fixations for an object on square with col " + col.to_s + " and row " + row.to_s + ".  Fixations calculated: " + movement_fixations.to_s + ".")
      
      for i in 0...expected_movement_fixations.size
        expected_movement_fixation = expected_movement_fixations[i].to_s
        expected_movement_fixation_in_movement_fixations_returned = false
        for j in 0...movement_fixations.size
          if movement_fixations[j].to_s == expected_movement_fixation
            expected_movement_fixation_in_movement_fixations_returned = true
          end
        end
        assert_true(expected_movement_fixation_in_movement_fixations_returned, "Occurred when checking the movement fixations for an object on square with col " + col.to_s + " and row " + row.to_s + ".  Fixations calculated: " + movement_fixations.to_s + ".")
      end
      
      col += 1
    end
    
    row += 1
    col = 0
  end
  
end
