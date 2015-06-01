package org.smssecure.smssecure.components.emoji;

public interface EmojiPageModel {
  int getIconRes();
  int[] getCodePoints();
  boolean isDynamic();
}
