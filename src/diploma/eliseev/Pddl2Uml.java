package diploma.eliseev;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.uml2.uml.Package;

import diploma.eliseev.expr.OCLExprTranslator;
import diploma.eliseev.expr.PDDLExprTranslator;
import diploma.eliseev.expr.ExprTranslator;
import pddl4j.PDDLObject;
import pddl4j.Parser;

public class Pddl2Uml {

	private static void print_usage(){
		System.out.print(
				"Usage:\n" +
				"    pddl2uml [options] <domain> [<problem> ...]\n" +
				"Options:\n" +
				"    -o output_dir\n    write models to specified directory\n" + 
				"    -c                 use package composition instead of package import\n" +
				"    -ocl               try to translate PDDL expressions to OCL\n"
				);
	}
	
	public static void main(String[] args) {		
		List<File> inputFiles = new LinkedList<>();
		Map<String, Object> options = new HashMap<>(); 
		try {
			for (int i = 0; i < args.length; i++ ){
				if (args[i].equals("-o")){
					i++;
					options.put("rootdir", args[i]);
				}
				else if (args[i].equals("-c")){
					options.put("composition", new Boolean(true));
				}
				else if (args[i].equals("-ocl")){
					options.put("useocl", new Boolean(true));
				}
				else{
					inputFiles.add(new File(args[i]));
				}
			}
		}
		catch (IndexOutOfBoundsException ex){
			print_usage();
			return;
		}
		
		if (inputFiles.size() == 0){
			print_usage();
			return;
		}
		
		Pddl2Uml pddl2uml = new Pddl2Uml(options);
		pddl2uml.run(inputFiles.get(0), inputFiles.subList(1, inputFiles.size()));
	}
	
	Map<String, Object> options;
	String rootDir;
	ExprTranslator exprTranslator;
	Boolean useComposition;
	
	public Pddl2Uml(Map<String, Object> options){
		this.options = options;
		if (options.containsKey("rootdir")) this.rootDir = (String) options.get("rootdir");
		if (options.containsKey("useocl") && (Boolean) options.get("useocl") == true){
			this.exprTranslator = new OCLExprTranslator();
		}
		else {
			this.exprTranslator = new PDDLExprTranslator();
		}
		if (options.containsKey("composition")){
			this.useComposition = (Boolean) options.get("composition");
		}
		else{
			this.useComposition = false;
		}
	}
	
	public int run(File domainFile, List<File> problemFiles){
		Parser pddlParser = new Parser(Parser.getDefaultOptions());
		
		PDDLObject domain;
		try {
			System.out.println("Try to parse file: \"" + domainFile.getAbsolutePath() + "\"");
			domain = pddlParser.parse(domainFile.getAbsoluteFile());
		} catch (FileNotFoundException e) {
			System.out.println("Domain file not found: " + domainFile.getAbsolutePath());
			e.printStackTrace();
			return -1;
		}
		
		DomainTranslator domTranslator = new DomainTranslator(exprTranslator);
		Package domPackage = domTranslator.translate(domain);
		
		if (rootDir == null) rootDir = domain.getDomainName();
		String domUMLFileName = domain.getDomainName() + ".uml";
		
		if (!useComposition){
			String oldPath = System.setProperty("user.dir", new File(rootDir).getAbsolutePath());
			try{
				AbstractTranslator.saveToFile(domUMLFileName, domPackage);
				System.out.println("Domain model saved to \"" + domUMLFileName + "\"");
			} catch (IOException e) {
				System.out.println("Can't save model to \"" + domUMLFileName + "\"");
				e.printStackTrace();
			}
			finally{
				System.setProperty("user.dir", oldPath);
			}
		}
		
		ProblemTranslator probTranslator =  new ProblemTranslator(domPackage, exprTranslator);
		for (File problemFile : problemFiles){
			PDDLObject problem;
			System.out.println();
			try {
				System.out.println("Try to parse file: \"" + problemFile.getAbsolutePath() + "\"");
				problem = pddlParser.parse(problemFile);
			} catch (FileNotFoundException e) {
				System.out.println("Problem file not found: \"" + problemFile.getAbsolutePath() + "\"");
				e.printStackTrace();
				continue;
			}
			
			PDDLObject fullProblem = pddlParser.link(domain, problem);
			Package probPackage = probTranslator.translate(fullProblem);
			String probUMLFileName = problem.getProblemName() + ".uml";
			if (!useComposition){
				probPackage.createPackageImport(domPackage);
				
				String oldPath = System.setProperty("user.dir", new File(rootDir).getAbsolutePath());
				try {
					AbstractTranslator.saveToFile(probUMLFileName, probPackage);		
					System.out.println("Problem model saved to \"" + probUMLFileName + "\"");
				} catch (IOException e) {
					System.out.println("Can't save model to \"" + probUMLFileName + "\"");
					e.printStackTrace();
					continue;
				}
				finally{
					System.setProperty("user.dir", oldPath);
				}
			}
			else{
				domPackage.getPackagedElements().add(probPackage);
				System.out.println("Problem model (" + probPackage.getName() + ") added to domain model");
			}
		}
		
		if (useComposition){
			String oldPath = System.setProperty("user.dir", new File(rootDir).getAbsolutePath());
			try {
				AbstractTranslator.saveToFile(domUMLFileName, domPackage);
				System.out.println("Model saved to \"" + domUMLFileName + "\"");
			} catch (IOException e) {
				System.out.println("Can't save model to \"" + domUMLFileName + "\"");
				e.printStackTrace();
				return -1;
			}
			finally{
				System.setProperty("user.dir", oldPath);
			}
			
		}
			
		return 0;
	}

}
