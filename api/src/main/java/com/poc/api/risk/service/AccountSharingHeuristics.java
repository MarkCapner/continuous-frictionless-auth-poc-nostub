package com.poc.api.risk.service;

import com.poc.api.telemetry.persistence.DeviceProfile;
import com.poc.api.telemetry.persistence.DeviceProfileRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AccountSharingHeuristics {

  private final DeviceProfileRepository deviceProfileRepository;

  public AccountSharingHeuristics(DeviceProfileRepository deviceProfileRepository) {
    this.deviceProfileRepository = deviceProfileRepository;
  }

  public Result evaluate(String userId) {
    List<DeviceProfile> profiles = deviceProfileRepository.findByUser(userId);
    if (profiles.isEmpty()) {
      return new Result(false, 0, 0);
    }
    Set<String> tlsFingerprints = new HashSet<>();
    Set<String> countries = new HashSet<>();
    for (DeviceProfile p : profiles) {
      if (p.tlsFp != null && !p.tlsFp.isBlank()) {
        tlsFingerprints.add(p.tlsFp);
      }
      if (p.lastCountry != null && !p.lastCountry.isBlank()) {
        countries.add(p.lastCountry);
      }
    }
    boolean suspicious = tlsFingerprints.size() >= 5 || countries.size() >= 3;
    return new Result(suspicious, tlsFingerprints.size(), countries.size());
  }

  public record Result(boolean suspicious, int tlsFingerprintCount, int countryCount) {}
}
