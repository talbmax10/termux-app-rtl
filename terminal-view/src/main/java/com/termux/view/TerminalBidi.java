package com.termux.view;

import com.termux.terminal.WcWidth;

import java.text.Bidi;
import java.util.ArrayList;
import java.util.List;

/** Visual-only bidi layout. The terminal buffer always remains in logical order. */
final class TerminalBidi {
    static final class Cell {
        final int column, width, start, end;
        final byte level;
        int visualColumn, contextStart, contextEnd;

        Cell(int column, int width, int start, int end, byte level) {
            this.column = column;
            this.width = width;
            this.start = start;
            this.end = end;
            this.level = level;
        }

        boolean isRtl() { return (level & 1) != 0; }
    }

    final Cell[] cells;
    private final int columns;

    static TerminalBidi create(char[] text, int length, int columns) {
        if (!Bidi.requiresBidi(text, 0, length)) return null;
        return new TerminalBidi(text, length, columns);
    }

    private TerminalBidi(char[] text, int length, int columns) {
        this.columns = columns;
        // A terminal is an LTR grid: keep prompts and unused trailing cells anchored.
        Bidi bidi = new Bidi(new String(text, 0, length), Bidi.DIRECTION_LEFT_TO_RIGHT);
        List<Cell> logical = new ArrayList<>();
        for (int index = 0, column = 0; index < length && column < columns;) {
            int start = index;
            int cp = Character.codePointAt(text, index, length);
            int width = Math.max(1, WcWidth.width(cp));
            index += Character.charCount(cp);
            // Combining marks and supplementary code points must never be reversed internally.
            while (index < length) {
                cp = Character.codePointAt(text, index, length);
                if (WcWidth.width(cp) > 0) break;
                index += Character.charCount(cp);
            }
            logical.add(new Cell(column, width, start, index, (byte) bidi.getLevelAt(start)));
            column += width;
        }
        cells = logical.toArray(new Cell[0]);
        byte[] levels = new byte[cells.length];
        for (int start = 0; start < cells.length;) {
            int end = start + 1;
            while (end < cells.length && cells[end].level == cells[start].level) end++;
            for (int i = start; i < end; i++) {
                cells[i].contextStart = cells[start].start;
                cells[i].contextEnd = cells[end - 1].end;
                levels[i] = cells[i].level;
            }
            start = end;
        }
        Bidi.reorderVisually(levels, 0, cells, 0, cells.length);
        int visual = 0;
        for (Cell cell : cells) {
            cell.visualColumn = visual;
            visual += cell.width;
        }
    }

    int logicalColumn(int visual) {
        for (Cell cell : cells)
            if (visual >= cell.visualColumn && visual < cell.visualColumn + cell.width)
                return cell.column + (visual - cell.visualColumn);
        return Math.max(0, Math.min(columns - 1, visual));
    }

    /** Boundary of a selected logical cell, accounting for RTL edge direction. */
    int visualBoundary(int logical, boolean end) {
        for (Cell cell : cells)
            if (logical >= cell.column && logical < cell.column + cell.width)
                return cell.visualColumn + ((end != cell.isRtl()) ? cell.width : 0);
        return Math.max(0, Math.min(columns, logical));
    }
}
