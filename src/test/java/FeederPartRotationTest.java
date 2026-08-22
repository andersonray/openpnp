import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.machine.neoden4.Neoden4Feeder;
import org.openpnp.machine.rapidplacer.RapidFeeder;
import org.openpnp.machine.reference.feeder.AdvancedLoosePartFeeder;
import org.openpnp.machine.reference.feeder.BlindsFeeder;
import org.openpnp.machine.reference.feeder.ReferenceAutoFeeder;
import org.openpnp.machine.reference.feeder.ReferenceDragFeeder;
import org.openpnp.machine.reference.feeder.ReferenceHeapFeeder;
import org.openpnp.machine.reference.feeder.ReferenceLeverFeeder;
import org.openpnp.machine.reference.feeder.ReferenceLoosePartFeeder;
import org.openpnp.machine.reference.feeder.ReferencePushPullFeeder;
import org.openpnp.machine.reference.feeder.ReferenceRotatedTrayFeeder;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder;
import org.openpnp.machine.reference.feeder.ReferenceStripFeeder;
import org.openpnp.machine.reference.feeder.ReferenceTrayFeeder;
import org.openpnp.machine.reference.feeder.ReferenceTubeFeeder;
import org.openpnp.machine.reference.feeder.SchultzFeeder;
import org.openpnp.machine.reference.feeder.SlotSchultzFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Feeder;

import com.google.common.io.Files;

/**
 * Verifies the {@link Feeder#getPartRotation()} contract across the Feeder implementations:
 * increasing the part rotation by d must increase getPickLocation()'s rotation by d, and must not
 * move the pick X, Y or Z.
 * <p>
 * This is the property the Part Rotation Preview tool relies on to show a correct preview, and it
 * is easy to break with a copy/paste error (several Feeders store the rotation in a dedicated
 * attribute, one stores it negated, and on the slot based Feeders the obvious field also rotates
 * the pick X/Y).
 */
public class FeederPartRotationTest {
    /**
     * A Feeder configured enough that getPickLocation() works without hardware or vision.
     */
    private static class Fixture {
        final Feeder feeder;
        final boolean expectAdjustable;
        /**
         * True for feeders whose real pick location is produced by vision at feed time, and whose
         * getPickLocation() therefore returns a placeholder until a feed has happened. The slope
         * of the part rotation can only be checked against the real, vision derived expression,
         * which needs a camera, so those are checked by the dedicated storage tests instead.
         */
        final boolean pickLocationIsVisionDerived;

        Fixture(Feeder feeder, boolean expectAdjustable) {
            this(feeder, expectAdjustable, false);
        }

        Fixture(Feeder feeder, boolean expectAdjustable, boolean pickLocationIsVisionDerived) {
            this.feeder = feeder;
            this.expectAdjustable = expectAdjustable;
            this.pickLocationIsVisionDerived = pickLocationIsVisionDerived;
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");

        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/BasicJobTest/machine.xml"),
                new File(workingDirectory, "machine.xml"));
        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/BasicJobTest/packages.xml"),
                new File(workingDirectory, "packages.xml"));
        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/BasicJobTest/parts.xml"),
                new File(workingDirectory, "parts.xml"));

        Configuration.initialize(workingDirectory);
        Configuration.get().load();
    }

    private static List<Fixture> fixtures() throws Exception {
        List<Fixture> fixtures = new ArrayList<>();
        Location location = new Location(LengthUnit.Millimeters, 10, 20, -3, 17);

        ReferenceAutoFeeder autoFeeder = new ReferenceAutoFeeder();
        autoFeeder.setLocation(location);
        fixtures.add(new Fixture(autoFeeder, true));

        ReferenceTubeFeeder tubeFeeder = new ReferenceTubeFeeder();
        tubeFeeder.setLocation(location);
        fixtures.add(new Fixture(tubeFeeder, true));

        SchultzFeeder schultzFeeder = new SchultzFeeder();
        schultzFeeder.setLocation(location);
        fixtures.add(new Fixture(schultzFeeder, true));

        RapidFeeder rapidFeeder = new RapidFeeder();
        rapidFeeder.setLocation(location);
        fixtures.add(new Fixture(rapidFeeder, true));

        ReferenceTrayFeeder trayFeeder = new ReferenceTrayFeeder();
        trayFeeder.setLocation(location);
        trayFeeder.setTrayCountX(2);
        trayFeeder.setTrayCountY(2);
        trayFeeder.setOffsets(new Location(LengthUnit.Millimeters, 5, 5, 0, 0));
        fixtures.add(new Fixture(trayFeeder, true));

        ReferenceDragFeeder dragFeeder = new ReferenceDragFeeder();
        dragFeeder.setLocation(location);
        fixtures.add(new Fixture(dragFeeder, true));

        ReferenceLeverFeeder leverFeeder = new ReferenceLeverFeeder();
        leverFeeder.setLocation(location);
        fixtures.add(new Fixture(leverFeeder, true));

        ReferenceLoosePartFeeder loosePartFeeder = new ReferenceLoosePartFeeder();
        loosePartFeeder.setLocation(location);
        fixtures.add(new Fixture(loosePartFeeder, true));

        // The advanced loose part feeder builds its pick rotation as -(visionAngle +
        // location.rotation), but its un-fed getPickLocation() returns the raw location as a
        // placeholder, which is not negated. Only the vision derived branch drives real
        // placements, so that is the one getPartRotation() matches, and the slope cannot be
        // checked here. See testAdvancedLoosePartFeederNegatesInternally().
        AdvancedLoosePartFeeder advancedLoosePartFeeder = new AdvancedLoosePartFeeder();
        advancedLoosePartFeeder.setLocation(location);
        fixtures.add(new Fixture(advancedLoosePartFeeder, true, true));

        ReferenceStripFeeder stripFeeder = new ReferenceStripFeeder();
        stripFeeder.setLocation(location);
        stripFeeder.setReferenceHoleLocation(new Location(LengthUnit.Millimeters, 10, 20, 0, 0));
        stripFeeder.setLastHoleLocation(new Location(LengthUnit.Millimeters, 30, 20, 0, 0));
        fixtures.add(new Fixture(stripFeeder, true));

        ReferencePushPullFeeder pushPullFeeder = new ReferencePushPullFeeder();
        pushPullFeeder.setLocation(location);
        pushPullFeeder.setHole1Location(new Location(LengthUnit.Millimeters, 10, 18, 0, 0));
        pushPullFeeder.setHole2Location(new Location(LengthUnit.Millimeters, 14, 18, 0, 0));
        fixtures.add(new Fixture(pushPullFeeder, true));

        ReferenceRotatedTrayFeeder rotatedTrayFeeder = new ReferenceRotatedTrayFeeder();
        rotatedTrayFeeder.setLocation(location);
        rotatedTrayFeeder.setTrayCountCols(2);
        rotatedTrayFeeder.setTrayCountRows(2);
        rotatedTrayFeeder.setOffsets(new Location(LengthUnit.Millimeters, 5, 5, 0, 0));
        fixtures.add(new Fixture(rotatedTrayFeeder, true));

        Neoden4Feeder neoden4Feeder = new Neoden4Feeder();
        neoden4Feeder.setLocation(location);
        fixtures.add(new Fixture(neoden4Feeder, true));

        ReferenceSlotAutoFeeder slotAutoFeeder = new ReferenceSlotAutoFeeder();
        slotAutoFeeder.setLocation(location);
        ReferenceSlotAutoFeeder.Feeder bankFeeder = new ReferenceSlotAutoFeeder.Feeder();
        bankFeeder.setOffsets(new Location(LengthUnit.Millimeters, 1, 2, 0, 0));
        slotAutoFeeder.getBank().getFeeders().add(bankFeeder);
        slotAutoFeeder.setFeeder(bankFeeder);
        fixtures.add(new Fixture(slotAutoFeeder, true));

        SlotSchultzFeeder slotSchultzFeeder = new SlotSchultzFeeder();
        slotSchultzFeeder.setLocation(location);
        SlotSchultzFeeder.Feeder schultzBankFeeder = new SlotSchultzFeeder.Feeder();
        schultzBankFeeder.setOffsets(new Location(LengthUnit.Millimeters, 1, 2, 0, 0));
        slotSchultzFeeder.getBank().getFeeders().add(schultzBankFeeder);
        slotSchultzFeeder.setFeeder(schultzBankFeeder);
        fixtures.add(new Fixture(slotSchultzFeeder, true));

        BlindsFeeder blindsFeeder = new BlindsFeeder();
        blindsFeeder.setLocation(location);
        blindsFeeder.setFiducial1Location(new Location(LengthUnit.Millimeters, 100, 100, 0, 0));
        blindsFeeder.setFiducial2Location(new Location(LengthUnit.Millimeters, 180, 100, 0, 0));
        blindsFeeder.setFiducial3Location(new Location(LengthUnit.Millimeters, 100, 165, 0, 0));
        blindsFeeder.setPocketPitch(new Length(4, LengthUnit.Millimeters));
        blindsFeeder.setPocketCenterline(new Length(5, LengthUnit.Millimeters));
        fixtures.add(new Fixture(blindsFeeder, true));

        // The heap feeder gets its pick rotation entirely from drop box vision, so it must NOT
        // claim to have an adjustable part rotation.
        fixtures.add(new Fixture(new ReferenceHeapFeeder(), false));

        return fixtures;
    }

    @Test
    public void testPartRotationIsAdjustableWhereExpected() throws Exception {
        for (Fixture fixture : fixtures()) {
            assertEquals(fixture.expectAdjustable, fixture.feeder.isPartRotationAdjustable(),
                    fixture.feeder.getClass().getSimpleName()
                            + ": unexpected isPartRotationAdjustable()");
        }
    }

    /**
     * The core contract: the part rotation has slope +1 on the pick rotation.
     */
    @Test
    public void testPartRotationHasUnitSlopeOnPickRotation() throws Exception {
        final double delta = 30;
        for (Fixture fixture : fixtures()) {
            Feeder feeder = fixture.feeder;
            String name = feeder.getClass().getSimpleName();
            if (!feeder.isPartRotationAdjustable() || fixture.pickLocationIsVisionDerived) {
                continue;
            }

            double basePartRotation = feeder.getPartRotation();
            Location basePickLocation = feeder.getPickLocation()
                    .convertToUnits(LengthUnit.Millimeters);

            feeder.setPartRotation(basePartRotation + delta);

            assertEquals(basePartRotation + delta, feeder.getPartRotation(), 0.0001,
                    name + ": getPartRotation() did not round trip");

            Location newPickLocation = feeder.getPickLocation()
                    .convertToUnits(LengthUnit.Millimeters);

            assertEquals(basePickLocation.getRotation() + delta, newPickLocation.getRotation(),
                    0.0001, name + ": pick rotation did not follow the part rotation");

            // The pick point itself must not move.
            assertEquals(basePickLocation.getX(), newPickLocation.getX(), 0.0001,
                    name + ": pick X moved");
            assertEquals(basePickLocation.getY(), newPickLocation.getY(), 0.0001,
                    name + ": pick Y moved");
            assertEquals(basePickLocation.getZ(), newPickLocation.getZ(), 0.0001,
                    name + ": pick Z moved");
        }
    }

    /**
     * Feeders that do not advertise an adjustable part rotation must refuse to set one rather than
     * silently writing somewhere unrelated.
     */
    @Test
    public void testNonAdjustableFeedersRefuse() throws Exception {
        for (Fixture fixture : fixtures()) {
            if (fixture.feeder.isPartRotationAdjustable()) {
                continue;
            }
            try {
                fixture.feeder.setPartRotation(45);
                fail(fixture.feeder.getClass().getSimpleName()
                        + ": setPartRotation() should have thrown");
            }
            catch (UnsupportedOperationException e) {
                // Expected.
            }
        }
    }

    /**
     * The push pull feeder must not write the part rotation through setLocation(), which would
     * discard its vision calibration.
     */
    @Test
    public void testPushPullFeederKeepsFeederOrientation() throws Exception {
        ReferencePushPullFeeder feeder = new ReferencePushPullFeeder();
        Location location = new Location(LengthUnit.Millimeters, 10, 20, -3, 17);
        feeder.setLocation(location);
        feeder.setHole1Location(new Location(LengthUnit.Millimeters, 10, 18, 0, 0));
        feeder.setHole2Location(new Location(LengthUnit.Millimeters, 14, 18, 0, 0));

        feeder.setPartRotation(90);

        assertEquals(90, feeder.getRotationInFeeder(), 0.0001,
                "the part rotation should be stored in rotationInFeeder");
        assertEquals(17, feeder.getLocation().getRotation(), 0.0001,
                "the feeder's own orientation should be untouched");
    }

    /**
     * The slot based feeders store the part rotation on the feeder loaded into the slot, not on
     * the slot itself, because the slot rotation also rotates the pick offsets.
     */
    @Test
    public void testSlotFeederStoresRotationOnTheBankFeeder() throws Exception {
        ReferenceSlotAutoFeeder slot = new ReferenceSlotAutoFeeder();
        slot.setLocation(new Location(LengthUnit.Millimeters, 10, 20, -3, 17));
        ReferenceSlotAutoFeeder.Feeder bankFeeder = new ReferenceSlotAutoFeeder.Feeder();
        bankFeeder.setOffsets(new Location(LengthUnit.Millimeters, 1, 2, 0, 0));
        slot.getBank().getFeeders().add(bankFeeder);
        slot.setFeeder(bankFeeder);

        slot.setPartRotation(45);

        assertEquals(45, bankFeeder.getOffsets().getRotation(), 0.0001,
                "the part rotation should be stored on the bank feeder's offsets");
        assertEquals(17, slot.getLocation().getRotation(), 0.0001,
                "the slot's own rotation should be untouched");
    }

    /**
     * The advanced loose part feeder derives its pick rotation with an inverted sign, and is
     * expected to hide that from callers.
     */
    @Test
    public void testAdvancedLoosePartFeederNegatesInternally() throws Exception {
        AdvancedLoosePartFeeder feeder = new AdvancedLoosePartFeeder();
        feeder.setLocation(new Location(LengthUnit.Millimeters, 10, 20, -3, 0));

        feeder.setPartRotation(30);

        assertEquals(30, feeder.getPartRotation(), 0.0001, "getPartRotation() should round trip");
        assertEquals(-30, feeder.getLocation().getRotation(), 0.0001,
                "the stored location rotation should be negated");
        assertTrue(feeder.isPartRotationAdjustable());
    }
}
