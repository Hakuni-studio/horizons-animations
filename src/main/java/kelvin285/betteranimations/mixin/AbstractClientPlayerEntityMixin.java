/*
 * Based on Kelvin's Better Animations by Kevin Merrill (Kelvin285).
 */
package kelvin285.betteranimations.mixin;

import com.mojang.authlib.GameProfile;
import dev.kosmx.playerAnim.api.IPlayable;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.FirstPersonModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import kelvin285.betteranimations.HorizonAnimation;
import kelvin285.betteranimations.IPlayerAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerEntityMixin extends Player implements IPlayerAccessor {

    @Unique
    private final ModifierLayer<IAnimation> modAnimationContainer = new ModifierLayer<>();

    public AbstractClientPlayerEntityMixin(ClientLevel level, GameProfile gameProfile) {
        super(level, level.getSharedSpawnPos(), level.getSharedSpawnAngle(), gameProfile);
    }

    @Unique
    private KeyframeAnimation animIdle;
    @Unique
    private KeyframeAnimation animSneakIdle;
    @Unique
    private KeyframeAnimation animSneakWalk;
    @Unique
    private KeyframeAnimation animWalk;
    @Unique
    private KeyframeAnimation animRun;
    @Unique
    private KeyframeAnimation animTurnRight;
    @Unique
    private KeyframeAnimation animTurnLeft;
    @Unique
    private KeyframeAnimation animFalling;
    @Unique
    private KeyframeAnimation animLanding;
    @Unique
    private KeyframeAnimation animSwimming;
    @Unique
    private KeyframeAnimation animSwimIdle;
    @Unique
    private KeyframeAnimation animCrawlIdle;
    @Unique
    private KeyframeAnimation animCrawling;
    @Unique
    private KeyframeAnimation animEating;
    @Unique
    private KeyframeAnimation animClimbing;
    @Unique
    private KeyframeAnimation animClimbingIdle;
    @Unique
    private KeyframeAnimation animSprintStop;
    @Unique
    private KeyframeAnimation animFenceIdle;
    @Unique
    private KeyframeAnimation animFenceWalk;
    @Unique
    private KeyframeAnimation animEdgeIdle;
    @Unique
    private KeyframeAnimation animElytraFly;
    @Unique
    private KeyframeAnimation animFlintAndSteel;
    @Unique
    private KeyframeAnimation animFlintAndSteelSneak;
    @Unique
    private KeyframeAnimation animBoatIdle;
    @Unique
    private KeyframeAnimation animBoatLeftPaddle;
    @Unique
    private KeyframeAnimation animBoatRightPaddle;
    @Unique
    private KeyframeAnimation animBoatForward;
    @Unique
    private KeyframeAnimation animRolling;

    @Unique
    private int punchIndex = 0;
    @Unique
    private int jumpIndex = 0;
    @Unique
    private final KeyframeAnimation[] animJump = new KeyframeAnimation[2];
    @Unique
    private final KeyframeAnimation[] animFall = new KeyframeAnimation[2];
    @Unique
    private final KeyframeAnimation[] animPunch = new KeyframeAnimation[2];
    @Unique
    private final KeyframeAnimation[] animPunchSneaking = new KeyframeAnimation[2];
    @Unique
    private final KeyframeAnimation[] animSwordSwing = new KeyframeAnimation[2];
    @Unique
    private final KeyframeAnimation[] animSwordSwingSneak = new KeyframeAnimation[2];

    @Unique
    private float leanAmount = 0;
    @Unique
    private float leanMultiplier = 1;
    @Unique
    private float realLeanMultiplier = 1;
    @Unique
    private float squash = 0;
    @Unique
    private float realSquash = 0;
    @Unique
    private float momentum = 0;
    @Unique
    private float turnDelta = 0;
    @Unique
    private Vec3 lastPos = Vec3.ZERO;
    @Unique
    private boolean lastOnGround = false;
    @Unique
    private KeyframeAnimation currentAnimation;
    @Unique
    private boolean modified = false;
    @Unique
    private boolean armAnimationsEnabled = true;
    @Unique
    private boolean animationsLoaded = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(ClientLevel level, GameProfile profile, CallbackInfo info) {
        PlayerAnimationAccess.getPlayerAnimLayer((AbstractClientPlayer) (Object) this).addAnimLayer(1000, modAnimationContainer);

        FirstPersonModifier firstPersonModifier = new FirstPersonModifier();
        firstPersonModifier.setCurrentFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
        firstPersonModifier.setCurrentFirstPersonConfig(FirstPersonModifier.FirstPersonConfigEnum.ENABLE_BOTH_ARMS);
        modAnimationContainer.addModifierLast(firstPersonModifier);
    }

    @Unique
    private static KeyframeAnimation loadAnimation(String name) {
        IPlayable playable = PlayerAnimationRegistry.getAnimation(HorizonAnimation.asResource(name));
        if (playable instanceof KeyframeAnimation animation) {
            return animation;
        }
        return null;
    }

    @Unique
    private void loadAnimationsIfNeeded() {
        if (animationsLoaded) {
            return;
        }

        animIdle = loadAnimation("idle");
        animFall[0] = loadAnimation("fall_first");
        animFall[1] = loadAnimation("fall_second");
        animJump[0] = loadAnimation("jump_first");
        animJump[1] = loadAnimation("jump_second");
        animSneakIdle = loadAnimation("sneak_idle");
        animSneakWalk = loadAnimation("sneak_walk");
        animWalk = loadAnimation("walking");
        animRun = loadAnimation("running");
        animTurnRight = loadAnimation("turn_right");
        animTurnLeft = loadAnimation("turn_left");
        animPunch[0] = loadAnimation("punch_right");
        animPunch[1] = loadAnimation("punch_left");
        animPunchSneaking[0] = loadAnimation("punch_right_sneak");
        animPunchSneaking[1] = loadAnimation("punch_left_sneak");
        animSwordSwing[0] = loadAnimation("sword_swing_first");
        animSwordSwing[1] = loadAnimation("sword_swing_second");
        animSwordSwingSneak[0] = loadAnimation("sword_swing_sneak_first");
        animSwordSwingSneak[1] = loadAnimation("sword_swing_sneak_second");
        animFalling = loadAnimation("falling");
        animLanding = loadAnimation("landing");
        animSwimming = loadAnimation("swimming");
        animSwimIdle = loadAnimation("swim_idle");
        animCrawlIdle = loadAnimation("crawl_idle");
        animCrawling = loadAnimation("crawling");
        animEating = loadAnimation("eating");
        animClimbing = loadAnimation("climbing");
        animClimbingIdle = loadAnimation("climbing_idle");
        animSprintStop = loadAnimation("sprint_stop");
        animFenceIdle = loadAnimation("fence_idle");
        animFenceWalk = loadAnimation("fence_walk");
        animEdgeIdle = loadAnimation("edge_idle");
        animElytraFly = loadAnimation("elytra_fly");
        animFlintAndSteel = loadAnimation("flint_and_steel");
        animFlintAndSteelSneak = loadAnimation("flint_and_steel_sneak");
        animBoatIdle = loadAnimation("boat_idle");
        animBoatForward = loadAnimation("boat_forward");
        animBoatRightPaddle = loadAnimation("boat_right_paddle");
        animBoatLeftPaddle = loadAnimation("boat_left_paddle");
        animRolling = loadAnimation("rolling");

        animationsLoaded = true;
    }

    @Unique
    private static boolean blockSupportsStanding(BlockState state, Level level, BlockPos pos) {
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    @Override
    public void tick() {
        super.tick();
        loadAnimationsIfNeeded();

        float delta = 1.0f / 20.0f;
        Vec3 pos = position();

        if (!onGround() && lastOnGround && getDeltaMovement().y > 0) {
            playJumpAnimation();
        }

        BlockState standingBlockState = level().getBlockState(blockPosition().below());
        var standingBlock = standingBlockState.getBlock();
        boolean onFence = (standingBlock instanceof FenceBlock || standingBlock instanceof WallBlock || standingBlock instanceof IronBarsBlock) && onGround();

        boolean onEdge = !blockSupportsStanding(standingBlockState, level(), blockPosition().below()) && onGround();

        boolean airborneFall = !onGround() && getDeltaMovement().y < 0;

        if (airborneFall) {
            leanAmount = Mth.lerp(delta * 12, leanAmount, 0);
        } else if (turnDelta != 0) {
            leanAmount = Mth.lerp(delta * 4, leanAmount, yBodyRot - yBodyRotO);
        } else {
            leanAmount = Mth.lerp(delta * 4, leanAmount, 0);
        }
        leanMultiplier = Mth.lerp(delta * 8, leanMultiplier, airborneFall ? 1 : realLeanMultiplier);
        squash = Mth.lerp(delta * 10, squash, realSquash);
        realSquash = Mth.lerp(delta * 10, realSquash, 0);

        Vector3f movementVector = new Vector3f((float) (pos.x - lastPos.x), 0, (float) (pos.z - lastPos.z));
        Vector3f lookVector = new Vector3f((float) Math.cos(Math.toRadians(yBodyRot + 90)), 0, (float) Math.sin(Math.toRadians(yBodyRot + 90)));

        boolean isWalking = movementVector.length() > 0;
        boolean isWalkingForwards = isWalking && movementVector.dot(lookVector) > 0;
        float walkSign = isWalking ? (isWalkingForwards ? 1 : -1) : 0;
        float sprintMultiplier = ((isSprinting() && isWalkingForwards) ? 2 : 1);
        momentum = Mth.lerp(delta * 2 * sprintMultiplier, momentum, walkSign * sprintMultiplier);

        if (realLeanMultiplier < 1) {
            realLeanMultiplier += 0.1f;
        } else {
            realLeanMultiplier = 1;
        }

        KeyframeAnimation anim = null;
        float animSpeed = 1.0f;
        int fadeTime = 5;

        boolean onGroundInWater = isUnderWater() && blockSupportsStanding(getBlockStateOn(), level(), blockPosition()) && !isSprinting();

        if (!swinging || swingTime >= getCurrentSwingDuration() / 2 || swingTime < 0) {
            if (isPassenger() && getVehicle() instanceof Boat boat) {
                anim = animBoatIdle;
                boolean leftPaddle = boat.getPaddleState(0);
                boolean rightPaddle = boat.getPaddleState(1);
                if (leftPaddle && rightPaddle) {
                    anim = animBoatForward;
                } else if (leftPaddle) {
                    anim = animBoatLeftPaddle;
                } else if (rightPaddle) {
                    anim = animBoatRightPaddle;
                }
            } else if (level().getBlockState(blockPosition()).getBlock() instanceof LadderBlock && !onGround() && !jumping) {
                anim = animClimbingIdle;
                if (getDeltaMovement().y > 0) {
                    anim = animClimbing;
                }
            } else if (isUsingItem() && getMainHandItem().getFoodProperties(this) != null) {
                anim = animEating;
            } else if (getAbilities().flying) {
                anim = animFalling;
            } else if (isFallFlying()) {
                anim = animElytraFly;
            } else if (onGround() || onGroundInWater) {
                anim = animIdle;
                if (onFence) {
                    anim = animFenceIdle;
                } else if (onEdge) {
                    anim = animEdgeIdle;
                }

                if (turnDelta != 0 && !onEdge) {
                    anim = animTurnRight;
                    if (turnDelta < 0) {
                        anim = animTurnLeft;
                    }
                }

                if ((isInWater() || isInLava()) && !onGroundInWater) {
                    if (isSwimming() || isSprinting()) {
                        anim = animSwimming;
                    }
                } else if (isVisuallyCrawling()) {
                    if (isWalking) {
                        if (currentAnimation == animCrawlIdle) {
                            fadeTime = 0;
                        }
                        anim = animCrawling;
                    } else {
                        if (currentAnimation == animCrawling) {
                            fadeTime = 0;
                        }
                        anim = animCrawlIdle;
                    }
                } else if (isCrouching()) {
                    anim = animSneakIdle;
                    if (isWalking || turnDelta != 0) {
                        anim = animSneakWalk;
                    }
                } else if (isWalking) {
                    if (momentum > 1 && !isWalkingForwards) {
                        anim = animSprintStop;
                        fadeTime = 2;
                    } else if (isSprinting() && !isUsingItem()) {
                        anim = animRun;
                        animSpeed = 1.0f;
                    } else {
                        anim = animWalk;
                        if (onFence) {
                            anim = animFenceWalk;
                        }
                    }
                }
            } else if (isInWater() || isInLava()) {
                if (isSwimming() || isSprinting()) {
                    anim = animSwimming;
                } else {
                    anim = animSwimIdle;
                }
            } else if (fallDistance > 1) {
                anim = fallDistance > 3 ? animFalling : animFall[jumpIndex];
            }
        }

        playAnimation(anim, animSpeed, fadeTime);

        if (isUsingItem()) {
            ItemStack activeItem = getUseItem();
            if (!activeItem.isEmpty()) {
                UseAnim action = activeItem.getUseAnimation();
                if (action == UseAnim.BOW || action == UseAnim.CROSSBOW || action == UseAnim.SPYGLASS
                        || action == UseAnim.SPEAR || action == UseAnim.TOOT_HORN || action == UseAnim.BLOCK
                        || action == UseAnim.DRINK) {
                    disableArmAnimations();
                } else if (activeItem.getItem() instanceof FlintAndSteelItem) {
                    anim = animFlintAndSteel;
                    if (isCrouching()) {
                        anim = animFlintAndSteelSneak;
                    }
                    playAnimation(anim, animSpeed, fadeTime);
                }
            }
        } else {
            enableArmAnimations();
        }

        if (onGround() && !lastOnGround && fallDistance >= 3) {
            playAnimation(animLanding, 1.0f, 0);
        }

        lastPos = pos;
        lastOnGround = onGround();
    }

    @Unique
    public void playAnimation(KeyframeAnimation anim) {
        playAnimation(anim, 1.0f, 10);
    }

    @Unique
    public void playAnimation(KeyframeAnimation anim, float speed, int fade) {
        if (currentAnimation == anim || anim == null) {
            return;
        }

        currentAnimation = anim;
        ModifierLayer<IAnimation> animationContainer = modAnimationContainer;

        var builder = anim.mutableCopy();
        builder.leftArm.setEnabled(armAnimationsEnabled);
        builder.rightArm.setEnabled(armAnimationsEnabled);
        anim = builder.build();

        if (modified) {
            animationContainer.removeModifier(0);
        }
        modified = true;
        animationContainer.addModifierBefore(new SpeedModifier(speed));
        animationContainer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(fade, Ease.LINEAR), new KeyframeAnimationPlayer(anim));
        animationContainer.setupAnim(1.0f / 20.0f);
    }

    @Unique
    public void disableArmAnimations() {
        if (currentAnimation != null && armAnimationsEnabled) {
            armAnimationsEnabled = false;
            ModifierLayer<IAnimation> animationContainer = modAnimationContainer;

            var builder = currentAnimation.mutableCopy();
            builder.leftArm.setEnabled(false);
            builder.rightArm.setEnabled(false);
            currentAnimation = builder.build();

            if (modified) {
                animationContainer.removeModifier(0);
            }
            modified = true;
            animationContainer.addModifierBefore(new SpeedModifier(1.0f));
            animationContainer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(5, Ease.LINEAR), new KeyframeAnimationPlayer(currentAnimation));
            animationContainer.setupAnim(1.0f / 20.0f);
            animationContainer.tick();
        }
    }

    @Unique
    public void enableArmAnimations() {
        if (currentAnimation != null && !armAnimationsEnabled) {
            armAnimationsEnabled = true;
            ModifierLayer<IAnimation> animationContainer = modAnimationContainer;

            var builder = currentAnimation.mutableCopy();
            builder.leftArm.setEnabled(true);
            builder.rightArm.setEnabled(true);
            currentAnimation = builder.build();

            if (modified) {
                animationContainer.removeModifier(0);
            }
            modified = true;
            animationContainer.addModifierBefore(new SpeedModifier(1.0f));
            animationContainer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(5, Ease.LINEAR), new KeyframeAnimationPlayer(currentAnimation));
        }
    }

    @Unique
    public void playJumpAnimation() {
        jumpIndex++;
        jumpIndex %= 2;
        playAnimation(animJump[jumpIndex], 1.0f, 0);
    }

    @Unique
    private int getHandSwingDuration() {
        if (MobEffectUtil.hasDigSpeed(this)) {
            return 6 - (1 + MobEffectUtil.getDigSpeedAmplification(this));
        } else if (hasEffect(MobEffects.DIG_SLOWDOWN)) {
            return 6 + (1 + getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) * 2;
        }
        return 6;
    }

    @Override
    public void swing(net.minecraft.world.InteractionHand hand) {
        super.swing(hand);

        if (getUseItem().getItem() instanceof FlintAndSteelItem && isUsingItem()) {
            return;
        }

        if (!swinging || swingTime >= getHandSwingDuration() / 2 || swingTime < 0) {
            punchIndex++;
            punchIndex %= 2;

            ItemStack stack = getMainHandItem();
            boolean swordAnimations = false;

            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof PickaxeItem
                        || stack.getItem() instanceof AxeItem || stack.getItem() instanceof HoeItem
                        || stack.getItem() instanceof ShovelItem || stack.getItem() instanceof FishingRodItem) {
                    swordAnimations = true;
                }
            }

            if (isCrouching()) {
                if (swordAnimations) {
                    playAnimation(animSwordSwingSneak[punchIndex], 1.0f, 0);
                } else {
                    playAnimation(animPunchSneaking[punchIndex], 1.0f, 0);
                }
            } else if (swordAnimations) {
                playAnimation(animSwordSwing[punchIndex], 1.0f, 0);
            } else {
                playAnimation(animPunch[punchIndex], 1.0f, 0);
            }
        }
    }

    @Override
    public float getLeanAmount() {
        return leanAmount * 0.01f;
    }

    @Override
    public float getLeanMultiplier() {
        return leanMultiplier;
    }

    @Override
    public float getSquash() {
        return squash;
    }
}
