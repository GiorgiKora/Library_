CREATE DATABASE IF NOT EXISTS library;
\c library

CREATE TABLE books (
    code VARCHAR(255) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL
);

CREATE TABLE members (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE borrowings (
    book_code VARCHAR(255) REFERENCES books(code) NOT NULL,
    member_id INTEGER REFERENCES members(id) NOT NULL,
    borrow_date DATE NOT NULL,
    return_date DATE,
    PRIMARY KEY (book_code, member_id, borrow_date)
);