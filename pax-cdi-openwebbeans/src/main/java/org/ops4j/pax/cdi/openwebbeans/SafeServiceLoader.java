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
package org.ops4j.pax.cdi.openwebbeans.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Loads service provider instances from {@code META-INF/services} using a given class loader,
 * avoiding the system class loader used by {@code java.util.ServiceLoader}.
 *
 * @author Harald Wellmann
 */
public class SafeServiceLoader
{
    private ClassLoader classLoader;

    /**
     * Constructs a service loader using the given class loader.
     */
    public SafeServiceLoader( ClassLoader classLoader )
    {
        this.classLoader = classLoader;
    }

    /**
     * Returns a list of service instances for the given a service type, finding all
     * META-INF/services resources for the given type and loading and instantiating all classes
     * listed in these resources.
     * <p>
     * A class that cannot be loaded by the class loader passed to the constructor of this class is
     * silently ignored.
     *
     * @param serviceType fully qualified service class name
     * @return list of services matching the given service type
     */
    public <T> List<T> load( String serviceType )
    {
        List<T> services = new ArrayList<T>();
        String resourceName = "/META-INF/services/" + serviceType;
        try
        {
            Enumeration<URL> resources = classLoader.getResources( resourceName );
            while( resources.hasMoreElements() )
            {
                URL url = resources.nextElement();
                List<String> classNames = parse( url );
                for( String className : classNames )
                {
                    Class<T> klass = loadClassIfVisible( className, classLoader );
                    if( klass != null )
                    {
                        T service = klass.newInstance();
                        services.add( service );
                    }
                }
            }
        }
        catch ( Exception exc )
        {
            throw new IllegalStateException( exc );
        }
        return services;
    }

    /**
     * Loads a class with the given name from the given class loader.
     *
     * @param className fully qualified class name
     * @param classLoader class loader
     * @return class with given name, or null
     */
    @SuppressWarnings( "unchecked" )
    private <T> Class<T> loadClassIfVisible( String className, ClassLoader classLoader )
    {
        try
        {
            return (Class<T>) classLoader.loadClass( className );
        }
        catch ( ClassNotFoundException e )
        {
            return null;
        }
    }

    /**
     * Parses a META-INF/services resource and returns the list of service provider class names
     * defined in that resource.
     *
     * @param url a URL of a META-INF/services resource
     * @return list of service class names (not null, but possibly empty)
     */
    private List<String> parse( URL url ) throws IOException
    {
        InputStream is;
        BufferedReader reader = null;
        List<String> names = new ArrayList<String>();
        try
        {
            is = url.openStream();
            reader = new BufferedReader( new InputStreamReader( is, "UTF-8" ) );
            String line;
            while( ( line = reader.readLine() ) != null )
            {
                parseLine( names, line );
            }
        }
        finally
        {
            closeSilently( reader );
        }
        return names;
    }

    /**
     * Closes the given reader, silently ignoring any exception.
     */
    private void closeSilently( BufferedReader reader )
    {
        try
        {
            if( reader != null )
            {
                reader.close();
            }
        }
        catch ( IOException exc )
        {
            // ignore
        }
    }

    /**
     * Parses a single line of a META-INF/services resources. If the line contains a class name, the
     * name is added to the given list.
     *
     * @param names list of class names
     * @param line line to be parsed
     */
    private void parseLine( List<String> names, String line )
    {
        int commentPos = line.indexOf( '#' );
        if( commentPos >= 0 )
        {
            line = line.substring( 0, commentPos );
        }
        line = line.trim();
        if( !line.isEmpty() && !names.contains( line ) )
        {
            names.add( line );
        }
    }
}
