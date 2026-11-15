# JAX-RS Student REST Service

A complete Java coursework project using **Jakarta RESTful Web Services (JAX-RS), Jersey 3.1.11, and embedded Grizzly**. No separate application server or database is needed. The Java sources target JDK 17 and were verified with the installed JDK 25.

## Build and run

From PowerShell in this directory:

```powershell
.\scripts\build.cmd
.\scripts\run.cmd
```

The Windows launchers select JDK 17+ from `JAVA_HOME` or the installed `javac` on `PATH` and work with this computer's current PowerShell script policy. This handles the default `java` pointing to Java 8. Maven is downloaded by the official Maven wrapper on first use; an internet connection is required for the first build. Optional PowerShell equivalents are also included for environments that permit `.ps1` scripts.

Alternatively, with `JAVA_HOME` already pointing to JDK 17+:

```powershell
.\mvnw.cmd clean verify
& "$env:JAVA_HOME\bin\java.exe" -jar target/student-rest-service.jar
```

- API: **http://localhost:8085/api/students**
- Generated WADL: **http://localhost:8085/api/application.wadl**
- JavaDoc: `target/reports/apidocs/index.html`
- Coverage: `target/site/jacoco/index.html`
- JUnit results: `target/surefire-reports/`

Port 8085 avoids an existing service on this computer's port 8080. Stop the API with Ctrl+C **before rebuilding its JAR**. An alternative address can be passed to `scripts/run.cmd http://localhost:9090/api/`. If the address changes, regenerate OpenAPI and update the Postman environment's `baseUrl`.

Storage is in memory. Restarting resets the two sample students, Ada Lovelace (ID 1) and Alan Turing (ID 2), and all changes. IDs are assigned by the server and are not reused within a run. This is a local educational service without authentication.

## API operations

All student request and response bodies use `application/json`. `PUT` requires a complete replacement body. `id` is output-only; supplying it in request JSON is rejected as an unknown property.

| Method | Path relative to `/api` | Success | Purpose |
| --- | --- | --- | --- |
| GET | `/students` | 200 | List students, ordered by ID |
| GET | `/students/{id}` | 200 | Read a student |
| POST | `/students` | 201 + `Location` | Create a student |
| PUT | `/students/{id}` | 200 | Replace editable fields |
| DELETE | `/students/{id}` | 204, empty body | Delete a student |
| GET | `/students/{id}/subjects` | 200 | Read dependent subjects |

Example create/replace body:

```json
{
  "name": "Grace Hopper",
  "age": 25,
  "gpa": 3.7,
  "active": true,
  "studyGroup": "C",
  "subjects": [
    { "code": "CS301", "name": "Compilers", "credits": 6 }
  ]
}
```

`Student` and `StudentRequest` demonstrate `String`, `int`, `float`, `boolean`, `char`, and `List<Subject>`; `Student` also has a `long` ID. Java records provide immutable fields and accessors with domain names (`name()`, `subjects()`, etc.). JSON represents `char` as a one-character string.

Validation rules: name contains 1–100 characters after trimming; age 16–120; finite GPA 0–4; group A–Z; nonnull subjects list with no null entries or repeated subject codes. Subject code contains 1–20 characters, name 1–100, and credits 1–30. All request fields are required, including `active` and `subjects`; an empty subjects list is valid. Incorrect scalar types, unknown fields, fractional ages, null primitives, malformed JSON, and trailing JSON values are rejected.

Errors have a JSON body such as `{"status":404,"message":"Student 99 was not found."}`. Typical errors are 400 for invalid data, 404 for absent students, 405 for unsupported methods, and 415 for an unsupported request media type.

## Assignment steps 1–4: project, annotations, publication

The Maven project contains domain models, a storage interface, an in-memory implementation, application services, and a JAX-RS HTTP resource. `StudentResource` demonstrates `@Path`, `@GET`, `@POST`, `@PUT`, `@DELETE`, `@PathParam`, `@Consumes`, `@Produces`, and `@Context`. `StudentApplication` publishes these annotated resources through an embedded server. `@Inject` supplies the resource's service dependency. An explicit JAXB runtime enables Jersey's generated WADL on modern JDKs.

## Step 5: SoapUI and WADL

1. Start the service. In SoapUI Open Source choose **File → New REST Project**.
2. Choose **Import WADL** and enter `http://localhost:8085/api/application.wadl`. For a disk import, use `api/application.wadl`; its exported grammar is in `api/schemas/`.
3. Expand `students` and its generated methods. Send a GET to `/api/students` and inspect the HTTP 200 JSON array.
4. For POST, set request media type to `application/json` and paste `api/examples/create-student.json`. Send it, inspect HTTP 201, and copy the returned `id` and `Location`.
5. Read that ID, read its `/subjects`, replace it using `api/examples/update-student.json`, then delete it. Observe 200, 200, 200, and 204. A subsequent GET returns 404.
6. Send a body with age 10 and observe 400. Send JSON as `text/plain` and observe 415.

Use `Accept: application/json` for student operations. For WADL use `Accept: application/vnd.sun.wadl+xml` or omit `Accept`. Jersey correctly returns 406 if a client asks for WADL as JSON.

## Step 6: WADL → OpenAPI JSON

A repeatable local converter is included as the transformation tool:

```powershell
python tools/wadl_to_openapi.py --url http://localhost:8085/api/application.wadl
```

This obtains the **running service's WADL**, saves a portable copy with its grammar, and produces `api/openapi.json` (OpenAPI 3.0.3). To regenerate from the saved WADL without running the service:

```powershell
python tools/wadl_to_openapi.py
```

Jersey WADL describes paths, HTTP methods, IDs, and media types, but does not infer JSON field schemas or the actual 201/204/error responses. `api/contract-metadata.json` explicitly supplies these details. The converter verifies all six operation IDs, paths, and HTTP methods before writing the enriched contract. It supports this application's WADL subset rather than arbitrary WADL documents. `tools/build_client_assets.py` is the source for supplemental metadata, request examples, and the Postman collection.

For the assignment's **APIMatic alternative**, upload `api/application.wadl` to [API Transformer](https://www.apimatic.io/transformer), select OpenAPI JSON, and export it. Compare the six operations to `api/openapi.json`; add any JSON schemas and actual success/error statuses missing from the raw conversion. Do not expect a cloud transformer to reach a `localhost` URL. The committed OpenAPI file was produced by the included local converter, not by APIMatic.

## Step 7: Swagger Editor and Swagger UI

With the Java service running, start a local documentation server from this project directory:

```powershell
python -m http.server 8081 --bind 127.0.0.1
```

Open **http://localhost:8081/docs/swagger-editor.html** for the real Swagger Editor distribution, preloaded with `api/openapi.json`. The left side shows the editable contract and the right side renders the API. Expand GET `/students`, choose **Try it out → Execute**, and inspect the request URL, status, headers, and JSON response. Exercise POST/GET/PUT/DELETE using the returned ID.

**http://localhost:8081/docs/swagger-ui.html** provides the same request exploration in a simpler Swagger UI. These pages load pinned official Swagger packages from a CDN, so browser internet access is required. Their server dropdown points to the Java API, not the Python documentation server.

Alternatively open [Swagger Editor](https://editor.swagger.io/), use **File → Import file** for `api/openapi.json`, and test its operations. The API allows CORS from the official Editor and the local documentation origin. A browser may require permission to access a local service from an HTTPS website; the locally hosted Editor avoids that cross-site situation.

## Step 8: Postman

1. Import `postman/StudentService.postman_collection.json` and `postman/Local.postman_environment.json` into Postman.
2. Select **Student Service - Local** and run the whole collection in order.
3. The collection obtains WADL, lists students, creates its own student, captures the ID, reads subjects, replaces and deletes the student, and verifies error responses.
4. Inspect each response and the **Tests/Test Results** tab. There are **13 requests and 39 assertions**. The collection removes its created student at the end and can be run repeatedly.

The same Postman collection can be executed locally using Postman's Newman runner, without an account:

```powershell
npm.cmd ci --ignore-scripts
npm.cmd run api:validate
npm.cmd run api:test
```

The Java server must already be running for `api:test`. Results are written to `target/newman/results.json` and `target/newman/results.xml`. Node.js 18+ and npm are optional tooling for these checks; they are not needed to build or run the Java API.

## Tests, formatting, JavaDoc, and SOLID

`mvnw.cmd clean verify` checks Java formatting, runs **49 JUnit tests**, builds the executable JAR, generates JavaDoc with all doclint checks and warnings treated as failures, and writes a JaCoCo report. Formatting is enforced by Spotless with the AOSP Google Java Format style. To apply formatting, run `mvnw.cmd spotless:apply` before verification.

Every production class has a corresponding test class. The repository interface is exercised through its implementation's contract tests. Tests cover model fields and validation boundaries, immutable snapshots, concurrent ID allocation, use cases, error mapping, actual HTTP CRUD, strict JSON handling, CORS, application configuration, and published WADL. HTTP tests use dynamically assigned ports and HTTP/1.1, independent of the manual demonstration server. Converter tests are separate:

```powershell
python -m unittest discover -s tools -v
```

SOLID responsibilities are explicit:

- **Single responsibility:** models validate data; the repository stores it; the service implements use cases; the resource adapts HTTP; error and CORS adapters handle their own concerns.
- **Open/closed:** another repository implementation can be supplied without modifying resource or service logic.
- **Liskov substitution:** `StudentRepository` documents ordering, missing results, stable IDs, and atomic updates; the in-memory implementation follows that contract.
- **Interface segregation:** the storage contract exposes only the five operations the service needs.
- **Dependency inversion:** `StudentService` depends on `StudentRepository`; the application composition root selects and injects concrete dependencies.

Actual verification results and the remaining interactive checks are recorded in [docs/verification.md](docs/verification.md).

Create a submission ZIP containing the source, API files, executable JAR, JavaDoc, coverage, and test reports with `python tools/package_submission.py` after building and running the API checks.

## References

- [Jersey user guide, including generated WADL](https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest31x/user-guide.html)
- [SoapUI REST and WADL import](https://www.soapui.org/docs/rest-testing/working-with-rest-services/)
- [APIMatic format transformation](https://docs.apimatic.io/v4/define-validate/transform-formats/)
- [Swagger CORS documentation](https://swagger.io/docs/open-source-tools/swagger-ui/usage/cors/)
- [Postman Newman usage](https://learning.postman.com/docs/reference/newman-cli/installing-running-newman)
- [Apache Maven Wrapper](https://maven.apache.org/tools/wrapper/)
