package usagebilling;

import usagebilling.controller.UsageEventController;
import usagebilling.controller.UsageSummaryController;
import usagebilling.repository.InMemoryPriceRepository;
import usagebilling.repository.InMemoryUsageEventRepository;
import usagebilling.repository.PriceRepository;
import usagebilling.repository.UsageEventRepository;
import usagebilling.service.PricingService;
import usagebilling.service.UsageAggregationService;
import usagebilling.service.UsageEventService;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args)
            throws Exception {

        /*
         * -------------------------------
         * Repository layer
         * -------------------------------
         */

        UsageEventRepository eventRepository =
                new InMemoryUsageEventRepository();

        PriceRepository priceRepository =
                new InMemoryPriceRepository();


        /*
         * -------------------------------
         * Service layer
         * -------------------------------
         */

        UsageEventService eventService =
                new UsageEventService(
                        eventRepository
                );

        UsageAggregationService
                aggregationService =
                new UsageAggregationService(
                        eventRepository
                );

        PricingService pricingService =
                new PricingService(
                        priceRepository
                );


        /*
         * -------------------------------
         * Controller layer
         * -------------------------------
         */

        UsageEventController
                eventController =
                new UsageEventController(
                        eventService
                );

        UsageSummaryController
                summaryController =
                new UsageSummaryController(
                        aggregationService,
                        pricingService
                );


        /*
         * -------------------------------
         * HTTP server
         * -------------------------------
         */

        int port = 8080;

        HttpServer server =
                HttpServer.create(
                        new InetSocketAddress(port),
                        0
                );

        server.createContext(
                "/v1/usage/events",
                eventController
        );

        server.createContext(
                "/v1/customers",
                summaryController
        );

        server.setExecutor(null);

        server.start();

        System.out.println(
                "Usage Billing Service started on port "
                        + port
        );
    }
}