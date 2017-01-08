package core.services;

import core.entities.GhostPassword;
import core.entities.GhostPasswordDAO;
import core.entities.User;
import core.entities.UserDAO;
import core.security.GenerateAPIKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.sql.Timestamp;

@Service
public class GhostWorker {
    private final UserDAO userDAO;
    private final GhostPasswordDAO ghostPasswordDAO;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public GhostWorker(UserDAO userDAO, PasswordEncoder passwordEncoder, GhostPasswordDAO ghostPasswordDAO) {
        Assert.notNull(userDAO, "UserDAO must not be null!");
        Assert.notNull(ghostPasswordDAO, "GhostPasswordDAO must not be null!");
        Assert.notNull(passwordEncoder, "PasswordEncoder must not be null!");
        this.userDAO = userDAO;
        this.ghostPasswordDAO = ghostPasswordDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @Scheduled(cron = "0 1 2 * * ?") // 2:01am
    private void updateGhostPasswords() {
        for(GhostPassword ghostPassword : ghostPasswordDAO.findAll()) {
            String ghostPwd = GenerateAPIKey.generateGenericKey();
            ghostPassword.setPassword(ghostPwd);
            ghostPassword.setUpdated(new Timestamp(System.currentTimeMillis()));

            User user = userDAO.findByUserName("ghost@"+ghostPassword.getAccountId());
            user.setPassword(passwordEncoder.encode(ghostPwd));
            userDAO.save(user);
            ghostPasswordDAO.save(ghostPassword);
        }
    }
}
