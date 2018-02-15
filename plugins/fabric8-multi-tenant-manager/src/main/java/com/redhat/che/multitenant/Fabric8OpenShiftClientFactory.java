/*
 * Copyright (c) 2016-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package com.redhat.che.multitenant;

import com.google.inject.Provider;
import io.fabric8.kubernetes.client.Config;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.workspace.server.WorkspaceRuntimes;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.RuntimeContext;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.workspace.infrastructure.openshift.OpenShiftClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Sergii Leshchenko */
@Singleton
public class Fabric8OpenShiftClientFactory extends OpenShiftClientFactory {

  private final Fabric8WorkspaceEnvironmentProvider envProvider;
  private final Provider<WorkspaceRuntimes> workspaceRuntimeProvider;

  private static final Logger LOG = LoggerFactory.getLogger(Fabric8OpenShiftClientFactory.class);

  @Inject
  public Fabric8OpenShiftClientFactory(
      Fabric8WorkspaceEnvironmentProvider envProvider,
      Provider<WorkspaceRuntimes> workspaceRuntimeProvider) {
    super(null, null, null, null, false);
    this.envProvider = envProvider;
    this.workspaceRuntimeProvider = workspaceRuntimeProvider;
  }

  @Override
  public Config buildConfig(Config defaultConfig, @Nullable String workspaceId)
      throws InfrastructureException {
    Subject currentSubject = EnvironmentContext.getCurrent().getSubject();
    if (workspaceId != null) {
      Optional<String> userIdFromWorkspaceId = getUserIdForWorkspaceId(workspaceId);
      if (userIdFromWorkspaceId.isPresent()) {
        if (!userIdFromWorkspaceId.get().equals(currentSubject.getUserId())) {
          LOG.warn(
              "Current user Id ('{}') is different from the user Id ('{}') that started workspace {}",
              currentSubject.getUserId(),
              userIdFromWorkspaceId.get(),
              workspaceId);
        }
      }
    }
    return envProvider.getWorkspacesOpenshiftConfig(currentSubject);
  }

  private Optional<String> getUserIdForWorkspaceId(String workspaceId) {
    Optional<RuntimeContext> context =
        workspaceRuntimeProvider.get().getRuntimeContext(workspaceId);
    return context.map(c -> c.getIdentity().getOwnerId());
  }
}