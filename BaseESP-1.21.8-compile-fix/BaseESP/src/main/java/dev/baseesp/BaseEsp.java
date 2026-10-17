package dev.baseesp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class BaseEsp implements ClientModInitializer {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("baseesp.json");
    static Config config = new Config();
    static class Config {
        boolean enabled = true, finder = true;
        int radius = 48, threshold = 2;
        float opacity = .65f;
        List<String> blocks = new ArrayList<>(List.of("minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel", "minecraft:shulker_box", "minecraft:hopper", "minecraft:furnace"));
    }
    private static KeyBinding toggle, settings;
    private static final Map<BlockPos, String> detected = new HashMap<>();
    private static final Set<Long> alerted = new HashSet<>();
    private static int scanIndex = 0, ticks = 0;
    private static Object lastWorld;
    static void save() { try { Files.createDirectories(CONFIG.getParent()); Files.writeString(CONFIG, GSON.toJson(config)); } catch (Exception ignored) {} }
    private static void load() { try { if (Files.exists(CONFIG)) { Config c = GSON.fromJson(Files.readString(CONFIG), Config.class); if (c != null && c.blocks != null) config = c; } } catch (Exception ignored) {} }
    static boolean addBlock(String raw) {
        Identifier id = Identifier.tryParse(raw.trim().toLowerCase(Locale.ROOT));
        if (id == null || !Registries.BLOCK.containsId(id)) return false;
        String value = id.toString();
        if (!config.blocks.contains(value)) config.blocks.add(value);
        save(); return true;
    }
    static void removeBlock(String id) { config.blocks.remove(id); save(); }
    static void radius(int d) { config.radius = Math.max(16, Math.min(96, config.radius + d)); save(); }
    static void opacity(float d) { config.opacity = Math.max(.15f, Math.min(1f, config.opacity + d)); save(); }
    static void threshold(int d) { config.threshold = Math.max(1, Math.min(12, config.threshold + d)); save(); }
    @Override public void onInitializeClient() {
        load();
        toggle = KeyBindingHelper.registerKeyBinding(new KeyBinding("Toggle BaseESP", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "BaseESP"));
        settings = KeyBindingHelper.registerKeyBinding(new KeyBinding("BaseESP Settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "BaseESP"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggle.wasPressed()) { config.enabled = !config.enabled; save(); if (client.player != null) client.player.sendMessage(Text.literal("BaseESP: " + (config.enabled ? "ON" : "OFF")), true); }
            while (settings.wasPressed()) client.setScreen(new EspSettingsScreen());
            if (client.world == null || client.player == null) { detected.clear(); alerted.clear(); lastWorld = null; return; }
            if (lastWorld != client.world) { detected.clear(); alerted.clear(); scanIndex = 0; lastWorld = client.world; }
            if (!config.enabled || ++ticks % 3 != 0) return;
            // Incremental chunk scanning avoids traversing the entire render distance each frame.
            int cr = (config.radius + 15) / 16, side = cr * 2 + 1;
            int index = scanIndex++ % (side * side);
            BlockPos center = client.player.getBlockPos();
            int cx = (center.getX() >> 4) + index % side - cr;
            int cz = (center.getZ() >> 4) + index / side - cr;
            if (!client.world.isChunkLoaded(cx, cz)) return;
            Set<String> selected = new HashSet<>(config.blocks);
            int minY = Math.max(client.world.getBottomY(), center.getY() - config.radius);
            int maxY = Math.min(client.world.getTopYInclusive(), center.getY() + config.radius);
            int amethyst = 0, other = 0;
            for (int x = cx << 4; x < (cx << 4) + 16; x++) for (int z = cz << 4; z < (cz << 4) + 16; z++) for (int y = minY; y <= maxY; y++) {
                BlockPos pos = new BlockPos(x,y,z);
                if (pos.getSquaredDistance(center) > config.radius * config.radius) continue;
                BlockState state = client.world.getBlockState(pos);
                Block block = state.getBlock();
                String id = Registries.BLOCK.getId(block).toString();
                boolean cluster = block == Blocks.SMALL_AMETHYST_BUD || block == Blocks.MEDIUM_AMETHYST_BUD || block == Blocks.LARGE_AMETHYST_BUD || block == Blocks.AMETHYST_CLUSTER;
                if (cluster) amethyst++;
                if (selected.contains(id)) { detected.put(pos, id); if (!cluster) other++; }
                else detected.remove(pos);
            }
            if (config.finder && amethyst > 0 && other >= config.threshold) {
                long key = net.minecraft.util.math.ChunkPos.toLong(cx, cz);
                if (alerted.add(key)) client.player.sendMessage(Text.literal("§d[BaseESP] Potential base near chunk " + cx + ", " + cz + " (amethyst + " + other + " selected blocks)"), false);
            }
            detected.keySet().removeIf(p -> p.getSquaredDistance(center) > config.radius * config.radius);
        });
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!config.enabled || client.world == null || client.player == null || context.matrixStack() == null || context.consumers() == null) return;
            MatrixStack matrices = context.matrixStack();
            Vec3d camera = context.camera().getPos();
            VertexConsumer lines = context.consumers().getBuffer(RenderLayer.getLines());
            for (Map.Entry<BlockPos,String> entry : detected.entrySet()) {
                BlockPos p = entry.getKey();
                if (!client.world.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) continue;
                float[] c = color(entry.getValue());
                VertexRendering.drawBox(matrices, lines, new Box(p).offset(-camera.x,-camera.y,-camera.z).expand(.002), c[0],c[1],c[2],config.opacity);
            }
        });
    }
    private static float[] color(String id) {
        if (id.contains("shulker")) return new float[]{.95f,.4f,1f};
        if (id.contains("hopper")) return new float[]{.35f,.8f,1f};
        if (id.contains("amethyst")) return new float[]{.8f,.4f,1f};
        return new float[]{.6f,.35f,1f};
    }
}
