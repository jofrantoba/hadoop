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
package org.apache.hadoop.yarn.server.resourcemanager.scheduler.fair.policies;

import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.server.resourcemanager.resource.ResourceType;
import org.apache.hadoop.yarn.server.resourcemanager.resource.ResourceWeights;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.fair.FakeSchedulable;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.fair.Schedulable;
import org.apache.hadoop.yarn.server.utils.BuilderUtils;
import org.apache.hadoop.yarn.util.resource.Resources;
import org.junit.Test;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * comparator.compare(sched1, sched2) < 0 means that sched1 should get a
 * container before sched2
 */
public class TestDominantResourceFairnessPolicy {

  private Comparator<Schedulable> createComparator(int clusterMem,
      int clusterCpu, int clusterGpu) {
    DominantResourceFairnessPolicy policy = new DominantResourceFairnessPolicy();
    policy.initialize(BuilderUtils.newResource(clusterMem, clusterCpu, clusterGpu));
    return policy.getComparator();
  }

  private Comparator<Schedulable> createComparator(Resource capacity) {
    Set<ResourceType> enabledResourceTypes = new HashSet<ResourceType>();
    for (ResourceType type : ResourceType.values()) {
      enabledResourceTypes.add(type);
    }
    return createComparator(capacity, enabledResourceTypes);
  }

  private Comparator<Schedulable> createComparator(Resource capacity,
      Set<ResourceType> enabledResourceTypes) {
    DominantResourceFairnessPolicy policy = new DominantResourceFairnessPolicy();
    policy.setEnabledResourceTypes(enabledResourceTypes);
    policy.initialize(capacity);
    return policy.getComparator();
  }
    
  private Schedulable createSchedulable(int memUsage, int cpuUsage, int gpuUsage) {
    return createSchedulable(memUsage, cpuUsage, gpuUsage, ResourceWeights.NEUTRAL, 0, 0, 0);
  }
  
  private Schedulable createSchedulable(int memUsage, int cpuUsage, int gpuUsage,
      int minMemShare, int minCpuShare, int minGpuShare) {
    return createSchedulable(memUsage, cpuUsage, gpuUsage, ResourceWeights.NEUTRAL,
        minMemShare, minCpuShare, minGpuShare);
  }
  
  private Schedulable createSchedulable(int memUsage, int cpuUsage, int gpuUsage,
      ResourceWeights weights) {
    return createSchedulable(memUsage, cpuUsage, gpuUsage, weights, 0, 0, 0);
  }

  
  private Schedulable createSchedulable(int memUsage, int cpuUsage, int gpuUsage,
      ResourceWeights weights, int minMemShare, int minCpuShare, int minGpuShare) {
    Resource usage = BuilderUtils.newResource(memUsage, cpuUsage, gpuUsage);
    Resource minShare = BuilderUtils.newResource(minMemShare, minCpuShare, minGpuShare);
    return new FakeSchedulable(minShare,
        Resources.createResource(Integer.MAX_VALUE, Integer.MAX_VALUE),
        weights, Resources.none(), usage, 0l);
  }
  
  @Test
  public void testSameDominantResource() {
    assertTrue(createComparator(8000, 4, 4).compare(
        createSchedulable(1000, 1, 1),
        createSchedulable(2000, 1, 1)) < 0);
  }
  
  @Test
  public void testDifferentDominantResource() {
    assertTrue(createComparator(8000, 8, 8).compare(
        createSchedulable(4000, 3, 3),
        createSchedulable(2000, 5, 5)) < 0);
  }
  
  @Test
  public void testOneIsNeedy() {
    assertTrue(createComparator(8000, 8, 8).compare(
        createSchedulable(2000, 5, 1, 0, 6, 0),
        createSchedulable(4000, 3, 1, 0, 0, 0)) < 0);
  }
  
  @Test
  public void testBothAreNeedy() {
    assertTrue(createComparator(8000, 100, 100).compare(
        // dominant share is 2000/8000
        createSchedulable(2000, 5, 5),
        // dominant share is 4000/8000
        createSchedulable(4000, 3, 3)) < 0);
    assertTrue(createComparator(8000, 100, 100).compare(
        // dominant min share is 2/3
        createSchedulable(2000, 5, 1, 3000, 6, 6),
        // dominant min share is 4/5
        createSchedulable(4000, 3, 1, 5000, 4, 4)) < 0);
  }
  
  @Test
  public void testEvenWeightsSameDominantResource() {
    assertTrue(createComparator(8000, 8, 20).compare(
        createSchedulable(3000, 1, 2, new ResourceWeights(2.0f)),
        createSchedulable(2000, 1, 2)) < 0);
    assertTrue(createComparator(8000, 8, 20).compare(
        createSchedulable(1000, 3, 6, new ResourceWeights(2.0f)),
        createSchedulable(1000, 2, 4)) < 0);
  }
  
  @Test
  public void testEvenWeightsDifferentDominantResource() {
    assertTrue(createComparator(8000, 8, 20).compare(
        createSchedulable(1000, 3, 6, new ResourceWeights(2.0f)),
        createSchedulable(2000, 1, 1)) < 0);
    assertTrue(createComparator(8000, 8, 8).compare(
        createSchedulable(3000, 1, 1, new ResourceWeights(2.0f)),
        createSchedulable(1000, 2, 2)) < 0);
  }
  
  @Test
  public void testUnevenWeightsSameDominantResource() {
    assertTrue(createComparator(8000, 8, 8).compare(
        createSchedulable(3000, 1, 1, new ResourceWeights(3.0f, 2.0f, 1.0f)),
        createSchedulable(2000, 1, 1)) < 0);
    assertTrue(createComparator(8000, 8, 8).compare(
        createSchedulable(1000, 3, 3, new ResourceWeights(1.0f, 2.0f, 3.0f)),
        createSchedulable(1000, 2, 2)) < 0);
  }
  
  @Test
  public void testUnevenWeightsDifferentDominantResource() {
    assertTrue(createComparator(8000, 8, 8).compare(
        createSchedulable(1000, 3, 3, new ResourceWeights(1.0f, 2.0f, 3.0f)),
        createSchedulable(2000, 1, 1)) < 0);
    assertTrue(createComparator(8000, 8, 8).compare(
        createSchedulable(3000, 1, 1, new ResourceWeights(3.0f, 2.0f, 1.0f)),
        createSchedulable(1000, 2, 2)) < 0);
  }
  
  @Test
  public void testCalculateShares() {
    Resource used = Resources.createResource(10, 5, 8);
    Resource capacity = Resources.createResource(100, 10, 20);
    ResourceType[] resourceOrder = new ResourceType[3];
    ResourceWeights shares = new ResourceWeights();
    ((DominantResourceFairnessPolicy.DominantResourceFairnessComparator)
        createComparator(capacity)).calculateShares(
        used, capacity, shares, resourceOrder, ResourceWeights.NEUTRAL);
    
    assertEquals(.1, shares.getWeight(ResourceType.MEMORY), .00001);
    assertEquals(.5, shares.getWeight(ResourceType.CPU), .00001);
    assertEquals(.4, shares.getWeight(ResourceType.GPU), .00001);
    assertEquals(ResourceType.CPU, resourceOrder[0]);
    assertEquals(ResourceType.MEMORY, resourceOrder[1]);
    assertEquals(ResourceType.GPU, resourceOrder[2]);
  }
}
