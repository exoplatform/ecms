/**
 * Copyright (C) 2023 eXo Platform SAS.
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
package org.exoplatform.services.document;

import org.exoplatform.commons.utils.QName;

/**
 * Created by The eXo Platform SAS . Dublin Core metadata element set
 * definitions see http://dublincore.org/documents/dces/
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public interface DCMetaData
{

   public final String DC_NAMESPACE = "http://purl.org/dc/elements/1.1/";

   /**
    * Definition: An entity responsible for making contributions to the resource.
    * Comment: Examples of a Contributor include a person, an organisation, or a
    * service. Typically, the name of a Contributor should be used to indicate
    * the entity.
    */
   public final QName CONTRIBUTOR = new QName(DC_NAMESPACE, "contributor");

   /**
    * Definition: The spatial or temporal topic of the resource, the spatial
    * applicability of the resource, or the jurisdiction under which the resource
    * is relevant. Comment: Spatial topic may be a named place or a location
    * specified by its geographic coordinates. Temporal period may be a named
    * period, date, or date range. A jurisdiction may be a named administrative
    * entity or a geographic place to which the resource applies. Recommended
    * best practice is to use a controlled vocabulary such as the Thesaurus of
    * Geographic Names [TGN]). Where appropriate, named places or time periods
    * can be used in preference to numeric identifiers such as sets of
    * coordinates or date ranges. References: [TGN]
    * http://www.getty.edu/research/tools/vocabulary/tgn/index.html
    */
   public final QName COVERAGE = new QName(DC_NAMESPACE, "coverage");

   /**
    * Definition: An entity primarily responsible for making the resource.
    * Comment: Examples of a Creator include a person, an organisation, or a
    * service. Typically, the name of a Creator should be used to indicate the
    * entity.
    */
   public final QName CREATOR = new QName(DC_NAMESPACE, "creator");

   /**
    * Definition: A point or period of time associated with an event in the
    * lifecycle of the resource. Comment: Date may be used to express temporal
    * information at any level of granularity. Recommended best practice is to
    * use an encoding scheme, such as the W3CDTF profile of ISO 8601 [W3CDTF].
    * References: [W3CDTF] http://www.w3.org/TR/NOTE-datetime
    */
   public final QName DATE = new QName(DC_NAMESPACE, "date");

   /**
    * Definition: An account of the resource. Comment: Description may include
    * but is not limited to: an abstract, a table of contents, a graphical
    * representation, or a free-text account of the resource.
    */
   public final QName DESCRIPTION = new QName(DC_NAMESPACE, "description");

   /**
    * Definition: The file format, physical medium, or dimensions of the
    * resource. Comment: Examples of dimensions include size and duration.
    * Recommended best practice is to use a controlled vocabulary such as the
    * list of Internet Media Types [MIME]. References: [MIME]
    * http://www.iana.org/assignments/media-types/
    */
   public final QName FORMAT = new QName(DC_NAMESPACE, "format");

   /**
    * Definition: An unambiguous reference to the resource within a given
    * context. Comment: Recommended best practice is to identify the resource by
    * means of a string conforming to a formal identification system.
    */
   public final QName IDENTIFIER = new QName(DC_NAMESPACE, "identifier");

   /**
    * Definition: A language of the resource. Comment: Recommended best practice
    * is to use a controlled vocabulary such as RFC 3066 [RFC3066]. References:
    * [RFC3066] http://www.ietf.org/rfc/rfc3066.txt
    */
   public final QName LANGUAGE = new QName(DC_NAMESPACE, "language");

   /**
    * Definition: An entity responsible for making the resource available.
    * Comment: Examples of a Publisher include a person, an organisation, or a
    * service. Typically, the name of a Publisher should be used to indicate the
    * entity.
    */
   public final QName PUBLISHER = new QName(DC_NAMESPACE, "publisher");

   /**
    * Definition: A related resource. Comment: Recommended best practice is to
    * identify the related resource by means of a string conforming to a formal
    * identification system.
    */
   public final QName RESOURCE = new QName(DC_NAMESPACE, "resource");

   /**
    * Definition: Information about rights held in and over the resource.
    * Comment: Typically, rights information includes a statement about various
    * property rights associated with the resource, including intellectual
    * property rights.
    */
   public final QName RIGHTS = new QName(DC_NAMESPACE, "rights");

   /**
    * Definition: The resource from which the described resource is derived.
    * Comment: The described resource may be derived from the related resource in
    * whole or in part. Recommended best practice is to identify the related
    * resource by means of a string conforming to a formal identification system.
    */
   public final QName SOURCE = new QName(DC_NAMESPACE, "source");

   /**
    * Definition: The topic of the resource. Comment: Typically, the topic will
    * be represented using keywords, key phrases, or classification codes.
    * Recommended best practice is to use a controlled vocabulary. To describe
    * the spatial or temporal topic of the resource, use the Coverage element.
    */
   public final QName SUBJECT = new QName(DC_NAMESPACE, "subject");

   /**
    * Definition: A name given to the resource. Comment: Typically, a Title will
    * be a name by which the resource is formally known.
    */
   public final QName TITLE = new QName(DC_NAMESPACE, "title");

   /**
    * Definition: The nature or genre of the resource. Comment: Recommended best
    * practice is to use a controlled vocabulary such as the DCMI Type Vocabulary
    * [DCMITYPE]). To describe the file format, physical medium, or dimensions of
    * the resource, use the Format element. References: [DCMITYPE]
    * http://dublincore.org/documents/dcmi-type-vocabulary/
    */
   public final QName TYPE = new QName(DC_NAMESPACE, "type");

   /**
    * Term Name: relation
    * URI:  http://purl.org/dc/elements/1.1/relation 
    * Label:  Relation
    * Definition:   A related resource.
    * Comment:  Recommended best practice is to identify the related resource 
    * by means of a string conforming to a formal identification system.
    */
   public final QName RELATION = new QName(DC_NAMESPACE, "relation");

}
