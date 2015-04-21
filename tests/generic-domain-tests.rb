unit_test "normalise" do
  #Item square patterns are handled specially by the GenericDomain.normalise() 
  #function hence the verbose variable names.
  empty_item_square_pattern = ItemSquarePattern.new(Scene.getEmptySquareIdentifier, 0, 1)
  self_item_square_pattern = ItemSquarePattern.new(Scene.getSelfIdentifier, 0, 2)
  non_empty_item_square_pattern = ItemSquarePattern.new("A", 0, 3)
  duplicate_non_empty_item_square_pattern = ItemSquarePattern.new("A", 0, 3)
  
  generic_domain = GenericDomain.new
  
  #The normalised ListPattern returned by the GenericDomain.normalise() function
  #should always be equal to the modality of the ListPattern that is to be 
  #normalised.
  for modality in Modality.values() do
    
    #Empty square and self identifiers should be ignored by the 
    #GenericDomain.normalise method if they are of type StringPattern.
    string_pattern_one = Pattern.makeString(Scene.getEmptySquareIdentifier())
    string_pattern_two = Pattern.makeString(Scene.getSelfIdentifier())
    string_list_pattern = ListPattern.new(modality)
    string_list_pattern.add(string_pattern_one)
    string_list_pattern.add(string_pattern_two)
    
    number_first = Pattern.makeNumber(123)
    number_second = Pattern.makeNumber(456)
    number_list_pattern = ListPattern.new(modality)
    number_list_pattern.add(number_first)
    number_list_pattern.add(number_second)
    
    item_square_list_pattern = ListPattern.new(modality)
    item_square_list_pattern.add(empty_item_square_pattern)
    item_square_list_pattern.add(self_item_square_pattern)
    item_square_list_pattern.add(non_empty_item_square_pattern)
    item_square_list_pattern.add(duplicate_non_empty_item_square_pattern)
    
    #Pass list patterns to the method.
    normalised_string_pattern = generic_domain.normalise(string_list_pattern)
    normalised_number_pattern = generic_domain.normalise(number_list_pattern)
    normalised_item_square_pattern = generic_domain.normalise(item_square_list_pattern)
    
    #Original string ListPattern should remain unaltered.
    assert_equal(2, normalised_string_pattern.size())
    assert_equal(modality, normalised_string_pattern.getModality())
    assert_equal(string_list_pattern.toString(), normalised_string_pattern.toString())
    
    #Original number ListPattern should remain unaltered.
    assert_equal(2, normalised_number_pattern.size())
    assert_equal(modality, normalised_number_pattern.getModality())
    assert_equal(number_list_pattern.toString(), normalised_number_pattern.toString())
    
    #Original item square ListPattern should be altered i.e. only 1 
    #PrimitivePattern should be in the returned ListPattern.
    assert_equal(1, normalised_item_square_pattern.size(), "occurred when checking the size of the normalised item-square pattern")
    assert_equal(modality, normalised_item_square_pattern.getModality(), "occurred when checking the modality of the normalised item-square pattern")
    assert_equal(non_empty_item_square_pattern, normalised_item_square_pattern.getItem(0), "occurred when checking the contents of the normalised item-square pattern")
  end
end
