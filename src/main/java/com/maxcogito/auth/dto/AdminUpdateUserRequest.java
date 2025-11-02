package com.maxcogito.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Admin-only profile update. Does NOT update roles or password.
 * Fields that are null are ignored (kept as-is).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUpdateUserRequest {

    // immutable key: the path variable {username} selects the user
    // username change is NOT supported here

    @Email
    private String email;

    @Size(max = 64) private String firstName;
    @Size(max = 64) private String middleName;
    @Size(max = 64) private String lastName;

    private Boolean mfaRequired;

    @Size(max = 128) private String addressLine1;
    @Size(max = 128) private String addressLine2;
    @Size(max = 64)  private String city;
    @Size(max = 32)  private String state;
    @Size(max = 32)  private String postalCode;
    @Size(max = 64)  private String country;

    @Size(max = 32)  private String phoneNumber;

    // If you want email changes to auto-invalidate verification, set this to true in controller logic.
    // We do not expose emailVerified here to prevent admins from “forcibly verifying” without token proof.

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public Boolean getMfaRequired() { return mfaRequired; }
    public void setMfaRequired(Boolean mfaRequired) { this.mfaRequired = mfaRequired; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}

