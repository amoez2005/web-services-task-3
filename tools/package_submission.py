"""Package coursework source and verified build artifacts without dependency caches."""

from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "submission/student-rest-service.zip"
SOURCE_FILES = [
    "README.md", "pom.xml", "mvnw", "mvnw.cmd", ".gitignore", ".gitattributes",
    "package.json", "package-lock.json",
]
SOURCE_DIRS = [".mvn", "src", "api", "docs", "postman", "scripts", "tools"]
BUILD_DIRS = ["target/reports/apidocs", "target/site/jacoco", "target/surefire-reports", "target/newman"]


def main() -> None:
    """Create the ZIP only after required generated deliverables are present."""
    required = [
        "target/student-rest-service.jar", "target/reports/apidocs/index.html",
        "target/site/jacoco/index.html", "target/newman/results.json", "api/openapi.json",
    ]
    for relative in required:
        if not (ROOT / relative).is_file():
            raise FileNotFoundError(f"Build and run the checks first: missing {relative}")
    files = [ROOT / relative for relative in SOURCE_FILES + ["target/student-rest-service.jar"]]
    for relative in SOURCE_DIRS + BUILD_DIRS:
        files.extend(path for path in (ROOT / relative).rglob("*")
                     if path.is_file() and "__pycache__" not in path.parts)
    OUTPUT.parent.mkdir(exist_ok=True)
    with zipfile.ZipFile(OUTPUT, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(set(files)):
            archive.write(path, "student-rest-service/" + path.relative_to(ROOT).as_posix())
    print(f"Packaged {len(set(files))} files: {OUTPUT} ({OUTPUT.stat().st_size:,} bytes)")


if __name__ == "__main__":
    main()
