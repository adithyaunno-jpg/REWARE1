package rewear.models;

/**
 * Interface for entities that support keyword matching and filtering.
 * Demonstrates Abstraction and Interface contracts in Java.
 */
public interface Searchable {
    boolean matchesKeyword(String keyword);
}

