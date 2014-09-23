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

process_test "'Minds eye tests'" do
  model = Chrest.new
  baseTime = 100
  timeToMoveObject = 250
  
  ##############################################################################
  # Create and instantiate a mind's eye and check that its representation of the
  # domain environment is correct.
  ##############################################################################
  # 
  # Tests to see if the 2D array of the mind's eye represents the observer's 
  # vision as expected:
  # 1) Do the first dimension elements in the 2D mind's eye array represent the 
  #    x coordinates of the observer's current vision?
  # 2) Do the second dimension elements in the 2D mind's eye array represent the
  #    y coordinates of the observer's current vision?
  # 3) Do the x and y elements in the 2D mind's eye array represent the 
  #    domain-specific coordinates in an ascending order?
  # 4) Are "blind spots" in the vision used to intantiate the mind's eye 
  #    correctly respresented as null values in the 2D mind's eye array?
  # 
  # The vision used to instantiate the mind's eye 2D array in these tests 
  # resembles a "cone" of vision i.e. the further ahead the observer sees, the
  # wider its field of vision.  A diagram of this vision can be found below:
  # "-" and "|" are used to denote squares that represent units of vision, "x" 
  # represents a "blind spot", "A"/"B"/"C"/"D" represent 4 distinct objects that 
  # the observer can see and the numbers to the bottom and left of the diagram 
  # represent the domain-specific x and y coordinates, respectively. 
  # 
  #   -------------------------------
  # 3 |     | C,D |     |     |     | 
  #   -------------------------------
  # 2   x   |     |  B  |     |  x
  #         -------------------
  # 1   x      x  |  A  |  x     x
  #               -------
  #    -2     -1     0     1     2
  #
  # The 2D mind's eye array created should be composed of 5 first dimension 
  # array elements that represent the x coordinates of the vision and a 3 
  # element array in each of the first dimension elements to represent the y 
  # coordinates of the vision.  The 2D mind's eye array element 0,0 should 
  # contain "null" since the domain-specific coordinates -2,1 can not be seen.  
  # The 2D mind's eye array element 0,2 should contain "" since the 
  # domain-specific coordinates -2,3 can be seen but does not contain an object.  
  #
  # Declare the vision cone "backwards" i.e. maximum domain-specific x/y 
  # coordinates first so that it can be asserted that the 2D mind's eye array is 
  # created from the minimum domain-specific x/y coordinates in ascending order, 
  # x-coordinates first.
  
  expectedTime = 0
  empty = ""
  null = "null"
  objectA = "A"
  objectB = "B"
  objectC = "C"
  objectD = "D"
  
  visionCone = [
    empty + ";2;3",
    empty + ";1;3",
    empty + ";0;3",
    objectC + "," + objectD + ";-1;3",
    empty + ";-2;3",
    empty + ";1;2",
    objectB + ";0;2",
    empty + ";-1;2",
    objectA + ";0;1"
  ]
    
  def get_minds_eye_contents(model)
    contents = Array.new
    for i in -2..2
      for j in 1..3
        contents << model.getMindsEyeContentUsingDomainSpecificCoords(i, j)
      end
    end
    return contents
  end
  
  #Represents the initial state of the mind's eye.
  expectedMindsEyeContents = [
    null,
    null,
    empty,
    null,
    empty,
    objectC + "," + objectD,
    objectA,
    objectB,
    empty,
    null, 
    empty,
    empty,
    null,
    null,
    empty,
  ]
  
  model.createNewMindsEye(visionCone, baseTime, timeToMoveObject)
  
  mindsEyeContents = get_minds_eye_contents(model)
  mindsEyeContents.each_with_index{ 
    |val, index| 
    assert_equal(expectedMindsEyeContents[index], val, "Occurred with element " + index.to_s + " when checking the initial state of the mind's eye.") 
  }
  
  ##############################################################################
  # Specify more than one object in initial move
  ##############################################################################
  
  multipleObjectsIdentifiedInitially = [ 
    [ objectC + "," + objectD + ";-1;3", objectC + "," + objectD + ";0;3"] 
  ]
  moveResult = model.moveObjects(multipleObjectsIdentifiedInitially)
  assert_false(moveResult[0], "Occurred when checking result of specifying two objects initially.")
  
  assert_equal(expectedTime, model.getClock(), "Occurred when checking the CHREST model's clock after specifying two objects initially.")
  
  mindsEyeContents = get_minds_eye_contents(model)
  mindsEyeContents.each_with_index{
    |val, index|
    assert_equal(expectedMindsEyeContents[index], val, "Occurred with element " + index.to_s + " when checking the state of the mind's eye after specifying two objects initially.")
  }
  
  ##############################################################################
  # Specify intial coordinates for an object but no moves
  ##############################################################################
  
  noObjectBMoves = [
    [ objectA + ";0;1", objectA + ";0;3" ],
    [ objectB + ";0;2" ]
  ]
  moveResult = model.moveObjects(noObjectBMoves)
  assert_false(moveResult[0], "Occurred when checking result of specifying initial coordinates and no moves for an object.")
  
  assert_equal(expectedTime, model.getClock(), "Occurred when checking the CHREST model's clock after specifying initial coordinates and no moves for an object.")
  
  mindsEyeContents = get_minds_eye_contents(model)
  mindsEyeContents.each_with_index{
    |val, index|
    assert_equal(expectedMindsEyeContents[index], val, "Occurred with element " + index.to_s + " when checking the state of the mind's eye after specifying initial coordinates and no moves for an object.")
  }
  
  ##############################################################################
  # Specify wrong initial coordinates for an object
  ##############################################################################
  wrongInitialCoordinates = [
    [ objectA + ";0;1", objectA + ";0;3" ], 
    [ objectB + ";1;2", objectB + ";1;3" ]
  ]
  moveResult = model.moveObjects(wrongInitialCoordinates)
  assert_false(moveResult[0], "Occurred when checking result of attempting to move an object whose specified initial coordinates are incorrect.")
  
  assert_equal(expectedTime, model.getClock(), "Occurred when checking the CHREST model's clock after attempting to move an object whose specified initial coordinates are incorrect.")
  
  mindsEyeContents = get_minds_eye_contents(model)
  mindsEyeContents.each_with_index{
    |val, index|
    assert_equal(expectedMindsEyeContents[index], val, "Occurred with element " + index.to_s + " when checking the state of the mind's eye after moving two objects multiple times each.")
  }
  
  ##############################################################################
  # Move two objects, multiple times each
  ##############################################################################
  twoObjectsMultipleTimes = [
    [ objectC + ";-1;3", objectC + ";0;3", objectC + ";1;3"],
    [ objectB + ";0;2", objectB + ";-1;2", objectB + ";-1;3"]
  ]
  moveResult = model.moveObjects(twoObjectsMultipleTimes)
  assert_true(moveResult[0], "Occurred when checking the result of moving two objects multiple times each.")
  
  expectedTime = baseTime + (timeToMoveObject * 4)
  assert_equal(expectedTime, model.getClock(), "Occurred when checking the CHREST model's clock after moving two objects multiple times.")
  
  expectedMindsEyeContents[5] = objectD
  expectedMindsEyeContents[8] = empty
  expectedMindsEyeContents[11] = objectC
  
  expectedMindsEyeContents[7] = empty
  expectedMindsEyeContents[4] = empty
  expectedMindsEyeContents[5] = objectD + "," + objectB
  mindsEyeContents = get_minds_eye_contents(model)
  mindsEyeContents.each_with_index{
    |val, index|
    assert_equal(expectedMindsEyeContents[index], val, "Occurred with element " + index.to_s + " when checking the state of the mind's eye after moving two objects multiple times each.")
  }
  
  ##############################################################################
  # Move two objects onto the same coordinates in the mind's eye
  ##############################################################################
  twoObjectsSameCoord = [ [objectB + ";-1;3", objectB + ";1;3"] ]
  moveResult = model.moveObjects(twoObjectsSameCoord)
  assert_true(moveResult[0], "Occurred when checking the result of moving two objects onto the same coordinates.")
  
  expectedTime += baseTime + timeToMoveObject
  assert_equal(expectedTime, model.getClock(), "Occurred when checking the CHREST model's clock after moving two objects onto the same coordinates.")
  
  expectedMindsEyeContents[5] = objectD
  expectedMindsEyeContents[11] = objectC + "," + objectB
  mindsEyeContents = get_minds_eye_contents(model)
  mindsEyeContents.each_with_index{
    |val, index|
    assert_equal(expectedMindsEyeContents[index], val, "Occurred with element " + index.to_s + " when checking the state of the mind's eye after moving two objects onto the same coordinates.")
  }
  
  ##############################################################################
  # Move a different object part-way through moving another object.
  ##############################################################################
  illegalMoveSequence = [
    [objectB + ";1;3", objectC + ";1;3", objectB + ";2;3"],
    [objectC + ";1;3", objectC + ";1;2", objectC + ";1;3"]
  ]
  moveResult = model.moveObjects(illegalMoveSequence)
  assert_false(moveResult[0], "Occurred when checking the result of moving a different object part-way through moving another.")
  
  assert_equal(expectedTime, model.getClock(), "Occurred when checking the CHREST model's clock after moving a different object part-way through moving another.")
  
  mindsEyeContents = get_minds_eye_contents(model)
  mindsEyeContents.each_with_index{
    |val, index|
    assert_equal(expectedMindsEyeContents[index], val, "Occurred with element " + index.to_s + " when checking the state of the mind's eye after moving a different object part-way through moving another.")
  }
  
  ##############################################################################
  # Move an object onto coordinates not represented in the mind's eye.
  # 
  # This test also checks to see if a coordinates content is correct if the last
  # object on the initial coordinates specified is moved.
  ##############################################################################
  moveObjectBOutsideVisualSpatialField = [[objectB + ";1;3", objectB + ";1;4"]]
  modelResult = model.moveObjects(moveObjectBOutsideVisualSpatialField)
  assert_true(modelResult[0], "Occurred when checking the result of moving an object to coordinates not in the current range of the mind's eye.")

  expectedTime += (baseTime + timeToMoveObject)
  assert_equal(expectedTime, model.getClock(), "Occurred when checking the CHREST model's clock after moving an object to coordinates not in the current range of the mind's eye.")
  
  expectedMindsEyeContents[11] = objectC
  mindsEyeContents = get_minds_eye_contents(model)
  mindsEyeContents.each_with_index{
    |val, index|
    assert_equal(expectedMindsEyeContents[index], val, "Occurred with element " + index.to_s + " when checking the state of the mind's eye after moving an object to coordinates not in the current range of the mind's eye.")
  }
  
  assert_false(mindsEyeContents.include?(objectB), "Occurred when checking for the abscence of the object that was moved to coordinates not in the current range of the mind's eye." )
  
  ##############################################################################
  # Move an object onto the same coordinates as another before moving the first
  # object on these coordinates to other cooridnates.
  ##############################################################################
  
  moveFirstObjectFromSharedCoordinates = [
    [objectA + ";0;1", objectA + ";1;3"], 
    [objectC + ";1;3", objectC + ";2;3"]
  ]
  resultOfMove = model.moveObjects(moveFirstObjectFromSharedCoordinates)
  assert_true(resultOfMove[0], "Occurred when checking the result of moving an object to shared coordinates and then moving the object originally at these shared coordinates.")

  expectedTime += (baseTime + timeToMoveObject * 2)
  assert_equal(expectedTime, model.getClock(), "Occurred when checking the CHREST model's clock after moving an object to shared coordinates and then moving the object originally at these shared coordinates.")
  
  expectedMindsEyeContents[6] = empty
  expectedMindsEyeContents[11] = objectA
  expectedMindsEyeContents[14] = objectC
  mindsEyeContents = get_minds_eye_contents(model)
  mindsEyeContents.each_with_index{
    |val, index|
    assert_equal(expectedMindsEyeContents[index], val, "Occurred with element " + index.to_s + " when checking the state of the mind's eye after moving an object to shared coordinates and then moving the object originally at these shared coordinates.")
  }
  
  ##############################################################################
  # Attempt to move object after it has been moved into a blind spot
  ##############################################################################
  
  moveObjectAfterMovingToBlindSpot = [[objectC + ";2;3", objectC + ";2;4", objectC + ";2;3"]]
  resultOfMove = model.moveObjects(moveObjectAfterMovingToBlindSpot)
  assert_true(resultOfMove[0], "Occurred when checking the result of attempting to move an object after it has been moved out of mind's eye range.")
  
  expectedTime += (baseTime + timeToMoveObject)
  assert_equal(expectedTime, model.getClock(), "Occurred when checking the CHREST model's clock after attempting to move an object after it has been moved out of mind's eye range.")
  
  expectedMindsEyeContents[14] = empty
  mindsEyeContents = get_minds_eye_contents(model)
  mindsEyeContents.each_with_index{
    |val, index|
    assert_equal(expectedMindsEyeContents[index], val, "Occurred with element " + index.to_s + " when checking the state of the mind's eye after attempting to move an object after it has been moved out of mind's eye range.")
  }
  
  assert_false(mindsEyeContents.include?(objectC), "Occurred when checking for the abscence of the object that was moved out of the mind's eye range and then moved again afterwards.")

  ##############################################################################
  # Check that mind's eye contents are returned correctly.
  ##############################################################################
  domainRowAndCols = [
    "-2;1",
    "-2;2",
    "-2;3",
    "-1;1",
    "-1;2",
    "-1;3",
    "0;1",
    "0;2",
    "0;3",
    "1;1",
    "1;2",
    "1;3",
    "2;1",
    "2;2",
    "2;3",
  ]
  mindsEyeContents = model.getMindsEyeContentSpecificToDomain()
  mindsEyeContents.each_with_index{
    |val, index| 
    objectInformation = val.split(";")
    domainRowAndCol = domainRowAndCols[index].split(";")
    assert_equal(expectedMindsEyeContents[index], objectInformation[0])
    assert_equal(domainRowAndCol[0], objectInformation[1])
    assert_equal(domainRowAndCol[1], objectInformation[2])
  }
end
