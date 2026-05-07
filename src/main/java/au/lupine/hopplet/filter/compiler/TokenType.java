package au.lupine.hopplet.filter.compiler;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public enum TokenType {

    IDENT(null),
    STRING(null),
    LPAREN('('),
    RPAREN(')'),
    AND('&'),
    OR('|'),
    NOT('!'),
    COMMA(','),
    EOF(null);

    private static final Set<Character> DELIMITERS = Arrays.stream(values())
        .map(type -> type.character)
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableSet());

    private final @Nullable Character character;

    TokenType(@Nullable Character character) {
        this.character = character;
    }

    public static boolean delimiter(char character) {
        return Character.isWhitespace(character) || DELIMITERS.contains(character);
    }

    public @NonNull String display() {
        return switch (this) {
            case IDENT -> "identifier";
            case STRING -> "string";
            case LPAREN -> "(";
            case RPAREN -> ")";
            case AND -> "&";
            case OR -> "|";
            case NOT -> "!";
            case COMMA -> ",";
            case EOF -> "end of filter";
        };
    }
}
