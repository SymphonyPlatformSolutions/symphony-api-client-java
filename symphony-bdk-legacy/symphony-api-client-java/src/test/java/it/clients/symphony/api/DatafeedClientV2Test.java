package it.clients.symphony.api;

import authentication.SymBotRSAAuth;
import clients.SymBotClient;
import clients.symphony.api.constants.PodConstants;
import clients.symphony.api.DatafeedClient;
import clients.symphony.api.constants.AgentConstants;
import exceptions.SymClientException;
import it.commons.BotTest;
import model.DatafeedEvent;
import model.EventPayload;
import model.InboundMessage;
import model.Initiator;
import model.Stream;
import model.User;
import model.datafeed.DatafeedV2;
import model.events.MessageSent;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

public class DatafeedClientV2Test extends BotTest {

    private  static  final Logger logger = LoggerFactory.getLogger(DatafeedClientV2Test.class);
    private DatafeedClient datafeedClient;

    @Before
    public void initClient() throws IOException {
        stubGet(PodConstants.GETSESSIONUSER,
                readResourceContent("/response_content/authenticate/get_session_user.json"));
        config.setDatafeedVersion("v2");
        SymBotRSAAuth auth = new SymBotRSAAuth(config);
        auth.setSessionToken("eyJhbGciOiJSUzUxMiJ9.eyJzdWIiOiJ0ZXN0LWJvdCIsImlzcyI6InN5bXBob255Iiwic2Vzc2lvbklkIjoiZmRiOTAxMmQzOTgwMGE3NzNkMTJjYWFmZGY5MjU4ZjZjOWEyMTE2MmYyZDU1ODQ3M2Y5ZDU5MDUyNjA0Mjg1ZjU0MWM5Yzg0Mzc5YTE0MjZmODNiZmZkZTljYmQ5NjRjMDAwMDAxNmRmMjMyODIwNTAwMDEzZmYwMDAwMDAxZTgiLCJ1c2VySWQiOiIzNTE3NzUwMDE0MTIwNzIifQ.DlQ_-sAqZLlAcVTr7t_PaYt_Muq_P82yYrtbEEZWMpHMl-7qCciwfi3uXns7oRbc1uvOrhQd603VKQJzQxaZBZBVlUPS-2ysH0tBpCS57ocTS6ZwtQwPLCZYdT-EZ70EzQ95kG6P5TrLENH6UveohgeDdmyzSPOEiwyEUjjmzaXFE8Tu0R3xQDwl-BKbsyUAAgd1X7T0cUDC3WIDl9xaTvyxavep4ZJnZJl4qPc1Tan0yU7JrxtXeD8uwNYlKLudT3UVxduFPMQP_2jyj5Laa-YWGKvRtXkcy2d3hzf4ll1l1wVnyJc1e6hW2EnRlff_Nxge-QCJMcZ_ALrpOUtAyQ");
        symBotClient = SymBotClient.initBot(config, auth);
        datafeedClient = symBotClient.getDatafeedClient();
    }

  @Test
  public void createDatafeedSuccess() throws IOException {
    stubPost(AgentConstants.CREATEDATAFEEDV2,
        readResourceContent("/response_content/datafeedv2/create_datafeedv2.json"));

    try {

      assertNotNull(datafeedClient);

      final String datafeedId = datafeedClient.createDatafeed();

      assertEquals("21449143d35a86461e254d28697214b4_f", datafeedId);

    } catch (SymClientException e) {
      fail();
    }
  }

    @Test
    public void listDatafeedIdsSuccess() throws IOException {
        stubGet(AgentConstants.LISTDATAFEEDV2,
                readResourceContent("/response_content/datafeedv2/list_datafeedv2.json"));

      try {

        assertNotNull(datafeedClient);

        final List<DatafeedV2> datafeedIds = datafeedClient.listDatafeedId();
        assertNotNull(datafeedIds);

        assertEquals(3, datafeedIds.size());

        final List<String> ids = new ArrayList(Arrays.asList(
                                      "2c2e8bb339c5da5711b55e32ba7c4687_f",
                                      "4dd10564ef289e053cc59b2092080c3b_f",
                                      "83b69942b56288a14d8625ca2c85f264_f"));

        final  List<Long> createdAts = new ArrayList(Arrays.asList(
                                            1536346282592L,
                                            1536346282592L,
                                            1536346282592L));

        for(int i = 0; i < 3; i++) {

          assertEquals(ids.get(i), datafeedIds.get(i).getId());
          assertEquals(createdAts.get(i).longValue(), datafeedIds.get(i).getCreatedAt().longValue());
        }
      } catch (SymClientException e) {
        fail();
      }
    }

    @Test
    public void readDatafeedEventSuccess() throws IOException {
      stubPost(AgentConstants.READDATAFEEDV2.replace("{id}", "21449143d35a86461e254d28697214b4_f"),
              readResourceContent("/response_content/datafeedv2/read_datafeedv2.json"));

      try {

        assertNotNull(datafeedClient);

        final List<DatafeedEvent> datafeedEvents = datafeedClient.readDatafeed("21449143d35a86461e254d28697214b4_f");

        assertEquals("ack_id_string", datafeedClient.getAckId());

        assertEquals(1, datafeedEvents.size());

        final DatafeedEvent event = datafeedEvents.get(0);
        assertNotNull(event);
        assertEquals("ulPr8a:eFFDL7", event.getId());
        assertEquals("CszQa6uPAA9V", event.getMessageId());
        assertEquals(1536346282592L, event.getTimestamp().longValue());
        assertEquals("MESSAGESENT", event.getType());

        final Initiator initiator = event.getInitiator();
        assertNotNull(initiator);
        final User user = initiator.getUser();
        assertNotNull(user);
        assertEquals(1456852L, user.getUserId().longValue());
        assertEquals("Local Bot01", user.getDisplayName());
        assertEquals("bot.user1@test.com", user.getEmail());
        assertEquals("bot.user1", user.getUsername());

        final EventPayload payload = event.getPayload();
        assertNotNull(payload);
        final MessageSent messageSent = payload.getMessageSent();
        assertNotNull(messageSent);
        final InboundMessage message = messageSent.getMessage();
        assertNotNull(message);
        assertEquals("CszQa6uPAA9", message.getMessageId());
        assertEquals(1536346282592L, message.getTimestamp().longValue());
        final String expectedMessage = "<div data-format=\"PresentationML\" data-version=\"2.0\">Hello World</div>";
        assertEquals(expectedMessage, message.getMessage());

        final User messageUser = message.getUser();
        assertNotNull(messageUser);
        assertEquals(14568529L, messageUser.getUserId().longValue());
        assertEquals("Local Bot01", messageUser.getDisplayName());
        assertEquals("bot.user1@test.com", messageUser.getEmail());
        assertEquals("bot.user1", messageUser.getUsername());

        final Stream stream = message.getStream();
        assertNotNull(stream);
        assertEquals("wTmSDJSNPXgB", stream.getStreamId());
        assertEquals("ROOM", stream.getStreamType());

        assertFalse(message.getExternalRecipients());
        assertEquals("Agent-2.2.8-Linux-4.9.77-31.58.amzn1.x86_64", message.getUserAgent());
        assertEquals("com.symphony.messageml.v2", message.getOriginalFormat());

      } catch (SymClientException e) {
        fail();
      }
    }

    @Test
    public void deleteDatafeedSuccess() {
      stubFor(get(urlEqualTo(AgentConstants.DELETEDATAFEEDV2.replace("{id}", "1")))
          .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
          .willReturn(aResponse()
              .withStatus(200)
              .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
              .withBody("{"
                  + "\"id\": \"CszQa6uPAA9\","
                  + "\"createdAt\": 1536346282592"
                  + "}")));

      assertNotNull(datafeedClient);

      datafeedClient.deleteDatafeed("1");

      assertTrue(true);
    }
}
