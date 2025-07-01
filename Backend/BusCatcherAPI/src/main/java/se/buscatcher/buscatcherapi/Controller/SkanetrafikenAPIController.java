package se.buscatcher.buscatcherapi.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for handling requests to the Skånetrafiken API.
 * It is called by the frontend when the user wants to find nearby stops or plan
 * a trip.
 * The methods in this class will call the Skånetrafiken API and return the
 * response to the frontend.
 */
@CrossOrigin(origins = "http://localhost:5500", allowedHeaders = "*")
@RestController
@RequestMapping("/api/skanetrafiken")
public class SkanetrafikenAPIController {

    @Value("${resrobot.api.key}")
    private String API_KEY;

    /**
     * This method is called when the user wants to find nearby stops.
     * This method is called only at the beginning when the user inputs their
     * start/stop location, but shall also be called when the user wants to update
     * their location
     * It will call the Skånetrafiken API and return the response to the frontend.
     * 
     * @param lat The latitude of the user's location
     * @param lon The longitude of the user's location
     * @return A JSON string containing the name and distance of each nearby stop
     * @throws IOException
     * @throws GeneralSecurityException
     */
    @GetMapping("/nearbyStops")
    public String getNearbyStops(@RequestParam("lat") String lat, @RequestParam("lon") String lon)
            throws IOException, GeneralSecurityException {

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://api.resrobot.se/v2.1/location.nearbystops");
        builder.queryParam("originCoordLat", lat);
        builder.queryParam("originCoordLong", lon);
        builder.queryParam("format", "json");
        builder.queryParam("accessId", API_KEY);
        // api.resrobot.se/v2.1/location.nearbystops?originCoordLat=57.708895&originCoordLong=11.973479&format=json&accessId=API_KEY
        // final String url =
        // "https://api.resrobot.se/v2.1/location.nearbystops?originCoordLat=" + lat +
        // "&originCoordLong=" + lon + "&format=json&accessId=" + API_KEY;

        RestTemplate restTemplate = new RestTemplate();
        // ResponseEntity<String> response = restTemplate.getForEntity(url,
        // String.class);
        ResponseEntity<String> response = restTemplate.getForEntity(builder.toUriString(), String.class);

        // return response.getBody();
        return parseNearbyStops(response);
    }

    /**
     * This method parses the response from the Skånetrafiken API and returns a JSON
     * string containing the name and distance of each nearby stop.
     * 
     * @param response
     * @return
     * @throws JsonProcessingException
     */
    public String parseNearbyStops(ResponseEntity<String> response) throws JsonProcessingException {
        // Parse the response
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.getBody());
        JsonNode stopLocations = root.get("stopLocationOrCoordLocation");

        // Extract the name and dist from each StopLocation and add them to a list
        List<Map<String, String>> parsedStops = new ArrayList<>();
        for (JsonNode item : stopLocations) {
            JsonNode stopLocation = item.get("StopLocation");
            Map<String, String> parsedStop = new HashMap<>();
            parsedStop.put("name", stopLocation.get("name").asText());
            parsedStop.put("dist", stopLocation.get("dist").asText());
            parsedStops.add(parsedStop);
        }
        // Convert the list to a JSON string and return it
        return mapper.writeValueAsString(parsedStops);
    }

    /**
     * This method is called when the user wants to plan a trip.
     * It will call the Skånetrafiken API and return the response to the frontend.
     * 
     * @param originLat The latitude of the user's origin
     * @param originLon The longitude of the user's origin
     * @param destLat   The latitude of the user's destination
     * @param destLon   The longitude of the user's destination
     * @return A JSON string containing the trip information
     * @throws IOException
     * @throws GeneralSecurityException
     */
    @GetMapping("/planTrip/{originLat},{originLon}/{destLat},{destLon}/{DepartOrArrive}/{arrivalTime}") // "/planTrip/{originLat},{originLon}/{destLat},{destLon}/"
    public String planTrip(@PathVariable String originLat, @PathVariable String originLon, @PathVariable String destLat,
            @PathVariable String destLon, @PathVariable String DepartOrArrive, @PathVariable String arrivalTime)
            throws IOException, GeneralSecurityException {
        // ?originId=740000001&destId=740000003&passlist=true&showPassingPoints=true&accessId=API_KEY
        if (arrivalTime.length() >= 2) {
            arrivalTime = arrivalTime.substring(0, 2) + ":" + arrivalTime.substring(2);
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://api.resrobot.se/v2.1/trip");
        builder.queryParam("originCoordLat", originLat);
        builder.queryParam("originCoordLong", originLon);
        builder.queryParam("destCoordLat", destLat);
        builder.queryParam("destCoordLong", destLon);
        builder.queryParam("format", "json");
        builder.queryParam("showPassingPoints", true);
        builder.queryParam("time", arrivalTime);
        if (DepartOrArrive.equals("arrive")) {
            builder.queryParam("searchForArrival", "1");
        }
        builder.queryParam("accessId", API_KEY);

        RestTemplate restTemplate = new RestTemplate();
        // ResponseEntity<String> response = restTemplate.getForEntity(url,
        // String.class);
        ResponseEntity<String> response = restTemplate.getForEntity(builder.toUriString(), String.class);

        //return response.getBody();
        return parseTrip(response);
    }

    public String parseTrip(ResponseEntity<String> response) throws JsonProcessingException {
        // Parse the response
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.getBody());
        JsonNode trips = root.get("Trip");

        // Extract the details from each Trip and add them to a list
        List<Map<String, Object>> parsedTrips = new ArrayList<>();
        for (JsonNode trip : trips) {
            Map<String, Object> parsedTrip = new HashMap<>();
            JsonNode originNode = trip.get("Origin");
            if (originNode != null) {
                parsedTrip.put("OriginTime", originNode.get("time").asText());
                parsedTrip.put("OriginDate", originNode.get("date").asText());
            }
            JsonNode destNode = trip.get("Destination");
            if (destNode != null) {
                parsedTrip.put("DestinationTime", destNode.get("time").asText());
            }
            parsedTrip.put("duration", trip.get("duration"));

            List<Map<String, String>> parsedLegs = new ArrayList<>();
            for (JsonNode leg : trip.get("LegList").get("Leg")) {
                Map<String, String> parsedLeg = new HashMap<>();
                JsonNode legOriginNode = leg.get("Origin");
                if (legOriginNode != null) {
                    parsedLeg.put("OriginName", legOriginNode.get("name").asText());
                    parsedLeg.put("OriginTime", legOriginNode.get("time").asText());
                }
                JsonNode destinationNode = leg.get("Destination");
                if (destinationNode != null) {
                    parsedLeg.put("DestinationName", destinationNode.get("name").asText());
                    parsedLeg.put("DestinationTime", destinationNode.get("time").asText());
                }
                JsonNode nameNode = leg.get("name");
                if (nameNode != null) {
                    parsedLeg.put("name", nameNode.asText());
                }
                JsonNode typeNode = leg.get("type");
                if (typeNode != null) {
                    parsedLeg.put("type", typeNode.asText());
                }
                JsonNode durationNode = leg.get("duration");
                if (durationNode != null) {
                    parsedLeg.put("duration", durationNode.asText());
                }
                JsonNode distNode = leg.get("dist");
                if (distNode != null) {
                    parsedLeg.put("dist", distNode.asText());
                }
                parsedLegs.add(parsedLeg);
            }
            parsedTrip.put("LegList", parsedLegs);

            parsedTrips.add(parsedTrip);
        }

        // Convert the list to a JSON string and return it
        return mapper.writeValueAsString(parsedTrips);
    }

    // https://api.resrobot.se/v2.1/trip?format=json&originId=740000001&destId=740000003&passlist=true&showPassingPoints=true&accessId=API_KEY
}