package cc.whohow.jpa.template;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class TemplateQueryResolver {
    private final Method method;
    private final TemplateQuery templateQuery;

    public TemplateQueryResolver(Method method, TemplateQuery templateQuery) {
        this.method = method;
        this.templateQuery = templateQuery;
    }

    public TemplateQuery getTemplateQuery() {
        return templateQuery;
    }

    public boolean isNativeQuery() {
        return templateQuery.nativeQuery();
    }

    public Class<?> getResultClass() {
        return templateQuery.resultClass();
    }

    public String getQuery() {
        if (!templateQuery.value().isEmpty()) {
            return templateQuery.value();
        }
        String resource = templateQuery.name().isEmpty() ?
                getDefaultResource() :
                getNamedResource();
        String query = read(resource);
        if (query == null) {
            throw new IllegalStateException();
        }
        return query;
    }

    private String getDefaultResource() {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName() + ".sql";
    }

    private String getNamedResource() {
        return templateQuery.name() + ".sql";
    }

    private String read(String resource) {
        InputStream stream = method.getDeclaringClass().getClassLoader().getResourceAsStream(resource);
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return null;
    }
}
