/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.compiler;

import java.util.List;
import java.util.Map;
import org.jruby.ast.NodeType;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.util.ByteList;

/**
 *
 * @author headius
 */
public interface BodyCompiler {
    /**
     * End compilation for the method associated with the specified token. This should
     * close out all structures created for compilation of the method.
     * 
     * @param token A token identifying the method to be terminated.
     */
    public void endBody();
    
    /**
     * As code executes, values are assumed to be "generated", often by being pushed
     * on to some execution stack. Generally, these values are consumed by other
     * methods on the context, but occasionally a value must be "thrown out". This method
     * provides a way to discard the previous value generated by some other call(s).
     */
    public void consumeCurrentValue();
    
    /**
     * Push a copy the topmost value on the stack.
     */
    public void duplicateCurrentValue();
    
    /**
     * For logging, println the object reference currently atop the stack
     */
    public void aprintln();
    
    /**
     * Swap the top and second values on the stack.
     */
    public void swapValues();

    /**
     * Reverse the top n values on the stack.
     *
     * @param n The number of values to reverse.
     */
    public void reverseValues(int n);
    
    /**
     * This method provides a way to specify a line number for the current piece of code
     * being compiled. The compiler may use this information to create debugging
     * information in a bytecode-format-dependent way.
     * 
     * @param position The ISourcePosition information to use.
     */
    public void lineNumber(ISourcePosition position);
    
    public VariableCompiler getVariableCompiler();
    
    public InvocationCompiler getInvocationCompiler();
    
    /**
     * Retrieve the current "self" and put a reference on top of the stack.
     */
    public void retrieveSelf();
    
    /**
     * Retrieve the current "self" object's metaclass and put a reference on top of the stack
     */
    public void retrieveSelfClass();
    
    public void retrieveClassVariable(String name);
    
    public void assignClassVariable(String name);
    
    public void assignClassVariable(String name, CompilerCallback value);
    
    public void declareClassVariable(String name);
    
    public void declareClassVariable(String name, CompilerCallback value);
    
    /**
     * Generate a new "Fixnum" value.
     */
    public void createNewFixnum(long value);

    /**
     * Generate a new "Float" value.
     */
    public void createNewFloat(double value);

    /**
     * Generate a new "Bignum" value.
     */
    public void createNewBignum(java.math.BigInteger value);
    
    /**
     * Generate a new "String" value.
     */
    public void createNewString(ByteList value);

    /**
     * Generate a new dynamic "String" value.
     */
    public void createNewString(ArrayCallback callback, int count);
    public void createNewSymbol(ArrayCallback callback, int count);

    /**
     * Generate a new "Symbol" value (or fetch the existing one).
     */
    public void createNewSymbol(String name);
    
    public void createObjectArray(Object[] elementArray, ArrayCallback callback);

    /**
     * Combine the top <pre>elementCount</pre> elements into a single element, generally
     * an array or similar construct. The specified number of elements are consumed and
     * an aggregate element remains.
     * 
     * @param elementCount The number of elements to consume
     */
    public void createObjectArray(int elementCount);

    /**
     * Given an aggregated set of objects (likely created through a call to createObjectArray)
     * create a Ruby array object.
     */
    public void createNewArray(boolean lightweight);

    /**
     * Given an aggregated set of objects (likely created through a call to createObjectArray)
     * create a Ruby array object. This version accepts an array of objects
     * to feed to an ArrayCallback to construct the elements of the array.
     */
    public void createNewArray(Object[] sourceArray, ArrayCallback callback, boolean lightweight);

    /**
     * Create an empty Ruby array
     */
    public void createEmptyArray();
    
    /**
     * Create an empty Ruby Hash object and put a reference on top of the stack.
     */
    public void createEmptyHash();
    
    /**
     * Create a new hash by calling back to the specified ArrayCallback. It is expected that the keyCount
     * will be the actual count of key/value pairs, and the caller will handle passing an appropriate elements
     * collection in and dealing with the sequential indices passed to the callback.
     * 
     * @param elements An object holding the elements from which to create the Hash.
     * @param callback An ArrayCallback implementation to which the elements array and iteration counts
     * are passed in sequence.
     * @param keyCount the total count of key-value pairs to be constructed from the elements collection.
     */
    public void createNewHash(Object elements, ArrayCallback callback, int keyCount);
    
    /**
     * Create a new range. It is expected that the stack will contain the end and begin values for the range as
     * its topmost and second topmost elements.
     * 
     * @param isExclusive Whether the range is exclusive or not (inclusive)
     */
    public void createNewRange(CompilerCallback beginEndCalback, boolean isExclusive);

    /**
     * Create a new literal lambda. The stack should contain a reference to the closure object.
     */
    public void createNewLambda(CompilerCallback closure);
    
    /**
     * Perform a boolean branch operation based on the Ruby "true" value of the top value
     * on the stack. If Ruby "true", invoke the true branch callback. Otherwise, invoke the false branch callback.
     * 
     * @param trueBranch The callback for generating code for the "true" condition
     * @param falseBranch The callback for generating code for the "false" condition
     */
    public void performBooleanBranch(BranchCallback trueBranch, BranchCallback falseBranch);
    
    /**
     * Perform a logical short-circuited Ruby "and" operation, using Ruby notions of true and false.
     * If the value on top of the stack is false, it remains and the branch is not executed. If it is true,
     * the top of the stack is replaced with the result of the branch.
     * 
     * @param longBranch The branch to execute if the "and" operation does not short-circuit.
     */
    public void performLogicalAnd(BranchCallback longBranch);
    
    
    /**
     * Perform a logical short-circuited Ruby "or" operation, using Ruby notions of true and false.
     * If the value on top of the stack is true, it remains and the branch is not executed. If it is false,
     * the top of the stack is replaced with the result of the branch.
     * 
     * @param longBranch The branch to execute if the "or" operation does not short-circuit.
     */
    public void performLogicalOr(BranchCallback longBranch);
    
    /**
     * Perform a boolean loop using the given condition-calculating branch and body branch. For
     * while loops, pass true for checkFirst. For statement-modifier while loops, pass false. For
     * unless loops, reverse the result of the condition after calculating it.
     * 
     * This version ensures the stack is maintained so while results can be used in any context.
     * 
     * @param condition The code to execute for calculating the loop condition. A Ruby true result will
     * cause the body to be executed again.
     * @param body The body to executed for the loop.
     * @param checkFirst whether to check the condition the first time through or not.
     */
    public void performBooleanLoopSafe(BranchCallback condition, BranchCallback body, boolean checkFirst);
    
    /**
     * Perform a boolean loop using the given condition-calculating branch and body branch. For
     * while loops, pass true for checkFirst. For statement-modifier while loops, pass false. For
     * unless loops, reverse the result of the condition after calculating it.
     * 
     * @param condition The code to execute for calculating the loop condition. A Ruby true result will
     * cause the body to be executed again.
     * @param body The body to executed for the loop.
     * @param checkFirst whether to check the condition the first time through or not.
     */
    public void performBooleanLoop(BranchCallback condition, BranchCallback body, boolean checkFirst);
    
    /**
     * Perform a boolean loop using the given condition-calculating branch and body branch. For
     * while loops, pass true for checkFirst. For statement-modifier while loops, pass false. For
     * unless loops, reverse the result of the condition after calculating it.
     * 
     * This version does not handle non-local flow control which can bubble out of
     * eval or closures, and only expects normal flow control to be used within
     * its body.
     * 
     * @param condition The code to execute for calculating the loop condition. A Ruby true result will
     * cause the body to be executed again.
     * @param body The body to executed for the loop.
     * @param checkFirst whether to check the condition the first time through or not.
     */
    public void performBooleanLoopLight(BranchCallback condition, BranchCallback body, boolean checkFirst);
    
    /**
     * Return the current value on the top of the stack, taking into consideration surrounding blocks.
     */
    public void performReturn();

    /**
     * Create a new closure (block) using the given lexical scope information, call arity, and
     * body generated by the body callback. The closure will capture containing scopes and related information.
     *
     * @param scope The static scoping information
     * @param arity The arity of the block's argument list
     * @param body The callback which will generate the closure's body
     */
    public void createNewClosure(int line, StaticScope scope, int arity, CompilerCallback body, CompilerCallback args, boolean hasMultipleArgsHead, NodeType argsNodeId, ASTInspector inspector);

    /**
     * Create a new closure (block) using the given lexical scope information, call arity, and
     * body generated by the body callback. The closure will capture containing scopes and related information.
     *
     * @param scope The static scoping information
     * @param arity The arity of the block's argument list
     * @param body The callback which will generate the closure's body
     */
    public void createNewClosure19(int line, StaticScope scope, int arity, CompilerCallback body, CompilerCallback args, boolean hasMultipleArgsHead, NodeType argsNodeId, ASTInspector inspector);
    
    /**
     * Create a new closure (block) for a for loop with the given call arity and
     * body generated by the body callback.
     * 
     * @param scope The static scoping information
     * @param arity The arity of the block's argument list
     * @param body The callback which will generate the closure's body
     */
    public void createNewForLoop(int arity, CompilerCallback body, CompilerCallback args, boolean hasMultipleArgsHead, NodeType argsNodeId, ASTInspector inspector);
    
    /**
     * Define a new method with the given name, arity, local variable count, and body callback.
     * This will create a new compiled method and bind it to the given name at this point in
     * the program's execution.
     * 
     * @param name The name to which to bind the resulting method.
     * @param arity The arity of the method's argument list
     * @param localVarCount The number of local variables within the method
     * @param body The callback which will generate the method's body.
     */
    public void defineNewMethod(String name, int methodArity, StaticScope scope,
            CompilerCallback body, CompilerCallback args,
            CompilerCallback receiver, ASTInspector inspector, boolean root);
    
    /**
     * Define an alias for a new name to an existing oldName'd method.
     * 
     * @param newName The new alias to create
     * @param oldName The name of the existing method or alias
     */
    public void defineAlias(String newName, String oldName);
    
    public void assignConstantInCurrent(String name);
    
    public void assignConstantInModule(String name);
    
    public void assignConstantInObject(String name);
    
    /**
     * Retrieve the constant with the specified name available at the current point in the
     * program's execution.
     * 
     * @param name The name of the constant
     */
    public void retrieveConstant(String name);

    /**
     * Retreive a named constant from the RubyModule/RubyClass that's just been pushed.
     * 
     * @param name The name of the constant
     */
    public void retrieveConstantFromModule(String name);

    /**
     * Retreive a named constant from the RubyModule/RubyClass that's just been pushed.
     *
     * @param name The name of the constant
     */
    public void retrieveConstantFromObject(String name);
    
    /**
     * Load a Ruby "false" value on top of the stack.
     */
    public void loadFalse();
    
    /**
     * Load a Ruby "true" value on top of the stack.
     */
    public void loadTrue();
    
    /**
     * Load a Ruby "nil" value on top of the stack.
     */
    public void loadNil();
    
    public void loadNull();
    
    /**
     * Load the given string as a symbol on to the top of the stack.
     * 
     * @param symbol The symbol to load.
     */
    public void loadSymbol(String symbol);
    
    /**
     * Load the Object class
     */
    public void loadObject();
    
    /**
     * Retrieve the instance variable with the given name, based on the current "self".
     * 
     * @param name The name of the instance variable to retrieve.
     */
    public void retrieveInstanceVariable(String name);
    
    /**
     * Assign the value on top of the stack to the instance variable with the specified name
     * on the current "self". The value is consumed.
     * 
     * @param name The name of the value to assign.
     */
    public void assignInstanceVariable(String name);
    
    /**
     * Assign the value on top of the stack to the instance variable with the specified name
     * on the current "self". The value is consumed.
     * 
     * @param name The name of the value to assign.
     * @param value A callback for compiling the value to assign
     */
    public void assignInstanceVariable(String name, CompilerCallback value);
    
    /**
     * Assign the top of the stack to the global variable with the specified name.
     * 
     * @param name The name of the global variable.
     */
    public void assignGlobalVariable(String name);
    
    /**
     * Assign the top of the stack to the global variable with the specified name.
     * 
     * @param name The name of the global variable.
     * @param value The callback to compile the value to assign
     */
    public void assignGlobalVariable(String name, CompilerCallback value);
    
    /**
     * Retrieve the global variable with the specified name to the top of the stack.
     * 
     * @param name The name of the global variable.
     */
    public void retrieveGlobalVariable(String name);
    
    /**
     * Perform a logical Ruby "not" operation on the value on top of the stack, leaving the
     * negated result.
     */
    public void negateCurrentValue();
    
    /**
     * Convert the current value into a "splatted value" suitable for passing as
     * method arguments or disassembling into multiple variables.
     */
    public void splatCurrentValue();
    
    /**
     * Given a splatted value, extract a single value. If no splat or length is
     * zero, use nil
     */
    public void singlifySplattedValue();
    
    /**
     * Given an IRubyObject[] on the stack (or otherwise available as the present object)
     * call back to the provided ArrayCallback 'callback' for 'count' elements, starting with 'start'.
     * Each call to callback will have a value from the input array on the stack; once the items are exhausted,
     * the code in nilCallback will be invoked *with no value on the stack*.
     */
    public void forEachInValueArray(int count, int start, Object source, ArrayCallback callback, CompilerCallback argsCallback);
    
    /**
     * Ensures that the present value is an IRubyObject[] by wrapping it with one if it is not.
     */
    public void ensureRubyArray();
    
    /**
     * Ensures that the present value is an IRubyObject[] by wrapping it with one or coercing it if it is not.
     */
    public void ensureMultipleAssignableRubyArray(boolean masgnHasHead);
    
    public void issueBreakEvent(CompilerCallback value);
    
    public void issueNextEvent(CompilerCallback value);
    
    public void issueRedoEvent();
    
    public void issueRetryEvent();

    public void asString();

    public void nthRef(int match);

    public void match();

    public void match2(CompilerCallback value);

    public void match3();

    public void createNewRegexp(ByteList value, int options);
    public void createNewRegexp(CompilerCallback createStringCallback, int options);
    
    public void pollThreadEvents();

    /**
     * Push the current back reference
     */
    public void backref();
    /**
     * Call a static helper method on RubyRegexp with the current backref 
     */
    public void backrefMethod(String methodName);
    
    public void nullToNil();

    /**
     * Makes sure that the code in protectedCode will always run after regularCode.
     */
    public void protect(BranchCallback regularCode, BranchCallback protectedCode, Class ret);
    public void rescue(BranchCallback regularCode, Class exception, BranchCallback protectedCode, Class ret);
    public void performRescue(BranchCallback regularCode, BranchCallback rubyCatchCode, boolean needsRetry);
    public void performEnsure(BranchCallback regularCode, BranchCallback ensuredCode);
    public void inDefined();
    public void outDefined();
    public void stringOrNil();
    public void pushNull();
    public void pushString(String strVal);
    public void isMethodBound(String name, BranchCallback trueBranch, BranchCallback falseBranch);
    public void hasBlock(BranchCallback trueBranch, BranchCallback falseBranch);
    public void isGlobalDefined(String name, BranchCallback trueBranch, BranchCallback falseBranch);
    public void isConstantDefined(String name, BranchCallback trueBranch, BranchCallback falseBranch);
    public void isInstanceVariableDefined(String name, BranchCallback trueBranch, BranchCallback falseBranch);
    public void isClassVarDefined(String name, BranchCallback trueBranch, BranchCallback falseBranch);
    public Object getNewEnding();
    public void ifNull(Object gotoToken);
    public void isNil(BranchCallback trueBranch, BranchCallback falseBranch);
    public void isNull(BranchCallback trueBranch, BranchCallback falseBranch);
    public void ifNotNull(Object gotoToken);
    public void setEnding(Object endingToken);
    public void go(Object gotoToken);
    public void isConstantBranch(BranchCallback setup, BranchCallback isConstant, BranchCallback isMethod, BranchCallback none, String name);
    public void metaclass();
    public void getVisibilityFor(String name);
    public void isPrivate(Object gotoToken, int toConsume);
    public void isNotProtected(Object gotoToken, int toConsume);
    public void selfIsKindOf(Object gotoToken);
    public void loadCurrentModule();
    public void notIsModuleAndClassVarDefined(String name, Object gotoToken);
    public void loadSelf();
    public void ifSingleton(Object gotoToken);
    public void getInstanceVariable(String name);
    public void getFrameName();
    public void getFrameKlazz(); 
    public void superClass();
    public void attached();    
    public void ifNotSuperMethodBound(Object token);
    public void isInstanceOf(Class clazz, BranchCallback trueBranch, BranchCallback falseBranch);
    public void isCaptured(int number, BranchCallback trueBranch, BranchCallback falseBranch);
    public void concatArrays();
    public void appendToArray();
    public void convertToJavaArray();
    public void aryToAry();
    public void toJavaString();
    public void aliasGlobal(String newName, String oldName);
    public void undefMethod(String name);
    public void defineClass(String name, StaticScope staticScope, CompilerCallback superCallback, CompilerCallback pathCallback, CompilerCallback bodyCallback, CompilerCallback receiverCallback, ASTInspector inspector);
    public void defineModule(String name, StaticScope staticScope, CompilerCallback pathCallback, CompilerCallback bodyCallback, ASTInspector inspector);
    public void unwrapPassedBlock();
    public void performBackref(char type);
    public void callZSuper(CompilerCallback closure);
    public void appendToObjectArray();
    public void checkIsExceptionHandled(ArgumentsCallback rescueArgs);
    public void rethrowException();
    public void loadClass(String name);
    public void loadStandardError();
    public void unwrapRaiseException();
    public void loadException();
    public void setFilePosition(ISourcePosition position);
    public void setLinePosition(ISourcePosition position);
    public void checkWhenWithSplat();
    public void createNewEndBlock(CompilerCallback body);
    public void runBeginBlock(StaticScope scope, CompilerCallback body);
    public void rethrowIfSystemExit();

    public BodyCompiler chainToMethod(String name);
    public BodyCompiler outline(String methodName);
    public void wrapJavaException();
    public void literalSwitch(int[] caseInts, Object[] caseBodies, ArrayCallback casesCallback, CompilerCallback defaultCallback);
    public void typeCheckBranch(Class type, BranchCallback trueCallback, BranchCallback falseCallback);
    public void loadFilename();
    public void storeExceptionInErrorInfo();
    public void clearErrorInfo();

    public void compileSequencedConditional(
            CompilerCallback inputValue,
            Class fastPathClass,
            Map<CompilerCallback, int[]> switchCases,
            List<ArgumentsCallback> conditionals,
            List<CompilerCallback> bodies,
            CompilerCallback fallback);
}
