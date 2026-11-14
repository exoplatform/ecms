/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.connector.clouddrives;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;

@RunWith(MockitoJUnitRunner.class)
public class FeaturesServiceTest {

  @Mock
  private SettingService  settingService;

  private FeaturesService featuresService;

  @Before
  public void setUp() {
    featuresService = new FeaturesService(null, null, null, null, settingService);
  }

  @Test
  public void testSwitchAStoredStatusReadAsText() {
    // The setting store returns every value as text, whatever type was set
    doReturn(SettingValue.create("true")).when(settingService).get(any(Context.class), any(Scope.class), anyString());

    Response response = featuresService.setCloudDriveStatus(null);

    assertEquals(200, response.getStatus());
    assertEquals("{\"result\":\"false\"}", response.getEntity());
    verify(settingService).set(any(Context.class),
                               any(Scope.class),
                               eq("isCloudDriveEnabled"),
                               argThat(value -> Boolean.FALSE.equals(value.getValue())));
  }

  @Test
  public void testSwitchADisabledStatus() {
    doReturn(SettingValue.create("false")).when(settingService).get(any(Context.class), any(Scope.class), anyString());

    Response response = featuresService.setCloudDriveStatus(null);

    assertEquals("{\"result\":\"true\"}", response.getEntity());
    verify(settingService).set(any(Context.class),
                               any(Scope.class),
                               eq("isCloudDriveEnabled"),
                               argThat(value -> Boolean.TRUE.equals(value.getValue())));
  }

  @Test
  public void testSwitchAStatusNeverStored() {
    // No value means enabled, so the first switch disables
    Response response = featuresService.setCloudDriveStatus(null);

    assertEquals(200, response.getStatus());
    assertEquals("{\"result\":\"false\"}", response.getEntity());
    verify(settingService).set(any(Context.class),
                               any(Scope.class),
                               eq("isCloudDriveEnabled"),
                               argThat(value -> Boolean.FALSE.equals(value.getValue())));
  }

}
