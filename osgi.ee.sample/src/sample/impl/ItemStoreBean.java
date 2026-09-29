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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

import osgi.cdi.annotation.Service;
import osgi.cdi.annotation.ServiceReference;
import sample.api.ItemStore;

/**
 * CDI bean, exported as OSGi service, that uses the entity managers registered by the JPA extender.
 */
@ApplicationScoped
@Service(properties = "sample=true")
public class ItemStoreBean implements ItemStore {
    @Inject
    @ServiceReference(filter = "(osgi.unit.name=sample-jta)")
    private EntityManager jta;
    @Inject
    @ServiceReference(filter = "(osgi.unit.name=sample-local)")
    private EntityManager local;
    @Inject
    @ServiceReference
    private UserTransaction transaction;

    private long add(EntityManager manager, String name) {
        try {
            transaction.begin();
            try {
                manager.persist(new Item(name));
                manager.flush();
                long count = manager.createQuery("select count(i) from Item i", Long.class).getSingleResult();
                transaction.commit();
                return count;
            } catch (RuntimeException exc) {
                transaction.rollback();
                throw exc;
            }
        } catch (RuntimeException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    @Override
    public long addJta(String name) {
        return add(jta, name);
    }

    @Override
    public long addResourceLocal(String name) {
        return add(local, name);
    }
}
