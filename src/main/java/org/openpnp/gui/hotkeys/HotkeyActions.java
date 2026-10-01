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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.Action;

import org.openpnp.gui.JogControlsPanel;
import org.openpnp.gui.MainFrame;
import org.openpnp.model.HotkeyBinding;
import org.openpnp.model.HotkeysConfiguration;

/**
 * The registry of actions that can be bound to a keyboard shortcut.
 * <p>
 * Iteration order is registration order, which is also the order the shortcut table displays.
 * <p>
 * This is a registry with a {@link #register} method rather than an enum so that further actions,
 * for example running a named script through {@link org.openpnp.gui.ScriptAction}, can be added
 * without changing this class.
 */
public class HotkeyActions {
    public static final String GROUP_JOB = "Hotkeys.Group.Job"; //$NON-NLS-1$
    public static final String GROUP_MACHINE = "Hotkeys.Group.Machine"; //$NON-NLS-1$
    public static final String GROUP_NOZZLE = "Hotkeys.Group.Nozzle"; //$NON-NLS-1$
    public static final String GROUP_JOG = "Hotkeys.Group.Jog"; //$NON-NLS-1$

    private static final LinkedHashMap<String, HotkeyActionDescriptor> descriptors =
            new LinkedHashMap<>();

    static {
        registerBuiltIns();
    }

    private HotkeyActions() {
    }

    private static JogControlsPanel jogControls() {
        MainFrame mainFrame = MainFrame.get();
        if (mainFrame == null || mainFrame.getMachineControls() == null) {
            return null;
        }
        return mainFrame.getMachineControls().getJogControlsPanel();
    }

    private static Supplier<Action> job(Supplier<Action> supplier) {
        return () -> MainFrame.get() == null || MainFrame.get().getJobTab() == null ? null
                : supplier.get();
    }

    private static void registerBuiltIns() {
        // Job.
        register(new HotkeyActionDescriptor("job.start-pause-resume", GROUP_JOB, //$NON-NLS-1$
                "Hotkeys.Action.Job.StartPauseResume", //$NON-NLS-1$
                job(() -> MainFrame.get().getJobTab().startPauseResumeJobAction),
                "shift ctrl pressed R")); //$NON-NLS-1$
        register(new HotkeyActionDescriptor("job.step", GROUP_JOB, //$NON-NLS-1$
                "Hotkeys.Action.Job.Step", //$NON-NLS-1$
                job(() -> MainFrame.get().getJobTab().stepJobAction),
                "shift ctrl pressed S")); //$NON-NLS-1$
        register(new HotkeyActionDescriptor("job.stop", GROUP_JOB, //$NON-NLS-1$
                "Hotkeys.Action.Job.Stop", //$NON-NLS-1$
                job(() -> MainFrame.get().getJobTab().stopJobAction),
                "shift ctrl pressed A")); //$NON-NLS-1$

        // Machine.
        register(new HotkeyActionDescriptor("machine.home", GROUP_MACHINE, //$NON-NLS-1$
                "Hotkeys.Action.Machine.Home", //$NON-NLS-1$
                () -> MainFrame.get() == null || MainFrame.get().getMachineControls() == null ? null
                        : MainFrame.get().getMachineControls().homeAction,
                "ctrl pressed H", "shift ctrl pressed H")); //$NON-NLS-1$ //$NON-NLS-2$
        // Deliberately ships unbound. Adding a shortcut that stops the machine to every existing
        // user's configuration without them asking for it would be an unpleasant surprise.
        register(new HotkeyActionDescriptor("machine.toggle-enabled", GROUP_MACHINE, //$NON-NLS-1$
                "Hotkeys.Action.Machine.ToggleEnabled", //$NON-NLS-1$
                () -> MainFrame.get() == null || MainFrame.get().getMachineControls() == null ? null
                        : MainFrame.get().getMachineControls().startStopMachineAction));

        // Nozzle.
        register(jogAction("nozzle.park-xy", GROUP_NOZZLE, "Hotkeys.Action.Nozzle.ParkXy", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.xyParkAction, "shift ctrl pressed P")); //$NON-NLS-1$
        register(jogAction("nozzle.park-z", GROUP_NOZZLE, "Hotkeys.Action.Nozzle.ParkZ", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.zParkAction, "shift ctrl pressed L")); //$NON-NLS-1$
        register(jogAction("nozzle.safe-z", GROUP_NOZZLE, "Hotkeys.Action.Nozzle.SafeZ", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.safezAction, "shift ctrl pressed Z")); //$NON-NLS-1$
        register(jogAction("nozzle.discard", GROUP_NOZZLE, "Hotkeys.Action.Nozzle.Discard", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.discardAction, "shift ctrl pressed D")); //$NON-NLS-1$

        // Jog. Each jog action is bound twice, to Ctrl and to Ctrl+Shift, because
        // JogControlsPanel reads the shift state separately to select the coarse jog speed.
        // Removing either binding silently breaks coarse jogging.
        register(jogAction("jog.y-plus", GROUP_JOG, "Hotkeys.Action.Jog.YPlus", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.yPlusAction, "ctrl pressed UP", "shift ctrl pressed UP")); //$NON-NLS-1$ //$NON-NLS-2$
        register(jogAction("jog.y-minus", GROUP_JOG, "Hotkeys.Action.Jog.YMinus", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.yMinusAction, "ctrl pressed DOWN", "shift ctrl pressed DOWN")); //$NON-NLS-1$ //$NON-NLS-2$
        register(jogAction("jog.x-minus", GROUP_JOG, "Hotkeys.Action.Jog.XMinus", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.xMinusAction, "ctrl pressed LEFT", "shift ctrl pressed LEFT")); //$NON-NLS-1$ //$NON-NLS-2$
        register(jogAction("jog.x-plus", GROUP_JOG, "Hotkeys.Action.Jog.XPlus", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.xPlusAction, "ctrl pressed RIGHT", "shift ctrl pressed RIGHT")); //$NON-NLS-1$ //$NON-NLS-2$
        register(jogAction("jog.z-plus", GROUP_JOG, "Hotkeys.Action.Jog.ZPlus", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.zPlusAction, "ctrl pressed QUOTE", "shift ctrl pressed QUOTE")); //$NON-NLS-1$ //$NON-NLS-2$
        register(jogAction("jog.z-minus", GROUP_JOG, "Hotkeys.Action.Jog.ZMinus", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.zMinusAction, "ctrl pressed SLASH", "shift ctrl pressed SLASH")); //$NON-NLS-1$ //$NON-NLS-2$
        register(jogAction("jog.c-plus", GROUP_JOG, "Hotkeys.Action.Jog.CPlus", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.cPlusAction, "ctrl pressed COMMA", "shift ctrl pressed COMMA")); //$NON-NLS-1$ //$NON-NLS-2$
        register(jogAction("jog.c-minus", GROUP_JOG, "Hotkeys.Action.Jog.CMinus", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.cMinusAction, "ctrl pressed PERIOD", "shift ctrl pressed PERIOD")); //$NON-NLS-1$ //$NON-NLS-2$
        register(jogAction("jog.increment-lower", GROUP_JOG, "Hotkeys.Action.Jog.IncrementLower", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.lowerIncrementAction, "ctrl pressed MINUS", "shift ctrl pressed MINUS")); //$NON-NLS-1$ //$NON-NLS-2$
        register(jogAction("jog.increment-raise", GROUP_JOG, "Hotkeys.Action.Jog.IncrementRaise", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.raiseIncrementAction, "ctrl pressed EQUALS", "shift ctrl pressed EQUALS")); //$NON-NLS-1$ //$NON-NLS-2$
        register(jogAction("jog.increment-1", GROUP_JOG, "Hotkeys.Action.Jog.Increment1", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.setIncrement1Action, "shift ctrl pressed F1")); //$NON-NLS-1$
        register(jogAction("jog.increment-2", GROUP_JOG, "Hotkeys.Action.Jog.Increment2", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.setIncrement2Action, "shift ctrl pressed F2")); //$NON-NLS-1$
        register(jogAction("jog.increment-3", GROUP_JOG, "Hotkeys.Action.Jog.Increment3", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.setIncrement3Action, "shift ctrl pressed F3")); //$NON-NLS-1$
        register(jogAction("jog.increment-4", GROUP_JOG, "Hotkeys.Action.Jog.Increment4", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.setIncrement4Action, "shift ctrl pressed F4")); //$NON-NLS-1$
        register(jogAction("jog.increment-5", GROUP_JOG, "Hotkeys.Action.Jog.Increment5", //$NON-NLS-1$ //$NON-NLS-2$
                p -> p.setIncrement5Action, "shift ctrl pressed F5")); //$NON-NLS-1$
    }

    private static HotkeyActionDescriptor jogAction(String id, String groupKey, String nameKey,
            java.util.function.Function<JogControlsPanel, Action> accessor,
            String... defaultKeyStrokes) {
        return new HotkeyActionDescriptor(id, groupKey, nameKey, () -> {
            JogControlsPanel panel = jogControls();
            return panel == null ? null : accessor.apply(panel);
        }, defaultKeyStrokes);
    }

    /**
     * Adds a bindable action. Registering the same id twice replaces the earlier descriptor.
     */
    public static void register(HotkeyActionDescriptor descriptor) {
        descriptors.put(descriptor.getId(), descriptor);
    }

    /**
     * @return every bindable action, in registration order.
     */
    public static Collection<HotkeyActionDescriptor> getAll() {
        return Collections.unmodifiableCollection(descriptors.values());
    }

    /**
     * @return the descriptor for the given id, or null if it is not registered. A configuration
     *         written by a newer version of OpenPnP may well name actions this version does not
     *         have, so callers must handle null.
     */
    public static HotkeyActionDescriptor get(String id) {
        return descriptors.get(id);
    }

    /**
     * @return the Action for the given id, or null if the id is unknown or the GUI is not built.
     */
    public static Action getAction(String id) {
        HotkeyActionDescriptor descriptor = descriptors.get(id);
        return descriptor == null ? null : descriptor.getAction();
    }

    /**
     * @return the default bindings for every registered action. This is the single source of truth
     *         for both first run and the Restore Defaults button, which is why no default
     *         hotkeys.xml is shipped as a classpath resource.
     */
    public static HotkeysConfiguration defaultConfiguration() {
        return new HotkeysConfiguration(defaultBindings());
    }

    public static List<HotkeyBinding> defaultBindings() {
        List<HotkeyBinding> bindings = new ArrayList<>();
        for (HotkeyActionDescriptor descriptor : descriptors.values()) {
            for (String keyStroke : descriptor.getDefaultKeyStrokes()) {
                bindings.add(new HotkeyBinding(descriptor.getId(), keyStroke));
            }
        }
        return bindings;
    }
}
