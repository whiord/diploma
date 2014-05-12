package diploma.eliseev;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Property;

import diploma.eliseev.expr.ExprTranslator;
import diploma.eliseev.expr.TranslationContext;
import pddl4j.Domain;
import pddl4j.PDDLObject;
import pddl4j.RequireKey;
import pddl4j.exp.AtomicFormula;
import pddl4j.exp.Exp;
import pddl4j.exp.action.Action;
import pddl4j.exp.action.ActionDef;
import pddl4j.exp.action.ActionID;
import pddl4j.exp.term.Substitution;
import pddl4j.exp.term.Term;
import pddl4j.exp.term.Variable;
import pddl4j.exp.type.Type;

public class DomainTranslator extends AbstractTranslator {
	private Domain domain;
	private Map<RequireKey, Boolean> reqs;
	
	private void getDomainRequirements(){
		reqs = new HashMap<>();
		for (RequireKey req: RequireKey.values()){
			reqs.put(req, false);
		}
		for (Iterator<RequireKey> reqIter = domain.requirementsIterator(); reqIter.hasNext();){
			RequireKey req = reqIter.next();
			reqs.put(req, true);
		} 		
	}
	
	private void translateTypes(){
		if (!reqs.get(RequireKey.TYPING)){
			System.out.println("Typing not supported by domain!");
			return;
		}
		System.out.println("Types will be extracted and created on fly.");
	}
	
	private void translatePredicates(){
		System.out.println("Processing predicates...");
				
		for (Iterator<AtomicFormula> predIter = domain.predicatesIterator(); predIter.hasNext();){
			AtomicFormula pred = predIter.next();
			String predInfoString = pred.getPredicate() + "(" + pred.getArity() + ")" + " -> "; 
						
			switch  (pred.getArity()){
			case 0:{
				Class globalClass = getClassByName(rootPkg, GLOBAL_CLASS_NAME, true);
				globalClass.setIsAbstract(true);
					
				Property prop = globalClass.createOwnedAttribute(extractPDDLName(pred.getPredicate()), UML_TYPE_BOOLEAN);
				prop.setIsStatic(true);
				
				predInfoString += "global attribute";
			} break;
			case 1:{
				
				Term firstTerm = pred.iterator().next();
				Type firstType = firstTerm.getTypeSet().iterator().next();
				
				Class predOwner = getClassForType(rootPkg, firstType);			
				predOwner.createOwnedAttribute(extractPDDLName(pred.getPredicate()), UML_TYPE_BOOLEAN);
				
				predInfoString += "boolean attribute of " + predOwner.getName();
				
			} break;
			case 2:{				
				Iterator<Term> termIter = pred.iterator();
				Term firstTerm = termIter.next(),
					 secondTerm = termIter.next();
				
				predInfoString += "association of ";
								
				for (Type firstType : firstTerm.getTypeSet()){
					for (Type secondType : secondTerm.getTypeSet()){
						Class assocOwner = getClassForType(rootPkg, firstType),
							  otherEnd = getClassForType(rootPkg, secondType);
						
						Association assoc = otherEnd.createAssociation(false, AggregationKind.NONE_LITERAL,  "x", 0, -1, assocOwner, 
								 false, AggregationKind.NONE_LITERAL, extractPDDLName(pred.getPredicate()), 0, -1);
		
						assoc.setName(getAssociationName(pred.getPredicate(), assocOwner.getName(), otherEnd.getName()));
						
						predInfoString += assocOwner.getName() + " ";
					}
				}
			} break;
			default:{
				predInfoString += "n-ary association not supported";
			}
			}
			System.out.println(predInfoString);
		}
	}
	
	private Exp substituteSelf(Action actCl, Exp expr){
		List<Term> params = actCl.getParameters();
		if (params.size() > 0){
			Substitution subst = new Substitution();
			Term firstTerm = params.get(0);

			Variable first = new Variable(firstTerm.getImage().substring(1), firstTerm.getTypeSet()),
					 self  = new Variable("self", firstTerm.getTypeSet());
			
			subst.bind(first, self);
			expr = expr.apply(subst);
		}
		return expr;
	}
	
	private void translateActionExpressions(Operation op, Action actCl){
		Exp preExp = substituteSelf(actCl, actCl.getPrecondition()),
			effExp = substituteSelf(actCl, actCl.getEffect());
		Constraint pre = exprTranslator.translateExpr(new TranslationContext(domain, null, rootPkg, null), preExp), 
				   post = exprTranslator.translateExpr(new TranslationContext(domain, null, rootPkg, null, true), effExp);
		
		op.getPreconditions().add(pre);
		op.getPostconditions().add(post);
		
		pre.setName(op.getName() + ".pre");
		pre.setContext(op);
		post.setName(op.getName() + ".post");
		post.setContext(op);
	}

	private void translateActions(){
		System.out.println("Processing actions...");
			
		for (Iterator<ActionDef> actIter = domain.actionsIterator(); actIter.hasNext();){
			ActionDef act = actIter.next();
			if (act.getActionID() == ActionID.DURATIVE_ACTION){
				System.out.println(act.getName() + " -> " + "durative action not supported");
			}
			
			String actInfoString = act.getName() + "(";
			
			Iterator<Term> termIter =  act.iterator();
			Class opOwner;
			if (termIter.hasNext()){
				Term firstTerm = termIter.next();
				opOwner = getClassForType(rootPkg, firstTerm.getTypeSet().iterator().next());
			}
			else{
				opOwner = getClassByName(rootPkg, GLOBAL_CLASS_NAME, true);
			}
			String opName = extractPDDLName(act.getName());
			Operation op = opOwner.createOwnedOperation(opName, null, null);
			if (opOwner.getName().equals(GLOBAL_CLASS_NAME)){
				op.setIsStatic(true);
			}
			
			for (;termIter.hasNext();){
				Term nextTerm = termIter.next();
				Class nextArgClass = getClassForType(rootPkg, nextTerm.getTypeSet().iterator().next()); 
				String argName = extractPDDLName(nextTerm.toString());
				op.createOwnedParameter(argName, nextArgClass);
				
				actInfoString += argName + ": " + nextArgClass.getName() + (termIter.hasNext()?", ":"");
			}
			
			System.out.println(actInfoString + ") -> " + "operation of " + opOwner.getName());
		
			translateActionExpressions(op,  (Action) act);
		}
	}
		
	private void translateDomain(){
		
		getDomainRequirements();
		
		translateTypes();
		translatePredicates();
		translateActions();
	}
	
	public DomainTranslator(ExprTranslator exprTranslator){
		super(exprTranslator);
	}
	
	@Override
	public Package translate(PDDLObject smth) {
		
		rootPkg.setName(smth.getDomainName());
		domain = smth;
		
		translateDomain();
		
		return rootPkg;
	}

}
