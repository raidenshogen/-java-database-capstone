# Smart Clinic Management System - Architecture Documentation

## Section 1: Architecture Summary

The Smart Clinic Management System is built on a three-tier Spring Boot architecture that
cleanly separates presentation, application logic, and data persistence. The presentation
tier serves two types of clients: server-rendered Thymeleaf dashboards for Admin and Doctor
users, and JSON-based REST API consumers for modules like Appointments, Patient Dashboard,
and Patient Records. Both client types route their requests into the Spring Boot application
tier, where Thymeleaf Controllers handle HTML view rendering and REST Controllers handle
JSON request/response cycles. Regardless of entry point, all controllers delegate business
logic to a single, shared Service Layer, which enforces validation rules, coordinates
workflows, and keeps a clean separation between request handling and data access. The
Service Layer then interacts with two distinct repository types based on data nature:
Spring Data JPA repositories manage structured, relational data (Patient, Doctor,
Appointment, Admin) stored in MySQL, while Spring Data MongoDB repositories manage
flexible, document-based data (Prescription) stored in MongoDB. This dual-database strategy
allows the system to leverage strong consistency and relational integrity where needed,
while also supporting schema flexibility for evolving data like prescriptions. The layered
design improves maintainability, testability, and scalability, since each tier and each
database integration can evolve independently without breaking the others.

## Section 2: Numbered Flow

1. **User Interaction** — The user interacts with the system either through a Thymeleaf-based
   web dashboard (Admin Dashboard, Doctor Dashboard) or through a REST API client (mobile app,
   Appointments module, Patient Dashboard, Patient Records module).

2. **Request Routing** — The incoming HTTP request is routed based on URL pattern and HTTP
   method to the appropriate controller: Thymeleaf Controllers for server-rendered views, or
   REST Controllers for JSON-based API calls.

3. **Controller Delegation** — The controller (Thymeleaf or REST) receives the request, performs
   minimal processing, and delegates the actual business logic to the Service Layer.

4. **Business Logic Execution** — The Service Layer applies business rules and validations
   (e.g., checking doctor availability), coordinates any multi-entity workflows, and determines
   which repository/repositories need to be accessed.

5. **Repository Call** — The Service Layer calls the appropriate repository: MySQL Repositories
   (via Spring Data JPA) for structured entities like Patient, Doctor, Appointment, and Admin,
   or the MongoDB Repository (via Spring Data MongoDB) for document-based Prescription data.

6. **Database Access** — Each repository communicates directly with its underlying database
   engine — MySQL Database for relational data, or MongoDB Database for document-based data —
   to perform CRUD operations.

7. **Model Binding** — Data retrieved from the database is mapped into Java objects: MySQL rows
   are bound into JPA Entity classes (annotated with `@Entity`), while MongoDB documents are
   bound into Document model classes (annotated with `@Document`).

8. **Response Construction** — The bound models are passed back up through the Service Layer
   to the controller. In the MVC flow, Thymeleaf Controllers pass the model to an HTML template,
   which is rendered into dynamic content for the browser. In the REST flow, REST Controllers
   serialize the model (or a DTO) into JSON and return it as the HTTP response body to the client.