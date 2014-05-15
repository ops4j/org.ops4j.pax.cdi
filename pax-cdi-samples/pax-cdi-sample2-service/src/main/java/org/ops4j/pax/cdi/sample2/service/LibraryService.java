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
package org.ops4j.pax.cdi.sample2.service;

import java.util.List;

import org.ops4j.pax.jpa.sample1.model.Author;
import org.ops4j.pax.jpa.sample1.model.Book;


/**
 * 
 * @author Harald Wellmann
 * 
 */
public interface LibraryService {
    
    List<Book> findBooks();
    List<Book> findBooksByAuthor(String lastName);
    List<Book> findBooksByTitle(String title);
    Author createAuthor(String firstName, String lastName);
    Author createAuthorViaDao(String firstName, String lastName);
    Author findAuthor(String firstName, String lastName);
    Book createBook(String title, Author author);
    long getNumBooks();
    long getNumAuthors();
}
