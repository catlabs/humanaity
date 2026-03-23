package eu.catlabs.humanaity.infrastructure.web;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
