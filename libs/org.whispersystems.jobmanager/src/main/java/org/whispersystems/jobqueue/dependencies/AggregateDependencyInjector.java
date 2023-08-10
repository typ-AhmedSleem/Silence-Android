package org.whispersystems.jobqueue.dependencies;

import android.content.Context;

import org.whispersystems.jobqueue.Job;
import org.whispersystems.jobqueue.requirements.Requirement;

public class AggregateDependencyInjector {

  private final DependencyInjector dependencyInjector;

  public AggregateDependencyInjector(DependencyInjector dependencyInjector) {
    this.dependencyInjector = dependencyInjector;
  }

  public void injectDependencies(Context context, Job job) {
    if (job instanceof ContextDependent) {
      ((ContextDependent)job).setContext(context);
    }

    for (Requirement requirement : job.getRequirements()) {
      if (requirement instanceof ContextDependent) {
        ((ContextDependent)requirement).setContext(context);
      }
    }

    if (dependencyInjector != null) {
      dependencyInjector.injectDependencies(job);

      for (Requirement requirement : job.getRequirements()) {
        dependencyInjector.injectDependencies(requirement);
      }
    }
  }

}
