package genomancer.vine.das2.client.modelimpl;

import genomancer.trellis.das2.model.Das2PropertyI;

public class Das2Property implements Das2PropertyI {
    String key;
    String value;

    public Das2Property(String key, String value)  {
	this.key = key;
	this.value = value;
    }

    public String getKey() { return key; }
    public String getValue() { return value; }


}