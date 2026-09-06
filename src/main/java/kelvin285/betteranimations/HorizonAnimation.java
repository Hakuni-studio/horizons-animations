/*
 * Horizon Animation — Create Sky player animation addon.
 *
 * Based on Kelvin's Better Animations by Kevin Merrill (Kelvin285).
 * Original project licensed under the MIT License; see LICENSE.
 */
package kelvin285.betteranimations;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;

@Mod(HorizonAnimation.MOD_ID)
public class HorizonAnimation {

    public static final String MOD_ID = "horizonanimation";

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
