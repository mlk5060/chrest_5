################################################################################
# Tests the TileworldDomain constructor using a number of scenarios that focus 
# on various constructor parameter settings:
#
# - Scenario 1
#   ~ Chrest model specified as a parameter is not learning object locations
#     relative to the agent equipped with it.
#
# - Scenario 2
#   ~ Chrest model specified as a parameter is learning object locations 
#     relative to the agent equipped with it. 
#   ~ Initial fixation threshold specified as a parameter is less than 0
#
# - Scenario 3
#   ~ Chrest model specified as a parameter is learning object locations 
#     relative to the agent equipped with it. 
#   ~ Initial fixation threshold specified as a parameter is equal to 0
#
# - Scenario 4
#   ~ Chrest model specified as a parameter is learning object locations 
#     relative to the agent equipped with it. 
#   ~ Initial fixation threshold specified as a parameter is greater than the
#     maximum number of fixations specified.
#
# - Scenario 5
#   ~ Chrest model specified as a parameter is learning object locations 
#     relative to the agent equipped with it. 
#   ~ Initial fixation threshold specified as a parameter is less than the
#     maximum number of fixations specified.
#   ~ Maximum attempts to use a peripheral item fixation specified as a 
#     parameter is less than 0
#
# - Scenario 6
#   ~ Chrest model specified as a parameter is learning object locations 
#     relative to the agent equipped with it. 
#   ~ Initial fixation threshold specified as a parameter is less than the
#     maximum number of fixations specified.
#   ~ Maximum attempts to use a peripheral item fixation specified as a 
#     parameter is equal to 0
#     
# - Scenario 7
#   ~ Chrest model specified as a parameter is learning object locations 
#     relative to the agent equipped with it. 
#   ~ Initial fixation threshold specified as a parameter is less than the
#     maximum number of fixations specified.
#   ~ Maximum attempts to use a peripheral item fixation specified as a 
#     parameter is greater than 0 
#   ~ Time taken to decide upon movement fixations is less than 0
#   
# - Scenario 8
#   ~ Chrest model specified as a parameter is learning object locations 
#     relative to the agent equipped with it. 
#   ~ Initial fixation threshold specified as a parameter is less than the
#     maximum number of fixations specified.
#   ~ Maximum attempts to use a peripheral item fixation specified as a 
#     parameter is greater than 0 
#   ~ Time taken to decide upon movement fixations is greater than/equal to 0
#   ~ Time taken to decide upon salient object fixations is less than 0
#
#- Scenario 9
#   ~ Chrest model specified as a parameter is learning object locations 
#     relative to the agent equipped with it. 
#   ~ Initial fixation threshold specified as a parameter is less than the
#     maximum number of fixations specified.
#   ~ Maximum attempts to use a peripheral item fixation specified as a 
#     parameter is greater than 0 
#   ~ Time taken to decide upon movement fixations is greater than/equal to 0
#   ~ Time taken to decide upon salient object fixations is greater than/equal 
#     to 0
#
# Expected Output
# ===============
#
# In all scenarios except scenario 9, an exception should be thrown by the
# constructor due to invalid constructor parameters being provided.  In scenario
# 9 the relevant instance variables of the TileworldDomain instance constructed
# should be set as specified.
unit_test "constructor" do

  #########################################
  ##### SET-UP ACCESS TO CLASS FIELDS #####
  #########################################

  domain_specifics_associated_model_field = DomainSpecifics.java_class.declared_field("_associatedModel")
  domain_specifics_associated_model_field.accessible = true
  
  domain_specifics_max_fixations_in_set_field = DomainSpecifics.java_class.declared_field("_maxFixationsInSet")
  domain_specifics_max_fixations_in_set_field.accessible = true
  
  tileworld_domain_initial_fixation_threshold_field = TileworldDomain.java_class.declared_field("_initialFixationThreshold")
  tileworld_domain_initial_fixation_threshold_field.accessible = true

  tileworld_domain_peripheral_item_fixation_max_attempts_field = TileworldDomain.java_class.declared_field("_peripheralItemFixationMaxAttempts")
  tileworld_domain_peripheral_item_fixation_max_attempts_field.accessible = true
  
  TileworldDomain.class_eval{
    field_accessor :_timeTakenToDecideUponMovementFixations, :_timeTakenToDecideUponSalientObjectFixations
  }
  #########################
  ##### SCENARIO LOOP #####
  #########################
  for scenario in 1..9
    50.times do
     
      #########################################################
      ##### SET-UP TileworldDomain CONSTRUCTOR PARAMETERS #####
      #########################################################

      model = Chrest.new(0, (scenario == 1 ? false : true))
      max_fixations_in_set = 10
      initial_fixation_threshold = (scenario == 2 ? -1 : scenario == 3 ? 0 : scenario == 4 ? max_fixations_in_set + 1 : 3)
      max_peripheral_item_fixation_attempts =  (scenario == 5 ? -1 : scenario == 6 ?  0 : 3)
      time_taken_to_decide_on_movement_fixation = (scenario == 7 ? -1 : [0, 1].sample)
      time_taken_to_decide_on_salient_object_fixation = (scenario == 8 ? -1 : [0, 1].sample)
      
      ##############################
      ##### INVOKE CONSTRUCTOR #####
      ##############################
      
      exception_thrown = false
      tileworld_domain = nil
      begin
        tileworld_domain = TileworldDomain.new(
          model, 
          max_fixations_in_set, 
          initial_fixation_threshold, 
          max_peripheral_item_fixation_attempts,
          time_taken_to_decide_on_movement_fixation,
          time_taken_to_decide_on_salient_object_fixation
        )
      rescue
        exception_thrown = true
      end

      #################
      ##### TESTS #####
      #################
      
      # Test if exception was thrown as expected.
      expected_exception_thrown = (scenario == 9 ? false : true)
      assert_equal(
        expected_exception_thrown,
        exception_thrown,
        "occurred when checking if an exception is thrown in scenario " + scenario.to_s
      )

      # If an exception was not thrown check if instance variables set correctly 
      if !expected_exception_thrown
        assert_equal(
          domain_specifics_associated_model_field.value(tileworld_domain),
          model,
          "occurred when checking the CHREST model in scenario " + scenario.to_s
        )
        
        assert_equal(
          domain_specifics_max_fixations_in_set_field.value(tileworld_domain),
          max_fixations_in_set,
          "occurred when checking the maximum fixations in a set in scenario " + 
          scenario.to_s
        )
        
        assert_equal(
          tileworld_domain_initial_fixation_threshold_field.value(tileworld_domain),
          initial_fixation_threshold,
          "occurred when checking the initial fixations set in scenario " + 
          scenario.to_s
        )
        
        assert_equal(
          tileworld_domain_peripheral_item_fixation_max_attempts_field.value(tileworld_domain),
          max_peripheral_item_fixation_attempts,
          "occurred when checking the maximum number of peripheral item " +
          "fixation attempts set in a set in scenario " + scenario.to_s
        )
        
        assert_equal(
          tileworld_domain._timeTakenToDecideUponMovementFixations,
          time_taken_to_decide_on_movement_fixation,
          "occurred when checking the time taken to decide on a movement " +
          "fixation in scenario " + scenario.to_s
        )
        
        assert_equal(
          tileworld_domain._timeTakenToDecideUponSalientObjectFixations,
          time_taken_to_decide_on_salient_object_fixation,
          "occurred when checking the time taken to decide on a salient " +
          "object fixation in scenario " + scenario.to_s
        )
      end
    end
  end
end

################################################################################
unit_test "normalise" do
  ListPattern.class_eval{
    field_accessor :_list, :_modality, :_finished
  }
  
  for scenario in 1..2
    50.times do
      
      ##############################################
      ##### CONSTRUCT ListPattern TO NORMALISE #####
      ##############################################
      
      list_pattern = ListPattern.new(Modality::VISUAL)
      list_pattern._list.add(ItemSquarePattern.new(Scene.getBlindSquareToken(), 0, 0))
      list_pattern._list.add(ItemSquarePattern.new(Scene.getEmptySquareToken(), 1, 1))
      list_pattern._list.add(ItemSquarePattern.new(Scene.getCreatorToken(), 2, 2))
      list_pattern._list.add(ItemSquarePattern.new(TileworldDomain::HOLE_SCENE_OBJECT_TYPE_TOKEN, 3, 3))
      list_pattern._list.add(ItemSquarePattern.new(TileworldDomain::HOLE_SCENE_OBJECT_TYPE_TOKEN, 3, 3))
      list_pattern._list.add(ItemSquarePattern.new(TileworldDomain::TILE_SCENE_OBJECT_TYPE_TOKEN, 4, 4))
      list_pattern._list.add(ItemSquarePattern.new(TileworldDomain::TILE_SCENE_OBJECT_TYPE_TOKEN, 4, 4))
      list_pattern._list.add(ItemSquarePattern.new(TileworldDomain::OPPONENT_SCENE_OBJECT_TYPE_TOKEN, 5, 5))
      list_pattern._list.add(ItemSquarePattern.new(TileworldDomain::OPPONENT_SCENE_OBJECT_TYPE_TOKEN, 5, 5))
      list_pattern._finished = (scenario == 1 ? true : false)
      
      ##############################################################
      ##### CONSTRUCT EXPECTED ListPattern AFTER NORMALISATION #####
      ##############################################################
      
      expected_normalised_list_pattern = ListPattern.new(Modality::VISUAL)
      expected_normalised_list_pattern._list.add(ItemSquarePattern.new(TileworldDomain::HOLE_SCENE_OBJECT_TYPE_TOKEN, 3, 3))
      expected_normalised_list_pattern._list.add(ItemSquarePattern.new(TileworldDomain::TILE_SCENE_OBJECT_TYPE_TOKEN, 4, 4))
      expected_normalised_list_pattern._list.add(ItemSquarePattern.new(TileworldDomain::OPPONENT_SCENE_OBJECT_TYPE_TOKEN, 5, 5))
      expected_normalised_list_pattern._finished = (scenario == 1 ? true : false)
      
      ##############################
      ##### INVOKE "normalise" #####
      ##############################
      
      normalised_list_pattern = TileworldDomain.new(Chrest.new(0, true), 10, 3, 3, 0, 0).normalise(list_pattern)
      
      #################
      ##### TESTS #####
      #################
      
      # Check number of patterns in ListPattern and the actual contents
      assert_equal(
        expected_normalised_list_pattern._list.size(), 
        normalised_list_pattern._list.size(), 
        "occurred when checking the size of the normalised ListPattern in " +
        "scenario " + scenario.to_s
      )
      
      for p in 0...normalised_list_pattern._list.size()
        assert_equal(
          expected_normalised_list_pattern._list.get(p), 
          normalised_list_pattern._list.get(p),
          "occurred when checking pattern " + p.to_s + " in the normalised " +
          "ListPattern in scenario " + scenario.to_s
        )
      end
      
      # Check modality and finished properties
      assert_equal(
        expected_normalised_list_pattern._modality, 
        normalised_list_pattern._modality,
        "occurred when checking the modality of the normalised ListPattern " +
        "in scenario " + scenario.to_s
      )
      
      assert_equal(
        expected_normalised_list_pattern._finished, 
        normalised_list_pattern._finished,
        "occurred when checking the 'finished' property of the normalised " +
        "ListPattern in scenario " + scenario.to_s
      )
    end
  end
end

################################################################################
# Simply checks if the "getInitialFixationInSet()" method returns an instance of 
# AheadOfAgentFixation.  Variables for this instance are checked in the 
# AheadOfAgentFixation tests.
unit_test "get_initial_fixation_in_set" do
  50.times do
    assert_equal(
      AheadOfAgentFixation.new(150, 0).java_class,
      TileworldDomain.new(Chrest.new(0, true), 10, 3, 3, 0, 0).getInitialFixationInSet(0).java_class
    )
  end
end

################################################################################
# Checks operation of the "getNonInitialFixationInSet" function.
# 
# Note that none of the Fixations constructed are actually "performed", rather,
# their variables are set as though they are in the normal course of running a
# simulation with CHREST.  Thus, some variables may not make much sense with 
# respect to the values they are set with however, other tests ensure that such 
# variables are set correctly.
unit_test "get_non_initial_fixation_in_set" do
  
  ########################################
  ##### SET-UP INSTANCE FIELD ACCESS #####
  ########################################
  
  # Fixation instance variables need to be set so they appear to have been 
  # "performed", grant access to these variables.
  Fixation.class_eval{ 
    field_accessor :_scene, :_performanceTime, :_timeDecidedUpon, :_performed, :_colFixatedOn, :_rowFixatedOn, :_objectSeen
  }
  
  # Scene dimensions need to be accessed at times, grant access here.
  scene_width_field = Scene.java_class.declared_field("_width")
  scene_width_field.accessible = true
  scene_height_field = Scene.java_class.declared_field("_height")
  scene_height_field.accessible = true
  
  # Particular Fixation data structures in a CHREST model and Perceiver are 
  # integral to the operation of the function being tested and need to be 
  # manipulated precisely.  Access to these data structures is enabled here.
  Chrest.class_eval{
    field_accessor :_fixationsScheduled, :_saccadeTime
  }
  perceiver_fixations_field = Perceiver.java_class.declared_field("_fixations")
  perceiver_fixations_field.accessible = true
  
  #####################
  ##### MAIN LOOP #####
  #####################
  
  # Some Fixations returned when a HypothesisDiscriminationFixation has not been 
  # performed successfully can return null depending on the previous Fixation 
  # made.  Essentially, this should be allowed to occur since the code's ability 
  # to deal with this needs to be verified.  However, its not possible to 
  # determine if null was returned when a Fixation was generated under these
  # circumstances so the best solution is to run the test a number of times to 
  # ensure that all possible situations can occur and are handled.
  200.times do
  
    time = 0
    
    ###############################
    ##### CHREST MODEL SET-UP #####
    ###############################
    
    model = Chrest.new(time, true)

    #########################
    ##### DOMAIN SET-UP #####
    #########################

    intial_fixation_threshold = 3
    tileworld_domain = TileworldDomain.new(model, intial_fixation_threshold, 3, 8, 0, 0)

    ########################
    ##### SCENE SET-UP #####
    ########################

    # The Scene will be used when setting each Fixation's "_scene" instance
    # variable and will not actually be fixated on.  Consequently, it just needs
    # to be instantiated and not populated with SceneObjects.
    scene = Scene.new("", 5, 5, 0, 0, nil)
    
    ########################################################
    ##### SET-UP FIXATION DATA STRUCTURE FOR PERCEIVER #####
    ########################################################
    
    fixations_attempted = ArrayList.new()

    ######################################################################
    ##### GET FIXATIONS WHEN INITIAL FIXATIONS THRESHOLD NOT REACHED #####
    ######################################################################

    # Populate the model's perceiver fixations with x performed fixations where
    # x is equal to intial_fixation_threshold.
    time += 50
    intial_fixation_threshold.times do
      
      # Get Fixation and check its class
      fixation = tileworld_domain.getNonInitialFixationInSet(time)
      assert_true(
        fixation.java_kind_of?(SalientObjectFixation),
        "occurred when checking the type of Fixation returned when initial " +
        "fixations have not been completed yet"
      )
      
      # Add Fixation to CHREST model's "_fixationsScheduled" data structure.
      fixations_scheduled = ArrayList.new()
      fixations_scheduled.add(fixation)
      model._fixationsScheduled.put(time.to_java(:int), fixations_scheduled)
      
      # Set Fixation variables so it has been "performed"
      fixation._performed = true
      fixation._performanceTime = fixation._timeDecidedUpon + model._saccadeTime
      fixation._scene = scene
      fixation._colFixatedOn = 0
      fixation._rowFixatedOn = 1
      
      # The SceneObject fixated on should be a hole, opponent or tile given the
      # Fixation used.  Done for "realism", not really important.
      fixation._objectSeen = SceneObject.new(
        [ 
          TileworldDomain::HOLE_SCENE_OBJECT_TYPE_TOKEN, 
          TileworldDomain::OPPONENT_SCENE_OBJECT_TYPE_TOKEN,
          TileworldDomain::TILE_SCENE_OBJECT_TYPE_TOKEN
        ].sample
      )

      # Remove/add the Fixation from/to the CHREST model's/Perceiver's Fixation 
      # data structure
      fixations_scheduled = ArrayList.new()
      model._fixationsScheduled.put(fixation._performanceTime.to_java(:int), fixations_scheduled)
      fixations_attempted.add(fixation)
      perceiver_fixations_field.value(model.getPerceiver()).put(fixation._performanceTime.to_java(:int), fixations_attempted)
      
      # Advance time
      time = fixation._performanceTime + 300
    end
    
    ##################################################################
    ##### GET FIXATIONS WHEN INITIAL FIXATIONS THRESHOLD REACHED #####
    ##################################################################

    # When the initial fixations threshold has been reached, a 
    # HypothesisDiscriminationFixation should always be returned unless:
    # 
    # 1. A HypothesisDiscriminationFixation is scheduled to be performed but has
    #    not been performed when the function is invoked. 
    # 2. The previous Fixation attempted was a HypothesisDiscriminationFixation 
    #    but wasn't performed successfully.  
    # 
    # In these cases, a SalientObjectFixation, MovementFixation, 
    # PeripheralItemFixation or PeripheralSquareFixation should be returned.  
    # However, since there is an equal probability of generating these Fixation
    # types in the cases described, the function will be invoked until all these
    # Fixation types have been returned in both cases.  To facilitate this, 
    # create boolean flags that indicate whether each Fixation has been returned
    # in each case and set them to false initially.
    
    # Boolean flags when function is invoked before 
    # HypothesisDiscriminationFixation is performed.
    salient_object_fixation_returned_before_hypothesis_discrimination_fixation_performed = false
    movement_fixation_returned_before_hypothesis_discrimination_fixation_performed = false
    peripheral_item_fixation_returned_before_hypothesis_discrimination_fixation_performed = false
    peripheral_square_fixation_returned_before_hypothesis_discrimination_fixation_performed = false
    
    # Boolean flags when function is invoked after
    # HypothesisDiscriminationFixation is performed.
    salient_object_fixation_returned_after_hypothesis_discrimination_fixation_performed = false
    movement_fixation_returned_after_hypothesis_discrimination_fixation_performed = false
    peripheral_item_fixation_returned_after_hypothesis_discrimination_fixation_performed = false
    peripheral_square_fixation_returned_after_hypothesis_discrimination_fixation_performed = false
    
    # Function invocation loop
    while 
      !salient_object_fixation_returned_before_hypothesis_discrimination_fixation_performed or 
      !movement_fixation_returned_before_hypothesis_discrimination_fixation_performed
      !peripheral_item_fixation_returned_before_hypothesis_discrimination_fixation_performed or 
      !peripheral_square_fixation_returned_before_hypothesis_discrimination_fixation_performed or
      !salient_object_fixation_returned_after_hypothesis_discrimination_fixation_performed or
      !movement_fixation_returned_after_hypothesis_discrimination_fixation_performed or
      !peripheral_item_fixation_returned_after_hypothesis_discrimination_fixation_performed or
      !peripheral_square_fixation_returned_after_hypothesis_discrimination_fixation_performed
    
      ############################################################
      ##### GET HypothesisDiscriminationFixation AND PERFORM #####
      ############################################################
      
      # Get the next Fixation from the function.  This should be a 
      # HypothesisDiscriminationFixation instance since:
      #
      # 1. The function is invoked for the first time after the initial fixation
      #    threshold has been reached (first iteration of while loop).
      # 2. The function is invoked after a HypothesisDiscriminationFixation has 
      #    been attempted but performed unsuccessfully (iteration 2+ of while 
      #    loop).
      fixation = tileworld_domain.getNonInitialFixationInSet(time)
      assert_equal(
        HypothesisDiscriminationFixation.java_class,
        fixation.java_class,
        "occurred when checking the type of Fixation returned after initial " + 
        "fixations have been completed and a hypothesis-discrimination fixation " +
        "hasn't been attempted"
      )
      
      # Add the Fixation to the CHREST model's Fixation to make data structure.
      fixations_scheduled = ArrayList.new()
      fixations_scheduled.add(fixation)
      model._fixationsScheduled.put(time.to_java(:int), fixations_scheduled)
      
      # Set Fixation variables that would be set if the Fixation were performed
      # "properly"
      fixation._performanceTime = fixation._timeDecidedUpon + model._saccadeTime
      fixation._scene = scene
      
      # Remove/add the last, unperformed, HypothesisDiscriminationFixation 
      # instance from/to the CHREST model's/Perceiver's Fixation data structure
      fixations_scheduled = ArrayList.new()
      model._fixationsScheduled.put(fixation._performanceTime.to_java(:int), fixations_scheduled)
      fixations_attempted.add(fixation)
      perceiver_fixations_field.value(model.getPerceiver()).put(fixation._performanceTime.to_java(:int), fixations_attempted)
      
      ##########################################################################
      ##### GET FIXATION BEFORE HypothesisDiscriminationFixation PERFORMED #####
      ##########################################################################
      
      # Invoke the function before the time the HypothesisDiscriminationFixation
      # was performed to see if the correct type of Fixation is performed when a 
      # Hypothesis DiscriminationFixation is scheduled to be performed but 
      # hasn't been performed yet.  This Fixation will not be added to the 
      # Fixation data structures, however. 
      fixation_returned_before_hypothesis_discrimination_fixation_performed = tileworld_domain.getNonInitialFixationInSet(
        rand(time...fixation._performanceTime)
      )
      
      assert_true(
        (
          fixation_returned_before_hypothesis_discrimination_fixation_performed.java_kind_of?(SalientObjectFixation) || 
          fixation_returned_before_hypothesis_discrimination_fixation_performed.java_kind_of?(MovementFixation) || 
          fixation_returned_before_hypothesis_discrimination_fixation_performed.java_kind_of?(PeripheralItemFixation) ||
          fixation_returned_before_hypothesis_discrimination_fixation_performed.java_kind_of?(PeripheralSquareFixation)
        ),
        "occurred when checking the Fixation class returned by the function when " +
        "a HypothesisDiscriminationFixation is scheduled for performance but " +
        "hasn't been performed when the function is invoked.  Fixation returned:" +
        fixation_returned_before_hypothesis_discrimination_fixation_performed.toString()
      )
      
      # Set while loop control variable accordingly.
      case fixation_returned_before_hypothesis_discrimination_fixation_performed.java_class
      when SalientObjectFixation.java_class
        salient_object_fixation_returned_before_hypothesis_discrimination_fixation_performed = true
      when MovementFixation.java_class
        movement_fixation_returned_before_hypothesis_discrimination_fixation_performed = true
      when PeripheralItemFixation.java_class
        peripheral_item_fixation_returned_before_hypothesis_discrimination_fixation_performed = true
      when PeripheralSquareFixation.java_class
        peripheral_square_fixation_returned_before_hypothesis_discrimination_fixation_performed = true
      end
      
      #############################
      ##### GET NEXT FIXATION #####
      #############################
      
      # Advance time
      time = fixation._performanceTime + 100
      
      #Get the next fixation, this will be performed.
      fixation = tileworld_domain.getNonInitialFixationInSet(time)
      assert_true(
        (
          fixation.java_kind_of?(SalientObjectFixation) || 
          fixation.java_kind_of?(MovementFixation) || 
          fixation.java_kind_of?(PeripheralItemFixation) ||
          fixation.java_kind_of?(PeripheralSquareFixation)
        ),
        "occurred when checking the type of Fixation returned when the last " +
        "Fixation attempted was a HypothesisDiscriminationFixation but was not " +
        "performed successfully. Fixation returned was a " + 
        fixation.java_class.to_s + "."
      )

      #Set the relevant boolean value that controls the while loop 
      case fixation.java_class
      when SalientObjectFixation.java_class 
        salient_object_fixation_returned_after_hypothesis_discrimination_fixation_performed = true
      when MovementFixation.java_class
        movement_fixation_returned_after_hypothesis_discrimination_fixation_performed = true
      when PeripheralItemFixation.java_class 
        peripheral_item_fixation_returned_after_hypothesis_discrimination_fixation_performed = true
      when PeripheralSquareFixation.java_class 
        peripheral_square_fixation_returned_after_hypothesis_discrimination_fixation_performed = true 
      end
      
      # Add the Fixation to the CHREST model's Fixation to make data structure.
      fixations_scheduled = ArrayList.new()
      fixations_scheduled.add(fixation)
      model._fixationsScheduled.put(time.to_java(:int), fixations_scheduled)

      # "Perform" the fixation.  Note that the coordinates fixated on are 
      # randomly generated, this is because some Fixations returned when a 
      # HypothesisDiscriminationFixation has not been performed successfully 
      # can return null depending on the previous Fixation made.  Essentially,
      # this should be allowed to occur since the code's ability to deal with
      # this needs to be verified.
      time = fixation.getTimeDecidedUpon() + model._saccadeTime
      fixation._performanceTime = time
      fixation._performed = true
      fixation._scene = scene
      fixation._colFixatedOn = rand(0...scene_width_field.value(scene))
      fixation._rowFixatedOn =  rand(0..scene_height_field.value(scene))
      
      # The SceneObject fixated on should be a hole, opponent or tile given the
      # Fixations used.  Done for "realism", not really important.
      fixation._objectSeen = SceneObject.new(
        SecureRandom.uuid(), 
        [ 
          TileworldDomain::HOLE_SCENE_OBJECT_TYPE_TOKEN, 
          TileworldDomain::OPPONENT_SCENE_OBJECT_TYPE_TOKEN,
          TileworldDomain::TILE_SCENE_OBJECT_TYPE_TOKEN
        ].sample
      )
        
      # Remove/add the last Fixation from/to the CHREST model's/Perceiver's 
      # Fixation data structure
      fixations_scheduled = ArrayList.new()
      model._fixationsScheduled.put(fixation._performanceTime.to_java(:int), fixations_scheduled)
      fixations_attempted.add(fixation)
      perceiver_fixations_field.value(model.getPerceiver()).put(fixation._performanceTime.to_java(:int), fixations_attempted)
      
      # Advance time
      time = fixation._performanceTime + 300
    end
  end
end