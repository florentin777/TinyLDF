package com.example.tinyldf;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

import java.io.File;

public class RdfStore {
  private static Repository repo;

  public static void init() {
    File dataDir = new File("data");
    dataDir.mkdir();
    repo = new SailRepository(new NativeStore(dataDir));
    repo.init();
  }

  public static RepositoryConnection getConnection() {
    return repo.getConnection();
  }
}
