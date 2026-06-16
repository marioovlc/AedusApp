<div align="center">
  <img src="banner.png" alt="Aedus Banner" width="100%" />
</div>

<div align="center">
  <h1>🎓 AedusApp</h1>
  <p>Desktop application built with JavaFX and PostgreSQL</p>

  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/JavaFX-19.0.2-007396?style=for-the-badge&logo=java&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-42.7.2-336791?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Maven-3.11.0-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white" />
</div>

---

## 📖 About

AedusApp is a desktop application developed in Java using JavaFX. It connects to a PostgreSQL database and provides a modern, responsive UI powered by the AtlantaFX theme and Ikonli icon packs.

---

## ✨ Features

- 🖥️ Modern desktop UI with the **AtlantaFX** theme
- 🗄️ Connection to **PostgreSQL** database via **HikariCP** connection pool
- 🔐 Secure password hashing with **Spring Security Crypto**
- 🎨 Icon support via **Ikonli** (FontAwesome 5 + Ant Design)
- ⚙️ Environment variable management with **dotenv-java**
- 📦 Packaged as a fat JAR with **Maven Shade Plugin**

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Core language |
| JavaFX | 19.0.2 | Desktop UI framework |
| PostgreSQL | 42.7.2 | Database |
| HikariCP | 5.1.0 | Connection pooling |
| AtlantaFX | 2.0.1 | UI theme |
| ControlsFX | 11.2.0 | Extended UI controls |
| Ikonli | 12.3.1 | Icon packs |
| Gson | 2.10.1 | JSON parsing |
| dotenv-java | 3.0.0 | Environment variables |
| Spring Security Crypto | 6.2.1 | Password encryption |
| Logback | 1.5.6 | Logging |

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL database running

### Installation

1. Clone the repository:
```bash
git clone https://github.com/marioovlc/AedusApp.git
cd AedusApp
```

2. Set up environment variables by copying the example file:
```bash
cp .env.example .env
```

3. Edit `.env` with your database credentials:
```env
DB_URL=jdbc:postgresql://localhost:5432/your_database
DB_USER=your_user
DB_PASSWORD=your_password
```

4. Initialize the database:
```bash
mvn exec:java
```

5. Run the application:
```bash
mvn clean javafx:run
```

### Build a JAR

```bash
mvn clean package
java -jar target/AedusApp-1.0-SNAPSHOT.jar
```

---

## 📁 Project Structure

```
AedusApp/
├── src/main/
│   └── java/com/example/aedusapp/
│       ├── MainApp.java          # Application entry point
│       ├── Launcher.java         # JAR launcher
│       └── database/
│           └── DatabaseSetup.java
├── .env.example                  # Environment variable template
├── pom.xml                       # Maven configuration
└── README.md
```

---

## 👤 Author

**Mario Fernández**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white)](https://linkedin.com/in/mario-fernández-9417502a1)
[![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/marioovlc)

---

<div align="center">
  <sub>Built with ☕ Java and passion</sub>
</div>
