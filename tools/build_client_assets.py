"""Regenerate the supplemental OpenAPI metadata and importable Postman assets."""

import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EXAMPLE = {
    "name": "Grace Hopper", "age": 25, "gpa": 3.7, "active": True, "studyGroup": "C",
    "subjects": [{"code": "CS301", "name": "Compilers", "credits": 6}],
}


def write_json(relative_path: str, value: dict) -> None:
    """Write portable, readable JSON inside the project."""
    path = ROOT / relative_path
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def ref(name: str) -> dict:
    """Return an OpenAPI component reference."""
    return {"$ref": f"#/components/schemas/{name}"}


def response(description: str, schema: dict | None = None) -> dict:
    """Describe an optional JSON response body."""
    value = {"description": description}
    if schema is not None:
        value["content"] = {"application/json": {"schema": schema}}
    return value


def build_metadata() -> None:
    """Supply details that are absent from Jersey's generated JSON-oriented WADL."""
    fields = {
        "name": {"type": "string", "minLength": 1, "maxLength": 100, "pattern": r"\S",
                 "description": "Trimmed student name; must contain a non-whitespace character"},
        "age": {"type": "integer", "format": "int32", "minimum": 16, "maximum": 120},
        "gpa": {"type": "number", "format": "float", "minimum": 0, "maximum": 4},
        "active": {"type": "boolean"},
        "studyGroup": {"type": "string", "minLength": 1, "maxLength": 1, "pattern": "^[A-Z]$",
                       "description": "Java char, represented as a single-character JSON string"},
        "subjects": {"type": "array", "items": ref("Subject"),
                     "description": "Required list, possibly empty; subject codes must be unique"},
    }
    schemas = {
        "Subject": {
            "type": "object", "additionalProperties": False, "required": ["code", "name", "credits"],
            "properties": {
                "code": {"type": "string", "minLength": 1, "maxLength": 20, "pattern": r"\S"},
                "name": {"type": "string", "minLength": 1, "maxLength": 100, "pattern": r"\S"},
                "credits": {"type": "integer", "format": "int32", "minimum": 1, "maximum": 30},
            },
        },
        "StudentRequest": {"type": "object", "additionalProperties": False,
                           "required": list(fields), "properties": fields, "example": EXAMPLE},
        "Student": {"type": "object", "additionalProperties": False,
                    "required": ["id", *fields],
                    "properties": {"id": {"type": "integer", "format": "int64", "minimum": 1,
                                          "readOnly": True}, **copy.deepcopy(fields)},
                    "example": {"id": 3, **EXAMPLE}},
        "ApiError": {"type": "object", "required": ["status", "message"],
                     "properties": {"status": {"type": "integer"}, "message": {"type": "string"}},
                     "example": {"status": 404, "message": "Student 999 was not found."}},
    }
    operations = {}
    definitions = [
        ("listStudents", "GET", "/students", "List all students", "200",
         {"type": "array", "items": ref("Student")}),
        ("createStudent", "POST", "/students", "Create a student", "201", ref("Student")),
        ("getStudent", "GET", "/students/{id}", "Get a student", "200", ref("Student")),
        ("updateStudent", "PUT", "/students/{id}", "Replace a student", "200", ref("Student")),
        ("deleteStudent", "DELETE", "/students/{id}", "Delete a student", "204", None),
        ("listSubjects", "GET", "/students/{id}/subjects", "List a student's subjects", "200",
         {"type": "array", "items": ref("Subject")}),
    ]
    for operation_id, method, path, summary, status, schema in definitions:
        responses = {status: response("Success" if status != "204" else "Deleted; no response body", schema)}
        if "{id}" in path or method in {"POST", "PUT"}:
            responses["400"] = response("Invalid identifier or student JSON", ref("ApiError"))
        if "{id}" in path:
            responses["404"] = response("Student not found", ref("ApiError"))
        if method in {"POST", "PUT"}:
            responses["415"] = response("Content-Type must be application/json", ref("ApiError"))
        operation = {"method": method, "path": path, "summary": summary, "responses": responses}
        if method in {"POST", "PUT"}:
            operation["requestBody"] = {"required": True, "content": {
                "application/json": {"schema": ref("StudentRequest"), "example": EXAMPLE}}}
        if method == "POST":
            responses["201"]["headers"] = {"Location": {
                "description": "Absolute URL of the created student", "schema": {"type": "string", "format": "uri"}}}
        operations[operation_id] = operation
    write_json("api/contract-metadata.json", {
        "info": {"title": "Student REST Service", "version": "1.0.0",
                 "description": "JAX-RS coursework API. In-memory storage resets on restart. PUT replaces all editable fields."},
        "components": {"schemas": schemas}, "operations": operations,
    })


def build_postman() -> None:
    """Create a repeatable collection with an isolated create/read/update/delete flow."""
    items = []

    def add(name: str, method: str, path: str, status: int,
            body: dict | str | None = None, checks: list[str] | None = None,
            media_type: str = "application/json") -> None:
        request = {"method": method,
                   "header": [{"key": "Accept", "value": "application/vnd.sun.wadl+xml"
                               if path == "application.wadl" else "application/json"}],
                   "url": "{{baseUrl}}/" + path}
        if body is not None:
            request["header"].append({"key": "Content-Type", "value": media_type})
            request["body"] = {"mode": "raw", "raw": body if isinstance(body, str) else json.dumps(body, indent=2),
                               "options": {"raw": {"language": "json"}}}
        scripts = [f'pm.test("HTTP {status}", () => pm.response.to.have.status({status}));']
        if path != "application.wadl" and status != 204:
            scripts.append('pm.test("JSON response", () => pm.expect(pm.response.headers.get("Content-Type")).to.include("application/json"));')
        if status >= 400:
            scripts.append(f'pm.test("Error contract", () => {{ pm.expect(pm.response.json().status).to.eql({status}); '
                           'pm.expect(pm.response.json().message).to.be.a("string"); });')
        scripts.extend(checks or [])
        items.append({"name": name, "request": request,
                      "event": [{"listen": "test", "script": {"type": "text/javascript", "exec": scripts}}]})

    add("01 - Fetch generated WADL", "GET", "application.wadl", 200, checks=[
        'pm.test("WADL contains students", () => pm.expect(pm.response.text()).to.include("students"));'])
    add("02 - List students", "GET", "students", 200, checks=[
        'pm.test("Student list", () => pm.expect(pm.response.json()).to.be.an("array"));'])
    add("03 - Create student", "POST", "students", 201, EXAMPLE, [
        'const student = pm.response.json(); pm.collectionVariables.set("studentId", String(student.id));',
        'pm.test("Server-assigned ID", () => pm.expect(student.id).to.be.above(0));',
        'pm.test("Location identifies student", () => pm.expect(pm.response.headers.get("Location")).to.eql(pm.variables.get("baseUrl") + "/students/" + student.id));',
        'pm.test("Dependent subject", () => pm.expect(student.subjects[0].code).to.eql("CS301"));'])
    add("04 - Get created student", "GET", "students/{{studentId}}", 200, checks=[
        'pm.test("Correct student", () => pm.expect(pm.response.json().name).to.eql("Grace Hopper"));'])
    add("05 - Get dependent subjects", "GET", "students/{{studentId}}/subjects", 200, checks=[
        'pm.test("Subject list", () => pm.expect(pm.response.json()[0].name).to.eql("Compilers"));'])
    updated = {**EXAMPLE, "name": "Grace Murray Hopper", "active": False, "gpa": 4.0}
    add("06 - Replace student", "PUT", "students/{{studentId}}", 200, updated, [
        'pm.test("Replacement applied", () => { pm.expect(pm.response.json().name).to.eql("Grace Murray Hopper"); pm.expect(pm.response.json().active).to.eql(false); });'])
    add("07 - Reject invalid age", "POST", "students", 400, {**EXAMPLE, "age": 10})
    add("08 - Reject missing required field", "POST", "students", 400,
        {key: value for key, value in EXAMPLE.items() if key != "active"})
    add("09 - Reject malformed JSON", "POST", "students", 400, '{"name":')
    add("10 - Reject wrong media type", "POST", "students", 415, EXAMPLE, media_type="text/plain")
    add("11 - Reject missing student", "GET", "students/9223372036854775807", 404)
    add("12 - Delete created student", "DELETE", "students/{{studentId}}", 204, checks=[
        'pm.test("No response body", () => pm.expect(pm.response.text()).to.eql(""));'])
    add("13 - Confirm deletion", "GET", "students/{{studentId}}", 404)
    write_json("postman/StudentService.postman_collection.json", {
        "info": {"name": "Student REST Service", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
                 "description": "Run in order against the local service. The created student is deleted at the end."},
        "variable": [{"key": "baseUrl", "value": "http://localhost:8085/api"},
                     {"key": "studentId", "value": ""}], "item": items,
    })
    write_json("postman/Local.postman_environment.json", {
        "name": "Student Service - Local", "_postman_variable_scope": "environment",
        "values": [{"key": "baseUrl", "value": "http://localhost:8085/api", "enabled": True}],
    })
    write_json("api/examples/create-student.json", EXAMPLE)
    write_json("api/examples/update-student.json", updated)


if __name__ == "__main__":
    build_metadata()
    build_postman()
    print("Generated API metadata, examples, and Postman collection/environment.")
