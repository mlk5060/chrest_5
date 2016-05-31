################################################################################
# Tests that constructor parameters are set correctly.
unit_test "constructor" do
  
  #######################################################
  ##### SET-UP ACCESS TO PRIVATE INSTANCE VARIABLES #####
  #######################################################
  
  chrest_creation_time_field = Chrest.java_class.declared_field("_creationTime")
  chrest_creation_time_field.accessible = true
  
  chrest_learn_object_loc_field = Chrest.java_class.declared_field("_learnObjectLocationsRelativeToAgent")
  chrest_learn_object_loc_field.accessible = true
  
  DomainSpecifics.class_eval{
    field_accessor :_maxFixationsInSet
  }
  
  domain_model_field = DomainSpecifics.java_class.declared_field("_associatedModel")
  domain_model_field.accessible = true
  
  generic_domain_peripheral_item_fix_max_attempts_field = GenericDomain.java_class.declared_field("_peripheralItemFixationMaxAttempts")
  generic_domain_peripheral_item_fix_max_attempts_field.accessible = true
  
  20.times do
    
    #############################################
    ##### INITIALISE CONSTRUCTOR PARAMETERS #####
    #############################################
    
    # Set each construction parameter to a unique value relative to other 
    # construction parameters to check that the constructor assigns the relevant 
    # instance variable correctly.
    
    # Create a CHREST model with known values so they can be checked to see if
    # the model is assigned correctly by the constructor.
    model_creation_time = rand(0..200)
    learn_object_locations_relative_to_self = [true, false].sample
    model = Chrest.new(model_creation_time, learn_object_locations_relative_to_self)
    
    # Set the maximum fixations in a set 
    max_fixations_in_set = 10
    
    # Set the maximum number of peripheral item fixations allowed.
    peripheral_item_fixation_max_attempts = 3
    
    ##########################################################
    ##### INVOKE CONSTRUCTOR AND STORE INSTANCE RETURNED #####
    ##########################################################
    
    domain = GenericDomain.new(model, max_fixations_in_set, peripheral_item_fixation_max_attempts)
    
    #################
    ##### TESTS #####
    #################
    
    # Check that the CHREST model passed as a parameter has been set to the 
    # correct instance variable in the GenericDomain
    model_set = domain_model_field.value(domain)
    assert_equal(
      model_creation_time,
      chrest_creation_time_field.value(model_set), 
      "occurred when checking the creation time of the CHREST model " +
      "associated with the new domain instance"
    )
   
    assert_equal(
      learn_object_locations_relative_to_self,
      chrest_learn_object_loc_field.value(model), 
      "occurred when checking the 'learn object locations relative to self' " +
      "parameter of the CHREST model associated with the new domain instance"
    )
    
    # Check that the maximum fixations in set parameter has been set to the 
    # correct instance variable in the GenericDomain
    assert_equal(max_fixations_in_set, domain._maxFixationsInSet)
    
    # Check that the peripheral item fixation maximum attempts parameter has 
    # been set to the correct instance variable in the GenericDomain
    assert_equal(peripheral_item_fixation_max_attempts, generic_domain_peripheral_item_fix_max_attempts_field.value(domain))
  end
end

################################################################################

unit_test "normalise" do
  
  #Item square patterns are handled specially by the GenericDomain.normalise() 
  #function hence the verbose variable names.
  blind_item_square_pattern = ItemSquarePattern.new(Scene::BLIND_SQUARE_TOKEN, 0, 0)
  empty_item_square_pattern = ItemSquarePattern.new(Scene::EMPTY_SQUARE_TOKEN, 0, 1)
  self_item_square_pattern = ItemSquarePattern.new(Scene::CREATOR_TOKEN, 0, 2)
  non_empty_item_square_pattern = ItemSquarePattern.new("A", 0, 3)
  duplicate_non_empty_item_square_pattern = ItemSquarePattern.new("A", 0, 3)
  
  # Constructor parameters not important here, pay no attention to them.
  generic_domain = GenericDomain.new(Chrest.new(0, false), 10, 3)
  
  # Create a ListPattern of each Modality defined to test whether the normalised 
  # ListPattern is always equal to the modality of the ListPattern that is to be 
  # normalised.
  for modality in Modality.values() do
    
    #Empty square and self identifiers should be ignored by the 
    #GenericDomain.normalise method if they are of type StringPattern.
    string_pattern_one = Pattern.makeString(Scene.getEmptySquareToken())
    string_pattern_two = Pattern.makeString(Scene.getCreatorToken())
    string_list_pattern = ListPattern.new(modality)
    string_list_pattern.add(string_pattern_one)
    string_list_pattern.add(string_pattern_two)
    
    number_first = Pattern.makeNumber(123)
    number_second = Pattern.makeNumber(456)
    number_list_pattern = ListPattern.new(modality)
    number_list_pattern.add(number_first)
    number_list_pattern.add(number_second)
    
    item_square_list_pattern = ListPattern.new(modality)
    item_square_list_pattern.add(blind_item_square_pattern)
    item_square_list_pattern.add(empty_item_square_pattern)
    item_square_list_pattern.add(self_item_square_pattern)
    item_square_list_pattern.add(non_empty_item_square_pattern)
    item_square_list_pattern.add(duplicate_non_empty_item_square_pattern)
    
    #########################
    ##### INVOKE METHOD #####
    #########################
    normalised_string_pattern = generic_domain.normalise(string_list_pattern)
    normalised_number_pattern = generic_domain.normalise(number_list_pattern)
    normalised_item_square_pattern = generic_domain.normalise(item_square_list_pattern)
    
    #################
    ##### TESTS #####
    #################
    
    #The expected result for the list pattern containing item square patterns 
    #consists only of the "non_empty_item_square_pattern" in a list pattern. 
    expected_item_square_list_pattern = ListPattern.new(modality)
    expected_item_square_list_pattern.add(non_empty_item_square_pattern)
    assert_equal(1, normalised_item_square_pattern.size(), "occurred when checking the size of the normalised item-square pattern")
    assert_equal(modality, normalised_item_square_pattern.getModality(), "occurred when checking the modality of the normalised item-square pattern")
    assert_equal(expected_item_square_list_pattern.toString(), normalised_item_square_pattern.toString(), "occurred when checking the contents of the normalised item-square pattern")
    
    #Original string ListPattern should remain unaltered.
    assert_equal(2, normalised_string_pattern.size(), "occurred when checking the size of the normalised string pattern")
    assert_equal(modality, normalised_string_pattern.getModality(), "occurred when checking the modality of the normalised string pattern")
    assert_equal(string_list_pattern.toString(), normalised_string_pattern.toString(), "occurred when checking the contents of the normalised string pattern")
    
    #Original number ListPattern should remain unaltered.
    assert_equal(2, normalised_number_pattern.size(), "occurred when checking the size of the normalised number pattern")
    assert_equal(modality, normalised_number_pattern.getModality(), "occurred when checking the modality of the normalised number pattern")
    assert_equal(number_list_pattern.toString(), normalised_number_pattern.toString(), "occurred when checking the contents of the normalised number pattern")
  end
end

################################################################################
unit_test "get_initial_fixation_in_set" do
  50.times do
    learn_object_locations_relative_to_self = [true, false].sample
    model = Chrest.new(0, learn_object_locations_relative_to_self)
    domain = GenericDomain.new(model, 10, 3)
    
    expected_fixation = (learn_object_locations_relative_to_self ? AheadOfAgentFixation : CentralFixation)
    assert_equal(
      expected_fixation.java_class,
      domain.getInitialFixationInSet(0).java_class,
      "occurred when the CHREST model invoking the method is " + 
      (learn_object_locations_relative_to_self ? "" : "not") + " learning " +
      "object locations relative to itself"
    )
  end
end

################################################################################
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

    domain = GenericDomain.new(model, 10, 8)

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
    
    ########################
    ##### GET FIXATION #####
    ########################

    # A HypothesisDiscriminationFixation should always be returned unless:
    # 
    # 1. A HypothesisDiscriminationFixation is scheduled to be performed but has
    #    not been performed when the function is invoked. 
    # 2. The previous Fixation attempted was a HypothesisDiscriminationFixation 
    #    but wasn't performed successfully.  
    # 
    # In these cases, a PeripheralItemFixation or PeripheralSquareFixation 
    # should be returned.  However, since there is an equal probability of 
    # generating these Fixation types in the cases described, the method will be 
    # invoked until both of these Fixation types have been returned in both 
    # cases.  To facilitate this, create boolean flags that indicate whether 
    # each Fixation has been returned in each case and set them to false 
    # initially.
    
    # Boolean flags when function is invoked before 
    # HypothesisDiscriminationFixation is performed.
    peripheral_item_fixation_returned_before_hypothesis_discrimination_fixation_performed = false
    peripheral_square_fixation_returned_before_hypothesis_discrimination_fixation_performed = false
    
    # Boolean flags when function is invoked after
    # HypothesisDiscriminationFixation is performed.
    peripheral_item_fixation_returned_after_hypothesis_discrimination_fixation_performed = false
    peripheral_square_fixation_returned_after_hypothesis_discrimination_fixation_performed = false
    
    # Function invocation loop
    while 
      !peripheral_item_fixation_returned_before_hypothesis_discrimination_fixation_performed or 
      !peripheral_square_fixation_returned_before_hypothesis_discrimination_fixation_performed or
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
      fixation = domain.getNonInitialFixationInSet(time)
      assert_equal(
        HypothesisDiscriminationFixation.java_class,
        fixation.java_class,
        "occurred when checking the type of Fixation returned when a " +
        "hypothesis-discrimination Fixation hasn't been attempted"
      )
      
      # Add the Fixation to the CHREST model's scheduled Fixations data 
      # structure.
      fixations_scheduled = ArrayList.new()
      fixations_scheduled.add(fixation)
      model._fixationsScheduled.put(time.to_java(:int), fixations_scheduled)
      
      # Set Fixation variables that would be set if the Fixation were performed
      # "properly"
      fixation._performanceTime = fixation._timeDecidedUpon + model._saccadeTime
      fixation._scene = scene
      
      # Remove/add the last, unperformed, HypothesisDiscriminationFixation 
      # instance from/to the CHREST model's/Perceiver's Fixation data structure
      # at the time the Fixation is performed
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
      fixation_returned_before_hypothesis_discrimination_fixation_performed = domain.getNonInitialFixationInSet(
        rand(time...fixation._performanceTime)
      )
      
      assert_true(
        (
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
      fixation = domain.getNonInitialFixationInSet(time)
      assert_true(
        (
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
      
      # The SceneObject fixated on should be non-blind and not the creator given 
      # the Fixations used.  Done for "realism", not really important.
      fixation._objectSeen = SceneObject.new(
        [ 
          Scene::EMPTY_SQUARE_TOKEN, 
          "A"
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

################################################################################
unit_test "should_learn_from_new_fixations" do
  50.times do
    domain = GenericDomain.new(Chrest.new(0, [true, false].sample), 10, 3)
    assert_false(domain.shouldLearnFromNewFixations(rand(0..100)))
  end
end

################################################################################
unit_test "is_fixation_set_complete" do
  50.times do
    domain = GenericDomain.new(Chrest.new(0, [true, false].sample), 10, 3)
    assert_false(domain.isFixationSetComplete(rand(0..100)))
  end
end

################################################################################
unit_test "should_add_new_fixation" do
  50.times do
    domain = GenericDomain.new(Chrest.new(0, [true, false].sample), 10, 3)
    assert_true(domain.shouldAddNewFixation(rand(0..100)))
  end
end



