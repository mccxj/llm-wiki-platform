package com.llmwiki.common.types;

import jakarta.validation.constraints.NotNull;
import java.util.Objects;

@SuppressWarnings("unused")
public class CharInterval {
    private int startOffset;
    private int endOffset;

    public CharInterval() {
    }

    public CharInterval(int startOffset, int endOffset) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    public int length() {
        return endOffset - startOffset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharInterval that = (CharInterval) o;
        return startOffset == that.startOffset && endOffset == that.endOffset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startOffset, endOffset);
    }

    @Override
    public String toString() {
        return "CharInterval{" +
                "startOffset=" + startOffset +
                ", endOffset=" + endOffset +
                '}';
    }
}
