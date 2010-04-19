/**
 * Copyright (C) 2008  Bull S. A. S.
 * Bull, Rue Jean Jaures, B.P.68, 78340, Les Clayes-sous-Bois
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA  02110-1301, USA.
 **/
package org.exoplatform.services.workflow.impl.bonita;

import java.util.Comparator;

import org.exoplatform.services.workflow.Task;

/**
 * TODO describe the code purpose
 * @author Le Gall Rodrigue <rodrigue.le-gall@bull.net>
 */
public class TaskComparator implements Comparator<Task> {

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Task t1, Task t2) {
        return t1.getTaskName().compareToIgnoreCase(t2.getTaskName());
    }


}
