package com.symphony.bdk.app.spring.auth.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * A pair of tokens used during the Circle of Trust authentication.
 */
@Data
public class TokenPair {

  /**
   * {@link AppToken} generated by the Extension App Backend.
   */
  @NotBlank(message = "App Token is mandatory")
  private final String appToken;

  /**
   * Symphony Token generated by the Symphony Backend.
   */
  @NotBlank(message = "Symphony Token is mandatory")
  private final String symphonyToken;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public TokenPair(@JsonProperty("appToken") String appToken, @JsonProperty("symphonyToken") String symphonyToken) {
    this.appToken = appToken;
    this.symphonyToken = symphonyToken;
  }
}