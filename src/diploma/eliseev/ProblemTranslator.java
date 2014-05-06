package diploma.eliseev;

import org.eclipse.uml2.uml.Package;

import pddl4j.Domain;
import pddl4j.PDDLObject;
import pddl4j.Problem;

public class ProblemTranslator extends AbstractTranslator {
	Package domPkg;
	Domain domain;
	Problem problem;
	
	private void translateProblem(){
		
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
