unit_test "constructor" do
  AheadOfAgentFixation.class_eval{ field_accessor :_timeDecidedUpon }
  time_decided_upon = 200
  fixation = AheadOfAgentFixation.new(time_decided_upon, 0)
  
  assert_equal(fixation._timeDecidedUpon, time_decided_upon)
end

################################################################################
# Tests the "make" function using 4 scenarios:
#
# Scenario Descriptions
# =====================
# 
# - Scenario 1
#   ~ Function invoked before Fixation performance time
#   
# - Scenario 2
#   ~ Function invoked on Fixation performance time
#   ~ Scene to make fixation on does not contain agent equipped with CHREST
#
# - Scenario 3
#   ~ Function invoked on Fixation performance time
#   ~ Scene to make fixation on does contain agent equipped with CHREST
#   ~ Row ahead of agent equipped with CHREST not represented in Scene to make
#     Fixation on.
#
# - Scenario 4
#   ~ Function invoked on Fixation performance time
#   ~ Scene to make fixation on does contain agent equipped with CHREST
#   ~ Row ahead of agent equipped with CHREST is represented in Scene to make
#     Fixation on.
#
# - Scenario 5
#   ~ Function invoked *after* Fixation performance time
#   ~ Scene to make fixation on does contain agent equipped with CHREST
#   ~ Row ahead of agent equipped with CHREST not represented in Scene to make
#     Fixation on.
unit_test "make" do
  
  ################################################
  ##### SET-UP PRIVATE INSTANCE FIELD ACCESS #####
  ################################################
  
  Scene.class_eval{
    field_accessor :_scene
  }
  
  Fixation.class_eval{
    field_accessor :_performanceTime
  }
  
  #########################
  ##### SCENARIO LOOP #####
  #########################
  
  for scenario in 1..5
    
    ###########################
    ##### CONSTRUCT SCENE #####
    ###########################
    
    scene = Scene.new("", 5, (scenario == 3 ? 3 : 5), 2, 2, nil)
    if scenario != 2 then scene._scene.get(2).set(2, SceneObject.new(Scene::CREATOR_TOKEN)) end
  
    ###################################################################
    ##### CONSTRUCT AheadOfAgentFixation AND SET PERFORMANCE TIME #####
    ###################################################################
    
    time = 0
    fixation = AheadOfAgentFixation.new(time, 0)
    fixation._performanceTime = (time += 50)
    
    ####################################################
    ##### SET TIME TO MAKE AheadOfAgentFixation AT #####
    ####################################################
    
    time_to_make_fixation = (scenario == 1 ? 
      fixation._performanceTime - 1 : 
      (scenario == 5 ?
        fixation._performanceTime + 1 :
        fixation._performanceTime
      )
    )
    
    ###########################
    ##### INVOKE FUNCTION #####
    ###########################
    
    exception_thrown = false
    result = nil
    begin
      result = fixation.make(scene, time_to_make_fixation)
    rescue
      exception_thrown = true
    end
    
    #################
    ##### TESTS #####
    #################
    
    assert_equal(
      (scenario == 2 ? 
        true : 
        false
      ), 
      exception_thrown, 
      "occurred when checking if an exception is thrown in scenario " + 
      scenario.to_s
    )
    
    assert_equal(
      ([4, 5].include?(scenario) ? 
        Square.new(2, 3) : 
        nil
      ), 
      result, 
      "occurred when checking the result of making the fixation in scenario " + 
      scenario.to_s
    )
  end
end
