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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.Action;

import org.openpnp.Translations;

/**
 * Describes one action that the user can bind a keyboard shortcut to.
 * <p>
 * The Action itself is reached through a {@link Supplier} rather than held directly, because the
 * Actions live on GUI panels that are constructed inside the MainFrame constructor, long after this
 * registry is class-initialized. Resolving lazily also means the registry never has to be told when
 * a panel is rebuilt.
 */
public class HotkeyActionDescriptor {
    private final String id;
    private final String groupKey;
    private final String nameKey;
    private final List<String> defaultKeyStrokes;
    private final Supplier<Action> actionSupplier;

    /**
     * @param id stable identifier used as the persistence key. Never rename one of these: an id
     *        that changes silently unbinds the user's shortcut.
     * @param groupKey translation key for the group this action is shown under.
     * @param nameKey translation key for the display name.
     * @param actionSupplier resolves the Action, lazily. May return null if the GUI is not built.
     * @param defaultKeyStrokes default shortcuts, in canonical KeyStroke.toString() form. May be
     *        empty for an action that ships unbound.
     */
    public HotkeyActionDescriptor(String id, String groupKey, String nameKey,
            Supplier<Action> actionSupplier, String... defaultKeyStrokes) {
        this.id = id;
        this.groupKey = groupKey;
        this.nameKey = nameKey;
        this.actionSupplier = actionSupplier;
        this.defaultKeyStrokes =
                Collections.unmodifiableList(new ArrayList<>(Arrays.asList(defaultKeyStrokes)));
    }

    public String getId() {
        return id;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public String getNameKey() {
        return nameKey;
    }

    public String getGroup() {
        return Translations.getString(groupKey);
    }

    /**
     * @return the localized display name. This is deliberately not taken from the Action's NAME
     *         value, because JobPanel rewrites that between Start, Pause and Resume as the job
     *         state changes, which would make the shortcut table flicker.
     */
    public String getName() {
        return Translations.getString(nameKey);
    }

    public List<String> getDefaultKeyStrokes() {
        return defaultKeyStrokes;
    }

    /**
     * @return the Action, or null if the GUI has not been built yet.
     */
    public Action getAction() {
        return actionSupplier.get();
    }

    @Override
    public String toString() {
        return id;
    }
}
