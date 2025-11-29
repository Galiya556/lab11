using System;
using System.Collections.Generic;
using System.Linq;

namespace SingleFileLibrary
{
    public class Book
    {
        public string ISBN { get; }
        public string Title { get; set; }
        public string Author { get; set; }
        public bool IsAvailable { get; private set; } = true;

        public Book(string isbn, string title, string author)
        {
            ISBN = isbn ?? throw new ArgumentNullException(nameof(isbn));
            Title = title;
            Author = author;
        }

        public void MarkAsLoaned() => IsAvailable = false;
        public void MarkAsAvailable() => IsAvailable = true;
        public override string ToString() => $"{Title} автор: {Author} (ISBN: {ISBN}) - {(IsAvailable ? "Доступна" : "Выдана")}";
    }

    public class Reader
    {
        public Guid Id { get; } = Guid.NewGuid();
        public string Name { get; set; }
        public string Email { get; set; }

        public Reader(string name, string email)
        {
            Name = name;
            Email = email;
        }

        public override string ToString() => $"{Name} ({Email}) [{Id}]";
    }

    public class Librarian
    {
        public string Name { get; set; }
        public string Position { get; set; }

        public Librarian(string name, string position)
        {
            Name = name;
            Position = position;
        }

        public override string ToString() => $"{Name} - {Position}";
    }

    public enum LoanStatus { Active, Completed, Overdue }

    public class Loan
    {
        public Guid Id { get; } = Guid.NewGuid();
        public Book Book { get; set; }
        public Reader Reader { get; set; }
        public DateTime LoanDate { get; set; }
        public DateTime ReturnDate { get; set; }
        public LoanStatus Status { get; private set; } = LoanStatus.Active;
        public Librarian IssuedBy { get; set; }

        public Loan(Book book, Reader reader, DateTime loanDate, DateTime returnDate, Librarian issuedBy)
        {
            Book = book;
            Reader = reader;
            LoanDate = loanDate;
            ReturnDate = returnDate;
            IssuedBy = issuedBy;
        }

        public void Complete()
        {
            if (Status == LoanStatus.Active)
            {
                Status = LoanStatus.Completed;
                Book.MarkAsAvailable();
            }
        }

        public void MarkOverdue() => Status = LoanStatus.Overdue;

        public override string ToString() =>
            $"{Id} : {Book.Title} -> {Reader.Name} с {LoanDate:d} по {ReturnDate:d} [{Status}] (выдан: {IssuedBy?.Name})";
    }

    public interface IRepository
    {
        // Книги
        void AddBook(Book book);
        bool RemoveBook(string isbn);
        Book GetBookByIsbn(string isbn);
        IEnumerable<Book> GetAllBooks();

        // Читатели
        void AddReader(Reader reader);
        bool RemoveReader(Guid readerId);
        Reader GetReader(Guid readerId);
        IEnumerable<Reader> GetAllReaders();

        // Выдачи
        void AddLoan(Loan loan);
        Loan GetLoan(Guid loanId);
        IEnumerable<Loan> GetAllLoans();
    }

    public class InMemoryRepository : IRepository
    {
        private readonly List<Book> _books = new();
        private readonly List<Reader> _readers = new();
        private readonly List<Loan> _loans = new();

        // Книги
        public void AddBook(Book book) => _books.Add(book);

        public bool RemoveBook(string isbn)
        {
            var b = _books.FirstOrDefault(x => x.ISBN == isbn);
            if (b == null) return false;
            _books.Remove(b);
            return true;
        }

        public Book GetBookByIsbn(string isbn) => _books.FirstOrDefault(x => x.ISBN == isbn);
        public IEnumerable<Book> GetAllBooks() => _books;

        // Читатели
        public void AddReader(Reader reader) => _readers.Add(reader);

        public bool RemoveReader(Guid readerId)
        {
            var r = _readers.FirstOrDefault(x => x.Id == readerId);
            if (r == null) return false;
            _readers.Remove(r);
            return true;
        }

        public Reader GetReader(Guid readerId) => _readers.FirstOrDefault(x => x.Id == readerId);
        public IEnumerable<Reader> GetAllReaders() => _readers;

        // Выдачи
        public void AddLoan(Loan loan)
        {
            _loans.Add(loan);
            loan.Book.MarkAsLoaned();
        }

        public Loan GetLoan(Guid loanId) => _loans.FirstOrDefault(x => x.Id == loanId);
        public IEnumerable<Loan> GetAllLoans() => _loans;
    }

    public class LibraryService
    {
        private readonly IRepository _repo;
        public LibraryService(IRepository repo) { _repo = repo; }

        // Книги
        public void AddBook(Book book) => _repo.AddBook(book);
        public bool RemoveBook(string isbn) => _repo.RemoveBook(isbn);
        public Book GetBookByIsbn(string isbn) => _repo.GetBookByIsbn(isbn);
        public IEnumerable<Book> SearchBooksByTitle(string title) =>
            _repo.GetAllBooks().Where(b => b.Title?.IndexOf(title ?? "", StringComparison.OrdinalIgnoreCase) >= 0);

        public IEnumerable<Book> SearchBooksByAuthor(string author) =>
            _repo.GetAllBooks().Where(b => b.Author?.IndexOf(author ?? "", StringComparison.OrdinalIgnoreCase) >= 0);

        // Читатели
        public void AddReader(Reader r) => _repo.AddReader(r);
        public bool RemoveReader(Guid readerId) => _repo.RemoveReader(readerId);
        public Reader GetReader(Guid id) => _repo.GetReader(id);

        // Выдачи
        public Loan IssueLoan(string isbn, Guid readerId, DateTime loanDate, DateTime returnDate, Librarian issuedBy)
        {
            var book = _repo.GetBookByIsbn(isbn);
            if (book == null)
            {
                Console.WriteLine("Книга не найдена.");
                return null;
            }
            if (!book.IsAvailable)
            {
                Console.WriteLine("Книга в настоящее время недоступна.");
                return null;
            }

            var reader = _repo.GetReader(readerId);
            if (reader == null)
            {
                Console.WriteLine("Читатель не найден.");
                return null;
            }

            var loan = new Loan(book, reader, loanDate, returnDate, issuedBy);
            _repo.AddLoan(loan);
            return loan;
        }

        public bool ReturnBook(Guid loanId)
        {
            var loan = _repo.GetLoan(loanId);
            if (loan == null)
            {
                Console.WriteLine("Выдача не найдена.");
                return false;
            }
            if (loan.Status != LoanStatus.Active)
            {
                Console.WriteLine("Выдача не активна.");
                return false;
            }
            loan.Complete();
            return true;
        }

        public IEnumerable<Loan> GetAllLoans() => _repo.GetAllLoans();

        // Отчёты
        public IEnumerable<Book> GetAvailableBooks() => _repo.GetAllBooks().Where(b => b.IsAvailable);
        public IEnumerable<Book> GetLoanedBooks() => _repo.GetAllBooks().Where(b => !b.IsAvailable);
    }

    class Program
    {
        static void Main(string[] args)
        {
            // Настройка репозитория и сервиса
            var repo = new InMemoryRepository();
            var library = new LibraryService(repo);

            // Библиотекарь
            var librarian = new Librarian("Елена Смирнова", "Главный библиотекарь");

            // Добавление книг
            library.AddBook(new Book("978-0140449136", "Одиссея", "Гомер"));
            library.AddBook(new Book("978-0261102217", "Хоббит", "Дж. Р. Р. Толкин"));
            library.AddBook(new Book("978-0131103627", "Язык программирования C", "Керниган и Ричи"));

            // Добавление читателя
            var reader = new Reader("Иван Петров", "ivan.petrov@example.com");
            library.AddReader(reader);

            Console.WriteLine("=== Изначальные книги ===");
            foreach (var b in library.GetAvailableBooks()) Console.WriteLine(" - " + b);

            // Поиск по названию
            Console.WriteLine("\nПоиск 'Хоббит':");
            var found = library.SearchBooksByTitle("Хоббит");
            foreach (var f in found) Console.WriteLine(" Найдена: " + f);

            // Выдача книги
            Console.WriteLine("\nВыдача 'Хоббит' Ивану...");
            var bookToLoan = library.GetBookByIsbn("978-0261102217");
            var loan = library.IssueLoan(bookToLoan.ISBN, reader.Id, DateTime.UtcNow, DateTime.UtcNow.AddDays(14), librarian);
            if (loan != null) Console.WriteLine(" Выдача создана: " + loan);

            // Попытка выдать ту же книгу снова
            Console.WriteLine("\nПопытка выдать ту же книгу снова:");
            var loan2 = library.IssueLoan(bookToLoan.ISBN, reader.Id, DateTime.UtcNow, DateTime.UtcNow.AddDays(14), librarian);
            Console.WriteLine(loan2 == null ? " Невозможно выдать: книга недоступна." : "Выдана снова (неожиданно).");

            // Отчёты
            Console.WriteLine("\n=== Выданные книги ===");
            foreach (var lb in library.GetLoanedBooks()) Console.WriteLine(" - " + lb);

            Console.WriteLine("\n=== Все выдачи ===");
            foreach (var L in library.GetAllLoans()) Console.WriteLine(" - " + L);

            // Возврат книги
            Console.WriteLine("\nВозврат книги...");
            library.ReturnBook(loan.Id);
            Console.WriteLine("После возврата - доступность:");
            foreach (var b in library.GetAvailableBooks()) Console.WriteLine(" - " + b);

            // Дополнительные операции: добавить/удалить читателя
            Console.WriteLine("\nДобавление ещё одного читателя и удаление его:");
            var r2 = new Reader("Ольга Иванова", "olga@example.com");
            library.AddReader(r2);
            Console.WriteLine(" Кол-во читателей после добавления: " + repo.GetAllReaders().Count());
            bool removed = library.RemoveReader(r2.Id);
            Console.WriteLine(" Удалён? " + removed + ". Кол-во читателей теперь: " + repo.GetAllReaders().Count());

            // Поиск по автору
            Console.WriteLine("\nПоиск по автору 'Гомер':");
            foreach (var a in library.SearchBooksByAuthor("Гомер")) Console.WriteLine(" - " + a);

            Console.WriteLine("\nДемонстрация завершена.");
        }
    }
}
