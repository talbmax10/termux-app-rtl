package com.termux.rtlclient;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.termux.terminal.TerminalTransport;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

final class LocalSsh {
    static final int PORT = 8023;

    static String ensureKey(File directory) throws Exception {
        File privateKey = new File(directory, "ssh-rsa");
        File publicKey = new File(directory, "ssh-rsa.pub");
        if (!privateKey.isFile() || !publicKey.isFile()) {
            KeyPair key = KeyPair.genKeyPair(new JSch(), KeyPair.RSA, 3072);
            try {
                key.writePrivateKey(privateKey.getAbsolutePath());
                privateKey.setReadable(false, false);
                privateKey.setWritable(false, false);
                privateKey.setReadable(true, true);
                privateKey.setWritable(true, true);
                key.writePublicKey(publicKey.getAbsolutePath(), "termux-rtl-local");
            } finally { key.dispose(); }
        }
        return new String(Files.readAllBytes(publicKey.toPath()), StandardCharsets.UTF_8).trim();
    }

    static String fingerprint(byte[] key) throws Exception {
        return "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(key));
    }

    static boolean validFingerprint(String pin) {
        return pin != null && pin.matches("SHA256:[A-Za-z0-9+/]{43}");
    }

    static final class PinnedHost implements HostKeyRepository {
        private final String pin;
        PinnedHost(String pin) {
            if (!validFingerprint(pin)) throw new IllegalArgumentException("Invalid SHA256 host fingerprint");
            this.pin = pin;
        }
        public int check(String host, byte[] key) {
            try {
                return MessageDigest.isEqual(pin.getBytes(StandardCharsets.US_ASCII),
                    fingerprint(key).getBytes(StandardCharsets.US_ASCII)) ? OK : CHANGED;
            } catch (Exception e) { return CHANGED; }
        }
        public void add(HostKey key, UserInfo info) { throw new SecurityException("Host key must be pinned explicitly"); }
        public void remove(String host, String type) {}
        public void remove(String host, String type, byte[] key) {}
        public String getKnownHostsRepositoryID() { return "User-verified local Termux host fingerprint"; }
        public HostKey[] getHostKey() { return new HostKey[0]; }
        public HostKey[] getHostKey(String host, String type) { return new HostKey[0]; }
    }

    static TerminalTransport connect(File directory, String username, String pin) throws Exception {
        JSch jsch = new JSch();
        jsch.setHostKeyRepository(new PinnedHost(pin));
        jsch.addIdentity(new File(directory, "ssh-rsa").getAbsolutePath());
        // This is deliberately not a configurable host: never expose this connection on the LAN.
        Session ssh = jsch.getSession(username, "127.0.0.1", PORT);
        ssh.setConfig("StrictHostKeyChecking", "yes");
        ssh.setConfig("PreferredAuthentications", "publickey");
        ssh.setConfig("server_host_key", "rsa-sha2-512,rsa-sha2-256");
        ssh.setServerAliveInterval(15000);
        ssh.setServerAliveCountMax(3);
        try {
            ssh.connect(15000);
            ChannelShell channel = (ChannelShell) ssh.openChannel("shell");
            channel.setPtyType("xterm-256color", 80, 24, 0, 0);
            InputStream input = channel.getInputStream();
            OutputStream output = channel.getOutputStream();
            channel.connect(15000);
            return new TerminalTransport() {
                final ExecutorService control = Executors.newSingleThreadExecutor();
                final AtomicBoolean closed = new AtomicBoolean();
                public InputStream input() { return input; }
                public OutputStream output() { return output; }
                public synchronized void resize(int columns, int rows, int cellWidth, int cellHeight) {
                    if (!closed.get()) control.execute(() -> {
                        if (!closed.get()) channel.setPtySize(columns, rows, columns * cellWidth, rows * cellHeight);
                    });
                }
                public synchronized void close() {
                    if (closed.compareAndSet(false, true)) {
                        control.execute(() -> {
                            channel.disconnect(); ssh.disconnect();
                            try { jsch.removeAllIdentity(); } catch (Exception ignored) {}
                        });
                        control.shutdown();
                    }
                }
            };
        } catch (Exception e) {
            ssh.disconnect();
            jsch.removeAllIdentity();
            throw e;
        }
    }

    static String setupScript(String publicKey) {
        if (!publicKey.matches("ssh-rsa [A-Za-z0-9+/=]+ termux-rtl-local"))
            throw new IllegalArgumentException("Unexpected public key");
        return "(\nset -eu\n" +
            "pkg install -y openssh\n" +
            "umask 077\nmkdir -p \"$HOME/.ssh\"\n" +
            "printf '%s\\n' '" + publicKey + "' > \"$HOME/.ssh/termux-rtl-authorized_keys\"\n" +
            "ssh-keygen -A\n" +
            "cat > \"$HOME/.ssh/termux-rtl-sshd_config\" <<EOF\n" +
            "Port 8023\nAddressFamily inet\nListenAddress 127.0.0.1\n" +
            "HostKey $PREFIX/etc/ssh/ssh_host_rsa_key\n" +
            "PidFile $HOME/.ssh/termux-rtl-sshd.pid\n" +
            "AuthorizedKeysFile .ssh/termux-rtl-authorized_keys\n" +
            "PubkeyAuthentication yes\nPasswordAuthentication no\nKbdInteractiveAuthentication no\n" +
            "PermitRootLogin no\nAllowTcpForwarding no\nAllowAgentForwarding no\nX11Forwarding no\nPermitTunnel no\n" +
            "EOF\n" +
            "sshd -t -f \"$HOME/.ssh/termux-rtl-sshd_config\"\n" +
            "sshd -f \"$HOME/.ssh/termux-rtl-sshd_config\" -E \"$HOME/.ssh/termux-rtl-sshd.log\"\n" +
            "printf '\\nUsername / اسم المستخدم: '; whoami\n" +
            "printf '\\nHost fingerprint / بصمة الخادم:\\n'\n" +
            "ssh-keygen -lf \"$PREFIX/etc/ssh/ssh_host_rsa_key.pub\" -E sha256\n)\n";
    }
}
