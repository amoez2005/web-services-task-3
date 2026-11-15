/** Validate syntax, references and OpenAPI structure using Swagger Parser. */
const SwaggerParser = require("@apidevtools/swagger-parser");

SwaggerParser.validate("api/openapi.json")
  .then((api) => {
    console.log(`Valid OpenAPI ${api.openapi}: ${api.info.title}`);
  })
  .catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
