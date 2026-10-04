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

import java.util.HashSet;
import java.util.Set;

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

    // Items we've already warned about, so a broken override doesn't spam the log on every tooltip refresh.
    private static final Set<Object> WARNED = new HashSet<>();

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

    private static void warnOnce(Object key, String message, Object... args)
    {
        if(WARNED.add(key))
            LOGGER.warn(message, args);
    }

    /**
     * Checks that a link can safely be opened: its entry exists and its resolved page is inside the entry.
     */
    private static boolean isValidLink(ManualLink link)
    {
        if(link==null)
            return false;
        try
        {
            ManualEntry entry = link.getKey();
            if(entry==null)
                return false;
            int pageCount = entry.getPageCount();
            int page = link.getPage();
            return page>=0&&page<pageCount;
        }
        catch(Exception e)
        {
            return false;
        }
    }

    /**
     * Never throws. Returns the override link if it is valid, otherwise IE's default link for the item, otherwise
     * null (no link, so no tooltip line).
     */
    static ManualLink resolveLink(ItemStack stack)
    {
        try
        {
            if(!manualInitSucceeded||stack==null||stack.isEmpty())
                return null;

            ManualInstance manual = ManualHelper.getManual();
            if(manual==null)
                return null;

            ManualLinkOverrides.LinkOverride override = ManualLinkOverrides.getOverride(stack.getItem());
            if(override!=null)
            {
                ManualLink overrideLink = buildOverrideLink(manual, stack, override);
                if(overrideLink!=null)
                    return overrideLink;
                // Invalid override: fall through to IE's own link
            }

            ManualLink link = manual.getManualLink(stack);
            return isValidLink(link) ? link : null;
        }
        catch(Exception e)
        {
            warnOnce("resolve:"+stack, "Failed to resolve a manual link, ignoring it: {}", e.toString());
            return null;
        }
    }

    private static ManualLink buildOverrideLink(
            ManualInstance manual, ItemStack stack, ManualLinkOverrides.LinkOverride override
    )
    {
        Object itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
        try
        {
            if(override.entry()==null)
                return null;
            ManualEntry overrideEntry = manual.getEntry(override.entry());
            if(overrideEntry==null)
            {
                warnOnce(
                        "entry:"+itemKey,
                        "Manual_links override for {} points at unknown manual entry {}, using default link",
                        itemKey, override.entry()
                );
                return null;
            }

            ManualLink link = new ManualLink(overrideEntry, null, override.page()-1);
            if(!isValidLink(link))
            {
                warnOnce(
                        "page:"+itemKey,
                        "Manual_links override for {} uses page {} which is outside entry {}, using default link",
                        itemKey, override.page(), override.entry()
                );
                return null;
            }
            return link;
        }
        catch(Exception e)
        {
            warnOnce("override:"+itemKey, "Manual_links override for {} is unusable ({}), using default link",
                    itemKey, e.toString());
            return null;
        }
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
        try
        {
            handleTooltip(event);
        }
        catch(Exception e)
        {
            isHolding = false;
            warnOnce("tooltip", "Failed to add the manual tooltip, skipping: {}", e.toString());
        }
    }

    private static void handleTooltip(ItemTooltipEvent event)
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
        try
        {
            handleKeyPressed(event);
        }
        catch(Exception e)
        {
            isHolding = false;
            warnOnce("keypress", "Failed to handle the manual keybind, ignoring: {}", e.toString());
        }
    }

    private static void handleKeyPressed(ScreenEvent.KeyPressed.Pre event)
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
            manualInitSucceeded = false;
            manualInitGaveUp = false;
            manualInitAttempts = 0;
            WARNED.clear();
            ticksSinceLastAttempt = RETRY_INTERVAL_TICKS; // attempt immediately on the first tick in-world
        }
        wasLevelPresent = levelPresent;

        if(levelPresent)
        {
            try
            {
                ensureManualIndexed();
            }
            catch(Exception e)
            {
                warnOnce("tick-init", "Unexpected error while initialising manual links: {}", e.toString());
            }
        }

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
            if(!isValidLink(link))
            {
                LOGGER.warn("Refusing to open an invalid manual link");
                return;
            }

            ManualInstance manual = ManualHelper.getManual();
            if(manual==null)
                return;
            var screen = manual.getGui();
            if(screen==null)
                return;

            Minecraft mc = Minecraft.getInstance();
            var previous = mc.screen;
            mc.setScreen(screen);
            try {
                link.changePage(screen, false);
            } catch(Exception e) {
                // Don't leave the player stuck on a half-initialised manual screen.
                mc.setScreen(previous);
                throw e;
            }
        } catch(Exception e) {
            LOGGER.error("Failed to open the Engineer's Manual to a specific page ", e);
        }
    }

    private ManualLinkHandler()
    {
    }
}