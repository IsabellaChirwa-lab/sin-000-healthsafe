package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

public class EquipmentAlertServiceApp {

    public static void main(String[] args) {
        EquipmentAlertConsumer consumer = new EquipmentAlertConsumer();
        consumer.start();
        // Ensure consumer is closed on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::close));

        Javalin app = Javalin.create().start(7034);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Uses a Queue to guarantee delivery of critical medical equipment failure alerts.)
        // Mechanism: ActiveMQ Queue (guaranteed delivery)
        // Now implemented via EquipmentAlertConsumer.
    }
}