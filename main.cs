using System;
using System.Collections.Generic;

namespace LibrarySystem
{
    public class Book
    {
        public string Title  { get; private set; }
        public string Author { get; private set; }
        public string Isbn   { get; private set; }
        public bool IsAvailable { get; private set; } = true;

        public Book(string title, string author, string isbn)
        {
            Title  = title;
            Author = author;
            Isbn   = isbn;
        }

        public void MarkAsLoaned()
        {
            IsAvailable = false;
        }

        public void MarkAsAvailable()
        {
            IsAvailable = true;
        }
    }

    public class Reader
    {
        public int Id     { get; private set; }
        public string Name  { get; private set; }
        public string Email { get; private set; }

        public Reader(int id, string name, string email)
        {
            Id    = id;
            Name  = name;
            Email = email;
        }

        public Loan BorrowBook(Book book, Librarian librarian, Library library)
        {
            return library.CreateLoan(book, this, librarian);
        }
    }

    public class Librarian
    {
        public int Id        { get; private set; }
        public string Name   { get; private set; }
        public string Position { get; private set; }

        public Librarian(int id, string name, string position)
        {
            Id       = id;
            Name     = name;
            Position = position;
        }

        public void AddBook(Book book, Library library)
        {
            library.AddBook(book);
        }

        public void RemoveBook(Book book, Library library)
        {
            library.RemoveBook(book);
        }

        public void RegisterReader(Reader reader, Library library)
        {
            library.RegisterReader(reader);
        }
    }

    public class Loan
    {
        public int Id         { get; private set; }
        public Book Book      { get; private set; }
        public Reader Reader  { get; private set; }
        public Librarian Librarian { get; private set; }
        public DateTime LoanDate   { get; private set; }
        public DateTime? ReturnDate { get; private set; }
        public bool IsCompleted    { get; private set; }

        public Loan(int id, Book book, Reader reader, Librarian librarian)
        {
            Id        = id;
            Book      = book;
            Reader    = reader;
            Librarian = librarian;
        }

        public void IssueLoan()
        {
            LoanDate = DateTime.Now;
            Book.MarkAsLoaned();
        }

        public void CompleteLoan()
        {
            ReturnDate  = DateTime.Now;
            IsCompleted = true;
            Book.MarkAsAvailable();
        }
    }

    public class Library
    {
        private readonly List<Book> _books      = new();
        private readonly List<Reader> _readers  = new();
        private readonly List<Librarian> _librarians = new();
        private readonly List<Loan> _loans      = new();

        public IReadOnlyList<Book> Books       => _books;
        public IReadOnlyList<Reader> Readers   => _readers;
        public IReadOnlyList<Librarian> Librarians => _librarians;
        public IReadOnlyList<Loan> Loans       => _loans;

        public void AddBook(Book book) => _books.Add(book);
        public void RemoveBook(Book book) => _books.Remove(book);

        public void RegisterReader(Reader reader) => _readers.Add(reader);

        public List<Book> FindBooksByTitle(string title)
        {
            return _books.FindAll(b => b.Title.Contains(title, StringComparison.OrdinalIgnoreCase));
        }

        public List<Book> FindBooksByAuthor(string author)
        {
            return _books.FindAll(b => b.Author.Contains(author, StringComparison.OrdinalIgnoreCase));
        }

        public Loan CreateLoan(Book book, Reader reader, Librarian librarian)
        {
            if (!book.IsAvailable)
                throw new InvalidOperationException("Книга уже выдана.");

            var loan = new Loan(_loans.Count + 1, book, reader, librarian);
            loan.IssueLoan();
            _loans.Add(loan);
            return loan;
        }
    }
}
