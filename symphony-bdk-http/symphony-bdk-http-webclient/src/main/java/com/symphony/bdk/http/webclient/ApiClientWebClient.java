package com.symphony.bdk.http.webclient;

import com.symphony.bdk.http.api.ApiClient;
import com.symphony.bdk.http.api.ApiException;
import com.symphony.bdk.http.api.ApiResponse;
import com.symphony.bdk.http.api.Pair;
import com.symphony.bdk.http.api.util.TypeReference;

import org.apiguardian.api.API;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring WebClient implementation for the {@link ApiClient} interface called by generated code.
 */
@API(status = API.Status.EXPERIMENTAL)
public class ApiClientWebClient implements ApiClient {

  protected WebClient webClient;
  protected String basePath;
  protected Map<String, String> defaultHeaderMap;

  public ApiClientWebClient(final WebClient webClient, String basePath, Map<String, String> defaultHeaders) {
    this.webClient = webClient;
    this.basePath = basePath;
    this.defaultHeaderMap = new HashMap<>(defaultHeaders);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> ApiResponse<T> invokeAPI(
      final String path,
      final String method,
      final List<Pair> queryParams,
      final Object body,
      final Map<String, String> headerParams,
      final Map<String, String> cookieParams,
      final Map<String, Object> formParams,
      final String accept,
      final String contentType,
      final String[] authNames,
      final TypeReference<T> returnType
  ) throws ApiException {
    HttpMethod httpMethod = HttpMethod.resolve(method);
    if (httpMethod == null) {
      throw new ApiException(500, "unknown method type " + method);
    }

    WebClient.RequestBodySpec requestBodySpec =
        this.webClient.method(httpMethod).uri(uriBuilder -> {
          uriBuilder = uriBuilder.path(path);
          if (queryParams != null) {
            for (Pair queryParam : queryParams) {
              if (queryParam.getValue() != null) {
                uriBuilder = uriBuilder.queryParam(queryParam.getName(), escapeString(queryParam.getValue()));
              }
            }
          }
          return uriBuilder.build();
        }).contentType(MediaType.parseMediaType(contentType));

    if (!"".equals(accept)) {
      requestBodySpec.accept(MediaType.valueOf(accept));
    }

    for (Map.Entry<String, String> headerParam : headerParams.entrySet()) {
      String value = headerParam.getValue();
      if (value != null) {
        requestBodySpec = requestBodySpec.header(headerParam.getKey(), headerParam.getValue());
      }
    }

    for (Map.Entry<String, String> cookieParam : cookieParams.entrySet()) {
      String value = cookieParam.getValue();
      if (value != null) {
        requestBodySpec = requestBodySpec.cookie(cookieParam.getKey(), cookieParam.getValue());
      }
    }

    for (Map.Entry<String, String> defaultHeaderParam : this.defaultHeaderMap.entrySet()) {
      String key = defaultHeaderParam.getKey();
      if (!headerParams.containsKey(key)) {
        String value = defaultHeaderParam.getValue();
        if (value != null) {
          requestBodySpec = requestBodySpec.header(key, value);
        }
      }
    }

    if (formParams != null) {
      if (contentType.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
        MultiValueMap<String, Object> formValueMap = new LinkedMultiValueMap<>();
        for (Map.Entry<String, Object> param : formParams.entrySet()) {
          if (param.getValue() instanceof File) {
            File file = (File) param.getValue();
            formValueMap.add(param.getKey(), file);
          } else {
            formValueMap.add(param.getKey(), parameterToString(param.getValue()));
          }
        }
        requestBodySpec.body(BodyInserters.fromMultipartData(formValueMap));
      } else if (contentType.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
        MultiValueMap<String, String> formValueMap = new LinkedMultiValueMap<>();
        for (Map.Entry<String, Object> param : formParams.entrySet()) {
          formValueMap.add(param.getKey(), parameterToString(param.getValue()));
        }
        requestBodySpec.body(BodyInserters.fromFormData(formValueMap));
      }
    }
    if (body != null) {
      requestBodySpec.body(BodyInserters.fromValue(body));
    }

    Mono<ClientResponse> mono = requestBodySpec.exchange();
    ClientResponse response = mono.block();
    Map<String, List<String>> headers = response
        .headers().asHttpHeaders().entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue
        ));
    for (Map.Entry<String, List<String>> entry : response.headers().asHttpHeaders().entrySet()) {
      headers.put(entry.getKey(), entry.getValue());
    }

    if (response.statusCode().equals(HttpStatus.NO_CONTENT)) {
      return new ApiResponse<>(response.statusCode().value(), headers);
    } else if (response.statusCode().equals(HttpStatus.OK)) {
      if (returnType == null) {
        return new ApiResponse<>(response.statusCode().value(), headers);
      } else {
        if (returnType.getType() instanceof Class) {
          Class<T> clazz = (Class<T>) returnType.getType();
          T entity = response.bodyToMono(clazz).block();
          return new ApiResponse<>(response.statusCode().value(), headers, entity);
        } else {
          ParameterizedTypeReference<T> reference = ParameterizedTypeReference.forType(returnType.getType());
          T entity = response.bodyToMono(reference).block();
          return new ApiResponse<>(response.statusCode().value(), headers, entity);
        }
      }
    } else {
      String message = "error";
      String respBody = response.bodyToMono(String.class).block();
      if (respBody != null) {
        message = respBody;
      }

      throw new ApiException(
          response.rawStatusCode(),
          message,
          headers,
          respBody);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getBasePath() {
    return this.basePath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String parameterToString(Object param) {
    if (param == null) {
      return "";
    } else if (param instanceof Collection) {
      StringBuilder b = new StringBuilder();
      for (Object o : (Collection<?>) param) {
        if (b.length() > 0) {
          b.append(',');
        }
        b.append(o);
      }
      return b.toString();
    } else {
      return String.valueOf(param);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Pair> parameterToPairs(String collectionFormat, String name, Object value) {
    List<Pair> params = new ArrayList<Pair>();

    // preconditions
    if (name == null || name.isEmpty() || value == null) {
      return params;
    }

    Collection<?> valueCollection;
    if (value instanceof Collection) {
      valueCollection = (Collection) value;
    } else {
      params.add(new Pair(name, parameterToString(value)));
      return params;
    }

    if (valueCollection.isEmpty()) {
      return params;
    }

    // get the collection format (default: csv)
    String format = (collectionFormat == null || collectionFormat.isEmpty() ? "csv" : collectionFormat);

    // create the params based on the collection format
    if ("multi".equals(format)) {
      for (Object item : valueCollection) {
        params.add(new Pair(name, parameterToString(item)));
      }

      return params;
    }

    String delimiter = ",";

    switch (format) {
      case "csv":
        delimiter = ",";
        break;
      case "ssv":
        delimiter = " ";
        break;
      case "tsv":
        delimiter = "\t";
        break;
      case "pipes":
        delimiter = "|";
        break;
    }

    StringBuilder sb = new StringBuilder();
    for (Object item : valueCollection) {
      sb.append(delimiter);
      sb.append(parameterToString(item));
    }

    params.add(new Pair(name, sb.substring(1)));

    return params;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String selectHeaderAccept(String[] accepts) {
    if (accepts.length == 0) {
      return null;
    }
    for (String accept : accepts) {
      if (isJsonMime(accept)) {
        return accept;
      }
    }
    return String.join(",", accepts);
  }

  protected boolean isJsonMime(String mime) {
    String jsonMime = "(?i)^(application/json|[^;/ \t]+/[^;/ \t]+[+]json)[ \t]*(;.*)?$";
    return mime != null && (mime.matches(jsonMime) || mime.equals("*/*"));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String selectHeaderContentType(String[] contentTypes) {
    if (contentTypes.length == 0) {
      return "application/json";
    }
    for (String contentType : contentTypes) {
      if (isJsonMime(contentType)) {
        return contentType;
      }
    }
    return contentTypes[0];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String escapeString(String str) {
    try {
      return URLEncoder.encode(str, "utf8").replaceAll("\\+", "%20");
    } catch (UnsupportedEncodingException e) {
      return str;
    }
  }
}