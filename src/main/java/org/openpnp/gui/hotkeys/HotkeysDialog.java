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

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.MenuElement;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;

import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.model.HotkeyBinding;
import org.openpnp.model.HotkeysConfiguration;
import org.openpnp.util.UiUtils;
import org.pmw.tinylog.Logger;

/**
 * Lets the user bind keyboard shortcuts to OpenPnP actions.
 * <p>
 * The dialog is modal and suspends hotkey dispatch for as long as it is open, so that pressing a
 * bound key in order to rebind it cannot also trigger it. Without that, rebinding Stop Job would
 * stop the job.
 */
@SuppressWarnings("serial")
public class HotkeysDialog extends JDialog {
    private final List<HotkeyActionDescriptor> rows =
            new ArrayList<>(HotkeyActions.getAll());

    /**
     * A working copy, so that Cancel really discards.
     */
    private final List<HotkeyBinding> bindings = new ArrayList<>();

    private final HotkeysTableModel tableModel = new HotkeysTableModel();
    private final JTable table = new JTable(tableModel);
    private final JCheckBox globalHookCheckBox =
            new JCheckBox(Translations.getString("Hotkeys.Dialog.GlobalHook.Enable")); //$NON-NLS-1$

    public static void showDialog(Window parent) {
        HotkeysDialog dialog = new HotkeysDialog(parent);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private HotkeysDialog(Window parent) {
        super(parent, Translations.getString("Hotkeys.Dialog.Title"), //$NON-NLS-1$
                Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        HotkeysConfiguration configuration = Configuration.get().getHotkeys();
        if (configuration == null) {
            configuration = HotkeyActions.defaultConfiguration();
        }
        bindings.addAll(configuration.getBindings());
        globalHookCheckBox.setSelected(configuration.isGlobalHookEnabled());

        buildContent();

        // Suspend dispatch for the lifetime of the dialog.
        setDispatcherSuspended(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                setDispatcherSuspended(false);
            }
        });

        setMinimumSize(new Dimension(640, 480));
        pack();
    }

    private void setDispatcherSuspended(boolean suspended) {
        HotkeyDispatcher dispatcher = MainFrame.get() == null ? null
                : MainFrame.get().getHotkeyDispatcher();
        if (dispatcher != null) {
            dispatcher.setSuspended(suspended);
        }
    }

    private void buildContent() {
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setContentPane(content);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (tableModel.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
            // Otherwise the viewport can come up scrolled to the end of the list.
            table.scrollRectToVisible(table.getCellRect(0, 0, true));
        }
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setPreferredWidth(240);
        // Show a useful number of rows without the user having to resize the dialog first.
        table.setPreferredScrollableViewportSize(new Dimension(560, 18 * table.getRowHeight()));
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        content.add(buildSideButtons(), BorderLayout.EAST);
        content.add(buildBottom(), BorderLayout.SOUTH);
    }

    private JPanel buildSideButtons() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JButton(setAction));
        panel.add(Box.createVerticalStrut(4));
        panel.add(new JButton(addAction));
        panel.add(Box.createVerticalStrut(4));
        panel.add(new JButton(clearAction));
        panel.add(Box.createVerticalStrut(12));
        panel.add(new JButton(restoreRowDefaultAction));
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildBottom() {
        JPanel globalHookPanel = new JPanel(new BorderLayout(4, 4));
        globalHookPanel.setBorder(BorderFactory.createTitledBorder(
                Translations.getString("Hotkeys.Dialog.GlobalHook.Title"))); //$NON-NLS-1$
        globalHookPanel.add(globalHookCheckBox, BorderLayout.NORTH);
        JLabel warning =
                new JLabel(Translations.getString("Hotkeys.Dialog.GlobalHook.Warning")); //$NON-NLS-1$
        warning.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
        globalHookPanel.add(warning, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(new JButton(restoreAllDefaultsAction));
        buttons.add(Box.createHorizontalStrut(16));
        buttons.add(new JButton(okAction));
        buttons.add(new JButton(cancelAction));

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(globalHookPanel, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.SOUTH);
        return bottom;
    }

    private HotkeyActionDescriptor getSelectedDescriptor() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rows.get(table.convertRowIndexToModel(row));
    }

    private List<KeyStroke> keyStrokesFor(String actionId) {
        List<KeyStroke> keyStrokes = new ArrayList<>();
        for (HotkeyBinding binding : bindings) {
            if (binding.getAction().equals(actionId)) {
                KeyStroke keyStroke = binding.toKeyStroke();
                if (keyStroke != null) {
                    keyStrokes.add(keyStroke);
                }
            }
        }
        return keyStrokes;
    }

    private void removeBindingsFor(String actionId) {
        bindings.removeIf(binding -> binding.getAction().equals(actionId));
    }

    private String findActionBoundTo(KeyStroke keyStroke) {
        for (HotkeyBinding binding : bindings) {
            if (keyStroke.equals(binding.toKeyStroke())) {
                return binding.getAction();
            }
        }
        return null;
    }

    /**
     * Prompts for a keystroke, resolves any conflict, then adds it to the selected action.
     *
     * @param replace true to drop the action's existing shortcuts first.
     */
    private void captureInto(HotkeyActionDescriptor descriptor, boolean replace) {
        JDialog capture = new JDialog(this, Translations.getString("Hotkeys.Capture.Title"), //$NON-NLS-1$
                Dialog.ModalityType.APPLICATION_MODAL);
        capture.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        KeyStroke[] captured = new KeyStroke[1];
        KeyStrokeCaptureField field = new KeyStrokeCaptureField(new KeyStrokeCaptureField.CaptureListener() {
            @Override
            public void captured(KeyStroke keyStroke) {
                captured[0] = keyStroke;
                capture.dispose();
            }

            @Override
            public void cancelled() {
                capture.dispose();
            }
        });

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JLabel(String.format(
                Translations.getString("Hotkeys.Capture.Message"), descriptor.getName())), //$NON-NLS-1$
                BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        capture.setContentPane(panel);
        capture.pack();
        capture.setLocationRelativeTo(this);
        field.requestFocusInWindow();
        capture.setVisible(true);

        if (captured[0] == null) {
            return;
        }
        if (!resolveConflict(captured[0], descriptor)) {
            return;
        }
        if (replace) {
            removeBindingsFor(descriptor.getId());
        }
        else if (keyStrokesFor(descriptor.getId()).contains(captured[0])) {
            // Already bound to this very action; nothing to do.
            return;
        }
        bindings.add(new HotkeyBinding(descriptor.getId(), captured[0]));
        warnIfShadowsMenuAccelerator(captured[0]);
        tableModel.fireTableDataChanged();
        selectRowFor(descriptor);
    }

    /**
     * @return true to go ahead with the assignment.
     */
    private boolean resolveConflict(KeyStroke keyStroke, HotkeyActionDescriptor descriptor) {
        String otherId = findActionBoundTo(keyStroke);
        if (otherId == null || otherId.equals(descriptor.getId())) {
            return true;
        }
        HotkeyActionDescriptor other = HotkeyActions.get(otherId);
        String otherName = other == null ? otherId : other.getName();
        int result = JOptionPane.showConfirmDialog(this,
                String.format(Translations.getString("Hotkeys.Dialog.Conflict.Message"), //$NON-NLS-1$
                        HotkeyText.toDisplayString(keyStroke), otherName),
                Translations.getString("Hotkeys.Dialog.Conflict.Title"), //$NON-NLS-1$
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return false;
        }
        bindings.removeIf(binding -> keyStroke.equals(binding.toKeyStroke()));
        return true;
    }

    /**
     * A hotkey takes priority over a menu accelerator, because the hotkey dispatcher runs before
     * normal event dispatch. That is easy to trip over, so say so rather than silently shadowing
     * for example Ctrl+S.
     */
    private void warnIfShadowsMenuAccelerator(KeyStroke keyStroke) {
        String menuText = findMenuAccelerator(keyStroke);
        if (menuText == null) {
            return;
        }
        JOptionPane.showMessageDialog(this,
                String.format(Translations.getString("Hotkeys.Dialog.MenuConflict.Message"), //$NON-NLS-1$
                        HotkeyText.toDisplayString(keyStroke), menuText),
                Translations.getString("Hotkeys.Dialog.MenuConflict.Title"), //$NON-NLS-1$
                JOptionPane.INFORMATION_MESSAGE);
    }

    private String findMenuAccelerator(KeyStroke keyStroke) {
        MainFrame mainFrame = MainFrame.get();
        if (mainFrame == null || mainFrame.getJMenuBar() == null) {
            return null;
        }
        return findMenuAccelerator(mainFrame.getJMenuBar(), keyStroke, new HashSet<>());
    }

    private String findMenuAccelerator(MenuElement element, KeyStroke keyStroke,
            Set<MenuElement> visited) {
        if (!visited.add(element)) {
            return null;
        }
        if (element instanceof JMenuItem) {
            JMenuItem item = (JMenuItem) element;
            if (keyStroke.equals(item.getAccelerator())) {
                return item.getText();
            }
        }
        for (MenuElement child : element.getSubElements()) {
            String found = findMenuAccelerator(child, keyStroke, visited);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void selectRowFor(HotkeyActionDescriptor descriptor) {
        int index = rows.indexOf(descriptor);
        if (index >= 0) {
            int viewIndex = table.convertRowIndexToView(index);
            table.setRowSelectionInterval(viewIndex, viewIndex);
        }
    }

    private final AbstractAction setAction =
            new AbstractAction(Translations.getString("Hotkeys.Dialog.Action.Set")) { //$NON-NLS-1$
                @Override
                public void actionPerformed(ActionEvent e) {
                    HotkeyActionDescriptor descriptor = getSelectedDescriptor();
                    if (descriptor != null) {
                        captureInto(descriptor, true);
                    }
                }
            };

    private final AbstractAction addAction =
            new AbstractAction(Translations.getString("Hotkeys.Dialog.Action.Add")) { //$NON-NLS-1$
                @Override
                public void actionPerformed(ActionEvent e) {
                    HotkeyActionDescriptor descriptor = getSelectedDescriptor();
                    if (descriptor != null) {
                        captureInto(descriptor, false);
                    }
                }
            };

    private final AbstractAction clearAction =
            new AbstractAction(Translations.getString("Hotkeys.Dialog.Action.Clear")) { //$NON-NLS-1$
                @Override
                public void actionPerformed(ActionEvent e) {
                    HotkeyActionDescriptor descriptor = getSelectedDescriptor();
                    if (descriptor != null) {
                        removeBindingsFor(descriptor.getId());
                        tableModel.fireTableDataChanged();
                        selectRowFor(descriptor);
                    }
                }
            };

    private final AbstractAction restoreRowDefaultAction =
            new AbstractAction(Translations.getString("Hotkeys.Dialog.Action.RestoreDefault")) { //$NON-NLS-1$
                @Override
                public void actionPerformed(ActionEvent e) {
                    HotkeyActionDescriptor descriptor = getSelectedDescriptor();
                    if (descriptor == null) {
                        return;
                    }
                    removeBindingsFor(descriptor.getId());
                    for (String keyStroke : descriptor.getDefaultKeyStrokes()) {
                        KeyStroke parsed = KeyStroke.getKeyStroke(keyStroke);
                        if (parsed != null && resolveConflict(parsed, descriptor)) {
                            bindings.add(new HotkeyBinding(descriptor.getId(), keyStroke));
                        }
                    }
                    tableModel.fireTableDataChanged();
                    selectRowFor(descriptor);
                }
            };

    private final AbstractAction restoreAllDefaultsAction =
            new AbstractAction(Translations.getString("Hotkeys.Dialog.Action.RestoreAllDefaults")) { //$NON-NLS-1$
                @Override
                public void actionPerformed(ActionEvent e) {
                    int result = JOptionPane.showConfirmDialog(HotkeysDialog.this,
                            Translations.getString("Hotkeys.Dialog.RestoreAllDefaults.Message"), //$NON-NLS-1$
                            Translations.getString("Hotkeys.Dialog.Action.RestoreAllDefaults"), //$NON-NLS-1$
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.YES_OPTION) {
                        return;
                    }
                    bindings.clear();
                    bindings.addAll(HotkeyActions.defaultBindings());
                    tableModel.fireTableDataChanged();
                }
            };

    private final AbstractAction okAction =
            new AbstractAction(Translations.getString("General.Ok")) { //$NON-NLS-1$
                @Override
                public void actionPerformed(ActionEvent e) {
                    apply();
                }
            };

    private final AbstractAction cancelAction =
            new AbstractAction(Translations.getString("General.Cancel")) { //$NON-NLS-1$
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            };

    private void apply() {
        HotkeysConfiguration configuration = Configuration.get().getHotkeys();
        if (configuration == null) {
            configuration = new HotkeysConfiguration();
            Configuration.get().setHotkeys(configuration);
        }
        configuration.setBindings(bindings);
        configuration.setGlobalHookEnabled(globalHookCheckBox.isSelected());

        HotkeyDispatcher dispatcher = MainFrame.get().getHotkeyDispatcher();
        if (dispatcher != null) {
            dispatcher.setBindings(configuration.getBindings());
        }

        applyGlobalHook(configuration);

        try {
            Configuration.get().saveHotkeys();
        }
        catch (Exception e) {
            Logger.error(e, "Could not save hotkeys.xml");
            MessageBoxes.errorBox(this,
                    Translations.getString("Hotkeys.Dialog.SaveError.Title"), e); //$NON-NLS-1$
        }
        dispose();
    }

    private void applyGlobalHook(HotkeysConfiguration configuration) {
        if (!configuration.isGlobalHookEnabled()) {
            GlobalHotkeyHook.getInstance().disable();
            return;
        }
        if (GlobalHotkeyHook.getInstance().enable()) {
            return;
        }
        // Enabling failed, most likely because the platform needs permission we do not have.
        // Report it and turn the setting back off so the UI does not claim something untrue.
        configuration.setGlobalHookEnabled(false);
        globalHookCheckBox.setSelected(false);
        JOptionPane.showMessageDialog(this,
                Translations.getString("Hotkeys.Dialog.GlobalHook.Failed"), //$NON-NLS-1$
                Translations.getString("Hotkeys.Dialog.GlobalHook.Title"), //$NON-NLS-1$
                JOptionPane.WARNING_MESSAGE);
    }

    private class HotkeysTableModel extends AbstractTableModel {
        private final String[] columns = {
                Translations.getString("Hotkeys.Dialog.Column.Group"), //$NON-NLS-1$
                Translations.getString("Hotkeys.Dialog.Column.Action"), //$NON-NLS-1$
                Translations.getString("Hotkeys.Dialog.Column.Shortcut"), //$NON-NLS-1$
        };

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            HotkeyActionDescriptor descriptor = rows.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return descriptor.getGroup();
                case 1:
                    return descriptor.getName();
                case 2:
                    return formatKeyStrokes(descriptor.getId());
                default:
                    return null;
            }
        }

        private String formatKeyStrokes(String actionId) {
            StringBuilder text = new StringBuilder();
            for (KeyStroke keyStroke : keyStrokesFor(actionId)) {
                if (text.length() > 0) {
                    text.append(", "); //$NON-NLS-1$
                }
                text.append(HotkeyText.toDisplayString(keyStroke));
            }
            return text.toString();
        }
    }
}
