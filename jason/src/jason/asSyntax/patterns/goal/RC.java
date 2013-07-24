package jason.asSyntax.patterns.goal;

import jason.asSemantics.Agent;
import jason.asSyntax.Literal;
import jason.asSyntax.Plan;
import jason.asSyntax.Pred;
import jason.asSyntax.Term;
import jason.asSyntax.directives.Directive;
import jason.asSyntax.directives.DirectiveProcessor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the  Relativised Commitment pattern (see DALT 2006 paper)
 * 
 * @author jomi
 */
public class RC implements Directive {

    static Logger logger = Logger.getLogger(RC.class.getName());
    
    public Agent process(Pred directive, Agent outerContent, Agent innerContent) {
    	try {
            Term goal = directive.getTerm(0);
            Term motivation = directive.getTerm(1);
            Literal subDir = Literal.parseLiteral("bc("+goal+")");
            //logger.fine("parameters="+goal+","+motivation+","+subDir);
            Directive sd = DirectiveProcessor.getDirective(subDir.getFunctor());

            // apply sub directive
            Agent newAg = sd.process(subDir, outerContent, innerContent); 
            if (newAg != null) {

                // add -m : true <- .succeed_goal(g).
            	newAg.getPL().add(Plan.parse("-"+motivation+" <- .succeed_goal("+goal+")."));
                
                return newAg;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Directive error.", e);
        }
        return null;
    }
}