package diploma.eliseev;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Property;

import pddl4j.Domain;
import pddl4j.PDDLObject;
import pddl4j.RequireKey;
import pddl4j.exp.AtomicFormula;
import pddl4j.exp.action.Action;
import pddl4j.exp.action.ActionDef;
import pddl4j.exp.action.ActionID;
import pddl4j.exp.term.Term;

public class DomainTranslator extends AbstractTranslator {
	private Domain domain;
	private Map<RequireKey, Boolean> reqs;
	private boolean rootTypeChecked;
	
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
		System.out.println("Types will be extracted and created on fly");
		rootTypeChecked = false;
	}
	
	private void translatePredicates(){
		System.out.println("Processing predicates...");
				
		for (Iterator<AtomicFormula> predIter = domain.predicatesIterator(); predIter.hasNext();){
			AtomicFormula pred = predIter.next();
			String predInfoString = pred.getPredicate() + "(" + pred.getArity() + ")" + " -> "; 
						
			switch  (pred.getArity()){
			case 0:{
				if (globalClass == null){
					globalClass = getClassByName(GLOBAL_CLASS_NAME, true);
					globalClass.setIsAbstract(true);
					
				}
				Property prop = globalClass.createOwnedAttribute(pred.getPredicate(), UML_TYPE_BOOLEAN);
				prop.setIsStatic(true);
				predInfoString += "global attribute";
			}
			case 1:{
				
				Term firstTerm = pred.iterator().next();
				pddl4j.exp.type.Type firstType = firstTerm.getTypeSet().iterator().next();
				
				Class predOwner = getClassForType(firstType);			
				predOwner.createOwnedAttribute(pred.getPredicate(), UML_TYPE_BOOLEAN);
				
				predInfoString += "boolean attribute of " + predOwner.getName();
				
			} break;
			case 2:{				
				Iterator<Term> termIter = pred.iterator();
				Term firstTerm = termIter.next(),
					 secondTerm = termIter.next();
				
				predInfoString += "association of ";
								
				for (pddl4j.exp.type.Type firstType : firstTerm.getTypeSet()){
					for (pddl4j.exp.type.Type secondType : secondTerm.getTypeSet()){
						Class assocOwner = getClassForType(firstType),
							  otherEnd = getClassForType(secondType);
						
						Association assoc = assocOwner.createAssociation(false, AggregationKind.NONE_LITERAL, pred.getPredicate(), 0, -1, otherEnd, 
								 false, AggregationKind.NONE_LITERAL, "x", 0, -1);
		
						assoc.setName(pred.getPredicate());
						
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
				opOwner = getClassForType(firstTerm.getTypeSet().iterator().next());
			}
			else{
				opOwner = globalClass;
			}
			Operation op = opOwner.createOwnedOperation(act.getName(), null, null);
			if (opOwner == globalClass){
				op.setIsStatic(true);
			}
			
			for (;termIter.hasNext();){
				Term nextTerm = termIter.next();
				Class nextArgClass = getClassForType(nextTerm.getTypeSet().iterator().next()); 
				 
				op.createOwnedParameter(nextTerm.toString(), nextArgClass);
				
				actInfoString += nextTerm.toString()+": "+nextArgClass.getName() + (termIter.hasNext()?", ":"");
			}
			
			System.out.println(actInfoString + ") -> " + "operation of " + opOwner.getName());

			Constraint pre = op.createPrecondition(act.getName() + ".precond"),
			           post = op.createPostcondition(act.getName() + ".postcond");
			
			LiteralString specPre = FACTORY.createLiteralString(),
					      specPost = FACTORY.createLiteralString();
			
			
			Action actCl = (Action) act;
			
			specPre.setValue("{PDDL} " + actCl.getPrecondition().toString());
			specPost.setValue("{PDDL}" + actCl.getEffect().toString());
			pre.setSpecification(specPre);
			post.setSpecification(specPost);
		}
	}
	
	private Class extractClassHierarchy(pddl4j.exp.type.Type type){
		String clName = getClassNameForString(type.getImage());
		Class res = rootPkg.createOwnedClass(clName, false);
		
		for (pddl4j.exp.type.Type superType : type.getSuperTypes()){
			if (superType.getImage().equals(pddl4j.exp.type.Type.OBJECT_SYMBOL)){
				rootType = superType;
				continue;
			}
			Class superClass = getClassByName(superType.getImage(), false);
			if (superClass == null) {
				System.out.println("Extracting type: " + type.getImage() + " --> " + superType.getImage());
				superClass = extractClassHierarchy(superType);
				res.createGeneralization(superClass);
			}
			
		}
		
		for (pddl4j.exp.type.Type subType : type.getSubTypes()){
			Class subClass = getClassByName(subType.getImage(), false);
			if (subClass == null){
				System.out.println("Extracting type: " + type.getImage() + " <-- " + subType.getImage());
				subClass = extractClassHierarchy(subType);
				subClass.createGeneralization(res);
			}
		}
				
		return res;
	}
	
	private void checkRootType(){
		rootTypeChecked = true;
		System.out.println("Performing root type check...");
		for (pddl4j.exp.type.Type subType: rootType.getAllSubTypes()){
			if (getClassByName(subType.getImage()) == null ){
				extractClassHierarchy(subType);
			}
		}
		
	}
	
	private String getClassNameForString(String name){
		return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
	}
	
	private Class getClassForType(pddl4j.exp.type.Type type){
		String clName = getClassNameForString(type.getImage());
		
		Class res = getClassByName(clName, false);
		
		if (res != null) return res;
		res = extractClassHierarchy(type);
		
		if (!rootTypeChecked && rootType != null){
			checkRootType();
		}
				
		return res;	
	}
	
	private Class getClassByName(String clName, boolean create) {
		clName = getClassNameForString(clName);
		EList<NamedElement> list = rootPkg.getMembers();
		for (NamedElement elem : list){
			if (elem.getName().equals(clName) && elem instanceof Class) return (Class) elem; 
		}
		if (create) return rootPkg.createOwnedClass(clName, false);
		return null;
	}
	
	private Class getClassByName(String clName){
		return getClassByName(clName, false);
	}

	private void translateDomain(){
		
		getDomainRequirements();
		
		translateTypes();
		translatePredicates();
		translateActions();
		
	}
	
	@Override
	public Package translate(PDDLObject smth) {
		
		rootPkg.setName(smth.getDomainName());
		domain = smth;
		
		translateDomain();
		
		return rootPkg;
	}

}
