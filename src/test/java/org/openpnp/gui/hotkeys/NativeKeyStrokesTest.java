package org.openpnp.gui.hotkeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

/**
 * Loading NativeKeyEvent does not load the native library, only GlobalScreen.registerNativeHook
 * does, so this runs on a headless build machine.
 */
public class NativeKeyStrokesTest {

    private NativeKeyEvent nativeKeyEvent(int nativeKeyCode, int nativeModifiers) {
        return new NativeKeyEvent(NativeKeyEvent.NATIVE_KEY_PRESSED, nativeModifiers, 0,
                nativeKeyCode, NativeKeyEvent.CHAR_UNDEFINED);
    }

    /**
     * F13 to F24 are the keys we recommend for a macropad at the machine, so a gap here would
     * defeat the point of the whole feature.
     */
    @Test
    public void allFunctionKeysMap() {
        int[] nativeCodes = {NativeKeyEvent.VC_F1, NativeKeyEvent.VC_F2, NativeKeyEvent.VC_F3,
                NativeKeyEvent.VC_F4, NativeKeyEvent.VC_F5, NativeKeyEvent.VC_F6,
                NativeKeyEvent.VC_F7, NativeKeyEvent.VC_F8, NativeKeyEvent.VC_F9,
                NativeKeyEvent.VC_F10, NativeKeyEvent.VC_F11, NativeKeyEvent.VC_F12,
                NativeKeyEvent.VC_F13, NativeKeyEvent.VC_F14, NativeKeyEvent.VC_F15,
                NativeKeyEvent.VC_F16, NativeKeyEvent.VC_F17, NativeKeyEvent.VC_F18,
                NativeKeyEvent.VC_F19, NativeKeyEvent.VC_F20, NativeKeyEvent.VC_F21,
                NativeKeyEvent.VC_F22, NativeKeyEvent.VC_F23, NativeKeyEvent.VC_F24};
        int[] awtCodes = {KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4,
                KeyEvent.VK_F5, KeyEvent.VK_F6, KeyEvent.VK_F7, KeyEvent.VK_F8, KeyEvent.VK_F9,
                KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12, KeyEvent.VK_F13,
                KeyEvent.VK_F14, KeyEvent.VK_F15, KeyEvent.VK_F16, KeyEvent.VK_F17,
                KeyEvent.VK_F18, KeyEvent.VK_F19, KeyEvent.VK_F20, KeyEvent.VK_F21,
                KeyEvent.VK_F22, KeyEvent.VK_F23, KeyEvent.VK_F24};

        for (int i = 0; i < nativeCodes.length; i++) {
            assertEquals(awtCodes[i], NativeKeyStrokes.toAwtKeyCode(nativeCodes[i]),
                    "F" + (i + 1) + " does not map");
        }
    }

    @Test
    public void lettersMap() {
        assertEquals(KeyEvent.VK_A, NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_A));
        assertEquals(KeyEvent.VK_R, NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_R));
        assertEquals(KeyEvent.VK_S, NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_S));
        assertEquals(KeyEvent.VK_Z, NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_Z));
    }

    @Test
    public void digitsMap() {
        assertEquals(KeyEvent.VK_0, NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_0));
        assertEquals(KeyEvent.VK_9, NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_9));
    }

    @Test
    public void arrowsMap() {
        assertEquals(KeyEvent.VK_UP, NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_UP));
        assertEquals(KeyEvent.VK_DOWN, NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_DOWN));
        assertEquals(KeyEvent.VK_LEFT, NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_LEFT));
        assertEquals(KeyEvent.VK_RIGHT, NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_RIGHT));
    }

    /**
     * These six carry the shipped jog and jog increment defaults. If any failed to map, those
     * defaults would be unreachable through the global hook while still working through AWT, which
     * would be a baffling inconsistency.
     */
    @Test
    public void defaultPunctuationMaps() {
        assertEquals(KeyEvent.VK_QUOTE, NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_QUOTE));
        assertEquals(KeyEvent.VK_SLASH, NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_SLASH));
        assertEquals(KeyEvent.VK_COMMA, NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_COMMA));
        assertEquals(KeyEvent.VK_PERIOD, NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_PERIOD));
        assertEquals(KeyEvent.VK_MINUS, NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_MINUS));
        assertEquals(KeyEvent.VK_EQUALS, NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_EQUALS));
    }

    @Test
    public void unmappedKeyYieldsUndefined() {
        assertEquals(KeyEvent.VK_UNDEFINED,
                NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_MEDIA_PLAY));
        assertEquals(KeyEvent.VK_UNDEFINED,
                NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_UNDEFINED));
        assertEquals(KeyEvent.VK_UNDEFINED,
                NativeKeyStrokes.toAwtKeyCode(NativeKeyEvent.VC_BROWSER_HOME));
    }

    @Test
    public void modifiersTranslate() {
        assertEquals(0, NativeKeyStrokes.toAwtModifiers(0));
        assertEquals(InputEvent.SHIFT_DOWN_MASK,
                NativeKeyStrokes.toAwtModifiers(NativeKeyEvent.SHIFT_MASK));
        assertEquals(InputEvent.CTRL_DOWN_MASK,
                NativeKeyStrokes.toAwtModifiers(NativeKeyEvent.CTRL_MASK));
        assertEquals(InputEvent.ALT_DOWN_MASK,
                NativeKeyStrokes.toAwtModifiers(NativeKeyEvent.ALT_MASK));
        assertEquals(InputEvent.META_DOWN_MASK,
                NativeKeyStrokes.toAwtModifiers(NativeKeyEvent.META_MASK));
        assertEquals(InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
                NativeKeyStrokes.toAwtModifiers(
                        NativeKeyEvent.CTRL_MASK | NativeKeyEvent.SHIFT_MASK));
    }

    /**
     * The whole point: a native event must produce the same KeyStroke that the AWT path and
     * hotkeys.xml agree on, or a binding would work through one path and not the other.
     */
    @Test
    public void producesTheSameKeyStrokeAsTheAwtPath() {
        NativeKeyEvent event = nativeKeyEvent(NativeKeyEvent.VC_A,
                NativeKeyEvent.CTRL_MASK | NativeKeyEvent.SHIFT_MASK);

        KeyStroke keyStroke = NativeKeyStrokes.toKeyStroke(event);

        assertNotNull(keyStroke);
        assertEquals(KeyStroke.getKeyStroke("shift ctrl pressed A"), keyStroke);
        assertEquals("shift ctrl pressed A", keyStroke.toString());
    }

    @Test
    public void bareFunctionKeyProducesBareKeyStroke() {
        KeyStroke keyStroke = NativeKeyStrokes.toKeyStroke(nativeKeyEvent(NativeKeyEvent.VC_F13, 0));

        assertEquals(KeyStroke.getKeyStroke("pressed F13"), keyStroke);
    }

    @Test
    public void unmappedKeyYieldsNullKeyStroke() {
        assertNull(NativeKeyStrokes.toKeyStroke(nativeKeyEvent(NativeKeyEvent.VC_VOLUME_UP, 0)));
    }

    /**
     * Guards against the modifier masks being confused for one another, which would silently
     * misroute every shortcut.
     */
    @Test
    public void modifierMasksAreDistinct() {
        assertNotEquals(NativeKeyStrokes.toAwtModifiers(NativeKeyEvent.CTRL_MASK),
                NativeKeyStrokes.toAwtModifiers(NativeKeyEvent.SHIFT_MASK));
        assertNotEquals(NativeKeyStrokes.toAwtModifiers(NativeKeyEvent.ALT_MASK),
                NativeKeyStrokes.toAwtModifiers(NativeKeyEvent.META_MASK));
    }
}
