package cat.joni.engtoolup.client;

import cat.joni.engtoolup.Engtoolup;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = Engtoolup.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ManualLinkOverrides extends SimpleJsonResourceReloadListener
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Item, LinkOverride> OVERRIDES = new HashMap<>();

    public ManualLinkOverrides()
    {
        super(new Gson(), "manual_links");
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event)
    {
        event.registerReloadListener(new ManualLinkOverrides());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler)
    {
        Map<Item, LinkOverride> updated = new HashMap<>();

        for(Map.Entry<ResourceLocation, JsonElement> fileEntry: data.entrySet()) {
            ResourceLocation fileId = fileEntry.getKey();

            JsonObject fileJson;

            try {
                fileJson = fileEntry.getValue().getAsJsonObject();
            } catch(Exception e) {
                LOGGER.warn("manual_links/{}.json isn't a JSON object, skipping", fileId);
                continue;
            }

            for(Map.Entry<String, JsonElement> itemEntry : fileJson.entrySet()) {
                String itemIdStr = itemEntry.getKey();
                try {
                    ResourceLocation itemId = ResourceLocation.parse(itemIdStr);
                    Item item = BuiltInRegistries.ITEM.getValue(itemId);
                    if(item==null) {
                        LOGGER.warn("manual_links/{}.json targets unknown item {}, skipping", fileId, itemId);
                        continue;
                    }

                    JsonObject linkJson = itemEntry.getValue().getAsJsonObject();
                    ResourceLocation entryId = ResourceLocation.parse(GsonHelper.getAsString(linkJson, "entry"));
                    int page = GsonHelper.getAsInt(linkJson, "page", 1);
                    if(page<1) {
                        LOGGER.warn(
                                "manual_links/{}.json: {} has page {} -- pages are numbered from 1, treating as 1",
                                fileId, itemIdStr, page
                        );
                        page = 1;
                    }

                    if(updated.containsKey(item)) {
                        LOGGER.warn(
                                "manual_links/{}.json overrides {} again -- another loaded manual_links file "+
                                        "already targets it, and which one wins isn't guaranteed",
                                fileId, itemIdStr
                        );
                    }

                    updated.put(item, new LinkOverride(entryId, page));
                } catch(Exception e) {
                    LOGGER.warn("Failed to parse manual_links/{}.json entry {}: {}", fileId, itemIdStr, e.getMessage());
                }
            }
        }

        OVERRIDES.clear();
        OVERRIDES.putAll(updated);
    }


    @Nullable
    public static LinkOverride getOverride(Item item) {
        return OVERRIDES.get(item);
    }


    public record LinkOverride(ResourceLocation entry, int page) {
    }
}
