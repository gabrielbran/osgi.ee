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
package osgi.extender.jpa.service;

/**
 * Service property names as defined by the OSGi JPA service specification (chapter 127). The
 * specification's API package (org.osgi.service.jpa) is bound to the javax.persistence namespace
 * and is therefore not usable in a Jakarta EE environment, so the names are defined here.
 */
public final class JpaProperties {
    /** Name of the persistence unit, set on entity manager factories and entity managers. */
    public static final String JPA_UNIT_NAME = "osgi.unit.name";
    /** Class name of the persistence provider, set on persistence provider services. */
    public static final String JPA_UNIT_PROVIDER = "osgi.unit.provider";

    private JpaProperties() {
    }
}
