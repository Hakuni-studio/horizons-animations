/*
 * Based on Kelvin's Better Animations by Kevin Merrill (Kelvin285).
 */
package kelvin285.betteranimations.mixin;

import kelvin285.betteranimations.IPlayerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public PlayerEntityRendererMixin(EntityRendererProvider.Context ctx, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Shadow
    protected abstract void setModelProperties(AbstractClientPlayer player);

    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    public void render(AbstractClientPlayer player, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo info) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.gameRenderer.getMainCamera().isDetached() && player == minecraft.player) {
            return;
        }

        if (player.getAbilities().flying) {
            return;
        }

        poseStack.pushPose();

        boolean airborneFall = !player.onGround() && player.getDeltaMovement().y < 0;

        float leanX = airborneFall ? 0 : (float) player.getDeltaMovement().z;
        float leanZ = airborneFall ? 0 : -(float) player.getDeltaMovement().x;

        float turnLeanAmount = airborneFall ? 0 : ((IPlayerAccessor) player).getLeanAmount();
        float leanMultiplier = airborneFall ? 1 : ((IPlayerAccessor) player).getLeanMultiplier();
        float playerSquash = Mth.clamp(((IPlayerAccessor) player).getSquash(), -1, 1) * 0.08f;

        float hScale = Mth.lerp(Math.abs(playerSquash), 1, 0.92f);
        float vScale = Mth.lerp(Math.abs(playerSquash), 1, 1.08f);

        float yaw = (float) Math.toRadians(player.yBodyRot + 90);
        leanX += Math.cos(yaw) * turnLeanAmount;
        leanZ += Math.sin(yaw) * turnLeanAmount;

        leanX *= leanMultiplier;
        leanZ *= leanMultiplier;

        if (player.isFallFlying()) {
            leanX = 0;
            leanZ = 0;
            hScale = 1.0f;
            vScale = 1.0f;
        }

        Quaternionf quat = new Quaternionf();
        quat = new Matrix4f().rotate(leanX, new Vector3f(1, 0, 0)).rotate(leanZ, new Vector3f(0, 0, 1)).getNormalizedRotation(quat);

        poseStack.mulPose(quat);
        if (playerSquash != 0) {
            poseStack.scale(hScale, vScale, hScale);
        }
        this.setModelProperties(player);
        super.render(player, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();

        info.cancel();
    }
}
