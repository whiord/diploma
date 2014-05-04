package diploma.eliseev;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.event.ObjectChangeListener;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.ValueSpecification;
import org.eclipse.uml2.uml.internal.impl.LiteralStringImpl;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Class;

import pddl4j.Domain;
import pddl4j.Parser;
import pddl4j.RequireKey;
import pddl4j.exp.AtomicFormula;
import pddl4j.exp.action.ActionDef;
import pddl4j.exp.term.Term;
import pddl4j.exp.term.Variable;

public class Pddl2Uml {

	private static void print_usage(){
		System.out.print(
				"Usage:\n" +
				"    pddl2uml <domain> [<problem> ...]");
	}
	
	public static void main(String[] args) {
		if (args.length == 0) {
			print_usage();
			return;
		}
		
		File domain_file = new File(args[0]);
		List<File> problem_files = new LinkedList<>();
		
		for (int i = 1; i < args.length; i++ ){
			problem_files.add(new File(args[i]));
		}
		
		Pddl2Uml pddl2uml = new Pddl2Uml();
		pddl2uml.run(domain_file, problem_files);
	}
	
	private ResourceSet resSet;
	private static final String LIBRARY_PATH = "libs/org.eclipse.uml2.uml.resources.jar";
	private Model PRIMITIVE_TYPES;
	private Type UML_TYPE_BOOLEAN;
	private UMLFactory FACTORY;
	private Package rootPkg;
	private Domain domain;
	private Map<RequireKey, Boolean> reqs;
	
	public Pddl2Uml(){
		resSet = new org.eclipse.emf.ecore.resource.impl.ResourceSetImpl();
		
		
		Map<String, Object> pkgReg = resSet.getPackageRegistry();
		pkgReg.put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		
		// Register extension
		resSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
		
		Map<URI, URI> uriMap = resSet.getURIConverter().getURIMap();
		URI libraryURI = URI.createURI("jar:"+ (new File(LIBRARY_PATH)).toURI() + "!/" ); 
		
		uriMap.put(URI.createURI(UMLResource.LIBRARIES_PATHMAP), libraryURI.appendSegment("libraries").appendSegment(""));
		uriMap.put(URI.createURI(UMLResource.METAMODELS_PATHMAP), libraryURI.appendSegment("metamodels").appendSegment(""));
		uriMap.put(URI.createURI(UMLResource.PROFILES_PATHMAP), libraryURI.appendSegment("profiles").appendSegment(""));
		
		Resource primTypesRes = resSet.getResource(URI.createURI(UMLResource.UML_PRIMITIVE_TYPES_LIBRARY_URI), true);
		PRIMITIVE_TYPES = (Model) primTypesRes.getContents().get(0);
		UML_TYPE_BOOLEAN = PRIMITIVE_TYPES.getOwnedType("Boolean");
		FACTORY = UMLFactory.eINSTANCE;
	}
	
	private Map<RequireKey, Boolean> getDomainRequirements(Domain domain){
		Map<RequireKey, Boolean> res = new HashMap<>();
		for (Iterator<RequireKey> reqIter = domain.requirementsIterator(); reqIter.hasNext();){
			RequireKey req = reqIter.next();
			res.put(req, true);
		}
		return res;		
	}
	
	private Package transformDomain(){
		
		String domain_name = domain.getDomainName();
		rootPkg.setName(domain_name);
		
		reqs = getDomainRequirements(domain);
		
		
		
		if (!typing){
			System.out.println("Typing not supported by domain");
			System.out.println("Processing predicates...");
			Class object = root.createOwnedClass("Object", false);
			for (Iterator<AtomicFormula> predIter = domain.predicatesIterator(); predIter.hasNext();){
				AtomicFormula pred = predIter.next();
				System.out.print(pred.getPredicate() + "(" + pred.getArity() + ")" + " -> ");
				switch  (pred.getArity()){
				case 1:{
					System.out.println("boolean attribute");
					object.createOwnedAttribute(pred.getPredicate(), UML_TYPE_BOOLEAN);
					
				} break;
				case 2:{
					System.out.println("association");				
					Association assoc = object.createAssociation(false, AggregationKind.NONE_LITERAL, pred.getPredicate(), 0, -1, object, 
											 false, AggregationKind.NONE_LITERAL, "x", 0, -1);
					
					assoc.setName(pred.getPredicate());

				} break;
				default:{
					System.out.println("not supported");
				}
				}
				
				
				//object.createOwnedOperation(pred.getPredicate(), ownedParameterNames, ownedParameterTypes)
				/*
				for (Iterator<Term> termIter = pred.iterator(); termIter.hasNext();){
					Term term = termIter.next();
					System.out.println(term.toString() + "..." + term.toTypedString());
					
				}*/
			}
			
			System.out.println("Processing actions...");
			
			for (Iterator<ActionDef> actIter = domain.actionsIterator(); actIter.hasNext();){
				ActionDef act = actIter.next();
				System.out.print(act.getName() + "(");
				
				Operation op = object.createOwnedOperation(act.getName(), null, null);
				for (Iterator<Term> termIter =  act.iterator(); termIter.hasNext();){
					Term term = termIter.next();
					System.out.print(term.toString() + (termIter.hasNext()?", ":""));
					op.createOwnedParameter(term.toString(), object);
				}
				System.out.println(")");
				Constraint pre = op.createPrecondition(act.getName() + ".pre");
				
				LiteralString specPre = FACTORY.createLiteralString();
				specPre.setValue("{OCL} " + act.toString());
				pre.setSpecification(specPre);
				//ValueSpecification preSpec = pre.createSpecification(null, UML_TYPE_BOOLEAN, );
				
			}
		}
		
		return root;
	}
	
	public int run(File domain_file, List<File> problem_files){
		Parser pddlParser = new Parser(Parser.getDefaultOptions());
		//System.out.println(pddlParser.getOptions());
		//System.out.println(Parser.getDefaultOptions());
	
		try {
			System.out.println("Try to parse file: \"" + domain_file.getAbsolutePath() + "\"");
			domain = pddlParser.parse(domain_file.getAbsoluteFile());
		} catch (FileNotFoundException e) {
			System.out.println("Domain file not found: " + domain_file.getAbsolutePath());
			e.printStackTrace();
			return -1;
		}
		
		rootPkg = FACTORY.createPackage();
		rootPkg.createPackageImport(PRIMITIVE_TYPES);
		
		transformDomain(domain);
		
		Resource domainUML = resSet.createResource(URI.createURI(domain.getDomainName()+".uml"));
		domainUML.getContents().add(rootPkg);
		
		try {
			domainUML.save(null);
		} catch (IOException e) {
			System.out.println("Can't save model");
			e.printStackTrace();
		}
		
		return 0;
	}

}
