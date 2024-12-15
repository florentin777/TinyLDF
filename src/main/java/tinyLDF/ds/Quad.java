package ds;

import java.util.Objects;

public class Quad {
    private String subject;
    private String predicate;
    private String object;

    public Quad() {}

    public Quad(String subject, String predicate, String object) {
        if (subject == null || predicate == null || object == null) {
            throw new IllegalArgumentException("Subject, predicate, and object cannot be null.");
        }
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPredicate() {
        return predicate;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quad quad = (Quad) o;
        return Objects.equals(subject, quad.subject) &&
               Objects.equals(predicate, quad.predicate) &&
               Objects.equals(object, quad.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, predicate, object);
    }

    @Override
    public String toString() {
        return "Quad{" +
               "subject='" + subject + '\'' +
               ", predicate='" + predicate + '\'' +
               ", object='" + object + '\'' +
               '}';
    }
}
