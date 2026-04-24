package au.lupine.hopplet.util;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalAssignedToNull"})
public abstract class Either<L, R> {

  public abstract <T> T map(final Function<? super L, ? extends T> l, final Function<? super R, ? extends T> r);

  public abstract Optional<L> left();

  public abstract Optional<R> right();

  public static <L, R> Either<L, R> left(final L value) {
    return new Left<>(value);
  }

  public static <L, R> Either<L, R> right(final R value) {
    return new Right<>(value);
  }

  private static final class Left<L,R> extends Either<L, R> {
    private final L value;
    private @Nullable Optional<L> optional;

    private Left(final L value) {
      this.value = Objects.requireNonNull(value);
    }

    @Override
    public <T> T map(Function<? super L, ? extends T> l, Function<? super R, ? extends T> r) {
      return l.apply(this.value);
    }

    @Override
    public Optional<L> left() {
      return this.optional != null ? this.optional : (this.optional = Optional.of(this.value));
    }

    @Override
    public Optional<R> right() {
      return Optional.empty();
    }
  }

  private static final class Right<L, R> extends Either<L, R> {
    private final R value;
    private @Nullable Optional<R> optional;

    private Right(R value) {
      this.value = Objects.requireNonNull(value);
    }

    @Override
    public <T> T map(Function<? super L, ? extends T> l, Function<? super R, ? extends T> r) {
      return r.apply(this.value);
    }

    @Override
    public Optional<L> left() {
      return Optional.empty();
    }

    @Override
    public Optional<R> right() {
      return this.optional != null ? this.optional : (this.optional = Optional.of(this.value));
    }
  }
}
