package de.thm.spec2tool.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConvertController {

    private static final Logger logger = LoggerFactory.getLogger(ConvertController.class);

    @PostMapping("/convert")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getConvertedSpec(String input) {
        logger.info("Converting Spec: {}", input);
        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"json_placeholder\",\"description\":\"Free fake API for testing and prototyping.\",\"tools\":[{\"name\":\"returns_all_posts\",\"description\":\"Returns all posts\",\"requestMethod\":\"GET\",\"endpoint\":\"https://jsonplaceholder.typicode.com/posts\",\"inputSchema\":{}},{\"name\":\"create_a_new_post\",\"description\":\"Create a new post\",\"requestMethod\":\"POST\",\"endpoint\":\"https://jsonplaceholder.typicode.com/posts\",\"inputSchema\":{\"type\":\"object\",\"properties\":{\"body\":{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"number\",\"description\":\"ID of the post\"},\"title\":{\"type\":\"string\",\"description\":\"Title of the post\"},\"body\":{\"type\":\"string\",\"description\":\"Body of the post\"},\"userId\":{\"type\":\"number\",\"description\":\"ID of the user who created the post\"}}}}}},{\"name\":\"get_a_single_post\",\"description\":\"Get a single post\",\"requestMethod\":\"GET\",\"endpoint\":\"https://jsonplaceholder.typicode.com/posts/{id}\",\"inputSchema\":{\"type\":\"object\",\"properties\":{\"p_id\":{\"type\":\"number\",\"description\":\"ID of the post\"}},\"required\":[\"p_id\"]}},{\"name\":\"update_a_post\",\"description\":\"Update a post\",\"requestMethod\":\"PUT\",\"endpoint\":\"https://jsonplaceholder.typicode.com/posts/{id}\",\"inputSchema\":{\"type\":\"object\",\"properties\":{\"p_id\":{\"type\":\"string\",\"description\":\"ID of the post\"},\"body\":{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"number\",\"description\":\"ID of the post\"},\"title\":{\"type\":\"string\",\"description\":\"Title of the post\"},\"body\":{\"type\":\"string\",\"description\":\"Body of the post\"},\"userId\":{\"type\":\"number\",\"description\":\"ID of the user who created the post\"}}}},\"required\":[\"p_id\",\"body\"]}},{\"name\":\"delete_a_post\",\"description\":\"Delete a post\",\"requestMethod\":\"DELETE\",\"endpoint\":\"https://jsonplaceholder.typicode.com/posts\",\"inputSchema\":{\"type\":\"object\",\"properties\":{\"body\":{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"number\",\"description\":\"ID of the post\"},\"title\":{\"type\":\"string\",\"description\":\"Title of the post\"},\"body\":{\"type\":\"string\",\"description\":\"Body of the post\"},\"userId\":{\"type\":\"number\",\"description\":\"ID of the user who created the post\"}}}}}}]}");
    }
}
