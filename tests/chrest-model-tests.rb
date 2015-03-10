# Overall tests of Chrest

process_test "timings" do
  model = Chrest.new
  model.setRho 1.0
  model.setFamiliarisationTime 2000
  model.setDiscriminationTime 10000
  patternA = Pattern.makeVisualList(["B", "I", "F"].to_java(:String))
  patternB = Pattern.makeVisualList(["X", "A", "Q"].to_java(:String))

  assert_equal(0, model.getLearningClock)
  # check changed on one learning operation
  model.recogniseAndLearn patternA # -- discriminate node for 'B'
  assert_equal(10000, model.getLearningClock)
  # check changed on second learning operation
  model.recogniseAndLearn patternA # -- familiarise node for 'B'
  assert_equal(12000, model.getLearningClock)
  # check a busy model is not changed
  model.recogniseAndLearn(patternB, 10000) # -- busy, no change
  assert_equal(12000, model.getLearningClock)
  model.recogniseAndLearn patternA # -- discriminate node for 'I'
  assert_equal(22000, model.getLearningClock)
  model.recogniseAndLearn patternA # -- familiarise node with 'BI'
  assert_equal(24000, model.getLearningClock)
  model.recogniseAndLearn patternA # -- discriminate node for 'F'
  assert_equal(34000, model.getLearningClock)
  model.recogniseAndLearn patternA # -- familiarise node for 'BIF'
  assert_equal(36000, model.getLearningClock)
  model.recogniseAndLearn patternA # -- no change, pattern fully learnt
  assert_equal(36000, model.getLearningClock)
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
process_test "reinforcement theory tests" do
  
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

process_test "minds eye tests" do
  ArrayList = java.util.ArrayList
  model = Chrest.new
  minds_eye_lifespan = 500
  object_placement_time = 50
  minds_eye_access_time = 100
  time_to_move_object = 250
  domain_time = 0
  lifespan_for_recognised_objects = 10000
  lifespan_for_unrecognised_objects = 5000
  
  empty = ""
  null = "null"
  object_a = "A"
  object_b = "B"
  object_c = "C"
  object_d = "D"
  
#  def get_minds_eye_contents(model, domain_time)
#    contents = Array.new
#    for i in -2..2
#      for j in 1..3
#        contents << model.getSpecificMindsEyeContent(i, j, domain_time)
#      end
#    end
#    return contents
#  end
#  
#  def prior_move_performance_checks(model, domain_time, action_description)
#    assert_true(model.mindsEyeExists(domain_time), "Occurred when checking for the existence of the mind's eye before " + action_description + ".")
#    assert_true(model.attentionFree(domain_time), "Occurred when checking if CHREST's attention is free before " + action_description + ".")
#  end
#  
#  def legal_action_checks(model, domain_time, moves_performed, moves, number_of_moves, minds_eye_access_time, time_to_move_object, expected_attention_free_time, expected_terminus_time, expected_minds_eye_contents, minds_eye_lifespan, action_description)
#    
#    if(moves_performed)
#      model.moveObjectsInMindsEye(moves, domain_time)
#      expected_attention_free_time = domain_time + minds_eye_access_time + (time_to_move_object * number_of_moves)
#      expected_terminus_time = model.getAttentionClock + minds_eye_lifespan
#    end
#      
#    #Since the action should have been performed, the attention clock of the 
#    #CHREST model and the terminus value of the mind's eye should have both been 
#    #altered and should equal the values passed in the 
#    #"expectedAttentionFreeTime" and "expectedMindsEyeTerminusTime" variables.
#    post_action_checks(model, domain_time, expected_attention_free_time, expected_terminus_time, action_description)
#    
#    #Attempts to create a new mind's eye and to retrieve contents of the mind's 
#    #eye should be blocked if attempted at any time between the value of 
#    #"domain_time" and before the value of "expected_attention_free_time".
#    attention_not_free_time = rand(domain_time...expected_attention_free_time)
#    assert_false(model.createNewMindsEye(["A;0;2", "B;3;4"], 500, 50, 100, 250, attention_not_free_time ) , "Occurred when attempting to initialise a new minds eye after" + action_description +".")
#    minds_eye_contents = Array.new
#    minds_eye_contents[0] = model.getMindsEyeContent(attention_not_free_time)
#    minds_eye_contents[1] = model.getSpecificMindsEyeContent(1, 2, attention_not_free_time)
#    assert_equal(nil, minds_eye_contents[0], "Occurred when attempting to retrieve the visual-spatial contents of the mind's eye at time " + attention_not_free_time.to_s + " using the Chrest.getMindsEyeContent() function after " + action_description + ".")
#    assert_equal(nil, minds_eye_contents[1], "Occurred when attempting to retrieve the visual-spatial contents of the mind's eye at time " + attention_not_free_time.to_s + " using the Chrest.getMindsSpecificMindsEyeContent() function after " + action_description + ".")
#  
#    #Simulate domain time passing by setting "domain_time" to a value greater 
#    #than the current time that CHREST's attention will be free but less than 
#    #the minds eye terminus.  Retrieval of mind's eye content should now be
#    #possible and the visual-spatial field should be as specified in the
#    #"expected_minds_eye_contents" parameter passed.
#    domain_time = rand( (model.getAttentionClock + 1)...(model.getMindsEyeTerminus(domain_time)) )
#    expected_terminus_time = visual_spatial_field_checks(model, domain_time, expected_minds_eye_contents, expected_attention_free_time, minds_eye_lifespan, action_description)
#
#    #Return the updated "domain_time" and "expected_terminus_time" values for 
#    #subsequent use.
#    return domain_time, expected_terminus_time
#  end
#  
#  def post_action_checks(model, domain_time, expected_attention_free_time, expected_terminus_time, action_description)
#    attention_free_time = model.getAttentionClock()
#    terminus_time = model.getMindsEyeTerminus(domain_time)
#    assert_equal(expected_attention_free_time, attention_free_time, "Occurred when checking the CHREST model's attention clock after " + action_description + ".")
#    assert_equal(expected_terminus_time, terminus_time, "Occurred when checking the terminus value of the mind's eye after " + action_description + ".")
#    
#    #Edge-case testing for "expected_terminus_time".
#    assert_true(model.mindsEyeExists( (expected_terminus_time - 1) ), "Occurred during edge case testing when checking for the existence of the minds eye with an expected terminus time of " + (expected_terminus_time - 1).to_s +  " after " + action_description + ".")
#    assert_false(model.mindsEyeExists( (expected_terminus_time) ), "Occurred during edge case testing when checking for the existence of the minds eye with an expected terminus time of " + (expected_terminus_time).to_s +  " after " + action_description + ".")
#    assert_false(model.mindsEyeExists( (expected_terminus_time + 1) ), "Occurred during edge case testing when checking for the existence of the minds eye with an expected terminus time of " + (expected_terminus_time + 1).to_s +  " after " + action_description + ".")
#    
#    #Edge-case testing for "expected_attention_free_time"
#    assert_false(model.attentionFree( (expected_attention_free_time - 1) ), "Occurred during edge case testing when checking whether CHREST's attention is free with an expected attention free time of " + (expected_attention_free_time - 1).to_s +  " after " + action_description + ".")
#    assert_true(model.attentionFree( (expected_attention_free_time) ), "Occurred during edge case testing when checking whether CHREST's attention is free with an expected attention free time of " + (expected_attention_free_time).to_s +  " after " + action_description + ".")
#    assert_true(model.attentionFree( (expected_attention_free_time + 1) ), "Occurred during edge case testing when checking whether CHREST's attention is free with an expected attention free time of " + (expected_attention_free_time + 1).to_s +  " after " + action_description + ".")
#  end
#  
#  def visual_spatial_field_checks(model, domain_time, expected_minds_eye_contents, expected_attention_free_time, minds_eye_lifespan, action_description)
#    
#    #CHREST model attention should be free so that the contents of the 
#    #visual-spatial field can be checked.
#    assert_true(model.attentionFree(domain_time), "Occurred when checking if CHREST's attention is free before after " + action_description + " and when the domain time is greater than the time at which CHREST's attention should be free." )
#    
#    #Check actual visual-spatial field content against what is expected. 
#    minds_eye_contents = Array.new
#    minds_eye_contents[0] = model.getMindsEyeContent(domain_time)
#    minds_eye_contents[1] = get_minds_eye_contents(model, domain_time)
#    minds_eye_contents[0].each_with_index{
#      |val, index|
#      coordinateInfo = val.split(";")
#      assert_equal(expected_minds_eye_contents[index], coordinateInfo[0], "Occurred with element " + index.to_s + " when checking the contents of the mind's eye using the Chrest.getMindsEyeContent() function at a time when CHREST's attention should be free (" + domain_time.to_s + ") and after " + action_description + ".")
#    }
#    minds_eye_contents[1].each_with_index{ 
#      |val, index| 
#      assert_equal(expected_minds_eye_contents[index], val, "Occurred with element " + index.to_s + " when checking the contents of the mind's eye using the Chrest.getMindsSpecificMindsEyeContent() function at a time when CHREST's attention should be free (" + domain_time.to_s + ") and after " + action_description + ".") 
#    }
#    
#    #Since retrieval of mind's eye content does not incur a time cost with 
#    #regards to attention, at the current value of "domain_time" the following 
#    #conditions should all be true
#    assert_true(model.mindsEyeExists(domain_time), "Occurred when checking for the existence of the mind's eye after checking the visual-spatial field of the mind's eye at time " + domain_time.to_s + " and after " + action_description + ".")
#    assert_true(model.attentionFree(domain_time), "Occurred when checking if CHREST's attention is free after checking the visual-spatial field of the mind's eye at time " + domain_time.to_s + " and after " + action_description + ".")
#    assert_equal(expected_attention_free_time, model.getAttentionClock(), "Occurred when checking the CHREST model's attention clock after checking the visual-spatial field of the mind's eye at time " + domain_time.to_s + " and after " + action_description + ".")
#    
#    #Retrieval of mind's eye content does affect the minds eye terminus value 
#    #though so this should be checked and returned for subsequent use.
#    expected_terminus_time = domain_time + minds_eye_lifespan
#    assert_equal(expected_terminus_time, model.getMindsEyeTerminus(domain_time), "Occurred when checking the terminus value of the mind's eye after checking the visual-spatial field of the mind's eye at time " + domain_time.to_s + " and after " + action_description + ".")
#    return expected_terminus_time
#  end
  
  ##############################################################################
  # Create and instantiate a mind's eye and check that its representation of the
  # domain environment is correct.
  ##############################################################################
  # 
  # The vision used to instantiate the mind's eye 2D array in these tests 
  # resembles a "cone" of vision i.e. the further ahead the observer sees, the
  # wider its field of vision.  A diagram of this vision can be found below:
  # "-" and "|" are used to denote squares that represent units of vision, "x" 
  # represents a "blind spot", "A"/"B"/"C"/"D" represent 4 distinct objects that 
  # the observer can see and the numbers to the bottom and left of the diagram 
  # represent the minds eye x and y coordinates, respectively. 
  # 
  #   -------------------------------
  # 2 |     | C,D |     |     |     | 
  #   -------------------------------
  # 1   x   |     |  B  |     |  x
  #         -------------------
  # 0   x      x  |  A  |  x     x
  #               -------
  #     0      1     2     3     4
  #
  # The 2D mind's eye array created should be composed of 5 first dimension 
  # array elements that represent the x coordinates of the vision and a 3 
  # element array in each of the first dimension elements to represent the y 
  # coordinates of the vision.  The 2D mind's eye array element 0, 0 should 
  # contain "null" since the domain-specific coordinates -2, 1 can not be seen.  
  # The 2D mind's eye array element 0, 2 (the square to the left of where 
  # objects C and D are originally located should contain "." since the square 
  # is empty.  
  
  action_description = "initial mind's eye instantiation"
  
  #Create the scene to be transposed into the mind's eye.
  scene = Scene.new("Test scene", 3, 5)
  scene.addItemToSquare(0, 2, object_a)
  scene.addItemToSquare(1, 2, object_b)
  scene.addItemToSquare(2, 1, object_c)
  scene.addItemToSquare(2, 1, object_d)
  
  #Represents the initial state of the mind's eye.
  expected_minds_eye_scene = scene.getScene
  
  assert_false(model.mindsEyeExists(domain_time), "Occurred when checking for the existence of the minds eye before " + action_description + ".")
  assert_true(model.attentionFree(domain_time), "Occurred when checking if CHREST's attention is free before " + action_description + ".")
  
  model.createNewMindsEye(
    scene, 
    minds_eye_lifespan, 
    object_placement_time, 
    minds_eye_access_time, 
    time_to_move_object, 
    domain_time,
    lifespan_for_recognised_objects,
    lifespan_for_unrecognised_objects
  )
  
  # Since none of the scene has been learned yet, the expected attention free
  # time should be equal to the domain time that the minds eye was created plus
  # the time taken to access the minds eye plus time taken to place 4 individual
  # objects.
  expected_attention_free_time = (domain_time + minds_eye_access_time + (object_placement_time * 4) )
  expected_terminus_time = (expected_attention_free_time + minds_eye_lifespan)
  
  assert_equal(expected_attention_free_time, model.getAttentionClock(), "Attention clock not correct")
  assert_equal(expected_terminus_time, model.getMindsEyeTerminus(expected_attention_free_time), "Minds eye terminus not correct")
  minds_eye_scene = model.getMindsEyeScene(expected_attention_free_time).getScene();
  assert_equal(minds_eye_scene, expected_minds_eye_scene)
  
#  values_returned = legal_action_checks(model, domain_time, false, nil, nil, minds_eye_access_time, time_to_move_object, expected_attention_free_time, expected_terminus_time, expected_minds_eye_contents, minds_eye_lifespan, action_description)
#  domain_time = values_returned[0]
#  expected_terminus_time = values_returned[1]
  
#  ##############################################################################
#  # Move two objects, multiple times each
#  ##############################################################################
#  
#  action_description = "moving two objects multiple times each"
#  
#  #Since the time at which CHREST's attention is free will not have changed from
#  #instantiation (previous move was illegal), simulate domain time passing by 
#  #setting "domainTime" to a value greater than its current value but less than
#  #the minds eye terminus.
#  domain_time = rand((domain_time + 1)...model.getMindsEyeTerminus(domain_time))
#  prior_move_performance_checks(model, domain_time, action_description)
#  
#  move_sequence_1 = ArrayList.new
#  move_sequence_1.add(object_c + ";-1;3")
#  move_sequence_1.add(object_c + ";0;3")
#  move_sequence_1.add(object_c + ";1;3")
#  
#  move_sequence_2 = ArrayList.new
#  move_sequence_2.add(object_b + ";0;2")
#  move_sequence_2.add(object_b + ";-1;2")
#  move_sequence_2.add(object_b + ";-1;3")
#  
#  two_objects_multiple_times = ArrayList.new
#  two_objects_multiple_times.add(move_sequence_1)
#  two_objects_multiple_times.add(move_sequence_2)
#  
#  expected_minds_eye_contents[4] = empty
#  expected_minds_eye_contents[5] = object_d + "," + object_b
#  expected_minds_eye_contents[7] = empty
#  expected_minds_eye_contents[8] = empty
#  expected_minds_eye_contents[11] = object_c
#  
#  values_returned = legal_action_checks(model, domain_time, true, two_objects_multiple_times, 4, minds_eye_access_time, time_to_move_object, expected_attention_free_time, expected_terminus_time, expected_minds_eye_contents, minds_eye_lifespan, action_description)
#  domain_time = values_returned[0]
#  expected_terminus_time = values_returned[1]
#  
#  ##############################################################################
#  # Move two objects onto the same coordinates in the mind's eye
#  ##############################################################################
#  
#  action_description = "moving two objects onto the same coordinates"
#  
#  #Simulate domain time passing by setting "domainTime" to a value greater than
#  #the current domain time but less than the minds eye terminus.
#  domain_time = rand((domain_time + 1)...model.getMindsEyeTerminus(domain_time))
#  prior_move_performance_checks(model, domain_time, action_description)
#  
#  move_sequence_1.clear
#  move_sequence_1.add(object_b + ";-1;3")
#  move_sequence_1.add(object_b + ";1;3")
#  
#  two_objects_same_coord = ArrayList.new
#  two_objects_same_coord.add(move_sequence_1)
#  
#  expected_minds_eye_contents[5] = object_d
#  expected_minds_eye_contents[11] = object_c + "," + object_b
#  
#  values_returned = legal_action_checks(model, domain_time, true, two_objects_same_coord, 1, minds_eye_access_time, time_to_move_object, expected_attention_free_time, expected_terminus_time, expected_minds_eye_contents, minds_eye_lifespan, action_description)
#  domain_time = values_returned[0]
#  expected_terminus_time = values_returned[1]
#  
#  ##############################################################################
#  # Move an object onto coordinates not represented in the mind's eye.
#  # 
#  # This test also checks to see if a coordinates content is correct if the last
#  # object on the initial coordinates specified is moved.
#  ##############################################################################
#  
#  action_description = "moving an object to coordinates not in the current range of the mind's eye"
#  
#  #Simulate domain time passing by setting "domainTime" to a value greater than
#  #the current domain time but less than the minds eye terminus.
#  domain_time = rand( (domain_time + 1)...model.getMindsEyeTerminus(domain_time))
#  prior_move_performance_checks(model, domain_time, action_description)
#  
#  move_sequence_1.clear
#  move_sequence_1.add(object_b + ";1;3")
#  move_sequence_1.add(object_b + ";1;4")
#  
#  move_object_outside_minds_eye_range = ArrayList.new
#  move_object_outside_minds_eye_range.add(move_sequence_1)
#  
#  expected_minds_eye_contents[11] = object_c
#  
#  values_returned = legal_action_checks(model, domain_time, true, move_object_outside_minds_eye_range, 1, minds_eye_access_time, time_to_move_object, expected_attention_free_time, expected_terminus_time, expected_minds_eye_contents, minds_eye_lifespan, action_description)
#  domain_time = values_returned[0]
#  expected_terminus_time = values_returned[1]  
#  
#  #Assert that the object moved out of mind's eye range no longer exists in the
#  #visual-spatial field of the mind's eye.
#  assert_false(model.getMindsEyeContent(domain_time).include?(object_b), "Occurred when checking for the abscence of the object that was moved to coordinates not in the current range of the mind's eye after retrieving all visual-spatial contents at once." )
#  expected_terminus_time = (domain_time + minds_eye_lifespan)
#  assert_equal(expected_terminus_time, model.getMindsEyeTerminus(domain_time), "Occurred when checking the mind's eye terminus after checking to see if the object moved out of mind's eye range is still included in the visual-spatial field.") 
#  
#  ##############################################################################
#  # Move an object onto the same coordinates as another before moving the first
#  # object on these coordinates to other cooridnates.
#  ##############################################################################
#  
#  action_description = "moving first object from shared coordinates"
#  
#  #Simulate domain time passing by setting "domainTime" to a value greater than
#  #the current domain time but less than the minds eye terminus.
#  domain_time = rand( (domain_time + 1)...model.getMindsEyeTerminus(domain_time))
#  prior_move_performance_checks(model, domain_time, action_description)
#  
#  move_sequence_1.clear
#  move_sequence_1.add(object_a + ";0;1")
#  move_sequence_1.add(object_a + ";1;3")
#  
#  move_sequence_2.clear
#  move_sequence_2.add(object_c + ";1;3")
#  move_sequence_2.add(object_c + ";2;3")
#  
#  move_first_object_from_shared_coordinates = ArrayList.new
#  move_first_object_from_shared_coordinates.add(move_sequence_1)
#  move_first_object_from_shared_coordinates.add(move_sequence_2)
#  
#  expected_minds_eye_contents[6] = empty
#  expected_minds_eye_contents[11] = object_a
#  expected_minds_eye_contents[14] = object_c
#  
#  values_returned = legal_action_checks(model, domain_time, true, move_first_object_from_shared_coordinates, 2, minds_eye_access_time, time_to_move_object, expected_attention_free_time, expected_terminus_time, expected_minds_eye_contents, minds_eye_lifespan, action_description)
#  domain_time = values_returned[0]
#  expected_terminus_time = values_returned[1]  
#  
#  ##############################################################################
#  # Familiarise a part of the initial "visionCone" scene and check that minds
#  # eye instantiation time is reduced accordingly.
#  ##############################################################################
#  
#  action_description = "instantiating a new minds eye"
#  
#  object_c_pattern = ItemSquarePattern.new(object_c, -1, 3)
#  object_d_pattern = ItemSquarePattern.new(object_d, -1, 3)
#  
#  pattern_to_learn = ListPattern.new
#  pattern_to_learn.add(object_c_pattern)
#  pattern_to_learn.add(object_d_pattern)
#  result = model.recogniseAndLearn(pattern_to_learn, domain_time).getImage
#  
#  until result.contains(object_c_pattern) && result.contains(object_d_pattern) do
#    domain_time += 10000
#    result = model.recogniseAndLearn(pattern_to_learn, domain_time).getImage
#  end
#  
#  model.createNewMindsEye(vision, minds_eye_lifespan, object_placement_time, minds_eye_access_time, time_to_move_object, domain_time)
#  expected_attention_free_time = ( domain_time + (object_placement_time * 3) )
#  expected_terminus_time = (expected_attention_free_time + minds_eye_lifespan)
#  expected_minds_eye_contents = [
#    null,
#    null,
#    empty,
#    null,
#    empty,
#    object_c + "," + object_d,
#    object_a,
#    object_b,
#    empty,
#    null, 
#    empty,
#    empty,
#    null,
#    null,
#    empty,
#  ]
#  legal_action_checks(model, domain_time, false, nil, nil, minds_eye_access_time, time_to_move_object, expected_attention_free_time, expected_terminus_time, expected_minds_eye_contents, minds_eye_lifespan, action_description)
#  
#  ##############################################################################
#  # Destroy the mind's eye instance and check that it no longer exists
#  ##############################################################################
#  
#  model.destroyMindsEye(domain_time)
#  assert_equal(model.getAttentionClock, domain_time, "Occurred when checking the value of the CHREST model's '_attentionClock' instance variable after destroying the mind's eye at time " + domain_time.to_s + ".")
#  assert_false(model.mindsEyeExists(domain_time), "Occurred when checking for the existence of the mind's eye after destroying it.")
end
