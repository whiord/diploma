package diploma.eliseev;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Class;

import pddl4j.Domain;
import pddl4j.Parser;
import pddl4j.RequireKey;
import pddl4j.exp.AtomicFormula;
import pddl4j.exp.action.Action;
import pddl4j.exp.action.ActionDef;
import pddl4j.exp.action.ActionID;
import pddl4j.exp.term.Term;

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
	private pddl4j.exp.type.Type rootType;
	private boolean rootTypeChecked;
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
	
	private void getDomainRequirements(){
		reqs = new HashMap<>();
		for (RequireKey req: RequireKey.values()){
			reqs.put(req, false);
		}
		for (Iterator<RequireKey> reqIter = domain.requirementsIterator(); reqIter.hasNext();){
			RequireKey req = reqIter.next();
			reqs.put(req, true);
		} 		
	}
	
	private void transformTypes(){
		if (!reqs.get(RequireKey.TYPING)){
			System.out.println("Typing not supported by domain!");
			return;
		}
		System.out.println("Types will be extracted and created on fly");
		rootTypeChecked = false;
	}
	
	private void transformPredicates(){
		System.out.println("Processing predicates...");
				
		for (Iterator<AtomicFormula> predIter = domain.predicatesIterator(); predIter.hasNext();){
			AtomicFormula pred = predIter.next();
			String predInfoString = pred.getPredicate() + "(" + pred.getArity() + ")" + " -> "; 
			
			if (pred.getArity() == 0) {
				System.out.println(predInfoString + "unknown");
				continue;
			}
			
			switch  (pred.getArity()){
			case 1:{
				
				Term firstTerm = pred.iterator().next();
				pddl4j.exp.type.Type firstType = firstTerm.getTypeSet().iterator().next();
				
				Class predOwner = getClassForType(firstType);			
				predOwner.createOwnedAttribute(pred.getPredicate(), UML_TYPE_BOOLEAN);
				
				predInfoString += "boolean attribute of " + predOwner.getName();
				
			} break;
			case 2:{				
				Iterator<Term> termIter = pred.iterator();
				Term firstTerm = termIter.next(),
					 secondTerm = termIter.next();
				
				predInfoString += "association of ";
								
				for (pddl4j.exp.type.Type firstType : firstTerm.getTypeSet()){
					for (pddl4j.exp.type.Type secondType : secondTerm.getTypeSet()){
						Class assocOwner = getClassForType(firstType),
							  otherEnd = getClassForType(secondType);
						
						Association assoc = assocOwner.createAssociation(false, AggregationKind.NONE_LITERAL, pred.getPredicate(), 0, -1, otherEnd, 
								 false, AggregationKind.NONE_LITERAL, "x", 0, -1);
		
						assoc.setName(pred.getPredicate());
						
						predInfoString += assocOwner.getName() + " ";
					}
				}
				
				
				
			} break;
			default:{
				predInfoString += "n-ary association not supported";
			}
			}
			System.out.println(predInfoString);
		}
	}

	
	private void transformActions(){
		System.out.println("Processing actions...");
			
		for (Iterator<ActionDef> actIter = domain.actionsIterator(); actIter.hasNext();){
			ActionDef act = actIter.next();
			if (act.getActionID() == ActionID.DURATIVE_ACTION){
				System.out.println(act.getName() + " -> " + "durative action not supported");
			}
			
			String actInfoString = act.getName() + "(";
			
			Iterator<Term> termIter =  act.iterator();
			Term firstTerm = termIter.next();
			
			Class opOwner = getClassForType(firstTerm.getTypeSet().iterator().next());
			Operation op = opOwner.createOwnedOperation(act.getName(), null, null);
			
			for (;termIter.hasNext();){
				Term nextTerm = termIter.next();
				Class nextArgClass = getClassForType(nextTerm.getTypeSet().iterator().next()); 
				 
				op.createOwnedParameter(nextTerm.toString(), nextArgClass);
				
				actInfoString += nextTerm.toString()+": "+nextArgClass.getName() + (termIter.hasNext()?", ":"");
			}
			
			System.out.println(actInfoString + ") -> " + "operation of " + opOwner.getName());

			Constraint pre = op.createPrecondition(act.getName() + ".precond"),
			           post = op.createPostcondition(act.getName() + ".postcond");
			
			LiteralString specPre = FACTORY.createLiteralString(),
					      specPost = FACTORY.createLiteralString();
			
			
			Action actCl = (Action) act;
			
			specPre.setValue("{PDDL} " + actCl.getPrecondition().toString());
			specPost.setValue("{PDDL}" + actCl.getEffect().toString());
			pre.setSpecification(specPre);
			post.setSpecification(specPost);
		}
	}
	
	private Class extractClassHierarchy(pddl4j.exp.type.Type type){
		String clName = getClassNameForString(type.getImage());
		Class res = rootPkg.createOwnedClass(clName, false);
		
		for (pddl4j.exp.type.Type superType : type.getSuperTypes()){
			if (superType.getImage().equals(pddl4j.exp.type.Type.OBJECT_SYMBOL)){
				rootType = superType;
				continue;
			}
			Class superClass = getClassByName(superType.getImage(), false);
			if (superClass == null) {
				System.out.println("Extracting type: " + type.getImage() + " --> " + superType.getImage());
				superClass = extractClassHierarchy(superType);
				res.createGeneralization(superClass);
			}
			
		}
		
		for (pddl4j.exp.type.Type subType : type.getSubTypes()){
			Class subClass = getClassByName(subType.getImage(), false);
			if (subClass == null){
				System.out.println("Extracting type: " + type.getImage() + " <-- " + subType.getImage());
				subClass = extractClassHierarchy(subType);
				subClass.createGeneralization(res);
			}
		}
				
		return res;
	}
	
	private void checkRootType(){
		rootTypeChecked = true;
		System.out.println("Performing root type check...");
		for (pddl4j.exp.type.Type subType: rootType.getAllSubTypes()){
			if (getClassByName(subType.getImage()) == null ){
				extractClassHierarchy(subType);
			}
		}
		
	}
	
	private String getClassNameForString(String name){
		return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
	}
	
	private Class getClassForType(pddl4j.exp.type.Type type){
		String clName = getClassNameForString(type.getImage());
		
		Class res = getClassByName(clName, false);
		
		if (res != null) return res;
		res = extractClassHierarchy(type);
		
		if (!rootTypeChecked && rootType != null){
			checkRootType();
		}
				
		return res;	
	}
	
	private Class getClassByName(String clName, boolean create) {
		clName = getClassNameForString(clName);
		EList<NamedElement> list = rootPkg.getMembers();
		for (NamedElement elem : list){
			if (elem.getName().equals(clName) && elem instanceof Class) return (Class) elem; 
		}
		if (create) return rootPkg.createOwnedClass(clName, false);
		return null;
	}
	
	private Class getClassByName(String clName){
		return getClassByName(clName, false);
	}

	private void transformDomain(){
		
		String domain_name = domain.getDomainName();
		rootPkg.setName(domain_name);
		
		getDomainRequirements();
		
		
		transformTypes();
		transformPredicates();
		transformActions();
		
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
		
		transformDomain();
		
		Resource domainUML = resSet.createResource(URI.createURI(domain.getDomainName()+".uml"));
		domainUML.getContents().add(rootPkg);
		
		try {
			domainUML.save(null);
		} catch (IOException e) {
			System.out.println("Can't save model");
			e.printStackTrace();
		}
		System.out.println("Model saved to \""+domainUML.getURI().toFileString()+"\"");
		
		return 0;
	}

}
