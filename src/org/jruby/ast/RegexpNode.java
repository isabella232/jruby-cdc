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
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.ast;

import java.util.List;
import jregex.Pattern;

import org.jruby.RegexpTranslator;
import org.jruby.ast.types.ILiteralNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.Instruction;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.util.ByteList;

/** Represents a simple regular expression literal.
 *
 * @author  jpetersen
 */
public class RegexpNode extends Node implements ILiteralNode {
    static final long serialVersionUID = -1566813018564622077L;

    private static final RegexpTranslator translator = new RegexpTranslator();
    
    private Pattern pattern;
    private int flags;
    private final ByteList value;
    private final int options;
    
    public RegexpNode(ISourcePosition position, ByteList value, int options) {
        super(position, NodeTypes.REGEXPNODE);
        
        this.value = value;
        this.options = options;
    }

    public Instruction accept(NodeVisitor iVisitor) {
        return iVisitor.visitRegexpNode(this);
    }

    /**
     * Gets the options.
     * @return Returns a int
     */
    public int getOptions() {
        return options;
    }

    /**
     * Gets the value.
     * @return Returns a ByteList
     */
    public ByteList getValue() {
        return value;
    }
    
    public int getFlags(int extra_options) {
        if (pattern == null) {
            flags = RegexpTranslator.translateFlags(options | extra_options);
            pattern = translator.translate(value, options, flags);
        }
        return flags;
    }

    public Pattern getPattern(int extra_options) {
        if (pattern == null) {
            flags = RegexpTranslator.translateFlags(options | extra_options);
            pattern = translator.translate(value, options, flags);
        }
        return pattern;
    }
    
    public List childNodes() {
        return EMPTY_LIST;
    }

}
