<h1 align="center">
    <br>
    📚 Library Management System
    <br>
</h1>

<p align="center">
  <strong>Full-stack Library Management System với AI Chatbot & Gamification</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.9-brightgreen?style=for-the-badge&logo=spring" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Angular-20.3.12-red?style=for-the-badge&logo=angular" alt="Angular">
  <img src="https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql" alt="MySQL">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Redis-7.0-red?style=for-the-badge&logo=redis" alt="Redis">
  <img src="https://img.shields.io/badge/Security-JWT%20%2B%20BCrypt-yellowgreen?style=for-the-badge&logo=spring-security" alt="Security">
</p>

<p align="center">
  <a href="#live-demo"><strong>🌐 Live Demo</strong></a> •
  <a href="#quick-start"><strong>🚀 Quick Start</strong></a> •
  <a href="#deployment"><strong>☁️ Deploy</strong></a> •
  <a href="#documentation"><strong>📚 Docs</strong></a>
</p>

---

## 🌐 Live Demo

> **🎉 Try it now! No installation required.**

| Environment | URL                                 | Status  |
| ----------- | ----------------------------------- | ------- |
| Frontend    | `https://your-app.vercel.app`       | 🟢 Live |
| Backend API | `https://your-backend.onrender.com` | 🟢 Live |

**Test Accounts:**

- 👨‍💼 Admin: `admin@example.com` / `admin123`
- 👤 User: `user@example.com` / `user123`

> **Note:** Free tier backend may sleep after 15min inactive. First request takes ~30s to wake up.

---

## 🌟 Features Overview

### Core Functionality

- 📖 **Book Management** - CRUD operations for books, categories, authors
- 👥 **User Management** - Admin can manage users, roles, permissions
- 🔄 **Circulation** - Borrow, return, renew books with due date tracking
- 📝 **Reviews & Ratings** - Users can review books and add comments
- 🎯 **Reservations** - Queue system for popular books
- 💰 **Fine Management** - Automatic calculation for overdue books

### Advanced Features

- 🤖 **AI Chatbot** - RAG-powered assistant using Google Gemini API
- 🎮 **Gamification** - Points, badges, reading challenges
- 📊 **Analytics** - Book popularity, user statistics, reports
- 📧 **Email Notifications** - Automated reminders for due dates
- 🔐 **JWT Authentication** - Secure token-based auth with BCrypt
- 🌐 **Multi-role Access** - Admin, User with different permissions
- 📱 **Responsive UI** - Bootstrap 5 with modern design

---

## 🚀 Quick Start

### Option 1: Docker (Recommended - 1 Command!)

```bash
# Clone repository
git clone https://github.com/yourusername/library-management-system.git
cd library-management-system

# Run with Docker Compose (includes MySQL + Redis + Backend + Frontend)
docker-compose -f docker-compose.simple.yml up --build

# 🎉 Done! Open http://localhost
```

**Services:**

- 🌐 Frontend: http://localhost
- ⚙️ Backend: http://localhost:8080
- ❤️ Health: http://localhost:8080/actuator/health

📖 **Full Docker guide:** [START_DOCKER.md](./START_DOCKER.md)

---

### Option 2: Local Development

#### Prerequisites

- Java 21+
- Node.js 18+
- MySQL 8.0+
- Maven 3.9+

#### 1️⃣ Clone Repository

```bash
git clone https://github.com/yourusername/library-management-system.git
cd library-management-system
```

#### 2️⃣ Setup Environment Variables

```bash
# Copy template
cp .env.example .env

# Edit .env with your credentials
# Required: DB_PASSWORD, APP_JWT_SECRET
```

#### 3️⃣ Database Setup

```sql
CREATE DATABASE lms_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lms_db;
SOURCE lms_db_backup.sql;
```

#### 4️⃣ Run Backend

```bash
cd lms-backend
mvn spring-boot:run -Dspring.profiles.active=dev
# Backend: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui/index.html
```

#### 5️⃣ Run Frontend

```bash
cd lms-frontend
npm install
npm start
# Frontend: http://localhost:4200
```

---

## ☁️ Deployment

### 🚀 Deploy to Production (Free Hosting)

| Platform   | Service      | Guide                                     | Cost    |
| ---------- | ------------ | ----------------------------------------- | ------- |
| **Vercel** | Frontend     | [📘 DEPLOY_VERCEL.md](./DEPLOY_VERCEL.md) | 💰 FREE |
| **Render** | Backend + DB | [📙 DEPLOY_RENDER.md](./DEPLOY_RENDER.md) | 💰 FREE |

**One-click setup:**

1. ✅ Push code to GitHub
2. ✅ Connect Vercel → Auto-deploy frontend
3. ✅ Connect Render → Auto-deploy backend
4. 🎉 **Done!** Share your live demo URL

---

## 🔑 Test Credentials

| Role  | Username | Password   |
| ----- | -------- | ---------- |
| Admin | `admin`  | `admin123` |
| User  | `user`   | `user123`  |

---

## 📚 Documentation

| Document                                                      | Description                          |
| ------------------------------------------------------------- | ------------------------------------ |
| [🐳 START_DOCKER.md](./START_DOCKER.md)                       | **Docker one-command setup**         |
| [📘 DEPLOY_VERCEL.md](./DEPLOY_VERCEL.md)                     | **Deploy frontend to Vercel (FREE)** |
| [📙 DEPLOY_RENDER.md](./DEPLOY_RENDER.md)                     | **Deploy backend to Render (FREE)**  |
| [🚀 ENVIRONMENT_SETUP.md](./ENVIRONMENT_SETUP.md)             | Complete setup guide                 |
| [🔒 SECURITY_BEST_PRACTICES.md](./SECURITY_BEST_PRACTICES.md) | Security checklist                   |
| [✅ IMPROVEMENTS_SUMMARY.md](./IMPROVEMENTS_SUMMARY.md)       | Recent improvements                  |
| [📖 START_HERE.md](./START_HERE.md)                           | Project overview                     |
| [⚡ QUICK_REFERENCE.md](./QUICK_REFERENCE.md)                 | API reference                        |

---

## 🏗️ Architecture

```
library-management-system/
├── lms-backend/          # Spring Boot 3.5.9 + Java 21
│   ├── controller/       # REST APIs (@RestController)
│   ├── service/          # Business logic (@Transactional)
│   ├── dao/              # Spring Data JPA repositories
│   ├── entity/           # JPA entities
│   ├── dto/              # Data transfer objects
│   ├── configuration/    # Security, CORS, JWT config
│   └── exceptions/       # Custom exception handlers
│
├── lms-frontend/         # Angular 20.3.12 + TypeScript
│   ├── services/         # HTTP clients
│   ├── auth/             # Guards, interceptors, JWT
│   ├── admin/            # Admin dashboard components
│   └── [features]/       # Feature modules (books, chatbot, etc.)
│
├── docker-compose.yml    # Full stack deployment
├── .env.example          # Environment variables template
└── *.md                  # Documentation files
```

---

## 🔒 Security Features

✅ **No Hardcoded Secrets** - All credentials in environment variables  
✅ **JWT Authentication** - Token-based auth with expiration  
✅ **BCrypt Password Hashing** - Industry-standard encryption  
✅ **Role-Based Access Control** - Admin vs User permissions  
✅ **CORS Protection** - Configurable allowed origins  
✅ **Input Validation** - Jakarta Bean Validation on all DTOs  
✅ **SQL Injection Prevention** - Parameterized queries via JPA  
✅ **Rate Limiting** - Redis-backed request throttling  
✅ **Secure Logging** - No sensitive data in logs

---

## 🛠️ Tech Stack

### Backend

- **Framework:** Spring Boot 3.5.9
- **Language:** Java 21
- **Database:** MySQL 8.0 with utf8mb4
- **ORM:** Hibernate 6 (Spring Data JPA)
- **Security:** Spring Security + JWT
- **Cache:** Redis 7.0
- **AI:** Google Gemini 1.5 Flash API
- **Build:** Maven 3.9

### Frontend

- **Framework:** Angular 20.3.12
- **Language:** TypeScript 5.8
- **UI:** Bootstrap 5 + CSS3
- **State:** RxJS Observables
- **HTTP:** Angular HttpClient
- **Package Manager:** npm / pnpm

### DevOps

- **Containerization:** Docker + Docker Compose
- **Web Server:** Nginx (production)
- **Reverse Proxy:** Tomcat 10 (embedded)
- **Version Control:** Git + GitHub

---

    "bookAuthor": "Author Name",
    "bookGenre": "Genre",
    "noOfCopies": 7

}

```
* Delete Mapping to delete a book.

## User
* Get Mapping to find all users in the database.
* Get Mapping to find user by id provided.
* Post Mapping to create user.
```

{
"username": "user",
"name": "First User",
"password": "password",
"role": [
{
"roleName": "Admin"
}
]
}

```
* Put Mapping to edit user.
```

{
"username": "user",
"name": "New First User",
"password": "password",
"role": [
{
"roleName": "User"
}
]
}

```

## Borrow
* Get Mapping to find all transactions taken place.
* Get Mapping to find list of books borrowed by a user.
* Get Mapping to find list of users who have borrowed a particular book.
* Post Mapping to borrow a book.
```

{
"bookId": 3,
"userId": 5
}

```
* Post Mapping to return a book.
```

{
"borrowId": 1
}

```

<br>

# Screenshots

## Home & Login
### Home
![Home Page](./screenshots/home.png "Home Page")

### Login
![Login Page](./screenshots/login.png "Login Page")

## Admin
### All books present
![Book List Page](./screenshots/book_list.png "Book List Page")

### Adding a book
![Add Book Page](./screenshots/book_add.png "Add Book Page")

### Updating book details
![Update Book Page](./screenshots/book_update.png "Update Book Page")

### Borrow history of a book
![Book Details Page](./screenshots/book_details.png "Book Details Page")

### All users present
![User List Page](./screenshots/user_list.png "User List Page")

### Adding a user
![Add User  Page](./screenshots/user_add.png "Add User Page")

### Borrow history to the user
![User Details Page](./screenshots/user_details.png "User Details Page")

## User
### Borrow book
![Borrow Book](./screenshots/borrow_book.png "Borrow Book Page")

### Return book
![Return Book](./screenshots/return_book.png "Return Book Page")

### Forbidden
![Forbidden](./screenshots/forbidden.png "Forbidden Page")

<br>

# Application Properties
```

server.port = yourPreferredPortNumber

spring.datasource.url = jdbc:mysql://localhost:3306/yourSchemaName
spring.datasource.username = yourUsername
spring.datasource.password = yourPassword

````

# Development
## Requirements
- **Java 21 (LTS)**: The backend requires JDK 21. Ensure `java -version` shows Java 21 before building.

Windows (recommended) quick install examples:

- Using `winget` (if available):

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e
````

- If `winget` is not available, download the Temurin (Adoptium) JDK 21 installer from:
  https://adoptium.net/releases.html and run the installer.

- After installation, set `JAVA_HOME` and update `PATH` (PowerShell example for current session):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
java -version
```

To persist `JAVA_HOME` for all sessions use `setx` (requires a new shell to pick up):

```powershell
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-21" /M
```

Note: This project enforces Java 21 via Maven enforcer. If you attempt to build with a different Java version, the build will fail.

- Frontend

```
npm install
```

- Backend

```
mvn install
```

# Build

- Frontend

```
ng serve
```

- Backend

```
mvn spring-boot:run
```
