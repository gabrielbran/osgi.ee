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

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.UserTransaction;

import sample.api.ItemStore;

/**
 * Uses the OSGi services of the sample bundle and the JPA extender from a web application.
 * GET /items?name=x stores an item and reports the number of items.
 */
@WebServlet("/items")
public class ItemServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    @Inject
    private OsgiServices services;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        String name = request.getParameter("name") == null ? "web" : request.getParameter("name");
        // 1. A service exported by a CDI bean of a bundle (@Service).
        ItemStore store = services.get(ItemStore.class, null);
        out.println("items after insert via ItemStore service: " + store.addJta(name));
        // 2. The entity manager service of the JPA extender. It works with the transaction manager of
        //    the extender, so the transaction is demarcated with its UserTransaction service.
        EntityManager manager = services.get(EntityManager.class, "(osgi.unit.name=sample-jta)");
        UserTransaction transaction = services.get(UserTransaction.class, null);
        try {
            transaction.begin();
            try {
                Object count = manager.createQuery("select count(i) from Item i").getSingleResult();
                out.println("items counted via EntityManager service: " + count);
            } finally {
                transaction.commit();
            }
        } catch (Exception exc) {
            throw new IOException(exc);
        }
    }
}
