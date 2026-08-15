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

    private static ItemStack hoveredStack = ItemStack.EMPTY;
    private static boolean manualInitAttempted = false;
    private static boolean manualInitSucceeded = false;

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

    static void ensureManualIndexed()
    {
        if(manualInitAttempted)
            return;
        manualInitAttempted = true;

        try
        {
            ManualHelper.getManual().getGui();
            manualInitSucceeded = true;
        }
        catch(Exception e)
        {
            LOGGER.error("Failed to initialize the Engineer's Manual for page-linking. Manual links will be unavailable this session.", e);
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

        if(Minecraft.getInstance().level!=null)
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