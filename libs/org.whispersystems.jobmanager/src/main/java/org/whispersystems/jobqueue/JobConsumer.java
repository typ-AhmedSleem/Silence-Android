/**
 * Copyright (C) 2014 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.whispersystems.jobqueue;

import android.util.Log;

import org.whispersystems.jobqueue.persistence.PersistentStorage;

class JobConsumer extends Thread {

  private static final String TAG = JobConsumer.class.getSimpleName();

  enum JobResult {
    SUCCESS,
    FAILURE,
    DEFERRED
  }

  private final JobQueue          jobQueue;
  private final PersistentStorage persistentStorage;

  public JobConsumer(String name, JobQueue jobQueue, PersistentStorage persistentStorage) {
    super(name);
    this.jobQueue          = jobQueue;
    this.persistentStorage = persistentStorage;
  }

  @Override
  public void run() {
    while (true) {
      Job       job    = jobQueue.getNext();
      JobResult result = runJob(job);

      if (result == JobResult.DEFERRED) {
        jobQueue.push(job);
      } else {
        if (result == JobResult.FAILURE) {
          job.onCanceled();
        }

        if (job.isPersistent()) {
          persistentStorage.remove(job.getPersistentId());
        }

        if (job.getWakeLock() != null && job.getWakeLockTimeout() == 0) {
          job.getWakeLock().release();
        }
      }

      if (job.getGroupId() != null) {
        jobQueue.setGroupIdAvailable(job.getGroupId());
      }
    }
  }

  private JobResult runJob(Job job) {
    int retryCount   = job.getRetryCount();
    int runIteration = job.getRunIteration();

    for (;runIteration<retryCount;runIteration++) {
      try {
        job.onRun();
        return JobResult.SUCCESS;
      } catch (Exception exception) {
        Log.w(TAG, exception);
        if (exception instanceof RuntimeException) {
          throw (RuntimeException)exception;
        } else if (!job.onShouldRetry(exception)) {
          return JobResult.FAILURE;
        } else if (!job.isRequirementsMet()) {
          job.setRunIteration(runIteration+1);
          return JobResult.DEFERRED;
        }
      }
    }

    return JobResult.FAILURE;
  }

}
