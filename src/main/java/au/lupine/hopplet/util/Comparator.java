package au.lupine.hopplet.util;

import org.jspecify.annotations.NonNull;

public enum Comparator {

    LESS_THAN_OR_EQUAL_TO("<="),
    GREATER_THAN_OR_EQUAL_TO(">="),
    EQUAL_TO("="),
    LESS_THAN("<"),
    GREATER_THAN(">");

    private final @NonNull String symbol;

    Comparator(@NonNull String symbol) {
        this.symbol = symbol;
    }

    public @NonNull String symbol() {
        return symbol;
    }

    public boolean compare(int left, int right) {
        return switch (this) {
            case EQUAL_TO -> left == right;
            case LESS_THAN_OR_EQUAL_TO -> left <= right;
            case GREATER_THAN_OR_EQUAL_TO -> left >= right;
            case LESS_THAN -> left < right;
            case GREATER_THAN -> left > right;
        };
    }
}
