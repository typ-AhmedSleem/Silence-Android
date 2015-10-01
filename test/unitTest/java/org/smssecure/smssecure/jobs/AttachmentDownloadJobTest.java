package org.smssecure.smssecure.jobs;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.smssecure.smssecure.BaseUnitTest;
import org.smssecure.smssecure.database.PartDatabase.PartId;
import org.smssecure.smssecure.jobs.AttachmentDownloadJob.InvalidPartException;
import org.smssecure.smssecure.util.Util;

import ws.com.google.android.mms.pdu.PduPart;

import static org.powermock.api.mockito.PowerMockito.mock;

public class AttachmentDownloadJobTest extends BaseUnitTest {
  private AttachmentDownloadJob job;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    job = new AttachmentDownloadJob(mock(Context.class), 1L, new PartId(1L, 1L));
  }

  @Test(expected = InvalidPartException.class)
  public void testCreateAttachmentPointerInvalidId() throws Exception {
    PduPart part = new PduPart();
    part.setContentDisposition(Util.toIsoBytes("a long and acceptable valid key like we all want"));
    job.createAttachmentPointer(masterSecret, part);
  }

  @Test(expected = InvalidPartException.class)
  public void testCreateAttachmentPointerInvalidKey() throws Exception {
    PduPart part = new PduPart();
    part.setContentDisposition(Util.toIsoBytes("1"));
    job.createAttachmentPointer(masterSecret, part);
  }
}
