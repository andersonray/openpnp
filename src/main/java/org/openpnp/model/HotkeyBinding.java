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

package org.openpnp.model;

import javax.swing.KeyStroke;

import org.simpleframework.xml.Attribute;

/**
 * A single binding of a keyboard shortcut to a named action.
 * <p>
 * The action is referenced by the stable String id of a HotkeyActionDescriptor rather than by the
 * Action instance, because the Action instances live on GUI panels that do not exist yet when the
 * configuration is deserialized.
 * <p>
 * The keystroke is stored in the canonical form produced by {@link KeyStroke#toString()}, for
 * example {@code shift ctrl pressed R}. Note that this form is canonical but not free: AWT always
 * emits modifiers in the order {@code shift ctrl meta alt altGraph}, so {@code ctrl shift pressed R}
 * parses correctly but is not what {@link KeyStroke#toString()} would produce. Always round-trip
 * through {@link KeyStroke} rather than comparing these strings directly.
 */
public class HotkeyBinding {
    /**
     * The id of the action this keystroke triggers. See HotkeyActions for the registry of valid
     * ids. An id that is not in the registry is ignored at load time rather than treated as an
     * error, so that a configuration written by a newer version of OpenPnP still loads.
     */
    @Attribute
    private String action;

    /**
     * The keystroke, in {@link KeyStroke#toString()} form.
     */
    @Attribute
    private String keyStroke;

    /**
     * Required by simple-xml.
     */
    @SuppressWarnings("unused")
    private HotkeyBinding() {
    }

    public HotkeyBinding(String action, String keyStroke) {
        this.action = action;
        this.keyStroke = keyStroke;
    }

    public HotkeyBinding(String action, KeyStroke keyStroke) {
        this(action, keyStroke.toString());
    }

    public String getAction() {
        return action;
    }

    public String getKeyStroke() {
        return keyStroke;
    }

    /**
     * @return the parsed keystroke, or null if it could not be parsed. Callers must handle null,
     *         because the file is user editable.
     */
    public KeyStroke toKeyStroke() {
        if (keyStroke == null) {
            return null;
        }
        return KeyStroke.getKeyStroke(keyStroke);
    }

    @Override
    public String toString() {
        return action + " = " + keyStroke;
    }
}
