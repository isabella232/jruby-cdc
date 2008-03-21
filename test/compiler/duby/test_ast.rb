require 'compiler/duby'
require 'test/unit'
require 'jruby'

class TestAst < Test::Unit::TestCase
  def test_args
    node = JRuby.parse("def foo(a, b = 1, *c, &d); end")
    args_node = node.child_nodes[0].next_node.args_node
    
    new_ast = args_node.transform(nil)
    
    assert_not_nil(new_ast)
    inspected = "Arguments\n RequiredArgument(a)\n OptionalArgument(b)\n  LocalAssignment(b)\n   Fixnum(1)\n RestArgument(c)\n BlockArgument(d)"
    assert_equal(inspected, new_ast.inspect)
    
    assert(Compiler::Duby::AST::Arguments === new_ast)
    children = new_ast.children
    assert_equal(4, children.size)
    assert(Array === children[0])
    assert(Compiler::Duby::AST::RequiredArgument === children[0][0])
    assert_equal("a", children[0][0].name)
    assert_equal(new_ast, children[0][0].parent)
    assert(Array === children[1])
    assert(Compiler::Duby::AST::OptionalArgument === children[1][0])
    assert_equal("b", children[1][0].name)
    assert_equal(new_ast, children[1][0].parent)
    # TODO: should be LocalDeclaration soon
    assert(Compiler::Duby::AST::LocalAssignment === children[1][0].child)
    assert_equal(children[1][0], children[1][0].child.parent)
    assert(Compiler::Duby::AST::Fixnum, children[1][0].child.value.parent)
    assert_equal(children[1][0].child, children[1][0].child.value.parent)
    assert(Compiler::Duby::AST::RestArgument === children[2])
    assert_equal("c", children[2].name)
    assert_equal(new_ast, children[2].parent)
    assert(Compiler::Duby::AST::BlockArgument === children[3])
    assert_equal("d", children[3].name)
    assert_equal(new_ast, children[3].parent)
  end
  
  def test_locals
    node = JRuby.parse("a = 1; a")
    new_ast = node.child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert(Compiler::Duby::AST::Body === new_ast)
    inspected = "Body\n LocalAssignment(a)\n  Fixnum(1)\n Local(a)"
    assert_equal(inspected, new_ast.inspect)
    assert(!new_ast.newline)
    
    asgn = new_ast[0]
    var = new_ast[1]
    
    assert(Compiler::Duby::AST::LocalAssignment === asgn)
    assert(asgn.newline)
    assert_equal("a", asgn.name)
    assert(Compiler::Duby::AST::Fixnum === asgn.value)
    assert(!asgn.value.newline)
    assert(Compiler::Duby::AST::Local === var)
    assert(var.newline)
    assert_equal("a", var.name)
  end
  
  def test_fields
    node = JRuby.parse("@a = 1; @a")
    new_ast = node.child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert(Compiler::Duby::AST::Body === new_ast)
    inspected = "Body\n FieldAssignment(@a)\n  Fixnum(1)\n Field(@a)"
    assert_equal(inspected, new_ast.inspect)
    assert(!new_ast.newline)
    
    asgn = new_ast[0]
    var = new_ast[1]
    
    assert(Compiler::Duby::AST::FieldAssignment === asgn)
    assert(asgn.newline)
    assert_equal("@a", asgn.name)
    assert(Compiler::Duby::AST::Fixnum === asgn.value)
    assert(!asgn.value.newline)
    assert(Compiler::Duby::AST::Field === var)
    assert(var.newline)
    assert_equal("@a", var.name)
  end
  
  def test_array
    node = JRuby.parse("[a = 1, 1]")
    new_ast = node.child_nodes[0].child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert(Compiler::Duby::AST::Array === new_ast)
    assert_equal("Array\n LocalAssignment(a)\n  Fixnum(1)\n Fixnum(1)", new_ast.inspect)
    
    assert(Compiler::Duby::AST::LocalAssignment === new_ast[0])
    assert(Compiler::Duby::AST::Fixnum === new_ast[1])
  end
  
  def test_call
    node = JRuby.parse("1.foo(1)")
    new_ast = node.child_nodes[0].child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert(Compiler::Duby::AST::Call === new_ast)
    assert_equal("Call(foo)\n Fixnum(1)\n Fixnum(1)", new_ast.inspect)
    
    assert_equal("foo", new_ast.name)
    assert(Compiler::Duby::AST::Fixnum === new_ast.target)
    assert_not_nil(new_ast.parameters)
    assert_equal(1, new_ast.parameters.size)
    assert(Compiler::Duby::AST::Fixnum === new_ast.parameters[0])
  end
  
  def test_fcall
    node = JRuby.parse("foo(1)")
    new_ast = node.child_nodes[0].child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert(Compiler::Duby::AST::FunctionalCall === new_ast)
    assert_equal("FunctionalCall(foo)\n Fixnum(1)", new_ast.inspect)
    
    assert_equal("foo", new_ast.name)
    assert_not_nil(new_ast.parameters)
    assert_equal(1, new_ast.parameters.size)
    assert(Compiler::Duby::AST::Fixnum === new_ast.parameters[0])
  end
  
  def test_if
    node = JRuby.parse("if 1; 2; elsif !3; 4; else; 5; end")
    new_ast = node.child_nodes[0].child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert(Compiler::Duby::AST::If === new_ast)
    assert_equal("If\n Condition\n  Fixnum(1)\n Fixnum(2)\n If\n  Condition\n   Not\n    Fixnum(3)\n  Fixnum(4)\n  Fixnum(5)", new_ast.inspect)
    
    assert(Compiler::Duby::AST::Condition === new_ast.condition)
    assert(Compiler::Duby::AST::Fixnum === new_ast.condition.predicate)
    assert(Compiler::Duby::AST::Fixnum === new_ast.body)
    assert(Compiler::Duby::AST::If === new_ast.else)
    assert(Compiler::Duby::AST::Condition === new_ast.else.condition)
    assert(Compiler::Duby::AST::Not === new_ast.else.condition.predicate)
    assert(Compiler::Duby::AST::Fixnum === new_ast.else.body)
    assert(Compiler::Duby::AST::Fixnum === new_ast.else.else)
  end
  
  def test_begin
    node = JRuby.parse("begin; 1; 2; end")
    new_ast = node.child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert_equal("Body\n Fixnum(1)\n Fixnum(2)", new_ast.inspect)
    assert(Compiler::Duby::AST::Body === new_ast)
    assert(Compiler::Duby::AST::Fixnum === new_ast[0])
    
    node = JRuby.parse("begin; 1; end")
    new_ast = node.child_nodes[0].transform(nil)
    assert(Compiler::Duby::AST::Fixnum === new_ast)
  end
  
  def test_block
    node = JRuby.parse("1; 2")
    new_ast = node.child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert_equal("Body\n Fixnum(1)\n Fixnum(2)", new_ast.inspect)
    assert(Compiler::Duby::AST::Body === new_ast)
    assert(Compiler::Duby::AST::Fixnum === new_ast[0])
    
    node = JRuby.parse("1")
    new_ast = node.child_nodes[0].transform(nil)
    assert(Compiler::Duby::AST::Fixnum === new_ast)
  end
  
  def test_fixnum
    node = JRuby.parse("1")
    
    new_ast = node.child_nodes[0].transform(nil)
    assert_not_nil(new_ast)
    assert_equal("Fixnum(1)", new_ast.inspect)
    assert(Compiler::Duby::AST::Fixnum === new_ast)
    assert_equal(1, new_ast.literal)
  end
  
  def test_float
    node = JRuby.parse("1.0")
    
    new_ast = node.child_nodes[0].transform(nil)
    assert_not_nil(new_ast)
    assert_equal("Float(1.0)", new_ast.inspect)
    assert(Compiler::Duby::AST::Float === new_ast)
    assert_equal(1.0, new_ast.literal)
  end
  
  def test_class
    node = JRuby.parse("class Foo < Bar; 1; 2; end")
    new_ast = node.child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert_equal("ClassDefinition(Foo)\n Constant(Bar)\n Body\n  Fixnum(1)\n  Fixnum(2)", new_ast.inspect)
    assert(Compiler::Duby::AST::ClassDefinition === new_ast)
    assert_equal("Foo", new_ast.name)
    
    assert(Compiler::Duby::AST::Constant === new_ast.superclass)
    assert(Compiler::Duby::AST::Body === new_ast.body)
    assert(Compiler::Duby::AST::Fixnum === new_ast.body[0])
  end
  
  def test_defn
    node = JRuby.parse("def foo(a, b); 1; end")
    new_ast = node.child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert_equal("MethodDefinition(foo)\n VoidType\n Arguments\n  RequiredArgument(a)\n  RequiredArgument(b)\n Fixnum(1)", new_ast.inspect)
    assert(Compiler::Duby::AST::MethodDefinition === new_ast)
    assert_equal("foo", new_ast.name)
    assert_not_nil(new_ast.signature)
    assert_equal(1, new_ast.signature.size)
    assert(Compiler::Duby::AST::VoidType === new_ast.signature[0])
    assert(Compiler::Duby::AST::Arguments === new_ast.arguments)
    assert(Compiler::Duby::AST::Fixnum === new_ast.body)
  end
  
  def test_defs
    node = JRuby.parse("def self.foo(a, b); 1; end")
    new_ast = node.child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert_equal("StaticMethodDefinition(foo)\n VoidType\n Arguments\n  RequiredArgument(a)\n  RequiredArgument(b)\n Fixnum(1)", new_ast.inspect)
    assert(Compiler::Duby::AST::StaticMethodDefinition === new_ast)
    assert_equal("foo", new_ast.name)
    assert_not_nil(new_ast.signature)
    assert_equal(1, new_ast.signature.size)
    assert(Compiler::Duby::AST::VoidType === new_ast.signature[0])
    assert(Compiler::Duby::AST::Arguments === new_ast.arguments)
    assert(Compiler::Duby::AST::Fixnum === new_ast.body)
  end
  
  def test_signature
    node = JRuby.parse("def self.foo(a, b); {a => :foo, b => :bar, :return => :baz}; 1; end")
    new_ast = node.child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast.signature)
    inspected = "StaticMethodDefinition(foo)\n TypeReference(baz, array = false)\n TypeReference(foo, array = false)\n TypeReference(bar, array = false)\n Arguments\n  RequiredArgument(a)\n  RequiredArgument(b)\n Body\n  Noop\n  Fixnum(1)"
    assert_equal(inspected, new_ast.inspect)
    signature = new_ast.signature
    assert_equal(3, signature.size)
    assert(Compiler::Duby::AST::TypeReference === signature[0])
    assert_equal("baz", signature[0].name)
    assert(Compiler::Duby::AST::TypeReference === signature[1])
    assert_equal("foo", signature[1].name)
    assert(Compiler::Duby::AST::TypeReference === signature[2])
    assert_equal("bar", signature[2].name)
  end
  
  def test_type_reference
    node = JRuby.parse("{a => :foo, b => java.lang.Object, :return => ArrayList}")
    signature = node.child_nodes[0].signature(nil)
    
    inspected = "[TypeReference(ArrayList, array = false), TypeReference(foo, array = false), TypeReference(java.lang.Object, array = false)]"
    assert_equal(inspected, signature.inspect)
    assert_equal(3, signature.size)
    assert(Compiler::Duby::AST::TypeReference === signature[0])
    assert_equal("ArrayList", signature[0].name)
    assert(Compiler::Duby::AST::TypeReference === signature[1])
    assert_equal("foo", signature[1].name)
    assert(Compiler::Duby::AST::TypeReference === signature[2])
    assert_equal("java.lang.Object", signature[2].name)
  end
  
  def test_return
    node = JRuby.parse("return 1")
    new_ast = node.child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    inspected = "Return\n Fixnum(1)"
    assert_equal(inspected, new_ast.inspect)
    assert(Compiler::Duby::AST::Return === new_ast)
    assert(Compiler::Duby::AST::Fixnum === new_ast.value)
  end
  
  def test_vcall
    node = JRuby.parse("foo")
    new_ast = node.child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert(Compiler::Duby::AST::FunctionalCall === new_ast)
    assert_equal("FunctionalCall(foo)", new_ast.inspect)
    
    assert_equal("foo", new_ast.name)
    assert_not_nil(new_ast.parameters)
    assert_equal(0, new_ast.parameters.size)
  end
  
  def test_while
    node = JRuby.parse("while 1; 2; end")
    new_ast = node.child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert(Compiler::Duby::AST::Loop === new_ast)
    assert_equal("Loop(check_first = true, negative = false)\n Condition\n  Fixnum(1)\n Fixnum(2)", new_ast.inspect)
    assert(new_ast.check_first?)
    assert(!new_ast.negative?)
    assert(Compiler::Duby::AST::Condition === new_ast.condition)
    assert(Compiler::Duby::AST::Fixnum === new_ast.condition.predicate)
    assert(Compiler::Duby::AST::Fixnum === new_ast.body)
    
    node = JRuby.parse("begin; 2; end while 1")
    new_ast = node.child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert(Compiler::Duby::AST::Loop === new_ast)
    assert_equal("Loop(check_first = false, negative = false)\n Condition\n  Fixnum(1)\n Fixnum(2)", new_ast.inspect)
    assert(!new_ast.check_first?)
    assert(!new_ast.negative?)
    assert(Compiler::Duby::AST::Condition === new_ast.condition)
    assert(Compiler::Duby::AST::Fixnum === new_ast.condition.predicate)
    assert(Compiler::Duby::AST::Fixnum === new_ast.body)
  end
  
  def test_until
    node = JRuby.parse("until 1; 2; end")
    new_ast = node.child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert(Compiler::Duby::AST::Loop === new_ast)
    assert_equal("Loop(check_first = true, negative = true)\n Condition\n  Fixnum(1)\n Fixnum(2)", new_ast.inspect)
    assert(new_ast.check_first?)
    assert(new_ast.negative?)
    assert(Compiler::Duby::AST::Condition === new_ast.condition)
    assert(Compiler::Duby::AST::Fixnum === new_ast.condition.predicate)
    assert(Compiler::Duby::AST::Fixnum === new_ast.body)
    
    node = JRuby.parse("begin; 2; end until 1")
    new_ast = node.child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert(Compiler::Duby::AST::Loop === new_ast)
    assert_equal("Loop(check_first = false, negative = true)\n Condition\n  Fixnum(1)\n Fixnum(2)", new_ast.inspect)
    assert(!new_ast.check_first?)
    assert(new_ast.negative?)
    assert(Compiler::Duby::AST::Condition === new_ast.condition)
    assert(Compiler::Duby::AST::Fixnum === new_ast.condition.predicate)
    assert(Compiler::Duby::AST::Fixnum === new_ast.body)
  end
  
  def test_string
    node = JRuby.parse("'foo'")
    new_ast = node.child_nodes[0].transform(nil)
    
    assert_not_nil(new_ast)
    assert(Compiler::Duby::AST::String === new_ast)
    assert_equal("String(\"foo\")", new_ast.inspect)
    assert_equal("foo", new_ast.literal)
  end
  
  def test_root
    node = JRuby.parse("1")
    new_ast = node.transform(nil)
    
    assert_not_nil(new_ast)
    assert(Compiler::Duby::AST::Fixnum === new_ast)
  end
end