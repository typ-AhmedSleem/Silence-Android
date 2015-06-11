package org.smssecure.smssecure.components.emoji;

public interface EmojiPageModel {
  int getIconRes();
  String[] getEmoji();
  boolean hasSpriteMap();
  String getSprite();
  boolean isDynamic();
}
