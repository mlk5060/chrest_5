################################################################################
# Tests for functions in the jchrest.architecture.Node class.
#
# Note that the following functions are not explicitly tested but are used in
# the testing of other, more complex functions so they are implicitly tested:
#
# - getContents()
# - getReference()
# - isRootNode()
################################################################################

################################################################################
# Tests that root nodes are constructed as expected and relevant parameters of
# the associated CHREST model are as expected.
# 
# NOTE: the root node constructor in jchrest.architecture.Node should only be 
#       invoked by a new jchrest.architecture.Chrest instance so create a new 
#       jchrest.architecture.Chrest instance and use relevant functions to get
#       the LTM modality root nodes.
process_test "root node constructor" do
  creation_time = 0
  model = Chrest.new(creation_time, false)
  
  modality_root_nodes = Array.new
  Modality.values().each do |modality|
    root_node = model.getLtmModalityRootNode(modality)
    modality_root_nodes.push(root_node)
    assert_equal(modality, root_node.getModality(), "when checking the modality of node with ref " + root_node.getReference().to_s)
  end
  
  #The total number of LTM nodes should be 0 since modality root nodes are not
  #included in the count.
  assert_equal(0, model.getLtmSize(creation_time), "when checking total number of LTM nodes")
  assert_equal(3, model.getNextLtmNodeReference(), "when checking next node reference")
  
  modality_root_nodes.each do |modality_root_node|
    modality = modality_root_node.getModality()
    modality_str = modality.toString()
    
    expected_ref = 0
    expected_contents = Pattern.makeList(["Root"].to_java(:String), modality).toString()
    expected_image = Pattern.makeList(["Root"].to_java(:String), modality).toString()
    
    if modality == Modality::VERBAL
      expected_ref = 1
    elsif modality == Modality::ACTION
      expected_ref = 2
    end
    
    assert_true(modality_root_node.isRootNode(), "when checking if " + modality_str + " root node is a root node")
    assert_equal(expected_ref, modality_root_node.getReference(), "when checking reference of " + modality_str + " root node")
    assert_equal(expected_contents, modality_root_node.getContents().toString(), "when checking contents of " + modality_str + " root node")
    assert_equal(expected_image, modality_root_node.getImage(creation_time).toString(), "when checking image of " + modality_str + " root node")
    assert_equal(creation_time, modality_root_node.getCreationTime(), "when checking creation time of " + modality_str + " root node")
  end
end

################################################################################
# Tests that the non-root node constructor acts as expected.  This test is split
# into two sub-tests:
#
# 1. Node should be constructed when requested.
# 2. Node should not be constructed when requested.
process_test "non-root node constructor" do
  
  Chrest.class_eval{
    field_accessor :_nextLtmNodeReference
  }
  
  model_creation_time = 0
  model = Chrest.new(model_creation_time, false) # Modality root nodes will have 
                                                 # been constructed now too.
  time = model_creation_time
  Modality.values().each do |modality|
    
    ##################
    ### SUB-TEST 1 ###
    ##################

    # Node construction request should be served since the creation time of the
    # new Node will be greater than or equal to the time the associated model 
    # was created.
  
    error_thrown = false
    begin
      node_creation_time = (time += 1)
      node_contents = Pattern.makeList(["content"].to_java(:String), modality)
      node_image = Pattern.makeList(["image"].to_java(:String), modality)

      node = Node.new(
        model,
        node_contents,
        node_image,
        node_creation_time
      )
      model._nextLtmNodeReference += 1

      assert_false(node.isRootNode(), "when checking if node is a root node")
      assert_equal(node_contents, node.getContents(), "when checking contents of node")
      assert_equal(node_image, node.getImage(node_creation_time), "when checking image of node")
      assert_equal(node_creation_time, node.getCreationTime(), "when checking creation time of node")
      assert_equal(modality, node.getModality(), "when checking the modality of node")
    rescue
      error_thrown = true
    end

    assert_false(
      error_thrown, 
      "when checking if an error is thrown when the Node is constructed at/after " +  
      "the time its associated model was created"
    )
  
    ##################
    ### SUB-TEST 2 ###
    ##################

    # Node construction request should not be served since the creation time of 
    # the new Node will be less than the time the associated model was created.
    begin
      node = Node.new(
        model,
        Pattern.makeList(["content"].to_java(:String), Modality::VISUAL),
        Pattern.makeList(["image"].to_java(:String), Modality::VISUAL),
        model_creation_time - 1
      )
      model._nextLtmNodeReference += 1
    rescue
      error_thrown = true
    end
    assert_true(
      error_thrown, 
      "when checking if an error is thrown when the Node is constructed before " + 
      "the time its associated model was created"
    )
  end
end

################################################################################
unit_test "size" do
  
  #############
  ### SETUP ###
  #############
  
  Chrest.class_eval {
    field_accessor :_nextLtmNodeReference
  }
  
  #Node.addChild() needs to be made publicly accessible
  add_child = Node.java_class.declared_method(:addChild, ListPattern, Node, Java::int, java.lang.String)
  add_child.accessible = true
  
  # Setup CHREST model
  model_creation_time = 0
  model = Chrest.new(model_creation_time, false)
  
  #Setup nodes.
  visual_root_node = model.getLtmModalityRootNode(Modality::VISUAL)
  
  node_1_contents = Pattern.makeVisualList(["1"].to_java(:String))
  node_1_image = Pattern.makeVisualList(["1"].to_java(:String))
  node_1_creation_time = model_creation_time + 1
  node_1 = Node.new(model, node_1_contents, node_1_image, node_1_creation_time)
  model._nextLtmNodeReference += 1
  
  node_2_contents = Pattern.makeVisualList(["2"].to_java(:String))
  node_2_image = Pattern.makeVisualList(["2"].to_java(:String))
  node_2_creation_time = node_1_creation_time + 1
  node_2 = Node.new(model, node_2_contents, node_2_image, node_2_creation_time)
  model._nextLtmNodeReference += 1
  
  node_3_contents = Pattern.makeVisualList(["3"].to_java(:String))
  node_3_image = Pattern.makeVisualList(["3"].to_java(:String))
  node_3_creation_time = node_2_creation_time + 1
  node_3 = Node.new(model, node_3_contents, node_3_image, node_3_creation_time)
  model._nextLtmNodeReference += 1
  
  add_node_1_as_child_to_visual_root_time = node_3_creation_time + 5
  visual_root_node.addChild(
    node_1_contents,
    node_1,
    add_node_1_as_child_to_visual_root_time,
    ""
  )
  
  add_node_2_as_child_to_node_1_time = add_node_1_as_child_to_visual_root_time + 5
  add_child.invoke(
    node_1,
    node_2_contents,
    node_2,
    add_node_2_as_child_to_node_1_time,
    ""
  )
  
  add_node_3_as_child_to_node_2_time = add_node_2_as_child_to_node_1_time + 5
  add_child.invoke(
    node_2,
    node_3_contents,
    node_3,
    add_node_3_as_child_to_node_2_time,
    ""
  )
  
  #############
  ### TESTS ###
  #############
  
  # Check sizes for visual root node.
  for time in (model_creation_time - 1)..add_node_3_as_child_to_node_2_time + 5
    if time < model_creation_time
      assert_equal(0, visual_root_node.size(time), "occurred when checking the size of the network from the visual root node before the visual root node is created.")
    elsif time >= model_creation_time and time < add_node_1_as_child_to_visual_root_time
      assert_equal(1, visual_root_node.size(time), "occurred when checking the size of the network from the visual root node after the visual root node is created but before any nodes are added as children.")
    elsif time >= add_node_1_as_child_to_visual_root_time and time < add_node_2_as_child_to_node_1_time
      assert_equal(2, visual_root_node.size(time), "occurred when checking the size of the network from the visual root node after node 1 has been added as a child to the visual root node")
    elsif time >= add_node_2_as_child_to_node_1_time and time < add_node_3_as_child_to_node_2_time
      assert_equal(3, visual_root_node.size(time), "occurred when checking the size of the network from the visual root node after node 2 has been added as a child to node 1")
    elsif time >= add_node_3_as_child_to_node_2_time
      assert_equal(4, visual_root_node.size(time), "occurred when checking the size of the network from the visual root node after node 3 has been added as a child to node 2")
    end
  end
  
  # Check sizes for node 1
  for time in model_creation_time..add_node_3_as_child_to_node_2_time + 5
    if time < node_1_creation_time
      assert_equal(0, node_1.size(time), "occurred when checking the size of the network from node 1 before node 1 is created")
    elsif time >= node_1_creation_time and time < add_node_2_as_child_to_node_1_time
      assert_equal(1, node_1.size(time), "occurred when checking the size of the network from node 1 after node 1 is created but before any children are added to it")
    elsif time >= add_node_2_as_child_to_node_1_time and time < add_node_3_as_child_to_node_2_time
      assert_equal(2, node_1.size(time), "occurred when checking the size of the network from node 1 after node 2 has been added as a child to node 1")
    elsif time >= add_node_3_as_child_to_node_2_time
      assert_equal(3, node_1.size(time), "occurred when checking the size of the network from node 1 after node 3 has been added as a child to node 2")
    end
  end
  
  # Check sizes for node 2
  for time in model_creation_time..add_node_3_as_child_to_node_2_time + 5
    if time < node_2_creation_time
      assert_equal(0, node_2.size(time), "occurred when checking the size of the network from node 2 before node 2 is created")
    elsif time >= node_2_creation_time and time < add_node_3_as_child_to_node_2_time
      assert_equal(1, node_2.size(time), "occurred when checking the size of the network from node 2 after node 2 is created but before any children are added to it")
    elsif time >= add_node_3_as_child_to_node_2_time
      assert_equal(2, node_2.size(time), "occurred when checking the size of the network from node 2 after node 3 has been added as a child to node 2")
    end
  end
  
  # Check sizes for node 3
  for time in model_creation_time..add_node_3_as_child_to_node_2_time + 5
    if time < node_3_creation_time
      assert_equal(0, node_3.size(time), "occurred when checking the size of the network from node 3 before node 3 is created")
    elsif time >= node_3_creation_time
      assert_equal(1, node_3.size(time), "occurred when checking the size of the network from node 3 after node 3 is created")
    end
  end
end

################################################################################
# Tests functions concerning the getting and setting of children for a Node.
#
# Crucially, these functions use times specified to determine how to act so a 
# large majority of the test is devoted to passing different timings to these
# functions and analysing the output.
process_test "child functionality" do
  
  #############
  ### SETUP ###
  #############
  
  Chrest.class_eval{
    field_accessor :_nextLtmNodeReference
  }
  
  # The "Node.addChild()" method is overloaded with 2 variations: one with an 
  # extended number of parameters (ext_add_child below), the other with a 
  # restricted number of parameters (res_add_child below) and both have private 
  # access in Node.  So, to run the tests, these methods need to be made public.
  ext_add_child = Node.java_class.declared_method(:addChild, ListPattern, Node, Java::int, java.lang.String)
  res_add_child = Node.java_class.declared_method(:addChild, ListPattern, Java::int)
  ext_add_child.accessible = true
  res_add_child.accessible = true
  
  # Setup the model and Nodes to use.
  model_creation_time = 0
  model = Chrest.new(model_creation_time, false)
  
  parent_node_creation_time = model_creation_time + 1
  parent_node_contents = Pattern.makeList(["parent_contents"].to_java(:String), Modality::VISUAL)
  parent_node_image = Pattern.makeList(["parent_image"].to_java(:String), Modality::VISUAL)
  parent_node = Node.new(
    model,
    parent_node_contents,
    parent_node_image,
    parent_node_creation_time
  )
  model._nextLtmNodeReference += 1
  
  #Child is invalid since it is not the same Modality as the parent.
  invalid_child_creation_time = parent_node_creation_time + 1
  invalid_child_contents = Pattern.makeList(["invalid_child_contents"].to_java(:String), Modality::ACTION)
  invalid_child_image = Pattern.makeList(["invalid_child_image"].to_java(:String), Modality::ACTION)
  invalid_child = Node.new(
    model,
    invalid_child_contents,
    invalid_child_image,
    invalid_child_creation_time
  )
  # Note that model._nextLtmNodeReference isn't incremented by 1 here since, 
  # during regular CHREST model operation, this Node would not be added as a 
  # child and, consequently, the model's "_nextLtmNodeReference" variable would
  # not be incremented by 1.  So, the reference for the Node created above and
  # below are the same.
  
  child_node_1_creation_time = parent_node_creation_time + 1
  child_node_1_contents = Pattern.makeList(["child_1_contents"].to_java(:String), Modality::VISUAL)
  child_node_1_image = Pattern.makeList(["child_1_image"].to_java(:String), Modality::VISUAL)
  child_node_1 = Node.new(
    model,
    child_node_1_contents,
    child_node_1_image,
    child_node_1_creation_time
  )
  model._nextLtmNodeReference += 1
  
  child_node_2_creation_time = child_node_1_creation_time + 1
  child_node_2_contents = Pattern.makeList(["child_2_contents"].to_java(:String), Modality::VISUAL)
  child_node_2_image = Pattern.makeList(["child_2_image"].to_java(:String), Modality::VISUAL)
  child_node_2 = Node.new(
    model,
    child_node_2_contents,
    child_node_2_image,
    child_node_2_creation_time
  )
  model._nextLtmNodeReference += 1
  
  
  # Reset the model's "_nextLtmReference" variable to 4, not 3, as expected.  To
  # explain: when the model is constructed, the modality root Nodes for LTM are
  # constructed and the model's "_nextLtmReference" variable is incremented 
  # accordingly.  This means that, when the parent Node is constructed, it is 
  # assigned the reference 3.  However, this test doesn't add the parent Node to
  # the relevant modality root Node using "Node.addChild()" so the model's
  # "_nextLtmReference" variable is not incremented programatically.  Therefore,
  # the expected value of the model's "_nextLtmReference" variable when the first
  # child is added to the parent Node should be 4.  This is absolutely vital for
  # correct test progression since Node reference numbers determine whether 
  # child links should be added or not.
  model._nextLtmNodeReference -= 2
  
  # Setup times to use
  before_model_created_time = model_creation_time - 1
  after_model_created_but_before_parent_created_time = parent_node_creation_time - 2
  after_model_and_parent_created_but_before_children_created_time = child_node_1_creation_time - 1
  after_model_parent_and_child_1_created_but_before_child_2_created_time = child_node_2_creation_time - 1
  after_model_parent_and_both_child_nodes_created_time = child_node_2_creation_time
  restricted_parameter_function_invocation_time = child_node_2_creation_time + 3
  
  ###########################################################
  ### ADD CHILDREN USING BOTH VERSIONS OF Node.addChild() ###
  ###########################################################
  
  add_child_before_model_created = ext_add_child.invoke(parent_node, child_node_1_contents, child_node_1, before_model_created_time, "")
  add_child_after_model_created_but_before_parent_created = ext_add_child.invoke(parent_node, child_node_1_contents, child_node_1, after_model_created_but_before_parent_created_time, "")
  attempt_to_add_self_as_child = ext_add_child.invoke(parent_node, parent_node_contents, parent_node, parent_node_creation_time, "")
  add_invalid_child_after_model_parent_and_child_created = ext_add_child.invoke(parent_node, invalid_child_contents, invalid_child, invalid_child_creation_time, "")
  add_child_after_model_and_parent_created_but_before_child_1_created = ext_add_child.invoke(parent_node, child_node_1_contents, child_node_1, after_model_and_parent_created_but_before_children_created_time, "")
  
  add_child_after_model_parent_child_1_created_but_before_child_2_created = ext_add_child.invoke(parent_node, child_node_1_contents, child_node_1, after_model_parent_and_child_1_created_but_before_child_2_created_time, "")
  add_child_after_model_parent_and_both_child_nodes_created = ext_add_child.invoke(parent_node, child_node_2_contents, child_node_2, after_model_parent_and_both_child_nodes_created_time, "")
  add_child_when_already_a_child = ext_add_child.invoke(parent_node, child_node_1_contents, child_node_1, restricted_parameter_function_invocation_time - 1, "")
  attempt_to_rewrite_history = ext_add_child.invoke(parent_node, child_node_2_contents, child_node_2, after_model_parent_and_both_child_nodes_created_time, "")
  
  # The restricted parameter version of Node.addChild() just calls the extended
  # parameter version so, since all permutations of the extended parameter 
  # version's output are returned using the extended parameter invocations 
  # above, there's no need to save the result of the restricted parameter 
  # version invocation.
  test_for_third_child = Pattern.makeVisualList(["restricted"].to_java(:String))
  res_add_child.invoke(parent_node, test_for_third_child, restricted_parameter_function_invocation_time)
  
  assert_false(
    add_child_before_model_created, 
    "occurred when checking the success of attempting to add a child to a parent " +
    "Node before the CHREST model associated with these Nodes is created "
  )
  
  assert_false(
    add_child_after_model_created_but_before_parent_created, 
    "occurred when checking the success of attempting to add a child to a parent " +
    "Node after the CHREST model associated with these Nodes is created but " +
    "before the parent Node has been created"
  )
  
  assert_false(
    attempt_to_add_self_as_child, 
    "occurred when checking the success of attempting to add a node as " +
    "a child of itself at a valid time"
  )
  
  assert_false(
    add_invalid_child_after_model_parent_and_child_created, 
    "occurred when checking the success of attempting to add a child to a parent " +
    "Node after the CHREST model associated with these Nodes is created and the " +
    "parent Node has been created but the modality of the child Node is different " +
    "to the parent Node"
  )
  
  assert_false(
    add_child_after_model_and_parent_created_but_before_child_1_created, 
    "occurred when checking the success of attempting to add a child to a parent " +
    "Node after the CHREST model associated with these Nodes is created and the " +
    "parent Node has been created but before the child Node has been created"
  )
  
  assert_true(
    add_child_after_model_parent_child_1_created_but_before_child_2_created, 
    "occurred when checking the success of attempting to add a child to a parent " +
    "Node after the CHREST model associated with these Nodes and the Nodes " +
    "themselves have been created"
  )
  
  assert_true(
    add_child_after_model_parent_and_both_child_nodes_created, 
    "occurred when checking the success of attempting to add another child to a " +
    "parent Node after the CHREST model associated with these Nodes and the Nodes " +
    "themselves have been created"
  )
  
  # Test result of attempting to add a child that has already been added.
  assert_false(
    add_child_when_already_a_child, 
    "occurred when checking the success of attempting to add another child to a " +
    "parent Node after the CHREST model associated with these Nodes and the Nodes " +
    "themselves have been created but the child to add is already a child of the " +
    "parent"
  )
  
  # Test result of attempting to rewrite the child history of the node.
  assert_false(
    attempt_to_rewrite_history, 
    "occurred when checking the success of attempting to rewrite the child " +
    "history of the Node that has had a child added to it parent"
  )
  
  
  ######################
  ### GET TEST LINKS ###
  ######################
  
  children_before_model_created = parent_node.getChildren(before_model_created_time)
  children_after_model_created_but_before_parent_created = parent_node.getChildren(after_model_created_but_before_parent_created_time)
  children_after_model_and_parent_created_but_before_children_added = parent_node.getChildren(after_model_and_parent_created_but_before_children_created_time)
  children_after_first_child_added_to_parent = parent_node.getChildren(after_model_parent_and_child_1_created_but_before_child_2_created_time)
  children_after_two_children_added_to_parent = parent_node.getChildren(after_model_parent_and_both_child_nodes_created_time)
  children_after_adding_child_using_restricted_parameter_function = parent_node.getChildren(restricted_parameter_function_invocation_time)
  
  # Check numbers of test links returned at different times
  assert_equal(
    nil, 
    children_before_model_created, 
    "occurred when checking the number of children for a parent Node at a time " +
    "before the CHREST model associated with the parent Node has been created"
  )
  assert_equal(
    nil, 
    children_after_model_created_but_before_parent_created, 
    "occurred when checking the number of children for a parent Node at a time " +
    "after the CHREST model associated with the parent Node has been created but " +
    "before the parent Node has been created"
  )
  assert_equal(
    0, 
    children_after_model_and_parent_created_but_before_children_added.size(), 
    "occurred when checking the number of children for a parent Node at a time " +
    "after the CHREST model associated with the parent Node and the parent Node " +
    "have been created but before any children have been added to the parent Node"
  )
  assert_equal(
    1, 
    children_after_first_child_added_to_parent.size(), 
    "occurred when checking the number of children for a parent Node at a time " +
    "after the CHREST model associated with the parent Node and the parent Node " +
    "have been created and after one child has been added to the parent Node"
  )
  assert_equal(
    2, 
    children_after_two_children_added_to_parent.size(), 
    "occurred when checking the number of children for a parent Node at a time " +
    "after the CHREST model associated with the parent Node and the parent Node " +
    "have been created and after two children have been added to the parent Node"
  )
  assert_equal(
    3,
    children_after_adding_child_using_restricted_parameter_function.size(),
    "occurred when checking the number of children for a parent Node at a time " +
    "after the CHREST model associated with the parent Node and the parent Node " +
    "have been created and after three children have been added to the parent " +
    "Node"
  )
  
  # Test details of children returned at different times
  child_after_adding_one_child = children_after_first_child_added_to_parent.get(0)
  assert_equal(
    child_node_1, 
    child_after_adding_one_child.getChildNode(),
    "occurred when checking the Node added to the parent Node after attempting " +
    "to add the first child"
  )
  assert_equal(
    child_node_1_contents, 
    child_after_adding_one_child.getTest(),
    "occurred when checking the test of the Link added to the parent Node after " +
    "attempting to add the first child"
  )
  assert_equal(
    after_model_parent_and_child_1_created_but_before_child_2_created_time, 
    child_after_adding_one_child.getCreationTime(),
    "occurred when checking the creation time of the Link added to the parent " +
    "Node after attempting to add the first child"
  )
  assert_equal(
    "", 
    child_after_adding_one_child.getExperimentCreatedIn(),
    "occurred when checking the experiment name of the Link added to the parent " +
    "Node after attempting to add the first child"
  )
  
  first_child_after_adding_two_children = children_after_two_children_added_to_parent.get(0)
  second_child_after_adding_two_children = children_after_two_children_added_to_parent.get(1)
  assert_equal(
    child_node_2, 
    first_child_after_adding_two_children.getChildNode(),
    "occurred when checking the first child of the parent Node after attempting " +
    "to add a second child"
  )
  assert_equal(
    child_node_2_contents, 
    first_child_after_adding_two_children.getTest(),
    "occurred when checking the Link's test for the first child of the parent " +
    "Node after attempting to add a second child"
  )
  assert_equal(
    after_model_parent_and_both_child_nodes_created_time, 
    first_child_after_adding_two_children.getCreationTime(),
    "occurred when checking the Link's creation time for the first child of the " +
    "parent Node after attempting to add a second child"
  )
  assert_equal(
    "", 
    first_child_after_adding_two_children.getExperimentCreatedIn(),
    "occurred when checking the experiment name for the first child of the " +
    "parent Node after attempting to add a second child"
  )
  
  assert_equal(
    child_node_1, 
    second_child_after_adding_two_children.getChildNode(),
    "occurred when checking the second child of the parent Node after attempting " +
    "to add a second child"
  )
  assert_equal(
    child_node_1_contents, 
    second_child_after_adding_two_children.getTest(),
    "occurred when checking the Link's test for the second child of the parent " +
    "Node after attempting to add a second child"
  )
  assert_equal(
    after_model_parent_and_child_1_created_but_before_child_2_created_time, 
    second_child_after_adding_two_children.getCreationTime(),
    "occurred when checking the Link's creation time for the second child of the " +
    "parent Node after attempting to add a second child"
  )
  assert_equal(
    "", 
    second_child_after_adding_two_children.getExperimentCreatedIn(),
    "occurred when checking the experiment name for the second child of the " +
    "parent Node after attempting to add a second child"
  )
  
  first_child_after_adding_three_children = children_after_adding_child_using_restricted_parameter_function.get(0)
  second_child_after_adding_three_children = children_after_adding_child_using_restricted_parameter_function.get(1)
  third_child_after_adding_three_children = children_after_adding_child_using_restricted_parameter_function.get(2)
  assert_false(
    first_child_after_adding_three_children.getChildNode().isRootNode(),
    "occurred when checking if the third child Node added is a root Node"
  )
  assert_equal(
    6, # 3 modality root nodes created when CHREST created (0, 1, 2), parent 
       # node (3), invalid child (4) first and second children (4, 5), this node
       # (6).
    first_child_after_adding_three_children.getChildNode().getReference(),
    "occurred when checking the reference of the third child Node added"
  )
  assert_equal(
    parent_node_contents.append(test_for_third_child), 
    first_child_after_adding_three_children.getChildNode().getContents(),
    "occurred when checking contents of third child Node added"
  )
  assert_equal(
    parent_node_contents.append(test_for_third_child), 
    first_child_after_adding_three_children.getChildNode().getImage(restricted_parameter_function_invocation_time),
    "occurred when checking image of third child Node added"
  )
  assert_equal(
    restricted_parameter_function_invocation_time, 
    first_child_after_adding_three_children.getChildNode().getCreationTime(),
    "occurred when checking creation time of third child node added"
  )
  assert_equal(
    test_for_third_child, 
    first_child_after_adding_three_children.getTest(),
    "occurred when checking the Link's test for the first child of the parent " +
    "Node after attempting to add a third child"
  )
  assert_equal(
    restricted_parameter_function_invocation_time, 
    first_child_after_adding_three_children.getCreationTime(),
    "occurred when checking the Link's creation time for the first child of the " +
    "parent Node after attempting to add a third child"
  )
  assert_equal(
    "", 
    first_child_after_adding_three_children.getExperimentCreatedIn(),
    "occurred when checking the experiment name for the first child of the " +
    "parent Node after attempting to add a third child"
  )
  
  assert_equal(
    child_node_2, 
    second_child_after_adding_three_children.getChildNode(),
    "occurred when checking the second child of the parent Node after attempting " +
    "to add a third child"
  )
  assert_equal(
    child_node_2_contents, 
    second_child_after_adding_three_children.getTest(),
    "occurred when checking the Link's test for the second child of the parent " +
    "Node after attempting to add a third child"
  )
  assert_equal(
    after_model_parent_and_both_child_nodes_created_time, 
    second_child_after_adding_three_children.getCreationTime(),
    "occurred when checking the Link's creation time for the second child of the " +
    "parent Node after attempting to add a third child"
  )
  assert_equal(
    "", 
    second_child_after_adding_three_children.getExperimentCreatedIn(),
    "occurred when checking the experiment name for the second child of the " +
    "parent Node after attempting to add a third child"
  )
  
  assert_equal(
    child_node_1, 
    third_child_after_adding_three_children.getChildNode(),
    "occurred when checking the third child of the parent Node after attempting " +
    "to add a third child"
  )
  assert_equal(
    child_node_1_contents, 
    third_child_after_adding_three_children.getTest(),
    "occurred when checking the Link's test for the third child of the parent " +
    "Node after attempting to add a third child"
  )
  assert_equal(
    after_model_parent_and_child_1_created_but_before_child_2_created_time, 
    third_child_after_adding_three_children.getCreationTime(),
    "occurred when checking the Link's creation time for the third child of the " +
    "parent Node after attempting to add a third child"
  )
  assert_equal(
    "", 
    third_child_after_adding_three_children.getExperimentCreatedIn(),
    "occurred when checking the experiment name for the third child of the " +
    "parent Node after attempting to add a third child"
  )
end

################################################################################
# Tests functions that are concerned with getting, setting and extending the
# image of a Node.
#
process_test "image functionality" do
  
  #############
  ### SETUP ###
  #############
  
  # Node.setImage() has private access so, to test it, its accessibility must
  # be public.
  set_image = Node.java_class.declared_method(:setImage, ListPattern, Java::int)
  set_image.accessible = true
  
  # Create a CHREST model.
  model_creation_time = 0
  model = Chrest.new(model_creation_time, false)
  
  # Create a node within the CHREST model just created.
  node_creation_time = model_creation_time + 2
  node = Node.new(
    model,
    Pattern.makeVisualList(["contents"].to_java(:String)),
    Pattern.makeVisualList(["initial_image"].to_java(:String)),
    node_creation_time
  )
  
  # Create a new valid and invalid image.
  new_valid_image = Pattern.makeVisualList(["new_image"].to_java(:String))
  new_invalid_image = Pattern.makeActionList(["action_image"].to_java(:String))
  
  ########################
  ### INVOKE FUNCTIONS ###
  ########################
  
  # Attempt to set the image of the node created to the valid image (this should
  # succeed since the image is set at a time when the node exists, the node is
  # not a root node and the new image has the same modality as the node).
  new_image_set_at_time = node_creation_time + 2
  set_image.invoke(node, new_valid_image, new_image_set_at_time)
  
  # Try to overwrite the image just set with a ListPattern of a different 
  # modality (this should fail so the node's image should still be set to the
  # new one set above).
  new_image_set_at_time = node_creation_time + 2
  set_image.invoke(node, new_invalid_image, new_image_set_at_time)
  
  # Extend the image of the node by testing all possible inputs to the
  # execution conditional except the root node check in Node.extendImage().
  invalid_image_extension = Pattern.makeVerbalList(["_extended"].to_java(:String))
  valid_image_extension = Pattern.makeVisualList(["_extended"].to_java(:String))
  extend_image_at_invalid_time = node_creation_time - 1
  extend_image_at_valid_time = new_image_set_at_time + 2
  
  invalid_image_extension_invalid_time_result = node.extendImage(invalid_image_extension, extend_image_at_invalid_time)
  invalid_image_extension_valid_time_result = node.extendImage(invalid_image_extension, extend_image_at_valid_time)
  valid_image_extension_invalid_time_result = node.extendImage(valid_image_extension, extend_image_at_invalid_time)
  valid_image_extension_valid_time_result = node.extendImage(valid_image_extension, extend_image_at_valid_time)
  
  # Try to set and extend the image of all modality root nodes in the model with 
  # a ListPattern of the same modality (this should fail since root node images 
  # can not be altered).
  modality_extend_image_results = []
  Modality.values().each do |modality|
    root_node = model.getLtmModalityRootNode(modality)
    set_image.invoke(root_node, Pattern.makeList(["new"].to_java(:String), modality), model_creation_time)
    modality_extend_image_results.push([
      modality.toString(),
      root_node.extendImage(Pattern.makeList(["extension"].to_java(:String), modality), model_creation_time + 1)
    ])
  end
  
  #############
  ### TESTS ###
  #############
  
  # Check setImage() and getImage() function correctly.
  for time in model_creation_time..(extend_image_at_valid_time + 3)
    result = node.getImage(time)
    
    if time >= model_creation_time and time < (node_creation_time - 1)
      assert_equal(
        nil, 
        result, 
        "occurred when checking node image before node created"
      )
    elsif time >= (node_creation_time - 1) and time < new_image_set_at_time
      assert_equal(
        Pattern.makeVisualList(["initial_image"].to_java(:String)).toString(), 
        result.toString(), 
        "occurred when checking node image before/after node created but before image changed"
      )
    elsif time >= new_image_set_at_time and time < extend_image_at_valid_time
      assert_equal(
        Pattern.makeVisualList(["new_image"].to_java(:String)).toString(), 
        result.toString(), 
        "occurred when checking node image after node image changed but before node image extended"
      )
    elsif time >= extend_image_at_valid_time
      assert_equal(
        Pattern.makeVisualList(["new_image"].to_java(:String)).append(Pattern.makeVisualList(["_extended"].to_java(:String))).toString(), 
        result.toString(), 
        "occurred when checking node image after extension"
      )
    end
  end
  
  # Check that the output of Node.extendImage() is as expected.
  assert_false(invalid_image_extension_invalid_time_result, "occurred when attempting to extend a Node's image with an invalid ListPattern at an invalid time")
  assert_false(invalid_image_extension_valid_time_result, "occurred when attempting to extend a Node's image with an invalid ListPattern at a valid time")
  assert_false(valid_image_extension_invalid_time_result, "occurred when attempting to extend a Node's image with a valid ListPattern at an invalid time")
  assert_true(valid_image_extension_valid_time_result, "occurred when attempting to extend a Node's image with an valid ListPattern at a valid time")
  
  # Check that the root node images haven't been changed
  Modality.values().each do |modality|
    for time in model_creation_time..(model_creation_time + 1)
      assert_equal(
        Pattern.makeList(["Root"].to_java(:String), modality), 
        model.getLtmModalityRootNode(modality).getImage(time),
        "occurred when checking the image of the " + modality.toString() + " root node at time " + time.to_s
      )
    end
  end
  
  # Check that the extendImage() function produced expected results when invoked
  # on modality root nodes.
  modality_extend_image_results.each do |modality_and_result|
    modality = modality_and_result[0]
    result = modality_and_result[1]
    assert_false(result, "occurred when checking the result of attempting to extend the " + modality + " root Node's image at a valid time with a valid extension")
  end
  
end

################################################################################
# Tests for correct operation of the functions related to productions in the
# jchrest.architecture.Node class.
# 
process_test "production functionality" do
  
  ##################
  ### TEST SETUP ###
  ##################
  
  time = 0
  model = Chrest.new(time, false)
  
  visual_node_1_image_and_content = ListPattern.new(Modality::VISUAL)
  visual_node_1_image_and_content.add(ItemSquarePattern.new("T", 0, 1))
  visual_node_1_image_and_content.add(ItemSquarePattern.new("U", 0, 2))
  
  visual_node_2_image_and_content = ListPattern.new(Modality::VISUAL)
  visual_node_2_image_and_content.add(ItemSquarePattern.new("T", 0, 1))
  visual_node_2_image_and_content.add(ItemSquarePattern.new("V", 0, 2))
  
  action_node_1_image_and_content = ListPattern.new(Modality::ACTION)
  action_node_1_image_and_content.add(ItemSquarePattern.new("Push", 0, 0))
  
  action_node_2_image_and_content = ListPattern.new(Modality::ACTION)
  action_node_2_image_and_content.add(ItemSquarePattern.new("Pull", 0, 0))
  
  action_node_3_image_and_content = ListPattern.new(Modality::ACTION)
  action_node_2_image_and_content.add(ItemSquarePattern.new("Move", 1, 0))
  
  time_visual_node_1_created = time + 1
  visual_node_1 = Node.new(
    model,
    visual_node_1_image_and_content,
    visual_node_1_image_and_content,
    time_visual_node_1_created
  )
  
  time_action_node_1_created = time_visual_node_1_created + 1
  action_node_1 = Node.new(
    model,
    action_node_1_image_and_content,
    action_node_1_image_and_content,
    time_action_node_1_created
  )
  
  time_action_node_2_created = time_action_node_1_created + 1
  action_node_2 = Node.new(
    model,
    action_node_2_image_and_content,
     action_node_2_image_and_content,
    time_action_node_2_created
  )
  
  time_action_node_3_created = time_action_node_2_created + 1
  action_node_3 = Node.new(
    model,
    action_node_3_image_and_content,
    action_node_3_image_and_content,
    time_action_node_3_created
  )
  
  #################################
  ### "addProduction()" TESTING ###
  #################################
  
  # Test that each of the sub-conditions of the major conditional in the 
  # "Node.addProduction" function evaluate to false and block production 
  # creation correctly.
  assert_equal(
    ChrestStatus::LEARN_PRODUCTION_FAILED,
    visual_node_1.addProduction(action_node_1, time_visual_node_1_created - 1), 
    "occurred when attempting to add a production to the visual node before the visual node exists"
  )
  
  assert_equal(
    ChrestStatus::LEARN_PRODUCTION_FAILED,
    visual_node_1.addProduction(action_node_1, time_action_node_1_created - 1), 
    "occurred when attempting to add a production to the visual node before the action node exists"
  )
  
  assert_equal(
    ChrestStatus::LEARN_PRODUCTION_FAILED,
    action_node_1.addProduction(visual_node_1, time_action_node_1_created), 
    "occurred when attempting to add a visual node as a production to an action node"
  )
  
  Modality.values().each do |modality|
    if modality != Modality::VISUAL
      non_visual_node = Node.new(model, ListPattern.new(modality), ListPattern.new(modality), time_action_node_1_created)
      assert_equal(
        ChrestStatus::LEARN_PRODUCTION_FAILED,
        non_visual_node.addProduction(action_node_1, time_action_node_1_created), 
        "occurred when attempting to add a production and the source Node is not a visual Node"
      )
    end
    
    if modality != Modality::ACTION
      non_action_node = Node.new(model, ListPattern.new(modality), ListPattern.new(modality), time_action_node_1_created)
      assert_equal(
        ChrestStatus::LEARN_PRODUCTION_FAILED,
        visual_node_1.addProduction(non_action_node, time_action_node_1_created), 
        "occurred when attempting to add a production and the terminal Node is not an action Node"
      )
    end
    
    assert_equal(
      ChrestStatus::LEARN_PRODUCTION_FAILED,
      model.getLtmModalityRootNode(modality).addProduction(action_node_1, time_action_node_1_created),
      "occurred when attempting to add a production whose source is the " + modality.toString() + " root node"
    )
    assert_equal(
      ChrestStatus::LEARN_PRODUCTION_FAILED,
      visual_node_1.addProduction(model.getLtmModalityRootNode(modality), time_action_node_1_created),
      "occurred when attempting to add a production whose terminus is the " + modality.toString() + " root node"
    )
  end
  
  # Try to add two productions when no sub-condition of the major conditional in 
  # the "Node.addProduction" function should evaluate to false.
  time_first_production_added = time_action_node_2_created + 10
  time_second_production_added = time_first_production_added + 10
  
  assert_equal(
    ChrestStatus::LEARN_PRODUCTION_SUCCESSFUL,
    visual_node_1.addProduction(action_node_1, time_first_production_added), 
    "occurred when checking the result of adding the first production"
  )
  
  assert_equal(
    ChrestStatus::LEARN_PRODUCTION_SUCCESSFUL,
    visual_node_1.addProduction(action_node_2, time_second_production_added), 
    "occurred when checking the result of adding the second production"
  )
  
  # Try to add the same productions again
  time_first_production_added_again = time_second_production_added + 10
  time_second_production_added_again = time_first_production_added_again + 10
  
  assert_equal(
    ChrestStatus::PRODUCTION_ALREADY_LEARNED,
    visual_node_1.addProduction(action_node_1, time_first_production_added_again), 
    "occurred when checking the result of attempting to add the first production when it already exists"
  )
  
  assert_equal(
    ChrestStatus::PRODUCTION_ALREADY_LEARNED,
    visual_node_1.addProduction(action_node_2, time_second_production_added_again), 
    "occurred when checking the result of attempting to add the second production when it already exists"
  )
  
  #######################################
  ### "reinforceProduction()" TESTING ###
  #######################################
  
  time_first_production_reinforced = time_second_production_added + 100
  variables_to_calculate_reinforcement_value = [1.0, 0.5, 1.0, 1.0].to_java(:Double)
  
  # Test that each of the sub-conditions of the first conditional in the 
  # "Node.reinforceProduction" function evaluate to false and block production 
  # creation correctly (except for the rewrite history conditional: this can 
  # only be tested correctly after productions have been reinforced).
  #
  # NOTE: at this point, the CHREST model associated with the Nodes being used
  #       has not had its reinforcement learning theory set yet so the 
  #       conditional related to this should correctly evaluate to false.
  assert_false(visual_node_1.reinforceProduction(action_node_2, variables_to_calculate_reinforcement_value, time_visual_node_1_created - 1), "occurred when checking the result of calling 'reinforceProduction' before the visual and action node is created.")
  assert_false(visual_node_1.reinforceProduction(action_node_2, variables_to_calculate_reinforcement_value, time_action_node_2_created - 1), "occurred when checking the result of calling 'reinforceProduction' after the visual node is created but before the action node is created.")
  assert_false(action_node_2.reinforceProduction(visual_node_1, variables_to_calculate_reinforcement_value, time_action_node_2_created), "occurred when checking the result of calling 'reinforceProduction' and specifying the action node as the source of the production and the visual node as the terminus.")
  assert_false(visual_node_1.reinforceProduction(action_node_2, variables_to_calculate_reinforcement_value, time_first_production_reinforced), "occurred when checking the result of calling 'reinforceProduction' before the associated model's reinforcement learning theory has been set.")
  
  # Set the reinforcement learning theory of the CHREST model associated with 
  # the Nodes being used so that all sub-conditions of the minor-conditional in 
  # the "Node.reinforceProduction" function can be checked.
  model.setReinforcementLearningTheory(ReinforcementLearning::Theory::PROFIT_SHARING_WITH_DISCOUNT_RATE)
  assert_false(visual_node_1.reinforceProduction(action_node_3, variables_to_calculate_reinforcement_value, time_first_production_reinforced), "occurred when checking the result of calling 'reinforceProduction' and specifying a production to be reinforced for the visual node that doesn't exist.")
  
  # Check that the Node.reinforceProduction function returns true (allows 
  # production reinforcement) when the major and minor conditionals evaluate to 
  # true.
  assert_true(visual_node_1.reinforceProduction(action_node_2, variables_to_calculate_reinforcement_value, time_first_production_reinforced), "occurred when checking the result of calling 'reinforceProduction' and the reinforcement should occur.")
  
  ##################################
  ### "getProductions()" TESTING ###
  ##################################
  
  productions_before_visual_node_1_created = visual_node_1.getProductions(time_visual_node_1_created - 2)
  assert_equal(nil, productions_before_visual_node_1_created, "occurred when checking the productions present in a visual node before the visual node is created")
  
  productions_when_visual_node_1_created = visual_node_1.getProductions(time_visual_node_1_created - 1)
  assert_equal(LinkedHashMap.new(), productions_when_visual_node_1_created, "occurred when checking the productions present in a visual node when the visual node is created")
  
  productions_before_any_production_added = visual_node_1.getProductions(time_first_production_added - 1)
  assert_equal(0, productions_before_any_production_added.size(), "occurred when checking the number of productions present in a visual node before any productions are added")
  
  productions_after_first_production_added_but_before_second = visual_node_1.getProductions(time_second_production_added - 1)
  productions = productions_after_first_production_added_but_before_second.keySet()
  values = productions_after_first_production_added_but_before_second.values()
  assert_equal(1, productions_after_first_production_added_but_before_second.size(), "occurred when checking the number of productions after adding one production")
  assert_true(productions.contains(action_node_1), "occurred when checking if the first action node is present in the set of productions returned after adding one production")
  assert_true(values[0] == 1.0, "occurred when checking the value in the set of productions after adding one production")
  
  productions_after_second_production_added = visual_node_1.getProductions(time_second_production_added)
  productions = productions_after_second_production_added.keySet()
  values = productions_after_second_production_added.values()
  assert_equal(2, visual_node_1.getProductions(time_second_production_added).size(), "occurred when checking the number of productions after adding two productions")
  assert_true(productions.contains(action_node_1), "occurred when checking if the first action node is present in the set of productions returned after adding two productions")
  assert_true(productions.contains(action_node_2), "occurred when checking if the second action node is present in the set of productions returned after adding two productions")
  assert_true(values[0] == 1.0, "occurred when checking the first value in the set of productions returned after adding two productions")
  assert_true(values[1] == 1.0, "occurred when checking the second value in the set of productions returned after adding two productions")
  
  productions_and_values_before_reinforcement = visual_node_1.getProductions(time_first_production_reinforced - 1)
  productions_and_values_after_reinforcement = visual_node_1.getProductions(time_first_production_reinforced + 1)
  assert_equal(1.0, productions_and_values_before_reinforcement.get(action_node_2), "occurred when checking the value of the production that is reinforced before the reinforcement occurs")
  assert_equal(2.0, productions_and_values_after_reinforcement.get(action_node_2), "occurred when checking the value of the production that is reinforced after the reinforcement occurs")
end

################################################################################
process_test "semantic link functionality" do
  
  #############
  ### SETUP ###
  #############
  
  model_creation_time = 0
  model = Chrest.new(model_creation_time, false)
  
  node_1_creation_time = model_creation_time + 5
  node_1 = Node.new(
    model,
    Pattern.makeVisualList(["node_1"].to_java(:String)),
    Pattern.makeVisualList(["node_1"].to_java(:String)),
    node_1_creation_time
  )
  
  node_2_creation_time = node_1_creation_time + 5
  node_2 = Node.new(
    model,
    Pattern.makeVisualList(["node_2"].to_java(:String)),
    Pattern.makeVisualList(["node_2"].to_java(:String)),
    node_2_creation_time
  )
  
  node_3_creation_time = node_2_creation_time + 5
  node_3 = Node.new(
    model,
    Pattern.makeVisualList(["node_3"].to_java(:String)),
    Pattern.makeVisualList(["node_3"].to_java(:String)),
    node_3_creation_time
  )
  
  #############
  ### TESTS ###
  #############
  
  # Try to add a semantic link between the same node at a valid time.
  assert_false(
    node_1.addSemanticLink(node_1, node_1_creation_time),
    "occurred when attempting to semantically link a node to itself"
  )
  
  # Try to add semantic links at times when the source and terminus node do not
  # exist.  This will isolate the timing conditions in the major conditional of 
  # Node.addSemanticLink() and allow us to test if these conditions are 
  # operating as expected.
  assert_false(
    node_1.addSemanticLink(node_2, node_1_creation_time - 1), 
    "occurred when checking the result of attempting to add a semantic link " +
    "between two nodes when neither node exists"
  )
  assert_false(
    node_1.addSemanticLink(node_2, node_2_creation_time - 1),
    "occurred when checking the result of attempting to add a semantic link " +
    "between two nodes when the node to create the production from exists but " +
    "the node that the production goes to does not"
  )
  assert_false(
    node_2.addSemanticLink(node_1, node_2_creation_time - 1),
    "occurred when checking the result of attempting to add a semantic link " +
    "between two nodes when the node to create the production to exists but " +
    "the node that the production goes from does not"
  )
  
  # Neither node should have any semantic links in the time range specified (the 
  # max value is much greater than the time any attempt was made to add a 
  # semantic link to either node.
  for time in model_creation_time..(node_2_creation_time + 5)
    node_1_semantic_links_at_time = node_1.getSemanticLinks(time)
    node_2_semantic_links_at_time = node_2.getSemanticLinks(time)
    
    if time < (node_1_creation_time - 1)
      assert_equal(
        nil, 
        node_1_semantic_links_at_time, 
        "occurred when checking the semantic links of node_1 before it is created"
      )
    else
      assert_true(
        node_1_semantic_links_at_time.isEmpty(), 
        "occurred when checking the semantic links of node_1 after attempting to " +
        "add semantic links to non-root nodes that do not exist at the time " +
        "semantic link creation is requested"
      )
    end
    
    if time >= node_2_creation_time
      assert_true(
        node_2_semantic_links_at_time.isEmpty(),
        "occurred when checking the semantic links of node_2 after attempting to " +
        "add semantic links to non-root nodes that do not exist at the time " +
        "semantic link creation is requested"
      )
    end
  end
  
  # Try to add each modality root node as the terminus and source of a semantic
  # link at a time when both nodes in the semantic link have been created.  This
  # will isolate the root node conditions in the major conditional of 
  # Node.addSemanticLink() and allow us to test if these conditions are 
  # operating as expected.
  Modality.values().each do |modality|
    modality_root_node = model.getLtmModalityRootNode(modality)
    node_1.addSemanticLink(modality_root_node, node_1_creation_time + 1)
    modality_root_node.addSemanticLink(node_1, node_1_creation_time + 1)
  end
  
  # No modality root node or node_1 should have any semantic links in the time 
  # range specified (the max value is much greater than the time any attempt was 
  # made to add a semantic link to any of the nodes).
  for time in model_creation_time..(node_1_creation_time + 5)
    node_1_semantic_links_at_time = node_1.getSemanticLinks(time)
    
    Modality.values().each do |modality|
      modality_root_node = model.getLtmModalityRootNode(modality)
      
      if time < (node_1_creation_time - 1)
        assert_equal(
          nil,
          node_1_semantic_links_at_time,
          "occurred when checking the semantic links of node_1 at a time before " +
          "node_1 was created and after attempting to add semantic links to " +
          "modality root nodes at times where both the source and terminus " +
          "nodes of the semantic link exist"
        )
      else
        assert_true(
          node_1_semantic_links_at_time.isEmpty(),
          "occurred when checking the semantic links of node_1 when node_1 was " +
          "created or after node_1 was created and after attempting to add " +
          "semantic links to modality root nodes at times where both the " +
          "source and terminus nodes of the semantic link exist"
        )
      end
      
      assert_true(
        modality_root_node.getSemanticLinks(time).isEmpty(),
        "occurred when checking the semantic links of the " + modality.toString() +
        "root node after attempting to add semantic links to non-root nodes at " +
        "times where both the source and terminus nodes of the semantic link " +
        "exist"
      )
    end
  end
  
  # Add semantic links and pass parameters to the function so that addition will
  # be successful but there are time gaps between nodes being added.
  node_1_to_node_2_semantic_link_creation_time = node_3_creation_time + 20
  node_1_to_node_3_semantic_link_creation_time = node_3_creation_time + 25
  node_1.addSemanticLink(node_2, node_1_to_node_2_semantic_link_creation_time)
  node_1.addSemanticLink(node_3, node_1_to_node_3_semantic_link_creation_time)
  
  # Try to add an existing semantic link again after it was originally added
  final_time = node_1_to_node_3_semantic_link_creation_time + 10
  assert_false(
    node_1.addSemanticLink(node_2, final_time),
    "occurred when checking the result of trying to add a semantic link at " +
    "a time when it already exists for the node"
  )
  
  # Check that the semantic link history of node_1 is as expected
  for time in model_creation_time..(final_time + 1)
    semantic_links = node_1.getSemanticLinks(time)
    
    if time >= model_creation_time and time < (node_1_creation_time - 1)
      assert_equal(
        nil, 
        semantic_links,
        "occurred when checking the semantic links for node_1 before the node " +
        "was created"
      )
    elsif time >= (node_1_creation_time - 1) and time < node_1_to_node_2_semantic_link_creation_time
      assert_true(
        semantic_links.isEmpty(),
        "occurred when checking the number of semantic links for node_1 before " +
        "any semantic links are added"
      )
    elsif time >= node_1_to_node_2_semantic_link_creation_time and time < node_1_to_node_3_semantic_link_creation_time
      assert_equal(
        1, 
        semantic_links.size(),
        "occurred when checking the number of semantic links for node_1 after " +
        "one semantic link is added"
      )
      assert_equal(
        node_2, semantic_links.get(0),
        "occurred when checking the node that is semantically linked to " +
        "node_1 after one semantic link is added"
      )
    elsif time >= node_1_to_node_3_semantic_link_creation_time
      assert_equal(
        2, 
        semantic_links.size(),
        "occurred when checking the number of semantic links for node 1 after " +
        "two semantic links are added to it"
      )
      assert_equal(
        node_3, 
        semantic_links.get(0),
        "occurred when checking the first node that is semantically linked to " +
        "node_1 after two semantic links have been added"
      )
      assert_equal(
        node_2, 
        semantic_links.get(1),
        "occurred when checking the second node that is semantically linked to " +
        "node_1 after two semantic links have been added"
      )
    end
  end
end

process_test "named by functionality" do
  
  #####################
  ### TIMING SET-UP ###
  #####################
  
  model_creation_time = 0
  node_0_creation_time = 5
  node_1_creation_time = 25
  node_2_creation_time = 50
  node_3_creation_time = 75
  node_4_creation_time = 100
  
  ######################
  ### RESOURCE SETUP ###
  ######################
  
  model = Chrest.new(model_creation_time, false)
  
  node_0 = Node.new(
    model,
    Pattern.makeVerbalList(["node_0_c"].to_java(:String)),
    Pattern.makeVerbalList(["node_0_i"].to_java(:String)),
    node_0_creation_time
  )
  
  node_1 = Node.new(
    model,
    Pattern.makeVisualList(["node_1_c"].to_java(:String)),
    Pattern.makeVisualList(["node_1_i"].to_java(:String)),
    node_1_creation_time
  )
  
  node_2 = Node.new(
    model,
    Pattern.makeVisualList(["node_2_c"].to_java(:String)),
    Pattern.makeVisualList(["node_2_i"].to_java(:String)),
    node_2_creation_time
  )
  
  node_3 = Node.new(
    model,
    Pattern.makeVerbalList(["node_3_c"].to_java(:String)),
    Pattern.makeVerbalList(["node_3_i"].to_java(:String)),
    node_3_creation_time
  )
  
  node_4 = Node.new(
    model,
    Pattern.makeVerbalList(["node_4_c"].to_java(:String)),
    Pattern.makeVerbalList(["node_4_i"].to_java(:String)),
    node_4_creation_time
  )
  
  #############
  ### TESTS ###
  #############
  
  # Attempt to set node_1 named by node_0 before node_1 created but after node_0 
  # created (isolates creation time check on named node)
  assert_false(
    node_1.setNamedBy(node_0, 2),
    "occurred when checking the result of attempting to set that node_1 is " +
    "named by node_0 when node_0 exists but node_1 doesn't"
  )
  
  # Attempt to set node_1 named by node_3 after node_1 created but before node_3 
  # created (isolates creation time check on naming node)
  assert_false(
    node_1.setNamedBy(node_3, 26),
    "occurred when checking the result of attempting to set node_1 named by " +
    "node_3 when node_1 exists but node_3 doesn't"
  ) 
  
  time = 27
  Modality.values().each do |modality|
    
    # Set named by from each modality root node to node_0 when both nodes exist 
    # (isolates root node check on named node).
    assert_false(
      model.getLtmModalityRootNode(modality).setNamedBy(node_0, time+=1),
      "occurred when checking the result of attempting to set that the " + 
      modality.toString() + " root node is named by node_1 when both nodes exist"
    )
    
    # Set named by from node_1 to each modality root node when both nodes exist 
    # (isolates root node check on naming node)
    assert_false(
      node_1.setNamedBy(model.getLtmModalityRootNode(modality), time += 1),
      "occurred when checking the result of attempting to set that node_1 " + 
      "is named by the " + modality.toString() + " root node when both nodes " +
      "exist"
    )
    
    # For each modality (except visual), create a node of that modality and set 
    # that the node with the modality specified is named by a non-root verbal 
    # node at a time that both nodes exist (isolates visual modality check on 
    # named nodes)
    if(modality != Modality::VISUAL)
      modality_node = Node.new(model, ListPattern.new(modality), ListPattern.new(modality), time+= 1)
      verbal_node = Node.new(model, ListPattern.new(modality), ListPattern.new(modality), time += 1)
      assert_false(
        modality_node.setNamedBy(verbal_node, 4),
        "occurred when checking the result of attempting to set that a " + 
        modality.toString() + " root node is named by a non-root verbal node"
      )
      
      # To be sure, check the named by history of the modality node to ensure 
      # that no nodes name it at any point.
      for i in model_creation_time..time
        assert_equal(
          nil, 
          modality_node.getNamedBy(i),
          "occurred when checking the named by history of the " + 
          modality.toString() + " root node at time " + i.to_s + " after " +
          "attempting to set that this modality root node is named by a " +
          "non-root verbal node"
        )
      end
    end
    
    # For each modality (except verbal), create a node of that modality and a 
    # non-root visual node and set that the visual node is named by the node 
    # with the modality specified at a time that both nodes exist (isolates 
    # verbal modality check on naming nodes)
    if(modality != Modality::VERBAL)
      modality_node = Node.new(model, ListPattern.new(modality), ListPattern.new(modality), 2)
      visual_node = Node.new(model, ListPattern.new(modality), ListPattern.new(modality), 3)
      assert_false(
        visual_node.setNamedBy(modality_node, 4),
        "occurred when checking the result of attempting to set that a " + 
        "non-root visual node is named by a " + modality.toString() + " root " +
        "node"
      )
      
      # To be sure, check the named by history of the visual node to ensure that 
      # no nodes name it at any point.
      for i in model_creation_time..time
        assert_equal(
          nil, 
          visual_node.getNamedBy(i),
          "occurred when checking the named by history of a non-root visual " +
          "node at time " + i.to_s + " after attempting to set that this node " + 
          "is named by the " + modality.toString() + " root node"
        )
      end
    end
  end
  
  # invoke function with parameters so that the conditional passes and state that node_1 is named by node_4
  node_1_named_by_node_4_at_time = node_4_creation_time + 5
  assert_true(
    node_1.setNamedBy(node_4, node_1_named_by_node_4_at_time),
    "occurred when checking the result of attempting to set that node_1 is " + 
    "named by node_4"
  )
  
  # invoke function with parameters so that the conditional passes and state that node_1 is named by node_3
  node_1_named_by_node_3_at_time = node_1_named_by_node_4_at_time + 10
  assert_true(
    node_1.setNamedBy(node_3, node_1_named_by_node_3_at_time),
    "occurred when checking the result of attempting to set that node_1 is " + 
    "named by node_3"
  )
  
  # Finally, check the named by history of node_1
  for time in model_creation_time..(node_1_named_by_node_3_at_time + 5)
    node_1_named_by_at_time = node_1.getNamedBy(time)
    
    if time < node_1_named_by_node_4_at_time
      assert_equal(
        nil, 
        node_1_named_by_at_time,
        "occurred when checking the named by history of node_1 before the time " +
        "that node_1 was set to be named by node_4"
        )
    elsif time >= node_1_named_by_node_4_at_time and time < node_1_named_by_node_3_at_time
      assert_equal(
        node_4, 
        node_1_named_by_at_time,
        "occurred when checking the named by history of node_1 after the time " +
        "that node_1 was set to be named by node_4 but before it was set to be " +
        "named by node_3"
      )
    elsif time >= node_1_named_by_node_4_at_time
      assert_equal(
        node_3, 
        node_1_named_by_at_time,
        "occurred when checking the named by history of node_1 after the time " +
        "that node_1 was set to be named by node_3"
      )
    end
  end
end

################################################################################
#
# This set of tests ensures that all template functionality in the 
# jchrest.architecture.Node class operates correctly.  Since template 
# construction is complex, the test setup requires some explanation.
# 
# Five nodes are used in this set of tests however, only one becomes a template.
# Since template construction can only occur under certain conditions, the
# LTM network constructed only allows for one of the nodes, node 3, to become a 
# template.
# 
# The CHREST model used in this test initially has its template construction 
# parameters set to:
# 
# - Minimum depth of node in LTM: 0 
# - Minimum number of repeat primitive pattern occurrences in node image: 0 
# 
# These parameters are not usually allowed in normal conditions (must be >= 1)
# however, to isolate the root node check in the Node.makeTemplate() conditional
# this must be done.  After this check is performed, the parameters are set to:
# 
# - Minimum depth of node in LTM: 2
# - Minimum number of repeat item/position occurrences in aggregated node 
#   image: 3
# 
# 3 is used as the value of the second parameter since the image aggregation 
# functionality can then be tested fully since images are aggregated from the 
# node that has "makeTemplate()" invoked upon it, the node's children and any
# semantically linked to nodes.  This is also why node 3 has only 1 child and
# 1 semantic link to another node.
# 
# The long term memory network constructed in the CHREST model used in this test
# is illustrated below.  Note that:
# 
# 1. Node 1 is not able to be made into a template since it is only of depth 1
# 2. Node 2 is not able to be made into a template since no pattern in its 
#    image occurs 3 times, even after image aggreagtion (of which no additional
#    primitives are found since node 2 has no children and is not semantically
#    linked to any other nodes).
# 3. Node 3 is eligible to be made into a template since it is of depth 2 and 
#    has 3 occurrences of an item ("D") and a position ([2 0])
# 3. Node 4 is not able to be made into a template since it is only of depth 1
# 4. Node 5 is not able to be made into a template (of which no additional
#    primitives are found since node 2 has no children and is not semantically
#    linked to any other nodes).
#    
# The timeline of events is also illustrated below and is important in 
# understanding why certain tests have the results expected.
# 
# ===================
# === LTM Network ===
# ===================
# 
# - "o" denotes visual root node
# - "-" and "|" denotes standard links
# - "*" denotes a semantic link
# - "[]" denotes an ItemSquarePattern
# - "{}" denotes a test on a Link
# - "<>" denotes a Node image
# - "1/2/3/4/5:" denotes the node reference (in context of this test)
#
# o---{[A 0 0]}---< 1:[A 0 0]>---{[B 0 1]}---< 2:[A 0 0][B 0 1]>
#   |                          |
#   |                          --{[C 0 1]}---< 3:[A 0 0][C 0 1][D 1 1][E 2 0]>---{[F 9 0]}---< 5:[A 0 0][C 0 1][D 1 1][E 2 0][F 9 0][D 3 4][K 2 0]>
#   |                                                         *
#   --{Z 4 5}---< 4:[Z 4 5][D 7 8][G 2 0]>*********************
# 
# ================
# === Timeline ===
# ================
# 
# - Model created
# - Node 1 created
# - Node 1 becomes child of visual root
# - Node 2 created
# - Node 2 becomes child of node 1
# - Node 3 created
# - Node 3 becomes child of node 1
# - Node 4 created
# - Node 4 becomes child of visual root
# - Node 4 semantically linked to node 3
# - Node_5 created
# - Node_5 becomes child of node 3
# - Node_3 becomes template
# - Node_3 slots filled
# - Node_3 slots cleared
# - Node_3 slots filled again
# - Node_3 becomes non template
process_test "template functionality" do
  
  #############
  ### SETUP ###
  #############
  
  #Create model
  model_creation_time = 0
  model = Chrest.new(model_creation_time, false)
  
  # Create node 1 and add as child to visual modality root node.
  node_1_contents = ListPattern.new(Modality::VISUAL)
  node_1_contents.add(ItemSquarePattern.new("A", 0, 0))
  node_1_image = ListPattern.new(Modality::VISUAL)
  node_1_image.add(ItemSquarePattern.new("A", 0, 0))
  node_1_creation_time = model_creation_time + 5
  node_1 = Node.new(
    model,
    node_1_contents,
    node_1_image,
    node_1_creation_time
  )
  
  node_1_becomes_child_of_root_time = node_1_creation_time + 5
  model.getLtmModalityRootNode(Modality::VISUAL).addChild(node_1_contents, node_1, node_1_becomes_child_of_root_time, "")
  
  # Create node 2 and add as child to node_1.
  node_2_contents = node_1_contents.append(ItemSquarePattern.new("B", 0, 1))
  node_2_image = node_1_contents.append(ItemSquarePattern.new("B", 0, 1))
  node_2_creation_time = node_1_becomes_child_of_root_time + 5
  node_2 = Node.new(
    model,
    node_2_contents,
    node_2_image,
    node_2_creation_time
  )
  
  node_2_becomes_node_1_child_time = node_2_creation_time + 5
  node_1.addChild(node_2_contents, node_2, node_2_becomes_node_1_child_time, "")
  
  # Create node 3 and add as child to node 1
  node_3_contents = node_1_contents.append(ItemSquarePattern.new("C", 0, 1))
  node_3_image = node_1_contents.append(ItemSquarePattern.new("C", 0, 1))
  node_3_image.add(ItemSquarePattern.new("D", 1, 1))
  node_3_image.add(ItemSquarePattern.new("E", 2, 0))
  node_3_creation_time = node_2_becomes_node_1_child_time + 5
  node_3 = Node.new(
    model,
    node_3_contents,
    node_3_image,
    node_3_creation_time
  )
  
  node_3_added_to_node_1_at_time = node_3_creation_time + 5
  node_1.addChild(node_3_contents, node_3, node_3_added_to_node_1_at_time, "")
  
  # Create node 4, add as child to visual root and semantically link to node 3
  node_4_contents = ListPattern.new(Modality::VISUAL)
  node_4_contents.add(ItemSquarePattern.new("Z", 4, 5))
  node_4_image = ListPattern.new(Modality::VISUAL)
  node_4_image.add(ItemSquarePattern.new("Z", 4, 5))
  node_4_image.add(ItemSquarePattern.new("D", 7, 8))
  node_4_image.add(ItemSquarePattern.new("G", 2, 0))
  node_4_creation_time = node_3_creation_time + 5
  node_4 = Node.new(
    model,
    node_4_contents,
    node_4_image,
    node_4_creation_time
  )
  
  node_4_added_to_visual_root_at_time = node_4_creation_time + 5
  model.getLtmModalityRootNode(Modality::VISUAL).addChild(node_4_contents, node_4, node_4_added_to_visual_root_at_time, "")
  
  node_4_added_as_semantic_link_to_node_3_at_time = node_4_added_to_visual_root_at_time + 5
  node_3.addSemanticLink(node_4, node_4_added_as_semantic_link_to_node_3_at_time)
  
  # Create node 5 and add as child of node 3
  node_5_contents = node_3_contents.append(ItemSquarePattern.new("F", 9, 0))
  node_5_image = node_3_contents.append(ItemSquarePattern.new("F", 9, 0))
  node_5_image.add(ItemSquarePattern.new("D", 3, 4))
  node_5_image.add(ItemSquarePattern.new("K", 2, 0))
  node_5_creation_time = node_4_creation_time + 5
  node_5 = Node.new(
    model,
    node_5_contents,
    node_5_image,
    node_5_creation_time
  )
  
  node_5_added_as_child_to_node_3_at_time = node_4_added_as_semantic_link_to_node_3_at_time + 5
  node_3.addChild(node_5_contents, node_5, node_5_added_as_child_to_node_3_at_time, "")
    
  ##########################################################
  ### Node.canBeTemplate() AND Node.makeTemplate() TESTS ###
  ##########################################################
  
  error_msg = "occurred during Node.canBeTemplate() and Node.makeTemplate() tests "
  
  # Assert that root nodes can not be templates.
  min_template_depth_field = Chrest.java_class.declared_field("_minNodeDepthInNetworkToBeTemplate")
  min_item_position_occurrences = Chrest.java_class.declared_field("_minItemOrPositionOccurrencesInNodeImagesToBeSlotValue")
  
  min_template_depth_field.accessible = true
  min_item_position_occurrences.accessible = true
  
  min_template_depth_field.set_value(model, 0)
  min_item_position_occurrences.set_value(model, 0)
  
  Modality.values().each do |modality|
    assert_false(
      model.getLtmModalityRootNode(modality).canBeTemplate(5),
      error_msg + "(1) with " + modality.toString() + " modality root node"
    )
  end
  
  #Set template construction parameters now to enable isolation of other 
  #conditions
  model.setTemplateConstructionParameters(2, 3)
  
  # Assert that nodes that haven't yet been created can't be templates
  assert_false(
    node_2.canBeTemplate(node_2_creation_time - 1),
    error_msg + "(2)"
  )
  
  # Assert that node 3 can only become a template when node 5 is added as a
  # child to it since it has a depth of 2 but only node 5 is added as a child 
  # does it have enough repeated items/positions in its aggregated image.
  for time in model_creation_time..node_5_added_as_child_to_node_3_at_time + 5
    if time < node_5_added_as_child_to_node_3_at_time
      assert_false(
        node_3.canBeTemplate(time),
        error_msg + "(3)"
      )
    else
      assert_true(
        node_3.canBeTemplate(time),
        error_msg + "(4)"
      )
    end
  end
  
  # At this point, we can't check that Node.canFormTemplate() returns false if
  # the function is invoked on a node that is already a template.  To do this, 
  # we need a template node.  Since node 3 can be a template, turn it into one
  # then invoke "makeTemplate()" on it afterwards.
  time_node_3_becomes_template = node_5_added_as_child_to_node_3_at_time + 5
  assert_true(
    node_3.makeTemplate(time_node_3_becomes_template),
    error_msg + "(5)"
  )
  assert_false(
    node_3.canBeTemplate(time_node_3_becomes_template + 5),
    error_msg + "(6)"
  )
    
  #############################
  ### Node.fillSlots() TEST ###
  #############################
  
  error_msg = "occurred during Node.fillSlots() tests "
  
  list_pattern_to_fill_slots_with = ListPattern.new(Modality::VISUAL)
  list_pattern_to_fill_slots_with.add(ItemSquarePattern.new("D", 0, 1))
  list_pattern_to_fill_slots_with.add(ItemSquarePattern.new("K", 2, 0))
  
  #Assert that Node.fillSlots() will be blocked until node 3 becomes a template.
  for time in model_creation_time...time_node_3_becomes_template
    assert_equal(
      nil,
      node_3.fillSlots(
        list_pattern_to_fill_slots_with,
        time
      ),
      error_msg + "(1) at time " + time.to_s
    )
  end
  
  #Assert that, when asked to fill the slots of a node with a list pattern whose
  #modality differs to the node, the method doesn't fill the slots.
  Modality.values().each do |modality|
    if modality != node_3.getImage(time_node_3_becomes_template + 5).getModality()
      list_pattern_to_fill_slots_with.setModality(modality)
      assert_equal(
        nil,
        node_3.fillSlots(
          list_pattern_to_fill_slots_with,
          time_node_3_becomes_template + 5
        ),
        error_msg + "(2) with a ListPattern of modality '" + modality.toString + "'"
      )
    end
  end
  
  #Assert that Node.fillSlots() fills node 3's slots and returns true when 
  #node_3 is a template, the list pattern passed is of the same modality as 
  #node_3 and filling node_3's slots will not rewrite any part of node_3's
  #template history.
  list_pattern_to_fill_slots_with.setModality(Modality::VISUAL)
  time_node_3_slots_filled = time_node_3_becomes_template + 6
  assert_equal(
    2,
    node_3.fillSlots(
      list_pattern_to_fill_slots_with,
      time_node_3_slots_filled
    ),
    error_msg + "(3)"
  )
  
  ###############################
  ### Node.clearFilledSlots() ###
  ###############################
  
  error_msg = "occurred during Node.clearFilledSlots() tests "
  
  # Assert that Node.clearFilledSlots() will only return true when invoked on
  # a node at a time when the node is a template and won't rewrite the node's
  # template history
  time_filled_slots_cleared = time_node_3_slots_filled + 3
  assert_true(
    node_3.clearFilledSlots(time_filled_slots_cleared),
    error_msg + "(2)"
  )
  
  ###################################
  ### Node.makeNonTemplate() TEST ###
  ###################################
  
  error_msg = "occurred during Node.makeNonTemplate() tests "
  
  #Fill node 3's slots again so that we can assert that filled slots are 
  #cleared along with regular slots.
  time_node_3_slots_filled_again = time_filled_slots_cleared + 10
  assert_equal(
    2,
    node_3.fillSlots(list_pattern_to_fill_slots_with, time_node_3_slots_filled_again),
    error_msg + "(1)"
  )
  
  time_node_3_made_non_template_again = time_node_3_slots_filled_again + 5
  assert_true(
    node_3.makeNonTemplate(time_node_3_made_non_template_again),
    error_msg + "(3)"
  )
  
  ###############################################
  ### SET FINAL TEST TIME FOR QUERY FUNCTIONS ###
  ###############################################
  max_test_time = time_node_3_made_non_template_again + 10
  
  ##############################
  ### Node.isTemplate() TEST ###
  ##############################
  
  for time in model_creation_time..max_test_time
    isTemplate = node_3.isTemplate(time)
    error_msg = "occurred during Node.isTemplate() tests at time " + time.to_s + " i.e. "
    
    if time < time_node_3_becomes_template
      assert_false(
        isTemplate,
        error_msg + "before node_3 becomes a template"
      )
    elsif time >= time_node_3_becomes_template and time < time_node_3_made_non_template_again
      assert_true(
        isTemplate,
        error_msg + "after node_3 becomes a template but before it reverts to a non-template"
      )
    elsif time >= time_node_3_made_non_template_again
      assert_false(
        isTemplate,
        error_msg + "after node_3 reverts to a non-template"
      )
    end
  end
  
  ################################
  ### Node.getItemSlots() TEST ###
  ################################
  
  for time in model_creation_time..max_test_time
    itemSlots = node_3.getItemSlots(time)
    error_msg = "occurred during Node.getItemSlots() tests at time " + time.to_s + " i.e. "
    
    if time < time_node_3_becomes_template
      assert_equal(
        nil, 
        itemSlots,
        error_msg + "before node_3 becomes a template"
      )
    elsif time >= time_node_3_becomes_template and time < time_node_3_made_non_template_again
      assert_equal(
        1, 
        itemSlots.size(),
        error_msg + "after node_3 becomes a template but before it reverts back to being a non-template (size check)"
      )
      assert_equal(
        "D", 
        itemSlots.get(0),
        error_msg + "after node_3 becomes a template but before it reverts to a non-template (contents check)"
      )
    elsif time >= time_node_3_made_non_template_again
      assert_equal(
        nil, 
        itemSlots,
        error_msg + "after node_3 reverts to a non-template"
      )
    end
  end 
  
  ####################################
  ### Node.getPositionSlots() TEST ###
  ####################################
  
  for time in model_creation_time..max_test_time
    positionSlots = node_3.getPositionSlots(time)
    error_msg = "occurred during Node.getPositionSlots() tests at time " + time.to_s + " i.e. "
    
    if time < time_node_3_becomes_template
      assert_equal(
        nil, 
        positionSlots,
        error_msg + "before node_3 becomes a template"
      )
    elsif time >= time_node_3_becomes_template and time < time_node_3_made_non_template_again
      assert_equal(
        1, 
        positionSlots.size(),
        error_msg + "after node_3 becomes a template but before it reverts back to being a non-template (size check)"
      )
      assert_equal(
        Square.new(2, 0).toString(), 
        positionSlots.get(0).toString(),
        error_msg + "after node_3 becomes a template but before it reverts to a non-template (contents check)"
      )
    elsif time >= time_node_3_made_non_template_again
      assert_equal(
        nil, 
        positionSlots,
        error_msg + "after node_3 reverts to a non-template"
      )
    end
  end
  
  ######################################
  ### Node.getFilledItemSlots() TEST ###
  ######################################
  
  for time in model_creation_time..max_test_time
    filledItemSlots = node_3.getFilledItemSlots(time)
    error_msg = "occurred during Node.getFilledItemSlots() tests at time " + time.to_s + " i.e. "
    
    if time < time_node_3_becomes_template
      assert_equal(
        nil, 
        filledItemSlots,
        error_msg + "before node_3 becomes a template"
      )
    elsif time >= time_node_3_becomes_template and time < time_node_3_slots_filled
      assert_true(
        filledItemSlots.isEmpty(),
        error_msg + "after node_3 becomes a template but before its slots are filled"
      )
    elsif time >= time_node_3_slots_filled and time < time_filled_slots_cleared
      assert_equal(
        1, 
        filledItemSlots.size(),
        error_msg + "after node_3's slots are filled but before its slots are cleared (size check)"
      )
      assert_equal(
        ItemSquarePattern.new("D", 0, 1).toString, 
        filledItemSlots.get(0).toString(),
        error_msg + "after node_3's slots are filled but before its slots are cleared (contents check)"
      )
    elsif time >= time_filled_slots_cleared and time < time_node_3_slots_filled_again
      assert_true(
        filledItemSlots.isEmpty(),
        error_msg + "after node_3's slots are cleared but before they're filled again"
      )
    elsif time >= time_node_3_slots_filled_again and time < time_node_3_made_non_template_again
      assert_equal(
        1, 
        filledItemSlots.size(),
        error_msg + "after node_3's slots are filled again but before it reverts to a non-template (size check)"
      )
      assert_equal(
        ItemSquarePattern.new("D", 0, 1).toString, 
        filledItemSlots.get(0).toString(),
        error_msg + "after node_3's slots are filled again but before it reverts to a non-template (contents check)"
      )
    elsif time >= time_node_3_made_non_template_again
      assert_equal(
        nil, 
        filledItemSlots,
        error_msg + "after node_3 reverts to a non-template"
      )
    end
  end
  
  ##########################################
  ### Node.getFilledPositionSlots() TEST ###
  ##########################################
  
  for time in model_creation_time..max_test_time
    filledPositionSlots = node_3.getFilledPositionSlots(time)
    error_msg = "occurred during Node.getFilledPositionSlots() tests at time " + time.to_s + " i.e. "
    
    if time < time_node_3_becomes_template
      assert_equal(
        nil, 
        filledPositionSlots,
        error_msg + "before node_3 becomes a template"
      )
    elsif time >= time_node_3_becomes_template and time < time_node_3_slots_filled
      assert_true(
        filledPositionSlots.isEmpty(),
        error_msg + "after node_3 becomes a template but before its slots are filled"
      )
    elsif time >= time_node_3_slots_filled and time < time_filled_slots_cleared
      assert_equal(
        1, 
        filledPositionSlots.size(),
        error_msg + "after node_3's slots are filled but before its slots are cleared (size check)"
      )
      assert_equal(
        ItemSquarePattern.new("K", 2, 0).toString, 
        filledPositionSlots.get(0).toString(),
        error_msg + "after node_3's slots are filled but before its slots are cleared (contents check)"
      )
    elsif time >= time_filled_slots_cleared and time < time_node_3_slots_filled_again
      assert_true(
        filledPositionSlots.isEmpty(),
        error_msg + "after node_3's slots are cleared but before they're filled again"
      )
    elsif time >= time_node_3_slots_filled_again and time < time_node_3_made_non_template_again
      assert_equal(
        1, 
        filledPositionSlots.size(),
        error_msg + "after node_3's slots are filled again but before it reverts to a non-template (size check)"
      )
      assert_equal(
        ItemSquarePattern.new("K", 2, 0).toString, 
        filledPositionSlots.get(0).toString(),
        error_msg + "after node_3's slots are filled again but before it reverts to a non-template (contents check)"
      )
    elsif time >= time_node_3_made_non_template_again
      assert_equal(
        nil, 
        filledPositionSlots,
        error_msg + "after node_3 reverts to a non-template"
      )
    end
  end
end

################################################################################
# Tests regarding information should come after tests regarding template 
# functionality since templates are used in the information tests below and 
# hence must be constructed correctly in order for these tests to operate 
# correctly.
#
process_test "information" do
  
  #############
  ### SETUP ###
  #############
  
  Chrest.class_eval{
    field_accessor :_nextLtmNodeReference,
    :_minNodeDepthInNetworkToBeTemplate,
    :_minItemOrPositionOccurrencesInNodeImagesToBeSlotValue
  }
  
  # Node.setImage() has private access so, to use it in this test, its 
  # accessibility must be public.
  set_image = Node.java_class.declared_method(:setImage, ListPattern, Java::int)
  set_image.accessible = true
  
  # Create the model that all tests will occur in context of.
  model_creation_time = 0
  model = Chrest.new(model_creation_time, false)
  
  ##################
  ### ROOT NODES ###
  ##################
  
  # Invoke information() on every modality root node at the following times:
  # 
  # 1. When they were initially created.
  # 2. At the time an attempt is made to set their image (which should fail any 
  #    way, see Node.setImage()).
  # 3. After the time an attempt is made to set their image.
  #
  # In all cases, the result of invoking information should be 0.
  Modality.values().each do |modality|
    root_node = model.getLtmModalityRootNode(modality)
    set_image.invoke(root_node, Pattern.makeVisualList(["A", "B"].to_java(:String)), 5)
    
    assert_equal(0, root_node.information(model_creation_time), "when checking information at time of " + modality.toString() + " root node creation, before any attempt to set this node's image is made") 
    assert_equal(0, root_node.information(5), "when checking information at the time an attempt is made to set the " + modality.toString() + " root node's image")
    assert_equal(0, root_node.information(6), "when checking information at a time after an attempt is made to set the " + modality.toString() + " root node's image")
  end
  
  ######################
  ### NON-ROOT NODES ###
  ######################
  
  # Construct a non-root node with an empty image.  Since template slots also 
  # count as information, the non-root node will need to be converted to a 
  # template to test Node.information() fully.  Since only visual nodes 
  # containing jchrest.lib.ItemSquarePatterns in their image can be currently
  # converted into templates, the image of this non-root node has to be
  # a ListPattern composed of ItemSquarePattern instances.
  node_creation_time = model_creation_time + 1
  node_contents = ListPattern.new(Modality::VISUAL)
  node_contents.add(ItemSquarePattern.new("A", 0, 0))
  node_image = ListPattern.new(Modality::VISUAL)
  
  node = Node.new(
    model,
    node_contents,
    node_image,
    node_creation_time
  )
  model._nextLtmNodeReference += 1
  
  # Update the non-root node image.
  node_image_update_time = node_creation_time + 2
  node_updated_image = ListPattern.new(Modality::VISUAL)
  node_updated_image.add(ItemSquarePattern.new("A", 0, 0))
  node_updated_image.add(ItemSquarePattern.new("B", 1, 0))
  node_updated_image.add(ItemSquarePattern.new("C", 0, 1))
  set_image.invoke(node, node_updated_image, node_image_update_time)
  
  # Convert the non-root node into a template.  To do this, hack the minimum 
  # template depth field value of the model and set it to 0 (the 
  # Chrest.setTemplateConstructionParameters() won't allow values 
  # to be set below sensible levels but this would make the test very long)
  model._minNodeDepthInNetworkToBeTemplate = 0
  model._minItemOrPositionOccurrencesInNodeImagesToBeSlotValue = 1
  
  child_creation_time = node_image_update_time + 1
  
  node_contents.add(ItemSquarePattern.new("D", 2, 0))
  child_contents = node_contents
  child_image = ListPattern.new(Modality::VISUAL)
  child_image.add(ItemSquarePattern.new("A", 0, 0))
  child_image.add(ItemSquarePattern.new("D", 1, 0))
  child_image.add(ItemSquarePattern.new("C", 0, 1))
  child = Node.new(
    model,
    child_contents,
    child_image,
    child_creation_time
  )
  model._nextLtmNodeReference += 1
  
  child_addition_time = child_creation_time + 5
  node.addChild(
    child_contents,
    child,
    child_addition_time,
    ""
  )
  
  template_construction_time = (child_addition_time + 2)
  assert_true(node.makeTemplate(template_construction_time), "occurred when checking the rsult of converting the node to a template")
  
  for time in node_creation_time..(template_construction_time + 3)
    if time >= (node_creation_time - 1) and time < node_image_update_time
      # When node is constructed, its image is empty, i.e. no primitives so its
      # information amount is 0.
      assert_equal(0, node.information(time), "occurred when checking the information count of the node before its image is updated")
    elsif time >= node_image_update_time and time < template_construction_time
      # When the image is updated, it holds 3 primitives so its information
      # amount is 3.
      assert_equal(3, node.information(time), "occurred when checking the information count of the node after its image has been updated but before it is converted to a template")
    elsif time >= template_construction_time
      # The amount of information in node should now equal 8 since its image 
      # contains 3 primitives + 3 item slots (B, C, D) + 2 position slots ([0,1] 
      # and [1,0]).
      assert_equal(8, node.information(time), "occurred when checking the information count of the node after it has been converted to a template")
    end
  end
end

################################################################################
unit_test "get_all_information" do
  
  ##############################################
  ##### SET-UP ACCESS TO PRIVATE VARIABLES #####
  ##############################################
  
  # Need to be able to edit a Node's image and fill its item and position slots
  Node.class_eval{
    field_accessor :_imageHistory, :_filledItemSlotsHistory, :_filledPositionSlotsHistory
  }
  
  # Needed to that Node contents/image can be constructed precisely.
  ListPattern.class_eval{
    field_accessor :_list
  }
  
  #####################
  ##### MAIN LOOP #####
  #####################
  
  50.times do
  
    time_model_created = 0
    model = Chrest.new(time_model_created, [true,false].sample)
    
    ##########################
    ##### CONSTRUCT NODE #####
    ##########################

    # Construct node contents
    node_contents = ListPattern.new(Modality::VISUAL)
    node_contents._list.add(ItemSquarePattern.new("A", 3, 3))

    # Construct node image
    node_image = ListPattern.new(Modality::VISUAL)

    # Construct node so that its contents, image and item/position slots 
    # can be instantiated.
    time_node_created = time_model_created + 5
    node = Node.new(model, node_contents, node_image, time_node_created)

    # Add information to the image after node has been constructed.  Image will
    # contain the ItemSquarePattern in contents so the test can check if 
    # duplicate PrimitivePatterns in the contents/image are removed.
    new_image = ListPattern.new(Modality::VISUAL)
    for pattern in node_contents._list
      new_image._list.add(pattern)
    end
    new_image._list.add(ItemSquarePattern.new("B", 5, 5))
    time_image_updated = time_node_created + 5
    node._imageHistory.put(time_image_updated.to_java(:int), new_image)

    # Construct and fill node's filled item/position slots (the Node doesn't have 
    # to actually be a template to do this since the test has access to the 
    # relevant private Node instance variables).
    node_filled_item_slots = ArrayList.new()
    node_filled_item_slots.add(ItemSquarePattern.new("C", 5, 3))
    node_filled_item_slots_history = HistoryTreeMap.new()
    time_item_slots_filled = time_image_updated + 5
    node_filled_item_slots_history.put(time_item_slots_filled.to_java(:int), node_filled_item_slots)
    node._filledItemSlotsHistory = node_filled_item_slots_history

    # Construct node filled position slots
    node_filled_position_slots = ArrayList.new()
    node_filled_position_slots.add(ItemSquarePattern.new("D", 3, 5))
    node_filled_position_slots_history = HistoryTreeMap.new()
    time_position_slots_filled = time_item_slots_filled + 5
    node_filled_position_slots_history.put(time_position_slots_filled.to_java(:int), node_filled_position_slots)
    node._filledPositionSlotsHistory = node_filled_position_slots_history

    ################
    ##### TEST #####
    ################

    info_before_image_updated = node.getAllInformation( rand(time_node_created...time_image_updated).to_java(:int) )
    info_after_image_updated_before_item_slots_filled = node.getAllInformation( rand(time_image_updated...time_item_slots_filled).to_java(:int) )
    info_after_item_slots_filled_before_position_slots_filled = node.getAllInformation( rand(time_item_slots_filled...time_position_slots_filled).to_java(:int) )
    info_after_position_slots_filled = node.getAllInformation( rand(time_position_slots_filled...(time_position_slots_filled + 5)).to_java(:int) )

    expected_info = ListPattern.new(Modality::VISUAL)
    expected_info._list.add(ItemSquarePattern.new("A", 3, 3))
    assert_equal(
      expected_info.toString(),
      info_before_image_updated.toString(),
      "occurred when checking return value before node image updated"
    )
    
    expected_info._list.add(ItemSquarePattern.new("B", 5, 5))
    assert_equal(
      expected_info.toString(),
      info_after_image_updated_before_item_slots_filled.toString(),
      "occurred when checking return value after node image updated but " +
      "before item slots are filled"
    )
    
    expected_info._list.add(ItemSquarePattern.new("C", 5, 3))
    assert_equal(
      expected_info.toString(),
      info_after_item_slots_filled_before_position_slots_filled.toString(),
      "occurred when checking return value after item slots filled but " +
      "before position slots are filled"
    )
    
    expected_info._list.add(ItemSquarePattern.new("D", 3, 5))
    assert_equal(
      expected_info.toString(),
      info_after_position_slots_filled.toString(),
      "occurred when checking return value after position slots are filled"
    )
  end
end

