/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

/** Requests text-style presentation for standalone emoji-capable symbols. */
final class TextStyleEmoji {
    private static final int TEXT_PRESENTATION = 0xFE0E;
    private static final int EMOJI_PRESENTATION = 0xFE0F;
    private static final int ZERO_WIDTH_JOINER = 0x200D;
    private static final int KEYCAP = 0x20E3;
    private static final int TAG_START = 0xE0020;
    private static final int TAG_END = 0xE007F;

    private TextStyleEmoji() {}

    static String preferTextPresentation(String text) {
        StringBuilder result = new StringBuilder(text.length() + 8);
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            int nextOffset = offset + Character.charCount(codePoint);
            int next = nextOffset < text.length()
                    ? text.codePointAt(nextOffset) : -1;

            if (codePoint == EMOJI_PRESENTATION
                    && next == KEYCAP) {
                result.appendCodePoint(codePoint);
            } else if (isEmojiCandidate(codePoint)
                    && !isSequenceComponent(text, offset, nextOffset,
                            codePoint, next)) {
                result.appendCodePoint(codePoint);
                if (next == EMOJI_PRESENTATION) {
                    result.appendCodePoint(TEXT_PRESENTATION);
                    nextOffset += Character.charCount(next);
                } else if (next != TEXT_PRESENTATION) {
                    result.appendCodePoint(TEXT_PRESENTATION);
                }
            } else {
                result.appendCodePoint(codePoint);
            }
            offset = nextOffset;
        }
        return result.toString();
    }

    private static boolean isSequenceComponent(
            String text, int offset, int nextOffset, int codePoint, int next) {
        int previous = offset > 0 ? text.codePointBefore(offset) : -1;
        if (previous == ZERO_WIDTH_JOINER || next == ZERO_WIDTH_JOINER) return true;
        if (isRegionalIndicator(codePoint) || isSkinToneModifier(codePoint)) {
            return true;
        }
        if (isEmojiCandidate(codePoint)
                && (isSkinToneModifier(next) || isEmojiTag(next))) return true;

        // Do not turn a keycap sequence (for example, 1️⃣) into text.
        int selectorOffset = next == EMOJI_PRESENTATION
                ? nextOffset + Character.charCount(next) : nextOffset;
        return selectorOffset < text.length()
                && text.codePointAt(selectorOffset) == KEYCAP;
    }

    private static boolean isRegionalIndicator(int codePoint) {
        return codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF;
    }

    private static boolean isSkinToneModifier(int codePoint) {
        return codePoint >= 0x1F3FB && codePoint <= 0x1F3FF;
    }

    private static boolean isEmojiTag(int codePoint) {
        return codePoint >= TAG_START && codePoint <= TAG_END;
    }

    private static boolean isEmojiCandidate(int codePoint) {
        return codePoint == 0x00A9 || codePoint == 0x00AE
                || codePoint == 0x203C || codePoint == 0x2049
                || codePoint == 0x2122 || codePoint == 0x2139
                || codePoint == 0x3030 || codePoint == 0x303D
                || codePoint == 0x3297 || codePoint == 0x3299
                || (codePoint >= 0x2300 && codePoint <= 0x23FF)
                || (codePoint >= 0x2600 && codePoint <= 0x27BF)
                || (codePoint >= 0x1F000 && codePoint <= 0x1FAFF);
    }
}
