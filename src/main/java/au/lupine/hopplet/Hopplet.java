package au.lupine.hopplet;

import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.function.*;
import au.lupine.hopplet.listener.FilterCacheListener;
import au.lupine.hopplet.listener.FilterEditListener;
import au.lupine.hopplet.listener.HopperInventoryListener;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Hopplet extends JavaPlugin {

    private static Hopplet instance;

    @Override
    public void onEnable() {
        instance = this;

        loadTranslations();

        registerListeners(
            new FilterCacheListener(),
            new FilterEditListener(),
            new HopperInventoryListener()
        );

        Function.register(
            new BookAuthorFunction(),
            new DisplayNameContainsFunction(),
            new DisplayNameEndsWithFunction(),
            new DisplayNameFunction(),
            new DisplayNameStartsWithFunction(),
            new EnchantmentFunction(),
            new MaterialContainsFunction(),
            new MaterialEndsWithFunction(),
            new MaterialFunction(),
            new MaterialStartsWithFunction(),
            new PotionDurationFunction(),
            new PotionEffectFunction(),
            new TagFunction(),
            new ThrowerFunction()
        );
    }

    private void registerListeners(@NonNull Listener... listeners) {
        PluginManager pm = getServer().getPluginManager();
        for (Listener listener : listeners) {
            pm.registerEvents(listener, this);
        }
    }

    public static @NonNull Hopplet instance() {
        return instance;
    }

    private void loadTranslations() {
        MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("hopplet", "translations"));

        for (Locale locale : Locale.getAvailableLocales()) {
            try {
                ResourceBundle bundle = ResourceBundle
                        .getBundle(
                                "lang.Bundle",
                                locale,
                                getClassLoader(),
                                UTF8ResourceBundleControl.utf8ResourceBundleControl()
                        );

                store.registerAll(locale, bundle, false);
            } catch (MissingResourceException ignored) {}
        }

        GlobalTranslator.translator().addSource(store);
    }
}
