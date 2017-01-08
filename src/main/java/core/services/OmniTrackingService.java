package core.services;

import com.bugsnag.Bugsnag;
import core.entities.*;
import core.entities.enums.OmniTrackingState;
import core.entities.enums.TransactionState;
import core.outbound.MailedIt;
import core.thirdParty.aftership.classes.AftershipAPIException;
import core.thirdParty.aftership.classes.Checkpoint;
import core.thirdParty.aftership.classes.ConnectionAPI;
import core.thirdParty.aftership.classes.Tracking;
import core.thirdParty.aftership.enums.StatusTag;
import core.util.CheckData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OmniTrackingService {

    private final MailedIt mailedIt;
    private final Bugsnag bugsnag;
    private final EnvVariablesService envVariablesService;
    private final IsoCountryService isoCountryService;
    private final CourierNameService courierNameService;
    private final OmniTransactionDAO omniTransactionDAO;
    private final OmniTrackingDAO omniTrackingDAO;
    private final OmniTransactionDetailDAO omniTransactionDetailDAO;
    private final OmniTrackingDetailDAO omniTrackingDetailDAO;



    @Autowired
    public OmniTrackingService(MailedIt mailedIt,
                               Bugsnag bugsnag,
                               EnvVariablesService envVariablesService,
                               IsoCountryService isoCountryService,
                               CourierNameService courierNameService,
                               OmniTransactionDAO omniTransactionDAO,
                               OmniTrackingDAO omniTrackingDAO,
                               OmniTransactionDetailDAO omniTransactionDetailDAO,
                               OmniTrackingDetailDAO omniTrackingDetailDAO) {
        Assert.notNull(mailedIt, "MailedIt must not be null!");
        Assert.notNull(bugsnag, "Bugsnag must not be null!");
        Assert.notNull(envVariablesService, "EnvVariablesService must not be null!");
        Assert.notNull(isoCountryService, "IsoCountryService must not be null!");
        Assert.notNull(courierNameService, "CourierNameService must not be null!");
        Assert.notNull(omniTransactionDAO, "OmniTransactionDAO must not be null!");
        Assert.notNull(omniTrackingDAO, "OmniTrackingDAO must not be null!");
        Assert.notNull(omniTransactionDetailDAO, "OmniTransactionDetailDAO must not be null!");
        Assert.notNull(omniTrackingDetailDAO, "OmniTrackingDetailDAO must not be null!");
        this.mailedIt = mailedIt;
        this.bugsnag = bugsnag;
        this.envVariablesService = envVariablesService;
        this.isoCountryService = isoCountryService;
        this.courierNameService = courierNameService;
        this.omniTransactionDAO = omniTransactionDAO;
        this.omniTrackingDAO = omniTrackingDAO;
        this.omniTransactionDetailDAO = omniTransactionDetailDAO;
        this.omniTrackingDetailDAO = omniTrackingDetailDAO;
    }

    @Scheduled(fixedDelay = 1800000) // every 30 min
    public void pullNewTrackingNumbersAndCreateOmniTracking() {
        int updateSuccess = 0;
        int updateFail = 0;
        for(OmniTransaction omniTransaction : omniTransactionDAO.findFirst250ByTrackingNumberIsNotNullAndOmniTrackingIdLessThan(1)) {
            try {
                // Aftership only allows 10 requests per second
                // and we run WAY faster than that, so I slowed it down.
                TimeUnit.MILLISECONDS.sleep(500);
                if(postOmniTracking(omniTransaction)) {
                    updateSuccess++;
                } else {
                    updateFail++;
                }
            } catch (Exception e) {
                bugsnag.notify(e);
            }
        }
        if(updateSuccess > 0 || updateFail > 0) {
            mailedIt.generateAndSendEmail(envVariableService.getAdminEmailAddress(),"Scheduler: pullNewTrackingNumbersAndCreateOmniTracking","Scheduler: pullNewTrackingNumbersAndCreateOmniTracking:" +
                            "<br>ENV:"+envVariablesService.getEnvName()+
                            "<br>Method: OmniTrackingService.pullNewTrackingNumbersAndCreateOmniTracking" +
                            "<br>SUCCESS: "+updateSuccess+
                            "<br>FAILED: "+updateFail,
                    false,"SCHEDULER -- pullNewTrackingNumbersAndCreateOmniTracking");
        }
    }

    public boolean postOmniTracking(OmniTransaction omniTransaction) {
//        boolean allGood = false;
        if(isEmptyTracking(omniTransaction)) {
            return false;
        }
        String afterShipId = "";
        String slug = "";
        StatusTag statusTag = StatusTag.Pending;
        OmniTracking omniTracking = new OmniTracking();

//            boolean postSuccess = true;
            ConnectionAPI connection = new ConnectionAPI(envVariablesService.getAftershipApiKey());
            // Post tracking number to Aftership
            try {
                Tracking tracking = new Tracking(omniTransaction.getTrackingNumber());
                omniTracking.setTrackingNumber(omniTransaction.getTrackingNumber());
                String customerName = omniTransaction.getFirst()+" "+omniTransaction.getLast();
                tracking.setCustomerName(customerName);
                omniTracking.setCustomerName(customerName);
                tracking.setOrderID(omniTransaction.getOrderId());
                omniTracking.setOrderID(omniTransaction.getOrderId());
                omniTracking.setShippingNotifications(omniTransaction.isShippingNotifications());
                if(omniTransaction.isShippingNotifications() && CheckData.isValidEmailAddress(omniTransaction.getEmail())) {
                    tracking.addEmails(omniTransaction.getEmail());
                }
                tracking.addCustomFields("accountId",omniTransaction.getAccountId()+"");
                omniTracking.setAccountId(omniTransaction.getAccountId());
                if(isoCountryService.getISO3Country(omniTransaction.getShippingCountry()) != null) {
                    tracking.setDestinationCountryISO3(isoCountryService.getISO3Country(omniTransaction.getShippingCountry()));
                    omniTracking.setDestinationCountryISO3(isoCountryService.getISO3Country(omniTransaction.getShippingCountry()));
                }
                Tracking trackingPosted = connection.postTracking(tracking);
                if(trackingPosted != null) {
                    afterShipId = trackingPosted.getId();
                    if(trackingPosted.getSlug() != null && trackingPosted.getSlug().trim().length() > 2) {
                        slug = trackingPosted.getSlug();
                    }
                    if(trackingPosted.getTag() != null) {
                        statusTag = trackingPosted.getTag();
                    }
                }
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if(errorMessage.contains(":4003") && errorMessage.contains("slug")) {
                    try {
                        String errorSlug = errorMessage.substring(errorMessage.indexOf("\"slug\":\"")+8);
                        errorSlug = errorSlug.substring(0,errorSlug.indexOf("\""));
                        Tracking trackingToGet = new Tracking(omniTransaction.getTrackingNumber());
                        trackingToGet.setSlug(errorSlug);
                        Tracking trackingRetrieved = connection.getTrackingByNumber(trackingToGet);
                        afterShipId = trackingRetrieved.getId();
                        slug = errorSlug;
                        if(trackingRetrieved.getTag() != null) {
                            statusTag = trackingRetrieved.getTag();
                        }
                    } catch (Exception e2) {
//                        postSuccess = false;
                        mailedIt.generateAndSendEmail(envVariableService.getAdminEmailAddress(),"FAILED POST Tracking to Aftership (TYPE 3)","FAILED POST Tracking to Aftership (TYPE 3)" +
                                "<br>OmniTransaction ID: "+omniTransaction.getId()+"<br>Tracking Number: "+omniTransaction.getTrackingNumber() +
                                "<br>ENV:"+envVariablesService.getEnvName()+
                                "<br>Method: OmniTrackingService.postOmniTracking" +
                                "<br>ERROR: "+e2.getMessage(),false,"POST OMNI TRACKING ERROR");
                        bugsnag.notify(e);
                        return false;
                    }
                } else if(errorMessage.contains(":4005") && errorMessage.contains("invalid")) {
                    recordFailedTracking(omniTransaction,"Invalid Tracking Number: "+omniTracking.getTrackingNumber());
                    return false;
                } else {
//                    postSuccess = false;
                    mailedIt.generateAndSendEmail(envVariableService.getAdminEmailAddress(),"FAILED POST Tracking to Aftership (TYPE 1)","FAILED POST Tracking to Aftership (TYPE 1)" +
                            "<br>OmniTransaction ID: "+omniTransaction.getId()+"<br>Tracking Number: "+omniTransaction.getTrackingNumber() +
                            "<br>ENV:"+envVariablesService.getEnvName()+
                            "<br>Method: OmniTrackingService.postOmniTracking" +
                            "<br>ERROR: "+e.getMessage(),false,"POST OMNI TRACKING ERROR");
                    bugsnag.notify(e);
                    return false;
                }
            }
            if(afterShipId == null || afterShipId.trim().isEmpty()) {
//                postSuccess = false;
                mailedIt.generateAndSendEmail(envVariableService.getAdminEmailAddress(),"FAILED POST Tracking to Aftership (TYPE 2)","FAILED POST Tracking to Aftership (TYPE 2)" +
                                "<br>OmniTransaction ID: "+omniTransaction.getId()+"<br>Tracking Number: "+omniTransaction.getTrackingNumber() +
                                "<br>ENV:"+envVariablesService.getEnvName()+
                                "<br>Method: OmniTrackingService.postOmniTracking" +
                                "<br>ERROR: afterShipId == null || afterShipId.trim().isEmpty()",
                        false,"POST OMNI TRACKING ERROR");
                return false;
            }
//            if(postSuccess) {
                omniTracking.setActive(true);
                omniTracking.setCreatedByDateTime(LocalDateTime.now());
                omniTracking.setLastUpdatedDateTime(LocalDateTime.now());
                omniTracking.setAftershipTrackingId(afterShipId);
                if(slug != null && !slug.trim().isEmpty() && courierNameService.isSlug(slug)) {
                    omniTracking.setSlug(slug);
                    omniTransaction.setShippingCarrierCode(slug);
                    omniTransaction.setShippingCarrierName(courierNameService.getCourierName(slug));
                } else {
                    omniTracking.setSlug("INITIAL (OTS-POT)");
                    omniTransaction.setShippingCarrierCode("INITIAL (OTS-POT)");
                    omniTransaction.setShippingCarrierName("Waiting For Response");
                }
                omniTransaction.setTransactionState(TransactionState.SHIPPED_WAITING_FOR_DELIVERED);
                omniTracking.setOmniTrackingState(OmniTrackingState.ACTIVE);
                boolean trackingFinished = false;
                boolean trackingFailed = false;
                if(statusTag != null) {
                    omniTracking.setTag(statusTag);
                    omniTransaction.setShippingStatusTag(statusTag);
                    if(statusTag.equals(StatusTag.Delivered)) {
                        omniTransaction.setTransactionState(TransactionState.COMPLETE);
                        omniTracking.setActive(false);
                        trackingFinished = true;
                    } else if(statusTag.equals(StatusTag.Exception) || statusTag.equals(StatusTag.Expired)) {
                        omniTransaction.setTransactionState(TransactionState.COMPLETE);
                        omniTracking.setActive(false);
                        trackingFailed = true;
                    }
                }
                omniTrackingDAO.save(omniTracking);
                omniTransaction.setOmniTrackingId(omniTracking.getId());
                omniTransactionDAO.save(omniTransaction);

                omniTransactionDetailDAO.save(new OmniTransactionDetail(omniTransaction.getAccountId(),omniTransaction.getId(),"Posted tracking number to carrier for tracking details.",omniTransaction.getTransactionState()));
                if(trackingFinished) {
                    omniTransactionDetailDAO.save(new OmniTransactionDetail(omniTransaction.getAccountId(),omniTransaction.getId(),"Package delivered, tracking complete.",omniTransaction.getTransactionState()));
                } else if (trackingFailed) {
                    omniTransactionDetailDAO.save(new OmniTransactionDetail(omniTransaction.getAccountId(),omniTransaction.getId(),"Tracking failed with carrier error or exception.",omniTransaction.getTransactionState()));
                }
//                allGood = true;
//            }
        return true;
    }

    private void recordFailedTracking(OmniTransaction omniTransaction, String failure) {
        long fakeNumber = 999999999999999999L;
        omniTransaction.setOmniTrackingId(fakeNumber);
        omniTransaction.setTransactionState(TransactionState.COMPLETE);
        OmniTransactionDetail omniTransactionDetail = new OmniTransactionDetail(omniTransaction.getAccountId(),omniTransaction.getId(),failure,TransactionState.COMPLETE);
        omniTransactionDAO.save(omniTransaction);
        omniTransactionDetailDAO.save(omniTransactionDetail);
    }

    private boolean isEmptyTracking(OmniTransaction omniTransaction) {
        if(omniTransaction.getTrackingNumber() != null && omniTransaction.getTrackingNumber().trim().length() < 4) {
            omniTransaction.setTrackingNumber(null);
            omniTransactionDAO.save(omniTransaction);
            return true;
        }
        return false;
    }

    @Scheduled(cron = "0 3 1 * * ?") // 3:01am
    private void updateNullOmniTrackingData() {
        int updateSuccess = 0;
        int updateFail = 0;
        ConnectionAPI connection = new ConnectionAPI(envVariablesService.getAftershipApiKey());
        for (OmniTracking omniTracking : omniTrackingDAO.findBySlugIsNullOrTagIsNull()) {
            if(updateMissingShippingData(omniTracking,connection)) {
                updateSuccess++;
            } else {
                updateFail++;
            }
        }
        if(updateSuccess > 0 || updateFail > 0) {
            mailedIt.generateAndSendEmail(envVariableService.getAdminEmailAddress(),"Scheduler: updateNullOmniTrackingData","Scheduler: updateNullOmniTrackingData:" +
                            "<br>ENV:"+envVariablesService.getEnvName()+
                            "<br>Method: OmniTrackingService.updateNullOmniTrackingData" +
                            "<br>SUCCESS: "+updateSuccess+
                            "<br>FAILED: "+updateFail,
                    false,"SCHEDULER -- updateNullOmniTrackingData");
        }
    }

    @Scheduled(cron = "0 1 1 * * ?") // 1:01am
    private void updateOverdueShippingData() {
        int updateSuccess = 0;
        int updateFail = 0;
        ConnectionAPI connection = new ConnectionAPI(envVariablesService.getAftershipApiKey());
        LocalDateTime fourtyEightHoursAgo = LocalDateTime.now();
        fourtyEightHoursAgo = fourtyEightHoursAgo.minusHours(48);
        for (OmniTracking omniTracking : omniTrackingDAO.findByActiveTrueAndSlugStartingWithAndLastUpdatedDateTimeBefore("INITIAL",fourtyEightHoursAgo)) {
            if(updateMissingShippingData(omniTracking,connection)) {
                updateSuccess++;
            } else {
                updateFail++;
            }
        }
        if(updateSuccess > 0 || updateFail > 0) {
            mailedIt.generateAndSendEmail(envVariableService.getAdminEmailAddress(),"Scheduler: updateOverdueShippingData","Scheduler: updateOverdueShippingData:" +
                            "<br>ENV:"+envVariablesService.getEnvName()+
                            "<br>Method: OmniTrackingService.updateOverdueShippingData" +
                            "<br>SUCCESS: "+updateSuccess+
                            "<br>FAILED: "+updateFail,
                    false,"SCHEDULER -- updateOverdueShippingData");
        }
    }


    public boolean updateMissingShippingData(OmniTracking omniTracking, ConnectionAPI connection) {
        boolean success = false;
        Tracking tracking = new Tracking();
        tracking.setId(omniTracking.getAftershipTrackingId());
        try {
            // Aftership only allows 10 requests per second
            // and we run WAY faster than that, so I slowed it down.
            TimeUnit.MILLISECONDS.sleep(500);
            boolean gotTracking = false;
            try {
                tracking = connection.getTrackingByNumber(tracking);
                gotTracking = true;
            } catch (AftershipAPIException asae) {
                mailedIt.generateAndSendEmail(envVariableService.getAdminEmailAddress(), "Tracking Number Failed", "Unable to get tracking from Aftership with tracking number " + omniTracking.getTrackingNumber() + " for omniTracing id " + omniTracking.getId(), false, "OmniTrackingService.updateMissingShippingData");
                bugsnag.notify(asae);
            }
            if (gotTracking) {
                if (tracking.getSlug() != null && tracking.getSlug().trim().length() > 0) {
                    omniTracking.setSlug(tracking.getSlug());
                    omniTracking.setCourierName(courierNameService.getCourierName(tracking.getSlug()));
                } else {
                    omniTracking.setSlug("INITIAL (OTS-POT)");
                }
                omniTracking.setDeliveryTime(tracking.getDeliveryTime());
                if (tracking.getDestinationCountryISO3() != null) {
                    omniTracking.setDestinationCountryISO3(tracking.getDestinationCountryISO3());
                }
                if (tracking.getOriginCountryISO3() != null) {
                    omniTracking.setOriginCountryISO3(tracking.getOriginCountryISO3());
                }
                omniTracking.setOrderIDPath(tracking.getOrderIDPath());
                omniTracking.setActive(tracking.isActive());
                if (!omniTracking.isActive()) {
                    if (tracking.getTag().equals(StatusTag.Exception) || tracking.getTag().equals(StatusTag.Expired)) {
                        omniTracking.setOmniTrackingState(OmniTrackingState.FAILED);
                    } else {
                        omniTracking.setOmniTrackingState(OmniTrackingState.COMPLETE);
                    }
                } else {
                    if (tracking.getTag().equals(StatusTag.Delivered)) {
                        omniTracking.setOmniTrackingState(OmniTrackingState.COMPLETE);
                        omniTracking.setActive(false);
                    } else {
                        omniTracking.setOmniTrackingState(OmniTrackingState.ACTIVE);
                    }
                }
                omniTracking.setExpectedDelivery(tracking.getExpectedDelivery());
                omniTracking.setShipmentPackageCount(tracking.getShipmentPackageCount());
                omniTracking.setShipmentType(tracking.getShipmentType());
                omniTracking.setSignedBy(tracking.getSignedBy());
                omniTracking.setSource(tracking.getSource());
                omniTracking.setTag(tracking.getTag());
                omniTracking.setTrackingAccountNumber(tracking.getTrackingAccountNumber());
                omniTracking.setTrackingPostalCode(tracking.getTrackingPostalCode());
                omniTracking.setTrackingShipDate(tracking.getTrackingShipDate());
                omniTracking.setLastUpdatedDateTime(LocalDateTime.now());
                omniTrackingDAO.save(omniTracking);
                String lastCheckpointTag = "";
                List<OmniTrackingDetail> omniTrackingDetails = omniTrackingDetailDAO.findByOmniTrackingId(omniTracking.getId());
                if(tracking.getCheckpoints().size() > omniTrackingDetails.size()) {
                    omniTrackingDetailDAO.delete(omniTrackingDetails);
                    lastCheckpointTag = updateCheckpoints(tracking, omniTracking.getId(), omniTracking.getAccountId());
                }


                for(OmniTransaction omniTransaction : omniTransactionDAO.findByOmniTrackingId(omniTracking.getId())) {
                    if(omniTracking.getSlug() != null && !omniTracking.getSlug().trim().isEmpty() && courierNameService.isSlug(omniTracking.getSlug())) {
                        omniTransaction.setShippingCarrierCode(omniTracking.getSlug());
                        omniTransaction.setShippingCarrierName(courierNameService.getCourierName(omniTracking.getSlug()));
                    } else {
                        omniTransaction.setShippingCarrierCode("INITIAL (OTS-UMS)");
                        omniTransaction.setShippingCarrierName("Waiting For Response");
                    }
                    omniTransaction.setShippingStatusTag(omniTracking.getTag());
                    if(tracking.getTag().equals(StatusTag.Exception) || tracking.getTag().equals(StatusTag.Expired) || tracking.getTag().equals(StatusTag.Delivered)) {
                        omniTransaction.setTransactionState(TransactionState.COMPLETE);
                    }
                    omniTransactionDAO.save(omniTransaction);
                    omniTransactionDetailDAO.save(new OmniTransactionDetail(omniTransaction.getAccountId(),omniTransaction.getId(),"Shipping Update: Tracking Tag NULL or Courier NULL, forced shipping tracking pull.", omniTransaction.getTransactionState()));

                }
                success = true;
            } else {
                // TODO: close tracking on transaction and ShippingTracking
            }
        } catch (Exception e) {
            bugsnag.notify(e);
        }
        return success;
    }

    private String updateCheckpoints(Tracking tracking, long omniTrackingId, long accountId) {
        String lastCheckpointTag = "";
        for (Checkpoint checkpoint : tracking.getCheckpoints()) {
            OmniTrackingDetail omniTrackingDetail =
                    new OmniTrackingDetail(
                            omniTrackingId,
                            accountId,
                            checkpoint.getCreatedAt(),
                            checkpoint.getCheckpointTime(),
                            checkpoint.getCity(),
                            checkpoint.getCountryISO3(),
                            checkpoint.getCountryName(),
                            checkpoint.getMessage(),
                            checkpoint.getState(),
                            checkpoint.getTag(),
                            checkpoint.getZip(),
                            checkpoint.getLocation());
            omniTrackingDetailDAO.save(omniTrackingDetail);
            lastCheckpointTag = omniTrackingDetail.getTag();
        }
        return lastCheckpointTag;
    }
}
