/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.services.wcm.skin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.portal.resource.SkinConfig;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.resource.SkinURL;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.URIWriter;
import org.exoplatform.web.url.MimeType;
import org.gatein.portal.controller.resource.ResourceRequestHandler;

/**
 * An implementation of the skin config.
 *
 * Created by The eXo Platform SAS Jan 19, 2007
 */
class SimpleSkin implements SkinConfig {

    private final SkinService service_;

    private final String module_;

    private final String name_;

    private final String cssPath_;

    private final String id_;

    private final int priority;

    private String             type;


    public SimpleSkin(SkinService service, String module, String name, String cssPath) {
        this(service, module, name, cssPath, Integer.MAX_VALUE);
    }

    public SimpleSkin(SkinService service, String module, String name, String cssPath, int cssPriority) {
        service_ = service;
        module_ = module;
        name_ = name;
        cssPath_ = cssPath;
        id_ = module.replace('/', '_');
        priority = cssPriority;
    }

    public int getCSSPriority() {
        return priority;
    }

    public String getId() {
        return id_;
    }

    public String getModule() {
        return module_;
    }

    public String getCSSPath() {
        return cssPath_;
    }

    public String getName() {
        return name_;
    }

    public String toString() {
        return "SimpleSkin[id=" + id_ + ",module=" + module_ + ",name=" + name_ + ",cssPath=" + cssPath_ + ", priority="
                + priority + "]";
    }

    public SkinURL createURL(final ControllerContext context) {
        if (context == null) {
            throw new NullPointerException("No controller context provided");
        }
        return new SkinURL() {

            Orientation orientation = null;
            boolean compress = !PropertyManager.isDevelopping();

            public void setOrientation(Orientation orientation) {
                this.orientation = orientation;
            }

            @Override
            public String toString() {
                try {
                    String resource = cssPath_.substring(1, cssPath_.length() - ".css".length());

                    //
                    Map<QualifiedName, String> params = new HashMap<QualifiedName, String>();
                    params.put(ResourceRequestHandler.VERSION_QN, ResourceRequestHandler.VERSION);
                    params.put(ResourceRequestHandler.ORIENTATION_QN, orientation == Orientation.RT ? "rt" : "lt");
                    params.put(ResourceRequestHandler.COMPRESS_QN, compress ? "min" : "");
                    params.put(WebAppController.HANDLER_PARAM, "skin");
                    params.put(ResourceRequestHandler.RESOURCE_QN, resource);
                    StringBuilder url = new StringBuilder();
                    context.renderURL(params, new URIWriter(url, MimeType.PLAIN));

                    //
                    return url.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
    }


    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }
}
