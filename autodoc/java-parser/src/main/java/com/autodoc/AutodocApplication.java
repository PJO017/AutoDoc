package com.autodoc;

import com.autodoc.generator.JavaToOpenApiGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

public class AutodocApplication {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar autodoc.jar <source_directory>");
            System.exit(1);
        }

        try {
            Path sourcePath = new File(args[0]).toPath();
            JavaToOpenApiGenerator generator = new JavaToOpenApiGenerator();
            Map<String, Object> spec = generator.generate(sourcePath);

            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            String json = mapper.writeValueAsString(spec);
            System.out.println(json);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
