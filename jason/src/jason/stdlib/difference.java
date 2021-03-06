//----------------------------------------------------------------------------
// Copyright (C) 2003  Rafael H. Bordini, Jomi F. Hubner, et al.
// 
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
// 
// To contact the authors:
// http://www.dur.ac.uk/r.bordini
// http://www.inf.furb.br/~jomi
//
//----------------------------------------------------------------------------

package jason.stdlib;

import jason.JasonException;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.InternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Term;

/**
  <p>Internal action: <b><code>.difference(S1,S2,S3)</code></b>.

  <p>Description: S3 is the difference between the sets S1 and S2 (represented by lists).
  The result set is sorted.  

  <p>Parameters:<ul>
  <li>+ arg[0] (a list).<br/>
  <li>+ arg[1] (a list).<br/>
  <li>+/- arg[2]: the difference. 
  </ul>

  <p>Examples:<ul>
  <li> <code>.difference("[a,b,c]","[b,e]",X)</code>: <code>X</code> unifies with "[a,c]".
  <li> <code>.difference("[a,b,a,c]","[f,e,a,c]",X)</code>: <code>X</code> unifies with "[b]".
  </ul>

  @see jason.stdlib.concat
  @see jason.stdlib.delete
  @see jason.stdlib.length
  @see jason.stdlib.member
  @see jason.stdlib.sort
  @see jason.stdlib.substring
  @see jason.stdlib.nth
  @see jason.stdlib.max
  @see jason.stdlib.min
  @see jason.stdlib.reverse
  
  @see jason.stdlib.intersection
  @see jason.stdlib.union
*/
public class difference extends DefaultInternalAction {

	private static InternalAction singleton = null;
	public static InternalAction create() {
		if (singleton == null) 
			singleton = new difference();
		return singleton;
	}

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args)	throws Exception {
		
	    if (!args[0].isList())
            throw new JasonException("First argument of difference '"+args[0]+"'is not a list.");
        if (!args[1].isList())
            throw new JasonException("Second argument of difference '"+args[1]+"'is not a list.");
        
        if (!args[2].isVar() && !args[2].isList())
			throw new JasonException("Last argument of difference '"+args[2]+"'is not a list nor a variable.");

		return un.unifies(args[2], ((ListTerm)args[0]).difference( (ListTerm)args[1]) );
	}
}
