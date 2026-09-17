package com.termux.terminal;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import static org.junit.Assert.*;

public class TerminalTransportTest {
    @Test public void remoteSessionHasNoLocalPidAndCanCloseBeforeViewAttachment() {
        boolean[] closed = {false};
        TerminalTransport transport = new TerminalTransport() {
            public InputStream input() { return new ByteArrayInputStream(new byte[0]); }
            public OutputStream output() { return new ByteArrayOutputStream(); }
            public void resize(int c, int r, int w, int h) {}
            public void close() { closed[0] = true; }
        };
        TerminalSessionClient client = (TerminalSessionClient) Proxy.newProxyInstance(
            TerminalSessionClient.class.getClassLoader(), new Class[]{TerminalSessionClient.class},
            (proxy, method, args) -> null);
        TerminalSession session = new TerminalSession(transport, 1000, client);
        assertEquals(0, session.getPid());
        assertNull(session.getCwd());
        assertTrue(session.isRunning());
        session.finishIfRunning();
        assertTrue(closed[0]);
        assertFalse(session.isRunning());
        session.cleanupResources(0); // Must not attempt JNI.close() for a remote terminal.
    }
}
