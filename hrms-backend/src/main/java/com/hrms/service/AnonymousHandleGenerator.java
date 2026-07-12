package com.hrms.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AnonymousHandleGenerator {

    private static final String[] ADJECTIVES = {
            "Quiet", "Steady", "Amber", "Calm", "Bold", "Gentle", "Swift", "Bright",
            "Silent", "Distant", "Curious", "Patient", "Vivid", "Nimble", "Wandering"
    };

    private static final String[] NOUNS = {
            "Falcon", "River", "Maple", "Compass", "Harbor", "Lantern", "Summit", "Meadow",
            "Sparrow", "Cedar", "Ridge", "Ember", "Beacon", "Willow", "Anchor"
    };

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        String adjective = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[random.nextInt(NOUNS.length)];
        int number = 1000 + random.nextInt(9000);
        return "Anonymous " + adjective + " " + noun + " " + number;
    }
}
