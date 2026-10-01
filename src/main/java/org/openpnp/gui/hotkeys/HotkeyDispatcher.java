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

import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import javax.swing.Action;
import javax.swing.KeyStroke;

import org.openpnp.model.HotkeyBinding;
import org.pmw.tinylog.Logger;

/**
 * Maps keystrokes to actions and fires them.
 * <p>
 * This deliberately knows nothing about MainFrame, AWT's EventQueue or the native global hook, so
 * that it can be unit tested headlessly. Both the AWT path and the native hook path funnel through
 * here, which is what keeps their behaviour identical.
 */
public class HotkeyDispatcher {
    private final Function<String, Action> actionResolver;
    private final BooleanSupplier textInputFocusedSupplier;

    /**
     * Read by the native hook thread and replaced wholesale by the event dispatch thread, never
     * mutated in place. Publishing an immutable map through a volatile field is enough to make that
     * safe without locking on the key path.
     */
    private volatile Map<KeyStroke, String> bindings = Collections.emptyMap();

    private volatile boolean suspended;

    /**
     * @param actionResolver maps an action id to its Action, typically HotkeyActions::getAction.
     *        May return null.
     * @param textInputFocusedSupplier true when a text field has focus, typically
     *        UiUtils::isTextInputFocused. Injected rather than called directly so that tests do not
     *        depend on KeyboardFocusManager state.
     */
    public HotkeyDispatcher(Function<String, Action> actionResolver,
            BooleanSupplier textInputFocusedSupplier) {
        this.actionResolver = actionResolver;
        this.textInputFocusedSupplier = textInputFocusedSupplier;
    }

    /**
     * Rebuilds the keystroke lookup. Bindings whose keystroke will not parse, and duplicate
     * keystrokes, are logged and dropped; the last binding for a given keystroke wins.
     */
    public void setBindings(List<HotkeyBinding> newBindings) {
        Map<KeyStroke, String> map = new HashMap<>();
        if (newBindings != null) {
            for (HotkeyBinding binding : newBindings) {
                KeyStroke keyStroke = binding.toKeyStroke();
                if (keyStroke == null) {
                    Logger.warn("Ignoring hotkey binding with unparseable keystroke: {}", binding);
                    continue;
                }
                String previous = map.put(keyStroke, binding.getAction());
                if (previous != null && !previous.equals(binding.getAction())) {
                    Logger.warn("Hotkey {} is bound to both {} and {}; using {}.", keyStroke,
                            previous, binding.getAction(), binding.getAction());
                }
            }
        }
        bindings = Collections.unmodifiableMap(map);
    }

    /**
     * @return the action id bound to the keystroke, or null.
     */
    public String getActionId(KeyStroke keyStroke) {
        return bindings.get(keyStroke);
    }

    public boolean isSuspended() {
        return suspended;
    }

    /**
     * Switches dispatch off. The shortcut editor holds this for as long as it is open, so that
     * pressing a bound key in order to rebind it cannot also trigger it. Without this, rebinding
     * Stop Job would stop the job.
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    /**
     * Fires the action bound to the given keystroke, if any.
     * <p>
     * Applies the enabled guard but not the text input guard, because the native hook path checks
     * focus separately. Must be called on the event dispatch thread.
     *
     * @return true if an action was fired, in which case the caller should consume the event.
     */
    public boolean handleKeyStroke(KeyStroke keyStroke) {
        if (suspended || keyStroke == null) {
            return false;
        }
        String actionId = bindings.get(keyStroke);
        if (actionId == null) {
            return false;
        }
        Action action = actionResolver.apply(actionId);
        if (action == null) {
            // Either an action id from a newer version of OpenPnP, or the GUI is not built yet.
            return false;
        }
        if (!action.isEnabled()) {
            // Job and machine actions disable themselves when they are not applicable, so this is
            // what makes "stop" a no-op when no job is running.
            return false;
        }
        action.actionPerformed(null);
        return true;
    }

    /**
     * Handles an AWT key event.
     * <p>
     * Only KEY_PRESSED is considered. KeyStroke.getKeyStrokeForEvent yields a keyChar based stroke
     * for KEY_TYPED and a release stroke for KEY_RELEASED, neither of which can match a binding, so
     * examining them only wasted work.
     *
     * @return true if an action was fired, in which case the caller should consume the event.
     */
    public boolean handleAwtKeyEvent(KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_PRESSED) {
            return false;
        }
        if (textInputFocusedSupplier.getAsBoolean()) {
            // Do not jog the machine while the user is editing a text field.
            return false;
        }
        return handleKeyStroke(KeyStroke.getKeyStrokeForEvent(e));
    }
}
