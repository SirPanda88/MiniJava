package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.CodeGenerator.CodeGenerator;
import miniJava.ContextualAnalyzer.Identification;
import miniJava.ContextualAnalyzer.TypeChecking;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.AbstractSyntaxTrees.*;

public class Compiler {

    public static void main (String[] args) {

        // my compiler
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
        // turn trace on in Parser for debugging

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
            System.out.println("Identification complete:");
            if (errorReporter.hasErrors()) {
                System.out.println("Identification unsuccessful - contextually invalid miniJava program");
                System.exit(4);
            } else {
                System.out.println("Identification successful");
                System.out.println("Beginning type checking: ...");
                TypeChecking typeChecker = new TypeChecking(ast, errorReporter);
                typeChecker.typeCheck();
                System.out.println("Type checking complete:");
                if (errorReporter.hasErrors()) {
                    System.out.println("Type checking unsuccessful - contextually invalid miniJava program");
                    System.exit(4);
                } else {
                    System.out.println("Type checking successful - contextually valid miniJava program");
                    System.out.println("Beginning code generation: ...");
                    CodeGenerator codeGenerator = new CodeGenerator(ast, errorReporter);
                    codeGenerator.generate(args[0]);
                    System.out.println("Code generation complete:");
                    if (errorReporter.hasErrors()) {
                        System.out.println("Code generation unsuccessful");
                        System.exit(4);
                    } else {
                        System.out.println("Code generation successful");
                    }
                }
            }
            System.exit(0);
        }


        // script for pa2
//        File folder = new File("/Users/ezhan/Comp520/pa2_tests");
//        File[] listOfFiles = folder.listFiles();
//        int numFiles = 0;
//        int failCount = 0;
//        ErrorReporter errorReporter = new ErrorReporter();
//
//        for (int i = 0; i < listOfFiles.length; i++) {
//            if (listOfFiles[i].isFile()) {
//                System.out.println("(" + (++numFiles) + ") + Testing: " + listOfFiles[i].getName());
//                AST ast = new Parser(new Scanner(new FileInputStream(listOfFiles[i]), errorReporter), errorReporter).parse();
//                try {
//                    new Identification(ast, errorReporter).identify();
//                    new TypeChecking(ast, errorReporter).typeCheck();
//                    System.out.println("PARSED");
//                } catch (Exception e) {
//                    failCount++;
//                    e.printStackTrace();
//                    System.out.println("PROBLEMO");
//                }
//            }
//        }
    }
}


