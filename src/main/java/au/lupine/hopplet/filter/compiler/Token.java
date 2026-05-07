package au.lupine.hopplet.filter.compiler;

import org.jspecify.annotations.NonNull;

public final class Token {

    private final @NonNull TokenType type;
    private final @NonNull String text;
    private final int position;

    public Token(@NonNull TokenType type, @NonNull String text, int position) {
        this.type = type;
        this.text = text;
        this.position = position;
    }

    public @NonNull TokenType type() {
        return type;
    }

    public @NonNull String text() {
        return text;
    }

    public int position() {
        return position;
    }
}
