package cc.whohow.jpa.template.directive;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.Writer;

public class TemplateQueryParameter extends Directive {
    private static final String COUNT = "TemplateQueryParameter.count";

    @Override
    public String getName() {
        return "x";
    }

    @Override
    public int getType() {
        return Directive.LINE;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {
        String name = getTempName(context);
        context.put(name, node.jjtGetChild(0).value(context));
        writer.write(":");
        writer.write(name);
        return true;
    }

    private String getTempName(InternalContextAdapter context) {
        Integer count = (Integer) context.get(COUNT);
        if (count == null) {
            context.put(COUNT, 1);
            return getTempName(0);
        } else {
            context.put(COUNT, count + 1);
            return getTempName(count);
        }
    }

    private String getTempName(Integer i) {
        return "_x" + i + "_";
    }
}