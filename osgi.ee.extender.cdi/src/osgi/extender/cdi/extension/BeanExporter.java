/*
 * Copyright 2014, aVineas IT Consulting
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
package osgi.extender.cdi.extension;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.Bean;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import osgi.cdi.annotation.Service;

/**
 * Helper functionality for registration of exported beans. Performs the action related to 
 * exporting the beans that are marked as such. Static methods only.
 * 
 * @author Arie van Wijngaarden 
 */
class BeanExporter {
    
    /**
     * Perform the service registration for a bean. The method checks whether a service annotation is
     * present on the bean, and if such, registers an OSGi service for the bean by determining the
     * interfaces implemented by the class. The properties of the service are retrieved from the
     * service annotation and set on the service registration.
     * 
     * @param context The context used for registering the service
     * @param bean The bean for which the action needs to take place
     * @param instance The instance or service factory of the bean that is used for registration
     * @return A service registration for the instance, or null if it is not a service
     */
    static ServiceRegistration<?> registerService(BundleContext context, Bean<?> bean, Object instance) {
        Service service = getServiceDefinition(bean);
        if (service == null) return null;
        // Check the properties.
        Dictionary<String, ?> dict = getProperties(service);
        Collection<String> classes = getInterfaces(bean.getTypes());
        ServiceRegistration<?> s = context.registerService(
                classes.toArray(new String[classes.size()]), instance, dict);
        return s;
    }

    /**
     * Get the interfaces of the types specified by the bean.
     * 
     * @param types The types of the bean
     * @return A set with interface names
     */
    static Set<String> getInterfaces(Collection<Type> types) {
        // Stream: filter classes, get the interfaces of them map them to their name.
        return types.stream().filter((t) -> (t instanceof Class<?>)).
                flatMap((t) -> Arrays.asList(((Class<?>) t).getInterfaces()).stream()).
                map((c) -> c.getName()).
                collect(Collectors.toSet());
    }
    
    /**
     * Get the service properties from a service definition. The method parses the properties
     * defined and translates them into a dictionary that can be set on a service. The conversion
     * follows the specification of the Declarative Services specification, meaning that it
     * is possible to add the type like "foo:Integer=1". See compendium 5 specification 112.4.6.
     * 
     * @param service The service annotation to extract the properties from
     * @return A dictionary with the service properties to be used during registration
     */
    static Dictionary<String, Object> getProperties(Service service) {
        final Hashtable<String, Object> props = new Hashtable<>();
        // Parse them.
        Arrays.asList(service.properties()).stream().forEach((s) -> {
            String[] splitted = s.split("=");
            if (splitted.length != 2 || splitted[0].trim().length() == 0 || splitted[1].trim().length() == 0) 
                throw new RuntimeException("invalid property for @Service: " + s);
            String stringValue = splitted[1].trim();
            String propAndOptionalType = splitted[0].trim();
            // Check for type conversion.
            String[] propAndType = propAndOptionalType.split(":");
            String propertyName = propAndType[0].trim();
            Object value;
            if (propAndType.length == 1) {
                // String type, default.
                value = stringValue;
            }
            else {
                // Conversion needed.
                String type = propAndType[1].trim();
                try {
                    if ("String".equals(type)) {
                        value = stringValue;
                    }
                    else if ("Double".equals(type)) {
                        value = Double.valueOf(stringValue);
                    }
                    else if ("Integer".equals(type)) {
                        value = Integer.valueOf(stringValue);
                    }
                    else if ("Long".equals(type)) {
                        value = Long.valueOf(stringValue);
                    }
                    else if ("Float".equals(type)) {
                        value = Float.valueOf(stringValue);
                    }
                    else if ("Byte".equals(type)) {
                        value = Byte.valueOf(stringValue);
                    }
                    else if ("Boolean".equals(type)) {
                        value = Boolean.valueOf(stringValue);
                    }
                    else if ("Short".equals(type)) {
                        value = Short.valueOf(stringValue);
                    }
                    else if ("Character".equals(type)) {
                        Integer c = Integer.valueOf(stringValue);
                        value = new Character(Character.toChars(c)[0]);
                    }
                    else {
                        throw new RuntimeException("type \"" + type + "\" is an unknown conversion type");
                    }
                } catch (Exception exc) {
                    throw new RuntimeException("cannot convert service property \"" + propertyName + "\" for " + service, exc);
                }
            }
            props.put(propertyName, value);
        });
        return props;
    }
    
    /**
     * Get the service definition from a bean. Performs some sanity checking, like multiple service
     * annotations (not allowed).
     * 
     * @param bean The bean to get the service definition from
     * @return The service definition, if indicated. Null otherwise
     */
    static Service getServiceDefinition(Bean<?> bean) {
        Service[] services = bean.getBeanClass().getAnnotationsByType(Service.class);
        if (services.length == 0) return null;
        if (services.length > 1) {
            throw new RuntimeException("cannot have more than one @Service annotation. Class: " + 
                    bean.getBeanClass().getName());
        }
        return services[0];
    }
}
