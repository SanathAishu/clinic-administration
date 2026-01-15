package com.clinic.common.dto.request;

import com.clinic.common.enums.BloodGroup;
import com.clinic.common.enums.Gender;
import com.clinic.common.enums.MaritalStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePatientRequest {

    // Demographics
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Middle name cannot exceed 100 characters")
    private String middleName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Gender gender;
    private BloodGroup bloodGroup;

    // Contact Information
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Phone number must be valid")
    private String phone;

    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Phone number must be valid")
    private String alternatePhone;

    // Address
    private String addressLine1;
    private String addressLine2;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @Size(max = 10, message = "Pincode cannot exceed 10 characters")
    private String pincode;

    // ABHA Integration
    @Pattern(regexp = "^\\d{14}$", message = "ABHA ID must be 14 digits")
    private String abhaId;

    @Size(max = 17, message = "ABHA number cannot exceed 17 characters")
    private String abhaNumber;

    // Additional Information
    private MaritalStatus maritalStatus;

    @Size(max = 100, message = "Occupation cannot exceed 100 characters")
    private String occupation;

    @Size(max = 200, message = "Emergency contact name cannot exceed 200 characters")
    private String emergencyContactName;

    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Phone number must be valid")
    private String emergencyContactPhone;

    @Size(max = 50, message = "Emergency contact relation cannot exceed 50 characters")
    private String emergencyContactRelation;

    // Medical Information
    private String[] allergies;
    private String[] chronicConditions;
}
