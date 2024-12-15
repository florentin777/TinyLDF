package ds;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;

import javax.inject.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

import com.google.appengine.api.datastore.Transaction;

@Api(name = "myTinyLDF",
     version = "v1",
     audiences = "1042893705344-ba9l671424s8uhqp9pdrt6jqgq9u69k7.apps.googleusercontent.com",
  	 clientIds = {"1042893705344-ba9l671424s8uhqp9pdrt6jqgq9u69k7.apps.googleusercontent.com"}
        
     )
     public class QuadEndpoint {

        private static final int PAGE_SIZE = 10; // Limite de pagination
    
        // Méthode pour récupérer un quad par ID
        @ApiMethod(name = "getQuad", path = "quads/{id}", httpMethod = HttpMethod.GET)
        public Entity getQuadById(@Named("id") Long id) throws NotFoundException {
            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            Key quadKey = KeyFactory.createKey("Quad", id);
    
            Query.Filter keyFilter = new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, Query.FilterOperator.EQUAL, quadKey);
            Query query = new Query("Quad").setFilter(keyFilter);
            PreparedQuery preparedQuery = datastore.prepare(query);
    
            Entity quadEntity = preparedQuery.asSingleEntity();
            if (quadEntity != null) {
                return quadEntity;
            } else {
                throw new NotFoundException("Quad non trouvé pour l'ID : " + id);
            }
        }
           /*
     * Ajout d'un utilisateur à notre datastore
     */
	@ApiMethod(name = "addUser", path = "addUser", httpMethod = HttpMethod.POST)
    public Entity addUser(CreateUserDTO pm) throws UnauthorizedException, ConflictException {
        List<Object> result =null;

        try{
            result = GoogleAuthVerifier.verifyToken(pm.token, pm.userId);

       }
       catch (Exception e) {
           throw new UnauthorizedException("Votre Id ou votre token est invalide ou expiré.");
       }
       boolean isVerified = (Boolean) result.get(0);
       String email = (String) result.get(1);
       String pseudo = (String) result.get(2);
       if (!isVerified) {
           throw new UnauthorizedException("Votre Id ou votre token est invalide ou expiré.");
       }
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = datastore.beginTransaction();

        Query.Filter propertyFilter = new Query.FilterPredicate("mail", Query.FilterOperator.EQUAL, email);
            Query q = new Query("Client").setFilter(propertyFilter);
            PreparedQuery pq = datastore.prepare(q);
            Entity userEntity = pq.asSingleEntity();
        if (userEntity == null){ // Si le nouvel utilisateur n'est pas déjà dans notre base de données
            try {
                // Stocker le user
                userEntity = new Entity("Client");
                userEntity.setProperty("mail", email);
                userEntity.setProperty("pseudo", (pm.pseudo==null || pm.pseudo=="") ? pseudo : pm.pseudo); // si le pseudo de l'utilisateur est vide, alors on utilise son nom prénom google
                datastore.put(txn, userEntity);
                txn.commit();
                return userEntity;
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
        }
        else {
            throw new ConflictException("Utilisateur déjà créé : " + email);
        }

    }
    
        @ApiMethod(name = "createQuad", httpMethod = HttpMethod.POST)
        public Entity createQuad(PostQuadDTO pm) throws UnauthorizedException {
        // Vérification de l'authentification via le token et l'ID utilisateur
        List<Object> result = GoogleAuthVerifier.verifyToken(pm.token, pm.userId);
        boolean isVerified = (Boolean) result.get(0);
        if (!isVerified) {
            throw new UnauthorizedException("Votre ID ou votre token est invalide ou expiré.");
        }

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = datastore.beginTransaction();

        // Validation des données du quad
        if (pm.subject == null || pm.predicate == null || pm.object == null) {
            throw new IllegalArgumentException("Le sujet, le prédicat et l'objet ne peuvent pas être nuls.");
        }

        // Création de l'entité "Quad"
        Entity quadEntity = new Entity("Quad");
        quadEntity.setProperty("subject", pm.subject);
        quadEntity.setProperty("predicate", pm.predicate);
        quadEntity.setProperty("object", pm.object);
        quadEntity.setProperty("creatorEmail", result.get(1));  // Email de l'utilisateur authentifié
        quadEntity.setProperty("createdAt", new Date());

        try {
            datastore.put(txn, quadEntity);
            txn.commit();  // Validation de la transaction
        } finally {
            if (txn.isActive()) {
                txn.rollback();  // Annulation si une erreur se produit
            }
        }

        return quadEntity;  // Retourner l'entité quad créée
    }

    
        // Méthode pour récupérer les quads créés par un utilisateur avec pagination
        @ApiMethod(name = "quadsCreatedUser", path = "quadsCreatedUser/{userId}", httpMethod = HttpMethod.GET)
        public PaginatedResponse getQuadsCreatedByUser(@Named("userId") String userId, @Named("cursor") @Nullable String cursor) throws NotFoundException {
            long startTime = System.currentTimeMillis(); // Mesurer le temps d'exécution
    
            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            Query.Filter userFilter = new Query.FilterPredicate("userId", Query.FilterOperator.EQUAL, userId);
            Query userQuery = new Query("User").setFilter(userFilter);
            PreparedQuery userPq = datastore.prepare(userQuery);
            Entity userEntity = userPq.asSingleEntity();
    
            if (userEntity == null) {
                throw new NotFoundException("Utilisateur introuvable pour l'ID : " + userId);
            }
    
            Query.Filter quadUserFilter = new Query.FilterPredicate("userId", Query.FilterOperator.EQUAL, userId);
            Query quadQuery = new Query("Quad").setFilter(quadUserFilter).addSort("createdAt", Query.SortDirection.DESCENDING);
    
            FetchOptions fetchOptions = FetchOptions.Builder.withLimit(PAGE_SIZE);
            if (cursor != null && !cursor.isEmpty()) {
                fetchOptions.startCursor(Cursor.fromWebSafeString(cursor));
            }
    
            PreparedQuery quadPreparedQuery = datastore.prepare(quadQuery);
            QueryResultList<Entity> quadEntities = quadPreparedQuery.asQueryResultList(fetchOptions);
    
            List<Quad> quads = new ArrayList<>();
            for (Entity quadEntity : quadEntities) {
                quads.add(new Quad(
                        (String) quadEntity.getProperty("subject"),
                        (String) quadEntity.getProperty("predicate"),
                        (String) quadEntity.getProperty("object")
                ));
            }
    
            String nextCursor = quadEntities.getCursor() != null ? quadEntities.getCursor().toWebSafeString() : null;
    
            long endTime = System.currentTimeMillis(); // Mesurer le temps d'exécution
            System.out.println("Temps d'exécution : " + (endTime - startTime) + " ms");
    
            return new PaginatedResponse(quads, nextCursor);
        }
    
        // Méthode pour rechercher des quads avec des opérateurs logiques
        @ApiMethod(name = "advancedSearch", path = "search/advanced", httpMethod = HttpMethod.GET)
        public PaginatedResponse advancedSearch(@Named("subject") @Nullable String subject, @Named("predicate") @Nullable String predicate, @Named("object") @Nullable String object, @Named("logic") @Nullable String logic, @Named("cursor") @Nullable String cursor) {
            long startTime = System.currentTimeMillis(); // Mesurer le temps d'exécution
        
            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            List<Query.Filter> filters = new ArrayList<>();
            if (subject != null) filters.add(new Query.FilterPredicate("subject", Query.FilterOperator.EQUAL, subject));
            if (predicate != null) filters.add(new Query.FilterPredicate("predicate", Query.FilterOperator.EQUAL, predicate));
            if (object != null) filters.add(new Query.FilterPredicate("object", Query.FilterOperator.EQUAL, object));
        
            Query.Filter finalFilter = filters.isEmpty() ? null
                    : (logic != null && logic.equalsIgnoreCase("OR"))
                    ? Query.CompositeFilterOperator.or(filters)
                    : Query.CompositeFilterOperator.and(filters);
        
            Query query = new Query("Quad");
            if (finalFilter != null) {
                query.setFilter(finalFilter);
            }
        
            FetchOptions fetchOptions = FetchOptions.Builder.withLimit(PAGE_SIZE);
            if (cursor != null && !cursor.isEmpty()) {
                fetchOptions.startCursor(Cursor.fromWebSafeString(cursor));
            }
        
            PreparedQuery preparedQuery = datastore.prepare(query);
            QueryResultList<Entity> results = preparedQuery.asQueryResultList(fetchOptions);
        
            List<Quad> quads = results.stream()
                    .map(entity -> new Quad(
                            (String) entity.getProperty("subject"),
                            (String) entity.getProperty("predicate"),
                            (String) entity.getProperty("object")
                    ))
                    .collect(Collectors.toList());
        
            String nextCursor = results.getCursor() != null ? results.getCursor().toWebSafeString() : null;
        
            long endTime = System.currentTimeMillis(); // Mesurer le temps d'exécution
            System.out.println("Temps d'exécution : " + (endTime - startTime) + " ms");
        
            return new PaginatedResponse(quads, nextCursor);
        }
        
        // Statistiques : nombre total de quads
        @ApiMethod(name = "getQuadStats", path = "stats", httpMethod = HttpMethod.GET)
        public Map<String, Object> getQuadStats() {
            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            Query query = new Query("Quad");
            int quadCount = datastore.prepare(query).countEntities(FetchOptions.Builder.withDefaults());
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalQuads", quadCount);
            return stats;
        }
    }