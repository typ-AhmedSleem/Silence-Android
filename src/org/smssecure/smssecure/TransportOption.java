package org.smssecure.smssecure;

import org.smssecure.smssecure.util.CharacterCalculator;
import org.smssecure.smssecure.util.CharacterCalculator.CharacterState;
import org.smssecure.smssecure.util.EncryptedSmsCharacterCalculator;
import org.smssecure.smssecure.util.PushCharacterCalculator;
import org.smssecure.smssecure.util.SmsCharacterCalculator;

public class TransportOption {
  public int                 drawable;
  public String              text;
  public String              key;
  public String              composeHint;
  public CharacterCalculator characterCalculator;

  public TransportOption(String key, int drawable, String text, String composeHint) {
    this.key         = key;
    this.drawable    = drawable;
    this.text        = text;
    this.composeHint = composeHint;

    if (isPlaintext() && isSms()) {
      this.characterCalculator = new SmsCharacterCalculator();
    } else if (isSms()) {
      this.characterCalculator = new EncryptedSmsCharacterCalculator();
    } else {
      this.characterCalculator = new PushCharacterCalculator();
    }
  }

  public boolean isPlaintext() {
    return key.equals("insecure_sms");
  }

  public boolean isSms() {
    return key.equals("insecure_sms") || key.equals("secure_sms");
  }

  public CharacterState calculateCharacters(int charactersSpent) {
    return characterCalculator.calculateCharacters(charactersSpent);
  }
}
