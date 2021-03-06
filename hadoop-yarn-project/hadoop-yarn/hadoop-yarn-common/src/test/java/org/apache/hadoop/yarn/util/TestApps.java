/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.hadoop.yarn.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.util.Shell;
import org.junit.Test;

public class TestApps {
  @Test
  public void testSetEnvFromInputString() {
    Map<String, String> environment = new HashMap<String, String>();
    environment.put("JAVA_HOME", "/path/jdk");
    String goodEnv = "a1=1,b_2=2,_c=3,d=4,e=,f_win=%JAVA_HOME%"
        + ",g_nix=$JAVA_HOME";
    Apps.setEnvFromInputString(environment, goodEnv, File.pathSeparator);
    assertEquals("1", environment.get("a1"));
    assertEquals("2", environment.get("b_2"));
    assertEquals("3", environment.get("_c"));
    assertEquals("4", environment.get("d"));
    assertEquals("", environment.get("e"));
    if (Shell.WINDOWS) {
      assertEquals("$JAVA_HOME", environment.get("g_nix"));
      assertEquals("/path/jdk", environment.get("f_win"));
    } else {
      assertEquals("/path/jdk", environment.get("g_nix"));
      assertEquals("%JAVA_HOME%", environment.get("f_win"));
    }
    String badEnv = "1,,2=a=b,3=a=,4==,5==a,==,c-3=3,=";
    environment.clear();
    Apps.setEnvFromInputString(environment, badEnv, File.pathSeparator);
    assertEquals(environment.size(), 0);

    // Test "=" in the value part
    environment.clear();
    Apps.setEnvFromInputString(environment, "b1,e1==,e2=a1=a2,b2",
        File.pathSeparator);
    assertEquals("=", environment.get("e1"));
    assertEquals("a1=a2", environment.get("e2"));
  }

  @Test
  public void testSetEnvFromInputStringWithQuotes() {
    Map<String, String> environment = new HashMap<String, String>();
    String envString = "YARN_CONTAINER_RUNTIME_TYPE=docker"
      + ",YARN_CONTAINER_RUNTIME_DOCKER_IMAGE=hadoop-docker"
      + ",YARN_CONTAINER_RUNTIME_DOCKER_LOCAL_RESOURCE_MOUNTS=\"/dir1:/targetdir1,/dir2:/targetdir2\""
      + ",YARN_CONTAINER_RUNTIME_DOCKER_ENVIRONMENT_VARIABLES=\"HADOOP_CONF_DIR=$HADOOP_CONF_DIR,HADOOP_HDFS_HOME=/hadoop\""
      ;

    Apps.setEnvFromInputString(environment, envString, File.pathSeparator);
    assertEquals("docker", environment.get("YARN_CONTAINER_RUNTIME_TYPE"));
    assertEquals("hadoop-docker", environment.get("YARN_CONTAINER_RUNTIME_DOCKER_IMAGE"));
    assertEquals("/dir1:/targetdir1,/dir2:/targetdir2", environment.get("YARN_CONTAINER_RUNTIME_DOCKER_LOCAL_RESOURCE_MOUNTS"));
    assertEquals("HADOOP_CONF_DIR=,HADOOP_HDFS_HOME=/hadoop", environment.get("YARN_CONTAINER_RUNTIME_DOCKER_ENVIRONMENT_VARIABLES"));

    environment.clear();

    environment.put("HADOOP_CONF_DIR", "etc/hadoop");
    Apps.setEnvFromInputString(environment, envString, File.pathSeparator);
    assertEquals("docker", environment.get("YARN_CONTAINER_RUNTIME_TYPE"));
    assertEquals("hadoop-docker", environment.get("YARN_CONTAINER_RUNTIME_DOCKER_IMAGE"));
    assertEquals("/dir1:/targetdir1,/dir2:/targetdir2", environment.get("YARN_CONTAINER_RUNTIME_DOCKER_LOCAL_RESOURCE_MOUNTS"));
    assertEquals("HADOOP_CONF_DIR=etc/hadoop,HADOOP_HDFS_HOME=/hadoop", environment.get("YARN_CONTAINER_RUNTIME_DOCKER_ENVIRONMENT_VARIABLES"));
  }
}
