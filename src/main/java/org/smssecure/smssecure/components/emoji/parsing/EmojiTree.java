/**
 * Copyright (c) 2014-present Vincent DURMONT vdurmont@gmail.com
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.smssecure.smssecure.components.emoji.parsing;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Based in part on code from emoji-java
 */
public class EmojiTree {

    private final EmojiTreeNode root = new EmojiTreeNode();

    public void add(String emojiEncoding, EmojiDrawInfo emoji) {
        EmojiTreeNode tree = root;

        for (char c : emojiEncoding.toCharArray()) {
            if (!tree.hasChild(c)) {
                tree.addChild(c);
            }

            tree = tree.getChild(c);
        }

        tree.setEmoji(emoji);
    }

    public Matches isEmoji(CharSequence sequence, int startPosition, int endPosition) {
        if (sequence == null) {
            return Matches.POSSIBLY;
        }

        EmojiTreeNode tree = root;

        for (int i = startPosition; i < endPosition; i++) {
            char character = sequence.charAt(i);

            if (!tree.hasChild(character)) {
                return Matches.IMPOSSIBLE;
            }

            tree = tree.getChild(character);
        }

        return tree.isEndOfEmoji() ? Matches.EXACTLY : Matches.POSSIBLY;
    }

    public @Nullable EmojiDrawInfo getEmoji(CharSequence unicode, int startPosition, int endPostiion) {
        EmojiTreeNode tree = root;

        for (int i = startPosition; i < endPostiion; i++) {
            char character = unicode.charAt(i);

            if (!tree.hasChild(character)) {
                return null;
            }

            tree = tree.getChild(character);
        }

        return tree.getEmoji();
    }


    public enum Matches {
        EXACTLY, POSSIBLY, IMPOSSIBLE;

        public boolean exactMatch() {
            return this == EXACTLY;
        }

        public boolean impossibleMatch() {
            return this == IMPOSSIBLE;
        }
    }

    private static class EmojiTreeNode {

        private final Map<Character, EmojiTreeNode> children = new HashMap<>();
        private EmojiDrawInfo emoji;

        public @Nullable EmojiDrawInfo getEmoji() {
            return emoji;
        }

        public void setEmoji(EmojiDrawInfo emoji) {
            this.emoji = emoji;
        }

        boolean hasChild(char child) {
            return children.containsKey(child);
        }

        void addChild(char child) {
            children.put(child, new EmojiTreeNode());
        }

        EmojiTreeNode getChild(char child) {
            return children.get(child);
        }

        boolean isEndOfEmoji() {
            return emoji != null;
        }
    }

}
