package core.services;

import com.bugsnag.Bugsnag;
import core.Application;
import core.entities.CourierSlugName;
import core.entities.CourierSlugNameDAO;
import core.thirdParty.aftership.classes.ConnectionAPI;
import core.thirdParty.aftership.classes.Courier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.sql.Timestamp;
import java.util.ArrayList;

@Service
public class CourierNameService {

    private final CourierSlugNameDAO courierSlugNameDAO;
    private final Bugsnag bugsnag;
    private final EnvVariablesService envVariablesService;

    @Autowired
    public CourierNameService(CourierSlugNameDAO courierSlugNameDAO,
                              Bugsnag bugsnag,
                              EnvVariablesService envVariablesService) {
        Assert.notNull(courierSlugNameDAO, "CourierSlugNameDAO must not be null!");
        Assert.notNull(bugsnag, "Bugsnag must not be null!");
        Assert.notNull(envVariablesService, "EnvVariablesService must not be null!");
        this.courierSlugNameDAO = courierSlugNameDAO;
        this.bugsnag = bugsnag;
        this.envVariablesService = envVariablesService;
    }

    @Scheduled(fixedDelay = 604800000) // 7 days
    public void updateCourierSlugNames() {
        try {
            ConnectionAPI connection = new ConnectionAPI(envVariablesService.getAftershipApiKey());
            for (Courier courier : connection.getAllCouriers()) {
                CourierSlugName courierSlugName = courierSlugNameDAO.findBySlug(courier.getSlug());
                if (courierSlugName == null || courierSlugName.getId() < 1) {
                    courierSlugName = new CourierSlugName();
                    courierSlugName.setCreated(new Timestamp(System.currentTimeMillis()));
                    courierSlugName.setUpdateCount(0);
                    courierSlugName.setSlug(courier.getSlug());
                } else {
                    courierSlugName.setUpdateCount(courierSlugName.getUpdateCount() + 1);
                }
                courierSlugName.setName(courier.getName());
                courierSlugName.setOther_name(courier.getOther_name());
                ArrayList<String> reqFields = new ArrayList<>();
                if (courier.getRequireFields() != null && courier.getRequireFields().size() > 0) {
                    for (String s : courier.getRequireFields()) {
                        reqFields.add(s);
                    }
                }
                courierSlugName.setRequireFields(reqFields);
                courierSlugName.setUpdated(new Timestamp(System.currentTimeMillis()));
                courierSlugNameDAO.save(courierSlugName);
            }
        } catch (Exception e) {
            bugsnag.notify(e);
        }
    }

    @Cacheable("slugNames")
    public String getCourierName(String slug) {
        CourierSlugName courierSlugName = courierSlugNameDAO.findBySlug(slug);
        if(courierSlugName !=null && courierSlugName.getName() != null && courierSlugName.getName().trim().length() > 0) {
            return courierSlugName.getName();
        }
        return "Unknown";
    }

    @Cacheable("isSlug")
    public boolean isSlug(String slug) {
        CourierSlugName courierSlugName = courierSlugNameDAO.findBySlug(slug);
        if(courierSlugName !=null && courierSlugName.getId() > 0) {
            return true;
        }
        return false;
    }

    @Cacheable("getCourierSlug")
    public String getCourierSlug(String name) {
        CourierSlugName courierSlugName = courierSlugNameDAO.findByName(name);
        if(courierSlugName !=null && courierSlugName.getId() > 0) {
            return courierSlugName.getSlug();
        }
        return null;
    }

    @CacheEvict(value = { "slugNames"}, allEntries = true)
    public void evictSlugNamesCaches() {
    }

    @Scheduled(fixedRate = 82800000) // 23 days
    public void runEvict() {
        evictSlugNamesCaches();
    }
}
