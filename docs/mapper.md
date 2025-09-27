


## Mapping values from OpenAPI to GroMoKoSo

| Description                       | OpenApi                                      | GroMoKoSo              |
|-----------------------------------|----------------------------------------------|------------------------|
| Name of Toolset                   | snake_case of `info/title`                   | `name`                 |
| Description of Toolset            | `info/description`                           | `description`          |
| Tool definitionen (API endpoints) | `paths`                                      | `tools`                |
| Description of a tool             | `paths/{PATH}/{METHOD}/summary`              | `{TOOL}summary`        |
| Name of a tool                    | snake_case  of summary (see above)           | `{TOOL}/name`          |
| Endpoint url of a tool            | `servers/url` + `paths/{PATH}` (key of PATH) | `{TOOL}/endpoint`      |
| Request Method of a tool          | `paths/{PATH}/{METHOD}` (key of METHOD)      | `{TOOL}/requestMethod` |


## Schema definition

TOOL_SET
```json
{
  "name": "string",
  "description": "string",
  "tools": "TOOL[]"
}
```

TOOL
```json
{
  "name": "string",
  "description": "string",
  "endpoint": "string",
  "requestMethod": "string",
  "inputSchema": {
    "type": "object"
    "properties": {
      "path": { "type": "object", "properties": {} },
      "query": { "type": "object", "properties": {} },
      "headers": { "type": "object", "properties": {} },
      "body": { "type": "object", "properties": {} }
    }
  }
}
```

**name**
- The name of the tool MUST be unique to the MCP server.
- To ensure this across all toolsets, the value MUST be prefixed with the API_ID and the index of the request in the spec. (`{API_ID}_{INDEX_IN_TOOLSET}_{NAME}`)

**inputSchema**
- The `inputSchema` MUST include all relevant parameters from an HTTP/REST call. These parameter include all relevant header, query (request), path and body parameter.
- Each key of a top level parameter MUST be unique and mapped unambiguously in both directions.
- To ensure this behavior, each parameter MUST be prefixed based on their origin.
- The `inputSchema` MUST be self-contained, meaning all references must be inlined recursively (FIXME: How to prevent infinite recursion?).

| Origin | Prefix |
|--------|--------|
| HEADER | h_     |
| PATH   | p_     |
| QUERY  | q_     |
| BODY   | b_     |


## Links and References

- [OpenAPI Spec](https://spec.openapis.org/oas/latest.html)
- [MCP Schema References](https://modelcontextprotocol.io/specification/2025-06-18/schema)


- [Article: Lessons from complex OpenAPI spec to MCP server conversions](https://www.stainless.com/blog/lessons-from-openapi-to-mcp-server-conversion)
- [Documentation: Convert OpenAPI Specs To MCP Servers](https://www.stainless.com/mcp/convert-openapi-specs-to-mcp-servers)
- [Example Project in JS: OpenAPI to MCP Generator (openapi-mcp-generator)](https://github.com/harsha-iiiv/openapi-mcp-generator)