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

import org.ow2.bonita.util.Base64;

/**
 * Encode and decode a serialiazable object
 * @author Le Gall Rodrigue <rodrigue.le-gall@bull.net>
 */
public class ObjectSerializer {

    static int options = Base64.URL_SAFE | Base64.DONT_BREAK_LINES;

    public static String encode(java.io.Serializable serializableObject) {
        return Base64.encodeObject(serializableObject,options);
    }

    public static Object decode(String encodedObject) {
        return Base64.decodeToObject(encodedObject, options);
    }
}
