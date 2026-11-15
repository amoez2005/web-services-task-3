"""Convert this project's Jersey WADL to OpenAPI 3.0 JSON, without external services.

WADL supplies URLs, operation IDs, HTTP methods and media types. Explicit metadata
supplies JSON models, validation rules and actual response codes that Jersey WADL
cannot infer. This intentionally supports this service's WADL subset, not all WADL.
"""

import argparse
import copy
import json
from pathlib import Path
import re
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

NAMESPACE = {"w": "http://wadl.dev.java.net/2009/02"}
ROOT = Path(__file__).resolve().parents[1]


def convert(wadl: bytes, metadata: dict) -> dict:
    """Build the contract, failing if any endpoint lacks explicit metadata."""
    if b"<!DOCTYPE" in wadl.upper() or b"<!ENTITY" in wadl.upper():
        raise ValueError("DTD and entity declarations are not supported.")
    application = ET.fromstring(wadl)
    resources = application.find("w:resources", NAMESPACE)
    if resources is None or not resources.get("base"):
        raise ValueError("Expected Jersey WADL resources with a base URL.")
    result = {
        "openapi": "3.0.3",
        "info": metadata["info"],
        "servers": [{"url": resources.get("base").rstrip("/")}],
        "tags": [{"name": "Students", "description": "Students and their dependent subjects"}],
        "paths": {},
        "components": metadata["components"],
        "x-generation": "Jersey WADL plus explicit JSON schema and response metadata",
    }
    found = set()

    def visit(resource: ET.Element, parent: str) -> None:
        path = parent.rstrip("/") + "/" + resource.get("path", "").strip("/")
        for method in resource.findall("w:method", NAMESPACE):
            verb = method.get("name", "").lower()
            if verb in {"head", "options"}:
                continue  # Jersey's protocol-generated methods are implicit in OpenAPI.
            operation_id = method.get("id")
            if operation_id not in metadata["operations"]:
                raise ValueError(f"Missing metadata for {verb.upper()} {path}: {operation_id}")
            if operation_id in found:
                raise ValueError(f"Duplicate operation ID: {operation_id}")
            found.add(operation_id)
            operation = copy.deepcopy(metadata["operations"][operation_id])
            expected_method = operation.pop("method").lower()
            expected_path = operation.pop("path")
            if (verb, path) != (expected_method, expected_path):
                raise ValueError(f"WADL and metadata disagree for {operation_id}")
            operation["operationId"] = operation_id
            operation["tags"] = ["Students"]
            parameters = re.findall(r"\{([^}]+)\}", path)
            if parameters:
                operation["parameters"] = [
                    {"name": name, "in": "path", "required": True,
                     "description": "Positive, server-assigned student identifier",
                     "schema": {"type": "integer", "format": "int64", "minimum": 1}}
                    for name in parameters
                ]
            result["paths"].setdefault(path, {})[verb] = operation
        for child in resource.findall("w:resource", NAMESPACE):
            visit(child, path)

    for resource in resources.findall("w:resource", NAMESPACE):
        visit(resource, "")
    missing = set(metadata["operations"]) - found
    if missing:
        raise ValueError(f"WADL is missing operations: {sorted(missing)}")
    return result


def main() -> None:
    """Optionally fetch the running WADL, then write the converted JSON contract."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--wadl", type=Path, default=ROOT / "api/application.wadl")
    parser.add_argument("--metadata", type=Path, default=ROOT / "api/contract-metadata.json")
    parser.add_argument("--output", type=Path, default=ROOT / "api/openapi.json")
    parser.add_argument("--url", help="Fetch WADL from this running endpoint before converting")
    args = parser.parse_args()
    if args.url:
        with urllib.request.urlopen(args.url, timeout=10) as response:
            wadl = response.read()
        # Keep grammar references usable when this WADL is imported from disk.
        # Rewrite only href attributes, preserving namespace declarations in the XML.
        document = ET.fromstring(wadl)
        for index, include in enumerate(document.findall("w:grammars/w:include", NAMESPACE)):
            href = include.get("href")
            if not href:
                continue
            schema_url = urllib.parse.urljoin(args.url, href)
            if urllib.parse.urlsplit(schema_url).netloc != urllib.parse.urlsplit(args.url).netloc:
                raise ValueError("Only same-origin WADL grammar references are supported.")
            with urllib.request.urlopen(schema_url, timeout=10) as response:
                schema = response.read()
            relative_schema = f"schemas/schema-{index}.xsd"
            schema_path = args.wadl.parent / relative_schema
            schema_path.parent.mkdir(parents=True, exist_ok=True)
            schema_path.write_bytes(schema)
            wadl = wadl.replace(f'href="{href}"'.encode(), f'href="{relative_schema}"'.encode())
    else:
        wadl = args.wadl.read_bytes()
    contract = convert(wadl, json.loads(args.metadata.read_text(encoding="utf-8")))
    if args.url:
        args.wadl.parent.mkdir(parents=True, exist_ok=True)
        args.wadl.write_bytes(wadl)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(contract, indent=2) + "\n", encoding="utf-8")
    operation_count = sum(len(methods) for methods in contract["paths"].values())
    print(f"Converted {operation_count} operations to {args.output}")


if __name__ == "__main__":
    main()
