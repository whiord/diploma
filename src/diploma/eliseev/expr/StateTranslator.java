package diploma.eliseev.expr;

import org.eclipse.uml2.uml.Constraint;

import pddl4j.exp.Exp;

public interface StateTranslator {
	public Constraint translateState(TranslationContext ctx, Exp expr);
}
