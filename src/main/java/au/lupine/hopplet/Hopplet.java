package au.lupine.hopplet;

import au.lupine.hopplet.base.Plugin;
import au.lupine.hopplet.command.HoppletCommand;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.cache.Cache;
import au.lupine.hopplet.filter.function.Function;
import au.lupine.hopplet.filter.function.impl.*;
import au.lupine.hopplet.listener.FilterCacheListener;
import au.lupine.hopplet.listener.FilterEditListener;
import au.lupine.hopplet.listener.HopperInventoryListener;
import au.lupine.hopplet.listener.ItemCraftListener;
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
        Filter.style = MiniMessage.miniMessage().deserialize(
            config().root()
                .node("filter", "magic_style")
                .getString("<gold>")
        ).style();

        // `enable` scope is confined to filtering functionality for now.
        if (config().root().node("enable").getBoolean(true)) {
            listeners(
                new FilterCacheListener(),
                new HopperInventoryListener()
            );
        }

        listeners(
            new FilterEditListener(),
            new ItemCraftListener()
        );

        Function.register(
            new AmountFunction(),
            new ArmorTrimMaterialFunction(),
            new ArmorTrimPatternFunction(),
            new BookAuthorFunction(),
            new BookGenerationFunction(),
            new DisplayNameContainsFunction(),
            new DisplayNameEndsWithFunction(),
            new DisplayNameFunction(),
            new DisplayNameStartsWithFunction(),
            new DurabilityFunction(),
            new EnchantmentFunction(),
            new HasArmorTrimFunction(),
            new HasLoreFunction(),
            new IsDamagedFunction(),
            new IsEdibleFunction(),
            new IsEnchantedFunction(),
            new IsFuelFunction(),
            new IsRepairableFunction(),
            new IsSmeltableFunction(),
            new IsStackableFunction(),
            new IsUnbreakableFunction(),
            new LoreContainsFunction(),
            new LoreEndsWithFunction(),
            new LoreFunction(),
            new LoreStartsWithFunction(),
            new MapIDFunction(),
            new MaterialContainsFunction(),
            new MaterialEndsWithFunction(),
            new MaterialFunction(),
            new MaterialStartsWithFunction(),
            new PotionDurationFunction(),
            new PotionEffectFunction(),
            new RarityFunction(),
            new SkullOwnerFunction(),
            new SkullTextureFunction(),
            new SmeltableByFunction(),
            new SourceInventoryTypeFunction(),
            new StoredExperienceFunction(),
            new TagFunction(),
            new ThrowerFunction(),
            new ThrowerNationFunction(),
            new ThrowerTownFunction()
        );
    }

    @Override
    public void disable() {
        Cache.invalidate();
    }

    @Override
    public @NonNull Map<String, Object> nodes() {
        return Map.of(
            "enable", true,
            "filter", Map.of(
                "disable_hopper_on_compilation_error", true,
                "edit", Map.of(
                    "dialog", Map.of(
                        "title_text", "<gold><bold>Hopplet",
                        "show_item", true,
                        "show_credits", false,
                        "show_documentation", true,
                        "documentation_url", "https://github.com/jwkerr/Hopplet/wiki",
                        "show_discord", true,
                        "discord_url", "https://discord.lupine.au",
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
