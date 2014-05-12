package diploma.eliseev.expr;

import org.eclipse.uml2.uml.Constraint;

import pddl4j.exp.Exp;

public interface ExprTranslator {
	public Constraint translateExpr(TranslationContext ctx, Exp expr);
}
