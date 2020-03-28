/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.notification.plugin;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.wcm.notification.plugin.ShareFileToSpacePlugin;
import org.exoplatform.wcm.notification.plugin.ShareFileToUserPlugin;
import org.exoplatform.wcm.notification.plugin.WebTemplateProvider;

/**
 * Templates for Push Notifications for Share file actions.
 * It extends WebTemplateProvider since we want the same information for web notifications, but it
 * re-calculate target URL in order to pass it as a standalone data to the push notifications manager.
 * The target URL is passed in the field "subject" of the MessageInfo object since there is no field to pass custom data.
 * TODO Improve MessageInfo to allow to pass custom data
 */
@TemplateConfigs (
  templates = {
    @TemplateConfig(pluginId = ShareFileToUserPlugin.ID, template = "war:/groovy/ecm/social-integration/plugin/notification/push-notifications/ShareDocumentToUser.gtmpl"),
    @TemplateConfig(pluginId = ShareFileToSpacePlugin.ID, template = "war:/groovy/ecm/social-integration/plugin/notification/push-notifications/ShareDocumentToSpace.gtmpl")
  }
)
public class SharePushTemplateProvider extends WebTemplateProvider {

  private final Map<PluginKey, AbstractTemplateBuilder> webTemplateBuilders = new HashMap<>();

  /** Defines the template builder for ShareFileToUserPlugin*/
  private AbstractTemplateBuilder shareFileToUser = new AbstractTemplateBuilder() {
    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      MessageInfo messageInfo = webTemplateBuilders.get(new PluginKey(ShareFileToUserPlugin.ID)).buildMessage(ctx);

      NotificationInfo notification = ctx.getNotificationInfo();
      String documentURL = notification.getValueOwnerParameter(ShareFileToUserPlugin.DOCUMENT_URL);

      return messageInfo.subject(documentURL).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  };

  /** Defines the template builder for ShareFileToSpacePlugin*/
  private AbstractTemplateBuilder shareFileToSpace = new AbstractTemplateBuilder() {
    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      MessageInfo messageInfo = webTemplateBuilders.get(new PluginKey(ShareFileToSpacePlugin.ID)).buildMessage(ctx);

      NotificationInfo notification = ctx.getNotificationInfo();
      String documentURL = notification.getValueOwnerParameter(ShareFileToSpacePlugin.DOCUMENT_URL);

      return messageInfo.subject(documentURL).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  };

  public SharePushTemplateProvider(InitParams initParams) {
    super(initParams);
    this.webTemplateBuilders.putAll(this.templateBuilders);
    this.templateBuilders.put(PluginKey.key(ShareFileToUserPlugin.ID), shareFileToUser);
    this.templateBuilders.put(PluginKey.key(ShareFileToSpacePlugin.ID), shareFileToSpace);
  }
}
