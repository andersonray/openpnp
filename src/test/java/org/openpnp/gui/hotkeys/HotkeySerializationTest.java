package org.openpnp.gui.hotkeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

/**
 * hotkeys.xml stores keystrokes as strings, so the whole persistence layer rests on
 * KeyStroke.toString() and KeyStroke.getKeyStroke(String) being exact inverses for every key a user
 * might bind. This proves it for the range we support, rather than assuming it.
 */
public class HotkeySerializationTest {

    private static final int[] MODIFIER_COMBINATIONS = {
            0,
            InputEvent.SHIFT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK,
            InputEvent.ALT_DOWN_MASK,
            InputEvent.META_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK,
    };

    private void assertRoundTrips(int keyCode, int modifiers) {
        KeyStroke original = KeyStroke.getKeyStroke(keyCode, modifiers);
        assertNotNull(original);

        String text = original.toString();
        KeyStroke parsed = KeyStroke.getKeyStroke(text);

        assertNotNull(parsed, "could not parse " + text);
        // Compare KeyStrokes, not strings: AWT normalises modifier order, so a string comparison
        // against an arbitrarily ordered input would fail for the wrong reason.
        assertEquals(original, parsed, "round trip failed for " + text);
        assertEquals(text, parsed.toString(), "toString is not stable for " + text);
    }

    private void assertAllRoundTrip(List<Integer> keyCodes) {
        for (int keyCode : keyCodes) {
            for (int modifiers : MODIFIER_COMBINATIONS) {
                assertRoundTrips(keyCode, modifiers);
            }
        }
    }

    private List<Integer> range(int first, int last) {
        List<Integer> codes = new ArrayList<>();
        for (int code = first; code <= last; code++) {
            codes.add(code);
        }
        return codes;
    }

    /**
     * F13 to F24 are the keys we recommend for a macropad, so they matter most of all. Note that
     * VK_F13 is not VK_F12 + 1, which is why they are listed separately.
     */
    @Test
    public void functionKeysRoundTrip() {
        assertAllRoundTrip(range(KeyEvent.VK_F1, KeyEvent.VK_F12));
        assertAllRoundTrip(range(KeyEvent.VK_F13, KeyEvent.VK_F24));
    }

    @Test
    public void lettersAndDigitsRoundTrip() {
        assertAllRoundTrip(range(KeyEvent.VK_A, KeyEvent.VK_Z));
        assertAllRoundTrip(range(KeyEvent.VK_0, KeyEvent.VK_9));
    }

    /**
     * These six are the punctuation keys the shipped defaults use for jogging Z and C and for
     * changing the jog increment. If any of them failed to round trip, those defaults would be
     * silently lost on the first save.
     */
    @Test
    public void defaultPunctuationRoundTrips() {
        assertAllRoundTrip(List.of(KeyEvent.VK_QUOTE, KeyEvent.VK_SLASH, KeyEvent.VK_COMMA,
                KeyEvent.VK_PERIOD, KeyEvent.VK_MINUS, KeyEvent.VK_EQUALS));
    }

    @Test
    public void navigationKeysRoundTrip() {
        assertAllRoundTrip(List.of(KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT,
                KeyEvent.VK_RIGHT, KeyEvent.VK_HOME, KeyEvent.VK_END, KeyEvent.VK_PAGE_UP,
                KeyEvent.VK_PAGE_DOWN, KeyEvent.VK_INSERT, KeyEvent.VK_DELETE));
    }

    @Test
    public void whitespaceAndControlKeysRoundTrip() {
        assertAllRoundTrip(List.of(KeyEvent.VK_ENTER, KeyEvent.VK_SPACE, KeyEvent.VK_TAB,
                KeyEvent.VK_ESCAPE, KeyEvent.VK_BACK_SPACE));
    }

    @Test
    public void numpadKeysRoundTrip() {
        assertAllRoundTrip(range(KeyEvent.VK_NUMPAD0, KeyEvent.VK_NUMPAD9));
        assertAllRoundTrip(List.of(KeyEvent.VK_DIVIDE, KeyEvent.VK_MULTIPLY, KeyEvent.VK_ADD,
                KeyEvent.VK_SUBTRACT, KeyEvent.VK_DECIMAL, KeyEvent.VK_KP_UP, KeyEvent.VK_KP_DOWN,
                KeyEvent.VK_KP_LEFT, KeyEvent.VK_KP_RIGHT));
    }

    /**
     * Documents the modifier ordering trap that the canonical-form assertions elsewhere rely on.
     */
    @Test
    public void modifierOrderIsNormalisedToShiftCtrl() {
        assertEquals("shift ctrl pressed R", KeyStroke.getKeyStroke("ctrl shift pressed R").toString());
        assertEquals(KeyStroke.getKeyStroke("ctrl shift pressed R"),
                KeyStroke.getKeyStroke("shift ctrl pressed R"));
    }
}
