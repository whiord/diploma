package diploma.eliseev;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.uml2.uml.InstanceSpecification;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Slot;

import pddl4j.Domain;
import pddl4j.PDDLObject;
import pddl4j.Problem;
import pddl4j.exp.AtomicFormula;
import pddl4j.exp.ExpID;
import pddl4j.exp.InitEl;
import pddl4j.exp.term.Constant;
import pddl4j.exp.term.Term;
import pddl4j.exp.type.Type;

public class ProblemTranslator extends AbstractTranslator {
	Package domPkg;
	Domain domain;
	Problem problem;
	
	private void translateObjects(){
		System.out.println("Processing objects...");
		Map <Class, Set<InstanceSpecification>> instances = new HashMap<>(); 
		
		for (Iterator<Constant> constIter = problem.constantsIterator(); constIter.hasNext();){
			Constant constant = constIter.next();
			Type instType = constant.getTypeSet().iterator().next();
			Class instClass = getClassForType(domPkg, instType);

			InstanceSpecification instSpec = FACTORY.createInstanceSpecification();
			instSpec.setName(constant.getImage());
			instSpec.getClassifiers().add(instClass);
			
			rootPkg.getPackagedElements().add(instSpec);
			if (!instances.containsKey(instClass)){
				instances.put(instClass, new HashSet<InstanceSpecification>());
			}
			instances.get(instClass).add(instSpec);
		}
		
		for (Class instClass : instances.keySet()){
			System.out.print(instClass.getName() + ": ");
			for (InstanceSpecification instSpec : instances.get(instClass)){
				System.out.print(instSpec.getName() + " ");
			}
			System.out.println();
			
		}
	}
	
	private Class getGlobalClass(Package pkg){
		return getClassByName(pkg, GLOBAL_CLASS_NAME, false);
	}
	
	private void translateInitialState() {
		System.out.println("Processing initial state...");
		Package initPkg = rootPkg.createNestedPackage("init");
		
		for ( InitEl initElement : problem.getInit()){
			if (initElement.getExpID() == ExpID.ATOMIC_FORMULA){
				AtomicFormula atFormula = (AtomicFormula) initElement;
				String initElemInfo = "";
				switch (atFormula.getArity()) {
				case 0:
				case 1:{
					InstanceSpecification spec;
					Class instClass;
					
					if (atFormula.getArity() == 0){
						spec = getInstSpecificationByName(initPkg, GLOBAL_CLASS_NAME, getGlobalClass(domPkg));
						instClass = getGlobalClass(domPkg);
					}
					else{
						Term instTerm = atFormula.iterator().next();
						Type instType = instTerm.getTypeSet().iterator().next();
						instClass = getClassForType(domPkg, instType);
						spec = getInstSpecificationByName(initPkg, instTerm.getImage(), instClass);
					}
					
					Property definingProperty = getClassPropertyByName(instClass, atFormula.getPredicate());
					Slot slot = spec.createSlot();
					slot.setDefiningFeature(definingProperty);
					LiteralBoolean value = FACTORY.createLiteralBoolean();
					value.setValue(true);
					slot.getValues().add(value);
					
					initElemInfo = spec.getName() + "." + definingProperty.getName() + " = true";
				} break;
				case 2: {
					
				} break;
				default:
					initElemInfo = "n-ary association not supported";
				}
				
				System.out.println(initElemInfo);
			}
			
		}
		
	}
	
	private void translateProblem(){
		translateObjects();
		
		translateInitialState();
	}

	public ProblemTranslator(Package domainPackage){
		domPkg = domainPackage;
	}

	@Override
	public Package translate(PDDLObject smth) {
		domain = smth;
		problem = smth;
		rootPkg.setName(problem.getProblemName());
		
		translateProblem();
		
		return rootPkg;
	}

}
