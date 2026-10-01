package org.openpnp.gui.hotkeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

public class HotkeyTextTest {

    @Test
    public void nullKeyStrokeYieldsEmptyString() {
        assertEquals("", HotkeyText.toDisplayString((KeyStroke) null));
    }

    @Test
    public void bareKeyHasNoModifiers() {
        assertEquals(KeyEvent.getKeyText(KeyEvent.VK_F13),
                HotkeyText.toDisplayString(KeyStroke.getKeyStroke("pressed F13")));
    }

    /**
     * KeyStroke.getModifiers returns a mask carrying both the legacy and the extended modifier
     * bits. The formatter tests the extended bits, so this pins down that the combination actually
     * round-trips through KeyStroke rather than only working for masks we build by hand.
     */
    @Test
    public void modifiersSurviveKeyStrokeConstruction() {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_R,
                InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);

        String text = HotkeyText.toDisplayString(keyStroke);

        assertTrue(text.contains(KeyEvent.getKeyText(KeyEvent.VK_CONTROL)), text);
        assertTrue(text.contains(KeyEvent.getKeyText(KeyEvent.VK_SHIFT)), text);
        assertTrue(text.endsWith(KeyEvent.getKeyText(KeyEvent.VK_R)), text);
    }

    /**
     * Ctrl must always be printed before Shift, so the same combination never reads two ways.
     */
    @Test
    public void modifierOrderIsStable() {
        String fromCtrlShift = HotkeyText.toDisplayString(
                KeyStroke.getKeyStroke("ctrl shift pressed R"));
        String fromShiftCtrl = HotkeyText.toDisplayString(
                KeyStroke.getKeyStroke("shift ctrl pressed R"));

        assertEquals(fromCtrlShift, fromShiftCtrl);
        int ctrlIndex = fromCtrlShift.indexOf(KeyEvent.getKeyText(KeyEvent.VK_CONTROL));
        int shiftIndex = fromCtrlShift.indexOf(KeyEvent.getKeyText(KeyEvent.VK_SHIFT));
        assertTrue(ctrlIndex < shiftIndex, fromCtrlShift);
    }

    @Test
    public void noModifiersYieldsEmptyPrefix() {
        assertEquals("", HotkeyText.toDisplayString(0));
    }

    @Test
    public void modifierPrefixEndsWithSeparator() {
        String prefix = HotkeyText.toDisplayString(InputEvent.CTRL_DOWN_MASK);

        assertTrue(prefix.endsWith("+"), prefix);
        assertFalse(prefix.isEmpty());
    }
}
