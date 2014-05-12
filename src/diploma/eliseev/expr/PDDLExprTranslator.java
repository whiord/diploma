package diploma.eliseev.expr;

import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.LiteralString;

import diploma.eliseev.AbstractTranslator;
import pddl4j.exp.Exp;

public class PDDLExprTranslator implements ExprTranslator {

	@Override
	public Constraint translateExpr(TranslationContext ctx, Exp expr) {
		Constraint constraint = AbstractTranslator.FACTORY.createConstraint();
		LiteralString literalString = AbstractTranslator.FACTORY.createLiteralString();
		literalString.setValue("{PDDL} " + expr.toString());
		literalString.setName("PDDL");
		constraint.setSpecification(literalString);
		constraint.setContext(ctx.probPkg);
		return constraint;
	}

}
