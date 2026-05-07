package au.lupine.hopplet.filter.compiler;

import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class Parser {

    private final @NonNull List<Token> tokens;
    private int position = 0;

    public Parser(@NonNull List<Token> tokens) {
        this.tokens = tokens;
    }

    private @NonNull Token peek() {
        return tokens.get(position);
    }

    private @NonNull Token advance() {
        return tokens.get(position++);
    }

    private @NonNull Token expect(@NonNull TokenType type) throws FilterCompileException {
        Token token = peek();

        if (token.type() != type) throw new FilterCompileException(
            Component.translatable(
                "hopplet.filter.compilation.exception.expected_token",
                Argument.string("expected", type.display()),
                Argument.string("got", token.type().display()),
                Argument.numeric("position", token.position() + 1)
            )
        );

        return advance();
    }

    public @NonNull Node parse() throws FilterCompileException {
        Node node = or();
        expect(TokenType.EOF);
        return node;
    }

    private @NonNull Node call() throws FilterCompileException {
        Token name = expect(TokenType.IDENT);
        expect(TokenType.LPAREN);

        List<String> arguments = new ArrayList<>();
        if (peek().type() != TokenType.RPAREN) {
            arguments.add(argument());

            while (peek().type() == TokenType.COMMA) {
                advance();
                arguments.add(argument());
            }
        }

        expect(TokenType.RPAREN);

        String identifier = name.text();
        Function<?> function = Function.of(identifier);
        if (function == null) throw new FilterCompileException(
            Component.translatable(
                "hopplet.filter.compilation.exception.unknown_function",
                Argument.string("name", identifier),
                Argument.numeric("position", name.position() + 1)
            )
        );

        if (!function.enabled()) throw new FilterCompileException(
            Component.translatable(
                "hopplet.filter.compilation.exception.function_disabled",
                Argument.string("name", identifier),
                Argument.numeric("position", name.position() + 1)
            )
        );

        return Node.Call.of(function, arguments);
    }

    private @NonNull String argument() throws FilterCompileException {
        Token token = peek();

        if (token.type() == TokenType.IDENT || token.type() == TokenType.STRING) {
            advance();
            return token.text();
        }

        throw new FilterCompileException(
            Component.translatable(
                "hopplet.filter.compilation.exception.expected_argument",
                Argument.string("got", token.type().display()),
                Argument.numeric("position", token.position() + 1)
            )
        );
    }

    private @NonNull Node and() throws FilterCompileException {
        Node left = not();

        while(peek().type() == TokenType.AND) {
            advance();
            Node right = not();
            left = new Node.And(left, right);
        }

        return left;
    }

    private @NonNull Node or() throws FilterCompileException {
        Node left = and();

        while(peek().type() == TokenType.OR) {
            advance();
            Node right = and();
            left = new Node.Or(left, right);
        }

        return left;
    }

    private @NonNull Node not() throws FilterCompileException {
        if (peek().type() == TokenType.NOT) {
            advance();
            return new Node.Not(not());
        }

        return primary();
    }

    private @NonNull Node primary() throws FilterCompileException {
        Token token = peek();

        if (token.type() == TokenType.LPAREN) {
            advance();

            Node inner = or();
            expect(TokenType.RPAREN);

            return inner;
        }

        if (token.type() == TokenType.IDENT) return call();

        throw new FilterCompileException(
            Component.translatable(
                "hopplet.filter.compilation.exception.unexpected_token",
                Argument.string("token", token.type().display()),
                Argument.numeric("position", token.position() + 1)
            )
        );
    }
}
