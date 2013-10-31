package org.exoplatform.ecm.webui.component.explorer;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * This class is used to avoid break UI when call ajax update.
 *
 * User: dongpd
 * Date: 10/31/13
 * Time: 9:42 AM
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UIEmptyAjaxBlock extends UIContainer {
}
