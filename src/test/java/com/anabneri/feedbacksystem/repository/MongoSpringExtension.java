package com.anabneri.feedbacksystem.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class MongoSpringExtension implements BeforeEachCallback, AfterEachCallback {

    /**
     * Path to where our test JSON files are stored.
     */
    private static Path JSON_PATH = Paths.get("src", "test", "resources", "data");

    /**
     * Jackson ObjectMapper: used to load a JSON file into a list of objects
     */
    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Called before each test executes. This callback is responsible for importing the JSON document, defined
     * by the MongoDataFile annotation, into the embedded MongoDB, through the provided MongoTemplate.
     *
     * @param context       The ExtensionContext, which provides access to the test method.
     * @throws Exception    If an error occurs retrieving the test method or extracting the MongoDataFile annotation.
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        context.getTestMethod().ifPresent(method -> {

            MongoDataFile mongoDataFile = method.getAnnotation(MongoDataFile.class);

            getMongoTemplate(context).ifPresent(mongoTemplate -> {
                try {
                    List objects = mapper.readValue(JSON_PATH.resolve(mongoDataFile.value()).toFile(),
                            mapper.getTypeFactory().constructCollectionType(List.class, mongoDataFile.classType()));

                    objects.forEach(mongoTemplate::save);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            });
        });
    }

    /**
     * Called after each test executes. This callback is responsible for dropping the test's MongoDB collection
     * so that the next test that runs is clean.
     *
     * @param context       The ExtensionContext, which provides access to the test method.
     * @throws Exception    If an error occurs retrieving the test method or extracting the MongoDataFile annotation.
     */
    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        context.getTestMethod().ifPresent(method -> {

            MongoDataFile mongoDataFile = method.getAnnotation(MongoDataFile.class);

            Optional<MongoTemplate> mongoTemplate = getMongoTemplate(context);
            mongoTemplate.ifPresent(t -> t.dropCollection(mongoDataFile.collectionName()));
        });
    }

    /**
     * Helper method that uses reflection to invoke the getMongoTemplate() method on the test instance.
     * @param context   The ExtensionContext, which provides access to the test instance.
     * @return          An optional MongoTemplate, if it exists.
     */
    private Optional<MongoTemplate> getMongoTemplate(ExtensionContext context) {
        Optional<Class<?>> clazz = context.getTestClass();
        if (clazz.isPresent()) {
            Class<?> c = clazz.get();
            try {
                Method method = c.getMethod("getMongoTemplate", null);

                Optional<Object> testInstance = context.getTestInstance();
                if (testInstance.isPresent()) {
                    return Optional.of((MongoTemplate)method.invoke(testInstance.get(), null));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Optional.empty();
    }
}