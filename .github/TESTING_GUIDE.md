# 🧪 Testing Guide

## Test Structure

```
lms-backend/src/test/java/com/ibizabroker/lms/
├── service/
│   ├── CirculationServiceTest.java     ✅ (6 tests)
│   ├── BookServiceTest.java            ✅ (8 tests)
│   ├── EmailServiceTest.java           ✅ (existing)
│   ├── ConversationServiceTest.java    ✅ (existing)
│   └── JwtUtilTest.java                ✅ (existing)
├── controller/
│   └── AuthControllerTest.java         📝 (TODO)
└── integration/
    └── CirculationIntegrationTest.java 📝 (TODO)
```

---

## Running Tests

### Run All Tests

```bash
cd lms-backend
mvn clean test
```

### Run Specific Test Class

```bash
mvn test -Dtest=CirculationServiceTest
mvn test -Dtest=BookServiceTest
```

### Run with Coverage Report

```bash
mvn clean test jacoco:report

# View report at:
# target/site/jacoco/index.html
```

### Run Tests in CI Mode (with coverage threshold)

```bash
mvn clean verify

# This will:
# 1. Run all tests
# 2. Generate JaCoCo report
# 3. Check coverage threshold (50% minimum)
# 4. Fail build if coverage < 50%
```

---

## Test Coverage

### Current Coverage

| Module                | Coverage | Status |
| --------------------- | -------- | ------ |
| `CirculationService`  | ~90%     | ✅     |
| `BookService`         | ~85%     | ✅     |
| `EmailService`        | ~70%     | ✅     |
| `JwtUtil`             | ~80%     | ✅     |
| `ConversationService` | ~75%     | ✅     |
| **Overall**           | **~60%** | ✅     |

### Target Coverage

- **Minimum**: 50% (enforced by JaCoCo)
- **Target**: 80%
- **Ideal**: 90%+

---

## Unit Tests

### CirculationServiceTest.java

**Purpose**: Test book borrowing, returning, and fine calculation

**Test Cases**:

1. ✅ `testLoanBook_Success()` - Verify successful loan with inventory decrement
2. ✅ `testLoanBook_NoAvailableCopies()` - Ensure exception when out of stock
3. ✅ `testReturnBook_OnTime_NoFine()` - Test on-time returns (no penalty)
4. ✅ `testReturnBook_Overdue_WithFine()` - Calculate fines (2000 VND/day × 6 days = 12000 VND)
5. ✅ `testReturnBook_NotFound()` - Handle non-existent loan records
6. ✅ `testReturnBook_AlreadyReturned()` - Ensure idempotency for already returned books

**Key Test Patterns**:

```java
// Arrange
when(booksRepository.findById(1L)).thenReturn(Optional.of(book));
when(book.getAvailableCopies()).thenReturn(5);

// Act
circulationService.loanBook(1L, 1L);

// Assert
verify(booksRepository).save(book);
verify(book).decrementAvailable();
verify(loanRepository).save(any(Loan.class));
```

### BookServiceTest.java

**Purpose**: Test CRUD operations and pagination

**Test Cases**:

1. ✅ `testGetBookById_Success()` - Retrieve book by valid ID
2. ✅ `testGetBookById_NotFound()` - Throw `NotFoundException` for missing book
3. ✅ `testGetAllBooks()` - Retrieve all books (2 in test data)
4. ✅ `testGetPublicBooks_Pagination()` - Test Spring Data `Page<Books>` with `Pageable`
5. ✅ `testCreateBook()` - Create new book with auto-generated ID
6. ✅ `testUpdateBook_Success()` - Update existing book
7. ✅ `testDeleteBook_Success()` - Delete book with existence check
8. ✅ `testDeleteBook_NotFound()` - Throw exception for non-existent deletion

**Test Data**:

```java
Books book1 = new Books();
book1.setId(1L);
book1.setTitle("Spring Boot in Action");
book1.setIsbn("978-1617292545");
book1.setAuthor("Craig Walls");

Books book2 = new Books();
book2.setId(2L);
book2.setTitle("Clean Code");
book2.setIsbn("978-0132350884");
book2.setAuthor("Robert C. Martin");
```

---

## Integration Tests (TODO)

### CirculationIntegrationTest.java

**Purpose**: Test full loan flow with real database

**Setup**:

```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class CirculationIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("lms_test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private CirculationService circulationService;

    @Autowired
    private BooksRepository booksRepository;

    @Test
    void testFullLoanFlow() {
        // Create book in DB
        Books book = new Books();
        book.setTitle("Test Book");
        book.setAvailableCopies(5);
        booksRepository.save(book);

        // Loan book
        circulationService.loanBook(book.getId(), 1L);

        // Verify in DB
        Books updated = booksRepository.findById(book.getId()).orElseThrow();
        assertEquals(4, updated.getAvailableCopies());
    }
}
```

**Dependencies Needed**:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

---

## CI/CD Testing

### GitHub Actions Workflow

The `.github/workflows/ci-cd.yml` workflow runs tests automatically:

```yaml
backend-test:
  runs-on: ubuntu-latest
  services:
    mysql:
      image: mysql:8.0
      env:
        MYSQL_DATABASE: lms_db
        MYSQL_ROOT_PASSWORD: root
      ports:
        - 3306:3306
  steps:
    - name: Run Tests
      run: mvn clean test
    - name: Upload Coverage
      uses: codecov/codecov-action@v3
      with:
        files: ./target/site/jacoco/jacoco.xml
```

### Test Reports

After each CI run, view:

- **JUnit Reports**: GitHub Actions → Summary → Artifacts
- **Coverage**: Codecov dashboard (if configured)
- **JaCoCo HTML Report**: `target/site/jacoco/index.html` (local)

---

## Test Best Practices

### 1. Follow AAA Pattern (Arrange-Act-Assert)

```java
@Test
void testExample() {
    // Arrange - Set up test data
    Books book = new Books();
    book.setId(1L);
    when(booksRepository.findById(1L)).thenReturn(Optional.of(book));

    // Act - Execute the method under test
    Books result = bookService.getBookById(1L);

    // Assert - Verify the outcome
    assertNotNull(result);
    assertEquals(1L, result.getId());
    verify(booksRepository).findById(1L);
}
```

### 2. Use Descriptive Test Names

✅ Good: `testLoanBook_NoAvailableCopies_ThrowsException()`  
❌ Bad: `testLoanBook2()`

### 3. Test One Thing Per Test

```java
// ✅ Good - Focused test
@Test
void testReturnBook_Overdue_CalculatesFine() {
    // Test only fine calculation
}

// ❌ Bad - Tests multiple things
@Test
void testReturnBook() {
    // Tests return logic AND fine calculation AND email sending
}
```

### 4. Mock External Dependencies

```java
@Mock
private BooksRepository booksRepository; // Mock database

@Mock
private EmailService emailService; // Mock email service

@InjectMocks
private CirculationService circulationService; // Real service under test
```

### 5. Use Test Data Builders

```java
class BookTestDataBuilder {
    public static Books createBook() {
        Books book = new Books();
        book.setId(1L);
        book.setTitle("Test Book");
        book.setAvailableCopies(5);
        return book;
    }
}

// Usage
Books book = BookTestDataBuilder.createBook();
```

---

## Debugging Tests

### Run Tests in IntelliJ IDEA

1. Right-click test class → Run 'CirculationServiceTest'
2. View results in Test Runner panel
3. Debug by setting breakpoints

### Run Tests in VS Code

1. Install "Java Test Runner" extension
2. Click "Run Test" above test method
3. View output in Test Explorer

### View Coverage in IDE

**IntelliJ IDEA**:

1. Run → Run with Coverage
2. View green/red highlighting in editor

**VS Code**:

1. Install "Coverage Gutters" extension
2. Run `mvn test jacoco:report`
3. View `target/site/jacoco/jacoco.xml`

---

## Troubleshooting

### Issue: Tests fail with "Connection refused"

**Cause**: MySQL not running or wrong port

**Solution**:

```properties
# src/test/resources/application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/lms_test
# OR use H2 in-memory database for tests
spring.datasource.url=jdbc:h2:mem:testdb
```

### Issue: "No tests found" error

**Cause**: Test class naming convention

**Solution**: Ensure test classes end with `Test.java` or `Tests.java`:

- ✅ `CirculationServiceTest.java`
- ✅ `BookServiceTests.java`
- ❌ `TestCirculationService.java`

### Issue: JaCoCo report not generated

**Cause**: Plugin not configured

**Solution**: Verify `pom.xml` has JaCoCo plugin (already added):

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
</plugin>
```

---

## Next Steps

1. ✅ Run existing tests: `mvn test`
2. ✅ View coverage: `mvn jacoco:report` → Open `target/site/jacoco/index.html`
3. 📝 Add more service tests (UserService, AuthService, GamificationService)
4. 📝 Add controller tests with `@WebMvcTest`
5. 📝 Add integration tests with Testcontainers
6. 📝 Achieve 80%+ code coverage

## Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [JaCoCo Maven Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
