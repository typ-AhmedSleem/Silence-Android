package org.smssecure.smssecure.database.loaders;

import android.content.Context;
import android.database.Cursor;
import androidx.loader.content.CursorLoader;

import org.smssecure.smssecure.database.DatabaseFactory;

public class IdentityLoader extends CursorLoader {

    private final Context context;

    public IdentityLoader(Context context) {
        super(context);
        this.context = context.getApplicationContext();
    }

    @Override
    public Cursor loadInBackground() {
        return DatabaseFactory.getIdentityDatabase(context).getIdentities();
    }

}
