/**
 * Copyright (C) 2011 Whisper Systems
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.smssecure.smssecure.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.crypto.AsymmetricMasterCipher;
import org.smssecure.smssecure.crypto.AsymmetricMasterSecret;
import org.smssecure.smssecure.crypto.MasterCipher;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.model.DisplayRecord;
import org.smssecure.smssecure.database.model.SmsMessageRecord;
import org.smssecure.smssecure.sms.IncomingTextMessage;
import org.smssecure.smssecure.sms.OutgoingTextMessage;
import org.smssecure.smssecure.util.LRUCache;
import org.whispersystems.libsignal.InvalidMessageException;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Map;

public class EncryptingSmsDatabase extends SmsDatabase {

    private final PlaintextCache plaintextCache = new PlaintextCache();

    public EncryptingSmsDatabase(Context context, SQLiteOpenHelper databaseHelper) {
        super(context, databaseHelper);
    }

    private String getAsymmetricEncryptedBody(AsymmetricMasterSecret masterSecret, String body) {
        AsymmetricMasterCipher bodyCipher = new AsymmetricMasterCipher(masterSecret);
        return bodyCipher.encryptBody(body);
    }

    private String getEncryptedBody(MasterSecret masterSecret, String body) {
        MasterCipher bodyCipher = new MasterCipher(masterSecret);
        String ciphertext = bodyCipher.encryptBody(body);
        plaintextCache.put(ciphertext, body);

        return ciphertext;
    }

    public long insertMessageOutbox(MasterSecret masterSecret, long threadId,
                                    OutgoingTextMessage message, boolean forceSms,
                                    long timestamp) {
        long type = Types.BASE_SENDING_TYPE;
        message = message.withBody(getEncryptedBody(masterSecret, message.getMessageBody()));
        type |= Types.ENCRYPTION_SYMMETRIC_BIT;

        return insertMessageOutbox(threadId, message, type, forceSms, timestamp);
    }

    public Pair<Long, Long> insertMessageInbox(MasterSecret masterSecret,
                                               IncomingTextMessage message) {
        long type = Types.BASE_INBOX_TYPE;

        if (masterSecret == null && message.isSecureMessage()) {
            type |= Types.ENCRYPTION_REMOTE_BIT;
        } else {
            type |= Types.ENCRYPTION_SYMMETRIC_BIT;
            message = message.withMessageBody(getEncryptedBody(masterSecret, message.getMessageBody()));
        }

        return insertMessageInbox(message, type);
    }

    public Pair<Long, Long> insertMessageInbox(AsymmetricMasterSecret masterSecret,
                                               IncomingTextMessage message) {
        long type = Types.BASE_INBOX_TYPE;

        if (message.isSecureMessage()) {
            type |= Types.ENCRYPTION_REMOTE_BIT;
        } else {
            message = message.withMessageBody(getAsymmetricEncryptedBody(masterSecret, message.getMessageBody()));
            type |= Types.ENCRYPTION_ASYMMETRIC_BIT;
        }

        return insertMessageInbox(message, type);
    }

    public Pair<Long, Long> updateBundleMessageBody(MasterSecret masterSecret, long messageId, String body) {
        String encryptedBody = getEncryptedBody(masterSecret, body);
        return updateMessageBodyAndType(messageId, encryptedBody, Types.TOTAL_MASK,
                Types.BASE_INBOX_TYPE | Types.ENCRYPTION_SYMMETRIC_BIT | Types.SECURE_MESSAGE_BIT);
    }

    public void updateMessageBody(MasterSecret masterSecret, long messageId, String body) {
        String encryptedBody = getEncryptedBody(masterSecret, body);
        updateMessageBodyAndType(messageId, encryptedBody, Types.ENCRYPTION_MASK,
                Types.ENCRYPTION_SYMMETRIC_BIT);
    }

    public Reader getMessages(MasterSecret masterSecret, int skip, int limit) {
        Cursor cursor = super.getMessages(skip, limit);
        return new DecryptingReader(masterSecret, cursor);
    }

    public Reader getOutgoingMessages(MasterSecret masterSecret) {
        Cursor cursor = super.getOutgoingMessages();
        return new DecryptingReader(masterSecret, cursor);
    }

    public SmsMessageRecord getMessage(MasterSecret masterSecret, long messageId) throws NoSuchMessageException {
        Cursor cursor = super.getMessage(messageId);
        DecryptingReader reader = new DecryptingReader(masterSecret, cursor);
        SmsMessageRecord record = reader.getNext();

        reader.close();

        if (record == null) throw new NoSuchMessageException("No message for ID: " + messageId);
        else return record;
    }

    public Reader getDecryptInProgressMessages(MasterSecret masterSecret) {
        Cursor cursor = super.getDecryptInProgressMessages();
        return new DecryptingReader(masterSecret, cursor);
    }

    public Reader readerFor(MasterSecret masterSecret, Cursor cursor) {
        return new DecryptingReader(masterSecret, cursor);
    }

    private static class PlaintextCache {
        private static final int MAX_CACHE_SIZE = 2000;
        private static final Map<String, SoftReference<String>> decryptedBodyCache =
                Collections.synchronizedMap(new LRUCache<String, SoftReference<String>>(MAX_CACHE_SIZE));

        public void put(String ciphertext, String plaintext) {
            decryptedBodyCache.put(ciphertext, new SoftReference<String>(plaintext));
        }

        public String get(String ciphertext) {
            SoftReference<String> plaintextReference = decryptedBodyCache.get(ciphertext);

            if (plaintextReference != null) {
                String plaintext = plaintextReference.get();

                return plaintext;
            }

            return null;
        }
    }

    public class DecryptingReader extends SmsDatabase.Reader {

        private final MasterCipher masterCipher;

        public DecryptingReader(MasterSecret masterSecret, Cursor cursor) {
            super(cursor);
            this.masterCipher = new MasterCipher(masterSecret);
        }

        @Override
        protected DisplayRecord.Body getBody(Cursor cursor) {
            long type = cursor.getLong(cursor.getColumnIndexOrThrow(SmsDatabase.TYPE));
            String ciphertext = cursor.getString(cursor.getColumnIndexOrThrow(SmsDatabase.BODY));

            if (ciphertext == null) {
                return new DisplayRecord.Body("", true);
            }

            try {
                if (SmsDatabase.Types.isSymmetricEncryption(type)) {
                    String plaintext = plaintextCache.get(ciphertext);

                    if (plaintext != null)
                        return new DisplayRecord.Body(plaintext, true);

                    plaintext = masterCipher.decryptBody(ciphertext);

                    plaintextCache.put(ciphertext, plaintext);
                    return new DisplayRecord.Body(plaintext, true);
                } else {
                    return new DisplayRecord.Body(ciphertext, true);
                }
            } catch (InvalidMessageException e) {
                Log.w("EncryptingSmsDatabase", e);
                return new DisplayRecord.Body(context.getString(R.string.EncryptingSmsDatabase_error_decrypting_message), true);
            }
        }
    }
}
