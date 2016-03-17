unit_test "constructor" do
  
  # Need to be able to read the value of the private "_model" SalientManFixation 
  # instance variable to ensure the constructor operates correctly.
  SalientManFixation.class_eval{
    field_reader :_model
  }
  
  ########################################
  ##### CONSTRUCT MODEL AND FIXATION #####
  ########################################
  time = 0
  model = Chrest.new(time += 50, false)
  
  time + 50
  fixation = SalientManFixation.new(model, time)
  
  #################
  ##### TESTS #####
  #################
  assert_equal(
    time + 150,
    fixation.getTimeDecidedUpon(),
    "occurred when checking the time the fixation is decided upon"
  )
  
  assert_equal(
    model,
    fixation._model,
    "occurred when checking the model the fixation is associated with"
  )
end

################################################################################
# To test SalientManFixation.make() the following scenarios are repeated 3 times
# with the time the function is invoked differing:
# 
# Repeat 1: "make()" invoked before SalientManFixation performance time.
# Repeat 2: "make()" invoked at SalientManFixation performance time.
# Repeat 3: "make()" invoked after SalientManFixation performance time.
# 
# For repeat 1, all scenarios should fail (return nil).  For repeats 2 and 3, 
# all scenarios except the last two should fail.  The scenarios run are 
# described below:
#   
# Scenario 1: Fail
#   - Scene passed as input parameter is entirely blind
#     
# Scenario 2: Fail
#   - Scene passed as input parameter is not entirely blind
#   - Scene passed as input parameter is not a ChessBoard instance
#
# In the next four scenarios, the scene used is the same but the model will be
# experienced/inexperienced.  This will ensure that the correct method is called
# to determine salient pieces when the model is/is not experienced (determined 
# in the final two scenarios).
# 
# Scenario 3: Fail
#   - Scene passed as input parameter is not entirely blind
#   - Scene passed as input parameter is a ChessBoard instance
#   - Model making the SalientManFixation is not experienced
#   - Scene passed as input parameter should not return any Squares to fixate on
#
# Scenario 4: Fail
#   - Scene passed as input parameter is not entirely blind
#   - Scene passed as input parameter is a ChessBoard instance
#   - Model making the SalientManFixation is experienced
#   - Scene passed as input parameter should not return any Squares to fixate on
#
# Scenario 5: Pass (if performed in context of repeat 2/3 otherwise, fail)
#   - Scene passed as input parameter is not entirely blind
#   - Scene passed as input parameter is a ChessBoard instance
#   - Model making the SalientManFixation is not experienced
#   - Scene passed as input parameter should contain suitable Squares to fixate 
#     on
#
# Scenario 6: Pass (if performed in context of repeat 2/3 otherwise, fail)
#   - Scene passed as input parameter is not entirely blind
#   - Scene passed as input parameter is a ChessBoard instance
#   - Model making the SalientManFixation is experienced
#   - Scene passed as input parameter should contain suitable Squares to fixate 
#     on
unit_test "make" do
  
  for invocation_time in 1..3
    for scenario in 1..6
      
      ########################################
      ##### CONSTRUCT MODEL AND FIXATION #####
      ########################################

      time = 0

      # Need to be able to specify if a CHREST model is experienced "on-the-fly"
      # otherwise, the model would have to have a certain number of Nodes, n, in 
      # LTM to return "true" when the model's "experienced" status is queried 
      # to determine what strategy to use to make the SalientManFixation.  Thus, if 
      # n is changed this test will break and performing this learning in a test 
      # causes unnecessary issues to deal with.
      #
      # To circumvent this, subclass the "Chrest" Java class with a Ruby class 
      # that will be used in place of the "Chrest" Java class in this test. In 
      # the Ruby subclass, override "Chrest.isExperienced()" (the method used to 
      # determine the "experienced" status of a CHREST model) and have it return 
      # a class variable (for the subclass) that can be set at will.
      model = Class.new(Chrest) {
        @@experienced = false

        def isExperienced(x)
          return @@experienced
        end

        def setExperienced(bool)
          @@experienced = bool
        end
      }.new(time, false)
      
      if [4,6].include?(scenario) then model.setExperienced(true) end

      # Construct SalientManFixation
      time + 50
      fixation = SalientManFixation.new(model, time)

      fixation_performance_time = fixation.getTimeDecidedUpon + 100
      fixation.setPerformanceTime(fixation_performance_time)
      time = fixation_performance_time

      ########################################
      ##### SET "make()" INVOCATION TIME #####
      ########################################
      
      make_invocation_time = 
        (invocation_time == 1 ? 
          fixation_performance_time -= 10 : 
          (invocation_time == 2 ?
            fixation_performance_time :
            fixation_performance_time += 10  
          )
        )
        
      ###############################################
      ##### CONSTRUCT SCENE TO MAKE FIXATION ON #####
      ###############################################
      
      scene = nil
      if scenario == 2
        scene = Scene.new("", 8, 8, 1, 1, nil)
      elsif scenario == 3 || scenario == 4
        
        # The "scene" should be a chess board but that will return no Square to
        # fixate on because it doesn't contain any big or offensive pieces.
        # An empty chess board will meet this criteria so construct one and set
        # it to "scene".
        scene = 
          "......../" +
          "......../" +
          "......../" +
          "......../" +
          "......../" +
          "......../" +
          "......../" +
          "........"
      else
        
        # The "scene" should contain big and offensive pieces.
        scene = 
          "rnbqkbnr/" +
          "pp.ppppp/" +
          ".....P../" +
          "......../" +
          "..p...../" +
          "......../" +
          "PPPPP.PP/" +
          "RNBQKBNR"
      end
      
      if scene.is_a? String then scene = ChessDomain.constructBoard(scene) end
      
      if scenario == 1
        for col in 0...scene.getWidth()
          for row in 0...scene.getHeight()
            scene.addItemToSquare(col, row, Scene.getBlindSquareToken(), Scene.getBlindSquareToken())
          end
        end
      end
      
      ####################################################
      ##### SET EXPECTED FIXATION MADE AND RUN TESTS #####
      ####################################################
      
      expected_fixations = [nil]
      
      if invocation_time != 1
        if scenario == 5
          expected_fixations = []
          
          for col in 0...scene.getWidth()  
            expected_fixations.push(Square.new(col, 0))
            expected_fixations.push(Square.new(col, 7))
          end

          expected_fixations.map!{|x| x.toString()}
        elsif scenario == 6
          expected_fixations = []
          expected_fixations.push(Square.new(2, 3))
          expected_fixations.push(Square.new(5, 5))

          expected_fixations.map!{|x| x.toString()}
        end
      end
      
      # Repeat the test 500 times to ensure that consistent output is generated
      # and that all possible Fixations are accounted for.
      500.times do
        result = fixation.make(scene, make_invocation_time)
        if result != nil then result = result.toString() end
        
        assert_true(
          expected_fixations.include?(result),
          "occurred in scenario " + scenario.to_s + " when invoking 'make' " +
          (invocation_time == 1 ? "before" : invocation_time == 2 ? "at" : 
          "after") + " the Fixation's performance time; '" + (result == nil ?
          "null" : result.to_s) + "' is not expected"
        )
      end
    end
  end
end
