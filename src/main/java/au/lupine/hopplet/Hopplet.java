package au.lupine.hopplet;

import au.lupine.hopplet.base.Plugin;
import au.lupine.hopplet.command.HoppletCommand;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.function.*;
import au.lupine.hopplet.listener.FilterCacheListener;
import au.lupine.hopplet.listener.FilterEditListener;
import au.lupine.hopplet.listener.HopperInventoryListener;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

public final class Hopplet extends Plugin {

    private static Hopplet instance;

    @Override
    public void load() {
        instance = this;

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(HoppletCommand.build(), List.of("hopper", "hopperfilter"));
        });
    }

    @Override
    public void enable() {
        // `enable` scope is confined to filtering functionality for now
        if (config().root().node("enable").getBoolean(true)) {
            listeners(
                new FilterCacheListener(),
                new HopperInventoryListener()
            );
        }

        Filter.style = MiniMessage.miniMessage().deserialize(
            config().root()
                .node("filter", "magic_style")
                .getString("<gold>")
        ).style();

        listeners(
            new FilterEditListener()
        );

        Function.register(
            new BookAuthorFunction(),
            new BookGenerationFunction(),
            new DisplayNameContainsFunction(),
            new DisplayNameEndsWithFunction(),
            new DisplayNameFunction(),
            new DisplayNameStartsWithFunction(),
            new EnchantmentFunction(),
            new IsEdibleFunction(),
            new IsEnchantedFunction(),
            new IsFuelFunction(),
            new IsRepairableFunction(),
            new IsStackableFunction(),
            new IsUnbreakableFunction(),
            new ItemDurabilityFunction(),
            new ItemRarityFunction(),
            new MaterialContainsFunction(),
            new MaterialEndsWithFunction(),
            new MaterialFunction(),
            new MaterialStartsWithFunction(),
            new PotionDurationFunction(),
            new PotionEffectFunction(),
            new SmeltableByFunction(),
            new SourceTypeFunction(),
            new TagFunction(),
            new ThrowerFunction()
        );
    }

    @Override
    public void disable() {
        Filter.Cache.invalidate();
    }

    @Override
    public @NonNull Map<String, Object> nodes() {
        return Map.of(
            "enable", true,
            "filter", Map.of(
                "disable_hopper_on_compilation_error", true,
                "edit", Map.of(
                    "dialog", Map.of(
                        "max_input_length", 512
                    )
                ),
                "function", Map.of(
                    "disable", List.of()
                ),
                "magic_style", "<gold>"
            )
        );
    }

    public static @NonNull Hopplet instance() {
        return instance;
    }
}
