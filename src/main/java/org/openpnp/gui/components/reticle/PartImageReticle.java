/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 *
 * This file is part of OpenPnP.
 *
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui.components.reticle;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;

/**
 * A Reticle that draws a previously captured image semi-transparently over the live camera view,
 * anchored to a machine location and rotated by a given angle.
 * <p>
 * This is used to show what a Part picked from a Feeder will look like once it has been rotated
 * and placed onto the board, by ghosting an image captured at the Feeder's pick location over the
 * live view of the placement location.
 * <p>
 * Unlike the other Reticles this one ignores the rotation passed to
 * {@link #draw(Graphics2D, LengthUnit, double, double, double, double, int, int, double)}, which is
 * the rotation of the currently selected tool. The rotation to draw at is an explicit property.
 */
public class PartImageReticle implements Reticle {
    /**
     * The image to ghost over the camera view. Its center is taken to be the anchor point.
     */
    private volatile BufferedImage image;

    /**
     * The camera units per (source) pixel that were in effect when the image was captured. This is
     * what makes the ghost keep its true physical size: image width times this units per pixel is
     * the physical width of the captured region, which does not change, so dividing it by the live
     * units per pixel gives the correct on screen size at any camera Z and any zoom level.
     */
    private volatile Location capturedUnitsPerPixel;

    /**
     * The machine location the center of the image is pinned to. Null pins it to the center of the
     * camera view instead.
     */
    private volatile Location anchorLocation;

    /**
     * The camera the view belongs to, used to resolve the anchor location against the camera's
     * current position.
     */
    private volatile Camera camera;

    /**
     * Degrees, counter-clockwise, in machine coordinates, to rotate the image by.
     */
    private volatile double rotation;

    private volatile float alpha = 0.55f;

    public PartImageReticle(Camera camera) {
        this.camera = camera;
    }

    @Override
    public void draw(Graphics2D g2d, LengthUnit cameraUnitsPerPixelUnits,
            double cameraUnitsPerPixelX, double cameraUnitsPerPixelY, double viewPortCenterX,
            double viewPortCenterY, int viewPortWidth, int viewPortHeight, double toolRotation) {
        // Take local copies, these can be changed from the dialog at any time.
        BufferedImage image = this.image;
        Location capturedUnitsPerPixel = this.capturedUnitsPerPixel;
        Location anchorLocation = this.anchorLocation;
        Camera camera = this.camera;
        if (image == null || capturedUnitsPerPixel == null || camera == null) {
            return;
        }
        if (cameraUnitsPerPixelX == 0 || cameraUnitsPerPixelY == 0) {
            return;
        }

        // Where the anchor sits on screen. The camera view draws the live image centered on the
        // camera's own location, so the anchor is offset from the view port center by the machine
        // distance between the two, converted to screen pixels. Machine Y is up, screen Y is down,
        // hence the negated Y. This mirrors VisionUtils.getLocationPixelCenterOffsets(), but in
        // screen pixels rather than source pixels.
        double anchorX = viewPortCenterX;
        double anchorY = viewPortCenterY;
        if (anchorLocation != null) {
            Location delta = anchorLocation.convertToUnits(cameraUnitsPerPixelUnits)
                    .subtract(camera.getLocation().convertToUnits(cameraUnitsPerPixelUnits));
            anchorX += delta.getX() / cameraUnitsPerPixelX;
            anchorY -= delta.getY() / cameraUnitsPerPixelY;
        }

        Location capturedUpp = capturedUnitsPerPixel.convertToUnits(cameraUnitsPerPixelUnits);
        double scaleX = capturedUpp.getX() / cameraUnitsPerPixelX;
        double scaleY = capturedUpp.getY() / cameraUnitsPerPixelY;

        Graphics2D g2 = (Graphics2D) g2d.create();
        try {
            // Reticles are drawn without a clip, so keep the ghost inside the live image rather
            // than letting it spill onto the letterboxed background.
            g2.clipRect((int) (viewPortCenterX - viewPortWidth / 2.0),
                    (int) (viewPortCenterY - viewPortHeight / 2.0), viewPortWidth, viewPortHeight);
            g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            AffineTransform tx = new AffineTransform();
            tx.translate(anchorX, anchorY);
            // The captured image and the screen share the same handedness (both have Y down), so
            // no Y flip is needed here, unlike in the Reticles that draw shapes given in machine
            // coordinates. A counter-clockwise machine rotation appears clockwise in a Y down
            // frame, and AffineTransform.rotate() turns clockwise in a Y down frame, so the
            // machine rotation is negated.
            tx.rotate(Math.toRadians(-rotation));
            tx.scale(scaleX, scaleY);
            tx.translate(-image.getWidth() / 2.0, -image.getHeight() / 2.0);
            g2.drawImage(image, tx, null);
        }
        finally {
            g2.dispose();
        }
    }

    public BufferedImage getImage() {
        return image;
    }

    /**
     * Sets the image to ghost, along with the units per pixel that were in effect when it was
     * captured.
     *
     * @param image The image, or null to draw nothing.
     * @param capturedUnitsPerPixel The camera units per pixel at capture time.
     */
    public void setImage(BufferedImage image, Location capturedUnitsPerPixel) {
        this.image = image;
        this.capturedUnitsPerPixel = capturedUnitsPerPixel;
    }

    public Location getAnchorLocation() {
        return anchorLocation;
    }

    public void setAnchorLocation(Location anchorLocation) {
        this.anchorLocation = anchorLocation;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = Math.max(0f, Math.min(1f, alpha));
    }
}
