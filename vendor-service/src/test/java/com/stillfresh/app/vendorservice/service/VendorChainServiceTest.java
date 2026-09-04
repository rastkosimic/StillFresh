package com.stillfresh.app.vendorservice.service;

import com.stillfresh.app.sharedentities.enums.OnboardingStatus;
import com.stillfresh.app.sharedentities.enums.PayoutModel;
import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.dto.VendorStatsResponse;
import com.stillfresh.app.sharedentities.vendor.events.OfferRelatedVendorDetailsEvent;
import com.stillfresh.app.vendorservice.client.AuthorizationServiceClient;
import com.stillfresh.app.vendorservice.client.OrderClient;
import com.stillfresh.app.vendorservice.dto.ChainLocationStatsResponse;
import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.publisher.VendorEventPublisher;
import com.stillfresh.app.vendorservice.repository.VendorRepository;
import com.stillfresh.app.vendorservice.security.CustomVendorDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Covers the chain, worker and bank-detail rules of {@link VendorService}: the paths where a
 * mistake either leaks access across chain members or sends money to the wrong account.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VendorChainServiceTest {

    private static final String CHAIN_ID = "chain-1";

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private VendorEventPublisher eventPublisher;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthorizationServiceClient authorizationServiceClient;

    @Mock
    private OrderClient orderClient;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private VendorService vendorService;

    private Vendor headquarters;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(vendorService, "maxChainLocations", 200);
        ReflectionTestUtils.setField(vendorService, "maxWorkersPerLocation", 100);
        ReflectionTestUtils.setField(vendorService, "loginUrl", "http://localhost/auth/login");

        headquarters = location(1L, "hq@chain.test", "HQ");
        headquarters.setIsHeadquarters(true);

        when(authorizationServiceClient.updateUserStatus(anyLong(), any()))
            .thenReturn(successResponse());
        when(authorizationServiceClient.deleteUser(anyLong())).thenReturn(successResponse());
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- helpers ----------

    private Map<String, Object> successResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return response;
    }

    private Vendor location(Long id, String email, String locationName) {
        Vendor vendor = new Vendor();
        vendor.setId(id);
        vendor.setEmail(email);
        vendor.setUsername(locationName.toLowerCase());
        vendor.setLocationName(locationName);
        vendor.setRole(Role.VENDOR_ADMIN);
        vendor.setStatus(Status.ACTIVE);
        vendor.setChainId(CHAIN_ID);
        vendor.setChainName("Test Chain");
        vendor.setIsChainLocation(true);
        vendor.setIsHeadquarters(false);
        vendor.setIsUniqueVendor(false);
        vendor.setOnboardingStatus(OnboardingStatus.COMPLETED);
        vendor.setPayoutModel(PayoutModel.MOR);
        vendor.setBankIban("DE89370400440532013000");
        vendor.setBankAccountHolderName("Test Chain d.o.o.");
        return vendor;
    }

    private Vendor worker(Long id, Long assignedLocationId) {
        Vendor vendor = location(id, "worker" + id + "@chain.test", "Worker " + id);
        vendor.setRole(Role.VENDOR);
        vendor.setAssignedLocationId(assignedLocationId);
        vendor.setIsHeadquarters(false);
        return vendor;
    }

    private void authenticateAs(Vendor vendor) {
        CustomVendorDetails details = new CustomVendorDetails(vendor);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private void authenticateAsSuperAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "super-admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))));
    }

    private VendorStatsResponse vendorStats(long units, long earningsCents, long feeCents, long grossCents) {
        return new VendorStatsResponse(units, earningsCents, feeCents, grossCents, List.of(), null, null);
    }

    private void stubVendors(Vendor... vendors) {
        for (Vendor vendor : vendors) {
            when(vendorRepository.findById(vendor.getId())).thenReturn(Optional.of(vendor));
        }
        when(vendorRepository.findByChainId(CHAIN_ID)).thenReturn(Arrays.asList(vendors));
    }

    // ---------- chain locations ----------

    @Test
    void getChainLocationsExcludesWorkers() {
        Vendor branch = location(2L, "branch@chain.test", "Branch");
        Vendor branchWorker = worker(3L, branch.getId());
        stubVendors(headquarters, branch, branchWorker);
        authenticateAs(headquarters);

        List<Vendor> locations = vendorService.getChainLocations();

        assertEquals(2, locations.size());
        assertTrue(locations.stream().noneMatch(v -> v.getId().equals(branchWorker.getId())),
            "workers live in the same table but are not sellable locations");
    }

    @Test
    void getChainLocationsRejectsVendorWithoutChain() {
        Vendor standalone = location(9L, "solo@test.test", "Solo");
        standalone.setIsChainLocation(false);
        standalone.setChainId(null);
        authenticateAs(standalone);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> vendorService.getChainLocations());
        assertTrue(ex.getMessage().contains("not part of a chain"));
    }

    @Test
    void branchAdminCannotRemoveSiblingLocation() {
        Vendor branchA = location(2L, "a@chain.test", "A");
        Vendor branchB = location(3L, "b@chain.test", "B");
        stubVendors(headquarters, branchA, branchB);
        authenticateAs(branchA);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> vendorService.removeChainLocation(branchB.getId()));

        assertTrue(ex.getMessage().contains("Only headquarters"));
        verify(vendorRepository, never()).save(branchB);
    }

    @Test
    void headquartersCannotBeRemoved() {
        stubVendors(headquarters);
        authenticateAs(headquarters);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> vendorService.removeChainLocation(headquarters.getId()));
        assertTrue(ex.getMessage().contains("Cannot remove headquarters"));
    }

    @Test
    void removingLocationDeactivatesItsWorkersAndSyncsAuthorizationService() {
        Vendor branch = location(2L, "branch@chain.test", "Branch");
        Vendor branchWorker = worker(3L, branch.getId());
        stubVendors(headquarters, branch, branchWorker);
        when(vendorRepository.findByAssignedLocationId(branch.getId()))
            .thenReturn(Collections.singletonList(branchWorker));
        authenticateAs(headquarters);

        vendorService.removeChainLocation(branch.getId());

        assertEquals(Status.INACTIVE, branch.getStatus());
        assertEquals(Status.INACTIVE, branchWorker.getStatus(),
            "a worker of a removed location must not keep selling");
        verify(authorizationServiceClient).updateUserStatus(branch.getId(), Status.INACTIVE);
        verify(authorizationServiceClient).updateUserStatus(branchWorker.getId(), Status.INACTIVE);
    }

    @Test
    void locationFromDifferentChainIsRejected() {
        Vendor foreign = location(2L, "foreign@other.test", "Foreign");
        foreign.setChainId("chain-2");
        stubVendors(headquarters, foreign);
        authenticateAs(headquarters);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> vendorService.removeChainLocation(foreign.getId()));
        assertTrue(ex.getMessage().contains("different chain"));
    }

    @Test
    void updatingLocationRepublishesOfferDetails() {
        Vendor branch = location(2L, "branch@chain.test", "Old name");
        stubVendors(headquarters, branch);
        authenticateAs(headquarters);

        com.stillfresh.app.vendorservice.dto.LocationRequest request =
            new com.stillfresh.app.vendorservice.dto.LocationRequest();
        request.setLocationName("New name");
        request.setAddress("New address 1");
        request.setZipCode("11000");
        request.setLatitude(44.8);
        request.setLongitude(20.4);

        vendorService.updateChainLocation(branch.getId(), request);

        ArgumentCaptor<OfferRelatedVendorDetailsEvent> captor =
            ArgumentCaptor.forClass(OfferRelatedVendorDetailsEvent.class);
        verify(eventPublisher).publishOfferRelatedVendorDetailsEvent(captor.capture());
        assertEquals("New name", captor.getValue().getLocationName(),
            "offers snapshot the location name, so listings would keep the stale one");
    }

    // ---------- upgrade to chain ----------

    @Test
    void upgradeToChainRequiresCompletedOnboarding() {
        Vendor standalone = location(5L, "solo@test.test", "Solo");
        standalone.setIsChainLocation(false);
        standalone.setChainId(null);
        standalone.setIsUniqueVendor(true);
        standalone.setOnboardingStatus(OnboardingStatus.BANKING_SETUP);
        authenticateAs(standalone);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> vendorService.upgradeToChain("Fresh Bakery"));
        assertTrue(ex.getMessage().contains("Finish onboarding"));
    }

    @Test
    void upgradeToChainRejectsChainNameOwnedByAnotherChain() {
        Vendor standalone = location(5L, "solo@test.test", "Solo");
        standalone.setIsChainLocation(false);
        standalone.setChainId(null);
        standalone.setIsUniqueVendor(true);
        authenticateAs(standalone);

        Vendor otherChainMember = location(6L, "other@other.test", "Other");
        otherChainMember.setChainId("chain-2");
        when(vendorRepository.findByChainName("Fresh Bakery"))
            .thenReturn(Collections.singletonList(otherChainMember));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> vendorService.upgradeToChain("Fresh Bakery"));
        assertTrue(ex.getMessage().contains("already in use"));
    }

    @Test
    void upgradeToChainMakesTheVendorItsOwnHeadquarters() {
        Vendor standalone = location(5L, "solo@test.test", "Solo");
        standalone.setIsChainLocation(false);
        standalone.setChainId(null);
        standalone.setIsUniqueVendor(true);
        authenticateAs(standalone);
        when(vendorRepository.findByChainName(anyString())).thenReturn(Collections.emptyList());

        vendorService.upgradeToChain("Fresh Bakery");

        assertNotNull(standalone.getChainId());
        assertEquals("Fresh Bakery", standalone.getChainName());
        assertTrue(standalone.getIsHeadquarters());
        assertTrue(standalone.getIsChainLocation());
        assertFalse(standalone.getIsUniqueVendor());
        assertFalse(standalone.getUsesSharedPaymentAccount(),
            "a fresh chain starts on the individual banking model");
    }

    @Test
    void upgradeToChainRejectsAnExistingChainMember() {
        authenticateAs(headquarters);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> vendorService.upgradeToChain("Another Name"));
        assertTrue(ex.getMessage().contains("already part of a chain"));
    }

    // ---------- workers ----------

    @Test
    void branchAdminCannotReassignWorkerToSiblingLocation() {
        Vendor branchA = location(2L, "a@chain.test", "A");
        Vendor branchB = location(3L, "b@chain.test", "B");
        Vendor branchAWorker = worker(4L, branchA.getId());
        stubVendors(headquarters, branchA, branchB, branchAWorker);
        authenticateAs(branchA);

        com.stillfresh.app.vendorservice.dto.WorkerUpdateRequest request =
            new com.stillfresh.app.vendorservice.dto.WorkerUpdateRequest();
        request.setAssignedLocationId(branchB.getId());

        assertThrows(RuntimeException.class, () -> vendorService.updateWorker(branchAWorker.getId(), request));
        assertEquals(branchA.getId(), branchAWorker.getAssignedLocationId());
    }

    @Test
    void headquartersReassignsWorkerAndCopiesLocationContext() {
        Vendor branchA = location(2L, "a@chain.test", "A");
        Vendor branchB = location(3L, "b@chain.test", "B");
        branchB.setAddress("Address B");
        branchB.setZipCode("21000");
        Vendor branchAWorker = worker(4L, branchA.getId());
        stubVendors(headquarters, branchA, branchB, branchAWorker);
        authenticateAs(headquarters);

        com.stillfresh.app.vendorservice.dto.WorkerUpdateRequest request =
            new com.stillfresh.app.vendorservice.dto.WorkerUpdateRequest();
        request.setAssignedLocationId(branchB.getId());

        vendorService.updateWorker(branchAWorker.getId(), request);

        assertEquals(branchB.getId(), branchAWorker.getAssignedLocationId());
        assertEquals("B", branchAWorker.getLocationName());
        assertEquals("Address B", branchAWorker.getAddress());
    }

    @Test
    void workerUpdateRejectsTakenUsername() {
        Vendor branch = location(2L, "branch@chain.test", "Branch");
        Vendor branchWorker = worker(3L, branch.getId());
        stubVendors(headquarters, branch, branchWorker);
        when(vendorRepository.existsByUsername("taken")).thenReturn(true);
        authenticateAs(headquarters);

        com.stillfresh.app.vendorservice.dto.WorkerUpdateRequest request =
            new com.stillfresh.app.vendorservice.dto.WorkerUpdateRequest();
        request.setUsername("taken");

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> vendorService.updateWorker(branchWorker.getId(), request));
        assertTrue(ex.getMessage().contains("Username already taken"));
    }

    @Test
    void deletingWorkerAlsoRemovesTheLoginAccount() {
        Vendor branch = location(2L, "branch@chain.test", "Branch");
        Vendor branchWorker = worker(3L, branch.getId());
        stubVendors(headquarters, branch, branchWorker);
        authenticateAs(headquarters);

        vendorService.deleteWorker(branchWorker.getId());

        verify(vendorRepository).deleteById(branchWorker.getId());
        verify(authorizationServiceClient).deleteUser(branchWorker.getId());
    }

    @Test
    void workerEndpointsRejectALocationRow() {
        Vendor branch = location(2L, "branch@chain.test", "Branch");
        stubVendors(headquarters, branch);
        authenticateAs(headquarters);

        assertThrows(RuntimeException.class, () -> vendorService.deactivateWorker(branch.getId()));
        assertEquals(Status.ACTIVE, branch.getStatus());
    }

    // ---------- payout routing ----------

    @Test
    void sharedBankingModelResolvesPayoutOwnerForMoR() {
        Vendor branch = location(2L, "branch@chain.test", "Branch");
        branch.setUsesSharedPaymentAccount(true);
        branch.setSharedPaymentAccountVendorId(headquarters.getId());
        branch.setBankIban(null);
        branch.setBankAccountNumber(null);
        stubVendors(headquarters, branch);

        Vendor owner = vendorService.resolvePayoutAccountOwner(branch);

        assertEquals(headquarters.getId(), owner.getId());
        assertTrue(vendorService.hasPayoutDestination(branch),
            "a shared-account location is paid through headquarters' bank details");
    }

    @Test
    void individualBankingModelWithoutBankDetailsHasNoPayoutDestination() {
        Vendor branch = location(2L, "branch@chain.test", "Branch");
        branch.setUsesSharedPaymentAccount(false);
        branch.setBankIban(null);
        branch.setBankAccountNumber(null);

        assertFalse(vendorService.hasPayoutDestination(branch));
    }

    @Test
    void connectLocationWithoutStripeAccountHasNoPayoutDestination() {
        Vendor branch = location(2L, "branch@chain.test", "Branch");
        branch.setPayoutModel(PayoutModel.CONNECT);
        branch.setStripeAccountId(null);

        assertFalse(vendorService.hasPayoutDestination(branch));
    }

    @Test
    void locationWithoutPayoutAccountCannotPublishOffers() {
        Vendor branch = location(2L, "branch@chain.test", "Branch");
        branch.setBankIban(null);
        branch.setBankAccountNumber(null);
        authenticateAs(branch);

        OfferDto offer = new OfferDto();
        offer.setName("Surprise bag");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> vendorService.createOffer(offer));
        assertTrue(ex.getMessage().contains("no payout account"));
        verify(eventPublisher, never()).publishOfferCreationEvent(any());
    }

    @Test
    void inactiveLocationCannotPublishOffers() {
        Vendor branch = location(2L, "branch@chain.test", "Branch");
        branch.setStatus(Status.INACTIVE);
        authenticateAs(branch);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> vendorService.createOffer(new OfferDto()));
        assertTrue(ex.getMessage().contains("not active"));
    }

    // ---------- bank details ----------

    @Test
    void bankDetailsRejectInvalidIbanChecksum() {
        authenticateAs(headquarters);

        Map<String, String> details = new HashMap<>();
        details.put("iban", "DE89370400440532013001");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> vendorService.submitBankDetails(details));
        assertTrue(ex.getMessage().contains("checksum"));
    }

    @Test
    void bankDetailsRejectMalformedIban() {
        authenticateAs(headquarters);

        Map<String, String> details = new HashMap<>();
        details.put("iban", "12-345");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> vendorService.submitBankDetails(details));
        assertTrue(ex.getMessage().contains("format is invalid"));
    }

    @Test
    void bankDetailsNormalizeSpacingAndCase() {
        authenticateAs(headquarters);

        Map<String, String> details = new HashMap<>();
        details.put("iban", "de89 3704 0044 0532 0130 00");
        details.put("swiftCode", "deutdeff");

        vendorService.submitBankDetails(details);

        assertEquals("DE89370400440532013000", headquarters.getBankIban());
        assertEquals("DEUTDEFF", headquarters.getBankSwiftCode());
    }

    @Test
    void bankDetailsStripHyphensFromIbanAndSwift() {
        authenticateAs(headquarters);

        Map<String, String> details = new HashMap<>();
        // Mobile apps often insert hyphens for readability; the stored value must be continuous.
        details.put("iban", "DE89-3704-0044-0532-0130-00");
        details.put("swiftCode", "DEUT-DEFF");
        details.put("accountNumber", "111-2222222-33");

        vendorService.submitBankDetails(details);

        assertEquals("DE89370400440532013000", headquarters.getBankIban());
        assertEquals("DEUTDEFF", headquarters.getBankSwiftCode());
        assertEquals("111-2222222-33", headquarters.getBankAccountNumber(),
            "Serbian domestic account numbers keep their hyphens");
    }

    @Test
    void bankDetailsRejectMalformedSwift() {
        authenticateAs(headquarters);

        Map<String, String> details = new HashMap<>();
        details.put("swiftCode", "DEUT1");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> vendorService.submitBankDetails(details));
        assertTrue(ex.getMessage().contains("SWIFT"));
    }

    @Test
    void partialBankDetailsUpdateKeepsUntouchedFields() {
        headquarters.setBankName("Old Bank");
        authenticateAs(headquarters);

        Map<String, String> details = new HashMap<>();
        details.put("swiftCode", "DEUTDEFF500");

        vendorService.submitBankDetails(details);

        assertEquals("Old Bank", headquarters.getBankName());
        assertEquals("Test Chain d.o.o.", headquarters.getBankAccountHolderName());
        assertEquals("DE89370400440532013000", headquarters.getBankIban(),
            "a partial update must not wipe the payout destination");
    }

    @Test
    void bankDetailsRequireAPayoutDestination() {
        headquarters.setBankIban(null);
        headquarters.setBankAccountNumber(null);
        authenticateAs(headquarters);

        Map<String, String> details = new HashMap<>();
        details.put("holderName", "Test Chain d.o.o.");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> vendorService.submitBankDetails(details));
        assertTrue(ex.getMessage().contains("IBAN or an account number"));
    }

    @Test
    void changingThePayoutDestinationWarnsTheVendorByEmail() throws Exception {
        authenticateAs(headquarters);

        Map<String, String> details = new HashMap<>();
        details.put("iban", "GB33BUKB20201555555555");

        vendorService.submitBankDetails(details);

        assertEquals("GB33BUKB20201555555555", headquarters.getBankIban());
        verify(emailService).sendEmail(eq(headquarters.getEmail()), anyString(), anyString());
    }

    @Test
    void firstTimeBankDetailsDoNotTriggerAChangeWarning() throws Exception {
        headquarters.setBankIban(null);
        headquarters.setBankAccountNumber(null);
        authenticateAs(headquarters);

        Map<String, String> details = new HashMap<>();
        details.put("iban", "GB33BUKB20201555555555");
        details.put("holderName", "Test Chain d.o.o.");

        vendorService.submitBankDetails(details);

        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void bankDetailsAreRejectedForConnectVendors() {
        headquarters.setPayoutModel(PayoutModel.CONNECT);
        authenticateAs(headquarters);

        Map<String, String> details = new HashMap<>();
        details.put("iban", "DE89370400440532013000");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> vendorService.submitBankDetails(details));
        assertTrue(ex.getMessage().contains("not using MoR"));
    }

    // ---------- chain stats ----------

    @Test
    void hqAdminGetsAllLocationStatsWithCorrectTotals() {
        Vendor branch = location(2L, "branch@chain.test", "Branch");
        stubVendors(headquarters, branch);
        authenticateAs(headquarters);

        when(orderClient.getVendorStats(1L, null, null, null))
            .thenReturn(vendorStats(10, 1000, 200, 1200));
        when(orderClient.getVendorStats(2L, null, null, null))
            .thenReturn(vendorStats(5, 500, 100, 600));

        ChainLocationStatsResponse response = vendorService.getChainLocationStats(null, null, null);

        assertEquals(CHAIN_ID, response.getChainId());
        assertEquals("Test Chain", response.getChainName());
        assertEquals(2, response.getLocations().size());
        assertEquals(15, response.getChainTotals().getTotalUnitsSold());
        assertEquals(1500, response.getChainTotals().getTotalVendorEarningsCents());
        assertEquals(300, response.getChainTotals().getTotalPlatformFeeCents());
        assertEquals(1800, response.getChainTotals().getTotalGrossRevenueCents());
        assertTrue(response.getLocations().stream().allMatch(e -> e.getError() == null));
    }

    @Test
    void branchAdminCannotViewChainStats() {
        Vendor branch = location(2L, "branch@chain.test", "Branch");
        stubVendors(headquarters, branch);
        authenticateAs(branch);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> vendorService.getChainLocationStats(null, null, null));
        assertTrue(ex.getMessage().contains("Only headquarters"));
        verify(orderClient, never()).getVendorStats(anyLong(), any(), any(), any());
    }

    @Test
    void nonChainVendorAdminCannotViewChainStats() {
        Vendor standalone = location(9L, "solo@test.test", "Solo");
        standalone.setIsChainLocation(false);
        standalone.setChainId(null);
        authenticateAs(standalone);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> vendorService.getChainLocationStats(null, null, null));
        assertTrue(ex.getMessage().contains("not part of a chain"));
    }

    @Test
    void superAdminRequiresChainId() {
        authenticateAsSuperAdmin();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> vendorService.getChainLocationStats(null, null, null));
        assertTrue(ex.getMessage().contains("chainId is required"));
    }

    @Test
    void superAdminWithChainIdGetsAllLocations() {
        Vendor branch = location(2L, "branch@chain.test", "Branch");
        stubVendors(headquarters, branch);
        authenticateAsSuperAdmin();

        when(orderClient.getVendorStats(anyLong(), any(), any(), any()))
            .thenReturn(vendorStats(1, 100, 10, 110));

        ChainLocationStatsResponse response = vendorService.getChainLocationStats(null, null, CHAIN_ID);

        assertEquals(CHAIN_ID, response.getChainId());
        assertEquals(2, response.getLocations().size());
        verify(orderClient).getVendorStats(1L, null, null, null);
        verify(orderClient).getVendorStats(2L, null, null, null);
    }

    @Test
    void partialFeignFailureReturnsErrorOnLocation() {
        Vendor branch = location(2L, "branch@chain.test", "Branch");
        stubVendors(headquarters, branch);
        authenticateAs(headquarters);

        when(orderClient.getVendorStats(1L, null, null, null))
            .thenReturn(vendorStats(10, 1000, 200, 1200));
        when(orderClient.getVendorStats(2L, null, null, null))
            .thenThrow(new RuntimeException("order-service unavailable"));

        ChainLocationStatsResponse response = vendorService.getChainLocationStats(null, null, null);

        assertEquals(2, response.getLocations().size());
        assertNull(response.getLocations().stream()
            .filter(e -> e.getVendorId().equals(1L)).findFirst().orElseThrow().getError());
        assertEquals("order-service unavailable", response.getLocations().stream()
            .filter(e -> e.getVendorId().equals(2L)).findFirst().orElseThrow().getError());
        assertEquals(10, response.getChainTotals().getTotalUnitsSold());
        assertEquals(1200, response.getChainTotals().getTotalGrossRevenueCents());
    }
}
