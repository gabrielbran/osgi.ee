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
package sample.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import sample.api.ItemStore;

/**
 * Waits for the CDI bean exported as service and exercises it. The results are logged.
 */
public class Activator implements BundleActivator {
    private static final Logger LOG = Logger.getLogger("osgi.ee.sample");
    private ServiceTracker<ItemStore, ItemStore> tracker;

    @Override
    public void start(BundleContext context) {
        tracker = new ServiceTracker<>(context, ItemStore.class, null);
        tracker.open();
        new Thread(() -> {
            try {
                ItemStore store = tracker.waitForService(120000L);
                if (store == null) {
                    LOG.severe("SAMPLE FAILED: no ItemStore service");
                    return;
                }
                LOG.info("SAMPLE JTA count after 1st insert: " + store.addJta("first"));
                LOG.info("SAMPLE JTA count after 2nd insert: " + store.addJta("second"));
                LOG.info("SAMPLE RESOURCE_LOCAL count after 1st insert: " + store.addResourceLocal("first"));
                LOG.info("SAMPLE OK");
            } catch (Throwable exc) {
                LOG.log(Level.SEVERE, "SAMPLE FAILED", exc);
            }
        }, "osgi.ee.sample").start();
    }

    @Override
    public void stop(BundleContext context) {
        tracker.close();
    }
}
