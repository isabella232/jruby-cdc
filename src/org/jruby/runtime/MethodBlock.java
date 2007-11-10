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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2004-2007 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby.runtime;

import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.exceptions.JumpException;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;

/**
 *  Internal live representation of a block ({...} or do ... end).
 */
public class MethodBlock extends Block{
    
    private RubyMethod method;
    private Callback callback;
    
    private final Arity arity;
    
    public boolean isLambda = false;

    public static MethodBlock createMethodBlock(ThreadContext context, DynamicScope dynamicScope, Callback callback, RubyMethod method, IRubyObject self) {
        return new MethodBlock(self,
                               context.getCurrentFrame().duplicate(),
                         context.getCurrentFrame().getVisibility(),
                         context.getRubyClass(),
                         dynamicScope,
                         callback,
                         method);
    }

    public MethodBlock(IRubyObject self, Frame frame, Visibility visibility, RubyModule klass,
        DynamicScope dynamicScope, Callback callback, RubyMethod method) {
        super(self, frame, visibility, klass, dynamicScope);
        this.callback = callback;
        this.method = method;
        this.arity = Arity.createArity((int) method.arity().getLongValue());
    }

    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding) {
        return yield(context, context.getRuntime().newArrayNoCopy(args), null, null, true, binding);
    }
    
    protected void pre(ThreadContext context, RubyModule klass, Binding binding) {
        context.preYieldSpecificBlock(binding, klass);
    }
    
    protected void post(ThreadContext context, Binding binding) {
        context.postYield(binding);
    }
    
    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding) {
        return yield(context, value, null, null, false, binding);
    }

    /**
     * Yield to this block, usually passed to the current call.
     * 
     * @param context represents the current thread-specific data
     * @param value The value to yield, either a single value or an array of values
     * @param self The current self
     * @param klass
     * @param aValue Should value be arrayified or not?
     * @return
     */
    public IRubyObject yield(ThreadContext context, IRubyObject value, IRubyObject self, 
            RubyModule klass, boolean aValue, Binding binding) {
        if (klass == null) {
            self = binding.getSelf();
            binding.getFrame().setSelf(self);
        }
        
        pre(context, klass, binding);

        try {
            // This while loop is for restarting the block call in case a 'redo' fires.
            while (true) {
                try {
                    return callback.execute(value, new IRubyObject[] { method, self }, NULL_BLOCK);
                } catch (JumpException.RedoJump rj) {
                    context.pollThreadEvents();
                    // do nothing, allow loop to redo
                } catch (JumpException.BreakJump bj) {
                    if (bj.getTarget() == null) {
                        bj.setTarget(this);                            
                    }                        
                    throw bj;
                }
            }
        } catch (JumpException.NextJump nj) {
            // A 'next' is like a local return from the block, ending this call or yield.
            return (IRubyObject)nj.getValue();
        } finally {
            post(context, binding);
        }
    }

    public Block cloneBlock(Binding binding) {
        // We clone dynamic scope because this will be a new instance of a block.  Any previously
        // captured instances of this block may still be around and we do not want to start
        // overwriting those values when we create a new one.
        // ENEBO: Once we make self, lastClass, and lastMethod immutable we can remove duplicate
        Block newBlock = new MethodBlock(binding.getSelf(), binding.getFrame().duplicate(), binding.getVisibility(), binding.getKlass(), 
                binding.getDynamicScope().cloneScope(), callback, method);

        return newBlock;
    }

    /**
     * What is the arity of this block?
     * 
     * @return the arity
     */
    public Arity arity() {
        return arity;
    }
}
