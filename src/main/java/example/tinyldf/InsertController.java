package com.example.tinyldf;

import org.springframework.web.bind.annotation.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.*;

import java.io.StringReader;

@RestController
public class InsertController {

  @PostMapping("/insert")
  public String insertData(@RequestBody String rdfData) {
    // attend des données N-Quads
    try (RepositoryConnection conn = RdfStore.getConnection()) {
      conn.add(new StringReader(rdfData), "", RDFFormat.NQUADS);
    } catch (Exception e) {
      return "{\"status\":\"error\",\"message\":\""+e.getMessage()+"\"}";
    }
    return "{\"status\":\"success\"}";
  }
}
