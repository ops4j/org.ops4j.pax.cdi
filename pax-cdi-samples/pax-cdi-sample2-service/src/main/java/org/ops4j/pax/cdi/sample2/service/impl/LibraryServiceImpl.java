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
package org.ops4j.pax.cdi.sample2.service.impl;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.deltaspike.jpa.api.transaction.Transactional;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.ops4j.pax.cdi.sample2.service.AuthorDao;
import org.ops4j.pax.cdi.sample2.service.LibraryService;
import org.ops4j.pax.jpa.sample1.model.Author;
import org.ops4j.pax.jpa.sample1.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Harald Wellmann
 *
 */
@Transactional
@OsgiServiceProvider
public class LibraryServiceImpl implements LibraryService {

    private static Logger log = LoggerFactory.getLogger(LibraryServiceImpl.class);

    @Inject
    private AuthorDao authorDao;

    @Inject
    private EntityManager em;

    @Override
    public List<Book> findBooks() {
        log.info("finding books");
        String jpql = "select b from Book b";
        TypedQuery<Book> query = em.createQuery(jpql, Book.class);
        List<Book> books = query.getResultList();
        return books;
    }

    @Override
    public List<Book> findBooksByAuthor(String lastName) {
        String jpql = "select b from Book b where b.author.lastName = :lastName";
        TypedQuery<Book> query = em.createQuery(jpql, Book.class);
        query.setParameter("lastName", lastName);
        List<Book> books = query.getResultList();
        return books;
    }

    @Override
    public List<Book> findBooksByTitle(String title) {
        String jpql = "select b from Book b where b.title = :title";
        TypedQuery<Book> query = em.createQuery(jpql, Book.class);
        query.setParameter("title", title);
        List<Book> books = query.getResultList();
        return books;
    }

    @Override
    public Author createAuthor(String firstName, String lastName) {
        Author author = new Author();
        author.setFirstName(firstName);
        author.setLastName(lastName);
        em.persist(author);
        return author;
    }

    @Override
    public Author createAuthorViaDao(String firstName, String lastName) {
        Author author = new Author();
        author.setFirstName(firstName);
        author.setLastName(lastName);
        return authorDao.save(author);
    }


    @Override
    public Author findAuthor(String firstName, String lastName) {
        String jpql = "select a from Author a where a.firstName = :firstName and a.lastName = :lastName";
        TypedQuery<Author> query = em.createQuery(jpql, Author.class);
        query.setParameter("firstName", firstName);
        query.setParameter("lastName", lastName);
        List<Author> authors = query.getResultList();
        return authors.isEmpty() ? null : authors.get(0);
    }


    @Override
    public Book createBook(String title, Author author) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        author.getBooks().add(book);
        em.persist(book);
        return book;
    }

    @Override
    public long getNumBooks() {
        String jpql = "select count(b) from Book b";
        Long numBooks = (Long) em.createQuery(jpql).getSingleResult();
        return numBooks;
    }

    @Override
    public long getNumAuthors() {
        String jpql = "select count(a) from Author a";
        Long numAuthors = (Long) em.createQuery(jpql).getSingleResult();
        return numAuthors;
    }
}
