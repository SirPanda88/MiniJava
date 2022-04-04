package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.ContextualAnalysis.Identification;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.AbstractSyntaxTrees.*;

public class Compiler {

    public static void main (String[] args) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(args[0]);
        } catch (FileNotFoundException e) {
            System.out.println("Input file: " + args[0] + " not found");
            System.exit(3);
        }

        ErrorReporter errorReporter = new ErrorReporter();
        Scanner scanner = new Scanner(inputStream, errorReporter);
        Parser parser = new Parser(scanner, errorReporter);

        System.out.println("Beginning syntactic analysis: ...");
        AST ast = parser.parse();
        System.out.println("Syntactic analysis complete:");
        if (errorReporter.hasErrors()) {
            System.out.println("Syntactically invalid miniJava program");
            System.exit(4);
        } else {
            System.out.println("Syntactically valid miniJava program");
//            new ASTDisplay().showTree(ast);
            System.out.println("Beginning identification: ...");
            Identification identification = new Identification(ast, errorReporter);
            identification.identify();
            System.out.println("Identification complete: ");
            if (errorReporter.hasErrors()) {
                System.out.println("Identification unsuccessful - contextually invalid miniJava program");
                System.exit(4);
            } else {
                System.out.println("Identification successful");
                System.out.println("Beginning type checking: ...");
            }
            System.exit(0);
        }
    }
}
