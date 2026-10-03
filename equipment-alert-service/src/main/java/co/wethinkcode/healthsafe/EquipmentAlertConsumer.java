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

/**
 * Consumes equipment failure alerts from the ActiveMQ queue and logs them.
 */
public class EquipmentAlertConsumer implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Connection connection;
    private Session session;
    private MessageConsumer consumer;

    public EquipmentAlertConsumer() {
    }

    public void start() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            factory.setSendTimeout(3000);
            Connection c = factory.createConnection();
            try {
                Session s = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
                // Note: QUEUE, not topic
                javax.jms.Queue queue = s.createQueue(MqConfig.QUEUE);
                MessageConsumer consumer = s.createConsumer(queue);
                consumer.setMessageListener(message -> {
                    if (message instanceof javax.jms.TextMessage textMessage) {
                        try {
                            String json = textMessage.getText();
                            // We expect a simple map; we can deserialize to JsonNode or just log.
                            System.out.println("equipment-alert-service: received equipment failure alert: " + json);
                        } catch (JMSException | JsonProcessingException e) {
                            System.err.println("equipment-alert-service: could not process equipment failure alert: " + e.getMessage());
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

    @Override
    public void close() {
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