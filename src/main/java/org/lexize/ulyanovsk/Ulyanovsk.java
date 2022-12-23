package org.lexize.ulyanovsk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.commands.CommandDispatcher;
import net.minecraft.commands.CommandListenerWrapper;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.command.VanillaCommandWrapper;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.lexize.lomponent.LomponentSerializer;
import org.lexize.lomponent.LomponentStyleContainer;
import org.lexize.lomponent.components.Component;
import org.lexize.lomponent.components.DecorationComponent;
import org.lexize.lomponent.components.GroupComponent;
import org.lexize.lomponent.models.Color;
import org.lexize.lomponent.tags.PlaceholderTag;
import org.lexize.ulyanovsk.commands.JailCommand;
import org.lexize.ulyanovsk.commands.JailHistoryCommand;
import org.lexize.ulyanovsk.commands.ReleaseCommand;
import org.lexize.ulyanovsk.event_handlers.UlyanovskMainHandler;
import org.lexize.ulyanovsk.exceptions.TimestampKeyNotFound;
import org.lexize.ulyanovsk.exceptions.TimestampNotMatches;
import org.lexize.ulyanovsk.models.JailData;
import org.lexize.ulyanovsk.models.JailedPlayerSavedData;
import org.lexize.ulyanovsk.tags.FunctionalPlaceholderTag;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Ulyanovsk extends JavaPlugin {

    private static Ulyanovsk _instance;
    private final CommandDispatcher _commandDispatcher = new CommandDispatcher();
    private UlyanovskConfig _config = new UlyanovskConfig();
    private UlyanovskDB _database;
    private Gson _json = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    private World _jailWorld;
    public UlyanovskDB getDatabase() {
        return _database;
    }
    public static Ulyanovsk getInstance() {
        return _instance;
    }
    public UlyanovskConfig getConfiguration() {
        return _config;
    }
    public World getJailWorld() {
        return _jailWorld;
    }
    public Gson getJson() {
        return _json;
    }
    private UlyanovskReleaseRunnable _releaseRunnable;
    private UlyanovskTranslation _translation;
    public UlyanovskTranslation getTranslation() {
        return _translation;
    }
    private LomponentSerializer _lomponentSerializer;
    public LomponentSerializer getLomponentSerializer() {
        return _lomponentSerializer;
    }
    @Override
    public void onEnable() {
        _instance = this;
        // Plugin startup logic
        CraftServer server = (CraftServer)(getServer());
        getDataFolder().mkdirs();
        File configFile = getDataFolder().toPath().resolve("config.json").toFile();
        if (configFile.exists()) {
            _config = _json.fromJson(new String(Utils.readFile(configFile)), UlyanovskConfig.class);
        }
        Utils.writeFile(configFile, _json.toJson(_config).getBytes(StandardCharsets.UTF_8));
        File translationFile = getDataFolder().toPath().resolve("translation.yml").toFile();
        try {
            _translation = new UlyanovskTranslation(translationFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        File databaseFile = getDataFolder().toPath().resolve("ulyanovsk.db").toFile();
        _database = new UlyanovskDB(databaseFile);
        _RegisterCommand(JailCommand.getCommand());
        _RegisterCommand(ReleaseCommand.getCommand());
        _RegisterCommand(JailHistoryCommand.getCommand());
        server.getCommandMap().registerAll(_config.CommandNamespace, _GetCommands());
        _jailWorld = getServer().createWorld(
                WorldCreator.name(_config.WorldName).generator(new VoidWorldGenerator())
        );
        _lomponentSerializer = LomponentSerializer.defaultBuilder().build();
        _releaseRunnable = new UlyanovskReleaseRunnable();
        _releaseRunnable.runTaskTimer(this, 0, 20*60);
        Bukkit.getPluginManager().registerEvents(new UlyanovskMainHandler(), this);
    }

    public void JailPlayer(CommandSender invoker, Player playerToJail, long time, String reason) {
        JailedPlayerSavedData savedData = new JailedPlayerSavedData();
        String[] base64Items;
        List<String> base64Effects = new ArrayList<>();
        PlayerInventory inventory = playerToJail.getInventory();
        ItemStack[] contents = inventory.getContents();
        base64Items = new String[contents.length];
        var base64Encoder = Base64.getEncoder();
        for (int i = 0; i < contents.length; i++) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos);
                boos.writeObject(contents[i]);
                boos.close();
                byte[] data = baos.toByteArray();
                baos.close();
                base64Items[i] = base64Encoder.encodeToString(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (PotionEffect effect :
                playerToJail.getActivePotionEffects()) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos);
                boos.writeObject(effect);
                boos.close();
                byte[] data = baos.toByteArray();
                baos.close();
                base64Effects.add(base64Encoder.encodeToString(data));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        savedData.encodedInventoryData = base64Items;
        savedData.encodedPotionEffects = base64Effects;
        Location playerLocation = playerToJail.getLocation();
        savedData.posX = playerLocation.getX();
        savedData.posY = playerLocation.getY();
        savedData.posZ = playerLocation.getZ();
        savedData.rotPitch = playerLocation.getPitch();
        savedData.rotYaw = playerLocation.getYaw();
        savedData.world_uuid = playerLocation.getWorld().getUID().toString();
        savedData.health = playerToJail.getHealth();
        String invokerUUID = "null";
        if (invoker instanceof ConsoleCommandSender) invokerUUID = "CONSOLE";
        else if (invoker instanceof Player player) {
            invokerUUID = player.getUniqueId().toString();
        }
        long startTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        try {
            JailData data = new JailData(invokerUUID, startTime,playerToJail.getUniqueId().toString(),
                    startTime, time, reason, savedData);
            _database.AddPlayerToJail(data);
            inventory.clear();
            for (PotionEffect e :
                    playerToJail.getActivePotionEffects()) {
                playerToJail.removePotionEffect(e.getType());
            }
            playerToJail.setHealth(playerToJail.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            var pos = _config.TeleportPosition;
            playerToJail.teleport(new Location(getJailWorld(), pos.x,pos.y,pos.z,pos.yaw,pos.pitch));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
    public void RestorePlayerData(Player pl, JailedPlayerSavedData savedData) throws IOException, ClassNotFoundException {
        var worldToTP = Bukkit.getWorld(UUID.fromString(savedData.world_uuid));
        Location teleportLocation = new Location(worldToTP, savedData.posX, savedData.posY,savedData.posZ, savedData.rotYaw, savedData.rotPitch);
        ItemStack[] inventory = new ItemStack[savedData.encodedInventoryData.length];
        List<PotionEffect> effectsToRestore = new ArrayList<>();
        var baseDecoder = Base64.getDecoder();
        for (int i = 0; i < savedData.encodedInventoryData.length; i++) {
            byte[] itemData = baseDecoder.decode(savedData.encodedInventoryData[i]);
            ByteArrayInputStream bais = new ByteArrayInputStream(itemData);
            BukkitObjectInputStream bois = new BukkitObjectInputStream(bais);
            inventory[i] = (ItemStack) bois.readObject();
            bois.close();
            bais.close();
        }
        for (String encodedEffect :
                savedData.encodedPotionEffects) {
            byte[] potionData = baseDecoder.decode(encodedEffect);
            ByteArrayInputStream bais = new ByteArrayInputStream(potionData);
            BukkitObjectInputStream bois = new BukkitObjectInputStream(bais);
            effectsToRestore.add((PotionEffect) bois.readObject());
            bois.close();
            bais.close();
        }
        pl.teleport(teleportLocation);
        pl.getInventory().setContents(inventory);
        pl.addPotionEffects(effectsToRestore);
        pl.setHealth(savedData.health);
    }
    @Override
    public void onDisable() {
        _releaseRunnable.cancel();
        try {
            _database.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void SendMessageToEveryoneWithPermission(String permission, BaseComponent... components) {
        for (Player pl :
                Bukkit.getOnlinePlayers()) {
            if (pl.hasPermission(permission)) {
                pl.spigot().sendMessage(components);
            }
        }
        Bukkit.getConsoleSender().spigot().sendMessage(components);
    }

    public OfflinePlayer getPlayer(String name) {
        Player pl = Bukkit.getPlayer(name);
        if (pl != null) return pl;
        for (OfflinePlayer player :
                Bukkit.getOfflinePlayers()) {
            if (player.getName().equals(name)) return player;
        }
        return null;
    }

    private void _RegisterCommand(LiteralArgumentBuilder<CommandListenerWrapper> cmd) {
        _commandDispatcher.a().register(cmd);
        getLogger().info("Command %s successfully registered".formatted(cmd.getLiteral()));
    }
    private List<Command> _GetCommands() {
        ArrayList<Command> commands = new ArrayList<>();
        for (var c :
                _commandDispatcher.a().getRoot().getChildren()) {
            commands.add(new VanillaCommandWrapper(_commandDispatcher, c));
        }
        return commands;
    }

    public void ReleasePlayer(JailData data, String reason) {
        try {

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class Utils {
        public static boolean argumentExistsInCtx(CommandContext<CommandListenerWrapper> context, String name) {
            for (var node :
                    context.getNodes()) {
                if (node.getNode().getName().equals(name)) return true;
            }
            return false;
        }

        public static byte[] readFile(File fileToRead) {
            try {
                FileInputStream fio = new FileInputStream(fileToRead);
                byte[] data = fio.readAllBytes();
                fio.close();
                return data;
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static void writeFile(File fileToWrite, byte[] data) {
            try {
                FileOutputStream fos = new FileOutputStream(fileToWrite);
                fos.write(data);
                fos.close();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static long GetTimeByTimestampData(Map<String, Long> data) {
            return data.getOrDefault("sec",0L) +
                    (data.getOrDefault("min",0L)*60)+
                    (data.getOrDefault("hours",0L)*3600)+
                    (data.getOrDefault("days",0L)*3600*24)+
                    (data.getOrDefault("months",0L)*3600*24*30);
        }

        public static final String[] AvailableTimestampKeys = new String[] {
                "sec","min","hours","days","months"
        };

        public static Map<String, Long> GetTimestampData(String timestampString) throws TimestampNotMatches, TimestampKeyNotFound {
            UlyanovskConfig config = Ulyanovsk.getInstance()._config;
            if (!Pattern.compile(JailCommand.TimestampCorrectCheckPattern).matcher(timestampString).matches())
                throw new TimestampNotMatches(timestampString);
            Matcher matcher = Pattern.compile(JailCommand.TimestampComponentPattern).matcher(timestampString);
            HashMap<String, Long> data = new HashMap<>();
            while (matcher.find()) {
                Long num = Long.valueOf(matcher.group(1));
                String key = matcher.group(2);
                if (Arrays.stream(AvailableTimestampKeys).noneMatch(key::equals)) {
                    boolean nf = true;
                    for (Map.Entry<String, String[]> kv:
                         config.TimestampComponentAliases.entrySet()) {
                        String ac = kv.getKey();
                        if (Arrays.stream(AvailableTimestampKeys).noneMatch(ac::equals)) continue;
                        if (Arrays.asList(kv.getValue()).contains(key)) {
                            data.put(ac, num);
                            nf = false;
                            break;
                        }
                    }
                    if (nf) throw new TimestampKeyNotFound(key);
                }
                else {
                    data.put(key, num);
                }
            }
            return data;
        }

        public static List<Field> GetAllFields(Class<?> c){
            List<Field> fields = new ArrayList<>();
            for (Field f :
                    c.getDeclaredFields()) {
                fields.add(f);
            }
            Class<?> superClass = c.getSuperclass();
            if (superClass != null) {
                fields.addAll(GetAllFields(superClass));
            }
            return fields;
        }

        //public static BaseComponent ParseLomponent(String sourceText, Map<String, String> placeholders) {
        //    var serializerBuilder = Ulyanovsk.getInstance().getLomponentSerializer().getBuilder();
        //    if(placeholders != null) {
        //        for (Map.Entry<String, String> kv :
        //                placeholders.entrySet()) {
        //            serializerBuilder.addTags(new PlaceholderTag(kv.getKey(),kv.getValue()));
        //        }
        //    }
        //    var serializer = serializerBuilder.build();
        //    GroupComponent c = serializer.parse(sourceText);
        //    BaseComponent baseComponent = FromLomponentToBaseComponent(c);
        //    return baseComponent;
        //}

        public static BaseComponent ParseLomponent(String sourceText, Map<String, Supplier<String>> placeholders) {
            var serializerBuilder = Ulyanovsk.getInstance().getLomponentSerializer().getBuilder();
            if(placeholders != null) {
                for (Map.Entry<String, Supplier<String>> kv :
                        placeholders.entrySet()) {
                    serializerBuilder.addTags(new FunctionalPlaceholderTag(kv.getKey(),kv.getValue()));
                }
            }
            var serializer = serializerBuilder.build();
            GroupComponent c = serializer.parse(sourceText);
            BaseComponent baseComponent = FromLomponentToBaseComponent(c);
            return baseComponent;
        }

        public static BaseComponent FromLomponentToBaseComponent(GroupComponent component) {
            List<TextComponent> textComponents = new ArrayList<>();
            _IterateOverComponent(component, textComponents);
            return new TextComponent(textComponents.toArray(textComponents.toArray(new TextComponent[0])));
        }

        private static void _IterateOverComponent(GroupComponent component, List<TextComponent> textComponentsList) {
            for (Component cm :
                    component.getChildren()) {
                if (cm instanceof org.lexize.lomponent.components.TextComponent tc) {
                    LomponentStyleContainer previousStyle = new LomponentStyleContainer();
                    tc.onStyleGet(previousStyle, 0);
                    StringBuilder csb = new StringBuilder();
                    String content = tc.getContent();
                    for (int i = 0; i <= content.length(); i++) {
                        LomponentStyleContainer csc = new LomponentStyleContainer();
                        char c = '.';
                        if (i < content.length()) {
                            tc.onStyleGet(csc, i);
                            c = content.charAt(i);
                        }
                        if (i == content.length() || !previousStyle.equals(csc)) {
                            TextComponent textComponent = new TextComponent(csb.toString());
                            csb = new StringBuilder();
                            var styles = previousStyle.getStyles();
                            for (Map.Entry<Object, Object> kv :
                                    styles.entrySet()) {
                                if (kv.getKey() instanceof DecorationComponent.Decoration dec && (boolean)(kv.getValue())) {
                                    switch (dec) {
                                        case Bold -> textComponent.setBold(true);
                                        case Italic -> textComponent.setItalic(true);
                                        case Obfuscated -> textComponent.setObfuscated(true);
                                        case Underlined -> textComponent.setUnderlined(true);
                                        case Strikethrough -> textComponent.setStrikethrough(true);
                                    }
                                }
                                else if (kv.getValue() instanceof Color color) {
                                    textComponent.setColor(ChatColor.of(new java.awt.Color(color.getR(), color.getG(), color.getB())));
                                }
                            }

                            textComponentsList.add(textComponent);
                            previousStyle = csc;
                        }
                        if (i == content.length()) break;
                        csb.append(c);
                    }
                }
                else if (cm instanceof GroupComponent gc) {
                    _IterateOverComponent(gc, textComponentsList);
                }
            }
        }

        public static String GetTimestampString(long time) {
            long month = time / 2629800;
            long day = (time % 2629800) / 86400;
            long hour = (time % 86400) / 3600;
            long minutes = (time % 3600) / 60;
            long seconds = time % 60;
            List<String> s = new ArrayList<>();
            if (month > 0) s.add(_instance._translation.getTranslation("timestamp_months").formatted(month));
            if (day > 0) s.add(_instance._translation.getTranslation("timestamp_days").formatted(day));
            if (hour > 0) s.add(_instance._translation.getTranslation("timestamp_hours").formatted(hour));
            if (minutes > 0) s.add(_instance._translation.getTranslation("timestamp_minutes").formatted(minutes));
            if (seconds > 0 ||
                Arrays.stream(new long[] {month,day,hour,minutes}).allMatch((l) -> l == 0)
            ) s.add(_instance._translation.getTranslation("timestamp_seconds").formatted(seconds));
            return String.join(" ", s);
        }

        public static String GetDateString(long epoch) {
            var dateTime = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC);
            return BaseComponent.toPlainText(ParseLomponent(Ulyanovsk.getInstance().getTranslation().getTranslation("date_pattern"), new HashMap<>() {{
                put("day", () -> Integer.toString(dateTime.getDayOfMonth()));
                put("month", () -> Integer.toString(dateTime.getMonthValue()));
                put("year", () -> Integer.toString(dateTime.getYear()));
            }}));
        }

        public static String GetTimeString(long epoch) {
            var dateTime = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC);
            return BaseComponent.toPlainText(ParseLomponent(Ulyanovsk.getInstance().getTranslation().getTranslation("time_pattern"), new HashMap<>() {{
                put("second", () -> Integer.toString(dateTime.getSecond()));
                put("minute", () -> Integer.toString(dateTime.getMinute()));
                put("hour", () -> Integer.toString(dateTime.getHour()));
            }}));
        }

        public static String GetDatetimeString(long epoch) {
            var dateTime = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC);
            return BaseComponent.toPlainText(ParseLomponent(Ulyanovsk.getInstance().getTranslation().getTranslation("datetime_pattern"), new HashMap<>() {{
                put("day", () -> Integer.toString(dateTime.getDayOfMonth()));
                put("month", () -> Integer.toString(dateTime.getMonthValue()));
                put("year", () -> Integer.toString(dateTime.getYear()));
                put("second", () -> Integer.toString(dateTime.getSecond()));
                put("minute", () -> Integer.toString(dateTime.getMinute()));
                put("hour", () -> Integer.toString(dateTime.getHour()));
            }}));
        }
    }

    private static class VoidWorldGenerator extends ChunkGenerator {
        @Override
        public boolean shouldGenerateSurface() {
            return false;
        }
    }
}
