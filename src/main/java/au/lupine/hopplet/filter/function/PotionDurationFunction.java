package au.lupine.hopplet.filter.function;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import au.lupine.hopplet.util.Comparator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PotionDurationFunction implements Function<Set<PotionDurationFunction.Spec>> {

    @Override
    public @NonNull String name() {
        return "potion_duration";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("duration");
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.potion_duration.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull Set<Spec> compile(@NonNull List<String> arguments) throws FilterCompileException {
        argsRequired(arguments);

        Set<Spec> durations = new HashSet<>();
        for (String argument : arguments) {
            String normalised = argument.toLowerCase().replaceAll("\\s", "");

            if (normalised.equals("infinite") || normalised.equals("unlimited")) {
                durations.add(new Spec(Comparator.EQUAL_TO, PotionEffect.INFINITE_DURATION));
                continue;
            }

            Comparator comparator = Comparator.EQUAL_TO;
            String value = normalised;

            for (Comparator c : Comparator.values()) {
                String symbol = c.symbol();

                if (normalised.startsWith(symbol)) {
                    comparator = c;
                    value = normalised.substring(symbol.length());
                    break;
                }
            }

            int ticks = parseTicks(value, argument);
            durations.add(new Spec(comparator, ticks));
        }

        return durations;
    }

    @Override
    public boolean test(Filter.@NonNull Context context, @NonNull Set<Spec> arguments) {
        ItemStack item = context.stack();

        if (!(item.getItemMeta() instanceof PotionMeta meta)) return false;

        List<PotionEffect> effects = new ArrayList<>();

        PotionType base = meta.getBasePotionType();
        if (base != null) effects.addAll(base.getPotionEffects());

        effects.addAll(meta.getCustomEffects());
        if (effects.isEmpty()) return false;

        for (Spec argument : arguments) {
            for (PotionEffect effect : effects) {
                int duration = effect.getDuration();

                if (duration == PotionEffect.INFINITE_DURATION) {
                    if (argument.ticks == PotionEffect.INFINITE_DURATION) return true;
                    if (argument.comparator == Comparator.GREATER_THAN || argument.comparator == Comparator.GREATER_THAN_OR_EQUAL_TO) return true;

                    continue;
                }

                if (argument.ticks == PotionEffect.INFINITE_DURATION) continue;

                if (argument.comparator.compare(duration, argument.ticks)) return true;
            }
        }

        return false;
    }

    private static int parseTicks(@NonNull String text, @NonNull String argument) {
        if (text.isEmpty()) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.potion_duration.compilation.exception.no_duration_specified",
                    Argument.string("input", argument)
                )
            );
        }

        int ticks;
        try {
            ticks = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.potion_duration.compilation.exception.invalid_duration",
                    Argument.string("input", argument)
                )
            );
        }

        if (ticks < -1) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.potion_duration.compilation.exception.less_than_negative_one",
                    Argument.string("input", argument)
                )
            );
        }

        return ticks;
    }

    public record Spec(@NonNull Comparator comparator, int ticks) {}
}
