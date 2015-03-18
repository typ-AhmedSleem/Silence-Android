package org.BastienLQ.SecuredText;

import android.content.Context;
import android.test.InstrumentationTestCase;

public class SecuredTextTestCase extends InstrumentationTestCase {

  @Override
  public void setUp() throws Exception {
    System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
  }

  protected Context getContext() {
    return getInstrumentation().getContext();
  }
}
