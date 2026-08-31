package cat.joni.engtoolup.client;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.lib.manual.ManualEntry;
import blusunrize.lib.manual.ManualInstance;
import blusunrize.lib.manual.ManualInstance.ManualLink;
import cat.joni.engtoolup.Engtoolup;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * This feature I hope gets implemented into IE. My implementations is a long patchwork replicating what create does.
 *
 * This creates a Tooltip to all items listed in assets/engtoolup/manual_links and items that are referenced in the
 * manual.
 *
 * Holding the KEYBIND should change the Tooltip to a loading bar and then opens the manual.
 */
@Mod.EventBusSubscriber(modid = Engtoolup.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ManualLinkHandler
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final long HOLD_DURATION_MS = 1000;
    private static final int BAR_LENGTH = 10;

    // Building the manual's GUI for the first time makes IE index every entry, including multiblock structure
    // templates. On a freshly created world this can race against the integrated server finishing its own setup
    // (see the "Template ... does not exist!" RuntimeException), so a single failed attempt doesn't necessarily
    // mean the manual is unusable -- it's worth retrying a few times before giving up for the session.
    private static final int RETRY_INTERVAL_TICKS = 40; // 2 seconds
    private static final int MAX_INIT_ATTEMPTS = 10; // ~20 seconds of retrying, after the immediate first try

    private static ItemStack hoveredStack = ItemStack.EMPTY;
    private static boolean manualInitSucceeded = false;
    private static boolean manualInitGaveUp = false;
    private static int manualInitAttempts = 0;
    private static int ticksSinceLastAttempt = 0;
    private static boolean wasLevelPresent = false;

    private static boolean isHolding = false;
    private static long holdStartTimeMillis = 0;
    private static ItemStack holdTargetStack = ItemStack.EMPTY;

    private static boolean pendingOpen = false;
    private static ManualLink pendingLink = null;

    static ManualLink resolveLink(ItemStack stack)
    {
        if(!manualInitSucceeded)
            return null;

        ManualInstance manual = ManualHelper.getManual();

        ManualLinkOverrides.LinkOverride override = ManualLinkOverrides.getOverride(stack.getItem());
        if(override!=null) {
            ManualEntry overrideEntry = manual.getEntry(override.entry());
            if(overrideEntry!=null) {
                return new ManualLink(overrideEntry, null, override.page()-1);
            }
            LOGGER.warn(
                    "Manual_links override for {} points at unknown manual entry {}",
                    ForgeRegistries.ITEMS.getKey(stack.getItem()), override.entry()
            );
        }

        return manual.getManualLink(stack);
    }

    /**
     * Called once per client tick while a level is loaded. Resets its retry state whenever the player (re)joins a
     * world, then attempts to build/fetch the manual's GUI immediately and, if that fails, retries every
     * {@link #RETRY_INTERVAL_TICKS} ticks up to {@link #MAX_INIT_ATTEMPTS} times before giving up for the session.
     */
    static void ensureManualIndexed()
    {
        if(manualInitSucceeded || manualInitGaveUp)
            return;

        if(ticksSinceLastAttempt < RETRY_INTERVAL_TICKS)
        {
            ticksSinceLastAttempt++;
            return;
        }
        ticksSinceLastAttempt = 0;
        manualInitAttempts++;

        Minecraft mc = Minecraft.getInstance();
        LOGGER.info(
                "Attempting to initialize the Engineer's Manual for page-linking (attempt {}/{}, level={}, "+
                        "connection={}, world time={} ticks)",
                manualInitAttempts, MAX_INIT_ATTEMPTS,
                mc.level==null ? "null" : mc.level.dimension().location(),
                mc.getConnection()!=null,
                mc.level==null ? -1 : mc.level.getGameTime()
        );

        boolean lastAttempt = manualInitAttempts>=MAX_INIT_ATTEMPTS;

        ManualInstance manual;
        try
        {
            manual = ManualHelper.getManual();
        }
        catch(Exception e)
        {
            logInitFailure("ManualHelper.getManual() threw", e, lastAttempt);
            return;
        }

        if(manual==null)
        {
            logInitFailure(
                    "ManualHelper.getManual() returned null. This usually means Immersive Engineering hasn't "+
                            "finished its own client setup yet",
                    null, lastAttempt
            );
            return;
        }

        LOGGER.info("ManualHelper.getManual() returned {}, now building/fetching its GUI (manual.getGui())", manual);

        try
        {
            var gui = manual.getGui();
            if(gui==null)
            {
                logInitFailure("manual.getGui() returned null", null, lastAttempt);
                return;
            }

            manualInitSucceeded = true;
            LOGGER.info(
                    "Engineer's Manual initialized successfully for page-linking after {} attempt(s) (gui={})",
                    manualInitAttempts, gui
            );
        }
        catch(Exception e)
        {
            logInitFailure("manual.getGui() threw", e, lastAttempt);
        }
    }

    private static void logInitFailure(String reason, Exception e, boolean lastAttempt)
    {
        manualInitSucceeded = false;

        if(lastAttempt)
        {
            manualInitGaveUp = true;
            String message =
                    "Failed to initialize the Engineer's Manual for page-linking after "+manualInitAttempts+
                            " attempt(s): "+reason+". Giving up for this session -- manual links will be "+
                            "unavailable until the world is rejoined.";
            if(e!=null)
                LOGGER.error(message, e);
            else
                LOGGER.error(message);
        }
        else
        {
            // Likely just IE/the integrated server not being fully ready yet -- log briefly and let the next
            // retry (in RETRY_INTERVAL_TICKS) try again. Include the full stack trace on the very first failure
            // only, so we still capture diagnostics without spamming the log on every retry.
            String message =
                    "Attempt "+manualInitAttempts+"/"+MAX_INIT_ATTEMPTS+
                            " to initialize the Engineer's Manual for page-linking failed: "+reason+
                            ". Will retry in "+RETRY_INTERVAL_TICKS+" ticks.";
            if(e!=null)
            {
                if(manualInitAttempts==1)
                    LOGGER.warn(message, e);
                else
                    LOGGER.warn("{} ({}: {})", message, e.getClass().getName(), e.getMessage());
            }
            else
            {
                LOGGER.warn(message);
            }
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event)
    {
        ItemStack stack = event.getItemStack();
        hoveredStack = stack;

        if(isHolding && !ItemStack.matches(stack, holdTargetStack))
            isHolding = false;

        ManualLink link = resolveLink(stack);
        if(link==null)
            return;

        if(isHolding) {
            long elapsed = System.currentTimeMillis() - holdStartTimeMillis;
            float fraction = Math.min(1.0f, elapsed/(float)HOLD_DURATION_MS);

            if(fraction >= 1.0f) {
                pendingOpen = true;
                pendingLink = link;
                isHolding = false;
            } else {
                event.getToolTip().add(Component.literal(buildProgressBar(fraction)));
            }
        } else if(!EngtoolupKeybinds.OPEN_MANUAL_ENTRY.isUnbound()) {
            event.getToolTip().add(Component.translatable(
                    "tooltip.engtoolup.openManualEntry",
                    EngtoolupKeybinds.OPEN_MANUAL_ENTRY.getTranslatedKeyMessage()
            ).withStyle(ChatFormatting.GRAY));
        } else {
            event.getToolTip().add(Component.translatable(
                    "tooltip.engtoolup.noKeyToManual"
            ).withStyle(ChatFormatting.GRAY));
        }
    }

    private static String buildProgressBar(float fraction)
    {
        int filled = Math.max(0, Math.min(BAR_LENGTH, Math.round(fraction*BAR_LENGTH)));
        StringBuilder bar = new StringBuilder();
        bar.append(ChatFormatting.GREEN);
        bar.append("|".repeat(filled));
        bar.append(ChatFormatting.DARK_GRAY);
        bar.append(":".repeat(BAR_LENGTH-filled));
        return bar.toString();
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event)
    {
        if(!EngtoolupKeybinds.OPEN_MANUAL_ENTRY.matches(event.getKeyCode(), event.getScanCode()))
            return;

        if(isHolding) {
            event.setCanceled(true);
            return;
        }

        if(resolveLink(hoveredStack)==null)
            return; // nothing to inspect right now

        isHolding = true;
        holdStartTimeMillis = System.currentTimeMillis();
        holdTargetStack = hoveredStack;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenKeyReleased(ScreenEvent.KeyReleased.Pre event)
    {
        if(!EngtoolupKeybinds.OPEN_MANUAL_ENTRY.matches(event.getKeyCode(), event.getScanCode()))
            return;
        isHolding = false;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if(event.phase!=TickEvent.Phase.END)
            return;

        boolean levelPresent = Minecraft.getInstance().level!=null;
        if(levelPresent && !wasLevelPresent)
        {
            // (Re)joined a world -- reset retry state so a fresh world gets its own full set of attempts instead
            // of inheriting a previous world's "gave up" state.
            manualInitSucceeded = false;
            manualInitGaveUp = false;
            manualInitAttempts = 0;
            ticksSinceLastAttempt = RETRY_INTERVAL_TICKS; // attempt immediately on the first tick in-world
        }
        wasLevelPresent = levelPresent;

        if(levelPresent)
            ensureManualIndexed();

        if(!pendingOpen)
            return;
        pendingOpen = false;

        ManualLink link = pendingLink;
        pendingLink = null;
        if(link==null)
            return;

        openLink(link);
    }

    static void openLink(ManualLink link)
    {
        try {
            ManualInstance manual = ManualHelper.getManual();
            var screen = manual.getGui();
            if(screen==null)
                return;

            Minecraft.getInstance().setScreen(screen);
            link.changePage(screen, false);
        } catch(Exception e) {
            LOGGER.error("Failed to open the Engineer's Manual to a specific page ", e);
        }
    }

    private ManualLinkHandler()
    {
    }
}