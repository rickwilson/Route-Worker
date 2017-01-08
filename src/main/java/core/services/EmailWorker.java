package core.services;

import core.entities.EmailLogger;
import core.entities.EmailLoggerDAO;
import core.outbound.MailedIt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class EmailWorker {
    private final EmailLoggerDAO emailLoggerDAO;
    private final MailedIt mailedIt;

    @Autowired
    public EmailWorker(EmailLoggerDAO emailLoggerDAO, MailedIt mailedIt) {
        Assert.notNull(emailLoggerDAO, "EmailLoggerDAO must not be null!");
        Assert.notNull(mailedIt, "MailedIt must not be null!");
        this.emailLoggerDAO = emailLoggerDAO;
        this.mailedIt = mailedIt;
    }

    @Scheduled(fixedDelay = 1800000) // every 30 min
    public void SendDelayedTransactionalEmails() {
        for(EmailLogger emailLogger : emailLoggerDAO.findBySentAndFailed(false,false)) {
            mailedIt.sendNow(emailLogger);
        }
    }
}
