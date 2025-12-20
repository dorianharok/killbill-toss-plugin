package org.killbill.billing.plugin.toss.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TossError {

    @JsonProperty("code")
    private final String code;

    @JsonProperty("message")
    private final String message;

    public TossError(@JsonProperty("code") String code, @JsonProperty("message") String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "TossError{code='" + code + "', message='" + message + "'}";
    }
}
