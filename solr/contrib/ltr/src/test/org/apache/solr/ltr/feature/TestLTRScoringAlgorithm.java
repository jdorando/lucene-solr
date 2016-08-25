/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.ltr.feature;

import java.util.HashMap;
import java.util.Map;

import org.apache.solr.ltr.TestRerankBase;
import org.apache.solr.ltr.ranking.RankSVMModel;
import org.apache.solr.ltr.rest.ManagedModelStore;
import org.apache.solr.ltr.util.FeatureException;
import org.apache.solr.ltr.util.ModelException;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestLTRScoringAlgorithm extends TestRerankBase {

  static ManagedModelStore store = null;
  static FeatureStore fstore = null;

  @BeforeClass
  public static void setup() throws Exception {
    setuptest();
    // loadFeatures("features-store-test-model.json");
    store = getNewManagedModelStore();
    fstore = getNewManagedFeatureStore().getFeatureStore("test");

  }

  @Test
  public void getInstanceTest() {
    final Map<String,Object> weights = new HashMap<>();
    weights.put("constant1", 1d);
    weights.put("constant5", 1d);

    Map<String,Object> params = new HashMap<String,Object>();
    params.put("weights", weights);
    final LTRScoringAlgorithm meta = new RankSVMModel("test1",
        getFeatures(new String[] {
            "constant1", "constant5"}), "test", fstore.getFeatures(),
        params);

    store.addMetadataModel(meta);
    final LTRScoringAlgorithm m = store.getModel("test1");
    assertEquals(meta, m);
  }

  @Test
  public void nullFeatureWeightsTest() {
    final ModelException expectedException = 
        new ModelException("Model test2 doesn't contain any weights");
    try {
      final LTRScoringAlgorithm meta = new RankSVMModel("test2",
          getFeatures(new String[] {
              "constant1", "constant5"}), "test", fstore.getFeatures(), null);
      fail("unexpectedly got here instead of catching "+expectedException);
    } catch (ModelException actualException) {
      assertEquals(expectedException.toString(), actualException.toString());
    }
  }

  @Test
  public void existingNameTest() {
    final ModelException expectedException = 
        new ModelException("model 'test3' already exists. Please use a different name");
    try {
      final Map<String,Object> weights = new HashMap<>();
      weights.put("constant1", 1d);
      weights.put("constant5", 1d);

      Map<String,Object> params = new HashMap<String,Object>();
      params.put("weights", weights);
      final LTRScoringAlgorithm meta = new RankSVMModel("test3",
          getFeatures(new String[] {
              "constant1", "constant5"}), "test", fstore.getFeatures(),
              params);
      store.addMetadataModel(meta);
      final LTRScoringAlgorithm m = store.getModel("test3");
      assertEquals(meta, m);
      store.addMetadataModel(meta);
      fail("unexpectedly got here instead of catching "+expectedException);
    } catch (ModelException actualException) {
      assertEquals(expectedException.toString(), actualException.toString());
    }
  }

  @Test
  public void duplicateFeatureTest() {
    final ModelException expectedException = 
        new ModelException("duplicated feature constant1 in model test4");
    try {
      final Map<String,Object> weights = new HashMap<>();
      weights.put("constant1", 1d);
      weights.put("constant5", 1d);

      Map<String,Object> params = new HashMap<String,Object>();
      params.put("weights", weights);
      final LTRScoringAlgorithm meta = new RankSVMModel("test4",
          getFeatures(new String[] {
              "constant1", "constant1"}), "test", fstore.getFeatures(),
              params);
      store.addMetadataModel(meta);
      fail("unexpectedly got here instead of catching "+expectedException);
    } catch (ModelException actualException) {
      assertEquals(expectedException.toString(), actualException.toString());
    }

  }

  @Test
  public void missingFeatureWeightTest() {
    final ModelException expectedException = 
        new ModelException("no weight for feature constant5");
    try {
      final Map<String,Object> weights = new HashMap<>();
      weights.put("constant1", 1d);
      weights.put("constant5missing", 1d);

      Map<String,Object> params = new HashMap<String,Object>();
      params.put("weights", weights);
      final LTRScoringAlgorithm meta = new RankSVMModel("test5",
          getFeatures(new String[] {
              "constant1", "constant5"}), "test", fstore.getFeatures(),
              params);
      fail("unexpectedly got here instead of catching "+expectedException);
    } catch (ModelException actualException) {
      assertEquals(expectedException.toString(), actualException.toString());
    }
  }

  @Test
  public void emptyFeaturesTest() {
    final ModelException expectedException = 
        new ModelException("no features declared for model test6");
    try {
      final Map<String,Object> weights = new HashMap<>();
      weights.put("constant1", 1d);
      weights.put("constant5missing", 1d);

      Map<String,Object> params = new HashMap<String,Object>();
      params.put("weights", weights);
      final LTRScoringAlgorithm meta = new RankSVMModel("test6",
          getFeatures(new String[] {}), "test", fstore.getFeatures(),
          params);
      store.addMetadataModel(meta);
      fail("unexpectedly got here instead of catching "+expectedException);
    } catch (ModelException actualException) {
      assertEquals(expectedException.toString(), actualException.toString());
    }
  }
}
