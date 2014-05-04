package diploma.eliseev;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.Package;


public class Test {

	public static void main(String[] args) {
		
		org.eclipse.emf.ecore.resource.ResourceSet resSet = new org.eclipse.emf.ecore.resource.impl.ResourceSetImpl();
		
		Map<String, Object> set = resSet.getPackageRegistry();
		
		UMLPackage.eINSTANCE.eClass();
		
		set.put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);

		resSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
		
		Map<URI, URI> uriMap = resSet.getURIConverter().getURIMap();
		
		File uml_resources_file = new File("./libs/org.eclipse.uml2.uml.resources.jar");
		
		URI uri = URI.createURI("jar:" + uml_resources_file.toURI() + "!/"); 
		
		uriMap.put(URI.createURI(UMLResource.LIBRARIES_PATHMAP), uri.appendSegment("libraries").appendSegment(""));
		uriMap.put(URI.createURI(UMLResource.METAMODELS_PATHMAP), uri.appendSegment("metamodels").appendSegment(""));
		uriMap.put(URI.createURI(UMLResource.PROFILES_PATHMAP), uri.appendSegment("profiles").appendSegment(""));
		
		Resource resource = resSet.createResource(URI.createURI("my1.uml"));
		
		UMLFactory factory = UMLFactory.eINSTANCE;
		Package root = factory.createPackage();
		root.setName("UMLPackage");
		
		
		Resource prim_types_res = resSet.getResource(URI.createURI(UMLResource.UML_PRIMITIVE_TYPES_LIBRARY_URI), true);
		Model prim_types = (Model) prim_types_res.getContents().get(0);
		
		root.createPackageImport(prim_types);
		
		Package sub = root.createNestedPackage("Subpackage");
		Class cl1 = sub.createOwnedClass("Class1", false);
		cl1.createOwnedAttribute("capacity", prim_types.getOwnedType("Integer"));
		resource.getContents().add(root);
		try {
			resource.save(null);
		} catch (IOException e) {
		
			e.printStackTrace();
		}
	}

}
