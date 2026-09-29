/*
 * Copyright 2015, Imtech Traffic & Infra
 * Copyright 2015, aVineas IT Consulting
 *
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
package osgi.extender.cdi.weld;

import java.security.ProtectionDomain;
import java.util.Arrays;

import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.Bundle;

import osgi.extender.cdi.DelegatingClassLoader;
import osgi.extender.cdi.Helper;

/**
 * Proxy service handling for proxying beans. Basically, proxying is done using the combination
 * of the class loader from the bundle we are extending combined with the class loader of the weld bundle.
 * Since Weld 3 the integrator defines the proxy classes. They are all defined in one class loader per
 * extended bundle, which sees both the bean classes and the Weld internals that proxies refer to.
 */
class BundleProxyService implements ProxyServices {
    private ProxyClassLoader loader;

    BundleProxyService(Bundle extendedBundle) {
        ClassLoader extendedBundleLoader = Helper.getWiring(extendedBundle).getClassLoader();
        // The Weld bundle is the one providing the bootstrap package to us. It contains all Weld
        // internals, among which the classes proxies are built upon.
        ClassLoader weldLoader = WeldBootstrap.class.getClassLoader();
        loader = new ProxyClassLoader(DelegatingClassLoader.from(Arrays.asList(weldLoader, extendedBundleLoader)));
    }

    @Override
    public void cleanup() {
        // Nothing to clean.
    }

    @Override
    public Class<?> defineClass(Class<?> originalClass, String className, byte[] classBytes, int off, int len) {
        return defineClass(originalClass, className, classBytes, off, len, null);
    }

    @Override
    public Class<?> defineClass(Class<?> originalClass, String className, byte[] classBytes, int off, int len,
            ProtectionDomain protectionDomain) {
        return loader.define(className, classBytes, off, len, protectionDomain);
    }

    @Override
    public Class<?> loadClass(Class<?> originalClass, String classBinaryName) throws ClassNotFoundException {
        return loader.loadClass(classBinaryName);
    }

    /**
     * Class loader holding the generated proxy classes.
     */
    private static class ProxyClassLoader extends ClassLoader {
        static {
            registerAsParallelCapable();
        }

        ProxyClassLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> define(String name, byte[] bytes, int off, int len, ProtectionDomain domain) {
            synchronized (getClassLoadingLock(name)) {
                // The same proxy may be requested concurrently.
                Class<?> clz = findLoadedClass(name);
                if (clz == null) {
                    clz = defineClass(name, bytes, off, len, domain);
                }
                return clz;
            }
        }
    }
}
