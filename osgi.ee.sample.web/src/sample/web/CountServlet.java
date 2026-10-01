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

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Counts the items without demarcating a transaction itself: the TransactionFilter declared in
 * web.xml wraps the request in a transaction of the JPA extender.
 */
@WebServlet("/tx/count")
public class CountServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    @Inject
    private OsgiServices services;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EntityManager manager = services.get(EntityManager.class, "(osgi.unit.name=sample-jta)");
        response.setContentType("text/plain");
        response.getWriter().println("items: " + manager.createQuery("select count(i) from Item i").getSingleResult());
    }
}
