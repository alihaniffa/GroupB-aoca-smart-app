package org.example.dementia_tester_app.data

/**
 * Enum representing the types of users in the system
 */
enum class UserType(val value: String) {
    USER("user"),
    DOCTOR("doctor"),
    CAREGIVER("caregiver"),
    ADMIN("admin");
    
    companion object {
        /**
         * Convert a string to UserType
         * @param value The string value to convert
         * @return The corresponding UserType or USER if not found
         */
        fun fromString(value: String): UserType {
            return entries.find { it.value == value } ?: USER
        }
    }
}

/**
 * Data class representing a user profile.
 * This class maps to user records stored under 'UserProfile' in firebase realtime DB.
 */
data class UserProfile(
    // User details
    val userId: String = "",
    val name: String = "",
    val dateOfBirth: String = "", // Format: DD/MM/YYYY
    val email: String = "",
    val phoneNumber: String = "",
    val userType: UserType = UserType.USER,
    // Address fields
    val address: String = "",
    val suburb: String = "",
    val state: String = "",
    val postcode: String = "",
    val country: String = "",
    val gender: String = "", // "Male" or "Female"
    // Emergency contact details
    val emergencyName: String = "",
    val emergencyEmail: String = "",
    val emergencyRelation: String = "",
    val emergencyPhoneNumber: String = "",
    val profileImageUrl: String = ""
) {
    /**
     * Convert the UserProfile to a map for firebase realtime DB.
     */
    fun toMap(): Map<String, Any> {
        // Convert date from DD/MM/YYYY to DD-MMM-YYYY format
        val dateStr = dateOfBirth
        val formattedDate = if (dateStr.isNotEmpty()) {
            try {
                // Parse DD/MM/YYYY format
                val dateParts = dateStr.split("/")
                if (dateParts.size == 3) {
                    val day = dateParts[0]
                    // Convert month number to name
                    val monthNum = dateParts[1].toIntOrNull() ?: 1
                    val month = monthNumberToName(monthNum)
                    val year = dateParts[2]
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
        // create combined address for backward compatibility
        val fullAddress = listOf(
            address,
            suburb,
            state,
            postcode,
            country
        )
            .filter { it.isNotEmpty() }
            .joinToString(", ")

        return mapOf(
            "userId" to userId,
            "fullName" to name,
            "dateOfBirth" to formattedDate,
            "email" to email,
            "contactNumber" to phoneNumber,
            "userType" to userType.value,

            // keeping combined address for existing code/database records
            "address" to fullAddress,

            // store address components separately
            "streetAddress" to address,
            "suburb" to suburb,
            "state" to state,
            "postcode" to postcode,
            "country" to country,

            "gender" to gender,
            "emergencyContactName" to emergencyName,
            "emergencyEmail" to emergencyEmail,
            "relation" to emergencyRelation,
            "emergencyContactNumber" to emergencyPhoneNumber,
            "profileImageUrl" to profileImageUrl

            )
    }

// previous code
/*
        val fullAddress = listOf(address, suburb, state, postcode, country)
            .filter { it.isNotEmpty() }
            .joinToString(", ")
            
        return mapOf(
            "userId" to userId,
            "fullName" to name,
            "dateOfBirth" to formattedDate,
            "email" to email,
            "contactNumber" to phoneNumber,
            "userType" to userType.value,
            "address" to fullAddress,
            "gender" to gender,
            "emergencyContactName" to emergencyName,
            "emergencyEmail" to emergencyEmail,
            "relation" to emergencyRelation,
            "emergencyContactNumber" to emergencyPhoneNumber,
            "profileImageUrl" to profileImageUrl
 */

    companion object {
        /**
         * Convert month number to three-letter month name
         * 
         * @param monthNumber The month number (1-12)
         * @return Three-letter month abbreviation (e.g., "JAN", "FEB")
         */
        fun monthNumberToName(monthNumber: Int): String {
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
                else -> "JAN" // Default to January for invalid input
            }
        }
        
        /**
         * Convert three-letter month name to month number as string
         * 
         * @param monthName Three-letter month abbreviation (e.g., "JAN", "FEB")
         * @return Month number as string with leading zero if needed (e.g., "01", "12")
         */
        fun monthNameToNumber(monthName: String): String {
            return when (monthName.uppercase()) {
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
                else -> "01" // Default to January for invalid input
            }
        }

        /**
         * Create a UserProfile from a Firestore Realtime DB data.
         *
         * @param map The map containing the user profile data
         * @param userId The ID of the user
         * @return A UserProfile object populated with data from the map
         */
        fun fromMap(map: Map<*, *>, userId: String): UserProfile {
            fun getStringValue(key: String): String {
                return (map[key] as? String) ?: ""
            }

            val dobStr = getStringValue("dateOfBirth")

            // Convert date from DD-MMM-YYYY to DD/MM/YYYY format for storage in UserProfile
            val formattedDate = if (dobStr.isNotEmpty()) {
                try {
                    // Check if the date is already in DD/MM/YYYY format
                    if (dobStr.contains("/")) {
                        dobStr
                    } else {
                        // Parse DD-MMM-YYYY format
                        val dateParts = dobStr.split("-")
                        if (dateParts.size == 3) {
                            val day = dateParts[0]
                            val monthStr = dateParts[1].uppercase()
                            val month = monthNameToNumber(monthStr)
                            val year = dateParts[2]
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
            // read the new separate address fields first
            val streetAddress = getStringValue("streetAddress")
            val storedSuburb = getStringValue("suburb")
            val storedState = getStringValue("state")
            val storedPostcode = getStringValue("postcode")
            val storedCountry = getStringValue("country")

            // read the old combined address for backward compatibility
            val fullAddress = getStringValue("address")
            val addressParts = fullAddress.split(", ")

            // use separate fields when available.
            // otherwise, fall back to the old combined address.
            val addressComponent =
                streetAddress.ifBlank {
                    addressParts.getOrNull(0) ?: ""
                }

            val suburbComponent =
                storedSuburb.ifBlank {
                    addressParts.getOrNull(1) ?: ""
                }

            val stateComponent =
                storedState.ifBlank {
                    addressParts.getOrNull(2) ?: ""
                }

            val postcodeComponent =
                storedPostcode.ifBlank {
                    addressParts.getOrNull(3) ?: ""
                }

            val countryComponent =
                storedCountry.ifBlank {
                    addressParts.getOrNull(4) ?: ""
                }

            return UserProfile(
                userId = userId,
                name = getStringValue("fullName"),
                dateOfBirth = formattedDate,
                email = getStringValue("email"),
                phoneNumber = getStringValue("contactNumber"),
                userType = UserType.fromString(
                    getStringValue("userType")
                ),
                address = addressComponent,
                suburb = suburbComponent,
                state = stateComponent,
                postcode = postcodeComponent,
                country = countryComponent,
                gender = getStringValue("gender"),
                emergencyName = getStringValue("emergencyContactName"),
                emergencyEmail = getStringValue("emergencyEmail"),
                emergencyRelation = getStringValue("relation"),
                emergencyPhoneNumber = getStringValue("emergencyContactNumber"),
                profileImageUrl = getStringValue("profileImageUrl")
            )
        }

// previous code
/*
            // Parse the address string from the database
            val fullAddress = getStringValue("address")
            val addressParts = fullAddress.split(", ")
            
            // Extract address components
            val addressComponent = addressParts.getOrNull(0) ?: ""
            val suburbComponent = addressParts.getOrNull(1) ?: ""
            val stateComponent = addressParts.getOrNull(2) ?: ""
            val postcodeComponent = addressParts.getOrNull(3) ?: ""
            val countryComponent = addressParts.getOrNull(4) ?: ""
            
            return UserProfile(
                userId = userId,
                name = getStringValue("fullName"),
                dateOfBirth = formattedDate,
                email = getStringValue("email"),
                phoneNumber = getStringValue("contactNumber"),
                userType = UserType.fromString(getStringValue("userType")),
                address = addressComponent,
                suburb = suburbComponent,
                state = stateComponent,
                postcode = postcodeComponent,
                country = countryComponent,
                gender = getStringValue("gender"),
                emergencyName = getStringValue("emergencyContactName"),
                emergencyEmail = getStringValue("emergencyEmail"),
                emergencyRelation = getStringValue("relation"),
                emergencyPhoneNumber = getStringValue("emergencyContactNumber"),
                profileImageUrl = getStringValue("profileImageUrl")
            )
 */
    }
}