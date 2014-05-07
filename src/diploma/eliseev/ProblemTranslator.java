package diploma.eliseev;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.uml2.uml.InstanceSpecification;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Class;

import pddl4j.Domain;
import pddl4j.PDDLObject;
import pddl4j.Problem;
import pddl4j.exp.term.Constant;
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
	
		
	}
	
	private void translateProblem(){
		translateObjects();
		
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
