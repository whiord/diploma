package diploma.eliseev.expr;

import pddl4j.Domain;
import pddl4j.Problem;
import org.eclipse.uml2.uml.Package;

public class TranslationContext {
	public Domain domain;
	public Problem problem;
	public Package domPkg;
	public Package probPkg;
	public boolean isEffect;
	
	public TranslationContext(Domain domain, Problem problem, Package domPkg, Package probPkg) {
		this.domain = domain;
		this.problem = problem;
		this.domPkg = domPkg;
		this.probPkg = probPkg;
		this.isEffect = false;
	}
	
	public TranslationContext(Domain domain, Problem problem, Package domPkg, Package probPkg, boolean isEffect){
		this(domain, problem, domPkg, probPkg);
		this.isEffect = true;
	}
}
