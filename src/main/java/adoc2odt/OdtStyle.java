package adoc2odt;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.util.Map;

public class OdtStyle {

    private final String role;
    private final String style;

    public OdtStyle(Map<String, Object> attributes) {
        if (attributes.get("role") != null)
            this.role = "adoc-" + attributes.get("role");
        else
            this.role = null;
        if (attributes.get("style") != null)
            this.style = "adoc-" + attributes.get("style");
        else
            this.style = null;
    }

    public OdtStyle(String styleName) {
        style = styleName;
        this.role = null;
    }

    public String toString(){
        Preconditions.checkArgument(isValid(), "invalid style: both role and style are null");
        if (role != null)
            return role;
        else
            return style;
    }

    public boolean isValid() {
        return role != null || style != null;
    }
}
