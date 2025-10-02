package de.thm.spec2tool.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.thm.spec2tool.dto.ToolSetDto;
import de.thm.spec2tool.exception.ConversionException;
import de.thm.spec2tool.service.ConversionServiceImpl;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

@Component
public class OpenApiMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    Logger logger = LoggerFactory.getLogger(ConversionServiceImpl.class);

    /**
     * Convert an API spec into a tool specification.
     * @param spec The API specification as Map of Strings to Objects.
     * @return A ToolSpecification object.
     */
    public ToolSetDto convert(Map<String, Object> spec) throws JsonProcessingException {
        logger.info("====== Start to convert spec to tool ======");
        logger.debug("Convert spec to raw string...");
        String raw = MAPPER.writeValueAsString(spec);

        logger.debug("Parse spec to OpenApi object format...");
        OpenAPI api = new OpenAPIV3Parser().readContents(raw, null, null).getOpenAPI();
        if (api == null) {
            logger.error("Failed to parse spec to OpenApi obejct!");
            throw new IllegalArgumentException("Failed to parse OpenAPI.");
        }

        logger.debug("Extract URLs from spec and set first as base URL...");
        String baseUrl = Optional.ofNullable(api.getServers()).filter(s -> !s.isEmpty())
                .map(s -> s.get(0)).map(Server::getUrl).orElse("");

        logger.debug("Set head entries: 'name', 'description' and 'toolset'...");
        ObjectNode toolset = MAPPER.createObjectNode();
        toolset.put("name", toSnakeCase(Optional.ofNullable(api.getInfo()).map(Info::getTitle).orElse("toolset")));
        toolset.put("description", toSnakeCase(Optional.ofNullable(api.getInfo()).map(Info::getDescription).orElse("")));
        ArrayNode tools = MAPPER.createArrayNode();
        toolset.set("tools", tools);

        if (api.getPaths() != null) {
            api.getPaths().forEach((pathKey, pathItem) -> {
                if (pathItem == null) {
                    logger.error("No corresponding path for key {}... abort", pathKey);
                    throw new ConversionException("No corresponding path for key " + pathKey);
                }

                // Collect non-null operations for this path
                operationsOf(pathItem).forEach((httpMethod, operation) -> {
                    if (operation == null) {
                        logger.error("No operation for HTTP Method {}, abort...", httpMethod);
                        throw new ConversionException("No Operation defined for defined HTTP Method " + httpMethod);
                    }

                    String summary = Optional.ofNullable(operation.getSummary())
                            .orElse(Optional.ofNullable(operation.getDescription()).orElse(httpMethod + " " + pathKey));

                    logger.info("Add tool with following attributes: name: {}\ndescription: {}\nrequestMethod: {}\nendpoint: {}",
                            toSnakeCase(summary), summary, httpMethod.name(), concat(baseUrl, pathKey));

                    ObjectNode tool = MAPPER.createObjectNode();
                    tool.put("name", toSnakeCase(summary));
                    tool.put("description", summary);
                    tool.put("requestMethod", httpMethod.name());
                    tool.put("endpoint", concat(baseUrl, pathKey));

                    // Create nodes for mandatory fields inputSchema, properties and required
                    ObjectNode inputSchema = MAPPER.createObjectNode();
                    inputSchema.put("type", "object");
                    ObjectNode properties  = MAPPER.createObjectNode();
                    ArrayNode rootRequired = MAPPER.createArrayNode();

                    // Buckets for parameters
                    ObjectNode pathProps = MAPPER.createObjectNode();
                    ObjectNode queryProps = MAPPER.createObjectNode();
                    ObjectNode headerProps = MAPPER.createObjectNode();
                    ArrayNode pathReq = MAPPER.createArrayNode();

                    // Merge path-level + operation-level parameters
                    List<Parameter> params = new ArrayList<>();
                    if (pathItem.getParameters() != null) params.addAll(pathItem.getParameters());
                    if (operation.getParameters() != null) params.addAll(operation.getParameters());

                    // 7) Classify parameters by "in": path/query/header
                    logger.info("Add parameters to input schema");
                    for (Parameter parameter : params) {
                        if (parameter == null || parameter.getIn() == null) {
                            logger.warn("Parameter is null or skipping...");
                            continue;
                        }
                        String parameterName = parameter.getName();
                        String parameterDesc = Optional.ofNullable(parameter.getDescription()).orElse("string");
                        Schema<?> parameterSchema = parameter.getSchema();

                        if (parameterSchema == null) throw new ConversionException("Schema for parameter " + parameterName + " is null!");

                        ObjectNode n = MAPPER.createObjectNode();
                        switch (parameter.getIn()) {
                            case "path" -> {
                                logger.debug("Add path parameter {}", parameterName);
                                n.put("type",  parameterSchema.getTypes().toArray()[0].toString());
                                n.put("description", parameterDesc.endsWith("(path)") ? parameterDesc : parameterDesc + " (path)");
                                pathProps.set(parameterName, n);
                                if (parameter.getRequired()) pathReq.add(parameterName);
                            }
                            case "query" -> {
                                logger.debug("Add Query parameter {}", parameterName);
                                n.put("type", parameterSchema.getTypes().toArray()[0].toString());
                                n.put("description", parameterDesc);
                                queryProps.set(parameterName, n);
                            }
                            case "header" -> {
                                logger.debug("Add header {}", parameterName);
                                n.put("description", parameterDesc);
                                headerProps.set(parameterName, n);
                            }
                        }
                    }

                    logger.debug("Check if body is required and has jsonFormat...");
                    boolean bodyRequired = operation.getRequestBody() != null &&
                            operation.getRequestBody().getRequired();
                    boolean hasJsonBody = operation.getRequestBody() != null &&
                            operation.getRequestBody().getContent() != null &&
                            operation.getRequestBody().getContent().containsKey("application/json");

                    ObjectNode bodyNode = null;
                    if (hasJsonBody) {
                        // Resolve and convert body schema
                        Schema<?> bodySchema = resolveSchema(api, preferJsonSchema(operation.getRequestBody().getContent()));
                        bodyNode = schemaToBodyObject(api, bodySchema);
                    }

                    if (!pathProps.isEmpty() || !pathReq.isEmpty()) {
                        logger.debug("Set path properties...");
                        properties.set("path", objWithProps(pathProps, pathReq));
                        if (!pathReq.isEmpty()) { rootRequired.add("path"); }
                    }

                    if (!queryProps.isEmpty()) {
                        logger.debug("Set query properties...");
                        properties.set("query", objWithProps(queryProps, null));
                    }

                    if (!headerProps.isEmpty()) {
                        logger.debug("Set header...");
                        properties.set("headers", headerProps);
                    }

                    // Body: include only if object-with-props or array
                    if (bodyNode != null) {
                        String bodyRootType = bodyNode.path("type").asText(null);
                        boolean includeBody = "array".equals(bodyRootType)
                                || ("object".equals(bodyRootType) && !bodyNode.path("properties").isEmpty());
                        if (includeBody) {
                            logger.debug("Add body properties...");
                            properties.set("body", bodyNode);
                            if (bodyRequired) rootRequired.add("body");
                        }
                    }

                    logger.debug("Set schema of tool");
                    inputSchema.set("properties", properties);
                    if (!rootRequired.isEmpty()) inputSchema.set("required", rootRequired);
                    tool.set("inputSchema", inputSchema);

                    logger.info("Successfully convert tool '{}'", toSnakeCase(operation.getSummary()));
                    tools.add(tool);
                });
            });
        }

        logger.info("====== Ending to convert spec to tool ======");
        return MAPPER.treeToValue(toolset, ToolSetDto.class);
    }

    // Helper: wrap props (+ optional required) into a { type:"object", properties, required? } node
    private static ObjectNode objWithProps(ObjectNode props, ArrayNode required) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("type", "object");
        n.set("properties", props == null ? MAPPER.createObjectNode() : props);
        if (required != null && !required.isEmpty()) n.set("required", required);
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
            n.put("type", s != null && !s.getTypes().isEmpty() ? s.getTypes().toArray()[0].toString() : "object");
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
