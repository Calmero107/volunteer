package com.web.volunteer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.web.volunteer.dto.response.EventResponse;
import com.web.volunteer.dto.response.UserResponse;
import com.web.volunteer.entity.Event;
import com.web.volunteer.entity.User;
import com.web.volunteer.repository.EventRepository;
import com.web.volunteer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Export events to CSV
     */
    @Transactional(readOnly = true)
    public String exportEventsToCSV() throws IOException {
        logger.info("Exporting events to CSV");

        List<Event> events = eventRepository.findAll();

        StringWriter writer = new StringWriter();
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader("ID", "Title", "Description", "Location", "Event Date",
                        "Status", "Creator Email", "Max Participants", "Created At"));

        for (Event event : events) {
            csvPrinter.printRecord(
                    event.getId(),
                    event.getTitle(),
                    event.getDescription(),
                    event.getLocation(),
                    event.getEventDate(),
                    event.getStatus(),
                    event.getCreator().getEmail(),
                    event.getMaxParticipants(),
                    event.getCreatedAt()
            );
        }

        csvPrinter.flush();
        logger.info("Exported {} events to CSV", events.size());
        return writer.toString();
    }

    /**
     * Export events to JSON
     */
    @Transactional(readOnly = true)
    public String exportEventsToJSON() throws IOException {
        logger.info("Exporting events to JSON");

        List<Event> events = eventRepository.findAll();
        List<EventResponse> eventResponses = events.stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String json = mapper.writeValueAsString(eventResponses);
        logger.info("Exported {} events to JSON", events.size());
        return json;
    }

    /**
     * Export users to CSV
     */
    @Transactional(readOnly = true)
    public String exportUsersToCSV() throws IOException {
        logger.info("Exporting users to CSV");

        List<User> users = userRepository.findAll();

        StringWriter writer = new StringWriter();
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader("ID", "Full Name", "Email", "Phone", "Role",
                        "Active", "Locked", "Created At"));

        for (User user : users) {
            csvPrinter.printRecord(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhoneNumber(),
                    user.getRole(),
                    user.isActive(),
                    user.isLocked(),
                    user.getCreatedAt()
            );
        }

        csvPrinter.flush();
        logger.info("Exported {} users to CSV", users.size());
        return writer.toString();
    }

    /**
     * Export users to JSON
     */
    @Transactional(readOnly = true)
    public String exportUsersToJSON() throws IOException {
        logger.info("Exporting users to JSON");

        List<User> users = userRepository.findAll();
        List<UserResponse> userResponses = users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String json = mapper.writeValueAsString(userResponses);
        logger.info("Exported {} users to JSON", users.size());
        return json;
    }

    // ========== Private Helper Methods ==========

    private EventResponse mapToEventResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .eventDate(event.getEventDate())
                .registrationDeadline(event.getRegistrationDeadline())
                .maxParticipants(event.getMaxParticipants())
                .status(event.getStatus().name())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .active(user.isActive())
                .locked(user.isLocked())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}