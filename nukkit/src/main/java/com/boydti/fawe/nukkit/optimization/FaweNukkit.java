package com.boydti.fawe.nukkit.optimization;

import cn.nukkit.Nukkit;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.response.FormResponseData;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindow;
import com.boydti.fawe.Fawe;
import com.boydti.fawe.IFawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.nukkit.core.NukkitTaskManager;
import com.boydti.fawe.nukkit.core.NukkitWorldEdit;
import com.boydti.fawe.nukkit.core.gui.NukkitFormBuilder;
import com.boydti.fawe.nukkit.core.gui.ResponseFormWindow;
import com.boydti.fawe.nukkit.listener.BrushListener;
import com.boydti.fawe.nukkit.optimization.queue.NukkitQueue;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.brush.visualization.VisualChunk;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.regions.general.plot.PlotSquaredFeature;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.gui.FormBuilder;
import com.google.common.base.Charsets;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class FaweNukkit implements IFawe, Listener {

    private final NukkitWorldEdit plugin;

    public FaweNukkit(NukkitWorldEdit mod) {
        this.plugin = mod;
        FaweChunk.HEIGHT = 256;
        VisualChunk.VISUALIZE_BLOCK = 20 << 4;



        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        try {
            new BrushListener(mod);
        } catch (Throwable e) {
            debug("====== BRUSH LISTENER FAILED ======");
            e.printStackTrace();
            debug("===================================");
        }
    }

    @Override
    public FormBuilder getFormBuilder() {
        return new NukkitFormBuilder();
    }

    @Override
    public int getPlayerCount() {
        return plugin.getServer().getOnlinePlayers().size();
    }

    @Override
    public boolean isOnlineMode() {
        return false;
    }

    @Override
    public String getPlatformVersion() {
        return Nukkit.VERSION;
    }

    @EventHandler
    public void onFormSubmit(PlayerFormRespondedEvent event) {
        FormWindow window = event.getWindow();
        if (window instanceof ResponseFormWindow) {
            ResponseFormWindow responseWindow = (ResponseFormWindow) window;
            FormResponse response = event.getResponse();
            if (response instanceof FormResponseSimple) {
                FormResponseSimple simple = (FormResponseSimple) response;
                ElementButton button = simple.getClickedButton();
                int index = simple.getClickedButtonId();

                //System.out.println("Simple: " + index);

                responseWindow.respond(Collections.singletonMap(index, "true"));

            } else if (response instanceof FormResponseCustom) {
                FormResponseCustom custom = (FormResponseCustom) response;
                HashMap<Integer, Object> responses = custom.getResponses();

                HashMap<Integer, Object> parsedResponses = new HashMap<>();
                for (Map.Entry<Integer, Object> responseEntry : responses.entrySet()) {
                    int index = responseEntry.getKey();
                    Object value = responseEntry.getValue();
                    if (value instanceof FormResponseData) {
                        value = ((FormResponseData) value).getElementContent();
                    } else if (value instanceof Float) {
                        value = (double) (float) value;
                    }
                    parsedResponses.put(index, value);
                }

                responseWindow.respond(parsedResponses);

                //System.out.println("Custom: " + parsedResponses);
            }
        }
        // TODO
    }

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
        FawePlayer fp = Fawe.get().getCachedPlayer(player.getName());
        if (fp != null) {
            fp.unregister();
        }
        Fawe.get().unregister(event.getPlayer().getName());
	}

    @Override
    public void debug(String s) {
        if (plugin.getWELogger() == null) {
            System.out.println(s);
        } else {
            plugin.getWELogger().log(Level.INFO, s);
        }
    }

    @Override
    public File getDirectory() {
        return plugin.getDataFolder();
    }

    @Override
    public void setupCommand(String label, final FaweCommand cmd) {
        plugin.getServer().getCommandMap().register(label, new NukkitCommand(label, cmd));


    }

    @Override
    public FawePlayer wrap(Object obj) {
        if (obj.getClass() == String.class) {
            String name = (String) obj;
            FawePlayer existing = Fawe.get().getCachedPlayer(name);
            if (existing != null) {
                return existing;
            }
            return new FaweNukkitPlayer(getPlugin().getServer().getPlayer(name));
        } else if (obj instanceof Player) {
            Player player = (Player) obj;
            FawePlayer existing = Fawe.get().getCachedPlayer(player.getName());
            return existing != null ? existing : new FaweNukkitPlayer(player);
        } else {
            return null;
        }
    }

    public NukkitWorldEdit getPlugin() {
        return plugin;
    }

    @Override
    public void setupVault() {

    }

    @Override
    public TaskManager getTaskManager() {
        return new NukkitTaskManager(plugin);
    }

    @Override
    public FaweQueue getNewQueue(World world, boolean fast) {
        Settings.IMP.HISTORY.COMBINE_STAGES = false;
        return new NukkitQueue(this, world);
    }

    @Override
    public FaweQueue getNewQueue(String world, boolean fast) {
        Settings.IMP.HISTORY.COMBINE_STAGES = false;
        return new NukkitQueue(this, world);
    }

    @Override
    public String getWorldName(World world) {
        return world.getName();
    }

    @Override
    public Collection<FaweMaskManager> getMaskManagers() {
        return new ArrayList<>();
    }

    @Override
    public void startMetrics() {
        //Metrics metrics = new Metrics(plugin);
        //metrics.start();
    }

    @Override
    public String getPlatform() {
        return "nukkit";
    }

    @Override
    public UUID getUUID(String name) {
        try {
            return UUID.fromString(name);
        } catch (Exception e) {
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name.toLowerCase()).getBytes(Charsets.UTF_8));
        }
    }

    private Map<UUID, String> names = new HashMap<>();

    @Override
    public String getName(UUID uuid) {
        try {
            Class.forName("com.boydti.fawe.regions.general.plot.PlotSquaredFeature");
            String name = PlotSquaredFeature.getName(uuid);
            if (name != null) return name;
        } catch (Throwable ignore){
            String mapped = names.get(uuid);
            if (mapped != null) return mapped;

            boolean namesEmpty = names.isEmpty();
            for (Player player : Server.getInstance().getOnlinePlayers().values()) {
                String name = player.getName();
                UUID plrUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name.toLowerCase()).getBytes(Charsets.UTF_8));
                names.put(plrUUID, name);
            }

            if (namesEmpty) {
                for (File file : new File("players").listFiles()) {
                    String name = file.getName();
                    name = name.substring(0, name.length() - 4);
                    UUID plrUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name.toLowerCase()).getBytes(Charsets.UTF_8));
                    names.put(plrUUID, name);

                }
            }

            mapped = names.get(uuid);
            if (mapped != null) return mapped;
        }
        return uuid.toString();
    }

    @Override
    public Object getBlocksHubApi() {
        return null;
    }
}
