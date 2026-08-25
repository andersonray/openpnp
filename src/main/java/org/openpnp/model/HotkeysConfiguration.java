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

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * The user's keyboard shortcut configuration, persisted as hotkeys.xml in the configuration
 * directory.
 * <p>
 * The binding list is a flat list of (action, keystroke) pairs rather than a map of action to
 * keystroke, because one action may legitimately have several keystrokes bound to it. The jog
 * actions rely on this: they are bound to both Ctrl and Ctrl+Shift so that JogControlsPanel can
 * read the shift state separately to select the coarse jog speed.
 * <p>
 * A configuration that has been loaded from disk is authoritative and complete. An action with no
 * binding row is unbound, and defaults are deliberately not merged in, so that a shortcut the user
 * has cleared stays cleared across restarts.
 */
@Root(name = "openpnp-hotkeys")
public class HotkeysConfiguration {
    /**
     * Format version, so that a future change to the file layout has a migration lever. Written
     * always, currently only read for logging.
     */
    @Attribute(required = false)
    private int version = 1;

    /**
     * Whether shortcuts should also fire while OpenPnP does not have keyboard focus, using a native
     * global keyboard hook. Off by default: the hook needs additional permissions on some
     * platforms, and cannot prevent the focused application from also receiving the keys.
     */
    @Attribute(required = false)
    private boolean globalHookEnabled = false;

    @ElementList(inline = true, entry = "binding", required = false)
    private ArrayList<HotkeyBinding> bindings = new ArrayList<>();

    public HotkeysConfiguration() {
    }

    public HotkeysConfiguration(List<HotkeyBinding> bindings) {
        this.bindings = new ArrayList<>(bindings);
    }

    public int getVersion() {
        return version;
    }

    public boolean isGlobalHookEnabled() {
        return globalHookEnabled;
    }

    public void setGlobalHookEnabled(boolean globalHookEnabled) {
        this.globalHookEnabled = globalHookEnabled;
    }

    /**
     * @return the live binding list. Never null, but may be empty, which means every shortcut is
     *         switched off.
     */
    public List<HotkeyBinding> getBindings() {
        if (bindings == null) {
            // simple-xml leaves an inline ElementList null when no entries are present.
            bindings = new ArrayList<>();
        }
        return bindings;
    }

    public void setBindings(List<HotkeyBinding> bindings) {
        this.bindings = new ArrayList<>(bindings);
    }
}
