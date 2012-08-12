package org.ops4j.pax.cdi.sample1.web;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/random")
public class RandomServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;

    @Inject
    private RandomBean randomBean;

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("************* init RandomServlet");
    }

    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
        IOException {
        resp.getOutputStream().println(randomBean.getId());
    }
}
