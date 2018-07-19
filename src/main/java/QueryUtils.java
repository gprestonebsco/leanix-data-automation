import net.leanix.api.common.ApiClient;
import net.leanix.api.common.ApiException;

import java.util.HashMap;
import java.util.Map;

class QueryUtils {

  // Get the revision number of a fact sheet with its ID
  static String getRev(ApiClient apiClient, String id) throws ApiException {
    Map<String, String> revIds = new HashMap<String, String>();
    revIds.put("id", id);

    Query revQuery = new Query(apiClient, "rev.graphql", revIds);
    Map<String, Map<String, Object>> revData = revQuery.execute();

    return revData.get("factSheet").get("rev").toString();
  }
}
