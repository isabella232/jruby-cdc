/*
 * RubyObjectSpace.java - No description
 * Created on 04. November 2001, 22:53
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby;

import java.util.Iterator;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyObjectSpace {

    /** Create the ObjectSpace module and add it to the Ruby runtime.
     * 
     */
    public static RubyModule createObjectSpaceModule(Ruby runtime) {
        RubyModule objectSpaceModule = runtime.defineModule("ObjectSpace");
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyObjectSpace.class);
        objectSpaceModule.defineModuleFunction("each_object", callbackFactory.getOptSingletonMethod("each_object"));
        objectSpaceModule.defineModuleFunction("garbage_collect", callbackFactory.getSingletonMethod("garbage_collect"));
        objectSpaceModule.defineModuleFunction("define_finalizer", 
        		callbackFactory.getOptSingletonMethod("define_finalizer"));

        return objectSpaceModule;
    }

    // TODO: Figure out feasibility of this...
    public static IRubyObject define_finalizer(IRubyObject recv, IRubyObject[] args) {
    	// Put in to fake tempfile.rb out.
    	return recv;
    }
    
    public static IRubyObject each_object(IRubyObject recv, IRubyObject[] args) {
        RubyModule rubyClass;
        if (args.length == 0) {
            rubyClass = recv.getRuntime().getClasses().getObjectClass();
        } else {
            rubyClass = (RubyModule) args[0];
        }
        int count = 0;
        Iterator iter = recv.getRuntime().objectSpace.iterator(rubyClass);
        while (iter.hasNext()) {
            count++;
            recv.getRuntime().yield((IRubyObject) iter.next());
        }
        return recv.getRuntime().newFixnum(count);
    }

    public static IRubyObject garbage_collect(IRubyObject recv) {
        return RubyGC.start(recv);
    }
}
