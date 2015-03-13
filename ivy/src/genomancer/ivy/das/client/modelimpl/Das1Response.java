package genomancer.ivy.das.client.modelimpl;

import genomancer.ivy.das.model.Das1ResponseI;

/**
 *
 */
public class Das1Response implements Das1ResponseI  {
    String request_url;
    String version;

    public Das1Response(String request_url, String version)  {
	this.request_url = request_url;
	this.version = version;
    }

    public String getRequestURL() {
	return request_url;
    }

    public String getVersion() {
	return version;
    }

}
