package com.daldon.winterphenomena;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.level.block.Blocks;

/**
 * Tiny block-textured ice pellets with physical ground bounce.
 * Client only. We use Minecraft's ice sprite; no copied third-party assets.
 */
public final class HailStoneParticle extends TextureSheetParticle {
    private int bounces;

    public HailStoneParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 38;
        this.quadSize = 0.065f;
        this.gravity = 0.65f;
        this.hasPhysics = true;
        this.xd = (level.random.nextDouble() - 0.5) * 0.11;
        this.yd = -0.35 - level.random.nextDouble() * 0.14;
        this.zd = (level.random.nextDouble() - 0.5) * 0.11;
        this.setSprite(Minecraft.getInstance().getBlockRenderer()
            .getBlockModelShaper().getParticleIcon(Blocks.ICE.defaultBlockState()));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
            return;
        }

        this.yd -= 0.046;
        this.move(this.xd, this.yd, this.zd);
        if (this.onGround) {
            if (bounces >= 2) {
                remove();
                return;
            }
            bounces++;
            this.yd = bounces == 1 ? 0.24 : 0.09;
            this.xd *= 0.58;
            this.zd *= 0.58;
            this.onGround = false;
        } else {
            this.xd *= 0.985;
            this.zd *= 0.985;
        }
    }
}
