package cz.raixo.blocks.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import cz.raixo.blocks.MineBlocksPlugin;
import cz.raixo.blocks.block.MineBlock;
import cz.raixo.blocks.block.health.BlockHealth;
import cz.raixo.blocks.block.hologram.BlockHologram;
import cz.raixo.blocks.config.blocks.BlocksConfig;
import cz.raixo.blocks.menu.edit.EditMenu;
import cz.raixo.blocks.util.color.Colors;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * The {@code /mineblocks} command tree.
 *
 * <p>Built on Paper's Brigadier API rather than a shaded command framework, so there is no
 * reflection into server internals and tab completion comes from the server itself.</p>
 */
public final class MineBlocksCommand {

    private static final String BLOCK_ARG = "block";

    public static LiteralCommandNode<CommandSourceStack> build(MineBlocksPlugin plugin) {
        MineBlocksCommand command = new MineBlocksCommand(plugin);
        return command.tree();
    }

    private final MineBlocksPlugin plugin;

    private MineBlocksCommand(MineBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    private LiteralCommandNode<CommandSourceStack> tree() {
        return Commands.literal("mineblocks")
                .requires(source -> source.getSender().hasPermission("mineblocks.admin"))
                .executes(ctx -> help(ctx.getSource().getSender()))
                .then(Commands.literal("help")
                        .executes(ctx -> help(ctx.getSource().getSender())))
                .then(Commands.literal("reload")
                        .requires(perm("mineblocks.admin.reload"))
                        .executes(ctx -> reload(ctx.getSource().getSender())))
                .then(Commands.literal("list")
                        .requires(perm("mineblocks.admin.list"))
                        .executes(ctx -> list(ctx.getSource().getSender())))
                .then(Commands.literal("teleport")
                        .requires(perm("mineblocks.admin.teleport"))
                        .then(blockArgument().executes(this::teleport)))
                .then(Commands.literal("version")
                        .executes(ctx -> version(ctx.getSource().getSender())))
                .then(Commands.literal("reset")
                        .requires(perm("mineblocks.admin.reset"))
                        .then(blockArgument().executes(this::reset)))
                .then(Commands.literal("sethealth")
                        .requires(perm("mineblocks.admin.sethealth"))
                        .then(blockArgument()
                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                        .executes(this::setHealth))))
                .then(Commands.literal("edit")
                        .requires(perm("mineblocks.admin.edit"))
                        .then(blockArgument().executes(this::edit)))
                .then(Commands.literal("create")
                        .requires(perm("mineblocks.admin.create"))
                        .then(Commands.argument(BLOCK_ARG, StringArgumentType.word())
                                .executes(this::create)))
                .then(Commands.literal("remove")
                        .requires(perm("mineblocks.admin.remove"))
                        .then(blockArgument().executes(this::remove)))
                .then(Commands.literal("hologram")
                        .requires(perm("mineblocks.admin.hologram"))
                        .then(Commands.literal("show")
                                .then(blockArgument().executes(this::showHologram)))
                        .then(Commands.literal("addline")
                                .then(blockArgument()
                                        .then(Commands.argument("content", StringArgumentType.greedyString())
                                                .executes(this::addLine))))
                        .then(Commands.literal("removeline")
                                .then(blockArgument()
                                        .then(Commands.argument("line", IntegerArgumentType.integer(1))
                                                .executes(this::removeLine))))
                        .then(Commands.literal("setline")
                                .then(blockArgument()
                                        .then(Commands.argument("line", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("content", StringArgumentType.greedyString())
                                                        .executes(this::setLine))))))
                .build();
    }

    private java.util.function.Predicate<CommandSourceStack> perm(String permission) {
        return source -> source.getSender().hasPermission(permission);
    }

    private ArgumentBuilder<CommandSourceStack, ?> blockArgument() {
        return Commands.argument(BLOCK_ARG, StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    String remaining = builder.getRemainingLowerCase();
                    plugin.getBlockRegistry().getBlocks().stream()
                            .map(MineBlock::getId)
                            .filter(id -> id.toLowerCase().startsWith(remaining))
                            .forEach(builder::suggest);
                    return builder.buildFuture();
                });
    }

    // --- commands -------------------------------------------------------------------------------

    private int help(CommandSender sender) {
        String base = "#2C74B3/mb ";
        Colors.send(sender,
                "#205295&lMineBlocks help menu:",
                base + "reload &7Reloads the configuration and every block",
                base + "version &7Shows the installed version",
                base + "create <block> &7Creates a new mine block at your position",
                base + "edit <block> &7Opens the editor for a block",
                base + "hologram show <block> &7Shows the hologram lines of a block",
                base + "hologram addline <block> <content> &7Adds a hologram line",
                base + "hologram removeline <block> <line> &7Removes a hologram line",
                base + "hologram setline <block> <line> <content> &7Replaces a hologram line",
                base + "list &7Lists every mine block",
                base + "reset <block> &7Resets a block's health and cooldown",
                base + "sethealth <block> <health> &7Sets the remaining health of a block",
                base + "remove <block> &7Deletes a block (console only)",
                base + "teleport <block> &7Teleports you to a block"
        );
        return Command.SUCCESS;
    }

    private int reload(CommandSender sender) {
        plugin.reload();
        Colors.send(sender, "#2C74B3Plugin was reloaded!");
        return Command.SUCCESS;
    }

    private int version(CommandSender sender) {
        Colors.send(sender, "#2C74B3MineBlocks version " + plugin.getPluginMeta().getVersion());
        return Command.SUCCESS;
    }

    private int list(CommandSender sender) {
        Collection<MineBlock> blocks = plugin.getBlockRegistry().getBlocks();
        if (blocks.isEmpty()) {
            Colors.send(sender, "#DF2E38There are no blocks!");
            return Command.SUCCESS;
        }
        Colors.send(sender, "#2C74B3List of blocks:");
        for (MineBlock block : blocks) {
            Colors.send(sender, "#0A2647 - #2C74B3" + block.getId() + " #205295" + describeLocation(block));
        }
        return Command.SUCCESS;
    }

    private String describeLocation(MineBlock block) {
        return Optional.ofNullable(block.getLocation())
                .map(loc -> Optional.ofNullable(loc.getWorld()).map(World::getName).orElse("unknown world")
                        + ", " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ())
                .orElse("");
    }

    private int teleport(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            Colors.send(sender, "#DF2E38Only a player can teleport!");
            return Command.SUCCESS;
        }
        return withBlock(ctx, (block, ignored) -> {
            player.teleport(block.getLocation().clone().add(0.5, 1.5, 0.5), PlayerTeleportEvent.TeleportCause.COMMAND);
            Colors.send(player, "#2C74B3Teleported to block " + block.getId() + "!");
        });
    }

    private int reset(CommandContext<CommandSourceStack> ctx) {
        return withBlock(ctx, (block, sender) -> {
            block.reset();
            Colors.send(sender, "#2C74B3Block " + block.getId() + " was reset!");
        });
    }

    private int setHealth(CommandContext<CommandSourceStack> ctx) {
        int health = IntegerArgumentType.getInteger(ctx, "health");
        return withBlock(ctx, (block, sender) -> {
            BlockHealth blockHealth = block.getHealth();
            blockHealth.setHealth(Math.max(1, Math.min(health, blockHealth.getMaxHealth())));
            block.getHologram().update();
            Colors.send(sender, "#2C74B3Health of block " + block.getId() + " was set to " + blockHealth.getHealth() + "!");
        });
    }

    private int edit(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            Colors.send(sender, "#DF2E38The editor can only be opened by a player!");
            return Command.SUCCESS;
        }
        return withBlock(ctx, (block, ignored) -> new EditMenu(block).open(player));
    }

    private int create(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            Colors.send(sender, "#DF2E38A block can only be created by a player, it is placed at your position!");
            return Command.SUCCESS;
        }
        String name = StringArgumentType.getString(ctx, BLOCK_ARG);
        if (plugin.getBlockRegistry().get(name) != null) {
            Colors.send(sender, "#DF2E38Block with name " + name + " already exists!");
            return Command.SUCCESS;
        }
        MineBlock block = MineBlock.createDefault(plugin, name, player.getLocation().getBlock().getLocation());
        plugin.getBlockRegistry().register(block);
        BlocksConfig blocksConfig = plugin.getConfiguration().getBlocksConfig();
        blocksConfig.setBlock(block);
        plugin.saveConfiguration();
        Colors.send(sender, "#2C74B3Block " + name + " was created! Edit it with /mb edit " + name);
        return Command.SUCCESS;
    }

    private int remove(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof ConsoleCommandSender)) {
            Colors.send(sender, "#DF2E38Blocks can only be deleted from the GUI editor or the console!");
            return Command.SUCCESS;
        }
        return withBlock(ctx, (block, ignored) -> {
            plugin.getBlockRegistry().delete(block);
            Colors.send(sender, "#2C74B3Block " + block.getId() + " was successfully deleted!");
        });
    }

    private int showHologram(CommandContext<CommandSourceStack> ctx) {
        return withBlock(ctx, (block, sender) -> showHologram(sender, block));
    }

    private int addLine(CommandContext<CommandSourceStack> ctx) {
        String content = StringArgumentType.getString(ctx, "content");
        return withBlock(ctx, (block, sender) -> {
            block.getHologram().addLine(content);
            Colors.send(sender, "#2C74B3Line was inserted!");
            showHologram(sender, block);
            saveBlock(block);
        });
    }

    private int removeLine(CommandContext<CommandSourceStack> ctx) {
        int line = IntegerArgumentType.getInteger(ctx, "line") - 1;
        return withBlock(ctx, (block, sender) -> {
            BlockHologram hologram = block.getHologram();
            if (line < 0 || line >= hologram.getLines().size()) {
                Colors.send(sender, "#DF2E38Line " + (line + 1) + " does not exist!");
                return;
            }
            hologram.removeLine(line);
            Colors.send(sender, "#2C74B3Line was removed!");
            showHologram(sender, block);
            saveBlock(block);
        });
    }

    private int setLine(CommandContext<CommandSourceStack> ctx) {
        int line = IntegerArgumentType.getInteger(ctx, "line") - 1;
        String content = StringArgumentType.getString(ctx, "content");
        return withBlock(ctx, (block, sender) -> {
            BlockHologram hologram = block.getHologram();
            if (line < 0 || line >= hologram.getLines().size()) {
                Colors.send(sender, "#DF2E38Line " + (line + 1) + " does not exist!");
                return;
            }
            hologram.setLine(line, content);
            Colors.send(sender, "#2C74B3Line was updated!");
            showHologram(sender, block);
            saveBlock(block);
        });
    }

    // --- helpers --------------------------------------------------------------------------------

    private int withBlock(CommandContext<CommandSourceStack> ctx, BiConsumer<MineBlock, CommandSender> action) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, BLOCK_ARG);
        MineBlock block = plugin.getBlockRegistry().get(name);
        if (block == null) {
            Colors.send(sender, "#DF2E38Block with name " + name + " was not found!");
            return Command.SUCCESS;
        }
        action.accept(block, sender);
        return Command.SUCCESS;
    }

    private void saveBlock(MineBlock block) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getConfiguration().getBlocksConfig().setBlock(block);
            plugin.saveConfiguration();
        });
    }

    /** Prints the hologram lines with click-to-edit shortcuts. Also used by the GUI editor. */
    public static void showHologram(Audience audience, MineBlock block) {
        String blockId = block.getId();
        Component message = Colors.component("#2C74B3Hologram of block " + blockId + ":");

        List<String> lines = block.getHologram().getLines();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            message = message.append(Component.newline()).append(
                    Component.text(" - ", NamedTextColor.DARK_GRAY)
                            .clickEvent(ClickEvent.suggestCommand("/mb hologram setline " + blockId + " " + (i + 1) + " " + line))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to edit!", TextColor.color(32, 82, 149))))
                            .append(Component.text(line, NamedTextColor.GRAY))
                            .append(Component.text(" [Remove]", TextColor.color(223, 46, 56))
                                    .hoverEvent(HoverEvent.showText(Component.text("Click to remove!", TextColor.color(223, 46, 56))))
                                    .clickEvent(ClickEvent.suggestCommand("/mb hologram removeline " + blockId + " " + (i + 1))))
            );
        }

        message = message.append(Component.newline()).append(
                Component.text(" Add new line", TextColor.color(44, 116, 179))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to add a line!", TextColor.color(32, 82, 149))))
                        .clickEvent(ClickEvent.suggestCommand("/mb hologram addline " + blockId + " "))
        );

        audience.sendMessage(message);
    }

    /** Brigadier result codes, named so the intent is obvious at the call sites. */
    private static final class Command {
        static final int SUCCESS = com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

}
