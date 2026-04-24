package au.lupine.hopplet;

import au.lupine.hopplet.base.Plugin;
import au.lupine.hopplet.command.HoppletCommand;
import au.lupine.hopplet.event.HoppletEnabledEvent;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.function.*;
import au.lupine.hopplet.listener.FilterCacheListener;
import au.lupine.hopplet.listener.FilterEditListener;
import au.lupine.hopplet.listener.HopperInventoryListener;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Hopplet extends Plugin {

    private static Hopplet instance;

    @Override
    public void load() {
        instance = this;

        commands(HoppletCommand.build());
    }

    @Override
    public void enable() {
        listeners(
            new FilterCacheListener(),
            new FilterEditListener(),
            new HopperInventoryListener()
        );

        Function.register(
            new BookAuthorFunction(), new BookGenerationFunction(), new DisplayNameContainsFunction(), new DisplayNameEndsWithFunction(),
            new DisplayNameFunction(), new DisplayNameStartsWithFunction(), new EnchantmentFunction(), new IsEdibleFunction(),
            new IsEnchantedFunction(), new IsFuelFunction(), new IsRepairableFunction(), new IsStackableFunction(),
            new ItemDurabilityFunction(), new MaterialContainsFunction(), new MaterialEndsWithFunction(), new MaterialFunction(),
            new MaterialStartsWithFunction(), new PotionDurationFunction(), new PotionEffectFunction(), new TagFunction(),
            new ThrowerFunction()
        );

        HoppletEnabledEvent event = new HoppletEnabledEvent();
        event.callEvent();
    }

    @Override
    public void disable() {
        Filter.Cache.invalidate();
        Function.FUNCTIONS.clear();
    }

    @Override
    public @NonNull Map<String, Object> nodes() {
        Map<String, Object> nodes = new LinkedHashMap<>();

        Map<String, Object> dialog = new LinkedHashMap<>();
        dialog.put("input_length", 512);

        Map<String, Object> functions = new LinkedHashMap<>();
        functions.put("disabled", List.of());

        nodes.put("dialog", dialog);
        nodes.put("functions", functions);

        return nodes;
    }

    public static @NonNull Hopplet instance() {
        return instance;
    }
}
