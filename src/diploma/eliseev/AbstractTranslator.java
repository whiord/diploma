package diploma.eliseev;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.InstanceSpecification;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

import pddl4j.PDDLObject;

public abstract class AbstractTranslator {
	static final String LIBRARY_PATH = "libs/org.eclipse.uml2.uml.resources.jar";
	static final String GLOBAL_CLASS_NAME = "Global";
	
	static ResourceSet resSet;
	public static Model PRIMITIVE_TYPES;
	public static Type UML_TYPE_BOOLEAN;
	public static UMLFactory FACTORY;
	
	{
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
	
	Package rootPkg;
	
	public AbstractTranslator(){
		rootPkg = FACTORY.createPackage();
		rootPkg.createPackageImport(PRIMITIVE_TYPES);
	}
	
	public abstract Package translate(PDDLObject smth);
	
	public static String getClassName(String name){
		return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
	}
	
	public static String getAssociationName(String name, String end1, String end2){
		return name.toLowerCase();
		//return name.toLowerCase() + "_" + end1.toLowerCase() + "_" + end2.toLowerCase();
	}
	
	public static Association getAssociation(Package pkg, String name, Class end1, Class end2){
		String assocName = getAssociationName(name, end1.getName(), end2.getName());

		for (NamedElement elem : pkg.getMembers()){
			if (elem.getName().equals(assocName) && elem instanceof Association){
				Association assoc = (Association) elem;
				EList<Property> propList = assoc.getOwnedEnds();
				
				if (propList.size() < 2) continue;
				
				if ( end1.conformsTo(propList.get(0).getType()) &&
					 end2.conformsTo(propList.get(1).getType())){
					return assoc;
				}
			}
		}
		return null;
	}
	
	static Class extractClassHierarchy(Package pkg, pddl4j.exp.type.Type type){
		Set<pddl4j.exp.type.Type> types = type.getAllSuperTypes();
		types.add(type);
		types.addAll(type.getAllSubTypes());
		Class res = getClassByName(pkg, type.getImage(), true);
		
		for (pddl4j.exp.type.Type subType : types){
			Class subClass = getClassByName(pkg, subType.getImage(), true);
			
			for (pddl4j.exp.type.Type superType : subType.getSuperTypes()){
				if (superType.getImage().equals(pddl4j.exp.type.Type.OBJECT_SYMBOL)){
					continue;
				}
				
				Class superClass = getClassByName(pkg, superType.getImage(), true);
				subClass.createGeneralization(superClass);
				
				System.out.println("Generalization: " + superClass.getName() + "  <---  " + subClass.getName());
			}
		}
		
		return res;
	}	
	
	public static Class getClassForType(Package pkg, pddl4j.exp.type.Type type){
		String clName = getClassName(type.getImage());
		
		Class res = getClassByName(pkg, clName, false);
		
		if (res != null) return res;
		res = extractClassHierarchy(pkg, type);
				
		return res;	
	}
	
	public static Class getClassByName(Package pkg, String clName, boolean create) {
		clName = getClassName(clName);
		EList<NamedElement> list = pkg.getMembers();
		for (NamedElement elem : list){
			if (elem.getName().equals(clName) && elem instanceof Class) return (Class) elem; 
		}
		if (create) return pkg.createOwnedClass(clName, false);
		return null;
	}
	
	public static Class getClassByName(Package pkg, String clName){
		return getClassByName(pkg, clName, false);
	}
	
	public static InstanceSpecification getClassInstSpecification(Package pkg, String name, Class cl){
		for (NamedElement elem : pkg.getMembers()){
			if (elem.getName().equals(name) && elem instanceof InstanceSpecification){
				InstanceSpecification spec = (InstanceSpecification) elem;
				if (spec.getClassifier(cl.getName())== cl) return spec;
			}			
		}
		if (cl != null){
			InstanceSpecification res = FACTORY.createInstanceSpecification();
			res.setName(name);
			res.getClassifiers().add(cl);
			pkg.getPackagedElements().add(res);
			return res;
		}
		
		return null;
	}
	
	public static InstanceSpecification getAssocInstSpecification(Package pkg, String name, Association assoc){
		for (NamedElement elem : pkg.getMembers()){
			if (elem.getName().equals(name) && elem instanceof InstanceSpecification){
				InstanceSpecification spec = (InstanceSpecification) elem;
				if (spec.getClassifier(assoc.getName())== assoc) return spec;
			}
		}
		if (assoc != null){
			InstanceSpecification res = FACTORY.createInstanceSpecification();
			res.setName(name);
			res.getClassifiers().add(assoc);
			pkg.getPackagedElements().add(res);
			return res;
		}
		
		return null;
	}
	
	public static Property getClassPropertyByName(Class cl, String name){
		for (Property prop: cl.getAllAttributes()){
			if (prop.getName().equals(name)){
				return prop;
			}
		}
		return null;
	}
		
	public static void saveToFile(String fName, Package pkg) throws IOException{
		Resource saveResource = resSet.createResource(URI.createURI(fName));
		saveResource.getContents().add(pkg);
		
		saveResource.save(null);
		
	}
}
