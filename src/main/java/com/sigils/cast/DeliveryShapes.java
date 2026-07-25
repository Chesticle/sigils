package com.sigils.cast;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import com.sigils.core.spell.Delivery;

/** Resolves a delivery shape to the set of world points effects are dispatched at. */
public final class DeliveryShapes {

    private static final int BEAM_STEPS = 12;

    private DeliveryShapes() {}

    public static List<Vec3> points(CastContext ctx, Vec3 target, Delivery delivery) {
        return switch (delivery.shapeId()) {
            case "sigils:beam" -> beam(ctx.origin(), target);
            default -> List.of(target); // sigils:burst and anything unrecognised
        };
    }

    private static List<Vec3> beam(Vec3 from, Vec3 to) {
        List<Vec3> pts = new ArrayList<>();
        for (int i = 1; i <= BEAM_STEPS; i++) {
            pts.add(from.lerp(to, (double) i / BEAM_STEPS));
        }
        return pts;
    }
}