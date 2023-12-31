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
import android.util.Log;

import java.io.File;

public class CanonicalSessionMigrator {

    private static void migrateSession(File sessionFile, File sessionsDirectory, long canonicalAddress) {
        File canonicalSessionFile = new File(sessionsDirectory.getAbsolutePath() + File.separatorChar + canonicalAddress);
        sessionFile.renameTo(canonicalSessionFile);
        Log.w("CanonicalSessionMigrator", "Moving: " + sessionFile + " to " + canonicalSessionFile);

        File canonicalSessionFileLocal = new File(sessionsDirectory.getAbsolutePath() + File.separatorChar + canonicalAddress + "-local");
        File localFile = new File(sessionFile.getAbsolutePath() + "-local");
        if (localFile.exists())
            localFile.renameTo(canonicalSessionFileLocal);

        Log.w("CanonicalSessionMigrator", "Moving " + localFile + " to " + canonicalSessionFileLocal);

        File canonicalSessionFileRemote = new File(sessionsDirectory.getAbsolutePath() + File.separatorChar + canonicalAddress + "-remote");
        File remoteFile = new File(sessionFile.getAbsolutePath() + "-remote");
        if (remoteFile.exists())
            remoteFile.renameTo(canonicalSessionFileRemote);

        Log.w("CanonicalSessionMigrator", "Moving " + remoteFile + " to " + canonicalSessionFileRemote);

    }

    public static void migrateSessions(Context context) {
        if (context.getSharedPreferences("SecureSMS", Context.MODE_PRIVATE).getBoolean("canonicalized", false))
            return;

        CanonicalAddressDatabase canonicalDb = CanonicalAddressDatabase.getInstance(context);
        File rootDirectory = context.getFilesDir();
        File sessionsDirectory = new File(rootDirectory.getAbsolutePath() + File.separatorChar + "sessions");
        sessionsDirectory.mkdir();

        String[] files = rootDirectory.list();

        for (String file : files) {
            File item = new File(rootDirectory.getAbsolutePath() + File.separatorChar + file);

            if (!item.isDirectory() && file.matches("[0-9]+")) {
                long canonicalAddress = canonicalDb.getCanonicalAddressId(file);
                migrateSession(item, sessionsDirectory, canonicalAddress);
            }
        }

        context.getSharedPreferences("SecureSMS", Context.MODE_PRIVATE).edit().putBoolean("canonicalized", true).apply();
    }

}
