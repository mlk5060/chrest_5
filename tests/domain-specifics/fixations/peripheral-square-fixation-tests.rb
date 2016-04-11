################################################################################
# Constructor sets the CHREST model to use when 
# "PeripheralSquareFixation.make()" is invoked and the time the 
# PeripheralSquareFixation will be decided upon so these variables are checked
# after construction of the PeripheralSquareFixation instance.
unit_test "constructor" do
  
  # This test needs to be able to access the private "_model" 
  # PeripheralSquareFixation object variables so enable this now.
  PeripheralSquareFixation.class_eval{ 
    field_reader :_model
  }
  
  # Construct instance.
  time = 0
  model = Chrest.new(time, false)
  fixation = PeripheralSquareFixation.new(model, time += 100)
  
  #Perform tests
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
end

################################################################################
# To test "PeripheralItemFixation.make()", 13 scenarios are setup and run 3 
# times with different times to invoke the function (before, at and after the
# performance time of a PeripheralSquareFixation).  These times will usually be
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
#  - No Fixation has been performed before the PeripheralSquareFixation is 
#    attempted to be made.
#
# Scenario 3: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralSquareFixation.
#  - Fixation field of view for the Perceiver associated with the model 
#    attempting to make the PeripheralSquareFixation is < 0.
#    
# Scenario 4: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralSquareFixation.
#  - Fixation field of view for the Perceiver associated with the model 
#    attempting to make the PeripheralSquareFixation is = 0.
#    
# Scenario 5: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralSquareFixation.
#  - Fixation field of view for the Perceiver associated with the model 
#    attempting to make the PeripheralSquareFixation is > 0.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has no Scene set.
#  
# Scenario 6: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralSquareFixation.
#  - Fixation field of view for the Perceiver associated with the model 
#    attempting to make the PeripheralSquareFixation is > 0.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has no x coordinate set for the Square fixated on.
#
# Scenario 7: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralSquareFixation.
#  - Fixation field of view for the Perceiver associated with the model 
#    attempting to make the PeripheralSquareFixation is > 0.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has an x coordinate set for the Square fixated on.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has no y coordinate set for the Square fixated on.
#
# Scenario 8: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralItemFixation.
#  - Fixation field of view for the Perceiver associated with the model 
#    attempting to make the PeripheralSquareFixation is > 0.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has an x coordinate set for the Square fixated on.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has a y coordinate set for the Square fixated on.
#  - Scene previously fixated on and the Scene to be fixated on do not refer 
#    to the same external domain space.
#
# Scenario 9: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralSquareFixation.
#  - Fixation field of view for the Perceiver associated with the model 
#    attempting to make the PeripheralSquareFixation is > 0.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has an x coordinate set for the Square fixated on.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has a y coordinate set for the Square fixated on.
#  - Scene previously fixated on and the Scene to be fixated on refer to the 
#    same external domain space.
#  - Scene previously fixated on is entirely blind.
#
# Scenario 10: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralSquareFixation.
#  - Fixation field of view for the Perceiver associated with the model 
#    attempting to make the PeripheralSquareFixation is > 0.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has an x coordinate set for the Square fixated on.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has a y coordinate set for the Square fixated on.
#  - Scene previously fixated on and the Scene to be fixated on refer to the 
#    same external domain space.
#  - Scene previously fixated on is not entirely blind.
#  -  The Square to fixate on is outside of the Scene to fixate on's 
#    dimensions.
#
# Scenario 11: Fail
#  - Function invoked after CHREST model created.
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralSquareFixation.
#  - Fixation field of view for the Perceiver associated with the model 
#    attempting to make the PeripheralSquareFixation is > 0.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has an x coordinate set for the Square fixated on.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has a y coordinate set for the Square fixated on.
#  - Scene previously fixated on and the Scene to be fixated on refer to the 
#    same external domain space.
#  - Scene previously fixated on is not entirely blind.
#  - The Square to fixate on is inside of the Scene to fixate on's dimensions.
#  - The Square to fixate on is a blind Square.
#
# Scenario 12: Fail
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralSquareFixation.
#  - Fixation field of view for the Perceiver associated with the model 
#    attempting to make the PeripheralSquareFixation is > 0.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has an x coordinate set for the Square fixated on.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has a y coordinate set for the Square fixated on.
#  - Scene previously fixated on and the Scene to be fixated on refer to the 
#    same external domain space.
#  - Scene previously fixated on is not entirely blind.
#  - The Square to fixate on is inside of the Scene to fixate on's dimensions.
#  - The Square to fixate on is not a blind Square.
#  - The Square to fixate on contains the agent that created the Scene.
#
# Scenario 13: Pass
#  - Function invoked after CHREST model created
#  - Another Fixation has been performed before an attempt is made to make the 
#    PeripheralSquareFixation.
#  - Fixation field of view for the Perceiver associated with the model 
#    attempting to make the PeripheralSquareFixation is > 0.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has a Scene set.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has an x coordinate set for the Square fixated on.
#  - Fixation performed before the PeripheralSquareFixation is attempted to be 
#    made has a y coordinate set for the Square fixated on.
#  - Scene previously fixated on and the Scene to be fixated on refer to the 
#    same external domain space.
#  - Scene previously fixated on is not entirely blind.
#  - The Square to fixate on is inside of the Scene to fixate on's dimensions.
#  - The Square to fixate on is not a blind Square.
#  - The Square to fixate on does not contain the agent that created the 
#    Scene.
unit_test "make" do
  
  #Need to be able to set Fixation object instance variables precisely so make
  #these private instance variables accessible and writable.
  Fixation.class_eval{ 
    field_writer :_performanceTime, :_performed, :_scene, :_colFixatedOn, :_rowFixatedOn
  }
  
  Scene.class_eval{
    field_accessor :_scene
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
      model = Chrest.new(time, false)

      ###############################################
      ##### CONSTRUCT AND ADD PREVIOUS FIXATION #####
      ###############################################

      # To make a peripheral item fixation, a previous fixation needs to have been
      # made on a Scene, construct this Scene first.
      first_fixation_scene = Scene.new("", 5, 5, 0, 0, nil) #Initialised as being entirely blind

      # For scenario 9, first_fixation_scene should remain entirely blind 
      # otherwise, make it entirely empty
      if scenario != 9
        empty = Scene.getEmptySquareToken()
        for col in 0...first_fixation_scene.getWidth()
          for row in 0...first_fixation_scene.getHeight()
            first_fixation_scene._scene.get(col).set(row, SceneObject.new(empty))
          end
        end
      end

      # Construct and add a Fixation that has been performed successfully to the
      # Perceiver associated with "model" if the scenario stipulates this.
      first_fixation = CentralFixation.new(time += 100)
      first_fixation._performanceTime = (time += 50)
      first_fixation._performed = true
      first_fixation._scene = first_fixation_scene
      first_fixation._colFixatedOn = 2
      first_fixation._rowFixatedOn = 2

      # If the scenario is equal to 2, first_fixation will not have been performed.
      if scenario == 2 then first_fixation._performed = false end
      
      # If scenario is equal to 10 then the coordinates should never allow
      # the PeripheralSquareFixation to be made to return a Square that is 
      # within its dimensions.
      if scenario == 10 
        first_fixation._colFixatedOn = first_fixation_scene.getWidth() + (model.getPerceiver().getFixationFieldOfView() + 1)
        first_fixation._rowFixatedOn = first_fixation_scene.getHeight() + (model.getPerceiver().getFixationFieldOfView() + 1)
      end

      model.getPerceiver().addFixation(first_fixation)

      # Set various instance variables of "first_fixation" to nil after adding it
      # to the Perceiver's fixations (if this is done before the addition, the 
      # addition will fail for the scenario's indicated causing the test to not
      # complete).
      if(scenario == 5) then first_fixation._scene = nil end
      if(scenario == 6) then first_fixation._colFixatedOn = nil end
      if(scenario == 7) then first_fixation._rowFixatedOn = nil end

      #############################################################
      ##### CONSTRUCT AND MAKE THE PERIPHERAL-SQUARE FIXATION #####
      #############################################################

      # Construct the PeripheralSquareFixation that will be tested and set its
      # performance time.
      fixation_to_make = PeripheralSquareFixation.new(model, time += 50)
      fixation_to_make._performanceTime = (time += 100)

      # Set time to invoke PeripheralSquareFixation.make() now that the 
      # PeripheralSquareFixation has been set.  Note that, no matter what the
      # performance_time value is, if the scenario is equal to 1, the 
      # time passed as an input parameter to PeripheralSquareFixation.make() 
      # should always be earlier than the creation time of the model making the 
      # PeripheralSquareFixation.
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

      # Set the Scene to make the PeripheralSquareFixation in context of.  This 
      # should refer to the same "external domain space" as first_fixation_scene
      # in all but scenario 8.
      peripheral_square_fixation_scene = (scenario == 8 ? Scene.new("", 5, 5, 3, 3, nil) : Scene.new("", 5, 5, 0, 0, nil))

      # For scenario 9, peripheral_square_fixation_scene should remain entirely 
      # blind.
      if scenario != 9

        # For most scenarios, the Scene to make the PeripheralSquareFixation in 
        # context of should be entirely populated with non-blind SceneObjects that 
        # don't denote the agent that created the Scene except for the Square the 
        # previous fixation was made in context of (this should never be selected 
        # but, when checking that blind Squares are rejected, leaving this Square 
        # non-blind means that the "isEntirelyBlind()" check in the function will 
        # not evaluate to true so the while loop will be encountered).
        object_id = 0
        object_class = nil

        if(scenario == 11) then object_class = Scene.getBlindSquareToken() end
        if(scenario == 12) then object_class = Scene.getCreatorToken() end

        for col in 0...peripheral_square_fixation_scene.getWidth()
          for row in 0...peripheral_square_fixation_scene.getHeight()

            if col != 2 and row != 2 
              peripheral_square_fixation_scene._scene.get(col).set(row, SceneObject.new(
                object_id.to_s, 
                (object_class == nil ? 
                  (rand(1..2) == 1 ? 
                    "J" : 
                    Scene.getEmptySquareToken()
                  ) : 
                  object_class
                )
              ))
            end

            object_id+=1
          end
        end
      end

      # Set the "fixation field of view" parameter for the Perceiver associated 
      # with the model making the PeripheralSquareFixation.
      model.getPerceiver().setFixationFieldOfView(
        (scenario == 3 ? 
          -1 : 
          (scenario == 4 ?
            0 :
            2
          )
        )
      )
      
      #################
      ##### TESTS #####
      #################
      
      error_msg = "occurred when the fixation is to be performed " + 
      (performance_time == 1 ? "before" : performance_time == 2 ? "at" : "after") +
      " its defined performance time in scenario " + scenario.to_s

      # For scenarios 10 to 12, the test setup can't guarantee that the 
      # particular condition being tested is the one that causes null to be 
      # returned since the x and y displacement values may both be set to 0 
      # sometimes due to the random nature of this assignment.  So, make the
      # fixation enough times to ensure that the desired condition will be 
      # triggered.  This also ensures consistent behaviour in other scenarios.
      500.times do

        fixation = fixation_to_make.make(peripheral_square_fixation_scene, time_to_make_fixation)

        if performance_time == 1 or (performance_time != 1 and scenario != 13)
          expected_fixation = nil

          assert_equal(
            expected_fixation,
            fixation,
            error_msg
          )
        # If performance_time is != 1 and this is scenario 13, the outcome of 
        # attempting to make the PeripheralSquareFixation should never be null 
        # but may vary (any Square other than [2, 2]).  
        else
          expected_fixations = []
          for col in 0...peripheral_square_fixation_scene.getWidth()
            for row in 0...peripheral_square_fixation_scene.getHeight()
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
