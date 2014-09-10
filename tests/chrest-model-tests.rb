# Overall tests of Chrest

process_test "timings" do
  model = Chrest.new
  model.setRho 1.0
  model.setFamiliarisationTime 2000
  model.setDiscriminationTime 10000
  patternA = Pattern.makeVisualList(["B", "I", "F"].to_java(:String))
  patternB = Pattern.makeVisualList(["X", "A", "Q"].to_java(:String))

  assert_equal(0, model.getClock)
  # check changed on one learning operation
  model.recogniseAndLearn patternA # -- discriminate node for 'B'
  assert_equal(10000, model.getClock)
  # check changed on second learning operation
  model.recogniseAndLearn patternA # -- familiarise node for 'B'
  assert_equal(12000, model.getClock)
  # check a busy model is not changed
  model.recogniseAndLearn(patternB, 10000) # -- busy, no change
  assert_equal(12000, model.getClock)
  model.recogniseAndLearn patternA # -- discriminate node for 'I'
  assert_equal(22000, model.getClock)
  model.recogniseAndLearn patternA # -- familiarise node with 'BI'
  assert_equal(24000, model.getClock)
  model.recogniseAndLearn patternA # -- discriminate node for 'F'
  assert_equal(34000, model.getClock)
  model.recogniseAndLearn patternA # -- familiarise node for 'BIF'
  assert_equal(36000, model.getClock)
  model.recogniseAndLearn patternA # -- no change, pattern fully learnt
  assert_equal(36000, model.getClock)
end

process_test "base case" do
  model = Chrest.new
  emptyList = Pattern.makeVisualList([].to_java(:int))
  assert_true(Pattern.makeVisualList(["Root"].to_java(:String)).equals(model.recognise(emptyList).getImage))
end

process_test "learning case 1" do
  # Every item that is learnt must first be learnt at the top-level,
  # as a primitive.  Learning that top-level node is done with an empty image.
  model = Chrest.new
  emptyList = Pattern.makeVisualList([].to_java(:int))
  list = Pattern.makeVisualList([1,2,3,4].to_java(:int))
  list.setFinished
  prim = Pattern.makeVisualList([1].to_java(:int))
  prim_test = Pattern.makeVisualList([1].to_java(:int))
  prim.setFinished

  model.recogniseAndLearn list
  assert_equal(1, model.getLtmByModality(list).getChildren.size)

  firstChild = model.getLtmByModality(list).getChildren.get(0)
  assert_false(emptyList.equals(firstChild.getChildNode.getContents))
  assert_true(firstChild.getTest.equals(prim_test))
  assert_true(firstChild.getChildNode.getContents.equals(prim_test))
  assert_true(firstChild.getChildNode.getImage.equals(emptyList))
end

process_test "learning case 2" do
  # Same as 'learning case 1', but using item-on-square instead of simple numbers
  model = Chrest.new
  emptyList = ListPattern.new
  list = ListPattern.new
  list.add ItemSquarePattern.new("P", 1, 2)
  list.add ItemSquarePattern.new("P", 2, 2)
  list.add ItemSquarePattern.new("P", 3, 2)
  list.add ItemSquarePattern.new("P", 4, 2)
  list.setFinished
  prim= ListPattern.new
  prim.add ItemSquarePattern.new("P", 1, 2)
  prim_test = prim.clone
  prim.setFinished

  model.recogniseAndLearn list
  assert_equal(1, model.getLtmByModality(list).getChildren.size)

  firstChild = model.getLtmByModality(list).getChildren.get(0)
  assert_false(emptyList.equals(firstChild.getChildNode.getContents))
  assert_true(firstChild.getTest.equals(prim_test))
  assert_true(firstChild.getChildNode.getContents.equals(prim_test))
  assert_true(firstChild.getChildNode.getImage.equals(emptyList))
end

process_test "simple retrieval 1" do
  # Check that after learning a primitive, the model will retrieve 
  # that node on trying to recognise the list
  model = Chrest.new
  list = Pattern.makeVisualList([1,2,3,4].to_java(:int))
  list.setFinished
  emptyList = Pattern.makeVisualList([].to_java(:int))
  prim = Pattern.makeVisualList([1].to_java(:int))
  prim_test = Pattern.makeVisualList([1].to_java(:int))
  prim.setFinished

  model.recogniseAndLearn list
  node = model.recognise list

  assert_false emptyList.equals(node.getContents)
  assert_true prim_test.equals(node.getContents)
  assert_true emptyList.equals(node.getImage)
end

process_test "simple learning 2" do
  model = Chrest.new
  list = Pattern.makeVisualList([1,2,3,4].to_java(:int))
  list2 = Pattern.makeVisualList([2,3,4].to_java(:int))
  list3 = Pattern.makeVisualList([1,3,4].to_java(:int))
  list3_test = Pattern.makeVisualList([1,3].to_java(:int))
  emptyList = Pattern.makeVisualList([].to_java(:int))
  prim1 = Pattern.makeVisualList [1].to_java(:int)
  prim2 = Pattern.makeVisualList [2].to_java(:int)

  model.recogniseAndLearn list2
  model.recogniseAndLearn list
  assert_equal(2, model.getLtmByModality(list).getChildren.size)
  # check most recent becomes the first child node
  assert_true prim1.equals(model.getLtmByModality(list).getChildren.get(0).getChildNode.getContents)
  assert_true prim2.equals(model.getLtmByModality(list).getChildren.get(1).getChildNode.getContents)
  # force discriminate from node 0
  # by first overlearning
  model.recogniseAndLearn list
  model.recogniseAndLearn list
  assert_true model.recognise(list).getImage.equals(Pattern.makeVisualList([1,2].to_java(:int)))
  node = model.getLtmByModality(list).getChildren.get(0).getChildNode
  assert_equal(0, node.getChildren.size)
  model.recogniseAndLearn list3 # first learn the '3' to use as test
  model.recogniseAndLearn list3 # now trigger discrimination
  assert_equal(1, node.getChildren.size)
  assert_true list3_test.equals(node.getChildren.get(0).getChildNode.getImage)
  assert_true list3_test.equals(node.getChildren.get(0).getChildNode.getContents)
  # and familiarise
  node = node.getChildren.get(0).getChildNode
  model.recogniseAndLearn list3
  model.recogniseAndLearn list3
  assert_true list3.equals(node.getImage)
end

process_test "check learning of < $ >" do
  model = Chrest.new
  list1 = Pattern.makeVisualList(["A", "B", "C"].to_java(:String))
  list2 = Pattern.makeVisualList(["A", "B"].to_java(:String))
  8.times do 
    model.recogniseAndLearn list1
  end
  assert_true list1.equals(model.recallPattern(list1))
  assert_true list1.equals(model.recallPattern(list2))
  node = model.recognise list2
  assert_true list1.equals(node.getImage)
  # learning should result in discrimination with < $ >
  model.recogniseAndLearn list2
  assert_equal(1, node.getChildren.size)
end

process_test "full learning" do 
  model = Chrest.new
  list1 = Pattern.makeVisualList([3,4].to_java(:int))
  list2 = Pattern.makeVisualList([1,2].to_java(:int))

  20.times do 
    model.recogniseAndLearn list1
    model.recogniseAndLearn list2
  end

  assert_true list1.equals(model.recallPattern(list1))
  assert_true list2.equals(model.recallPattern(list2))
end

#The aim of this test is to check for the correct operation of setting a CHREST
#instance's "_reinforcementLearningTheory" variable.  The following tests are
#run:
# 1) After creating a new CHREST instance, its "_reinforcementLearningTheory" 
# variable should be set to null.
# 2) You should be able to set a CHREST instance's "_reinforcementLearningTheory" 
# variable if it is currently set to null.
# 3) You should not be able to set a CHREST instance's "_reinforcementLearningTheory"
# variable if it is not currently set to null.
process_test "set reinforcement learning theory" do
  model = Chrest.new
  
  #Test 1.
  validReinforcementLearningTheories = ReinforcementLearning.getReinforcementLearningTheories()
  assert_equal("null", model.getReinforcementLearningTheory, "See test 1.")
  
  #Test 2.
  model.setReinforcementLearningTheory(validReinforcementLearningTheories[0])
  assert_equal(validReinforcementLearningTheories[0].to_s, model.getReinforcementLearningTheory, "See test 2.")
  
  #Test 3.
  model.setReinforcementLearningTheory(nil)
  assert_equal(validReinforcementLearningTheories[0].to_s, model.getReinforcementLearningTheory, "See test 3.")
end

#The aim of this test is to check for the correct operation of all implemented
#reinforcement theories in the jchrest.lib.ReinforcementLearning class in the
#CHREST architecture.  The following tests are run:
# 1) The size of the "_actionLinks" variable should = 0 after initialisation.
# 2) After adding an action node to a visual node's "_actionLinks" variable, 
#    the action node should be present in the visual node's "_actionLinks"
#    variable.
# 3) After adding an action node to a visual node's "_actionLinks" variable, 
#    the reinforcement value of the link between the visual and action node 
#    should be set to 0.0.
# 4) Passing too few variables to a reinforcement learning theory should return 
#    boolean false from that theory's "correctNumberOfVariables" method.
# 5) Passing too many variables to a reinforcement learning theory should return 
#    boolean false from that theory's "correctNumberOfVariables" method.
# 6) Passing the correct number of variables to a reinforcement learning theory 
#    should return boolean true from that theory's "correctNumberOfVariables" 
#    method.
# 7) After passing the correct number of variables to a reinforcement theory and
#    using that theory's "calculateReinforcementValue" method, the value 
#    returned should equal an expected value.
# 8) After passing the correct number of variables to a reinforcement theory and
#    using the visual node's "reinforceActionLink" method, the final 
#    reinforcement value of the link between the visual and action node should 
#    equal an expected value.
process_test "'Reinforcement theory tests'" do
  
  #Retrieve all currently implemented reinforcement learning theories.
  validReinforcementLearningTheories = ReinforcementLearning.getReinforcementLearningTheories()
  
  #Construct a test visual pattern.
  visualPattern = Pattern.makeVisualList [1].to_java(:int)
  visualPattern.setFinished
  visualPatternString = visualPattern.toString
  
  #Construct a test action pattern.
  actionPattern = Pattern.makeActionList ["A"].to_java(:string)
  actionPattern.setFinished
  actionPatternString = actionPattern.toString
  
  #Test each reinforcement learning theory implemented in the CHREST 
  #architecture.
  validReinforcementLearningTheories.each do |reinforcementLearningTheory|
    
    #Create a new CHREST model instance and set its reinforcement learning 
    #theory to the one that is to be tested.
    model = Chrest.new
    model.setReinforcementLearningTheory(reinforcementLearningTheory)
    reinforcementLearningTheoryName = reinforcementLearningTheory.toString
  
    #Learn visual and action patterns.
    model.recogniseAndLearn(visualPattern)
    model.recogniseAndLearn(actionPattern)
  
    #Retrieve visual and action nodes after learning.
    visualNode = model.recognise(visualPattern)
    actionNode = model.recognise(actionPattern)
  
    #Test 1.
    visualNodeActionLinks = visualNode.getActionLinks
    assert_equal(0, visualNodeActionLinks.size, "See test 1.")
  
    #Test 2 and 3.
    visualNode.addActionLink(actionNode)
    visualNodeActionLinks = visualNode.getActionLinks
    visualNodeActionLinkValue = visualNodeActionLinks.get(actionNode)
    assert_true(visualNodeActionLinks.containsKey(actionNode), "After adding " + actionPatternString + " to the _actionLinks variable of " + visualPatternString + ", " + visualPatternString + "'s _actionLinks does not contain " + actionPatternString + ".")
    assert_equal(0.0, visualNodeActionLinkValue, "See test 3.")
  
    #Depending upon the model's current reinforcement learning theory, 5 
    #variables should be created:
    # 1) tooLittleVariables = an array of numbers whose length is less than the
    #    number of variables needed by the current reinforcement theory to 
    #    calculate a reinforcement value.
    # 2) tooManyVariables = an array of numbers whose length is more than the
    #    number of variables needed by the current reinforcement theory to 
    #    calculate a reinforcement value.
    # 3) correctVariables = an array of arrays.  Each inner array's length 
    #    should equal the number of variables needed by the current 
    #    reinforcement learning theory.
    # 4) expectedCalculationValues = an array of numbers that should specify
    #    the value returned by a reinforcement learning theory has been 
    #    calculated.  There is a direct mapping between this array's indexes 
    #    and the indexes of the "correctVariables" array i.e. the variables in 
    #    index 0 of the "correctVariables" array should produce the variable 
    #    stored in index 0 of the "expectedCalculationValues" array.
    # 5) expectedReinforcementValues = an array of numbers that should specify 
    #    the value returned by a reinforcement learning theory after a 
    #    reinforcement value has been calculated AND added to the current 
    #    reinforcement value between the visual node and action node.  There is 
    #    a direct mapping between this array's indexes and the indexes of the 
    #    "correctVariables" array i.e. the variables in index 0 of the 
    #    "correctVariables" array should produce the variable stored in index 0 
    #    of the "expectedReinforcementValues" array after adding the calculated
    #    reinforcement value to the current reinforcement value between the 
    #    visual and action node.
    case 
      when reinforcementLearningTheoryName.casecmp("profit_sharing_with_discount_rate").zero?
        puts
          tooFewVariables = [1]
          tooManyVariables = [1,2,3,4,5]
          correctVariables = [[1,0.5,2,2],[1,0.5,2,1]]
          expectedCalculationValues = [1,0.5]
          expectedReinforcementValues = [1,1.5]
    end
    
    #Convert declared arrays into Double[] data types since the methods in the
    #ReinforcementLearning class require Double[] variables.
    tooFewVariables = tooFewVariables.to_java(:Double)
    tooManyVariables = tooManyVariables.to_java(:Double)
    expectedCalculationValues = expectedCalculationValues.to_java(:Double)
    expectedReinforcementValues = expectedReinforcementValues.to_java(:Double)
    
    #Same as above but for each inner array in the "correctVariables" array.
    correctVariables.each do |groupOfCorrectVariables|
      groupOfCorrectVariables.to_java(:Double)
    end
    
    #Tests 4 and 5.
    assert_false(reinforcementLearningTheory.correctNumberOfVariables(tooFewVariables), "FOR " + reinforcementLearningTheoryName + ": The number of variables in the 'tooFewVariables' parameter is not incorrect.")
    assert_false(reinforcementLearningTheory.correctNumberOfVariables(tooManyVariables), "FOR " + reinforcementLearningTheoryName + ": The number of variables in the 'tooManyVariables' parameter is not incorrect.")
    
    #Tests 6, 7 and 8.
    index = 0
    correctVariables.each do |groupOfCorrectVariables|
      assert_true(reinforcementLearningTheory.correctNumberOfVariables(groupOfCorrectVariables), "FOR " + reinforcementLearningTheoryName + ": The number of variables in item " + index.to_s + " of the 'correctvariables' parameter is incorrect.")
      calculationValue = reinforcementLearningTheory.calculateReinforcementValue(groupOfCorrectVariables)
      visualNode.reinforceActionLink(actionNode, groupOfCorrectVariables)
      visualNodeActionLinkValue = visualNode.getActionLinks.get(actionNode)
      assert_equal(expectedCalculationValues[index], calculationValue, "Triggered by the " + reinforcementLearningTheoryName + ".calculateReinforcementValue() method.  See item " + index.to_s + " in the 'expectedCalculationValues' variable.")
      assert_equal(expectedReinforcementValues[index], visualNodeActionLinkValue, "Triggered by the Node.reinforceActionLink() method (addition of current and new reinforcement values incorrect).  See item " + index.to_s + " in the 'expectedReinforcementValues' variable for the '" + reinforcementLearningTheoryName + "' reinforcement learning theory.")
      index += 1
    end
  end
end

#This test attempts to verify correct operation of a CHREST model's "mind's eye"
#by creating a new instance of CHREST and running the following tests:
# 1) After setting the size of its mind's eye to a known value, does the size of
#    the mind's eye equal this value?
# 2) Is it the case that only non-empty visual patterns can be added to the 
#    mind's eye?  
# 3) Is it the case that trying to add more patterns to the mind's eye than the 
#    maximum value allowed results in that pattern not being added? 
# 4) Is it the case that the patterns added to the mind's eye are present in 
#    the mind's eye contents?
# 5) After clearing the mind's eye, is it the case that all mind's eye contents
#    are equal to nil?
process_test "'Minds eye tests'" do
  model = Chrest.new
  
  #Test 1
  mindsEyeSize = 1
  model.setMindsEyeSize(mindsEyeSize)
  assert_equal(model.getMindsEyeSize(), mindsEyeSize, "The size of the mind's eye was set to " + mindsEyeSize.to_s + " but the actual size of the mind's eye is " + model.getMindsEyeSize().to_s + ".")
  
  #Test 2
  actionPattern = Pattern.makeActionList(["Action"].to_java(:String))
  verbalPattern = Pattern.makeVerbalList(["Verbal"].to_java(:String))
  visualPatternEmpty = Pattern.makeVisualList([].to_java(:String))
  visualPattern1 = Pattern.makeVisualList(["Test1"].to_java(:String))
  
  assert_false(model.addToMindsEye(actionPattern), "Action pattern was successfully added to the mind's eye: only visual patterns should be allowed.")
  assert_false(model.addToMindsEye(verbalPattern), "Verbal pattern was successfully added to the mind's eye: only visual patterns should be allowed.")
  assert_false(model.addToMindsEye(visualPatternEmpty), "An empty visual pattern was successfully added to the mind's eye: only non-empty visual patterns should be allowed.")
  assert_true(model.addToMindsEye(visualPattern1), "The visual item-square pattern: " + visualPattern1.toString() + " should have been added successfully to the mind's eye since its max size has been set to " + mindsEyeSize.to_s + " and should be empty before adding this pattern.")
  
  #Test 3
  visualPattern2 = Pattern.makeVisualList(["Test2"].to_java(:String))
  assert_false(model.addToMindsEye(visualPattern2), "Thhe visual item-square pattern: " + visualPattern2.toString() + " should not have been added successfully to the mind's eye since its max size has been set to " + mindsEyeSize.to_s + " and its current size should equal this value.")
  
  #Test 4
  mindsEyeContents = model.getMindsEyeContents()
  assert_equal(mindsEyeContents[0], visualPattern1, "The first item in the mind's eye should equal " + visualPattern1.toString() + " but doesn't: " + mindsEyeContents[0].toString() + ".")
  
  #Test 5
  model.clearMindsEye()
  mindsEyeContents = model.getMindsEyeContents()
  index = 0
  mindsEyeContents.each do |mindsEyeElement|
    assert_equal(mindsEyeElement, nil, "After clearing the mind's eye, there is a non-empty element in its contents at index " + index.to_s + ".")
    index += 1
  end
end
