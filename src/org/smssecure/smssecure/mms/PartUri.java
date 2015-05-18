package org.smssecure.smssecure.mms;

import android.content.ContentUris;
import android.net.Uri;

import org.smssecure.smssecure.util.Hex;

import java.io.IOException;

public class PartUri {

  private final Uri uri;

  public PartUri(Uri uri) {
    this.uri = uri;
  }

  public long getId() {
    return ContentUris.parseId(uri);
  }

  public byte[] getContentId() {
    try {
      return Hex.fromStringCondensed(uri.getPathSegments().get(1));
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

}
