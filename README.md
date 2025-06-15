# Payment Application

A Spring Boot web application that integrates with the pay.tech API to process payments.

## Features

- Payment form with amount validation (positive amounts only)
- Integration with pay.tech API using WebClient
- Automatic idempotency key generation (UUID)
- Idempotency key storage
- H2 database for persistence
- Global exception handling with custom error pages
- Form validation with user-friendly error messages
- Responsive web interface using Thymeleaf templates

## Requirements

- Java 21 or higher
- Maven 3.6 or higher

## Running the Application

1. Build the application:
   ```bash
   mvn clean install
   ```

2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

3. Access the application at: http://localhost:8080

## Configuration

The application uses the following key configurations:

- **Database**: H2 file-based database (`jdbc:h2:file:./data/paymentdb`)
- **API**: pay.tech sandbox environment
- **Timeout**: 30 seconds for API calls
- **Validation**: Spring Validation with custom error handling

## API Integration

The application integrates with pay.tech API:
- **Endpoint**: POST https://engine-sandbox.pay.tech/api/v1/payments
- **Authentication**: Bearer token
- **Idempotency**: UUID-based keys for duplicate prevention
- **Error Handling**: Comprehensive exception handling with user-friendly error pages

## Database Schema

The application uses H2 database with the following entities:

**IdempotencyKey Table:**
- `id` - Primary key (auto-generated)
- `key_value` - Unique idempotency key (UUID)
- `created_at` - Timestamp of creation
- `payment_id` - Associated payment ID (when successful)

**H2 Console Access:**
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/paymentdb`
- Username: `sa`
- Password: (empty)

## Testing

Run tests with:
```bash
mvn test
```

## Project Structure

- `src/main/java/com/paytech/payment/`
  - `controller/` - Web controllers (PaymentController)
  - `service/` - Business logic (PaymentService with validation and API integration)
  - `entity/` - JPA entities (IdempotencyKey)
  - `repository/` - Data access layer (IdempotencyKeyRepository)
  - `dto/` - Data transfer objects (PaymentRequest, PaymentApiRequest/Response)
  - `exception/` - Custom exceptions and global exception handler
  - `validator/` - Form validation helpers
  - `enums/` - Enumerations (IdempotencyKeyStatus)
- `src/main/resources/`
  - `templates/` - Thymeleaf templates (payment-form.html, payment-error.html)
  - `application.yml` - Application configuration

## Key Components

- **PaymentController**: Handles web requests and form submission
- **PaymentService**: Core business logic with idempotency handling
- **GlobalExceptionHandler**: Centralized error handling
- **ValidationHelper**: Form validation utilities
- **IdempotencyKey**: Entity for tracking payment request uniqueness