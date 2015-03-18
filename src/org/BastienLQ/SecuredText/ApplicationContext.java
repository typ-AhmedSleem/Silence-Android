/*
 * Copyright (C) 2013 Open Whisper Systems
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
package org.BastienLQ.SecuredText;

import android.app.Application;
import android.content.Context;

import org.BastienLQ.SecuredText.crypto.PRNGFixes;
import org.BastienLQ.SecuredText.dependencies.AxolotlStorageModule;
import org.BastienLQ.SecuredText.dependencies.InjectableType;
import org.BastienLQ.SecuredText.dependencies.SecuredTextCommunicationModule;
import org.BastienLQ.SecuredText.jobs.GcmRefreshJob;
import org.BastienLQ.SecuredText.jobs.persistence.EncryptingJobSerializer;
import org.BastienLQ.SecuredText.jobs.requirements.MasterSecretRequirementProvider;
import org.BastienLQ.SecuredText.jobs.requirements.ServiceRequirementProvider;
import org.BastienLQ.SecuredText.util.SecuredTextPreferences;
import org.whispersystems.jobqueue.JobManager;
import org.whispersystems.jobqueue.dependencies.DependencyInjector;
import org.whispersystems.jobqueue.requirements.NetworkRequirementProvider;
import org.whispersystems.libaxolotl.logging.AxolotlLoggerProvider;
import org.whispersystems.libaxolotl.util.AndroidAxolotlLogger;

import java.security.Security;

import dagger.ObjectGraph;

/**
 * Will be called once when the SecuredText process is created.
 *
 * We're using this as an insertion point to patch up the Android PRNG disaster,
 * to initialize the job manager, and to check for GCM registration freshness.
 *
 * @author Moxie Marlinspike
 */
public class ApplicationContext extends Application implements DependencyInjector {

  private JobManager jobManager;
  private ObjectGraph objectGraph;

  public static ApplicationContext getInstance(Context context) {
    return (ApplicationContext)context.getApplicationContext();
  }

  @Override
  public void onCreate() {
    initializeRandomNumberFix();
    initializeLogging();
    initializeDependencyInjection();
    initializeJobManager();
    initializeGcmCheck();
  }

  @Override
  public void injectDependencies(Object object) {
    if (object instanceof InjectableType) {
      objectGraph.inject(object);
    }
  }

  public JobManager getJobManager() {
    return jobManager;
  }

  private void initializeRandomNumberFix() {
    PRNGFixes.apply();
  }

  private void initializeLogging() {
    AxolotlLoggerProvider.setProvider(new AndroidAxolotlLogger());
  }

  private void initializeJobManager() {
    this.jobManager = JobManager.newBuilder(this)
                                .withName("SecuredTextJobs")
                                .withDependencyInjector(this)
                                .withJobSerializer(new EncryptingJobSerializer())
                                .withRequirementProviders(new MasterSecretRequirementProvider(this),
                                                          new ServiceRequirementProvider(this),
                                                          new NetworkRequirementProvider(this))
                                .withConsumerThreads(5)
                                .build();
  }

  private void initializeDependencyInjection() {
    this.objectGraph = ObjectGraph.create(new SecuredTextCommunicationModule(this),
                                          new AxolotlStorageModule(this));
  }

  private void initializeGcmCheck() {
    if (SecuredTextPreferences.isPushRegistered(this) &&
        SecuredTextPreferences.getGcmRegistrationId(this) == null)
    {
      this.jobManager.add(new GcmRefreshJob(this));
    }
  }

}
