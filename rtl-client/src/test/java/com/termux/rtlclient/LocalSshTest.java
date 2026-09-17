package com.termux.rtlclient;

import com.jcraft.jsch.HostKeyRepository;
import org.junit.Test;
import static org.junit.Assert.*;

public class LocalSshTest {
    @Test public void hostPinRequiresAnExactMatch() throws Exception {
        byte[] hostKey = "test-host-key".getBytes("UTF-8");
        String pin = LocalSsh.fingerprint(hostKey);
        assertTrue(LocalSsh.validFingerprint(pin));
        LocalSsh.PinnedHost repository = new LocalSsh.PinnedHost(pin);
        assertEquals(HostKeyRepository.OK, repository.check("[127.0.0.1]:8023", hostKey));
        assertEquals(HostKeyRepository.CHANGED, repository.check("[127.0.0.1]:8023", new byte[]{1, 2}));
    }
    @Test public void invalidAndEmptyPinsAreRejected() {
        assertFalse(LocalSsh.validFingerprint(""));
        assertFalse(LocalSsh.validFingerprint("SHA256:invalid"));
        assertFalse(LocalSsh.validFingerprint(null));
    }
    @Test public void setupIsLocalKeyOnlyAndDoesNotReplaceOriginalAuthorizedKeys() {
        String script = LocalSsh.setupScript("ssh-rsa AAAA termux-rtl-local");
        assertTrue(script.contains("ListenAddress 127.0.0.1"));
        assertTrue(script.contains("PasswordAuthentication no"));
        assertTrue(script.contains("AllowTcpForwarding no"));
        assertTrue(script.contains("termux-rtl-authorized_keys"));
        assertFalse(script.contains(".ssh/authorized_keys\""));
        assertFalse(script.contains("0.0.0.0"));
        assertFalse(script.contains("rm "));
    }
    @Test(expected = IllegalArgumentException.class) public void setupRejectsShellInjection() {
        LocalSsh.setupScript("ssh-rsa AAAA'; touch /tmp/unwanted; '");
    }
}
