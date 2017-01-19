/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ops4j.pax.cdi.weld.impl.util;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;

import org.osgi.framework.*;

/**
 * BundleContext for DelegatingBundle. 
 * 
 * @version $Rev: 1308440 $ $Date: 2012-04-02 10:55:44 -0700 (Mon, 02 Apr 2012) $
 */
class DelegatingBundleContext implements BundleContext {

    private DelegatingBundle bundle;
    private BundleContext bundleContext;
    
    DelegatingBundleContext(DelegatingBundle bundle, BundleContext bundleContext) {
        this.bundle = bundle;
        this.bundleContext = bundleContext;
    }
    
    public Bundle getBundle() {
        return bundle;
    }
        
    public void addBundleListener(BundleListener arg0) {
        bundleContext.addBundleListener(arg0);
    }

    public void addFrameworkListener(FrameworkListener arg0) {
        bundleContext.addFrameworkListener(arg0);
    }

    public void addServiceListener(ServiceListener arg0, String arg1) throws InvalidSyntaxException {
        bundleContext.addServiceListener(arg0, arg1);
    }

    public void addServiceListener(ServiceListener arg0) {
        bundleContext.addServiceListener(arg0);
    }

    public Filter createFilter(String arg0) throws InvalidSyntaxException {
        return bundleContext.createFilter(arg0);
    }

    public Bundle getBundle(long arg0) {
        return bundleContext.getBundle(arg0);
    }

    public Bundle[] getBundles() {
        return bundleContext.getBundles();
    }

    public File getDataFile(String arg0) {
        return bundleContext.getDataFile(arg0);
    }

    public String getProperty(String arg0) {
        return bundleContext.getProperty(arg0);
    }

    public <S> S getService(ServiceReference<S> reference) {
        return bundleContext.getService(reference);
    }

    public ServiceReference<?> getServiceReference(String clazz) {
        return bundleContext.getServiceReference(clazz);
    }

    public ServiceReference<?>[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        return bundleContext.getServiceReferences(clazz, filter);
    }

    public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
        return bundleContext.getServiceReference(clazz);
    }

    public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter) throws InvalidSyntaxException {
        return bundleContext.getServiceReferences(clazz, filter);
    }
    
    public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        return bundleContext.getAllServiceReferences(clazz, filter);
    }
    
    public Bundle installBundle(String arg0, InputStream arg1) throws BundleException {
        return bundleContext.installBundle(arg0, arg1);
    }

    public Bundle installBundle(String arg0) throws BundleException {
        return bundleContext.installBundle(arg0);
    }

    public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties) {
        return bundleContext.registerService(clazz, service, properties);
    }

    public ServiceRegistration<?> registerService(String[] classes, Object service, Dictionary<String, ?> properties) {
        return bundleContext.registerService(classes, service, properties);
    }
    
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
        return bundleContext.registerService(clazz, service, properties);
    }

    public void removeBundleListener(BundleListener arg0) {
        bundleContext.removeBundleListener(arg0);
    }

    public void removeFrameworkListener(FrameworkListener arg0) {
        bundleContext.removeFrameworkListener(arg0);
    }

    public void removeServiceListener(ServiceListener arg0) {
        bundleContext.removeServiceListener(arg0);
    }

    public boolean ungetService(ServiceReference<?> reference) {
        return bundleContext.ungetService(reference);
    }

    public Bundle getBundle(String location) {
        return bundleContext.getBundle(location);
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, ServiceFactory<S> factory, Dictionary<String, ?> properties) {
        return bundleContext.registerService(clazz, factory, properties);
    }

    @Override
    public <S> ServiceObjects<S> getServiceObjects(ServiceReference<S> reference) {
        return bundleContext.getServiceObjects(reference);
    }
}
