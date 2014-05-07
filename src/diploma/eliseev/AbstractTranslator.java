package diploma.eliseev;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
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
	static Model PRIMITIVE_TYPES;
	static Type UML_TYPE_BOOLEAN;
	static UMLFactory FACTORY;
	
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
	
	public static String getClassNameForString(String name){
		return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
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
		String clName = getClassNameForString(type.getImage());
		
		Class res = getClassByName(pkg, clName, false);
		
		if (res != null) return res;
		res = extractClassHierarchy(pkg, type);
				
		return res;	
	}
	
	public static Class getClassByName(Package pkg, String clName, boolean create) {
		clName = getClassNameForString(clName);
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
	
	public static InstanceSpecification getInstSpecificationByName(Package pkg, String name, Class cl){
		for (NamedElement elem : pkg.getMembers()){
			if (elem.getName().equals(name) && elem instanceof InstanceSpecification) 
				return (InstanceSpecification) elem;
			
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
