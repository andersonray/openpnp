package org.openpnp.gui.hotkeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.model.HotkeyBinding;

public class HotkeyDispatcherTest {

    private static final String STOP = "job.stop";
    private static final KeyStroke CTRL_SHIFT_A = KeyStroke.getKeyStroke("shift ctrl pressed A");
    private static final KeyStroke CTRL_SHIFT_B = KeyStroke.getKeyStroke("shift ctrl pressed B");

    /** Counts invocations so we can assert an action fired exactly once. */
    private static class CountingAction extends AbstractAction {
        private int count;

        @Override
        public void actionPerformed(ActionEvent e) {
            count++;
        }
    }

    private Map<String, Action> actions;
    private CountingAction stopAction;
    private boolean textInputFocused;
    private HotkeyDispatcher dispatcher;

    @BeforeEach
    public void setUp() {
        actions = new HashMap<>();
        stopAction = new CountingAction();
        actions.put(STOP, stopAction);
        textInputFocused = false;
        dispatcher = new HotkeyDispatcher(actions::get, () -> textInputFocused);
        dispatcher.setBindings(Arrays.asList(new HotkeyBinding(STOP, CTRL_SHIFT_A)));
    }

    private KeyEvent keyEvent(int id, int keyCode, int modifiers) {
        return new KeyEvent(new JPanel(), id, 0L, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    private KeyEvent ctrlShiftA(int id) {
        return keyEvent(id, KeyEvent.VK_A,
                InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
    }

    @Test
    public void firesBoundEnabledAction() {
        assertTrue(dispatcher.handleKeyStroke(CTRL_SHIFT_A));
        assertEquals(1, stopAction.count);
    }

    /**
     * The enabled guard is what makes a Stop button harmless when no job is running, so it is
     * load bearing rather than cosmetic.
     */
    @Test
    public void doesNotFireDisabledAction() {
        stopAction.setEnabled(false);

        assertFalse(dispatcher.handleKeyStroke(CTRL_SHIFT_A));
        assertEquals(0, stopAction.count);
    }

    @Test
    public void ignoresUnboundKeyStroke() {
        assertFalse(dispatcher.handleKeyStroke(CTRL_SHIFT_B));
        assertEquals(0, stopAction.count);
    }

    @Test
    public void ignoresNullKeyStroke() {
        assertFalse(dispatcher.handleKeyStroke(null));
    }

    @Test
    public void doesNotFireWhileSuspended() {
        dispatcher.setSuspended(true);

        assertFalse(dispatcher.handleKeyStroke(CTRL_SHIFT_A));
        assertEquals(0, stopAction.count);

        dispatcher.setSuspended(false);
        assertTrue(dispatcher.handleKeyStroke(CTRL_SHIFT_A));
        assertEquals(1, stopAction.count);
    }

    /**
     * A configuration written by a newer OpenPnP may name actions this version does not have.
     */
    @Test
    public void unknownActionIdIsIgnored() {
        dispatcher.setBindings(Arrays.asList(new HotkeyBinding("no.such.action", CTRL_SHIFT_A)));

        assertFalse(dispatcher.handleKeyStroke(CTRL_SHIFT_A));
    }

    @Test
    public void unparseableKeyStrokeIsIgnored() {
        dispatcher.setBindings(Arrays.asList(new HotkeyBinding(STOP, "not a keystroke")));

        assertNull(dispatcher.getActionId(CTRL_SHIFT_A));
        assertFalse(dispatcher.handleKeyStroke(CTRL_SHIFT_A));
    }

    @Test
    public void nullBindingListClearsEverything() {
        dispatcher.setBindings(null);

        assertFalse(dispatcher.handleKeyStroke(CTRL_SHIFT_A));
    }

    @Test
    public void duplicateKeyStrokeResolvesToLastBinding() {
        CountingAction other = new CountingAction();
        actions.put("other", other);
        dispatcher.setBindings(Arrays.asList(new HotkeyBinding(STOP, CTRL_SHIFT_A),
                new HotkeyBinding("other", CTRL_SHIFT_A)));

        assertEquals("other", dispatcher.getActionId(CTRL_SHIFT_A));
        assertTrue(dispatcher.handleKeyStroke(CTRL_SHIFT_A));
        assertEquals(0, stopAction.count);
        assertEquals(1, other.count);
    }

    @Test
    public void awtKeyPressedFires() {
        assertTrue(dispatcher.handleAwtKeyEvent(ctrlShiftA(KeyEvent.KEY_PRESSED)));
        assertEquals(1, stopAction.count);
    }

    /**
     * One physical keypress produces KEY_PRESSED, KEY_TYPED and KEY_RELEASED. Only the first may
     * be acted on, or a single press could fire the action more than once.
     */
    @Test
    public void awtKeyReleasedAndTypedDoNotFire() {
        assertFalse(dispatcher.handleAwtKeyEvent(ctrlShiftA(KeyEvent.KEY_RELEASED)));
        assertFalse(dispatcher.handleAwtKeyEvent(
                new KeyEvent(new JPanel(), KeyEvent.KEY_TYPED, 0L,
                        InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
                        KeyEvent.VK_UNDEFINED, 'A')));
        assertEquals(0, stopAction.count);
    }

    @Test
    public void wholeKeyPressSequenceFiresExactlyOnce() {
        dispatcher.handleAwtKeyEvent(ctrlShiftA(KeyEvent.KEY_PRESSED));
        dispatcher.handleAwtKeyEvent(new KeyEvent(new JPanel(), KeyEvent.KEY_TYPED, 0L,
                InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_UNDEFINED, 'A'));
        dispatcher.handleAwtKeyEvent(ctrlShiftA(KeyEvent.KEY_RELEASED));

        assertEquals(1, stopAction.count);
    }

    @Test
    public void textInputFocusSuppressesAwtPath() {
        textInputFocused = true;

        assertFalse(dispatcher.handleAwtKeyEvent(ctrlShiftA(KeyEvent.KEY_PRESSED)));
        assertEquals(0, stopAction.count);
    }

    /**
     * The native hook path applies its own focus rules, so the text input guard must not be baked
     * into handleKeyStroke.
     */
    @Test
    public void textInputFocusDoesNotSuppressDirectKeyStroke() {
        textInputFocused = true;

        assertTrue(dispatcher.handleKeyStroke(CTRL_SHIFT_A));
        assertEquals(1, stopAction.count);
    }
}
