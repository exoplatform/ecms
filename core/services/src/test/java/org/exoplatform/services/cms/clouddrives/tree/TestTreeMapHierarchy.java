/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.cms.clouddrives.tree;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import junit.framework.TestCase;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: TestTreeMapHierarchy.java 00000 Sep 12, 2012 pnedonosko $
 */
public class TestTreeMapHierarchy extends TestCase {

  private TreeMap<String, Integer> treeMap;

  /**
   * setUp.
   * 
   * @throws java.lang.Exception
   */
  protected void setUp() throws Exception {
    super.setUp();

    treeMap = new TreeMap<>();
  }

  /**
   * tearDown.
   * 
   * @throws java.lang.Exception
   */
  protected void tearDown() throws Exception {
    treeMap.clear();
    super.tearDown();
  }

  /**
   * Test ordering.
   *
   * @throws Exception the exception
   */
  public void testOrdering() throws Exception {
    int index = 0;
    // treeMap.put("/folder2/fileB1", 1);
    // treeMap.put("/folder2/fileB2", 2);
    treeMap.put("/folder1/folder2/file21", index++);
    treeMap.put("/folder1/folder2", index++);
    treeMap.put("/folder1/fileA2", index++);
    treeMap.put("/folder1/fileA1", index++);
    treeMap.put("/folder1/fileA3", index++);
    // treeMap.put("/folder1", index++);
    // treeMap.put("/file2", index++);
    // treeMap.put("/file1", index++);
    // treeMap.put("/folder2", index++);
    // treeMap.put("/folder3", index++);
    // treeMap.put("/afolder1/file-ajdksajdlsjd", 8);
    // treeMap.put("/afolder1/file-bdssajdksjd", 9);
    // treeMap.put("/afolder1/folder-ctest", 10);
    // treeMap.put("/zfolder1/ajdskjdksj", 11);
    // treeMap.put("/zfolder1/bcjdcidjciijcidjic", 12);
    // treeMap.put("/zfolder1/c", 13);
    // treeMap.put("/afolder1/FOLDER-czdsaa", 14);
    // treeMap.put("/afolder1/file-deerrrrllkl-kjshasjahskhasjhakshkh", 15);
    // treeMap.put("/afolder1/folder-ctest/f1-sjdksjdksj", 16);
    // treeMap.put("/afolder1/folder-ctest/f2-ldksjdksj", 17);
    // treeMap.put("/afolder1/folder-ctest/afile1", 18);
    // treeMap.put("/afolder1/folder-ctest/n-file1", 19);
    // treeMap.put("/afolder1/folder-ctest/s-fileA", 20);

    String target = "/folder1/folder2";

    //
    System.out.println("Full map:");
    System.out.println(mapStr(treeMap));

    //
    System.out.println("\nTail " + target + ":");
    System.out.println(mapStr(treeMap.tailMap(target)));

    //
    System.out.println("\nHead " + target + ":");
    System.out.println(mapStr(treeMap.headMap(target)));

    //
    System.out.println("\nfloorKey " + target + ":" + treeMap.floorKey(target));

    //
    System.out.println("lowerKey " + target + ": " + treeMap.lowerKey(target));

    //
    System.out.println("ceilingKey " + target + ": " + treeMap.ceilingKey(target));

    //
    System.out.println("higherKey " + target + ": " + treeMap.higherKey(target));

    //
    System.out.println("\nEnd");
  }

  private String mapStr(Map<String, Integer> map) {
    return map.entrySet().stream().map(e -> "[\t" + e.getValue() + "\t]\t" + e.getKey()).collect(Collectors.joining("\n"));
  }
}
