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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        User user = authService.getUserByUsername(authentication.getName());
        return ResponseEntity.ok(toProfile(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request
    ) {
        User user = authService.getUserByUsername(authentication.getName());
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail());
        }
        User saved = userRepository.save(user);
        return ResponseEntity.ok(toProfile(saved));
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> addAddress(Authentication authentication, @RequestBody AddressRequest request) {
        User user = authService.getUserByUsername(authentication.getName());
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.isDefault()) {
            clearDefaultAddress(managedUser.getId());
        }

        Address saved = addressRepository.save(Address.builder()
                .userId(managedUser.getId())
                .street(request.getStreet())
                .city(request.getCity())
                .postalCode(request.getPostalCode())
                .isDefault(request.isDefault())
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(toAddress(saved));
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressResponse>> getAddresses(Authentication authentication) {
        User user = authService.getUserByUsername(authentication.getName());
        List<AddressResponse> response = addressRepository.findByUserId(user.getId())
                .stream()
                .map(this::toAddress)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody AddressRequest request
    ) {
        User user = authService.getUserByUsername(authentication.getName());

        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        if (request.getStreet() != null) {
            address.setStreet(request.getStreet());
        }
        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }
        if (request.getPostalCode() != null) {
            address.setPostalCode(request.getPostalCode());
        }
        if (request.isDefault()) {
            clearDefaultAddress(user.getId());
            address.setDefault(true);
        }

        Address saved = addressRepository.save(address);
        return ResponseEntity.ok(toAddress(saved));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(Authentication authentication, @PathVariable String id) {
        User user = authService.getUserByUsername(authentication.getName());
        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));
        addressRepository.delete(address);
        return ResponseEntity.noContent().build();
    }

    private void clearDefaultAddress(String userId) {
        List<Address> addresses = addressRepository.findByUserId(userId);
        for (Address address : addresses) {
            if (address.isDefault()) {
                address.setDefault(false);
                addressRepository.save(address);
            }
        }
    }

    private UserProfileResponse toProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }

    private AddressResponse toAddress(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .city(address.getCity())
                .postalCode(address.getPostalCode())
                .isDefault(address.isDefault())
                .build();
    }
}
