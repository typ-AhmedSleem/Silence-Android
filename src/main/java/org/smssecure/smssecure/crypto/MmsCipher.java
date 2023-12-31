package org.smssecure.smssecure.crypto;

import android.content.Context;
import android.util.Log;

import com.google.android.mms.ContentType;
import com.google.android.mms.pdu_alt.EncodedStringValue;
import com.google.android.mms.pdu_alt.MultimediaMessagePdu;
import com.google.android.mms.pdu_alt.PduBody;
import com.google.android.mms.pdu_alt.PduComposer;
import com.google.android.mms.pdu_alt.PduParser;
import com.google.android.mms.pdu_alt.PduPart;
import com.google.android.mms.pdu_alt.SendReq;

import org.smssecure.smssecure.mms.TextTransport;
import org.smssecure.smssecure.protocol.WirePrefix;
import org.smssecure.smssecure.recipients.RecipientFormattingException;
import org.smssecure.smssecure.transport.UndeliverableMessageException;
import org.smssecure.smssecure.util.Util;
import org.whispersystems.libsignal.DuplicateMessageException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.LegacyMessageException;
import org.whispersystems.libsignal.NoSessionException;
import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.protocol.SignalMessage;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.util.guava.Optional;

import java.io.IOException;

public class MmsCipher {

    private static final String TAG = MmsCipher.class.getSimpleName();

    private final TextTransport textTransport = new TextTransport();
    private final SignalProtocolStore axolotlStore;

    public MmsCipher(SignalProtocolStore axolotlStore) {
        this.axolotlStore = axolotlStore;
    }

    public MultimediaMessagePdu decrypt(Context context, MultimediaMessagePdu pdu)
            throws InvalidMessageException, LegacyMessageException, DuplicateMessageException,
            NoSessionException, UntrustedIdentityException {
        try {
            SessionCipher sessionCipher = new SessionCipher(axolotlStore, new SignalProtocolAddress(pdu.getFrom().getString(), 1));
            Optional<byte[]> ciphertext = getEncryptedData(pdu);

            if (!ciphertext.isPresent()) {
                throw new InvalidMessageException("No ciphertext present!");
            }

            byte[] decodedCiphertext = textTransport.getDecodedMessage(ciphertext.get());
            byte[] plaintext;

            if (decodedCiphertext == null) {
                throw new InvalidMessageException("failed to decode ciphertext");
            }

            try {
                plaintext = sessionCipher.decrypt(new SignalMessage(decodedCiphertext));
            } catch (InvalidMessageException e) {
                // NOTE - For some reason, Sprint seems to append a single character to the
                // end of message text segments.  I don't know why, so here we just try
                // truncating the message by one if the MAC fails.
                if (ciphertext.get().length > 2) {
                    Log.w(TAG, "Attempting truncated decrypt...");
                    byte[] truncated = Util.trim(ciphertext.get(), ciphertext.get().length - 1);
                    decodedCiphertext = textTransport.getDecodedMessage(truncated);
                    plaintext = sessionCipher.decrypt(new SignalMessage(decodedCiphertext));
                } else {
                    throw e;
                }
            }

            return (MultimediaMessagePdu) new PduParser(plaintext).parse();
        } catch (IOException e) {
            throw new InvalidMessageException(e);
        }
    }

    public SendReq encrypt(Context context, SendReq message)
            throws NoSessionException, RecipientFormattingException, UndeliverableMessageException,
            UntrustedIdentityException {
        EncodedStringValue[] encodedRecipient = message.getTo();
        String recipientString = encodedRecipient[0].getString();
        byte[] pduBytes = new PduComposer(context, message).make();

        if (pduBytes == null) {
            throw new UndeliverableMessageException("PDU composition failed, null payload");
        }

        if (!axolotlStore.containsSession(new SignalProtocolAddress(recipientString, 1))) {
            throw new NoSessionException("No session for: " + recipientString);
        }

        SessionCipher cipher = new SessionCipher(axolotlStore, new SignalProtocolAddress(recipientString, 1));
        CiphertextMessage ciphertextMessage = cipher.encrypt(pduBytes);
        byte[] encryptedPduBytes = textTransport.getEncodedMessage(ciphertextMessage.serialize());

        PduBody body = new PduBody();
        PduPart part = new PduPart();

        part.setContentId((String.valueOf(System.currentTimeMillis())).getBytes());
        part.setContentType(ContentType.TEXT_PLAIN.getBytes());
        part.setName((String.valueOf(System.currentTimeMillis())).getBytes());
        part.setData(encryptedPduBytes);
        body.addPart(part);
        message.setSubject(new EncodedStringValue(WirePrefix.calculateEncryptedMmsSubject()));
        message.setBody(body);

        return message;
    }


    private Optional<byte[]> getEncryptedData(MultimediaMessagePdu pdu) {
        for (int i = 0; i < pdu.getBody().getPartsNum(); i++) {
            if (new String(pdu.getBody().getPart(i).getContentType()).equals(ContentType.TEXT_PLAIN)) {
                return Optional.of(pdu.getBody().getPart(i).getData());
            }
        }

        return Optional.absent();
    }


}
