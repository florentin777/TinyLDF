package ds;

public class PostQuadDTO {
    public String subject;
    public String predicate;
    public String object;
    public String userId;
    public String token;

    // Constructeur par défaut
    public PostQuadDTO() {}

    // Constructeur avec paramètres
    public PostQuadDTO(String subject, String predicate, String object, String userId, String token) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.userId = userId;
        this.token = token;
    }
}
