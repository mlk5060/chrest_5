unit_test "constructor" do
  
  #Test that an IllegalArgumentException is thrown correctly if the maximum 
  #number of attempts constructor parameter is < 1
  time = 0
  model = Chrest.new(time, false)
  
  exception_thrown = false
  begin
    PeripheralItemFixation.new(model, 0, time += 2)
  rescue
    exception_thrown = true
  end
  
  assert_true(
    exception_thrown,
    "occurred when checking if an exception is thrown when the maximum number " +
    "of attempts parameter is < 1"
  )
  
  #Test that no IllegalArgumentException is thrown if the maximum number of 
  #attempts constructor parameter is >= 1 and that all variables in the new
  #PeripheralItemFixation object are set correctly.  To do this, the test needs
  #to be able to access the private "_model" and "_maxAttempts" 
  #PeripheralItemFixation object variables so enable this now.
  PeripheralItemFixation.class_eval{ 
    field_reader :_model 
    field_reader :_maxAttempts
  }
  
  for i in 1..2
    exception_thrown = false
    fixation = nil
    maximum_attempts_parameter = (i == 1 ? 1 : 5)
    begin
      fixation = PeripheralItemFixation.new(model, maximum_attempts_parameter, time)
    rescue
      exception_thrown = true
    end
    
    assert_false(
      exception_thrown,
      "occurred when checking if an exception is thrown when the maximum attempts " +
      "parameter is set to " + maximum_attempts_parameter.to_s  
    )
    
    assert_equal(
      time, 
      fixation.getTimeDecidedUpon(),
      "occurred when checking the time that the Fixation should be decided upon"
    )
    
    assert_equal(
      model, 
      fixation._model,
      "occurred when checking the CHREST model the Fixation should be loaded with"
    )
    
    assert_equal(
      maximum_attempts_parameter, 
      fixation._maxAttempts,
      "occurred when checking the maximum attempts parameter the Fixation should " +
      "be loaded with"
    )
  end
end

################################################################################
# To test "PeripheralItemFixation.make()", 13 scenarios are setup and run 3 
# times with different times to invoke the function (before, at and after the
# performance time of a PeripheralItemFixation).  These times will usually be
# greater than the CHREST model's creation time (see scenario 1 note).
# 
# Each scenario translates into a conditional in the function that is tested to 
# ensure that it controls access through the function appropriately.  Note that
# scenario tests are cumulative, e.g. scenario 2 ensures that the conditional 
# tested in scenario 1 passes.
# 
# Scenario 1: Fail.  
#  - Function invoked before CHREST model created.  NOTE: for this scenario, the 
#    repeat conditions concerning invocation times of the function do not apply.
# 
# Scenario 2: Fail
#  - Function invoked after CHREST model created
#  - No Fixation has been performed before the PeripheralItemFixation is 
#    attempted to be made.
#
# Scenario 3: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralItemFixation.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has no Scene set.
#  
# Scenario 4: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralItemFixation.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has no x coordinate set for the Square fixated on.
#
# Scenario 5: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralItemFixation.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has an x coordinate set for the Square fixated on.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has no y coordinate set for the Square fixated on.
#
# Scenario 6: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralItemFixation.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has an x coordinate set for the Square fixated on.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a y coordinate set for the Square fixated on.
#  - Scene previously fixated on and the Scene to be fixated on do not refer 
#    to the same external domain space.
#
# Scenario 7: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralItemFixation.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has an x coordinate set for the Square fixated on.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a y coordinate set for the Square fixated on.
#  - Scene previously fixated on and the Scene to be fixated on refer to the 
#    same external domain space.
#  - Scene previously fixated on is entirely blind.
#
# Scenario 8: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralItemFixation.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has an x coordinate set for the Square fixated on.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a y coordinate set for the Square fixated on.
#  - Scene previously fixated on and the Scene to be fixated on refer to the 
#    same external domain space.
#  - Scene previously fixated on is not entirely blind.
#  - Function invoked after PeripheralItemFixation performance time
#  - The PeripheralItemFixation suggests the same Square to fixate on as in 
#    the most recent and successful Fixation.
#
# Scenario 9: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralItemFixation.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has an x coordinate set for the Square fixated on.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a y coordinate set for the Square fixated on.
#  - Scene previously fixated on and the Scene to be fixated on refer to the 
#    same external domain space.
#  - Scene previously fixated on is not entirely blind.
#  - Function invoked after PeripheralItemFixation performance time
#  - The PeripheralItemFixation suggests a different Square to fixate on than
#    the one fixated on by the most recent and successful Fixation.
# -  The Square to fixate on is outside of the Scene to fixate on's 
#    dimensions.
#
# Scenario 10: Fail
#  - Function invoked after CHREST model created.
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralItemFixation.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has an x coordinate set for the Square fixated on.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a y coordinate set for the Square fixated on.
#  - Scene previously fixated on and the Scene to be fixated on refer to the 
#    same external domain space.
#  - Scene previously fixated on is not entirely blind.
#  - Function invoked after PeripheralItemFixation performance time.
#  - The PeripheralItemFixation suggests a different Square to fixate on than
#    the one fixated on by the most recent and successful Fixation.
#  - The Square to fixate on is inside of the Scene to fixate on's dimensions.
#  - The Square to fixate on is a blind Square.
#
# Scenario 11: Fail
#  - Function invoked after CHREST model created.
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralItemFixation.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has an x coordinate set for the Square fixated on.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a y coordinate set for the Square fixated on.
#  - Scene previously fixated on and the Scene to be fixated on refer to the 
#    same external domain space.
#  - Scene previously fixated on is not entirely blind.
#  - Function invoked after PeripheralItemFixation performance time.
#  - The PeripheralItemFixation suggests a different Square to fixate on than
#    the one fixated on by the most recent and successful Fixation.
#  - The Square to fixate on is inside of the Scene to fixate on's dimensions.
#  - The Square to fixate on is not a blind Square.
#  - The Square to fixate on is an empty Square.
#
# Scenario 12: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralItemFixation.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has an x coordinate set for the Square fixated on.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a y coordinate set for the Square fixated on.
#  - Scene previously fixated on and the Scene to be fixated on refer to the 
#    same external domain space.
#  - Scene previously fixated on is not entirely blind.
#  - Function invoked after PeripheralItemFixation performance time.
#  - The PeripheralItemFixation suggests a different Square to fixate on than
#    the one fixated on by the most recent and successful Fixation.
#  - The Square to fixate on is inside of the Scene to fixate on's dimensions.
#  - The Square to fixate on is not a blind Square.
#  - The Square to fixate on is not an empty Square.
#  - The Square to fixate on contains the agent that created the Scene.
#
# Scenario 13: Pass
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralItemFixation.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has an x coordinate set for the Square fixated on.
#  - Fixation performed before the PeripheralItemFixation is attempted to be 
#    made has a y coordinate set for the Square fixated on.
#  - Scene previously fixated on and the Scene to be fixated on refer to the 
#    same external domain space.
#  - Scene previously fixated on is not entirely blind.
#  - Function invoked after PeripheralItemFixation performance time
#  - The PeripheralItemFixation suggests a different Square to fixate on than
#    the one fixated on by the most recent and successful Fixation.
#  - The Square to fixate on is inside of the Scene to fixate on's dimensions.
#  - The Square to fixate on is not a blind Square.
#  - The Square to fixate on is not an empty Square.
#  - The Square to fixate on does not contain the agent that created the 
#    Scene.
unit_test "make" do
  
  #Need to be able to set Fixation object instance variables precisely so make
  #these private instance variables accessible and writable.
  Fixation.class_eval{ 
    field_writer :_performanceTime, :_performed, :_scene, :_colFixatedOn, :_rowFixatedOn
  }
  
  for performance_time in 1..3
    for scenario in 1..13

      ##################################
      ##### CONSTRUCT CHREST MODEL #####
      ##################################
      
      # Set the time that the CHREST model will be created to a value > 0 so 
      # that, if the "scenario" variable is = 1, the time the "make" function is
      # invoked can be less than this time but not be a negative value.
      time = 5
      model_creation_time = time
      model = Chrest.new(model_creation_time, false)

      ###############################################
      ##### CONSTRUCT AND ADD PREVIOUS FIXATION #####
      ###############################################

      # To make a peripheral item fixation, a previous fixation needs to have been
      # made on a Scene, construct this Scene first.
      first_fixation_scene = Scene.new("", 5, 5, 0, 0, nil) #Initialised as being entirely blind

      # For scenario 7, first_fixation_scene should remain entirely blind.
      if scenario != 7
        empty = Scene.getEmptySquareToken()
        for col in 0...first_fixation_scene.getWidth()
          for row in 0...first_fixation_scene.getHeight()
            first_fixation_scene.addItemToSquare(col, row, empty, empty)
          end
        end
      end

      # Construct and add a Fixation that has been performed successfully to the
      # Perceiver associated with "model" if the scenario stipulates this.
      first_fixation = CentralFixation.new(time += 100)
      first_fixation._performanceTime = (time += 50)

      # If the scenario is equal to 2, the Fixation will not have been performed.
      if scenario != 2
        first_fixation._performed = true
        first_fixation._scene = first_fixation_scene

        # If scenario is equal to 9 then the coordinates should never allow
        # the PeripheralItemFixation to be made to return a Square that is within
        # its dimensions.
        first_fixation._colFixatedOn = (
          scenario == 9 ? 
            first_fixation_scene.getWidth() + (model.getPerceiver().getFixationFieldOfView() + 1) : 
            2
        )

        first_fixation._rowFixatedOn = (
          scenario == 9 ? 
            first_fixation_scene.getHeight() + (model.getPerceiver().getFixationFieldOfView() + 1) : 
            2
        )
      end

      model.getPerceiver().addFixation(first_fixation)

      # Set various instance variables of "first_fixation" to nil after adding it
      # to the Perceiver's fixations (if this is done before the addition, the 
      # addition will fail for the scenario's indicated causing the test to not
      # complete).
      if(scenario == 3) then first_fixation._scene = nil end
      if(scenario == 4) then first_fixation._colFixatedOn = nil end
      if(scenario == 5) then first_fixation._rowFixatedOn = nil end

      ###########################################################
      ##### CONSTRUCT AND MAKE THE PERIPHERAL-ITEM FIXATION #####
      ###########################################################

      # Construct the PeripheralItemFixation that will be tested.  Note that the
      # maximum number of attempts to suggest a suitable Square to make the 
      # fixation on is huge.  This helps to ensure that the outcome of making the
      # PeripheralItemFixation (if the scenario ensures that the while loop that 
      # generates a possible Square to fixate on is reached) is due to the 
      # condition(s) being checked by the scenario.
      fixation_to_make = PeripheralItemFixation.new(model, 200, time += 50)
      fixation_to_make._performanceTime = (time += 100)

      # Set time to invoke PeripheralItemFixation.make()
      time_to_make_fixation = 
        (scenario == 1 ?
          model_creation_time - 1 :
          (performance_time == 1 ?
            fixation_to_make.getPerformanceTime() - 1 :
            (performance_time == 2 ?
              fixation_to_make.getPerformanceTime() :
              fixation_to_make.getPerformanceTime() + 1
            )
          )
        )
      
      # Override time set above if this is scenario 1.
      if scenario == 1 then time_to_make_fixation = model_creation_time - 1 end

      # Set the Scene to make the PeripheralItemFixation in context of.  This 
      # should refer to the same "external domain space" as first_fixation_scene
      # in all but scenario 6.
      peripheral_item_fixation_scene = (scenario == 6 ? Scene.new("", 5, 5, 3, 3, nil) : Scene.new("", 5, 5, 0, 0, nil))

      # For scenario 7, peripheral_item_fixation_scene should remain entirely 
      # blind.
      if scenario != 7

        # For most scenarios, the Scene to make the PeripheralItemFixation in 
        # context of should be entirely populated with non-blind and non-empty 
        # SceneObjects that don't denote the agent that created the Scene except 
        # for the Square the previous fixation was made in context of (this should
        # never be selected but, if checking that blind Squares are rejected, 
        # leaving this Square non-blind means that the "isEntirelyBlind()" check
        # in the function will not evaluate to true so the while loop will be 
        # encountered).
        object_id = 0
        object_class = "J"

        if(scenario == 10) then object_class = Scene.getBlindSquareToken() end
        if(scenario == 11) then object_class = Scene.getEmptySquareToken() end
        if(scenario == 12) then object_class = Scene.getCreatorToken() end

        for col in 0...peripheral_item_fixation_scene.getWidth()
          for row in 0...peripheral_item_fixation_scene.getHeight()

            if col != 2 and row != 2 
              peripheral_item_fixation_scene.addItemToSquare(col, row, object_id.to_s, object_class)
            end

            object_id+=1
          end
        end
      end

      # If scenario is 9, set the "fixation field of view" parameter for the 
      # Perceiver associated with the model making the PeripheralItemFixation to
      # 0.  This will ensure that the Square selected for fixation when the 
      # PeripheralItemFixation is made will be the same as the Square fixated on
      # when the first Fixation was made.  Otherwise, set it to 2 so that there
      # is a chance that the Square selected to be fixated on by the 
      # PeripheralItemFixation is not the same as the Square fixated on when the 
      # first Fixation was made.
      model.getPerceiver().setFixationFieldOfView(scenario == 8 ? 0 : 2)
      
      #################
      ##### TESTS #####
      #################
      
      error_msg = "occurred when the fixation is to be performed " + 
      (performance_time == 1 ? "before" : performance_time == 2 ? "at" : "after") +
      " its defined performance time in scenario " + scenario.to_s

      # For scenarios 8 to 13, the test setup can't guarantee that the 
      # particular condition being tested is the one that causes null to be 
      # returned since the x and y displacement values may both be set to 0 
      # sometimes due to the random nature of this assignment.  So, make the
      # fixation enough times to ensure that the desired condition will be 
      # triggered.  This also ensures consistent behaviour in other scenarios.
      500.times do

        fixation = fixation_to_make.make(peripheral_item_fixation_scene, time_to_make_fixation)

        # If the test scenario isn't 13, the outcome of attempting to make the 
        # PeripheralItemFixation should always be null.
        if performance_time == 1 or (performance_time != 1 and scenario != 13)
          expected_fixation = nil

          assert_equal(
            expected_fixation,
            fixation,
            error_msg
          )
        # If this is scenario 13, the outcome of attempting to make the 
        # PeripheralItemFixation should never be null but may vary (any Square 
        # other than [2, 2]).  
        else
          expected_fixations = []
          for col in 0...peripheral_item_fixation_scene.getWidth()
            for row in 0...peripheral_item_fixation_scene.getHeight()
              if col != 2 and row != 2
                expected_fixations.push(Square.new(col, row).toString())
              end
            end
          end

          assert_true(
            expected_fixations.include?(fixation.toString()),
            error_msg
          )
        end
      end
    end
  end
end
