require_relative "visual-spatial-field-tests"

################################################################################
# Checks that the Perceiver constructor sets the "_fixations" data structure 
# correctly, i.e. should be empty at the time of construction.
# 
# Unfortunately, its not possible to test if the associated CHREST model is set
# correctly since a Perceiver object is constructed when a CHREST model is which
# means that its not possible to test the Perceiver constructor directly. 
unit_test "constructor" do
  time = 0
  
  # Perceiver constructor is package access only so it needs to be made 
  # accessible through reflection.
  constructor = Perceiver.java_class.declared_constructors[0] 
  constructor.accessible = true
  perceiver = constructor.new_instance(Chrest.new(time, false), time).to_java
  
  # The "_fixations" field is private and final so Perceiver.class_eval can't be
  # used to make the field accessible, it needs to be done "manually".
  fixation_field = perceiver.java_class.declared_field("_fixations")
  fixation_field.accessible = true

  assert_true(
    fixation_field.value(perceiver).floorEntry(time.to_java(:int)).getValue().isEmpty(),
    "occurred when checking that the fixation data structure is initialised as expected"
  )
end

################################################################################
# Uses 24 scenarios, each of which are repeated 100 times to ensure that 
# functionality is consistent, to test the "addFixation" method.  Each scenario
# tests an individual aspect of the function (usually a sub-conditional).
# 
# Scenario 1/13:
#   - Fixation is null
# 
# Scenario 2/14: 
#   - Fixation is not null
#   - No performance time set for Fixation
# 
# Scenario 3/15:
#   - Fixation is not null
#   - Performance time set for Fixation but this is before the creation time of 
#     the Perceiver that the Fixation is being added to.
#     
# Scenario 4/16:
#   - Fixation is not null
#   - Fixation performance time set to a value after the Perceiver has been 
#     created.  
#   - Fixation performance set to false.
#     
# Scenario 5/17:
#   - Fixation is not null
#   - Fixation performance time set to a value after the Perceiver has been 
#     created.  
#   - Fixation performance set to true.
#   - Scene fixated on set to nil
#   
# Scenario 6/18: 
#   - Fixation is not null
#   - Fixation performance time set to a value after the Perceiver has been 
#     created.  
#   - Fixation performance set to true.
#   - Scene fixated on not set to nil.
#   - Column fixated on set to nil.
#   
# Scenario 7/19: 
#   - Fixation is not null
#   - Fixation performance time set to a value after the Perceiver has been 
#     created.  
#   - Fixation performance set to true.
#   - Scene fixated on not set to nil.
#   - Column fixated on not set to nil.
#   - Row fixated on set to nil.
#   
# Scenario 8/20:
#   - Fixation is not null
#   - Fixation performance time set to a value after the Perceiver has been 
#     created.  
#   - Fixation performance set to true.
#   - Scene fixated on not set to nil.
#   - Column fixated on not set to nil.
#   - Row fixated on not set to nil.
#   
#   - CHREST model is not learning object locations relative to itself
#   - No Nodes in visual STM.
#
# Scenario 9/21:
#   - Fixation is not null
#   - Fixation performance time set to a value after the Perceiver has been 
#     created.  
#   - Fixation performance set to true.
#   - Scene fixated on not set to nil.
#   - Column fixated on not set to nil.
#   - Row fixated on not set to nil.
#   
#   - CHREST model is not learning object locations relative to itself
#   - Template Node as visual STM hypothesis.
#   
# Scenario 10/22:
#   - Fixation is not null
#   - Fixation performance time set to a value after the Perceiver has been 
#     created.  
#   - Fixation performance set to true.
#   - Scene fixated on not set to nil.
#   - Column fixated on not set to nil.
#   - Row fixated on not set to nil.
#   
#   - CHREST model is learning object locations relative to itself
#   - Location of agent equipped with CHREST is not encoded in the Scene fixated
#     on
#   
# Scenario 11/23:
#   - Fixation is not null
#   - Fixation performance time set to a value after the Perceiver has been 
#     created.  
#   - Fixation performance set to true.
#   - Scene fixated on not set to nil.
#   - Column fixated on not set to nil.
#   - Row fixated on not set to nil.
#   
#   - CHREST model is learning object locations relative to itself
#   - Location of agent equipped with CHREST is encoded in the Scene fixated on
#   - No Nodes in visual STM. 
#   
# Scenario 12/24:
#   - Fixation is not null
#   - Fixation performance time set to a value after the Perceiver has been 
#     created.  
#   - Fixation performance set to true.
#   - Scene fixated on not set to nil.
#   - Column fixated on not set to nil.
#   - Row fixated on not set to nil.
#   
#   - CHREST model is learning object locations relative to itself
#   - Location of agent equipped with CHREST is encoded in the Scene fixated on
#   - Template Node as visual STM hypothesis.
#   
# Scenarios 13-24 are as Scenarios 1-12 but the Scene fixated on represents a
# VisualSpatialField.
#     
# NOTE: To test whether the visual STM hypotheses' slot values are filled the 
#       function will be invoked twice in scenarios 8/19 and 11/22.  This is 
#       because the template Node must first be made the visual STM hypothesis 
#       (occurs during the first function invocation) and the function must then 
#       be called again to ascertain if the "fill slots" functionality operates
#       as expected.
#
# The Scene that the Fixations added are made in context of is shown below. 
# Note that the actual (DS/domain-specific) coordinates and Scene coordinates 
# (SC) are different.  This enables the test to verify if the coordinate 
# translation functionality in the "addFixation" function operates correctly.
# 
# SC  DS
#        |----|----|----|----|----|
# 4   5  |    |    |    |    |    |
#        |----|----|----|----|----|
# 3   4  |    |    | P  |    | K  |
#        |----|----|----|----|----|
# 2   3  | G  |    |    |    |    |
#        |----|----|----|----|----|
# 1   2  |    |    |SELF| H  |    |
#        |----|----|----|----|----|
# 0   1  |    |    |    |    |    |
#        |----|----|----|----|----|
#          1    2    3    4    5
#          0    1    2    3    4
#
# The test checks the following after each invocation of "addFIxation":
# 
# - Whether an exception is thrown.
# - The ListPattern returned.
# - The state of the Perceiver's "Fixations attempted" data structure.
# - The cognition clock of the CHREST model associated with the Perceiver adding
#   the Fixation.
# - The attention clock of the CHREST model associated with the Perceiver adding
#   the Fixation.
unit_test "add_fixation" do
  
  Chrest.class_eval{
    field_accessor :_recognisedVisualSpatialFieldObjectLifespan, 
      :_unrecognisedVisualSpatialFieldObjectLifespan,
      :_timeTakenToDecideUponCentralFixations,
      :_cognitionClock,
      :_attentionClock,
      :_nextLtmNodeReference
  }
  
  vsf_field = VisualSpatialField.java_class.declared_field("_visualSpatialField")
  vsf_field.accessible = true
  
  VisualSpatialFieldObject.class_eval{
    field_accessor :_terminus
  }
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  # Need access to private instance variables of Fixation.
  Fixation.class_eval{
    field_accessor :_scene, :_colFixatedOn, :_rowFixatedOn, :_performanceTime, :_performed
  }
  
  # Need access to private instance variables of Node for template construction
  # so that scenario 8 can operate correctly.
  Node.class_eval{
    field_accessor :_childHistory, 
    :_itemSlotsHistory, 
    :_positionSlotsHistory, 
    :_filledItemSlotsHistory, 
    :_filledPositionSlotsHistory, 
    :_templateHistory
  }
  
  for scenario in 1..24
    100.times do
      time = 0
      
      ###########################################
      ##### SET-UP CHREST MODEL & PERCEIVER #####
      ###########################################
     
      # Set the CHREST model's "_learnObjectLocationsRelativeToAgent" variable 
      # to what is expected in the scenario.
      learn_object_locations_relative_to_self = ([10,11,12,22,23,24].include?(scenario) ? true : false)
      model = Chrest.new(time, learn_object_locations_relative_to_self)
      
      # Set the model's VisualSpatialFieldObject lifespan variables to very high
      # numbers so that, if a VisualSpatialField is encoded and the 
      # "addFixation()" function is invoked twice, the VisualSpatialFieldObjects
      # will not have decayed by the time of the second invocation and their 
      # termini are refreshed as expected.  The 
      # _unrecognisedVisualSpatialFieldObjectLifespan parameter is set to a 
      # slightly lower value to enable verification of correct termini 
      # refreshment functionality.
      model._recognisedVisualSpatialFieldObjectLifespan = 9999999
      model._unrecognisedVisualSpatialFieldObjectLifespan = 9999990

      # Construct Perceiver (one is constructed when a CHREST model is created: 
      # easier than using reflection, see 'constructor' test above), ensure that 
      # its fixation field of view completely encompasses the Scene that the 
      # Fixation to add is made on and 
      
      perceiver = model.getPerceiver()
      perceiver.setFixationFieldOfView(2) # Can see 5 * 5 squares in scene (whole scene)   

      #############################################################
      ##### CONSTRUCT Scene FIXATED ON AND VisualSpatialField #####
      #############################################################
      
      # The VisualSpatialField should be an exact replica of the Scene.
      visual_spatial_field = nil
      if(scenario > 12)
        
        creator_details = nil
        if [23,24].include?(scenario)
          creator_details = ArrayList.new()
          creator_details.add("0")
          creator_details.add(Square.new(2, 1))
        end
        visual_spatial_field = VisualSpatialField.new("", 5, 5, 1, 1, model, creator_details, time)
        
        #Populate VisualSpatialField with remaining VisualSpatialFieldObjects.
        vsf = vsf_field.value(visual_spatial_field)
        vsf.get(2).get(3).lastEntry().getValue().add(VisualSpatialFieldObject.new("1", "P", model, visual_spatial_field, time, true, true))
        vsf.get(4).get(3).lastEntry().getValue().add(VisualSpatialFieldObject.new("2", "K", model, visual_spatial_field, time, false, true))
        vsf.get(0).get(2).lastEntry().getValue().add(VisualSpatialFieldObject.new("3", "G", model, visual_spatial_field, time, true, true))
        vsf.get(3).get(1).lastEntry().getValue().add(VisualSpatialFieldObject.new("4", "H", model, visual_spatial_field, time, false, true))
      end

      # Minimum domain column and row coordinates should not be zero-indexed so
      # they're different to Scene-specific coordinates).
      scene = Scene.new("", 5, 5, 1, 1, visual_spatial_field)
      
      #Add items using scene-specific coordinates.
      if [11,12,23,24].include?(scenario) then scene._scene.get(2).set(1, SceneObject.new("0", Scene.getCreatorToken())) end
      scene._scene.get(2).set(3, SceneObject.new("1", "P"))
      scene._scene.get(4).set(3, SceneObject.new("2", "K"))
      scene._scene.get(0).set(2, SceneObject.new("3", "G"))
      scene._scene.get(3).set(1, SceneObject.new("4", "H"))

      ##############################
      ##### CONSTRUCT FIXATION #####
      ##############################

      fixation = CentralFixation.new(time, model._timeTakenToDecideUponCentralFixations)
      fixation._performanceTime = (time + 10).to_java(:int)
      fixation._performed = true
      fixation._scene = scene
      fixation._colFixatedOn = 2
      fixation._rowFixatedOn = 2

      if [1, 13].include?(scenario) then fixation = nil end
      if [2, 14].include?(scenario) then fixation._performanceTime = nil end
      if [3, 15].include?(scenario) then fixation._performanceTime = (time - 10).to_java(:int) end
      if [4, 16].include?(scenario) then fixation._performed = false end
      if [5, 17].include?(scenario) then fixation._scene = nil end
      if [6, 18].include?(scenario) then fixation._colFixatedOn = nil end
      if [7, 19].include?(scenario) then fixation._rowFixatedOn = nil end

      ###################################
      ##### CONSTRUCT TEMPLATE NODE #####
      ###################################
      
      if [9,12,21,24].include?(scenario)

        # Create the template node image.  This should only contain the first 
        # item in the ListPattern that will be created when the Fixation is 
        # made, i.e. the most south-westerly SceneObject from the Fixation point 
        # which is "H" in this case.  No other items are in the image since, 
        # when the template's slot values are filled, if an item is in the 
        # template's image, its slot value will not be filled.  Furthermore,
        # "G"s information will be added to the image when the "addFixation" is 
        # first invoked (the next south-westerly SceneObject from "H" hence, the
        # next item in the ListPattern constructed and learned). This leaves
        # SceneObjects "P" and "K" as slot values.
        template_node_image = ListPattern.new(Modality::VISUAL)
        template_node_image.add(ItemSquarePattern.new(
          "H",
          ([9, 21].include?(scenario) ? 4 : 1), 
          ([9, 21].include?(scenario) ? 2 : 0) 
        ))

        # Content of template node should be the first item that is "seen" in 
        # the ListPattern constructed so the template Node is retrieved when 
        # recogniseAndLearn is first invoked in the "addFixation" function.  
        # Otherwise, the test can't verify that the template Node's slots are 
        # filled correctly.
        template_node_content = ListPattern.new(Modality::VISUAL)
        template_node_content.add(template_node_image.getItem(0))
        template_node = Node.new(model, template_node_content, template_node_image, time)
        model._nextLtmNodeReference += 1

        # Specify slot values for template node.
        itemSlots = ArrayList.new()
        itemSlots.add("P")
        itemSlots.add("K")
        positionSlots = ArrayList.new()
        positionSlots.add(Square.new(
          ([9, 21].include?(scenario) ? 3: 0),
          ([9, 21].include?(scenario) ? 4: 2)
        ))
        positionSlots.add(Square.new(
          ([9, 21].include?(scenario) ? 5 : 2), 
          ([9, 21].include?(scenario) ? 4 : 2)
        ))

        # Instantiate template variables for the Node (these are all set to null
        # when a Node is created to save on memory since most Nodes do not become
        # templates).
        template_node._templateHistory = HistoryTreeMap.new()
        template_node._itemSlotsHistory = HistoryTreeMap.new()
        template_node._positionSlotsHistory = HistoryTreeMap.new()
        template_node._filledItemSlotsHistory = HistoryTreeMap.new()
        template_node._filledPositionSlotsHistory = HistoryTreeMap.new()

        # Set template variables for the node.
        template_node._templateHistory.put((time - 1).to_java(:int), true)
        template_node._itemSlotsHistory.put((time - 1).to_java(:int), itemSlots)
        template_node._positionSlotsHistory.put((time - 1).to_java(:int), positionSlots)
        template_node._filledItemSlotsHistory.put((time - 1).to_java(:int), ArrayList.new())
        template_node._filledPositionSlotsHistory.put((time - 1).to_java(:int), ArrayList.new())

        # Construct a child history for the visual LTM root node (don't want to 
        # rely on methods since, if they're changed, this test may break).
        link = Link.new(template_node_content, template_node, time - 1, "")
        links = ArrayList.new()
        links.add(link)

        # Set the visual root Node's child history to that created above.
        visual_ltm_root_node_child_history = HistoryTreeMap.new()
        visual_ltm_root_node_child_history.put(time.to_java(:int), links)
        model.getLtmModalityRootNode(Modality::VISUAL)._childHistory = visual_ltm_root_node_child_history
      end
      
      ##################################
      ##### SET-UP EXPECTED VALUES #####
      ##################################
      
      expected_list_pattern_returned = nil
      
      if [8,9,11,12,20,21,23,24].include?(scenario)
        expected_list_pattern_returned = ListPattern.new(Modality::VISUAL)
        expected_list_pattern_returned.add(ItemSquarePattern.new(
          "H",
          ([11,12,23,24].include?(scenario) ? 1 : 4), 
          ([11,12,23,24].include?(scenario) ? 0 : 2) 
        ))
      
        expected_list_pattern_returned.add(ItemSquarePattern.new(
          "G",
          ([11,12,23,24].include?(scenario) ? -2 : 1), 
          ([11,12,23,24].include?(scenario) ? 1 : 3) 
        ))
        
        expected_list_pattern_returned.add(ItemSquarePattern.new(
          "P",
          ([11,12,23,24].include?(scenario) ? 0 : 3), 
          ([11,12,23,24].include?(scenario) ? 2 : 4) 
        ))
      
        expected_list_pattern_returned.add(ItemSquarePattern.new(
          "K",
          ([11,12,23,24].include?(scenario) ? 2 : 5), 
          ([11,12,23,24].include?(scenario) ? 2 : 4) 
        ))
      end
      
      expected_exception_thrown = ([1,4,8,9,11,12,13,16,20,21,23,24].include?(scenario) ? false : true)

      expected_fixations = ([4,8,9,11,12,16,20,21,23,24].include?(scenario) ? [fixation] : [])

      # The cognition clock is expected to be equal to its initial value if the 
      # Fixation has not been performed or it has been but either the 
      # information required to learn from it is not present or the associated
      # CHREST model is to learn object locations relative to the agent that
      # is equipped with CHREST but the agent's location is not specified in the
      # Scene fixated on.
      # 
      # In scenarios 8/11/20/23, the cognition clock will be equal to the 
      # Fixation's performance time + the time taken to recognise the 
      # ListPattern generated from the field of view around the Fixation (only 1 
      # visual LTM Node: the root, so only 1 link traversed when recognition 
      # occurs) plus the time taken to discriminate (since the first item in the 
      # ListPattern generated from the field of view around the Fixation will 
      # not be recognised and thus will be learned).
      #
      # In scenarios 9/12/21/24, the visual LTM node has a child (the template 
      # Node) which will be traversed to since its image contains the first item 
      # of the ListPattern passed to recogniseAndlearn so 2 LTM links will be 
      # traversed instead of just 1.  The model will discriminate again since 
      # the second item in the ListPattern generated will be unrecognised.
      expected_cognition_clock = 
        (scenario == 8 || scenario == 11 || scenario == 20 || scenario == 23 ?
          fixation._performanceTime + (model.getLtmLinkTraversalTime()) + model.getDiscriminationTime() :
          (scenario == 9 || scenario == 12 || scenario == 21 || scenario == 24 ?
            fixation._performanceTime + (model.getLtmLinkTraversalTime() * 2) + model.getDiscriminationTime() :
            time - 1
          )
        )

      # Calculating the expected attention clock value is much the same as 
      # calculating the expected cognition clock value except, instead of 
      # learning, the model will take time to update STM.
      # 
      # Essentially, the expected attention clock value differs depending on 
      # scenario for the same reasons as the expected cognition clock value does. 
      # In scenarios 8, 11, 20 and 23, nothing is recognised when Fixations are
      # learned from so STM isn't updated whereas in 9, 12, 21 and 24, a 
      # non-root Node is recognised so is added to STM, consuming attention.
      expected_attention_clock = 
        (scenario == 9 || scenario == 12 || scenario == 21 || scenario == 24 ?
          fixation._performanceTime + (model.getLtmLinkTraversalTime() * 2) + model.getTimeToUpdateStm() :
          time - 1
        )
        
      # Set expected VisualSpatialField data
      expected_visual_spatial_field_data = nil
      
      if scenario > 12
        expected_visual_spatial_field_data = Array.new(5){ Array.new(5){ Array.new }}
        
        if [23,24].include?(scenario)
          expected_visual_spatial_field_data[2][1] = [["0", Scene.getCreatorToken(), false, time, nil]]
        end
        
        expected_visual_spatial_field_data[2][3] = [["1", "P", true, time, time + model._recognisedVisualSpatialFieldObjectLifespan]]
        expected_visual_spatial_field_data[4][3] = [["2", "K", false, time, time + model._unrecognisedVisualSpatialFieldObjectLifespan]]
        expected_visual_spatial_field_data[0][2] = [["3", "G", true, time, time + model._recognisedVisualSpatialFieldObjectLifespan]]
        expected_visual_spatial_field_data[3][1] = [["4", "H", false, time, time + model._unrecognisedVisualSpatialFieldObjectLifespan]]

        if [20,21,23,24].include?(scenario)
          expected_visual_spatial_field_data[2][3][0][4] = fixation._performanceTime + model._recognisedVisualSpatialFieldObjectLifespan
          expected_visual_spatial_field_data[4][3][0][4] = fixation._performanceTime + model._unrecognisedVisualSpatialFieldObjectLifespan
          expected_visual_spatial_field_data[0][2][0][4] = fixation._performanceTime + model._recognisedVisualSpatialFieldObjectLifespan
          expected_visual_spatial_field_data[3][1][0][4] = fixation._performanceTime + model._unrecognisedVisualSpatialFieldObjectLifespan
        end
      end
      
      ######################################
      ##### INVOKE FUNCTION FIRST TIME #####
      ######################################
      
      list_pattern_returned = nil
      exception_thrown = false
      begin
      list_pattern_returned = perceiver.addFixation(fixation)
      rescue
        exception_thrown = true
      end
        
      #################
      ##### TESTS #####
      #################
      
      # Need to check the "_fixations" variable of the Perceiver instance during
      # testing.  Since this is final as well as private, can't use 
      # Perceiver.class_eval to access the variable; needs to be made accessible
      # "manually".
      fixation_field = Perceiver.java_class.declared_field("_fixations")
      fixation_field.accessible = true
      
      assert_equal(
        expected_list_pattern_returned,
        list_pattern_returned,
        "occurred when checking the ListPattern constructed in scenario " + scenario.to_s
      )

      assert_equal(
        expected_exception_thrown,
        exception_thrown,
        "occurred when checking if an exception is thrown in scenario " + scenario.to_s
      )

      assert_equal(
        expected_fixations,
        fixation_field.value(perceiver).lastEntry().getValue(),
        "occurred when checking Fixations data structure in scenario " + scenario.to_s
      )

      assert_equal(
        expected_cognition_clock,
        model._cognitionClock,
        "occurred when checking cognition clock in scenario " + scenario.to_s
      )

      assert_equal(
        expected_attention_clock,
        model._attentionClock,
        "occurred when checking attention clock in scenario " + scenario.to_s
      )
      
      if expected_visual_spatial_field_data != nil
        check_visual_spatial_field_against_expected(
          visual_spatial_field, 
          expected_visual_spatial_field_data, 
          fixation != nil && fixation._performanceTime != nil ? fixation._performanceTime + 100 : time, 
          "in scenario " + scenario.to_s
        )
      end
      
      #######################################
      ##### INVOKE FUNCTION SECOND TIME #####
      #######################################

      # In scenarios 9/12/21/24, the ability of the function to fill the slots 
      # of the visual STM hypothesis needs to be tested.  The first call to 
      # "addFixation()" should have triggered the updating of STM with the 
      # template node constructed earlier (since this should have been 
      # recognised given the fixation added).  In this case, the second 
      # Fixation should be just like the first so that the slot values 
      # filled can be accurately determined but its performance time should 
      # be different, i.e. after the template node has been placed in STM.
      # Therefore, the second fixation performance time is set to the clock with
      # the maximum value: cognition/attention (also ensures that a second bout
      # of learning will occur as well as slot filling).
      if [9,12,21,24].include?(scenario)
        fixation._performanceTime = ([model.getAttentionClock(), model.getCognitionClock()].max).to_java(:int)
        
        # Invoke function
        list_pattern_returned = nil
        exception_thrown = false
        begin
        list_pattern_returned = perceiver.addFixation(fixation)
        rescue
          exception_thrown = true
        end
        
        # Need access to the private, final STM variable that contains the items
        # in STM. Can't use class_eval construct to access final variables so
        # access needs to be granted "manually".
        visual_stm = model.getStm(Modality::VISUAL)
        item_history_field = visual_stm.java_class.declared_field("_itemHistory")
        item_history_field.accessible = true

        # Get the visual STM hypothesis and its filled item and position slot 
        # histories when the second Fixation is added (its slot values should be
        # filled at this time).
        visual_stm_hypothesis = item_history_field.value(visual_stm).floorEntry(fixation._performanceTime.to_java(:int)).getValue().get(0)
        visual_stm_hypothesis_filled_item_slots = visual_stm_hypothesis._filledItemSlotsHistory.floorEntry(fixation._performanceTime.to_java(:int)).getValue()
        visual_stm_hypothesis_filled_position_slots = visual_stm_hypothesis._filledPositionSlotsHistory.floorEntry(fixation._performanceTime.to_java(:int)).getValue()
        visual_stm_hypothesis_filled_slots = visual_stm_hypothesis_filled_item_slots + visual_stm_hypothesis_filled_position_slots
        visual_stm_hypothesis_filled_slots = visual_stm_hypothesis_filled_slots.to_a

        expected_visual_stm_hypothesis_filled_slots = [
          ItemSquarePattern.new(
            "P", 
            ([12,24].include?(scenario) ? 0 : 3), 
            ([12,24].include?(scenario) ? 2 : 4)
          ),
          ItemSquarePattern.new(
            "K", 
            ([12,24].include?(scenario) ? 2 : 5), 
            ([12,24].include?(scenario) ? 2 : 4)
          )
        ]
        
        # Update the expected_visual_spatial_field_data structure (if 
        # applicable)
        if scenario > 12
          expected_visual_spatial_field_data[2][3][0][4] = fixation._performanceTime + model._recognisedVisualSpatialFieldObjectLifespan
          expected_visual_spatial_field_data[4][3][0][4] = fixation._performanceTime + model._unrecognisedVisualSpatialFieldObjectLifespan
          expected_visual_spatial_field_data[0][2][0][4] = fixation._performanceTime + model._recognisedVisualSpatialFieldObjectLifespan
          expected_visual_spatial_field_data[3][1][0][4] = fixation._performanceTime + model._unrecognisedVisualSpatialFieldObjectLifespan
        end
        
        #################
        ##### TESTS #####
        #################

        assert_equal(
          expected_visual_stm_hypothesis_filled_slots.size(),
          visual_stm_hypothesis_filled_slots.size(),
          "occurred when checking the number of slots filled after addFixation " +
          "is called for the second time in scenario " + scenario.to_s
        )

        # Convert contents of expected and actual slot values to strings to allow
        # for accurate equality checks (this also negates any errors that may 
        # occur due to incorrect ordering of expected and actual slot value 
        # contents).
        expected_visual_stm_hypothesis_filled_slots.map! {|x|x.toString()}
        visual_stm_hypothesis_filled_slots.map! {|x|x.toString()}
        for expected_slot_value in expected_visual_stm_hypothesis_filled_slots
          assert_true(
            visual_stm_hypothesis_filled_slots.include?(expected_slot_value),
            "occurred when checking if " + expected_slot_value + " is included " +
            "in the slots filled in the template node after addFixation is " +
            "called for the second time in scenario " + scenario.to_s + "(slots " +
            "filled: " + visual_stm_hypothesis_filled_slots.to_s + ")"
          )
        end
        
        # Perform remaining tests.
        assert_equal(
          expected_list_pattern_returned,
          list_pattern_returned,
          "occurred when checking the ListPattern constructed after addFixation " +
          "is called for the second time in scenario " + scenario.to_s
        )

        assert_equal(
          false,
          exception_thrown,
          "occurred when checking if an exception is thrown after addFixation " +
          "is called for the second time in scenario " + scenario.to_s 
        )

        # Another Fixation will have been added to the Perceiver's "Fixations
        # attempted" data structure.
        expected_fixations.push(fixation)
        assert_equal(
          expected_fixations,
          fixation_field.value(perceiver).lastEntry().getValue(),
          "occurred when checking Fixations data structure after addFixation " +
          "is called for the second time in scenario " + scenario.to_s
        )

        # This time, familiarisation should occur since the second item in the
        # ListPattern generated has been learned as a primitive during the first
        # invocation but isn't present in the template Node's image (this is the
        # Node retrieved when recogniseAndLearn is invoked in the second 
        # addFixation invocation).
        expected_cognition_clock = (fixation._performanceTime + (model.getLtmLinkTraversalTime() * 2) + model.getFamiliarisationTime())
        assert_equal(
          expected_cognition_clock,
          model._cognitionClock,
          "occurred when checking cognition clock after addFixation " +
          "is called for the second time in scenario " + scenario.to_s
        )

        expected_attention_clock = (fixation._performanceTime + (model.getLtmLinkTraversalTime() * 2) + model.getTimeToUpdateStm())
        assert_equal(
          expected_attention_clock,
          model._attentionClock,
          "occurred when checking attention clock after addFixation " +
          "is called for the second time in scenario " + scenario.to_s
        )
        
        if expected_visual_spatial_field_data != nil
          check_visual_spatial_field_against_expected(
            visual_spatial_field, 
            expected_visual_spatial_field_data, 
            fixation != nil && fixation._performanceTime != nil ? fixation._performanceTime + 100 : time, 
            "after addFixation is called for the second time in scenario " + scenario.to_s
          )
        end
      end
    end
  end
end

################################################################################
# Checks for correct operation of the "clearFixations()" function by adding 
# Fixations to a Perceiver object's "_fixations" data structure, setting its 
# "_fixationToLearnFrom" variable to a value > 0 and calling "clearFixations()"
# afterwards.
#
# At the time "clearFixations()" is invoked, the "_fixations" data structure 
# should be empty and the "_fixationToLearnFrom" variable should be reset to 0.
unit_test "clear_fixations" do
  
  ##################
  ##### SET-UP #####
  ##################
  
  # Need access to the private, non-final "_fixationToLearnFrom" Perceiver 
  # instance variable.
  Perceiver.class_eval{
    field_accessor :_fixationToLearnFrom
  }
 
  100.times do
    # Construct and get Perceiver instance.
    time = 0
    model = Chrest.new(time, false)
    perceiver = model.getPerceiver()

    # Need access to the private, final "_fixations" Perceiver instance variable.
    # Since its private, its accessible proprty needs to be manually set rather 
    # than using Perceiver.class_eval.
    fixations_field = perceiver.java_class.declared_field("_fixations")
    fixations_field.accessible = true
    perceiver_fixations = fixations_field.value(perceiver)

    # Create an ArrayList of 10 Fixations and put this in the Perceiver's 
    # Fixation data structure and set the "_fixationToLearnFrom" instance variable
    # to 10.
    fixations = ArrayList.new()
    10.times do fixations.add(CentralFixation.new(time, 10)) end
    perceiver_fixations.put(time.to_java(:int), fixations)
    perceiver._fixationToLearnFrom = 10

    # Invoke "clearFixations" after Fixations have been put in the Perceiver's 
    # Fixation data structure so that there's a change in the state of this data 
    # structure that can be analysed.
    perceiver.clearFixations(time += 10)

    #################
    ##### TESTS #####
    #################

    # Check that putting data in the Perceiver's "_fixations" data structure
    # occurred.
    assert_equal(
      10, 
      perceiver_fixations.floorEntry( (time - 1).to_java(:int) ).getValue.size(),
      "occurred when checking the number of fixations in the Perceiver's " + 
      "'_fixations' data structure before clearFixations() was called"
    )

    # Check that the expected variables have been reset.
    assert_equal(
      0, 
      perceiver_fixations.floorEntry(time.to_java(:int)).getValue.size(),
      "occurred when checking the number of fixations in the Perceiver's " + 
      "'_fixations' data structure when clearFixations() was called"
    )
    
    assert_equal(
      0, 
      perceiver._fixationToLearnFrom,
      "occurred when checking the '_fixationToLearnFrom' Perceiver instance " +
      "variable after clearFixations() was called"
    )
  end
end

################################################################################
unit_test "get_fixations" do
  
  100.times do
    
    ##################
    ##### SET-UP #####
    ##################
    time = 0
    model = Chrest.new(time, false)
    perceiver = model.getPerceiver()

    # Need access to the private, final "_fixations" Perceiver instance variable.
    # Since its private, its accessible proprty needs to be manually set rather 
    # than using Perceiver.class_eval.
    fixations_field = perceiver.java_class.declared_field("_fixations")
    fixations_field.accessible = true
    
    # Put 10 entries into in the Perceiver's Fixation data structure, 1 every 
    # 10 milliseconds.  Each entry should add 1 more Fixation to the previous 
    # entry in the data structure.  This facilitates checking of the 
    # "getFixations()" function since, every 10ms, there should be a change in
    # the number of Fixations returned.
    time_increment = 10
    number_fixations_to_add = 10
    number_fixations_to_add.times do
      current_fixations = fixations_field.value(perceiver).floorEntry(time.to_java(:int)).getValue()

      new_fixations = ArrayList.new()
      new_fixations.addAll(current_fixations)
      new_fixations.add(CentralFixation.new(time, time_increment))
      time += time_increment
      
      fixations_field.value(perceiver).put(time.to_java(:int), new_fixations)
    end

    ###################
    ##### TESTING #####
    ###################

    # Run getFixations for every time increment (10ms) starting from before the
    # model was created and check the number of Fixations returned.  Since the
    # Fixations are all the same type, forego checking this.
    for ms in (model.getCreationTime() - time_increment)..time

      if ms % time_increment == 0
        fixations = perceiver.getFixations(ms)

        if ms < 0
          assert_equal(
            nil, 
            fixations, 
            "occurred when getting Fixations from the Perceiver at a time " + 
            "before the Perceiver was created"
          )
        else 
          assert_equal(
            ms/time_increment,
            fixations.size(),
            "occurred when getting Fixations from the Perceiever at a time " + 
            "when/after the Perceiver was created (" + ms.to_s + "ms)"
          )
        end

      end
    end
  end
end

################################################################################
unit_test "get_fixations_performed" do
  100.times do
    
    ##################
    ##### SET-UP #####
    ##################
    
    # Need to be able to set the protected "_performed" Fixation instance 
    # variable "on the fly".
    Fixation.class_eval{
      field_accessor :_performed
    }
    
    time = 0
    model = Chrest.new(time, false)
    perceiver = model.getPerceiver()

    # Need access to the private, final "_fixations" Perceiver instance variable.
    # Since its private, its accessible proprty needs to be manually set rather 
    # than using Perceiver.class_eval.
    fixations_field = perceiver.java_class.declared_field("_fixations")
    fixations_field.accessible = true
    
    # Put 10 entries into in the Perceiver's Fixation data structure, 1 every 
    # 10 milliseconds.  Each entry should add 1 more Fixation to the previous 
    # entry in the data structure and every second Fixation added should be 
    # performed.
    time_increment = 10
    number_fixations_to_add = 10
    for fixation in 1..number_fixations_to_add
      current_fixations = fixations_field.value(perceiver).floorEntry(time.to_java(:int)).getValue()

      new_fixations = ArrayList.new()
      new_fixations.addAll(current_fixations)
      
      fixation_to_add = CentralFixation.new(time, time_increment)
      time += time_increment
      if fixation % 2 == 0 then fixation_to_add._performed = true end
      new_fixations.add(fixation_to_add)

      fixations_field.value(perceiver).put(time.to_java(:int), new_fixations)
    end

    ###################
    ##### TESTING #####
    ###################

    # Run getFixationsPerformed for every time increment (10ms) starting from
    # before the model was created and check the number of Fixations returned.  
    # Since the Fixations are all the same type, forego checking this.
    for ms in (model.getCreationTime() - time_increment)..time

      if ms % time_increment == 0
        fixations_performed = perceiver.getFixationsPerformed(ms)
        if fixations_performed != nil then fixations_performed = fixations_performed.size() end

        assert_equal(
          (ms < 0 ? nil : ms/20),
          fixations_performed,
          "occurred when getting Fixations from the Perceiever at a time " + 
          "when/after the Perceiever was created (" + ms.to_s + "ms)"
        )
      end
    end
  end
end

################################################################################
unit_test "get_most_recent_fixation_performed" do
  100.times do
    
    ##################
    ##### SET-UP #####
    ##################
    
    # Need to be able to set the protected "_performed" Fixation instance 
    # variable "on the fly".
    Fixation.class_eval{
      field_accessor :_performed
    }
    
    time = 0
    model = Chrest.new(time, false)
    perceiver = model.getPerceiver()

    # Need access to the private, final "_fixations" Perceiver instance variable.
    # Since its private, its accessible proprty needs to be manually set rather 
    # than using Perceiver.class_eval.
    fixations_field = perceiver.java_class.declared_field("_fixations")
    fixations_field.accessible = true
    
    # Put 10 entries into in the Perceiver's Fixation data structure, 1 every 
    # 10 milliseconds.  Each entry should add 1 more Fixation to the previous 
    # entry in the data structure and every second Fixation added should be 
    # performed.  Record the unique reference of each performed Fixation so they
    # can be used to check if the correct Fixation is returned when the
    # "getMostRecentFixationPerformed" function is invoked.
    time_increment = 10
    number_fixations_to_add = 10
    fixations_performed_references = []
    for fixation in 1..number_fixations_to_add
      current_fixations = fixations_field.value(perceiver).floorEntry(time.to_java(:int)).getValue()

      new_fixations = ArrayList.new()
      new_fixations.addAll(current_fixations)
      
      fixation_to_add = CentralFixation.new(time, time_increment)
      time += time_increment
      if fixation % 2 == 0 
        fixation_to_add._performed = true
        fixations_performed_references.push(fixation_to_add.getReference())
      end
      new_fixations.add(fixation_to_add)

      fixations_field.value(perceiver).put(time.to_java(:int), new_fixations)
    end

    ###################
    ##### TESTING #####
    ###################

    # Run getFixationsPerformed for every time increment (10ms) starting from
    # before the model was created and check the number of Fixations returned.  
    # Since the Fixations are all the same type, forego checking this.
    for ms in (model.getCreationTime() - time_increment)..time
      most_recent_fixation_performed_reference = perceiver.getMostRecentFixationPerformed(ms)
      if most_recent_fixation_performed_reference != nil then most_recent_fixation_performed_reference = most_recent_fixation_performed_reference.getReference() end
      
      expected_reference_most_recent_fixation_performed = 
        (ms < 20 ? 
          nil : 
          fixations_performed_references[(ms/20) - 1]
        )
      
        assert_equal(
          expected_reference_most_recent_fixation_performed,
          most_recent_fixation_performed_reference,
          "occurred when getting most recent Fixation performed at time " + 
          ms.to_s + "ms"
        )
    end
  end
end

################################################################################
# This test ensures that the "learnFromNewFixations" function operates as 
# expected.  To do this, 18 scenarios are run and repeated 100 times each to
# ensure consistency of function behaviour.  For the first 12 scenarios, the
# function is invoked once however, for scenarios 13 -> 16 and 18 it is invoked
# twice, in scenario 17, it is invoked three times.
# 
# To test the function, a Perceiver object's Fixations attempted data structure 
# is set manually to enable prediction of results.  The CHREST model associated
# with the Perceiver has its domain-specifics set to the GenericDomain, 
# important since the "normalise" method of a CHREST model's domain-specifics is
# used when preparing the ListPattern to learn.  In the case of the 
# GenericDomain, any empty and blind squares fixated on are removed (important
# with respect to understanding what the expected ListPattern learned is in
# scenario 18).
# 
# Two sets of Fixations are made and the first set is usually present in the 
# Perceiver's Fixation data structure when the "learnFromNewFixations" function 
# is first invoked (except in scenarios 1 and 2).  The second set of Fixations
# is added to the Perceiver's Fixation data structure after the first and will
# not be returned until the second function invocation is made.  Setting 
# different sets of Fixations to different times in the Perceiver's Fixation 
# data structure enables the test to check if "_fixationToLearnFrom" 
# Perceiever index variable considerations in the function operate correctly.
# 
# Each Fixation used is made on the same Scene in all but 1 scenario where the
# Scene is set to nil (scenario 6).  The Scene always contains the location of
# the agent that created the Scene and made the Fixation so that agent-relative
# object locations can be calculated if the CHREST model is set to do this in a
# scenario. The objects fixated on are located in each cardinal compass 
# direction around the agent's avatar in the Scene so that negative and positive 
# agent-relative coordinates need to be calculated.  The Scene itself is 13 * 13 
# Squares to give enough space to make enough distinct Fixations, a graphical 
# version of the Scene is displayed below for ease of reference.  Numbers on the 
# axis are domain-specific coordinates (note that the Scene and domain-specific 
# coordinates are not equal, Scene specific coordinates are zero-indexed). This 
# facilitates checking of correct coordinate calculation when constructing the 
# ListPattern to learn if the CHREST model is to learn object locations relative 
# to the agent's location).  Numbers in the grid refer to the order of object 
# placement and are, in most scenarios, the IDs of objects.
# 
#    |----|----|----|----|----|----|----|----|----|----|----|----|----|
# 13 |    |    |    |    |    |    |    |    |    |    |    |    |    |
#    |----|----|----|----|----|----|----|----|----|----|----|----|----|
# 12 |    |    |    |    |    |    |    |    |    |    | 10 |    |    |
#    |----|----|----|----|----|----|----|----|----|----|----|----|----|
# 11 |    |    |    |    |    |    |    |    |    | 9  |    | 20 |    |
#    |----|----|----|----|----|----|----|----|----|----|----|----|----|
# 10 |    |    |    |    |    |    |    |    | 8  |    | 19 |    |    |
#    |----|----|----|----|----|----|----|----|----|----|----|----|----|
# 9  |    |    |    |    |    |    |    | 7  |    | 18 |    |    |    |
#    |----|----|----|----|----|----|----|----|----|----|----|----|----|
# 8  |    |    |    |    |    |    | 6  |    | 17 |    |    |    |    |
#    |----|----|----|----|----|----|----|----|----|----|----|----|----|
# 7  |    |    |    |    |    | 5  |SELF| 16 |    |    |    |    |    |
#    |----|----|----|----|----|----|----|----|----|----|----|----|----|
# 6  |    |    |    |    | 4  |    | 15 |    |    |    |    |    |    |
#    |----|----|----|----|----|----|----|----|----|----|----|----|----|
# 5  |    |    |    | 3  |    | 14 |    |    |    |    |    |    |    |
#    |----|----|----|----|----|----|----|----|----|----|----|----|----|
# 4  |    |    | 2  |    | 13 |    |    |    |    |    |    |    |    |
#    |----|----|----|----|----|----|----|----|----|----|----|----|----|
# 3  |    | 1  |    | 12 |    |    |    |    |    |    |    |    |    |
#    |----|----|----|----|----|----|----|----|----|----|----|----|----|
# 2  |    |    | 11 |    |    |    |    |    |    |    |    |    |    |
#    |----|----|----|----|----|----|----|----|----|----|----|----|----|
# 1  |    |    |    |    |    |    |    |    |    |    |    |    |    |
#    |----|----|----|----|----|----|----|----|----|----|----|----|----|
#      1    2    3    4    5    6    7    8    9    10   11   12   13
# 
# Scenario Descriptions
# =====================
# 
# Scenario 1: 
#   - Function invoked before perceiver created
#   
# Scenario 2: 
#   - Function invoked after perceiver created but before any Fixations 
#     attempted
#     
# The following scenarios invoke the function when the last Fixation in the 
# first set have been added to the Perceiver's data structure.  All Fixations
# referred to in the following sceario descriptions pertain to Fixations in the
# first set.
# 
# Scenario 3: 
#   - "_objectSeen" variable for all Fixations attempted set to null
# 
# Scenario 4: 
#   - "_colFixatedOn" variable for all Fixations attempted set to null
#   
# Scenario 5: 
#   - "_rowFixatedOn" variable for all Fixations attempted set to null
#   
# Scenario 6:
#   - "_scene" variable for all Fixations attempted set to null
#   
# Scenario 7: 
#   - Fixations made on creator object
# 
# Scenario 8: 
#   - Fixations made on duplicate, non blind/empty square SceneObjects (same ID) 
#     that have distinct locations
#   
# Scenario 9: 
#   - Fixations made on distinct, non blind/empty square SceneObjects (different 
#     IDs) that have duplicate locations
#
# Scenario 10: 
#   - Fixations made on blind square SceneObjects (same ID) that have duplicate 
#     locations
#     
# Scenario 11: 
#   - Fixations made on empty square SceneObjects (same ID) that have duplicate 
#     locations
# 
# In the following scenarios, Fixation variables tested in scenarios 3 -> 6 are 
# set correctly, no Fixation is made on the creator and all SceneObjects 
# fixated on are non blind/empty Squares in distinct locations. 
# 
# Scenario 12: 
#   -  No Fixation has been performed successfully.
# 
# Two sets of Fixations used in the following scenarios.  As in scenario 12, 
# Fixation variables tested in scenarios 3 -> 6 are set correctly, no Fixation 
# is made on the creator and all SceneObjects fixated on are non blind/empty 
# Squares in distinct locations. Furthermore, every second Fixation in each set 
# is performed successfully (can now test if the function discards unsuccessful
# Fixations correctly) and the function is invoked twice: first when only the 
# first set of Fixations are present in the Perceiver's data structure, second
# when the first and second set of Fixations are present in the Perceiver's data 
# structure.
# 
# Scenario 13: 
#   -Agent-relative object coordinate learning disabled.
#              
# Scenario 14: 
#   - Agent-relative object coordinate learning enabled.
#              
# Scenario 15: 
#   - As Scenario 13 but the Perceiver's "_fixationToLearnFrom" variable should 
#     be set to 2 before function invoked for the first time (first two 
#     Fixations performed "missed").
#              
# Scenario 16: 
#   - As Scenario 14 but the Perceiver's "_fixationToLearnFrom" variable should 
#     be set to 2 before function invoked for the first time (first two 
#     Fixations performed "missed").
#              
# Scenario 17: 
#   - As scenario 13 but function called for a third time without adding new 
#     Fixations
#              
# Scenario 18: As scenario 13, but objects fixated on are blind/empty
#              
# Expected Output
# ===============
# 
# For each Scenario the test will check:
# - If an exception is thrown by the function
# - The ListPattern generated and learned
# - The Perceiver's "_fixationToLearnFrom" variable after function invocation
# 
# Expected output:
# 
# - Exception thrown
#   ~ In scenarios 3 -> 11
#
# - ListPattern generated
#   ~ Null in scenarios 1, 2 and 17
#   ~ Empty in scenario 12
#   ~ Non-empty in scenarios 13 -> 16 and also 18 but this is due to the 
#     operation of the GenericDomain's "normalise" method (all blind and empty
#     squares will be removed from the ListPattern generated).
#
# - "_fixationToLearnFrom" variable
#   ~ 0 in scenarios 1, 2 and 17
#   ~ In scenarios 12 -> 16 and 18, after each function invocation it should 
#     equal the result of invoking "size" on the Fixation set used in that 
#     invocation.
unit_test "learn_from_new_fixatons" do
  
  # Need to be able to set the private  "_domainSpecifics" Chrest object 
  # variable without relying on methods.
  Chrest.class_eval{
    field_accessor :_domainSpecifics
  }
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  # Need access to the private, non-final "_fixationToLearnFrom" Perceiver 
  # instance variable so it can be examined during testing.
  Perceiver.class_eval{
    field_accessor :_fixationToLearnFrom
  }
  
  # Need access to various private fields in Fixations so they can be set 
  # easily for testing and without recourse to using Fixation object methods.
  Fixation.class_eval{
    field_accessor :_timeDecidedUpon, :_performanceTime, :_performed, :_scene, :_colFixatedOn, :_rowFixatedOn, :_objectSeen
  }
  
  #########################
  ##### SCENARIO LOOP #####
  #########################
  
  for scenario in 1..18
    100.times do
    
      time = 0
      model = Chrest.new(time, ([14,16].include?(scenario) ? true : false))
      model._domainSpecifics = GenericDomain.new(model, nil, 3)
      perceiver = model.getPerceiver()

      # Need access to the private, final "_fixations" Fixation instance variable
      # for test set-up.  Since this field is private, using a field_accessor in
      # a jRuby "class_eval" construct will not allow access to it.
      fixations_field = perceiver.java_class.declared_field("_fixations")
      fixations_field.accessible = true

      ###########################
      ##### CONSTRUCT SCENE #####
      ###########################

      scene_to_fixate_on = Scene.new("test",13,13,1,1,nil)

      # Add the creator to the Scene.  
      scene_to_fixate_on._scene.get(6).set(6, SceneObject.new("0", Scene.getCreatorToken()))

      #########################
      ##### SET FIXATIONS #####
      #########################

      # The object ID variable will be used across construction of both fixation 
      # sets to ensure that, if required, no duplicate items will be fixated on.
      object_id = 0

      ##### FIXATION SET 1 #####
      first_invocation_fixation_set = ArrayList.new()

      # The two variables below will be used to set-up the Squares fixated on for
      # each Fixation in the first set.  Both variables start at 1 below the 
      # actual value used in the Fixations so that they can be set using ternary
      # "if" statements (more elegant code).  So the first Fixation will actually
      # be made on Square with col = 1, row = 2.
      col_fixated_on = 0
      row_fixated_on = 1

      # Add 10 Fixations to the first set.
      for fixation in 1..10
        f = CentralFixation.new(time, 10)
        time += 10
        f._performanceTime = (f.getTimeDecidedUpon + 50)

        f._performed = 
          (scenario == 12 ? 
            false : 
            (fixation % 2 == 0 ? 
              true : 
              false
            )
          )

        f._objectSeen = 
          (scenario == 3 ? 
            nil : 
            SceneObject.new(
              (scenario == 8 ? 
                  object_id.to_s : 
                  (object_id += 1).to_s 
              ),
              (scenario == 7 ?
                Scene.getCreatorToken() :
                (scenario == 10 ?
                  Scene.getBlindSquareToken() :
                  (scenario == 11 ?
                    Scene.getEmptySquareToken() :
                    (scenario == 18 ?
                      [Scene.getBlindSquareToken(), Scene.getEmptySquareToken()].sample :
                      "A_class"
                    )
                  )
                )
              )
            )
          )

        f._colFixatedOn = 
          (scenario == 4 ? 
            nil : 
            (scenario.between?(9,11) ?
              col_fixated_on :
              (col_fixated_on += 1) 
            )
          )
        f._rowFixatedOn = 
          (scenario == 5 ? 
            nil : 
            (scenario.between?(9,11) ?
              row_fixated_on :
              (row_fixated_on += 1) 
            )
          )

        f._scene = (scenario == 6 ? nil : scene_to_fixate_on)

        first_invocation_fixation_set.add(f)
        prev_fixations_in_perceiver = fixations_field.value(perceiver).floorEntry(f.getPerformanceTime().to_java(:int)).getValue()
        new_fixations = ArrayList.new()
        new_fixations.addAll(prev_fixations_in_perceiver)
        new_fixations.add(f)
        fixations_field.value(perceiver).put(f.getPerformanceTime().to_java(:int), new_fixations)
        time = f.getPerformanceTime()
      end

      ##### FIXATION SET 2 #####
      second_invocation_fixation_set = ArrayList.new()

      #The two variables below will be used to set-up the Squares fixated on for
      # each Fixation in the second set.  Both variables start at 1 below the 
      # actual value used in the Fixations so that they can be set using ternary
      # "if" statements (more elegant code).  So the first Fixation will actually
      # be made on Square with col = 2, row = 1.
      col_fixated_on = 1
      row_fixated_on = 0

      # Add 10 Fixations to the second set.
      for fixation in 1..10
        f = CentralFixation.new(time, 10)
        time += 10
        f._performanceTime = (f.getTimeDecidedUpon + 50)

        f._performed = 
          (scenario == 12 ? 
            false : 
            (fixation % 2 == 0 ? 
              true : 
              false
            )
          )

        f._objectSeen = SceneObject.new( (object_id += 1).to_s, "A_class")
        f._colFixatedOn = (col_fixated_on += 1) 
        f._rowFixatedOn = (row_fixated_on += 1)
        f._scene = scene_to_fixate_on

        second_invocation_fixation_set.add(f)
        prev_fixations_in_perceiver = fixations_field.value(perceiver).floorEntry(f.getPerformanceTime().to_java(:int)).getValue()
        new_fixations = ArrayList.new()
        new_fixations.addAll(prev_fixations_in_perceiver)
        new_fixations.add(f)
        fixations_field.value(perceiver).put(f.getPerformanceTime().to_java(:int), new_fixations)
        time = f.getPerformanceTime()
      end

      #########################################
      ##### SET FUNCTION INVOCATION TIMES #####
      #########################################

      first_invocation_time = 
      (scenario == 1 ?
        model.getCreationTime() - 5 :
        (scenario == 2 ? 
          first_invocation_fixation_set[0].getPerformanceTime() - 1 :
          first_invocation_fixation_set.get(first_invocation_fixation_set.size() - 1).getPerformanceTime()
        )
      )

      second_invocation_time = second_invocation_fixation_set.get(second_invocation_fixation_set.size() - 1).getPerformanceTime()

      ###############################################
      ##### SET "_fixationToLearnFrom" VARIABLE #####
      ###############################################

      perceiver._fixationToLearnFrom = ([15,16].include?(scenario) ? 2 : 0 )

      #################
      ##### TESTS #####
      #################

      ##### FIRST INVOCATION #####

      # Set expected ListPattern to be returned.
      expected_list_pattern_learned_first_invocation = (scenario < 12 ? nil : ListPattern.new(Modality::VISUAL))
      if scenario.between?(13,17) 
        start_fixation = 0
        if [15,16].include?(scenario) then start_fixation = 2 end
        for i in start_fixation...first_invocation_fixation_set.size()
          if (i + 1) % 2 == 0
            fixation = first_invocation_fixation_set[i]
            expected_list_pattern_learned_first_invocation.add(
              ItemSquarePattern.new(
                fixation._objectSeen.getObjectType(),
                ([14,16].include?(scenario) ? fixation._colFixatedOn - 6 : fixation._colFixatedOn),
                ([14,16].include?(scenario) ? fixation._rowFixatedOn - 6 : fixation._rowFixatedOn)
              )
            )
          end
        end
      end

      # Set expected _learnFromFixations value.
      expected_fixation_to_learn_from_index_first_invocation = 
        (scenario >= 12 ? 
          (first_invocation_fixation_set.size()) :
          0
        )

      exception_thrown_first_invocation = false
      list_pattern_learned = nil
      begin
        list_pattern_learned = perceiver.learnFromNewFixations(first_invocation_time)
      rescue
        exception_thrown_first_invocation = true
      end

      # Check if exception is thrown
      assert_equal(
        (scenario.between?(3,11) ? true : false),
        exception_thrown_first_invocation,
        "occurred when checking if an exception is thrown during first invocation " +
        "of function in scenario " + scenario.to_s
      )

      # Check ListPattern returned
      assert_equal(
        expected_list_pattern_learned_first_invocation,
        list_pattern_learned,
        "occurred when checking the ListPattern learned after first invocation " +
        "of function in scenario " + scenario.to_s
      )

      # Check _fixationToLearnFrom variable.
      assert_equal(
        expected_fixation_to_learn_from_index_first_invocation,
        perceiver._fixationToLearnFrom,
        "occurred when checking the '_fixationToLearnFrom' Perceiver variable " +
        "after first invocation of function in scenario " + scenario.to_s
      )

      ##### SECOND INVOCATION #####

      #Do second invocation if scenario is not 1-12
      if(!scenario.between?(1,12))

        expected_list_pattern_learned_second_invocation = ListPattern.new(Modality::VISUAL)
        if scenario.between?(13,18)
          for i in 0...second_invocation_fixation_set.size()
            if (i + 1) % 2 == 0
              fixation = second_invocation_fixation_set[i]
              expected_list_pattern_learned_second_invocation.add(
                ItemSquarePattern.new(
                  fixation._objectSeen.getObjectType(),
                  ([14,16].include?(scenario) ? fixation._colFixatedOn - 6 : fixation._colFixatedOn),
                  ([14,16].include?(scenario) ? fixation._rowFixatedOn - 6 : fixation._rowFixatedOn)
                )
              )
            end
          end
        end

        expected_fixation_to_learn_from_index_second_invocation = (fixations_field.value(perceiver).lastEntry().getValue().size)
        exception_thrown_second_invocation = false

        begin
          list_pattern_learned = perceiver.learnFromNewFixations(second_invocation_time)
        rescue
          exception_thrown_second_invocation = true
        end

        assert_equal(
          exception_thrown_second_invocation,
          false,
          "occurred when checking if an exception is thrown during second invocation " +
          "of function in scenario " + scenario.to_s
        )

        # Check ListPattern returned
        assert_equal(
          expected_list_pattern_learned_second_invocation,
          list_pattern_learned,
          "occurred when checking the ListPattern learned after second invocation " +
          "of function in scenario " + scenario.to_s
        )

        # Check _fixationToLearnFrom variable.
        assert_equal(
          expected_fixation_to_learn_from_index_second_invocation,
          perceiver._fixationToLearnFrom,
          "occurred when checking the '_fixationToLearnFrom' Perceiver variable " +
          "after second invocation of function in scenario " + scenario.to_s
        )

        ##### THIRD INVOCATION #####
        if scenario == 17
          exception_thrown_third_invocation = false

          begin
            list_pattern_learned = perceiver.learnFromNewFixations(second_invocation_time + 100)
          rescue
            exception_thrown_third_invocation = true
          end

          assert_equal(
            exception_thrown_third_invocation,
            false,
            "occurred when checking if an exception is thrown during third invocation " +
            "of function in scenario " + scenario.to_s
          )

          # Check ListPattern returned
          assert_equal(
            nil,
            list_pattern_learned,
            "occurred when checking the ListPattern learned after third invocation " +
            "of function in scenario " + scenario.to_s
          )

          # Check _fixationToLearnFrom variable.
          assert_equal(
            expected_fixation_to_learn_from_index_second_invocation,
            perceiver._fixationToLearnFrom,
            "occurred when checking the '_fixationToLearnFrom' Perceiver variable " +
            "after third invocation of function in scenario " + scenario.to_s
          )
        end
      end
    end
  end
end

################################################################################
unit_test "get_fixation_to_learn_from" do
  
  # Need to set the private "_fixationToLearnFrom" Perceiver instance variable
  # manually.
  Perceiver.class_eval{
    field_accessor :_fixationToLearnFrom
  }
  
  # Construct Perceiver and set "_fixationToLearnFrom" variable
  model = Chrest.new(0, false)
  perceiver = model.getPerceiver()
  fixation_to_learn_from = 745
  perceiver._fixationToLearnFrom = fixation_to_learn_from
  
  # Test that what is expected is returned.
  assert_equal(
    fixation_to_learn_from,
    perceiver.getFixationToLearnFrom()
  )
end

################################################################################
unit_test "get_and_set_fixation_field_of_view" do
  100.times do
    # Need to set the private "_fixationToLearnFrom" Perceiver instance variable
    # manually.
    Perceiver.class_eval{
      field_accessor :_fixationFieldOfView
    }

    # Construct Perceiver and set "_fixationToLearnFrom" variable
    model = Chrest.new(0, false)
    perceiver = model.getPerceiver()

    # Create an array of integers from 1 to 100, remove the default 
    # "_fixationFieldOfView" parameter from the array and randomly select one of
    # these values to set as the new "_fixationFieldOfView" value.
    fixation_field_of_view = ([*1..100] - [perceiver._fixationFieldOfView]).sample

    # Check that the parameter is set correctly.
    perceiver.setFixationFieldOfView(fixation_field_of_view)
    assert_equal(
      fixation_field_of_view,
      perceiver._fixationFieldOfView,
      "occurred when checking if the parameter is set correctly"
    )

    # Check that the getter returns the expected value.
    assert_equal(
      fixation_field_of_view,
      perceiver.getFixationFieldOfView(),
      "occurred when checking if the parameter is returned correctly"
    )
  end
end