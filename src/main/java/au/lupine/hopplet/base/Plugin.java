package au.lupine.hopplet.base;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public abstract class Plugin extends JavaPlugin {

    private final @NonNull List<Listener> listeners = new ArrayList<>();
    private final @NonNull List<ScheduledTask> tasks = new ArrayList<>();
    private Config config;

    @Override
    public final void onLoad() {
        config = new Config(this);
        translations();
        load();
    }

    public void load() {}

    @Override
    public final void onEnable() {
        initialise();
    }

    public final void initialise() {
        config.reload();

        enable();

        PluginManager pm = this.getServer().getPluginManager();
        listeners.forEach(listener -> pm.registerEvents(listener, this));
    }

    public void enable() {}

    @Override
    public final void onDisable() {
        terminate();
    }

    public final void terminate() {
        disable();

        listeners.forEach(HandlerList::unregisterAll);
        listeners.clear();

        tasks.forEach(ScheduledTask::cancel);
        tasks.clear();
    }

    public void disable() {}

    public void reload() {
        terminate();
        initialise();
    }

    public @NonNull Config config() {
        return config;
    }

    public @NonNull Map<String, Object> nodes() {
        return Map.of();
    }

    public final void listeners(@NonNull Listener... listeners) {
        this.listeners.addAll(List.of(listeners));
    }

    public final void tasks(@NonNull ScheduledTask... tasks) {
        this.tasks.addAll(List.of(tasks));
    }

    private void translations() {
        MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key(namespace(), "translations"));

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

    public static final class Config {

        private final Plugin owner;

        private final @NonNull GsonConfigurationLoader loader;
        private @NonNull ConfigurationNode root;

        public Config(@NonNull Plugin owner) {
            this.owner = owner;

            Path file = owner.getDataFolder()
                .toPath()
                .resolve("config.json");

            try {
                Files.createDirectories(file.getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            GsonConfigurationLoader.Builder builder = GsonConfigurationLoader.builder();
            builder.path(file);
            loader = builder.build();

            try {
                root = Files.exists(file) ? loader.load() : loader.createNode();
                defaults(root, owner.nodes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            save();
        }

        @SuppressWarnings("unchecked")
        private static void defaults(@NonNull ConfigurationNode node, Map<String, Object> defaults) throws SerializationException {
            for (Map.Entry<String, Object> entry : defaults.entrySet()) {
                ConfigurationNode child = node.node(entry.getKey());
                Object value = entry.getValue();

                if (value instanceof Map<?, ?> nested) {
                    defaults(child, (Map<String, Object>) nested);
                } else if (child.virtual()) {
                    child.set(value);
                }
            }
        }

        public @NonNull Plugin owner() {
            return owner;
        }

        public @NonNull ConfigurationNode root() {
            return root;
        }

        public void save() {
            try {
                loader.save(root);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void reload() {
            try {
                root = loader.load();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
