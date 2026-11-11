package com.example.pawsomeminigames.sakurabrawl;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** One sumo wrestler: holds position and movement, and knows how to draw itself. */
public class Capybara {

    // Position and movement
    public float x, y;
    public float vx, vy;
    public float radius;      // size of the wrestler
    public int facing;        // +1 = looks right, -1 = looks left
    public int beltColor;
    public int species = Species.CAPYBARA;   // which animal this is (looks only, same physics)

    // Tuning values
    private static final float FRICTION = 4.0f;       // higher = stops faster
    private static final float DASH_COOLDOWN = 0.18f; // seconds between dashes
    private static final float PUSH_TIME = 0.20f;
    private static final float HIT_TIME = 0.35f;
    private static final float MAX_SPEED_FACTOR = 12f; // times radius

    private float animTime = 0f;
    private float dashCooldown = 0f;
    private float pushTimer = 0f;
    private float hitTimer = 0f;
    private boolean dashed = false;   // set when a dash happens, read by consumeDash()

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();

    public Capybara(float x, float y, float radius, int facing, int beltColor) {
        this(x, y, radius, facing, beltColor, Species.CAPYBARA);
    }

    public Capybara(float x, float y, float radius, int facing, int beltColor, int species) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.facing = facing;
        this.beltColor = beltColor;
        this.species = Species.safe(species);
    }

    public void setSpecies(int s) {
        species = Species.safe(s);
    }

    /** Put the wrestler back at a spot and stop all movement. */
    public void resetTo(float nx, float ny, int newFacing) {
        x = nx;
        y = ny;
        vx = 0;
        vy = 0;
        facing = newFacing;
        dashCooldown = 0;
        pushTimer = 0;
        hitTimer = 0;
        dashed = false;
    }

    /** True once after each successful dash. Used to play the push sound. */
    public boolean consumeDash() {
        boolean d = dashed;
        dashed = false;
        return d;
    }

    public void update(float dt) {
        animTime += dt;
        if (dashCooldown > 0) dashCooldown -= dt;
        if (pushTimer > 0) pushTimer -= dt;
        if (hitTimer > 0) hitTimer -= dt;

        // Move
        x += vx * dt;
        y += vy * dt;

        // Friction (smooth slow-down, independent of frame rate)
        float f = (float) Math.exp(-FRICTION * dt);
        vx *= f;
        vy *= f;

        // Speed limit
        float max = radius * MAX_SPEED_FACTOR;
        float speed = (float) Math.sqrt(vx * vx + vy * vy);
        if (speed > max) {
            vx = vx / speed * max;
            vy = vy / speed * max;
        }
    }

    /** Normal-strength dash. Returns true if the dash happened. */
    public boolean dash(float dirX, float dirY) {
        return dash(dirX, dirY, 1f);
    }

    /** Dash with a strength multiplier (1 = normal). Returns true if the dash happened. */
    public boolean dash(float dirX, float dirY, float strength) {
        if (dashCooldown > 0) return false;
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len < 0.0001f) return false;
        float power = radius * 5.5f * strength;
        vx += dirX / len * power;
        vy += dirY / len * power;
        dashCooldown = DASH_COOLDOWN;
        pushTimer = PUSH_TIME;
        dashed = true;
        return true;
    }

    /** Get knocked back in a direction (nx, ny must be a unit vector). */
    public void knock(float nx, float ny, float force) {
        vx += nx * force;
        vy += ny * force;
        hitTimer = HIT_TIME;
    }

    // ------------------------------------------------------------- drawing

    /** Mixes two ARGB colors (t = 0 gives a, t = 1 gives b). Alpha is kept opaque. */
    private static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static int darker(int c, float t) {
        return mix(c, 0xFF000000, t);
    }

    private static int lighter(int c, float t) {
        return mix(c, 0xFFFFFFFF, t);
    }

    /** Same color with a different transparency (alpha 0..255). */
    private static int withAlpha(int c, int alpha) {
        return (alpha << 24) | (c & 0x00FFFFFF);
    }

    public void draw(Canvas canvas) {
        float s = radius;
        float bob = (float) Math.sin(animTime * 5f) * 0.03f;
        float push = Math.max(0f, pushTimer / PUSH_TIME);
        float hit = Math.max(0f, hitTimer / HIT_TIME);
        float wobble = (float) Math.sin(animTime * 40f) * 6f * hit;

        int body = Species.BODY[species];
        int head = Species.HEAD[species];
        int dark = Species.DARK[species];
        int belly = Species.BELLY[species];
        int outline = darker(body, 0.55f);

        // Shadow on the sand
        oval(canvas, x, y + 0.78f * s, 1.15f * s, 0.25f * s, 0x33000000);

        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(wobble);
        canvas.scale(facing * (1f + 0.12f * push), (1f + bob) * (1f - 0.08f * push));

        drawTail(canvas, s, body, head, outline);
        drawLegs(canvas, s, dark, outline);
        drawBody(canvas, s, body, belly, dark, outline);

        drawBandana(canvas, s);

        drawEars(canvas, s, head, dark, outline);

        // Head: outline, shaded edge, main color, shine on top
        float hx = 0.88f, hy = -0.14f;
        oval(canvas, hx * s, (hy + 0.02f) * s, 0.69f * s, 0.59f * s, outline);
        oval(canvas, hx * s, hy * s, 0.66f * s, 0.56f * s, darker(head, 0.15f));
        oval(canvas, (hx - 0.02f) * s, (hy - 0.04f) * s, 0.63f * s, 0.52f * s, head);
        oval(canvas, (hx - 0.06f) * s, (hy - 0.30f) * s, 0.40f * s, 0.14f * s, 0x45FFFFFF);

        drawFace(canvas, s, head, belly, hit);

        canvas.restore();
    }

    /** Tails wag a little. The capybara only has a tiny stub. */
    private void drawTail(Canvas canvas, float s, int body, int head, int outline) {
        float wag = (float) Math.sin(animTime * 6f) * 0.05f;
        if (species == Species.RACCOON) {
            int ring = 0xFF3A3A44;
            // Outline first, then the striped tail (tip to body)
            ovalU(canvas, s, -1.62f, -0.62f + wag, 0.23f, 0.23f, outline);
            ovalU(canvas, s, -1.45f, -0.46f + wag, 0.28f, 0.28f, outline);
            ovalU(canvas, s, -1.27f, -0.30f + wag, 0.30f, 0.30f, outline);
            ovalU(canvas, s, -1.08f, -0.14f + wag, 0.30f, 0.30f, outline);
            ovalU(canvas, s, -1.62f, -0.62f + wag, 0.20f, 0.20f, ring);
            ovalU(canvas, s, -1.45f, -0.46f + wag, 0.25f, 0.25f, head);
            ovalU(canvas, s, -1.27f, -0.30f + wag, 0.27f, 0.27f, ring);
            ovalU(canvas, s, -1.08f, -0.14f + wag, 0.27f, 0.27f, head);
        } else if (species == Species.FOX) {
            ovalU(canvas, s, -1.78f, -0.72f + wag, 0.265f, 0.265f, outline);
            ovalU(canvas, s, -1.58f, -0.52f + wag, 0.335f, 0.335f, outline);
            ovalU(canvas, s, -1.33f, -0.30f + wag, 0.365f, 0.365f, outline);
            ovalU(canvas, s, -1.08f, -0.10f + wag, 0.335f, 0.335f, outline);
            ovalU(canvas, s, -1.78f, -0.72f + wag, 0.23f, 0.23f, 0xFFFFF4E6);
            ovalU(canvas, s, -1.58f, -0.52f + wag, 0.30f, 0.30f, body);
            ovalU(canvas, s, -1.33f, -0.30f + wag, 0.33f, 0.33f, body);
            ovalU(canvas, s, -1.08f, -0.10f + wag, 0.30f, 0.30f, body);
            ovalU(canvas, s, -1.30f, -0.42f + wag, 0.20f, 0.10f, 0x44FFFFFF);   // fluffy shine
        } else if (species == Species.CAPYBARA) {
            ovalU(canvas, s, -1.02f, 0.05f, 0.10f, 0.09f, Species.DARK[species]);
        }
    }

    private void drawLegs(Canvas canvas, float s, int dark, int outline) {
        for (int leg = 0; leg < 2; leg++) {
            float lx = leg == 0 ? -0.6f : 0.5f;
            ovalU(canvas, s, lx, 0.58f, 0.30f, 0.32f, outline);
            ovalU(canvas, s, lx, 0.57f, 0.27f, 0.29f, dark);
            if (species == Species.SLOTH) {
                // Long pale claws
                for (int c = -1; c <= 1; c++) {
                    ovalU(canvas, s, lx + c * 0.12f, 0.88f, 0.035f, 0.09f, 0xFFF3EAD2);
                }
            } else {
                // Little toes
                int toe = darker(dark, 0.25f);
                for (int c = -1; c <= 1; c++) {
                    ovalU(canvas, s, lx + 0.07f + c * 0.10f, 0.80f, 0.045f, 0.05f, toe);
                }
            }
        }
    }

    private void drawBody(Canvas canvas, float s, int body, int belly, int dark, int outline) {
        ovalU(canvas, s, 0f, 0.02f, 1.04f, 0.76f, outline);
        ovalU(canvas, s, 0f, 0f, 1.0f, 0.72f, darker(body, 0.18f));          // shaded edge
        ovalU(canvas, s, -0.02f, -0.04f, 0.96f, 0.66f, body);
        ovalU(canvas, s, 0f, 0.30f, 0.78f, 0.36f,
                species == Species.CAPYBARA ? mix(body, belly, 0.7f) : belly);  // lighter belly
        ovalU(canvas, s, -0.15f, -0.40f, 0.62f, 0.15f, 0x40FFFFFF);          // shine on the back
        if (species == Species.SLOTH) {
            // A bit of green moss on its back
            ovalU(canvas, s, -0.35f, -0.42f, 0.30f, 0.12f, 0x778FA66A);
            ovalU(canvas, s, 0.05f, -0.52f, 0.18f, 0.08f, 0x778FA66A);
        }
        // A few short strokes of fur
        int fur = withAlpha(darker(body, 0.25f), 0x55);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.03f * s);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(fur);
        float[][] strokes = {{-0.7f, -0.3f}, {-0.5f, -0.5f}, {0.2f, -0.5f},
                {-0.8f, 0.1f}, {-0.55f, 0.25f}, {0.5f, -0.3f}};
        for (float[] p : strokes) {
            rect.set((p[0] - 0.1f) * s, (p[1] - 0.05f) * s, (p[0] + 0.1f) * s, (p[1] + 0.1f) * s);
            canvas.drawArc(rect, 200f, 120f, false, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawEars(Canvas canvas, float s, int head, int dark, int outline) {
        if (species == Species.FOX) {
            // Tall pointed ears: outline, orange, dark inside, dark tips
            triangle(canvas, s, 0.50f, -0.40f, 0.97f, -0.50f, 0.66f, -1.12f, outline);
            triangle(canvas, s, 0.86f, -0.50f, 1.30f, -0.38f, 1.14f, -1.08f, outline);
            triangle(canvas, s, 0.54f, -0.42f, 0.94f, -0.50f, 0.67f, -1.06f, head);
            triangle(canvas, s, 0.88f, -0.50f, 1.27f, -0.40f, 1.13f, -1.02f, head);
            triangle(canvas, s, 0.64f, -0.50f, 0.86f, -0.53f, 0.69f, -0.90f, 0xFF3A1E0C);
            triangle(canvas, s, 0.98f, -0.53f, 1.18f, -0.48f, 1.11f, -0.87f, 0xFF3A1E0C);
            triangle(canvas, s, 0.67f, -0.96f, 0.74f, -0.96f, 0.675f, -1.06f, 0xFF3A1E0C);
            triangle(canvas, s, 1.09f, -0.95f, 1.15f, -0.93f, 1.13f, -1.03f, 0xFF3A1E0C);
        } else if (species == Species.RACCOON) {
            ovalU(canvas, s, 0.64f, -0.64f, 0.20f, 0.21f, outline);
            ovalU(canvas, s, 1.06f, -0.62f, 0.20f, 0.21f, outline);
            ovalU(canvas, s, 0.64f, -0.64f, 0.17f, 0.18f, head);
            ovalU(canvas, s, 1.06f, -0.62f, 0.17f, 0.18f, head);
            ovalU(canvas, s, 0.64f, -0.63f, 0.10f, 0.11f, 0xFF3A3A44);
            ovalU(canvas, s, 1.06f, -0.61f, 0.10f, 0.11f, 0xFF3A3A44);
        } else if (species == Species.SLOTH) {
            ovalU(canvas, s, 0.62f, -0.62f, 0.15f, 0.16f, outline);
            ovalU(canvas, s, 1.08f, -0.60f, 0.15f, 0.16f, outline);
            ovalU(canvas, s, 0.62f, -0.62f, 0.12f, 0.13f, dark);
            ovalU(canvas, s, 1.08f, -0.60f, 0.12f, 0.13f, dark);
        } else {
            // Capybara: small round ears with pink inside
            ovalU(canvas, s, 0.62f, -0.64f, 0.17f, 0.18f, outline);
            ovalU(canvas, s, 1.06f, -0.62f, 0.17f, 0.18f, outline);
            ovalU(canvas, s, 0.62f, -0.64f, 0.14f, 0.15f, dark);
            ovalU(canvas, s, 1.06f, -0.62f, 0.14f, 0.15f, dark);
            ovalU(canvas, s, 0.63f, -0.63f, 0.08f, 0.09f, 0xFFE8A8A0);
            ovalU(canvas, s, 1.07f, -0.61f, 0.08f, 0.09f, 0xFFE8A8A0);
        }
    }

    /** Patches, eye, blush, nose and mouth. hit > 0 while the wrestler was just knocked. */
    private void drawFace(Canvas canvas, float s, int head, int belly, float hit) {
        float ex = 1.04f, ey = -0.28f;

        switch (species) {
            case Species.RACCOON:
                ovalU(canvas, s, 0.60f, 0.16f, 0.20f, 0.17f, lighter(head, 0.25f));   // fluffy cheeks
                ovalU(canvas, s, 1.28f, 0.06f, 0.32f, 0.27f, 0xFFF2F2F2);             // white muzzle
                ovalU(canvas, s, 1.00f, -0.27f, 0.34f, 0.18f, 0xFF2E2E36);            // eye mask
                ovalU(canvas, s, 0.70f, -0.21f, 0.12f, 0.09f, 0xFF2E2E36);
                ovalU(canvas, s, 1.04f, -0.50f, 0.12f, 0.045f, 0xFFF2F2F2);           // pale brow
                break;
            case Species.FOX:
                ovalU(canvas, s, 1.04f, 0.14f, 0.50f, 0.30f, 0xFFFFF4E6);             // white lower face
                ovalU(canvas, s, 1.38f, 0.02f, 0.26f, 0.19f, 0xFFFFF4E6);             // snout
                ovalU(canvas, s, 0.58f, 0.14f, 0.17f, 0.14f, 0xFFFFF4E6);             // cheek tuft
                break;
            case Species.SLOTH:
                ovalU(canvas, s, 0.98f, -0.10f, 0.52f, 0.43f, 0xFFEADFC4);            // pale face
                ovalU(canvas, s, 1.02f, -0.26f, 0.24f, 0.13f, 0xFF6A5538);            // eye stripe
                break;
            default:
                ovalU(canvas, s, 1.27f, 0.04f, 0.38f, 0.31f, mix(head, belly, 0.55f)); // boxy muzzle
                ovalU(canvas, s, 1.22f, -0.06f, 0.20f, 0.10f, 0x35FFFFFF);
                break;
        }

        // Eye: big and glossy. It closes for a blink, and squints when hit.
        boolean blink = ((animTime + species * 0.9f) % 3.4f) < 0.12f;
        int eyeColor;
        switch (species) {
            case Species.RACCOON: eyeColor = 0xFF111114; break;
            case Species.FOX: eyeColor = 0xFF2A140A; break;
            case Species.SLOTH: eyeColor = 0xFF140C06; break;
            default: eyeColor = 0xFF2A180A; break;
        }
        if (hit > 0.05f) {
            strokeArc(canvas, s, ex - 0.11f, ey - 0.09f, ex + 0.11f, ey + 0.09f, 200f, 140f, 0.055f, 0xFF2A180A);
        } else if (blink) {
            strokeArc(canvas, s, ex - 0.11f, ey - 0.07f, ex + 0.11f, ey + 0.09f, 20f, 140f, 0.05f, 0xFF2A180A);
        } else {
            if (species == Species.RACCOON) {
                ovalU(canvas, s, ex, ey, 0.125f, 0.14f, 0xFFF2F2F2);                    // white ring around the eye
            }
            ovalU(canvas, s, ex, ey, 0.105f, 0.125f, eyeColor);
            ovalU(canvas, s, ex + 0.035f, ey - 0.045f, 0.045f, 0.05f, 0xFFFFFFFF);      // big shine
            ovalU(canvas, s, ex - 0.04f, ey + 0.04f, 0.022f, 0.022f, 0xCCFFFFFF);       // small shine
        }

        // Cheek blush
        ovalU(canvas, s, 0.96f, 0.02f, 0.14f, 0.08f, 0x88FF8FA3);

        // Nose and mouth
        int nose = Species.NOSE[species];
        switch (species) {
            case Species.RACCOON:
                ovalU(canvas, s, 1.53f, -0.03f, 0.10f, 0.08f, nose);
                ovalU(canvas, s, 1.50f, -0.06f, 0.035f, 0.02f, 0xAAFFFFFF);
                strokeArc(canvas, s, 1.30f, 0.02f, 1.52f, 0.22f, 20f, 140f, 0.035f, 0xFF3A3A44);
                break;
            case Species.FOX:
                ovalU(canvas, s, 1.60f, -0.03f, 0.085f, 0.07f, nose);
                ovalU(canvas, s, 1.58f, -0.055f, 0.03f, 0.018f, 0xAAFFFFFF);
                strokeArc(canvas, s, 1.34f, 0.00f, 1.58f, 0.20f, 20f, 140f, 0.03f, 0xFF6A2E0C);
                break;
            case Species.SLOTH:
                ovalU(canvas, s, 1.40f, -0.07f, 0.11f, 0.085f, nose);
                ovalU(canvas, s, 1.37f, -0.10f, 0.035f, 0.022f, 0xAAFFFFFF);
                strokeArc(canvas, s, 1.00f, -0.02f, 1.38f, 0.24f, 20f, 140f, 0.045f, 0xFF4A2E18);
                break;
            default:
                // Capybara: two buck teeth under the nose
                roundRect(canvas, 1.31f, 0.00f, 1.46f, 0.20f, 0.04f, s, 0xFF5A3A1E);
                roundRect(canvas, 1.32f, 0.01f, 1.385f, 0.19f, 0.025f, s, 0xFFFFFFFF);
                roundRect(canvas, 1.395f, 0.01f, 1.455f, 0.19f, 0.025f, s, 0xFFFFFFFF);
                ovalU(canvas, s, 1.52f, -0.10f, 0.13f, 0.10f, nose);
                ovalU(canvas, s, 1.49f, -0.13f, 0.045f, 0.028f, 0xAAFFFFFF);
                break;
        }
    }

    // ---- small drawing helpers ----

    /** Oval by center and radii, all in units of s (the wrestler's size). */
    private void ovalU(Canvas canvas, float s, float cx, float cy, float rx, float ry, int color) {
        oval(canvas, cx * s, cy * s, rx * s, ry * s, color);
    }

    /** The colored bandana: a curved band with dots, a knot and two fluttering tails. */
    private void drawBandana(Canvas canvas, float s) {
        int base = beltColor;
        int edge = darker(base, 0.35f);
        int light = lighter(base, 0.35f);
        float flutter = (float) Math.sin(animTime * 6f);

        float kx = -0.12f, ky = -0.66f;   // where the knot sits

        // Tails (drawn first so the band covers their start)
        triangle(canvas, s, kx, ky,
                kx - 0.46f, ky - 0.14f + flutter * 0.06f,
                kx - 0.30f, ky + 0.08f + flutter * 0.04f, edge);
        triangle(canvas, s, kx, ky,
                kx - 0.40f, ky + 0.12f - flutter * 0.05f,
                kx - 0.16f, ky + 0.20f, base);

        // Band: dark outline, then the main color slightly smaller
        for (int pass = 0; pass < 2; pass++) {
            float i = pass == 0 ? 0f : 0.03f;
            path.reset();
            path.moveTo((-0.32f + i) * s, (-0.66f + i) * s);
            path.quadTo((-0.42f + i) * s, 0f, (-0.32f + i) * s, (0.66f - i) * s);
            path.lineTo((0.10f - i) * s, (0.66f - i) * s);
            path.quadTo((0.00f - i) * s, 0f, (0.10f - i) * s, (-0.66f + i) * s);
            path.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(pass == 0 ? edge : base);
            canvas.drawPath(path, paint);
        }

        // Soft shine strip and polka dots
        roundRect(canvas, -0.27f, -0.58f, -0.20f, 0.50f, 0.04f, s, withAlpha(light, 0x66));
        float[] dotY = {-0.45f, -0.15f, 0.15f, 0.45f};
        for (float dy : dotY) {
            float tt = dy / 0.66f;
            float cx = -0.11f - 0.05f * (1f - tt * tt);   // follows the curve of the band
            oval(canvas, cx * s, dy * s, 0.045f * s, 0.045f * s, 0xDDFFFFFF);
        }

        // Knot
        oval(canvas, kx * s, (ky + 0.02f) * s, 0.12f * s, 0.10f * s, edge);
        oval(canvas, kx * s, (ky + 0.01f) * s, 0.09f * s, 0.075f * s, base);
    }

    /** Rounded rectangle, coordinates in units of s. */
    private void roundRect(Canvas canvas, float l, float t, float r, float b, float rad, float s, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        rect.set(l * s, t * s, r * s, b * s);
        canvas.drawRoundRect(rect, rad * s, rad * s, paint);
    }

    /** A curved line (part of an oval), coordinates in units of s. */
    private void strokeArc(Canvas canvas, float s, float l, float t, float r, float b,
                           float start, float sweep, float width, int color) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width * s);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(color);
        rect.set(l * s, t * s, r * s, b * s);
        canvas.drawArc(rect, start, sweep, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    /** Helper: a filled triangle. Coordinates are in units of s (the wrestler's size). */
    private void triangle(Canvas canvas, float s, float x1, float y1, float x2, float y2,
                          float x3, float y3, int color) {
        path.reset();
        path.moveTo(x1 * s, y1 * s);
        path.lineTo(x2 * s, y2 * s);
        path.lineTo(x3 * s, y3 * s);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawPath(path, paint);
    }

    /** Helper: draws a filled oval by center and radii. */
    private void oval(Canvas canvas, float cx, float cy, float rx, float ry, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        rect.set(cx - rx, cy - ry, cx + rx, cy + ry);
        canvas.drawOval(rect, paint);
    }
}