package com.example.authservice.controller;

import com.example.authservice.dto.AddressRequest;
import com.example.authservice.dto.AddressResponse;
import com.example.authservice.dto.UpdateProfileRequest;
import com.example.authservice.dto.UserProfileResponse;
import com.example.authservice.entity.Address;
import com.example.authservice.entity.User;
import com.example.authservice.repository.AddressRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe(Authentication authentication) {
        User user = authService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(toProfile(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request
    ) {
        User user = authService.getUserByEmail(authentication.getName());
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            userRepository.findByEmail(request.getEmail())
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Email already in use");
                    });
            user.setEmail(request.getEmail());
        }
        User saved = userRepository.save(user);
        if (request.getAddresses() != null) {
            syncAddresses(saved.getId(), request.getAddresses());
        }
        return ResponseEntity.ok(toProfile(saved));
    }

    private UserProfileResponse toProfile(User user) {
        List<AddressResponse> addresses = addressRepository.findByUserId(user.getId())
            .stream()
            .map(this::toAddress)
            .toList();

        AddressResponse primaryAddress = addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                .map(this::toAddress)
                .orElse(null);

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.isActive())
                .primaryAddress(primaryAddress)
                .addresses(addresses)
                .build();
    }

    private void syncAddresses(String userId, List<AddressRequest> requests) {
        if (requests.size() > 3) {
            throw new IllegalArgumentException("A user can save up to 3 addresses only");
        }

        long defaultCount = requests.stream().filter(request -> Boolean.TRUE.equals(request.getIsDefault())).count();
        if (defaultCount > 1) {
            throw new IllegalArgumentException("Only one default address is allowed");
        }

        List<Address> existing = addressRepository.findByUserId(userId);
        Set<String> incomingIds = new HashSet<>();

        for (AddressRequest request : requests) {
            Address address;
            if (request.getId() != null && !request.getId().isBlank()) {
                address = existing.stream()
                        .filter(item -> item.getId().equals(request.getId()))
                        .findFirst()
                        .orElseGet(() -> Address.builder().userId(userId).build());
                incomingIds.add(request.getId());
            } else {
                address = Address.builder().userId(userId).build();
            }

            mapAddressFields(address, request);
            address.setDefault(Boolean.TRUE.equals(request.getIsDefault()));
            addressRepository.save(address);
            if (address.getId() != null) {
                incomingIds.add(address.getId());
            }
        }

        for (Address address : existing) {
            if (!incomingIds.contains(address.getId())) {
                addressRepository.delete(address);
            }
        }

        if (defaultCount == 0 && !requests.isEmpty()) {
            Address first = addressRepository.findByUserId(userId).stream().findFirst().orElse(null);
            if (first != null) {
                first.setDefault(true);
                addressRepository.save(first);
            }
        }
    }

    private void mapAddressFields(Address address, AddressRequest request) {
        if (request.getLabel() != null) {
            address.setLabel(request.getLabel());
        }

        if (request.getStreet() != null) {
            address.setStreet(request.getStreet());
        }

        if (request.getAddressLine1() != null) {
            address.setAddressLine1(request.getAddressLine1());
            if (address.getStreet() == null || address.getStreet().isBlank()) {
                address.setStreet(request.getAddressLine1());
            }
        }

        if (request.getAddressLine2() != null) {
            address.setAddressLine2(request.getAddressLine2());
        }

        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }

        if (request.getState() != null) {
            address.setState(request.getState());
        }

        if (request.getPostalCode() != null) {
            address.setPostalCode(request.getPostalCode());
        }

        if (request.getCountry() != null) {
            address.setCountry(request.getCountry());
        }

        if (request.getFormattedAddress() != null) {
            address.setFormattedAddress(request.getFormattedAddress());
            if ((address.getStreet() == null || address.getStreet().isBlank()) &&
                    address.getAddressLine1() == null) {
                address.setStreet(request.getFormattedAddress());
            }
        }

        if (request.getLatitude() != null) {
            address.setLatitude(request.getLatitude());
        }

        if (request.getLongitude() != null) {
            address.setLongitude(request.getLongitude());
        }

        if (request.getGooglePlaceId() != null) {
            address.setGooglePlaceId(request.getGooglePlaceId());
        }

        if (request.getLocationSource() != null) {
            address.setLocationSource(request.getLocationSource());
        }
    }

    private AddressResponse toAddress(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .label(address.getLabel())
                .street(address.getStreet())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .formattedAddress(address.getFormattedAddress())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .googlePlaceId(address.getGooglePlaceId())
                .locationSource(address.getLocationSource())
                .isDefault(address.isDefault())
                .build();
    }
}
