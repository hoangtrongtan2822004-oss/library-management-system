import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { NgForm } from '@angular/forms';
import { BooksService } from '../services/books.service';
import { Book, Author, Category } from '../models/book';
import { forkJoin } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

interface BookCreateModel {
  name?: string;
  numberOfCopiesAvailable?: number;
  publishedYear?: number;
  isbn?: string;
  coverUrl?: string;
  shelfCode?: string;
  description?: string;
  authorIds?: number[];
  categoryIds?: number[];
}

@Component({
  selector: 'app-create-book',
  templateUrl: './create-book.component.html',
  styleUrls: ['./create-book.component.css'],
  standalone: false,
})
export class CreateBookComponent implements OnInit {
  book: BookCreateModel = {
    authorIds: [],
    categoryIds: [],
  };
  errorMessage = '';
  submitting = false;
  showAddCategoryModal = false;
  newCategoryName = '';
  savingCategory = false;
  showAddAuthorModal = false;
  newAuthorName = '';
  savingAuthor = false;
  fetchingIsbn = false;
  isExtractingOcr = false;
  isGeneratingDescription = false;
  coverPreviewError = false;

  allAuthors: Author[] = [];
  allCategories: Category[] = [];
  filteredAuthors: Author[] = [];
  authorSearchQuery = '';

  constructor(
    private booksService: BooksService,
    private router: Router,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    forkJoin({
      authors: this.booksService.getAllAuthors(),
      categories: this.booksService.getAllCategories(),
    }).subscribe({
      next: ({ authors, categories }) => {
        this.allAuthors = authors;
        this.allCategories = categories;
        this.filteredAuthors = [...authors];
      },
      error: (err) => {
        this.errorMessage = 'Không thể tải danh sách tác giả/thể loại.';
        console.error(err);
      },
    });
  }

  fetchBookDataByISBN(): void {
    const isbn = (this.book.isbn || '').trim();
    if (!isbn) {
      this.toastr.warning('Vui lòng nhập mã ISBN trước.');
      return;
    }

    this.fetchingIsbn = true;
    this.errorMessage = '';

    this.booksService.lookupByIsbn(isbn).subscribe({
      next: (response) => {
        if (response?.title) {
          this.book.name = response.title;
        }

        if (response?.publishedYear) {
          this.book.publishedYear = response.publishedYear;
        }

        if (response?.coverUrl) {
          this.book.coverUrl = response.coverUrl;
          this.coverPreviewError = false;
        }

        if (response?.authors && response.authors.length > 0) {
          this.autoSelectOrCreateAuthors(response.authors);
        }

        if (response?.isbn) {
          this.book.isbn = response.isbn;
        }

        this.toastr.success('Đã tải thông tin sách từ Google Books!');
        this.fetchingIsbn = false;
      },
      error: (err) => {
        console.error('ISBN lookup error:', err);
        const message = err?.error?.message || 'Không thể tải dữ liệu ISBN.';
        this.errorMessage = message;
        if (err?.status === 404) {
          this.toastr.warning('Không tìm thấy thông tin sách với ISBN này.');
        } else {
          this.toastr.error(message);
        }
        this.fetchingIsbn = false;
      },
    });
  }

  extractFromImage(event: any): void {
    const file: File = event?.target?.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      this.toastr.error('Vui lòng chọn file ảnh (JPG, PNG, ...).');
      return;
    }
    this.isExtractingOcr = true;
    this.errorMessage = '';
    this.booksService.extractBookInfo(file).subscribe({
      next: (data: any) => {
        if (data?.title) this.book.name = data.title;
        if (data?.isbn) this.book.isbn = data.isbn;
        if (data?.authors?.length > 0)
          this.autoSelectOrCreateAuthors(data.authors);
        if (data?.coverUrl) {
          this.book.coverUrl = data.coverUrl;
          this.coverPreviewError = false;
        }
        this.toastr.success('AI đã trích xuất thông tin sách!');
        this.isExtractingOcr = false;
        // Reset file input
        event.target.value = '';
      },
      error: (err: any) => {
        const message =
          err?.error?.message || 'Không thể trích xuất thông tin.';
        this.errorMessage = message;
        this.toastr.error(message);
        this.isExtractingOcr = false;
        event.target.value = '';
      },
    });
  }

  private autoSelectOrCreateAuthors(authorNames: string[]): void {
    const selectedIds: number[] = [];

    authorNames.forEach((authorName) => {
      // Try to find existing author (case-insensitive)
      const existing = this.allAuthors.find(
        (a) => a.name.toLowerCase() === authorName.toLowerCase(),
      );

      if (existing) {
        selectedIds.push(existing.id);
      } else {
        // Suggest creating new author
        this.toastr.info(
          `Tác giả "${authorName}" chưa có. Hãy thêm thủ công.`,
          '',
          { timeOut: 5000 },
        );
      }
    });

    if (selectedIds.length > 0) {
      this.book.authorIds = selectedIds;
    }
  }

  filterAuthors(query: string): void {
    this.authorSearchQuery = query;
    if (!query.trim()) {
      this.filteredAuthors = [...this.allAuthors];
      return;
    }

    const lowerQuery = query.toLowerCase();
    this.filteredAuthors = this.allAuthors.filter((author) =>
      author.name.toLowerCase().includes(lowerQuery),
    );
  }

  onCoverError(): void {
    this.coverPreviewError = true;
  }

  goToBulkImport(): void {
    this.router.navigate(['/admin']);
    this.toastr.info('Chức năng Import hàng loạt ở trang Admin.');
  }

  generateDescription(): void {
    if (!this.book.name || this.isGeneratingDescription) return;

    const authorNames = (this.book.authorIds || [])
      .map((id) => this.allAuthors.find((a) => a.id === id)?.name)
      .filter((n): n is string => !!n);

    const categoryNames = (this.book.categoryIds || [])
      .map((id) => this.allCategories.find((c) => c.id === id)?.name)
      .filter((n): n is string => !!n);

    this.isGeneratingDescription = true;
    this.booksService
      .previewBookDescription(this.book.name, authorNames, categoryNames)
      .subscribe({
        next: (res: any) => {
          const desc = res?.data?.description || res?.description;
          if (desc) {
            this.book.description = desc;
            this.toastr.success('AI đã tạo mô tả sách!');
          } else {
            this.toastr.warning('AI không trả về mô tả. Vui lòng thử lại.');
          }
          this.isGeneratingDescription = false;
        },
        error: (err: any) => {
          this.toastr.error(
            err?.error?.message || 'Không thể tạo mô tả lúc này.',
          );
          this.isGeneratingDescription = false;
        },
      });
  }

  saveBook(bookForm: NgForm) {
    if (bookForm.invalid) {
      this.errorMessage = 'Please fill in all required fields.';
      return;
    }

    this.errorMessage = '';
    this.submitting = true;

    const payload = {
      name: this.book.name,
      authorIds: this.book.authorIds,
      categoryIds: this.book.categoryIds,
      numberOfCopiesAvailable: this.book.numberOfCopiesAvailable,
      publishedYear: this.book.publishedYear,
      isbn: this.book.isbn,
      coverUrl: this.book.coverUrl,
      shelfCode: this.book.shelfCode,
      description: this.book.description,
    };

    // Cast payload cho đúng kiểu tham số của createBook
    this.booksService.createBook(payload as any).subscribe({
      next: () => {
        this.toastr.success('Tạo sách thành công!');
        this.router.navigate(['/books']);
      },
      error: (err) => {
        console.error(err);
        this.errorMessage =
          err.error?.message || 'An error occurred while creating the book.';
        this.toastr.error(this.errorMessage);
        this.submitting = false;
      },
    });
  }

  openAddCategory(): void {
    this.newCategoryName = '';
    this.showAddCategoryModal = true;
  }

  closeAddCategory(): void {
    if (this.savingCategory) return;
    this.showAddCategoryModal = false;
  }

  saveNewCategory(): void {
    const name = (this.newCategoryName || '').trim();
    if (!name) {
      this.errorMessage = 'Vui lòng nhập tên thể loại.';
      return;
    }
    this.errorMessage = '';
    this.savingCategory = true;
    this.booksService.createCategory(name).subscribe({
      next: (created) => {
        this.allCategories.push(created);
        if (created?.id != null) {
          // Auto-select newly created category
          this.book.categoryIds = [
            ...(this.book.categoryIds || []),
            created.id,
          ];
        }
        this.toastr.success('Tạo thể loại thành công!');
        this.savingCategory = false;
        this.showAddCategoryModal = false;
      },
      error: (err) => {
        const msg = err?.error?.message || 'Không thể tạo thể loại.';
        this.errorMessage = msg;
        this.toastr.error(msg);
        this.savingCategory = false;
      },
    });
  }

  openAddAuthor(): void {
    this.newAuthorName = '';
    this.showAddAuthorModal = true;
  }

  closeAddAuthor(): void {
    if (this.savingAuthor) return;
    this.showAddAuthorModal = false;
  }

  saveNewAuthor(): void {
    const name = (this.newAuthorName || '').trim();
    if (!name) {
      this.errorMessage = 'Vui lòng nhập tên tác giả.';
      return;
    }
    this.errorMessage = '';
    this.savingAuthor = true;
    this.booksService.createAuthor(name).subscribe({
      next: (created) => {
        this.allAuthors.push(created);
        this.filteredAuthors = [...this.allAuthors];
        if (created?.id != null) {
          // Auto-select newly created author
          this.book.authorIds = [...(this.book.authorIds || []), created.id];
        }
        this.toastr.success('Tạo tác giả thành công!');
        this.savingAuthor = false;
        this.showAddAuthorModal = false;
      },
      error: (err) => {
        const msg = err?.error?.message || 'Không thể tạo tác giả.';
        this.errorMessage = msg;
        this.toastr.error(msg);
        this.savingAuthor = false;
      },
    });
  }
}
