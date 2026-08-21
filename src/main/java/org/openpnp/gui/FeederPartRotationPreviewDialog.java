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

package org.openpnp.gui;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.openpnp.Translations;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.reticle.PartImageReticle;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Footprint;
import org.openpnp.model.Job;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Point;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.ImageUtils;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

/**
 * A modeless dialog that gives visual feedback for a Feeder's part rotation.
 * <p>
 * It captures an image of the part at the Feeder's pick location, moves the camera to a placement
 * of that part in the currently open Job, and ghosts the captured part over the live camera view,
 * rotated by exactly the rotation the machine will apply between picking and placing. Editing the
 * part rotation updates the ghost immediately, so the correct value can be found by eye and then
 * written back to the Feeder.
 *
 * @see PartImageReticle
 * @see Feeder#getPartRotation()
 */
@SuppressWarnings("serial")
public class FeederPartRotationPreviewDialog extends JDialog {
    private static final String RETICLE_KEY = PartImageReticle.class.getName();

    private static final String PREF_WINDOW_X = "FeederPartRotationPreviewDialog.windowX";
    private static final String PREF_WINDOW_Y = "FeederPartRotationPreviewDialog.windowY";
    private static final String PREF_WINDOW_WIDTH = "FeederPartRotationPreviewDialog.windowWidth";
    private static final String PREF_WINDOW_HEIGHT = "FeederPartRotationPreviewDialog.windowHeight";

    /**
     * How much bigger than the part body the captured ghost is, so that a little of the
     * surroundings gives context.
     */
    private static final double GHOST_SIZE_MARGIN = 1.4;

    private static final double GHOST_FEATHER_FRACTION = 0.12;

    private final Preferences prefs =
            Preferences.userNodeForPackage(FeederPartRotationPreviewDialog.class);

    private final Feeder feeder;
    private final Camera camera;
    private final PartImageReticle reticle;

    /**
     * The Feeder's part rotation when the dialog was opened, and the pick rotation that went with
     * it. The preview works off these two, so that no value has to be written to the Feeder until
     * Apply is pressed.
     */
    private final double basePartRotation;
    private final double basePickRotation;

    /** The full resolution capture, retained so the ghost can be re-cropped without re-capturing. */
    private BufferedImage capturedImage;
    private Location capturedUnitsPerPixel;
    private Point capturedCenterPixels;

    private CameraView cameraView;

    private final JComboBox<PlacementCandidate> placementCombo;
    private final JSpinner rotationSpinner;
    private final JSpinner ghostSizeSpinner;
    private final JSlider opacitySlider;
    private final JLabel pickAngleLabel;
    private final JLabel placeAngleLabel;
    private final JLabel deltaAngleLabel;
    private final JButton applyButton;

    private boolean updating;

    /**
     * A placement of the Feeder's part in the open Job, paired with the board it sits on.
     */
    private static class PlacementCandidate {
        final BoardLocation boardLocation;
        final Placement placement;

        PlacementCandidate(BoardLocation boardLocation, Placement placement) {
            this.boardLocation = boardLocation;
            this.placement = placement;
        }

        /**
         * @return The machine location, including rotation, that the part will be placed at.
         */
        Location getMachineLocation() {
            return Utils2D.calculateBoardPlacementLocation(boardLocation, placement.getLocation());
        }

        @Override
        public String toString() {
            String board = boardLocation.getBoard() == null ? "?" : boardLocation.getBoard().getName();
            return String.format("%s / %s @ %.2f°", board, placement.getId(),
                    getMachineLocation().getRotation());
        }
    }

    /**
     * Finds the placements of the given Part in the currently open Job. Placements that have
     * already been placed are deliberately included: those are exactly the ones worth inspecting
     * when a rotation looks wrong.
     */
    private static List<PlacementCandidate> findPlacements(Part part) {
        List<PlacementCandidate> candidates = new ArrayList<>();
        if (part == null || MainFrame.get() == null || MainFrame.get().getJobTab() == null) {
            return candidates;
        }
        Job job = MainFrame.get().getJobTab().getJob();
        if (job == null) {
            return candidates;
        }
        // getBoardLocations() is already flattened through any nested panels.
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            if (!boardLocation.isEnabled() || boardLocation.getBoard() == null) {
                continue;
            }
            for (Placement placement : boardLocation.getBoard().getPlacements()) {
                if (placement.getPart() != part) {
                    continue;
                }
                if (placement.getType() != Placement.Type.Placement) {
                    continue;
                }
                if (!placement.isEnabled()) {
                    continue;
                }
                if (placement.getSide() != boardLocation.getGlobalSide()) {
                    continue;
                }
                candidates.add(new PlacementCandidate(boardLocation, placement));
            }
        }
        return candidates;
    }

    private FeederPartRotationPreviewDialog(Feeder feeder, Camera camera, double partRotation,
            double pickRotation, List<PlacementCandidate> candidates) {
        super(MainFrame.get(), String.format("%s - %s",
                Translations.getString("FeederPartRotationPreviewDialog.Title"), feeder.getName()),
                true);
        this.feeder = feeder;
        this.camera = camera;
        this.basePartRotation = partRotation;
        this.basePickRotation = pickRotation;
        this.reticle = new PartImageReticle(camera);

        setModalityType(JDialog.ModalityType.MODELESS);

        JPanel content = new JPanel();
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));
        getContentPane().add(content);

        content.add(new JLabel(Translations.getString(
                "FeederPartRotationPreviewDialog.Placement")), "1, 1, right, default"); //$NON-NLS-1$
        placementCombo = new JComboBox<>(new DefaultComboBoxModel<>(
                candidates.toArray(new PlacementCandidate[0])));
        placementCombo.setToolTipText(Translations.getString(
                "FeederPartRotationPreviewDialog.Placement.ToolTip")); //$NON-NLS-1$
        // Picking a different placement re-anchors the ghost and changes the place angle.
        placementCombo.addActionListener(e -> updatePreview());
        content.add(placementCombo, "3, 1");
        JButton moveButton = new JButton(Translations.getString(
                "FeederPartRotationPreviewDialog.Action.MoveToPlacement")); //$NON-NLS-1$
        moveButton.setToolTipText(Translations.getString(
                "FeederPartRotationPreviewDialog.Action.MoveToPlacement.Description")); //$NON-NLS-1$
        moveButton.addActionListener(e -> moveToPlacement());
        content.add(moveButton, "5, 1");

        content.add(new JLabel(Translations.getString(
                "FeederPartRotationPreviewDialog.PartRotation")), "1, 3, right, default"); //$NON-NLS-1$
        JPanel rotationPanel = new JPanel();
        // SpinnerNumberModel throws if the initial value falls outside its range, and a feeder is
        // free to store any rotation at all, so clamp rather than fail to open.
        rotationSpinner = new JSpinner(new SpinnerNumberModel(
                clamp(partRotation, -360.0, 360.0), -360.0, 360.0, 1.0));
        rotationSpinner.setToolTipText(Translations.getString(
                "FeederPartRotationPreviewDialog.PartRotation.ToolTip")); //$NON-NLS-1$
        ((JSpinner.DefaultEditor) rotationSpinner.getEditor()).getTextField().setColumns(7);
        rotationSpinner.addChangeListener(e -> updatePreview());
        rotationPanel.add(rotationSpinner);
        for (double step : new double[] {-90, -1, 1, 90}) {
            JButton button = new JButton(String.format("%+.0f°", step));
            button.setMargin(new java.awt.Insets(2, 4, 2, 4));
            button.addActionListener(e -> nudgeRotation(step));
            rotationPanel.add(button);
        }
        content.add(rotationPanel, "3, 3, 3, 1");

        pickAngleLabel = new JLabel();
        content.add(new JLabel(Translations.getString(
                "FeederPartRotationPreviewDialog.PickAngle")), "1, 5, right, default"); //$NON-NLS-1$
        content.add(pickAngleLabel, "3, 5, 3, 1");

        placeAngleLabel = new JLabel();
        content.add(new JLabel(Translations.getString(
                "FeederPartRotationPreviewDialog.PlaceAngle")), "1, 7, right, default"); //$NON-NLS-1$
        content.add(placeAngleLabel, "3, 7, 3, 1");

        deltaAngleLabel = new JLabel();
        content.add(new JLabel(Translations.getString(
                "FeederPartRotationPreviewDialog.DeltaAngle")), "1, 9, right, default"); //$NON-NLS-1$
        content.add(deltaAngleLabel, "3, 9, 3, 1");

        content.add(new JLabel(Translations.getString(
                "FeederPartRotationPreviewDialog.GhostSize")), "1, 11, right, default"); //$NON-NLS-1$
        JPanel ghostPanel = new JPanel();
        ghostSizeSpinner = new JSpinner(new SpinnerNumberModel(
                clamp(defaultGhostSizeMm(feeder.getPart()), 0.5, 200.0), 0.5, 200.0, 0.5));
        ghostSizeSpinner.setToolTipText(Translations.getString(
                "FeederPartRotationPreviewDialog.GhostSize.ToolTip")); //$NON-NLS-1$
        ((JSpinner.DefaultEditor) ghostSizeSpinner.getEditor()).getTextField().setColumns(6);
        ghostSizeSpinner.addChangeListener(e -> rebuildGhost());
        ghostPanel.add(ghostSizeSpinner);
        ghostPanel.add(new JLabel("mm")); //$NON-NLS-1$
        ghostPanel.add(new JLabel("   " + Translations.getString(
                "FeederPartRotationPreviewDialog.Opacity"))); //$NON-NLS-1$
        opacitySlider = new JSlider(0, 100, 55);
        opacitySlider.addChangeListener(e -> {
            reticle.setAlpha(opacitySlider.getValue() / 100f);
            repaintCameraView();
        });
        ghostPanel.add(opacitySlider);
        content.add(ghostPanel, "3, 11, 3, 1");

        JPanel buttons = new JPanel();
        JButton recaptureButton = new JButton(Translations.getString(
                "FeederPartRotationPreviewDialog.Action.Capture")); //$NON-NLS-1$
        recaptureButton.setToolTipText(Translations.getString(
                "FeederPartRotationPreviewDialog.Action.Capture.Description")); //$NON-NLS-1$
        recaptureButton.addActionListener(e -> capture(false));
        buttons.add(recaptureButton);
        applyButton = new JButton(Translations.getString(
                "FeederPartRotationPreviewDialog.Action.Apply")); //$NON-NLS-1$
        applyButton.setToolTipText(Translations.getString(
                "FeederPartRotationPreviewDialog.Action.Apply.Description")); //$NON-NLS-1$
        applyButton.addActionListener(e -> apply());
        buttons.add(applyButton);
        JButton revertButton = new JButton(Translations.getString(
                "FeederPartRotationPreviewDialog.Action.Revert")); //$NON-NLS-1$
        revertButton.addActionListener(e -> {
            rotationSpinner.setValue(basePartRotation);
            updatePreview();
        });
        buttons.add(revertButton);
        JButton closeButton = new JButton(Translations.getString(
                "FeederPartRotationPreviewDialog.Action.Close")); //$NON-NLS-1$
        closeButton.addActionListener(e -> dispose());
        buttons.add(closeButton);
        content.add(buttons, "1, 13, 5, 1");

        // Tear down from windowClosed rather than the Close button, so that closing via the window
        // decoration does not leave the reticle behind.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                removeReticle();
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                saveWindowGeometry();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                saveWindowGeometry();
            }
        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        restoreWindowGeometry();

        installReticle();
        updatePreview();
        // Kick off the whole sequence: capture at the pick location, then go and look at the board.
        capture(true);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * The default size of the ghost, derived from the part's footprint with a margin. Falls back
     * through the footprint body dimensions, the pad bounds and the outer dimension, since many
     * footprints only fill in some of those.
     */
    private static double defaultGhostSizeMm(Part part) {
        double width = 0;
        double height = 0;
        LengthUnit units = LengthUnit.Millimeters;
        Package pkg = part == null ? null : part.getPackage();
        Footprint footprint = pkg == null ? null : pkg.getFootprint();
        if (footprint != null) {
            units = footprint.getUnits();
            width = footprint.getBodyWidth();
            height = footprint.getBodyHeight();
            if (width <= 0 || height <= 0) {
                java.awt.Shape pads = footprint.getPadsShape();
                if (pads != null && !pads.getBounds2D().isEmpty()) {
                    width = pads.getBounds2D().getWidth();
                    height = pads.getBounds2D().getHeight();
                }
            }
            if (width <= 0 || height <= 0) {
                width = height = footprint.getOuterDimension();
            }
        }
        if (width <= 0 || height <= 0) {
            // Nothing to go on. Something visible that the user can then adjust.
            return 10;
        }
        double diagonal = Math.hypot(width, height) * GHOST_SIZE_MARGIN;
        return new Length(diagonal, units).convertToUnits(LengthUnit.Millimeters).getValue();
    }

    /**
     * Moves the camera to the Feeder's pick location and captures the part.
     *
     * @param thenMoveToPlacement If true, continue on to the placement location afterwards, so the
     *        whole sequence runs as a single machine task.
     */
    private void capture(boolean thenMoveToPlacement) {
        PlacementCandidate candidate = (PlacementCandidate) placementCombo.getSelectedItem();
        UiUtils.submitUiMachineTask(() -> {
            Nozzle nozzle;
            try {
                nozzle = FeedersPanel.getCompatibleNozzleAndTip(feeder, false);
            }
            catch (Exception e) {
                nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
            }
            Location pickLocation = FeedersPanel.preliminaryPickLocation(feeder, nozzle);
            MovableUtils.moveToLocationAtSafeZ(camera, pickLocation);
            MovableUtils.fireTargetedUserAction(camera);
            BufferedImage image = camera.lightSettleAndCapture();
            Location unitsPerPixel = camera.getUnitsPerPixelAtZ();
            Point centerPixels = VisionUtils.getLocationPixels(camera, pickLocation);
            SwingUtilities.invokeAndWait(() -> {
                capturedImage = image;
                capturedUnitsPerPixel = unitsPerPixel;
                capturedCenterPixels = centerPixels;
                rebuildGhost();
            });
            if (thenMoveToPlacement && candidate != null) {
                MovableUtils.moveToLocationAtSafeZ(camera, candidate.getMachineLocation());
                MovableUtils.fireTargetedUserAction(camera);
            }
        });
    }

    private void moveToPlacement() {
        PlacementCandidate candidate = (PlacementCandidate) placementCombo.getSelectedItem();
        if (candidate == null) {
            return;
        }
        UiUtils.submitUiMachineTask(() -> {
            MovableUtils.moveToLocationAtSafeZ(camera, candidate.getMachineLocation());
            MovableUtils.fireTargetedUserAction(camera);
        });
    }

    /**
     * Crops the retained capture down to the current ghost size and hands it to the reticle.
     */
    private void rebuildGhost() {
        if (capturedImage == null || capturedUnitsPerPixel == null) {
            return;
        }
        double sizeMm = (Double) ghostSizeSpinner.getValue();
        Location upp = capturedUnitsPerPixel.convertToUnits(LengthUnit.Millimeters);
        if (upp.getX() <= 0 || upp.getY() <= 0) {
            Logger.warn("Camera {} has no usable units per pixel, cannot build the ghost image.",
                    camera.getName());
            return;
        }
        double halfWidth = (sizeMm / 2) / upp.getX();
        double halfHeight = (sizeMm / 2) / upp.getY();
        double centerX = capturedCenterPixels == null
                ? capturedImage.getWidth() / 2.0 : capturedCenterPixels.getX();
        double centerY = capturedCenterPixels == null
                ? capturedImage.getHeight() / 2.0 : capturedCenterPixels.getY();
        BufferedImage ghost = ImageUtils.cropCentered(capturedImage, centerX, centerY,
                halfWidth, halfHeight);
        if (ghost == null) {
            Logger.warn("The requested ghost region falls outside the captured image.");
            return;
        }
        ghost = ImageUtils.featherEdges(ghost, GHOST_FEATHER_FRACTION);
        reticle.setImage(ghost, capturedUnitsPerPixel);
        updatePreview();
    }

    /**
     * Recomputes the preview angles and pushes them to the reticle. Everything is derived from the
     * values read when the dialog opened plus the current editor value, so nothing is written to
     * the Feeder until Apply.
     */
    private void updatePreview() {
        if (updating) {
            return;
        }
        updating = true;
        try {
            double partRotation = (Double) rotationSpinner.getValue();
            // Increasing the part rotation by d increases the pick rotation by d, by contract.
            double pickRotation = basePickRotation + (partRotation - basePartRotation);
            PlacementCandidate candidate = (PlacementCandidate) placementCombo.getSelectedItem();
            Double placeRotation = null;
            if (candidate != null) {
                try {
                    Location placeLocation = candidate.getMachineLocation();
                    placeRotation = placeLocation.getRotation();
                    reticle.setAnchorLocation(placeLocation);
                }
                catch (Exception e) {
                    // The job may have changed underneath us.
                    Logger.warn(e, "Could not resolve the placement location.");
                }
            }
            pickAngleLabel.setText(String.format("%.2f°", pickRotation));
            if (placeRotation == null) {
                placeAngleLabel.setText("-"); //$NON-NLS-1$
                deltaAngleLabel.setText("-"); //$NON-NLS-1$
            }
            else {
                // This is how far the part is physically turned between being picked and being
                // placed, which is exactly what the ghost has to show.
                double delta = placeRotation - pickRotation;
                placeAngleLabel.setText(String.format("%.2f°", placeRotation));
                deltaAngleLabel.setText(String.format("%.2f°", delta));
                reticle.setRotation(delta);
            }
            repaintCameraView();
        }
        finally {
            updating = false;
        }
    }

    private void nudgeRotation(double step) {
        double value = (Double) rotationSpinner.getValue() + step;
        // Keep the spinner inside its own model bounds.
        while (value > 360) {
            value -= 360;
        }
        while (value < -360) {
            value += 360;
        }
        rotationSpinner.setValue(value);
        updatePreview();
    }

    private void apply() {
        UiUtils.messageBoxOnException(() -> {
            feeder.setPartRotation((Double) rotationSpinner.getValue());
            // Read back: some feeders quantize or normalize the value, and the user should see
            // what was actually stored.
            double stored = feeder.getPartRotation();
            rotationSpinner.setValue(stored);
            updatePreview();
            if (MainFrame.get() != null && MainFrame.get().getFeedersTab() != null) {
                MainFrame.get().getFeedersTab().refresh(feeder);
            }
        });
    }

    private void installReticle() {
        if (MainFrame.get() == null || MainFrame.get().getCameraViews() == null) {
            return;
        }
        cameraView = MainFrame.get().getCameraViews().ensureCameraVisible(camera);
        if (cameraView != null) {
            reticle.setAlpha(opacitySlider.getValue() / 100f);
            cameraView.setReticle(RETICLE_KEY, reticle);
        }
    }

    private void removeReticle() {
        if (cameraView != null) {
            cameraView.removeReticle(RETICLE_KEY);
            cameraView.repaint();
            cameraView = null;
        }
    }

    private void repaintCameraView() {
        // A camera that is not streaming would otherwise leave the ghost frozen at its old angle.
        if (cameraView != null) {
            cameraView.repaint();
        }
    }

    private void restoreWindowGeometry() {
        int x = prefs.getInt(PREF_WINDOW_X, Integer.MIN_VALUE);
        int y = prefs.getInt(PREF_WINDOW_Y, Integer.MIN_VALUE);
        int width = prefs.getInt(PREF_WINDOW_WIDTH, 0);
        int height = prefs.getInt(PREF_WINDOW_HEIGHT, 0);
        if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || width <= 0 || height <= 0) {
            setLocationRelativeTo(MainFrame.get());
            return;
        }
        setBounds(x, y, width, height);
    }

    private void saveWindowGeometry() {
        if (!isShowing()) {
            return;
        }
        prefs.putInt(PREF_WINDOW_X, getX());
        prefs.putInt(PREF_WINDOW_Y, getY());
        prefs.putInt(PREF_WINDOW_WIDTH, getWidth());
        prefs.putInt(PREF_WINDOW_HEIGHT, getHeight());
    }

    /**
     * Opens the dialog for the given Feeder, reading the Feeder state that the preview is based on
     * off the machine thread first.
     *
     * @param feeder The Feeder whose part rotation is being previewed.
     */
    public static void open(Feeder feeder) {
        UiUtils.messageBoxOnException(() -> {
            if (!feeder.isPartRotationAdjustable()) {
                throw new Exception(Translations.getString(
                        "FeederPartRotationPreviewDialog.Error.NotAdjustable")); //$NON-NLS-1$
            }
            List<PlacementCandidate> candidates = findPlacements(feeder.getPart());
            if (candidates.isEmpty()) {
                throw new Exception(Translations.getString(
                        "FeederPartRotationPreviewDialog.Error.NoJob")); //$NON-NLS-1$
            }
            Camera camera = MainFrame.get().getMachineControls().getSelectedTool().getHead()
                    .getDefaultCamera();
            double partRotation = feeder.getPartRotation();
            double pickRotation = feeder.getPickLocation().getRotation();
            new FeederPartRotationPreviewDialog(feeder, camera, partRotation, pickRotation,
                    candidates).setVisible(true);
        });
    }
}
