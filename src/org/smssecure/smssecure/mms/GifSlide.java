package org.smssecure.smssecure.mms;

import android.content.Context;
import android.net.Uri;

import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.util.BitmapDecodingException;

import java.io.IOException;

import ws.com.google.android.mms.pdu.PduPart;

public class GifSlide extends ImageSlide {
  public GifSlide(Context context, MasterSecret masterSecret, PduPart part) {
    super(context, masterSecret, part);
  }

  public GifSlide(Context context, MasterSecret masterSecret, Uri uri)
      throws IOException, BitmapDecodingException
  {
    super(context, masterSecret, uri);
  }

  @Override public Uri getThumbnailUri() {
    return getPart().getDataUri();
  }
}
