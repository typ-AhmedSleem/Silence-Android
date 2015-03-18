package org.BastienLQ.SecuredText;

import org.BastienLQ.SecuredText.util.CharacterCalculator;
import org.BastienLQ.SecuredText.util.CharacterCalculator.CharacterState;
import org.BastienLQ.SecuredText.util.EncryptedSmsCharacterCalculator;
import org.BastienLQ.SecuredText.util.PushCharacterCalculator;
import org.BastienLQ.SecuredText.util.SmsCharacterCalculator;

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
