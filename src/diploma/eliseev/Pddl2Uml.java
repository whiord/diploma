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
		
		File domain_file = new File(args[0]);
		List<File> problem_files = new LinkedList<>();
		
		for (int i = 1; i < args.length; i++ ){
			problem_files.add(new File(args[i]));
		}
		
		Pddl2Uml pddl2uml = new Pddl2Uml();
		pddl2uml.run(domain_file, problem_files);
	}
	
	public int run(File domain_file, List<File> problem_files){
		Parser pddlParser = new Parser(Parser.getDefaultOptions());
		
		PDDLObject domain;
		try {
			System.out.println("Try to parse file: \"" + domain_file.getAbsolutePath() + "\"");
			domain = pddlParser.parse(domain_file.getAbsoluteFile());
		} catch (FileNotFoundException e) {
			System.out.println("Domain file not found: " + domain_file.getAbsolutePath());
			e.printStackTrace();
			return -1;
		}
		
		DomainTranslator domTranslator = new DomainTranslator();
		Package domPackage = domTranslator.translate(domain);
		String domUMLFileName = domain.getDomainName()+".uml";
		try{
			AbstractTranslator.saveToFile(domUMLFileName, domPackage);
		} catch (IOException e) {
			System.out.println("Can't save model");
			e.printStackTrace();
		}
		System.out.println("Model saved to \""+domUMLFileName+"\"");
			
		return 0;
	}

}
