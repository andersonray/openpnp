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

import javax.swing.KeyStroke;

/**
 * Formats keystrokes the way a user expects to read them, for example {@code Ctrl+Shift+R}, rather
 * than in the {@code shift ctrl pressed R} form that is written to hotkeys.xml.
 */
public class HotkeyText {

    private HotkeyText() {
    }

    /**
     * @param keyStroke may be null.
     * @return a display string, or an empty string if the keystroke is null.
     */
    public static String toDisplayString(KeyStroke keyStroke) {
        if (keyStroke == null) {
            return ""; //$NON-NLS-1$
        }
        String modifiers = toDisplayString(keyStroke.getModifiers());
        String key = KeyEvent.getKeyText(keyStroke.getKeyCode());
        return modifiers.isEmpty() ? key : modifiers + key;
    }

    /**
     * @return the modifiers, each followed by a plus sign, so that a key name can be appended
     *         directly. Empty if there are no modifiers.
     */
    public static String toDisplayString(int modifiers) {
        StringBuilder text = new StringBuilder();
        // Fixed order, so that the same combination always reads the same way. This deliberately
        // does not use KeyEvent.getKeyModifiersText, which joins with the platform separator and
        // orders differently.
        appendModifier(text, modifiers, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_CONTROL);
        appendModifier(text, modifiers, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_ALT);
        appendModifier(text, modifiers, InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_SHIFT);
        appendModifier(text, modifiers, InputEvent.META_DOWN_MASK, KeyEvent.VK_META);
        appendModifier(text, modifiers, InputEvent.ALT_GRAPH_DOWN_MASK, KeyEvent.VK_ALT_GRAPH);
        return text.toString();
    }

    private static void appendModifier(StringBuilder text, int modifiers, int mask, int keyCode) {
        if ((modifiers & mask) != 0) {
            text.append(KeyEvent.getKeyText(keyCode)).append('+');
        }
    }
}
