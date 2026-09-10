package com.virtu_stock.DTO.Response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class IPOAlertsDTO {
    @AllArgsConstructor
    @Data
    public static class Error {
        private String id;
        private String name;
        private String message;
    }

    @AllArgsConstructor
    @Data
    public static class Saved {
        private UUID id;
        private String name;
        private String type;
    }

    @AllArgsConstructor
    @Data
    public static class Skipped {
        private String id;
        private String name;
        private String reason;
    }

    @AllArgsConstructor
    @Data
    public static class Exists {
        private String id;
        private String name;
    }
}
