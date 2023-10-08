package org.smssecure.smssecure;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

public class AdvancedSearchOptions {

    // Defaults
    public static final int DEFAULT_CONTACTS_LIMIT = 25;
    public static final int DEFAULT_RESULTS_LIMIT = 2500;
    public static final int DEFAULT_MSG_LIMIT = 250;

    // Preferences Keys
    public static final String PREF_NAME = "SearchOptions";
    public static final String KEY_CONTACTS_LIMIT = "SearchOptContactsLimit";
    public static final String KEY_RESULTS_LIMIT = "SearchOptResultsLimit";
    public static final String KEY_MSG_LIMIT = "SearchOptMsgLimit";
    public static final String KEY_UNREAD_ONLY = "SearchOptUnreadOnly";
    public static final String KEY_INCLUDE_ARCHIVED = "SearchOptIncludeArchived";
    public static final String KEY_PINNED_ONLY = "SearchOptPinnedOnly";

    // Fields
    private @IntRange(from = 1, to = 9999) int contactsLimit;
    private @IntRange(from = 1, to = 9999) int resultsLimit;
    private @IntRange(from = 1, to = 9999) int msgLimit;
    private boolean unreadOnly;
    private boolean includeArchived;
    private boolean pinnedOnly;

    public AdvancedSearchOptions(int contactsLimit,
                                 int resultsLimit,
                                 int msgLimit,
                                 boolean unreadOnly,
                                 boolean includeArchived,
                                 boolean pinnedOnly) {
        this.contactsLimit = contactsLimit;
        this.resultsLimit = resultsLimit;
        this.msgLimit = msgLimit;
        this.unreadOnly = unreadOnly;
        this.includeArchived = includeArchived;
        this.pinnedOnly = pinnedOnly;
    }

    public int getContactsLimit() {
        return contactsLimit;
    }

    public int getResultsLimit() {
        return resultsLimit;
    }

    public int getMsgLimit() {
        return msgLimit;
    }

    public boolean isUnreadOnly() {
        return unreadOnly;
    }

    public boolean isIncludeArchived() {
        return includeArchived;
    }

    public boolean isPinnedOnly() {
        return pinnedOnly;
    }

    public void update(AdvancedSearchOptions options) {
        if (options == null) return;
        this.contactsLimit = options.contactsLimit;
        this.resultsLimit = options.resultsLimit;
        this.msgLimit = options.msgLimit;
        this.unreadOnly = options.unreadOnly;
        this.includeArchived = options.includeArchived;
        this.pinnedOnly = options.pinnedOnly;
    }

    @NonNull
    @Override
    public String toString() {
        return "AdvancedSearchOptions{" +
                "contactsLimit=" + contactsLimit +
                ", resultsLimit=" + resultsLimit +
                ", msgLimit=" + msgLimit +
                ", unreadOnly=" + unreadOnly +
                ", includeArchived=" + includeArchived +
                ", pinnedOnly=" + pinnedOnly +
                '}';
    }

    public static class Builder {
        private int contactsLimit;
        private int resultsLimit;
        private int msgLimit;
        private boolean unreadOnly;
        private boolean includeArchived;
        private boolean pinnedOnly;

        public Builder setContactsLimit(int contactsLimit) {
            this.contactsLimit = contactsLimit;
            return this;
        }

        public Builder setResultsLimit(int resultsLimit) {
            this.resultsLimit = resultsLimit;
            return this;
        }

        public Builder setMsgLimit(int msgLimit) {
            this.msgLimit = msgLimit;
            return this;
        }

        public Builder setUnreadOnly(boolean unreadOnly) {
            this.unreadOnly = unreadOnly;
            return this;
        }

        public Builder setIncludeArchived(boolean includeArchived) {
            this.includeArchived = includeArchived;
            return this;
        }

        public Builder setPinnedOnly(boolean pinnedOnly) {
            this.pinnedOnly = pinnedOnly;
            return this;
        }

        public AdvancedSearchOptions build() {
            return new AdvancedSearchOptions(
                    contactsLimit,
                    resultsLimit,
                    msgLimit,
                    unreadOnly,
                    includeArchived,
                    pinnedOnly);
        }

    }

}
