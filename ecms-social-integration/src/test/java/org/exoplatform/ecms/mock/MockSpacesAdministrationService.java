package org.exoplatform.ecms.mock;

import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.space.SpacesAdministrationService;

import java.util.List;

public class MockSpacesAdministrationService implements SpacesAdministrationService {
  @Override
  public List<MembershipEntry> getSpacesAdministratorsMemberships() {
    return null;
  }

  @Override
  public void updateSpacesAdministratorsMemberships(List<MembershipEntry> permissionsExpressions) {

  }

  @Override
  public List<MembershipEntry> getSpacesCreatorsMemberships() {
    return null;
  }

  @Override
  public void updateSpacesCreatorsMemberships(List<MembershipEntry> permissionsExpressions) {

  }

  @Override
  public boolean canCreateSpace(String username) {
    return false;
  }
}
