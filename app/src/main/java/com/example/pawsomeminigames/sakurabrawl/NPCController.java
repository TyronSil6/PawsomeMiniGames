package com.example.pawsomeminigames.sakurabrawl;

import android.graphics.RectF;

import java.util.Random;

/** The AI brain that controls the second wrestler in 1-player mode. */
public class NPCController {

    private final Capybara npc;
    private final Random random = new Random();

    private float thinkInterval = 0.40f;  // seconds between decisions
    private float aggression = 0.80f;     // 0..1 chance to attack
    private float powerScale = 1.10f;     // dash strength multiplier
    private float jitter = 0.14f;         // aiming error in radians
    private float lead = 0.15f;           // seconds ahead it aims (predicts where you are going)
    private float flankChance = 0.55f;    // chance to circle around you to get on the center side
    private float startDelay = 0.6f;      // pause at the start of a round
    private float thinkTimer = 0.6f;

    public NPCController(Capybara npc) {
        this.npc = npc;
    }

    /** Sets how tough the NPC is. round starts at 1 and makes it harder each round. */
    public void setLevel(int difficulty, int round) {
        switch (difficulty) {
            case SettingsManager.EASY:
                thinkInterval = 0.70f;
                aggression = 0.55f;
                powerScale = 0.85f;
                jitter = 0.35f;
                lead = 0f;
                flankChance = 0.20f;
                startDelay = 1.0f;
                break;
            case SettingsManager.HARD:
                thinkInterval = 0.24f;
                aggression = 0.92f;
                powerScale = 1.30f;
                jitter = 0.06f;
                lead = 0.30f;
                flankChance = 0.85f;
                startDelay = 0.35f;
                break;
            default: // NORMAL
                thinkInterval = 0.40f;
                aggression = 0.80f;
                powerScale = 1.10f;
                jitter = 0.14f;
                lead = 0.15f;
                flankChance = 0.55f;
                startDelay = 0.6f;
                break;
        }

        int extra = Math.max(0, round - 1);
        thinkInterval = Math.max(0.20f, thinkInterval * (float) Math.pow(0.94, extra));
        aggression = Math.min(0.97f, aggression + 0.02f * extra);
        powerScale = Math.min(1.45f, powerScale + 0.03f * extra);
        jitter = Math.max(0.03f, jitter - 0.015f * extra);
        lead = Math.min(0.40f, lead + 0.02f * extra);
        flankChance = Math.min(0.95f, flankChance + 0.05f * extra);
        thinkTimer = startDelay;
    }

    /** Call when a round restarts: gives the player a short moment before the NPC acts. */
    public void reset() {
        thinkTimer = startDelay;
    }

    public void update(float dt, Capybara target, RectF ring) {
        thinkTimer -= dt;
        if (thinkTimer > 0) return;
        thinkTimer = thinkInterval * (0.85f + random.nextFloat() * 0.3f);

        float cx = ring.centerX();
        float cy = ring.centerY();
        float r = npc.radius;
        float myEdge = edgeAt(npc.x, npc.y, ring);
        float targetEdge = edgeAt(target.x, target.y, ring);

        float toX = target.x - npc.x;
        float toY = target.y - npc.y;
        float dist = (float) Math.sqrt(toX * toX + toY * toY);

        // 1. Defend: too close to the edge (now, or where it will slide to), go back to the center
        float myFuture = edgeAt(npc.x + npc.vx * 0.25f, npc.y + npc.vy * 0.25f, ring);
        if (Math.max(myEdge, myFuture) > 0.70f && random.nextFloat() < 0.90f) {
            dashToward(cx - npc.x, cy - npc.y, 0.85f, jitter * 0.5f);
            return;
        }

        // 2. Flank: if the NPC is farther out than the player, pushing would only shove the
        //    player toward the center. Circle around to the center side first.
        float tcx = target.x - cx;
        float tcy = target.y - cy;
        float tLen = (float) Math.sqrt(tcx * tcx + tcy * tcy);
        if (tLen > 0.0001f && targetEdge > 0.30f && dist < r * 4.5f && myEdge < 0.60f) {
            float ox = tcx / tLen;   // unit vector from the center toward the player
            float oy = tcy / tLen;
            float outside = (npc.x - target.x) * ox + (npc.y - target.y) * oy;
            if (outside > -r * 0.3f && random.nextFloat() < flankChance) {
                float side = (npc.x - target.x) * (-oy) + (npc.y - target.y) * ox;
                float sign = side >= 0f ? 1f : -1f;
                if (Math.abs(side) < r * 0.3f) sign = random.nextBoolean() ? 1f : -1f;
                // Sideways around the player, with a pull toward the center
                dashToward(-oy * sign - ox * 0.7f, ox * sign - oy * 0.7f, 0.9f, jitter);
                return;
            }
        }

        // 3. Attack: more likely when the player is near the edge. Aim where the player is going.
        float chance = aggression;
        if (targetEdge > 0.60f) chance += 0.15f;
        if (random.nextFloat() < chance) {
            float aimX = target.x + target.vx * lead;
            float aimY = target.y + target.vy * lead;
            dashToward(aimX - npc.x, aimY - npc.y, 1.0f, jitter);
            // Up close, follow up quickly with another hit
            if (dist < r * 3f) thinkTimer = Math.max(0.22f, thinkInterval * 0.55f);
            return;
        }

        // 4. Wander: random direction, leaning toward the center
        float angle = random.nextFloat() * (float) (Math.PI * 2);
        float wx = (float) Math.cos(angle);
        float wy = (float) Math.sin(angle);
        float tx = cx - npc.x;
        float ty = cy - npc.y;
        float len = (float) Math.sqrt(tx * tx + ty * ty);
        if (len > 0.0001f) {
            wx += tx / len * 0.8f;
            wy += ty / len * 0.8f;
        }
        dashToward(wx, wy, 0.5f, 0f);
    }

    /** 0 = center of the ring, 1 = exactly on the edge, above 1 = outside. */
    private float edgeAt(float px, float py, RectF ring) {
        float nx = (px - ring.centerX()) / (ring.width() / 2f);
        float ny = (py - ring.centerY()) / (ring.height() / 2f);
        return (float) Math.sqrt(nx * nx + ny * ny);
    }

    /** Dashes in a direction, with a random aiming error. */
    private void dashToward(float dx, float dy, float scale, float jitterRad) {
        float a = (random.nextFloat() * 2f - 1f) * jitterRad;
        float cos = (float) Math.cos(a);
        float sin = (float) Math.sin(a);
        float rx = dx * cos - dy * sin;
        float ry = dx * sin + dy * cos;
        npc.dash(rx, ry, powerScale * scale);
    }
}