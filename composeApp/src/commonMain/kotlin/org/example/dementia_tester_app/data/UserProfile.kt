package org.example.dementia_tester_app.data

/**
 * Enum representing the types of users in the system.
 *
 * For the current project implementation:
 * - USER = patient
 * - DOCTOR = clinician and admin
 * - CAREGIVER = caregiver
 *
 * A separate ADMIN role can be introduced later if required.
 */
enum class UserType(val value: String) {
    USER("user"),
    DOCTOR("doctor"),
    CAREGIVER("caregiver");

    // ADMIN is not currently used as a separate user type.
    // For now, the DOCTOR / clinician also acts as ADMIN.
    // Uncomment or reintroduce this role later if a separate
    // administrator account is required.
    // ADMIN("admin");

    companion object {

        /**
         * Convert a string value from the database to UserType.
         *
         * Defaults to USER if the value is missing or unknown.
         */
        fun fromString(value: String): UserType {
            return entries.find {
                it.value.equals(value, ignoreCase = true)
            } ?: USER
        }
    }
}

/**
 * Data class representing a user profile.
 *
 * User profiles are stored in Firebase Realtime Database under:
 *
 * UserProfiles/{userId}
 */
data class UserProfile(

    // User details
    val userId: String = "",
    val name: String = "",
    val dateOfBirth: String = "", // Format: DD/MM/YYYY
    val email: String = "",
    val phoneNumber: String = "",
    val userType: UserType = UserType.USER,

    /**
     * Firebase UID of the doctor assigned to this patient.
     *
     * Used to ensure doctors only see patients assigned to them.
     *
     * This will normally be empty for doctor and caregiver profiles.
     */
    val assignedDoctorId: String = "",

    /**
     * Firebase UID of the caregiver assigned to this patient.
     *
     * Used to ensure caregivers only access patients assigned to them.
     *
     * This will normally be empty for doctor and caregiver profiles.
     */
    val assignedCaregiverId: String = "",

    // Address fields
    val address: String = "",
    val suburb: String = "",
    val state: String = "",
    val postcode: String = "",
    val country: String = "",

    val gender: String = "",

    // Emergency contact details
    val emergencyName: String = "",
    val emergencyEmail: String = "",
    val emergencyRelation: String = "",
    val emergencyPhoneNumber: String = "",

    // Firebase Storage image URL
    val profileImageUrl: String = ""
) {

    /**
     * Convert UserProfile into a map for Firebase Realtime Database.
     */
    fun toMap(): Map<String, Any> {

        /*
         * Convert the date from:
         *
         * DD/MM/YYYY
         *
         * to:
         *
         * DD-MMM-YYYY
         */
        val dateStr = dateOfBirth

        val formattedDate =
            if (dateStr.isNotEmpty()) {

                try {

                    val dateParts =
                        dateStr.split("/")

                    if (dateParts.size == 3) {

                        val day =
                            dateParts[0]

                        val monthNum =
                            dateParts[1]
                                .toIntOrNull()
                                ?: 1

                        val month =
                            monthNumberToName(
                                monthNum
                            )

                        val year =
                            dateParts[2]

                        "$day-$month-$year"

                    } else {

                        dateStr
                    }

                } catch (e: Exception) {

                    dateStr
                }

            } else {

                dateStr
            }

        /*
         * Keep the combined address field for
         * backward compatibility with older records.
         */
        val fullAddress =
            listOf(
                address,
                suburb,
                state,
                postcode,
                country
            )
                .filter {
                    it.isNotEmpty()
                }
                .joinToString(", ")

        return mapOf(

            // User information
            "userId" to userId,
            "fullName" to name,
            "dateOfBirth" to formattedDate,
            "email" to email,
            "contactNumber" to phoneNumber,
            "userType" to userType.value,

            // Patient-doctor relationship
            "assignedDoctorId" to assignedDoctorId,

            // Patient-caregiver relationship
            "assignedCaregiverId" to assignedCaregiverId,

            // Combined address retained for compatibility
            "address" to fullAddress,

            // Individual address fields
            "streetAddress" to address,
            "suburb" to suburb,
            "state" to state,
            "postcode" to postcode,
            "country" to country,

            // Other profile details
            "gender" to gender,

            // Emergency contact
            "emergencyContactName" to emergencyName,
            "emergencyEmail" to emergencyEmail,
            "relation" to emergencyRelation,
            "emergencyContactNumber" to emergencyPhoneNumber,

            // Profile image
            "profileImageUrl" to profileImageUrl
        )
    }

    companion object {

        /**
         * Convert month number to three-letter month name.
         *
         * Example:
         * 1 -> JAN
         * 12 -> DEC
         */
        fun monthNumberToName(
            monthNumber: Int
        ): String {

            return when (monthNumber) {

                1 -> "JAN"
                2 -> "FEB"
                3 -> "MAR"
                4 -> "APR"
                5 -> "MAY"
                6 -> "JUN"
                7 -> "JUL"
                8 -> "AUG"
                9 -> "SEP"
                10 -> "OCT"
                11 -> "NOV"
                12 -> "DEC"

                else -> "JAN"
            }
        }

        /**
         * Convert three-letter month name
         * to a two-digit month number.
         *
         * Example:
         * JAN -> 01
         * DEC -> 12
         */
        fun monthNameToNumber(
            monthName: String
        ): String {

            return when (
                monthName.uppercase()
            ) {

                "JAN" -> "01"
                "FEB" -> "02"
                "MAR" -> "03"
                "APR" -> "04"
                "MAY" -> "05"
                "JUN" -> "06"
                "JUL" -> "07"
                "AUG" -> "08"
                "SEP" -> "09"
                "OCT" -> "10"
                "NOV" -> "11"
                "DEC" -> "12"

                else -> "01"
            }
        }

        /**
         * Create a UserProfile object from
         * Firebase Realtime Database data.
         */
        fun fromMap(
            map: Map<*, *>,
            userId: String
        ): UserProfile {

            /**
             * Safely read a String value.
             *
             * Missing fields return an empty string,
             * which keeps older database records compatible.
             */
            fun getStringValue(
                key: String
            ): String {

                return map[key]
                    ?.toString()
                    ?: ""
            }

            /*
             * Read date of birth.
             */
            val dobStr =
                getStringValue(
                    "dateOfBirth"
                )

            /*
             * Convert stored:
             *
             * DD-MMM-YYYY
             *
             * back to:
             *
             * DD/MM/YYYY
             */
            val formattedDate =
                if (dobStr.isNotEmpty()) {

                    try {

                        if (
                            dobStr.contains("/")
                        ) {

                            // Already in app format
                            dobStr

                        } else {

                            val dateParts =
                                dobStr.split("-")

                            if (
                                dateParts.size == 3
                            ) {

                                val day =
                                    dateParts[0]

                                val monthStr =
                                    dateParts[1]
                                        .uppercase()

                                val month =
                                    monthNameToNumber(
                                        monthStr
                                    )

                                val year =
                                    dateParts[2]

                                "$day/$month/$year"

                            } else {

                                dobStr
                            }
                        }

                    } catch (e: Exception) {

                        dobStr
                    }

                } else {

                    dobStr
                }

            /*
             * Read the newer separate
             * address fields first.
             */
            val streetAddress =
                getStringValue(
                    "streetAddress"
                )

            val storedSuburb =
                getStringValue(
                    "suburb"
                )

            val storedState =
                getStringValue(
                    "state"
                )

            val storedPostcode =
                getStringValue(
                    "postcode"
                )

            val storedCountry =
                getStringValue(
                    "country"
                )

            /*
             * Read the older combined address
             * for backward compatibility.
             */
            val fullAddress =
                getStringValue(
                    "address"
                )

            val addressParts =
                fullAddress.split(", ")

            /*
             * Prefer the separate fields.
             * If they don't exist, fall back
             * to the older combined address.
             */
            val addressComponent =
                streetAddress.ifBlank {

                    addressParts
                        .getOrNull(0)
                        ?: ""
                }

            val suburbComponent =
                storedSuburb.ifBlank {

                    addressParts
                        .getOrNull(1)
                        ?: ""
                }

            val stateComponent =
                storedState.ifBlank {

                    addressParts
                        .getOrNull(2)
                        ?: ""
                }

            val postcodeComponent =
                storedPostcode.ifBlank {

                    addressParts
                        .getOrNull(3)
                        ?: ""
                }

            val countryComponent =
                storedCountry.ifBlank {

                    addressParts
                        .getOrNull(4)
                        ?: ""
                }

            /*
             * Build and return the UserProfile.
             */
            return UserProfile(

                userId =
                    userId,

                name =
                    getStringValue(
                        "fullName"
                    ),

                dateOfBirth =
                    formattedDate,

                email =
                    getStringValue(
                        "email"
                    ),

                phoneNumber =
                    getStringValue(
                        "contactNumber"
                    ),

                userType =
                    UserType.fromString(
                        getStringValue(
                            "userType"
                        )
                    ),

                /*
                 * For existing profiles that do not
                 * yet have this field, this becomes "".
                 */
                assignedDoctorId =
                    getStringValue(
                        "assignedDoctorId"
                    ),

                /*
                 * For existing profiles that do not
                 * yet have this field, this becomes "".
                 */
                assignedCaregiverId =
                    getStringValue(
                        "assignedCaregiverId"
                    ),

                address =
                    addressComponent,

                suburb =
                    suburbComponent,

                state =
                    stateComponent,

                postcode =
                    postcodeComponent,

                country =
                    countryComponent,

                gender =
                    getStringValue(
                        "gender"
                    ),

                emergencyName =
                    getStringValue(
                        "emergencyContactName"
                    ),

                emergencyEmail =
                    getStringValue(
                        "emergencyEmail"
                    ),

                emergencyRelation =
                    getStringValue(
                        "relation"
                    ),

                emergencyPhoneNumber =
                    getStringValue(
                        "emergencyContactNumber"
                    ),

                profileImageUrl =
                    getStringValue(
                        "profileImageUrl"
                    )
            )
        }
    }
}