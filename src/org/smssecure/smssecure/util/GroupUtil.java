package org.smssecure.smssecure.util;

import android.content.Context;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.util.List;

import org.smssecure.smssecure.R;
import static org.whispersystems.textsecure.internal.push.TextSecureProtos.GroupContext;

public class GroupUtil {

  private static final String ENCODED_GROUP_PREFIX = "__textsecure_group__!";

  public static String getEncodedId(byte[] groupId) {
    return ENCODED_GROUP_PREFIX + Hex.toStringCondensed(groupId);
  }

  public static byte[] getDecodedId(String groupId) throws IOException {
    if (!isEncodedGroup(groupId)) {
      throw new IOException("Invalid encoding");
    }

    return Hex.fromStringCondensed(groupId.split("!", 2)[1]);
  }

  public static boolean isEncodedGroup(String groupId) {
    return groupId.startsWith(ENCODED_GROUP_PREFIX);
  }

  public static String getDescription(Context context, String encodedGroup) {
    if (encodedGroup == null) {
      return "";
    }

    try {
      StringBuilder description  = new StringBuilder();
      GroupContext  groupContext = GroupContext.parseFrom(Base64.decode(encodedGroup));
      List<String>  members      = groupContext.getMembersList();
      String        title        = groupContext.getName();

      if (!members.isEmpty()) {
        description.append("");
      }

      if (title != null && !title.trim().isEmpty()) {
        if (description.length() > 0) description.append(" ");
        description.append("");
      }

      if (description.length() > 0) {
        return description.toString();
      } else {
        return "";
      }
    } catch (InvalidProtocolBufferException e) {
      Log.w("GroupUtil", e);
      return "";
    } catch (IOException e) {
      Log.w("GroupUtil", e);
      return "";
    }
  }
}
