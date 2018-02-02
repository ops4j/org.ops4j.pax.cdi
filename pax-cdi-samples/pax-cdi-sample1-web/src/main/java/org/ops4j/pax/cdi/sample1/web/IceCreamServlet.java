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
package org.ops4j.pax.cdi.sample1.web;

import java.io.IOException;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.pax.cdi.api.Component;
import org.ops4j.pax.cdi.api.Dynamic;
import org.ops4j.pax.cdi.api.Filter;
import org.ops4j.pax.cdi.api.Immediate;
import org.ops4j.pax.cdi.api.Service;
import org.ops4j.pax.cdi.sample1.IceCreamService;

@WebServlet(urlPatterns = "/ice")
@Component @Immediate
public class IceCreamServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    @Service @Dynamic @Filter("(flavour=chocolate)")
    private IceCreamService chocolate;

    @Inject
    @Service @Dynamic
    private Instance<IceCreamService> iceCreamServices;

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("************* init IceCreamServlet");
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
        IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        System.out.println(cl);
        resp.getOutputStream().println(chocolate.getFlavour() + " by filter");
        for (IceCreamService service : iceCreamServices) {
            resp.getOutputStream().println("++++ " + service.getFlavour());
        }
    }
}
