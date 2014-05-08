package diploma.eliseev.expr;

import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.LiteralString;

import diploma.eliseev.AbstractTranslator;
import pddl4j.exp.Exp;

public class PDDLStateTranslator implements StateTranslator {

	@Override
	public Constraint translateState(TranslationContext ctx, Exp expr) {
		Constraint constraint = AbstractTranslator.FACTORY.createConstraint();
		LiteralString literalString = AbstractTranslator.FACTORY.createLiteralString();
		literalString.setValue("{PDDL} " + expr.toString());
		constraint.setSpecification(literalString);
		return constraint;
	}

}
