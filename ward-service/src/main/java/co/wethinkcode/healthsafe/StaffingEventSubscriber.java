package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.mq.MqConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Topic;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Subscribes to staffing-events-topic and caches the latest event per ward.
 * This allows ward-service to react to staffing updates without polling staffing-service.
 */
public class StaffingEventSubscriber implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Connection connection;
    private Session session;
    private MessageConsumer consumer;

    /** thread‑safe cache of the most recent event per ward */
    private final Map<String, StaffingEvent> latestEvents = Collections.synchronizedMap(new HashMap<>());

    public StaffingEventSubscriber() {
        // intentionally does not connect until start()
    }

    /** Connects to the broker and starts listening for messages. */
    public void start() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            factory.setSendTimeout(3000);
            Connection c = factory.createConnection();
            try {
                Session s = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Topic t = s.createTopic(MqConfig.TOPIC);
                MessageConsumer consumer = s.createConsumer(t);
                consumer.setMessageListener(message -> {
                    if (message instanceof javax.jms.TextMessage textMessage) {
                        try {
                            String json = textMessage.getText();
                            StaffingEvent event = MAPPER.readValue(json, StaffingEvent.class);
                            latestEvents.put(event.wardId(), event);
                            System.out.println("ward-service: received staffing event for ward " + event.wardId());
                        } catch (JMSException | JsonProcessingException e) {
                            System.err.println("ward-service: could not process staffing event: " + e.getMessage());
                        }
                    }
                });
                c.start();
                this.connection = c;
                this.session = s;
                this.consumer = consumer;
            } catch (JMSException e) {
                closeQuietly(c);
                throw e;
            }
        } catch (JMSException e) {
            throw new RuntimeException("could not connect to ActiveMQ at " + MqConfig.BROKER_URL, e);
        }
    }

    /** Returns a copy of the cached events (wardId -> event). */
    public Map<String, StaffingEvent> getLatestEvents() {
        return Collections.unmodifiableMap(new HashMap<>(latestEvents));
    }

    /** Returns the most recent event for the given ward, or null if none received yet. */
    public StaffingEvent getLatestEvent(String wardId) {
        return latestEvents.get(wardId);
    }

    @Override
    public void close() {
        // closeQuietly(connection);
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException ignored) {
            }
        }
    }

    private static void closeQuietly(Connection c) {
        if (c != null) {
            try {
                c.close();
            } catch (JMSException ignored) {
            }
        }
    }
}