package au.lupine.hopplet.filter.function.impl;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.context.Context;
import au.lupine.hopplet.filter.context.ItemEntityContext;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import au.lupine.hopplet.filter.function.Matcher;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.UUID;

public final class ThrowerNationFunction implements Matcher<String> {

    @Override
    public @NonNull String name() {
        return "thrower_nation";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("nation");
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.thrower_nation.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public boolean disabled() {
        return !Hopplet.instance().getServer()
            .getPluginManager()
            .isPluginEnabled("Towny");
    }

    @Override
    public @NonNull String parse(@NonNull String argument) throws FilterCompileException {
        return argument;
    }

    @Override
    public boolean matches(@NonNull Context context, @NonNull String argument) {
        if (!(context instanceof ItemEntityContext ctx)) return false;

        UUID thrower = ctx.item().getThrower();
        if (thrower == null) return false;

        TownyAPI towny = TownyAPI.getInstance();

        Resident resident = towny.getResident(thrower);
        if (resident == null) return false;

        Nation nation;
        try {
            nation = towny.getNation(UUID.fromString(argument));
        } catch (IllegalArgumentException e) {
            nation = towny.getNation(argument);
        }

        if (nation == null) return false;

        return nation.hasResident(resident);
    }
}
