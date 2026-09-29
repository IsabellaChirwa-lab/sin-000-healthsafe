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

/**
 * Publishes to the ActiveMQ TOPIC. Messages are NON_PERSISTENT: this is a fire-and-forget broadcast
 * of "current state", and the next change (or request) re-derives it, so we don't pay for disk writes.
 * The connection is created lazily and re-created after a failure.
 */
public class JmsStaffingEventPublisher implements StaffingEventPublisher, AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Connection connection;
    private Session session;
    private MessageProducer producer;

    @Override
    public synchronized void publish(StaffingEvent event) {
        try {
            ensureConnected();
            producer.send(session.createTextMessage(MAPPER.writeValueAsString(event)));
        } catch (JMSException | JsonProcessingException e) {
            disconnect(); // start fresh next time
            throw new PublishException("could not publish to " + MqConfig.TOPIC + ": " + e.getMessage(), e);
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
            MessageProducer p = s.createProducer(s.createTopic(MqConfig.TOPIC));
            p.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
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
                // nothing useful to do
            }
        }
    }

    @Override
    public synchronized void close() {
        disconnect();
    }
}