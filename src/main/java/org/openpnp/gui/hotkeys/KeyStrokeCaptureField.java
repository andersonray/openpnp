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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import org.openpnp.Translations;

/**
 * A field that captures the next keystroke the user presses, rather than letting them type into it.
 * <p>
 * Escape cancels. Pressing a modifier on its own does nothing, so the field waits until a real key
 * is pressed with whatever modifiers are held.
 */
@SuppressWarnings("serial")
public class KeyStrokeCaptureField extends JTextField {
    /**
     * Notified when the user has pressed a key, or has pressed Escape to cancel.
     */
    public interface CaptureListener {
        void captured(KeyStroke keyStroke);

        void cancelled();
    }

    private KeyStroke keyStroke;

    public KeyStrokeCaptureField(CaptureListener listener) {
        setEditable(false);
        setHorizontalAlignment(SwingConstants.CENTER);
        // Without this, Tab and Enter would be consumed by focus traversal and could never be
        // bound to anything.
        setFocusTraversalKeysEnabled(false);
        setText(Translations.getString("Hotkeys.Capture.Prompt")); //$NON-NLS-1$

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                e.consume();
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    listener.cancelled();
                    return;
                }
                if (isModifierOnly(e.getKeyCode())) {
                    // Wait for a real key. Show the modifiers held so far as feedback.
                    setText(HotkeyText.toDisplayString(e.getModifiersEx()));
                    return;
                }
                keyStroke = KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiersEx());
                setText(HotkeyText.toDisplayString(keyStroke));
                listener.captured(keyStroke);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                e.consume();
            }

            @Override
            public void keyTyped(KeyEvent e) {
                e.consume();
            }
        });
    }

    private static boolean isModifierOnly(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_SHIFT:
            case KeyEvent.VK_CONTROL:
            case KeyEvent.VK_ALT:
            case KeyEvent.VK_ALT_GRAPH:
            case KeyEvent.VK_META:
            case KeyEvent.VK_CAPS_LOCK:
            case KeyEvent.VK_NUM_LOCK:
            case KeyEvent.VK_SCROLL_LOCK:
                return true;
            default:
                return false;
        }
    }

    public KeyStroke getKeyStroke() {
        return keyStroke;
    }
}
