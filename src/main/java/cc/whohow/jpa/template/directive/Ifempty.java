package cc.whohow.jpa.template.directive;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.Writer;

public class Ifempty extends Directive {

    public String getName() {
        return "ifempty";
    }

    public int getType() {
        return BLOCK;
    }

    public boolean render(InternalContextAdapter context, Writer writer,
                          Node node) throws IOException, ResourceNotFoundException,
            ParseErrorException, MethodInvocationException {
        Object value = node.jjtGetChild(0).value(context);
        if (Objects.isEmpty(value)) {
            Node content = node.jjtGetChild(1);
            content.render(context, writer);
        }
        return true;
    }

}