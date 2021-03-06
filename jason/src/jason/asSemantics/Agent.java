// ----------------------------------------------------------------------------
// Copyright (C) 2003 Rafael H. Bordini, Jomi F. Hubner, et al.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
// To contact the authors:
// http://www.dur.ac.uk/r.bordini
// http://www.inf.furb.br/~jomi
//
//----------------------------------------------------------------------------

package jason.asSemantics;

import jason.JasonException;
import jason.RevisionFailedException;
import jason.architecture.AgArch;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.Plan;
import jason.asSyntax.PlanLibrary;
import jason.asSyntax.Pred;
import jason.asSyntax.Rule;
import jason.asSyntax.Term;
import jason.asSyntax.Trigger;
import jason.asSyntax.Trigger.TEOperator;
import jason.asSyntax.Trigger.TEType;
import jason.asSyntax.directives.FunctionRegister;
import jason.asSyntax.directives.Include;
import jason.asSyntax.parser.ParseException;
import jason.asSyntax.parser.as2j;
import jason.bb.BeliefBase;
import jason.bb.DefaultBeliefBase;
import jason.functions.Count;
import jason.functions.RuleToFunction;
import jason.runtime.Settings;
import jason.util.ApplicationProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;



/**
 * The Agent class has the belief base and plan library of an
 * AgentSpeak agent. It also implements the default selection
 * functions of the AgentSpeak semantics.
 */
public class Agent {

    // Members
    protected BeliefBase       bb = new DefaultBeliefBase();
    protected PlanLibrary      pl = new PlanLibrary();
    protected TransitionSystem ts = null;
    protected String           aslSource = null;
    
    private List<Literal>      initialGoals = new ArrayList<Literal>(); // initial goals in the source code
    private List<Literal>      initialBels  = new ArrayList<Literal>(); // initial beliefs in the source code

    private Map<String, InternalAction> internalActions = new HashMap<String, InternalAction>();
    private Map<String, ArithFunction>  functions       = new HashMap<String, ArithFunction>();
    //Umbrales
    public static boolean datosUmbral = ApplicationProperties.getBooleanProperty("datosPrioridad");
    //Mapa de eventos
    private Map <Event, Double> mapEvents = new HashMap<Event, Double> ();
    //Mapa de Opciones
    private Map <Option, Double> mapOptions = new HashMap<Option, Double> ();
    //Mapa de Intenciones
    private Map <Intention, Double> mapIntentions = new HashMap<Intention, Double> ();
    
    private boolean hasCustomSelOp = true;
    
    private ScheduledExecutorService scheduler = null; 
    
    
    
    protected Logger logger = Logger.getLogger(Agent.class.getName());

    public Agent() {
        checkCustomSelectOption();
    }

    private void checkCustomSelectOption() {
        hasCustomSelOp = false;
        for (Method m: this.getClass().getMethods()) {
            if (!m.getDeclaringClass().equals(Agent.class) && m.getName().equals("selectOption")) {
                hasCustomSelOp = true;
            }
        }
    }
    
    public boolean hasCustomSelectOption() {
        return hasCustomSelOp;
    }
    
    /** Creates the TS of this agent, parses its AS source, and sets its Settings */
    public TransitionSystem initAg(AgArch arch, BeliefBase bb, String asSrc, Settings stts) throws JasonException {
        // set the agent
        try {
            setLogger(arch);
            logger.setLevel(stts.logLevel());

            if (bb != null)
                this.bb = bb;
            
            initDefaultFunctions();
            setTS(new TransitionSystem(this, new Circumstance(), stts, arch));

            boolean parsingOk = true;
            if (asSrc != null) {
                asSrc = asSrc.replaceAll("\\\\", "/");
                setASLSrc(asSrc);
    
                if (asSrc.startsWith(Include.CRPrefix)) {
                	// loads the class from a jar file (for example)
                    parseAS(Agent.class.getResource(asSrc.substring(Include.CRPrefix.length())).openStream());
                } else {
    	            // check whether source is an URL string
    	            try {
    	                parsingOk = parseAS(new URL(asSrc));
    	            } catch (MalformedURLException e) {
    	                parsingOk = parseAS(new File(asSrc));
    	            }
                }
            }

            if (parsingOk) {
                addInitialBelsInBB();
                addInitialGoalsInTS();
            }

            // kqml Plans at the end of the ag PS
            if (JasonException.class.getResource("/asl/kqmlPlans.asl") != null) { 
                setASLSrc("kqmlPlans.asl");
                parseAS(JasonException.class.getResource("/asl/kqmlPlans.asl"));
                setASLSrc(asSrc);
            } else {
                logger.warning("The kqmlPlans.asl was not found!");
            }
            

            return ts;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating the agent class!", e);
            throw new JasonException("Error creating the agent class! - " + e);
        }
    }
    
    public void stopAg() {
        bb.stop();
        
        if (scheduler != null) 
            scheduler.shutdownNow();
    }

    /** 
     *  Clone BB, PL, Circumstance. 
     *  A new TS is created (based on the cloned circumstance).
     */
    public Agent clone(AgArch arch) {
    	Agent a = new Agent();
    	
    	a.setLogger(arch);
        a.logger.setLevel(this.getTS().getSettings().logLevel());

    	a.bb = (BeliefBase)this.bb.clone();
    	a.pl = (PlanLibrary)this.pl.clone();
    	a.aslSource = this.aslSource;
    	a.initDefaultFunctions();
        a.setTS(new TransitionSystem(a, (Circumstance)this.getTS().getC().clone(), this.getTS().getSettings(), arch));

    	return a;
    }
    
    public void setLogger(AgArch arch) {
        if (arch != null)
            logger = Logger.getLogger(Agent.class.getName() + "." + arch.getAgName());
    }

    public Logger getLogger() {
        return logger;
    }
    
    public ScheduledExecutorService getScheduler() {
        if (scheduler == null)
            scheduler = Executors.newScheduledThreadPool(1);
        return scheduler;
    }
    

    /** Returns the .asl file source used to create this agent */
    public String getASLSrc() {
        return aslSource;
    }
    
    public void setASLSrc(String file) {
        if (file != null && file.startsWith("./")) 
            file = file.substring(2);
        aslSource = file;    	
    }

    /** Adds beliefs and plans form an URL */
    public boolean parseAS(URL asURL) {
        try {
            parseAS(asURL.openStream());
            logger.fine("as2j: AgentSpeak program '" + asURL + "' parsed successfully!");
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "as2j: the AgentSpeak source file '"+asURL+"' was not found!");
        } catch (ParseException e) {
            logger.log(Level.SEVERE, "as2j: parsing error: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "as2j: error parsing \"" + asURL + "\"", e);
        }
        return false;
    }

    /** Adds beliefs and plans form a file */
    public boolean parseAS(File asFile) {
        try {
            parseAS(new FileInputStream(asFile));
            logger.fine("as2j: AgentSpeak program '" + asFile + "' parsed successfully!");
            return true;
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "as2j: the AgentSpeak source file '"+asFile+"' was not found!");
        } catch (ParseException e) {
            logger.log(Level.SEVERE, "as2j: parsing error:" + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "as2j: error parsing \"" + asFile + "\"", e);
        }
        return false;
    }

    public void parseAS(InputStream asIn) throws ParseException, JasonException {
        as2j parser = new as2j(asIn);
        parser.agent(this);
    }
    public void parseAS(Reader asIn) throws ParseException, JasonException {
        as2j parser = new as2j(asIn);
        parser.agent(this);
    }

    @SuppressWarnings("unchecked")
	public InternalAction getIA(String iaName) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (iaName.charAt(0) == '.')
            iaName = "jason.stdlib" + iaName;
        InternalAction objIA = internalActions.get(iaName);
        if (objIA == null) {
        	Class iaclass = Class.forName(iaName);
        	try {
        		// check if the class has "create" method -- singleton implementation
        		Method create = iaclass.getMethod("create", (Class[])null);
        		objIA = (InternalAction)create.invoke(null, (Object[])null);
        	} catch (Exception e) {
            	objIA = (InternalAction)iaclass.newInstance();        		
        	}
    		internalActions.put(iaName, objIA);
        }
        return objIA;
    }
    
    public void initDefaultFunctions() {
        addFunction(Count.class, false);
    }
 
    /** register an arithmetic function implemented in Java */
    public void addFunction(Class<? extends ArithFunction> c) {
        addFunction(c,true);
    }
    /** register an arithmetic function implemented in Java */
    private void addFunction(Class<? extends ArithFunction> c, boolean user) {
        try {
            ArithFunction af = c.newInstance();
            String error = null;
            if (user)
                error = FunctionRegister.checkFunctionName(af.getName());
            if (error != null)
                logger.warning(error);
            else
                functions.put(af.getName(),af);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error registering function "+c.getName(),e);
        }
    }
     
    /** register an arithmetic function implemented in AS (by a rule, literal, or internal action) */
    public void addFunction(String function, int arity, String literal) {
        try {
            String error = FunctionRegister.checkFunctionName(function);
            if (error != null)
                logger.warning(error);
            else
                functions.put(function, new RuleToFunction(literal, arity));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error registering function "+literal,e);
        }
    }
    
    /** get the object the implements the arithmetic function <i>function/arity</i>,  
     *  either global (like math.max) or local (like .count).
     */
    public ArithFunction getFunction(String function, int arity) {
        ArithFunction af = functions.get(function);
        if (af == null || !af.checkArity(arity))
            // try global function
            af = FunctionRegister.getFunction(function, arity);
        if (af != null && af.checkArity(arity))
            return af;
        else 
            return null;
    }

    
    public void addInitialGoal(Literal g) {
    	initialGoals.add(g);
    }
    public void addInitialBel(Literal b) {
    	initialBels.add(b);
    }
    public List<Literal> getInitialBels() {
    	return initialBels;
    }
    
    /** add the initial beliefs in BB and produce the corresponding events */
    public void addInitialBelsInBB() throws RevisionFailedException {
        // Once beliefs are stored in a Stack in the BB, insert them in inverse order
        for (int i=initialBels.size()-1; i >=0; i--) {
            Literal b = initialBels.get(i);

            // if l is not a rule and has free vars (like l(X)), convert it into a rule like "l(X) :- true."
            if (!b.isRule() && !b.isGround())
                b = new Rule(b,Literal.LTrue);

            b.apply(new Unifier()); // to solve arithmetic expressions
                            
            // does not do BRF for rules (and so do not produce events +bel for rules)
            if (b.isRule())
            	getBB().add(b);
            else
            	addBel(b);
        }
        initialBels.clear();
    }
    
    public void addInitialGoalsInTS() {
	    for (Literal g: initialGoals) {
	    	g.makeVarsAnnon(null);
	    	if (! g.hasSource())
	    	    g.addAnnot(BeliefBase.TSelf);
	    	getTS().getC().addAchvGoal(g,Intention.EmptyInt);
	    }
    }

    /** Imports beliefs, plans and initial goals from another agent. Initial beliefs and goals 
     *  are stored in "initialBels" and "initialGoals" lists but not included in the BB / TS.
     *  The methods addInitialBelsInBB and addInitialGoalsInTS should be called in the sequel to
     *  add those beliefs and goals into the agent. */
    public void importComponents(Agent a) throws JasonException {
    	if (a != null) {
	    	for (Literal b: a.initialBels) {
	    		this.addInitialBel(b);
	    	}
	    	for (Literal g: a.initialGoals) {
	    		this.addInitialGoal(g);
	    	}
	    	for (Plan p: a.getPL()) {
	    		this.getPL().add(p);
	    	}
    	}
    }
    
    
    /**
     * Follows the default implementation for the agent's message acceptance
     * relation and selection functions
     */
    public boolean socAcc(Message m) {
        return true;
    }

    public Event selectEvent(Queue<Event> events) {
        // make sure the selected Event is removed from 'events' queue
    	/*
    	 * @Author:Adrian Biga
    	 * El evento es extraido de la cola de eventos ,la funcion pool retorna
    	 * y remueve la cabeza de la cola.No tiene en cuenta las anotaciones de los beliefs para
    	 * esto. 
    	 *
    	 */        
    	 Iterator<Event> it=events.iterator();
    	 while(it.hasNext()){
    		 Event e = it.next();
    		// System.out.println("Selectora de eventos (literales): " + e.getTrigger().getLiteral().getAnnots());
    		 if(datosUmbral == true){
    			ordenarListaEventosUmbral(e); 
    		 }else{
    			 ordenarListaEventos(e);
    		 }
    	 }
    	 //return events.poll();(version anterior)
    	 Event selected = selectEventN(events);
    	 events.remove(selected);
    	 return selected;
    	 
    }
    private  void ordenarListaEventos (Event e){
    	ListTerm l = e.getTrigger().getLiteral().getAnnots();
    	List <Term> lis = l.getAsList();
    	Iterator<Term> it =lis.iterator();
    	Double f = -1.0;
    	while(it.hasNext()){
    		Term t   = it.next();
    		String s = t.toString();
    		//System.out.println("Terminos del evento "+s);
    		int i    = s.indexOf("degOfCert");
    		
    		//encontro la parte del termino que posee "degOfCert"
    		if(i!=-1 ){
    			if(s.charAt(i+10) != '1'){
	    			String sub = s.substring(i+10,i+13);
	    			f =new Double(sub);
	    			this.mapEvents.put(e, f);
    			}
    			else {
    				this.mapEvents.put(e, 1.0);
    			}
    		}
    	}
    	if(f.equals(-1.0)){
			this.mapEvents.put(e, 1.0);
    	}
		
    }
    private  void ordenarListaEventosUmbral(Event e){
    	String l = e.getTrigger().getLiteral().getFunctor();
    	Double f = -1.0;
    	String grImp = ApplicationProperties.getProperty(l.trim());
    	
    	if(grImp != null ){
    			    f =new Double(grImp);
	    			this.mapEvents.put(e, f);
	    			//System.out.println("Umbral " + l + ":" + f);
    	}
    	else{
    				this.mapEvents.put(e, 1.0);
    				//System.out.println("Umbral " + l + ":" + f);
    	}
    	
    }
    private Event selectEventN(Queue<Event> events){
    	
    	Iterator<Event> it = events.iterator();
    	Event temp = null;
    	Event max = null;
    	if(it.hasNext()){
    	 temp =it.next(); 
    	 max  = temp;
    	}
    	while(it.hasNext()){
    		Event e =it.next();
    		if(mapEvents.get(e) >mapEvents.get(temp)){
    			max = e;
    			temp = max;
    		}
    	}
    	//System.out.println("Evento con el grado mas alto:" + max.toString());
    	return max;
    } 
    public Option selectOption(List<Option> options) {
    	/*
    	 * @Author:Adrian Biga
    	 * La opcion es extraida de una lista y "remove" la retorna.No tiene en 
    	 * cuenta las anotaciones ni los labels para esto.
    	 *
    	 */  
    	if (options != null && !options.isEmpty()) {
    		Iterator<Option> it=options.iterator();
       	 	while(it.hasNext()){
       	 		Option o = it.next();
       	 		//System.out.println("Selectora de opciones (literales): " + o.getPlan().toString() + "..." + o.getUnifier().toString());
       	 		ordenarListaOpciones(o);
       	    }
            
       	 	//return options.remove(0);
	       	 Option selected = selectOptionN(options);
	    	 options.remove(selected);
	    	 return selected;
        } else {
            return null;
        }
    }
    private  void ordenarListaOpciones (Option o){
    	//Obtengo el predicado del plan
    //	Pred pred = o.getPlan().getLabel();
    	//System.out.println("Anotaciones del label del plan para Opciones: "+ o.getPlan().getLabel().getAnnots());
    	ListTerm l = o.getPlan().getLabel().getAnnots();
    	List <Term> lis = l.getAsList();
    	Iterator<Term> it =lis.iterator();
    	Double f = -1.0;
    	while(it.hasNext()){
    		Term t   = it.next();
    		String s = t.toString();
    	//	System.out.println("Terminos de la  opcion: "+s);
    		//int i    = s.indexOf("degOfCert");
    		int i    = s.indexOf("chanceOfSucess");
    		//encontro la parte del termino que posee "chanceOfSucess"
    		if(i!=-1 ){
    			if(s.charAt(i+15) != '1'){
	    			String sub = s.substring(i+15,i+18);
	    			f =new Double(sub);
	    			this.mapOptions.put(o, f);
    			}
    			else {
    				this.mapOptions.put(o, 1.0);
    			}
    		}
    	}
    	if(f.equals(-1.0)){
			this.mapOptions.put(o, 1.0);
    	}
    }
    
    private Option selectOptionN(List<Option> options){
    	
    	Iterator<Option> it = options.iterator();
    	Option temp = null;
    	Option max = null;
    	if(it.hasNext()){
    	 temp =it.next(); 
    	 max  = temp;
    	}
    	while(it.hasNext()){
    		Option e =it.next();
    		if(mapOptions.get(e) >mapOptions.get(temp)){
    			max = e;
    			temp = max;
    		}
    	}
    	//System.out.println("Opcion con el grado mas alto:" + max.toString());
    	return max;
    } 
    public Intention selectIntention(Queue<Intention> intentions) {
        // make sure the selected Intention is removed from 'intentions'
        // and make sure no intention will "starve"!!!
        
    	//return intentions.poll();
    	
    	/*
    	 * @Author:Adrian Biga
    	 * La intencion es extraida de una cola y "poll" la retorna.No tiene en 
    	 * cuenta las anotaciones ni los labels para esto.
    	 *
    	 */  
    	 Iterator<Intention> it=intentions.iterator();
	   	 while(it.hasNext()){
	   		 Intention i = it.next();
	   		// System.out.println("Selectora de intenciones (como termino): " + i.getAsTerm());
	   		 ordenarListaIntenciones(i);
	   	 }
	   	
	   	 Intention selected = selectIntentionN(intentions);
	   	 intentions.remove(selected);
	   	 return selected;
	   
    }
    
    private  void ordenarListaIntenciones(Intention i){
    	
    	Term ter = i.getAsTerm();
    	String sTerm = ter.toString();
    	Double temp = 0.0;
    	Double max =  0.0;
    	Double f = -1.0;
    	//System.out.println("Terminos de la intencion: "+ sTerm);
        //Puede aparecer mas de una ves ,por lo cual nos tenemos que quedar con el maximo
    	
    	int j  = sTerm.indexOf("chanceOfSucess");
    	String sub = sTerm.substring(j+15,j+18);
    	if(j == -1){
    		 this.mapIntentions.put(i,1.0);
    	}
    	else {
    		if(j!=-1 ){
    			
    			if(sTerm.charAt(j+15) != '1'){
	    			
	    			f =new Double(sub);
	    			//this.mapOptions.put(o, f);
	    			temp = f;
	    			max  = temp;
	    			
    			}
    			else {
    				temp = 1.0;
	    			max  = temp;
    				//this.mapOptions.put(o, 1.0);
    			}
    		}
    		int index = j + 15;
    		while(sTerm.indexOf("chanceOfSucess",index)!= -1){
    			j = sTerm.indexOf("chanceOfSucess",index);
    			
    			if(sTerm.charAt(j+15) != '1'){
	    			
	    			f =new Double(sub);
	    			if(f > temp){
	    				max = f;
	    				temp = max;
	    			}
    			}
    			else {
    				if(1.0 > temp){
	    				max = 1.0;
	    				temp = max;
	    			}
    				//this.mapOptions.put(o, 1.0);
    			}
    		   index = j + 15;
    		}
    	}
    	this.mapIntentions.put(i,max);
    		
    	
    	
    }
    
    private Intention selectIntentionN(Queue <Intention> intentions){
    	
    	Iterator<Intention> it = intentions.iterator();
    	Intention temp = null;
    	Intention max = null;
    	if(it.hasNext()){
    	 temp =it.next(); 
    	 max  = temp;
    	}
    	while(it.hasNext()){
    		Intention e =it.next();
    		if(mapIntentions.get(e) >mapIntentions.get(temp)){
    			max = e;
    			temp = max;
    		}
    	}
    	//System.out.println("Intencion con el grado mas alto:" + max.toString());
    	return max;
    } 
    
    public Message selectMessage(Queue<Message> messages) {
        // make sure the selected Message is removed from 'messages'
        return messages.poll();
    }

    public ActionExec selectAction(List<ActionExec> actList) {
        // make sure the selected Action is removed from actList
        // (do not return suspended intentions)
    	synchronized (actList) {
            Iterator<ActionExec> i = actList.iterator();
            while (i.hasNext()) {
                ActionExec a = i.next();
                if (!a.getIntention().isSuspended()) {
                    i.remove();
                    return a;
                }
            }			
		}
        return null;
    }

    /** TS Initialisation (called by the AgArch) */
    public void setTS(TransitionSystem ts) {
        this.ts = ts;
    }

    public TransitionSystem getTS() {
        return ts;
    }

    public BeliefBase getBB() {
        return bb;
    }

    public void setPL(PlanLibrary pl) {
    	this.pl = pl;
    }
    
    public PlanLibrary getPL() {
        return pl;
    }

    /** Belief Update Function: adds/removes percepts into belief base */
    public void buf(List<Literal> percepts) {
        if (percepts == null) {
            return;
        }

        // deleting percepts in the BB that is not perceived anymore
        Iterator<Literal> perceptsInBB = getBB().getPercepts();
        while (perceptsInBB.hasNext()) { 
            Literal l = perceptsInBB.next();

            // could not use percepts.contains(l), since equalsAsTerm must be
            // used (to ignore annotations)
            boolean wasPerceived = false;
            Iterator<Literal> ip = percepts.iterator();
            while (ip.hasNext()) {
            	Literal t = ip.next();
            	
                // if perception t is already in BB
                if (l.equalsAsStructure(t) && l.negated() == t.negated()) {
                    wasPerceived = true;
                    // remove in percepts, since it already is in BB 
                    // [can not be always removed, since annots in this percepts should be added in BB
                    //  Jason team for AC, for example, use annots in perceptions]
                    if (!l.hasAnnot())
                        ip.remove();
                    break;
                }
            }
            if (!wasPerceived) {
                // new version (it is sure that l is in BB, only clone l when the event is relevant)
                perceptsInBB.remove(); // remove l as perception from BB
                
                Trigger te = new Trigger(TEOperator.del, TEType.belief, l);
                if (ts.getC().hasListener() || pl.hasCandidatePlan(te)) {
                    l = (Literal)l.clone();
                    l.clearAnnots();
                    l.addAnnot(BeliefBase.TPercept);
                    te.setLiteral(l);
                    ts.getC().addEvent(new Event(te, Intention.EmptyInt));
                }
        
                /*
                // old version
                // can not delete l, but l[source(percept)]
                l = (Literal)l.clone();
                l.clearAnnots();
                l.addAnnot(BeliefBase.TPercept);
	            if (bb.remove(l)) {
	                ts.updateEvents(new Event(new Trigger(TEOperator.del, TEType.belief, l), Intention.EmptyInt));
	            }
	            */
            }
        }

        // BUF only adds a belief when appropriate
        // checking all percepts for new beliefs
        for (Literal lp : percepts) {
            try {
                lp = (Literal) lp.clone();
                lp.addAnnot(BeliefBase.TPercept);
                if (getBB().add(lp)) {
                    Trigger te = new Trigger(TEOperator.add, TEType.belief, lp);
                    ts.updateEvents(new Event(te, Intention.EmptyInt));
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error adding percetion " + lp, e);
            }
        }
    }

    /**
     * Returns true if BB contains the literal <i>bel</bel> (using unification to test).
     * The unifier <i>un</i> is updated by the method.
     */
    public boolean believes(LogicalFormula bel, Unifier un) {
        try {
            Iterator<Unifier> iun = bel.logicalConsequence(this, un);
            if (iun != null && iun.hasNext()) {
                un.compose(iun.next());
                return true;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in believe("+bel+","+un+").",e);
        }
        return false;
    }

    /**
     * Find a literal in BB using unification to test.
     * 
     * Returns the belief as it is in BB, e.g. findBel(a(_),...) 
     * may returns a(10)[source(ag)].
     *   
     * The unifier <i>un</i> is updated by the method.
     */
    public Literal findBel(Literal bel, Unifier un) {
        Iterator<Literal> relB = bb.getCandidateBeliefs(bel, un);
        if (relB != null) {
            while (relB.hasNext()) {
                Literal b = relB.next();
                
                // recall that order is important because of annotations!
                if (!b.isRule() && un.unifies(bel, b)) {
                    return b;
                }
            }
        } 
        return null;
    }

    
    /**
     * This function should revise the belief base with the given literal to
     * add, to remove, and the current intention that triggered the operation.
     * 
     * <p>In its return, List[0] has the list of actual additions to
     * the belief base, and List[1] has the list of actual deletions;
     * this is used to generate the appropriate internal events. If
     * nothing change, returns null.
     */
    @SuppressWarnings("unchecked")
    public List<Literal>[] brf(Literal beliefToAdd, Literal beliefToDel,  Intention i) throws RevisionFailedException {
        // This class does not implement belief revision! It
        // is supposed that a subclass will do it.
        // It simply add/del the belief.

        List<Literal>[] result = null;
        try {
	        if (beliefToAdd != null) {
	        	if (logger.isLoggable(Level.FINE)) logger.fine("adding belief " + beliefToAdd);
	            
	            if (getBB().add(beliefToAdd)) {
	            	result = new List[2];
	                result[0] = Collections.singletonList(beliefToAdd);
	                result[1] = Collections.emptyList();
	            }
	        }
	
	        if (beliefToDel != null) {
	            Unifier u = null;
	            try {
	                u = i.peek().unif; // get from current intention
	            } catch (Exception e) {
	                u = new Unifier();
	            }
	
	            if (logger.isLoggable(Level.FINE)) logger.fine("doing brf for " + beliefToDel + " in BB=" + believes(beliefToDel, u));
	            
	            if (believes(beliefToDel, u)) {
	            	beliefToDel.apply(u);
	                if (getBB().remove(beliefToDel)) {
	                    if (logger.isLoggable(Level.FINE)) logger.fine("Removed:" + beliefToDel);
	                    if (result == null) {
	                    	result = new List[2];
	                        result[0] = Collections.emptyList();
	                    }
	                    result[1] = Collections.singletonList(beliefToDel);
	                }
	            }
	
	        }
    	} catch (Exception e) {
    		logger.log(Level.WARNING, "Error at BRF.",e);
    	}
        return result;
    }

    /**
     * Adds <i>bel</i> in belief base (calling brf) and generates the
     * events. If <i>bel</i> has no source, add
     * <code>source(self)</code>. (the belief is not cloned!)
     */
    public boolean addBel(Literal bel) throws RevisionFailedException {
        if (!bel.hasSource()) {
            bel.addAnnot(BeliefBase.TSelf);
        }
        List<Literal>[] result = brf(bel, null, Intention.EmptyInt);
        if (result != null && ts != null) {
            ts.updateEvents(result, Intention.EmptyInt);
            return true;
        } else {
            return false;
        }
    }

    /**
     * If the agent believes in <i>bel</i>, removes it (calling brf)
     * and generate the event.
     */
    public boolean delBel(Literal bel) throws RevisionFailedException {
        List<Literal>[] result = brf(null, bel, Intention.EmptyInt);
        if (result != null && ts != null) {
            ts.updateEvents(result, Intention.EmptyInt);
            return true;
        } else {
            return false;
        }
    }
    
    /** Removes all occurrences of <i>bel</i> in BB. 
        If <i>un</i> is null, an empty Unifier is used. 
     */
    public void abolish(Literal bel, Unifier un) throws RevisionFailedException {
        List<Literal> toDel = new ArrayList<Literal>();
        if (un == null) un = new Unifier();
        Iterator<Literal> il = getBB().getCandidateBeliefs(bel, un);
        if (il != null) {
            while (il.hasNext()) {
                Literal inBB = il.next();
                if (!inBB.isRule()) {
                    // need to clone unifier since it is changed in previous iteration
                    if (un.copy().unifiesNoUndo(bel, inBB)) {
                        toDel.add(inBB);
                    }
                }
            }
        }
        
        for (Literal l: toDel) {
            delBel(l);
        }
    }

    static DocumentBuilder builder = null;

    /** Gets the agent "mind" (Beliefs, plans and circumstance) as XML */
    public Document getAgState() {
        if (builder == null) {
            try {
                builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error creating XML builder\n");
                return null;
            }
        }
        Document document = builder.newDocument();
        document.appendChild(document.createProcessingInstruction("xml-stylesheet", "href='http://jason.sf.net/xml/agInspection.xsl' type='text/xsl' "));

        Element ag = getAsDOM(document);
        document.appendChild(ag);

        ag.appendChild(ts.getC().getAsDOM(document));
        return document;
    }

    @Override
    public String toString() {
        return "Agent "+getASLSrc();
    }
    
    /** Gets the agent "mind" as XML */
    public Element getAsDOM(Document document) {
        Element ag = (Element) document.createElement("agent");
        ag.setAttribute("name", ts.getUserAgArch().getAgName());
        ag.setAttribute("cycle", ""+ts.getUserAgArch().getCycleNumber());

        ag.appendChild(bb.getAsDOM(document));
        // ag.appendChild(ps.getAsDOM(document));
        return ag;
    }

    /** Gets the agent program (Beliefs and plans) as XML */
    public Document getAgProgram() {
        if (builder == null) {
            try {
                builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error creating XML builder\n");
                return null;
            }
        }
        Document document = builder.newDocument();
        Element ag = (Element) document.createElement("agent");
        if (getASLSrc() != null && getASLSrc().length() > 0) {
        	ag.setAttribute("source", getASLSrc());
        }
        ag.appendChild(bb.getAsDOM(document));
        ag.appendChild(pl.getAsDOM(document));
        document.appendChild(ag);

        return document;
    }

}
