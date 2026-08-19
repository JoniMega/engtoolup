package cat.joni.engtoolup.client;

import blusunrize.lib.manual.ManualInstance.ManualLink;
import cat.joni.engtoolup.Engtoolup;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@EventBusSubscriber(modid = Engtoolup.MODID, value = Dist.CLIENT)
public class ManualLinkSlowStressTest {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int DELAY_TICKS = 5;

    private static final Deque<QueuedEntry> queue = new ArrayDeque<>();
    private static boolean running = false;
    private static int ticksUntilNext = 0;
    private static int total = 0;
    private static int opened = 0;

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("engtoolup")
                        .then(Commands.literal("manualstresstest")
                                .executes(ManualLinkSlowStressTest::start)
                                .then(Commands.literal("stop")
                                        .executes(ManualLinkSlowStressTest::stop)))
        );
    }

    private static int start(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (running) {
            source.sendFailure(Component.literal(
                    "A manual stress test is already running (" + opened + "/" + total + "). " +
                            "Use \"/engtoolup manualstresstest stop\" to cancel it first."
            ));
            return 0;
        }

        ManualLinkHandler.ensureManualIndexed();

        List<QueuedEntry> found = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty())
                continue;

            ManualLink link;
            try {
                link = ManualLinkHandler.resolveLink(stack);
            } catch (Exception e) {
                LOGGER.error("Manual stress test: failed to resolve a manual link for {}",
                        BuiltInRegistries.ITEM.getKey(item), e);
                continue;
            }

            if (link != null)
                found.add(new QueuedEntry(stack, link));
        }

        if (found.isEmpty()) {
            source.sendFailure(Component.literal(
                    "No items with a resolvable manual entry were found. Is the Engineer's Manual loaded?"
            ));
            return 0;
        }

        queue.clear();
        queue.addAll(found);
        total = found.size();
        opened = 0;
        ticksUntilNext = 0;
        running = true;

        LOGGER.info("Manual stress test started: {} item(s) with a manual entry, {} ticks apart", total, DELAY_TICKS);
        source.sendSuccess(() -> Component.literal(
                "Manual stress test started: " + total + " item(s) with a manual entry, " + DELAY_TICKS + " ticks apart. " +
                        "Watch the log for progress."
        ), false);

        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!running) {
            source.sendFailure(Component.literal("No manual stress test is currently running."));
            return 0;
        }

        int openedSoFar = opened;
        int totalCount = total;
        cancel();

        source.sendSuccess(() -> Component.literal(
                "Manual stress test cancelled after " + openedSoFar + "/" + totalCount + " entries."
        ), false);
        return 1;
    }

    private static void cancel() {
        running = false;
        queue.clear();
        ticksUntilNext = 0;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!running)
            return;

        if (Minecraft.getInstance().level == null) {
            // Bailed out of the world mid-run (disconnected, closed the game to the title screen, etc.)
            cancel();
            return;
        }

        if (ticksUntilNext > 0) {
            ticksUntilNext--;
            return;
        }

        QueuedEntry next = queue.poll();
        if (next == null) {
            LOGGER.info("Manual stress test finished: opened {}/{} entries", opened, total);
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(Component.literal(
                        "Manual stress test finished: opened " + opened + "/" + total + " entries."
                ), false);
            }
            cancel();
            return;
        }

        opened++;
        LOGGER.info("[{}/{}] Opening manual entry for {}",
                opened, total, BuiltInRegistries.ITEM.getKey(next.stack.getItem()));

        try {
            ManualLinkHandler.openLink(next.link);
        } catch (Exception e) {
            LOGGER.error("Manual stress test: failed to open manual entry for {}",
                    BuiltInRegistries.ITEM.getKey(next.stack.getItem()), e);
        }

        ticksUntilNext = DELAY_TICKS;
    }

    private record QueuedEntry(ItemStack stack, ManualLink link) {
    }

    private ManualLinkSlowStressTest() {
    }
}