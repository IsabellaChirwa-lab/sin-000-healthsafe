package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.mq.MqConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.util.Objects;

/**
 * Publishes equipment failure alerts to the ActiveMQ QUEUE. Messages are PERSISTENT
 * to guarantee delivery even if the consumer is temporarily down.
 */
public class EquipmentFailurePublisher implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Connection connection;
    private Session session;
    private MessageProducer producer;

    public synchronized void publish(String wardId, String issue) {
        try {
            ensureConnected();
            var message = session.createTextMessage(
                    MAPPER.writeValueAsString(new Alert(wardId, issue))
            );
            message.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(message);
        } catch (JMSException | JsonProcessingException e) {
            disconnect(); // start fresh next time
            throw new RuntimeException("could not publish to " + MqConfig.QUEUE + ": " + e.getMessage(), e);
        }
    }

    private void ensureConnected() throws JMSException {
        if (connection != null) {
            return;
        }
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
        factory.setSendTimeout(3000);
        Connection c = factory.createConnection();
        try {
            Session s = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer p = s.createProducer(s.createQueue(MqConfig.QUEUE));
            p.setDeliveryMode(DeliveryMode.PERSISTENT);
            c.start();
            connection = c;
            session = s;
            producer = p;
        } catch (JMSException e) {
            closeQuietly(c);
            throw e;
        }
    }

    private void disconnect() {
        closeQuietly(connection);
        connection = null;
        session = null;
        producer = null;
    }

    private static void closeQuietly(Connection c) {
        if (c != null) {
            try {
                c.close();
            } catch (JMSException ignored) {
            }
        }
    }

    @Override
    public synchronized void close() {
        disconnect();
    }

    /** Simple payload for an equipment failure alert. */
    public static record Alert(String wardId, String issue, String occurredAt) {
        public Alert(String wardId, String issue) {
            this(wardId, issue, java.time.Instant.now().toString());
        }
    }
}