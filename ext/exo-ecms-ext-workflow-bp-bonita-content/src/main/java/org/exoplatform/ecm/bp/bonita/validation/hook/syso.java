package org.exoplatform.ecm.bp.bonita.validation.hook;

import org.ow2.bonita.definition.Hook;
import org.ow2.bonita.facade.QueryAPIAccessor;
import org.ow2.bonita.facade.runtime.ActivityBody;
import org.ow2.bonita.facade.runtime.ActivityInstance;

public class syso implements Hook {

	public void execute(QueryAPIAccessor arg0,
			ActivityInstance<ActivityBody> arg1) throws Exception {
		System.out.println("#####################   I AM IN THE ACTIVITY : " + arg1.getActivityId());
		
	}

}
