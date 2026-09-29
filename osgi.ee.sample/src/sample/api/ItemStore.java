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
package sample.api;

/**
 * Service exported by the sample CDI bean.
 */
public interface ItemStore {
    /**
     * Store an item via the JTA persistence unit and count the stored items.
     *
     * @param name The name of the item
     * @return The number of items stored so far
     */
    long addJta(String name);

    /**
     * Store an item via the resource local persistence unit and count the stored items.
     *
     * @param name The name of the item
     * @return The number of items stored so far
     */
    long addResourceLocal(String name);
}
