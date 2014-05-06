package diploma.eliseev;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

import pddl4j.PDDLObject;

public abstract class AbstractTranslator {
	static final String LIBRARY_PATH = "libs/org.eclipse.uml2.uml.resources.jar";
	static final String GLOBAL_CLASS_NAME = "global";
	
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
	Class globalClass;
	pddl4j.exp.type.Type rootType;
	
	public AbstractTranslator(){
		rootPkg = FACTORY.createPackage();
		rootPkg.createPackageImport(PRIMITIVE_TYPES);
	}
	
	public abstract Package translate(PDDLObject smth);
	
	public static void saveToFile(String fName, Package pkg) throws IOException{
		Resource saveResource = resSet.createResource(URI.createURI(fName));
		saveResource.getContents().add(pkg);
		
		saveResource.save(null);
		
	}
}
