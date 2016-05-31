################################################################################
unit_test "constructor" do
  model_creation_time = 5
  model = Chrest.new(model_creation_time, false)
  
  error_thrown = false
  
  # Assert that constructing a new STM instance at a time that is >= its 
  # associated model's time of creation does not throw an error.
  begin
    stm = Stm.new(model, Modality::ACTION, 4, model_creation_time)
  rescue
    error_thrown = true
  end
  assert_false(error_thrown, "test 1")
  
  begin
    stm = Stm.new(model, Modality::ACTION, 4, model_creation_time + 1)
  rescue
    error_thrown = true
  end
  assert_false(error_thrown, "test 2")
  
  # Assert that constructing a new STM instance at a time that is < its 
  # associated model's time of creation throws an error.
  begin
    stm = Stm.new(model, Modality::ACTION, 4, model_creation_time - 1)
  rescue
    error_thrown = true
  end
  assert_true(error_thrown, "test 3")
end

################################################################################
# This function essentially tests all functions of jchrest.architecture.Stm.
# The test is split into two major sections: 
#
# 1. Setter function tests
# 2. Getter function tests
#
# Note that, during setter function tests, only the value returned by a function 
# is checked; changes that a function makes to STM contents/capacity are checked 
# during getter function tests.
process_test "getter and setters" do
  
  #############
  ### SETUP ###
  #############
  
  model_creation_time = 0
  model = Chrest.new(model_creation_time, false)
  
  # Construct a new STM with a different creation time to that of the model so
  # that conditional checks in setter methods that check the creation time of
  # STM can be tested with values greater than 0.
  time_stm_created = model_creation_time + 5
  stm = Stm.new(model, Modality::VISUAL, 4, time_stm_created)
  
  # Create nodes that will be used to isolate conditionals in setter methods.
  node_created_before_stm = Node.new(model, ListPattern.new(Modality::VISUAL), ListPattern.new(Modality::VISUAL), time_stm_created - 1)
  node_with_mismatched_stm_modality = Node.new(model, ListPattern.new(Modality::ACTION), ListPattern.new(Modality::ACTION), time_stm_created)
  
  # Create the nodes that will be used in successful setter function 
  # invocations.
  time_node_1_created = time_stm_created + 5
  pattern_1 = ListPattern.new(Modality::VISUAL)
  pattern_1.add(ItemSquarePattern.new("A", 0, 1))
  node_1 = Node.new(
    model, 
    pattern_1, 
    pattern_1, 
    time_node_1_created
  )
  
  time_node_2_created = time_node_1_created + 5
  pattern_2 = pattern_1.append(ItemSquarePattern.new("B", 2, 0))
  node_2 = Node.new(
    model, 
    pattern_2, 
    pattern_2, 
    time_node_2_created
  )
  
  time_node_3_created = time_node_2_created + 5
  pattern_3 = pattern_2.append(ItemSquarePattern.new("C", 3, 0))
  node_3 = Node.new(
    model, 
    pattern_3, 
    pattern_3, 
    time_node_3_created
  )
  
  time_node_4_created = time_node_3_created + 5
  pattern_4 = pattern_3.append(ItemSquarePattern.new("D", 4, 0))
  node_4 = Node.new(
    model, 
    pattern_4,
    pattern_4, 
    time_node_4_created
  )
  
  time_node_5_created = time_node_4_created + 5
  pattern_5 = pattern_4.append(ItemSquarePattern.new("E", 5, 0))
  node_5 = Node.new(
    model, 
    pattern_5,
    pattern_5, 
    time_node_5_created
  )
  
  time_node_6_created = time_node_5_created + 5
  pattern_6 = ListPattern.new().append(ItemSquarePattern.new("Z", 0, 1))
  node_6 = Node.new(
    model,
    pattern_6,
    pattern_6,
    time_node_6_created
  )
  
  time_node_7_created = time_node_6_created + 5
  pattern_7 = pattern_6.append(ItemSquarePattern.new("Y", 0, 2))
  node_7 = Node.new(
    model,
    pattern_7,
    pattern_7,
    time_node_7_created
  )
  
  time_node_8_created = time_node_7_created + 5
  pattern_8 = pattern_7.append(ItemSquarePattern.new("X", 0, 3))
  node_8 = Node.new(
    model,
    pattern_8,
    pattern_8,
    time_node_8_created
  )
  
  #===============================#
  #==== SETTER FUNCTION TESTS ====#
  #===============================#
  
  ##########################################
  ### SETTER FUNCTION CONDITIONAL CHECKS ###
  ##########################################
  
  # Before invoking any setter functions successfully, check that non-history 
  # rewriting checks in the setter function conditionals work correctly.  If the 
  # setter functions are invoked and they are successful, historical data 
  # structures in STM will become populated making it impossible to isolate 
  # non-history rewriting checks.  Therefore, we could never assert that checks
  # on creation time/modalities etc. are working correctly since the history
  # rewriting checks would confound the test result.
  assert_false(stm.add(node_created_before_stm, time_stm_created - 1), "see test 1") #Isolates STM creation time check
  assert_false(stm.add(node_1, time_node_1_created - 1), "see test 2") #Isolates node creation time check
  assert_false(stm.add(node_with_mismatched_stm_modality, time_stm_created + 5), "see test 3") # Isolates modality check
  
  assert_false(stm.replaceHypothesis(node_created_before_stm, time_stm_created - 1), "see test 4") #Isolates STM creation time check
  assert_false(stm.replaceHypothesis(node_1, time_node_1_created - 1), "see test 5") #Isolates node creation time check
  assert_false(stm.replaceHypothesis(node_with_mismatched_stm_modality, time_stm_created + 5), "see test 6") # Isolates modality check
  
  assert_false(stm.clear(time_stm_created - 1), "see test 7")
  
  assert_false(stm.setCapacity(1, time_stm_created - 1), "see test 8")
  
  ######################
  ### Stm.add() TEST ###
  ######################
  
  # The first 5 nodes to be added are presequences of one another however, each
  # node is more informative than the last:
  # 
  # Node 1 content and image: <[A 0 1]>
  # Node 2 content and image: <[A 0 1][B 2 0]>
  # Node 3 content and image: <[A 0 1][B 2 0][C 3 0]>
  # Node 4 content and image: <[A 0 1][B 2 0][C 3 0][D 4 0]>
  # Node 5 content and image: <[A 0 1][B 2 0][C 3 0][D 4 0][E 5 0]>
  # 
  # So, if these nodes are added to STM in order, STM will reach its capacity 
  # upon insertion of node 5 and will lose node 1.  After adding the first 5
  # nodes, their positions in STM will be reversed when compared to their 
  # natural numbering.  So, after adding the node 5, STM should look like the 
  # following:
  #
  # STM item 1 (hypothesis): node 5 
  # STM item 2: node 4
  # STM item 3: node 3
  # STM item 4: node 2
  #
  # 3 nodes are then added so that the previous hypothesis is pushed to the 
  # bottom of STM (the reason for this will be explained below).  The 3 nodes 
  # added have contents and images that are different to the first 5 but are 
  # presequences of one another:  
  #
  # Node 6 image and content: <[Z 0 1]>
  # Node 7 image and content: <[Z 0 1][Y 0 2]>
  # Node 8 image and content: <[Z 0 1][Y 0 2][X 0 3]>
  # 
  # Therefore, like after adding the first 5 nodes, the positions of nodes 6-8 
  # in STM will be reversed when compared to their natural numbering.  So, after 
  # adding node 8, STM should look like the following:
  #
  # STM item 1 (hypothesis): node 8
  # STM item 2: node 7
  # STM item 3: node 6
  # STM item 4: node 5
  #
  # Note that, at this point, adding another node whose content doesn't match 
  # node 5 would cause node 5 to be removed from STM.  However, to test the 
  # retainment of the most informative node given new input, we add node 2 
  # again.  This should result in node 5 being retained and reinserted at the 
  # top of STM since node 5 is a presequence of node 2 but is more informative.
  # So, after adding node 2, STM should look like the following:
  #
  # STM item 1 (hypothesis): node 5 
  # STM item 2: node 2
  # STM item 3: node 8
  # STM item 4: node 7
  
  # Set timings for STM node addition
  time_node_1_added_to_stm = time_node_8_created + 5
  time_node_2_added_to_stm = time_node_1_added_to_stm + 5
  time_node_3_added_to_stm = time_node_2_added_to_stm + 5
  time_node_4_added_to_stm = time_node_3_added_to_stm + 5
  time_node_5_added_to_stm = time_node_4_added_to_stm + 5
  time_node_6_added_to_stm = time_node_5_added_to_stm + 5
  time_node_7_added_to_stm = time_node_6_added_to_stm + 5
  time_node_8_added_to_stm = time_node_7_added_to_stm + 5
  time_node_2_added_to_stm_again = time_node_8_added_to_stm + 5
  
  # Add nodes to STM (expected to be successful).
  assert_true(stm.add(node_1, time_node_1_added_to_stm), "see test 9")
  assert_true(stm.add(node_2, time_node_2_added_to_stm), "see test 10")
  assert_true(stm.add(node_3, time_node_3_added_to_stm), "see test 11")
  assert_true(stm.add(node_4, time_node_4_added_to_stm), "see test 12")
  assert_true(stm.add(node_5, time_node_5_added_to_stm), "see test 13")
  assert_true(stm.add(node_6, time_node_6_added_to_stm), "see test 14")
  assert_true(stm.add(node_7, time_node_7_added_to_stm), "see test 15")
  assert_true(stm.add(node_8, time_node_8_added_to_stm), "see test 16")
  assert_true(stm.add(node_2, time_node_2_added_to_stm_again), "see test 17")
  
  #####################################
  ### Stm.replaceHypothesis() TESTS ###
  #####################################
  
  # Replace node 5 (the latest hypothesis) with node 7.  Since node 7 is already
  # present in the latest version of STM, it will be moved from position 3 to
  # position 1 and STM will only contain 3 items now (rather than 4) due to
  # node 5 being removed:
  #
  # STM item 1 (hypothesis): node 7
  # STM item 2: node 2
  # STM item 3: node 8
  time_hypothesis_replaced = time_node_2_added_to_stm_again + 5
  assert_true(stm.replaceHypothesis(node_7, time_hypothesis_replaced), "see test 18")
  
  #########################
  ### Stm.clear() TESTS ###
  #########################
  
  time_stm_cleared = time_hypothesis_replaced + 5
  assert_true(stm.clear(time_stm_cleared), "see test 19")

  ######################
  ### Stm.iterator() ###
  ######################
  
  # Add node_3 then node_7 again and see if the correct time state is returned 
  # (this creates a unique STM state and allows for unambiguous assertion that 
  # the iterator returns the most recent state of STM).
  time_node_3_added_again = time_stm_cleared + 5
  time_node_7_added_again = time_node_3_added_again + 5
  assert_true(stm.add(node_3, time_node_3_added_again), "see test 20")
  assert_true(stm.add(node_7, time_node_7_added_again), "see test 21")
  stm_it = stm.iterator()
  
  i = 0
  while stm_it.hasNext()
    expected_node = nil
    
    i == 0 ? expected_node = node_7 : expected_node = node_3
    
    assert_equal(expected_node, stm_it.next(), "see test 22")
    i+=1
  end
  
  ###############################
  ### Stm.setCapacity() TESTS ###
  ###############################
  
  time_stm_capacity_modified_first = time_node_7_added_again + 5
  time_stm_capacity_modified_second = time_stm_capacity_modified_first + 5
  assert_true(stm.setCapacity(1, time_stm_capacity_modified_first), "see test 23")
  assert_true(stm.setCapacity(6, time_stm_capacity_modified_second), "see test 24")
  
  #===============================#
  #==== GETTER FUNCTION TESTS ====#
  #===============================#
  
  final_event_time = time_stm_capacity_modified_second
  
  #########################
  ### Stm.getContents() ###
  #########################
  
  for time in model_creation_time..(final_event_time + 5)
    expected_stm_contents = ArrayList.new()
    time_description = ""
    
    if time < (time_stm_created - 1)
      expected_stm_contents = nil
      time_description = "before STM created"
    elsif time >= (time_stm_created - 1) and time < time_node_1_added_to_stm
      # expected_stm_contents should be empty i.e remain unchanged
      time_description = "after STM created but before node 1 added to STM"
    elsif time >= time_node_1_added_to_stm and time < time_node_2_added_to_stm
      expected_stm_contents.add(node_1)
      time_description = "after node 1 added to STM but before node 2 added to STM"
    elsif time >= time_node_2_added_to_stm and time < time_node_3_added_to_stm
      expected_stm_contents.add(node_2)
      expected_stm_contents.add(node_1)
      time_description = "after node 2 added to STM but before node 3 added to STM"
    elsif time >= time_node_3_added_to_stm and time < time_node_4_added_to_stm
      expected_stm_contents.add(node_3)
      expected_stm_contents.add(node_2)
      expected_stm_contents.add(node_1)
      time_description = "after node 3 added to STM but before node 4 added to STM"
    elsif time >= time_node_4_added_to_stm and time < time_node_5_added_to_stm
      expected_stm_contents.add(node_4)
      expected_stm_contents.add(node_3)
      expected_stm_contents.add(node_2)
      expected_stm_contents.add(node_1)
      time_description = "after node 4 added to STM but before node 5 added to STM"
    elsif time >= time_node_5_added_to_stm and time < time_node_6_added_to_stm
      expected_stm_contents.add(node_5)
      expected_stm_contents.add(node_4)
      expected_stm_contents.add(node_3)
      expected_stm_contents.add(node_2)
      time_description = "after node 5 added to STM but before node 6 added to STM"
    elsif time >= time_node_6_added_to_stm and time < time_node_7_added_to_stm
      expected_stm_contents.add(node_6)
      expected_stm_contents.add(node_5)
      expected_stm_contents.add(node_4)
      expected_stm_contents.add(node_3)
      time_description = "after node 6 added to STM but before node 7 added to STM"
    elsif time >= time_node_7_added_to_stm and time < time_node_8_added_to_stm
      expected_stm_contents.add(node_7)
      expected_stm_contents.add(node_6)
      expected_stm_contents.add(node_5)
      expected_stm_contents.add(node_4)
      time_description = "after node 7 added to STM but before node 8 added to STM"
    elsif time >= time_node_8_added_to_stm and time < time_node_2_added_to_stm_again
      expected_stm_contents.add(node_8)
      expected_stm_contents.add(node_7)
      expected_stm_contents.add(node_6)
      expected_stm_contents.add(node_5)
      time_description = "after node 8 added to STM but before node 2 is added to STM again"
    elsif time >= time_node_2_added_to_stm_again and time < time_hypothesis_replaced
      expected_stm_contents.add(node_5)
      expected_stm_contents.add(node_2)
      expected_stm_contents.add(node_8)
      expected_stm_contents.add(node_7)
      time_description = "after node 2 is added to STM again but before the hypothesis is replaced"
    elsif time >= time_hypothesis_replaced and time < time_stm_cleared
      expected_stm_contents.add(node_7)
      expected_stm_contents.add(node_2)
      expected_stm_contents.add(node_8)
      time_description = "after the hypothesis is replaced but before STM is cleared"
    elsif time >= time_stm_cleared and time < time_node_3_added_again
      # expected_stm_contents should be empty i.e remain unchanged.
      time_description = "after STM is cleared but before node 3 is added to STM again"
    elsif time >= time_node_3_added_again and time < time_node_7_added_again
      expected_stm_contents.add(node_3)
      time_description = "after node 3 is added to STM again but before node 7 is added to STM again"
    elsif time >= time_node_7_added_again and time < time_stm_capacity_modified_first
      expected_stm_contents.add(node_7)
      expected_stm_contents.add(node_3)
      time_description = "after node 3 is added to STM again but before STM capacity is modified"
    elsif time >= time_stm_capacity_modified_first
      # expected_stm_contents should be empty i.e remain unchanged.  This is 
      # because, after the STM capacity is changed for the first time, no nodes
      # are added.
      time_description = "after STM capacity is modified"
    end
    
    assert_equal(expected_stm_contents, stm.getContents(time), "see test 29 at time " + time.to_s + " (" + time_description + ")")
  end
  
  ######################
  ### Stm.getCount() ###
  ######################
  
  for time in model_creation_time..(final_event_time + 5)
    expected_stm_count = nil
    time_description = ""
    
    if time < (time_stm_created - 1)
      # expected_stm_count should be nil i.e. remain unchanged
      time_description = "before STM created"
    elsif time >= (time_stm_created - 1) and time < time_node_1_added_to_stm
      expected_stm_count = 0
      time_description = "after STM created but before node 1 added to STM"
    elsif time >= time_node_1_added_to_stm and time < time_node_2_added_to_stm
      expected_stm_count = 1
      time_description = "after node 1 added to STM but before node 2 added to STM"
    elsif time >= time_node_2_added_to_stm and time < time_node_3_added_to_stm
      expected_stm_count = 2
      time_description = "after node 2 added to STM but before node 3 added to STM"
    elsif time >= time_node_3_added_to_stm and time < time_node_4_added_to_stm
      expected_stm_count = 3
      time_description = "after node 3 added to STM but before node 4 added to STM"
    elsif time >= time_node_4_added_to_stm and time < time_hypothesis_replaced
      expected_stm_count = 4
      time_description = "after node 4 added to STM but before hypothesis replaced"
    elsif time >= time_hypothesis_replaced and time < time_stm_cleared
      expected_stm_count = 3
      time_description = "after hypothesis replaced but before STM cleared"
    elsif time >= time_stm_cleared and time < time_node_3_added_again
      expected_stm_count = 0
      time_description = "after STM cleared but before node 3 added to STM again"
    elsif time >= time_node_3_added_again and time < time_node_7_added_again
      expected_stm_count = 1
      time_description = "after node 3 added to STM again but before node 7 added to STM again"
    elsif time >= time_node_7_added_again and time < time_stm_capacity_modified_first
      expected_stm_count = 2
      time_description = "after node 7 added to STM again but before STM capacity is modified"
    elsif time >= time_stm_capacity_modified_first 
      expected_stm_count = 0
      time_description = "after STM capacity is modified"
    end
    
    assert_equal(expected_stm_count, stm.getCount(time), "see test 30 at time " + time.to_s + "(" + time_description + ")")
  end
  
  ######################
  #### Stm.getItem() ###
  ######################
  
  for time in model_creation_time..(final_event_time + 5)
    expected_stm_items = nil
    time_description = ""
    
    if time < (time_stm_created - 1)
      # expected_stm_items should be nil i.e remain unchanged
      time_description = "before STM created"
    elsif time >= (time_stm_created - 1) and time < time_node_1_added_to_stm
      expected_stm_items = []
      time_description = "after STM created but before node 1 added to STM"
    elsif time >= time_node_1_added_to_stm and time < time_node_2_added_to_stm
      expected_stm_items = [node_1]
      time_description = "after node 1 added to STM but before node 2 added to STM"
    elsif time >= time_node_2_added_to_stm and time < time_node_3_added_to_stm
      expected_stm_items = [node_2, node_1]
      time_description = "after node 2 added to STM but before node 3 added to STM"
    elsif time >= time_node_3_added_to_stm and time < time_node_4_added_to_stm
      expected_stm_items = [node_3, node_2, node_1]
      time_description = "after node 3 added to STM but before node 4 added to STM"
    elsif time >= time_node_4_added_to_stm and time < time_node_5_added_to_stm
      expected_stm_items = [node_4, node_3, node_2, node_1]
      time_description = "after node 4 added to STM but before node 5 added to STM"
    elsif time >= time_node_5_added_to_stm and time < time_node_6_added_to_stm
      expected_stm_items = [node_5, node_4, node_3, node_2]
      time_description = "after node 5 added to STM but before node 6 added to STM"
    elsif time >= time_node_6_added_to_stm and time < time_node_7_added_to_stm
      expected_stm_items = [node_6, node_5, node_4, node_3]
      time_description = "after node 6 added to STM but before node 7 added to STM"
    elsif time >= time_node_7_added_to_stm and time < time_node_8_added_to_stm
      expected_stm_items = [node_7, node_6, node_5, node_4]
      time_description = "after node 7 added to STM but before node 8 added to STM"
    elsif time >= time_node_8_added_to_stm and time < time_node_2_added_to_stm_again
      expected_stm_items = [node_8, node_7, node_6, node_5]
      time_description = "after node 8 added to STM but before node 2 is added again to STM"
    elsif time >= time_node_2_added_to_stm_again and time < time_hypothesis_replaced
      expected_stm_items = [node_5, node_2, node_8, node_7]
      time_description = "after node 2 is added again to STM but before the hypothesis is replaced"
    elsif time >= time_hypothesis_replaced and time < time_stm_cleared
      expected_stm_items = [node_7, node_2, node_8]
      time_description = "after the hypothesis is replaced but before STM is cleared"
    elsif time >= time_stm_cleared and time < time_node_3_added_again
      expected_stm_items = []
      time_description = "after STM is cleared but before node 3 is added to STM again"
    elsif time >= time_node_3_added_again and time < time_node_7_added_again
      expected_stm_items = [node_3]
      time_description = "after node 3 is added to STM again but before node 7 is added to STM again"
    elsif time >= time_node_7_added_again and time < time_stm_capacity_modified_first
      expected_stm_items = [node_7, node_3]
      time_description = "after node 7 is added to STM again but before STM capacity is modified"
    elsif time >= time_stm_capacity_modified_first
      expected_stm_items = []
      time_description = "after STM capacity is modified"
    end
    
    stm_items_at_time = stm.getContents(time)
    
    if stm_items_at_time == nil
      assert_equal(expected_stm_items, stm_items_at_time, "see test 31 at time " + time.to_s + "(" + time_description +")")
    else
      #This should throw a null pointer type error if the arrays returned are not 
      #of an equal size (desired behaviour since this will indicate that something
      #is wrong: either too many or too little expected items).
      for i in 0...[stm_items_at_time.size(), expected_stm_items.size()].max
        assert_equal(expected_stm_items[i], stm_items_at_time.get(i), "see test 32 at time " + time.to_s + "(" + time_description +")")
      end
    end
  end
end
