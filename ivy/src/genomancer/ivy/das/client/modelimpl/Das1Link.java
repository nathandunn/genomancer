package genomancer.ivy.das.client.modelimpl;

import genomancer.ivy.das.model.Das1LinkI;

public class Das1Link implements Das1LinkI  {
    String href;  // URL string
    String label;

    public Das1Link(String href, String label) {
        this.href = href;
        this.label = label;
    }

    public String getHref() {
        return href;
    }

    public String getLabel() {
        return label;
    }

}