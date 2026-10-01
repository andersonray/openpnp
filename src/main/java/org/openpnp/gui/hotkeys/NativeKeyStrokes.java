/*
 * Copyright (C) 2026 ray <cpirius@gmail.com>
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

package org.openpnp.gui.hotkeys;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.KeyStroke;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

/**
 * Converts JNativeHook's native key events into AWT KeyStrokes, so that bindings mean the same
 * thing whether they arrive through AWT or through the global hook.
 * <p>
 * This class deliberately does not touch GlobalScreen, so loading it does not load the native
 * library. That keeps it unit testable on a headless build machine.
 * <p>
 * Two known limitations, both consequences of KeyStroke carrying no key location:
 * <ul>
 * <li>Numpad keys are reported by JNativeHook with the same virtual codes as their main keyboard
 * equivalents, so a numpad digit binds as the ordinary digit and numpad Enter as ordinary Enter.
 * <li>Left and right modifiers are not distinguished; both map to the same modifier mask.
 * </ul>
 */
public class NativeKeyStrokes {
    private static final Map<Integer, Integer> KEY_CODES;

    static {
        Map<Integer, Integer> map = new HashMap<>();

        // Letters. The native codes are scancode ordered rather than contiguous, so these cannot
        // be generated with a loop.
        map.put(NativeKeyEvent.VC_A, KeyEvent.VK_A);
        map.put(NativeKeyEvent.VC_B, KeyEvent.VK_B);
        map.put(NativeKeyEvent.VC_C, KeyEvent.VK_C);
        map.put(NativeKeyEvent.VC_D, KeyEvent.VK_D);
        map.put(NativeKeyEvent.VC_E, KeyEvent.VK_E);
        map.put(NativeKeyEvent.VC_F, KeyEvent.VK_F);
        map.put(NativeKeyEvent.VC_G, KeyEvent.VK_G);
        map.put(NativeKeyEvent.VC_H, KeyEvent.VK_H);
        map.put(NativeKeyEvent.VC_I, KeyEvent.VK_I);
        map.put(NativeKeyEvent.VC_J, KeyEvent.VK_J);
        map.put(NativeKeyEvent.VC_K, KeyEvent.VK_K);
        map.put(NativeKeyEvent.VC_L, KeyEvent.VK_L);
        map.put(NativeKeyEvent.VC_M, KeyEvent.VK_M);
        map.put(NativeKeyEvent.VC_N, KeyEvent.VK_N);
        map.put(NativeKeyEvent.VC_O, KeyEvent.VK_O);
        map.put(NativeKeyEvent.VC_P, KeyEvent.VK_P);
        map.put(NativeKeyEvent.VC_Q, KeyEvent.VK_Q);
        map.put(NativeKeyEvent.VC_R, KeyEvent.VK_R);
        map.put(NativeKeyEvent.VC_S, KeyEvent.VK_S);
        map.put(NativeKeyEvent.VC_T, KeyEvent.VK_T);
        map.put(NativeKeyEvent.VC_U, KeyEvent.VK_U);
        map.put(NativeKeyEvent.VC_V, KeyEvent.VK_V);
        map.put(NativeKeyEvent.VC_W, KeyEvent.VK_W);
        map.put(NativeKeyEvent.VC_X, KeyEvent.VK_X);
        map.put(NativeKeyEvent.VC_Y, KeyEvent.VK_Y);
        map.put(NativeKeyEvent.VC_Z, KeyEvent.VK_Z);

        // Digits.
        map.put(NativeKeyEvent.VC_0, KeyEvent.VK_0);
        map.put(NativeKeyEvent.VC_1, KeyEvent.VK_1);
        map.put(NativeKeyEvent.VC_2, KeyEvent.VK_2);
        map.put(NativeKeyEvent.VC_3, KeyEvent.VK_3);
        map.put(NativeKeyEvent.VC_4, KeyEvent.VK_4);
        map.put(NativeKeyEvent.VC_5, KeyEvent.VK_5);
        map.put(NativeKeyEvent.VC_6, KeyEvent.VK_6);
        map.put(NativeKeyEvent.VC_7, KeyEvent.VK_7);
        map.put(NativeKeyEvent.VC_8, KeyEvent.VK_8);
        map.put(NativeKeyEvent.VC_9, KeyEvent.VK_9);

        // Function keys. F13 to F24 are the range we recommend for a macropad at the machine,
        // because essentially nothing else binds them.
        map.put(NativeKeyEvent.VC_F1, KeyEvent.VK_F1);
        map.put(NativeKeyEvent.VC_F2, KeyEvent.VK_F2);
        map.put(NativeKeyEvent.VC_F3, KeyEvent.VK_F3);
        map.put(NativeKeyEvent.VC_F4, KeyEvent.VK_F4);
        map.put(NativeKeyEvent.VC_F5, KeyEvent.VK_F5);
        map.put(NativeKeyEvent.VC_F6, KeyEvent.VK_F6);
        map.put(NativeKeyEvent.VC_F7, KeyEvent.VK_F7);
        map.put(NativeKeyEvent.VC_F8, KeyEvent.VK_F8);
        map.put(NativeKeyEvent.VC_F9, KeyEvent.VK_F9);
        map.put(NativeKeyEvent.VC_F10, KeyEvent.VK_F10);
        map.put(NativeKeyEvent.VC_F11, KeyEvent.VK_F11);
        map.put(NativeKeyEvent.VC_F12, KeyEvent.VK_F12);
        map.put(NativeKeyEvent.VC_F13, KeyEvent.VK_F13);
        map.put(NativeKeyEvent.VC_F14, KeyEvent.VK_F14);
        map.put(NativeKeyEvent.VC_F15, KeyEvent.VK_F15);
        map.put(NativeKeyEvent.VC_F16, KeyEvent.VK_F16);
        map.put(NativeKeyEvent.VC_F17, KeyEvent.VK_F17);
        map.put(NativeKeyEvent.VC_F18, KeyEvent.VK_F18);
        map.put(NativeKeyEvent.VC_F19, KeyEvent.VK_F19);
        map.put(NativeKeyEvent.VC_F20, KeyEvent.VK_F20);
        map.put(NativeKeyEvent.VC_F21, KeyEvent.VK_F21);
        map.put(NativeKeyEvent.VC_F22, KeyEvent.VK_F22);
        map.put(NativeKeyEvent.VC_F23, KeyEvent.VK_F23);
        map.put(NativeKeyEvent.VC_F24, KeyEvent.VK_F24);

        // Arrows.
        map.put(NativeKeyEvent.VC_UP, KeyEvent.VK_UP);
        map.put(NativeKeyEvent.VC_DOWN, KeyEvent.VK_DOWN);
        map.put(NativeKeyEvent.VC_LEFT, KeyEvent.VK_LEFT);
        map.put(NativeKeyEvent.VC_RIGHT, KeyEvent.VK_RIGHT);

        // Navigation and editing.
        map.put(NativeKeyEvent.VC_ESCAPE, KeyEvent.VK_ESCAPE);
        map.put(NativeKeyEvent.VC_ENTER, KeyEvent.VK_ENTER);
        map.put(NativeKeyEvent.VC_SPACE, KeyEvent.VK_SPACE);
        map.put(NativeKeyEvent.VC_TAB, KeyEvent.VK_TAB);
        map.put(NativeKeyEvent.VC_BACKSPACE, KeyEvent.VK_BACK_SPACE);
        map.put(NativeKeyEvent.VC_INSERT, KeyEvent.VK_INSERT);
        map.put(NativeKeyEvent.VC_DELETE, KeyEvent.VK_DELETE);
        map.put(NativeKeyEvent.VC_HOME, KeyEvent.VK_HOME);
        map.put(NativeKeyEvent.VC_END, KeyEvent.VK_END);
        map.put(NativeKeyEvent.VC_PAGE_UP, KeyEvent.VK_PAGE_UP);
        map.put(NativeKeyEvent.VC_PAGE_DOWN, KeyEvent.VK_PAGE_DOWN);
        map.put(NativeKeyEvent.VC_PRINTSCREEN, KeyEvent.VK_PRINTSCREEN);
        map.put(NativeKeyEvent.VC_PAUSE, KeyEvent.VK_PAUSE);
        map.put(NativeKeyEvent.VC_CLEAR, KeyEvent.VK_CLEAR);

        // Punctuation. The first six are the keys the shipped defaults use for jogging Z and C and
        // for changing the jog increment, so without them those defaults would be unreachable
        // through the global hook.
        map.put(NativeKeyEvent.VC_QUOTE, KeyEvent.VK_QUOTE);
        map.put(NativeKeyEvent.VC_SLASH, KeyEvent.VK_SLASH);
        map.put(NativeKeyEvent.VC_COMMA, KeyEvent.VK_COMMA);
        map.put(NativeKeyEvent.VC_PERIOD, KeyEvent.VK_PERIOD);
        map.put(NativeKeyEvent.VC_MINUS, KeyEvent.VK_MINUS);
        map.put(NativeKeyEvent.VC_EQUALS, KeyEvent.VK_EQUALS);
        map.put(NativeKeyEvent.VC_SEMICOLON, KeyEvent.VK_SEMICOLON);
        map.put(NativeKeyEvent.VC_OPEN_BRACKET, KeyEvent.VK_OPEN_BRACKET);
        map.put(NativeKeyEvent.VC_CLOSE_BRACKET, KeyEvent.VK_CLOSE_BRACKET);
        map.put(NativeKeyEvent.VC_BACK_SLASH, KeyEvent.VK_BACK_SLASH);
        map.put(NativeKeyEvent.VC_BACKQUOTE, KeyEvent.VK_BACK_QUOTE);

        KEY_CODES = Collections.unmodifiableMap(map);
    }

    private NativeKeyStrokes() {
    }

    /**
     * @return the AWT key code, or {@link KeyEvent#VK_UNDEFINED} if this key has no AWT equivalent
     *         we support, for example a media or browser key.
     */
    public static int toAwtKeyCode(int nativeKeyCode) {
        return KEY_CODES.getOrDefault(nativeKeyCode, KeyEvent.VK_UNDEFINED);
    }

    /**
     * Translates the native modifier mask. The combined masks are used rather than the left and
     * right variants, because an AWT KeyStroke cannot express which side was pressed.
     */
    public static int toAwtModifiers(int nativeModifiers) {
        int modifiers = 0;
        if ((nativeModifiers & NativeKeyEvent.SHIFT_MASK) != 0) {
            modifiers |= InputEvent.SHIFT_DOWN_MASK;
        }
        if ((nativeModifiers & NativeKeyEvent.CTRL_MASK) != 0) {
            modifiers |= InputEvent.CTRL_DOWN_MASK;
        }
        if ((nativeModifiers & NativeKeyEvent.ALT_MASK) != 0) {
            modifiers |= InputEvent.ALT_DOWN_MASK;
        }
        if ((nativeModifiers & NativeKeyEvent.META_MASK) != 0) {
            modifiers |= InputEvent.META_DOWN_MASK;
        }
        return modifiers;
    }

    /**
     * @return the equivalent KeyStroke, or null if the key cannot be represented, in which case the
     *         caller should ignore the event.
     */
    public static KeyStroke toKeyStroke(NativeKeyEvent e) {
        int keyCode = toAwtKeyCode(e.getKeyCode());
        if (keyCode == KeyEvent.VK_UNDEFINED) {
            return null;
        }
        return KeyStroke.getKeyStroke(keyCode, toAwtModifiers(e.getModifiers()));
    }
}
