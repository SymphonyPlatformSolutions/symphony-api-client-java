package com.symphony.bdk.core.service;

import com.symphony.bdk.core.api.invoker.ApiClient;
import com.symphony.bdk.core.api.invoker.ApiException;
import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.core.auth.exception.AuthenticationException;
import com.symphony.bdk.gen.api.MessagesApi;
import com.symphony.bdk.gen.api.model.V4Message;

import lombok.extern.slf4j.Slf4j;
import org.apiguardian.api.API;

/**
 * PLEASE PLEASE don't review this class !!
 */
@Slf4j
@API(status = API.Status.EXPERIMENTAL)
public class MessageService {

  private final MessagesApi messagesApi;

  public MessageService(ApiClient agentClient) {
    this.messagesApi = new MessagesApi(agentClient);
  }

  public V4Message sendMessage(AuthSession authSession, String streamId, String message) {
    try {
      return this.messagesApi.v4StreamSidMessageCreatePost(
          streamId,
          authSession.getSessionToken(),
          authSession.getKeyManagerToken(),
          message,
          null,
          null,
          null,
          null
      );
    } catch (ApiException e) {
      log.error("Cannot send message to stream {}", streamId, e);
      sleep(10_000);
      try {
        authSession.refresh();
      } catch (AuthenticationException exception) {
        log.error("Cannot authenticate", exception);
      }
      return this.sendMessage(authSession, streamId, message);
    }
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException ex) {
      // nothing to do
    }
  }
}
