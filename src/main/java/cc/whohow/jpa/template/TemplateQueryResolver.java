package cc.whohow.jpa.template;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TemplateQueryResolver implements Function<String, String> {
    private final Method method;

    public TemplateQueryResolver(Method method) {
        this.method = method;
    }

    @Override
    public String apply(String name) {
        if (name.isEmpty()) {
            return read(method.getDeclaringClass().getSimpleName() + "." + method.getName() + ".sql");
        } else {
            String query = read(name + ".sql");
            if (query == null) {
                throw new IllegalArgumentException(name);
            }
            return query;
        }
    }

    private String read(String name) {
        InputStream stream = method.getDeclaringClass().getClassLoader().getResourceAsStream(name);
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
