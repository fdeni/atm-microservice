# atm-microservice
# ATM Microservice

Ringkasan  
Repositori ini berisi kumpulan microservice berbasis Spring Boot (Java 17) untuk fitur ATM sederhana. Struktur utama yang ditemukan:
- auth-service — layanan otentikasi (JWT + bcrypt)
- account-service — layanan manajemen akun
- transaction-service — layanan pencatatan transaksi

Semua modul menggunakan Maven, Spring Boot, JPA, Lombok, dan MySQL (driver ada di pom.xml tiap modul).

Fitur utama
- Otentikasi dan otorisasi (JWT)
- CRUD & query untuk akun
- Pencatatan transaksi (deposit, withdraw)
- Konfigurasi melalui environment variables
- Mudah dibangun dan dijalankan secara independen per service

Persyaratan
- Java 17 (OpenJDK / Oracle JDK)
- Maven 3.6+
- MySQL (atau gunakan profile in-memory untuk testing jika disediakan)
- (Opsional) Docker & docker-compose
- IDE: pasang plugin Lombok (IDEA/VS Code/Eclipse) agar anotasi Lombok bekerja tanpa error di IDE

Build & jalankan (per modul)
Cara cepat (menjalankan lewat Maven):
1. Masuk ke folder modul:
    - cd auth-service
    - atau cd account-service
    - atau cd transaction-service
2. Build:
   mvn -B clean package
3. Jalankan:
   java -jar target/*.jar

Atau langsung via Spring Boot Maven plugin:
mvn spring-boot:run

Menjalankan semua modul secara paralel (opsional)
Jika repo memiliki parent pom / aggregator (tidak ditemukan di scan awal), Anda dapat membangun tiap modul dan menjalankannya di terminal terpisah. Setiap service perlu port yang berbeda — konfigurasi port ada di application.properties / application.yml tiap modul. Jika tidak ada konfigurasi, tambahkan JVM property saat menjalankan:
java -jar target/*.jar --server.port=8081

Variabel lingkungan (umum)
Setiap service yang terhubung ke database memerlukan konfigurasi datasource. Contoh nama env var yang dapat digunakan:
- SPRING_DATASOURCE_URL (contoh: jdbc:mysql://db:3306/atm_db)
- SPRING_DATASOURCE_USERNAME
- SPRING_DATASOURCE_PASSWORD
- SERVER_PORT (opsional: untuk override default server.port)
  Auth-service khusus:
- JWT_SECRET (secret signing token)
- JWT_EXPIRATION_MS (opsional, lamanya token berlaku)

Contoh .env (sesuaikan):
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/atm
SPRING_DATASOURCE_USERNAME=atm_user
SPRING_DATASOURCE_PASSWORD=secret
JWT_SECRET=replace-with-secure-secret
SERVER_PORT=8081

Contoh endpoint (placeholder — periksa controller actual di kode)
- POST /api/auth/login
    - body: { "username":"user", "password":"pass" }
    - response: { "token":"<jwt>" }
- GET /api/accounts/{accountId}
    - auth: Bearer <token>
- GET /api/accounts/{accountId}/balance
- POST /api/accounts/{accountId}/withdraw
    - body: { "amount": 100.0, "currency":"IDR" }
- POST /api/accounts/{accountId}/deposit
    - body: { "amount": 200.0, "currency":"IDR" }
- GET /api/accounts/{accountId}/transactions

Contoh curl (login lalu cek saldo)
1) Login:
   curl -s -X POST http://localhost:8081/api/auth/login \
   -H "Content-Type: application/json" \
   -d '{"username":"admin","password":"secret"}'

2) Dengan token (ganti <TOKEN>):
   curl -s -X GET http://localhost:8082/api/accounts/123/balance \
   -H "Authorization: Bearer <TOKEN>"

Testing
Jalankan unit test per modul:
cd auth-service && mvn test

License
Tambahkan file LICENSE di repositori jika belum ada. Sesuaikan badge/license sesuai kebutuhan.

Kontak
Pemilik repo: @fdeni (GitHub)

