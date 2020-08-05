package cc.whohow.jpa.template;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateQueryWriter extends Writer {
    private static final Pattern WHERE = Pattern.compile("\\s+where\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern AND_OR = Pattern.compile("^(?<s>\\s*)(and|or)\\s+", Pattern.CASE_INSENSITIVE);
    private final List<CharSequence> buffer = new ArrayList<>();
    private int length = 0;

    @Override
    public void write(String text) throws IOException {
        if (text.isEmpty()) {
            return;
        }
        buffer.add(text);
        length += text.length();
    }

    @Override
    public void write(String text, int off, int len) throws IOException {
        if (len == 0) {
            return;
        }
        buffer.add(CharBuffer.wrap(text, off, off + len));
        length += len;
    }

    @Override
    public Writer append(CharSequence text) throws IOException {
        if (text.length() == 0) {
            return this;
        }
        buffer.add(text);
        length += text.length();
        return this;
    }

    @Override
    public Writer append(CharSequence text, int start, int end) throws IOException {
        int len = end - start;
        if (len == 0) {
            return this;
        }
        buffer.add(CharBuffer.wrap(text, start, end));
        length += len;
        return this;
    }

    @Override
    public void write(char[] text, int off, int len) throws IOException {
        if (len == 0) {
            return;
        }
        buffer.add(CharBuffer.wrap(text, off, len));
        length += len;
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public String toString() {
        // TODO where 1=1
        StringBuilder builder = new StringBuilder(length);
        boolean where = false;
        for (CharSequence sequence : buffer) {
            if (where) {
                Matcher matcher = AND_OR.matcher(sequence);
                if (matcher.find()) {
                    builder.append(matcher.group("s"));
                    builder.append(sequence, matcher.group().length(), sequence.length());
                } else {
                    builder.append(sequence);
                }
            } else {
                builder.append(sequence);
            }
            where = mayWhere(sequence) && WHERE.matcher(sequence).find();
        }
        return builder.toString();
    }

    private boolean mayWhere(CharSequence text) {
        return text.length() > 5; // where
    }
}
