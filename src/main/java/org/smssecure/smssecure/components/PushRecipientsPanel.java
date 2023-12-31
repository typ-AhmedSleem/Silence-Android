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
package org.smssecure.smssecure.components;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RelativeLayout;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.contacts.RecipientsAdapter;
import org.smssecure.smssecure.contacts.RecipientsEditor;
import org.smssecure.smssecure.recipients.Recipient;
import org.smssecure.smssecure.recipients.RecipientFactory;
import org.smssecure.smssecure.recipients.RecipientFormattingException;
import org.smssecure.smssecure.recipients.Recipients;
import org.smssecure.smssecure.recipients.Recipients.RecipientsModifiedListener;

import java.util.LinkedList;
import java.util.List;

/**
 * Panel component combining both an editable field with a button for
 * a list-based contact selector.
 *
 * @author Moxie Marlinspike
 */
public class PushRecipientsPanel extends RelativeLayout implements RecipientsModifiedListener {
    private static final int RECIPIENTS_MAX_LENGTH = 312;
    private final String TAG = PushRecipientsPanel.class.getSimpleName();
    private RecipientsPanelChangedListener panelChangeListener;
    private RecipientsEditor recipientsText;
    private View panel;

    public PushRecipientsPanel(Context context) {
        super(context);
        initialize();
    }

    public PushRecipientsPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public PushRecipientsPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    public void addRecipient(String name, String number) {
        if (name != null) recipientsText.append(name + "< " + number + ">, ");
        else recipientsText.append(number + ", ");
    }

    public void addRecipients(Recipients recipients) {
        List<Recipient> recipientList = recipients.getRecipientsList();

        for (Recipient recipient : recipientList) {
            addRecipient(recipient.getName(), recipient.getNumber());
        }
    }

    public Recipients getRecipients() throws RecipientFormattingException {
        String rawText = recipientsText.getText().toString();
        Recipients recipients = RecipientFactory.getRecipientsFromString(getContext(), rawText, true);

        if (recipients.isEmpty())
            throw new RecipientFormattingException("Recipient List Is Empty!");

        return recipients;
    }

    public void disable() {
        recipientsText.setText("");
        panel.setVisibility(View.GONE);
    }

    public void setPanelChangeListener(RecipientsPanelChangedListener panelChangeListener) {
        this.panelChangeListener = panelChangeListener;
    }

    private void initialize() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.push_recipients_panel, this, true);

        View imageButton = findViewById(R.id.contacts_button);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            ((MarginLayoutParams) imageButton.getLayoutParams()).topMargin = 0;

        panel = findViewById(R.id.recipients_panel);
        initRecipientsEditor();
    }

    private void initRecipientsEditor() {
        Recipients recipients;
        recipientsText = findViewById(R.id.recipients_text);

        try {
            recipients = getRecipients();
        } catch (RecipientFormattingException e) {
            recipients = RecipientFactory.getRecipientsFor(getContext(), new LinkedList<Recipient>(), true);
        }
        recipients.addListener(this);

        recipientsText.setAdapter(new RecipientsAdapter(this.getContext()));
        recipientsText.populate(recipients);

        recipientsText.setOnFocusChangeListener(new FocusChangedListener());
        recipientsText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (panelChangeListener != null) {
                    try {
                        panelChangeListener.onRecipientsPanelUpdate(getRecipients());
                    } catch (RecipientFormattingException rfe) {
                        panelChangeListener.onRecipientsPanelUpdate(null);
                    }
                }
                recipientsText.setText("");
            }
        });
    }

    @Override
    public void onModified(Recipients recipients) {
        recipientsText.populate(recipients);
    }

    public interface RecipientsPanelChangedListener {
        void onRecipientsPanelUpdate(Recipients recipients);
    }

    private class FocusChangedListener implements View.OnFocusChangeListener {
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus && (panelChangeListener != null)) {
                try {
                    panelChangeListener.onRecipientsPanelUpdate(getRecipients());
                } catch (RecipientFormattingException rfe) {
                    panelChangeListener.onRecipientsPanelUpdate(null);
                }
            }
        }
    }

}
