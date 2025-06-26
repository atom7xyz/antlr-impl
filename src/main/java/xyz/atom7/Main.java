package xyz.atom7;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import xyz.atom7.api.interpreter.Interpreter;
import xyz.atom7.api.interpreter.Program;
import xyz.atom7.api.parser.ParserHelper;
import xyz.atom7.api.parser.error.ParserError;
import xyz.atom7.api.parser.semantic.SemanticError;
import xyz.atom7.api.parser.semantic.SemanticWarning;
import xyz.atom7.api.tracer.Tracer;
import xyz.atom7.interpreter.asm8088.ASM8088Program;
import xyz.atom7.interpreter.ijvm.IJVMProgram;
import xyz.atom7.parser.asm8088.ASM8088ParserHelper;
import xyz.atom7.parser.ijvm.IJVMParserHelper;
import xyz.atom7.tracer.asm8088.ASM8088Tracer;
import xyz.atom7.tracer.ijvm.IJVMTracer;

import java.nio.file.Path;
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
    private static final String FLAG_8088 = "-asm8088";
    private static final String FLAG_FILE = "-file";
    private static final String FLAG_PARSE = "-parse";
    private static final String FLAG_INTERPRET = "-interpret";
    private static final String FLAG_TRACE = "-trace";

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
        boolean trace = options.containsKey("trace");
        Utils.DEBUG = options.containsKey("debug");

        switch (mode) {
            case "parse":
                handleParser(lang, filePath);
                break;
            case "interpret":
                handleParser(lang, filePath);
                handleInterpreter(lang, filePath, trace);
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
        
        for (int i = 0; i < args.length; i++)
        {
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
                case FLAG_TRACE:
                    options.put("trace", "true");
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
    private static void handleInterpreter(String lang, String filePath, boolean trace) throws Exception
    {
        switch (lang) {
            case "ijvm": {
                interpretIJVM(filePath, trace);
                break;
            }
            case "8088": {
                interpret8088(filePath, trace);
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
        System.err.println("  -ijvm, -asm8088          Specify the language");
        System.err.println("  -file <path>             Path to the source file");
        System.err.println("  -d, -debug               Enable debug mode");
        System.err.println("  -tracer                  Enable tracer for step-by-step execution view");
    }

    private static void parse(ParserHelper<?> helper, String filePath) throws Exception
    {
        var result = helper.parseFile(filePath);

        var parserError = result.getParserErrors();
        var semanticWarning = result.getSemanticWarnings();
        var semanticError = result.getSemanticErrors();

        debugln("--------------------------------");
        debug("Parser errors: ");

        debugln(parserError.isEmpty() ? "NONE" : "");

        for (ParserError message : parserError)
            debugln(message.getFormattedMessage());

        debugln("--------------------------------");
        debug("Semantic warnings: ");

        debugln(semanticWarning.isEmpty() ? "NONE" : "");

        for (SemanticWarning message : semanticWarning)
            debugln(message.getFormattedMessage());

        debugln("--------------------------------");
        debug("Semantic errors: ");

        debugln(semanticError.isEmpty() ? "NONE" : "");

        for (SemanticError message : semanticError)
            debugln(message.getFormattedMessage());

        debugln("--------------------------------");
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
        parse(helper, filePath);
    }

    /**
     * Parses an 8088 file
     * 
     * @param filePath The path to the file to parse
     * @throws Exception If an error occurs
     */
    private static void parse8088(String filePath) throws Exception
    {
        ASM8088ParserHelper helper = new ASM8088ParserHelper();
        parse(helper, filePath);
    }

    /**
     * Interprets an IJVM file
     * 
     * @param filePath The path to the file to interpret
     * @throws Exception If an error occurs
     */
    private static void interpretIJVM(String filePath, boolean trace) throws Exception
    {
        var program = new IJVMProgram<>();
        var tracer = new IJVMTracer(program);

        init(program, tracer, filePath, trace);
    }

    /**
     * Interprets an 8088 file
     * 
     * @param filePath The path to the file to interpret
     * @throws Exception If an error occurs
     */
    private static void interpret8088(String filePath, boolean trace) throws Exception
    {
        var program = new ASM8088Program<>();
        var tracer = new ASM8088Tracer(program);

        init(program, tracer, filePath, trace);
    }

    /**
     * Initializes the interpreter with the given file and tracer
     *
     * @param interpreter The interpreter to initialize
     * @param tracer The tracer to use for debugging
     * @param filePath The path to the file to interpret
     * @param trace Whether to enable tracing
     * @throws Exception If an error occurs
     */
    private static void init(Interpreter<?> interpreter,
                             Tracer<?, ?, ?> tracer,
                             String filePath,
                             boolean trace) throws Exception
    {
        Path path = Paths.get(filePath);
        CharStream chars = CharStreams.fromPath(path);
        interpreter.init(String.valueOf(chars));

        if (trace) {
            Utils.TRACER = tracer;
        }

        interpreter.execute();
    }
}