package xyz.atom7.api.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

import java.io.IOException;
import java.nio.file.Paths;

public abstract class ParserHelper<T extends ParseResult<?>>
{
    /**
     * Parse an IJVM source file
     *
     * @param filePath Path to the IJVM source file
     * @return ParseResult with program context, parser errors, semantic errors, and semantic warnings
     * @throws IOException If the file cannot be read
     */
    public T parseFile(String filePath) throws IOException
    {
        CharStream input = CharStreams.fromPath(Paths.get(filePath));
        return parseStream(input);
    }

    /**
     * Parse an IJVM string with custom error formatting.
     *
     * @param code IJVM code string to parse
     * @return The parsed program result containing the tree and any errors
     */
    public T parseString(String code)
    {
        CharStream input = CharStreams.fromString(code);
        return parseStream(input);
    }

    /**
     * Parse an IJVM code from a CharStream.
     *
     * @param input CharStream containing IJVM code
     * @return The parsed program result containing the tree and any errors
     */
    protected abstract T parseStream(CharStream input);
}
