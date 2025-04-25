package xyz.atom7;

import org.antlr.v4.runtime.CharStreams;
import xyz.atom7.api.parser.error.ParserError;
import xyz.atom7.interpreter.IJVMProgram;
import xyz.atom7.parser.IJVMParserHelper;
import xyz.atom7.parser.semantic.SemanticError;
import xyz.atom7.parser.semantic.SemanticWarning;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static xyz.atom7.Utils.debug;
import static xyz.atom7.Utils.debugln;

/**
 * Main application entry point for the IJVM compiler.
 */
public class Main
{
    private static final String FLAG_DEBUG = "-d";
    private static final String FLAG_DEBUG_LONG = "-debug";
    private static final String FLAG_IJVM = "-ijvm";
    private static final String FLAG_8088 = "-8088";
    private static final String FLAG_FILE = "-file";
    private static final String FLAG_PARSE = "-parse";
    private static final String FLAG_INTERPRET = "-interpret";

    public static void main(String[] args) throws Exception
    {
        Map<String, String> options = parseArgs(args);
        
        if (!validateOptions(options)) {
            printHelp();
            return;
        }
        
        String lang = options.getOrDefault("lang", "");
        String mode = options.getOrDefault("mode", "");
        String filePath = options.getOrDefault("file", "");
        Utils.DEBUG = options.containsKey("debug");

        switch (mode) {
            case "parse":
                handleParser(lang, filePath);
                break;
            case "interpret":
                handleParser(lang, filePath);
                handleInterpreter(lang, filePath);
                break;
            default:
                printHelp();
        }
    }
    
    /**
     * Parses the command line arguments
     * 
     * @param args The command line arguments
     * @return A map of options and their values
     */
    private static Map<String, String> parseArgs(String[] args)
    {
        Map<String, String> options = new HashMap<>();
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            switch (arg) {
                case FLAG_DEBUG:
                case FLAG_DEBUG_LONG:
                    options.put("debug", "true");
                    break;
                case FLAG_IJVM:
                    options.put("lang", "ijvm");
                    break;
                case FLAG_8088:
                    options.put("lang", "8088");
                    break;
                case FLAG_PARSE:
                    options.put("mode", "parse");
                    break;
                case FLAG_INTERPRET:
                    options.put("mode", "interpret");
                    break;
                case FLAG_FILE:
                    if (i + 1 < args.length) {
                        options.put("file", args[++i]);
                    }
                    break;
            }
        }
        
        return options;
    }
    
    /**
     * Validates the command line options
     * 
     * @param options The map of options and their values
     * @return True if the options are valid, false otherwise
     */
    private static boolean validateOptions(Map<String, String> options)
    {
        return options.containsKey("lang") && 
               options.containsKey("mode") && 
               options.containsKey("file");
    }

    /**
     * Handles the parser
     * 
     * @param lang The language to parse
     * @param filePath The path to the file to parse
     * @throws Exception If an error occurs
     */
    private static void handleParser(String lang, String filePath) throws Exception
    {
        switch (lang) {
            case "ijvm": {
                parseIJVM(filePath);
                break;
            }
            case "8088": {
                parse8088(filePath);
                break;
            }
            default:
                printHelp();
        }
    }

    /**
     * Handles the interpreter
     * 
     * @param lang The language to interpret
     * @param filePath The path to the file to interpret
     * @throws Exception If an error occurs
     */
    private static void handleInterpreter(String lang, String filePath) throws Exception
    {
        switch (lang) {
            case "ijvm": {
                interpretIJVM(filePath);
                break;
            }
            case "8088": {
                parse8088(filePath);
                break;
            }
            default:
                printHelp();
        }
    }

    /**
     * Prints the help message
     */
    private static void printHelp()
    {
        System.err.println("Usage: java -jar antlr-impl.jar [options]");
        System.err.println("Options:");
        System.err.println("  -parse, -interpret       Specify the mode");
        System.err.println("  -ijvm, -8088             Specify the language");
        System.err.println("  -file <path>             Path to the source file");
        System.err.println("  -d, -debug               Enable debug mode");
    }

    /**
     * Parses an IJVM file
     * 
     * @param filePath The path to the file to parse
     * @throws Exception If an error occurs
     */
    private static void parseIJVM(String filePath) throws Exception
    {
        IJVMParserHelper helper = new IJVMParserHelper();
        var result = helper.parseFile(filePath);
        
        var parserError = result.getParserErrors();
        var semanticWarning = result.getSemanticWarnings();
        var semanticError = result.getSemanticErrors();

        debugln("--------------------------------");
        debug("Parser errors: ");
        
        if (parserError.isEmpty()) {
            debugln("NONE");
        }
        
        for (ParserError message : parserError) {
            debugln(message.getFormattedMessage());
        }

        debugln("--------------------------------");
        debug("Semantic warnings: ");

        if (semanticWarning.isEmpty()) {
            debugln("NONE");
        }
        
        for (SemanticWarning message : semanticWarning) {
            debugln(message.getFormattedMessage());
        }

        debugln("--------------------------------");
        debug("Semantic errors: ");

        if (semanticError.isEmpty()) {
            debugln("NONE");
        }
        
        for (SemanticError message : semanticError) {
            debugln(message.getFormattedMessage());
        }
        debugln("--------------------------------");
    }

    /**
     * Parses an 8088 file
     * 
     * @param filePath The path to the file to parse
     * @throws Exception If an error occurs
     */
    private static void parse8088(String filePath) throws Exception
    {
        // todo
    }

    /**
     * Interprets an IJVM file
     * 
     * @param filePath The path to the file to interpret
     * @throws Exception If an error occurs
     */
    private static void interpretIJVM(String filePath) throws Exception
    {
        var program = new IJVMProgram<>();
        program.init(String.valueOf(CharStreams.fromPath(Paths.get(filePath))));
        program.execute();
    }

    /**
     * Interprets an 8088 file
     * 
     * @param filePath The path to the file to interpret
     * @throws Exception If an error occurs
     */
    private static void interpret8088(String filePath) throws Exception
    {
        // todo
    }

}