# 📄 Document Version Control System

A full-stack web application for managing documents with version control, approval workflows, and role-based access.

Developed as part of a SAP course project.

---

## 🚀 Features

### 👥 Roles
- **AUTHOR**
  - Create documents
  - Create and edit draft versions
  - View document history

- **REVIEWER**
  - Approve or reject versions
  - Add comments

- **READER**
  - View approved versions only

- **ADMIN**
  - Manage users and roles

---

### 📄 Document Management
- Create and manage documents
- Each document contains metadata and version history

---

### 🔄 Version Control
- Versions are created from the latest active version
- Versions are immutable
- Status flow:
  - `DRAFT`
  - `APPROVED`
  - `REJECTED`

---

### ✅ Approval Workflow
- Only approved versions become active
- Rejected versions are preserved in history

---

### 📜 History & Comparison
- Full version history
- Version comparison using **java-diff-utils**

---

### ☁️ File Storage
- Files are uploaded and stored in **AWS S3**

---

### 📄 Export
- Export documents to **PDF (iText)**

---

### 🔐 Security
- JWT Authentication
- Spring Security
- Role-based authorization

---

### 📊 API Documentation
- Swagger UI available

---

## 🏗️ Architecture

- Client-Server architecture
- REST API (Spring Boot)
- Stateless authentication (JWT)
- Cloud storage (AWS S3)
- Database migrations with Flyway

---

## 🛠️ Tech Stack

### 🔙 Backend (Spring Boot)
- Java 17
- Spring Boot 3.3.5
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- AWS SDK (S3)
- JWT (jjwt)
- Swagger (OpenAPI)
- Lombok
- iText PDF
- Java Diff Utils
- Docker

---

### 🎨 Frontend (React + Vite)
- React 18
- TypeScript
- Vite
- React Router
- Axios
- Zustand (state management)
- React Hook Form + Zod (validation)
- Tailwind CSS
- React Quill (rich text editor)
- Lucide Icons
- Sonner (toast notifications)

---

## 📦 Backend Setup

### 🔧 Requirements
- Java 17+
- Docker
- PostgreSQL

---

Environment Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/your_db
spring.datasource.username=postgres
spring.datasource.password=postgres

aws.accessKey=YOUR_KEY
aws.secretKey=YOUR_SECRET
aws.region=YOUR_REGION
aws.bucketName=YOUR_BUCKET

## Frontend Setup
### 🔧 Requirements
- Node Version Manager (nvm)
- Node.js **v20.19.0**
- npm


### ▶️ Run Backend
docker-compose up --build in DocumentSystemBackend/documentsSystem
Backend runs on: http://localhost:8080

### ▶️ Run Frontend
npm install
npm run dev
Frontend runs on: http://localhost:5173

---

##
```md
### ⚠️ Important
Make sure you are using the correct Node.js version.  
Using a different version may cause dependency or build issues.
