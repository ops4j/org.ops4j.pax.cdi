/*
 * Copyright 2012 Harald Wellmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.sample1.web.session;

import java.io.IOException;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/invalidate")
public class InvalidateSessionServlet extends HttpServlet {

    @Inject
    private BeanManager beanManager;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        response.setContentType("text/text");

        if (request.getParameter("timeout") != null) {
            SimpleSessionBean.setBeanDestroyed(false);
            request.getSession().setMaxInactiveInterval(
                Integer.parseInt(request.getParameter("timeout")));
        }
        else if (request.getParameter("isBeanConstructed") != null) {
            response.getWriter().print(SimpleSessionBean.isBeanConstructed());
        }
        else if (request.getParameter("isBeanDestroyed") != null) {
            response.getWriter().print(SimpleSessionBean.isBeanDestroyed());
        }
        else {
            SimpleSessionBean.setBeanDestroyed(false);
            request.getSession().invalidate();
        }
    }
}
