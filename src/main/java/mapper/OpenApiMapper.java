package mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

public class OpenApiMapper {
    // Shared JSON builder for output
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        // 1) Load OpenAPI JSON from classpath (/test.json)
        InputStream in = OpenApiMapper.class.getResourceAsStream("/test.json");
        if (in == null) throw new IllegalStateException("Could not find test.json in resources.");
        String raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);

        // 2) Parse OpenAPI content
        OpenAPI api = new OpenAPIV3Parser().readContents(raw, null, null).getOpenAPI();
        if (api == null) throw new IllegalArgumentException("Failed to parse OpenAPI.");

        // 3) Determine base URL (first server if present)
        String baseUrl = Optional.ofNullable(api.getServers()).filter(s -> !s.isEmpty())
                .map(s -> s.get(0)).map(Server::getUrl).orElse("");

        // 4) Create toolset envelope (name/description/tools[])
        ObjectNode toolset = MAPPER.createObjectNode();
        toolset.put("name", toSnakeCase(Optional.ofNullable(api.getInfo()).map(Info::getTitle).orElse("toolset")));
        toolset.put("description", "4-in-1 conversion of the OpenAPI spec.");
        ArrayNode tools = MAPPER.createArrayNode();
        toolset.set("tools", tools);

        // 5) Walk all paths → all operations → emit one tool per operation
        if (api.getPaths() != null) {
            api.getPaths().forEach((pathKey, pathItem) -> {
                if (pathItem == null) return;

                // Collect non-null operations for this path
                operationsOf(pathItem).forEach((method, op) -> {
                    if (op == null) return;

                    // Tool identity: name/description/method/endpoint
                    String summary = Optional.ofNullable(op.getSummary())
                            .orElse(Optional.ofNullable(op.getDescription()).orElse(method + " " + pathKey));

                    ObjectNode tool = MAPPER.createObjectNode();
                    tool.put("name", toSnakeCase(summary));
                    tool.put("description", summary);
                    tool.put("requestMethod", method.name());
                    tool.put("endpoint", concat(baseUrl, pathKey));

                    // 6) Build 4-in-1 inputSchema (path, query, headers, body)
                    ObjectNode inputSchema = MAPPER.createObjectNode();
                    ObjectNode properties  = MAPPER.createObjectNode();
                    inputSchema.put("type", "object");
                    ArrayNode  rootRequired = MAPPER.createArrayNode();

                    // Buckets for parameters
                    ObjectNode pathProps = MAPPER.createObjectNode();
                    ObjectNode queryProps = MAPPER.createObjectNode();
                    ObjectNode headerProps = MAPPER.createObjectNode();
                    ArrayNode  pathReq = MAPPER.createArrayNode();

                    // Merge path-level + operation-level parameters
                    List<Parameter> params = new ArrayList<>();
                    if (pathItem.getParameters() != null) params.addAll(pathItem.getParameters());
                    if (op.getParameters() != null) params.addAll(op.getParameters());

                    // 7) Classify parameters by "in": path/query/header
                    for (Parameter p : params) {
                        if (p == null || p.getIn() == null) continue;
                        String name = p.getName();
                        Schema<?> s = p.getSchema();
                        String desc = Optional.ofNullable(p.getDescription()).orElse("string");

                        ObjectNode n = MAPPER.createObjectNode();
                        switch (p.getIn()) {
                            case "path" -> {
                                // Path param includes type + description; mark required names
                                n.put("type", s != null && s.getType() != null ? s.getType() : "string");
                                n.put("description", desc.endsWith("(path)") ? desc : desc + " (path)");
                                pathProps.set(name, n);
                                if (Boolean.TRUE.equals(p.getRequired())) pathReq.add(name);
                            }
                            case "query" -> {
                                // Query param includes type + description
                                n.put("type", s != null && s.getType() != null ? s.getType() : "string");
                                n.put("description", desc);
                                queryProps.set(name, n);
                            }
                            case "header" -> {
                                // Headers: NO "type" per requirement — only description (and possibly enum later)
                                n.put("description", desc);
                                headerProps.set(name, n);
                            }
                        }
                    }

                    // 8) Request body (only application/json recognized)
                    boolean hasJsonBody = op.getRequestBody() != null &&
                            op.getRequestBody().getContent() != null &&
                            op.getRequestBody().getContent().containsKey("application/json");
                    boolean bodyRequired = op.getRequestBody() != null &&
                            Boolean.TRUE.equals(op.getRequestBody().getRequired());

                    ObjectNode bodyNode = null;
                    if (hasJsonBody) {
                        // Resolve and convert body schema
                        Schema<?> bodySchema = resolveSchema(api, preferJsonSchema(op.getRequestBody().getContent()));
                        bodyNode = schemaToBodyObject(api, bodySchema);

                        // Add implicit Content-Type header without "type", but with allowed value enum
                        ObjectNode ct = MAPPER.createObjectNode();
                        ArrayNode en = MAPPER.createArrayNode(); en.add("application/json");
                        ct.set("enum", en);
                        headerProps.set("Content-Type", ct);
                    }

                    // 9) Include non-empty sections; set required flags at root for path/body
                    if (pathProps.size() > 0 || pathReq.size() > 0) {
                        properties.set("path", objWithProps(pathProps, pathReq));
                        if (pathReq.size() > 0) rootRequired.add("path");
                    }
                    if (queryProps.size() > 0) {
                        properties.set("query", objWithProps(queryProps, null));
                    }

                    // Headers: direct map (no {type:"object"} wrapper, and no types per header)
                    if (headerProps.size() > 0) {
                        properties.set("headers", headerProps);
                    }

                    // Body: include only if object-with-props or array
                    if (bodyNode != null) {
                        String t = bodyNode.path("type").asText(null);
                        boolean includeBody = "array".equals(t)
                                || ("object".equals(t) && bodyNode.path("properties").size() > 0);
                        if (includeBody) {
                            properties.set("body", bodyNode);
                            if (bodyRequired) rootRequired.add("body");
                        }
                    }

                    // 10) Finalize schema + attach to tool
                    inputSchema.set("properties", properties);
                    if (rootRequired.size() > 0) inputSchema.set("required", rootRequired);
                    tool.set("inputSchema", inputSchema);

                    // Add tool to collection
                    tools.add(tool);
                });
            });
        }

        // 11) Print resulting toolset JSON
        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(toolset));
    }

    // Helper: wrap props (+ optional required) into a { type:"object", properties, required? } node
    private static ObjectNode objWithProps(ObjectNode props, ArrayNode required) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("type", "object");
        n.set("properties", props == null ? MAPPER.createObjectNode() : props);
        if (required != null && required.size() > 0) n.set("required", required);
        return n;
    }

    // Helper: pick application/json schema if available; else first content schema
    private static Schema<?> preferJsonSchema(Content content) {
        if (content == null || content.isEmpty()) return null;
        MediaType mt = content.get("application/json");
        return mt != null ? mt.getSchema() : content.values().stream().findFirst().map(MediaType::getSchema).orElse(null);
    }

    // Helper: follow $ref → components/schemas to concrete schema (up to 10 hops)
    private static Schema<?> resolveSchema(OpenAPI api, Schema<?> schema) {
        if (schema == null) return null;
        Schema<?> cur = schema;
        for (int i = 0; i < 10 && cur.get$ref() != null; i++) {
            String name = cur.get$ref().substring(cur.get$ref().lastIndexOf('/') + 1);
            Schema<?> next = Optional.ofNullable(api.getComponents())
                    .map(Components::getSchemas).map(m -> m.get(name)).orElse(null);
            if (next == null) break;
            cur = next;
        }
        return cur;
    }

    // Helper: convert OpenAPI schema to compact JSON-schema-like node for body (object/array only)
    private static ObjectNode schemaToBodyObject(OpenAPI api, Schema<?> schema) {
        ObjectNode body = MAPPER.createObjectNode();
        if (schema == null) {
            // Empty object; caller decides whether to include
            body.put("type", "object");
            body.set("properties", MAPPER.createObjectNode());
            return body;
        }
        Schema<?> r = resolveSchema(api, schema);

        // Array body: { type:"array", items:{ type, properties? } }
        if (r instanceof ArraySchema arr) {
            body.put("type", "array");
            Schema<?> items = resolveSchema(api, arr.getItems());
            ObjectNode itemsNode = MAPPER.createObjectNode();
            itemsNode.put("type", items != null && items.getType() != null ? items.getType() : "object");
            if (items != null && "object".equals(items.getType()) && items.getProperties() != null) {
                ObjectNode p = MAPPER.createObjectNode();
                items.getProperties().forEach((k, v) -> p.set(k, fieldNode(resolveSchema(api, (Schema<?>) v))));
                itemsNode.set("properties", p);
            }
            body.set("items", itemsNode);
            return body;
        }

        // Object body: { type:"object", properties:{...} }
        body.put("type", r.getType() == null ? "object" : r.getType());
        ObjectNode p = MAPPER.createObjectNode();
        if (r.getProperties() != null) {
            r.getProperties().forEach((k, v) -> p.set(k, fieldNode(resolveSchema(api, (Schema<?>) v))));
        }
        body.set("properties", p);
        return body;
    }

    // Helper: field node with type/description; arrays include an "items" stub
    private static ObjectNode fieldNode(Schema<?> s) {
        ObjectNode n = MAPPER.createObjectNode();
        if (s instanceof ArraySchema a) {
            n.put("type", "array");
            ObjectNode items = MAPPER.createObjectNode();
            Schema<?> it = a.getItems();
            items.put("type", it != null && it.getType() != null ? it.getType() : "object");
            n.set("items", items);
        } else {
            n.put("type", s != null && s.getType() != null ? s.getType() : "object");
        }
        n.put("description", s != null && s.getDescription() != null ? s.getDescription() : "string");
        return n;
    }

    // Helper: snake_case normalization for names
    private static String toSnakeCase(String s) {
        if (s == null) return "tool";
        String t = s.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        return t.isEmpty() ? "tool" : t;
    }

    // Helper: join base + path without double slash at the boundary
    private static String concat(String base, String path) {
        if (base == null) base = "";
        if (path == null) path = "";
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + path;
    }

    // Helper: collect non-null operations for a PathItem, preserving HTTP method order
    private static Map<PathItem.HttpMethod, Operation> operationsOf(PathItem item) {
        Map<PathItem.HttpMethod, Operation> m = new LinkedHashMap<>();
        Stream.of(PathItem.HttpMethod.values()).forEach(h -> {
            Operation op = switch (h) {
                case GET -> item.getGet();
                case PUT -> item.getPut();
                case POST -> item.getPost();
                case DELETE -> item.getDelete();
                case OPTIONS -> item.getOptions();
                case HEAD -> item.getHead();
                case PATCH -> item.getPatch();
                case TRACE -> item.getTrace();
            };
            if (op != null) m.put(h, op);
        });
        return m;
    }
}
