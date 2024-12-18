package tinyLDF.ds;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.datastore.*;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;

import javax.inject.Named;

import com.google.api.server.spi.config.Nullable;

@Api(
    name = "myTinyLDF",
    version = "v1",
    audiences = "1042893705344-ba9l671424s8uhqp9pdrt6jqgq9u69k7.apps.googleusercontent.com",
    clientIds = {"1042893705344-ba9l671424s8uhqp9pdrt6jqgq9u69k7.apps.googleusercontent.com"}
)
public class QuadEndpoint {

    private static final int PAGE_SIZE = 10; // Limite de pagination

    /**
     * Endpoint LDF : Renvoie des triples filtrés par subject/predicate/object sous forme N-Triples.
     */
    @ApiMethod(name="getLDF", path="ldf", httpMethod=HttpMethod.GET)
    public RawResponse getLDF(@Named("subject") @Nullable String subject,
                              @Named("predicate") @Nullable String predicate,
                              @Named("object") @Nullable String object,
                              @Named("page") @Nullable Integer page) {
        long startTime = System.currentTimeMillis();
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        // Construire les filtres
        List<Query.Filter> filters = new ArrayList<>();
        if (subject != null && !subject.isEmpty()) {
            filters.add(new Query.FilterPredicate("subject", Query.FilterOperator.EQUAL, subject));
        }
        if (predicate != null && !predicate.isEmpty()) {
            filters.add(new Query.FilterPredicate("predicate", Query.FilterOperator.EQUAL, predicate));
        }
        if (object != null && !object.isEmpty()) {
            filters.add(new Query.FilterPredicate("object", Query.FilterOperator.EQUAL, object));
        }

        Query.Filter finalFilter = null;
        if (!filters.isEmpty()) {
            if (filters.size() == 1) {
                finalFilter = filters.get(0);
            } else {
                finalFilter = Query.CompositeFilterOperator.and(filters);
            }
        }

        Query query = new Query("Quad");
        if (finalFilter != null) {
            query.setFilter(finalFilter);
        }

        int currentPage = (page == null || page < 0) ? 0 : page;
        FetchOptions fetchOptions = FetchOptions.Builder.withOffset(currentPage * PAGE_SIZE).limit(PAGE_SIZE);

        PreparedQuery pq = datastore.prepare(query);
        List<Entity> entities = pq.asList(fetchOptions);

        StringBuilder sb = new StringBuilder();
        for (Entity e : entities) {
            String s = (String) e.getProperty("subject");
            String p = (String) e.getProperty("predicate");
            String o = (String) e.getProperty("object");
            // Format N-Triples : <subject> <predicate> <object> .
            sb.append("<").append(s).append("> <").append(p).append("> <").append(o).append("> .\n");
        }

        long endTime = System.currentTimeMillis();
        long execTime = endTime - startTime;
        sb.append("# Execution time: ").append(execTime).append(" ms\n");

        // Construire le lien vers la page suivante
        StringBuilder nextPageUrl = new StringBuilder("/_ah/api/myTinyLDF/v1/ldf?");
        if (subject != null && !subject.isEmpty()) nextPageUrl.append("subject=").append(subject).append("&");
        if (predicate != null && !predicate.isEmpty()) nextPageUrl.append("predicate=").append(predicate).append("&");
        if (object != null && !object.isEmpty()) nextPageUrl.append("object=").append(object).append("&");
        nextPageUrl.append("page=").append(currentPage+1);

        sb.append("# Next page: ").append(nextPageUrl.toString()).append("\n");

        return new RawResponse(sb.toString(), "text/plain");
    }

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
        if (userEntity == null){
            try {
                userEntity = new Entity("Client");
                userEntity.setProperty("mail", email);
                userEntity.setProperty("pseudo", (pm.pseudo==null || pm.pseudo.isEmpty()) ? pseudo : pm.pseudo);
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
        List<Object> result = GoogleAuthVerifier.verifyToken(pm.token, pm.userId);
        boolean isVerified = (Boolean) result.get(0);
        if (!isVerified) {
            throw new UnauthorizedException("Votre ID ou votre token est invalide ou expiré.");
        }

        if (pm.subject == null || pm.predicate == null || pm.object == null) {
            throw new IllegalArgumentException("Le sujet, le prédicat et l'objet ne peuvent pas être nuls.");
        }

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = datastore.beginTransaction();

        Entity quadEntity = new Entity("Quad");
        quadEntity.setProperty("subject", pm.subject);
        quadEntity.setProperty("predicate", pm.predicate);
        quadEntity.setProperty("object", pm.object);
        quadEntity.setProperty("creatorEmail", result.get(1));
        quadEntity.setProperty("createdAt", new Date());

        try {
            datastore.put(txn, quadEntity);
            txn.commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }

        return quadEntity;
    }

    @ApiMethod(name = "quadsCreatedUser", path = "quadsCreatedUser/{userId}", httpMethod = HttpMethod.GET)
    public PaginatedResponse getQuadsCreatedByUser(@Named("userId") String userId, @Named("cursor") @Nullable String cursor) throws NotFoundException {
        long startTime = System.currentTimeMillis();
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

        long endTime = System.currentTimeMillis();
        System.out.println("Temps d'exécution : " + (endTime - startTime) + " ms");

        return new PaginatedResponse(quads, nextCursor);
    }

    @ApiMethod(name = "advancedSearch", path = "search/advanced", httpMethod = HttpMethod.GET)
    public PaginatedResponse advancedSearch(@Named("subject") @Nullable String subject, @Named("predicate") @Nullable String predicate, @Named("object") @Nullable String object, @Named("logic") @Nullable String logic, @Named("cursor") @Nullable String cursor) {
        long startTime = System.currentTimeMillis();

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

        List<Quad> quads = new ArrayList<>();
        for (Entity entity : results) {
            quads.add(new Quad(
                    (String) entity.getProperty("subject"),
                    (String) entity.getProperty("predicate"),
                    (String) entity.getProperty("object")
            ));
        }

        String nextCursor = results.getCursor() != null ? results.getCursor().toWebSafeString() : null;

        long endTime = System.currentTimeMillis();
        System.out.println("Temps d'exécution : " + (endTime - startTime) + " ms");

        return new PaginatedResponse(quads, nextCursor);
    }

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
