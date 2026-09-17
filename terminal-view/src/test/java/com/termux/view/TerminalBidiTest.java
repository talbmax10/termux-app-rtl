package com.termux.view;

import com.termux.terminal.WcWidth;
import org.junit.Test;
import static org.junit.Assert.*;

public class TerminalBidiTest {
    private TerminalBidi layout(String text) {
        int columns = text.codePoints().map(cp -> Math.max(0, WcWidth.width(cp))).sum();
        return TerminalBidi.create(text.toCharArray(), text.length(), columns);
    }

    private String visual(String text) {
        TerminalBidi bidi = layout(text);
        StringBuilder result = new StringBuilder();
        for (TerminalBidi.Cell cell : bidi.cells) result.append(text, cell.start, cell.end);
        return result.toString();
    }

    @Test public void asciiUsesOriginalRenderer() {
        assertNull(layout("user@host:~$ ls -la 123"));
    }

    @Test public void arabicReordersButKeepsPromptAndTrailingCells() {
        assertEquals("$ ابحرم   ", visual("$ مرحبا   "));
    }

    @Test public void hebrewAndLatin() {
        assertEquals("abc םולש xyz", visual("abc שלום xyz"));
    }

    @Test public void numbersRemainLeftToRight() {
        assertEquals("abc 123 םולש", visual("abc שלום 123"));
    }

    @Test public void combiningMarksStayWithBase() {
        assertEquals("بَرَ", visual("رَبَ"));
    }

    @Test public void wideCellsAndSurrogatesArePreserved() {
        String text = "שלום 😀界";
        TerminalBidi bidi = layout(text);
        int total = 0;
        for (TerminalBidi.Cell cell : bidi.cells) {
            assertEquals(total, cell.visualColumn);
            total += cell.width;
            if (text.substring(cell.start, cell.end).equals("😀")) {
                assertEquals(2, cell.width);
                assertEquals(2, cell.end - cell.start);
            }
            for (int offset = 0; offset < cell.width; offset++)
                assertEquals(cell.column + offset, bidi.logicalColumn(cell.visualColumn + offset));
        }
        assertEquals(9, total);
    }

    @Test public void selectionEdgesFollowDirection() {
        TerminalBidi bidi = layout("$ שלום ");
        assertEquals(5, bidi.logicalColumn(2));
        assertEquals(6, bidi.visualBoundary(2, false));
        assertEquals(5, bidi.visualBoundary(2, true));
        assertEquals(0, bidi.visualBoundary(0, false));
        assertEquals(1, bidi.visualBoundary(0, true));
    }

    @Test public void styleSplitsCanRetainArabicShapingContext() {
        TerminalBidi bidi = layout("$ مرحبا ");
        for (TerminalBidi.Cell cell : bidi.cells) {
            if (!cell.isRtl()) continue;
            assertEquals(2, cell.contextStart);
            assertEquals(7, cell.contextEnd);
        }
    }
}
