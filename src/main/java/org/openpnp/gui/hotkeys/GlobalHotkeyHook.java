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

import java.awt.KeyboardFocusManager;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.pmw.tinylog.Logger;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

/**
 * An optional native keyboard hook, so that hotkeys also fire while OpenPnP does not have keyboard
 * focus. This is what lets a USB macropad or foot pedal at the machine work without first clicking
 * on the OpenPnP window.
 * <p>
 * Off by default, and every failure path degrades to AWT only dispatch rather than propagating.
 * <p>
 * This class is deliberately kept to plumbing. The key conversion lives in {@link NativeKeyStrokes}
 * and the decision of what to fire lives in {@link HotkeyDispatcher}, both of which are unit
 * tested; a real hook needs a real OS and real permissions and so cannot be.
 * <p>
 * Note that JNativeHook cannot reliably consume events, so a key bound here also reaches whichever
 * application currently has focus. That is why F13 to F24 are the recommended keys.
 */
public class GlobalHotkeyHook implements NativeKeyListener {
    private static final GlobalHotkeyHook instance = new GlobalHotkeyHook();

    private HotkeyDispatcher dispatcher;
    private boolean enabled;

    private GlobalHotkeyHook() {
    }

    public static GlobalHotkeyHook getInstance() {
        return instance;
    }

    public void setDispatcher(HotkeyDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Registers the native hook.
     *
     * @return true if the hook is now active. Never throws: a missing native library, a platform
     *         that will not allow the hook, or a denied permission all report false so the caller
     *         can fall back to AWT only hotkeys.
     */
    public synchronized boolean enable() {
        if (enabled) {
            return true;
        }
        try {
            silenceLogging();
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            enabled = true;
            Logger.info("Global hotkey hook enabled.");
        }
        catch (Throwable t) {
            // Catching Throwable on purpose: as well as NativeHookException this can fail with
            // UnsatisfiedLinkError, NoClassDefFoundError or SecurityException depending on the
            // platform and how OpenPnP was launched.
            Logger.warn(t, "Could not enable the global hotkey hook. Hotkeys will only work "
                    + "while OpenPnP has focus.");
            enabled = false;
        }
        return enabled;
    }

    public synchronized void disable() {
        if (!enabled) {
            return;
        }
        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
            Logger.info("Global hotkey hook disabled.");
        }
        catch (Throwable t) {
            Logger.warn(t, "Could not cleanly disable the global hotkey hook.");
        }
        finally {
            enabled = false;
        }
    }

    /**
     * JNativeHook logs through java.util.logging, whose default handler writes to the original
     * System.err. OpenPnP has already wrapped that to log at ERROR level, so without this every
     * keystroke would show up in the log as an error.
     */
    private static void silenceLogging() {
        java.util.logging.Logger logger =
                java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(java.util.logging.Level.OFF);
        logger.setUseParentHandlers(false);
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        HotkeyDispatcher currentDispatcher = dispatcher;
        if (currentDispatcher == null) {
            return;
        }
        KeyStroke keyStroke = NativeKeyStrokes.toKeyStroke(e);
        if (keyStroke == null) {
            return;
        }
        // This callback runs on JNativeHook's own thread. Everything below touches Swing and
        // submits machine tasks, so it has to happen on the event dispatch thread.
        SwingUtilities.invokeLater(() -> {
            // If any OpenPnP window is active then the AWT path has already seen this key, and
            // either fired it or deliberately suppressed it. Firing here as well would double it.
            // This check must be inside invokeLater: KeyboardFocusManager is per AppContext, so
            // asking from the hook thread would consult the wrong one.
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() != null) {
                return;
            }
            currentDispatcher.handleKeyStroke(keyStroke);
        });
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        // Hotkeys fire on press only, matching the AWT path.
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // Hotkeys fire on press only, matching the AWT path.
    }
}
