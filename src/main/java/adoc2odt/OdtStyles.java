package adoc2odt;

import java.util.Stack;

public class OdtStyles {
    private Stack<OdtStyle> styleStack = new Stack<OdtStyle>();

    OdtStyle getCurrentStyle() {
        return styleStack.empty()? null : styleStack.peek();
    }

    public void pop() {
        styleStack.pop();
    }

    public void push(OdtStyle odtStyle) {
        styleStack.push(odtStyle);
    }
}
