package diploma.eliseev.expr;

import java.util.Iterator;

import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Property;

import diploma.eliseev.AbstractTranslator;
import diploma.eliseev.DomainTranslator;
import pddl4j.exp.AtomicFormula;
import pddl4j.exp.Exp;
import pddl4j.exp.ExpID;
import pddl4j.exp.ListExp;
import pddl4j.exp.NotExp;
import pddl4j.exp.term.Term;
import pddl4j.exp.type.Type;

public class OCLExprTranslator implements ExprTranslator {

	TranslationContext ctx;

	static int getPriority(ExpID id){
		switch (id) {
		case NOT:   return 10;
		case ATOMIC_FORMULA: return 9;
		case AND:   return 8;
		case OR:    return 6;
		case IMPLY: return 1;
		default:    return 0;
		}
	}
	
	static boolean hasHigherPriority(ExpID id1, ExpID id2){
		return getPriority(id1) > getPriority(id2);
	}
	
	String translateAtomic(AtomicFormula atomic, boolean isPositive){
		String res;
		String predName = AbstractTranslator.extractPDDLName(atomic.getPredicate());
		String _01format = "%s.%s = %s ";
		int arity = atomic.getArity();
		switch (arity) {
		case 0:{
			res = String.format(_01format, DomainTranslator.GLOBAL_CLASS_NAME, predName, isPositive ? "true":"false");
		} break;
		case 1:{
			String argName = atomic.iterator().next().getImage();
			res = String.format(_01format, AbstractTranslator.extractPDDLName(argName), predName, isPositive ? "true":"false");
		} break;				
		case 2:{
			Iterator<Term> termIterator = atomic.iterator(); 
			Term firstTerm = termIterator.next(),
				 secondTerm = termIterator.next();
			Type firstType = firstTerm.getTypeSet().iterator().next(),
				 secondType = secondTerm.getTypeSet().iterator().next();
			Class firstClass = AbstractTranslator.getClassForType(ctx.domPkg, firstType),
				  secondClass = AbstractTranslator.getClassForType(ctx.domPkg, secondType);
			Association assoc = AbstractTranslator.getAssociation(ctx.domPkg, predName, firstClass, secondClass);
			Property secondEnd = assoc.getOwnedEnds().get(1);
			
			String format = ctx.isEffect ? ("%1$s.%2$s = %1$s.%2$s@pre->" + (isPositive ? "including":"excluding") + "(%3$s) ") 
										 : ("%s.%s->" + (isPositive ? "includes":"excludes") + "(%s) "); 
			res = String.format(format, AbstractTranslator.extractPDDLName(firstTerm.getImage()),
								secondEnd.getName(), AbstractTranslator.extractPDDLName(secondTerm.getImage()));
	
		} break;
		default:
			res = "?#1";
		}
		
		return res;
	}
	
	String translateAtomic(AtomicFormula atomic){
		return translateAtomic(atomic, true);
	}
	
	String translateExpr(Exp expr){
		String res = "";
		
		switch (expr.getExpID()) {
		case AND:
		case OR:{
			ListExp andExpr = (ListExp) expr;
			Iterator<Exp> expIterator = andExpr.iterator();
			
			res = translateExpr(expIterator.next());
			
			for (; expIterator.hasNext();){
				Exp subExp = expIterator.next();
				String subString = translateExpr(subExp);
				 
				if (hasHigherPriority(expr.getExpID(), subExp.getExpID())){
					subString = "(" + subString + ")";
				}
				res += String.format("\n%s %s ", expr.getExpID().toString().toLowerCase(), subString);
			}
					
		} break;
		case NOT:{
			NotExp notExp = (NotExp) expr;
			Exp subExp = notExp.getExp();
			boolean subIsAtomic = (subExp instanceof AtomicFormula) ? true : false;
			
			String subString;
			
			if (subIsAtomic){
				subString = translateAtomic((AtomicFormula)subExp, false);
			}
			else{
				subString = translateExpr(subExp);
				if (hasHigherPriority(notExp.getExp().getExpID(), expr.getExpID())){
					subString = "(" + subString + ")";
				}
			}
			
			String format = subIsAtomic ? "%s " : "not %s ";
			res = String.format(format, subString);
			
		} break;
		case ATOMIC_FORMULA:{
			res = translateAtomic((AtomicFormula) expr);
		} break;			
		default:
			res = "?#2";
		}
		return res;
	}
	
	@Override
	public Constraint translateExpr(TranslationContext ctx, Exp expr) {
		this.ctx = ctx;
		Constraint result = AbstractTranslator.FACTORY.createConstraint();
		LiteralString resString = AbstractTranslator.FACTORY.createLiteralString();
		resString.setValue("{OCL} " + translateExpr(expr));
		resString.setName("OCL");
		result.setSpecification(resString);
		return result;
	}

}
