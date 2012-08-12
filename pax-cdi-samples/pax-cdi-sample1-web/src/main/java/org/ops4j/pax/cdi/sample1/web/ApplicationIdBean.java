package org.ops4j.pax.cdi.sample1.web;

import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ApplicationIdBean {
    
    private String id;
    
    @PostConstruct
    public void init() {
        id = UUID.randomUUID().toString();
    }
    
    public String getId() {
        return id;
    }
}
