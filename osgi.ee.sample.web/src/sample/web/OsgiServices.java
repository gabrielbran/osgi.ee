/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

import sample.api.ItemStore;

/**
 * Access from a Jakarta EE application to the OSGi service registry of GlassFish. The application
 * sees the packages exported by the bundles installed in GlassFish, among which the OSGi framework API.
 */
@ApplicationScoped
public class OsgiServices {
    private static final long TIMEOUT = 30000L;
    private final Map<String, ServiceTracker<?, ?>> trackers = new ConcurrentHashMap<>();

    /**
     * The context of the system bundle. It is reached via a class of an active bundle, here the
     * bundle exporting the service interface used by this application.
     */
    private static BundleContext context() {
        Bundle bundle = FrameworkUtil.getBundle(ItemStore.class);
        if (bundle == null || bundle.getBundleContext() == null) {
            throw new IllegalStateException("bundle of " + ItemStore.class + " is not active");
        }
        return bundle.getBundleContext().getBundle(0).getBundleContext();
    }

    /**
     * Get a service, waiting for it to become available if needed.
     *
     * @param type The service interface
     * @param filter An OSGi filter on the service properties, may be null
     * @param <T> The service type
     * @return The service
     */
    public <T> T get(Class<T> type, String filter) {
        String key = type.getName() + filter;
        ServiceTracker<?, ?> tracker = trackers.computeIfAbsent(key, (k) -> {
            try {
                String full = "(objectClass=" + type.getName() + ")";
                if (filter != null) {
                    full = "(&" + full + filter + ")";
                }
                BundleContext context = context();
                ServiceTracker<?, ?> created = new ServiceTracker<>(context, context.createFilter(full), null);
                created.open();
                return created;
            } catch (InvalidSyntaxException exc) {
                throw new IllegalArgumentException(exc);
            }
        });
        try {
            Object service = tracker.waitForService(TIMEOUT);
            if (service == null) {
                throw new IllegalStateException("no service " + key);
            }
            return type.cast(service);
        } catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exc);
        }
    }

    @PreDestroy
    void close() {
        trackers.values().forEach(ServiceTracker::close);
    }
}
