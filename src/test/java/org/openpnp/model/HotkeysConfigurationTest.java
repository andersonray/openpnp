package org.openpnp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;
import org.simpleframework.xml.Serializer;

/**
 * Verifies that HotkeysConfiguration round-trips through the same simple-xml serializer that writes
 * every other OpenPnP configuration file.
 */
public class HotkeysConfigurationTest {

    private String write(HotkeysConfiguration configuration) throws Exception {
        Serializer serializer = Configuration.createSerializer();
        StringWriter writer = new StringWriter();
        serializer.write(configuration, writer);
        return writer.toString();
    }

    private HotkeysConfiguration read(String xml) throws Exception {
        Serializer serializer = Configuration.createSerializer();
        return serializer.read(HotkeysConfiguration.class, new StringReader(xml));
    }

    @Test
    public void bindingsRoundTripInOrder() throws Exception {
        List<HotkeyBinding> bindings = Arrays.asList(
                new HotkeyBinding("job.stop", "shift ctrl pressed A"),
                new HotkeyBinding("jog.y-plus", "ctrl pressed UP"),
                new HotkeyBinding("jog.y-plus", "shift ctrl pressed UP"));

        HotkeysConfiguration read = read(write(new HotkeysConfiguration(bindings)));

        assertEquals(3, read.getBindings().size());
        for (int i = 0; i < bindings.size(); i++) {
            assertEquals(bindings.get(i).getAction(), read.getBindings().get(i).getAction());
            assertEquals(bindings.get(i).getKeyStroke(), read.getBindings().get(i).getKeyStroke());
        }
    }

    @Test
    public void globalHookFlagRoundTrips() throws Exception {
        HotkeysConfiguration configuration = new HotkeysConfiguration();
        configuration.setGlobalHookEnabled(true);

        assertTrue(read(write(configuration)).isGlobalHookEnabled());
    }

    @Test
    public void globalHookDefaultsToOff() throws Exception {
        assertFalse(new HotkeysConfiguration().isGlobalHookEnabled());
        assertFalse(read(write(new HotkeysConfiguration())).isGlobalHookEnabled());
    }

    /**
     * An empty binding list means "every shortcut is switched off". It must survive a round trip as
     * empty, and must not come back as null or as the defaults, or a user who cleared every
     * shortcut would get them all back on the next restart.
     */
    @Test
    public void emptyBindingListRoundTripsAsEmpty() throws Exception {
        HotkeysConfiguration read = read(write(new HotkeysConfiguration(new ArrayList<>())));

        assertNotNull(read.getBindings());
        assertTrue(read.getBindings().isEmpty());
    }

    @Test
    public void usesHyphenatedNames() throws Exception {
        HotkeysConfiguration configuration =
                new HotkeysConfiguration(Arrays.asList(new HotkeyBinding("job.stop", "shift ctrl pressed A")));
        configuration.setGlobalHookEnabled(true);

        String xml = write(configuration);

        assertTrue(xml.contains("<openpnp-hotkeys"), xml);
        assertTrue(xml.contains("global-hook-enabled=\"true\""), xml);
        assertTrue(xml.contains("key-stroke=\"shift ctrl pressed A\""), xml);
        assertTrue(xml.contains("action=\"job.stop\""), xml);
    }

    @Test
    public void toKeyStrokeParsesCanonicalForm() {
        HotkeyBinding binding = new HotkeyBinding("job.stop", "shift ctrl pressed A");

        assertEquals(KeyStroke.getKeyStroke("shift ctrl pressed A"), binding.toKeyStroke());
    }

    /**
     * The file is user editable, so an unparseable keystroke must yield null rather than throw.
     */
    @Test
    public void toKeyStrokeReturnsNullForGarbage() {
        assertNull(new HotkeyBinding("job.stop", "this is not a keystroke").toKeyStroke());
    }

    @Test
    public void keyStrokeConstructorStoresCanonicalForm() {
        KeyStroke keyStroke = KeyStroke.getKeyStroke("ctrl shift pressed R");

        // AWT always emits modifiers in the order shift ctrl meta alt altGraph, so the canonical
        // form is not the string that was parsed.
        assertEquals("shift ctrl pressed R", new HotkeyBinding("job.start", keyStroke).getKeyStroke());
    }
}
