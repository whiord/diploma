package diploma.eliseev;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.uml2.uml.Package;
import pddl4j.PDDLObject;
import pddl4j.Parser;

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
		
		File domainFile = new File(args[0]);
		List<File> problemFiles = new LinkedList<>();
		
		for (int i = 1; i < args.length; i++ ){
			problemFiles.add(new File(args[i]));
		}
		
		Pddl2Uml pddl2uml = new Pddl2Uml();
		pddl2uml.run(domainFile, problemFiles);
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
		
		DomainTranslator domTranslator = new DomainTranslator();
		Package domPackage = domTranslator.translate(domain);
		
		String rootUMLPath = domain.getDomainName();
		String domUMLFileName = domain.getDomainName() + ".uml";
		
		try{
			String oldPath = System.setProperty("user.dir", new File(rootUMLPath).getAbsolutePath());
			AbstractTranslator.saveToFile(domUMLFileName, domPackage);
			System.setProperty("user.dir", oldPath);
		} catch (IOException e) {
			System.out.println("Can't save model to \"" + domUMLFileName + "\"");
			e.printStackTrace();
		}
		System.out.println("Domain model saved to \"" + domUMLFileName + "\"");
		
		ProblemTranslator probTranslator =  new ProblemTranslator(domPackage);
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
			probPackage.createPackageImport(domPackage);
			try {
				String oldPath = System.setProperty("user.dir", new File(rootUMLPath).getAbsolutePath());
				AbstractTranslator.saveToFile(probUMLFileName, probPackage);
				System.setProperty("user.dir", oldPath);
			} catch (IOException e) {
				System.out.println("Can't save model to \"" + probUMLFileName + "\"");
				e.printStackTrace();
			}
			System.out.println("Problem model saved to \"" + probUMLFileName + "\"");
		}
			
		return 0;
	}

}
