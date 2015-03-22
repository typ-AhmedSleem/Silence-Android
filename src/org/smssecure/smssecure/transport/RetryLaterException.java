package org.smssecure.smssecure.transport;

import java.io.IOException;

public class RetryLaterException extends Exception {
  public RetryLaterException(Exception e) {
    super(e);
  }
}
