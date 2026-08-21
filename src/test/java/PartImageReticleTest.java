import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;
import org.openpnp.gui.components.reticle.PartImageReticle;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;

/**
 * Pins down the rotation sign convention of {@link PartImageReticle}, which is the one piece of
 * the Part Rotation Preview that is easy to get backwards and hard to notice: a mirrored or
 * counter-rotating ghost still looks plausible.
 * <p>
 * OpenPnP machine coordinates are +X right, +Y up, with rotation counter-clockwise positive.
 * Camera images and Swing components are +X right, +Y down. So a machine rotation of +90 degrees,
 * which carries +X onto +Y, must move a mark from the right of the ghost to above it on screen.
 */
public class PartImageReticleTest {
    private static final int CANVAS = 101;
    private static final int CENTER = CANVAS / 2;

    /**
     * A 9x9 ghost, transparent except for an opaque white block on its right hand edge.
     */
    private static BufferedImage markOnTheRight() {
        BufferedImage image = new BufferedImage(9, 9, BufferedImage.TYPE_INT_ARGB);
        for (int y = 3; y <= 5; y++) {
            for (int x = 6; x <= 8; x++) {
                image.setRGB(x, y, 0xffffffff);
            }
        }
        return image;
    }

    /**
     * Renders the reticle at the given rotation and returns the centroid of what it drew, in
     * canvas pixels.
     */
    private static double[] renderCentroid(double rotationDegrees) {
        PartImageReticle reticle = new PartImageReticle(mock(Camera.class));
        reticle.setImage(markOnTheRight(), new Location(LengthUnit.Millimeters, 1, 1, 0, 0));
        reticle.setRotation(rotationDegrees);
        reticle.setAlpha(1f);
        // Leave the anchor null so the ghost pins to the view port center and the mocked camera's
        // location is never consulted.
        reticle.setAnchorLocation(null);

        BufferedImage canvas = new BufferedImage(CANVAS, CANVAS, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = canvas.createGraphics();
        // One machine mm per screen pixel, matching the captured units per pixel, so the ghost is
        // drawn at 1:1 and the arithmetic stays easy to reason about.
        reticle.draw(g2d, LengthUnit.Millimeters, 1, 1, CENTER, CENTER, CANVAS, CANVAS, 0);
        g2d.dispose();

        double sumX = 0;
        double sumY = 0;
        double weight = 0;
        for (int y = 0; y < CANVAS; y++) {
            for (int x = 0; x < CANVAS; x++) {
                int alpha = (canvas.getRGB(x, y) >>> 24) & 0xff;
                if (alpha > 0) {
                    sumX += x * alpha;
                    sumY += y * alpha;
                    weight += alpha;
                }
            }
        }
        assertTrue(weight > 0, "the reticle drew nothing at rotation " + rotationDegrees);
        return new double[] {sumX / weight, sumY / weight};
    }

    @Test
    public void testUnrotatedGhostKeepsTheMarkOnTheRight() {
        double[] centroid = renderCentroid(0);
        assertTrue(centroid[0] > CENTER + 1,
                "at 0 degrees the mark should stay right of center, was x=" + centroid[0]);
        assertTrue(Math.abs(centroid[1] - CENTER) < 1.5,
                "at 0 degrees the mark should stay vertically centered, was y=" + centroid[1]);
    }

    @Test
    public void testPositiveRotationIsCounterClockwiseInMachineTerms() {
        // Machine +90 carries +X onto +Y, and machine +Y is up, which is a smaller screen y.
        double[] centroid = renderCentroid(90);
        assertTrue(centroid[1] < CENTER - 1,
                "at +90 degrees the mark should move above center, was y=" + centroid[1]);
        assertTrue(Math.abs(centroid[0] - CENTER) < 1.5,
                "at +90 degrees the mark should be horizontally centered, was x=" + centroid[0]);
    }

    @Test
    public void testNegativeRotationIsClockwiseInMachineTerms() {
        double[] centroid = renderCentroid(-90);
        assertTrue(centroid[1] > CENTER + 1,
                "at -90 degrees the mark should move below center, was y=" + centroid[1]);
        assertTrue(Math.abs(centroid[0] - CENTER) < 1.5,
                "at -90 degrees the mark should be horizontally centered, was x=" + centroid[0]);
    }

    @Test
    public void testOneHundredEightyDegreesMirrorsThroughTheCenter() {
        double[] centroid = renderCentroid(180);
        assertTrue(centroid[0] < CENTER - 1,
                "at 180 degrees the mark should move left of center, was x=" + centroid[0]);
    }

    /**
     * The ghost keeps its true physical size: it is captured in units per pixel at the pick plane
     * and drawn in units per pixel at the current camera Z, so halving the live units per pixel
     * (zooming in, or a lower camera) must double its size on screen.
     */
    @Test
    public void testGhostScalesWithTheLiveUnitsPerPixel() {
        PartImageReticle reticle = new PartImageReticle(mock(Camera.class));
        BufferedImage ghost = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                ghost.setRGB(x, y, 0xffffffff);
            }
        }
        reticle.setImage(ghost, new Location(LengthUnit.Millimeters, 1, 1, 0, 0));
        reticle.setAlpha(1f);

        assertTrue(opaqueArea(reticle, 1) > 0, "the ghost should be drawn at 1:1");
        double atOneToOne = opaqueArea(reticle, 1);
        double atTwoToOne = opaqueArea(reticle, 0.5);
        assertTrue(atTwoToOne > atOneToOne * 3,
                "halving the live units per pixel should roughly quadruple the drawn area, was "
                        + atOneToOne + " then " + atTwoToOne);
    }

    private static double opaqueArea(PartImageReticle reticle, double liveUnitsPerPixel) {
        BufferedImage canvas = new BufferedImage(CANVAS, CANVAS, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = canvas.createGraphics();
        reticle.draw(g2d, LengthUnit.Millimeters, liveUnitsPerPixel, liveUnitsPerPixel, CENTER,
                CENTER, CANVAS, CANVAS, 0);
        g2d.dispose();
        int count = 0;
        for (int y = 0; y < CANVAS; y++) {
            for (int x = 0; x < CANVAS; x++) {
                if (((canvas.getRGB(x, y) >>> 24) & 0xff) > 128) {
                    count++;
                }
            }
        }
        return count;
    }
}
