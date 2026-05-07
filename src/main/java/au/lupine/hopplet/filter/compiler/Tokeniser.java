package au.lupine.hopplet.filter.compiler;

import au.lupine.hopplet.filter.exception.FilterCompileException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class Tokeniser {

    private Tokeniser() {}

    public static List<Token> tokenise(@NonNull String input) throws FilterCompileException {
        List<Token> tokens = new ArrayList<>();
        int i = 0;

        while (i < input.length()) {
            char character = input.charAt(i);

            if (Character.isWhitespace(character)) {
                i++;
                continue;
            }

            switch (character) {
                case '(' -> { tokens.add(new Token(TokenType.LPAREN, "(", i)); i++; }
                case ')' -> { tokens.add(new Token(TokenType.RPAREN, ")", i)); i++; }
                case '&' -> { tokens.add(new Token(TokenType.AND, "&", i)); i++; }
                case '|' -> { tokens.add(new Token(TokenType.OR, "|", i)); i++; }
                case '!' -> { tokens.add(new Token(TokenType.NOT, "!", i)); i++; }
                case ',' -> { tokens.add(new Token(TokenType.COMMA, ",", i)); i++; }
                case '"' -> {
                    int start = i;
                    i++;

                    int stringStart = i;
                    while (i < input.length() && input.charAt(i) != '"') i++;

                    if (i >= input.length()) {
                        throw new FilterCompileException(
                            Component.translatable(
                                "hopplet.filter.compilation.exception.unterminated_string",
                                Argument.numeric("position", start + 1)
                            )
                        );
                    }

                    String string = input.substring(stringStart, i);
                    tokens.add(new Token(TokenType.STRING, string, start));
                    i++;
                }
                default -> {
                    int start = i;
                    while (i < input.length() && !TokenType.delimiter(input.charAt(i))) i++;

                    String ident = input.substring(start, i);
                    tokens.add(new Token(TokenType.IDENT, ident, start));
                }
            }
        }

        tokens.add(new Token(TokenType.EOF, "", input.length()));
        return tokens;
    }
}
