package com.termux.terminal;

import java.io.InputStream;
import java.io.OutputStream;

/** An already authenticated remote PTY. Implementations must keep resize/close non-blocking. */
public interface TerminalTransport {
    InputStream input();
    OutputStream output();
    void resize(int columns, int rows, int cellWidthPixels, int cellHeightPixels);
    void close();
}
