import net.leanix.api.*;
import net.leanix.api.common.*;
import net.leanix.api.models.*;

import java.util.Map;

class Query {
  private ApiClient apiClient;
  private String query;

  Query(ApiClient apiClient, String path, Map<String, String> identifiers) {
    this.apiClient = apiClient;

    // Read query from file
    FileUtils file = new FileUtils(path);
    this.query = file.readlines();

    // Replace given identifiers
    for (String key : identifiers.keySet()) {
      this.query = this.query.replaceAll("#" + key, identifiers.get(key));
    }
  }

  Map<String, Map<String, Object>> execute() throws ApiException {
    GraphqlApi graphqlApi = new GraphqlApi(this.apiClient);
    GraphQLRequest request = new GraphQLRequest();
    request.setQuery(this.query);
    GraphQLResult result = graphqlApi.processGraphQL(request);

    if (result.getErrors() != null) {
      System.out.println("ERROR:");
      System.out.println(result.getErrors());
      throw new ApiException();
    }

    if (result.getData() != null) {
      return (Map<String, Map<String, Object>>) result.getData();
    } else {
      return null;
    }
  }
}
