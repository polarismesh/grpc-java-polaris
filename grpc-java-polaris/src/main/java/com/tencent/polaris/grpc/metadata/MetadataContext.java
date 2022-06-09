/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.tencent.polaris.grpc.metadata;

import io.grpc.Context;
import io.grpc.Context.Key;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * copy from https://github.com/Tencent/spring-cloud-tencent/blob/main/spring-cloud-tencent-commons/src/main/java/com/tencent/cloud/common/metadata/MetadataContext.java
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public final class MetadataContext {

    public static final Key<MetadataContext> METADATA_CONTEXT_KEY = Context.keyWithDefault("MetadataContext", new MetadataContext());

    public static final String FRAGMENT_HEADER = "header";

    public static final String FRAGMENT_GRPC_CONTEXT = "grpc_context";

    private Map<String, Map<String, String>> fragmentContexts;

    public MetadataContext() {
        this.fragmentContexts = new ConcurrentHashMap<>();
    }

    public Map<String, String> getHeaderFragment() {
        return getFragment(FRAGMENT_HEADER);
    }

    public Map<String, String> getGrpcContextFragment() {
        return getFragment(FRAGMENT_GRPC_CONTEXT);
    }

    private Map<String, String> getFragment(final String fragment) {
        Map<String, String> fragmentContext = fragmentContexts.get(fragment);
        if (fragmentContext == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(fragmentContext);
    }

    public void putHeaderFragment(final String key, final String value) {
        putHeaderFragment(FRAGMENT_HEADER, key, value);
    }

    public void putContextFragment(final String key, final String value) {
        putHeaderFragment(FRAGMENT_GRPC_CONTEXT, key, value);
    }

    private void putHeaderFragment(final String fragment, final String key, final String value) {
        Map<String, String> fragmentContext = fragmentContexts.get(fragment);
        if (fragmentContext == null) {
            fragmentContext = new ConcurrentHashMap<>();
            fragmentContexts.put(fragment, fragmentContext);
        }
        fragmentContext.put(key, value);
    }

    public void reset() {
        fragmentContexts = new ConcurrentHashMap<>();
    }

    @Override
    public String toString() {
        return "MetadataContext{" +
                "fragmentContexts=" + fragmentContexts +
                '}';
    }
}
