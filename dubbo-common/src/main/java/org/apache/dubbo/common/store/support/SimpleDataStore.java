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
package org.apache.dubbo.common.store.support;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.store.DataStore;
import org.apache.dubbo.common.store.DataStoreUpdateListener;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SimpleDataStore implements DataStore {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(SimpleDataStore.class);

    // <component name or id, <data-name, data-value>>
    private final ConcurrentMap<String, ConcurrentMap<String, Object>> data = new ConcurrentHashMap<>();
    private final ConcurrentHashSet<DataStoreUpdateListener> listeners = new ConcurrentHashSet<>();

    @Override
    public Map<String, Object> get(String componentName) {
        ConcurrentMap<String, Object> value = data.get(componentName);
        if (value == null) {
            return new HashMap<>();
        }

        return new HashMap<>(value);
    }

    @Override
    public Object get(String componentName, String key) {
        if (!data.containsKey(componentName)) {
            return null;
        }
        return data.get(componentName).get(key);
    }

    @Override
    public void put(String componentName, String key, Object value) {
        Map<String, Object> componentData =
                ConcurrentHashMapUtils.computeIfAbsent(data, componentName, k -> new ConcurrentHashMap<>());
        componentData.put(key, value);
        notifyListeners(componentName, key, value);
    }

    @Override
    public void remove(String componentName, String key) {
        if (!data.containsKey(componentName)) {
            return;
        }
        data.get(componentName).remove(key);
        notifyListeners(componentName, key, null);
    }

    @Override
    public void addListener(DataStoreUpdateListener dataStoreUpdateListener) {
        listeners.add(dataStoreUpdateListener);
    }

    private void notifyListeners(String componentName, String key, Object value) {
        for (DataStoreUpdateListener listener : listeners) {
            try {
                listener.onUpdate(componentName, key, value);
            } catch (Throwable t) {
                logger.warn(
                        LoggerCodeConstants.INTERNAL_ERROR,
                        "",
                        "",
                        "Failed to notify data store update listener. " + "ComponentName: " + componentName + " Key: "
                                + key,
                        t);
            }
        }
    }
}
