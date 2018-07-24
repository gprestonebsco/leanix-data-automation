import net.leanix.api.common.ApiClient;
import net.leanix.api.common.ApiException;

import java.util.HashMap;
import java.util.Map;

class QueryUtils {

  // Get the fact sheet data returned by the rev.graphql query
  private static Map<String, Object> getRevFactSheet(ApiClient apiClient, String id) throws ApiException {
    Map<String, String> revIds = new HashMap<String, String>();
    revIds.put("id", id);

    Query revQuery = new Query(apiClient, "rev.graphql", revIds);
    Map<String, Map<String, Object>> revData = revQuery.execute();

    return revData.get("factSheet");
  }

  // Get the revision number of a fact sheet with its ID
  static int getRev(ApiClient apiClient, String id) throws ApiException {
    return Integer.parseInt(getRevFactSheet(apiClient, id).get("rev").toString());
  }

  // Get the type of a fact sheet with its ID
  static String getType(ApiClient apiClient, String id) throws ApiException {
    return getRevFactSheet(apiClient, id).get("type").toString();
  }
}
