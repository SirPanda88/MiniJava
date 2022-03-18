package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

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

        System.out.println("Syntactic analysis: ... ");
        AST ast = parser.parse();
        System.out.println("Syntactic analysis complete:");
        if (errorReporter.hasErrors()) {
            System.out.println("Invalid miniJava program");
            System.exit(4);
        } else {
            System.out.println("Valid miniJava program");
            new ASTDisplay().showTree(ast);
            System.exit(0);
        }
    }
}
