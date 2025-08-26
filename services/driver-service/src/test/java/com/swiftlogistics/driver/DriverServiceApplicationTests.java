// services/driver-service/src/test/java/com/swiftlogistics/driver/DriverServiceApplicationTests.java
package com.swiftlogistics.driver;

import com.swiftlogistics.driver.dto.DriverRegistrationRequest;
import com.swiftlogistics.driver.dto.DriverResponse;
import com.swiftlogistics.driver.dto.LocationUpdateRequest;
import com.swiftlogistics.driver.entity.Driver;
import com.swiftlogistics.driver.enums.DriverStatus;
import com.swiftlogistics.driver.enums.VehicleType;
import com.swiftlogistics.driver.repository.DriverRepository;
import com.swiftlogistics.driver.service.DriverService;
import com.swiftlogistics.driver.service.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DriverServiceApplicationTests {

	@Autowired
	private DriverService driverService;

	@Autowired
	private DriverRepository driverRepository;

	@Autowired
	private JwtTokenService jwtTokenService;

	@Test
	void contextLoads() {
		assertNotNull(driverService);
		assertNotNull(driverRepository);
		assertNotNull(jwtTokenService);
	}

	@Test
	void testDriverRegistration() {
		// Create driver registration request
		DriverRegistrationRequest request = new DriverRegistrationRequest();
		request.setFullName("Test Driver");
		request.setEmail("test.driver@swift.lk");
		request.setPhoneNumber("+94771234567");
		request.setLicenseNumber("B1234567");
		request.setVehicleType(VehicleType.MOTORCYCLE);
		request.setVehicleNumber("TEST-123");
		request.setPassword("password123");

		// Register driver
		DriverResponse response = driverService.registerDriver(request);

		// Verify response
		assertNotNull(response);
		assertNotNull(response.getDriverId());
		assertEquals("Test Driver", response.getFullName());
		assertEquals("test.driver@swift.lk", response.getEmail());
		assertEquals(VehicleType.MOTORCYCLE, response.getVehicleType());
		assertEquals(DriverStatus.OFFLINE, response.getStatus());
		assertFalse(response.isVerified());
	}

	@Test
	void testDriverLocationUpdate() {
		// Create and save test driver
		Driver driver = new Driver("Test Driver", "location.test@swift.lk",
				"+94771234567", "B1234567", VehicleType.CAR);
		driver = driverRepository.save(driver);

		// Update location
		LocationUpdateRequest locationRequest = new LocationUpdateRequest(6.9271, 79.8612);
		DriverResponse response = driverService.updateDriverLocation(driver.getDriverId(), locationRequest);

		// Verify location update
		assertNotNull(response.getCurrentLatitude());
		assertNotNull(response.getCurrentLongitude());
		assertEquals(6.9271, response.getCurrentLatitude(), 0.0001);
		assertEquals(79.8612, response.getCurrentLongitude(), 0.0001);
		assertNotNull(response.getLastLocationUpdate());
	}

	@Test
	void testDriverStatusUpdate() {
		// Create and save test driver
		Driver driver = new Driver("Test Driver", "status.test@swift.lk",
				"+94771234567", "B1234567", VehicleType.VAN);
		driver = driverRepository.save(driver);

		// Update status
		DriverResponse response = driverService.updateDriverStatus(driver.getDriverId(), DriverStatus.AVAILABLE);

		// Verify status update
		assertEquals(DriverStatus.AVAILABLE, response.getStatus());
	}

	@Test
	void testGetAvailableDrivers() {
		// Create test drivers with different statuses
		Driver availableDriver = new Driver("Available Driver", "available@swift.lk",
				"+94771234567", "B1111111", VehicleType.MOTORCYCLE);
		availableDriver.updateStatus(DriverStatus.AVAILABLE);
		driverRepository.save(availableDriver);

		Driver busyDriver = new Driver("Busy Driver", "busy@swift.lk",
				"+94772345678", "B2222222", VehicleType.MOTORCYCLE);
		busyDriver.updateStatus(DriverStatus.BUSY);
		driverRepository.save(busyDriver);

		// Get available drivers
		List<DriverResponse> availableDrivers = driverService.getAvailableDrivers(VehicleType.MOTORCYCLE);

		// Should only return available driver
		assertTrue(availableDrivers.size() >= 1);
		assertTrue(availableDrivers.stream().allMatch(d -> d.getStatus() == DriverStatus.AVAILABLE));
	}

	@Test
	void testNearbyDrivers() {
		// Create driver with location
		Driver driver = new Driver("Nearby Driver", "nearby@swift.lk",
				"+94771234567", "B3333333", VehicleType.CAR);
		driver.updateStatus(DriverStatus.AVAILABLE);
		driver.updateLocation(6.9271, 79.8612); // Colombo location
		driverRepository.save(driver);

		// Search for nearby drivers
		List<DriverResponse> nearbyDrivers = driverService.getNearbyDrivers(6.9271, 79.8612, 5.0);

		// Should find the driver within radius
		assertTrue(nearbyDrivers.size() >= 1);
		assertTrue(nearbyDrivers.stream().anyMatch(d -> d.getDriverId().equals(driver.getDriverId())));
	}

	@Test
	void testJwtTokenGeneration() {
		String driverId = "TEST-DRIVER-001";

		// Generate token
		String token = jwtTokenService.generateToken(driverId);
		assertNotNull(token);
		assertFalse(token.isEmpty());

		// Validate token
		assertTrue(jwtTokenService.validateToken(token));

		// Extract driver ID from token
		String extractedDriverId = jwtTokenService.getDriverIdFromToken(token);
		assertEquals(driverId, extractedDriverId);

		// Check if token is not expired
		assertFalse(jwtTokenService.isTokenExpired(token));
	}

	@Test
	void testDriverVerification() {
		// Create unverified driver
		Driver driver = new Driver("Unverified Driver", "unverified@swift.lk",
				"+94771234567", "B4444444", VehicleType.SCOOTER);
		driver = driverRepository.save(driver);
		assertFalse(driver.isVerified());

		// Verify driver
		driverService.verifyDriver(driver.getDriverId());

		// Check verification status
		Driver verifiedDriver = driverRepository.findByDriverId(driver.getDriverId()).orElse(null);
		assertNotNull(verifiedDriver);
		assertTrue(verifiedDriver.isVerified());
	}

	@Test
	void testDriverSuspension() {
		// Create active driver
		Driver driver = new Driver("Active Driver", "active@swift.lk",
				"+94771234567", "B5555555", VehicleType.TRUCK);
		driver.setActive(true);
		driver.updateStatus(DriverStatus.AVAILABLE);
		driver = driverRepository.save(driver);

		// Suspend driver
		String suspensionReason = "Test suspension";
		driverService.suspendDriver(driver.getDriverId(), suspensionReason);

		// Check suspension status
		Driver suspendedDriver = driverRepository.findByDriverId(driver.getDriverId()).orElse(null);
		assertNotNull(suspendedDriver);
		assertEquals(DriverStatus.SUSPENDED, suspendedDriver.getStatus());
		assertFalse(suspendedDriver.isActive());
	}

	@Test
	void testDuplicateDriverRegistration() {
		// Create first driver
		DriverRegistrationRequest request1 = new DriverRegistrationRequest();
		request1.setFullName("First Driver");
		request1.setEmail("duplicate@swift.lk");
		request1.setPhoneNumber("+94771234567");
		request1.setLicenseNumber("B1111111");
		request1.setVehicleType(VehicleType.MOTORCYCLE);
		request1.setPassword("password123");

		driverService.registerDriver(request1);

		// Try to create driver with same email
		DriverRegistrationRequest request2 = new DriverRegistrationRequest();
		request2.setFullName("Second Driver");
		request2.setEmail("duplicate@swift.lk"); // Same email
		request2.setPhoneNumber("+94772345678");
		request2.setLicenseNumber("B2222222");
		request2.setVehicleType(VehicleType.CAR);
		request2.setPassword("password456");

		// Should throw exception
		assertThrows(RuntimeException.class, () -> {
			driverService.registerDriver(request2);
		});
	}

	@Test
	void testTopPerformingDrivers() {
		// Create drivers with different ratings
		Driver topDriver = new Driver("Top Driver", "top@swift.lk",
				"+94771111111", "B1111111", VehicleType.MOTORCYCLE);
		topDriver.setRating(4.8);
		topDriver.setCompletedDeliveries(100);
		topDriver.updateStatus(DriverStatus.AVAILABLE);
		driverRepository.save(topDriver);

		Driver averageDriver = new Driver("Average Driver", "average@swift.lk",
				"+94772222222", "B2222222", VehicleType.CAR);
		averageDriver.setRating(4.2);
		averageDriver.setCompletedDeliveries(50);
		averageDriver.updateStatus(DriverStatus.AVAILABLE);
		driverRepository.save(averageDriver);

		// Get top performing drivers
		List<DriverResponse> topDrivers = driverService.getTopPerformingDrivers(5);

		// Should be ordered by rating and completion count
		assertFalse(topDrivers.isEmpty());
		// Top driver should be first (highest rating)
		assertEquals("Top Driver", topDrivers.get(0).getFullName());
	}
}