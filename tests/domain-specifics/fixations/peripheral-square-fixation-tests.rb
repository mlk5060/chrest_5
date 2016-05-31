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
# To test "PeripheralItemFixation.make()", a number of scenarios are setup and 
# run 3 times with different times to invoke the function (before, at and after 
# the performance time of a PeripheralSquareFixation).  These times will usually 
# be greater than the CHREST model's creation time (see scenario 1 note).
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
#  - The Square to fixate on is outside of the Scene to fixate on's 
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
#  - No valid Squares to fixate on.
#
# Scenario 12: Pass
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
#  - There is one valid Square in the Scene, to the south-west of the Square
#    previously fixated on (checks that the method uses negative column and row
#    displacements).
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
#  - There is one valid Square in the Scene, to the north-east of the Square
#    previously fixated on (checks that the method uses positive column and row
#    displacements).
unit_test "make" do
  
  #Need to be able to set Fixation object instance variables precisely so make
  #these private instance variables accessible and writable.
  Fixation.class_eval{ 
    field_accessor :_performanceTime, :_performed, :_scene, :_colFixatedOn, :_rowFixatedOn
  }
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  chrest_perceiver_field = Chrest.java_class.declared_field("_perceiver")
  chrest_perceiver_field.accessible = true
  
  Perceiver.class_eval{
    field_accessor :_fixationFieldOfView
  }
  
  perceiver_fixations_field = Perceiver.java_class.declared_field("_fixations")
  perceiver_fixations_field.accessible = true
  
  for performance_time in 1..3
    for scenario in 1..13
      100.times do

        ##################################
        ##### CONSTRUCT CHREST MODEL #####
        ##################################

        # Set the time that the CHREST model will be created to a value > 0 so 
        # that, if the "scenario" variable is = 1, the time the "make" function is
        # invoked can be less than this time but not be a negative value.
        time = 5
        model_creation_time = time
        model_learning_object_locations_relative_to_self = [true,false].sample
        model = Chrest.new(time, model_learning_object_locations_relative_to_self)
        
        # Set the "fixation field of view" parameter for the Perceiver associated 
        # with the model making the PeripheralSquareFixation.
        chrest_perceiver_field.value(model)._fixationFieldOfView = 
          scenario == 3 ? -1 : 
          scenario == 4 ? 0 :
          2

        ###############################################################
        ##### CONSTRUCT PREVIOUS FIXATION AND SCENE IT FIXATED ON #####
        ###############################################################

        # To make a peripheral item fixation, a previous fixation needs to have been
        # made on a Scene, construct this Scene first.
        previous_fixation_scene = Scene.new("", 5, 5, 0, 0, nil) #Initialised as being entirely blind

        # For scenario 9, first_fixation_scene should remain entirely blind 
        # otherwise, make it entirely empty.
        if scenario != 9
          for col in 0...(previous_fixation_scene._scene.size())
            for row in 0...(previous_fixation_scene._scene.get(col).size())
              previous_fixation_scene._scene.get(col).set(row, SceneObject.new(Scene::EMPTY_SQUARE_TOKEN))
            end
          end
        end
        
        # If the model is learning object locations relative to itself, encode 
        # the creator on the Square appropriately.
        if model_learning_object_locations_relative_to_self
          previous_fixation_scene._scene.get(2).set(1, SceneObject.new(Scene::CREATOR_TOKEN))
        end

        # Construct previous Fixation
        previous_fixation = nil
        time_previous_fixation_decided_upon = time += 100
        
        previous_fixation = (
          model_learning_object_locations_relative_to_self ? 
            AheadOfAgentFixation.new(time_previous_fixation_decided_upon) :
            CentralFixation.new(time_previous_fixation_decided_upon)
        )
        
        previous_fixation._performanceTime = (time += 50)
        previous_fixation._performed = true
        previous_fixation._scene = previous_fixation_scene
        previous_fixation._colFixatedOn = 2
        previous_fixation._rowFixatedOn = 2

        #  Set various instance variables of the previous fixation to nil 
        #  depending on the current scenario
        if scenario == 2 then previous_fixation._performed = false end
        if scenario == 5 then previous_fixation._scene = nil end
        if scenario == 6 then previous_fixation._colFixatedOn = nil end
        if scenario == 7 then previous_fixation._rowFixatedOn = nil end

        # If scenario is equal to 10 then the coordinates should never allow
        # the PeripheralSquareFixation to be made to return a Square that is 
        # within its dimensions.
        if scenario == 10 
          previous_fixation._colFixatedOn = previous_fixation_scene.getWidth() + (chrest_perceiver_field.value(model)._fixationFieldOfView + 1)
          previous_fixation._rowFixatedOn = previous_fixation_scene.getHeight() + (chrest_perceiver_field.value(model)._fixationFieldOfView + 1)
        end

        ########################################################
        ##### ADD PREVIOUS FIXATION TO PERCEIVER FIXATIONS #####
        ########################################################

        fixations = ArrayList.new()
        fixations.add(previous_fixation)

        fixations_history = HistoryTreeMap.new()
        fixations_history.put(previous_fixation._performanceTime.to_java(:int), fixations)

        perceiver_fixations_field.set_value(chrest_perceiver_field.value(model), fixations_history)

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
              fixation_to_make._performanceTime - 1 :
              (performance_time == 2 ?
                fixation_to_make._performanceTime :
                fixation_to_make._performanceTime + 1
              )
            )
          )
          
        ########################################
        ##### CONSTRUCT SCENE TO FIXATE ON #####
        ########################################
        
        # Scene to fixate on should refer to the same domain space as the Scene
        # the previous Fixation fixates on in each Scenario except 8.
        scene_to_fixate_on = (scenario == 8 ? Scene.new("", 5, 5, 3, 3, nil) : Scene.new("", 5, 5, 0, 0, nil))
        
        # Place a SceneObject on the Square to ensure that the method should 
        # return a Fixation, if the Scenario stipulates that this should occur
        # (any other Fixation other than 11).  Therefore, in Scenario 11, the
        # Scene will be blind (other than the creator which is added below if 
        # the model is learning object locations relative to itself, in which 
        # case it is still regarded as being blind) meaning that no valid 
        # Squares will be found.
        #
        # The SceneObject will be placed to the south-west of the Square that 
        # the previous Fixation fixated on, unless this is Scenario 13, in which
        # case, it will be placed to the north-east.
        #
        # NOTE: in Scenario 10, the Square previously fixated on is outside the 
        #       scope of the Scene to fixate on so tring to add a SceneObject to
        #       coordinates based on these will cause an index error. Similarly,
        #       if the column/row fixated on by the previous Fixation is set to
        #       nil (as it is in certain Scenarios), its not possible to 
        #       determine where to place the SceneObject based on the previous 
        #       Fixation's data.  In either case, by default, place the 
        #       SceneObject on the most south-western Square in the Scene to 
        #       fixate on and, in scenario 13, place it on the most 
        #       north-eastern Square.
        if scenario != 11
          scene_object = SceneObject.new("0", ([1,2].sample == 1 ? "J" : Scene::EMPTY_SQUARE_TOKEN))
          col = ( (scenario == 10) || (previous_fixation._colFixatedOn == nil) ? 0 : previous_fixation._colFixatedOn - 1)
          row = ( (scenario == 10) || (previous_fixation._rowFixatedOn == nil) ? 0 : previous_fixation._rowFixatedOn - 1)
          
          if scenario == 13 
            col = (previous_fixation._colFixatedOn == nil ? 2 : previous_fixation._colFixatedOn + 1)
            row = (previous_fixation._rowFixatedOn == nil ? 2 : previous_fixation._rowFixatedOn + 1)
          end
          
          scene_to_fixate_on._scene.get(col).set(row, scene_object)
        end
        
        # Encode the creator on the Scene if the model is learning object 
        # locations relative to itself.
        if model_learning_object_locations_relative_to_self 
          scene_to_fixate_on._scene.get(2).set(1, SceneObject.new(Scene::CREATOR_TOKEN))
        end

        #################
        ##### TESTS #####
        #################

        error_msg = "occurred when the fixation is to be performed " + 
        (performance_time == 1 ? "before" : performance_time == 2 ? "at" : "after") +
        " its defined performance time in scenario " + scenario.to_s + " when the " +
        "model is " + (model_learning_object_locations_relative_to_self ? "" : "not") +
        " learning object locations relative to itself"

        fixation = fixation_to_make.make(scene_to_fixate_on, time_to_make_fixation)

        expected_fixation = nil
        
        # If performance_time is != 1 and this is scenario 12/13, the outcome of 
        # attempting to make the PeripheralSquareFixation should never be null
        if performance_time != 1 
          if scenario == 12 then expected_fixation = Square.new(previous_fixation._colFixatedOn - 1, previous_fixation._rowFixatedOn - 1) end 
          if scenario == 13 then expected_fixation = Square.new(previous_fixation._colFixatedOn + 1, previous_fixation._rowFixatedOn + 1) end
          
          assert_equal(
            expected_fixation,
            fixation,
            error_msg
          )
        end
      end
    end
  end
end
