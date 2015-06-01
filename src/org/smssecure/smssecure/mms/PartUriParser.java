package org.smssecure.smssecure.mms;

import android.content.ContentUris;
import android.net.Uri;

import org.smssecure.smssecure.database.PartDatabase;
import org.smssecure.smssecure.util.Hex;

import java.io.IOException;

public class PartUriParser {

  private final Uri uri;

  public PartUriParser(Uri uri) {
    this.uri = uri;
  }

  public PartDatabase.PartId getPartId() {
    return new PartDatabase.PartId(getId(), getUniqueId());
  }

  private long getId() {
    return ContentUris.parseId(uri);
  }

  private long getUniqueId() {
    return Long.parseLong(uri.getPathSegments().get(1));
  }

}
