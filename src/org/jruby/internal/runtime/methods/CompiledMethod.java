/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2007 Charles Oliver Nutter <headius@headius.com>
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
package org.jruby.internal.runtime.methods;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.exceptions.JumpException;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Block;

public abstract class CompiledMethod extends DynamicMethod implements Cloneable{
    private Arity arity;
    private StaticScope staticScope;
//    private boolean needsImplementer;
    
    public CompiledMethod(RubyModule implementationClass, Arity arity, Visibility visibility, StaticScope staticScope) {
    	super(implementationClass, visibility);
        this.arity = arity;
        this.staticScope = staticScope;
        
        // CompiledMethod will eventually need this logic, since it will eventually compile module methods with super in them
//        if (implementationClass != null) {
//            needsImplementer = !implementationClass.isClass();
//        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args, boolean noSuper, Block block) {
        Ruby runtime = context.getRuntime();
        
//        RubyModule implementer = null;
//        if (needsImplementer) {
//            // modules are included with a shim class; we must find that shim to handle super() appropriately
//            implementer = klazz.findImplementer(getImplementationClass());
//        } else {
//            // classes are directly in the hierarchy, so no special logic is necessary for implementer
//            implementer = getImplementationClass();
//        }
        
        try {
            context.preDefMethodInternalCall(klazz, name, self, args, arity.required(), block, noSuper, staticScope, this);
            // tracing doesn't really work (or make sense yet?) for AOT compiled code
//            if(runtime.getTraceFunction() != null) {
//                ISourcePosition position = context.getPosition();
//                RubyBinding binding = RubyBinding.newBinding(runtime);
//    
//                runtime.callTraceFunction(context, "c-call", position, binding, name, getImplementationClass());
//                try {
//                    return call(context, self, args, block);
//                } finally {
//                    runtime.callTraceFunction(context, "c-return", position, binding, name, getImplementationClass());
//                }
//            }
            return call(context, self, args, block);
        } catch (JumpException.ReturnJump rj) {
            if (rj.getTarget() == this) {
                return (IRubyObject) rj.getValue();
            }
            throw rj;
        } catch (JumpException.RedoJump rj) {
            throw runtime.newLocalJumpError("redo", runtime.getNil(), "unexpected redo");
        } finally {
            context.postDefMethodInternalCall();
        }
    }

    protected abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block);
    
    public DynamicMethod dup() {
        try {
            CompiledMethod msm = (CompiledMethod)clone();
            return msm;
        } catch (CloneNotSupportedException cnse) {
        return null;
    }
    }

    public Arity getArity() {
        return arity;
    }
}// SimpleInvocationMethod
