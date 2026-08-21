package cat.joni.engtoolup.addons.fluidSensor;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FastColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public final class FluidSensorAlgorithm {
    public static int getModeColor(TextureAtlasSprite sprite) {
        int width = sprite.contents().width();
        int height = sprite.contents().height();

        Map<Integer, Integer> counts = new HashMap<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = sprite.getPixelRGBA(0, x, y);
                int rgb;
                if (FastColor.ABGR32.alpha(pixel) != 0) {
                    rgb = FastColor.ARGB32.color(
                            FastColor.ABGR32.red(pixel), FastColor.ABGR32.green(pixel), FastColor.ABGR32.blue(pixel)
                    );
                    counts.merge(rgb, 1, Integer::sum);
                }
            }
        }

        int bestColor = 0xFFFFFFFF;
        int bestCount = 0;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                bestColor = entry.getKey();
            }
        }

        return bestColor;
    }

    public static int getAverageColor(TextureAtlasSprite sprite) {
        int width = sprite.contents().width();
        int height = sprite.contents().height();

        long sumR = 0, sumG = 0, sumB = 0, sumA = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = sprite.getPixelRGBA(0, x, y);

                int alpha = FastColor.ABGR32.alpha(pixel);
                if (alpha == 0)
                    continue;

                sumR += (long) FastColor.ABGR32.red(pixel) * alpha;
                sumG += (long) FastColor.ABGR32.green(pixel) * alpha;
                sumB += (long) FastColor.ABGR32.blue(pixel) * alpha;
                sumA += alpha;
            }
        }

        if (sumA == 0)
            return 0xFFFFFFFF;

        int r = (int) (sumR / sumA);
        int g = (int) (sumG / sumA);
        int b = (int) (sumB / sumA);

        return FastColor.ARGB32.color(r, g, b);
    }

    private FluidSensorAlgorithm() {
    }
}