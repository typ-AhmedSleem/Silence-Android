package org.smssecure.smssecure;

import android.support.annotation.DrawableRes;

import org.smssecure.smssecure.util.CharacterCalculator;
import org.smssecure.smssecure.util.CharacterCalculator.CharacterState;
import org.smssecure.smssecure.util.EncryptedSmsCharacterCalculator;
import org.smssecure.smssecure.util.SmsCharacterCalculator;

public class TransportOption {

  public enum Type {
    INSECURE_SMS,
    SECURE_SMS
  }

  private int                 drawable;
  private int                 backgroundColor;
  private String              text;
  private Type                type;
  private String              composeHint;
  private CharacterCalculator characterCalculator;

  public TransportOption(Type type,
                         @DrawableRes int drawable,
                         int backgroundColor,
                         String text,
                         String composeHint,
                         CharacterCalculator characterCalculator)
  {
    this.type                = type;
    this.drawable            = drawable;
    this.backgroundColor     = backgroundColor;
    this.text                = text;
    this.composeHint         = composeHint;
    this.characterCalculator = characterCalculator;
  }

  public Type getType() {
    return type;
  }

  public boolean isType(Type type) {
    return this.type == type;
  }

  public CharacterState calculateCharacters(int charactersSpent) {
    return characterCalculator.calculateCharacters(charactersSpent);
  }

  public @DrawableRes int getDrawable() {
    return drawable;
  }

  public int getBackgroundColor() {
    return backgroundColor;
  }

  public String getComposeHint() {
    return composeHint;
  }

  public String getDescription() {
    return text;
  }
}
