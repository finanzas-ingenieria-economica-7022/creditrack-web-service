# CrediTrack Web-Service

This project is the backend web-service and REST API for CrediTrack, a vehicle credit simulation platform (Compra Inteligente). The application is built using Java 21, Spring Boot 3.3.0, Spring Security, JWT (JSON Web Tokens), and Hibernate/JPA connected to a local MySQL instance.

---

## Bounded Contexts and DDD Architecture

The system utilizes a modular monolith approach based on Domain-Driven Design (DDD) principles. It is structured into four distinct Bounded Contexts to isolate business capabilities:

1. **IAM (Identity & Access Management)**: Handles user registration, JWT-based secure authentication, and user profile metadata management.
2. **Catalog**: Manages the CRUD endpoints for vehicles, banks/financial entities, and customer information.
3. **Simulation**: The core calculation engine that models the credit schedule using the French Method adapted to the Compra Inteligente modalidad.
4. **Analytics**: Captures simulation metrics asynchronously using Spring domain events and caches aggregate data to serve dashboard statistics.

---

## Database Consolidation (MySQL)

To simplify local administration, all Bounded Contexts are consolidated into a single database named `creditrack_db` on your local MySQL server.
By using `createDatabaseIfNotExist=true` in our JDBC URLs, the database will be created automatically in your MySQL Workbench upon starting the service:
* **Database URL**: `jdbc:mysql://localhost:3306/creditrack_db?createDatabaseIfNotExist=true`

The tables from all contexts will reside in this database, while maintaining the package and transaction boundaries at the code level. Each context still uses its own dedicated connection pool (HikariCP), LocalContainerEntityManagerFactoryBean, and PlatformTransactionManager in their respective infrastructure configs.

---

## Mathematical Simulation Specifications

The simulation engine replicates the exact calculations modeled in the Interbank vehicle credit Excel workbook:

* **Real Loan Amount**: Calculated as:
  `Loan Amount = Vehicle Price - Initial Payment + Capitalized Expenses (Notary, Registration, Appraisal, Study, Activation)`
* **Daily Capitalization**: When a Tasa Nominal Anual (TNA) is supplied, it is converted to a Tasa Efectiva Anual (TEA) assuming a strict 360-day daily capitalization:
  `TEA = (1 + TNA / 360)^360 - 1`
* **Double Parallel Amortization**:
  * **Balloon Payment (Cuotón)**: Starts at `CF / (1 + TEM + pSegDes)^(N+1)` and grows period by period capitalizing interests and desgravamen insurance until it reaches exactly the balloon amount at month N+1.
  * **Regular Amortization**: Evaluates the remaining regular loan balance and calculates regular payments dynamically.
* **Grace Periods**:
  * **Total Grace (T)**: Regular cuota is 0, regular amortization is 0, and interest is capitalized into the loan balance.
  * **Partial Grace (P)**: Regular cuota equals monthly interest, amortization is 0, and loan balance remains constant.
  * **No Grace (S)**: Regular cuota is calculated using the PMT formula with the period rate `TEM + pSegDesPer` over the remaining terms.
* **Profitability Indicators**:
  * **TIR**: Solved via Newton-Raphson numerical analysis on net cash flows (Loan amount as inflow, client payments as outflows).
  * **TCEA**: Calculated by annualizing the monthly TIR: `TCEA = (1 + TIR)^12 - 1`.
  * **VAN**: Net Present Value of cash flows discounted using the period-adjusted Cost of Opportunity Rate (COKi).

---

## Step-by-Step Execution Guide

### Prerequisite Checklist
* Java SDK 21 installed.
* IntelliJ IDEA (Community or Ultimate).
* Local MySQL server running on port `3306` with user `root` and password `12345` (standard local settings).
* Maven Wrapper files (`mvnw` and `mvnw.cmd`) included in the project directory.

### Running inside IntelliJ IDEA (GUI Method)

To run the application using IntelliJ GUI Maven plugins:
1. Open the project folder in IntelliJ IDEA.
2. Open the **Maven** tool window (usually on the right sidebar. If missing, go to `View -> Tool Windows -> Maven`).
3. Click the **Reload All Maven Projects** icon (the circular arrows) at the top-left of the Maven tool window. This downloads the Lombok version 1.18.34, MySQL connector, and other dependencies.
4. Expand `creditrack-web-service` -> **Plugins** -> **spring-boot**.
5. Double-click **`spring-boot:run`**. The application will start on port `8080` and automatically create the 4 databases inside your MySQL Workbench.

### Running with the IntelliJ IDE Run Button (Direct Java Execution)

If you prefer to run the application using the green Play button inside `src/main/java/com/creditrack/CrediTrackApplication.java`:
1. Habilitar annotation processing: Go to `File -> Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors`. Check the box **"Enable annotation processing"** and click Apply.
2. Set Compiler compliance: In Settings, go to `Build, Execution, Deployment -> Compiler -> Java Compiler`. Verify the project module bytecode level is set to `21`.
3. Set Project SDK: Go to `File -> Project Structure -> Project` and select a Java 21 SDK.
4. Click the green Play button next to the `main` method in `CrediTrackApplication.java`.

---

## Testing API Endpoints via Swagger UI

Once the application is running, you can test all endpoints in your browser:
* Swagger UI Page: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* OpenAPI Specifications: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Recommended Testing Order

1. **User Registration**:
   * Use endpoint `POST /api/auth/register` to register a new user. Include user information and profile details.
2. **User Authentication**:
   * Use endpoint `POST /api/auth/login` to login with the registered credentials. Copy the JWT token returned in the JSON response.
3. **Authorize Swagger Request Header**:
   * Click the **Authorize** button on the top-right of the Swagger UI page.
   * Paste the token in the text box using the format: `Bearer <your_token_value_here>` and click Authorize.
4. **CRUD Catalog Entities**:
   * Register a Customer (`POST /api/customers`), a Vehicle (`POST /api/vehicles`), and a Financial Entity (`POST /api/financial-entities`).
5. **Run Simulation**:
   * Run a simulation using `POST /api/simulations` passing the configured IDs and grace periods. Check the returned cronograma and indicators (TCEA, TIR, VAN).
6. **Inspect Dashboard Metrics**:
   * Fetch aggregate data in `GET /api/analytics/dashboard` to verify metrics update automatically.

---

## Test Suite and H2 Fallback Profile

To run tests without requiring a local running MySQL database, the project includes an active test profile:
* **Active Profile**: `test` (configured in `src/test/resources/application-test.yml`)
* **Test Database URL**: In-memory H2 databases (such as `jdbc:h2:mem:iamdb_test`, etc.) configured dynamically for each Bounded Context.
* **Execution**: To run the entire test suite, execute the following command in the project directory:
  ```bash
  ./mvnw.cmd clean test
  ```

---

## Swagger UI Refinement and Custom Configuration

The OpenAPI / Swagger documentation has been customized for a professional look and ease of use:
* **Global JWT Authorization**: Added a global JWT Bearer Security Scheme through `OpenApiConfig`. Clicking the "Authorize" button allows authentication across all secured endpoints.
* **Clean Tagging**: Section names in the Swagger UI have been cleaned of the "Controller" suffix and reorganized with numbered prefixes for logical flow (e.g., `1. Authentication`, `2. Profiles`, `3. Customers`, `4. Vehicles`, `5. Financial Entities`, `6. Simulations`, `7. Analytics Dashboard`).

