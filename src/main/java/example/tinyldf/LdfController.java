package com.example.tinyldf;

import org.springframework.web.bind.annotation.*;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;

@RestController
public class LdfController {

  @GetMapping("/ldf")
  public String getLdf(@RequestParam(required=false) String subject,
                       @RequestParam(required=false) String predicate,
                       @RequestParam(required=false) String object,
                       @RequestParam(required=false) String graph,
                       @RequestParam(defaultValue="1") int page,
                       @RequestParam(defaultValue="10") int pagesize) {

    long startTime = System.currentTimeMillis();

    StringBuilder sb = new StringBuilder();
    sb.append("SELECT ?s ?p ?o ?g WHERE { GRAPH ?g { ?s ?p ?o } ");
    if (subject != null) sb.append("FILTER(?s = <").append(subject).append(">) ");
    if (predicate != null) sb.append("FILTER(?p = <").append(predicate).append(">) ");
    if (object != null) sb.append("FILTER(?o = <").append(object).append(">) ");
    if (graph != null) sb.append("FILTER(?g = <").append(graph).append(">) ");
    sb.append("} LIMIT ").append(pagesize).append(" OFFSET ").append((page-1)*pagesize);

    String queryStr = sb.toString();

    StringBuilder response = new StringBuilder();
    response.append("{\"results\":[");

    try (RepositoryConnection conn = RdfStore.getConnection()) {
      TupleQuery query = conn.prepareTupleQuery(queryStr);
      try (TupleQueryResult result = query.evaluate()) {
        boolean first = true;
        while (result.hasNext()) {
          if (!first) response.append(",");
          BindingSet bs = result.next();
          response.append("{")
                  .append("\"s\":\"").append(bs.getValue("s")).append("\",")
                  .append("\"p\":\"").append(bs.getValue("p")).append("\",")
                  .append("\"o\":\"").append(bs.getValue("o")).append("\",")
                  .append("\"g\":\"").append(bs.getValue("g")).append("\"")
                  .append("}");
          first = false;
        }
      }
    }

    response.append("],");

    long endTime = System.currentTimeMillis();
    response.append("\"executionTimeMs\":").append(endTime - startTime).append(",");
    response.append("\"nextPage\":\"?page=").append(page+1).append("&pagesize=").append(pagesize).append("\"");
    response.append("}");

    return response.toString();
  }
}
