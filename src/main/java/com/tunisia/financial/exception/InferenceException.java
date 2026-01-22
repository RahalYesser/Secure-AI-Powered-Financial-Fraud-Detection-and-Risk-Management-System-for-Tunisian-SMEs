package com.tunisia.financial.exception;

/**
 * Exception thrown when model inference fails
 */
public class InferenceException extends RuntimeException {
    
    public InferenceException(String message) {
        super(message);
    }
    
    public InferenceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /*  public InferenceException(String modelName, Throwable cause) {
        super("Inference failed for model: " + modelName, cause);
    }*/
}
