package com.tunisia.financial.exception;

/**
 * Exception thrown when an AI model fails to load
 */
public class ModelLoadException extends RuntimeException {
    
    /*public ModelLoadException(String modelName) {
        super("Failed to load model: " + modelName);
    }*/
    
    public ModelLoadException(String modelName, Throwable cause) {
        super("Failed to load model: " + modelName, cause);
    }
    
    public ModelLoadException(String message) {
        super(message);
    }
}
