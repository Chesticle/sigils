package com.sigils.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

import com.sigils.core.particle.ParticleProfile;
import com.sigils.particle.SigilParticleOptions;

/** One particle for the whole mod; everything about it comes from the profile. */
public class SigilParticle extends SingleQuadParticle {

    private final boolean emissiveGlow;

    protected SigilParticle(ClientLevel level, double x, double y, double z,
                            double vx, double vy, double vz,
                            ParticleProfile profile, TextureAtlasSprite sprite, RandomSource random) {
        super(level, x, y, z, vx, vy, vz, sprite);

        // Profile colour is LINEAR; the particle wants display (sRGB) colour.
        this.rCol = linearToDisplay(profile.red());
        this.gCol = linearToDisplay(profile.green());
        this.bCol = linearToDisplay(profile.blue());

        float sizeMul = 1f + (random.nextFloat() * 2f - 1f) * profile.sizeJitter();
        this.quadSize = Math.max(0.02f, profile.size() * sizeMul);

        float lifeJit = (random.nextFloat() * 2f - 1f) * profile.lifetimeJitter();
        this.lifetime = Math.max(1, Math.round(profile.lifetime() + lifeJit));

        this.gravity = profile.gravity() * 4f; // signed: <0 rises, >0 sinks
        this.friction = 0.96f;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;

        this.emissiveGlow = profile.emissive() >= 0.5f;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        // Soft, alpha-blended blob. If TRANSLUCENT isn't a constant, Ctrl+Click
        // SingleQuadParticle.Layer and pick the translucent one; OPAQUE (like
        // FlameParticle) is a safe fallback.
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return emissiveGlow ? 0xF000F0 : super.getLightCoords(partialTick); // 0xF000F0 = full-bright
    }

    private static float linearToDisplay(float c) {
        float s = c <= 0.0031308f ? c * 12.92f : 1.055f * (float) Math.pow(c, 1f / 2.4f) - 0.055f;
        return Math.clamp(s, 0f, 1f);
    }

    public static class Provider implements ParticleProvider<SigilParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SigilParticleOptions options, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz,
                                       RandomSource random) {
            return new SigilParticle(level, x, y, z, vx, vy, vz,
                    options.profile(), this.sprites.get(random), random);
        }
    }
}