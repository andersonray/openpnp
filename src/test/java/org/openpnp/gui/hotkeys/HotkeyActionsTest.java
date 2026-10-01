package org.openpnp.gui.hotkeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;
import org.openpnp.Translations;
import org.openpnp.model.HotkeyBinding;

public class HotkeyActionsTest {

    private static final int CTRL = KeyEvent.CTRL_DOWN_MASK;
    private static final int CTRL_SHIFT = KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK;

    @Test
    public void idsAreUniqueAndNonEmpty() {
        Set<String> ids = new HashSet<>();
        for (HotkeyActionDescriptor descriptor : HotkeyActions.getAll()) {
            assertNotNull(descriptor.getId());
            assertFalse(descriptor.getId().trim().isEmpty());
            assertTrue(ids.add(descriptor.getId()), "duplicate id " + descriptor.getId());
        }
        assertFalse(ids.isEmpty());
    }

    /**
     * Catches a translation key that was added to the registry but forgotten in
     * translations.properties. Translations returns a !key! sentinel for a missing key rather than
     * throwing, so without this the omission would only show up as a mangled label at runtime.
     */
    @Test
    public void everyTranslationKeyResolves() {
        for (HotkeyActionDescriptor descriptor : HotkeyActions.getAll()) {
            assertResolves(descriptor.getNameKey());
            assertResolves(descriptor.getGroupKey());
        }
    }

    private void assertResolves(String key) {
        String value = Translations.getString(key);
        assertNotNull(value, key);
        assertFalse(value.contains("!"), "missing translation for " + key + ", got " + value);
        assertFalse(value.trim().isEmpty(), key);
    }

    @Test
    public void everyDefaultKeyStrokeParses() {
        for (HotkeyActionDescriptor descriptor : HotkeyActions.getAll()) {
            for (String keyStroke : descriptor.getDefaultKeyStrokes()) {
                assertNotNull(KeyStroke.getKeyStroke(keyStroke),
                        "unparseable default " + keyStroke + " on " + descriptor.getId());
            }
        }
    }

    /**
     * Defaults must be stored in the canonical form KeyStroke.toString() produces, because that is
     * the form written to hotkeys.xml. AWT always emits modifiers in the order
     * shift ctrl meta alt altGraph, so "ctrl shift pressed R" parses but is not canonical.
     */
    @Test
    public void defaultKeyStrokesAreCanonical() {
        for (HotkeyActionDescriptor descriptor : HotkeyActions.getAll()) {
            for (String keyStroke : descriptor.getDefaultKeyStrokes()) {
                assertEquals(KeyStroke.getKeyStroke(keyStroke).toString(), keyStroke,
                        "non-canonical default on " + descriptor.getId());
            }
        }
    }

    @Test
    public void noDuplicateKeyStrokesInDefaults() {
        Set<KeyStroke> seen = new HashSet<>();
        for (HotkeyBinding binding : HotkeyActions.defaultBindings()) {
            assertTrue(seen.add(binding.toKeyStroke()),
                    "default keystroke bound twice: " + binding.getKeyStroke());
        }
    }

    @Test
    public void machineToggleEnabledShipsUnbound() {
        HotkeyActionDescriptor descriptor = HotkeyActions.get("machine.toggle-enabled");

        assertNotNull(descriptor);
        assertTrue(descriptor.getDefaultKeyStrokes().isEmpty());
    }

    /**
     * The regression oracle for this whole feature. The expected set below is transcribed literally
     * from the hardcoded block that MainFrame used to install, so if the defaults ever stop
     * reproducing the shortcuts that shipped before hotkeys became configurable, this fails.
     */
    @Test
    public void defaultsReproduceThePreviouslyHardcodedBindings() {
        Set<String> expected = new LinkedHashSet<>();
        for (int mask : new int[] {CTRL, CTRL_SHIFT}) {
            expected.add(expect("jog.y-plus", KeyEvent.VK_UP, mask));
            expected.add(expect("jog.y-minus", KeyEvent.VK_DOWN, mask));
            expected.add(expect("jog.x-minus", KeyEvent.VK_LEFT, mask));
            expected.add(expect("jog.x-plus", KeyEvent.VK_RIGHT, mask));
            expected.add(expect("jog.z-plus", KeyEvent.VK_QUOTE, mask));
            expected.add(expect("jog.z-minus", KeyEvent.VK_SLASH, mask));
            expected.add(expect("jog.c-plus", KeyEvent.VK_COMMA, mask));
            expected.add(expect("jog.c-minus", KeyEvent.VK_PERIOD, mask));
            expected.add(expect("jog.increment-lower", KeyEvent.VK_MINUS, mask));
            expected.add(expect("jog.increment-raise", KeyEvent.VK_EQUALS, mask));
            expected.add(expect("machine.home", KeyEvent.VK_H, mask));
        }
        expected.add(expect("job.start-pause-resume", KeyEvent.VK_R, CTRL_SHIFT));
        expected.add(expect("job.step", KeyEvent.VK_S, CTRL_SHIFT));
        expected.add(expect("job.stop", KeyEvent.VK_A, CTRL_SHIFT));
        expected.add(expect("nozzle.park-xy", KeyEvent.VK_P, CTRL_SHIFT));
        expected.add(expect("nozzle.park-z", KeyEvent.VK_L, CTRL_SHIFT));
        expected.add(expect("nozzle.safe-z", KeyEvent.VK_Z, CTRL_SHIFT));
        expected.add(expect("nozzle.discard", KeyEvent.VK_D, CTRL_SHIFT));
        expected.add(expect("jog.increment-1", KeyEvent.VK_F1, CTRL_SHIFT));
        expected.add(expect("jog.increment-2", KeyEvent.VK_F2, CTRL_SHIFT));
        expected.add(expect("jog.increment-3", KeyEvent.VK_F3, CTRL_SHIFT));
        expected.add(expect("jog.increment-4", KeyEvent.VK_F4, CTRL_SHIFT));
        expected.add(expect("jog.increment-5", KeyEvent.VK_F5, CTRL_SHIFT));

        Set<String> actual = new LinkedHashSet<>();
        List<String> actualList = new ArrayList<>();
        for (HotkeyBinding binding : HotkeyActions.defaultBindings()) {
            String entry = binding.getAction() + " -> " + binding.toKeyStroke();
            actual.add(entry);
            actualList.add(entry);
        }

        assertEquals(expected, actual);
        // Also assert there are no duplicate rows hiding inside the set comparison.
        assertEquals(actualList.size(), actual.size());
    }

    private String expect(String actionId, int keyCode, int modifiers) {
        return actionId + " -> " + KeyStroke.getKeyStroke(keyCode, modifiers);
    }
}
