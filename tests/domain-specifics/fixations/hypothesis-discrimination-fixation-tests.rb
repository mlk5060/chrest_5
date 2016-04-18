unit_test "constructor" do
  time = 0
  model = Chrest.new(time, false)
  fixation = HypothesisDiscriminationFixation.new(model, time)
  
  assert_equal(
    time + model.getTimeToRetrieveItemFromStm(),
    fixation.getTimeDecidedUpon(),
    "occurred when checking what time the fixation should be decided upon"
    )
end

################################################################################
# 11 scenarios are tested here and repeated twice: the first repeat has the
# CHREST model created learning object locations not-relative to the agent
# equipped with CHREST whereas the second repeat does.
# 
# Scenario 1: Pass
#   - Scene not entirely blind
#   - Hypothesis present in visual STM
#   - Hypothesis has children
#   - First child of hypothesis has a test-link
#   - First item in test-link of first hypothesis child is an ItemSquarePattern
#   - Square indicated by ItemSquarePattern above is represented in the Scene
#     that the fixation is to be made in context of
#   - Square indicated by ItemSquarePattern above is not blind in the Scene
#     that the fixation is to be made in context of
#   - Square indicated by ItemSquarePattern above is not already being fixated
#     upon in the Scene that the fixation is to be made in context of
#   - Fixation attempted at time fixation is scheduled to be performed
#   - Visual STM hypothesis should be updated.
#   
# Scenario 2: Pass
#   - As scenario 1 but fixation attempted after fixation scheduled to be
#     performed.
#     
# Scenario 3: Fail 
#   - As scenario 1 but fixation attempted before fixation performance time.
#   
# Scenario 4: Fail 
#   - Scene entirely blind.
#
# Scenario 5: Fail
#   - Scene not entirely blind.
#   - Time fixation is to be made is before model creation time
#
# Scenario 6: Fail
#   - Scene not entirely blind.
#   - Time fixation is to be made is after model creation time but before 
#     visual STM has any Nodes added.
#
# Scenario 7: Fail
#   - Scene not entirely blind.
#   - Time fixation is to be made is after visual STM has Nodes added.
#   - First child of visual STM hypothesis has empty test on link to it
#
# Scenario 8: Fail
#   - Scene not entirely blind.
#   - Time fixation is to be made is after visual STM has Nodes added.
#   - First child of visual STM hypothesis has non-empty test on link to it
#   - First item of test on link to first child of visual STM hypothesis is
#     not an ItemSquarePattern
#
# Scenario 9: Fail
#   - Scene not entirely blind.
#   - Time fixation is to be made is after visual STM has Nodes added.
#   - First child of visual STM hypothesis has non-empty test on link to it
#   - First item of test on link to first child of visual STM hypothesis is
#     an ItemSquarePattern
#   - First item of test on link to first child of visual STM hypothesis 
#     specifies a Square not present in the Scene to make the fixation in 
#     context of.
#
# Scenario 10: Fail
#   - Scene not entirely blind.
#   - Time fixation is to be made is after visual STM has Nodes added.
#   - First child of visual STM hypothesis has non-empty test on link to it
#   - First item of test on link to first child of visual STM hypothesis is
#     an ItemSquarePattern
#   - First item of test on link to first child of visual STM hypothesis 
#     specifies a Square present in the Scene to make the fixation in context 
#     of.
#   - Square specified is blind
#
# Scenario 11: Fail
#   - Scene not entirely blind.
#   - Time fixation is to be made is after visual STM has Nodes added.
#   - First child of visual STM hypothesis has non-empty test on link to it
#   - First item of test on link to first child of visual STM hypothesis is
#     an ItemSquarePattern
#   - First item of test on link to first child of visual STM hypothesis 
#     specifies a Square present in the Scene to make the fixation in context 
#     of.
#   - Square specified is not blind
#   - Previous fixation looked at same Square as that proposed in new fixation
unit_test "make" do
  
  scene_field = Scene.java_class.declared_field("_scene")
  scene_field.accessible = true
  
  Node.class_eval{
    field_accessor :_childHistory
  }
  
  Fixation.class_eval{
    field_accessor :_timeDecidedUpon, :_performanceTime, :_performed, :_scene, :_colFixatedOn, :_rowFixatedOn, :_objectSeen
  }
  
  stm_item_history_field = Stm.java_class.declared_field("_itemHistory")
  stm_item_history_field.accessible = true
  
  perceiver_fixations = Perceiver.java_class.declared_field("_fixations")
  perceiver_fixations.accessible = true
  
  for repeat in 1..2
    for scenario in 1..11
      
      ########################
      ##### SETUP CHREST #####
      ########################

      time = 0 
      model_creation_time = (time += 5) 
      model = Chrest.new(model_creation_time, (repeat == 1 ? false : true))
      
      #################################
      ##### SET-UP FIXATION SCENE #####
      #################################

      # Construct scene.  Note that the minimum domain-specific column and row
      # coordinates represented are not 0 so don't map directly onto 
      # scene-specific coordinates.  This means that the coordinate translation 
      # mechanisms in the function will be tested thoroughly.
      scene = Scene.new("", 3, 3, 4, 4, nil) # Will be entirely blind
      
      # Set items in Scene (the SELF, i.e the creator of the Scene is only 
      # present in repeat 2).  In the test, the Fixation to be made should be on
      # coordinates (Square) [5, 5] (scene-specific = [1, 1]).  Remember that, 
      # in scenario 10, the Square to fixate on should be blind so, depending on
      # the scenario, the final Scene to fixate on will look like either of the 
      # following (DS = domain-specific coordinates, SS = scene-specific):
      #       
      #        Scenario != 10      Scenario = 10
      # SS DS
      #       |----|----|----|    |----|----|----|
      # 2  6  |SELF|    |    |    |SELF|    |    |
      #       |----|----|----|    |----|----|----|
      # 1  5  |    | H  |    |    |    | x  |    |
      #       |----|----|----|    |----|----|----|
      # 0  4  | T  |    |    |    | T  |    |    |
      #       |----|----|----|    |----|----|----|
      #         4    5    6        4     5    6
      #         0    1    2        0     1    2
      if scenario != 4
        scene_state = scene_field.value(scene)
        scene_state.get(0).set(0, SceneObject.new("1", "T"))
        scenario == 10 ? 
          scene_state.get(1).set(1, SceneObject.new(Scene.getBlindSquareToken(), Scene.getBlindSquareToken())) :
          scene_state.get(1).set(1, SceneObject.new("2", "H"))
        if repeat == 2 then scene_state.get(0).set(2, SceneObject.new("0", Scene.getCreatorToken())) end
      end

      ########################
      ##### SET-UP NODES #####
      ########################
      
      # For the function to operate as expected, there should be a visual STM
      # hypothesis Node whose contents are present in the Scene and whose first
      # child Node contents are also present in the Scene (the first child 
      # Node's contents should be the result of the function, if null is to not
      # be returned; dictated by scenario).  In context of the test then, the 
      # Fixation should be made on the centre of the Scene (domain-specific 
      # coordinates [5, 5], scene-specific [1, 1]).  Therefore, the contents of 
      # the visual STM hypothesis will be [T, 4, 4] since this is represented in 
      # the Scene.

      # Set-up Node 1.  This Node will be the visual STM hypothesis when the 
      # Fixation is made so its contents should be equal to [T, 4, 4].  To make 
      # the test more realistic, include another ItemSquarePattern in its image 
      # that would cause its children to be created if the LTM discrimination
      # network were grown organically.
      time += 100
      node_1_contents = ListPattern.new(Modality::VISUAL)
      node_1_contents.add(ItemSquarePattern.new("T", (repeat == 1 ? 4 : 0), (repeat == 1 ? 4 : -2)))
      node_1_image = ListPattern.new(Modality::VISUAL)
      node_1_image.add(ItemSquarePattern.new("T", (repeat == 1 ? 4 : 0), (repeat == 1 ? 4 : -2)))
      node_1_image.add(ItemSquarePattern.new("H", (repeat == 1 ? 4 : 1), (repeat == 1 ? 4 : 0)))
      node_1 = Node.new(model, node_1_contents, node_1_image, time)

      # Set-up Node 2. Since the last child added to a Node becomes its first 
      # child, the contents of this Node should not be selected as the Square to
      # fixate on.  Indeed, this is why this Node is used: it allows for 
      # verification that it is the first child of the visual STM hypotheses'
      # contents that is selected, not any other child's.  Note that its 
      # contents refers to a Square that isn't represented in the Scene, this is
      # desirable for certain test scenarios. 
      time += 100
      node_2_contents = ListPattern.new(Modality::VISUAL)
      node_2_contents.add(ItemSquarePattern.new("H", (repeat == 1 ? 4 : -1), (repeat == 1 ? 7 : 3)))
      node_2_image = ListPattern.new(Modality::VISUAL)
      node_2_image.add(ItemSquarePattern.new("T", (repeat == 1 ? 4 : 0), (repeat == 1 ? 4 : -2)))
      node_2_image.add(ItemSquarePattern.new("H", (repeat == 1 ? 4 : -1), (repeat == 1 ? 7 : 3)))
      node_2 = Node.new(model, node_2_contents, node_2_image, time)

      # Set-up node 3.  This is the Node whose contents will be selected for 
      # Fixation so there is some differences in its contents depending on 
      # scenario 
      time += 100
      node_3_contents = ListPattern.new(Modality::VISUAL)

      if scenario != 7  
        node_3_contents.add(
          (scenario == 8 ? 
            Pattern.makeString("Bad") : 
            ItemSquarePattern.new(
              "H", 
              (repeat == 1 ? 5 : 1), 
              (repeat == 1 ? 5 : -1)
            )
          )
        )
      end

      node_3_image = ListPattern.new(Modality::VISUAL)
      node_3_image.add(ItemSquarePattern.new("T", (repeat == 1 ? 4 : 0), (repeat == 1 ? 4 : -2)))
      node_3_image.append(node_3_contents)
      node_3 = Node.new(model, node_3_contents, node_3_image, time)

      # Add node 2 as a child of node 1.
      time += 100
      node_1_node_2_child_history = ArrayList.new()
      node_1_node_2_child_history.add(Link.new(node_2_contents, node_2, time, ""))
      node_1._childHistory.put(time, node_1_node_2_child_history)

      # Add node 3 as a child of node 1, this will now be the first child of 
      # node 1.  However, don't do this in scenario 9 since this scenario checks
      # if a Square suggested by the first child contents of the visual STM 
      # hypothesis is handled correctly. Therefore, in scenario 9, node 2 will 
      # be the first child of the visual STM hypothesis and its contents will be
      # used to make this Fixation however, the Square it refers to are not 
      # present in the Scene.
      if scenario != 9
        time += 100
        node_1_node_3_child_history = ArrayList.new()
        node_1_node_3_child_history.add(Link.new(node_3_contents, node_3, time, ""))
        node_1_node_3_child_history.addAll(node_1_node_2_child_history)
        node_1._childHistory.put(time, node_1_node_3_child_history)
      end

      ############################
      ##### SETUP VISUAL STM #####
      ############################

      # Add Node 1 as hypothesis to visual STM after all Nodes created.
      time += 100
      time_when_hypothesis_added_to_visual_stm = time
      stm_items = ArrayList.new()
      stm_items.add(node_1)
      current_stm_items = stm_item_history_field.value(model.getStm(Modality::VISUAL))
      current_stm_items.put(time_when_hypothesis_added_to_visual_stm, stm_items)
      
      ################################
      ##### SETUP PRIOR FIXATION #####
      ################################
      
      # In scenario 11, add a Fixation that was made after the model was created 
      # which fixated on the same Square as that which will be proposed by the 
      # HypothesisDiscriminationFixation.
      if scenario == 11
        prev_fixation = CentralFixation.new(model_creation_time)
        prev_fixation._performanceTime = (model_creation_time + 1)
        prev_fixation._performed = true
        prev_fixation._scene = scene 
        prev_fixation._colFixatedOn = 1 
        prev_fixation._rowFixatedOn = 1
        prev_fixation._objectSeen = scene_field.value(scene).get(1).get(1)
        
        current_fixations = perceiver_fixations.value(model.getPerceiver())
        fixations = ArrayList.new()
        fixations.add(prev_fixation)
        current_fixations.put(prev_fixation._performanceTime, fixations)
      end

      ###########################
      ##### SET-UP FIXATION #####
      ###########################
      
      time += 100
      fixation = HypothesisDiscriminationFixation.new(model, time)
      fixation._performanceTime = fixation._timeDecidedUpon() + 100
      time = fixation._performanceTime()

      # Set time fixation is to be made
      time_fixation_to_be_made = time
      if scenario == 2 then time_fixation_to_be_made += 10 end
      if scenario == 3 then time_fixation_to_be_made -= 10 end
      if scenario == 5 then time_fixation_to_be_made = (model_creation_time - 1) end
      if scenario == 6 then time_fixation_to_be_made = rand(model_creation_time...time_when_hypothesis_added_to_visual_stm) end

      ##################################
      ##### SET EXPECTED VARIABLES #####
      ##################################

      # Set expected fixation
      expected_fixation = nil
      if [1,2].include?(scenario)
        expected_fixation = Square.new(1, 1)
      end
      
      # Set expected STM hypothesis, if the fixation was made successfully, this
      # should be the last child added to the hypothesis whose first test-link
      # item is present on the scene the fixation is made on.  So, since scenarios
      # 1 & 2 are the only 2 scenarios where the fixation should be made 
      # successfully, the STM hypothesis node should be updated to node 3.
      expected_stm_hypothesis = nil
      if [1,2].include?(scenario)
        expected_stm_hypothesis = node_3
      else
        expected_stm_hypothesis = node_1 
      end

      ###########################
      ##### INVOKE FUNCTION #####
      ###########################

      fixation_made = fixation.make(scene, time_fixation_to_be_made)

      #################
      ##### TESTS #####
      #################

      assert_equal(
        expected_fixation,
        fixation_made,
        "occurred when checking the fixation made in scenario " + scenario.to_s + 
        " on repeat " + repeat.to_s
      )

      #Get the most up-to-date contents of STM.
      stm_hypothesis = model.getStm(Modality::VISUAL).iterator().next()
      assert_equal(
        expected_stm_hypothesis.getReference(),
        stm_hypothesis.getReference(),
        "occurred when checking the STM hypothesis in scenario " + scenario.to_s + 
        " on repeat " + repeat.to_s
      )
    end
  end
end
